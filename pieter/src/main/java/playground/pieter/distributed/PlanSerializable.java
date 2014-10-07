package playground.pieter.distributed;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.vehicles.Vehicle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class PlanSerializable implements Serializable {
	interface PlanElementSerializable extends Serializable {

	}

	class ActivitySerializable implements PlanElementSerializable {
		private CoordSerializable coord;
		private double endTime;
		private String facIdString;
		private String linkIdString;
		private double maximumDuration;
		private double startTime;
		private String type;

		public ActivitySerializable(Activity act) {
			coord = new CoordSerializable(act.getCoord());
			endTime = act.getEndTime();
			facIdString = act.getFacilityId() == null ? null : act.getFacilityId().toString();
			linkIdString = act.getLinkId().toString();
			maximumDuration = act.getMaximumDuration();
			startTime = act.getStartTime();
			type = act.getType();
		}

		public Activity getActivity() {
			ActivityImpl activity = new ActivityImpl(type, coord.getCoord(), Id.createLinkId(linkIdString));
			activity.setEndTime(endTime);
			activity.setFacilityId(facIdString == null ? null : Id.create(facIdString, ActivityFacility.class));
			activity.setMaximumDuration(maximumDuration);
			activity.setStartTime(startTime);
			return activity;
		}
	}

	class LegSerializable implements PlanElementSerializable {
		private double departureTime;
		private String mode;
		private double travelTime;
		private RouteSerializable route;

		public LegSerializable(Leg leg) {
			departureTime = leg.getDepartureTime();
			mode = leg.getMode();
			travelTime = leg.getTravelTime();
			if(mode.equals(TransportMode.pt))
				System.out.print("");
			if (leg.getRoute() instanceof NetworkRoute)
				route = new LinkNetworkRouteSerializable((NetworkRoute) leg.getRoute());
			else if (leg.getRoute() instanceof GenericRoute)
				route = new GenericRouteSerializable((GenericRoute) leg.getRoute());

			
		}

		public Leg getLeg() {
			Leg leg = new LegImpl(mode);
			leg.setDepartureTime(departureTime);
			leg.setTravelTime(travelTime);
			leg.setRoute(route.getRoute(mode));
			return leg;
		}
	}

	class CoordSerializable implements Serializable {
		private double x;
		private double y;

		public CoordSerializable(Coord coord) {
			x = coord.getX();
			y = coord.getY();
		}

		public Coord getCoord() {
			return new CoordImpl(x, y);

		}
	}

	interface RouteSerializable extends Serializable {
		Route getRoute(String mode);
	}

	class LinkNetworkRouteSerializable implements RouteSerializable {

		private double distance;
		private String endLinkIdString;
		private String startLinkIdString;
		private double travelCost;
		private double travelTime;
		private String vehicleIdString;
		private List<String> linkIdStrings;

		public LinkNetworkRouteSerializable(NetworkRoute route) {
			distance = route.getDistance();
			endLinkIdString = route.getEndLinkId().toString();
			startLinkIdString = route.getStartLinkId().toString();
			travelCost = route.getTravelCost();
			travelTime = route.getTravelTime();
			vehicleIdString = route.getVehicleId() == null ? null : route.getVehicleId().toString();
			List<Id<Link>> linkIds = route.getLinkIds();
			linkIdStrings = new ArrayList<>();
			for (Id<Link> linkid : linkIds)
				linkIdStrings.add(linkid.toString());
		}

		@Override
		public Route getRoute(String mode) {
            Id<Link> startLinkId = Id.createLinkId(startLinkIdString);
            Id<Link> endLinkId = Id.createLinkId(endLinkIdString);
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setDistance(distance);
			List<Id<Link>> linkIds = new ArrayList<>();
			for (String linkId : linkIdStrings)
				linkIds.add(Id.createLinkId(linkId));
			route.setLinkIds(startLinkId, linkIds, endLinkId);
			route.setTravelCost(travelCost);
			route.setTravelTime(travelTime);
			route.setVehicleId(vehicleIdString == null ? null : Id.create(vehicleIdString, Vehicle.class));
			return route;
		}

	}

	class GenericRouteSerializable implements RouteSerializable {

		private double distance;
		private String endLinkIdString;
		private String routeDescription;
		private String startLinkIdString;
		private double travelTime;

		public GenericRouteSerializable(GenericRoute route) {
			distance = route.getDistance();
			endLinkIdString = route.getEndLinkId().toString();
			routeDescription = route.getRouteDescription();
			startLinkIdString = route.getStartLinkId().toString();
			travelTime = route.getTravelTime();
		}

		@Override
		public Route getRoute(String mode) {
			GenericRoute route;
            Id<Link> startLinkId = Id.createLinkId(startLinkIdString);
            Id<Link> endLinkId = Id.createLinkId(endLinkIdString);
            if(!mode.equals(TransportMode.pt)){
				route = new GenericRouteImpl(startLinkId, endLinkId);
			}else{
				route = (GenericRoute) new ExperimentalTransitRouteFactory().createRoute(startLinkId, endLinkId);
			}
			route.setDistance(distance);
			route.setTravelTime(travelTime);
			route.setRouteDescription(startLinkId, routeDescription, endLinkId);
			return route;
		}

	}

	private ArrayList<PlanElementSerializable> planElements;
	private String personId;
	private Double score;
	private String type;

	public PlanSerializable(Plan plan) {
		planElements = new ArrayList<>();
		for (PlanElement planElement : plan.getPlanElements())
			if (planElement instanceof Activity)
				planElements.add(new ActivitySerializable((Activity) planElement));
			else
				planElements.add(new LegSerializable((Leg) planElement));
		personId = plan.getPerson().getId().toString();
		score = plan.getScore();
		type = plan.getType();
	}

	public Plan getPlan(Population population) {
		Plan plan = new PlanImpl(population.getPersons().get(Id.createPersonId(personId)));
		plan.setScore(score);
		plan.setType(type);
		for (PlanElementSerializable planElementSerializable : planElements)
			if (planElementSerializable instanceof ActivitySerializable)
				plan.addActivity(((ActivitySerializable) planElementSerializable).getActivity());
			else
				plan.addLeg(((LegSerializable) planElementSerializable).getLeg());
		return plan;
	}

}
