package org.matsim.dashboard;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleStartsParkingSearch;
import org.matsim.api.core.v01.events.handler.VehicleEndsParkingSearchEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleStartsParkingSearchEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

public class ParkingEventHandler implements VehicleStartsParkingSearchEventHandler, VehicleEndsParkingSearchEventHandler {
	Map<Id<Vehicle>, Double> startTimeByVehicle = new HashMap<>();
	List<Double> searchTimes = new ArrayList<>();
	Map<Id<Link>, Double> totalParkingSearchTimePerLink = new HashMap<>();
	Map<Id<Person>, Double> totalParkingSearchTimePerPerson = new HashMap<>();
	private final Map<Id<Person>, List<Double>> parkingSearchTimesPerPerson = new HashMap<>();
	private final Map<Id<Link>, List<Double>> parkingSearchTimesPerLink = new HashMap<>();
	private final List<ParkingEvent> parkingEvents = new ArrayList<>();

	@Override
	public void handleEvent(VehicleStartsParkingSearch event) {
		Double old = startTimeByVehicle.put(event.getVehicleId(), event.getTime());
		if (old != null) {
			throw new IllegalStateException("Vehicle " + event.getVehicleId() + " started parking search twice.");
		}
	}

	@Override
	public void handleEvent(VehicleEndsParkingSearch event) {
		Double startTime = startTimeByVehicle.remove(event.getVehicleId());
		if (startTime == null) {
			throw new IllegalStateException("Vehicle " + event.getVehicleId() + " ended parking search without starting it.");
		}
		searchTimes.add(event.getTime() - startTime);

		if(event.getLinkId() != null) {
			totalParkingSearchTimePerLink.merge(event.getLinkId(), event.getTime() - startTime, Double::sum);
			parkingSearchTimesPerLink
				.computeIfAbsent(event.getLinkId(), k -> new ArrayList<>())
				.add(event.getTime() - startTime);
		}

		if(event.getPersonId() != null) {
			totalParkingSearchTimePerPerson.merge(event.getPersonId(), event.getTime() - startTime, Double::sum);
		}
		if (event.getPersonId() != null) {
			parkingSearchTimesPerPerson
				.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>())
				.add(event.getTime() - startTime);
		}

		if (event.getPersonId() != null) {
			parkingEvents.add(
					new ParkingEvent(
							event.getPersonId(),
							event.getTime(),
							event.getTime() - startTime
					)
			);
		}

	}

	public Map<Double, Double> parkingSearchTimesDensity() {
		int total = searchTimes.size();
		return searchTimes.stream()
			.collect(Collectors.groupingBy(Double::doubleValue, Collectors.counting()))
			.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> e.getValue() / (double) total
			));
	}

	public ArrayList<Double> parkingSearchTimesList() {
		return new ArrayList<>(searchTimes);
	}

	public Map<Id<Link>, List<Double>> parkingSearchTimesPerLink() {
		return parkingSearchTimesPerLink;
	}

	public Map<Id<Person>, Double> totalParkingSearchTimePerPerson() {
		return totalParkingSearchTimePerPerson;
	}

	public Map<Id<Person>, List<Double>> parkingSearchTimesPerPerson() {
		return parkingSearchTimesPerPerson;
	}

	public Collection<ParkingEvent> parkingSearchAtTimeOfDay() {
		return parkingEvents;
	}


	class ParkingEvent {
		Id<Person> personId;
		double time;
		double searchTime;

		public ParkingEvent(Id<Person> personId, double time, double searchTime) {
			this.personId = personId;
			this.time = time;
			this.searchTime = searchTime;
		}
	}

}
