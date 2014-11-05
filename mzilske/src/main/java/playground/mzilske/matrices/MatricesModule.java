/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MatricesModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.matrices;

import org.matsim.core.controler.AbstractModule;
import playground.mzilske.cdr.LinkIsZone;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.SightingsImpl;
import playground.mzilske.cdr.ZoneTracker;

public class MatricesModule extends AbstractModule {
    @Override
    public void install() {
        bindAsSingleton(Sightings.class, SightingsImpl.class);
        bindAsSingleton(ZoneTracker.LinkToZoneResolver.class, LinkIsZone.class);
        addControlerListenerByProvider(MatrixDemandControlerListener.class);
        addControlerListener(MatricesUpdater.class);
    }
}
