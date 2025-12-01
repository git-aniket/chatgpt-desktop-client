package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.TotalMotilityGenerator;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Draws the total motility, calculated by
 * TotalMotilityGenerator, in a graph. Used in the
 * label tab
 */
public class TotalMotilityDrawer extends DataDrawer {

	double[]		values;
	private int		nValues;
	private long	startTime	= CurrentOpenData.getInstance().getStartTimeInUS();
	private boolean	isEE1;
	private double	width;

	public TotalMotilityDrawer(String name, YAxis yAxis, boolean isEE1) {
		super(name, yAxis);
		this.isEE1 = isEE1;

		resetValues();
		if (values == null)
			return;
		nValues = values.length;
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		if (values != null) {
			double lTime = graph.getxAxis().getLeftTime();
			double rTime = graph.getxAxis().getRightTime();
			double bVal = yAxis.getBottomValue();
			double tVal = yAxis.getTopValue();
			int w = graph.getWidth();
			int h = graph.getHeight();
			int li = (int) ((lTime - startTime) / width);
			if (li < 1)
				li = 1;
			for (int i = li; i < nValues; i++) {
				double rightTime = startTime + (i + 0.5) * width;
				double leftTime = startTime + (i - 0.5) * width;
				if (leftTime > rTime)
					return;
				int lXPos = Utils.getPixelCoordinate(leftTime, lTime, rTime, w);
				int rXPos = Utils.getPixelCoordinate(rightTime, lTime, rTime, w);
				int lYVal = Utils.getPixelCoordinate(values[i - 1], bVal, tVal, h);
				int rYVal = Utils.getPixelCoordinate(values[i], bVal, tVal, h);
				g.drawLine(lXPos, h - lYVal, rXPos, h - rYVal);
			}
		} else {

		}
	}

	@Override
	public double[] getBounds() {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < nValues; i++) {
			double time = startTime + (i + 0.5) * width;
			if (time > lTime && time < rTime) {
				if (values[i] < ret[0])
					ret[0] = values[i];
				if (values[i] > ret[1])
					ret[1] = values[i];
			}
		}
		return ret;
	}

	public void resetValues() {
		if (isEE1) {
			values = TotalMotilityGenerator.getTotalMotility();
			width = 1000000.;
		} else {
			values = TotalMotilityGenerator.getEE2Data();
			width = 60000000.;
		}
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		for (int i = 1; i < nValues; i++) {
			double pointTime = startTime + (i + 0.5) * width;
			if (pointTime > lTime && pointTime < rTime) {
				double val =values[i];
				av += val;
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
		for (int i = 1; i < nValues; i++) {
			double pointTime = startTime + (i + 0.5) * width;
			if (pointTime > lTime && pointTime < rTime) {
				double val = values[i];
				realVal	 = val;
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
