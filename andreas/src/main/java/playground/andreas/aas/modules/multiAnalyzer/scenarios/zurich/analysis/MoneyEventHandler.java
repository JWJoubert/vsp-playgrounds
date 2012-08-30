/* *********************************************************************** *
 * project: org.matsim.*
 * Trb09Analysis
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
package playground.andreas.aas.modules.multiAnalyzer.scenarios.zurich.analysis;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;

/**
 * @author benjamin
 *
 */
public class MoneyEventHandler implements AgentMoneyEventHandler{

	SortedMap<Id, Double> id2Toll = new TreeMap<Id, Double>();
	
	@Override
	public void handleEvent(AgentMoneyEvent event) {
		
		Id id = event.getPersonId();
		Double tollByEvent = event.getAmount();
		Double tollSoFar = id2Toll.get(id);
		
		if(tollSoFar == null){
			id2Toll.put(id, tollByEvent);
		}
		else{
			tollSoFar += tollByEvent;
			id2Toll.put(id, tollSoFar);
		}	
	}

	public Map<Id, Double> getPersonId2TollMap() {
		return Collections.unmodifiableMap(id2Toll);
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub	
	}
}
