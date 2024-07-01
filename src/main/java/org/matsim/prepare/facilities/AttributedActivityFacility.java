package org.matsim.prepare.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Map;

import static org.matsim.prepare.population.Attributes.ATTRACTION_OTHER;
import static org.matsim.prepare.population.Attributes.ATTRACTION_WORK;

/**
 * Wraps any {@link ActivityFacility} and adds cached attributes because the normal attributes are terrible slow.

 */
public final class AttributedActivityFacility implements ActivityFacility {

	private final ActivityFacility facility;

	private final double workAttraction;
	private final double otherAttraction;

	public AttributedActivityFacility(ActivityFacility facility) {
		this.facility = facility;
		this.workAttraction = (double) facility.getAttributes().getAttribute(ATTRACTION_WORK);
		this.otherAttraction = (double) facility.getAttributes().getAttribute(ATTRACTION_OTHER);
	}

	public double getWorkAttraction() {
		return workAttraction;
	}

	public double getOtherAttraction() {
		return otherAttraction;
	}

	@Override
	public Map<String, ActivityOption> getActivityOptions() {
		return facility.getActivityOptions();
	}

	@Override
	public void addActivityOption(ActivityOption option) {
		facility.addActivityOption(option);
	}

	@Override
	public void setCoord(Coord coord) {
		facility.setCoord(coord);
	}

	@Override
	public Id<ActivityFacility> getId() {
		return facility.getId();
	}

	@Override
	public Id<Link> getLinkId() {
		return facility.getLinkId();
	}

	@Override
	public Coord getCoord() {
		return facility.getCoord();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return facility.getCustomAttributes();
	}

	@Override
	public Attributes getAttributes() {
		return facility.getAttributes();
	}
}
