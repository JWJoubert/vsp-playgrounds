
/* *********************************************************************** *
 * project: org.matsim.*
 * Main_exampleV2G.java
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

//package playground.wrashid.sschieffer;
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G.StochasticLoadCollector;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.HubInfo;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.StellasHubMapping;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.DetermisticLoadPricingCollector;

import java.util.*;


/**
 * This class highlights how to use V2G functions of this package:
 * 
 * V2G can only follow after the Decentralized Smart Charger has been run
 * since its based on the charging schedules planned by the Decentralized Smart Charger
 * 
 * For all specified stochastic loads the V2G procedure will then check if rescheduling is possible 
 * and if the utility of the agent can be increased by rescheduling.
 * 
 * 
 * PLease provide the following folders in the output path to store results locally
 * please provide a folder Output with the following sub-folders
 * <ul>
 * 		<li>DecentralizedCharger
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP	
 * 					<ul><li>EV <li> PHEV</ul>
 * 			</ul>
 * 		<li>Hub
 * 		<li>V2G
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP
 * 					<ul><li>EV <li> PHEV</ul>
 *  		</ul>
 * </ul>
 * @author Stella
 *
 */
public class Main_exampleV2G {
	
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
		/*************
		 * SET UP STANDARD Decentralized Smart Charging simulation 
		 * *************/
		
		final double electrification= 1.0; 
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=0.0; 
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\20000Plans15Min\\";
		String configPath="test/input/playground/wrashid/sschieffer/config_plans20000.xml";// 100 agents
		//String configPath="test/input/playground/wrashid/sschieffer/config_plans1_8797.xml";
		
		double priceMaxPerkWh=0.11;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWh=0.07;
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		//String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_1000.txt";
		ArrayList<HubInfo> myHubInfo = new ArrayList<HubInfo>(0);
		myHubInfo.add(new HubInfo(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
		final double standardChargingLength=15.0*DecentralizedSmartCharger.SECONDSPERMIN;
		final double bufferBatteryCharge=0.0;
		
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		
		DecentralizedChargingSimulation mySimulation= new DecentralizedChargingSimulation(
				configPath, 
				outputPath, 
				electrification, ev,
				bufferBatteryCharge,
				standardChargingLength,
				myMappingClass,
				myHubInfo,
				false  // indicate if you want graph output for every agent to visualize the SOC over the day
				
				);
		
		/******************************************
		 * SETUP V2G
		 * *****************************************
		
		/**
		 * SPECIFY WHAT PERCENTAGE OF THE POPULATION PROVIDES V2G
		 * <li> no V2G
		 * <li> only down (only charging)
		 * <li> up and down (charging and discharging)
		 * 
		 * i.e. 0/0.5/0.5
		 * 50% do only down, 50% do up and down
		 */
		
		final double xPercentDown=0.0;
		final double xPercentDownUp=1.0;
		
		/*
		 * ************************
		 * Stochastic Sources
		 * they need to be defined per scenario. dummy values for the demonstration
		 * have been entered in StochasticLoadCollector
		 * 
		 * StochasticLoadSources can be defined
		 * <li> general hub Stochastic Load
		 * <li> for vehicles (i.e. solar roof)
		 * <li> local sources for hubs (wind turbine, etc)
		 * 
		 * the sources are given as Schedules of LoadDistributionIntervalsdistribution [Watt]
		 * <li> negative values mean, that the source needs energy --> regulation up
		 * <li> positive values mean, that the source has too much energy--> regulation down
		 * 
		 * THe LoadDistributionIntervals indicate 
		 * <li> a time interval: start second, end second 
		 * <li>PolynomialFunction indicating the free Watts over the time interval
		 * <li> an optimality boolean(true, if stochastic load is positive=electricity for charging available and false if not)
		 * </br>
		 */			
					
		
		double compensationPerKWHRegulationUp=0.1;
		double compensationPerKWHRegulationDown=0.005;
		double compensationPERKWHFeedInVehicle=0.005;
		
		mySimulation.setUpV2G(				
				xPercentDown,
				xPercentDownUp,
				new StochasticLoadCollector(mySimulation),
				compensationPerKWHRegulationUp,
				compensationPerKWHRegulationDown,
				compensationPERKWHFeedInVehicle);
	
		mySimulation.controler.run();
		
		/*********************
		 * Example how to Use V2G
		 *********************/
		
		// REVENUES FROM V2G
		System.out.println("average revenue from V2G for agents: "+mySimulation.getAverageRevenueV2GPerAgent());
		
		System.out.println("average revenue from V2G for EV agents: "+mySimulation.getAverageRevenueV2GPerEV());
				
		System.out.println("average revenue from V2G for PHEV agents: "+mySimulation.getAverageRevenueV2GPerPHEV());
		
		
		//JOULES PROVIDED IN REGULATION UP OR DOWN
		System.out.println("total joules from V2G for regulation up: "+mySimulation.getTotalJoulesV2GRegulationUp());
		
		System.out.println("total joules from V2G for regulation up from EVs: "+mySimulation.getTotalJoulesV2GRegulationUpEV());
		
		System.out.println("total joules from V2G for regulation up from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationUpPHEV());
		
		
		//JOULES PROVIDED IN REGULATION UP OR DOWN
		System.out.println("total joules from V2G for regulation down: "+mySimulation.getTotalJoulesV2GRegulationDown());
		
		System.out.println("total joules from V2G for regulation down from EVs: "+mySimulation.getTotalJoulesV2GRegulationDownEV());
		
		System.out.println("total joules from V2G for regulation down from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationDownPHEV());
		
	
	}

}
