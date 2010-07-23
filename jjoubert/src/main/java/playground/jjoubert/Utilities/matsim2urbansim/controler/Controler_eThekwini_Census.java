/* *********************************************************************** *
 * project: org.matsim.*
 * Controler_eThekwini_Census.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim.controler;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;

public class Controler_eThekwini_Census {
	private Config config;
	private static int numberOfIterations;
	private static boolean overwrite;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2){
			throw new IllegalArgumentException("Need number of iterations and logical overwrite argument");
		} else{
			numberOfIterations = Integer.parseInt(args[0]);
			overwrite = Boolean.parseBoolean(args[1]);
		}
		Controler_eThekwini_Census eThekwini = new Controler_eThekwini_Census();
		Controler c = new Controler(eThekwini.config);
		c.setCreateGraphs(true);
		c.setWriteEventsInterval(20);
		c.setOverwriteFiles(overwrite);
		c.run();
	}
	
	public Controler_eThekwini_Census() {
		config = this.setupConfig();
	}
	
	private Config setupConfig(){
		Config config = new Config();
		config.addCoreModules();
		
		// Global.
		config.global().setCoordinateSystem("WGS84_UTM35S");
		config.global().setRandomSeed(1234);
		config.global().setNumberOfThreads(4);
		
		// Network.
		config.network().setInputFile("./input/output_network_100_Emme.xml.gz");
		
		// Plans.
		config.plans().setInputFile("./input/Generated_plans_100.xml.gz");
		
		// Controler.
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(numberOfIterations);
		config.controler().setOutputDirectory("./output/");
		
		// Simulation.
		config.simulation().setStartTime(0);
		config.simulation().setEndTime(86400);
		config.simulation().setSnapshotPeriod(0);
		config.simulation().setSnapshotFormat("googleearth");
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);
		
		// PlanCalcScore
		config.charyparNagelScoring().setLearningRate(1.0);
		config.charyparNagelScoring().setBrainExpBeta(2.0);
		config.charyparNagelScoring().setLateArrival(-18.0);
		config.charyparNagelScoring().setEarlyDeparture(-18.0);
		config.charyparNagelScoring().setPerforming(6.0);
		config.charyparNagelScoring().setTraveling(-6.0);
		//---------------------------------------------------------------------
		ActivityParams home = new ActivityParams("home");
		home.setPriority(1.0);
		home.setMinimalDuration(28800); // 8 hours
		home.setTypicalDuration(43200); // 12 hours
		config.charyparNagelScoring().addActivityParams(home);
		//---------------------------------------------------------------------
		ActivityParams work = new ActivityParams("work");
		work.setPriority(1.0);
		work.setMinimalDuration(25200); // 7 hours
		work.setTypicalDuration(32400); // 9 hours
		work.setOpeningTime(25200);		// 07:00:00
		work.setLatestStartTime(32400); // 09:00:00
		work.setClosingTime(64800);		// 18:00:00
		config.charyparNagelScoring().addActivityParams(work);
		//---------------------------------------------------------------------
		
		// Strategy.
		config.strategy().setMaxAgentPlanMemorySize(5);
		//---------------------------------------------------------------------
		StrategySettings s1 = new StrategySettings(new IdImpl("1"));
		s1.setModuleName("SelectExpBeta");
		s1.setProbability(0.85);
		config.strategy().addStrategySettings(s1);
		//---------------------------------------------------------------------
		StrategySettings s2 = new StrategySettings(new IdImpl("2"));
		s2.setModuleName("ReRoute");
		s2.setProbability(0.15);
		config.strategy().addStrategySettings(s2);
		//---------------------------------------------------------------------
		
		return config;
	}

}
