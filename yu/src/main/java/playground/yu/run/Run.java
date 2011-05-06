/* *********************************************************************** *
 * project: org.matsim.*
 * Run2.java
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

package playground.yu.run;

import org.matsim.core.controler.Controler;

public class Run {

	/**
	 * @param args
	 *            args[0] - configfile;
	 *            <P>
	 *            args[1] - writeGraphs (boolean)
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(args[0]);
		controler.setCreateGraphs(Boolean.parseBoolean(args[1]));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
