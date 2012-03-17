/* *********************************************************************** *
 * project: org.matsim.*
 * TestCellKnowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge.container;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityCalculator;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;

public class TestCellKnowledge {

	private final static Logger log = Logger.getLogger(TestCellKnowledge.class);

	private final ScenarioImpl scenario;
	private Config config;
	private final Person person;
	private SelectNodesDijkstra selectNodesDijkstra;
	private Map<Id, Node> nodesMap;
	private CellKnowledge cellKnowledge;
	private final CellNetworkMapping cellNetworkMapping;
	private CellKnowledgeCreator createCellKnowledge;

	private final String configFileName = "mysimulations/kt-zurich/config.xml";
	private final String dtdFileName = null;
	private final String networkFile = "mysimulations/kt-zurich/input/network.xml";
	private final String populationFile = "mysimulations/kt-zurich/input/plans.xml";
	//private final String networkFile = "D:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
	//private final String networkFile = "D:/Master_Thesis_HLI/Workspace/myMATSIM/mysimulations/kt-zurich/networks/ivtch-zh-cut/network.xml";


	public static void main(final String[] args)
	{
		new TestCellKnowledge();
	}

	public TestCellKnowledge()
	{
		DBConnectionTool dbct = new DBConnectionTool();
		dbct.connect();

		loadConfig();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
		loadNetwork();
		loadPopulation();

		log.info("Network size: " + this.scenario.getNetwork().getLinks().size());
		log.info("Population size: " + this.scenario.getPopulation().getPersons().size());

		//Person person = population.getPersons().values().iterator().next();
		this.person = this.scenario.getPopulation().getPersons().get(new IdImpl(100000));
		log.info("Person: " + this.person);
		log.info("ID: " + this.person.getId());

		initNodeSelector();
		createKnownNodes();
		log.info("Found known Nodes: " + this.nodesMap.size());

		this.cellNetworkMapping = new CellNetworkMapping((NetworkImpl) this.scenario.getNetwork());
		this.cellNetworkMapping.createMapping();

		CreateCellKnowledge();

		log.info("included Nodes in CellKnowledge: " + this.cellKnowledge.getKnownNodes().size());
		this.cellKnowledge.findFullCells();

/*
		long memUsage;
		memUsage = calculateMemoryUsage(cellKnowledge);
		System.out.println("CellKnowledge took " + memUsage + " bytes of memory");

		memUsage = calculateMemoryUsage(nodesMap);
		System.out.println("NodesMap took " + memUsage + " bytes of memory");
*/
	}

	private void CreateCellKnowledge()
	{
		this.createCellKnowledge = new CellKnowledgeCreator(this.cellNetworkMapping);
		this.cellKnowledge = this.createCellKnowledge.createCellKnowledge(this.nodesMap);
	}

	private void createKnownNodes()
	{
		Plan plan = this.person.getSelectedPlan();

		this.nodesMap = new TreeMap<Id, Node>();

		// get all acts of the selected plan
		ArrayList<Activity> acts = new ArrayList<Activity>();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				acts.add((Activity) pe);
			}
		}

		for(int j = 1; j < acts.size(); j++)
		{
			Node startNode = this.scenario.getNetwork().getLinks().get(acts.get(j-1).getLinkId()).getToNode();
			Node endNode = this.scenario.getNetwork().getLinks().get(acts.get(j).getLinkId()).getFromNode();

			this.selectNodesDijkstra.setStartNode(startNode);
			this.selectNodesDijkstra.setEndNode(endNode);

			this.selectNodesDijkstra.addNodesToMap(this.nodesMap);
		}
	}

	private void initNodeSelector()
	{
		this.selectNodesDijkstra = new SelectNodesDijkstra(this.scenario.getNetwork());
		this.selectNodesDijkstra.setCostCalculator(new OnlyTimeDependentTravelDisutilityCalculator(null));
		this.selectNodesDijkstra.setCostFactor(20.0);
	}

	private void loadNetwork()
	{
		new MatsimNetworkReader(this.scenario).readFile(this.networkFile);
		log.info("Loading Network ... done");
	}

	private void loadPopulation()
	{
		new MatsimPopulationReader(this.scenario).readFile(this.populationFile);
		log.info("Loading Population ... done");
	}

	private void loadConfig()
	{
		this.config = new Config();
		this.config.addCoreModules();
		new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
		log.info("Loading Config ... done");
	}
/*
	private long calculateMemoryUsage(Object object)
	{
	    System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();

		long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		object = null;

	    System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();

		long mem0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		return mem1 - mem0;
	 }
*/
}
