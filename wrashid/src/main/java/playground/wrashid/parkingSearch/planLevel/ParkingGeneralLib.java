package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

public class ParkingGeneralLib {

	/**
	 * reurns null, if no parking activity found else id of first parking
	 * facility
	 * 
	 * @param plan
	 * @return
	 */
	public static Id getFirstParkingFacilityId(Plan plan) {

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					return activity.getFacilityId();
				}

			}
		}

		return null;
	}

	/**
	 * The home/first parking comes last (because it is the last parking arrival
	 * of the day).
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<Id> getAllParkingFacilityIds(Plan plan) {
		LinkedList<Id> parkingFacilityIds = new LinkedList<Id>();

		// recognize parking arrival patterns (this means, there is car leg
		// after which there is
		// a parking activity).
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						parkingFacilityIds.add(activity.getFacilityId());
					}
				}
			}
		}

		return parkingFacilityIds;
	}

	/**
	 * get the first activity after each arrival at a parking.
	 * 
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<ActivityImpl> getParkingTargetActivities(Plan plan) {
		LinkedList<ActivityImpl> list = new LinkedList<ActivityImpl>();

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						// parking arrival pattern recognized.

						ActivityImpl targetActivity = (ActivityImpl) plan.getPlanElements().get(i + 2);
						list.add(targetActivity);
					}
				}
			}
		}

		return list;
	}

	/**
	 * Get the ParkingTimeInfo of the parking related to the
	 * given activity
	 * 
	 * @param activity
	 * @return
	 */
	public static ParkingTimeInfo getParkingTimeInfo(Plan plan, ActivityImpl activity) {
		ActivityImpl arrivalParkingAct = getArrivalParkingAct(plan, activity);
		ActivityImpl departureParkingAct = getDepartureParkingAct(plan, activity);

		return new ParkingTimeInfo(arrivalParkingAct.getStartTime(), departureParkingAct.getEndTime());
	}

	public static ActivityImpl getDepartureParkingAct(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfDepartingParkingAct = getDepartureParkingActIndex(plan, activity);

		ActivityImpl departureParkingAct = (ActivityImpl) pe.get(indexOfDepartingParkingAct);
		return departureParkingAct;
	}

	public static int getDepartureParkingActIndex(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfDepartingParkingAct = -1;

		for (int i = activityIndex; i < pe.size(); i++) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
				if (parkingAct.getType().equalsIgnoreCase("parking")) {
					indexOfDepartingParkingAct = i;
					break;
				}
			}
		}

		// if home parking
		if (indexOfDepartingParkingAct == -1) {
			for (int i = 0; i < pe.size(); i++) {
				if (pe.get(i) instanceof ActivityImpl) {
					ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
					if (parkingAct.getType().equalsIgnoreCase("parking")) {
						indexOfDepartingParkingAct = i;
						break;
					}
				}
			}
		}

		if (indexOfDepartingParkingAct == -1) {
			throw new Error("plan wrong: no parking in the whole plan");
		}

		return indexOfDepartingParkingAct;
	}

	/**
	 * Get the arrival parking responding to this activity.
	 * 
	 * @return
	 */
	public static ActivityImpl getArrivalParkingAct(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfArrivalParkingAct = getArrivalParkingActIndex(plan, activity);

		ActivityImpl arrivalParkingAct = (ActivityImpl) pe.get(indexOfArrivalParkingAct);
		return arrivalParkingAct;
	}

	public static int getArrivalParkingActIndex(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfArrivalParkingAct = -1;

		for (int i = activityIndex; i < 0; i--) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
				if (parkingAct.getType().equalsIgnoreCase("parking")) {
					indexOfArrivalParkingAct = i;
					break;
				}
			}
		}

		if (indexOfArrivalParkingAct == -1) {
			throw new Error("no parking arrival activity found - something is wrong with the plan");
		}

		return indexOfArrivalParkingAct;
	}

}
