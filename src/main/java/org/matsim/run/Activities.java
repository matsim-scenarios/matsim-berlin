package org.matsim.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

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

	work(6, 20),
	work_business(8, 20),
	personal_business(8, 20),
	leisure(9, 27),
	dining(8, 27),
	shop_daily(8, 20),
	shop_other(8, 20);

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
	public PlanCalcScoreConfigGroup.ActivityParams apply(PlanCalcScoreConfigGroup.ActivityParams params) {
		if (start >= 0)
			params = params.setOpeningTime(start * 3600.);
		if (end >= 0)
			params = params.setClosingTime(end * 3600.);

		return params;
	}

	/**
	 * Add required activity params for the scenario.
	 */
	public static void addScoringParams(Config config, boolean splitTypes) {

		for (Activities value : Activities.values()) {
			// Default length if none is given
			config.planCalcScore().addActivityParams(value.apply(new PlanCalcScoreConfigGroup.ActivityParams(value.name())).setTypicalDuration(6 * 3600));

			if (splitTypes)
				for (long ii = 600; ii <= 97200; ii += 600) {
					config.planCalcScore().addActivityParams(value.apply(new PlanCalcScoreConfigGroup.ActivityParams(value.name() + "_" + ii).setTypicalDuration(ii)));
				}
		}

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("ride interaction").setTypicalDuration(60));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("service").setTypicalDuration(3600));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("start").setTypicalDuration(3600));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("end").setTypicalDuration(3600));

	}

}
