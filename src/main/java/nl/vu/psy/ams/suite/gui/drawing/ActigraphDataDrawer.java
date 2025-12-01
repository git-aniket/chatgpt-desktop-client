package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;

import nl.vu.psy.ams.suite.data.ReadActigraphRawData;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Draws an external file
 */
public class ActigraphDataDrawer extends DataDrawer {

	double[]	xData	= null;
	double[]	yData	= null;
	String		name	= null;

	public ActigraphDataDrawer(String name, YAxis yAxis) {	
		super(name, yAxis);			
		ReadActigraphRawData.getXName();
		xData = ReadActigraphRawData.getXData();
		if(name.equals("Actigraph_MXR")){
			
			yData = ReadActigraphRawData.getMXRData();	
		}else if(name.equals("Actigraph_MYR")){
			//xData = ReadActigraphRawData.getXData();
			yData = ReadActigraphRawData.getMYRData();	
		}else{
			//xData = ReadActigraphRawData.getXData();
			yData = ReadActigraphRawData.getMZRData();	
		}
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();

		for (int i = 1; i < xData.length; i++) {
			int xPosL = Utils.getPixelCoordinate(xData[i - 1], lTime, rTime, w);
			int yPosL = Utils.getPixelCoordinate(yData[i - 1], bVal, tVal, h);
			int xPosR = Utils.getPixelCoordinate(xData[i], lTime, rTime, w);
			int yPosR = Utils.getPixelCoordinate(yData[i], bVal, tVal, h);
			g.drawLine(xPosL, h - yPosL, xPosR, h - yPosR);
		}

	}

	@Override
	public double[] getBounds() {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < xData.length; i++) {
			if (xData[i] > lTime && xData[i] < rTime) {
				if (yData[i] < ret[0])
					ret[0] = yData[i];
				if (yData[i] > ret[1])
					ret[1] = yData[i];
			}
		}
		return ret;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}
}
