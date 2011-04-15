/* *********************************************************************** *
 * project: org.matsim.*
 * Agent2D.java
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
package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v2.simulation.floor.Force;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Agent2D extends PersonDriverAgentImpl {

	private Coordinate currentPosition;
	private final Force force = new Force();
	private final double desiredVelocity;
	private Coordinate currentVelocity;
	private double vx;
	private double vy;

	public static final double AGENT_WEIGHT = 80;// * 1000;
	public static final double AGENT_DIAMETER = 0.7;

	/**
	 * @param p
	 * @param sim2d
	 */
	public Agent2D(Person p, Netsim sim2d) {
		super(p, sim2d);

		// TODO think about this
		this.desiredVelocity = 1 + (MatsimRandom.getRandom().nextDouble() - 0.5) / 4;

	}

	/**
	 * @return
	 */
	public Coordinate getPosition() {
		return this.currentPosition;
	}

	public void setPostion(Coordinate pos) {
		this.currentPosition = pos;
	}

	public Force getForce() {
		return this.force;
	}

	/**
	 * @param newPos
	 */
	public void moveToPostion(Coordinate newPos) {
		// TODO check for choose next link and so on ...
		this.currentPosition.setCoordinate(newPos);
	}

	/**
	 * @return
	 */
	public double getDesiredVelocity() {
		return this.desiredVelocity;
	}

	public void setCurrentVelocity(double vx, double vy) {
		this.vx = vx;
		this.vy = vy;

	}

	public double getVx() {
		return this.vx;
	}

	public double getVy() {
		return this.vy;
	}

	public double getWeight() {
		return AGENT_WEIGHT;
	}

	// /**
	// * @return
	// */
	// public double getWeight() {
	// return this.agentWeight;
	// }

}
