/* *********************************************************************** *
 * project: org.matsim.*
 * SNControllerListener3.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.controler;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.activitySpaces.ActivitySpaces;
import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.interactions.NonSpatialInteractor;
import playground.jhackney.socialnetworks.interactions.SpatialInteractorEvents;
import playground.jhackney.socialnetworks.io.ActivityActReader;
import playground.jhackney.socialnetworks.io.ActivityActWriter;
import playground.jhackney.socialnetworks.io.PajekWriter;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.scoring.EventSocScoringFactory;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;
import playground.jhackney.socialnetworks.statistics.SocialNetworkStatistics;


/**
 * This controler initializes a social network which permits the exchange of influence
 * and information between agents within the MobSim iterations. The agents'
 * plans are modified according to the new information within the iterations of the MobSim.
 * Thus the social network replanning occurs in parallel to the normal replanning and not
 * serial to it. <p>
 *
 * Contrast this functionality to <a href= <a> playground/jhackney/controler/SNControllerListenerSecLoc.java</a>, which replans outside the
 * MobSim loop and generates new initial demand (100% of agents replan with social network
 * and a portion of the plans are optimized subsequently in MobSim).<p>
 *
 * It is likely that neither implementation is right or wrong, but that different
 * experiments will use different Controllers: e.g. secondary location choice still needs
 * a route and/or departure time optimization either for the new secondary activities or
 * for the primary activities.<p>
 *
 * The fraction of agents socially interacting is set in the config.xml variables,
 * "fract_s_interact" for spatial interactions, and "fract_ns_interact" for simulating
 * other interactions which occur/have occured outside the framework of the plans
 * under consideration.<p>
 *
 * After these interactions occur, a percent of agents adapt their plans to their social
 * group. This is done in a PlanAlgorithm written to make the desired kind of changes
 * one wants to make to the plans as a result of social interactions. The PlanAlgorithm
 * must be added to the StrategyManager.<p>
 *
 * Initialization of social networks can use the initial plans and/or
 * other algorithms to generate relationships.
 *
 * @author jhackney
 *
 */
public class SNControllerListener3 implements StartupListener, BeforeMobsimListener, IterationEndsListener,  ScoringListener{

	private static final boolean CALCSTATS = true;
	public static String SOCNET_OUT_DIR = null;

	SocialNetwork snet;
	SocialNetworkStatistics snetstat;
	ActivityActWriter aaw;
	ActivityActReader aar = null;
	PajekWriter pjw;
	NonSpatialInteractor plansInteractorNS;//non-spatial (not observed, ICT)

	SpatialInteractorEvents plansInteractorS;
	int snIter;
	private String [] infoToExchange;//type of info for non-spatial exchange is read in
	public static String activityTypesForEncounters[]={"home","work","shop","education","leisure"};

	private EventsMapStartEndTimes epp=null;
	private MakeTimeWindowsFromEvents teo=null;
	private LinkedHashMap<Activity,ArrayList<Double>> actStats=null;
//	private LinkedHashMap<Facility,ArrayList<TimeWindow>> twm=null;
	private EventsToScore scoring = null;

//	Variables for allocating the spatial meetings among different types of activities
	double fractionS[];
	LinkedHashMap<String,Double> rndEncounterProbs= new LinkedHashMap<String,Double>();
//	New variables for replanning
	int replan_interval;
	
	private final Logger log = Logger.getLogger(SNControllerListener3.class);

//	private Controler controler = null;
	private playground.jhackney.controler.SNController3 controler=null;
	private Knowledges knowledges;

	public SNControllerListener3(playground.jhackney.controler.SNController3 controler){
		this.controler=controler;
	}
	
	public void notifyStartup(final StartupEvent event) {
//		this.controler = event.getControler();
		this.knowledges = (controler.getScenario()).getKnowledges();
		// Complete the world to make sure that the layers all have relevant mapping rules
		new WorldConnectLocations().run(event.getControler().getWorld());

		this.log.info(" Initializing agent knowledge about geography ...");

//		initializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities());
		new InitializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities(), this.knowledges);
		this.log.info("... done");

		this.log.info("   Instantiating a new social network scoring factory with new SocialActs");

		epp=new EventsMapStartEndTimes(this.controler.getPopulation());

		this.controler.getEvents().addHandler(this.epp);

		//TODO superfluous in 0th iteration and not necessary anymore except that scoring function needs it (can null be passed?)
		teo=new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		controler.setTwm(teo.getTimeWindowMap());
//		twm=teo.getTimeWindowMap();

		this.log.info(" ... Instantiation of events overlap tracking done");
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(controler.getTwm(), controler.getFacilities());
		EventSocScoringFactory factory = new EventSocScoringFactory("leisure", controler.getScoringFunctionFactory(),actStats);
//		SocScoringFactoryEvent factory = new playground.jhackney.scoring.SocScoringFactoryEvent("leisure", actStats);

		this.controler.setScoringFunctionFactory(factory);
		this.log.info("... done");

		this.log.info("  Instantiating social network EventsToScore for scoring the plans");
//		scoring = new playground.jhackney.scoring.EventsToScoreAndReport(this.controler.getPopulation(), factory);
		scoring = new EventsToScore(this.controler.getPopulation(),factory);
		this.controler.getEvents().addHandler(scoring);
		this.log.info(" ... Instantiation of social network scoring done");

		snsetup();

	}

	public void notifyScoring(final ScoringEvent event){

		this.log.info("scoring");
//		if( event.getIteration()%replan_interval==0 && event.getIteration()!=this.controler.getFirstIteration()){
		if( event.getIteration()%replan_interval==0){
//			got new epp from mobsim
//			make new timewindows and map (uses old plans and new events)
			Gbl.printMemoryUsage();
			controler.stopwatch.beginOperation("spatialencounters");
			this.log.info(" Making time Windows and Map from Events");
//			teo=new MakeTimeWindowsFromEvents(epp);
			teo.makeTimeWindows(epp);
			this.log.info(" ... done making time windows and map");
//			twm= teo.getTimeWindowMap();
			controler.setTwm(teo.getTimeWindowMap());
//			execute spatial interactions (uses timewindows)

			if (total_spatial_fraction(this.fractionS) > 0) {
				this.plansInteractorS.interact(this.controler.getPopulation(), this.rndEncounterProbs, snIter, controler.getTwm());
			} else {
				this.log.info("     (none)");
			}
			this.log.info(" ... Spatial interactions done\n");
			controler.stopwatch.endOperation("spatialencounters");

			Gbl.printMemoryUsage();
//			execute nonspatial interactions (uses new social network)
			this.log.info(" Non-Spatial interactions ...");
			controler.stopwatch.beginOperation("infoexchange");
			for (int ii = 0; ii < this.infoToExchange.length; ii++) {
				String facTypeNS = this.infoToExchange[ii];
				if (!facTypeNS.equals("none")) {
					this.log.info("  Geographic Knowledge about all types of places is being exchanged ...");
					this.plansInteractorNS.exchangeGeographicKnowledge(facTypeNS, snIter);
				}
			}
			controler.stopwatch.endOperation("infoexchange");

//			Exchange of knowledge about people
			this.log.info("Introducing people");
			double fract_intro=Double.parseDouble(this.controler.getConfig().socnetmodule().getFriendIntroProb());
			if (fract_intro > 0) {
				this.log.info("  Knowledge about other people is being exchanged ...");
				this.plansInteractorNS.exchangeSocialNetKnowledge(snIter);
			}
			else{
				this.log.info("  No introductions");
			}
			this.log.info("  ... introducing people done");
//			forget knowledge
			//TODO  Should be an algorithm
			this.log.info("Forgetting knowledge");
			for (Person p : this.controler.getPopulation().getPersons().values()) {
//				Remember a number of activities equal to at least the number of
//				acts per plan times the number of plans in memory
				int max_memory = (int) (p.getSelectedPlan().getPlanElements().size()/2*p.getPlans().size()*1.5);
				((MentalMap)p.getCustomAttributes().get(MentalMap.NAME)).manageMemory(max_memory, p.getPlans());
			}
			this.log.info(" ... forgetting knowledge done");
			Gbl.printMemoryUsage();

			//dissolve social ties
			this.log.info(" Removing social links ...");
			controler.stopwatch.beginOperation("dissolvelinks");
			this.snet.removeLinks(snIter);
			this.log.info(" ... removing social links done");
			controler.stopwatch.endOperation("dissolvelinks");

//			make new actstats (uses new twm AND new socialnet)
			this.log.info(" Remaking actStats from events");
			this.actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(controler.getTwm(), controler.getFacilities()));

			Gbl.printMemoryUsage();

			this.log.info("SSTEST Finish Scoring with actStats "+snIter);
			scoring.finish();
			this.log.info(" ... scoring with actStats finished");

//			snIter++;
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {

		this.log.info("finishIteration ... "+event.getIteration());

		Gbl.printMemoryUsage();

		if( event.getIteration()%replan_interval==0){

			Gbl.printMemoryUsage();
			controler.stopwatch.beginOperation("netstats");
			this.log.info(" Calculating and reporting network statistics ...");
			this.snetstat.calculate(snIter, this.snet, this.controler.getPopulation(), this.knowledges);
			this.log.info(" ... done");
			controler.stopwatch.endOperation("netstats");

			Gbl.printMemoryUsage();

			if(CALCSTATS && (event.getIteration()%50==0)){

				this.log.info("  Opening the file to write out the map of Acts to Facilities");
				aaw=new ActivityActWriter(this.controler.getFacilities());
				aaw.openFile(SOCNET_OUT_DIR+"/ActivityActMap"+snIter+".txt");
				this.log.info(" Writing out the map between Acts and Facilities ...");
				aaw.write(snIter,this.controler.getPopulation());
				aaw.close();
				this.log.info(" ... done");
			}

//			if(event.getIteration()%10==0){
//			this.log.info(" Writing out social network for iteration " + snIter + " ...");
//			this.pjw.write(this.snet.getLinks(), this.controler.getPopulation(), snIter);
//			this.pjw.writeGeo(this.controler.getPopulation(), this.snet, snIter);
//			this.log.info(" ... done");
//
////			Write out the KML for the EgoNet of a chosen agent
//			this.log.info(" Writing out KMZ activity spaces and day plans for agent's egoNet");
//			Person testP=this.controler.getPopulation().getPerson("21924270");//1pct
////			Person testP=this.controler.getPopulation().getPerson("21462061");//10pct
//			EgoNetPlansItersMakeKML.loadData(testP,event.getIteration());
//			this.log.info(" ... done");
//			}
			snIter++;
		}
		if (event.getIteration() == this.controler.getLastIteration()) {
			if(CALCSTATS){
				this.log.info("----------Closing social network statistic files and wrapping up ---------------");
				this.snetstat.closeFiles();
			}
		}

		if (event.getIteration() == this.controler.getLastIteration()){

			EgoNetPlansItersMakeKML.write();
		}

	}

	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
/**
 * Clears the spatial social interaction tables (time overlap, or time windows) AFTER
 * the replanning step but before the new assignment
 */
		this.epp.reset(snIter);// I think this doesn't need to be called
		this.teo.clearTimeWindowMap();// needs to be called because it's not an eventhandler
		this.actStats.clear();// needs to be called because it's not an eventhandler
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */

	void initializeKnowledge(final PopulationImpl plans, ActivityFacilitiesImpl facilities ) {

		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		Attempt to open file of mental maps and read it in
		System.out.println("  Opening the file to read in the map of Acts to Facilities");
		int initIter = Integer.parseInt(controler.getConfig().socnetmodule().getInitIter());
		aar = new ActivityActReader(initIter);

		String fileName = controler.getConfig().socnetmodule().getInDirName()+ "ActivityActMap"+initIter+".txt";
		aar.openFile(fileName);
		System.out.println(" ... done");

		for (Person person : plans.getPersons().values()) {

			KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(person.getId());
			if(k ==null){
				k = this.knowledges.getFactory().createKnowledge(person.getId(), "created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				Plan plan = person.getPlans().get(ii);

				// TODO balmermi: double check if this is the right place to create the MentalMap and the EgoNet
				if (person.getCustomAttributes().get(MentalMap.NAME) == null) { person.getCustomAttributes().put(MentalMap.NAME,new MentalMap(k)); }
				if (person.getCustomAttributes().get(EgoNet.NAME) == null) { person.getCustomAttributes().put(EgoNet.NAME,new EgoNet()); }

				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).prepareActs(plan); // // JH Hack to make sure act types are compatible with social nets
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapRandom(plan);
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapFromFile(plan,facilities,aar);
//				Reset activity spaces because they are not read or written correctly
				ActivitySpaces.resetActivitySpaces(person);
			}
		}
		aar.close();//close the file with the input act-activity map
	}

	private void snsetup() {

//		Config config = Gbl.getConfig();

		SOCNET_OUT_DIR = this.controler.getConfig().socnetmodule().getOutDirName();// no final slash
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + SOCNET_OUT_DIR + " could not be created.");
		}

		this.replan_interval = Integer.parseInt(this.controler.getConfig().socnetmodule().getRPInt());
		String rndEncounterProbString = this.controler.getConfig().socnetmodule().getFacWt();
		String xchangeInfoString = this.controler.getConfig().socnetmodule().getXchange();
		this.infoToExchange = getFacTypes(xchangeInfoString);
		this.fractionS = toNumber(rndEncounterProbString);
		// TODO JH 12.2008 This has to coincide with the activity types in the plan
		// activityTypesForEncounters should be filled by entries in the config.xml, added to a list like replanning modules
		// rndEncounterProbString should be associated with the activity types like the probabilities for replanning modules is done
		// 
		this.rndEncounterProbs = mapActivityWeights(activityTypesForEncounters, rndEncounterProbString);

		this.log.info(" Instantiating the Pajek writer ...");
		this.pjw = new PajekWriter(SOCNET_OUT_DIR, controler.getFacilities(), this.knowledges);
		this.log.info("... done");

		this.log.info(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.controler.getPopulation(), this.controler.getFacilities());
		this.log.info("... done");

		if(CALCSTATS){
//			this.log.info(" Calculating the statistics of the initial social network)...");
			this.log.info(" Opening the files for the social network statistics...");
			this.snetstat=new SocialNetworkStatistics(SOCNET_OUT_DIR, this.controler.getFacilities());
			this.snetstat.openFiles();
//			Social networks do not change until the first iteration of Replanning,
//			so we can skip writing out this initial state because the networks will still be unchanged after the first assignment
//			this.snetstat.calculate(0, this.snet, this.controler.getPopulation());
			this.log.info(" ... done");

		}

		this.log.info("  Initializing the KML output");

		EgoNetPlansItersMakeKML.setUp(this.controler.getConfig(), this.controler.getNetwork(), this.controler.getFacilities());
		EgoNetPlansItersMakeKML.generateStyles();
		this.log.info("... done");

		this.log.info(" Writing out the initial social network ...");
		this.pjw.write(this.snet.getLinks(), this.controler.getPopulation(), this.controler.getFirstIteration());
		this.log.info("... done");

		this.log.info(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet, this.knowledges);
		this.log.info("... done");

		this.log.info(" Setting up the Spatial interactor ...");
		//InteractorTest
//		this.plansInteractorS=new SpatialInteractorActs(this.snet);
		this.plansInteractorS=new SpatialInteractorEvents(this.snet, teo, this.controler.getFacilities());
		this.log.info("... done");

		this.snIter = this.controler.getFirstIteration();
	}

	/**
	 * A method for decyphering the config codes. Part of configuration
	 * reader. Replace eventually with a routine that runs all of the
	 * facTypes but uses a probability for each one, summing to 1.0. Change
	 * the interactors accordingly.
	 *
	 * @param longString
	 * @return
	 */
	private String[] getFacTypes(final String longString) {
		String patternStr = ",";
		String[] s;
		log.info(this.getClass()+ "getFacTypes:	!!add keyword\"any\" and a new interact method to exchange info of any factility types (compatible with probabilities)");
		if (longString.equals("all-p")) {
			s = new String[5];
			s[0] = "home";
			s[1] = "work";
			s[2] = "education";
			s[3] = "leisure";
			s[4] = "shop";
		} else if (longString.equals("all+p")) {
			s = new String[6];
			s[0] = "home";
			s[1] = "work";
			s[3] = "education";
			s[4] = "leisure";
			s[5] = "shop";
			s[6] = "person";
		} else {
			s = longString.split(patternStr);
		}
		for (int i = 0; i < s.length; i++) {
			// if(s[i]!="home"&&s[i]!="work"&&s[i]!="education"&&s[i]!="leisure"&&s[i]!="shop"&&s[i]!="person"&&s[i]!="none"){
			if (!s[i].equals("home") && !s[i].equals("work") && !s[i].equals("education") && !s[i].equals("leisure")
					&& !s[i].equals("shop") && !s[i].equals("person") && !s[i].equals("none")) {
				this.log.info(this.getClass() + ":" + s[i]);
				Gbl.errorMsg("Error on type of info to exchange. Check config file. Use commas with no spaces");
			}
		}
		return s;
	}

	private double[] toNumber(final String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
	private LinkedHashMap<String,Double> mapActivityWeights(final String[] types, final String longString) {
		String patternStr = ",";
		String[] s;
		LinkedHashMap<String,Double> map = new LinkedHashMap<String,Double>();
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
			map.put(types[i],w[i]);
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return map;
	}

	private double total_spatial_fraction(final double[] fractionS2) {
//		See if we use spatial interaction at all: sum of these must > 0 or else no spatial
//		interactions take place
		double total_spatial_fraction=0;
		for (int jjj = 0; jjj < fractionS2.length; jjj++) {
			total_spatial_fraction = total_spatial_fraction + fractionS2[jjj];
		}
		return total_spatial_fraction;
	}

	/**
	 * returns the path to the specified social network iteration directory. The directory path does not include the trailing '/'
	 * @param snIter the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int snIter) {
		return Controler.getOutputFilename("ITERS/" + snIter);
	}

	/**
	 * returns the path to the specified iteration directory,
	 * including social network iteration. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @param snIter
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int iteration, final int snIter) {
		return Controler.getOutputFilename("ITERS/" + snIter + "/it." + iteration);
	}
}

