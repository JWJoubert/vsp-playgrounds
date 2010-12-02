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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;



public class FourWaysVisNoLanes {

	public static final String TESTINPUTDIR = "../../matsim/test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {


    String netFile = TESTINPUTDIR + "network.xml.gz";
    String popFile = TESTINPUTDIR + "plans.xml.gz";
    
    
    ScenarioImpl scenario = new ScenarioImpl();
    scenario.getConfig().network().setInputFile(netFile);
    scenario.getConfig().plans().setInputFile(popFile);
    scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
    scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
    scenario.getConfig().getQSimConfigGroup().setStuckTime(100.0);
    
    
    ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
    loader.loadScenario();
    
    EventsManager events = new EventsManagerImpl();
    QSim otfVisQSim = new QSim(scenario, events);
    OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(otfVisQSim);
    otfVisQSim.addFeature(queueSimulationFeature);
    queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
    
    QSim client = otfVisQSim;
    client.run();
	}

}
