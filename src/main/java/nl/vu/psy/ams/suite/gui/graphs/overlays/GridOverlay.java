package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;

import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
/*
 * Show a regular grid (if enabled). Normally used
 * as underlay.
 */
public class GridOverlay extends Overlay {

	public GridOverlay(Graph graph) {
		super(graph);
	}
	
	@Override
	public void draw(Graphics2D g) {
		int gridEnabled = AppSettings.getInstance().getIntProperty(Settings.GRIDENABLED);
		if (gridEnabled == 0)
			return;
//		int gridSize = AppSettings.getInstance().getIntProperty(Settings.GRIDSIZE);
		int w = graph.getWidth();
		int h = graph.getHeight();
		if (graph.getXMajorTicks().size() < 2 || graph.getYMajorTicks().size() < 2)
			return;
			
		Graphics2D g2d = (Graphics2D) g.create();
		// Grid lines @ major ticks:
//		g2d.setColor(new Color(1f, 0.875f, 0.875f, 1.0f));
		g2d.setColor(new Color(0.85f, 0.85f, 0.85f, 1.0f));
		Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
		g2d.setStroke(dashed);
		int XTickSpacing = graph.getXMajorTicks().get(1) - graph.getXMajorTicks().get(0);;
		for (int x : graph.getXMajorTicks()){
			g2d.drawLine(x, 0, x, h);
		}
		
		int YTickSpacing = graph.getYMajorTicks().get(1) - graph.getYMajorTicks().get(0);
		for (int y : graph.getYMajorTicks()){
			g2d.drawLine(0, y, w, y);
		}
		
		// Grid lines @ minor (half) ticks:
		Stroke dotted = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
		g2d.setStroke(dotted);
//		g2d.setColor(new Color(1f, 0.875f, 0.875f, 0.7f));
//		g2d.setColor(new Color(0.75f, 0.75f, 0.75f, 0.7f));
		g2d.drawLine(graph.getXMajorTicks().get(0)-XTickSpacing/2, 0, graph.getXMajorTicks().get(0)-XTickSpacing/2, h);
		for (int x : graph.getXMajorTicks()){
			g2d.drawLine(x+XTickSpacing/2, 0, x+XTickSpacing/2, h);
		}
		
		g2d.drawLine(0, graph.getYMajorTicks().get(0)-YTickSpacing/2, w, graph.getYMajorTicks().get(0)-YTickSpacing/2);
		for (int y : graph.getYMajorTicks()){
			g2d.drawLine(0, y+YTickSpacing/2, w, y+YTickSpacing/2);
		}
		
		g2d.dispose();
	}
}
