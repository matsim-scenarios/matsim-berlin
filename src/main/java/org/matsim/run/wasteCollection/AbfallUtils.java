package org.matsim.run.wasteCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.VehicleType;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

/**
 * @author Ricardo Ewert
 *
 */
class AbfallUtils {

	static final Logger log = Logger.getLogger(AbfallUtils.class);

	static int amountOfCollectedDustbins;
	static int matsimIterations;
	static int jspritIterations;
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
	static Multimap<String, String> linksInDistricts;
	static boolean streetAlreadyInGarbageLinks = false;

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
	static HashMap<String, Carrier> createCarrier(Carriers carriers) {
		HashMap<String, Carrier> carrierMap = new HashMap<String, Carrier>();

		carrierMap.put("Nordring", carriers.getCarriers().get(Id.create("BSR_Nordring", Carrier.class)));
		carrierMap.put("MalmoeerStr", carriers.getCarriers().get(Id.create("BSR_MalmoeerStr", Carrier.class)));
		carrierMap.put("Forckenbeck", carriers.getCarriers().get(Id.create("BSR_Forckenbeck", Carrier.class)));
		carrierMap.put("Gradestrasse", carriers.getCarriers().get(Id.create("BSR_Gradestrasse", Carrier.class)));

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
					linksInDistricts.put(district.getAttribute("Ortsteil").toString(), link.getId().toString());
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
	static Config prepareConfig(Config config, int lastIteration, String inputVehicleTypes, String inputCarriers) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastIteration);
		matsimIterations = lastIteration + 1;
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(TransformationFactory.GK4);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setCarriersFile(inputCarriers);
		freightConfigGroup.setCarriersVehicleTypesFile(inputVehicleTypes);
		freightConfigGroup.setTravelTimeSliceWidth(1800);
		freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);

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
			double volumeBigDustbin, double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		int garbageToCollect = 0;
		String depot = null;
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		createMapEnt();
		for (String districtToCollect : districtsForShipments) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteil").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						garbageToCollect = (int) ((double) districtInformation.getAttribute(day) * 1000);
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 14 && link.getAllowedModes().contains("car")) {
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
						log.warn("At District " + districtInformation.getAttribute("Ortsteil").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}
				}

			}
			if (garbageLinks.size() != 0) {
				districtsWithShipments.add(districtToCollect);
				createShipmentsForCarrierII(garbageToCollect, volumeBigDustbin, serviceTimePerBigTrashcan,
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
			Map<Id<Link>, ? extends Link> allLinks, double volumeBigDustbin, double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		String depot = null;
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		createMapEnt();
		for (String districtToCollect : areasForShipmentPerMeterMap.keySet()) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteil").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 14 && link.getAllowedModes().contains("car")) {
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
						log.warn("At District " + districtInformation.getAttribute("Ortsteil").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}

				}

			}
			if (garbageLinks.size() != 0)
				districtsWithShipments.add(districtToCollect);
			double garbagePerMeterToCollect = areasForShipmentPerMeterMap.get(districtToCollect);
			createShipmentsForCarrierI(garbagePerMeterToCollect, volumeBigDustbin, serviceTimePerBigTrashcan,
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
			Map<Id<Link>, ? extends Link> allLinks, double volumeBigDustbin, double serviceTimePerBigTrashcan) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		String depot = null;
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		createMapEnt();
		for (String districtToCollect : areasForShipmentPerVolumeMap.keySet()) {
			for (SimpleFeature districtInformation : districtsWithGarbage) {
				if (districtInformation.getAttribute("Ortsteil").equals(districtToCollect)) {
					if ((double) districtInformation.getAttribute(day) > 0) {
						dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
						depot = districtInformation.getAttribute("Depot").toString();
						for (Link link : allLinks.values()) {
							for (String linkInDistrict : linksInDistricts.get(districtToCollect)) {
								if (Id.createLinkId(linkInDistrict) == link.getId()) {
									if (link.getFreespeed() < 14 && link.getAllowedModes().contains("car")) {
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
						log.warn("At District " + districtInformation.getAttribute("Ortsteil").toString()
								+ " no garbage will be collected at " + day);
						districtsWithNoShipments.add(districtToCollect);
					}

				}

			}
			if (garbageLinks.size() != 0)
				districtsWithShipments.add(districtToCollect);
			int garbageVolumeToCollect = areasForShipmentPerVolumeMap.get(districtToCollect);
			createShipmentsForCarrierII(garbageVolumeToCollect, volumeBigDustbin, serviceTimePerBigTrashcan,
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
			HashMap<String, Carrier> carrierMap, Map<Id<Link>, ? extends Link> allLinks, double volumeBigTrashcan,
			double serviceTimePerBigDustbin, boolean oneCarrierForEachDistrict) {
		Id<Link> dumpId = null;
		double distanceWithShipments = 0;
		int garbageToCollect = 0;
		String usedCarrier = null;
		String district = null;
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		createMapEnt();
//		carrierMap.clear();
		for (SimpleFeature districtInformation : districtsWithGarbage) {
			if ((double) districtInformation.getAttribute(day) > 0) {
				garbageToCollect = (int) ((double) districtInformation.getAttribute(day) * 1000);
				dumpId = garbageDumps.get(districtInformation.getAttribute(dataEnt.get(day)));
				usedCarrier = districtInformation.getAttribute("Depot").toString();
				if (oneCarrierForEachDistrict == true) {
					district = districtInformation.getAttribute("Ortsteil").toString();
					Carrier newCarrier = createSingleCarrier(usedCarrier, carrierMap, district);
					carrierMap.put(district, newCarrier);
					usedCarrier = district;
				}
				for (Link link : allLinks.values()) {
					for (String linkInDistrict : linksInDistricts
							.get(districtInformation.getAttribute("Ortsteil").toString())) {
						if (Id.createLinkId(linkInDistrict) == link.getId()) {
							if (link.getFreespeed() < 14 && link.getAllowedModes().contains("car")) {
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
				log.warn("At District " + districtInformation.getAttribute("Ortsteil").toString()
						+ " no garbage will be collected at " + day);
			}

			if (garbageLinks.size() != 0) {
				districtsWithShipments.add(districtInformation.getAttribute("Ortsteil").toString());

				createShipmentsForCarrierII(garbageToCollect, volumeBigTrashcan, serviceTimePerBigDustbin,
						distanceWithShipments, garbageLinks, scenario,
						carrierMap.get(usedCarrier), dumpId, carriers);
			}
			distanceWithShipments = 0;
			garbageLinks.clear();
		}
		if (oneCarrierForEachDistrict == true) {
			carrierMap.remove("Nordring");
			carrierMap.remove("MalmoeerStr");
			carrierMap.remove("Forckenbeck");
			carrierMap.remove("Gradestrasse");
		}
		carriers.getCarriers().clear();
		for (Carrier carrier : carrierMap.values()) {
			carriers.addCarrier(carrier);
		}
	}

	private static Carrier createSingleCarrier(String depot, HashMap<String, Carrier> carrierMap, String district) {
		Carrier newCarrier = CarrierUtils.createCarrier(Id.create("Carrier " + district, Carrier.class));
		for (Carrier originalCarrier : carrierMap.values()) {
			if (originalCarrier.getId().toString().contains(depot)) {
				newCarrier.getCarrierCapabilities()
						.setFleetSize(originalCarrier.getCarrierCapabilities().getFleetSize());
				newCarrier.getCarrierCapabilities().getCarrierVehicles()
						.putAll(originalCarrier.getCarrierCapabilities().getCarrierVehicles());
				newCarrier.getCarrierCapabilities().getVehicleTypes()
						.addAll(originalCarrier.getCarrierCapabilities().getVehicleTypes());
			}
		}

		return newCarrier;
	}

	/**
	 * Creates a Shipment for every garbagelink and ads all shipments to myCarrier.
	 * The volumeGarbage is in garbage per meter. So the volumeGarbage of every
	 * shipment depends of the input garbagePerMeterToCollect.
	 * 
	 * @param
	 */
	static void createShipmentsForCarrierI(double garbagePerMeterToCollect, double volumeBigDustbin,
			double serviceTimePerBigTrashcan, Map<Id<Link>, Link> garbageLinks, Scenario scenario, Carrier thisCarrier,
			Id<Link> dumpId, Carriers carriers) {

		for (Link link : garbageLinks.values()) {
			double maxWeightBigDustbin = volumeBigDustbin * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
			int volumeGarbage = (int) Math.ceil(link.getLength() * garbagePerMeterToCollect);
			amountOfCollectedDustbins = amountOfCollectedDustbins
					+ (int) Math.ceil(((double) volumeGarbage) / maxWeightBigDustbin);
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigDustbin) * serviceTimePerBigTrashcan;
			// double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * 60;
			double deliveryTime = ((double) volumeGarbage / 11000) * 45 * 60;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(), (dumpId),
							volumeGarbage)
					.setPickupServiceTime(serviceTime).setPickupTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600))
					.setDeliveryServiceTime(deliveryTime).build();
			thisCarrier.getShipments().put(shipment.getId(), shipment);
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
	static void createShipmentsForCarrierII(int garbageToCollect, double volumeBigDustbin,
			double serviceTimePerBigTrashcan, double distanceWithShipments, Map<Id<Link>, Link> garbageLinks,
			Scenario scenario, Carrier thisCarrier, Id<Link> garbageDumpId, Carriers carriers) {
		int count = 1;
		int garbageCount = 0;
		double roundingError = 0;
		for (Link link : garbageLinks.values()) {
			double maxWeightBigDustbin = volumeBigDustbin * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
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
			amountOfCollectedDustbins = amountOfCollectedDustbins
					+ (int) Math.ceil(((double) volumeGarbage) / maxWeightBigDustbin);
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigDustbin) * serviceTimePerBigTrashcan;
//			double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * 60;
			double deliveryTime = ((double) volumeGarbage / 11000) * 45 * 60;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(),
							garbageDumpId, volumeGarbage)
					.setPickupServiceTime(serviceTime).setPickupTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600))
					.setDeliveryServiceTime(deliveryTime).build();
			thisCarrier.getShipments().put(shipment.getId(), shipment);
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
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution
	 * 
	 * @param
	 */
	static void solveWithJsprit(Scenario scenario, Carriers carriers, HashMap<String, Carrier> carrierMap,
			int jspritIteration) {

		int carrierCount = 1;
		CarrierVehicleTypes vehicleTypes = (CarrierVehicleTypes) scenario.getScenarioElement("carrierVehicleTypes");
		Network network = scenario.getNetwork();
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
			log.info("Creating solution for carrier " + carrierCount + " of " + carrierMap.size() + " Carriers");
			algorithm.setMaxIterations(jspritIterations = jspritIteration);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
			costsJsprit = costsJsprit + bestSolution.getCost();

			// Routing bestPlan to Network
			CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(singleCarrier, bestSolution);
			NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
			singleCarrier.setSelectedPlan(carrierPlanServices);
			noPickup = noPickup + bestSolution.getUnassignedJobs().size();
			carrierCount++;
			if (singleCarrier.getId() == Id.create("Carrier_Chessboard", Carrier.class))
				new Plotter(problem, bestSolution).plot(
						scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.png",
						"bestSolution");
		}
		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans.xml");

	}

//	/**
//	 * @param
//	 */
//	static void scoringAndManagerFactory(Scenario scenario, final Controler controler) {
//		controler.addOverridingModule(new CarrierModule());
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bind(CarrierScoringFunctionFactory.class).toInstance(createMyScoringFunction2(scenario));
//				bind(CarrierPlanStrategyManagerFactory.class).toInstance(createMyStrategymanager());
//			}
//		});
//	}
	static Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		Freight.configure(controler);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CarrierModule());
//                bind(CarrierPlanStrategyManagerFactory.class).toInstance( null );
//                bind(CarrierScoringFunctionFactory.class).toInstance(null );
			}
		});

		return controler;
	}
	/**
	 * @param scenario
	 * @return
	 */
//	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {
//
//		return new CarrierScoringFunctionFactoryImpl(scenario.getNetwork());
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
//	}

//	/**
//	 * @return
//	 */
//	private static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
//		return new CarrierPlanStrategyManagerFactory() {
//			@Override
//			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
//				return null;
//			}
//		};
//	}

	/**
	 * Gives an output of a .txt file with some important information
	 * 
	 * @param allGarbage
	 * 
	 * @param
	 */
	static void outputSummary(Collection<SimpleFeature> districtsWithGarbage, Scenario scenario,
			HashMap<String, Carrier> carrierMap, String day, double volumeDustbin,
			double secondsServiceTimePerDustbin) {
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
		int sizeRuhleben = 0;
		int sizePankow = 0;
		int sizeReinickendorf = 0;
		int sizeUmladestationGradestrasse = 0;
		int sizeGruenauerStr = 0;
		int sizeChessboardDelivery = 0;
		double distanceTour = 0;
		double powerConsumptionTour = 0;
		int sizeTour = 0;
		int carrierWithShipments = 0;
		double matsimCosts = 0;
		List<Double> tourDistancesNordring = new ArrayList<Double>();
		List<Double> tourDistancesForckenbeck = new ArrayList<Double>();
		List<Double> tourDistancesMalmoeerStr = new ArrayList<Double>();
		List<Double> tourDistancesGradestrasse = new ArrayList<Double>();
		List<Double> tourDistancesChessboard = new ArrayList<Double>();
		List<Double> powerConsumptionTourNordring = new ArrayList<Double>();
		List<Double> powerConsumptionTourForckenbeck = new ArrayList<Double>();
		List<Double> powerConsumptionTourMalmoeerStr = new ArrayList<Double>();
		List<Double> powerConsumptionTourGradestrasse = new ArrayList<Double>();
		List<Double> powerConsumptionTourChessboard = new ArrayList<Double>();
		double maxTourForckenbeck = 0;
		double minTourForckenbeck = 0;
		double distanceToursForckenbeck = 0;
		double averageTourDistanceForckenbeck = 0;
		double maxTourNordring = 0;
		double minTourNordring = 0;
		double distanceToursNordring = 0;
		double averageTourDistanceNordring = 0;
		double maxTourMalmoeerStr = 0;
		double minTourMalmoeerStr = 0;
		double distanceToursMalmoeerStr = 0;
		double averageTourDistanceMalmoeerStr = 0;
		double maxTourGradestrasse = 0;
		double minTourGradestrasse = 0;
		double distanceToursGradestrasse = 0;
		double averageTourDistanceGradestrasse = 0;
		double maxPowerConsumptionForckenbeck = 0;
		double minPowerConsumptionForckenbeck = 0;
		double averagePowerConsumptionForckenbeck = 0;
		double powerConsumptionForckenbeck = 0;
		double maxPowerConsumptionNordring = 0;
		double minPowerConsumptionNordring = 0;
		double averagePowerConsumptionNordring = 0;
		double powerConsumptionNordring = 0;
		double maxPowerConsumptionMalmoeerStr = 0;
		double minPowerConsumptionMalmoeerStr = 0;
		double averagePowerConsumptionMalmoeerStr = 0;
		double powerConsumptionMalmoeerStr = 0;
		double maxPowerConsumptionGradestrasse = 0;
		double minPowerConsumptionGradestrasse = 0;
		double averagePowerConsumptionGradestrasse = 0;
		double powerConsumptionGradestrasse = 0;
		double tourDistanceChessboard = 0;
		double powerConsumptionChessboard = 0;
		boolean electricCar = false;
		double capacityTruck = 0;
		String vehicleTypeId = null;
		double energyConsumptionPerDistance = 0;
		double energyConsumptionPerWeight = 0;

		Carriers allCarriers = (Carriers) scenario.getScenarioElement("carriers");
		Carrier testCarrier = allCarriers.getCarriers().values().iterator().next();
		for (VehicleType usedType : testCarrier.getCarrierCapabilities().getVehicleTypes()) {
			if (usedType.getEngineInformation().getAttributes().getAttribute("fuelType").equals("electric")) {
				electricCar = true;
				energyConsumptionPerDistance = (double) usedType.getEngineInformation().getAttributes()
						.getAttribute("engeryConsumptionPerKm");
				energyConsumptionPerWeight = 1.4;
			}
			capacityTruck = usedType.getCapacity().getOther();
			vehicleTypeId = usedType.getId().toString();
		}

		for (Carrier thisCarrier : carrierMap.values()) {

			Collection<ScheduledTour> tours = thisCarrier.getSelectedPlan().getScheduledTours();
			Map<Id<CarrierShipment>, CarrierShipment> shipments = thisCarrier.getShipments();
			HashMap<String, Integer> shipmentSizes = new HashMap<String, Integer>();
			matsimCosts = matsimCosts + thisCarrier.getSelectedPlan().getScore();

			for (Entry<Id<CarrierShipment>, CarrierShipment> entry : shipments.entrySet()) {
				String shipmentId = entry.getKey().toString();
				int shipmentSize = entry.getValue().getSize();
				shipmentSizes.put(shipmentId, shipmentSize);
			}
			for (ScheduledTour scheduledTour : tours) {
				distanceTour = 0;
				powerConsumptionTour = 0;
				sizeTour = 0;
				List<TourElement> elements = scheduledTour.getTour().getTourElements();
				for (TourElement element : elements) {
					if (element instanceof Pickup) {
						Pickup pickupElement = (Pickup) element;
						String pickupShipmentId = pickupElement.getShipment().getId().toString();
						if (scheduledTour.getVehicle().getId().toString().contains("TruckForckenbeck")) {
							sizeForckenbeck = sizeForckenbeck + (shipmentSizes.get(pickupShipmentId));
							sizeTour = sizeTour + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getId().toString().contains("TruckMalmoeer")) {
							sizeMalmooer = sizeMalmooer + (shipmentSizes.get(pickupShipmentId));
							sizeTour = sizeTour + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getId().toString().contains("TruckNordring")) {
							sizeNordring = sizeNordring + (shipmentSizes.get(pickupShipmentId));
							sizeTour = sizeTour + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getId().toString().contains("TruckGradestrasse")) {
							sizeGradestrasse = sizeGradestrasse + (shipmentSizes.get(pickupShipmentId));
							sizeTour = sizeTour + (shipmentSizes.get(pickupShipmentId));
						}
						if (scheduledTour.getVehicle().getId().toString().contains("TruckChessboard")) {
							sizeChessboard = sizeChessboard + (shipmentSizes.get(pickupShipmentId));
							sizeTour = sizeTour + (shipmentSizes.get(pickupShipmentId));
						}
					}
					if (element instanceof Delivery) {
						Delivery deliveryElement = (Delivery) element;
						String deliveryShipmentId = deliveryElement.getShipment().getId().toString();
						if (deliveryElement.getLocation() == Id.createLinkId(linkMhkwRuhleben)) {
							sizeRuhleben = sizeRuhleben + (shipmentSizes.get(deliveryShipmentId));
						}
						if (deliveryElement.getLocation() == Id.createLinkId(linkMpsPankow)) {
							sizePankow = sizePankow + (shipmentSizes.get(deliveryShipmentId));
						}
						if (deliveryElement.getLocation() == Id.createLinkId(linkMpsReinickendorf)) {
							sizeReinickendorf = sizeReinickendorf + (shipmentSizes.get(deliveryShipmentId));
						}
						if (deliveryElement.getLocation() == Id.createLinkId(linkUmladestationGradestrasse)) {
							sizeUmladestationGradestrasse = sizeUmladestationGradestrasse
									+ (shipmentSizes.get(deliveryShipmentId));
						}
						if (deliveryElement.getLocation() == Id.createLinkId(linkGruenauerStr)) {
							sizeGruenauerStr = sizeGruenauerStr + (shipmentSizes.get(deliveryShipmentId));
						}
						if (scheduledTour.getVehicle().getId() == Id.createVehicleId("TruckChessboard")) {
							sizeChessboardDelivery = sizeChessboardDelivery + (shipmentSizes.get(deliveryShipmentId));
						}
					}
					if (element instanceof Leg) {
						Leg legElement = (Leg) element;
						if (legElement.getRoute().getDistance() != 0)
							distanceTour = distanceTour + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(),
									0, 0, scenario.getNetwork());

					}
				}
				allCollectedGarbage = sizeForckenbeck + sizeMalmooer + sizeNordring + sizeGradestrasse + sizeChessboard;
				powerConsumptionTour = (double) (distanceTour / 1000) * energyConsumptionPerDistance
						+ (double) (sizeTour / 1000) * energyConsumptionPerWeight;

				if (scheduledTour.getVehicle().getId().toString().contains("TruckForckenbeck")) {
					tourDistancesForckenbeck.add(vehiclesForckenbeck, (double) Math.round(distanceTour / 1000));
					powerConsumptionTourForckenbeck.add(vehiclesForckenbeck, (double) Math.round(powerConsumptionTour));
					vehiclesForckenbeck++;
				}
				if (scheduledTour.getVehicle().getId().toString().contains("TruckMalmoeer")) {
					tourDistancesMalmoeerStr.add(vehiclesMalmoeer, (double) Math.round(distanceTour / 1000));
					powerConsumptionTourMalmoeerStr.add(vehiclesMalmoeer, (double) Math.round(powerConsumptionTour));
					vehiclesMalmoeer++;
				}
				if (scheduledTour.getVehicle().getId().toString().contains("TruckNordring")) {
					tourDistancesNordring.add(vehiclesNordring, (double) Math.round(distanceTour / 1000));
					powerConsumptionTourNordring.add(vehiclesNordring, (double) Math.round(powerConsumptionTour));
					vehiclesNordring++;
				}
				if (scheduledTour.getVehicle().getId().toString().contains("TruckGradestrasse")) {
					tourDistancesGradestrasse.add(vehiclesGradestrasse, (double) Math.round(distanceTour / 1000));
					powerConsumptionTourGradestrasse.add(vehiclesGradestrasse,
							(double) Math.round(powerConsumptionTour));
					vehiclesGradestrasse++;
				}
				if (scheduledTour.getVehicle().getId().toString().contains("TruckChessboard")) {
					tourDistancesChessboard.add(vehiclesChessboard, (double) Math.round(distanceTour / 1000));
					tourDistanceChessboard = tourDistanceChessboard + tourDistancesChessboard.get(vehiclesChessboard);
					powerConsumptionTourChessboard.add(vehiclesChessboard, (double) Math.round(powerConsumptionTour));
					powerConsumptionChessboard = powerConsumptionChessboard
							+ powerConsumptionTourChessboard.get(vehiclesChessboard);
					vehiclesChessboard++;
				}
				numberVehicles = vehiclesForckenbeck + vehiclesMalmoeer + vehiclesNordring + vehiclesGradestrasse
						+ vehiclesChessboard;
			}
			if (thisCarrier.getShipments().size() > 0)
				carrierWithShipments++;
		}
		if (vehiclesForckenbeck > 0) {
			maxTourForckenbeck = tourDistancesForckenbeck.get(0);
			minTourForckenbeck = tourDistancesForckenbeck.get(0);
			distanceToursForckenbeck = tourDistancesForckenbeck.get(0);
			maxPowerConsumptionForckenbeck = powerConsumptionTourForckenbeck.get(0);
			minPowerConsumptionForckenbeck = powerConsumptionTourForckenbeck.get(0);
			powerConsumptionForckenbeck = powerConsumptionTourForckenbeck.get(0);
			for (int index = 1; index < tourDistancesForckenbeck.size(); index++) {
				if (tourDistancesForckenbeck.get(index) > maxTourForckenbeck)
					maxTourForckenbeck = tourDistancesForckenbeck.get(index);
				if (tourDistancesForckenbeck.get(index) < minTourForckenbeck)
					minTourForckenbeck = tourDistancesForckenbeck.get(index);
				if (powerConsumptionTourForckenbeck.get(index) > maxPowerConsumptionForckenbeck)
					maxPowerConsumptionForckenbeck = powerConsumptionTourForckenbeck.get(index);
				if (powerConsumptionTourForckenbeck.get(index) < minPowerConsumptionForckenbeck)
					minPowerConsumptionForckenbeck = powerConsumptionTourForckenbeck.get(index);
				distanceToursForckenbeck = distanceToursForckenbeck + tourDistancesForckenbeck.get(index);
				powerConsumptionForckenbeck = powerConsumptionForckenbeck + powerConsumptionTourForckenbeck.get(index);
			}
			averageTourDistanceForckenbeck = distanceToursForckenbeck / vehiclesForckenbeck;
			averagePowerConsumptionForckenbeck = powerConsumptionForckenbeck / vehiclesForckenbeck;
		}
		if (vehiclesMalmoeer > 0) {
			maxTourMalmoeerStr = tourDistancesMalmoeerStr.get(0);
			minTourMalmoeerStr = tourDistancesMalmoeerStr.get(0);
			distanceToursMalmoeerStr = tourDistancesMalmoeerStr.get(0);
			maxPowerConsumptionMalmoeerStr = powerConsumptionTourMalmoeerStr.get(0);
			minPowerConsumptionMalmoeerStr = powerConsumptionTourMalmoeerStr.get(0);
			powerConsumptionMalmoeerStr = powerConsumptionTourMalmoeerStr.get(0);
			for (int index = 1; index < tourDistancesMalmoeerStr.size(); index++) {
				if (tourDistancesMalmoeerStr.get(index) > maxTourMalmoeerStr)
					maxTourMalmoeerStr = tourDistancesMalmoeerStr.get(index);
				if (tourDistancesMalmoeerStr.get(index) < minTourMalmoeerStr)
					minTourMalmoeerStr = tourDistancesMalmoeerStr.get(index);
				if (powerConsumptionTourMalmoeerStr.get(index) > maxPowerConsumptionMalmoeerStr)
					maxPowerConsumptionMalmoeerStr = powerConsumptionTourMalmoeerStr.get(index);
				if (powerConsumptionTourMalmoeerStr.get(index) < minPowerConsumptionMalmoeerStr)
					minPowerConsumptionMalmoeerStr = powerConsumptionTourMalmoeerStr.get(index);
				distanceToursMalmoeerStr = distanceToursMalmoeerStr + tourDistancesMalmoeerStr.get(index);
				powerConsumptionMalmoeerStr = powerConsumptionMalmoeerStr + powerConsumptionTourMalmoeerStr.get(index);
			}
			averageTourDistanceMalmoeerStr = distanceToursMalmoeerStr / vehiclesMalmoeer;
			averagePowerConsumptionMalmoeerStr = powerConsumptionMalmoeerStr / vehiclesMalmoeer;
		}
		if (vehiclesNordring > 0) {
			maxTourNordring = tourDistancesNordring.get(0);
			minTourNordring = tourDistancesNordring.get(0);
			distanceToursNordring = tourDistancesNordring.get(0);
			maxPowerConsumptionNordring = powerConsumptionTourNordring.get(0);
			minPowerConsumptionNordring = powerConsumptionTourNordring.get(0);
			powerConsumptionNordring = powerConsumptionTourNordring.get(0);
			for (int index = 1; index < tourDistancesNordring.size(); index++) {
				if (tourDistancesNordring.get(index) > maxTourNordring)
					maxTourNordring = tourDistancesNordring.get(index);
				if (tourDistancesNordring.get(index) < minTourNordring)
					minTourNordring = tourDistancesNordring.get(index);
				if (powerConsumptionTourNordring.get(index) > maxPowerConsumptionNordring)
					maxPowerConsumptionNordring = powerConsumptionTourNordring.get(index);
				if (powerConsumptionTourNordring.get(index) < minPowerConsumptionNordring)
					minPowerConsumptionNordring = powerConsumptionTourNordring.get(index);
				distanceToursNordring = distanceToursNordring + tourDistancesNordring.get(index);
				powerConsumptionNordring = powerConsumptionNordring + powerConsumptionTourNordring.get(index);
			}
			averageTourDistanceNordring = distanceToursNordring / vehiclesNordring;
			averagePowerConsumptionNordring = powerConsumptionNordring / vehiclesNordring;
		}
		if (vehiclesGradestrasse > 0) {
			maxTourGradestrasse = tourDistancesGradestrasse.get(0);
			minTourGradestrasse = tourDistancesForckenbeck.get(0);
			distanceToursGradestrasse = tourDistancesGradestrasse.get(0);
			maxPowerConsumptionGradestrasse = powerConsumptionTourGradestrasse.get(0);
			minPowerConsumptionGradestrasse = powerConsumptionTourGradestrasse.get(0);
			powerConsumptionGradestrasse = powerConsumptionTourGradestrasse.get(0);
			for (int index = 1; index < tourDistancesGradestrasse.size(); index++) {
				if (tourDistancesGradestrasse.get(index) > maxTourGradestrasse)
					maxTourGradestrasse = tourDistancesGradestrasse.get(index);
				if (tourDistancesGradestrasse.get(index) < minTourGradestrasse)
					minTourGradestrasse = tourDistancesGradestrasse.get(index);
				if (powerConsumptionTourGradestrasse.get(index) > maxPowerConsumptionGradestrasse)
					maxPowerConsumptionGradestrasse = powerConsumptionTourGradestrasse.get(index);
				if (powerConsumptionTourGradestrasse.get(index) < minPowerConsumptionGradestrasse)
					minPowerConsumptionGradestrasse = powerConsumptionTourGradestrasse.get(index);
				distanceToursGradestrasse = distanceToursGradestrasse + tourDistancesGradestrasse.get(index);
				powerConsumptionGradestrasse = powerConsumptionGradestrasse
						+ powerConsumptionTourGradestrasse.get(index);
			}
			averageTourDistanceGradestrasse = distanceToursGradestrasse / vehiclesGradestrasse;
			averagePowerConsumptionGradestrasse = powerConsumptionGradestrasse / vehiclesGradestrasse;
		}

		FileWriter writer;
		File file;
		file = new File(scenario.getConfig().controler().getOutputDirectory() + "/01_Zusammenfassung.txt");
		try {
			writer = new FileWriter(file, true);
			if (day != null) {
				writer.write("Anzahl der Gebiete im gesamten Netzwerk:\t\t\t\t\t" + districtsWithGarbage.size() + "\n");
				writer.write("Wochentag:\t\t\t\t\t\t\t\t\t\t\t\t\t" + day + "\n\n");
			}
			writer.write(
					"Anzahl der untersuchten Gebiete mit Abholung:\t\t\t\t" + districtsWithShipments.size() + "\n");
			writer.write("Untersuchte Gebiete mit Abholung:\t\t\t\t\t\t\t" + districtsWithShipments.toString() + "\n");
			if (day != null) {
				writer.write("\n" + "Anzahl der untersuchten Gebiete ohne Abholung:\t\t\t\t"
						+ districtsWithNoShipments.size() + "\n");
				writer.write("Untersuchte Gebiete ohne Abholung:\t\t\t\t\t\t\t" + districtsWithNoShipments.toString()
						+ "\n");
			}
			writer.write("\n" + "Fahrzeug: \t\t\t\t\t\t\t\t\t\t\t\t\t" + vehicleTypeId + "\n");
			writer.write(
					"Kapazität je Fahrzeug: \t\t\t\t\t\t\t\t\t\t" + ((double) capacityTruck / 1000) + " Tonnen\n\n");
			writer.write("Volumen der Mülltonne: \t\t\t\t\t\t\t\t\t\t" + volumeDustbin + " Liter\n");
			writer.write(
					"ServiceTime pro Mülltonne:\t\t\t\t\t\t\t\t\t" + secondsServiceTimePerDustbin + " Sekunden\n\n");
			writer.write("Iterationen jsprit:\t\t\t\t\t\t\t\t\t\t\t" + jspritIterations + "\n");
			writer.write("Iterationen MATSim:\t\t\t\t\t\t\t\t\t\t\t" + matsimIterations + "\n");
			writer.write("\n" + "Die Summe des abzuholenden Mülls beträgt: \t\t\t\t\t" + ((double) allGarbage) / 1000
					+ " t\n\n");
			writer.write("Anzahl der Abholstellen: \t\t\t\t\t\t\t\t\t" + numberOfShipments + "\n");
			writer.write("Anzahl der Abholstellen ohne Abholung: \t\t\t\t\t\t" + noPickup + "\n\n");
			writer.write("Anzahl der Carrier mit Shipments:\t\t\t\t\t\t\t" + carrierWithShipments + "\n\n");
			writer.write("Anzahl der entleerten Mülltonnen:\t\t\t\t\t\t\t" + amountOfCollectedDustbins + "\n\n");
			writer.write("Anzahl der Muellfahrzeuge im Einsatz: \t\t\t\t\t\t" + (numberVehicles) + "\t\tMenge gesamt:\t"
					+ ((double) allCollectedGarbage) / 1000 + " t\n\n");
			if (day != null) {
				writer.write("\t Anzahl aus dem Betriebshof Forckenbeckstrasse: \t\t\t" + vehiclesForckenbeck
						+ "\t\t\tMenge:\t\t" + ((double) sizeForckenbeck) / 1000 + " t\n");
				if (vehiclesForckenbeck > 0) {
					writer.write("\t\t\tFahrstrecke Summe:\t\t\t\t" + distanceToursForckenbeck + " km\n");
					writer.write("\t\t\tFahrstrecke Max:\t\t\t\t" + maxTourForckenbeck + " km\n");
					writer.write("\t\t\tFahrstrecke Min:\t\t\t\t" + minTourForckenbeck + " km\n");
					writer.write("\t\t\tFahrstrecke Durchschnitt:\t\t" + Math.round(averageTourDistanceForckenbeck)
							+ " km\n");

					if (electricCar == true) {
						writer.write("\t\t\tEnergieverbrauch Summe:\t\t\t" + powerConsumptionForckenbeck + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Max:\t\t\t" + maxPowerConsumptionForckenbeck + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Min:\t\t\t" + minPowerConsumptionForckenbeck + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Durchschnitt:\t"
								+ Math.round(averagePowerConsumptionForckenbeck) + " kwh\n");
					}
				}
				writer.write("\n" + "\t Anzahl aus dem Betriebshof Malmoeer Strasse: \t\t\t\t" + vehiclesMalmoeer
						+ "\t\t\tMenge:\t\t" + ((double) sizeMalmooer) / 1000 + " t\n");
				if (vehiclesMalmoeer > 0) {
					writer.write("\t\t\tFahrstrecke Summe:\t\t\t\t" + distanceToursMalmoeerStr + " km\n");
					writer.write("\t\t\tFahrstrecke Max:\t\t\t\t" + maxTourMalmoeerStr + " km\n");
					writer.write("\t\t\tFahrstrecke Min:\t\t\t\t" + minTourMalmoeerStr + " km\n");
					writer.write("\t\t\tFahrstrecke Durchschnitt:\t\t" + Math.round(averageTourDistanceMalmoeerStr)
							+ " km\n");
					if (electricCar == true) {
						writer.write("\t\t\tEnergieverbrauch Summe:\t\t\t" + powerConsumptionMalmoeerStr + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Max:\t\t\t" + maxPowerConsumptionMalmoeerStr + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Min:\t\t\t" + minPowerConsumptionMalmoeerStr + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Durchschnitt:\t"
								+ Math.round(averagePowerConsumptionMalmoeerStr) + " kw/h\n");
					}
				}
				writer.write("\n" + "\t Anzahl aus dem Betriebshof Nordring: \t\t\t\t\t\t" + vehiclesNordring
						+ "\t\t\tMenge:\t\t" + ((double) sizeNordring) / 1000 + " t\n");
				if (vehiclesNordring > 0) {
					writer.write("\t\t\tFahrstrecke Summe:\t\t\t\t" + distanceToursNordring + " km\n");
					writer.write("\t\t\tFahrstrecke Max:\t\t\t\t" + maxTourNordring + " km\n");
					writer.write("\t\t\tFahrstrecke Min:\t\t\t\t" + minTourNordring + " km\n");
					writer.write(
							"\t\t\tFahrstrecke Durchschnitt:\t\t" + Math.round(averageTourDistanceNordring) + " km\n");
					if (electricCar == true) {
						writer.write("\t\t\tEnergieverbrauch Summe:\t\t\t" + powerConsumptionNordring + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Max:\t\t\t" + maxPowerConsumptionNordring + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Min:\t\t\t" + minPowerConsumptionNordring + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Durchschnitt:\t"
								+ Math.round(averagePowerConsumptionNordring) + " kwh\n");
					}
				}
				writer.write("\n" + "\t Anzahl aus dem Betriebshof Gradestraße: \t\t\t\t\t" + vehiclesGradestrasse
						+ "\t\t\tMenge:\t\t" + ((double) sizeGradestrasse) / 1000 + " t\n");
				if (vehiclesGradestrasse > 0) {
					writer.write("\t\t\tFahrstrecke Summe:\t\t\t\t" + distanceToursGradestrasse + " km\n");
					writer.write("\t\t\tFahrstrecke Max:\t\t\t\t" + maxTourGradestrasse + " km\n");
					writer.write("\t\t\tFahrstrecke Min:\t\t\t\t" + minTourGradestrasse + " km\n");
					writer.write("\t\t\tFahrstrecke Durchschnitt:\t\t" + Math.round(averageTourDistanceGradestrasse)
							+ " km\n");
					if (electricCar == true) {
						writer.write("\t\t\tEnergieverbrauch Summe:\t\t\t" + powerConsumptionGradestrasse + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Max:\t\t\t" + maxPowerConsumptionGradestrasse + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Min:\t\t\t" + minPowerConsumptionGradestrasse + " kwh\n");
						writer.write("\t\t\tEnergieverbrauch Durchschnitt:\t"
								+ Math.round(averagePowerConsumptionGradestrasse) + " kwh\n");
					}
				}
				writer.write("\n" + "Anzuliefernde Menge (IST):\tMHKW Ruhleben:\t\t\t\t\t"
						+ ((double) sizeRuhleben) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Pankow:\t\t\t\t\t\t" + ((double) sizePankow) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tMPS Reinickendorf:\t\t\t\t" + ((double) sizeReinickendorf) / 1000 + " t\n");
				writer.write("\t\t\t\t\t\t\tUmladestation Gradestrasse:\t\t"
						+ ((double) sizeUmladestationGradestrasse) / 1000 + " t\n");
				writer.write(
						"\t\t\t\t\t\t\tMA Gruenauer Str.:\t\t\t\t" + ((double) sizeGruenauerStr) / 1000 + " t\n\n");
			}
			if (vehiclesChessboard > 0) {
				writer.write("Gefahrene Kilometer je Fahrzeug:\t\t\t\t\t\t\t" + tourDistancesChessboard + " \n");
				if (electricCar == true)
					writer.write(
							"Energieverbrauch in kwh je Fahrzeug:\t\t\t\t\t\t" + powerConsumptionTourChessboard + "\n");
			}
			writer.write(
					"Gefahrene Strecke gesamt:\t\t\t\t\t\t\t\t\t" + (distanceToursForckenbeck + distanceToursMalmoeerStr
							+ distanceToursNordring + distanceToursGradestrasse + tourDistanceChessboard) + " km\n\n");
			if (electricCar == true)
				writer.write("Verbrauche Energie gesamt:\t\t\t\t\t\t\t\t\t"
						+ (powerConsumptionForckenbeck + powerConsumptionMalmoeerStr + powerConsumptionNordring
								+ powerConsumptionGradestrasse + powerConsumptionChessboard)
						+ " kwh\n\n");
			writer.write("Kosten (Jsprit): \t\t\t\t\t\t\t\t\t\t\t" + (Math.round(costsJsprit)) + " €\n\n");
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
	 * Creates an output of a summary of important informations of the created
	 * shipments
	 * 
	 */
	static void outputSummaryShipments(Scenario scenario, String day, HashMap<String, Carrier> carrierMap) {

		double capacityTruck = 0;
		String vehicleTypeId = null;

		Carriers allCarriers = (Carriers) scenario.getScenarioElement("carriers");
		Carrier testCarrier = allCarriers.getCarriers().values().iterator().next();
		for (VehicleType usedType : testCarrier.getCarrierCapabilities().getVehicleTypes()) {
			capacityTruck = usedType.getCapacity().getOther();
			vehicleTypeId = usedType.getId().toString();
		}
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
			writer.write("Fahrzeug: \t\t\t\t\t\t\t\t\t\t\t\t\t" + vehicleTypeId + "\n");
			writer.write(
					"Kapazität je Fahrzeug: \t\t\t\t\t\t\t\t\t\t" + ((double) capacityTruck / 1000) + " Tonnen\n\n");
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

	/**
	 * @param scenario
	 * @param carriers
	 * @param vehicleTypes
	 * @throws IOException
	 */

	@SuppressWarnings("null")
	static void createResultFile(Scenario scenario, Carriers carriers) throws Exception {

		log.info("Starting");

		// String inputDir;
		Map<Id<Person>, Double> personId2tourDistance = new HashMap<>();
		Map<Id<Person>, Double> personId2tourConsumptionkWh = new HashMap<>();
		Map<String, Integer> usedNumberPerVehicleType = new HashMap<>();
		ArrayList<String> toursWithOverconsumption = new ArrayList<>();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		Carriers allCarriers = (Carriers) scenario.getScenarioElement("carriers");
		Carrier testCarrier = allCarriers.getCarriers().values().iterator().next();
		for (VehicleType usedType : testCarrier.getCarrierCapabilities().getVehicleTypes()) {
			vehicleTypes.getVehicleTypes().put(usedType.getId(), usedType);
			usedNumberPerVehicleType.put(usedType.getId().toString(), 0);
		}

		Network network = scenario.getNetwork();

		BufferedWriter writer;
		File file;
		file = new File(scenario.getConfig().controler().getOutputDirectory() + "/02_SummaryOutput.txt");

		writer = new BufferedWriter(new FileWriter(file, true));
		String now = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
		writer.write("Tourenstatisitik erstellt am: " + now + "\n\n");

		for (Carrier singleCarrier : carriers.getCarriers().values()) {

			double totalDistance = 0;
			int numberOfVehicles = 0;
			double distanceTour;
			int numCollections = 0;
			int tourNumberCarrier = 1;
			VehicleType vt = null;

			for (ScheduledTour scheduledTour : singleCarrier.getSelectedPlan().getScheduledTours()) {
				distanceTour = 0.0;
				for (VehicleType vt2 : singleCarrier.getCarrierCapabilities().getVehicleTypes()) {
					if (vt2.getId().toString()
							.contains(scheduledTour.getVehicle().getType().getId().toString())) {

						vt = vt2;
						break;
					}
				}

				int vehicleTypeCount = usedNumberPerVehicleType
						.get(scheduledTour.getVehicle().getType().getId().toString());
				usedNumberPerVehicleType.replace(scheduledTour.getVehicle().getType().getId().toString(),
						vehicleTypeCount + 1);

				List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
				for (Tour.TourElement element : elements) {
					if (element instanceof Tour.Pickup) {
						numCollections++;

					}
					if (element instanceof Tour.Leg) {
						Tour.Leg legElement = (Tour.Leg) element;
						if (legElement.getRoute().getDistance() != 0)
							distanceTour = distanceTour
									+ RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0, network);
					}
				}
				Id<Person> personId = Id.create(
						scheduledTour.getVehicle().getType().getId() + "-Tour " + tourNumberCarrier,
						Person.class);
				personId2tourDistance.put(personId, distanceTour);
				if (vt.getEngineInformation().getAttributes().getAttribute("fuelType").equals("electricity")) {
					personId2tourConsumptionkWh.put(personId, (distanceTour / 1000) * (double) vt.getEngineInformation()
							.getAttributes().getAttribute("engeryConsumptionPerKm"));
				}
				totalDistance = totalDistance + distanceTour;
				tourNumberCarrier++;
			}
			numberOfVehicles = numberOfVehicles + (tourNumberCarrier - 1);
			writer.write("\n\n" + "Version: " + singleCarrier.getId().toString() + "\n");
			writer.write("\tAnzahl der Abholstellen (Soll): \t\t\t\t\t" + singleCarrier.getShipments().size() + "\n");
			writer.write("\tAnzahl der Abholstellen ohne Abholung: \t\t\t\t"
					+ (singleCarrier.getShipments().size() - numCollections) + "\n");
			writer.write("\tAnzahl der Fahrzeuge:\t\t\t\t\t\t\t\t" + numberOfVehicles + "\n");
			for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
				if (singleCarrier.getId().toString().equals(singleVehicleType.getDescription())) {
					writer.write("\t\t\tAnzahl Typ " + singleVehicleType.getId().toString() + ":\t\t\t\t"
							+ usedNumberPerVehicleType.get(singleVehicleType.getId().toString()) + "\n");
				}
			}
			writer.write(
					"\n" + "\tGefahrene Kilometer insgesamt:\t\t\t\t\t\t" + Math.round(totalDistance / 1000) + " km\n");
			writer.write("\tVerfügbare Fahrzeugtypen:\t\t\t\t\t\n\n");
			for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {

				writer.write(
						"\t\t\tID: " + singleVehicleType.getId() + "\t\tAntrieb: "
								+ singleVehicleType.getEngineInformation().getAttributes().getAttribute("fuelType")
										.toString()
								+ "\t\tKapazität: " + singleVehicleType.getCapacity().getOther() + "\t\tFixkosten:"
								+ singleVehicleType.getCostInformation().getFixedCosts() + " €");
				if (singleVehicleType.getEngineInformation().getAttributes().getAttribute("fuelType")
						.equals("electricity")) {
					double electricityConsumptionPer100km = 0;
					double electricityCapacityinkWh = 0;
					electricityConsumptionPer100km = (double) singleVehicleType.getEngineInformation().getAttributes()
							.getAttribute("engeryConsumptionPerKm");
					electricityCapacityinkWh = (double) singleVehicleType.getEngineInformation().getAttributes()
							.getAttribute("engeryCapacity");

					writer.write("\t\tLadekapazität: " + electricityCapacityinkWh + " kWh\t\tVerbrauch: "
							+ electricityConsumptionPer100km + " kWh/100km\t\tReichweite: "
							+ (int) Math.round(electricityCapacityinkWh / electricityConsumptionPer100km) + " km\n");
				} else
					writer.write("\n");

			}
			writer.write("\n\n" + "\tTourID\t\t\t\t\t\tdistance (max Distance) (km)\tconsumption (capacity) (kWh)\n\n");

			for (Id<Person> id : personId2tourDistance.keySet()) {

				int tourDistance = (int) Math.round(personId2tourDistance.get(id) / 1000);
				int consumption = 0;
				double distanceRange = 0;
				double electricityCapacityinkWh = 0;
				double electricityConsumptionPerkm = 0;

				for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {

					if (id.toString().contains(singleVehicleType.getId().toString()) && singleVehicleType
							.getEngineInformation().getAttributes().getAttribute("fuelType").equals("electricity")) {

						electricityConsumptionPerkm = (double) singleVehicleType.getEngineInformation().getAttributes()
								.getAttribute("engeryConsumptionPerKm");
						electricityCapacityinkWh = (double) singleVehicleType.getEngineInformation().getAttributes()
								.getAttribute("engeryCapacity");
						distanceRange = (int) Math.round(electricityCapacityinkWh / electricityConsumptionPerkm);
						consumption = (int) Math.round(personId2tourConsumptionkWh.get(id));

						if (consumption > electricityCapacityinkWh)
							toursWithOverconsumption.add(id.toString());
					}
				}

				writer.write("\t" + id + "\t\t" + tourDistance);
				if (distanceRange > 0) {
					writer.write(" (" + distanceRange + ")\t\t\t\t\t\t" + consumption + " (" + electricityCapacityinkWh
							+ ")");
				} else
					writer.write("\t\t\t\t\t\t\t\t\t\t");
				writer.newLine();

			}
			personId2tourConsumptionkWh.clear();
			personId2tourDistance.clear();
		}
		writer.flush();
		writer.close();
		log.info("Output geschrieben");
		log.info("### Done.");
		if (toursWithOverconsumption.isEmpty() == false)
			throw new Exception("The tour(s) " + toursWithOverconsumption.toString()
					+ " have a higher consumption then their capacity");

	}

}
