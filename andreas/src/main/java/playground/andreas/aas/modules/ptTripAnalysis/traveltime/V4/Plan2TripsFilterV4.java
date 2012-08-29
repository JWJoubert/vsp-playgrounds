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
package playground.andreas.aas.modules.ptTripAnalysis.traveltime.V4;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.PlanElement;

import playground.andreas.aas.modules.ptTripAnalysis.AbstractPlan2TripsFilter;
import playground.andreas.aas.modules.ptTripAnalysis.traveltime.AbstractTTAnalysisTrip;

/**
 * @author droeder
 *
 */
public class Plan2TripsFilterV4 extends AbstractPlan2TripsFilter{

	@Override
	protected AbstractTTAnalysisTrip generateTrip(ArrayList<PlanElement> elements) {
		AbstractTTAnalysisTrip trip = new TTAnalysisTripV4();
		trip.addElements(elements);
		return trip;
	}
	

}
