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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;




public class ColdEmissionHandler implements LinkEnterEventHandler,LinkLeaveEventHandler,
ActivityEndEventHandler,ActivityStartEventHandler,AgentDepartureEventHandler{


	private Network network = null;
	private HbefaColdEmissionTable hbefaColdTable = null;
	private AnalysisModuleCold coldstartAnalyseModul = null;

	public ColdEmissionHandler(final Network network,HbefaColdEmissionTable hbefaTable,ColdEmissionAnalysisModule coldEmissionAnalysisModule ) {
		this.network = network;
		this.hbefaColdTable = hbefaTable;
		this.coldstartAnalyseModul = coldEmissionAnalysisModule;
	}
	
	
	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> linkleave = new TreeMap<Id, Double>();
	private final Map<Id, Double> activityend = new TreeMap<Id, Double>();
	private final Map<Id, Double> activitystart = new TreeMap<Id, Double>();
	
	private  Map<Id, Double> accumulate = new TreeMap<Id, Double>();
	private  Map<Id, Double> activityDuration = new TreeMap<Id, Double>();


	
	

	public void reset(int iteration) {

		this.linkenter.clear();
		this.activityend.clear();
		this.activitystart.clear();
		System.out.println("reset...");
	}
	
	public void handleEvent(LinkEnterEvent event) {

		this.linkenter.put(event.getPersonId(), event.getTime());}

	
	public void handleEvent(AgentDepartureEvent event) {
				Id personId= event.getPersonId();
				Id linkId = event.getLinkId();
				if (event.getLegMode().equals("pt")|| event.getLegMode().equals("walk")|| event.getLegMode().equals("bike"))	{
					//System.out.println("+++++++personId "+personId+" leg "+ event.getLegMode());
					coldstartAnalyseModul.calculatePerPersonPtBikeWalk(personId, linkId,this.hbefaColdTable);

					}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {


		Id personId= event.getPersonId();	
		Id linkId = event.getLinkId();
		
		this.activitystart.put(event.getPersonId(), event.getTime());
					
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		double distance = link.getLength();
	

		if (this.accumulate.containsKey(personId) && this.activityDuration.containsKey(personId)){

			try {
				double totalDistance =  this.accumulate.get(personId);//without Distance of LinkID of startact; one link is counted too much
				double actDuration = this.activityDuration.get(personId);
				
//				String id = event.getPersonId().toString();
//				if(id.contains("569253.3#11147"))	
//				System.out.println("TotalDistance " +TotalDistance + " actDuration " + actDuration);
				
				this.coldstartAnalyseModul.calculateColdEmissionsPerLink(personId, actDuration, totalDistance, this.hbefaColdTable);
//				System.out.println("personId "+ personId + " actStart " +actStart+ " actDuration " + actDuration+ " Distance " + TotalDistance );
				
				this.accumulate.remove(personId);
			
			} catch (Exception e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			
		}
		else {
			// System.out.println("count1    "+count1++);

			this.accumulate.put(personId, distance);
			this.activityDuration.put(personId, event.getTime());
			
			double TotalDistance =  this.accumulate.get(personId);//without Distance of LinkID of startact; one link is counted too much
			double actDuration = this.activityDuration.get(personId);
			
			try {
				this.coldstartAnalyseModul.calculateColdEmissionsPerLink(personId, actDuration, TotalDistance, this.hbefaColdTable);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
	}
	
	
	public void handleEvent(ActivityEndEvent  event) {
		
		Id personId= event.getPersonId();
	
		this.activityend.put(event.getPersonId(), event.getTime());
		
		if (this.activityDuration.containsKey(personId)){
		double actstart = this.activitystart.get(personId);// EndTime
		double actDuration = event.getTime() - actstart;
		this.activityDuration.put(personId, actDuration);
		
//		String id = event.getPersonId().toString();
//		if(id.contains("569253.3#11147"))	
//			System.out.println("startTime" +event.getTime()+"actDuration" + actDuration);
		}
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
			
//		String id = event.getPersonId().toString();
//		if(id.contains("569253.3#11147"))		
//			System.out.println(event.getLinkId()+"   "+distance);
		
		}
		else {
			this.accumulate.put(personId, distance);
		}
	}
}
	

	
	

