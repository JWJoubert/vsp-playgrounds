/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.run.online;

import static pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizationPolicy.*;
import static playground.michalm.vrp.run.online.AlgorithmConfig.AlgorithmType.*;
import static playground.michalm.vrp.run.online.AlgorithmConfig.TravelCostSource.*;
import static playground.michalm.vrp.run.online.AlgorithmConfig.TravelTimeSource.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizationPolicy;


public class AlgorithmConfig
{
    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED(24 * 60 * 60, 1), // no eventsFileName
        EVENTS_24_H(24 * 60 * 60, 1), // based on eventsFileName, with 24-hour time interval
        EVENTS_15_MIN(15 * 60, 24 * 4); // based on eventsFileName, with 15-minute time interval

        final int travelTimeBinSize;
        final int numSlots;


        private TravelTimeSource(int travelTimeBinSize, int numSlots)
        {
            this.travelTimeBinSize = travelTimeBinSize;
            this.numSlots = numSlots;
        }
    }


    public enum TravelCostSource
    {
        TIME, // travel time
        DISTANCE; // travel distance
    }


    public static enum AlgorithmType
    {
        NO_SCHEDULING, // only idle vehicles
        ONE_TIME_SCHEDULING, // formerly "optimistic"
        RE_SCHEDULING, // formerly "pessimistic"
        PRE_ASSIGNMENT; // according to the "offline" mode
    }


    public static AlgorithmConfig NOS_STRAIGHT_LINE = new AlgorithmConfig(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig NOS_TRAVEL_DISTANCE = new AlgorithmConfig(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig NOS_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig NOS_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig NOS_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig OTS_REQ_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig OTS_REQ_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig OTS_REQ_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig OTS_DRV_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    public static AlgorithmConfig OTS_DRV_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    public static AlgorithmConfig OTS_DRV_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    public static AlgorithmConfig RES_REQ_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig RES_REQ_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig RES_REQ_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    public static AlgorithmConfig RES_DRV_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    public static AlgorithmConfig RES_DRV_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    public static AlgorithmConfig RES_DRV_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    final TravelTimeSource ttimeSource;
    final TravelCostSource tcostSource;
    final AlgorithmType algorithmType;
    final TaxiOptimizationPolicy optimizationPolicy;


    public AlgorithmConfig(TravelTimeSource ttimeSource, TravelCostSource tcostSource,
            AlgorithmType algorithmType, TaxiOptimizationPolicy optimizationPolicy)
    {
        this.ttimeSource = ttimeSource;
        this.tcostSource = tcostSource;
        this.algorithmType = algorithmType;
        this.optimizationPolicy = optimizationPolicy;
    }
}
