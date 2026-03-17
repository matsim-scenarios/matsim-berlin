package org.matsim.run.policies;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * Utils class for plugging together different policies in the same scenario.
 */
public final class MobilityToGridScenariosUtils {
	private static final Logger log = LogManager.getLogger(MobilityToGridScenariosUtils.class);

	private static final String AVERAGE = "average";

	private MobilityToGridScenariosUtils() {}

	public static void addEngineInformationToVehicleTypes(Scenario scenario, String carFuelType) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();

//				only set engine information if none are present
			if (engineInformation.getAttributes().isEmpty()) {
				switch (type.getId().toString()) {
//						all other vehicle types (which are not listed here) already have engine information assigned in the input vehicle types file
//						berlin-v6.4.vehicleTypes.xml in same dir as config.
//						for hbefa 4.1 (which we are using here) diesel, petrol etc. is saved as "EmissionConcept" whereas it HBEFA 4.2 it is saved as technology???
					case TransportMode.car -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
//							based on Kraftfahrzeugbestand germany 1.1.2025 ~60% petrol and 28% diesel, so we take petrol here.
//							source: https://www.kba.de/DE/Presse/Pressemitteilungen/Fahrzeugbestand/2025/pm10_fz_bestand_pm_komplett.html
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, carFuelType);
					}
					case TransportMode.ride -> {
//							ignore ride, the mode is routed on network, but then teleported
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case TransportMode.bike -> {
//							ignore bikes
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case "freight", TransportMode.truck -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, "diesel");
					}
					default -> throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
				}
			}
		}
//			ignore all pt veh types
		scenario.getTransitVehicles()
			.getVehicleTypes()
			.values().forEach(type -> VehicleUtils.setHbefaVehicleCategory(type.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString()));
	}

	/**
	 * Enum for setting HBEFA 4.1 emission concept = fuel type for a vehicle type.
	 */
	public enum Hbefa41EmissionConcept {PETROL_4S, DIESEL, ELECTRICITY}

	/**
	 * This is an adapted version of class OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier for sharing modes.
	 */
	static final class OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier implements AnalysisMainModeIdentifier {
		public static final String ANALYSIS_MAIN_MODE_PT_WITH_SHARING_USED_FOR_ACCESS_OR_EGRESS = "pt_w_sharing_used";
		private static final Logger log = LogManager.getLogger(OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier.class);
		private final List<String> modeHierarchy = new ArrayList<>() ;
		private final List<String> sharingModes;

		@Inject
		OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier() {
			sharingModes = Arrays.asList(OpenBerlinScooterSharingScenario.E_SCOOTER, "sharing_" + OpenBerlinScooterSharingScenario.E_SCOOTER);

			modeHierarchy.add( TransportMode.walk ) ;
			// TransportMode.bike is not registered as main mode, only "bicycle" ;
			modeHierarchy.add( TransportMode.bike );
			modeHierarchy.add( TransportMode.ride ) ;
			modeHierarchy.add( TransportMode.car ) ;
			modeHierarchy.add( "car2" ) ;
			for (String sharingMode: sharingModes) {
				modeHierarchy.add( sharingMode ) ;
			}
			modeHierarchy.add( TransportMode.pt ) ;
			modeHierarchy.add( "freight" );
			modeHierarchy.add( "truck" );

			// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
			// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
			// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
			// trip as two separate trips. kai, sep'16
		}

		@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
			int mainModeIndex = -1 ;
			List<String> modesFound = new ArrayList<>();
			for ( PlanElement pe : planElements ) {
				int index;
				String mode;
				if ( pe instanceof Leg leg) {
					mode = leg.getMode();
				} else {
					continue;
				}
				if (mode.equals(TransportMode.non_network_walk)) {
					// skip, this is only a helper mode in case walk is routed on the network
					continue;
				}
				modesFound.add(mode);
				index = modeHierarchy.indexOf( mode ) ;
				if ( index < 0 ) {
					log.error("unknown mode={}", mode );
					throw new IllegalStateException("") ;
				}
				if ( index > mainModeIndex ) {
					mainModeIndex = index ;
				}
			}
			if (mainModeIndex == -1) {
				log.error("no main mode found for trip {}", planElements);
				throw new IllegalStateException("") ;
			}

			String mainMode = modeHierarchy.get( mainModeIndex ) ;
			// differentiate pt monomodal/intermodal
			if (mainMode.equals(TransportMode.pt)) {
				boolean isSharingPt = false;
				for (String modeFound: modesFound) {
					if (modeFound.equals(TransportMode.pt)) {
						continue;
					} else if (modeFound.equals(TransportMode.walk)) {
						continue;
					} else if (sharingModes.contains(modeFound)) {
						isSharingPt = true;
					} else {
						log.error("unknown intermodal pt trip: {}", planElements);
						throw new IllegalStateException("unknown intermodal pt trip");
					}
				}

				if (isSharingPt) {
					return ANALYSIS_MAIN_MODE_PT_WITH_SHARING_USED_FOR_ACCESS_OR_EGRESS;
				} else {
					return TransportMode.pt;
				}

			} else {
				return mainMode;
			}
		}
	}

	/**
	 * This is an adapted version of class OpenBerlinIntermodalPtDrtRouterModeIdentifier for sharing modes.
	 * I do not understand why this class is necessary as -- except some small differences -- is the same as OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier.
	 * For OpenBerlinDrtScenario such a system of 2 classes seems to be necessary, so we will use it for sharing as well. -sm0226
	 */
	static final class OpenBerlinIntermodalPtSharingRouterModeIdentifier implements AnalysisMainModeIdentifier {
		private final List<String> modeHierarchy = new ArrayList<>() ;
		private final List<String> sharingModes;

		@Inject
		OpenBerlinIntermodalPtSharingRouterModeIdentifier() {
			sharingModes = Arrays.asList(OpenBerlinScooterSharingScenario.E_SCOOTER, "sharing_" + OpenBerlinScooterSharingScenario.E_SCOOTER);

			modeHierarchy.add( TransportMode.walk ) ;
			modeHierarchy.add( TransportMode.bike );
			modeHierarchy.add( TransportMode.ride ) ;
			modeHierarchy.add( TransportMode.car ) ;
			modeHierarchy.add( "car2" ) ;
			for (String sharingMode: sharingModes) {
				modeHierarchy.add( sharingMode ) ;
			}
			modeHierarchy.add( TransportMode.pt ) ;
			modeHierarchy.add( "freight" );

			// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
			// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
			// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
			// trip as two separate trips. kai, sep'16
		}

		@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
			int mainModeIndex = -1 ;
			for ( PlanElement pe : planElements ) {
				int index;
				String mode;
				if ( pe instanceof Leg leg ) {
					mode = leg.getMode();
				} else {
					continue;
				}
				if (mode.equals(TransportMode.non_network_walk)) {
					// skip, this is only a helper mode in case walk is routed on the network
					continue;
				}
				index = modeHierarchy.indexOf( mode ) ;
				if ( index < 0 ) {
					log.error("unknown mode={}", mode);
					throw new IllegalStateException("") ;
				}
				if ( index > mainModeIndex ) {
					mainModeIndex = index ;
				}
			}
			if (mainModeIndex == -1) {
				log.error("no main mode found for trip {}", planElements);
				throw new IllegalStateException("") ;
			}
			return modeHierarchy.get( mainModeIndex ) ;
		}
	}
}
