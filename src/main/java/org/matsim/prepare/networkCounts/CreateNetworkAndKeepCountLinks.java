/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.prepare.networkCounts;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * 
 * 
 * The osm input file may be generated as follows:
 * 
 * Download the latest osm files (*osm.pbf) from https://download.geofabrik.de
 * 
 * Install the programm osmosis and use the following commands to plug everything together:
 * 
 * detailed network:
 * osmosis --rb file=berlin-latest.osm.pbf --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street --used-node --wb berlin-latest-2018-02-20_incl-residential-and-living-street.osm.pbf
 * 
 * coarse network:
 * osmosis --rb file=brandenburg-latest.osm.pbf --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction --used-node --wb brandenburg-latest-2018-02-20_incl-tertiary.osm.pbf
 * 
 * merge:
 * osmosis --rb file=brandenburg-latest-2018-02-20_incl-tertiary.osm.pbf --rb berlin-latest-2018-02-20_incl-residential-and-living-street.osm.pbf --merge --wx berlin-brandenburg-network_2018-02-20.osm
 * 
 * @author tschlenther, ikaddoura
 *
 */
public class CreateNetworkAndKeepCountLinks {
	
	private final Logger log = Logger.getLogger(CreateNetworkAndKeepCountLinks.class);

	private final String INPUT_OSMFILE ;
	private final List<String> INPUT_COUNT_NODE_MAPPINGS;
	private final String outputDir;
	private final String prefix;
	private final String networkCS ;

	private Network network = null;
	private String outnetworkPrefix ;
	
	public static void main(String[] args) {
		String osmfile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/network/osm/berlin-brandenburg-network_2018-02-20.osm";
		
		String prefix = "berlin-car_be_5_withVspAdjustments" + new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String outDir = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_5/network/";

		boolean keepCountsOSMnodes = true;
		List<String> inputCountNodeMappingFiles = Arrays.asList("/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/counts/counts_OSM-nodes.csv");
		
		String crs = TransformationFactory.DHDN_GK4;
		CreateNetworkAndKeepCountLinks berlinNetworkCreator = new CreateNetworkAndKeepCountLinks(osmfile, inputCountNodeMappingFiles, crs , outDir, prefix);

		boolean keepPaths = false;
		boolean clean = true;
		boolean simplify = false;
		
		berlinNetworkCreator.createNetwork(keepCountsOSMnodes, keepPaths, simplify, clean);
		berlinNetworkCreator.writeNetwork();
	}
	
	public CreateNetworkAndKeepCountLinks(String inputOSMFile, List<String> inputCountNodeMappingFiles, String networkCoordinateSystem, String outputDir, String prefix) {
		this.INPUT_OSMFILE = inputOSMFile;
		this.INPUT_COUNT_NODE_MAPPINGS = inputCountNodeMappingFiles;
		this.networkCS = networkCoordinateSystem;
		this.outputDir = outputDir.endsWith("/")?outputDir:outputDir+"/";
		this.prefix = prefix;
		this.outnetworkPrefix = prefix;
		
		initLogger();
		log.info("--- set the coordinate system for network to be created to " + this.networkCS + " ---");
	}

	public void writeNetwork(){
		String outNetwork = this.outputDir+outnetworkPrefix+"_network.xml.gz";
		log.info("Writing network to " + outNetwork);
		new NetworkWriter(network).write(outNetwork);
		log.info("... done.");
	}
	
	public void createNetwork(boolean keepCountOSMnodes, boolean keepPaths, boolean doSimplify, boolean doCleaning){
		CoordinateTransformation ct =
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, networkCS);
		
		Set<Long> nodeIDsToKeep = readNodeIDs(INPUT_COUNT_NODE_MAPPINGS);

		if(this.network == null) {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			network = scenario.getNetwork();

			log.info("start parsing from osm file " + INPUT_OSMFILE);
	
			OsmNetworkReader networkReader = new OsmNetworkReader(network,ct, true, true);
						
			if (keepPaths) {
				networkReader.setKeepPaths(true);
			} else {
				networkReader.setKeepPaths(false);
			}
//			outnetworkPrefix += "_keepPaths-" + keepPaths;

			if (keepCountOSMnodes) {
				networkReader.setNodeIDsToKeep(nodeIDsToKeep);
			}
//			outnetworkPrefix += "_keepCountOSMnodeIDs-" + keepCountOSMnodes;

			networkReader.parse(INPUT_OSMFILE);
			log.info("finished parsing osm file");
		}	
		
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK BEFORE SIMPLIFYING AND CLEANING");
			}
		}
		
		if (doSimplify){
			outnetworkPrefix += "_simplified";
			log.info("number of nodes before simplifying:" + network.getNodes().size());
			log.info("number of links before simplifying:" + network.getLinks().size());
			log.info("start simplifying the network");
			/*
			 * simplify network: merge links that are shorter than the given threshold
			 */

			NetworkSimplifier simp = new NetworkSimplifier();
			simp.setNodesNotToMerge(nodeIDsToKeep);
			simp.setMergeLinkStats(false);
			simp.run(network);
			
			log.info("number of nodes after simplifying:" + network.getNodes().size());
			log.info("number of links after simplifying:" + network.getLinks().size());
			
			log.info("checking if all count nodes are in the network..");
			for(Long id : nodeIDsToKeep){
				if(!network.getNodes().containsKey(Id.createNodeId(id))){
					log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER SIMPLIFYING");
				}
			}
		}
		
		if (doCleaning){
//			outnetworkPrefix += "_cleaned";
				/*
				 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
				 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
				 */
			log.info("number of nodes before cleaning:" + network.getNodes().size());
			log.info("number of links before cleaning:" + network.getLinks().size());
			log.info("attempt to clean the network");
			new NetworkCleaner().run(network);
		}
		
		log.info("number of nodes after cleaning:" + network.getNodes().size());
		log.info("number of links after cleaning:" + network.getLinks().size());
		
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER NETWORK CREATION");
			}
		}
		
	}
	
	/**
	 * expects a path to a csv file that has the following structure: <br><br>
	 * 
	 * COUNT-ID;OSM_FROMNODE_ID;OSM_TONODE_ID <br><br>
	 * 
	 * It is assumed that the csv file contains a header line.
	 * Returns a set of all mentioned osm-node-ids.
	 * 
	 * @param listOfCSVFiles
	 * @return
	 */
	private Set<Long> readNodeIDs(List<String> listOfCSVFiles){
		final Set<Long> allNodeIDs = new HashSet<Long>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {";"});
	    
	    log.info("start reading osm node id's of counts");
	    for(String path : listOfCSVFiles){
	    	log.info("reading node id's from" + path);
	    	config.setFileName(path);	
	    	new TabularFileParser().parse(config, new TabularFileHandler() {
	    		boolean header = true;
	    		@Override
	    		public void startRow(String[] row) {
	    			if(!header){
	    				if( !(row[1].equals("") || row[2].equals("") ) ){
	    					allNodeIDs.add( Long.parseLong(row[1]));
	    					allNodeIDs.add( Long.parseLong(row[2]));
	    				}
	    			}
	    			header = false;				
	    		}
	    	});
	    }
	    return allNodeIDs;
	}
	
	private void initLogger(){
//		FileAppender fa = new FileAppender();
//		fa.setFile(outputDir + prefix +"_LOG_" + CreateNetworkAndKeepCountLinks.class.getSimpleName() + outnetworkPrefix + ".txt");
//		fa.setName("BerlinNetworkCreator");
//		fa.activateOptions();
//		fa.setLayout(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS} %-4r [%t] %-5p %c %x - %m%n"));
//	    log.addAppender(fa);
		throw new RuntimeException( "seems to have broken with update of log4j.  kai, mar'20" );
	}
}
