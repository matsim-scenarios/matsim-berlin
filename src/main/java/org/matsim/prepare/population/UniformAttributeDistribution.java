package org.matsim.prepare.population;

import java.util.*;

/**
 * Distribution where each attribute is equally likely.
 */
public class UniformAttributeDistribution<T> implements AttributeDistribution<T> {

	private final List<T> attributes;
	private final SplittableRandom rnd;

	public UniformAttributeDistribution(Collection<T> attributes, long seed) {
		this.attributes = new ArrayList<>(attributes);
		this.rnd = new SplittableRandom(seed);
	}

	@Override
	public T sample() {
		return attributes.get(rnd.nextInt(attributes.size()));
	}

}
