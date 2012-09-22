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

package playground.michalm.vrp.taxi.taxicab;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.model.TaxiCustomer;
import playground.michalm.vrp.data.network.MatsimArc;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;
import playground.michalm.vrp.taxi.TaxiSimEngine;


public class TaxiAgentLogic
    implements DynAgentLogic
{
    private final TaxiSimEngine taxiSimEngine;

    private final Vehicle vrpVehicle;
    private DynAgent agent;

    private Request currentRequest;


    public TaxiAgentLogic(Vehicle vrpVehicle, TaxiSimEngine taxiSimEngine)
    {
        this.vrpVehicle = vrpVehicle;
        this.taxiSimEngine = taxiSimEngine;
    }


    @Override
    public DynActivity init(DynAgent adapterAgent)
    {
        this.agent = adapterAgent;
        return createBeforeScheduleActivity();// INITIAL ACTIVITY (activate the agent in QSim)
    }


    @Override
    public DynAgent getDynAgent()
    {
        return agent;
    }


    @Override
    public DynAction computeNextAction(DynAction oldAction, double now)
    {
        if (oldAction instanceof TaxiLeg) {
            ((TaxiLeg)oldAction).endLeg(now);// handle passenger-related stuff
        }

        return scheduleNextTask(now);
    }


    @Override
    public void notifyMoveOverNode(Id oldLinkId, Id newLinkId)
    {}


    public void schedulePossiblyChanged()
    {
        agent.update();
    }


    private DynAction scheduleNextTask(double now)
    {
        Schedule schedule = vrpVehicle.getSchedule();
        ScheduleStatus status = schedule.getStatus();

        if (status == ScheduleStatus.UNPLANNED) {
            return createAfterScheduleActivity();// FINAL ACTIVITY (deactivate the agent in QSim)
        }

        int time = (int)now;

        if (status == ScheduleStatus.STARTED) {// TODO: should also be called if PLANNED???????
            taxiSimEngine.updateAndOptimizeBeforeNextTask(vrpVehicle, now);
        }

        Task task = schedule.nextTask();
        status = schedule.getStatus();// REFRESH status!!!

        if (status == ScheduleStatus.COMPLETED) {
            return createAfterScheduleActivity();// FINAL ACTIVITY (deactivate the agent in QSim)
        }

        if (task.getSchedule().getVehicle().getId() == printVehId) {
            System.err.println("NEXT TASK: " + task + " time=" + now);
        }

        switch (task.getType()) {
            case DRIVE: // driving both with and without passengers
                // ======DEBUG PRINTOUTS======
                // DriveTask dt = (DriveTask)task;
                // Id fromLinkId = ((MATSimVertex)dt.getFromVertex()).getLink().getId();
                // Id toLinkId = ((MATSimVertex)dt.getToVertex()).getLink().getId();
                //
                // System.err.println("************");
                // System.err.println("Drive task: " + task + " for Veh: " + vrpVehicle.getId()
                // + " agent: " + agent.getId());
                // System.err.println("fromLink" + fromLinkId + " toLink: " + toLinkId);

                if (currentRequest != null) {
                    DynLeg leg = createLegWithPassenger((DriveTask)task, time, currentRequest);
                    currentRequest = null;
                    return leg;
                }
                else {
                    return createLeg((DriveTask)task, time);
                }

            case SERVE: // pick up passenger
                return createServeActivity((ServeTask)task, now);

            case WAIT:
                return TaxiTaskActivity.createWaitActivity((WaitTask)task);

            default:
                throw new IllegalStateException();
        }
    }


    private final int printVehId = -13;


    // ========================================================================================

    private DynActivity createBeforeScheduleActivity()
    {
        return new DynActivityImpl("Before schedule: " + vrpVehicle.getId(), -1) {

            @Override
            public double getEndTime()
            {
                Schedule s = vrpVehicle.getSchedule();

                switch (s.getStatus()) {
                    case PLANNED:
                        return s.getBeginTime();
                    case UNPLANNED:
                        return vrpVehicle.getT1();
                    default:
                        throw new IllegalStateException();
                }
            }
        };

    }


    private DynActivity createAfterScheduleActivity()
    {
        return new DynActivityImpl("After schedule: " + vrpVehicle.getId(),
                Double.POSITIVE_INFINITY);
    }


    // ========================================================================================

    // picking-up a passenger
    private TaxiTaskActivity createServeActivity(ServeTask task, double now)
    {
        currentRequest = task.getRequest();

        // serve the customer
        MobsimAgent passenger = ((TaxiCustomer)currentRequest.getCustomer()).getPassenger();
        Id currentLinkId = passenger.getCurrentLinkId();

        if (currentLinkId != agent.getCurrentLinkId()) {
            throw new IllegalStateException("Passanger and taxi on different links!");
        }

        // if
        // (taxiSimEngine.getMobsim().unregisterAdditionalAgentOnLink(passenger.getId(),currentLinkId)
        // == null) {
        if (taxiSimEngine.getInternalInterface().unregisterAdditionalAgentOnLink(passenger.getId(),
                currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for taxi");
        }

        // event handling
        EventsManager events = taxiSimEngine.getMobsim().getEventsManager();
        EventsFactory evFac = (EventsFactory)events.getFactory();
        events.processEvent(evFac.createPersonEntersVehicleEvent(now, passenger.getId(),
                agent.getId()));

        return TaxiTaskActivity.createServeActivity(task);
    }


    // ========================================================================================

    private TaxiLeg createLegWithPassenger(DriveTask driveTask, int realDepartTime,
            final Request request)
    {
        MatsimArc arc = (MatsimArc)driveTask.getArc();
        ShortestPath path = arc.getShortestPath(realDepartTime);
        Id destinationLinkId = arc.getToVertex().getLink().getId();

        return new TaxiLeg(path, destinationLinkId) {
            @Override
            public void endLeg(double now)
            {
                // following line only works if PassengerAgent can indeed be cast into MobsimAgent
                // ...
                // ... but that makes sense for what the current system is constructed. kai, sep'12
                MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassenger();

                // deliver the passenger
                EventsManager events = taxiSimEngine.getMobsim().getEventsManager();
                EventsFactory evFac = (EventsFactory)events.getFactory();
                events.processEvent(evFac.createPersonLeavesVehicleEvent(now, passenger.getId(),
                        agent.getId()));

                passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
                passenger.endLegAndComputeNextState(now);
                TaxiAgentLogic.this.taxiSimEngine.getInternalInterface().arrangeNextAgentState(
                        passenger);
            }
        };
    }


    private TaxiLeg createLeg(DriveTask driveTask, int realDepartTime)
    {
        MatsimArc arc = (MatsimArc)driveTask.getArc();
        ShortestPath path = arc.getShortestPath(realDepartTime);
        Id destinationLinkId = arc.getToVertex().getLink().getId();

        return new TaxiLeg(path, destinationLinkId);
    }
}
