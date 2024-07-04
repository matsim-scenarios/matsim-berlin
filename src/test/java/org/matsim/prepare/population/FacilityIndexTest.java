package org.matsim.prepare.population;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.prepare.facilities.AttributedActivityFacility;

import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

class FacilityIndexTest {

	private SplittableRandom rnd = new SplittableRandom(0);
	private ActivityFacilitiesFactory factory = new ActivityFacilitiesFactoryImpl();
	private AttributedActivityFacility createFacility(double weight) {
		ActivityFacility f = factory.createActivityFacility(Id.create(rnd.nextLong(), ActivityFacility.class), Id.createLinkId("link"));
		f.getAttributes().putAttribute(Attributes.ATTRACTION_WORK, weight);
		f.getAttributes().putAttribute(Attributes.ATTRACTION_OTHER, weight);
		f.getAttributes().putAttribute("accept", true);
		return new AttributedActivityFacility(f);
	}

	private AttributedActivityFacility createFacility(double weight, boolean reject) {
		ActivityFacility f = factory.createActivityFacility(Id.create(rnd.nextLong(), ActivityFacility.class), Id.createLinkId("link"));
		f.getAttributes().putAttribute(Attributes.ATTRACTION_WORK, weight);
		f.getAttributes().putAttribute(Attributes.ATTRACTION_OTHER, weight);
		f.getAttributes().putAttribute("accept", !reject);
		return new AttributedActivityFacility(f);
	}

	/**
	 * Sample and return relative occurrence of the first alternative.
	 */
	private double sample(AttributedActivityFacility... facilities) {

		List<AttributedActivityFacility> list = Arrays.stream(facilities).toList();
		int count = 0;
		for (int i = 0; i < 10_000; i++) {
			int idx = FacilityIndex.sampleByWeight(list, AttributedActivityFacility::getWorkAttraction, rnd);
			if (list.get(idx) == list.get(0))
				count++;
		}

		return count / 10000.0;
	}

	/**
	 * Sample and return relative occurrence of the first alternative.
	 */
	private double sampleRejection(AttributedActivityFacility... facilities) {

		List<AttributedActivityFacility> list = Arrays.stream(facilities).toList();
		int count = 0;
		for (int i = 0; i < 10_000; i++) {
			ActivityFacility a = FacilityIndex.sampleByWeightWithRejection(list, (af) -> (boolean) af.getAttributes().getAttribute("accept"),
				AttributedActivityFacility::getWorkAttraction, rnd);
			if (a == list.get(0))
				count++;
		}

		return count / 10000.0;
	}


	@Test
	void sampling() {

		assertThat(sample(createFacility(2)))
			.isCloseTo(1, Offset.offset(0.0001));

		assertThat(sample(createFacility(2), createFacility(2)))
			.isCloseTo(0.5, Offset.offset(0.01));

		assertThat(sample(createFacility(100), createFacility(50), createFacility(50)))
			.isCloseTo(0.5, Offset.offset(0.01));


		assertThat(sample(createFacility(1), createFacility(200), createFacility(300), createFacility(500)))
			.isCloseTo(0.001, Offset.offset(0.01));

	}

	@Test
	void rejectionSampling() {

		assertThat(sampleRejection(createFacility(2, false)))
			.isCloseTo(1, Offset.offset(0.0001));

		assertThat(sampleRejection(createFacility(2, true)))
			.isCloseTo(0, Offset.offset(0.0001));

		assertThat(sampleRejection(createFacility(50), createFacility(100, true), createFacility(50)))
			.isCloseTo(0.5, Offset.offset(0.01));

		assertThat(sampleRejection(createFacility(1), createFacility(200), createFacility(300), createFacility(500)))
			.isCloseTo(0.001, Offset.offset(0.01));

		assertThat(sampleRejection(createFacility(200), createFacility(1, true), createFacility(300), createFacility(500)))
			.isCloseTo(0.2, Offset.offset(0.01));

		assertThat(sampleRejection(createFacility(200), createFacility(1, true), createFacility(300,true), createFacility(500)))
			.isCloseTo(2.0/7.0, Offset.offset(0.01));

	}
}
