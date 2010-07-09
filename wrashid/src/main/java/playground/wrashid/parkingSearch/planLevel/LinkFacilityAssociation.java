package playground.wrashid.parkingSearch.planLevel;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;

public class LinkFacilityAssociation {

	protected HashMap<Id,ArrayList<ActivityFacilityImpl>> linkFacilityMapping=new HashMap<Id, ArrayList<ActivityFacilityImpl>>();
	protected NetworkImpl network;
	 
	protected LinkFacilityAssociation(){
		
	}
	
	public LinkFacilityAssociation(Controler controler) {
		ActivityFacilitiesImpl facilities=(ActivityFacilitiesImpl) controler.getFacilities();
		
		this.network=controler.getNetwork();
		
		init(facilities);
	}
	
	public LinkFacilityAssociation(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		this.network=network;
		init(facilities);
	}
	
	private void init(ActivityFacilitiesImpl facilities){
		for (ActivityFacilityImpl facility: facilities.getFacilities().values()){
			addFacilityToHashMap(facility);
		}
	}
	
	
	/**
	 * put the facility into the arrayList for the appropriate link.
	 * @param facility
	 */
	private void addFacilityToHashMap(ActivityFacilityImpl facility) {
		Id facilityLink=getClosestLink(facility);
		
		assureHashMapInitializedForLink(facilityLink);
		
		ArrayList<ActivityFacilityImpl> list=linkFacilityMapping.get(facilityLink);
		
		// implicit assumption: a facility will only get added once for the same link
		list.add(facility);		
	}

	/**
	 * need also to takle the case, if facility not assigned
	 * @return
	 */
	protected Id getClosestLink(ActivityFacilityImpl facility){
		if (facility.getLinkId()==null){
			return network.getNearestLink(facility.getCoord()).getId();
		} else {
			return facility.getLinkId();
		}
	}
	
	

	/**
	 * Make sure, that in the HashMap an entry exists for the given linkId
	 * @param linkId
	 */
	protected void assureHashMapInitializedForLink(Id linkId) {
		if (!linkFacilityMapping.containsKey(linkId)){
			linkFacilityMapping.put(linkId, new ArrayList<ActivityFacilityImpl>());
		}
	}

	public ArrayList<ActivityFacilityImpl> getFacilities(Id linkId){
		return linkFacilityMapping.get(linkId);
	}
	
	
	
}
