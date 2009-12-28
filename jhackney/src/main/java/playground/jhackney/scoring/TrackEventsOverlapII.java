package playground.jhackney.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 * EventOverlap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.handler.DeprecatedActivityEndEventHandler;
import org.matsim.core.events.handler.DeprecatedActivityStartEventHandler;
import org.matsim.core.population.PersonImpl;

import playground.jhackney.socialnetworks.mentalmap.TimeWindow;


/**
 * Calculates overlapping between the selected plans of a given population
 * based on events.<br>
 *
 * @author jhackney
 */
public class TrackEventsOverlapII implements DeprecatedActivityStartEventHandler, DeprecatedActivityEndEventHandler {

	LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>> timeWindowMap=new LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>>();
	LinkedHashMap<Activity,Double> startMap = new LinkedHashMap<Activity,Double>();
	LinkedHashMap<Activity,Double> endMap = new LinkedHashMap<Activity,Double>();

	static final private Logger log = Logger.getLogger(TrackEventsOverlapII.class);

	public TrackEventsOverlapII() {
		super();
//		makeTimeWindows();
		log.info(" Looking through plans and mapping social interactions for scoring");
	}


	public void handleEvent(final ActivityEndEventImpl event) {

		double eventStartTime=0;// event start time is unknown
		double eventEndTime=event.getTime();

		if(startMap!=null && startMap.size()>0){
			if(startMap.get(event.getAct())!=null){//TODO probably not necessary
				eventStartTime=startMap.get(event.getAct());
			}
		}
		// Make a TimeWindow and add to TimeWindowMap
		if(eventStartTime>=0){// TODO probably wrong
			PersonImpl agent = event.getPerson();

			ActivityFacility facility = event.getAct().getFacility();
			if(this.timeWindowMap.containsKey(facility)){
				ArrayList<TimeWindow> windowList=timeWindowMap.get(facility);
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.getAct()));
				timeWindowMap.remove(facility);
				timeWindowMap.put(facility, windowList);
			}else{
				ArrayList<TimeWindow> windowList= new ArrayList<TimeWindow>();
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.getAct()));
				timeWindowMap.put(facility, windowList);
			}
		}else{
			//do nothing immediately if there is no start event, just save this end event for later
			endMap.put(event.getAct(),event.getTime());
		}
	}

	public void handleEvent(final ActivityStartEventImpl event) {

		double eventStartTime=event.getTime();
		double eventEndTime=-999;// the event end time is not known
		if(endMap!=null && endMap.size()>0){
			if(endMap.get(event.getAct())!=null){
				eventEndTime=endMap.get(event.getAct());
			}
		}else{
			eventEndTime=3600.*30;
		}
		if(eventEndTime>=0){// if a valid end time is found, make a timeWindow and add to Map
			PersonImpl agent = event.getPerson();

			ActivityFacility facility = event.getAct().getFacility();
			if(this.timeWindowMap.containsKey(facility)){
				ArrayList<TimeWindow> windowList=timeWindowMap.get(facility);
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.getAct()));
				timeWindowMap.remove(facility);
				timeWindowMap.put(facility, windowList);
			}else{
				ArrayList<TimeWindow> windowList= new ArrayList<TimeWindow>();
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.getAct()));
				timeWindowMap.put(facility, windowList);
			}
		}else{
			// if the event is not complete, save the start information for later
			startMap.put(event.getAct(),event.getTime());
		}
	}


	public void reset(final int iteration) {
//		this.timeWindowMap.clear();
	}
	public void clearTimeWindowMap(){
		this.timeWindowMap.clear();
	}

	public LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>> getTimeWindowMap(){
		return this.timeWindowMap;
	}

}

