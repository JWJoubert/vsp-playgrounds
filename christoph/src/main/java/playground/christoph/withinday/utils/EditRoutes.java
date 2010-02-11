package playground.christoph.withinday.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.population.algorithms.PlanAlgorithm;

public class EditRoutes {

	private static final Logger logger = Logger.getLogger(EditRoutes.class);
	
	/*
	 * We create a new Plan which contains only the Leg
	 * that should be replanned and its previous and next
	 * Activities. By doing so the PlanAlgorithm will only
	 * change the Route of that Leg.
	 */
	public boolean replanFutureLegRoute(Plan plan, Leg leg, PlanAlgorithm planAlgorithm)
	{
		if (plan == null) return false;
		if (leg == null) return false;
		if (planAlgorithm == null) return false; 
		
		int index = plan.getPlanElements().indexOf(leg);
		
		if (index == -1) return false;
		
		Activity fromActivity = (Activity) plan.getPlanElements().get(index - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(index + 1);
		
		Route oldRoute = leg.getRoute();
		
		if (oldRoute != null)
		{
			// Update the startLinkId if it has changed.
			if (!fromActivity.getLinkId().equals(oldRoute.getStartLinkId()))
			{
				if (oldRoute instanceof RouteWRefs)
				{
					((RouteWRefs) oldRoute).setStartLinkId(fromActivity.getLinkId());
				}
				else
				{
					logger.warn("Could not update the StartLinkId of the Route! Route was not replanned!");
					return false;
				}
			}
			
			// Update the endLinkId if it has changed.
			if (!toActivity.getLinkId().equals(oldRoute.getEndLinkId()))
			{
				if (oldRoute instanceof RouteWRefs)
				{
					((RouteWRefs) oldRoute).setEndLinkId(toActivity.getLinkId());
				}
				else
				{
					logger.warn("Could not update the EndLinkId of the Route! Route was not replanned!");
					return false;
				}
			}		
		}
		
		/*
		 *  Create a new Plan that contains only the Leg
		 *  which should be replanned and run the PlanAlgorithm.
		 */
		PlanImpl newPlan = new PlanImpl(plan.getPerson());
		newPlan.addActivity(fromActivity);
		newPlan.addLeg(leg);
		newPlan.addActivity(toActivity);
		planAlgorithm.run(newPlan);
		
		Route newRoute = leg.getRoute();

		if (oldRoute != null)
		{
			// If the Route Object was replaced...
			if (oldRoute != newRoute)
			{
				if (oldRoute instanceof NetworkRouteWRefs)
				{
					List<Id> linkIds = ((NetworkRouteWRefs) newRoute).getLinkIds();
					((NetworkRouteWRefs) oldRoute).setLinkIds(newRoute.getStartLinkId(), linkIds, newRoute.getEndLinkId());
					leg.setRoute(oldRoute);
				}
				else
				{
					logger.warn("A new Route Object was created. The Route data could not be copied to the old Route. Cached Referenced to the old Route may cause Problems!");
				}			
			}		
		}
		
		return true;
	}
	
	/*
	 * We create a new Plan which contains only the Leg
	 * that should be replanned and its previous and next
	 * Activities. By doing so the PlanAlgorithm will only
	 * change the Route of that Leg.
	 * 
	 * Use currentNodeIndex from a DriverAgent if possible!
	 * 
	 * Otherwise code it as following:
	 * startLink - Node1 - routeLink1 - Node2 - routeLink2 - Node3 - endLink
	 * The currentNodeIndex has to Point to the next Node
	 * (which is the endNode of the current Link)
	 */
	public boolean replanCurrentLegRoute(Plan plan, Leg leg, int currentNodeIndex, PlanAlgorithm planAlgorithm, Network network, double time)
	{
		if (plan == null) return false;
		if (leg == null) return false;
		if (planAlgorithm == null) return false; 
		
		int index = plan.getPlanElements().indexOf(leg);
		
		if (index == -1) return false;
		
		Activity fromActivity = (Activity) plan.getPlanElements().get(index - 1);
		Activity toActivity = (Activity) plan.getPlanElements().get(index + 1);

		Route oldRoute = leg.getRoute();
		
		// Get the Id of the current Link.
		Id currentLinkId = null;
		if (currentNodeIndex == 1)
		{
			currentLinkId = oldRoute.getStartLinkId();
		}
		else
		{
			if (oldRoute instanceof NetworkRouteWRefs)
			{
				List<Id> ids = ((NetworkRouteWRefs) oldRoute).getLinkIds();
//				if (ids.size() <= currentNodeIndex - 1) currentLinkId = oldRoute.getEndLinkId();
//				else currentLinkId = ids.get(currentNodeIndex - 1);
				currentLinkId = ids.get(currentNodeIndex - 2);
			}
			else
			{
				logger.warn("Could not retrieve the LinkIds of the current Route. Route is not replanned!");
				return false;
			}
		}

		/*
		 *  Create a new Plan with one Leg that leeds from the
		 *  current Position to the destination Activity.
		 */
		Activity newFromActivity = new ActivityImpl(fromActivity.getType(), currentLinkId);
		newFromActivity.setStartTime(time);
		newFromActivity.setEndTime(time);
		
		// The linkIds of the new Route
		List<Id> linkIds = new ArrayList<Id>();
		
		// Get those Links which have already been passed.
		if (oldRoute instanceof NetworkRouteWRefs)
		{
			List<Id> oldLinkIds = ((NetworkRouteWRefs) oldRoute).getLinkIds();
			//TODO use correct index...
			linkIds.addAll(oldLinkIds.subList(0, currentNodeIndex - 1));
		}
		else
		{
			logger.warn("Could not retrieve the LinkIds of the current Route. Route is not replanned!");
			return false;
		}
					
		// Create a new Route from the current Link to the Destination Link
//		LinkNetworkRouteImpl subRoute = new LinkNetworkRouteImpl(currentLinkId, toActivity.getLinkId(), network);
		
		// Create a new leg and use the subRoute.
		Leg newLeg = new LegImpl((LegImpl) leg);
		newLeg.setDepartureTime(time);
//		newLeg.setRoute(subRoute);
		
		/*
		 *  Create a new Plan that contains only the Leg
		 *  which should be replanned and run the PlanAlgorithm.
		 */
		PlanImpl newPlan = new PlanImpl(plan.getPerson());
		newPlan.addActivity(newFromActivity);
		newPlan.addLeg(newLeg);
		newPlan.addActivity(toActivity);
		planAlgorithm.run(newPlan);
		
		Route newRoute = newLeg.getRoute();
		
		// Merge old and new Route.
		if (newRoute instanceof NetworkRouteWRefs)
		{
			linkIds.addAll(((NetworkRouteWRefs) newRoute).getLinkIds());
		}
		else
		{
			logger.warn("The Route data could not be copied to the old Route. Old Route will be used!");
			return false;
		}
		
		// Overwrite old Route
		if (oldRoute instanceof NetworkRouteWRefs)
		{
			((NetworkRouteWRefs) oldRoute).setLinkIds(oldRoute.getStartLinkId(), linkIds, toActivity.getLinkId());
		}
		else
		{
			logger.warn("The new Route data could not be copied to the old Route. Old Route will be used!");
			return false;
		}	
		
		return true;
	}
}
