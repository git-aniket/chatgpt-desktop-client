package nl.vu.psy.ams.suite.data.structures;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.RingBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import java.util.ArrayList;

/*
 * A part of a signal, that can be average from multiple
 * parts of the original signal. Main use: creating an
 * r-peak locked impedance average signal.
 */
public class SignalPart {
	private double[] values;
	private int nAv = 0;
	private double sampleTimeInUS;
	private AmsLabel label;
	private boolean calculating;
	private boolean filtered = false;
	private double avIBI = 0;

	private ArrayList<Double> Blocations = new ArrayList<Double>();
	private ArrayList<Double> Bvalues = new ArrayList<Double>();
	private ArrayList<Double> Clocations = new ArrayList<Double>();
	private ArrayList<Double> Cvalues = new ArrayList<Double>();
	private ArrayList<Double> Xlocations = new ArrayList<Double>();
	private ArrayList<Double> Xvalues = new ArrayList<Double>();

	public SignalPart() {
	};

	public SignalPart(double rPeakTime, String chan, AmsLabel l, boolean filtered) {
		this.label = l;
		this.avIBI = label.getAverage(false);
		this.filtered = filtered;
		double tL = 256000 + avIBI * 1000;

		// ---------------- To add FilteredDZDT Signal---------------------------
		// if (chan.equals("DZDT")
		// &&
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERDZDTNew)
		// == 1) {
		// chan = "FILTDZDT";
		// filtered = true;
		// }
		// if ((chan.equals("ECG") || chan.equals("V2ecg") || chan.equals("V3ecg"))
		// && AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FILTERECGNew)
		// == 1) {
		// chan = "FILT" + chan;
		// filtered = true;
		// }
		// ---------------------------------------------------------------------
		BinaryFile bf = new BinaryFile(chan);
		sampleTimeInUS = bf.getSampleTimeInUS();
		double[] data = bf.getDataRun(rPeakTime - 256000, tL);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (chan.equals("DZ")) {
			BinaryFile minbf = new BinaryFile("FILTDZ");
			double[] mindata = minbf.getDataRun(rPeakTime - 256000, tL);
			for (int i = 0; i < data.length; i++)
				data[i] -= mindata[i];
			try {
				minbf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		values = data;
	}

	public SignalPart(double[] values) {
		this.values = values;
		nAv = 1;
	}

	public void addToAverage(SignalPart part) {
		if (values == null) {
			values = part.values.clone(); // by value copy, not by reference!
			nAv = 1;
		} else {
			for (int i = 0; i < values.length; i++) {
				values[i] *= nAv;
				values[i] += part.values[i];
				values[i] /= nAv + 1;
			}
			nAv++;
		}
	}

	public void calcECGPoints(double avheartrate, boolean algoscoring) {
		if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")
				|| (CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS1"))) {
			setECGQPoint(-48000);
			return;
		} else {
			int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
			// Calculate no of samples
			/*
			 * i = 1024 -> 1000 Hz Sampling
			 * i = 512 -> 500 Hz Sampling
			 * i = 256 -> 250 Hz Sampling
			 */
			int i = (int) Math.floor((256000) / sampleTimeInUS);
			if (i > values.length) {
				return;
			}

			setECGRPoint(0);

			// ------------------------- To find Q point/R-onset
			// point------------------------
			RingBuffer QBuffer = new RingBuffer(3);
			int nPointsIn50ms = (int) (50000 / sampleTimeInUS);
			double time = 0;
			double minTime = 0;
			int minIndex = 0;
			double minTimes[] = new double[5];
			int minIndec[] = new int[5];
			double[] minVal = new double[5];
			int j = 0;
			// System.out.println("ECG QOnset point: _ "+ getECGQOnsetPoint());
			// To Check if the Q point is already scored
			if ((getECGQOnsetPoint() != Double.NEGATIVE_INFINITY) && (getECGQOnsetPoint() != misVal)) {
				setECGQOnsetPoint(getECGQOnsetPoint());
			} else {
				for (int q = i; q > i - nPointsIn50ms; q--) {
					/*
					 * if(values[q] < minVal) {
					 * minVal = values[q];
					 * minTime = time;
					 * minIndex = q;
					 * } else {
					 * break;
					 * }
					 */
					if (values[q] < values[q - 1] && values[q] <= values[q + 1] && values[q] < values[q - 2]
							&& values[q] <= values[q + 2]) {
						// minTime = time;
						// minIndex = q;
						minTimes[j] = time;
						minIndec[j] = q;
						minVal[j] = values[q];
						j++;
						if (j > 4)
							break;
					}
					time -= sampleTimeInUS;
				}
				double absMin = Double.POSITIVE_INFINITY;
				for (int k = 0; k < j; k++) {
					if (minVal[k] < absMin) {
						minTime = minTimes[k];
						minIndex = minIndec[k];
						absMin = minVal[k];
					}
				}
				if (minTime != 0)
					setECGQOnsetPoint(minTime);
				else {
					setECGQOnsetPoint(-35 * sampleTimeInUS);
					minTime = -35 * sampleTimeInUS;
				}
			}

			// --------------------- To find Q-onset---------------------------------
			// Requires - Q Point for calculation
			double slopetime = 0;
			double qamplitudeminVal = Double.NEGATIVE_INFINITY;
			double Qonsettime = minTime - (sampleTimeInUS * 2);
			int negativerange = (int) (2 / ((sampleTimeInUS / 1000)));
			int positiverange = (int) (25 / ((sampleTimeInUS / 1000)));

			// To Check if the Q-onset point is already scored
			if ((getECGQPoint() != Double.NEGATIVE_INFINITY) && (getECGQPoint() != misVal)) {
				setECGQPoint(getECGQPoint());
			} else {
				if (minIndex == 0) {
					minIndex = 243; // corresponding with -35 ms
				}
				for (int qon = (minIndex - negativerange); qon > (minIndex - positiverange); qon--) {
					double qamplitude = Math.ceil((getValueAt(Qonsettime) * 1000)) / 1000;

					QBuffer.add(qamplitude);
					if ((QBuffer.get(2) >= QBuffer.get(1)) && (QBuffer.get(2) >= QBuffer.get(0))) {

						if (qamplitude > qamplitudeminVal) {
							qamplitudeminVal = qamplitude;
							slopetime = Qonsettime;
						}
					}
					Qonsettime -= sampleTimeInUS;
				}
				if ((slopetime <= ((((256 - (minIndex - 25)) * 1000) - 2000) * (-1))) || (slopetime == 0)
						|| (slopetime == Double.NEGATIVE_INFINITY)) {
					setECGQPoint(minTime - 12000); // default 12 ms as in manual
				} else {
					setECGQPoint(slopetime);
				}
			}

			// ------------------------- To find P point------------------------
			// Requires - Q Point for calculation
			double RRInterval = 60 / avheartrate;
			int range = (int) (((RRInterval * 1000)));
			int RRdecidingfactor = (int) ((0.55 * range) / (sampleTimeInUS / 1000));
			if (RRdecidingfactor > i)
				RRdecidingfactor = i;
			double PminVal = Double.NEGATIVE_INFINITY;
			double ptime = minTime;
			double PminTime = 0;
			int PminIndex = 0;
			// To Check if the P point is already scored
			if ((getECGPOnsetPoint() != Double.NEGATIVE_INFINITY) && (getECGPOnsetPoint() != misVal)) {
				setECGPOnsetPoint(getECGPOnsetPoint());
			} else {
				for (int p = minIndex; p > (i - RRdecidingfactor); p--) {
					if (values[p] > PminVal) {
						PminVal = values[p];
						PminTime = ptime;
						PminIndex = p;
					}

					ptime -= sampleTimeInUS;
				}
				setECGPOnsetPoint(PminTime);
			}

			// --------------------- To find P-onset---------------------------------
			// Requires - P Point for calculation
			RingBuffer PBuffer = new RingBuffer(3);
			double Pslopetime = 0;
			double pamplitudeminVal = Double.POSITIVE_INFINITY;
			double Ponsettime = PminTime - (sampleTimeInUS * 2);
			positiverange = (int) (75 / ((sampleTimeInUS / 1000)));

			// To Check if the P-onset point is already scored
			if ((getECGPPoint() != Double.NEGATIVE_INFINITY) && (getECGPPoint() != misVal)) {
				setECGPPoint(getECGPPoint());
			} else {
				for (int pon = (PminIndex - negativerange); pon > (PminIndex - positiverange); pon--) {
					double pamplitude = Math.ceil((getValueAt(Ponsettime) * 1000)) / 1000;

					PBuffer.add(pamplitude);
					if ((PBuffer.get(2) <= PBuffer.get(1)) && (PBuffer.get(2) <= PBuffer.get(0))) {

						if (pamplitude < pamplitudeminVal) {
							pamplitudeminVal = pamplitude;
							Pslopetime = Ponsettime;
						}
					}
					Ponsettime -= sampleTimeInUS;
				}
				if ((Pslopetime <= ((((256 - (PminIndex - 75)) * 1000) - 2000) * (-1))) || (Pslopetime == 0)
						|| (Pslopetime == Double.NEGATIVE_INFINITY)) {
					setECGPPoint(PminTime - 20000);
				} else {
					setECGPPoint(Pslopetime);
				}
			}

			// ---------------------------- To find S
			// point-------------------------------------
			double STime = 0;
			double SminTime = 0;
			int SminIndex = 0;
			minTimes = new double[5];
			minIndec = new int[5];
			minVal = new double[5];
			j = 0;

			// To Check if the S point is already scored
			if ((getECGSPoint() != Double.NEGATIVE_INFINITY)) {
				setECGSPoint(getECGSPoint());
			} else {
				for (int s = i; s < (i + nPointsIn50ms); s++) {
					/*
					 * if(values[s] < SminVal){
					 * SminVal = values[s];
					 * SminTime = STime;
					 * SminIndex = s;
					 * }
					 */
					if (values[s] < values[s - 1] && values[s] <= values[s + 1] && values[s] < values[s - 2]
							&& values[s] <= values[s + 2]) {
						// SminTime = STime;
						// SminIndex = s;
						minTimes[j] = STime;
						minIndec[j] = s;
						minVal[j] = values[s];
						j++;
						if (j > 4)
							break;
					}
					STime += sampleTimeInUS;
				}
				double absMin = Double.POSITIVE_INFINITY;
				for (int k = 0; k < j; k++) {
					if (minVal[k] < absMin) {
						SminTime = minTimes[k];
						SminIndex = minIndec[k];
						absMin = minVal[k];
					}
				}
				if (SminTime == 0) {
					SminTime = STime;
					SminIndex = i + nPointsIn50ms;
				}
				setECGSPoint(SminTime);
			}

			// ---------------------------- To find S-Offset
			// point-------------------------------------
			double[] arrayset = new double[50];
			double slopechangetime = 0;
			int sOffsetIndex = 0;

			for (int q = 0; q < 30; q++) {
				arrayset[q] = 0;
			}
			// System.out.println("S_Offset Point:_ "+ SOffsetTime +"==="+
			// getECGSOffsetPoint());
			// To Check if the SOffset point is already scored
			if ((getECGSOffsetPoint() != Double.NEGATIVE_INFINITY)) {
				setECGSOffsetPoint(getECGSOffsetPoint());
			} else {
				if (SminIndex == 0) {
					SminTime = 29000;
					SminIndex = 46;
				}
				for (int s = SminIndex; s < (SminIndex + 50); s++) {
					double diff = Math.abs((values[s + 1] - values[s - 1]) / sampleTimeInUS);
					// double diff2 = (values[s + 1] - 2 * values[s] + values[s - 1]) /
					// (sampleTimeInUS * sampleTimeInUS);
					// Inflection point - slope is maximum at first derivative and zero crossing
					// point in the second derivative (measure of curvature of the signal)

					// find first minimum
					// double diff_min1 = Math.abs((values[s] - values[s - 2]) / sampleTimeInUS);
					double diff2_min1 = (values[s] - 2 * values[s - 1] + values[s - 2])
							/ (sampleTimeInUS * sampleTimeInUS);
					// double diff_plus1 = Math.abs((values[s + 2] - values[s]) / sampleTimeInUS);
					double diff2_plus1 = (values[s + 2] - 2 * values[s + 1] + values[s])
							/ (sampleTimeInUS * sampleTimeInUS);
					if (((diff2_plus1 < 0 && diff2_min1 > 0) || (diff2_plus1 > 0 && diff2_min1 < 0))) { // inflection
						slopechangetime = -256000 + s * sampleTimeInUS;
						sOffsetIndex = s;
						// System.out.println(s + " " + slopechangetime + " " + diff);
						if (Math.abs(diff) < 0.00001)
							break;
					}

					/*
					 * Math.ceil((getValueAt(SOffsetTime) *100));
					 * arrayset[m] = getValueAt(SOffsetTime-1000);
					 * arrayset[m+1] = getValueAt(SOffsetTime);
					 * dS = (arrayset[m+1] - arrayset[m])/1000;
					 * 
					 * if((dS > 0) && (dS < minimumchangeinslope) && (dS >= 0.00001)){
					 * minimumchangeinslope = dS;
					 * slopechangetime = SOffsetTime;
					 * }else if(dS < 0.00001){
					 * s = SminIndex + 50;
					 * slopechangetime = SminTime+50000;// To avoid SOffset time being negative
					 * infinity - From 3.6 beta
					 * }
					 */
					// m++;
				}
				setECGSOffsetPoint(slopechangetime);
			}

			// ------------------ To find T point------------------------------------
			RRdecidingfactor = (int) ((0.55 * range) / (sampleTimeInUS / 1000));
			if (RRdecidingfactor > values.length - i)
				RRdecidingfactor = values.length - i;
			double ttime = SminTime;
			double tminTime = 0;
			int tminIndex = 0;
			double tminVal = Double.NEGATIVE_INFINITY;
			if (sOffsetIndex == 0)
				sOffsetIndex = SminIndex;
			else
				ttime = slopechangetime;

			// To Check if the T point is already scored
			if ((getECGTPoint() != Double.NEGATIVE_INFINITY)) {
				setECGTPoint(getECGTPoint());
			} else {
				for (int t = sOffsetIndex; t < (i + RRdecidingfactor); t++) {
					if (values[t] > tminVal) {
						tminVal = values[t];
						tminIndex = t;
						tminTime = ttime;
					}
					ttime += sampleTimeInUS;
				}
				if (tminTime == 0) {
					tminTime = ttime;
					tminIndex = i + RRdecidingfactor;
				}
				setECGTPoint(tminTime);
			}

			// --------------------- To find T-Offset---------------------------------
			double TminTime = 0;
			double TOffsetTime = tminTime + 50000;
			RingBuffer tfrontBuffer = new RingBuffer(3);

			// To Check if the T -Offset point is already scored
			if ((getECGTOffsetPoint() != Double.NEGATIVE_INFINITY)) {
				setECGTOffsetPoint(getECGTOffsetPoint());
			} else {
				for (int qon = tminIndex + 50; qon < (tminIndex + 95); qon++) {
					double tamplitude = Math.ceil((getValueAt(TOffsetTime) * 100)) / 100;
					tfrontBuffer.add(tamplitude);
					if ((tfrontBuffer.get(0) == tfrontBuffer.get(1)) && (tfrontBuffer.get(1) == tfrontBuffer.get(2))
							&& (tfrontBuffer.get(0) == tfrontBuffer.get(2))
							&& ((Math.abs(tfrontBuffer.get(2))) - (Math.abs(tfrontBuffer.get(1))) >= 0)) {
						TminTime = TOffsetTime - 2000;
						TOffsetTime += sampleTimeInUS;
					} else if ((tfrontBuffer.get(2) <= tfrontBuffer.get(1)) && (TOffsetTime < 400000)
							&& (tfrontBuffer.get(0) < tfrontBuffer.get(1))) {
						TminTime = TOffsetTime - 2000;
						TOffsetTime += sampleTimeInUS;
					} else if (tfrontBuffer.get(2) < tfrontBuffer.get(1)) {
						TminTime = TOffsetTime - 2000;
						TOffsetTime += sampleTimeInUS;
					} else if ((tfrontBuffer.get(0) != 0) && (tfrontBuffer.get(0) != 0)
							&& (tfrontBuffer.get(2) > tfrontBuffer.get(1))) {
						qon = (tminIndex + 95);
					}
				}
				setECGTOffsetPoint(TminTime);
			}

			// ----------------To save Algorithm Scoring and use it for
			// tuning------------------------------------
			if (getECGSPointAlgo() == Double.NEGATIVE_INFINITY) {
				setECGPOnsetPointAlgo(PminTime);
				if ((Pslopetime <= ((((256 - (PminIndex - 25)) * 1000) - 2000) * (-1))) || (Pslopetime == 0)
						|| (Pslopetime == Double.NEGATIVE_INFINITY)) {
					setECGPPointAlgo(PminTime - 20000);
				} else {
					setECGPPointAlgo(Pslopetime);
				}
				setECGQOnsetPointAlgo(minTime);
				if ((slopetime <= ((((256 - (minIndex - 25)) * 1000) - 2000) * (-1))) || (slopetime == 0)
						|| (slopetime == Double.NEGATIVE_INFINITY)) {
					setECGQPointAlgo(minTime - 5000);
				} else {
					setECGQPointAlgo(slopetime);
				}
				setECGSPointAlgo(SminTime);
				setECGSOffsetPointAlgo(slopechangetime);
				setECGTPointAlgo(tminTime);
				setECGTOffsetPointAlgo(TminTime);
			}
			// -----------------------------------------------------------------------------------------------------
			if (label != null) {
				label.setECGBeenScored(true);
				if (label.hasECGBeenScored() && label.hasICGBeenScored()) {
					if (label.hasScoringBeenSaved() == false && !filtered) {
						String scoringName = "Detection Algorithm";
						saveICGScoring(scoringName);
						label.setScoringBeenSaved(true);
					} else if (label.hasFilterScoringBeenSaved() == false && filtered) {
						String scoringName = "Algorithm Filtered";
						saveICGScoring(scoringName);
						label.setFilterScoringBeenSaved(true);
					}
				}
			}
		}
	}

	public void calcICGPoints(double avheartrate) {
		// Calculate no of samples
		/*
		 * i = 1024 -> 1000 Hz Sampling
		 * i = 512 -> 500 Hz Sampling
		 * i = 256 -> 250 Hz Sampling
		 */
		int i = (int) Math.floor((256000) / sampleTimeInUS);

		if (i > values.length) {
			setcPoint(150000);
			setxPoint(350000);
			setbPoint(getcPoint() - 36000.); // (36000. + (getcPoint() - 36000.) / 2);
			return;
		}
		Blocations.clear();
		Bvalues.clear();
		Clocations.clear();
		Cvalues.clear();
		Xlocations.clear();
		Xvalues.clear();
		// -------------------------------- C Point -----------------------------
		double RRInterval = 60 / avheartrate;
		i += (int) (RRInterval * 40);
		int searchwindow_for_C = (int) (RRInterval * 300000);
		double[] minVal = new double[5];
		minVal[0] = Double.NEGATIVE_INFINITY;
		double minTimes[] = new double[5];
		double time = (int) (RRInterval * 40) * 1000;
		int minIndec[] = new int[5];
		minIndec[0] = i;
		int j = 0;

		while (time <= searchwindow_for_C) { // (From R-peak to RPeak+300ms)
			if (values[i] > values[i - 1] && values[i] >= values[i + 1] && values[i] > values[i - 2]
					&& values[i] >= values[i + 2]) {
				minVal[j] = values[i];
				minTimes[j] = time;
				minIndec[j] = i;
				Clocations.add(time);
				Cvalues.add(getValueAt(time));
				j++;
			}
			if (j > 4)
				break;
			/*
			 * System.out.print(i);
			 * System.out.print(",");
			 * System.out.println(values[i]);
			 */
			i++;
			time += sampleTimeInUS;
		}

		if (minIndec[0] == 0) {
			minTimes[0] = 150000;
			minIndec[0] = (int) Math.floor((minTimes[0] + 256000) / sampleTimeInUS);
		}
		double minTime = minTimes[0];
		int minIndex = minIndec[0];
		if (j > 1) {
			int CIndex = 0, x = 0;
			for (int k = 1; k < j; k++) {
				if (minVal[k] > 1.4 * minVal[x]) {
					CIndex = k;
					x = k;
				}
			}
			minTime = minTimes[CIndex];
			minIndex = minIndec[CIndex];
		}
		setClocations(Clocations.toArray(new Double[Clocations.size()]));
		setcPoint(minTime);

		// -------------------------X Point------------------------------------
		int nPointsIn10ms = (int) (10000 / sampleTimeInUS);
		if (getECGTPoint() == Double.NEGATIVE_INFINITY)
			calcECGPoints(avheartrate, false);
		int nPointsBeforeT = (int) ((getECGTPoint() + 256000) / sampleTimeInUS);
		int searchwindow_for_X = (int) (RRInterval * 400);
		searchwindow_for_X = (int) (600 - 2.5 * avheartrate);
		double minXTime = Double.NEGATIVE_INFINITY;
		/*
		 * if (searchwindow_for_X > values.length - nPointsIn50ms)
		 * searchwindow_for_X = values.length - nPointsIn50ms;
		 */
		int searchstart_for_X = minIndex + nPointsIn10ms;
		if (nPointsBeforeT > searchstart_for_X) // Start from ECG T point
			searchstart_for_X = nPointsBeforeT + nPointsIn10ms;
		if (searchstart_for_X + searchwindow_for_X >= values.length - 2)
			searchwindow_for_X = values.length - 2 - searchstart_for_X;
		// double maxVal = Double.POSITIVE_INFINITY;

		for (j = searchstart_for_X; j < searchstart_for_X + searchwindow_for_X; j++) { // From CPoint+50ms to 2048-50 ms
			double cV = values[j];
			// if (cV < maxVal)
			// maxVal = cV;
			if (cV <= values[j - 1] && cV <= values[j + 1] && cV <= values[j - 2] && cV <= values[j + 2]) { // If
																											// current
																											// value is
																											// lesser
																											// than
																											// previous
																											// and next
																											// value
				if (minXTime == Double.NEGATIVE_INFINITY)
					minXTime = -256000 + j * sampleTimeInUS;
				Xlocations.add(-256000 + j * sampleTimeInUS);
				Xvalues.add(getValueAt(-256000 + j * sampleTimeInUS));
			}
		}
		setXlocations(Xlocations.toArray(new Double[Xlocations.size()]));
		setxPoint(minXTime);

		// -------------------------------------------- B
		// Point--------------------------------------------------------------------------
		int nPointsin10ms = (int) (10000 / sampleTimeInUS);
		i = (int) Math.floor((256000) / sampleTimeInUS);
		double[] smoothedVals = new double[values.length];
		double maxDiff = Double.NEGATIVE_INFINITY;
		double maxDiff2 = Double.NEGATIVE_INFINITY;
		// double minScore = Double.MAX_VALUE;
		minTime = Double.NEGATIVE_INFINITY;

		// --------------------Smoothing-------------------------
		for (int q = i + nPointsin10ms; q < values.length - nPointsin10ms; q++) { // From RPeak-26ms to 2048-10 ms
			double av = 0;
			int nVals = 0;
			for (j = q - nPointsin10ms; j <= q + nPointsin10ms; j++) { // Smoothing
				av += values[j];
				nVals++;
			}
			av /= nVals;
			smoothedVals[q] = av;
		}
		// ------------------------------------------------------

		// -------------Find Absolute maxima of first and second derivative from the
		// smoothed values----
		for (int q = i - 15 + 1; q < minIndex - 1; q++) {
			double diff = Math.abs((smoothedVals[q + 1] - smoothedVals[q - 1]) / sampleTimeInUS); // First order
																									// differentiation
			if (diff > maxDiff)
				maxDiff = diff; // Find maximum first derivative
			double diff2 = Math.abs((smoothedVals[q + 1] - 2 * smoothedVals[q] + smoothedVals[q - 1])
					/ (sampleTimeInUS * sampleTimeInUS)); // Second order differentiation
			if (diff2 > maxDiff2)
				maxDiff2 = diff2; // Find maximum second derivative
		}
		// ----------------------------------------------------------

		// int CPoint_index = (int) ((-256000 + minIndex *
		// sampleTimeInUS)/sampleTimeInUS);
		// int min_search_interval = (int) Math.floor((256000) / sampleTimeInUS) ;
		// int max_search_interval = minIndex - 256; //(int) (RRInterval *
		// CPoint_index);
		// max_search_interval = (int) (min_search_interval + 0.8 * (max_search_interval
		// - min_search_interval));

		// for (int q = (min_search_interval + nPointsin10ms) ; q < (min_search_interval
		// + max_search_interval ); q++) {
		// for (int q = (min_search_interval + max_search_interval ); q >
		// (min_search_interval + nPointsin10ms) ; q--) {
		for (int q = minIndex; q > i - 15; q--) { // C point to R point
			double diff = Math.abs((smoothedVals[q + 1] - smoothedVals[q - 1]) / sampleTimeInUS);
			// double diff2 = (smoothedVals[q + 1] - 2 * smoothedVals[q] + smoothedVals[q -
			// 1]) / (sampleTimeInUS * sampleTimeInUS);
			// Inflection point - slope is maximum at first derivative and zero crossing
			// point in the second derivative (measure of curvature of the signal)
			// double score = diff / maxDiff - diff2 / maxDiff2;

			// find first minimum
			// double diff_min1 = Math.abs((smoothedVals[q] - smoothedVals[q - 2]) /
			// sampleTimeInUS);
			double diff2_min1 = (smoothedVals[q] - 2 * smoothedVals[q - 1] + smoothedVals[q - 2])
					/ (sampleTimeInUS * sampleTimeInUS);
			// double score_min1 = diff_min1 / maxDiff - diff2_min1 / maxDiff2;
			// double diff_plus1 = Math.abs((smoothedVals[q + 2] - smoothedVals[q]) /
			// sampleTimeInUS);
			double diff2_plus1 = (smoothedVals[q + 2] - 2 * smoothedVals[q + 1] + smoothedVals[q])
					/ (sampleTimeInUS * sampleTimeInUS);
			// double score_plus1 = diff_plus1 / maxDiff - diff2_plus1 / maxDiff2;
			if (((diff2_plus1 < 0 && diff2_min1 > 0) || (diff2_plus1 > 0 && diff2_min1 < 0))) { // inflection
				// (score < score_plus1 && score <= score_min1) {
				// minScore = score;
				// System.out.println(q + " " + minTime + " " + diff);
				if (Math.abs(diff) < 0.00002) {
					if (minTime == Double.NEGATIVE_INFINITY)
						minTime = -256000 + q * sampleTimeInUS;
					Blocations.add(-256000 + q * sampleTimeInUS);
					Bvalues.add(getValueAt(-256000 + q * sampleTimeInUS));
				}
			}
			if (values[q] < values[q - 1] && values[q] <= values[q + 1] && values[q] < values[q - 2]
					&& values[q] <= values[q + 2]) { // minimum
				// (score < score_plus1 && score <= score_min1) {
				// minScore = score;
				Blocations.add(-256000 + q * sampleTimeInUS);
				Bvalues.add(getValueAt(-256000 + q * sampleTimeInUS));
				if (minTime == Double.NEGATIVE_INFINITY)
					minTime = -256000 + q * sampleTimeInUS;
				// System.out.println(q + " " + minTime + " " + minScore);
			}
		}
		setBlocations(Blocations.toArray(new Double[Blocations.size()]));
		setbPoint(minTime);

		if (getcPoint() == Double.NEGATIVE_INFINITY)
			setcPoint(150000);
		if (getxPoint() == Double.NEGATIVE_INFINITY)
			setxPoint(350000);
		if (getbPoint() == Double.NEGATIVE_INFINITY)
			setbPoint(getcPoint() - 36000.); // (36000. + (getcPoint() - 36000.) / 2);

		if (label != null) {
			label.setICGBeenScored(true);
			if (label.hasICGBeenScored() && label.hasECGBeenScored()) {
				if (label.hasScoringBeenSaved() == false && !filtered) {
					String scoringName = "Detection Algorithm";
					saveICGScoring(scoringName);
					label.setScoringBeenSaved(true);
				} else if (label.hasFilterScoringBeenSaved() == false && filtered) {
					String scoringName = "Algorithm Filtered";
					saveICGScoring(scoringName);
					label.setFilterScoringBeenSaved(true);
				}
			}
		}
	}

	@Override
	public boolean equals(Object e) {
		SignalPart p = (SignalPart) e;
		if (values == null || p.values == null)
			return false;
		if (values.length != p.values.length)
			return false;
		for (int i = 0; i < values.length; i++) {
			if (values[i] != p.values[i])
				return false;
		}
		return true;
	}

	public synchronized boolean getCalculating() {
		return calculating;
	}

	public double getECGPPoint() {
		if (label != null)
			return label.getECGPPoint();
		return 0;
	}

	public double getECGPPointAlgo() {
		if (label != null)
			return label.getECGPPointAlgo();
		return 0;
	}

	public double getECGPOnsetPoint() {
		if (label != null)
			return label.getECGPOnsetPoint();
		return 0;
	}

	public double getECGPOnsetPointAlgo() {
		if (label != null)
			return label.getECGPOnsetPointAlgo();
		return 0;
	}

	public double getECGQPoint() {
		if (label != null)
			return label.getECGQPoint();
		return 0;
	}

	public double getECGQPointAlgo() {
		if (label != null)
			return label.getECGQPointAlgo();
		return 0;
	}

	public double getECGQOnsetPoint() {
		if (label != null)
			return label.getECGQOnsetPoint();
		return 0;
	}

	public double getECGQOnsetPointAlgo() {
		if (label != null)
			return label.getECGQOnsetPointAlgo();
		return 0;
	}

	public double getECGRPoint() {
		if (label != null)
			return label.getECGRPoint();
		return 0;
	}

	public double getECGSPoint() {
		if (label != null)
			return label.getECGSPoint();
		return 0;
	}

	public double getECGSPointAlgo() {
		if (label != null)
			return label.getECGSPointAlgo();
		return 0;
	}

	public double getECGSOffsetPoint() {
		if (label != null)
			return label.getECGSOffsetPoint();
		return 0;
	}

	public double getECGSOffsetPointAlgo() {
		if (label != null)
			return label.getECGSOffsetPointAlgo();
		return 0;
	}

	public double getECGTPoint() {
		if (label != null)
			return label.getECGTPoint();
		return 0;
	}

	public double getECGTPointAlgo() {
		if (label != null)
			return label.getECGTPoint();
		return 0;
	}

	public double getECGTOffsetPoint() {
		if (label != null)
			return label.getECGTOffsetPoint();
		return 0;
	}

	public double getECGTOffsetPointAlgo() {
		if (label != null)
			return label.getECGTOffsetPointAlgo();
		return 0;
	}

	public double getECGPVal() {
		if (label != null)
			return label.getECGPVal();
		return 0;
	}

	public double getECGPValAlgo() {
		if (label != null)
			return label.getECGPValAlgo();
		return 0;
	}

	public double getECGPOnsetVal() {
		if (label != null)
			return label.getECGPOnsetVal();
		return 0;
	}

	public double getECGPOnsetValAlgo() {
		if (label != null)
			return label.getECGPOnsetValAlgo();
		return 0;
	}

	public double getECGQVal() {
		if (label != null)
			return label.getECGQVal();
		return 0;
	}

	public double getECGQValAlgo() {
		if (label != null)
			return label.getECGQValAlgo();
		return 0;
	}

	public double getECGQOnsetVal() {
		if (label != null)
			return label.getECGQOnsetVal();
		return 0;
	}

	public double getECGQOnsetValAlgo() {
		if (label != null)
			return label.getECGQOnsetValAlgo();
		return 0;
	}

	public double getECGRVal() {
		if (label != null)
			return label.getECGRVal();
		return 0;
	}

	public double getECGSVal() {
		if (label != null)
			return label.getECGSVal();
		return 0;
	}

	public double getECGSValAlgo() {
		if (label != null)
			return label.getECGSValAlgo();
		return 0;
	}

	public double getECGSOffsetVal() {
		if (label != null)
			return label.getECGSOffsetVal();
		return 0;
	}

	public double getECGSOffsetValAlgo() {
		if (label != null)
			return label.getECGSOffsetValAlgo();
		return 0;
	}

	public double getECGTVal() {
		if (label != null)
			return label.getECGTVal();
		return 0;
	}

	public double getECGTValAlgo() {
		if (label != null)
			return label.getECGTValAlgo();
		return 0;
	}

	public double getECGTOffsetVal() {
		if (label != null)
			return label.getECGTOffsetVal();
		return 0;
	}

	public double getECGTOffsetValAlgo() {
		if (label != null)
			return label.getECGTOffsetValAlgo();
		return 0;
	}

	public AmsLabel getLabel() {
		return label;
	}

	public double[] getBlocations() {
		if (label != null)
			return label.getICGbPoints();
		return new double[0];
		// return Blocations;
	}

	public double[] getBvalues() {
		if (label != null)
			return label.getICGbVals();
		return new double[0];
		// return Bvalues;
	}

	public double[] getClocations() {
		if (label != null)
			return label.getICGcPoints();
		return new double[0];
		// return Clocations;
	}

	public double[] getCvalues() {
		if (label != null)
			return label.getICGcVals();
		return new double[0];
		// return Cvalues;
	}

	public double[] getXlocations() {
		if (label != null)
			return label.getICGxPoints();
		return new double[0];
		// return Xlocations;
	}

	public double[] getXvalues() {
		if (label != null)
			return label.getICGxVals();
		return new double[0];
		// return Xvalues;
	}

	public boolean isAlgorithmScoring() {
		return label.isalgorithmscoring();
	}

	public double getbPoint() {
		if (label != null)
			return label.getICGbPoint();
		return 0;
	}

	public double getbVal() {
		if (label != null)
			return label.getICGbVal();
		return 0;
	}

	public double[] getMinMax() {
		double ret[] = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		for (double d : values) {
			if (d < ret[0])
				ret[0] = d;
			if (d > ret[1])
				ret[1] = d;
		}
		return ret;
	}

	public double getcPoint() {
		if (label != null)
			return label.getICGcPoint();
		return 0;
	}

	public double getcVal() {
		if (label != null)
			return label.getICGcVal();
		return 0;
	}

	public int getnumberOfComplexes() {
		return nAv;
	}

	public double getxPoint() {
		if (label != null)
			return label.getICGxPoint();
		return 0;
	}

	public double getxVal() {
		if (label != null)
			return label.getICGxVal();
		return 0;
	}

	public double getSampleTimeInUS() {
		return sampleTimeInUS;
	}

	public double getValueAt(double time) {
		int lindex = (int) Math.floor((time + 256000) / sampleTimeInUS);
		int rindex = (int) Math.ceil((time + 256000) / sampleTimeInUS);
		if (values == null)
			return 0;
		if (lindex < 0 || rindex >= values.length)
			return 0;
		double rT = (time - (-256000 + lindex * sampleTimeInUS)) / sampleTimeInUS;
		double yVal = (1 - rT) * values[lindex] + rT * values[rindex];
		return yVal;
	}

	public double[] getValues() {
		return values;
	}

	public boolean isECGMissing() {
		if (label != null)
			return label.isECGMissing();
		return false;
	}

	public boolean isICGMissing() {
		if (label != null)
			return label.isICGMissing();
		return false;
	}

	public boolean isECGQOnsetMissing() {
		if (label != null)
			return label.isECGQOnsetMissing();
		return false;
	}

	public boolean isECGQPointMissing() {
		if (label != null)
			return label.isECGQPointMissing();
		return false;
	}

	public void removeFromAverage(SignalPart part) {
		for (int i = 0; i < values.length; i++) {
			values[i] *= nAv;
			values[i] -= part.values[i];
			values[i] /= nAv - 1;
		}
		nAv--;
	}

	public void restoreScoring(String ret) {
		if (label != null)
			label.restoreICGScoring(ret);
	}

	public void saveICGScoring(String string) {
		if (label != null) {
			label.saveICGScoring(string);
		}
	}

	public synchronized void setCalculating(boolean b) {
		calculating = b;
	}

	// --- P onset point---
	public void setECGPPoint(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGpPoint(time);
				double rVal = getValueAt(label.getECGPPoint());
				setECGPVal(rVal);
			}
		}
	}

	public void setECGPPointAlgo(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGpPointAlgo(time);
				double rVal = getValueAt(label.getECGPPointAlgo());
				setECGPValAlgo(rVal);
			}
		}
	}

	// ---- P point-----
	public void setECGPOnsetPoint(double time) {

		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGpOnsetPoint(time);
				double ponsetVal = getValueAt(label.getECGPOnsetPoint());
				setECGPonsetVal(ponsetVal);
			}
		}
	}

	public void setECGPOnsetPointAlgo(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGpOnsetPointAlgo(time);
				double ponsetVal = getValueAt(label.getECGPOnsetPointAlgo());
				setECGPonsetValAlgo(ponsetVal);
			}
		}
	}

	// --- Q onset point---
	public void setECGQPoint(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGqPoint(time);
				double rVal = getValueAt(label.getECGQPoint());
				setECGQVal(rVal);
			}
		}
	}

	public void setECGQPointAlgo(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGqPointAlgo(time);
				double rVal = getValueAt(label.getECGQPointAlgo());
				setECGQValAlgo(rVal);
			}
		}
	}

	// ---- Q point-----
	public void setECGQOnsetPoint(double time) {

		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGqOnsetPoint(time);
				double qonsetVal = getValueAt(label.getECGQOnsetPoint());
				setECGQonsetVal(qonsetVal);
			}
		}
	}

	public void setECGQOnsetPointAlgo(double time) {
		if (time > -256000 && time < 0) {
			if (label != null) {
				label.setECGqOnsetPointAlgo(time);
				double qonsetVal = getValueAt(label.getECGQOnsetPointAlgo());
				setECGQonsetValAlgo(qonsetVal);
			}
		}
	}

	// ---- R point-----
	public void setECGRPoint(double time) {
		if (time == 0) {
			if (label != null) {
				label.setECGRPoint(time);
				double rVal = getValueAt(label.getECGRPoint());
				setECGRVal(rVal);
			}
		}
	}

	// ---- S point-----
	public void setECGSPoint(double time) {
		if (time > 0 && time < 100000) {
			if (label != null) {
				label.setECGSPoint(time);
				double rVal = getValueAt(label.getECGSPoint());
				setECGSVal(rVal);
			}
		}
	}

	public void setECGSPointAlgo(double time) {
		if (time > 0 && time < 100000) {
			if (label != null) {
				label.setECGSPointAlgo(time);
				double rVal = getValueAt(label.getECGSPointAlgo());
				setECGSValAlgo(rVal);
			}
		}
	}

	// ---- S offset point-----
	public void setECGSOffsetPoint(double time) {
		if (time > 0 && time < 100000) {
			if (label != null) {
				label.setECGSOffsetPoint(time);
				double rVal = getValueAt(label.getECGSOffsetPoint());
				setECGSOffsetVal(rVal);
			}
		}
	}

	public void setECGSOffsetPointAlgo(double time) {
		if (time > 0 && time < 100000) {
			if (label != null) {
				label.setECGSOffsetPointAlgo(time);
				double rVal = getValueAt(label.getECGSOffsetPointAlgo());
				setECGSOffsetValAlgo(rVal);
			}
		}
	}

	// ---- T point-----
	public void setECGTPoint(double time) {
		if (time > 0 && time < avIBI * 1000) {
			if (label != null) {
				label.setECGTpoint(time);
				double tVal = getValueAt(label.getECGTPoint());
				setECGTVal(tVal);
			}
		}
	}

	public void setECGTPointAlgo(double time) {
		if (time > 0 && time < avIBI * 1000) {
			if (label != null) {
				label.setECGTpointAlgo(time);
				double tVal = getValueAt(label.getECGTPointAlgo());
				setECGTValAlgo(tVal);
			}
		}
	}

	// ---- T offset point-----
	public void setECGTOffsetPoint(double time) {
		if (time > 0 && time < avIBI * 1000) {
			if (label != null) {
				label.setECGTOffsetpoint(time);
				double tOffsetVal = getValueAt(label.getECGTOffsetPoint());
				setECGTOffsetVal(tOffsetVal);
			}
		}
	}

	public void setECGTOffsetPointAlgo(double time) {
		if (time > 0 && time < avIBI * 1000) {
			if (label != null) {
				label.setECGTOffsetpointAlgo(time);
				double tOffsetVal = getValueAt(label.getECGTOffsetPointAlgo());
				setECGTOffsetValAlgo(tOffsetVal);
			}
		}
	}

	public void setECGPVal(double rVal) {
		if (label != null)
			label.setECGPVal(rVal);
	}

	public void setECGPValAlgo(double rVal) {
		if (label != null)
			label.setECGPValAlgo(rVal);
	}

	public void setECGPonsetVal(double rVal) {
		if (label != null)
			label.setECGPOnsetVal(rVal);
	}

	public void setECGPonsetValAlgo(double rVal) {
		if (label != null)
			label.setECGPOnsetValAlgo(rVal);
	}

	public void setECGQVal(double rVal) {
		if (label != null)
			label.setECGQVal(rVal);
	}

	public void setECGQValAlgo(double rVal) {
		if (label != null)
			label.setECGQValAlgo(rVal);
	}

	public void setECGQonsetVal(double rVal) {
		if (label != null)
			label.setECGQOnsetVal(rVal);
	}

	public void setECGQonsetValAlgo(double rVal) {
		if (label != null)
			label.setECGQOnsetValAlgo(rVal);
	}

	public void setECGRVal(double rVal) {
		if (label != null)
			label.setECGRVal(rVal);
	}

	public void setECGSVal(double rVal) {
		if (label != null)
			label.setECGSVal(rVal);
	}

	public void setECGSValAlgo(double rVal) {
		if (label != null)
			label.setECGSValAlgo(rVal);
	}

	public void setECGSOffsetVal(double rVal) {
		if (label != null)
			label.setECGSOffsetVal(rVal);
	}

	public void setECGSOffsetValAlgo(double rVal) {
		if (label != null)
			label.setECGSOffsetValAlgo(rVal);
	}

	public void setECGTVal(double tVal) {
		if (label != null)
			label.setECGTVal(tVal);
	}

	public void setECGTValAlgo(double tVal) {
		if (label != null)
			label.setECGTValAlgo(tVal);
	}

	public void setECGTOffsetVal(double tVal) {
		if (label != null)
			label.setECGTOffsetVal(tVal);
	}

	public void setECGTOffsetValAlgo(double tVal) {
		if (label != null)
			label.setECGTOffsetValAlgo(tVal);
	}

	public void setLabel(AmsLabel label) {
		this.label = label;
		this.avIBI = label.getAverage(false);
	}

	public void setBlocations(Double[] Blocations) {
		if (label != null) {
			double[] bloc_d = new double[Blocations.length];
			for (int i = 0; i < Blocations.length; i++)
				bloc_d[i] = (double) Blocations[i];
			label.setICGbPoints(bloc_d);
			Double[] bVal = Bvalues.toArray(new Double[Bvalues.size()]);
			double[] bVald = new double[Blocations.length];
			for (int i = 0; i < bVal.length; i++)
				bVald[i] = (double) bVal[i];
			setBvalues(bVald);
		}
	}

	public void setBvalues(double[] Bvalues) {
		if (label != null) {
			label.setICGbVals(Bvalues);
		}
	}

	public void setbPoint(double bPoint) {
		if (bPoint > -256000 && bPoint < avIBI * 1000) {
			if (label != null) {
				label.setICGbPoint(bPoint);
				double bVal = getValueAt(label.getICGbPoint());
				setbVal(bVal);
			}
		}
	}

	public void setbVal(double bVal) {
		if (label != null)
			label.setICGbVal(bVal);
	}

	public void setClocations(Double[] Clocations) {
		if (label != null) {
			double[] cloc_d = new double[Clocations.length];
			for (int i = 0; i < Clocations.length; i++)
				cloc_d[i] = (double) Clocations[i];
			label.setICGcPoints(cloc_d);
			Double[] cVal = Cvalues.toArray(new Double[Cvalues.size()]);
			double[] cVald = new double[Clocations.length];
			for (int i = 0; i < cVal.length; i++)
				cVald[i] = (double) cVal[i];
			setCvalues(cVald);
		}
	}

	public void setCvalues(double[] Cvalues) {
		if (label != null) {
			label.setICGcVals(Cvalues);
		}
	}

	public void setcPoint(double cPoint) {
		if (cPoint > -256000 && cPoint < avIBI * 1000) {
			if (label != null) {
				label.setICGcPoint(cPoint);
				double cVal = getValueAt(label.getICGcPoint());
				setcVal(cVal);
			}
		}
	}

	public void setcVal(double cVal) {
		if (label != null)
			label.setICGcVal(cVal);
	}

	public void setNumberOfComplexes(int numberOfComplexes) {
		nAv = numberOfComplexes;
	}

	public void setxPoint(double xPoint) {
		if (xPoint > -256000 && xPoint < avIBI * 1000) {
			if (label != null) {
				label.setICGxPoint(xPoint);
				double xVal = getValueAt(label.getICGxPoint());
				setxVal(xVal);
			}
		}
	}

	public void setXlocations(Double[] Xlocations) {
		if (label != null) {
			double[] xloc_d = new double[Xlocations.length];
			for (int i = 0; i < Xlocations.length; i++)
				xloc_d[i] = (double) Xlocations[i];
			label.setICGxPoints(xloc_d);
			Double[] xVal = Xvalues.toArray(new Double[Xvalues.size()]);
			double[] xVald = new double[Xlocations.length];
			for (int i = 0; i < xVal.length; i++)
				xVald[i] = (double) xVal[i];
			setXvalues(xVald);
		}
	}

	public void setXvalues(double[] Xvalues) {
		if (label != null) {
			label.setICGxVals(Xvalues);
		}
	}

	public void setxVal(double xVal) {
		if (label != null)
			label.setICGxVal(xVal);
	}

	public void setSampleTimeInUS(double sampleTimeInUS) {
		this.sampleTimeInUS = sampleTimeInUS;
	}

	public void setValues(double[] values) {
		this.values = values;
	}

}
