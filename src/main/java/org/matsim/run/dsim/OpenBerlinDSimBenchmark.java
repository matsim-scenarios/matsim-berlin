package org.matsim.run.dsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.NetworkDecomposition;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Runs Berlin v7.1 with DSim, set up like {@code berlin_iterations_v7} of the parallel QSim in rust (repository parallel-qsim-berlin),
 * to compare the runtimes and the results of both simulations.
 * <p>
 * The configs are in parallel-qsim-berlin ({@code input/v7.1/berlin-v7.1-*pct.{pt,nopt}.dsim.config.xml}) and read the same input files
 * as the rust configs there. The scoring parameters come from a config of their own ({@code --scoring-config}), which rust reads as well.
 * Plans are scored with the standard MATSim scoring and these parameters, without the Berlin scoring function, pseudo-random errors or
 * taste variations. Which persons are simulated is decided by the plans file alone, e.g. a file without pt users. Of each person, only
 * the selected plan is kept.
 * <p>
 * Unlike {@link OpenBerlinScenario}, no strategies, dashboards or analysis modules are added.
 */
@CommandLine.Command(header = ":: Open Berlin Scenario, DSim benchmark ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinDSimBenchmark extends MATSimApplication {

	@CommandLine.Option(names = "--threads", description = "Number of DSim threads (= partitions) and replanning threads. Overrides dsim.threads and global.numberOfThreads.")
	private Integer threads;

	@CommandLine.Option(names = "--scoring-config", required = true, description = "MATSim config with the scoringParameters sets of the scoring module, "
		+ "e.g. berlin-v7.1-1pct.scoring.xml in shared-svn. Read on top of the config.")
	private Path scoringConfig;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinDSimBenchmark.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		// Replaces the default scoring parameter set by the sets of the file. The scoring parameters are also used by the travel
		// disutility of the router.
		new ConfigReader(config).readFile(scoringConfig.toString());

		// Needed for the bicycle travel time of the router
		ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

		if (threads != null) {
			config.dsim().setThreads(threads);
			config.global().setNumberOfThreads(threads);
		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		// only the selected plans are kept, as in berlin_iterations_v7
		scenario.getPopulation().getPersons().values().forEach(PersonUtils::removeUnselectedPlans);
		checkPartitions(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		controler.addOverridingModule(new OpenBerlinScenario.TravelTimeBinding());
	}

	/**
	 * With partitioning 'none', the partitions are read from the network (e.g. the networks partitioned for the rust runs). Their number must
	 * match the number of threads, DSim does not check this.
	 */
	private static void checkPartitions(Scenario scenario) {

		DSimConfigGroup dsim = scenario.getConfig().dsim();
		if (dsim.getPartitioning() != DSimConfigGroup.Partitioning.none)
			return;

		int threads = dsim.getThreads() > 0 ? dsim.getThreads() : Runtime.getRuntime().availableProcessors();
		int maxPartition = -1;
		for (Node node : scenario.getNetwork().getNodes().values()) {
			Object partition = node.getAttributes().getAttribute(NetworkDecomposition.PARTITION_ATTR_KEY);
			if (partition == null)
				throw new IllegalArgumentException("dsim.partitioning is 'none', but node " + node.getId() + " has no partition attribute. Use a partitioned network.");

			maxPartition = Math.max(maxPartition, (int) partition);
		}

		if (maxPartition + 1 != threads)
			throw new IllegalArgumentException("The network has " + (maxPartition + 1) + " partitions, but DSim runs with " + threads + " threads.");
	}
}
