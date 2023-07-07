package org.matsim.dashboard.analysis;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.Formula;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.prepare.counts.CreateCountsFromGeoPortalBerlin;
import picocli.CommandLine;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.DataFrameReader;
import tech.tablesaw.io.ReaderRegistry;
import tech.tablesaw.selection.Selection;
import tech.tablesaw.table.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(name = "dtv", description = "analysis of simulated traffic volumes")
@CommandSpec(
		requireEvents = true,
		requireNetwork = true,
		requires = "dtv_berlin.csv",
		produces = {"mapping_overview.csv", "stations_per_road_type.csv", "day_quality_per_road_type.csv"}
)
public class DTVAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DTVAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);

	@CommandLine.Option(names = "--upper-limit", description = "upper limit for 'exact' estimation", defaultValue = "1.3")
	private double upperLimit;

	@CommandLine.Option(names = "--lower-limit", description = "lower limit for 'exact' estimation", defaultValue = "0.7")
	private double lowerLimit;

	private final double peakPercentage = 0.09;

	@Override
	public Integer call() throws Exception {

		Table dtvFromGeoPortal = new DataFrameReader(new ReaderRegistry()).csv(input.getPath("dtv_berlin.csv"));

		Set<Id<Link>> matched = dtvFromGeoPortal.stringColumn("to_link").append(dtvFromGeoPortal.stringColumn("from_link"))
				.asList().stream().map(Id::createLinkId).collect(Collectors.toSet());

		Network filtered;
		{
			Network network = input.getNetwork();

			NetworkFilterManager manager = new NetworkFilterManager(network, new NetworkConfigGroup());
			manager.addLinkFilter(l -> matched.contains(l.getId()));

			filtered = manager.applyFilters();
		}

		createMappingOverviewFiles(dtvFromGeoPortal, filtered.getLinks());
		dtvFromGeoPortal.intColumn("vol_car").setName("observed_daily_traffic_volume");

		//reading events file & create volumes
		EventsManager eventsManager = EventsUtils.createEventsManager();
		VolumesAnalyzer volume = new VolumesAnalyzer(3600, 86400, filtered, true);
		eventsManager.addHandler(volume);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, input.getEventsPath());
		eventsManager.finishProcessing();

		Table sim = createSimDtvTable(volume);

		Table toJoin = dtvFromGeoPortal.copy().removeColumns("from_link").dropWhere(dtvFromGeoPortal.stringColumn("to_link").isEmptyString())
				.joinOn("to_link", "link_id").leftOuter(sim);

		Table fromJoin = dtvFromGeoPortal.copy().removeColumns("to_link").dropWhere(dtvFromGeoPortal.stringColumn("to_link").isEmptyString())
				.joinOn("to_link", "link_id").leftOuter(sim);

		generateQualityLabel(List.of(toJoin, fromJoin));

		Table append = toJoin.append(fromJoin).removeColumns("vol_freight", "both_directions");

		Table valid = append.where(append.booleanColumn("is_valid").isTrue());

		Table dailyShare = valid.copy().summarize("station", AggregateFunctions.count).by("daily_quality");
		DoubleColumn shareColumn = normalizeColumnNames(dailyShare).intColumn("station").asDoubleColumn().divide(dailyShare.rowCount()).setName("share");
		dailyShare.addColumns(shareColumn).write().csv(output.getPath("day_quality_per_road_type.csv").toFile());

		Table peakShare = valid.copy().summarize("station", AggregateFunctions.count).by("peak_hour_quality");
		DoubleColumn peakShareColumn = normalizeColumnNames(peakShare).intColumn("station").asDoubleColumn().divide(peakShare.rowCount()).setName("share");
		peakShare.addColumns(shareColumn).write().csv(output.getPath("day_quality_per_road_type.csv").toFile());

		return 0;
	}

	private void generateQualityLabel(List<Table> tables) {

		for (Table table : tables) {

			for(Column<?> column: table.columns()) {
				if (column.name().contains("_link"))
					column.setName("link_id");

				if(column.name().contains("capacity"))
					column.setName("capacity");
			}

			DoubleColumn obsPeakHour = table.intColumn("vol_car").copy().multiply(peakPercentage).setName("observed_peak_hour_traffic_volume");

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

		Table simVolumes = Table.create(StringColumn.create("link_id"), DoubleColumn.create("simulated_peak_hour_traffic_volume"),
				DoubleColumn.create("simulated_daily_traffic_volume"));

		for (Id<Link> id : volume.getLinkIds()) {

			int[] volumesForLink = volume.getVolumesForLink(id);

			if (volumesForLink.length != 24)
				continue;

			//morning peak is during 8 and 9 am
			int peakVolume = volumesForLink[8];
			int dailyTrafficVolume = IntArrayList.of(volumesForLink).intStream().sum();

			Row row = simVolumes.appendRow();
			row.setString("link_id", id.toString());
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

			String toLink = row.getString("to_link");

			if (!toLink.isBlank()) {
				Link to = links.get(Id.createLinkId(toLink));
				toCapacity = to.getCapacity();
				type = RoadType.valueOf(NetworkUtils.getHighwayType(to));
			}

			String fromLink = row.getString("from_link");

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
