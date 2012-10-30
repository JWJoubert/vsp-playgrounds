/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.michalm.demand;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;

import playground.michalm.vrp.data.network.router.*;


public class InterZoneDistanceCalculator
{
    private static class ZoneCentroid
    {
        private int zoneId;
        private Node node;
    }


    private Scenario scenario;
    private LeastCostPathCalculator router;
    private ZoneCentroid[] zoneCentroids;


    private void readNetwork(String filename)
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(filename);
    }


    private void readZoneCentroids(String filename)
        throws FileNotFoundException
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        List<ZoneCentroid> zoneCentroidList = new ArrayList<ZoneCentroid>();

        File file = new File(filename);
        Scanner scanner = new Scanner(file);

        scanner.nextLine();// skip the header line

        while (scanner.hasNext()) {
            ZoneCentroid zc = new ZoneCentroid();
            zc.zoneId = scanner.nextInt();

            double x = scanner.nextDouble();
            double y = scanner.nextDouble();
            zc.node = network.getNearestNode(scenario.createCoord(x, y));
            
            zoneCentroidList.add(zc);
        }

        zoneCentroids = zoneCentroidList.toArray(new ZoneCentroid[zoneCentroidList.size()]);
    }


    private void initDijkstra()
    {
        TravelTime ttimeCalc = new FreeSpeedTravelTimeCalculator();

        boolean distanceMode = true; // distance or freeflow speed
        TravelDisutility tcostCalc = distanceMode ? new DistanceAsTravelDisutility()
                : new TimeAsTravelDisutility(ttimeCalc);

        router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);
    }


    private void writeDistances(String filename)
        throws IOException
    {
        File file = new File(filename);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        // Header line
        bw.write("\t");
        for (ZoneCentroid j : zoneCentroids) {
            bw.write(j.zoneId + "\t");
        }

        // normal lines
        for (ZoneCentroid i : zoneCentroids) {
            System.out.println(i.zoneId + "...");
            
            bw.write(i.zoneId + "\t");
            for (ZoneCentroid j : zoneCentroids) {
                Path path = router.calcLeastCostPath(i.node, j.node, 0, null, null);
                bw.write(path.travelCost + "\t");
            }

            bw.newLine();
        }
        
        bw.close();
    }


    public void go(String networkFilename, String centroidsFilename, String distancesFilename)
        throws IOException
    {
        readNetwork(networkFilename);
        readZoneCentroids(centroidsFilename);
        initDijkstra();
        writeDistances(distancesFilename);
    }
    
    
    public static void main(String[] args) throws IOException
    {
        String networkFilename = "d:\\PP-rad\\poznan\\network.xml";
        String centroidsFilename = "d:\\PP-rad\\poznan\\wspol_centr.txt";
        String distancesFilename = "d:\\PP-rad\\poznan\\inter_zone_distances.txt";
        
        new InterZoneDistanceCalculator().go(networkFilename, centroidsFilename, distancesFilename);
    }
}
