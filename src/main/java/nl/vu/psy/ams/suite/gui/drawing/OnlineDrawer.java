package nl.vu.psy.ams.suite.gui.drawing;

// import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import nl.vu.psy.ams.suite.data.qrs.QRSDetector;
import nl.vu.psy.ams.suite.data.qrs.shape.QRSShapeDetector;
// import nl.vu.psy.ams.suite.data.structures.sets.StepSet;
// import nl.vu.psy.ams.suite.data.structures.Step;
import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter;
import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter3Order;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.RingBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draws online data. Data is moved to the left automatically,
 * to avoid 'chopiness' with slow connections (bluetooth).
 */
public class OnlineDrawer extends DataDrawer {

	private static final int NVALUES = 300000;

	public static final int CONNECTED = 0;
	public static final int TRYINGTORECONNECT = 1;
	public static final int CONNECTIONLOST = 2;
	public static final int MARKER = 3;
	private RingBuffer buff = new RingBuffer(NVALUES);
	private RingBuffer tickbuff = new RingBuffer(NVALUES);
	// private RingBuffer frontbuf = new RingBuffer(3);
	// private RingBuffer timebuf = new RingBuffer(3);
	private RingBuffer heartratebuf = new RingBuffer(30);
	private double avgHR = 0, stdDevHR = -1, currHR = 0;
	private boolean showHR = false;
	private boolean sclOutOfRange = false;
	private int divider = 10;
	private boolean isdt;
	private boolean isECG;
	private boolean isSCL;
	private boolean isMeanMot;
	private boolean isFiltered = false;
	private double realSlope;

	private double realConstant;

	private double prevVal;

	private double prev2Val;

	private long lastGetTime;
	private int lastGetTick;

	private DecimalFormat df = new DecimalFormat("0.0000E0", new DecimalFormatSymbols(Locale.US));

	private int nValsIn10S = 0;

	public boolean drawVarianceInScreen = false;

	private int nValsIn20S = 0;

	private int nValsIn30S = 0;

	private double varianceIn10S = 0;

	private double varianceIn20S = 0;

	private double varianceIn30S = 0;

	private double meanIn10S = 0;

	private double meanIn20S = 0;

	private double meanIn30S = 0;

	private int connectionStatus;
	private int markervalue;
	private int count = 0; // , countM = 0;
	private String text = "Hide Heart Rate";
	private boolean isReal;
	private boolean useTicks;
	private int curTick;
	private Color color;
	private ButterworthFilter bf;
	private QRSDetector qrs;
	private double ptpw, upw, downw, hT, lT;
	// private StepSet stepSet = new StepSet();

	public OnlineDrawer(String name, YAxis yAxis, boolean showVar, boolean isReal, boolean useTicks, Color color) {
		super(name, yAxis);
		for (int i = 0; i < NVALUES; i++) {
			addValue(0);
		}
		// Initially add 0.85 seconds as beat to beat interval - (i.e) 70 beats per
		// second
		for (int i = 0; i < 30; i++) {
			heartratebuf.add(0.85);
		}
		if (showVar) {
			drawVarianceInScreen = true;
		}
		this.isReal = isReal;
		this.useTicks = useTicks;
		this.color = color;
	}

	public OnlineDrawer(String name, YAxis yAxis, boolean showVar, boolean isReal, boolean useTicks) {
		this(name, yAxis, showVar, isReal, useTicks, Color.BLACK);
	}

	public synchronized void addData(int[] data) {
		for (int i = 0; i < data.length; i++) {
			addValue(data[i]);
			if (isSCL) {
				if (data[i] == 3448)
					sclOutOfRange = true;
				else
					sclOutOfRange = false;
			}
		}
		varianceIn10S = buff.getVarianceFromLastNPoints(nValsIn10S);
		varianceIn20S = buff.getVarianceFromLastNPoints(nValsIn20S);
		varianceIn30S = buff.getVarianceFromLastNPoints(nValsIn30S);
		meanIn10S = buff.getMeanFromLastNPoints(nValsIn10S);
		meanIn20S = buff.getMeanFromLastNPoints(nValsIn20S);
		meanIn30S = buff.getMeanFromLastNPoints(nValsIn30S);
	}

	public synchronized void addData(double[] data) {
		for (int i = 0; i < data.length; i++)
			addValue(data[i]);
		varianceIn10S = buff.getVarianceFromLastNPoints(nValsIn10S);
		varianceIn20S = buff.getVarianceFromLastNPoints(nValsIn20S);
		varianceIn30S = buff.getVarianceFromLastNPoints(nValsIn30S);
		meanIn10S = buff.getMeanFromLastNPoints(nValsIn10S);
		meanIn20S = buff.getMeanFromLastNPoints(nValsIn20S);
		meanIn30S = buff.getMeanFromLastNPoints(nValsIn30S);
	}

	public synchronized void addTicks(int[] ticks) {
		for (int i = 0; i < ticks.length; i++)
			tickbuff.add(ticks[i]);
	}

	public void clear() {
		buff.clear();
	}

	public void clear(double val) {
		double newVal = getRealValueFromSampleValue(val);
		buff.clear(newVal);
	}

	public void addValue(double val) {
		double newVal;
		if (isReal)
			newVal = val;
		else
			newVal = getRealValueFromSampleValue(val);
		if (isDT()) {
			// --- To prevent axis title to revert back from DZDT to DT upon autoscale----
			yAxis.setAxisTitle("-DZ/DT [\u2126/s]");
			double filtVal = ((ButterworthFilter3Order) bf).process(newVal);
			// ----------------------------------------------------------------------------
			double rnewVal = (filtVal - prev2Val) / (2 * divider / 1000.);
			if (prevVal == Double.NEGATIVE_INFINITY || prev2Val == Double.NEGATIVE_INFINITY)
				rnewVal = 0;
			// System.out.println(val + " " + newVal + " " + prevVal + " " + prev2Val + " "
			// + rnewVal);
			prev2Val = prevVal;
			prevVal = filtVal;
			newVal = -rnewVal;
		}
		/*
		 * frontbuf.add(newVal);
		 * 
		 * if (count <= 2) {
		 * // ----------------------- First 3
		 * // Beats-----------------------------------------------------------
		 * if ((frontbuf.get(1) > 3) && ((frontbuf.get(1) > frontbuf.get(0)) &&
		 * (frontbuf.get(1) > frontbuf.get(2)))) {
		 * timebuf.add(System.currentTimeMillis());
		 * count += 1;
		 * }
		 * } else {
		 * if ((frontbuf.get(1) > 3) && ((frontbuf.get(1) > frontbuf.get(0)) &&
		 * (frontbuf.get(1) > frontbuf.get(2)))) {
		 * 
		 * // double previousRRInterval =
		 * Math.abs((timebuf.get(2)-timebuf.get(1))/1000);
		 * double currentRRInterval = Math.abs((System.currentTimeMillis() -
		 * timebuf.get(2)) / 1000);
		 * // System.out.println("Current RR Interval:_ "+
		 * // currentRRInterval+"----"+"Previous RR Interval:_ "+previousRRInterval );
		 * 
		 * // -------------------------- Current work---------------------------
		 * if (currentRRInterval > 1.5) {
		 * // When the RR Interval is greater than 1.5 sec. The interval is 1.5 seconds
		 * // only if the heart rate is 40.
		 * // This means there is no beat detected by the program
		 * // System.out.println("Current Interval is greater than 1.5 seconds. No Beats
		 * // detected");
		 * timebuf.add(System.currentTimeMillis());
		 * } else if (currentRRInterval < 0.27) {
		 * // When the RR Interval is less than 0.27 sec. The interval is 2.7 seconds
		 * only
		 * // if the heart rate is above 210.
		 * // This means false beats are detected by the program (Ex: Noise or T-wave is
		 * // mistaken to be a beat)
		 * // System.out.println("Current Interval is less than 0.27 seconds. False
		 * Beats
		 * // detected");
		 * timebuf.add(System.currentTimeMillis());
		 * } else {
		 * timebuf.add(System.currentTimeMillis());
		 * }
		 * heartratebuf.add((timebuf.get(2) - timebuf.get(1)) / 1000);
		 * 
		 * for (int j = 16; j < 30; j++) {
		 * avgheartrate += heartratebuf.get(j);
		 * }
		 * 
		 * HR = 60 / (avgheartrate / 15);
		 * 
		 * }
		 * avgheartrate = 0;
		 * 
		 * }
		 */
		buff.add(newVal);
		if (isECG && count == 50 && showHeartRate()) {
			// heartratebuf2.add(val);
			qrs = new QRSShapeDetector(ptpw, upw, downw, hT, lT, 0, true, useTicks);
			double[] tmpValues = new double[NVALUES / 100]; // 30 s
			buff.getN(tmpValues);
			double[] tmpTicks = new double[NVALUES / 100];
			tickbuff.getN(tmpTicks);
			// System.out.println(tmpValues[0] + " " + tmpTicks[0]);
			List<Double> tempBeats = qrs.findBeats(tmpValues, tmpTicks);
			int n = tempBeats.size();
			int beatCount = 0;
			double avgIBI = 0.0;
			double[] IBIs = new double[5];
			if (n > 2) {
				// std dev
				double m = 0;
				double s = 0;
				double tmpM = 0;
				int k = 1;
				double value;
				for (int i = 0; i < n - 1; i++) {
					value = tempBeats.get(n - 1 - i) - tempBeats.get(n - 2 - i);
					value = 60000000 / value;
					tmpM = m;
					m += (value - tmpM) / k;
					s += (value - tmpM) * (value - m);
					k++;
				}
				stdDevHR = Math.sqrt(s / (k - 2));
				// std dev
				for (int i = 0; i < 5; i++) {
					if (i == n - 1)
						break;
					// if (tempBeats.get(n - 1 - i) - tempBeats.get(n - 2 - i) < 1800000) {
					IBIs[i] = tempBeats.get(n - 1 - i) - tempBeats.get(n - 2 - i);
					avgIBI += tempBeats.get(n - 1 - i) - tempBeats.get(n - 2 - i);
					beatCount++;
					// }
				}
				currHR = 60000000 / (tempBeats.get(n - 1) - tempBeats.get(n - 2));
				avgHR = 60000000 / (avgIBI / beatCount);
				// if (avgHR < 60) {
				// System.out.println(IBIs[0] + " " + IBIs[1] + " " + IBIs[2] + " " + IBIs[3] +
				// " " + IBIs[4]);
				// System.out.println("HR: " + avgHR + " " + tempBeats + " " + n);
				// }
			} else {
				avgHR = 0;
			}
			count = 0;
		} else if (isECG && showHeartRate()) {
			// heartratebuf2.add(val);
			count += 1;
		}
		// if (isMeanMot && countM == 50) {
		// stepSet.steps.clear();
		// double[] tmpValues = new double[NVALUES / 100]; // 30 s
		// buff.getN(tmpValues);
		// double[] tmpTicks = new double[NVALUES / 100];
		// tickbuff.getN(tmpTicks);
		// double avTemp = 0, stepSearchThreshold;
		// for (int i = 0; i < NVALUES / 100; i++)
		// avTemp += tmpValues[i];
		// stepSearchThreshold = avTemp / (NVALUES / 100.0);
		// double stepDetectionThreshold = stepSearchThreshold + 0.04; // 230
		// boolean checkForStep = false;

		// for (int j = 0; j < tmpValues.length; j++) {
		// if (tmpValues[j] <= stepSearchThreshold && checkForStep == false) {
		// checkForStep = true;
		// }
		// if (checkForStep) {
		// if (tmpValues[j] >= stepDetectionThreshold) {
		// checkForStep = false;
		// long curTime = Calendar.getInstance().getTimeInMillis();
		// long diffTime = curTime - getLastGetTime();
		// curTick = getLastGetTick() + (int) diffTime;
		// double pointTime = tmpTicks[j] - curTick;
		// Step s = new Step(pointTime);
		// s.setImpactValue(tmpValues[j]);
		// stepSet.steps.add(s);
		// }
		// }
		// }
		// countM = 0;
		// } else if (isMeanMot) {
		// countM += 1;
		// }
	}

	@Override
	public void drawData(Graphics2D g) {
		drawDataPoints(g);
		int stat = getConnectionStatus();
		if (stat != CONNECTED) {
			Font oldFont = g.getFont();
			g.setFont(new Font(oldFont.getName(), Font.PLAIN, 20));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String text = "";
			if (stat == TRYINGTORECONNECT) {
				text = "Connection temporarily lost -- trying to automatically reconnect...";
				g.setColor(Color.ORANGE);
			} else if (stat == CONNECTIONLOST) {
				if (isECG()) {
					text = "Connection lost -- ReCalculating Heart Rate!";
				} else {
					text = "Connection lost!";
				}
				g.setColor(Color.RED);
			}
			int strW = g.getFontMetrics().stringWidth(text);
			int strH = g.getFontMetrics().getMaxAscent();
			g.drawString(text, graph.getWidth() / 2f - strW / 2f, 4 * strH);
		}
		if (showHeartRate() == true && isECG() == true) {
			Font oldFont = g.getFont();
			g.setFont(new Font(oldFont.getName(), Font.PLAIN, 20));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String text = "Heart Rate = ";
			double HRRounded = Math.round(avgHR * 10.0) / 10.0;
			double HRCurrRounded = Math.round(currHR * 10.0) / 10.0;
			double SDRounded = Math.round(stdDevHR * 10.0) / 10.0;
			text += Double.toString(HRCurrRounded);
			text += " bpm; avg HR = " + HRRounded + " bpm; SDNN = " + SDRounded + " ms";
			if ((HRRounded > 100) && (HRRounded < 170)) {
				g.setColor(Color.ORANGE);
			} else if ((HRRounded > 170) && (HRRounded < 205)) {
				g.setColor(Color.RED);
			} else if ((HRRounded < 45) || (HRRounded > 205)) {
				// text = "";
			} else {
				g.setColor(Color.GREEN);
			}
			int strW = g.getFontMetrics().stringWidth(text);
			int strH = g.getFontMetrics().getMaxAscent();
			g.drawString(text, graph.getWidth() / 2f - strW / 2f, 2 * strH);
		}
		if (isSCL && sclOutOfRange) {
			Font oldFont = g.getFont();
			g.setFont(new Font(oldFont.getName(), Font.PLAIN, 20));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String text = "SCL value out of range!";
			g.setColor(Color.RED);
			int strW = g.getFontMetrics().stringWidth(text);
			int strH = g.getFontMetrics().getMaxAscent();
			g.drawString(text, graph.getWidth() / 2f - strW / 2f, 2 * strH);
		}
	}

	public synchronized void drawDataPoints(Graphics2D g) {
		g.setColor(color);
		double lTime = graph.getxAxis().getLeftTime() / 1000.;
		double rTime = graph.getxAxis().getRightTime() / 1000.;
		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		long curTime = Calendar.getInstance().getTimeInMillis();
		long diffTime = curTime - getLastGetTime();
		int xPosOld = -1, yPosMin = 100000, yPosMax = -100000, yPosOld = 0;
		if (!useTicks) {
			for (int i = 1; i < NVALUES; i++) {

				double pointTime = divider * (i - NVALUES) - diffTime;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					int xPos = Utils.getPixelCoordinate(pointTime, lTime, rTime, w);
					int yPos = Utils.getPixelCoordinate(val, bVal, tVal, h);
					if (yPos < yPosMin)
						yPosMin = yPos;
					if (yPos > yPosMax)
						yPosMax = yPos;
					if (xPos != xPosOld) {
						if (xPosOld != -1) {
							g.drawLine(xPosOld, h - yPosMin, xPosOld, h - yPosMax);
							g.drawLine(xPosOld, h - yPosOld, xPos, h - yPosMin);
						}
						// if (xPos - xPosOld != 1)
						// g.drawLine(xlPos, h - ylPos, xPos, h - yPos);
						xPosOld = xPos;
						yPosOld = yPosMax;
						yPosMin = 100000;
						yPosMax = -100000;
					}
				}
			}
		} else {
			curTick = getLastGetTick() + (int) diffTime;
			for (int i = 1; i < NVALUES; i++) {

				double pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					int xPos = Utils.getPixelCoordinate(pointTime, lTime, rTime, w);
					int yPos = Utils.getPixelCoordinate(val, bVal, tVal, h);
					if (yPos < yPosMin)
						yPosMin = yPos;
					if (yPos > yPosMax)
						yPosMax = yPos;
					if (xPos != xPosOld) {
						if (xPosOld != -1) {
							g.drawLine(xPosOld, h - yPosMin, xPosOld, h - yPosMax);
							g.drawLine(xPosOld, h - yPosOld, xPos, h - yPosMin);
						}
						// if (xPos - xPosOld != 1)
						// g.drawLine(xlPos, h - ylPos, xPos, h - yPos);
						xPosOld = xPos;
						yPosOld = yPosMax;
						yPosMin = 100000;
						yPosMax = -100000;
					}
				}
			}
		}
		// if (isMeanMot && stepSet.steps.size() < 1000) {
		// int xPos, yPos;
		// for (Step beat : stepSet.steps) {
		// g.setColor(Color.MAGENTA);
		// g.setStroke(new BasicStroke(1));
		// xPos = Utils.getPixelCoordinate(beat.getTime(), lTime, rTime, w);
		// yPos = Utils.getPixelCoordinate(beat.getImpactValue(), bVal, tVal, h);
		// g.drawLine(xPos, h - yPos, xPos, h - 5);
		// g.drawLine(xPos, h, xPos - 5, h - 5);
		// g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
		// g.drawLine(xPos + 5, h - 5, xPos, h);
		// }
		// }

		if (drawVarianceInScreen) {
			Font oldFont = g.getFont();
			g.setFont(new Font(oldFont.getFontName(), Font.PLAIN, 15));
			String text10s = "Variance last 10s: " + df.format(varianceIn10S) + "; mean: " + df.format(meanIn10S);
			String text20s = "Variance last 20s: " + df.format(varianceIn20S) + "; mean: " + df.format(meanIn20S);
			String text30s = "Variance last 30s: " + df.format(varianceIn30S) + "; mean: " + df.format(meanIn30S);
			if (isFiltered) {
				FontMetrics metric = g.getFontMetrics();
				Rectangle2D bounds = metric.getStringBounds(text10s, g);
				double tw = bounds.getWidth();
				g.drawString(text10s, (int) Math.round(w - tw - 10), 30);
				bounds = metric.getStringBounds(text20s, g);
				g.drawString(text20s, (int) Math.round(w - tw - 10), 45);
				bounds = metric.getStringBounds(text30s, g);
				g.drawString(text30s, (int) Math.round(w - tw - 10), 60);

			} else {
				g.drawString(text10s, 10, 30);
				g.drawString(text20s, 10, 45);
				g.drawString(text30s, 10, 60);
			}
		}
	}

	@Override
	public synchronized double[] getBounds() {
		double lTime = graph.getxAxis().getLeftTime() / 1000.;
		double rTime = graph.getxAxis().getRightTime() / 1000.;
		double[] ret = new double[2];
		long curTime = Calendar.getInstance().getTimeInMillis();
		long diffTime = curTime - getLastGetTime();
		double min = Double.NEGATIVE_INFINITY, max = Double.POSITIVE_INFINITY, pointTime;
		if (useTicks)
			curTick = getLastGetTick() + (int) diffTime;
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 1) {
			int length = 0;
			double sum = 0.0, standardDeviation = 0.0;
			for (int i = 0; i < NVALUES; i++) {
				if (!useTicks)
					pointTime = divider * (i - NVALUES) - diffTime;
				else
					pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					sum += val;
					length++;
				}
			}
			double mean = sum / length;
			for (int i = 0; i < NVALUES; i++) {
				if (!useTicks)
					pointTime = divider * (i - NVALUES) - diffTime;
				else
					pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					standardDeviation += Math.pow(val - mean, 2);
				}
			}
			standardDeviation = Math.sqrt(standardDeviation / length);
			min = mean - 3 * standardDeviation;
			max = mean + 3 * standardDeviation;
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 2) {
			int length = 0, i2 = 0;
			for (int i = 0; i < NVALUES; i++) {
				if (!useTicks)
					pointTime = divider * (i - NVALUES) - diffTime;
				else
					pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					length++;
				}
			}
			int arrSize = length / 100;
			if (arrSize > 500)
				arrSize = 500;
			if (arrSize > 0) {
				double[] highOnePercent = new double[arrSize];
				double[] lowOnePercent = new double[arrSize];
				int i = 0;
				for (i = 0; i2 < arrSize; i++) {
					if (!useTicks)
						pointTime = divider * (i - NVALUES) - diffTime;
					else
						pointTime = tickbuff.get(i) - curTick;
					if (pointTime > lTime && pointTime < rTime) {
						highOnePercent[i2] = buff.get(i);
						lowOnePercent[i2] = buff.get(i);
						i2++;
					}
				}
				Arrays.sort(lowOnePercent);
				Arrays.sort(highOnePercent);
				for (; i < NVALUES; i++) {
					if (!useTicks)
						pointTime = divider * (i - NVALUES) - diffTime;
					else
						pointTime = tickbuff.get(i) - curTick;
					if (pointTime > lTime && pointTime < rTime) {
						double val = buff.get(i);
						if (val > highOnePercent[0]) {
							highOnePercent[0] = val;
							Arrays.sort(highOnePercent);
						}
						if (val < lowOnePercent[arrSize - 1]) {
							lowOnePercent[arrSize - 1] = val;
							Arrays.sort(lowOnePercent);
						}
					}
				}
				min = lowOnePercent[arrSize - 1];
				max = highOnePercent[0];
			} else if (length > 0) {
				max = Double.NEGATIVE_INFINITY;
				min = Double.POSITIVE_INFINITY;
				for (int i = 0; i < NVALUES; i++) {
					if (!useTicks)
						pointTime = divider * (i - NVALUES) - diffTime;
					else
						pointTime = tickbuff.get(i) - curTick;
					if (pointTime > lTime && pointTime < rTime) {
						double val = buff.get(i);
						if (val < min)
							min = val;
						if (val > max)
							max = val;
					}
				}
			}
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL) == 0) {
			max = Double.NEGATIVE_INFINITY;
			min = Double.POSITIVE_INFINITY;
			for (int i = 0; i < NVALUES; i++) {
				if (!useTicks)
					pointTime = divider * (i - NVALUES) - diffTime;
				else
					pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					if (val < min)
						min = val;
					if (val > max)
						max = val;
				}
			}
		}
		ret[0] = min;
		ret[1] = max;
		return ret;
	}

	public synchronized int getConnectionStatus() {
		return connectionStatus;
	}

	public synchronized long getLastGetTime() {
		return lastGetTime;
	}

	public synchronized int getLastGetTick() {
		return lastGetTick;
	}

	public double getRealValueFromSampleValue(double sampleValue) {
		return realSlope * sampleValue + realConstant;
	}

	public synchronized boolean isDT() {
		return isdt;
	}

	public synchronized boolean isECG() {
		return isECG;
	}

	public synchronized boolean isMeanMot() {
		return isMeanMot;
	}

	public synchronized void setConnectionStatus(int status) {
		connectionStatus = status;
	}

	public void setDivider(int divider2) {
		divider = divider2;
		nValsIn10S = (int) Math.round(10000. / divider);
		nValsIn20S = (int) Math.round(20000. / divider);
		nValsIn30S = (int) Math.round(30000. / divider);
	}

	public int getDivider() {
		return divider;
	}

	public synchronized void setDT(boolean dt) {
		this.isdt = dt;
		if (dt == true) {
			bf = new ButterworthFilter3Order(1000, 2);
			prevVal = Double.NEGATIVE_INFINITY;
			prev2Val = Double.NEGATIVE_INFINITY;
		}
	}

	public synchronized void setECG(boolean ECG) {
		this.isECG = ECG;
		ptpw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.PTPW), 0, 100) / 100.;
		upw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.UPW), 0, 100) / 100.;
		downw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.DOWNW), 0, 100) / 100.;
		hT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.HIGHTHRESHOLD), 0, 100) / 100.;
		lT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.LOWTHRESHOLD), 0, 100) / 100.;
		qrs = new QRSShapeDetector(ptpw, upw, downw, hT, lT, 0, true, useTicks);
	}

	public synchronized void setSCL(boolean SCL) {
		this.isSCL = SCL;
	}

	public synchronized void setMeanMot(boolean mot) {
		this.isMeanMot = mot;
	}

	public synchronized void setFiltered(boolean filtered) {
		this.isFiltered = filtered;
	}

	public synchronized void setLastGetTime(long time) {
		this.lastGetTime = time;
	}

	public synchronized void setLastGetTick(int tick) {
		this.lastGetTick = tick;
	}

	public void setSlopeAndConstant(double realSlope, double realConstant) {
		this.realSlope = realSlope;
		this.realConstant = realConstant;
	}

	public void setSlopeAndConstant(double lowerBound, double upperBound, double lowerValue, double upperValue) {
		realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
		realConstant = lowerValue - realSlope * lowerBound;
	}

	public synchronized void toggleVarianceInScreen() {
		if (drawVarianceInScreen) {
			drawVarianceInScreen = false;
		} else {
			drawVarianceInScreen = true;
		}
	}

	public synchronized boolean updateIfNeeded(long l) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		if (lTime > 0 || rTime < 0) {
			graph.getxAxis().setLeftTimeF(lTime - l * 1000.);
			graph.getxAxis().setRightTimeF(rTime - l * 1000.);
			return false;
		}
		return true;
	}

	public void setMarkerValue(int value) {
		this.markervalue = value;
	}

	public int getMarkerValue() {
		return markervalue;
	}

	public void showHeartRate(boolean state) {
		if (showHR == false) {
			setHRText("Show Heart Rate");
			showHR = true;
		} else {
			setHRText("Hide Heart Rate");
			showHR = false;
		}
	}

	public boolean showHeartRate() {
		return showHR;
	}

	public void setHRText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public int getCurTick() {
		return curTick;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		long curTime = Calendar.getInstance().getTimeInMillis();
		long diffTime = curTime - getLastGetTime();
		double av = 0;
		long nvals = 0;
		if (!useTicks) {
			for (int i = 1; i < NVALUES; i++) {
				double pointTime = divider * (i - NVALUES) - diffTime;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					av += val;
					nvals++;
				}
			}
		} else {
			curTick = getLastGetTick() + (int) diffTime;
			for (int i = 1; i < NVALUES; i++) {
				double pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					av += val;
					nvals++;
				}
			}
		}
		return av / nvals;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		long curTime = Calendar.getInstance().getTimeInMillis();
		long diffTime = curTime - getLastGetTime();
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double realVal;
		if (!useTicks) {
			for (int i = 1; i < NVALUES; i++) {
				double pointTime = divider * (i - NVALUES) - diffTime;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
					realVal = val;
					tmpM = m;
					m += (realVal - tmpM) / k;
					s += (realVal - tmpM) * (realVal - m);
					k++;
				}
			}
		} else {
			curTick = getLastGetTick() + (int) diffTime;
			for (int i = 1; i < NVALUES; i++) {

				double pointTime = tickbuff.get(i) - curTick;
				if (pointTime > lTime && pointTime < rTime) {
					double val = buff.get(i);
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
		long curTime = Calendar.getInstance().getTimeInMillis();
		long diffTime = curTime - getLastGetTime();
		if (!useTicks) {
			for (int i = 1; i < NVALUES; i++) {
				double pointTime = divider * (i - NVALUES) - diffTime;
				if (pointTime == time) {
					return buff.get(i);
				}
			}
		} else {
			curTick = getLastGetTick() + (int) diffTime;
			for (int i = 1; i < NVALUES; i++) {
				double pointTime = tickbuff.get(i) - curTick;
				if (pointTime == time) {
					return buff.get(i);
				}
			}
		}
		return 0;
	}
}
