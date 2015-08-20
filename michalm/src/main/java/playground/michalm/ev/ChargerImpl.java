/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.ev;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;


public class ChargerImpl
    implements Charger
{
    private final Id<Charger> id;
    private final double power;
    private final int capacity;
    private final Link link;

    private ChargingLogic logic;


    public ChargerImpl(Id<Charger> id, double power, int capacity, Link link)
    {
        this.id = id;
        this.power = power;
        this.capacity = capacity;
        this.link = link;
    }


    @Override
    public ChargingLogic getLogic()
    {
        return logic;
    }


    public void setLogic(ChargingLogic logic)
    {
        this.logic = logic;
    }


    @Override
    public Id<Charger> getId()
    {
        return id;
    }


    @Override
    public double getPower()
    {
        return power;
    }


    @Override
    public int getCapacity()
    {
        return capacity;
    }


    @Override
    public Link getLink()
    {
        return link;
    }


    @Override
    public Coord getCoord()
    {
        return link.getCoord();
    }
}
