package org.matsim.run.drt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
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
		private static final Logger log = Logger.getLogger( RunDrtOpenBerlinScenarioTest.class ) ;
		
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
			// Decrease population to 0.1% sample 
			List<Id<Person>> agentsToRemove = new ArrayList<>();
			for (Id<Person> id: scenario.getPopulation().getPersons().keySet()) {
				if (MatsimRandom.getRandom().nextDouble() < 0.1) {agentsToRemove.add(id);}
			}
			for (Id<Person> id: agentsToRemove) {
				scenario.getPopulation().removePerson(id);
			}
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;			
			
			// TODO: test the scores in iteration 0 and 4
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// During debug some exceptions only occured at the replanning stage of the 3rd
	// iteration, so we need at least 3 iterations.
	// Have at least 0.1 pct of the population to have as many strange corner cases
	// as possible (because those tend to cause exceptions otherwise not found).
	@Test
	public final void testAFewAgentsOnly() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-Berlkoenig-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(2);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../../test/input/drt/BerlkoenigAreaTestAgents.xml");
			
			// jvm on build server has less cores than we set in the input config file and would complain about that
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			config.controler().setWritePlansInterval(1);
			
			// make pt more attractive to obtain less transit_walks due to drt triangle walk being more attractive 
			config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(5);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
			}
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			controler.run() ;	
			
			Plan intermodalPtAgentPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("285614901pt")).getSelectedPlan();
			
			boolean foundIntermodalTrip = false;
			
			List<Trip> trips = TripStructureUtils.getTrips(intermodalPtAgentPlan.getPlanElements());
			
			for (Trip trip: trips) {
				Set<String> modes = new HashSet<>();
				for (Leg leg: trip.getLegsOnly()) {
					modes.add(leg.getMode());
				}
				if (modes.contains(TransportMode.drt) && modes.contains(TransportMode.pt)) {
					foundIntermodalTrip = true;
				}
			}
			Assert.assertTrue(foundIntermodalTrip);
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
}
