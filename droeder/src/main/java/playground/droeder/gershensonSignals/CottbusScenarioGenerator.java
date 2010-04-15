/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.gershensonSignals;

import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsFactory;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class CottbusScenarioGenerator {
	
		private static final String INPUT = DaPaths.DGSTUDIES + "cottbus/";
		private static final String OUTPUT =DaPaths.DASTUDIES + "cottbus/";
		//INPUT
		private static final String NETWORKFILE = INPUT + "network.xml";
		private static final String LANESINPUTFILE = INPUT + "laneDefinitions.xml";
		private static final String SIGNALSYSTEMINPUTFILE = OUTPUT + "signalSystemsByNodes.xml";
		private static final String PLANSINPUTFILE = INPUT + "plans.xml"; 
		
		// OUTPUT
		private static final String CHANGEEVENTSFILE = OUTPUT +"changeEventsFile.xml";
		public static final String CONFIGOUTPUTFILE = OUTPUT + "cottbusConfig.xml";
		private static final String SIGNALSYSTEMCONFIG = OUTPUT + "signalSystemConfig.xml";
		private static final String OUTPUTDIRECTORY = DaPaths.OUTPUT + "cottbus/" ;
		
		// DEFINITIONS
		protected static String controllerClass = DaAdaptiveController.class.getCanonicalName();
		private static final int iterations = 1;
		Id id1 = new IdImpl("1");
		Id id2 = new IdImpl("2");
	
	public void createScenario(){
		ScenarioImpl sc = new ScenarioImpl();
		
		Config conf = sc.getConfig();
		
		// set Network and Lanes
		conf.network().setInputFile(NETWORKFILE);
		conf.scenario().setUseLanes(true);
		conf.network().setLaneDefinitionsFile(LANESINPUTFILE);
		
		
		// set plans
		conf.plans().setInputFile(PLANSINPUTFILE);
		
		//set Signalsystems
		conf.scenario().setUseSignalSystems(true);
		conf.signalSystems().setSignalSystemFile(SIGNALSYSTEMINPUTFILE);
		
		//create and write SignalSystemConfig
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(sc);
		loader.loadScenario();
		createSignalSystemsConfig(sc);
		conf.signalSystems().setSignalSystemConfigFile(SIGNALSYSTEMCONFIG);
		
		//create changeEvents
//		this.createChangeEvents(sc);
		
		//create and write config
		createConfig(conf);
		new ConfigWriter(conf).writeFile(CONFIGOUTPUTFILE);
	}
	
	private void createSignalSystemsConfig (ScenarioImpl sc){
		SignalSystemConfigurations configs = sc.getSignalSystemConfigurations();
		SignalSystemConfigurationsFactory factory = configs.getFactory();
		
		for (Entry<Id, SignalSystemDefinition> e : sc.getSignalSystems().getSignalSystemDefinitions().entrySet()){
			SignalSystemConfiguration systemConfig = factory.createSignalSystemConfiguration(e.getKey());
			AdaptiveSignalSystemControlInfo controlInfo = factory.createAdaptiveSignalSystemControlInfo();
			for (SignalGroupDefinition sd: sc.getSignalSystems().getSignalGroupDefinitions().values()){
				if(sd.getSignalSystemDefinitionId().equals(e.getKey())){
					controlInfo.addSignalGroupId(sd.getId());
				}
			}
			controlInfo.setAdaptiveControlerClass(controllerClass);
			systemConfig.setSignalSystemControlInfo(controlInfo);
			configs.getSignalSystemConfigurations().put(systemConfig.getSignalSystemId(), systemConfig);
		}
		
		SignalSystemConfigurations ssConfigs = configs;
		MatsimSignalSystemConfigurationsWriter ssConfigsWriter = new MatsimSignalSystemConfigurationsWriter(ssConfigs);	
		ssConfigsWriter.writeFile(SIGNALSYSTEMCONFIG);
	}
	
private void createConfig(Config config) {
		
		config.network().setInputFile(NETWORKFILE);
		config.network().setChangeEventInputFile(CHANGEEVENTSFILE);
		config.network().setTimeVariantNetwork(false);
		config.plans().setInputFile(PLANSINPUTFILE);
	

		// configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(0.0);

		// set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0",
		"16:00:00");
		config.charyparNagelScoring().addParam("activityType_1", "w");
		config.charyparNagelScoring().addParam("activityTypicalDuration_1",
				"08:00:00");

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(1);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(OUTPUTDIRECTORY);
		config.controler().setLinkToLinkRoutingEnabled(true);
		config.controler().setWriteEventsInterval(0);
	


		// configure simulation and snapshot writing
		config.setQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotFormat("otfvis");
		config.getQSimConfigGroup().setSnapshotFile("cmcf.mvi");
		config.getQSimConfigGroup().setSnapshotPeriod(60.0);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		config.getQSimConfigGroup().setStuckTime(20000);
		config.getQSimConfigGroup().setRemoveStuckVehicles(true);
//		config.getQSimConfigGroup().setStartTime(6*3600);
		config.getQSimConfigGroup().setEndTime(24*3600);
		config.otfVis().setDrawLinkIds(true);
		
		
		// configure strategies for replanning
		
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(id1);
		selectExp.setProbability(0.9);
		selectExp.setModuleName("ChangeExpBeta");
		config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(id2);
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		config.strategy().addStrategySettings(reRoute);
	}

public static void main(final String[] args) {
	try {
		new CottbusScenarioGenerator().createScenario();
	} catch (Exception e) {
		e.printStackTrace();
	}

}

}
