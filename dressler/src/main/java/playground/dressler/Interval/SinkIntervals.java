/* *********************************************************************** *
 * project: org.matsim.*												   *
 * SinkIntervals.java													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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


package playground.dressler.Interval;

import java.util.ArrayList;

import playground.dressler.control.Debug;

//java imports

//playground imports

/**
 * Class representing the flow into a sink in the Time Expanded Network
 * @author Manuel Schneider, Daniel Dressler
 *
 */
public class SinkIntervals extends Intervals<EdgeInterval> {

//**********************************FIELDS*****************************************//
	
	/**
	 * debug flag
	 */
	private static int _debug =0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeInterval [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public SinkIntervals(EdgeInterval interval){
		super(interval);
	}

//--------------------------------------FLOW---------------------------------------//	
	
	/**
	 * Gives the Flow on the Edge at time t
	 * @param t time
	 * @return flow at t
	 */
	public int getFlowAt(final int t){
		return getIntervalAt(t).getFlow();
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(final int debug){
		SinkIntervals._debug=debug;
	}


	/**
	 * Checks whether flow is entering the sink within incoming
	 * @param incoming The interval on which one arrives.
	 * @return the first interval with flow on it (within incoming), or null 
	 */
	public Interval canSendFlowBack(final Interval incoming){

		EdgeInterval current;
		
		current = this.getIntervalAt(incoming.getLowBound());
		while (current != null) {		
			if (current.getFlow() > 0) {				
				int low = Math.max(current.getLowBound(), incoming.getLowBound());					  
				int high = Math.min(current.getHighBound(), incoming.getHighBound());
				return new Interval(low, high);

			}
			if (current.getHighBound() >= incoming.getHighBound()) {
				break;
			}
			
			if (this.isLast(current)) {
				break;
			}
			current = this.getIntervalAt(current.getHighBound());
		}
			

		return null;
	}
	
	/**
	 * Gives a list of intervals when flow into the sink can be undone.
	 * @param timeHorizon for easy reference
	 * @return plain old ArrayList of Interval
	 */
	public ArrayList<Interval> canSendFlowBackAll(int timeHorizon){

		ArrayList<Interval> result = new ArrayList<Interval>();

		EdgeInterval current;
		Interval toinsert;

		int low = -1;
		int high = -1;						
		boolean collecting = false;

		
		current = this.getIntervalAt(0);
		
		while (current.getLowBound() < timeHorizon) {				
			if (current.getFlow() > 0) {				
				if (collecting) {
					high = current.getHighBound();
				} else {
					collecting = true;
					low = current.getLowBound();					  
					high = current.getHighBound();
				}
			} else {
				if (collecting) { // finish the Interval
					high = Math.min(high, timeHorizon);
					if (low < high) {
						toinsert = new Interval(low, high);					  
						result.add(toinsert);
					}
					collecting = false;
				}
			}
						
			if (this.isLast(current)) {
				break;
			}
			current = this.getIntervalAt(current.getHighBound());				
		} 

		if (collecting) { // finish the Interval
			high = Math.min(high, timeHorizon);
			if (low < high) {
				toinsert = new Interval(low, high);					  
				result.add(toinsert);
			}
			collecting = false;
		}


		return result;
	}
	
	
//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent EdgeIntervals, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();
		EdgeInterval i, j;
		i = getIntervalAt(0);
		while (i != null) {
			if (i.getHighBound() == timestop) break;	
			j = this.getIntervalAt(i.getHighBound());

			if ((i.getHighBound() == j.getLowBound()) && 
					(i.getFlow() == j.getFlow())) {
				// FIXME use a safer method for removing things!
				_tree.remove(i);
				_tree.remove(j);
				_tree.insert(new EdgeInterval(i.getLowBound(), j.getHighBound(), i.getFlow()));
				gain++;
			} else {
				i = j;
			}		 		 
		}
		this._last = i; // we might have to update it, just do it always
		return gain;
	}
	
//------------------------Augmentation--------------------------------//
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f if capacity is obeyed
	 * @param t raising time
	 * @param f aumount of flow to augment
	 * @param u capcity of the edge
	 */
	public void augment(final int t,final int f,final int u){
		if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
			if (t<0){
				throw new IllegalArgumentException("negative time: "+ t);
			}
		}
		
		EdgeInterval i = getIntervalAt(t);
		
		if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
			if (i.getFlow()+f>u){
				throw new IllegalArgumentException("too much flow! flow: " + i.getFlow() + " + " +
						f + " > " + u);
			}

			if (i.getFlow()+f<0){
				throw new IllegalArgumentException("negative flow! flow: " + i.getFlow() + " + " +
						f + " < 0");
			}
		}
		
		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > (t+1)){
			splitAt(t+1);
		}
		i.augment(f, u);
	}
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f
	 * no checking is done
	 * @param t raising time
	 * @param gamma aumount of flow to augment (can be negative) 
	 */
	public void augmentUnsafe(final int t, final int gamma){
		EdgeInterval i = getIntervalAt(t);

		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > (t+1)){
			splitAt(t+1);
		}
		i.augmentUnsafe(gamma);
	}

}