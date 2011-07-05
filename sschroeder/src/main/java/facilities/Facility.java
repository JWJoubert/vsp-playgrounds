package facilities;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class Facility {
	
	private Id id;
	
	private Id locationId;
	
	private Coordinate coordinate;
	
	private Map<String,String> attributes = new HashMap<String, String>();
	
	public Map<String, String> getAttributes() {
		return attributes;
	}

	public Facility(Id id) {
		super();
		this.id = id;
	}
	
	public void setLocationId(Id locationId) {
		this.locationId = locationId;
	}

	public abstract String getType();
	
	public Id getId() {
		return id;
	}

	public Id getLocationId() {
		return locationId;
	}
	
	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	@Override
	public String toString() {
		return "id="+id+" locationId="+locationId+" type="+getType();
	}
	
}