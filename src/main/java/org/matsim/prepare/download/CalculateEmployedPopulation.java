package org.matsim.prepare.download;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
		name = "calc-employment-rate",
		description = "Calculate employment rate from population and labor statistics."
)
public class CalculateEmployedPopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CalculateEmployedPopulation.class);


	@CommandLine.Option(names = "--labor", description = "Path to labor.csv", required = true)
	private Path labor;

	@CommandLine.Option(names = "--employment", description = "Path to employment.csv", required = true)
	private Path employment;

	@CommandLine.Option(names = "--output", description = "Path to employment_rate.json", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions();

	public static void main(String[] args) {
		new CalculateEmployedPopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Map<Integer, Integer> employment = readEmployment(this.employment);
		Map<Integer, Employment> labor = readLabor(this.labor);

		// Scale labor statistics to employment statistics
		for (Map.Entry<Integer, Employment> e : labor.entrySet()) {

			Integer employed = employment.get(e.getKey());

			if (employed == null) {
				log.warn("No employment entry for {}", e.getKey());
				continue;
			}

			double f = employed / e.getValue().sum();

			if (f < 1) {
				log.warn("Rate is less than 1 for {}", e.getKey());
				continue;
			}

			e.getValue().scale(f);
		}

		JsonMapper mapper = new JsonMapper();

		mapper.writerFor(new TypeReference<Map<Integer, Employment>>() {
				})
				.withDefaultPrettyPrinter()
				.writeValue(Files.newBufferedWriter(output), labor);

		return 0;
	}

	private Map<Integer, Integer> readEmployment(Path path) throws IOException {

		Map<Integer, Integer> result = new HashMap<>();

		try (CSVParser parser = csv.createParser(path)) {

			for (CSVRecord record : parser) {

				String code = record.get("code");
				if (code.equals("DG"))
					continue;

				result.put(Integer.parseInt(code), Integer.parseInt(record.get("employed")));
			}
		}

		return result;
	}

	private Map<Integer, Employment> readLabor(Path path) throws IOException {

		Map<Integer, Employment> result = new HashMap<>();

		try (CSVParser parser = csv.createParser(path)) {
			for (CSVRecord record : parser) {

				String code = record.get("code");
				if (code.equals("DG"))
					continue;

				int gem = Integer.parseInt(code);
				Employment row = result.computeIfAbsent(gem, k -> new Employment());

				for (String gender : List.of("m", "f")) {

					String age = record.get("age");
					Entry entry = gender.equals("m") ? row.men : row.women;
					int n = Integer.parseInt(record.get(gender));

					switch (age) {
						case "0 - 20" -> entry.age18_20 += n;
						case "20 - 25" -> entry.age20_25 += n;
						case "25 - 30" -> entry.age25_30 += n;
						case "30 - 50" -> entry.age30_50 += n;
						case "50 - 60" -> entry.age50_60 += n;
						case "60 - 65" -> entry.age60_65 += n;
						case "65 - inf" -> entry.age65_75 += n;
						default -> throw new IllegalStateException("Unknown age group: " + age);
					}
				}
			}
		}

		return result;
	}

	public static final class Employment {

		@JsonProperty
		public final Entry men = new Entry();

		@JsonProperty
		public final Entry women = new Entry();

		public double sum() {
			return men.sum() + women.sum();
		}

		/**
		 * Multiplier all entries with a factor.
		 */
		void scale(double f) {
			men.scale(f);
			women.scale(f);
		}
	}

	/**
	 * Age grouping specific for available data.
	 */
	public static final class Entry {
		@JsonProperty
		double age18_20;
		@JsonProperty
		double age20_25;
		@JsonProperty
		double age25_30;
		@JsonProperty
		double age30_50;
		@JsonProperty
		double age50_60;
		@JsonProperty
		double age60_65;
		@JsonProperty
		double age65_75;

		public double sum() {
			return age18_20 + age20_25 + age25_30 + age30_50 + age50_60 + age60_65 + age65_75;
		}

		/**
		 * Subtract value from the age groups.
		 *
		 * @param amount amount to subtract
		 * @param age    age
		 * @return true if the result is larger than zero.
		 */
		public boolean subtract(double amount, int age) {

			if (age >= 75 || age < 18)
				return false;

			if (age < 20)
				return (age18_20 -= amount) >= 0;
			if (age < 25)
				return (age20_25 -= amount) >= 0;
			if (age < 30)
				return (age25_30 -= amount) >= 0;
			if (age < 50)
				return (age30_50 -= amount) >= 0;
			if (age < 60)
				return (age50_60 -= amount) >= 0;
			if (age < 65)
				return (age60_65 -= amount) >= 0;

			return (age65_75 -= amount) >= 0;
		}

		void scale(double f) {
			age18_20 *= f;
			age20_25 *= f;
			age25_30 *= f;
			age30_50 *= f;
			age50_60 *= f;
			age60_65 *= f;
			age65_75 *= f;
		}
	}
}
