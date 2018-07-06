package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

class KNRunBerlinScenario {
	public static void main(String[] args) {
//		String configFileName = "scenarios/berlin-v5.0-10pct-2018-06-18/input/berlin-5.0_config.xml";
		String configFileName = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml";
//		String configFileName = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
		
		final RunBerlinScenario berlin = new RunBerlinScenario( configFileName );
		
		final Config config = berlin.prepareConfig() ;
		
		config.global().setNumberOfThreads( 6 );
		config.qsim().setNumberOfThreads( 6 );
		config.parallelEventHandling().setNumberOfThreads( 1 );
		
//		config.transit().setUsingTransitInMobsim( false );
		
		final Scenario scenario = berlin.prepareScenario() ;
		
		final Controler controler = berlin.prepareControler() ;
		
		controler.addOverridingModule( new AbstractModule() {
			@Override public void install() {
				DiversityGeneratingPlansRemover.Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
				final double ccc = 5. ;
				builder.setSameLocationPenalty( ccc ) ;
				builder.setSameActivityTypePenalty( ccc ) ;
				builder.setSameActivityEndTimePenalty( ccc ) ;
				builder.setSameModePenalty( ccc ) ;
				builder.setSameRoutePenalty( ccc ) ;
//				builder.setStageActivityTypes( tripRouter.getStageActivityTypes() ) ;
				this.bindPlanSelectorForRemoval().toProvider( builder ) ;
			}
		} );
		
		berlin.run() ;
	}
	
}
