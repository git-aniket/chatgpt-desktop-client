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
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.CubicSplineInterpolator;

/*
 * Draws the heart rate in a graph. Some dark
 * magic is used to make drawing faster, especially
 * by scaling everything with an affinetransform.
 */
public class HRDrawer extends DataDrawer {

	private ArrayList<ECGBeat> beatsInView = new ArrayList<ECGBeat>();
	private BeatSet beatSet = CurrentOpenData.getInstance().getBeatSet();
	private boolean drawRaw = true;
	private boolean drawAv = true;
	private double[] av;
	private double avLength = 10000000;
	private boolean isCalculating;
	private Thread thread;
	private CubicSplineInterpolator csi;
	private Color avColor = new Color(0, 128, 0);
	private static double defaultBottom = 0;
	private static double defaultTop = 200.0;

	public HRDrawer() {
		super("HR", new YAxis(defaultBottom, defaultTop, defaultBottom, defaultTop, "HR [bpm]"));
		getYAxis().setBottomValue(defaultBottom);
		getYAxis().setTopValue(defaultTop);
		recalcAverage();
	}

	private void drawAverage(Graphics2D g, AffineTransform at) {
		if (av == null)
			return;
		g.setColor(avColor);
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2));

		int nPointsInPath = 0;
		double curTime = beatSet.getBeats().first().getRPeakTime() + avLength / 2;
		int n = av.length;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, 10); // * n);
		int li = (int) ((graph.getxAxis().getLeftTime() - beatSet.getBeats().first().getRPeakTime()) / avLength) - 1;
		if (li < 0)
			li = 0;
		curTime += li * avLength;
		// for (int i = 10 * li; i < 10 * n; i++) {
		// if (av[i / 10] != -1) {
		for (int i = li; i < n; i++) {
			if (av[i] != -1) {
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
					path = new Path2D.Double(Path2D.WIND_EVEN_ODD, n - i);
				}
			}
			curTime += avLength; // / 10;
		}
		if (nPointsInPath > 0) {
			path.transform(at);
			g.draw(path);
		}
		g.setStroke(oldStroke);
	}

	public void drawAverageHR(boolean drawAv) {
		this.drawAv = drawAv;
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();

		resetBeatsInView();

		if (beatsInView.size() < 2)
			return;
		double lBeat, rBeat, value;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, beatsInView.size());

		AffineTransform at = new AffineTransform();
		at.scale(graph.getWidth() / (rTime - lTime),
				-graph.getHeight() / (yAxis.getTopValue() - yAxis.getBottomValue()));
		at.translate(-lTime, -yAxis.getTopValue());
		g.setColor(Color.BLACK);

		if (drawRaw) {
			int nPointsInPath = 0;
			for (int i = 0; i < beatsInView.size() - 1; i++) {
				lBeat = beatsInView.get(i).getRPeakTime();
				rBeat = beatsInView.get(i + 1).getRPeakTime();
				if (beatsInView.get(i + 1).isFirstInSeries()) {
					if (nPointsInPath > 0) {
						path.transform(at);
						g.draw(path);
						nPointsInPath = 0;
						path = new Path2D.Double(Path2D.WIND_EVEN_ODD, beatsInView.size());
					}
				} else {
					value = 60000000. / (rBeat - lBeat);
					if (nPointsInPath == 0) {
						path.moveTo(lBeat, value);
					} else {
						path.lineTo(lBeat, value);
					}
					nPointsInPath++;
				}
			}
			if (nPointsInPath > 0) {
				path.transform(at);
				g.draw(path);
			}
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
			drawAverage(g, at);
		}
	}

	public void drawRawHR(boolean drawRaw) {
		this.drawRaw = drawRaw;
	}

	public Color getAvColor() {
		return avColor;
	}

	public double getAvLength() {
		return avLength;
	}

	@Override
	public double[] getBounds() {
		double[] bounds = new double[2];
		bounds[0] = java.lang.Double.POSITIVE_INFINITY;
		bounds[1] = java.lang.Double.NEGATIVE_INFINITY;
		if (drawRaw) {
			resetBeatsInView();
			double lBeat, rBeat, value;
			for (int i = 0; i < beatsInView.size() - 1; i++) {
				lBeat = beatsInView.get(i).getRPeakTime();
				rBeat = beatsInView.get(i + 1).getRPeakTime();
				value = 60000000. / (rBeat - lBeat);
				if (beatsInView.get(i + 1).isFirstInSeries() == false) {
					if (value < bounds[0])
						bounds[0] = value;
					if (value > bounds[1])
						bounds[1] = value;
				}
			}
			return bounds;
		}
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double curTime = beatSet.getBeats().first().getRPeakTime() + 30000000;
		if (av == null) {
			System.out.println("I'm using null loop in HR Drawer");
			av = new double[60];
		}
		int n = av.length;
		for (int i = 0; i < n; i++) {
			if (curTime > lTime && curTime < rTime) {
				if (av[i] != -1) {
					if (av[i] < bounds[0])
						bounds[0] = av[i];
					if (av[i] > bounds[1])
						bounds[1] = av[i];
				}
			}
			curTime += avLength;
		}
		return bounds;
	}

	private synchronized boolean getCalculating() {
		return isCalculating;
	}

	public void recalcAverage() {
		beatSet = CurrentOpenData.getInstance().getBeatSet();
		if (beatSet.getBeats().isEmpty())
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
				double curTime = beatSet.getBeats().first().getRPeakTime();
				double endTime = beatSet.getBeats().last().getRPeakTime();
				int nSamples = (int) Math.ceil((endTime - curTime) / avLength);
				av = new double[nSamples];
				for (int j = 0; j < av.length; j++)
					av[j] = -1;
				int nPointsInPath = 0;
				double curAv = 0;
				double value;
				int i = 0;
				for (ECGBeat beat : beatSet.getBeats()) {
					double lBeat = beat.getRPeakTime();
					ECGBeat rightBeat = beatSet.getNextBeat(beat);
					if (rightBeat == null)
						break;
					double rBeat = rightBeat.getRPeakTime();

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
					if (rightBeat.isFirstInSeries() == false) {
						value = 60000000. / (rBeat - lBeat);
						curAv += value;
						nPointsInPath++;
					}
				}
				if (nPointsInPath == 0 && i < av.length) {
					av[i] = -1;
				} else if (av.length > 0) {
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
				curTime = beatSet.getBeats().first().getRPeakTime() + avLength / 2;
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

	private void resetBeatsInView() {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		resetBeatsInView(lTime, rTime);
	}

	private void resetBeatsInView(double lTime, double rTime) {
		SortedSet<ECGBeat> beats = beatSet.getBeats();

		ArrayList<ECGBeat> beatsAfterRTime = new ArrayList<ECGBeat>(beats.tailSet(new ECGBeat(rTime)));
		ECGBeat rightMostBeat = new ECGBeat(rTime + 1);
		if (beatsAfterRTime.size() >= 2) {
			rightMostBeat = new ECGBeat(beatsAfterRTime.get(1).getRPeakTime() + 1);
		} else if (beatsAfterRTime.size() == 1) {
			rightMostBeat = new ECGBeat(beatsAfterRTime.get(0).getRPeakTime() + 1);
		}

		SortedSet<ECGBeat> beatsBeforeLTime = beats.headSet(new ECGBeat(lTime));
		ECGBeat leftMostBeat = new ECGBeat(lTime);
		if (beatsBeforeLTime.isEmpty() == false) {
			leftMostBeat = beatsBeforeLTime.last();
		}

		beatsInView = new ArrayList<ECGBeat>(beats.subSet(leftMostBeat, rightMostBeat));
	}

	public void setAvColor(Color avColor) {
		this.avColor = avColor;
	}

	public void setAvLength(double newLength) {
		avLength = newLength;
		recalcAverage();
		if (graph != null)
			graph.repaint();
	}

	private synchronized void setCalculating(boolean calc) {
		isCalculating = calc;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		resetBeatsInView();
		double lBeat, rBeat, value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			rBeat = beatsInView.get(i + 1).getRPeakTime();
			value = 60000000. / (rBeat - lBeat);
			if (beatsInView.get(i + 1).isFirstInSeries() == false) {
				av += value;
				nvals++;
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
		resetBeatsInView();
		double lBeat, rBeat, value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			rBeat = beatsInView.get(i + 1).getRPeakTime();
			value = 60000000. / (rBeat - lBeat);
			if (beatsInView.get(i + 1).isFirstInSeries() == false) {
				realVal = value;
				tmpM = m;
				m += (realVal - tmpM) / k;
				s += (realVal - tmpM) * (realVal - m);
				k++;
			}
		}
		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}
}
