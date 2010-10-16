/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEngineFactory.java
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

import java.util.Random;

/**
 * @author laemmel
 * 
 */
public class DefaultSim2DEngineFactory {

	public Sim2DEngine createSim2DEngine(final Sim2D sim, final Random random) {
		return new Sim2DEngine(sim, random);

	}
}
