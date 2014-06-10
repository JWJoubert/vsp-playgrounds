package playground.balac.retailers.strategies;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;

import playground.balac.retailers.data.LinkRetailersImpl;


public class RetailerStrategyImpl
  implements RetailerStrategy
{
	  private static final Logger log = Logger.getLogger(RetailerStrategyImpl.class);
	  protected Map<Id, ActivityFacilityImpl> retailerFacilities;
	  protected Controler controler;
	
	  public RetailerStrategyImpl(Controler controler)
	  {
	    this.controler = controler;
	    log.info("Controler" + this.controler);
	  }
	
	  @Override
		public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, TreeMap<Id, LinkRetailersImpl> links)
	  {
	    return null;
	  }
	
	  protected TreeMap<Integer, String> createInitialLocationsForGA(TreeMap<Id, LinkRetailersImpl> availableLinks) {
	    TreeMap<Integer, String> locations = new TreeMap<Integer, String>();
	    int intCount = 0;
	    for (ActivityFacilityImpl af : this.retailerFacilities.values())
	    {
	      locations.put(Integer.valueOf(intCount), ((NetworkImpl) this.controler.getNetwork()).getNearestLink(af.getCoord()).getId().toString());
	      ++intCount;
	      log.info("The facility with Id: " + af.getId() + " has been added, this is located on the link: " + af.getLinkId());
	    }
	    for (LinkRetailersImpl l : availableLinks.values()) {
	      if (locations.containsValue(l.getId().toString())) {
	        log.info("The Link: " + l.getId() + " is already on the list");
	      }
	      else {
	        locations.put(Integer.valueOf(intCount), l.getId().toString());
	        ++intCount;
	        log.info("The Link: " + l.getId() + " has been added");
	      }
	    }
	
	    log.info("Initial Locations (with Free Links) = " + locations);
	    return locations;
	  }
	
	  protected TreeMap<Id, LinkRetailersImpl> mergeLinks(TreeMap<Id, LinkRetailersImpl> freeLinks, Map<Id, ActivityFacilityImpl> retailerFacilities)
	  {
	    this.retailerFacilities = retailerFacilities;
	    TreeMap<Id,LinkRetailersImpl> availableLinks = new TreeMap<Id,LinkRetailersImpl>();
	    for (ActivityFacilityImpl af : this.retailerFacilities.values()) {
	    	Id id = af.getLinkId();
	    	LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(id), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	    	availableLinks.put(id, link);
	    }
	    availableLinks.putAll(freeLinks);
	    return availableLinks;
	  }
	


}
