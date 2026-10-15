package org.matsim.run;

import org.matsim.analysis.PriceElasticitiesAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.dashboard.PriceElasticitiesDashboard;
import org.matsim.simwrapper.SimWrapper;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Run the normal Berlin scenario with optional multipliers for car distance costs and PT daily costs.
 */
@CommandLine.Command(header = ":: Open Berlin Cost Elasticities ::", version = OpenBerlinScenario.VERSION,
	mixinStandardHelpOptions = true, showDefaultValues = true)
public class RunOpenBerlinElasticities extends OpenBerlinScenario {

	@CommandLine.Option(names = "--car-monetary-distance-rate-factor", defaultValue = "1.0",
		description = "Multiplier for car monetaryDistanceRate (e.g. 1.1 increases the cost by 10%).")
	private double carFactor = 1.0;

	@CommandLine.Option(names = "--pt-daily-monetary-constant-factor", defaultValue = "1.0",
		description = "Multiplier for PT dailyMonetaryConstant (e.g. 1.6 increases the cost by 60%).")
	private double ptFactor = 1.0;

	@CommandLine.Option(names = "--report", description = "Generate the single comparison dashboard from completed runs under --output, without simulating.")
	private boolean report;

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinElasticities.class, args);
	}

	@Override
	public Integer call() throws Exception {
		if (!report)
			return super.call();
		Path directory = output == null ? Path.of("output/elasticities") : output;
		var results = PriceElasticitiesAnalysis.run(directory);
		SimWrapper simWrapper = SimWrapper.create();
		simWrapper.addDashboard(new PriceElasticitiesDashboard(results));
		simWrapper.generate(directory);
		return 0;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		if (!Double.isFinite(carFactor) || carFactor < 0 || !Double.isFinite(ptFactor) || ptFactor < 0)
			throw new IllegalArgumentException("Cost factors must be finite and non-negative.");

		var config = scenario.getConfig();
		for (var scoring : config.scoring().getScoringParametersPerSubpopulation().values()) {
			var car = scoring.getModes().get(TransportMode.car);
			if (car != null)
				car.setMonetaryDistanceRate(car.getMonetaryDistanceRate() * carFactor);
			var pt = scoring.getModes().get(TransportMode.pt);
			if (pt != null)
				pt.setDailyMonetaryConstant(pt.getDailyMonetaryConstant() * ptFactor);
		}
		String name = carFactor == 1.0 && ptFactor == 1.0 ? "base" : "car-" + carFactor + "_pt-" + ptFactor;
		config.controller().setOutputDirectory(Path.of(config.controller().getOutputDirectory(), name).toString());
		config.controller().setRunId(name.replace('.', 'p'));
	}
}
