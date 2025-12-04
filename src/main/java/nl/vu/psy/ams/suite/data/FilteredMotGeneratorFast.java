package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.posture.ZeroPhaseFilter;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Generates a filtered signal from Measured motility signal.
 *  All the frequencies above 5 Hz are removed
 *  
 *  [b a] = butter();
 *  
 *  Filtering in both forward and reverse directions to avoid shifting of peaks
 *  
 *  To test use filtsignal = filtfilt(b,a,x) instead of filt(b,a,x)
 */
public class FilteredMotGeneratorFast {
	double realSlope = 1;
	double realConstant = 0;
	int[] samp;
	int[] outBuf;

	public void GenerateFilteredMot(String channel) {

		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File inFile = new File(tempDir, channel + ".bin");
			File filtFile = new File(tempDir, "FILT" + channel + ".bin");
			File filtStepFile = new File(tempDir, "FILTstep" + channel + ".bin");

			if (filtFile.exists() && filtStepFile.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(filtFile);
				ssf1.start();
				return;
			}

			cod.dirtyFiles.add(filtFile);

			Ams7fsChannelInfo s = null;
			int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID(channel);
				if (s.getRealSlope() == 0) {
					double lowerBound = -Math.pow(2, s.getnBits() - 1);
					double upperBound = Math.pow(2, s.getnBits() - 1) - 1;
					double lowerValue = (double) s.getlMinValue() / s.getlMinMaxDivider();
					double upperValue = (double) s.getlMaxValue() / s.getlMinMaxDivider();
					realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
					realConstant = lowerValue - realSlope * lowerBound;
				} else {
					realSlope = s.getRealSlope();
					realConstant = s.getRealConstant();
				}
				sampleTimeInUS *= s.getDwDivider();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Determine sampling frequency and filter parameters
			int samplingFreqHz = 1000000 / sampleTimeInUS; // Convert from microseconds to Hz
			int filterOrder = 2;
			double lowPassCutoffHz;
			double bandPassLowHz;
			double bandPassHighHz;

			// Set filter parameters based on sampling rate
			if (sampleTimeInUS == 1000) { // 1000 Hz
				lowPassCutoffHz = 20.0; // 20 Hz cutoff
				bandPassLowHz = 0.01; // 0.01 Hz
				bandPassHighHz = 3.5; // 3.5 Hz
			} else if (sampleTimeInUS == 20000) { // 50 Hz
				lowPassCutoffHz = 20.0; // 20 Hz cutoff (same as before)
				bandPassLowHz = 0.01; // 0.01 Hz
				bandPassHighHz = 3.5; // 3.5 Hz
			} else {
				// Default values
				lowPassCutoffHz = 20.0;
				bandPassLowHz = 0.01;
				bandPassHighHz = 3.5;
			}

			long fL = inFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered " + channel, null, 0, (int) fL);

			// Apply low-pass filter using ZeroPhaseFilter to display the filtered
			applyZeroPhaseFilterToFile(inFile, filtFile, samplingFreqHz, filterOrder, lowPassCutoffHz,
					FilterType.LOW_PASS, progress);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(filtFile);
			ssf1.start();

			// Apply bandpass filter for step detection using ZeroPhaseFilter
			applyZeroPhaseFilterToFile(inFile, filtStepFile, samplingFreqHz, filterOrder, bandPassLowHz,
					bandPassHighHz, FilterType.BAND_PASS, progress);

			progress.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filter " + channel + " took (" + timer.getTime() / 1000. + " sec)");
	}

	private enum FilterType {
		LOW_PASS, BAND_PASS
	}

	/**
	 * Apply zero-phase filter to a file using ZeroPhaseFilter from posture package.
	 * Reads binary file, converts to physical units, applies filter, and writes
	 * back.
	 */
	private void applyZeroPhaseFilterToFile(File inFile, File outFile, int samplingFreqHz, int order,
			double cutoffHz, FilterType filterType, ProgressMonitor progress) throws IOException {
		applyZeroPhaseFilterToFile(inFile, outFile, samplingFreqHz, order, cutoffHz, 0, filterType, progress);
	}

	/**
	 * Apply zero-phase filter to a file using ZeroPhaseFilter from posture package.
	 * Supports both low-pass and band-pass filters.
	 */
	private void applyZeroPhaseFilterToFile(File inFile, File outFile, int samplingFreqHz, int order,
			double param1, double param2, FilterType filterType, ProgressMonitor progress) throws IOException {

		int size = (int) inFile.length();
		int nSamples = size / 4;

		// Read input file
		samp = new int[nSamples];
		try (FileInputStream fis = new FileInputStream(inFile);
				FileChannel channel = fis.getChannel()) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(size);
			IntBuffer intBuffer = buffer.asIntBuffer();
			channel.read(buffer);
			intBuffer.get(samp, 0, nSamples);
		}

		if (nSamples < 1) {
			return;
		}

		// Convert to physical units (double)
		double[] signal = new double[nSamples];
		for (int i = 0; i < nSamples; i++) {
			signal[i] = realConstant + realSlope * samp[i];
			if (i % (nSamples / 10 + 1) == 0) {
				progress.setProgress(i / 2);
			}
		}

		// Apply zero-phase filtering using ZeroPhaseFilter from posture package
		double[] filtered;
		if (filterType == FilterType.LOW_PASS) {
			filtered = ZeroPhaseFilter.zeroPhaseLowPassFilterJDSP(
					signal, order, param1, samplingFreqHz);
		} else { // BAND_PASS
			filtered = ZeroPhaseFilter.zeroPhaseBandPassFilterJDSP(
					signal, order, param1, param2, samplingFreqHz);
		}

		// Convert back to integer counts
		outBuf = new int[nSamples];
		for (int i = 0; i < nSamples; i++) {
			double newVal = (filtered[i] - realConstant) / realSlope;
			int intVal = (int) Math.round(newVal);

			if (newVal < Integer.MIN_VALUE) {
				intVal = Integer.MIN_VALUE;
			} else if (newVal > Integer.MAX_VALUE) {
				intVal = Integer.MAX_VALUE;
			}

			outBuf[i] = intVal;
			if (i % (nSamples / 10 + 1) == 0) {
				progress.setProgress(size / 2 + i / 2);
			}
		}

		// Write output file
		try (FileOutputStream fos = new FileOutputStream(outFile);
				FileChannel channel = fos.getChannel()) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(size);
			IntBuffer intBuffer = buffer.asIntBuffer();
			intBuffer.put(outBuf, 0, nSamples);
			buffer.limit(4 * nSamples);
			channel.write(buffer);
		}
	}
}
