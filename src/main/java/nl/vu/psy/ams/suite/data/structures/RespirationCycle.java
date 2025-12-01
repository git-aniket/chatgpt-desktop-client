package nl.vu.psy.ams.suite.data.structures;

import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * A respiration cycle, calculated from filtered DZ.
 * With start and end times of inspiration and expiration,
 * and booleans that show if it is an artifact, for different
 * reasons.
 */
public class RespirationCycle implements Comparable<RespirationCycle> {
	private double inspStart;
	private double expStart;
	private double expEnd;
	private int shortchangeCount = Integer.MIN_VALUE;

	private double lowValue, highValue;
	private boolean expEndSet = false;
	private double[] shortestIbi = null;
	private double[] longestIbi = null;
	private int longchangeCount = Integer.MIN_VALUE;
	private boolean irregularRR = false;
	// private boolean irregularIbi = false;
	private boolean isClippingDZ = false;
	private double realHighvalue;
	private double realLowvalue;
	private Double RRSuspicion;

	public RespirationCycle() {
	}

	public RespirationCycle(double time) {
		inspStart = time;
	}

	@Override
	public int compareTo(RespirationCycle o) {
		return Double.compare(inspStart, o.inspStart);
	}

	public void delExpEnd() {
		expEndSet = false;

	}

	public double getExpEnd() {
		return expEnd;
	}

	public double getExpStart() {
		return expStart;
	}

	public double getHighValue() {
		return highValue;
	}

	public double getInspStart() {
		return inspStart;
	}

	public double getLength() {
		if (expEndSet == false)
			return -1;
		return expEnd - inspStart;
	}

	public double[] getLongestIBI(double[] shortest) {
		if (isIrregularRR() || /* isIrregularIbi() || */ isClippingDZ())
			return null;
		if (expEndSet == false)
			return null;
		double[] ret = new double[2];
		ret[0] = Double.NEGATIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		if (bSet.getChangeCount() == longchangeCount) {
			return longestIbi;
		}
		longchangeCount = bSet.getChangeCount();
		double startTime = expStart;
		if (shortest != null) {
			if (startTime < shortest[0])
				startTime = shortest[0];
		}
		double afterLongest = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERLONGEST) * 1000.;
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(bSet.subSet(startTime, expEnd + afterLongest));
		if (bts.isEmpty()) {
			longestIbi = null;
			return null;
		}
		ECGBeat befBeat = bSet.getBeatBeforeTime(startTime);
		ECGBeat befBefBeat = null;
		if (befBeat != null) {
			befBefBeat = bSet.getPrevBeat(befBeat);
		}
		double prevIbi = Double.POSITIVE_INFINITY;
		if (befBeat != null && befBefBeat != null) {
			if (befBeat.isFirstInSeries() == false) {
				prevIbi = befBeat.getRPeakTime() - befBefBeat.getRPeakTime();
			}
		}
		if (befBeat != null) {
			if (bts.get(0).isFirstInSeries() == false) {
				double ibi = bts.get(0).getRPeakTime() - befBeat.getRPeakTime();
				if (ibi > prevIbi && ibi > ret[1]) {
					ret[0] = bts.get(0).getRPeakTime();
					ret[1] = ibi;
				}
				prevIbi = ibi;
			} else {
				prevIbi = Double.POSITIVE_INFINITY;
			}
		}
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries() == false) {
				double ibi = bts.get(i).getRPeakTime() - bts.get(i - 1).getRPeakTime();
				if (ibi > prevIbi && ibi > ret[1]) {
					ret[0] = bts.get(i).getRPeakTime();
					ret[1] = ibi;
				}
				prevIbi = ibi;
			} else {
				prevIbi = Double.POSITIVE_INFINITY;
			}
		}
		if (ret[1] == Double.NEGATIVE_INFINITY) {
			longestIbi = null;
			return null;
		}
		longestIbi = ret;
		return ret;
	}

	public double getLowValue() {
		return lowValue;
	}

	public Double getMeanIBI() {
		if (isExpEndSet() == false)
			return null;
		double meanIBI = CurrentOpenData.getInstance().getBeatSet().getAverageBetweenTimes(inspStart, expEnd, false);
		if (meanIBI < 0)
			return null;
		return meanIBI;
	}

	public double getRealHighValue() {
		return realHighvalue;
	}

	public double getRealLowValue() {
		return realLowvalue;
	}

	public Double getRespirationRate() {
		if (expEndSet == false)
			return null;
		return 60000000. / (expEnd - inspStart);
	}

	public Double getRSA() {
		double[] shortest = getShortestIBI();
		double[] longest = getLongestIBI(shortest);
		if (shortest == null || longest == null) {
			return null;
		}
		if (shortest[0] > longest[0])
			return null;
		double rsa = longest[1] - shortest[1];
		// if(rsa<0) return null;
		return rsa / 1000.;
	}

	public Double getRSAAdded() {
		double[] shortest = getShortestIBI();
		double[] longest = getLongestIBI(shortest);
		if (shortest == null || longest == null) {
			return null;
		}
		if (shortest[0] > longest[0])
			return null;
		double rsa = longest[1] + shortest[1];
		// if(rsa<0) return null;
		return rsa / 1000.;
	}

	public double[] getShortestIBI() {
		if (isIrregularRR() || /* isIrregularIbi() || */ isClippingDZ())
			return null;
		double[] ret = new double[2];
		ret[0] = Double.NEGATIVE_INFINITY;
		ret[1] = Double.POSITIVE_INFINITY;
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		if (bSet.getChangeCount() == shortchangeCount) {
			return shortestIbi;
		}
		shortchangeCount = bSet.getChangeCount();
		double afterShortest = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERSHORTEST) * 1000.;
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(bSet.subSet(inspStart, expStart + afterShortest));
		if (bts.isEmpty()) {
			shortestIbi = null;
			return null;
		}
		ECGBeat befBeat = bSet.getBeatBeforeTime(inspStart);
		ECGBeat befBefBeat = null;
		if (befBeat != null) {
			befBefBeat = bSet.getPrevBeat(befBeat);
		}
		double prevIbi = Double.NEGATIVE_INFINITY;
		if (befBeat != null && befBefBeat != null) {
			if (befBeat.isFirstInSeries() == false) {
				prevIbi = befBeat.getRPeakTime() - befBefBeat.getRPeakTime();
			}
		}
		if (befBeat != null) {
			if (bts.get(0).isFirstInSeries() == false) {
				double ibi = bts.get(0).getRPeakTime() - befBeat.getRPeakTime();
				if (ibi < prevIbi && ibi < ret[1]) {
					ret[0] = bts.get(0).getRPeakTime();
					ret[1] = ibi;
				}
				prevIbi = ibi;
			} else {
				prevIbi = Double.NEGATIVE_INFINITY;
			}
		}
		for (int i = 1; i < bts.size(); i++) {
			if (bts.get(i).isFirstInSeries() == false) {
				double ibi = bts.get(i).getRPeakTime() - bts.get(i - 1).getRPeakTime();
				if (ibi < prevIbi && ibi < ret[1]) {
					ret[0] = bts.get(i).getRPeakTime();
					ret[1] = ibi;
				}
				prevIbi = ibi;
			} else {
				prevIbi = Double.NEGATIVE_INFINITY;
			}
		}
		if (ret[1] == Double.POSITIVE_INFINITY) {
			shortestIbi = null;
			return null;
		}
		shortestIbi = ret;
		return ret;
	}

	public Double getTidalVolume() {
		return realHighvalue - realLowvalue;
	}

	public Double getTidalVolumeRaw() {
		return highValue - lowValue;
	}

	public boolean isArtefact() {
		return /* irregularIbi || */ irregularRR || isClippingDZ;
	}

	public boolean isClippingDZ() {
		return isClippingDZ;
	}

	public boolean isExpEndSet() {
		return expEndSet;
	}

	// public boolean isIrregularIbi() {
	// return irregularIbi;
	// }

	public boolean isIrregularRR() {
		return irregularRR;
	}

	public Double getRRSuspicion() {
		if (RRSuspicion == null)
			return Double.valueOf(0);
		return RRSuspicion;
	}

	public void setClippingDZ(boolean b) {
		isClippingDZ = b;
	}

	public void setExpEnd(double expEnd) {
		if (expEnd > expStart) {
			this.expEnd = expEnd;
			expEndSet = true;
		} else
			System.out.println("Expiration end must be later than exp start");
	}

	public void setExpStart(double expStart) {
		if ((!expEndSet || expEnd > expStart) && expStart > inspStart) {
			this.expStart = expStart;
		} else
			System.out.println("Expiration start must be later than insp start and earlier than exp end");
	}

	public void setHighRealValue(double realValueFromSampleValue) {
		realHighvalue = realValueFromSampleValue;
	}

	public void setHighValue(double highValue) {
		this.highValue = highValue;
	}

	public void setInspStart(double inspStart) {
		if (expStart == 0 || inspStart < expStart) {
			this.inspStart = inspStart;
		} else
			System.out.println("Inspiration start must be earlier than exp start");
	}

	// public void setIrregularIBI(boolean b) {
	// irregularIbi = b;
	// }

	public void setIrregularRR(boolean isIrregular) {
		irregularRR = isIrregular;
	}

	public void setLowValue(double lowValue) {
		this.lowValue = lowValue;
	}

	public void setLowRealValue(double realValueFromSampleValue) {
		realLowvalue = realValueFromSampleValue;
	}

	public void setRRSuspicion(Double rrSuspicion) {
		RRSuspicion = rrSuspicion;
	}

	public boolean isFirstInSeries() {
		return false;
	}

}
