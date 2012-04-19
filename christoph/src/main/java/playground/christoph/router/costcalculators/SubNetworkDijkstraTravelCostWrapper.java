/* *********************************************************************** *
 * project: org.matsim.*
 * SubNetworkDijkstra.java
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

package playground.christoph.router.costcalculators;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.christoph.router.SubNetworkDijkstra;

/*
 * This is only a wrapper class that hands the given person
 * over to a SubNetworkDijkstra LeastCostPathCalculator.
 * 
 * The calculation of the travel costs is done by a given
 * TravelCost implementation.
 * 
 * We don't clone or copy the SubNetworkDijkstra - a new one 
 * has to be created elsewhere using a SubNetworkDijkstraFactory.
 */
public class SubNetworkDijkstraTravelCostWrapper implements TravelDisutility, Cloneable {

	private TravelDisutility travelCost;
	private SubNetworkDijkstra subNetworkDijkstra = null;
	
	private static final Logger log = Logger.getLogger(SubNetworkDijkstraTravelCostWrapper.class);
	
	public SubNetworkDijkstraTravelCostWrapper(TravelDisutility travelCost)
	{
		this.travelCost = travelCost;
	}
	
	public void setSubNetworkDijkstra(SubNetworkDijkstra subNetworkDijkstra)
	{
		this.subNetworkDijkstra = subNetworkDijkstra;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle)
	{
		if (subNetworkDijkstra != null) subNetworkDijkstra.setPerson(person);
		return travelCost.getLinkTravelDisutility(link, time, person, vehicle);
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return travelCost.getLinkMinimumTravelDisutility(link);
	}
	
	@Override
	public SubNetworkDijkstraTravelCostWrapper clone()
	{
		TravelDisutility travelCostClone = null;
		if (travelCost instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelCost.getClass().getMethod("clone", new Class[]{});
				travelCostClone = travelCost.getClass().cast(method.invoke(travelCost, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelCostClone == null)
		{
			travelCostClone = travelCost;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		SubNetworkDijkstraTravelCostWrapper clone = new SubNetworkDijkstraTravelCostWrapper(travelCostClone);

		return clone;
	}
}