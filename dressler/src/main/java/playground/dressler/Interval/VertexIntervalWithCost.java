/* *********************************************************************** *
 * project: org.matsim.*
 * VertexIntervalWithCost.java
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

public class VertexIntervalWithCost extends VertexInterval {

	/**
	 * relative gives time t cost = t + cost;
	 * absolute gives time t cost = cost;
	 * for an interval of length 1, relative cost 0 is the same as absolute cost lowbound 
	 */
	public boolean costIsRelative = true;
	public int cost = 0;
	
	public VertexIntervalWithCost(final int l,final int r) {
		super(l, r);
	}
	
	/**
	 * Should the other VertexInterval be replaced with this?
	 * It is assumed that this is reachable! Why else would you call this function? 
	 * Note that in intricate situations of being better "here and there",
	 * the method has to be called on subintervals after the first returned interval again.
	 * CAVE: This version that understands costs is not compatible with the mixed search!     
	 * @param other a VertexInterval
	 * @return null if not, and the first subinterval of other that should be replaced otherwise 
	 */
	public Interval isBetterThan(final VertexInterval other) {
		// argh.
		if (!(other instanceof VertexIntervalWithCost)) return super.isBetterThan(other);
		
		VertexIntervalWithCost temp = (VertexIntervalWithCost) other;  
		
		boolean isbetter = false;
		
		// get the intersection first.
		int l = Math.max(this._l, temp._l);
		int r = Math.min(this._r, temp._r);
		
		// empty intersection?
	    if (l >= r) return null;
		
		if (!temp.reachable) {
			isbetter = true;
		} else {
			if (this.costIsRelative == temp.costIsRelative) {
				if (this.cost < temp.cost) {
					isbetter = true;
				}
			} else { // one is relative, one is absolute
				// FIXME this case can fragment the intervals!
				// TODO test ...
				if (this.costIsRelative) {			
					// we increase steadily in cost, the other does not
					// stop one before the breakeven spot. 
					r = temp.cost - this.cost - 1;
					
					isbetter = true; // this relies on the final l < r check!
				} else {
					// we stay constant, the other increases
					// start one after the breakeven spot
					l = temp.cost - this.cost + 1;
					
					isbetter = true; // this relies on the final l < r check!
				}
			}
		}
			
		// having more breadcrumbs than other is also better
		// but this framework is not compatible with mixed search,
		// so it does not matter at all!
		// for forward and reverse search, reachable should take care of this 
		/*if (this._pred != null && other._pred == null) {
			isbetter = true;
		}
		else if (this._succ != null && other._succ == null) {
			isbetter = true;
		}*/
		
		if (isbetter) {			
			if (l < r) {
			  return new Interval(l, r);
			}
		}
		
		return null;		
	}
	
	
	/**
	 * Set the fields of the VertexInterval reachable true, scanned false, and the _pred to pred
	 * This performs no checks at all!
	 * Note that this is not suitable for the Reverse search anymore! 
	 * @param pred which is set as predecessor. It is never shifted anymore.
	 */	
	public void setArrivalAttributesForward (final PathStep pred)
	{
		// we might have already scanned this interval
		// but someone insists on relabelling it.
		this.scanned = false;
		this.reachable = true;
		this._pred = pred;
	}
	
	/**
	 * Set the fields of the VertexInterval reachable true, scanned false, and the _pred to pred
	 * This performs no checks at all!
	 * @param succ which is set as successor. It is never shifted anymore.
	 */		
	public void setArrivalAttributesReverse (final PathStep succ)
	{
		// we might have already scanned this interval
		// but someone insists on relabelling it.
		this.scanned = false;
		this.reachable = true;
		this._succ = succ;
	}
	
	/**
	 * Set the fields of the VertexInterval to the one given.
	 * Predecessor or Successor are only updated if they are not null. 
	 * @param other The VertexInterval from which the settings are copied
	 * @return if there is an unusual reason to scan again ... this is never true here, but would be if costs get upgraded  
	 */
	public boolean setArrivalAttributes (final VertexInterval other)
	{
		// argh.
		if (!(other instanceof VertexIntervalWithCost)) return super.setArrivalAttributes(other);
		
		VertexIntervalWithCost temp = (VertexIntervalWithCost) other;
		
		this.scanned = temp.scanned;
		this.reachable = temp.reachable;
		if (temp._pred != null) 
		  this._pred = temp._pred;
		
		if (temp._succ != null)
		  this._succ = temp._succ;
		
		this.costIsRelative = temp.costIsRelative;
		this.cost = temp.cost;
		
		return false;
	}
	
	/**
	 * Can this VertexInterval be combined with other?
	 * Times or interval bounds are not checked
	 * except for intervals of length 1, where relative and absolute costs behave the same
	 * Note that the results are not transitive, that is, check only two at a time an join them afterwards!
	 * Do not scan an entire list for the largest joinable part.  
	 * @param other VertexInterval to compare to
	 * @return true iff the intervalls agree on their arrival properties
	 */
	public boolean continuedBy(final VertexInterval o) {
		// argh.
		if (!(o instanceof VertexIntervalWithCost)) return super.setArrivalAttributes(o);
		
		VertexIntervalWithCost other = (VertexIntervalWithCost) o;
		
		if (this.scanned != other.scanned) return false;
		if (this.reachable != other.reachable) return false;
		
		if (this._pred == null) {
			if (other._pred != null) return false;			
		} else {
			if (!this._pred.continuedBy(other._pred)) return false;
		}
		
		if (this._succ == null) {
			if (other._succ != null) return false;
		} else {
		   if (!this._succ.continuedBy(other._succ)) return false;
		}
		
		// FIXME test
		if (this.costIsRelative) {
			if (other.costIsRelative) {
				if (this.cost != other.cost) return false;
			} else if (this.length() == 1) {
				// an interval of length 1 could just as well be absolute
				if (this.cost + this._l != other.cost) return false; 
			} else if (other.length() == 1) {
				// an interval of length 1 could just as well be relative
				if (this.cost != other.cost - other._l) return false;
			}
		} else {
			if (!other.costIsRelative) {
				if (this.cost != other.cost) return false;
			} else if (this.length() == 1) {
				// we could just as well be relative
				if (this.cost - this._l != other.cost) return false;
			} else if (other.length() == 1) {
				// other could just as well be absolute
				if (this.cost != other.cost + other._l) return false;
			}
		}
		
		return true;
	}
	
	
}
