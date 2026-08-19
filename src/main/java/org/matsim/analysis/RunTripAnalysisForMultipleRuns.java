package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "trips-multiple-runs", description = "Run TripAnalysis class for looping over different run directories.")
public class RunTripAnalysisForMultipleRuns implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(RunTripAnalysisForMultipleRuns.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which air pollution analysis should be run.")
	private List<Path> inputPaths;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--groups-ref-data", description = "Csv file with ref data per group.", required = true)
	private String groupRefDataPath;

	public static void main(String[] args) {
		new RunTripAnalysisForMultipleRuns().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			String tripsPath = ApplicationUtils.matchInput("output_trips.csv.gz", runDirectory).toString();
			String personsPath = ApplicationUtils.matchInput("output_persons.csv.gz", runDirectory).toString();

			Path outputDir = runDirectory.resolve("analysis").resolve("population");

			new TripAnalysisNoModeShift().execute(
				"--match-id", "^berlin.+",
				"--shp-filter", "none",
				"--input-ref-data", groupRefDataPath,
				"--input-trips", tripsPath,
				"--input-persons", personsPath,
				"--shp", shp.getShapeFile(),
				"--output-mode-share", outputDir.resolve("mode_share.csv").toString(),
				"--output-mode-share-per-dist", outputDir.resolve("mode_share_per_dist.csv").toString(),
				"--output-mode-users", outputDir.resolve("mode_users.csv").toString(),
				"--output-trip-stats", outputDir.resolve("trip_stats.csv").toString(),
				"--output-mode-share-per-purpose", outputDir.resolve("mode_share_per_purpose.csv").toString(),
				"--output-mode-share-per-%s", outputDir.resolve("mode_share_per_%s.csv").toString(),
				"--output-population-trip-stats", outputDir.resolve("population_trip_stats.csv").toString(),
				"--output-trip-purposes-by-hour", outputDir.resolve("trip_purposes_by_hour.csv").toString(),
				"--output-mode-share-distance-distribution", outputDir.resolve("mode_share_distance_distribution.csv").toString(),
//				"--output-mode-shift", outputDir.resolve("mode_shift.csv").toString(),
				"--output-mode-chains", outputDir.resolve("mode_chains.csv").toString(),
				"--output-mode-choices", outputDir.resolve("mode_choices.csv").toString(),
				"--output-mode-choice-evaluation", outputDir.resolve("mode_choice_evaluation.csv").toString(),
				"--output-mode-choice-evaluation-per-mode", outputDir.resolve("mode_choice_evaluation_per_mode.csv").toString(),
				"--output-mode-confusion-matrix", outputDir.resolve("mode_confusion_matrix.csv").toString(),
				"--output-mode-prediction-error", outputDir.resolve("mode_prediction_error.csv").toString()
				);
		}






		return 0;
	}
}
