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

package playground.mrieser.core.sim.network.api;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.sim.api.SimVehicle;

public interface SimLink {

	public static final double POSITION_AT_FROM_NODE = 0.0;
	public static final double POSITION_AT_TO_NODE = 1.0;

	public static final double PRIORITY_AS_SOON_AS_SPACE_AVAILABLE = 0.0;
	public static final double PRIORITY_IMMEDIATELY = 1.0;
	public static final double PRIORITY_PARKING = -1.0;

	public Id getId();

	/**
	 * Inserts a vehicle into the traffic flow at a specified position with a
	 * specified priority. The position is specified as fraction from start-node
	 * (0.0) to end-node (1.0) and is only used as a hint, where to place the
	 * vehicle. The actual implementation may insert the vehicle at another point
	 * than specified. The priority describes if the vehicle is added once there
	 * is place available (0.0) or forcefully inserted in any case (1.0). Values
	 * between 0.0 and 1.0 may be handled differently by different
	 * implementations. If a vehicle cannot immediately be inserted into the
	 * traffic flow, the request is cached and performed once space is available.
	 * The special case {@link SimLink#PRIORITY_PARKING} is used to initially
	 * place the vehicles on the network.
	 *
	 * @param vehicle
	 * @param position value between 0.0 ({@link SimLink#POSITION_AT_FROM_NODE}) and 1.0 ({@link SimLink#POSITION_AT_TO_NODE})
	 * @param priority value between 0.0 ({@link SimLink#PRIORITY_AS_SOON_AS_SPACE_AVAILABLE}) and 1.0 ({@link SimLink#PRIORITY_IMMEDIATELY}), or {@link SimLink#PRIORITY_PARKING}.
	 */
	public void insertVehicle(final SimVehicle vehicle, final double position, final double priority);

	/**
	 * Removes a vehicle from the traffic flow or from parking.
	 *
	 * @param vehicle
	 * @return
	 */
	public void removeVehicle(final SimVehicle vehicle);

	/**
	 * Stops the vehicle on the link wherever it currently is, probably blocking
	 * other traffic.
	 *
	 * @param vehicle
	 * @see SimLink#continueVehicle(SimVehicle)
	 */
	public void stopVehicle(final SimVehicle vehicle);

	/**
	 * Lets a vehicle continue again after it was stopped or parked. If a
	 * vehicle was not stopped or parked before, the method does nothing.
	 *
	 * @param vehicle
	 * @see SimLink#stopVehicle(SimVehicle)
	 * @see SimLink#parkVehicle(SimVehicle)
	 */
	public void continueVehicle(final SimVehicle vehicle);

	/**
	 * Removes a vehicle from the traffic flow, but keeps the vehicle
	 * registered on that link.
	 *
	 * @param vehicle
	 * @see SimLink#getParkedVehicle(Id)
	 * @see SimLink#continueVehicle(SimVehicle)
	 */
	public void parkVehicle(final SimVehicle vehicle);

	/**
	 * @param vehicleId
	 * @return <code>null</code> if no vehicle with the given id is parked along this link
	 */
	public SimVehicle getParkedVehicle(final Id vehicleId);

}
