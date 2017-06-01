/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.moneyTravelDisutility;

import org.matsim.core.controler.AbstractModule;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;

/**
* @author ikaddoura
*/

public class MoneyTravelDisutilityModule extends AbstractModule {
	
	private final String mode;
	private final MoneyTimeDistanceTravelDisutilityFactory factory;
	private final AgentFilter agentFilter;
	
	public MoneyTravelDisutilityModule(String mode, MoneyTimeDistanceTravelDisutilityFactory factory, AgentFilter agentFilter) {
		this.mode = mode;
		this.factory = factory;
		this.agentFilter = agentFilter;
	}

	public MoneyTravelDisutilityModule(String mode, MoneyTimeDistanceTravelDisutilityFactory factory) {
		this.mode = mode;
		this.factory = factory;
		this.agentFilter = null;
	}

	@Override
	public void install() {
		
		if (agentFilter != null) this.bind(AgentFilter.class).toInstance(agentFilter);
		
		this.addTravelDisutilityFactoryBinding(mode).toInstance(factory);
		
		this.bind(MoneyEventAnalysis.class).asEagerSingleton();	
		this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
		this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
	}

}

