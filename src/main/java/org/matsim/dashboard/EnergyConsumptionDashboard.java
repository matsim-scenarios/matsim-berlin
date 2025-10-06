package org.matsim.dashboard;

import org.matsim.analysis.EnergyConsumptionAnalysis;
import org.matsim.application.options.ShpOptions;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Tile;
import picocli.CommandLine;

/**
 * Shows emission in the scenario.
 */
public class EnergyConsumptionDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Energy Consumption";
		header.description = "This dashboard indicates the energy consumption of cars assuming 100% electrification. \n" +
				" More specifically, all car trips are considered and an average consumption value is applied to estimate the total energy consumption. \n" +
				" For that, all person agents who conduct an activity of type 'home*' within a given area are considered residents of the area. \n" +
				" All person agents that conduct 1+ activities of any type within the given area are considered non-residents. \n" +
				" If no shape file was provided, ALL person agents are considered to be residents." +
				" Results are scaled to 100%.";

		layout.row("distance")
				.el(Tile.class, (viz, data) -> {
					viz.title = "Distance Statistics";
					viz.dataset = data.compute(EnergyConsumptionAnalysis.class, "distance_stats.csv");
					viz.height = 1.;
					viz.description = " Note that the values are scaled to 100%.";
				});

		layout.row("consumption")
				.el(Tile.class, (viz, data) -> {
					viz.title = "Energy Consumption Estimation";
					viz.dataset = data.compute(EnergyConsumptionAnalysis.class, "energy_consumption_stats.csv");
					viz.height = 1.;
					viz.description = "The energy consumption should not be directly translated into charging demand, as charging can also happen at other places (e.g. at work, depot, etc.). \n" +
							" Note that the values are scaled to 100% electrification and the average consumption value is configurable. \n" +
							" The estimation does not consider any temporal aspects (when cars are charged). \n" +
							" The estimation does not consider any spatial aspects (where cars are charged). \n" +
							" The estimation does not consider any vehicle types (e.g. vans, trucks, etc.). \n" +
							" The estimation does not consider any specific vehicle characteristics (e.g. battery size, efficiency, etc.). \n" +
							" The estimation does not consider any trip characteristics (e.g. speed, congestion, etc.). \n" +
							" The estimation assumes that all car trips are conducted by electric vehicles.";
				});

		layout.row("shape")
				.el(MapPlot.class, (viz, data) -> {
					viz.title = "Considered Area";
					viz.description = "Area determining residents and non-residents for the energy consumption estimation.";
					viz.setShape(data.compute(EnergyConsumptionAnalysis.class, "energy_consumption_area.gpkg"));
					viz.center = data.context().getCenter();
					viz.zoom = data.context().getMapZoomLevel();
					viz.display.fill.fixedColors = new String[]{"#e15759"}; //
					viz.height = 10.0;
				});

	}
}
