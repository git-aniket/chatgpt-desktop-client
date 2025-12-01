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
	private FileChannel ifC;
	private FileChannel ofC;
	ByteBuffer bb;
	IntBuffer sb;
	ByteBuffer obb;
	IntBuffer osb;
	int[] samp;
	int[] outBuf;
	double[] tempBuf;
	double[] backBuffer = new double[4];
	double[] forBuffer = new double[4];
	double[] bBackBuffer = new double[4];
	double[] bForBuffer = new double[4];
	double realSlope = 1;
	double realConstant = 0;

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

			double[] b = null;
			double[] a = null;

			// cut-off frequency 5 Hz
			// if (sampleTimeInUS == 1000) { // Sampling time is 1 ms = 1000 us, 1000 Hz
			// a = new double[] { 1, -1.9555778328194147, 0.9565432688144089 };
			// b = new double[] { 0.00024135899874854145, 0.0004827179974970829,
			// 0.00024135899874854145 };
			// }
			// if (sampleTimeInUS == 20000) { // Sampling time is 20 ms = 20000 us, 50 Hz
			// a = new double[] { 1, -1.1429772843080923, 0.41279762014290533 };
			// b = new double[] { 0.06745508395870334, 0.13491016791740668,
			// 0.06745508395870334 };
			// }

			// cut-off frequency 20 Hz
			if (sampleTimeInUS == 1000) { // [b,a] = butter(2, 0.04, 'low')
				a = new double[] { 1, -1.822694925196308, 0.837181651256023 };
				b = new double[] { 0.003621681514929, 0.007243363029857, 0.003621681514929 };
			}
			if (sampleTimeInUS == 20000) { // [b,a] = butter(2, 0.8, 'low')
				a = new double[] { 1, 1.142980502539901, 0.412801598096189 };
				b = new double[] { 0.638945525159022, 1.277891050318045, 0.638945525159022 };
			}

			if (b == null || a == null)
				return;

			long fL = inFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered " + channel, null, 0, (int) fL);

			filterForAndBackward(a, b, inFile, filtFile, progress);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(filtFile);
			ssf1.start();
			// Aniket for step detection: [b, a] = butter(2,[0.01/500, 3.5/500], "bandpass")
			backBuffer = new double[4];
			forBuffer = new double[4];
			bBackBuffer = new double[4];
			bForBuffer = new double[4];
			bb.clear();
			obb.clear();
			sb.clear();
			osb.clear();
			a = new double[] { 1, -3.968988363243609, 5.907441364711835,
					-3.907917597495268, 0.969464596028921 };
			b = new double[] { 1.0e-03 * 0.118372652424925, 0, 1.0e-03 * -0.236745304849850, 0,
					1.0e-03 * 0.118372652424925 };
			filterForAndBackward(a, b, inFile, filtStepFile, progress);

			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
		} catch (IOException e) {
			e.printStackTrace();
		}

		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filter " + channel + " took (" + timer.getTime() / 1000. + " sec)");
	}

	void filterForAndBackward(double[] a, double[] b, File inFile, File tempFile, ProgressMonitor progress)
			throws IOException {
		int size = (int) inFile.length();
		bb = ByteBuffer.allocateDirect(size);
		sb = bb.asIntBuffer();
		obb = ByteBuffer.allocateDirect(size);
		osb = obb.asIntBuffer();

		samp = new int[size / 4];
		outBuf = new int[size / 4];
		tempBuf = new double[size / 4];
		FileInputStream fis = new FileInputStream(inFile);
		FileOutputStream fos = new FileOutputStream(tempFile);
		ifC = fis.getChannel();
		ofC = fos.getChannel();

		int nRead = size;
		int nOut;

		bb.position(0);
		sb.position(0);
		nRead = ifC.read(bb);
		int nSRead = nRead / 4;
		if (nRead < 1) {
			ofC.close();
			ifC.close();
			fis.close();
			fos.close();
			return;
		}
		sb.get(samp, 0, nSRead);
		double vl = realConstant + realSlope * samp[0];
		for (int q = 0; q < backBuffer.length; q++)
			backBuffer[q] = vl;
		for (int q = 0; q < forBuffer.length; q++)
			forBuffer[q] = vl;
		int progressIncrement = nSRead / 10;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				progress.setProgress((int) q / 2);
			}
			vl = realConstant + realSlope * samp[q];
			double val = vl * b[0];
			for (int z = 0; z < b.length - 1; z++)
				val += backBuffer[z] * b[z + 1];
			for (int z = 0; z < a.length - 1; z++)
				val -= forBuffer[z] * a[z + 1];
			for (int z = 0; z < backBuffer.length - 1; z++)
				backBuffer[3 - z] = backBuffer[2 - z];
			for (int z = 0; z < forBuffer.length - 1; z++)
				forBuffer[3 - z] = forBuffer[2 - z];
			backBuffer[0] = vl;
			forBuffer[0] = val;

			tempBuf[q] = val;
		}

		vl = tempBuf[nSRead - 1];
		for (int q = 0; q < bBackBuffer.length; q++)
			bBackBuffer[q] = vl;
		for (int q = 0; q < bForBuffer.length; q++)
			bForBuffer[q] = vl;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				progress.setProgress(size / 2 + (int) q / 2);
			}
			vl = tempBuf[nSRead - q - 1];
			double val = vl * b[b.length - 1];
			for (int z = 0; z < b.length - 1; z++)
				val += bBackBuffer[z] * b[z + 1];
			for (int z = 0; z < a.length - 1; z++)
				val -= bForBuffer[z] * a[z + 1];
			for (int z = 0; z < bBackBuffer.length - 1; z++)
				bBackBuffer[3 - z] = bBackBuffer[2 - z];
			for (int z = 0; z < bForBuffer.length - 1; z++)
				bForBuffer[3 - z] = bForBuffer[2 - z];
			bBackBuffer[0] = vl;
			bForBuffer[0] = val;
			double newVal = ((val - realConstant) / realSlope);
			int shortVal = (int) newVal;

			if (newVal < Integer.MIN_VALUE) {
				shortVal = Integer.MIN_VALUE;
			} else if (newVal > Integer.MAX_VALUE) {
				shortVal = Integer.MAX_VALUE;
			}

			outBuf[nSRead - q - 1] = shortVal;
		}
		nOut = nSRead;

		obb.clear();
		osb.clear();
		osb.put(outBuf, 0, nOut);
		obb.limit(4 * nOut);
		ofC.write(obb);

		ofC.close();
		ifC.close();
		fis.close();
		fos.close();
	}
}
