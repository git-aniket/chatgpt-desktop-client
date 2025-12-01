package nl.vu.psy.ams.suite.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nl.vu.psy.ams.suite.tools.Utils;
/*
 * File loader and average calculator for externally loaded file
 */
public class ExternalFile3Data {

	private static String	name;
	private static double[]	xData;
	private static double[]	yData;

	private static boolean	isCalculated	= false;
	public static void calculate() {
		if (isCalculated)
			return;
		isCalculated = true;
		xData = null;
		yData = null;
		File fl = new File(CurrentOpenData.getInstance().getFilePath(), "extfile3.dat");
		CurrentOpenData cod = CurrentOpenData.getInstance();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fl));
			DateFormat df = new SimpleDateFormat("dd-MM-yy/HH:mm:ss.SSS");
			name = reader.readLine();
			double startTime = cod.getStartTimeInUS();
			String dateline = reader.readLine();
		// Add this line	
			//read subject
			reader.readLine();
			//-----------
			Date begDate = Utils.parseDate(df, dateline);
			if (begDate != null) {
				startTime = Utils.getTimeFromDate(begDate);
			} else {
				df = new SimpleDateFormat("dd-MM-yy/HH:mm:ss");
				begDate = Utils.parseDate(df, dateline);
				if (begDate != null)
					startTime = Utils.getTimeFromDate(begDate);
			}
			String line = reader.readLine();
			ArrayList<Double> xAr = new ArrayList<Double>();
			ArrayList<Double> yAr = new ArrayList<Double>();
			while (line != null) {
				String[] vals = line.split("\\s+");
				xAr.add(Double.parseDouble(vals[0]));
				yAr.add(Double.parseDouble(vals[1]));
				line = reader.readLine();
			}
			xData = new double[xAr.size()];
			yData = new double[yAr.size()];
			for (int i = 0; i < xAr.size(); i++) {
				xData[i] = startTime + 1000. * xAr.get(i);
			}
			for (int i = 0; i < yAr.size(); i++) {
				yData[i] = yAr.get(i);
			}
			// graph.autoScale();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}
	public static void clear() {
		isCalculated = false;
		xData = null;
		yData = null;
	}
	public static Double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		double totalTime = 0;
		boolean atLeastOneInAv = false;

		if (xData == null || yData == null)
			return null;

		for (int i = 1; i < xData.length; i++) {
			if (xData[i - 1] > lTime && xData[i] < rTime) {
				atLeastOneInAv = true;
				double avVal = (yData[i] + yData[i - 1]) / 2;
				av += avVal * (xData[i] - xData[i - 1]);
				totalTime += (xData[i] - xData[i - 1]);
			}
		}

		for (int i = 1; i < xData.length; i++) {
			if (xData[i - 1] < lTime && xData[i] > lTime) {
				atLeastOneInAv = true;
				double pos = lTime;
				double slope = (yData[i] - yData[i - 1]) / (xData[i] - xData[i - 1]);
				double intercept = yData[i - 1] - slope * xData[i - 1];
				double valAtPos = intercept + slope * pos;
				double avVal = (valAtPos + yData[i]) / 2.;
				av += avVal * (xData[i] - lTime);
				totalTime += (xData[i] - lTime);
			}
		}

		for (int i = 1; i < xData.length; i++) {
			if (xData[i - 1] < rTime && xData[i] > rTime) {
				atLeastOneInAv = true;
				double pos = rTime;
				double slope = (yData[i] - yData[i - 1]) / (xData[i] - xData[i - 1]);
				double intercept = yData[i - 1] - slope * xData[i - 1];
				double valAtPos = intercept + slope * pos;
				double avVal = (yData[i - 1] + valAtPos) / 2.;
				av += avVal * (rTime - xData[i - 1]);
				totalTime += (rTime - xData[i - 1]);
			}
		}

		for (int i = 1; i < xData.length; i++) {
			if (xData[i - 1] < lTime && xData[i] > rTime) {
				atLeastOneInAv = true;
				double lpos = lTime;
				double rpos = rTime;
				double slope = (yData[i] - yData[i - 1]) / (xData[i] - xData[i - 1]);
				double intercept = yData[i - 1] - slope * xData[i - 1];
				double valAtLPos = intercept + slope * lpos;
				double valAtRPos = intercept + slope * rpos;
				double avVal = (valAtRPos + valAtLPos) / 2.;
				av += avVal * (rTime - lTime);
				totalTime += (rTime - lTime);
			}
		}

		if (atLeastOneInAv == false)
			return null;
		return av / totalTime;

	}

	public static String getName() {
		calculate();
		return name;
	}
	public static double[] getXData() {
		calculate();
		return xData;
	}

	public static double[] getYData() {
		calculate();
		return yData;
	}
}
