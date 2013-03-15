/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.ikaddoura.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


import playground.ikaddoura.optimization.externalDelayEffects.InVehicleDelayHandler;
import playground.ikaddoura.optimization.externalDelayEffects.WaitingDelayHandler;
import playground.ikaddoura.optimization.handler.MarginalCostPricingHandler;
import playground.ikaddoura.optimization.handler.MoneyDetailEventHandler;
import playground.ikaddoura.optimization.handler.MoneyEventHandler;
import playground.ikaddoura.optimization.io.TextFileWriter;

/**
 * 
 * @author ikaddoura
 *
 */
public class DefaultAnalysisMain {
	
	private String configFile = "/Users/Ihab/Desktop/5min_output_config.xml";
	private String outputPath = "/Users/Ihab/Desktop";
	private String eventsFile = "/Users/Ihab/Desktop/ConstFare_5min_maxWelfare_events_noAmounts.xml";
	private String eventsFile_withExtDelayEvents = "/Users/Ihab/Desktop/eventsWithExtDelayEffects.xml";
	
	public static void main(String[] args) {
		
		DefaultAnalysisMain aM = new DefaultAnalysisMain();
		aM.writeExtDelayAndMoneyEvents();
		aM.analyseAmounts();
	}

	private void writeExtDelayAndMoneyEvents() {
		
		// other agentMoneyEvents than external delay costs not allowed!
		Config config = ConfigUtils.loadConfig(configFile);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		InVehicleDelayHandler inVehDelayHandler = new InVehicleDelayHandler(events, scenario);
		events.addHandler(inVehDelayHandler);
		WaitingDelayHandler waitingDelayHandler = new WaitingDelayHandler(events, scenario);
		events.addHandler(waitingDelayHandler);
		MarginalCostPricingHandler mcpHandler = new MarginalCostPricingHandler(events, scenario);
		events.addHandler(mcpHandler);
		
		EventWriterXML eventWriter = new EventWriterXML(eventsFile_withExtDelayEvents);
		events.addHandler(eventWriter);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		eventWriter.closeFile();
	}


	private void analyseAmounts() {
		
		// other agentMoneyEvents than external delay costs not allowed!
		EventsManager events = EventsUtils.createEventsManager();
		
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		events.addHandler(moneyHandler);
		MoneyDetailEventHandler moneyDetailEventHandler = new MoneyDetailEventHandler();
		events.addHandler(moneyDetailEventHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile_withExtDelayEvents);
		
		TextFileWriter writer = new TextFileWriter();
		writer.wrtieFarePerTime(outputPath, moneyDetailEventHandler.getAvgFarePerTripDepartureTime());		
		writer.writeFareData(outputPath, moneyHandler.getfareDataList());
		writer.writeTripFarePerId(outputPath, moneyDetailEventHandler.getPersonId2fareFirstTrip(), moneyDetailEventHandler.getPersonId2fareSecondTrip());
		System.out.println("total external Costs: " + moneyHandler.getRevenues());
		System.out.println("avg ext. cost per trip: " + moneyHandler.getAverageAmountPerPerson() * 8);
	}
	
}
