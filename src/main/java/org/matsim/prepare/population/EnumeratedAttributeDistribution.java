package org.matsim.prepare.population;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Distribution with fixed probabilities for each entry.
 *
 * @param <T> type of the produced value.
 */
public class EnumeratedAttributeDistribution<T> implements AttributeDistribution<T> {

	private final EnumeratedDistribution<T> dist;

	/**
	 * Constructor.
	 *
	 * @param probabilities map of attributes to their probabilities.
	 */
	public EnumeratedAttributeDistribution(Map<T, Double> probabilities, long seed) {
		List<Pair<T, Double>> pairs = probabilities.entrySet().stream().map(
				e -> new Pair<>(e.getKey(), e.getValue())
		).collect(Collectors.toList());

		dist = new EnumeratedDistribution<>(new Well19937c(seed), pairs);
	}

	@Override
	public T sample() {
		return dist.sample();
	}
}
