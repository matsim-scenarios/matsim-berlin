package org.matsim.run.scoring;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config for the Dresden scoring extensions (see {@link BerlinScoringFunctionFactory}), so the switch state is
 * recorded in the output config: whether activities without a typicalDuration attribute are tolerated; see
 * {@code DresdenModel}'s {@code --allow-config-typical-durations}.
 */
public final class BerlinScoringConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "berlinScoring";

	/** Allow person-subpopulation activities without a typicalDuration attribute to score against the config typical
	 * duration. Default false = such an activity aborts the run: the experiment requires every person activity to be
	 * scored against its survey-derived typical duration. Enable only for legacy type-encoded populations (v1.1 and
	 * earlier), whose typicals live in the activity type names. */
	private boolean allowConfigTypicalDurations = false;

	public BerlinScoringConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter("allowConfigTypicalDurations")
	public boolean isAllowConfigTypicalDurations() {
		return allowConfigTypicalDurations;
	}

	@StringSetter("allowConfigTypicalDurations")
	public void setAllowConfigTypicalDurations(boolean allowConfigTypicalDurations) {
		this.allowConfigTypicalDurations = allowConfigTypicalDurations;
	}
}
