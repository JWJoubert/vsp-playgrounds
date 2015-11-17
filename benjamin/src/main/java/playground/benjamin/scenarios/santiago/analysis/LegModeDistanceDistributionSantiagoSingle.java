/* *********************************************************************** *
 * project: org.matsim.*
 * RunLegModeDistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.santiago.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution.RunLegModeDistanceDistribution;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistributionSantiagoSingle {
	private final static Logger logger = Logger.getLogger(LegModeDistanceDistributionSantiagoSingle.class);
	
	static String baseFolder = "../../../runs-svn/santiago/run32/output/";
	static String configFile = baseFolder + "output_config.xml.gz";
	static String iteration = "0";
	
	//TODO: find some good solution here
	static UserGroup userGroup = null;

	public static void main(String[] args) {
		Gbl.startMeasurement();
		RunLegModeDistanceDistribution rlmdd = new RunLegModeDistanceDistribution(baseFolder, configFile, iteration, userGroup);
		rlmdd.run();
	}
}