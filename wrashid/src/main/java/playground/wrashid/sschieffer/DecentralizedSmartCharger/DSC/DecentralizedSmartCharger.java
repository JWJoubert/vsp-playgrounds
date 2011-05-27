package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import lpsolve.LpSolveException;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.geotools.referencing.factory.AllAuthoritiesFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;


import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G.V2G;


/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedV1G.java
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


/**
 * Controls the charging algorithm
	 * 1) determining and sorting agents schedules
	 * 2) LP
	 * 3) charging slot optimization
	 * 4) V2G 
	 * 
 * @author Stella
 *
 */
public class DecentralizedSmartCharger {
	
	public static boolean debug=false;	
	//***********************************************************************
	//CONSTANTS
	final public static double SECONDSPERMIN=60;
	final public static double SECONDSPER15MIN=15*60;
	final public static double SECONDSPERDAY=24*60*60;
	final public static int MINUTESPERDAY=24*60;
	
	final public static double KWHPERJOULE=1.0/(3600.0*1000.0);
	
	final public static double STANDARDCONNECTIONSWATT= 3500.0;
	//***********************************************************************	
	public static String outputPath;
	final Controler controler;
	public static HubLoadDistributionReader myHubLoadReader;
	public static ChargingSlotDistributor myChargingSlotDistributor;
	public static AgentTimeIntervalReader myAgentTimeReader;
	
	private LPEV lpev;
	private LPPHEV lpphev;
	
	private VehicleTypeCollector myVehicleTypes;
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	public ParkingTimesPlugin parkingTimesPlugin;
	public EnergyConsumptionPlugin energyConsumptionPlugin;

	private HashMap<Id, Schedule> agentParkingAndDrivingSchedules = new HashMap<Id, Schedule>(); 
	private HashMap<Id, Schedule> agentChargingSchedules = new HashMap<Id, Schedule>();
	private double averageChargingCostsAgent, averageChargingCostsAgentEV, averageChargingCostsAgentPHEV;	
	
	public double minChargingLength;	
	public double emissionCounter=0.0;	
	
	public LinkedList<Id> chargingFailureEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithPHEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithCombustion=new LinkedList<Id>();
	
	public LinkedList<Id> deletedAgents=new LinkedList<Id>();// agents where ParkingTimes were not found
	
	private HashMap<Id, Double> agentChargingCosts = new HashMap<Id,  Double>();
	
	//***********************************************************************
	public static V2G myV2G;
	private HashMap<Id, ContractTypeAgent> agentContracts;
	private double  xPercentDown, xPercentDownUp;
	//***********************************************************************
	private DifferentiableMultivariateVectorialOptimizer optimizer;
	private VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(10000,-10000);//
	//(double relativeThreshold, double absoluteThreshold)
	//In order to perform only relative checks, the absolute tolerance must be set to a negative value. 
	
	public static SimpsonIntegrator functionIntegrator= new SimpsonIntegrator();		
	public static PolynomialFitter polyFit;
	
	final public static DrawingSupplier supplier = new DefaultDrawingSupplier();
	//***********************************************************************	
	/**
	 * times to track time use of different calculations
	 */
	private double startTime, agentReadTime, LPTime, distributeTime, wrapUpTime;
	private double startV2G, timeCheckVehicles,timeCheckHubSources,timeCheckRemainingSources;
	
			
	//***********************************************************************
	
	/**
	 * initialization of object with basic parameters
	 */
	public DecentralizedSmartCharger(Controler controler, 
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin,
			String outputPath,
			VehicleTypeCollector myVehicleTypes) throws IOException, OptimizationException{
		
		this.controler=controler;						
		this.outputPath=outputPath;	
		
		optimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		optimizer.setMaxIterations(10000);		
		optimizer.setConvergenceChecker(checker);		
		polyFit= new PolynomialFitter(20, optimizer);
		
		myAgentTimeReader= new AgentTimeIntervalReader(
				parkingTimesPlugin, 
				energyConsumptionPlugin);
		
		this.myVehicleTypes=myVehicleTypes;
	}
	
	
	/**
	 * sets Agent contracts, these are relevant for the V2G procedure
	 * @param agentContracts
	 */
	public void setAgentContracts(HashMap<Id, ContractTypeAgent> agentContracts){
		this.agentContracts= agentContracts;
	}


	/**
	 * turns extra output on or off, that can be helpful for debugging
	 * i.e. understanding because of which agent the simulation was shut down, etc.
	 * @param onOff
	 */
	public void setDebug(boolean onOff){
		debug=onOff;
	}
	
	
	
	/**
	 * initialize LPs for EV, PHEV and combustion vehicle
	 * <li>the buffer is important for the EV calculation
	 * <li> the boolean regulates if SOC graphs for all agents over the day are printed after the LPs
	 * @param buffer
	 * @param output
	 */
	public void initializeLP(double buffer, boolean output){
		lpev=new LPEV(buffer, output);
		lpphev=new LPPHEV(output);
		
	}



	/**
	 * initialize ChargingSlotDistributor by setting the standard charging length
	 * <p> the standard charging length is the time interval in which charging slots are usually booked. 
	 * Only if the time is very constrained within in interval and if the allocation of a sufficient number of charging slots can not be achieved
	 * one long charging slot can be assigned. Smaller charging slots can also be assigned in order to book the required full charging time for an interval</p>
	 * @param minChargingLength
	 */
	public void initializeChargingSlotDistributor(double minChargingLength){
		this.minChargingLength=minChargingLength; 
		myChargingSlotDistributor=new ChargingSlotDistributor(minChargingLength);
	}



	public void setLinkedListValueHashMapVehicles(LinkedListValueHashMap<Id, Vehicle> vehicles){
		this.vehicles=vehicles;
		myV2G= new V2G(this);
	}

	public HashMap<Id, ContractTypeAgent> getAgentContracts(){
		return agentContracts;
	}

	
	
	public LPEV getLPEV(){
		return lpev;
	}
	
	public LPPHEV getLPPHEV(){
		return lpphev;
	}
	
	/**
	 * initializes HubLoadDistributionReader with its basic parameters
	 * @param hubLinkMapping
	 * @param deterministicHubLoadDistribution
	 * @param pricingHubDistribution
	 * @throws OptimizationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void initializeHubLoadDistributionReader(
			HubLinkMapping hubLinkMapping, 
			HashMap<Integer, Schedule> deterministicHubLoadDistribution,			
			HashMap<Integer, Schedule> pricingHubDistribution,
			double minPricePerkWhAllHubs,
			double maxPricePerkWhAllHubs
			) throws OptimizationException, IOException, InterruptedException{
		
		myHubLoadReader=new HubLoadDistributionReader(controler, 
				hubLinkMapping, 
				deterministicHubLoadDistribution,				
				pricingHubDistribution,
				myVehicleTypes,
				outputPath,
				minPricePerkWhAllHubs,
				maxPricePerkWhAllHubs
				);
	}

	
	/**
	 * sets the stochastic loads in the hubDistributionReader
	 * @param stochasticHubLoadDistribution
	 * @param locationSourceMapping
	 * @param agentVehicleSourceMapping
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public void setStochasticSources(
			HashMap<Integer, Schedule> stochasticHubLoadDistribution,
			HashMap<Id, GeneralSource> locationSourceMapping,
			HashMap<Id, Schedule> agentVehicleSourceMapping) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		myHubLoadReader.setStochasticSources(
				stochasticHubLoadDistribution,
				locationSourceMapping, 
				agentVehicleSourceMapping);
	}
	

	/**
	 * get agent schedules, find required charging times, assign charging times
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void run() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		startTime = System.currentTimeMillis();
		System.out.println("\n Reading agent Schedules");
		readAgentSchedules();
		
		agentReadTime = System.currentTimeMillis();
		System.out.println("\n Starting LP");
		findRequiredChargingTimes();
		
		LPTime = System.currentTimeMillis();
		System.out.println("\n Assigning charging times");
		assignChargingTimes();
		
		distributeTime = System.currentTimeMillis();
		
		System.out.println("\n Find Charging distribution");
		findChargingDistribution();
		
		System.out.println("\n Update loads ");
		updateDeterministicLoad(); // bottle neck of wrap up
		
		System.out.println("\n Update costs ");
		calculateChargingCostsAllAgents();
		wrapUpTime = System.currentTimeMillis();
		System.out.println("Decentralized Smart Charger DONE");
		
		writeSummaryDSCHTML("DSC"+vehicles.getKeySet().size()+"agents_"+minChargingLength+"chargingLength");
	}



	
	
	/**
	 * Loops over all agents
	 * Calls AgentChargingTimeReader to read in their schedule
	 * saves the schedule in agentParkingAndDrivingSchedules
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 * @throws IOException 
	 */
	public void readAgentSchedules() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		for (Id id : vehicles.getKeySet()){
			if(DecentralizedSmartCharger.debug){
				System.out.println("getAgentSchedule: "+ id.toString());
			}
			// in 10000 id 8797 had error with schedulesize=0
			agentParkingAndDrivingSchedules.put(id,myAgentTimeReader.readParkingAndDrivingTimes(id));
			
			if(agentParkingAndDrivingSchedules.get(id).getNumberOfEntries()==0){
				deletedAgents.add(id);
				
			}
		}
		deleteAgentEverywhere();
		
	}

	
	public void deleteAgentEverywhere(){
		for(int i=0; i< deletedAgents.size(); i++){
			Id id= deletedAgents.get(i);
			System.out.println("agent "+id.toString()+
			" had to be deleted - no parking times were found for him -- schedule == empty");
			agentParkingAndDrivingSchedules.remove(id);
			vehicles.getKeySet().remove(id);// referenced in agentContracts - so automatically deleted... later used for stochastic load		
			
		}
	}
	
	/**
	 * finds the required charging times for all parking intervals based on the daily plan of the agent
	 * the required charging times are then stored within the existing parkingAndDrivingSchedule of the agent
	 * @throws LpSolveException
	 * @throws IOException
	 */
	public void findRequiredChargingTimes() throws LpSolveException, IOException{
		
		
		for (Id id : vehicles.getKeySet()){			
			if (debug){
				System.out.println("Find required charging times - LP - agent"+id.toString() );
			}
			String type="";			
				/*
				 * EV OR PHEV
				 */
				double joulesFromEngine=0;
				
				double batterySize= getBatteryOfAgent(id).getBatterySize();
				double batteryMin=getBatteryOfAgent(id).getMinSOC();
				double batteryMax=getBatteryOfAgent(id).getMaxSOC(); 
				
				if(hasAgentPHEV(id)){	
					agentsWithPHEV.add(id);
					type="PHEVVehicle";
					
				}else{
					agentsWithEV.add(id);
					type="EVVehicle";
				}
				
				//try EV first				
				Schedule scheduleAfterLP= lpev.solveLP(agentParkingAndDrivingSchedules.get(id),
						id, 
						batterySize, batteryMin, batteryMax, 
						type);
				if (scheduleAfterLP !=null){
					// if successful --> save
					
					agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
					if(hasAgentPHEV(id)){
						// only if agent has PHEV change joules to emissions
						emissionCounter= joulesToEmissionInKg(id,joulesFromEngine); // still 0
												
					}
				}else{					
					//if fails, try PHEV
										
					scheduleAfterLP= lpphev.solveLP(agentParkingAndDrivingSchedules.get(id),id, batterySize, batteryMin, batteryMax, type);
					agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
					
					joulesFromEngine= lpphev.getEnergyFromCombustionEngine();
					if(hasAgentEV(id)){						
						chargingFailureEV.add(id);
						
					}else{						
						emissionCounter+= joulesToEmissionInKg(id, joulesFromEngine);
					}
				
			}
		}
	}



	/**
	 * passes schedule with required charging information to
	 * ChargingSlotDistributor to obtain exact charging Slots
	 * Saves charging slots in agentChargignSchedule
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 * @throws IOException 
	 * @throws OptimizationException 
	 */
	public void assignChargingTimes() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException, OptimizationException{
		
				
		for (Id id : vehicles.getKeySet()){
		
			System.out.println("Assign charging times agent "+ id.toString());
			
			Schedule chargingSchedule=myChargingSlotDistributor.distribute(id, agentParkingAndDrivingSchedules.get(id));
			
			agentChargingSchedules.put(id, chargingSchedule);
			
			if(id.toString().equals(Integer.toString(1))){
				visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), 
						agentChargingSchedules.get(id), 
						id);
			}
		}
		
		if(debug || vehicles.getKeySet().size()<1000){
			printGraphChargingTimesAllAgents();
		}
		
		
	}

	
	
	/**
	 * visualizes the daily plans (parking driving charging)of all agents and saves the files in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @throws IOException
	 */
	public void visualizeDailyPlanForAllAgents() throws IOException{
		for (Id id : vehicles.getKeySet()){
			
			visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), 
					agentChargingSchedules.get(id), 
					id);
		}
	}
	

	/**
	 *  visualizes the daily plan for agent with given id and saves the file in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @param id
	 * @throws IOException
	 */
	public void visualizeDailyPlanForAgent(Id id ) throws IOException{
			
			visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), 
					agentChargingSchedules.get(id), 
					id);
		
	}
	
	
	/**
	 * records the parking times of all agents at hubs and visualizes the distribition for each hub over the day
	 * 
	 * 
	 * @throws IOException
	 * @throws OptimizationException 
	 */
	private void findChargingDistribution() throws IOException, OptimizationException{
		
		for(int i=0; i<MINUTESPERDAY; i++){
			double thisSecond= i*SECONDSPERMIN;
			for(Id id : vehicles.getKeySet()){
				
				Schedule thisAgentParkAndDrive = agentParkingAndDrivingSchedules.get(id);
				if (debug){
					System.out.println("Finding Charging distribution Agent "+ id.toString());
					thisAgentParkAndDrive.printSchedule();
				}
				
				int interval= thisAgentParkAndDrive.timeIsInWhichInterval(thisSecond);
				
				//PARKING
				if (thisAgentParkAndDrive.timesInSchedule.get(interval).isParking()){
					
					int hub= myHubLoadReader.getHubForLinkId(
							((ParkingInterval)thisAgentParkAndDrive.timesInSchedule.get(interval)).getLocation() 
							);
						
					myHubLoadReader.recordParkingAgentAtHubInMinute(i, hub);
					
				}
				
			}
		}
		
		myHubLoadReader.calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader();		
		
	}


	/**
	 * for every agent's charging times the deterministic hub load is updated within the HubLoadreader,
	 * the new load distribution is then visualized: visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger();
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void updateDeterministicLoad() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
	
	for(Id id : vehicles.getKeySet()){
			System.out.println("update deterministic hub load with agent "+ id.toString());
			Schedule thisAgent= agentParkingAndDrivingSchedules.get(id);			
			for(int i=0; i< thisAgent.getNumberOfEntries(); i++){
				if (thisAgent.timesInSchedule.get(i).isParking()){
					ParkingInterval p= (ParkingInterval)thisAgent.timesInSchedule.get(i);
					if (p.getRequiredChargingDuration()!=0.0){
						Schedule charging= p.getChargingSchedule();
						for(int c=0; c< charging.getNumberOfEntries(); c++){
							// for every parking reduce deterministichubLoad
							
							myHubLoadReader.updateLoadAfterDeterministicChargingDecision(
									(charging.timesInSchedule.get(c)).getStartTime(), 
									(charging.timesInSchedule.get(c)).getEndTime(), 
									p.getLocation(), 
									p.getChargingSpeed(),false);
						}
					}
					
				}
			}
		}
		
		visualizeDeterministicLoadXYSeriesBeforeAfterDecentralizedSmartCharger();
		if(controler.getPopulation().getPersons().size()<=1000.0){
			visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger();
		}
		
	}

	
	
	
	/**
	 * calculates and stores the final charging costs for all agent schedules and their associated charging times
	 * </br> </br>
	 * the results are stored in agentChargingCosts
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void calculateChargingCostsAllAgents() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		for(Id id : vehicles.getKeySet()){			
			Schedule s= agentParkingAndDrivingSchedules.get(id);
			double thisChargingCost=calculateChargingCostForAgentSchedule(id, s) ;
			agentChargingCosts.put(id,thisChargingCost );
			
			averageChargingCostsAgent+=thisChargingCost;
			if(hasAgentEV(id)){
				averageChargingCostsAgentEV+=thisChargingCost;
			}else{
				averageChargingCostsAgentPHEV+=thisChargingCost;	
			}
		}
		averageChargingCostsAgent=averageChargingCostsAgent/(agentsWithEV.size()+agentsWithPHEV.size());
		averageChargingCostsAgentEV=averageChargingCostsAgentEV/(agentsWithEV.size());
		averageChargingCostsAgentPHEV=averageChargingCostsAgentPHEV/(agentsWithPHEV.size());
	}



	/**
	 * calculates the costs of charging for a schedule
	 * <li> integrates all charging tims with price functions
	 * <li> for PHEVS: adds gas costs for joules that had to be taken from engine
	 * <li> for EVs: adds Double.MAX_VALUE to the costs, in case the LP was not successful and a battery swap was necessary
	 * @param id
	 * @param s
	 * @return
	 */
	public double calculateChargingCostForAgentSchedule(Id id, Schedule s) {
		double totalCost=0;
		
		for(int i=0; i<s.getNumberOfEntries();i++){
			
			TimeInterval t = s.timesInSchedule.get(i); 
			
			// for every parking interval which has a charging schedule
			if(t.isParking() && ((ParkingInterval)t).getChargingSchedule()!=null){
				
				Id linkId = ((ParkingInterval)t).getLocation();
				ArrayList <LoadDistributionInterval> pricingLoadsDuringInterval= 
					myHubLoadReader.getPricingLoadDistributionIntervalsAtLinkAndTime(linkId, t);
				
				Schedule charging= ((ParkingInterval)t).getChargingSchedule();
				
				for(int priceIntervalX=0; priceIntervalX<pricingLoadsDuringInterval.size(); priceIntervalX++){
					// in case the parking interval goes over multiple pricing intervals
					// get and loop over all overlaps and calculate price
					LoadDistributionInterval currentpricingInterval= pricingLoadsDuringInterval.get(priceIntervalX);
					PolynomialFunction currentPriceFunc= currentpricingInterval.getPolynomialFunction();
					
					Schedule currentOverlapCharging=new Schedule();
					
					currentOverlapCharging= charging.getOverlapWithLoadDistributionInterval(currentpricingInterval);
					
					for(int c=0; c<currentOverlapCharging.getNumberOfEntries(); c++){
						try {
							totalCost+= functionIntegrator.integrate(currentPriceFunc, 
									currentOverlapCharging.timesInSchedule.get(c).getStartTime(),
									currentOverlapCharging.timesInSchedule.get(c).getEndTime()
									);
							
						} catch (Exception e) {
							System.out.println("ERROR - Method: calculateChargingCostForAgentSchedule");
							System.out.println("current charging Schedule");
							currentOverlapCharging.printSchedule();
							System.out.println("Agent Schedule");
							s.printSchedule();
							e.printStackTrace();
						} 
					}
				}
			}
			
			if(hasAgentPHEV(id)){
				if(t.isDriving() && ((DrivingInterval)t).getExtraConsumption()>0){
					totalCost +=  joulesExtraConsumptionToGasCosts(id,((DrivingInterval)t).getExtraConsumption());
				}
			}
			if(hasAgentEV(id)){
				if(t.isDriving() && ((DrivingInterval)t).getExtraConsumption()>0){
					if(DecentralizedSmartCharger.debug){
						System.out.println("extra consumption EV price calculation ");
						System.out.println("assigned "+Double.MAX_VALUE+" for agent "+ id.toString());
					}
				
					totalCost += Double.MAX_VALUE;
				}
			}
			
		}
		return totalCost;
	}


	
	/**
	 * get the percentage of agents with EV or PHEV who have the contract type: regulation up and down
	 * @return
	 */
	public double getPercentDownUp(){
		return xPercentDownUp;
		
	}
	 
	
	/**
	 * get the percentage of agents with EV or PHEV who have the contract type: regulation down
	 * @return
	 */
	public double getPercentDown(){
		return xPercentDown;
		
	}
	
	
	public void setV2GRegUpAndDownStats(
			double xPercentDown,
			double xPercentDownUp){
		
		this.xPercentDownUp=xPercentDownUp;
		this.xPercentDown=xPercentDown;
	}
	
	
	public void initializeAndRunV2G(
			double xPercentDown,
			double xPercentDownUp
			) throws FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, ConvergenceException{
		
		myV2G.initializeAgentStats();
		setV2GRegUpAndDownStats(xPercentDown,xPercentDownUp);
		
		startV2G=System.currentTimeMillis();
			
		System.out.println("START CHECKING VEHICLE SOURCES");
		
		checkVehicleSources();
		timeCheckVehicles	=System.currentTimeMillis();		
		
		System.out.println("START CHECKING HUB SOURCES");
		checkHubSources();
		timeCheckHubSources	=System.currentTimeMillis();
		
		System.out.println("START CHECKING STOCHASTIC HUB LOADS");
		//the remaining generalstochasticHubLoad needs to be recalculated 
		myHubLoadReader.recalculateStochasticHubLoadCurveAfterVehicleAndHubSources();
		checkHubStochasticLoads();
		
		timeCheckRemainingSources=System.currentTimeMillis();
		
		myV2G.calcV2GVehicleStats();
		System.out.println("DONE V2G");
		writeSummaryV2G("V2G"+vehicles.getKeySet().size()+"agents_"+minChargingLength+"chargingLength");
		
	}
	
	
	
	

	/**
	 * loops over all vehicle sources and tries to provide regulation up or down if this function is activated by the agent
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws IOException
	 * @throws OptimizationException
	 */
	public void checkVehicleSources() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		if(myHubLoadReader.agentVehicleSourceMapping!=null){
			for(Id id : myHubLoadReader.agentVehicleSourceMapping.keySet()){				
				
				if(DecentralizedSmartCharger.debug){
					System.out.println("check VehicleSource for"+ id.toString());
				}
					
					Schedule electricSource= myHubLoadReader.agentVehicleSourceMapping.get(id);
					
					for(int i=0; i<electricSource.getNumberOfEntries(); i++){
						
						LoadDistributionInterval electricSourceInterval= (LoadDistributionInterval)electricSource.timesInSchedule.get(i);
						// split up in small intervals of maximum length= mincharging length
						int intervals= (int) Math.ceil(electricSourceInterval.getIntervalLength()/minChargingLength);
						
						for(int intervalNum=0; intervalNum<intervals; intervalNum++){
							
							double bit=0;							
							if(intervalNum<intervals-1){
								bit=minChargingLength;								
							}else{// i=intervals-1
								bit=electricSourceInterval.getIntervalLength()- (intervals-1)*minChargingLength;								
							}
							if(bit>30.0){
								// sometimes numeric inaccuracies. e.g. if bit 10^-12, then start==end and integration fails
								double start=electricSourceInterval.getStartTime()+intervalNum*minChargingLength;
								double end= start+bit;
																
								PolynomialFunction func= new PolynomialFunction(
										electricSourceInterval.getPolynomialFunction().getCoefficients().clone()
										);
								
								LoadDistributionInterval currentStochasticLoadInterval= new LoadDistributionInterval(start, 
										end, 
										func, 
										electricSourceInterval.isOptimal());								
								
								double joulesFromSource= functionIntegrator.integrate(func, 
										currentStochasticLoadInterval.getStartTime(), 
										currentStochasticLoadInterval.getEndTime());
								
								String type;								
								if(hasAgentPHEV(id)){									
									type="PHEVRescheduleVehicleSourceAgent_"+id.toString();									
								}else{
									type="EVRescheduleVehicleSourceAgent_"+id.toString();
								}
																	
								/*
								 * NO CONTRACTS NECESSARY
								 * IN Any case it will be attempted to provide regulation up down
								 * if within correct battery state and if rescheduling is possible
								 */
								
								double compensation;
								if (Math.abs(joulesFromSource)>0.05*STANDARDCONNECTIONSWATT){
									if (joulesFromSource>0){//regulation down = local production
										/*
										 * compensation for providing your own energy
										 * self production cost is 0.0
										 * but saving costs of charging for the joules
										 * so has to reflect costs saved
										 * conservative estimate - worst case = compensation is cheapest possible cost/kWh
										 */
										compensation=myHubLoadReader.getMinPricePerKWHAllHubs()*KWHPERJOULE*joulesFromSource; 
										
										myV2G.regulationUpDownVehicleLoad(id,
												currentStochasticLoadInterval,											
												compensation,
												joulesFromSource,													
												type);
									}else{
										/*
										 * using up own battery for local extra load, e.g. music or air conditioning
										 * no compensation, has to charge all energy fully from electric grid later
										 * compensation =0.0
										 */
										// regulation up = local demand
											compensation=0.0;
											myV2G.regulationUpDownVehicleLoad(id,
													currentStochasticLoadInterval,											
													compensation,
													joulesFromSource,													
													type);
									}
								
								}
																						
							}
						}	
					}
					// check V2G effect
					if(debug|| id.toString().equals(Integer.toString(1))  ){
						visualizeStochasticLoadVehicleBeforeAfterV2G(id);												
					}	
			}
		}
		
	}


	
	
	
	public void checkHubSources() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		if(myHubLoadReader.locationSourceMapping!=null){
			for(Id linkId : myHubLoadReader.locationSourceMapping.keySet()){				
				
				if(DecentralizedSmartCharger.debug){
					System.out.println("check Hub Sources for"+ linkId.toString());
				}
					
					Schedule electricSource= myHubLoadReader.locationSourceMapping.get(linkId).getLoadSchedule();
					
					for(int i=0; i<electricSource.getNumberOfEntries(); i++){
						
						LoadDistributionInterval electricSourceInterval= (LoadDistributionInterval)electricSource.timesInSchedule.get(i);
						
						double joulesFromSource= functionIntegrator.integrate(
								electricSourceInterval.getPolynomialFunction(), 
								electricSourceInterval.getStartTime(), 
								electricSourceInterval.getEndTime());
						
						///////////////////////////
						double compensation;
						if (Math.abs(joulesFromSource)>0.05*STANDARDCONNECTIONSWATT){
							if (joulesFromSource>0){//regulation down = local production
								/*
								 * feed in 
								 */
								compensation=myHubLoadReader.locationSourceMapping.get(linkId).getFeedInCompensationPerKWH()
											*KWHPERJOULE*joulesFromSource; 
								
								myV2G.feedInHubSource(linkId,
										electricSourceInterval,											
										compensation,
										joulesFromSource													
										);
							}else{
								/*
								 * using up own battery for local extra load, e.g. music or air conditioning
								 * no compensation, has to charge all energy fully from electric grid later
								 * compensation =0.0
								 */
								if (joulesFromSource<0){// regulation up = local demand
									
									myV2G.hubSourceChargeExtra(linkId,
											electricSourceInterval,	
											joulesFromSource													
											);
								}
							}
						}
						
					}
					visualizeStochasticLoadHubSourceBeforeAfterV2G(linkId);
			}
		}
	}
	
	
	
	/**
	 * loops over all hubs and the stochastic loads in small time intervals of the same length as the standard charging slot length
	 * </br> 
	 * if agents are activated for V2G, it will be checked if the agent can perform V2G (i.e. is he at the right hub and parking, economic decision?)
	 *  </br>   </br> 
	 * the result is then visualized </br> 
	 * visualizeStochasticLoadBeforeAfterV2G();
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws OptimizationException
	 * @throws LpSolveException
	 * @throws IOException
	 */
	public void checkHubStochasticLoads() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException, LpSolveException, IOException{
		
		if(myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources !=null){
			for(Integer h : myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.keySet()){
				
				if(DecentralizedSmartCharger.debug){
					System.out.println("check hubSource for Hub "+ h.toString());
				}
				
				Schedule hubStochasticSchedule= myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.get(h);
				
				for(int j=0; j<hubStochasticSchedule.getNumberOfEntries(); j++){
					
					//each entry needs to be split down into sufficiently small time intervals					
					LoadDistributionInterval stochasticLoad= (LoadDistributionInterval)hubStochasticSchedule.timesInSchedule.get(j);
					PolynomialFunction func= stochasticLoad.getPolynomialFunction();
					
					int intervals =(int) Math.ceil(stochasticLoad.getIntervalLength()/minChargingLength);
					
					for(int i=0; i<intervals; i++){
						
						double bit=0;						
						if(i<intervals-1){
							bit=minChargingLength;
							
						}else{// i=intervals-1
							bit=stochasticLoad.getIntervalLength()- (intervals-1)*minChargingLength;							
						}
						//*********************************
						//*********************************
						if(bit>30.0){
							// sometimes numeric inaccuracies. e.g. if bit 10^-12, then start==end and integration fails
							//FINALLY HAVE INTERVAL TO LOOK AT IN THIS ITERATION
							double start=stochasticLoad.getStartTime()+i*minChargingLength;
							double end= start+bit;							
							//*********************************													
							double joulesFromSource= functionIntegrator.integrate(func, start, end);
							
							if (Math.abs(joulesFromSource)>0.05*STANDARDCONNECTIONSWATT){
								if(joulesFromSource<0 ){
									// regulation UP
									double expectedNumberOfParkingAgents=getPercentDownUp()*
										myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(
												h, 
												start);
									
									// SANITY CHECK
									if(expectedNumberOfParkingAgents>0.0){
										func=func.multiply(new PolynomialFunction(new double []{1/expectedNumberOfParkingAgents}));
										LoadDistributionInterval currentStochasticLoadInterval= new LoadDistributionInterval(start, 
												end, 
												func, 
												stochasticLoad.isOptimal());						
										
										// loop over all agents 
										// find who is in regulation up and do regulation up for him
										
										for(Id agentId :vehicles.getKeySet()){
											
											String type;						
											if(hasAgentPHEV(agentId)){											
												type="PHEVStochasticLoadRegulationUp";											
											}else{																			
												type="EVStochasticLoadRegulationUp";										}
											
											if(isAgentRegulationUp(agentId)){
												
												myV2G.regulationUpDownHubLoad(agentId, 
														currentStochasticLoadInterval, 
														agentParkingAndDrivingSchedules.get(agentId), 
														type,												
														h);
											}
										}
									
									}
								}
								
								if(joulesFromSource>0 ){
									double expectedNumberOfParkingAgents=(getPercentDown()+getPercentDownUp())*myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(
											h, 
											start);
									if (debug){
										System.out.println("expected number of agents: "+ myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(
												h, start));
										System.out.println("of which reg only down"+ getPercentDown()+" of which reg up and down"+getPercentDownUp());
										System.out.println("expected number of agents: "+ myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(
												h, start));
									}
									
									
									if(expectedNumberOfParkingAgents>0.0){
										func=func.multiply(new PolynomialFunction(new double []{1/expectedNumberOfParkingAgents}));
										LoadDistributionInterval currentStochasticLoadInterval= new LoadDistributionInterval(start, 
												end, 
												func, 
												stochasticLoad.isOptimal());
										
										
										for(Id agentId : vehicles.getKeySet()){
											
											String type;									
											
											if(hasAgentPHEV(agentId)){
												
												type="PHEVStochasticLoadRegulationDown";
												
											}else{
																					
												type="EVStochasticLoadRegulationDown";
											}
											
											if(isAgentRegulationDown(agentId)){
												
												myV2G.regulationUpDownHubLoad(agentId, 
														currentStochasticLoadInterval, 
														agentParkingAndDrivingSchedules.get(agentId), 
														type,												
														h);
												
											}
										}
									}
								}
							}// if absolute value target of 1 not hit
							
							
						}
						
					}//end for(int i=0; i<intervals; i++){
					
				}
				
			// VISUALIZE
				visualizeStochasticLoadBeforeAfterV2G();
				
			}
		}
		
	}



	/**
	 * checks the total revenue from V2G of the agent
	 * @param id
	 * @return
	 */
	public double getV2GRevenueForAgent(Id id){
		return myV2G.getAgentV2GRevenues(id);
	}

	/**
	 * checks if agents contract allows regulation up
	 * @param id
	 * @return
	 */
	public boolean isAgentRegulationUp(Id id){
		return agentContracts.get(id).isUp();
	}
	
	/**
	 * checks if agents contract allows regulation down
	 * @param id
	 * @return
	 */
	public boolean isAgentRegulationDown(Id id){
		return agentContracts.get(id).isDown();
	}
	
		
	/**
	 * checks if agent has a PHEV
	 * @param id
	 * @return
	 */
	public static boolean hasAgentPHEV(Id id){
		
		Vehicle v= vehicles.getValue(id);
		
		if(v.getClass().equals(PlugInHybridElectricVehicle.class)){
			return true;
		}else{return false;}
	}
	
	
	/**
	 * checks if agent has a EV
	 * @param id
	 * @return
	 */
	public static  boolean hasAgentEV(Id id){
		
		Vehicle v= vehicles.getValue(id);
		
		if(v.getClass().equals(ElectricVehicle.class)){
			return true;
		}else{return false;}
	}
	
	
	
	/**
	 * returns the HashMap<Id, Double> with all charging costs of all agents from charging and V2G
	 * @return
	 */
	public HashMap<Id, Double> getChargingCostsForAgents(){
		return agentChargingCosts;
	}
	
	
	/**
	 * returns the chronological sequences/schedules of parking and driving intervals for all agents
	 * @return
	 */
	public HashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules(){
		return agentParkingAndDrivingSchedules;
	}
	
	
	/**
	 * returns HashMap<Id, Schedule> agentChargingSchedules
	 */
	public HashMap<Id, Schedule> getAllAgentChargingSchedules(){
		return agentChargingSchedules;
	}
	
	/**
	 * returns a linkedList of all agents with an EV
	 * @return
	 */
	public LinkedList <Id> getAllAgentsWithEV(){
		return agentsWithEV;
	}
	
	/**
	 * returns a linkedList of all agents with a PHEV
	 * @return
	 */
	public LinkedList <Id> getAllAgentsWithPHEV(){
		return agentsWithPHEV;
	}
	
		
	/**
	 * get charging schedule of agent with Id id
	 * @param id
	 * @return
	 */
	public Schedule getAgentChargingSchedule(Id id){
		return agentChargingSchedules.get(id);
	}
	
	/**
	 * returns a linkedList of all agents with an EV, where the linear optimization was not successful, i.e. a battery swap would have been necessary
	 * @return
	 */
	public LinkedList<Id> getIdsOfEVAgentsWithFailedOptimization(){
		return chargingFailureEV;
	}
	
	
	/**
	 * returns the total Consumption in joules of the agent for normal electric driving and from the engine
	 * @param id
	 * @return
	 */
	public double getTotalDrivingConsumptionOfAgent(Id id){
		
		return agentParkingAndDrivingSchedules.get(id).getTotalBatteryConsumption()
		+agentParkingAndDrivingSchedules.get(id).getTotalConsumptionFromEngine();
	}
	
	
	/**
	 * returns the total consumption in joules from the battery
	 * @param id
	 * @return
	 */
	public double getTotalDrivingConsumptionOfAgentFromBattery(Id id){
		
		return agentParkingAndDrivingSchedules.get(id).getTotalBatteryConsumption();
	}
	
	
	/**
	 * returns the total consumption in joules from other sources than battery
	 * <li> for PHEVs engine
	 * <li> for EVs missing energy that would have needed a battery swap
	 * @param id
	 * @return
	 */
	public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id id){
		
		return agentParkingAndDrivingSchedules.get(id).getTotalConsumptionFromEngine();
	}
	
	/**
	 * returns total emissions caused from PHEVs
	 * @return
	 */
	public double getTotalEmissions(){
		return emissionCounter;
	}
	
	
	/**
	 * return average charging time of agents
	 * @return
	 */
	public double getAverageChargingCostAgents(){
		return averageChargingCostsAgent;
	}
	
	
	/**
	 * return average charging time of EV agents
	 * @return
	 */
	public double getAverageChargingCostEV(){
		return averageChargingCostsAgentEV;
	}
	
	/**
	 * return average charging time of PHEV agents
	 * @return
	 */
	public double getAverageChargingCostPHEV(){
		return averageChargingCostsAgentPHEV;
	}
	
	
	/**
	 * clears agentParkingAndDrivingSchedules, agentChargingSchedules, emissions and lists of EV/PHEV/conventional car owners
	 */
	public void clearResults(){
		agentParkingAndDrivingSchedules = new HashMap<Id, Schedule>(); 
		agentChargingSchedules = new HashMap<Id, Schedule>();
		
		emissionCounter=0.0;
		
		chargingFailureEV=new LinkedList<Id>();
		agentsWithEV=new LinkedList<Id>();
		agentsWithPHEV=new LinkedList<Id>();
		agentsWithCombustion=new LinkedList<Id>();
		
	}



	/**
	 * plots daily schedule and charging times of agent 
	 * and save it in: 
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
		  
	 * @param dailySchedule
	 * @param chargingSchedule
	 * @param id
	 * @throws IOException
	 */
	private void visualizeAgentChargingProfile(Schedule dailySchedule, Schedule chargingSchedule, Id id) throws IOException{
		
		// 1 charging, 2 suboptimal, 3 optimal, 4 driving
		
		XYSeriesCollection agentOverview= new XYSeriesCollection();
		
		//************************************
		//GET EXTRA CONSUMPTION TIMES - MAKE RED LATER
		
		int extraConsumptionCount=0;
		
		
		if(vehicles.getValue(id).getClass().equals(PlugInHybridElectricVehicle.class)){
			
			
			for(int i=0; i<dailySchedule.getNumberOfEntries();i++){
				
				if(dailySchedule.timesInSchedule.get(i).isDriving()){
					
					DrivingInterval thisD= (DrivingInterval) dailySchedule.timesInSchedule.get(i);
					
					if(thisD.hasExtraConsumption()){
						
						
						XYSeries drivingTimesSet= new XYSeries("extra consumption");
						drivingTimesSet.add(thisD.getStartTime(), 3.75);
						drivingTimesSet.add(thisD.getStartTime()+thisD.getEngineTime(), 3.75);
						agentOverview.addSeries(drivingTimesSet);
						extraConsumptionCount++;
					}
					
				}
			}
		}
	
		
		//************************************
		// ADD ALL OTHER TIMES DRIVING; PARKING;..
		//************************************
		for(int i=0; i<dailySchedule.getNumberOfEntries();i++){
			if(dailySchedule.timesInSchedule.get(i).isDriving()){
				
				XYSeries drivingTimesSet= new XYSeries("driving times");
				drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getStartTime(), 4);
				drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getEndTime(), 4);
				agentOverview.addSeries(drivingTimesSet);
				
			}else{
				
				ParkingInterval p= (ParkingInterval) dailySchedule.timesInSchedule.get(i);
				
				if(p.isInSystemOptimalChargingTime()){
					XYSeries parkingOptimalTimesSet= new XYSeries("parking times during optimal charging time");
					parkingOptimalTimesSet.add(p.getStartTime(), 3);
					parkingOptimalTimesSet.add(p.getEndTime(), 3);
					agentOverview.addSeries(parkingOptimalTimesSet);
					
				}else{
					
					XYSeries parkingSuboptimalTimesSet= new XYSeries("parking times during suboptimal charging time");
					parkingSuboptimalTimesSet.add(p.getStartTime(), 2);
					parkingSuboptimalTimesSet.add(p.getEndTime(), 2);
					agentOverview.addSeries(parkingSuboptimalTimesSet);
					
				}
				
			}
		}
		
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			
			XYSeries chargingTimesSet= new XYSeries("charging times");
			chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getStartTime(), 1);
			chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getEndTime(), 1);
			agentOverview.addSeries(chargingTimesSet);
			
		}
		
		//************************************
		// MAKE CHART
		//************************************
		JFreeChart chart = ChartFactory.createXYLineChart("Travel pattern agent : "+ id.toString(), 
				"time [s]", 
				"charging, off-peak parking, peak-parking, driving times", 
				agentOverview, 
				PlotOrientation.VERTICAL, 
				true, 
				true, 
				false);
		
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
        
        
        //TextAnnotation offPeak= new TextAnnotation("Off Peak parking time");
        XYTextAnnotation txt1= new XYTextAnnotation("Charging time", 20000,1.1);
        XYTextAnnotation txt2= new XYTextAnnotation("Driving time", 20000,4.1);
        XYTextAnnotation txt3= new XYTextAnnotation("Optimal parking time", 20000,3.1);
        XYTextAnnotation txt4= new XYTextAnnotation("Suboptimal parking time", 20000,2.1);
        XYTextAnnotation txt5= new XYTextAnnotation("Driving with engine power", 20000,3.85);
        
        txt1.setFont(new Font("Arial", Font.PLAIN, 14));
        txt2.setFont(new Font("Arial", Font.PLAIN, 14));
        txt3.setFont(new Font("Arial", Font.PLAIN, 14));
        txt4.setFont(new Font("Arial", Font.PLAIN, 14));
        txt5.setFont(new Font("Arial", Font.PLAIN, 14));
        //public Font(String name,int style,int size)
        
        txt5.setPaint(Color.red);
        
        plot.addAnnotation(txt1);
        plot.addAnnotation(txt2);
        plot.addAnnotation(txt3);
        plot.addAnnotation(txt4);
        plot.addAnnotation(txt5);
        
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, SECONDSPERDAY);
        
        
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 5);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setVisible(false);
        
        int numSeries=dailySchedule.getNumberOfEntries()+chargingSchedule.getNumberOfEntries()+extraConsumptionCount;
        
        for(int j=0; j<numSeries; j++){
        	
        	// IF FROM ENGINE MAKE RED
        	if (j<extraConsumptionCount){
        		plot.getRenderer().setSeriesPaint(j, Color.red);
        		
        	}else{
        		// ALL OTHERS MAKE BLACK
        		plot.getRenderer().setSeriesPaint(j, Color.black);
        	}        	
        	
        	plot.getRenderer().setSeriesStroke(
    	            j, 
    	          
    	            new BasicStroke(
    	                1.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {1.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
            
        }
        ChartUtilities.saveChartAsPNG(new File(outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png") , chart, 1000, 1000);
		  
	}
	
	
	
	private void visualizeDeterministicLoadXYSeriesBeforeAfterDecentralizedSmartCharger()throws IOException{
		
		  //************************************
			//READ IN VALUES FOR EVERY HUB
			// and make chart before-after
			//************************************
			for( Integer i : myHubLoadReader.deterministicHubLoadAfter15MinBins.keySet()){
				visualizeTwoXYLoadSeriesBeforeAfter(
						myHubLoadReader.deterministicHubLoadDistribution.get(i),//before 
						myHubLoadReader.deterministicHubLoadAfter15MinBins.get(i).getXYSeries("Free Load at Hub "+ i.toString()+ " after"),//after
						"Free Load at Hub "+ i.toString()+ " before",
						outputPath+"Hub/deterministicLoadBeforeAfter_hub"+i.toString()+".png", 
						"DeterministicLoadBeforeAfter_hub"+i.toString());
				
			}   		    		
     
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	private void visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger() throws IOException{
		
		  //************************************
			//READ IN VALUES FOR EVERY HUB
			// and make chart before-after
			//************************************
			for( Integer i : myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.keySet()){
				visualizeTwoLoadSchedulesBeforeAfter(
						myHubLoadReader.deterministicHubLoadDistribution.get(i),//before 
						myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.get(i),//after
						"Free Load at Hub "+ i.toString()+ " before",
						"Free Load at Hub "+ i.toString()+ " after",
						outputPath+"Hub/deterministicLoadBeforeAfterContinuous_hub"+i.toString()+".png", 
						"DeterministicLoadBeforeAfter_hub"+i.toString());
				
			}   		    		
       
	}
	
	
	
	private void visualizeStochasticLoadBeforeAfterV2G() throws IOException{
		
		  //************************************
			//READ IN VALUES FOR EVERY HUB
			// and make chart before-after
			//************************************
			for( Integer i : myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.keySet()){
				Schedule sBefore= myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.get(i);
				//Schedule sAfter=myHubLoadReader.stochasticHubLoadDistributionAfterContinuous.get(i);
				XYSeries sAfter = myHubLoadReader.stochasticHubLoadAfter15MinBins.get(i).getXYSeries("Stochastic free load at hub "+ i.toString()+ " after V2G");
				
				visualizeTwoXYLoadSeriesBeforeAfter(sBefore, sAfter, 
						"Stochastic free load at hub "+ i.toString()+ " before V2G",
						outputPath+"V2G/stochasticLoadBeforeAfter_hub"+i.toString()+".png", 
						"StochasticLoadBeforeAfter_hub"+i.toString());
			}   		    		
     
	}
	
	
	
	private void visualizeStochasticLoadVehicleBeforeAfterV2G(Id id) throws IOException{
		
		Schedule sBefore= myHubLoadReader.agentVehicleSourceMapping.get(id);
		
		XYSeries sAfter=myHubLoadReader.agentVehicleSourceAfter15MinBins.get(id).getXYSeries("Stochastic free load Agent "+ id.toString()+ " after V2G");
		visualizeTwoXYLoadSeriesBeforeAfter(sBefore,
				sAfter,
				"Stochastic free load Agent "+ id.toString()+ " before V2G",						
				outputPath+"V2G/stochasticLoadBeforeAfter_agentVehicle"+id.toString()+".png", 
				"StochasticLoadBeforeAfter_agentVehicle"+id.toString());     
	}
	
	
	
	
	private void  visualizeStochasticLoadHubSourceBeforeAfterV2G(Id linkId){
		Schedule sBefore= myHubLoadReader.locationSourceMapping.get(linkId).getLoadSchedule();
		
		String name=myHubLoadReader.locationSourceMapping.get(linkId).getName();
		XYSeries sAfter=myHubLoadReader.locationSourceMappingAfter15MinBins.get(linkId).getXYSeries(
				"Hub Source '"+name +"' at Link "+ linkId.toString()+ " after V2G");
		visualizeTwoXYLoadSeriesBeforeAfter(sBefore,
				sAfter,
				"Hub Source '"+name +"' at Link "+ linkId.toString()+ "before V2G",						
				outputPath+"V2G/stochasticHubSourceBeforeAfter_"+name+".png", 
				"StochasticHubSourceBeforeAfter_"+name); 
	}
	
	/**
	 * 
	 * @param before
	 * @param after
	 * @param XYBefore
	 * @param XYafter
	 * @param outputFile
	 * @param graphTitle
	 */
	public void visualizeTwoLoadSchedulesBeforeAfter(
			Schedule before, Schedule after, 
			String XYBefore, String XYAfter,
			String outputFile, String graphTitle){
		
		try{
		    // Create file 
		    FileWriter fstream = new FileWriter(outputPath+graphTitle+".txt");
		    BufferedWriter out = new BufferedWriter(fstream);
		   
		    out.write("time \t after \n");
		    
			XYSeriesCollection load= new XYSeriesCollection();
			XYSeries beforeXY= new XYSeries(XYBefore);
			XYSeries afterXY= new XYSeries(XYAfter);
			//************************************
			//AFTER//BEFORE
			
			for(int entry=0; entry<before.getNumberOfEntries();entry++){
				TimeInterval t= before.timesInSchedule.get(entry);
				
				if(t.isLoadDistributionInterval()){
					LoadDistributionInterval t2=(LoadDistributionInterval) t;
					for(double a=t2.getStartTime(); a<=t2.getEndTime();){
						beforeXY.add(a, t2.getPolynomialFunction().value(a));
						a+=60;//in one minute bins
					}
				}
			}
			
			//************************************
			for(int entry=0; entry<after.getNumberOfEntries();entry++){
				TimeInterval t= after.timesInSchedule.get(entry);
				if(t.isLoadDistributionInterval()){
					LoadDistributionInterval t2=(LoadDistributionInterval) t;
					
					for(double a=t2.getStartTime(); a<=t2.getEndTime();){
						double loadT=t2.getPolynomialFunction().value(a);
						afterXY.add(a, loadT);
						a+=60;//in one minute bins								
						out.write(Double.toString(a)+" \t "+ loadT +" \n");
					}
				}
			}
			
			//************************************
			load.addSeries(afterXY);
			load.addSeries(beforeXY);
					
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart(graphTitle, 
					"time [s]", 
					"available load [W]", 
					load, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.red);//after
	        plot.getRenderer().setSeriesPaint(1, Color.black);//before
	        
        	plot.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                1.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {1.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        	
        	plot.getRenderer().setSeriesStroke(
    	            1, 
    	          
    	            new BasicStroke(
    	                5.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {1.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
        	ChartUtilities.saveChartAsPNG(new File(outputFile) , chart, 1000, 1000);
           
        	
        	//Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		}
		
		
		
	}
	
	
	public void visualizeTwoXYLoadSeriesBeforeAfter(
			Schedule beforeSchedule, XYSeries after, 
			String XYBefore,
			String outputFile, String graphTitle){
		
		try{
		    // Create file 
		    FileWriter fstream = new FileWriter(outputPath+graphTitle+".txt");
		    BufferedWriter out = new BufferedWriter(fstream);
		   
		    out.write("time \t after \n");
		    
			XYSeriesCollection load= new XYSeriesCollection();
		
			//************************************
			//AFTER//BEFORE
			//************************************
			XYSeries before= new XYSeries(XYBefore);
			
			//************************************
			//AFTER//BEFORE
			
			for(int entry=0; entry<beforeSchedule.getNumberOfEntries();entry++){
				TimeInterval t= beforeSchedule.timesInSchedule.get(entry);
				
				if(t.isLoadDistributionInterval()){
					LoadDistributionInterval t2=(LoadDistributionInterval) t;
					for(double a=t2.getStartTime(); a<=t2.getEndTime();){
						before.add(a, t2.getPolynomialFunction().value(a));
						a+=60.0*15.0;//in one minute bins
					}
				}
			}
			
			load.addSeries(after);
			load.addSeries(before);
					
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart(graphTitle, 
					"time [s]", 
					"available load [W]", 
					load, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.red);//after
	        plot.getRenderer().setSeriesPaint(1, Color.black);//before
	        
        	plot.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                1.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {1.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        	
        	plot.getRenderer().setSeriesStroke(
    	            1, 
    	          
    	            new BasicStroke(
    	                5.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {1.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
        	ChartUtilities.saveChartAsPNG(new File(outputFile) , chart, 1000, 1000);
           
        	
        	//Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		}
		
		
		
	}
	
	private void printGraphChargingTimesAllAgents() throws IOException{
		
		XYSeriesCollection allAgentsOverview= new XYSeriesCollection();
		
		int seriesCount=0;
		
		for(Id id : vehicles.getKeySet()){
			
			Schedule s1= agentChargingSchedules.get(id);
			
			for(int i=0; i<s1.getNumberOfEntries(); i++){
				
				String strId= id.toString();
				int intId= Integer.parseInt(strId);
			    
				XYSeries chargingTimesSet= new XYSeries("charging time");
								
				chargingTimesSet.add(s1.timesInSchedule.get(i).getStartTime(),intId); 
				chargingTimesSet.add(s1.timesInSchedule.get(i).getEndTime(), intId);
				
				allAgentsOverview.addSeries(chargingTimesSet);
				seriesCount++;
			}
		
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart("Distribution of charging times for all agents by agent Id number", 
				"time [s]", 
				"charging times", 
				allAgentsOverview, 
				PlotOrientation.VERTICAL, 
				false, true, false);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, SECONDSPERDAY);
       
        NumberAxis yaxis = (NumberAxis) plot.getRangeAxis();
        yaxis.setRange(0, vehicles.getKeySet().size()); // y axis size dependent on number of agents
        
        for(int j=0; j<seriesCount; j++){
        	plot.getRenderer().setSeriesPaint(j, Color.black);
        	
        	plot.getRenderer().setSeriesStroke(
    	            j, 
    	            new BasicStroke(
    	                1.0f, 
    	                BasicStroke.CAP_ROUND, 
    	                BasicStroke.JOIN_ROUND, 
    	                1.0f,
    	                new float[] {1.0f, 0.0f}, 
    	                1.0f
    	            )
    	        );
        }
        
        chart.setTitle(new TextTitle("Distribution of charging times for all agents by agent Id number", 
    		   new Font("Arial", Font.BOLD, 20)));
        
        ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger\\allAgentsChargingTimes.png"), chart, 2000, (int)(20.0*(vehicles.getKeySet().size())));//width, height	
	
	}
	
	
	
	
	/**
	 * transforms joules to emissions according to the vehicle and gas type data stored in the vehicleCollector
	 * <p>When converting the joules needed for driving into liters of gas, the engine efficiency stored in the vehicle type
	 * is considered, i.e.
	 * </br>
	 * joulesUsed = numLiter * joulesPerLiter/efficiency
	 * </p>
	 * 
	 * @param agentId
	 * @param joules
	 * @return
	 */
	public double joulesToEmissionInKg(Id agentId, double joules){
		Vehicle v= vehicles.getValue(agentId);
		GasType vGT= myVehicleTypes.getGasType(v);
		
		// joules used = numLiter * possiblejoulesPer liter/efficiecy
		// numLiter= joulesUsed/(possiblejoulesPer liter/efficiecy)
		double liter=1/(vGT.getJoulesPerLiter())*1/myVehicleTypes.getEfficiencyOfEngine(v)*joules; 		
		double emission= vGT.getEmissionsPerLiter()*liter; 
				
		return emission;
	}
	
	
	/**
	 * converts the extra joules into costs
	 * <p>When converting the joules needed for driving into liters of gas, the engine efficiency stored in the vehicle type
	 * is considered, i.e.
	 * </br>
	 * joulesUsed = numLiter * joulesPerLiter/efficiency
	 * </p>
	 * @param agentId
	 * @param joules
	 * @return
	 */
	public double joulesExtraConsumptionToGasCosts(Id agentId, double joules){
		
		Vehicle v= vehicles.getValue(agentId);
		GasType vGT= myVehicleTypes.getGasType(v);
		double liter=1/(vGT.getJoulesPerLiter())*1/myVehicleTypes.getEfficiencyOfEngine(v)*joules; 
		
		double cost= vGT.getPricePerLiter()*liter; // xx CHF/liter 
		
		return cost;
	}
	
	
	/**
	 * gets the battery object of the vehicle of the agent
	 * @param agentId
	 * @return
	 */
	public Battery getBatteryOfAgent(Id agentId){
		return myVehicleTypes.getBattery(vehicles.getValue(agentId));
		
	}
	
	/**
	 * gets the gasType object of the vehicle of the agent
	 * @param agentId
	 * @return
	 */
	public GasType getGasTypeOfAgent(Id agentId){
		return myVehicleTypes.getGasType(vehicles.getValue(agentId));
		
	}
	
	
	public void writeOutLoadDistribution(String configName){
		try{
		    // Create file 
			String title=(outputPath + configName+ "_summary.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    out.write("Schedule loadSchedule= new Schedule()");
		    for(int i=0; i<myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.get(1).getNumberOfEntries(); i++){
		    	
		    	LoadDistributionInterval l= (LoadDistributionInterval )
		    	myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.get(1).timesInSchedule.get(i);
		    	 out.write("LoadDistributionInterval l"+i+" = new LoadDistributionInterval("+
		    			 l.getStartTime()+", "+ 
		    			 l.getEndTime()+", "+
		    			 "new PolynomialFunction (new double[]{"+
		    			 turnDoubleInString(l.getPolynomialFunction().getCoefficients())//doubles
		    			 +"} " +
		    			 ")"//Polynomial
		    			 +", "+
		    			 l.isOptimal() +"); \n ");
		    	 
		    	 out.write("loadSchedule.addTimeInterval()l"+i+"); \n");
		    }
		    
		   		    
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}


	public String turnDoubleInString(double[] d){
		String s="";
		for(int i=0; i<d.length; i++){
			s=s.concat(Double.toString(d[i]));
			if(i<d.length-1){
				s=s.concat(", ");
			}
		}
		return s;
	}


	/**
	 * writes a summary after the decentralized charging process is complete with data on:
	 * <li>  number of vehicles
	 * <li> standard charging slot length
	 * <li> time of calculations:  for reading agents, for LP, for slot distribution
	 * <li> total emissions
	 * <li> summary of vehicle types used
	 * <li> graph of price distribution over the day for every hub
	 * <li> graph of deterministic hub load before and after for every hub
	 * @param configName
	 */
	public void writeSummaryDSCHTML(String configName){
		try{
		    // Create file 
			String title=(outputPath + configName+ "_summary.html");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    out.write("<html>");
		    out.write("<body>");
		    
		    //*************************************
		    out.write("<h1>Summary of run:  </h1>");
		  //*************************************
		  //*************************************
		    out.write("<p>"); //add where read in ferom.. what file..?
		  //*************************************
		    out.write("Decentralized Smart Charger </br> </br>");
		    
		    out.write("Number of PHEVs: "+ getAllAgentsWithPHEV().size()
		    		+"</br>");
		    out.write("Number of EVs: "+ getAllAgentsWithEV().size()
		    		+" of which "+ chargingFailureEV.size()+" could not complete their trip"+"</br>");
		   
		    out.write("</br>");
			   
		    out.write("Time </br> </br>");
		    out.write("Standard charging sloth length [s]:"+ minChargingLength
		    		+"</br>");
		    
		    out.write("Time [ms] reading agent schedules:"+ (agentReadTime-startTime)
		    		+"</br>");
		    out.write("Time [ms] LP:"+ (LPTime-agentReadTime)
		    		+"</br>");
		    out.write("Time [ms] slot distribution:"+ (distributeTime-LPTime)
		    		+"</br>");
		    out.write("Time [ms] wrapping up:"+ (wrapUpTime-distributeTime)
		    		+"</br>");
		    
		    //deletedAgents
		    for(int i=0; i<deletedAgents.size(); i++){
		    	out.write("</br>");
			    out.write("DELETED AGENT: ");
			    out.write("id: "+deletedAgents.get(i).toString());
			    out.write("</br>");
		    }
		    
		    out.write("</br>");
		    out.write("CHARGING TIMES </br>");
		    out.write("Average charging time of agents: "+getAverageChargingCostAgents()+"</br>");
		    out.write("Average charging time of EV agents: "+getAverageChargingCostEV()+"</br>");			   
		    out.write("Average charging time of PHEV agents: "+getAverageChargingCostPHEV()+"</br>");
		    out.write("</br>");
		    
		    out.write("TOTAL EMISSIONS: "+ getTotalEmissions() +"</br>");
		    
		    //FIX SOMETHING NOT GOOD WITH IT: out.write(myVehicleTypes.printHTMLSummary());
		    
		    out.write("</br>");
		    
		    for(Integer hub: myHubLoadReader.deterministicHubLoadDistribution.keySet()){
		    	out.write("HUB"+hub.toString()+"</br> </br>");
		    	
		    	out.write("Prices </br>");
		    	String picPrices=  outputPath+ "Hub\\pricesHub_"+ hub.toString()+".png";
		    	out.write("<img src='"+picPrices+"' alt='' width='80%'");
		    	out.write("</br> </br>");
		    	
		    	out.write("Load Before and after </br>");
		    	String picBeforeAfter= outputPath+"Hub/deterministicLoadBeforeAfter_hub1.png";
		    	out.write("<img src='"+picBeforeAfter+"' alt='' width='80%'");
		    	out.write("</br> </br>");
		    }
		 
		   
		    out.write("</p>");
		  //*************************************
		    out.write("</body>");
		    out.write("</html>");   
		    
		    
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
	
	
	
	
	
	
	public void writeSummaryV2G(String configName){
		try{
		    // Create file 
			String title=(outputPath + configName+ "_summary.html");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    out.write("<html>");
		    out.write("<body>");
		    
		    //*************************************
		    out.write("<h1>Summary of V2G:  </h1>");
		  //*************************************
		  //*************************************
		    out.write("<p>"); //add where read in ferom.. what file..?
				    
		  //*************************************
		    out.write("TIME </br>");
		    out.write("Time [ms] checking vehicle sources:"+ 
		    		(timeCheckVehicles-startV2G)+"</br>");
		    
		    out.write("Time [ms] checking hub sources:"+ 
		    		(timeCheckHubSources-timeCheckVehicles)+"</br>");
		    
		    out.write("Time [ms] checking remaining sources:"+ 
		    		(timeCheckRemainingSources-timeCheckHubSources)+"</br>");
		    out.write(" </br> </br>");
		    
		    out.write("V2G INPUT </br>");
		    out.write("% of agents with contract 'regulation down': "+getPercentDown() +" </br>");
		    out.write("% of agents with contract 'regulation up and down': "+getPercentDownUp() +" </br>");
		    
		    out.write(" </br> </br>");
		    
		    out.write("V2G REVENUE </br>");
		    out.write("Average revenue per agent: "+myV2G.getAverageV2GRevenueAgent() +" </br>");
		    out.write("Average revenue per EV agent: "+myV2G.getAverageV2GRevenueEV() +" </br>");
		    out.write("Average revenue per PHEV agent: "+myV2G.getAverageV2GRevenuePHEV() +" </br>");
		    out.write(" </br> </br>");
		    
		    out.write("V2G CHARGING </br>");
		    out.write("Total V2G Up provided by all Agents: "+myV2G.getTotalRegulationUp() +" </br>");
		    out.write("      of which EV: "+myV2G.getTotalRegulationUpEV() +" </br>");
		    out.write("      of which PHEV: "+myV2G.getTotalRegulationUpPHEV() +" </br> </br>");
		    
		    out.write("Total V2G Down provided by all Agents: "+myV2G.getTotalRegulationDown() +" </br>");
		    out.write("      of which EV: "+myV2G.getTotalRegulationDownEV() +" </br>");
		    out.write("      of which PHEV: "+myV2G.getTotalRegulationDownPHEV() +" </br>");
		   
		    
		    //LOAD CURVES BEFORE AND AFTER
		    if(myHubLoadReader.agentVehicleSourceMapping!=null){
		    	 //Vehicle 1
			    out.write("Vehicle Load of agent: 1 </br> </br>");
		    	out.write("Stochastic load before and after </br>");
		    	String picBeforeAfter= outputPath+"V2G/stochasticLoadBeforeAfter_agentVehicle1.png";
		    	out.write("<img src='"+picBeforeAfter+"' alt='' width='80%'");
		    	out.write("</br> </br>");
		    }else{
		    	out.write("No Vehicle Loads </br> </br>");
		    }
		   
	    	
		    //HUB
		    for(Integer hub: myHubLoadReader.stochasticHubLoadDistribution.keySet()){
		    	out.write("HUB"+hub.toString()+"</br> </br>");
		    			    	
		    	out.write("Stochastic load before and after </br>");
		    	String picBeforeAfter= outputPath+"V2G/stochasticLoadBeforeAfter_hub"+hub.toString()+".png";
		    	out.write("<img src='"+picBeforeAfter+"' alt='' width='80%'");
		    	out.write("</br> </br>");
		    }
		   
		    out.write("</p>");
		 
		    out.write("</body>");
		    out.write("</html>");  
		    
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}


	
	

	public void setV2G(V2G setV2G){
		myV2G=setV2G;
	}
	
	public static PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		DecentralizedSmartCharger.polyFit.clearObservations();
		
		for (int i=0;i<data.length;i++){
			DecentralizedSmartCharger.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		
		PolynomialFunction poly = DecentralizedSmartCharger.polyFit.fit();
		 
		DecentralizedSmartCharger.polyFit.clearObservations();
		return poly;
	}

	public static void linkedListIdPrinter(HashMap<Id, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Id id: list.keySet()){
			list.get(id).printSchedule();
		}
		
	}
	
	public  static void linkedListIntegerPrinter(HashMap<Integer, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Integer id: list.keySet()){
			list.get(id).printSchedule();
		}
	
}
	
}
