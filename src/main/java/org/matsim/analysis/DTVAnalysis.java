package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import java.io.BufferedReader;
import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "dtv", description = "analysis of simulated traffic volumes")
@CommandSpec(
	requireEvents = true,
	requireNetwork = true,
	produces = {"dtv_comparison.csv", "dtv_quality_per_road_type.csv"}
)
public class DTVAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(DTVAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(DTVAnalysis.class);

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions();

	@CommandLine.Option(names = "--input-dtv", description = "Path to the dtv matched file", required = true)
	private String dtvPath;

	public static void main(String[] args) {
		new DTVAnalysis().execute(args);
	}

	private static double sqv(double simulated, double observed, double scale) {
		// 1/(1 + np.sqrt(((y_pred-y_true)*(y_pred-y_true))/(scaling_factor*y_true)))

		return 1 / (1 + Math.sqrt(Math.pow(simulated - observed, 2) / (scale * observed)));
	}

	@Override
	public Integer call() throws Exception {

		Table dtv;
		try (BufferedReader reader = IOUtils.getBufferedReader(dtvPath)) {
			dtv = Table.read().csv(CsvReadOptions.builder(reader)
				.columnTypesPartial(Map.of("from_link", ColumnType.STRING, "to_link", ColumnType.STRING))
				.build());
		}

		Network network = input.getNetwork();

		//reading events file & create volumes
		EventsManager eventsManager = EventsUtils.createEventsManager();
		VolumesAnalyzer volume = new VolumesAnalyzer(3600, 86400, network, false);
		eventsManager.addHandler(volume);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, input.getEventsPath());
		eventsManager.finishProcessing();

		Table sim = createSimDtvTable(dtv, network, volume);

		// Sort by quality
		List<String> labels = List.of("under", "ok", "over");
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString("quality")));

		sim = sim.sortOn(cmp);
		sim.write().csv(output.getPath("dtv_comparison.csv").toFile());

		Table aggr = sim.summarize("quality", count).by("quality", "road_type");

		aggr.doubleColumn("Count [quality]").setName("n");

		aggr.addColumns(DoubleColumn.create("share"));

		Set<String> roadTypes = aggr.stringColumn("road_type").asSet();
		// Norm within each road type
		for (String roadType : roadTypes) {
			DoubleColumn share = aggr.doubleColumn("n");
			Selection sel = aggr.stringColumn("road_type").isEqualTo(roadType);

			double total = share.where(sel).sum();
			if (total > 0)
				aggr.doubleColumn("share").set(sel, share.divide(total));
		}

		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("road_type")));

		aggr.write().csv(output.getPath("dtv_quality_per_road_type.csv").toFile());

		return 0;
	}

	private Table createSimDtvTable(Table dtv, Network network, VolumesAnalyzer volume) {

		dtv.addColumns(
			StringColumn.create("link_id"),
			StringColumn.create("road_type"),
			DoubleColumn.create("simulated_traffic_volume"),
			DoubleColumn.create("diff"),
			DoubleColumn.create("abs_error"),
			DoubleColumn.create("rel_error"),
			DoubleColumn.create("sqv"),
			StringColumn.create("quality")
		);

		for (Row row : dtv) {

			Id<Link> linkId = null;
			String fromLink = row.getString("from_link");
			String toLink = row.getString("to_link");

			double vol = 0;
			if (fromLink != null && !fromLink.isBlank()) {
				linkId = Id.createLinkId(fromLink);
				vol = sum(volume.getVolumesForLink(linkId)) * sample.getUpscaleFactor();
			}

			if (toLink != null && !toLink.isBlank()) {
				linkId = Id.createLinkId(toLink);
				vol += sum(volume.getVolumesForLink(linkId)) * sample.getUpscaleFactor();
			}

			row.setString("link_id", linkId.toString());
			row.setString("road_type", NetworkUtils.getHighwayType(network.getLinks().get(linkId)));
			row.setDouble("diff", vol - row.getInt("vol"));
			row.setDouble("simulated_traffic_volume", vol);
			row.setDouble("abs_error", Math.abs(vol - row.getInt("vol")));
			double relError = row.getDouble("abs_error") / row.getInt("vol");
			row.setDouble("rel_error", relError);

			row.setDouble("sqv", sqv(vol, row.getInt("vol"), 10_000));

			double rel = vol / row.getInt("vol");
			if (rel > 1.25)
				row.setString("quality", "over");
			else if (relError < 0.85)
				row.setString("quality", "under");
			else
				row.setString("quality", "ok");
		}

		return dtv;
	}

	private static double sum(int[] volumes) {
		return volumes == null ? 0 : Arrays.stream(volumes).sum();
	}
}
