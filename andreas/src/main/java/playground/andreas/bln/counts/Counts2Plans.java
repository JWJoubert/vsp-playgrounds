package playground.andreas.bln.counts;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

public class Counts2Plans {
	
	private static final Logger log = Logger.getLogger(Counts2Plans.class);
	private static final Random rnd = new Random(4711);
	
	private Counts access = new Counts();
	private Counts egress = new Counts();
	private TransitSchedule transitSchedule;
	
	private HashMap<String, LinkedList<Id>> lines = new HashMap<String, LinkedList<Id>>();
	
	private int runningID = 1;
	private int numberOfPersonsWithValidPlan = 0;
	private int numberOfPersonsLeftInBusAtEndOfLine = 0;
	private int numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo = 0;
	
	private LinkedList<PersonImpl> completedAgents = new LinkedList<PersonImpl>();
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Counts2Plans counts2plans = new Counts2Plans();
		counts2plans.access = Counts2Plans.readCountsFile("d:/Berlin/BVG/berlin-bvg09/counts/pt/counts_boarding_M44_344.xml");
		counts2plans.egress = Counts2Plans.readCountsFile("d:/Berlin/BVG/berlin-bvg09/counts/pt/counts_alighting_M44_344.xml");
		
		counts2plans.transitSchedule = Counts2Plans.readTransitSchedule("d:/Berlin/BVG/berlin-bvg09/pt/m4_demand/transitSchedule.xml", "d:/Berlin/BVG/berlin-bvg09/pt/m4_demand/network.xml");
		
//		counts2plans.addLine("344  ");

		counts2plans.addM44_H();
		counts2plans.addM44_R();
		counts2plans.add344_H();
		counts2plans.add344_R();
		
		counts2plans.createPlans();
		counts2plans.createPopulation("E:/_out/plans.xml.gz");
		
		counts2plans.printLog();
		
		Counts2Plans.log.info("Finished");

	}

	private void printLog() {
		log.info(this.numberOfPersonsWithValidPlan + " persons were created according to counts files");
		log.info(this.numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo + " persons were supposed to leave the bus, but the vehicle didn't contain any");
		log.info(this.numberOfPersonsLeftInBusAtEndOfLine + " persons where sitting in the bus at the end of line, because they never got the possibility to leave the vehicle");		
	}

	private void createPlans() {

		for (int hour = 1; hour <= 24; hour++) {

			log.info("Hour: " + hour);				
			LinkedList<PersonImpl> passengersInVehicle = new LinkedList<PersonImpl>();

			for (LinkedList<Id> line : this.lines.values()) {

				for (Id stopID : line) {

					if (this.egress.getCount(stopID) != null) {
						if (this.egress.getCount(stopID).getVolume(hour) != null) {

							for (int i = 0; i < this.egress.getCount(stopID).getVolume(hour).getValue(); i++) {

								PersonImpl person = passengersInVehicle.pollFirst();

								if (person == null) {
									log.warn("StopID: " + stopID + ", Passenger should leave the vehicle, but none is there");
									this.numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo++;
								} else {
									((PlanImpl) person.getSelectedPlan()).createAndAddActivity("finish", this.transitSchedule.getFacilities().get(stopID).getLinkId());
									((PlanImpl) person.getSelectedPlan()).getLastActivity().setCoord(this.transitSchedule.getFacilities().get(stopID).getCoord());
									//									((PlanImpl) person.getSelectedPlan()).createAndAddActivity("finish", this.egress.getCount(stopID).getCoord());
									this.completedAgents.add(person);
									this.numberOfPersonsWithValidPlan++;
								}
							}
						}
					}

					if (this.access.getCount(stopID) != null) {
						if (this.access.getCount(stopID).getVolume(hour) != null) {

							for (int i = 0; i < this.access.getCount(stopID).getVolume(hour).getValue(); i++) {
								PersonImpl person = createPerson();
								((PlanImpl) person.getSelectedPlan()).createAndAddActivity("start", this.transitSchedule.getFacilities().get(stopID).getLinkId());
								((PlanImpl) person.getSelectedPlan()).getFirstActivity().setCoord(this.transitSchedule.getFacilities().get(stopID).getCoord());
								//								((PlanImpl) person.getSelectedPlan()).createAndAddActivity("start", this.access.getCount(stopID).getCoord());
								((PlanImpl) person.getSelectedPlan()).createAndAddLeg(TransportMode.pt);
								((PlanImpl) person.getSelectedPlan()).getFirstActivity().setEndTime((hour - 1 + rnd.nextDouble()) * 3600);
								passengersInVehicle.add(person);
							}
						}
					}

				}

				if(passengersInVehicle.size() != 0){
					log.warn(hour + " hour, " + passengersInVehicle.size() + " passengers still in vehicle after last stop");
					this.numberOfPersonsLeftInBusAtEndOfLine += passengersInVehicle.size();
				}

			}

		}

	}

	private PersonImpl createPerson(){
		PersonImpl person = new PersonImpl(new IdImpl(this.runningID));
		person.createAndAddPlan(true);
		this.runningID++;
		return person;
	}

	private void createPopulation(String filename) {
				
		PopulationImpl pop = new PopulationImpl(new ScenarioImpl());
		
		for (PersonImpl person : this.completedAgents) {
			pop.addPerson(person);
		}
		
		PopulationWriter popWriter = new PopulationWriter(pop, null);
		popWriter.writeStartPlans(filename);
		popWriter.writePersons();
		popWriter.writeEndPlans();
		
	}

	private static Counts readCountsFile(String filename) {		
		Counts counts = new Counts();		
		MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
		matsimCountsReader.readFile(filename);
		return counts;		
	}
	
	private static TransitSchedule readTransitSchedule(String transitScheduleFile, String networkFile) {
		
		ScenarioImpl scenario = new ScenarioImpl();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		
		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, scenario.getNetwork());
		
		try {
			transitScheduleReaderV1.readFile(transitScheduleFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return transitSchedule;
	}

	private void add344_H() {
		LinkedList<Id> b344_H = new LinkedList<Id>();
		this.lines.put("344_H", b344_H);
		
		b344_H.add(new IdImpl("792040.1"));
		b344_H.add(new IdImpl("792200.1"));
		b344_H.add(new IdImpl("792013.1"));
		b344_H.add(new IdImpl("792030.1"));
		b344_H.add(new IdImpl("792023.1"));
		b344_H.add(new IdImpl("792910.1"));
		b344_H.add(new IdImpl("781060.1"));
		b344_H.add(new IdImpl("781040.1"));			
	}

	private void add344_R() {
		LinkedList<Id> b344_R = new LinkedList<Id>();
		this.lines.put("344_R", b344_R);
		
		b344_R.add(new IdImpl("781015.2"));
		b344_R.add(new IdImpl("792910.2"));
		b344_R.add(new IdImpl("792023.2"));
		b344_R.add(new IdImpl("792030.2"));
		b344_R.add(new IdImpl("792013.2"));
		b344_R.add(new IdImpl("792200.2"));
		b344_R.add(new IdImpl("792040.2"));
	}
	
	private void addM44_H() {
		LinkedList<Id> m44_H = new LinkedList<Id>();
		this.lines.put("m44_H", m44_H);
		
		m44_H.add(new IdImpl("812020.1"));
		m44_H.add(new IdImpl("812550.1"));
		m44_H.add(new IdImpl("812030.1"));
		m44_H.add(new IdImpl("812560.1"));
		m44_H.add(new IdImpl("812570.1"));
		m44_H.add(new IdImpl("812013.1"));
		m44_H.add(new IdImpl("806520.1"));
		m44_H.add(new IdImpl("806030.1"));
		m44_H.add(new IdImpl("806010.1"));
		m44_H.add(new IdImpl("806540.1"));
		m44_H.add(new IdImpl("804070.1"));
		m44_H.add(new IdImpl("804060.1"));
		m44_H.add(new IdImpl("801020.1"));
		m44_H.add(new IdImpl("801030.1"));
		m44_H.add(new IdImpl("801530.1"));
		m44_H.add(new IdImpl("801040.1"));
		m44_H.add(new IdImpl("792050.1"));
		m44_H.add(new IdImpl("792200.3"));
	}
	
	private void addM44_R() {
		LinkedList<Id> m44_R = new LinkedList<Id>();
		this.lines.put("m44_R", m44_R);
		
		m44_R.add(new IdImpl("792200.4"));
		m44_R.add(new IdImpl("792050.2"));
		m44_R.add(new IdImpl("801040.2"));
		m44_R.add(new IdImpl("801530.2"));
		m44_R.add(new IdImpl("801030.2"));
		m44_R.add(new IdImpl("801020.2"));
		m44_R.add(new IdImpl("804060.2"));
		m44_R.add(new IdImpl("804070.2"));
		m44_R.add(new IdImpl("806540.2"));
		m44_R.add(new IdImpl("806010.2"));
		m44_R.add(new IdImpl("806030.2"));
		m44_R.add(new IdImpl("806520.2"));
		m44_R.add(new IdImpl("812013.2"));
		m44_R.add(new IdImpl("812570.2"));
		m44_R.add(new IdImpl("812560.2"));
		m44_R.add(new IdImpl("812030.2"));
		m44_R.add(new IdImpl("812550.2"));
		m44_R.add(new IdImpl("812020.2"));		
	}

	private void addLine(String lineID) {
		
		TransitLine transitLine = this.transitSchedule.getTransitLines().get(new IdImpl(lineID));
		LinkedList<Id> routeStops = new LinkedList<Id>();		
		
		for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
			if(transitRoute.getStops().size() > routeStops.size()){
				routeStops = new LinkedList<Id>();
				for (TransitRouteStop stop : transitRoute.getStops()) {
					routeStops.add(stop.getStopFacility().getId());
				}
			}
		}
		
		this.lines.put("344_H", routeStops);
		
	}

}
