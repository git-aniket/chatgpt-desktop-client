package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

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
 * Shows the number of suspicious beats as an overlay. Used in QRS
 * tab.
 */
public class ECGScoreOverlay extends Overlay {

	private SignalPartDrawer id;
	private int dragType;
	private ImpInfoPanel iip;
	private ImpScoreOverlay iso;
	private boolean showSavedScores = false;
	private boolean showpoints = true;
	private boolean isDirty = false;

	public ECGScoreOverlay(Graph graph) {
		super(graph, true);
	}

	@Override
	public void draw(Graphics2D g) {

		Font font = new Font("MM", Font.BOLD, 12);
		g.setFont(font);

		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();

		double bVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double tVal = graph.getDrawers().get(0).getYAxis().getTopValue();

		SignalPart selPart = id.getSelPart();

		if (selPart == null || selPart.getCalculating() || selPart.getValues() == null)
			return;

		int w = graph.getWidth();
		int h = graph.getHeight();
		int xPos;
		int yPos;

		if (selPart.isECGMissing() == false) {
			if (AppSettings.getInstance().getIntProperty(Settings.FILTERECGNew) == 1) {
				String filtname = "ECG Filter Enabled";
				g.drawString(filtname, w - (filtname.length() + 200), h - (h - 20));
			}
			double avheartrate = id.getSelPart().getLabel().getAverage(true);
			if (isDirty) {
				selPart.calcECGPoints(avheartrate, false);
				isDirty = false;
			}

			double PTime = selPart.getECGPPoint(); // Q - Onset Time
			double PonsetTime = selPart.getECGPOnsetPoint();// Q Time
			double QTime = selPart.getECGQPoint(); // Q - Onset Time
			double QonsetTime = selPart.getECGQOnsetPoint();// Q Time
			double RTime = selPart.getECGRPoint();
			double STime = selPart.getECGSPoint();
			double SOffsetTime = selPart.getECGSOffsetPoint();
			double ttime = selPart.getECGTPoint();
			double TOffsetTime = selPart.getECGTOffsetPoint();

			double RVal = selPart.getECGRVal();
			double PPointVal = selPart.getECGPOnsetVal();
			double PVal = selPart.getECGPVal();
			double QPointVal = selPart.getECGQOnsetVal();
			double QVal = selPart.getECGQVal();
			double SVal = selPart.getECGSVal();
			double SOffsetVal = selPart.getECGSOffsetVal();
			double TVal = selPart.getECGTVal();
			double TOffsetVal = selPart.getECGTOffsetVal();

			if (RTime == Double.NEGATIVE_INFINITY || QTime == Double.NEGATIVE_INFINITY
					|| QonsetTime == Double.NEGATIVE_INFINITY || STime == Double.NEGATIVE_INFINITY
					|| SOffsetTime == Double.NEGATIVE_INFINITY || ttime == Double.NEGATIVE_INFINITY
					|| TOffsetTime == Double.NEGATIVE_INFINITY) {

				selPart.calcECGPoints(avheartrate, false);

				PTime = selPart.getECGPPoint();
				PonsetTime = selPart.getECGPOnsetPoint();
				QTime = selPart.getECGQPoint();
				QonsetTime = selPart.getECGQOnsetPoint();
				RTime = selPart.getECGRPoint();
				STime = selPart.getECGSPoint();
				SOffsetTime = selPart.getECGSOffsetPoint();
				ttime = selPart.getECGTPoint();
				TOffsetTime = selPart.getECGTOffsetPoint();
			}

			RVal = selPart.getECGRVal();
			PPointVal = selPart.getECGPOnsetVal();
			PVal = selPart.getECGPVal();
			QPointVal = selPart.getECGQOnsetVal();
			QVal = selPart.getECGQVal();
			SVal = selPart.getECGSVal();
			SOffsetVal = selPart.getECGSOffsetVal();
			TVal = selPart.getECGTVal();
			TOffsetVal = selPart.getECGTOffsetVal();

			if (RVal == Double.NEGATIVE_INFINITY) {
				RVal = selPart.getValueAt(RTime);
				selPart.setECGRVal(RVal);
			}
			if ((PPointVal == Double.NEGATIVE_INFINITY)) {
				PPointVal = selPart.getValueAt(PonsetTime);
				selPart.setECGPonsetVal(PPointVal);
			}
			if ((PVal == Double.NEGATIVE_INFINITY)) {
				PVal = selPart.getValueAt(PTime);
				selPart.setECGPVal(PVal);

			}
			if ((QPointVal == Double.NEGATIVE_INFINITY)) {
				QPointVal = selPart.getValueAt(QonsetTime);
				selPart.setECGQonsetVal(QPointVal);
			}
			if ((QVal == Double.NEGATIVE_INFINITY)) {
				QVal = selPart.getValueAt(QTime);
				selPart.setECGQVal(QVal);
			}
			if (SVal == Double.NEGATIVE_INFINITY) {
				SVal = selPart.getValueAt(STime);
				selPart.setECGSVal(SVal);
			}
			if (SOffsetVal == Double.NEGATIVE_INFINITY) {
				SOffsetVal = selPart.getValueAt(SOffsetTime);
				selPart.setECGSOffsetVal(SOffsetVal);
			}
			if (TVal == Double.NEGATIVE_INFINITY) {
				TVal = selPart.getValueAt(ttime);
				selPart.setECGTVal(TVal);
			}
			if (TOffsetVal == Double.NEGATIVE_INFINITY) {
				TOffsetVal = selPart.getValueAt(TOffsetTime);
				selPart.setECGTOffsetVal(TOffsetVal);
			}
			iip.updateLabelTexts();
			// --------P Point---------
			if (PonsetTime != Double.NEGATIVE_INFINITY) {

				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(PonsetTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(PPointVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("P", xPos - 8, h - yPos - 5);
				}
			}

			// --------P Onset ---------
			if (PTime != Double.NEGATIVE_INFINITY) {

				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(PTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(PVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("P-Onset", xPos - 30, h - yPos + 15);
				}
			}

			// --------Q Onset---------
			if ((selPart.isECGQOnsetMissing() == false) && (QTime != Double.NEGATIVE_INFINITY)) {

				g.setColor(Color.RED);
				xPos = Utils.getPixelCoordinate(QTime, leftTime, rightTime, w);
				g.drawLine(xPos, 0, xPos, h - 5);
				g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
				g.drawLine(xPos, h, xPos + 5, h - 5);
				g.drawLine(xPos, h, xPos - 5, h - 5);
				yPos = Utils.getPixelCoordinate(QVal, bVal, tVal, h);
				g.drawLine(5, h - yPos, w, h - yPos);
				g.drawLine(5, h - yPos - 5, 5, h - yPos + 5);
				g.drawLine(0, h - yPos, 5, h - yPos + 5);
				g.drawLine(0, h - yPos, 5, h - yPos - 5);

				if (showpoints == true) {
					g.setColor(Color.RED);
					g.drawString("Q-Onset", xPos - 30, h - yPos + 15);
				}
			}

			// --------R Onset---------
			if ((selPart.isECGQPointMissing() == false) && (QonsetTime != Double.NEGATIVE_INFINITY)) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(QonsetTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(QPointVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("Q", xPos - 5, h - yPos - 5);
				}

			}

			// ----- R Point------
			if (RTime != Double.NEGATIVE_INFINITY) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(RTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(RVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("R", xPos - 5, h - yPos - 5);
				}
			}
			// ----- S Point------
			if (STime != Double.NEGATIVE_INFINITY) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(STime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(SVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("S", xPos - 5, h - yPos - 5);
				}
			}
			// ----- SOffset Point------
			if (SOffsetTime != Double.NEGATIVE_INFINITY) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(SOffsetTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(SOffsetVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("S-Offset", xPos - 5, h - yPos - 5);
				}
			}

			// ----- T Point------
			if (ttime != Double.NEGATIVE_INFINITY) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(ttime, leftTime, rightTime, w);
				g.drawLine(xPos, 0, xPos, h - 5);
				g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
				g.drawLine(xPos, h, xPos + 5, h - 5);
				g.drawLine(xPos, h, xPos - 5, h - 5);

				yPos = Utils.getPixelCoordinate(TVal, bVal, tVal, h);
				g.drawLine(5, h - yPos, w, h - yPos);
				g.drawLine(5, h - yPos - 5, 5, h - yPos + 5);
				g.drawLine(0, h - yPos, 5, h - yPos + 5);
				g.drawLine(0, h - yPos, 5, h - yPos - 5);

				if (showpoints == true) {
					g.setColor(Color.RED);
					g.drawString("T", xPos - 8, h - yPos - 5);
				}
			}
			// ----- TOffset Point------
			if (TOffsetTime != Double.NEGATIVE_INFINITY) {
				g.setColor(Color.BLUE);
				xPos = Utils.getPixelCoordinate(TOffsetTime, leftTime, rightTime, w);
				yPos = Utils.getPixelCoordinate(TOffsetVal, bVal, tVal, h);
				g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);
				g.drawOval(xPos - 5, h - yPos - 5, 10, 10);

				if (showpoints == true) {
					g.setColor(Color.BLUE);
					g.drawString("T-Offset", xPos - 5, h - yPos - 5);
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
					g.setColor(Utils.getDarkColorFromInteger((float) (0.7 * i / nSaved)));

					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

					SavedICGPoint sip = l.getSavedIGGPoint(i);
					String name = sip.getName();
					int txtWidth = fm.stringWidth(name);
					if (txtWidth > maxStringWidth)
						maxStringWidth = txtWidth;

					if (sip.isMissing())
						continue;

					double x = sip.getpECGp();
					double y = sip.getpECGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getpOnsetp();
					y = sip.getpOnsetECGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getqECGp();
					y = sip.getqECGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getqOnsetp();
					y = sip.getqOnsetECGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getSp();
					y = sip.getSv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.getSOffsetp();
					y = sip.getsOffsetv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.gettECGp();
					y = sip.gettECGv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
					g.drawLine(xPos - 5, h - yPos - 5, xPos + 5, h - yPos + 5);
					g.drawLine(xPos - 5, h - yPos + 5, xPos + 5, h - yPos - 5);

					x = sip.gettOffsetp();
					y = sip.gettOffsetv();
					xPos = Utils.getPixelCoordinate(x, leftTime, rightTime, w);
					yPos = Utils.getPixelCoordinate(y, bVal, tVal, h);
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
					g.setColor(Utils.getDarkColorFromInteger((float) (0.7 * i / nSaved)));
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

	@Override
	public void mouseDragged(MouseEvent arg0) {

		double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
		double val = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY());
		SignalPart selPart = id.getSelPart();
		double pTime = selPart.getECGPPoint(); // P - onset time
		double pOnsetTime = selPart.getECGPOnsetPoint(); // P Time
		double qTime = selPart.getECGQPoint(); // Q - onset time
		double qOnsetTime = selPart.getECGQOnsetPoint(); // Q Time
		double STime = selPart.getECGSPoint();
		double SOffsetTime = selPart.getECGSOffsetPoint();
		double tTime = selPart.getECGTPoint();
		double tOffsetTime = selPart.getECGTOffsetPoint();

		if (selPart.isECGMissing())
			return;
		if (dragType == 13) {
			if (time > pOnsetTime)
				time = pOnsetTime;
			selPart.setECGPPoint(time); // For P - Onset Time
		} else if (dragType == 14) {
			if (time < pTime)
				time = pTime;
			if (time > qTime)
				time = qTime;
			selPart.setECGPOnsetPoint(time); // For P Time
		} else if (dragType == 1) {
			if (time < pOnsetTime)
				time = pOnsetTime;
			if (time > qOnsetTime)
				time = qOnsetTime;
			selPart.setECGQPoint(time); // For Q - Onset Time
		} else if (dragType == 2) {
			if (time < qTime)
				time = qTime;
			if (time > 0) // R Time
				time = 0;
			selPart.setECGQOnsetPoint(time); // For Q Time
		} else if (dragType == 3) {
			if (time < 0) // R Time
				time = 0;
			if (time > SOffsetTime)
				time = SOffsetTime;
			selPart.setECGSPoint(time); // For S Time
		} else if (dragType == 4) {
			if (time < STime)
				time = STime;
			if (time > tTime)
				time = tTime;
			selPart.setECGSOffsetPoint(time); // For S - Offset Time
		} else if (dragType == 5) {
			if (time < SOffsetTime)
				time = SOffsetTime;
			if (time > tOffsetTime)
				time = tOffsetTime;
			selPart.setECGTPoint(time); // For T - Time
			iso.recalcX(time);
		} else if (dragType == 6) {
			if (time < tTime)
				time = tTime;
			selPart.setECGTOffsetPoint(time); // For T - Offset Time
		} else if (dragType == 15) {
			selPart.setECGPVal(val); // For P - Onset Value
		} else if (dragType == 16) {
			selPart.setECGPonsetVal(val); // For P val
		} else if (dragType == 7) {
			selPart.setECGQVal(val); // For Q - Onset Value
		} else if (dragType == 8) {
			selPart.setECGQonsetVal(val); // For Q val
		} else if (dragType == 9) {
			selPart.setECGSVal(val); // For S - Val
		} else if (dragType == 10) {
			selPart.setECGSOffsetVal(val); // For S - Offset Val
		} else if (dragType == 11) { // For T - Val
			selPart.setECGTVal(val);
		} else if (dragType == 12) { // For T - Offset Val
			selPart.setECGTOffsetVal(val);
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

		if (selPart == null || selPart.isECGMissing())
			return;

		double pTime = selPart.getECGPPoint(); // P - onset time
		double pOnsetTime = selPart.getECGPOnsetPoint(); // P Time
		double qTime = selPart.getECGQPoint();
		double qOnsetTime = selPart.getECGQOnsetPoint();
		double STime = selPart.getECGSPoint();
		double SOffsetTime = selPart.getECGSOffsetPoint();
		double tTime = selPart.getECGTPoint();
		double tOffsetTime = selPart.getECGTOffsetPoint();

		double rMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() - 1);
		double lMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() + 1);

		double pVal = selPart.getECGPVal();
		double PPointVal = selPart.getECGPOnsetVal();
		double qVal = selPart.getECGQVal();
		double QPointVal = selPart.getECGQOnsetVal();
		double SVal = selPart.getECGSVal();
		double SOffsetVal = selPart.getECGSOffsetVal();
		double tVal = selPart.getECGTVal();
		double tOffsetVal = selPart.getECGTOffsetVal();

		if (pTime > lMTime && pTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (pOnsetTime > lMTime && pOnsetTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (qTime > lMTime && qTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (qOnsetTime > lMTime && qOnsetTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (STime > lMTime && STime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (SOffsetTime > lMTime && SOffsetTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (tTime > lMTime && tTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (tOffsetTime > lMTime && tOffsetTime < rMTime) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (pVal > lMVal && pVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (PPointVal > lMVal && PPointVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (qVal > lMVal && qVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (QPointVal > lMVal && QPointVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (SVal > lMVal && SVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (SOffsetVal > lMVal && SOffsetVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		} else if (tVal > lMVal && tVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (tOffsetVal > lMVal && tOffsetVal < rMVal) {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
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
		if (selPart == null || selPart.isECGMissing())
			return;

		double pTime = selPart.getECGPPoint(); // P - onset time
		double pOnsetTime = selPart.getECGPOnsetPoint(); // P Time
		double qTime = selPart.getECGQPoint(); // Q - onset time
		double qOnsetTime = selPart.getECGQOnsetPoint(); // Q Time
		double STime = selPart.getECGSPoint();
		double SOffsetTime = selPart.getECGSOffsetPoint();
		double tTime = selPart.getECGTPoint();
		double tOffsetTime = selPart.getECGTOffsetPoint();

		double rMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() - snapSize);
		double lMVal = graph.getDrawers().get(0).getYAxis().getValueFromPixel(arg0.getY() + snapSize);

		double pVal = selPart.getECGPVal();
		double PPointVal = selPart.getECGPOnsetVal();
		double qVal = selPart.getECGQVal();
		double QPointVal = selPart.getECGQOnsetVal();
		double SVal = selPart.getECGSVal();
		double SOffsetVal = selPart.getECGSOffsetVal();
		double tVal = selPart.getECGTVal();
		double tOffsetVal = selPart.getECGTOffsetVal();

		if (pTime > lMTime && pTime < rMTime) {
			dragType = 13;
		} else if (pOnsetTime > lMTime && pOnsetTime < rMTime) {
			dragType = 14;
		} else if (qTime > lMTime && qTime < rMTime) {
			dragType = 1;
		} else if (qOnsetTime > lMTime && qOnsetTime < rMTime) {
			dragType = 2;
		} else if (STime > lMTime && STime < rMTime) {
			dragType = 3;
		} else if (SOffsetTime > lMTime && SOffsetTime < rMTime) {
			dragType = 4;
		} else if (tTime > lMTime && tTime < rMTime) {
			dragType = 5;
		} else if (tOffsetTime > lMTime && tOffsetTime < rMTime) {
			dragType = 6;
		} else if (pVal > lMVal && pVal < rMVal) {
			dragType = 15;
		} else if (PPointVal > lMVal && PPointVal < rMVal) {
			dragType = 16;
		} else if (qVal > lMVal && qVal < rMVal) {
			dragType = 7;
		} else if (QPointVal > lMVal && QPointVal < rMVal) {
			dragType = 8;
		} else if (SVal > lMVal && SVal < rMVal) {
			dragType = 9;
		} else if (SOffsetVal > lMVal && SOffsetVal < rMVal) {
			dragType = 10;
		} else if (tVal > lMVal && tVal < rMVal) {
			dragType = 11;
		} else if (tOffsetVal > lMVal && tOffsetVal < rMVal) {
			dragType = 12;
		} else {
			dragType = 0;
		}
	}

	public void setECGDrawer(SignalPartDrawer ecgD) {
		this.id = ecgD;
	}

	public void setImpInfoPanel(ImpInfoPanel iip) {
		this.iip = iip;
	}

	public void setImpOverlay(ImpScoreOverlay iso) {
		this.iso = iso;
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
