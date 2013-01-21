package playground.anhorni.csestimation;

import java.text.DecimalFormat;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class SurveyAnalyzer {
	private final static Logger log = Logger.getLogger(SurveyAnalyzer.class);	
	private TreeMap<Id, EstimationPerson> population;
	private String outdir;
	private DecimalFormat formatter = new DecimalFormat("0.0");
	
	public SurveyAnalyzer(TreeMap<Id, EstimationPerson> population, String outdir) {
		this.population = population;
		this.outdir = outdir;
	}
	
	public SurveyAnalyzer(Population population, String outdir) {
		this.population = new TreeMap<Id, EstimationPerson>();
		for (Person p:population.getPersons().values()) {
			EstimationPerson person = (EstimationPerson)p;
			this.population.put(p.getId(), person);		
		}
		this.outdir = outdir;
	} 
	
	private void analyzeVariableBinSizeMZ() {	
		double totalGenderWeight = 0.0;
		double totalAgeWeight = 0.0;
		double totalIncomeWeight = 0.0;
		
		for (EstimationPerson p : this.population.values()) {
			double weight = p.getWeight();
			int age = p.getAge();
			
			if (age >= 18) {
				totalAgeWeight += weight;
			}
			double income = p.getHhIncome();
			if (income > 0.0) { // exclude missing values
				totalIncomeWeight += weight;
			}
			String gender = p.getSex();
			if (gender.contains("f") || gender.contains("m")) {
				totalGenderWeight += weight;
			}
			
		}
		double genders[] = {0.0, 0.0};
		double incomes[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double ages[] = {0.0, 0.0, 0.0, 0.0};
		
		for (EstimationPerson p : this.population.values()) {
			double weight = p.getWeight();
			if (p.getAge() > 0) {
				if (p.getAge() > 0) {
					double age = p.getAge();
					if (age >= 18 && age <= 35) {
						ages[0] += weight / totalAgeWeight * 100.0;
					}
					else if (age >= 26 && age <= 50) {
						ages[1] += weight / totalAgeWeight * 100.0;
					}
					else if (age >= 51 && age <= 65) {
						ages[2] += weight / totalAgeWeight * 100.0;
					}
					else if (age > 65) {
						ages[3] += weight / totalAgeWeight * 100.0;
					}				
				}				
			}
			int incomeCat = (int)(p.getHhIncome() - 1);
			if (incomeCat >= 0) {
				incomes[incomeCat] += weight / totalIncomeWeight * 100.0;
			}
			if (p.getSex().equals("m")) {
				genders[0] += weight / totalGenderWeight * 100.0;
			}
			else if (p.getSex().equals("f")) {
				genders[1] += weight / totalGenderWeight * 100.0;
			}
		}
		for (int i = 0; i < ages.length; i++) {
			log.info("age bin " + i + ": " + formatter.format(ages[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < genders.length; i++) {
			log.info("gender bin " + i + ": " + formatter.format(genders[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < incomes.length; i++) {
			log.info("incomes bin " + i + ": " + formatter.format(incomes[i]));
		}
		log.info("-------------------------");
	}
	
	private void analyzeVariableBinSize() {
		double totalIncomeWeight = 0.0;
		double totalAgeWeight = 0.0;
		double totalGenderWeight = 0.0;
		
		for (EstimationPerson p:this.population.values()) {
			double weight = p.getWeight();
			double age = p.getAge();
			if (age >= 18) {
				totalAgeWeight += weight;
			}
			double income = p.getHhIncome();
			if (income > 0.0) { // exclude missing values
				totalIncomeWeight += weight;
			}
			String gender = p.getSex();
			if (gender.contains("f") || gender.contains("m")) {
				totalGenderWeight += weight;
			}
			
		}		
		double genders[] = {0.0, 0.0};
		double incomes[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		double ages[] = {0.0, 0.0, 0.0, 0.0};
		
		for (EstimationPerson p:this.population.values()) {
			double weight = p.getWeight();
			if (p.getAge() > 0) {
				double age = p.getAge();
				if (age >= 18 && age <= 35) {
					ages[0] += weight / totalAgeWeight * 100.0;
				}
				else if (age >= 26 && age <= 50) {
					ages[1] += weight / totalAgeWeight * 100.0;
				}
				else if (age >= 51 && age <= 65) {
					ages[2] += weight / totalAgeWeight * 100.0;
				}
				else if (age > 65) {
					ages[3] += weight / totalAgeWeight * 100.0;
				}				
			}
			double income = p.getHhIncome();
			if (income >= 0.0) {
				if (income < 2000) {
					incomes[0] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 2000 && income <= 4000) {
					incomes[1] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 4001 && income <= 6000) {
					incomes[2] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 6001 && income <= 8000) {
					incomes[3] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 8001 && income <= 10000) {
					incomes[4] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 10001 && income <= 12000) {
					incomes[5] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 12001 && income <= 14000) {
					incomes[6] += weight / totalIncomeWeight * 100.0;
				}
				else if (income >= 14001 && income <= 16000) {
					incomes[7] += weight / totalIncomeWeight * 100.0;
				}
				else if (income > 16000) {
					incomes[8] += weight / totalIncomeWeight * 100.0;
				}
			}
			if (p.getSex().equals("m")) {
				genders[0] += weight / totalGenderWeight * 100.0;
			}
			else if (p.getSex().equals("f")) {
				genders[1] += weight / totalGenderWeight * 100.0;
			}
		}
		for (int i = 0; i < ages.length; i++) {
			log.info("age bin " + i + ": " + formatter.format(ages[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < genders.length; i++) {
			log.info("gender bin " + i + ": " + formatter.format(genders[i]));
		}
		log.info("-------------------------");
		for (int i = 0; i < incomes.length; i++) {
			log.info("incomes bin " + i + ": " + formatter.format(incomes[i]));
		}
		log.info("-------------------------");
	}
	
	public void analyzeMZ() {
		log.info("Analyzing " + this.population.size() + " persons");
		this.analyzeVariableBinSizeMZ();
		
		Bins ageBins = new Bins(10, 100, "age"); //	interval, maxVal
		Bins incomeBins = new Bins(1, 9, "income");
		
		for (EstimationPerson p:this.population.values()) {				
			if (p.getAge() > 0) {
				ageBins.addVal(p.getAge(), p.getWeight());
			}			
			if (p.getHhIncome() > 0.0) {
				incomeBins.addVal(p.getHhIncome() - 1.0, p.getWeight());
			}	
		}
		ageBins.plotBinnedDistribution(outdir, "age bins", " year");
		incomeBins.plotBinnedDistribution(outdir, "income bins", " income cat");		
	}
	
	public void analyze() {
		this.analyzeVariableBinSize();
	}
}
