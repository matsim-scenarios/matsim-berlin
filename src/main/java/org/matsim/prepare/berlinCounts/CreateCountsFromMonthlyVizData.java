package org.matsim.prepare.berlinCounts;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	@CommandLine.Mixin
	CsvOptions csvOptions;

	Map<String, Station> stations = new HashMap<>();
	private final Logger logger = LogManager.getLogger(CreateCountsFromMonthlyVizData.class);

	@Override
	public Integer call() throws Exception {

		Stream<Path> files = Files.walk(input);
		Path stationData = files.filter(path -> path.endsWith(".csv")).findFirst().get();
		extractStations(stationData, stations);
		return 0;
	}

	private void extractStations(Path path, Map<String, Station> stations) {

		List<CSVRecord> records;
		try (CSVParser parser = csvOptions.createParser(path)) {
			records = parser.getRecords();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

		for (CSVRecord row : records) {
			String id = row.get(1);
			String name = row.get(6);
			String direction = row.get(9);
			double x = Double.parseDouble(row.get(13));
			double y = Double.parseDouble(row.get(12));

			Station station = new Station(id, name, direction, new Coord(x, y));
			stations.put(id, station);
		}
	}

	private void processCountData(List<Path> paths, Map<String, Station> stations) {

		//Read all files and build one record collection
		List<CSVRecord> records = new ArrayList<>();
		for (Path path : paths) {
			try {
				FileSystem fs = FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader());
				try (Stream<Path> stream = Files.walk(fs.getPath("/"))) {
					Optional<Path> opt = stream.filter(p -> !p.toString().equals("/")).findFirst();
					if (opt.isPresent()) {
						Path data = opt.get();
						records.addAll(csvOptions.createParser(data).getRecords());
					}
				}
			} catch (IOException e) {
				logger.warn("Error processing file {}: ", path.toString(), e);
				return;
			}
		}

		List<String> ids = records.stream().map(row -> row.get(1)).distinct().collect(Collectors.toList());
		Calendar cl = Calendar.getInstance();
		for (String id : ids) {

			if (!stations.containsKey(id))
				throw new RuntimeException("Station {} is in acutal count data, but not in the station data ...");

			Map<Integer, Double> mivVolumes = stations.get(id).miv();
			Map<Integer, Double> freightVolumes = stations.get(id).freight();

			List<CSVRecord> volumes = records.stream()
					.filter(row -> row.get(1).equals(id))
					.filter(row -> {
						List<Integer> date = Arrays.stream(row.get(2).split("\\.")).map(Integer::parseInt).collect(Collectors.toList());
						cl.set(date.get(2), date.get(1), date.get(0));
						int wd = cl.get(Calendar.DAY_OF_WEEK);
						return wd > 1 && wd < 5;
					})
					.collect(Collectors.toList());

			for (int i = 0; i < 24; i++) {
				int finalI = i;
				var volumeAtHourI = volumes.stream()
						.filter(row -> row.get(2).equals(String.valueOf(finalI)));

				Optional<Double> mivOpt = volumeAtHourI.map(row -> Double.parseDouble(row.get("q_pkw_mq_hr"))).reduce(Double::sum);
				double miv = mivOpt.orElse(0.0);

				Optional<Double> freightOpt = volumeAtHourI.map(row -> Double.parseDouble(row.get("q_pkw_mq_hr"))).reduce(Double::sum);
				double freight = freightOpt.orElse(0.0);

				mivVolumes.put(i, miv);
				freightVolumes.put(i, freight);
			}
		}
	}
}
