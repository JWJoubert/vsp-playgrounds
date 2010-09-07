/* *********************************************************************** *
 * project: org.matsim.*
 * AsciiToPlan.java
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
package playground.gregor.demandmodeling;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;


public class AsciiToPlan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String INPUT_FILE = "../../inputs/networks/m3.dat";
		final String NET_FILE="../../inputs/networks/padang_net_v20080618.xml";
		final String OUTPUT_FILE="./tmp/population.xml.gz";
		final int MAX_SIZE = 13;
		final char TRENNER = ' ';

		BufferedReader in = null;
		String spalten_v[] = new String[MAX_SIZE*2];
		String spalten[] = new String[MAX_SIZE];
		int zaehler=0;
		Double homex,homey,awayx,awayy,start,dur,actType,mode;
		char zeichen;
		int wert=0;
		StringBuffer kette = new StringBuffer();


		//ffne ASCII Input File
		try {
			in = new BufferedReader(new FileReader(new File(INPUT_FILE)));
		} catch (FileNotFoundException e) {
			System.out.println("Fehler beim Einlesen");
			e.printStackTrace();

		}
		System.out.println("ffnen der Datei erfolgreich!");
		//lese 1. Zeile ein
		String zeile = null;
		try {
			zeile = in.readLine() + " ";
		} catch (IOException e) {
			System.out.println("Fehler beim Einlesen der 1. Zeile");
			e.printStackTrace();
		}

		//ffnen des Szenarios
		ScenarioImpl sc = new ScenarioImpl();
		new MatsimNetworkReader(sc).readFile(NET_FILE);
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		//Starte die Verarbeitung
		do{
			int j=0;
			zaehler++;
			Id id =new IdImpl(zaehler);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);
			//Lese wichtige Daten ein
//			spalten_v = StringUtils.explode(zeile, TRENNER);
			for (int i =0; i<zeile.length();i++){
				zeichen = zeile.charAt(i);
				if (zeichen!=' '){
					kette.append(zeichen);
					if (i<zeile.length()-1){
						if (zeile.charAt(i+1) == ' '){
							spalten[j] = kette.toString();
							j++;
							kette.delete(0, kette.length());
						}
					}
					else {
						spalten[j] = kette.toString();
					}
				}
			}
			if (Double.parseDouble(spalten[7])<0) spalten[7]="0";
//			for (int i = 0; i<MAX_SIZE;i++){
//				System.out.println(spalten[i]);
//			}

//			for (int i = 0; i<(MAX_SIZE*2+1); i++){
//				if ((i % 2 == 0) && (i>0))
//					spalten[(i/2)-1]=spalten_v[i];
//			}
			//Verarbeite Daten
			actType=Double.parseDouble(spalten[5]);
			mode=Double.parseDouble(spalten[6]);
			start=Double.parseDouble(spalten[7]);
			dur=Double.parseDouble(spalten[8]);
			homex=Double.parseDouble(spalten[9]);
			homey=Double.parseDouble(spalten[10]);
			awayx=Double.parseDouble(spalten[11]);
			awayy=Double.parseDouble(spalten[12]);

			//Erzeuge Homeactivity
			Coord coord1 = new CoordImpl(homex,homey);
			String type = "h";
			Activity home1 = plan.createAndAddActivity(type, coord1);
			home1.setEndTime(start);

			plan.addLeg(pb.createLeg(TransportMode.car));

			//Erzeuge Workactivity
			Coord coord2 = new CoordImpl(awayx,awayy);

			if (actType == 1) {
				type = "w";
			} else	if (actType == 2) {
				type = "edu";
			}else	if (actType == 3) {
				type = "r";
			}else	if (actType == 4) {
				type = "soc";
			}
			Activity work = plan.createAndAddActivity(type, coord2);
			work.setStartTime(start);
			work.setEndTime(start + dur);

			plan.addLeg(pb.createLeg(TransportMode.car));

			//Erzeuge 2. Homeactivity
			type = "h";
			Activity home2 = plan.createAndAddActivity(type, coord1);
			home2.setStartTime(start + dur);

			try {
				zeile=in.readLine();
			} catch (IOException e) {
				System.out.println("Fehler beim Einlesen einer Zeile");
				e.printStackTrace();
			}
//			System.out.println(zaehler +". Person eingelesen");
			pop.addPerson(person);
		} while(zeile!=null);

		//Alles fertig --- Ergebnis ausgeben.
		new PopulationWriter(pop, sc.getNetwork()).write(OUTPUT_FILE);
		System.out.println("Ausgabe erzeugt");


	}

}
