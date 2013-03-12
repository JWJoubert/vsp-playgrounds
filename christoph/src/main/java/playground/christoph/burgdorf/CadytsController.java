/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;

import playground.christoph.burgdorf.cadyts.CadytsContext;
import playground.christoph.burgdorf.cadyts.CadytsPlanChanger;

public class CadytsController {

	public static void main(String[] args) {
		Controler controler = new Controler(args);
		
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		controler.addControlerListener(cContext);
		
		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.0) ;
		controler.getConfig().strategy().addStrategySettings(stratSets);
		
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);
				return new PlanStrategyImpl(planSelector);
			}
		});
		
		controler.run();
	}
}
