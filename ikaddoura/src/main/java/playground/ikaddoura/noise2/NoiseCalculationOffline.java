/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.noise2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * (1) Computes noise emissions and immissions based on a standard events file.
 * (2) Optionally throws noise immission damage events.
 * (2) Writes out some analysis.
 * 
 * @author ikaddoura
 *
 */
public class NoiseCalculationOffline {
	private static final Logger log = Logger.getLogger(NoiseCalculationOffline.class);
	
	private static String runDirectory;
	private static String outputDirectory;
	private static int lastIteration;
	private static double receiverPointGap;
				
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			
			lastIteration = Integer.valueOf(args[1]);
			log.info("last iteration: " + lastIteration);
			
			outputDirectory = args[2];		
			log.info("output directory: " + outputDirectory);
			
			receiverPointGap = Double.valueOf(args[3]);		
			log.info("Receiver point gap: " + receiverPointGap);
			
		} else {
			
//			runDirectory = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/";
//			lastIteration = 100;
//			outputDirectory = "../../runs-svn/berlin_internalizationCar/output/baseCase_2/analysis_localRun/";
//			receiverPointGap = 100.;
//			scaleFactor = 10.;
			
			runDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
			lastIteration = 5;
			outputDirectory = "../../shared-svn/studies/ihab/noiseTestScenario/output/";
			receiverPointGap = 250.;
		}
		
		NoiseCalculationOffline noiseCalculation = new NoiseCalculationOffline();
		noiseCalculation.run();
	}

	private void run() {
	
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(lastIteration);
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setScaleFactor(1.);
		
		// Berlin Tunnel Link IDs
//		List<Id<Link>> tunnelLinkIDs = new ArrayList<Id<Link>>();
//		tunnelLinkIDs.add(Id.create("108041", Link.class));
//		tunnelLinkIDs.add(Id.create("108142", Link.class));
//		tunnelLinkIDs.add(Id.create("108970", Link.class));
//		tunnelLinkIDs.add(Id.create("109085", Link.class));
//		tunnelLinkIDs.add(Id.create("109757", Link.class));
//		tunnelLinkIDs.add(Id.create("109919", Link.class));
//		tunnelLinkIDs.add(Id.create("110060", Link.class));
//		tunnelLinkIDs.add(Id.create("110226", Link.class));
//		tunnelLinkIDs.add(Id.create("110164", Link.class));
//		tunnelLinkIDs.add(Id.create("110399", Link.class));
//		tunnelLinkIDs.add(Id.create("96503", Link.class));
//		tunnelLinkIDs.add(Id.create("110389", Link.class));
//		tunnelLinkIDs.add(Id.create("110116", Link.class));
//		tunnelLinkIDs.add(Id.create("110355", Link.class));
//		tunnelLinkIDs.add(Id.create("92604", Link.class));
//		tunnelLinkIDs.add(Id.create("92603", Link.class));
//		tunnelLinkIDs.add(Id.create("25651", Link.class));
//		tunnelLinkIDs.add(Id.create("25654", Link.class));
//		tunnelLinkIDs.add(Id.create("112540", Link.class));
//		tunnelLinkIDs.add(Id.create("112556", Link.class));
//		tunnelLinkIDs.add(Id.create("5052", Link.class));
//		tunnelLinkIDs.add(Id.create("5053", Link.class));
//		tunnelLinkIDs.add(Id.create("5380", Link.class));
//		tunnelLinkIDs.add(Id.create("5381", Link.class));
//		tunnelLinkIDs.add(Id.create("106309", Link.class));
//		tunnelLinkIDs.add(Id.create("106308", Link.class));
//		tunnelLinkIDs.add(Id.create("26103", Link.class));
//		tunnelLinkIDs.add(Id.create("26102", Link.class));
//		tunnelLinkIDs.add(Id.create("4376", Link.class));
//		tunnelLinkIDs.add(Id.create("4377", Link.class));
//		tunnelLinkIDs.add(Id.create("106353", Link.class));
//		tunnelLinkIDs.add(Id.create("106352", Link.class));
//		tunnelLinkIDs.add(Id.create("103793", Link.class));
//		tunnelLinkIDs.add(Id.create("103792", Link.class));
//		tunnelLinkIDs.add(Id.create("26106", Link.class));
//		tunnelLinkIDs.add(Id.create("26107", Link.class));
//		tunnelLinkIDs.add(Id.create("4580", Link.class));
//		tunnelLinkIDs.add(Id.create("4581", Link.class));
//		tunnelLinkIDs.add(Id.create("4988", Link.class));
//		tunnelLinkIDs.add(Id.create("4989", Link.class));
//		tunnelLinkIDs.add(Id.create("73496", Link.class));
//		tunnelLinkIDs.add(Id.create("73497", Link.class));
//		noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);
		
//		// Berlin Coordinates: Area around the city center of Berlin (Tiergarten)
//		double xMin = 4590855.;
//		double yMin = 5819679.;
//		double xMax = 4594202.;
//		double yMax = 5821736.;
		
////		 Berlin Coordinates: Area of Berlin
//		double xMin = 4573258.;
//		double yMin = 5801225.;
//		double xMax = 4620323.;
//		double yMax = 5839639.;
//		
//		noiseParameters.setReceiverPointsGridMinX(xMin);
//		noiseParameters.setReceiverPointsGridMinY(yMin);
//		noiseParameters.setReceiverPointsGridMaxX(xMax);
//		noiseParameters.setReceiverPointsGridMaxY(yMax);
		
		// Berlin Activity Types
//		String[] consideredActivities = {"home"};
		String[] consideredActivities = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivities(consideredActivities);
				
		log.info("Loading scenario...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		EventWriterXML eventWriter = new EventWriterXML(outputFilePath + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
		events.addHandler(eventWriter);
			
		NoiseContext noiseContext = new NoiseContext(scenario, noiseParameters);
		noiseContext.initialize();
		NoiseWriter2.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/");
		
		NoiseTimeTracker timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
		events.addHandler(timeTracker);
				
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		timeTracker.computeFinalTimeInterval();

		eventWriter.closeFile();
	}
}
		

