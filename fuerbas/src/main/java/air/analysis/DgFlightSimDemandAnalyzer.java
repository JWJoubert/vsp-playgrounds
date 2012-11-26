/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightSimDemandAnalyzer
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
package air.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import air.demand.DgDemandReader;
import air.demand.DgDemandUtils;
import air.demand.DgDemandWriter;
import air.demand.DgDestatisCompare;
import air.demand.FlightODRelation;


/**
 * @author dgrether
 *
 */
public class DgFlightSimDemandAnalyzer {

	public static void main(String[] args) throws IOException {
//	String populationFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/population_september_2011_tabelle_2.2.2.xml.gz";
	String odDemand21 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.1.csv";
	String odDemand22 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
//	String outputDiffFile2 = "/home/dgrether/data/work/repos/runs-svn/run1835/ITERS/it.500/1835.500.destatis_2.2.2-simulated_direct_flights.csv";
//	String outputDiffFileAbs2 = "/home/dgrether/data/work/repos/runs-svn/run1835/ITERS/it.500/1835.500.destatis_2.2.2-simulated_direct_flights_abs.csv";
	String outputRelativeErrorFile = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_relative_error_2.2.2-2.2.1.csv";

	List<String> results = new ArrayList<String>();
	
	List<FlightODRelation> demand21List = new DgDemandReader().readFile(odDemand21);
	List<FlightODRelation> demand22List = new DgDemandReader().readFile(odDemand22);
	DgDemandUtils.convertToDailyDemand(demand21List);
	DgDemandUtils.convertToDailyDemand(demand22List);
	SortedMap<String, SortedMap<String, FlightODRelation>> destatisDemand21 = DgDemandUtils.createFromAirportCodeToAirportCodeMap(demand21List);
	SortedMap<String, SortedMap<String, FlightODRelation>> destatisDemand22 = DgDemandUtils.createFromAirportCodeToAirportCodeMap(demand22List);
	DgDemandWriter writer = new DgDemandWriter();

	int totalDirectFlights21 = 0;
	for (FlightODRelation od : demand21List){
		if (od.getNumberOfTrips() != null)
			totalDirectFlights21 += od.getNumberOfTrips();
	}
	List<FlightODRelation> diff = new DgDestatisCompare().createDifferenceList(demand21List, demand22List);
	SortedMap<String, SortedMap<String, FlightODRelation>> diffMap = DgDemandUtils.createFromAirportCodeToAirportCodeMap(diff);
	double variance = DgDemandUtils.calcVariance(diffMap);
	
	SortedMap<String, SortedMap<String, FlightODRelation>> relativeErrorMap = DgDemandUtils.createRelativeErrorMap(destatisDemand22, destatisDemand21);
	writer.writeFlightODRelations(outputRelativeErrorFile, relativeErrorMap);
	double meanRelError = DgDemandUtils.calcMeanRelativeError(relativeErrorMap);
	
	String result = "2.2.2 - 2.2.1" + " &  " + "-" + " & " + getStringFromDouble(variance, -1) + " & " + getStringFromDouble(Math.sqrt(variance), -1) + " & " + getStringFromDouble(meanRelError, 4) + " & " + "-" + " & " + "-" + " \\\\";
	System.out.println(result);
	results.add(result);

	
	String[] runNumbers ={
			"1836", 
			"1837",
			"1838",
			"1839",
			"1840",
			"1841"				
	};
	String iteration = "600";
	
	for (String runNumber : runNumbers) {
		String events = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".events.xml.gz";
		String outputFile = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".simulated_direct_flights.csv";
		String outputDiffFile = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".destatis_2.2.1-simulated_direct_flights.csv";
		String outputDiffFileAbs = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".destatis_2.2.1-simulated_direct_flights_abs.csv";
		outputRelativeErrorFile = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".relative_error_simulated_direct_flights-destatis_2.2.1.csv";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		DgFlightDemandEventHandler handler = new DgFlightDemandEventHandler();
		eventsManager.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(events);
		SortedMap<String, SortedMap<String, FlightODRelation>> simulatedDirectFlights = handler.getDirectFlightODRelations();
		writer.writeFlightODRelations(outputFile, simulatedDirectFlights, handler.getNumberOfDirectFlights(), (int)handler.getStucked());
		
		diffMap = DgDemandUtils.createDifferenceMap(destatisDemand21, simulatedDirectFlights);
		variance = DgDemandUtils.calcVariance(diffMap);
		writer.writeFlightODRelations(outputDiffFile, diffMap,totalDirectFlights21, 0, false);
		writer.writeFlightODRelations(outputDiffFileAbs, diffMap,totalDirectFlights21, 0, true);
		
		relativeErrorMap = DgDemandUtils.createRelativeErrorMap(simulatedDirectFlights, destatisDemand21);
		writer.writeFlightODRelations(outputRelativeErrorFile, relativeErrorMap);
		meanRelError = DgDemandUtils.calcMeanRelativeError(relativeErrorMap);
		double boardingDenied = handler.getBoardingDenied();
		double stucked = handler.getStucked();
		
		result = runNumber + " & " + iteration + " & " + getStringFromDouble(variance, -1) + " & " + getStringFromDouble(Math.sqrt(variance), -1) + " & " + getStringFromDouble(meanRelError, 4)+ " & " + getStringFromDouble(boardingDenied, -1) + " & " + getStringFromDouble(stucked, -1) + " \\\\";
		System.out.println(result);
		results.add(result);
	}
	
	System.out.println("runNumber & iteration & variance  &  standard deviation & rel error  & boarding denied & stucked \\\\");
	for (String s : results){
		System.out.println(s);
	}
}



private static String getStringFromDouble(double d, int decimalPlaces){
	String res = Double.toString(d);
	return res.substring(0, res.lastIndexOf(".") + decimalPlaces + 1);
}
	
}
