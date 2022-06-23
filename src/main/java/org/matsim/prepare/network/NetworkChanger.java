package org.matsim.prepare.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.HashSet;
import java.util.Set;

public class NetworkChanger {


	public static void addA100Extension(Network network) {
		double length;
		Node startNode1;
		Node startNode2;
		Node targetNode1;
		Node targetNode2;

		// set specification for all links
		Set<String> allowedModes = new HashSet<>();
		allowedModes.add(TransportMode.car);
		allowedModes.add(TransportMode.ride);
		allowedModes.add(TransportMode.motorcycle);
		allowedModes.add(TransportMode.truck);


		double freespeed = 80 / 3.6;
		double lanes = 3;
		double capacity = lanes * 2 * 1000.0;

		// set link specific properties and add links to network
		// ---- Abschnitt 16 (3.2 km) ----
		{
			// Grenzallee to Sonnenallee
			length = null;  // TODO
			startNode1 = null;  // 27542432 nach Norden
			startNode2 = null;  // 27542414 nach Sueden/Westen
			targetNode1 = null;  // 31390656 nach Norden
			targetNode2 = null;  // 1918530791
			addLinkToNetwork(network, startNode1, targetNode1, allowedModes, capacity, freespeed, length, lanes);
			addLinkToNetwork(network, targetNode2, startNode2, allowedModes, capacity, freespeed, length, lanes);

			// Sonnenallee to Treptower Park
			length = null;  // TODO
			startNode1 = null;  // 31357356 nach Norden
			startNode2 = null;  // 31357357 nach Sueden
			targetNode1 = null;  // 29787110
			targetNode2 = null;  // 29787103 sueden
			addLinkToNetwork(network, startNode1, targetNode1, allowedModes, capacity, freespeed, length, lanes);
			addLinkToNetwork(network, targetNode2, startNode2, allowedModes, capacity, freespeed, length, lanes);
		}

		// ---- Abschnitt 17 (4.1 km) ----
		{
			// Treptower Park to Ostkreuz
			length = null;  // TODO
			startNode1 = null;  // 29787110 nach Norden
			startNode2 = null;  // 29787103 nach Sueden
			targetNode1 = null;  // 4317221792    --> Ostkreuz hat nur einen Punkt auf der Markgrafenstr, einen auf Hauptstr
			targetNode2 = null;  // Hauptstr: 4370346530 nach Osten, 4245068305   nach Westen
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
			addLinkToNetwork(network, targetNode, startNode, allowedModes, capacity, freespeed, length, lanes);

			// Ostkreuz to Franfurter Allee
			length = null;  // TODO
			startNode1 = null; //
			startNode2 = null;  //
			targetNode1 = null;  //  Guertelstr: 1791505417
			targetNode2 = null;  //  Kreuzung Moellendorffstr/Frankfurter Allee (von unten links gegen den Uhrzeigersinn):
			// 12614683, 598234402, 288267826, 29784919
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
			addLinkToNetwork(network, targetNode, startNode, allowedModes, capacity, freespeed, length, lanes);


			// Franfurter Allee to Storkower Strasse (28373619, 28373623)
			length = null;  // TODO
			startNode1 = null; //
			startNode2 = null;  //
			targetNode1 = null;  //
			targetNode2 = null;  //
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
			addLinkToNetwork(network, targetNode, startNode, allowedModes, capacity, freespeed, length, lanes);
		}
	}

	private static void addLinkToNetwork(Network network,
	                                      Node startNode, Node targetNode,
	                                      Set<String> allowedModes,
	                                      double capacity,
	                                      double freespeed,
	                                      double length,
	                                      double lanes){

		Id linkId = Id.createLinkId(Id.getNumberOfIds(Link.class) + 1);  // TODO
		network.addLink(new L);

		// make sure that start and target node are already in the network
		if (!network.getNodes().containsValue(startNode)){
			network.addNode(startNode);
		}
		if (!network.getNodes().containsValue(targetNode)){
			network.addNode(targetNode);
		}

		Link link = network.getLinks().get(linkId);

		// add start and target node to link
		link.setFromNode(startNode);
		link.setToNode(targetNode);

		link.setAllowedModes(allowedModes);
		link.setCapacity(capacity);
		link.setFreespeed(freespeed);
		link.setLength(length);
		link.setNumberOfLanes(lanes);

		return linkId;
	}
}
