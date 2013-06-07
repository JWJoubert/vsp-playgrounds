/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.pt.CadytsContext;
import org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup;
import org.matsim.contrib.cadyts.pt.CadytsPtPlanChanger;
import org.matsim.contrib.cadyts.pt.CadytsPtScoring;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;


public class CadytsAsScorWRndRouter {

	private static TransitRouterFactory createRandomizedTransitRouterFactory (final PreparedTransitSchedule preparedSchedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return 
		new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = 
					new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
				//ttCalculator.setDataCollection(DataCollection.randomizedParameters, false) ;
				//ttCalculator.setDataCollection(DataCollection.additionInformation, false) ;
				return new TransitRouterImpl(trConfig, preparedSchedule, routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}

	public static void main(String[] args) {
		String configFile ;
		final double cadytsScoringWeight;
		if(args.length==0){
			configFile = "../../";
			cadytsScoringWeight = 0.0;
		}else{
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
		}
		
		final Config config = ConfigUtils.loadConfig(configFile) ;
		final double beta=30. ;
		
		configFile= null; //M
		
		config.planCalcScore().setBrainExpBeta(beta) ;
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		
		//strategies settings
		{ //cadyts
		StrategySettings stratSets = new StrategySettings(new IdImpl(lastStrategyIdx+1));
		stratSets.setModuleName("myCadyts");
		stratSets.setProbability(0.9);
		config.strategy().addStrategySettings(stratSets);
		}
		
		{ //rnd router
		StrategySettings stratSets2 = new StrategySettings(new IdImpl(lastStrategyIdx+2));
		stratSets2.setModuleName("ReRoute"); // 
		stratSets2.setProbability(0.1);
		stratSets2.setDisableAfter(400) ;
		config.strategy().addStrategySettings(stratSets2);
		}		

		//set the controler
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true) ;

		//create cadyts context
		CadytsPtConfigGroup ccc = new CadytsPtConfigGroup() ;
		config.addModule(CadytsPtConfigGroup.GROUP_NAME, ccc) ;
		final CadytsContext cContext = new CadytsContext( config ) ;
		controler.addControlerListener(cContext) ;
		
		//set cadyts as strategy for plan selector
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPtPlanChanger planSelector = new CadytsPtPlanChanger(scenario2, cContext);
				//planSelector.setCadytsWeight(0.0) ;   // <-set it to zero if only cadyts scores
				return new PlanStrategyImpl(planSelector);
			}
		} ) ;
		

		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				//if (params == null) {																			//<- see comment about performance improvement in 
				//	params = new CharyparNagelScoringParameters(config.planCalcScore()); //org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.createNewScoringFunction
				//}

				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsPtScoring scoringFunction = new CadytsPtScoring(plan,config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
 
				return scoringFunctionAccumulator;
			}
		}) ;
		
		//create and set the factory for rndizedRouter 
		final TransitSchedule routerSchedule = controler.getScenario().getTransitSchedule();
		final TransitRouterConfig trConfig = new TransitRouterConfig( config );
		
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(routerSchedule, trConfig.beelineWalkConnectionDistance);
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(routerSchedule);
		TransitRouterFactory randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (preparedSchedule, trConfig, routerNetwork);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
		
		
		///controler listener to write object attributes each 10 iterations.  They are obtained from plan's custom attributes
//		controler.addControlerListener(new IterationEndsListener() {
//			final String strPlan = "plan";
//			
//			@Override
//			public void notifyIterationEnds(IterationEndsEvent event) {
//				if ( event.getIteration()%10==0  && event.getIteration()>0){
//					ObjectAttributes attrs = new ObjectAttributes() ;
//					
//					Population pop = event.getControler().getPopulation() ;
//					for ( Person person : pop.getPersons().values() ) {
//						int cnt = 0 ;
//						for ( Plan plan : person.getPlans() ) {
//							Double cadytsCorrection = 0. ;
//							if ( plan.getCustomAttributes() != null ) {
//								cadytsCorrection = (Double) plan.getCustomAttributes().get(CadytsPtPlanChanger.CADYTS_CORRECTION) ;
//								attrs.putAttribute( person.getId().toString() , strPlan  + Integer.toString(cnt) , cadytsCorrection) ;
//							}
//							//System.err.println( " personId: " + person.getId() + " planScore: " + plan.getScore()  + " cadytsCorrection: " + cadytsCorrection ) ; 
//							cnt++ ;
//						}
//					}
//					OutputDirectoryHierarchy hhh = event.getControler().getControlerIO() ;
//					new ObjectAttributesXmlWriter(attrs).writeFile(hhh.getIterationFilename(event.getIteration(), "cadytsCorrections.xml") ) ;					
//				}
//			}
//		}) ;

		controler.run();
	} 

}
