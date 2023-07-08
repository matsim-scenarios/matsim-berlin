package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * Helper class to ensure the initial input selected plan is kept until the last iteration. Helps to ease issues when
 * learning speed between base case continued and drt policy cases is different.
 *
 * @author vsp-gleich
 */
class PlanTypeOverwriter implements BeforeMobsimListener, IterationStartsListener {

    private static final Logger log = LogManager.getLogger(RunBerlinScenario.class);
    static final String initialPlanType = "initial";
    static final String modifiedPlanType = "modified";
    private final BerlinExperimentalConfigGroup berlinExperimentalConfigGroup;
    private final Population population;

    PlanTypeOverwriter(BerlinExperimentalConfigGroup berlinExperimentalConfigGroup, Population population) {
        this.berlinExperimentalConfigGroup = berlinExperimentalConfigGroup;
        this.population = population;
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
        /*
         * Unfortunately new modified plans from the replanning step have the same plan type as the original plan
         * from which they were created. But they are always the selected plan, so if there are 2 plans of type
         * "initial", the selected one is new and the unselected one is the real original initial plan. This feels
         * hacky, but seems to be the only possible way at the moment. vsp-gleich april'21
         */
        if (berlinExperimentalConfigGroup.getPlanTypeOverwriting().equals(
                BerlinExperimentalConfigGroup.PlanTypeOverwriting.TAG_INITIAL_SELECTED_PLAN_AND_MODIFIED_PLANS_DIFFERENTLY)) {
            for (Person person : population.getPersons().values()) {
                int countPlansTaggedInitialPlan = 0;
                for (Plan plan : person.getPlans()) {
                    if (plan.getType().equals(initialPlanType)) {
                        countPlansTaggedInitialPlan++;
                    }
                }
                // if countPlansTaggedInitialPlan==1 do nothing, the only initial plan is the real initial plan
                if (countPlansTaggedInitialPlan == 2) {
                    // rename the selected plan which has type "initial" but is not the initial plan
                    person.getSelectedPlan().setType(modifiedPlanType);
                } else if (countPlansTaggedInitialPlan > 2) {
                    log.error("More than two plans tagged initial plan for person " + person.getId().toString() +
                            " this should not happen. Terminating.");
                    throw new RuntimeException("\"More than two plans tagged initial plan for person " + person.getId().toString() +
                            " this should not happen. Terminating.");
                }
            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        if (iterationStartsEvent.getIteration() == 0 && berlinExperimentalConfigGroup.getPlanTypeOverwriting().equals(
                BerlinExperimentalConfigGroup.PlanTypeOverwriting.TAG_INITIAL_SELECTED_PLAN_AND_MODIFIED_PLANS_DIFFERENTLY)) {
            population.getPersons().values().forEach(person -> person.getSelectedPlan().setType(initialPlanType));
        }
    }
}
