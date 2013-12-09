/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.southafrica.population.freight;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class RunNationalFreight {
	private final static Logger LOG = Logger.getLogger(RunNationalFreight.class);
	
	
	private static final class DigicorePlanStrategyModule implements
			PlanStrategyModule {
		
		public DigicorePlanStrategyModule() {

		}
		
		
		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handlePlan(Plan plan) {
			// TODO Auto-generated method stub
			LOG.info("   ====> Woopie: plan handled.");
		}

		@Override
		public void finishReplanning() {
			// TODO Auto-generated method stub

		}
	}

	private static String NETWORK = "/Users/jwjoubert/Documents/workspace/Data-southAfrica/network/southAfrica_20131202_coarseNetwork_clean.xml.gz";
	private static String POPULATION = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/5000/nationalFreight_5000.xml.gz";
	private static String POPULATION_ATTR = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/5000/nationalFreight_Attributes.xml.gz";
	private static String OUTPUT_DIRECTORY = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/5000/output/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunNationalFreight.class.toString(), args);
		if(args.length == 4){
			NETWORK = args[0];
			POPULATION = args[1];
			POPULATION_ATTR = args[2];
			OUTPUT_DIRECTORY = args[3];
		} else{
			LOG.warn("None, or insufficient run arguments passed. Reverts back to defaults.");
		}
		
		/* Config stuff */
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(OUTPUT_DIRECTORY);
		config.controler().setLastIteration(5);
		config.controler().setWriteEventsInterval(1);
		
		config.network().setInputFile(NETWORK);
		config.plans().setInputFile(POPULATION);
		config.plans().setInputPersonAttributeFile(POPULATION_ATTR);
		
		config.global().setCoordinateSystem("WGS84_SA_Albers");
		
		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		
		String[] modes ={"commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		
			/* PlanCalcScore */
		ActivityParams major = new ActivityParams("major");
		major.setTypicalDuration(10*3600);
		config.planCalcScore().addActivityParams(major);

		ActivityParams minor = new ActivityParams("minor");
		minor.setTypicalDuration(1880);
		config.planCalcScore().addActivityParams(minor);
		
			/* Generic strategy */
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
//		StrategySettings changeExpBetaStrategySettings = new StrategySettings(new IdImpl("1"));
		changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setProbability(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			/* Subpopulation strategy */
		StrategySettings commercialStrategy = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		commercialStrategy.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
		commercialStrategy.setProbability(0.9);
		commercialStrategy.setSubpopulation("commercial");
		config.strategy().addStrategySettings(commercialStrategy);
		
		/* Scenario stuff */
		Scenario sc = ScenarioUtils.loadScenario(config);
		config.scenario().setUseVehicles(true);
		
		/* Set the population as "subpopulation", and create a vehicle for each. */
		Vehicles vehicles = ((ScenarioImpl)sc).getVehicles();
		VehicleType truckType = new VehicleTypeImpl(new IdImpl("commercial"));
		truckType.setMaximumVelocity(100./3.6);
		truckType.setLength(18.);
		vehicles.getVehicleTypes().put(truckType.getId(), truckType);
		
		for(Person person : sc.getPopulation().getPersons().values()){
			/* Subpopulation. */
			sc.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), config.plans().getSubpopulationAttributeName(), "commercial");
			
			/* Vehicles */
			Vehicle truck = VehicleUtils.getFactory().createVehicle(person.getId(), truckType);
			vehicles.getVehicles().put(person.getId(), truck);
		}
		
		/* Run the controler */
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		
		PlanStrategyFactory planStrategyFactory = new PlanStrategyFactory() {

			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan>() );
				strategy.addStrategyModule(new DigicorePlanStrategyModule());
				return strategy;
			}
		};
		
		controler.addPlanStrategyFactory("Digicore1", planStrategyFactory );
		
		controler.run();
		Header.printFooter();
	}

}
