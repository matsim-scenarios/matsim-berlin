package org.matsim.dashboard;

import org.matsim.dashboard.analysis.DTVAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.Table;

public class DTVComparisonDashboard implements Dashboard {

	@Override
	public double priority() {
		return -1;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Daily traffic volume analysis dashboard";
		header.description = "Analysis of count data source provided by 'FIS-Broker' (Open geo-data portal by Berlin)";

		layout.row("first").el(PieChart.class, (viz, data) -> {

			viz.dataset = data.compute(DTVAnalysis.class, "stations_per_road_type.csv");
			viz.title = "Mapped count stations per road type";
			viz.height = 10.0;
		}).el(Table.class, (viz, data) -> {

			viz.dataset = data.compute(DTVAnalysis.class, "mapping_overview.csv");
			viz.enableFilter = false;
			viz.title = "Characteristics";
		});

		/*
		* TODO
		*  peak hour comparision
		*  whole day comaprision
		*  map plot for both comparision?
		* */
	}
}
