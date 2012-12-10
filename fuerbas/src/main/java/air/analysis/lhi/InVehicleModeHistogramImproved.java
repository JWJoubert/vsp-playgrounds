/* *********************************************************************** *
 * project: org.matsim.*
 * InVehicleModeHistogramImproved
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.analysis.lhi;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

import air.analysis.categoryhistogram.CategoryHistogram;


/**
 * @author dgrether
 *
 */
public class InVehicleModeHistogramImproved implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private CategoryHistogram histogram;

	private Map<Id, PersonEntersVehicleEvent> enterVehEventByPersId;
	
	private int binSize = 5 * 60;
	
	
	@Override
	public void reset(int iteration) {
		this.histogram = new CategoryHistogram(binSize);
		this.enterVehEventByPersId = new HashMap<Id, PersonEntersVehicleEvent>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (! event.getPersonId().toString().startsWith("pt_")){
			this.enterVehEventByPersId.put(event.getPersonId(), event);
		}
	}

	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (! event.getPersonId().toString().startsWith("pt_")){
			PersonEntersVehicleEvent enterEvent = this.enterVehEventByPersId.remove(event.getPersonId());
			if (enterEvent != null){
				this.histogram.increase(enterEvent.getTime(), 1, "pt");
				this.histogram.decrease(event.getTime(), 1, "pt");
			}
		}
	}


	public CategoryHistogram getCategoryHistogram() {
		return this.histogram;
	}

}
