package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVParser;
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
import picocli.CommandLine;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.io.InputStream;
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
			"monday=MONDAY",
			"midweek=TUESDAY,WEDNESDAY,THURSDAY",
			"friday=FRIDAY",
			"saturday=SATURDAY",
			"sunday=SUNDAY"
	);

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	/**
	 * An aggregation name is used as an output directory name, so it must not contain a path separator.
	 */
	private static final Pattern AGGREGATION_NAME = Pattern.compile("[A-Za-z0-9_-]+");

	@CommandLine.Option(names = "--input", description = "directory the monthly count data is read from, not searched recursively", required = true)
	Path input;

	@CommandLine.Option(names = "--stations", description = "station data of the count stations (xlsx)", required = true)
	Path stationData;

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

	@CommandLine.Option(names = "--aggregate", description = "one aggregation of the hourly counts, repeatable. " +
			"Syntax: <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]], e.g. " +
			"saturday=SATURDAY@2022-03-01:2022-05-31+2022-09-01:2022-10-31. Dates are inclusive and given as " +
			"yyyy-MM-dd, a day is used if it falls into any of the ranges. Without ranges the whole input period " +
			"is used. Each aggregation is written to its own output subdirectory. " +
			"Default: monday=MONDAY, midweek=TUESDAY,WEDNESDAY,THURSDAY, friday=FRIDAY, saturday=SATURDAY, sunday=SUNDAY")
	List<String> aggregationSpecs;

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

		//Get filepaths, count data is stored in .gz. Only the directory itself is read, so that sibling directories
		//holding data of a different aggregation, e.g. the single lane detectors, are not picked up as well
		List<Path> countPaths;
		try (Stream<Path> paths = Files.list(input)) {
			countPaths = paths.filter(path -> path.toString().endsWith(".gz")).toList();
		}

		if (countPaths.size() < 12)
			logger.warn("Expected 12 files, but only {} files containing count data were provided.", countPaths.size());

		extractStations(stationData, stations, counts);
		matchWithNetwork(networkPath, geometries, stations, counts, outputString);

		List<CSVRecord> records = readCountData(countPaths);
		Table table = createTable(records);

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

		//Opening the workbook by path opens it read-write and copies it back over the original on close, which must
		//never happen to the raw input data. Reading from a stream leaves the file untouched.
		XSSFSheet sheet;
		try (InputStream is = Files.newInputStream(path); XSSFWorkbook wb = new XSSFWorkbook(is)) {
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

		//Assign aggregted hourly traffic volumes to count objects AND write median speed per link and hour to csv file
		try (CSVPrinter printer = csv.createPrinter(directory.resolve(scenario + ".median_speed.csv"))) {
			printer.print(ColumnNames.id);
			printer.print(ColumnNames.hour);
			printer.print(ColumnNames.carMedianSpeed);
			printer.print(ColumnNames.freightMedianSpeed);
			printer.println();

			int counter = 0;
			for (Map.Entry<String, Station> entry : stations.entrySet()) {
				String key = entry.getKey();
				Station station = entry.getValue();

				Table idFiltered = summarized.copy().where(t -> t.stringColumn(ColumnNames.id).isEqualTo(key));

				if (idFiltered.rowCount() != HOURS_PER_DAY) {
					logger.warn("Station {} - {} does not contain hour values for the whole day in aggregation {}!", key, station.name(), aggregation.name());
					counter++;
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
			logger.info("Aggregation {}: wrote {} stations, skipped {}, because data was incomplete!",
					aggregation.name(), aggregated.getMeasureLocations().size(), counter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
