package org.matsim.legacy.analysis.emissions;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

public class EmissionsOnLinkHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = new HashMap<>();

    @Override
    public void reset(int iteration) {
    	link2pollutants.clear();
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
    	Map<Pollutant,Double> map = new HashMap<>() ;
        for( Map.Entry<Pollutant, Double> entry : event.getWarmEmissions().entrySet() ){
            map.put( entry.getKey(), entry.getValue() ) ;
        }
        handleEmissionEvent(event.getTime(), event.getLinkId(), map );
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions());
    }

    private void handleEmissionEvent(double time, Id<Link> linkId, Map<Pollutant, Double> emissions) {
    	if (link2pollutants.get(linkId) == null) {
    		link2pollutants.put(linkId, emissions);
    	} else {
    		for (Pollutant pollutant : emissions.keySet()) {
    			link2pollutants.get(linkId).merge(pollutant, emissions.get(pollutant), Double::sum);
    		}
    	}
    }

	public Map<Id<Link>, Map<Pollutant, Double>> getLink2pollutants() {
		return link2pollutants;
	}

}
