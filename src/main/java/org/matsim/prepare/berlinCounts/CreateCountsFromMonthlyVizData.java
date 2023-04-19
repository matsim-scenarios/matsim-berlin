package org.matsim.prepare.berlinCounts;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOption;
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

	@CommandLine.Option(names = "--output", description = "output directory", defaultValue = "input/")
	Path output;

	@CommandLine.Option(names = "--scenario", description = "scenario name for output files", defaultValue = "berlin-v6.0")
	String scenario;

	@CommandLine.Option(names = "--year", description = "year of count data", defaultValue = "2022")
	int year;

	@CommandLine.Mixin
	private CsvOptions csvOptions = new CsvOptions();

	@CommandLine.Mixin
	private CrsOptions crsOptions = new CrsOptions();

	@CommandLine.Mixin
	CountsOption countsOption = new CountsOption();

	private final Map<String, Station> stations = new HashMap<>();
	private final Logger logger = LogManager.getLogger(CreateCountsFromMonthlyVizData.class);

	public static void main(String[] args) {
		new CreateCountsFromMonthlyVizData().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		String outputString = !output.toString().endsWith("/") || !output.toString().endsWith("\\") ? output + "/" : output.toString();

		Counts<Link> car = new Counts<>();
		car.setName(scenario + " car counts");
		car.setDescription("Car counts based on data from the 'Verkehrsinformationszentrale Berlin'.");
		car.setYear(year);
		Counts<Link> freight = new Counts<>();
		freight.setName(scenario + " freight counts");
		freight.setDescription("Freight counts based on data from the 'Verkehrsinformationszentrale Berlin'.");
		freight.setYear(year);

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
		extractStations(stationPath, stations, countsOption);
		matchWithNetwork(networkPath, stations, countsOption);

		List<CSVRecord> records = readCountData(countPaths);
		aggregateAndAssignCountData(records, stations, car, freight, outputString);

		new CountsWriter(car).write(outputString + scenario + ".counts_car.xml");
		new CountsWriter(freight).write(outputString + scenario + ".counts_freight.xml");

		return 0;
	}

	private void matchWithNetwork(Path networkPath, Map<String, Station> stations, CountsOption countsOption) {

		Network network = NetworkUtils.readNetwork(networkPath.toString());
		CoordinateTransformation transformation = crsOptions.getTransformation();
		NetworkIndex<Station> index = new NetworkIndex(network, 50, toMatch -> {
			Coord coord = ((Station) toMatch).coord();
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		});
		//Add link direction filter
		index.addLinkFilter((link, station) -> {
			String direction = station.direction();

			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();

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
		index.addLinkFilter(((link, station) -> !link.getId().toString().startsWith("pt_")));

		for (var it = stations.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Station> next = it.next();

			//Check for manual matching!
			Id<Link> manuallyMatched = countsOption.isManuallyMatched(next.getKey());
			if (manuallyMatched != null) {
				if (!network.getLinks().containsKey(manuallyMatched))
					throw new RuntimeException("Link {} is not in the network!");
				Link link = network.getLinks().get(manuallyMatched);
				next.getValue().linkAtomicReference().set(link);
				index.remove(link);
				continue;
			}

			Link query = index.query(next.getValue());

			if (query == null) {
				it.remove();
				continue;
			}

			next.getValue().linkAtomicReference().set(query);
			index.remove(query);
		}
	}

	private void extractStations(Path path, Map<String, Station> stations, CountsOption countsOption) {

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
			String name = row.getCell(5).getStringCellValue();
			String direction = row.getCell(8).getStringCellValue();
			double x = row.getCell(11).getNumericCellValue();
			double y = row.getCell(12).getNumericCellValue();

			if (countsOption.isIgnored(id))
				continue;

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

				List<CSVRecord> month = csvOptions.createParser(decompressed).getRecords();

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
			StringColumn id = StringColumn.create(ColumnNames.id.name());
			DateColumn date = DateColumn.create(ColumnNames.date.name());
			StringColumn hour = StringColumn.create(ColumnNames.hour.name());
			DoubleColumn car = DoubleColumn.create(ColumnNames.car_volume.name());
			DoubleColumn freight = DoubleColumn.create(ColumnNames.freight_volume.name());
			DoubleColumn carSpeed = DoubleColumn.create(ColumnNames.car_avg_speed.name());
			DoubleColumn freightSpeed = DoubleColumn.create(ColumnNames.freight_avg_speed.name());

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
		Table summarized = table.where(t -> t.dateColumn(ColumnNames.date.name()).eval(dayFilter))
				.summarize(ColumnNames.car_volume.name(), ColumnNames.freight_volume.name(), ColumnNames.car_avg_speed.name(), ColumnNames.freight_avg_speed.name(), mean)
				.by(ColumnNames.id.name(), ColumnNames.hour.name());

		//Column names were edited by summarize function
		for (String name : table.columnNames())
			summarized.columnNames().stream().filter(s -> s.contains(name)).findFirst().ifPresent(s -> summarized.column(s).setName(name));

		//Assign aggregted hourly traffic volumes to count objects AND write avg speed per link and hour to csv file
		try (CSVPrinter printer = csvOptions.createPrinter(Path.of(outputString + scenario + ".avg_speed.csv"))) {
			printer.print(ColumnNames.id);
			printer.print(ColumnNames.hour);
			printer.print(ColumnNames.car_avg_speed);
			printer.print(ColumnNames.freight_avg_speed);
			printer.println();

			int counter = 0;
			for (Map.Entry<String, Station> entry : stations.entrySet()) {
				String key = entry.getKey();
				Station station = entry.getValue();

				Table idFiltered = summarized.copy().where(t -> t.stringColumn(ColumnNames.id.name()).isEqualTo(key));

				if (idFiltered.rowCount() != 24) {
					logger.warn("Station {} does not contain hour values for the whole day!", key);
					counter++;
					continue;
				}

				Count<Link> carCount = carCounts.createAndAddCount(station.linkAtomicReference().get().getId(), key);
				Count<Link> freightCount = freightCounts.createAndAddCount(station.linkAtomicReference().get().getId(), key);

				for (tech.tablesaw.api.Row row : idFiltered) {
					double car = row.getDouble(ColumnNames.car_volume.name());
					//in VIZ data hours starts at 0, in MATSim count data starts at 1
					int hour = Integer.parseInt(row.getString(ColumnNames.hour.name())) + 1;
					double freight = row.getDouble(ColumnNames.freight_volume.name());
					carCount.createVolume(hour, Math.round(car));
					freightCount.createVolume(hour, Math.round(freight));

					//print to file
					double carSpeed = row.getDouble(ColumnNames.car_avg_speed.name());
					double freightSpeed = row.getDouble(ColumnNames.freight_avg_speed.name());

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

	private enum ColumnNames {
		id,
		date,
		hour,
		car_volume,
		car_avg_speed,
		freight_volume,
		freight_avg_speed
	}
}
