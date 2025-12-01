package nl.vu.psy.ams.suite.data.motility;

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
import nl.vu.psy.ams.suite.data.structures.Step;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.tools.RingBuffer;

/* Class that detects steps based on accelerometry */

public class StepShapeDetector implements StepDetector {

    private class EndOfDataException extends Exception {

        private static final long serialVersionUID = 1L;

        public EndOfDataException(String message) {
            super(message);
        }

    }

    private double leftTime, rightTime;
    private long lOffset, rOffset, curOffset;
    private double curTime;
    private long startTime;
    private long sampleTimeInUS;

    private File xFile, yFile, zFile, ticksFile;
    private long totalNumberOfShorts;

    private FileInputStream isX = null, isY = null, isZ = null, isT = null;
    private FileChannel fcX = null, fcY = null, fcZ = null, fcT = null;
    private int nSamplesIn15ms;
    private int nSamplesIn50ms;
    private RingBuffer backBuffer;

    private RingBuffer frontBuffer;
    private RingBuffer minBuffer;
    private RingBuffer samplesX, samplesM, sampleTicks;

    private SortedSet<Step> returnSet = new TreeSet<Step>();
    private List<Double> returnTimes = new ArrayList<Double>();

    private double[] xvals = new double[3];
    private double[] mvals = new double[3];
    private double[] tvals = new double[3];
    private double prevTime, prevAngle;
    private ProgressMonitor monitor = null;
    private static int size = 1048576;
    private ByteBuffer bb = ByteBuffer.allocate(size);
    private IntBuffer sb = bb.asIntBuffer();

    private int[] bufferX = new int[size / 4];
    private int[] bufferY = new int[size / 4];
    private int[] bufferZ = new int[size / 4];
    private int[] ticks = new int[size / 4];

    private int bufferPos = size / 4; // 524288 262144

    private int nRead = size / 4; // 524288
    private boolean useTicks = false;
    int STEP_THRESHOLD = 100; // degrees
    double VECTOR_MAGNITUDE_THRESHOLD = 0.02; // m/s^2
    double PHASE_RATE_THRESHOLD = 90.0; // degrees per second
    double samplingInterval = 1.0 / 1000.0; // 1000 Hz
    long MIN_STEP_INTERVAL_US = 300_000; // 300 ms in microseconds
    double slope = 1;

    public StepShapeDetector() {
        this(new File(CurrentOpenData.getInstance().getFilePath(), "FILTstepMXR.bin"),
                new File(CurrentOpenData.getInstance().getFilePath(), "FILTstepMYR.bin"),
                new File(CurrentOpenData.getInstance().getFilePath(), "FILTstepMZR.bin"),
                new File(CurrentOpenData.getInstance().getFilePath(), "TicksM.bin"), false, false);
    }

    public StepShapeDetector(File xFile, File yFile, File zFile, File ticksFile, boolean isLive, boolean liveTicks) {
        startTime = CurrentOpenData.getInstance().getStartTimeInUS();
        sampleTimeInUS = 1000;
        this.ticksFile = ticksFile; // .bin file
        this.xFile = xFile; // .bin file
        this.yFile = yFile; // .bin file
        this.zFile = zFile;
        if ((!isLive && ticksFile.exists()) || liveTicks)
            useTicks = true;
        nSamplesIn15ms = (int) (15000 / sampleTimeInUS); // 15
        nSamplesIn50ms = (int) (50000 / sampleTimeInUS); // 50
        backBuffer = new RingBuffer(nSamplesIn15ms); // 15
        frontBuffer = new RingBuffer(nSamplesIn15ms);// 15
        minBuffer = new RingBuffer(nSamplesIn50ms);// 50
        samplesX = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
        samplesM = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
        sampleTicks = new RingBuffer(nSamplesIn15ms + nSamplesIn50ms + 1);// 66
        if (!isLive)
            totalNumberOfShorts = xFile.length() / 4;
        try {
            Ams7fsChannelInfo chanM;
            chanM = CurrentOpenData.getInstance().getChannelInfoFromID("MXR");
            slope = chanM.getRealSlope();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addOptimumAsBeat(double time) {

        prevTime = time;
        CurrentOpenData.getInstance().getStepSet().addStep(time);
    }

    private void checkForLocalOptimum() {
        samplesX.get3(xvals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
        samplesM.get3(mvals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
        if (useTicks)
            sampleTicks.get3(tvals, nSamplesIn15ms - 1); // returns vals[0], vals[1], vals[2]
        double angle = Math.atan2(xvals[0], (xvals[1] - xvals[0])) * 180 / Math.PI;
        double phaseRate = (angle - prevAngle) / samplingInterval; // deg/sec
        if (angle > STEP_THRESHOLD &&
                prevAngle <= STEP_THRESHOLD) {
            // System.out.println(mvals[0] + " " + tvals[1] / 1000);
            if (mvals[0] >= VECTOR_MAGNITUDE_THRESHOLD &&

                    Math.abs(phaseRate) >= PHASE_RATE_THRESHOLD &&
                    (prevTime == Double.NEGATIVE_INFINITY || (curTime - prevTime) >= MIN_STEP_INTERVAL_US)) {
                if (useTicks) {
                    addOptimumAsBeat(tvals[1]);
                } else
                    addOptimumAsBeat(curTime);
            }
        }
        prevAngle = angle;
    }

    private void closeInputStream() {
        try {
            if (isX != null)
                isX.close();
            if (isY != null)
                isY.close();
            if (isZ != null)
                isZ.close();
            if (isT != null)
                isT.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillBuffers() throws EndOfDataException {
        double[] curVal;
        for (int i = 0; i < nSamplesIn15ms; i++) { // i<15
            curVal = getNextValues(); // getNextValue() returns an int
            backBuffer.add(curVal[0]); // ring buffer of 15
            samplesX.add(curVal[0]); // ring buffer of 66
            samplesM.add(curVal[1]);
            if (i == 1) {
                prevAngle = Math.atan2(samplesX.get(0), (curVal[0] - samplesX.get(0))) * 180 / Math.PI;
            }
        }
        samplesX.add(getNextValues()[0]);
        samplesM.add(getNextValues()[1]);
        for (int i = 0; i < nSamplesIn15ms; i++) {
            curVal = getNextValues();
            frontBuffer.add(curVal[0]); // ring buffer of 15
            samplesX.add(curVal[0]);
            samplesM.add(curVal[1]);
            minBuffer.add(curVal[0]); // ring buffer of 50
        }
        for (int i = 0; i < nSamplesIn50ms - nSamplesIn15ms; i++) {
            curVal = getNextValues();
            samplesX.add(curVal[0]);
            samplesM.add(curVal[1]);
            minBuffer.add(curVal[0]);
        }
    }

    private void fillNextBatchOfLocalOptima() throws EndOfDataException {
        while (true) {
            checkForLocalOptimum();
            updateBuffers();
            if (monitor != null)
                monitor.setProgress((int) (curTime / 1000000));
        }
    }

    @Override
    public SortedSet<Step> findSteps(double leftTime, double rightTime) {

        returnSet.clear(); // SortedSet of ECG
        this.leftTime = leftTime;
        this.rightTime = rightTime;
        initializeOffsets(); // Initiates curOffset, curTime and prevTime

        try {
            openInputStream(); // open the .bin file
            fillBuffers(); // fill front, back, minimum and sample buffer with values
            prevTime = Double.NEGATIVE_INFINITY;
            fillNextBatchOfLocalOptima();
        } catch (EndOfDataException e) {
        } finally {
            closeInputStream();
        }
        // System.out.println("ECG checks: "+checks);
        return returnSet;
    }

    @Override
    public List<Double> findSteps(double[] data, double[] ticks) {
        // returnTimes.clear();
        // this.data = data;
        // this.ticksD = ticks;
        // bufferPos = 0;
        // prevTime = 0;
        // try {
        // fillBuffers(); // fill front, back, minimum and sample buffer with values
        // setInternalValues();
        // prevTime = Double.NEGATIVE_INFINITY;
        // while (true) {
        // if (monitor != null) {
        // monitor.setProgress((int) (curTime / 1000000));
        // }
        // try {
        // findBeatsFromLocalOptima(highThreshold);
        // clearOptima();
        // fillNextBatchOfLocalOptima();
        // } catch (NoBeatFound e) {
        // try {
        // findBeatsFromLocalOptima(lowThreshold);
        // clearOptima();
        // fillNextBatchOfLocalOptima();
        // } catch (NoBeatFound f) {
        // setInternalValues();
        // }
        // }
        // }
        // } catch (EndOfDataException e) {
        // try {
        // findBeatsFromLocalOptima(lowThreshold);
        // } catch (NoBeatFound e1) {
        // }
        // clearOptima(true);

        // }
        return returnTimes;
    }

    private double[] getNextValues() throws EndOfDataException {
        // if (isLive) {
        // if (bufferPos >= data.length)
        // throw new EndOfDataException("EOB during getNextValue");
        // double nextValue = data[bufferPos];
        // if (useTicks) {
        // curTime = startTime + (ticksD[bufferPos] - ticksD[0]) * 1000.0;
        // sampleTicks.add(curTime);
        // } else
        // curTime += sampleTimeInUS;
        // bufferPos++;
        // return nextValue;
        // } else {
        try {
            if (bufferPos >= nRead) { // bufferPos=524288 and nRead = 524288
                bb.clear(); // byte buffer
                sb.clear(); // long buffer
                nRead = fcX.read(bb);
                if (nRead < 4)
                    throw new EndOfDataException("EOD during getNextValue");
                nRead /= 4;
                sb.get(bufferX, 0, nRead);
                bb.clear(); // byte buffer
                sb.clear(); // long buffer
                nRead = fcY.read(bb);
                if (nRead < 4)
                    throw new EndOfDataException("EOD during getNextValue");
                nRead /= 4;
                sb.get(bufferY, 0, nRead);
                bb.clear(); // byte buffer
                sb.clear(); // long buffer
                nRead = fcZ.read(bb);
                if (nRead < 4)
                    throw new EndOfDataException("EOD during getNextValue");
                nRead /= 4;
                sb.get(bufferZ, 0, nRead);
                bufferPos = 0;
                if (useTicks) {
                    bb.clear(); // byte buffer
                    sb.clear(); // long buffer
                    nRead = fcT.read(bb);
                    // System.out.println(fc.position() + " " + fcT.position());
                    if (nRead < 4)
                        throw new EndOfDataException("EOD during getNextValue");
                    nRead /= 4;
                    sb.get(ticks, 0, nRead);
                }
            }
            double[] nextValues = new double[2];
            nextValues[0] = bufferX[bufferPos] * slope;
            nextValues[1] = Math.sqrt(bufferX[bufferPos] * slope * bufferX[bufferPos] * slope
                    + bufferY[bufferPos] * slope * bufferY[bufferPos] * slope
                    + bufferZ[bufferPos] * slope * bufferZ[bufferPos] * slope);
            if (useTicks) {
                curTime = ticks[bufferPos] * 1000.0;
                sampleTicks.add(curTime);
            } else
                curTime += sampleTimeInUS;
            bufferPos++;
            curOffset++;
            if (curTime > rightTime)
                throw new EndOfDataException("EOT during getNextValue");
            return nextValues;
        } catch (IOException e) {
            throw new EndOfDataException("IOException during getNextValue");
        }
        // }
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
            // prevTime = leftTime;
        } else {
            curOffset = lOffset;
            curTime = startTime + (curOffset - nSamplesIn50ms - 1) * sampleTimeInUS;
            // prevTime = startTime + curOffset * sampleTimeInUS;
        }
    }

    private void openInputStream() throws EndOfDataException {
        try {
            isX = new FileInputStream(xFile);
            fcX = isX.getChannel();
            fcX.position(4 * lOffset); // Skipping of bytes
            isY = new FileInputStream(yFile);
            fcY = isY.getChannel();
            fcY.position(4 * lOffset); // Skipping of bytes
            isZ = new FileInputStream(zFile);
            fcZ = isZ.getChannel();
            fcZ.position(4 * lOffset); // Skipping of bytes
            if (useTicks) {
                isT = new FileInputStream(ticksFile);
                fcT = isT.getChannel();
                fcT.position(4 * lOffset); // Skipping of bytes
                // System.out.println(fc.position() + " " + fcT.position());
            }
        } catch (IOException e) {
            throw new EndOfDataException("EOD during openInputStream");
        }
    }

    public static String removeExtension(String fname) {
        int pos = fname.lastIndexOf('.');
        if (pos > -1)
            return fname.substring(0, pos);
        else
            return fname;
    }

    @Override
    public void setMonitor(ProgressMonitor mon) {
        this.monitor = mon;
    }

    private void updateBuffers() throws EndOfDataException {
        backBuffer.add(samplesX.get(nSamplesIn15ms));
        frontBuffer.add(samplesX.get(2 * nSamplesIn15ms + 1));
        double[] newVal = getNextValues();
        // minBuffer.remove(samples.get(nSamplesIn15ms + 1));
        minBuffer.add(newVal[0]);
        // samples.remove(0);
        samplesX.add(newVal[0]);
        samplesM.add(newVal[1]);
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
                    System.out.println("correct ticks steps: " + loopCount + " " + diff);
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
