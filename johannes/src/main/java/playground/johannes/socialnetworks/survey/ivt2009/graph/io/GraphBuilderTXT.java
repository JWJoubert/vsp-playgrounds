/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilderTXT.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLWriter;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.EgoAlterTableReader.RespondentData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class GraphBuilderTXT {
	
	private static final Logger logger = Logger.getLogger(GraphBuilderTXT.class);

	private SampledSocialGraphBuilder builder = new SampledSocialGraphBuilder();
	
	private GeometryFactory geoFactory = new GeometryFactory();
	
	private Scenario scenario = new ScenarioImpl();
	
	public SampledSocialGraph buildGraph(String egoTableFile, String alterTableFile, String surveyData) throws IOException {
		EgoAlterTableReader tableReader = new EgoAlterTableReader();
		Set<RespondentData> egoData = tableReader.readEgoData(egoTableFile);
		Set<RespondentData> alterData = tableReader.readAlterData(alterTableFile);
		
		SampledSocialGraph graph = builder.createGraph(CRSUtils.getCRS(4326));
		/*
		 * build ego vertices
		 */
		Map<Integer, SampledSocialVertex> egos = new HashMap<Integer, SampledSocialVertex>();
		for(RespondentData data : egoData) {
			SampledSocialVertex vertex = builder.addVertex(graph, createPerson(data), createPoint(data));
			vertex.sample(infereIterationSampled(data.id));
			vertex.detect(Math.max(0, vertex.getIterationSampled() - 1));
			if(egos.put(data.id, vertex) != null) {
				logger.warn(String.format("An ego with ID=%1$s already exists!", data.id));
				System.exit(-1);
			}
		}
		logger.info(String.format("Built %1$s egos.", egos.size()));
		/*
		 * build alter vertices
		 */
		ErrorLogger errLogger = new ErrorLogger();
		
		Map<Integer, SampledSocialVertex> alteri = new HashMap<Integer, SampledSocialVertex>();
		for(RespondentData data : alterData) {
			SampledSocialVertex source = egos.get(data.source);
			if(source != null) {
				/*
				 * check if there is already an ego with this id
				 */
				SampledSocialVertex vertex = egos.get(data.id);
				/*
				 * if not check if there is already an alter with this id
				 */
				if(vertex == null)
					vertex = alteri.get(data.id);
				/*
				 * if not create a new one
				 */
				if(vertex == null) {
					vertex = builder.addVertex(graph, createPerson(data), createPoint(data));
					vertex.detect(source.getIterationSampled());
					alteri.put(data.id, vertex);
				} else {
					/*
					 * vertex has been named by multiple egos
					 */
					errLogger.multipleNamedVertex(vertex);
				}
				SampledSocialEdge edge = builder.addEdge(graph, vertex, source); 
				if(edge != null) {
					/*
					 * add edge attributes
					 */
				} else {
					/*
					 * edge has already been named
					 */
					errLogger.doubledEdge();
				}
			} else {
				/*
				 * the stated ego is not in the ego list
				 */
				errLogger.unknownSource(data.source);
			}
		}
	
		logger.info(String.format("Built %1$s alteri.", alteri.size()));
		
		errLogger.report();
		
		return graph;
	}
	
	private Point createPoint(RespondentData data) {
		if(data.latitude == null || data.longitude == null)
			return geoFactory.createPoint(new Coordinate(0, 0));
		else
			return geoFactory.createPoint(new Coordinate(data.latitude, data.longitude));
	}
	
	private SocialPerson createPerson(RespondentData data) {
		PersonImpl matsimPerson = new PersonImpl(scenario.createId(data.id.toString()));
		SocialPerson person = new SocialPerson(matsimPerson);
		return person;
	}
	
	private int infereIterationSampled(Integer id) {
		if(id >= 0 && id < 1000)
			return 0;
		else if(id >= 1000 && id < 10000)
			return 1;
		else if(id >= 10000)
			return 2;
		else {
			logger.warn(String.format("Cannot infere sampling iteration (%1$s)", id));
			return -1;
		}
	}
	
	private class ErrorLogger {
		
		private Set<Integer> unkownSources = new HashSet<Integer>();
		
		private Set<SampledSocialVertex> mulitpleNamedVertices = new HashSet<SampledSocialVertex>();
		
		private int doubledEdges = 0;
		
		private void unknownSource(Integer id) {
			unkownSources.add(id);
		}
		
		private void multipleNamedVertex(SampledSocialVertex vertex) {
			mulitpleNamedVertices.add(vertex);
		}
		
		private void doubledEdge() {
			doubledEdges++;
		}
		
		public void report() {
			StringBuilder builder = new StringBuilder();
			builder.append("The following egos have not been found: ");
			for(Integer id : unkownSources) {
				builder.append(id.toString());
				builder.append(" ");
			}
			logger.warn(builder.toString());
			
			logger.warn(String.format("%1$s vertices have been named by more than one ego.", mulitpleNamedVertices.size()));
			
			logger.warn(String.format("Rejected %1$s doubled eges.", doubledEdges));
		}
	}
	
	public static void main(String args[]) throws IOException {
		String egoTableFile = "/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/egos.xy.txt";
		String alterTableFile = "/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/alteri.xy.txt";
		String surveyData = "";
		
		GraphBuilderTXT builder = new GraphBuilderTXT();
		SampledSocialGraph graph = builder.buildGraph(egoTableFile, alterTableFile, surveyData);
		
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, "/Users/jillenberger/Work/work/socialnets/data/ivt2009/tmp.graphml");
	}
}
