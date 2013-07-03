/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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


package playground.wdoering.grips.scenariomanager.control;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.shape.BoxShape;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.LineShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape.DrawMode;
import playground.wdoering.grips.scenariomanager.model.shape.ShapeStyle;

/**
 * helper class for creating standard shapes
 * 
 * @author wdoering
 *
 */
public class ShapeFactory
{

	public static BoxShape getNetBoxShape(int shapeRendererId, Rectangle2D bbRect, boolean light)
	{
		//create box shape, set description and id
		BoxShape boundingBox = new BoxShape(shapeRendererId, bbRect);
		boundingBox.setDescription(Constants.DESC_OSM_BOUNDINGBOX);
		boundingBox.setId(Constants.ID_NETWORK_BOUNDINGBOX);
		
		//set style
		ShapeStyle style;
		if (light)
			style = new ShapeStyle(Constants.COLOR_NET_LIGHT_BOUNDINGBOX_FILL, Constants.COLOR_NET_LIGHT_BOUNDINGBOX_FILL, 1f, DrawMode.FILL);
		else
			style = new ShapeStyle(Constants.COLOR_NET_BOUNDINGBOX_FILL, Constants.COLOR_NET_BOUNDINGBOX_CONTOUR, 4f, DrawMode.FILL_WITH_CONTOUR);
		
		boundingBox.setStyle(style);
		
		return boundingBox;
	}
	
	public static CircleShape getEvacCircle(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		CircleShape evacCircle = new CircleShape(shapeRendererId, c0, c1);
		evacCircle.setId(Constants.ID_EVACAREAPOLY);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_EVACAREA;
		evacCircle.setStyle(style);
		
		return evacCircle;
	}
	
	public static CircleShape getPopShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		CircleShape popCircle = new CircleShape(shapeRendererId, c0, c1);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_POPAREA;
		style.setHoverColor(Constants.COLOR_POPAREA_HOVER);
		style.setSelectColor(Constants.COLOR_POPAREA_SELECTED);
		popCircle.setStyle(style);
		
		return popCircle;
	}
	
	public static LineShape getRoadClosureShape(int shapeRendererId, String linkID, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape roadClosureLine = new LineShape(shapeRendererId, c0, c1);
		roadClosureLine.setId(Constants.ID_ROADCLOSURE_PREFIX + linkID);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_ROADCLOSURE;
		style.setSelectColor(Constants.COLOR_POPAREA_SELECTED);
		roadClosureLine.setStyle(style);
		
		roadClosureLine.setArrow(false);
		return roadClosureLine;
	}
	
	public static LineShape getHoverLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_HOVERELEMENT);
		
		//set style
		ShapeStyle style = Constants.SHAPESTYLE_HOVER_LINE;
		hoverLine.setStyle(style);
		
		
		return hoverLine;
	}
	
	public static LineShape getPrimarySelectedLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_LINK_PRIMARY);
		
		//set style
		ShapeStyle style = new ShapeStyle(Color.RED, Color.RED, 4f, DrawMode.FILL);
		hoverLine.setStyle(style);
		
		hoverLine.setOffsetX(10);
		hoverLine.setOffsetY(10);
		
		hoverLine.setArrow(true);
		
		
		return hoverLine;
	}
	
	public static LineShape getSecondarySelectedLineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		//create circle shape, set id
		LineShape hoverLine = new LineShape(shapeRendererId, c0, c1);
		hoverLine.setId(Constants.ID_LINK_SECONDARY);
		
		//set style
		ShapeStyle style = new ShapeStyle(Color.GREEN, Color.GREEN, 4f, DrawMode.FILL);
		hoverLine.setStyle(style);
		
		hoverLine.setOffsetX(-10);
		hoverLine.setOffsetY(-10);
		
		hoverLine.setArrow(true);
		
		return hoverLine;
	}
	
	public static BoxShape getBusStopShape(String linkID, int shapeRendererId, Point2D pos)
	{
		
		BoxShape busStop = new BoxShape(shapeRendererId, pos);
		busStop.setId(Constants.ID_BUSSTOP_PREFIX + linkID);
		busStop.setImage(Constants.IMG_BUSSTOP);
		busStop.setFixedSize(80,40);
		busStop.setOffset(0,-40);
		
		//set style
		ShapeStyle style = new ShapeStyle(Color.ORANGE, Color.ORANGE, 4f, DrawMode.IMAGE_FILL);
		busStop.setStyle(style);
		
		return busStop;
	}
	
}
