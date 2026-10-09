package org.matsim.prepare.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stages in {@link FilterOsmWays} and {@link MergeOsm} are only as good as the osmosis modules on the
 * classpath, and a missing or wrongly scoped one shows up as a task name that cannot be resolved. These
 * tests check exactly that, which needs no OSM input data.
 */
class OsmosisPipelineTest {

	@ParameterizedTest
	@ValueSource(strings = {"read-pbf", "write-pbf", "read-xml", "write-xml", "tag-filter", "used-node",
		"bounding-polygon", "merge", "tag-transform"})
	void taskIsAvailable(String task) {
		assertThat(OsmosisPipeline.registrar().getFactoryRegister().getInstance(task))
			.as("osmosis task %s has to be on the classpath", task)
			.isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"rb", "wb", "rx", "wx", "tf", "un", "bp", "m"})
	void shortFormIsAvailable(String task) {
		// the stages use the short forms, so they have to come from the same modules
		assertThat(OsmosisPipeline.registrar().getFactoryRegister().getInstance(task))
			.as("osmosis task %s has to be on the classpath", task)
			.isNotNull();
	}

	@Test
	void formatIsPickedFromTheExtension() {
		assertThat(OsmosisPipeline.readerFor(Path.of("network.osm.pbf"))).isEqualTo("--rb");
		assertThat(OsmosisPipeline.writerFor(Path.of("network.osm.pbf"))).isEqualTo("--wb");
		assertThat(OsmosisPipeline.readerFor(Path.of("network.osm"))).isEqualTo("--rx");
		assertThat(OsmosisPipeline.writerFor(Path.of("network.osm"))).isEqualTo("--wx");
		assertThat(OsmosisPipeline.writerFor(Path.of("some/dir/network.osm.gz"))).isEqualTo("--wx");
	}
}
