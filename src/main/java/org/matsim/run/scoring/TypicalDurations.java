package org.matsim.run.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.TypicalDurationCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * The typical durations a person's activities are scored against, kept on the person rather than on the activities.
 * <p>
 * The {@value #ATTRIBUTE} person attribute lists the person's main activities (stage activities excluded) in the
 * order of the plan, each with its typical duration in seconds: {@code home_morning=24944;work=29550;home_evening=41206}.
 * The n-th entry belongs to the n-th main activity; types may repeat. The type is there so that whoever pairs
 * activities with entries can check the pairing. All plans of a person share the sequence of activities, since no
 * replanning strategy of the scenario changes it.
 * <p>
 * On the person, the typical durations are not part of the plan an agent may change, and they are copied into the
 * experienced plans with the other person attributes. A plain String, in the shape of core's
 * {@code PersonUtils#getModeTasteVariations}, because writers with the default attribute converters drop other
 * classes silently. See {@link org.matsim.prepare.EncodeTypicalDuration} for where the values come from.
 */
public final class TypicalDurations {

	/** Person attribute holding the typical durations. */
	public static final String ATTRIBUTE = "typicalDurations";

	/** One main activity: its type and its typical duration in seconds. */
	public record Entry(String type, double seconds) {
		public Entry {
			if (type.contains(";") || type.contains("=")) {
				throw new IllegalArgumentException("Activity type " + type + " contains ';' or '=', which the encoding uses as separators.");
			}
		}
	}

	private TypicalDurations() {
	}

	/**
	 * The typical durations of the person's main activities in plan order, or null if the person has none (e.g. a
	 * stay-home agent, or a subpopulation scored against the config values).
	 */
	public static List<Entry> get(Person person) {
		Object value = person.getAttributes().getAttribute(ATTRIBUTE);
		return value == null ? null : parse(value.toString());
	}

	/**
	 * Sets the typical durations of the person's main activities, in plan order; null removes them.
	 */
	public static void set(Person person, List<Entry> entries) {
		if (entries == null) {
			person.getAttributes().removeAttribute(ATTRIBUTE);
		} else {
			person.getAttributes().putAttribute(ATTRIBUTE, format(entries));
		}
	}

	static String format(List<Entry> entries) {
		StringJoiner joiner = new StringJoiner(";");
		for (Entry entry : entries) {
			double s = entry.seconds();
			String seconds = s == Math.rint(s) && Math.abs(s) < 1e15 ? Long.toString((long) s) : Double.toString(s);
			joiner.add(entry.type() + "=" + seconds);
		}
		return joiner.toString();
	}

	static List<Entry> parse(String value) {
		List<Entry> entries = new ArrayList<>();
		if (value.isBlank()) {
			return entries;
		}
		for (String pair : value.split(";")) {
			int eq = pair.lastIndexOf('=');
			if (eq < 0) {
				throw new IllegalArgumentException("Malformed " + ATTRIBUTE + " entry '" + pair + "' in '" + value + "'; expected type=seconds.");
			}
			entries.add(new Entry(pair.substring(0, eq).trim(), Double.parseDouble(pair.substring(eq + 1).trim())));
		}
		return entries;
	}

	/**
	 * The typical duration calculator for a person. Persons must have a typical duration for every scored activity
	 * unless {@link BerlinScoringConfigGroup#isAllowConfigTypicalDurations()} is set; the other subpopulations use
	 * theirs where they have one and the config values otherwise.
	 */
	public static TypicalDurationCalculator forPerson(Config config, Person person) {
		BerlinScoringConfigGroup berlinScoring = ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class);
		boolean required = "person".equals(PopulationUtils.getSubpopulation(person)) && !berlinScoring.isAllowConfigTypicalDurations();
		return new PersonTypicalDurationCalculator(person, required);
	}
}
