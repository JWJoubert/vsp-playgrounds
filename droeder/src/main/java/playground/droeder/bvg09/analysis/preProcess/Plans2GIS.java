/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.bvg09.analysis.preProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.droeder.DRPaths;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class Plans2GIS {
	
	private static final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";
	private static final String NET = DIR + "scenario/input/network-base_ext.xml.gz";
	private static final String POPULATION = DIR + "matsimOutput/congestion/selectedPlansCar.xml.gz";
	private static final String OUTPUTSHP = DIR + "matsimOutput/congestion/selectedPlansCar.shp";

//	private static final String NET = DRPaths.VSP + "BVG09_Auswertung/input/network.final.xml.gz";
//	private static final String POPULATION = DRPaths.VSP + "BVG09_Auswertung/testPopulation1.xml";
//	private static final String OUTPUTSHP = DRPaths.VSP + "BVG09_Auswertung/trips.shp";
	
	public static void main(String[] args){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(NET);
		new MatsimPopulationReader(sc).parse(POPULATION);
		
		Map<String, SortedMap<Integer, Coord>> lineStrings = new HashMap<String, SortedMap<Integer, Coord>>();
		
		for (Person p : sc.getPopulation().getPersons().values()){
			SortedMap<Integer, Coord> temp = new TreeMap<Integer, Coord>();
			int i = 0;
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					temp.put(i, ((Activity) pe).getCoord());
					i++;
				}
			}
			
			if(i<1){
				System.out.println("not enough points for Agent " + p.getId());
			}else{
				lineStrings.put(p.getId().toString(), temp);
			}
		}
		
		DaShapeWriter.writeDefaultLineString2Shape(OUTPUTSHP, "SlectedPlans", lineStrings, null);
	}

}
