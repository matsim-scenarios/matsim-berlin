package org.matsim.dashboard;

import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.HistogramTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

public class ParkingDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Parking";
		header.description = "Parking analysis";

		layout.row("parking-distribution").el(Plotly.class, (viz, data) -> {

			viz.title = "Parking search time distribution";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Time [s]").build())
				.yAxis(Axis.builder().title("Density").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.compute(ParkingAnalysis.class, "parking_search_times_density.csv"));

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE)
					.build(),
				ds.mapping()
					.x("search_time")
					.y("density")
			);

		});

		layout.row("parking-distribution").el(Plotly.class, (viz, data) -> {

			viz.title = "Parking capacity vs. initially used";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("# capacity").build())
				.yAxis(Axis.builder().title("# initially occupied").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.output("ITERS", "it.0", "(*.)?parking_initial_occupancy.csv"));

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.MARKERS)
					.build(),
				ds.mapping()
					.x("capacity")
					.y("occupancy")
			);

		});

		layout.row("parking-search-over-time").el(Plotly.class, (viz, data) -> {

			viz.title = "Parking search time by time of day";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Time of day [s]").build())
				.yAxis(Axis.builder().title("Search time [s]").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.compute(ParkingAnalysis.class, "parking_search_times_time_of_day.csv"));

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.MARKERS)
					.build(),
				ds.mapping()
					.x("time_of_day")
					.y("search_time")
			);

		});

		layout.row("parking-totals").el(Plotly.class, (viz, data) -> {

			viz.title = "Total parking search time per person";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Total search time [s]").build())
				.yAxis(Axis.builder().title("Persons").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.compute(ParkingAnalysis.class, "total_parking_search_time_per_person.csv"));

			viz.addTrace(HistogramTrace.builder(Plotly.INPUT)
					.histFunc(HistogramTrace.HistFunc.COUNT)
					.nBinsX(30)
					.build(),
				ds.mapping().x("total_parking_search_time")
			);

		});

		layout.row("parking-totals").el(Plotly.class, (viz, data) -> {

			viz.title = "Total parking search time per link";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Total search time [s]").build())
				.yAxis(Axis.builder().title("Links").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.compute(ParkingAnalysis.class, "total_parking_search_time_per_link.csv"));

			viz.addTrace(HistogramTrace.builder(Plotly.INPUT)
					.histFunc(HistogramTrace.HistFunc.COUNT)
					.nBinsX(30)
					.build(),
				ds.mapping().x("total_parking_search_time")
			);

		});

		layout.row("parking-search-map").el(MapPlot.class, (viz, data) -> {

			viz.title = "Total parking search time per link";
			viz.description = "Sum of parking search time recorded on each link.";
			viz.height = 12d;
			viz.center = data.context().getCenter();
			viz.zoom = data.context().getMapZoomLevel();

			viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro"), "id");
			viz.addDataset("parking", data.compute(ParkingAnalysis.class, "total_parking_search_time_per_link.csv"));

			viz.display.lineColor.dataset = "parking";
			viz.display.lineColor.columnName = "total_parking_search_time";
			viz.display.lineColor.join = "link_id";
			viz.display.lineColor.setColorRamp(ColorScheme.Oranges, 8, false);

			viz.display.lineWidth.dataset = "@8";

		});

		layout.row("parking-search-average-map").el(MapPlot.class, (viz, data) -> {

			viz.title = "Average parking search time per link";
			viz.description = "Mean parking search time recorded on each link.";
			viz.height = 12d;
			viz.center = data.context().getCenter();
			viz.zoom = data.context().getMapZoomLevel();

			viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro"), "id");
			viz.addDataset("parking", data.compute(ParkingAnalysis.class, "average_parking_search_time_per_link.csv"));

			viz.display.lineColor.dataset = "parking";
			viz.display.lineColor.columnName = "average_parking_search_time";
			viz.display.lineColor.join = "link_id";
			viz.display.lineColor.setColorRamp(ColorScheme.Oranges, 8, false);

			viz.display.lineWidth.dataset = "@8";

		});
	}
}
