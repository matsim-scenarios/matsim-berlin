package org.matsim.prepare.population;

import java.util.*;

/**
 * Distribution where each attribute is equally likely.
 */
public class UniformAttributeDistribution<T> implements AttributeDistribution<T> {

	private final List<T> attributes;
	private final Random rnd;

	public UniformAttributeDistribution(T... attributes) {
		this(Arrays.stream(attributes).toList());
	}

	public UniformAttributeDistribution(Collection<T> attributes) {
		this.attributes = new ArrayList<>(attributes);
		this.rnd = new Random(0);
	}

	@Override
	public T sample() {
		return attributes.get(rnd.nextInt(attributes.size()));
	}

}
