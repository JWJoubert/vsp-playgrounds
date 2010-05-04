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

package playground.wrashid.PHEV.Triangle;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class PrintLinkCoordinates {

	public static void main(String[] args) throws Exception {
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);
		sl.loadNetwork();

		System.out.println("linkId\tx\ty");
		for (Link link : sl.getScenario().getNetwork().getLinks().values()){
			System.out.println(link.getId().toString() +"\t"+ getXCoordinate(link) +"\t"+  getYCoordinate(link));
		}

	}

	public static double getXCoordinate(Link link){
		return (link.getFromNode().getCoord().getX()+ link.getToNode().getCoord().getX())/2;
	}

	public static double getYCoordinate(Link link){
		return (link.getFromNode().getCoord().getY()+ link.getToNode().getCoord().getY())/2;
	}

}
