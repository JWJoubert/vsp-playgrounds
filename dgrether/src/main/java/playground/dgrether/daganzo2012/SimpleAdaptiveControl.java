/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAdaptiveControl
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
package playground.dgrether.daganzo2012;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class SimpleAdaptiveControl implements MobsimEngine, LinkEnterEventHandler, LinkLeaveEventHandler {

	private Queue<Double> vehicleExitTimesOnLink5 = new LinkedList<Double>() ;
	
	private InternalInterface internalInterface;
	private Id id5 = new IdImpl("5");

	private SignalizeableItem link4;

	private SignalizeableItem link5;

	private double initialRedOn4 = 0;
	
	@Override
	public void doSimStep(double time) {
		//324 first agent reaches the end of link 4
		if (time < this.initialRedOn4){
			link4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
			link5.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
			return;
		}
		
		final Double vehTime = this.vehicleExitTimesOnLink5.peek();
		if (vehTime != null && vehTime < time) {
			link4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
			link5.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
		} 
		else {
			link4.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
			link5.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
		}
			
	}


	@Override
	public void onPrepareSim() {
		link4 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("4")) ;
		link5 = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("5")) ;
		link4.setSignalized(true);
		link5.setSignalized(true);
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;		
	}
	
	public Netsim getMobsim() {
		return this.internalInterface.getMobsim();
	}

	@Override
	public void reset(int iteration) {
		this.vehicleExitTimesOnLink5.clear();
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (id5.equals(event.getLinkId())){
			this.vehicleExitTimesOnLink5.add(event.getTime() + 100.0);
		}
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
			if (id5.equals(event.getLinkId())){
				this.vehicleExitTimesOnLink5.poll();
			}
	}


	public void setInitialRedOn4(double initialRed) {
		this.initialRedOn4  = initialRed;
	}

}
