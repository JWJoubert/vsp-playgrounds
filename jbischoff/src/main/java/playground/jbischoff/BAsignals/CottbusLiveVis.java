/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusLiveVis
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
package playground.jbischoff.BAsignals;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.jbischoff.BAsignals.builder.JbSignalBuilder;
import playground.jbischoff.BAsignals.model.AdaptiveControllHead;
import playground.jbischoff.BAsignals.model.CarsOnLaneHandler;


/**
 * @author dgrether
 *
 */
public class CottbusLiveVis {

	private final String config = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-slv/dg_cottbus_config_live_vis.xml";

	
	private void runCottbus() {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		ScenarioImpl scenario = loader.getScenario();
		
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new XY2Links(scenario.getNetwork()));
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalc, timeCostCalc, new AStarLandmarksFactory(scenario.getNetwork(), timeCostCalc)));
		((PopulationImpl)scenario.getPopulation()).runAlgorithms();
		
		EventsManagerImpl events = new EventsManagerImpl();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		QSim qSim = new QSim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			AdaptiveControllHead adaptiveControllHead = new AdaptiveControllHead();
			CarsOnLaneHandler carsOnLaneHandler = new CarsOnLaneHandler();
			carsOnLaneHandler.setAdaptiveControllHead(adaptiveControllHead);
			JbSignalBuilder jbBuilder = new JbSignalBuilder(scenario.getScenarioElement(SignalsData.class), new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events), carsOnLaneHandler);
			SignalEngine engine = new QSimSignalEngine(jbBuilder.createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(qSim);
		qSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		qSim.setControlerIO(controlerIO);
		qSim.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		qSim.run();
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CottbusLiveVis().runCottbus();
	}


}
