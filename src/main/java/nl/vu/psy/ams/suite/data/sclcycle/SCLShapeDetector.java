package nl.vu.psy.ams.suite.data.sclcycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
//import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.SCLCycle;
import nl.vu.psy.ams.suite.tools.ProgressInterface;
import nl.vu.psy.ams.suite.tools.RingBuffer;

public class SCLShapeDetector implements SCLDetector {

	private class EndOfSCLDataException extends Exception {

		private static final long serialVersionUID = 1L;

		public EndOfSCLDataException(String message) {
			super(message);
		}

	}

	private class LocalSCLOptimum {

		private boolean stimulusapplied = false;
		private double time;
		private String type;
		// private double SCRValue;
		// private double p2pH;

		// protected LocalSCLOptimum( boolean stimulus, double time, double value,
		// double peakToPeakHeight, double slopeUp, double slopeDown, String type) {
		protected LocalSCLOptimum(boolean stimulus, double time, double slopeUp, double slopeDown, String type) {
			this.stimulusapplied = stimulus;
			this.time = time;
			// this.SCRValue = value;
			this.type = type;
			// this.p2pH = peakToPeakHeight;
		}

		public boolean isStimulusApplied() {
			return stimulusapplied;
		}

		public double getTime() {
			return time;
		}

		public double getSCRValue() {
			return 0; // SCRValue;
		}

		/*
		 * public double getp2pH() {
		 * return p2pH;
		 * }
		 */
		public String getType() {
			return type;
		}
	}

	private class NoCycleFound extends Exception {
		private static final long serialVersionUID = 1L;

		public NoCycleFound(String message) {
			super(message);
		}
	}

	private double leftTime, rightTime;
	private long lOffset, rOffset, curOffset;

	private ArrayList<LocalSCLOptimum> opts = new ArrayList<LocalSCLOptimum>();
	private SortedSet<SCLCycle> returnSet = new TreeSet<SCLCycle>();

	private double curTime;
	private long startTime;
	private long sampleTimeInUS;

	private File sclFile;
	private long totalNumberOfShorts;

	private FileInputStream is;
	private FileChannel fc;
	private int nSamplesIn15ms;
	private int nSamplesIn50ms;

	private RingBuffer backBuffer;
	private RingBuffer frontBuffer;
	private RingBuffer minBuffer;
	private RingBuffer samples;

	private double[] minmax = new double[2];
	private double[] vals = new double[3];

	// private double lasttime;
	private double onsettime;
	private double peaktime;
	private double prevTime;
	// private double p2pscore;
	// private double currentp2pscore;
	// private double highThreshold;

	// private ProgressMonitor monitor = null;
	private static int size = 1048576;
	private ByteBuffer bb = ByteBuffer.allocate(size);
	private IntBuffer sb = bb.asIntBuffer();
	private DoubleBuffer db = bb.asDoubleBuffer();

	private int[] buffer = new int[size / 4];
	private double[] bufferd = new double[size / 4];

	private int bufferPos = size / 4; // 524288 262144

	private int nRead = size / 4; // 524288
	// private int checks = 0;
	private double slopeUp, slopeDown; // peakToPeakHeight;
	private boolean is7fs = false;

	public SCLShapeDetector() {
		this(CurrentOpenData.getInstance().getStartTimeInUS(), 100000,
				new File(CurrentOpenData.getInstance().getFilePath(), "FILTSCL.bin"));
	}

	public SCLShapeDetector(long startTime, long sampleTimeInUS, File ecgFile) {

		this.startTime = startTime; // start time in milliseconds
		this.sampleTimeInUS = sampleTimeInUS; // 100000
		this.sclFile = ecgFile; // .bin file

		nSamplesIn15ms = (int) (1500000 / sampleTimeInUS); // 15
		nSamplesIn50ms = (int) (5000000 / sampleTimeInUS); // 50
		backBuffer = new RingBuffer(nSamplesIn15ms); // 15
		frontBuffer = new RingBuffer(nSamplesIn15ms);// 15
		minBuffer = new RingBuffer(nSamplesIn50ms);// 50
		samples = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
		totalNumberOfShorts = ecgFile.length() / 4;
		if (ecgFile.getName().endsWith("dbin")) {
			is7fs = true;
			totalNumberOfShorts = ecgFile.length() / 8;
		}
	}

	private void addOptimumAsCycle(LocalSCLOptimum opt) {

		prevTime = opt.getTime();
		if (returnSet.isEmpty()) {
			// lasttime = 0;
			onsettime = 0;
			peaktime = 0;
			// p2pscore = 0;
			// currentp2pscore = 0;
		}

		if (opt.getType().equals("Onset")) {
			if (((opt.getTime() - onsettime) > 5000000)) {
				onsettime = opt.getTime();
				// p2pscore = opt.getSCRValue();
				// currentp2pscore = opt.getp2pH();
				// System.out.println("Difference:_ " + (currentp2pscore-p2pscore));
				SCLCycle cycle = new SCLCycle(opt.getTime());
				cycle.setStimulus(opt.isStimulusApplied());
				cycle.setOnset(true);
				cycle.setPeak(false);
				cycle.setLatencyTime(-9999);
				cycle.setOnsetTime(opt.getTime());
				cycle.setPeakTime(-9999);
				cycle.setOnsetValue(opt.getSCRValue());
				cycle.setPeakValue(-9999);
				cycle.setSCLValue(opt.getSCRValue());
				returnSet.add(cycle);
			}
		} else {
			if (((opt.getTime() - peaktime) > 5000000)) {
				peaktime = opt.getTime();
				// currentp2pscore = opt.getSCRValue();
				// System.out.println("2ndDifference:_ " + opt.getTime() + "------"+
				// (currentp2pscore-p2pscore));
				SCLCycle cycle = new SCLCycle(opt.getTime());
				cycle.setStimulus(opt.isStimulusApplied());
				cycle.setOnset(false);
				cycle.setPeak(true);
				cycle.setLatencyTime(-9999);
				cycle.setOnsetTime(-9999);
				cycle.setPeakTime(opt.getTime());
				cycle.setOnsetValue(-9999);
				cycle.setPeakValue(opt.getSCRValue());
				cycle.setSCLValue(opt.getSCRValue());
				returnSet.add(cycle);
			}
			// p2pscore = currentp2pscore;
		}

	}

	private void checkForLocalOptimum() {
		String typeonset = "Onset";
		String typepeak = "Peak";
		boolean stimuluspresent = false;
		// BinaryFile bf = new BinaryFile("FILTSCL");
		samples.get3(vals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
		if (vals[1] > vals[2] && vals[1] >= vals[0]) { // Local optimum // If the middle point is greater than first and
														// second point
			slopeUp = backBuffer.getSlope();
			if (slopeUp > 0) {
				slopeDown = frontBuffer.getSlope();
				if (slopeDown < 0) {
					minBuffer.getMinMax(minmax);
					// peakToPeakHeight = vals[1] - minmax[0];
					// opts.add(new LocalSCLOptimum(stimuluspresent,curTime,
					// bf.getRealValueFromSampleValue(vals[1]),peakToPeakHeight, slopeUp, slopeDown,
					// typepeak)); // opts - Arraylist of LocalOptimum
					opts.add(new LocalSCLOptimum(stimuluspresent, curTime, slopeUp, slopeDown, typepeak)); // opts -
																											// Arraylist
																											// of
																											// LocalOptimum
				}
			}
		} else if (vals[1] < vals[2] && vals[1] <= vals[0]) {
			slopeUp = backBuffer.getSlope();
			if (slopeUp < 0) {
				slopeDown = frontBuffer.getSlope();
				if (slopeDown > 0) {
					minBuffer.getMinMax(minmax);
					// peakToPeakHeight = vals[1] - minmax[0];
					// opts.add(new
					// LocalSCLOptimum(stimuluspresent,curTime,bf.getRealValueFromSampleValue(vals[1]),
					// peakToPeakHeight, slopeUp, slopeDown, typeonset)); // opts - Arraylist of
					// LocalOptimum
					opts.add(new LocalSCLOptimum(stimuluspresent, curTime, slopeUp, slopeDown, typeonset)); // opts -
																											// Arraylist
																											// of
																											// LocalOptimum
				}
			}
		}
		// checks++;
	}

	private void clearOptima() {
		ArrayList<LocalSCLOptimum> toBeDeleted = new ArrayList<LocalSCLOptimum>(opts.size());
		for (LocalSCLOptimum o : opts) {
			if (o.getTime() < prevTime + 1)
				toBeDeleted.add(o);
		}
		for (LocalSCLOptimum o : toBeDeleted)
			opts.remove(o);
	}

	private void closeInputStream() {
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void fillBuffers() throws EndOfSCLDataException {
		double curVal;
		for (int i = 0; i < 15; i++) { // i<15
			curVal = getNextValue();
			backBuffer.add(curVal); // ring buffer of 15
			samples.add(curVal); // ring buffer of 66
		}
		samples.add(getNextValue());
		for (int i = 0; i < 15; i++) {
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

	private void fillNextBatchOfLocalOptima() throws EndOfSCLDataException {
		checkForLocalOptimum();
		updateBuffers();
	}

	@Override
	public SortedSet<SCLCycle> findCycle(double leftTime, double rightTime) {
		return findCycle(leftTime, rightTime, null);
	}

	@Override
	public SortedSet<SCLCycle> findCycle(double leftTime, double rightTime, ProgressInterface pi) {

		returnSet.clear();
		this.leftTime = leftTime;
		this.rightTime = rightTime;
		initializeOffsets(); // Initiates curOffset, curTime and prevTime

		try {
			openInputStream(); // open the .bin file
			fillBuffers(); // fill front, back, minimum and sample buffer with values
			setInternalValues();
			prevTime = Double.NEGATIVE_INFINITY;
			// double startTime = curTime;
			while (true) {
				/*
				 * if (monitor != null) {
				 * monitor.setProgress((int) (curTime / 1000000));
				 * if (monitor.isCanceled())
				 * return new TreeSet<SCLCycle>();
				 * }
				 */
				if (pi != null)
					pi.progressUpdated((int) (curTime / 1000000));
				try {
					findCycleFromLocalOptima();
					clearOptima();
					fillNextBatchOfLocalOptima();
				} catch (NoCycleFound e1) {
					setInternalValues();
				}
			}
		} catch (EndOfSCLDataException e) {
			try {
				findCycleFromLocalOptima();
			} catch (NoCycleFound e1) {
			}
			clearOptima();

		} finally {
			closeInputStream();
		}
		// System.out.println("SCL checks: "+checks);
		return returnSet;
	}

	private void findCycleFromLocalOptima() throws NoCycleFound {

		// double rtimecurrent=0;
		boolean cycleFound = false;
		for (LocalSCLOptimum opt : opts) {
			addOptimumAsCycle(opt);
			// rtimecurrent = opt.getTime();
			cycleFound = true;
		}

		if (cycleFound == false)
			throw new NoCycleFound("No cycle found");
	}

	private double getNextValue() throws EndOfSCLDataException {

		try {
			if (bufferPos >= nRead) { // bufferPos=524288 and nRead = 524288
				bb.clear(); // byte buffer
				sb.clear(); // long buffer
				db.clear();
				nRead = fc.read(bb);
				if (is7fs) {
					if (nRead < 8)
						throw new EndOfSCLDataException("EOD during getNextValue");
					nRead /= 8;
					db.get(bufferd, 0, nRead);
				} else {
					if (nRead < 4)
						throw new EndOfSCLDataException("EOD during getNextValue");
					nRead /= 4;
					sb.get(buffer, 0, nRead);
				}
				bufferPos = 0;
			}
			// long nextValue = is.readShort();
			int nextValue = buffer[bufferPos];
			double nextValued = bufferd[bufferPos];
			bufferPos++;
			curOffset++;
			curTime += sampleTimeInUS;
			if (curOffset > rOffset) // rOffset is half of the length of ecg.bin file
				throw new EndOfSCLDataException("EOT during getNextValue");
			if (is7fs)
				return nextValued;
			else
				return nextValue;
		} catch (IOException e) {
			throw new EndOfSCLDataException("IOException during getNextValue");
		}
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

		curOffset = lOffset;
		curTime = startTime + (curOffset - nSamplesIn50ms - 1) * sampleTimeInUS;
		prevTime = startTime + curOffset * sampleTimeInUS;
	}

	private void openInputStream() throws EndOfSCLDataException {
		try {
			is = new FileInputStream(sclFile);
			fc = is.getChannel();
			if (is7fs)
				fc.position(8 * lOffset);
			else
				fc.position(4 * lOffset); // Skipping of bytes
		} catch (IOException e) {
			throw new EndOfSCLDataException("EOD during openInputStream");
		}
	}

	private void setInternalValues() throws EndOfSCLDataException {

		// boolean cycleFound = false;

		// while (cycleFound == false) {
		while (curTime - prevTime < 30000000) { // 30 seconds
			checkForLocalOptimum(); // will add curTime, peak to peak height, slopeup and slopedown to opts.
			updateBuffers();
		}
		ArrayList<LocalSCLOptimum> delOpts = new ArrayList<LocalSCLOptimum>();
		for (LocalSCLOptimum opt : opts) {
			if (opt.getTime() < prevTime + 200000) // 0.2 seconds // if the CUrrent time in milli seconds and the opt
													// time is less than 0.2 seconds
				delOpts.add(opt);
		}
		// System.out.println(delOpts.size() +" " + opts.size());

		opts.removeAll(delOpts);
		// System.out.println(opts.size());
		// if (cycleFound == false)
		prevTime = curTime;
		// }

	}

	@Override
	public void setMonitor(ProgressMonitor mon) {
		// this.monitor = mon;
	}

	private void updateBuffers() throws EndOfSCLDataException {
		backBuffer.add(samples.get(nSamplesIn15ms));
		frontBuffer.add(samples.get(2 * nSamplesIn15ms + 1));
		double newVal = getNextValue();
		// minBuffer.remove(samples.get(nSamplesIn15ms + 1));
		minBuffer.add(newVal);
		// samples.remove(0);
		samples.add(newVal);
	}

}
