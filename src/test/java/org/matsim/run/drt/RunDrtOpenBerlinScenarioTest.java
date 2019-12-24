package org.matsim.run.drt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author gleich
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunDrtOpenBerlinScenarioTest {
		
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// During debug some exceptions only occured at the replanning stage of the 3rd
	// iteration, so we need at least 3 iterations.
	// Have at least 0.1 pct of the population to have as many strange corner cases
	// as possible (because those tend to cause exceptions otherwise not found).
	@Test
	public final void eTest0_1pctUntilIteration3() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(2);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			// jvm on build server has less cores than we set in the input config file and would complain about that
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
			}
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			// Decrease population to 0.01% sample 
			List<Id<Person>> agentsToRemove = new ArrayList<>();
			for (Id<Person> id: scenario.getPopulation().getPersons().keySet()) {
				if (MatsimRandom.getRandom().nextDouble() > 0.01) {agentsToRemove.add(id);}
			}
			for (Id<Person> id: agentsToRemove) {
				scenario.getPopulation().removePerson(id);
			}
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;			
			
			// TODO: test the scores in iteration 0 and 4
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	// During debug some exceptions only occured at the replanning stage of the 3rd
	// iteration, so we need at least 3 iterations.
	@Test
	public final void testAFewAgentsOnly() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(2);
			config.strategy().clearStrategySettings();
			
			// Use RandomSingleTripReRoute, because in this branch only in RandomSingleTripReRoute drt is allowed as access/egress mode to pt
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName("RandomSingleTripReRoute");
			stratSets.setWeight(1.0);
			stratSets.setSubpopulation("person");
			config.strategy().addStrategySettings(stratSets);
			
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../../test/input/drt/drt-test-agents.xml");
			
			// jvm on build server has less cores than we set in the input config file and would complain about that
			config.global().setNumberOfThreads(4);
			config.qsim().setNumberOfThreads(1);
			
			config.controler().setWritePlansInterval(1);
			
			// make pt more attractive to obtain less direct walks (routing mode pt) due to drt triangle walk being more attractive 
			config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(5);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
				drtCfg.setDrtServiceAreaShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlkoenig-area/berlkoenig-area.shp");
			}
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			FareEventChecker fareChecker = new FareEventChecker();
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(fareChecker);
				}
			});
			
			controler.run() ;	
			
			Id<Person> intermodalPtAgentId = Id.createPersonId("285614901pt_w_drt");
			Plan intermodalPtAgentPlan = scenario.getPopulation().getPersons().get(intermodalPtAgentId).getSelectedPlan();
			
			int intermodalTripCounter = 0;
			int drtLegsInIntermodalTripsCounter = 0;
			
			List<Trip> trips = TripStructureUtils.getTrips(intermodalPtAgentPlan.getPlanElements());
			
			for (Trip trip: trips) {
				Map<String, Integer> mode2NumberOfLegs = new HashMap<>();
				for (Leg leg: trip.getLegsOnly()) {
					if (!mode2NumberOfLegs.containsKey(leg.getMode())) {
						mode2NumberOfLegs.put(leg.getMode(), 1);
					} else {
						mode2NumberOfLegs.put(leg.getMode(), mode2NumberOfLegs.get(leg.getMode()) + 1);
					}
				}
				if (mode2NumberOfLegs.containsKey(TransportMode.drt) && mode2NumberOfLegs.containsKey(TransportMode.pt)) {
					intermodalTripCounter++;
					drtLegsInIntermodalTripsCounter = drtLegsInIntermodalTripsCounter + mode2NumberOfLegs.get(TransportMode.drt);
				}
			}
			Assert.assertTrue("pt agent has no intermodal route (=drt for access or egress to pt)", intermodalTripCounter > 0);
			
			// check drt-pt-intermodal trip fare compensator
			List<PersonMoneyEvent> moneyEventsIntermodalAgent = fareChecker.getEventsForPerson(intermodalPtAgentId);
			double expectedCompensationAmountPerTrip = 1.0;// TODO: get from config instead?
			int compensatorMoneyEventsCounter = 0;
			for(PersonMoneyEvent event: moneyEventsIntermodalAgent) {
				if (Math.abs(event.getAmount() - expectedCompensationAmountPerTrip) < MatsimTestUtils.EPSILON) {
					compensatorMoneyEventsCounter++;
				}
			}
			
			Assert.assertEquals("Number of intermodal trips and of intermodal trip fare compensator money events should be equal.", drtLegsInIntermodalTripsCounter, compensatorMoneyEventsCounter);
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}


	class FareEventChecker implements PersonMoneyEventHandler {
		private Map<Id<Person>, List<PersonMoneyEvent>> person2moneyEvents = new HashMap<>();

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			if (!person2moneyEvents.containsKey(event.getPersonId())) {
				person2moneyEvents.put(event.getPersonId(), new ArrayList<>());
			}
			person2moneyEvents.get(event.getPersonId()).add(event);
		}

		List<PersonMoneyEvent> getEventsForPerson(Id<Person> personId) {
			return person2moneyEvents.get(personId);
		}
	}
}