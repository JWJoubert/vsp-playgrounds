package playground.sergioo.FacilitiesGeneratorTemp;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import util.dataBase.DataBaseAdmin;
import util.dataBase.NoConnectionException;

public class FacilitiesFileGenerator {

	//Constants
	private static final String FACILITIES_FILE = "./data/currentSimulation/facilities/facilities.xml";
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	
	//Methods
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("Facilities Singapore");
		DataBaseAdmin dataBaseFacilities  = new DataBaseAdmin(new File("./data/facilities/DataBaseFacilities.properties"));
		Map<Integer, Collection<Tuple<String,Double>>> activityOptions = new HashMap<Integer, Collection<Tuple<String,Double>>>();
		ResultSet optionsResult = dataBaseFacilities.executeQuery("SELECT facility_id,type,capacity FROM Activity_options");
		while(optionsResult.next()) {
			Collection<Tuple<String,Double>> options = activityOptions.get(optionsResult.getInt(1));
			if(options==null)
				options=new ArrayList<Tuple<String,Double>>();
			options.add(new Tuple<String, Double>(optionsResult.getString(2), optionsResult.getDouble(3)));
			activityOptions.put(optionsResult.getInt(1), options);
		}
		optionsResult.close();
		Map<String, Collection<Tuple<Double, Double>>> openingTimes = new HashMap<String, Collection<Tuple<Double,Double>>>();
		ResultSet timesResult = dataBaseFacilities.executeQuery("SELECT start_time,end_time,type,facility_id FROM Opening_times");
		while(timesResult.next()) {
			Collection<Tuple<Double, Double>> tims = openingTimes.get(timesResult.getString(3)+"##"+timesResult.getInt(4));
			if(tims==null)
				tims = new ArrayList<Tuple<Double,Double>>();
			tims.add(new Tuple<Double, Double>(timesResult.getDouble(1), timesResult.getDouble(2)));
			openingTimes.put(timesResult.getString(3)+"##"+timesResult.getInt(4), tims);
		}
		timesResult.close();
		ResultSet facilitiesResult = dataBaseFacilities.executeQuery("SELECT external_id,x,y,id FROM Facilities");
		while(facilitiesResult.next()) {
			ActivityFacility facility = facilities.createFacility(new IdImpl(facilitiesResult.getInt(1)), new CoordImpl(facilitiesResult.getDouble(2), facilitiesResult.getDouble(3)));
			int facilityId=facilitiesResult.getInt(4);
			for(Tuple<String,Double> optionData:activityOptions.get(facilitiesResult.getInt(4))) {
				ActivityOption option = ((ActivityFacilityImpl)facility).createActivityOption(optionData.getFirst());
				option.setCapacity(optionData.getSecond());
				Collection<Tuple<Double, Double>> times = openingTimes.get(optionData.getFirst()+"##"+facilityId);
				for(Tuple<Double, Double> time:times)
					option.addOpeningTime(new OpeningTimeImpl(DayType.wkday, time.getFirst(), time.getSecond()));
			}
			optionsResult.close();
		}
		facilitiesResult.close();
		dataBaseFacilities.close();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(facilities, (NetworkImpl)scenario.getNetwork());
		new FacilitiesWriter(facilities).write(FACILITIES_FILE);
	}
	
	/*
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("Facilities Singapore");
		DataBaseAdmin dataBaseFacilities  = new DataBaseAdmin(new File("./data/facilities/DataBaseFacilities.properties"));
		ResultSet facilitiesResult = dataBaseFacilities.executeQuery("SELECT external_id,x,y,id FROM Facilities");
		while(facilitiesResult.next()) {
			ActivityFacility facility = facilities.createFacility(new IdImpl(facilitiesResult.getInt(1)), new CoordImpl(facilitiesResult.getDouble(2), facilitiesResult.getDouble(3)));
			int facilityId=facilitiesResult.getInt(4);
			ResultSet optionsResult = dataBaseFacilities.executeQuery("SELECT type,capacity FROM Activity_options WHERE facility_id="+facilityId);
			while(optionsResult.next()) {
				ActivityOption option = ((ActivityFacilityImpl)facility).createActivityOption(optionsResult.getString(1));
				option.setCapacity(optionsResult.getDouble(2));
				ResultSet timesResult = dataBaseFacilities.executeQuery("SELECT day_type,start_time,end_time FROM Opening_times WHERE facility_id="+facilityId+" AND type='"+optionsResult.getString(1)+"'");
				while(timesResult.next())
					option.addOpeningTime(new OpeningTimeImpl(DayType.valueOf(timesResult.getString(1)), timesResult.getDouble(2), timesResult.getDouble(3)));
				timesResult.close();
			}
			optionsResult.close();
		}
		facilitiesResult.close();
		dataBaseFacilities.close();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[1]));
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(facilities, (NetworkImpl) scenario.getNetwork());
		new FacilitiesWriter(facilities).write(FACILITIES_FILE);
	*/

}
