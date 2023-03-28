package org.matsim.synthetic.smallScaleCommercialTraffic;

import org.matsim.smallScaleCommercialTrafficGeneration.CreateSmallScaleCommercialTrafficDemand;

public class RunGenerationFreightTraffic {

	public static void main(String[] args) {
		String inputDataDirectory = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.0/input/commercialTraffic/";
		String sampleSize = "0.001";
		String jspritIterations = "1";
//		String creationOption = "useExistingCarrierFileWithoutSolution";
		String creationOption = "createNewCarrierFile";
		String landuseConfiguration = "useOSMBuildingsAndLanduse";
		String trafficType = "businessTraffic";
//		String includeExistingModels = "false";
		String zoneShapeFileName = "../shp/berlinBrandenburg_Zones_VKZ_4326.shp";
		String buildingsShapeFileName = "../shp/buildings_BerlinBrandenburg_4326.shp";
		String landuseShapeFileName = "../shp/berlinBrandenburg_landuse_4326.shp";
		String shapeCRS = "EPSG:4326";
		String resistanceFactor = "0.005";


		new CreateSmallScaleCommercialTrafficDemand().execute(
				inputDataDirectory,
				"--sample", sampleSize,
				"--jspritIterations", jspritIterations,
				"--creationOption", creationOption,
				"--landuseConfiguration", landuseConfiguration,
				"--trafficType", trafficType,
//				"--includeExistingModels", includeExistingModels,
				"--zoneShapeFileName", zoneShapeFileName,
				"--buildingsShapeFileName", buildingsShapeFileName,
				"--landuseShapeFileName", landuseShapeFileName,
				"--shapeCRS", shapeCRS,
				"--resistanceFactor", resistanceFactor
		);
	}

}
