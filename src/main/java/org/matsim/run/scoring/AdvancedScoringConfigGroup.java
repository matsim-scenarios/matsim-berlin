package org.matsim.run.scoring;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

/**
 * Stores scoring parameters for {@link AdvancedScoringModule}.
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public final class AdvancedScoringConfigGroup extends ReflectiveConfigGroup {

	private static final String GROUP_NAME = "advancedScoring";

	private final List<ScoringParameters> scoringParameters = new ArrayList<>();

	/**
	 * Different options for income dependent scoring.
	 */
	public enum IncomeDependentScoring {
		none,
		avg_by_personal_income
	}

	@Parameter
	@Comment("The distance groups if marginal utility of distance is adjusted. In meters.")
	public List<Integer> distGroups;

	@Parameter
	@Comment("Enable income dependent marginal utility of money.")
	public IncomeDependentScoring incomeDependent = IncomeDependentScoring.avg_by_personal_income;


	// TODO: maybe option to re-assign variations or use them from attributes
	// TODO: could load the random variations from a file, helper function to only generate the variations

	public AdvancedScoringConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 *  Return the defined scoring parameters.
	 */
	public List<ScoringParameters> getScoringParameters() {
		return Collections.unmodifiableList(scoringParameters);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(ScoringParameters.GROUP_NAME)) {
			return new ScoringParameters();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof ScoringParameters p) {
			super.addParameterSet(set);
			scoringParameters.add(p);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	/**
	 * Scoring parameters for a specific group of agents.
	 * This group allows arbitrary attributes to be defined, which are matched against person attributes.
	 */
	public static final class ScoringParameters extends ReflectiveConfigGroup {

		private static final String GROUP_NAME = "scoringParameters";

		/**
		 * Params per mode.
		 */
		private final Map<String, ModeParams> modeParams = new HashMap<>();

		public ScoringParameters() {
			super(GROUP_NAME, true);
		}

		/**
		 * Checks if the given attributes match the config. If true these parameters are applicable to tbe object.
		 */
		public boolean matchObject(Attributes attr, Map<String, Category> categories) {

			for (Map.Entry<String, String> e : this.getParams().entrySet()) {
				// might be null if not defined
				Object objValue = attr.getAttribute(e.getKey());
				String category = categories.get(e.getKey()).categorize(objValue);

				// compare as string
				if (!Objects.toString(category).equals(e.getValue()))
					return false;
			}

			return true;
		}

		public Map<String, ModeParams> getModeParams() {
			return modeParams;
		}

		@Override
		public ConfigGroup createParameterSet(final String type) {
			return switch (type) {
				case ModeParams.GROUP_NAME -> new ModeParams();
				default -> throw new IllegalArgumentException(type);
			};
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
		 * Retrieve mode parameters.
		 */
		public ModeParams getOrCreateModeParams(String mode) {
			if (!modeParams.containsKey(mode)) {
				ModeParams p = new ModeParams();
				p.mode = mode;

				addParameterSet(p);
				return p;
			}

			return modeParams.get(mode);
		}

	}

	/**
	 * Stores mode specific parameters and also attributes to whom to apply this specification.
	 */
	public static final class ModeParams extends ReflectiveConfigGroup {

		private static final String GROUP_NAME = "modeParams";

		@Parameter
		@Comment("The mode for which the parameters are defined.")
		public String mode;

		@Parameter
		@Comment("[utils/leg] alternative-specific constant.")
		public double deltaConstant;

		@Parameter
		@Comment("Variation of the constant across individuals.")
		public VariationType varConstant = VariationType.fixed;

		@Parameter
		@Comment("[utils/day] if the mode is used at least once.")
		public double deltaDailyConstant;

		@Parameter
		@Comment("Variation of the daily constant across individuals.")
		public VariationType varDailyConstant = VariationType.fixed;

		@Parameter
		@Comment("total delta utility per dist group.")
		public List<Double> deltaPerDistGroup;

		public ModeParams() {
			super(GROUP_NAME);
		}
	}

	/**
	 * Variate values with random draw from specific distribution.
	 */
	public enum VariationType {
		fixed, normal, truncatedNormal
	}
}
