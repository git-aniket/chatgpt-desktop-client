package nl.vu.psy.ams.suite.data.freq;

import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.CubicSplineInterpolator;
/*
 * Takes an ibi sequence, and resamples it to a regular grid.
 */
public class BeatSetToGridData {

	private static double	minSampleTime	= 250000; // 0.25 seconds

	public static double getLTime() {
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		return bSet.getBeats().first().getRPeakTime();
	}

	public static double getRTime() {
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		double rTime = bSet.getBeats().last().getRPeakTime();
		rTime = bSet.getBeatBeforeTime(rTime).getRPeakTime();
		return rTime;
	}

	public static double[] getSmoothedGridDataUnpadded(double leftTime, double rightTime) {
		/* Steps
		 * 1. Get Beat Set
		 * 2. Get Beats under the label (4 mins)
		 * 3. Calculate tWidth (tWidth = LastbeatRpeaktime - FirstbeaRPeaktime)
		 * 4. Calculate no of samples after re-sampling into a regular grid of 0.25 sec  (n = tWidth/250000)
		 * 5. Calculate IBI
		 * 6. Construct IBI Series (x - Rpeaktime, y - IBI in sec)
		 * 7. Calculate Mean and SD of the IBI series (y values)
		 * 8. Check for artifact in the IBI series
		 * 9. Construct the artifact free IBI series
		 * 10. Construct a cubic spline interpolation of the artifact free IBI series
		 * 11. Return the interpolated IBI series
		 * 
		*/
		
		BeatSet bSet 					= CurrentOpenData.getInstance().getBeatSet();
		ArrayList<ECGBeat> beatList 	= new ArrayList<ECGBeat>(bSet.subSet(leftTime, rightTime)); // Righttime-lefttime = 4 minutes
		
		if (beatList.size() < 10)
			return null;
		
		double lTime 					= beatList.get(0).getRPeakTime();
		double rTime 					= beatList.get(beatList.size() - 1).getRPeakTime();
		double timeWidth 				= rTime - lTime;

		int nNeeded 					= (int) Math.round(Math.floor(timeWidth / minSampleTime)); // minSampleTime = 0.25sec
		int nSamples 					= nNeeded;

		if (nSamples < 10)
			return null;

		double sTime 					= minSampleTime; // minSampleTime = 0.25sec
		double[] ret 					= new double[nSamples];

		ArrayList<Double> xData 		= new ArrayList<Double>();
		ArrayList<Double> hrData 		= new ArrayList<Double>();

		// Construct IBI Series
		for (int i = 0; i < beatList.size() - 1; i++) {
			if (beatList.get(i + 1).isFirstInSeries() == false) {
				double y 				= beatList.get(i + 1).getRPeakTime() - beatList.get(i).getRPeakTime();
				if (beatList.get(i).isFirstInSeries() == true) {
					xData.add(Double.NEGATIVE_INFINITY);
					hrData.add(y / 1000000.);
				}
				xData.add(beatList.get(i).getRPeakTime());
				hrData.add(y / 1000000.); // Inter-beat Interval in seconds
			}
		}

		int n 							= hrData.size() - 1;
		double m						= 0;
		double s 						= 0;
		double tmpM 					= 0;		
		int k 							= 1;
		
		// Find Mean and standard deviation
		for (int i = 1; i <= n; i++) { 
			double diff 				= hrData.get(i) - hrData.get(i - 1);
			tmpM 						= m;
			m 							+= (diff - tmpM) / k;
			s 							+= (diff - tmpM) * (diff - m);
			k++;
		}

		double stdDev 					= Math.sqrt(s / (k - 2)); // Standard Deviation

		ArrayList<Double> xData2 		= new ArrayList<Double>();
		ArrayList<Double> hrData2 		= new ArrayList<Double>();

		int begI 						= 0;
		
		while (xData.get(begI) 			== Double.NEGATIVE_INFINITY)
			begI++;
		
		xData2.add(xData.get(begI));
		hrData2.add(hrData.get(begI));

		double threshold 				= AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FREQSIGMA) / 1000.; // 3.5 => Artifact removal std value

		for (int i = begI + 1; i <= n; i++) {
			if (xData.get(i) > Double.NEGATIVE_INFINITY) {
				double diff 			= hrData.get(i) - hrData.get(i - 1); 
				if (Math.abs(diff) < threshold * stdDev) { // Artifact Removal 
					xData2.add(xData.get(i));
					hrData2.add(hrData.get(i));
				}
			}
		}

		double xAr[] 					= new double[xData2.size()];
		double yAr[] 					= new double[hrData2.size()];

		for (int i = 0; i < hrData2.size(); i++) {
			xAr[i] 						= xData2.get(i);
			yAr[i] 						= hrData2.get(i);
		}
		
		CubicSplineInterpolator csi 	= new CubicSplineInterpolator(xAr, yAr);
		
		for (int i = 0; i < nSamples; i++) {
			double curTime 				= lTime + i * sTime; //sTime = 0.25 seconds
			ret[i] 						= csi.splint(curTime);
		}

		return ret;
	}
}
