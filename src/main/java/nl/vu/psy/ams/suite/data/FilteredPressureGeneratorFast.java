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

/*
 * Generates a filtered Skin Conductance signal from Measured Pressure signal.
 *  All the frequencies above 2 Hz are removed
 *  
 *  [b a] = butter(4, 0.4, 'low');
 *  
 *  Filtering in both forward and reverse directions to avoid shifting of peaks
 *  
 *  To test use filtsignal = filtfilt(b,a,x) instead of filt(b,a,x)
 */
public class FilteredPressureGeneratorFast {
	private FileChannel ifC;
	private FileChannel ofC;
	ByteBuffer bb;
	IntBuffer sb;
	ByteBuffer obb;
	DoubleBuffer osb;
	int[] samp;
	double[] sampF;
	double[] outBuf;
	double[] tempBuf;
	double[] backBuffer = new double[4];
	double[] forBuffer = new double[4];
	double[] bBackBuffer = new double[4];
	double[] bForBuffer = new double[4];
	int HAMPELWINDOW = 5;
	double[] hampelBuffer = new double[HAMPELWINDOW]; // outlier detection
	double realSlope = 1, realSlopeT = 1;
	double realConstant = 0, realConstantT = 0;
	int outliers = 0;

	public void GenerateFilteredPressure() {

		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File pscFile = new File(tempDir, "P_sc.bin");
			File fpscFile = new File(tempDir, "FILTP_sc.dbin");

			File tscFile = new File(tempDir, "T_sc.bin");
			File ftscFile = new File(tempDir, "FILTT_sc.dbin");

			if (fpscFile.exists() && ftscFile.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(fpscFile);
				ssf1.start();
				SubsetFilesSingle ssf2 = new SubsetFilesSingle(ftscFile);
				ssf2.start();
				return;
			}

			cod.dirtyFiles.add(fpscFile);
			cod.dirtyFiles.add(ftscFile);

			Ams7fsChannelInfo s = null;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("P_sc");
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
				s = CurrentOpenData.getInstance().getChannelInfoFromID("T_sc");
				if (s.getRealSlope() == 0) {
					double lowerBound = -Math.pow(2, s.getnBits() - 1);
					double upperBound = Math.pow(2, s.getnBits() - 1) - 1;
					double lowerValue = (double) s.getlMinValue() / s.getlMinMaxDivider();
					double upperValue = (double) s.getlMaxValue() / s.getlMinMaxDivider();
					realSlopeT = (upperValue - lowerValue) / (upperBound - lowerBound);
					realConstantT = lowerValue - realSlopeT * lowerBound;
				} else {
					realSlopeT = s.getRealSlope();
					realConstantT = s.getRealConstant();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			double[] b = null;
			double[] a = null;

			// Low pass filter - Remove all frequencies above 2.5 Hz
			// [b,a] = butter(4, 0.25, 'low')
			// b = new double[]{0.010209480791203, 0.040837923164812, 0.061256884747219,
			// 0.040837923164812, 0.010209480791203};
			// a = new double[]{1.000000000000000, -1.968427786938519, 1.735860709208887,
			// -0.724470829507363, 0.120389599896245};

			// [b,a] = butter(4, 0.07/2.5, 'low')
			// b = new double[]{3.3442551676373e-06, 1.33770206705492e-05,
			// 2.00655310058238e-05, 1.33770206705492e-05, 3.3442551676373e-06};
			// a = new double[]{1.000000000000000, -3.770173287743291, 5.336562363960367,
			// -3.360909332285416, 0.794573764151023};

			// [b,a] = butter(4, 0.2/2.5, 'low')
			b = new double[] { 0.000183216023369609, 0.000732864093478435, 0.00109929614021765, 0.000732864093478435,
					0.000183216023369609 };
			a = new double[] { 1.000000000000000, -3.34406783771187, 4.23886395088406, -2.40934285658632,
					0.517478199788040 };

			long fL = pscFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered Pressure", null, 0, (int) fL);

			outliers = 0;
			filterForAndBackward(a, b, pscFile, fpscFile, progress, true, tscFile);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(fpscFile);
			ssf1.start();

			System.out.println("P_sc: " + outliers + " outliers found");
			progress.close();

			// [b,a] = butter(4, 0.2/2.5, 'low')
			b = new double[] { 0.000183216023369609, 0.000732864093478435, 0.00109929614021765, 0.000732864093478435,
					0.000183216023369609 };
			a = new double[] { 1.000000000000000, -3.34406783771187, 4.23886395088406, -2.40934285658632,
					0.517478199788040 };

			fL = pscFile.length();
			progress = new ProgressMonitor(frame, "Generating Filtered T_sc", null, 0, (int) fL);

			outliers = 0;
			filterForAndBackward(a, b, tscFile, ftscFile, progress, false, null);
			// filterHampel(tscFile, ftscFile, progress);
			SubsetFilesSingle ssf2 = new SubsetFilesSingle(ftscFile);
			ssf2.start();

			System.out.println("T_sc: " + outliers + " outliers found");
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
			System.out.println("filterPressure took (" + timer.getTime() / 1000. + " sec)");
	}

	void filterForAndBackward(double[] a, double[] b, File pscFile, File tempPressureFile, ProgressMonitor progress,
			boolean pressure, File tscFile) throws IOException {
		int size = (int) pscFile.length();
		bb = ByteBuffer.allocateDirect(size);
		sb = bb.asIntBuffer();
		obb = ByteBuffer.allocateDirect(size * 2);
		osb = obb.asDoubleBuffer();

		samp = new int[size / 4];
		sampF = new double[size / 4];
		outBuf = new double[size / 2];
		tempBuf = new double[size / 2];

		FileInputStream fis = new FileInputStream(pscFile);
		FileOutputStream fos = new FileOutputStream(tempPressureFile);

		int[] sampT = new int[size / 4];
		ByteBuffer bbT = ByteBuffer.allocateDirect(size);
		IntBuffer sbT = bbT.asIntBuffer();
		FileInputStream fisT = null;
		FileChannel ifCT = null;
		if (pressure) {
			fisT = new FileInputStream(tscFile);
			ifCT = fisT.getChannel();
		}
		ifC = fis.getChannel();
		ofC = fos.getChannel();

		Expression e;
		try {
			Ams7fsChannelInfo chanP = CurrentOpenData.getInstance().getChannelInfoFromID("P_sc");
			Ams7fsChannelInfo chanT = CurrentOpenData.getInstance().getChannelInfoFromID("T_sc");
			if (pressure) {
				Set<String> variables = new HashSet<String>(chanP.getConstants().keySet());
				variables.add(chanP.getSzID());
				variables.add("T_sc");
				e = new ExpressionBuilder(chanP.getFormula()).variables(variables).build()
						.setVariables(chanP.getConstants());
			} else {
				Set<String> variables = new HashSet<String>(chanT.getConstants().keySet());
				variables.add(chanT.getSzID());
				e = new ExpressionBuilder(chanT.getFormula()).variables(variables).build()
						.setVariables(chanT.getConstants());
			}
			int nRead = size;
			int nOut;
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

			bb.position(0);
			sb.position(0);
			bbT.position(0);
			sbT.position(0);
			int nSRead;
			if (pressure) {
				nRead = ifCT.read(bbT);
				ifC.read(bb);
				nSRead = nRead / 4;
				sbT.get(sampT, 0, nSRead);
				sb.get(samp, 0, nSRead);
			} else {
				nRead = ifC.read(bb);
				nSRead = nRead / 4;
				sb.get(sampT, 0, nSRead);
			}
			if (nRead < 1) {
				ofC.close();
				ifC.close();
				fis.close();
				fos.close();
				return;
			}
			e.setVariable("T_sc", realConstantT + realSlopeT * sampT[0]);
			if (pressure)
				e.setVariable("P_sc", realConstant + realSlope * samp[0]);
			double vl = e.evaluate();
			for (int q = 0; q < backBuffer.length; q++)
				backBuffer[q] = vl;
			for (int q = 0; q < forBuffer.length; q++)
				forBuffer[q] = vl;
			for (int q = 0; q < hampelBuffer.length; q++) {
				e.setVariable("T_sc", realConstantT + realSlopeT * sampT[q]);
				if (pressure)
					e.setVariable("P_sc", realConstant + realSlope * samp[q]);
				hampelBuffer[q] = e.evaluate();
				left.add(q);
			}
			int progressIncrement = nSRead / 10;
			for (int q = 0; q < nSRead; q++) {
				if (q % progressIncrement == 0) {
					progress.setProgress((int) q / 2);
				}
				e.setVariable("T_sc", realConstantT + realSlopeT * sampT[q]);
				if (pressure)
					e.setVariable("P_sc", realConstant + realSlope * samp[q]);
				vl = e.evaluate();
				sampF[q] = vl;
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
					e.setVariable("T_sc", realConstantT + realSlopeT * sampT[q + HAMPELWINDOW / 2 + 1]);
					if (pressure)
						e.setVariable("P_sc", realConstant + realSlope * samp[q + HAMPELWINDOW / 2 + 1]);
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

			vl = tempBuf[nSRead - 1];
			for (int q = 0; q < bBackBuffer.length; q++)
				bBackBuffer[q] = vl;
			for (int q = 0; q < bForBuffer.length; q++)
				bForBuffer[q] = vl;
			for (int q = 0; q < nSRead; q++) {

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

				outBuf[nSRead - q - 1] = val;
			}
			nOut = nSRead;

			obb.clear();
			osb.clear();
			osb.put(outBuf, 0, nOut);
			obb.limit(8 * nOut);
			ofC.write(obb);
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			ofC.close();
			ifC.close();
			fis.close();
			fos.close();
		}
	}
}
