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
		Node startNode;
		Node targetNode;

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
			startNode = null;
			targetNode = null;
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);

			// Sonnenallee to Treptower Park
			length = null;  // TODO
			startNode = null;
			targetNode = null;
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
		}

		// ---- Abschnitt 17 (4.1 km) ----
		{
			// Treptower Park to Ostkreuz
			length = null;  // TODO
			startNode = null;
			targetNode = null;
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);

			// Ostkreuz to Storkower Strasse
			length = null;  // TODO
			startNode = null;
			targetNode = null;
			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
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
