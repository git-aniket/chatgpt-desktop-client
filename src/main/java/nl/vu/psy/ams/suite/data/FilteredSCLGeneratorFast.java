package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

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
public class FilteredSCLGeneratorFast {
	private FileChannel ifC;
	private FileChannel ofC;
	ByteBuffer bb;
	IntBuffer sb;
	ByteBuffer obb;
	IntBuffer osb;
	DoubleBuffer osd;
	int[] samp;
	double[] sampF;
	int[] outBuf;
	double[] outBufd;
	double[] tempBuf;
	int size = 1048576;
	double[] backBuffer = new double[4];
	double[] forBuffer = new double[4];
	double[] bBackBuffer = new double[4];
	double[] bForBuffer = new double[4];
	int HAMPELWINDOW = 5;
	double[] hampelBuffer = new double[HAMPELWINDOW]; // outlier detection
	double realSlope = 1;
	double realConstant = 0;
	int outliers = 0;
	boolean is7fs;

	public void GenerateFilteredSCL() {

		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			is7fs = (Utils.getExtension(cod.getDataFile()).equals("7fs")
					|| cod.getFileHeader().getDwHardwareVersion() == 7);
			File tempDir = cod.getFilePath();

			File sclFile = new File(tempDir, "SCL.bin");
			File fsclFile;
			if (is7fs)
				fsclFile = new File(tempDir, "FILTSCL.dbin");
			else
				fsclFile = new File(tempDir, "FILTSCL.bin");

			if (fsclFile.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(fsclFile);
				ssf1.start();
				return;
			}

			cod.dirtyFiles.add(fsclFile);
			bb = ByteBuffer.allocateDirect(size);
			sb = bb.asIntBuffer();
			obb = ByteBuffer.allocateDirect(size);
			osb = obb.asIntBuffer();
			osd = obb.asDoubleBuffer();

			samp = new int[size / 4];
			sampF = new double[size / 4];
			outBuf = new int[size / 4];
			outBufd = new double[size / 4];
			tempBuf = new double[size / 4];

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

			if (sampleTimeInUS == 100000) { // Sampling time is 100 ms = 100000 us, 10 Hz

				// Low pass filter - Remove all frequencies above 2 Hz
				// [b,a] = butter(4, 0.4, 'low')

				b = new double[] { 0.046582906636444, 0.186331626545775, 0.279497439818662, 0.186331626545775,
						0.046582906636444 };
				a = new double[] { 1.000000000000000, -0.782095198023338, 0.679978526916299, -0.182675697753032,
						0.030118875043169 };
			} else if (sampleTimeInUS == 1000) { // Sampling time is 1 ms = 1000 us, 1000 Hz

				// Low pass filter - Remove all frequencies above 2 Hz
				// [b,a] = butter(4, 0.004, 'low')

				b = new double[] { 1.0e-08 * 0.153324552060193, 1.0e-08 * 0.613298208240774,
						1.0e-08 * 0.919947312361161, 1.0e-08 * 0.613298208240774, 1.0e-08 * 0.153324552060193 };
				a = new double[] { 1.000000000000000, -3.967162595948849, 5.902025861490880, -3.902558784823240,
						0.967695543813137 };
			}

			if (b == null || a == null)
				return;

			long fL = sclFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered SCL", null, 0, (int) fL);

			outliers = 0;
			filterForAndBackward(a, b, sclFile, fsclFile, progress);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(fsclFile);
			ssf1.start();
			System.out.println("SCL: " + outliers + " outliers found");

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
			System.out.println("filterSCL took (" + timer.getTime() / 1000. + " sec)");
	}

	void filterForAndBackward(double[] a, double[] b, File sclFile, File tempSCLFile, ProgressMonitor progress)
			throws IOException {
		long fL = sclFile.length();
		FileInputStream fis = new FileInputStream(sclFile);
		// byte[] buf = new byte[(int) fL];
		// fis.read(buf);
		// ByteBuffer inBuffer = ByteBuffer.wrap(buf);
		// fis.close();
		// sb = inBuffer.asIntBuffer();
		FileOutputStream fos = new FileOutputStream(tempSCLFile);
		ifC = fis.getChannel();
		ofC = fos.getChannel();

		boolean firstrun = true;
		int nRead = size;
		int nOut;
		long pos = 0;

		if (is7fs) {
			// byte[] outbuf = new byte[(int) fL * 2];
			// ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
			// osd = outBuffer.asDoubleBuffer();
			Comparator<Integer> comparator = (a1, b1) -> sampF[a1] != sampF[b1] ? Double.compare(sampF[a1], sampF[b1])
					: a1 - b1;
			TreeSet<Integer> left = new TreeSet<>(comparator.reversed());
			TreeSet<Integer> right = new TreeSet<>(comparator);

			Supplier<Double> medians = (HAMPELWINDOW % 2 == 0)
					? () -> (sampF[left.first()] + sampF[right.first()]) / 2
					: () -> sampF[right.first()];

			// balance lefts size and rights size (if not equal then right will be larger by
			// one)
			Runnable balance = () -> {
				while (left.size() > right.size())
					right.add(left.pollFirst());
			};
			Expression e;
			try {
				Ams7fsChannelInfo chan = CurrentOpenData.getInstance().getChannelInfoFromID("SCL");
				Set<String> variables = new HashSet<String>(chan.getConstants().keySet());
				variables.add("ch4");
				e = new ExpressionBuilder(chan.getFormula()).variables(variables).build()
						.setVariables(chan.getConstants());
				for (long i = 0; i < fL; i += nRead) {
					progress.setProgress((int) i);
					bb.position(0);
					sb.position(0);
					// int nSRead;
					// if (sb.capacity() - sb.position() < nRead / 4)
					// nSRead = sb.capacity() - sb.position();
					// else
					// nSRead = nRead / 4;
					long newPos = pos;
					pos += size;
					if (pos > fL)
						pos = fL;
					nRead = (int) (pos - newPos);
					ifC.read(bb, newPos);
					int nSRead = nRead / 4;
					if (nRead < 1)
						break;
					sb.get(samp, 0, nSRead);
					if (firstrun == true) {
						e.setVariable("ch4", samp[0]);
						double vl = e.evaluate();
						for (int q = 0; q < backBuffer.length; q++)
							backBuffer[q] = vl;
						for (int q = 0; q < forBuffer.length; q++)
							forBuffer[q] = vl;
						for (int q = 0; q < hampelBuffer.length; q++) {
							e.setVariable("ch4", samp[q]);
							hampelBuffer[q] = e.evaluate();
							left.add(q);
						}
					}
					for (int q = 0; q < nSRead; q++) {

						e.setVariable("ch4", samp[q]);
						double vl = e.evaluate();
						// System.out.println(vl);

						// remove outliers: Hampel method with threshold 10 sd
						// double[] ary = Arrays.copyOf(hampelBuffer, 5);
						// Arrays.sort(ary);
						// slow 'normal' median
						// double median0 = ary[2];

						// sliding window median from @KidOptimo on
						// https://leetcode.com/problems/sliding-window-median/solutions/96346/java-using-two-tree-sets-o-n-logk/
						balance.run();
						double median = medians.get();
						double[] window = new double[HAMPELWINDOW];
						for (int z = 0; z < window.length; z++)
							window[z] = Math.abs(hampelBuffer[z] - median);
						Arrays.sort(window);
						double threshold = 10 * 1.4826 * window[2];
						if (Math.abs(vl - median) > threshold) {
							// System.out.println(samp[q] + " " + vl + " " + median + " " + threshold + " "
							// + Math.abs(vl - median));
							vl = median;
							outliers++;
						}
						if (q > HAMPELWINDOW / 2 + 1 && q < samp.length - (HAMPELWINDOW / 2 + 1)) {
							for (int z = 0; z < hampelBuffer.length - 1; z++)
								hampelBuffer[z] = hampelBuffer[z + 1];
							e.setVariable("ch4", samp[q + 1]);
							hampelBuffer[hampelBuffer.length - 1] = e.evaluate();
							// remove tail of window from either left or right
							// }
							// if (q >= HAMPELWINDOW) {
							if (!left.remove(q - HAMPELWINDOW + 1))
								right.remove(q - HAMPELWINDOW + 1);

							// add next num, this will always increase left size
							right.add(q + 1);
							left.add(right.pollFirst());
						}
						// end remove outliers
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
					nOut = nSRead;

					if (firstrun == true) {
						double vl = tempBuf[nSRead - 1];
						for (int q = 0; q < bBackBuffer.length; q++)
							bBackBuffer[q] = vl;
						for (int q = 0; q < bForBuffer.length; q++)
							bForBuffer[q] = vl;
						firstrun = false;
					}
					for (int q = 0; q < nSRead; q++) {

						double vl = tempBuf[nSRead - q - 1];
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

						outBufd[nSRead - q - 1] = val;
					}
					nOut = nSRead;

					obb.clear();
					osd.clear();
					osd.put(outBufd, 0, nOut);
					obb.limit(8 * nOut);
					ofC.write(obb, newPos);
				}
				// fos.write(outbuf);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else {
			// byte[] outbuf = new byte[(int) fL];
			// ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
			// osb = outBuffer.asIntBuffer();
			Comparator<Integer> comparator = (a1, b1) -> samp[a1] != samp[b1] ? Integer.compare(samp[a1], samp[b1])
					: a1 - b1;
			TreeSet<Integer> left = new TreeSet<>(comparator.reversed());
			TreeSet<Integer> right = new TreeSet<>(comparator);

			Supplier<Double> medians = (HAMPELWINDOW % 2 == 0)
					? () -> ((double) samp[left.first()] + samp[right.first()]) / 2
					: () -> (double) samp[right.first()];

			// balance lefts size and rights size (if not equal then right will be larger by
			// one)
			Runnable balance = () -> {
				while (left.size() > right.size())
					right.add(left.pollFirst());
			};
			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) i / 2);
				bb.position(0);
				sb.position(0);
				// int nSRead;
				// if (sb.capacity() - sb.position() < nRead / 4)
				// nSRead = sb.capacity() - sb.position();
				// else
				// nSRead = nRead / 4;
				long newPos = pos;
				pos += size;
				if (pos > fL)
					pos = fL;
				nRead = (int) (pos - newPos);
				ifC.read(bb, newPos);
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
					for (int q = 0; q < hampelBuffer.length; q++) {
						hampelBuffer[q] = realConstant + realSlope * samp[q];
						left.add(q);
					}
				}
				for (int q = 0; q < nSRead; q++) {

					double vl = realConstant + realSlope * samp[q];
					// remove outliers: Hampel method with threshold 10 sd
					// double[] ary = Arrays.copyOf(hampelBuffer, 5);
					// Arrays.sort(ary);
					// slow 'normal' median
					// double median0 = ary[2];

					// sliding window median from @KidOptimo on
					// https://leetcode.com/problems/sliding-window-median/solutions/96346/java-using-two-tree-sets-o-n-logk/
					balance.run();
					double median = realConstant + realSlope * medians.get();
					double[] window = new double[HAMPELWINDOW];
					for (int z = 0; z < window.length; z++)
						window[z] = Math.abs(hampelBuffer[z] - median);
					Arrays.sort(window);
					double threshold = 10 * 1.4826 * window[2];
					if (Math.abs(vl - median) > threshold) {
						// if (vl != 0.0)
						// fp_outliers++;
						vl = median;
						outliers++;
						// } else if (vl == 0.0) {
						// m_outliers++;
					}
					if (q > HAMPELWINDOW / 2 + 1 && q < samp.length - (HAMPELWINDOW / 2 + 1)) {
						for (int z = 0; z < hampelBuffer.length - 1; z++)
							hampelBuffer[z] = hampelBuffer[z + 1];
						hampelBuffer[hampelBuffer.length - 1] = realConstant
								+ realSlope * samp[q + 1];
						// remove tail of window from either left or right
						// }
						// if (q >= HAMPELWINDOW) {
						if (!left.remove(q - HAMPELWINDOW + 1))
							right.remove(q - HAMPELWINDOW + 1);

						// add next num, this will always increase left size
						right.add(q + 1);
						left.add(right.pollFirst());
					}
					// end remove outliers
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
				nOut = nSRead;

				if (firstrun == true) {
					double vl = tempBuf[nSRead - 1];
					for (int q = 0; q < bBackBuffer.length; q++)
						bBackBuffer[q] = vl;
					for (int q = 0; q < bForBuffer.length; q++)
						bForBuffer[q] = vl;
					firstrun = false;
				}
				for (int q = 0; q < nSRead; q++) {
					double vl = tempBuf[nSRead - q - 1];
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
				ofC.write(obb, newPos);
			}
		}
		ofC.close();
		ifC.close();
		fis.close();
		fos.close();
	}
}