package playground.wrashid.bsc.vbmh.vmParking;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.wrashid.bsc.vbmh.controler.VMConfig;
import playground.wrashid.bsc.vbmh.util.CSVWriter;
import playground.wrashid.bsc.vbmh.util.RemoveDuplicate;
import playground.wrashid.bsc.vbmh.util.VMCharts;
import playground.wrashid.bsc.vbmh.vmEV.EVControl;


/**
 * Manages the whole parking process of one Agent at a time. One instance of this class is kept by the Park_Handler 
 * which starts the Park() / leave().
 * Parking: First the availability of a private parking belonging to the destination facility is checked. Then all public 
 * parking in a specific area around the destination of the agent are checked for free spots
 * and then the best one is selected. 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */

public class ParkControl {
	
	
	//Zur berechnung des besten oeffentlichen Parkplatzes: (Negative Werte, hoechste Score gewinnt)
	//werden jetzt beim startup() aus der Config geladen
	double betaMoney; //= -10; 
	double betaWalk; //= -1; // !! Zweiphasige Kurve einbauen?
	
	
	int countPrivate = 0;
	int countPublic = 0;
	int countNotParked = 0;
	int countEVParkedOnEVSpot = 0;
	
	LinkedList<double[]> availableParkingStat = new LinkedList<double[]>();  //zaehlt für jeden Parkvorgang die Parkplaetze, die zur verfuegung stehen
	VMCharts vmCharts = new VMCharts(); 
	HashMap<Integer, Integer> peekLoad;
	HashMap<Integer, Integer> load;
	
	
	Controler controller;
	ParkingMap parkingMap = new ParkingMap(); //Beinhaltet alle Parkplaetze
	PricingModels pricing = new PricingModels(); //Behinhaltet die Preismodelle
	ParkHistoryWriter phwriter = new ParkHistoryWriter(); //Schreibt XML Datei mit Park events
	EVControl evControl;
	boolean evUsage=false;
	
	double time; //Wird auf aktuelle Zeit gesetzt (Vom event)
	Coord cordinate; //Koordinaten an denen die Zie Facility ist. Von hier aus wird gesucht.
	boolean ev;
	
	
	//--------------------------- S T A R T  U P---------------------------------------------
	public int startup(String parkingFilename, String pricingFilename, Controler controller){
		this.controller=controller;
		
		//Get Betas from Config
		Map<String, String> planCalcParams = this.controller.getConfig().getModule("planCalcScore").getParams();
		betaMoney=-Double.parseDouble(planCalcParams.get("marginalUtilityOfMoney")); //!! in Config positiver Wert >> stimmt das dann so?
		betaWalk=Double.parseDouble(planCalcParams.get("traveling_walk"));
		
		//System.out.println(betaMoney);
		
		//Charts starten:
		
		
		//Parkplaetze Laden
		File parkingfile = new File( parkingFilename );
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		this.parkingMap=karte;
		
		//Preise Laden
		File pricingfile = new File( pricingFilename ); 
		this.pricing = JAXB.unmarshal( pricingfile, PricingModels.class ); //Laedt Preise aus XML
		
		
		return 0;
	
	}
	
	public void iterStart(){
		vmCharts.addChart("Available parkings");
		vmCharts.setAxis("Available parkings", "time", "available parkings in area");
		vmCharts.addSeries("Available parkings", "for ev");
		vmCharts.addSeries("Available parkings", "for nev");
		vmCharts.addChart("Available EVparkings");
		vmCharts.addSeries("Available EVparkings", "slow charge");
		vmCharts.addSeries("Available EVparkings", "fast charge");
		vmCharts.addSeries("Available EVparkings", "turbo charge[x100]");
		peekLoad = new HashMap<Integer, Integer>();
		load = new HashMap<Integer, Integer>();
		for(Parking parking : parkingMap.getParkings()){
			peekLoad.put(parking.id, 0);
			load.put(parking.id, 0);
		}
		
		
	}
	
	
	
	
	//--------------------------- P A R K ---------------------------------------------
	public int park(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		this.time=event.getTime();

		
		// FACILITY UND KOORDINATEN LADEN
		IdImpl facilityid = new IdImpl(event.getAttributes().get("facility"));
		Map<Id, ? extends ActivityFacility> facilitymap = controller.getFacilities().getFacilities();
		ActivityFacility facility = facilitymap.get(facilityid);
		this.cordinate = facility.getCoord();
		
		/*
		Parkplatz finden: Es werden zur Facility gehoerende Privatparkplaetze und oeffentliche in der Umgebung
		in einer Liste gesammelt. Anschliessend wird jeder bewertet und der beste ausgewaehlt. Bewertet wird fuer 
		NEVs nach Distanz und Kosten. Bei EVs wird das moegliche Laden beruecksichtigt
		*/
		
		//Geschaetzte Dauer laden
		//sSystem.out.println(getEstimatedDuration(event)/3600);
		
		//EV Checken:
		ev=false;
		if(evUsage){ //Ueberpruefen ob EV Control verwendet wird (Damit Parking weiterhin als standalone funktioniert)
			if(evControl.hasEV(personId)){
				ev=true;
				//System.out.println("Suche Parking fuer EV");
			}
		}
		
		// Geschatzte Dauer und noch Zurueckzulegende Strecke berechnen
		double [] futureInfo = getFutureInfo(event);
		double estimatedDuration = 0;
		double restOfDayDistance = 0;
		if(futureInfo != null ){	
			estimatedDuration = futureInfo[0];
			restOfDayDistance = futureInfo[1];
			//System.out.println("rest of day distance: "+restOfDayDistance);
		} else {
			System.out.println("F E H L E R in der Future Info");
		}
		
		
		// NICHT EV Plaetze
		ParkingSpot privateParking = checkPrivateParking(facilityid.toString(), event.getActType(), false);
		LinkedList<ParkingSpot> spotsInArea = getPublicParkings(cordinate, false);
		if (privateParking != null) {
			spotsInArea.add(privateParking); // Privates Parking anfuegen
		} 
		// ------------
		
		//EV Plaetze dazu	//!! Spots koennten doppelt sein !
		if (ev){
			ParkingSpot privateParkingEV = checkPrivateParking(facilityid.toString(), event.getActType(), true);
			LinkedList<ParkingSpot> spotsInAreaEV = getPublicParkings(cordinate, true);
			if (privateParking != null) {
				spotsInAreaEV.add(privateParkingEV); // Privates Parking anfuegen
			} 
			
			spotsInArea.addAll(spotsInAreaEV);
		}
		//-----------
		RemoveDuplicate.RemoveDuplicate(spotsInArea);
		
		//Statistik
		if(ev){
			VMCharts.addValues("Available parkings", "for ev", time, spotsInArea.size());
		}else{
			VMCharts.addValues("Available parkings", "for nev", time, spotsInArea.size());
		}	
		availableParkingStat.add(new double[]{time, spotsInArea.size()});
		//--
		
		//Select 
		if(spotsInArea.size()>0){ 
			selectParking(spotsInArea, personId, estimatedDuration, restOfDayDistance, ev);
			return 1;
		}
		//-----
		
		//System.err.println("Nicht geparkt"); // !! Was passiert wenn Kein Parkplatz im Umkreis gefunden?
		
		// !! Provisorisch: Agents bestrafen die nicht Parken:
		Map<String, Object> personAttributes = controller.getPopulation().getPersons().get(personId).getCustomAttributes();
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		scorekeeper.add(-30);
		
		phwriter.addAgentNotParked(Double.toString(this.time), personId.toString());
		
		this.countNotParked++;
		return -1;
	}

	//--------------------------- SELECT PARKING ---------------------------------------------	
	private void selectParking(LinkedList<ParkingSpot> spotsInArea, Id personId, double duration, double restOfDayDistance, boolean ev) {
		// TODO Auto-generated method stub
		boolean sufficientEVSpotFound = false; //Marks if there is a spot with anough possible charging to get the agent back home
		boolean hasToCharge=false;
		double score = 0;
		double bestScore=-10000; //Nicht elegant, aber Startwert muss kleiner sein als alle moeglichen Scores
		double stateOfCharge=0;
		double neededBatteryPercentage=0;
		int countSlowCharge=0;
		int countFastCharge=0;
		int countTurboCharge=0;
		ParkingSpot bestSpot;
		bestSpot=null;
		
		if(ev){
			stateOfCharge = evControl.stateOfChargePercentage(personId);
			neededBatteryPercentage = evControl.calcEnergyConsumptionForDistancePerc(personId, restOfDayDistance);
			//System.out.println("Needed battery perc :"+neededBatteryPercentage);
			//System.out.println("State of charge: "+stateOfCharge);
			if(neededBatteryPercentage>stateOfCharge){
				phwriter.addAgentHasToCharge(Double.toString(time), personId.toString());
				hasToCharge=true;
			}
		}
		
		
		
		for (ParkingSpot spot : spotsInArea){
			// SCORE
			double evRelatedScore = 0;
			double distance = CoordUtils.calcDistance(this.cordinate, spot.parking.getCoordinate());
			double pricem = spot.parkingPriceM;
			double cost = pricing.calculateParkingPrice(duration, false, (int) pricem);
			//System.out.println("Cost :"+ Double.toString(cost));
			
			//EV Score:
			if(ev && spot.charge){
				
				double newStateOfChargePerc = evControl.calcNewStateOfChargePercentage(personId, spot.chargingRate, duration);
				double stateOfChargeGainPerc = newStateOfChargePerc-stateOfCharge;
				double chargableAmountOfEnergy =evControl.clalcChargedAmountOfEnergy(personId, spot.chargingRate, duration);
				
				if(stateOfCharge<neededBatteryPercentage && newStateOfChargePerc>neededBatteryPercentage){
					//Rest des Tages kann ohne Laden nicht gefahren werden mit jedoch schon.
					evRelatedScore+=30; //!! Wert anpassen
					sufficientEVSpotFound = true;
				}
				
				evRelatedScore += VMConfig.pricePerKWH*chargableAmountOfEnergy*betaMoney*-1; //Ersparnis gegenueber zu hause Laden
				//!! Vorzeichen?
				
				double betaBatteryPerc = 0.1; //!! Gerhoert nicht hier her und sollte nicht als konstant angenommen werden
				//evRelatedScore += betaBatteryPerc  * stateOfChargeGainPerc; //!! Nur provisorisch !
				
				//System.out.println("Ev related Score :" + Double.toString(evRelatedScore));
			
				//Stats
				if(spot.chargingRate<3){
					countSlowCharge++;
				}else if(spot.chargingRate<5){
					countFastCharge++;
				}else{
					countTurboCharge++;
				}
				//---------
			
			
			
			
			}
			
			double walkingTime = distance/VMConfig.walkingSpeed; //in h 
			//System.out.println("Walking Time: "+Double.toString(walkingTime));
			score =  this.betaMoney*1*cost+this.betaWalk*walkingTime+evRelatedScore; //!! Vorzeichen
			//___

			if(score > bestScore){
				bestScore=score;
				bestSpot=spot;
			}
			
		}
		
		if(sufficientEVSpotFound && !bestSpot.charge){
			phwriter.addEVChoseWrongSpot(Double.toString(time), personId.toString(), bestScore);
		}
		//Stat
		if(hasToCharge){
			//vmCharts.addValues("Available EVparkings", "slow charge", time, countSlowCharge);
			vmCharts.addValues("Available EVparkings", "fast charge", time, countFastCharge);
			vmCharts.addValues("Available EVparkings", "turbo charge[x100]", time, countTurboCharge*100);
		}
		//-----
		
		parkOnSpot(bestSpot, bestScore, personId);
		
	}

	//--------------------------- C H E C K   P R I V A T ---------------------------------------------
	ParkingSpot checkPrivateParking(String facilityId, String facilityActType, boolean ev) {
		//Gibt falls verfuegbar Spot auf Privatparkplatz passend zur Aktivitaet in der Facility zurueck
		//Bei EVS werden priorisiert EV Plaetze zurueck gegeben
		// !! Zur Beschleunigung Map erstellen ? <facility ID, Private Parking> ?
		ParkingSpot selectedSpot = null;
		for (Parking parking : parkingMap.getParkings()) {
			// System.out.println("Suche Parking mit passender facility ID");
			if(parking.facilityId!=null){ //Es gibt datensaetze ohne Facility ID >> Sonst Nullpointer
				if (parking.facilityId.equals(facilityId) && parking.facilityActType.equals(facilityActType)) { 
					//System.out.println("checke Parking");
					selectedSpot = parking.checkForFreeSpot(); //Gibt null oder einen freien Platz zurueck
					if(ev){
						selectedSpot = parking.checkForFreeSpotEVPriority(); // !!Wenn ev Spot vorhanden wird er genommen.
					}
					if (selectedSpot != null) {
						return selectedSpot;
					}
					
				}
			}
		}
		return null;
	}

	//--------------------------- G E T  P U B L I C ---------------------------------------------
	LinkedList<ParkingSpot> getPublicParkings(Coord coord, boolean ev) {
		// !! Mit quadtree oder aehnlichem Beschleunigen??
		LinkedList<ParkingSpot> list = new LinkedList<ParkingSpot>();
		for (Parking parking : parkingMap.getParkings()) {
			if (parking.type.equals("public")) {
				ParkingSpot spot = null;
				double distance = CoordUtils.calcDistance(coord,
						parking.getCoordinate());
				if (distance < VMConfig.maxDistance) {
					spot = parking.checkForFreeSpot();
					if(ev){
						spot = parking.checkForFreeSpotEVPriority();
					}
					
					if (spot != null) {
						list.add(spot);
					}
				}
			}
		}
		if (list.isEmpty()) {
			//list = null; // !! Oder Radius vergroessern?
		}

		return list;
	}
	
	
	//--------------------------- leave Parking  ---------------------------------------------
	public void leave(ActivityEndEvent event) {
		double time = event.getTime();
		Id personId = event.getPersonId();
		ParkingSpot selectedSpot = null;
		VMScoreKeeper scorekeeper = null;
		Person person = controller.getPopulation().getPersons().get(personId);
		Map<String, Object> personAttributes = person.getCustomAttributes();
		if(personAttributes.get("selectedParkingspot")!=null){
			selectedSpot = (ParkingSpot) personAttributes.get("selectedParkingspot");
			personAttributes.remove("selectedParkingspot");
			
			boolean wasOccupied = false;
			if(selectedSpot.parking.checkForFreeSpot()==null){ //Sinde alle anderen Plaetze belegt? Dann von Besetzt >> Frei
				wasOccupied = true;
			}
			selectedSpot.setOccupied(false); //Platz freigeben
			
			load.put(selectedSpot.parking.id, load.get(selectedSpot.parking.id)-1);
			
			
			//kosten auf matsim util funktion
			double duration=this.time-selectedSpot.getTimeVehicleParked(); //Parkzeit berechnen
			//System.out.println(duration);
			
			double payedParking = pricing.calculateParkingPrice(duration, false, selectedSpot.parkingPriceM); // !! EV Boolean anpassen
			// System.out.println(payed_parking);
			
			//System.out.println("bezahltes Parken (Score): "+payedParking*this.betaMoney);

			
			if (personAttributes.get("VMScoreKeeper")!= null){
				scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
			} else{
				scorekeeper = new VMScoreKeeper();
				personAttributes.put("VMScoreKeeper", scorekeeper);
			}
			scorekeeper.add(payedParking*this.betaMoney);
		
			//EVs:
			if(!evUsage){return;}
			if(evControl.hasEV(event.getPersonId())){
				if(selectedSpot.charge){
					evControl.charge(personId, selectedSpot.chargingRate, duration);
					//System.out.println("EV charged person: "+personId.toString()+" parking: "+selectedSpot.parking.id+" new state of charge [%]: "+evControl.stateOfChargePercentage(personId));
				}
			}
		
			//Events
			String spotType;
			if(selectedSpot.charge){
				spotType="ev";
			}else{
				spotType="nev";
			}

			if(evControl.hasEV(personId)){
				phwriter.addEVLeft(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), selectedSpot.parking.type, spotType, Double.toString(evControl.stateOfChargePercentage(personId)));
			} else {
				phwriter.addNEVLeft(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), selectedSpot.parking.type, spotType);
			}
			
			if(selectedSpot.parking.checkForFreeSpot()==null){
				phwriter.addParkingOccupied(selectedSpot.parking, Double.toString(this.time), personId.toString());
			}
			
			if(wasOccupied){
				phwriter.addParkingAvailible(selectedSpot.parking, Double.toString(event.getTime()));
			}	
		
		
		}
		
		
	}

	
	//--------------------------- P A R K   O N   S P O T ---------------------------------------------
	int parkOnSpot(ParkingSpot selectedSpot, double score, Id personId) {
		Person person = controller.getPopulation().getPersons().get(personId);
		Map<String, Object> personAttributes = person.getCustomAttributes();
		personAttributes.put("selectedParkingspot", selectedSpot);
		ParkingSpot selectedSpotToSet = (ParkingSpot) personAttributes.get("selectedParkingspot");
		selectedSpotToSet.setOccupied(true);
		selectedSpotToSet.setTimeVehicleParked(this.time);
		int currentLoad = load.get(selectedSpotToSet.parking.id);
		load.put(selectedSpotToSet.parking.id, currentLoad+1);
		if(peekLoad.get(selectedSpotToSet.parking.id)<currentLoad+1){
			peekLoad.put(selectedSpotToSet.parking.id, currentLoad+1);
		}
		
		
		
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		double distance = CoordUtils.calcDistance(this.cordinate, selectedSpot.parking.getCoordinate());
		double walkingTime = distance/VMConfig.walkingSpeed; 
		//System.out.println("Walking Score :"+betaWalk*walkingTime);
		scorekeeper.add(betaWalk*walkingTime);
		

		//Events
		
		String spotType;
		if(selectedSpot.charge){
			spotType="ev";
		}else{
			spotType="nev";
		}

		if(evControl.hasEV(personId)){
			phwriter.addEVParked(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), score, selectedSpot.parking.type, spotType, Double.toString(evControl.stateOfChargePercentage(personId)));
		} else {
			phwriter.addNEVParked(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), score, selectedSpot.parking.type, spotType);
		}
		
		if(selectedSpot.parking.checkForFreeSpot()==null){
			phwriter.addParkingOccupied(selectedSpot.parking, Double.toString(this.time), personId.toString());
		}
		//--
		
		return 1;
	}

	//--------------------------- ---------------------------------------------
	public void printStatistics(){
		System.out.println("Privat geparkt:" + Double.toString(this.countPrivate));
		System.out.println("Oeffentlich geparkt:" + Double.toString(this.countPublic));
		System.out.println("Nicht geparkt:" + Double.toString(this.countNotParked));
		System.out.println("EVs auf EV Spots geparkt:" + this.countEVParkedOnEVSpot);
		
		String filename = controller.getConfig().getModule("controler").getValue("outputDirectory")+"/Charts/Parkplatzauswahl_"+controller.getIterationNumber()+".png";
		XYScatterChart chart = new XYScatterChart("Parkplatzauswahl", "Time", "available Spots");
		double[] time = new double[availableParkingStat.size()];
		double[] availableParkings = new double[availableParkingStat.size()];
		int i=0;
		for(double[] element : availableParkingStat){
			time[i]=element[0];
			availableParkings[i]=element[1];
			i++;
		}
		chart.addSeries("anzahl", time, availableParkings);
		chart.saveAsPng(filename, 800, 600);
		
		CSVWriter csvWriter = new CSVWriter(controller.getConfig().getModule("controler").getValue("outputDirectory")+"/parkhistory/peekload_"+controller.getIterationNumber());
		LinkedList<LinkedList<String>> peekLoadOutput = new LinkedList<LinkedList<String>>();
		for(Parking parking : parkingMap.getParkings()){
			peekLoadOutput.add(parking.getLinkedList());
			peekLoadOutput.getLast().add(Integer.toString(peekLoad.get(parking.id)));
			System.out.println(peekLoadOutput.getLast().toString());
		}
		csvWriter.writeAll(peekLoadOutput);
		csvWriter.close();
		
		
	}
	
	//---------------------------  ---------------------------------------------
	public void resetStatistics(){
		this.countNotParked=0;
		this.countPrivate=0;
		this.countPublic=0;
		this.countEVParkedOnEVSpot=0;
		availableParkingStat.clear();
	}
	
	//--------------------------- G E T     F U T U R E     I N F O  -----
	
	public double[] getFutureInfo(ActivityStartEvent event){
		//System.out.println("Get estimated duration:");
		//[0]: Estimated duration of parking
		//[1]: Estimated distance to travel during rest of day
		
		
		PersonImpl person = (PersonImpl) controller.getPopulation().getPersons().get(event.getPersonId());
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		double endTime=0;
		int actCount = (Integer) person.getCustomAttributes().get("ActCounter");
		ActivityImpl actFromCounter = (ActivityImpl) person.getSelectedPlan().getPlanElements().get((Integer) person.getCustomAttributes().get("ActCounter"));
		ActivityImpl activity = actFromCounter;
		
		//Aktuelle activity finden:
		/*
		boolean getnext = true;
		ActivityImpl activity = (ActivityImpl) plan.getFirstActivity();
		while(getnext){
			if(activity.equals(plan.getLastActivity())){
				endTime = plan.getFirstActivity().getEndTime();
				double [] returnValue = {24*3600-event.getTime()+endTime, 0}; //Letzte activity >> Parkdauer laenger als Rest der Iteration
				
				if(actFromCounter.equals(activity)){
					System.out.println("Aktivitaeten stimmen ueberein");
				}else{
					System.out.println("F E H L E R: Aktivitaeten stimmen N I C H T ueberein");
				}
				
				return returnValue;
			}
			
			if(activity.getFacilityId().equals(event.getFacilityId()) && Math.abs(activity.getStartTime()-event.getTime())<3600){ //!! Nicht zwei activitys am gleichen Ort innerhalb einer Stunde?
				//gefunden
				getnext=false;
			} else{
				Leg leg = plan.getNextLeg(activity);
				if(leg==null){return null;}
				activity=(ActivityImpl) plan.getNextActivity(leg); // Naechste laden
				if(activity==null){ return null;} //Aktuelle activity nicht gefunden >> sollte nicht passieren
				
			}
		}

		
		if(actFromCounter.equals(activity)){
			System.out.println("Aktivitaeten stimmen ueberein");
		}else{
			System.out.println("F E H L E R: Aktivitaeten stimmen N I C H T ueberein");
		}
		*/
		
		
		//Pruefen ob letzte am Tag:
		if(activity.equals(plan.getLastActivity())){
			endTime = plan.getFirstActivity().getEndTime();
			double [] returnValue = {24*3600-event.getTime()+endTime, 0}; //Letzte activity >> Parkdauer laenger als Rest der Iteration
			return returnValue;
		}
		

		
		// Naechste Car leg nach aktueller activity finden:
		boolean foundNextCarLeg = false;
		Leg nextCarLeg=null;
		while (foundNextCarLeg == false){
			Leg leg = plan.getNextLeg(activity);
			if(leg.getMode().equalsIgnoreCase("car")){
				endTime = leg.getDepartureTime();
				nextCarLeg=leg;
				foundNextCarLeg=true;
			}else{
				Activity act = plan.getNextActivity(leg);
				if(act==null){return null;}
				leg=plan.getNextLeg(act);
				if(leg==null){
					System.out.println("F E H L E R letzte activity nicht identifiziert");
					System.out.println("Person: "+person.getId().toString()+" count: "+actCount);
					return null; //Scheint letzte Activity zu sein >> Parkdauer laenger als Rest der Iteration
				}
			}
			
		}
		
		
		//Calculate the distance to drive during the rest of the day
		double restOfDayDistance = 0;
		restOfDayDistance+=nextCarLeg.getRoute().getDistance();
		boolean goOn = true;
		while(goOn){
			Activity act = plan.getNextActivity(nextCarLeg);
			if(act==null){
				goOn=false;
				break;
			}
			nextCarLeg=plan.getNextLeg(act);
			if(nextCarLeg==null){
				break;
			}
			if(nextCarLeg.getMode().equalsIgnoreCase("car")){
				restOfDayDistance+=nextCarLeg.getRoute().getDistance();
			}
			
		}
		
		//System.out.println("Rest of day distance: "+restOfDayDistance);
		
		if(endTime==0){return null;}
		double parkDuration = endTime-event.getTime();
		double [] returnValue = {parkDuration, restOfDayDistance};
		return returnValue;
		
	}




	public void setEvControl(EVControl evControl) {
		this.evControl = evControl;
		this.evUsage=true;
	}

	
	
	public void clearAgents(){
		for (Person person : controller.getPopulation().getPersons().values()){
			person.getCustomAttributes().remove("selectedParkingspot");
		}
	}
	
	
	
}


/*//			//				EVENT??
IdImpl person_park_id = new IdImpl(person_id.toString()+"P");
ActivityStartEvent write_event= new ActivityStartEvent(event.getTime(), person_park_id, event.getLinkId(), facilityid, "ParkO");
controller.getEvents().processEvent(write_event);
//-----------
*/

//Das Programm ist jetzt zu ende!!
