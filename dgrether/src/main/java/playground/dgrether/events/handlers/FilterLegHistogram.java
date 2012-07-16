/* *********************************************************************** *
 * project: org.matsim.*
 * FilterLegHistogram
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
package playground.dgrether.events.handlers;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class FilterLegHistogram implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentStuckEventHandler {

	private int iteration = 0;
	private final int binSize;
	private final int nofBins;
	private ModeData allModesData = null;
	

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public FilterLegHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.nofBins = nofBins;
		reset(0);
	}

	/** Creates a new LegHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public FilterLegHistogram(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}

	/* Implementation of EventHandler-Interfaces */

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsDep[index]++;
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsArr[index]++;
	}

	@Override
	public void handleEvent(final AgentStuckEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsStuck[index]++;
	}

	@Override
	public void reset(final int iter) {
		this.iteration = iter;
		this.allModesData = new ModeData(this.nofBins + 1);
	}

	/* output methods */

	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		stream.print("time\ttime\tdepartures_all\tarrivals_all\tstuck_all\ten-route_all");
		stream.print("\n");
		int allEnRoute = 0;
		for (int i = 0; i < this.allModesData.countsDep.length; i++) {
			// data about all modes
			allEnRoute = allEnRoute + this.allModesData.countsDep[i] - this.allModesData.countsArr[i] - this.allModesData.countsStuck[i];
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			stream.print("\t" + this.allModesData.countsDep[i] + "\t" + this.allModesData.countsArr[i] + "\t" + this.allModesData.countsStuck[i] + "\t" + allEnRoute);
			// new line
			stream.print("\n");
		}
	}

	/**
	 * @return a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips
	 */
	public JFreeChart getGraphic() {
		return getGraphic(this.allModesData, "all");
	}


	private JFreeChart getGraphic(final ModeData modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("en route", false, true);
		int onRoute = 0;
		for (int i = 0; i < modeData.countsDep.length; i++) {
			onRoute = onRoute + modeData.countsDep[i] - modeData.countsArr[i] - modeData.countsStuck[i];
			double hour = i*this.binSize / 60.0 / 60.0;
			departuresSerie.add(hour, modeData.countsDep[i]);
			arrivalsSerie.add(hour, modeData.countsArr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "Leg Histogram, " + modeName + ", it." + this.iteration,
        "time", "# vehicles",
        xyData,
        PlotOrientation.VERTICAL,
        true,   // legend
        false,   // tooltips
        false   // urls
    );

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}

	/**
	 * @return number of departures per time-bin, for all legs
	 */
	public int[] getDepartures() {
		return this.allModesData.countsDep.clone();
	}

	/**
	 * @return number of all arrivals per time-bin, for all legs
	 */
	public int[] getArrivals() {
		return this.allModesData.countsArr.clone();
	}

	/**
	 * @return number of all vehicles that got stuck in a time-bin, for all legs
	 */
	public int[] getStuck() {
		return this.allModesData.countsStuck.clone();
	}


	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips to the specified file.
	 *
	 * @param filename
	 *
	 * @see #getGraphic()
	 */
	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}


	private static class ModeData {
		public final int[] countsDep;
		public final int[] countsArr;
		public final int[] countsStuck;

		public ModeData(final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
			this.countsStuck = new int[nofBins];
		}
	}
}
