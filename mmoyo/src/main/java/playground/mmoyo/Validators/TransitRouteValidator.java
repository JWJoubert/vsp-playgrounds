package playground.mmoyo.Validators;

import playground.mmoyo.PTRouter.MyDijkstra;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import playground.mmoyo.PTRouter.LogicFactory;
import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.network.Node;

/**
 * Identifies isolated TransitRoutes
 */
public class TransitRouteValidator {

	private NetworkLayer logicNetwork;		
	private TransitSchedule transitSchedule;
	
	public TransitRouteValidator(TransitSchedule transitSchedule){
		this.logicNetwork =	new LogicFactory(transitSchedule).getLogicNet();
		getIsolatedPTLines();
	}
	
	public void getIsolatedPTLines(){
		
		int isolated=0;
		int comparisons=0;
		PseudoTimeCost pseudoTimeCost = new PseudoTimeCost();
		LeastCostPathCalculator expressDijkstra = new MyDijkstra(logicNetwork, pseudoTimeCost, pseudoTimeCost);
		
		List<Id[]> ptLineIdList = new ArrayList<Id[]>();
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				for (TransitRoute transitRoute2 : transitLine.getRoutes().values()){
					if(!transitRoute.equals(transitRoute2)){
						Node node1 = transitRoute.getRoute().getNodes().get(0);
						Node node2 = transitRoute2.getRoute().getNodes().get(transitRoute2.getRoute().getNodes().size()-1);;
						Path path = expressDijkstra.calcLeastCostPath(node1, node2, 600);
						comparisons++;
						if (path==null){
							//Id[2] intArray = [ptLine.getId(),ptLine2.getId()];
							Id[] idArray = new Id[2];
							idArray[0] = transitRoute.getId();
							idArray[1] = transitRoute2.getId();
							ptLineIdList.add(idArray);
							isolated++;
						}
					}
				}
			}
		}
	
		for(Id[] idarray: ptLineIdList){
			System.out.println("\n" + idarray[0] + "\n" + idarray[1] );
		}
		System.out.println(	"Total comparisons: " + comparisons + "\nisolated: " + isolated);
	}
	
	/**
	 * Returns the minimal distance between two PTLines. This can help the decision of joining them with a Detached Transfer 
	 */
	public double getMinimalDistance (final TransitRoute transitRoute1, final TransitRoute transitRoute2){
		double minDistance=0;
		// ->compare distances from first ptline with ever node of secondptline, store the minimal distance		
		return minDistance;
	}	
	
	class PseudoTimeCost implements TravelCost, TravelTime {

		public PseudoTimeCost() {
		}

		public double getLinkTravelCost(final Link link, final double time) {
			return 1.0;
		}

		public double getLinkTravelTime(final Link link, final double time) {
			return 1.0;
		}
	}
	
	
	
}