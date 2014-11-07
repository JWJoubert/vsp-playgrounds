package pedCA.engine;

import java.io.IOException;

import pedCA.context.Context;
import pedCA.output.Log;

public class SimulationEngine {
	private int step;
	private final int finalStep;
	private AgentsGenerator agentGenerator;
	private AgentsUpdater agentUpdater;
	private ConflictSolver conflictSolver;
	private AgentMover agentMover;
	
	public SimulationEngine(int finalStep, Context context){
		step = 1;
		this.finalStep = finalStep;
		agentGenerator = new AgentsGenerator(context);
		agentUpdater = new AgentsUpdater(context.getPopulation());
		conflictSolver = new ConflictSolver(context);
		agentMover = new AgentMover(context);
	}
	
	public SimulationEngine(int finalStep, String path) throws IOException{
		this(finalStep,new Context(path));
	}
	
	public SimulationEngine(Context context){
		this(0,context);
	}
	
	private void step(){
		agentGenerator.step();
		agentUpdater.step();
		conflictSolver.step();
		agentMover.step();			
		step++;
	}
	
	
	//FOR MATSIM CONNECTOR
	public void doSimStep(double time){
		Log.log("STEP at: "+time);
		agentUpdater.step();
		conflictSolver.step();
		agentMover.step(time);			
		step++;
	}
	
	//FOR MATSIM CONNECTOR
	public AgentsGenerator getAgentGenerator(){
		return agentGenerator;
	}
	
	//FOR MATSIM CONNECTOR
	public void setAgentMover(AgentMover agentMover){
		this.agentMover = agentMover;
	}
	
	public void run(){
		while(step<=finalStep){
			Log.step(step);
			step();
		}
	}
}
