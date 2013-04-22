/* *********************************************************************** *
 * project: org.matsim.*
 * DgKSNetMatsimNetFacade
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;


/**
 * @author dgrether
 *
 */
public class DgKSNet2MatsimNet  {

	private static final Logger log = Logger.getLogger(DgKSNet2MatsimNet.class);
	
	public Network convertNetwork(DgKSNetwork ksNet){
		Network network = NetworkImpl.createNetwork();
		log.info("Converting streets...");
		for (DgStreet street : ksNet.getStreets().values()){
			this.convertStreet(street, network);
		}
		for (DgCrossing crossing : ksNet.getCrossings().values()){
			for (DgStreet street : crossing.getLights().values()){
				this.convertStreet(street, network);	
			}
		}
		return network;
	}
	
	private void convertStreet(DgStreet street, Network net){
		DgCrossingNode fromNode = street.getFromNode();
		DgCrossingNode toNode = street.getToNode();
		if (fromNode.getId().equals(toNode.getId())){
			log.warn("found street with toNode == fromNode...");
			return;
		}
		Node from = net.getNodes().get(fromNode.getId());
		if (from == null) {
			from = net.getFactory().createNode(fromNode.getId(), fromNode.getCoordinate());
			net.addNode(from);
		}
		Node to = net.getNodes().get(toNode.getId());
		if (to == null) {
			to = net.getFactory().createNode(toNode.getId(), toNode.getCoordinate());
			net.addNode(to);
		}
		Link link = net.getFactory().createLink(street.getId(), from, to);
		net.addLink(link);
	}
	
}
