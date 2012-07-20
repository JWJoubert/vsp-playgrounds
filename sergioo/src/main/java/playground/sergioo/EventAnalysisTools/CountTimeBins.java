package playground.sergioo.EventAnalysisTools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;

public class CountTimeBins implements LinkEnterEventHandler {
	
	//Constants
	private static final int ONE_HOUR = 3600;
	
	//Attributes
	private Map<String, SortedMap<Integer, Map<Id, Integer>>> numberOfVehicles = new HashMap<String, SortedMap<Integer, Map<Id, Integer>>>();
	
	//Methods
	public CountTimeBins(String[] modes, Collection<Id> linkIds, int totalTime) {
		for(String mode:modes) {
			SortedMap<Integer, Map<Id, Integer>> modeMap = new TreeMap<Integer, Map<Id,Integer>>();
			for(int interval = ONE_HOUR; interval<totalTime+ONE_HOUR-1; interval+=ONE_HOUR) {
				Map<Id, Integer> binMap = new HashMap<Id, Integer>();
				for(Id linkId:linkIds)
					binMap.put(linkId, 0);
				modeMap.put(interval, binMap);
			}
			numberOfVehicles.put(mode, modeMap);
		}
	}
	public Map<String, SortedMap<Integer, Map<Id, Integer>>> getNumberOfVehicles() {
		return numberOfVehicles;
	}
	@Override
	public void reset(int iteration) {
		
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Integer interval = -1;
		for(Integer bin:numberOfVehicles.get(numberOfVehicles.keySet().iterator().next()).keySet())
			if(event.getTime()<bin)
				interval=bin;
		if(event.getVehicleId().toString().startsWith("tr_"))
			numberOfVehicles.get("pt").get(interval).put(event.getLinkId(), numberOfVehicles.get("pt").get(interval).get(event.getLinkId())+1);
		else
			numberOfVehicles.get("car").get(interval).put(event.getLinkId(), numberOfVehicles.get("car").get(interval).get(event.getLinkId())+1);
	}
	
}
