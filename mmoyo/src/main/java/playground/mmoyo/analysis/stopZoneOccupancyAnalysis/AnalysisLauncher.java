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

package playground.mmoyo.analysis.stopZoneOccupancyAnalysis;

import java.io.File;

import org.apache.commons.lang.math.NumberUtils;
import org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

/**
 * From a events file, creates an occupancy analysis: configurable time bin size, selected lines, per stop zone 
 */
public class AnalysisLauncher {

	public static void main(String[] args) {
		String configFile;
		String eventFileName;
		if (args.length>0){
			configFile=args[0];
			eventFileName=args[1];
		}else{
			configFile= "";
			eventFileName = "";	
		}
		
		//load calibrated lines and TimeBinSize from config
		final Config config = ConfigUtils.loadConfig(configFile);
		CadytsPtConfigGroup cadytsConfig = new CadytsPtConfigGroup();
		config.addModule(CadytsPtConfigGroup.GROUP_NAME, cadytsConfig);
		
		//read events
		ConfigurableOccupancyAnalyzer occupancyAnalyzerAllDay = new ConfigurableOccupancyAnalyzer(cadytsConfig.getCalibratedLines(), cadytsConfig.getTimeBinSize());
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(occupancyAnalyzerAllDay);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventFileName);
		
		//find out iteration number from events file name
		File strObjEventFile  = new File(eventFileName);
		String strIntNum= FacilityUtils.getStrUntilPoint(strObjEventFile.getName());
		int itNum= NumberUtils.isNumber(strIntNum) ? Integer.valueOf (strIntNum) : 0;	
		
		//write kml file 
		Controler controler = new Controler(config);
		KMZPtCountSimComparisonWriter kmzPtCountSimComparisonWritter = new KMZPtCountSimComparisonWriter (controler);
		kmzPtCountSimComparisonWritter.write(occupancyAnalyzerAllDay.getOccuAnalyzer(), itNum);
	}

}
