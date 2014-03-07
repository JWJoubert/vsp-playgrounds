/* *********************************************************************** *
 * project: org.matsim.*
 * FourWaysVis
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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;


public class FourWaysVis {

	public static final String TESTINPUTDIR = "../../../matsim/trunk/src/test/resources/test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = TESTINPUTDIR + "network.xml.gz";
		String lanesFile  = TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		String lanesFile20  = TESTINPUTDIR + "testLaneDefinitions_v2.0.xml";
		String popFile = TESTINPUTDIR + "plans.xml.gz";
		String signalFile = TESTINPUTDIR + "testSignalSystems_v2.0.xml";
		String signalGroupsFile = TESTINPUTDIR + "testSignalGroups_v2.0.xml";
		String signalControlFile = TESTINPUTDIR + "testSignalControl_v2.0.xml";
		
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(popFile);
		config.qsim().setSnapshotStyle("queue");
		config.qsim().setStuckTime(100.0);
		
		
		config.signalSystems().setSignalSystemFile(signalFile);
		config.signalSystems().setSignalGroupsFile(signalGroupsFile);
		config.signalSystems().setSignalControlFile(signalControlFile);
		config.scenario().setUseSignalSystems(true);
		
		OTFVisConfigGroup otfconfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfconfig.setAgentSize(130.0f);

		
//		LaneDefinitions laneDefinitions = new LaneDefinitionsImpl();
//		LaneDefinitionsReader11 reader = new LaneDefinitionsReader11(laneDefinitions, MatsimLaneDefinitionsReader.SCHEMALOCATIONV11);
//		reader.readFile(lanesFile);
		LaneDefinitonsV11ToV20Converter converter = new LaneDefinitonsV11ToV20Converter();
		converter.convert(lanesFile, lanesFile20, netFile);

		config.network().setLaneDefinitionsFile(lanesFile20);
		config.scenario().setUseLanes(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager events = EventsUtils.createEventsManager();
		
//		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(config.signalSystems());
//		SignalsData signalsData = signalsLoader.loadSignalsData();
//		scenario.addScenarioElement(SignalsData.ELEMENT_NAME , signalsData);
		FromDataBuilder builder = new FromDataBuilder(scenario, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		
		QSim otfVisQSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		//		client.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
//		client.setLaneDefinitions(scenario.getLaneDefinitions());
//		client.setSignalSystems(scenario.getSignalSystems(), scenario.getSignalSystemConfigurations());
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, scenario, events, otfVisQSim);
		OTFClientLive.run(config, server);
		
		otfVisQSim.run();
		
		
	}

}
