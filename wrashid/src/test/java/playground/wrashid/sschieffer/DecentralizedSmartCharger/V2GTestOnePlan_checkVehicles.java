/* *********************************************************************** *
 * project: org.matsim.*
 * V2GTestOnePlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * tests methods:
 * <li> checkVehicle sources
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_checkVehicles extends TestCase{

	/**
	 * @param args
	 */
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final Controler controler=new Controler(configPath);
	
	
	public Id agentOne=null;
	
	public static V2G myV2G;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	public static void testMain(String[] args) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException {
		
	}
	

	/**
	*  
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void testV2GCheckVehicles() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		//--> gas Price per second for PHEV 4.6511627906976747E-4
		
		
		double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
		double suboptimalPrice=optimalPrice*3; // cost/second  
		//0.24*10^-4 EUR per second
			
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readHubsPricingTest(optimalPrice, suboptimalPrice);
		
		
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		
		final double bufferBatteryCharge=0.0;
		
		
		SetUpVehicleCollector sv= new SetUpVehicleCollector();
		final VehicleTypeCollector myVehicleTypes = sv.setUp();		
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		
		controler.addControlerListener(e);
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					mapHubsTest(controler,hubLinkMapping);
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath,
							myVehicleTypes
							);
					
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(MINCHARGINGLENGTH);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution
							);
					
					myDecentralizedSmartCharger.run();
					
					/***********************************
					 * V2G
					 * *********************************
					 */
					
					LinkedListValueHashMap<Integer, Schedule> stochasticLoad = 
						readStochasticLoad(1);
						
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping =
						makeAgentVehicleSourceNegativeAndPositive(controler);
					
					linkedListIntegerPrinter(stochasticLoad, "Before stochastic general");
					
					linkedListIdPrinter(agentVehicleSourceMapping,  "Before agent");
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							null, 
							agentVehicleSourceMapping);
					
					// setting agent Contracts
					
					double compensationPerKWHRegulationUp=0.15;
					double compensationPerKWHRegulationDown=0.15;
					 
					AgentContractCollector myAgentContractsCollector= new AgentContractCollector (
							myDecentralizedSmartCharger,
							 compensationPerKWHRegulationUp,
							 compensationPerKWHRegulationDown);
					
					LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts= 
						myAgentContractsCollector.makeAgentContracts(
								controler,
								0,
								0,
								1);
					
					myDecentralizedSmartCharger.setAgentContracts(agentContracts);
					
					
					
					// instead of initalize and run go through steps
					System.out.println("START CHECKING VEHICLE SOURCES");
					
					/*
					 * CHECK reduceAgentVehicleLoadsByGivenLoadInterval
					 */
					
					for(Id id: controler.getPopulation().getPersons().keySet()){
						agentOne=id;
						System.out.println("AGENT VEHICLE SOURCE BEFORE V2G of -3500 between 0-300");
						agentVehicleSourceMapping.getValue(id).printSchedule();
					}
					
					myDecentralizedSmartCharger.initializeAndRunV2G();
						
					
						System.out.println("AGENT VEHICLE SOURCE BEFORE V2G of -3500 between 0-300");
						agentVehicleSourceMapping.getValue(agentOne).printSchedule();
						
						
						LoadDistributionInterval lFirst= 
							(LoadDistributionInterval)agentVehicleSourceMapping.getValue(agentOne).timesInSchedule.get(0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0], 
								0.0);
					
					
										
					/*
					 * reassign stochastic load and check results after  checkVehicleLoads()
					 * and checkHubStochasticLoads();
					 * 
					public void testFindAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(){
						
					}
					
					public void testReassignJoulesToSchedule(){
						
					}
					 */
					
					//System.out.println("START CHECKING STOCHASTIC HUB LOADS");
					//myDecentralizedSmartCharger.checkHubStochasticLoads();
					
					
					/*
					linkedListIntegerPrinter(stochasticLoad, "After stochastic general");
					linkedListIdPrinter(agentVehicleSourceMapping,  "After agent");
					*/
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	public void linkedListIdPrinter(LinkedListValueHashMap<Id, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Id id: list.getKeySet()){
			list.getValue(id).printSchedule();
		}
		
	}
	
	public void linkedListIntegerPrinter(LinkedListValueHashMap<Integer, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Integer id: list.getKeySet()){
			list.getValue(id).printSchedule();
		}
		
	}
	
	
	
	
	public LinkedListValueHashMap<Id, Schedule> makeAgentVehicleSourceNegativeAndPositive(Controler controler){
		LinkedListValueHashMap<Id, Schedule> agentSource= 
			new LinkedListValueHashMap<Id, Schedule>();
		
		//Id
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(0, 2000.0, pMinus, false));
				
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(20000.0, 20300.0, pPlus, true));
				
				
				agentSource.put(id, bullShitMinus);	
			
			
		}
		return agentSource;
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		
		return hubLoadDistribution1;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
		return hubLoadDistribution1;
		
	}
	
	
	
	
	public static Schedule makeBullshitScheduleTest() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{10};// 
		double[] bullshitCoeffs2 = new double[]{-10};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
	
		bullShitSchedule.addTimeInterval(l2);
		//bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}
	
	
	public void mapHubsTest(Controler controler, HubLinkMapping hubLinkMapping){
		
		
		for (Link link:controler.getNetwork().getLinks().values()){
			
			hubLinkMapping.addMapping(link.getId().toString(), 1);
			}
			
	}
	
	
	
public static LinkedListValueHashMap<Integer, Schedule> readStochasticLoad(int num){
		
		LinkedListValueHashMap<Integer, Schedule> stochastic= new LinkedListValueHashMap<Integer, Schedule>();
		
		Schedule bullShitStochastic= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShitStochastic.addTimeInterval(new LoadDistributionInterval(0, 24*3600, p, true));
		for (int i=0; i<num; i++){
			stochastic.put(i+1, bullShitStochastic);
		}
		return stochastic;
	
		
	}
	
		
	

public static Schedule makeBullshitPricingScheduleTest(double optimal, double suboptimal) throws IOException{
	
	Schedule bullShitSchedule= new Schedule();
	
	PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});	
	PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
	
	
	LoadDistributionInterval l1= new LoadDistributionInterval(
			0.0,
			62490.0,
			pOpt,//p
			true//boolean
	);
	
	bullShitSchedule.addTimeInterval(l1);
	
	
	LoadDistributionInterval l2= new LoadDistributionInterval(					
			62490.0,
			DecentralizedSmartCharger.SECONDSPERDAY,
			pSubopt,//p
			false//boolean
	);

	bullShitSchedule.addTimeInterval(l2);
	//bullShitSchedule.printSchedule();
	
	return bullShitSchedule;
}

	
	
	

	
}


