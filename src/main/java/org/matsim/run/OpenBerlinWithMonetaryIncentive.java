package org.matsim.run;

import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Run the {@link OpenBerlinScenario} with monetary incentive policies.
 */
public class OpenBerlinWithMonetaryIncentive extends OpenBerlinScenario{


	@CommandLine.Option(names = "--distanceBasedReward",
		defaultValue = "0.0",
		description = "monetary value an agents receives for cycling, walking or using pt")
	private Double distanceBasedReward;

	@CommandLine.Option(names = "--mobilityBudget",
		defaultValue = "0.0",
		description = "monetary value of the mobility budget")
	private Double mobilityBudget;

	@CommandLine.Option(names = "--simpleMobilityBudget",
		defaultValue = "0.0",
		description = "monetary value of the mobility budget")
	private Double simpleMobilityBudget;



	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinWithMonetaryIncentive.class, args);
	}


	@Override
	protected Config prepareConfig(Config config) {
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		// teleported beeline is the same for bike and walk
		if (distanceBasedReward > 0.0) {

			DistanceBasedMoneyReward distanceBasedMoneyReward = new DistanceBasedMoneyReward(
				controler.getScenario().getConfig().routing().getBeelineDistanceFactors().get(TransportMode.walk),
				controler.getScenario().getNetwork(), distanceBasedReward);
			addKlimaTaler(controler, distanceBasedMoneyReward);
		}

		if (mobilityBudget > 0.0) {
			MobilityBudgetEventHandler mobilityBudgetEventHandler = new MobilityBudgetEventHandler(getPersonsEligibleForMobilityBudget2FixedValue(controler.getScenario(), mobilityBudget));
			addMobilityBudgetHandler(controler, mobilityBudgetEventHandler);
		}

		if (simpleMobilityBudget != 0.0) {
			double oldDailyMonetaryConstant = controler.getScenario().getConfig().scoring().getModes().get(TransportMode.car).getDailyMonetaryConstant();
			controler.getScenario().getConfig().scoring().getModes().get(TransportMode.car).setDailyMonetaryConstant(oldDailyMonetaryConstant + simpleMobilityBudget);
		}

	}

	private static void addKlimaTaler(Controler controler, DistanceBasedMoneyReward klimaTaler) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(klimaTaler);
				addControlerListenerBinding().toInstance(klimaTaler);
				new PersonMoneyEventsAnalysisModule();
			}
		});
	}

	private static void addMobilityBudgetHandler(Controler controler, MobilityBudgetEventHandler mobilityBudgetEventHandler) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
				addControlerListenerBinding().toInstance(mobilityBudgetEventHandler);
				new PersonMoneyEventsAnalysisModule();
			}
		});
	}


	private static class DistanceBasedMoneyReward implements PersonDepartureEventHandler,
		PersonArrivalEventHandler,
		AfterMobsimListener,
		ActivityStartEventHandler {

		private final Map<Id<Person>, Double> distanceTravelledWalk = new HashMap<>();
		private final Map<Id<Person>, Double> distanceTravelledBike = new HashMap<>();
		private final double beelineDistanceFactor;
		private final Network network;
		private final double klimaTaler;
		private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
		private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();
		private final Map<Id<Person>, Double> distanceTravelledPt = new HashMap<>();

		private Map<Id<Person>, Coord> agentDepartureLocations = new HashMap<>();

		DistanceBasedMoneyReward(double modeSpecificBeelineDistanceFactor, Network network, double klimaTaler) {
			this.beelineDistanceFactor = modeSpecificBeelineDistanceFactor;
			this.network = network;
			this.klimaTaler = klimaTaler;
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			Id<Link> linkId = event.getLinkId();
			Coord endcoord = network.getLinks().get(linkId).getCoord();
			Coord startCoord = this.agentDepartureLocations.get(event.getPersonId());

			if (event.getLegMode().equals(TransportMode.walk)) {
				if (startCoord != null) {
					double beelineDistance = CoordUtils.calcEuclideanDistance(startCoord, endcoord);
					double distance = beelineDistance * beelineDistanceFactor;
					if (!distanceTravelledWalk.containsKey(event.getPersonId())) {
						distanceTravelledWalk.put(event.getPersonId(), distance);
					} else {
						distance = distanceTravelledWalk.get(event.getPersonId()) + distance;
						distanceTravelledWalk.replace(event.getPersonId(), distance);
					}
				}
			}

			if (event.getLegMode().equals(TransportMode.bike)) {
				if (startCoord != null) {
					double beelineDistance = CoordUtils.calcEuclideanDistance(startCoord, endcoord);
					double distance = beelineDistance * beelineDistanceFactor;
					if (!distanceTravelledBike.containsKey(event.getPersonId())) {
						distanceTravelledBike.put(event.getPersonId(), distance);
					} else {
						distance = distanceTravelledBike.get(event.getPersonId()) + distance;
						distanceTravelledBike.replace(event.getPersonId(), distance);
					}
				}
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			//in the pt test case the routing mode can be null
			if (event.getRoutingMode() !=null) {
				if (event.getRoutingMode().equals(TransportMode.walk)) {
					Id<Link> linkId = event.getLinkId();
					Coord coord = network.getLinks().get(linkId).getCoord();
					this.agentDepartureLocations.put(event.getPersonId(), coord);
				}

				if (event.getRoutingMode().equals(TransportMode.bike) || event.getRoutingMode().equals("bicycle")) {
					Id<Link> linkId = event.getLinkId();
					Coord coord = network.getLinks().get(linkId).getCoord();
					this.agentDepartureLocations.put(event.getPersonId(), coord);
				}
			}
		}

		@Override
		public void reset(int iteration) {
			this.agentDepartureLocations.clear();
			this.distanceTravelledWalk.clear();
			this.distanceTravelledPt.clear();
			this.distanceTravelledBike.clear();
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
			for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledWalk.entrySet()) {
				Id<Person> person = idDoubleEntry.getKey();
				double emissionsSaved = idDoubleEntry.getValue() * 0.176;
				double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
				afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForWalk", null, null));
			}

			for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledBike.entrySet()) {
				Id<Person> person = idDoubleEntry.getKey();
				double emissionsSaved = idDoubleEntry.getValue() * 0.176;
				double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
				afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForBike", null, null));
			}

			for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledPt.entrySet()) {
				Id<Person> person = idDoubleEntry.getKey();
				double emissionsSaved = idDoubleEntry.getValue() * 0.076;
				double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
				afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForPt", null, null));
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of first pt interaction an agent has in the whole leg
				personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real activity (i.e. finish the leg)
			}

			if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
				Id<Person> personId = event.getPersonId();
				if (personDepartureCoordMap.containsKey(personId)) {
					double distance = CoordUtils.calcEuclideanDistance(personDepartureCoordMap.get(personId), personArrivalCoordMap.get(personId));
					if (distanceTravelledPt.containsKey(event.getPersonId())) {
						distance = distanceTravelledPt.get(personId) + distance;
						distanceTravelledPt.replace(personId, distance);
					}
					if (!distanceTravelledPt.containsKey(event.getPersonId())) {
						distanceTravelledPt.put(personId, distance);

					}
					personDepartureCoordMap.remove(personId);
					personArrivalCoordMap.remove(personId);
				}
			}
		}
	}




	private static class MobilityBudgetEventHandler implements PersonDepartureEventHandler, AfterMobsimListener, PersonStuckEventHandler {

		private final Map<Id<Person>, Double> person2MobilityBudget;
		private final Map<Id<Person>, Double> currentIterationMobilityBudget = new HashMap<>();
		private final List<Id<Person>> personWhoAreStuck= new ArrayList<>();

		MobilityBudgetEventHandler(Map<Id<Person>, Double> personsEligibleForMobilityBudget2MoneyValue) {
			this.person2MobilityBudget = personsEligibleForMobilityBudget2MoneyValue;
		}

		@Override
		public void reset(int iteration) {
			currentIterationMobilityBudget.clear();
			currentIterationMobilityBudget.putAll(person2MobilityBudget);
			personWhoAreStuck.clear();
		}

		@Override
		public void handleEvent(PersonDepartureEvent personDepartureEvent) {
			Id<Person> personId = personDepartureEvent.getPersonId();
			if (this.currentIterationMobilityBudget.containsKey(personId) && personDepartureEvent.getLegMode().equals(TransportMode.car)) {
				this.currentIterationMobilityBudget.replace(personId, 0.0);
			}
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			double totalSumMobilityBudget = 0.;
			for (Map.Entry<Id<Person>, Double> idDoubleEntry : currentIterationMobilityBudget.entrySet()) {
				Id<Person> person = idDoubleEntry.getKey();
				Double mobilityBudget = idDoubleEntry.getValue();
				event.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, mobilityBudget, "mobilityBudget", null, null));
				totalSumMobilityBudget = totalSumMobilityBudget + mobilityBudget;
			}

			for (Id<Person> personId: personWhoAreStuck) {
				event.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, personId, -1000, "punishmentForBeingStuck", null, null));
			}
		}

		/*
		This is here to add an extra punishment to people who get stuck and benefit from getting the mob budget and thus obtaining a high score
		 */
		@Override
		public void handleEvent(PersonStuckEvent personStuckEvent) {
			personWhoAreStuck.add(personStuckEvent.getPersonId());
		}
	}

	static Map<Id<Person>, Double> getPersonsEligibleForMobilityBudget2FixedValue(Scenario scenario, Double value) {
		Map<Id<Person>, Double> persons2Budget = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String subpopulation = (String) person.getAttributes().getAttribute("subpopulation");

			if (subpopulation.contains("person")) {
				Plan plan = person.getSelectedPlan();
				//TripStructureUtil get Legs
				List<String> transportModeList = new ArrayList<>();
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
				for (TripStructureUtils.Trip trip: trips) {
					List<Leg> listLegs = trip.getLegsOnly();
					for (Leg leg: listLegs) {
						transportModeList.add(leg.getMode());
					}
				}
				if (transportModeList.contains(TransportMode.car)) {
					persons2Budget.put(person.getId(), value);
				}
			}
		}
		return persons2Budget;
	}
}
