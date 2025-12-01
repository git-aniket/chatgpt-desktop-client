package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputListener;

import nl.vu.psy.ams.suite.gui.graphs.Graph;

/*
 * Base overlay class. Extends this to create a new overlay
 */
public abstract class Overlay implements MouseInputListener {

	protected Graph graph;
	protected double average;

	public Overlay(Graph graph) {
		this.graph = graph;
	}

	public Overlay(Graph graph, boolean mouseOverlay) {
		this(graph);
		if (mouseOverlay) {
			graph.addMouseListener(this);
			graph.addMouseMotionListener(this);
		}
	}

	public abstract void draw(Graphics2D g);

	public String getToolTipText(MouseEvent e) {
		return null;
	}

	public void setAverage(double av) {
		this.average = av;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}
