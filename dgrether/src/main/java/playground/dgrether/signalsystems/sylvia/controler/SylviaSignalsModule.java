/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package playground.dgrether.signalsystems.sylvia.controler;

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.controler.SignalsControllerListener;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Provides;

import playground.dgrether.signalsystems.LinkSensorManager;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaSignalModelFactory;

/**
 * Add this module if you want to simulate signals with sylvia. 
 * It also works with fixed time signals, i.e. without sylvia (with the default controller identifier in the signal control file) 
 * and without signals.
 * 
 * @author tthunig
 */
public class SylviaSignalsModule extends AbstractModule {

	private boolean alwaysSameMobsimSeed = false;
	private DgSylviaConfig sylviaConfig = new DgSylviaConfig();
	
	@Override
	public void install() {
		if (alwaysSameMobsimSeed) {
			MobsimInitializedListener randomSeedResetter = new MobsimInitializedListener() {
				@Override
				public void notifyMobsimInitialized(MobsimInitializedEvent e) {
					// make sure it is same random seed every time
					MatsimRandom.reset(0);
				}
			};
			this.addMobsimListenerBinding().toInstance(randomSeedResetter);
		}
		
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// sylvia specific bindings
			bind(DgSylviaConfig.class).toInstance(sylviaConfig);
			bind(SignalModelFactory.class).to(DgSylviaSignalModelFactory.class);
			
			// bindings for sensor based signals
			bind(LinkSensorManager.class).asEagerSingleton();
            addEventHandlerBinding().to(LinkSensorManager.class);
            bind(SignalsControllerListener.class).to(DgSylviaSignalControlerListener.class);
            addControlerListenerBinding().to(SignalsControllerListener.class);
            
			// general signal bindings
			bind(FromDataBuilder.class);
			bind(QSimSignalEngine.class);
            addMobsimListenerBinding().to(QSimSignalEngine.class);
            
            // bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
            addEventHandlerBinding().to(SignalEvents2ViaCSVWriter.class);
            addControlerListenerBinding().to(SignalEvents2ViaCSVWriter.class);
        }
	}
	
	@Provides SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, FromDataBuilder modelBuilder) {	
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		signalManager.resetModel(replanningContext.getIteration());		
		return signalManager;
    }

	public void setAlwaysSameMobsimSeed(boolean alwaysSameMobsimSeed) {
		this.alwaysSameMobsimSeed = alwaysSameMobsimSeed;
	}

	public void setSylviaConfig(DgSylviaConfig sylviaConfig) {
		this.sylviaConfig = sylviaConfig;
	}

}
