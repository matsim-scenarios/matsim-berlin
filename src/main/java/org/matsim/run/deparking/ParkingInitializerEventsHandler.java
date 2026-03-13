package org.matsim.run.deparking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// This class is needed because we need to first determine the initial parking counts before we can track the parking events properly.
public class ParkingInitializerEventsHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private final Map<Id<Link>, Double> countByLink = new HashMap<>(600000);
	private final Map<String, Set<Id<Person>>> personsAlreadyTravelledByMode = new HashMap<>(600000);
	private final Set<String> parkingModes;

	public ParkingInitializerEventsHandler(Set<String> parkingModes) {
		this.parkingModes = parkingModes;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (ParkingAnalyzer.isPt(event.getLinkId())) {
			// Ignore pt links
			return;
		}

		if (!parkingModes.contains(event.getNetworkMode())) {
			// Other mode than parking mode => ignore
			return;
		}

		// No mass conservation needs to be taken into account because we only track whether an agent is already travelled with a mode or not.
		// We don't care if the last trip ended at the same link where the new trip starts.
		personsAlreadyTravelledByMode.putIfAbsent(event.getNetworkMode(), new HashSet<>());
		Set<Id<Person>> persons = personsAlreadyTravelledByMode.get(event.getNetworkMode());
		boolean alreadyTravelled = persons.remove(event.getPersonId());

		if (!alreadyTravelled) {
			// Vehicle entered traffic without having left before => A car was already parked.
			countByLink.putIfAbsent(event.getLinkId(), 0.);
			double count = countByLink.get(event.getLinkId());
			count++;
			countByLink.put(event.getLinkId(), count);
		}
		// Nothing else to do: A vehicle already registered as traveled is now entering traffic.
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (ParkingAnalyzer.isPt(event.getLinkId())) {
			// Ignore pt links
			return;
		}

		if (!parkingModes.contains(event.getNetworkMode())) {
			// Other mode than parking mode => ignore
			return;
		}

		personsAlreadyTravelledByMode.putIfAbsent(event.getNetworkMode(), new HashSet<>());
		Set<Id<Person>> ids = personsAlreadyTravelledByMode.get(event.getNetworkMode());
		boolean added = ids.add(event.getPersonId());

		if (!added) {
			throw new RuntimeException("Person " + event.getPersonId() + " is already en route with mode " + event.getNetworkMode());
		}
	}

	@Override
	public void reset(int iteration) {
		countByLink.clear();
		personsAlreadyTravelledByMode.clear();
	}

	public Map<Id<Link>, Double> getCountByLink() {
		return countByLink;
	}
}
