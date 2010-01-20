/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.dgrether.daganzosignal;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsFactory;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;

/**
 * @author dgrether
 *
 */
public class DaganzoScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(DaganzoScenarioGenerator.class);

	private static final String DAGANZOBASEDIR = DgPaths.SHAREDSVN + "studies/dgrether/daganzo/";

	public static final String DAGANZONETWORKFILE = DAGANZOBASEDIR
			+ "daganzoNetwork.xml";

//	public static final String NETWORKFILE =  DAGANZOBASEDIR + "daganzoNetworkNoLanes.xml";//DAGANZONETWORKFILE;
	public static final String NETWORKFILE =  DAGANZONETWORKFILE;

	private static final String PLANS1OUT = DAGANZOBASEDIR
			+ "daganzoPlansNormalRoute.xml";

	private static final String PLANS2OUT = DAGANZOBASEDIR
			+ "daganzoPlansAlternativeRoute.xml";

	private static final String CONFIG1OUT = DAGANZOBASEDIR
			+ "daganzoConfigNormalRoute.xml";

	private static final String CONFIG2OUT = DAGANZOBASEDIR
			+ "daganzoConfigAlternativeRoute.xml";

	public static final String LANESOUTPUTFILE = DAGANZOBASEDIR
		+ "daganzoLaneDefinitions.xml";

	public static final String SIGNALSYSTEMSOUTPUTFILE = DAGANZOBASEDIR
		+ "daganzoSignalSystems.xml";

	public static final String SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE = DAGANZOBASEDIR
		+ "daganzoSignalSystemsConfigs.xml";

	private static final String OUTPUTDIRECTORYNORMALROUTE = DAGANZOBASEDIR
		+ "output/normalRoute/";

	private static final String OUTPUTDIRECTORYALTERNATIVEROUTE = DAGANZOBASEDIR
		+ "output/alternativeRoute/";


	public String configOut, plansOut, outputDirectory;

	private static final boolean isAlternativeRouteEnabled = true;

	private static final boolean isUseLanes = true;

	private static final boolean isUseSignalSystems = true;

	private static final int iterations = 1000;
	private static final int iterations2 = 0;

	private static final String controllerClass = AdaptiveController.class.getCanonicalName();

	private Id id1, id2, id4, id5, id6;

	public DaganzoScenarioGenerator() {
		init();
	}

	private void init() {
		if (isAlternativeRouteEnabled) {
			plansOut = PLANS2OUT;
			configOut = CONFIG2OUT;
			outputDirectory = OUTPUTDIRECTORYALTERNATIVEROUTE;
		}
		else {
			plansOut = PLANS1OUT;
			configOut = CONFIG1OUT;
			outputDirectory = OUTPUTDIRECTORYNORMALROUTE;
		}
	}

	private void createIds(ScenarioImpl sc){
		id1 = sc.createId("1");
		id2 = sc.createId("2");
		id4 = sc.createId("4");
		id5 = sc.createId("5");
		id6 = sc.createId("6");
	}

	public void createScenario() {
		//create a scenario
		ScenarioImpl scenario = new ScenarioImpl();
		//get the config
		Config config = scenario.getConfig();
		//set the network input file to the config and load it
		config.network().setInputFile(NETWORKFILE);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		//create some ids as members of the class for convenience reasons
		createIds(scenario);
		//create the plans and write them
		createPlans(scenario);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFile(plansOut);
		if (isUseLanes) {
			config.scenario().setUseLanes(true);
			config.network().setLaneDefinitionsFile(LANESOUTPUTFILE);
			//create the lanes and write them
			LaneDefinitions lanes = createLanes(scenario);
			MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(lanes);
			laneWriter.writeFile(LANESOUTPUTFILE);
		}
		if (isUseSignalSystems) {
			//enable lanes and signal system feature in config
			config.scenario().setUseSignalSystems(true);
			config.signalSystems().setSignalSystemFile(SIGNALSYSTEMSOUTPUTFILE);
			config.signalSystems().setSignalSystemConfigFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
			//create the signal systems and write them
			SignalSystems signalSystems = createSignalSystems(scenario);
			MatsimSignalSystemsWriter ssWriter = new MatsimSignalSystemsWriter(signalSystems);
			ssWriter.writeFile(SIGNALSYSTEMSOUTPUTFILE);
			//create the signal system's configurations and write them
			SignalSystemConfigurations ssConfigs = createSignalSystemsConfig(scenario);
			MatsimSignalSystemConfigurationsWriter ssConfigsWriter = new MatsimSignalSystemConfigurationsWriter(ssConfigs);
			ssConfigsWriter.writeFile(SIGNALSYSTEMCONFIGURATIONSOUTPUTFILE);
		}

		//create and write the config with the correct paths to the files created above
		createConfig(config);
		new ConfigWriter(config).writeFile(configOut);

		log.info("scenario written!");
	}


	private void createPlans(ScenarioImpl scenario) {
		Network network = scenario.getNetwork();
		PopulationImpl population = scenario.getPopulation();
		int firstHomeEndTime =  1 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = network.getLinks().get(scenario.createId("1"));
		Link l7 = network.getLinks().get(scenario.createId("7"));
		PopulationFactory factory = population.getFactory();

		for (int i = 1; i <= 1000; i++) {
			PersonImpl p = (PersonImpl) factory.createPerson(scenario.createId(Integer
					.toString(i)));
			Plan plan = factory.createPlan();
			p.addPlan(plan);
			// home
			// homeEndTime = homeEndTime + ((i - 1) % 3);
			homeEndTime+= 1;
//			if ((i - 1) % 3 == 0) {
//				homeEndTime++;
//			}

			ActivityImpl act1 = (ActivityImpl) factory.createActivityFromLinkId("h", l1.getId());
			act1.setEndTime(homeEndTime);
			plan.addActivity(act1);
			// leg to home
			LegImpl leg = (LegImpl) factory.createLeg(TransportMode.car);
			// TODO check this
			LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(l1, l7);
			if (isAlternativeRouteEnabled) {
				route.setLinks(l1, NetworkUtils.getLinks(network, "2 3 5 6"), l7);
			}
			else {
				route.setLinks(l1, NetworkUtils.getLinks(network, "2 4 6"), l7);
			}
			leg.setRoute(route);

			plan.addLeg(leg);

			ActivityImpl act2 = (ActivityImpl) factory.createActivityFromLinkId("h", l7.getId());
			act2.setLinkId(l7.getId());
			plan.addActivity(act2);
			population.addPerson(p);
		}
	}

	private void createConfig(Config config) {
	// set scenario
		config.network().setInputFile(NETWORKFILE);
		config.plans().setInputFile(plansOut);

		// configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(6.0);
		// this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
		// homeParams.setOpeningTime(0);
		config.charyparNagelScoring().addActivityParams(homeParams);
		// set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0",
				"24:00:00");

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(1);
		config.controler().setLastIteration(iterations + iterations2);
		config.controler().setOutputDirectory(outputDirectory);


		// configure simulation and snapshot writing
		config.setQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotFormat("otfvis");
		config.getQSimConfigGroup().setSnapshotFile("cmcf.mvi");
		config.getQSimConfigGroup().setSnapshotPeriod(60.0);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		// configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(1));
		selectExp.setProbability(0.9);
		selectExp.setModuleName("ChangeExpBeta");
		config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(2));
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		config.strategy().addStrategySettings(reRoute);
	}


	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory factory = lanes.getFactory();
		//lanes for link 4
		LanesToLinkAssignment lanesForLink4 = factory.createLanesToLinkAssignment(id4);
		Lane link4lane1 = factory.createLane(id1);
		link4lane1.addToLinkId(id6);
		link4lane1.setNumberOfRepresentedLanes(3);
		lanesForLink4.addLane(link4lane1);
		lanes.addLanesToLinkAssignment(lanesForLink4);
		//lanes for link 5
		LanesToLinkAssignment lanesForLink5 = factory.createLanesToLinkAssignment(id5);
		Lane link5lane1 = factory.createLane(id1);
		link5lane1.setNumberOfRepresentedLanes(3);
		link5lane1.addToLinkId(id6);
		lanesForLink5.addLane(link5lane1);
		lanes.addLanesToLinkAssignment(lanesForLink5);
		return lanes;
	}


	private SignalSystems createSignalSystems(ScenarioImpl scenario) {
		SignalSystems systems = scenario.getSignalSystems();
		SignalSystemsFactory factory = systems.getFactory();
		//create the signal system no 1
		SignalSystemDefinition definition = factory.createSignalSystemDefinition(id1);
		systems.addSignalSystemDefinition(definition);

		//create signal group for traffic on link 4 on lane 1 with toLink 6
		SignalGroupDefinition groupLink4 = factory.createSignalGroupDefinition(id4, id1);
		groupLink4.addLaneId(id1);
		groupLink4.addToLinkId(id6);
		//assing the group to the system
		groupLink4.setSignalSystemDefinitionId(id1);
		//add the signalGroupDefinition to the container
		systems.addSignalGroupDefinition(groupLink4);

		//create signal group  with id no 2 for traffic on link 5 on lane 1 with toLink 6
		SignalGroupDefinition groupLink5 = factory.createSignalGroupDefinition(id5, id2);
		groupLink5.addLaneId(id1);
		groupLink5.addToLinkId(id6);
		//assing the group to the system
		groupLink5.setSignalSystemDefinitionId(id1);

		//add the signalGroupDefinition to the container
		systems.addSignalGroupDefinition(groupLink5);

		return systems;
	}

	private SignalSystemConfigurations createSignalSystemsConfig(
			ScenarioImpl scenario) {
		SignalSystemConfigurations configs = scenario.getSignalSystemConfigurations();
		SignalSystemConfigurationsFactory factory = configs.getFactory();

		SignalSystemConfiguration systemConfig = factory.createSignalSystemConfiguration(id1);
		AdaptiveSignalSystemControlInfo controlInfo = factory.createAdaptiveSignalSystemControlInfo();
		controlInfo.addSignalGroupId(id1);
		controlInfo.addSignalGroupId(id2);
		controlInfo.setAdaptiveControlerClass(controllerClass);
		systemConfig.setSignalSystemControlInfo(controlInfo);

		configs.getSignalSystemConfigurations().put(systemConfig.getSignalSystemId(), systemConfig);

		return configs;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new DaganzoScenarioGenerator().createScenario();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
