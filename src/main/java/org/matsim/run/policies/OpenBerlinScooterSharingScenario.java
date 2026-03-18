package org.matsim.run.policies;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.run.OpenBerlinDrtScenario;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Berlin scenario including the possibility to change beta money and thus the perception of monetary prices.
 * E.g. for pt or car.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinScooterSharingScenario extends OpenBerlinScenario {
	static final String E_SCOOTER = "eScooter";
	private static final String STOP_FILTER = "eScooterStopFilter";
	private static final String STOP_FILTER_VALUE = "station_S/U/RE/RB_eScooter";
	private static final String BERLIN_SHP_STRING = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/Berlin_25832.shp";

	@CommandLine.Option(names = "--sharing-service", description = "Path to sharing service xml file with stations and vehicles.", required = true)
	private String serviceFile;
	@CommandLine.Option(names = "--intermodal-e-scooter", defaultValue = "E_SCOOTER_REGULAR_AND_INTERMODAL", description = "INTERMODAL_E_SCOOTER_ONLY: eScooter can only be used for access/egress to PT. E_SCOOTER_REGULAR_AND_INTERMODAL: eScooter used for intermodal access/egress to PT and as separate mode.")
	private EScooterIntermodalityHandling intermodal;
	@CommandLine.Option(names = "--base-fare", description = "Base fare for a sharing trip := fare for unlocking the vehicle. Default = 1Eu. " +
		"Value has to be provided as non negative double.", defaultValue = "1.0")
	private double baseFare;
	@CommandLine.Option(names = "--distance-fare", description = "Distance based fare for a sharing trip [Eu/m]. Default = 0Eu/m. " +
		"Value has to be provided as non negative double.", defaultValue = "0.0")
	private double distanceFare;
	@CommandLine.Option(names = "--time-fare", description = "Time based fare for a sharing trip [Eu/s]. Default = 0.0045Eu/s. " +
		"Value has to be provided as non negative double.", defaultValue = "0.0045")
	private double timeFare;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		addSharingServiceInConfig(config, serviceFile, baseFare, distanceFare, timeFare, intermodal);

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		copyBikeModeConstantsForSharingInScenario(scenario);

//		tag intermodal eScooter-pt-stations
		OpenBerlinDrtScenario.tagTransitStopsInServiceArea(scenario.getTransitSchedule(),
			STOP_FILTER, STOP_FILTER_VALUE,
			BERLIN_SHP_STRING,
			"stopFilter", "station_S/U/RE/RB",
			// some S+U stations are located slightly outside the shp File, e.g. U7 Neukoelln, U8
			// Hermannstr., so allow buffer around the shape.
			// This does not mean that a drt vehicle can pick the passenger up outside the service area,
			// rather the passenger has to walk the last few meters from the drt drop off to the station.
//			we now use whole of berlin instead of Hundekopf, but will keep using the buffer, assuming that the same issues might occur for other stations -sm0126
			200.0);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

		addSharingModuleAndIntermodalFareCompensationInController(controler);
	}

	/**
	 * add and configure the sharing config group with the given parameters.
	 */
	static void addSharingServiceInConfig(Config config, String serviceFile, double baseFare, double distanceFare, double timeFare,
										  EScooterIntermodalityHandling intermodal) {
		SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config, SharingConfigGroup.class);
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		serviceConfig.setId(E_SCOOTER);
		serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
		serviceConfig.setMaximumAccessEgressDistance(10000);
		serviceConfig.setServiceInputFile(serviceFile);
		serviceConfig.setServiceAreaShapeFile(BERLIN_SHP_STRING);
		serviceConfig.setMode(E_SCOOTER);
//		pricing of different providers in berlin:
//		bolt: 0€ unlock fee, 0.25€/min
//		dott: 1€, 0.29€/min
//		voi: 0.49€, 0.27€/min
//		lime: 1€, 0.27€/min
//		Hence, our default values in run params are 1€ unlock fare and 0.27€/min = 0.0045€/s time fare, 0€/m distance fare.
//		fare has to be set as non-negative, it is transformed to a negative double later on (in the shared mobility contrib)
		serviceConfig.setBaseFare(Math.max(-baseFare, baseFare));
		serviceConfig.setTimeFare(Math.max(-timeFare, timeFare));
		serviceConfig.setDistanceFare(Math.max(-distanceFare, distanceFare));

		sharingConfigGroup.addService(serviceConfig);

		// Register the shared mode as a teleportation mode
		RoutingConfigGroup.TeleportedModeParams eScooterParams = new RoutingConfigGroup.TeleportedModeParams(E_SCOOTER);
//		2.98 is the speed of veh type bike in the veh type file
		eScooterParams.setTeleportedModeSpeed(2.98);
		eScooterParams.setBeelineDistanceFactor(1.3);
		config.routing().addTeleportedModeParams(eScooterParams);


		if (intermodal == EScooterIntermodalityHandling.E_SCOOTER_REGULAR_AND_INTERMODAL) {
			// Add the shared mode to mode choice
			List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
//			here we need to use geServiceMode(), because this builds a string prexix_serviceId and adds it to SMC in a subsequent step.
//			if we just add E_SCOOTER, agents will be able to use eScooter as a separate mode without sharing!!!
//			I do not like this, but for this matsim version we have to accept it. -sm0226
			modes.add(SharingUtils.getServiceMode(serviceConfig));
			config.subtourModeChoice().setModes(modes.toArray(new String[0]));
		}

		// Add activity types used in shared mobility
		for (String act : List.of(SharingUtils.PICKUP_ACTIVITY, SharingUtils.DROPOFF_ACTIVITY, SharingUtils.BOOKING_ACTIVITY)) {
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams(act);
			params.setScoringThisActivityAtAll(false);
			config.scoring().addActivityParams(params);
		}

		// Scoring params of eScooter mode based on bike scoring params
		ScoringConfigGroup.ModeParams bikeParams = config.scoring().getModes().get(TransportMode.bike);

		ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(E_SCOOTER);
		modeParams.setConstant(0.);
		modeParams.setMarginalUtilityOfDistance(0.);
		modeParams.setMarginalUtilityOfTraveling(0.);
		modeParams.setDailyUtilityConstant(0.);

		config.scoring().addModeParams(modeParams);

//		configure intermodal access/egress to pt
		SwissRailRaptorConfigGroup raptorConfigGroup = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet intermodalParams = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		intermodalParams.setMode(E_SCOOTER);
		intermodalParams.setSearchExtensionRadius(1000.);
//		in this DLR report (p. 7), multiple sources about avg. eScooter trip length are cited https://elib.dlr.de/141837/1/ArbeitsberichteVF_Nr4_2021.pdf
//		avg. eScooter trip length seems to ~2km
//		thus, we set initialSearchRadius to 2km and maxRadius to 2 * initialRadius. -sm0126
		intermodalParams.setInitialSearchRadius(2000.);
		intermodalParams.setMaxRadius(4000.);
		intermodalParams.setStopFilterAttribute(STOP_FILTER);
//		we assume that -- similar to DRT -- access/egress to PT is not done to bus/tram
		intermodalParams.setStopFilterValue(STOP_FILTER_VALUE);

		raptorConfigGroup.addIntermodalAccessEgress(intermodalParams);
	}

	/**
	 * copy bike mode constants for eScooter if available.
	 */
	static void copyBikeModeConstantsForSharingInScenario(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (PersonUtils.getModeConstants(person) != null && PersonUtils.getModeConstants(person).containsKey(TransportMode.bike)) {
//				assume that preference for bike is similar for eScooter

				Map<String, String> modeConstants = new HashMap<>(PersonUtils.getModeConstants(person));
				modeConstants.put(E_SCOOTER, modeConstants.get(TransportMode.bike));
				PersonUtils.setModeConstants(person, modeConstants);
			}
		}
	}

	/**
	 * add sharin module as well as refund handler for intermodal pt-eScooter trips.
	 */
	static void addSharingModuleAndIntermodalFareCompensationInController(Controler controler) {
		controler.addOverridingModule(new SharingModule());
		controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class)));

//		add intermodal trip compensation when pt is used once in a day for eScooter trips
		SharingRefundHandler refundHandler = new SharingRefundHandler(TransportMode.pt);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(refundHandler);
				addControlerListenerBinding().toInstance(refundHandler);
				bind(AnalysisMainModeIdentifier.class).to(MobilityToGridScenariosUtils.OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier.class);
				bind(MainModeIdentifier.class).to(MobilityToGridScenariosUtils.OpenBerlinIntermodalPtSharingRouterModeIdentifier.class);
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
			}
		});
	}

	/**
	 * Helper enum to enable/disable functionalities.
	 */
	enum EScooterIntermodalityHandling {INTERMODAL_E_SCOOTER_ONLY, E_SCOOTER_REGULAR_AND_INTERMODAL}

	private static final class SharingRefundHandler implements PersonDepartureEventHandler, PersonMoneyEventHandler, AfterMobsimListener {
		@Inject
		private EventsManager events;
		@Inject
		QSimConfigGroup qSimConfigGroup;

		private final String ptMode;

		private final Set<Id<Person>> ptUsers = new HashSet<>();
		private final Map<Id<Person>, List<Double>> eScooterFaresPerPerson = new HashMap<>();

		private SharingRefundHandler(String ptMode) {
			this.ptMode = ptMode;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
//			register pt users
			if (event.getLegMode().equals(ptMode)) {
				ptUsers.add(event.getPersonId());
			}
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
//			register fare amount if eScooter fare money event
			if (event.getPurpose().equals(SharingTeleportedRentalsHandler.PERSON_MONEY_EVENT_PURPOSE_SHARING_FARE)) {
				eScooterFaresPerPerson.computeIfAbsent(event.getPersonId(), personId -> new ArrayList<>()).add(event.getAmount());
			}
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			for (Map.Entry<Id<Person>, List<Double>> entry : eScooterFaresPerPerson.entrySet()) {
				if (ptUsers.contains(entry.getKey())) {
//					only refund eScooter trips when pt was used once per day
					double fareSum = entry.getValue()
						.stream()
						.mapToDouble(Double::doubleValue)
						.sum();

					double time = (Double.isFinite(qSimConfigGroup.getEndTime().seconds()) && qSimConfigGroup.getEndTime().seconds() > 0)
						? qSimConfigGroup.getEndTime().seconds()
						: Double.MAX_VALUE;

					events.processEvent(new PersonMoneyEvent(time, entry.getKey(), -fareSum, "intermodal-eScooter-refund",
						entry.getKey().toString(), null));
				}
			}
		}

		@Override
		public void reset(int iteration) {
			ptUsers.clear();
			eScooterFaresPerPerson.clear();
		}
	}
}
