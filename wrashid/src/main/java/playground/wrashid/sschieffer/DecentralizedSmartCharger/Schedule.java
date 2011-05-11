/* *********************************************************************** *
 * project: org.matsim.*
 * Schedule.java
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
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;


/**
 * A schedule is a LinkedList of timeIntervals.
 * It helps to store related time intervals or activties in one object
 * i.e. to keep the daily plans of an agent and to sort his activities.
 * or to store all charging intervals during one parking interval
 * 
 * @author Stella
 *
 */
public class Schedule {
	
	double totalJoulesInOptimalParkingTimes=0;
	double totalJoulesInSubOptimalParkingTimes=0;
	private double startingSOC;
	
	LinkedList<TimeInterval> timesInSchedule= new LinkedList<TimeInterval>();
	
	
	public Schedule(){
		
	}
	
	
	public void clearJoules(){
		totalJoulesInOptimalParkingTimes=0;
		totalJoulesInSubOptimalParkingTimes=0;
		clearJoulesInIntervals();
		
	}
	
	
	private void clearJoulesInIntervals(){
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval t = timesInSchedule.get(i);
			
			if(t.isParking()){
				((ParkingInterval) t).setJoulesInPotentialChargingInterval(0.0);
				
			}
		}
	}
	
	
	
	public int getNumberOfEntries(){
		return timesInSchedule.size();
	}


	public double getStartingSOC(){
		return startingSOC;
	}


	public double getTotalConsumption(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getConsumption();
			}
		}
		return total;
	}
	
	
	
	public double getTotalConsumptionFromEngine(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getExtraConsumption();
			}
		}
		return total;
	}
	
	
	
	public double getTotalDrivingTimeWithoutEnginePower(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getEngineTime();
			}
		}
		return total;
	}
	
	
	
	
	public double getTotalTimeOfIntervalsInSchedule(){
		double totalTime=0;
		
		for(int i=0; i<getNumberOfEntries();i++){
			totalTime=totalTime+timesInSchedule.get(i).getIntervalLength();
		} 
		return totalTime;
	}
	
	
	
	public void addTimeInterval(TimeInterval t){
		timesInSchedule.add(t);
	}
	
	public void sort(){
		Collections.sort(timesInSchedule);
	}
	
	public void printSchedule(){
		System.out.println("*************************");
		System.out.println("Starting SOC: "+ getStartingSOC());
		for(TimeInterval t: timesInSchedule){
			t.printInterval();
		}
		System.out.println("*************************");
		
	}
	
	public void mergeSchedules(Schedule schedule2){
		for(int i=0; i< schedule2.getNumberOfEntries(); i++){
			timesInSchedule.add(schedule2.timesInSchedule.get(i));
		}
		
		this.sort();
	}
	
	
	public void setStartingSOC(double SOC){
		startingSOC=SOC;
	}
	
	
	
	/**
	 * only meant for schedules with loaddistributionIntervals,
	 * 
	 * passed String is title String of diagram
	 * @param name
	 * @throws IOException
	 */
	
	public void visualizeLoadDistribution(String name) throws IOException{
		XYSeriesCollection loadDistributionIntervals = new XYSeriesCollection();
				
		for(int i=0; i<getNumberOfEntries();i++){
			TimeInterval t= timesInSchedule.get(i);
			if(t.isLoadDistributionInterval()){
				LoadDistributionInterval t2=(LoadDistributionInterval) t;
				loadDistributionIntervals.addSeries(t2.getXYSeries(""));
			}
		}
		
        
        JFreeChart chart = ChartFactory.createXYLineChart(
        		name, "time of day", "free load [W]", loadDistributionIntervals, 
        		PlotOrientation.VERTICAL, false, true, false);
        
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(DecentralizedSmartCharger.supplier);
    
        plot.getRenderer().setSeriesPaint(0, Color.black);
        plot.getRenderer().setSeriesStroke(
	            0, 
	            new BasicStroke(
	                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
	                1.0f, new float[] {10.0f, 6.0f}, 5.0f
	            )
	        );
	   
	    
        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray); 
      
        ChartUtilities.saveChartAsPNG(
        		new File(DecentralizedSmartCharger.outputPath+"Hub\\"+ name+".png") , 
        		chart, 800, 600);
       
	}
	
	
	/**
	 * checks if there is an overlap with any of the Timeintervals in the schedule and the specified TimeInterval
	 * 
	 * returns true if overlap
	 * false if no overlap
	 * 
	 * @param t
	 * @return
	 */
	public boolean overlapWithTimeInterval(TimeInterval t){
		boolean overlap=false;
		
		for(int i=0; i<timesInSchedule.size(); i++){
			TimeInterval thisT= timesInSchedule.get(i);
			
			if(   (t.getStartTime()>=thisT.getStartTime()  && t.getStartTime()<thisT.getEndTime())
					|| (t.getEndTime()>thisT.getStartTime()  && t.getEndTime()<=thisT.getEndTime())){
				//if overlap
				overlap=true;
			}
		}
		
		return overlap;
	}
	
	
	public int timeIsInWhichInterval(double time){
		int solution=-1;
		for (int i=0; i<timesInSchedule.size(); i++){
			if(time<timesInSchedule.get(i).getEndTime() && 
					time>=timesInSchedule.get(i).getStartTime()){
				solution =i;
			}				
		}
		
		if(time==timesInSchedule.get(timesInSchedule.size()-1).getEndTime()){
			solution =timesInSchedule.size()-1;
		}
		return solution;
	}
	
	
	public int intervalIsInWhichTimeInterval(TimeInterval t){
		int solution =-1;
		for (int i=0; i<timesInSchedule.size(); i++){
			if(t.getEndTime()<=timesInSchedule.get(i).getEndTime() && 
					t.getStartTime()>=timesInSchedule.get(i).getStartTime()){
				solution =i;
			}
		}	
			
		return solution;
	}
	
	
	
	public void addJoulesToTotalSchedule(double joules){
		if(joules<=0){
			totalJoulesInSubOptimalParkingTimes+=joules;
		}
		else{
			totalJoulesInOptimalParkingTimes+=joules;
		}
	}
	
	
	public int numberOfDrivingTimes(){
		int count=0;
		for(int i=0; i<timesInSchedule.size(); i++){
			if(timesInSchedule.get(i).isDriving()){
				count++;
			}
		}
		return count;
	}
	
	
	public int positionOfIthDrivingTime(int ithTime){
		int sol=0;
		int count=0;
		//this.printSchedule();
		
		for(int i=0; i<timesInSchedule.size(); i++){
			
			if(timesInSchedule.get(i).isDriving()){
				if(count == ithTime){
					sol= i;
					i=timesInSchedule.size();
					break;
				}
				
				if(count<ithTime){
					count++;
				
				}
			}
		}
		return sol;
	}
	
	
	/**
	 * checks if second is within one interval of the schedule
	 * true if in interval
	 * 
	 * @param sec
	 * @return
	 */
	public boolean isSecondWithinOneInterval(double sec){
		boolean inInterval=false;
		
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval t= timesInSchedule.get(i);
			if(sec<=t.getEndTime() && sec>=t.getStartTime()){
				// in interval
				inInterval=true;
			}
		}
		return inInterval;
		
	}
	
	
	public boolean sameTimeIntervalsInThisSchedule(Schedule s){
		boolean isSame=false;
		
		if (this.getNumberOfEntries()!=s.getNumberOfEntries()){
			
			return  isSame;
		}else{
			
			for(int i=0; i<this.getNumberOfEntries();i++){
				if(this.timesInSchedule.get(i).getStartTime()==s.timesInSchedule.get(i).getStartTime()){
					isSame=true;
				}else{return false;}
				
				if(this.timesInSchedule.get(i).getEndTime()==s.timesInSchedule.get(i).getEndTime()){
					isSame=true;
				}else{return false;}
				
			}
		}
		
		return isSame;
		
		
	}
	
	
	
	public void insertChargingIntervalsIntoParkingIntervalSchedule(Schedule intervalsToPutIn){
		//
//		System.out.println("InsertChargingInterval: Schedule into which toinsert:");
//		printSchedule();
//		System.out.println("InsertChargingInterval: ChargingSChedule to insert:");
//		intervalsToPutIn.printSchedule();
		
		
		
		for(int i=0; i<intervalsToPutIn.getNumberOfEntries(); i++){
			TimeInterval t= intervalsToPutIn.timesInSchedule.get(i);
			int interval= intervalIsInWhichTimeInterval(t);
			
			//chck value of interval
			if (interval==-1){
				//trouble
				System.out.println("time is not in schedule: "+ t);
				printSchedule();
			}
			double start=timesInSchedule.get(interval).getStartTime();//currently in schedule
			double end=timesInSchedule.get(interval).getEndTime();
			
			Id linkId = ((ParkingInterval) timesInSchedule.get(interval)).getLocation();
			boolean type= ((ParkingInterval) timesInSchedule.get(interval)).isInSystemOptimalChargingTime();
				
			deleteIntervalAtEntry(interval);
			
			//before 
			if(t.getStartTime()>start){
				ParkingInterval p= new ParkingInterval(start, t.getStartTime(), linkId);
				p.setParkingOptimalBoolean(type);
				p.setRequiredChargingDuration(0);
				addTimeInterval(p);
			}
			
			//charging
			if(t.getStartTime()>start){
				ChargingInterval p= new ChargingInterval(t.getStartTime(),t.getEndTime());
				
				addTimeInterval(p);
			}
			
			//after
			if(end>t.getEndTime()){
				ParkingInterval p= new ParkingInterval(t.getEndTime(),end, linkId);
				p.setParkingOptimalBoolean(type);
				p.setRequiredChargingDuration(0);
				addTimeInterval(p);
			}
			sort();
		}
		
	}
	
	
	public void addLoadDistributionIntervalToExistingLoadDistributionSchedule(LoadDistributionInterval loadToAdd) 
	throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		//LinkedList<Integer> toDelete = new LinkedList<Integer>();
		printSchedule();
		
		if (overlapWithTimeInterval(loadToAdd)
			){
			Schedule newSchedule=new Schedule();
			
			LoadDistributionInterval loadLeftToAdd= loadToAdd.clone();
			
			for(int i=0; i<getNumberOfEntries();i++){
				LoadDistributionInterval t=  (LoadDistributionInterval)timesInSchedule.get(i);
				
				if( t.overlap(loadLeftToAdd) ||
						(loadLeftToAdd.getStartTime()==t.getStartTime()
								&& loadLeftToAdd.getEndTime()==t.getEndTime())){
					
					// interval Before LoadLeftToAdd
					if(t.getStartTime()<loadLeftToAdd.getStartTime()){
						newSchedule.addTimeInterval(new LoadDistributionInterval(t.getStartTime(), loadLeftToAdd.getStartTime(),
								t.getPolynomialFunction() , 
								t.isOptimal()));
					}
					
					
					
					if(loadLeftToAdd.getEndTime()>t.getEndTime()){
						// NOT fully in	
						//overlappingPart till interval End
						
						PolynomialFunction combinedFunc= t.getPolynomialFunction().add(loadLeftToAdd.getPolynomialFunction());
						boolean newOptimal;
						if(DecentralizedSmartCharger.functionIntegrator.integrate(combinedFunc, 
								loadLeftToAdd.getStartTime(), 
								t.getEndTime())>=0){
							newOptimal=true;
						}else{
							newOptimal=false;
						}
						
						//add overlapping new LoadINterval
						newSchedule.addTimeInterval(new LoadDistributionInterval(loadLeftToAdd.getStartTime(),
								t.getEndTime(),
								combinedFunc, 
								newOptimal));
						newSchedule.printSchedule();
						
						//update loadLeftTOAdd
						loadLeftToAdd=new LoadDistributionInterval(t.getEndTime(), 
								loadLeftToAdd.getEndTime(), 
								loadLeftToAdd.getPolynomialFunction(), 
								loadLeftToAdd.isOptimal());
						
					}else{
						PolynomialFunction combinedFunc= t.getPolynomialFunction().add(loadLeftToAdd.getPolynomialFunction());
						boolean newOptimal;
						if(DecentralizedSmartCharger.functionIntegrator.integrate(combinedFunc, 
								loadLeftToAdd.getStartTime(), 
								loadLeftToAdd.getEndTime())>=0){
							newOptimal=true;
						}else{
							newOptimal=false;
						}
						
						newSchedule.addTimeInterval(new LoadDistributionInterval(loadLeftToAdd.getStartTime(),
								loadLeftToAdd.getEndTime(),
								combinedFunc , 
								newOptimal));
						newSchedule.printSchedule();
						
						if(loadLeftToAdd.getEndTime()<t.getEndTime()){
							newSchedule.addTimeInterval(new LoadDistributionInterval(loadLeftToAdd.getEndTime(),
									t.getEndTime(),
									t.getPolynomialFunction() , 
									t.isOptimal()));
							newSchedule.printSchedule();
						}
						loadLeftToAdd= new LoadDistributionInterval(0, 0, null, false);
					}
					
				}else{
					newSchedule.addTimeInterval(t);
					newSchedule.printSchedule();
				}
				
			}	
			
			if(loadLeftToAdd.getIntervalLength()>0){
				newSchedule.addTimeInterval(loadLeftToAdd);
				newSchedule.printSchedule();
			}
			
			this.timesInSchedule= newSchedule.timesInSchedule;
			sort();
			printSchedule();
		}else{//if no overlap between schedule and interval
			addTimeInterval(loadToAdd.clone());
			printSchedule();
			sort();
			
		}
		
	}
	
	
	
	
	
	
	public void deleteIntervalAtEntry(int i){
		timesInSchedule.remove(i);
	}
	
	
	
	
	
}
