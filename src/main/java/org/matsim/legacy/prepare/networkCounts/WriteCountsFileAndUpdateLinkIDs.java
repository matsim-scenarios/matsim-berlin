/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.legacy.prepare.networkCounts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

/**
* @author ikaddoura
*/

public class WriteCountsFileAndUpdateLinkIDs {

	private final Logger log = LogManager.getLogger(WriteCountsFileAndUpdateLinkIDs.class);

	private Network network;
	private Counts<Link> inputCounts;
	private Map<String, Id<Link>> countId2LinkId;

	public static void main(String[] args) {

		final String countsfile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/network_counts/vmz_di-do_shortIds.xml";
		final String osmNodeFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/berlin_4.0/counts/counts_OSM-nodes.csv";
		final String networkFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/berlin_4.0/network/berlin_brandenburg_berlin4.0_v1_2018-02-21_keepPaths-false_keepCountOSMnodeIDs-true_cleaned_network.xml.gz";

		final String countsOutputFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/berlin_4.0/counts/vmz_di-do_berlin4.0_detailed-network-v1.xml.gz";

		WriteCountsFileAndUpdateLinkIDs writer = new WriteCountsFileAndUpdateLinkIDs(networkFile, countsfile, osmNodeFile);
		writer.write(countsOutputFile);

	}

	private void write(String countsOutputFile) {

		Counts<Link> outputCounts = new Counts<>();

		outputCounts.setDescription("based on 'vmz_di-do.xml' with link IDs for the detailed berlin 4.0 network v1");
		outputCounts.setName("vmz_di-do_detailed-network_berlin-4.0_v1");
		outputCounts.setYear(inputCounts.getYear());

		Set<String> processedCountLinkIDs = new HashSet<>();
		for (Count<Link> count : inputCounts.getCounts().values()) {

			if (processedCountLinkIDs.contains(count.getCsLabel())) {
				log.warn(count.getCsLabel() + " already processed. Skipping...");
			} else {
				Id<Link> newLinkId = null;
				if (this.countId2LinkId.get(count.getCsLabel()) == null) {
					log.warn("Counting station ID " + count.getCsLabel() + " not found in OSM node ID file: " + countId2LinkId.toString());
				} else {
					newLinkId = this.countId2LinkId.get(count.getCsLabel());

					log.info("Adding count Id " + count.getCsLabel() + " on link " + newLinkId);
					outputCounts.createAndAddCount(newLinkId, count.getCsLabel());
					outputCounts.getCounts().get(newLinkId).setCoord(count.getCoord());
					outputCounts.getCounts().get(newLinkId).getVolumes().putAll(count.getVolumes());
				}

				processedCountLinkIDs.add(count.getCsLabel());
			}
		}

		CountsWriter countsWriter = new CountsWriter(outputCounts);
		countsWriter.write(countsOutputFile);

		log.info("Counts file written to " + countsOutputFile);
	}

	public WriteCountsFileAndUpdateLinkIDs(String networkFile, String countsfile, String osmNodeFile) {

		network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(network);
		reader.readFile(networkFile);

		countId2LinkId = getCountId2LinkId(osmNodeFile);

		inputCounts = new Counts<>();
		MatsimCountsReader countsReader = new MatsimCountsReader(inputCounts);
		countsReader.readFile(countsfile);

		log.info("--> Counts in Count OSM node ID file: " + countId2LinkId.size());
		log.info("--> Counts in counts file: " + inputCounts.getCounts().size());

		if (countId2LinkId.size() != inputCounts.getCounts().size()) {
			log.warn("Counts file and Count OSM node ID file doesn't seem to match.");
		}
	}

	private Map<String, Id<Link>> getCountId2LinkId(String csvFile){
		final Map<String, Id<Link>> countId2LinkId = new HashMap<>();

		TabularFileParserConfig tabfileParserConfig = new TabularFileParserConfig();
	    tabfileParserConfig.setDelimiterTags(new String[] {";"});

	    	log.info("Reading count Id and osm node IDs from" + csvFile);

	    	tabfileParserConfig.setFileName(csvFile);
	    	new TabularFileParser().parse(tabfileParserConfig, new TabularFileHandler() {
	    		boolean header = true;
	    		int lineCounter = 0;
	    		@Override
	    		public void startRow(String[] row) {
	    			if(!header){

	    				if( !(row[0].equals("") || row[1].equals("") || row[2].equals("") ) ) {
	    					String countId = row[0];
	    					Id<Node> fromNodeId = Id.createNodeId(row[1]);
						Id<Node> toNodeId = Id.createNodeId(row[2]);

						Id<Link> linkId = getLinkId(fromNodeId, toNodeId);
						countId2LinkId.put(countId, linkId);

						log.info(countId + " --> " + linkId);

	    				}
	    			}
    				lineCounter++;

    				if (lineCounter%10 == 0) {
    					log.info("Line " + lineCounter);
    				}
	    			header = false;
	    		}
	    	});
	    return countId2LinkId;
	}

	private Id<Link> getLinkId(Id<Node> fromNodeId, Id<Node> toNodeId) {
		Id<Link> linkId = null;
		for (Link link : network.getLinks().values()) {
			if (link.getFromNode().getId().toString().equals(fromNodeId.toString())
					&& link.getToNode().getId().toString().equals(toNodeId.toString())) {
				if (linkId == null) {
					linkId = link.getId();
				} else {
					log.warn("There is more than one link with the from node Id " + fromNodeId + " and to node Id " + toNodeId + ": "
				+ linkId + " and " + link.getId());
				}
			}
		}
		if (linkId == null) {
			log.warn("No Link id found for from node Id " + fromNodeId + " and to node Id " + toNodeId + ".");
//			throw new RuntimeException("Aborting...");
		}
		return linkId;
	}

}

