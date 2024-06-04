package org.matsim.prepare.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PersonUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class is used to read and match persons from the reference data in csv format.
 */
public class PersonMatcher implements Iterable<Map.Entry<String, CSVRecord>> {

	private static final Logger log = LogManager.getLogger(PersonMatcher.class);

	private final String idxColumn;

	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);
	private final Map<Key, List<String>> groups = new HashMap<>();
	private final Map<String, CSVRecord> persons = new HashMap<>();

	public PersonMatcher(String idxColumn, Path personsPath) {
		this.idxColumn = idxColumn;

		try (CSVParser parser = csv.createParser(personsPath)) {
			buildSubgroups(parser);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Match reference person to a person in the population.
	 * @return person id
	 */
	public String matchPerson(Person person, SplittableRandom rnd) {

		Key key = createKey(person);

		List<String> subgroup = groups.get(key);
		if (subgroup == null) {
			log.error("No subgroup found for key {}", key);
			throw new IllegalStateException("Invalid entry");
		}

		if (subgroup.size() < 30) {
			log.warn("Group {} has low sample size: {}", key, subgroup.size());
		}

		return subgroup.get(rnd.nextInt(subgroup.size()));
	}

	/**
	 * Matches a person csv entry to one person in a list.
	 * @return null if no match was found
	 */
	public Person matchEntry(CSVRecord p, List<Person> refPersons, SplittableRandom rnd) {

		int regionType = Integer.parseInt(p.get("region_type"));
		String gender = p.get("gender");
		String employment = p.get("employment");
		int age = Integer.parseInt(p.get("age"));

		Set<Key> keys = createKey(gender, age, regionType, employment).collect(Collectors.toSet());

		List<Person> matched = refPersons.stream()
			.filter(person -> keys.contains(createKey(person)))
			.toList();

		if (matched.isEmpty())
			return null;

		return matched.get(rnd.nextInt(matched.size()));
	}

	/**
	 * Return reference person with given index.
	 */
	public CSVRecord getPerson(String personId) {
		return persons.get(personId);
	}

	/**
	 * Create subpopulations for sampling.
	 */
	private void buildSubgroups(CSVParser csv) {

		int i = 0;

		for (CSVRecord r : csv) {

			String idx = r.get(idxColumn);
			int regionType = Integer.parseInt(r.get("region_type"));
			String gender = r.get("gender");
			String employment = r.get("employment");
			int age = Integer.parseInt(r.get("age"));

			Stream<Key> keys = createKey(gender, age, regionType, employment);
			keys.forEach(key -> groups.computeIfAbsent(key, (k) -> new ArrayList<>()).add(idx));
			persons.put(idx, r);
			i++;
		}

		log.info("Read {} persons from csv.", i);
	}

	private Stream<Key> createKey(String gender, int age, int regionType, String employment) {
		if (age < 6) {
			return IntStream.rangeClosed(0, 5).mapToObj(i -> new Key(null, i, regionType, null));
		}
		if (age <= 10) {
			return IntStream.rangeClosed(6, 10).mapToObj(i -> new Key(null, i, regionType, null));
		}
		if (age < 18) {
			return IntStream.rangeClosed(11, 18).mapToObj(i -> new Key(gender, i, regionType, null));
		}

		Boolean isEmployed = age > 65 ? null : !employment.equals("unemployed");
		int min = Math.max(18, age - 6);
		int max = Math.min(65, age + 6);

		// larger groups for older people
		if (age > 65) {
			min = Math.max(66, age - 10);
			max = Math.min(99, age + 10);
		}

		return IntStream.rangeClosed(min, max).mapToObj(i -> new Key(gender, i, regionType, isEmployed));
	}

	private Key createKey(Person person) {

		Integer age = PersonUtils.getAge(person);
		String gender = PersonUtils.getSex(person);
		if (age <= 10)
			gender = null;

		Boolean employed = PersonUtils.isEmployed(person);
		if (age < 18 || age > 65)
			employed = null;

		int regionType = (int) person.getAttributes().getAttribute(Attributes.RegioStaR7);

		// Region types have been reduced to 1 and 3
		if (regionType != 1)
			regionType = 3;

		return new Key(gender, age, regionType, employed);
	}

	@NotNull
	@Override
	public Iterator<Map.Entry<String, CSVRecord>> iterator() {
		return persons.entrySet().iterator();
	}

	/**
	 * Key used to match persons.
	 */
	public record Key(String gender, int age, int regionType, Boolean employed) {
	}

}
