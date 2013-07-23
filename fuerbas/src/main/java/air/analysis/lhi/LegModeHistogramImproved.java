/* *********************************************************************** *
 * project: org.matsim.*
 * DgLegHistogramImproved
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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;

import air.analysis.categoryhistogram.CategoryHistogram;

/**
 * Improved version of LegHistogram - no maximal time bin size.
 * 
 * @author dgrether
 * 
 */
public class LegModeHistogramImproved implements
		AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	public static final String all = "all";
	
	private CategoryHistogram histogram;

	private Map<Id, AgentDepartureEvent> departureEventsByPersonId;
	
	public LegModeHistogramImproved() {
		this(5 * 60);
	}

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 * 
	 * @param binSize
	 *          The size of a time bin in seconds.
	 * @param nofBins
	 *          The number of time bins for this analysis.
	 */
	public LegModeHistogramImproved(final int binSize) {
		this.histogram = new CategoryHistogram(binSize);
		this.departureEventsByPersonId = new HashMap<Id, AgentDepartureEvent>();
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.histogram.reset(iteration);
		this.departureEventsByPersonId.clear();
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.departureEventsByPersonId.put(event.getPersonId(), event);
	}
	
	private void processDeparture(double time, String mode){
		this.histogram.increase(time, 1, all);
		this.histogram.increase(time, 1, mode);
	}
	
	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		AgentDepartureEvent e = this.departureEventsByPersonId.get(event.getPersonId());
		if (e != null) {
			this.processDeparture(e.getTime(), e.getLegMode());
			this.histogram.decrease(event.getTime(), 1, all);
			this.histogram.decrease(event.getTime(), 1, event.getLegMode());
		}
	}

	@Override
	public void handleEvent(final AgentStuckEvent event) {
		AgentDepartureEvent e = this.departureEventsByPersonId.get(event.getPersonId());
		if (e != null) {
			this.processDeparture(e.getTime(), e.getLegMode());
			this.histogram.abort(event.getTime(), 1, all);
			this.histogram.abort(event.getTime(), 1, event.getLegMode());
		}
	}

	public CategoryHistogram getCategoryHistogram() {
		return this.histogram;
	}

}
