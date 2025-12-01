package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.IBIValue;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.SubSetIbiSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * Draws IBIs in a graph, as a signal. If a lot of
 * ibis have to be drawn, it only draws a subset of these
 * ibis using SubSetIbiSet.
 * 
 * (Searching for bar on bottom that shows the entire
 * recording IBI signal? >> TimeBar.java)
 */
public class IbiDrawer extends DataDrawer {

	private ArrayList<ECGBeat> beatsInView = new ArrayList<ECGBeat>();
	private BeatSet beatSet = CurrentOpenData.getInstance().getBeatSet();

	private int maxDispPoints = AppSettings.getInstance().getIntProperty(Settings.MAXDISPLAYPOINTS);

	private int changeCount = 0;
	private double prevLTime = Double.NaN, prevRTime = Double.NaN;

	private SubSetIbiSet ssSet = CurrentOpenData.getInstance().getIbiSubSet(0);
	private static double defaultBottom = 0;
	private static double defaultTop = 1000.0;

	public IbiDrawer(YAxis Yaxis) {
		super("IBI", Yaxis);
		beatSet = CurrentOpenData.getInstance().getBeatSet();
		ssSet = CurrentOpenData.getInstance().getIbiSubSet(CurrentOpenData.getInstance().getECGChannel());
		getYAxis().setBottomValue(defaultBottom);
		getYAxis().setTopValue(defaultTop);
	}

	public IbiDrawer() {
		this(new YAxis(defaultBottom, defaultTop, defaultBottom, defaultTop, "IBI (msec)"));
	}

	public IbiDrawer(int i, YAxis Yaxis) {
		super("IBI", Yaxis);
		getYAxis().setBottomValue(defaultBottom);
		getYAxis().setTopValue(defaultTop);
		beatSet = CurrentOpenData.getInstance().getBeatSet(i);
		ssSet = CurrentOpenData.getInstance().getIbiSubSet(i);
	}

	public IbiDrawer(int i) {
		this(i, new YAxis(defaultBottom, defaultTop, defaultBottom, defaultTop, "IBI (msec)"));
		this.getYAxis().setBottomValue(0);
		this.getYAxis().setTopValue(1500);
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		if (lTime > rTime)
			return;
		setBeatsInView();
		if (beatsInView.size() < 2)
			return;
		if (beatsInView.size() > maxDispPoints) {
			drawSubSet(g);
			return;
		}
		double lBeat, rBeat, value;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, beatsInView.size());

		AffineTransform at = new AffineTransform();
		at.scale(graph.getWidth() / (rTime - lTime),
				-graph.getHeight() / (1000 * (yAxis.getTopValue() - yAxis.getBottomValue())));
		at.translate(-lTime, -1000 * yAxis.getTopValue());
		g.setColor(Color.BLACK);

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
				value = (rBeat - lBeat);
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

	private void drawSubSet(Graphics2D g) {
		ssSet.reCalculateSubSet(false);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		AffineTransform at = new AffineTransform();
		at.scale(graph.getWidth() / (rTime - lTime),
				-graph.getHeight() / (1000 * (yAxis.getTopValue() - yAxis.getBottomValue())));
		at.translate(-lTime, -1000 * yAxis.getTopValue());
		TreeSet<Double> sections = ssSet.getSections();
		TreeSet<IBIValue> ibiValues = ssSet.getIBIValues();
		SortedSet<Double> sectInView = sections.subSet(lTime, rTime + 1);
		double curTime = lTime;
		for (Double d : sectInView) {
			ArrayList<IBIValue> vals = new ArrayList<IBIValue>(
					ibiValues.subSet(new IBIValue(curTime, 0), true, new IBIValue(d, 0), false));
			if (vals.size() < 2)
				continue;
			Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, vals.size());
			path.moveTo(vals.get(0).getTime(), vals.get(0).getValue());
			for (int i = 1; i < vals.size(); i++)
				path.lineTo(vals.get(i).getTime(), vals.get(i).getValue());
			path.transform(at);
			g.draw(path);
			curTime = d;
		}
		ArrayList<IBIValue> vals = new ArrayList<IBIValue>(
				ibiValues.subSet(new IBIValue(curTime, 0), true, new IBIValue(rTime, 0), false));
		if (vals.size() < 2)
			return;
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, vals.size());
		path.moveTo(vals.get(0).getTime(), vals.get(0).getValue());
		for (int i = 1; i < vals.size(); i++)
			path.lineTo(vals.get(i).getTime(), vals.get(i).getValue());
		path.transform(at);
		g.draw(path);
	}

	@Override
	public double[] getBounds() {
		setBeatsInView();
		double[] bounds = new double[2];
		bounds[0] = Double.POSITIVE_INFINITY;
		bounds[1] = Double.NEGATIVE_INFINITY;
		if (beatsInView.size() < 2)
			return null;
		if (beatsInView.size() > maxDispPoints) {
			double lTime = graph.getxAxis().getLeftTime();
			double rTime = graph.getxAxis().getRightTime();
			double value;
			TreeSet<IBIValue> ibiValues = ssSet.getIBIValues();
			ArrayList<IBIValue> vals = new ArrayList<IBIValue>(
					ibiValues.subSet(new IBIValue(lTime, 0), true, new IBIValue(rTime, 0), true));
			for (IBIValue v : vals) {
				value = v.getValue() / 1000;
				if (value < bounds[0])
					bounds[0] = value;
				if (value > bounds[1])
					bounds[1] = value;
			}
			return bounds;
		}

		double lBeat, rBeat, value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			rBeat = beatsInView.get(i + 1).getRPeakTime();
			value = (rBeat - lBeat) / 1000;
			if (beatsInView.get(i + 1).isFirstInSeries() == false) {
				if (value < bounds[0])
					bounds[0] = value;
				if (value > bounds[1])
					bounds[1] = value;
			}
		}
		return bounds;
	}

	private void setBeatsInView() {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		setBeatsInView(lTime, rTime);
	}

	private void setBeatsInView(double lTime, double rTime) {
		if (lTime != prevLTime || rTime != prevRTime
				|| changeCount != beatSet.getChangeCount() || beatsInView.size() == 0) {
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
			prevLTime = lTime;
			prevRTime = rTime;
			changeCount = beatSet.getChangeCount();
		}
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		setBeatsInView();
		double lBeat, rBeat, value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			rBeat = beatsInView.get(i + 1).getRPeakTime();
			value = (rBeat - lBeat) / 1000;
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
		setBeatsInView();
		double lBeat, rBeat, value;
		for (int i = 0; i < beatsInView.size() - 1; i++) {
			lBeat = beatsInView.get(i).getRPeakTime();
			rBeat = beatsInView.get(i + 1).getRPeakTime();
			value = (rBeat - lBeat) / 1000;
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
