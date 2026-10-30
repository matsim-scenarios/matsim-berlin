package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.Category;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dashboard.PriceElasticitiesDashboard;
import org.matsim.simwrapper.SimWrapper;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Compare person trips using the same demographic categories as Berlin's Trips dashboard. */
@CommandLine.Command(name = "price-elasticities", mixinStandardHelpOptions = true,
	description = "Generate an elasticity dashboard from completed base and experiment runs, without simulating.")
public final class PriceElasticitiesAnalysis implements MATSimAppCommand {

	public static final String ALL = "All persons";
	public static final List<String> CATEGORIES = List.of("age", "income", "employment", "economic_status", "zone");
	public static final String DIRECTORY = "analysis/price-elasticities";
	private static final String UNKNOWN = "Unknown";
	private static final double EPS = 1e-9;
	private static final String[] HEADER = {"Scenario", "Experiment", "Run type", "Price component", "Cost change [%]",
		"Category", "Group", "Mode", "Base trips", "Trips", "Trip change [trips]", "Trip change [%]", "Elasticity", "Status"};

	@CommandLine.Option(names = "--output", defaultValue = "output/elasticities",
		description = "Dashboard directory. Without explicit inputs, scans its run subdirectories and uses its base folder.")
	private Path output;

	@CommandLine.Option(names = "--base", description = "Base run output directory. Default: <output>/base.")
	private Path basePath;

	@CommandLine.Parameters(arity = "0..*", paramLabel = "<experiment>",
		description = "Experiment output directories. Default: run subdirectories of --output, excluding the base.")
	private List<Path> experiments = new ArrayList<>();

	public static void main(String[] args) {
		new PriceElasticitiesAnalysis().execute(args);
	}

	@Override
	public Integer call() throws IOException {
		Path base = basePath == null ? output.resolve("base") : basePath;
		List<Path> inputs = experiments.isEmpty() ? findExperiments(output, base) : experiments;
		Result results = run(output, base, inputs);
		SimWrapper simWrapper = SimWrapper.create();
		simWrapper.addDashboard(new PriceElasticitiesDashboard(results));
		simWrapper.generate(output);
		return 0;
	}

	public record Row(String experiment, String scenario, String price, double carFactor, double carDailyFactor, double ptFactor, Double costChange,
					  String category, String group, String mode, long baseTrips, long trips, long tripDelta,
					  Double tripChange, Double elasticity, String status) {
	}

	public record Result(List<String> groups, List<Row> rows) {
	}

	private record Key(String category, String group, String mode) {
	}

	private record Cost(String group, String mode, String parameter) {
	}

	private record Run(String name, Config config, Map<Key, Long> counts, Map<Cost, Double> costs) {
	}

	private record Prices(double carFactor, double carDailyFactor, double ptFactor, String component, Double change, String label) {
	}

	/** Read completed runs, validate them, and write overview and demographic comparisons. */
	public static Result run(Path directory) throws IOException {
		Path base = directory.resolve("base");
		return run(directory, base, findExperiments(directory, base));
	}

	/** Compare explicitly selected run directories, regardless of the scenario runner or folder naming scheme. */
	public static Result run(Path directory, Path baseDirectory, List<Path> experimentDirectories) throws IOException {
		Map<String, Set<String>> labels = categoryLabels();
		Map<String, Category> categories = new LinkedHashMap<>();
		labels.forEach((name, values) -> categories.put(name, new Category(values)));
		Run base = read(baseDirectory, categories);
		List<Run> runs = new ArrayList<>(List.of(base));
		Set<Path> paths = new HashSet<>(List.of(baseDirectory.toAbsolutePath().normalize()));
		Set<String> names = new HashSet<>(List.of(base.name));
		for (Path input : experimentDirectories) {
			if (!paths.add(input.toAbsolutePath().normalize()))
				throw new IllegalArgumentException("Duplicate run directory: " + input);
			Run run = read(input, categories);
			if (!names.add(run.name)) {
				run = new Run(input.toAbsolutePath().normalize().toString(), run.config, run.counts, run.costs);
				names.add(run.name);
			}
			runs.add(run);
		}
		if (paths.contains(directory.toAbsolutePath().normalize()))
			throw new IllegalArgumentException("Dashboard output must differ from the input run directories: " + directory);
		if (runs.size() == 1)
			throw new IllegalArgumentException("No experiment results found in " + directory);
		for (Run run : runs) validate(base, run);
		runs.subList(1, runs.size()).sort(Comparator.comparing((Run r) -> prices(base, r).component)
			.thenComparingDouble(r -> prices(base, r).carFactor).thenComparingDouble(r -> prices(base, r).carDailyFactor)
			.thenComparingDouble(r -> prices(base, r).ptFactor)
			.thenComparing(Run::name));

		Map<String, Set<String>> groups = new LinkedHashMap<>();
		groups.put(ALL, new LinkedHashSet<>(List.of(ALL)));
		labels.forEach((category, values) -> groups.put(category, new LinkedHashSet<>(values)));
		TreeSet<String> modes = new TreeSet<>();
		for (Run run : runs) {
			run.counts.keySet().forEach(k -> {
				groups.get(k.category).add(k.group);
				modes.add(k.mode);
			});
		}
		if (modes.isEmpty())
			throw new IllegalArgumentException("No non-truck trips for subpopulation person in " + directory);

		List<Row> rows = new ArrayList<>();
		for (Run run : runs) {
			Prices prices = prices(base, run);
			for (var category : groups.entrySet()) {
				for (String group : category.getValue()) {
					for (String mode : modes) {
						Key key = new Key(category.getKey(), group, mode);
						long before = base.counts.getOrDefault(key, 0L);
						long after = run.counts.getOrDefault(key, 0L);
						Double demandChange = before == 0 ? null : (double) after / before - 1;
						String status = run == base ? "Base" : prices.change == null ? "Multiple prices changed"
							: equal(prices.change, 0) ? "No price change" : before == 0 ? "No baseline trips" : "OK";
						Double elasticity = status.equals("OK") ? demandChange / prices.change : null;
						rows.add(new Row(run.name, prices.label, prices.component, prices.carFactor, prices.carDailyFactor, prices.ptFactor,
							prices.change == null ? null : prices.change * 100, category.getKey(), group, mode, before, after,
							after - before, demandChange == null ? null : demandChange * 100, elasticity, status));
					}
				}
			}
		}
		Path output = directory.resolve(DIRECTORY);
		Files.createDirectories(output);
		write(output.resolve("elasticities.csv"), rows);
		List<String> tabs = List.copyOf(groups.keySet());
		for (int i = 0; i < tabs.size(); i++) {
			String category = tabs.get(i);
			write(output.resolve("group-" + i + ".csv"), rows.stream().filter(r -> r.category.equals(category)).toList());
		}
		writePrices(output.resolve("prices.csv"), base, runs);
		return new Result(tabs, List.copyOf(rows));
	}

	private static List<Path> findExperiments(Path directory, Path base) throws IOException {
		List<Path> inputs = new ArrayList<>();
		try (var children = Files.list(directory)) {
			for (Path child : children.filter(Files::isDirectory).sorted().toList()) {
				if (child.toAbsolutePath().normalize().equals(base.toAbsolutePath().normalize())) continue;
				try (var files = Files.list(child)) {
					if (files.anyMatch(p -> p.getFileName().toString().endsWith("output_config.xml")
						|| p.getFileName().toString().contains("persons.csv") || p.getFileName().toString().contains("trips.csv")))
						inputs.add(child);
				}
			}
		}
		return inputs;
	}

	private static Map<String, Set<String>> categoryLabels() throws IOException {
		Map<String, Set<String>> labels = new LinkedHashMap<>();
		CATEGORIES.forEach(category -> labels.put(category, new LinkedHashSet<>()));
		try (var reader = new InputStreamReader(Objects.requireNonNull(PriceElasticitiesAnalysis.class
			.getResourceAsStream("/mode_share_per_group_dist_ref.csv"), "Missing Trips demographic reference data"), StandardCharsets.UTF_8);
			 var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
			for (var row : csv)
				labels.forEach((category, values) -> {
					if (!row.get(category).isBlank()) values.add(row.get(category));
				});
		}
		return labels;
	}

	private static Run read(Path directory, Map<String, Category> categories) throws IOException {
		if (!Files.isDirectory(directory))
			throw new IllegalArgumentException("Missing run directory: " + directory);
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", directory).toString());
		Map<String, List<String>> persons = new HashMap<>();
		Path file = ApplicationUtils.matchInput("persons.csv", directory);
		try (var csv = csv(file)) {
			requireColumns(csv, file, "person", "subpopulation");
			for (var row : csv) {
				if (!row.get("subpopulation").equals("person")) continue;
				List<String> groups = new ArrayList<>();
				categories.forEach((category, classifier) -> {
					String raw = row.isMapped(category) ? row.get(category) : null;
					String value = raw == null || raw.isBlank() || raw.equalsIgnoreCase("NaN") ? null : classifier.categorize(raw);
					groups.add(value == null || value.isBlank() ? UNKNOWN : value);
				});
				if (persons.putIfAbsent(row.get("person"), groups) != null)
					throw new IllegalArgumentException("Duplicate person in " + file + ": " + row.get("person"));
			}
		}
		if (persons.isEmpty())
			throw new IllegalArgumentException("No agents with subpopulation person in " + file);
		Map<Key, Long> counts = new HashMap<>();
		file = ApplicationUtils.matchInput("trips.csv", directory);
		try (var csv = csv(file)) {
			requireColumns(csv, file, "person", "main_mode");
			for (var row : csv) {
				List<String> groups = persons.get(row.get("person"));
				if (groups == null) continue;
				String mode = row.get("main_mode");
				if (mode.isBlank() && row.isMapped("longest_distance_mode")) mode = row.get("longest_distance_mode");
				if (mode.isBlank()) throw new IllegalArgumentException("Missing trip mode in " + file);
				if (mode.toLowerCase(Locale.ROOT).startsWith("truck") || mode.equals("freight")) continue;
				counts.merge(new Key(ALL, ALL, mode), 1L, Long::sum);
				for (int i = 0; i < CATEGORIES.size(); i++)
					counts.merge(new Key(CATEGORIES.get(i), groups.get(i), mode), 1L, Long::sum);
			}
		}
		Map<Cost, Double> costs = new HashMap<>();
		config.scoring().getScoringParametersPerSubpopulation().forEach((group, scoring) ->
			scoring.getModes().forEach((mode, params) -> {
				costs.put(new Cost(group, mode, "monetaryDistanceRate"), params.getMonetaryDistanceRate());
				costs.put(new Cost(group, mode, "dailyMonetaryConstant"), params.getDailyMonetaryConstant());
			}));
		return new Run(directory.getFileName().toString(), config, counts, costs);
	}

	private static CSVParser csv(Path file) throws IOException {
		return CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
			.setDelimiter(CsvOptions.detectDelimiter(file.toString())).build().parse(IOUtils.getBufferedReader(file.toString()));
	}

	private static void requireColumns(CSVParser csv, Path file, String... columns) {
		if (!csv.getHeaderMap().keySet().containsAll(List.of(columns)))
			throw new IllegalArgumentException("Missing required columns " + List.of(columns) + " in " + file);
	}

	private static void validate(Run base, Run run) {
		if (!equal(base.config.qsim().getFlowCapFactor(), run.config.qsim().getFlowCapFactor())
			|| !equal(base.config.qsim().getStorageCapFactor(), run.config.qsim().getStorageCapFactor()))
			throw new IllegalArgumentException("Different sample/capacity factors in " + run.name);
		if (!Objects.equals(base.config.plans().getInputFile(), run.config.plans().getInputFile())
			|| base.config.global().getRandomSeed() != run.config.global().getRandomSeed()
			|| base.config.controller().getFirstIteration() != run.config.controller().getFirstIteration()
			|| base.config.controller().getLastIteration() != run.config.controller().getLastIteration())
			throw new IllegalArgumentException("Different starting plans, seed or iteration range in " + run.name);
		if (!base.costs.keySet().equals(run.costs.keySet()))
			throw new IllegalArgumentException("Different scoring modes/subpopulations in " + run.name);
		for (Cost key : base.costs.keySet()) {
			if (key.mode.equals("car")
				|| key.mode.equals("pt") && key.parameter.equals("dailyMonetaryConstant")) continue;
			if (!equal(base.costs.get(key), run.costs.get(key)))
				throw new IllegalArgumentException("Unrelated monetary parameter changed in " + run.name + ": " + key);
		}
	}

	private static Prices prices(Run base, Run run) {
		double car = factor(base, run, "car", "monetaryDistanceRate");
		double carDaily = factor(base, run, "car", "dailyMonetaryConstant");
		double pt = factor(base, run, "pt", "dailyMonetaryConstant");
		boolean distanceChanged = !equal(car, 1), dailyChanged = !equal(carDaily, 1), ptChanged = !equal(pt, 1);
		boolean carChanged = distanceChanged || dailyChanged;
		boolean combinedCar = distanceChanged && dailyChanged;
		boolean differentCarFactors = combinedCar && !equal(car, carDaily);
		String component = combinedCar ? "Car distance and daily" : distanceChanged ? "Car distance" : dailyChanged ? "Car daily" : "";
		String label = combinedCar ? differentCarFactors ? "Car distance " + percent(car) + " / daily " + percent(carDaily)
			: "Car distance + daily " + percent(car) : distanceChanged ? "Car " + percent(car) : dailyChanged ? "Car daily " + percent(carDaily) : "";
		if (ptChanged) {
			component += (carChanged ? " and " : "") + "PT daily";
			label += (carChanged ? " / " : "") + "PT " + percent(pt);
		}
		// A common multiplier for both car components defines one price change, not their sum.
		Double change = (carChanged && ptChanged) || differentCarFactors ? null
			: distanceChanged ? car - 1 : dailyChanged ? carDaily - 1 : pt - 1;
		return new Prices(car, carDaily, pt, component.isEmpty() ? "Unchanged" : component, change,
			run == base ? "Base (continued)" : label.isEmpty() ? run.name : label);
	}

	private static String percent(double factor) {
		double value = (factor - 1) * 100;
		return (value > 0 ? "+" : "") + decimal(value, 4) + "%";
	}

	private static double factor(Run base, Run run, String mode, String parameter) {
		Double factor = null;
		for (Cost key : base.costs.keySet()) {
			if (!key.mode.equals(mode) || !key.parameter.equals(parameter)) continue;
			double before = base.costs.get(key), after = run.costs.get(key);
			if (!Double.isFinite(before) || !Double.isFinite(after) || before == 0 && after != 0)
				throw new IllegalArgumentException("Cannot determine price factor in " + run.name + ": " + key);
			if (before == 0) continue;
			double candidate = after / before;
			if (!Double.isFinite(candidate) || candidate < 0 || factor != null && !equal(factor, candidate))
				throw new IllegalArgumentException("Non-uniform or invalid price factor in " + run.name + ": " + key);
			factor = candidate;
		}
		return factor == null ? 1 : factor;
	}

	private static boolean equal(double first, double second) {
		return Math.abs(first - second) <= EPS * Math.max(1, Math.max(Math.abs(first), Math.abs(second)));
	}

	private static String decimal(Double value, int places) {
		return value == null ? null : BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private static void write(Path file, List<Row> rows) throws IOException {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(file), CSVFormat.DEFAULT.builder().setHeader(HEADER).build())) {
			for (Row row : rows)
				csv.printRecord(row.scenario, row.experiment, row.status.equals("Base") ? "Base" : "Experiment", row.price,
					decimal(row.costChange, 2), row.category, row.group, row.mode, row.baseTrips, row.trips, row.tripDelta,
					decimal(row.tripChange, 2), decimal(row.elasticity, 3), row.status);
		}
	}

	private static void writePrices(Path file, Run base, List<Run> runs) throws IOException {
		String[] header = {"Scenario", "Experiment", "Car factor", "Car daily factor", "PT factor", "Base car rate [EUR/km]", "Car rate [EUR/km]",
			"Base car daily constant [EUR/day]", "Car daily constant [EUR/day]", "Base PT daily constant [EUR/day]", "PT daily constant [EUR/day]"};
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(file), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (Run run : runs) {
				Prices prices = prices(base, run);
				csv.printRecord(prices.label, run.name, decimal(prices.carFactor, 8), decimal(prices.carDailyFactor, 8), decimal(prices.ptFactor, 8),
					decimal(personCost(base, "car", true), 8), decimal(personCost(run, "car", true), 8),
					decimal(personCost(base, "car", false), 8), decimal(personCost(run, "car", false), 8),
					decimal(personCost(base, "pt", false), 8), decimal(personCost(run, "pt", false), 8));
			}
		}
	}

	private static double personCost(Run run, String mode, boolean distance) {
		var params = run.config.scoring().getScoringParameters("person").getModes().get(mode);
		return params == null ? 0 : distance ? params.getMonetaryDistanceRate() * 1000 : params.getDailyMonetaryConstant();
	}
}
