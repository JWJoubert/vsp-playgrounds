/* *********************************************************************** *
 * project: org.matsim.*
 * LPTest.java
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.StringTokenizer;

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

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * at example schedule, test if the LP is set up correctly
 *<li>objective
 *<li>(in)equalities
 *<li>bounds
 *
 * @author Stella
 *
 */
public class LPEVTest extends TestCase{

	
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	
	final double phev=0.0;
	final double ev=1.0;
	final double combustion=0.0;
	
	private double chargingSpeed;
	
	private TestSimulationSetUp mySimulation;
	private Controler controler;
	
	final double bufferBatteryCharge=0.0;
	
	final double MINCHARGINGLENGTH=5*60;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	final static double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
	final static double suboptimalPrice=optimalPrice*3; // cost/second  
	
	public Id agentOne;
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
	public void testLPEV() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		mySimulation = new TestSimulationSetUp(
				configPath, 
				phev, 
				ev, 
				combustion);
		
		controler= mySimulation.getControler();
		
		
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					HubLinkMapping hubLinkMapping = mySimulation.mapHubsTest();
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							mySimulation.getParkingTimesPlugIn(),
							mySimulation.getEnergyConsumptionInit().getEnergyConsumptionPlugin(),
							outputPath,
							mySimulation.getVehicleTypeCollector()
							);
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(MINCHARGINGLENGTH);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							mySimulation.getEnergyConsumptionInit().getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							mySimulation.getDeterministicLoadSchedule(),							
							mySimulation.getDetermisiticPricing()
							);
					
					/***********************************
					 * LP TEST
					 * *********************************
					 */
					
					
					for(Id id : controler.getPopulation().getPersons().keySet()){
						
							agentOne=id;
							
							Schedule testSchedule = makeFakeSchedule();
							
							testSchedule=myDecentralizedSmartCharger.lpev.solveLP(testSchedule, 
									id, 
									100, 
									0.1,
									0.9, 
									"lpevTEST"
									);
							
							testSchedule.printSchedule();
							String name= outputPath+"DecentralizedCharger\\LP\\EV\\LP_agent"+ id.toString()+"printLp.txt";
							
							 try
							    {
							       FileReader fro = new FileReader( name );
							       BufferedReader bro = new BufferedReader( fro );
							       
							       // declare String variable and prime the read
							       String stringRead = bro.readLine( ); // model name
							       stringRead = bro.readLine( ); // C1 C2...
							       
							       //*********************
							       String objStringRead = bro.readLine( );  
							       // Minimize        -1 -0.0434251        0 -0.146437        0 -0.810138        1 
							       StringTokenizer st = new StringTokenizer(objStringRead);
							       st.nextToken(); // minimize
							       
							       
							       /*
							        * OBJECTIVE FUNCTION
							        * /*
									 * /*
										 * Parking 0  10  true  joules =100
										 * Parking 10  20 false joules =100
										 * Driving 20  30  consumption =-10
										 * Parking 30  40  false joules =100
										 *
									 */
							
							       
							       //SOC=-1  
							       String next= st.nextToken(); 
							       System.out.println(next);
							       							       
							       assertEquals(Integer.toString(-1), next);
							       /*
							        * optimal weight
							        * (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
							        * 
							        */
							       
							       next= st.nextToken(); 
							       System.out.println(next);
							       double expected= -1.0*100.0/100.0;							       
							       int expectedInt = (int)expected;
							       assertEquals(Integer.toString(expectedInt), next);
							       /*
							        * Parking suboptimal
							        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
							        * 
							        */
							       next= st.nextToken(); 
							       System.out.println(next);
							       expected= 100.0/200.0;
							       assertEquals(Double.toString(expected), next);
								     /*
								      * Driving 0
								      * 
								      */
							      
							       assertEquals(Integer.toString(0), st.nextToken());
							       
							       /*
							        * Parking suboptimal
							        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
							        *  
							        */
							       next= st.nextToken(); 
							       System.out.println(next);
							       expected= 100.0/200.0;							       
							       assertEquals(Double.toString(expected), next);
							       
							       
							     //*********************
							       String constraint1 = bro.readLine( ); 
							       //R1               1     3500        0        0        0        0        0 <=       90
							       
							       st = new StringTokenizer(constraint1);
							       st.nextToken();
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(3500), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							      
							       
							       //*********************
							       String constraint2 = bro.readLine( );// R2               1     3500        0        0        0        0        0 >=       10
							       //*********************
							       String constraint3 = bro.readLine( );//R3               1     3500 -1.87863e+007        0        0        0        0 <=       90
							       //*********************
							       String constraint4 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							       //*********************
							       String constraint5 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
								     //*********************
								   String constraint6 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							     //*********************
							       String constraint7 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							     //*********************
							       String constraint8 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							     //*********************
							       String constraint9 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							       st = new StringTokenizer(constraint9);
							       st.nextToken();
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(3500), st.nextToken());
							       assertEquals(Integer.toString(3500), st.nextToken());
							       
							       //*********************
							        //*********************
							       stringRead = bro.readLine( ); // Type..
							       //*********************
							       String upBo = bro.readLine();
							       //upbo            90    21600        1    13200        1    24450    23910
							       st = new StringTokenizer(upBo);
							       st.nextToken();
							       assertEquals(Integer.toString(90), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							     
							       //*********************	
							       
							       String lowBo = bro.readLine( );
							       //lowbo           10        0        1        0        1        0        0 
							       st = new StringTokenizer(lowBo);
							       st.nextToken();
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							      
							       
							       
							       
							       bro.close( );
							    }
							 
							    catch( FileNotFoundException filenotfoundexxption )
							    {
							      System.out.println( name +" , does not exist" );
							    }
							 
							    catch( IOException ioexception )
							    {
							      ioexception.printStackTrace( );
							    }
						}
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	
		
	}
	
	
	
		
	public Schedule makeFakeSchedule(){
		Id linkId=null;
		for (Link link:controler.getNetwork().getLinks().values())
		{
			linkId=link.getId();
			break;
		}
		/*
		 * Parking 0  10  true  joules =100
		 * Parking 10  20 false joules =100
		 * Driving 20  30  consumption =-10
		 * Parking 30  40  false joules =100
		 */
		Schedule s= new Schedule();
		ParkingInterval p1= new ParkingInterval (0, 10, linkId);
		p1.setParkingOptimalBoolean(true);
		p1.setJoulesInPotentialChargingInterval(100);
		
		ParkingInterval p2= new ParkingInterval (10, 20, linkId);
		p2.setParkingOptimalBoolean(false);
		p2.setJoulesInPotentialChargingInterval(-100);
		
		DrivingInterval d3 = new DrivingInterval(20, 30, 1);
		
		ParkingInterval p4= new ParkingInterval (30, 40, linkId);
		p4.setParkingOptimalBoolean(false);
		p4.setJoulesInPotentialChargingInterval(-100);
		
		s.addTimeInterval(p1);
		s.addTimeInterval(p2);
		s.addTimeInterval(d3);
		s.addTimeInterval(p4);
		
		s.addJoulesToTotalSchedule(100);
		s.addJoulesToTotalSchedule(-200);
		
		chargingSpeed= p1.getChargingSpeed();
		return s;
	}
	
	

	
}


