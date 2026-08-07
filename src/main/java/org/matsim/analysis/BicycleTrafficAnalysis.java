package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;
import static tech.tablesaw.aggregate.AggregateFunctions.sum;

@CommandLine.Command(name = "traffic", description = "Calculates volumes and avg speed for bicycle/bike. However, there is some statistics for other modes, as this analysis was copied from TrafficAnalysis in matsim application." +
	"Please watch out when interpreting the results. vol_bike and avg_speed definitely are bike specific, the rest might not be.")
@CommandSpec(requireEvents = true, requireNetwork = true, requireRunDirectory = true,
	produces = {"traffic_stats_by_link_daily_bike.csv", "traffic_stats_by_link_and_hour_bike.csv", "traffic_stats_by_road_type_daily_bike.csv", "traffic_stats_by_road_type_and_hour_bike.csv"}
)
public class BicycleTrafficAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(BicycleTrafficAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(BicycleTrafficAnalysis.class);

	/**
	 * Sample size is used to compute actual flow capacity.
	 */
	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Mixin
	private ShpOptions shp;

//	@CommandLine.Option(names = "--transport-modes", description = "transport modes to analyze", split = ",")
	private Set<String> modes = Set.of(TransportMode.bike);

	public static void main(String[] args) {
		new BicycleTrafficAnalysis().execute(args);
	}

	private static Table normalizeColumns(Table table) {
		for (Column<?> c : table.columns()) {
			int start = c.name().indexOf("[");
			int end = c.name().indexOf("]");

			if (start > -1 && end > -1) {
				c.setName(c.name().substring(start + 1, end));
			}
		}

		return table;
	}

	@Override
	public Integer call() throws Exception {

		String vehiclesFileName = ApplicationUtils.globFile(input.getRunDirectory(), "*output_vehicles.xml.gz").toString();
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
		reader.readFile(vehiclesFileName);

		VehicleType bikeVehicleType = vehicles.getVehicleTypes().get(Id.create(TransportMode.bike, VehicleType.class));

		Network network = filterNetwork();

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		if (modes != null && !modes.isEmpty()) {
			builder.setFilterModes(true);
			builder.setAnalyzedModes(modes);
		}

		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(86400);
		builder.setTimeslice(900);

		TravelTimeCalculator travelTimes = builder.build();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 86400, network, true);

		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(travelTimes);
		manager.addHandler(volumes);

		manager.initProcessing();
		EventsUtils.readEvents(manager, input.getEventsPath());
		manager.finishProcessing();

		BicycleTrafficStatsCalculator calc = new BicycleTrafficStatsCalculator(network, travelTimes.getLinkTravelTimes(), volumes, 900,
			VehicleUtils.createVehicle(Id.createVehicleId("dummyBike"), bikeVehicleType));

		Table ds = createDataset(network, calc, volumes, bikeVehicleType);

//		List<String> means = List.of("excess_travel_time_index", "avg_speed",
//			"road_capacity_utilization", "lane_km");
		List<String> means = List.of("avg_speed", "lane_km", "avg_speed_km_h");
		Table dailyMean = normalizeColumns(ds.summarize(means, mean).by("link_id"));

		List<String> sums = ds.columnNames().stream().filter(s -> s.startsWith("vol_") || s.endsWith("_volume")).toList();
		Table dailySum = normalizeColumns(ds.summarize(sums, sum).by("link_id"));

		Table daily = dailyMean.joinOn("link_id").inner(dailySum);

		daily.write().csv(output.getPath("traffic_stats_by_link_daily_bike.csv").toFile());

		// Copy of table with all link links under one road_type
		Table copy = ds.copy();
		copy.stringColumn("road_type").set(Selection.withRange(0, ds.rowCount()), "all");
		copy.forEach(ds::append);

//		Table perRoadTypeAndHour = Table.create(StringColumn.create("road_type"), IntColumn.create("hour"),
//			DoubleColumn.create("excess_travel_time_index"));
		Table perRoadTypeAndHour = Table.create(StringColumn.create("road_type"), IntColumn.create("hour"));
		Set<String> roadTypes = new HashSet<>(ds.stringColumn("road_type").asList());

		for (int hour = 0; hour < 24; hour++) {

			for (String roadType : roadTypes) {

				double congestionIndex = calc.getNetworkCongestionIndex(hour * 3600, (hour + 1) * 3600, roadType.equals("all") ? null : roadType);
				double excessTravelTimeIndex = calc.getTomTomNetworkCongestionIndex(hour * 3600, (hour + 1) * 3600, roadType.equals("all") ? null : roadType);
				Row row = perRoadTypeAndHour.appendRow();
				row.setString("road_type", roadType);
				row.setInt("hour", hour);
//				row.setDouble("excess_travel_time_index", excessTravelTimeIndex);
//				row.setDouble("congestion_index", congestionIndex);
			}
		}

		perRoadTypeAndHour
			.sortOn("road_type", "hour")
			.write().csv(output.getPath("traffic_stats_by_road_type_and_hour_bike.csv").toFile());

//		Table dailyCongestionIndex = Table.create(StringColumn.create("road_type"), DoubleColumn.create("excess_travel_time_index"));
		Table dailyCongestionIndex = Table.create(StringColumn.create("road_type"));

		for (String roadType : roadTypes) {

			double congestionIndex = calc.getNetworkCongestionIndex(0, 86400, roadType.equals("all") ? null : roadType);
			double excessTravelTimeIndex = calc.getTomTomNetworkCongestionIndex(0, 86400, roadType.equals("all") ? null : roadType);
			Row row = dailyCongestionIndex.appendRow();
			row.setString("road_type", roadType);
//			row.setDouble("excess_travel_time_index", excessTravelTimeIndex);
//			row.setDouble("congestion_index", congestionIndex);
		}

//		Table perRoadType = dailyCongestionIndex.joinOn("road_type").leftOuter(
//			weightedMeanBy(ds, means, "road_type").rejectColumns( "excess_travel_time_index")
//		);
		Table perRoadType = dailyCongestionIndex.joinOn("road_type").leftOuter(
			weightedMeanBy(ds, means, "road_type")
		);

		DoubleColumn meanLaneKm = perRoadType.doubleColumn("lane_km").divide(24).multiply(1000).round().divide(1000).setName("lane_km");
		perRoadType.replaceColumn(meanLaneKm);

		perRoadType.column("lane_km").setName("Total lane km");
		perRoadType.column("road_type").setName("Road Type");
//		perRoadType.column("road_capacity_utilization").setName("Cap. Utilization");
		perRoadType.column("avg_speed").setName("Avg. Speed [m/s]");
		perRoadType.column("avg_speed_km_h").setName("Avg. Speed [km/h]");
//		perRoadType.column("congestion_index").setName("Congestion Index");
//		perRoadType.column("excess_travel_time_index").setName("Excess Travel Time Index");

		roundColumns(perRoadType);
		perRoadType
			.sortOn("Road Type")
			.write().csv(output.getPath("traffic_stats_by_road_type_daily_bike.csv").toFile());

		return 0;
	}

	private Table weightedMeanBy(Table table, List<String> aggr, String... by) {
		Table first = multiplyWithLinkLength(table).summarize(aggr, sum).by(by);
		return divideByLength(normalizeColumns(first));
	}

	private Table divideByLength(Table table) {

		Table copy = Table.create();
		for (Column<?> column : table.columns()) {

			if (column instanceof DoubleColumn d && !column.name().equals("lane_km")) {
				String name = column.name();
				DoubleColumn divided = d.divide(table.doubleColumn("lane_km")).setName(name);
				copy.addColumns(divided);
			} else
				copy.addColumns(column);
		}

		return copy;
	}

	private Table multiplyWithLinkLength(Table table) {

		Table copy = Table.create();

		for (Column<?> column : table.columns()) {

			if (column instanceof DoubleColumn d && !column.name().equals("lane_km")) {
				DoubleColumn multiplied = d.multiply(table.doubleColumn("lane_km")).setName(column.name());
				copy.addColumns(multiplied);
			} else
				copy.addColumns(column);
		}
		return copy;
	}

	private void roundColumns(Table table) {

		for (Column<?> column : table.columns()) {
			if (column instanceof DoubleColumn d) {
				d.set(Selection.withRange(0, d.size()), d.multiply(1000).round().divide(1000));
			}
		}
	}

	/**
	 * Create table with all disaggregated data.
	 */
	private Table createDataset(Network network, BicycleTrafficStatsCalculator calc, VolumesAnalyzer volumes, VehicleType bikeVehicleType) {

		Table all = Table.create(
			StringColumn.create("link_id"),
			IntColumn.create("hour"),
			StringColumn.create("road_type"),
			DoubleColumn.create("lane_km"),
//			DoubleColumn.create("excess_travel_time_index"),
			DoubleColumn.create("avg_speed"),
			DoubleColumn.create("avg_speed_km_h"),
//			DoubleColumn.create("road_capacity_utilization"),
			DoubleColumn.create("simulated_traffic_volume")
		);

		// Somehow Expensive operation
		Set<String> modes = volumes.getModes();

		for (String mode : modes) {
			all.addColumns(DoubleColumn.create("vol_" + mode));
		}

		for (Link link : network.getLinks().values()) {

			double[] vol = volumes.getVolumesPerHourForLink(link.getId());

			for (int h = 0; h < 24; h += 1) {
				Row row = all.appendRow();

				row.setString("link_id", link.getId().toString());
				row.setInt("hour", h);
				row.setString("road_type", NetworkUtils.getHighwayType(link));
				row.setDouble("lane_km", (link.getLength() * link.getNumberOfLanes()) / 1000);

				int startTime = h * 3600;
				int endTime = (h + 1) * 3600;

//				row.setDouble("excess_travel_time_index", calc.getLinkExcessTravelTimeIndex(link, startTime, endTime));

				double capacity = link.getCapacity() * sample.getSample();
//				row.setDouble("road_capacity_utilization", vol[h] / capacity);

				row.setDouble("simulated_traffic_volume", vol[h] / sample.getSample());

				double bikeVolume = 0.;
//				this check is not needed as we removed the run param for this.modes and made this.modes a final set. will keep it
//				if (this.modes.size() == 1 && this.modes.contains(TransportMode.bike)) {
					bikeVolume = volumes.getVolumesPerHourForLink(link.getId(), TransportMode.bike)[h];
//				}

				for (String mode : modes) {
					row.setDouble("vol_" + mode, volumes.getVolumesPerHourForLink(link.getId(), mode)[h] / sample.getSample());
				}

//				only calc avg speed for link at given time bin when there actually are bike traversing the link
				if (bikeVolume > 0) {
					// Beforehand, the avg_speed was in km/h. That was very confusing. I (paul) set to m/s. paul, jul '26.
					double avgSpeed = calc.getAvgSpeed(link, startTime, endTime);
					row.setDouble("avg_speed", avgSpeed);
					row.setDouble("avg_speed_km_h",
						BigDecimal.valueOf(avgSpeed * 3.6).setScale(2, RoundingMode.HALF_UP).doubleValue());
				} else {
//					if there is no bike traffic on the network, the usual approach sets avg_speed to the freespeed, which is much bigger than the usual bike speed
//					in order to see some congestion effects = lower bike speeds, we have to set the avg speed with no traffic on the link to the avg bike speed
//					as long as the maximum bike speed in the veh type is smaller than usual link (car) freespeeds, we can use this approach.
					double avgBikeSpeed = bikeVehicleType.getMaximumVelocity();
					if (link.getFreespeed() < avgBikeSpeed) {
						avgBikeSpeed = link.getFreespeed();
					}

					row.setDouble("avg_speed", avgBikeSpeed);
					row.setDouble("avg_speed_km_h",
						BigDecimal.valueOf(avgBikeSpeed * 3.6).setScale(2, RoundingMode.HALF_UP).doubleValue());
				}
			}
		}

		return all;
	}

	private Network filterNetwork() {

		Network unfiltered = input.getNetwork();
		NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());

		// Must contain one of the analyzed modes
		manager.addLinkFilter(l -> l.getAllowedModes().stream().anyMatch(s -> modes.contains(s)));

		if (shp.isDefined()) {
			String crs = ProjectionUtils.getCRS(unfiltered);
			ShpOptions.Index index = shp.createIndex(crs != null ? crs : shp.getShapeCrs(), "_");
			manager.addLinkFilter(l -> index.contains(l.getCoord()));
		}

		return manager.applyFilters();
	}
}
