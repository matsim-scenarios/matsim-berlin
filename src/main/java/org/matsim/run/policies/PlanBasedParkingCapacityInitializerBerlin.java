package org.matsim.run.policies;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.contrib.parking.parkingsearchparameterization.ParkingCapacityInitializer;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/*
This is a hard copy of the PlanBasedParkingCapacityInitializer in org.matsim.core.mobsim.qsim.qnetsimengine.parking
with minor modifications of the logging messages for the Berlin scenario.
 */

public class PlanBasedParkingCapacityInitializerBerlin implements ParkingCapacityInitializer {

	private static final Logger log = LogManager.getLogger(PlanBasedParkingCapacityInitializerBerlin.class);
	private final Population population;
	private final Network network;
	private final Config config;


	@Inject
	PlanBasedParkingCapacityInitializerBerlin(Network network, Population population, Config config) {
		this.network = network;
		this.population = population;
		this.config = config;
	}

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize() {
		ZeroParkingCapacityInitializerBerlin zeroParkingCapacityInitializer = new ZeroParkingCapacityInitializerBerlin(network, config);
		Map<Id<Link>, ParkingInitialCapacity> capacity = zeroParkingCapacityInitializer.initialize();

		AtomicInteger warnCount = new AtomicInteger(0);

		population.getPersons().values().stream()
			.map(p -> PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(p.getSelectedPlan()))
			.filter(Objects::nonNull)
			.map(Activity::getLinkId)
			.collect(Collectors.groupingBy(l -> l, Collectors.counting()))
			.forEach((linkId, count) -> {
				capacity.compute(linkId, (l, p) -> {
					if (p == null) {
						if (warnCount.incrementAndGet() <= 3) {
							log.warn(
								"Link {} has no parking capacity defined. Assuming 0 capacity and {} initial parking spots.",
								l, count
							);
							if (warnCount.get() == 3) {
								log.warn("Further warnings of this type are suppressed.");
							}
						}
						return new ParkingInitialCapacity(0, count.intValue());
					}

					if (count > p.capacity()) {
						if (warnCount.incrementAndGet() <= 3) {
							log.warn(
								"Link {} has less parking capacity ({}) than estimated initial parking spots ({}).",
								l, p.capacity(), count
							);
							if (warnCount.get() == 3) {
								log.warn("Further warnings of this type are suppressed.");
							}
						}
					}
					return new ParkingInitialCapacity(p.capacity(), count.intValue());
				});
			});

		return capacity;
	}
}
