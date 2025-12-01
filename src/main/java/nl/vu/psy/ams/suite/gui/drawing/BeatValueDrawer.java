package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;

/*
 * Draws the heart rate in a graph. Some dark
 * magic is used to make drawing faster, especially
 * by scaling everything with an affinetransform.
 */
public class BeatValueDrawer extends DataDrawer {

	private ArrayList<ECGBeat> beatsInView = new ArrayList<ECGBeat>();
	private BeatSet beatSet = CurrentOpenData.getInstance().getBeatSet();
	private boolean baseline = true, filtered = false;
	private static double defaultBottom = 0;
	private static double defaultTop = 200.0;

	public BeatValueDrawer(boolean baseline, boolean filtered, Color color) {
		// if (baseline)
		super((baseline ? "ECG base line" : "PeakHelght"), new YAxis(defaultBottom, defaultTop, defaultBottom,
				defaultTop, (baseline ? "ECG base [mV]" : "R amplitude [mV]")), color);
		// else
		// super("Peak Height", new YAxis(defaultBottom, defaultTop, defaultBottom,
		// defaultTop, "R amplitude [mV]"));
		this.baseline = baseline;
		this.filtered = filtered;
		getYAxis().setBottomValue(defaultBottom);
		getYAxis().setTopValue(defaultTop);
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();

		resetBeatsInView();

		if (beatsInView.size() < 2)
			return;
		double lBeat, value;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, beatsInView.size());

		AffineTransform at = new AffineTransform();
		at.scale(graph.getWidth() / (rTime - lTime),
				-graph.getHeight() / (yAxis.getTopValue() - yAxis.getBottomValue()));
		at.translate(-lTime, -yAxis.getTopValue());
		Color oldColor = g.getColor();
		g.setColor(color);

		int nPointsInPath = 0;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			if (beatsInView.get(i + 1).isFirstInSeries()) {
				if (nPointsInPath > 0) {
					path.transform(at);
					g.draw(path);
					nPointsInPath = 0;
					path = new Path2D.Double(Path2D.WIND_EVEN_ODD, beatsInView.size());
				}
			} else {
				if (baseline) {
					if (filtered)
						value = beatsInView.get(i).getBaseHeightF();
					else
						value = beatsInView.get(i).getBaseHeight();
				} else {
					if (filtered)
						value = beatsInView.get(i).getRPeakHeightF();
					else
						value = beatsInView.get(i).getRPeakHeight();
				}
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

		g.setColor(oldColor);
	}

	@Override
	public double[] getBounds() {
		double[] bounds = new double[2];
		bounds[0] = java.lang.Double.POSITIVE_INFINITY;
		bounds[1] = java.lang.Double.NEGATIVE_INFINITY;
		resetBeatsInView();
		double value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			if (baseline) {
				if (filtered)
					value = beatsInView.get(i).getBaseHeightF();
				else
					value = beatsInView.get(i).getBaseHeight();
			} else {
				if (filtered)
					value = beatsInView.get(i).getRPeakHeightF();
				else
					value = beatsInView.get(i).getRPeakHeight();
			}
			if (beatsInView.get(i + 1).isFirstInSeries() == false) {
				if (value < bounds[0])
					bounds[0] = value;
				if (value > bounds[1])
					bounds[1] = value;
			}
		}
		return bounds;
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

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		resetBeatsInView();
		double value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			if (baseline) {
				if (filtered)
					value = beatsInView.get(i).getBaseHeightF();
				else
					value = beatsInView.get(i).getBaseHeight();
			} else {
				if (filtered)
					value = beatsInView.get(i).getRPeakHeightF();
				else
					value = beatsInView.get(i).getRPeakHeight();
			}
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
		double value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			if (baseline) {
				if (filtered)
					value = beatsInView.get(i).getBaseHeightF();
				else
					value = beatsInView.get(i).getBaseHeight();
			} else {
				if (filtered)
					value = beatsInView.get(i).getRPeakHeightF();
				else
					value = beatsInView.get(i).getRPeakHeight();
			}
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
