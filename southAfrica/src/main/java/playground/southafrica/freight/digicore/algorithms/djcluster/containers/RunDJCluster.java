/* *********************************************************************** *
 * project: org.matsim.*
 * RunDJCluster.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.freight.digicore.algorithms.djcluster.containers;

import playground.southafrica.freight.digicore.algorithms.djcluster.DJCluster;
import playground.southafrica.utilities.Header;

/**
 * Runs a single {@link DJCluster} instance.
 * @author jwjoubert
 */
public class RunDJCluster {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunDJCluster.class.toString(), args);
		
		
		Header.printFooter();
	}

}
