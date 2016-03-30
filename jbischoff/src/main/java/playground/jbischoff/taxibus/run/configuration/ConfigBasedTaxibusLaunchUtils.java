/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package playground.jbischoff.taxibus.run.configuration;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.taxi.multirun.VrpLauncherUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.population.algorithms.PermissibleModesCalculator;

import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LinesUtils;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusPassengerOrderManager;
import playground.jbischoff.taxibus.run.sim.TaxibusPermissibleModesCalculatorImpl;
import playground.jbischoff.taxibus.run.sim.TaxibusQSimProvider;
import playground.jbischoff.taxibus.run.sim.TaxibusServiceRoutingModule;
import playground.jbischoff.taxibus.scenario.strategies.TaxibusAndWOBScenarioPermissibleModesCalculator;

/**
 * @author jbischoff
 *
 */
public class ConfigBasedTaxibusLaunchUtils {
		private MatsimVrpContextImpl context;
		private Controler controler;
		
		
		
		public ConfigBasedTaxibusLaunchUtils(Controler controler) {
			this.controler = controler;
			
		}
		
	 
	public  void initiateTaxibusses(boolean wobCase){
		//this is done exactly once per simulation
		
		
		final TaxibusConfigGroup tbcg = (TaxibusConfigGroup) controler.getScenario().getConfig().getModule("taxibusConfig");
      	context = new MatsimVrpContextImpl();
		context.setScenario(controler.getScenario());
        VrpData vrpData = new VrpDataImpl();
        new VehicleReader(context.getScenario().getNetwork(), vrpData).parse(tbcg.getVehiclesFile());

		
		
		final PermissibleModesCalculator taxibusPermissibleModesCalculator;
		
		final LineDispatcher dispatcher = LinesUtils.createLineDispatcher(tbcg.getLinesFile(), tbcg.getZonesXmlFile(), tbcg.getZonesShpFile(),context,tbcg);	
		final TaxibusPassengerOrderManager orderManager = new TaxibusPassengerOrderManager();
		
		final String[] availableModes = controler.getScenario().getConfig().subtourModeChoice().getModes();
		final String[] chainBasedModes = controler.getScenario().getConfig().subtourModeChoice().getChainBasedModes();
		
		if (wobCase){
			taxibusPermissibleModesCalculator = new TaxibusAndWOBScenarioPermissibleModesCalculator(availableModes, dispatcher, controler.getScenario());
		}
		else {
			taxibusPermissibleModesCalculator = new TaxibusPermissibleModesCalculatorImpl(availableModes, dispatcher);
		}
		
		context.setVrpData(vrpData);	 
           
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
//				addPlanStrategyBinding(DefaultStrategy.SubtourModeChoice.name()+"_new").toProvider(new javax.inject.Provider<PlanStrategy>(){
//					@Override
//					public PlanStrategy get() {
//						SubtourModeChoice choice  = new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false);
//						choice.setPermissibleModesCalculator(taxibusPermissibleModesCalculator);
//						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
//						builder.addStrategyModule(new ReRoute(controler.getScenario()));
//						builder.addStrategyModule(choice);
//						return builder.build();
//					}
//				});
				addEventHandlerBinding().toInstance(dispatcher);
				addEventHandlerBinding().toInstance(orderManager);
				bindMobsim().toProvider(TaxibusQSimProvider.class);
				addRoutingModuleBinding("taxibus").toInstance(new TaxibusServiceRoutingModule(controler));
				bind(TaxibusPassengerOrderManager.class).toInstance(orderManager);
				bind(MatsimVrpContext.class).toInstance(context);
				bind(LineDispatcher.class).toInstance(dispatcher);

			}
		});
		
		
		
		
	}


	public void initiateTaxibusses() {
		initiateTaxibusses(false);	
	} 
}
