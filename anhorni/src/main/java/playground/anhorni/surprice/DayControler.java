/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.analysis.AgentAnalysisShutdownListener;
import playground.anhorni.surprice.analysis.ModeSharesControlerListener;
import playground.anhorni.surprice.scoring.SurpriceScoringFunctionFactory;
import playground.anhorni.surprice.scoring.SurpriceTravelCostCalculatorFactoryImpl;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	private ObjectAttributes preferences;
	private Random random;
		
	public DayControler(final Config config, AgentMemories memories, String day, ObjectAttributes preferences) {
		super(config);	
		super.setOverwriteFiles(true);
		this.memories = memories;	
		this.day = day;
		this.preferences = preferences;
	} 
			
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new SurpriceScoringFunctionFactory(
	  			this, this.config.planCalcScore(), this.network, this.memories, this.day, this.preferences);	  		
	}
	
	protected void setUp() {
		this.generateAlphaGammaTrip();
		SurpriceTravelCostCalculatorFactoryImpl costCalculatorFactory = new SurpriceTravelCostCalculatorFactoryImpl(this.day);
		this.setTravelDisutilityFactory(costCalculatorFactory);
		super.setUp();	
	}
	
	
//	@Override
//	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
//		
//		RoadPricingSchemeImpl scheme = (RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class);
//		
//		if (scheme.getType().equals("area")) {		
//		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
//		return new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts,
//				travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, 
//				(RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class));
//		}
//		else {
//			final TravelDisutilityFactory previousTravelCostCalculatorFactory = super.getTravelDisutilityFactory();
//			TravelDisutility costsIncludingTolls = new TravelDisutilityIncludingToll(previousTravelCostCalculatorFactory.createTravelDisutility(
//					travelTimes, this.config.planCalcScore()), 
//					(RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class));
//			return super.createRoutingAlgorithm(costsIncludingTolls, travelTimes);
//		}
//	}
	
	private void setTermination(double stoppingRate) {
		super.setTerminationCriterion(new TerminationCriterionScoreBased(stoppingRate, this.getScoreStats()));
	}
	
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
	  	this.addControlerListener(new Memorizer(this.memories, this.day));
	  	this.addControlerListener(new ModeSharesControlerListener("times"));
	  	this.addControlerListener(new ModeSharesControlerListener("distances"));  	
	  	this.addControlerListener(new AgentAnalysisShutdownListener(this.day, this.getControlerIO().getOutputPath()));
	  	
	  	if (Boolean.parseBoolean(this.config.findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {	
	  		this.addControlerListener(new RoadPricing(this.day));
		}
	  	double stoppingCriterionVal = Double.parseDouble(this.config.findParam(Surprice.SURPRICE_RUN, "stoppingCriterionVal"));
	  	if (stoppingCriterionVal > 0.0) {	
	  		this.setTermination(stoppingCriterionVal);
	  	}	  	
	}
	
	private void generateAlphaGammaTrip() {
		for (Person p : this.scenarioData.getPopulation().getPersons().values()) {
			this.random = new Random(Integer.parseInt(p.getId().toString()) + Surprice.days.indexOf(this.day) * 10000);
			
			for (int i = 0; i < 100; i++) {
				this.random.nextDouble();
			}
			
			double alphaTripRange = Double.parseDouble(this.getConfig().findParam(Surprice.SURPRICE_RUN, "alphaTripRange"));
			double r = this.random.nextDouble();
			double alphaTrip = alphaTripRange * (0.5 - r);	// tripRange * [-0.5 .. 0.5]
			double gammaTrip = -1.0 * alphaTrip;
			double alpha = (Double)this.preferences.getAttribute(p.getId().toString(), "alpha");
			double gamma = (Double)this.preferences.getAttribute(p.getId().toString(), "gamma");
			
			p.getCustomAttributes().put(day + ".alpha_tot", alpha + alphaTrip);
			p.getCustomAttributes().put(day + ".gamma_tot", gamma + gammaTrip);
		}
	}
}
