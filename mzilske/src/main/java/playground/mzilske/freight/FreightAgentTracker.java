package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;

public class FreightAgentTracker implements AgentSource, ActivityEndEventHandler, LinkEnterEventHandler {
	
	private Collection<CarrierImpl> carriers;

	private Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	private Collection<CarrierCostListener> costListeners = new ArrayList<CarrierCostListener>();
	
	double weight = 1;

	private PlanAlgorithm router;

	private EventsManager eventsManager;
	
	private Network network;
	
	public FreightAgentTracker(Collection<CarrierImpl> carriers, PlanAlgorithm router, EventsManager eventsManager) {
		this.carriers = carriers;
		this.router = router;
		this.eventsManager = eventsManager;
		createCarrierAgents();
	}

	@Override
	public List<PlanAgent> getAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plans = carrierAgent.createFreightDriverPlans();
			for (Plan plan : plans) {
				PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
				agents.add(planAgent);
			}
		}
		return agents;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		String activityType = event.getActType();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.activityEnds(personId, activityType);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double distance = network.getLinks().get(linkId).getLength();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.tellDistance(personId, distance);
			}
		}
	}

	public void calculateCostsScoreCarriersAndInform() {
		//inclusive cost per shipment
		for(CarrierAgent carrierAgent : carrierAgents){
			carrierAgent.calculateCostsOfSelectedPlan();
			carrierAgent.scoreSelectedPlan();
			List<Tuple<Shipment,Double>> shipmentCostTuple = carrierAgent.calculateCostsOfSelectedPlanPerShipment();
			for(Tuple<Shipment,Double> t : shipmentCostTuple){
				informCostListeners(t.getFirst(),t.getSecond());
			}
		}
		
	}

	public Collection<CarrierCostListener> getCostListeners() {
		return costListeners;
	}

	private void informCostListeners(Shipment shipment, Double cost) {
		for(CarrierCostListener cl : costListeners){
			cl.informCost(shipment, cost);
		}
	}

	private void createCarrierAgents() {
		for (CarrierImpl carrier : carriers) {
			CarrierAgent carrierAgent = new CarrierAgent(carrier, router);
			carrierAgent.setCostAllocator(new CostAllocator());
			carrierAgents.add(carrierAgent);
		}
	}
	
	
	
}
