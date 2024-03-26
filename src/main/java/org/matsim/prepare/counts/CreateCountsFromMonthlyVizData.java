package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.opengis.referencing.operation.TransformException;
import picocli.CommandLine;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

	@CommandLine.Option(names = "--use-road-names", description = "use road names to filter map matching results")
	boolean roadNames;

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
		matchWithNetwork(networkPath, geometries, stations, counts);

		List<CSVRecord> records = readCountData(countPaths);
		aggregateAndAssignCountData(records, stations, car, freight, outputString);

		new CountsWriter(car).write(outputString + scenario + ".counts_car.xml");
		new CountsWriter(freight).write(outputString + scenario + ".counts_freight.xml");

		return 0;
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

	private void matchWithNetwork(Path networkPath, Path geometries, Map<String, Station> stations, CountsOptions countsOption) throws TransformException, IOException {

		Network network = NetworkUtils.readNetwork(networkPath.toString());
		CoordinateTransformation transformation = crs.getTransformation();

		Map<Id<Link>, Geometry> networkGeometries = NetworkIndex.readGeometriesFromSumo(geometries.toString(), IdentityTransform.create(2));
		NetworkIndex<Station> index = new NetworkIndex<>(network, networkGeometries, 50, toMatch -> {
			Coord coord = toMatch.coord();
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		});
		//Add link direction filter
		index.addLinkFilter((link, station) -> {
			String direction = station.direction();

			Coord from = link.link().getFromNode().getCoord();
			Coord to = link.link().getToNode().getCoord();

			String linkDir = "";
			if (to.getY() > from.getY()) {
				linkDir += "nord";
			} else
				linkDir += "süd";

			if (to.getX() > from.getX()) {
				linkDir += "ost";
			} else
				linkDir += "west";

			Pattern pattern = Pattern.compile(direction, Pattern.CASE_INSENSITIVE);
			return pattern.matcher(linkDir).find();
		});
		index.addLinkFilter(((link, station) -> !link.link().getId().toString().startsWith("pt_")));

		if (roadNames) {
			index.addLinkFilter((link, station) -> {
				String name = station.name().toLowerCase();

				if (name.endsWith("straße") || name.endsWith("str"))
					name.replace("straße", "").replace("str", "");

				if (name.equals("straße des 17. juni"))
					return true;

				Object linkRoadName = link.link().getAttributes().getAttribute("name");

				if (linkRoadName == null)
					return true;

				return Pattern.compile(name, Pattern.CASE_INSENSITIVE).matcher((String) linkRoadName).find();
			});
		}

		logger.info("Start matching stations to network.");
		int counter = 0;
		for (var it = stations.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Station> next = it.next();

			//Check for manual matching!
			Id<Link> manuallyMatched = countsOption.isManuallyMatched(next.getKey());
			if (manuallyMatched != null) {
				if (!network.getLinks().containsKey(manuallyMatched))
					throw new RuntimeException("Link " + manuallyMatched.toString() + " is not in the network!");
				Link link = network.getLinks().get(manuallyMatched);
				next.getValue().linkAtomicReference().set(link);
				index.remove(link);
				continue;
			}

			Link query = index.query(next.getValue());

			if (query == null) {
				counter++;
				it.remove();
				continue;
			}

			next.getValue().linkAtomicReference().set(query);
			index.remove(query);
		}

		logger.info("Could not match {} stations", counter);
	}

	private void extractStations(Path path, Map<String, Station> stations, CountsOptions countsOption) {

		XSSFSheet sheet;
		try (XSSFWorkbook wb = new XSSFWorkbook(path.toString())) {
			sheet = wb.getSheetAt(0);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
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
				throw new RuntimeException(e.getMessage());
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
			throw new RuntimeException(e.getMessage());
		}
	}

	private static class ColumnNames {
		static String id = "id";
		static String date = "date";
		static String hour = "hour";
		static String carVolume = "car_volume";
		static String carAvgSpeed = "car_avg_speed";
		static String freightVolume = "freight_volume";
		static String freightAvgSpeed = "freight_avg_speed";
	}

}
