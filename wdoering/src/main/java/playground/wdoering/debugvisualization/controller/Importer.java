package playground.wdoering.debugvisualization.controller;
import java.awt.Point;
import java.io.File;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;

import javax.management.AttributeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Importer implements XYVxVyEventsHandler {

	private HashMap<String, Agent> agents = null; 
	private HashMap<Integer, DataPoint> nodes = null;
	private HashMap<Integer, int[]> links = null;
	
	private Double maxPosX,maxPosY,maxPosZ,maxTimeStep=Double.MIN_VALUE;
	private Double minPosX,minPosY,minPosZ,minTimeStep=Double.MAX_VALUE;
	
	private Controller controller = null;
	
	private Double[] timeStepsAsDoubleValues;
	
	private LinkedList<Double> timeSteps;

	public Importer(Controller controller)
	{
		this.controller = controller;
		
	}

	public void readEventFile(String fileName)
	{
		try {

			File file = new File(fileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList eventList = doc.getElementsByTagName("event");

			agents = new HashMap<String,Agent>();

			int currentAgent = 0;

			timeSteps = new LinkedList<Double>();
			//minPosX = minPosY = minPosZ = minTimeStep = Double.MAX_VALUE;
			//maxPosX = maxPosY = maxPosZ = maxTimeStep = Double.MIN_VALUE;

			for (int s = 0; s < eventList.getLength(); s++) {


				Node currentNode = eventList.item(s);

				NamedNodeMap attributeList = currentNode.getAttributes();

				//check all attributes
				for (int r = 0; r < attributeList.getLength(); r++) 
				{

					//xml /w type node
					if (attributeList.item(r).getNodeName().equals("type"))
					{
						String nodeValue = attributeList.item(r).getNodeValue();
						


						if (nodeValue.equals("XYZAzimuth"))
						{

							//System.out.println("peng");
							//System.out.println("x val:" + Double.valueOf(attributeList.getNamedItem("x").getNodeValue()));
							String agentNumber = String.valueOf(attributeList.getNamedItem("person").getNodeValue());

							//Get current agent data & check if agent data has already been collected
							Agent agent = agents.get(agentNumber);
							if (agent==null)
								agent = new Agent();

							//Collect time and position
							Double time = Double.valueOf(attributeList.getNamedItem("time").getNodeValue());
							Double posX = Double.valueOf(attributeList.getNamedItem("x").getNodeValue());
							Double posY = Double.valueOf(attributeList.getNamedItem("y").getNodeValue());
							Double posZ = Double.valueOf(attributeList.getNamedItem("z").getNodeValue());

							//add time
							if (!timeSteps.contains(time))
								timeSteps.addLast(time);

							//Determine minimum and maximum positions
							maxPosX = Math.max(maxPosX, posX); minPosX = Math.min(minPosX, posX);
							maxPosY = Math.max(maxPosY, posY); minPosY = Math.min(minPosY, posY);
							maxPosZ = Math.max(maxPosZ, posZ); minPosZ = Math.min(minPosZ, posZ);
							maxTimeStep = Math.max(maxTimeStep, time); minTimeStep = Math.min(minTimeStep, time);
							
							System.out.println("px:"+posX+" | mpx: "+ maxPosX );

							//add dataPoint to agent
							agent.addDataPoint(time, posX, posY, posZ);

							//add agent data to agents hashMap
							agents.put(String.valueOf(agentNumber), agent);

						}
					}



					//System.out.println(attributeList.item(r).getNodeName() + ":" + attributeList.item(r).getNodeValue());
				}
				
				

				//System.out.println(timeLine.size());
				//System.out.println("first:" + timeLine.first() + "| last: " + timeLine.last());

				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {

				}

			}
			

		} catch (Exception e) {
			
			
			
			e.printStackTrace();
		}
	}

	public HashMap<String, Agent> importAgentData()
	{


		return agents;
	}

	public Double[] getExtremeValues()
	{
		Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep}; 
		return extremeValues;
	}
	
	public LinkedList<Double> getTimeSteps()
	{
		timeStepsAsDoubleValues = timeSteps.toArray(new Double[timeSteps.size()]);
		Arrays.sort(timeStepsAsDoubleValues);
		
		timeSteps = new LinkedList<Double>();
		for (Double timeStepValue : timeStepsAsDoubleValues)
			timeSteps.addLast(timeStepValue);
		
		return timeSteps;
		
		//return timeStepsAsDoubleValues;
	}

	/**
	 * Read the network file. This is the file that contains
	 * all the nodes and the links between them.
	 * 
	 * @param networkFileName the String containing the network file name
	 */
	public void readNetworkFile(String networkFileName)
	{
		
		nodes = new HashMap<Integer, DataPoint>();
		links = new HashMap<Integer, int[]>();

		try
		{
			

			File file = new File(networkFileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			
			//System.out.println("Root element " + doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("node");
			//System.out.println("Information of all events");

			nodes = new HashMap<Integer,DataPoint>();

			minPosX = minPosY = minPosZ = minTimeStep = Double.MAX_VALUE;
			maxPosX = maxPosY = maxPosZ = maxTimeStep = Double.MIN_VALUE;
			
			
			for (int s = 0; s < nodeList.getLength(); s++)
			{

				//Get current node and attributes
				Node currentNode = nodeList.item(s);
				NamedNodeMap attributeList = currentNode.getAttributes();

				//check all attributes (<node id="2" x="386420.2693861949" y="5819507.022613811" />)
				//Get nodeID and coordinates
				int nodeID = Integer.valueOf(attributeList.getNamedItem("id").getNodeValue());
				Double posX = Double.valueOf(attributeList.getNamedItem("x").getNodeValue());
				Double posY = Double.valueOf(attributeList.getNamedItem("y").getNodeValue());
				
				maxPosX = Math.max(maxPosX, posX); minPosX = Math.min(minPosX, posX);
				maxPosY = Math.max(maxPosY, posY); minPosY = Math.min(minPosY, posY);

				//create a new dataPoint containing the coordinates
				DataPoint nodeDataPoint = new DataPoint(posX, posY);
				
				//add node ID and coordinates to node ArrayList
				nodes.put(nodeID, nodeDataPoint);
			
				System.out.println("node (" + nodeID + ") : x:" + posX + " | y: " + posY );

			}
			
			//max/min timestep not relevant within network data & z value handling not implemented yet
			maxPosZ = minPosZ = maxTimeStep = minTimeStep = Double.NaN;
			

			//Get links
			NodeList linkList = doc.getElementsByTagName("link");

			//Iterate over all links 
			for (int s = 0; s < linkList.getLength(); s++)
			{

				//Get current link and attributes
				Node currentNode = linkList.item(s);
				NamedNodeMap attributeList = currentNode.getAttributes();

				//<link id="0" from="0" to="1" length="1.6245923691312782" freespeed="1.34" capacity="1.0" permlanes="1.0" oneway="1" modes="car" />

				//Get linkID and coordinates
				int linkID = Integer.valueOf(attributeList.getNamedItem("id").getNodeValue());
				int from = Integer.valueOf(attributeList.getNamedItem("from").getNodeValue());
				int to = Integer.valueOf(attributeList.getNamedItem("to").getNodeValue());
				
				int[] fromTo = {from,to};
				
				//add link ID and from/to to link ArrayList
				links.put(linkID, fromTo);
			
				System.out.println("link (" + linkID + ") : from:" + from + " | to: " + to );

			}


			
			
			

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public HashMap<Integer, int[]> getLinks()
	{
		return links;
	}

	public HashMap<Integer, DataPoint> getNodes()
	{
		return nodes;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(XYVxVyEvent event)
	{
		if (controller!=null)
			controller.console.println("time: " + event.getTime() + " - Agent " + event.getPersonId().toString() + ": " + event.getX() + "|" + event.getY() );
		//event.getTime()
		
		controller.updateAgentData(event.getPersonId().toString(), event.getX(), event.getY(), event.getTime());
		
	}




}
