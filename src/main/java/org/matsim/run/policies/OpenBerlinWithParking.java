package org.matsim.run.policies;

import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_ON_STREET_SPOTS;

/*
This class extends the matsim berlin scenario by parking functionality
 */

public class OpenBerlinWithParking extends OpenBerlinScenario {

	@CommandLine.Option(names = "--parking-supply",
		description = "Path to parking supply data", required = true)
	private String parkingSupply;

	@CommandLine.Option(names = "--alpha", description = "Alpha parameter for Belloche parking search time function", defaultValue = "0.11")
	private double alpha;

	@CommandLine.Option(names = "--beta", description = "Beta parameter for Belloche parking search time function", defaultValue = "-8.586")
	private double beta;

	@CommandLine.Option(names = "--noModeChoice", defaultValue = "true")
	private boolean noModeChoice;

	private static final Logger log = LogManager.getLogger(OpenBerlinWithParking.class );


	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinWithParking.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		Config preparedConfig = super.prepareConfig(config);
		preparedConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		if (noModeChoice) {
			// no mode choice if we simulate parking
			for (ReplanningConfigGroup.StrategySettings strategySettings : config.replanning().getStrategySettings()) {
				if (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)) {
					strategySettings.setWeight(0.0);
				}
			}
		}


		return preparedConfig;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		//get the sample size to scale parking capacities accordingly
		double sampleSize = scenario.getConfig().qsim().getFlowCapFactor();
		log.info("Using sample size of " + sampleSize + " to scale parking capacities.");
		// Read parking supply data from CSV and add to link attributes
		Map<Id<Link>, ParkingSpots> parkingSpotsPerLink = readCSV(parkingSupply);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (parkingSpotsPerLink.containsKey(link.getId())) {
				//log.info("Parking spots for " + link.getId() + ": on-street=" + parkingSpotsPerLink.get(link.getId()).onstreetSpots + ", off-street=" + parkingSpotsPerLink.get(link.getId()).offstreetSpots);
				//log.info("Scaled parking spots for " + link.getId() + ": on-street=" + onStreetParkingSpots + ", off-street=" + offStreetParkingSpots);
				link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, (int) Math.round(parkingSpotsPerLink.get(link.getId()).onstreetSpots * sampleSize));
				link.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, (int) Math.round(parkingSpotsPerLink.get(link.getId()).offstreetSpots * sampleSize));
			}
		}

		NetworkUtils.writeNetwork(scenario.getNetwork(), "network-v6.4-with-parking.xml.gz");
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("ParkingOccupancyOberserver").to(ParkingOccupancyObserver.class);
				addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ParkingOccupancyObserver.class).in(Singleton.class);
				bind(ParkingCapacityInitializer.class).to(PlanBasedParkingCapacityInitializerBerlin.class);
				bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
				bind(KernelDistance.class).toInstance(new ConstantKernelDistance(500));
				// use parameters from Belloche Paper https://pdf.sciencedirectassets.com/308315/1-s2.0-S2352146515X00032/1-s2.0-S2352146515000526/main.pdf?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEKn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCICjqKOg8BiZaX434gG0V6S68pKD3x%2BfiiAj529bEHVzEAiEAize6AxQ2S06wRldce%2BX2Pn7tRssYhfhLdYHCcfOm3CMqsgUIEhAFGgwwNTkwMDM1NDY4NjUiDJTkFL2yfN9d7Een5SqPBdoyLYsf38Quz5jeOoOlee6BoBYPWd3pdF5Ft4OFuEtY2vBT8D1mNvwjiIc5o9EMbil%2Bn4J4ubPZLgahREJtFLKvTN%2FSYWaQlaNAr7g%2FFqyOMkNkZ4zRmuTFZHuhUCDkBsFzM7%2BP%2FeB509RzVnNtEFcXnSGmM5CU2QLXxaSpydoaYjNJa7GHot4wTWpAQSPNms98xmQw9SOcZpEEJKs5KbRpzjNzE%2Fw8oOckV4%2FcfXmCNboUbUVp7R%2Bt58iQakgTgumFGS2Mi1sJsVoRu0fc6EnGLsigMrrjftN8xAL7ravb0qhYY%2F1ohHd0LzzFmoJK32pNJonSai6zT97ttRM8PCH7hqoInHlVoRkm7asM8BUdDltEKQwxqG7i37IURCJj5ppO1EDK8YUoiJjgZUDFsl25g0zMGCD4UHpTiFnpl2JkrqHi0QN6du7OwhWUvu7mNUbrAkOleA79iT5FxS6x%2FMIYv2c63rGYxDy5uR5VZcZOS%2FQf5xH9yi5dnjgEMFdFO8%2FfiCyHNUP5a%2B6TQxJvoWcIP9dxZR0CeYQh4hguWeUiiKrFE4Z751wjgqMEEh6%2BktHL6Oh4wcnPm%2BRLjerz1AeQ7lqRyEJ6Jeef3qxbLPZpAum6AYFeKRwQeS6RDDfIgfNgrJVShSskjLUM9rxsgMMVZSreXSRzabXuYIdS%2BnV6sECS%2BahjG2XYX5VXSU%2BDAK8RKqHW323obs4iCBfWx6hMHPuBm57dWfSS829PiOEg7N1Y7u7IhN2nw99t9bwLiSKz6DJO%2FvQg5yeNtO61tYtEj5qxiHu8Sl4Myo3k%2Ba%2FgltmdpjddFAP7Gj%2BWDNc8G1KCN9nFKpNcMQp8%2Fc1evd0irMNgPjIeDi8fZQRpntowzt%2BJvwY6sQF3KQaY7OKk8oCrJwBhqBCXka4LjU%2BmSSEtBfnCSGyP2rGdPMG%2BEa5A4UoXaOve97jMdluCz8qxtStBCcFIGtOlVkduNXiYL%2BEK9Mfsrsh7by6oPxl%2BYjEhxfB%2BcTta4%2BODDQlxAFYuTh9TMyzFkSsrpug2HAxx5wOs02Nyr3i3N%2B1EyurdvJS7wxnn5yUhaglO%2FTrYeIlRXygDrQeOKrcvPBaIHVLm%2Bo7YyQWHH%2BtFFoI%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250325T091640Z&X-Amz-SignedHeaders=host&X-Amz-Expires=300&X-Amz-Credential=ASIAQ3PHCVTYQYKBTS5L%2F20250325%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=c2f30fe9ebfee1301f9171ec0be610f8f6264eef89de1593c609a77eb2eff192&hash=35e4e916f56003c6740345396f4f9903fabcef13107cfa7a35d4b431a1044238&host=68042c943591013ac2b2430a89b270f6af2c76d8dfd086a07176afe7c76c2c61&pii=S2352146515000526&tid=spdf-2ba191d3-1250-4cf4-afd7-42eeada590bd&sid=9d2025d3623a93452e5b8c37c012e71dc9d7gxrqb&type=client&tsoh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&rh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&ua=1e035650550554095a5d0d&rr=925d4f92bb04e531&cc=de
				//f(occ, K) = alpha * exp(-beta * (occ / K))
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(alpha, beta));
				addControlerListenerBinding().to(ParkingOccupancyObserver.class);
				addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
			}
		});
	}

	public record ParkingSpots(int onstreetSpots, int offstreetSpots) {
	}

	/*
	Read parking supply data from a CSV file and return a map of Link IDs to ParkingSpots objects.
	 */

	public static Map<Id<Link>, ParkingSpots> readCSV(String filePath) {
		Map<Id<Link>, ParkingSpots> parkingMap = new HashMap<>();
		String line;

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			// Skip the header line
			br.readLine();

			// Read each subsequent line in the CSV
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");

				// Assuming the order: id, onstreet_spots, offstreet_spots
				Id<Link> id = Id.createLinkId(values[0]);
				int onstreetSpots = parseSpotValue(values[1]);
				int offstreetSpots = parseSpotValue(values[2]);

				// Create a ParkingData object and store it in the map
				ParkingSpots parkingSpots = new ParkingSpots(onstreetSpots, offstreetSpots);
				parkingMap.put(id, parkingSpots);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.out.println("Error: Invalid number format.");
		}

		return parkingMap;
	}

	// Helper method to handle NA and parse the number
	private static int parseSpotValue(String value) {
		if (value.equalsIgnoreCase("NA")) {
			return 0; // Treat "NA" as zero
		}
		return Integer.parseInt(value); // Otherwise, parse the integer
	}


}
