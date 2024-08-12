package org.matsim.prepare.facilities;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.locationtech.jts.geom.MultiPolygon;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * Features for one facility, stored as bit set.
 */
final class Feature {

	final OsmEntity entity;
	final OsmType osmType;
	final BitSet bits;
	final boolean isBuilding;
	final boolean isUnspecific;
	final boolean isLanduse;
	final MultiPolygon geometry;

	/**
	 * The activity types and their index.
	 */
	private final Object2IntMap<String> types;

	/**
	 * Stores all assigned osm members.
	 */
	List<Feature> members;

	/**
	 * These feature has low matching priority and will not be assigned to another feature.
	 */
	boolean lowPriority = false;

	/**
	 * Number of levels/floors in the building.
	 */
	private int levels;

	/**
	 * Was assigned as member to another feature.
	 */
	private boolean assigned = false;

	/**
	 * Will be set to true if issues occurred with the geometry of this feature.
	 */
	public boolean geomIssues = false;

	Feature(OsmEntity entity, Object2IntMap<String> types, MultiPolygon geometry,
			boolean isBuilding, boolean isUnspecific, boolean isLanduse) {

		this.entity = entity;
		this.osmType = entity instanceof OsmWay ? OsmType.way : entity instanceof OsmNode ? OsmType.node : OsmType.relation;
		this.types = types;
		this.bits = new BitSet(types.size());
		this.isBuilding = isBuilding;
		this.isUnspecific = isUnspecific;
		this.isLanduse = isLanduse;
		this.bits.clear();
		this.geometry = geometry;
	}

	/**
	 * Check if the entity has any activity types.
	 */
	boolean hasTypes() {
		return !bits.isEmpty();
	}

	void set(Set<String> acts) {
		for (String act : acts) {
			bits.set(types.getInt(act), true);
		}
	}

	void assign(Feature other) {
		if (this.members == null) {
			this.members = new ArrayList<>();
		}

		// A feature can have exactly one parent
		if (!other.assigned) {
			this.members.add(other);
			// copy potential other members
			if (other.members != null) {
				other.members.clear();
				this.members.addAll(other.members);
			}
		}

		for (int i = 0; i < types.size(); i++) {
			if (other.bits.get(i))
				this.bits.set(i);
		}

		other.assigned = true;
	}

	public int getLevels() {
		return levels;
	}

	void setLevels(String value) {
		try {
			levels = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	public boolean isAssigned() {
		return assigned;
	}

	/**
	 * Check if the entity has a specific landuse tag.
	 */
	boolean hasLanduse(String landuse) {

		if (!isLanduse)
			return false;

		int n = entity.getNumberOfTags();
		for (int i = 0; i < n; i++) {
			OsmTag tag = entity.getTag(i);
			if (tag.getKey().equals("landuse") && (landuse == null || tag.getValue().equals(landuse))) {
				return true;
			}
		}
		return false;
	}

	boolean isResidentialOnly() {
		return bits.get(types.getInt("resident")) && bits.cardinality() == 1;
	}

	public void setLowPriority() {
		this.lowPriority = true;
	}

	public enum OsmType {
		way, node, relation
	}
}
