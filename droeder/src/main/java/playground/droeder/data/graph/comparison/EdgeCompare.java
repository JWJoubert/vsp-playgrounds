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
package playground.droeder.data.graph.comparison;

import java.util.ListIterator;

import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingSegment;

/**
 * @author droeder
 *
 */
public class EdgeCompare extends AbstractCompare{
	private Double 	refTotalLength = 0.0, 
			compTotalLength = 0.0, 
			avDist = 0.0, 
			avAngle = 0.0, 
			matchedLengthRef = 0.0, 
			matchedLengthComp = 0.0;
	

	public EdgeCompare(MatchingEdge refElement, MatchingEdge compareElement) {
		super(refElement, compareElement);
		this.computeValues(refElement, compareElement);
		this.refTotalLength = refElement.getSegmentLength();
		this.compTotalLength = compareElement.getSegmentLength();
	}

	/**
	 * @param refElement
	 * @param compareElement
	 */
	private void computeValues(MatchingEdge refElement,	MatchingEdge compareElement) {
		ListIterator<MatchingSegment> candIt = refElement.getSegments().listIterator();
		ListIterator<MatchingSegment> refIt = compareElement.getSegments().listIterator();
		
		MatchingSegment rs = null, cs = null;
		SegmentCompare sc = null;
		int cnt = 0;
		
		while(candIt.hasNext() && refIt.hasNext()){
			if((rs == null) && (cs == null)){
				rs = refIt.next();
				cs = candIt.next();
			}else if(sc.refIsUndershot()){
				rs = refIt.next();
			}else if(!sc.refIsUndershot()){
				cs = candIt.next();
			}
			sc = new SegmentCompare(rs, cs);
			cnt++;
			avDist += sc.getAvDist();
			avAngle += sc.getDeltaAngle();
			matchedLengthRef += sc.getMatchedLengthRef();
			matchedLengthComp += sc.getMatchedLengthComp();
		}
		avDist = avDist / cnt;
		avAngle = avAngle / cnt;
	}
	
	public boolean isMatched(Double dDistMax, Double dPhiMax, Double lengthTolerancePercentage){
		if((avDist < dDistMax) && 
				(avAngle < dPhiMax) && 
				(Math.abs(1 - (matchedLengthRef / refTotalLength)) < lengthTolerancePercentage ) && 
				(Math.abs(1 - (matchedLengthComp / compTotalLength)) < lengthTolerancePercentage )){
			super.setScore((avDist / dDistMax) + 
					(avAngle / dPhiMax) + 
					(Math.abs(1 - (matchedLengthRef / refTotalLength)) / lengthTolerancePercentage ) + 
					(Math.abs(1 - (matchedLengthComp / compTotalLength)) / lengthTolerancePercentage ));
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(AbstractCompare o) {
		return super.compareTo(o);
	}

	/**
	 * @return the refTotalLength
	 */
	public Double getRefTotalLength() {
		return refTotalLength;
	}

	/**
	 * @return the compTotalLength
	 */
	public Double getCompTotalLength() {
		return compTotalLength;
	}

	/**
	 * @return the avDist
	 */
	public Double getAvDist() {
		return avDist;
	}

	/**
	 * @return the avAngle
	 */
	public Double getAvAngle() {
		return avAngle;
	}

	/**
	 * @return the matchedLengthRef
	 */
	public Double getMatchedLengthRef() {
		return matchedLengthRef;
	}

	/**
	 * @return the matchedLengthComp
	 */
	public Double getMatchedLengthComp() {
		return matchedLengthComp;
	}
	
	
}
