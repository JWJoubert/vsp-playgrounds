/* *********************************************************************** *
 * project: org.matsim.*
 * HubLoadDistributionReader.java
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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;



/**
 * keeps track of all deterministic and stochastic loads and prices for EVs and PHEVs
 *  over the day.
 * 
 * @author Stella
 *
 */
public class HubLoadDistributionReader {
	
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverBisection = factory.newBisectionSolver();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
	
		
	private HubLinkMapping hubLinkMapping;
	
	LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution;
	LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistributionPHEVAdjusted;
	LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution;
	LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
	LinkedListValueHashMap<Integer, TimeDataCollector> connectivityHubDistribution;
	
	LinkedListValueHashMap<Integer, Schedule> locationSourceMapping;
	LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping;
	
	LinkedListValueHashMap<Integer, double [][]> originalDeterministicChargingDistribution;
	LinkedListValueHashMap<Integer, double [][]> loadAfterDeterministicChargingDecision;
	
	//double [time - seconds in day] = available free load in W on grid in Hub
	
	Controler controler;
	VehicleTypeCollector myVehicleTypeCollector;
	
	/**
	 * Reads in load data for all hubs and stores PolynomialFunctions 
	 * of load valleys and peak load times
	 * @throws IOException 
	 * @throws OptimizationException 
	 * @throws InterruptedException 
	 */
	public HubLoadDistributionReader(Controler controler, 
			HubLinkMapping hubLinkMapping,
			LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution,			
			LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution,		
			VehicleTypeCollector myVehicleTypeCollector) throws IOException, OptimizationException, InterruptedException{
		
		this.controler=controler;
		
		this.hubLinkMapping=hubLinkMapping;
		
		this.deterministicHubLoadDistribution=deterministicHubLoadDistribution; // continuous functions
		
		
		this.pricingHubDistribution=pricingHubDistribution; // continuous functions with same intervals as deterministic HubLoadDistribution!!!
		
		this.myVehicleTypeCollector= myVehicleTypeCollector;
		checkForPlugInHybrid();// if PHEV then deterministicLoad function changes for this one
		
		
		visualizeLoadDistributionGeneralAndPHEV();
		
		
		if (false==checkIfPricingAndDeterministicHaveSameTimeIntervals()){
			System.out.println("WRONG INPUT: Deterministic Load Distribution " +
					"does not have same time intervals as pricing Distribution");
			controler.wait();
		}
		
		initializeLoadAfterDeterministicChargingDecision();
		
		initializeConnectivityHubDistribution();
		
	}
	
	
	
	public void checkForPlugInHybrid() throws IOException{
		
		PlugInHybridElectricVehicle p= new PlugInHybridElectricVehicle(new IdImpl(1));
		
		if (myVehicleTypeCollector.containsVehicleTypeForThisVehicle(p)){
			
			double gasPriceInCostPerSecond; // cost/second = cost/liter * liter/joules * joules/second			
			
			GasType phevGasType= myVehicleTypeCollector.getGasType(p);
			double engineWatt =  myVehicleTypeCollector.getWattOfEngine(p);
			gasPriceInCostPerSecond=phevGasType.getPricePerLiter() * 1/(phevGasType.getJoulesPerLiter()) * engineWatt;
						
			deterministicHubLoadDistributionPHEVAdjusted=getPHEVDeterministicHubLoad(gasPriceInCostPerSecond);
			visualizePricingAndGas(gasPriceInCostPerSecond);
			
		}else{
			deterministicHubLoadDistributionPHEVAdjusted=deterministicHubLoadDistribution;
		}
		
	}
	
	
	
	public void setStochasticSources(
			LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution,
			LinkedListValueHashMap<Integer, Schedule> locationSourceMapping,
			LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping){
		
		this.stochasticHubLoadDistribution=stochasticHubLoadDistribution; // continuous functions		
		this.locationSourceMapping=locationSourceMapping;
		this.agentVehicleSourceMapping=agentVehicleSourceMapping;
	}
	
	
	public void initializeConnectivityHubDistribution(){
		connectivityHubDistribution= new LinkedListValueHashMap<Integer, TimeDataCollector> ();
		
		for(Integer i: deterministicHubLoadDistribution.getKeySet()){
			connectivityHubDistribution.put(i, new TimeDataCollector(DecentralizedSmartCharger.MINUTESPERDAY));
		}
		
	}
	
	
	
	/**
	 * Pricing functions should have same time intervals as deterministic time intervals
	 * this function checks returns true if this requirement is correct
	 * @return
	 */
	private boolean checkIfPricingAndDeterministicHaveSameTimeIntervals(){
		boolean isSame=false;
		
		if(pricingHubDistribution.getKeySet().size()!= 
			deterministicHubLoadDistribution.getKeySet().size()){
			return isSame;
		}else{
			
			for(Integer i: pricingHubDistribution.getKeySet()){
				isSame=pricingHubDistribution.getValue(i).sameTimeIntervalsInThisSchedule(
						deterministicHubLoadDistribution.getValue(i));				
				
			}
		}
		return isSame;
		
	}
	
	
	
	
	
	/**
	 * return the Hub corresponding to a certain idLink
	 * @param idLink
	 * @return
	 */
	public int getHubForLinkId(Id idLink){
		
		int hubNumber= (int) hubLinkMapping.getHubNumber(idLink.toString()); //returns Number
		
		return hubNumber;
	}
	
	
	public Schedule getDeterministicHubLoadDistribution(int hubId){
		return deterministicHubLoadDistribution.getValue(hubId);
	}
	
	public Schedule getDeterministicHubLoadDistributionPHEVAdjusted(int hubId){
		return deterministicHubLoadDistributionPHEVAdjusted.getValue(hubId);
	}
	
	
	/**
	 * gets the loadDistribution Function for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public PolynomialFunction getDeterministicLoadPolynomialFunctionAtLinkAndTime(Id agentId, Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		Schedule hubLoadSchedule;
		
		if(DecentralizedSmartCharger.hasAgentEV(agentId)){
			hubLoadSchedule = deterministicHubLoadDistribution.getValue(hub);
		}else{
			hubLoadSchedule = deterministicHubLoadDistributionPHEVAdjusted.getValue(hub);
		}
		
		
		int interval = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
				
		LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(interval);
		
		return l1.getPolynomialFunction();
		
	}
	

	
	/**
	 * gets the pricing Function for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public PolynomialFunction getPricingPolynomialFunctionAtLinkAndTime(Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = pricingHubDistribution.getValue(hub);
		int interval = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
				
		LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(interval);
		
		return l1.getPolynomialFunction();
		
	}
	
	
	/**
	 * 
	 * @param agentId - Id agent
	 * @param idLink - location Link
	 * @return
	 */
	public Schedule getLoadDistributionScheduleForHubId(Id agentId, Id idLink){
		int hub= getHubForLinkId(idLink);
		if(DecentralizedSmartCharger.hasAgentEV(agentId)){
			return deterministicHubLoadDistribution.getValue(hub);
		}else{
			return deterministicHubLoadDistributionPHEVAdjusted.getValue(hub);
		}
		
		
	}
	

	
	public PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		DecentralizedSmartCharger.polyFit.clearObservations();
		
		for (int i=0;i<data.length;i++){
			DecentralizedSmartCharger.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		
		PolynomialFunction poly = DecentralizedSmartCharger.polyFit.fit();
		 
		DecentralizedSmartCharger.polyFit.clearObservations();
		return poly;
	}
	
	
	
	
	private void  initializeLoadAfterDeterministicChargingDecision(){
		loadAfterDeterministicChargingDecision= new LinkedListValueHashMap<Integer, double [][]>();
		originalDeterministicChargingDistribution= new LinkedListValueHashMap<Integer, double [][]>();
		
		for(Integer i : deterministicHubLoadDistribution.getKeySet()){
			Schedule s= deterministicHubLoadDistribution.getValue(i);
			
			double [][] loadBefore= new double [ (int)DecentralizedSmartCharger.MINUTESPERDAY ][2];
			double [][] loadAfter= new double [ (int)DecentralizedSmartCharger.MINUTESPERDAY ][2];
			for(int j=0; j<DecentralizedSmartCharger.MINUTESPERDAY; j++){
				
				double second= j*DecentralizedSmartCharger.SECONDSPERMIN;
				
				int interval= s.timeIsInWhichInterval(second);
				LoadDistributionInterval l= (LoadDistributionInterval) s.timesInSchedule.get(interval);
				PolynomialFunction func= l.getPolynomialFunction();
				
				loadBefore[j][0]=second; //time in second
				loadBefore[j][1]=func.value(second); // Watt at second
				
				loadAfter[j][0]=second; //time in second
				loadAfter[j][1]=func.value(second); // Watt at second
			}
			loadAfterDeterministicChargingDecision.put(i, loadAfter);
			originalDeterministicChargingDistribution.put(i, loadBefore);
		}
		
		
		
	}
	
	
	
	/**
	 * VISUALIZATION PURPOSE
	 * updates the double[] loadAfterDeterministicChargingDecision with charging activities of agents; 
	 * only done for first second in minute, to get an overall idea of load distribution for visualization purposes over the day
	 * 
	 * @param linkId
	 * @param minInDay
	 * @param wattReduction
	 */
	public void updateLoadAfterDeterministicChargingDecision(Id linkId, int minInDay, double wattReduction){
		
		
		int hubId= getHubForLinkId(linkId);
		
	
		double [][]loadAfter=loadAfterDeterministicChargingDecision.getValue(hubId);
		
		loadAfter[minInDay][0]=minInDay*DecentralizedSmartCharger.SECONDSPERMIN;
		loadAfter[minInDay][1]=loadAfter[minInDay][1]-wattReduction;
		                    
		loadAfterDeterministicChargingDecision.put(hubId, loadAfter);
		
		
	}
	
	
	
	/**
	 * provide minute in which agent is parking, and hub at which he is parking
	 * and the corresponding data value in connectivityHubDistribution will be increased by 1
	 * @param minute
	 * @param hub
	 */
	public void recordParkingAgentAtHubInMinute(
			int minute,
			int hub
			){
		
		double second= minute*DecentralizedSmartCharger.SECONDSPERMIN;
		double before= connectivityHubDistribution.getValue(hub).getYAtEntry(minute);
		connectivityHubDistribution.getValue(hub).addDataPoint(minute, second, before+1);
	}
	
	
	
	
	public double getExpectedNumberOfParkingAgentsAtHubAtTime(int hub, double time){
		return connectivityHubDistribution.getValue(hub).getFunction().value(time);
	}
	
	
	
	
	
	public void calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader() throws OptimizationException, IOException{
		
		XYSeriesCollection connectivity= new XYSeriesCollection();
		
		for(Integer i: connectivityHubDistribution.getKeySet()){
			
			TimeDataCollector data= connectivityHubDistribution.getValue(i);
			data.fitFunction();
			XYSeries xx= data.getXYSeries("Parking Vehicles at Hub"+ i);
			connectivity.addSeries(xx);
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart("Number of parking agents at hubs over the day", 
				"time [s]", 
				"number of agents", 
				connectivity, 
				PlotOrientation.VERTICAL, 
				true, true, false);
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
		
        int i=0;
        for(Integer j:  connectivityHubDistribution.getKeySet()){
        	//System.out.println(i);
        	plot.getRenderer().setSeriesPaint(i, Color.black);//after
        	plot.getRenderer().setSeriesStroke(
                    i, 
                  
                    new BasicStroke(
                        3.0f,  //float width
                        BasicStroke.CAP_ROUND, //int cap
                        BasicStroke.JOIN_ROUND, //int join
                        1.0f, //float miterlimit
                        new float[] {(1+i)*2.0f, i*3f}, //float[] dash
                        0.0f //float dash_phase
                    )
                );
        	i++;
        }
        String s= outputPath+ "Hub\\connectivityOfAgentsOverDay.png";
        ChartUtilities.saveChartAsPNG(new File(s) , chart, 1000, 1000);
       
		
	}
	
	
	
	
	
	/**
	 * method looks at the pricing functions.
	 * if the prices for charging are above the gas price, the determinstic load distribution profile will be adjusted for the PHEV case.
	 * those time intervals will be turned into suboptimal parking slots
	 * @return
	 */
	private LinkedListValueHashMap<Integer, Schedule> getPHEVDeterministicHubLoad(double gasPriceInCostPerSecond){
		
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistributionPHEVAdjusted= new LinkedListValueHashMap<Integer, Schedule> ();
		
		for(Integer i : pricingHubDistribution.getKeySet()){
			
			Schedule pricingS= pricingHubDistribution.getValue(i);
			
			Schedule deterministicSchedule=deterministicHubLoadDistribution.getValue(i);
			
			Schedule sPHEV= new Schedule();
			for(int j=0; j<pricingS.getNumberOfEntries(); j++){
				
				LoadDistributionInterval currentDeterministicLoadInterval= (LoadDistributionInterval)deterministicSchedule.timesInSchedule.get(j);
				
				LoadDistributionInterval currentPricingLoadInterval= (LoadDistributionInterval)pricingS.timesInSchedule.get(j);
				
				PolynomialFunction func= currentPricingLoadInterval.getPolynomialFunction();
				
				double [] d= func.getCoefficients();
				d[0]=d[0]-gasPriceInCostPerSecond;
				
				PolynomialFunction f = new PolynomialFunction(d);
				
				ArrayList<ChargingInterval> badIntervals= new ArrayList<ChargingInterval> ();
				
				checkRoot(currentDeterministicLoadInterval, 
						f, 
						gasPriceInCostPerSecond,
						badIntervals);
				
				PolynomialFunction pBad= new PolynomialFunction(new double[]{-1000000});
				
				if(badIntervals.size()==0){
					sPHEV.addTimeInterval(currentDeterministicLoadInterval);
					
				}else{
					
					
					// IF BAD INTERVALS DONT START AT BEGINNING OF INTERVAL
					if(currentDeterministicLoadInterval.getStartTime()<badIntervals.get(0).getStartTime()){
						
						LoadDistributionInterval lReplaceSubOptimal= new LoadDistributionInterval(currentDeterministicLoadInterval.getStartTime(), 
								badIntervals.get(0).getStartTime(), 
								currentDeterministicLoadInterval.getPolynomialFunction(), 
								currentDeterministicLoadInterval.isOptimal());
						sPHEV.addTimeInterval(lReplaceSubOptimal);
					}
					
					//FOR ALL BAD INTERVALS
					
					for(int u=0; u<badIntervals.size(); u++){
						
						TimeInterval t= badIntervals.get(u);
						LoadDistributionInterval lReplaceSubOptimal= new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								pBad, 
								false);
						
						sPHEV.addTimeInterval(lReplaceSubOptimal);
						
						
						//****************good interval between to  consecutive bad intervals
						// add good interval
						
						if(u<badIntervals.size()-1){// if second to last or before
							
							if(badIntervals.get(u).getEndTime() < badIntervals.get(u+1).getStartTime()){
								LoadDistributionInterval lReplaceSubOptimal2= new LoadDistributionInterval(
										badIntervals.get(u).getEndTime(), 
										badIntervals.get(u+1).getStartTime(), 
										currentDeterministicLoadInterval.getPolynomialFunction(), 
										currentDeterministicLoadInterval.isOptimal());
								sPHEV.addTimeInterval(lReplaceSubOptimal2);
							}
						}
						
					}
					
					// IF BAD INTERVALS DONT STOP AT END OF INTERVAL
					double currentEnd= currentDeterministicLoadInterval.getEndTime();
					double badIntervalEnd= badIntervals.get(badIntervals.size()-1 ).getEndTime();
					
					if(currentEnd>badIntervalEnd){
						
						// if objective f >0 in last intervcal, then PHEV is bad interval
						LoadDistributionInterval lReplaceSubOptimal;
						if(f.value( (currentEnd+badIntervalEnd)/2)>0){
							lReplaceSubOptimal= new LoadDistributionInterval(
									badIntervals.get(badIntervals.size()-1 ).getEndTime(), 
									currentDeterministicLoadInterval.getEndTime(), 
									pBad,
									currentDeterministicLoadInterval.isOptimal());
						}else{
							lReplaceSubOptimal= new LoadDistributionInterval(
									badIntervals.get(badIntervals.size()-1 ).getEndTime(), 
									currentDeterministicLoadInterval.getEndTime(), 
									currentDeterministicLoadInterval.getPolynomialFunction(), 
									currentDeterministicLoadInterval.isOptimal());
						}
						
						sPHEV.addTimeInterval(lReplaceSubOptimal);
					}
					
				}
				
				
			}
			
			sPHEV.sort();
			hubLoadDistributionPHEVAdjusted.put(i, sPHEV);
			
		}
		
		return hubLoadDistributionPHEVAdjusted;
		
	}




	/**
	 * method is called within getPHEVDeterministicHubLoad()
	 * 
	 *it finds the roots=the times, where the price equals the gas price
	 *
	 * @param l
	 * @param objective
	 * @param badIntervals
	 */
	private void checkRoot(LoadDistributionInterval l, 
			PolynomialFunction objective, 
			double gasPriceInCostPerSecond, // function - gasprice
			ArrayList<ChargingInterval> badIntervals){
		
		
		//*********************************
		
		if(objective.degree()==0){
			//constant
			if(objective.getCoefficients()[0]>0.0){
				// then entire interval is 
				badIntervals.add(new ChargingInterval(l.getStartTime(), l.getEndTime()));
			}
			
		}else{
			if(objective.degree()==1){
				//linear
				double c;
				try {
					c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime());
					if(c<=l.getEndTime() && c>=l.getStartTime()){
						
						//contains bad interval
						
						if(objective.value((l.getStartTime()+c)/2)>0){
							badIntervals.add(new ChargingInterval(l.getStartTime(), c));
						}else{
							badIntervals.add(new ChargingInterval( c,l.getEndTime()));
						}
						
					}
				} catch (ConvergenceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FunctionEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
				
			}else{
				//anything else
				ArrayList<Double> roots = new ArrayList<Double>(0);
				
				//
				for(double i=l.getStartTime(); i<=l.getEndTime();  ){
					
					if(Math.abs(objective.value(i))<0.0001){
						
						try {
							double c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime(), i);
							//System.out.println("c: "+c);
							//System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime()+ " guess "+ i);
							if(c<=l.getEndTime() && c>=l.getStartTime()){
								// if roots are found multiple times
								// check first, if 'same (error 1 minute)' root has been founded before already
								if (roots.size()>1){
									if(Math.abs(roots.get(roots.size()-1))-c>60.0 ){
										roots.add(c);
									}
								}
								if(roots.size()==0){
									roots.add(c);
								}
								
							}
						} catch (ConvergenceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (FunctionEvaluationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					i+=60;
				}
				
				double start= l.getStartTime();
				boolean good;
				if(objective.value(l.getStartTime())<0)
				{good=true;}else{good=false;}
				
				
				// now clean up root array... find bad intervals
				for(int i=0; i<roots.size();i++){
					
					if(start!=roots.get(i) ){
						if(good){
							start=roots.get(i);
							good=false;
						}else{
							badIntervals.add(new ChargingInterval(start, roots.get(i)));
							start=roots.get(i);
							good=true;
						}
					}
					
				}
				if(start!=l.getEndTime()){
					//add last interval
					if(!good){
						badIntervals.add(new ChargingInterval(start, l.getEndTime()));
					}
				}
				
				
			}
			
		}
	}
	

	
	
	/*
	 * Debugging purposes convenience method
	 */
	private void printArrayList(ArrayList<ChargingInterval> b){
		for(int i=0; i<b.size();i++){
			b.get(i).printInterval();
		}
	}

	
	private void visualizePricingAndGas(double gasPriceInCostPerSecond) throws IOException{
		for( Integer i : pricingHubDistribution.getKeySet()){
			
			XYSeriesCollection prices= new XYSeriesCollection();
			//************************************
			//AFTER//BEFORE
			
			XYSeries hubPricing= new XYSeries("hub"+i.toString()+"pricing");
			XYSeries gasPriceXY= new XYSeries("gasprice");
			
			gasPriceXY.add(0, gasPriceInCostPerSecond);
			gasPriceXY.add(DecentralizedSmartCharger.SECONDSPERDAY,gasPriceInCostPerSecond);
			
			for(int j=0; j<pricingHubDistribution.getValue(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)pricingHubDistribution.getValue(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					hubPricing.add(time, l.getPolynomialFunction().value(time)); 
				}
				
				
			}
			
			prices.addSeries(hubPricing);
			prices.addSeries(gasPriceXY);
			//************************************
			
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Prices at Hub "+ i.toString(), 
					"time [s]", 
					"available load [W]", 
					prices, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.black);//after
	        
	        plot.getRenderer().setSeriesPaint(1, Color.red);//after
	        
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
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\pricesHub_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
		
	}

	
	
	
	private void visualizeLoadDistributionGeneralAndPHEV() throws IOException{
		
				
		for( Integer i : deterministicHubLoadDistribution.getKeySet()){
			
			XYSeriesCollection deterministicGeneral= new XYSeriesCollection();
			
			//************************************
			//AFTER//BEFORE
			
			XYSeries hubDeterministic= new XYSeries("hub"+i.toString()+"Deterministic");
						
			for(int j=0; j<deterministicHubLoadDistribution.getValue(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)deterministicHubLoadDistribution.getValue(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					hubDeterministic.add(time, l.getPolynomialFunction().value(time)); 
				}
				
				
			}
			deterministicGeneral.addSeries(hubDeterministic);
				
		
			
			
			XYSeries hubDeterministicPHEV= new XYSeries("hub"+i.toString()+"DeterministicPHEV");
			//deterministicHubLoadDistributionPHEVAdjusted.getValue(i).printSchedule();
			
			for(int j=0; j<deterministicHubLoadDistributionPHEVAdjusted.getValue(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)deterministicHubLoadDistributionPHEVAdjusted.getValue(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					
					//System.out.println("added"+ time + " , "+ l.getPolynomialFunction().value(time));
					hubDeterministicPHEV.add(time, l.getPolynomialFunction().value(time)); 
				}
				
				
			}
			
			deterministicGeneral.addSeries(hubDeterministicPHEV);
		
			
			//************************************
			
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Deterministic LoadDistribution "+ i.toString(), 
					"time [s]", 
					"load [W]", 
					deterministicGeneral, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.black);//after
	        plot.getRenderer().setSeriesPaint(1, Color.red);//after
	        
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
    	                1.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {6.0f, 6.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\hubDeterministic_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
		}
		
	}
	

