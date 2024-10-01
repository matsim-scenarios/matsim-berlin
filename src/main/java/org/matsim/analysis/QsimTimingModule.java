package org.matsim.analysis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class QsimTimingModule extends AbstractModule {

	@Override
	public void install() {
		// bind the timer, so that it can be injected into CheckForLastIteration
		// use singleton scope, because, we want the same timer everywhere.
		bind(Timer.class).in(Singleton.class);
		addMobsimListenerBinding().to(Timer.class);
		addControlerListenerBinding().to(CheckForLastIteration.class);
	}

	private static class CheckForLastIteration implements BeforeMobsimListener {

		private final Timer timer;
		private final Config config;

		@Inject
		private CheckForLastIteration(Config config, Timer timer) {
			this.timer = timer;
			this.config = config;
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent e) {
			// using e.getIsLastIteration, does not yield the correct result somehow
			this.timer.setIsLastIteration(config.controller().getLastIteration() == e.getIteration());
		}
	}

	private static class Timer implements MobsimInitializedListener, MobsimBeforeCleanupListener {
		private Instant start;
		private boolean isLastIteration;

		private final Config config;
		private final OutputDirectoryHierarchy outDir;

		@Inject
		private Timer(Config config, OutputDirectoryHierarchy outDir) {
			this.config = config;
			this.outDir = outDir;
		}

		void setIsLastIteration(boolean isLastIteration) {
			this.isLastIteration = isLastIteration;
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			if (isLastIteration) {
				start = Instant.now();
			}
		}

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			if (isLastIteration) {
				var now = Instant.now();
				var duration = Duration.between(start, now);
				var size = config.qsim().getNumberOfThreads();
				var filename = Paths.get(outDir.getOutputFilename("runtimes.csv"));
				try (var writer = Files.newBufferedWriter(filename); var p = new CSVPrinter(writer, createWriteFormat("size", "rank", "runtime", "rtr"))) {
					var rtr = config.qsim().getEndTime().seconds() / duration.toSeconds();
					p.printRecord(size, 0, duration.toMillis(), rtr);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		private static CSVFormat createWriteFormat(String... header) {
			return CSVFormat.DEFAULT.builder()
				.setHeader(header)
				.setSkipHeaderRecord(false)
				.build();
		}
	}
}
