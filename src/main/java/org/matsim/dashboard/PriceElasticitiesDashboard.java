package org.matsim.dashboard;

import org.matsim.analysis.PriceElasticitiesAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/** A single comparison dashboard at the experiment root; it does not register any default dashboards. */
public final class PriceElasticitiesDashboard implements Dashboard {

	private final List<String> groups;
	private final List<String> modes;

	public PriceElasticitiesDashboard(PriceElasticitiesAnalysis.Result results) {
		this.groups = List.copyOf(results.groups());
		this.modes = results.rows().stream().map(PriceElasticitiesAnalysis.Row::mode).distinct().sorted().toList();
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {
		header.title = "Price elasticities";
		header.description = "Trips of the person subpopulation, excluding trucks. Demographic bins match the Berlin Trips dashboard. " +
			"Elasticity = relative change in trips / relative change in cost, including cross-price effects. " +
			"Car distance and daily costs scaled together use their common percentage change. " +
			"Tables show absolute simulated trip counts and changes; missing attributes appear as Unknown.";
		for (int i = 0; i < groups.size(); i++) {
			String group = groups.get(i);
			boolean overview = group.equals(PriceElasticitiesAnalysis.ALL);
			String tab = overview ? "Overview" : switch (group) {
				case "age" -> "Age";
				case "income" -> "Income";
				case "employment" -> "Employment";
				case "economic_status" -> "Economic status";
				case "zone" -> "Zone";
				default -> group;
			};
			String file = PriceElasticitiesAnalysis.DIRECTORY + "/group-" + i + ".csv";
			if (overview) {
				layout.row("trips-" + i, tab).el(Plotly.class, (viz, data) -> {
					chart(viz, "Base and experiment trips", "Transport mode", "Simulated trips");
					var ds = viz.addDataset(data.output(file));
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
						ds.mapping().x("Mode").y("Trips").name("Scenario"));
				});
				layout.row("elasticity-" + i, tab).el(Plotly.class, (viz, data) -> {
					chart(viz, "Price elasticities by transport mode", "Price change", "Elasticity");
					var ds = viz.addDataset(data.output(file)).filter("Status", "OK");
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
						ds.mapping().x("Scenario").y("Elasticity").name("Mode"));
				});
				layout.row("response-" + i, tab).el(Plotly.class, (viz, data) -> {
					chart(viz, "Change in trips relative to base", "Price change", "Trip change [%]");
					var ds = viz.addDataset(data.output(file)).filter("Run type", "Experiment");
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
						ds.mapping().x("Scenario").y("Trip change [%]").name("Mode"));
				});
			} else {
				for (String mode : modes) {
					layout.row("elasticity-" + i + "-" + mode, tab).el(Plotly.class, (viz, data) -> {
						String name = mode.equals("pt") ? "PT" : Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
						chart(viz, name + " trip elasticity by " + tab.toLowerCase(java.util.Locale.ROOT),
							tab, "Elasticity");
						viz.description = "Each series is a car or PT price experiment. Other demand modes show cross-price responses. Click a legend entry to toggle it.";
						var ds = viz.addDataset(data.output(file)).filter("Mode", mode).filter("Status", "OK");
						viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
							ds.mapping().x("Group").y("Elasticity").name("Scenario"));
					});
				}
			}
			layout.row("table-" + i, tab).el(Table.class, (viz, data) -> {
				viz.title = "Absolute trip counts, changes and elasticities";
				viz.description = "Counts refer to simulated trips without sample expansion. Undefined elasticities are blank; see Status.";
				viz.dataset = data.output(file);
				viz.enableFilter = true;
				viz.showAllRows = false;
				viz.show = overview
					? List.of("Scenario", "Mode", "Base trips", "Trips", "Trip change [trips]", "Trip change [%]", "Elasticity", "Status")
					: List.of("Scenario", "Group", "Mode", "Base trips", "Trips", "Trip change [trips]", "Trip change [%]", "Elasticity", "Status");
			});
			if (overview) {
				layout.row("prices", tab).el(Table.class, (viz, data) -> {
					viz.title = "Applied monetary parameters";
					viz.description = "Saved scoring parameters for person agents. Negative values represent costs; car distance rates are converted from EUR/m to EUR/km.";
					viz.dataset = data.output(PriceElasticitiesAnalysis.DIRECTORY + "/prices.csv");
					viz.hide = List.of("Experiment");
					viz.showAllRows = true;
				});
			}
		}
	}

	private static void chart(Plotly viz, String title, String x, String y) {
		viz.title = title;
		viz.height = 6d;
		viz.layout = tech.tablesaw.plotly.components.Layout.builder()
			.barMode(tech.tablesaw.plotly.components.Layout.BarMode.GROUP)
			.xAxis(Axis.builder().title(x).type(Axis.Type.CATEGORY).build())
			.yAxis(Axis.builder().title(y).build()).build();
	}
}
