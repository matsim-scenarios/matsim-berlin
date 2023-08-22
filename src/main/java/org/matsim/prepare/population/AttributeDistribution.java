package org.matsim.prepare.population;

/**
 * Distribution of attribute values.
 * @param <T> attribute type
 */
public interface AttributeDistribution<T> {


	/**
	 * Draw a random sample from the distribution.
	 */
	T sample();

}
