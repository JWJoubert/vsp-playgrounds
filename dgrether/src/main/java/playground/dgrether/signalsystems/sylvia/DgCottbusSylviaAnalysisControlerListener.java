/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusSylviaAnalysisControlerListener
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
package playground.dgrether.signalsystems.sylvia;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.dgrether.signalsystems.analysis.DgGreenSplitWriter;
import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;

/**
 * 
 * @author dgrether
 * @deprecated
 */
@Deprecated
public class DgCottbusSylviaAnalysisControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

	private static final Logger log = Logger.getLogger(DgCottbusSylviaAnalysisControlerListener.class);

	private final static boolean collectGreenSplitInformation = true;
	
	private DgTimeCalcEventHandler timeCalcHandler;

	private DgSignalGreenSplitHandler signalGreenSplitHandler;

	
	@Override
	public void notifyStartup(StartupEvent e) {
		this.timeCalcHandler = new DgTimeCalcEventHandler();
		e.getControler().getEvents().addHandler(this.timeCalcHandler);
		if (collectGreenSplitInformation){
			this.initGreenSplitHandler();
			e.getControler().getEvents().addHandler(this.signalGreenSplitHandler);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent e) {

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent e) {
		if (e.getIteration() == 0 || e.getIteration() % 10 == 0){
			log.info("Analysis of run id: " + e.getControler().getConfig().controler().getRunId());
			log.info("Agents that passed an adaptive signal system (1,17 or 18) at least once: "
					+ this.timeCalcHandler.getPassedAgents());
			
			if (collectGreenSplitInformation){
				this.writeSignalStats(e);
			}
		}
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent e) {
	}

	
	private void initGreenSplitHandler(){
		this.signalGreenSplitHandler = new DgSignalGreenSplitHandler();
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("18"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("17"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("1"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("28"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("27"));
		this.signalGreenSplitHandler.addSignalSystem(new IdImpl("12"));
	}

	
	
	private void writeSignalStats(IterationEndsEvent event){
			String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "signal_statistic.csv");
			new DgGreenSplitWriter(signalGreenSplitHandler).writeFile(filename);
			
			log.info("Wrote signalsystemstats.");
	}
}
