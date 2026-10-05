package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Aggregate statistics for the OD matrices produced by the small scale commercial traffic generation.
 * <p>
 * Reads the {@code calculatedData} folder that {@code GenerateSmallScaleCommercialTrafficDemand} writes next to its
 * output population: one {@code odMatrix_<segment>_<modeOrVehType>_purpose<N>.csv} per segment/mode/purpose, plus the
 * {@code TrafficVolume_<segment>_{start,stop}PerZone_<sample>pt.csv} files holding the generation potentials that went
 * into the gravity model.
 * <p>
 * Three things are worth knowing about the model this analyses, because they determine what the numbers below mean:
 * <ul>
 *     <li>The gravity model is <em>destination constrained</em>. {@code sum_i F_ij == Z_j} holds by construction; the
 *     origin potentials {@code Q_i} are only weights and are <em>not</em> reproduced. The origin-side fit is therefore
 *     a real diagnostic, the destination-side one only a sanity check on the rounding carry.</li>
 *     <li>A matrix cell counts <em>stops</em> served in j from i, not vehicle trips. Tours are chained afterwards, so
 *     the realised leg pattern is an output of the tour building, not of this matrix.</li>
 *     <li>The resistance function uses monetary travel cost, not distance. The decay estimated here is therefore an
 *     effective, reduced-form value per kilometre. It is the interpretable one, but it is not the {@code --resistanceFactor}
 *     that went in.</li>
 * </ul>
 */
@CommandLine.Command(
	name = "commercial-od",
	description = "Aggregate statistics for the small scale commercial traffic OD matrices."
)
public class CommercialOdMatrixAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CommercialOdMatrixAnalysis.class);

	/** Upper bounds in km. Open ended above the last one. */
	private static final double[] DIST_BINS = {1, 2, 5, 10, 15, 20, 30, 50, 75, 100, 150, 200};

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--calculated-data", required = true,
		description = "Path to the 'calculatedData' folder written next to the generated commercial population.")
	private Path calculatedData;

	@CommandLine.Option(names = "--zone-id-column", defaultValue = "id",
		description = "Attribute of the zone shape file holding the zone id used in the OD matrices.")
	private String zoneIdColumn;

	@CommandLine.Option(names = "--target-crs", defaultValue = "EPSG:25832",
		description = "Metric CRS the zone centroids are projected into before distances are measured.")
	private String targetCrs;

	@CommandLine.Option(names = "--output", required = true, description = "Output folder for the csv files.")
	private Path output;

	public static void main(String[] args) {
		new CommercialOdMatrixAnalysis().execute(args);
	}

	/** One matrix file: segment x mode/vehicle type x purpose. */
	private record MatrixKey(String segment, String modeOrVehType, String purpose) {
		@Override
		public String toString() {
			return segment + "/" + modeOrVehType + "/purpose" + purpose;
		}
	}

	/** Accumulates the per-cell statistics of one or more matrices. */
	private static final class Stats {
		double trips;
		double intraZonalTrips;
		long nonZeroCells;
		double tripKm;
		final double[] binnedTrips = new double[DIST_BINS.length + 1];
		final double[] binnedKm = new double[DIST_BINS.length + 1];
		// interzonal only: the intrazonal resistance is hard coded to 1.0 in the model, so those cells sit above any
		// decay curve by construction and would bias the fit
		final double[] binnedTripsInter = new double[DIST_BINS.length + 1];
		final double[] binnedExpectedInter = new double[DIST_BINS.length + 1];
		final double[] binnedKmInter = new double[DIST_BINS.length + 1];
		final List<double[]> distanceWeights = new ArrayList<>();

		void add(double dist, double f, double expected, boolean intraZonal) {
			int b = bin(dist);
			trips += f;
			tripKm += f * dist;
			binnedTrips[b] += f;
			binnedKm[b] += f * dist;
			if (!intraZonal) {
				binnedTripsInter[b] += f;
				binnedExpectedInter[b] += expected;
				binnedKmInter[b] += f * dist;
			}
			if (f > 0) {
				nonZeroCells++;
				distanceWeights.add(new double[]{dist, f});
				if (dist == 0)
					intraZonalTrips += f;
			}
		}

		double quantile(double q) {
			if (distanceWeights.isEmpty())
				return Double.NaN;
			distanceWeights.sort(Comparator.comparingDouble(a -> a[0]));
			double target = q * trips;
			double cum = 0;
			for (double[] dw : distanceWeights) {
				cum += dw[1];
				if (cum >= target)
					return dw[0];
			}
			return distanceWeights.getLast()[0];
		}
	}

	private static int bin(double distKm) {
		for (int i = 0; i < DIST_BINS.length; i++) {
			if (distKm < DIST_BINS[i])
				return i;
		}
		return DIST_BINS.length;
	}

	private static String binLabel(int i) {
		if (i == 0)
			return "[0," + fmt(DIST_BINS[0]) + ")";
		if (i == DIST_BINS.length)
			return "[" + fmt(DIST_BINS[DIST_BINS.length - 1]) + ",inf)";
		return "[" + fmt(DIST_BINS[i - 1]) + "," + fmt(DIST_BINS[i]) + ")";
	}

	private static String fmt(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined())
			throw new IllegalArgumentException("Zone shape file is required [--shp]");
		if (!Files.isDirectory(calculatedData))
			throw new IllegalArgumentException("Not a directory: " + calculatedData);

		Files.createDirectories(output);

		Map<String, Coord> centroids = readZoneCentroids();
		log.info("Read {} zone centroids from {}", centroids.size(), shp.getShapeFile());

		// generation potentials that went into the gravity model: potentials[segment][zone][mode][purpose]
		Map<String, Map<String, Map<String, Map<String, Double>>>> startPotential = new HashMap<>();
		Map<String, Map<String, Map<String, Map<String, Double>>>> stopPotential = new HashMap<>();
		try (Stream<Path> files = Files.list(calculatedData)) {
			for (Path p : files.sorted().toList()) {
				String n = p.getFileName().toString();
				if (!n.startsWith("TrafficVolume_"))
					continue;
				String[] parts = n.split("_");
				String segment = parts[1];
				if (n.contains("_startPerZone_"))
					readTrafficVolume(p, startPotential.computeIfAbsent(segment, k -> new HashMap<>()));
				else if (n.contains("_stopPerZone_"))
					readTrafficVolume(p, stopPotential.computeIfAbsent(segment, k -> new HashMap<>()));
			}
		}

		Map<MatrixKey, Stats> perMatrix = new LinkedHashMap<>();
		Map<String, Stats> perSegment = new LinkedHashMap<>();
		Stats total = new Stats();

		// realised origin/destination totals, summed over mode and purpose, per segment
		Map<String, Map<String, Double>> realisedOrigin = new HashMap<>();
		Map<String, Map<String, Double>> realisedDestination = new HashMap<>();

		List<Path> matrices;
		try (Stream<Path> files = Files.list(calculatedData)) {
			matrices = files.filter(p -> p.getFileName().toString().startsWith("odMatrix_")).sorted().toList();
		}
		if (matrices.isEmpty())
			throw new IllegalStateException("No odMatrix_*.csv found in " + calculatedData);

		for (Path p : matrices) {
			MatrixKey key = parseKey(p);
			Stats st = new Stats();

			Map<String, Double> q = potentialFor(startPotential, key);
			Map<String, Double> z = potentialFor(stopPotential, key);
			double sumQ = q.values().stream().mapToDouble(Double::doubleValue).sum();

			readMatrix(p, (origin, dest, value) -> {
				Coord a = centroids.get(origin);
				Coord b = centroids.get(dest);
				if (a == null || b == null) {
					log.warn("No centroid for zone {} or {}, cell skipped", origin, dest);
					return;
				}
				boolean intraZonal = origin.equals(dest);
				double dist = intraZonal ? 0 : distanceKm(a, b);
				// distance blind reference: destination constrained gravity model with resistance == 1 everywhere.
				// Must be summed over every cell, not just the occupied ones, or it collapses exactly where the
				// observed matrix is sparse and the ratio blows up.
				double expected = sumQ > 0 ? q.getOrDefault(origin, 0d) * z.getOrDefault(dest, 0d) / sumQ : 0;

				st.add(dist, value, expected, intraZonal);
				perSegment.computeIfAbsent(key.segment(), k -> new Stats()).add(dist, value, expected, intraZonal);
				total.add(dist, value, expected, intraZonal);

				if (value == 0)
					return;

				realisedOrigin.computeIfAbsent(key.segment(), k -> new HashMap<>()).merge(origin, value, Double::sum);
				realisedDestination.computeIfAbsent(key.segment(), k -> new HashMap<>()).merge(dest, value, Double::sum);
			});

			perMatrix.put(key, st);
			log.info("{}: {} stops, {} non-zero cells, mean distance {} km",
				key, Math.round(st.trips), st.nonZeroCells, round(st.trips > 0 ? st.tripKm / st.trips : 0));
		}

		writeSummary(perMatrix, perSegment, total);
		writeDistanceDistribution(perSegment, total);
		writeOriginFit(startPotential, stopPotential, realisedOrigin, realisedDestination);
		writeDecay(perSegment, total);

		return 0;
	}

	// ------------------------------------------------------------------ reading

	private Map<String, Coord> readZoneCentroids() {
		// createTransformation(x) goes x -> shp, which is the direction for testing scenario coordinates against the
		// shape. Here the centroids come out of the shape and need to go the other way, into the metric CRS.
		CoordinateTransformation ct = shp.createInverseTransformation(targetCrs);
		Map<String, Coord> centroids = new HashMap<>();
		for (SimpleFeature f : shp.readFeatures()) {
			Object id = f.getAttribute(zoneIdColumn);
			if (id == null)
				throw new IllegalArgumentException("Zone shape file has no attribute '" + zoneIdColumn + "'. Available: "
					+ f.getFeatureType().getAttributeDescriptors());
			Point c = ((Geometry) f.getDefaultGeometry()).getCentroid();
			centroids.put(id.toString(), ct.transform(new Coord(c.getX(), c.getY())));
		}
		return centroids;
	}

	/** zoneID, mode/vehType, then one column per purpose. */
	private void readTrafficVolume(Path p, Map<String, Map<String, Map<String, Double>>> into) throws Exception {
		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(p.toString()),
			CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).get())) {
			List<String> header = parser.getHeaderNames();
			for (CSVRecord r : parser) {
				String zone = r.get("zoneID");
				String mode = r.get("mode/vehType");
				Map<String, Double> byPurpose = into
					.computeIfAbsent(zone, k -> new HashMap<>())
					.computeIfAbsent(mode, k -> new HashMap<>());
				for (String col : header) {
					if (col.equals("zoneID") || col.equals("mode/vehType"))
						continue;
					String v = r.get(col);
					byPurpose.put(col, v == null || v.isBlank() ? 0 : Double.parseDouble(v));
				}
			}
		}
	}

	/** Pull out the (zone -> potential) map belonging to one matrix. */
	private Map<String, Double> potentialFor(Map<String, Map<String, Map<String, Map<String, Double>>>> all, MatrixKey key) {
		Map<String, Double> out = new HashMap<>();
		Map<String, Map<String, Map<String, Double>>> perZone = all.get(key.segment());
		if (perZone == null)
			return out;
		for (Map.Entry<String, Map<String, Map<String, Double>>> e : perZone.entrySet()) {
			Map<String, Double> byPurpose = e.getValue().get(key.modeOrVehType());
			if (byPurpose != null)
				out.put(e.getKey(), byPurpose.getOrDefault(key.purpose(), 0d));
		}
		return out;
	}

	private interface CellConsumer {
		void accept(String origin, String dest, double value);
	}

	/** Wide TSV: first row are the destination zone ids, first column the origin zone. */
	private void readMatrix(Path p, CellConsumer consumer) throws Exception {
		try (BufferedReader reader = IOUtils.getBufferedReader(p.toString())) {
			String headerLine = reader.readLine();
			if (headerLine == null)
				return;
			String[] header = headerLine.split("\t", -1);
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank())
					continue;
				String[] cells = line.split("\t", -1);
				String origin = cells[0];
				for (int i = 1; i < cells.length && i < header.length; i++) {
					String v = cells[i];
					if (v.isBlank())
						continue;
					consumer.accept(origin, header[i], Double.parseDouble(v));
				}
			}
		}
	}

	private static MatrixKey parseKey(Path p) {
		// odMatrix_<segment>_<modeOrVehType>_purpose<N>.csv
		String n = p.getFileName().toString().replaceFirst("^odMatrix_", "").replaceFirst("\\.csv$", "");
		int last = n.lastIndexOf("_purpose");
		if (last < 0)
			throw new IllegalArgumentException("Cannot parse matrix file name: " + p);
		String purpose = n.substring(last + "_purpose".length());
		String rest = n.substring(0, last);
		int firstUnderscore = rest.indexOf('_');
		return new MatrixKey(rest.substring(0, firstUnderscore), rest.substring(firstUnderscore + 1), purpose);
	}

	private static double distanceKm(Coord a, Coord b) {
		return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) / 1000d;
	}

	private static double round(double v) {
		return Math.round(v * 100d) / 100d;
	}

	// ------------------------------------------------------------------ writing

	private void writeSummary(Map<MatrixKey, Stats> perMatrix, Map<String, Stats> perSegment, Stats total) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_summary.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "modeOrVehType", "purpose", "stops", "nonZeroCells",
				"intraZonalShare", "meanDistanceKm", "medianDistanceKm", "p90DistanceKm", "shareOver50km", "stopKm").get())) {

			for (Map.Entry<MatrixKey, Stats> e : perMatrix.entrySet())
				writeSummaryRow(csv, e.getKey().segment(), e.getKey().modeOrVehType(), e.getKey().purpose(), e.getValue());
			for (Map.Entry<String, Stats> e : perSegment.entrySet())
				writeSummaryRow(csv, e.getKey(), "*", "*", e.getValue());
			writeSummaryRow(csv, "*", "*", "*", total);
		}
	}

	private void writeSummaryRow(CSVPrinter csv, String segment, String mode, String purpose, Stats st) throws Exception {
		double over50 = 0;
		for (int i = 0; i <= DIST_BINS.length; i++) {
			if (i >= bin(50))
				over50 += st.binnedTrips[i];
		}
		csv.printRecord(segment, mode, purpose, Math.round(st.trips), st.nonZeroCells,
			round(st.trips > 0 ? st.intraZonalTrips / st.trips : 0),
			round(st.trips > 0 ? st.tripKm / st.trips : 0),
			round(st.quantile(0.5)), round(st.quantile(0.9)),
			round(st.trips > 0 ? over50 / st.trips : 0), Math.round(st.tripKm));
	}

	private void writeDistanceDistribution(Map<String, Stats> perSegment, Stats total) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_distance_distribution.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "distanceBinKm", "stops", "share", "stopKm").get())) {

			Map<String, Stats> all = new LinkedHashMap<>(perSegment);
			all.put("*", total);
			for (Map.Entry<String, Stats> e : all.entrySet()) {
				Stats st = e.getValue();
				for (int i = 0; i <= DIST_BINS.length; i++) {
					csv.printRecord(e.getKey(), binLabel(i), Math.round(st.binnedTrips[i]),
						round(st.trips > 0 ? st.binnedTrips[i] / st.trips : 0), Math.round(st.binnedKm[i]));
				}
			}
		}
	}

	/**
	 * The origin side is where the destination constrained gravity model is allowed to miss. A ratio far from 1 means
	 * the zone's start potential was scaled to make the destination totals add up.
	 */
	private void writeOriginFit(Map<String, Map<String, Map<String, Map<String, Double>>>> startPotential,
								Map<String, Map<String, Map<String, Map<String, Double>>>> stopPotential,
								Map<String, Map<String, Double>> realisedOrigin,
								Map<String, Map<String, Double>> realisedDestination) throws Exception {

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_origin_fit.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "zone", "side", "target", "realised", "ratio").get())) {

			for (String segment : realisedOrigin.keySet()) {
				writeFitSide(csv, segment, "origin", flatten(startPotential.get(segment)), realisedOrigin.get(segment));
				writeFitSide(csv, segment, "destination", flatten(stopPotential.get(segment)), realisedDestination.get(segment));
			}
		}

		// and the aggregate view, which is the one you actually read
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_fit_summary.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "side", "zones", "targetTotal", "realisedTotal",
				"totalRatio", "correlation", "meanAbsRelError", "medianRatio", "zonesWithin10pct").get())) {

			for (String segment : realisedOrigin.keySet()) {
				writeFitSummary(csv, segment, "origin", flatten(startPotential.get(segment)), realisedOrigin.get(segment));
				writeFitSummary(csv, segment, "destination", flatten(stopPotential.get(segment)), realisedDestination.get(segment));
			}
		}
	}

	/** Sum a zone -> mode -> purpose -> value map over mode and purpose. */
	private Map<String, Double> flatten(Map<String, Map<String, Map<String, Double>>> perZone) {
		Map<String, Double> out = new HashMap<>();
		if (perZone == null)
			return out;
		perZone.forEach((zone, byMode) -> byMode.forEach((mode, byPurpose) ->
			byPurpose.values().forEach(v -> out.merge(zone, v, Double::sum))));
		return out;
	}

	private void writeFitSide(CSVPrinter csv, String segment, String side,
							  Map<String, Double> target, Map<String, Double> realised) throws Exception {
		Set<String> zones = new TreeSet<>(target.keySet());
		zones.addAll(realised.keySet());
		for (String zone : zones) {
			double t = target.getOrDefault(zone, 0d);
			double r = realised.getOrDefault(zone, 0d);
			csv.printRecord(segment, zone, side, round(t), round(r), t > 0 ? round(r / t) : "");
		}
	}

	private void writeFitSummary(CSVPrinter csv, String segment, String side,
								 Map<String, Double> target, Map<String, Double> realised) throws Exception {
		Set<String> zones = new TreeSet<>(target.keySet());
		zones.addAll(realised.keySet());

		double sumT = 0, sumR = 0, n = 0, relErr = 0, within = 0;
		List<Double> ratios = new ArrayList<>();
		List<double[]> pairs = new ArrayList<>();
		for (String zone : zones) {
			double t = target.getOrDefault(zone, 0d);
			double r = realised.getOrDefault(zone, 0d);
			sumT += t;
			sumR += r;
			pairs.add(new double[]{t, r});
			if (t > 0) {
				n++;
				double ratio = r / t;
				ratios.add(ratio);
				relErr += Math.abs(r - t) / t;
				if (Math.abs(ratio - 1) <= 0.1)
					within++;
			}
		}
		Collections.sort(ratios);
		csv.printRecord(segment, side, zones.size(), Math.round(sumT), Math.round(sumR),
			sumT > 0 ? round(sumR / sumT) : "", round(correlation(pairs)),
			n > 0 ? round(relErr / n) : "",
			ratios.isEmpty() ? "" : round(ratios.get(ratios.size() / 2)),
			n > 0 ? round(within / n) : "");
	}

	private static double correlation(List<double[]> pairs) {
		int n = pairs.size();
		if (n < 2)
			return Double.NaN;
		double mx = pairs.stream().mapToDouble(p -> p[0]).average().orElse(0);
		double my = pairs.stream().mapToDouble(p -> p[1]).average().orElse(0);
		double sxy = 0, sxx = 0, syy = 0;
		for (double[] p : pairs) {
			sxy += (p[0] - mx) * (p[1] - my);
			sxx += (p[0] - mx) * (p[0] - mx);
			syy += (p[1] - my) * (p[1] - my);
		}
		return sxx > 0 && syy > 0 ? sxy / Math.sqrt(sxx * syy) : Double.NaN;
	}

	/**
	 * Realised distance decay: observed stops per distance bin against a distance blind reference with the same
	 * potentials. The slope of ln(ratio) over distance is the effective decay per km.
	 */
	private void writeDecay(Map<String, Stats> perSegment, Stats total) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_decay.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "distanceBinKm", "meanDistanceKm", "observedStops",
				"expectedIfNoDecay", "ratio", "lnRatio").get())) {

			Map<String, Stats> all = new LinkedHashMap<>(perSegment);
			all.put("*", total);

			try (CSVPrinter fit = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_od_decay_fit.csv")),
				CSVFormat.DEFAULT.builder().setHeader("segment", "effectiveDecayPerKm", "halfValueDistanceKm", "bins", "rSquared").get())) {

				for (Map.Entry<String, Stats> e : all.entrySet()) {
					Stats st = e.getValue();
					List<double[]> points = new ArrayList<>();
					for (int i = 0; i <= DIST_BINS.length; i++) {
						double obs = st.binnedTripsInter[i];
						double exp = st.binnedExpectedInter[i];
						double meanDist = obs > 0 ? st.binnedKmInter[i] / obs : Double.NaN;
						Double ratio = exp > 0 ? obs / exp : null;
						csv.printRecord(e.getKey(), binLabel(i), round(meanDist), Math.round(obs), Math.round(exp),
							ratio == null ? "" : round(ratio), ratio == null || ratio <= 0 ? "" : round(Math.log(ratio)));
						// weight by observed stops so that a handful of far, near empty bins cannot set the slope
						if (ratio != null && ratio > 0 && !Double.isNaN(meanDist) && meanDist > 0)
							points.add(new double[]{meanDist, Math.log(ratio), obs});
					}
					double[] reg = regress(points);
					double beta = -reg[0];
					fit.printRecord(e.getKey(), round(beta), beta > 0 ? round(Math.log(2) / beta) : "",
						points.size(), round(reg[2]));
				}
			}
		}
	}

	/** Weighted least squares over {x, y, weight}. Returns slope, intercept, r squared. */
	private static double[] regress(List<double[]> pts) {
		if (pts.size() < 2)
			return new double[]{Double.NaN, Double.NaN, Double.NaN};
		double sw = pts.stream().mapToDouble(p -> p[2]).sum();
		if (sw <= 0)
			return new double[]{Double.NaN, Double.NaN, Double.NaN};
		double mx = pts.stream().mapToDouble(p -> p[0] * p[2]).sum() / sw;
		double my = pts.stream().mapToDouble(p -> p[1] * p[2]).sum() / sw;
		double sxy = 0, sxx = 0, syy = 0;
		for (double[] p : pts) {
			sxy += p[2] * (p[0] - mx) * (p[1] - my);
			sxx += p[2] * (p[0] - mx) * (p[0] - mx);
			syy += p[2] * (p[1] - my) * (p[1] - my);
		}
		if (sxx == 0)
			return new double[]{Double.NaN, Double.NaN, Double.NaN};
		double slope = sxy / sxx;
		return new double[]{slope, my - slope * mx, syy > 0 ? (sxy * sxy) / (sxx * syy) : Double.NaN};
	}
}
