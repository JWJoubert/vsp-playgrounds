/* *********************************************************************** *
 * project: org.matsim.*
 * AgentInteractionScoringFunctionFactory.java
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

package scoring;

import java.util.HashSet;
import java.util.TreeMap;

import occupancy.FacilitiesOccupancyCalculator;
import occupancy.FacilityOccupancy;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class AgentInteractionScoringFunctionFactory extends CharyparNagelScoringFunctionFactory {

	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	private ObjectAttributes attributes;
	private final CharyparNagelScoringParameters params;
	private final ActivityFacilities facilities;
	private Network network;
	private double scaleNumberOfPersons;

	private final Controler controler;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private HashSet<String> flexibleTypes;
	private Config config2;

	public AgentInteractionScoringFunctionFactory(final Controler controler,
			final Config config2, final PlanCalcScoreConfigGroup config,
			final ActivityFacilities facilities, Network network,
			double scaleNumberOfPersons,TreeMap<Id, FacilityOccupancy> facilityOccupancies,
			ObjectAttributes attributes,
			ScaleEpsilon scaleEpsilon,
			ActTypeConverter actTypeConverter, HashSet<String> flexibleTypes) {

		super(config, network);
		this.params = new CharyparNagelScoringParameters(config);
		this.facilities = facilities;
		this.network = network;
		this.scaleNumberOfPersons = scaleNumberOfPersons;
		this.facilityOccupancies = facilityOccupancies;
		this.attributes = attributes;

		this.controler = controler;
		this.scaleEpsilon = scaleEpsilon;
		this.actTypeConverter = actTypeConverter;		
		this.flexibleTypes = flexibleTypes;	
		this.config2 = config2;
		this.createObjectAttributes(Long.parseLong(config2.locationchoice().getRandomSeed()));
	}

	private void createObjectAttributes(long seed) {
		this.facilitiesKValues = new ObjectAttributes();
		this.personsKValues = new ObjectAttributes();

		String pkValues = this.config2.locationchoice().getpkValuesFile();
		if (!pkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personsKValues);
			try {
				attributesReader.parse(pkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
			}
		}
		else {
			this.computeAttributes(seed);
		}
		String fkValues = this.config2.locationchoice().getfkValuesFile();
		if (!fkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.facilitiesKValues);
			try {
				attributesReader.parse(fkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
			}
		}
		else {
			this.computeAttributes(seed);
		}
	}

	private void computeAttributes(long seed) {
//		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(
//				seed, this.controler.getScenario(), this.scaleEpsilon, this.actTypeConverter, this.flexibleTypes);
//		computer.assignKValues();
//
//		this.personsKValues = computer.getPersonsKValues();
//		this.facilitiesKValues = computer.getFacilitiesKValues();
		
		throw new RuntimeException("Hey Alex, sorry for that. I will show you how to integrate LC with a listener now. " +
				"Large refactoring has been done in the LC module. ah feb'13");
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		AgentInteractionScoringFunction scoringFunction = new AgentInteractionScoringFunction(plan, params,
				facilityOccupancies, this.facilities, this.attributes, this.scaleNumberOfPersons
				,this.controler.getConfig(),this.facilitiesKValues, this.personsKValues, this.scaleEpsilon);

		scoringFunctionAccumulator.addScoringFunction(scoringFunction);

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
		//scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}

}