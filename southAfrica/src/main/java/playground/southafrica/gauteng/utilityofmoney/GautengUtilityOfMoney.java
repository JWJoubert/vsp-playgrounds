/**
 * 
 */
package playground.southafrica.gauteng.utilityofmoney;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor.Type;

/**
 * Calculates the utility of money from a given Value of Time (VoT). 
 * @author nagel
 * @author jwjoubert
 */
public class GautengUtilityOfMoney implements UtilityOfMoneyI {
	
	private final Logger log = Logger.getLogger(GautengUtilityOfMoney.class);
	private PlanCalcScoreConfigGroup planCalcScore;
	private final double baseValueOfTime;
	private final double commercialMultiplier;

	/**
	 * Class to calculate the marginal utility of money (beta_money) for 
	 * different vehicle types given the value of time (VoT).  
	 * @param cnScoringGroup
	 * @param baseValueOfTime expressed in Currency/hr that is currently (March 
	 * 2012) used for private cars, taxis and external traffic. 
	 * @param valueOfTimeMultiplier to inflate the base VoT for heavy commercial 
	 * vehicles. A multiplier of <i>half</i> this value is used for busses and
	 * smaller commercial vehicles.
	 */
	public GautengUtilityOfMoney( final PlanCalcScoreConfigGroup cnScoringGroup, double baseValueOfTime, double valueOfTimeMultiplier ) {
		this.planCalcScore = cnScoringGroup ;
		log.warn("Value of Time (VoT) used as base: " + baseValueOfTime) ;
		log.warn("Value of Time multiplier: " + valueOfTimeMultiplier) ;
		
		for ( Type vehType : Type.values() ) {
			log.info( " vehType: " + vehType.toString() 
					+ "; utility of travel time savings per hr: " + getUtilityOfTravelTime_hr()
					+ "; value of travel time savings per hr: " + getValueOfTime_hr(vehType)
					+ "; => utility of money: " + getUtilityOfMoneyFromValueOfTime( getValueOfTime_hr(vehType)) ) ;
		}
		this.baseValueOfTime = baseValueOfTime ;
		this.commercialMultiplier = valueOfTimeMultiplier ;
	}

	public double getUtilityOfMoney_normally_positive(final Id personId ) {
		Type vehicleType = SanralTollFactor.typeOf(personId);
		double valueOfTime_hr = getValueOfTime_hr(vehicleType);
		double utilityOfMoney = getUtilityOfMoneyFromValueOfTime(valueOfTime_hr);
		
		return utilityOfMoney ;
	}

	private double getUtilityOfMoneyFromValueOfTime(double valueOfTime_hr) {
		final double utilityOfTravelTime_hr = getUtilityOfTravelTime_hr();
	
		double utilityOfMoney = utilityOfTravelTime_hr / valueOfTime_hr ;
		return utilityOfMoney;
	}

	private double getUtilityOfTravelTime_hr() {
		final double utilityOfTravelTime_hr = 
			this.planCalcScore.getPerforming_utils_hr() - this.planCalcScore.getTraveling_utils_hr() ;
		// "performing" is normally positive
		// "traveling" is normally negative
		return utilityOfTravelTime_hr;
	}

	private double getValueOfTime_hr(Type vehicleType) {
		double valueOfTime_hr = baseValueOfTime ;
		switch( vehicleType ) {
		case carWithTag:
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
		case commercialClassBWithoutTag:
		case busWithTag:
		case busWithoutTag:
			valueOfTime_hr = baseValueOfTime*0.5*commercialMultiplier ; 
			break;
		case commercialClassCWithTag:
		case commercialClassCWithoutTag:
			valueOfTime_hr = baseValueOfTime*commercialMultiplier ; 
			break ;
		case taxiWithTag:
		case taxiWithoutTag:
		case extWithTag:
		case extWithoutTag:
			valueOfTime_hr = baseValueOfTime;
			break ;
		}
		return valueOfTime_hr;
	}

}
