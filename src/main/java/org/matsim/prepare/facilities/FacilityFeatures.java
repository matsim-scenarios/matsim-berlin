package org.matsim.prepare.facilities;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.matsim.application.prepare.Predictor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The features of a facility as the attraction models see them.
 * <p>
 * The models are generated code that reads its features from a map by name, and a fastutil map answers a name it does
 * not contain with 0. So a feature that does not make it into the map, e.g. because the datastore hands it over as a
 * type that is not converted here, silently turns into a 0 and the model keeps predicting. This class closes that gap
 * from both sides: the features a model needs are determined from the model itself, the schema of the facilities file
 * is checked against them before anything is predicted, and the map handed to the models throws on a name it does not
 * contain.
 */
public final class FacilityFeatures {

	private FacilityFeatures() {
	}

	/**
	 * The names of the features the given models read.
	 */
	public static Set<String> requiredFeatures(Predictor... models) {
		RecordingMap recorder = new RecordingMap();
		for (Predictor model : models) {
			model.getData(recorder, null);
		}
		return recorder.requested;
	}

	/**
	 * Check that the schema provides every name in {@code required} as a numeric or boolean attribute.
	 *
	 * @throws IllegalArgumentException listing every missing or unconvertible attribute
	 */
	public static void checkSchema(SimpleFeatureType schema, Collection<String> required, String neededBy) {
		List<String> problems = new ArrayList<>();
		for (String name : required) {
			AttributeDescriptor descriptor = schema.getDescriptor(name);
			if (descriptor == null) {
				problems.add(name + " (missing)");
			} else if (!isConvertible(descriptor.getType().getBinding())) {
				problems.add(name + " (" + descriptor.getType().getBinding().getSimpleName() + ")");
			}
		}
		if (!problems.isEmpty()) {
			throw new IllegalArgumentException("The facilities file does not provide the attributes needed by " + neededBy +
				" as numbers or booleans: " + String.join(", ", problems));
		}
	}

	/**
	 * The features of {@code ft}, as a map that throws on a name it does not contain.
	 *
	 * @throws IllegalArgumentException if a required attribute is missing or of a type that is not converted
	 */
	public static Object2DoubleMap<String> features(SimpleFeature ft, Collection<String> required) {
		StrictMap features = new StrictMap(required.size());
		for (String name : required) {
			features.put(name, toDouble(ft.getAttribute(name), name));
		}
		return features;
	}

	/**
	 * Convert an attribute value to a feature value: numbers as they are, booleans as 1 and 0.
	 */
	static double toDouble(Object value, String name) {
		if (value instanceof Number number)
			return number.doubleValue();
		if (value instanceof Boolean bool)
			return bool ? 1 : 0;
		if (value == null)
			throw new IllegalArgumentException("Attribute " + name + " is missing or null");
		throw new IllegalArgumentException("Attribute " + name + " is a " + value.getClass().getSimpleName() + ", not a number or boolean");
	}

	private static boolean isConvertible(Class<?> binding) {
		return Number.class.isAssignableFrom(binding) || Boolean.class.equals(binding)
			|| binding.isPrimitive() && binding != char.class && binding != void.class;
	}

	/**
	 * Records the names the models ask for.
	 */
	private static final class RecordingMap extends Object2DoubleOpenHashMap<String> {
		private final Set<String> requested = new LinkedHashSet<>();

		@Override
		public double getDouble(Object key) {
			requested.add((String) key);
			return 0;
		}
	}

	/**
	 * Throws instead of answering an unknown name with the default value.
	 */
	private static final class StrictMap extends Object2DoubleOpenHashMap<String> {
		StrictMap(int expected) {
			super(expected);
		}

		@Override
		public double getDouble(Object key) {
			if (!containsKey(key))
				throw new NoSuchElementException("No feature " + key + "; the facilities file does not provide it");
			return super.getDouble(key);
		}
	}
}
