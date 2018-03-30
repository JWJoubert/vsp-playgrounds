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
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

import playground.michalm.taxi.ev.ETaxiChargingLogic;

public class ETaxiChargingTask extends StayTaskImpl implements TaxiTask {
	private final ETaxiChargingLogic chargingLogic;
	private final ElectricVehicle ev;
	private double chargingStartedTime;

	public ETaxiChargingTask(double beginTime, double endTime, Charger charger, ElectricVehicle ev) {
		super(beginTime, endTime, charger.getLink());
		this.chargingLogic = (ETaxiChargingLogic)charger.getLogic();
		this.ev = ev;
	}

	public ETaxiChargingLogic getChargingLogic() {
		return chargingLogic;
	}

	public ElectricVehicle getElectricVehicle() {
		return ev;
	}

	public void setChargingStartedTime(double chargingStartedTime) {
		this.chargingStartedTime = chargingStartedTime;
	}

	public double getChargingStartedTime() {
		return chargingStartedTime;
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.STAY;
	}

	@Override
	protected String commonToString() {
		return "[CHARGING]" + super.commonToString();
	}
}
