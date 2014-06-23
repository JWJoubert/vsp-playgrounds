package playground.jbischoff.taxi.berlin.supply;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class TaxiIdReader {
	
	private static final Logger log = Logger.getLogger(TaxiIdReader.class);
	private Map<Date,Integer> addToSystem;
	private Map<Date,Integer> removeFromSystem;
	private Map<Date,Integer> inSystem;
	private List<TaxiIdData> taxiIdData;
	
	public static void main(String[] args) throws ParseException {
//		for (int i = 15; i<22; i++){
	    SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date start = SDF.parse("2013-02-25 00:00:00");
	    Date end = SDF.parse("2013-03-03 00:30:00");
		TaxiIdReader tir = new TaxiIdReader(start,end);
		tir.go();
//		}
	}
	private void go(){
		
		TaxiIdParser tip = new TaxiIdParser();
		this.read("C:/local_jb/data/OD/kw9/rawFCD_20130225-20130304.dat", tip);
		this.taxiIdData = tip.getTaxiIds();
		this.analyse();
		this.write("C:/local_jb/data/OD/kw9/taxisweekly_n.csv");
		
	}
	
	
	private void write(String string) {
		BufferedWriter bw = IOUtils.getBufferedWriter(string);
		
		try {
			for (Entry<Date,Integer> sec : this.inSystem.entrySet()){
				bw.append(sec.getKey()+"\t"+sec.getValue()+"\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void analyse() {
		for (TaxiIdData td: this.taxiIdData){
			this.addTaxi(td.getStartDate());
			this.removeTaxi(td.getEndDate());
		}
	
	for (Date d : this.inSystem.keySet()){
	        int previous = 0;
	        Date previousTime = getPreviousTime(d);
	        if (this.inSystem.containsKey(previousTime)) previous = this.inSystem.get(previousTime);
	        this.incTaxi(d, previous);
			int inc = 0;
			if (this.addToSystem.containsKey(d)) inc = this.addToSystem.get(d);
			this.incTaxi(d,inc);
			if (this.removeFromSystem.containsKey(d)) {
			    this.decTaxi(d, this.removeFromSystem.get(d));
			}

	}
	}
	
	public TaxiIdReader(Date start, Date end) {
	    System.out.println(start);
	    System.out.println(end);
		this.addToSystem = new HashMap<Date, Integer>();
		this.removeFromSystem = new HashMap<Date, Integer>();
		this.inSystem = new TreeMap<Date, Integer>();
		Date currentDate = start;
		this.inSystem.put(getPreviousTime(currentDate), 0);
		do {
		    this.inSystem.put(currentDate, 0);
		    currentDate = getNextTime(currentDate);
		}
		while (currentDate.before(end));
	}
	   private Date getNextTime(Date currentTime){
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(currentTime);
	        cal.add(Calendar.SECOND, 1);
	        return cal.getTime();
	    }
	   
	   private Date getPreviousTime(Date currentTime){
           Calendar cal = Calendar.getInstance();
           cal.setTime(currentTime);
           cal.add(Calendar.SECOND, -1);
           return cal.getTime();
       }

	private void read(String file, TabularFileHandler handler) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		log.info("parsing " + file);
		config.setDelimiterTags(new String[]{"\t"});
		config.setFileName(file);
		config.setCommentTags(new String[]{"#"});
		new TabularFileParser().parse(config, handler);
		log.info("done. (parsing " + file + ")");
	}
	
	private void addTaxi (Date date){
		int amountbefore = 0;
		if (this.addToSystem.containsKey(date)) amountbefore = this.addToSystem.get(date);
		amountbefore++;
		this.addToSystem.put(date, amountbefore);
	}
	private void removeTaxi (Date time){

	    int amountbefore = 0;
	    if (this.removeFromSystem.containsKey(time)) amountbefore = this.removeFromSystem.get(time);
		amountbefore++;
		this.removeFromSystem.put(time, amountbefore);
	}
	private void incTaxi (Date time, int amount){
		int amountbefore = this.inSystem .get(time);
		amountbefore+=amount;
		this.inSystem.put(time, amountbefore);
	}
	private void decTaxi (Date time, int amount){
		int amountbefore = this.inSystem .get(time);
		amountbefore-=amount;
		this.inSystem.put(time, amountbefore);
	}
	

}


class TaxiIdData{
	private Id taxiId;
	private Date startDate;
	private Date endDate;
	
	
	TaxiIdData(Id taxiId, Date startDate, Date endDate){
		this.taxiId = taxiId;
		this.startDate = startDate;
		this.endDate = endDate;

	}

	public Id getTaxiId() {
		return taxiId;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}


	
		
}

class TaxiIdParser implements TabularFileHandler{

	private List<TaxiIdData> taxiIds = new ArrayList<TaxiIdData>();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	@Override
	public void startRow(String[] row) {
	    try {
		Id taxiId = new IdImpl(row[0]);
		Date startDate = sdf.parse(row[1]);
		Date endDate = sdf.parse(row[2]);
		
		if (!(endDate.after(startDate))) return; //kicks out 0-sec-statuses
				
		taxiIds.add(new TaxiIdData(taxiId, startDate, endDate))	;
	    }
	    catch (ParseException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}



	public List<TaxiIdData> getTaxiIds() {
		return taxiIds;
	}
	
	
	}