/* *********************************************************************** *
 * project: org.matsim.*
 * MDSAM.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;


/**
 * Determines similarity of plans, based on 
 * - act chain sequence
 * - mode choice
 * - location choice.
 *
 * @author mfeil
 */
public class MDSAM {

	private final PopulationImpl population;
	private List<List<Double>> sims;
	private final double GWact, GWmode, GWlocation; 
	private static final Logger log = Logger.getLogger(MDSAM.class);
	private final String outputFile;
	private boolean printing;
	private long unidimensional, multidimensional;
	private int counter;


	public MDSAM(final PopulationImpl population) {
		this.population=population;
		this.GWact = 2.0;
		this.GWmode = 1.0;
		this.GWlocation = 1.0;
	//	this.outputFile = "./plans/plans_similarity.xls";	
		this.outputFile = "/home/baug/mfeil/data/mz/simlog.xls";	
		this.printing = false;
		this.unidimensional = 0;
		this.multidimensional = 0;
		this.counter = 0;
	}
	
	public List<List<Double>> runPopulation () {
		long overall = System.currentTimeMillis();
		log.info("Calculating similarity of plans of population...");
		
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new ArrayList<List<Double>>();
		}	
		
		this.sims = new ArrayList<List<Double>>();
		
		int counter=0;
		for (Person person : this.population.getPersons().values()) {
			counter++;
			if (counter>20) this.printing=false;
			
			this.sims.add(new ArrayList<Double>()); // store overall similarities of all persons' plans
			double [][] matrix = new double [person.getPlans().size()][person.getPlans().size()]; //store a person's similarities among plans
			
			for (int i=0;i<person.getPlans().size();i++){
				Plan plan = person.getPlans().get(i);
				
				if (this.printing){
					stream.println("Person "+person.getId());
					stream.println("origPlan");
					stream.print("Acts\t");
					for (int k=0;k<person.getSelectedPlan().getPlanElements().size();k+=2){
						stream.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getType()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()<plan.getPlanElements().size()){
						for (int z=0;z<(plan.getPlanElements().size()-person.getSelectedPlan().getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tModes\t");
					for (int k=1;k<person.getSelectedPlan().getPlanElements().size();k+=2){
						stream.print(((LegImpl)(person.getSelectedPlan().getPlanElements().get(k))).getMode()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()<plan.getPlanElements().size()){
						for (int z=0;z<(plan.getPlanElements().size()-person.getSelectedPlan().getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tLocations\t");
					for (int k=0;k<person.getSelectedPlan().getPlanElements().size();k+=2){
						stream.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getLinkId()+"\t");
					}
					stream.println();
				}
				
				double sim = 0;
				for (int j=0;j<person.getPlans().size();j++){
					if (i>j) sim += matrix[j][i];
					else if (i==j)	matrix[i][j] = 0;
					else {
						if (this.printing){
							stream.println("comparePlan");
							stream.print("Acts\t");
							for (int k=0;k<plan.getPlanElements().size();k+=2){
								stream.print(((ActivityImpl)(plan.getPlanElements().get(k))).getType()+"\t");
							}
							if (person.getSelectedPlan().getPlanElements().size()>plan.getPlanElements().size()){
								for (int z=0;z<(person.getSelectedPlan().getPlanElements().size()-plan.getPlanElements().size())/2;z++) stream.print("\t");
							}
							stream.print("\tModes\t");
							for (int k=1;k<plan.getPlanElements().size();k+=2){
								stream.print(((LegImpl)(plan.getPlanElements().get(k))).getMode()+"\t");
							}
							if (person.getSelectedPlan().getPlanElements().size()>plan.getPlanElements().size()){
								for (int z=0;z<(person.getSelectedPlan().getPlanElements().size()-plan.getPlanElements().size())/2;z++) stream.print("\t");
							}
							stream.print("\tLocations\t");
							for (int k=0;k<plan.getPlanElements().size();k+=2){
								stream.print(((ActivityImpl)(plan.getPlanElements().get(k))).getLinkId()+"\t");
							}
							stream.println();
						}
						matrix[i][j] = this.runPlans(person.getPlans().get(j), plan, stream);
						sim += matrix[i][j];
					}
				}
				this.sims.get(this.sims.size()-1).add(sim/person.getPlans().size());	
			}
		}
		stream.close();
		log.info("Overall runtime was "+(System.currentTimeMillis()-overall));
		log.info("Unidimensional runtime was "+this.unidimensional);
		log.info("Multidimensional runtime was "+this.multidimensional);
		log.info(this.counter+" plans run.");
		log.info("done...");
		return this.sims;
	}
	
	public double runPlans(Plan origPlan, Plan comparePlan, PrintStream stream){
		
		long runStartTime = System.currentTimeMillis();
		this.counter++;
		
		// Calculate tables per attribute dimension
		// Length is number of acts minus last home plus 0th position, or number of legs respectively
		
		double [][][] table = new double [3][origPlan.getPlanElements().size()/2+1][comparePlan.getPlanElements().size()/2+1];
		//double [][][] table = new double [1][origPlan.getPlanElements().size()/2+1][comparePlan.getPlanElements().size()/2+1];
		
		for (int k=0;k<table.length;k++){
			double GW = 0;
			if (k==0) GW = this.GWact;
			else if (k==1) GW = this.GWmode;
			else GW = this.GWlocation;
			// Levenshtein distance
			for (int i=0;i<table[k].length;i++){
				for (int j=0;j<table[k][0].length;j++){
					if (j==0){
						// margin orig plan
						table[k][i][j]= i * GW;
					}
					else {
						//margin compare plan
						if (i==0) table[k][i][j]= j * GW;
						else if (k==0) { // Activity type sequence
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getType(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getType(), table[k],i, j, GW);
						}
						else if (k==1){ // Modes
							table[k][i][j] = this.minPath(((LegImpl)(origPlan.getPlanElements().get((i-1)*2+1))).getMode(), ((LegImpl)(comparePlan.getPlanElements().get((j-1)*2+1))).getMode(), table[k],i, j, GW);
						}
						else { // Locations (via linkIDs)
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getLinkId(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getLinkId(), table[k],i, j, GW);
						}
					}
				}
			}
		}
		
		// Print tables
		if (this.printing){			
			for (int i=0;i<table[0].length;i++){
				for (int k=0;k<table.length;k++){
					if (k!=2) stream.print("\t");
					for (int j=0;j<table[k][0].length;j++){
						stream.print(table[k][i][j]+"\t");
					}
					if (table[0].length>table[0][0].length) {
						for (int z=0;z<table[0].length-table[0][0].length;z++) stream.print("\t");
					}
					stream.print("\t");
				}
				stream.println();
			}
			stream.println();
		}
		
		this.unidimensional += System.currentTimeMillis()-runStartTime;
		long trajectoryTime = System.currentTimeMillis();
	
		// Find one optimal trajectory close to the diagonal, for each attribute dimension
		ArrayList<int[]> oset = new ArrayList<int[]>();	// contains the operation and position
		ArrayList<ArrayList<Integer>> dimensions = new ArrayList<ArrayList<Integer>>(); // contains the attribute dimensions of operation and position
		
		
		
		for (int k=0;k<table.length;k++){
			double GW = 0;
			if (k==0) GW = this.GWact;
			else if (k==1) GW = this.GWmode;
			else GW = this.GWlocation;
			int i=table[k].length-1;
			int j=table[k][0].length-1;
			boolean goLeft = true;
			while (i!=0 || j!=0){
				if (i>0 && j>0 && table[k][i-1][j-1]>=table[k][i][j]-GW){
					//System.out.println("Identity.");
					i--;
					j--;
				}
				// check insertion {1,x}
				else if (j>0 &&	table[k][i][j-1]==table[k][i][j]-GW && osetContains(oset,dimensions,k,1,j)){
					//System.out.println("Insertion.");
					j--;
				}
				// check deletion {2,x}
				else if (i>0 &&	table[k][i-1][j]==table[k][i][j]-GW && osetContains(oset,dimensions,k,2,i)){
					//System.out.println("Deletion.");
					i--;
				}
				// go new path (insertion) in zick zack
				else if (goLeft && j>0) {
					oset.add(new int[]{1,j});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					j--;
					goLeft = false;
					//System.out.println("New insertion.");
				}
				// go new path (deletion) in zick zack
				else if (!goLeft && i>0){
					oset.add(new int[]{2,i});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					i--;
					goLeft = true;
					//System.out.println("New deletion.");
				}
				// go new path (insertion) 
				else if (j>0) {
					oset.add(new int[]{1,j});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					j--;
					//System.out.println("New insertion.");
				}
				// go new path (deletion)
				else {
					oset.add(new int[]{2,i});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					i--;
					//System.out.println("New deletion.");
				}
			}
		}
		
		// Print arrays
		if (this.printing){ 
			for (int m=0;m<oset.size();m++){
				stream.print("("+oset.get(m)[0]+","+oset.get(m)[1]+")");
				for (int n=0;n<dimensions.get(m).size();n++) stream.print(", "+dimensions.get(m).get(n));
				stream.println();
			}
		}
		
		
		double sum=0;
		for (int m=0;m<oset.size();m++){
			double GW=0;
			if (dimensions.get(m).contains(0)) GW = this.GWact; 
			if (dimensions.get(m).contains(1)) GW = java.lang.Math.max(GW, this.GWmode);
			if (dimensions.get(m).contains(2)) GW = java.lang.Math.max(GW, this.GWlocation);
			sum += GW;
		}
		if (this.printing){
			stream.println("Sum is "+sum);
			stream.println();
		}
		this.multidimensional += System.currentTimeMillis()-trajectoryTime;
		return sum;
	}
	
	private boolean osetContains (ArrayList<int[]> oset, ArrayList<ArrayList<Integer>> dimensions ,int k, int operation, int position){
		for (int m=0;m<oset.size();m++){
			if (oset.get(m)[0]==operation && oset.get(m)[1]==position){
				dimensions.get(m).add(k);
				return true;
			}
		}
		return false;
	}
	
	private double minPath(Object orig, Object compare, double[][]table, int i, int j, double GW){
		double del = table[i-1][j]+GW;
		double ins = table[i][j-1]+GW;
		double sub = Double.MAX_VALUE;
		
		// identity (position-sensitive)
		if (orig.equals(compare)){
			sub =  table[i-1][j-1] + GW/java.lang.Math.max(table.length-1, table[0].length-1)*java.lang.Math.abs(i-j);
		}
		// substitution
		else {
			sub = table[i-1][j-1] + 2 * GW;	
		}
		// return minimum of ins, del, sub
		del = java.lang.Math.min(del, ins);		
		return java.lang.Math.min(del, sub);
	}
}

