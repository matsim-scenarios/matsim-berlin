package org.matsim.analysis.linkAnalysis;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.*;
import java.util.List;


public class linksInSuperblocks {
    final static String networkFile = "/Users/moritzkreuschner/Desktop/networks/berlin-v5.5-network.xml.gz";
    final static String superblockFile1="/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/S0001.shp";
    final static String superblockFile2="/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/anderesKBS/S0001_wgs84.shp";
    final static String superblockFile3="/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/anderesKBS/S0001_31468.shp";
    final static String superblockFile4="/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/S00092.shp";
    final static String superblockLinks1="/Users/moritzkreuschner/Desktop/R/datasets/airpollution/linksInsideSuperblock1.txt";
    final static String superblockLinks2="/Users/moritzkreuschner/Desktop/R/datasets/airpollution/linksInsideSuperblock1a.txt";
    final static String superblockLinks3="/Users/moritzkreuschner/Desktop/R/datasets/airpollution/linksInsideSuperblock1b.txt";
    final static String superblockLinks4="/Users/moritzkreuschner/Desktop/R/datasets/airpollution/linksInsideSuperblock92.txt";

    public static void main(String[] args) {
        writeLinksInAreasFiles();
    }


    private static void writeLinksInAreasFiles() {
        Network network = NetworkUtils.readNetwork(networkFile);
        List<PreparedGeometry> Cat1 = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(superblockFile1));
        writeLinksInAreaToTxt(network, Cat1, superblockLinks1);
        List<PreparedGeometry> Cat2 = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(superblockFile2));
        writeLinksInAreaToTxt(network, Cat2, superblockLinks2);
        List<PreparedGeometry> Cat3 = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(superblockFile3));
        writeLinksInAreaToTxt(network, Cat3, superblockLinks3);
        List<PreparedGeometry> Cat4 = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(superblockFile4));
        writeLinksInAreaToTxt(network, Cat4, superblockLinks4);

    }

    static void writeLinksInAreaToTxt(Network network, List<PreparedGeometry> geoms, String txtFile){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
            writer.write("linkId");
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
