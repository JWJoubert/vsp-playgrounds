/* *********************************************************************** *
 * project: org.matsim.*
 * CommonUtilies.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.tnicolai.matsim4opus.utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.tnicolai.matsim4opus.constants.Constants;


/**
 * @author thomas
 *
 */
public class UtilityCollection {

	/**
	 * This is used to parse a header line from a tab-delimited urbansim header and generate a Map that allows to look up column
	 * numbers (starting from 0) by giving the header line.
	 *
	 * I.e. if you have a header "from_id <tab> to_id <tab> travel_time", then idxFromKey.get("to_id") will return "1".
	 *
	 * This makes the reading of column-oriented files independent from the sequence of the columns.
	 *
	 * @param line
	 * @return idxFromKey as described above (mapping from column headers into column numbers)
	 *
	 * @author nagel
	 */
	public static Map<String,Integer> createIdxFromKey( String line, String seperator ) {
		String[] keys = line.split( seperator ) ;

		Map<String,Integer> idxFromKey = new HashMap<String, Integer>() ;
		for ( int i=0 ; i<keys.length ; i++ ) {
			idxFromKey.put(keys[i], i ) ;
		}
		return idxFromKey ;
	}
	
	/**
	 * See describtion from createIdxFromKey( String line, String seperator )
	 * 
	 * @param keys
	 * @return idxFromKey
	 */
	public static Map<Integer,Integer> createIdxFromKey( int keys[]) {

		Map<Integer,Integer> idxFromKey = new HashMap<Integer, Integer>() ;
		for ( int i=0 ; i<keys.length ; i++ ) {
			idxFromKey.put(keys[i], i ) ;
		}
		return idxFromKey ;
	}

	/**
	 * Helper method to start a plan by inserting the home location.  This is really only useful together with "completePlanToHwh",
	 * which completes the plan, and benefits from the fact that the Strings for the "home" and the "work" act are now concentrated
	 * here.
	 *
	 * @param plan
	 * @param homeCoord
	 * @param homeLocation
	 *
	 * @author nagel
	 */
	public static void makeHomePlan( PlanImpl plan, Coord homeCoord, ActivityFacility homeLocation) {
		ActivityImpl act = plan.createAndAddActivity( Constants.ACT_HOME, homeCoord) ;
		act.setFacilityId( homeLocation.getId() );	// tnicolai: added facility id to compute zone2zone trips
	}

	/**
	 * Helper method to complete a plan with *wh in a consistent way.  Assuming that the first activity is the home activity.
	 *
	 * @param plan
	 * @param workCoord
	 * @param jobLocation
	 *
	 * @author nagel
	 */
	public static void completePlanToHwh ( PlanImpl plan, Coord workCoord, ActivityFacility jobLocation ) {
		
		// complete the first activity (home) by setting end time. 
		ActivityImpl act = (ActivityImpl)plan.getFirstActivity();
		act.setEndTime( 7.*3600. ) ; // tnicolai: make configurable: see actType1.setOpeningTime(7*3600)
		// gather coordinate and facility id needed for last activity
		Coord homeCoord = act.getCoord();
		Id homeId = act.getFacilityId();
		
		// set Leg
		plan.createAndAddLeg(TransportMode.car);
		
		// set second activity (work)
		act = plan.createAndAddActivity( Constants.ACT_WORK, workCoord );
		act.setFacilityId( jobLocation.getId() );
		act.setMaximumDuration( 8.*3600. ) ; // tnicolai: make configurable: actType1.setTypicalDuration(8*60*60);
		
		// set Leg
		plan.createAndAddLeg(TransportMode.car) ;
		
		// set last activity (=first activity) and complete home-work-home plan.
		plan.createAndAddActivity( Constants.ACT_HOME, homeCoord );
		act = (ActivityImpl)plan.getLastActivity();
		act.setFacilityId( homeId );
	}
	
	/**
	 * makes sure that a path ends with "/"
	 * @param any desired path
	 * @return path that ends with "/"
	 */
	public static String checkPathEnding(String path){
		
		path.replace('\\', '/');
		
		if(path.endsWith("/"))
			return path;
		else
			return path + "/";
	}
	
	/**
	 * returns the path of the current directory
	 * @return class path
	 */
	@SuppressWarnings("all")
	public static String getCurrentPath(Class classObj){
		try{
			URL dirUrl = classObj.getResource("./"); // get directory of given class
			return dirUrl.getFile();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * replaces parts of a path with another subPath for a given directory
	 * hirachy (depth).
	 * Example:
	 * Path = "/home/username/dir/"
	 * Subpath = "/anotherDir/"
	 * 
	 * depth = 0 leads to:
	 * "/home/username/dir/anotherDir
	 * 
	 * pepth = 1 leads to:
	 * "/home/username/anotherDir
	 * 
	 * @param depth level of directory hirachy
	 * @param path
	 * @param subPath
	 * @return path that incorporates a given path and subpath
	 */
	public static String replaceSubPath(int depth, String path, String subPath){
		
		StringBuffer newPath = new StringBuffer("/");
		
		path.replace("\\", "/");
		
		String[] pathArray = path.split("/");
		String[] subPathArray = subPath.split("/");
		
		int iterations = pathArray.length - depth;
		if(pathArray.length >= iterations){
			
			for(int i = 0; i < iterations; i++)
				if(!pathArray[i].equalsIgnoreCase(""))
					newPath.append( pathArray[i] + "/" );
			for(int i = 0; i < subPathArray.length; i++)
				if(!subPathArray[i].equalsIgnoreCase(""))
					newPath.append( subPathArray[i] + "/");
			
			// remove last "/"
			newPath.deleteCharAt( newPath.length()-1 );
			return newPath.toString().trim();
		}
		return null;
	}
	
	/**
	 * returns the directory to the UrbanSim input data for MATSim Warm Start
	 * @return directory to the UrbanSim input data
	 */
	@SuppressWarnings("all")
	public static String getWarmStartUrbanSimInputData(Class<?>  classObj){		
//		return concatPath(1, "matsimTestData/warmstart/urbanSimOutput/", classObj);
		return UtilityCollection.checkPathEnding( UtilityCollection.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_URBANSIM_OUTPUT );
	}
	
	/**
	 * returns the directory to the input plans file for MATSim Warm Start
	 * @return directory to the input plans file
	 */
	@SuppressWarnings("all")
	public static String getWarmStartInputPlansFile(Class<?>  classObj){		
//		return concatPath(1, "matsimTestData/warmstart/inputPlan/", classObj);
		return UtilityCollection.checkPathEnding( UtilityCollection.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_INPUT_PLANS);
	}
	
	/**
	 * returns the directory to the MATSim Warm Start network
	 * @return directory to the network
	 */
	@SuppressWarnings("all")
	public static String getWarmStartNetwork(Class<?>  classObj){		
//		return concatPath(1, "matsimTestData/warmstart/network/", classObj);
		return UtilityCollection.checkPathEnding( UtilityCollection.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_WARM_START_NETWORK );
	}
	
	/**
	 * returns the directory to the UrbanSim input data for MATSim
	 * @return directory to the UrbanSim input data
	 */
	@SuppressWarnings("all")
	public static String getTestUrbanSimInputDataDir(Class<?>  classObj){
		return UtilityCollection.checkPathEnding( UtilityCollection.getCurrentPath( classObj ) + Constants.MATSIM_TEST_DATA_DEFAULT_URBANSIM_OUTPUT );
		
//		return concatPath(1, "matsimTestData/urbanSimOutput/", classObj);
	}
	
	/**
	 * sorts a given array
	 * 
	 * @param array
	 * 
	 * @author thomas
	 */
	public static int[] ArrayQuicksort(int array[]){
	    int i;
	
	    System.out.println("Values Before the sort:\n");
	    for(i = 0; i < array.length; i++)
	      System.out.print( array[i]+"  ");
	    System.out.println();
	    quick_srt(array,0,array.length-1);
	    System.out.print("Values after the sort:\n");
	    for(i = 0; i <array.length; i++)
	      System.out.print(array[i]+"  ");
	    return array;
	}
	
	private static void quick_srt(int array[],int low, int n){
	    int lo = low;
	    int hi = n;
	    if (lo >= n) {
	      return;
	    }
	    int mid = array[(lo + hi) / 2];
	    while (lo < hi) {
	      while (lo<hi && array[lo] < mid) {
	        lo++;
	      }
	      while (lo<hi && array[hi] > mid) {
	        hi--;
	      }
	      if (lo < hi) {
	        int T = array[lo];
	        array[lo] = array[hi];
	        array[hi] = T;
	      }
	    }
	    if (hi < lo) {
	      int T = hi;
	      hi = lo;
	      lo = T;
	    }
	    quick_srt(array, low, lo);
	    quick_srt(array, lo == low ? lo+1 : lo, n);
	  }
}

