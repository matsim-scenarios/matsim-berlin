package org.matsim.dashboard;

import org.matsim.analysis.EnergyConsumptionAnalysis;
import org.matsim.application.options.ShpOptions;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Tile;
import picocli.CommandLine;

/**
 * Shows emission in the scenario.
 */
public class EnergyConsumptionDashboard implements Dashboard {

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();


	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Energy Consumption";
		header.description = "This dashboard indicates the energy consumption of cars assuming 100% electrification. <br>" +
				" More specifically, all car trips are considered and an average consumption value is applied to estimate the total energy consumption. <br>" +
				" For that, all person agents who conduct an activity of type 'home*' within a given area are considered residents of the area. <br>" +
				" All person agents that conduct 1+ activities of any type within the given area are considered non-residents. <br>" +
				" If no shape file was provided, ALL person agents are considered to be residents." +
				" Results are scaled to 100%.";

		String[] args;
//		if (shp.isDefined() != null) {
//			args = new ArrayList<>(List.of("--shp", this.shapeFile)).toArray(new String[0]);
//		} else {
//			args = new String[0];
//		}

		layout.row("stats")
				.el(Tile.class, (viz, data) -> {
//					viz.dataset = data.compute(EnergyConsumptionAnalysis.class, "energy_consumption_stats.csv", args);
					viz.dataset = data.compute(EnergyConsumptionAnalysis.class, "energy_consumption_stats.csv");

					viz.height = 0.1;
					viz.description = "ajkhdfjkahjkdhjahjhdfjkahdjhajkhdjhjahk";
				});

		if (shp.isDefined()) {
			layout.row("shape")
					.el(MapPlot.class, (viz, data) -> {
						viz.title = "Considered Area";
						viz.description = "Area determining residents and non-residents for the energy consumption estimation.";
						viz.setShape(shp.getShapeFile());
					});
		}
	}
}
