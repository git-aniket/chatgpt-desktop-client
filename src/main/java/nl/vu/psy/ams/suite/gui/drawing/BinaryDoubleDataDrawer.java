package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Class that draws a binary file of double values, with a given
 * sample rate.
 */
public class BinaryDoubleDataDrawer extends DataDrawer implements AutoCloseable {

	private double[] buffer;
	private int[] ticks;
	private int bufferSize;
	private int maxDisplayPoints;
	private long lBuffer, rBuffer;
	private long sampleTimeInUS;
	// private double width;
	private int nValues;
	private long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
	private boolean useTicks = false;
	private long totalNumberOfShorts;

	private File baseFile;
	private int curSkip;
	private boolean isCalculating = true;
	private long lOffset;
	private long rOffset;

	private ByteBuffer bb;
	private ByteBuffer bbt;
	private DoubleBuffer buff;
	private IntBuffer tickbuff;
	private String tickFile;
	private RandomAccessFile is = null;
	private long fl = 0;

	public BinaryDoubleDataDrawer(String name, YAxis yAxis, File inFile, double sRateInUS) {
		this(name, yAxis, inFile, sRateInUS, "");
	}

	public BinaryDoubleDataDrawer(String name, YAxis yAxis, File inFile, double sRateInUS, String tickFileStr) {
		this(name, yAxis, inFile, sRateInUS, tickFileStr, Color.BLACK);
	}

	public BinaryDoubleDataDrawer(String name, YAxis yAxis, File inFile, double sRateInUS, String tickFileStr,
			Color color) {
		super(name, yAxis, color);
		File tickFile = new File(CurrentOpenData.getInstance().getFilePath(), tickFileStr + ".bin");
		baseFile = inFile;
		maxDisplayPoints = AppSettings.getInstance().getIntProperty(Settings.MAXDISPLAYPOINTS);
		bufferSize = AppSettings.getInstance().getIntProperty(Settings.BINARYBUFFERSIZE);
		buffer = new double[bufferSize];
		ticks = new int[bufferSize];
		lBuffer = -1;
		rBuffer = -1;
		sampleTimeInUS = (long) sRateInUS;
		if (tickFileStr != "") {
			if (tickFile.exists()) {
				useTicks = true;
				this.tickFile = tickFileStr;
			}
		}
		startTime = 1000L * CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		curSkip = -1;
		bb = ByteBuffer.allocateDirect(bufferSize * 8);
		buff = bb.asDoubleBuffer();
		bbt = ByteBuffer.allocateDirect(bufferSize * 4);
		tickbuff = bbt.asIntBuffer();
		if (useTicks)
			try {
				is = new RandomAccessFile(tickFile, "r");
				fl = is.length();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void close() throws Exception {
		is.close();
	}

	@Override
	public void drawData(Graphics2D g) {
		Color oldColor = g.getColor();
		g.setColor(color);

		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		lOffset = (long) Math.floor(leftOffset);
		rOffset = (long) Math.ceil(rightOffset) + 1;

		if (totalNumberOfShorts == 0)
			totalNumberOfShorts = baseFile.length() / 8;
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return;

		int w = graph.getWidth();
		int h = graph.getHeight();

		long nShorts = rOffset - lOffset;
		int skipNeeded = Utils.GetNextPowerOfTwo((int) (nShorts / maxDisplayPoints));
		if (skipNeeded == 0)
			skipNeeded = 1;
		if (lOffset < lBuffer || rOffset > rBuffer || skipNeeded != curSkip) {
			refillBuffer((rOffset + lOffset) / 2, skipNeeded);
		}

		if (isCalculating == true) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String calcText = "Calculating...";
			g.setFont(new Font("Arial", Font.BOLD, 20));
			FontMetrics met = g.getFontMetrics();
			Rectangle2D bounds = met.getStringBounds(calcText, g);
			g.drawString(calcText, (float) (graph.getWidth() / 2. - bounds.getWidth() / 2.),
					(float) (graph.getHeight() / 2. + bounds.getHeight() / 3.));
			return;
		}
		int curOffset = (int) ((lOffset - lBuffer) / curSkip);
		int nPoints = (int) (nShorts / curSkip);
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, nPoints);
		long curFileOffset;
		curFileOffset = lOffset;
		long prevTick = 0, curTick;
		if (useTicks)
			prevTick = (((long) ticks[curOffset] * 1000L - startTime) / sampleTimeInUS);
		// double wScale = w / (rightOffset - leftOffset);
		double wScale = w / ((rightTime - leftTime) / sampleTimeInUS);
		path.moveTo(curFileOffset, buffer[curOffset]);
		for (int i = 1; i < nPoints; i++) {
			curOffset++;
			if (curOffset >= nValues) {
				// wScale = w / ((rightTime - leftTime) / sampleTimeInUS);
				break;
			}
			if (useTicks) {
				curTick = (((long) ticks[curOffset] * 1000L - startTime) / sampleTimeInUS);
				curFileOffset += (curTick - prevTick);
				prevTick = curTick;
				// if (curFileOffset > rOffset)
				// break;
			} else
				curFileOffset += curSkip;
			path.lineTo(curFileOffset, buffer[curOffset]);
		}
		// String d1 = Double.toString(rightOffset-leftOffset), d2 =
		// Double.toString((rightTime-leftTime) / sampleTimeInUS);
		// System.out.println(d1 + " " + d2);
		AffineTransform at = new AffineTransform();
		at.scale(wScale, -h / (yAxis.getTopValue() - yAxis.getBottomValue()));
		at.translate(-leftOffset, -yAxis.getTopValue());

		path.transform(at);

		g.draw(path);

		// if (values != null) {
		// double lTime = graph.getxAxis().getLeftTime();
		// double rTime = graph.getxAxis().getRightTime();
		// double bVal = yAxis.getBottomValue();
		// double tVal = yAxis.getTopValue();
		// int w = graph.getWidth();
		// int h = graph.getHeight();
		// if (!useTicks) {
		// int li = (int) ((lTime - startTime) / width);
		// if (li < 1)
		// li = 1;
		// for (int i = li; i < nValues; i++) {
		// double rightTime = startTime + (i + 0.5) * width;
		// double leftTime = startTime + (i - 0.5) * width;
		// if (leftTime > rTime)
		// return;
		// int lXPos = Utils.getPixelCoordinate(leftTime, lTime, rTime, w);
		// int rXPos = Utils.getPixelCoordinate(rightTime, lTime, rTime, w);
		// int lYVal = Utils.getPixelCoordinate(values[i - 1], bVal, tVal, h);
		// int rYVal = Utils.getPixelCoordinate(values[i], bVal, tVal, h);
		// g.drawLine(lXPos, h - lYVal, rXPos, h - rYVal);
		// }
		// } else {
		// for (int i = 1; i < nValues; i++) {
		// double rightTime = ticks[i] * 1000.0;
		// double leftTime = ticks[i - 1] * 1000.0;
		// if (leftTime > lTime && rightTime < rTime) {
		// int lXPos = Utils.getPixelCoordinate(leftTime, lTime, rTime, w);
		// int rXPos = Utils.getPixelCoordinate(rightTime, lTime, rTime, w);
		// int lYVal = Utils.getPixelCoordinate(values[i - 1], bVal, tVal, h);
		// int rYVal = Utils.getPixelCoordinate(values[i], bVal, tVal, h);
		// g.drawLine(lXPos, h - lYVal, rXPos, h - rYVal);
		// }
		// }
		// }
		// }
		g.setColor(oldColor);
	}

	private double getOffsetFromTime(double time) {
		if (useTicks) {
			long offset = (long) Math.floor((time - startTime) / sampleTimeInUS);
			int sampleTimeinMS = (int) sampleTimeInUS / 1000;
			long tick, diff, oldOffset = offset;
			long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
			try {
				long len = fl / 4;
				if (offset > len - 1)
					offset = len - 1;
				if (offset < 0)
					offset = 0;
				is.seek(4 * offset);
				// System.out.println(offset + " " + len);
				tick = (is.readInt() - startTime) / sampleTimeinMS;
				diff = tick - offset;
				offset -= diff;
				long diff2 = tick - oldOffset;
				if (Math.abs(diff) < 100000) {
					int loopCount = 0; // , div = 2;
					while (Math.abs(diff2) > sampleTimeinMS && loopCount < 1000) {
						if (offset < 0 || 4 * offset >= fl) {
							break;
						}
						is.seek(4 * offset);
						tick = (is.readInt() - startTime) / sampleTimeinMS;
						diff2 = tick - oldOffset;
						offset -= diff2;
						loopCount++;
					}
					if (loopCount > 2)
						System.out.println("correct ticks ddrawer: " + loopCount + " " + diff);
				} else {
					// binary search
					long low = 0, high = len - 1, mid = 0;
					while (low <= high) {
						mid = low + (high - low) / 2;
						is.seek(4 * mid);
						tick = (is.readInt() - startTime) / sampleTimeinMS;

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
						is.seek(4 * low);
						long tick1 = (is.readInt() - startTime) / sampleTimeinMS;
						if (high < 0)
							high = 0;
						if (high > len - 1)
							high = len - 1;
						is.seek(4 * high);
						long tick2 = (is.readInt() - startTime) / sampleTimeinMS;
						if (oldOffset - tick1 < tick2 - oldOffset)
							offset = low;
						else
							offset = high;
					}
					System.out.println("binary search ddrawer: " + diff);
				}
				if (offset < 0)
					offset = 0;
				if (offset >= fl)
					offset = fl - 1;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return offset;
		} else
			return ((time - startTime) / sampleTimeInUS);
	}

	private void refillBuffer(long midPoint, int skipNeeded) {
		totalNumberOfShorts = baseFile.length() / 8;
		long lPos = midPoint - skipNeeded * bufferSize / 2;
		if (lPos < 0)
			lPos = 0;
		long rPos = lPos + bufferSize * skipNeeded;
		if (rPos >= totalNumberOfShorts) {
			rPos = totalNumberOfShorts;
			lPos = rPos - bufferSize * skipNeeded;
			if (lPos < 0)
				lPos = 0;
		}
		nValues = (int) ((rPos - lPos) / skipNeeded);

		File dataFile, fileTicks;
		if (skipNeeded == 1) {
			dataFile = baseFile;
			fileTicks = new File(CurrentOpenData.getInstance().getFilePath(), tickFile + ".bin");
		} else {
			dataFile = new File(new File(baseFile.getParentFile(), "tmp"), baseFile.getName() + "-" + skipNeeded);
			fileTicks = new File(new File(baseFile.getParentFile(), "tmp"), tickFile + ".bin-" + skipNeeded);
		}
		if (dataFile.exists()) {
			FileChannel fc = null;
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(dataFile);
				fc = fis.getChannel();
				bb.clear();
				bb.limit(8 * nValues);
				fc.read(bb, (long) (8 * Math.floor(lPos / skipNeeded)));
				buff.clear();
				fc.close();
				fis.close();
				buff.get(buffer, 0, nValues);
				if (fileTicks.exists()) {
					fis = new FileInputStream(fileTicks);
					fc = fis.getChannel();
					bbt.clear();
					bbt.limit(4 * nValues);
					fc.read(bbt, (long) (4 * Math.floor(lPos / skipNeeded)));
					tickbuff.clear();
					tickbuff.get(ticks, 0, nValues);
					useTicks = true;
				} else {
					useTicks = false;
				}
			} catch (IOException e) {

			} finally {
				if (fc != null) {
					try {
						fc.close();
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			isCalculating = false;
			curSkip = skipNeeded;
			lBuffer = lPos;
			rBuffer = rPos;
			if (lPos < lBuffer || rPos > rBuffer)
				System.out.println("Problem refill buffer BinarDoubleDataDrawer");
		} else {
			for (int i = 0; i < nValues; i++)
				buffer[i] = Integer.MIN_VALUE;
			isCalculating = true;
			curSkip = 1;
			lBuffer = -1;
			rBuffer = -1;
			nValues = 0;
		}
	}

	@Override
	public double[] getBounds() {
		double[] retArray = new double[2];
		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();
		double leftOffset = getOffsetFromTime(leftTime);
		double rightOffset = getOffsetFromTime(rightTime);
		lOffset = (long) Math.floor(leftOffset);
		rOffset = (long) Math.ceil(rightOffset) + 1;

		totalNumberOfShorts = baseFile.length() / 8;
		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;
		if (lOffset >= rOffset)
			return null;

		long nShorts = rOffset - lOffset;
		int skipNeeded = Utils.GetNextPowerOfTwo((int) (nShorts / maxDisplayPoints));
		if (skipNeeded == 0)
			skipNeeded = 1;
		if (lOffset < lBuffer || rOffset > rBuffer || skipNeeded != curSkip) {
			refillBuffer((rOffset + lOffset) / 2, skipNeeded);
			// try {
			// Thread.sleep(100);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
		}
		if (isCalculating == true)
			return null;

		int lBufferOffset = (int) ((lOffset - lBuffer) / curSkip);
		int nPoints = (int) (nShorts / curSkip);
		int curOffset = lBufferOffset;
		double min = Double.NEGATIVE_INFINITY, max = Double.POSITIVE_INFINITY;
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 1) {
			int length = 0;
			double sum = 0.0, standardDeviation = 0.0;
			for (int i = 0; i < nPoints; i++) {
				if (curOffset >= bufferSize)
					break;
				sum += buffer[curOffset];
				length++;
				curOffset++;
			}
			curOffset = lBufferOffset;
			double mean = sum / length;
			for (int i = 0; i < nPoints; i++) {
				if (curOffset >= bufferSize)
					break;
				standardDeviation += Math.pow(buffer[curOffset] - mean, 2);
				curOffset++;
			}
			standardDeviation = Math.sqrt(standardDeviation / length);
			curOffset = lBufferOffset;
			min = mean - 3 * standardDeviation;
			max = mean + 3 * standardDeviation;
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 2) {
			int arrSize = nPoints / 100;
			if (arrSize > 500)
				arrSize = 500;
			if (arrSize > 0) {
				double[] highOnePercent = new double[arrSize];
				double[] lowOnePercent = new double[arrSize];
				for (int i = 0; i < arrSize; i++) {
					highOnePercent[i] = buffer[curOffset];
					lowOnePercent[i] = buffer[curOffset];
					curOffset++;
				}
				Arrays.sort(lowOnePercent);
				Arrays.sort(highOnePercent);
				for (int i = arrSize; i < nPoints; i++) {
					if (curOffset >= bufferSize)
						break;
					double val = buffer[curOffset];
					if (val > highOnePercent[0]) {
						highOnePercent[0] = val;
						Arrays.sort(highOnePercent);
					}
					if (val < lowOnePercent[arrSize - 1]) {
						lowOnePercent[arrSize - 1] = val;
						Arrays.sort(lowOnePercent);
					}
					curOffset++;
				}
				curOffset = lBufferOffset;
				min = lowOnePercent[arrSize - 1];
				max = highOnePercent[0];
			} else if (nPoints > 0) {
				max = Double.NEGATIVE_INFINITY;
				min = Double.POSITIVE_INFINITY;
				double newValue;
				for (int i = 0; i < nPoints; i++) {
					if (curOffset >= bufferSize)
						break;
					newValue = buffer[curOffset];
					if (newValue < min)
						min = newValue;
					if (newValue > max)
						max = newValue;
					curOffset++;
				}
			}
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 0) {
			max = Double.NEGATIVE_INFINITY;
			min = Double.POSITIVE_INFINITY;
			double newValue;
			for (int i = 0; i < nPoints; i++) {
				if (curOffset >= bufferSize)
					break;
				newValue = buffer[curOffset];
				if (newValue < min)
					min = newValue;
				if (newValue > max)
					max = newValue;
				curOffset++;
			}
		}
		retArray[0] = min;
		retArray[1] = max;

		// double lTime = graph.getxAxis().getLeftTime();
		// double rTime = graph.getxAxis().getRightTime();
		// double[] ret = new double[2];
		// ret[0] = Double.POSITIVE_INFINITY;
		// ret[1] = Double.NEGATIVE_INFINITY;
		// for (int i = 0; i < nValues; i++) {
		// double time = startTime + (i + 0.5) * width;
		// if (time > lTime && time < rTime) {
		// if (values[i] < ret[0])
		// ret[0] = values[i];
		// if (values[i] > ret[1])
		// ret[1] = values[i];
		// }
		// }
		return retArray;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		double av = 0;
		long nvals = 0;
		if (!useTicks) {
			for (int i = 1; i < nValues; i++) {
				double pointTime = startTime + (i + 0.5) * sampleTimeInUS;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buffer[i];
					av += val;
					nvals++;
				}
			}
		} else {
			for (int i = 1; i < nValues; i++) {
				double pointTime = ticks[i] * 1000.0;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buffer[i];
					av += val;
					nvals++;
				}
			}
		}
		return av / nvals;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double realVal;
		if (!useTicks) {
			for (int i = 1; i < nValues; i++) {
				double pointTime = startTime + (i + 0.5) * sampleTimeInUS;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buffer[i];
					realVal = val;
					tmpM = m;
					m += (realVal - tmpM) / k;
					s += (realVal - tmpM) * (realVal - m);
					k++;
				}
			}
		} else {
			for (int i = 1; i < nValues; i++) {

				double pointTime = ticks[i] * 1000.0;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buffer[i];
					realVal = val;
					tmpM = m;
					m += (realVal - tmpM) / k;
					s += (realVal - tmpM) * (realVal - m);
					k++;
				}
			}
		}
		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}

	@Override
	public double getValueAtTime(double time) {
		long offset = Math.round(getOffsetFromTime(time));
		int bufferOffset;
		if (offset < lBuffer || offset > rBuffer)
			refillBuffer(offset, curSkip);
		totalNumberOfShorts = baseFile.length() / 8;
		if (offset < 0 || offset >= totalNumberOfShorts)
			return Integer.MAX_VALUE;
		bufferOffset = (int) ((offset - lBuffer) / curSkip);
		if (bufferOffset < 0 || bufferOffset > nValues)
			return Integer.MAX_VALUE;
		return buffer[bufferOffset];
	}

	public double[] getValuesBetweenTimes(double lTime, double rTime) {
		double lOff = getOffsetFromTime(lTime);
		double rOff = getOffsetFromTime(rTime);
		long loffset = (long) Math.ceil(lOff);
		long roffset = (long) Math.floor(rOff);
		totalNumberOfShorts = baseFile.length() / 8;
		if (loffset < 0 || loffset >= totalNumberOfShorts)
			return null;
		if (roffset < 0 || roffset >= totalNumberOfShorts)
			return null;
		int lbO = (int) ((loffset - lBuffer) / curSkip);
		int rbO = (int) ((roffset - lBuffer) / curSkip);
		if (lbO < 0 || lbO > buffer.length)
			return null;
		if (rbO < 0 || rbO > buffer.length)
			return null;
		int n = 2 * (rbO - lbO + 1);
		if (n < 1)
			return null;
		double[] ret = new double[n];
		int ind = 0;
		for (int i = lbO; i <= rbO; i++) {
			ret[ind] = lTime + (i * curSkip + lBuffer - lOff) * sampleTimeInUS;
			ind++;
			ret[ind] = buffer[i];
			ind++;
		}
		return ret;
	}
}
