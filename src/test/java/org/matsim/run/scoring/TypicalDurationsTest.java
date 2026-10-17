package org.matsim.run.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypicalDurationsTest {

	@Test
	void roundTrip() {
		List<TypicalDurations.Entry> entries = List.of(
			new TypicalDurations.Entry("home_morning", 24944),
			new TypicalDurations.Entry("work", 29550),
			new TypicalDurations.Entry("work", 1800.5),
			new TypicalDurations.Entry("home_evening", 41206));

		String encoded = TypicalDurations.format(entries);

		// integral seconds without a fraction, repeated types kept in order
		assertThat(encoded).isEqualTo("home_morning=24944;work=29550;work=1800.5;home_evening=41206");
		assertThat(TypicalDurations.parse(encoded)).isEqualTo(entries);
	}

	@Test
	void onThePerson() {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		assertThat(TypicalDurations.get(person)).isNull();

		List<TypicalDurations.Entry> entries = List.of(new TypicalDurations.Entry("home_morning", 25200), new TypicalDurations.Entry("home_evening", 37800));
		TypicalDurations.set(person, entries);

		assertThat(person.getAttributes().getAttribute(TypicalDurations.ATTRIBUTE)).isEqualTo("home_morning=25200;home_evening=37800");
		assertThat(TypicalDurations.get(person)).isEqualTo(entries);

		TypicalDurations.set(person, null);
		assertThat(TypicalDurations.get(person)).isNull();
	}

	@Test
	void typesMustNotContainTheSeparators() {
		assertThatThrownBy(() -> new TypicalDurations.Entry("a;b", 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TypicalDurations.Entry("a=b", 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TypicalDurations.parse("work")).isInstanceOf(IllegalArgumentException.class);
	}
}
