package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.*;
import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;

class KNRunBerlinScenario {
	private enum MyScenario { bln1pct, bln10pct, equil } ;
	private static MyScenario myScenario = MyScenario.equil ;
	
	public static void main(String[] args) {
//		String configFileName = "scenarios/berlin-v5.0-10pct-2018-06-18/input/berlin-5.0_config.xml";
//		String configFileName = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.1_config.xml";
		String configFileName = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.1_config_reduced.xml";
		
		// "overridingConfig.xml" is taken as an argument now which was hard-coded in there. Amit July'18
//		final RunBerlinScenario berlin = new RunBerlinScenario( configFileName);
		final RunBerlinScenario berlin = new RunBerlinScenario( configFileName, "overridingConfig.xml" );
		
		final Config config = berlin.prepareConfig() ;
		
		config.global().setNumberOfThreads( 6 );
		config.qsim().setNumberOfThreads( 6 );
		config.parallelEventHandling().setNumberOfThreads( 1 );
		
		config.controler().setLastIteration( 500 );

		config.strategy().setFractionOfIterationsToDisableInnovation( 0.7 );
		// 50 w/ 500
		// 20 w/ 100
		
		
		
		//		config.controler().setWritePlansInterval( 10 );
		config.controler().setOutputDirectory( "./output" );
		config.controler().setRunId( null );
		config.planCalcScore().setWriteExperiencedPlans( true );
		{
			final StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode );
			stratSets.setWeight( 0.2 );
			stratSets.setSubpopulation( "person" );
			config.strategy().addStrategySettings( stratSets );
			
			config.changeMode().setModes( config.subtourModeChoice().getModes() );
		}
		for ( StrategySettings settings : config.strategy().getStrategySettings() ) {
			if ( settings.getStrategyName().equals( DefaultStrategy.SubtourModeChoice ) ) {
				settings.setWeight( 0.0 );
			}
		}
		//		config.transit().setUsingTransitInMobsim( false );
		
		switch( myScenario ) {
			case bln1pct:
				break;
			case bln10pct:
				break;
			case equil:
				config.global().setNumberOfThreads( 1 );
				config.qsim().setNumberOfThreads( 1 );
				config.parallelEventHandling().setNumberOfThreads( 1 );

				config.network().setInputFile( "../../equil/network.xml" );
//				config.plans().setInputFile( "../../equil/plans100.xml" );
				config.plans().setInputFile( "../../equil/plans2000.xml.gz" );
				config.plans().setInputPersonAttributeFile( null );
				
				config.qsim().setFlowCapFactor( 1.0 );
				config.qsim().setStorageCapFactor( 1.0 );

				config.transit().setUseTransit( false );
			{
				final ModeRoutingParams modeRoutingParams = new ModeRoutingParams( TransportMode.pt ) ;
				modeRoutingParams.setTeleportedModeFreespeedFactor(3. );
				config.plansCalcRoute().addModeRoutingParams( modeRoutingParams );
			}
			{
//			final ModeParams params = config.planCalcScore().getScoringParametersPerSubpopulation().get( "person" ).getModes().get( TransportMode.pt );
				final ModeParams params = config.planCalcScore().getModes().get( TransportMode.pt );
				params.setMarginalUtilityOfDistance( -0.0001 ); // yyyy should rather be a distance cost rate
			}
			{
				final ActivityParams params = config.planCalcScore().getActivityParams( "work_28800.0" );;
				Gbl.assertNotNull( params );
				params.setOpeningTime( 8*3600. );
				params.setLatestStartTime( 8*3600. );
			}
			config.planCalcScore().setLateArrival_utils_hr( -6 );
//			{
//				final ActivityParams actParams = new ActivityParams( "w" ) ;
//				actParams.setTypicalDuration( 8.*3600. );
//				actParams.setOpeningTime( 6.*3600. );
//				actParams.setClosingTime( 18.*3600. );
//				config.planCalcScore().getScoringParametersPerSubpopulation().get("person").addActivityParams( actParams );
//			}
//			{
//				final ActivityParams actParams = new ActivityParams( "h" ) ;
//				actParams.setTypicalDuration( 16.*3600. );
//				config.planCalcScore().getScoringParametersPerSubpopulation().get("person").addActivityParams( actParams );
//			}
				break;
		}
		
		final Scenario scenario = berlin.prepareScenario() ;
		
		switch ( myScenario ) {
			case bln1pct:
				break;
			case bln10pct:
				break;
			case equil:
				for ( Link link : scenario.getNetwork().getLinks().values() ) {
					final Set<String> allowedModes = new HashSet<>( link.getAllowedModes() ) ;
					allowedModes.add( "freight") ;
					allowedModes.add( "ride") ;
					link.setAllowedModes( allowedModes ) ;
				}
				for ( Person person : scenario.getPopulation().getPersons().values()  ) {
					person.getAttributes().putAttribute( config.plans().getSubpopulationAttributeName(), "person" ) ;
					
					final String objectId = person.getId().toString() ;
					final String attribute = config.plans().getSubpopulationAttributeName() ;
					scenario.getPopulation().getPersonAttributes().putAttribute( objectId, attribute, "person" ) ;
				}
				break;
		}
		
		List<AbstractModule> overridingModules = new ArrayList<>() ;
		
		overridingModules.add( new AbstractModule() {
			@Override public void install() {
				DiversityGeneratingPlansRemover.Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
				final double ccc = 0.03 ;
				builder.setSameLocationPenalty( ccc ) ;
				builder.setSameActivityTypePenalty( ccc ) ;
				builder.setSameActivityEndTimePenalty( ccc ) ;
				builder.setSameModePenalty( ccc ) ;
				builder.setSameRoutePenalty( ccc ) ;
//				builder.setStageActivityTypes( tripRouter.getStageActivityTypes() ) ;
				this.bindPlanSelectorForRemoval().toProvider( builder ) ;
			}
		} );
		
		overridingModules.add( new KaiAnalysisListener.Module() );
		
		berlin.prepareControler( overridingModules.toArray( new AbstractModule[0] ) );
		
		berlin.run() ;
	}
	
}
