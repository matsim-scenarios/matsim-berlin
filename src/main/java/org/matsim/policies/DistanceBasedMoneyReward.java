package org.matsim.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;
import java.util.HashMap;
import java.util.Map;

public class DistanceBasedMoneyReward implements PersonDepartureEventHandler,
        PersonArrivalEventHandler,
        AfterMobsimListener,
        ActivityStartEventHandler {

    private final Map<Id<Person>, Double> distanceTravelledWalk = new HashMap<>();
    private final Map<Id<Person>, Double> distanceTravelledBike = new HashMap<>();
    private Map<Id<Vehicle>, Id<Person>> vehicles2Persons = new HashMap<>();
    private static final Logger log = LogManager.getLogger(DistanceBasedMoneyReward.class);
    private final double beelineDistanceFactor;
    private final Network network;
    private final double klimaTaler;
    private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
    private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();
    private final Map<Id<Person>, Double> distanceTravelledPt = new HashMap<>();

    private Map<Id<Person>, Coord> agentDepartureLocations = new HashMap<>();

    public DistanceBasedMoneyReward(double modeSpecificBeelineDistanceFactor, Network network, double klimaTaler) {
        this.beelineDistanceFactor = modeSpecificBeelineDistanceFactor;
        this.network = network;
        this.klimaTaler = klimaTaler;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.walk)) {
            Id<Link> linkId = event.getLinkId();
            Coord endcoord = network.getLinks().get(linkId).getCoord();
            Coord startCoord = this.agentDepartureLocations.get(event.getPersonId());
            if (startCoord != null) {
                double beelineDistance = CoordUtils.calcEuclideanDistance(startCoord, endcoord);
                double distance = beelineDistance * beelineDistanceFactor;
                if (!distanceTravelledWalk.containsKey(event.getPersonId())) {
                    distanceTravelledWalk.put(event.getPersonId(), distance);
                } else {
                    distance = distanceTravelledWalk.get(event.getPersonId()) + distance;
                    distanceTravelledWalk.replace(event.getPersonId(), distance);
                }
            }
        }

        if (event.getLegMode().equals("bicycle")) {
            Id<Link> linkId = event.getLinkId();
            Coord endcoord = network.getLinks().get(linkId).getCoord();
            Coord startCoord = this.agentDepartureLocations.get(event.getPersonId());
            if (startCoord != null) {
                double beelineDistance = CoordUtils.calcEuclideanDistance(startCoord, endcoord);
                double distance = beelineDistance * beelineDistanceFactor;
                if (!distanceTravelledBike.containsKey(event.getPersonId())) {
                    distanceTravelledBike.put(event.getPersonId(), distance);
                } else {
                    distance = distanceTravelledBike.get(event.getPersonId()) + distance;
                    distanceTravelledBike.replace(event.getPersonId(), distance);
                }
            }
        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        //in the pt test case the routing mode can be null
        if (event.getRoutingMode() !=null) {
            if (event.getRoutingMode().equals(TransportMode.walk)) {
                Id<Link> linkId = event.getLinkId();
                Coord coord = network.getLinks().get(linkId).getCoord();
                this.agentDepartureLocations.put(event.getPersonId(), coord);
            }

            if (event.getRoutingMode().equals(TransportMode.bike) || event.getRoutingMode().equals("bicycle")) {
                Id<Link> linkId = event.getLinkId();
                Coord coord = network.getLinks().get(linkId).getCoord();
                this.agentDepartureLocations.put(event.getPersonId(), coord);
            }
        }
    }


    @Override
    public void reset(int iteration) {
        this.agentDepartureLocations.clear();
        this.vehicles2Persons.clear();
        this.distanceTravelledWalk.clear();
        this.distanceTravelledPt.clear();
        this.distanceTravelledBike.clear();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledWalk.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            double emissionsSaved = idDoubleEntry.getValue() * 0.176;
            double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForWalk", null));
        }

        for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledBike.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            double emissionsSaved = idDoubleEntry.getValue() * 0.176;
            double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForBike", null));
        }

        for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledPt.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            double emissionsSaved = idDoubleEntry.getValue()  * 0.076;
            double klimaTaler = emissionsSaved / 5000 * this.klimaTaler;
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForPt", null));
        }
    }


    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
            personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of first pt interaction an agent has in the whole leg
            personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real activity (i.e. finish the leg)
        }

        if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
            Id<Person> personId = event.getPersonId();
            if (personDepartureCoordMap.containsKey(personId)) {
                double distance = CoordUtils.calcEuclideanDistance(personDepartureCoordMap.get(personId), personArrivalCoordMap.get(personId));
                if (distanceTravelledPt.containsKey(event.getPersonId())) {
                    distance = distanceTravelledPt.get(personId) + distance;
                    distanceTravelledPt.replace(personId, distance);
                }
                if (!distanceTravelledPt.containsKey(event.getPersonId())) {
                    distanceTravelledPt.put(personId, distance);

                }
                personDepartureCoordMap.remove(personId);
                personArrivalCoordMap.remove(personId);
            }
        }
    }


}
