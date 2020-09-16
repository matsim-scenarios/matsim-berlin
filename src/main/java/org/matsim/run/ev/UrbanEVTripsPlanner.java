/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run.ev;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import one.util.streamex.StreamEx;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.util.StraightLineKnnFinder;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import javax.inject.Provider;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

class UrbanEVTripsPlanner implements MobsimInitializedListener {

	@Inject
	private Provider<TripRouter> tripRouterProvider;

	@Inject
	Scenario scenario;

	@Inject
	Vehicles vehicles;

	@Inject
	private SingleModeNetworksCache singleModeNetworksCache;

	@Inject
	private ElectricFleetSpecification electricFleetSpecification;

	@Inject
	private ChargingInfrastructureSpecification chargingInfrastructureSpecification;

	@Inject
	private DriveEnergyConsumption.Factory driveConsumptionFactory;

	@Inject
	private AuxEnergyConsumption.Factory auxConsumptionFactory;

	@Inject
	private Map<String, TravelTime> travelTimes;

	@Inject
	EvConfigGroup evConfigGroup;

	private QSim qsim;
	private EditTrips tripsEditor;
	private EditPlans plansEditor;

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		if(! (e.getQueueSimulation() instanceof QSim)){
			throw new IllegalStateException(UrbanEVTripsPlanner.class.toString() + " only works with a mobsim of type " + QSim.class);
		}
		//if we want to avoid retrieving and casting the qsim from the event, we probably have to convert this class into a MobimEngine..
		Set<Plan> selectedEVPlans = collectEVUserPlans();
		this.qsim = (QSim) e.getQueueSimulation();
		//editors
		this.tripsEditor = new EditTrips(tripRouterProvider.get(), scenario, null); //internal interface seems to be needed for pt only...
		this.plansEditor = new EditPlans(qsim, tripsEditor);
		processPlans(selectedEVPlans);
	}

	/**
	 * collect agents that use an EV at least once in their selected plan
	 * @return a map of personId to selected plan
	 */
	private Set<Plan> collectEVUserPlans() {
		return scenario.getPopulation().getPersons().values().parallelStream()
				.map(person -> person.getSelectedPlan())
				.filter(plan -> ! getEVLegs(plan).isEmpty())
				.collect(toSet());
	}

	/**
	 * retrieve all legs from a plan for which the registered vehicle is an EV.
	 * @param plan
	 * @return
	 */
	private Set<Leg> getEVLegs(Plan plan){
		return TripStructureUtils.getLegs(plan).stream()
				.filter(leg -> isEV(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())))
				.collect(Collectors.toSet());
	}

	private boolean isEV(Id<Vehicle> vehicleId) {
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
		VehicleType vType = vehicle.getType();
		return VehicleUtils.getHbefaTechnology(vType.getEngineInformation()).equals("electricity");
	}

	private void processPlans(Set<Plan> selectedEVPlans) {
		for (Plan plan : selectedEVPlans) {
			Set<Leg> evLegs = getEVLegs(plan);
			Preconditions.checkState(!evLegs.isEmpty(), "no ev legs found");
			//map vehicles to the list of legs they are used for
			Map<Vehicle, List<Leg>> vehicleToLegs = StreamEx.of(evLegs)
					.mapToEntry(leg -> vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())), leg -> leg)
					.grouping(toList());
			if(vehicleToLegs.size() > 1) throw new RuntimeException("person " + plan.getPerson().getId() + " uses more than one EV. Currently we can can handle that..");

			replan(plan, vehicleToLegs.entrySet().stream().findFirst().orElseThrow());
		}
	}

	private void replan(Plan plan, Map.Entry<Vehicle, List<Leg>> vehicle2Legs) {
		//create pseudo EVSpecification
		//create pseudo EV
		Vehicle vehicle = vehicle2Legs.getKey();
		ElectricVehicleSpecification electricVehicleSpecification = getElectricVehicleSpecification(vehicle);

		ElectricVehicle pseudoVehicle = ElectricVehicleImpl.create(electricVehicleSpecification, driveConsumptionFactory, auxConsumptionFactory,
				v -> charger -> {
					throw new UnsupportedOperationException();
				});

		double consumedEnergyThreshold = electricVehicleSpecification.getBatteryCapacity() * (0.8); //TODO randomize?

		//estimate consumptionPerLegPerLink
		//i do not use lambda syntax and streams here as we need to be sure that the order of legs is right

		double totalEnergyConsumption = 0;
		for (Leg leg : vehicle2Legs.getValue()) {
			double energyConsumption = estimateLegConsumptionPerLink(pseudoVehicle,leg).values().stream()
					.mapToDouble(Number::doubleValue)
					.sum();

			if (totalEnergyConsumption + energyConsumption >= consumedEnergyThreshold){

				Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(leg.getMode());

				//TODO replan here
				//plan charging trip
				int legIndex = plan.getPlanElements().indexOf(leg);
				Preconditions.checkState(legIndex > -1, "could not locate leg in plan");

				MobsimAgent mobsimagent = qsim.getAgents().get(plan.getPerson().getId());

				//find suitable non-stage activity before threshold passover
				//TODO possibly put behind interface
				int realActIndex = plansEditor.findIndexOfRealActBefore(mobsimagent, legIndex);
				Activity realAct = (Activity) WithinDayAgentUtils.getModifiablePlan(mobsimagent).getPlanElements().get(realActIndex);

//				if ( (realAct.getEndTime() - realAct.getStartTime()) < evConfigGroup.getMinimumChargeTime()) //TODO

				//
				ChargerSpecification selectedCharger = selectChargerNearToLink(realAct.getLinkId(), electricVehicleSpecification, modeNetwork);
				Link selectedChargerLink = modeNetwork.getLinks().get(selectedCharger.getLinkId());

//				Activity chargeAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(selectedChargerLink.getCoord(),
//						selectedChargerLink.getId(), leg.getMode() + "_plugin");
//				double maxPowerEstimate = Math.min(selectedCharger.getPlugPower(), pseudoEVSpecification.getBatteryCapacity() / 3.6);
//				double estimatedChargingTime = (pseudoEVSpecification.getBatteryCapacity() * 1.5) / maxPowerEstimate;
//				chargeAct.setMaximumDuration(Math.max(evConfigGroup.getMinimumChargeTime(), estimatedChargingTime));

				{//here is where the actual replanning takes place
					Activity plugInAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(selectedChargerLink.getCoord(),
							selectedChargerLink.getId(), leg.getMode() + UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER);
					plansEditor.insertActivity(mobsimagent, realActIndex - 1, plugInAct, leg.getMode(), TransportMode.walk);
					Activity plugOutAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(selectedChargerLink.getCoord(),
							selectedChargerLink.getId(), leg.getMode() + UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER);
					plansEditor.insertActivity(mobsimagent, realActIndex + 1, plugOutAct, TransportMode.walk, leg.getMode());
				}

				//now we would have to guess how much energy we could charge and reset energyConsumption
				//estimate energyCosumption including charging trip
				//TODO: check whether threshold is exceeded again!
			} else {
				totalEnergyConsumption += energyConsumption;
			}
		}

	}

	private ElectricVehicleSpecification getElectricVehicleSpecification(Vehicle vehicle) {
		ElectricVehicleSpecification electricVehicleSpecification = electricFleetSpecification.getVehicleSpecifications().get(Id.create(vehicle.getId(), ElectricVehicle.class));
		if(electricVehicleSpecification == null){
//			consider using ImmutableElectricVehicleSpecification.newBuilder()
			electricVehicleSpecification = new ElectricVehicleSpecification() {
				@Override public String getVehicleType() { return vehicle.getType().getId().toString(); }

				@Override public ImmutableList<String> getChargerTypes() {
					return EVUtils.getChargerTypes(vehicle.getType().getEngineInformation());
				}

				@Override public double getInitialSoc() { return EVUtils.getInitialEnergy(vehicle.getType().getEngineInformation()); }

				@Override public double getBatteryCapacity() { return VehicleUtils.getEnergyCapacity(vehicle.getType().getEngineInformation()); }

				@Override public Id<ElectricVehicle> getId() { return Id.create(vehicle.getId(), ElectricVehicle.class); }
			};
			electricFleetSpecification.addVehicleSpecification(electricVehicleSpecification);
		}
		return electricVehicleSpecification;
	}

	//possibly put behind interface
	private ChargerSpecification selectChargerNearToLink(Id<Link> linkId, ElectricVehicleSpecification vehicleSpecification, Network network){
		StraightLineKnnFinder<Link, ChargerSpecification> straightLineKnnFinder = new StraightLineKnnFinder<>(
				1, l -> l, s -> network.getLinks().get(s.getLinkId())); //TODO get closes 2 chargers and choose randomly?
		List<ChargerSpecification> nearestChargers = straightLineKnnFinder.findNearest(network.getLinks().get(linkId),
				chargingInfrastructureSpecification.getChargerSpecifications()
						.values()
						.stream()
						.filter(charger -> vehicleSpecification.getChargerTypes().contains(charger.getChargerType())));
		if (nearestChargers.isEmpty()){
			throw new RuntimeException("no charger could be found for vehicle type " + vehicleSpecification.getVehicleType());
		}
		return nearestChargers.get(0);
	}


	//TODO check whether we can simplify this by directly summing up consumption without the need of storing it in a Map..
	private Map<Link, Double> estimateLegConsumptionPerLink(ElectricVehicle ev, Leg basicLeg) {
		//retrieve mode specific network
		Network network = this.singleModeNetworksCache.getSingleModeNetworksCache().get(basicLeg.getMode());
		//retrieve routin mode specific travel time
		String routingMode = TripStructureUtils.getRoutingMode(basicLeg);
		TravelTime travelTime = this.travelTimes.get(routingMode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode " + routingMode + ".");
		}

		Map<Link, Double> consumptions = new LinkedHashMap<>();
		NetworkRoute route = (NetworkRoute)basicLeg.getRoute();
		List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());

		DriveEnergyConsumption driveEnergyConsumption = ev.getDriveEnergyConsumption();
		AuxEnergyConsumption auxEnergyConsumption = ev.getAuxEnergyConsumption();
		double lastSoc = ev.getBattery().getSoc();
		double linkEnterTime = basicLeg.getDepartureTime().seconds();
		for (Link l : links) {
			double travelT = travelTime.getLinkTravelTime(l, basicLeg.getDepartureTime().seconds(), null, null);

			double consumption = driveEnergyConsumption.calcEnergyConsumption(l, travelT, linkEnterTime)
					+ auxEnergyConsumption.calcEnergyConsumption(basicLeg.getDepartureTime().seconds(), travelT, l.getId());
			ev.getBattery().changeSoc(-consumption);
			double currentSoc = ev.getBattery().getSoc();
			// to accomodate for ERS, where energy charge is directly implemented in the consumption model
			double consumptionDiff = (lastSoc - currentSoc);
			lastSoc = currentSoc;
			consumptions.put(l, consumptionDiff);
			linkEnterTime += travelT;
		}
		return consumptions;
	}


}


