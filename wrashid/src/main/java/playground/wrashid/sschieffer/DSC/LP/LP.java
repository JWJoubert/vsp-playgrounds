package playground.wrashid.sschieffer.DSC.LP;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;

public abstract class LP {

	
	private Schedule schedule;
	
	private LpSolve solver; 
	
	private int numberOfVariables;
	private Id personId;
	private double energyFromCombustionEngine;
	
	private double batterySize;
	private double  batteryMin;
	private double  batteryMax;
	
	
	private boolean output;
	
	public LP(boolean output){		
		this.output=output;
		
	}
	
	public Schedule getSchedule(){
		return schedule;
	}
	
	protected Id getPersonId(){
		return personId;
	}
	
	protected boolean isOutput(){
		return output;
	}
	
	
	protected LpSolve getSolver(){
		return solver;
	}
	
	
	public void solveLP(Schedule schedule, 
			Id id, 
			double batterySize, 
			double batteryMin, 
			double batteryMax
			) throws LpSolveException {
		
		this.batteryMax=batteryMax;
		this.batteryMin=batteryMin;
		this.batterySize=batterySize;
		this.personId=id;
		this.schedule=schedule;
		
		setEnergyFromCombustionEngine(0);
		
		numberOfVariables= getSchedule().getNumberOfEntries()+1;
		solver = LpSolve.makeLp(0, numberOfVariables);
		}
	
	
	
	public void printSolution() throws LpSolveException{
		double [] solutionNF =solver.getPtrVariables();
		
		System.out.println("solution ");
		for(int i=0; i<solutionNF.length; i++){
			System.out.println(solutionNF[i] + ",  ");
		}
	}
	
	public void setInequalityContraintsForBatteryUpperLower() throws LpSolveException{
		// at all points should be within battery limit
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			String inequality=setInEqualityBatteryConstraint(i);
			solver.strAddConstraint(inequality, LpSolve.LE, batterySize*batteryMax);
			solver.strAddConstraint(inequality, LpSolve.GE, batterySize*batteryMin);
			
		}
	}
	
	public void setInequalityContraintsForBatteryUpper(double reducedBy, int startingAtIntervalI) throws LpSolveException{
		// at all points should be within battery limit
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			String inequality=setInEqualityBatteryConstraint(i);
			if(i<startingAtIntervalI){
				solver.strAddConstraint(inequality, LpSolve.LE, batterySize*batteryMax);
			}else{
				solver.strAddConstraint(inequality, LpSolve.LE, batterySize*batteryMax-reducedBy);
			}
			
		}
	}
	
	
	/**
	 * sets
	 * totalConsumption<=totalCharged<=totalConsumption*(1+buffer)
	 * where totalConsumption
	 * =chargingSpeed* (1 1 0 1)*(parkingTime parkingTime DrivingTime parkingTime)'
	 * @param buffer
	 * @throws LpSolveException 
	 */
	public void setTotalChargedGreaterEqualTotalConsumptionAndSmallerThanBuffer(double limit) throws LpSolveException{
		String inequality="0 ";
		double totalConsumption=0;
		//schedule.printSchedule();
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
				String s= Double.toString(thisParkingInterval.getChargingSpeed());
				s= s.concat(" ");
				inequality=inequality.concat(s);
				
			}else{
				if(schedule.timesInSchedule.get(i).isDriving()){
					DrivingInterval thisDrivingInterval= (DrivingInterval)schedule.timesInSchedule.get(i);
					totalConsumption+=thisDrivingInterval.getTotalConsumption();
					inequality=inequality.concat("0 ");
				}
				
			}
		}
		
		solver.strAddConstraint(inequality, LpSolve.GE, totalConsumption);
		solver.strAddConstraint(inequality, LpSolve.LE, totalConsumption*(1+limit));
	}
	
	
	/**
	 * sets objective function
	 * 
	 * minimizing time in peak hours
	 * minimizing (-)*charging in off peak hours
	 * @throws LpSolveException
	 */
	protected void setObjectiveFunction() throws LpSolveException{
		double [] objective= new double[numberOfVariables];
		//starting SOC is chosen freely with no constraints
		objective[0]= 0;
		
		//weights for parking intervals to encourage or discourage charging
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			// if Parking interval
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
				
				if(thisParkingInterval.isInSystemOptimalChargingTime()){
					
					double weightOptimal=calculateWeightOptimal(thisParkingInterval);
					objective[ 1+i] = weightOptimal;
					
					}
				
				else{
					// want to minimize charging in this time. thus need to minimize the absolute value of the weight
					double weightSubOptimal=calculateWeightSubOptimal(thisParkingInterval);
					objective[ 1+i] = weightSubOptimal;
					
				}
			}else{//Driving
				
				objective[ 1+i] = 0;				
				
			}
		}
		
		
		// now loop to add maximize SOC after consumption,
		// setDrivingConsumptionSmallerSOC Inequality
		for(int i=0; i<schedule.numberOfDrivingTimes();i++){
			objective=objectiveToMinimizeCombustionEngineUse(objective, i);
		}
		
		// maximizes SOC right before first driving time
		objectiveToMaximizeSOCBeforeFirstDriving(objective);
		
		String objectiveStr= makeStringObjectiveFromDoubleObjective(objective);
	
		solver.strSetObjFn(objectiveStr);	
		
		solver.setMinim(); //minimize the objective
	}
	
	
	
	
	/**
	 * weight is sum of free Joules in interval*(-1) /total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	protected double calculateWeightOptimal(ParkingInterval thisParkingInterval){
		// want to maximize charging in this time. thus need to minimize the negative of the weight
		
		return (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
		//return (-1 )*Math.pow(1.1, (10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes));
	}
	
	
	
	/**
	 * weight is sum of free Joules in interval /total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	protected double calculateWeightSubOptimal(ParkingInterval thisParkingInterval){
		// TODO or to think... add constant term or not?
		// negative joules/negative total = positive
		
		return thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
		//return Math.pow(1.1, 10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes) ;
	}
	
	
	
	/**
	 * returns String for inequality vector
	 * (1  charging speed  -consumption  charging speed  0 0 0) *x <SOC
	 * @param threshold
	 * @return
	 */
	private String setInEqualityBatteryConstraint(int threshold){
		
		String objectiveStr="1 ";// first entry for SOC
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			
			
			if (i<=threshold){
				if(schedule.timesInSchedule.get(i).isParking()){
					ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
					String s= Double.toString(thisParkingInterval.getChargingSpeed());
					s= s.concat(" ");
					objectiveStr=objectiveStr.concat(s);
					
					}
				if(schedule.timesInSchedule.get(i).isDriving() ){
					DrivingInterval thisDrivingInterval= (DrivingInterval)schedule.timesInSchedule.get(i);
					String s= Double.toString((thisDrivingInterval.getTotalConsumption())*(-1));
					s= s.concat(" ");
					objectiveStr=objectiveStr.concat(s);
					
					
					}
				
			}else{
				objectiveStr=objectiveStr.concat("0 ");
			}
		}
		
		return objectiveStr;
	}

	
	/**
	 * sets upper and lower bounds on all variables
	 * 0<SOC<battery capacity
	 * 0 <t< parking timeS
	 * 
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @throws LpSolveException
	 */
	protected void setLowerAndUpperBounds() throws LpSolveException{
		solver.setLowbo(1,batterySize*batteryMin);
		solver.setUpbo(1,batterySize*batteryMax);
		
		for(int i=2; i<=numberOfVariables; i++){
			if(schedule.timesInSchedule.get(i-2).isParking()){
				solver.setLowbo(i, 0);
				solver.setUpbo(i, schedule.timesInSchedule.get(i-2).getIntervalLength());
			}else{
				// Driving times
				solver.setLowbo(i, 1);
				solver.setUpbo(i, 1);
			}
			
		}
	}
	
	
	
	/**
	 * sets lower and upper bounds on all variables: 
	 * SOC= given Starting SOC,
	 * 0 <t< parking timeS
	 * 
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @throws LpSolveException
	 */
	protected void setLowerAndUpperBoundsWithStartingSOC(double startingSOC) throws LpSolveException{
		setLowerAndUpperBounds();
		//overwrite first entry
		getSolver().setLowbo(1, startingSOC);
		getSolver().setUpbo(1, startingSOC);
		
		
	}
	
	
	
	/**
	 * updates the required charging times in agentSchedules according to results of LP
	 * @return
	 * @throws LpSolveException
	 */
	protected Schedule update() throws LpSolveException{
		double[] solution = solver.getPtrVariables();
		
		schedule.setStartingSOC(solution[0]);
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				
				ParkingInterval thisParking= (ParkingInterval) schedule.timesInSchedule.get(i);
				
				if(solution[i+1]>0.0){
					 thisParking.setRequiredChargingDuration(solution[i+1]);
					
				}else{
					// in case LP has some problem and a negative number is the result
					 thisParking.setRequiredChargingDuration(0);
					 thisParking.setChargingSchedule(null);
				}
				
			}
		}
		return schedule;
	}
	
	
	

	
	
	
	public double getEnergyFromCombustionEngine(){
		return energyFromCombustionEngine;
	}

	public void setEnergyFromCombustionEngine(double energyFromCombustionEngine){
		this.energyFromCombustionEngine=energyFromCombustionEngine;
	}

	
	public void setSchedule(Schedule s){
		schedule=s;
	}
	
	
	
	/**
	 * modifies objective double array such that the SOC right after every driving trip is maximized
	 * the battery of the PHEV is not bounded by minBattery restrictions
	 * thus we have to ensure otherwise that energy is preferably charged from the battery
	 * 
	 * 
	 * @param objective array of coefficients from other objective restrictions so far
	 * @param a = ath driving time starting at 0th
	 * @return
	 * @throws LpSolveException
	 */
	private double[] objectiveToMinimizeCombustionEngineUse(double [] objective, int a) throws LpSolveException{
		
		objective[0]+=-1;
		
		int pos=schedule.positionOfIthDrivingTime(a);
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			if(i<=pos){
				
				if(schedule.timesInSchedule.get(i).isParking()){
					objective[1+i]+= (-1)* ((ParkingInterval)schedule.timesInSchedule.get(i)).getChargingSpeed();
					
				}
				
				if(schedule.timesInSchedule.get(i).isDriving()){
					objective[1+i]+= ((DrivingInterval)schedule.timesInSchedule.get(i)).getTotalConsumption();
					
				}
				
			}
		}
		
		return objective;
		
	}
	
	
	private double[] objectiveToMaximizeSOCBeforeFirstDriving(double [] objective) throws LpSolveException{
		
				
		int pos=schedule.positionOfIthDrivingTime(0);
		//SOC at timex= SOC+SUM(chargingtimes*chargingSpeed)
		//maximize SOC==> minimize -SOC
		objective[0]+= (-1);
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			if(i<pos){
				
				if(schedule.timesInSchedule.get(i).isParking()){
					objective[1+i]+= (-1)* ((ParkingInterval)schedule.timesInSchedule.get(i)).getChargingSpeed();
					
				}
				
			}
		}
		
		return objective;
		
	}
	
	
	/**
	 * turns a double array into a string separated by spaces
	 * 
	 * @param objective
	 * @return
	 */
	private String makeStringObjectiveFromDoubleObjective(double[]  objective){
		String s="";
		
		for(int i=0; i<objective.length; i++){
			s= s.concat(Double.toString(objective[i]) + " ");
		}
		return s;
	}
	
	
	
public void visualizeSOCAgentWithAndWithoutNonBattery(double [] solution, String filename1,String filename2, Id id) throws LpSolveException, IOException{
		
		XYSeriesCollection SOCAgent= new XYSeriesCollection();
		XYSeriesCollection SOCAgentBat= new XYSeriesCollection();
		
		XYSeries SOCAgentSeries= new XYSeries("SOC agent total consumption"+ id.toString());
		XYSeries SOCAgentSeriesBat= new XYSeries("SOC agent battery"+ id.toString());
		
		
		double [] SOC= solution.clone();
		double [] SOCBat= solution.clone();	
		
		SOCAgentSeries.add(schedule.timesInSchedule.get(0).getStartTime(),SOC[0]);
		SOCAgentSeriesBat.add(schedule.timesInSchedule.get(0).getStartTime(),SOC[0]);
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisP= (ParkingInterval)schedule.timesInSchedule.get(i);
				SOC[i+1]= SOC[i]+thisP.getChargingSpeed()	*solution[1+i];
				SOCBat[i+1]= SOCBat[i]+thisP.getChargingSpeed()	*solution[1+i];
				// add
				SOCAgentSeries.add(thisP.getEndTime(),SOC[i+1]);
				SOCAgentSeriesBat.add(thisP.getEndTime(),SOCBat[i+1]);
			}else{
				//subtract
				DrivingInterval thisD = (DrivingInterval)schedule.timesInSchedule.get(i);
				
				SOC[i+1]=SOC[i] - (thisD).getTotalConsumption();
				SOCBat[i+1]=SOCBat[i] - (thisD).getBatteryConsumption();
				
				SOCAgentSeries.add(thisD.getEndTime(),SOC[i+1]);
				SOCAgentSeriesBat.add(thisD.getEndTime(),SOCBat[i+1]);
			}
		}
		
		SOCAgent.addSeries(SOCAgentSeries);
		SOCAgentBat.addSeries(SOCAgentSeriesBat);
		
		plotSeries(SOCAgent, filename1,  id);
		plotSeries(SOCAgentBat, filename2,  id);
		
	  	
	}
	
	
public void visualizeSOCAgent(double [] solution, String filename1, Id id) throws LpSolveException, IOException{
	
	XYSeriesCollection SOCAgent= new XYSeriesCollection();
	
	XYSeries SOCAgentSeries= new XYSeries("SOC agent total consumption"+ id.toString());
	
	
	double [] SOC= solution.clone();
	
	SOCAgentSeries.add(schedule.timesInSchedule.get(0).getStartTime(),SOC[0]);
	for(int i=0; i<schedule.getNumberOfEntries(); i++){
		if(schedule.timesInSchedule.get(i).isParking()){
			ParkingInterval thisP= (ParkingInterval)schedule.timesInSchedule.get(i);
			SOC[i+1]= SOC[i]+thisP.getChargingSpeed()	*solution[1+i];
			// add
			SOCAgentSeries.add(thisP.getEndTime(),SOC[i+1]);
		}
		if(schedule.timesInSchedule.get(i).isDriving()){
			//subtract
			DrivingInterval thisD = (DrivingInterval)schedule.timesInSchedule.get(i);
			
			SOC[i+1]=SOC[i] - (thisD).getTotalConsumption();
			
			SOCAgentSeries.add(thisD.getEndTime(),SOC[i+1]);
			
		}
	}
	
	SOCAgent.addSeries(SOCAgentSeries);
	
	
	plotSeries(SOCAgent, filename1,  id);	
  	
}


	public void plotSeries(XYSeriesCollection SOCAgent, String filename, Id id) throws IOException{
		//********************************
		XYSeries SOCMax= new XYSeries("SOC Max");
		SOCMax.add(0, batterySize);
		SOCMax.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize);		
		
		XYSeries SOCMin= new XYSeries("SOC Min");
		SOCMin.add(0, 0);
		SOCMin.add(DecentralizedSmartCharger.SECONDSPERDAY, 0);
		//********************************
		XYSeries SOCMaxSuggested= new XYSeries("SOC Max Suggested");
		SOCMaxSuggested.add(0, batterySize*batteryMax);
		SOCMaxSuggested.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize*batteryMax);
		
		XYSeries SOCMinSuggested= new XYSeries("SOC MinSuggested");
		SOCMinSuggested.add(0,batterySize*batteryMin);
		SOCMinSuggested.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize*batteryMin);
		//********************************
		
		SOCAgent.addSeries(SOCMax);
		SOCAgent.addSeries(SOCMin);
		SOCAgent.addSeries(SOCMaxSuggested);
		SOCAgent.addSeries(SOCMinSuggested);
		
		JFreeChart chart = ChartFactory.createXYLineChart("SOC for agent"+ id.toString(), 
				"time of day [s]", 
				"SOC[J]", 
				SOCAgent, 
				PlotOrientation.VERTICAL, 
				false, true, false);
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
        
        
    	//******************************** SOC
        plot.getRenderer().setSeriesPaint(0, Color.black);
     	plot.getRenderer().setSeriesStroke(
 	            0, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 1.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	
    	//******************************** SOCMAX
     	plot.getRenderer().setSeriesPaint(1, Color.red);
     	plot.getRenderer().setSeriesStroke(
 	            1, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 4.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
    	//********************************SOCMIN
     	plot.getRenderer().setSeriesPaint(2, Color.red);
     	plot.getRenderer().setSeriesStroke(
 	            2, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 4.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	//********************************SOCMaxsugg
     	plot.getRenderer().setSeriesPaint(3, Color.gray);
     	plot.getRenderer().setSeriesStroke(
 	            3, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {3.0f, 3.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	//********************************SOCMinsugg
     	plot.getRenderer().setSeriesPaint(4, Color.gray);
     	plot.getRenderer().setSeriesStroke(
 	            4, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {3.0f, 3.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	
     	ChartUtilities.saveChartAsPNG(new File(filename) , chart, 800, 600);
	}
	
}
