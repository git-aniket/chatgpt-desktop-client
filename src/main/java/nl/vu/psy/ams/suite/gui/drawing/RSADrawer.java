package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draws RSA for RSA scoring. Each point gets a different color and
 * shape, to indicate whether it is rejected or not.
 * Could be a lot faster by using AffineTransform (see IbiDrawer for instance)
 */
public class RSADrawer extends DataDrawer {

	private boolean drawArtefacts = false;
	private boolean drawDots = true;

	public RSADrawer(String name, YAxis yAxis) {
		super(name, yAxis);
	}

	public RSADrawer(String colName) {
		this(colName, new YAxis(0, 1000.0, 0, 1000.0, colName));
	}

	@Override
	public void drawData(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
		g.setColor(Color.BLACK);
		ArrayList<RespirationCycle> cycs = new ArrayList<RespirationCycle>(rSet.getCycles());
		for (int i = 1; i < cycs.size(); i++) {
			if (cycs.get(i - 1).isExpEndSet() == false)
				continue;
			double lPos = cycs.get(i - 1).getExpStart();
			double rPos = cycs.get(i).getExpStart();
			if (rPos < lTime)
				continue;
			if (lPos > rTime)
				break;
			Double lVal = cycs.get(i - 1).getRSA();
			Double rVal = cycs.get(i).getRSA();
			double zlVal = 0;
			if (lVal != null) {
				if (lVal > 0)
					zlVal = lVal;
			}
			double zrVal = 0;
			if (rVal != null) {
				if (rVal > 0)
					zrVal = rVal;
			}
			int lPix = Utils.getPixelCoordinate(lPos, lTime, rTime, w);
			int rPix = Utils.getPixelCoordinate(rPos, lTime, rTime, w);
			int lPixVal = Utils.getPixelCoordinate(zlVal, bVal, tVal, h);
			int rPixVal = Utils.getPixelCoordinate(zrVal, bVal, tVal, h);
			if (drawArtefacts || (cycs.get(i).isArtefact() == false && cycs.get(i - 1).isArtefact() == false))
				g.drawLine(lPix, h - lPixVal, rPix, h - rPixVal);

		}
		if (drawDots) {
			for (int i = 0; i < cycs.size(); i++) {
				if (cycs.get(i).isArtefact() == true)
					continue;
				double rPos = cycs.get(i).getExpStart();
				if (rPos < lTime)
					continue;
				if (rPos > rTime)
					break;
				Double rVal = cycs.get(i).getRSA();
				double zrVal = 0;
				if (rVal != null) {
					if (rVal > 0)
						zrVal = rVal;
				}
				int rPix = Utils.getPixelCoordinate(rPos, lTime, rTime, w);
				int rPixVal = Utils.getPixelCoordinate(zrVal, bVal, tVal, h);
				if (cycs.get(i).isClippingDZ()) {
					g.setColor(Color.PINK);
					g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
					g.setColor(Color.BLACK);
					g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
				} else if (cycs.get(i).isIrregularRR()) {
					g.setColor(Color.BLUE);
					g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
					g.setColor(Color.BLACK);
					g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
					// } else if (cycs.get(i).isIrregularIbi()) {
					// g.setColor(Color.ORANGE);
					// g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
					// g.setColor(Color.BLACK);
					// g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
				} else {
					if (rVal != null) {
						if (rVal > 0) {
							g.fillRect(rPix - 2, h - rPixVal - 2, 5, 5);
						} else {
							g.setColor(Color.RED);
							g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
							g.setColor(Color.BLACK);
							g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
						}
					} else {
						double[] shortest = cycs.get(i).getShortestIBI();
						double[] longest = cycs.get(i).getLongestIBI(shortest);
						if (longest == null) {
							if (shortest == null) {
								g.setColor(Color.YELLOW);
								g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
								g.setColor(Color.BLACK);
								g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
							} else {
								g.setColor(Color.MAGENTA);
								g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
								g.setColor(Color.BLACK);
								g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
							}
						} else {
							if (shortest == null) {
								g.setColor(Color.GREEN);
								g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
								g.setColor(Color.BLACK);
								g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
							} else {
								g.setColor(Color.WHITE);
								g.fillArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
								g.setColor(Color.BLACK);
								g.drawArc(rPix - 2, h - rPixVal - 2, 5, 5, 0, 360);
							}
						}
					}
				}
			}
		} else {
			this.getYAxis().autoScale();
		}
	}

	@Override
	public double[] getBounds() {
		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
		ArrayList<RespirationCycle> cycs = new ArrayList<RespirationCycle>(rSet.getCycles());
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double ret[] = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < cycs.size(); i++) {
			double rPos = cycs.get(i).getExpStart();
			if (rPos < lTime)
				continue;
			if (rPos > rTime)
				break;
			Double rVal = cycs.get(i).getRSA();
			double rzVal = 0;
			if (rVal != null) {
				if (rVal > 0)
					rzVal = rVal;
			}
			if (drawArtefacts || cycs.get(i).isArtefact() == false) {
				if (rzVal < ret[0])
					ret[0] = rzVal;
				if (rzVal > ret[1])
					ret[1] = rzVal;
			}

		}
		return ret;
	}

	public void setDrawArtefacts(boolean drawArtefacts2) {
		drawArtefacts = drawArtefacts2;
		graph.repaint();
	}

	public void setDrawDots(boolean drawDots2) {
		drawDots = drawDots2;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
		ArrayList<RespirationCycle> cycs = new ArrayList<RespirationCycle>(rSet.getCycles());
		for (int i = 0; i < cycs.size(); i++) {
			double rPos = cycs.get(i).getExpStart();
			if (rPos < lTime)
				continue;
			if (rPos > rTime)
				break;
			Double rVal = cycs.get(i).getRSA();
			if (rVal != null) {
				if (drawArtefacts || cycs.get(i).isArtefact() == false) {
					av += rVal;
					nvals++;
				}
			}
		}
		return av / nvals;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double realVal;
		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
		ArrayList<RespirationCycle> cycs = new ArrayList<RespirationCycle>(rSet.getCycles());
		for (int i = 0; i < cycs.size(); i++) {
			double rPos = cycs.get(i).getExpStart();
			if (rPos < lTime)
				continue;
			if (rPos > rTime)
				break;
			Double rVal = cycs.get(i).getRSA();
			if (rVal != null) {
				if (drawArtefacts || cycs.get(i).isArtefact() == false) {
					realVal = rVal;
					tmpM = m;
					m += (realVal - tmpM) / k;
					s += (realVal - tmpM) * (realVal - m);
					k++;
				}
			}
		}
		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}
}
