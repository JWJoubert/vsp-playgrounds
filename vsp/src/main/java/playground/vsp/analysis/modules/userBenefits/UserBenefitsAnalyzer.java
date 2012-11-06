/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.userBenefits;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author ikaddoura
 *
 */
public class UserBenefitsAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(UserBenefitsAnalyzer.class);
	private ScenarioImpl scenario;
	private UserBenefitsCalculator userWelfareCalculator;
	
	private double allUsersLogSum;
	private int personWithNoValidPlanCnt;
	private Map<Id, Double> personId2Logsum;

	public UserBenefitsAnalyzer(String ptDriverPrefix) {
		super(UserBenefitsAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		
		this.userWelfareCalculator = new UserBenefitsCalculator(this.scenario.getConfig());
		this.userWelfareCalculator.reset();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		// nothing to return
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		this.allUsersLogSum = this.userWelfareCalculator.calculateLogsum(this.scenario.getPopulation());
		this.personWithNoValidPlanCnt = this.userWelfareCalculator.getNoValidPlanCnt();
		log.warn("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + personWithNoValidPlanCnt);
		this.personId2Logsum = this.userWelfareCalculator.getPersonId2Logsum();
	}

	@Override
	public void postProcessData() {
		// nothing to do
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "userBenefits.txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("monetary user benefits (all users logsum): " + this.allUsersLogSum);
			bw.newLine();
			bw.write("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + this.personWithNoValidPlanCnt);
			bw.newLine();
			
			bw.newLine();
			bw.write("userID \t monetary user logsum");
			bw.newLine();
			
			for (Id id : this.personId2Logsum.keySet()){
				String row = id + "\t" + this.personId2Logsum.get(id);
				bw.write(row);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
