/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentWithTimeWindowFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.recreation.BestInsertion;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;
import vrp.algorithms.ruinAndRecreate.ruin.RadialRuin;
import vrp.algorithms.ruinAndRecreate.ruin.RandomRuin;
import vrp.algorithms.ruinAndRecreate.thresholdFunctions.SchrimpfsRRThresholdFunction;
import vrp.api.VRP;
import vrp.basics.InitialSolutionFactoryImpl;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import vrp.basics.VrpUtils;

/**
 * Creates ready to use ruin-and-recreate-algorithms.
 * 
 * @author stefan schroeder
 *
 */


public class RuinAndRecreateFactory {
	
	private static Logger logger = Logger.getLogger(RuinAndRecreateFactory.class);
	
	private Collection<RecreationListener> recreationListeners = new ArrayList<RecreationListener>();
	
	private Collection<RuinAndRecreateListener> ruinAndRecreationListeners = new ArrayList<RuinAndRecreateListener>();

	private int warmUp = 10;
	
	private int iterations = 50;
	
	public void addRecreationListener(RecreationListener l){
		recreationListeners.add(l);
	}
	
	public void addRuinAndRecreateListener(RuinAndRecreateListener l){
		ruinAndRecreationListeners.add(l);
	}
	
	
	/**
	 * Standard ruin and recreate without time windows. This algo is configured according to Schrimpf et. al (2000).
	 * @param vrp
	 * @param tours
	 * @param vehicleCapacity
	 * @return
	 */
	public RuinAndRecreate createStandardAlgo(VRP vrp, Collection<Tour> tours, int vehicleCapacity){
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory(vrp);
		Solution initialSolution = getInitialSolution(vrp,tours,tourAgentFactory,vehicleCapacity);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion(vrp);
		recreationStrategy.setInitialSolutionFactory(new InitialSolutionFactoryImpl());
		recreationStrategy.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.2);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.3);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		for(RecreationListener l : recreationListeners){
			recreationStrategy.addListener(l);
		}
		
		return ruinAndRecreateAlgo;
	}
	
	public int getWarmUp() {
		return warmUp;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	/**
	 * PDTW - Pickup and Delivery with TimeWindows
	 * @param vrp
	 * @param tours
	 * @param vehicleCapacity
	 * @return
	 */
	public RuinAndRecreate createAlgoWithPDTW(VRP vrp, Collection<Tour> tours, int vehicleCapacity){
		logger.info("create algo with time windows");
		RRTourAgentWithTimeWindowFactory tourAgentFactory = new RRTourAgentWithTimeWindowFactory(vrp);
		Solution initialSolution = getInitialSolution(vrp,tours,tourAgentFactory,vehicleCapacity);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion(vrp);
		recreationStrategy.setTourAgentFactory(tourAgentFactory);
		recreationStrategy.setInitialSolutionFactory(new InitialSolutionFactoryImpl());
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.3);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		logger.info("done");
		return ruinAndRecreateAlgo;
	}
	
	

	private Solution getInitialSolution(VRP vrp, Collection<Tour> tours, TourAgentFactory tourAgentFactory, int vehicleCapacity) {
		logger.info("make initial solution");
		Collection<TourAgent> tourAgents = new ArrayList<TourAgent>();
		for(Tour tour : tours){
			Vehicle vehicle = VrpUtils.createVehicle(vehicleCapacity);
			tourAgents.add(tourAgentFactory.createTourAgent(tour, vehicle));
		}
		logger.info("done");
		return new Solution(tourAgents);
	}

	public void setWarmUp(int nOfWarmUpIterations) {
		this.warmUp = nOfWarmUpIterations;
		
	}

}
