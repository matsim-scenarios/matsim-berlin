package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import picocli.CommandLine;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Draw each person's deviation from the mode constants, i.e. the random alternative specific constant of the estimated
 * mixed logit model, and store it as the person's mode taste variations.
 * <p>
 * The deviation is drawn once, here, instead of in every run: the scoring adds the stored value to the mode constant of
 * the config ({@link org.matsim.core.config.groups.TasteVariationsConfigParameterSet} with variationsOf=constant), so
 * the population carries the taste distribution and every run that reads it - the ASC calibration, the scenario, a
 * policy case - scores the same person the same way. The constant in the config stays the mean of the distribution and
 * is what the ASC calibration tunes.
 * <p>
 * Only the subpopulation that is scored with taste variations gets a deviation. Modes without a standard deviation are
 * left alone: walk is the reference mode, and the bus constant is a fixed part of the estimated model, not a taste.
 */
@CommandLine.Command(
	name = "draw-mode-constant-variations",
	description = "Draw and store each person's deviation from the mode constants."
)
public class DrawModeConstantVariations implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DrawModeConstantVariations.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--sigma", split = ",", required = true, description = "Standard deviation of the " +
		"normally distributed deviation from the mode constant, per mode, e.g. --sigma car=1.5,pt=1.7. These are the " +
		"estimated standard deviations of the random constants of the choice model.")
	private Map<String, Double> sigmas = new LinkedHashMap<>();

	@CommandLine.Option(names = "--subpopulation", defaultValue = "person", description = "Only persons of this " +
		"subpopulation get a deviation; the others are excluded from the taste variations in the config anyway.")
	private String subpopulation;

	@CommandLine.Option(names = "--seed", defaultValue = "1", description = "Seed of the draws. Together with the " +
		"person id it determines a person's deviations, so they do not depend on the order persons are processed in.")
	private long seed;

	public static void main(String[] args) {
		new DrawModeConstantVariations().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

//		sorted, so that the draws do not depend on the order the modes were given on the command line
		Map<String, Double> modes = new TreeMap<>(sigmas);
		modes.values().removeIf(sigma -> sigma == null || sigma <= 0);

		if (modes.isEmpty()) {
			log.error("No mode has a positive standard deviation: {}", sigmas);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		int drawn = 0;
		for (Person person : population.getPersons().values()) {

			if (!subpopulation.equals(PopulationUtils.getSubpopulation(person)))
				continue;

			SplittableRandom rnd = initRandomNumberGenerator(person);

			Map<String, Map<ModeUtilityParameters.Type, Double>> variations = new LinkedHashMap<>();
			for (Map.Entry<String, Double> mode : modes.entrySet()) {
				Map<ModeUtilityParameters.Type, Double> variation = new EnumMap<>(ModeUtilityParameters.Type.class);
				variation.put(ModeUtilityParameters.Type.constant, rnd.nextGaussian() * mode.getValue());
				variations.put(mode.getKey(), variation);
			}

			PersonUtils.setModeTasteVariations(person, variations);
			drawn++;
		}

		log.info("Drew deviations of {} for {} persons of subpopulation {}", modes, drawn, subpopulation);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Initializes random number generator with person specific seed.
	 */
	private SplittableRandom initRandomNumberGenerator(Person person) {
		BigInteger i = new BigInteger(person.getId().toString().getBytes());
		SplittableRandom rnd = new SplittableRandom(i.longValue() + seed * 31);

		// warm up the random number generator
		for (int j = 0; j < 100; j++) {
			rnd.nextDouble();
		}

		return rnd;
	}
}
