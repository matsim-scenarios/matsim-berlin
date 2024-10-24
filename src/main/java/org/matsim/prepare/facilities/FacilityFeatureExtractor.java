package org.matsim.prepare.facilities;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.strtree.STRtree;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Predicate;

/**
 * Class to extract more features / columns for the facility shape file.
 */
final class FacilityFeatureExtractor {

	final STRtree entities;
	final STRtree pois;
	final STRtree landuse;

	final SimpleFeatureType featureType;

	final ThreadLocal<SimpleFeatureBuilder> featureBuilder;

	private final Object2IntMap<String> types;

	FacilityFeatureExtractor(String crs, Object2IntMap<String> types,
									Long2ObjectMap<Feature> entities, Long2ObjectMap<Feature> pois,
									Long2ObjectMap<Feature> landuse) throws FactoryException {

		this.entities = createIndex(entities);
		this.pois = createIndex(pois);
		this.landuse = createIndex(landuse);
		this.types = types;

		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("facilities");
		typeBuilder.setCRS(CRS.decode(crs));
		typeBuilder.add("osm_id", Long.class);
		typeBuilder.add("osm_type", String.class);
		typeBuilder.add("the_geom", MultiPolygon.class);
		typeBuilder.add("area", Double.class);
		typeBuilder.add("levels", Integer.class);
		typeBuilder.add("landuse", Boolean.class);
		typeBuilder.add("building", Boolean.class);
		typeBuilder.add("residential_only", Boolean.class);
		typeBuilder.add("landuse_residential_500m", Double.class);
		typeBuilder.add("landuse_residential_1500m", Double.class);
		typeBuilder.add("landuse_retail_500m", Double.class);
		typeBuilder.add("landuse_retail_1500m", Double.class);
		typeBuilder.add("landuse_commercial_500m", Double.class);
		typeBuilder.add("landuse_commercial_1500m", Double.class);
		typeBuilder.add("landuse_recreation_1500m", Double.class);
		typeBuilder.add("parking_space_500m", Double.class);
		typeBuilder.add("nearest_bus_stop", Double.class);
		typeBuilder.add("nearest_train_station", Double.class);

		typeBuilder.add("poi_leisure", Integer.class);
		typeBuilder.add("poi_leisure_250m", Integer.class);
		typeBuilder.add("poi_shop", Integer.class);
		typeBuilder.add("poi_shop_250m", Integer.class);
		typeBuilder.add("poi_dining", Integer.class);
		typeBuilder.add("poi_dining_250m", Integer.class);

		for (String t : types.keySet()) {
			typeBuilder.add(t, Boolean.class);
		}

		this.featureType = typeBuilder.buildFeatureType();

		this.featureBuilder = ThreadLocal.withInitial(() -> new SimpleFeatureBuilder(featureType));
	}

	private static STRtree createIndex(Long2ObjectMap<Feature> entities) {
		STRtree index = new STRtree();
		for (Feature entity : entities.values()) {
			index.insert(entity.geometry.getBoundary().getEnvelopeInternal(), entity);
		}
		index.build();
		return index;
	}

	/**
	 * Create features for one facility.
	 */
	public SimpleFeature createFeature(Feature ft) {

		// feature building is thread safe
		SimpleFeatureBuilder b = featureBuilder.get();

		b.add(ft.entity.getId());
		b.add(ft.osmType.toString());
		b.add(ft.geometry);
		b.add(BigDecimal.valueOf(ft.geometry.getArea()).setScale(2, RoundingMode.HALF_EVEN).doubleValue());
		b.add(ft.getLevels());
		b.add(ft.hasLanduse(null));
		b.add(ft.isBuilding);
		b.add(ft.isResidentialOnly());

		b.add(calcLanduse("residential", ft, 500));
		b.add(calcLanduse("residential", ft, 1500));
		b.add(calcLanduse("retail", ft, 500));
		b.add(calcLanduse("retail", ft, 1500));
		b.add(calcLanduse("commercial", ft, 500));
		b.add(calcLanduse("commercial", ft, 1500));
		b.add(calcLanduse("recreation_ground", ft, 1500));
		b.add(calcArea("parking", ft, 500));
		b.add(findNearest(ft, f -> f.isBusStop));
		b.add(findNearest(ft, f -> f.isTrainStop));

		b.add(countPOIs("leisure", ft));
		b.add(countPOIs("leisure", ft, 250));
		b.add(countPOIs("shop", ft));
		b.add(countPOIs("shop", ft, 250));
		b.add(countPOIs("dining", ft));
		b.add(countPOIs("dining", ft, 250));

		for (int i = 0; i < types.size(); i++) {
			b.add(ft.bits.get(i));
		}
		return b.buildFeature(null);
	}

	/**
	 * Calculate the area of landuse within a given radius.
	 */
	@SuppressWarnings("unchecked")
	private double calcLanduse(String type, Feature ft, double radius) {

		if (ft.isResidentialOnly()) {
			return 0;
		}

		MultiPolygon geometry = ft.geometry;
		Geometry bbox = geometry.getCentroid().buffer(radius);

		double res = 0;
		List<Feature> query = landuse.query(bbox.getEnvelopeInternal());
		for (Feature q : query) {

			if (!q.geomIssues && q.hasLanduse(type)) {
				try {
					res += q.geometry.intersection(bbox).getArea();
				} catch (TopologyException e) {
					q.geomIssues = true;
				}
			}
		}

		// convert to square kilometers
		return BigDecimal.valueOf(res / 1_000_000).setScale(4, RoundingMode.HALF_EVEN).doubleValue();
	}

	private double calcArea(String activityType, Feature ft, double radius) {

		if (ft.isResidentialOnly()) {
			return 0;
		}

		MultiPolygon geometry = ft.geometry;
		Geometry bbox = geometry.getCentroid().buffer(radius);

		double res = 0;
		List<Feature> query = entities.query(bbox.getEnvelopeInternal());
		for (Feature q : query) {
			if (!q.geomIssues && q.hasType(activityType)) {
				try {
					res += q.geometry.intersection(bbox).getArea();
				} catch (TopologyException e) {
					q.geomIssues = true;
				}
			}
		}

		return BigDecimal.valueOf(res / 1_000).setScale(4, RoundingMode.HALF_EVEN).doubleValue();
	}

	private double findNearest(Feature ft, Predicate<Feature> filter) {

		if (ft.isResidentialOnly()) {
			return 0;
		}

		MultiPolygon geometry = ft.geometry;
		for (Integer radius : List.of(500, 5000, 20000)) {

			Geometry bbox = geometry.getCentroid().buffer(radius);

			List<Feature> query = pois.query(bbox.getEnvelopeInternal());

			OptionalDouble dist = query.stream()
				.filter(filter)
				.mapToDouble(a -> a.geometry.distance(geometry))
				.min();

			if (dist.isPresent()) {
				return dist.getAsDouble();
			}

		}

		return 20000;
	}

	private int countPOIs(String type, Feature ft) {

		int count = 0;
		int typeIndex = types.getInt(type);

		if (ft.bits.get(typeIndex)) {
			count++;
		}
		if (ft.members != null) {
			for (Feature m : ft.members) {
				if (m.bits.get(typeIndex)) {
					count++;
				}
			}
		}

		return count;
	}

	@SuppressWarnings("unchecked")
	private int countPOIs(String type, Feature ft, double radius) {

		if (ft.isResidentialOnly()) {
			return 0;
		}

		// Base count
		int count = countPOIs(type, ft);

		Geometry bbox = ft.geometry.getCentroid().buffer(radius);

		Iterable<Feature> query = entities.query(bbox.getEnvelopeInternal());
		for (Feature q : query) {
			try {
				if (!q.geomIssues && q.geometry.distance(ft.geometry.getCentroid()) < radius && q != ft) {
					count += countPOIs(type, q);
				}
			} catch (TopologyException e) {
				q.geomIssues = true;
			}
		}

		return count;
	}
}
