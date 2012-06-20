package playground.wrashid.parkingSearch.withindayFW.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategyFW.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class ParkingStrategyManager implements BeforeMobsimListener, MobsimInitializedListener {

	private final ParkingStrategyActivityMapperFW strategyActivityMapper;
	private final Collection<ParkingStrategy> parkingStrategies;

	private final TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> currentlySelectedParkingStrategies;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private int iteration;

	private TwoHashMapsConcatenated<Id, Integer, String> legModeActivityTypes;
	private final ParkingPersonalBetas parkingPersonalBetas;

	public ParkingStrategyManager(ParkingStrategyActivityMapperFW strategyActivityMapper,
			Collection<ParkingStrategy> parkingStrategies, ParkingPersonalBetas parkingPersonalBetas) {
		this.strategyActivityMapper = strategyActivityMapper;
		this.parkingStrategies = parkingStrategies;
		this.parkingPersonalBetas = parkingPersonalBetas;
		currentlySelectedParkingStrategies = new TwoHashMapsConcatenated<Id, Integer, ParkingStrategy>();
		agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
		legModeActivityTypes = new TwoHashMapsConcatenated<Id, Integer, String>();
	}

	public void refreshStrategiesAtStartingOfIteration() {

		
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}

		if (this.iteration == 0) {
			// assign strategies per leg at random
			
			for (ExperimentalBasicWithindayAgent agent : agents.values()) {
				Plan selectedPlan = agent.getSelectedPlan();

				if (selectedPlan.getPerson().getId().equals(new IdImpl(304))){
					System.out.println();
				}
				
				for (int i = 0; i < selectedPlan.getPlanElements().size(); i++) {
					PlanElement planElement = selectedPlan.getPlanElements().get(i);
					if (selectedPlan.getPlanElements().get(i) instanceof LegImpl) {

						LegImpl leg = (LegImpl) selectedPlan.getPlanElements().get(i);

						if (leg.getMode().equals(TransportMode.car)) {
							//+1/2 would be parking act/walk leg
							ActivityImpl activity = (ActivityImpl) selectedPlan.getPlanElements().get(i + 3);

							startNewRandomStrategy(agent, i, activity);
						}
					}
				}
			}
		} else {

			for (ExperimentalBasicWithindayAgent agent : agents.values()) {

				DebugLib.traceAgent(agent.getId());
				
				Plan selectedPlan = agent.getSelectedPlan();

				for (int i = 0; i < selectedPlan.getPlanElements().size(); i++) {
					PlanElement planElement = selectedPlan.getPlanElements().get(i);

					if (selectedPlan.getPlanElements().get(i) instanceof LegImpl) {

						LegImpl leg = (LegImpl) selectedPlan.getPlanElements().get(i);


						if (leg.getMode().equals(TransportMode.car)) {
							ActivityImpl activity = (ActivityImpl) selectedPlan.getPlanElements().get(i + 3);
							if (!legModeActivityTypes.get(agent.getId(), i).equals(leg.getMode())
									|| !legModeActivityTypes.get(agent.getId(), i + 3).equals(activity.getType())) {

								if (legModeActivityTypes.get(agent.getId(), i).equals(TransportMode.car)) {
									// as we changed the activity type, we must
									// tidy up the previous strategy score

									tidyUpUnusedStrategyScores(agent, i);
								}

								/*
								 * we switched to car mode or switched the
								 * activity type => we must start from beginning
								 */

								startNewRandomStrategy(agent, i, activity);
							} else {
								// use most of the time best strategy, but also try out others
								
								// TODO: make this unprobabiliistik later (anstatt dess period angeben, e.g. 10). 
								if (MatsimRandom.getRandom().nextDouble() < 0.9) {
									selectStrategyWithHighestScore(agent, i, activity);
								} else {
									Collection<ParkingStrategy> parkingStrategies = strategyActivityMapper.getParkingStrategies(agent.getId(),
											activity.getType());
									
									// TODO: implement avoid starvation later here (round robin anstatt probabilistik).
										int nextInt = MatsimRandom.getRandom().nextInt(parkingStrategies.size());

										ParkingStrategy selectedParkingStrategy = null;
										int j = 0;
										for (ParkingStrategy parkingStrategy : parkingStrategies) {
											if (j == nextInt) {
												selectedParkingStrategy = parkingStrategy;
											}
											j++;
										}

										getCurrentlySelectedParkingStrategies().put(agent.getId(), i, selectedParkingStrategy);
								}
							}
						} else {
							if (legModeActivityTypes.get(agent.getId(), i).equals(TransportMode.car)) {
								// as we were driving car before, we have to
								// tidy up the strategy score

								tidyUpUnusedStrategyScores(agent, i);

							}
						}

					}
				}
			}
		}

		for (ExperimentalBasicWithindayAgent agent : agents.values()) {
			updatedPlanElementHashCodes(agent);
		}
	}

	private void selectStrategyWithHighestScore(ExperimentalBasicWithindayAgent agent, int legPlanElementIndex, ActivityImpl activity) {
		Collection<ParkingStrategy> parkingStrategies = strategyActivityMapper.getParkingStrategies(agent.getId(),
				activity.getType());

		ParkingStrategy selectedParkingStrategy = null;
		double bestStrategyScore = Double.NEGATIVE_INFINITY ;
		for (ParkingStrategy parkingStrategy : parkingStrategies) {
			if (agent.getId().toString().equalsIgnoreCase("303")){
				System.out.println(parkingStrategy.getScore(agent.getId(), 3));
			}
			
			if (parkingStrategy.getScore(agent.getId(), legPlanElementIndex) > bestStrategyScore) {
				selectedParkingStrategy = parkingStrategy;
			}
		}
		
		

		getCurrentlySelectedParkingStrategies().put(agent.getId(), legPlanElementIndex, selectedParkingStrategy);
	}

	private void tidyUpUnusedStrategyScores(ExperimentalBasicWithindayAgent agent, int i) {
		Collection<ParkingStrategy> parkingStrategiesUsedInLastIteration = strategyActivityMapper.getParkingStrategies(
				agent.getId(), legModeActivityTypes.get(agent.getId(), i + 3));

		for (ParkingStrategy parkingStrategy : parkingStrategiesUsedInLastIteration) {
			parkingStrategy.removeScore(agent.getId(), i);

		}

		getCurrentlySelectedParkingStrategies().removeValue(agent.getId(), i);
	}

	private void startNewRandomStrategy(ExperimentalBasicWithindayAgent agent, int i, ActivityImpl activity) {
		Collection<ParkingStrategy> parkingStrategies = strategyActivityMapper.getParkingStrategies(agent.getId(),
				activity.getType());

		if (parkingStrategies.size()==0){
			System.out.println();
		}
		
		int nextInt = MatsimRandom.getRandom().nextInt(parkingStrategies.size());

		ParkingStrategy selectedParkingStrategy = null;
		int j = 0;
		for (ParkingStrategy parkingStrategy : parkingStrategies) {
			parkingStrategy.putScore(agent.getId(), i, Double.NEGATIVE_INFINITY );
			if (j == nextInt) {
				selectedParkingStrategy = parkingStrategy;
			}
			j++;
		}

		getCurrentlySelectedParkingStrategies().put(agent.getId(), i, selectedParkingStrategy);
	}

	// we only handle the cases here, which do not make any sense anymore
	// if location changes, but activity type not, this has to be handled by the
	// general evolutionary algorithm
	private void updatedPlanElementHashCodes(ExperimentalBasicWithindayAgent agent) {

		Plan selectedPlan = agent.getSelectedPlan();

		for (int i = 0; i < selectedPlan.getPlanElements().size(); i++) {

			if (selectedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) selectedPlan.getPlanElements().get(i);
				legModeActivityTypes.put(agent.getId(), i, activity.getType());
			} else if (selectedPlan.getPlanElements().get(i) instanceof LegImpl) {
				LegImpl leg = (LegImpl) selectedPlan.getPlanElements().get(i);
				legModeActivityTypes.put(agent.getId(), i, leg.getMode());
			}
		}

	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.iteration = event.getIteration();
	}

	public TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> getCurrentlySelectedParkingStrategies() {
		return currentlySelectedParkingStrategies;
	}

	public ParkingPersonalBetas getParkingPersonalBetas() {
		return parkingPersonalBetas;
	}

}
