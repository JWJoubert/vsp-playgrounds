/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DRPaths;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.MatchingSegment;
import playground.droeder.data.graph.comparison.EdgeCompare;
import playground.droeder.data.graph.comparison.NodeCompare;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class GraphMatching {
	private static final Logger log = Logger.getLogger(GraphMatching.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;

	private Double deltaDist;
	private Double deltaPhi;
	private double maxLengthDiff;
	
	/*
	 * a LinkMatch is generated, if a Link is of the matchingGraph equal or a subpart 
	 * of a Link in the ReferenceGraph
	 */
	public GraphMatching(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
		this.deltaDist = Double.MAX_VALUE;
		this.deltaPhi = Double.MAX_VALUE;
		this.maxLengthDiff = 1.0;
	}
	
	public MatchingGraph getRefGraph(){
		return this.reference;
	}
	
	public MatchingGraph getCandGraph(){
		return this.matching;
	}
	
	public void setMaxDist(Double maxDeltaDist){
		this.deltaDist = maxDeltaDist;
	}
	
	public void setMaxAngle(Double maxDeltaPhi){
		this.deltaPhi = maxDeltaPhi;
	}
	
	public void setMaxLengthTolerancePerc(double lengthDiffPerc) {
		this.maxLengthDiff = lengthDiffPerc;
	}

	public void run() {
		log.info("starting bottom-up-Matching...");
		this.nodeMatchingBottomUp();
		this.computeEdgeCandidatesFromMappedNodes();
		this.edgeMatchingBottomUp();
		log.info("bottom-up-matching finished...");
		log.info("starting top-down-Matching...");
		this.edgeMatchingTopDown();
		this.nodeMatchingTopDown();
		log.info("top-down-matching finished...");
	}


	// ###### NODEMATCHING #######
	private Map<Id, List<NodeCompare>> nodesReference2match;
	private List<Id> nodesUnmatchedRef;

	private void nodeMatchingBottomUp() {
		log.info("start matching Nodes...");
		this.nodesReference2match = new HashMap<Id, List<NodeCompare>>();
		this.nodesUnmatchedRef = new ArrayList<Id>();
		
		List<NodeCompare> candidates;
		NodeCompare comp;

		// iterate over all nodes
		for(MatchingNode ref: this.reference.getNodes().values()){
			candidates = new ArrayList<NodeCompare>();
			
			//iterate over all possible candidates
			for(MatchingNode match: this.matching.getNearestNodes(ref.getCoord().getX(), ref.getCoord().getY(), this.deltaDist)){
				comp = new NodeCompare(ref, match);
				if((comp.getDist() < this.deltaDist) && (comp.getPhi() < this.deltaPhi)){
					comp.setScore(((comp.getDist() / this.deltaDist) + (comp.getPhi() / this.deltaPhi)) / 2);
					candidates.add(comp);
				}
			}
			
			if(candidates.size() > 0){
				Collections.sort(candidates);
				this.nodesReference2match.put(ref.getId(), candidates);
			}else{
				this.nodesUnmatchedRef.add(ref.getId());
			}
		}
		log.info(this.nodesUnmatchedRef.size() +" nodes are unmatched!");
		log.info(this.nodesReference2match.size() + " of " + reference.getNodes().size() + " nodes have one or more match after bottom-up node-matching!");
		log.info("node-matching finished... ");
	}

	// ##### SEGMENTMATCHING #####
	//doesn't make sense, because segments will be compared in edge matching
//	private void segmentMatching() {
//		log.info("start segment matching...");
//		this.computeEdgeCandidatesFromNodes();
//		
//		this.ref2CandEdgesFromSegmentMatching = new HashMap<Id, Map<Id,List<SegmentCompare>>>();
//		Map<Id, List<SegmentCompare>> candEdge2Segments = new HashMap<Id, List<SegmentCompare>>();
//		List<SegmentCompare> segComp;
//		SegmentCompare sc = null;
//		MatchingSegment rs = null, cs = null;
//		ListIterator<MatchingSegment> candIt, refIt;
//		
//		//iterate over all candidate edges 
//		for(Entry<MatchingEdge, List<MatchingEdge>> e: this.ref2CandEdgesFromMappedNodes.entrySet()){
//			for(MatchingEdge cand : e.getValue()){
//				segComp = new ArrayList<SegmentCompare>();
//				
//				candIt = cand.getSegments().listIterator();
//				refIt = e.getKey().getSegments().listIterator();
//				
//				while(candIt.hasNext() && refIt.hasNext()){
//					if((rs == null) && (cs == null)){
//						rs = refIt.next();
//						cs = candIt.next();
//					}else if(sc.refIsUndershot()){
//						rs = refIt.next();
//					}else if(!sc.refIsUndershot()){
//						cs = candIt.next();
//					}
//					sc = new SegmentCompare(rs, cs);
//					segComp.add(sc);
//				}
//				candEdge2Segments.put(cand.getId(), segComp);
//			}
//			ref2CandEdgesFromSegmentMatching.put(e.getKey().getId(), candEdge2Segments);
//		}
//		
//		// clear temporally matched Edges to save memory 
//		this.ref2CandEdgesFromMappedNodes.clear();
//		log.info("segment matching finished...");
//	}
	
	private Map<MatchingEdge, List<MatchingEdge>> edgeCandidatesRef2matchFromNodes;

	private ArrayList<Id> edgesUnmatchedRef;
	private void computeEdgeCandidatesFromMappedNodes() {
		log.info("compute candidate edges from mapped nodes...");
		this.edgeCandidatesRef2matchFromNodes = new HashMap<MatchingEdge, List<MatchingEdge>>();
		this.edgesUnmatchedRef = new ArrayList<Id>();
		
		Id candFrom, candTo, refFrom, refTo;
		List<MatchingEdge> tempCandidates;
		
		//iterate over all edges
		for(MatchingEdge ref : this.reference.getEdges().values()){
			tempCandidates = new ArrayList<MatchingEdge>();
			refFrom = ref.getFromNode().getId();
			refTo = ref.getToNode().getId();

			// if the matched nodes contain the start- and end-node, go on
			if(this.nodesReference2match.containsKey(refFrom) && 
					this.nodesReference2match.containsKey(refTo)){
				// iterate over all edges going out from the candidateStartNode which is mapped to the referenceStartNode 
				for(MatchingEdge cand : this.matching.getNodes().get(this.nodesReference2match.get(refFrom).get(0).getCandId()).getOutEdges()){
					candFrom = cand.getFromNode().getId();
					candTo = cand.getToNode().getId();

					// if the refNodes and candNodes where mapped in NodeMatching, store the candidateEdge  
					if(this.nodesReference2match.get(refFrom).get(0).getCandId().equals(candFrom) 
							&& this.nodesReference2match.get(refTo).get(0).getCandId().equals(candTo)){
						tempCandidates.add(cand);
					}
				}
			}
			if(tempCandidates.size() > 0){
				this.edgeCandidatesRef2matchFromNodes.put(ref, tempCandidates);
			}else{
				this.edgesUnmatchedRef.add(ref.getId());
			}
		}
		log.info(this.edgeCandidatesRef2matchFromNodes.size() + " of " + reference.getEdges().size() + " edges from the reference-Graph are preMapped");
	}

	// ##### EDGEMATCHING #####
	private Map<Id, List<EdgeCompare>> edgesRef2Match;
	private Map<Id, List<EdgeCompare>> edgesRef2MatchUnmatchedAgain;

	private Map<Id, List<EdgeCompare>> edgesRef2MatchPart;
	private void edgeMatchingBottomUp() {
		log.info("start bottom-up edge-matching...");
		
		edgesRef2Match = new HashMap<Id, List<EdgeCompare>>();
		edgesRef2MatchPart = new HashMap<Id, List<EdgeCompare>>();
		edgesRef2MatchUnmatchedAgain = new HashMap<Id, List<EdgeCompare>>();
		List<EdgeCompare> tempComp;
		List<EdgeCompare> tempCompPart;
		List<EdgeCompare>  tempUnmatched;
		EdgeCompare comp;
		
		for(Entry<MatchingEdge, List<MatchingEdge>> e: edgeCandidatesRef2matchFromNodes.entrySet()){
			if(e.getKey().getId().toString().contains("U5")){
				log.debug("debug");
			}
			tempComp = new ArrayList<EdgeCompare>();
			tempCompPart = new ArrayList<EdgeCompare>();
			tempUnmatched = new ArrayList<EdgeCompare>();
			for(MatchingEdge cand : e.getValue()){
				comp = new EdgeCompare(e.getKey(), cand);
				if(comp.isMatched(deltaDist, deltaPhi, maxLengthDiff)){
					tempComp.add(comp);
				}else if(comp.isPartlyMatched(deltaDist, deltaPhi, maxLengthDiff)){
					tempCompPart.add(comp);
				}else{
					tempUnmatched.add(comp);
				}
			}
			if(tempComp.size() > 0){
				Collections.sort(tempComp);
				edgesRef2Match.put(e.getKey().getId(), tempComp);
			}else if(tempCompPart.size() > 0){
				Collections.sort(tempComp);
				edgesRef2MatchPart.put(e.getKey().getId(), tempCompPart);
			}else{
				edgesUnmatchedRef.add(e.getKey().getId());
				if(tempUnmatched.size() > 0){
					edgesRef2MatchUnmatchedAgain.put(e.getKey().getId(), tempUnmatched);
				}
			}
		}
		log.info(edgesRef2Match.size() + " of " + reference.getEdges().size() + " edges have one or more match after bottom-up edge-matching...");
		log.info("edge matching finished...");
	}
	
	
	
	// ################## top-down-matching ###################
	
	private void edgeMatchingTopDown(){
		log.info("starting top-down edge matching...");
		List<Id> newMatched = new ArrayList<Id>();
		List<EdgeCompare> tempComp;
		List<EdgeCompare> tempCompPart;
		EdgeCompare comp;
		Coord refCoord;
		MatchingEdge refEdge;
		int cnt = 1,
		msg = 1;
		for(Id ref: edgesUnmatchedRef){
			refEdge = this.reference.getEdges().get(ref);
			refCoord = refEdge.getFromNode().getCoord();
			tempComp = new ArrayList<EdgeCompare>();
			tempCompPart = new ArrayList<EdgeCompare>();
			/*
			 *  here it would be correct (according to the given algorithm) to use ALL Edges
			 *  but this would cause a very large computation time
			 *  so my decision is to use only the nearest edges. Assuming that the coordinate-system is correct,
			 *  it wouldn't make sense to compare edges which are not in an acceptable distance.
			 */
			for(MatchingNode n: this.matching.getNearestNodes(refCoord.getX(), refCoord.getY(), deltaDist)){
				for(MatchingEdge e : n.getOutEdges()){
					comp = new EdgeCompare(refEdge, e);
//					if(comp.isMatched(deltaDist, deltaPhi, maxLengthDiff)){
//						tempComp.add(comp);
//					}else 
					if(comp.isPartlyMatched(deltaDist, deltaPhi, maxLengthDiff)){
						tempCompPart.add(comp);
					}
				}
			}
			// store only, if there is at least one match
//			if(tempComp.size() > 0 ){
//				Collections.sort(tempComp);
//				edgesRef2Match.put(ref, tempComp);
//				newMatched.add(ref);
//			}else
			if(tempCompPart.size() > 0){
				Collections.sort(tempCompPart);
				edgesRef2MatchPart.put(ref, tempCompPart);
				newMatched.add(ref);
			}
			if(cnt%msg == 0){
				log.info("processed " + cnt + " of " + edgesUnmatchedRef.size() + " of unmatched Edges. New matched: " + newMatched.size());
				msg *= 2;
			}
			cnt++;
		}
		
		//remove newMatched from unmatched
		for(Id id: newMatched){
			this.edgesUnmatchedRef.remove(id);
		}
		log.info(newMatched.size() + " edges are new matched after top-down edge matching!");
		log.info(edgesRef2Match.size() + " edges are matched after top-down edge-matching!");
		log.info("finished top-down edge-matching...");
	}
	
	Map<Id, Id> nodeFinalRef2Match;
	Map<Id, Id> edgeFinalRef2Match;
	Map<Id, Id> edgeFinalRef2MatchPart;
	private void nodeMatchingTopDown(){
		nodeFinalRef2Match = new HashMap<Id, Id>();
		edgeFinalRef2Match = new HashMap<Id, Id>();
		edgeFinalRef2MatchPart = new HashMap<Id, Id>();
		
		EdgeCompare c;
		for(Entry<Id, List<EdgeCompare>> e: edgesRef2Match.entrySet()){
			c = e.getValue().get(0);
			this.edgeFinalRef2Match.put(c.getRefId(), c.getCandId());
		}
		
		for(Entry<Id, List<EdgeCompare>> e : edgesRef2MatchPart.entrySet()){
			c = e.getValue().get(0);
			this.edgeFinalRef2MatchPart.put(c.getRefId(), c.getCandId());
		}
		
		// TODO implement nodeMatching Top-down
//		MatchingEdge ref, match;
//		for(Entry<Id, List<EdgeCompare>> e: edgesRef2Match.entrySet()){
//			for(EdgeCompare ee: e.getValue()){
//				ref = this.reference.getEdges().get(ee.getRefId());
//				match = this.matching.getEdges().get(ee.getCompId());
//				
//				if(this.nodesReference2match.containsKey(ref.getFromNode().getId()) && this.nodesReference2match.containsKey(ref.getToNode().getId())){
//					if(!this.nodesReference2match.get(ref.getFromNode().getId()).equals(match.getFromNode().getId())
//							&& !this.nodesReference2match.get(ref.getToNode().getId()).equals(match.getToNode().getId())){
//						
//					}
//				}
//			}
//		}
	}
	
	public Map<Id, List<NodeCompare>> getNodeIdRef2Match(){
		return this.nodesReference2match;
	}
	
	
	//####### results 2 shape ########
	public void nodes2Shape(String outPath){
		Map<String, Coord> ref = new HashMap<String, Coord>();
		
		Map<String, SortedMap<String, String>> attrib = new HashMap<String, SortedMap<String,String>>();
		
		MatchingNode refNode, matchNode;
		int matched = 0;
		for(Entry<Id, List<NodeCompare>> e: this.nodesReference2match.entrySet()){
			refNode = this.reference.getNodes().get(e.getKey());
			matchNode = this.matching.getNodes().get(e.getValue().get(0).getCandId());
			
			ref.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), refNode.getCoord());
			ref.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), matchNode.getCoord());
			
			SortedMap<String, String> temp = new TreeMap<String, String>();
			temp.put("match_nr", String.valueOf(matched));
			attrib.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), temp);
			attrib.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), temp);
			matched++;
		}
		
		DaShapeWriter.writeDefaultPoints2Shape(outPath + "matched_Nodes.shp", "matched_Nodes", ref, attrib);
		
		Map<String, Coord> unmatched = new HashMap<String, Coord>();
		
		if(nodesUnmatchedRef.size()>0){
			for(Id id : nodesUnmatchedRef){
				unmatched.put(id.toString(), this.reference.getNodes().get(id).getCoord());
			}
			
			DaShapeWriter.writeDefaultPoints2Shape(outPath + "unmatched_Nodes.shp", "unmatched_Nodes", unmatched, null);
		}else{
			log.info("can not write " +outPath + "unmatched_Nodes.shp, because no nodes in the ref network are unmatched...");
		}
	}
	
	public void baseSegments2Shape(String outpath){
		Map<String, SortedMap<Integer, Coord>> edges = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> temp;
		int i;
		for(Entry<Id, MatchingEdge> e: this.reference.getEdges().entrySet()){
			temp = new TreeMap<Integer, Coord>();
			i = 0;
			for(MatchingSegment s: e.getValue().getSegments()){
				temp.put(i, s.getStart());
				i++;
				temp.put(i, s.getEnd());
				i++;
			}
			edges.put(e.getKey().toString(), temp);
		}
		DaShapeWriter.writeDefaultLineString2Shape(outpath + "refGraphSegments.shp", "refGraphSegments", edges, null);

		edges = new HashMap<String, SortedMap<Integer,Coord>>();
		for(Entry<Id, MatchingEdge> e: this.matching.getEdges().entrySet()){
			temp = new TreeMap<Integer, Coord>();
			i = 0;
			for(MatchingSegment s: e.getValue().getSegments()){
				temp.put(i, s.getStart());
				i++;
				temp.put(i, s.getEnd());
				i++;
			}
			edges.put(e.getKey().toString(), temp);
		}
		DaShapeWriter.writeDefaultLineString2Shape(outpath + "matchingGraphSegments.shp", "matchingGraphSegments", edges, null);
	}
	
	public void matchedSegments2Shape(String outPath){
		if(edgesRef2Match.size() < 1){
			return;
		}
		Map<String, SortedMap<Integer, Coord>> edges = new HashMap<String, SortedMap<Integer,Coord>>();
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		
		SortedMap<Integer, Coord> coords;
		SortedMap<String, String> attribValues;
		
		EdgeCompare e;
		MatchingEdge ref, cand;
		String refId, candId;
		int cnt;
		int matchNr = 0;
		
		for(List<EdgeCompare> el: this.edgesRef2Match.values()){
			attribValues = new TreeMap<String, String>();
			attribValues.put("matchNr", String.valueOf(matchNr));

			coords = new TreeMap<Integer, Coord>();
			
			e = el.get(0);

			ref = reference.getEdges().get(e.getRefId());
			refId = String.valueOf(matchNr) + "_" + "ref_" + ref.getId().toString();
			cnt = 0;
			for(MatchingSegment s: ref.getSegments()){
				coords.put(cnt, s.getStart());
				cnt++;
				coords.put(cnt, s.getEnd());
				cnt++;
			}
			edges.put(refId, coords);
			attribs.put(refId, attribValues);
			
			cand = matching.getEdges().get(e.getCandId());
			candId = String.valueOf(matchNr) + "_" + "cand_" + cand.getId().toString();
			cnt = 0;
			coords =  new TreeMap<Integer, Coord>();
			for(MatchingSegment s: cand.getSegments()){
				coords.put(cnt, s.getStart());
				cnt++;
				coords.put(cnt, s.getEnd());
				cnt++;
			}
			edges.put(candId, coords);
			attribs.put(candId, attribValues);
			
			matchNr++;
		}
		
		DaShapeWriter.writeDefaultLineString2Shape(outPath + "matchedSegments.shp", "matchedSegments", edges, attribs);
	}
	
	// just for debugging
	public void unmatchedAfterPrematchingOut(String outPath){
		if(edgesRef2Match.size() < 1){
			return;
		}
		Map<String, SortedMap<Integer, Coord>> edges = new HashMap<String, SortedMap<Integer,Coord>>();
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		
		SortedMap<Integer, Coord> coords;
		SortedMap<String, String> attribValues;
		
		EdgeCompare e;
		MatchingEdge ref, cand;
		String refId, candId;
		int cnt;
		int matchNr = 0;
		
		for(List<EdgeCompare> el: this.edgesRef2MatchUnmatchedAgain.values()){
			attribValues = new TreeMap<String, String>();
			attribValues.put("matchNr", String.valueOf(matchNr));

			coords = new TreeMap<Integer, Coord>();
			
			e = el.get(0);

			ref = reference.getEdges().get(e.getRefId());
			refId = String.valueOf(matchNr) + "_" + "ref_" + ref.getId().toString();
			cnt = 0;
			for(MatchingSegment s: ref.getSegments()){
				coords.put(cnt, s.getStart());
				cnt++;
				coords.put(cnt, s.getEnd());
				cnt++;
			}
			edges.put(refId, coords);
			attribs.put(refId, attribValues);
			
			for(EdgeCompare ee : el){
				cand = matching.getEdges().get(ee.getCandId());
				candId = String.valueOf(matchNr) + "_" + "cand_" + cand.getId().toString();
				cnt = 0;
				coords =  new TreeMap<Integer, Coord>();
				for(MatchingSegment s: cand.getSegments()){
					coords.put(cnt, s.getStart());
					cnt++;
					coords.put(cnt, s.getEnd());
					cnt++;
				}
				edges.put(candId, coords);
				attribs.put(candId, attribValues);
			}
			
			matchNr++;
		}
		
//		DaShapeWriter.writeDefaultLineString2Shape(outPath + "edgesUnmatchedAfterPreMatching.shp", "edgesUnmatchedAfterPreMatching", edges, attribs);
		
		// to string
		BufferedWriter w = IOUtils.getBufferedWriter(outPath + "edgesUnmatchedAfterPreMatching.csv");
		try {
			w.write("refID; matchID; avDist; avAngle; refTotalLength; refMatchedLength; matchTotalLength; matchMatchedLength;");
			w.newLine();
			
			for(List<EdgeCompare> el : this.edgesRef2MatchUnmatchedAgain.values()){
				for(EdgeCompare ec: el){
					w.write(ec.getRefId() + ";" + ec.getCandId() + ";" + ec.getAvDist() + ";" + ec.getAvAngle() + ";" +
							ec.getRefTotalLength() + ";" + ec.getMatchedLengthRef() + ";" + ec.getCompTotalLength() + ";" + ec.getMatchedLengthComp() + ";");
					w.newLine();
				}
			}
			w.flush();
			w.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
	public static void main(String[] args){
		MatchingGraph ref = new MatchingGraph();
		MatchingNode r1, r2, r3;
		
		r1 = new MatchingNode(new IdImpl("r1"), new CoordImpl(0, 0));
		r2 = new MatchingNode(new IdImpl("r2"), new CoordImpl(0, 100));
		r3 = new MatchingNode(new IdImpl("r3"), new CoordImpl(0,200));
		ref.addNode(r1);
		ref.addNode(r2);
		ref.addNode(r3);
		
		ref.addEdge(new MatchingEdge(new IdImpl("re12"), r1, r2));
		ref.addEdge(new MatchingEdge(new IdImpl("re23"), r2, r3));
		ref.addEdge(new MatchingEdge(new IdImpl("re32"), r3, r2));
		ref.addEdge(new MatchingEdge(new IdImpl("re21"), r2, r1));
		
		MatchingGraph match = new MatchingGraph();
		final MatchingNode m1, m2, m3, m4, m5, m6;
		m1 = new MatchingNode(new IdImpl("m1"), new CoordImpl(1,-1));
		m2 = new MatchingNode(new IdImpl("m2"), new CoordImpl(100,100));
		m3 = new MatchingNode(new IdImpl("m3"), new CoordImpl(1,201));
		m4 = new MatchingNode(new IdImpl("m4"), new CoordImpl(-10,-1));
		m5 = new MatchingNode(new IdImpl("m5"), new CoordImpl(-11,100));
		m6 = new MatchingNode(new IdImpl("m6"), new CoordImpl(-10,202));
		match.addNode(m1);
		match.addNode(m2);
		match.addNode(m3);
		match.addNode(m4);
		match.addNode(m5);
		match.addNode(m6);
		
		MatchingEdge me12 = new MatchingEdge(new IdImpl("me12"), m1, m2);
//		me12.addShapePointsAndCreateSegments(new ArrayList<Coord>(){{
//			add(m1.getCoord());
//			add(new CoordImpl(5,50));
//			add(m2.getCoord());
//		}});
		match.addEdge(me12);
		match.addEdge(new MatchingEdge(new IdImpl("me23"), m2, m3));
		match.addEdge(new MatchingEdge(new IdImpl("me32"), m3, m2));
		match.addEdge(new MatchingEdge(new IdImpl("me21"), m2, m1));
		
		MatchingEdge me45 = new MatchingEdge(new IdImpl("me45"), m4, m5);
		me45.addShapePointsAndCreateSegments(new ArrayList<Coord>(){{
			add(m4.getCoord());
			add(new CoordImpl(5,25));
			add(new CoordImpl(-5,50));
			add(m5.getCoord());
		}});
		match.addEdge(me45);
		match.addEdge(new MatchingEdge(new IdImpl("me56"), m5, m6));
		match.addEdge(new MatchingEdge(new IdImpl("me65"), m6, m5));
		match.addEdge(new MatchingEdge(new IdImpl("me54"), m5, m4));
		
		
		final String OUT = DRPaths.PROJECTS + "geoAlgorithm/";
		GraphMatching gm = new GraphMatching(ref, match);
		gm.setMaxAngle(Math.PI/4);
		gm.setMaxDist(25.0);
		gm.setMaxLengthTolerancePerc(0.2);
		gm.run();
		gm.nodes2Shape(OUT);
		gm.baseSegments2Shape(OUT);
		gm.matchedSegments2Shape(OUT);
	}
	
//	public static void main(String[] args){
//		final String PATH = DaPaths.OUTPUT + "bvg09/";
//		final String OUT = DaPaths.OUTPUT + "geoAlgorithm/";
//		final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
//		final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
//		
//
//		MatchingEdge e;
//		MatchingNode start, end;
//		
//		ScenarioImpl visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		visumSc.getConfig().scenario().setUseTransit(true);
//		TransitScheduleReader reader = new TransitScheduleReader(visumSc);
//		reader.readFile(VISUMTRANSITFILE);
//		MatchingGraph v = new MatchingGraph();
//		List<TransitStopFacility> facs;
//		ArrayList<Coord> shape;
//		
//		String temp;
//		for(TransitLine line: visumSc.getTransitSchedule().getTransitLines().values()){
//			temp = line.getId().toString().substring(0, 1);
//			
//			if(temp.equals("P") || temp.equals("S") || temp.equals("R") || temp.equals("V") || temp.equals("N")) continue;
//			
//			for(TransitRoute route: line.getRoutes().values()){
//				facs = new ArrayList<TransitStopFacility>();
//				shape = new ArrayList<Coord>();
//				for(TransitRouteStop stop : route.getStops()){
//					facs.add(stop.getStopFacility());
//					shape.add(stop.getStopFacility().getCoord());
//				}
//				if(facs.size() < 2){
//					log.error("can not create an edge for TransitRoute " + route.getId() + " on TransitLine " +
//							line.getId() + " beacause it have less than 2 stops!");
//					continue;
//				}
//				
//				// create or get start-node
//				if(v.getNodes().containsKey(facs.get(0).getId())){
//					start = v.getNodes().get(facs.get(0).getId());
//				}else{
//					start = new MatchingNode(facs.get(0).getId(), facs.get(0).getCoord());
//					v.addNode(start);
//				}
//
//				// create or get end-node
//				if(v.getNodes().containsKey(facs.get(facs.size()-1).getId())){
//					end = v.getNodes().get(facs.get(facs.size()-1).getId());
//				}else{
//					end = new MatchingNode(facs.get(facs.size()-1).getId(), facs.get(facs.size()-1).getCoord());
//					v.addNode(end);
//				}
//				
//				e = new MatchingEdge(route.getId(), start, end);
//				e.addShapePointsAndCreateSegments(shape);
//				v.addEdge(e);
//			}
//		}
//		
//		ScenarioImpl hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		hafasSc.getConfig().scenario().setUseTransit(true);
//		TransitScheduleReader reader2 = new TransitScheduleReader(hafasSc);
//		reader2.readFile(HAFASTRANSITFILE);
//		MatchingGraph h = new MatchingGraph();
//		
//		
//		for(TransitLine line: hafasSc.getTransitSchedule().getTransitLines().values()){
//			for(TransitRoute route: line.getRoutes().values()){
//				facs = new ArrayList<TransitStopFacility>();
//				shape = new ArrayList<Coord>();
//				for(TransitRouteStop stop : route.getStops()){
//					facs.add(stop.getStopFacility());
//					shape.add(stop.getStopFacility().getCoord());
//				}
//				if(facs.size() < 2){
//					log.error("can not create an edge for TransitRoute " + route.getId() + " on TransitLine " +
//							line.getId() + " beacause it have less than 2 stops!");
//					continue;
//				}
//				
//				if(h.getNodes().containsKey(facs.get(0).getId())){
//					start = h.getNodes().get(facs.get(0).getId());
//				}else{
//					start = new MatchingNode(facs.get(0).getId(), facs.get(0).getCoord());
//					h.addNode(start);
//				}
//				
//				if(h.getNodes().containsKey(facs.get(facs.size()-1).getId())){
//					end = h.getNodes().get(facs.get(facs.size()-1).getId());
//				}else{
//					end = new MatchingNode(facs.get(facs.size()-1).getId(), facs.get(facs.size()-1).getCoord());
//					h.addNode(end);
//				}
//				
//				e = new MatchingEdge(new IdImpl(line.getId() + "_" + route.getId()), 
//						h.getNodes().get(facs.get(0).getId()), h.getNodes().get(facs.get(facs.size()-1).getId()));
//				e.addShapePointsAndCreateSegments(shape);
//				h.addEdge(e);
//			}
//		}
//		
//		GraphMatching gm = new GraphMatching(v, h);
//		gm.setMaxAngle(Math.PI / 6);
//		gm.setMaxDist(500.0);
//		gm.setMaxLengthTolerancePerc(0.1);
//		gm.run();
//		gm.nodes2Shape(OUT);
//		gm.baseSegments2Shape(OUT);
//		gm.matchedSegments2Shape(OUT);
//		gm.unmatchedAfterPrematchingOut(OUT);
//	}
	
//	public static void main(String[] args){
//		final String PATH = DaPaths.OUTPUT + "bvg09/";
//		final String OUT = DaPaths.OUTPUT + "geoAlgorithm/";
//		final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
//		final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
//		
//		ScenarioImpl visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		visumSc.getConfig().scenario().setUseTransit(true);
//		TransitScheduleReader reader = new TransitScheduleReader(visumSc);
//		reader.readFile(VISUMTRANSITFILE);
//		MatchingGraph v = new MatchingGraph();
//		
//		for(TransitStopFacility stop : visumSc.getTransitSchedule().getFacilities().values()){
//			v.addNode(new MatchingNode(stop.getId(), stop.getCoord()));
//		}
//		
//		TransitStopFacility fac = null;
//		int i = 0;
//		String temp;
//		for(TransitLine line: visumSc.getTransitSchedule().getTransitLines().values()){
//			temp = line.getId().toString().substring(0, 1);
//			
//			if(temp.equals("P") || temp.equals("S") || temp.equals("R") || temp.equals("V") || temp.equals("N")) continue;
//			for(TransitRoute route: line.getRoutes().values()){
//				for(TransitRouteStop stop : route.getStops()){
//					if(!(fac == null)){
//						v.addEdge(new MatchingEdge(new IdImpl(i), v.getNodes().get(fac.getId()), v.getNodes().get(stop.getStopFacility().getId())));
//					}
//					fac = stop.getStopFacility();
//					i++;
//				}
//				fac = null;
//			}
//		}
//		
//		ScenarioImpl hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		hafasSc.getConfig().scenario().setUseTransit(true);
//		TransitScheduleReader reader2 = new TransitScheduleReader(hafasSc);
//		reader2.readFile(HAFASTRANSITFILE);
//		MatchingGraph h = new MatchingGraph();
//		
//		for(TransitStopFacility stop : hafasSc.getTransitSchedule().getFacilities().values()){
//			h.addNode(new MatchingNode(stop.getId(), stop.getCoord()));
//		}
//		
//		fac = null;
//		i = 0;
//		for(TransitLine line: hafasSc.getTransitSchedule().getTransitLines().values()){
//			for(TransitRoute route: line.getRoutes().values()){
//				for(TransitRouteStop stop : route.getStops()){
//					if(!(fac == null)){
//						h.addEdge(new MatchingEdge(new IdImpl(i), h.getNodes().get(fac.getId()), h.getNodes().get(stop.getStopFacility().getId())));
//					}
//					fac = stop.getStopFacility();
//					i++;
//				}
//				fac =null;
//			}
//		}
//		
//		GraphMatching gm = new GraphMatching(v, h);
//		gm.setMaxAngle(Math.PI / 6);
//		gm.setMaxDist(250.0);
//		gm.setMaxLengthTolerancePerc(0.1);
//		gm.run();
//		gm.nodes2Shape(OUT);
//		gm.baseSegments2Shape(OUT);
//		gm.matchedSegments2Shape(OUT);
//		gm.unmatchedAfterPrematchingOut(OUT);
//	}

	/**
	 * @return
	 */
	public Map<Id, Id> getCompleteMatchedEdges() {
		return this.edgeFinalRef2Match;
	}
	
	public Map<Id, Id> getPartlyMatchedEdges(){
		return this.edgeFinalRef2MatchPart;
	}
	

}

