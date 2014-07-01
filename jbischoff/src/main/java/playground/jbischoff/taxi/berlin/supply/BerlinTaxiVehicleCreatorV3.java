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

package playground.jbischoff.taxi.berlin.supply;

import java.text.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.*;
import org.matsim.matrices.*;

import pl.poznan.put.util.random.WeightedRandomSelection;
import playground.michalm.supply.VehicleGenerator;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.zone.*;


public class BerlinTaxiVehicleCreatorV3
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = Logger.getLogger(BerlinTaxiVehicleCreatorV3.class);

    private Map<Date, Integer> taxisOverTime = new TreeMap<Date,Integer>();
    private double[] taxisOverTimeHourlyAverage;//24h from startDate, e.g. from 4am to 3am
    private WeightedRandomSelection<Id> wrs;

    private Scenario scenario;
    private Map<Id, Zone> zones;
    private List<Vehicle> vehicles;

    double evShare;
    double maxTime;


    public static void main(String[] args)
        throws ParseException
    {
        String dir = "C:/local_jb/data/";
        String taxisOverTimeFile = dir + "taxi_berlin/2013/vehicles/taxisweekly.csv";
        String statusMatrixFile = dir + "taxi_berlin/2013/status/statusMatrixAvg.xml";
        String networkFile = dir + "scenarios/2014_05_basic_scenario_v3/berlin_brb.xml";
        String zoneShpFile = dir + "shp_merged/zones.shp";
        String zoneXmlFile = dir + "shp_merged/zones.xml";
        String vehicleFile = dir + "scenarios/2014_05_basic_scenario_v3/taxis4to4_EV";

        BerlinTaxiVehicleCreatorV3 btv = new BerlinTaxiVehicleCreatorV3();
        btv.evShare = 1.0;
        btv.maxTime = 14.0 * 3600;
        btv.readTaxisOverTime(taxisOverTimeFile);
        btv.createAverages(SDF.parse("2013-04-16 04:00:00"), 1);
        btv.prepareNetwork(networkFile, zoneShpFile, zoneXmlFile);
        btv.prepareMatrices(statusMatrixFile);
        btv.createVehicles();

        btv.writeVehicles(vehicleFile);
    }


    private void prepareNetwork(String networkFile, String zoneShpFile, String zoneXmlFile)
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        zones = Zones.readZones(scenario, zoneXmlFile, zoneShpFile);
    }


    private void readTaxisOverTime(String taxisOverTimeFile)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + taxisOverTimeFile);
        config.setDelimiterTags(new String[] { "\t" });
        config.setFileName(taxisOverTimeFile);

        new TabularFileParser().parse(config, new TabularFileHandler() {
            @Override
            public void startRow(String[] row)
            {
                try {
                    taxisOverTime.put(SDF.parse(row[0]), Integer.parseInt(row[1]));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        log.info("done. (parsing " + taxisOverTimeFile + ")");
    }


    @SuppressWarnings("deprecation")
    private void createAverages(Date start, int days)
    {
        if (start.getMinutes() != 0 || start.getSeconds() != 0) {
            throw new RuntimeException("Must start with hh:00:00");
        }

        long startTime = start.getTime() / 1000;//in seconds
        long endTime = startTime + days * 24 * 3600;//in seconds

        taxisOverTimeHourlyAverage = new double[24];
        int sum = 0;
        int hour = 0;

        for (long t = startTime; t < endTime; t++) {
            sum += this.taxisOverTime.get(new Date(t * 1000));//seconds -> milliseconds

            if ( (t + 1) % 3600 == 0) {
                taxisOverTimeHourlyAverage[hour % 24] += (double)sum / 3600 / days;
                sum = 0;
                hour++;
            }
        }

        System.out.println(Arrays.asList(taxisOverTimeHourlyAverage));
    }


    private void prepareMatrices(String statusMatrixFile)
    {
        wrs = new WeightedRandomSelection<Id>();
        Matrix avestatus = MatrixUtils.readMatrices(statusMatrixFile).getMatrix("avg");

        for (Map.Entry<Id, ArrayList<Entry>> fromLOR : avestatus.getFromLocations().entrySet()) {
            wrs.add(fromLOR.getKey(), MatrixUtils.calculateTotalValue(fromLOR.getValue()));
        }
    }


    private void createVehicles()
    {
        BerlinTaxiCreator btc = new BerlinTaxiCreator(scenario, zones, wrs, evShare);
        VehicleGenerator vg = new VehicleGenerator(maxTime, maxTime, btc);
        vg.generateVehicles(taxisOverTimeHourlyAverage, 4 * 3600, 3600);
        vehicles = vg.getVehicles();
    }


    private void writeVehicles(String vehicleFile)
    {
        new VehicleWriter(vehicles).write(vehicleFile + evShare + ".xml");
    }
}