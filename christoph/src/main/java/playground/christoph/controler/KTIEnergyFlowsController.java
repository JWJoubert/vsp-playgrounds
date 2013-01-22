/* *********************************************************************** *
 * project: org.matsim.*
 * KTIEnergyFlowsController.java
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

package playground.christoph.controler;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.Module;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.energyflows.controller.EnergyFlowsController;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

public class KTIEnergyFlowsController extends EnergyFlowsController {

	final private static Logger log = Logger.getLogger(KTIEnergyFlowsController.class);
	
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final KtiConfigGroup ktiConfigGroup;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	
	public KTIEnergyFlowsController(String[] args) {
		super(args);

		/*
		 * Create the empty object. They are filled in the loadData() method.
		 */
		this.ktiConfigGroup = new KtiConfigGroup();
		this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(this.ktiConfigGroup);
	}
	
	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			
			/*
			 * The KTIConfigGroup is loaded as generic Module. We replace this
			 * generic object with a KtiConfigGroup object and copy all its parameter.
			 */
			Module module = this.config.getModule(KtiConfigGroup.GROUP_NAME);
			this.config.removeModule(KtiConfigGroup.GROUP_NAME);
			this.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
			
			for (Entry<String, String> entry : module.getParams().entrySet()) {
				this.ktiConfigGroup.addParam(entry.getKey(), entry.getValue());
			}
				
			/*
			 * Use KTI route factories.
			 */
			((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(this.getNetwork(), new PlanomatConfigGroup()));
			((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

			
			KtiScenarioLoaderImpl loader = new KtiScenarioLoaderImpl(this.scenarioData, this.plansCalcRouteKtiInfo, this.ktiConfigGroup);
			loader.loadScenario();
			if (this.config.scenario().isUseHouseholds()) {
				this.loadHouseholds();
			}
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
		}
		
		// connect facilities to links
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), (NetworkImpl) getNetwork());
	}
	
	private void loadHouseholds() {
		if ((this.scenarioData.getHouseholds() != null) && (this.config.households() != null) && (this.config.households().getInputFile() != null) ) {
			String hhFileName = this.config.households().getInputFile();
			log.info("loading households from " + hhFileName);
			new HouseholdsReaderV10(this.scenarioData.getHouseholds()).parse(hhFileName);
			log.info("households loaded.");
		}
		else {
			log.info("no households file set in config or feature disabled, not able to load anything");
		}
	}
	
	@Override
	protected void setUp() {

		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				this.scenarioData,
				this.ktiConfigGroup,
				this.getScenario().getScenarioElement(FacilityPenalties.class).getFacilityPenalties(),
				this.getFacilities());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.setTravelDisutilityFactory(costCalculatorFactory);

		super.setUp();
	}
	
	@Override
	protected void loadControlerListeners() {

		super.loadControlerListeners();

		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(this.getScenario().getScenarioElement(FacilityPenalties.class).getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm() {
		return this.ktiConfigGroup.isUsePlansCalcRouteKti() ?
				createKtiRoutingAlgorithm(
						this.createTravelCostCalculator(),
						this.getLinkTravelTimes()) :
				super.createRoutingAlgorithm();
	}

	private PlanAlgorithm createKtiRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		return new PlansCalcRouteKti(
					super.getConfig().plansCalcRoute(),
					super.network,
					travelCosts,
					travelTimes,
					super.getLeastCostPathCalculatorFactory(),
					((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(),
					this.plansCalcRouteKtiInfo);
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KTIFlowsController config-file rerouting-share");
			System.out.println();
		} else if (args.length != 2) {
			log.error("Unexpected number of input arguments!");
			log.error("Expected path to a config file (String) and rerouting share (double, 0.0 ... 1.0) for transit agents.");
			System.exit(0);
		} else {
			final KTIEnergyFlowsController controler = new KTIEnergyFlowsController(args);
			controler.run();
		}
		
		System.exit(0);
	}
}
