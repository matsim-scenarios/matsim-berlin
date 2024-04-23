package org.matsim.prepare.facilities;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.slimjars.dist.gnu.trove.iterator.TLongObjectIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.JTS;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.OpenBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(
	name = "facility-shp",
	description = "Generate facility shape file from OSM data."
)
public class ExtractFacilityGeoPkg implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ExtractFacilityGeoPkg.class);
	private static final double POI_BUFFER = 6;

	/**
	 * Structures of this size are completely ignored.
	 */
	private static final double MAX_AREA = 50_000_000;

	/**
	 * Structures larger than this will not be assigned smaller scale types, but remain independently.
	 * Usually large areas such as parks, campus, etc.
	 */
	private static final double MAX_ASSIGN = 50_000;

	/**
	 * Percentage of minimum required intersection.
	 */
	private static final double INTERSECT_THRESHOLD = 0.2;

	private final GeometryBuilder geometryBuilder = new GeometryBuilder();
	@CommandLine.Option(names = "--input", description = "Path to input .pbf file", required = true)
	private Path pbf;
	@CommandLine.Option(names = "--output", description = "Path to output .gpkg file", required = true)
	private Path output;
	@CommandLine.Option(names = "--activity-mapping", description = "Path to activity napping json", required = true)
	private Path mappingPath;
	@CommandLine.Option(names = "--exclude", description = "Exclude these activities types from the output", split = ",", defaultValue = "")
	private Set<String> exclude;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions("EPSG:4326", OpenBerlinScenario.CRS);

	/**
	 * Maps types to feature index.
	 */
	private Object2IntMap<String> types;
	private ActivityMapping config;
	private Long2ObjectMap<Feature> pois;
	private Long2ObjectMap<Feature> landuse;
	private Long2ObjectMap<Feature> entities;
	private MathTransform transform;
	private InMemoryMapDataSet data;
	private int ignored;

	public static void main(String[] args) {
		new ExtractFacilityGeoPkg().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		PbfIterator reader = new PbfIterator(Files.newInputStream(pbf), true);

		config = new ObjectMapper().readerFor(ActivityMapping.class).readValue(mappingPath.toFile());

		CRSAuthorityFactory cFactory = CRS.getAuthorityFactory(true);
		transform = CRS.findMathTransform(cFactory.createCoordinateReferenceSystem(crs.getInputCRS()), CRS.decode(crs.getTargetCRS()), true);

		log.info("Configured tags: {}", config.getTypes());

		types = new Object2IntLinkedOpenHashMap<>();

		config.types.values().stream()
			.flatMap(c -> c.values.values().stream())
			.flatMap(Collection::stream)
			.filter(t -> !exclude.contains(t))
			.distinct()
			.sorted()
			.forEach(e -> types.put(e, types.size()));


		log.info("Configured activity types: {}", types.keySet());

		if (types.keySet().stream().anyMatch(t -> t.length() > 10)) {
			log.error("Activity names max length is 10, due to shp format limitation.");
			return 2;
		}

		if (Files.exists(output)) {
			log.info("Deleting already existing file: {}", output);
			Files.delete(output);
		}

		// Collect all geometries first
		pois = new Long2ObjectLinkedOpenHashMap<>();
		entities = new Long2ObjectLinkedOpenHashMap<>();
		landuse = new Long2ObjectLinkedOpenHashMap<>();

		data = MapDataSetLoader.read(reader, true, true, true);

		log.info("Finished loading pbf file.");

		TLongObjectIterator<OsmNode> it = data.getNodes().iterator();
		while (it.hasNext()) {
			it.advance();
			process(it.value());
		}

		log.info("Collected {} POIs", pois.size());

		TLongObjectIterator<OsmWay> it2 = data.getWays().iterator();
		while (it2.hasNext()) {
			it2.advance();
			process(it2.value());
		}

		TLongObjectIterator<OsmRelation> it3 = data.getRelations().iterator();
		while (it3.hasNext()) {
			it3.advance();
			process(it3.value());
		}

		log.info("Collected {} landuse shapes", landuse.size());
		log.info("Collected {} other entities", entities.size());

		if (ignored > 0)
			log.warn("Ignored {} invalid geometries", ignored);

		FacilityFeatureExtractor ft = new FacilityFeatureExtractor(crs.getTargetCRS(), types, entities, pois, landuse);

		processIntersection(landuse.values(), ft.entities, INTERSECT_THRESHOLD);

		log.info("Remaining landuse shapes after assignment: {} ", landuse.size());

		processIntersection(pois.values(), ft.entities, 0);

		log.info("Remaining POI after assignment: {}", pois.size());

		DataStore ds = DataStoreFinder.getDataStore(Map.of(
			GeoPkgDataStoreFactory.DBTYPE.key, "geopkg",
			GeoPkgDataStoreFactory.DATABASE.key, output.toFile().toString(),
			JDBCDataStoreFactory.BATCH_INSERT_SIZE.key, 100,
			GeoPkgDataStoreFactory.READ_ONLY.key, false
		));

		ds.createSchema(ft.featureType);

		SimpleFeatureStore source = (SimpleFeatureStore) ds.getFeatureSource(ft.featureType.getTypeName());
		ListFeatureCollection collection = new ListFeatureCollection(ft.featureType);

		addFeatures(entities, ft, collection);
		addFeatures(landuse, ft, collection);
		addFeatures(pois, ft, collection);

		Transaction transaction = new DefaultTransaction("create");
		source.setTransaction(transaction);

		source.addFeatures(collection);
		transaction.commit();

		log.info("Wrote {} features", collection.size());

		transaction.close();

		ds.dispose();

		writeMapping(output.toString().replace(".gpkg", "_mapping.csv.gz"),
			entities.values(), landuse.values(), pois.values());

		return 0;
	}

	/**
	 * Tags buildings within that intersections with geometries from list. Used geometries are removed from the list.
	 */
	private void processIntersection(Collection<Feature> list, STRtree index, double threshold) {

		Iterator<Feature> it = ProgressBar.wrap(list.iterator(), "Assigning features");

		while (it.hasNext()) {
			Feature ft = it.next();

			List<Feature> query = index.query(ft.geometry.getBoundary().getEnvelopeInternal());

			for (Feature other : query) {
				// Assign other features to the buildings
				double otherArea = other.geometry.getArea();
				try {
					if (ft.geometry.intersects(other.geometry) && otherArea < MAX_ASSIGN) {

						double intersectArea = ft.geometry.intersection(other.geometry).getArea();
						if (intersectArea / otherArea > threshold) {
							other.assign(ft);
						}
					}
				} catch (TopologyException e) {
					// some geometries are not well defined
					if (ft.geometry.getBoundary().intersects(other.geometry.getBoundary()) && otherArea < MAX_ASSIGN) {

						double intersectArea = ft.geometry.getBoundary().intersection(other.geometry.getBoundary()).getArea();
						if (intersectArea / otherArea > threshold) {
							other.assign(ft);
						}
					}
				}
			}

			if (ft.isAssigned())
				it.remove();
		}
	}

	private void addFeatures(Long2ObjectMap<Feature> fts, FacilityFeatureExtractor exc,
							 ListFeatureCollection collection) {


		try (ProgressBar pb = new ProgressBar("Creating features", fts.size())) {

			List<SimpleFeature> features = fts.values().parallelStream()
				.filter(ft -> !ft.bits.isEmpty())
				.map(f -> {
					pb.step();
					return exc.createFeature(f);
				})
				.toList();

			// toList retains the original order
			collection.addAll(features);
		}
	}

	/**
	 * Writes how osm ids are mapped to other ids.
	 */
	@SafeVarargs
	private void writeMapping(String path, Iterable<Feature>... features) {

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("osm_id", "parent_id");
			for (Feature feature : Iterables.concat(features)) {

				if (feature.bits.isEmpty())
					continue;

				csv.printRecord(feature.entity.getId(), feature.entity.getId());
				if (feature.members != null) {
					for (Feature member : feature.members) {
						csv.printRecord(member.entity.getId(), feature.entity.getId());
					}
				}
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Stores entities and geometries as necessary.
	 */
	private void process(OsmEntity entity) {
		boolean filtered = true;
		int n = entity.getNumberOfTags();
		for (int i = 0; i < n; i++) {
			OsmTag tag = entity.getTag(i);

			// Buildings are always kept
			if (tag.getKey().equals("building")) {
				filtered = false;
				break;
			}

			MappingConfig c = config.types.get(tag.getKey());
			if (c != null) {
				if (c.values.containsKey("*") || c.values.containsKey(tag.getValue())) {
					filtered = false;
					break;
				}
			}
		}

		if (filtered)
			return;

		if (entity instanceof OsmNode node) {

			Point p = geometryBuilder.build(node);
			MultiPolygon geometry;
			try {
				Polygon polygon = (Polygon) JTS.transform(p, transform).buffer(POI_BUFFER);
				geometry = geometryBuilder.getGeometryFactory().createMultiPolygon(new Polygon[]{polygon});
			} catch (TransformException e) {
				ignored++;
				return;
			}

			Feature ft = new Feature(entity, types, geometry);
			parse(ft, entity);
			pois.put(entity.getId(), ft);
		} else {
			boolean landuse = false;
			for (int i = 0; i < n; i++) {
				if (entity.getTag(i).getKey().equals("landuse")) {
					landuse = true;
					break;
				}
			}

			MultiPolygon geometry;
			try {
				geometry = createPolygon(entity);
				if (geometry == null) {
					ignored++;
					return;
				}
				geometry = (MultiPolygon) JTS.transform(geometry, transform);
			} catch (TransformException e) {
				// Will be ignored
				geometry = null;
			}

			if (geometry == null) {
				ignored++;
				return;
			}

			Feature ft = new Feature(entity, types, geometry);
			parse(ft, entity);
			if (landuse) {
				this.landuse.put(ft.entity.getId(), ft);
			} else {
				// some non landuse shapes might be too large
				if (ft.geometry.getArea() < MAX_AREA)
					entities.put(ft.entity.getId(), ft);
			}
		}
	}

	/**
	 * Parse tags into features. Can also be from different entity.
	 */
	private void parse(Feature ft, OsmEntity entity) {
		for (int i = 0; i < entity.getNumberOfTags(); i++) {

			OsmTag tag = entity.getTag(i);

			if (tag.getKey().equals("building:levels")) {
				ft.setLevels(tag.getValue());
			}

			ExtractFacilityGeoPkg.MappingConfig conf = config.getTag(tag.getKey());
			if (conf == null)
				continue;

			if (conf.values.containsKey("*")) {
				ft.set(conf.values.get("*"));
			}

			if (conf.values.containsKey(tag.getValue())) {
				ft.set(conf.values.get(tag.getValue()));
			}
		}
	}

	private MultiPolygon createPolygon(OsmEntity entity) {
		Geometry geom = null;
		try {
			if (entity instanceof OsmWay) {
				geom = geometryBuilder.build((OsmWay) entity, data);
			} else if (entity instanceof OsmRelation) {
				geom = geometryBuilder.build((OsmRelation) entity, data);
			}
		} catch (EntityNotFoundException e) {
			return null;
		}

		if (geom == null)
			throw new IllegalStateException("Unrecognized type.");

		if (geom instanceof LinearRing lr) {
			Polygon polygon = geometryBuilder.getGeometryFactory().createPolygon(lr);
			return geometryBuilder.getGeometryFactory().createMultiPolygon(new Polygon[]{polygon});
		} else if (geom instanceof MultiPolygon p) {
			return p;
		}

		return null;
	}

	private static final class ActivityMapping {
		private final Map<String, MappingConfig> types = new HashMap<>();

		public Set<String> getTypes() {
			return types.keySet();
		}

		@JsonAnyGetter
		public MappingConfig getTag(String type) {
			return types.get(type);
		}

		@JsonAnySetter
		private void setTag(String type, MappingConfig config) {
			types.put(type, config);
		}

	}

	/**
	 * Helper class to define data structure for mapping.
	 */
	public static final class MappingConfig {

		private final Map<String, Set<String>> values = new HashMap<>();

		@JsonAnyGetter
		public Set<String> getActivities(String value) {
			return values.get(value);
		}

		@JsonAnySetter
		private void setActivities(String value, Set<String> activities) {
			values.put(value, activities);
		}

	}

}
