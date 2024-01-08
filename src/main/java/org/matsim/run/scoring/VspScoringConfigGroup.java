package org.matsim.run.scoring;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;

/**
 * Stores vsp specific scoring parameters.
 */
public final class VspScoringConfigGroup extends ReflectiveConfigGroup {

	private static final String GROUP_NAME = "vspScoring";

	private final Map<String, ModeParams> modeParams = new HashMap<>();

	@Parameter
	@Comment("The distance groups for which the marginal utility of distance is defined.")
	private String distGroups;

	public VspScoringConfigGroup() {
		super(GROUP_NAME);
	}


	public List<Integer> getDistGroups() {
		return distGroups == null ? List.of() : Arrays.stream(distGroups.split(",")).map(Double::parseDouble).map(Double::intValue).toList();
	}

	/**
	 * Configured mode parameters.
	 */
	public Map<String, ModeParams> getModeParams() {
		return modeParams;
	}

	/**
	 * Retrieve mode parameters.
	 */
	public ModeParams getModeParams(String mode) {
		if (!modeParams.containsKey(mode)) {
			ModeParams p = new ModeParams();
			p.mode = mode;

			addParameterSet(p);
			return p;
		}

		return modeParams.get(mode);
	}

	@Override
	public ModeParams createParameterSet(String type) {
		if (type.equals(ModeParams.GROUP_NAME)) {
			return new ModeParams();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof ModeParams p) {
			super.addParameterSet(set);
			modeParams.put(p.mode, p);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	/**
	 * Stores context specific parameters.
	 */
	public static final class ModeParams extends ReflectiveConfigGroup {

		private static final String GROUP_NAME = "modeParams";

		@Parameter
		@Comment("The mode for which the parameters are defined.")
		public String mode;


		public ModeParams() {
			super(GROUP_NAME, true);
		}

		public ModeParams setDistUtil(int dist, double util) {
			Map<String, String> p = getParams();
			p.put(Double.toString(dist), Double.toString(util));
			return this;
		}

		/**
		 * Get the utility for given distance group.
		 */
		public OptionalDouble getDistUtil(int distGroup) {

			Map<String, String> p = getParams();

			String key = Integer.toString(distGroup);
			if (p.containsKey(key)) {
				return OptionalDouble.of(Double.parseDouble(p.get(key)));
			}

			return OptionalDouble.empty();
		}
	}
}
