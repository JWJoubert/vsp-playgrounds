/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilder.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.VertexDegreeColorizer;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLWriter;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.AlterTableReader.VertexRecord;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphML2KML.SampledPartition;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class GraphBuilder {
	
	public static final Logger logger = Logger.getLogger(GraphBuilder.class);
	
	private SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(4326));
	
	private SpatialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> projBuilder
		= new SpatialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
	
	private Scenario scenario = new ScenarioImpl();
	
	private GeometryFactory geoFacotry = new GeometryFactory();

	private ErrorLogger errLogger;
	
	public SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> buildGraph(
			List<String> alterTables, List<String> egoTables, List<String> sqlDumps) throws IOException {
		errLogger = new ErrorLogger();
		/*
		 * Load raw data.
		 */
		AlterTableReader alterReader = new AlterTableReader(alterTables);
		EgoTableReader egoReader = new EgoTableReader(egoTables);
		SQLDumpReader sqlReader = new SQLDumpReader(sqlDumps);
		/*
		 * Build the raw graph and a sampled projection.
		 */
		SocialSparseGraph graph = builder.createGraph();
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> proj = projBuilder.createGraph(graph);
		/*
		 * Create the vertices.
		 */
		Map<SocialSparseVertex, SampledVertexDecorator<SocialSparseVertex>> projMap =
			new HashMap<SocialSparseVertex, SampledVertexDecorator<SocialSparseVertex>>();
		Map<String, SocialSparseVertex> idMap = new HashMap<String, SocialSparseVertex>();
//		Map<String, VertexRecord> recordMap = new HashMap<String, VertexRecord>();
		
		for(Entry<String, VertexRecord> entry : alterReader.getVertices().entrySet()) {
			VertexRecord vRecord = entry.getValue();
			/*
			 * Extract the home location.
			 */
			Point point;
			if(vRecord.isEgo) {
				point = sqlReader.getEgoLocation(vRecord.egoSQLId);
				if(point == null) {
					/*
					 * try getting coordinates via google
					 */
					logger.info("Requesting google server for coordinates.");
					point = egoReader.getEgoLocation(vRecord.id);
				}
			} else {
				point = sqlReader.getAlterLocation(vRecord.alterKeys);
			}
			if(point == null) {
				errLogger.logNoCoordinate(vRecord.isEgo);
				point = geoFacotry.createPoint(new Coordinate(0, 0));
			}
			/*
			 * Create a vertex and its projection.
			 */
			SocialSparseVertex vertex = builder.addVertex(graph, createPerson(vRecord, sqlReader), point);
			SampledVertexDecorator<SocialSparseVertex> vProj = projBuilder.addVertex(proj, vertex);
			/*
			 * If it is an ego set the snowball attributes.
			 */
			if(vRecord.isEgo) {
				vProj.sample(infereIterationSampled(new Integer(vRecord.id)));
				vProj.detect(vProj.getIterationSampled() - 1);
			}
			
			projMap.put(vertex, vProj);
			idMap.put(vRecord.id, vertex);
//			recordMap.put(vRecord.id, vRecord);
		}
		/*
		 * Create the edges.
		 */
		for(Tuple<VertexRecord, VertexRecord> edge : alterReader.getEdges()) {
			SocialSparseVertex v1 = idMap.get(edge.getFirst().id);
			SocialSparseVertex v2 = idMap.get(edge.getSecond().id);
			SocialSparseEdge socialEdge = builder.addEdge(graph, v1, v2);
			/*
			 * Check if we have double edges.
			 */
			if(socialEdge != null) {
				SampledVertexDecorator<SocialSparseVertex> vProj1 = projMap.get(v1);
				SampledVertexDecorator<SocialSparseVertex> vProj2 = projMap.get(v2);
			
				projBuilder.addEdge(proj, vProj1, vProj2, socialEdge);
				/*
				 * Set the snowball attributes if it is not an ego.
				 */
				if(!vProj1.isSampled()) {
					if(vProj1.isDetected())
						/*
						 * If this vertex is already detected check if the adjacent vertex has been sampled earlier.
						 */
						vProj1.detect(Math.min(vProj1.getIterationDetected(), vProj2.getIterationSampled()));
					else
						vProj1.detect(vProj2.getIterationSampled());
				}
				
				if(!vProj2.isSampled()) {
					if(vProj2.isDetected())
						/*
						 * If this vertex is already detected check if the adjacent vertex has been sampled earlier.
						 */
						vProj2.detect(Math.min(vProj2.getIterationDetected(), vProj1.getIterationSampled()));
					else
						vProj2.detect(vProj1.getIterationSampled());
				}
				/*
				 * add edge attributes
				 */
				VertexRecord rec1 = edge.getFirst();
				VertexRecord rec2 = edge.getSecond();
				double freq = 0;
				if(rec1.isEgo) {
					freq = sqlReader.getF2FFrequencey(rec1.egoSQLId, rec2.alterKeys.get(rec1.egoSQLId));
				} else {
					freq = sqlReader.getF2FFrequencey(rec2.egoSQLId, rec1.alterKeys.get(rec2.egoSQLId));
				}
				socialEdge.setFrequency(freq);
				
			} else {
				errLogger.logDoubleEdge();
			}
		}
		logger.info(errLogger.toString());
		return proj;
	}
	
	private SocialPerson createPerson(VertexRecord record, SQLDumpReader sqlData) {
		PersonImpl matsimPerson = new PersonImpl(scenario.createId(record.id));
		SocialPerson person = new SocialPerson(matsimPerson);
		
		int age;
		if(record.isEgo)
			age = sqlData.getEgoAge(record.egoSQLId);
		else
			age = sqlData.getAlterAge(record.alterKeys);
		
		if(age < 0)
			errLogger.logNoAge(record.isEgo);
		else
			matsimPerson.setAge(age);
		
		String sex = sqlData.getSex(record);
		if(sex != null)
			matsimPerson.setSex(sex);
		else
			errLogger.logNoSex(record.isEgo);
		
		if(record.isEgo)
			matsimPerson.setLicence(sqlData.getLicense(record));
		
		if(record.isEgo)
			matsimPerson.setCarAvail(sqlData.getCarAvail(record));
		
		person.setCitizenship(sqlData.getCitizenship(record));
		
		return person;
	}
	
	private Integer infereIterationSampled(Integer id) {
		if(id >= 0 && id < 1000)
			return 0;
		else if(id >= 1000 && id < 10000)
			return 1;
		else if(id >= 10000)
			return 2;
		else {
			logger.warn(String.format("Cannot infere sampling iteration (%1$s)", id));
			return null;
		}
	}
	
	private class ErrorLogger {
		
		private int noEgoCoords;
		
		private int noAlterCoords;
		
		private int noEgoAge;
		
		private int noAlterAge;
		
		private int doubleEdges;
		
		private int noEgoSex;
		
		private int noAlterSex;
		
		public void logNoCoordinate(boolean isEgo) {
			if(isEgo)
				noEgoCoords++;
			else
				noAlterCoords++;
		}
		
		public void logDoubleEdge() {
			doubleEdges++;
		}
		
		public void logNoAge(boolean isEgo) {
			if(isEgo)
				noEgoAge++;
			else
				noAlterAge++;
		}
		
		public void logNoSex(boolean isEgo) {
			if(isEgo)
				noEgoSex++;
			else
				noAlterSex++;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("The following warnings occurred:\n");
			builder.append(String.valueOf(noEgoCoords));
			builder.append(" egos without coordinates\n");
			builder.append(String.valueOf(noAlterCoords));
			builder.append(" alters without coordinates\n");
			builder.append(String.valueOf(noEgoAge));
			builder.append(" egos without age\n");
			builder.append(String.valueOf(noAlterAge));
			builder.append(" alters without age\n");
			builder.append(String.valueOf(doubleEdges));
			builder.append(" double edges\n");
			builder.append(String.valueOf(noEgoSex));
			builder.append(" egos without sex\n");
			builder.append(String.valueOf(noAlterSex));
			builder.append(" alters without sex\n");
			return builder.toString();
		}
	}
	
	public static void main(String args[]) throws IOException {
		GraphBuilder builder = new GraphBuilder();
		
		ArrayList<String> alterTables = new ArrayList<String>();
		alterTables.add("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/alters1.test.txt");
		alterTables.add("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/alters2.txt");
		
		ArrayList<String> egoTables = new ArrayList<String>();
		egoTables.add("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/egos1.txt");
		egoTables.add("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/egos2.txt");
		
		ArrayList<String> sqlDumps = new ArrayList<String>();
		sqlDumps.add("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/snowball.filter.csv");
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = builder.buildGraph(alterTables, egoTables, sqlDumps);
		SampledGraphProjMLWriter writer = new SampledGraphProjMLWriter(new SocialSparseGraphMLWriter());
		writer.write(graph, "/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/graph.graphml");
	}
}
