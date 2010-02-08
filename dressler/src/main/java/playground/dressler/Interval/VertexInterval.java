/* *********************************************************************** *
 * project: org.matsim.*
 * VertexInterval.java
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

//matsim imports
import org.matsim.api.core.v01.network.Link;

import playground.dressler.ea_flow.PathStep;


/**
 * @author Manuel Schneider
 * class representing a node in an time expanded network
 * between two integer Points of time
 * carries a predecessor node and a minimal distance for shortest path computation
 */
public class VertexInterval extends Interval {

//---------------------------FIELDS----------------------------//	
	
	/**
	 * shows whether the vertex is reacheable during the time interval
	 */
	private boolean reachable = false;
	
	private boolean scanned = false;


	/**
	 * predecessor in a shortest path
	 * the times stored in the step are valid for arrival at the low bound!
	 */
	private PathStep _predecessor=null;
	

//---------------------------METHODS----------------------------//
//**************************************************************//
	


//--------------------------CONSTUCTORS-------------------------//
	/**
	 * Default costructor creates an (0,1) Interval 
	 * with Integer.MAX_VALUE as initial distance to the sink
	 * no predesessor is specified
	 */
	public VertexInterval() {
		super();
	}

	/**
	 * Creates in VertexInterval from l to r
	 * @param l lowbound
	 * @param r highbound
	 */
	public VertexInterval(final int l,final int r) {
		super(l, r);
		// dummy values
		this.reachable = false;
		this.scanned = false;
		this._predecessor = null;
	}
	
	/**
	 * construct an VertexInterval from l to r with the settings of other
	 * the Predecessor will be shifted to the new value of l
	 * @param l lowbound
	 * @param r highbound
	 * @param other Interval to copy settings from
	 */
	public VertexInterval(final int l,final int r,final VertexInterval other)
	{
		super(l,r);
		//this.lastDepartureAtFromNode = other.lastDepartureAtFromNode;		
		this.reachable = other.reachable;
		this.scanned = other.scanned;
		this._predecessor = other._predecessor.copyShiftedToArrival(l);
	}

	/**
	 * creates an VertexInterval instance as a copy of an Interval
	 * Predecessor is set to null and dist to Integer.MAX_VALUE
	 * @param j Interval to copy
	 */
	public VertexInterval(final Interval j) {
		super(j.getLowBound(),j.getHighBound());
		
	}

//------------------------Getter Setter----------------------//
	/**
	 * Setter for the min distance to the sink at time lowbound
	 * @param d min distance to sink
	 */
	public void setReachable(final boolean d){
		this.reachable=d;
	}
	
	/**
	 * Getter for the min distance to the sink at time lowbound
	 * @return if reachable from source
	 */
	public boolean getReachable(){
		return this.reachable;
	}
	
	
	/**
	 * Setter for the predecessor in a shortest path
	 * @param pred predesessor vertex
	 */
	public void setPredecessor(final PathStep pred){
		this._predecessor=pred;
	}
	
	/**
	 * Getter for the predecessor in a shortest path
	 * @return predecessor vertex 
	 */
	public PathStep getPredecessor(){
		return this._predecessor;
	}
	
	
	/**
	 * getter for scanned
	 * @return scanned
	 */
	public boolean isScanned() {
		return scanned;
	}

	/**
	 * setter for scanned
	 * @param scanned
	 */
	public void setScanned(final boolean scanned) {
		this.scanned = scanned;
	}
	
	/**
	 * Set the fields of the VertexInterval reachable true, scanned false.
	 * This performs no checks at all!
	 * @param pred which is set as predecessor, always shifted to low bound!
	 */
	public void setArrivalAttributes (final PathStep pred)
	{
		//we might have already scanned this interval
		this.scanned = false;
		this.reachable = true;				
		// FIXME this shifting here has a tendency to be confusing and to break things!
		this._predecessor = pred.copyShiftedToArrival(this.getLowBound());
		//ourInterval.setLastDepartureAtFromNode(arrive.getLastDepartureAtFromNode());		
	}
	
//----------------------------SPLITTING----------------------------//
	
	/**
	 * splits the referenced VertexInterval at t contained in (lowbound, higbound)
	 * by changing!! referenced Interval to (lowbound,t) 
	 * and creating a new EdgeInterval (t,highbound) with same Predecesor as the referenced
	 *@param t time to split at
	 *@return new Interval 
	 */
	public VertexInterval splitAt(int t){		
		Interval j = super.splitAt(t);
		VertexInterval k = new VertexInterval(j);
		k.reachable = this.reachable;
		k.scanned = this.scanned;
		if (this._predecessor == null) {
			k._predecessor = null;
		} else {
		    k._predecessor= this._predecessor.copyShiftedToArrival(k.getLowBound());
		}
		//k.lastDepartureAtFromNode = this.lastDepartureAtFromNode;
		return k;
	}
	
	/**
	 * gives a string representation of tje VertexInterval of the form with optional predecessor
	 * [x,y): reachable: true scanned: false pred: 
	 * @return string representation 
	 */
	 
	public String toString()
	{
		return super.toString() + "; reachable: " + this.reachable + "; scanned: " + this.scanned + "; pred: " + this._predecessor;
	}
}
