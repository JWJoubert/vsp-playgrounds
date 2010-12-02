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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

/**
 * @author laemmel
 * 
 */
public class DrivingForceModule implements ForceModule {

	private final Scenario2DImpl scenario;
	private final Floor floor;
	private HashMap<Id, Coordinate> drivingDirections;

	/**
	 * @param floor
	 * @param scenario
	 */
	public DrivingForceModule(Floor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.scenario = scenario;
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
	public void run(Agent2D agent) {
		Coordinate d = this.drivingDirections.get(agent.getCurrentLinkId());
		double driveX = d.x;
		double driveY = d.y;

		driveX *= (Sim2DConfig.TIME_STEP_SIZE * agent.getDesiredVelocity() - agent.getForce().getOldXComponent()) / Sim2DConfig.tau;
		driveY *= (Sim2DConfig.TIME_STEP_SIZE * agent.getDesiredVelocity() - agent.getForce().getOldYComponent()) / Sim2DConfig.tau;

		// DEBUG
		ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), new Coordinate(0, 0, 0), new Coordinate(-50 * driveX, -50 * driveY, 0), 0.f, 1.f, 0.f, 1);
		this.floor.getSim2D().getEventsManager().processEvent(arrow);

		agent.getForce().incrementX(driveX);
		agent.getForce().incrementY(driveY);

	}

}
