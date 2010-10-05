/* *********************************************************************** *
 * project: org.matsim.*
 * PSSControlerDumbCharging.java
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.EventReadControler;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.ARTEMISEnergyStateMaintainer_StartChargingUponArrival;

public class PSSControlerDumbCharging extends PSSControler {

	public PSSControlerDumbCharging(String configFilePath, ParametersPSFMutator parameterPSFMutator) {
		super(configFilePath, parameterPSFMutator);
	}

	public void runMATSimIterations() {

		// use the right Controler (read parameter
		Config config = new Config();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFilePath);
		String tempStringValue = config.findParam(ParametersPSF.getPSFModule(), "main.inputEventsForSimulationPath");
		if (tempStringValue != null) {
			// ATTENTION, this does not work at the moment, because the read link from the
			// event file is null and this causes some probelems in my handlers...
			controler = new EventReadControler(configFilePath,tempStringValue);
		} else {
			controler = new Controler(configFilePath);
		}

		
		
		ParametersPSF2.fleetInitializer=new DumbScenarioFleetInitializer();
		ParametersPSF2.energyConsumptionTable=new EnergyConsumptionTable(ParametersPSF2.pathToEnergyConsumptionTable);
		
		ParametersPSF2.energyStateMaintainer=new ARTEMISEnergyStateMaintainer_StartChargingUponArrival(ParametersPSF2.energyConsumptionTable);
		
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		
		
		
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		
		controler.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				ParametersPSF2.initVehicleFleet(event.getControler());
			}
		});
		

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(parameterPSFMutator);
		
		AfterSimulationListener afterSimulationListener = new AfterSimulationListener(logEnergyConsumption, logParkingTimes);
		controler.addControlerListener(afterSimulationListener);

		controler.run();

	}
	
}
