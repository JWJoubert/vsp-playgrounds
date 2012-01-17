/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyThrowEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn2;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */
public class MoneyThrowEventHandler implements PersonEntersVehicleEventHandler {

	private EventsManager events;
	private double fare;
	private Population population;

	public MoneyThrowEventHandler(EventsManager events, Population population, double fare) {
		this.events = events;
		this.population = population;
		this.fare = fare;
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().toString().contains("person") && event.getVehicleId().toString().contains("bus")){
			this.fare = calculateFare(event, population);
			AgentMoneyEvent moneyEvent = new AgentMoneyEventImpl(event.getTime(), event.getPersonId(), fare);
			this.events.processEvent(moneyEvent); //schickt das MoneyEvent an den EventManager
		}
	}

	private double calculateFare(PersonEntersVehicleEvent event, Population population) {
		return this.fare;
	}

}
