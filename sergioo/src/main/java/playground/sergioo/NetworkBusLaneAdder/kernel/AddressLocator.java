package playground.sergioo.NetworkBusLaneAdder.kernel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Hello world!
 */
public class AddressLocator {
	
	//Constants
	private static final String HTTP_SCHEME = "http";
	private static final String GOOGLE_MAPS_HOST = "maps.google.com";
	private static final String PATH_GEOCODING = "/maps/api/geocode/";
	private static String TYPE_RESPONSE = "xml";
	private static final int HTTP_STATUS_OK = 200;
	private static final String ADDRESS_STATUS_OK = "OK";
	
	//Attributes
	/**
	 * The address to locate
	 */
	private String address;
	/**
	 * The found location
	 */
	private Coord location;
	/**
	 * The number of results
	 */
	private int numResults;
	/**
	 * If the address is already located
	 */
	private boolean located;
	
	//Methods
	/**
	 * Constructs an address locator given the address
	 * @param address
	 */
	public AddressLocator(String address) {
		super();
		this.address = address;
		located = false;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
		located = false;
	}
	/**
	 * @return the coordinates
	 * @throws Exception if the address is not located yet
	 */
	public Coord getLocation() throws Exception {
		if(located)
			return location;
		else
			throw new Exception("The address is not located yet.");
	}
	/**
	 * @return the numResults
	 */
	public int getNumResults() {
		return numResults;
	}
	/**
	 * @return the located
	 */
	public boolean isLocated() {
		return located;
	}
	/**
	 * Locates the address
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IllegalStateException 
	 * @throws BadAddressException 
	 */
	public void locate() throws URISyntaxException, ClientProtocolException, IOException, ParserConfigurationException, IllegalStateException, SAXException, BadAddressException {
		Document response = getResponse(getRequest());
		String status = ((Element)response.getElementsByTagName("status").item(0)).getChildNodes().item(0).getNodeValue();
		if(!status.equals(ADDRESS_STATUS_OK))
			throw new BadAddressException(status);
		NodeList results = response.getElementsByTagName("result");
		numResults = results.getLength();
		Element coords=((Element)((Element)((Element)results.item(0)).getElementsByTagName("geometry").item(0)).getElementsByTagName("location").item(0));
		double latitude=Double.parseDouble(coords.getElementsByTagName("lat").item(0).getChildNodes().item(0).getNodeValue());
		double longitude=Double.parseDouble(coords.getElementsByTagName("lng").item(0).getChildNodes().item(0).getNodeValue());
		location = new CoordImpl(longitude, latitude);
		located=true;
	}
	/**
	 * @return The get HTTP protocol request
	 * @throws URISyntaxException 
	 */
	public HttpGet getRequest() throws URISyntaxException {
		List<NameValuePair> qParams = new ArrayList<NameValuePair>();
		qParams.add(new BasicNameValuePair("address", address));
		qParams.add(new BasicNameValuePair("sensor", "false"));
		URI uri = URIUtils.createURI(HTTP_SCHEME, GOOGLE_MAPS_HOST, -1, PATH_GEOCODING+TYPE_RESPONSE, URLEncodedUtils.format(qParams, "UTF-8"), null);
		System.out.println("URL: "+uri);
		return new HttpGet(uri);
	}
	/**
	 * @param request
	 * @return The processed response
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws IllegalStateException
	 * @throws SAXException
	 */
	public Document getResponse(HttpGet request) throws ClientProtocolException, IOException, ParserConfigurationException, IllegalStateException, SAXException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(request);
		if(response.getStatusLine().getStatusCode()!=HTTP_STATUS_OK)
			throw new ClientProtocolException("The HTTP request is wrong.");
		//System.out.println(EntityUtils.toString(response.getEntity()));
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    return dBuilder.parse(response.getEntity().getContent());
	}

	//Main method
	/**
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws ParseException 
	 * @throws Exception 
	 */
	public static void main( String[] args ) throws ParseException, ClientProtocolException, IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Insert the required address:");
			String address = reader.readLine();
			AddressLocator ad1 = new AddressLocator(address);
			ad1.locate();
			if(ad1.getNumResults()>1)
				System.out.println("Many results: "+ad1.getNumResults()+".");
			System.out.println(ad1.getLocation());
		} catch (Exception e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	
}
