/* *********************************************************************** *
 * project: org.matsim.*
 * ResizeCounts.java
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
package playground.benjamin.szenarios.munich;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.droeder.osm.AbstractResizeLinksByCount;

/**
 * @author benjamin
 *
 */
public class RunResizeCounts extends AbstractResizeLinksByCount{

	/**
	 * @param networkFile
	 * @param counts
	 * @param scaleFactor
	 */
	public RunResizeCounts(String networkFile, Counts counts, Double scaleFactor) {
		super(networkFile, counts, scaleFactor);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String countsFile = "../../detailedEval/counts/counts-2008-01-10_correctedSums_manuallyChanged.xml";
		String networkFile = "../../detailedEval/Net/network-86-85-87-84.xml";
		String outputFile = "../../detailedEval/Net/network-86-85-87-84_resizedByCounts.xml";

		Counts counts = new Counts();
		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
		try {
			countsReader.parse(countsFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		RunResizeCounts rrc = new RunResizeCounts(networkFile, counts, 1.1);
		rrc.run(outputFile);
	}

	/* (non-Javadoc)
	 * @see playground.droeder.osm.AbstractResizeLinksByCount#resize()
	 */
	@Override
	protected void resize() {
		TreeMap<Id, Count> counts = this.getOriginalCounts().getCounts();
		Map<Id, ? extends Link> links = this.getOrigNetwork().getLinks();
		for(Count count : counts.values()){
			Id locId = count.getLocId();
			Id linkId = links.get(locId).getId();
			double maxCount = count.getMaxVolume().getValue();
			
			System.out.println("setting capacity of link " + linkId + " from " + links.get(linkId).getCapacity() + " to " + 16 * maxCount);
			this.setNewLinkData(linkId, 16 * maxCount, 1.0);
		}
	}

}
