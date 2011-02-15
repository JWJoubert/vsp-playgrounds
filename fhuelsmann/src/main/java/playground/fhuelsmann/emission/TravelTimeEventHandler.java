package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;

public class TravelTimeEventHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, 
AgentArrivalEventHandler,AgentDepartureEventHandler {

	private final Network network;
	private  HbefaObject[][] HbefaTable;

	private LinkAndAgentAccountAnalyseModul linkAndAgentAccountAnalyseModul = new LinkAndAgentAccountAnalyseModul();

	public LinkAndAgentAccountAnalyseModul getLinkAndAgentAccountAnalyseModul() {
		return linkAndAgentAccountAnalyseModul;
	}

	public void setLinkAndAgentAccountAnalyseModul(LinkAndAgentAccountAnalyseModul linkAndAgentAccountAnalyseModul) {
		this.linkAndAgentAccountAnalyseModul = linkAndAgentAccountAnalyseModul;
	}

	public TravelTimeEventHandler(final Network network,HbefaObject[][] HbefaTable) {
		this.HbefaTable = HbefaTable;
		this.network = network;
	}

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();

	public void reset(int iteration) {

	}

	public void handleEvent(LinkEnterEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//		if (onelink.equals(new IdImpl("590000822"))){
		//			System.out.println(onelink);
		//			if(id.contains("testVehicle")){
		this.linkenter.put(event.getPersonId(), event.getTime());
		//			}
	}
	//	}

	public void handleEvent(AgentArrivalEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//		if (onelink == new IdImpl("590000822")){
		//		if(id.contains("testVehicle")){
		this.agentarrival.put(event.getPersonId(), event.getTime());
		//		}
	}
	//	}

	public void handleEvent(AgentDepartureEvent event) {
		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//			if (onelink == new IdImpl("590000822")){
		//				if(id.contains("testVehicle")){
		this.agentdeparture.put(event.getPersonId(), event.getTime());
		//				}
	}
	//	}

	public void handleEvent(LinkLeaveEvent event) {	

		//		String id = event.getPersonId().toString();
		//		Id onelink = event.getLinkId();
		//			if (onelink.equals(new IdImpl("590000822"))){
		//				if(id.contains("testVehicle")){
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		Double distance = link.getLength();
		Double freeVelocity = link.getFreespeed();

		String roadTypeString = link.getType();
		Integer roadType = null;
		try{
			roadType = Integer.parseInt(roadTypeString);
		}
		catch (NumberFormatException e){
			System.err.println("Error: roadtype missing");
		}

		if (this.linkenter.containsKey(event.getPersonId())) {						
			// link with activity
			if (this.agentarrival.containsKey(personId)) {
				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);

				double travelTime = event.getTime() - enterTime - departureTime + arrivalTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				this.agentarrival.remove(personId);

				linkAndAgentAccountAnalyseModul.calculateEmissionsPerLink(travelTime, linkId, averageSpeed,roadType, freeVelocity, distance, HbefaTable);	
				linkAndAgentAccountAnalyseModul.calculateEmissionsPerPerson(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, HbefaTable);	
			}
			// if (this.agentarrival.containsKey(personId)) is not the case (link without activity)
			else {
				double enterTime = this.linkenter.get(personId);
				double travelTime = event.getTime() - enterTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				linkAndAgentAccountAnalyseModul.calculateEmissionsPerLink(travelTime, linkId, averageSpeed,roadType, freeVelocity, distance, HbefaTable);	
				linkAndAgentAccountAnalyseModul.calculateEmissionsPerPerson(travelTime, personId, averageSpeed,roadType, freeVelocity, distance, HbefaTable);
			}
		}
		//		}
	}
}
