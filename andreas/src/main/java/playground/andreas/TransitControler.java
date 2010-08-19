/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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

package playground.andreas;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.andreas.fixedHeadway.FixedHeadwayControler;
import playground.andreas.fixedHeadway.FixedHeadwayCycleUmlaufDriverFactory;

/**
 * @author aneumann
 */
public class TransitControler extends Controler {

	private final static Logger log = Logger.getLogger(TransitControler.class);

//	private final static String COUNTS_MODULE_NAME = "ptCounts";

	private boolean useOTFVis = false;
	private boolean useHeadwayControler = true;
	
	public TransitControler(Config config) {
		super(config);
	}
	
	@Override
	protected void runMobSim() {
		
		log.info("Overriding runMobSim()");

		QSim simulation = (QSim) new QSimFactory().createMobsim(this.getScenario(), this.getEvents());

		simulation.getQSimTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());

		if (this.useOTFVis) {
			OTFVisMobsimFeature otfVisQSimFeature = new OTFVisMobsimFeature(simulation);
			otfVisQSimFeature.setVisualizeTeleportedAgents(simulation.getScenario().getConfig().otfVis().isShowTeleportedAgents());
			simulation.addFeature(otfVisQSimFeature);
		}

		if(this.useHeadwayControler){
			simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
			this.events.addHandler(new FixedHeadwayControler(simulation));		
		}

		if (simulation instanceof IOSimulation){
			((IOSimulation)simulation).setControlerIO(this.getControlerIO());
			((IOSimulation)simulation).setIterationNumber(this.getIterationNumber());
		}
		if (simulation instanceof ObservableSimulation){
			for (SimulationListener l : this.getQueueSimulationListener()) {
				((ObservableSimulation)simulation).addQueueSimulationListeners(l);
			}
		}
		simulation.run();
	}

	//	public static class OccupancyAnalyzerListener implements
	//			BeforeMobsimListener, AfterMobsimListener {
	//
	//		private OccupancyAnalyzer occupancyAnalyzer;
	//
	//		public OccupancyAnalyzerListener(OccupancyAnalyzer occupancyAnalyzer) {
	//			this.occupancyAnalyzer = occupancyAnalyzer;
	//		}
	//
	//		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
	//			int iter = event.getIteration();
	//			if (iter % 10 == 0&& iter > event.getControler().getFirstIteration()) {
	//				occupancyAnalyzer.reset(iter);
	//				event.getControler().getEvents().addHandler(occupancyAnalyzer);
	//			}
	//		}
	//
	//		public void notifyAfterMobsim(AfterMobsimEvent event) {
	//			int it = event.getIteration();
	//			if (it % 10 == 0 && it > event.getControler().getFirstIteration()) {
	//				event.getControler().getEvents().removeHandler(occupancyAnalyzer);
	//				occupancyAnalyzer.write(event.getControler().getControlerIO()
	//						.getIterationFilename(it, "occupancyAnalysis.txt"));
	//			}
	//		}
	//
	//	}
	
	//	boolean isUseOTFVis() {
	//		return this.useOTFVis;
	//	}
	
		void setUseOTFVis(boolean useOTFVis) {
			this.useOTFVis = useOTFVis;
		}

	public static void main(final String[] args) {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		TransitControler tc = new TransitControler(config);
		if(args.length > 1 && args[1].equalsIgnoreCase("true")){
			tc.setUseOTFVis(true);
		}
		tc.setOverwriteFiles(true);
//		tc.setCreateGraphs(false);
		tc.run();
	}
}
