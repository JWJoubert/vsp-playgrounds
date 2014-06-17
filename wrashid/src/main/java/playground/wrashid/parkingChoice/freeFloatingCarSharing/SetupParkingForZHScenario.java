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
package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunctionFactory;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class SetupParkingForZHScenario {

	public static void prepare(ParkingModuleWithFFCarSharingZH parkingModule, Controler controler){
		Config config = controler.getConfig();
		
		String baseDir = config.getParam("parkingChoice.ZH", "parkingDataDirectory");
		
		LinkedList<Parking> parkings = getParking(config, baseDir);
	
		ParkingScoreManager parkingScoreManager = prepareParkingScoreManager(parkingModule);
		ParkingInfrastructureManager pim=new ParkingInfrastructureManager(parkingScoreManager,  controler.getEvents());
		
		ParkingCostModel pcm=new ParkingCostModelZH(config,parkings);
		LinkedList<PublicParking> publicParkings=new LinkedList<PublicParking>();
		LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities=new LinkedList<PPRestrictedToFacilities>();
		for (Parking parking: parkings){
			String groupName=null;
			if (parking.getId().toString().contains("stp")){
				groupName="streetParking";
			} else if (parking.getId().toString().contains("gp")){
				groupName="garageParking";
			} else if (parking.getId().toString().contains("publicPOutsideCity")){
				groupName="publicPOutsideCity";
			}
			if (groupName!=null){
				PublicParking publicParking=new PublicParking(parking.getId(),parking.getIntCapacity(),parking.getCoord(),pcm,groupName);
				publicParkings.add(publicParking);
			} else {
				PrivateParking pp=(PrivateParking) parking;
				HashSet<Id> hs=new HashSet<Id>();
				hs.add(pp.getActInfo().getFacilityId());
				groupName="privateParking";
				PPRestrictedToFacilities PPRestrictedToFacilitiesTmp=new PPRestrictedToFacilities(parking.getId(),parking.getIntCapacity(),parking.getCoord(),pcm,groupName,hs);
				ppRestrictedToFacilities.add(PPRestrictedToFacilitiesTmp);
			}
		}
		
		pim.setPublicParkings(publicParkings); 
		pim.setPrivateParkingRestrictedToFacilities(ppRestrictedToFacilities);
		
		parkingModule.setParkingInfrastructurManager(pim);
		parkingModule.setParkingScoreManager(parkingScoreManager);
		appendScoringFactory(parkingModule);
	}

	private static LinkedList<Parking> getParking(Config config, String baseDir) {
		ParkingLoader.garageParkingCalibrationFactor=Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_garageParking"));
		ParkingLoader.parkingsOutsideZHCityScaling =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_publicPOutsideCity"));
		ParkingLoader.populationScalingFactor =Double.parseDouble(config.getParam("parkingChoice.ZH", "populationScalingFactor"));
		ParkingLoader.privateParkingCalibrationFactorZHCity  =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_privateParking"));
		ParkingLoader.streetParkingCalibrationFactor  =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_streetParking"));
		
		LinkedList<Parking> parkings = ParkingLoader.getParkingsForScenario(baseDir);
		return parkings;
	}
	
	public static void appendScoringFactory(ParkingModuleWithFFCarSharingZH parkingModule){
		parkingModule.getControler().setScoringFunctionFactory(new ParkingScoringFunctionFactory (parkingModule.getControler().getScoringFunctionFactory(),parkingModule.getParkingScoreManager()));
	}
	
	public static ParkingScoreManager prepareParkingScoreManager(ParkingModuleWithFFCarSharingZH parkingModule) {
		Controler controler=parkingModule.getControler();
		ParkingScoreManager parkingScoreManager = new ParkingScoreManager(getWalkTravelTime(parkingModule.getControler()), parkingModule.getControler());
		
		
		
		ParkingBetas parkingBetas=new ParkingBetas(getHouseHoldIncomeCantonZH(parkingModule.getControler().getPopulation()));
		parkingBetas.setParkingWalkBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingWalkBeta"));
		parkingBetas.setParkingCostBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingCostBeta"));
		parkingScoreManager.setParkingBetas(parkingBetas);
		
		double parkingScoreScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "parkingScoreScalingFactor"));
		parkingScoreManager.setParkingScoreScalingFactor(parkingScoreScalingFactor);
		double randomErrorTermScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "randomErrorTermScalingFactor"));
		parkingScoreManager.setRandomErrorTermScalingFactor(randomErrorTermScalingFactor);
		return parkingScoreManager;
	}
	
	// based on: playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh.getHouseHoldIncomeCantonZH
	public static DoubleValueHashMap<Id> getHouseHoldIncomeCantonZH(Population population) {
		DoubleValueHashMap<Id> houseHoldIncome=new DoubleValueHashMap<Id>();
		
		for (Id personId : population.getPersons().keySet()) {
			double rand = MatsimRandom.getRandom().nextDouble();
			if (rand<0.032) {
				houseHoldIncome.put(personId, 1000+MatsimRandom.getRandom().nextDouble()*1000);
			} else if (rand<0.206) {
				houseHoldIncome.put(personId, 2000+MatsimRandom.getRandom().nextDouble()*2000);
			} else if (rand<0.471) {
				houseHoldIncome.put(personId, 4000+MatsimRandom.getRandom().nextDouble()*2000);
			} else if (rand<0.674) {
				houseHoldIncome.put(personId, 6000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.803) {
				houseHoldIncome.put(personId, 8000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.885) {
				houseHoldIncome.put(personId, 10000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.927) {
				houseHoldIncome.put(personId, 12000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.952) {
				houseHoldIncome.put(personId, 14000+MatsimRandom.getRandom().nextDouble()*2000);
			} else {
				houseHoldIncome.put(personId, 16000+MatsimRandom.getRandom().nextDouble()*16000);
			}
		}
		
		return houseHoldIncome;
	}
	
	
	
	private static WalkTravelTime getWalkTravelTime(Controler controler){
		Map<Id, Double> linkSlopes=new HashMap<Id, Double>();
		String linkSlopeAttributeFile = controler.getConfig().getParam("parkingChoice.ZH", "networkLinkSlopes");
		ObjectAttributes lp = new ObjectAttributes();
		new ObjectAttributesXmlReader(lp).parse(linkSlopeAttributeFile);

		for (Id linkId : controler.getNetwork().getLinks().keySet()) {
			linkSlopes.put(linkId, (Double) lp.getAttribute(linkId.toString(), "slope"));
		}
		
		WalkTravelTime walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup(), linkSlopes);
		return walkTravelTime;
	}

}
