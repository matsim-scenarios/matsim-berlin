package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.DataFrameReader;
import tech.tablesaw.io.ReaderRegistry;
import tech.tablesaw.selection.Selection;

import java.util.Map;

@CommandSpec(
		requires = {"dtv_berlin.csv", "network.xml.gz"},
		produces = {"mapping_overview.csv", "stations_per_road_type.csv"}
)
public class DTVAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	InputOptions input = InputOptions.ofCommand(DTVAnalysis.class);

	@CommandLine.Mixin
	OutputOptions output = OutputOptions.ofCommand(DTVAnalysis.class);

	private final Logger logger = LogManager.getLogger(DTVAnalysis.class);

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();
		Table mapping = readCountMapping(input.getPath("counts.csv"), network);

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

		logger.info("Write overview table to {}", output.getPath("mapping_overview.csv"));
		overview.write().csv(output.getPath("mapping_overview.csv").toFile());

		Table perType = mapping.summarize("road_type", AggregateFunctions.count).apply();
		normalizeColumnNames(perType).write().csv(output.getPath("stations_per_road_type.csv").toFile());
		return 0;
	}

	private Table normalizeColumnNames(Table table){

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

	private Table readCountMapping(String path, Network network) {

		logger.info("Reading count mapping from {}", path);
		Map<Id<Link>, ? extends Link> links = network.getLinks();

		Table mapping = new DataFrameReader(new ReaderRegistry()).csv(path);

		mapping.addColumns(DoubleColumn.create("to_capacity"), DoubleColumn.create("from_capacity"), BooleanColumn.create("both_directions"),
				StringColumn.create("road_type"), BooleanColumn.create("is_valid"));

		for (Row row: mapping) {

			String toLink = row.getString("to_link");
			Link toLinkObj = links.get(Id.createLinkId(toLink));

			double toCapacity = toLinkObj.getCapacity();
			RoadType type = RoadType.valueOf(NetworkUtils.getHighwayType(toLinkObj));
			double fromCapacity = -1;

			String fromLink = row.getString("from_link");

			if (!fromLink.isBlank()) {
				Link fromLinkObj = links.get(Id.createLinkId(fromLink));
				fromCapacity = fromLinkObj.getCapacity();
				RoadType fromType = RoadType.valueOf(NetworkUtils.getHighwayType(fromLinkObj));

				type = type.ordinal() > fromType.ordinal() ? type : fromType;

				row.setString("road_type", type.toString());
				row.setBoolean("both_directions", true);
			} else {
				row.setString("road_type", type.toString());
				row.setBoolean("both_directions", false);
			}

			row.setBoolean("is_valid", isVolumeValid(0, toCapacity, fromCapacity));
		}

		return mapping;
	}

	public boolean isVolumeValid(double carVolume, double toCapacity, double fromCapacity) {

		double fraction;

		return (carVolume / 1.5) * 0.09 <= toCapacity;
	}

	private enum RoadType {
		motorway,
		trunk,
		primary,
		secondary,
		tertiary,
		residential,
		unknown
	}
}
