package org.matsim.run.deparking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.run.deparking.DeparkingConfigGroup.PenaltyEventType;

class DeparkingConfigGroupTest {
	@TempDir
	Path tempDir;

	@Test
	void penaltyEventType_defaultsToMoney() {
		DeparkingConfigGroup configGroup = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DeparkingConfigGroup.class);

		assertEquals(PenaltyEventType.MONEY, configGroup.getPenaltyEventType());
	}

	@Test
	void legacyConfigWithoutPenaltyEventType_usesMoney() throws IOException {
		Path configFile = tempDir.resolve("legacy-config.xml");
		Files.writeString(configFile, """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd">
			<config>
				<module name="deparking">
					<param name="maxCost" value="4.0" />
				</module>
			</config>
			""");

		Config config = ConfigUtils.loadConfig(configFile.toString(), new DeparkingConfigGroup());
		DeparkingConfigGroup configGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);

		assertEquals(PenaltyEventType.MONEY, configGroup.getPenaltyEventType());
		assertEquals(4.0, configGroup.getMaxCost());
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void penaltyEventType_survivesXmlRoundTrip(PenaltyEventType eventType) {
		Config config = ConfigUtils.createConfig(new DeparkingConfigGroup());
		ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class).setPenaltyEventType(eventType);
		Path configFile = tempDir.resolve("config.xml");
		new ConfigWriter(config).write(configFile.toString());

		Config reloaded = ConfigUtils.loadConfig(configFile.toString(), new DeparkingConfigGroup());

		assertEquals(eventType, ConfigUtils.addOrGetModule(reloaded, DeparkingConfigGroup.class).getPenaltyEventType());
	}
}
