/* *********************************************************************** *
 * project: org.matsim.*
 * BaseCaseControler.java
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
package playground.benjamin;

import org.matsim.core.controler.Controler;


/**
 * @author bkick
 *
 */
public class BaseCaseControler {


	public static void main(String[] args) {
		String config = "../../shared-svn/studies/bkick/routerTest/configRouterTest.xml";
		Controler controler = new Controler(config);
		controler.run();
	}

}
