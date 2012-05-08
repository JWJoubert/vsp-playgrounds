/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.sergioo.Visualizer2D.NetworkVisualizer.PublicTransportCapacity;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.sergioo.Visualizer2D.Camera;
import playground.sergioo.Visualizer2D.ImagePainter;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.PublicTransport.PublicTransportNetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.PublicTransportCapacity.PublicTransportNetworkWindow.Option;

public class PublicTransportNetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final PublicTransportNetworkWindow window;
	private int iniX;
	private int iniY;
	private Color backgroundColor = Color.WHITE;
	private boolean withNetwork = true;
	
	//Methods
	public PublicTransportNetworkPanel(PublicTransportNetworkWindow window, NetworkPainter networkPainter) {
		super();
		this.window = window;
		addLayer(new Layer(networkPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public PublicTransportNetworkPanel(PublicTransportNetworkWindow window, NetworkPainter networkPainter, File imageFile, Coord upLeft, Coord downRight) throws IOException {
		super();
		this.window = window;
		ImagePainter imagePainter = new ImagePainter(imageFile, this);
		imagePainter.setImageCoordinates(upLeft, downRight);
		addLayer(new Layer(imagePainter, false));
		addLayer(new Layer(networkPainter), true);
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public Camera getCamera() {
		return camera;
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		if(getNumLayers()<2)
			for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().getNetworkLinks()) {
				if(link!=null) {
					coords.add(link.getFromNode().getCoord());
					coords.add(link.getToNode().getCoord());
				}
			}
		else {
			coords.add(((ImagePainter)getLayer(0).getPainter()).getUpLeft());
			coords.add(((ImagePainter)getLayer(0).getPainter()).getDownRight());
		}
		super.calculateBoundaries(coords);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
		}
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		iniX = e.getX();
		iniY = e.getY();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		camera.move(getWorldX(iniX)-getWorldX(e.getX()),getWorldY(iniY)-getWorldY(e.getY()));
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		window.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		repaint();
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'n':
			withNetwork  = !withNetwork;
			break;
		case 'v':
			viewAll();
			break;
		case 't':
			((PublicTransportNetworkPainter)getPrincipalLayer().getPainter()).setWeight(((PublicTransportNetworkPainter)getPrincipalLayer().getPainter()).getWeight()/1.5f);
			break;
		case 'g':
			((PublicTransportNetworkPainter)getPrincipalLayer().getPainter()).setWeight(((PublicTransportNetworkPainter)getPrincipalLayer().getPainter()).getWeight()*1.5f);
			break;
		case 'i':
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.showSaveDialog(this);
			File file = jFileChooser.getSelectedFile();
			saveImage(file.getName().split("\\.")[file.getName().split("\\.").length-1], file, Integer.parseInt(JOptionPane.showInputDialog("Width", "12040")),  Integer.parseInt(JOptionPane.showInputDialog("Height", "6012")));
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
