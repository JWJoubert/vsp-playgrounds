package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.RetailerGA.RunRetailerGA;
import playground.ciarif.retailers.data.Consumer;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;
import playground.ciarif.retailers.models.GravityModel;
import playground.ciarif.retailers.utils.Utils;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class GravityModelRetailerStrategy implements RetailerStrategy { //TODO check the arguments that are passed from this class to 
	//the GravityModel class, in particular if it is really necessary to pass the controler. Have also a look to variables here, it shouldn't happen that fields here are the same as in the GravityModel class
	public static final String NAME = "gravityModelRetailerStrategy";
	private final static Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);
	private Controler controler;
	private Map<Id,ActivityFacilityImpl> shops;
	private RetailZones retailZones;
	private Map<Id, ActivityFacilityImpl> retailerFacilities;
	private Map<Id, ActivityFacilityImpl> movedFacilities = new TreeMap<Id, ActivityFacilityImpl>();

	public GravityModelRetailerStrategy(Controler controler) { //TODO think better if it is necessary to give to all strategies the controler
		
		this.controler = controler;
	}
	
	private TreeMap<Integer,String> createInitialLocationsForGA (TreeMap<Id,LinkRetailersImpl> availableLinks) {
		
		TreeMap<Integer,String> locations = new TreeMap<Integer,String> ();
		int intCount=0;
		for (ActivityFacilityImpl af:retailerFacilities.values()){
			
			locations.put(intCount,af.getLink().getId().toString());
			intCount = intCount+1;
			//log.info("The facility with Id: " + integer + " has been added");
		}
		
		for (LinkRetailersImpl l:availableLinks.values()) {
			if (locations.containsValue(l.getId())) {}
			else {
				locations.put(intCount,l.getId().toString());
				intCount = intCount+1;
				//log.info("The facility with Id: " + Integer.parseInt(l.getId().toString()) + " has been added");
			}
		}
		
		log.info("Initial Locations = " + locations);
		return locations;
		
	}
	
	private double[] computeParameters(DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers, Map<Id, Integer> shops_keys) {	
		
		int number_of_consumers = consumers.size();
		int number_of_retailer_shops = retailerFacilities.size();
		//WriteRetailersMatrices wrm = new WriteRetailersMatrices ();
		DenseDoubleMatrix1D regressand_matrix = new DenseDoubleMatrix1D(number_of_consumers);
	    if (regressand_matrix !=null) {log.info(" the regressand matrix has been created");}
	    DenseDoubleMatrix2D variables_matrix = new DenseDoubleMatrix2D(number_of_consumers, 2);
	    if (variables_matrix !=null) {log.info(" the variables matrix has been created");}
		log.info("This retailer owns " + number_of_retailer_shops + " shops and " + consumers.size() + " consumers ");
		log.info("This scenario has "+ this.retailZones.getRetailZones().size() + " zones");
		int cases = 0;
		for (Consumer c:consumers) {
			int consumer_index = Integer.parseInt(c.getId().toString());
	    	int zone_index = Integer.parseInt(c.getRzId().toString());
	    	ActivityFacilityImpl af = (ActivityFacilityImpl)c.getShoppingFacility();
    		double prob= prob_zone_shop.get(zone_index,shops_keys.get(af.getId()));
    		regressand_matrix.set(consumer_index, Math.log(prob/prob_zone_shop.viewRow(zone_index).zSum()));
    		double dist1 = (af.calcDistance(((PlanImpl) c.getPerson().getSelectedPlan()).getFirstActivity().getCoord()));
	        if (dist1 == 0.0D) {
	          dist1 = 10.0D;
	          cases = cases+1;
	        }
	        double sumDist = 0;
	        double sumDim = 0;
	        double dist2 = 0;
	        double dim = 0;
    		for (ActivityFacilityImpl aaff:this.shops.values()) {
    			dist2 = aaff.calcDistance(((PlanImpl) c.getPerson().getSelectedPlan()).getFirstActivity().getCoord());
    			sumDist = sumDist + dist2;
    			dim = aaff.getActivityOptions().get("shop").getCapacity().doubleValue();
    			sumDim = sumDim + dim;
    		}
    		variables_matrix.set(consumer_index, 0, Math.log(dist1/(sumDist/shops.size())));
    		variables_matrix.set(consumer_index, 1, Math.log(af.getActivityOptions().get("shop").getCapacity().doubleValue()/(sumDim/shops.size())));
	    	//}
	    }
		
	    log.info("A 'zero distance' has been detected and modified, in " + cases + " cases");
	    //wrm.writeRetailersMatrices(prob_zone_shop, "prob_zone_shop");
	    //wrm.writeRetailersMatrices(regressand_matrix, "regressand_matrix");
	    //wrm.writeRetailersMatrices(variables_matrix, "variables_matrix");
	    OLSMultipleLinearRegression olsmr = new OLSMultipleLinearRegression();
	    olsmr.newSampleData(regressand_matrix.toArray(), variables_matrix.toArray());
	    
	    double[] b = olsmr.estimateRegressionParameters();
	    log.info("Betas = " + b[0] + " " + b[1]);

	    return b;
	  }

	public Map<Id, ActivityFacilityImpl> getMovedFacilities() {
		log.info("moved Facilities are: " + movedFacilities);
		return this.movedFacilities;		
	}

	public Map<Id, ActivityFacilityImpl> moveFacilities(
			Map<Id, ActivityFacilityImpl> retailerFacilities,
			TreeMap<Id, LinkRetailersImpl> freeLinks) {
		// TODO Auto-generated method stub
		this.retailerFacilities = retailerFacilities;
		GravityModel gm = new GravityModel(this.controler, retailerFacilities); 
		gm.init();
		this.shops= gm.getScenarioShops();
		this.retailZones = gm.getRetailZones();
		Map<Id,Integer> shops_keys= new TreeMap<Id,Integer>();
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		DenseDoubleMatrix2D prob_i_j = new DenseDoubleMatrix2D (this.retailZones.getRetailZones().values().size(),shops.size());
		int consumer_count=0;
		int j=0;
		log.info("This scenario has " + shops.size() +" shops");
		for (ActivityFacilityImpl f:this.shops.values()) { 
			shops_keys.put(f.getId(),j);
			
			// gets the average probability of a person from a given zone going to a given shop 
			//(it is the same for all persons of a given zone)
			for (RetailZone rz : this.retailZones.getRetailZones().values()) {
				//zone_count++;
				double counter = 0;
				double prob = 0;
				ArrayList<Person> persons = rz.getPersons();
				
				for (Person p:persons) {
					boolean first_shop = true;
					for (PlanElement pe2 : p.getSelectedPlan().getPlanElements()) {
						
						if (pe2 instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe2;
							
							if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
								if (first_shop && this.retailerFacilities.containsKey(f.getId())) {
									Consumer consumer = new Consumer (consumer_count, p, rz.getId());
									consumer.setShoppingFacility(f);
									consumers.add(consumer);
									consumer_count++;
								}
								counter++;
								int i =Integer.parseInt(rz.getId().toString());
								prob = counter/persons.size();
								prob_i_j.set(i,j,prob);
								first_shop=false;
							}
						}
					}
				}
			}
			j=j+1;
		}	

		double [] b= this.computeParameters (prob_i_j, consumers, shops_keys);
		//double[] b = {-1, 1};
		gm.setBetas(b);
		RunRetailerGA rrGA = new RunRetailerGA();
		TreeMap<Integer, String> first = this.createInitialLocationsForGA(this.mergeLinks(freeLinks));
		gm.setFirst(first);
		ArrayList<Integer> solution = rrGA.runGA(first.size(),gm);		
		log.info("The optimized solution is: " + solution);
		int count=0;
		for (ActivityFacilityImpl af:this.retailerFacilities.values()) {
			if (first.get(solution.get(count)) != (af.getLink().getId().toString())) {
				Utils.moveFacility(af,controler.getNetwork().getLinks().get(new IdImpl(first.get(solution.get(count)))),this.controler.getWorld());
				log.info("The facility " + af.getId() + " has been moved");
				this.movedFacilities.put(af.getId(), af);
				log.info("Link Id after = "+ af.getLink().getId());
				count=count+1;
				log.info("Count= " + count);
			}
		}
		return movedFacilities;
	}
	
	

	private TreeMap<Id,LinkRetailersImpl> mergeLinks(TreeMap<Id,LinkRetailersImpl> freeLinks) {
		
		TreeMap<Id,LinkRetailersImpl> availableLinks = new TreeMap<Id,LinkRetailersImpl>();
		for (ActivityFacilityImpl af: this.retailerFacilities.values()){
			Id id = af.getLink().getId();
			LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)controler.getNetwork().getLinks().get(id),(NetworkLayer) controler.getNetwork());
			availableLinks.put(link.getId(),link);
		}
		availableLinks.putAll(freeLinks);
		return availableLinks;
	}

	
}
