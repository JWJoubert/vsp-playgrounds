/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.realTimeNavigation.velocityObstacles;

import java.util.Collection;

import playground.droeder.realTimeNavigation.movingObjects.MovingObject;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */
public abstract class AbstractVelocityObstacle implements VelocityObstacle {
	
	protected MovingObject theOne;
	protected Collection<MovingObject> opponents;
	private Geometry geometry;

	public AbstractVelocityObstacle(MovingObject theOne, Collection<MovingObject> opponents){
		this.theOne = theOne;
		this.opponents = opponents;
	}
	
	protected abstract Geometry calcGeometry();
	
	public Geometry getGeometry(){
		this.geometry = this.calcGeometry();
		return this.geometry;
	}
	

}
