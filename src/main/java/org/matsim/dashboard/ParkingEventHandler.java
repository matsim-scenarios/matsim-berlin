package org.matsim.dashboard;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleStartsParkingSearch;
import org.matsim.api.core.v01.events.handler.VehicleEndsParkingSearchEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleStartsParkingSearchEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParkingEventHandler implements VehicleStartsParkingSearchEventHandler, VehicleEndsParkingSearchEventHandler {
	Map<Id<Vehicle>, Double> startTimeByVehicle = new HashMap<>();
	List<Double> searchTimes = new ArrayList<>();
	Map<Id<Link>, Double> totalParkingSearchTimePerLink = new HashMap<>();
	Map<Id<Person>, Double> totalParkingSearchTimePerPerson = new HashMap<>();
	private final Map<Id<Person>, List<Double>> parkingSearchTimesPerPerson = new HashMap<>();
	private final Map<Id<Link>, List<Double>> parkingSearchTimesPerLink = new HashMap<>();

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

}
