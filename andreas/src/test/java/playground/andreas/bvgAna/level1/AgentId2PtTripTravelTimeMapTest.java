/**
 * 
 */
package playground.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author fuerbas
 *
 */
public class AgentId2PtTripTravelTimeMapTest {
	
	private TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>> agentId2PtTripTravelTimeMap = new TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>>();
	private TreeMap<Id, AgentId2PtTripTravelTimeMapData> tempList = new TreeMap<Id, AgentId2PtTripTravelTimeMapData>();

	/**
	 * Test method for {@link playground.andreas.bvgAna.level1.AgentId2PtTripTravelTimeMap#AgentId2PtTripTravelTimeMap(java.util.Set)}.
	 */
	@Test
	
	public void testAgentId2PtTripTravelTimeMap() {
	
		
	       Id[] ida= new Id[15];
	    	Set<Id> idSet = new TreeSet<Id>();
	        for (int ii=0; ii<15; ii++){
	        	ida[ii] = new IdImpl(ii); 
	            idSet.add(ida[ii]);
	        }
	        
//	        assign Ids to routes, vehicles and agents to be used in Test
	        
	        Id linkId1 = ida[1];
	        Id linkId2 = ida[2];
	        Id linkId3 = ida[3];
	        Id agentId1 = ida[4];
	        Id facilId1 = ida[5];
	        Id facilId2 = ida[6];        

	        
	        AgentId2PtTripTravelTimeMap test = new AgentId2PtTripTravelTimeMap(idSet);
	        
	        ActivityStartEvent event1 = new ActivityStartEvent(1.0, agentId1, linkId1, facilId1, "w");
	        ActivityEndEvent event2 = new ActivityEndEvent(1.2, agentId1, linkId1, facilId1, "w");
	        
	        PersonDepartureEvent event3 = new PersonDepartureEvent(1.2, agentId1, linkId2, "pt");        
	        PersonArrivalEvent event4 = new PersonArrivalEvent(1.9, agentId1, linkId3, "pt");
	        
	        ActivityStartEvent event5 = new ActivityStartEvent(1.9, agentId1, linkId3, facilId2, "h");	//home mit anderen werten
	        ActivityEndEvent event6 = new ActivityEndEvent(2.7, agentId1, linkId3, facilId2, "h");
	                
	        test.handleEvent(event1);
	        test.handleEvent(event2);
	        test.handleEvent(event3);
	        test.handleEvent(event4);
	        test.handleEvent(event5);
	        test.handleEvent(event6);
	        
//	        first tests, this works
	        
	        Assert.assertEquals(event4.getTime()-event3.getTime(), test.getAgentId2PtTripTravelTimeMap().get(agentId1).get(0).getTotalTripTravelTime(), 0.);
	        	        
		
	}



}
