package RunAbfall;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Ricardo Ewert
 *
 */
class AbfallUtils {

	static final Logger log = Logger.getLogger(AbfallUtils.class);

	static double costsJsprit = 0;
	static int noPickup = 0;
	static int allGarbage = 0;
	static int numberOfShipments = 0;
	static int garbageRuhleben = 0;
	static int garbagePankow = 0;
	static int garbageReinickenD = 0;
	static int garbageGradestr = 0;
	static int garbageGruenauerStr = 0;
	static String linkMhkwRuhleben = "142010";
	static String linkMpsPankow = "145812";
	static String linkMpsReinickendorf = "59055";
	static String linkUmladestationGradestrasse = "71781";
	static String linkGruenauerStr = "97944";
	static List<String> districtsWithShipments = new ArrayList<String>();
	static List<String> districtsWithNoShipments = new ArrayList<String>();
	static HashMap<String, String> dataEnt = new HashMap<String, String>();
	static CarrierVehicleTypes vehicleTypes = null;
	static CarrierVehicleType carrierVehType = null;
	static int capacityTruck = 0;
	static CarrierVehicle vehicleAtDepot = null;
	static Multimap<String, String> linksInDistricts;
	static boolean streetAlreadyInGarbageLinks = false;
	static boolean oneCarrierForEachDistrict = false;

	/**
	 * Creates a map for getting the name of the attribute, where you can find the
	 * dump for the selected day of pickup.
	 */
	private static void createMapEnt() {
		dataEnt.put("MO", "Mo-Ent");
		dataEnt.put("DI", "Di-Ent");
		dataEnt.put("MI", "Mi-Ent");
		dataEnt.put("DO", "Do-Ent");
		dataEnt.put("FR", "Fr-Ent");
	}

	/**
	 * Creates a map with the 4 depots in Berlin as 4 different carrier.
	 * 
	 * @return
	 */
	static HashMap<String, Carrier> createCarrier() {
		HashMap<String, Carrier> carrierMap = new HashMap<String, Carrier>();

		Carrier bsrForckenbeck = CarrierImpl.newInstance(Id.create("BSR_Forckenbeck", Carrier.class));
		Carrier bsrMalmoeer = CarrierImpl.newInstance(Id.create("BSR_MalmoeerStr", Carrier.class));
		Carrier bsrNordring = CarrierImpl.newInstance(Id.create("BSR_Nordring", Carrier.class));
		Carrier bsrGradestrasse = CarrierImpl.newInstance(Id.create("BSR_Gradestrasse", Carrier.class));
		carrierMap.put("Nordring", bsrNordring);
		carrierMap.put("MalmoeerStr", bsrMalmoeer);
		carrierMap.put("Forckenbeck", bsrForckenbeck);
		carrierMap.put("Gradestrasse", bsrGradestrasse);

		return carrierMap;
	}

	/**
	 * Creates a multimap where you can find behind every district every link
	 * containing this district.
	 * 
	 * @param
	 */
	static void createMapWithLinksInDistricts(Collection<SimpleFeature> districts,
			Map<Id<Link>, ? extends Link> allLinks) {
		linksInDistricts = ArrayListMultimap.create();
		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
		Point p;
		log.info("Started creating Multimap with all links of each district...");
		for (Link link : allLinks.values()) {
			xCoordFrom = link.getFromNode().getCoord().getX();
			xCoordTo = link.getToNode().getCoord().getX();
			yCoordFrom = link.getFromNode().getCoord().getY();
			yCoordTo = link.getToNode().getCoord().getY();
			if (xCoordFrom > xCoordTo)
				x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
			else
				x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
			if (yCoordFrom > yCoordTo)
				y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
			else
				y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);
			p = MGC.xy2Point(x, y);
			for (SimpleFeature district : districts) {
				if (((Geometry) district.getDefaultGeometry()).contains(p)) {
					linksInDistricts.put(district.getAttribute("Ortsteilna").toString(), link.getId().toString());
				}
			}
		}
		log.info("Finished creating Multimap with all links of each district!");
	}

	/**
	 * Creates a Map with the 5 dumps in Berlin.
	 * 
	 * @return
	 */
	static HashMap<String, Id<Link>> createDumpMap() {
		HashMap<String, Id<Link>> garbageDumps = new HashMap<String, Id<Link>>();

		garbageDumps.put("Ruhleben", Id.createLinkId(linkMhkwRuhleben));
		garbageDumps.put("Pankow", Id.createLinkId(linkMpsPankow));
		garbageDumps.put("Gradestr", Id.createLinkId(linkUmladestationGradestrasse));
		garbageDumps.put("ReinickenD", Id.createLinkId(linkMpsReinickendorf));
		garbageDumps.put("GruenauerStr", Id.createLinkId(linkGruenauerStr));
		return garbageDumps;
	}

	/**
	 * Deletes the existing output file and sets the number of the last iteration
	 * 
	 * @param config
	 */
	static Config prepareConfig(Config config, int lastIteration) {
		// (the directory structure is needed for jsprit output, which is before the
		// controler starts. Maybe there is a better alternative ...)
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(TransformationFactory.GK4);

		return config;
	}

	/**
	 * Creates Shipments for the selected areas for the selected weekday. The needed
	 * data is part of the read shapefile. There are informations about the volume
	 * of garbageToCollect for every day and the dump where the garbage have to
	 * bring to.
	 * 
	 * @param
	 */
	static void createShipmentsForSelectedArea(Collection<SimpleFeature> districtsWithGarbage,
			List<String> districtsForShipments, String day, HashMap<String, Id<Link>> garbageDumps, Scenario scenario,
			Carriers carriers, HashMap<String, Carrier> carrierMap, Map<Id<Link>, ? extends Link> allLinks,
			Map<Id<Link>, Link> garbageLinks, double volumeBigTrashcan, double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		int garbageToCollect = 0;
		String depot = null;
		createMapEnt();
		for (String districtToCollect : districtsForShipments) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteilna").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						garbageToCollect = (int) ((double) districtInformation.getAttribute(day) * 1000);
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 12 && link.getAllowedModes().contains("car")) {
										for (Link garbageLink : garbageLinks.values()) {
											if (link.getFromNode() == garbageLink.getToNode()
													&& link.getToNode() == garbageLink.getFromNode())
												streetAlreadyInGarbageLinks = true;

										}
										if (streetAlreadyInGarbageLinks != true) {
											garbageLinks.put(link.getId(), link);
											distanceWithShipments = distanceWithShipments + link.getLength();
										}
										streetAlreadyInGarbageLinks = false;
									}
								}
							}
						}
					} else {
						log.warn("At District " + districtInformation.getAttribute("Ortsteilna").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}
				}

			}
			if (garbageLinks.size() != 0) {
				districtsWithShipments.add(districtToCollect);
				createShipmentsForCarrierII(garbageToCollect, volumeBigTrashcan, serviceTimePerBigTrashcan,
						distanceWithShipments, garbageLinks, scenario, carrierMap.get(depot), dumpId, carriers);
			}
			distanceWithShipments = 0;
			garbageLinks.clear();
		}
		for (Carrier carrier : carrierMap.values())
			carriers.addCarrier(carrier);

	}

	/**
	 * Creates Shipments for the selected areas for the selected weekday. You have
	 * to select the areas and for every area the garbage volume per meter street.
	 * The information about the dump is given in the shapefile.
	 * 
	 * @param
	 */
	static void createShipmentsWithGarbagePerMeter(Collection<SimpleFeature> districtsWithGarbage,
			HashMap<String, Double> areasForShipmentPerMeterMap, String day, HashMap<String, Id<Link>> garbageDumps,
			Scenario scenario, Carriers carriers, HashMap<String, Carrier> carrierMap,
			Map<Id<Link>, ? extends Link> allLinks, Map<Id<Link>, Link> garbageLinks, double volumeBigTrashcan,
			double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		String depot = null;
		createMapEnt();
		for (String districtToCollect : areasForShipmentPerMeterMap.keySet()) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteilna").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 12 && link.getAllowedModes().contains("car")) {
										for (Link garbageLink : garbageLinks.values()) {
											if (link.getFromNode() == garbageLink.getToNode()
													&& link.getToNode() == garbageLink.getFromNode())
												streetAlreadyInGarbageLinks = true;

										}
										if (streetAlreadyInGarbageLinks != true) {
											garbageLinks.put(link.getId(), link);
											distanceWithShipments = distanceWithShipments + link.getLength();
										}
										streetAlreadyInGarbageLinks = false;
									}
								}
							}
						}
					} else {
						log.warn("At District " + districtInformation.getAttribute("Ortsteilna").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}

				}

			}
			if (garbageLinks.size() != 0)
				districtsWithShipments.add(districtToCollect);
			double garbagePerMeterToCollect = areasForShipmentPerMeterMap.get(districtToCollect);
			createShipmentsForCarrierI(garbagePerMeterToCollect, volumeBigTrashcan, serviceTimePerBigTrashcan,
					garbageLinks, scenario, carrierMap.get(depot), dumpId, carriers);
			distanceWithShipments = 0;
			garbageLinks.clear();
		}
		for (Carrier carrier : carrierMap.values())
			carriers.addCarrier(carrier);
	}

	/**
	 * Creates Shipments for Berlin for the selected weekday. You have to select the
	 * areas and for every area the garbage volume which should be select in this
	 * area. The information about the dump is given in the shapefile.
	 * 
	 * @param
	 */
	static void createShipmentsGarbagePerVolume(Collection<SimpleFeature> districtsWithGarbage,
			HashMap<String, Integer> areasForShipmentPerVolumeMap, String day, HashMap<String, Id<Link>> garbageDumps,
			Scenario scenario, Carriers carriers, HashMap<String, Carrier> carrierMap,
			Map<Id<Link>, ? extends Link> allLinks, Map<Id<Link>, Link> garbageLinks, double volumeBigTrashcan,
			double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		String depot = null;
		createMapEnt();
		for (String districtToCollect : areasForShipmentPerVolumeMap.keySet()) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteilna").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 12 && link.getAllowedModes().contains("car")) {
										for (Link garbageLink : garbageLinks.values()) {
											if (link.getFromNode() == garbageLink.getToNode()
													&& link.getToNode() == garbageLink.getFromNode())
												streetAlreadyInGarbageLinks = true;

										}
										if (streetAlreadyInGarbageLinks != true) {
											garbageLinks.put(link.getId(), link);
											distanceWithShipments = distanceWithShipments + link.getLength();
										}
										streetAlreadyInGarbageLinks = false;

									}
								}
							}
						}
					} else {
						log.warn("At District " + districtInformation.getAttribute("Ortsteilna").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}

				}

			}
			if (garbageLinks.size() != 0)
				districtsWithShipments.add(districtToCollect);
			int garbageVolumeToCollect = areasForShipmentPerVolumeMap.get(districtToCollect);
			createShipmentsForCarrierII(garbageVolumeToCollect, volumeBigTrashcan, serviceTimePerBigTrashcan,
					distanceWithShipments, garbageLinks, scenario, carrierMap.get(depot), dumpId, carriers);
			distanceWithShipments = 0;
			garbageLinks.clear();
		}
		for (Carrier carrier : carrierMap.values())
			carriers.addCarrier(carrier);
	}

	/**
	 * Creates the shipments for all districts where the garbage will be picked up
	 * at the selected day.
	 * 
	 * @param
	 */
	static void createShipmentsForSelectedDay(Collection<SimpleFeature> districtsWithGarbage, String day,
			HashMap<String, Id<Link>> garbageDumps, Scenario scenario, Carriers carriers,
			HashMap<String, Carrier> carrierMap, Map<Id<Link>, ? extends Link> allLinks,
			Map<Id<Link>, Link> garbageLinks, double volumeBigTrashcan, double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		int garbageToCollect = 0;
		// String depot = null;
		createMapEnt();
		carrierMap.clear();
		for (SimpleFeature districtInformation : districtsWithGarbage) {
			if ((double) districtInformation.getAttribute(day) > 0) {
				garbageToCollect = (int) ((double) districtInformation.getAttribute(day) * 1000);
				dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
				// depot = districtInformation.getAttribute("Depot").toString();
				carrierMap.put(districtInformation.getAttribute("Ortsteilna").toString(), CarrierImpl.newInstance(
						Id.create("Carrier "+districtInformation.getAttribute("Ortsteilna").toString(), Carrier.class)));
				for (Link link : allLinks.values()) {
					for (String linkInDistrict : linksInDistricts
							.get(districtInformation.getAttribute("Ortsteilna").toString())) {
						if (Id.createLinkId(linkInDistrict) == link.getId()) {
							if (link.getFreespeed() < 12 && link.getAllowedModes().contains("car")) {
								for (Link garbageLink : garbageLinks.values()) {
									if (link.getFromNode() == garbageLink.getToNode()
											&& link.getToNode() == garbageLink.getFromNode())
										streetAlreadyInGarbageLinks = true;

								}
								if (streetAlreadyInGarbageLinks != true) {
									garbageLinks.put(link.getId(), link);
									distanceWithShipments = distanceWithShipments + link.getLength();
								}
								streetAlreadyInGarbageLinks = false;

							}
						}
					}
				}
			} else {
				log.warn("At District " + districtInformation.getAttribute("Ortsteilna").toString()
						+ " no garbage will be collected at " + day);
			}

			if (garbageLinks.size() != 0) {
				districtsWithShipments.add(districtInformation.getAttribute("Ortsteilna").toString());

				createShipmentsForCarrierII(garbageToCollect, volumeBigTrashcan, serviceTimePerBigTrashcan,
						distanceWithShipments, garbageLinks, scenario,
						carrierMap.get(districtInformation.getAttribute("Ortsteilna").toString()), dumpId, carriers);
			}
			distanceWithShipments = 0;
			garbageLinks.clear();
		}
		oneCarrierForEachDistrict = true;
		for (Carrier carrier : carrierMap.values())
			carriers.addCarrier(carrier);
	}

	/**
	 * Creates a Shipment for every garbagelink and ads all shipments to myCarrier.
	 * The volumeGarbage is in garbage per meter. So the volumeGarbage of every
	 * shipment depends of the input garbagePerMeterToCollect.
	 * 
	 * @param
	 */
	static void createShipmentsForCarrierI(double garbagePerMeterToCollect, double volumeBigTrashcan,
			double serviceTimePerBigTrashcan, Map<Id<Link>, Link> garbageLinks, Scenario scenario, Carrier thisCarrier,
			Id<Link> dumpId, Carriers carriers) {

		for (Link link : garbageLinks.values()) {
			double maxWeightBigTrashcan = volumeBigTrashcan * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
			int volumeGarbage = (int) Math.ceil(link.getLength() * garbagePerMeterToCollect);
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigTrashcan) * serviceTimePerBigTrashcan;
			double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * 60;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(), (dumpId),
							volumeGarbage)
					.setPickupServiceTime(serviceTime).setPickupTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600))
					.setDeliveryServiceTime(deliveryTime).build();
			thisCarrier.getShipments().add(shipment);
			countingGarbage(dumpId, volumeGarbage);
		}
		numberOfShipments = numberOfShipments + garbageLinks.size();
	}

	/**
	 * Creates a Shipment for every link, ads all shipments to myCarrier and ads
	 * myCarrier to carriers. The volumeGarbage is in garbageToCollect [kg]. So the
	 * volumeGarbage of every shipment depends of the sum of all lengths from links
	 * with shipments.
	 * 
	 * @param
	 */
	static void createShipmentsForCarrierII(int garbageToCollect, double volumeBigTrashcan,
			double serviceTimePerBigTrashcan, double distanceWithShipments, Map<Id<Link>, Link> garbageLinks,
			Scenario scenario, Carrier thisCarrier, Id<Link> garbageDumpId, Carriers carriers) {
		int count = 1;
		int garbageCount = 0;
		double roundingError = 0;
		for (Link link : garbageLinks.values()) {
			double maxWeightBigTrashcan = volumeBigTrashcan * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
			int volumeGarbage;
			if (count == garbageLinks.size()) {
				volumeGarbage = garbageToCollect - garbageCount;

			} else {
				volumeGarbage = (int) Math.ceil(link.getLength() / distanceWithShipments * garbageToCollect);
				roundingError = roundingError
						+ (volumeGarbage - (link.getLength() / distanceWithShipments * garbageToCollect));
				if (roundingError > 1) {
					volumeGarbage = volumeGarbage - 1;
					roundingError = roundingError - 1;
				}
				count++;
			}
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigTrashcan) * serviceTimePerBigTrashcan;
			double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * 60;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(),
							garbageDumpId, volumeGarbage)
					.setPickupServiceTime(serviceTime).setPickupTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600))
					.setDeliveryServiceTime(deliveryTime).build();
			thisCarrier.getShipments().add(shipment);
			garbageCount = garbageCount + volumeGarbage;
			countingGarbage(garbageDumpId, volumeGarbage);
		}
		numberOfShipments = numberOfShipments + garbageLinks.size();

	}

	/**
	 * This method is counting the garbage for every different dump and the total
	 * volume of garbage, which has to be collected.
	 * 
	 * @param
	 */
	private static void countingGarbage(Id<Link> garbageDumpId, int volumeGarbage) {
		allGarbage = allGarbage + volumeGarbage;
		if (garbageDumpId.equals(Id.createLinkId(linkGruenauerStr)))
			garbageGruenauerStr = garbageGruenauerStr + volumeGarbage;
		if (garbageDumpId.equals(Id.createLinkId(linkMhkwRuhleben)))
			garbageRuhleben = garbageRuhleben + volumeGarbage;
		if (garbageDumpId.equals(Id.createLinkId(linkMpsPankow)))
			garbagePankow = garbagePankow + volumeGarbage;
		if (garbageDumpId.equals(Id.createLinkId(linkMpsReinickendorf)))
			garbageReinickenD = garbageReinickenD + volumeGarbage;
		if (garbageDumpId.equals(Id.createLinkId(linkUmladestationGradestrasse)))
			garbageGradestr = garbageGradestr + volumeGarbage;
	}

	/**
	 * 
	 */
	static void createAndAddVehicles() {
		String vehicleTypeId = "MB_Econic_Diesel";
		capacityTruck = 11500; // in kg
		double maxVelocity = 80 / 3.6;
		double costPerDistanceUnit = 0.000824; // Berechnung aus Excel
		double costPerTimeUnit = 0.02685; // Lohnkosten bei Fixkosten integriert
		double fixCosts = 265.31; // Berechnung aus Excel
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.00067; // Berechnung aus Ecxel

		createGarbageTruckType(vehicleTypeId, maxVelocity, costPerDistanceUnit, costPerTimeUnit, fixCosts,
				engineInformation, literPerMeter);
		adVehicleType();
	}

	/**
	 * Method creates a new garbage truck type
	 * 
	 * @param maxVelocity in m/s
	 * @return
	 */
	private static void createGarbageTruckType(String vehicleTypeId, double maxVelocity, double costPerDistanceUnit,
			double costPerTimeUnit, double fixCosts, FuelType engineInformation, double literPerMeter) {
		carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create(vehicleTypeId, VehicleType.class))
				.setCapacity(capacityTruck).setMaxVelocity(maxVelocity).setCostPerDistanceUnit(costPerDistanceUnit)
				.setCostPerTimeUnit(costPerTimeUnit).setFixCost(fixCosts)
				.setEngineInformation(new EngineInformationImpl(engineInformation, literPerMeter)).build();

	}

	/**
	 * Method adds a new vehicle Type to the list of vehicleTyps
	 * 
	 * @param
	 * @return
	 */
	private static void adVehicleType() {
		vehicleTypes = new CarrierVehicleTypes();
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

	}

	/**
	 * Method for creating a new Garbage truck
	 * 
	 * @param
	 * 
	 * @return
	 */
	static CarrierVehicle createGarbageTruck(String vehicleName, String linkDepot, double earliestStartingTime,
			double latestFinishingTime) {

		return vehicleAtDepot = CarrierVehicle.Builder
				.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId(linkDepot))
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime)
				.setTypeId(carrierVehType.getId()).build();
	}

	/**
	 * Creates the vehicles at the depots, ads this vehicles to the carriers and
	 * sets the capabilities. This method is for the Berlin network and creates the
	 * vehicles for the 4 different depots.
	 * 
	 * @param
	 */
	static void createCarriersBerlin(Collection<SimpleFeature> districtsWithGarbage, Carriers carriers,
			HashMap<String, Carrier> carrierMap, FleetSize fleetSize) {
		String depotForckenbeck = "27766";
		String depotMalmoeerStr = "116212";
		String depotNordring = "42882";
		String depotGradestrasse = "71781";

		String vehicleIdForckenbeck = "TruckForckenbeck";
		String vehicleIdMalmoeer = "TruckMalmoeer";
		String vehicleIdNordring = "TruckNordring";
		String vehicleIdGradestrasse = "TruckGradestrasse";
		double earliestStartingTime = 6 * 3600;
		double latestFinishingTime = 14 * 3600;

		HashMap<String, CarrierVehicle> depotMap = new HashMap<String, CarrierVehicle>();

		CarrierVehicle vehicleForckenbeck = createGarbageTruck(vehicleIdForckenbeck, depotForckenbeck,
				earliestStartingTime, latestFinishingTime);
		CarrierVehicle vehicleMalmoeerStr = createGarbageTruck(vehicleIdMalmoeer, depotMalmoeerStr,
				earliestStartingTime, latestFinishingTime);
		CarrierVehicle vehicleNordring = createGarbageTruck(vehicleIdNordring, depotNordring, earliestStartingTime,
				latestFinishingTime);
		CarrierVehicle vehicleGradestrasse = createGarbageTruck(vehicleIdGradestrasse, depotGradestrasse,
				earliestStartingTime, latestFinishingTime);
		depotMap.put("Forckenbeck", vehicleForckenbeck);
		depotMap.put("MalmoeerStr", vehicleMalmoeerStr);
		depotMap.put("Nordring", vehicleNordring);
		depotMap.put("Gradestrasse", vehicleGradestrasse);

		// define Carriers

		defineCarriersBerlin(depotMap, districtsWithGarbage, carriers, carrierMap, vehicleForckenbeck,
				vehicleMalmoeerStr, vehicleNordring, vehicleGradestrasse, fleetSize);
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers for the Berlin network
	 * 
	 * @param
	 * 
	 */
	private static void defineCarriersBerlin(HashMap<String, CarrierVehicle> depotMap,
			Collection<SimpleFeature> districtsWithGarbage, Carriers carriers, HashMap<String, Carrier> carrierMap,
			CarrierVehicle vehicleForckenbeck, CarrierVehicle vehicleMalmoeerStr, CarrierVehicle vehicleNordring,
			CarrierVehicle vehicleGradestrasse, FleetSize fleetSize) {

		if (oneCarrierForEachDistrict == false) {
			CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
					.addVehicle(vehicleForckenbeck).setFleetSize(fleetSize).build();
			carrierMap.get("Forckenbeck").setCarrierCapabilities(carrierCapabilities);
			carrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
					.addVehicle(vehicleMalmoeerStr).setFleetSize(fleetSize).build();
			carrierMap.get("MalmoeerStr").setCarrierCapabilities(carrierCapabilities);
			carrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
					.addVehicle(vehicleNordring).setFleetSize(fleetSize).build();
			carrierMap.get("Nordring").setCarrierCapabilities(carrierCapabilities);
			carrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
					.addVehicle(vehicleGradestrasse).setFleetSize(fleetSize).build();
			carrierMap.get("Gradestrasse").setCarrierCapabilities(carrierCapabilities);
		} else {
			for (Carrier carrier : carrierMap.values()) {
				for (SimpleFeature simpleFeature : districtsWithGarbage) {
					if (carrier.getId().toString().equals(simpleFeature.getAttribute("Ortsteilna"))){
					carrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
							.addVehicle(depotMap.get(simpleFeature.getAttribute("Depot"))).setFleetSize(fleetSize).build());}
				}
			}
		}
		// Fahrzeugtypen den Anbietern zuordenen
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
	}

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution
	 * 
	 * @param
	 */
	static void solveWithJsprit(Scenario scenario, Carriers carriers, HashMap<String, Carrier> carrierMap) {

		// Netzwerk integrieren und Kosten für jsprit
		Network network = scenario.getNetwork();
		// Network network = NetworkUtils.readNetwork(original_Chessboard);
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
		netBuilder.setTimeSliceWidth(1800);

		for (Carrier singleCarrier : carrierMap.values()) {
			// Build jsprit, solve and route VRP for carrierService only -> need solution to
			// convert Services to Shipments
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(singleCarrier,
					network);
			vrpBuilder.setRoutingCost(netBasedCosts);
			VehicleRoutingProblem problem = vrpBuilder.build();

			// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			algorithm.setMaxIterations(1);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
			costsJsprit = costsJsprit + bestSolution.getCost();

			// Routing bestPlan to Network
			CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(singleCarrier, bestSolution);
			NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
			singleCarrier.setSelectedPlan(carrierPlanServices);
			noPickup = noPickup + bestSolution.getUnassignedJobs().size();
		}
		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.xml");
		// new Plotter(problem, bestSolution).plot(
		// scenario.getConfig().controler().getOutputDirectory() +
		// "/jsprit_CarrierPlans_Test01.png",
		// "bestSolution");
	}

	/**
	 * @param
	 */
	static void scoringAndManagerFactory(Scenario scenario, Carriers carriers, final Controler controler) {
		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener);
	}

	/**
	 * @param scenario
	 * @return
	 */
	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {

		return new CarrierScoringFunctionFactoryImpl(scenario.getNetwork());
//		return new CarrierScoringFunctionFactoryImpl (scenario, scenario.getConfig().controler().getOutputDirectory()) {
//
//			public ScoringFunction createScoringFunction(final Carrier carrier){
//				SumScoringFunction sumSf = new SumScoringFunction() ;
//
//				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
//				sumSf.addScoringFunction(fixCost);
//
//				LegScoring legScoring = new LegScoring(carrier);
//				sumSf.addScoringFunction(legScoring);
//
//				//Score Activity w/o correction of waitingTime @ 1st Service.
//				//			ActivityScoring actScoring = new ActivityScoring(carrier);
//				//			sumSf.addScoringFunction(actScoring);
//
//				//Alternativ:
//				//Score Activity with correction of waitingTime @ 1st Service.
//				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
//				sumSf.addScoringFunction(actScoring);
//
//				return sumSf;
//			}
//		};
	}

	/**
	 * @return
	 */
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}

	/**
	 * Gives an output of a .txt file with some important information
	 * 
	 * @param allGarbage
	 * 
	 * @param
	 */
	static void outputSummary(Collection<SimpleFeature> districtsWithGarbage, Scenario scenario,
			HashMap<String, Carrier> carrierMap, String day) {
		int vehiclesForckenbeck = 0;
		int vehiclesMalmoeer = 0;
		int vehiclesNordring = 0;
		int vehiclesGradestrasse = 0;
		int vehiclesChessboard = 0;
		int numberVehicles = 0;
		int sizeForckenbeck = 0;
		int sizeMalmooer = 0;
		int sizeNordring = 0;
		int sizeGradestrasse = 0;
		int sizeChessboard = 0;
		int allCollectedGarbage = 0;
		double matsimCosts = 0;
		for (Carrier thisCarrier : carrierMap.values()) {

			Collection<ScheduledTour> tours = thisCarrier.getSelectedPlan().getScheduledTours();
			Collection<CarrierShipment> shipments = thisCarrier.getShipments();
			HashMap<String, Integer> shipmentSizes = new HashMap<String, Integer>();
			matsimCosts = matsimCosts + thisCarrier.getSelectedPlan().getScore();
			for (CarrierShipment carrierShipment : shipments) {
				String shipmentId = carrierShipment.getId().toString();
				int shipmentSize = carrierShipment.getSize();
				shipmentSizes.put(shipmentId, shipmentSize);
			}
			for (ScheduledTour scheduledTour : tours) {
				List<TourElement> elements = scheduledTour.getTour().getTourElements();
				for (TourElement element : elements) {
					if (element instanceof Pickup) {
						Pickup pickupElement = (Pickup) element;
						String pickupShipmentId = pickupElement.getShipment().getId().toString();
						if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckForckenbeck")) {
							sizeForckenbeck = sizeForckenbeck + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckMalmoeer")) {
							sizeMalmooer = sizeMalmooer + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckNordring")) {
							sizeNordring = sizeNordring + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckGradestrasse")) {
							sizeGradestrasse = sizeGradestrasse + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckChessboard")) {
							sizeChessboard = sizeChessboard + (shipmentSizes.get(pickupShipmentId));
						}
					}
				}
				allCollectedGarbage = sizeForckenbeck + sizeMalmooer + sizeNordring + sizeGradestrasse + sizeChessboard;

				if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckForckenbeck")) {
					vehiclesForckenbeck++;
				}
				if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckMalmoeer")) {
					vehiclesMalmoeer++;
				}
				if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckNordring")) {
					vehiclesNordring++;
				}
				if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckGradestrasse")) {
					vehiclesGradestrasse++;
				}
				if (scheduledTour.getVehicle().getVehicleId() == Id.createVehicleId("TruckChessboard")) {
					vehiclesChessboard++;
				}
				numberVehicles = vehiclesForckenbeck + vehiclesMalmoeer + vehiclesNordring + vehiclesGradestrasse
						+ vehiclesChessboard;
			}
		}
		FileWriter writer;
		File file;
		file = new File(scenario.getConfig().controler().getOutputDirectory() + "/01_Zusammenfassung.txt");
		try {
			writer = new FileWriter(file, true);
			if (day != null) {
				writer.write("Anzahl der Gebiete im gesamten Netzwerk:\t\t\t\t\t" + districtsWithGarbage.size() + "\n");
				writer.write("Wochentag:\t\t\t\t\t\t\t\t\t\t\t\t\t" + day + "\n");
			}
			writer.write("\n" + "Anzahl der untersuchten Gebiete mit Abholung:\t\t\t\t" + districtsWithShipments.size()
					+ "\n");
			writer.write("Untersuchte Gebiete mit Abholung:\t\t\t\t\t\t\t" + districtsWithShipments.toString() + "\n");
			if (day != null) {
				writer.write("\n" + "Anzahl der untersuchten Gebiete ohne Abholung:\t\t\t\t"
						+ districtsWithNoShipments.size() + "\n");
				writer.write("Untersuchte Gebiete ohne Abholung:\t\t\t\t\t\t\t" + districtsWithNoShipments.toString()
						+ "\n");
			}

			writer.write("\n" + "Die Summe des abzuholenden Mülls beträgt: \t\t\t\t\t" + ((double) allGarbage) / 1000
					+ " t\n\n");
			writer.write("Anzahl der Abholstellen: \t\t\t\t\t\t\t\t\t" + numberOfShipments + "\n");
			writer.write("Anzahl der Abholstellen ohne Abholung: \t\t\t\t\t\t" + noPickup + "\n\n");
			writer.write("Anzahl der Muellfahrzeuge im Einsatz: \t\t\t\t\t\t" + (numberVehicles) + "\t\tMenge gesamt:\t"
					+ ((double) allCollectedGarbage) / 1000 + " t\n");
			if (day != null) {
				writer.write("\t Anzahl aus dem Betriebshof Forckenbeckstrasse: \t\t\t" + vehiclesForckenbeck
						+ "\t\t\tMenge:\t\t" + ((double) sizeForckenbeck) / 1000 + " t\n");
				writer.write("\t Anzahl aus dem Betriebshof Malmoeer Strasse: \t\t\t\t" + vehiclesMalmoeer
						+ "\t\t\tMenge:\t\t" + ((double) sizeMalmooer) / 1000 + " t\n");
				writer.write("\t Anzahl aus dem Betriebshof Nordring: \t\t\t\t\t\t" + vehiclesNordring
						+ "\t\t\tMenge:\t\t" + ((double) sizeNordring) / 1000 + " t\n");
				writer.write("\t Anzahl aus dem Betriebshof Gradestraße: \t\t\t\t\t" + vehiclesGradestrasse
						+ "\t\t\tMenge:\t\t" + ((double) sizeGradestrasse) / 1000 + " t\n\n");
				writer.write("Anzuliefernde Menge (Soll):\tMHKW Ruhleben:\t\t\t\t\t" + ((double) garbageRuhleben) / 1000
						+ " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Pankow:\t\t\t\t\t\t" + ((double) garbagePankow) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Reinickendorf:\t\t\t\t" + ((double) garbageReinickenD) / 1000 + " t\n");
				writer.write(
						"\t\t\t\t\t\t\tUmladestation Gradestrasse:\t\t" + ((double) garbageGradestr) / 1000 + " t\n");
				writer.write(
						"\t\t\t\t\t\t\tMA Gruenauer Str.:\t\t\t\t" + ((double) garbageGruenauerStr) / 1000 + " t\n");
			}
			writer.write("\n" + "Kosten (Jsprit): \t\t\t\t\t\t\t\t\t\t\t" + (Math.round(costsJsprit)) + " €\n\n");
			writer.write("Kosten (MatSim): \t\t\t\t\t\t\t\t\t\t\t" + ((-1) * Math.round(matsimCosts)) + " €\n");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (noPickup == 0) {
			System.out.println("");
			System.out.println("Abfaelle wurden komplett von " + numberVehicles + " Fahrzeugen eingesammelt!");
		} else {
			System.out.println("");
			System.out.println("Abfall nicht komplett eingesammelt!");
		}
	}

	/**
	 * Creates an output of a summary of important information of the created
	 * shipments
	 * 
	 */
	static void outputSummaryShipments(Scenario scenario, String day, HashMap<String, Carrier> carrierMap) {

		FileWriter writer;
		File file;
		file = new File(scenario.getConfig().controler().getOutputDirectory() + "/01_ZusammenfassungShipments.txt");
		try {
			writer = new FileWriter(file, true);
			writer.write("Anzahl der Abholgebiete:\t\t\t\t\t\t\t\t\t" + districtsWithShipments.size() + "\n");
			writer.write("Abholgebiete:\t\t\t\t\t\t\t\t\t\t\t\t" + districtsWithShipments.toString() + "\n");
			if (day != null)
				writer.write("Wochentag:\t\t\t\t\t\t\t\t\t\t\t\t\t" + day + "\n");
			writer.write("\n" + "Die Summe des abzuholenden Mülls beträgt: \t\t\t\t\t" + ((double) allGarbage) / 1000
					+ " t\n\n");
			writer.write("Anzahl der Abholstellen: \t\t\t\t\t\t\t\t\t" + numberOfShipments + "\n");
			if (day != null) {
				for (Carrier carrier : carrierMap.values()) {
					writer.write("\t\t\t\t\t\t\t" + carrier.getId().toString() + ":\t\t\t\t\t\t"
							+ carrier.getShipments().size() + "\n");
				}
				writer.write("\n" + "Anzuliefernde Menge (Soll):\tMHKW Ruhleben:\t\t\t\t\t"
						+ ((double) garbageRuhleben) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Pankow:\t\t\t\t\t\t" + ((double) garbagePankow) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Reinickendorf:\t\t\t\t" + ((double) garbageReinickenD) / 1000 + " t\n");
				writer.write(
						"\t\t\t\t\t\t\tUmladestation Gradestrasse:\t\t" + ((double) garbageGradestr) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMA Gruenauer Str.:\t\t\t\t" + ((double) garbageGruenauerStr) / 1000 + " t");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
