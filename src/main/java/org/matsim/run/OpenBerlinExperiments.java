package org.matsim.run;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import picocli.CommandLine;

import java.util.Collection;

public final class OpenBerlinExperiments extends OpenBerlinScenario {
	Logger log = LogManager.getLogger( OpenBerlinExperiments.class );

	@CommandLine.Option(names = "--incomeExponent",
		description = "income exponent",
		defaultValue = "null" )
	private String incomeExponentAsString;

	@Override protected Config prepareConfig( Config config ) {
		super.prepareConfig(config);

		// I think this will create a problem in the future, since we decided that subpopulation==null is not a valid value...
		TasteVariationsConfigParameterSet tasteVariationsConfig = config.scoring().getScoringParameters(null).getTasteVariationsParams();
		Gbl.assertNotNull(tasteVariationsConfig);

		log.warn( "we are excluding the following subpops from taste variations config:" );
		for( String excludeSubpopulation : tasteVariationsConfig.getExcludeSubpopulations() ){
			log.warn( excludeSubpopulation );
		}

		log.warn( "variationsOf={}", tasteVariationsConfig.getVariationsOf() ) ;
		log.warn("the above should say Constant, and it means that the ASC (= the constant) is randomized.  And the values are already in the persons that are read from the plans file.");

		if ( ! "null".equals( incomeExponentAsString ) ){
			double incomeExponent = Double.parseDouble( incomeExponentAsString );
			log.warn( "incomeExponent before change={}", tasteVariationsConfig.getIncomeExponent() );
			tasteVariationsConfig.setIncomeExponent( incomeExponent );
			log.warn( "incomeExponent after change={}", tasteVariationsConfig.getIncomeExponent() );
		}
		return config;
	}

	public static void main(String[] args) {

		MATSimApplication.execute(OpenBerlinExperiments.class, args);

	}

}
