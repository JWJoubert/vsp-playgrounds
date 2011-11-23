/* *********************************************************************** *
 * project: org.matsim.*
 * DrivingForceModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * @author laemmel
 * 
 */
public class DrivingForceModule implements ForceModule {

	private final PhysicalFloor floor;
	private HashMap<Id, Coordinate> drivingDirections;

	// inertia -- needs to be revised
	private final double tau;

	/**
	 * @param floor
	 * @param scenario
	 */
	public DrivingForceModule(PhysicalFloor floor, Scenario scenario) {
		this.floor = floor;
		this.tau = 0.5; //1/1.52;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {

		this.drivingDirections = new HashMap<Id, Coordinate>();
		for (Link link : this.floor.getLinks()) {
			Coordinate c = new Coordinate(link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY());
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			this.drivingDirections.put(link.getId(), c);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent, double time) {
		Coordinate d = this.drivingDirections.get(agent.getMentalLink());
		double driveX = d.x  * agent.getDesiredVelocity();
		double driveY = d.y * agent.getDesiredVelocity();


		double dx = Agent2D.AGENT_WEIGHT *(driveX - agent.getForce().getVx())/this.tau;
		double dy = Agent2D.AGENT_WEIGHT *(driveY - agent.getForce().getVy())/this.tau;


		agent.getForce().incrementX(dx);
		agent.getForce().incrementY(dy);

	}

}
