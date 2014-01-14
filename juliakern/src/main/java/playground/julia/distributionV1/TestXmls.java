/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.julia.distributionV1;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;



public class TestXmls {

	public static void main (String[] args){
		
		 String stream = "input/sample/events.xml";
		EventWriterXML mxw = new EventWriterXML(stream);
		
		for(int i=1; i<11; i++){
			for(int j = 1; j<11; j++){
			Id agentId = new IdImpl("person_"+i+"_"+j);
			Id linkId = new IdImpl("link_"+i+"_"+j);
			Event event = new ActivityStartEvent(0, agentId, linkId, null, "home");
			mxw.handleEvent(event);
			}
		}
		
		mxw.closeFile();
		System.out.println("done");
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl) sc.getNetwork();
		for(int i=0; i<10; i++){
			for(int j=0; j<10; j++ ){
				Coord coordA = sc.createCoord(i*10+5, j*10+5);
				String nodeAs = "Node "+i+"_"+j+"A";
				//Node node1 = network.createAndAddNode(sc.createId("node 1"), sc.createCoord(-20000.0,     0.0));
				Node nodeA = network.createAndAddNode(sc.createId(nodeAs), coordA);
				Coord coordB = sc.createCoord(i*10+6, j*10+6);
				String nodeBs = "Node "+i+"_"+j+"B";
				Node nodeB = network.createAndAddNode(sc.createId(nodeBs), coordB);
				Id linkId = new IdImpl("link_"+i+"_"+j);
				network.createAndAddLink(linkId, nodeA, nodeB, 20., 30., 3600, 1, null, null);
			}
		}
		
		NetworkWriter nw = new NetworkWriter(network);
		nw.write("input/sample/test_network.xml");
		
		
		Population pop = sc.getPopulation();
		for(int i=1; i<11; i++){
			for(int j = 1; j<11; j++){
				Person person = pop.getFactory().createPerson(new IdImpl("person" +i+"_"+j));
				Coord coord = sc.createCoord(i*10-5, j*10-5);
				Activity homeAct = pop.getFactory().createActivityFromCoord("home", coord );
				Plan plan = pop.getFactory().createPlan();
				plan.addActivity(homeAct);
				person.addPlan(plan);
				pop.addPerson(person);
			}
		}
		
		PopulationWriter pw = new PopulationWriter(pop, network);
		pw.write("input/sample/test_plans.xml");
		
	}
	
	
}
