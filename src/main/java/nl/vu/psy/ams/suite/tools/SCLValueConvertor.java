package nl.vu.psy.ams.suite.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.device.AmsDeviceConstants;
import nl.vu.psy.ams.suite.gui.MainFrame;

/*
 * Converts raw SCL values from the 5fs file or online data,
 * using nonlinear tables. 
 */
public class SCLValueConvertor {

	private static int[][] acList = new int[][] {
			// uS*100 ADsteps
			{ 0, 14 }, { 100, 670 }, { 500, 3295 }, { 1000, 6453 }, { 1484, 9405 }, { 2000, 12480 }, { 3030, 18149 },
			{ 5000, 28111 }, { 6993, 36774 }, { 10000, 48096 }, };

	private static int[][] dcList = new int[][] {
			// uS*100 ADsteps
			{ 0, 0 }, { 100, 771 }, { 500, 3803 }, { 1000, 7449 }, { 1484, 10858 }, { 2000, 14408 }, { 3030, 20962 },
			{ 5000, 32487 }, { 6993, 42516 }, { 10000, 55631 }, };

	private static double[] convList = new double[0xFFFF];

	private static boolean isInitialized = false;
	private static double realSlope;
	private static double realConstant;

	public static void convertFile(File file) {
		if (file.exists() == false)
			return;
		if (isInitialized == false)
			initialize();
		DataInputStream in = null;
		DataOutputStream out = null;
		File tempFile = new File(file.getParentFile(), "tempSCL.bin");
		long fL = file.length();
		JFrame frame = MainFrame.getInstance().getMainFrame();
		try {
			in = new DataInputStream(
					new ProgressMonitorInputStream(frame, "Converting SCL values", new BufferedInputStream(
							new FileInputStream(file), 10485760)));
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile), 10485760));
			for (long i = 0; i < fL; i += 4) {
				out.writeInt(convertValue(in.readInt()));
			}
			in.close();
			out.close();
			file.delete();
			tempFile.renameTo(file);
		} catch (IOException e) {
		} finally {
			if (in != null) {
				try {
					in.close();
					/*
					 * frame.toFront();
					 * frame.requestFocus();
					 */
				} catch (IOException e) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static int convertValue(int inValue) {
		if (isInitialized == false)
			initialize();
		return inverseValue(convList[inValue - Short.MIN_VALUE]);
	}

	public static void initialize() {
		try {
			initialize(CurrentOpenData.getInstance().getChannelInfoFromID("SCL"),
					(CurrentOpenData.getInstance().getSettings().get(0).getDwSettingsFlags()
							& AmsDeviceConstants.AMSII_FLAG_SCLAC) != 0);
		} catch (Exception e) {
		}
	}

	public static void initialize(Ams7fsChannelInfo chan, boolean isSclAC) {
		initialize(chan.getnBits(), chan.getlMinValue(), chan.getlMaxValue(), chan.getlMinMaxDivider(), isSclAC,
				chan.getRealConstant(), chan.getRealSlope());
	}

	public static void initialize(Ams5fsChannelInfo chan, boolean isSclAC) {
		initialize(chan.getnBits(), chan.getlMinValue(), chan.getlMaxValue(), chan.getlMinMaxDivider(), isSclAC,
				chan.getRealConstant(), chan.getRealSlope());
	}

	public static void initialize(long nbits, int lmin, int lmax, int lminmaxdiv, boolean isSclAC, double constant,
			double slope) {
		if (slope == 0) {
			int lowerBound = (int) -Math.pow(2, nbits - 1);
			int upperBound = (int) Math.pow(2, nbits - 1) - 1;
			double lowerValue = (double) lmin / lminmaxdiv;
			double upperValue = (double) lmax / lminmaxdiv;
			realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
			realConstant = lowerValue - realSlope * lowerBound;
		} else {
			realSlope = slope;
			realConstant = constant;
		}

		for (int i = 0; i < 0xFFFF; i++)
			convList[i] = 0;

		int[][] tmpList;
		if (isSclAC) {
			tmpList = acList;
		} else {
			tmpList = dcList;
		}

		for (int i = 1; i < tmpList.length; i++) {
			int beg = tmpList[i - 1][1];
			int end = tmpList[i][1];
			int lVal = tmpList[i - 1][0];
			int rVal = tmpList[i][0];
			double valWidth = (rVal - lVal);
			double indWidth = (end - beg);
			for (int j = beg; j < end; j++) {
				convList[j] = (lVal + (j - beg) * valWidth / indWidth) / 100.;
			}
		}
		int endVal = tmpList[tmpList.length - 1][0];
		int endPos = tmpList[tmpList.length - 1][1];
		for (int i = endPos; i < 0xFFFF; i++)
			convList[i] = endVal / 100.;
		isInitialized = true;
	}

	private static short inverseValue(double val) {
		// never change to anything else than short: calculation is dependent on
		// overflow!!
		return (short) Math.round((val - realConstant) / realSlope);
	}

}
