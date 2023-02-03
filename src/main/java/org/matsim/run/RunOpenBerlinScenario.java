package org.matsim.run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.MergePopulations;
import org.matsim.synthetic.*;
import org.matsim.synthetic.actitopp.RunActitopp;
import org.matsim.synthetic.download.DownloadCommuterStatistic;
import picocli.CommandLine;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
		CreateLandUseShp.class, CreateBerlinPopulation.class, CreateBrandenburgPopulation.class, MergePopulations.class,
		LookupRegioStaR.class, ExtractFacilities.class, DownSamplePopulation.class, DownloadCommuterStatistic.class,
		AssignCommuters.class, RunActitopp.class
})
public class RunOpenBerlinScenario extends MATSimApplication {

	public static final String VERSION = "6.0";
	public static final String CRS = "EPSG:25832";

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinScenario.class, args);
	}

}
