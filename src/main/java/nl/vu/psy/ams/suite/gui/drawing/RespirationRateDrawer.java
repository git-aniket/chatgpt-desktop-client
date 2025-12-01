package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.CubicSplineInterpolator;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draws respiration rate for RSA scoring.
 * Could be a lot faster by using AffineTransform (see IbiDrawer for instance)
 */
public class RespirationRateDrawer extends DataDrawer {

	RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
	private boolean drawArtefacts = false;
	private boolean drawAv = true;
	private boolean drawRaw = true;
	private Color avColor = new Color(0, 128, 0);
	private boolean isCalculating;
	private Thread thread;

	private double avLength = 10000000.;
	private double[] av;
	private CubicSplineInterpolator csi;

	public RespirationRateDrawer(String name, YAxis yAxis) {
		super(name, yAxis);
		recalcAverage();
	}

	public RespirationRateDrawer(String colName) {
		this(colName, new YAxis(0, 1000.0, 0, 1000.0, colName));
	}

	private void drawAverage(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		if (av == null)
			return;
		g.setColor(avColor);
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2));

		AffineTransform at = new AffineTransform();
		at.scale(graph.getWidth() / (rTime - lTime),
				-graph.getHeight() / (yAxis.getTopValue() - yAxis.getBottomValue()));
		at.translate(-lTime, -yAxis.getTopValue());
		int nPointsInPath = 0;
		double curTime = rSet.getCycles().first().getExpStart() + avLength / 2;
		int n = av.length;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, 10 * n);
		int li = (int) ((graph.getxAxis().getLeftTime() - rSet.getCycles().first().getExpStart()) / avLength) - 1;
		if (li < 0)
			li = 0;
		curTime += li * avLength;
		for (int i = 10 * li; i < 10 * n; i++) {
			if (av[i / 10] != -1) {
				if (nPointsInPath == 0) {
					path.moveTo(curTime, csi.splint(curTime));
				} else {
					path.lineTo(curTime, csi.splint(curTime));
				}
				nPointsInPath++;
			} else {
				if (nPointsInPath > 0) {
					path.transform(at);
					g.draw(path);
					nPointsInPath = 0;
					path = new Path2D.Double(Path2D.WIND_EVEN_ODD, 10 * n - i);
				}
			}
			curTime += avLength / 10;
		}
		if (nPointsInPath > 0) {
			path.transform(at);
			g.draw(path);
		}
		g.setStroke(oldStroke);
	}

	@Override
	public void drawData(Graphics2D g) {
		if (drawRaw) {
			double lTime = graph.getxAxis().getLeftTime();
			double rTime = graph.getxAxis().getRightTime();
			double bVal = yAxis.getBottomValue();
			double tVal = yAxis.getTopValue();
			int w = graph.getWidth();
			int h = graph.getHeight();
			g.setColor(Color.BLACK);
			ArrayList<RespirationCycle> cycs = new ArrayList<RespirationCycle>(rSet.getCycles());
			for (int i = 1; i < cycs.size(); i++) {
				double lPos = cycs.get(i - 1).getExpStart();
				double rPos = cycs.get(i).getExpStart();
				if (rPos < lTime)
					continue;
				if (lPos > rTime)
					break;
				Double lVal = cycs.get(i - 1).getRespirationRate();
				Double rVal = cycs.get(i).getRespirationRate();
				if (lVal != null && rVal != null) {
					int lPix = Utils.getPixelCoordinate(lPos, lTime, rTime, w);
					int rPix = Utils.getPixelCoordinate(rPos, lTime, rTime, w);
					int lPixVal = Utils.getPixelCoordinate(lVal, bVal, tVal, h);
					int rPixVal = Utils.getPixelCoordinate(rVal, bVal, tVal, h);
					if (drawArtefacts || (cycs.get(i).isArtefact() == false && cycs.get(i - 1).isArtefact() == false)) {
						g.drawLine(lPix, h - lPixVal, rPix, h - rPixVal);
					}
				}
			}
			this.getYAxis().autoScale();
		}

		if (drawAv) {
			if (getCalculating() == true) {
				g.setColor(Color.BLACK);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				String calcText = "Calculating...";
				g.setFont(new Font("Arial", Font.BOLD, 20));
				FontMetrics met = g.getFontMetrics();
				int strWidth = met.stringWidth(calcText);
				int strHeight = met.getMaxAscent();
				g.drawString(calcText, (float) (graph.getWidth() / 2. - strWidth / 2.),
						(float) (graph.getHeight() / 2. + strHeight / 2.));
				return;
			}
			drawAverage(g);
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
			Double rVal = cycs.get(i).getRespirationRate();
			if (rVal != null) {
				if (drawArtefacts || cycs.get(i).isArtefact() == false) {
					if (rVal < ret[0])
						ret[0] = rVal;
					if (rVal > ret[1])
						ret[1] = rVal;
				}
			}

		}
		return ret;
	}

	private synchronized boolean getCalculating() {
		return isCalculating;
	}

	public void setDrawArtefacts(boolean drawArtefacts2) {
		drawArtefacts = drawArtefacts2;
		graph.repaint();
	}

	public void recalcAverage() {
		if (rSet.getCycles().isEmpty() || rSet.getCycles().size() == 1)
			return;

		if (thread != null) {
			// thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		setCalculating(true);

		thread = new Thread() {

			@Override
			public void run() {
				double curTime = rSet.getCycles().first().getExpStart();
				double endTime = rSet.getCycles().last().getExpStart();
				int nSamples = (int) Math.ceil((endTime - curTime) / avLength);
				av = new double[nSamples];
				for (int j = 0; j < av.length; j++)
					av[j] = -1;
				int nPointsInPath = 0;
				double curAv = 0;
				double value;
				int i = 0;
				boolean isFirstInSeries = true;
				for (RespirationCycle beat : rSet.getCycles()) {
					double lBeat = beat.getExpStart();
					RespirationCycle rightBeat = rSet.getNextCycle(beat);
					if (rightBeat == null)
						break;
					double rBeat = rightBeat.getExpStart();

					if (lBeat - curTime > avLength) {
						if (nPointsInPath == 0) {
							av[i] = -1;
						} else {
							value = curAv / nPointsInPath;
							av[i] = value;
						}
						curAv = 0;
						int newI = (int) Math.floor((lBeat - curTime) / avLength);
						for (int j = 1; j < newI; j++)
							av[i + j] = -1;
						i += newI;
						curTime += newI * avLength;
						nPointsInPath = 0;
					}
					if (isFirstInSeries == false) {
						value = 60000000. / (rBeat - lBeat);
						curAv += value;
						nPointsInPath++;
					}
					isFirstInSeries = false;
				}
				if (nPointsInPath == 0) {
					av[i] = -1;
				} else {
					value = curAv / nPointsInPath;
					av[i] = value;
					curAv = 0;
				}

				int nPointsInAV = 0;
				for (int j = 0; j < av.length; j++) {
					if (av[j] > 0)
						nPointsInAV++;
				}
				double xAr[] = new double[nPointsInAV];
				double yAr[] = new double[nPointsInAV];
				int index = 0;
				curTime = rSet.getCycles().first().getExpStart() + avLength / 2;
				for (int j = 0; j < av.length; j++) {
					if (av[j] > 0) {
						xAr[index] = curTime;
						yAr[index] = av[j];
						index++;
					}
					curTime += avLength;
				}
				csi = new CubicSplineInterpolator(xAr, yAr);
				setCalculating(false);
			}

		};

		thread.start();

	}

	public void drawAv(boolean drawAv) {
		this.drawAv = drawAv;
	}

	public void drawRaw(boolean drawRaw) {
		this.drawRaw = drawRaw;
	}

	public void setAvColor(Color avColor) {
		this.avColor = avColor;
	}

	private synchronized void setCalculating(boolean calc) {
		isCalculating = calc;
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
			Double rVal = cycs.get(i).getRespirationRate();
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
			Double rVal = cycs.get(i).getRespirationRate();
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
