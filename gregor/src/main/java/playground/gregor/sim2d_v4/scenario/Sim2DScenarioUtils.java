/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DScenarioUtils.java
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

package playground.gregor.sim2d_v4.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.io.Sim2DEnvironmentReader02;

public abstract class Sim2DScenarioUtils {
	
	//TODO make scenario identifiable so we can have several environments  
	public static  Sim2DScenario loadSim2DScenario(Sim2DConfig conf) {
		Sim2DScenario scenario = new Sim2DScenario(conf);
		for (String envPath : conf.getSim2DEnvironmentPaths()){
			Sim2DEnvironment env = new Sim2DEnvironment();
			new Sim2DEnvironmentReader02(env, false).readFile(envPath);
			scenario.getSim2DEnvironments().add(env);
			for (Id i : conf.getSim2DEnvAccessorNodes(envPath)) {
				Id m = conf.getQSimNode(i);
				env.addAccessorNodeQSimNodeMapping(i,m);
			}
			String netPath = conf.getNetworkPath(envPath);
			if (netPath != null) { //not yet clear if this can be null, maybe it even must be null [gl dec 12]
				Config c = ConfigUtils.createConfig();
				Scenario sc = ScenarioUtils.createScenario(c);
				new MatsimNetworkReader(sc).readFile(netPath);
				Network net = sc.getNetwork();
				env.setNetwork(net);
			}
		}
		
		return scenario;
	}

}
