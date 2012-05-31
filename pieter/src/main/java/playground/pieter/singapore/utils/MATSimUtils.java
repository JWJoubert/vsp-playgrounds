/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.singapore.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.core.network.*;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.population.algorithms.PlansFilterByLegMode.FilterType;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

import playground.pieter.singapore.hits.HITSTrip;
import playground.pieter.singapore.utils.plans.PlansFilterNoRoute;

/**
 * Assigns each activity in each plan of each person in the population a link
 * where the activity takes place based on the coordinates given for the activity.
 * This tool is used for mapping a new demand/population to a network for the first time.
 *
 * @author mrieser
 */
/**
 * @author cobus
 *
 */
/**
 * @author cobus
 *
 */
public class MATSimUtils {

	private Config config;
	private Scenario scenario;
	private Population population;
	private String networkFile;
	private String plansFile;
	private DataBaseAdmin dba;
	private NetworkImpl network;
	private String outPlansFile;



	public MATSimUtils(String plans, String network) {
		// TODO Auto-generated constructor stub
		this.plansFile = plans;
		this.networkFile = network;
	}

	public MATSimUtils(String networkFile) {
		// TODO Auto-generated constructor stub
		this.networkFile = networkFile;
	}
	
	public MATSimUtils(String networkFile, DataBaseAdmin dba) {
		// TODO Auto-generated constructor stub
		this.networkFile = networkFile;
		this.dba = dba;
	}
	
	
	/** Starts the assignment of links to activities.
	 *
	 * @param args command-line arguments
	 */
	public void countPlans() {
		
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = scenario.getPopulation();
		
		MatsimRandom.reset(123);
		
		new MatsimNetworkReader(scenario).readFile(this.networkFile);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		new MatsimPopulationReader(scenario).readFile(this.plansFile);
		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation();


		plans.printPlansCount();

		System.out.println("done.");
	}
	
	public void removeNonRoutedPlans() {
		
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = scenario.getPopulation();
		
		MatsimRandom.reset(123);
		
		new MatsimPopulationReader(scenario).readFile(this.plansFile);
		Iterator<Person> persons =  (Iterator<Person>) scenario.getPopulation().getPersons().values().iterator();

		while(persons.hasNext()){
			Person p = persons.next();
			ArrayList<Plan> plans = (ArrayList<Plan>) p.getPlans();
			for(Plan plan:plans){
//				if(plan.)
			}
		}

		System.out.println("done.");
	}
	
	public void run() {

		MatsimRandom.reset(123);
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(this.networkFile);
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		new MatsimPopulationReader(this.scenario).readFile(this.plansFile);		
		final PopulationImpl plans = (PopulationImpl) this.scenario.getPopulation();
		plans.setIsStreaming(true);
		
		final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		

		plans.addAlgorithm(plansWriter);
		plansReader.readFile(this.config.plans().getInputFile());
		plansWriter.startStreaming(this.outPlansFile);
		PlansFilterNoRoute pf = new PlansFilterNoRoute();
		pf.run(plans) ;
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}
	
	private void initNetwork(){
		
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario).readFile(this.networkFile);
		this.network = (NetworkImpl) scenario.getNetwork();
	}
	
	
	/**
	 * @param factor = the factor by which to multiply the network speed
	 * @param outfile = the file to write to
	 */
	public void changeNetworkSpeeds(double factor, String outfile) {
//		multiplies network free speed by factor
		if(network==null) initNetwork();
		Map<Id, Link> links = network.getLinks();
		Iterator<Link> linkIt = links.values().iterator();
		while(linkIt.hasNext()){
			Link currLink = linkIt.next();
			currLink.setFreespeed(currLink.getFreespeed()*factor);
		}
		new NetworkWriter(network).write(outfile);
	}

	/** Reads a csv file with a set of link attributes to search for in the network, then the values those have to change to.
	 * currently needs freespeed,	capacity,	permlanes,	freespeed_new,	capacity_new,	permlanes_new
	 * all rounded to one decimal

	 * @param csvFile
	 * @param outFile
	 * @throws IOException
	 */
	public void changeNetworkSpeedCaps(String csvFile, String outFile) throws IOException {

		if(network==null) initNetwork();
		BufferedReader reader = IOUtils.getBufferedReader(csvFile);
		HashMap<String,String[]> remapString = new HashMap<String,String[]> ();
		reader.readLine(); //just skip the headings, trying to have this procedure behave dynamically requires reflection
		String line = reader.readLine();
		while(line!=null){
			String[] e = line.split(",");
			remapString.put((e[0]+"_"+e[1]+"_"+e[2]), new String[]{e[3],e[4],e[5]});
			line = reader.readLine();
		}
		Map<Id, Link> links = network.getLinks();
		Iterator<Link> linkIt = links.values().iterator();
		while(linkIt.hasNext()){
			Link currLink = linkIt.next();
			if(currLink.getId().toString().equals("SW7_SW8")){
				System.out.println();
			}
			String type = Double.toString(roundOneDecimal(currLink
					.getFreespeed()))
					+ "_"
					+ Double.toString(roundOneDecimal(currLink.getCapacity()))
					+ "_"
					+ Double.toString(roundOneDecimal(currLink
							.getNumberOfLanes()));
			String[] newValues = remapString.get(type);
			if (newValues != null) {
				currLink.setFreespeed(Double.parseDouble(newValues[0]));
				currLink.setCapacity(Double.parseDouble(newValues[1]));
				currLink.setNumberOfLanes(Double.parseDouble(newValues[2]));
			}
			
		}
		new NetworkWriter(network).write(outFile);
	}
	
	double roundOneDecimal(double d) {
		DecimalFormat oneDForm = new DecimalFormat("#.#");
		return Double.valueOf(oneDForm.format(d));
	}
	public void writeLinkInfoToSQL(DataBaseAdmin dba) throws SQLException, NoConnectionException{
		if(network==null) initNetwork();
		Map<Id, Link> links = network.getLinks();
		Iterator<Link> linkIt = links.values().iterator();
		dba.executeStatement("drop table if exists linkmap;");
		dba.executeStatement("create table linkmap(linkid varchar(45), modes varchar(45), freespeed double, capacity double, lanes double, length double);");
		while(linkIt.hasNext()){
			Link currLink = linkIt.next();
			String id = currLink.getId().toString();
			String modes = currLink.getAllowedModes().toString();
			double length = currLink.getLength();
			double fs = currLink.getFreespeed();
			double cap = currLink.getCapacity();
			double lanes = currLink.getNumberOfLanes();
			String updateStatement = String.format("insert into linkmap values (\'%s\',\'%s\',%f,%f,%f, %f);",id,modes,fs,cap,lanes, length);
			dba.executeUpdate(updateStatement);
		}
	}

	public void writeLinkInfoToSQL() throws SQLException, NoConnectionException{
		this.writeLinkInfoToSQL(this.dba);
	}
	/**
	 * 	 *
	 * @param args Array of arguments, usually passed on the command line.
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 */
	public static void main(final String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
//		String plans = "E:\\temp\\LTA prep\\analysis\\output_plans.xml.gz";
		DataBaseAdmin dba = new DataBaseAdmin(new File("../krakatauPG/input/db.properties"));
		String networkFile = "../krakatauPG/input/singapore_mps_modes.xml";
		new MATSimUtils(networkFile).changeNetworkSpeedCaps("../krakatauPG/input/log/20110614 network fixes/linkmap.csv",
				"../krakatauPG/input/log/20110614 network fixes/singapore_mps_modes.xml");
		dba.close();
	}
	
	

}
