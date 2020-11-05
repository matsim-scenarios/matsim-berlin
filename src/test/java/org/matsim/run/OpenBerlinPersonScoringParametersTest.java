package org.matsim.run;

import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.run.OpenBerlinPersonScoringParameters;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.run.OpenBerlinPersonScoringParameters.INCOME_ATTRIBUTE_NAME;

/**
 * this class tests {@link OpenBerlinPersonScoringParameters}
 *
 * It checks whether the person specific income is read from the person attributes.
 * The marginalUtilityOfMoney should be calculated as averageIncome/personSpecificIncome and not taken from the subpopulation-specific scoring params.
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class OpenBerlinPersonScoringParametersTest {

	@Rule
	public MatsimTestUtils utils;
	private static TransitConfigGroup transitConfigGroup;
	private static ScenarioConfigGroup scenarioConfigGroup;
	private static PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private static OpenBerlinPersonScoringParameters personScoringParams;
	private static PopulationFactory factory;

	@BeforeClass
	public static void setUp() throws Exception {
		transitConfigGroup = new TransitConfigGroup();
		scenarioConfigGroup = new ScenarioConfigGroup();
		planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();

		PlanCalcScoreConfigGroup.ScoringParameterSet params_low = planCalcScoreConfigGroup.getOrCreateScoringParameters("low");
		params_low.setMarginalUtilityOfMoney(0.5);
		params_low.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);

		PlanCalcScoreConfigGroup.ScoringParameterSet params_med = planCalcScoreConfigGroup.getOrCreateScoringParameters("medium");
		params_med.setMarginalUtilityOfMoney(1);
		params_med.setMarginalUtlOfWaitingPt_utils_hr(1d * 3600);

		PlanCalcScoreConfigGroup.ScoringParameterSet params_high = planCalcScoreConfigGroup.getOrCreateScoringParameters("high");
		params_high.setMarginalUtilityOfMoney(2);
		params_high.setMarginalUtlOfWaitingPt_utils_hr(2d * 3600);

		personScoringParams = new OpenBerlinPersonScoringParameters(planCalcScoreConfigGroup, scenarioConfigGroup, transitConfigGroup);

		factory = PopulationUtils.getFactory();

	}

	@Test
	public void testPersonInLowSubPopulationWithLowIncome(){
		Person low_low = factory.createPerson(Id.createPersonId("low_low"));
		PopulationUtils.putSubpopulation(low_low, "low");
		PopulationUtils.putPersonAttribute(low_low, INCOME_ATTRIBUTE_NAME, 0.5d);
		ScoringParameters params_low_low = personScoringParams.getScoringParameters(low_low);
		makeAssert(params_low_low, 0.5d, 0.5d);
	}

	@Test
	public void testPersonInLowSubPopulationWithHighIncome(){
		Person low_high = factory.createPerson(Id.createPersonId("low_high"));
		PopulationUtils.putSubpopulation(low_high, "low");
		PopulationUtils.putPersonAttribute(low_high, INCOME_ATTRIBUTE_NAME, 2d);
		ScoringParameters params_low_High = personScoringParams.getScoringParameters(low_high);
		makeAssert(params_low_High, 2d, 0.5d);
	}

	@Test
	public void testPersonInMedSubPopulationWithMedIncome(){
		Person med_med = factory.createPerson(Id.createPersonId("med_med"));
		PopulationUtils.putSubpopulation(med_med, "medium");
		PopulationUtils.putPersonAttribute(med_med, INCOME_ATTRIBUTE_NAME, 1d);
		ScoringParameters params_low_High = personScoringParams.getScoringParameters(med_med);
		makeAssert(params_low_High, 1d, 1d);
	}

	@Test
	public void testPersonInMedSubPopulationWithHighIncome(){
		Person med_high = factory.createPerson(Id.createPersonId("med_high"));
		PopulationUtils.putSubpopulation(med_high, "medium");
		PopulationUtils.putPersonAttribute(med_high, INCOME_ATTRIBUTE_NAME, 2d);
		ScoringParameters params_low_High = personScoringParams.getScoringParameters(med_high);
		makeAssert(params_low_High, 2d, 1d);
	}

	@Test
	public void testPersonInHighSubPopulationWithMedIncome(){
		Person med_med = factory.createPerson(Id.createPersonId("high_med"));
		PopulationUtils.putSubpopulation(med_med, "high");
		PopulationUtils.putPersonAttribute(med_med, INCOME_ATTRIBUTE_NAME, 2d);
		ScoringParameters params_low_High = personScoringParams.getScoringParameters(med_med);
		makeAssert(params_low_High, 2d, 2d);
	}

	@Test
	public void testPersonInHighSubPopulationWithHighIncome(){
		Person med_high = factory.createPerson(Id.createPersonId("high_high"));
		PopulationUtils.putSubpopulation(med_high, "high");
		PopulationUtils.putPersonAttribute(med_high, INCOME_ATTRIBUTE_NAME, 2d);
		ScoringParameters params_low_High = personScoringParams.getScoringParameters(med_high);
		makeAssert(params_low_High, 2d, 2d);
	}


	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assert.assertEquals("marginalUtilityOfMoney is wrong", 1 / income , params.marginalUtilityOfMoney, 0.);
		Assert.assertEquals("marginalUtilityOfWaitingPt_s is wrong", marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0.);
	}


}