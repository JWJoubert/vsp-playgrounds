/* *********************************************************************** *
 * project: org.matsim.*
 * OevTest.java
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

package playground.kai.oevTest;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

/**
 * @author nagel
 *
 */
public class OevTest {

	public static void main(final String[] args) {

		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./test/shared/oev-test-network/configs/config1.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final Controler controler = new Controler(Gbl.getConfig());
		controler.setOverwriteFiles(true) ;
		controler.run();

	}

}
