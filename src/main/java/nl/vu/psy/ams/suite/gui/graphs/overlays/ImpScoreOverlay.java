package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabel.SavedICGPoint;
import nl.vu.psy.ams.suite.data.structures.SignalPart;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpInfoPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Show and edit impedance score lines to score PEP, X-point, etc.
 */
public class ImpScoreOverlay extends Overlay {

	private SignalPartDrawer id;
	private int dragType = 0;
	private ImpInfoPanel iip;
	private boolean showSavedScores = false;
	private boolean showpoints = true;
	private boolean isDirty = false;

	public ImpScoreOverlay(Graph graph) {
		super(graph, true);
	}

	@Override
	public void draw(Graphics2D g) {

		Font font = new Font("MM", Font.BOLD, 12);
		g.setFont(font);
		String filtname = "";
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

		double botVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double topVal = graph.getDrawers().get(0).getYAxis().getTopValue();

		SignalPart selPart = id.getSelPart();

		if (selPart == null || selPart.getCalculating() || selPart.getValues() == null)
			return;

		int w = graph.getWidth();
		int h = graph.getHeight();
		int xPos;
		int yPos;

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

			// if (bTime == Double.NEGATIVE_INFINITY || cTime == Double.NEGATIVE_INFINITY ||
			// xTime == Double.NEGATIVE_INFINITY) {
			// selPart.getLabel().resetICGScoring();
			// bTime = selPart.getbPoint();
			// cTime = selPart.getcPoint();
			// xTime = selPart.getxPoint();
			// iip.updateLabelTexts();
			// }

			double cVal = selPart.getcVal();

			if (cVal == Double.NEGATIVE_INFINITY) {
				cVal = selPart.getValueAt(cTime);
				selPart.setcVal(cVal);
				iip.updateLabelTexts();
			}

			double bVal = selPart.getbVal();
			if (bVal == Double.NEGATIVE_INFINITY) {
				bVal = selPart.getValueAt(bTime);
				selPart.setbVal(bVal);
				iip.updateLabelTexts();
			}

			double xVal = selPart.getxVal();
			if (xVal == Double.NEGATIVE_INFINITY) {
				xVal = selPart.getValueAt(xTime);
				selPart.setxVal(xVal);
				iip.updateLabelTexts();
			}

			// ------------- Quick Check to check if the signal is recorded below 1000 Hz
			// and update the correct values accordingly--------------------
			if (cVal > 3) {
				cVal = selPart.getValueAt(cTime);
				selPart.setcVal(cVal);
				bVal = selPart.getValueAt(bTime);
				selPart.setbVal(bVal);
				xVal = selPart.getValueAt(xTime);
				selPart.setxVal(xVal);
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
			g.drawLine(xPos, 0, xPos, h - 5);
			g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos - 5, h - 5);
			yPos = Utils.getPixelCoordinate(bVal, botVal, topVal, h);
			g.drawLine(5, h - yPos, w, h - yPos);
			g.drawLine(5, h - yPos - 5, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos - 5);

			if (AppSettings.getInstance().getIntProperty(Settings.FILTERDZDTNew) == 1) {
				filtname = "DZDT Filter Enabled";
				g.drawString(filtname, w - (filtname.length() + 200), h - (h - 20));
			}

			if (showpoints == true) {
				g.setColor(Color.BLUE);
				g.drawString("B", xPos - 5, h - yPos - 5);
			}

			if (selPart.getBlocations() != null) {
				g.setColor(Color.RED);
				for (int i = 0; i < selPart.getBlocations().length; i++) {
					bTime = selPart.getBlocations()[i];
					bVal = selPart.getBvalues()[i];
					xPos = Utils.getPixelCoordinate(bTime, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(bVal, botVal, topVal, h);

					g.drawLine(xPos - 2, h - yPos - 2, xPos + 2, h - yPos + 2);
					g.drawLine(xPos - 2, h - yPos + 2, xPos + 5, h - yPos - 2);
					g.drawOval(xPos - 2, h - yPos - 2, 4, 4);
				}
			}
			g.setColor(new Color(0, 100, 0));
			xPos = Utils.getPixelCoordinate(cTime, leftTime, rightTime, w);
			g.drawLine(xPos, 0, xPos, h - 5);
			g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos - 5, h - 5);
			yPos = Utils.getPixelCoordinate(cVal, botVal, topVal, h);
			g.drawLine(5, h - yPos, w, h - yPos);
			g.drawLine(5, h - yPos - 5, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos - 5);

			if (showpoints == true) {
				g.setColor(Color.BLUE);
				g.drawString("C", xPos - 5, h - yPos - 5);
			}

			if (selPart.getClocations() != null) {
				g.setColor(new Color(0, 100, 0));
				for (int i = 0; i < selPart.getClocations().length; i++) {
					bTime = selPart.getClocations()[i];
					bVal = selPart.getCvalues()[i];
					xPos = Utils.getPixelCoordinate(bTime, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(bVal, botVal, topVal, h);

					g.drawLine(xPos - 2, h - yPos - 2, xPos + 2, h - yPos + 2);
					g.drawLine(xPos - 2, h - yPos + 2, xPos + 5, h - yPos - 2);
					g.drawOval(xPos - 2, h - yPos - 2, 4, 4);
				}
			}
			g.setColor(Color.BLUE);
			xPos = Utils.getPixelCoordinate(xTime, leftTime, rightTime, w);
			g.drawLine(xPos, 0, xPos, h - 5);
			g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos + 5, h - 5);
			g.drawLine(xPos, h, xPos - 5, h - 5);
			yPos = Utils.getPixelCoordinate(xVal, botVal, topVal, h);
			g.drawLine(5, h - yPos, w, h - yPos);
			g.drawLine(5, h - yPos - 5, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos + 5);
			g.drawLine(0, h - yPos, 5, h - yPos - 5);

			if (showpoints == true) {
				g.setColor(Color.BLUE);
				g.drawString("X", xPos - 5, h - yPos - 5);
			}

			if (selPart.getXlocations() != null) {
				for (int i = 0; i < selPart.getXlocations().length; i++) {
					bTime = selPart.getXlocations()[i];
					bVal = selPart.getXvalues()[i];
					xPos = Utils.getPixelCoordinate(bTime, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(bVal, botVal, topVal, h);

					g.drawLine(xPos - 2, h - yPos - 2, xPos + 2, h - yPos + 2);
					g.drawLine(xPos - 2, h - yPos + 2, xPos + 5, h - yPos - 2);
					g.drawOval(xPos - 2, h - yPos - 2, 4, 4);
				}
			}
		}

		if (showSavedScores) {
			AmsLabel l = selPart.getLabel();
			if (l != null) {
				int maxStringWidth = Integer.MIN_VALUE;
				FontMetrics fm = g.getFontMetrics();
				int nSaved = l.getNumberOfSavedICGPoints();
				for (int i = 0; i < nSaved; i++) {
					g.setColor(Utils.getDarkColorFromInteger((float) (0.7 * i / (nSaved - 1))));

					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

					SavedICGPoint sip = l.getSavedIGGPoint(i);
					String name = sip.getName();
					int txtWidth = fm.stringWidth(name);
					if (txtWidth > maxStringWidth)
						maxStringWidth = txtWidth;

					if (sip.isMissing())
						continue;

					double x = sip.getbICGp();
					double y = sip.getbICGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, botVal, topVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getcICGp();
					y = sip.getcICGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, botVal, topVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getxICGp();
					y = sip.getxICGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, botVal, topVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				}
				g.setColor(Color.WHITE);
				g.fillRect(w - 20 - (60 + maxStringWidth), 20, 60 + maxStringWidth, 20 * nSaved);

				g.setColor(Color.BLACK);
				g.drawRect(w - 20 - (60 + maxStringWidth), 20, 60 + maxStringWidth, 20 * nSaved);

				int txtH = fm.getMaxAscent();
				for (int i = 0; i < nSaved; i++) {
					SavedICGPoint sip = l.getSavedIGGPoint(i);
					g.setColor(Utils.getDarkColorFromInteger((float) (0.7 * i / (nSaved - 1))));
					xPos = w - 20 - (60 + maxStringWidth) + 10;
					yPos = 30 + i * 20;
					g.drawLine(xPos, yPos - 5, xPos + 10, yPos + 5);
					g.drawLine(xPos, yPos + 5, xPos + 10, yPos - 5);
					g.setColor(Color.BLACK);
					String name = sip.getName();
					g.drawString(name, w - 20 - (60 + maxStringWidth) + 50, 30 + i * 20 + txtH / 2);
				}
			}

		}
	}

	public void flipShowSavedScoring() {
		if (showSavedScores) {
			showSavedScores = false;
		} else {
			showSavedScores = true;
		}
		graph.repaint();
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

	@Override
	public void mouseDragged(MouseEvent arg0) {
		double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
		double val = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY());
		SignalPart selPart = id.getSelPart();
		if (selPart.isICGMissing())
			return;
		double QTime = selPart.getECGQPoint();
		double bTime = selPart.getbPoint();
		double cTime = selPart.getcPoint();
		double xTime = selPart.getxPoint();

		if (dragType == 1) {
			if (time < QTime)
				time = QTime;
			if (time > cTime)
				time = cTime;
			selPart.setbPoint(time);
		} else if (dragType == 2) {
			if (time < bTime)
				time = bTime;
			if (time > xTime)
				time = xTime;
			selPart.setcPoint(time);
		} else if (dragType == 3) {
			if (time < cTime)
				time = cTime;
			selPart.setxPoint(time);
		} else if (dragType == 4) {
			selPart.setcVal(val);
		} else if (dragType == 5) {
			selPart.setbVal(val);
		} else if (dragType == 6) {
			selPart.setxVal(val);
		}
		if (dragType > 0) {
			MainFrame.getInstance().getMainFrame().repaint();
			iip.updateLabelTexts();
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {

		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double lMTime = graph.getxAxis().getTimeFromPixel(arg0.getX() - snapSize);
		double rMTime = graph.getxAxis().getTimeFromPixel(arg0.getX() + snapSize);
		SignalPart selPart = id.getSelPart();
		if (selPart == null || selPart.isICGMissing())
			return;
		double bTime = selPart.getbPoint();
		double cTime = selPart.getcPoint();
		double xTime = selPart.getxPoint();

		double rMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() - snapSize);
		double lMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() + snapSize);
		double cVal = selPart.getcVal();
		double bVal = selPart.getbVal();
		double xVal = selPart.getxVal();

		if (bTime > lMTime && bTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (cTime > lMTime && cTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (xTime > lMTime && xTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (cVal > lMVal && cVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.N_RESIZE_CURSOR));
		} else if (bVal > lMVal && bVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.N_RESIZE_CURSOR));
		} else if (xVal > lMVal && xVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.N_RESIZE_CURSOR));
		} else {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		}

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double lMTime = graph.getxAxis().getTimeFromPixel(arg0.getX() - snapSize);
		double rMTime = graph.getxAxis().getTimeFromPixel(arg0.getX() + snapSize);
		SignalPart selPart = id.getSelPart();
		if (selPart.isICGMissing())
			return;
		double bTime = selPart.getbPoint();
		double cTime = selPart.getcPoint();
		double xTime = selPart.getxPoint();

		double rMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() - snapSize);
		double lMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() + snapSize);
		double cVal = selPart.getcVal();
		double bVal = selPart.getbVal();
		double xVal = selPart.getxVal();

		if (bTime > lMTime && bTime < rMTime) {
			dragType = 1;
		} else if (cTime > lMTime && cTime < rMTime) {
			dragType = 2;
		} else if (xTime > lMTime && xTime < rMTime) {
			dragType = 3;
		} else if (cVal > lMVal && cVal < rMVal) {
			dragType = 4;
		} else if (bVal > lMVal && bVal < rMVal) {
			dragType = 5;
		} else if (xVal > lMVal && xVal < rMVal) {
			dragType = 6;
		} else {
			dragType = 0;
		}
	}

	public void setImpDrawer(SignalPartDrawer id) {
		this.id = id;
	}

	public void setImpInfoPanel(ImpInfoPanel iip) {
		this.iip = iip;
	}

	public void recalcX(double time) {
		SignalPart selPart = id.getSelPart();
		double sampleTimeInUS = selPart.getSampleTimeInUS();
		double[] values = selPart.getValues();
		double RRInterval = 60 / (selPart.getLabel().getAverage(true));
		selPart.setxPoint(time + 30);
		// -------------------------X Point------------------------------------
		int nPointsIn50ms = (int) (50000 / sampleTimeInUS);
		int nPointsBeforeT = (int) ((selPart.getECGTPoint() + 256000) / sampleTimeInUS);
		int searchwindow_for_X = (int) (RRInterval * 500000);
		int searchstart_for_X = nPointsBeforeT;

		for (int j = searchstart_for_X; j < searchwindow_for_X; j++) { // From CPoint+50ms to 2048-50 ms
			double cV = values[j];
			if (cV <= values[j - 1] && cV <= values[j + 1]) { // If current value is lesser than previous and next value
				double maxVal = cV;
				boolean accept = true;
				for (int q = j - nPointsIn50ms; q < j + nPointsIn50ms; q++) { // From currentvalue-50 ms to
																				// currentvalue+50 ms
					if (values[q] < maxVal) { // If value is lesser than currentvalue(max value), then it is the lowest
												// point
						accept = false;
						break;
					}
				}
				if (accept) {
					selPart.setxPoint(-256000 + j * sampleTimeInUS);
					break;
				}
			}
		}
		MainFrame.getInstance().getMainFrame().repaint();
		iip.updateLabelTexts();

	}

	public void showorhidenames() {
		if (showpoints) {
			showpoints = false;
		} else {
			showpoints = true;
		}
		graph.repaint();
	}

	public void setDirty(boolean dirty) {
		this.isDirty = dirty;
	}

	public boolean isDirty() {
		return isDirty;
	}
}
