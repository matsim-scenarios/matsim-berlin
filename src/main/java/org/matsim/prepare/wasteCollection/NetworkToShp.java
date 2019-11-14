package org.matsim.prepare.wasteCollection;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class NetworkToShp {

	public static void main(String[] args) {
		String networkFile = "original-input-data/berlin-v5.2-1pct.output_network.xml.gz";
		String shapeFileLine = "C:\\Users\\erica\\OneDrive\\Dokumente\\Studium\\0 Masterarbeit\\shapeNetzwerk\\berlin-v5.2-1pct.output_networkFl.shp";
		String shapeFilePoly = "C:\\Users\\erica\\OneDrive\\Dokumente\\Studium\\0 Masterarbeit\\shapeNetzwerk\\berlin-v5.2-1pct.output_networkP.shp";
		String crs = TransformationFactory.DHDN_GK4;
		Links2ESRIShape.main(new String[]{networkFile, shapeFileLine, shapeFilePoly, crs});

	}
}
