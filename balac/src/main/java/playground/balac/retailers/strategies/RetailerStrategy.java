package playground.balac.retailers.strategies;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.LinkRetailersImpl;


public abstract interface RetailerStrategy
{
  public abstract Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> paramMap, TreeMap<Id, LinkRetailersImpl> paramTreeMap);
}
