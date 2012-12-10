package playground.pieter.network.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NodeImpl;

public class ClusterNode implements Node {
	private NodeImpl node;
	private NodeCluster rootCluster;
	
	public ClusterNode(NodeImpl n ) {
		this.node = n;
	}
	

	public int hashCode() {
		return node.hashCode();
	}

	public final void setOrigId(String id) {
		node.setOrigId(id);
	}

	public final void setType(String type) {
		node.setType(type);
	}

	public boolean equals(Object obj) {
		return node.equals(obj);
	}

	public final boolean addInLink(Link inlink) {
		return node.addInLink(inlink);
	}

	public final boolean addOutLink(Link outlink) {
		return node.addOutLink(outlink);
	}

	public void setCoord(Coord coord) {
		node.setCoord(coord);
	}

	public final void removeInLink(Link inlink) {
		node.removeInLink(inlink);
	}

	public final void removeOutLink(Link outlink) {
		node.removeOutLink(outlink);
	}

	public final String getOrigId() {
		return node.getOrigId();
	}

	public final String getType() {
		return node.getType();
	}

	public final Map<Id, ? extends Link> getIncidentLinks() {
		return node.getIncidentLinks();
	}

	public final Map<Id, ? extends Node> getInNodes() {
		return node.getInNodes();
	}

	public final Map<Id, ? extends Node> getOutNodes() {
		return node.getOutNodes();
	}

	public final Map<Id, ? extends Node> getIncidentNodes() {
		return node.getIncidentNodes();
	}

	public Map<Id, ? extends Link> getInLinks() {
		return node.getInLinks();
	}

	public Map<Id, ? extends Link> getOutLinks() {
		return node.getOutLinks();
	}

	public Coord getCoord() {
		return node.getCoord();
	}

	public Id getId() {
		return node.getId();
	}

	public String toString() {
		return node.toString();
	}
	

	/**
	 * Once a cluster is chosen by the algorithm, it needs to be set to the new root
	 * @param root
	 */
	public void setNewRoot(NodeCluster root){
		this.rootCluster = root;
	}


}
