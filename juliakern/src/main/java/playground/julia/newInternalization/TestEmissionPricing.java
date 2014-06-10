/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
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
package playground.julia.newInternalization;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.julia.distribution.GridTools;


/**
 * @author benjamin
 *
 */
public class TestEmissionPricing {
	
	static String inputPath = "../../detailedEval/emissions/testScenario/input/";
	
	static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
	static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	static String emissionVehicleFile = inputPath + "emissionVehicles_1pct.xml.gz";
	
	static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
	
	static boolean isUsingDetailedEmissionCalculation = true;
	static String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	static String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
	
//	static String outputPath = "../../detailedEval/emissions/testScenario/output/";
	static String outputPath = "output/testEmissionPricing/";

	private static Logger logger;

	private static Double xMin = 0.0;

	private static Double xMax = 20000.;

	private static Double yMin = 0.0;

	private static Double yMax = 12500.;

	private static Map<Id, Integer> links2xCells;

	private static Integer noOfXCells = 16;

	private static Map<Id, Integer> links2yCells;

	private static Integer noOfYCells = 12;

	private static ResponsibilityGridTools rgt;

	private static Double timeBinSize = 3600.;

	private static int noOfTimeBins = 24;
	
	private static int numberOfIterations = 30;

	private static double epsilon = Double.MIN_NORMAL;
	
	public static void main(String[] args) {
		
		logger = Logger.getLogger(TestEmissionPricing.class);
		
		Config config = new Config();
		config.addCoreModules();
		config.setParam("strategy", "maxAgentPlanMemorySize", "11");
		Controler controler = new Controler(config);
		
		
	// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
	// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputPath);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(numberOfIterations);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
//		ccg.setRunId("321");
		
	// qsimConfigGroup
		QSimConfigGroup qcg = controler.getConfig().qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(30 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);
//		qcg.setFlowCapFactor(0.01);
//		qcg.setStorageCapFactor(0.03);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		qcg.setStuckTime(10.0);
		
	// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		Set<String> activities = new HashSet<String>();
		activities.add("unknown");
		activities.add("work");
		activities.add("pickup");
		activities.add("with adult");
		activities.add("other");
		activities.add("pvWork");
		activities.add("pvHome");
		activities.add("gvHome");
		activities.add("education");
		activities.add("business");
		activities.add("shopping");
		activities.add("private");
		activities.add("leisure");
		activities.add("sports");
		activities.add("home");
		activities.add("friends");
		
		for(String activity : activities){
			ActivityParams params = new ActivityParams(activity);
			if(activity.equals("home")){
				params.setTypicalDuration(12*3600);
			}else{
				params.setTypicalDuration(8 * 3600);
			}
			pcs.addActivityParams(params);
		}
		
		

	// strategy
		StrategyConfigGroup scg = controler.getConfig().strategy();
		StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("BestScore");
		strategySettings.setProbability(0.01);
		scg.addStrategySettings(strategySettings);
		StrategySettings strategySettingsR = new StrategySettings(new IdImpl("2"));
		strategySettingsR.setModuleName("ReRoute");
		strategySettingsR.setProbability(1.0);
		strategySettingsR.setDisableAfter(10);
		scg.addStrategySettings(strategySettingsR);
		
	// network
		Scenario scenario = controler.getScenario();
		createNetwork(scenario);
		
	// plans
		createActiveAgents(scenario);
		createPassiveAgents(scenario);
		
	// define emission tool input files	
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		vcg.setEmissionVehicleFile(emissionVehicleFile);
		
		vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		
		vcg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		vcg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		vcg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
		
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
//		vcg.setWritingOutputEvents(false) ;
//		
//		EmissionControlerListener ecl = new EmissionControlerListener(controler);
//		controler.addControlerListener(ecl);
//		controler.setScoringFunctionFactory(new ResponsibilityScoringFunctionFactory(config, controler.getNetwork(), ecl));
		//controler.setTravelDisutilityFactory(new ResDisFactory(ecl, ecl.emissionModule, new EmissionCostModule(1.0)));
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(1.0);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		links2xCells = gt.mapLinks2Xcells(noOfXCells);
		links2yCells = gt.mapLinks2Ycells(noOfYCells);
		
		rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(1.0,	false, rgt, links2xCells, links2yCells);
		EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		controler.setTravelDisutilityFactory(emfac);
		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));
		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis",new OTFFileWriterFactory());
		
		controler.run();
		
		Person activeAgent = scenario.getPopulation().getPersons().get(new IdImpl("567417.1#12424"));
		Double scoreOfSelectedPlan;
		Plan selectedPlan = activeAgent.getSelectedPlan();
		
		// check selected plan 
		scoreOfSelectedPlan = selectedPlan.getScore();
		for(PlanElement pe: selectedPlan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
					if(lnri.getLinkIds().contains(new IdImpl("39"))){
						logger.info("Selected route should not use link 39."); //System.out.println("39 contained");
					}else{
						if(lnri.getLinkIds().contains(new IdImpl("38"))){
							logger.info("Selected route avoids node 9 as it is supposed to. " +
									"It's score is " + selectedPlan.getScore());
						}
					}
				}
		}
		
		// check not selected plans - score should be worse if link 39 is used
		
		for(Plan p: activeAgent.getPlans()){			
				for(PlanElement pe: p.getPlanElements()){
					if(pe instanceof Leg){
						Leg leg = (Leg)pe;
						LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
						if(lnri.getLinkIds().contains(new IdImpl("39"))){
							logger.info("This plan uses node 9 and has score " + p.getScore()+ ". Selected = " + p.isSelected());
						}
						if(lnri.getLinkIds().contains(new IdImpl("38"))){
							logger.info("This plan uses node 8 and has score " + p.getScore() + ". Selected = " + p.isSelected());
						}
					}
				}
			
		}
		
		
//		// check links
//		for(Id linkId: scenario.getNetwork().getLinks().keySet()){
//			logger.info("link id " + linkId.toString() + " cell " + links2xCells.get(linkId) + " , " + links2yCells.get(linkId));
//		}
//		logger.info("epsilon"+epsilon);
	}
	
	private static void createNetwork(Scenario scenario) {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(1.0, 10000.0));
		Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord(2500.0, 10000.0));
		Node node3 = network.createAndAddNode(scenario.createId("3"), scenario.createCoord(4500.0, 10000.0));
		Node node4 = network.createAndAddNode(scenario.createId("4"), scenario.createCoord(17500.0, 10000.0));
		Node node5 = network.createAndAddNode(scenario.createId("5"), scenario.createCoord(19999.0, 10000.0));
		Node node6 = network.createAndAddNode(scenario.createId("6"), scenario.createCoord(19999.0, 1.0));
		Node node7 = network.createAndAddNode(scenario.createId("7"), scenario.createCoord(1.0, 1.0));
		Node node8 = network.createAndAddNode(scenario.createId("8"), scenario.createCoord(12500.0,  12499.0));
		Node node9 = network.createAndAddNode(scenario.createId("9"), scenario.createCoord(12500.0, 7500.0));
		Node homeNode = network.createAndAddNode(scenario.createId("homeNode"), scenario.createCoord(1.0, 2.0));
		Node workNode = network.createAndAddNode(scenario.createId("workNode"), scenario.createCoord(19999.0, 2.0));


		network.createAndAddLink(scenario.createId("12"), node1, node2, 1000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("23"), node2, node3, 2000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("45"), node4, node5, 2000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("56"), node5, node6, 1000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("67"), node6, node7, 1000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("71"), node7, node1, 1000, 30.00, 3600, 1, null, "22");
		
		// two similar path from node 3 to node 4 - north: route via node 8, south: route via node 9
		network.createAndAddLink(scenario.createId("38"), node3, node8, 5150, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("39"), node3, node9, 5000, 30.00, 3600, 1, null, "22"); // 34.50km/h => nearly same score
		network.createAndAddLink(scenario.createId("84"), node8, node4, 5000, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("94"), node9, node4, 5000, 30.00, 3600, 1, null, "22");
		
		// two small links for work and home
		network.createAndAddLink(scenario.createId("home7"), homeNode, node7, epsilon , 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("7home"), node7, homeNode, epsilon, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("work6"), workNode, node6, epsilon, 30.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("6work"), node6, workNode, epsilon, 30.00, 3600, 1, null, "22");
		
		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){
				String idpart = i.toString()+j.toString();

				double xCoord = 9250. + i*1250;
				double yCoord = 5200. + j*1041;
				
				// add a link for each person
				Node nodeA = network.createAndAddNode(scenario.createId("node_"+idpart+"A"), scenario.createCoord(xCoord, yCoord));
				Node nodeB = network.createAndAddNode(scenario.createId("node_"+idpart+"B"), scenario.createCoord(xCoord, yCoord+10.));
				network.createAndAddLink(scenario.createId("link_p"+idpart), nodeA, nodeB, 10, 30.0, 3600, 1);

			}
		}
	}
	
	private static void createPassiveAgents(Scenario scenario) {
		PopulationFactoryImpl pFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		// passive agents' home coordinates are around node 9 (12500, 7500)
		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){
				
				String idpart = i.toString()+j.toString();
				
				Person person = pFactory.createPerson(scenario.createId("passive_"+idpart)); //new PersonImpl (new IdImpl(i));

				double xCoord = 9250. + i*1250;
				double yCoord = 5200. + j*1041;
				Plan plan = pFactory.createPlan(); //person.createAndAddPlan(true);
				
				Coord coord = new CoordImpl(xCoord, yCoord);
				Activity home = pFactory.createActivityFromCoord("home", coord );
				home.setEndTime(6 * 3600);
				Leg leg = pFactory.createLeg(TransportMode.walk);
				Coord coord2 = new CoordImpl(xCoord, yCoord+10.);
				Activity home2 = pFactory.createActivityFromCoord("home", coord2);

				plan.addActivity(home);
				plan.addLeg(leg);
				plan.addActivity(home2);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
	}
	
	private static void createActiveAgents(Scenario scenario) {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(scenario.createId("567417.1#12424"));
		Plan plan = pFactory.createPlan();

	Activity home = pFactory.createActivityFromLinkId("home", scenario.createId("home7"));
		home.setEndTime(6 * 3600 + 10);
		plan.addActivity(home);

		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);

		Activity work = pFactory.createActivityFromLinkId("work", scenario.createId("6work"));
		work.setEndTime(16 * 3600 + 10);
		plan.addActivity(work);

		Leg leg2 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg2);

		home = pFactory.createActivityFromLinkId("home", scenario.createId("12"));
		plan.addActivity(home);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
}