/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.sim.network.queueNetwork;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.network.LinkImpl;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

/*package*/ class QueueLink implements SimLink, Steppable {

	private final static Logger log = Logger.getLogger(QueueLink.class);

	private final QueueNetwork network;
	private final Link link;

	/* DRIVING VEHICLE QUEUE */

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final LinkedList<SimVehicle> vehQueue = new LinkedList<SimVehicle>();
	private final HashMap<SimVehicle, Double> earliestLeaveTimes = new HashMap<SimVehicle, Double>();

	private double storageCapacity = 0.0;

	/* BUFFER */

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<SimVehicle> buffer = new LinkedList<SimVehicle>();
	private int bufferStorageCapacity = 0;
	/**
	 * The (flow) capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	private double bufferCap = 0.0;

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	private double buffercap_accumulate = 1.0;

	/* WAITING QUEUE = DRIVEWAYS */

	private final Queue<SimVehicle> waitingList = new LinkedList<SimVehicle>();

	/* PARKING */

	private final Map<Id, SimVehicle> parkedVehicles = new LinkedHashMap<Id, SimVehicle>(10);

	/* TRAFFIC FLOW CHARACTERISTICS */

	private double simulatedFlowCapacity = 0.0;
	private double flowCapFraction = 0.0;
	private double freespeedTravelTime = 0.0;
	private double usedStorageCapacity = 0.0;

	/* OTHER MEMBERS */

	private static int spaceCapWarningCount = 0;

	public QueueLink(final Link link, final QueueNetwork network) {
		this.link = link;
		this.network = network;
		recalculateAttributes();
	}

	private void recalculateAttributes() {
		double now = this.network.simEngine.getCurrentTime();
		double length = this.link.getLength();

		this.freespeedTravelTime = length / this.link.getFreespeed(now);

		this.simulatedFlowCapacity = ((LinkImpl)this.link).getFlowCapacity(now);
		this.simulatedFlowCapacity = this.simulatedFlowCapacity * this.network.simEngine.getTimestepSize() * this.network.getFlowCapFactor();
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);

		double numberOfLanes = this.link.getNumberOfLanes(now);
		// first guess at storageCapacity:
		this.storageCapacity = (length * numberOfLanes) / this.network.getEffectiveCellSize() * this.network.getStorageCapFactor();

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = this.freespeedTravelTime * this.simulatedFlowCapacity;
		if (this.storageCapacity < tempStorageCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.link.getId() + " too small: enlarge storage capcity from: " + this.storageCapacity + " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			this.storageCapacity = tempStorageCapacity;
		}

	}

	/*package*/ void addVehicle(final SimVehicle vehicle) {
		insertVehicle(vehicle, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_IMMEDIATELY);
	}

	@Override
	public void insertVehicle(final SimVehicle vehicle, final double position, final double priority) {
		double now = this.network.simEngine.getCurrentTime();
		if (priority == SimLink.PRIORITY_PARKING) {
			this.parkedVehicles.put(vehicle.getId(), vehicle);
			return;
		}
		if (position == SimLink.POSITION_AT_FROM_NODE) {
			// vehicle enters from intersection
			this.vehQueue.add(vehicle);
			double earliestLeaveTime = now + this.freespeedTravelTime;
			this.earliestLeaveTimes.put(vehicle, earliestLeaveTime);
			this.usedStorageCapacity += vehicle.getSizeInEquivalents();
			this.network.simEngine.getEventsManager().processEvent(
					new LinkEnterEventImpl(now, vehicle.getId(), this.link.getId()));
		} else {
			if (priority == SimLink.PRIORITY_IMMEDIATELY) {
				// vehicle enters from a driveway
				this.vehQueue.addFirst(vehicle);
			} else {
				// vehicle enters from a driveway
				this.waitingList.add(vehicle);
			}
		}
	}

	@Override
	public void removeVehicle(SimVehicle vehicle) {
		if (this.parkedVehicles.remove(vehicle.getId()) != null) {
			return;
		}
		if (this.waitingList.remove(vehicle)) {
			return;
		}
		if (this.vehQueue.remove(vehicle)) {
			return;
		}
		this.buffer.remove(vehicle);
	}

	@Override
	public void continueVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public SimVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	public void parkVehicle(SimVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getId(), vehicle);
	}

	@Override
	public void doSimStep(final double time) {
		updateBufferCapacity();
		moveLinkToBuffer(time);
		moveWaitToBuffer(time);
	}

	private void updateBufferCapacity() {
		this.bufferCap = this.simulatedFlowCapacity;
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.flowCapFraction;
		}
	}

	private void moveLinkToBuffer(final double time) {
		SimVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (this.earliestLeaveTimes.get(veh).doubleValue() > time) {
				return;
			}
			if (!hasBufferSpace()) {
				return;
			}
			addToBuffer(this.vehQueue.poll(), time);
			this.usedStorageCapacity -= veh.getSizeInEquivalents();
		} // end while
	}

	private void moveWaitToBuffer(final double time) {
		while (hasBufferSpace()) {
			SimVehicle vehicle = this.waitingList.poll();
			if (vehicle == null) {
				return;
			}

			this.network.simEngine.getEventsManager().processEvent(
					new AgentWait2LinkEventImpl(time, vehicle.getId(), this.link.getId(), TransportMode.car));
			addToBuffer(vehicle, time);
		}
	}

	private boolean hasBufferSpace() {
		return ((this.buffer.size() < this.bufferStorageCapacity) && ((this.bufferCap >= 1.0)
				|| (this.buffercap_accumulate >= 1.0)));
	}

	private void addToBuffer(final SimVehicle veh, final double now) {
		if (this.bufferCap >= 1.0) {
			this.bufferCap--;
		} else if (this.buffercap_accumulate >= 1.0) {
			this.buffercap_accumulate--;
		} else {
			throw new IllegalStateException("Buffer of link " + this.link.getId() + " has no space left!");
		}
		this.buffer.add(veh);
	}

	@Override
	public Id getId() {
		return this.link.getId();
	}

	/*package*/ SimVehicle getFirstVehicleInBuffer() {
		return this.buffer.peek();
	}

	/*package*/ SimVehicle removeFirstVehicleInBuffer() {
		return this.buffer.poll();
	}

}
