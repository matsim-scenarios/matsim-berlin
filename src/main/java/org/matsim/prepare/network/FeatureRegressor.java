package org.matsim.prepare.network;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * Predictor interface for regression.
 */
public interface FeatureRegressor {

	double predict(Object2DoubleMap<String> ft);

}
