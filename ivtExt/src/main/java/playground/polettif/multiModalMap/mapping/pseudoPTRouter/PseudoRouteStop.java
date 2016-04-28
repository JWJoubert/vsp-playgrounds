package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * A RouteStop used in the pseudoGraph.
 *
 * Link Candidates are made for each stop facility. Since one
 * stop facility might be accessed twice in the same transitRoute,
 * unique Link Candidates for each TransitRouteStop are needed. This
 * is achieved via this class.
 *
 * @author polettif
 */
public class PseudoRouteStop {

	public final String id;
	private final String name;

	private final String linkId;

	private final double departureOffset;
	private final double arrivalOffset;
	private final boolean awaitDepartureTime;

	private final Coord coord;
	private final boolean isBlockingLane;
	private final String facilityName;
	private final String stopPostAreaId;
	private final String parentStopFacilityId;
	private final String linkCandidateId;
	private final double linkWeight;

	/*
	private static PublicTransportMapEnum pseudoRouteWeightType;
	public static void setPseudoRouteWeightType(PublicTransportMapEnum weightType) {
		pseudoRouteWeightType = weightType;
	}
	*/

	/**
	 * Constructor. All values are stored here as well to make access easier during
	 * stop facility replacement.
	 * @param order
	 * @param routeStop
	 * @param linkCandidate
	 */
	public PseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		this.id = Integer.toString(order) + linkCandidate.getId();
		this.linkCandidateId = linkCandidate.getId();
		this.name = routeStop.getStopFacility().getName() + " (" + linkCandidate.getLinkIdStr() + ")";
		this.linkId = linkCandidate.getLinkIdStr();

		// stop facility values
		this.coord = routeStop.getStopFacility().getCoord();
		this.parentStopFacilityId = routeStop.getStopFacility().getId().toString();
		this.isBlockingLane = routeStop.getStopFacility().getIsBlockingLane();
		this.facilityName = routeStop.getStopFacility().getName();
		this.stopPostAreaId = routeStop.getStopFacility().getStopPostAreaId();

		// route stop values
		this.departureOffset = routeStop.getDepartureOffset();
		this.arrivalOffset = routeStop.getArrivalOffset();
		this.awaitDepartureTime = routeStop.isAwaitDepartureTime();

		// link value
//		this.linkWeight = (pseudoRouteWeightType.equals(PublicTransportMapEnum.travelTime) ? linkCandidate.getLinkTravelTime() : linkCandidate.getLinkLength());
		this.linkWeight = linkCandidate.getLinkLength();
	}

	public PseudoRouteStop(String id) {
		if(id.equals("SOURCE")) {
			this.id = "SOURCE";
		} else {
			this.id = "DESTINATION";
		}
		this.name = id;
		this.linkCandidateId = null;
		this.linkId = null;

		// stop facility values
		this.coord = null;
		this.parentStopFacilityId = null;
		this.isBlockingLane = false;
		this.facilityName = null;
		this.stopPostAreaId = null;

		// route stop values
		this.departureOffset = 0.0;
		this.arrivalOffset = 0.0;
		this.awaitDepartureTime = false;

		// link value
		this.linkWeight = 0.0;
	}


	public double getDepartureOffset() {
		return departureOffset;
	}

	public double getArrivalOffset() {
		return arrivalOffset;
	}

	public boolean isAwaitDepartureTime() {
		return awaitDepartureTime;
	}

	@Deprecated
	public String getChildStopFacilityId() {
		return linkCandidateId;
	}

	public String getName() {
		return name;
	}

	public Coord getCoord() {
		return coord;
	}

	public boolean getIsBlockingLane() {
		return isBlockingLane;
	}

	public String getFacilityName() {
		return facilityName;
	}

	public String getStopPostAreaId() {
		return stopPostAreaId;
	}

	public String getLinkIdStr() {
		return linkId;
	}

	public String getParentStopFacilityId() {
		return parentStopFacilityId;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		PseudoRouteStop other = (PseudoRouteStop) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public double getLinkWeight() {
		return linkWeight;
	}
}
