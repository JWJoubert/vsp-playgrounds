package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;

public class StopId2RouteId2DelayAtStopMapTest {
	
	@Test
	public void testStopId2RouteId2DelayAtStopMap() {
	    
		Id[] ida= new Id[15];
		Set<Id> idSet = new TreeSet<Id>();
	    for (int ii=0; ii<15; ii++){
	    	ida[ii] = new IdImpl(ii); 
	        idSet.add(ida[ii]);
	    }
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
		
		Id vehId1 = ida[1];
	    Id vehId2 = ida[2];
	    Id driverId1 = ida[4];
	    Id driverId2 = ida[5];
	    Id departureId1 = ida[6];
	    Id departureId2 = ida[7];
	    Id transitRouteId1 = ida[14];
	    Id transitRouteId2 = ida[2];
	    Id transitLineId1 = ida[4];
	    Id transitLineId2 = ida[6];
	    Id facilityId1 = ida[8];
	    Id facilityId2 = ida[9];
	    
//	    create events
	    
	    VehicleDepartsAtFacilityEvent event1 = new VehicleDepartsAtFacilityEvent(2., vehId1, facilityId1, 0.5);
	    VehicleDepartsAtFacilityEvent event2 = new VehicleDepartsAtFacilityEvent(2.7, vehId2, facilityId2, 0.7);
	    TransitDriverStartsEvent event3 = new TransitDriverStartsEvent(2.8, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
	    TransitDriverStartsEvent event4 = new TransitDriverStartsEvent(2.3, driverId2, vehId2, transitLineId2, transitRouteId2, departureId2);
	    
	    StopId2RouteId2DelayAtStopMap test = new StopId2RouteId2DelayAtStopMap();
	    
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	   	   
	    
	    // to be completed
	    
	    /**
	     * @TODO complete tests, first tests working now
	     */
	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).toString());
//	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getLineId());
	    
	    Assert.assertEquals(transitLineId1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getLineId());
	    
	    Assert.assertEquals(transitRouteId1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getRouteId());
	    
	    Assert.assertEquals(1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getRealizedDepartures().size());
	    
	    
	    
	    
//	    String test1 = "{"+transitRouteId1+"=Stop: "+event1.getFacilityId()+", Line: "+transitLineId1+", Route: "+transitRouteId1+", # planned Departures: 1, # realized Departures: 1}";
//	    System.out.println(test1);
//	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event2.getFacilityId()).toString());
	    

	    
	    
	    
	    
	}



}
