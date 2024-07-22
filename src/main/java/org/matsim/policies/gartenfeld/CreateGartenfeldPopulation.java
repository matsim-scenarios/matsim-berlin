package org.matsim.policies.gartenfeld;

import org.matsim.application.MATSimAppCommand;
import org.matsim.application.prepare.population.*;
import org.matsim.prepare.population.CreateFixedPopulation;
import org.matsim.prepare.population.InitLocationChoice;
import org.matsim.prepare.population.RemoveUnavailableRoutes;
import org.matsim.prepare.population.RunActivitySampling;
import picocli.CommandLine;

@CommandLine.Command(name = "create-gartenfeld-population", description = "Create the population for the Gartenfeld scenario.")
public class CreateGartenfeldPopulation implements MATSimAppCommand {

	private final static String SVN = "../shared-svn/projects/matsim-germany";

	@CommandLine.Option(names = "--output", description = "Path to output population", defaultValue = "input/gartenfeld/gartenfeld-population-10pct.xml.gz")
	private String output;

	public static void main(String[] args) {
		new CreateGartenfeldPopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		new CreateFixedPopulation().execute(
			"--n", "7400",
			"--sample", "0.1",
			"--unemployed", "0.013",
			"--age-dist", "0.149", "0.203",
			"--facilities", "input/gartenfeld/DNG_residential.gpkg",
			"--prefix", "dng",
			"--output", output
		);

		new RunActivitySampling().execute(
			"--seed", "1",
			"--persons", "src/main/python/table-persons.csv",
			"--activities", "src/main/python/table-activities.csv",
			"--input", output,
			"--output", output
		);

		new InitLocationChoice().execute(
			"--input", output,
			"--output", output,
			"--k", "1",
			"--facilities", "input/v6.3/berlin-v6.3-facilities.xml.gz",
			"--network", "input/v6.3/berlin-v6.3-network.xml.gz",
			"--shp", SVN + "/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp",
			"--commuter", SVN + "/regionalstatistik/commuter.csv",
			"--commute-prob", "0.1",
			"--sample", "0.1"
		);

		new SplitActivityTypesDuration().execute(
			"--input", output,
			"--output", output,
			"--exclude", "commercial_start,commercial_end,freight_start,freight_end"
		);

		new SetCarAvailabilityByAge().execute(
			"--input", output,
			"--output", output
		);

		new CheckCarAvailability().execute(
			"--input", output,
			"--output", output
		);

		new FixSubtourModes().execute(
			"--input", output,
			"--output", output,
			"--coord-dist", "100"
		);

		// Merge with calibrated plans into one
		new MergePopulations().execute(
			output, "input/v6.3/berlin-v6.3-10pct.plans.xml.gz",
			"--output", output
		);

		new RemoveUnavailableRoutes().execute(
			"--input", output,
			"--network", "input/gartenfeld/gartenfeld-network.xml.gz",
			"--output", output
		);

		return 0;
	}
}
