package org.matsim.run.dsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.NetworkDecomposition;
import org.matsim.run.Activities;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.util.List;

/**
 * Runs Berlin v7.1 with DSim, set up like {@code berlin_iterations_v7} of the parallel QSim in rust (repository parallel-qsim-berlin),
 * so that the runtimes of mobsim, replanning and scoring per iteration can be compared via the stopwatch ({@code *.stopwatch.csv}).
 * <p>
 * All settings that can be configured are in {@code input/v7.1/berlin-v7.1-*pct.dsim.config.xml}. This class only adds what the rust
 * binary does in code:
 * <ul>
 *     <li>Persons with a pt leg in any of their plans are removed, as pt is not simulated.</li>
 *     <li>Plans are scored by travel time only, see {@link OnlyTravelTimeScoringFunctionFactory}.</li>
 * </ul>
 * Unlike {@link OpenBerlinScenario}, no strategies, dashboards or analysis modules are added.
 */
@CommandLine.Command(header = ":: Open Berlin Scenario, DSim benchmark ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinDSimBenchmark extends MATSimApplication {

	private static final Logger log = LogManager.getLogger(OpenBerlinDSimBenchmark.class);

	@CommandLine.Option(names = "--threads", description = "Number of DSim threads (= partitions) and replanning threads. Overrides dsim.threads and global.numberOfThreads.")
	private Integer threads;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinDSimBenchmark.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		// Not scored, but used by the travel disutility of the router, as in OpenBerlinScenario
		Activities.addScoringParams(config, true);

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

		List<Id<Person>> ptUsers = scenario.getPopulation().getPersons().values().stream()
			.filter(p -> p.getPlans().stream()
				.flatMap(plan -> TripStructureUtils.getLegs(plan).stream())
				.anyMatch(leg -> TransportMode.pt.equals(leg.getRoutingMode())))
			.map(Person::getId)
			.toList();

		ptUsers.forEach(scenario.getPopulation()::removePerson);
		log.info("Removed {} persons using pt, {} persons remain.", ptUsers.size(), scenario.getPopulation().getPersons().size());

		checkPartitions(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new OpenBerlinScenario.TravelTimeBinding());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindScoringFunctionFactory().to(OnlyTravelTimeScoringFunctionFactory.class);
			}
		});
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
