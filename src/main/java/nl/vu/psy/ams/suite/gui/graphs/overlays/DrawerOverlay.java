package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Graphics2D;

import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
/*
 * Overlay that can draw a different DataDrawer.
 * For instance, used in RSA Tab to show filtered DZ and
 * raw DZ in a single graph.
 */
public class DrawerOverlay extends Overlay {

	private DataDrawer	draw;

	public DrawerOverlay(Graph graph, DataDrawer draw, YAxis yAxis) {
		super(graph);
		this.draw = draw;
		draw.setYAxis(yAxis);
		draw.setGraph(graph);
		// draw.connectToGraph(graph);
	}

	@Override
	public void draw(Graphics2D g) {
		draw.setAverage(this.average);
		draw.draw(g);
	}
}
