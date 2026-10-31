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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs a combination of the policy cases provided by {@link CornerCases} on top of the Open Berlin scenario.
 *
 * <p>The base case continues to run through {@link OpenBerlinScenario}. This class is used only for policy runs,
 * and every invocation applies its selected policies once to freshly prepared base inputs. Spatial policies require a local
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

	@CommandLine.Option(names = "--policy", required = true, split = ",",
			description = "Policy cases: ${COMPLETION-CANDIDATES}. Repeat the option or separate policies with commas.")
	private List<PolicyCase> policies = new ArrayList<>();

	@CommandLine.Option(names = "--policy-shp", paramLabel = "<file.shp>",
			description = "Local shape file used by spatial policies.")
	private Path policyShape;

	@CommandLine.Option(names = "--policy-factor", paramLabel = "<factor>",
			description = "Factor for a single policy. For combinations, use the separate policy factor options.")
	private Double policyFactor;

	@CommandLine.Option(names = "--bike-speed-factor", description = "Bike speed multiplier (> 0 and not 1).")
	private Double bikeSpeedFactor;

	@CommandLine.Option(names = "--car-speed-factor", description = "Car speed multiplier in (0, 1).")
	private Double carSpeedFactor;

	@CommandLine.Option(names = "--car-capacity-factor", description = "Car capacity multiplier in (0, 1).")
	private Double carCapacityFactor;

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

		if (policies.contains(PolicyCase.PARKING_COST)) {
			CornerCases.prepareConfigForParkingCost(prepared);
		}

		log.info("Running Berlin policies {} with shape={}, excludedRoadTypes={}",
				policies, policyShape, excludedRoadTypeSet());
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

		if (policies.contains(PolicyCase.PARKING_COST)) {
			CornerCases.prepareControllerForParkingCost(controler);
		}
	}

	/**
	 * Applies each selected policy once and verifies that every spatial policy actually touched the network.
	 * Package visibility is intentional for focused unit tests.
	 */
	void applyPolicy(Scenario scenario) {
		validatePolicyOptions();
		rejectPreexistingPolicyMarkers(scenario);

		for (PolicyCase policy : EnumSet.copyOf(policies)) switch (policy) {
			case BIKE_SPEED -> {
				CornerCases.modifyBikeSpeed(scenario, factor(policy));
				scenario.getNetwork().getAttributes().putAttribute(BIKE_SPEED_FACTOR_ATTRIBUTE, factor(policy));
			}
			case CAR_SPEED -> {
				CornerCases.reduceCarSpeed(scenario, normalizedPolicyShape(), factor(policy), excludedRoadTypeSet());
				verifyModifiedLinks(scenario, policy, CornerCases.SPEED_REDUCTION_FACTOR_ATTRIBUTE);
			}
			case CAR_CAPACITY -> {
				CornerCases.reduceCarCapacities(scenario, normalizedPolicyShape(), factor(policy),
						excludedRoadTypeSet());
				verifyModifiedLinks(scenario, policy, CornerCases.CAPACITY_REDUCTION_FACTOR_ATTRIBUTE);
			}
			case PARKING_COST -> {
				CornerCases.prepareScenarioForParkingCost(scenario, normalizedPolicyShape(), firstHourParkingCost,
						extraHourParkingCost, residentialParkingFee);
				verifyModifiedLinks(scenario, policy, FIRST_HOUR_PARKING_COST_ATTRIBUTE);
			}
		}
	}

	private void validatePolicyOptions() {
		if (policies.isEmpty()) {
			throw new IllegalArgumentException("--policy must be set.");
		}
		if (policyFactor != null && EnumSet.copyOf(policies).size() > 1)
			throw new IllegalArgumentException("For combinations, use --bike-speed-factor, --car-speed-factor and --car-capacity-factor instead of --policy-factor.");

		for (PolicyCase policy : EnumSet.copyOf(policies)) switch (policy) {
			case BIKE_SPEED -> validatePositiveFactor(factor(policy));
			case CAR_SPEED, CAR_CAPACITY -> {
				validateShape();
				validateReductionFactor(factor(policy));
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

	private Double factor(PolicyCase policy) {
		Double specific = switch (policy) {
			case BIKE_SPEED -> bikeSpeedFactor;
			case CAR_SPEED -> carSpeedFactor;
			case CAR_CAPACITY -> carCapacityFactor;
			case PARKING_COST -> null;
		};
		if (specific != null && policyFactor != null)
			throw new IllegalArgumentException("Use either --policy-factor or the separate factor for " + policy + ".");
		return specific != null ? specific : policyFactor;
	}

	private void validateShape() {
		if (policyShape == null) {
			throw new IllegalArgumentException("--policy-shp is required for spatial policies.");
		}
		if (!Files.isRegularFile(normalizedPolicyShape())) {
			throw new IllegalArgumentException("Policy shape does not exist or is not a file: " + policyShape);
		}
	}

	private void validatePositiveFactor(Double factor) {
		if (factor == null || !Double.isFinite(factor) || factor <= 0. || factor == 1.) {
			throw new IllegalArgumentException(
					"--bike-speed-factor (or --policy-factor for a single policy) must be finite, greater than zero, and different from one.");
		}
	}

	private void validateReductionFactor(Double factor) {
		if (factor == null || !Double.isFinite(factor) || factor <= 0. || factor >= 1.) {
			throw new IllegalArgumentException("--car-speed-factor/--car-capacity-factor (or --policy-factor for a single policy) must be finite and in (0, 1).");
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

	private void verifyModifiedLinks(Scenario scenario, PolicyCase policy, String attribute) {
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
