package nl.vu.psy.ams.suite.data.qrs;

import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
/*
 * Uses the second derivative of the ibi signal to
 * set a suspicion level for each beat.
 */
/*
 * 
 * 
 * 
 * Formula - 2nd Derivative
 * 
 * [ (y-ymin)/b]  -[(yplus-y)/a)]
 * -------------------------------
 *           [(a+b)/2]
 * 
 * 
 * 
 * 
 */
public class IBISuspicionFinder {

	public static void setIBISuspicionLevel(SortedSet<ECGBeat> beats) {
		ECGBeat[] prevBeats = new ECGBeat[3];
		for (int i = 0; i < 3; i++)
			prevBeats[i] = null;
		for (ECGBeat beat : beats)
			beat.setIBISuspicion(0.);
		for (ECGBeat beat : beats) {
			if (prevBeats[0] != null && prevBeats[1] != null && prevBeats[2] != null) {
				double a, b, ymin, y, yplus;
				double ibimin = 60000000.0 / AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.MAXHR);
				double ibimax = 60000000.0 / AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.MINHR);
				a = prevBeats[0].getRPeakTime() - prevBeats[1].getRPeakTime();
				b = prevBeats[1].getRPeakTime() - prevBeats[2].getRPeakTime();
				if (a < ibimin || a > ibimax || b < ibimin || b > ibimax) {
					prevBeats[1].setIBISuspicion(1001.0);
				} else {
					a /= 1000000;
					b /= 1000000;
					ymin = prevBeats[1].getRPeakTime() - prevBeats[2].getRPeakTime(); // last beat
					y = prevBeats[0].getRPeakTime() - prevBeats[1].getRPeakTime(); //current beat
					yplus = beat.getRPeakTime() - prevBeats[0].getRPeakTime(); // future beat
					ymin /= 1000;
					y /= 1000;
					yplus /= 1000;
					double d2y = 2 * (a * ymin - (a + b) * y + b * yplus) / (b * a * a + a * b * b);
					prevBeats[1].setIBISuspicion(Math.abs(d2y));
				}
			}
			prevBeats[2] = prevBeats[1];
			prevBeats[1] = prevBeats[0];
			prevBeats[0] = beat;
		}

	}
}
