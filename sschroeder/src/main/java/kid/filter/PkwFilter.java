package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

public class PkwFilter implements VehicleFilter {

	public boolean judge(Vehicle vehicle) {
		String type = vehicle.getAttributes().get(KiDSchema.VEHICLE_TYPE);
		if(type.equals("02")){
			return true;
		}
		return false;
	}

}
