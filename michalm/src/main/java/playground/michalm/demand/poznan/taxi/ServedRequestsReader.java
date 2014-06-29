/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand.poznan.taxi;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class ServedRequestsReader
{
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

    private final Scenario scenario;
    private final List<ServedRequest> requests;

    private Scanner scanner;


    public ServedRequestsReader(Scenario scenario, List<ServedRequest> requests)
    {
        this.scenario = scenario;
        this.requests = requests;
    }


    public void readFile(String file)
    {
        try {
            scanner = new Scanner(new File(file));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // ID Przyjęte Wydane Skąd-dług Skąd-szer Dokąd-dług Dokąd-szer Id.taxi
        scanner.nextLine();//skip the header line

        while (scanner.hasNext()) {
            //2014_02_000001  01-02-2014 00:00:26  01-02-2014 00:00:22  16.964106  52.401409  16.898370  52.428270  329
            Id id = scenario.createId(scanner.next());
            Date accepted = getNextDate();
            Date assigned = getNextDate();
            Coord from = getNextCoord();
            Coord to = getNextCoord();
            Id taxiId = scenario.createId(scanner.next());
            requests.add(new ServedRequest(id, accepted, assigned, from, to, taxiId));
        }

        scanner.close();
    }


    private Date getNextDate()
    {
        String day = scanner.next();
        String time = scanner.next();
        return parseDate(day + " " + time);
    }


    private Coord getNextCoord()
    {
        double x = scanner.nextDouble();
        double y = scanner.nextDouble();
        return ct.transform(new CoordImpl(x, y));
    }


    public static Date parseDate(String date)
    {
        try {
            return DATE_FORMAT.parse(date);
        }
        catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}