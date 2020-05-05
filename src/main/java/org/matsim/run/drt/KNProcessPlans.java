package org.matsim.run.drt;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.StagesAsNormalActivities;

class KNProcessPlans {
	public static void main( String[] args ) {
		
		Population pop = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
		pop.getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
//		PopulationUtils .readPopulation( pop,"berlin-drt-v5.5-1pct_drt-114/berlin-drt-v5.5-1pct_drt-114.output_plans.xml.gz" ) ;
//		PopulationUtils .readPopulation( pop,"/Users/kainagel/mnt/mathe/ils3/leich/open-berlin/output/berlin-drt-v5.5-1pct_drt-132/berlin-drt-v5.5-1pct_drt-132.output_plans.xml.gz" ) ;
//		PopulationUtils .readPopulation( pop,"/Users/kainagel/mnt/mathe/ils3/leich/open-berlin/output/berlin-drt-v5.5-1pct_drt-143/berlin-drt-v5.5-1pct_drt-143.output_plans.xml.gz") ;
		PopulationUtils.readPopulation( pop,
				"/Users/kainagel/mnt/mathe/ils3/kaddoura/avoev-intermodal-routing/output/output-i58/ITERS/it.450/i58.450.plans.xml.gz" );

//		for ( final Person person : pop.getPersons().values() ) {
//			person.getPlans().removeIf( (plan) -> !plan.equals( person.getSelectedPlan() ) ) ;
//		}
//		PopulationUtils.writePopulation( pop, "popWOnlySelectedPlans.xml.gz" );
		
		// only keep persons who use drt:
//		pop.getPersons().values().removeIf( (person) -> !selectedPlanContainsDrtAndPt( person ) ) ;
		
		// only keep persons who use nonpt:
		pop.getPersons().values().removeIf( (person) -> selectedPlanContainsPt( person ) ) ;

		// remove persons who do not have pt in unselected plans
		pop.getPersons().values().removeIf( (person) -> !unSelectedPlanContainsPt( person ) ) ;
		
		// make easier to read:
		for ( final Person person : pop.getPersons().values() ) {
			for ( final Plan plan : person.getPlans() ) {
				for ( final Leg leg : TripStructureUtils.getLegs( plan ) ) {
					leg.getAttributes().clear();
				}
				TripStructureUtils.getActivities( plan, StagesAsNormalActivities ).forEach( (act) -> act.getAttributes().clear() );
				plan.getPlanElements().removeIf( (pe) -> pe instanceof Activity && StageActivityTypeIdentifier.isStageActivity( ((Activity)pe).getType() ) );
			}
		}

		PopulationUtils.writePopulation( pop, "popCar.xml.gz" );
		
//		// of filtered plans, only write 1%:
//		{
//			Population popSel = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
//			for ( final Person person : pop.getPersons().values() ) {
//				if ( MatsimRandom.getRandom().nextDouble() < 0.01 ) {
//					final Person newPerson = popSel.getFactory().createPerson( person.getId() );
//					for ( final Plan plan : person.getPlans() ) {
//						Plan newPlan = PopulationUtils.createPlan();
//						PopulationUtils.copyFromTo( plan, newPlan );
//						newPlan.getPlanElements().removeIf( (pe) -> ( pe instanceof Activity  && ((Activity)pe).getType().endsWith( "interaction" ) ) ) ;
//						for ( final PlanElement planElement : newPlan.getPlanElements() ) {
//							planElement.getAttributes().clear() ;
//						}
//						newPerson.addPlan( newPlan );
//						if ( person.getSelectedPlan() == plan ) {
//							newPerson.setSelectedPlan( newPlan );
//						}
//					}
//					popSel.addPerson( newPerson );
//				}
//			}
//			PopulationUtils.writePopulation( popSel, "popDrtAndPtReduced.xml.gz" );
//		}
		
//		// of filtered plans, only write selected plans:
//		{
//			Population popSel = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
//			for ( final Person person : pop.getPersons().values() ) {
//				final Person newPerson = popSel.getFactory().createPerson( person.getId() );
//				Plan newPlan = PopulationUtils.createPlan();
//				PopulationUtils.copyFromTo( person.getSelectedPlan(), newPlan );
//				newPerson.addPlan( newPlan );
//				popSel.addPerson( newPerson );
//			}
//			PopulationUtils.writePopulation( popSel, "popDrtAndPtSel.xml.gz" );
//		}
//		{
//			List<Population> pops = new ArrayList<>() ;
//			for ( int ii=0 ; ii<5 ; ii++ ) {
//				pops.add( PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ) ;
//			}
//			for ( final Person person : pop.getPersons().values() ) {
//				int ii=-1 ;
//				for ( final Plan plan : person.getPlans() ) {
//					if ( plan==person.getSelectedPlan() ) {
//						continue ;
//					}
//					ii++ ;
//					Population pop2 = pops.get( ii ) ;
//
//					final Person newPerson = pop2.getFactory().createPerson( person.getId() );
//					Plan newPlan = PopulationUtils.createPlan();
//
//					PopulationUtils.copyFromTo( plan, newPlan );
//					newPerson.addPlan( newPlan );
//					pop2.addPerson( newPerson );
//				}
//
//			}
//			int jj = 1 ;
//			for ( final Population population : pops ) {
//				jj++ ;
//				PopulationUtils.writePopulation( population, "pop" + jj + ".xml.gz" );
//			}
//		}
		
	}
	
	private static boolean selectedPlanContainsDrtAndPt( final Person person ) {
		boolean containsDrt = false ;
		boolean containsPt = false ;
		Plan plan = person.getSelectedPlan() ;
		for ( final Leg leg : TripStructureUtils.getLegs( plan ) ) {
			if ( leg.getMode().contains( "drt" ) ) {
				containsDrt = true ;
			}
			if ( leg.getMode().contains( "pt" ) ) {
				containsPt = true ;
			}
		}
		return containsDrt && containsPt ;
	}
	private static boolean selectedPlanContainsNonPt( final Person person ) {
		boolean containsNonPt = false ;
		Plan plan = person.getSelectedPlan() ;
		for ( final Leg leg : TripStructureUtils.getLegs( plan ) ) {
			if ( leg.getMode().contains( "car" )  || leg.getMode().contains( "bike" ) ) {
				containsNonPt = true ;
			}
		}
		return containsNonPt ;
	}
	private static boolean selectedPlanContainsPt( final Person person ) {
		boolean containsPt = false ;
		Plan plan = person.getSelectedPlan() ;
		for ( final Leg leg : TripStructureUtils.getLegs( plan ) ) {
			if ( leg.getMode().contains( "pt" )  ) {
				containsPt = true ;
			}
		}
		return containsPt ;
	}
	private static boolean unSelectedPlanContainsPt( final Person person ) {
		boolean containsPt = false ;
		for ( final Plan plan : person.getPlans() ) {
			for ( final Leg leg : TripStructureUtils.getLegs( plan ) ) {
				if ( leg.getMode().contains( "pt" ) ) {
					containsPt = true;
				}
			}
		}
		return containsPt ;
	}
}
