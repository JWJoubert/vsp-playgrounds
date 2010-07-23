package playground.mmoyo.Validators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;

import playground.mmoyo.utils.FileCompressor;

public class PlanValidator {
	
	/**returns true if first act type is the same as last act type*/
	public boolean hasSameActType (Plan plan){
		Activity firstAct = (Activity) plan.getPlanElements().get(0);
		Activity lastAct = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
		return (firstAct.getType().equals(lastAct.getType()));
	}
	
	/**deletes plans having different type for initial and last act.*/ 
	public Population delDiffTypeActs(Population population){
		List<Id> personsToDel = new ArrayList<Id>();
		for(Person person: population.getPersons().values() ){
			List<Plan> plansToDel = new ArrayList<Plan>();
			for (Plan plan: person.getPlans()){
				if (!hasSameActType(plan)) plansToDel.add(plan);
			}
			person.getPlans().removeAll(plansToDel);
			if (person.getPlans().size()==0) personsToDel.add(person.getId());
		}
		
		//remove persons without plans
		for (Id personId: personsToDel){
			population.getPersons().remove(personId);
		}
		
		return population;
	}
	
	/** shows the number of plans with different types in initial and last act */
	private void detectDiffTypePlans(Population population){
		final String WARNING = "Different type of initial and last activity of agent: ";
		int dif =0;
		for(Entry<Id, ? extends Person> entry: population.getPersons().entrySet() ){
			Id id = entry.getKey();
			PersonImpl value = (PersonImpl)entry.getValue();
			for (Plan plan: value.getPlans()){
				if(!hasSameActType (plan)){
					System.out.println(WARNING + id );
					dif++;
				}
			}
		}
		double size = population.getPersons().size();
		double ddif = dif;
		System.out.println("total plans: " + population.getPersons().size() + " discrepancies: "+ dif +   " percentaje: " + (ddif/size)*100 );
	}
	
	private void detectManyPlans(final String directoryPath){
		File directory = new File(directoryPath);
		final String SEPARATOR = "/";
		if (directory.exists()){
			for (String planFileName: directory.list()){    
				File file = new File (directoryPath + SEPARATOR + planFileName);
				if (file.isFile()){
					System.out.println (file.getPath());
					Population population = readPopulation(file.getPath());
					detectDiffTypePlans(population);  //-> validate first that this is a plan file	
				}
			}
		}
	}
	
	private Population readPopulation(String planFile){
		Scenario scenario = new ScenarioImpl();
		new MatsimPopulationReader(scenario).readFile(planFile);
		return scenario.getPopulation();
	}

	/**Writes a new population file filtered out of plans with diff initial and final act types*/ 
	public void writeFilteredPlan(String configFile){
		Scenario scenario =  new ScenarioLoaderFactoryImpl().createScenarioLoader(configFile).loadScenario();
		Network network = scenario.getNetwork();
		Population origPopulation = scenario.getPopulation();
		Population filteredPopulation = this.delDiffTypeActs(origPopulation);
		File inputFile = new File(scenario.getConfig().plans().getInputFile());
		String newFile= scenario.getConfig().controler().getOutputDirectory() + inputFile.getName() + "filt_acts.xml" ;
		new PopulationWriter(filteredPopulation, network).write(newFile) ;
		new FileCompressor().run(newFile);
	}
	
	/**validates all plans inside a given directory*/
	public static void main(String[] args) {
		//->  attention: distinguish automatically between directory and plan file: MatsimFileTypeGuesser
		
		//validate many files in a directory
		/*
		String directoryPath = null;
		if (args.length==1){
			directoryPath = args[0];
		}else{
			directoryPath = "../playgrounds/mmoyo/output/eightth/output";
		}
		new PlanValidator().validateMany (directoryPath);
		*/

		//validate one file
		String file = null;
		if (args.length==1){
			file = args[0];
		}else{
			//file = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_10x_subset_xy2links.xml.gz";
			file = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}
		PlanValidator planValidator = new PlanValidator();

		/*
		Population population = planValidator.readPopulation(planFile);
		planValidator.detectDiffTypePlans(population);
		*/
		planValidator.writeFilteredPlan(file);
	}
}
