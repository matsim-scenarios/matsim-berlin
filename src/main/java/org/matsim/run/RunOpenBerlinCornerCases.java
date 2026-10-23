package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.AgentWiseComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.vsp.scenario.CornerCases;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs one of the policy cases provided by {@link CornerCases} on top of the Open Berlin scenario.
 *
 * <p>The base case continues to run through {@link OpenBerlinScenario}. This class is used only for policy runs,
 * and every invocation applies exactly one policy to freshly prepared base inputs. Spatial policies require a local
 * shapefile supplied through {@code --policy-shp}.</p>
 */
@CommandLine.Command(header = ":: Open Berlin Corner Cases ::", version = OpenBerlinScenario.VERSION,
		mixinStandardHelpOptions = true, showDefaultValues = true)
// MATSimApplication does not inherit this annotation, so expose the same analysis as the base scenario.
@MATSimApplication.Analysis({AgentWiseComparison.class})
public final class RunOpenBerlinCornerCases extends OpenBerlinScenario {

	private static final Logger log = LogManager.getLogger(RunOpenBerlinCornerCases.class);

	static final String FIRST_HOUR_PARKING_COST_ATTRIBUTE = "firstHourParkingCost";
	static final String EXTRA_HOUR_PARKING_COST_ATTRIBUTE = "extraHourParkingCost";
	static final String RESIDENTIAL_PARKING_FEE_ATTRIBUTE = "residentialParkingFee";
	static final String BIKE_SPEED_FACTOR_ATTRIBUTE = "berlinPolicyBikeSpeedFactor";

	// CornerCases excludes exact link type values; motorways and trunks stay unchanged by default.
	static final String DEFAULT_EXCLUDED_ROAD_TYPES =
			"highway.motorway,highway.motorway_link,highway.trunk,highway.trunk_link";

	@CommandLine.Option(names = "--policy", required = true,
			description = "Policy case: ${COMPLETION-CANDIDATES}.")
	private PolicyCase policy;

	@CommandLine.Option(names = "--policy-shp", paramLabel = "<file.shp>",
			description = "Local shape file used by spatial policies.")
	private Path policyShape;

	@CommandLine.Option(names = "--policy-factor", paramLabel = "<factor>",
			description = "Bike speed factor (> 0 and not 1), or car speed/capacity factor in (0, 1).")
	private Double policyFactor;

	@CommandLine.Option(names = "--excluded-road-types", split = ",",
			defaultValue = DEFAULT_EXCLUDED_ROAD_TYPES,
			description = "Exact network type values excluded from CAR_SPEED and CAR_CAPACITY.")
	private String[] excludedRoadTypes = DEFAULT_EXCLUDED_ROAD_TYPES.split(",");

	@CommandLine.Option(names = "--first-hour-parking-cost", paramLabel = "<amount>",
			description = "Parking cost for the first hour.")
	private Double firstHourParkingCost;

	@CommandLine.Option(names = "--extra-hour-parking-cost", paramLabel = "<amount>",
			description = "Parking cost for every additional hour.")
	private Double extraHourParkingCost;

	@CommandLine.Option(names = "--residential-parking-fee", paramLabel = "<amount>",
			description = "Residential parking fee.")
	private Double residentialParkingFee;

	public static void main(String[] args) {
		MATSimApplication.execute(RunOpenBerlinCornerCases.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		validatePolicyOptions();
		Config prepared = super.prepareConfig(config);

		if (policy == PolicyCase.PARKING_COST) {
			CornerCases.prepareConfigForParkingCost(prepared);
		}

		log.info("Running Berlin policy {} with shape={}, factor={}, excludedRoadTypes={}",
				policy, policyShape, policyFactor, excludedRoadTypeSet());
		return prepared;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		applyPolicy(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		if (policy == PolicyCase.PARKING_COST) {
			CornerCases.prepareControllerForParkingCost(controler);
		}
	}

	/**
	 * Applies exactly one selected policy and verifies that a spatial policy actually touched the network.
	 * Package visibility is intentional for focused unit tests.
	 */
	void applyPolicy(Scenario scenario) {
		validatePolicyOptions();
		rejectPreexistingPolicyMarkers(scenario);

		switch (policy) {
			case BIKE_SPEED -> {
				CornerCases.modifyBikeSpeed(scenario, policyFactor);
				scenario.getNetwork().getAttributes().putAttribute(BIKE_SPEED_FACTOR_ATTRIBUTE, policyFactor);
			}
			case CAR_SPEED -> {
				CornerCases.reduceCarSpeed(scenario, normalizedPolicyShape(), policyFactor, excludedRoadTypeSet());
				verifyModifiedLinks(scenario, CornerCases.SPEED_REDUCTION_FACTOR_ATTRIBUTE);
			}
			case CAR_CAPACITY -> {
				CornerCases.reduceCarCapacities(scenario, normalizedPolicyShape(), policyFactor,
						excludedRoadTypeSet());
				verifyModifiedLinks(scenario, CornerCases.CAPACITY_REDUCTION_FACTOR_ATTRIBUTE);
			}
			case PARKING_COST -> {
				CornerCases.prepareScenarioForParkingCost(scenario, normalizedPolicyShape(), firstHourParkingCost,
						extraHourParkingCost, residentialParkingFee);
				verifyModifiedLinks(scenario, FIRST_HOUR_PARKING_COST_ATTRIBUTE);
			}
		}
	}

	private void validatePolicyOptions() {
		if (policy == null) {
			throw new IllegalArgumentException("--policy must be set.");
		}

		switch (policy) {
			case BIKE_SPEED -> validatePositiveFactor();
			case CAR_SPEED, CAR_CAPACITY -> {
				validateShape();
				validateReductionFactor();
			}
			case PARKING_COST -> {
				validateShape();
				validateNonNegative("--first-hour-parking-cost", firstHourParkingCost);
				validateNonNegative("--extra-hour-parking-cost", extraHourParkingCost);
				validateNonNegative("--residential-parking-fee", residentialParkingFee);
				if (firstHourParkingCost == 0. && extraHourParkingCost == 0. && residentialParkingFee == 0.) {
					throw new IllegalArgumentException("At least one parking cost must be greater than zero.");
				}
			}
		}
	}

	private void validateShape() {
		if (policyShape == null) {
			throw new IllegalArgumentException("--policy-shp is required for policy " + policy + ".");
		}
		if (!Files.isRegularFile(normalizedPolicyShape())) {
			throw new IllegalArgumentException("Policy shape does not exist or is not a file: " + policyShape);
		}
	}

	private void validatePositiveFactor() {
		if (policyFactor == null || !Double.isFinite(policyFactor) || policyFactor <= 0. || policyFactor == 1.) {
			throw new IllegalArgumentException(
					"--policy-factor must be finite, greater than zero, and different from one for " + policy + ".");
		}
	}

	private void validateReductionFactor() {
		if (policyFactor == null || !Double.isFinite(policyFactor) || policyFactor <= 0. || policyFactor >= 1.) {
			throw new IllegalArgumentException("--policy-factor must be finite and in (0, 1) for " + policy + ".");
		}
	}

	private static void rejectPreexistingPolicyMarkers(Scenario scenario) {
		if (scenario.getNetwork().getAttributes().getAttribute(BIKE_SPEED_FACTOR_ATTRIBUTE) != null) {
			throw new IllegalStateException("The input network already contains Berlin policy markers. "
					+ "Every case must start from the calibrated base inputs.");
		}

		Set<String> linkMarkers = Set.of(
				CornerCases.SPEED_REDUCTION_FACTOR_ATTRIBUTE,
				CornerCases.CAPACITY_REDUCTION_FACTOR_ATTRIBUTE,
				FIRST_HOUR_PARKING_COST_ATTRIBUTE,
				EXTRA_HOUR_PARKING_COST_ATTRIBUTE,
				RESIDENTIAL_PARKING_FEE_ATTRIBUTE
		);
		boolean containsPolicyMarker = scenario.getNetwork().getLinks().values().stream()
				.anyMatch(link -> linkMarkers.stream()
						.anyMatch(marker -> link.getAttributes().getAttribute(marker) != null));
		if (containsPolicyMarker) {
			throw new IllegalStateException("The input network already contains Berlin policy markers. "
					+ "Every case must start from the calibrated base inputs.");
		}
	}

	private static void validateNonNegative(String option, Double value) {
		if (value == null || !Double.isFinite(value) || value < 0.) {
			throw new IllegalArgumentException(option + " must be finite and non-negative.");
		}
	}

	private Path normalizedPolicyShape() {
		return policyShape.toAbsolutePath().normalize();
	}

	private void verifyModifiedLinks(Scenario scenario, String attribute) {
		long modifiedLinks = scenario.getNetwork().getLinks().values().stream()
				.filter(link -> link.getAttributes().getAttribute(attribute) != null)
				.count();
		log.info("Policy {} marked {} network links with attribute {}.", policy, modifiedLinks, attribute);
		if (modifiedLinks == 0) {
			throw new IllegalStateException("Policy " + policy + " did not modify any network link. "
					+ "Check the policy shape CRS and the exact excluded road types.");
		}
	}

	private Set<String> excludedRoadTypeSet() {
		return Arrays.stream(excludedRoadTypes)
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	public enum PolicyCase {
		BIKE_SPEED,
		CAR_SPEED,
		CAR_CAPACITY,
		PARKING_COST
	}
}
