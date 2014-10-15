package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class bla {

	public static void main(String[] args) throws IOException {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
final BufferedWriter outLink = IOUtils.getBufferedWriter("P:/_TEMP/sschmutz/routing_matsim/20140506_trip_car/output/outputStatistics_trips_6.txt");

		
		for(Person per: sc.getPopulation().getPersons().values()) {
			double time = 0.0;
			double routeDistance = 0.0;
			Plan p = per.getPlans().get(0);
			Id<Link> linkId = null;
			Id<Link> linkId2 = null;
			Activity a = null;
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("leisure")) {
						a = (Activity) pe;
						break;
					}
				}
				else if (pe instanceof Leg) {
					
					time += ((Leg) pe).getTravelTime();
					linkId = ((Leg) pe).getRoute().getStartLinkId();
					linkId2 = ((Leg) pe).getRoute().getEndLinkId();
					
					routeDistance += ((LinkNetworkRouteImpl) ((Leg) pe).getRoute()).getDistance();
					
				}
				
			}
			
			outLink.write(per.getId() + " ");
			outLink.write(Double.toString(time) + " ");
			outLink.write(Double.toString(routeDistance) + " ");
			outLink.write(String.valueOf(CoordUtils.calcDistance(((Activity)p.getPlanElements().get(0)).getCoord(), sc.getNetwork().getLinks().get(linkId).getCoord())) + " ");
			outLink.write(String.valueOf(CoordUtils.calcDistance(a.getCoord(), sc.getNetwork().getLinks().get(linkId2).getCoord())));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();

	}

}
