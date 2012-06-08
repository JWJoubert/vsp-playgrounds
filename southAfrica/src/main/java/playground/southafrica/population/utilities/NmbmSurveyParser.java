/* *********************************************************************** *
 * project: org.matsim.*
 * NmbmSurveyParser.java
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

package playground.southafrica.population.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultizoneReader;

public class NmbmSurveyParser {
	private final static Logger LOG = Logger.getLogger(NmbmSurveyParser.class);
	private Scenario sc;
	private Households households;
	private MyMultizoneReader zones;
	private ObjectAttributes householdAttributes;
	private ObjectAttributes personAttributes;
	private Map<Id, Integer> locationlessPersons;
	private Map<String, Integer> locationlessType;
	private boolean removeNullLocationPersons = true;

	/**
	 * Creates a MATSim population from the 2004 travel survey done for Nelson 
	 * Mandela Bay Metropolitan (NMBM).
	 * @param args the following arguments are required:
	 * <ol>
	 * 	<li> the household data file extracted from the MySQL database;
	 * 	<li> the individual travel/trip file extracted from the database; and
	 * 	<li> the output folder where to write the population files to. 
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmSurveyParser.class.toString(), args);
		
		NmbmSurveyParser nsp = new NmbmSurveyParser();
		nsp.parseZones(args[0]);
		nsp.parseHousehold(args[1]);
		nsp.parseIndividual(args[2]);

		nsp.writeHouseholds(args[3]);
		nsp.writePopulation(args[3]);
		
		Header.printFooter();
	}

	public NmbmSurveyParser() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.locationlessPersons = new HashMap<Id, Integer>();
		this.locationlessType = new HashMap<String, Integer>();
	}
	
	
	/**
	 * Parses the households from a MySQL extract. Currently (Apr'12) the 
	 * comma-separated field format required is:
	 * <ol>
	 * 	<li> ENU NO,
	 * 	<li> Household No,
	 * 	<li> ZONE No,
	 * 	<li> Sub Zone No,
	 * 	<li> No People Interviewed,
	 * 	<li> Motor Vehicles,
	 * 	<li> Total Income (code) 
	 * </ol>
	 * @param filename
	 */
	public void parseHousehold(String filename){
		this.households = new HouseholdsImpl();
		this.householdAttributes = new ObjectAttributes();
		Counter counter = new Counter("  households # ");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String enu = sa[0];
				String hhNo = sa[1];
				String zone = sa[2];
				String subzone = sa[3];
				Integer numberOfPeople = Integer.parseInt(sa[4]);
				Integer cars = Integer.parseInt(sa[5]);
				
				Double inc = this.getIncome(Integer.parseInt(sa[6]));
				Income income = null;
				if(inc != null){
					income = new IncomeImpl(inc, IncomePeriod.month);
				}
				
				/* Create the household. */
				Id id = new IdImpl(enu + "_" + hhNo);
				Household hh = this.households.getFactory().createHousehold(id);
				hh.setIncome(income);
				
				/* Add the household attributes. */
				this.householdAttributes.putAttribute(id.toString(), "zone", zone);
				this.householdAttributes.putAttribute(id.toString(), "subzone", subzone);
				this.householdAttributes.putAttribute(id.toString(), "noOfPeopleInterviewed", numberOfPeople);
				this.householdAttributes.putAttribute(id.toString(), "numberOfVehicles", cars);
				
				this.households.getHouseholds().put(id, hh);
				counter.incCounter();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader for " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader for " + filename);
			}
		}
		counter.printCounter();
	}
	
	/**
	 * Parses the individuals from a MySQL extract. Currently (Apr'12) the 
	 * comma-separated field format required is:
	 * <ol>
	 * 	<li> ENU NO
	 * 	<li> Household No
	 * 	<li> FormNo
	 * 	<li> Gender
	 * 	<li> Age
	 * 	<li> Occupation - 6 months (code)
	 * 	<li> ErrandNo
	 * 	<li> From - indicating the activity type at origin
	 * 	<li> To - indicating the activity type at the destination
	 * 	<li> Start Time
	 * 	<li> End Time
	 * 	<li> ZoneFrom
	 * 	<li> ZoneTo
	 * 	<li> Mode
	 * </ol>
	 * @param filename
	 */
	public void parseIndividual(String filename){
		Population population = this.sc.getPopulation();
		population.setName("Nelson Mandela 2004 travel survey population");
		this.personAttributes = new ObjectAttributes();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		PersonImpl person = null;
		PlanImpl plan = null;
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String enu = sa[0];
				String hhn = sa[1];
				String hhPerson = sa[2];
				String gender = sa[3].equalsIgnoreCase("Male") ? "m" : "f";
				int age = Integer.parseInt(sa[4]);
				boolean isEmployed = this.isEmployed(Integer.parseInt(sa[5]));
				String hasCar = sa[6].equalsIgnoreCase("Yes") ? "always" : "never";
				int legNumber = Integer.parseInt(sa[7]);
				String activityTypeOrigin = getActivityTypeFromCode(Integer.parseInt(sa[8]));
				String activityTypeDestination = getActivityTypeFromCode(Integer.parseInt(sa[9]));
				
				/* Parse the trip start and end time, in seconds from midnight. */
				int startTime = Integer.parseInt(sa[10].substring(11, 13))*3600 + Integer.parseInt(sa[10].substring(14, 16))*60;
				int endTime = Integer.parseInt(sa[11].substring(11, 13))*3600 + Integer.parseInt(sa[11].substring(14, 16))*60;
				
				String zoneFrom = sa[12];
				String zoneTo = sa[13];
				
				String mode = getMode(Integer.parseInt(sa[14]));

				Id personId = new IdImpl(enu + "_" + hhn + "_" + hhPerson);

				/* Process person if it doesn't yet exist TODO This must be fixed - should not occur. */
				if(!population.getPersons().containsKey(personId)){
					if(legNumber == 1){
						/* Add previous person if it is not null. */
						if(person != null){
							population.addPerson(person);
							
							/* Add person to household. */
							Id hhId = new IdImpl(enu + "_" + String.valueOf(hhn));
							if(!households.getHouseholds().containsKey(hhId)){
								LOG.error("Could not find the household " + hhId.toString());
							}
							households.getHouseholds().get(hhId).getMemberIds().add(personId);
						}
						
						/* Create new person. */
						person = (PersonImpl) population.getFactory().createPerson(personId);
						person.setSex(gender);
						person.setAge(age);
						person.setEmployed(isEmployed);
						person.setCarAvail(hasCar);
						plan = (PlanImpl) population.getFactory().createPlan();
						
						/* Add the first activity. */
						Coord coord = getCoord(zoneFrom);
						if(coord == null){
							if(locationlessPersons.containsKey(personId)){
								Integer oldValue = locationlessPersons.get(personId);
								locationlessPersons.put(personId, oldValue + 1);
							} else{
								locationlessPersons.put(personId, new Integer(1));
							}
							if(locationlessType.containsKey(activityTypeOrigin)){
								Integer oldValue = locationlessType.get(activityTypeOrigin);
								locationlessType.put(activityTypeOrigin, oldValue + 1);
							} else{
								locationlessType.put(activityTypeOrigin, new Integer(1));
							}
						}
						Activity act = population.getFactory().createActivityFromCoord(activityTypeOrigin, getLocalScrambledCoord(coord));
						act.setEndTime(startTime);
						plan.addActivity(act);
						person.addPlan(plan);
					} else{
						/* Check that the person's trip being read is the same as the current person. */
						if(!person.getId().equals(personId)){
							LOG.error("Person Ids are not the same: should be " + person.getId().toString() + " but was " + personId.toString());
						}
						/* Update the previous activity's duration. */
						Activity a = plan.getLastActivity();
						a.setEndTime(startTime);
					}
					/* Add the leg and destination activity. */
					Leg leg = population.getFactory().createLeg(mode);
					leg.setDepartureTime(startTime);
					leg.setTravelTime(endTime - startTime);
					plan.addLeg(leg);
					Coord coord = getCoord(zoneTo);
					if(coord == null){
						Coord formerCoord = ( (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-2) ).getCoord();
						if(formerCoord != null){
							coord = getScrambledCoord(formerCoord);
						} else{
							if(locationlessPersons.containsKey(personId)){
								Integer oldValue = locationlessPersons.get(personId);
								locationlessPersons.put(personId, oldValue + 1);
							} else{
								locationlessPersons.put(personId, new Integer(1));
							}
							if(locationlessType.containsKey(activityTypeDestination)){
								Integer oldValue = locationlessType.get(activityTypeDestination);
								locationlessType.put(activityTypeDestination, oldValue + 1);
							} else{
								locationlessType.put(activityTypeDestination, new Integer(1));
							}							
						}
					}
					Activity act = population.getFactory().createActivityFromCoord(activityTypeDestination, getLocalScrambledCoord(coord));
					act.setStartTime(endTime);
					plan.addActivity(act);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader for " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader for " + filename);
			}
		}
		
		LOG.info("People with location-less activities: " + locationlessPersons.size());
		for(Id id : locationlessPersons.keySet()){
			LOG.info("   " + id.toString() + ": " + locationlessPersons.get(id));
		}
		for(String s : locationlessType.keySet()){
			LOG.info("   " + s + ": " + locationlessType.get(s));
		}
		
		LOG.info("Removing persons with location-less activities from population and households...");
		LOG.info("  original population size: " + population.getPersons().size());
		int cleaned = 0;
		for(Id id : locationlessPersons.keySet()){
			/* Clean population. */
			population.getPersons().remove(id);
			cleaned++;
			
			/* Clean population attributes. */
			this.personAttributes.removeAllAttributes(id.toString());
			
			/* Clean households. */
			for(Id hhid : this.households.getHouseholds().keySet()){
				List<Id> memberIds = this.households.getHouseholds().get(hhid).getMemberIds();
				if(memberIds.contains(id)){
					memberIds.remove(id);
				}
			}
		}
		LOG.info("  new population size: " + population.getPersons().size() + " (" + cleaned + " removed)");
	}
	
	
	/**
	 * Selects a random point inside a 5km radius around the previous activity 
	 * in the plan. 
	 * @param coord
	 * @return
	 */
	private Coord getScrambledCoord(Coord coord){
		if(coord == null){
			return null;
		}
		double randomRadius = 5000;
		double angle = Math.random()*2*Math.PI;
		double radius = Math.random()*randomRadius;
		double newX = coord.getX() + radius*Math.cos(angle);
		double newY = coord.getY() + radius*Math.sin(angle);
		
		return new CoordImpl(newX, newY);
	}
	
	/**
	 * Selects a random point inside a 500m radius around the previous activity 
	 * in the plan. 
	 * @param coord
	 * @return
	 */
	private Coord getLocalScrambledCoord(Coord coord){
		if(coord == null){
			return null;
		}
		double randomRadius = 1000;
		double angle = Math.random()*2*Math.PI;
		double radius = Math.random()*randomRadius;
		double newX = coord.getX() + radius*Math.cos(angle);
		double newY = coord.getY() + radius*Math.sin(angle);
		
		return new CoordImpl(newX, newY);
	}




	/**
 	 * Writes the households and their attributes to file.
	 * @param outputfolder
	 */
	public void writeHouseholds(String outputfolder){
		if(this.households == null || this.householdAttributes == null){
			LOG.error("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file.");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(households);
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file.");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(householdAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "HouseholdAttributes.xml");
		}
	}

	
	/**
 	 * Writes the population and their attributes to file.
	 * @param outputfolder
	 */
	public void writePopulation(String outputfolder){
		if(this.sc.getPopulation().getPersons().size() == 0 || this.personAttributes == null){
			LOG.error("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file.");
			PopulationWriter pw = new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file.");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(this.personAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}

	private Double getIncome(int code){
		Double income = null;
		switch (code) {
		/* If not specified, then the income remains null. */
		case 1:
			income = 0.0; break;
		case 2:
			income = (1.0 + 500.0)/2; break;
		case 3:
			income = (501.0 + 1500.0)/2; break;
		case 4:
			income = (1501.0 + 3500.0)/2; break;
		case 5:
			income = (3501.0 + 6000.0)/2; break;
		case 6:
			income = (6001.0 + 11000.0)/2; break;
		case 7:
			income = (11001.0 + 30000.0)/2; break;
		case 8:
			income = (30001.0 + 60000.0)/2; break;
		default:
			break;
		}
		return income;
	}
	
	private String getMode(int code){
		String mode = null;
		switch (code) {
		case 1:
			mode = "walk"; break;
		case 2:
			mode = "cycle"; break;
		case 3:
		case 4:
			mode = "car"; break;
		case 5:
			mode = "ride"; break;
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
			mode = "taxi"; break;
		case 11:
			mode = "pt1"; break;
		case 12:
			mode = "pt2"; break;
		case 0:
		case 13:
		case 14:
			mode = "unknown"; break;
		default:
			break;
		}
		return mode;
	}
	
	private boolean isEmployed(int code){
		boolean isEmployed = false;
		switch (code) {
		case 1:
		case 2:
		case 3:
			isEmployed = true;
			break;
		case 0: // Did not respond
		case 4: // Work within own household (housewife, homemaker or home with children)
		case 5: // Full-time studies
		case 6: // Part-time studies
		case 7: // Unemployed, looking for employment
		case 8: // Unemployed, not looking for employment
		case 9: // Retired
		case 10: // Part-time retired
		case 11: // Pension or disability pension
			break;
		}
		return isEmployed;
	}
	
	/**
	 * Convert an integer value, as provided in the travel survey, to a more 
	 * descriptive 
	 * @param code an integer value used in the travel survey;
	 * @return a more descriptive {@link String} for the trip purpose, and
	 * 		   null if the code could not be found, in which case an error
	 * 		   message will be written, but it will not terminate.
	 */
	private String getActivityTypeFromCode(int code){
		String tripPurpose = null;
		switch (code) {
		case 1: /* Home */
			tripPurpose = "h"; break;
		case 2: /* Work (retail/business) */
//			tripPurpose = "w1"; break;
		case 3: /* Work (industrial) */
//			tripPurpose = "w2"; break;
		case 4: /* Work (all other) */
		case 9: /* Errand at work */ 
//			tripPurpose = "w3"; break;
			tripPurpose = "w"; break;
		case 5: /* School (as scholar) */
			tripPurpose = "e1"; break;
		case 6: /* University, Technikon or College (as scholar) */
			tripPurpose = "e2"; break;
		case 7: /* Drop/pickup children at school */
			tripPurpose = "e3"; break;
		case 10: /* Shopping */
			tripPurpose = "s"; break; 
		case 11: /* Recreation/sport/entertainment */
		case 16: /* Visit second / holiday home */
		case 17: /* Visit another person's home */
			tripPurpose = "l"; break; 
		case 0: /* Unknown */
		case 8: /* Pickup another person */ 
//			tripPurpose = "p"; break;
		case 12: /* Petrol station / garage */
		case 13: /* Medicare / doctor / dentist */
		case 14: /* Post office/ bank / municipality / pension / etc. */
		case 15: /* Church / community or organized culture */
//			tripPurpose = "c"; break;
		case 18: /* Fetch water */
		case 19: /* Tend animals or go to field */
		case 20: /* Other */
		case 21: /* Other */
		case 22: /* Other */
			tripPurpose = "o"; 
		default:
			break;
		}
		if(tripPurpose == null){
			LOG.error("The code `" + code + "' could not be converted to a valid trip purpose!");
		}
		return tripPurpose;
	}
	
	
	private Coord getCoord(String zone){
		//FIXME Must fix this once we have coordinates for the zones.
		MyZone mz = this.zones.getZone(new IdImpl(zone));
		if(mz == null){
			return null;
		}
		double x = mz.getInteriorPoint().getX();
		double y = mz.getInteriorPoint().getY();
		return new CoordImpl(x, y);
	}

	
	private void parseZones(String filename){
		this.zones = new MyMultizoneReader();
		try {
			this.zones.readMultizoneShapefile(filename, 1);
		} catch (IOException e) {
			Gbl.errorMsg("Could not parse shapefile.");
		}
	}
	
}

