package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOptions;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

@CommandLine.Command(name = "counts-detailed", description = "Own aggregation of VIZ data for MATSim Counts")
public class CreateCountsFromMonthlyVizData implements MATSimAppCommand {

	/**
	 * Compass direction of travel (degrees clockwise from north) for the direction labels used in the VIZ station data.
	 */
	private static final Map<String, Double> COMPASS = Map.of(
			"nord", 0.,
			"nordost", 45.,
			"ost", 90.,
			"sudost", 135.,
			"sud", 180.,
			"sudwest", 225.,
			"west", 270.,
			"nordwest", 315.
	);

	/**
	 * A station's direction is one 45° compass sector. Accept links whose bearing lies within this tolerance around the
	 * sector's center. 55° keeps the neighbouring diagonal sectors admissible as a fallback, while still rejecting the
	 * opposite direction, which is always more than 90° away.
	 */
	private static final double DIRECTION_TOLERANCE = 55.;

	/**
	 * Distance in m subtracted from a candidate's distance if its road name is the one the station reports.
	 */
	private static final double NAME_BONUS = 40.;

	/**
	 * A candidate publishing a road name different from the station's is evidence against the match, not just missing
	 * evidence for it. The penalty is larger than {@link #NAME_BONUS}, so that a contradicting candidate has to be
	 * clearly closer than an unnamed one (motorways carry no name) to still win.
	 */
	private static final double NAME_CONTRADICTION_PENALTY = 60.;

	/**
	 * Suffixes collapsed when comparing road names, so that e.g. "Ollenhauer Straße" and "Ollenhauerstr." match.
	 */
	private static final List<String> STREET_SUFFIXES = List.of("strasse", "str");

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	@CommandLine.Option(names = "--input", description = "input count data directory", required = true)
	Path input;

	@CommandLine.Option(names = "--network", description = "MATSim network file path", required = true)
	Path networkPath;

	@CommandLine.Option(names = "--network-geometries", description = "network geometry file path", required = true)
	private Path geometries;

	@CommandLine.Option(names = "--output", description = "output directory", defaultValue = "input/")
	Path output;

	@CommandLine.Option(names = "--scenario", description = "scenario name for output files", defaultValue = "berlin-v6.0")
	String scenario;

	@CommandLine.Option(names = "--year", description = "year of count data", defaultValue = "2022")
	int year;

	@CommandLine.Option(names = "--use-road-names", description = "use road names to score map matching candidates")
	boolean roadNames;

	@CommandLine.Option(names = "--max-distance", description = "maximum distance [m] between a station and its matched link", defaultValue = "50")
	double maxDistance;

	@CommandLine.Mixin
	private final CsvOptions csv = new CsvOptions();

	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private final CountsOptions counts = new CountsOptions();

	private final Map<String, Station> stations = new HashMap<>();
	private final Logger logger = LogManager.getLogger(CreateCountsFromMonthlyVizData.class);

	public static void main(String[] args) {
		new CreateCountsFromMonthlyVizData().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		String outputString = !output.toString().endsWith("/") || !output.toString().endsWith("\\") ? output + "/" : output.toString();

		//Create Counts Objects
		Counts<Link> car = new Counts<>();
		car.setName(scenario + " car counts");
		car.setDescription("Car counts based on data from the 'Verkehrsinformationszentrale Berlin'.");
		car.setYear(year);
		Counts<Link> freight = new Counts<>();
		freight.setName(scenario + " freight counts");
		freight.setDescription("Freight counts based on data from the 'Verkehrsinformationszentrale Berlin'.");
		freight.setYear(year);

		//Get filepaths
		List<Path> countPaths = new ArrayList<>();
		Path stationPath = null;
		for (Path path : Files.walk(input).collect(Collectors.toList())) {
			//Station data is stored as .xlsx
			if (path.toString().endsWith(".xlsx") && stationPath == null)
				stationPath = path;
			//count data is stored in .gz
			if (path.toString().endsWith(".gz"))
				countPaths.add(path);
		}

		if (countPaths.size() < 12)
			logger.warn("Expected 12 files, but only {} files containing count data were provided.", countPaths.size());
		if (stationPath == null) {
			logger.warn("No station data were provided. Return Code 1");
			return 1;
		}

		extractStations(stationPath, stations, counts);
		matchWithNetwork(networkPath, geometries, stations, counts, outputString);

		List<CSVRecord> records = readCountData(countPaths);
		aggregateAndAssignCountData(records, stations, car, freight, outputString);

		new CountsWriter(car).write(outputString + scenario + ".counts_car.xml");
		new CountsWriter(freight).write(outputString + scenario + ".counts_freight.xml");

		return 0;
	}

	/**
	 * Compass bearing of a link, from its from- to its to-node, in degrees clockwise from north.
	 */
	private static double bearing(Link link) {

		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();

		double angle = Math.toDegrees(Math.atan2(to.getX() - from.getX(), to.getY() - from.getY()));

		return (angle + 360.) % 360.;
	}

	/**
	 * Smallest angle between two bearings, in degrees, always between 0 and 180.
	 */
	private static double angularDiff(double a, double b) {

		double diff = Math.abs(a - b) % 360.;

		return Math.min(diff, 360. - diff);
	}

	/**
	 * Maps a direction label of the VIZ station data onto a {@link #COMPASS} key, i.e. lower case and without umlauts.
	 */
	private static String normalizeDirection(String direction) {

		if (direction == null)
			return "";

		return direction.toLowerCase(Locale.GERMAN)
				.replace("ue", "u")
				.replace("ü", "u")
				.replace("ö", "o")
				.replace("ä", "a")
				.trim();
	}

	/**
	 * Straight line from a link's from- to its to-node, used for links without a detailed geometry.
	 */
	private static LineString link2LineString(Link link, GeometryFactory factory) {

		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());

		return factory.createLineString(new Coordinate[]{from, to});
	}

	/**
	 * Lower case, de-accented tokens of a road name, with the street suffix collapsed in both the standalone
	 * ("Ollenhauer Straße") and the concatenated form ("Ollenhauerstraße").
	 */
	private static Set<String> nameTokens(String name) {

		if (name == null || name.isBlank())
			return Set.of();

		String normalized = DIACRITICS.matcher(
				Normalizer.normalize(name.toLowerCase(Locale.GERMAN).replace("ß", "ss"), Normalizer.Form.NFKD)
		).replaceAll("");

		Set<String> tokens = new HashSet<>();
		for (String token : NON_ALPHANUMERIC.split(normalized)) {
			if (token.isEmpty() || STREET_SUFFIXES.contains(token))
				continue;

			for (String suffix : STREET_SUFFIXES) {
				if (token.endsWith(suffix) && token.length() > suffix.length()) {
					token = token.substring(0, token.length() - suffix.length());
					break;
				}
			}

			tokens.add(token);
		}

		return tokens;
	}

	/**
	 * Distance in m to add to a candidate, negative if the link's road name supports the match, positive if it
	 * contradicts it. Returns 0 if either side is unnamed, i.e. if there is nothing to compare.
	 * <p>
	 * Token set equality is deliberately strict: qualifiers like "Neue" or "Alte" usually denote a different street
	 * in Berlin, so "Späthstraße" must not be treated as "Neue Späthstraße".
	 */
	private static double nameScore(Station station, Link link) {

		if (link == null)
			return 0.;

		Object linkRoadName = link.getAttributes().getAttribute("name");

		Set<String> stationTokens = nameTokens(station.name());
		Set<String> linkTokens = linkRoadName == null ? Set.of() : nameTokens(linkRoadName.toString());

		if (stationTokens.isEmpty() || linkTokens.isEmpty())
			return 0.;

		return stationTokens.equals(linkTokens) ? -NAME_BONUS : NAME_CONTRADICTION_PENALTY;
	}

	private LineString parseCoordinates(String coordinateSequence, GeometryFactory factory) {

		String[] split = coordinateSequence.split("\\)");

		Coordinate[] coordinates = new Coordinate[split.length];

		for (int i = 0; i < split.length; i++) {
			String coord = split[i];
			int toRemove = coord.indexOf("(");

			String cleaned = coord.substring(toRemove + 1);

			String[] split1 = cleaned.split(",");

			Coordinate coordinate = new Coordinate();
			coordinate.setX(Double.parseDouble(split1[0]));
			coordinate.setY(Double.parseDouble(split1[1]));

			coordinates[i] = coordinate;
		}

		return factory.createLineString(coordinates);
	}

	/**
	 * Distance between a station and a link, based on the link's detailed geometry.
	 */
	private static double distance(Map<Id<Link>, Geometry> geometries, Link link, Station station, NetworkIndex.GeometryGetter<Station> getter) {
		return geometries.get(link.getId()).distance(getter.getGeometry(station));
	}

	/**
	 * Writes one row of the map matching result file. Link columns stay empty if there is no matched link.
	 */
	private static void printMatch(CSVPrinter printer, String stationId, Station station, Link link, double distance, String status) throws IOException {

		Object linkRoadName = link == null ? null : link.getAttributes().getAttribute("name");
		Double stationBearing = COMPASS.get(normalizeDirection(station.direction()));

		printer.printRecord(
				stationId,
				station.name(),
				station.direction(),
				status,
				link == null ? "" : link.getId(),
				linkRoadName == null ? "" : linkRoadName,
				Double.isNaN(distance) ? "" : Math.round(distance),
				link == null || stationBearing == null ? "" : Math.round(angularDiff(bearing(link), stationBearing))
		);
	}

	private void matchWithNetwork(Path networkPath, Path geometries, Map<String, Station> stations, CountsOptions countsOption, String outputString) throws TransformException, IOException {

		Network network = NetworkUtils.readNetwork(networkPath.toString());
		CoordinateTransformation transformation = crs.getTransformation();

		Map<Id<Link>, Geometry> networkGeometries = NetworkIndex.readGeometriesFromSumo(geometries.toString(), IdentityTransform.create(2));

		//The distance function only receives the geometry, so keep a geometry -> link mapping to access the link
		//attributes. Links without SUMO geometry are filled in here, so that every geometry in the index is known.
		GeometryFactory factory = new GeometryFactory();
		Map<Geometry, Link> geometryToLink = new IdentityHashMap<>();
		for (Link link : network.getLinks().values())
			geometryToLink.put(networkGeometries.computeIfAbsent(link.getId(), id -> link2LineString(link, factory)), link);

		NetworkIndex.GeometryGetter<Station> getter = toMatch -> {
			Coord coord = toMatch.coord();
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		};

		NetworkIndex<Station> index = new NetworkIndex<>(network, networkGeometries, maxDistance, getter);
		//Add link direction filter
		Set<String> unknownDirections = new HashSet<>();
		index.addLinkFilter((link, station) -> {
			Double stationBearing = COMPASS.get(normalizeDirection(station.direction()));

			//Unknown direction label: fall back to a purely distance based match
			if (stationBearing == null) {
				unknownDirections.add(station.direction());
				return true;
			}

			return angularDiff(bearing(link.link()), stationBearing) <= DIRECTION_TOLERANCE;
		});
		index.addLinkFilter((link, station) -> !link.link().getId().toString().startsWith("pt_"));

		//Road names are evidence, not a veto: they shift the distance a candidate is picked by, but never remove it
		if (roadNames)
			index.setDistanceCalculator((geom, station) -> geom.distance(getter.getGeometry(station))
					+ nameScore(station, geometryToLink.get(geom)));

		logger.info("Start matching stations to network.");
		int counter = 0;
		int tooFar = 0;
		//Every station is written to the matching file, matched or not, so that results can be reviewed
		Path matchingPath = Path.of(outputString + scenario + ".counts_matching.csv");
		try (CSVPrinter printer = csv.createPrinter(matchingPath)) {
			printer.printRecord("station_id", "station_name", "direction", "status", "link_id", "link_name", "distance", "angular_diff");

			for (var it = stations.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Station> next = it.next();
				Station station = next.getValue();

				//Check for manual matching!
				Id<Link> manuallyMatched = countsOption.isManuallyMatched(next.getKey());
				if (manuallyMatched != null) {
					if (!network.getLinks().containsKey(manuallyMatched))
						throw new RuntimeException("Link " + manuallyMatched.toString() + " is not in the network!");
					Link link = network.getLinks().get(manuallyMatched);
					station.linkAtomicReference().set(link);
					index.remove(link);
					printMatch(printer, next.getKey(), station, link, distance(networkGeometries, link, station, getter), "manual");
					continue;
				}

				Link query = index.query(station);

				if (query == null) {
					counter++;
					it.remove();
					printMatch(printer, next.getKey(), station, null, Double.NaN, "no_candidate");
					continue;
				}

				//The index queries an envelope around the station and picks the closest candidate without an upper bound,
				//so the actual distance has to be checked. The link stays in the index for the remaining stations.
				double distance = distance(networkGeometries, query, station, getter);
				if (distance > maxDistance) {
					logger.debug("Station {} - {} is {}m away from its closest candidate {}", next.getKey(),
							station.name(), Math.round(distance), query.getId());
					tooFar++;
					it.remove();
					printMatch(printer, next.getKey(), station, query, distance, "too_far");
					continue;
				}

				station.linkAtomicReference().set(query);
				index.remove(query);
				printMatch(printer, next.getKey(), station, query, distance, "matched");
			}
		}
		logger.info("Wrote map matching results to {}", matchingPath);

		if (!unknownDirections.isEmpty())
			logger.warn("Ignored {} unknown direction labels, these stations were matched by distance only: {}",
					unknownDirections.size(), unknownDirections);

		logger.info("Could not match {} stations, discarded {} more with no candidate within {}m", counter, tooFar, maxDistance);
	}

	private void extractStations(Path path, Map<String, Station> stations, CountsOptions countsOption) {

		XSSFSheet sheet;
		try (XSSFWorkbook wb = new XSSFWorkbook(path.toString())) {
			sheet = wb.getSheetAt(0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (Row row : sheet) {
			if (row.getRowNum() == 0)
				continue;

			String id = row.getCell(0).getStringCellValue();
			String lane = row.getCell(9).getStringCellValue();

			//for some reason count stations on bus lanes have an own station id, but same coordinates like the regular stations and causing trouble in map matching
			if (stations.containsKey(id) || "BUS".equals(lane) || countsOption.isIgnored(id))
				continue;

			String name = row.getCell(5).getStringCellValue();
			String direction = row.getCell(8).getStringCellValue();
			double x = row.getCell(11).getNumericCellValue();
			double y = row.getCell(12).getNumericCellValue();

			Station station = new Station(id, name, direction, new Coord(x, y));
			stations.put(id, station);
		}
	}

	private List<CSVRecord> readCountData(List<Path> paths) {

		//Read all files and build one record collection
		logger.info("Start parsing count data.");
		List<CSVRecord> records = new ArrayList<>();
		for (Path path : paths) {
			try (GZIPInputStream gis = new GZIPInputStream(
					new FileInputStream(path.toFile()))
			) {
				Path decompressed = Path.of(path.toString().replace(".gz", ""));
				if (!Files.exists(decompressed))
					Files.copy(gis, decompressed);

				List<CSVRecord> month = csv.createParser(decompressed).getRecords();

				records.addAll(month);
			} catch (IOException e) {
				logger.warn("Error processing file {}: ", path.toString());
				throw new RuntimeException(e);
			}
		}

		return records;
	}

	private void aggregateAndAssignCountData(List<CSVRecord> records, Map<String, Station> stations, Counts<Link> carCounts, Counts<Link> freightCounts, String outputString) {

		//Create table from records first
		Table table;
		{
			StringColumn id = StringColumn.create(ColumnNames.id);
			DateColumn date = DateColumn.create(ColumnNames.date);
			StringColumn hour = StringColumn.create(ColumnNames.hour);
			DoubleColumn car = DoubleColumn.create(ColumnNames.carVolume);
			DoubleColumn freight = DoubleColumn.create(ColumnNames.freightVolume);
			DoubleColumn carSpeed = DoubleColumn.create(ColumnNames.carAvgSpeed);
			DoubleColumn freightSpeed = DoubleColumn.create(ColumnNames.freightAvgSpeed);

			for (CSVRecord row : records) {
				id.append(row.get(0));
				LocalDate formatedDate = LocalDate.parse(row.get(1), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN));
				date.append(formatedDate);
				hour.append(row.get(2));
				car.append(Double.parseDouble(row.get(6)));
				carSpeed.append(Double.parseDouble(row.get(7)));
				freight.append(Double.parseDouble(row.get(8)));
				freightSpeed.append(Double.parseDouble(row.get(9)));
			}

			table = Table.create(id, date, hour, car, freight, carSpeed, freightSpeed);
		}

		Predicate<LocalDate> dayFilter = localDate -> {
			int day = localDate.getDayOfWeek().getValue();
			return day > 1 && day < 5;
		};

		//filter and aggregation
		logger.info("Start Aggregation");
		Table summarized = table.where(t -> t.dateColumn(ColumnNames.date).eval(dayFilter))
				.summarize(ColumnNames.carVolume, ColumnNames.freightVolume, ColumnNames.carAvgSpeed, ColumnNames.freightAvgSpeed, mean)
				.by(ColumnNames.id, ColumnNames.hour);

		//Column names were edited by summarize function
		for (String name : table.columnNames())
			summarized.columnNames().stream().filter(s -> s.contains(name)).findFirst().ifPresent(s -> summarized.column(s).setName(name));

		//Assign aggregted hourly traffic volumes to count objects AND write avg speed per link and hour to csv file
		try (CSVPrinter printer = csv.createPrinter(Path.of(outputString + scenario + ".avg_speed.csv"))) {
			printer.print(ColumnNames.id);
			printer.print(ColumnNames.hour);
			printer.print(ColumnNames.carAvgSpeed);
			printer.print(ColumnNames.freightAvgSpeed);
			printer.println();

			int counter = 0;
			for (Map.Entry<String, Station> entry : stations.entrySet()) {
				String key = entry.getKey();
				Station station = entry.getValue();

				Table idFiltered = summarized.copy().where(t -> t.stringColumn(ColumnNames.id).isEqualTo(key));

				if (idFiltered.rowCount() != 24) {
					logger.warn("Station {} - {} does not contain hour values for the whole day!", key, station.name());
					counter++;
					continue;
				}

				Count<Link> carCount = carCounts.createAndAddCount(station.linkAtomicReference().get().getId(), station.getStationId());
				Count<Link> freightCount = freightCounts.createAndAddCount(station.linkAtomicReference().get().getId(), station.getStationId());

				for (tech.tablesaw.api.Row row : idFiltered) {
					double car = row.getDouble(ColumnNames.carVolume);
					//in VIZ data hours starts at 0, in MATSim count data starts at 1
					int hour = Integer.parseInt(row.getString(ColumnNames.hour)) + 1;
					double freight = row.getDouble(ColumnNames.freightVolume);
					carCount.createVolume(hour, Math.round(car));
					freightCount.createVolume(hour, Math.round(freight));

					//print to file
					double carSpeed = row.getDouble(ColumnNames.carAvgSpeed);
					double freightSpeed = row.getDouble(ColumnNames.freightAvgSpeed);

					printer.print(station.linkAtomicReference().get().getId().toString());
					printer.print(hour);
					printer.print(Math.round(carSpeed));
					printer.print(Math.round(freightSpeed));
					printer.println();
				}
			}
			logger.info("Skipped {} stations, because data was incomplete!", counter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class ColumnNames {
		static String id = "id";
		static String date = "date";
		static String hour = "hour";
		static String carVolume = "car_volume";
		static String carAvgSpeed = "car_avg_speed";
		static String freightVolume = "freight_volume";
		static String freightAvgSpeed = "freight_avg_speed";
	}

}
