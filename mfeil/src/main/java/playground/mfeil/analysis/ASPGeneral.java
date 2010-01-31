/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansGeneral.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import playground.mfeil.attributes.AgentsAttributesAdder;
import playground.mfeil.ActChainEqualityCheck;


/**
 * This is a class that facilitates calling various other analysis classes. 
 * It summarizes the analysis functionalities and offers simple access.
 *
 * @author mfeil
 */
public class ASPGeneral {
	
	private static final Logger log = Logger.getLogger(ASPActivityChains.class);
	private ArrayList<List<PlanElement>> activityChainsMATSim;
	private ArrayList<ArrayList<Plan>> plansMATSim;
	private ArrayList<List<PlanElement>> activityChainsMZ;
	private ArrayList<ArrayList<Plan>> plansMZ;
	private ASPActivityChains sp;
	private ASPActivityChains spMZ;
	private Map<Id, Double> personsWeights;
	
	private void runMATSim (final PopulationImpl population){
		log.info("Analyzing MATSim population...");
		this.sp = new ASPActivityChains(population);
		this.sp.run();
		this.activityChainsMATSim = sp.getActivityChains();
		this.plansMATSim = sp.getPlans();
		log.info("done.");
	}
	
	private PopulationImpl reducePopulation (PopulationImpl pop, final String attributesInputFile){
		log.info("Reading weights of persons...");
		// Get persons' weights
		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		this.personsWeights = aaa.getAgentsWeight();
		log.info("done.");
		
		log.info("Reducing population...");
		// Drop persons for whom no weight info is available
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = pop.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!this.personsWeights.containsKey(person.getId())) pop.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+pop.getPersons().size()+".");
		return pop;
	}
	
	private void runMZ (final PopulationImpl population){
		log.info("Analyzing MZ population...");
		// Analyze the activity chains
		this.spMZ = new ASPActivityChains(population);
		this.spMZ.run();
		this.activityChainsMZ = spMZ.getActivityChains();
		this.plansMZ = spMZ.getPlans();
		log.info("done.");
	}
	
	private void compareMATSimAndMZ (final String compareOutput){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(compareOutput));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		/* Analysis of activity chains */
		double averageACLengthMZWeighted=0;
		double averageACLengthMZUnweighted=0;
		double averageACLengthMATSim=0;
		stream.println("MZ_weighted\t\tMZ_not_weighted\t\tMATSim");
		stream.println("Number of occurrences\tRelative\tNumber of occurrences\tRelative\tNumber of occurrences\tRelative\tActivity chain");
		
		// Overall weighted and unweighted number of persons in MZ	
		double overallWeight = 0;
		double overallUnweighted = 0;
		double overallMATSim = 0;
		for (int i=0; i<this.plansMZ.size();i++){
			for (int j=0;j<this.plansMZ.get(i).size();j++){
				overallWeight += this.personsWeights.get(((Plan)this.plansMZ.get(i).get(j)).getPerson().getId());
				overallUnweighted += this.plansMZ.get(i).size();
			}
		}
		// Overall number of persons in MATSim
		for (int i=0; i<this.plansMATSim.size();i++){
			overallMATSim += this.plansMATSim.get(i).size();
		}
		
		// Calculate MZ chains
		ActChainEqualityCheck check = new ActChainEqualityCheck();
		for (int i=0;i<this.activityChainsMZ.size();i++){
			// MZ weighted
			double weight = 0;
			for (int j=0;j<this.plansMZ.size();j++){
				weight += this.personsWeights.get(((Plan) this.plansMZ.get(j)).getPerson());
			}
			stream.print(weight+"\t"+weight/overallWeight+"\t");
			
			// MZ unweighted
			stream.print(this.plansMZ.get(i).size()+"\t"+this.plansMZ.get(i).size()/overallUnweighted+"\t");
			
			// MATSim
			boolean found = false;
			for (int k=0;k<this.activityChainsMATSim.size();k++){
				if (check.checkEqualActChains(this.activityChainsMATSim.get(k), this.activityChainsMZ.get(i))){
					stream.print(this.plansMATSim.get(k).size()+"\t"+this.plansMATSim.get(k).size()/overallMATSim+"\t");
					found = true;
					break;
				}
			}
			if (!found) stream.print("0\t0\t");
			
			// Activity chain
			for (int j=0; j<this.activityChainsMZ.size();j=j+2){
				stream.print(((ActivityImpl)(this.activityChainsMZ.get(i).get(j))).getType()+"\t");
			}
			stream.println();
		}
		
		// Calculate missing MATSim chains
		for (int i=0;i<this.activityChainsMATSim.size();i++){
			for (int k=0;k<this.activityChainsMZ.size();k++){
				if (check.checkEqualActChains(this.activityChainsMATSim.get(i), this.activityChainsMZ.get(k))){
					stream.print("0\t0\t0\t0\t"+this.plansMATSim.get(i).size()+"\t"+this.plansMATSim.get(i).size()/overallMATSim+"\t");
					for (int j=0; j<this.activityChainsMATSim.size();j=j+2){
						stream.print(((ActivityImpl)(this.activityChainsMATSim.get(i).get(j))).getType()+"\t");
					}
					stream.println();
					break;
				}
			}
		}
		// Average lenghts of act chains
		stream.println((averageACLengthMZWeighted/overallWeight)+"\t\t"+(averageACLengthMZUnweighted/overallUnweighted)+"\t\t"+(averageACLengthMATSim/overallMATSim)+"\tAverage number of activities");
		stream.println();
		
		double[] kpisMZWeighted = this.spMZ.analyzeActTypes(this.personsWeights);
		double[] kpisMZUnweighted = this.spMZ.analyzeActTypes(null);
		double[] kpisMATSim = this.sp.analyzeActTypes(null);
		stream.println(kpisMZWeighted[0]+"\t\t"+kpisMZUnweighted[0]+"\t\t"+kpisMATSim[0]+"\tAverage number of same consecutive acts per plan");
		stream.println(kpisMZWeighted[1]+"\t\t"+kpisMZUnweighted[1]+"\t\t"+kpisMATSim[1]+"\tPercentage of same consecutive acts");
		stream.println(kpisMZWeighted[2]+"\t\t"+kpisMZUnweighted[2]+"\t\t"+kpisMATSim[2]+"\tAverage number of occurrences of same acts per plan");
		stream.println(kpisMZWeighted[3]+"\t\t"+kpisMZUnweighted[3]+"\t\t"+kpisMATSim[3]+"\tAverage number of same acts per plan");
		stream.println(kpisMZWeighted[4]+"\t\t"+kpisMZUnweighted[4]+"\t\t"+kpisMATSim[4]+"\tAverage maximum number of same acts per plan");
		stream.println(kpisMZWeighted[5]+"\t\t"+kpisMZUnweighted[5]+"\t\t"+kpisMATSim[5]+"\tShare of plans in which same acts occur");
	}
	
	public static void main(final String [] args) {
		// Scenario files
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		
		// Special MZ file so that weights of MZ persons can be read
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
		// Population files
		final String populationFilenameMATSim = "/home/baug/mfeil/data/runs/run0922_initialdemand_20/output_plans.xml";
		final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
		
		// Output file
		final String outputFile = "/home/baug/mfeil/data/runs/run0922_initialdemand_20";	
		
		// Settings
		final boolean compareWithMZ = true; 
		
	
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);
		new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
		
		ScenarioImpl scenarioMZ = null;
		if (compareWithMZ){
			scenarioMZ = new ScenarioImpl();
			scenarioMZ.setNetwork(scenarioMATSim.getNetwork());
			new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
			new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
		}

		
		ASPGeneral asp = new ASPGeneral();
		asp.runMATSim(scenarioMATSim.getPopulation());
		
		if (compareWithMZ) {
			PopulationImpl pop = asp.reducePopulation(scenarioMZ.getPopulation(), attributesInputFile);
			asp.runMZ(pop);
			asp.compareMATSimAndMZ(outputFile);
		}

		
		log.info("Analysis of plan finished.");
	}

}

