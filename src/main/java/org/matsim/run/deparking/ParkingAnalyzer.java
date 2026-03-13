package org.matsim.run.deparking;

import com.github.luben.zstd.ZstdOutputStream;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.policies.autofrei.RunAutofreiPolicy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class ParkingAnalyzer implements IterationStartsListener, AfterMobsimListener {
	private static final Logger log = LogManager.getLogger(ParkingAnalyzer.class);

	private int writeInterval = 50;

	private int iteration = -1;

	// This lock is unlocked after the mobsim has run and all events have been processed. This is because the event handlers need to finish to get correct results.
	private boolean lock = true;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private ParkingInitializerEventsHandler initializer;

	@Inject
	private ParkingEventHandler parkingEventHandler;

	public ParkingAnalyzer(int writeInterval) {
		this.writeInterval = writeInterval;
	}

	public ParkingAnalyzer() {
	}

	/**
	 * Main method to let this class be run on as standalone. It analyzes the parking occupancy of links based on the events of a MATSim run.
	 * It tracks when vehicles enter and leave traffic to determine when they are parked.
	 */
	public static void main(String[] args) {
		String events = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended/berlin-v6.4.output_events.xml.zst";
		String networkPath = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended/berlin-v6.4.output_network.xml.zst";
		String output = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended/parking_occupancy_autofrei-1pct.csv.zst";

		ParkingEventHandler peh = run(events);
		ParkingAnalyzer.writeMaxRows(Path.of(output), NetworkUtils.readNetwork(networkPath), peh.getOccupancyEntriesByLink());
	}

	// convenience method to run the parking analyzer standalone
	public static ParkingEventHandler run(Consumer<EventsManager> readEvents) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		Set<String> modes = Set.of(TransportMode.car, TransportMode.truck, "freight", RunAutofreiPolicy.NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC);

		ParkingInitializerEventsHandler initializer = new ParkingInitializerEventsHandler(modes);
		eventsManager.addHandler(initializer);

		ParkingEventHandler parkingHandler = new ParkingEventHandler(initializer, modes);
		eventsManager.addHandler(parkingHandler);

		readEvents.accept(eventsManager);

		return parkingHandler;
	}

	// convenience method to run the parking analyzer standalone
	public static ParkingEventHandler run(String events) {
		return run((em) -> EventsUtils.readEvents(em, events));
	}

	/// Returns the occupancy of a link at a given time bin (from, to) in a given iteration. Both from and to are included.
	public List<OccupancyEntry> occupancy(int iteration, Id<Link> linkId, double from, double to) {
		if (lock) {
			log.error("Occupancy requested during locked state (probably before or during mobsim). Returning NaN.");
			throw new RuntimeException("ParkingAnalyzer is locked; occupancy data is not yet available.");
		}

		if (iteration != this.iteration) {
			log.error("Requested occupancy for iteration {}, but current iteration is {}. Returning NaN.", iteration, this.iteration);
			throw new RuntimeException("Iteration " + iteration + " is out of order");
		}

		List<OccupancyEntry> occupancyEntries = parkingEventHandler.getOccupancyEntriesByLink().getOrDefault(linkId, List.of());

		// filter entries to only those that overlap with [from, to]
		List<OccupancyEntry> list = getOccupancyEntriesInTimeBin(from, to, occupancyEntries);
		return list;
	}

	static List<OccupancyEntry> getOccupancyEntriesInTimeBin(double from, double to, List<OccupancyEntry> occupancyEntries) {
		List<OccupancyEntry> list = new ArrayList<>();
		for (OccupancyEntry o : occupancyEntries) {
			// there are 2 cases that we won't include: (1) entry is completely before 'from' and (2) entry is completely after 'to'
			if (o.toTime() <= from || o.fromTime() >= to) {
				continue;
			}
			// otherwise, we have some overlap; shrink the entry to fit into [from, to]
			double entryFrom = Math.max(o.fromTime(), from);
			double entryTo = Math.min(o.toTime(), to);
			list.add(new OccupancyEntry(entryFrom, entryTo, o.occupancy()));
		}
		return list;
	}

	private static boolean isPt(Id<Link> linkId) {
		String s = linkId.toString();
		return s.startsWith("pt_") || s.contains("_pt_");
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();
		this.lock = true;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		if (event.getIteration() % writeInterval == 0 || event.isLastIteration()) {
			log.info("Writing parking occupancy for iteration {}.", event.getIteration());
			Path file = Path.of(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parking_occupancy.csv.zst"));
			writeAllRows(file, event.getServices().getScenario().getNetwork(), parkingEventHandler.getOccupancyEntriesByLink());
		}
		this.lock = false;
	}

	static void writeMaxRows(Path file, Network network, Map<Id<Link>, List<OccupancyEntry>> occupancyEntries) {
		var rows = new ArrayList<List<String>>();

		for (var entries : occupancyEntries.entrySet()) {
			Id<Link> linkId = entries.getKey();

			OccupancyEntry max = entries.getValue().stream().max(Comparator.comparing(OccupancyEntry::occupancy)).orElseThrow();
			var row = List.of(
				linkId.toString(),
				String.valueOf(max.fromTime()),
				String.valueOf(max.toTime()),
				String.valueOf(network.getLinks().get(linkId).getLength()),
				String.valueOf(max.occupancy()),
				String.valueOf(entries.getValue().getFirst().occupancy)
			);
			rows.add(row);
		}

		writeRows(file, rows);
	}

	static void writeAllRows(Path file, Network network, Map<Id<Link>, List<OccupancyEntry>> occupancyEntries) {
		var rows = new ArrayList<List<String>>();

		for (var entries : occupancyEntries.entrySet()) {
			Id<Link> linkId = entries.getKey();

			for (OccupancyEntry entry : entries.getValue()) {
				var row = List.of(
					linkId.toString(),
					String.valueOf(entry.fromTime()),
					String.valueOf(entry.toTime()),
					String.valueOf(network.getLinks().get(linkId).getLength()),
					String.valueOf(entry.occupancy()),
					String.valueOf(entries.getValue().getFirst().occupancy)
				);
				rows.add(row);
			}
		}

		writeRows(file, rows);
	}

	private static void writeRows(Path file, ArrayList<List<String>> rows) {
		List<String> header = List.of("linkId", "from_time", "to_time", "length", "occupancy", "initial");
		// Use Apache Commons CSV to write the file with Zstandard compression
		try (var outputStream = java.nio.file.Files.newOutputStream(file);
			 var zstdOutputStream = new ZstdOutputStream(new BufferedOutputStream(outputStream));
			 var writer = new java.io.OutputStreamWriter(zstdOutputStream, java.nio.charset.StandardCharsets.UTF_8);
			 var csvPrinter = org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader(header.toArray(new String[0])).build().print(writer)) {
			for (var row : rows) {
				csvPrinter.printRecord(row);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ParkingEventHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private final ParkingInitializerEventsHandler initializer;
		private final Map<Id<Link>, List<OccupancyChange>> occupancyChangesByLink = new HashMap<>(600000);
		private Map<Id<Link>, List<OccupancyEntry>> occupancyEntriesByLinkCache = null;

		private final Set<String> parkingModes;
		private final Map<String, Map<Id<Person>, Id<Link>>> lastParkingLinkByPersonAndMode = new HashMap<>(600000);

		private boolean initialized = false;

		public ParkingEventHandler(ParkingInitializerEventsHandler initializer, Set<String> parkingModes) {
			this.parkingModes = parkingModes;
			this.initializer = initializer;
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			if (isPt(event.getLinkId())) {
				// Ignore pt links
				return;
			}

			if (!parkingModes.contains(event.getNetworkMode())) {
				// Other mode than parking mode => ignore
				return;
			}

			occupancyChangesByLink.putIfAbsent(event.getLinkId(), new LinkedList<>());
			var list = occupancyChangesByLink.get(event.getLinkId());
			list.add(new OccupancyChange(event.getTime(), -1.));

			// We need to check mass conservation here. If a person leaves a link where he/she never parked, we need to remove the parking count at the last link.
			// This might happen if the activity locations are very close and the coord Distance in subtour mode choice is > 0.
			lastParkingLinkByPersonAndMode.putIfAbsent(event.getNetworkMode(), new HashMap<>());
			var linkByPerson = lastParkingLinkByPersonAndMode.get(event.getNetworkMode());
			Id<Link> lastLink = linkByPerson.get(event.getPersonId());

			if (lastLink != null && !lastLink.equals(event.getLinkId())) {
				// remove the parking at the last link
				List<OccupancyChange> occupancyChanges = occupancyChangesByLink.get(lastLink);
				if (occupancyChanges == null) {
					throw new RuntimeException("No occupancy changes found for link " + lastLink + " while trying to remove a parking event.");
				}
				occupancyChanges.add(new OccupancyChange(event.getTime(), -1.));
				linkByPerson.remove(lastLink);
			}
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			if (isPt(event.getLinkId())) {
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
			list.add(new OccupancyChange(event.getTime(), 1.));

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
		Map<Id<Link>, List<OccupancyChange>> getOccupancyChangesByLink() {
			if (!initialized) {
				applyInitials();
				initialized = true;
			}
			return occupancyChangesByLink;
		}

		/// This function can only be called after all events have been read. If called before, the behavior is undefined.
		public Map<Id<Link>, List<OccupancyEntry>> getOccupancyEntriesByLink() {
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
				list.addFirst(new OccupancyChange(0, change));
			});
		}

		static List<OccupancyEntry> convert(List<OccupancyChange> occupancyChanges) {
			List<OccupancyEntry> entries = new ArrayList<>();
			occupancyChanges.sort(Comparator.comparingDouble(OccupancyChange::time));
			double currentOccupancy = 0.;
			double lastTime = 0.;

			for (OccupancyChange change : occupancyChanges) {
				// in case of time 0, only the occupancy is added and no entry is created
				if (change.time() > lastTime) {
					entries.add(new OccupancyEntry(lastTime, change.time(), currentOccupancy));
					lastTime = change.time();
				}
				currentOccupancy += change.change();
			}
			entries.add(new OccupancyEntry(lastTime, Double.POSITIVE_INFINITY, currentOccupancy));
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

	// This class is needed because we need to first determine the initial parking counts before we can track the parking events properly.
	public static class ParkingInitializerEventsHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private final Map<Id<Link>, Double> countByLink = new HashMap<>(600000);
		private final Map<String, Set<Id<Person>>> personsAlreadyTravelledByMode = new HashMap<>(600000);
		private final Set<String> parkingModes;

		public ParkingInitializerEventsHandler(Set<String> parkingModes) {
			this.parkingModes = parkingModes;
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			if (isPt(event.getLinkId())) {
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
			if (isPt(event.getLinkId())) {
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

	public record OccupancyEntry(double fromTime, double toTime, double occupancy) {
	}

	public record OccupancyChange(double time, double change) {
	}

	public static class ParkingAnalyzerFactory implements Provider<ParkingAnalyzer> {


		@Override
		public ParkingAnalyzer get() {
			return null;
		}
	}
}

