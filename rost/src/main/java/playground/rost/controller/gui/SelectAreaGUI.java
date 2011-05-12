/******************************************************************************
 *project: org.matsim.*
 * SelectAreaGUI.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.rost.controller.map.SelectBorderMap;
import playground.rost.controller.vismodule.VisModuleContainerImpl;
import playground.rost.controller.vismodule.implementations.AreaNodeVisModule;
import playground.rost.controller.vismodule.implementations.BorderVisModule;
import playground.rost.controller.vismodule.implementations.LinkVisModule;
import playground.rost.controller.vismodule.implementations.MarkNodeVisModule;
import playground.rost.graph.AreaExtractor;
import playground.rost.graph.Border;
import playground.rost.graph.BoundingBox;
import playground.rost.util.PathTracker;

public class SelectAreaGUI extends AbstractBasicMapGUIImpl {

	protected NetworkImpl network;
	protected JButton btnExtract;
	protected Border border;
	
	protected void extractArea()
	{
		AreaExtractor.extractNetworkAndWriteIntoFile(border, network, PathTracker.resolve("matExtract"), PathTracker.resolve("evacArea"));
		map.UIChange();

	}
	
	public SelectAreaGUI(NetworkImpl network) {
		super("Select Area");
		this.network = network;
		
		SelectBorderMap borderMap = new SelectBorderMap(network);
		this.border = borderMap.border;
		this.map = borderMap;
		
		this.vMContainer = new VisModuleContainerImpl(this);
		this.vMContainer.addVisModule(new AreaNodeVisModule(vMContainer, border, network));
		this.vMContainer.addVisModule(new LinkVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new BorderVisModule(vMContainer, border));
		this.vMContainer.addVisModule(new MarkNodeVisModule(vMContainer, network));
		
		btnExtract = new JButton("Extract Area!");
		this.ownContainer.add(btnExtract);
		btnExtract.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				extractArea();
			}
		});
		BoundingBox bBox = new BoundingBox();
		bBox.run(network);
		this.map.setBoundingBox(bBox);
		this.buildUI();
		this.map.addMapPaintCallback(this.vMContainer);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readNetworkAndShowGUI();
	}
	
	public static SelectAreaGUI readNetworkAndShowGUI()
	{
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(scenario);
		nReader.parse(PathTracker.resolve("matMap"));
		return (new SelectAreaGUI(network));
	}

}
