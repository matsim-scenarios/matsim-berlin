package org.matsim.prepare.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.util.Set;

@CommandLine.Command(name = "remove-unavailable-routes", description = "Remove routes from the population that are not available in the network.")
public class RemoveUnavailableRoutes implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private String input;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private String networkPath;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private String output;

	private Network network;

	@Override
	public Integer call() throws Exception {

		network = NetworkUtils.readNetwork(networkPath);

		Population population = PopulationUtils.readPopulation(input);

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		PopulationUtils.writePopulation(population, output);

		return 0;
	}

	@Override
	public void run(Person person) {

		Set<Id<Link>> allLinks = network.getLinks().keySet();

		for (Plan plan : person.getPlans()) {
			for (Leg leg : TripStructureUtils.getLegs(plan.getPlanElements())) {
				Route route = leg.getRoute();

				if (route == null)
					continue;

				if (route instanceof NetworkRoute nr) {
					if (!allLinks.containsAll(nr.getLinkIds()))
						leg.setRoute(null);
				}

				if (!allLinks.contains(route.getStartLinkId()) || !allLinks.contains(route.getEndLinkId()))
					leg.setRoute(null);
			}

			for (Activity act : TripStructureUtils.getActivities(plan.getPlanElements(), TripStructureUtils.StageActivityHandling.StagesAsNormalActivities)) {
				if (act.getLinkId() != null && !allLinks.contains(act.getLinkId()))
					act.setLinkId(null);

			}
		}
	}
}
