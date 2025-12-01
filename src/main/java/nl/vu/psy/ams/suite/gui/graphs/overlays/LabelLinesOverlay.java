package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Overlay that shows anchor lines for labels, used for
 * easier positioning of labels in the label tab. Use
 * drawSmallLines=true for the top graph, and false for
 * the lower graphs.
 */
public class LabelLinesOverlay extends Overlay {

	private AmsLabelSet		lSet;
	private LabelOverlay	lov;
	private boolean			drawSmallLines	= true;

	public LabelLinesOverlay(Graph graph, AmsLabelSet lSet, LabelOverlay lov) {
		super(graph);
		this.lSet = lSet;
		this.lov = lov;
		lov.addConnectedGraph(graph);
	}

	@Override
	public void draw(Graphics2D g) {
		g.setColor(Color.BLACK);
		float[] dash = {5, 5};
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, dash, 0));
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int w = graph.getWidth();
		int h = graph.getHeight();
		if (drawSmallLines) {
			for (AmsLabel l : lSet.getLabels()) {
				if (l.equals(lov.getMovingLabel()) == false) {
					int lPos = Utils.getPixelCoordinate(l.getLeftTime(), lTime, rTime, w);
					int rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
					int yPos = h / 4;

					g.drawLine(lPos, 0, lPos, yPos);
					g.drawLine(rPos, 0, rPos, yPos);
				}
			}
		}
		if (lov.getMovingLabel() != null) {
			int lPos = Utils.getPixelCoordinate(lov.getMovingLabel().getLeftTime(), lTime, rTime, w);
			int rPos = Utils.getPixelCoordinate(lov.getMovingLabel().getRightTime(), lTime, rTime, w);
			g.drawLine(lPos, 0, lPos, h);
			g.drawLine(rPos, 0, rPos, h);
		}
		g.setStroke(new BasicStroke(2f));
		g.setColor(Color.RED);
		if (drawSmallLines) {
			for (AmsLabel l : lSet.getLabels()) {
				for (AmsLabel l2 : lSet.getLabels()) {
					if (l.equals(l2) == false) {
						if (l.getLeftTime() > l2.getLeftTime() && l.getLeftTime() < l2.getRightTime()) {
							int lPos = Utils.getPixelCoordinate(l.getLeftTime(), lTime, rTime, w);
							int rPos = 0;
							if (l2.getRightTime() < l.getRightTime()) {
								rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
							} else {
								rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
							}
							int yPos = h / 4;

							g.drawLine(lPos, 0, lPos, yPos);
							g.drawLine(rPos, 0, rPos, yPos);
						}
					}
				}
			}
		}
		if (lov.getMovingLabel() != null) {
			AmsLabel l = lov.getMovingLabel();
			for (AmsLabel l2 : lSet.getLabels()) {
				if (l.equals(l2) == false) {
					if (l.getLeftTime() > l2.getLeftTime() && l.getLeftTime() < l2.getRightTime()) {
						int lPos = Utils.getPixelCoordinate(l.getLeftTime(), lTime, rTime, w);
						int rPos = 0;
						if (l2.getRightTime() < l.getRightTime()) {
							rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
							g.drawLine(lPos, 0, lPos, h);
							if (drawSmallLines)
								g.drawLine(rPos, 0, rPos, h / 4);
						} else {
							rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
							g.drawLine(lPos, 0, lPos, h);
							g.drawLine(rPos, 0, rPos, h);
						}
					}
					if (l2.getLeftTime() > l.getLeftTime() && l2.getLeftTime() < l.getRightTime()) {
						int lPos = Utils.getPixelCoordinate(l2.getLeftTime(), lTime, rTime, w);
						int rPos = 0;
						if (l2.getRightTime() < l.getRightTime()) {
							rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
							if (drawSmallLines) {
								g.drawLine(lPos, 0, lPos, h / 4);
								g.drawLine(rPos, 0, rPos, h / 4);
							}
						} else {
							rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
							if (drawSmallLines)
								g.drawLine(lPos, 0, lPos, h / 4);
							g.drawLine(rPos, 0, rPos, h);
						}
					}
				}
			}
		}
		g.setStroke(new BasicStroke());
	}

	public void enableSmallLines(boolean enable) {
		drawSmallLines = enable;
	}

}
