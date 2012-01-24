/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DSignalEngine
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.controller;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public class Sim2DSignalEngine implements SignalEngine {

	private static final Logger log = Logger.getLogger(Sim2DSignalEngine.class);
	
	private SignalSystemsManager signalManager;

	public Sim2DSignalEngine(SignalSystemsManager signalManager) {
		this.signalManager = signalManager;
	}

	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.initializeSignalizedItems(((Netsim)e.getQueueSimulation()));
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.signalManager.requestControlUpdate(e.getSimulationTime());
	}

	private void initializeSignalizedItems(Netsim qSim) {
		
		NetsimNetwork net = qSim.getNetsimNetwork();
		for (SignalSystem system : this.signalManager.getSignalSystems().values()){
			for (Signal signal : system.getSignals().values()){
				log.debug("initializing signal " + signal.getId() + " on link " + signal.getLinkId());
//TODO gregor replace with linker to 2DSim Links
			// get the instance that implements SignalizableItem
				//				NetsimLink link = net.getNetsimLinks().get(signal.getLinkId());
//				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
//					QLinkImpl l = (QLinkImpl) link; 
//					l.setSignalized(true);
//					signal.addSignalizeableItem(l);
//				}
//				else {
//					QLinkLanesImpl l = (QLinkLanesImpl) link;
//					for (Id laneId : signal.getLaneIds()){
//						QLane lane = getQLane(laneId, l);
//						lane.setSignalized(true);
//						signal.addSignalizeableItem(lane);
//					}
//				}
			}
			system.simulationInitialized(qSim.getSimTimer().getTimeOfDay());
		}
	}

	
}
