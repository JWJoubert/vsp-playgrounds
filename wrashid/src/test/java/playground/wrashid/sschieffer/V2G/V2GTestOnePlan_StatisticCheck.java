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

package playground.wrashid.sschieffer.V2G;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.LoadDistributionInterval;
import playground.wrashid.sschieffer.DSC.Schedule;
import playground.wrashid.sschieffer.DSC.TimeDataCollector;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TestSimulationSetUp;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checks if adding revenue from V2G works and statistik calc function works
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_StatisticCheck extends TestCase{

	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double electrification= 1.0; 
	// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
	final double ev=0.0;
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
	double compensationPerKWHRegulationUp=0.15;
	double compensationPerKWHRegulationDown=0.15;
	double compensationPERKWHFeedInVehicle=0.15;
	double xPercentNone=0;
	double xPercentDown=0;
	double xPercentDownUp=1.0;
	
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
		
		final TestSimulationSetUp mySimulation = new TestSimulationSetUp(
				configPath, 
				electrification, 
				ev 
				);
		
		controler= mySimulation.getControler();
		
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					mySimulation.setUpStochasticLoadDistributions();
					myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					myDecentralizedSmartCharger.run();
					
				
					// all day 3500
					HashMap<Integer, Schedule> stochasticLoad = mySimulation.getStochasticLoadSchedule();
						
					HashMap<Id, Schedule> agentVehicleSourceMapping =mySimulation.getAgentStochasticLoadSources();
					
					DecentralizedSmartCharger.linkedListIntegerPrinter(stochasticLoad, "Before stochastic general");
					
					DecentralizedSmartCharger.linkedListIdPrinter(agentVehicleSourceMapping,  "Before agent");
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							null, 
							agentVehicleSourceMapping);
					
					mySimulation.setUpAgentSchedules(
							myDecentralizedSmartCharger, 
							compensationPerKWHRegulationUp, 
							compensationPerKWHRegulationDown,
							compensationPERKWHFeedInVehicle,
							xPercentDown, 
							xPercentDownUp);
					
					myDecentralizedSmartCharger.setAgentContracts(mySimulation.getAgentContracts());
					
					myDecentralizedSmartCharger.initializeAndRunV2G(
							xPercentDown, xPercentDownUp);
					
					double revenue=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
					
					myDecentralizedSmartCharger.myV2G.addRevenueToAgentFromV2G(100.0, agentOne);
					
					double revenueNew=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
					
					assertEquals(revenue+100.0, revenueNew);
					
					
					/**********************************************
					 *  * CHECK
					 * addJoulesV2G
					 */
					
					double down = myDecentralizedSmartCharger.myV2G.getTotalRegulationUp();
					myDecentralizedSmartCharger.myV2G.addJoulesUpDownToAgentStats(1000.0, agentOne);
					double newDown = myDecentralizedSmartCharger.myV2G.getTotalRegulationUp();
					myDecentralizedSmartCharger.myV2G.calcV2GVehicleStats();
					assertEquals(1000.0, newDown-down);
					
					down = myDecentralizedSmartCharger.myV2G.getTotalRegulationDown();
					myDecentralizedSmartCharger.myV2G.addJoulesUpDownToAgentStats(-1000.0, agentOne);
					newDown = myDecentralizedSmartCharger.myV2G.getTotalRegulationDown();
					myDecentralizedSmartCharger.myV2G.calcV2GVehicleStats();
					assertEquals(1000.0, down-newDown);
					
					/**********************************************
					 *  * CHECK
					 * sensible outcome averageV2G
					 */
					
					assertEquals(myDecentralizedSmartCharger.myV2G.getAverageV2GRevenuePHEV(), myDecentralizedSmartCharger.myV2G.getAverageV2GRevenueAgent());
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	
	
	
	
}


