package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


public class StepEdge implements PathStep {	
	
	/**
	 * Edge in a path
	 */
	private final Link edge;
	
	/**
	 * time upon which the flow enters the edge
	 */
	private final int startTime;
	
	/**
	 * time upon the flow arrivs at the toNode
	 */
	private final int arrivalTime;
	
	
	/**
	 * reminder if this is a forward edge or not
	 */
	private final boolean forward;
	
	
	/**
	 * default Constructor setting the arguments when using a Link 
	 * @param edge Link used
	 * @param startTime starting time
	 * @param arrivalTime arrival time (usually < starttime for residual links)
	 * @param forward flag if edge is forward or backward
	 */
	StepEdge(Link edge, int startTime, int arrivalTime, boolean forward){
		if (edge == null) {
			throw new IllegalArgumentException("StepEdge, Link edge may not be null");
		}
		this.startTime = startTime;
		this.arrivalTime = arrivalTime;
		this.edge = edge;
		this.forward = forward;		
	}
	
	/**
	 * Method returning a String representation of the StepEdge
	 */
	@Override
	public String toString(){
		String s;

		s = this.startTime + " " + edge.getFromNode().getId().toString()+"-->" + edge.getToNode().getId().toString() + " " +this.arrivalTime;
		if(!this.forward){
			s += " backwards";
		} 

		return s;
	}

	/**
	 * Getter for the Link used
	 * @return the edge
	 */	
	public Link getEdge() {
		return edge;
	}

	/**
	 * checks whether the link is used in forward direction
	 * @return the forward
	 */
	@Override
	public boolean getForward() {
		return forward;
	}
	
	/**
	 * returns startNode
	 * @return startNode
	 */
	@Override
	public Node getStartNode() {
		if (this.forward) {
		  return this.edge.getFromNode();
		} else {
		  return this.edge.getToNode();
		}
		
	}
	
	@Override
	public Node getArrivalNode() {
		if (!this.forward) {
			return this.edge.getFromNode();
		} else {
			return this.edge.getToNode();
		}
	}
	
	/**
	 * getter for the time at which an edge is entered
	 * @return the time
	 */
	@Override
	public int getStartTime() {
		return startTime;
	}
	
	@Override
	public int getArrivalTime()
	{
		return arrivalTime;
	}
	
	/**
	 * checks if two PathEdges are indentical in all fields
	 * @return true iff identical
	 */
	@Override
	public boolean equals(PathStep other)
	{
		if (!(other instanceof StepEdge)) return false;
		StepEdge o = (StepEdge) other;
		if(this.startTime == o.startTime
				&& this.arrivalTime == o.arrivalTime
				&& this.forward == o.forward)
		{			
			return (this.edge.equals(o.edge));				 
		}
		return false;
		
	}
	
	/**
	 * checks if this is the Residual of other
	 * @param other a forward PathEdge 
	 * @return true iff this is the Residual of other
	 */
	@Override
	public boolean isResidualVersionOf(PathStep other)
	{
		if (this.forward)
			return false;
		
		if (!other.getForward())
			return false;
		
		if (!(other instanceof StepEdge)) return false;
		StepEdge o = (StepEdge) other;

		if(this.startTime == o.arrivalTime
				&& this.arrivalTime == o.startTime) {
			return (this.edge.equals(o.edge));
		}
		return false;						 
	}

	
	@Override
	public PathStep copyShiftedTo(int newStart) {
	    int shift = newStart - this.startTime;
		return new StepEdge(this.edge, newStart, this.arrivalTime + shift, this.forward); 
	}

};

