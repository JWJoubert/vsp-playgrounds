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
package playground.vsp.analysis.modules.transferPayments;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;

/**
 * @author ikaddoura, benjamin
 *
 */
public class MoneyEventHandler implements AgentMoneyEventHandler{

	SortedMap<Id, Double> id2amount = new TreeMap<Id, Double>();
	
	@Override
	public void handleEvent(AgentMoneyEvent event) {
		
		Id id = event.getPersonId();
		Double amountByEvent = event.getAmount();
		Double amountSoFar = id2amount.get(id);
		
		if(amountSoFar == null){
			id2amount.put(id, amountByEvent);
		}
		else{
			amountSoFar += amountByEvent;
			id2amount.put(id, amountSoFar);
		}	
	}

	public Map<Id, Double> getPersonId2amount() {
		return Collections.unmodifiableMap(id2amount);
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub	
	}
}
