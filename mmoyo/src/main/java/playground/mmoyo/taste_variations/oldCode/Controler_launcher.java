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

package playground.mmoyo.taste_variations.oldCode;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.taste_variations.CadytsUtlCorrectionsCollecter;

public class Controler_launcher {
	static final String TB = "\t";
	static final String NL = "\n";

	public static void main(String[] args)  {
		String configFile;
		final double cadytsScoringWeight;
		String strDoStzopZoneConversion;
		
		if (args.length > 0){
			configFile = args[0];
			cadytsScoringWeight = Double.parseDouble(args[1]);
			strDoStzopZoneConversion = args[2];
		}else {
			configFile = "../../ptManuel/calibration/my_config.xml";
			cadytsScoringWeight = 1.0;
			strDoStzopZoneConversion = "false";
		}

		final Config config = ConfigUtils.loadConfig(configFile);
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		final Network net = controler.getNetwork();
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		final boolean doStopZoneConversion = Boolean.parseBoolean(strDoStzopZoneConversion);
		strDoStzopZoneConversion= null;
		configFile = null;

		//////  CONFIGURE CADYTS//////////////////////////////////////////////////////
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}
		{ 
		StrategySettings stratSets = new StrategySettings(new IdImpl(lastStrategyIdx+1));
		stratSets.setModuleName("myCadyts");
		stratSets.setProbability(1.0);
		config.strategy().addStrategySettings(stratSets);
		}
		//create cadyts context
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;
		final CadytsPtContext cContext = new CadytsPtContext( config, controler.getEvents() ) ;
		
		controler.addControlerListener(cContext) ;
		//set cadyts as strategy for plan selector
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2, cContext);
				//planSelector.setCadytsWeight(0.0) ;
				return new PlanStrategyImpl(planSelector);
			}
			
		} ) ;
		
		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		//final boolean useBruteForce =Boolean.parseBoolean(config.getParam("cadytsPt", "useBruteForce")); 
		CadytsConfigGroup cptcg = (CadytsConfigGroup) controler.getConfig().getModule(CadytsConfigGroup.GROUP_NAME);
		final boolean useBruteForce = Boolean.parseBoolean(cptcg.getParams().get("useBruteForce"));
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				
				if (!useBruteForce){
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, net));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));	
				}
				
				final CadytsScoring scoringFunction = new CadytsScoring(plan,config, cContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
	 
				return scoringFunctionAccumulator;
			}
			
		}) ;
		
		//add svd calculator as control listener
		CadytsUtlCorrectionsCollecter utilCollecter = new CadytsUtlCorrectionsCollecter(net, schedule);
		controler.addControlerListener(utilCollecter);
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doStopZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);

		controler.run();
	}
}
