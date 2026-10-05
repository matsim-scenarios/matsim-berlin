package org.matsim.run;

import org.matsim.contrib.common.conventions.vsp.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;

/**
 * Defines available activity types.
 */
public enum Activities {
	home,
	other,
	outside_recreation,
	transport,
	visit,
	edu_kiga(7, 17),
	edu_primary(7, 17),
	edu_secondary(7, 17),
	edu_higher(7, 19),
	edu_other(7, 22),

	work(6, 21),
	work_business(8, 21),
	personal_business(8, 20),
	leisure(9, 27),
	dining(8, 27),
	shop_daily(8, 20),
	shop_other(8, 20),

	// Commercial traffic types
	service;

	/**
	 * Start time of an activity in hours, can be -1 if not defined.
	 */
	private final double start;

	/**
	 * End time of an activity in hours, can be -1 if not defined.
	 */
	private final double end;

	Activities(double start, double end) {
		this.start = start;
		this.end = end;
	}

	Activities() {
		this.start = -1;
		this.end = -1;
	}


	/**
	 * Apply start and end time to params.
	 */
	public ScoringConfigGroup.ActivityParams apply(ScoringConfigGroup.ActivityParams params) {
		if (start >= 0)
			params = params.setOpeningTime(start * 3600.);
		if (end >= 0)
			params = params.setClosingTime(end * 3600.);

		return params;
	}

	/**
	 * Add required activity params for the scenario.
	 *
	 * @param splitTypes        also register the duration-binned type variants (type_600 .. type_97200). Only needed
	 *                          for populations whose typical durations are still encoded in the activity type; with
	 *                          the typicalDuration attribute the untagged types are enough.
	 * @param withOpeningTimes  give the types their opening and closing times. Off for attribute-based typical
	 *                          durations, which carry the schedule themselves.
	 */
	public static void addScoringParams(Config config, boolean splitTypes, boolean withOpeningTimes) {

		for (Activities value : Activities.values()) {
			// Default length if none is given
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams(value.name()).setTypicalDuration(6 * 3600);
			config.scoring().addActivityParams(withOpeningTimes ? value.apply(params) : params);

//			the _morning and _evening variants that SplitWrapAroundActivities produces to switch off wrap-around
//			scoring; deliberately without opening times, since they are the two halves of an overnight activity.
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(SnzActivities.createMorningActivityType(value.name())).setTypicalDuration(6 * 3600));
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(SnzActivities.createEveningActivityType(value.name())).setTypicalDuration(6 * 3600));

			if (splitTypes)
				for (long ii = 600; ii <= 97200; ii += 600) {
					ScoringConfigGroup.ActivityParams binned = new ScoringConfigGroup.ActivityParams(value.name() + "_" + ii).setTypicalDuration(ii);
					config.scoring().addActivityParams(withOpeningTimes ? value.apply(binned) : binned);
				}
		}

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(3600));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(3600));

	}

}
