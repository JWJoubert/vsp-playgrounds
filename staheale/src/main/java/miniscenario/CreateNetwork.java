/* *********************************************************************** *
 * project: org.matsim.*
 * CreateNetwork.java
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

package miniscenario;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;


public class CreateNetwork {
	private ScenarioImpl scenario = null;	
	private Config config = null;
	public static ArrayList<String> days = new ArrayList<String>(Arrays.asList("mon", "tue", "wed", "thu", "fri", "sat", "sun"));
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk"));
	public static final String AGENT_INTERACTION_RUN = "agent_interaction_run";
	public static final String AGENT_INTERACTION_PREPROCESS = "agent_interaction_preprocess";
	
	private final static Logger log = Logger.getLogger(CreateNetwork.class);		
	
	public void createNetwork(ScenarioImpl scenario, Config config) {
		this.scenario = scenario;
		this.config = config;
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(this.scenario.getNetwork());
		
		this.addNodes(networkFactory);
		this.addLinks(networkFactory);
		
		this.write(config.findParam(AGENT_INTERACTION_PREPROCESS, "outPath"));
	}
			
	private void addLinks(NetworkFactoryImpl networkFactory) {		
		int linkCnt = 0;
		int facilityCnt = 0;
		double freeSpeed = 35.0 / 3.6;
		
		double sideLength = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "spacing"));
		double linkCapacity = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "linkCapacity"));
		
		int stepsPerSide = (int)(sideLength / spacing);
		
		for (int i = 0; i <= stepsPerSide ; i++) {
			
			for (int j = 0; j <= stepsPerSide; j++) {
				Id fromNodeId = new IdImpl(Integer.toString(i * (stepsPerSide + 1) + j));
				Node fromNode = this.scenario.getNetwork().getNodes().get(fromNodeId);
							
				if (j > 0) {
					// create backward link
					Id toNodeId = new IdImpl(Integer.toString(i * (stepsPerSide + 1) + j - 1));
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);
					
					Link l0 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);				
					l0.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;
					this.addFacility(l0, facilityCnt);
					facilityCnt++;						
					
					Link l1 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(((CoordImpl)toNode.getCoord()).calcDistance(fromNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}				
				
				if (i > 0) {
					// create downward link
					Id toNodeId = new IdImpl(Integer.toString((i - 1) * (stepsPerSide + 1) + j));
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);
					
					Link l0 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);
					l0.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;
					
					this.addFacility(l0, facilityCnt);						
					facilityCnt++;
					
					Link l1 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}
				
			}
		}
		log.info("Created " + linkCnt + " links");
		log.info("Created " + facilityCnt + " facilities");
		log.info("number of work facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("work").size());
		log.info("number of shop retail facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("shop_retail").size());
		log.info("number of shop service facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("shop_service").size());
		log.info("number of sports & fun facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("sports_fun").size());
		log.info("number of gastro & culture facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("gastro_culture").size());

	}
	
	private void addFacility(Link l, int facilityId) {
		int idnumber = facilityId;
		IdImpl id = new IdImpl(Integer.toString(facilityId));
		this.scenario.getActivityFacilities().createFacility(id, l.getCoord());

		Random random = new Random(4711+idnumber);
		ActivityFacilityImpl facility = (ActivityFacilityImpl)(this.scenario.getActivityFacilities().getFacilities().get(id));
		facility.createActivityOption("home");
		facility.createActivityOption("work");
		if (random.nextDouble()<0.0085){
    		facility.createActivityOption("shop_retail");
    		}
    	if (random.nextDouble()<0.008){
    		facility.createActivityOption("shop_service");
    		//log.info("created shop service facility");
    		}
    	if (random.nextDouble()<0.004){
    		facility.createActivityOption("sports_fun");
    		}
    	if (random.nextDouble()<0.012){
    		facility.createActivityOption("gastro_culture");
    		}
								
		ActivityOptionImpl actOptionHome = (ActivityOptionImpl)facility.getActivityOptions().get("home");
		OpeningTimeImpl opentimeHome = new OpeningTimeImpl(DayType.wk, 0.0 * 3600.0, 24.0 * 3600);
		actOptionHome.addOpeningTime(opentimeHome);
		
		ActivityOptionImpl actOptionWork = (ActivityOptionImpl)facility.getActivityOptions().get("work");
		OpeningTimeImpl opentimeWork = new OpeningTimeImpl(DayType.wk, 6.0 * 3600.0, 20.0 * 3600);
		actOptionWork.addOpeningTime(opentimeWork);
		
		if (facility.getActivityOptions().containsKey("shop_retail")){
			ActivityOptionImpl actOptionShopRetail = (ActivityOptionImpl)facility.getActivityOptions().get("shop_retail");
			OpeningTimeImpl opentimeShopRetail = new OpeningTimeImpl(DayType.wk, 7.5 * 3600.0, 18.0 * 3600);
			actOptionShopRetail.addOpeningTime(opentimeShopRetail);
			double cap = 2+random.nextInt(200);
			actOptionShopRetail.setCapacity(cap);
		}
		if (facility.getActivityOptions().containsKey("shop_service")){
			ActivityOptionImpl actOptionShopService = (ActivityOptionImpl)facility.getActivityOptions().get("shop_service");
			OpeningTimeImpl opentimeShopService = new OpeningTimeImpl(DayType.wk, 8.0 * 3600.0, 18.0 * 3600);
			actOptionShopService.addOpeningTime(opentimeShopService);
			double cap = 2+random.nextInt(29);
			actOptionShopService.setCapacity(cap);
			//log.info("shop service opentimes added");
		}
		if (facility.getActivityOptions().containsKey("sports_fun")){
			ActivityOptionImpl actOptionSportsFun = (ActivityOptionImpl)facility.getActivityOptions().get("sports_fun");
			OpeningTimeImpl opentimeSportsFun = new OpeningTimeImpl(DayType.wk, 9.0 * 3600.0, 24.0 * 3600);
			actOptionSportsFun.addOpeningTime(opentimeSportsFun);
			double cap = 2+random.nextInt(44);
			actOptionSportsFun.setCapacity(cap);
		}
		if (facility.getActivityOptions().containsKey("gastro_culture")){
			ActivityOptionImpl actOptionGastroCulture = (ActivityOptionImpl)facility.getActivityOptions().get("gastro_culture");
			OpeningTimeImpl opentimeGastroCulture = new OpeningTimeImpl(DayType.wk, 9.0 * 3600.0, 24.0 * 3600);
			actOptionGastroCulture.addOpeningTime(opentimeGastroCulture);
			double cap = 2+random.nextInt(61);
			actOptionGastroCulture.setCapacity(cap);
		}
		facility.setLinkId(l.getId());
	}
			
	private void addNodes(NetworkFactoryImpl networkFactory) {		
		double sideLength = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "spacing"));
		
		int nodeCnt = 0;
		int stepsPerSide = (int)(sideLength/ spacing);
		for (int i = 0; i <= stepsPerSide ; i++) {
			for (int j = 0; j <= stepsPerSide; j++) {
				Node n = networkFactory.createNode(new IdImpl(Integer.toString(nodeCnt)), new CoordImpl(i * spacing, j * spacing));
				this.scenario.getNetwork().addNode(n);
				nodeCnt++;
			}
		}
		log.info("Created " + nodeCnt + " nodes");
	}
	
	public void write(String path) {
		new File(path).mkdirs();
		this.writeNetwork(path);
		this.writeFacilities(path);
	}
			
	private void writeNetwork(String path) {
		log.info("Writing network ...");
		new NetworkWriter(this.scenario.getNetwork()).write(path + "network.xml");
	}
	
	private void writeFacilities(String path) {
		log.info("Writing facilities ...");
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(path + "facilities.xml");
	}
}
