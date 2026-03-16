package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.policies.MobilityToGridScenariosUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.dashboard.EmissionsDashboard;
import org.matsim.vehicles.MatsimVehicleWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.matsim.run.policies.MobilityToGridScenariosUtils.addEngineInformationToVehicleTypes;

@CommandLine.Command(name = "air-pollution-hbefa-types", description = "Run AirPollutionAnalysis with different combinations of HBEFA vehicle types.")
public class RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes.class);

	private static final String BEFORE = "_before_emissions.xml";
	private static final String AFTER = "_after_emissions.xml";
	private static final String XML = ".xml";

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which air pollution analysis should be run.")
	private List<Path> inputPaths;
	@CommandLine.Option(names = "--private-car-emission-concept", description = "Set HBEFA 4.1 emission concept aka fuel type for car vehicle type.")
	MobilityToGridScenariosUtils.Hbefa41EmissionConcept carEmissionConcept = MobilityToGridScenariosUtils.Hbefa41EmissionConcept.PETROL_4S;
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes().execute(args);
	}

	@Override
	public Integer call() throws Exception {
//		we only need to run 100% BEV and then have to interpolate between that and the required
//		we need:
//		1) 90% BEV, "normal", 5% H2
//		2) 60% BEV, 1% "normal", 20% synthetische Kraftstoffe, 19% H2

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			String configPath = ApplicationUtils.matchInput("config.xml", runDirectory).toString();
			Config config = ConfigUtils.loadConfig(configPath);
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
			if (shp.isDefined()){
//				use different shape file than in simwrapper config if provided.
				simwrapperCfg.defaultParams().shp = shp.getShapeFile();
			}
			//skip default dashboards (we only want to run AirPollutionAnalysis and create the corresponding dashboard).
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			sw.addDashboard(Dashboard.customize(new EmissionsDashboard(config.global().getCoordinateSystem())).context("emissions"));

//			configure emissions config group
			OpenBerlinScenario.configureEmissionsConfigGroup(config);

			String networkPath = ApplicationUtils.matchInput("output_network.xml.gz", runDirectory).toString();
			String vehiclesPath = ApplicationUtils.matchInput("output_vehicles.xml.gz", runDirectory).toString();
			String transitVehiclesPath = ApplicationUtils.matchInput("output_transitVehicles.xml.gz", runDirectory).toString();
			String populationPath = ApplicationUtils.matchInput("output_plans.xml.gz", runDirectory).toString();

			config.network().setInputFile(networkPath);
			config.vehicles().setVehiclesFile(vehiclesPath);
			config.transit().setVehiclesFile(transitVehiclesPath);
			config.plans().setInputFile(populationPath);

			Scenario scenario = ScenarioUtils.loadScenario(config);

			// add hbefa link attributes.
//			the link attributes should already be there as OpenBerlinScenario adds them, but in theory this class could also be run on some kind of initial network.
			HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
			roadTypeMapping.addHbefaMappings(scenario.getNetwork());

			String carFuelType = getCarFuelType();

			addEngineInformationToVehicleTypes(scenario, carFuelType);

//			write outputs with adapted files.
//			original output files need to be overwritten as AirPollutionAnalysis searches for "config.xml".
//			We will copy the original output files back to their old file names later. very clunky, but I see no alternative, if we want to keep our run output consistent.
//			copy old files to separate files
			Path beforeEmissionsConfigPath = getUniqueTargetPath(Path.of(configPath.split(XML)[0] + BEFORE));
			Path beforeEmissionsNetworkPath = getUniqueTargetPath(Path.of(networkPath.split(XML)[0] + BEFORE + ".gz"));
			Path beforeEmissionsVehiclesPath = getUniqueTargetPath(Path.of(vehiclesPath.split(XML)[0] + BEFORE + ".gz"));
			Path beforeEmissionsTransitVehiclesPath = getUniqueTargetPath(Path.of(transitVehiclesPath.split(XML)[0] + BEFORE + ".gz"));
			Files.copy(Path.of(configPath), beforeEmissionsConfigPath);
			Files.copy(Path.of(networkPath), beforeEmissionsNetworkPath);
			Files.copy(Path.of(vehiclesPath), beforeEmissionsVehiclesPath);
			Files.copy(Path.of(transitVehiclesPath), beforeEmissionsTransitVehiclesPath);

//			now we can write the prepared output to the usual output file paths.
			ConfigUtils.writeConfig(config, configPath);
			NetworkUtils.writeNetwork(scenario.getNetwork(), networkPath);
			new MatsimVehicleWriter(scenario.getVehicles()).writeFile(vehiclesPath);
			new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesPath);

			try {
				sw.generate(runDirectory, true);
				sw.run(runDirectory);
			} catch (IOException e) {
				InterruptedIOException ex = new InterruptedIOException("Simwrapper did not finish correctly.");
				ex.initCause(e);
				throw ex;
			}

//			after finishing the analysis we can
//			1) copy the transformed files to paths with _after_emissions suffix
//			2) copy the original files from paths with suffix _before_emissions to original paths
//			3) delete the files with _before_emissions suffix.
			Path afterEmissionsConfigPath = getUniqueTargetPath(Path.of(configPath.split(XML)[0] + AFTER));
			Path afterEmissionsNetworkPath = getUniqueTargetPath(Path.of(networkPath.split(XML)[0] + AFTER + ".gz"));
			Path afterEmissionsVehiclesPath = getUniqueTargetPath(Path.of(vehiclesPath.split(XML)[0] + AFTER + ".gz"));
			Path afterEmissionsTransitVehiclesPath = getUniqueTargetPath(Path.of(transitVehiclesPath.split(XML)[0] + AFTER + ".gz"));
			Files.copy(Path.of(configPath), afterEmissionsConfigPath);
			Files.copy(Path.of(networkPath), afterEmissionsNetworkPath);
			Files.copy(Path.of(vehiclesPath), afterEmissionsVehiclesPath);
			Files.copy(Path.of(transitVehiclesPath), afterEmissionsTransitVehiclesPath);
			Files.copy(beforeEmissionsConfigPath, Path.of(configPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsNetworkPath, Path.of(networkPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsVehiclesPath, Path.of(vehiclesPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsTransitVehiclesPath, Path.of(transitVehiclesPath), StandardCopyOption.REPLACE_EXISTING);
			Files.delete(beforeEmissionsConfigPath);
			Files.delete(beforeEmissionsNetworkPath);
			Files.delete(beforeEmissionsVehiclesPath);
			Files.delete(beforeEmissionsTransitVehiclesPath);
		}

		return 0;
	}

	@NotNull
	private String getCarFuelType() {
		String carFuelType;
		if (carEmissionConcept == MobilityToGridScenariosUtils.Hbefa41EmissionConcept.PETROL_4S) {
			carFuelType = "petrol (4S)";
		} else if (carEmissionConcept == MobilityToGridScenariosUtils.Hbefa41EmissionConcept.DIESEL) {
			carFuelType = "diesel";
		} else if (carEmissionConcept == MobilityToGridScenariosUtils.Hbefa41EmissionConcept.ELECTRICITY) {
			carFuelType = "electricity";
		} else {
			log.error("Invalid HBEFA 4.1 emission concept: {}.", carEmissionConcept);
			throw new IllegalStateException("");
		}
		return carFuelType;
	}

	private static Path getUniqueTargetPath(Path targetPath) {
		int counter = 1;
		Path uniquePath = targetPath;

		// Add a suffix if the file already exists
		while (Files.exists(uniquePath)) {
			String originalPath = targetPath.toString();
			int dotIndex = originalPath.lastIndexOf(".");
			if (dotIndex == -1) {
				uniquePath = Path.of(originalPath + "_" + counter);
			} else {
				uniquePath = Path.of(originalPath.substring(0, dotIndex) + "_" + counter + originalPath.substring(dotIndex));
			}
			counter++;
		}

		return uniquePath;
	}
}
