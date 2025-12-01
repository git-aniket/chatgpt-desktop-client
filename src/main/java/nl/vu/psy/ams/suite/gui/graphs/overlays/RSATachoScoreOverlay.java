package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Show the smallest and longest ibi for each respiration cycle
 * in the HR Tachogram by drawing red and blue lines.
 */
public class RSATachoScoreOverlay extends Overlay {

	public RSATachoScoreOverlay(Graph graph) {
		super(graph);
	}

	@Override
	public void draw(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int w = graph.getWidth();
		int h = graph.getHeight();
		double bVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double tVal = graph.getDrawers().get(0).getYAxis().getTopValue();

		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();

		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		for (RespirationCycle rc : rSet.getCycles()) {
			double[] shortest = rc.getShortestIBI();
			if (shortest != null) {
				int xPos = Utils.getPixelCoordinate(shortest[0], lTime, rTime, w);
				int yPos = Utils.getPixelCoordinate(shortest[1] / 1000, bVal, tVal, h);
				int lPos = Utils.getPixelCoordinate(shortest[0] - shortest[1], lTime, rTime, w);
				g.drawLine(lPos, h - yPos, xPos, h - yPos);
			}
		}
		g.setColor(Color.BLUE);
		for (RespirationCycle rc : rSet.getCycles()) {
			double[] longest = rc.getLongestIBI(rc.getShortestIBI());
			if (longest != null) {
				int xPos = Utils.getPixelCoordinate(longest[0], lTime, rTime, w);
				int yPos = Utils.getPixelCoordinate(longest[1] / 1000, bVal, tVal, h);
				int lPos = Utils.getPixelCoordinate(longest[0] - longest[1], lTime, rTime, w);
				g.drawLine(lPos, h - yPos, xPos, h - yPos);
			}
		}
		g.setStroke(oldStroke);
	}

}
