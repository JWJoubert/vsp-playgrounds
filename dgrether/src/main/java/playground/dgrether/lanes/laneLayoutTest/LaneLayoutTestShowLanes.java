/* *********************************************************************** *
 * project: org.matsim.*
 * LaneLayoutTestShowNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.lanes.laneLayoutTest;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQSim;


/**
 * @author dgrether
 *
 */
public class LaneLayoutTestShowLanes {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario sc = new ScenarioImpl();
		sc.getConfig().network().setInputFile(LaneLayoutTestFileNames.NETWORK);
		sc.getConfig().network().setLaneDefinitionsFile(LaneLayoutTestFileNames.LANEDEFINITIONSV2);
		sc.getConfig().scenario().setUseLanes(true);
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		sc.getConfig().otfVis().setLinkWidth(50.0f);
		sc.getConfig().otfVis().setDrawLinkIds(true);
		
		
		ScenarioLoader loader = new ScenarioLoaderImpl(sc);
		loader.loadScenario();
		EventsManagerImpl events = new EventsManagerImpl();
		
		ControlerIO controlerIO = new ControlerIO(sc.getConfig().controler().getOutputDirectory());
		OTFVisQSim queueSimulation = new OTFVisQSim(sc, events);
		queueSimulation.setControlerIO(controlerIO);
		queueSimulation.setIterationNumber(sc.getConfig().controler().getLastIteration());
		queueSimulation.run();
	
	}

}
