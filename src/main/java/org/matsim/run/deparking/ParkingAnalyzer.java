package org.matsim.run.deparking;

import com.github.luben.zstd.ZstdOutputStream;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

	private int iteration = -1;

	// This lock is unlocked after the mobsim has run and all events have been processed. This is because the event handlers need to finish to get correct results.
	private boolean lock = true;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private ParkingInitializerEventsHandler initializer;

	@Inject
	private ParkingEventHandler parkingEventHandler;

	@Inject
	private Config config;

	private Map<Integer, Map<Id<Link>, List<OccupancyEntry>>> occupancyEntriesByIteration = new HashMap<>();

	public ParkingAnalyzer() {
	}

	/**
	 * Main method to let this class be run on as standalone. It analyzes the parking occupancy of links based on the events of a MATSim run.
	 * It tracks when vehicles enter and leave traffic to determine when they are parked.
	 */
	public static void main(String[] args) {
		String events = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-policy/berlin-v6.4.output_events.xml.zst";
		String networkPath = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-policy/berlin-v6.4.output_network.xml.zst";
		String output = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-policy/parking_occupancy_autofrei-1pct.csv.zst";

//		String events = "output/deparking-debug/ITERS/it.9/berlin-v6.4.9.events.xml.zst";
//		String networkPath = "output/deparking-debug/berlin-v6.4.output_network.xml.zst";
//		String output = "output/deparking-debug/parking_occupancy_it9.csv.zst";

		// -1002247009


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

		return historicalOccupancy(iteration, linkId, from, to);
	}

	public List<OccupancyEntry> historicalOccupancy(int iteration, Id<Link> linkId, double from, double to) {
		Map<Id<Link>, List<OccupancyEntry>> occupancyEntriesByLink = occupancyEntriesByIteration.get(iteration);
		if (occupancyEntriesByLink == null) {
			log.error("Requested historical occupancy for iteration {}, but no data is available. Returning NaN.", iteration);
			throw new RuntimeException("No historical data for iteration " + iteration);
		}

		List<OccupancyEntry> occupancyEntries = occupancyEntriesByLink.getOrDefault(linkId, List.of());

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

	static boolean isPt(Id<Link> linkId) {
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
		if (event.getIteration() % ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class).getWriteInterval() == 0 || event.isLastIteration()) {
			log.info("Writing parking occupancy for iteration {}.", event.getIteration());
			Path file = Path.of(event.getServices().getControllerIO().getIterationFilename(event.getIteration(), "parking_occupancy.csv.zst"));
			writeAllRows(file, event.getServices().getScenario().getNetwork(), parkingEventHandler.getOccupancyEntriesByLink());
		}

		// deep copy parkingEventHandler.getOccupancyEntriesByLink()
		Map<Id<Link>, List<OccupancyEntry>> copy = new HashMap<>();
		for (var entry : parkingEventHandler.getOccupancyEntriesByLink().entrySet()) {
			copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		occupancyEntriesByIteration.put(event.getIteration(), copy);

		this.lock = false;
	}

	private static void writeMaxRows(Path file, Network network, Map<Id<Link>, List<OccupancyEntry>> occupancyEntries) {
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

	private static void writeAllRows(Path file, Network network, Map<Id<Link>, List<OccupancyEntry>> occupancyEntries) {
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

	public record OccupancyEntry(double fromTime, double toTime, double occupancy) {
	}

	public record OccupancyChange(double time, double change) {
	}

}

