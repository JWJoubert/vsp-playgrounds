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


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;




public class TimeAndDistanceEventHandler implements LinkEnterEventHandler,LinkLeaveEventHandler,
ActivityEndEventHandler,ActivityStartEventHandler{


	private Network network = null;
	private HbefaColdTable hbefaColdTable = null;
	private AnalysisModuleCold coldstartAnalyseModul = null;

	public TimeAndDistanceEventHandler(final Network network,HbefaColdTable hbefaTable,ColdstartAnalyseModul coldstartAnalyseModul ) {
		this.network = network;
		this.hbefaColdTable = hbefaTable;
		this.coldstartAnalyseModul = coldstartAnalyseModul;
	}
	
	
	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> linkleave = new TreeMap<Id, Double>();
	private final Map<Id, Double> activityend = new TreeMap<Id, Double>();
	private final Map<Id, Double> activitystart = new TreeMap<Id, Double>();
	
	private  Map<Id, Double> accumulate = new TreeMap<Id, Double>();
	
	private  Map<Id, Double> startTime = new TreeMap<Id, Double>();

	
	public Map<Id,Map<Integer,DistanceObject>> coldDistance  =
		new TreeMap<Id,Map<Integer,DistanceObject>>();

	public Map<Id,Map<Integer,ParkingTimeObject>> parkingTime  =
		new TreeMap<Id,Map<Integer,ParkingTimeObject>>();

	public void reset(int iteration) {

		this.linkenter.clear();
		this.activityend.clear();
		this.activitystart.clear();
		System.out.println("reset...");
	}
	
	public void handleEvent(LinkEnterEvent event) {

		this.linkenter.put(event.getPersonId(), event.getTime());}

	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {

		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		
		this.activitystart.put(event.getPersonId(), event.getTime());
		
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		double distance = link.getLength();
		
			
		if (this.accumulate.containsKey(personId) && this.startTime.containsKey(personId)){
		
			
			double actEnd = this.activityend.get(personId);// EndTime
			double actDuration =  actEnd - this.startTime.get(personId);
			try {
				double TotalDistance =  this.accumulate.get(personId) + distance;
				this.coldstartAnalyseModul.calculateColdEmissionsPerLink(personId, actDuration, TotalDistance, this.hbefaColdTable);
				//System.out.println("personId "+ personId + " actEnd " +actEnd+ " actDuration " + actDuration+ " Distance " + TotalDistance );
				this.accumulate.remove(personId);
			} catch (IOException e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			

		}else {
			
			this.accumulate.put(personId, distance);
			this.startTime.put(personId, event.getTime());
			}	
		
	}
	
	
	public void handleEvent(ActivityEndEvent  event) {
	
		this.activityend.put(event.getPersonId(), event.getTime());

	}
	
	
	public void handleEvent(LinkLeaveEvent event) {	
	  
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
					
		this.linkleave.put(event.getPersonId(), event.getTime());
				
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		double distance = link.getLength();
		
		if (this.accumulate.get(personId) != null){
			
			double oldValue = this.accumulate.get(personId);
			this.accumulate.put(personId, oldValue + distance);
			//System.out.println(personId);
			//String id = event.getPersonId().toString();
		//	if(id.contains("557568.2#8771"))
				
			
			//System.out.println(event.getLinkId()+"   "+distance);
		
		}else {
			this.accumulate.put(personId, distance);
		}
	}
}
	

	
	

