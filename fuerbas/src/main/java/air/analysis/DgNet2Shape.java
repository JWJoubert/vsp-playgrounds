/* *********************************************************************** *
 * project: org.matsim.*
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

package air.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
/**
 * @author dgrether
 */
public class DgNet2Shape {

	public static void writeNetwork2Shape(String networkFilename, String shapeFilename){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network  net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		//write shape file
		final WidthCalculator wc = new WidthCalculator() {
			@Override
			public double getWidth(Link link) {
				return 1.0;
			}
		};
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder() {
			@Override
			public FeatureGenerator createFeatureGenerator() {
				FeatureGenerator fg = new LineStringBasedFeatureGenerator(wc, MGC.getCRS(TransformationFactory.WGS84));
				return fg;
			}
		};
		
		
		new Links2ESRIShape(net, shapeFilename, builder).write();
	}
	
	public static void main(String[] args) {
		String networkFilename = "/home/soeren/workspace/germanAirNetwork.xml";
		String shapeFilename = "/home/soeren/workspace/networkDE.shp";
		writeNetwork2Shape(networkFilename, shapeFilename);
	}

}



