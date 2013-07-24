package playground.wrashid.PHEV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;

public class CreatePlans {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO: am schluss alle meiste pfade in config.xml reintun...
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/config.xml";
		Config config = ConfigUtils.loadConfig(args[0]);
//		config.plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/5000plan/plans.xml");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		Population plans = scenario.getPopulation();
		Knowledges knowledges = scenario.getKnowledges();

		// read facilities
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/facilities/facilities.xml");


		// get home and work activity
		ActivityOption home=null;
		ActivityOption work=null;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOption a = a_it.next();
				//System.out.println(a.getType());
				if (a.getType().equals("home")) {
					home=a;
				} else if (a.getType().equals("work")){
					work=a;
				}
			}
		}






		// create 100 persons
		for (int i=0;i<5000;i++){
			PersonImpl person = new PersonImpl(new IdImpl(i));
			plans.addPerson(person);


			KnowledgeImpl k = knowledges.getFactory().createKnowledge(person.getId(), "");
			k.addActivityOption(home, false);
			k.addActivityOption(work, false);

			PlanImpl plan = person.createAndAddPlan(true);
			ActivityFacility home_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("home").get(0).getFacility();
			ActivityFacility work_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("work").get(0).getFacility();
			ArrayList<? extends ActivityOption> acts = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities();

			double depTime=3600*8;
			double duration=3600*8;

			ActivityImpl a = plan.createAndAddActivity("home",home_facility.getCoord());
			a.setLinkId(home_facility.getLinkId());
			a.setEndTime(depTime);
			LegImpl l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTime);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTime);
			a = plan.createAndAddActivity("work",work_facility.getCoord());
			a.setLinkId(work_facility.getLinkId());
			a.setStartTime(depTime);
			a.setEndTime(depTime+duration);
			a.setMaximumDuration(duration);
			l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTime+duration);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTime+duration);
			a = plan.createAndAddActivity("home",home_facility.getCoord());
			a.setLinkId(home_facility.getLinkId());
			// assign home-work-home activities to each person


//			Leg l=null;

		}



		new PopulationWriter(plans, scenario.getNetwork()).write(null);//config.plans().getOutputFile());
	}

}
