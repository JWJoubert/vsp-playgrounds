package playground.benjamin.income;

import org.matsim.core.scoring.interfaces.BasicScoring;

public class ScoringFromDailyIncome implements BasicScoring {

	private static double betaIncomeCar = 4.58;
	private double incomePerDay;

	public ScoringFromDailyIncome(double householdIncomePerDay) {
		this.incomePerDay = householdIncomePerDay;
	}


	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return betaIncomeCar * Math.log(this.incomePerDay);
	}

	@Override
	public void reset() {

	}


}
