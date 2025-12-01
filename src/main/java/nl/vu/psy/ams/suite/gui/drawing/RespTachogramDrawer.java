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
 * Similar to RespirationRateDrawer, but draws the breaths in tachogram mode.
 * Used in RSA scoring.
 */
public class RespTachogramDrawer extends DataDrawer {

	private RespirationSet	rSet	= CurrentOpenData.getInstance().getRespSet();

	public RespTachogramDrawer(String name, YAxis yAxis) {
		super(name, yAxis);
	}

	@Override
	public void drawData(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();
		ArrayList<RespirationCycle> bts = new ArrayList<RespirationCycle>(rSet.getSubset(lTime, rTime));
		RespirationCycle beforeBeat = rSet.getCycleBeforeTime(lTime);
		RespirationCycle afterBeat = rSet.getCycleAfterTime(rTime);
		int w = graph.getWidth();
		int h = graph.getHeight();
		g.setColor(Color.GRAY);
		if (bts.isEmpty())
			return;
		int lPos, rPos, val;
		double prevYVal = -1;
		int prevYPix = 0;
		double yVal;
		if (beforeBeat != null) {
			if (bts.get(0).isFirstInSeries() == false) {
				lPos = Utils.getPixelCoordinate(beforeBeat.getInspStart(), lTime, rTime, w);
				rPos = Utils.getPixelCoordinate(bts.get(0).getInspStart(), lTime, rTime, w);
				yVal = (bts.get(0).getInspStart() - beforeBeat.getInspStart()) / 1000000;
				val = Utils.getPixelCoordinate(yVal, bVal, tVal, h);
				prevYVal = yVal;
				prevYPix = val;
				g.drawLine(lPos, h - val, rPos, h - val);
			}
		}
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries()) {
				prevYVal = -1;
				prevYPix = -1;
				continue;
			}
			lPos = Utils.getPixelCoordinate(bts.get(i - 1).getInspStart(), lTime, rTime, w);
			rPos = Utils.getPixelCoordinate(bts.get(i).getInspStart(), lTime, rTime, w);
			yVal =  (bts.get(i).getInspStart() - bts.get(i - 1).getInspStart()) / 1000000;
			val = Utils.getPixelCoordinate(yVal, bVal, tVal, h);
			g.drawLine(lPos, h - val, rPos, h - val);
			if (prevYVal > 0) {
				g.drawLine(lPos, h - prevYPix, lPos, h - val);
			}
			prevYVal = yVal;
			prevYPix = val;
		}
		if (afterBeat != null && afterBeat.isFirstInSeries() == false) {
			lPos = Utils.getPixelCoordinate(bts.get(bts.size() - 1).getInspStart(), lTime, rTime, w);
			rPos = Utils.getPixelCoordinate(afterBeat.getInspStart(), lTime, rTime, w);
			yVal =  (afterBeat.getInspStart() - bts.get(bts.size() - 1).getInspStart()) / 1000000;
			val = Utils.getPixelCoordinate(yVal, bVal, tVal, h);
			g.drawLine(lPos, h - val, rPos, h - val);
			if (prevYVal > 0) {
				g.drawLine(lPos, h - prevYPix, lPos, h - val);
			}
		}
	}

	@Override
	public double[] getBounds() {

		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		ArrayList<RespirationCycle> bts = new ArrayList<RespirationCycle>(rSet.getSubset(lTime, rTime));
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries())
				continue;
			double val =  (bts.get(i).getInspStart() - bts.get(i - 1).getInspStart()) / 1000000;
			if (val < ret[0])
				ret[0] = val;
			if (val > ret[1])
				ret[1] = val;
		}
		return ret;
	}


	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		ArrayList<RespirationCycle> bts = new ArrayList<RespirationCycle>(rSet.getSubset(lTime, rTime));
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries())
				continue;
			double val =  (bts.get(i).getInspStart() - bts.get(i - 1).getInspStart()) / 1000000;
			av += val;
			nvals++;
		}
		return av / nvals;		
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double realVal;
		ArrayList<RespirationCycle> bts = new ArrayList<RespirationCycle>(rSet.getSubset(lTime, rTime));
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries())
				continue;
			double val =  (bts.get(i).getInspStart() - bts.get(i - 1).getInspStart()) / 1000000;
			realVal	 = val;
			tmpM = m;
			m += (realVal - tmpM) / k;
			s += (realVal - tmpM) * (realVal - m);
			k++;
		}
		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}
}
