package org.matsim.prepare.counts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.prepare.counts.CreateCountsFromMonthlyVizData.nameTokens;

/**
 * Documents how road names of the VIZ stations and of the network links are reduced to a comparable form before they
 * are matched against each other.
 */
public class CreateCountsFromMonthlyVizDataTest {

	@Test
	public void unnamed() {

		//An empty set is the "no name given" signal: nameScore neither rewards nor punishes a candidate for it,
		//which matters for motorways, because they carry no name in the network
		assertThat(nameTokens(null)).isEmpty();
		assertThat(nameTokens("")).isEmpty();
		assertThat(nameTokens("   ")).isEmpty();

		//A name that is nothing but the suffix says nothing about which street it is
		assertThat(nameTokens("Straße")).isEmpty();
	}

	@Test
	public void caseAndDiacriticsAreRemoved() {

		//Comparison is case insensitive
		assertThat(nameTokens("HAUPTWEG")).containsExactly("hauptweg");

		//Umlauts are folded onto their base letter
		assertThat(nameTokens("Späth")).containsExactly("spath");
		assertThat(nameTokens("Köpenicker")).containsExactly("kopenicker");

		//The transliteration is not undone, though: unlike normalizeDirection, which maps "ue" onto "u", this method
		//leaves "ae"/"oe"/"ue" alone. A station spelling a name without the umlaut therefore does not match the link
		assertThat(nameTokens("Spaeth")).containsExactly("spaeth");
		assertThat(nameTokens("Spaeth")).isNotEqualTo(nameTokens("Späth"));

		//'ß' has no decomposition and is replaced by hand, which is why the suffix list spells it "strasse"
		assertThat(nameTokens("Gießener")).containsExactly("giessener");
	}

	@Test
	public void separatorsSplitTokens() {

		//Spaces, hyphens and the dot of an abbreviation all separate tokens, and a leading separator does not
		//produce an empty one
		assertThat(nameTokens("Otto-Braun")).containsExactlyInAnyOrder("otto", "braun");
		assertThat(nameTokens(" Am Treptower Park")).containsExactlyInAnyOrder("am", "treptower", "park");
		assertThat(nameTokens("B 96a")).containsExactlyInAnyOrder("b", "96a");
	}

	@Test
	public void streetSuffixIsCollapsed() {

		//The reason the method exists: the suffix appears standalone, concatenated and abbreviated, and all three
		//spellings have to reduce to the same token set
		assertThat(nameTokens("Ollenhauer Straße")).containsExactly("ollenhauer");
		assertThat(nameTokens("Ollenhauerstraße")).containsExactly("ollenhauer");
		assertThat(nameTokens("Ollenhauerstr.")).containsExactly("ollenhauer");
		assertThat(nameTokens("Ollenhauer Str")).containsExactly("ollenhauer");

		assertThat(nameTokens("Ollenhauer Straße")).isEqualTo(nameTokens("Ollenhauerstr."));

		//Only the trailing suffix is cut, the rest of a compound name stays
		assertThat(nameTokens("Alt-Moabit Straße")).containsExactlyInAnyOrder("alt", "moabit");
		assertThat(nameTokens("Sträßchenweg")).containsExactly("strasschenweg");
	}

	@Test
	public void otherSuffixesAreKept() {

		//Only "straße" is collapsed, because only it is written both ways in the two data sources. "Allee", "Damm"
		//and "Weg" are part of the name and distinguish streets that otherwise share it
		assertThat(nameTokens("Puschkinallee")).containsExactly("puschkinallee");
		assertThat(nameTokens("Puschkin Allee")).containsExactlyInAnyOrder("puschkin", "allee");
	}

	@Test
	public void orderDoesNotMatterButQualifiersDo() {

		//nameScore compares the two sets with equals, so word order is irrelevant ...
		assertThat(nameTokens("Am Treptower Park")).isEqualTo(nameTokens("Park Treptower Am"));

		//... but the set has to match exactly. A qualifier usually denotes a different street in Berlin, so
		//"Neue Späthstraße" must not be accepted as "Späthstraße"
		assertThat(nameTokens("Neue Späthstraße")).isNotEqualTo(nameTokens("Späthstraße"));
		assertThat(nameTokens("Neue Späthstraße")).containsExactlyInAnyOrder("neue", "spath");
	}
}
