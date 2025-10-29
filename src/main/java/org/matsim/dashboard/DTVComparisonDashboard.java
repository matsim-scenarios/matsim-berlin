package org.matsim.dashboard;

import org.matsim.analysis.DTVAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/**
 * Dashboard to compare dtv quality.
 */
public class DTVComparisonDashboard implements Dashboard {

	private final String dtvPath;

	public DTVComparisonDashboard(String dtvPath) {
		this.dtvPath = dtvPath;
	}

	@Override
	public double priority() {
		return -1;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "DTV";
		header.description = "Analysis of dtv data provided by 'FIS-Broker' (Open geo-data portal by Berlin). Error metrics: under: < 0.75; over: > 1.25";

		layout.row("overview")
			.el(Plotly.class, (viz, data) -> {

				viz.title = "DTV comparison";
				viz.description = "over all roads";

				Plotly.DataSet ds = viz.addDataset(data.compute(DTVAnalysis.class, "dtv_quality_per_road_type.csv", "--input-dtv", dtvPath))
					.aggregate(List.of("quality"), "n", Plotly.AggrFunc.SUM);

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.xAxis(Axis.builder().title("Number").build())
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(), ds.mapping()
					.x("n")
					.name("quality", ColorScheme.RdYlBu)
				);
			}).el(Plotly.class, (viz, data) -> {

				viz.title = "DTV comparison";
				viz.description = "by road type";

				Plotly.DataSet ds = viz.addDataset(data.compute(DTVAnalysis.class, "dtv_quality_per_road_type.csv", "--input-dtv", dtvPath));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("road_type")
					.y("share")
					.name("quality", ColorScheme.RdYlBu)
				);

			});

		layout.row("map")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Relative traffic volumes";
				viz.height = 8.0;

				viz.setShape(data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties"), "id");
				viz.addDataset("dtv", data.compute(DTVAnalysis.class, "dtv_comparison.csv", "--input-dtv", dtvPath));

				viz.center = data.context().getCenter();
				viz.zoom = data.context().getMapZoomLevel();

				viz.display.lineColor.dataset = "dtv";
				viz.display.lineColor.columnName = "quality";
				viz.display.lineColor.join = "link_id";
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 3, false);

				// 8px
				viz.display.lineWidth.dataset = "@8";
			});
	}
}
