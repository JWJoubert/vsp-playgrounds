/* *********************************************************************** *
 * project: org.matsim.*
 * Simulator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.coopsim;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.Desires;

import playground.johannes.coopsim.LoggerUtils;
import playground.johannes.coopsim.SimEngine;
import playground.johannes.coopsim.analysis.ActTypeShareTask;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ArrivalTimeTask;
import playground.johannes.coopsim.analysis.InfiniteScoresTask;
import playground.johannes.coopsim.analysis.JointActivityTask;
import playground.johannes.coopsim.analysis.PlansWriterTask;
import playground.johannes.coopsim.analysis.ScoreTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDistanceTask;
import playground.johannes.coopsim.eval.ActivityDurationEvaluator;
import playground.johannes.coopsim.eval.ActivityEvaluator;
import playground.johannes.coopsim.eval.EvalEngine;
import playground.johannes.coopsim.eval.EvaluatorComposite;
import playground.johannes.coopsim.eval.JointActivityEvaluator;
import playground.johannes.coopsim.eval.LegEvaluator;
import playground.johannes.coopsim.mental.MentalEngine;
import playground.johannes.coopsim.mental.choice.ActTypeTimeSelector;
import playground.johannes.coopsim.mental.choice.ActivityFacilitySelector;
import playground.johannes.coopsim.mental.choice.ActivityGroupGenerator;
import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ActivityTypeSelector;
import playground.johannes.coopsim.mental.choice.ArrivalTimeSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelectorComposite;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.coopsim.mental.choice.DurationSelector;
import playground.johannes.coopsim.mental.choice.EgoSelector;
import playground.johannes.coopsim.mental.choice.EgosFacilities;
import playground.johannes.coopsim.mental.choice.EgosHome;
import playground.johannes.coopsim.mental.choice.PlanIndexSelector;
import playground.johannes.coopsim.mental.choice.RandomAlter;
import playground.johannes.coopsim.mental.choice.RandomAlters;
import playground.johannes.coopsim.mental.planmod.ActivityDurationModAdaptor;
import playground.johannes.coopsim.mental.planmod.ActivityFacilityModAdaptor;
import playground.johannes.coopsim.mental.planmod.ActivityTypeModAdaptor;
import playground.johannes.coopsim.mental.planmod.ArrivalTimeModAdaptor;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptorComposite;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptorFactory;
import playground.johannes.coopsim.mental.planmod.concurrent.ConcurrentPlanModEngine;
import playground.johannes.coopsim.pysical.PhysicalEngine;
import playground.johannes.sna.util.MultiThreading;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.GaussDistribution;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author illenberger
 *
 */
public class Simulator {
	
	private static final Logger logger = Logger.getLogger(Simulator.class);

	private static SocialGraph graph;
	
	private static NetworkImpl network;
	
	private static ActivityFacilitiesImpl facilities;
	
	private static Config config;
	
	private static Map<Person, Desires> personDesires;
	
	public static void main(String[] args) throws IOException {
		LoggerUtils.setDisallowVerbose(false);
		
		LoggerUtils.setVerbose(false);
		config = new Config();
		config.addCoreModules();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		LoggerUtils.setVerbose(true);
		
		loadData(config);

		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		int iterations = (int) Double.parseDouble(config.getParam("controler", "lastIteration"));
		String output = config.getParam("controler", "outputDirectory");
		int sampleInterval = (int) Double.parseDouble(config.getParam("socialnets", "sampleinterval"));
		/*
		 * initialize physical engine
		 */
		logger.info("Initializing physical engine...");
		PhysicalEngine physical = new PhysicalEngine(network);
		/*
		 * do some pre-processing
		 */
		logger.info("Validating facilities...");
		FacilityValidator.generate(facilities, network, graph);
		
		logger.info("Generation initial state...");
		NetworkLegRouter router = initRouter(physical.getTravelTime());
		InitialStateGenerator.generate(graph, facilities, router);
		/*
		 * initialize choice selectors
		 */
		logger.info("Initializing choice selectors...");
		ChoiceSelector choiceSelector = initSelectors(random);
		
//		DurationChoiceLogger durLogger = new DurationChoiceLogger();
//		((ChoiceSelectorComposite)choiceSelector).addComponent(durLogger);
		/*
		 * initialize adaptors
		 */
		logger.info("Initializing choice adaptors...");
		AdaptorFactory adaptorFactory = new AdaptorFactory(physical);
		/*
		 * initialize mental engine
		 */
		logger.info("Initializing mental engine...");
		int threads = config.global().getNumberOfThreads();
		MultiThreading.setNumAllowedThreads(threads);
		MentalEngine mental;
		if(threads > 1)
			mental = new MentalEngine(graph, choiceSelector, new ConcurrentPlanModEngine(adaptorFactory), random);
		else
			mental = new MentalEngine(graph, choiceSelector, adaptorFactory.create(), random);
		/*
		 * initialize scoring
		 */
		logger.info("Initializing evaluation engine...");
		Map<String, Double> priorities = new HashMap<String, Double>();
		priorities.put(ActivityType.home.name(), Double.parseDouble(config.getParam("socialnets", "priority_home")));
		priorities.put(ActivityType.visit.name(), Double.parseDouble(config.getParam("socialnets", "priority_visit")));
		priorities.put(ActivityType.culture.name(), Double.parseDouble(config.getParam("socialnets", "priority_culture")));
		priorities.put(ActivityType.gastro.name(), Double.parseDouble(config.getParam("socialnets", "priority_gastro")));
		
		double beta_join = Double.parseDouble(config.getParam("socialnets", "beta_join"));
		double beta_duration = Double.parseDouble(config.getParam("socialnets", "beta_duration"));
		double beta_act = ((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")).getPerforming_utils_hr() / 3600.0;
		double beta_leg = ((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")).getTraveling_utils_hr() / 3600.0;
	
		EvaluatorComposite evaluator = new EvaluatorComposite();
		evaluator.addComponent(new LegEvaluator(beta_leg));
		evaluator.addComponent(new ActivityEvaluator(beta_act, personDesires, priorities));
		evaluator.addComponent(new JointActivityEvaluator(beta_join, physical.getVisitorTracker(), graph));
		evaluator.addComponent(new ActivityDurationEvaluator(beta_duration, personDesires));
		EvalEngine eval = new EvalEngine(evaluator);
		/*
		 * initialize simulation engine
		 */
		logger.info("Initializing simulation engine...");
		SimEngine simEngine = new SimEngine(graph, mental, physical, eval);
		simEngine.setSampleInterval(sampleInterval);
		simEngine.setLogInerval(500);
		/*
		 * initialize analyzer tasks
		 */
		TrajectoryAnalyzerTask.overwriteStratification(30, 1);
		TrajectoryAnalyzerTask task = initAnalyzerTask(physical, eval);
//		((TrajectoryAnalyzerTaskComposite)task).addTask(durLogger);
		simEngine.setAnalyzerTask(task, output);
		
		simEngine.run(iterations);
	}

	private static void loadData(Config config) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		logger.info("Loading network data...");
		LoggerUtils.setVerbose(false);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		network = scenario.getNetwork();
		LoggerUtils.setVerbose(true);
		
		logger.info("Loading facilities data...");
		LoggerUtils.setVerbose(false);
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(config.getParam("facilities", "inputFacilitiesFile"));
		facilities = scenario.getActivityFacilities();
		LoggerUtils.setVerbose(true);
		
		logger.info("Loading social graph data...");
		LoggerUtils.setVerbose(false);
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		graph = reader.readGraph(config.getParam("socialnets", "graphfile"));
		LoggerUtils.setVerbose(true);
	}
	
	private static NetworkLegRouter initRouter(final TravelTime travelTime) {
		TravelCost travelCost = new TravelCost() {
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
		};
		
		TravelMinCost travelMinCost = new TravelMinCost() {
			
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
			
			@Override
			public double getLinkMinimumTravelCost(Link link) {
				return travelTime.getLinkTravelTime(link, 0);
			}
		};
		
//		LeastCostPathCalculator router = new Dijkstra(network, travelCost, travelTime);
		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, travelMinCost);
		LeastCostPathCalculator router = factory.createPathCalculator(network, travelCost, travelTime);
		NetworkLegRouter legRouter = new NetworkLegRouter(network, router, new ModeRouteFactory());
		
		return legRouter;
	}
	
	private static ChoiceSelector initSelectors(Random random) throws IOException {
		ChoiceSelectorComposite choiceSelector = new ChoiceSelectorComposite();
		/*
		 * initialize plan index selector
		 */
		choiceSelector.addComponent(new PlanIndexSelector());
		/*
		 * initialize ego selector
		 */
		choiceSelector.addComponent(new EgoSelector(graph, random));
		/*
		 * initialize activity type selector
		 */
		ChoiceSet<String> actTypeChoiceSet = new ChoiceSet<String>(random);
		actTypeChoiceSet.addChoice(ActivityType.visit.name());
		actTypeChoiceSet.addChoice(ActivityType.gastro.name());
		actTypeChoiceSet.addChoice(ActivityType.culture.name());

		ActivityTypeSelector actTypeSelector = new ActivityTypeSelector(actTypeChoiceSet);
		choiceSelector.addComponent(actTypeSelector);
		/*
		 * initialize group selector
		 */
		ActivityGroupSelector groupSelector = new ActivityGroupSelector();
		ActivityGroupGenerator generator = null;
		String type = config.getParam("socialnets", "groupGenerator");
		if(type.equals("randomAlter")) {
			generator = new RandomAlter(random);
			groupSelector.addGenerator(ActivityType.visit.name(), generator);
			groupSelector.addGenerator(ActivityType.gastro.name(), generator);
			groupSelector.addGenerator(ActivityType.culture.name(), generator);
		} else if(type.equals("randomAlters")) {
			double p = Double.parseDouble(config.getParam("socialnets", "alterProba_visit"));
			groupSelector.addGenerator(ActivityType.visit.name(), new RandomAlters(p, 1, random));
			
			p = Double.parseDouble(config.getParam("socialnets", "alterProba_culture"));
			groupSelector.addGenerator(ActivityType.gastro.name(), new RandomAlters(p, random));
			
			p = Double.parseDouble(config.getParam("socialnets", "alterProba_gastro"));
			groupSelector.addGenerator(ActivityType.culture.name(), new RandomAlters(p, random));
		} else {
			throw new IllegalArgumentException(String.format("Activity group generator \"%1$s\" unknown.", type));
		}
		
		choiceSelector.addComponent(groupSelector);
		/*
		 * initialize facility selector
		 */
		ActivityFacilitySelector facilitySelector = new ActivityFacilitySelector();

		facilitySelector.addGenerator(ActivityType.visit.name(), new EgosHome(random));
//		FacilityChoiceSetGenerator generator2 = new FacilityChoiceSetGenerator(-1.4, 5, random, CartesianDistanceCalculator.getInstance());
//		try {
////			generator2.write(generator2.generate(graph, facilities, "gastro"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.gastro.145412.txt");
//			generator2.write(generator2.generate(graph, facilities, "culture"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.culture.txt");
////			generator2.write(generator2.generate(graph, facilities, "sports"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.sports.145412. txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
		facilitySelector.addGenerator(ActivityType.gastro.name(), new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam("socialnets", "choiceset_gastro"), graph), random));
		facilitySelector.addGenerator(ActivityType.culture.name(), new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam("socialnets", "choiceset_culture"), graph), random));

		choiceSelector.addComponent(facilitySelector);
		/*
		 * initialize arrival time selector
		 */
		ActTypeTimeSelector arrTimeSelector = new ActTypeTimeSelector();
		choiceSelector.addComponent(arrTimeSelector);
		// visit
		AdditiveDistribution arrTimePDF = new AdditiveDistribution();
		arrTimePDF.addComponent(new GaussDistribution(3169.5, 41787.8, 274.3));
		arrTimePDF.addComponent(new GaussDistribution(7619.2, 56968.2, 729.4));
		arrTimeSelector.addSelector(ActivityType.visit.name(), new ArrivalTimeSelector(initTimes(arrTimePDF, 86400, random), random));
		//culture
		arrTimePDF = new AdditiveDistribution();
		arrTimePDF.addComponent(new GaussDistribution(3040, 36046, 404));
		arrTimePDF.addComponent(new GaussDistribution(7687, 53845, 1144));
		arrTimeSelector.addSelector(ActivityType.culture.name(), new ArrivalTimeSelector(initTimes(arrTimePDF, 86400, random), random));
		//gastro
		arrTimePDF = new AdditiveDistribution();
		arrTimePDF.addComponent(new GaussDistribution(2023.9, 43138.4, 289.9));
		arrTimePDF.addComponent(new GaussDistribution(13158.8, 59829.9, 811.3));
		arrTimeSelector.addSelector(ActivityType.gastro.name(), new ArrivalTimeSelector(initTimes(arrTimePDF, 86400, random), random));
		/*
		 * initialize duration selector
		 */
		ActTypeTimeSelector durTimeSelector = new ActTypeTimeSelector();
		choiceSelector.addComponent(durTimeSelector);
		
		personDesires = new HashMap<Person, Desires>();
		//visit
		LogNormalDistribution durPDF = new LogNormalDistribution(1.027, 9.743, 1455.990);
		Map<SocialVertex, Double> durations = initTimes(durPDF, 13*3600, random);
		durTimeSelector.addSelector(ActivityType.visit.name(), new DurationSelector(durations, random));
		addDesire(durations, ActivityType.visit.name());
		//culture
		durPDF = new LogNormalDistribution(0.5689, 9.0656, 764.2739);
		durations = initTimes(durPDF, 14*3600, random);
		durTimeSelector.addSelector(ActivityType.culture.name(), new DurationSelector(durations, random));
		addDesire(durations, ActivityType.culture.name());
		//gastro
		durPDF = new LogNormalDistribution(0.6344, 8.7971, 885.4643);
		durations = initTimes(durPDF, 8*3600, random);
		durTimeSelector.addSelector(ActivityType.gastro.name(), new DurationSelector(durations, random));
		addDesire(durations, ActivityType.gastro.name());
		
		return choiceSelector;
	}
	
	private static void addDesire(Map<SocialVertex, Double> desire, String type) {
		for(java.util.Map.Entry<SocialVertex, Double> entry : desire.entrySet()) {
			Desires desires2 = personDesires.get(entry.getKey().getPerson().getPerson());
			if(desires2 == null) {
				desires2 = new Desires(null);
				personDesires.put(entry.getKey().getPerson().getPerson(), desires2);
			}
			desires2.putActivityDuration(type, entry.getValue());
		}
	}
	
	private static Map<SocialVertex, Double> initTimes(UnivariateRealFunction pdf, int max, Random random) {
		TimeSampler sampler = new TimeSampler(pdf, max, random);
		Map<SocialVertex, Double> map = new HashMap<SocialVertex, Double>(graph.getVertices().size());

		for(SocialVertex v : graph.getVertices()) {
			double sample =(double) sampler.nextSample();
			map.put(v, sample);
		}
		
		return map;
	}
	
	private static TrajectoryAnalyzerTask initAnalyzerTask(PhysicalEngine physical, EvalEngine eval) {
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
		composite.addTask(new PlansWriterTask(network));
		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripDistanceTask(facilities));
//		composite.addTask(new TripDistanceAccessibilityTask(graph, facilities));
		composite.addTask(new JointActivityTask(graph, physical.getVisitorTracker()));
		composite.addTask(new ScoreTask(eval));
		composite.addTask(new ActTypeShareTask());
		composite.addTask(new InfiniteScoresTask());
//		composite.addTask(new ActivityDurationPlanTask());
		
		return composite;
	}
	
	private static class AdaptorFactory implements Choice2ModAdaptorFactory {

		private final PhysicalEngine physical;
		
		public AdaptorFactory(PhysicalEngine physical) {
			this.physical = physical;
		}
		
		@Override
		public Choice2ModAdaptor create() {
			Choice2ModAdaptorComposite adaptor = new Choice2ModAdaptorComposite();
			adaptor.addComponent(new ActivityTypeModAdaptor());
			adaptor.addComponent(new ActivityFacilityModAdaptor(facilities, initRouter(physical.getTravelTime())));
			adaptor.addComponent(new ArrivalTimeModAdaptor());
			adaptor.addComponent(new ActivityDurationModAdaptor());
			return adaptor;
		}
		
	}
	
	private static enum ActivityType {
		
		home, visit, culture, gastro
		
	}
}
