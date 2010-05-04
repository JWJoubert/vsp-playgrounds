/* *********************************************************************** *
 * project: org.matsim.*
 * GTFNetCreator.java
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

/**
 *
 */
package playground.yu.newNetwork;

import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;

import playground.yu.utils.io.GTFParser;

/**
 * @author yu
 *
 */
public class GTFNetCreator {

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-changed.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		GTFParser g = new GTFParser(
				new TreeMap<String, HashMap<Double, Double>>());
		g.readFile("../schweiz-ivtch/greentimes/ivtch.xml");

		for (String linkId : g.getLinkgtfsMap().keySet()) {
			Link l = network.getLinks().get(new IdImpl(linkId));
			if (l != null) {
				System.out
						.println("#a-->linkId:\t"
								+ linkId
								+ "\tcapacity:\t"
								+ l.getCapacity());
				System.out.println("greentimefraction:\t"
						+ g.getAvgGtfs(linkId));
				l
						.setCapacity(l.getCapacity() * g.getAvgGtfs(linkId));
				System.out
						.println("#b-->linkId:\t"
								+ linkId
								+ "\tcapacity:\t"
								+ network.getLinks().get(new IdImpl(linkId)).getCapacity());
				System.out.println("-------------------------");
			}
		}

		new NetworkWriter(network).write("../schweiz-ivtch/network/ivtch-changed-with-GTF.xml");

		System.out.println("done!");
	}

}
