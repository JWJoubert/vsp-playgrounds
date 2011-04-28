/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips;

import java.util.ArrayList;
import java.util.ListIterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class AnalysisTrip {
	private String mode = null;
	private Coordinate start;
	private Coordinate end;
	private ArrayList<PersonEvent> events;
	
	//all modes
	private Double tripTTime = 0.0;
	
	// pt only
	private int accesWalkCnt = 0;
	private int accesWaitCnt = 0;
	private int egressWalkCnt = 0;
	private int switchWalkCnt= 0;
	private int switchWaitCnt = 0;
	private int lineCnt = 0;
	
	private double accesWalkTTime = 0.0;
	private double accesWaitTime = 0.0;
	private double egressWalkTTime = 0.0;
	private double switchWalkTTime = 0.0;
	private double switchWaitTime = 0.0;
	private double lineTTime = 0.0;
	
	
	public AnalysisTrip(ArrayList<PersonEvent> events, ArrayList<PlanElement> elements){
		this.events = events;
		
		//if no zones in TripSet are defined, coords not necessary
		if(!(((Activity) elements.get(0)).getCoord() == null) && !(((Activity) elements.get(elements.size() - 1)).getCoord() == null)){
			this.start = new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
					((Activity) elements.get(0)).getCoord().getY());
			this.end = new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
					((Activity) elements.get(elements.size() - 1)).getCoord().getY());
		}
		this.mode = this.findMode(elements);
		this.analyze();
	}
	
	public String getMode(){
		return this.mode;
	}
	
	// not essential but good to prevent mixing up different modes
	private String findMode(ArrayList<PlanElement> elements) {
		String mode = null;
		for(PlanElement p : elements){
			if(p instanceof Leg){
				if(((Leg) p).getMode().equals(TransportMode.transit_walk)){
					mode = TransportMode.transit_walk;
				}else{
					return ((Leg) p).getMode();
				}
			}
		}
		return mode;
	}

	private void analyze(){
		this.analyzeValuesForAllModes();
		if(this.mode.equals(TransportMode.pt)){
			this.analyzeValuesForPT();
		}
	}
	
	private void analyzeValuesForAllModes() {
		// from first departure to last arrival
		tripTTime = events.get(events.size() - 2).getTime() - events.get(1).getTime();
	}

	
	private void analyzeValuesForPT() {
		ListIterator<PersonEvent> it = this.events.listIterator();
		PersonEvent pe;
		double switchWait = 0;
		
		while(it.hasNext()){
			pe = it.next();
		
			if(pe instanceof ActivityEndEvent){
				if(it.previousIndex() == 4){
					accesWaitTime += pe.getTime();
					if(accesWaitTime > 0) accesWaitCnt++;
				}
				
				// only pt interactions between 2 pt legs are registered for waitTime
				else if((it.previousIndex() > 6) && (it.previousIndex() < events.size() - 7)){
					switchWait += pe.getTime();
					if(switchWait > 0) switchWaitCnt++;
					switchWaitTime += switchWait;
					switchWait = 0;
				}
			}
			
			else if(pe instanceof AgentDepartureEvent){
				// every pt leg is a new line
				if(((AgentDepartureEvent) pe).getLegMode().equals(TransportMode.pt)){
					lineCnt++;
					lineTTime -= pe.getTime();
				}
				
				// deside if transit_walk is used for access, egress or switch
				if(((AgentDepartureEvent) pe).getLegMode().equals(TransportMode.transit_walk)){
					if(it.previousIndex() == 1){
						accesWalkTTime -= pe.getTime();
						accesWalkCnt++;
					}else if(it.previousIndex() == events.size() - 3){
						egressWalkTTime -= pe.getTime();
						egressWalkCnt++;
					}else{
						switchWalkTTime -= pe.getTime();
						switchWalkCnt++;
					}
				}
			}
			
			else if(pe instanceof AgentArrivalEvent){
				
				if(((AgentArrivalEvent) pe).getLegMode().equals(TransportMode.pt)){
					lineTTime += pe.getTime();
				}

				// see at departure
				if(((AgentArrivalEvent) pe).getLegMode().equals(TransportMode.transit_walk)){
					if(it.previousIndex() == 2){
						accesWalkTTime += pe.getTime();
					}else if(it.previousIndex() == events.size() - 2){
						egressWalkTTime += pe.getTime();
					}else{
						switchWalkTTime += pe.getTime();
					}
				}
				
			}
			
			else if(pe instanceof ActivityStartEvent){
				if(it.previousIndex() == 3){
					accesWaitTime -= pe.getTime();
				}
				
				// see at endEvent
				else if((it.previousIndex() > 6) && (it.previousIndex() < events.size() - 8)){
					switchWait -= pe.getTime();
				}
			}
		}
		
	}


	/**
	 * @return
	 */
	public Geometry getStart() {
		return new GeometryFactory().createPoint(this.start);
	}

	/**
	 * @return
	 */
	public Geometry getEnd() {
		return new GeometryFactory().createPoint(this.end);
	}
	
	/**
	 * @return the tripTTime
	 */
	public Double getTripTTime() {
		return tripTTime;
	}

	/**
	 * @return the accesWalkCnt
	 */
	public int getAccesWalkCnt() {
		return accesWalkCnt;
	}

	/**
	 * @return the accesWaitCnt
	 */
	public int getAccesWaitCnt() {
		return accesWaitCnt;
	}

	/**
	 * @return the egressWalkCnt
	 */
	public int getEgressWalkCnt() {
		return egressWalkCnt;
	}

	/**
	 * @return the switchWalkCnt
	 */
	public int getSwitchWalkCnt() {
		return switchWalkCnt;
	}

	/**
	 * @return the switchWaitCnt
	 */
	public int getSwitchWaitCnt() {
		return switchWaitCnt;
	}

	/**
	 * @return the lineCnt
	 */
	public int getLineCnt() {
		return lineCnt;
	}

	/**
	 * @return the accesWalkTTime
	 */
	public double getAccesWalkTTime() {
		return accesWalkTTime;
	}

	/**
	 * @return the accesWaitTime
	 */
	public double getAccesWaitTime() {
		return accesWaitTime;
	}

	/**
	 * @return the egressWalkTTime
	 */
	public double getEgressWalkTTime() {
		return egressWalkTTime;
	}

	/**
	 * @return the switchWalkTTime
	 */
	public double getSwitchWalkTTime() {
		return switchWalkTTime;
	}

	/**
	 * @return the switchWaitTime
	 */
	public double getSwitchWaitTime() {
		return switchWaitTime;
	}

	/**
	 * @return the lineTTime
	 */
	public double getLineTTime() {
		return lineTTime;
	}
}
