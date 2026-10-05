package org.matsim.prepare.population;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.math3.special.Erf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.math.BigInteger;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

//TODO think about Brandenburg residents
/**
 * Makes the income distribution continuous and adjusts its spatial pattern to observed LOR medians.
 *
 * <p>The observed medians are used only as relative spatial factors. This is intentional: the available LOR
 * statistic describes gross earnings of full-time employees, whereas the population contains equivalised household
 * income. Applying the observed levels directly would mix two different income definitions. The command therefore
 * transfers only the ratios between LORs.</p>
 *
 * <p>The existing population income is the midpoint of an SrV household-income class divided by the household's
 * equivalence scale. The command reconstructs that class, estimates one Berlin-wide log-normal distribution from the
	 * resulting interval-censored observations, and draws from that distribution conditional on each person's original
	 * class. A LOR-specific multiplicative location shift is then applied to these draws and clipped at the original
	 * class limits. The shift is calibrated so that the requested local median is met whenever this is compatible with
	 * those limits. Infeasible targets are clipped to the closest feasible median.</p>
 */
@CommandLine.Command(
	name = "assign-spatial-income",
	description = "Create continuous person incomes and adjust their spatial distribution using LOR medians."
)
public class AssignSpatialIncome implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AssignSpatialIncome.class);
	private static final DataFormatter EXCEL_FORMATTER = new DataFormatter(Locale.GERMAN);
	private static final double MEDIAN_BOUNDARY_TOLERANCE_EUR = 1.0;

	@CommandLine.Option(names = "--input", description = "Input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--income-xlsx", description = "Excel table with LOR ids and median incomes", required = true)
	private Path incomeXlsx;

	@CommandLine.Option(names = "--sheet", description = "Excel sheet name or zero-based sheet index", defaultValue = "0")
	private String sheet;

	@CommandLine.Option(names = "--lor-column", description = "Header of the LOR id column; detected automatically when omitted")
	private String lorColumn;

	@CommandLine.Option(names = "--median-column", description = "Header of the median-income column; detected automatically when omitted")
	private String medianColumn;

	@CommandLine.Option(names = "--shp-id-attribute", description = "LOR id attribute in the shape file", defaultValue = "PLR_ID")
	private String shapeIdAttribute;

	@CommandLine.Option(names = "--seed", description = "Seed for deterministic person-specific draws", defaultValue = "4711")
	private long seed;

	@CommandLine.Option(names = "--spatial-exponent", description = "Exponent applied to relative LOR medians (0=no spatial shift, 1=full relative shift)", defaultValue = "1")
	private double spatialExponent;

	@CommandLine.Option(names = "--income-class-lower-bounds", split = ",", description = "Comma-separated lower bounds of the SrV monthly household-net-income classes", defaultValue = "0,500,900,1500,2000,2600,3000,3600,4600,5600")
	private double[] householdIncomeClassLowerBounds;

	@CommandLine.Option(names = "--minimum-income", description = "Minimum monthly income where compatible with the original SrV class", defaultValue = "249")
	private double minimumIncome;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation to adjust", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new AssignSpatialIncome().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		log.info("Starting spatial income assignment: input={}, output={}, subpopulation={}, seed={}, spatialExponent={}",
			input, output, subpopulation, seed, spatialExponent);
		log.info("Using SrV household-income class lower bounds {} and a minimum equivalent income of {} EUR where compatible with the class.",
			Arrays.toString(householdIncomeClassLowerBounds), minimumIncome);

		if (!shp.isDefined()) {
			log.error("A current LOR shape file is required. Specify it with --shp and --shp-crs.");
			return 2;
		}

		if (!Double.isFinite(spatialExponent) || spatialExponent < 0) {
			log.error("--spatial-exponent must be finite and non-negative.");
			return 2;
		}
		if (!Double.isFinite(minimumIncome) || minimumIncome <= 0) {
			log.error("--minimum-income must be finite and positive.");
			return 2;
		}

		if (!validClassBounds(householdIncomeClassLowerBounds)) {
			log.error("--income-class-lower-bounds must start at zero and be finite, non-negative, and strictly increasing.");
			return 2;
		}

		log.info("Reading spatial median incomes from {} (sheet {}).", incomeXlsx, sheet);
		Map<String, Double> observedMedians = readObservedMedians();
		if (observedMedians.isEmpty()) {
			log.error("No LOR median incomes could be read from {}.", incomeXlsx);
			return 2;
		}

		log.info("Loading MATSim population from {}.", input);
		Population population = PopulationUtils.readPopulation(input.toString());
		log.info("Loaded {} persons. Creating the LOR spatial index and matching home coordinates.",
			population.getPersons().size());
		ShpOptions.Index lorIndex = shp.createIndex(OpenBerlinScenario.CRS, shapeIdAttribute);

		Map<String, List<IncomeEntry>> byLor = new LinkedHashMap<>();
		int noLor = 0;
		int noIncome = 0;
		int noEquivalentSize = 0;
		int relaxedMinimum = 0;

		for (Person person : ProgressBar.wrap(population.getPersons().values(), "Indexing incomes by LOR")) {
			if (!subpopulation.equals(PopulationUtils.getSubpopulation(person)))
				continue;

			Double income = PersonUtils.getIncome(person);
			if (income == null || !Double.isFinite(income) || income <= 0) {
				noIncome++;
				continue;
			}

			Object spatialId = lorIndex.query(Attributes.getHomeCoord(person));
			String lorId = normalizeLorId(spatialId);
			if (lorId == null) {
				noLor++;
				continue;
			}

			double spatialMedian = findSpatialMedian(lorId, observedMedians);
			if (!Double.isFinite(spatialMedian)) {
				noLor++;
				continue;
			}

			Object equivalentSizeAttribute = person.getAttributes().getAttribute(Attributes.HOUSEHOLD_EQUIVALENT_SIZE);
			if (!(equivalentSizeAttribute instanceof Number equivalentSizeNumber)
				|| !Double.isFinite(equivalentSizeNumber.doubleValue()) || equivalentSizeNumber.doubleValue() <= 0) {
				noEquivalentSize++;
				continue;
			}

			double equivalentSize = equivalentSizeNumber.doubleValue();
			IncomeBounds bounds = incomeBounds(income, equivalentSize, minimumIncome, householdIncomeClassLowerBounds);
			if (bounds.minimumRelaxed())
				relaxedMinimum++;

			double quantile = uniform(person.getId().toString(), seed);
			byLor.computeIfAbsent(lorId, id -> new ArrayList<>())
				.add(new IncomeEntry(person, income, bounds, quantile, spatialMedian));
		}

		List<IncomeEntry> entries = byLor.values().stream().flatMap(List::stream).toList();
		if (entries.isEmpty()) {
			log.error("No persons could be matched to the income table and the LOR shape file.");
			return 2;
		}

		log.info("Matched {} persons to {} LORs. Fitting the Berlin-wide interval-censored log-normal income distribution.",
			entries.size(), byLor.size());
		LogNormalFit fit = fitLogNormal(entries);
		double originalBerlinMedian = median(entries.stream().map(IncomeEntry::originalIncome).toList());
		double originalBerlinMean = mean(entries.stream().map(IncomeEntry::originalIncome).toList());
		double observedBerlinMedian = median(entries.stream().map(IncomeEntry::spatialMedian).toList());
		log.info("Log-normal fit completed: mu={}, sigma={}. Original equivalent-income mean/median={}/{} EUR; population-weighted observed LOR median={} EUR.",
			fit.mu(), fit.sigma(), originalBerlinMean, originalBerlinMedian, observedBerlinMedian);
		log.info("Calibrating the continuous income distribution separately for {} LORs.", byLor.size());

		List<ProposedIncome> proposed = new ArrayList<>(entries.size());
		int clippedTargets = 0;
		int calibratedLors = 0;
		double largestMedianError = 0;
		for (Map.Entry<String, List<IncomeEntry>> group : byLor.entrySet()) {
			List<IncomeEntry> local = group.getValue();
			double spatialRatio = local.get(0).spatialMedian() / observedBerlinMedian;
			double desiredLocalMedian = originalBerlinMedian * Math.pow(spatialRatio, spatialExponent);
			CalibrationResult calibrated = calibrateMedian(local, fit, desiredLocalMedian);
			if (calibrated.clipped()) {
				clippedTargets++;
				log.warn("LOR {} cannot attain requested median {} without leaving SrV classes; using {} instead.",
					group.getKey(), calibrated.requestedTarget(), calibrated.appliedTarget());
			}
			largestMedianError = Math.max(largestMedianError,
				Math.abs(calibrated.achievedMedian() - calibrated.appliedTarget()));
			log.debug("LOR {}: persons={}, observedSpatialMedian={}, spatialRatio={}, requestedMedian={}, appliedMedian={}, achievedMedian={}, locationShift={}",
				group.getKey(), local.size(), local.get(0).spatialMedian(), spatialRatio,
				calibrated.requestedTarget(), calibrated.appliedTarget(), calibrated.achievedMedian(),
				calibrated.locationShift());

			for (int i = 0; i < local.size(); i++) {
				IncomeEntry entry = local.get(i);
				double income = calibrated.incomes().get(i);
				if (!Double.isFinite(income) || income <= 0)
					throw new IllegalStateException("Non-positive or non-finite income for person " + entry.person().getId()
						+ " in LOR " + group.getKey() + ".");
				if (income < entry.bounds().lower()
					|| (entry.bounds().upper() > entry.bounds().lower()
					&& Double.isFinite(entry.bounds().upper()) && income >= entry.bounds().upper()))
					throw new IllegalStateException("Income " + income + " for person " + entry.person().getId()
						+ " leaves the reconstructed SrV class in LOR " + group.getKey() + ".");
				proposed.add(new ProposedIncome(entry.person(), income));
			}

			calibratedLors++;
			if (calibratedLors % 50 == 0 || calibratedLors == byLor.size())
				log.info("Calibrated {}/{} LORs ({} infeasible targets clipped so far).",
					calibratedLors, byLor.size(), clippedTargets);
		}

		log.info("Applying {} continuous incomes and writing the adjusted population to {}.", proposed.size(), output);
		for (ProposedIncome value : proposed)
			PersonUtils.setIncome(value.person(), value.income());

		PopulationUtils.writePopulation(population, output.toString());

		double finalMedian = median(proposed.stream().map(ProposedIncome::income).toList());
		double finalMean = mean(proposed.stream().map(ProposedIncome::income).toList());

		log.info("Fitted interval-censored log-normal income distribution with mu={} and sigma={}.", fit.mu(), fit.sigma());
		log.info("Adjusted {} persons in {} LORs. Original Berlin mean/median: {}/{}; final mean/median: {}/{}.",
			proposed.size(), byLor.size(), originalBerlinMean, originalBerlinMedian, finalMean, finalMedian);
		log.info("Clipped {} infeasible LOR median targets; largest numerical median error was {} EUR.",
			clippedTargets, largestMedianError);
		log.info("Skipped {} persons without a usable income, {} without a spatial income match, and {} without a usable equivalence scale. For {} persons the minimum income was relaxed to preserve the original SrV class.",
			noIncome, noLor, noEquivalentSize, relaxedMinimum);
		log.info("Spatial income assignment completed successfully: {}.", output);

		return 0;
	}

	private Map<String, Double> readObservedMedians() throws Exception {
		Map<String, List<Double>> values = new LinkedHashMap<>();

		try (XSSFWorkbook workbook = new XSSFWorkbook(OPCPackage.open(incomeXlsx.toFile(), PackageAccess.READ))) {
			Sheet inputSheet = selectSheet(workbook);
			Header header = findHeader(inputSheet);

			for (int r = header.row() + 1; r <= inputSheet.getLastRowNum(); r++) {
				Row row = inputSheet.getRow(r);
				if (row == null)
					continue;

				String lorId = normalizeLorId(EXCEL_FORMATTER.formatCellValue(row.getCell(header.lorColumn())));
				double median = readNumber(row.getCell(header.medianColumn()));
				if (lorId == null || !Double.isFinite(median) || median <= 0)
					continue;

				values.computeIfAbsent(lorId, id -> new ArrayList<>()).add(median);
			}
		}

		Map<String, Double> result = new LinkedHashMap<>();
		values.forEach((id, medians) -> result.put(id, median(medians)));
		log.info("Read median incomes for {} LORs from {}.", result.size(), incomeXlsx);
		return result;
	}

	private Sheet selectSheet(XSSFWorkbook workbook) {
		try {
			int index = Integer.parseInt(sheet);
			return workbook.getSheetAt(index);
		} catch (NumberFormatException e) {
			Sheet result = workbook.getSheet(sheet);
			if (result == null)
				throw new IllegalArgumentException("Excel sheet not found: " + sheet);
			return result;
		}
	}

	private Header findHeader(Sheet inputSheet) {
		String expectedLor = normalizeHeader(lorColumn);
		String expectedMedian = normalizeHeader(medianColumn);

		for (int r = inputSheet.getFirstRowNum(); r <= Math.min(inputSheet.getLastRowNum(), 50); r++) {
			Row row = inputSheet.getRow(r);
			if (row == null)
				continue;

			int lor = -1;
			int income = -1;
			for (Cell cell : row) {
				String value = normalizeHeader(EXCEL_FORMATTER.formatCellValue(cell));
				if ((expectedLor != null && expectedLor.equals(value)) || (expectedLor == null && isLorHeader(value)))
					lor = cell.getColumnIndex();
				if ((expectedMedian != null && expectedMedian.equals(value)) || (expectedMedian == null && isMedianHeader(value)))
					income = cell.getColumnIndex();
			}

			if (lor >= 0 && income >= 0)
				return new Header(r, lor, income);
		}

		throw new IllegalArgumentException("Could not find the LOR and median-income columns in sheet "
			+ inputSheet.getSheetName() + ". Specify them with --lor-column and --median-column.");
	}

	static boolean isLorHeader(String value) {
		if (value == null)
			return false;

		return value.equals("lor") || value.equals("plr id") || value.equals("plr_id") || value.equals("raumid")
			|| ((value.contains("plr") || value.contains("planungsraum") || value.contains("lor"))
			&& (value.contains("id") || value.contains("schlussel") || value.contains("code")));
	}

	private static boolean isMedianHeader(String value) {
		if (value == null)
			return false;

		return value.contains("median")
			&& (value.contains("einkomm") || value.contains("entgelt") || value.contains("brutto"));
	}

	static String normalizeLorId(Object value) {
		if (value == null)
			return null;

		String text;
		if (value instanceof Number number)
			text = Long.toString(number.longValue());
		else
			text = value.toString().trim().replaceFirst("[.,]0+$", "");

		text = text.replaceAll("[^0-9]", "");
		if (text.isEmpty())
			return null;

		return text.length() < 8 ? "0".repeat(8 - text.length()) + text : text;
	}

	static double parseGermanNumber(String value) {
		if (value == null || value.isBlank())
			return Double.NaN;

		String number = value.trim().replaceAll("[^0-9,.-]", "");
		if (number.isEmpty() || number.equals("-") || number.equals("."))
			return Double.NaN;

		int comma = number.lastIndexOf(',');
		int dot = number.lastIndexOf('.');
		if (comma >= 0) {
			number = number.replace(".", "").replace(',', '.');
		} else if (dot >= 0 && number.length() - dot - 1 == 3) {
			number = number.replace(".", "");
		}

		try {
			return Double.parseDouble(number);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private static double readNumber(Cell cell) {
		if (cell == null)
			return Double.NaN;

		return switch (cell.getCellType()) {
			case NUMERIC -> cell.getNumericCellValue();
			case FORMULA -> cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
				? cell.getNumericCellValue() : parseGermanNumber(EXCEL_FORMATTER.formatCellValue(cell));
			default -> parseGermanNumber(EXCEL_FORMATTER.formatCellValue(cell));
		};
	}

	private static String normalizeHeader(String value) {
		if (value == null || value.isBlank())
			return null;

		return Normalizer.normalize(value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ")
			.trim();
	}

	private static double findSpatialMedian(String lorId, Map<String, Double> medians) {
		Double exact = medians.get(lorId);
		if (exact != null)
			return exact;

		// Suppressed or missing PLR values fall back through the LOR hierarchy: BZR, PGR, district, Berlin.
		for (int prefix : List.of(6, 4, 2)) {
			if (lorId.length() < prefix)
				continue;

			String key = lorId.substring(0, prefix);
			List<Double> candidates = medians.entrySet().stream()
				.filter(e -> e.getKey().startsWith(key))
				.map(Map.Entry::getValue)
				.toList();
			if (!candidates.isEmpty())
				return median(candidates);
		}

		return median(new ArrayList<>(medians.values()));
	}

	static boolean validClassBounds(double[] bounds) {
		if (bounds == null || bounds.length < 2 || bounds[0] != 0)
			return false;

		for (int i = 0; i < bounds.length; i++) {
			if (!Double.isFinite(bounds[i]) || bounds[i] < 0 || (i > 0 && bounds[i] <= bounds[i - 1]))
				return false;
		}
		return true;
	}

	/**
	 * Reconstruct the original SrV household-income interval and convert it to equivalent-income bounds.
	 */
	static IncomeBounds incomeBounds(double income, double equivalentSize, double minimumIncome, double[] classLowerBounds) {
		if (!Double.isFinite(income) || income <= 0 || !Double.isFinite(equivalentSize) || equivalentSize <= 0)
			throw new IllegalArgumentException("Income and equivalence scale must be finite and positive.");
		if (!validClassBounds(classLowerBounds))
			throw new IllegalArgumentException("Invalid income class bounds.");

		int incomeClass = 0;
		// Values below the MATSim minimum were clipped during activity sampling. Their original midpoint can no
		// longer be reconstructed from income * equivalenceScale and therefore remains in the lowest class.
		if (income > minimumIncome + 0.5) {
			double reconstructedHouseholdIncome = income * equivalentSize;
			for (int i = 1; i < classLowerBounds.length; i++) {
				if (reconstructedHouseholdIncome >= classLowerBounds[i])
					incomeClass = i;
				else
					break;
			}
		}

		double sourceLower = classLowerBounds[incomeClass] / equivalentSize;
		double sourceUpper = incomeClass + 1 < classLowerBounds.length
			? classLowerBounds[incomeClass + 1] / equivalentSize : Double.POSITIVE_INFINITY;
		boolean minimumRelaxed = Double.isFinite(sourceUpper) && minimumIncome >= sourceUpper;
		// In large households the regular minimum can lie above the upper bound of the lowest SrV class. Using the
		// mathematical lower bound (zero) in that case allows a boundary calibration to write zero incomes. For the
		// lowest class use its midpoint instead: this is the same representative-value convention used for the original
		// discrete SrV classes, remains strictly inside the class, and is safe for income-dependent scoring.
		double relaxedLower = sourceLower == 0 ? sourceUpper / 2 : sourceLower;
		double lower = minimumRelaxed ? relaxedLower : Math.max(minimumIncome, sourceLower);
		double upper = sourceUpper;

		return new IncomeBounds(sourceLower, sourceUpper, lower, upper, minimumRelaxed);
	}

	/**
	 * Maximum-likelihood fit for interval-censored observations under a log-normal distribution.
	 */
	static LogNormalFit fitLogNormal(List<IncomeEntry> entries) {
		Map<Interval, Long> grouped = new HashMap<>();
		for (IncomeEntry entry : entries) {
			IncomeBounds bounds = entry.bounds();
			grouped.merge(new Interval(bounds.sourceLower(), bounds.sourceUpper()), 1L, Long::sum);
		}

		List<WeightedInterval> intervals = grouped.entrySet().stream()
			.map(e -> new WeightedInterval(e.getKey().lower(), e.getKey().upper(), e.getValue()))
			.toList();

		double[] logs = entries.stream().mapToDouble(e -> Math.log(e.originalIncome())).toArray();
		double initialMu = Arrays.stream(logs).average().orElseThrow();
		double variance = Arrays.stream(logs)
			.map(v -> (v - initialMu) * (v - initialMu))
			.average()
			.orElse(0.25);
		double mu = initialMu;
		double logSigma = Math.log(Math.max(0.05, Math.sqrt(variance)));

		double stepMu = 0.25;
		double stepLogSigma = 0.20;
		double best = negativeLogLikelihood(mu, Math.exp(logSigma), intervals);

		for (int iteration = 0; iteration < 300 && Math.max(stepMu, stepLogSigma) > 1e-8; iteration++) {
			double bestMu = mu;
			double bestLogSigma = logSigma;
			double candidateBest = best;

			for (int m = -1; m <= 1; m++) {
				for (int s = -1; s <= 1; s++) {
					if (m == 0 && s == 0)
						continue;
					double candidateMu = mu + m * stepMu;
					double candidateLogSigma = logSigma + s * stepLogSigma;
					double candidateSigma = Math.exp(candidateLogSigma);
					double value = negativeLogLikelihood(candidateMu, candidateSigma, intervals);
					if (value < candidateBest) {
						candidateBest = value;
						bestMu = candidateMu;
						bestLogSigma = candidateLogSigma;
					}
				}
			}

			if (candidateBest < best) {
				mu = bestMu;
				logSigma = bestLogSigma;
				best = candidateBest;
			} else {
				stepMu *= 0.5;
				stepLogSigma *= 0.5;
			}
		}

		return new LogNormalFit(mu, Math.exp(logSigma));
	}

	static double negativeLogLikelihood(double mu, double sigma, List<WeightedInterval> intervals) {
		if (!Double.isFinite(mu) || !Double.isFinite(sigma) || sigma <= 0)
			return Double.POSITIVE_INFINITY;

		double result = 0;
		for (WeightedInterval interval : intervals) {
			double lower = interval.lower() <= 0 ? Double.NEGATIVE_INFINITY
				: (Math.log(interval.lower()) - mu) / sigma;
			double upper = Double.isInfinite(interval.upper()) ? Double.POSITIVE_INFINITY
				: (Math.log(interval.upper()) - mu) / sigma;
			double probability = normalProbability(lower, upper);
			if (!(probability > 0) || !Double.isFinite(probability))
				return Double.POSITIVE_INFINITY;
			result -= interval.count() * Math.log(probability);
		}
		return result;
	}

	static CalibrationResult calibrateMedian(List<IncomeEntry> entries, LogNormalFit fit, double requestedTarget) {
		List<BoundedDraw> draws = entries.stream()
			.map(e -> new BoundedDraw(e.bounds().lower(), e.bounds().upper(), e.quantile()))
			.toList();

		return calibrateMedianDraws(draws, fit, requestedTarget);
	}

	static CalibrationResult calibrateMedianDraws(List<BoundedDraw> draws, LogNormalFit fit, double requestedTarget) {
		if (draws.isEmpty())
			throw new IllegalArgumentException("At least one income draw is required.");
		if (!Double.isFinite(requestedTarget) || requestedTarget <= 0)
			throw new IllegalArgumentException("The requested median must be finite and positive.");

		double feasibleLower = medianIncludingInfinity(draws.stream().map(BoundedDraw::lower).toList());
		double feasibleUpper = medianIncludingInfinity(draws.stream().map(BoundedDraw::upper).toList());
		List<Double> baseValues = draws.stream()
			.map(draw -> drawTruncatedLogNormal(fit.mu(), fit.sigma(), draw))
			.toList();
		if (baseValues.stream().anyMatch(value -> !Double.isFinite(value)))
			throw new IllegalStateException("The Berlin-wide conditional income draw produced a non-finite value.");
		double appliedTarget = Math.max(requestedTarget, feasibleLower);
		if (Double.isFinite(feasibleUpper))
			appliedTarget = Math.min(appliedTarget, feasibleUpper);
		boolean clipped = Math.abs(appliedTarget - requestedTarget) > 1e-9;

		if (appliedTarget - feasibleLower <= MEDIAN_BOUNDARY_TOLERANCE_EUR) {
			List<Double> values = draws.stream().map(BoundedDraw::lower).toList();
			double boundaryMedian = median(values);
			return new CalibrationResult(values, requestedTarget, boundaryMedian, boundaryMedian,
				Double.NEGATIVE_INFINITY, clipped || Math.abs(boundaryMedian - requestedTarget) > 1e-9);
		}
		if (Double.isFinite(feasibleUpper)
			&& feasibleUpper - appliedTarget <= MEDIAN_BOUNDARY_TOLERANCE_EUR) {
			List<Double> values = upperBoundaryValues(draws, baseValues, feasibleUpper);
			double boundaryMedian = median(values);
			return new CalibrationResult(values, requestedTarget, boundaryMedian, boundaryMedian,
				Double.POSITIVE_INFINITY, clipped || Math.abs(boundaryMedian - requestedTarget) > 1e-9);
		}

		double lowShift = -16;
		double highShift = 16;
		double lowMedian = medianAtShift(draws, baseValues, lowShift);
		double highMedian = medianAtShift(draws, baseValues, highShift);
		if (lowMedian > appliedTarget) {
			List<Double> values = draws.stream().map(BoundedDraw::lower).toList();
			double boundaryMedian = median(values);
			return new CalibrationResult(values, requestedTarget, boundaryMedian, boundaryMedian,
				Double.NEGATIVE_INFINITY, true);
		}
		if (highMedian < appliedTarget && Double.isFinite(feasibleUpper)) {
			List<Double> values = upperBoundaryValues(draws, baseValues, feasibleUpper);
			double boundaryMedian = median(values);
			return new CalibrationResult(values, requestedTarget, boundaryMedian, boundaryMedian,
				Double.POSITIVE_INFINITY, true);
		}
		if (highMedian < appliedTarget)
			throw new IllegalStateException("Could not bracket LOR median " + appliedTarget
				+ " although its upper class boundary is open.");

		for (int i = 0; i < 60; i++) {
			double middle = (lowShift + highShift) / 2;
			if (medianAtShift(draws, baseValues, middle) < appliedTarget)
				lowShift = middle;
			else
				highShift = middle;
		}

		double shift = (lowShift + highShift) / 2;
		List<Double> values = valuesAtShift(draws, baseValues, shift);
		return new CalibrationResult(values, requestedTarget, appliedTarget, median(values), shift, clipped);
	}

	private static double medianAtShift(List<BoundedDraw> draws, List<Double> baseValues, double shift) {
		double[] values = new double[draws.size()];
		for (int i = 0; i < draws.size(); i++)
			values[i] = shiftAndClamp(draws.get(i), baseValues.get(i), shift);
		return medianInPlace(values);
	}

	private static List<Double> valuesAtShift(List<BoundedDraw> draws, List<Double> baseValues, double shift) {
		List<Double> values = new ArrayList<>(draws.size());
		for (int i = 0; i < draws.size(); i++)
			values.add(shiftAndClamp(draws.get(i), baseValues.get(i), shift));
		return values;
	}

	private static double shiftAndClamp(BoundedDraw draw, double baseValue, double shift) {
		if (draw.fixed())
			return draw.lower();

		double logValue = Math.log(baseValue) + shift;
		double value = logValue >= Math.log(Double.MAX_VALUE) ? Double.MAX_VALUE : Math.exp(logValue);
		value = Math.max(draw.lower(), value);
		if (Double.isFinite(draw.upper()))
			value = Math.min(Math.nextDown(draw.upper()), value);
		return value;
	}

	private static List<Double> upperBoundaryValues(List<BoundedDraw> draws, List<Double> baseValues,
												   double boundaryMedian) {
		List<Double> values = new ArrayList<>(draws.size());
		for (int i = 0; i < draws.size(); i++) {
			BoundedDraw draw = draws.get(i);
			if (draw.fixed())
				values.add(draw.lower());
			else if (Double.isFinite(draw.upper()))
				values.add(Math.nextDown(draw.upper()));
			else
				// Open top classes must remain finite. A value at least as large as the finite median boundary preserves
				// the ordering needed for the boundary median without inventing an artificial maximum income.
				values.add(Math.max(baseValues.get(i), boundaryMedian));
		}
		return values;
	}

	private static double medianInPlace(double[] values) {
		int middle = values.length / 2;
		double upper = select(values, middle);
		if (values.length % 2 == 1)
			return upper;
		return (select(values, middle - 1) + upper) / 2;
	}

	private static double select(double[] values, int rank) {
		int left = 0;
		int right = values.length - 1;
		while (left < right) {
			double pivot = values[(left + right) >>> 1];
			int lower = left;
			int upper = right;
			while (lower <= upper) {
				while (lower <= right && values[lower] < pivot)
					lower++;
				while (upper >= left && values[upper] > pivot)
					upper--;
				if (lower <= upper) {
					double value = values[lower];
					values[lower++] = values[upper];
					values[upper--] = value;
				}
			}

			if (rank <= upper)
				right = upper;
			else if (rank >= lower)
				left = lower;
			else
				return values[rank];
		}
		return values[left];
	}

	static double drawTruncatedLogNormal(double mu, double sigma, BoundedDraw draw) {
		if (draw.fixed())
			return draw.lower();

		double lower = (Math.log(draw.lower()) - mu) / sigma;
		double upper = Double.isInfinite(draw.upper()) ? Double.POSITIVE_INFINITY
			: (Math.log(draw.upper()) - mu) / sigma;
		double z;
		if (lower > 0) {
			// Work with survival probabilities in the upper tail. CDF differences would round to zero there.
			double lowerSurvival = standardNormalSurvival(lower);
			double upperSurvival = standardNormalSurvival(upper);
			double intervalSurvival = lowerSurvival - upperSurvival;
			if (!(intervalSurvival > 0))
				return draw.lower();
			double survival = lowerSurvival - draw.quantile() * intervalSurvival;
			z = standardNormalSurvivalQuantile(survival);
		} else {
			double lowerProbability = standardNormalCdf(lower);
			double upperProbability = standardNormalCdf(upper);
			double intervalProbability = upperProbability - lowerProbability;
			if (!(intervalProbability > 0))
				return Double.isFinite(draw.upper()) ? Math.nextDown(draw.upper()) : draw.lower();
			double probability = lowerProbability + draw.quantile() * intervalProbability;
			z = standardNormalQuantile(probability);
		}

		double value = Math.exp(mu + sigma * z);
		value = Math.max(draw.lower(), value);
		if (Double.isFinite(draw.upper()))
			value = Math.min(Math.nextDown(draw.upper()), value);
		return value;
	}

	private static double standardNormalCdf(double value) {
		return 0.5 * Erf.erfc(-value / Math.sqrt(2));
	}

	private static double standardNormalSurvival(double value) {
		return 0.5 * Erf.erfc(value / Math.sqrt(2));
	}

	private static double normalProbability(double lower, double upper) {
		if (lower > 0)
			return standardNormalSurvival(lower) - standardNormalSurvival(upper);
		return standardNormalCdf(upper) - standardNormalCdf(lower);
	}

	private static double standardNormalQuantile(double probability) {
		double bounded = Math.max(Double.MIN_NORMAL, Math.min(Math.nextDown(1.0), probability));
		return bounded < 0.5
			? -Math.sqrt(2) * Erf.erfcInv(2 * bounded)
			: Math.sqrt(2) * Erf.erfcInv(2 * (1 - bounded));
	}

	private static double standardNormalSurvivalQuantile(double probability) {
		double bounded = Math.max(Double.MIN_NORMAL, Math.min(Math.nextDown(1.0), probability));
		return Math.sqrt(2) * Erf.erfcInv(2 * bounded);
	}

	private static double medianIncludingInfinity(List<Double> values) {
		List<Double> sorted = values.stream()
			.filter(v -> !Double.isNaN(v))
			.sorted(Comparator.naturalOrder())
			.toList();
		if (sorted.isEmpty())
			return Double.NaN;

		int middle = sorted.size() / 2;
		return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle);
	}

	static double median(List<Double> values) {
		if (values.isEmpty())
			return Double.NaN;

		List<Double> sorted = values.stream()
			.filter(Double::isFinite)
			.sorted(Comparator.naturalOrder())
			.toList();
		if (sorted.isEmpty())
			return Double.NaN;

		int middle = sorted.size() / 2;
		return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle);
	}

	static double mean(List<Double> values) {
		return values.stream().filter(Double::isFinite).mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
	}

	private static double uniform(String personId, long seed) {
		long personSeed = new BigInteger(personId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).longValue();
		SplittableRandom random = new SplittableRandom(personSeed ^ seed);
		return Math.max(1e-12, Math.min(1 - 1e-12, random.nextDouble()));
	}

	private record Header(int row, int lorColumn, int medianColumn) {
	}

	record IncomeBounds(double sourceLower, double sourceUpper, double lower, double upper, boolean minimumRelaxed) {
	}

	record LogNormalFit(double mu, double sigma) {
	}

	record BoundedDraw(double lower, double upper, double quantile) {
		boolean fixed() {
			return lower == upper;
		}
	}

	record CalibrationResult(List<Double> incomes, double requestedTarget, double appliedTarget,
							 double achievedMedian, double locationShift, boolean clipped) {
	}

	record WeightedInterval(double lower, double upper, long count) {
	}

	private record Interval(double lower, double upper) {
	}

	private record IncomeEntry(Person person, double originalIncome, IncomeBounds bounds, double quantile,
							   double spatialMedian) {
	}

	private record ProposedIncome(Person person, double income) {
	}
}
