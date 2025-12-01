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
 * Generates a filtered Skin Conductance signal from Measured SCL signal.
 *  All the frequencies above 2 Hz are removed
 *  
 *  [b a] = butter(4, 0.4, 'low');
 *  
 *  Filtering in both forward and reverse directions to avoid shifting of peaks
 *  
 *  To test use filtsignal = filtfilt(b,a,x) instead of filt(b,a,x)
 */
public class FilteredSCLGenerator {

	@SuppressWarnings("resource")
	public static void GenerateFilteredSCL() {

		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File sclFile = new File(tempDir, "SCL.bin");
			File tempSCLFile = new File(tempDir, "SCLtemp.bin");
			File temp2SCLFile = new File(tempDir, "SCLtemp2.bin");
			File fsclFile = new File(tempDir, "FILTSCL.bin");

			if (fsclFile.exists())
				return;
			cod.dirtyFiles.add(fsclFile);
			FileChannel ifC = (new FileInputStream(sclFile)).getChannel();
			FileChannel ofC = (new FileOutputStream(tempSCLFile)).getChannel();

			int size = 1048576;
			ByteBuffer bb = ByteBuffer.allocateDirect(size);
			IntBuffer sb = bb.asIntBuffer();
			ByteBuffer obb = ByteBuffer.allocateDirect(size);
			IntBuffer osb = obb.asIntBuffer();

			double[] backBuffer = new double[4];
			double[] forBuffer = new double[4];

			int[] samp = new int[size / 4];
			int[] outBuf = new int[size / 4];

			boolean firstrun = true;
			long fL = sclFile.length();
			int nRead = 0;
			int nOut;

			double realSlope = 1;
			double realConstant = 0;

			Ams7fsChannelInfo s = null;
			int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("SCL");
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

			if (sampleTimeInUS == 100000) { // Sampling time is 100 ms = 100000 us

				// Low pass filter - Remove all frequencies above 2 Hz
				// [b,a] = butter(4, 0.4, 'low')

				b = new double[] { 0.046582906636444, 0.186331626545775, 0.279497439818662, 0.186331626545775,
						0.046582906636444 };
				a = new double[] { 1.000000000000000, -0.782095198023338, 0.679978526916299, -0.182675697753032,
						0.030118875043169 };
			}

			if (b == null || a == null)
				return;

			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered SCL", null, 0, (int) fL);
			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) i / 2);
				bb.position(0);
				sb.position(0);
				nRead = ifC.read(bb);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				if (firstrun == true) {
					double vl = realConstant + realSlope * samp[0];
					for (int q = 0; q < backBuffer.length; q++)
						backBuffer[q] = vl;
					for (int q = 0; q < forBuffer.length; q++)
						forBuffer[q] = vl;
					firstrun = false;
				}
				for (int q = 0; q < nSRead; q++) {

					double vl = realConstant + realSlope * samp[q];

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

					double newVal = ((val - realConstant) / realSlope);

					int shortVal = (int) newVal;

					if (newVal < Integer.MIN_VALUE) {
						shortVal = Integer.MIN_VALUE;
					} else if (newVal > Integer.MAX_VALUE) {
						shortVal = Integer.MAX_VALUE;
					}

					outBuf[q] = shortVal;
				}
				nOut = nSRead;

				obb.clear();
				osb.clear();
				osb.put(outBuf, 0, nOut);
				obb.limit(4 * nOut);
				ofC.write(obb);
			}
			ofC.close();
			ifC.close();

			long pos = fL;
			ifC = (new FileInputStream(tempSCLFile)).getChannel();
			ofC = (new FileOutputStream(fsclFile)).getChannel();

			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) (fL / 2 + i / 2));
				bb.position(0);
				sb.position(0);
				long newPos = pos - size;
				if (newPos < 0)
					newPos = 0;
				nRead = (int) (pos - newPos);
				pos -= size;
				ifC.read(bb, newPos);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				if (firstrun == true) {
					double vl = realConstant + realSlope * samp[nSRead - 1];
					for (int q = 0; q < backBuffer.length; q++)
						backBuffer[q] = vl;
					for (int q = 0; q < forBuffer.length; q++)
						forBuffer[q] = vl;
					firstrun = false;
				}
				for (int q = 0; q < nSRead; q++) {

					double vl = realConstant + realSlope * samp[nSRead - q - 1];

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
				ofC.write(obb, newPos);
			}

			ofC.close();
			ifC.close();
			tempSCLFile.delete();
			temp2SCLFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filterSCL took (" + timer.getTime() / 1000. + " sec)");
	}
}
