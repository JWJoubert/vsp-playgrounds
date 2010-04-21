package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
//import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.xml.sax.SAXException;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.TransScenarioLoader;

/**routes scenario with a defined cost calculations and increasing parameters coefficients**/
public class AdaptedLauncher {

	public void route(String configFile) throws FileNotFoundException {
		
		//load scenario
		ScenarioImpl scenarioImpl = new TransScenarioLoader ().loadScenario(configFile); 
		
		if (!new File(scenarioImpl.getConfig().controler().getOutputDirectory()).exists()){
			throw new FileNotFoundException("Can not find output directory: " + scenarioImpl.getConfig().controler().getOutputDirectory());
		}
		
		String routedPlansFile = scenarioImpl.getConfig().controler().getOutputDirectory()+ "/routedPlan_" + PTValues.scenarioName;
		
		//Get rid of only car plans
		if (PTValues.noCarPlans){
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(scenarioImpl.getPopulation());
		}
		
		//fragment plans
		// yy why is this necessary here?  I would think it could be done just before output? kai, apr'10
		if (PTValues.fragmentPlans){
			scenarioImpl.setPopulation(new PlanFragmenter().run(scenarioImpl.getPopulation()));					
		}
		
		//route
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(scenarioImpl.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		// yy The following comes from PlansCalcRoute; in consequence, one can configure the car router attributes.  In addition, one can configure _some_
		// of the transit attributes from here (transitSchedule, transitConfig), but not some others.  Please describe the design reason for this.  kai, apr'10
		AdaptedPlansCalcTransitRoute adaptedRouter = new AdaptedPlansCalcTransitRoute(scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenarioImpl.getTransitSchedule(), transitConfig);

		adaptedRouter.run(scenarioImpl.getPopulation());
		
		//write 
		System.out.println("writing output plan file..." + routedPlansFile + "routedPlan_" + PTValues.scenarioName + ".xml");
		PopulationWriter popwriter = new PopulationWriter(scenarioImpl.getPopulation(), scenarioImpl.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		
		//compress
		if (PTValues.compressPlan){
			new FileCompressor().run(routedPlansFile);
		}
		
	}
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split plans?
		-cost coefficients
		-only plans inside the investigation area?
		-ATTENTION. There is not "cost calculator 2" anymore.
		*/
		
		String configFile = null;
		
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans_Berlin5x.xml";
		}
		
//		PTValues.timeCoefficient=  0.85;
//		PTValues.distanceCoefficient = 0.15;
		PTValues.fragmentPlans = true;
		PTValues.noCarPlans= true;
		PTValues.allowDirectWalks= true;
		PTValues.compressPlan = true;
//		PTValues.walkCoefficient = 1;
//		PTValues.scenarioName =  "_time" + PTValues.timeCoefficient + "_dist" + PTValues.distanceCoefficient;
		
		System.out.println(PTValues.scenarioName);
		new AdaptedLauncher().route(configFile);
		

		/*
		PTValues.routerCalculator = 3;
		for (double x= 0.95; x> -0.05; x = x - 0.05 ){
			PTValues.timeCoefficient=  Math.round(x*100)/100.0;
			PTValues.distanceCoefficient = Math.round((1-x)*100)/100.0;

			PTValues.fragmentPlans = true;
			PTValues.noCarPlans= true;
			PTValues.allowDirectWalks= true;
			PTValues.compressPlan = true;
			PTValues.walkCoefficient = 1;
			
			PTValues.scenarioName =  "_time" + PTValues.timeCoefficient + "_dist" + PTValues.distanceCoefficient;
			
			System.out.println(PTValues.scenarioName);
			AdaptedLauncher adaptedLauncher	= new AdaptedLauncher();
			adaptedLauncher.route(configFile);
		}
		*/
		
		//shut down ms-windows
		//Runtime runtime = Runtime.getRuntime();
		//runtime.exec("shutdown -s -t 60 -f");  
	}	
}


