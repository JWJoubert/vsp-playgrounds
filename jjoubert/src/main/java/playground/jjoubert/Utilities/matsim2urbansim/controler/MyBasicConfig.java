/* *********************************************************************** *
 * project: org.matsim.*
 * MyBasicConfig.java
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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public class MyBasicConfig {
	private Config config;

	public MyBasicConfig() {
		config = new Config();
		config.addCoreModules();

		// Global.
		config.global().setCoordinateSystem("WGS84");
		config.global().setRandomSeed(1234);
		config.global().setNumberOfThreads(2);
		
		

		// Network.
		config.network().setInputFile("./input/output_network_100_Emme.xml.gz");

		// Plans.
		config.plans().setInputFile("./input/Generated_plans_100.xml.gz");

		// Controler.
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		config.controler().setLinkToLinkRoutingEnabled(false);
		config.controler().setWriteEventsInterval(20);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
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
		s1.setProbability(0.80);
		config.strategy().addStrategySettings(s1);
		//---------------------------------------------------------------------
		StrategySettings s2 = new StrategySettings(new IdImpl("2"));
		s2.setModuleName("ReRoute");
		s2.setProbability(0.10);
		config.strategy().addStrategySettings(s2);
		//---------------------------------------------------------------------
		StrategySettings s3 = new StrategySettings(new IdImpl("3"));
		s3.setModuleName("TimeAllocationMutator");
		s3.setProbability(0.10);
		config.strategy().addStrategySettings(s3);
		//---------------------------------------------------------------------

		
		// Parallel QSim
//		QSimConfigGroup qsim = new QSimConfigGroup();
//		qsim.setNumberOfThreads(2);
//		qsim.setSnapshotFormat("googleearth");
//		qsim.setSnapshotPeriod(900);
//		config.setQSimConfigGroup(qsim);
	}
	
	public Config getConfig(){
		return this.config;
	}
	
}


