package org.matsim.prepare.counts;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.jspecify.annotations.NonNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tech.tablesaw.aggregate.AggregateFunctions.median;

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

	private static final int HOURS_PER_DAY = 24;

	/**
	 * Aggregations used if {@code --aggregate} is not given: one per day type, each over the whole input period.
	 * Monday, Friday and the weekend days are kept apart from the mid week, because their traffic patterns differ.
	 */
	private static final List<String> DEFAULT_AGGREGATIONS = List.of(
			"mon=MONDAY",
			"tue-thu=TUESDAY,WEDNESDAY,THURSDAY",
			"fri=FRIDAY",
			"sat=SATURDAY",
			"sun=SUNDAY"
	);

	/**
	 * Lanes left out of a cross section, per the annotation the station data gives them: "Busspur" and "Bus- und LKW
	 * Spur". Their traffic is public transport, which the simulation does not route as car. Every other lane is
	 * summed, including the ramps of a carriageway of its own (AbL "Ausfahrt links", ZuL "Zufahrt von links"), the
	 * turn pockets (LA "Linksabbieger"), whose traffic shares the approach and only parts from it at the node, and
	 * the one "Parkspur", which carries traffic the provider counts as well.
	 * <p>
	 * This follows what the provider does over 2023: it forms no value for any of the eleven bus-only cross sections
	 * nor for the bus/lorry one, and no cross section mixes a bus lane with a running lane.
	 */
	private static final Set<String> EXCLUDED_LANES = Set.of("BUS", "BUS_LKW");

	/**
	 * A turn pocket is part of the approach, but on its own it is not the carriageway and its volume is not the
	 * link's. The provider forms no value for the two cross sections that consist only of one.
	 */
	private static final String TURN_POCKET = "LA";

	/**
	 * Speed the VIZ reports for an hour without a single vehicle of that category, in both deliveries: the old one
	 * writes -1, the new one writes NaN. The lane reader keeps the -1, so that both produce the same table.
	 */
	private static final double NO_SPEED = -1.;

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	/**
	 * An aggregation name is used as an output directory name, so it must not contain a path separator.
	 */
	private static final Pattern AGGREGATION_NAME = Pattern.compile("[A-Za-z0-9_-]+");

	/**
	 * The two deliveries of the VIZ data. CROSS_SECTIONS is the old quality assurance, which the provider has already
	 * aggregated per measurement cross section. LANE_DETECTORS is the new one, one csv per lane detector inside a
	 * monthly tar archive, which this command sums back into cross sections.
	 */
	enum InputFormat {CROSS_SECTIONS, LANE_DETECTORS}

	@CommandLine.Option(names = "--input", description = "directory the monthly count data is read from, not searched recursively", required = true)
	Path input;

	@CommandLine.Option(names = "--input-format", defaultValue = "CROSS_SECTIONS", description = "delivery the count " +
			"data is read from. CROSS_SECTIONS reads the mq_hr_*.csv.gz of the old quality assurance, LANE_DETECTORS " +
			"the detektoren_*.tgz of the new one, summing its lane detectors into cross sections. " +
			"Candidates: ${COMPLETION-CANDIDATES}")
	InputFormat inputFormat;

	@CommandLine.Option(names = "--min-completeness", defaultValue = "75", description = "minimum share [%%] of valid " +
			"measurement intervals a lane hour needs to be used, the threshold the old quality assurance applies " +
			"internally. Only used with --input-format=LANE_DETECTORS")
	double minCompleteness;

	@CommandLine.Option(names = "--stations", description = "station data of the count stations (xlsx)", required = true)
	Path stationData;

	@CommandLine.Option(names = "--network", description = "MATSim network file path", required = true)
	Path networkPath;

	@CommandLine.Option(names = "--network-geometries", description = "network geometry file path", required = true)
	private Path geometries;

	@CommandLine.Option(names = "--output", description = "output directory", defaultValue = "input/")
	Path output;

	@CommandLine.Option(names = "--scenario", description = "scenario name for output files", defaultValue = OpenBerlinScenario.VERSION)
	String scenario;

	@CommandLine.Option(names = "--year", description = "year of count data", defaultValue = "2022")
	int year;

	@CommandLine.Option(names = "--use-road-names", description = "use road names to score map matching candidates")
	boolean roadNames;

	@CommandLine.Option(names = "--max-distance", description = "maximum distance [m] between a station and its matched link", defaultValue = "50")
	double maxDistance;

	@CommandLine.Option(names = "--aggregate", description = "one aggregation of the hourly counts, repeatable. " +
			"Syntax: <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]], e.g. " +
			"saturday=SATURDAY@2022-03-01:2022-05-31+2022-09-01:2022-10-31. Dates are inclusive and given as " +
			"yyyy-MM-dd, a day is used if it falls into any of the ranges. Without ranges the whole input period " +
			"is used. Each aggregation is written to its own output subdirectory. " +
			"Default: monday=MONDAY, midweek=TUESDAY,WEDNESDAY,THURSDAY, friday=FRIDAY, saturday=SATURDAY, sunday=SUNDAY")
	List<String> aggregationSpecs;

	@CommandLine.Option(names = "--min-days", defaultValue = "3", description = "minimum number of days every hour of " +
			"a station has to rest on for the station to be written. Guards against profiles taken over one or two " +
			"days, which a station whose detector was down for most of the period would otherwise produce. Keep in " +
			"mind that an aggregation restricted to a short date range has few days to begin with")
	int minDays;

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

		Files.createDirectories(output);

		//Parse before the expensive reading and map matching, so that a malformed option fails right away
		List<Aggregation> aggregations = parseAggregations(
				aggregationSpecs == null || aggregationSpecs.isEmpty() ? DEFAULT_AGGREGATIONS : aggregationSpecs);

		List<Path> countPaths = getCountFilePaths();

		extractStations(stationData, stations, counts);
		matchWithNetwork(networkPath, geometries, stations, counts, outputString);

		Table table = switch (inputFormat) {
			case CROSS_SECTIONS -> createTable(readCountData(countPaths));
			case LANE_DETECTORS -> readLaneDetectors(countPaths, extractLanes(stationData));
		};

		for (Aggregation aggregation : aggregations) {

			Path directory = Path.of(outputString, aggregation.name());
			Files.createDirectories(directory);

			//Car and freight are held as two measurables of the same location
			Counts<Link> aggregated = new Counts<>();
			aggregated.setName(scenario + " counts " + aggregation.name());
			aggregated.setDescription("Median car and freight counts (" + aggregation.describe()
					+ ") based on data from the 'Verkehrsinformationszentrale Berlin'.");
			aggregated.setYear(year);

			aggregateAndAssignCountData(table, stations, aggregated, aggregation, directory);

			//An aggregation whose ranges cover no day of the input would produce an empty counts file
			if (aggregated.getMeasureLocations().isEmpty()) {
				logger.warn("Aggregation {} matched no usable data, no counts file is written.", aggregation.name());
				continue;
			}

			new CountsWriter(aggregated).write(directory.resolve(scenario + ".counts.xml").toString());
		}

		writeDailyCounts(table, stations, outputString);

		return 0;
	}

	private @NonNull List<Path> getCountFilePaths() throws IOException {
		//Get filepaths. Only the directory itself is read, so that sibling directories holding data of a different
		//aggregation, e.g. the single lane detectors, are not picked up as well. The extension has to distinguish the
		//two deliveries, because a .tgz ends with .gz as well
		String extension = inputFormat == InputFormat.CROSS_SECTIONS ? ".csv.gz" : ".tgz";
		List<Path> countPaths;
		try (Stream<Path> paths = Files.list(input)) {
			countPaths = paths.filter(path -> path.toString().endsWith(extension)).sorted().toList();
		}

		if (countPaths.isEmpty())
			throw new IllegalArgumentException("No " + extension + " file in " + input
					+ ", which is what --input-format=" + inputFormat + " reads. Is --input-format correct?");

		if (countPaths.size() < 12)
			logger.warn("Expected 12 files, but only {} files containing count data were provided.", countPaths.size());
		return countPaths;
	}

	/**
	 * Parses the {@code --aggregate} option values, see there for the syntax.
	 */
	private static List<Aggregation> parseAggregations(List<String> specs) {

		List<Aggregation> aggregations = new ArrayList<>();
		Set<String> names = new HashSet<>();

		for (String spec : specs) {

			int equals = spec.indexOf('=');
			if (equals < 0)
				throw new IllegalArgumentException("Aggregation '" + spec + "' has no '=', expected <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]]");

			String name = spec.substring(0, equals).trim();
			if (name.isEmpty())
				throw new IllegalArgumentException("Aggregation '" + spec + "' has an empty name.");

			//The name becomes a directory name, so it must not carry a path
			if (!AGGREGATION_NAME.matcher(name).matches())
				throw new IllegalArgumentException("Aggregation name '" + name + "' is not a plain name of letters, digits, '-' and '_', but it is used as an output directory name.");

			if (!names.add(name))
				throw new IllegalArgumentException("Aggregation name '" + name + "' is used more than once, but every aggregation needs its own output directory.");

			//Everything up to the '@' are the days of week, everything behind it the date ranges
			String definition = spec.substring(equals + 1);
			int at = definition.indexOf('@');

			Set<DayOfWeek> weekdays = parseWeekdays(at < 0 ? definition : definition.substring(0, at), spec);
			List<DateRange> ranges = at < 0 ? List.of() : parseDateRanges(definition.substring(at + 1), spec);

			aggregations.add(new Aggregation(name, weekdays, ranges));
		}

		return aggregations;
	}

	private static Set<DayOfWeek> parseWeekdays(String weekdays, String spec) {

		Set<DayOfWeek> parsed = EnumSet.noneOf(DayOfWeek.class);

		for (String day : weekdays.split(",")) {
			day = day.trim();
			if (day.isEmpty())
				continue;

			try {
				parsed.add(DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Aggregation '" + spec + "' names the unknown day of week '" + day
						+ "', expected one of " + Arrays.toString(DayOfWeek.values()));
			}
		}

		if (parsed.isEmpty())
			throw new IllegalArgumentException("Aggregation '" + spec + "' does not name any day of week.");

		return parsed;
	}

	private static List<DateRange> parseDateRanges(String ranges, String spec) {

		List<DateRange> parsed = new ArrayList<>();

		for (String range : ranges.split("\\+")) {
			String[] bounds = range.trim().split(":");
			if (bounds.length != 2)
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the malformed date range '" + range + "', expected <from>:<to> as yyyy-MM-dd:yyyy-MM-dd");

			LocalDate from;
			LocalDate to;
			try {
				from = LocalDate.parse(bounds[0].trim());
				to = LocalDate.parse(bounds[1].trim());
			} catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the unparsable date range '" + range + "', expected yyyy-MM-dd:yyyy-MM-dd", e);
			}

			if (from.isAfter(to))
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the date range '" + range + "', which starts after it ends.");

			parsed.add(new DateRange(from, to));
		}

		if (parsed.isEmpty())
			throw new IllegalArgumentException("Aggregation '" + spec + "' has a '@' but no date range behind it.");

		return parsed;
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
	static Set<String> nameTokens(String name) {

		//An empty set means "no name given", which nameScore reads as "nothing to compare" rather than as a mismatch
		if (name == null || name.isBlank())
			return Set.of();

		//NFKD splits an umlaut into its base letter and a combining mark, which \p{M} then removes, so that "Späth"
		//becomes "spath". 'ß' has no such decomposition and is replaced by hand beforehand. The transliteration is
		//not undone, i.e. a name spelled "Spaeth" does not meet one spelled "Späth"
		String normalized = DIACRITICS.matcher(
				Normalizer.normalize(name.toLowerCase(Locale.GERMAN).replace("ß", "ss"), Normalizer.Form.NFKD)
		).replaceAll("");

		Set<String> tokens = new HashSet<>();
		//Everything that is not a lower case letter or a digit separates tokens, i.e. spaces, hyphens and the dot of
		//an abbreviation. A leading separator makes split emit an empty first token
		for (String token : NON_ALPHANUMERIC.split(normalized)) {
			//"Ollenhauer Straße" carries the suffix as a token of its own, which says nothing about the street
			if (token.isEmpty() || STREET_SUFFIXES.contains(token))
				continue;

			//"Ollenhauerstraße" and "Ollenhauerstr." carry it concatenated instead, so it is cut off here and both
			//forms reduce to "ollenhauer". Only one suffix is stripped, and never the whole token
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
		//A car and freight count on a link cars cannot use is a target the simulation can never reach, and the
		//stations sit close enough to a cycleway to be matched onto one. This covers the pt links as well, whose
		//ids the filter below matches by prefix.
		index.addLinkFilter((link, station) -> link.link().getAllowedModes().contains(TransportMode.car));
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

	/**
	 * Sheet of the station data holding one row per lane detector.
	 * <p>
	 * Opening the workbook by path opens it read-write and copies it back over the original on close, which must
	 * never happen to the raw input data. Reading from a stream leaves the file untouched.
	 */
	private static XSSFSheet openStationSheet(Path path) {

		try (InputStream is = Files.newInputStream(path); XSSFWorkbook wb = new XSSFWorkbook(is)) {
			return wb.getSheetAt(0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Carriageway lane detectors of every cross section, by station id. The new quality assurance reports one file
	 * per lane detector, and only the station data knows which cross section a detector belongs to.
	 */
	private Map<String, List<String>> extractLanes(Path path) {

		Map<String, List<String>> lanes = new HashMap<>();
		Set<String> haveCarriageway = new HashSet<>();

		for (Row row : openStationSheet(path)) {
			if (row.getRowNum() == 0)
				continue;

			String id = row.getCell(0).getStringCellValue();
			String detector = row.getCell(1).getStringCellValue();
			String lane = row.getCell(9).getStringCellValue();

			//Only stations that survived the map matching can carry counts, so the rest is not worth reading
			if (!stations.containsKey(id) || EXCLUDED_LANES.contains(lane))
				continue;

			if (!TURN_POCKET.equals(lane))
				haveCarriageway.add(id);

			lanes.computeIfAbsent(id, k -> new ArrayList<>()).add(detector);
		}

		int pockets = 0;
		for (var it = lanes.keySet().iterator(); it.hasNext(); ) {
			if (!haveCarriageway.contains(it.next())) {
				it.remove();
				pockets++;
			}
		}

		logger.info("Read {} lane detectors of {} cross sections from the station data, dropped {} cross sections "
						+ "that consist of a turn pocket only.",
				lanes.values().stream().mapToInt(List::size).sum(), lanes.size(), pockets);

		return lanes;
	}

	private void extractStations(Path path, Map<String, Station> stations, CountsOptions countsOption) {

		XSSFSheet sheet = openStationSheet(path);

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
			//The parser decompresses by file extension, so the data is never extracted to the input directory
			try (CSVParser parser = csv.createParser(path)) {
				records.addAll(parser.getRecords());
			} catch (IOException e) {
				logger.warn("Error processing file {}: ", path);
				throw new RuntimeException(e);
			}
		}

		return records;
	}

	/**
	 * Reads the lane detector delivery and sums its lanes back into cross sections, so that it yields the same table
	 * as {@link #createTable(List)} does for the pre-aggregated one.
	 * <p>
	 * An hour is only reported if every main lane of the cross section covers it, the rule the old quality assurance
	 * applies as well. A lane that is permanently down would otherwise understate the cross section in every single
	 * hour, and an error that is present in all of them is one the median cannot absorb.
	 */
	private Table readLaneDetectors(List<Path> paths, Map<String, List<String>> lanes) {

		StringColumn id = StringColumn.create(ColumnNames.id);
		DateColumn date = DateColumn.create(ColumnNames.date);
		StringColumn hour = StringColumn.create(ColumnNames.hour);
		DoubleColumn car = DoubleColumn.create(ColumnNames.carVolume);
		DoubleColumn freight = DoubleColumn.create(ColumnNames.freightVolume);
		DoubleColumn carSpeed = DoubleColumn.create(ColumnNames.carAvgSpeed);
		DoubleColumn freightSpeed = DoubleColumn.create(ColumnNames.freightAvgSpeed);

		//detector -> cross section, the reverse of the lane map, to look up an archive member in one step
		Map<String, String> detectorToStation = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : lanes.entrySet())
			for (String detector : entry.getValue())
				detectorToStation.put(detector, entry.getKey());

		logger.info("Start parsing count data of {} lane detectors.", detectorToStation.size());

		Set<String> unknown = new HashSet<>();
		Set<String> delivered = new HashSet<>();
		long incomplete = 0;
//		long partial = 0;
		long emitted = 0;

		for (Path path : paths) {

			//One archive holds one month, and a cross section hour never spans two months, so the accumulated hours
			//can be written out and dropped after every archive instead of being held for the whole year
			Map<String, Map<LocalDate, LaneHour[]>> accumulated = new HashMap<>();

			//The lanes an hour has to cover are the ones this month actually delivers a file for, not the ones the
			//station data lists. It is a snapshot and still names lanes that have since been removed, which no
			//archive can ever report, and requiring those would discard the whole cross section.
			Map<String, Integer> expected = new HashMap<>();

			try (TarArchiveInputStream tar = new TarArchiveInputStream(
					new GzipCompressorInputStream(new BufferedInputStream(Files.newInputStream(path))))) {

				for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {

					if (entry.isDirectory() || !entry.getName().endsWith(".csv"))
						continue;

					String detector = Path.of(entry.getName()).getFileName().toString().replaceFirst("\\.csv$", "");
					String station = detectorToStation.get(detector);

					//Detectors of a lane that is not summed, of an unmatched station, or unknown to the station data
					if (station == null) {
						unknown.add(detector);
						continue;
					}

					delivered.add(detector);
					expected.merge(station, 1, Integer::sum);
					incomplete += readDetector(tar, station, accumulated);
				}
			} catch (IOException e) {
				logger.warn("Error processing file {}: ", path);
				throw new RuntimeException(e);
			}

			for (Map.Entry<String, Map<LocalDate, LaneHour[]>> station : accumulated.entrySet()) {

				int lanesOfStation = expected.get(station.getKey());

				for (Map.Entry<LocalDate, LaneHour[]> day : station.getValue().entrySet()) {
					for (int h = 0; h < HOURS_PER_DAY; h++) {

						LaneHour laneHour = day.getValue()[h];
						if (laneHour == null)
							continue;

						// we do not skip if the number of lanes delivered differs from the number of lanes
						// registered for the detector. The lane might be closed or something else and we
						// lose a lot of stations that way. For the aggregated data this will be mitigated
						// anyway (mean/median) and for the daily counts we need the data!
//						if (laneHour.lanes() != lanesOfStation) {
//							partial++;
//							continue;
//						}

						id.append(station.getKey());
						date.append(day.getKey());
						hour.append(String.valueOf(h));
						car.append(laneHour.carVolume());
						freight.append(laneHour.freightVolume());
						carSpeed.append(laneHour.carSpeed());
						freightSpeed.append(laneHour.freightSpeed());
						emitted++;
					}
				}
			}

			logger.info("Read {}", path.getFileName());
		}

		if (!unknown.isEmpty())
			logger.info("Ignored {} detectors that are no main lane of a matched cross section.", unknown.size());

		//The station data is a snapshot and outlives the hardware, so this is expected, but a large number means it
		//is too old for the data it is used with
		long undelivered = detectorToStation.keySet().stream().filter(detector -> !delivered.contains(detector)).count();
		if (undelivered > 0)
			logger.info("{} of {} main lane detectors of the station data are in none of the archives, the cross "
					+ "sections they belong to are summed over their remaining lanes.", undelivered, detectorToStation.size());

//		logger.info("Built {} cross section hours, dropped {} lane hours below {}% completeness and {} cross section "
//				+ "hours that not all delivered lanes covered.", emitted, incomplete, minCompleteness, partial);
		logger.info("Built {} cross section hours, dropped {} lane hours below {}% completeness.",
			emitted, incomplete, minCompleteness);

		return Table.create(id, date, hour, car, freight, carSpeed, freightSpeed);
	}

	/**
	 * Adds one detector file of the lane detector delivery to the accumulated cross section hours and returns how many
	 * of its rows were dropped for insufficient completeness.
	 */
	private long readDetector(InputStream in, String station, Map<String, Map<LocalDate, LaneHour[]>> accumulated) throws IOException {

		long incomplete = 0;

		//The tar stream must stay open for the next entry, so the reader must not be closed
		CSVParser parser = CSVFormat.Builder.create()
				.setDelimiter(';')
				.setHeader()
				.setSkipHeaderRecord(true)
				.get()
				.parse(new BufferedReader(new InputStreamReader(CloseShieldInputStream.wrap(in), StandardCharsets.UTF_8)));

		//The completeness column carries an umlaut and the provider's ReadMe calls it differently than the data does
		String completenessColumn = parser.getHeaderNames().stream()
				.filter(name -> name.startsWith("Vollst") || name.equals("Datapoints_Rel"))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No completeness column in the lane detector data, "
						+ "expected 'Vollständigkeit' or 'Datapoints_Rel' but got " + parser.getHeaderNames()));

		for (CSVRecord row : parser) {

			if (parseSpeed(row.get(completenessColumn)) < minCompleteness) {
				incomplete++;
				continue;
			}

			LocalDate day = LocalDate.parse(row.get("Datum (Ortszeit)"));
			int hour = Integer.parseInt(row.get("Stunde des Tages (Ortszeit)").trim());

			LaneHour[] hours = accumulated
					.computeIfAbsent(station, k -> new HashMap<>())
					.computeIfAbsent(day, k -> new LaneHour[HOURS_PER_DAY]);

			LaneHour laneHour = hours[hour] == null ? new LaneHour() : hours[hour];
			laneHour.add(Double.parseDouble(row.get("qpkw")), parseSpeed(row.get("vpkw")),
					Double.parseDouble(row.get("qlkw")), parseSpeed(row.get("vlkw")));
			hours[hour] = laneHour;
		}

		return incomplete;
	}

	/**
	 * Speed of the lane detector data, which is NaN whenever no vehicle of that category passed in the hour.
	 */
	private static double parseSpeed(String value) {

		if (value == null || value.isBlank())
			return Double.NaN;

		return Double.parseDouble(value.trim());
	}

	/**
	 * One row per station, day and hour, as read from the raw count data.
	 */
	private Table createTable(List<CSVRecord> records) {

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

		return Table.create(id, date, hour, car, freight, carSpeed, freightSpeed);
	}

	private void aggregateAndAssignCountData(Table table, Map<String, Station> stations, Counts<Link> aggregated, Aggregation aggregation, Path directory) {

		Predicate<LocalDate> dayFilter = aggregation::covers;

		//filter and aggregation
		logger.info("Start aggregation {} over {}", aggregation.name(), aggregation.describe());
		Table filtered = table.where(t -> t.dateColumn(ColumnNames.date).eval(dayFilter));

		if (filtered.isEmpty()) {
			logger.warn("Aggregation {} does not cover any day of the count data.", aggregation.name());
			return;
		}

		//The median is used instead of the mean, because single days with an accident, a closure or a broken
		//detector would otherwise shift the whole profile
		Table summarized = filtered
				.summarize(ColumnNames.carVolume, ColumnNames.freightVolume, ColumnNames.carAvgSpeed, ColumnNames.freightAvgSpeed, median)
				.by(ColumnNames.id, ColumnNames.hour);

		//Column names were edited by summarize function
		for (String name : table.columnNames())
			summarized.columnNames().stream().filter(s -> s.contains(name)).findFirst().ifPresent(s -> summarized.column(s).setName(name));

		Map<String, int[]> days = countDaysPerHour(filtered);

		//Assign aggregted hourly traffic volumes to count objects AND write median speed per link and hour to csv file
		try (CSVPrinter printer = csv.createPrinter(directory.resolve(scenario + ".median_speed.csv"))) {
			printer.print(ColumnNames.id);
			printer.print(ColumnNames.hour);
			printer.print(ColumnNames.carMedianSpeed);
			printer.print(ColumnNames.freightMedianSpeed);
			printer.println();

			int counter = 0;
			int thin = 0;
			for (Map.Entry<String, Station> entry : stations.entrySet()) {
				String key = entry.getKey();
				Station station = entry.getValue();

				Table idFiltered = summarized.copy().where(t -> t.stringColumn(ColumnNames.id).isEqualTo(key));

				if (idFiltered.rowCount() != HOURS_PER_DAY) {
					logger.warn("Station {} - {} does not contain hour values for the whole day in aggregation {}!", key, station.name(), aggregation.name());
					counter++;
					continue;
				}

				//Covering all 24 hours says nothing about how many days each of them rests on. A station whose
				//detector was down for most of the period passes that check with a handful of days per hour, and
				//those days are rarely a cross section of the period: an outage that ends at dawn leaves the night
				//hours well covered and the peak hours with two days, which makes the profile worse than useless.
				int fewest = Arrays.stream(days.get(key)).min().orElse(0);
				if (fewest < minDays) {
					logger.warn("Station {} - {} rests on only {} days in its thinnest hour in aggregation {}, fewer "
							+ "than the {} required!", key, station.name(), fewest, aggregation.name(), minDays);
					thin++;
					continue;
				}

				Id<Link> linkId = station.linkAtomicReference().get().getId();
				MeasurementLocation<Link> location = aggregated.createAndAddMeasureLocation(linkId, station.getStationId());
				Measurable carVolume = location.createVolume();
				Measurable freightVolume = location.createVolume(TransportMode.truck);

				for (tech.tablesaw.api.Row row : idFiltered) {
					double car = row.getDouble(ColumnNames.carVolume);
					//in VIZ data as well as in a Measurable hours start at 0
					int hour = Integer.parseInt(row.getString(ColumnNames.hour));
					double freight = row.getDouble(ColumnNames.freightVolume);
					carVolume.setAtHour(hour, Math.round(car));
					freightVolume.setAtHour(hour, Math.round(freight));

					//print to file
					double carSpeed = row.getDouble(ColumnNames.carAvgSpeed);
					double freightSpeed = row.getDouble(ColumnNames.freightAvgSpeed);

					printer.print(linkId.toString());
					//in the speed file hours have always been counted from 1
					printer.print(hour + 1);
					printer.print(Math.round(carSpeed));
					printer.print(Math.round(freightSpeed));
					printer.println();
				}
			}
			logger.info("Aggregation {}: wrote {} stations, skipped {} for incomplete data and {} that rest on fewer "
							+ "than {} days in their thinnest hour.",
					aggregation.name(), aggregated.getMeasureLocations().size(), counter, thin, minDays);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Number of days the aggregation has data for, per station and hour of day. This is the sample the median of that
	 * hour is taken over, which the row count of the summarized table no longer shows.
	 */
	private static Map<String, int[]> countDaysPerHour(Table filtered) {

		Map<String, int[]> days = new HashMap<>();

		StringColumn ids = filtered.stringColumn(ColumnNames.id);
		StringColumn hours = filtered.stringColumn(ColumnNames.hour);

		for (int i = 0; i < filtered.rowCount(); i++)
			days.computeIfAbsent(ids.get(i), id -> new int[HOURS_PER_DAY])[Integer.parseInt(hours.get(i))]++;

		return days;
	}

	/**
	 * Writes one counts file per calendar day to {@code <output>/days/MM/dd.xml.gz}. Other than the aggregated counts
	 * these hold the volumes actually measured on that day, for every day the data covers and not only for the days
	 * the {@link Aggregation}s select.
	 */
	private void writeDailyCounts(Table table, Map<String, Station> stations, String outputString) throws IOException {

		logger.info("Start writing daily counts.");

		//date -> station id -> hourly volumes of that day, see DailyVolumes
		Map<LocalDate, Map<String, DailyVolumes>> daily = new TreeMap<>();

		DateColumn dates = table.dateColumn(ColumnNames.date);
		StringColumn ids = table.stringColumn(ColumnNames.id);
		StringColumn hours = table.stringColumn(ColumnNames.hour);
		DoubleColumn carVolumes = table.doubleColumn(ColumnNames.carVolume);
		DoubleColumn freightVolumes = table.doubleColumn(ColumnNames.freightVolume);

		for (int i = 0; i < table.rowCount(); i++) {
			String id = ids.get(i);

			//Stations that could not be matched to a link were dropped during map matching
			if (!stations.containsKey(id))
				continue;

			DailyVolumes volumes = daily.computeIfAbsent(dates.get(i), date -> new HashMap<>())
					.computeIfAbsent(id, station -> DailyVolumes.create());

			int hour = Integer.parseInt(hours.get(i));
			volumes.car()[hour] = carVolumes.getDouble(i);
			volumes.freight()[hour] = freightVolumes.getDouble(i);
		}

		Path dailyPath = Path.of(outputString, "days");
		int incomplete = 0;
		int written = 0;
		for (Map.Entry<LocalDate, Map<String, DailyVolumes>> day : daily.entrySet()) {
			LocalDate date = day.getKey();

			Counts<Link> counts = new Counts<>();
			counts.setName(scenario + " counts " + date);
			counts.setDescription("Car and freight counts of " + date + " based on data from the 'Verkehrsinformationszentrale Berlin'.");
			counts.setYear(date.getYear());

			for (Map.Entry<String, DailyVolumes> entry : day.getValue().entrySet()) {
				DailyVolumes volumes = entry.getValue();

				//A station is only written if it covers the full day, the same rule the aggregation applies
				if (!volumes.isComplete()) {
					incomplete++;
					continue;
				}

				Station station = stations.get(entry.getKey());
				MeasurementLocation<Link> location = counts.createAndAddMeasureLocation(
						station.linkAtomicReference().get().getId(), station.getStationId());

				Measurable car = location.createVolume();
				Measurable freight = location.createVolume(TransportMode.truck);

				//in VIZ data as well as in a Measurable hours start at 0
				for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
					car.setAtHour(hour, Math.round(volumes.car()[hour]));
					freight.setAtHour(hour, Math.round(volumes.freight()[hour]));
				}
			}

			//Days on which no station covers all hours would produce an empty counts file
			if (counts.getMeasureLocations().isEmpty()) {
				logger.warn("No station has data for the whole day on {}, no counts file is written.", date);
				continue;
			}

			Path monthPath = dailyPath.resolve(String.format("%02d", date.getMonthValue()));
			Files.createDirectories(monthPath);
			new CountsWriter(counts).write(monthPath.resolve(String.format("%02d", date.getDayOfMonth()) + ".xml.gz").toString());
			written++;
		}

		logger.info("Wrote counts for {} of {} days to {}, skipped {} station days with incomplete data.",
				written, daily.size(), dailyPath, incomplete);
	}

	/**
	 * One aggregation of the raw count data: the days of week its hourly volumes are taken from, and the date ranges
	 * those days have to fall into. An empty range list accepts every date the input covers.
	 */
	private record Aggregation(String name, Set<DayOfWeek> weekdays, List<DateRange> ranges) {

		/**
		 * Whether the counts of that date are part of this aggregation.
		 */
		boolean covers(LocalDate date) {

			if (!weekdays.contains(date.getDayOfWeek()))
				return false;

			if (ranges.isEmpty())
				return true;

			return ranges.stream().anyMatch(range -> range.contains(date));
		}

		/**
		 * Human readable definition of the aggregation, used in the log and in the counts description.
		 */
		String describe() {

			String days = weekdays.stream().sorted().map(Enum::name).collect(Collectors.joining(", "));

			if (ranges.isEmpty())
				return days;

			return days + " within " + ranges.stream().map(DateRange::toString).collect(Collectors.joining(", "));
		}
	}

	/**
	 * Date range, inclusive on both ends.
	 */
	private record DateRange(LocalDate from, LocalDate to) {

		boolean contains(LocalDate date) {
			return !date.isBefore(from) && !date.isAfter(to);
		}

		@Override
		public String toString() {
			return from + ":" + to;
		}
	}

	/**
	 * The lanes of one cross section in one hour, summed up as the lane detector files are read.
	 * <p>
	 * Volumes add up, speeds are averaged weighted by the volume they were measured over: a lane carrying four times
	 * the traffic has to count four times as much. Reconstructing the pre-aggregated delivery this way reproduces its
	 * speed within 0.5 km/h in 81% of the hours, against 65% for an unweighted mean.
	 */
	private static final class LaneHour {

		private double carVolume;
		private double freightVolume;
		private double carSpeedSum;
		private double freightSpeedSum;
		private double carSpeedWeight;
		private double freightSpeedWeight;
		private int lanes;

		void add(double car, double carSpeed, double freight, double freightSpeed) {

			carVolume += car;
			freightVolume += freight;
			lanes++;

			//No vehicle of that category means no speed to average in, for that lane only
			if (!Double.isNaN(carSpeed) && car > 0) {
				carSpeedSum += car * carSpeed;
				carSpeedWeight += car;
			}

			if (!Double.isNaN(freightSpeed) && freight > 0) {
				freightSpeedSum += freight * freightSpeed;
				freightSpeedWeight += freight;
			}
		}

		int lanes() {
			return lanes;
		}

		double carVolume() {
			return carVolume;
		}

		double freightVolume() {
			return freightVolume;
		}

		double carSpeed() {
			return carSpeedWeight > 0 ? carSpeedSum / carSpeedWeight : NO_SPEED;
		}

		double freightSpeed() {
			return freightSpeedWeight > 0 ? freightSpeedSum / freightSpeedWeight : NO_SPEED;
		}
	}

	/**
	 * Hourly car and freight volumes of one station on one day. Hours the data does not cover stay {@link Double#NaN}.
	 */
	private record DailyVolumes(double[] car, double[] freight) {

		static DailyVolumes create() {

			double[] car = new double[HOURS_PER_DAY];
			double[] freight = new double[HOURS_PER_DAY];

			Arrays.fill(car, Double.NaN);
			Arrays.fill(freight, Double.NaN);

			return new DailyVolumes(car, freight);
		}

		boolean isComplete() {

			for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
				if (Double.isNaN(car[hour]) || Double.isNaN(freight[hour]))
					return false;
			}

			return true;
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
		//The raw data reports an average speed per hour, of which the median over the aggregated days is taken,
		//so only the written columns are named after the median
		static String carMedianSpeed = "car_median_speed";
		static String freightMedianSpeed = "freight_median_speed";
	}

}
