package nl.vu.psy.ams.suite.data.qrs;

import java.util.List;
import java.util.SortedSet;

import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.structures.ECGBeat;
/*
 * Interface that can detect beats from ECG file.
 * If you want to use a different algorithm, implement
 * this interface in a new class.
 */
public interface QRSDetector {
	public SortedSet<ECGBeat> findBeats(double leftTime, double rightTime);
	public List<Double> findBeats (double[] data, double[] ticks);

	public void setMonitor(ProgressMonitor mon);
}
