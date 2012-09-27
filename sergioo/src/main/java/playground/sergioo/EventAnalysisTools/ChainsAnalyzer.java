package playground.sergioo.EventAnalysisTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.routes.GenericRoute;


public class ChainsAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentStuckEventHandler {

	//Constants
	private static final NumberFormat numberFormat = new DecimalFormat("00.0");
	
	//Attributes
	private Map<Id, List<String>> activityChains;
	private EventsManager eventsManager;

	//Constructors
	public ChainsAnalyzer(String networkFile, String plansFile, String eventsFile, String noHomeFileLocation) throws FileNotFoundException {
		activityChains = new HashMap<Id, List<String>>();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkFile);
		new MatsimPopulationReader(scenario).parse(plansFile);
		eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(this);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		reviewActivities(noHomeFileLocation, scenario.getPopulation());
	}
	
	//Methods
	private void reviewLastActivity(String noHomeFileLocation) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(noHomeFileLocation);
		int noHome = 0;
		for(Entry<Id,List<String>> activityChain:activityChains.entrySet()) {
			if(!activityChain.getKey().toString().contains("pt_")) {
				String lastActivity = activityChain.getValue().get(activityChain.getValue().size()-1);
				if(!lastActivity.contains("home")) {
					writer.println(activityChain.getKey()+": "+lastActivity);
					noHome++;
				}
			}
		}
		System.out.println("No home: "+noHome);
	}
	private void reviewActivities(String noHomeFileLocation, Population population) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(noHomeFileLocation);
		int noHome = 0;
		for(Entry<Id,List<String>> activityChain:activityChains.entrySet()) {
			if(!activityChain.getKey().toString().contains("pt_")) {
				String lastActivity = activityChain.getValue().get(activityChain.getValue().size()-1);
				if(!lastActivity.contains("home")) {
					Id personId =  activityChain.getKey();
					String plan = personId+": ";
					for(PlanElement planElement:population.getPersons().get(personId).getSelectedPlan().getPlanElements())
						plan+=(planElement instanceof Activity?((Activity)planElement).getType(): ((Leg)planElement).getMode()+(((Leg)planElement).getMode().equals("pt")?"("+((GenericRoute)((Leg)planElement).getRoute()).getRouteDescription()+")":""))+"   ";
					writer.println(plan);
					String chain = activityChain.getKey()+": ";
					for(String activity:activityChain.getValue())
						chain+=activity+"   ";
					writer.println(chain);
					noHome++;
				}
			}
		}
		System.out.println("No home: "+noHome);
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain==null) {
			activityChain = new ArrayList<String>();
			activityChains.put(event.getPersonId(), activityChain);
		}
		activityChain.add("@"+event.getActType());
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain!=null || event.getActType().equals("home"))
			if(activityChain==null) {
				activityChain = new ArrayList<String>();
				activityChain.add("@"+event.getActType()+"("+numberFormat.format(event.getTime()/3600)+")");
				activityChains.put(event.getPersonId(), activityChain);
			}
			else {
				String lastActivity = activityChain.get(activityChain.size()-1);
				if(lastActivity.contains(event.getActType()) && lastActivity.length()==event.getActType().length()+1)
					activityChain.set(activityChain.size()-1, lastActivity+"("+numberFormat.format(event.getTime()/3600)+")");
				else
					throw new RuntimeException("Activity end without beginning");
			}
		else
			throw new RuntimeException("Activity end without person");
	}
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain==null) {
			activityChain = new ArrayList<String>();
			activityChains.put(event.getPersonId(), activityChain);
		}
		activityChain.add("*"+event.getLegMode()+"("+event.getLinkId()+")");
	}
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain!=null) {
			String lastLeg = activityChain.get(activityChain.size()-1);
			if(lastLeg.contains(event.getLegMode()))
				activityChain.set(activityChain.size()-1, lastLeg+"("+numberFormat.format(event.getTime()/3600)+")");
			else
				throw new RuntimeException("Leg end without beginning");
		}
		else
			throw new RuntimeException("Leg end without person");
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain!=null) {
			String lastLeg = activityChain.get(activityChain.size()-1);
			if(lastLeg.startsWith("*"))
				if(lastLeg.endsWith("+") || lastLeg.endsWith("+t"))
					activityChain.set(activityChain.size()-1, lastLeg+"-");
				else
					throw new RuntimeException("Leave vehicle without enter");
			else
				throw new RuntimeException("Leave vehicle without departure");
		}
		else
			throw new RuntimeException("Leave vehicle without person");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain!=null) {
			String lastLeg = activityChain.get(activityChain.size()-1);
			if(lastLeg.startsWith("*"))
				activityChain.set(activityChain.size()-1, lastLeg+"+");
			else
				throw new RuntimeException("Enter vehicle without departure");
		}
		else
			throw new RuntimeException("Enter vehicle without person");
	}
	@Override
	public void handleEvent(AgentStuckEvent event) {
		List<String> activityChain = activityChains.get(event.getPersonId());
		if(activityChain!=null)
			activityChain.add("%%%("+numberFormat.format(event.getTime()/3600)+")");
		else
			throw new RuntimeException("Enter vehicle without person");
	}

	//Main
	/**
	 * 
	 * @param args
	 * 			0 - Events file location
	 * 			1 - No home file location
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		new ChainsAnalyzer(args[0], args[1], args[2], args[3]);
		/*Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse("./input/network/singapore6.xml");
		System.out.println("Memory 1: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
		new PopulationReaderMatsimV4(scenario).readFile("./output_continued_25%veh_it30+/ITERS/it.30/30.plans.xml");
		System.out.println("Memory 2: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
		PrintWriter printWriter = new PrintWriter("./distances.txt");
		int i=0;
		for(Person person:scenario.getPopulation().getPersons().values()) {
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for(int p=1; p<planElements.size(); p++)
				if(planElements.get(p) instanceof Activity && ((Activity)planElements.get(p)).getType().startsWith("w_"))
					if(planElements.get(p-1) instanceof Leg && ((Leg)planElements.get(p-1)).getMode().equals("car"))
						printWriter.println(person.getId()+","+((Leg)planElements.get(p-1)).getRoute().getDistance());
		}
		printWriter.close();*/
	}
	
}
