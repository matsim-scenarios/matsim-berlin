package org.matsim.analysis.autofrei;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.policies.autofrei.AutofreiUtils;
import org.matsim.run.policies.autofrei.RunAutofreiBaseCaseCtdExtended;
import org.matsim.run.policies.autofrei.RunAutofreiPolicy;
import org.matsim.run.policies.autofrei.RunAutofreiPolicyDeparking;

import java.io.File;
import java.util.Set;

public class NetworkParking {
	public static void main(String[] args) {
		double pct = 0.1;

		Network network = NetworkUtils.readNetwork("/Users/paulh/runs-svn/matsim-berlin/autofrei/10pct-v6.4/berlin-autofrei-v6.4-policy-10pct-deparking-p2_d0_i0_max1000/berlin-v6.4.output_network.xml.zst");

		File shapefile = new File("input/v6.4/umweltzone/Umweltzone_Berlin.shp");
		ShpOptions shpOptions = new ShpOptions(shapefile.getAbsolutePath(), "EPSG:25833", null);
		ShpOptions.Index index = shpOptions.createIndex("_");

		MathTransform transform;
		try {
			CoordinateReferenceSystem sourceCRS = org.geotools.referencing.CRS.decode("EPSG:25832");
			CoordinateReferenceSystem targetCRS = org.geotools.referencing.CRS.decode("EPSG:25833");
			transform = org.geotools.referencing.CRS.findMathTransform(sourceCRS, targetCRS, true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}

		double allSpotsInHundekopf = 0.;
		double lengthInHundekopf = 0.;
		double carSpotsInHundekopf = 0.;
		double carLengthInHundekopf = 0.;
		for (Link link : network.getLinks().values()) {
			if (AutofreiUtils.notContainsNode(link.getFromNode(), index, transform) && AutofreiUtils.notContainsNode(link.getToNode(), index, transform)){
				// not in Hundekopf
				continue;
			}

			if (link.getAllowedModes().equals(Set.of("bike")) || link.getAllowedModes().equals(Set.of("pt"))) {
				// e.g., links with only mode bike or pt links should not be counted.
				continue;
			}

			Double parkingSpots = (Double) link.getAttributes().getAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR);
			allSpotsInHundekopf += parkingSpots;
			lengthInHundekopf += link.getLength();

			if(link.getAllowedModes().contains("car")) {
				carSpotsInHundekopf += parkingSpots;
				carLengthInHundekopf += link.getLength();
			}
		}

		// report
		System.out.println("Total parking spots in Hundekopf: " + allSpotsInHundekopf / pct);
		System.out.println("Remaining car parking spots in Hundekopf: " + carSpotsInHundekopf / pct);
		System.out.println("Share of car parking spots in Hundekopf: " + carSpotsInHundekopf / allSpotsInHundekopf);

		System.out.println("Total length in Hundekopf: " + lengthInHundekopf);
		System.out.println("Remaining car lane length in Hundekopf: " + carLengthInHundekopf);
		System.out.println("Share of car lane length in Hundekopf: " + carLengthInHundekopf / lengthInHundekopf);
	}
}

