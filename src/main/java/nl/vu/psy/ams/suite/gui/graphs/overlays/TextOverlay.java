package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import nl.vu.psy.ams.suite.gui.graphs.Graph;
/*
 * Shows a text in the middle of the graph.
 */
public class TextOverlay extends Overlay {

	private String	text;

	public TextOverlay(Graph graph, String text) {
		super(graph);
		this.text = text;
	}

	@Override
	public void draw(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("Arial", Font.BOLD, 14));
		g.setColor(Color.BLUE);
		FontMetrics metrics = g.getFontMetrics();
		Rectangle2D bounds = metrics.getStringBounds(text, g);
		g.drawString(text, (float) (graph.getWidth() - bounds.getWidth()) / 2, (float) (bounds.getHeight()));
	}

}
