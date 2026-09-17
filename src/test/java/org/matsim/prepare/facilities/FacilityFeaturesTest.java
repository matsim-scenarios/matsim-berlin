package org.matsim.prepare.facilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.matsim.application.options.ShpOptions;
import org.matsim.run.OpenBerlinScenario;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The attraction models read their features by name from a map that answers unknown names with 0, so a feature that
 * gets lost between the gpkg and the model is not an error but a wrong prediction. These tests pin down the three
 * places where it can get lost: the schema the extractor writes, the round trip through the geopackage datastore, and
 * the map the models read from.
 */
class FacilityFeaturesTest {

	private static final Set<String> REQUIRED = FacilityFeatures.requiredFeatures(
		FacilityAttractionModelWork.INSTANCE, FacilityAttractionModelOther.INSTANCE
	);

	/**
	 * The activity types the extractor writes as boolean columns, i.e. the keys of the facility mapping.
	 */
	private static Object2IntMap<String> activityTypes() throws IOException {
		CreateMATSimFacilities.MappingConfig mapping = new ObjectMapper().readerFor(CreateMATSimFacilities.MappingConfig.class)
			.readValue(Path.of("input/facility_mapping.json").toFile());
		Object2IntMap<String> types = new Object2IntLinkedOpenHashMap<>();
		for (String type : List.of("delivery", "depot", "dining", "edu_higher", "edu_kiga", "edu_other", "edu_prim", "leisure",
			"medical", "p_business", "parking", "religious", "resident", "shop", "shop_daily", "work")) {
			types.put(type, types.size());
		}
		assertThat(types.keySet()).containsAll(mapping.attributes());
		return types;
	}

	/**
	 * The schema of the gpkg as {@link ExtractFacilityGeoPkg} writes it.
	 */
	private static SimpleFeatureType extractorSchema() throws Exception {
		return new FacilityFeatureExtractor(OpenBerlinScenario.CRS, activityTypes(),
			new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>()).featureType;
	}

	@Test
	void modelsReadTheirFeaturesByName() {
		assertThat(REQUIRED)
			.hasSize(37)
			.contains("area", "levels", "building", "residential_only", "landuse", "work", "shop", "resident", "nearest_bus_stop", "poi_shop_250m");
	}

	@Test
	void extractorWritesEveryFeatureTheModelsNeed() throws Exception {
		SimpleFeatureType schema = extractorSchema();

		FacilityFeatures.checkSchema(schema, REQUIRED, "the attraction models");
		FacilityFeatures.checkSchema(schema, activityTypes().keySet(), "the facility mapping");
	}

	/**
	 * This is the one that would have caught the geotools 35 upgrade: the boolean columns came back as Boolean instead
	 * of numbers, were not converted, and 19 of the 37 features read as 0.
	 */
	@Test
	void featuresSurviveTheRoundTripThroughTheGeopackage(@TempDir Path tmp) throws Exception {
		SimpleFeatureType schema = extractorSchema();
		Path gpkg = tmp.resolve("facilities.gpkg");

		writeOneFacility(gpkg, schema);

		// Read it the way CreateMATSimFacilities does
		List<SimpleFeature> fts = new ShpOptions(gpkg, null, null).readFeatures();
		assertThat(fts).hasSize(1);

		FacilityFeatures.checkSchema(fts.get(0).getFeatureType(), REQUIRED, "the attraction models");
		Object2DoubleMap<String> features = FacilityFeatures.features(fts.get(0), REQUIRED);

		assertThat(features.keySet()).containsExactlyInAnyOrderElementsOf(REQUIRED);
		assertThat(features.getDouble("area")).isEqualTo(216.59);
		assertThat(features.getDouble("levels")).isEqualTo(3);
		assertThat(features.getDouble("building")).isEqualTo(1);
		assertThat(features.getDouble("residential_only")).isEqualTo(0);
		assertThat(features.getDouble("work")).isEqualTo(1);
		assertThat(features.getDouble("shop")).isEqualTo(0);

		// and the models must see the difference between an office and a house
		Object2DoubleMap<String> house = FacilityFeatures.features(fts.get(0), REQUIRED);
		house.put("work", 0);
		house.put("residential_only", 1);
		assertThat(FacilityAttractionModelWork.INSTANCE.predict(features, null))
			.isGreaterThan(FacilityAttractionModelWork.INSTANCE.predict(house, null));
	}

	@Test
	void missingFeaturesAreNotReadAsZero() throws Exception {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("facilities");
		builder.add("area", Double.class);
		builder.add("levels", String.class);
		SimpleFeatureType schema = builder.buildFeatureType();

		assertThatThrownBy(() -> FacilityFeatures.checkSchema(schema, REQUIRED, "the attraction models"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("building (missing)")
			.hasMessageContaining("levels (String)");

		SimpleFeature ft = new SimpleFeatureBuilder(schema).buildFeature("1", new Object[]{216.59, "3"});
		assertThatThrownBy(() -> FacilityFeatures.features(ft, REQUIRED))
			.isInstanceOf(IllegalArgumentException.class);

		Object2DoubleMap<String> features = FacilityFeatures.features(ft, Set.of("area"));
		assertThatThrownBy(() -> FacilityAttractionModelWork.INSTANCE.predict(features, null))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("levels");
	}

	/**
	 * One office building, written like the extractor writes them.
	 */
	private static void writeOneFacility(Path gpkg, SimpleFeatureType schema) throws Exception {
		GeometryFactory gf = new GeometryFactory();
		Polygon square = gf.createPolygon(new Coordinate[]{
			new Coordinate(797700, 5828800), new Coordinate(797720, 5828800), new Coordinate(797720, 5828820),
			new Coordinate(797700, 5828820), new Coordinate(797700, 5828800)
		});
		MultiPolygon geometry = gf.createMultiPolygon(new Polygon[]{square});

		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
		for (int i = 0; i < schema.getAttributeCount(); i++) {
			String name = schema.getDescriptor(i).getLocalName();
			Class<?> binding = schema.getDescriptor(i).getType().getBinding();
			fb.set(name, switch (name) {
				case "osm_id" -> 118529792L;
				case "osm_type" -> "way";
				case "the_geom" -> geometry;
				case "area" -> 216.59;
				case "levels" -> 3;
				case "building", "work" -> true;
				default -> Boolean.class.equals(binding) ? false : Double.class.equals(binding) ? 0.0 : 0;
			});
		}
		SimpleFeature ft = fb.buildFeature("1");

		DataStore ds = DataStoreFinder.getDataStore(Map.of(
			GeoPkgDataStoreFactory.DBTYPE.key, "geopkg",
			GeoPkgDataStoreFactory.DATABASE.key, gpkg.toString(),
			GeoPkgDataStoreFactory.READ_ONLY.key, false
		));
		try {
			ds.createSchema(schema);
			SimpleFeatureStore store = (SimpleFeatureStore) ds.getFeatureSource(schema.getTypeName());
			ListFeatureCollection collection = new ListFeatureCollection(schema);
			collection.add(ft);
			try (Transaction transaction = new DefaultTransaction("create")) {
				store.setTransaction(transaction);
				store.addFeatures(collection);
				transaction.commit();
			}
		} finally {
			ds.dispose();
		}
	}
}
