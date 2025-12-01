package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import nl.vu.psy.ams.suite.data.structures.SignalPart;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpInfoPanel;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Show and edit impedance score lines to score PEP, X-point, etc.
 */
public class ImpScoreOverlayMot extends Overlay {

	private SignalPartDrawer id;
	private ImpInfoPanel iip;
	private boolean isDirty = false;

	public ImpScoreOverlayMot(Graph graph) {
		super(graph, true);
	}

	@Override
	public void draw(Graphics2D g) {

		Font font = new Font("MM", Font.BOLD, 12);
		g.setFont(font);
		BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
		Graphics2D gi = bi.createGraphics();
		gi.setColor(Color.WHITE);
		gi.fillRect(0, 0, 5, 5);
		gi.setColor(Color.yellow);
		gi.drawLine(0, 0, 5, 5);
		gi.drawLine(0, 5, 5, 0);
		TexturePaint boxPaint = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));

		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();

		SignalPart selPart = id.getSelPart();

		if (selPart == null || selPart.getCalculating() || selPart.getValues() == null)
			return;

		int w = graph.getWidth();
		int h = graph.getHeight();
		int xPos;

		g.setColor(Color.RED);

		if (selPart.isICGMissing() == false) {
			double avheartrate = id.getSelPart().getLabel().getAverage(true);
			if (isDirty) {
				selPart.calcICGPoints(avheartrate);
				isDirty = false;
			}

			double QTime = selPart.getECGQPoint();
			double bTime = selPart.getbPoint();
			double cTime = selPart.getcPoint();
			double xTime = selPart.getxPoint();

			if (bTime == Double.NEGATIVE_INFINITY || cTime == Double.NEGATIVE_INFINITY
					|| xTime == Double.NEGATIVE_INFINITY) {
				selPart.calcICGPoints(avheartrate);
				bTime = selPart.getbPoint();
				cTime = selPart.getcPoint();
				xTime = selPart.getxPoint();
				iip.updateLabelTexts();
			}

			// ----------------------------------------------------------------------------------------------------------------------------------------
			double[] minMaxB = getMinMaxB(avheartrate, QTime);
			int BPos1 = Utils.getPixelCoordinate(minMaxB[0], leftTime, rightTime, w);
			int BPos2 = Utils.getPixelCoordinate(minMaxB[1], leftTime, rightTime, w);
			g.setPaint(boxPaint);
			g.fillRect(BPos1, 0, BPos2 - BPos1, h);
			bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
			gi = bi.createGraphics();
			gi.setColor(Color.WHITE);
			gi.fillRect(0, 0, 5, 5);
			gi.setColor(Color.orange);
			gi.drawLine(0, 0, 5, 5);
			gi.drawLine(0, 5, 5, 0);
			boxPaint = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));
			double[] minMaxX = getMinMaxX(avheartrate, bTime);
			int XPos1 = Utils.getPixelCoordinate(minMaxX[0], leftTime, rightTime, w);
			int XPos2 = Utils.getPixelCoordinate(minMaxX[1], leftTime, rightTime, w);
			g.setPaint(boxPaint);
			g.fillRect(XPos1, 0, XPos2 - XPos1, h);

			g.setColor(Color.RED);
			xPos = Utils.getPixelCoordinate(bTime, leftTime, rightTime, w);
			g.drawLine(xPos, 0, xPos, h);

			g.setColor(new Color(0, 100, 0));
			xPos = Utils.getPixelCoordinate(cTime, leftTime, rightTime, w);
			g.drawLine(xPos, 0, xPos, h);

			g.setColor(Color.BLUE);
			xPos = Utils.getPixelCoordinate(xTime, leftTime, rightTime, w);
			g.drawLine(xPos, 0, xPos, h);

		}
	}

	public void setImpDrawer(SignalPartDrawer id) {
		this.id = id;
	}

	public double[] getMinMaxB(double avheartrate, double QTime) {
		double ret[] = new double[2];
		// based on PEP
		ret[0] = ret[1] = QTime;
		if (avheartrate <= 60) {
			ret[0] += 100000;
			ret[1] += 140000;
		} else if (avheartrate <= 80) {
			ret[0] += 90000;
			ret[1] += 130000;
		} else if (avheartrate <= 100) {
			ret[0] += 80000;
			ret[1] += 120000;
		} else if (avheartrate <= 120) {
			ret[0] += 70000;
			ret[1] += 100000;
		} else {
			ret[0] += 35000;
			ret[1] += 80000;
		}
		return ret;
	}
	/*
	 * HR PEP (msec) LVET (msec)
	 * 40-60 100-140 300-450,
	 * 60-80 90-130 250-400
	 * 80-100 80-120 200-350
	 * 100-120 70-100 200-300
	 * 120+ <80 150-250 //Martin en Cor estimate 35 ms as minimum
	 */

	public double[] getMinMaxX(double avheartrate, double lTime) {
		double ret[] = new double[2];
		// based on LVET
		ret[0] = ret[1] = lTime;
		if (avheartrate <= 60) {
			ret[0] += 300000;
			ret[1] += 450000;
		} else if (avheartrate <= 80) {
			ret[0] += 250000;
			ret[1] += 400000;
		} else if (avheartrate <= 100) {
			ret[0] += 200000;
			ret[1] += 350000;
		} else if (avheartrate <= 120) {
			ret[0] += 200000;
			ret[1] += 300000;
		} else {
			ret[0] += 150000;
			ret[1] += 250000;
		}
		return ret;
	}

}
