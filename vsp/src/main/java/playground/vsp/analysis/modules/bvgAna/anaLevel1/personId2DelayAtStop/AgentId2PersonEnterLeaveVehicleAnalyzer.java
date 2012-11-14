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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.bvgAna.anaLevel1.personId2DelayAtStop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverPrefixAnalyzer;

/**
 * @author ikaddoura
 *
 */
public class AgentId2PersonEnterLeaveVehicleAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(AgentId2PersonEnterLeaveVehicleAnalyzer.class);
	private ScenarioImpl scenario;
	
	private List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();
	private PtDriverPrefixAnalyzer ptDriverPrefixAnalyzer;
	
	private PersonId2DelayAtStopHandler handler;
	private TreeMap<Id, PersonId2DelayAtStopData> personId2DelayAtStop;
	
	public AgentId2PersonEnterLeaveVehicleAnalyzer() {
		super(AgentId2PersonEnterLeaveVehicleAnalyzer.class.getSimpleName());
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverPrefixAnalyzer = new PtDriverPrefixAnalyzer();
		this.ptDriverPrefixAnalyzer.init(scenario);
		this.anaModules.add(ptDriverPrefixAnalyzer);
		
		this.handler = new PersonId2DelayAtStopHandler(this.ptDriverPrefixAnalyzer);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();

		// from (sub-)modules
		for (AbstractAnalyisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
		
		// own handler
		allEventHandler.add(this.handler);
		
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		log.info("Preprocessing all (sub-)modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
	}

	@Override
	public void postProcessData() {
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		this.personId2DelayAtStop = this.handler.getPersonId2DelayAtStopMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public TreeMap<Id, PersonId2DelayAtStopData> getPersonId2DelayAtStop() {
		return personId2DelayAtStop;
	}
	
	

}
