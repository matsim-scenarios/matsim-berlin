package org.matsim.analysis.linkAnalysis;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinksInArea {
    final static String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";

    final static String hundekopfFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/berlin-planungsraum-hundekopf/berlin-hundekopf-based-on-planungsraum.shp";
    final static String berlinFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
    final static String superblockFile = "/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/Superblocks_gesamt.shp";
    final static String hundekopfLinks = "/Users/moritzkreuschner/Desktop/linksInsideHundekopf.txt";
    final static String berlinLinks = "/Users/moritzkreuschner/Desktop/linksInsideBerlin.txt";
    final static String superblockLinks = "/Users/moritzkreuschner/Desktop/linksInsideSuperblock_total.txt";

    public static void main(String[] args) {
        writeLinksInAreasFiles();
    }

    static Set<Id<Link>> loadLinksInHundekopf(){
        Set<Id<Link>> links = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(hundekopfLinks));
            String header = reader.readLine();
            String line = reader.readLine();
            while(line != null){
                links.add(Id.createLinkId(line));
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not load file " + hundekopfLinks + ".\n you should run writeLinksInAreasFiles() first");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return links;
    }

    static Set<Id<Link>> loadLinksInBerlin(){
        Set<Id<Link>> links = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(berlinLinks));
            String header = reader.readLine();
            String line = reader.readLine();
            while(line != null){
                links.add(Id.createLinkId(line));
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not load file " + berlinLinks + ".\n you should run writeLinksInAreasFiles() first");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);		}
        return links;
    }

    static Set<Id<Link>> loadLinksInSuperblock(){
        Set<Id<Link>> links = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(superblockLinks));
            String header = reader.readLine();
            String line = reader.readLine();
            while(line != null){
                links.add(Id.createLinkId(line));
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not load file " + superblockLinks + ".\n you should run writeLinksInAreasFiles() first");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);		}
        return links;
    }

    private static void writeLinksInAreasFiles() {
        Network network = NetworkUtils.readNetwork(networkFile);
        List<PreparedGeometry> hundeKopf = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(hundekopfFile));

        writeLinksInAreaToTxt(network, hundeKopf, hundekopfLinks);

        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(berlinFile));
        writeLinksInAreaToTxt(network, berlin, berlinLinks);

        List<PreparedGeometry> superblock = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(superblockFile));
        writeLinksInAreaToTxt(network, superblock, superblockLinks);
    }

    static void writeLinksInAreaToTxt(Network network, List<PreparedGeometry> geoms, String txtFile){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
            writer.write("linkIDs");
            network.getLinks().values().stream()
                    .forEach(link -> {
                        if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(), geoms)
                                || ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), geoms) ){
                            try {
                                writer.newLine();
                                writer.write(link.getId().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
