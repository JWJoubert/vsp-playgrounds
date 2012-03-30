package playground.southafrica.gauteng;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.roadpricing.RoadPricingSchemeI;

import playground.kai.run.KaiAnalysisListener;
import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.routing.GautengTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.GautengScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

class PersonHouseholdMapping {
	Map<Id,Id> delegate = new HashMap<Id,Id>() ;
	// key = personId; value = householdId
	// Id hhId = personHouseholdMapping.get( personId ) ;

	Id getHhIdFromPersonId( Id personId ) {
		return delegate.get(personId) ;
	}
	Id insertPersonidHhidPair( Id personId, Id hhId ) {
		// maybe check if that key (= personId) is already taken.  Shoudl not happen.
		return delegate.put( personId, hhId) ;
	}
}

/**
 * Design comments:<ul>
 * <li> The money (toll) is converted into utils both for the router and for the scoring.
 * <li> However, setting up the toll scheme in terms of disutilities does not seem right.
 * </ul>
 *
 */
class GautengControler {
	static Logger log = Logger.getLogger(GautengControler.class) ;
	
	public static void main ( String[] args ) {
		if(args.length != 3){
			throw new RuntimeException("Must provide three arguments: config file path, base value of time (for cars) and multiplier for commercial vehicles.") ;
		}
		// Get arguments
		// Read the base Value-of-Time (VoT) for private cars, and the VoT multiplier from the arguments, johan Mar'12
		String configFileName = args[0] ;
//		String configFileName = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml" ;
		double baseValueOfTime = Double.parseDouble(args[1]);
		double valueOfTimeMultiplier = Double.parseDouble(args[2]);

		final Controler controler = new Controler( configFileName ) ;

		controler.setOverwriteFiles(true) ;
		
		Scenario sc = controler.getScenario();
		
//		constructPersonHhMappingAndInsertIntoScenario(sc);
		
		
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		
		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingSchemeI vehDepScheme = 
			new GautengRoadPricingScheme( sc.getConfig(), sc.getNetwork() , sc.getPopulation() );

		// CONSTRUCT UTILITY OF MONEY:
		
		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney( sc.getConfig().planCalcScore() , baseValueOfTime, valueOfTimeMultiplier) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( sc.getNetwork(), sc.getPopulation(), vehDepScheme) 
		) ;
		
		
		controler.setScoringFunctionFactory(
				new GautengScoringFunctionFactory(sc.getConfig(), sc.getNetwork(), personSpecificUtilityOfMoney )
		);

		// insert into routing:
		controler.setTravelDisutilityFactory( 
				new GautengTravelDisutilityInclTollFactory( vehDepScheme, personSpecificUtilityOfMoney ) 
		);
		
		
		
		// ADDITIONAL ANALYSIS:
		// This is not truly necessary.  It could be removed or copied in order to remove the dependency on the kai
		// playground.  For the time being, I (kai) would prefer to leave it the way it is since I am running the Gauteng
		// scenario and I don't want to maintain two separate analysis listeners.  But once that period is over, this
		// argument does no longer apply.  kai, mar'12
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		// RUN:
		controler.run();
	
	}

	private static void constructPersonHhMappingAndInsertIntoScenario(
			Scenario sc) {
		Households hhs = ((ScenarioImpl) sc).getHouseholds() ;
		
		PersonHouseholdMapping phm = new PersonHouseholdMapping() ;
		
		for ( Household hh : hhs.getHouseholds().values() ) {
			for ( Id personId : hh.getMemberIds() ) {
				phm.insertPersonidHhidPair( personId, hh.getId() ) ;
			}
		}
		sc.addScenarioElement( phm ) ;
		
		// retreive as follows:
		PersonHouseholdMapping retreivedPhm = sc.getScenarioElement( PersonHouseholdMapping.class ) ;
	}



}
