package nl.vu.psy.ams.suite.data.files;

import com.gitlab.teuniz.EDFwriter;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * This class can be used to get information from a
 * binary signal file (like ECG.bin).
 */
public class BinaryFile implements AutoCloseable {

	String name;
	private int sampleTimeInUS;
	private long startTime;
	private File baseFile, tickFile;
	private long totalNumberOfShorts;
	private double realSlope;
	private double realConstant;
	private long upperBound;
	private long lowerBound;
	private double upperValue;
	private double lowerValue;
	private String szUnit;
	private static Logger logger = LogManager.getLogger(BinaryFile.class.getName());
	private boolean useTicks = false;
	private RandomAccessFile ris = null;
	private FileInputStream fis = null, fisT = null;
	private FileChannel fc = null, fcT = null;;
	private long fl = 0;

	public BinaryFile(String channelName) {
		this.name = channelName;
		String chanName = name;
		if (channelName.startsWith("FILT")) {
			chanName = channelName.substring(4);
		}

		sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
		try {
			sampleTimeInUS *= CurrentOpenData.getInstance().getChannelInfoFromID(chanName).getDwDivider();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (name.equals("FILTDZ")) {
			sampleTimeInUS *= 100;
			if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
					|| CurrentOpenData.getInstance().fileHeader.getDwHardwareVersion() == 7)
				chanName = "Z0";
			else
				chanName = "DZ";
		}
		startTime = 1000 * CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		baseFile = new File(CurrentOpenData.getInstance().getFilePath(), name + ".bin");
		if (!baseFile.exists()) {
			baseFile = new File(CurrentOpenData.getInstance().getFilePath(), name + ".dbin");
			totalNumberOfShorts = baseFile.length() / 8;
		} else
			totalNumberOfShorts = baseFile.length() / 4;

		Ams7fsChannelInfo chan;
		try {
			chan = CurrentOpenData.getInstance().getChannelInfoFromID(chanName);
			lowerBound = (long) -Math.pow(2, chan.getnBits() - 1);
			upperBound = (long) Math.pow(2, chan.getnBits() - 1) - 1;
			lowerValue = (double) chan.getlMinValue() / chan.getlMinMaxDivider();
			upperValue = (double) chan.getlMaxValue() / chan.getlMinMaxDivider();
			szUnit = chan.getSzUnit();
			tickFile = new File(CurrentOpenData.getInstance().getFilePath(), chan.getTickFile() + ".bin");
			useTicks = tickFile.exists();

			// ----- For AMS Files - Files recorded using old VU-AMS Device---------
			if (chanName.equals("Z0")) {
				if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")
						|| (CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS1"))) {
					lowerBound = 0;
					upperBound = 1024;
					lowerValue = 0.39;
					upperValue = 24.25;
				}
			}
			// ---------------------------------------------------------------------
			if (chan.getRealSlope() == 0) {
				realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
				realConstant = lowerValue - realSlope * lowerBound;
			} else {
				realSlope = chan.getRealSlope();
				realConstant = chan.getRealConstant();
				lowerValue = lowerBound * realSlope + chan.getRealConstant();
				upperValue = upperBound * realSlope + chan.getRealConstant();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			fis = new FileInputStream(baseFile);
			fc = fis.getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (useTicks) {
			try {
				fisT = new FileInputStream(tickFile);
				fcT = fisT.getChannel();
				ris = new RandomAccessFile(tickFile, "r");
				fl = ris.length();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() throws Exception {
		fc.close();
		fis.close();
		if (useTicks) {
			fcT.close();
			fisT.close();
			ris.close();
		}
	}

	public double getAverageBetweenTimes(double leftTime, double rightTime) {
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;

		if (useTicks) {
			if (lOffset < 0)
				lOffset = 0;
			if (rOffset >= totalNumberOfShorts)
				rOffset = totalNumberOfShorts - 1;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return Double.NaN;
		double av = 0;

		// long curOffset = lOffset;
		long nvals = 0;
		// DataInputStream is = null;
		int noofvalues = (int) (rOffset - lOffset - 1);
		double[] datadArray = new double[noofvalues];
		int[] dataArray = new int[noofvalues];
		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		try {
			// is = new DataInputStream(new BufferedInputStream(new
			// FileInputStream(baseFile)));
			// if (baseFile.getName().endsWith(".dbin")) {
			// is.skip(8 * lOffset);
			// while (curOffset <= rOffset) {
			// av += getRealValueFromSampleValue(is.readDouble());
			// nvals++;
			// curOffset++;
			// }
			// } else {
			// is.skip(4 * lOffset);
			// while (curOffset <= rOffset) {
			// av += getRealValueFromSampleValue(is.readInt());
			// nvals++;
			// curOffset++;
			// }
			// }
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8 * noofvalues);
				db = bb.asDoubleBuffer();
				fc.position(8 * lOffset);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				for (int i = 0; i < noofvalues; i++) {
					av += getRealValueFromSampleValue(datadArray[i]);
					nvals++;
				}
			} else {
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				for (int i = 0; i < noofvalues; i++) {
					av += getRealValueFromSampleValue(dataArray[i]);
					nvals++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return av / nvals;
	}

	public double getStdDevBetweenTimes(double leftTime, double rightTime) {
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;

		if (useTicks) {
			if (lOffset < 0)
				lOffset = 0;
			if (rOffset >= totalNumberOfShorts)
				rOffset = totalNumberOfShorts - 1;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return Double.NaN;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double val;

		int noofvalues = (int) (rOffset - lOffset - 1);
		double[] datadArray = new double[noofvalues];
		int[] dataArray = new int[noofvalues];

		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		try {
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8 * noofvalues);
				db = bb.asDoubleBuffer();
				fc.position(8 * lOffset);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				for (int i = 0; i < noofvalues; i++) {
					val = getRealValueFromSampleValue(datadArray[i]);
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
				}
			} else {
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				for (int i = 0; i < noofvalues; i++) {
					val = getRealValueFromSampleValue(dataArray[i]);
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}

	public double[] getDataBetweenTimes(double leftTime, double rightTime) {
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;

		if (useTicks) {
			if (lOffset < 0)
				lOffset = 0;
			if (rOffset >= totalNumberOfShorts)
				rOffset = totalNumberOfShorts - 1;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return null;
		double av = 0;
		int noofvalues = (int) ((rightTime - leftTime) / 1000000) * 10;
		double array[] = new double[noofvalues + 20];
		double[] datadArray = new double[noofvalues];
		int[] dataArray = new int[noofvalues];

		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		int count = 0;
		try {
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8 * noofvalues);
				db = bb.asDoubleBuffer();
				fc.position(8 * lOffset);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				for (int i = 0; i < noofvalues; i++) {
					av = getRealValueFromSampleValue(datadArray[i]);
					array[count] = av;
					count++;
				}
			} else {
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				for (int i = 0; i < noofvalues; i++) {
					av = getRealValueFromSampleValue(dataArray[i]);
					array[count] = av;
					count++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return array;
	}

	public double getDataAtTime(double leftTime) {
		return getDataAtTime(leftTime, true);
	}

	public double getDataAtTime(double leftTime, boolean realData) {
		double dT = getOffsetFromTime(leftTime);
		long lT = Math.round(dT) - 1;
		if (useTicks) {
			if (lT < 0)
				lT = 0;
			if (lT >= totalNumberOfShorts)
				lT = totalNumberOfShorts - 1;
			lT = correctForTicks(lT);
		}

		if (lT < 0)
			lT = 0;
		if (lT >= totalNumberOfShorts)
			lT = totalNumberOfShorts - 1;

		double av = 0;
		double[] datadArray = new double[1];
		int[] dataArray = new int[1];

		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		try {
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8);
				db = bb.asDoubleBuffer();
				fc.position(8 * lT);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				if (realData)
					av = getRealValueFromSampleValue(datadArray[0]);
				else
					av = datadArray[0];
			} else {
				bb = ByteBuffer.allocateDirect(4);
				sb = bb.asIntBuffer();
				fc.position(4 * lT);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				if (realData)
					av = getRealValueFromSampleValue(dataArray[0]);
				else
					av = dataArray[0];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return av;
	}

	public double getMaximumAfterTime(double leftTime) {
		return getMaximumAfterTime(leftTime, true);
	}

	public double getMaximumAfterTime(double leftTime, boolean realData) {
		double dT = getOffsetFromTime(leftTime);
		long lT = Math.round(dT) - 1;
		if (useTicks) {
			if (lT < 0)
				lT = 0;
			if (lT >= totalNumberOfShorts)
				lT = totalNumberOfShorts - 1;
			lT = correctForTicks(lT);
		}

		if (lT < 0)
			lT = 0;
		if (lT >= totalNumberOfShorts)
			lT = totalNumberOfShorts - 1;

		double av = 0, avNext = 0;
		double[] datadArray = new double[1];
		int[] dataArray = new int[1];

		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		try {
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8);
				db = bb.asDoubleBuffer();
				fc.position(8 * lT);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				av = datadArray[0];
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				avNext = datadArray[0];
				while (avNext > av) {
					av = avNext;
					bb.position(0);
					db.position(0);
					fc.read(bb);
					db.get(datadArray, 0, datadArray.length);
					avNext = datadArray[0];
				}
			} else {
				fc.position(4 * lT);
				bb = ByteBuffer.allocateDirect(4);
				sb = bb.asIntBuffer();
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				av = dataArray[0];
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				avNext = dataArray[0];
				while (avNext > av) {
					av = avNext;
					bb.position(0);
					sb.position(0);
					fc.read(bb);
					sb.get(dataArray, 0, dataArray.length);
					avNext = dataArray[0];
				}
			}
			if (realData)
				av = getRealValueFromSampleValue(av);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return av;
	}

    public double[] getDataRun() {
        return getDataRun(startTime, totalNumberOfShorts * sampleTimeInUS);
    }

	public double[] getDataRun(double startTime, double timeLength) {
		int nSamples = (int) Math.ceil(timeLength / sampleTimeInUS);
		double[] ret = new double[nSamples];
		double leftOffset = getOffsetFromTime(startTime);
		double rightOffset = getOffsetFromTime(startTime + timeLength);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;
		if (useTicks) {
			if (lOffset < 0)
				return null;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset >= rOffset)
			return null;
		if (lOffset < 0)
			return null;
		if (lOffset + nSamples > totalNumberOfShorts)
			return null;
		int noofvalues = (int) (rOffset - lOffset - 1);
		double[] array = new double[noofvalues], datadArray = new double[noofvalues];
		int[] tickArray = new int[noofvalues], dataArray = new int[noofvalues];

		// DataInputStream is = null, isT = null;
		ByteBuffer bb = null, bbT = null;
		IntBuffer sb, sbT;
		DoubleBuffer db;
		try {
			// is = new DataInputStream(new BufferedInputStream(new
			// FileInputStream(baseFile)));
			// if (useTicks) {
			// isT = new DataInputStream(new BufferedInputStream(new
			// FileInputStream(tickFile)));
			// isT.skip(4 * lOffset);
			// }
			// if (baseFile.getName().endsWith(".dbin")) {
			// is.skip(8 * lOffset);
			// for (int i = 0; i < noofvalues; i++) {
			// double val = getRealValueFromSampleValue(is.readDouble());
			// array[i] = val;
			// if (useTicks)
			// tickArray[i] = isT.readInt();
			// }
			// } else {
			// is.skip(4 * lOffset);
			// for (int i = 0; i < noofvalues; i++) {
			// double val = getRealValueFromSampleValue(is.readInt());
			// array[i] = val;
			// if (useTicks)
			// tickArray[i] = isT.readInt();
			// }
			// }
			if (useTicks) {
				bbT = ByteBuffer.allocateDirect(4 * noofvalues);
				sbT = bbT.asIntBuffer();
				fcT.position(4 * lOffset);
				bbT.position(0);
				sbT.position(0);
				fcT.read(bbT);
				sbT.get(tickArray, 0, tickArray.length);
			}
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8 * noofvalues);
				db = bb.asDoubleBuffer();
				fc.position(8 * lOffset);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				for (int i = 0; i < noofvalues; i++)
					array[i] = getRealValueFromSampleValue(datadArray[i]);
			} else {
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				for (int i = 0; i < noofvalues; i++)
					array[i] = getRealValueFromSampleValue(dataArray[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!useTicks)
			return array;
		else {
			// if (nSamples - noofvalues > 100 || nSamples - noofvalues < 0) // buffer
			// overflows in ams7
			// return ret;
			int prevTick = tickArray[0], curTick, j = 1;
			double prevVal = array[0], curVal = 0;
			ret[0] = prevVal;
			for (int i = 1; i < array.length; i++) {
				if (j >= ret.length)
					break;
				curTick = tickArray[i];
				curVal = array[i];
				int tickDiff = curTick - prevTick;
				if (tickDiff == 1) {
					if (j >= ret.length)
						break;
					ret[j] = curVal;
					j++;
				} else if (tickDiff == 0) {
					if (j - 1 >= ret.length)
						break;
					ret[j - 1] = (curVal + prevVal) / 2.0;
				} else {
					for (int k = 1; k < tickDiff; k++) {
						if (j == ret.length)
							break;
						ret[j] = prevVal + k * (curVal - prevVal) / tickDiff;
						j++;
					}
					if (j >= ret.length)
						break;
					ret[j] = curVal;
					j++;
				}
				prevTick = curTick;
				prevVal = curVal;
			}
			if (j < ret.length) {
				for (int k = j; k < ret.length; k++)
					ret[k] = curVal;
			}
			// if (j < ret.length)
			// System.out.println("Datarun length: " + j + " " + ret.length);
			return ret;
		}
	}

	public double[][] getDataRuns(double[] startTime, double timeLength, int length) {
		int nSamples = (int) Math.round(timeLength / sampleTimeInUS); // 2048 samples
		double[][] ret = new double[length][nSamples];
		for (int i = 0; i < length; i++) {
			for (int j = 0; j < nSamples; j++) {
				ret[i][j] = 0;
			}
		}
		// DataInputStream is = null;
		// int size = 1048576;
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * nSamples), bbT = null;
		IntBuffer sb = bb.asIntBuffer(), sbT = null;
		if (useTicks) {
			bbT = ByteBuffer.allocateDirect(4 * nSamples);
			sbT = bbT.asIntBuffer();
		}
		try {
			double leftOffset = getOffsetFromTime(startTime[0]);
			Long lOffset = (Long) Math.round(leftOffset);
			if (useTicks)
				lOffset = correctForTicks(lOffset);
			int begI = 0;
			while (lOffset < 0) {
				begI++;
				leftOffset = getOffsetFromTime(startTime[begI]);
				lOffset = (long) Math.round(leftOffset);
				if (useTicks)
					lOffset = correctForTicks(lOffset);
			}
			if (lOffset < 0)
				return null;
			if (lOffset + nSamples >= totalNumberOfShorts)
				return null;
			for (int j = begI; j < length; j++) {
				leftOffset = getOffsetFromTime(startTime[j]);
				lOffset = (long) Math.round(leftOffset);
				if (useTicks)
					lOffset = correctForTicks(lOffset);
				double rightOffset = getOffsetFromTime(startTime[j] + timeLength);
				Long rOffset = (Long) Math.round(rightOffset);
				if (useTicks)
					rOffset = correctForTicks(rOffset);
				int noofvalues = (int) (rOffset - lOffset);
				if (lOffset >= rOffset) { // correctForTicks fail in gap in data
					rOffset = lOffset + 1024;
					noofvalues = 1024;
				}
				double[] array = new double[noofvalues];
				int[] tickArray = new int[noofvalues], dataArray = new int[noofvalues];
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				bbT = ByteBuffer.allocateDirect(4 * noofvalues);
				sbT = bbT.asIntBuffer();
				// if (nSamples - noofvalues > 100 || nSamples - noofvalues < 0) // buffer
				// overflows in ams7
				// continue;
				// double leftOffset2 = getOffsetFromTime(startTime[j]);
				// long lOffset2 = (long) Math.round(leftOffset2);
				// if (useTicks)
				// lOffset2 = correctForTicks(lOffset2);
				// int startI = 0;
				// if (lOffset2 - curOffset < 0) {
				// int df = (int) (lOffset2 - curOffset);
				// while (df < 0 && df + nSamples >= 0) {
				// ret[j][startI] = ret[j - 1][nSamples + df];
				// df++;
				// startI++;
				// }
				// } else {
				// // System.out.print((lOffset2-curOffset) + " ");
				// if (leftOffset2 != curOffset) {
				// long temp;
				// long nSkip = 4 * (lOffset2 - curOffset);
				// while ((temp = is.skip(nSkip)) > 0) {
				// nSkip -= temp;
				// };
				// curOffset = lOffset2;
				// }
				// }
				// for (int i = startI; i < nSamples; i++) {
				// try {
				// double val = getRealValueFromSampleValue(is.readInt());
				// ret[j][i] = val; // ret[no of beats] [ no of samples =2048 ] // for every
				// beat, get the r peaktime and subtract 1024 milliseconds in front of it. From
				// there, take every samples till 2048 milli seconds
				// } catch (EOFException e1) {
				// return ret;
				// }
				// curOffset++;
				// }
				// is.seek(4 * lOffset);
				// if (useTicks) {
				// isT.seek(4 * lOffset);
				// }
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				int nRead = fc.read(bb);
				// int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(dataArray, 0, dataArray.length);
				if (useTicks) {
					fcT.position(4 * lOffset);
					bbT.position(0);
					sbT.position(0);
					fcT.read(bbT);
					sbT.get(tickArray, 0, tickArray.length);
				}
				for (int i = 0; i < noofvalues; i++)
					array[i] = getRealValueFromSampleValue(dataArray[i]);
				// for (int i = 0; i < noofvalues; i++) {
				// double val = getRealValueFromSampleValue(is.readInt());
				// array[i] = val;
				// if (useTicks)
				// tickArray[i] = isT.readInt();
				// }
				if (!useTicks)
					for (int i = 0; i < nSamples; i++) {
						ret[j][i] = array[i];
					}
				else {
					int prevTick = tickArray[0], curTick, j2 = 1;
					double prevVal = array[0], curVal;
					ret[j][0] = prevVal;
					for (int i = 1; i < array.length; i++) {
						if (j2 >= ret[j].length)
							break;
						curTick = tickArray[i];
						curVal = array[i];
						int tickDiff = curTick - prevTick;
						if (tickDiff == 1) {
							if (j2 == ret[j].length)
								break;
							ret[j][j2] = curVal;
							j2++;
						} else if (tickDiff == 0) {
							if (j2 - 1 == ret[j].length)
								break;
							ret[j][j2 - 1] = (curVal + prevVal) / 2.0;
						} else {
							for (int k = 1; k < tickDiff; k++) {
								if (j2 == ret[j].length)
									break;
								ret[j][j2] = prevVal + k * (curVal - prevVal) / tickDiff;
								j2++;
							}
							if (j2 >= ret[j].length)
								break;
							ret[j][j2] = curVal;
							j2++;
						}
						prevTick = curTick;
						prevVal = curVal;
					}
					// if (j2 < ret[j].length)
					// System.out.println("Dataruns length: " + j2 + " " + ret.length);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

	public double[] getMinMaxBetweenTimes(double leftTime, double rightTime) {
		double[] ret = new double[2];
		ret[0] = Double.NaN;
		ret[1] = Double.NaN;
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;

		if (useTicks) {
			if (lOffset < 0)
				lOffset = 0;
			if (rOffset >= totalNumberOfShorts)
				rOffset = totalNumberOfShorts - 1;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return ret;

		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;

		int noofvalues = (int) (rOffset - lOffset - 1);
		double[] datadArray = new double[noofvalues];
		int[] dataArray = new int[noofvalues];

		ByteBuffer bb = null;
		IntBuffer sb;
		DoubleBuffer db;
		try {
			if (baseFile.getName().endsWith(".dbin")) {
				bb = ByteBuffer.allocateDirect(8 * noofvalues);
				db = bb.asDoubleBuffer();
				fc.position(8 * lOffset);
				bb.position(0);
				db.position(0);
				fc.read(bb);
				db.get(datadArray, 0, datadArray.length);
				for (int i = 0; i < noofvalues; i++) {
					double val = getRealValueFromSampleValue(datadArray[i]);
					if (val < ret[0])
						ret[0] = val;
					if (val > ret[1])
						ret[1] = val;
				}
			} else {
				bb = ByteBuffer.allocateDirect(4 * noofvalues);
				sb = bb.asIntBuffer();
				fc.position(4 * lOffset);
				bb.position(0);
				sb.position(0);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				for (int i = 0; i < noofvalues; i++) {
					double val = getRealValueFromSampleValue(dataArray[i]);
					if (val < ret[0])
						ret[0] = val;
					if (val > ret[1])
						ret[1] = val;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public double getMotilityAverage(double leftTime, double rightTime) {

		double[] b = null;
		double[] a = null;

		if (sampleTimeInUS == 1000) {
			b = new double[] { 0.99182421200053306, -3.9672968480021322, 5.9509452720031986, -3.9672968480021322,
					0.99182421200053306 };
			a = new double[] { 1, -3.9835812586585209, 5.9508784292666981, -3.9510124365728321, 0.98371526751047855 };
		} else if (sampleTimeInUS == 2000) {
			b = new double[] { 9.837151741297564000e-001, -3.934860696519025600e+000, 5.902291044778538200e+000,
					-3.934860696519025600e+000,
					9.837151741297564000e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.967162595948848100e+000, 5.902025861490878700e+000,
					-3.902558784823238400e+000,
					9.676955438131369400e-001 };
		} else if (sampleTimeInUS == 4000) {
			b = new double[] { 9.676948088896716300e-001, -3.870779235558686500e+000, 5.806168853338030000e+000,
					-3.870779235558686500e+000,
					9.676948088896716300e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.934325820798736800e+000, 5.805125421055139500e+000,
					-3.807232457228851200e+000,
					9.364332431520188100e-001 };
		} else if (sampleTimeInUS == 10000) {
			b = new double[] { 9.211709934999421400e-001, -3.684683973999768500e+000, 5.527025960999653300e+000,
					-3.684683973999768500e+000,
					9.211709934999421400e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.835825540647348900e+000, 5.520819136622231200e+000,
					-3.533535219463018100e+000,
					8.485559992664779600e-001 };
		} else if (sampleTimeInUS == 100000) {
			b = new double[] { 4.328466449902916800e-001, -1.731386579961166700e+000, 2.597079869941750100e+000,
					-1.731386579961166700e+000,
					4.328466449902916800e-001 };
			a = new double[] { 1.000000000000000000e+000, -2.369513007182036700e+000, 2.313988414415878200e+000,
					-1.054665405878566500e+000,
					1.873794923681847400e-001 };
		}

		if (b == null || a == null)
			return -9999;
		/*
		 * [B,A] = BUTTER(N,Wn) designs an Nth order lowpass digital Butterworth
		 * filter and returns the filter coefficients in length N+1 vectors B
		 * (numerator) and A (denominator). The coefficients are listed in
		 * descending powers of z. The cutoff frequency Wn must be 0.0 < Wn <
		 * 1.0, with 1.0 corresponding to half the sample rate.
		 * 
		 * If Wn is a two-element vector, Wn = [W1 W2], BUTTER returns an order
		 * 2N bandpass filter with passband W1 < W < W2. [B,A] =
		 * BUTTER(N,Wn,'high') designs a highpass filter. [B,A] =
		 * BUTTER(N,Wn,'low') designs a lowpass filter. [B,A] =
		 * BUTTER(N,Wn,'stop') is a bandstop filter if Wn = [W1 W2].
		 * 
		 * When used with three left-hand arguments, as in [Z,P,K] =
		 * BUTTER(...), the zeros and poles are returned in length N column
		 * vectors Z and P, and the gain in scalar K.
		 * 
		 * When used with four left-hand arguments, as in [A,B,C,D] =
		 * BUTTER(...), state-space matrices are returned.
		 * 
		 * BUTTER(N,Wn,'s'), BUTTER(N,Wn,'high','s') and BUTTER(N,Wn,'stop','s')
		 * design analog Butterworth filters. In this case, Wn is in [rad/s] and
		 * it can be greater than 1.0.
		 * 
		 * FILTFILT Zero-phase forward and reverse digital filtering. Y =
		 * FILTFILT(B, A, X) filters the data in vector X with the filter
		 * described by vectors A and B to create the filtered data Y. The
		 * filter is described by the difference equation:
		 * 
		 * y(n) = b(1)*x(n) + b(2)*x(n-1) + ... + b(nb+1)*x(n-nb) - a(2)*y(n-1)
		 * - ... - a(na+1)*y(n-na)
		 * 
		 * 
		 * After filtering in the forward direction, the filtered sequence is
		 * then reversed and run back through the filter; Y is the time reverse
		 * of the output of the second filtering operation. The result has
		 * precisely zero phase distortion and magnitude modified by the square
		 * of the filter's magnitude response. Care is taken to minimize startup
		 * and ending transients by matching initial conditions.
		 * 
		 * The length of the input x must be more than three times the filter
		 * order, defined as max(length(b)-1,length(a)-1).
		 * 
		 * Note that FILTFILT should not be used with differentiator and Hilbert
		 * FIR filters, since the operation of these filters depends heavily on
		 * their phase response.
		 */
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		long lOffset = (long) Math.floor(leftOffset);
		long rOffset = (long) Math.ceil(rightOffset) + 1;

		if (useTicks) {
			if (lOffset < 0)
				lOffset = 0;
			if (rOffset >= totalNumberOfShorts)
				rOffset = totalNumberOfShorts - 1;
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
		}
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return Double.NaN;
		double av = 0;

		int size = 1048576;
		ByteBuffer bb = ByteBuffer.allocateDirect(size);
		IntBuffer sb = bb.asIntBuffer();

		double[] backBuf = new double[4];
		double[] forBuf = new double[4];

		int[] samp = new int[size / 4];
		boolean firstrun = true;
		int nRead = 0;

		long curOffset = lOffset;
		long nvals = 0;
		// DataInputStream is = null;
		try {
			fc.position(4 * lOffset);
			// is = new DataInputStream(new BufferedInputStream(new
			// FileInputStream(baseFile)));
			// is.skip(4 * lOffset);

			while (curOffset <= rOffset) {

				bb.position(0);
				sb.position(0);
				nRead = fc.read(bb);
				int nSRead = nRead / 4;
				if (nSRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				for (int j = 0; j < nSRead; j++) {
					if (curOffset > rOffset)
						break;
					double val = getRealValueFromSampleValue(samp[j]);
					if (firstrun) {
						for (int i = 0; i < 4; i++)
							backBuf[i] = val;
						for (int i = 0; i < 4; i++)
							forBuf[i] = val;
						firstrun = false;
					}
					double newVal = b[0] * val;
					for (int i = 0; i < 4; i++)
						newVal += b[i + 1] * backBuf[i];
					for (int i = 0; i < 4; i++)
						newVal -= a[i + 1] * forBuf[i];
					for (int i = 0; i < 3; i++)
						backBuf[3 - i] = backBuf[2 - i];
					backBuf[0] = val;
					for (int i = 0; i < 3; i++)
						forBuf[3 - i] = forBuf[2 - i];
					forBuf[0] = newVal;

					av += Math.abs(newVal);

					nvals++;
					curOffset++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return av / nvals;
	}

	private double getOffsetFromTime(double time) {
		return ((time - startTime) / sampleTimeInUS);
	}

	public double getRealValueFromSampleValue(double sampleValue) {
		return realSlope * sampleValue + realConstant;
	}

	public int getSampleTimeInUS() {
		return sampleTimeInUS;
	}

	public void outputToASCII(File file, double sRateInUS, String channelname, boolean ecgonly) {
		if (CurrentOpenData.getInstance().channelExists(channelname) == true) {
			if (sRateInUS < sampleTimeInUS) {
				sRateInUS = sampleTimeInUS;
				logger.info("The output sample time is lesser than the input sample rate. " + " , " + "File name:_ "
						+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());

				// JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "The
				// output sample time (" + (int) (sRateInUS / 1000.)
				// + " msec) has to be larger than or equal to the input sample rate (" + (int)
				// (sampleTimeInUS / 1000.) + " msec)!", "Sample rate error",
				// JOptionPane.ERROR_MESSAGE);
				// return;
			}
			int skips = (int) Math.round(sRateInUS / sampleTimeInUS);
			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			String subjectname = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
			double curTime = startTime + skips * sampleTimeInUS / 4.;
			double av = 0;
			int nInAv = 0;
			DataInputStream dis = null;
			PrintWriter pw = null;
			AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
			AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> cats = config.getCategories();
			ArrayList<Ams5fsPacket> events = new ArrayList<Ams5fsPacket>();
			for (Ams5fsPacket p : CurrentOpenData.getInstance().getEvents()) {
				if (p.getlType() <= 2) {
					events.add(p);
				}
			}
			Ams5fsPacket nextEvent = null;
			if (events.isEmpty() == false)
				nextEvent = events.get(0);
			int curEvent = 0;
			String prevLine = null, prevEventString = null, prevLabelString = null, line = null;
			try {
				dis = new DataInputStream(new BufferedInputStream(new FileInputStream(baseFile)));
				pw = new PrintWriter(file);
				pw.println(name);
				pw.println(Utils.getDateAndTimeFromUSWithMS(startTime));
				pw.println("Subject Id: " + subjectname);
				long fL = baseFile.length() / 4;
				int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
				for (long j = 0; j < fL; j++) {
					if (baseFile.getName().endsWith(".dbin"))
						av += getRealValueFromSampleValue(dis.readDouble());
					else
						av += getRealValueFromSampleValue(dis.readInt());
					nInAv++;
					if (nInAv == skips) {
						if (channelname.equals("ECG")
								&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))// JdH Eco
																												// said
																												// warn
																												// and
																												// convert
																												// /3.6
							line = Math.round((curTime - startTime) / 1000.) + " " + ((av / 3.6) / nInAv);
						else
							line = Math.round((curTime - startTime) / 1000.) + " " + (av / nInAv);
						String labelString = "";
						String eventString = Integer.toString(misVal);
						if (ecgonly == false) {// JdH only handle labels and events if actually stored makes raw only
												// export 3 times faster
							AmsLabel lab = lSet.getLabelUnderTime(curTime);
							if (lab != null) {
								Map<String, String> att = lab.getAttributes();
								for (int q = 0; q < cats.size(); q++) {
									String val = att.get(cats.get(q));
									for (LabelValue lv : config.getConfig().get(cats.get(q))) {
										if (lv.getName().equals(val)) {
											labelString += Integer.toString(lv.getCode()) + " ";
										}
									}
								}
							} else {
								for (int q = 0; q < cats.size(); q++)
									labelString += Integer.toString(misVal) + " ";
							}
							while (nextEvent != null) {
								if (nextEvent.getDwClockTick_ms() < curTime / 1000.) {
									double diffTime = curTime - 1000. * nextEvent.getDwClockTick_ms();
									if (diffTime < skips * sampleTimeInUS / 4.) {
										eventString = Integer.toString(nextEvent.getlCode());
									} else {
										prevEventString = Integer.toString(nextEvent.getlCode());
									}
									curEvent++;
									if (curEvent < events.size()) {
										nextEvent = events.get(curEvent);
									} else {
										nextEvent = null;
									}
								} else {
									break;
								}
							}
						}
						if (prevLine != null)
							if (ecgonly == false) {
								pw.println(prevLine + " " + prevEventString + " " + prevLabelString);
							} else {
								pw.println(prevLine);
							}
						prevLine = line;
						if (ecgonly == false) {
							prevEventString = eventString;
							prevLabelString = labelString;
						}
						nInAv = 0;
						av = 0;
						curTime += skips * sampleTimeInUS;
					}
				}
				if (prevLine != null)
					if (ecgonly == false) {
						pw.println(prevLine + " " + prevEventString + " " + prevLabelString);
					} else {
						pw.println(prevLine);
					}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (pw != null) {
					pw.close();
				}
			}
		} else {
			logger.info("The selected channel does not exists or has not been recorded. " + ", "
					+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
		}
	}

	public void outputToEDF(File file, double sRateInUS, String channelname, boolean ecgonly) {
		if (CurrentOpenData.getInstance().channelExists(channelname) == true) {
			if (sRateInUS < sampleTimeInUS) {
				sRateInUS = sampleTimeInUS;
				logger.info("The output sample time is lesser than the input sample rate. " + " , " + "File name:_ "
						+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
			}
			int skips = (int) Math.round(sRateInUS / sampleTimeInUS);
			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			String subjectname = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
			double av = 0;
			int nInAv = 0;
			int sRateInHz = (int) Math.round(1000000 / sRateInUS);
			DataInputStream dis = null;
			EDFwriter ew;
			AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
			AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> cats = config.getCategories();
			ArrayList<Ams5fsPacket> events = new ArrayList<Ams5fsPacket>();
			for (Ams5fsPacket p : CurrentOpenData.getInstance().getEvents()) {
				if (p.getlType() <= 2) {
					events.add(p);
				}
			}
			try {
				dis = new DataInputStream(new BufferedInputStream(new FileInputStream(baseFile)));
				ew = new EDFwriter(file.getPath(), EDFwriter.EDFLIB_FILETYPE_EDFPLUS, 1);
				ew.setSignalLabel(0, name);
				int[] startTimeI = Utils.getDateAndTimeInt(startTime);
				ew.setStartDateTime(startTimeI[0], startTimeI[1], startTimeI[2], startTimeI[3], startTimeI[4],
						startTimeI[5], startTimeI[6]);
				ew.setPatientName(subjectname);
				ew.setPhysicalMaximum(0, upperValue);
				ew.setPhysicalMinimum(0, lowerValue);
				ew.setDigitalMaximum(0, (int) upperBound);
				ew.setDigitalMinimum(0, (int) lowerBound);
				ew.setPhysicalDimension(0, szUnit);
				ew.setSampleFrequency(0, sRateInHz);
				long fL = baseFile.length() / 4;
				double[] buf1 = new double[sRateInHz];
				int count = 0, err;
				for (long j = 0; j < fL; j++) {
					if (count == sRateInHz) {
						err = ew.writePhysicalSamples(buf1);
						if (err != 0) {
							System.out.printf("writePhysicalSamples() returned error: %d\n", err);
						}
						buf1 = new double[sRateInHz];
						count = 0;
					}
					av += getRealValueFromSampleValue(dis.readInt());
					nInAv++;
					if (nInAv == skips) {
						if (channelname.equals("ECG")
								&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))// JdH Eco
																												// said
																												// warn
																												// and
																												// convert
																												// /3.6
							buf1[count] = (av / 3.6) / nInAv;
						else
							buf1[count] = av / nInAv;
						count++;
						nInAv = 0;
						av = 0;
					}
				}
				err = ew.writePhysicalSamples(buf1);
				if (err != 0) {
					System.out.printf("writePhysicalSamples() returned error: %d\n", err);
				}

				if (ecgonly == false) {
					if (events.isEmpty() == false) {
						for (Ams5fsPacket nextEvent : events)
							ew.writeAnnotation((nextEvent.getDwClockTick_ms() - (long) startTime / 1000) * 10, -1,
									Integer.toString(nextEvent.getlCode()));
					}
					for (AmsLabel nextLabel : lSet.getLabels()) {
						String labelString = "";
						Map<String, String> att = nextLabel.getAttributes();
						for (int q = 0; q < cats.size(); q++) {
							String val = att.get(cats.get(q));
							for (LabelValue lv : config.getConfig().get(cats.get(q))) {
								if (lv.getName().equals(val)) {
									labelString += Integer.toString(lv.getCode()) + "_";
								}
							}
						}
						Long start, end;
						start = (long) ((nextLabel.getLeftTime() - (long) startTime) / 100);
						end = (long) ((nextLabel.getRightTime() - nextLabel.getLeftTime()) / 100);
						ew.writeAnnotation(start, end, labelString);
					}
				}
				err = ew.close();
				if (err != 0) {
					System.out.printf("Closing edf file returned error: %d\n", err);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (com.gitlab.teuniz.EDFException e) {
				e.printStackTrace();
			} finally {
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			logger.info("The selected channel does not exists or has not been recorded. " + ", "
					+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
		}
	}

	private Long correctForTicks(long offset) {
		long tick, diff, oldOffset = offset;
		long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		int sampleTime = sampleTimeInUS / 1000;
		if (name.equals("FILTDZ"))
			sampleTime = 1;
		try {
			long len = fl / 4;
			if (offset > len - 1)
				offset = len - 1;
			if (offset < 0)
				offset = 0;
			ris.seek(4 * (offset / sampleTime));
			tick = ris.readInt() - startTime;
			diff = tick - offset;
			offset -= diff;
			long diff2 = tick - oldOffset;
			if (Math.abs(diff) < 100000) {
				int loopCount = 0;
				while (Math.abs(diff2) > sampleTime && loopCount < 1000) {
					if (offset < 0 || 4 * offset >= fl) {
						break;
					}
					ris.seek(4 * (offset / sampleTime));
					tick = ris.readInt() - startTime;
					diff2 = tick - oldOffset;
					offset -= diff2;
					loopCount++;
				}
				if (loopCount > 2)
					System.out.println("correct ticks: " + loopCount + " " + diff + " " + diff2);
			} else {
				// binary search
				long low = 0, high = len - 1, mid = 0;
				while (low <= high) {
					mid = low + (high - low) / 2;
					ris.seek(4 * mid);
					tick = (ris.readInt() - startTime);

					if (tick == oldOffset) {
						offset = mid;
						break;
					} else if (tick < oldOffset)
						low = mid + 1;

					else
						high = mid - 1;
				}
				if (tick != oldOffset) { // not found
					if (low < 0)
						low = 0;
					if (low > len - 1)
						low = len - 1;
					ris.seek(4 * low);
					long tick1 = (ris.readInt() - startTime);
					if (high < 0)
						high = 0;
					if (high > len - 1)
						high = len - 1;
					ris.seek(4 * high);
					long tick2 = (ris.readInt() - startTime);
					if (oldOffset - tick1 < tick2 - oldOffset)
						offset = low;
					else
						offset = high;
				}
				System.out.println("binary search: " + diff);
			}
			if (offset < 0)
				offset = 0;
			if (offset >= fl)
				offset = fl - 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return offset;
	}

}
