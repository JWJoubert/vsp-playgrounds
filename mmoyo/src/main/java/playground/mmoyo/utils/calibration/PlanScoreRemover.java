package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.mmoyo.utils.DataLoader;

public class PlanScoreRemover {

	public void run(Population population){
		System.out.println("removing scores" + population.getPersons().size());
		for (Person person : population.getPersons().values()) {
			for (Plan plan: person.getPlans()){
				plan.setScore(null);
			}
		}
	}
	
	private void setScores2Zero(Population population){
		System.out.println("Setting all scores to zero: " + population.getPersons().size());
		Double zero = 0.0;
		for (Person person : population.getPersons().values()) {
			for (Plan plan :person.getPlans()){
				plan.setScore(zero);
			}
		}
	}

	public static void main(String[] args) {
		String populationFile = "../playgrounds/mmoyo/output/precalculation/setNull.xml";
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String outputFile = "../playgrounds/mmoyo/output/test/setNullwoSCORES.xml";
		
		ScenarioImpl scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
	
		new PlanScoreRemover().run(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
