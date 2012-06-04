/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeDistanceDistribution.java
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
package playground.benjamin.scenarios.munich.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistribution {
	private static final Logger logger = Logger.getLogger(LegModeDistanceDistribution.class);

//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_baseCase_ctd/";
//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_pricing_x10/";
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_zone30/";
	
	private static String initialIterationNo = "1000";
	private static String finalIterationNo = null;
	
	private static String initialPlansFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + initialIterationNo + ".plans.xml.gz";

	private static String finalPlansFile = runDirectory + "output_plans.xml.gz";
	private static String netFile = runDirectory + "output_network.xml.gz";

//	private static String finalPlansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";
//	private static String netFile = "../../detailedEval/Net/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";

//	private final UserGroup group2analyze = UserGroup.MID;
	private final UserGroup group2analyze = null;
	
	private final int noOfDistanceClasses = 15;
	private final boolean considerGroups = false;

	private boolean considerUserGroupOnly;
	private final List<Integer> distanceClasses;
	private final SortedSet<String> usedModes;

	public LegModeDistanceDistribution(){
		this.distanceClasses = new ArrayList<Integer>();
		this.usedModes = new TreeSet<String>();
		
		if(group2analyze == null){
			considerUserGroupOnly = false;
		} else {
			considerUserGroupOnly = true;
		}
	}

	private void run(String[] args) {
		Scenario initialScenario = loadScenario(netFile, initialPlansFile);
		Scenario finalScenario = loadScenario(netFile, finalPlansFile);
		setDistanceClasses(noOfDistanceClasses);

		Population initialPop = initialScenario.getPopulation();
		Population finalPop = finalScenario.getPopulation();

		getUsedModes(initialPop);

		PersonFilter personFilter = new PersonFilter();
		
//		if(considerGroups){
//			for(UserGroup userGroup : UserGroup.values()){
//				Population initialRelevantPop = personFilter.getPopulation(initialPop, userGroup);
//				Population finalRelevantPop = personFilter.getPopulation(finalPop, userGroup);
//				SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(initialRelevantPop);
//				SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(finalRelevantPop);
//				SortedMap<String, Map<Integer, Integer>> differenceMode2DistanceClassNoOfLegs = calculateDifferenceMode2DistanceClassNoOfLegs(initialMode2DistanceClassNoOfLegs, finalMode2DistanceClassNoOfLegs);
//				
//				System.out.println("\n*******************************************************************");
//				System.out.println("The initial LegModeDistanceDistribution for group " + userGroup + " is : " + initialMode2DistanceClassNoOfLegs);
//				System.out.println("The final LegModeDistanceDistribution for group " + userGroup + " is : " + finalMode2DistanceClassNoOfLegs);
//				System.out.println("The difference in the LegModeDistanceDistribution for group " + userGroup + " is :" + differenceMode2DistanceClassNoOfLegs);
//				System.out.println("\n*******************************************************************");
//				writeInformation(initialMode2DistanceClassNoOfLegs, userGroup + "_legModeDistanceDistributionInitial");
//				writeInformation(finalMode2DistanceClassNoOfLegs, userGroup + "_legModeDistanceDistributionFinal");
//				writeInformation(differenceMode2DistanceClassNoOfLegs, userGroup + "_legModeDistanceDistributionDifference");
//			}
//		}
		SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassNoOfLegs;
		SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassNoOfLegs;
		SortedMap<String, Map<Integer, Integer>> differenceMode2DistanceClassNoOfLegs;
		
		SortedMap<String, Integer> initialMode2NoOfLegs;
		SortedMap<String, Integer> finalMode2NoOfLegs;
		
		SortedMap<String, Double> initialMode2Share;
		SortedMap<String, Double> finalMode2Share;
		SortedMap<String, Double> differenceMode2Share;


		if(considerUserGroupOnly){
			logger.warn("Values are calculated for " + group2analyze + " ...");
			Population initialRelevantPop = personFilter.getPopulation(initialPop, group2analyze);
			Population finalRelevantPop = personFilter.getPopulation(finalPop, group2analyze);
			initialMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(initialRelevantPop);
			finalMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(finalRelevantPop);
			
			initialMode2NoOfLegs = calculateMode2NoOfLegs(initialRelevantPop);
			finalMode2NoOfLegs = calculateMode2NoOfLegs(finalRelevantPop);
		} else {
			logger.warn("Values are calculated for the whole population ...");
			initialMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(initialPop);
			finalMode2DistanceClassNoOfLegs = calculateMode2DistanceClassNoOfLegs(finalPop);
			
			initialMode2NoOfLegs = calculateMode2NoOfLegs(initialPop);
			finalMode2NoOfLegs = calculateMode2NoOfLegs(finalPop);
		}
		differenceMode2DistanceClassNoOfLegs = calculateDifferenceMode2DistanceClassNoOfLegs(initialMode2DistanceClassNoOfLegs, finalMode2DistanceClassNoOfLegs);

		initialMode2Share = calculateModeShare(initialMode2NoOfLegs);
		finalMode2Share = calculateModeShare(finalMode2NoOfLegs);
		differenceMode2Share = calculateModeShareDifference(initialMode2Share, finalMode2Share);
		
		System.out.println("\n*******************************************************************");
		System.out.println("The initial mode share [in %] is :" + initialMode2Share);
		System.out.println("The final mode share [in %] is :" + finalMode2Share);
		System.out.println("The difference in the mode share [in % points] is :" + differenceMode2Share);
		System.out.println("\n*******************************************************************");
		writeInformation(initialMode2DistanceClassNoOfLegs, "legModeDistanceDistributionInitial");
		writeInformation(finalMode2DistanceClassNoOfLegs, "legModeDistanceDistributionFinal");
		writeInformation(differenceMode2DistanceClassNoOfLegs, "legModeDistanceDistributionDifference");
	}

	private void writeInformation(Map<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs, String fileName) {
		String outFile = runDirectory + fileName + ".txt";
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			for(String mode : this.usedModes){
				out.write("\t" + mode);
			}
			out.write("\t" + "sum");
			out.write("\n");
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				//	Integer middleOfDistanceClass = ((this.distanceClasses.get(i) + this.distanceClasses.get(i + 1)) / 2);
				//	out.write(middleOfDistanceClass + "\t");
				out.write(this.distanceClasses.get(i+1) + "\t");
				Integer totalLegsInDistanceClass = 0;
				for(String mode : this.usedModes){
					Integer modeLegs = null;
					modeLegs = mode2DistanceClassNoOfLegs.get(mode).get(this.distanceClasses.get(i + 1));
					totalLegsInDistanceClass = totalLegsInDistanceClass + modeLegs;
					out.write(modeLegs.toString() + "\t");
				}
				out.write(totalLegsInDistanceClass.toString());
				out.write("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		}catch (Exception e){
			logger.error("Error: " + e.getMessage());
		}
	}

	private SortedMap<String, Map<Integer, Integer>> calculateDifferenceMode2DistanceClassNoOfLegs(SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassNoOfLegs, SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassNoOfLegs) {
		SortedMap<String, Map<Integer, Integer>> modeDifference2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();
		for(String mode : finalMode2DistanceClassNoOfLegs.keySet()){
			Map<Integer, Integer> finalMap = new TreeMap<Integer, Integer>();
			for(Entry<Integer, Integer> entry: finalMode2DistanceClassNoOfLegs.get(mode).entrySet()){
				Integer distanceClass = entry.getKey();
				Integer difference = entry.getValue() - initialMode2DistanceClassNoOfLegs.get(mode).get(entry.getKey());
				finalMap.put(distanceClass, difference);
			}
			modeDifference2DistanceClassNoOfLegs.put(mode, finalMap);
		}
		return modeDifference2DistanceClassNoOfLegs;
	}

	private SortedMap<String, Double> calculateModeShareDifference(SortedMap<String, Double> initialMode2Share,	SortedMap<String, Double> finalMode2Share) {
		SortedMap<String, Double> mode2ShareDiff = new TreeMap<String, Double>();
		
		for(String mode : finalMode2Share.keySet()){
			double finalModeShare = finalMode2Share.get(mode);
			double initialModeShare = initialMode2Share.get(mode);
			double modeShareDiff = finalModeShare - initialModeShare;
			mode2ShareDiff.put(mode, modeShareDiff);
		}
		return mode2ShareDiff;
	}

	private SortedMap<String, Double> calculateModeShare(SortedMap<String, Integer> mode2NoOfLegs) {
		SortedMap<String, Double> mode2Share = new TreeMap<String, Double>();
		int totalNoOfLegs = 0;
		for(String mode : mode2NoOfLegs.keySet()){
			int modeLegs = mode2NoOfLegs.get(mode);
			totalNoOfLegs += modeLegs;
		}
		for(String mode : mode2NoOfLegs.keySet()){
			double share = 100. * (double) mode2NoOfLegs.get(mode) / totalNoOfLegs;
			mode2Share.put(mode, share);
		}
		return mode2Share;
	}

	private SortedMap<String, Integer> calculateMode2NoOfLegs(Population population) {
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<String, Integer>();
		
		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();
					
					if(mode2NoOfLegs.get(mode) == null){
						mode2NoOfLegs.put(mode, 1);
					} else {
						int legsSoFar = mode2NoOfLegs.get(mode);
						int legsAfter = legsSoFar + 1;
						mode2NoOfLegs.put(mode, legsAfter);
					}
				}
			}
		}
		return mode2NoOfLegs;
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DistanceClassNoOfLegs(Population population) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassNoOfLegs = new TreeMap<String, Map<Integer, Integer>>();

		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> distanceClass2NoOfLegs = new TreeMap<Integer, Integer>();
			for(int i = 0; i < this.distanceClasses.size() - 1 ; i++){
				Integer noOfLegs = 0;
				for(Person person : population.getPersons().values()){
					PlanImpl plan = (PlanImpl) person.getSelectedPlan();
					List<PlanElement> planElements = plan.getPlanElements();
					for(PlanElement pe : planElements){
						if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							String legMode = leg.getMode();
							Coord from = plan.getPreviousActivity(leg).getCoord();
							Coord to = plan.getNextActivity(leg).getCoord();
							Double legDist = CoordUtils.calcDistance(from, to);

							if(legMode.equals(mode)){
								if(legDist > this.distanceClasses.get(i) && legDist <= this.distanceClasses.get(i + 1)){
									noOfLegs++;
								}
							}
						}
					}
				}
				distanceClass2NoOfLegs.put(this.distanceClasses.get(i + 1), noOfLegs);
			}
			mode2DistanceClassNoOfLegs.put(mode, distanceClass2NoOfLegs);
		}
		return mode2DistanceClassNoOfLegs;
	}

	private void getUsedModes(Population initialPop) {
		for(Person person : initialPop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!this.usedModes.contains(legMode)){
						this.usedModes.add(legMode);
					}
				}
			}
		}
		logger.info("The following transport modes are found in the initial population: " + this.usedModes);
	}

	private void setDistanceClasses(int i) {
		this.distanceClasses.add(0);
		for(int noOfClasses = 0; noOfClasses < i; noOfClasses++){
			int distanceClass = 100 * (int) Math.pow(2, noOfClasses);
			this.distanceClasses.add(distanceClass);
		}
		logger.info("The following distance classes were defined: " + this.distanceClasses);
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.run(args);
	}
}