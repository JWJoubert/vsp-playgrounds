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

package playground.michalm.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import playground.michalm.taxi.data.TaxiRequest;


public class TaxiDropoffStayTask
    extends StayTaskImpl
    implements TaxiTaskWithRequest
{
    private final TaxiRequest request;


    public TaxiDropoffStayTask(double beginTime, double endTime, TaxiRequest request)
    {
        super(beginTime, endTime, request.getToLink());

        this.request = request;
        request.setDropoffStayTask(this);
    }


    @Override
    public void removeFromRequest()
    {
        request.setDropoffStayTask(null);
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DROPOFF_STAY;
    }


    public TaxiRequest getRequest()
    {
        return request;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
