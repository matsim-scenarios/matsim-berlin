package org.matsim.analysis.mainmode;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;


import java.util.*;

public class MainModeHandler implements TransitDriverStartsEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler {
    private static final String workDir = "C:\\Users\\anton\\IdeaProjects\\Hausaufgaben\\HA2\\Analysis";
    private static final String shapeFile = workDir + "\\shapefiles\\bezirksgrenzen.shp";
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");
    private static final String Land_schluessel = "11";
    private static final String Gemeinde_s = "006";
    private static final List<String> modes = List.of(TransportMode.walk, TransportMode.bike, TransportMode.ride, TransportMode.car, TransportMode.pt, TransportMode.airplane);
    private final Set<Id<Person>> transitDrivers = new HashSet<>();
    private final Map<Id<Person>, List<String>> personTrips = new HashMap<>();
    private final Map<Id<Person>, List<Coord>> personCoords = new HashMap<>();
    private static int debugger = 0;

    public Map<Id<Person>, List<String>> getPersonTrips() {
        return personTrips;
    }

    public Map<Id<Person>, List<Coord>> getPersonCoords() {
        return personCoords;
    }

    @Override
    public void handleEvent(ActivityEndEvent e) {
        if (transitDrivers.contains(e.getPersonId()) || isInteraction(e.getActType())) return;

        var endCoord = e.getCoord();
        personCoords.computeIfAbsent(e.getPersonId(), coords -> new ArrayList<>()).add(endCoord);
        if (insideAreaOfInterest(transformation.transform(endCoord)) == false) return;

        personTrips.computeIfAbsent(e.getPersonId(), id -> new ArrayList<>()).add("");
    }

    @Override
    public void handleEvent(PersonDepartureEvent e) {
        if (transitDrivers.contains(e.getPersonId())) return;

        var lastOne = personCoords.get(e.getPersonId());
        var lastCoord = getLastCoord(lastOne);
        if (insideAreaOfInterest(transformation.transform(lastCoord)) == false) return;

        var trips = personTrips.get(e.getPersonId());
        var mainMode = getMainMode(getLast(trips), e.getLegMode());
        setLast(trips, mainMode);
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent transitDriverStartsEvent) {
        transitDrivers.add(transitDriverStartsEvent.getDriverId());
    }

    private boolean isInteraction(String type) {
        return type.endsWith(" interaction");
    }

    private String getMainMode(String current, String newMode) {

        var currentIndex = modes.indexOf(current);
        var newIndex = modes.indexOf(newMode);

        return currentIndex > newIndex ? current : newMode;
    }

    private String getLast(List<String> from) {
        return from.get(from.size() - 1);
    }

    private Coord getLastCoord(List<Coord> from) {
        return from.get(from.size() - 1);
    }

    private void setLast(List<String> to, String value) {
        to.set(to.size() - 1, value);
    }

    private boolean insideAreaOfInterest(Coord coord) {
        var features = ShapeFileReader.getAllFeatures(shapeFile);
        var geometry = features.stream().filter(feature -> feature.getAttribute("Land_schlu").equals(Land_schluessel))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();

        if (geometry.covers(MGC.coord2Point(coord))) {
            return true;
        } else{
                return false;
            }
        }
    }


