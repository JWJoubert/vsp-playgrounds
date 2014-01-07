/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.internalizationCar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * This handler calculates delays (caused by the flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. The marginal congestion event is thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 
 * @author ikaddoura
 *
 */
public abstract class MarginalCongestionHandler implements
	LinkEnterEventHandler,
	LinkLeaveEventHandler,
	TransitDriverStartsEventHandler,
	PersonDepartureEventHandler, 
	PersonStuckEventHandler {
	
	final static Logger log = Logger.getLogger(MarginalCongestionHandler.class);
	
	// If the following parameter is false, a Runtime Exception is thrown in case an agent is delayed by the storage capacity.
	final boolean allowForStorageCapacityConstraint = true;
		
	// If the following parameter is false, the delays resulting from the storage capacity are not internalized.
	final boolean calculateStorageCapacityConstraints = true;
	double delayNotInternalized_storageCapacity = 0.0;

	final ScenarioImpl scenario;
	final EventsManager events;
	final List<Id> ptVehicleIDs = new ArrayList<Id>();
	final Map<Id, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfo>();
	
	double totalInternalizedDelay = 0.0;
	double totalDelay = 0.0;
	double totalStorageDelay = 0.0;
	double delayNotInternalized_roundingErrors = 0.0;
	double delayNotInternalized_spillbackNoCausingAgent = 0.0;
		
	public MarginalCongestionHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
		
		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			throw new RuntimeException("Expecting a capacity period of 1h. Aborting...");
		}
		
		if (this.scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles() != true) {
			throw new RuntimeException("Expecting the qsim to insert waiting vehicles before driving vehicles. Aborting...");
		}
		
		if (this.scenario.getConfig().qsim().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 is not tested.");
		}
		
		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 is not tested.");
		}
		
		if (this.scenario.getConfig().qsim().getStuckTime() < 3600.){
			log.warn("The stuck time is very short. If an agent is moved to the next link even though the next link is full, the calculation of delay effects may be wrong.");
		}
			
		if (this.scenario.getConfig().scenario().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
		this.delayNotInternalized_storageCapacity = 0.0;
		this.totalDelay = 0.0;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.delayNotInternalized_spillbackNoCausingAgent = 0.0;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects: " + event.toString());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		
		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());	
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getVehicleId(), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
		}	
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		
		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			updateTrackingMarginalDelays(event);
			calculateCongestion(event);
			trackMarginalDelay(event);

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.setLastLeavingAgent(event.getVehicleId());
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());
		}
	}
	
	
	// ############################################################################################################################################################
	
	void updateTrackingMarginalDelays(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		if (linkInfo.getLeavingAgents().size() == 0) {
			// No agent is being tracked for that link.
			
		} else {
			// Clear trackings of persons leaving that link previously.
			double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
			double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

			if (event.getTime() > earliestLeaveTime + 1.){
				// Flow congestion has disappeared on that link. Deleting the information of agents previously leaving that link.
				linkInfo.getLeavingAgents().clear();
				linkInfo.getPersonId2linkLeaveTime().clear();
			}
		}
	}
	
	double throwFlowCongestionEventsAndReturnStorageDelay(double totalDelay, LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);
		
		double delayToPayFor = totalDelay;
		for (Id id : reverseList){
			if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
				if (event.getVehicleId().toString().equals(id.toString())) {
					log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
				} else {
					MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowStorageCapacity", id, event.getVehicleId(), linkInfo.getMarginalDelayPerLeavingVehicle_sec(), event.getLinkId());
					this.events.processEvent(congestionEvent);
					this.totalInternalizedDelay = this.totalInternalizedDelay + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				}
				delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				
			} else {
				if (delayToPayFor > 0.) {
					
					if (event.getVehicleId().toString().equals(id.toString())) {
						log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
					} else {
						MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowStorageCapacity", id, event.getVehicleId(), delayToPayFor, event.getLinkId());
						this.events.processEvent(congestionEvent);	
						this.totalInternalizedDelay = this.totalInternalizedDelay + delayToPayFor;

					}
					delayToPayFor = 0.;
				}
			}
		}
		
		if (delayToPayFor <= 1.) {
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += delayToPayFor;
			delayToPayFor = 0.;
		}
		
		return delayToPayFor;
	}
	
	void trackMarginalDelay(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		// Start tracking delays caused by that agent leaving the link.
		
		if (linkInfo.getPersonId2linkLeaveTime().containsKey(event.getVehicleId())){
			log.warn(event.getVehicleId() + " is already being tracked for link " + event.getLinkId() + ". Map 'personId2linkLeaveTime' at time step " + event.getTime() + ":");
			for (Id id : linkInfo.getPersonId2linkLeaveTime().keySet()) {
				log.warn(id + " // " + linkInfo.getPersonId2linkLeaveTime().get(id));
			}
		}
		if (linkInfo.getLeavingAgents().contains(event.getVehicleId())){
			log.warn(event.getVehicleId() + " is already being tracked for link " + event.getLinkId() + " (in List 'leavingAgents').");
		}
		linkInfo.getLeavingAgents().add(event.getVehicleId());
		linkInfo.getPersonId2linkLeaveTime().put(event.getVehicleId(), event.getTime());
	}

	void collectLinkInfos(Id linkId) {
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));
		
		double flowCapacity_hour = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
		double marginalDelay_sec = ((1 / (flowCapacity_hour / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
		
		int storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);
		
		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}
	
	double getLastLeavingTime(Map<Id, Double> personId2LinkLeaveTime) {
		
		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
	}
	
	public void writeCongestionStats(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("total delay [hours]: " + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("total internalized delay [hours]: " + this.totalInternalizedDelay / 3600.);
			bw.newLine();
			bw.write("not internalized delay (rounding errors) [hours]: " + this.delayNotInternalized_roundingErrors / 3600.);
			bw.newLine();
			bw.write("not internalized delay (spill-back related delays without identifiying the causing agent) [hours]: " + this.delayNotInternalized_spillbackNoCausingAgent / 3600.);
			bw.newLine();
			bw.write("not internalized delay (in case delays resulting from the storage capacity are ignored) [hours]: " + this.delayNotInternalized_storageCapacity / 3600.);
			bw.newLine();
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

	public double getTotalDelay() {
		return totalDelay;
	}
		
	public double getDelayNotInternalizedRoundingErrors() {
		return delayNotInternalized_roundingErrors;
	}
	
	public double getDelayNotInternalizedSpillbackNoCausingAgent() {
		return delayNotInternalized_spillbackNoCausingAgent;
	}

	abstract void calculateCongestion(LinkLeaveEvent event);
		
}
