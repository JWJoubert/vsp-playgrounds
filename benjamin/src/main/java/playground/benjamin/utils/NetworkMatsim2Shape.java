/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNet2Shape
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.benjamin.utils;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;


/**
 * @author benjamin
 *
 */
public class NetworkMatsim2Shape {
	
	private static Logger log = Logger.getLogger(NetworkMatsim2Shape.class);
	
	private static String filePath = "../../detailedEval/Net/";
	private static String networkName = "network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes";
//	private static String networkName = "../policies/network-86-85-87-84_simplified---withLanes_zone30";
	private static String inFileType = ".xml";
//	private static String inFileType = ".xml.gz";
	private static String outFileType = ".shp";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = filePath + networkName + inFileType;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		log.info("loading network from " + netFile);
		NetworkImpl net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
		log.info("done.");

		new Links2ESRIShape(net, filePath + networkName + outFileType, "DHDN_GK4").write();
	}

}
