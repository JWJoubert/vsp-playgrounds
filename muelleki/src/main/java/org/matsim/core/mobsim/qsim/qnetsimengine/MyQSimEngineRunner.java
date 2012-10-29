/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineRunner.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;

public class MyQSimEngineRunner extends NetElementActivator implements Runnable {
	final Logger log = Logger.getLogger(MyQSimEngineRunner.class);

	private double time = 0.0;
	private boolean simulateAllNodes = false;
	private boolean simulateAllLinks = false;
	private boolean useNodeArray = QNetsimEngine.useNodeArray;

	private volatile boolean simulationRunning = true;

	private final CyclicBarrier startBarrier;
	private final CyclicBarrier separationBarrier;
	private final CyclicBarrier endBarrier;

	private QNode[] nodesArray = null;
	private List<QNode> nodesList = null;
	private List<QLinkInternalI> linksList = new ArrayList<QLinkInternalI>();

	/** 
	 * This is the collection of nodes that have to be activated in the current time step.
	 * This needs to be thread-safe since it is not guaranteed that each incoming link is handled
	 * by the same thread as a node itself.
	 * A node could be activated multiple times concurrently from different incoming links within 
	 * a time step. To avoid this,
	 * a) 	the activateNode() method in the QNode class could be synchronized or 
	 * b) 	a map could be used instead of a list. By doing so, no multiple entries are possible.
	 * 		However, still multiple "put" operations will be performed for the same node.
	 */
	private final Map<Id, QNode> nodesToActivate = new ConcurrentHashMap<Id, QNode>();

	/** This is the collection of links that have to be activated in the current time step */
	private final ArrayList<QLinkInternalI> linksToActivate = new ArrayList<QLinkInternalI>();

	/*package*/ MyQSimEngineRunner(boolean simulateAllNodes, boolean simulateAllLinks, CyclicBarrier startBarrier, CyclicBarrier separationBarrier, CyclicBarrier endBarrier) {
		this.simulateAllNodes = simulateAllNodes;
		this.simulateAllLinks = simulateAllLinks;
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
	}

	/*package*/ void setQNodeArray(QNode[] nodes) {
		this.nodesArray = nodes;
	}

	/*package*/ void setQNodeList(List<QNode> nodes) {
		this.nodesList = nodes;
	}

	/*package*/ void setLinks(List<QLinkInternalI> links) {
		this.linksList = links;
	}

	/*package*/ void setTime(final double t) {
		time = t;
	}

	public void afterSim() {
		this.simulationRunning = false;
	}

	@Override
	public void run() {
		/*
		 * The method is ended when the simulationRunning Flag is
		 * set to false.
		 */
		ArrayList<Long> adt1 = new ArrayList<Long>();
		ArrayList<Long> adt2 = new ArrayList<Long>();

		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are
				 * triggered in the next TimeStep by the run() method in
				 * the ParallelQNetsimEngine.
				 */
				startBarrier.await();

				long t1 = System.nanoTime();

				/*
				 * Check if Simulation is still running.
				 * Otherwise print CPU usage and end Thread.
				 */
				if (!simulationRunning) {
					Gbl.printCurrentThreadCpuTime();
					log.info(adt1.toString());
					log.info(adt2.toString());
					return;
				}

				moveNodes();

				long dt1 = System.nanoTime() - t1;
				adt1.add(dt1);

				/*
				 * After moving the Nodes all we use a CyclicBarrier to synchronize
				 * the Threads. By using a Runnable within the Barrier we activate
				 * some Links.
				 */
				this.separationBarrier.await();

				long t2 = System.nanoTime();

				moveLinks();

				long dt2 = System.nanoTime() - t2;
				adt2.add(dt2);

				/*
				 * The End of the Moving is synchronized with
				 * the endBarrier. If all Threads reach this Barrier
				 * the main Thread can go on.
				 */
				endBarrier.await();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
				Gbl.errorMsg(e);
			}
		}
	}	// run()

	private final Callable<Object> moveNodesC = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			moveNodes();
			return null;
		}
	};

	private void moveNodes() {
		/*
		 * Move Nodes
		 */
		if (useNodeArray) {
			for (QNode node : nodesArray) {
				if (simulateAllNodes || node.isActive() /*|| node.isSignalized()*/) {
					Random random = (Random) node.getCustomAttributes().get(Random.class.getName());
					node.doSimStep(time, random);
				}
			}
		} else {
			ListIterator<QNode> simNodes = this.nodesList.listIterator();
			QNode node;

			while (simNodes.hasNext()) {
				node = simNodes.next();
				Random random = (Random) node.getCustomAttributes().get(Random.class.getName());
				node.doSimStep(time, random);

				if (!node.isActive()) simNodes.remove();
			}
		}
	}

	public Callable<Object> getMoveNodesCallable() {
		return moveNodesC;
	}

	final Callable<Object> moveLinksC = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			moveLinks();
			return null;
		}
	};

	private void moveLinks() {
		/*
		 * Move Links
		 */
		ListIterator<QLinkInternalI> simLinks = this.linksList.listIterator();
		QLinkInternalI link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();

			isActive = link.doSimStep(time);

			if (!isActive && !simulateAllLinks)
				simLinks.remove();
		}
	}

	public Callable<Object> getMoveLinksCallable() {
		return moveLinksC;
	}

	@Override
	protected void activateLink(QLinkInternalI link) {
		if (!simulateAllLinks) {
			linksToActivate.add(link);
		}
	}

	/*package*/ void activateLinks() {
		this.linksList.addAll(this.linksToActivate);
		this.linksToActivate.clear();
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.linksList.size();
	}

	@Override
	protected void activateNode(QNode node) {
		if (!useNodeArray && !simulateAllNodes) {
			this.nodesToActivate.put(node.getNode().getId(), node);
		}
	}

	/*package*/ void activateNodes() {
		if (!useNodeArray && !simulateAllNodes) {
			this.nodesList.addAll(this.nodesToActivate.values());
			this.nodesToActivate.clear();
		}
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		if (useNodeArray) return nodesArray.length;
		else return nodesList.size();
	}

	public NetsimNetworkFactory<QNode,QLinkInternalI> getNetsimNetworkFactory() {
		return new DefaultQNetworkFactory() ;
	}

}
