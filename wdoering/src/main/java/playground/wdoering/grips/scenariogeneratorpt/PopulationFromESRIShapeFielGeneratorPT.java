package playground.wdoering.grips.scenariogeneratorpt;

import java.util.List;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.grips.scenariogenerator.PopulationFromESRIShapeFileGenerator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class PopulationFromESRIShapeFielGeneratorPT extends
		PopulationFromESRIShapeFileGenerator {

	private QuadTree<TransitRouteStop> quad;

	public PopulationFromESRIShapeFielGeneratorPT(Scenario sc,
			String populationFile, Id safeLinkId, List<TransitRouteStop> stops) {
		super(sc, populationFile, safeLinkId);
		initQuad(stops);
	}
	
	private void initQuad(List<TransitRouteStop> stops) {
		Envelope e = new Envelope();
		for (TransitRouteStop stop : stops) {
			Coordinate c = MGC.coord2Coordinate(stop.getStopFacility().getCoord());
			e.expandToInclude(c);
		}
		this.quad = new QuadTree<TransitRouteStop>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (TransitRouteStop stop : stops) {
			this.quad.put(stop.getStopFacility().getCoord().getX(), stop.getStopFacility().getCoord().getY(), stop);
		}		
	}

	@Override
	protected void createPersons(Feature ft) {
	
		Population pop = this.scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		long number = (Long)ft.getAttribute("persons");
		for (; number > 0; number--) {
			if (MatsimRandom.getRandom().nextBoolean()) {
				createPT(pb,pop,ft);
			} else {
				createCAR(pb,pop,ft);
			}

		}
	}

	private void createCAR(PopulationFactory pb, Population pop, Feature ft) {
		Person pers = pb.createPerson(this.scenario.createId(Integer.toString(this.id++)));
		pop.addPerson(pers);
		Plan plan = pb.createPlan();
		Coord c = getRandomCoordInsideFeature(this.rnd, ft);
		TransitRouteStop stop = this.quad.get(c.getX(), c.getY());
		
		
		NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
		LinkImpl l = net.getNearestLink(c);
		Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
		((ActivityImpl)act).setCoord(c);
		act.setEndTime(0);
		plan.addActivity(act);
		Leg leg = pb.createLeg("car");
		plan.addLeg(leg);
		
//		Link ll = net.getLinks().get(stop.getStopFacility().getLinkId());
//		Activity act2 = pb.createActivityFromLinkId("wait", stop.getStopFacility().getLinkId());
//		((ActivityImpl)act2).setCoord(ll.getCoord());
//		act2.setEndTime(0);
//		plan.addActivity(act2);
//		Leg leg2 = pb.createLeg("pt");
//		plan.addLeg(leg2);
		
		Link lll = net.getLinks().get(new IdImpl("el1"));
		Activity act3 = pb.createActivityFromLinkId("post-evac", new IdImpl("el1"));
		((ActivityImpl)act3).setCoord(lll.getCoord());
		act3.setEndTime(0);
		plan.addActivity(act3);
		plan.setScore(0.);
		pers.addPlan(plan);
		
	}

	private void createPT(PopulationFactory pb, Population pop, Feature ft) {
		Person pers = pb.createPerson(this.scenario.createId(Integer.toString(this.id++)));
		pop.addPerson(pers);
		Plan plan = pb.createPlan();
		Coord c = getRandomCoordInsideFeature(this.rnd, ft);
		TransitRouteStop stop = this.quad.get(c.getX(), c.getY());
		
		
		NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
		LinkImpl l = net.getNearestLink(c);
		Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
		((ActivityImpl)act).setCoord(c);
		act.setEndTime(0);
		plan.addActivity(act);
		Leg leg = pb.createLeg("walk");
		plan.addLeg(leg);
		
		Link ll = net.getLinks().get(stop.getStopFacility().getLinkId());
		Activity act2 = pb.createActivityFromLinkId("wait", stop.getStopFacility().getLinkId());
		((ActivityImpl)act2).setCoord(ll.getCoord());
		act2.setEndTime(0);
		plan.addActivity(act2);
		Leg leg2 = pb.createLeg("pt");
		plan.addLeg(leg2);
		
		Link lll = net.getLinks().get(this.safeLinkId);
		Activity act3 = pb.createActivityFromLinkId("post-evac", this.safeLinkId);
		((ActivityImpl)act3).setCoord(lll.getCoord());
		act3.setEndTime(0);
		plan.addActivity(act3);
		plan.setScore(0.);
		pers.addPlan(plan);
		
	}

}
