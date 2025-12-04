package nl.vu.psy.ams.suite.data.structures;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.ExternalFile2Data;
import nl.vu.psy.ams.suite.data.ExternalFile3Data;
import nl.vu.psy.ams.suite.data.ExternalFileData;
import nl.vu.psy.ams.suite.data.TotalMotilityGenerator;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.freq.BeatSetToGridData;
import nl.vu.psy.ams.suite.data.freq.SmoothnessPriorMatrices;
import nl.vu.psy.ams.suite.data.posture.ActivityClassification;
import nl.vu.psy.ams.suite.data.posture.Posture;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.shared.array.RealArray;

/*
 * Holds all information about a label, such as start
 * time, end time and ICG scoring.
 * Also provides access to a lot of functions that
 * provide output data for a label.
 */
public class AmsLabel implements Comparable<AmsLabel> {

	public static class SavedICGPoint {
		private double bICGp, lICGp;
		private double cICGp, mICGp;
		private double xICGp, rICGp;
		private double bICGv, lICGv;
		private double cICGv, mICGv;
		private double xICGv, rICGv;
		private double pECGp, pECGv, pOnsetp, pOnsetv, qECGp, qECGv, qOnsetp, qOnsetv, rPoint, rv, sp, sv, sOffsetp,
				sOffsetv, tECGp, tECGv, tOffsetp, tOffsetv;
		private boolean missing;
		private String name;

		public double getbICGp() {
			return bICGp;
		}

		public double getbICGv() {
			return bICGv;
		}

		public double getcICGp() {
			return cICGp;
		}

		public double getcICGv() {
			return cICGv;
		}

		public double getxICGp() {
			return xICGp;
		}

		public double getxICGv() {
			return xICGv;
		}

		public String getName() {
			return name;
		}

		public double getpECGp() {
			return pECGp;
		}

		public double getpECGv() {
			return pECGv;
		}

		public double getpOnsetp() {
			return pOnsetp;
		}

		public double getpOnsetECGv() {
			return pOnsetv;
		}

		public double getqECGp() {
			return qECGp;
		}

		public double getqECGv() {
			return qECGv;
		}

		public double getqOnsetp() {
			return qOnsetp;
		}

		public double getqOnsetECGv() {
			return qOnsetv;
		}

		public double getRPoint() {
			return rPoint;
		}

		public double getRv() {
			return rv;
		}

		public double getSp() {
			return sp;
		}

		public double getSv() {
			return sv;
		}

		public double getSOffsetp() {
			return sOffsetp;
		}

		public double getsOffsetv() {
			return sOffsetv;
		}

		public double gettECGp() {
			return tECGp;
		}

		public double gettECGv() {
			return tECGv;
		}

		public double gettOffsetp() {
			return tOffsetp;
		}

		public double gettOffsetv() {
			return tOffsetv;
		}

		public boolean isMissing() {
			return missing;
		}

		public void setbICGp(double bICGp) {
			this.bICGp = bICGp;
		}

		public void setbICGv(double bICGv) {
			this.bICGv = bICGv;
		}

		public void setcICGp(double cICGp) {
			this.cICGp = cICGp;
		}

		public void setcICGv(double cICGv) {
			this.cICGv = cICGv;
		}

		public void setxICGp(double xICGp) {
			this.xICGp = xICGp;
		}

		public void setxICGv(double xICGv) {
			this.xICGv = xICGv;
		}

		public void setMissing(boolean missing) {
			this.missing = missing;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setpECGp(double pECGp) {
			this.pECGp = pECGp;
		}

		public void setpECGv(double pECGv) {
			this.pECGv = pECGv;
		}

		public void setpOnsetp(double pOnsetECGp) {
			this.pOnsetp = pOnsetECGp;
		}

		public void setpOnsetECGv(double pOnsetECGv) {
			this.pOnsetv = pOnsetECGv;
		}

		public void setqECGp(double qECGp) {
			this.qECGp = qECGp;
		}

		public void setqECGv(double qECGv) {
			this.qECGv = qECGv;
		}

		public void setqOnsetp(double qOnsetECGp) {
			this.qOnsetp = qOnsetECGp;
		}

		public void setqOnsetECGv(double qOnsetECGv) {
			this.qOnsetv = qOnsetECGv;
		}

		public void setRp(double Rp) {
			this.rPoint = Rp;
		}

		public void setRv(double RECGv) {
			this.rv = RECGv;
		}

		public void setSp(double Sp) {
			this.sp = Sp;
		}

		public void setSv(double sECGv) {
			this.sv = sECGv;
		}

		public void setSOffsetp(double SOffsetp) {
			this.sOffsetp = SOffsetp;
		}

		public void setsOffsetv(double sOffsetv) {
			this.sOffsetv = sOffsetv;
		}

		public void settECGp(double tECGp) {
			this.tECGp = tECGp;
		}

		public void settECGv(double tECGv) {
			this.tECGv = tECGv;
		}

		public void settOffsetp(double tOffsetp) {
			this.tOffsetp = tOffsetp;
		}

		public void settOffsetv(double tOffsetv) {
			this.tOffsetv = tOffsetv;
		}

	}

	public static AmsLabel generateECGArtefact(double lTime, double rTime, boolean state, double timewidth,
			String reason) {
		return new AmsLabel(lTime, rTime, state, timewidth, "ECGArtefact", reason);
	}

	public static AmsLabel generateSCLArtefact(double lTime, double rTime, boolean state, double timewidth,
			String reason) {
		return new AmsLabel(lTime, rTime, state, timewidth, "SCLArtefact", reason);
	}

	public static AmsLabel generatePostureLabel(double lTime, double rTime, String state) {
		return new AmsLabel(lTime, rTime, false, 0.0, "Posture", state);
	}

	public static AmsLabel generatePostureLabel(double lTime, double rTime, Posture posture) {
		return new AmsLabel(lTime, rTime, false, 0.0, "Posture", posture.name());
	}

	public static AmsLabel generateSpeechLabel(double lTime, double rTime, String state) {
		return new AmsLabel(lTime, rTime, false, 0.0, "Speech", state);
	}

	public static AmsLabel generateStairsLabel(double lTime, double rTime, String state) {
		return new AmsLabel(lTime, rTime, false, 0.0, "Stairs", state);
	}

	// public static AmsLabel generateMotLabel(double lTime, double rTime, String
	// state) {
	// return new AmsLabel(lTime, rTime, false, 0.0, "ActivityIntensity", state);
	// }

	private double leftTime, rightTime;
	private Map<String, String> attributes;
	private double bICGPoint = Double.NEGATIVE_INFINITY,
			cICGPoint = Double.NEGATIVE_INFINITY,
			xICGPoint = Double.NEGATIVE_INFINITY;

	private double cICGVal = Double.NEGATIVE_INFINITY;
	private double bICGVal = Double.NEGATIVE_INFINITY;
	private double xICGVal = Double.NEGATIVE_INFINITY;

	private double lICGPoint = Double.NEGATIVE_INFINITY,
			mICGPoint = Double.NEGATIVE_INFINITY,
			rICGPoint = Double.NEGATIVE_INFINITY;

	private double mICGVal = Double.NEGATIVE_INFINITY;
	private double lICGVal = Double.NEGATIVE_INFINITY;
	private double rICGVal = Double.NEGATIVE_INFINITY;

	private double[] bICGPoints, cICGPoints, xICGPoints;
	private double[] bICGVals, cICGVals, xICGVals;

	private boolean isTimeLabel = false;
	private double labeltimewidth = 0.0;

	private boolean ICGMissing = false;
	private boolean hasICGBeenScored = false;
	private LinkedList<SavedICGPoint> savedICGPoints = new LinkedList<AmsLabel.SavedICGPoint>();

	private double ECGPPoint = Double.NEGATIVE_INFINITY;
	private double ECGPPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGPOnsetPoint = Double.NEGATIVE_INFINITY;
	private double ECGPOnsetPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGQPoint = Double.NEGATIVE_INFINITY;
	private boolean ECGQOnsetPointmissing = false;
	private double ECGQPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGQOnsetPoint = Double.NEGATIVE_INFINITY;
	private boolean ECGQPointmissing = false;
	private double ECGQOnsetPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGRPoint = Double.NEGATIVE_INFINITY;

	private double ECGSPoint = Double.NEGATIVE_INFINITY;
	private double ECGSPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGSOffsetPoint = Double.NEGATIVE_INFINITY;
	private double ECGSOffsetPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGTPoint = Double.NEGATIVE_INFINITY;
	private double ECGTPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGTOffsetPoint = Double.NEGATIVE_INFINITY;
	private double ECGTOffsetPointAlgo = Double.NEGATIVE_INFINITY;

	private double ECGPVal = Double.NEGATIVE_INFINITY;
	private double ECGPValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGPOnsetVal = Double.NEGATIVE_INFINITY;
	private double ECGPOnsetValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGQVal = Double.NEGATIVE_INFINITY;
	private double ECGQValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGQOnsetVal = Double.NEGATIVE_INFINITY;
	private double ECGQOnsetValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGRVal = Double.NEGATIVE_INFINITY;

	private double ECGSVal = Double.NEGATIVE_INFINITY;
	private double ECGSValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGSOffsetVal = Double.NEGATIVE_INFINITY;
	private double ECGSOffsetValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGTVal = Double.NEGATIVE_INFINITY;
	private double ECGTValAlgo = Double.NEGATIVE_INFINITY;

	private double ECGTOffsetVal = Double.NEGATIVE_INFINITY;
	private double ECGTOffsetValAlgo = Double.NEGATIVE_INFINITY;

	private boolean ECGMissing = false;
	private boolean hasECGBeenScored = false;
	private boolean hasScoringBeenSaved = false;
	private boolean hasFilterScoringBeenSaved = false;
	private int labelnumber = 0, notInAv;
	private boolean algorithmscoring = true;

	public AmsLabel() {

	}

	public AmsLabel(double lTime, double rTime, boolean state, double timewidth, String... attributes) {
		this.attributes = new TreeMap<String, String>();
		for (int i = 0; i < attributes.length / 2; i++) {
			this.attributes.put(attributes[2 * i], attributes[2 * i + 1]);
		}
		this.leftTime = lTime;
		this.rightTime = rTime;
		setisTimeLabel(state);
		setLabelTimeWidth(timewidth);
	}

	public void clearICGScoring() {
		bICGPoint = Double.NEGATIVE_INFINITY;
		cICGPoint = Double.NEGATIVE_INFINITY;
		xICGPoint = Double.NEGATIVE_INFINITY;
		cICGVal = Double.NEGATIVE_INFINITY;
		bICGVal = Double.NEGATIVE_INFINITY;
		xICGVal = Double.NEGATIVE_INFINITY;
		ICGMissing = false;
		hasICGBeenScored = false;
		ECGPPoint = Double.NEGATIVE_INFINITY;
		ECGPOnsetPoint = Double.NEGATIVE_INFINITY;
		ECGQPoint = Double.NEGATIVE_INFINITY;
		ECGQOnsetPoint = Double.NEGATIVE_INFINITY;
		ECGRPoint = Double.NEGATIVE_INFINITY;
		ECGSPoint = Double.NEGATIVE_INFINITY;
		ECGSOffsetPoint = Double.NEGATIVE_INFINITY;
		ECGTPoint = Double.NEGATIVE_INFINITY;
		ECGTOffsetPoint = Double.NEGATIVE_INFINITY;
		ECGPVal = Double.NEGATIVE_INFINITY;
		ECGPOnsetVal = Double.NEGATIVE_INFINITY;
		ECGQVal = Double.NEGATIVE_INFINITY;
		ECGQOnsetVal = Double.NEGATIVE_INFINITY;
		ECGRVal = Double.NEGATIVE_INFINITY;
		ECGSVal = Double.NEGATIVE_INFINITY;
		ECGSOffsetVal = Double.NEGATIVE_INFINITY;
		ECGTVal = Double.NEGATIVE_INFINITY;
		ECGTOffsetVal = Double.NEGATIVE_INFINITY;
		ECGMissing = false;
		hasECGBeenScored = false;
		hasScoringBeenSaved = false;
		savedICGPoints.clear();
	}

	@Override
	public int compareTo(AmsLabel o) {
		int comp = Double.compare(getLeftTime(), o.getLeftTime());
		if (comp == 0) {
			return Double.compare(getRightTime(), o.getRightTime());
		}
		return comp;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public double getAverage(boolean inHR) {
		return CurrentOpenData.getInstance().getBeatSet().getAverageBetweenTimes(leftTime, rightTime, inHR);
	}

	public double getAverageRHeight() {
		return CurrentOpenData.getInstance().getBeatSet().getAverageHeightBetweenTimes(leftTime, rightTime);
	}

	public SignalPart[] getAverageSignalsUnderLabel(String chan, boolean filtered, ArrayList<Integer> removeIndices) {
		double tL = 256000 + getAverage(false) * 1000.;
		notInAv = 0;
		// ---------------- To add FilteredDZDT Signal---------------------------
		// if (chan.equals("DZDT")
		// &&
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERDZDTNew)
		// == 1) {
		// chan = "FILTDZDT";
		// }
		// if ((chan.equals("ECG") || chan.equals("V2ecg") || chan.equals("V3ecg"))
		// && AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERECGNew)
		// == 1) {
		// chan = "FILT" + chan;
		// }
		if (filtered) {
			chan = "FILT" + chan;
		}
		// ---------------------------------------------------------------------
		BinaryFile bf = new BinaryFile(chan);
		BinaryFile minBf = null;
		if (chan.equals("DZ")) {
			minBf = new BinaryFile("FILTDZ");
		}
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(
				CurrentOpenData.getInstance().getBeatSet().subSet(leftTime, rightTime));
		if (bts.isEmpty()) {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		SignalPart[] parts = new SignalPart[2];
		parts[0] = new SignalPart();
		parts[1] = new SignalPart();
		double[] times = new double[1000];
		int nLeft = bts.size();
		// boolean smallNoofBeats = nLeft < 1000;
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String currentfilename = Utils.removeExtension(datFile.getName());
		File retFile = new File(datFile.getParent(), currentfilename + "_label" + labelnumber + "_" + chan + ".csv"); // for
																														// output
																														// raw
																														// data
																														// per
																														// beat
		PrintWriter write = null;
		Long starttime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms() * 1000L;
		try {
			if ((chan.equals("ECG") || chan.equals("FILTECG") || chan.equals("DZDT") ||
					chan.equals("FILTDZDT"))
					&& AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
				write = new PrintWriter(new BufferedWriter(new FileWriter(retFile)));
			double averageECG = 0;
			if (chan.equals("ECG"))
				averageECG = getAverageECG();
			int chunk = 0;
			while (nLeft > 0) {
				int nToRead = 1000;
				if (nLeft < 1000) {
					nToRead = nLeft; // nRead - is the no of beats to read in the given time interval
				}

				for (int i = 0; i < nToRead; i++) {
					times[nToRead - i - 1] = bts.get(nLeft - 1).getRPeakTime() - 256000; // Get the rpeak time of every
																							// single beat and move 256
																							// milliseconds to the front
																							// of it
					if (times[nToRead - i - 1] < starttime)
						times[nToRead - i - 1] = starttime;
					nLeft--;
				}
				double[][] data = bf.getDataRuns(times, tL, nToRead); // startime, timelength, length
				double[][] mindata = null;
				if (minBf != null) {
					mindata = minBf.getDataRuns(times, tL, nToRead);
					for (int i = 0; i < nToRead; i++) {
						for (int j = 0; j < data[i].length; j++) {
							data[i][j] -= mindata[i][j];
						}
					}
				}
				if (chan.equals("ECG")) {
					for (int i = 0; i < nToRead; i++) {
						for (int j = 0; j < data[i].length; j++) {
							data[i][j] -= averageECG;
						}
					}
				}
				for (int i = 0; i < nToRead; i++) {
					parts[0].addToAverage(new SignalPart(data[i]));
					parts[1].addToAverage(new SignalPart(data[i]));
					if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.REMOVEICGBEATS) == 0
							&& write != null) { // for output raw data per beat
						String line = "K";
						for (int i2 = 0; i2 < data[i].length; i++) {
							String value = "\t";
							value += String.valueOf(data[i][i2]);
							line += value;
						}
						write.println(line);
					}
				}

				if (/* !smallNoofBeats && */ AppSettings.getInstance()
						.getIntPropertyOrToBeSaved(Settings.REMOVEICGBEATS) == 1) {
					if (chan.equals("FILTDZDT")) {
						double[] minMax = parts[0].getMinMax();
						double mmWidth = minMax[1] - minMax[0];
						double[] bounds = new double[2];
						bounds[0] = minMax[0] - mmWidth;
						bounds[1] = minMax[1] + mmWidth;
						String line = "";
						for (int i1 = 0; i1 < nToRead; i1++) {
							SignalPart pt = new SignalPart(data[i1]);
							double[] curMM = pt.getMinMax();
							if (curMM[0] < bounds[0] || curMM[1] > bounds[1]) {
								parts[1].removeFromAverage(pt);
								removeIndices.add(i1 + chunk * 1000);
								notInAv++;
								line += "R";
							} else if (curMM[1] - curMM[0] < mmWidth / 3.) {
								parts[1].removeFromAverage(pt);
								removeIndices.add(i1 + chunk * 1000);
								notInAv++;
								line += "R";
							} else
								line += "K";
							if (write != null) {
								for (int i = 0; i < data[i1].length; i++) {
									String value = "\t";
									value += String.valueOf(data[i1][i]);
									line += value;
								}
								write.println(line);
							}
							line = "";
						}
					} else {
						for (Integer index : removeIndices) {
							if (index < chunk * 1000)
								continue;
							if (index >= (chunk + 1) * 1000)
								break;
							SignalPart pt = new SignalPart(data[index - chunk * 1000]);
							parts[1].removeFromAverage(pt);
							notInAv++;
						}
					}
				}
				chunk++;
			}
			if (/* !smallNoofBeats && */ AppSettings.getInstance()
					.getIntPropertyOrToBeSaved(Settings.REMOVEICGBEATS) == 1)
				// System.out.println("Discarded beats " + chan + " " + labelnumber + " " +
				// notInAv + " " + bts.size());
				if (write != null) { // for output raw data average
					String line = "A";
					for (int i = 0; i < parts[1].getValues().length; i++) {
						String value = "\t";
						value += String.valueOf(parts[1].getValues()[i]);
						line += value;
					}
					write.println(line);
				}
		} catch (IOException e) { // for output raw data per beat
			e.printStackTrace();
		} finally {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (write != null) {
				write.close();
			}
		}
		return parts;

	}

	public SignalPart getAverageImprovedSignalUnderLabel(String chan, boolean filtered) {
		double tL = 256000 + getAverage(false) * 1000.;

		// ---------------- To add FilteredDZDT Signal---------------------------
		// if (chan.equals("DZDT")
		// &&
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERDZDTNew)
		// == 1) {
		// chan = "FILTDZDT";
		// }
		// if ((chan.equals("ECG") || chan.equals("V2ecg") || chan.equals("V3ecg"))
		// && AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERECGNew)
		// == 1) {
		// chan = "FILT" + chan;
		// }
		if (filtered) {
			chan = "FILT" + chan;
		}
		// ---------------------------------------------------------------------
		BinaryFile bf = new BinaryFile(chan);
		BinaryFile minBf = null;
		if (chan.equals("DZ")) {
			minBf = new BinaryFile("FILTDZ");
		}
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(
				CurrentOpenData.getInstance().getBeatSet().subSet(leftTime, rightTime));
		if (bts.isEmpty()) {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		SignalPart part = getAverageSignalUnderLabel(chan, filtered);
		if (part == null) {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.REMOVEICGBEATS) == 0) {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return part;
		}

		double[] minMax = part.getMinMax();
		double mmWidth = minMax[1] - minMax[0];

		double[] bounds = new double[2];
		bounds[0] = minMax[0] - mmWidth;
		bounds[1] = minMax[1] + mmWidth;

		double[] times = new double[1000];
		ECGBeat[] bts2 = new ECGBeat[1000];

		int nLeft = bts.size();
		while (nLeft > 0) {
			int nToRead = 1000;
			if (nLeft < 1000) {
				nToRead = nLeft;
			}
			for (int i1 = 0; i1 < nToRead; i1++) {
				times[nToRead - i1 - 1] = bts.get(nLeft - 1).getRPeakTime() - 256000;
				bts2[nToRead - i1 - 1] = bts.get(nLeft - 1);
				nLeft--;
			}
			double[][] data = bf.getDataRuns(times, tL, nToRead);
			double[][] mindata = null;
			if (minBf != null) {
				mindata = minBf.getDataRuns(times, tL, nToRead);
				for (int i = 0; i < nToRead; i++) {
					for (int j = 0; j < data[i].length; j++) {
						data[i][j] -= mindata[i][j];
					}
				}
			}
			for (int i1 = 0; i1 < nToRead; i1++) {
				SignalPart pt = new SignalPart(data[i1]);
				double[] curMM = pt.getMinMax();
				if (curMM[0] < bounds[0] || curMM[1] > bounds[1]) {
					part.removeFromAverage(pt);
				} else if (curMM[1] - curMM[0] < mmWidth / 3.) {
					part.removeFromAverage(pt);
				}
				if (part.getnumberOfComplexes() == 1)
					break;
			}
			if (part.getnumberOfComplexes() == 1)
				break;
		}

		try {
			bf.close();
			if (minBf != null)
				minBf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return part;
	}

	public SignalPart getAverageSignalUnderLabel(String chan, boolean filtered) {

		// ---------------- To add FilteredDZDT Signal---------------------------
		// if (chan.equals("DZDT")
		// &&
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERDZDTNew)
		// == 1) {
		// chan = "FILTDZDT";
		// }
		// if ((chan.equals("ECG") || chan.equals("V2ecg") || chan.equals("V3ecg"))
		// && AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERECGNew)
		// == 1) {
		// chan = "FILT" + chan;
		// }
		if (filtered) {
			chan = "FILT" + chan;
		}
		// ---------------------------------------------------------------------
		double tL = 256000 + getAverage(false) * 1000.;

		BinaryFile bf = new BinaryFile(chan);
		BinaryFile minBf = null;
		if (chan.equals("DZ")) {
			minBf = new BinaryFile("FILTDZ");
		}
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(
				CurrentOpenData.getInstance().getBeatSet().subSet(leftTime, rightTime));
		if (bts.isEmpty()) {
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		double[] times = new double[1000];
		int nLeft = bts.size();
		SignalPart part = new SignalPart();
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String currentfilename = Utils.removeExtension(datFile.getName());
		File retFile = new File(datFile.getParent(), currentfilename + "_label" + labelnumber + "_" + chan + ".csv"); // for
																														// output
																														// raw
																														// data
																														// per
																														// beat
		PrintWriter write = null;
		try {
			if ((chan.equals("ECG") || chan.equals("FILTECG") || chan.equals("DZDT") ||
					chan.equals("FILTDZDT"))
					&& AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
				write = new PrintWriter(new BufferedWriter(new FileWriter(retFile)));
			while (nLeft > 0) {
				int nToRead = 1000;
				if (nLeft < 1000) {
					nToRead = nLeft; // nRead - is the no of beats to read in the given time interval
				}

				for (int i = 0; i < nToRead; i++) {
					times[nToRead - i - 1] = bts.get(nLeft - 1).getRPeakTime() - 256000; // Get the rpeak time of
																							// every
																							// single beat and move
																							// 256
																							// milliseconds to the
																							// front
																							// of it
					nLeft--;
				}
				double[][] data = bf.getDataRuns(times, tL, nToRead); // startime, timelength, length
				double[][] mindata = null;
				if (minBf != null) {
					mindata = minBf.getDataRuns(times, tL, nToRead);
					for (int i = 0; i < nToRead; i++) {
						for (int j = 0; j < data[i].length; j++) {
							data[i][j] -= mindata[i][j];
						}
					}
				}
				for (int i = 0; i < nToRead; i++) {
					part.addToAverage(new SignalPart(data[i]));
				}
				if ((chan.equals("ECG") || chan.equals("FILTECG") || chan.equals("DZDT") || chan.equals("FILTDZDT"))
						&& write != null) { // for output raw data per beat
					String line = "";
					for (int j = 0; j < nToRead; j++) {
						for (int i = 0; i < data[j].length; i++) {
							String value = String.valueOf(data[j][i]);
							value += "\t";
							line += value;
						}
						write.println(line);
						line = "";
					}
				}
			}
		} catch (Exception e) { // for output raw data per beat
			e.printStackTrace();
		} finally {
			if (write != null) {
				write.close();
			}
			try {
				bf.close();
				if (minBf != null)
					minBf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return part;
	}

	public double getAverageZ0() {
		BinaryFile bf = new BinaryFile("Z0");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double getAverageECG() {
		BinaryFile bf = new BinaryFile("ECG");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double getStdDevECG() {
		BinaryFile bf = new BinaryFile("ECG");
		double av = bf.getStdDevBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double[] getMinMaxECG() {
		BinaryFile bf = new BinaryFile("ECG");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public double getStdDevZ0() {
		BinaryFile bf = new BinaryFile("Z0");
		double av = bf.getStdDevBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double[] getMinMaxZ0() {
		BinaryFile bf = new BinaryFile("Z0");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public double getAverageECG2() {
		BinaryFile bf = new BinaryFile("V2ecg");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double getStdDevECG2() {
		BinaryFile bf = new BinaryFile("V2ecg");
		double av = bf.getStdDevBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double[] getMinMaxECG2() {
		BinaryFile bf = new BinaryFile("V2ecg");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public double getAvMot() {
		BinaryFile bf = new BinaryFile("MYA");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1000. * av;
	}

	public double getAvMot(String string) {
		BinaryFile bf = new BinaryFile(string);
		// return bf.getMotilityAverage(leftTime, rightTime);
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double getAvSCL() {
		BinaryFile bf = new BinaryFile("FILTSCL");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double[] getMinMaxSCL() {
		BinaryFile bf = new BinaryFile("FILTSCL");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public double getSCLvalueatTime() {
		BinaryFile bf = new BinaryFile("FILTSCL");
		double val = bf.getDataAtTime(leftTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return val;
	}

	public double getAvStep() {
		BinaryFile bf = new BinaryFile("AccelVectorMag");
		double av = bf.getAverageBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return av;
	}

	public double[] getMinMaxStep() {
		BinaryFile bf = new BinaryFile("AccelVectorMag");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public boolean isTimeLabel() {
		return isTimeLabel;
	}

	public double getLabelTimeWidth() {
		return labeltimewidth;
	}

	public double getICGbPoint() {
		return bICGPoint;
	}

	public double getICGbVal() {
		return bICGVal;
	}

	public double getICGcPoint() {
		return cICGPoint;
	}

	public double getICGcVal() {
		return cICGVal;
	}

	public double getICGxPoint() {
		return xICGPoint;
	}

	public double getICGxVal() {
		return xICGVal;
	}

	public double getECGPPoint() {
		return ECGPPoint;
	}

	public double[] getICGbPoints() {
		return bICGPoints;
	}

	public double[] getICGbVals() {
		return bICGVals;
	}

	public double[] getICGcPoints() {
		return cICGPoints;
	}

	public double[] getICGcVals() {
		return cICGVals;
	}

	public double[] getICGxPoints() {
		return xICGPoints;
	}

	public double[] getICGxVals() {
		return xICGVals;
	}

	public double getECGPPointAlgo() {
		return ECGPPointAlgo;
	}

	public double getECGPOnsetPoint() {
		return ECGPOnsetPoint;
	}

	public double getECGPOnsetPointAlgo() {
		return ECGPOnsetPointAlgo;
	}

	public double getECGQPoint() {
		return ECGQPoint;
	}

	public double getECGQPointAlgo() {
		return ECGQPointAlgo;
	}

	public double getECGQOnsetPoint() {
		return ECGQOnsetPoint;
	}

	public double getECGQOnsetPointAlgo() {
		return ECGQOnsetPointAlgo;
	}

	public double getECGRPoint() {
		return ECGRPoint;
	}

	public double getECGSPoint() {
		return ECGSPoint;
	}

	public double getECGSPointAlgo() {
		return ECGSPointAlgo;
	}

	public double getECGSOffsetPoint() {
		return ECGSOffsetPoint;
	}

	public double getECGSOffsetPointAlgo() {
		return ECGSOffsetPointAlgo;
	}

	public double getECGTPoint() {
		return ECGTPoint;
	}

	public double getECGTPointAlgo() {
		return ECGTPointAlgo;
	}

	public double getECGTOffsetPoint() {
		return ECGTOffsetPoint;
	}

	public double getECGTOffsetPointAlgo() {
		return ECGTOffsetPointAlgo;
	}

	public double getECGPVal() {
		return ECGPVal;
	}

	public double getECGPValAlgo() {
		return ECGPValAlgo;
	}

	public double getECGPOnsetVal() {
		return ECGPOnsetVal;
	}

	public double getECGPOnsetValAlgo() {
		return ECGPOnsetValAlgo;
	}

	public double getECGQVal() {
		return ECGQVal;
	}

	public double getECGQValAlgo() {
		return ECGQValAlgo;
	}

	public double getECGQOnsetVal() {
		return ECGQOnsetVal;
	}

	public double getECGQOnsetValAlgo() {
		return ECGQOnsetValAlgo;
	}

	public double getECGRVal() {
		return ECGRVal;
	}

	public double getECGSVal() {
		return ECGSVal;
	}

	public double getECGSValAlgo() {
		return ECGSValAlgo;
	}

	public double getECGSOffsetVal() {
		return ECGSOffsetVal;
	}

	public double getECGSOffsetValAlgo() {
		return ECGSOffsetValAlgo;
	}

	public double getECGTVal() {
		return ECGTVal;
	}

	public double getECGTValAlgo() {
		return ECGTValAlgo;
	}

	public double getECGTOffsetVal() {
		return ECGTOffsetVal;
	}

	public double getECGTOffsetValAlgo() {
		return ECGTOffsetValAlgo;
	}

	public double getEE2() {
		double[] data = TotalMotilityGenerator.getEE2Data();
		if (data == null)
			return -1;
		double curTime = CurrentOpenData.getInstance().getStartTimeInUS() + 30000000.;
		int nVals = data.length;
		double av = 0;
		int nInAv = 0;
		for (int i = 0; i < nVals; i++) {
			if (curTime > leftTime && curTime < rightTime) {
				av += data[i];
				nInAv++;
			}
			curTime += 60000000.;
		}
		if (nInAv < 1)
			return -1;
		return av / nInAv;
	}

	public Double getExternalFileAverage() {
		return ExternalFileData.getAverageBetweenTimes(leftTime, rightTime);
	}

	public Double getExternalFile2Average() {
		return ExternalFile2Data.getAverageBetweenTimes(leftTime, rightTime);
	}

	public Double getExternalFile3Average() {
		return ExternalFile3Data.getAverageBetweenTimes(leftTime, rightTime);
	}

	public double[] getFreqPowers(boolean show) {
		return getFreqPowers(show, 1024);
	}

	public double[] getFreqPowers(boolean show, int nSamples) {
		/*
		 * Steps:
		 *
		 * 1. Construct cubic spline interpolated IBI series that is re-sampled to a
		 * regular size grid
		 *
		 *
		 *
		 */

		double[] gridData = BeatSetToGridData.getSmoothedGridDataUnpadded(leftTime, rightTime); // After cubic spline
																								// interpolation

		if (gridData == null)
			return null;

		double sTime = 0.25;
		double[] ret = null;
		int n = nSamples;

		if (gridData.length < n)
			return null;

		int nAv = (int) Math.ceil(2. * gridData.length / n) - 1;
		int nHighPoints = (nAv + 1) * n / 2;
		int nDiff = nHighPoints - gridData.length;
		double diffPerBlock = (double) nDiff / (nAv - 1);

		/*
		 * The FFT size (no of samples in the fft) is a power of 2, defines the number
		 * of bins used for dividing the window into equal strips, or bins.
		 * Hence, a bin is a spectrum sample, and defines the frequency resolution of
		 * the window.
		 * 
		 * N (Bins) = FFT Size/2
		 * 
		 * FR = Fmax/N(Bins)
		 * 
		 * For a 1000 sampling rate, we have a 500 Hz band. With a 720 FFT size, we
		 * divide this band into 360 bins.
		 * Frequency Resolution = 1000/720 = 1.38Hz per bin
		 * 
		 */

		int numUniqPts = (int) Math.ceil((n + 1) / 2);
		Matrix invT = SmoothnessPriorMatrices.getInstance().getSmoothnessPriorMatrix(n);
		ArtefactSet afSet = CurrentOpenData.getInstance().getECGArtefacts();
		double sFreq = 1. / (sTime * n); // 0.00555
		double[] avPer = new double[n];
		int nAvFound = 0;

		for (int i = 0; i < n; i++)
			avPer[i] = 0;

		for (int q = 0; q < nAv; q++) {
			int start = (int) (q * n / 2 - Math.ceil(q * diffPerBlock));
			int end = start + n;
			double startTime = leftTime + start * 250000;
			double endTime = startTime + n * 250000;

			if (afSet.isLongArtefactBetweenTimes(startTime, endTime)) {
				continue;
			}
			nAvFound++;

			double[] data = new double[n];

			for (int i = start; i < end; i++) {
				data[i - start] = gridData[i];
			}

			if (n > 10) {
				double relX, welchWin;

				Vector x = new DenseVector(data);

				DenseVector y = new DenseVector(data.length);

				invT.mult(x, y);

				data = y.getData();

				// double avWin = 0;
				double sqrAvWin = 0;
				for (int i = 0; i < n; i++) {
					relX = (i + 0.5) / n;
					welchWin = 1 - (2 * relX - 1) * (2 * relX - 1);
					// avWin += welchWin / n;
					sqrAvWin += (welchWin * welchWin) / n;
					data[i] *= welchWin;
				}

				RealArray ra = new RealArray(data);

				RealArray fftxar = ra.tocRe().fft().torAbs();

				double[] test = fftxar.values();

				// Complex[] fft = ffter.transform(data);

				double[] fftx = new double[numUniqPts];
				for (int i = 0; i < numUniqPts; i++) {
					// fftx[i] = fft[i].abs();
					fftx[i] = test[i];
				}
				for (int i = 0; i < numUniqPts; i++) {
					fftx[i] /= data.length;
				}
				for (int i = 0; i < numUniqPts; i++) {
					fftx[i] *= fftx[i];
				}
				for (int i = 1; i < numUniqPts - 1; i++) {
					fftx[i] *= 2;
				}
				for (int i = 1; i < numUniqPts - 1; i++) {
					fftx[i] /= sqrAvWin;
				}
				for (int i = 0; i < numUniqPts; i++) {
					avPer[i] += fftx[i];
				}

			}
		}
		if (nAvFound == 0)
			return null;
		for (int i = 0; i < numUniqPts; i++) {
			avPer[i] /= nAvFound;
		}

		ret = new double[2];
		ret[0] = 0;
		ret[1] = 0;

		if (show) {
			for (int i = 0; i < numUniqPts; i++) {
				System.out.println(i * sFreq + " " + avPer[i]);
			}
		}
		AppSettings set = AppSettings.getInstance();
		double lflB = set.getIntProperty(Settings.LFLB) / 1000.;
		double lfuB = set.getIntProperty(Settings.LFUB) / 1000.;
		double hflB = set.getIntProperty(Settings.HFLB) / 1000.;
		double hfuB = set.getIntProperty(Settings.HFUB) / 1000.;
		for (int i = 0; i < numUniqPts - 1; i++) {
			double freqP1 = i * sFreq;
			double freqP2 = (i + 1) * sFreq;
			double v1 = avPer[i] / sFreq;
			double v2 = avPer[i + 1] / sFreq;
			if (freqP1 > lflB && freqP1 < lfuB) {
				if (freqP2 > lflB && freqP2 < lfuB) {
					ret[0] += sFreq * (v1 + v2) / 2;
				} else if (freqP2 > lfuB && freqP2 > lflB) {
					double pos = lfuB;
					double slope = (v2 - v1) / sFreq;
					double intercept = v1 - slope * freqP1;
					double valAtPos = intercept + slope * pos;
					ret[0] += (pos - freqP1) * (v1 + valAtPos) / 2;
				}
			}
			if (freqP2 > lflB && freqP2 < lfuB) {
				if (freqP1 < lfuB && freqP1 < lflB) {
					double pos = lflB;
					double slope = (v2 - v1) / sFreq;
					double intercept = v1 - slope * freqP1;
					double valAtPos = intercept + slope * pos;
					ret[0] += (freqP2 - pos) * (v2 + valAtPos) / 2;
				}
			}
			if (freqP1 < lflB && freqP1 < lfuB && freqP2 > lflB && freqP2 > lfuB) {
				double slope = (v2 - v1) / sFreq;
				double intercept = v1 - slope * freqP1;
				double pos1 = lflB;
				double valAtPos1 = intercept + slope * pos1;
				double pos2 = lfuB;
				double valAtPos2 = intercept + slope * pos2;
				ret[0] += (pos2 - pos1) * (valAtPos1 + valAtPos2) / 2;
			}
			/*
			 * if ((i) * sFreq > lflB && (i) * sFreq < lfuB) { ret[0] +=
			 * avPer[i]; } if ((i) * sFreq > hflB && (i) * sFreq < hfuB) {
			 * ret[1] += avPer[i]; }
			 */
		}
		for (int i = 0; i < numUniqPts - 1; i++) {
			double freqP1 = i * sFreq;
			double freqP2 = (i + 1) * sFreq;
			double v1 = avPer[i] / sFreq;
			double v2 = avPer[i + 1] / sFreq;
			if (freqP1 > hflB && freqP1 < hfuB) {
				if (freqP2 > hflB && freqP2 < hfuB) {
					ret[1] += sFreq * (v1 + v2) / 2;
				} else if (freqP2 > hfuB && freqP2 > hflB) {
					double pos = hfuB;
					double slope = (v2 - v1) / sFreq;
					double intercept = v1 - slope * freqP1;
					double valAtPos = intercept + slope * pos;
					ret[1] += (pos - freqP1) * (v1 + valAtPos) / 2;
				}
			}
			if (freqP2 > hflB && freqP2 < hfuB) {
				if (freqP1 < hfuB && freqP1 < hflB) {
					double pos = hflB;
					double slope = (v2 - v1) / sFreq;
					double intercept = v1 - slope * freqP1;
					double valAtPos = intercept + slope * pos;
					ret[1] += (freqP2 - pos) * (v2 + valAtPos) / 2;
				}
			}
			if (freqP1 < hflB && freqP1 < hfuB && freqP2 > hflB && freqP2 > hfuB) {
				double slope = (v2 - v1) / sFreq;
				double intercept = v1 - slope * freqP1;
				double pos1 = hflB;
				double valAtPos1 = intercept + slope * pos1;
				double pos2 = hfuB;
				double valAtPos2 = intercept + slope * pos2;
				ret[1] += (pos2 - pos1) * (valAtPos1 + valAtPos2) / 2;
			}
			/*
			 * if ((i) * sFreq > hflB && (i) * sFreq < hfuB) { ret[0] +=
			 * avPer[i]; } if ((i) * sFreq > hflB && (i) * sFreq < hfuB) {
			 * ret[1] += avPer[i]; }
			 */
		}
		ret[0] *= 1000000;
		ret[1] *= 1000000;

		return ret;
	}

	public double getLeftTime() {
		return leftTime;
	}

	public double getRightTime() {
		return rightTime;
	}

	public double getMax(boolean inHR) {
		return CurrentOpenData.getInstance().getBeatSet().getMaxBetweenTimes(leftTime, rightTime, inHR);
	}

	public double getMin(boolean inHR) {
		return CurrentOpenData.getInstance().getBeatSet().getMinBetweenTimes(leftTime, rightTime, inHR);
	}

	public double[] getMinMaxMot() {
		BinaryFile bf = new BinaryFile("MYA");
		double[] mm = bf.getMinMaxBetweenTimes(leftTime, rightTime);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mm;
	}

	public int getNumberOfBeats() {
		return CurrentOpenData.getInstance().getBeatSet().subSet(leftTime, rightTime).size();
	}

	public int getNumberOfIBIs() {
		return CurrentOpenData.getInstance().getBeatSet().getNumberOfIBIs(leftTime, rightTime);
	}

	public int getNumberOfSavedICGPoints() {
		return savedICGPoints.size();
	}

	public Double getRespirationRate() {
		return CurrentOpenData.getInstance().getRespSet().getRespirationRateBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getRRVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxRR(getLeftTime(), getRightTime());
	}

	public Double getArtefactFreeLength() {
		return CurrentOpenData.getInstance().getRespSet().getLengthNotUnderArtefacts(getLeftTime(), getRightTime());
	}

	public double getRMSSD() {
		return CurrentOpenData.getInstance().getBeatSet().getRMSSDBetweenTimes(leftTime, rightTime);
	}

	public Double getRSA() {
		return CurrentOpenData.getInstance().getRespSet().getRSABetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getRSAVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxRSA(getLeftTime(), getRightTime());
	}

	public Double getRSA0() {
		return CurrentOpenData.getInstance().getRespSet().getRSA0BetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getRSA0Vals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxRSA0(getLeftTime(), getRightTime());
	}

	public Double getRSAAdded() {
		return CurrentOpenData.getInstance().getRespSet().getRSAAddedBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getRSAAddedVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxRSAAdded(getLeftTime(), getRightTime());
	}

	public Double getRSA0Added() {
		return CurrentOpenData.getInstance().getRespSet().getRSA0AddedBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getRSA0AddedVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxRSA0Added(getLeftTime(), getRightTime());
	}

	public Double getInsp() {
		return CurrentOpenData.getInstance().getRespSet().getInspDurationBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getInspVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxInsp(getLeftTime(), getRightTime());
	}

	public Double getExp() {
		return CurrentOpenData.getInstance().getRespSet().getExpDurationBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getExpVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxExp(getLeftTime(), getRightTime());
	}

	public Double getInspExp() {
		return CurrentOpenData.getInstance().getRespSet().getInspExpBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getInspExpVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxInspExp(getLeftTime(), getRightTime());
	}

	public Double getLongest() {
		return CurrentOpenData.getInstance().getRespSet().getLongestIBIBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getLongestVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxLongest(getLeftTime(), getRightTime());
	}

	public Double getShortest() {
		return CurrentOpenData.getInstance().getRespSet().getShortestIBIBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getShortestVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxShortest(getLeftTime(), getRightTime());
	}

	public SavedICGPoint getSavedIGGPoint(int index) {
		return savedICGPoints.get(index);
	}

	public double getStddev(boolean inHR) {
		return CurrentOpenData.getInstance().getBeatSet().getStddevBetweenTimes(leftTime, rightTime, inHR);
	}

	public double getStddevRHeight() {
		return CurrentOpenData.getInstance().getBeatSet().getStddevHeightBetweenTimes(leftTime, rightTime);
	}

	public Double getTidalVolume() {
		return CurrentOpenData.getInstance().getRespSet().getTidalVolumeBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getTidalVolumeVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxTidalVolume(getLeftTime(), getRightTime());
	}

	public Double getTidalVolumeRaw() {
		return CurrentOpenData.getInstance().getRespSet().getTidalVolumeRawBetweenTimes(getLeftTime(), getRightTime());
	}

	public double[] getTidalVolumeRawVals() {
		return CurrentOpenData.getInstance().getRespSet().getStddevMinMaxTidalVolumeRaw(getLeftTime(), getRightTime());
	}

	public double getPercentRejected() {
		return CurrentOpenData.getInstance().getRespSet().getPercentRejected(getLeftTime(), getRightTime());
	}

	public double getTotalMotility() {
		double[] data = TotalMotilityGenerator.getTotalMotility();
		if (data == null)
			return -1;
		double curTime = CurrentOpenData.getInstance().getStartTimeInUS() + 500000.;
		int nVals = data.length;
		double av = 0;
		int nInAv = 0;
		for (int i = 0; i < nVals; i++) {
			if (curTime > leftTime && curTime < rightTime) {
				av += data[i];
				nInAv++;
			}
			curTime += 1000000.;
		}
		if (nInAv < 1)
			return -1;
		return av / nInAv;
	}

	public double getTotalTimeUnderLabel() {
		double totalTime = 0;
		double time = getLeftTime();
		for (AmsLabel art : CurrentOpenData.getInstance().getECGArtefacts().getLabels()) {
			if (time > getRightTime())
				break;
			if (art.getLeftTime() > getRightTime())
				break;
			if (art.getRightTime() > time) {
				double addedTime = -1;
				if (art.getLeftTime() < getRightTime()) {
					addedTime = art.getLeftTime() - time;
				} else {
					addedTime = getRightTime() - time;
				}
				if (addedTime > 0) {
					totalTime += addedTime;
				}
				time = art.getRightTime();
			}
		}
		double addedTime = getRightTime() - time;
		if (addedTime > 0)
			totalTime += addedTime;
		return totalTime;
	}

	public double getTotalTimeUnderLabelSCL() {
		double totalTime = 0;
		double time = getLeftTime();
		for (AmsLabel art : CurrentOpenData.getInstance().getSCLArtefacts().getLabels()) {
			if (time > getRightTime())
				break;
			if (art.getLeftTime() > getRightTime())
				break;
			if (art.getRightTime() > time) {
				double addedTime = -1;
				if (art.getLeftTime() < getRightTime()) {
					addedTime = art.getLeftTime() - time;
				} else {
					addedTime = getRightTime() - time;
				}
				if (addedTime > 0) {
					totalTime += addedTime;
				}
				time = art.getRightTime();
			}
		}
		double addedTime = getRightTime() - time;
		if (addedTime > 0)
			totalTime += addedTime;
		return totalTime;
	}

	public int getSCLCycleCount() {
		double[] sclcycles = CurrentOpenData.getInstance().getSCLSet().getDataBetweenTimes(getLeftTime(),
				getRightTime());
		if (sclcycles == null)
			return 0;
		return sclcycles.length;
	}

	public int getStepCount() {
		double[] steps = CurrentOpenData.getInstance().getStepSet().getDataBetweenTimes(getLeftTime(), getRightTime());
		if (steps == null)
			return 0;
		return steps.length;
	}

	public Double getAverageMETBrageN() {
		return ActivityClassification.getAverageMET(leftTime, rightTime);
	}

	public Double getAverageMETBrageB() {
		return ActivityClassification.getAverageMET(leftTime, rightTime, 4);
	}

	public Double getAverageMETFreedson() {
		return ActivityClassification.getAverageMET(leftTime, rightTime, 5);
	}

	public Double getAverageMADxyz() {
		return ActivityClassification.getAverageMET(leftTime, rightTime, 1);
	}

	public Double getAverageMAD() {
		return ActivityClassification.getAverageMET(leftTime, rightTime, 2);
	}

	public Double getAverageSpeech() {
		return ActivityClassification.getAverageMET(leftTime, rightTime, 3);
	}

	public String getPosture() {
		return ActivityClassification.getPosture(leftTime, rightTime);
	}

	public int getNoofPrematureVentricularContractions() {
		int noofbeats = 0;
		double time = getLeftTime();
		for (AmsLabel art : CurrentOpenData.getInstance().getECGArtefacts().getLabels()) {
			if (time > getRightTime())
				break;
			if (art.getLeftTime() > getRightTime())
				break;
			if ((art.getLeftTime() >= getLeftTime()) && (art.getRightTime() <= getRightTime())) {
				if (art.getAttributes().toString().contains("Premature Ventricular Contraction")) {
					noofbeats += 1;
				}
			}
		}
		return noofbeats;
	}

	public int getNoofPrematureAtrialContractions() {
		int noofbeats = 0;
		double time = getLeftTime();
		for (AmsLabel art : CurrentOpenData.getInstance().getECGArtefacts().getLabels()) {
			if (time > getRightTime())
				break;
			if (art.getLeftTime() > getRightTime())
				break;
			if ((art.getLeftTime() >= getLeftTime()) && (art.getRightTime() <= getRightTime())) {
				if (art.getAttributes().toString().contains("Premature Atrial Contraction")) {
					noofbeats += 1;
				}
			}
		}
		return noofbeats;
	}

	public double getPEP() {
		double pep = -9999;
		if (isICGMissing() == false) {
			if (getICGbPoint() != Double.NEGATIVE_INFINITY
					&& getECGQOnsetPoint() != Double.NEGATIVE_INFINITY) {
				if (isECGQOnsetMissing() == false) { // Q-onset is present
					pep = (getICGbPoint() - getECGQPoint()) / 1000;
				} else { // Q-onset is missing
					if (isECGQPointMissing() == false) { // Q Point present
						pep = ((getICGbPoint() - getECGQOnsetPoint()) / 1000) + 12;
					} else {
						pep = ((getICGbPoint() - getECGRPoint()) / 1000) + 41;
					}
				}
			}
		}
		return pep;
	}

	public double getLVET() {
		double lvet = getICGxPoint() - getICGbPoint();
		if (getICGxPoint() == Double.NEGATIVE_INFINITY || getICGbPoint() == Double.NEGATIVE_INFINITY
				|| isICGMissing()) {
			lvet = -9999;
		} else {
			lvet /= 1000;
		}
		return lvet;
	}

	public synchronized boolean hasECGBeenScored() {
		return hasECGBeenScored;
	}

	public synchronized boolean hasICGBeenScored() {
		return hasICGBeenScored;
	}

	public synchronized boolean hasScoringBeenSaved() {
		return hasScoringBeenSaved;
	}

	public synchronized boolean hasFilterScoringBeenSaved() {
		return hasFilterScoringBeenSaved;
	}

	public boolean isECGMissing() {
		return ECGMissing;
	}

	public boolean isICGMissing() {
		return ICGMissing;
	}

	public boolean isECGQOnsetMissing() {
		return ECGQOnsetPointmissing;
	}

	public boolean isECGQPointMissing() {
		return ECGQPointmissing;
	}

	public void resetICGScoring() {
		setICGbPoint(Double.NEGATIVE_INFINITY);
		setICGcPoint(Double.NEGATIVE_INFINITY);
		setICGxPoint(Double.POSITIVE_INFINITY);
		setICGbPoint(80000);
		setICGcPoint(100000);
		setICGxPoint(350000);
	}

	public void restoreICGScoring(String ret) {
		for (SavedICGPoint sip : savedICGPoints) {

			if (sip.name.equals(ret)) {

				ICGMissing = sip.missing;
				bICGPoint = sip.bICGp;
				bICGVal = sip.bICGv;
				cICGPoint = sip.cICGp;
				cICGVal = sip.cICGv;
				xICGPoint = sip.xICGp;
				xICGVal = sip.xICGv;

				ECGPPoint = sip.pECGp;
				ECGPOnsetPoint = sip.pOnsetp;
				ECGQPoint = sip.qECGp;
				ECGQOnsetPoint = sip.qOnsetp;
				ECGRPoint = sip.rPoint;
				ECGSPoint = sip.sp;
				ECGSOffsetPoint = sip.sOffsetp;
				ECGTPoint = sip.tECGp;
				ECGTOffsetPoint = sip.tOffsetp;

				ECGPVal = sip.pECGv;
				ECGPOnsetVal = sip.pOnsetv;
				ECGQVal = sip.qECGv;
				ECGQOnsetVal = sip.qOnsetv;
				ECGRVal = sip.rv;
				ECGSVal = sip.sv;
				ECGSOffsetVal = sip.sOffsetv;
				ECGTVal = sip.tECGv;
				ECGTOffsetVal = sip.tOffsetv;
			}
		}
	}

	public void resettoAlgo() {
		setICGbPoint(Double.NEGATIVE_INFINITY);
		setICGcPoint(Double.NEGATIVE_INFINITY);
		setICGxPoint(Double.NEGATIVE_INFINITY);

		setECGpPoint(Double.NEGATIVE_INFINITY);
		setECGpPointAlgo(Double.NEGATIVE_INFINITY);

		setECGpOnsetPoint(Double.NEGATIVE_INFINITY);
		setECGpOnsetPointAlgo(Double.NEGATIVE_INFINITY);

		setECGqPoint(Double.NEGATIVE_INFINITY);
		setECGqPointAlgo(Double.NEGATIVE_INFINITY);

		setECGqOnsetPoint(Double.NEGATIVE_INFINITY);
		setECGqOnsetPointAlgo(Double.NEGATIVE_INFINITY);

		setECGSPoint(Double.NEGATIVE_INFINITY);
		setECGSPointAlgo(Double.NEGATIVE_INFINITY);

		setECGSOffsetPoint(Double.NEGATIVE_INFINITY);
		setECGSOffsetPointAlgo(Double.NEGATIVE_INFINITY);

		setECGTpoint(Double.NEGATIVE_INFINITY);
		setECGTpointAlgo(Double.NEGATIVE_INFINITY);

		setECGTOffsetpoint(Double.NEGATIVE_INFINITY);
		setECGTOffsetpointAlgo(Double.NEGATIVE_INFINITY);
	}

	public boolean savedICGNameExists(String name) {
		for (SavedICGPoint sip : savedICGPoints) {
			if (sip.getName().equals(name))
				return true;
		}
		return false;
	}

	public void saveICGScoring(String name) {

		SavedICGPoint sip = new SavedICGPoint();
		sip.setName(name);
		sip.setMissing(ICGMissing);

		sip.setbICGp(bICGPoint);
		sip.setcICGp(cICGPoint);
		sip.setxICGp(xICGPoint);
		sip.setbICGv(bICGVal);
		sip.setcICGv(cICGVal);
		sip.setxICGv(xICGVal);

		sip.setpECGp(ECGPPoint);
		sip.setpOnsetp(ECGPOnsetPoint);
		sip.setqECGp(ECGQPoint);
		sip.setqOnsetp(ECGQOnsetPoint);
		sip.setRp(ECGRPoint);
		sip.setSp(ECGSPoint);
		sip.setSOffsetp(ECGSOffsetPoint);
		sip.settECGp(ECGTPoint);
		sip.settOffsetp(ECGTOffsetPoint);

		sip.setpECGv(ECGPVal);
		sip.setpOnsetECGv(ECGPOnsetVal);
		sip.setqECGv(ECGQVal);
		sip.setqOnsetECGv(ECGQOnsetVal);
		sip.setRv(ECGRVal);
		sip.setSv(ECGSVal);
		sip.setsOffsetv(ECGSOffsetVal);
		sip.settECGv(ECGTVal);
		sip.settOffsetv(ECGTOffsetVal);

		savedICGPoints.add(sip);

	}

	public synchronized void setECGBeenScored(boolean hasBeenScored) {
		this.hasECGBeenScored = hasBeenScored;
	}

	public void setisTimeLabel(boolean state) {
		isTimeLabel = state;
	}

	public void setLabelTimeWidth(double timewidth) {
		labeltimewidth = timewidth;
	}

	public void setECGpPoint(double time) {
		ECGPPoint = time;
	}

	public void setECGpPointAlgo(double time) {
		ECGPPointAlgo = time;
	}

	public void setECGpOnsetPoint(double time) {
		ECGPOnsetPoint = time;
	}

	public void setECGpOnsetPointAlgo(double time) {
		ECGPOnsetPointAlgo = time;
	}

	public void setECGqPoint(double time) {
		ECGQPoint = time;
	}

	public void setECGqPointAlgo(double time) {
		ECGQPointAlgo = time;
	}

	public void setECGqOnsetPoint(double time) {
		ECGQOnsetPoint = time;
	}

	public void setECGqOnsetPointAlgo(double time) {
		ECGQOnsetPointAlgo = time;
	}

	public void setECGRPoint(double time) {
		ECGRPoint = time;
	}

	public void setECGSPoint(double time) {
		ECGSPoint = time;
	}

	public void setECGSPointAlgo(double time) {
		ECGSPointAlgo = time;
	}

	public void setECGSOffsetPoint(double time) {
		ECGSOffsetPoint = time;
	}

	public void setECGSOffsetPointAlgo(double time) {
		ECGSOffsetPointAlgo = time;
	}

	public void setECGTpoint(double time) {
		ECGTPoint = time;
	}

	public void setECGTpointAlgo(double time) {
		ECGTPointAlgo = time;
	}

	public void setECGTOffsetpoint(double time) {
		ECGTOffsetPoint = time;
	}

	public void setECGTOffsetpointAlgo(double time) {
		ECGTOffsetPointAlgo = time;
	}

	public void setECGPVal(double rVal) {
		ECGPVal = rVal;
	}

	public void setECGPValAlgo(double rVal) {
		ECGPValAlgo = rVal;
	}

	public void setECGPOnsetVal(double rVal) {
		ECGPOnsetVal = rVal;
	}

	public void setECGPOnsetValAlgo(double rVal) {
		ECGPOnsetValAlgo = rVal;
	}

	public void setECGQVal(double rVal) {
		ECGQVal = rVal;
	}

	public void setECGQValAlgo(double rVal) {
		ECGQValAlgo = rVal;
	}

	public void setECGQOnsetVal(double rVal) {
		ECGQOnsetVal = rVal;
	}

	public void setECGQOnsetValAlgo(double rVal) {
		ECGQOnsetValAlgo = rVal;
	}

	public void setECGRVal(double rVal) {
		ECGRVal = rVal;
	}

	public void setECGSVal(double rVal) {
		ECGSVal = rVal;
	}

	public void setECGSValAlgo(double rVal) {
		ECGSValAlgo = rVal;
	}

	public void setECGSOffsetVal(double rVal) {
		ECGSOffsetVal = rVal;
	}

	public void setECGSOffsetValAlgo(double rVal) {
		ECGSOffsetValAlgo = rVal;
	}

	public void setECGTVal(double tVal) {
		ECGTVal = tVal;
	}

	public void setECGTValAlgo(double tVal) {
		ECGTValAlgo = tVal;
	}

	public void setECGTOffsetVal(double tVal) {
		ECGTOffsetVal = tVal;
	}

	public void setECGTOffsetValAlgo(double tVal) {
		ECGTOffsetValAlgo = tVal;
	}

	public void setalgorithmscoring(boolean val) {
		this.algorithmscoring = val;
	}

	public boolean isalgorithmscoring() {
		return algorithmscoring;
	}

	public synchronized void setICGBeenScored(boolean hasBeenScored) {
		this.hasICGBeenScored = hasBeenScored;
	}

	public void setICGbPoint(double bPoint) {
		if (bPoint < cICGPoint || cICGPoint == Double.NEGATIVE_INFINITY) {
			if (bPoint < xICGPoint || xICGPoint == Double.NEGATIVE_INFINITY) {
				this.bICGPoint = bPoint;
			}
		}
	}

	public void setICGbVal(double bVal) {
		bICGVal = bVal;
	}

	public void setICGcPoint(double cPoint) {
		if (cPoint > bICGPoint || bICGPoint == Double.NEGATIVE_INFINITY) {
			if (cPoint < xICGPoint || xICGPoint == Double.NEGATIVE_INFINITY) {
				this.cICGPoint = cPoint;
			}
		}
	}

	public void setICGcVal(double cVal) {
		cICGVal = cVal;
	}

	public void setICGxPoint(double xPoint) {
		if (xPoint > bICGPoint || bICGPoint == Double.NEGATIVE_INFINITY) {
			if (xPoint > cICGPoint || cICGPoint == Double.NEGATIVE_INFINITY) {
				this.xICGPoint = xPoint;
			}
		}
	}

	public void setICGxVal(double xVal) {
		xICGVal = xVal;
	}

	public void setICGbPoints(double[] bPoints) {
		bICGPoints = bPoints;
	}

	public void setICGbVals(double[] bVals) {
		bICGVals = bVals;
	}

	public void setICGcPoints(double[] cPoints) {
		cICGPoints = cPoints;
	}

	public void setICGcVals(double[] cVals) {
		cICGVals = cVals;
	}

	public void setICGxPoints(double[] xPoints) {
		xICGPoints = xPoints;
	}

	public void setICGxVals(double[] xVals) {
		xICGVals = xVals;
	}

	public void setICGMissing(boolean missing) {
		this.ICGMissing = missing;
	}

	public void setECGMissing(boolean missing) {
		this.ECGMissing = missing;
	}

	public void setECGQOnsetMissing(boolean missing) {
		this.ECGQOnsetPointmissing = missing;
	}

	public void setECGQPOintMissing(boolean missing) {
		this.ECGQPointmissing = missing;
	}

	public void setLeftTime(double leftTime) {
		this.leftTime = leftTime;
		if (savedICGPoints.isEmpty() == false) {
			clearICGScoring();
		}
	}

	public void setRightTime(double rightTime) {
		this.rightTime = rightTime;
		if (savedICGPoints.isEmpty() == false) {
			clearICGScoring();
		}
	}

	public void setLabelNo(int number) {
		this.labelnumber = number;
	}

	public int getLabelNo() {
		return labelnumber;
	}

	public int getNotInAv() {
		return notInAv;
	}

	public synchronized void setScoringBeenSaved(boolean hasBeenSaved) {
		this.hasScoringBeenSaved = hasBeenSaved;
	}

	public synchronized void setFilterScoringBeenSaved(boolean hasBeenSaved) {
		this.hasFilterScoringBeenSaved = hasBeenSaved;
	}

	public void renameICGpoints() {
		if (bICGPoint == Double.NEGATIVE_INFINITY)
			bICGPoint = lICGPoint;
		if (cICGPoint == Double.NEGATIVE_INFINITY)
			cICGPoint = mICGPoint;
		if (xICGPoint == Double.NEGATIVE_INFINITY)
			xICGPoint = rICGPoint;
		if (bICGVal == Double.NEGATIVE_INFINITY)
			bICGVal = lICGVal;
		if (cICGVal == Double.NEGATIVE_INFINITY)
			cICGVal = mICGVal;
		if (xICGVal == Double.NEGATIVE_INFINITY)
			xICGVal = rICGVal;
		for (SavedICGPoint sip : savedICGPoints) {
			if (sip.bICGp == 0)
				sip.bICGp = sip.lICGp;
			if (sip.cICGp == 0)
				sip.cICGp = sip.mICGp;
			if (sip.xICGp == 0)
				sip.xICGp = sip.rICGp;
			if (sip.bICGv == 0)
				sip.bICGv = sip.lICGv;
			if (sip.cICGv == 0)
				sip.cICGv = sip.mICGv;
			if (sip.xICGv == 0)
				sip.xICGv = sip.rICGv;
		}
	}
}
