/**
 * 
 */
package gis.mapinfo;

/**
 * @author stefan
 *
 */
public class ProjectionFactory {
	
	public Projection createDefaultLongitudeLatitude(){
		return new Projection("\"L�nge / Breite\", 1, 0");
	}

}
