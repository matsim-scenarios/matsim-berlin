package org.matsim.dashboard.analysis;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.aggregate.NumericAggregateFunction;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "dtv", description = "analysis of simulated traffic volumes")
@CommandSpec(
		requireEvents = true,
		requireNetwork = true,
		requires = "dtv_berlin.csv",
		produces = {"mapping_overview.csv", "stations_per_road_type.csv", "day_quality_per_road_type.csv", "peak_quality_per_road_type.csv", "dtv_comparison.csv", "dtv_valid_and_quality.csv"}
)
public class DTVAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DTVAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(DTVAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(DTVAnalysis.class);

	@CommandLine.Option(names = "--upper-limit", description = "upper limit for 'exact' estimation", defaultValue = "1.3")
	private double upperLimit;

	@CommandLine.Option(names = "--lower-limit", description = "lower limit for 'exact' estimation", defaultValue = "0.7")
	private double lowerLimit;

	@CommandLine.Mixin
	private final CsvOptions csv = new CsvOptions();

	private final double peakPercentage = 0.09;

	@Override
	public Integer call() throws Exception {

		Table dtvFromGeoPortal = Table.read().csv(CsvReadOptions.builderFromFile(input.getPath("dtv_berlin.csv"))
				.columnTypesPartial(Map.of("from_link", ColumnType.TEXT, "to_link", ColumnType.TEXT))
				.build());

		Network network = input.getNetwork();

		createMappingOverviewFiles(dtvFromGeoPortal, network.getLinks());
		dtvFromGeoPortal.intColumn("vol_car").setName("observed_daily_traffic_volume");

		//reading events file & create volumes
		EventsManager eventsManager = EventsUtils.createEventsManager();
		VolumesAnalyzer volume = new VolumesAnalyzer(3600, 86400, network, true);
		eventsManager.addHandler(volume);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, input.getEventsPath());
		eventsManager.finishProcessing();

		Table sim = createSimDtvTable(volume);

		Table join = dtvFromGeoPortal.copy().joinOn("from_link").leftOuter(setIdColNameTo(sim, "from_link")).joinOn("to_link").leftOuter(setIdColNameTo(sim, "to_link"));

		join.removeColumns("vol_freight", "both_directions");

		Table valid = join.copy().where(join.booleanColumn("is_valid").isTrue());

		Table longTable = createLongTable(valid);

		generateQualityLabel(longTable);

		//write data frame for map plot
		log.info("Write long table to {}", output.getPath("dtv_comparison.csv").toString());
		writeMapPlotTable(join);

		log.info("Write daily estimation quality analysis to {}", output.getPath("day_quality_per_road_type.csv").toString());
		Table dailyShare = longTable.copy().countBy("daily_quality", "road_type");
		normalizeColumnNames(dailyShare).summarize("n", new NumericAggregateFunction("share") {
			@Override
			public Double summarize(NumericColumn<?> column) {
				return column.sum() / (double) column.size();
			}
		}).by("road_type").write().csv(output.getPath("day_quality_per_road_type.csv").toFile());

		log.info("Write peak hour estimation quality analysis to {}", output.getPath("peak_quality_per_road_type.csv").toString());
		Table peakShare = valid.copy().countBy("peak_hour_quality", "road_type");
		normalizeColumnNames(peakShare).summarize("n", new NumericAggregateFunction("share") {
			@Override
			public Double summarize(NumericColumn<?> column) {
				return column.sum() / (double) column.size();
			}
		}).by("road_type").write().csv(output.getPath("peak_quality_per_road_type.csv").toFile());

		return 0;
	}

	private Table createLongTable(Table table) {

		Table from = table.copy().selectColumns("from_link", "from_simulated_daily_traffic_volume", "road_type");
		Table to = table.copy().selectColumns("to_link", "to_simulated_daily_traffic_volume", "road_type");

		for(Table t: List.of(from, to)){
			for (Column<?> column : t.columns()) {
				String name = column.name();
				column.setName(name.replace("from_", "").replace("to_", ""));
			}
		}

		return from.append(to);
	}

	private void writeMapPlotTable(Table table) {

		try (CSVPrinter printer = csv.createPrinter(output.getPath("dtv_valid_and_quality.csv"))) {
			for (Row row : table) {

				String fromLink = row.getText("from_link");
				if (!fromLink.isBlank()) {
					Boolean valid = row.getBoolean("is_valid");
					String quality = row.getString("quality_from");

					printer.printRecord(fromLink, valid, quality);
				}

				String toLink = row.getText("to_link");
				if (!fromLink.isBlank()) {
					Boolean valid = row.getBoolean("is_valid");
					String quality = row.getString("quality_to");

					printer.printRecord(toLink, valid, quality);
				}
			}
		} catch (IOException e) {
			log.error("Error printing map plot table", e);
		}
	}

	private Table setIdColNameTo(Table sim, String colName) {

		Table copy = sim.copy();

		String direction = colName.split("_")[0];

		for (String name : copy.columnNames()) {

			Column<?> column = copy.column(name);
			if (name.equals("link_id")) {
				column.setName(colName);
			} else {
				String oldName = column.name();
				column.setName(direction + "_" + oldName);
			}
		}

		return copy;
	}

	private void generateQualityLabel(Table table) {

		DoubleColumn obsPeakHour = table.intColumn("simulated_daily_traffic_volume").copy().multiply(peakPercentage).setName("observed_peak_hour_traffic_volume");
		table.addColumns(obsPeakHour, StringColumn.create("peak_hour_quality"), StringColumn.create("daily_quality"));

		for (Row row : table) {

			double observedPeakHourTrafficVolume = row.getDouble("observed_peak_hour_traffic_volume");
			double observedDailyTrafficVolume = row.getDouble("observed_daily_traffic_volume");

			double simulatedPeakHourTrafficVolume = row.getDouble("simulated_peak_hour_traffic_volume");
			double simulatedDailyTrafficVolume = row.getDouble("simulated_daily_traffic_volume");

			String dayQuality = getEstimationQuality(observedDailyTrafficVolume, simulatedDailyTrafficVolume);
			String peakQuality = getEstimationQuality(observedPeakHourTrafficVolume, simulatedPeakHourTrafficVolume);

			row.setString("peak_hour_quality", peakQuality);
			row.setString("daily_quality", dayQuality);

		}
	}

	private String getEstimationQuality(double obs, double sim) {

		double upperVolume = upperLimit * obs;
		double lowerVolume = lowerLimit * obs;

		if (lowerVolume < sim && upperVolume >= sim)
			return "exact";

		if (sim <= lowerLimit)
			return "less";

		return "more";
	}

	private Table createSimDtvTable(VolumesAnalyzer volume) {

		Table simVolumes = Table.create(TextColumn.create("link_id"), DoubleColumn.create("simulated_peak_hour_traffic_volume"),
				DoubleColumn.create("simulated_daily_traffic_volume"));

		for (Id<Link> id : volume.getLinkIds()) {

			int[] volumesForLink = volume.getVolumesForLink(id);

			if (volumesForLink.length != 24)
				continue;

			//morning peak is during 8 and 9 am
			int peakVolume = volumesForLink[8];
			int dailyTrafficVolume = IntArrayList.of(volumesForLink).intStream().sum();

			Row row = simVolumes.appendRow();
			row.setText("link_id", id.toString());
			row.setInt("simulated_peak_volume_volume", peakVolume);
			row.setInt("day_volume_sim", dailyTrafficVolume);
		}

		return simVolumes;
	}

	private boolean isVolumeValid(double toCapacity, double fromCapacity, double avgCar) {

		if (toCapacity != -1 && fromCapacity != -1) {
			return avgCar / 1.5 * peakPercentage <= toCapacity;
		}

		if (toCapacity != -1) {
			return avgCar * peakPercentage <= toCapacity;
		}

		return avgCar * peakPercentage <= fromCapacity;
	}

	private Table normalizeColumnNames(Table table) {

		for (Column<?> c : table.columns()) {
			String name = c.name();

			int first = name.indexOf("[");
			int last = name.indexOf("]");

			if (first > -1 && last > -1) {
				c.setName(c.name().substring(first + 1, last));
			}
		}

		return table;
	}

	private void createMappingOverviewFiles(Table mapping, Map<Id<Link>, ? extends Link> links) {

		mapping.addColumns(DoubleColumn.create("to_capacity"), DoubleColumn.create("from_capacity"), BooleanColumn.create("both_directions"),
				StringColumn.create("road_type"), BooleanColumn.create("is_valid"));

		for (Row row : mapping) {

			RoadType type = RoadType.unknown;
			double toCapacity = -1;
			double fromCapacity = -1;

			String toLink = row.getText("to_link");

			if (!toLink.isBlank()) {
				Link to = links.get(Id.createLinkId(toLink));

				toCapacity = to.getCapacity();
				type = RoadType.valueOf(NetworkUtils.getHighwayType(to));
			}

			String fromLink = row.getText("from_link");

			if (!fromLink.isBlank()) {
				Link from = links.get(Id.createLinkId(fromLink));

				fromCapacity = from.getCapacity();

				RoadType fromType = RoadType.valueOf(NetworkUtils.getHighwayType(from));
				type = type.ordinal() > fromType.ordinal() ? type : fromType;

				row.setBoolean("both_directions", true);
			} else {
				row.setBoolean("both_directions", false);
			}

			row.setString("road_type", type.toString());
			row.setBoolean("is_valid", isVolumeValid(toCapacity, fromCapacity, row.getInt("vol_car")));
		}


		Table overview = Table.create(IntColumn.create("Number of stations"), IntColumn.create("Matched links"),
				IntColumn.create("Double matches"), IntColumn.create("Count volumes matching flow capacity"));

		Row row = overview.appendRow();
		row.setInt("Number of stations", mapping.rowCount());

		Selection directionSelection = mapping.booleanColumn("both_directions").isTrue();
		int doubleMatched = mapping.where(directionSelection).rowCount();
		row.setInt("Matched links", doubleMatched + mapping.rowCount());

		row.setInt("Double matches", doubleMatched);

		Selection validSelection = mapping.booleanColumn("is_valid").isTrue();
		int nValid = mapping.where(validSelection).rowCount();
		row.setInt("Count volumes matching flow capacity", nValid);

		log.info("Write overview table to {}", output.getPath("mapping_overview.csv"));
		overview.write().csv(output.getPath("mapping_overview.csv").toFile());

		log.info("Write overview table to {}", output.getPath("stations_per_road_type.csv"));
		Table perType = mapping.summarize("road_type", AggregateFunctions.count).by("road_type");
		normalizeColumnNames(perType).write().csv(output.getPath("stations_per_road_type.csv").toFile());

	}

	private enum RoadType {
		motorway,
		motorway_link,
		trunk,
		trunk_link,
		primary,
		primary_link,
		secondary,
		secondary_link,
		tertiary,
		residential,
		unknown
	}
}
