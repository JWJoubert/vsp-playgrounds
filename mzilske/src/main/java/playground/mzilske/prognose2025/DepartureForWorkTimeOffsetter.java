package playground.mzilske.prognose2025;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class DepartureForWorkTimeOffsetter implements TripFlowSink {

	private Network network;

	private Dijkstra dijkstra;

	private TripFlowSink sink;

	public DepartureForWorkTimeOffsetter(Network network) {
		this.network = network;
		FreespeedTravelTimeCost fttc = new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup());
		dijkstra = new Dijkstra(network, fttc, fttc);
	}

	@Override
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double travelTimeOffset) {
		Node quellNode = ((NetworkImpl) network).getNearestNode(quelle.coord);
		Node zielNode = ((NetworkImpl) network).getNearestNode(ziel.coord);
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0);
		double travelTimeToWork = calculateFreespeedTravelTimeToNode(this.network, path, zielNode);
		System.out.println("from zone " + quelle.id + " to zone " + ziel.id + ", it takes " + travelTimeToWork + " seconds to travel.");
		sink.process(quelle, ziel, quantity, mode, destinationActivityType, travelTimeOffset - travelTimeToWork );
	}

	void setSink(TripFlowSink sink) {
		this.sink = sink;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	private static double calculateFreespeedTravelTimeToNode(Network network, Path path, Node node) {
		double travelTime = 0.0;
		for (Link l : path.links) {
			travelTime += l.getLength() / l.getFreespeed();
		}
		return travelTime;
	}

}
