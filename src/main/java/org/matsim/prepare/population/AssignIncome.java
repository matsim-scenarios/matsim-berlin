package org.matsim.prepare.population;

import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.statistics.distribution.LogNormalDistribution;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Continuous draws within inferred SrV bands, stored as Äquivalenzeinkommen.
 * Infers bands from midpoint-based incomes; other positive incomes are retained.
 * Never reads original SrV records. LOGNORMAL fits one curve from prepared population attributes.
 * LOGNORMAL_BERLIN also uses published LOR gross-pay ratios as a proxy for spatial income differences.
 * Each person gets an independent draw, so household members are not assigned one shared household income.
 */
@CommandLine.Command(name = "assign-income")
public final class AssignIncome implements MATSimAppCommand, PersonAlgorithm {

	public enum Method { DRESDEN, LOGNORMAL, LOGNORMAL_BERLIN }

	// SrV household-income representatives, before division by weighted household size.
	private static final int[] REPRESENTATIVES = {250, 700, 1200, 1750, 2300, 2800, 3300, 4100, 5100, 6100};
	// Monthly household-income band boundaries in EUR. The lowest band starts at zero; the highest has no cap.
	private static final double[] BOUNDS = {0, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600, Double.POSITIVE_INFINITY};
	// Same seed as Dresden; reproducible for the same inputs and person iteration order.
	private final SplittableRandom rnd = new SplittableRandom(1234);
	private LogNormalDistribution logNormal;

	@CommandLine.Option(names = "--method", description = "Income assignment: ${COMPLETION-CANDIDATES}.")
	private Method method = Method.DRESDEN;

	@CommandLine.Option(names = "--input", required = true)
	private Path input;
	@CommandLine.Option(names = "--output", required = true)
	private Path output;

	public static void main(String[] args) {
		new AssignIncome().execute(args);
	}

	@Override
	public Integer call() {
		var population = PopulationUtils.readPopulation(input.toString());
		assignIncomeToPersons(population, method);
		PopulationUtils.writePopulation(population, output.toString());
		return 0;
	}

	public static void assignIncomeToPersons(Population population, Method method) {
		var assignment = new AssignIncome();
		assignment.method = method;
		// First fit one distribution from the unchanged input population; only then draw individual incomes.
		// DRESDEN needs no fitting and goes straight to the per-person assignment.
		if (method != Method.DRESDEN)
			assignment.logNormal = fitLogNormal(population);
		population.getPersons().values().forEach(assignment::run);
	}

	private static LogNormalDistribution fitLogNormal(Population population) {
		// counts[0] is class 1, ..., counts[9] is class 10. Each matching person contributes one observation.
		double[] counts = new double[REPRESENTATIVES.length];
		for (Person person : population.getPersons().values()) {
			if (!"person".equals(PopulationUtils.getSubpopulation(person)))
				continue;
			Double income = PersonUtils.getIncome(person);
			double size = householdSize(person);
			// Reproduce the conversion used to create the input income: max(249, round(representative / size)).
			// The first match wins if rounding/the floor makes classes ambiguous. Unmatched incomes are not fitted.
			for (int i = 0; income != null && i < REPRESENTATIVES.length; i++) {
				if (income == Math.max(249, Math.round(REPRESENTATIVES[i] / size))) {
					counts[i]++;
					break;
				}
			}
		}
		if (Arrays.stream(counts).sum() == 0)
			return null; // No reconstructable classes; existing continuous incomes are retained.

		// Grouped maximum likelihood: fit interval frequencies, not the midpoint amounts.
		// Counts weight persons equally; this estimates their household-income distribution.
		// Assume ln(household income) is normal: mu is its mean and sigma its standard deviation.
		// exp(mu) is the median household income; mu and sigma themselves are in log-income units.
		// Optimise mu and log(sigma), so sigma stays positive. Neither parameter is fixed.
		// Initial median 2500 EUR and sigma 0.7 are search starting values, not the fitted result.
		// 1e-9 are convergence tolerances; 2000 limits objective evaluations; 0.2 sets the initial search step.
		double[] fit = new SimplexOptimizer(1e-9, 1e-9).optimize(
			new MaxEval(2000), new InitialGuess(new double[]{Math.log(2500), Math.log(0.7)}),
			new NelderMeadSimplex(2, 0.2), GoalType.MINIMIZE,
			new ObjectiveFunction(p -> {
				// The optimiser's p[0] is mu; p[1] is log(sigma).
				var candidate = LogNormalDistribution.of(p[0], Math.exp(p[1]));
				double loss = 0;
				// Minimise -sum(count_i * log(P(income in band_i))) over the ten observed classes.
				// The numerical floor avoids log(0) when a candidate assigns zero probability to a populated band.
				for (int i = 0; i < counts.length; i++) {
					if (counts[i] > 0)
						loss -= counts[i] * Math.log(Math.max(Double.MIN_VALUE, candidate.probability(BOUNDS[i], BOUNDS[i + 1])));
				}
				return loss;
			})).getPoint();
		// getPoint() returns the fitted mu and log(sigma); the distribution constructor needs mu and sigma.
		return LogNormalDistribution.of(fit[0], Math.exp(fit[1]));
	}

	private static double householdSize(Person person) {
		// Weighted household size (equivalence scale), not the number of household members.
		// Both class reconstruction and equivalent-income conversion require a positive, finite value.
		Object size = person.getAttributes().getAttribute(Attributes.HOUSEHOLD_EQUIVALENT_SIZE);
		if (size == null)
			throw new IllegalArgumentException("Missing household_equivalent_size");
		double householdSize = Double.parseDouble(size.toString());
		if (!Double.isFinite(householdSize) || householdSize <= 0)
			throw new IllegalArgumentException("household_equivalent_size must be positive and finite");
		return householdSize;
	}

	@Override
	public void run(Person person) {
		// Freight, goods and commercial agents keep their existing attributes.
		if (!"person".equals(PopulationUtils.getSubpopulation(person)))
			return;

		Double currentIncome = PersonUtils.getIncome(person);
		double householdSize = householdSize(person);
		// Infer the class by reproducing Berlin's rounding and EUR 249 minimum.
		// This is not the original survey response; the first match selects the lower class if ambiguous.
		int incomeGroup = 0;
		for (int i = 0; currentIncome != null && i < REPRESENTATIVES.length; i++) {
			if (currentIncome == Math.max(249, Math.round(REPRESENTATIVES[i] / householdSize))) {
				incomeGroup = i + 1;
				break;
			}
		}
		// incomeGroup == 0 means no class matched, not an income of zero EUR.
		// Keep unmatched positive incomes, e.g. already smoothed output from a previous calibration run.
		// Missing, non-finite or non-positive incomes instead reach the shared fallback below.
		if (incomeGroup == 0 && currentIncome != null && Double.isFinite(currentIncome) && currentIncome > 0)
			return;

		if (method != Method.DRESDEN && incomeGroup > 0) {
			LogNormalDistribution distribution = logNormal;
			if (method == Method.LOGNORMAL_BERLIN) {
				Object lor = person.getAttributes().getAttribute(Attributes.LOR);
				// Outside Berlin, or with missing/suppressed LOR data, ratio = 1 leaves the curve unchanged.
				double median = BerlinIncomeUtils.medianGrossIncome(lor);
				// Gross individual pay is NOT household net/equivalent income: use only the relative spatial pattern.
				// Shifting mu by ln(local gross median / Berlin gross median) scales the curve's median by that ratio.
				// Sigma and class membership stay unchanged; this does not force local output medians to match the data.
				distribution = LogNormalDistribution.of(
					logNormal.getMu() + Math.log(median / BerlinIncomeUtils.BERLIN_MEDIAN_GROSS), logNormal.getSigma());
			}
			// Preserve this person's class. Observed class shares stay unchanged, so the final mixture
			// need not be exactly lognormal; the fitted curve determines the shape within each class.
			// F(x) is the fitted cumulative probability of household income <= x.
			// lower and upper are probabilities (0..1), not EUR amounts.
			double lower = distribution.cumulativeProbability(BOUNDS[incomeGroup - 1]);
			double upper = distribution.cumulativeProbability(BOUNDS[incomeGroup]);
			// Uniformly sample p between F(lower EUR bound) and F(upper EUR bound).
			// F^-1(p) then follows the lognormal conditional on this person's inferred class.
			// Exclude the zero/one CDF endpoints, which would give zero/infinite income.
			double p = rnd.nextDouble(Math.nextUp(lower), upper);
			// The new draw is HOUSEHOLD income: divide once by the equivalence scale, without rounding or a floor.
			PersonUtils.setIncome(person, distribution.inverseCumulativeProbability(p) / householdSize);
			// Stop here: neither lognormal method reaches the DRESDEN calculation after a successful draw.
			return;
		}

		// DRESDEN draws uniformly within the bands, with a half-normal upper tail in class 10.
		// The lowest uses (0, upper] to avoid zero income. Missing/invalid income uses the default for either method.
		double income = switch (incomeGroup) {
			//darf nicht bis null gehen...
			case 1 -> (1 - rnd.nextDouble()) * (500 / householdSize);
			case 2 -> rnd.nextDouble(500 / householdSize, 900 / householdSize);
			case 3 -> rnd.nextDouble(900 / householdSize, 1500 / householdSize);
			case 4 -> rnd.nextDouble(1500 / householdSize, 2000 / householdSize);
			case 5 -> rnd.nextDouble(2000 / householdSize, 2600 / householdSize);
			case 6 -> rnd.nextDouble(2600 / householdSize, 3000 / householdSize);
			case 7 -> rnd.nextDouble(3000 / householdSize, 3600 / householdSize);
			case 8 -> rnd.nextDouble(3600 / householdSize, 4600 / householdSize);
			case 9 -> rnd.nextDouble(4600 / householdSize, 5600 / householdSize);
			// Dresden's half-normal upper tail, starting at the SrV threshold of EUR 5600.
			case 10 -> (Math.abs(rnd.nextGaussian()) * 1000 + 5600) / householdSize;
			// Missing/invalid income uses Dresden's fallback: 2364 EUR per person/month (2021).
			// Dresden's source comment cites average gross household income of 4734 EUR/month
			// and about 2 persons per household (83.1M persons / 41.5M households).
			// Destatis, accessed 21 Sep. 2021 in the Dresden implementation:
			// https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html
			default -> 2364;
		};
		// Keep continuous equivalent income: no rounding, additional division or EUR 249 floor.
		PersonUtils.setIncome(person, income);
	}
}
