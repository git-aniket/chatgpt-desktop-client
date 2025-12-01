package nl.vu.psy.ams.suite.data.qrs.shape;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.qrs.QRSDetector;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.tools.RingBuffer;

/*
 * Class that uses a algorithm based on
 * ECG peak shape to detect beats.
 * Each local maximum gets a score, based on:
 * - peak to peak height
 * - upward slope
 * - downward slop
 * 
 * These scores are then checked against a 
 * high threshold. If the maximum is larger
 * than the threshold, a beat is detected.
 * Counters that hold the median ptp, slopes 
 * and ibi for the previous 10 beats are updated.
 * 
 * If no beat was found for 1.5 times the median of the
 * previous 10 ibis, we check the signal part again, but
 * with the low threshold. If no beat was found, even with
 * the low threshold, the entire algorithm is reset, which is
 * done in such a way that there is always at least one beat
 * that has the maximum score, so at least one beat will
 * always be detected.
 */
public class QRSShapeDetector implements QRSDetector {

	private class EndOfECGDataException extends Exception {

		private static final long serialVersionUID = 1L;

		public EndOfECGDataException(String message) {
			super(message);
		}

	}

	private class LocalECGOptimum {
		private double slopeUp, slopeDown, peakToPeakHeight;
		private double time;

		protected LocalECGOptimum(double time, double peakToPeakHeight, double slopeUp, double slopeDown) {
			this.time = time;
			this.peakToPeakHeight = peakToPeakHeight;
			this.slopeUp = slopeUp;
			this.slopeDown = slopeDown;
		}

		public double getPeakToPeakHeight() {
			return peakToPeakHeight;
		}

		public double getSlopeDown() {
			return slopeDown;
		}

		public double getSlopeUp() {
			return slopeUp;
		}

		public double getTime() {
			return time;
		}
	}

	private class NoBeatFound extends Exception {
		private static final long serialVersionUID = 1L;

		public NoBeatFound(String message) {
			super(message);
		}
	}

	private double wPeakToPeakHeight;
	private double wSlopeUp;
	private double wSlopeDown;
	private double wSum;
	private double leftTime, rightTime;
	private ArrayList<LocalECGOptimum> opts = new ArrayList<LocalECGOptimum>();
	private long lOffset, rOffset, curOffset;
	private double curTime;
	private long startTime;
	private long sampleTimeInUS;

	private File ecgFile, ticksFile;
	private long totalNumberOfShorts;

	private FileInputStream is = null, isT = null;
	private FileChannel fc = null, fcT = null;
	private int nSamplesIn15ms;
	private int nSamplesIn50ms;
	private double avPeakToPeak, avSlopeUp, avSlopeDown, avIBI;
	private RingBuffer peakToPeak10 = new RingBuffer(10);

	private RingBuffer slopeUp10 = new RingBuffer(10);
	private RingBuffer slopeDown10 = new RingBuffer(10);
	private RingBuffer ibi10 = new RingBuffer(10);
	private RingBuffer backBuffer;

	private RingBuffer frontBuffer;
	private RingBuffer minBuffer;
	private RingBuffer samples, sampleTicks;

	private SortedSet<ECGBeat> returnSet = new TreeSet<ECGBeat>();
	private List<Double> returnTimes = new ArrayList<Double>();
	private double[] minmax = new double[2];

	private double[] vals = new double[3];
	private double[] tvals = new double[3];
	private double prevTime;
	private double prevScore;
	private double highThreshold;
	private double lowThreshold;
	private ProgressMonitor monitor = null;
	private static int size = 1048576;
	private ByteBuffer bb = ByteBuffer.allocate(size);
	private IntBuffer sb = bb.asIntBuffer();

	private int[] buffer = new int[size / 4];
	private int[] ticks = new int[size / 4];

	private int bufferPos = size / 4; // 524288 262144

	private int nRead = size / 4; // 524288
	// private int checks = 0;
	private double slopeUp, slopeDown, peakToPeakHeight;
	private boolean useTicks = false, isLive = false;
	private double[] data, ticksD;

	public QRSShapeDetector(double wPeakToPeakHeight, double wSlopeUp, double wSlopeDown, double highThreshold,
			double lowThreshold) {
		this(wPeakToPeakHeight, wSlopeUp, wSlopeDown, highThreshold, lowThreshold,
				CurrentOpenData.getInstance().getStartTimeInUS(), CurrentOpenData
						.getInstance().getFileHeader().getDwSampleTime_us(),
				new File(CurrentOpenData.getInstance().getFilePath(), "FILTECG.bin"),
				new File(CurrentOpenData.getInstance().getFilePath(), "TicksA.bin"), false, false);
	}

	public QRSShapeDetector(double wPeakToPeakHeight, double wSlopeUp, double wSlopeDown, double highThreshold,
			double lowThreshold, String ecgFile) {
		this(wPeakToPeakHeight, wSlopeUp, wSlopeDown, highThreshold, lowThreshold,
				CurrentOpenData.getInstance().getStartTimeInUS(), CurrentOpenData
						.getInstance().getFileHeader().getDwSampleTime_us(),
				new File(CurrentOpenData.getInstance().getFilePath(), ecgFile),
				new File(CurrentOpenData.getInstance().getFilePath(), "TicksA.bin"), false, false);
	}

	public QRSShapeDetector(double wPeakToPeakHeight, double wSlopeUp, double wSlopeDown, double highThreshold,
			double lowThreshold, long startTime, boolean isLive, boolean useTicks) {
		this(wPeakToPeakHeight, wSlopeUp, wSlopeDown, highThreshold, lowThreshold, startTime, 1000, null, null, isLive,
				useTicks);
	}

	public QRSShapeDetector(double wPeakToPeakHeight, double wSlopeUp, double wSlopeDown, double highThreshold,
			double lowThreshold, long startTime,
			long sampleTimeInUS, File ecgFile, File ticksFile, boolean isLive, boolean liveTicks) {
		this.wPeakToPeakHeight = wPeakToPeakHeight; // 1.0
		this.wSlopeUp = wSlopeUp;// 0.33
		this.wSlopeDown = wSlopeDown;// 0.33
		wSum = wPeakToPeakHeight + wSlopeUp + wSlopeDown; // 1.66
		this.startTime = startTime; // start time in milliseconds
		if (!isLive) {
			// --------------------- Solved problem with ECG recorded less than 1000
			// Hz-------------------------
			sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			try {
				sampleTimeInUS *= CurrentOpenData.getInstance().getChannelInfoFromID("ECG").getDwDivider();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else
			sampleTimeInUS = 1000;
		this.sampleTimeInUS = sampleTimeInUS; // 1000
		this.ecgFile = ecgFile; // .bin file
		this.ticksFile = ticksFile; // .bin file
		if ((!isLive && ticksFile.exists()) || liveTicks)
			useTicks = true;

		this.highThreshold = highThreshold; // 0.8
		this.lowThreshold = lowThreshold; // 0.6

		nSamplesIn15ms = (int) (15000 / sampleTimeInUS); // 15
		nSamplesIn50ms = (int) (50000 / sampleTimeInUS); // 50
		backBuffer = new RingBuffer(nSamplesIn15ms); // 15
		frontBuffer = new RingBuffer(nSamplesIn15ms);// 15
		minBuffer = new RingBuffer(nSamplesIn50ms);// 50
		samples = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
		sampleTicks = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
		if (!isLive)
			totalNumberOfShorts = ecgFile.length() / 4;
		this.isLive = isLive;
	}

	private void addOptimumAsBeat(LocalECGOptimum opt, double curScore) {

		prevTime = opt.getTime();
		peakToPeak10.add(opt.getPeakToPeakHeight());
		slopeUp10.add(opt.getSlopeUp());
		slopeDown10.add(opt.getSlopeDown());
		if (isLive) {
			if (returnTimes.isEmpty() == false) {
				ibi10.add(opt.getTime() - returnTimes.get(returnTimes.size() - 1));
				avIBI = ibi10.getMedian();
			}

			returnTimes.add(opt.getTime());
		} else {
			if (returnSet.isEmpty() == false) {
				ibi10.add(opt.getTime() - returnSet.last().getRPeakTime());
				avIBI = ibi10.getMedian();
			}

			ECGBeat beat = new ECGBeat(opt.getTime()); // Get R-Peak time
			if (returnSet.isEmpty())
				beat.setFirstInSeries(true); // Check first in series

			returnSet.add(beat);
		}
		avPeakToPeak = peakToPeak10.getMedian();
		avSlopeUp = slopeUp10.getMedian();
		avSlopeDown = slopeDown10.getMedian();
		prevScore = curScore;
	}

	private void checkForLocalOptimum() {
		samples.get3(vals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
		if (useTicks)
			sampleTicks.get3(tvals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
		if (vals[1] > vals[2] && vals[1] >= vals[0]) { // Local optimum // If the middle point is greater than first and
														// second point
			slopeUp = backBuffer.getSlope();
			if (slopeUp > 0) {
				slopeDown = frontBuffer.getSlope();
				if (slopeDown < 0) {
					minBuffer.getMinMax(minmax);
					// minVal = minBuffer.first();
					peakToPeakHeight = vals[1] - minmax[0];
					if (useTicks) {
						opts.add(new LocalECGOptimum(tvals[1], peakToPeakHeight, slopeUp, slopeDown));
					} else
						opts.add(new LocalECGOptimum(curTime, peakToPeakHeight, slopeUp, slopeDown));
				}
			}
		}
		// checks++;
	}

	private void clearOptima() {
		clearOptima(false);
	}

	private void clearOptima(boolean all) {
		if (all) {
			ArrayList<LocalECGOptimum> toBeDeleted = new ArrayList<LocalECGOptimum>(opts.size());
			for (LocalECGOptimum o : opts) {
				toBeDeleted.add(o);
			}
			for (LocalECGOptimum o : toBeDeleted)
				opts.remove(o);
		} else {
			ArrayList<LocalECGOptimum> toBeDeleted = new ArrayList<LocalECGOptimum>(opts.size());
			for (LocalECGOptimum o : opts) {
				if (o.getTime() < prevTime + 1)
					toBeDeleted.add(o);
			}
			for (LocalECGOptimum o : toBeDeleted)
				opts.remove(o);
		}
	}

	private void closeInputStream() {
		try {
			if (is != null)
				is.close();
			if (isT != null)
				isT.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void fillBuffers() throws EndOfECGDataException {
		double curVal;
		for (int i = 0; i < nSamplesIn15ms; i++) { // i<15
			curVal = getNextValue(); // getNextValue() returns an int
			backBuffer.add(curVal); // ring buffer of 15
			samples.add(curVal); // ring buffer of 66
		}
		samples.add(getNextValue());
		for (int i = 0; i < nSamplesIn15ms; i++) {
			curVal = getNextValue();
			frontBuffer.add(curVal); // ring buffer of 15
			samples.add(curVal);
			minBuffer.add(curVal); // ring buffer of 50
		}
		for (int i = 0; i < nSamplesIn50ms - nSamplesIn15ms; i++) {
			curVal = getNextValue();
			samples.add(curVal);
			minBuffer.add(curVal);
		}
	}

	private void fillNextBatchOfLocalOptima() throws EndOfECGDataException {
		while (curTime - prevTime < 1.5 * avIBI) {
			checkForLocalOptimum();
			updateBuffers();
		}
	}

	@Override
	public SortedSet<ECGBeat> findBeats(double leftTime, double rightTime) {

		returnSet.clear(); // SortedSet of ECG
		this.leftTime = leftTime;
		this.rightTime = rightTime;
		initializeOffsets(); // Initiates curOffset, curTime and prevTime

		try {
			openInputStream(); // open the .bin file
			fillBuffers(); // fill front, back, minimum and sample buffer with values
			setInternalValues();
			prevTime = Double.NEGATIVE_INFINITY;
			while (true) {
				if (monitor != null) {
					monitor.setProgress((int) (curTime / 1000000));
				}
				try {
					findBeatsFromLocalOptima(highThreshold);
					clearOptima();
					fillNextBatchOfLocalOptima();
				} catch (NoBeatFound e) {
					try {
						findBeatsFromLocalOptima(lowThreshold);
						clearOptima();
						fillNextBatchOfLocalOptima();
					} catch (NoBeatFound f) {
						setInternalValues();
					}
				}
			}
		} catch (EndOfECGDataException e) {
			try {
				findBeatsFromLocalOptima(lowThreshold);
			} catch (NoBeatFound e1) {
			}
			clearOptima();

		} finally {
			closeInputStream();
		}
		// System.out.println("ECG checks: "+checks);
		return returnSet;
	}

	@Override
	public List<Double> findBeats(double[] data, double[] ticks) {
		returnTimes.clear();
		this.data = data;
		this.ticksD = ticks;
		bufferPos = 0;
		prevTime = 0;
		try {
			fillBuffers(); // fill front, back, minimum and sample buffer with values
			setInternalValues();
			prevTime = Double.NEGATIVE_INFINITY;
			while (true) {
				if (monitor != null) {
					monitor.setProgress((int) (curTime / 1000000));
				}
				try {
					findBeatsFromLocalOptima(highThreshold);
					clearOptima();
					fillNextBatchOfLocalOptima();
				} catch (NoBeatFound e) {
					try {
						findBeatsFromLocalOptima(lowThreshold);
						clearOptima();
						fillNextBatchOfLocalOptima();
					} catch (NoBeatFound f) {
						setInternalValues();
					}
				}
			}
		} catch (EndOfECGDataException e) {
			try {
				findBeatsFromLocalOptima(lowThreshold);
			} catch (NoBeatFound e1) {
			}
			clearOptima(true);

		}
		return returnTimes;
	}

	private void findBeatsFromLocalOptima(double threshold) throws NoBeatFound {
		double curScore;
		boolean beatFound = false;
		for (LocalECGOptimum opt : opts) {
			curScore = getScoreFromOptimum(opt);
			if (opt.getTime() < prevTime + 200000) {
				if (curScore > prevScore) {
					if (isLive) {
						if (returnTimes.size() > 0)
							returnTimes.remove(returnTimes.get(returnTimes.size() - 1));
					} else
						returnSet.remove(returnSet.last());
					peakToPeak10.stepBack();
					slopeUp10.stepBack();
					slopeDown10.stepBack();
					ibi10.stepBack();
					addOptimumAsBeat(opt, curScore);
					beatFound = true;
				}
			} else {
				if (curScore > threshold) {
					addOptimumAsBeat(opt, curScore);
					beatFound = true;
				}
			}
		}
		if (beatFound == false)
			throw new NoBeatFound("No beat found");
	}

	private double getNextValue() throws EndOfECGDataException {
		if (isLive) {
			if (bufferPos >= data.length)
				throw new EndOfECGDataException("EOB during getNextValue");
			double nextValue = data[bufferPos];
			if (useTicks) {
				curTime = startTime + (ticksD[bufferPos] - ticksD[0]) * 1000.0;
				sampleTicks.add(curTime);
			} else
				curTime += sampleTimeInUS;
			bufferPos++;
			return nextValue;
		} else {
			try {
				if (bufferPos >= nRead) { // bufferPos=524288 and nRead = 524288
					bb.clear(); // byte buffer
					sb.clear(); // long buffer
					nRead = fc.read(bb);
					if (nRead < 4)
						throw new EndOfECGDataException("EOD during getNextValue");
					nRead /= 4;
					sb.get(buffer, 0, nRead);
					bufferPos = 0;
					if (useTicks) {
						bb.clear(); // byte buffer
						sb.clear(); // long buffer
						nRead = fcT.read(bb);
						// System.out.println(fc.position() + " " + fcT.position());
						if (nRead < 4)
							throw new EndOfECGDataException("EOD during getNextValue");
						nRead /= 4;
						sb.get(ticks, 0, nRead);
					}
				}
				int nextValue = buffer[bufferPos];
				if (useTicks) {
					curTime = ticks[bufferPos] * 1000.0;
					sampleTicks.add(curTime);
				} else
					curTime += sampleTimeInUS;
				bufferPos++;
				curOffset++;
				if (curTime > rightTime)
					throw new EndOfECGDataException("EOT during getNextValue");
				return nextValue;
			} catch (IOException e) {
				throw new EndOfECGDataException("IOException during getNextValue");
			}
		}
	}

	private double getScoreFromOptimum(LocalECGOptimum opt) {

		double peakToPeakScore = opt.getPeakToPeakHeight() / avPeakToPeak;
		if (peakToPeakScore > 1)
			peakToPeakScore = 1;
		double slopeUpScore = opt.getSlopeUp() / avSlopeUp;
		if (slopeUpScore > 1)
			slopeUpScore = 1;
		double slopeDownScore = opt.getSlopeDown() / avSlopeDown;
		if (slopeDownScore > 1)
			slopeDownScore = 1;
		double retScore = (wPeakToPeakHeight * peakToPeakScore + wSlopeUp * slopeUpScore + wSlopeDown * slopeDownScore)
				/ wSum;
		return retScore;
	}

	private void initializeOffsets() {
		double leftOffset = ((leftTime - startTime) / sampleTimeInUS);
		double rightOffset = ((rightTime - startTime) / sampleTimeInUS);

		lOffset = (long) Math.floor(leftOffset);
		rOffset = (long) Math.ceil(rightOffset) + 1;

		if (lOffset < 0)
			lOffset = 0;
		if (rOffset >= totalNumberOfShorts)
			rOffset = totalNumberOfShorts - 1;

		if (useTicks) {
			lOffset = correctForTicks(lOffset);
			rOffset = correctForTicks(rOffset);
			curOffset = lOffset;
			curTime = leftTime - (nSamplesIn50ms + 1) * sampleTimeInUS;
			prevTime = leftTime;
		} else {
			curOffset = lOffset;
			curTime = startTime + (curOffset - nSamplesIn50ms - 1) * sampleTimeInUS;
			prevTime = startTime + curOffset * sampleTimeInUS;
		}
	}

	private void openInputStream() throws EndOfECGDataException {
		try {
			is = new FileInputStream(ecgFile);
			fc = is.getChannel();
			fc.position(4 * lOffset); // Skipping of bytes
			if (useTicks) {
				isT = new FileInputStream(ticksFile);
				fcT = isT.getChannel();
				fcT.position(4 * lOffset); // Skipping of bytes
				// System.out.println(fc.position() + " " + fcT.position());
			}
		} catch (IOException e) {
			throw new EndOfECGDataException("EOD during openInputStream");
		}
	}

	public static String removeExtension(String fname) {
		int pos = fname.lastIndexOf('.');
		if (pos > -1)
			return fname.substring(0, pos);
		else
			return fname;
	}

	private void setInternalValues() throws EndOfECGDataException {
		boolean beatFound = false;
		while (beatFound == false) {
			while (curTime - prevTime < 3000000) {
				checkForLocalOptimum(); // will add curTime, peak to peak height, slopeup and slopedown to opts.
				updateBuffers();
			}
			ArrayList<LocalECGOptimum> delOpts = new ArrayList<LocalECGOptimum>();
			for (LocalECGOptimum opt : opts) {
				if (opt.getTime() < prevTime + 200000) // 0.2 seconds // if the CUrrent time in milli seconds and the
														// opt time is less than 0.2 seconds
					delOpts.add(opt);
			}
			opts.removeAll(delOpts);
			if (opts.isEmpty() == false) {
				double maxPeakToPeak = Double.NEGATIVE_INFINITY, maxSlopeUp = Double.NEGATIVE_INFINITY,
						maxSlopeDown = Double.POSITIVE_INFINITY;
				for (LocalECGOptimum opt : opts) {
					if (opt.getPeakToPeakHeight() > maxPeakToPeak)
						maxPeakToPeak = opt.getPeakToPeakHeight();
					if (opt.getSlopeUp() > maxSlopeUp)
						maxSlopeUp = opt.getSlopeUp();
					if (opt.getSlopeDown() < maxSlopeDown)
						maxSlopeDown = opt.getSlopeDown();
				}
				double maxScore = Double.NEGATIVE_INFINITY;
				double curScore;
				avPeakToPeak = maxPeakToPeak;
				avSlopeUp = maxSlopeUp;
				avSlopeDown = maxSlopeDown;
				for (LocalECGOptimum opt : opts) {
					curScore = getScoreFromOptimum(opt);
					if (curScore > maxScore) { // maximum score is 1.
						maxScore = curScore;
						maxPeakToPeak = opt.getPeakToPeakHeight();
						maxSlopeUp = opt.getSlopeUp();
						maxSlopeDown = opt.getSlopeDown();
						beatFound = true;
					}
				}
				avPeakToPeak = maxPeakToPeak;
				avSlopeUp = maxSlopeUp;
				avSlopeDown = maxSlopeDown;
			}
			if (beatFound == false)
				prevTime = curTime;
		}
		avIBI = 2000000;
		avPeakToPeak *= 0.75;
		avSlopeUp *= 0.75;
		avSlopeDown *= 0.75;
		for (int i = 0; i < 10; i++) {
			peakToPeak10.add(avPeakToPeak);
			slopeUp10.add(avSlopeUp);
			slopeDown10.add(avSlopeDown);
			ibi10.add(avIBI);
		}
	}

	@Override
	public void setMonitor(ProgressMonitor mon) {
		this.monitor = mon;
	}

	private void updateBuffers() throws EndOfECGDataException {
		backBuffer.add(samples.get(nSamplesIn15ms));
		frontBuffer.add(samples.get(2 * nSamplesIn15ms + 1));
		double newVal = getNextValue();
		// minBuffer.remove(samples.get(nSamplesIn15ms + 1));
		minBuffer.add(newVal);
		// samples.remove(0);
		samples.add(newVal);
	}

	private Long correctForTicks(long offset) {
		RandomAccessFile is = null;
		long tick, diff, oldOffset = offset;
		long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		try {
			is = new RandomAccessFile(ticksFile, "r");
			long len = is.length() / 4;
			if (offset > len - 1)
				offset = len - 1;
			if (offset < 0)
				offset = 0;
			is.seek(4 * offset);
			tick = is.readInt() - startTime;
			diff = tick - offset;
			offset -= diff;
			long diff2 = tick - oldOffset;
			if (Math.abs(diff) < 100000) {
				int loopCount = 0;
				while (Math.abs(diff2) > 1 && loopCount < 1000) {
					if (offset < 0 || 4 * offset >= len * 4) {
						break;
					}
					is.seek(4 * offset);
					tick = is.readInt() - startTime;
					diff2 = tick - oldOffset;
					offset -= diff2;
					loopCount++;
				}
				if (loopCount > 2)
					System.out.println("correct ticks qrs: " + loopCount + " " + diff);
			} else {
				// binary search
				long low = 0, high = len - 1, mid = 0;
				while (low <= high) {
					mid = low + (high - low) / 2;
					is.seek(4 * mid);
					tick = (is.readInt() - startTime);

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
					long tick1 = (is.readInt() - startTime);
					if (high < 0)
						high = 0;
					if (high > len - 1)
						high = len - 1;
					is.seek(4 * high);
					long tick2 = (is.readInt() - startTime);
					if (oldOffset - tick1 < tick2 - oldOffset)
						offset = low;
					else
						offset = high;
				}
			}
			if (offset < 0)
				offset = 0;
			if (offset >= len * 4)
				offset = len * 4 - 1;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return offset;
	}
}
