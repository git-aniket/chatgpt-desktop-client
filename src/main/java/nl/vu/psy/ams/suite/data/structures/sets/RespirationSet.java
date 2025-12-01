package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.RingBuffer;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class RespirationSet {
	private class RRSuspicionComparator implements Comparator<RespirationCycle> {

		@Override
		public int compare(RespirationCycle o1, RespirationCycle o2) {
			return Double.compare(o2.getRRSuspicion(), o1.getRRSuspicion());
		}

	}

	private TreeSet<RespirationCycle> cycles = new TreeSet<RespirationCycle>();
	private ArrayList<RespirationCycle> cycleArray = new ArrayList<RespirationCycle>();
	private ArrayList<RespirationCycle> beatsSortedByRRSuspicion = new ArrayList<RespirationCycle>();
	private int nHigh;
	private int nMed;

	private int curSelectedSuspiciousRR = 0;

	public void checkForClippingDZ() {
		BinaryFile bf;
		CurrentOpenData cod = CurrentOpenData.getInstance();
		boolean respExists = (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.fileHeader.getDwHardwareVersion() == 7);
		if (respExists)
			// return;
			bf = new BinaryFile("Z0");
		else
			bf = new BinaryFile("DZ");
		int sTime = bf.getSampleTimeInUS();
		int skips = 1000000 / sTime;
		FileInputStream dis = null;
		int[] dataArray = new int[1];
		double minVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMINDZ) / 1000.; // -0.95
		double maxVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMAXDZ) / 1000.; // 0.95
		if (respExists) {
			minVal *= bf.getRealValueFromSampleValue(Math.pow(2, 23)); // signed 24 bits signal
			maxVal *= bf.getRealValueFromSampleValue(Math.pow(2, 23));
		}
		try {
			File dzFile;
			if (respExists)
				dzFile = new File(CurrentOpenData.getInstance().getFilePath(), "Z0.bin");
			else
				dzFile = new File(CurrentOpenData.getInstance().getFilePath(), "DZ.bin");
			dis = new FileInputStream(dzFile);
			FileChannel fc = dis.getChannel();
			ByteBuffer bb = ByteBuffer.allocateDirect(4);
			IntBuffer sb = bb.asIntBuffer();
			long fl = dzFile.length();
			boolean inArtefact = false;
			double lTime = 0;
			double curTime = CurrentOpenData.getInstance().getStartTimeInUS();
			int pos = 0;
			while (pos < fl) {
				bb.position(0);
				sb.position(0);
				fc.position(pos);
				fc.read(bb);
				sb.get(dataArray, 0, dataArray.length);
				double val = bf.getRealValueFromSampleValue(dataArray[0]);
				if (inArtefact) {
					if (val > minVal && val < maxVal) {
						inArtefact = false;
						Set<RespirationCycle> cycs = getCyclesBetweenTimes(lTime, curTime + 1000000.);
						for (RespirationCycle rc : cycs) {
							rc.setClippingDZ(true);
						}
					}
				} else {
					if (val < minVal || val > maxVal) {
						inArtefact = true;
						lTime = curTime - 1000000.;
					}
				}
				pos += skips * 4;
				curTime += skips * sTime;
			}
		} catch (IOException e) {
			// e.printStackTrace();
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
				}
			}
			try {
				bf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// public void checkForIrregularIbi() {
	// checkForIrregularIbi(cycles, new
	// ArrayList<ECGBeat>(CurrentOpenData.getInstance().getBeatSet().getBeats()));
	// }

	public void checkForIrregularRR() {
		checkForIrregularRR(cycles);
	}

	public ArrayList<RespirationCycle> getCycleArray() {
		return cycleArray;
	}

	public TreeSet<RespirationCycle> getCycles() {
		return cycles;
	}

	public NavigableSet<RespirationCycle> getCyclesBetweenTimes(double lTime, double rTime) {
		TreeSet<RespirationCycle> cycs = new TreeSet<RespirationCycle>();
		NavigableSet<RespirationCycle> temp = cycles.headSet(new RespirationCycle(lTime), true);
		if (temp.isEmpty() == false)
			cycs.add(temp.last());
		cycs.addAll(cycles.subSet(new RespirationCycle(lTime), true, new RespirationCycle(rTime), true));
		return cycs;
	}

	public ArrayList<RespirationCycle> getCyclesUnderTimeWithDelay(double time) {
		ArrayList<RespirationCycle> underTime = new ArrayList<RespirationCycle>();
		ArrayList<RespirationCycle> leftOfTime = new ArrayList<RespirationCycle>(
				cycles.headSet(new RespirationCycle(time), true));

		for (int i = leftOfTime.size() - 1; i >= 0; i--) {
			RespirationCycle rc = leftOfTime.get(i);
			if (rc.getExpEnd() + 1000000. > time) {
				underTime.add(rc);
			} else {
				break;
			}
		}
		return underTime;
	}

	public Double getLengthNotUnderArtefacts(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		double avTot = 0;
		for (RespirationCycle rc : cycs) {
			Double length = rc.getLength();
			if (rc.isArtefact() == false) {
				if (length > 0) {
					av += length;
				}
			}
			if (length > 0) {
				avTot += length;
			}
		}
		return av / avTot;
	}

	public Double getRespirationRateBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rr = rc.getRespirationRate();
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxRR(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getRespirationRate();
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getInspDurationBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rr = rc.getExpStart() - rc.getInspStart();
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxInsp(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getExpStart() - rc.getInspStart();
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getExpDurationBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rr = rc.getExpEnd() - rc.getExpStart();
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxExp(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getExpEnd() - rc.getExpStart();
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getInspExpBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rr = (rc.getExpStart() - rc.getInspStart()) / (rc.getExpEnd() - rc.getExpStart());
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxInspExp(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = (rc.getExpStart() - rc.getInspStart()) / (rc.getExpEnd() - rc.getExpStart());
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getShortestIBIBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		Double rr;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				double[] shortest = rc.getShortestIBI();
				if (shortest == null)
					rr = null;
				else
					rr = shortest[1];
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxShortest(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				double[] shortest = rc.getShortestIBI();
				if (shortest == null)
					val = null;
				else
					val = shortest[1];
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getLongestIBIBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		Double rr;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				double[] shortest = rc.getShortestIBI();
				double[] longest = rc.getLongestIBI(shortest);
				if (longest == null)
					rr = null;
				else
					rr = longest[1];
				if (rr != null) {
					av += rr;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxLongest(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				double[] shortest = rc.getShortestIBI();
				double[] longest = rc.getLongestIBI(shortest);
				if (longest == null)
					val = null;
				else
					val = longest[1];
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getRSA0BetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			// When irregular IBI is checked, it results in no difference between RSA0 and
			// RSA. So it is not used
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rsa = rc.getRSA();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (rsa != null) {
						if (rsa >= 0) {
							av += rsa;
							ninAv++;
						} else {
							av += 0;
							ninAv++;
						}
					} else {
						av += 0;
						ninAv++;
					}
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxRSA0(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getRSA();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (val != null && val >= 0)
						val = rc.getRSA();
					else
						val = 0.0;
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getRSABetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rsa = rc.getRSA();
				// Calculate RSA only if the respiration cycle has expiration marked
				if (rc.isExpEndSet()) {
					if (rc.getExpEnd() > rTime)
						break;
					if (rsa != null && rsa >= 0) {
						av += rsa;
						ninAv++;
					}
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxRSA(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getRSA();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (val != null && val >= 0) {
						val = rc.getRSA();
						tmpM = m;
						m += (val - tmpM) / k;
						s += (val - tmpM) * (val - m);
						k++;
						if (val > max)
							max = val;
						if (val < min)
							min = val;
					}
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getRSA0AddedBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			// When irregular IBI is checked, it results in no difference between RSA0 and
			// RSA. So it is not used
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rsa = rc.getRSAAdded();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (rsa != null) {
						if (rsa >= 0) {
							av += rsa;
							ninAv++;
						} else {
							av += 0;
							ninAv++;
						}
					} else {
						av += 0;
						ninAv++;
					}
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxRSA0Added(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getRSAAdded();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (val != null && val >= 0)
						val = rc.getRSAAdded();
					else
						val = 0.0;
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getRSAAddedBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double rsa = rc.getRSAAdded();
				// Calculate RSA only if the respiration cycle has expiration marked
				if (rc.isExpEndSet()) {
					if (rc.getExpEnd() > rTime)
						break;
					if (rsa != null && rsa >= 0) {
						av += rsa;
						ninAv++;
					}
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxRSAAdded(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getRSAAdded();
				if (rc.isExpEndSet()) { // When the cycle has expiration end marked
					if (rc.getExpEnd() > rTime)
						break;
					if (val != null && val >= 0) {
						val = rc.getRSAAdded();
						tmpM = m;
						m += (val - tmpM) / k;
						s += (val - tmpM) * (val - m);
						k++;
						if (val > max)
							max = val;
						if (val < min)
							min = val;
					}
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public NavigableSet<RespirationCycle> getSubset(double lTime, double rTime) {
		if (cycles.isEmpty())
			return new TreeSet<RespirationCycle>();
		RespirationCycle tempCycle = new RespirationCycle(lTime);
		RespirationCycle lCycle = cycles.floor(tempCycle);
		if (lCycle == null)
			lCycle = cycles.first();
		tempCycle = new RespirationCycle(rTime);
		RespirationCycle rCycle = cycles.ceiling(tempCycle);
		if (rCycle == null)
			rCycle = cycles.last();
		return cycles.subSet(lCycle, true, rCycle, true);
	}

	public Double getTidalVolumeBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double tidalVolume = rc.getTidalVolume();
				if (tidalVolume != null) {
					av += tidalVolume;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxTidalVolume(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getTidalVolume();
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public Double getTidalVolumeRawBetweenTimes(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double av = 0;
		long ninAv = 0;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				Double tidalVolume = rc.getTidalVolumeRaw();
				if (tidalVolume != null) {
					av += tidalVolume;
					ninAv++;
				}
			}
		}
		if (ninAv == 0)
			return null;
		return av / ninAv;
	}

	public double[] getStddevMinMaxTidalVolumeRaw(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.MAX_VALUE;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		Double val;
		for (RespirationCycle rc : cycs) {
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				val = rc.getTidalVolumeRaw();
				if (val != null) {
					tmpM = m;
					m += (val - tmpM) / k;
					s += (val - tmpM) * (val - m);
					k++;
					if (val > max)
						max = val;
					if (val < min)
						min = val;
				}
			}
		}
		double[] ret = new double[3];
		if (k == 1) {
			ret[0] = -1;
			ret[1] = -1;
			ret[2] = -1;
		} else {
			ret[0] = Math.sqrt(s / (k - 2));
			ret[1] = min;
			ret[2] = max;
		}
		return ret;
	}

	public double getPercentRejected(double lTime, double rTime) {
		NavigableSet<RespirationCycle> cycs = cycles.subSet(new RespirationCycle(lTime), true,
				new RespirationCycle(rTime), true);
		int accepted = 0, total = 0;
		for (RespirationCycle rc : cycs) {
			total++;
			if (rc.isClippingDZ() == false && rc.isIrregularRR() == false) {
				accepted++;
			}
		}
		return 1.0 - (accepted * 1.0 / total);
	}

	public RespirationCycle getBeatAtSelectedSuspiciousRR() {
		if (beatsSortedByRRSuspicion.isEmpty())
			return null;
		return beatsSortedByRRSuspicion.get(curSelectedSuspiciousRR);
	}

	public int getSelectedSuspiciousRR() {
		return curSelectedSuspiciousRR;
	}

	public RespirationCycle getBeatSortedByIBISuspicion(int index) {
		if (index < 0 || index >= beatsSortedByRRSuspicion.size())
			return null;
		return beatsSortedByRRSuspicion.get(index);
	}

	public int getnHigh() {
		return nHigh;
	}

	public int getnMed() {
		return nMed;
	}

	public void loadFromJSON(File file) {
		String inString;
		Type collectionType;
		Gson gson = new Gson();
		LinkedList<Object> tempList;
		if (file.exists()) {
			cycles.clear();
			collectionType = new TypeToken<LinkedList<RespirationCycle>>() {
			}.getType();
			inString = Utils.readStringFromFile(file);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				cycles.add((RespirationCycle) o);
			}
			cycleArray.clear();
			cycleArray.addAll(cycles);
		}
	}

	public void recalculate() {
		Timer tim = new Timer();
		tim.start();
		cycles.clear();
		File fDz = new File(CurrentOpenData.getInstance().getFilePath(), "FILTDZ.bin"), fDzR;
		File fT = new File(CurrentOpenData.getInstance().getFilePath(), "TicksARed.bin");
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7)
			fDzR = new File(CurrentOpenData.getInstance().getFilePath(), "Z0.bin");
		else
			fDzR = new File(CurrentOpenData.getInstance().getFilePath(), "DZ.bin");
		boolean useTicks = fT.exists();
		BinaryFile bf;
		if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7)
			bf = new BinaryFile("Z0");
		else
			bf = new BinaryFile("DZ");
		BinaryFile bff = new BinaryFile("FILTDZ");
		FileInputStream fis = null, fisT = null, fisR = null;
		boolean insFound = false;
		int sTime = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
		int sTimeR = sTime;
		try {
			sTime *= CurrentOpenData.getInstance().getChannelInfoFromID("DZ").getDwDivider();
		} catch (Exception e1) {
		}
		int nSkipped = (int) (100000 / CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us());
		sTime *= nSkipped;
		// if (sTime < 100000)
		// sTime = 100000;
		double curTime = CurrentOpenData.getInstance().getStartTimeInUS();
		double tickTime = curTime;
		RespirationCycle rc = null;

		int nValsIn500ms = (int) Math.ceil(500000. / sTime); // 5
		int halfIndex = nValsIn500ms / 2; // 2
		int rValsIn500ms = (int) Math.ceil(500000. / sTimeR); // 5
		int halfIndexR = rValsIn500ms / 2; // 2
		RingBuffer buffer500ms = new RingBuffer(nValsIn500ms); // 5
		RingBuffer buffer500msT = new RingBuffer(nValsIn500ms); // 5
		RingBuffer buffer500msR = new RingBuffer(rValsIn500ms); // 5

		curTime += (halfIndex) * sTime;

		double dt = sTime / 1000000.; // 0.1
		double f_exp = Math.pow(dt / 10., dt / 10.); // 0.95499
		double f_expDz = f_exp; // 0.95499

		double fAvgDZ = 0;
		double fAvgDZVar = 0;
		double matched_dz = 1e6;

		int relVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARELTHRESH); // 33
		double fRelativeThreshold = relVal / 100.; // 0.33

		// ArrayList<AmsLabel> artefacts = new ArrayList<AmsLabel>(
		// CurrentOpenData.getInstance().getECGArtefacts().getLabels());
		try {
			fis = new FileInputStream(fDz);
			fisR = new FileInputStream(fDzR);
			int size = 1048576;
			FileChannel fcT = null;
			ByteBuffer bbT = null;
			IntBuffer sbT = null;
			if (useTicks) {
				fisT = new FileInputStream(fT);
				fcT = fisT.getChannel();
				bbT = ByteBuffer.allocateDirect(size);
				sbT = bbT.asIntBuffer();
			}
			FileChannel fc = fis.getChannel();
			ByteBuffer bb = ByteBuffer.allocateDirect(size);
			IntBuffer sb = bb.asIntBuffer();
			FileChannel fcR = fisR.getChannel();
			ByteBuffer bbR = ByteBuffer.allocateDirect(size * nSkipped);
			IntBuffer sbR = bbR.asIntBuffer();
			int[] samp = new int[size / 4], ticks = new int[size / 4], sampR = new int[size / 4 * nSkipped];
			int nRead;
			// DataInputStream dis = new DataInputStream(new
			// ProgressMonitorInputStream(MainFrame.getInstance().getMainFrame(),
			// "Calculating respiration cycles", new
			// BufferedInputStream(fis,10*size)));
			bb.position(0);
			sb.position(0);
			nRead = fc.read(bb);
			fcR.read(bbR);
			int nSRead = nRead / 4;
			sb.get(samp, 0, nSRead);
			sbR.get(sampR, 0, nSRead * nSkipped);
			if (useTicks) {
				fcT.read(bbT);
				sbT.get(ticks, 0, nSRead);
			}
			long nSamples = fDz.length() / 4;
			double prev_dz = samp[0];
			buffer500ms.add(prev_dz);
			buffer500msT.add(ticks[0]);
			buffer500msR.add(sampR[0]);
			int j = 1;
			fAvgDZ = f_expDz * fAvgDZ + (1 - f_expDz) * prev_dz;
			fAvgDZVar = f_exp * fAvgDZVar + (1 - f_exp) * Math.abs(prev_dz - fAvgDZ);
			for (int i = 1; i < halfIndex; i++) {
				double val = samp[i];
				j++;
				buffer500ms.add(val);
				buffer500msT.add(ticks[i]);
				fAvgDZ = f_expDz * fAvgDZ + (1 - f_expDz) * val;
				fAvgDZVar = f_exp * fAvgDZVar + (1 - f_exp) * Math.abs(val - fAvgDZ);
			}
			for (int i = 1; i < halfIndexR; i++) {
				buffer500msR.add(sampR[i]);
			}
			for (int i = halfIndex; i < nValsIn500ms; i++) {
				buffer500ms.add(samp[i]);
				buffer500msT.add(ticks[i]);
				j++;
			}
			for (int i = halfIndex; i < rValsIn500ms; i++) {
				buffer500msR.add(sampR[i]);
			}
			double[] minmax = new double[2];
			int[] minmaxpos = new int[2];
			buffer500ms.getMinMaxWithPos(minmax, minmaxpos);
			// double lArtefactTime = Double.POSITIVE_INFINITY;
			// double rArtefactTime = Double.POSITIVE_INFINITY;
			// if (artefacts.isEmpty() == false) {
			// AmsLabel art = artefacts.get(0);
			// lArtefactTime = art.getLeftTime();
			// rArtefactTime = art.getRightTime();
			// }
			// int curArtefact = 0;
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor prog = new ProgressMonitor(frame, "Detection respiration cycles", null, 0, (int) nSamples);
			for (long i = 0; i < nSamples; i += nSRead) {
				prog.setProgress((int) i);
				if (j >= nSRead) {
					bb.position(0);
					sb.position(0);
					nRead = fc.read(bb);
					bbR.position(0);
					sbR.position(0);
					fcR.read(bbR);
					nSRead = nRead / 4;
					if (nRead < 1)
						break;
					sb.get(samp, 0, nSRead);
					sbR.get(sampR, 0, nSRead * nSkipped);
					if (useTicks) {
						bbT.position(0);
						sbT.position(0);
						fcT.read(bbT);
						sbT.get(ticks, 0, nSRead);

					}
					j = 0;
				}
				for (; j < nSRead; j++) {
					// if (curTime >= lArtefactTime && curTime <= rArtefactTime) {
					// fAvgDZ = 0;
					// fAvgDZVar = 0;
					// matched_dz = 1e6;
					// insFound = false;
					// curTime += sTime;
					// if (useTicks)
					// tickTime = ticks[j] * 1000.0;
					// else
					// tickTime = curTime;
					// buffer500ms.add(samp[j]);
					// buffer500msT.add(ticks[j]);
					// if (rc != null) {
					// rc.delExpEnd();
					// if (cycles.isEmpty() == false) {
					// cycles.last().delExpEnd();
					// }
					// }
					// rc = null;
					// continue;
					// }
					// while (curTime > rArtefactTime) {
					// curArtefact++;
					// if (curArtefact < artefacts.size()) {
					// AmsLabel art = artefacts.get(curArtefact);
					// lArtefactTime = art.getLeftTime();
					// rArtefactTime = art.getRightTime();
					// } else {
					// lArtefactTime = Double.POSITIVE_INFINITY;
					// rArtefactTime = Double.POSITIVE_INFINITY;
					// }
					// }
					double val = buffer500ms.get(halfIndex);
					double valR = buffer500msR.get(halfIndexR);
					if (useTicks)
						tickTime = buffer500msT.get(halfIndex) * 1000.0;
					fAvgDZ = f_expDz * fAvgDZ + (1 - f_expDz) * val;
					fAvgDZVar = f_exp * fAvgDZVar + (1 - f_exp) * Math.abs(val - fAvgDZ);
					if (insFound == false) {
						if (val == minmax[0]) {
							if (Math.abs(matched_dz - val) > fRelativeThreshold * fAvgDZVar) {
								insFound = true;
								if (rc != null) {
									rc.setExpEnd(tickTime);
								}
								rc = new RespirationCycle(tickTime);
								rc.setLowRealValue(bf.getRealValueFromSampleValue(val));
								rc.setLowValue(bff.getRealValueFromSampleValue(valR));
								matched_dz = val;
							}
						} else if (val == minmax[1]) {
							if (rc != null) {
								if (val > matched_dz) {
									rc.setExpStart(tickTime);
									rc.setHighRealValue(bf.getRealValueFromSampleValue(val));
									rc.setHighValue(bff.getRealValueFromSampleValue(valR));
									matched_dz = val;
								}
							}
						}
					} else {
						if (val == minmax[1]) {
							if (Math.abs(matched_dz - val) > fRelativeThreshold * fAvgDZVar) {
								insFound = false;
								rc.setExpStart(tickTime);
								cycles.add(rc);
								rc.setHighRealValue(bf.getRealValueFromSampleValue(val));
								rc.setHighValue(bff.getRealValueFromSampleValue(valR));
								matched_dz = val;
							}
						} else if (val == minmax[0]) {
							if (val < matched_dz) {
								rc = new RespirationCycle(tickTime);
								rc.setLowRealValue(bf.getRealValueFromSampleValue(val));
								rc.setLowValue(bff.getRealValueFromSampleValue(valR));
								matched_dz = val;
								if (cycles.isEmpty() == false) {
									if (cycles.last().isExpEndSet()) {
										cycles.last().setExpEnd(tickTime);
									}
								}
							}
						}
					}
					curTime += sTime;
					if (useTicks)
						tickTime = ticks[j] * 1000.0;
					else
						tickTime = curTime;
					double newVal = samp[j];
					buffer500ms.add(newVal);
					buffer500msT.add(ticks[j]);
					for (int k = 0; k < nSkipped; k++)
						buffer500msR.add(sampR[j * nSkipped + k]);
					minmaxpos[0]--;
					minmaxpos[1]--;
					if (newVal <= minmax[0]) {
						minmax[0] = newVal;
						minmaxpos[0] = nValsIn500ms;
					}
					if (newVal >= minmax[1]) {
						minmax[1] = newVal;
						minmaxpos[1] = nValsIn500ms;
					}
					if (minmaxpos[0] < 0 || minmaxpos[1] < 0) {
						buffer500ms.getMinMaxWithPos(minmax, minmaxpos);
					}
				}
			}
			prog.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fisR != null) {
				try {
					fisR.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fisT != null) {
				try {
					fisT.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				bf.close();
				bff.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		tim.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Respirationset recalculate took (" + tim.getTime() / 1000. + " sec)");

		int val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSADZRANGECHECK);
		if (val != 0 && !useTicks) // only for 5fs
			checkForClippingDZ();
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATECHECK);
		if (val != 0)
			checkForIrregularRR();
		// val =
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAIBICHECK);
		// if (val != 0)
		// checkForIrregularIbi();

		cycleArray.clear();
		cycleArray.addAll(cycles);
		CurrentOpenData.getInstance().setDirty(true);
	}

	// ----- To include IBI information for Respiration Cycle Calculation and keep
	// the two tabs synced--------
	public void rescan(double lTime, double rTime) {
		SortedSet<RespirationCycle> tempCycle = (SortedSet<RespirationCycle>) getCyclesBetweenTimes(lTime, rTime);
		// ArrayList<ECGBeat> beats = new ArrayList<ECGBeat>(
		// CurrentOpenData.getInstance().getBeatSet().getBeatBetweenTimes(lTime,
		// rTime));
		// checkForIrregularIbi(tempCycle, beats);
		checkForIrregularRR(tempCycle);
	}
	// -------------------------------------------------------------------------------------------------------
	// public void checkForIrregularIbi(SortedSet<RespirationCycle> respCycle,
	// ArrayList<ECGBeat> ecgbeats) {

	// for (RespirationCycle rc : respCycle)
	// rc.setIrregularIBI(false);

	// ArrayList<ECGBeat> beats = ecgbeats;
	// for (int i = 0; i < beats.size(); i++) {
	// ECGBeat beat = beats.get(i);
	// if (beat.getIBISuspicion() > 1000) {
	// ArrayList<RespirationCycle> underTime =
	// getCyclesUnderTimeWithDelay(beat.getRPeakTime());
	// for (RespirationCycle rc : underTime) {
	// rc.setIrregularIBI(true);
	// }
	// }
	// }
	// }

	public void checkForIrregularRR(SortedSet<RespirationCycle> respCycles) {
		for (RespirationCycle rc : respCycles) {
			rc.setIrregularRR(false);
		}
		int nCyclesInAv = 0;
		double expAv = 0;
		double maxDiff = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATEMAX) / 100.;
		double lowR = 1 - maxDiff;
		double hiR = 1 + maxDiff;
		for (RespirationCycle rc : respCycles) {
			Double rr = rc.getRespirationRate();
			if (rr != null) {
				if (nCyclesInAv == 20) {
					rc.setRRSuspicion(Math.abs((rr / expAv) - 1));
					if (rr < lowR * expAv || rr > hiR * expAv) {
						rc.setIrregularRR(true);
					}
					expAv = 0.95 * expAv + (1 - 0.95) * rr;
				} else {
					if (nCyclesInAv == 0) {
						expAv = rr;
					} else {
						expAv = 0.95 * expAv + (1 - 0.95) * rr;
					}
					nCyclesInAv++;
				}
				// if (Math.abs(rc.getRealHighValue() - rc.getRealLowValue()) < 0.01) {
				// rc.setIrregularRR(true);
				// if (rc.getRRSuspicion() < maxDiff)
				// System.out.println(rr + " " + expAv);
				// }
			} else {
				nCyclesInAv = 0;
			}
		}
		recheckSuspiciousRRS();
	}

	public void recheckSuspiciousRRS() {
		beatsSortedByRRSuspicion.clear();
		ArrayList<RespirationCycle> tempBeatList = new ArrayList<RespirationCycle>(cycles);
		Collections.sort(tempBeatList, new RRSuspicionComparator());
		double maxDiff = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATEMAX) / 100.;
		double[] minmax = new double[2];
		double[] minmaxSusp = new double[2];
		minmax[0] = Double.POSITIVE_INFINITY;
		minmax[1] = Double.NEGATIVE_INFINITY;
		minmaxSusp[0] = Double.POSITIVE_INFINITY;
		minmaxSusp[1] = Double.NEGATIVE_INFINITY;
		nHigh = 0;
		nMed = 0;
		for (RespirationCycle b : tempBeatList) {
			beatsSortedByRRSuspicion.add(b);
			if (b.getRRSuspicion() > maxDiff * 2) {
				nHigh++;
			} else if (b.getRRSuspicion() > maxDiff) {
				nMed++;
			}
			if (!b.isIrregularRR()) {
				if (b.getRRSuspicion() < minmax[0])
					minmax[0] = b.getRRSuspicion();
				if (b.getRRSuspicion() > minmax[1])
					minmax[1] = b.getRRSuspicion();
			} else {
				if (b.getRRSuspicion() < minmaxSusp[0])
					minmaxSusp[0] = b.getRRSuspicion();
				if (b.getRRSuspicion() > minmaxSusp[1])
					minmaxSusp[1] = b.getRRSuspicion();
			}
		}
		// System.out.println(minmax[0] + " " + minmax[1] + " " + minmaxSusp[0] + " " +
		// minmaxSusp[1]);
	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(cycles));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void toRSRFile(File file, boolean append) {
		int labelnumber = 0;
		AmsLabel curLabel = new AmsLabel();
		// For Batch export of RSR files, a boolean variable called isAppend is added.
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(file, append)));

			int counter = 0;
			String sId = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(2);
			nf.setGroupingUsed(false);
			AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> cats = config.getCategories();
			AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
			CurrentOpenData cod = CurrentOpenData.getInstance();
			String chan;
			if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7)
				chan = "Z0";
			else
				chan = "DZ";
			/*--- Code to print Header/Variable Names in the RSR File-----------------------*/
			if (!append) {
				pw.print("Subject Id");
				pw.print("\t");
				pw.print("Respiration Cycle Number");
				pw.print("\t");
				pw.print("Date");
				pw.print("\t");
				pw.print("Start of Respiratory Cycle");
				pw.print("\t");
				pw.print("Inspiration Time (msec)");
				pw.print("\t");
				pw.print("Expiration Time (msec)");
				pw.print("\t");
				pw.print("Shortest accelerating IBI in inspiration [msec]");
				pw.print("\t");
				pw.print("Longest decelerating IBI in expiration [msec]");
				pw.print("\t");
				pw.print("RR [breath per min]");
				pw.print("\t");
				pw.print("RSA [msec]");
				pw.print("\t");
				pw.print("Mean IBI across the cycle [msec]");
				pw.print("\t");
				pw.print("Amplitude filtered DZ at start inspiration [m\u2126/s]");
				pw.print("\t");
				pw.print("Amplitude filtered DZ at start expiration [m\u2126/s]");
				pw.print("\t");
				pw.print("Tidal volume (filtered) [m\u2126/s] -- calibration is needed to translate this to ml");
				pw.print("\t");
				pw.print("Amplitude " + chan + " at start inspiration [m\u2126/s]");
				pw.print("\t");
				pw.print("Amplitude " + chan + " at start expiration [m\u2126/s]");
				pw.print("\t");
				pw.print("Tidal volume [m\u2126/s] -- calibration is needed to translate this to ml");
				pw.print("\t");
				pw.print("Rejected (R) as artefact or accepted (A)");
				pw.print("\t");
				pw.print("Label #");
				for (int j = 0; j < cats.size(); j++) {
					pw.print("\t");
					pw.print("Label code (-9999 = not available)");
				}
				pw.println();
			}

			for (RespirationCycle rc : cycles) {
				String line = "";
				line += sId + "\t";
				line += counter + "\t";
				line += Utils.getDateFromUS(rc.getInspStart()) + "\t";
				line += Utils.getTimeFromUS(rc.getInspStart()) + "\t";
				line += nf.format((rc.getExpStart() - rc.getInspStart()) / 1000.) + "\t";
				if (rc.isExpEndSet()) {
					line += nf.format((rc.getExpEnd() - rc.getExpStart()) / 1000.) + "\t";
				} else {
					line += "-1\t";
				}
				double[] shortest = rc.getShortestIBI();
				double[] longest = rc.getLongestIBI(shortest);
				if (shortest != null) {
					line += nf.format(shortest[1] / 1000.) + "\t";
				} else {
					line += "-1\t";
				}
				if (longest != null) {
					line += nf.format(longest[1] / 1000.) + "\t";
				} else {
					line += "-1\t";
				}
				Double rr = rc.getRespirationRate();
				if (rr != null) {
					line += nf.format(rr) + "\t";
				} else {
					line += "-1\t";
				}
				Double rsa = rc.getRSA();
				if (rsa != null) {
					if (rsa > 0) {
						line += nf.format(rsa) + "\t";
					} else {
						line += "-4\t";
					}
				} else {
					if (shortest == null && longest == null) {
						line += "-3\t";
					} else {
						if (shortest == null) {
							line += "-1\t";
						} else if (longest == null) {
							line += "-2\t";
						} else {
							line += "-5\t";
						}
					}
				}
				Double meanIBI = rc.getMeanIBI();
				if (meanIBI != null) {
					line += nf.format(meanIBI) + "\t";
				} else {
					line += "-1\t";
				}

				double lowVal = rc.getRealLowValue();
				double highVal = rc.getRealHighValue();
				line += nf.format(lowVal * 1000.) + "\t";
				line += nf.format(highVal * 1000.) + "\t";
				line += nf.format((highVal - lowVal) * 1000.) + "\t";

				double lowValRaw = rc.getLowValue();
				double highValRaw = rc.getHighValue();
				line += nf.format(lowValRaw * 1000.) + "\t";
				line += nf.format(highValRaw * 1000.) + "\t";
				line += nf.format((highValRaw - lowValRaw) * 1000.) + "\t";

				if (rc.isClippingDZ() || rc.isExpEndSet() == false || /* rc.isIrregularIbi() || */ rc.isIrregularRR()) {
					line += "R\t";
				} else {
					line += "A\t";
				}

				AmsLabel lab = lSet.getLabelUnderTime(rc.getInspStart());
				if (lab != null) {
					// ------ To insert label number-----------------------
					if (!(curLabel.equals(lab))) {
						curLabel = lab;
						labelnumber += 1;
					}
					// ---------------------------------------------------
					if (rc.isExpEndSet()) {
						AmsLabel lab2 = lSet.getLabelUnderTime(rc.getExpEnd());
						if (lab2 != null)
							if (lab.equals(lab2) == false)
								lab = null;
					} else {
						AmsLabel lab2 = lSet.getLabelUnderTime(rc.getExpStart());
						if (lab2 != null)
							if (lab.equals(lab2) == false)
								lab = null;
					}
				}
				String labelString = "";
				if (lab != null) {
					labelString += Integer.toString(labelnumber) + "\t";
					Map<String, String> att = lab.getAttributes();
					for (int j = 0; j < cats.size(); j++) {
						String val = att.get(cats.get(j));
						for (LabelValue lv : config.getConfig().get(cats.get(j))) {
							if (lv.getName().equals(val)) {
								labelString += Integer.toString(lv.getCode()) + "\t";
							}
						}
					}
				} else {
					labelString += Integer.toString(0) + "\t";
					int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
					for (int j = 0; j < cats.size(); j++)
						labelString += Integer.toString(misVal) + "\t";
				}
				line += labelString;
				pw.println(line);
				counter++;
			}
		} catch (IOException e) {

		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	public RespirationCycle getNextCycle(RespirationCycle cycle) {
		if (cycle == null)
			return null;
		NavigableSet<RespirationCycle> tailSet = cycles.tailSet(cycle, false);
		if (tailSet.isEmpty())
			return null;
		return tailSet.first();
	}

	public RespirationCycle getCycleAfterTime(double time) {
		return getNextCycle(new RespirationCycle(time));
	}

	public RespirationCycle getPrevCycle(RespirationCycle cycle) {
		if (cycle == null)
			return null;
		NavigableSet<RespirationCycle> headSet = cycles.headSet(cycle, false);
		if (headSet.isEmpty())
			return null;
		return headSet.last();
	}

	public RespirationCycle getCycleBeforeTime(double time) {
		return getPrevCycle(new RespirationCycle(time));
	}

	public void setSelectedSuspiciousRR(int sel) {
		curSelectedSuspiciousRR = sel;
	}

	public int numberOfSuspiciousCycles() {
		return beatsSortedByRRSuspicion.size();
	}
}
