/* *********************************************************************** *
 * project: org.matsim.*
 * ActTimingsMZMATSim.java
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

package playground.mfeil.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;



/**
 * Reads an plans file and summarizes average activity durations
 *
 * @author mfeil
 */
public class ActTimingsMZMATSim {
	
	private static final Logger log = Logger.getLogger(ActTimingsMZMATSim.class);

	private PrintStream initiatePrinter(String outputFile){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return stream;
	}
	
	public void printHeader(PrintStream stream){
		stream.println("Activity timings");
		stream.println("\tHome\t\tInnerHome\tWork\tEducations\tLeisure\tShop\tPopSize");
		stream.println("\tStart\tEnd\tDuration\tDuration\tDuration\tDuration\tDuration");	
	}
	
	public void run(PopulationImpl populationMZ, PopulationImpl populationMATSim, PrintStream stream){
				
		this.runPopulation("MZ", populationMZ, stream);
		this.runPopulation("MATSim", populationMATSim, stream);
	}
		
	public void runPopulation (String name, PopulationImpl population, PrintStream stream){
		
		// Initiate output
		double startHome = 0;
		double endHome = 0;
		double durationInnerHome = 0;
		double durationWork = 0;
		double durationEducation = 0;
		double durationLeisure = 0;
		double durationShop = 0;		
		
		int counterHome = 0;
		int counterInnerHome = 0;
		int counterWork = 0;
		int counterEducation = 0;
		int counterLeisure = 0;
		int counterShop = 0;
		int size = population.getPersons().size();
		
		stream.print(name+"\t");
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (int i=0;i<plan.getPlanElements().size();i+=2){
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
				if (i==0) { // first home act
					if (act.getEndTime()!=Time.UNDEFINED_TIME){
						endHome += act.getEndTime(); 
					}
					else log.warn("The end time of person's "+person.getId()+" fist home act is undefined!");
					counterHome++;
				}
				else if (i==plan.getPlanElements().size()-1) { // last home act
					if (act.getStartTime()!=Time.UNDEFINED_TIME){
						startHome += act.getStartTime(); 
					}
					else log.warn("The start time of person's "+person.getId()+" last home act is undefined!");
					counterHome++;
				}
				else {
					if (act.getType().startsWith("h")) {
						durationInnerHome+=act.calculateDuration();
						counterInnerHome++;
					}
					else if (act.getType().startsWith("w")) {
						durationWork+=act.calculateDuration();
						counterWork++;
					}
					else if (act.getType().startsWith("e")) {
						durationEducation+=act.calculateDuration();
						counterEducation++;
					}
					else if (act.getType().startsWith("l")) {
						durationLeisure+=act.calculateDuration();
						counterLeisure++;
					}
					else if (act.getType().startsWith("s")) {
						durationShop+=act.calculateDuration();
						counterShop++;
					}
					else log.warn("Unknown act type in person's "+person.getId()+" plan at position "+i+"!");
				}
			}
		}
		stream.print(name+"\t");
		stream.print(Time.writeTime(startHome/counterHome)+"\t"+Time.writeTime(endHome/counterHome)+"\t");
		stream.print(Time.writeTime(durationInnerHome/counterInnerHome)+"\t"+Time.writeTime(durationWork/counterWork)+"\t"+Time.writeTime(durationEducation/counterEducation)+"\t"+Time.writeTime(durationLeisure/counterLeisure)+"\t"+Time.writeTime(durationShop/counterShop)+"\t");
		stream.println(size);
	}		
		
	
	public static void main(final String [] args) {
				final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
				final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
				final String populationFilenameMATSim = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mz05.xml";
				final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
				final String outputFile = "/home/baug/mfeil/data/choiceSet/trip_stats_mz05.xls";
	
				ScenarioImpl scenarioMZ = new ScenarioImpl();
				new MatsimNetworkReader(scenarioMZ).readFile(networkFilename);
				new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
				
				ScenarioImpl scenarioMATSim = new ScenarioImpl();
				scenarioMATSim.setNetwork(scenarioMZ.getNetwork());
				new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
								
				ActTimingsMZMATSim ts = new ActTimingsMZMATSim();
				PrintStream stream = ts.initiatePrinter(outputFile);
				ts.printHeader(stream);
				ts.run(scenarioMZ.getPopulation(), scenarioMATSim.getPopulation(), stream);
				log.info("Process finished.");
			}
}

