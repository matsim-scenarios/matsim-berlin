package org.matsim.prepare.facilities;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmTag;
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
	final BitSet bits;
	final MultiPolygon geometry;
	private final Object2IntMap<String> types;
	/**
	 * Stores all assigned osm members.
	 */
	List<Feature> members;
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

	Feature(OsmEntity entity, Object2IntMap<String> types, MultiPolygon geometry) {
		this.entity = entity;
		this.types = types;
		this.bits = new BitSet(types.size());
		this.bits.clear();
		this.geometry = geometry;
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
		int n = entity.getNumberOfTags();
		for (int i = 0; i < n; i++) {
			OsmTag tag = entity.getTag(i);
			if (tag.getKey().equals("landuse") && (landuse == null || tag.getValue().equals(landuse))) {
				return true;
			}
		}
		return false;
	}

}
