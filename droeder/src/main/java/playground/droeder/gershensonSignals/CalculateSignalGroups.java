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
package playground.droeder.gershensonSignals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * @author Daniel
 *
 */
public class CalculateSignalGroups{
	private static final Logger log = Logger.getLogger(CalculateSignalGroups.class);
	
	public Map<Id, Id> calculateCorrespondingGroups(Map<Id, SignalGroupDefinition> groups, Network net){
		SortedMap<Id, Id> corrGroups = new TreeMap<Id, Id>();
		Link l;
		//Iteration over all Signalgroupdefinitions
		for(SignalGroupDefinition sd : groups.values()) {
			l = net.getLinks().get(sd.getLinkRefId());
			if (l == null){
				corrGroups.put(sd.getId(), null);
			}else{
				l = calculateLink(l);
				//compare which Signalgroup fits to the calculated Link
				for(SignalGroupDefinition sd2 : groups.values()){
					if(sd2.getLinkRefId().equals(l.getId())){
						corrGroups.put(sd.getId(), sd2.getId());
					}else {
						corrGroups.put(sd.getId(), null);
					}
				}
			}
		}		
		return corrGroups;
	}
	
	public Map<Id, List<Id>> calculateCompetingGroups(Map<Id, Id> ids, Map<Id, SignalGroupDefinition> groups, Network net){
		Map<Id, List<Id>> cg = new TreeMap<Id,List<Id>>();
		
		for (Entry <Id, Id> ee : ids.entrySet()){
			List<Id> l = new ArrayList<Id>();
			for (SignalGroupDefinition sd : groups.values()){
				if (sd.getId().equals(ee.getKey()) || sd.getId().equals(ee.getValue())){
					
				}else{
					l.add(sd.getId());
				}
			}
			cg.put(ee.getKey(), l);
		}
		return cg;
	}
	
	public static Link calculateLink(Link inLink){
		Coord coordInLink = getVector(inLink);
		double temp = Math.PI / 2;
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		Link l = null;
		Map<Link, Double> linkAngle = new HashMap<Link, Double>();
				
		for (Link cl : inLink.getToNode().getInLinks().values()){
			
			Coord coordCorrLink = getVector(cl);
			double thetaCorrLink = Math.atan2(coordCorrLink.getY(), coordCorrLink.getX());
			double thetaDiff = thetaCorrLink - thetaInLink;
			if (thetaDiff < -Math.PI){
				thetaDiff += 2 * Math.PI;
			} else if (thetaDiff > Math.PI){
				thetaDiff -= 2 * Math.PI;
			}
			linkAngle.put(cl, (Math.abs(thetaDiff)));
		}
		
		for (Entry<Link, Double> e :  linkAngle.entrySet()) {
			if (((Math.PI / 2)  - e.getValue()) < temp ){
				temp = (Math.PI / 2) - e.getValue();
				if (inLink.getToNode().getInLinks().size() > 3){
					l = e.getKey();
				}else{
					l = null;
				}
				
			}
		}
		return l;
	}
	
	private static Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}
}
