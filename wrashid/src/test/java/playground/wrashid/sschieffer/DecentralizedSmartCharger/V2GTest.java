/* *********************************************************************** *
 * project: org.matsim.*
 * V2GTest.java
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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * checks if V2G methods ...
 * <li> cutScheduleAtTime
 * <li> cutChargingSchedule
 * <li> GetSOCAtTime
 * @author Stella
 *
 */
public class V2GTest extends TestCase{
	
	public  V2G someV2G= new V2G(null);
	
	public V2GTest(){
	}
	
	
	
	/*
	 * tests the schedule cutting procedures of the V2G class
	 */
	public void testV2GCutSchedule() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule someSchedule= setDummySchedule();
		System.out.println("schedule before");
		someSchedule.printSchedule();
		someSchedule=someV2G.cutScheduleAtTime(someSchedule, 40);
		System.out.println("schedule 1st half after cut at 40");
		someSchedule.printSchedule();
		
		assertEquals(4, someSchedule.getNumberOfEntries());
		assertEquals(40.0, someSchedule.timesInSchedule.get(3).getEndTime());
		
		someSchedule= setDummySchedule();
		System.out.println("schedule before");
		someSchedule.printSchedule();
		someSchedule=someV2G.cutScheduleAtTimeSecondHalf(someSchedule, 40);
		System.out.println("schedule second half after cut at 40");
		someSchedule.printSchedule();
		
		assertEquals(1, someSchedule.getNumberOfEntries());
		assertEquals(50.0, someSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(40.0, someSchedule.timesInSchedule.get(0).getStartTime());
	
	}
	
	
	/*
	 * test the charging schedule cutting procedures,
	 * charging schedules do not have to be continuous
	 */
	public void testV2GCutChargingSchedule(){
		
		
		Schedule someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("Schedule ");
		someChargingSchedule.printSchedule();
		
		System.out.println("cutting Schedule at 15, firstHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTime(someChargingSchedule, 15);
		someChargingSchedule.printSchedule();
		
		assertEquals(1,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		
		someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("cutting Schedule at 25, firstHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTime(someChargingSchedule, 25);
		someChargingSchedule.printSchedule();
		
		assertEquals(2,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(25.0,someChargingSchedule.timesInSchedule.get(1).getEndTime());
		assertEquals(20.0,someChargingSchedule.timesInSchedule.get(1).getStartTime());
		
		someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("cutting Schedule at 5, secondHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTimeSecondHalf(someChargingSchedule, 5);
		someChargingSchedule.printSchedule();
		assertEquals(2,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(5.0,someChargingSchedule.timesInSchedule.get(0).getStartTime());
		assertEquals(30.0,someChargingSchedule.timesInSchedule.get(1).getEndTime());
		assertEquals(20.0,someChargingSchedule.timesInSchedule.get(1).getStartTime());
		
		
	}
	

	
	
	/*
	 * tests the calculation of SOC at a certain time
	 */
	public void testGetSOCAtTime(DecentralizedSmartCharger mySmartCharger, Id agentId) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		V2G localV2G= new V2G(mySmartCharger);
		
		Schedule agentSchedule= mySmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(agentId);
		System.out.println("testV2G - testRegulationDown - agentSChedule: "+agentId.toString());
		agentSchedule.printSchedule();
		/*Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  0.0	  Optimal:  true	  Joules per Interval:  1.897038219281625E14
		Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
		Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  4635.074196830796	  Optimal:  true	  Joules per Interval:  6.53919159828E14
		Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
		Parking Interval 	 start: 38386.0	  end: 62490.0	  ChargingTime:  13988.57142857143	  Optimal:  true	  Joules per Interval:  3.5063335651397495E15
		Parking Interval 	 start: 62490.0	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -3.3411427243079994E14
		*/
		
		double socAt0= localV2G.getSOCAtTime(agentSchedule, 0);
		assertEquals(socAt0, agentSchedule.getStartingSOC());
		double differenceSOCInterval1= localV2G.getSOCAtTime(agentSchedule, 22846.0)-localV2G.getSOCAtTime(agentSchedule, 21609.0);
		assertEquals(differenceSOCInterval1, 1.6388045816871705E7);
		
	}
	
	
	
	
	
	public Schedule makeSimpleChargingSchedule(){
		Schedule someChargingSchedule= new Schedule();
		someChargingSchedule.addTimeInterval(new ChargingInterval(0, 10));
		someChargingSchedule.addTimeInterval(new ChargingInterval(20, 30));
		return someChargingSchedule;
	}
	
	public Schedule setDummySchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ParkingInterval(0, 10, null));
		s1.addTimeInterval(new DrivingInterval(10,15, 100.0) );
		s1.addTimeInterval(new ParkingInterval(15, 30, null));
		s1.addTimeInterval(new ParkingInterval(30, 50, null));
		
		
		return s1;
	}
	
}
