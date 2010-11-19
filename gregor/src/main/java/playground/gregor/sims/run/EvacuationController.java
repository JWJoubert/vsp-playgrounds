/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimControllerII.java
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
package playground.gregor.sims.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetFromNetcdfGenerator;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.EvacuationPlansGenerator;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.NetworkChangeEventsFromNetcdf;
import org.matsim.evacuation.config.EvacuationConfigGroup;
import org.matsim.evacuation.config.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.shelters.signalsystems.ShelterDoorBlockerSetup;
import org.matsim.evacuation.shelters.signalsystems.ShelterInputCounterSignalSystems;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLinkII;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.gregor.sims.config.ShelterConfigGroup;
import playground.gregor.sims.config.ShelterConfigGroup.InitialAssignment;
import playground.gregor.sims.config.ShelterConfigGroup.Version;
import playground.gregor.sims.shelters.assignment.EvacuationShelterNetLoaderForShelterAllocation;
import playground.gregor.sims.shelters.assignment.GreedyShelterAllocator;
import playground.gregor.sims.shelters.assignment.RandomShelterAllocator;
import playground.gregor.sims.shelters.assignment.ShelterAssignmentRePlanner;
import playground.gregor.sims.shelters.assignment.ShelterAssignmentSimulatedAnnealingRePlannerII;
import playground.gregor.sims.shelters.assignment.ShelterCapacityRePlanner;
import playground.gregor.sims.shelters.assignment.ShelterCounter;

public class EvacuationController extends Controler {

	final private static Logger log = Logger.getLogger(EvacuationController.class);

	private List<Building> buildings;

	private List<FloodingReader> netcdfReaders = null;

	private HashMap<Id, Building> shelterLinkMapping = null;

	PluggableTravelCostCalculator pluggableTravelCost = null;

	private EvacuationConfigGroup ec;

	private ShelterConfigGroup sc = null;

	private EvacuationShelterNetLoaderForShelterAllocation esnl;

	public EvacuationController(String[] args) {
		super(args);
		setOverwriteFiles(true);
		this.config.scenario().setUseSignalSystems(false);
		this.config.scenario().setUseLanes(true);
		this.config.setQSimConfigGroup(new QSimConfigGroup());
	}

	@Override
	protected void setUp() {

		super.setUp();

		if (this.ec.isLoadShelters() && this.sc == null) {
			loadShelterSignalSystems();
		}

		if (this.ec.isSocialCostOptimization()) {
			initSocialCostOptimization();
		}

		if (this.ec.isRiskMinimization()) {
			initRiskMinimization();
		}

		if (this.sc != null) {
			initShelterAssignment();
		}
		unloadNetcdfReaders();

	}

	/**
	 * 
	 */
	private void initShelterAssignment() {
		initPluggableTravelCostCalculator();
		Building b = new Building(new IdImpl("el1"), 0, 0, 0, 1, 1000000, 10000, 1, null);
		this.shelterLinkMapping.put(new IdImpl("el1"), b);
		this.buildings.add(b);
		if (this.sc.getAssignmentVersion() == Version.ICEC2010) {
			ShelterCounter sc = new ShelterCounter(this.scenarioData.getNetwork(), this.shelterLinkMapping);
			if (this.sc.isCapacityAdaption()) {
				ShelterCapacityRePlanner scap = new ShelterCapacityRePlanner(getScenario(), this.pluggableTravelCost, getTravelTimeCalculator(), this.buildings, sc);
				addControlerListener(scap);
			}
			ShelterAssignmentRePlanner sARP = new ShelterAssignmentRePlanner(getScenario(), this.pluggableTravelCost, getTravelTimeCalculator(), this.buildings, sc);
			addControlerListener(sARP);
		} else if (this.sc.getAssignmentVersion() == Version.SA) {
			ShelterAssignmentSimulatedAnnealingRePlannerII srp = new ShelterAssignmentSimulatedAnnealingRePlannerII(getScenario(), this.pluggableTravelCost, getTravelTimeCalculator(), this.shelterLinkMapping);
			addControlerListener(srp);
			if (this.sc.isCapacityAdaption()) {
				throw new RuntimeException("Capacity adaption has not yet been implemented for simulated annealing approach!");
			}
		}
	}

	/**
	 * 
	 */
	private void initSheltersConfigGroup() {
		Module m = this.config.getModule("shelters");
		ShelterConfigGroup sc = new ShelterConfigGroup(m);
		this.config.getModules().put("shelters", sc);
		this.sc = sc;
	}

	private void initSocialCostOptimization() {
		initPluggableTravelCostCalculator();

		if (this.ec.useSocialCostCalculatorII()) {
			SocialCostCalculatorSingleLinkII sc = new SocialCostCalculatorSingleLinkII(this.network, getConfig().travelTimeCalculator().getTraveltimeBinSize(), getEvents());
			this.pluggableTravelCost.addTravelCost(sc);
			this.events.addHandler(sc);
			this.strategyManager = loadStrategyManager();
			addControlerListener(sc);
		} else {

			SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network, getConfig().travelTimeCalculator().getTraveltimeBinSize(), getEvents());
			this.pluggableTravelCost.addTravelCost(sc);
			this.events.addHandler(sc);
			this.strategyManager = loadStrategyManager();
			addControlerListener(sc);
		}
	}

	private void initRiskMinimization() {
		initPluggableTravelCostCalculator();
		loadNetcdfReaders();

		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, this.netcdfReaders, getEvents(), this.ec.getBufferSize());
		this.pluggableTravelCost.addTravelCost(rc);
		this.events.addHandler(rc);
	}

	private void initPluggableTravelCostCalculator() {
		if (this.pluggableTravelCost == null) {
			if (this.travelTimeCalculator == null) {
				this.travelTimeCalculator = getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
			}
			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
			setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

				// This is thread-safe because pluggableTravelCost is
				// thread-safe.

				@Override
				public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup) {
					return EvacuationController.this.pluggableTravelCost;
				}

			});
		}
	}

	private void loadShelterSignalSystems() {
		this.config.network().setLaneDefinitionsFile("nullnull");

		ShelterInputCounterSignalSystems sic = new ShelterInputCounterSignalSystems(this.scenarioData, this.shelterLinkMapping);
		this.events.addHandler(sic);
		getQueueSimulationListener().add(sic);

		ShelterDoorBlockerSetup shelterSetup = new ShelterDoorBlockerSetup(sic);
		final SignalSystemsManager signalManager = shelterSetup.createSignalManager(getScenario());
		signalManager.setEventsManager(this.events);
		getQueueSimulationListener().add(new QSimSignalEngine(signalManager));

		addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				signalManager.resetModel(event.getIteration());
			}
		});
	}

	private void unloadNetcdfReaders() {
		this.netcdfReaders = null;
		log.info("netcdf readers destroyed");
	}

	private void loadNetcdfReaders() {
		if (this.netcdfReaders != null) {
			return;
		}
		log.info("loading netcdf readers");
		int count = this.ec.getSWWFileCount();
		if (count <= 0) {
			return;
		}
		this.netcdfReaders = new ArrayList<FloodingReader>();
		double offsetEast = this.ec.getSWWOffsetEast();
		double offsetNorth = this.ec.getSWWOffsetNorth();
		for (int i = 0; i < count; i++) {
			String netcdf = this.ec.getSWWRoot() + "/" + this.ec.getSWWFilePrefix() + i + this.ec.getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setOffset(offsetEast, offsetNorth);
			this.netcdfReaders.add(fr);
		}
		log.info("done.");
	}

	private void loadNetWorkChangeEvents(NetworkImpl net) {
		loadNetcdfReaders();
		if (this.netcdfReaders == null) {
			throw new RuntimeException("No netcdf reader could be loaded!");
		} else if (!net.getFactory().isTimeVariant()) {
			throw new RuntimeException("Network layer is not time variant!");
		} else if (net.getNetworkChangeEvents() != null) {
			throw new RuntimeException("Network change events allready loaded!");
		}
		List<NetworkChangeEvent> events = new NetworkChangeEventsFromNetcdf(this.netcdfReaders, this.scenarioData).createChangeEvents();
		net.setNetworkChangeEvents(events);
	}

	@Override
	protected void loadData() {
		super.loadData();

		Module m = this.config.getModule("evacuation");
		this.ec = new EvacuationConfigGroup(m);
		this.config.getModules().put("evacuation", this.ec);
		EvacuationScenario sc = this.ec.getEvacuationScanrio();

		if (sc == EvacuationScenario.from_file) {
			return;
		}

		// network
		NetworkImpl net = this.scenarioData.getNetwork();

		if (this.ec.isLoadShelters()) {
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
			}
			this.esnl = new EvacuationShelterNetLoaderForShelterAllocation(this.buildings, this.scenarioData, this.netcdfReaders);
			net = this.esnl.getNetwork();
			this.shelterLinkMapping = this.esnl.getShelterLinkMapping();

		} else {
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
				new EvacuationNetFromNetcdfGenerator(net, this.scenarioData.getConfig(), this.netcdfReaders).run();
			} else {
				new EvacuationNetGenerator(net, this.config).run();
			}
		}

		if (this.scenarioData.getConfig().network().isTimeVariantNetwork() && this.ec.isGenerateEvacNetFromSWWFile()) {
			loadNetWorkChangeEvents(net);
		}

		if (this.config.getModule("shelters") != null) {
			loadShelterPopulation();
		} else {
			loadNormalPopulation();
			if (this.ec.isLoadShelters()) {
				this.esnl.generateShelterLinks();
			}
		}

		this.population = this.scenarioData.getPopulation();

	}

	/**
	 * 
	 */
	private void loadNormalPopulation() {
		if (this.ec.isLoadPopulationFromShapeFile()) {
			if (this.scenarioData.getPopulation().getPersons().size() > 0) {
				throw new RuntimeException("Population already loaded. In order to load population from shape file, the population input file paramter in the population section of the config.xml must not be set!");
			}
			// population
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}

			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				new EvacuationPopulationFromShapeFileLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.netcdfReaders).getPopulation();
			} else {
				new EvacuationPopulationFromShapeFileLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData).getPopulation();
			}
		} else {
			if (this.ec.getEvacuationScanrio() != EvacuationScenario.night) {
				throw new RuntimeException("Evacuation simulation from plans file so far only works for the night scenario.");
			}
			new EvacuationPlansGenerator(this.population, this.network, this.network.getLinks().get(new IdImpl("el1"))).run();
		}

	}

	/**
	 * 
	 */
	private void loadShelterPopulation() {
		initSheltersConfigGroup();
		if (this.ec.isLoadPopulationFromShapeFile()) {
			if (this.scenarioData.getPopulation().getPersons().size() > 0) {
				throw new RuntimeException("Population already loaded. In order to load population from shape file, the population input file paramter in the population section of the config.xml must not be set!");
			}
			// population
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}

			if (this.sc.getInitialAssignment() == InitialAssignment.greedy) {
				if (this.ec.isGenerateEvacNetFromSWWFile()) {
					new GreedyShelterAllocator(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.esnl, this.netcdfReaders).getPopulation();
				} else {
					new GreedyShelterAllocator(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.esnl, null).getPopulation();
				}
			} else if (this.sc.getInitialAssignment() == InitialAssignment.random) {
				if (this.ec.isGenerateEvacNetFromSWWFile()) {
					new RandomShelterAllocator(this.buildings, this.scenarioData, this.esnl, this.netcdfReaders).getPopulation();
				} else {
					new RandomShelterAllocator(this.buildings, this.scenarioData, this.esnl, null).getPopulation();
				}
			}
		} else {
			throw new RuntimeException("This does not work!");
		}
	}

	public static void main(final String[] args) {
		final Controler controler = new EvacuationController(args);
		controler.run();
		System.exit(0);
	}

}
