/**
 * 
 */
package playground.johannes.socialnetworks.graph.analysis;


/**
 * @author illenberger
 *
 */
public interface GraphPropertyFactory {

	public Degree newDegree();
	
	public Transitivity newTransitivity();
	
}
