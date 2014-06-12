/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich1pct.java
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
package playground.benjamin.scenarios.munich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author benjamin
 *
 */
public class RunInternalizationMunich {

	static String configFile;
	static String emissionCostFactor;
	static String emissionEfficiencyFactor;
	static String considerCO2Costs;


	public static void main(String[] args) {
//		configFile = "../../detailedEval/internalization/munich1pct/input/config_munich_1pct.xml";
//		emissionCostFactor = "1.0";
//		emissionEfficiencyFactor = "1.0";
//		considerCO2Costs = "true";

		configFile = args[0];
		emissionCostFactor = args[1];
		emissionEfficiencyFactor = args[2];
		considerCO2Costs = args[3];

		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);

		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

		EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		controler.setTravelDisutilityFactory(emissionTducf);

		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.run();
	}
}