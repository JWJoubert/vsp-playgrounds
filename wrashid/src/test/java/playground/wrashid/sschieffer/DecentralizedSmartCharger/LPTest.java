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
 * at example of config_plans1.xml checks if
 * the LPEV sets up the correct LP (objective, (in)equalities, bounds)
 * @author Stella
 *
 */
public class LPTest extends TestCase{

	/**
	 * @param args
	 */
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final Controler controler=new Controler(configPath);
	
	public Id agentOne=null;
	
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
	public void testLPEV() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		double energyPricePerkWh=0.25;
		double standardConnectionElectricityJPerSecond= 3500; 
		
		
		final double optimalPrice=energyPricePerkWh*1/1000*1/3600*standardConnectionElectricityJPerSecond;//0.25 CHF per kWh		
		final double suboptimalPrice=optimalPrice*3; // cost/second  
		final double gasPrice=optimalPrice*2;
			
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readHubsPricingTest(optimalPrice, suboptimalPrice);
		
		
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		final double phev=0.0;
		final double ev=1.0;
		final double combustion=0.0;
		
		final double bufferBatteryCharge=0.0;
		
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		GasType normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
		
	
		double batterySizeEV= 24*3600*1000; 
		double batterySizePHEV= 24*3600*1000; 
		double batteryMinEV= 0.1; 
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		Battery EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		Battery PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
		
		
		VehicleType EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				new ElectricVehicle(null, new IdImpl(1)),
				80000);// Nissan leaf 80kW Engine
		
		VehicleType PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				new PlugInHybridElectricVehicle(new IdImpl(1)),
				80000);
		
		final VehicleTypeCollector myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);
		
		
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
					
					LinkedListValueHashMap<Integer, Schedule> locationSourceMapping= new LinkedListValueHashMap<Integer, Schedule>();
					//hub/LoadDIstributionSchedule
					
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping= new LinkedListValueHashMap<Id, Schedule>();
					
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
					
					/***********************************
					 * LPTEST
					 * *********************************
					 */
					
					
					for(Id id : controler.getPopulation().getPersons().keySet()){
						
							agentOne=id;
							
							myDecentralizedSmartCharger.readAgentSchedules();
							
							/*
							Parking and Driving Times:
							*************************
							Starting SOC: 0.0
							Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
							Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
							Parking Interval 	 start: 38386.0	  end: 86400.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							*************************
							
							 */
							
							// all EV
							Schedule agentSchedule = myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(id);
							
							myDecentralizedSmartCharger.lpev.solveLP(agentSchedule, 
									id, 
									100, 
									0.1,
									0.9, 
									"lpevTEST"
									);
							
							LpSolve solver= myDecentralizedSmartCharger.lpev.getSolver();
							String name= DecentralizedSmartCharger.outputPath+"LP_agent"+ id.toString()+"TEST_printLp.txt";
							solver.setOutputfile(name);
							solver.printLp();
							
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
							       
							       String next= st.nextToken(); //-1
							       System.out.println(next);
							       System.out.println(Integer.toString(-1));							       
							       assertEquals(Integer.toString(-1), next);
							       
							       next= st.nextToken(); //-0.043...
							       System.out.println(next);
							       System.out.println(Double.toString(-0.0434251));								       
							       assertEquals(Double.toString(-0.0434251), next);
							       
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Double.toString(-0.146437), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Double.toString(-0.810138), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       
							     //*********************
							       String constraint1 = bro.readLine( ); 
							       //R1               1     3500        0        0        0        0        0 <=       90
							       
							       st = new StringTokenizer(constraint1);
							       st.nextToken();
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(3500), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       //*********************
							       
							       //*********************
							       String constraint2 = bro.readLine( );// R2               1     3500        0        0        0        0        0 >=       10
							       //*********************
							       String constraint3 = bro.readLine( );//R3               1     3500 -1.87863e+007        0        0        0        0 <=       90
							       //*********************
							       String constraint4 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
							       //*********************
							       String constraint5 = bro.readLine( ); //R5               1     3500 -1.87863e+007     3500        0        0        0 <=       90
							       //*********************				       
							       String constraint6 = bro.readLine( ); // R6               1     3500 -1.87863e+007     3500        0        0        0 >=       10
							       //*********************
							       String constraint7 = bro.readLine( );//R7               1     3500 -1.87863e+007     3500 -4.87947e+007        0        0 <=       90
							       //*********************
							       String constraint8 = bro.readLine( );//R8               1     3500 -1.87863e+007     3500 -4.87947e+007        0        0 >=       10
							       //*********************
							       String constraint9 = bro.readLine( );//R9               1     3500 -1.87863e+007     3500 -4.87947e+007     3500        0 <=       90
							       //*********************
							       String constraint10 = bro.readLine( );// R10              1     3500 -1.87863e+007     3500 -4.87947e+007     3500        0 >=       10
							       //*********************
							       String constraint11 = bro.readLine( );//R11              1     3500 -1.87863e+007     3500 -4.87947e+007     3500     3500 <=       90
							       //*********************
							       String constraint12 = bro.readLine( );//R12              1     3500 -1.87863e+007     3500 -4.87947e+007     3500     3500 >=       10
							       //*********************
							       String constraint13 = bro.readLine( );//R13              1     3500        0        0        0        0        0 >= 1.87863e+007
							       //*********************						       
							       String constraint14 = bro.readLine( );//R14              1     3500 -1.87863e+007     3500        0        0        0 >= 4.87947e+007
							       //*********************
							       stringRead = bro.readLine( ); // Type..
							       //*********************
							       
							       String upBo = bro.readLine();
							       //upbo            90    21600        1    13200        1    24450    23910
							       st = new StringTokenizer(upBo);
							       st.nextToken();
							       assertEquals(Integer.toString(90), st.nextToken());
							       assertEquals(Integer.toString(21600), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(13200), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(24450), st.nextToken());
							       assertEquals(Integer.toString(23910), st.nextToken());
							       //*********************	
							       
							       String lowBo = bro.readLine( );
							       //lowbo           10        0        1        0        1        0        0 
							       st = new StringTokenizer(lowBo);
							       st.nextToken();
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       
							       //*********************
							      
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
		
		
		//*****************************************
		//*****************************************
		
		
		
		
	}
	
	
	
		
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());
		return hubLoadDistribution1;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(2, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(3, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(4, makeBullshitPricingScheduleTest(optimal, suboptimal));
		return hubLoadDistribution1;
		
	}
	
	
	
	
	public static Schedule makeBullshitScheduleTest() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{100, 5789, 56};// 
		double[] bullshitCoeffs2 = new double[]{-22, 5.6, -2.5};
		
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
		bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}
	
	
	public void mapHubsTest(Controler controler, HubLinkMapping hubLinkMapping){
		
	
		double maxX=5000;
		double minX=-20000;
		double diff= maxX-minX;
		
		for (Link link:controler.getNetwork().getLinks().values()){
			// x values of equil from -20000 up to 5000
			if (link.getCoord().getX()<(minX+diff)/4){
				
				hubLinkMapping.addMapping(link.getId().toString(), 1);
			}else{
				if (link.getCoord().getX()<(minX+diff)*2/4){
					hubLinkMapping.addMapping(link.getId().toString(), 2);
				}else{
					if (link.getCoord().getX()<(minX+diff)*3/4){
						hubLinkMapping.addMapping(link.getId().toString(), 3);
					}else{
						hubLinkMapping.addMapping(link.getId().toString(), 4);
					}
				}
			}
			
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
	bullShitSchedule.printSchedule();
	
	return bullShitSchedule;
}

	
	
	

	
}


