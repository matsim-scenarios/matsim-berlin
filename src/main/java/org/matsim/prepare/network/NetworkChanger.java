package org.matsim.prepare.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

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
		allowedModes.add(TransportMode.taxi);


		double freespeed = 80 / 3.6;
		double lanes = 3;
		double capacity = lanes * 2 * 1000.0;


		// set link specific properties and add links to network
		// ---- Abschnitt 16 (3.2 km) ----
//		{
//			// Grenzallee to Sonnenallee
//			startNode1 = null;  // 206191207 nach Norden
//			startNode2 = null;  // 27542414 nach Sueden/Westen
//			targetNode1 = null;  // 31390656 nach Norden
//			targetNode2 = null;  // 1918530791

			Node sonnenalleeN1 = network.getFactory().createNode(Id.createNodeId("sonnenallee-nord1"), new Coord(4599632, 5816201));
			Node sonnenalleeN2 = network.getFactory().createNode(Id.createNodeId("sonnenallee-nord2"), new Coord(4599672, 5816235));
			// TODO sued
//			Node sonnenalleeS1 = network.getFactory().createNode(Id.createNodeId("sonnenallee-sued1"), new Coord(, ));
//			Node sonnenalleeS2 = network.getFactory().createNode(Id.createNodeId("sonnenallee-sued2"), new Coord(, ));

			network.addNode(sonnenalleeN1);
			network.addNode(sonnenalleeN2);
//			network.addNode(sonnenalleeS1);
//			network.addNode(sonnenalleeS2);

			Node startNode = network.getNodes().get(Id.get("206191207", Node.class));  // Motorway Grenzallee

			addLinkToNetwork(network, "bab100-grenzallee-sonnenallee",
						startNode, sonnenalleeN1, allowedModes, capacity, freespeed, lanes, 746.230);
			addLinkToNetwork(network, "bab100-sonnenallee-n",
					sonnenalleeN1, sonnenalleeN2, allowedModes, capacity, freespeed, lanes, 52.458);

		// TODO Autobahn Richtung sueden
//			Node targetNode = network.getNodes().get(Id.get("27542414", Node.class));  // Motorway Grenzallee
//			addLinkToNetwork(network, "sonnenallee-grenzallee",
//					sonnenalleeS2, targetNode, allowedModes, capacity, freespeed, lanes, );
//			addLinkToNetwork(network, "sonnenallee-s",
//					sonnenalleeS1, sonnenalleeS2, allowedModes, capacity, freespeed, lanes, );

//			// Sonnenallee to Treptower Park
//			startNode1 = null;  // 31357356 nach Norden
//			startNode2 = null;  // 31357357 nach Sueden
//			targetNode1 = null;  // 29787110
//			targetNode2 = null;  // 29787103 sueden

			Node trParkN1 = network.getFactory().createNode(Id.createNodeId("trpark-nord1"), new Coord(4599218,5818728 ));
			Node trParkN2 = network.getFactory().createNode(Id.createNodeId("trpark-nord2"), new Coord(4599232, 5818744 ));
			// TODO sued
	//			Node trParkS1 = network.getFactory().createNode(Id.createNodeId("trpark-sued1"), new Coord(, ));
	//			Node trParkS2 = network.getFactory().createNode(Id.createNodeId("trpark-sued2"), new Coord(, ));

			network.addNode(trParkN1);
			network.addNode(trParkN2);
	//			network.addNode(trParkS1);
	//			network.addNode(trParkS2);

			addLinkToNetwork(network, "bab100-sonnenallee-trpark",
					sonnenalleeN2, trParkN1, allowedModes, capacity, freespeed, lanes, 2525.724);
			addLinkToNetwork(network, "bab100-trpark-n",
					trParkN1, trParkN2, allowedModes, capacity, freespeed, lanes, 21.320);

			// TODO Autobahn Richtung sueden


//		}
//
//		// ---- Abschnitt 17 (4.1 km) ----
//		{
//			// Treptower Park to Ostkreuz
//			startNode1 = null;  // 29787110 nach Norden
//			startNode2 = null;  // 29787103 nach Sueden
//			targetNode1 = null;  // 4317221792    --> Ostkreuz hat nur einen Punkt auf der Markgrafenstr, einen auf Hauptstr
//			targetNode2 = null;  // Hauptstr: 4370346530 nach Osten, 4245068305   nach Westen
			Node ostkreuzN1 = network.getFactory().createNode(Id.createNodeId("ostkreuz-nord1"), new Coord(4599744, 5819705));
			Node ostkreuzN2 = network.getFactory().createNode(Id.createNodeId("ostkreuz-nord2"), new Coord(4599807, 5819805));
			// TODO sued
			//			Node ostkreuzS1 = network.getFactory().createNode(Id.createNodeId("ostkreuz-sued1"), new Coord(, ));
			//			Node ostkreuzS2 = network.getFactory().createNode(Id.createNodeId("ostkreuz-sued2"), new Coord(, ));

			network.addNode(ostkreuzN1);
			network.addNode(ostkreuzN2);
			//			network.addNode(ostkreuzS1);
			//			network.addNode(ostkreuzS2);

			addLinkToNetwork(network, "bab100-trpark-ostkreuz",
					trParkN2, ostkreuzN1, allowedModes, capacity, freespeed, lanes, 1088.652);
			addLinkToNetwork(network, "bab100-ostkreuz-n",
					ostkreuzN1, ostkreuzN2, allowedModes, capacity, freespeed, lanes, 118.073);

			// TODO Autobahn Richtung sueden



//			// Ostkreuz to Frankfurter Allee
//			startNode1 = null; //
//			startNode2 = null;  //
//			targetNode1 = null;  //  Guertelstr: 1791505417
//			targetNode2 = null;  //  Kreuzung Moellendorffstr/Frankfurter Allee (von unten links gegen den Uhrzeigersinn):
//			// 12614683, 598234402, 288267826, 29784919
			Node frankfurterN1 = network.getFactory().createNode(Id.createNodeId("frankfurter-nord1"), new Coord(4600297, 5820952));
			Node frankfurterN2 = network.getFactory().createNode(Id.createNodeId("frankfurter-nord2"), new Coord(4600304, 5821024));
			// TODO sued
			//			Node ostkreuzS1 = network.getFactory().createNode(Id.createNodeId("frankfurter-sued1"), new Coord(, ));
			//			Node ostkreuzS2 = network.getFactory().createNode(Id.createNodeId("frankfurter-sued2"), new Coord(, ));

			network.addNode(frankfurterN1);
			network.addNode(frankfurterN2);
			//			network.addNode(frankfurterS1);
			//			network.addNode(frankfurterS2);

			addLinkToNetwork(network, "bab100-ostkreuz-frankfurter",
					ostkreuzN2, frankfurterN1, allowedModes, capacity, freespeed, lanes, 1246.026);
			addLinkToNetwork(network, "bab100-frankfurter-n",
					frankfurterN1, frankfurterN2, allowedModes, capacity, freespeed, lanes, 72.416);

			// TODO Autobahn Richtung sueden


//
//
//			// Frankfurter Allee to Storkower Strasse (28373619, 28373623)
//			length = null;  // TODO
//			startNode1 = null; //
//			startNode2 = null;  //
//			targetNode1 = null;  //
//			targetNode2 = null;  //
//			addLinkToNetwork(network, startNode, targetNode, allowedModes, capacity, freespeed, length, lanes);
//			addLinkToNetwork(network, targetNode, startNode, allowedModes, capacity, freespeed, length, lanes);
//		}
		NetworkUtils.writeNetwork(network, "scenarios/berlin-bab100-network-out.xml");
	}

	private static void addLinkToNetwork(Network network,
	                                     String linkId,
	                                     Node startNode, Node targetNode,
	                                     Set<String> allowedModes,
	                                     double capacity,
	                                     double freespeed,
	                                     double lanes,
	                                     double length){

		Link newLink = network.getFactory().createLink(Id.createLinkId(linkId), startNode, targetNode);

		// make sure that start and target node are already in the network
		if (!network.getNodes().containsValue(startNode)){
			network.addNode(startNode);
		}
		if (!network.getNodes().containsValue(targetNode)){
			network.addNode(targetNode);
		}


		// add start and target node to link
		newLink.setFromNode(startNode);
		newLink.setToNode(targetNode);

		newLink.setAllowedModes(allowedModes);
		newLink.setCapacity(capacity);
		newLink.setFreespeed(freespeed);
		newLink.setLength(length);
		newLink.setNumberOfLanes(lanes);

		network.addLink(newLink);
	}

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork(); // TODO add path to network file
		addA100Extension(network);
	}
}
