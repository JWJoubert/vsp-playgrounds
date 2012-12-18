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
package playground.vsp.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author droeder
 *
 */
public class VspAnalyzer {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(VspAnalyzer.class);
	private String outdir;
	private List<AbstractAnalyisModule> modules;
	private String eventsFile;

	/**
	 * Simple container-class to handle {@link AbstractAnalysisModule}s.
	 * The modules are processed in the order as they are added.
	 * The initialization of the single modules has to be done before.
	 * Make sure you add the correct data to the module, especially if you are
	 * handling events.Only one events-file will be handled!
	 * 
	 * @param outdir, the output-directory 
	 * @param eventsFile, the events
	 */
	public VspAnalyzer(String outdir, String eventsFile) {
		this.outdir = outdir;
		this.modules = new ArrayList<AbstractAnalyisModule>();
		this.eventsFile = eventsFile;
	}
	
	/**
	 * Simple container-class to handle {@link AbstractAnalysisModule}s.
	 * The modules are processed in the order as they are added.
	 * The initialization of the single modules has to be done before.
	 * Make sure you add the correct data to the module!
	 * 
	 * If you use this constructor, no events will be handled!!!
	 * 
	 * @param outdir, the output-directory 
	 */
	public VspAnalyzer(String outdir) {
		this.outdir = outdir;
		this.modules = new ArrayList<AbstractAnalyisModule>();
		this.eventsFile = null;
	}
	
	public void addAnalysisModule(AbstractAnalyisModule module){
		this.modules.add(module);
	}
	
	public void run(){
		log.info("running " + VspAnalyzer.class.getSimpleName());
		Gbl.startMeasurement();
		this.preProcess();
		if(!(this.eventsFile == null)){
			if(new File(this.eventsFile).exists()){
				this.handleEvents();
				Gbl.printElapsedTime(); Gbl.printMemoryUsage();
			}else{
				log.warn("can not handle events, because the specified file does not exist!");
			}
		}
		this.postProcess();
		this.writeResults();
		log.info("finished " + VspAnalyzer.class.getSimpleName());
	}

	/**
	 * 
	 */
	private void preProcess() {
		log.info("preprocessing all modules...");
		for(AbstractAnalyisModule module: this.modules){
			log.info("preprocessing " + module.getName());
			module.preProcessData();
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("preprocessing finished...");
	}

	/**
	 * 
	 */
	private void handleEvents() {
		log.info("handling events for all modules...");
		EventsManager manager = EventsUtils.createEventsManager();
		for(AbstractAnalyisModule module: this.modules){
			log.info("adding eventHandler from " + module.getName());
			for(EventHandler handler: module.getEventHandler()){
				manager.addHandler(handler);
			}
		}
		new MatsimEventsReader(manager).readFile(this.eventsFile);
		log.info("event-handling finished...");
	}

	/**
	 * 
	 */
	private void postProcess() {
		log.info("post-processing all modules...");
		for(AbstractAnalyisModule module: this.modules){
			log.info("postprocessing " + module.getName());
			module.postProcessData();
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("post-processing finished...");
		
	}

	/**
	 * 
	 */
	private void writeResults() {
		log.info("writing data for all modules...");
		String outdir;
		for(AbstractAnalyisModule module: this.modules){
			outdir = this.outdir + "/" + module.getName() + "/";
			log.info("writing output for " + module.getName() + " to " + outdir);
			if(!new File(outdir).exists()){
				new File(outdir).mkdirs();
			}
			module.writeResults(outdir);
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("writing finished...");
	}
}

