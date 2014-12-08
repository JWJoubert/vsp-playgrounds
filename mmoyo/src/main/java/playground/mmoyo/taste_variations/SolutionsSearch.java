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

package playground.mmoyo.taste_variations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.core.api.experimental.events.EventsManager;
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
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.randomizerPtRouter.RndPtRouterFactory;

/**
 * Another launcher for svd values search
 * 1) It invokes randomized router from iteration 0 to n
 * 2) It launches BRUTE FORCE calibration from iteration n until last iteration
 * 3) It calculates svd values from last iteration output plan  
 *      preferably nullify scores in config file!  for optimal brute force
 */ 
public class SolutionsSearch {

	
	public static void main(String[] args) {
		String configFile;
		final Integer rndRouterIterations;
		final double cadytsScoringWeight;
		String strDoStzopZoneConversion;
		
		if (args.length > 0){
			configFile = args[0];
			rndRouterIterations = Integer.valueOf (args[1]);
			cadytsScoringWeight = Double.parseDouble(args[2]);
			strDoStzopZoneConversion = args[3];
		}else {
			configFile = "../../ptManuel/calibration/my_config.xml";
			rndRouterIterations =20;
			cadytsScoringWeight = 1.0;
			strDoStzopZoneConversion = "false";
		}

		final Config config = ConfigUtils.loadConfig(configFile);
		Scenario scn = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true);
		final TransitSchedule schedule = scn.getTransitSchedule();
		final boolean doStopZoneConversion = Boolean.parseBoolean(strDoStzopZoneConversion);
		strDoStzopZoneConversion= null;
		configFile = null;

		/////CONFIGURE STRATEGIES//////////////////////////////////////////////////////////////////////////////
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
	
		////////  Randomized router ///
		{//////  set randomized router strategy/////////// 
		StrategySettings stratSets = new StrategySettings(Id.create(++lastStrategyIdx, StrategySettings.class));
		stratSets.setStrategyName("ReRoute");
		stratSets.setProbability(1.0);
		stratSets.setDisableAfter(20);
		config.strategy().addStrategySettings(stratSets);
		}
		//create and set randomized router factory
		final TransitRouterConfig trConfig = new TransitRouterConfig( config ) ;
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(schedule);
		RndPtRouterFactory rndPtRouterFactory = new RndPtRouterFactory();
		TransitRouterFactory randomizedTransitRouterFactory = rndPtRouterFactory.createFactory (preparedSchedule, trConfig, routerNetwork, false, false);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
		
		
		{//////  Cadyts as plan selector//////////////// 
		StrategySettings stratSets2 = new StrategySettings(Id.create(++lastStrategyIdx, StrategySettings.class));
		stratSets2.setStrategyName("myCadyts");
		stratSets2.setProbability(1.0);
		config.strategy().addStrategySettings(stratSets2);
		}
		//create cadyts context
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		ccc.setPreparatoryIterations(rndRouterIterations);
		ccc.setUseBruteForce(true);
		final CadytsPtContext cContext = new CadytsPtContext( config, controler.getEvents()  ) ;
		controler.addControlerListener(cContext) ;

		//set cadyts as strategy for plan selector
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			
			@Override   
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2, cContext);
				// planSelector.setCadytsWeight(0.0) ;    // <-set it to zero if only cadyts scores are desired
				return new PlanStrategyImpl(planSelector);
			}
		} ) ;
		

		//Cadyts as scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(),config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
				return scoringFunctionAccumulator;
			}
		}) ;
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doStopZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);

		//add a svd calculator as control listener to get svd values from final outputplans
		LeastSquareSolutionCalculatorFromScoreLastIteration svdCalculatorListener = new LeastSquareSolutionCalculatorFromScoreLastIteration();
		controler.addControlerListener(svdCalculatorListener);
		
		controler.run();
	}

}
