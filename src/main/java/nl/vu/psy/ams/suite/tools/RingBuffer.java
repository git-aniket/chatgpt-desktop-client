package nl.vu.psy.ams.suite.tools;

import java.util.Arrays;
/*
 * Simple ringbuffer of double values, used in
 * many algorithms
 */
public class RingBuffer {
	public double	data[];
	private int		curPos, bufferSize;
	private double	backValue;

	public RingBuffer(int bufferSize) {
		this.bufferSize = bufferSize;
		data = new double[bufferSize];
		curPos = 0;
		for (int i = 0; i < bufferSize; i++)
			add(0);
	}
	
	public void clear() {
		for (int i = 0; i < bufferSize; i++)
			add(0);
	}
	
	public void clear(double value) {
		for (int i = 0; i < bufferSize; i++)
			add(value);
	}

	public void add(double value) {
		data[curPos] = value;
		backValue = value;
		curPos++;
		if (curPos == bufferSize)
			curPos = 0;
	}

	public double get(int index) {
		int pos = curPos + index;
		while (pos >= bufferSize)
			pos -= bufferSize;
		return data[pos];
	}

	public void get3(double[] vals, int index) {
		int pos = curPos + index;
		if (pos >= bufferSize)
			pos -= bufferSize;
		if (pos < bufferSize - 2) {
			vals[0] = data[pos];
			vals[1] = data[pos + 1];
			vals[2] = data[pos + 2];
		} else if (pos < bufferSize - 1) {
			vals[0] = data[pos];
			vals[1] = data[pos + 1];
			vals[2] = data[0];
		} else {
			vals[0] = data[pos];
			vals[1] = data[0];
			vals[2] = data[1];
		}
	}

	public void getN(double[] vals) {
		int n = vals.length, index = 0;
		int pos = curPos - n;
		if (pos < 0)
			pos = bufferSize - (n - curPos);
		if (pos >= bufferSize)
			pos -= bufferSize;
		for (int i = 0; i < n; i++) {
			if (pos + i < bufferSize)
				vals[i] = data[pos + i];
			else {
				vals[i] = data[index];
				index++;
			}
		}
	}

	public double getBackValue() {
		return backValue;
	}

	public double getMean() {
		double val = 0;
		for (int i = 0; i < bufferSize; i++) {
			val += data[i];
		}
		val /= bufferSize;
		return val;
	}

	public double getMeanFromLastNPoints(int nPoints) {
		if (nPoints == 0)
			return 0;
		int n = 0;
		double mean = 0;
		for (int i = 1; i <= nPoints; i++) {
			n++;
			mean += get(bufferSize - i);
		}
		return mean / n;
	}
	
	public double getMedian() {
		double[] tmpData = Arrays.copyOf(data, bufferSize);
		Arrays.sort(tmpData);
		double median;
		if (bufferSize % 2 == 0) {
			median = tmpData[bufferSize / 2];
			median += tmpData[(bufferSize / 2) - 1];
			median /= 2.;
		} else {
			median = tmpData[bufferSize / 2];
		}
		return median;
	}

	public void getMinMax(double[] minmax) {
		minmax[0] = Double.POSITIVE_INFINITY;
		minmax[1] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < bufferSize; i++) {
			if (data[i] < minmax[0])
				minmax[0] = data[i];
			if (data[i] > minmax[1])
				minmax[1] = data[i];
		}
	}

	public void getMinMaxWithPos(double[] minmax, int[] minmaxpos) {
		minmax[0] = Double.POSITIVE_INFINITY;
		minmax[1] = Double.NEGATIVE_INFINITY;
		for (int i = curPos - 1; i >= 0; i--) {
			if (data[i] < minmax[0]) {
				minmax[0] = data[i];
				minmaxpos[0] = i - curPos;
			}
			if (data[i] > minmax[1]) {
				minmax[1] = data[i];
				minmaxpos[1] = i - curPos;
			}
		}
		for (int i = bufferSize - 1; i >= curPos; i--) {
			if (data[i] < minmax[0]) {
				minmax[0] = data[i];
				minmaxpos[0] = i - curPos;
			}
			if (data[i] > minmax[1]) {
				minmax[1] = data[i];
				minmaxpos[1] = i - curPos;
			}
		}
		if (minmaxpos[0] < 0)
			minmaxpos[0] += bufferSize;
		if (minmaxpos[1] < 0)
			minmaxpos[1] += bufferSize;
	}

	public double getSlope() {
		double sx = 0, sxx = 0, sy = 0, sxy = 0;
		int pos = curPos;
		for (int i = 0; i < bufferSize; i++) {
			sx += i;
			sxx += i * i;
			sxy += i * data[pos];
			sy += data[pos];
			pos++;
			if (pos == bufferSize)
				pos = 0;
		}
		return (bufferSize * sxy - sx * sy) / (bufferSize * sxx - sx * sx);
	}

	public double[] getSlopeAndIntercept() {
		double sx = 0, sxx = 0, sy = 0, sxy = 0;
		int pos = curPos;
		for (int i = 0; i < bufferSize; i++) {
			sx += i;
			sxx += i * i;
			sxy += i * data[pos];
			sy += data[pos];
			pos++;
			if (pos == bufferSize)
				pos = 0;
		}
		double ret[] = new double[2];
		ret[0] = (bufferSize * sxy - sx * sy) / (bufferSize * sxx - sx * sx);
		ret[1] = sy / bufferSize - ret[0] * sx / bufferSize;
		return ret;
	}

	public double getValue() {
		return data[curPos];
	}

	public double getVarianceFromLastNPoints(int nPoints) {
		if (nPoints < 2)
			return 0;
		int n = 0;
		double mean = 0;
		double M2 = 0;
		for (int i = 1; i <= nPoints; i++) {
			n++;
			double x = get(bufferSize - i);
			double delta = x - mean;
			mean += delta / n;
			M2 = M2 + delta * (x - mean);
		}
		return M2 / (n - 1);
	}
	
	public double getMinMax(double[] minmax, int nPoints) {
		minmax[0] = Double.POSITIVE_INFINITY;
		minmax[1] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < nPoints; i++) { // Buffer size = 200 
			if (data[i] < minmax[0])
				minmax[0] = data[i];
			if (data[i] > minmax[1])
				minmax[1] = data[i];
		}
		return minmax[1];
	}

	/*
	 * public void changeBackValue(double newBackValue){ int pos = curPos-1;
	 * if(pos==-1) pos = bufferSize-1; data[pos] = newBackValue; backValue =
	 * newBackValue; }
	 */
	public void stepBack() {
		curPos--;
		if (curPos == -1)
			curPos = bufferSize - 1;
	}

}
