package org.matsim.run.deparking;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class ParkingEventHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private final ParkingInitializerEventsHandler initializer;
	private final Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> occupancyChangesByLink = new HashMap<>(600000);
	private Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> occupancyEntriesByLinkCache = null;

	private final Set<String> parkingModes;
	private final Map<String, Map<Id<Person>, Id<Link>>> lastParkingLinkByPersonAndMode = new HashMap<>(600000);

	private boolean initialized = false;

	public ParkingEventHandler(ParkingInitializerEventsHandler initializer, Set<String> parkingModes) {
		this.parkingModes = parkingModes;
		this.initializer = initializer;
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

		occupancyChangesByLink.putIfAbsent(event.getLinkId(), new LinkedList<>());
		var list = occupancyChangesByLink.get(event.getLinkId());
		list.add(new ParkingAnalyzer.OccupancyChange(event.getTime(), -1.));

		// We need to check mass conservation here. If a person leaves a link where he/she never parked, we need to remove the parking count at the last link.
		// This might happen if the activity locations are very close and the coord Distance in subtour mode choice is > 0.
		lastParkingLinkByPersonAndMode.putIfAbsent(event.getNetworkMode(), new HashMap<>());
		var linkByPerson = lastParkingLinkByPersonAndMode.get(event.getNetworkMode());
		Id<Link> lastLink = linkByPerson.get(event.getPersonId());

		if (lastLink != null && !lastLink.equals(event.getLinkId())) {
			// remove the parking at the last link
			List<ParkingAnalyzer.OccupancyChange> occupancyChanges = occupancyChangesByLink.get(lastLink);
			if (occupancyChanges == null) {
				throw new RuntimeException("No occupancy changes found for link " + lastLink + " while trying to remove a parking event.");
			}
			occupancyChanges.add(new ParkingAnalyzer.OccupancyChange(event.getTime(), -1.));
			linkByPerson.remove(lastLink);
		}
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

		// update history with initial occupancy if needed
		occupancyChangesByLink.putIfAbsent(event.getLinkId(), new ArrayList<>());
		var list = occupancyChangesByLink.get(event.getLinkId());
		list.add(new ParkingAnalyzer.OccupancyChange(event.getTime(), 1.));

		// track where this person last parked
		lastParkingLinkByPersonAndMode.putIfAbsent(event.getNetworkMode(), new HashMap<>());
		var linkByPerson = lastParkingLinkByPersonAndMode.get(event.getNetworkMode());
		linkByPerson.put(event.getPersonId(), event.getLinkId());
	}

	// This function is called at the beginning of each iteration before any other controller listener is called (i.e. before mobsim starts listener).
	@Override
	public void reset(int iteration) {
		occupancyChangesByLink.clear();
		lastParkingLinkByPersonAndMode.clear();
		occupancyEntriesByLinkCache = null;
		initialized = false;
	}

	/// This function can only be called after all events have been read. If called before, the behavior is undefined.
	Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> getOccupancyChangesByLink() {
		if (!initialized) {
			applyInitials();
			initialized = true;
		}
		return occupancyChangesByLink;
	}

	/// This function can only be called after all events have been read. If called before, the behavior is undefined.
	public Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> getOccupancyEntriesByLink() {
		if (!initialized) {
			applyInitials();
			initialized = true;
		}

		if (occupancyEntriesByLinkCache == null) {
			//fill cache if needed
			occupancyEntriesByLinkCache = new HashMap<>();
			for (var entry : occupancyChangesByLink.entrySet()) {
				occupancyEntriesByLinkCache.put(entry.getKey(), convert(entry.getValue()));
			}
		}
		return this.occupancyEntriesByLinkCache;
	}

	private void applyInitials() {
		initializer.getCountByLink().forEach((linkId, change) -> {
			occupancyChangesByLink.putIfAbsent(linkId, new LinkedList<>());
			var list = occupancyChangesByLink.get(linkId);
			list.addFirst(new ParkingAnalyzer.OccupancyChange(0, change));
		});
	}

	static List<ParkingAnalyzer.OccupancyEntry> convert(List<ParkingAnalyzer.OccupancyChange> occupancyChanges) {
		List<ParkingAnalyzer.OccupancyEntry> entries = new ArrayList<>();
		occupancyChanges.sort(Comparator.comparingDouble(ParkingAnalyzer.OccupancyChange::time));
		double currentOccupancy = 0.;
		double lastTime = 0.;

		for (ParkingAnalyzer.OccupancyChange change : occupancyChanges) {
			// in case of time 0, only the occupancy is added and no entry is created
			if (change.time() > lastTime) {
				entries.add(new ParkingAnalyzer.OccupancyEntry(lastTime, change.time(), currentOccupancy));
				lastTime = change.time();
			}
			currentOccupancy += change.change();
		}
		entries.add(new ParkingAnalyzer.OccupancyEntry(lastTime, Double.POSITIVE_INFINITY, currentOccupancy));
		return entries;
	}

	public static class Factory implements Provider<ParkingEventHandler> {
		@Inject
		private ParkingInitializerEventsHandler initializer;

		private Set<String> parkingModes;

		public Factory(Set<String> parkingModes) {
			this.parkingModes = parkingModes;
		}

		@Override
		public ParkingEventHandler get() {
			return new ParkingEventHandler(initializer, parkingModes);
		}
	}
}
