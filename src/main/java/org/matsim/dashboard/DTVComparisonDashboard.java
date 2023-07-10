package org.matsim.dashboard;

import org.matsim.analysis.DTVAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.Table;

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

		header.title = "Daily traffic volume analysis dashboard";
		header.description = "Analysis of count data source provided by 'FIS-Broker' (Open geo-data portal by Berlin)";

		// TODO: dtvPath needs to be used in the args

		layout.row("mapping")
			.el(PieChart.class, (viz, data) -> {

				viz.dataset = data.compute(DTVAnalysis.class, "stations_per_road_type.csv");
				viz.title = "Mapped count stations per road type";
				viz.height = 10.0;
			}).el(Table.class, (viz, data) -> {

				viz.dataset = data.compute(DTVAnalysis.class, "mapping_overview.csv");
				viz.enableFilter = false;
				viz.title = "Characteristics";
			});

		layout.row("map")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Estimation quality";
				viz.height = 8.0D;
				viz.setShape(data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties"), "id");
				viz.addDataset("counts", data.compute(DTVAnalysis.class, "day_quality_per_road_type.csv"));
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.display.lineWidth.dataset = "@8";
			});

		layout.row("aggregation").el(Bar.class, (viz, data) -> {

		});

		/*
		 * TODO
		 *  peak hour comparision
		 *  whole day comaprision
		 *  map plot for both comparision?
		 * */
	}
}
