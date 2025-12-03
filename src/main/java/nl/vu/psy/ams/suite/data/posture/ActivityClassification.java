package nl.vu.psy.ams.suite.data.posture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
import nl.vu.psy.ams.suite.data.files.BinaryFile;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class ActivityClassification extends Thread {

    private static ActivityClassification instance;

    public static ActivityClassification getInstance() {
        if (instance == null) {
            instance = new ActivityClassification();
        }
        return instance;
    }

    public static ActivityClassification getNewInstance() {
        if (instance != null) {
            // instance.interrupt();
            try {
                instance.join(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            instance = null;
        }
        instance = new ActivityClassification();
        return instance;
    }

    private ActivityClassification() {
        this.setPriority(MIN_PRIORITY);
    }

    private static final int size = 1200000; // 1048576; // 2^20

    // Static buffers used by reduceSamplesize (currently unused but kept for
    // potential future use)
    static ByteBuffer bb;
    static IntBuffer sb;
    static ByteBuffer obb;
    static IntBuffer osb;
    static int[] samp;
    static int[] outBuf;

    // State variables for analysis
    // Tracks how many 5 Hz samples we've already written to altSm_debug.txt across
    // chunks
    static long altSmDebugSamplesWritten = 0L;
    static double firstVal;

    static int altitudeCount = 0;
    static double altitudeSum = 0;
    static double avMeanMotility = 0;
    static int lastState = 20;
    static final int windowSizeMotilityEntropy = 4096;
    static int freqAcceleromters = 1000;
    static int freqPressure = 5;

    static ArrayList<Double> altitudeCarryover = new ArrayList<Double>();

    static final int deltaIdx = 2000; // 2 seconds of samples
    static final double thresholdSitStand = 0.1;
    static final double thresholdStairs = 0.15;

    // states
    static final int VERTICAL = 5;
    static final int HORIZONTAL_LYING_ON_BACK = 500; // changed it since now there is vertical sitting and standing
    static final int HORIZONTAL_LYING_ON_BELLY = 100;
    static final int HORIZONTAL_LYING_ON_LEFT_SIDE = 200;
    static final int HORIZONTAL_LYING_ON_RIGHT_SIDE = 300;
    static final int HORIZONTAL_LYING_UNKNOWN = 600; // unknown

    static final int STATIONARY = 55;
    static final int WALKING = 60;
    static final int RUNNING = 70;
    static final int LYING_DOWN = 80;

    // transitions
    static final int LEVEL_GROUND_TO_STAIRS_UP = 90;
    static final int LEVEL_GROUND_TO_STAIRS_DOWN = 100;
    static final int SIT_TO_STAND = 110;
    static final int STAND_TO_SIT = 120;

    // acitivity intensity labels
    static final int ACTIVITY_LOW_INTENSITY = 200;
    static final int ACTIVITY_MEDIUM_INTENSITY = 220;
    static final int ACTIVITY_HIGH_INTENSITY = 240;

    // activity thresholds for RMS calculation
    static final double ACTIVITY_LOW_INTENSITY_THRESHOLD = 1.25;
    static final double ACTIVITY_MEDIUM_INTENSITY_THRESHOLD = 1.55;
    static RandomAccessFile is = null;
    static File tickFile;

    private static final int SAMPLING_FREQUENCY = 1000;
    private static List<Double> allMETsBB = new ArrayList<>();
    private static List<Double> allMETsBNB = new ArrayList<>();
    private static List<Double> allMETsF = new ArrayList<>();
    private static List<Double> allMADxyz = new ArrayList<>();
    private static List<Double> allMADs = new ArrayList<>();
    private static List<Double> allSpeech = new ArrayList<>();
    private static List<String> allPostureLabels = new ArrayList<>();

    @Override
    public void run() {
        try {
            generateMeanMotilityFile();
            MainFrame.getInstance().getMainFrame().repaint();
            generateClassificationFile();
            MainFrame.getInstance().getMainFrame().repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validate that required channels exist in the current open data.
     * 
     * @param channelIds Channel IDs to validate
     * @return true if all channels exist, false otherwise
     */
    private static boolean validateRequiredChannels(String... channelIds) {
        CurrentOpenData cod = CurrentOpenData.getInstance();
        for (String channelId : channelIds) {
            if (!cod.channelExists(channelId)) {
                return false;
            }
        }
        return true;
    }

    public static void generateMeanMotilityFile() throws Exception {
        CurrentOpenData cod = CurrentOpenData.getInstance();

        // Check if required channels exist
        if (!validateRequiredChannels("MXR", "MYR", "MZR")) {
            return;
        }

        File meanMotilityFile = new File(cod.getFilePath(), "StepInstances.bin");
        File meanMotilityFileF = new File(cod.getFilePath(), "FILTStepInstances.bin");

        // Early return if files already exist
        if (meanMotilityFile.exists() && meanMotilityFileF.exists() && cod.channelExists("StepInstances")) {
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(meanMotilityFileF);
            ssf2.start();
            return;
        }

        cod.dirtyFiles.add(meanMotilityFile);
        cod.dirtyFiles.add(meanMotilityFileF);
        tickFile = new File(cod.getFilePath(), "TicksM.bin");

        // Read filtered accelerometer files using helper
        File fAccelX = new File(cod.getFilePath(), "FILTMXR.bin");
        File fAccelY = new File(cod.getFilePath(), "FILTMYR.bin");
        File fAccelZ = new File(cod.getFilePath(), "FILTMZR.bin");

        AccelerometerDataReader.AccelData accelData = AccelerometerDataReader.readAllAsIntegers(
                fAccelX, fAccelY, fAccelZ);

        // Compute magnitude using helper
        int[] meanMotility = AccelerometerDataReader.calculateMagnitude(accelData);

        // Write to binary file
        try (FileOutputStream fos = new FileOutputStream(meanMotilityFile);
                FileChannel channel = fos.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(accelData.numSamples * 4);
            IntBuffer intBuffer = buffer.asIntBuffer();
            intBuffer.put(meanMotility);
            buffer.position(0);
            buffer.limit(accelData.numSamples * 4);
            channel.write(buffer);
        }

        // Register the file
        SubsetFilesSingle ssf1 = new SubsetFilesSingle(meanMotilityFile);
        ssf1.start();

        // Apply high-pass filter
        highPassFilterButt(meanMotilityFile, meanMotilityFileF);
        SubsetFilesSingle ssf2 = new SubsetFilesSingle(meanMotilityFileF);
        ssf2.start();

        // Calculate average motility
        try (BinaryFile bf = new BinaryFile("FILTStepInstances")) {
            avMeanMotility = bf.getAverageBetweenTimes(cod.getStartTimeInUS(), cod.getEndTimeInUS());
        }
    }

    public static double[] centralDifference(double[] input) {
        int size = input.length;
        double[] output = new double[size];

        // Handle the first element (padding)
        output[0] = (input[1] - input[0]);

        // Compute the central difference for the inner elements
        for (int i = 1; i < size - 1; i++) {
            double centralDiff = (input[i + 1] - input[i - 1]) / 2.0;
            output[i] = centralDiff;
        }

        // Handle the last element (padding)
        output[size - 1] = (input[size - 1] - input[size - 2]);

        return output;
    }

    // Get the indexes where sign changes have happened
    public static int[] getSignChanges(double[] input) {
        int size = input.length;
        int[] output = new int[size];

        for (int i = 1; i < size; i++) {
            if (input[i] > 0 && input[i - 1] < 0) {
                output[i] = 1; // slope is positive
            } else if (input[i] < 0 && input[i - 1] > 0) {
                output[i] = -1; // slope is negative
            } else {
                output[i] = 0; // else slope is zero
            }
        }

        return output;
    }

    public static void generateClassificationFile() throws Exception {
        CurrentOpenData cod = CurrentOpenData.getInstance();
        File tempDir = cod.getFilePath();
        boolean pressureAvailable = true;
        if (!validateRequiredChannels("MXR", "MYR", "MZR")) {
            return;
        }
        if (!validateRequiredChannels("P_sc", "T_sc")) {
            pressureAvailable = false;
        }
        File catFile = new File(cod.getFilePath(), "Activity.bin");
        File lyingFile = new File(cod.getFilePath(), "MotilityIntensity.dbin");
        File AltitudeFile = new File(cod.getFilePath(), "Altitude.dbin");

        if (/* catFile.exists() && */ lyingFile.exists() && AltitudeFile.exists() // &&
                                                                                  // cod.channelExists("Activity")
                && cod.channelExists("MotilityIntensity") && cod.channelExists("Altitude")
        /* && !CurrentOpenData.getInstance().getPostureLabels().getLabels().isEmpty() */)
            return;

        Ams7fsChannelInfo chanM = cod.getChannelInfoFromID("MXR");
        if (pressureAvailable) {
            cod.dirtyFiles.add(catFile);
            cod.dirtyFiles.add(lyingFile);
            cod.dirtyFiles.add(AltitudeFile);
        }
        // reduce sample size for accel x,y,z, and gyro x,y,z
        File fAccelX = new File(tempDir, "FILTMXR.bin");
        File fAccelY = new File(tempDir, "FILTMYR.bin");
        File fAccelZ = new File(tempDir, "FILTMZR.bin");
        File temperatureFile = new File(tempDir, "FILTT_sc.dbin");
        File pressureFile = new File(tempDir, "FILTP_sc.dbin");
        if (tickFile.exists())
            is = new RandomAccessFile(tickFile, "r");

        // === DIRECT READING: Load all data at once ===
        // Read all accelerometer data
        AccelerometerDataReader.AccelData allAccelData = AccelerometerDataReader.readAllAsIntegers(
                fAccelX, fAccelY, fAccelZ);

        int[] sampMX = allAccelData.x;
        int[] sampMY = allAccelData.y;
        int[] sampMZ = allAccelData.z;
        int totalSamples = allAccelData.numSamples;

        // Read all pressure/temperature data if available
        double[] allSampTemp = null;
        double[] allSampPres = null;
        if (pressureAvailable) {
            // Pressure/temperature are sampled at 200x slower rate
            int pressureSamples = totalSamples / 200;
            allSampTemp = new double[pressureSamples];
            allSampPres = new double[pressureSamples];

            try (FileInputStream fisTemp = new FileInputStream(temperatureFile);
                    FileChannel ifTemp = fisTemp.getChannel();
                    FileInputStream fisPres = new FileInputStream(pressureFile);
                    FileChannel ifPres = fisPres.getChannel()) {

                ByteBuffer bbP = ByteBuffer.allocateDirect(pressureSamples * 8);
                DoubleBuffer sbP = bbP.asDoubleBuffer();

                // Read temperature
                bbP.clear();
                sbP.clear();
                ifTemp.read(bbP);
                bbP.flip();
                sbP.get(allSampTemp);

                // Read pressure
                bbP.clear();
                sbP.clear();
                ifPres.read(bbP);
                bbP.flip();
                sbP.get(allSampPres);
            }
        }

        // Setup output file channels
        FileOutputStream fos2 = null, fosLying = null, fosAlt = null;
        FileChannel fc2 = null, fcLying = null, fcAlt = null;
        if (pressureAvailable) {
            fos2 = new FileOutputStream(catFile);
            fc2 = fos2.getChannel();
            fosLying = new FileOutputStream(lyingFile);
            fcLying = fosLying.getChannel();
            fosAlt = new FileOutputStream(AltitudeFile);
            fcAlt = fosAlt.getChannel();
        }

        int rawThreshold = AppSettings.getInstance().getIntProperty(Settings.LYINGTHRESHOLD);
        double threshold = (rawThreshold - chanM.getRealConstant()) / chanM.getRealSlope() / 1000.0;
        double YPosThreshold = (400 - chanM.getRealConstant()) / chanM.getRealSlope() / 1000.0;

        // Channel calibration for counts -> m/s^2 conversion
        Ams7fsChannelInfo chanX = cod.getChannelInfoFromID("MXR");
        Ams7fsChannelInfo chanY = cod.getChannelInfoFromID("MYR");
        Ams7fsChannelInfo chanZ = cod.getChannelInfoFromID("MZR");
        CurrentOpenData.getInstance().getPostureLabels().clear();

        // === PROCESS ALL DATA AT ONCE ===
        if (pressureAvailable) {
            // Convert raw counts to physical units (m/s^2)
            double[] dx = MotionDataUtils.toMs2(sampMX, chanX);
            double[] dy = MotionDataUtils.toMs2(sampMY, chanY);
            double[] dz = MotionDataUtils.toMs2(sampMZ, chanZ);

            long globalStartUS = CurrentOpenData.getInstance().getStartTimeInUS();
            int sampleTimeMicros = 1000; // 1000 Hz = 1000 microseconds per sample

            final int FsLocal = SAMPLING_FREQUENCY; // 1000 Hz

            // Detect steps for entire dataset (starts at sample 0)
            int[] stepLocations = StepDetector.getInstance().detectSteps(dx, dy, dz, 0L,
                    ActivityClassification::correctForTicks);

            // Run analyseMotility for entire dataset
            analyseMotility(sampMX, sampMY, sampMZ, stepLocations, allSampTemp, allSampPres, 1, threshold,
                    YPosThreshold,
                    fcLying, fc2, fcAlt, 0L);

            // NOTE: Stairs detection is now handled by StairsClassifier.java

            // NOTE: Posture classification is now handled by PostureClassifier
        }

        // NOTE: Stairs label cleanup is now handled by StairsClassifier.java

        // Close output channels
        if (fc2 != null)
            fc2.close();
        if (fos2 != null)
            fos2.close();
        if (fcLying != null)
            fcLying.close();
        if (fosLying != null)
            fosLying.close();
        if (fcAlt != null)
            fcAlt.close();
        if (fosAlt != null)
            fosAlt.close();

        if (is != null)
            is.close();

        if (pressureAvailable) {
            SubsetFilesSingle ssf1 = new SubsetFilesSingle(catFile);
            ssf1.start();
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(lyingFile);
            ssf2.start();
            SubsetFilesSingle ssf3 = new SubsetFilesSingle(AltitudeFile);
            ssf3.start();
        }
        altitudeSum = 0;
    }

    /**
     * Apply zero-phase 4th-order Butterworth high-pass filter for gravity removal.
     * Equivalent to MATLAB: [b,a] = butter(4, 0.005, 'high')
     * 
     * Uses ZeroPhaseFilter with frequency-dependent padding to avoid settling
     * transients.
     * 
     * @param inFile  Input file containing integer samples
     * @param outFile Output file for filtered samples
     */
    public static void highPassFilterButt(File inFile, File outFile) {
        try {
            // Read all samples from input file
            int numSamples = (int) (inFile.length() / 4); // 4 bytes per int
            int[] samples = new int[numSamples];

            try (FileInputStream fis = new FileInputStream(inFile);
                    FileChannel channel = fis.getChannel()) {
                ByteBuffer buffer = ByteBuffer.allocate(numSamples * 4);
                channel.read(buffer);
                buffer.flip();
                buffer.asIntBuffer().get(samples);
            }

            // Convert int[] to double[] for filtering
            double[] signal = new double[numSamples];
            for (int i = 0; i < numSamples; i++) {
                signal[i] = samples[i];
            }

            // Apply zero-phase high-pass filter
            // ZeroPhaseFilter now uses proper frequency-dependent padding
            final int ORDER = 4;
            final double CUTOFF_HZ = 0.005;
            final int SAMPLING_RATE = 1000; // Hz

            double[] filtered = ZeroPhaseFilter.zeroPhaseHighPassFilterJDSP(
                    signal, ORDER, CUTOFF_HZ, SAMPLING_RATE);

            // Convert back to int[] and write to output file
            int[] output = new int[numSamples];
            for (int i = 0; i < numSamples; i++) {
                output[i] = (int) Math.round(filtered[i]);
            }

            try (FileOutputStream fos = new FileOutputStream(outFile);
                    FileChannel channel = fos.getChannel()) {
                ByteBuffer buffer = ByteBuffer.allocate(numSamples * 4);
                buffer.asIntBuffer().put(output);
                buffer.limit(numSamples * 4);
                channel.write(buffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double[] getAltitudeFromPressure(double[] pressure) {
        double[] altitude = new double[pressure.length];
        for (int j = 0; j < pressure.length; j++) {
            altitude[j] = (pressure[j] - 102000.0) / -12.2;
        }
        return altitude;
    }

    public static double[] diff(double[] array) {
        double[] result = new double[array.length - 1];
        for (int i = 0; i < array.length - 1; i++) {
            result[i] = array[i + 1] - array[i];
        }
        return result;
    }

    // NOTE: saveColumns methods have been moved to MotionDataUtils.saveColumns()
    // Use MotionDataUtils.saveColumns(filePath, overwrite, columns) instead

    public static double analyseMotility(int[] sampMX, int[] sampMY, int[] sampMZ, int[] stepLocations,
            double sampTemp[], double sampPres[],
            int analysisWindowSize, double threshold, double thresholdLow, FileChannel fosLying,
            FileChannel fosCat, FileChannel fosAlt, long startSampleAbsolute) {

        // Use identical calibrated acceleration (counts -> m/s^2) as step detection
        // Get per-axis channel calibration
        Ams7fsChannelInfo chanXLoc = null;
        Ams7fsChannelInfo chanYLoc = null;
        Ams7fsChannelInfo chanZLoc = null;
        try {
            chanXLoc = CurrentOpenData.getInstance().getChannelInfoFromID("MXR");
            chanYLoc = CurrentOpenData.getInstance().getChannelInfoFromID("MYR");
            chanZLoc = CurrentOpenData.getInstance().getChannelInfoFromID("MZR");
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }

        // Find the shortest common length and convert once
        int minLength = Math.min(sampMX.length, Math.min(sampMY.length, sampMZ.length));
        double[] sampleMXDouble = MotionDataUtils.toMs2(java.util.Arrays.copyOf(sampMX, minLength), chanXLoc);
        double[] sampleMYDouble = MotionDataUtils.toMs2(java.util.Arrays.copyOf(sampMY, minLength), chanYLoc);
        double[] sampleMZDouble = MotionDataUtils.toMs2(java.util.Arrays.copyOf(sampMZ, minLength), chanZLoc);

        // **********************************************************************/

        // Use absolute sample index directly, with optional tick correction
        long startTime = startSampleAbsolute;
        if (tickFile != null && tickFile.exists()) {
            startTime = correctForTicks(startTime);
        }

        PhysicalActivityCalculator PAcalc = PhysicalActivityCalculator.getInstance();
        PAcalc.setAccelerometerData(sampleMXDouble, sampleMYDouble, sampleMZDouble);

        // estimate METs using different methods
        List<Double> METsBB = PAcalc.estimateMETs(startTime * 1000,
                PhysicalActivityCalculator.Method.BRAGE_NONBRANCHED);
        List<Double> METsBNB = PAcalc.estimateMETs(startTime * 1000, PhysicalActivityCalculator.Method.BRAGE_BRANCHED);
        List<Double> METsFreedson = PAcalc.estimateMETs(startTime * 1000, PhysicalActivityCalculator.Method.FREEDSON);
        List<Double> MADxyz = PAcalc.calculateMADxyz();
        List<Double> MAD = PAcalc.calculateMAD();
        List<Double> Speech = PAcalc.calculateSpeech(sampleMZDouble);

        // accumulate all results
        for (int i = 0; i < METsBB.size(); i++) {
            allMETsBB.add(METsBB.get(i));
            allMETsBNB.add(METsBNB.get(i));
            allMETsF.add(METsFreedson.get(i));
            allMADxyz.add(MADxyz.get(i));
            allMADs.add(MAD.get(i));
            allSpeech.add(Speech.get(i));
        }

        // NOTE: Posture classification is now handled by PostureClassifier

        // get altitude from pressure
        double[] altitude = getAltitudeFromPressure(sampPres);

        try {
            byte[] outbuf = new byte[size * 2];
            ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
            DoubleBuffer osb = outBuffer.asDoubleBuffer();
            double[] outBuf = new double[size];

            osb.clear();
            for (int j = 0; j < MADxyz.size(); j++) {
                outBuf[j] = MADxyz.get(j); // (int) (motilityIntensity[j] * 1000.0);
            }
            osb.put(outBuf, 0, MADxyz.size());
            outBuffer.limit(MADxyz.size() * 8);
            fosLying.write(outBuffer);
            // fosLying.write(outbuf, 0, MADxyz.size() * 8);
            osb.clear();
            outBuffer.clear();
            for (int k = 0; k < altitude.length; k++) {
                outBuf[k] = (altitude[k]);
            }
            osb.put(outBuf, 0, altitude.length);
            outBuffer.limit(altitude.length * 8);
            fosAlt.write(outBuffer);
            // fosAlt.write(outbuf, 0, altitude.length * 8);
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        return 0.0;
    }

    static private Long correctForTicks(long offset) {
        if (!tickFile.exists()) {
            return offset;
        }
        long tick, diff, oldOffset = offset;
        long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
        try {
            is = new RandomAccessFile(tickFile, "r");
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

    // get the average MET value for the given time range
    // leftTime and rightTime are in ms
    // ! There are multiple instances of MET values out of bounds
    // ! Needs to be fixed
    public static Double getAverageMET(double leftTime, double rightTime) {
        return getAverageMET(leftTime, rightTime, 0);
    }

    public static Double getAverageMET(double leftTime, double rightTime, int method) {
        rightTime = (rightTime / 1000) - CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
        leftTime = (leftTime / 1000) - CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
        double av = 0;
        long ninAv = 0;
        // for (int i = (int) (leftTime + 30000) / 60000; i < (rightTime + 30000) /
        // 60000; i += 60000)
        for (int i = 0; i < allMETsBB.size(); i++)
        // find minutes which are at least half in the label
        {
            int midpoint = i * 60000 + 30000;
            if (midpoint < leftTime)
                continue;
            if (midpoint > rightTime)
                break;
            // if (i >= 0 && i < allMETs.size()) {
            if (method == 0)
                av += allMETsBB.get(i);
            if (method == 1)
                av += allMADxyz.get(i);
            if (method == 2)
                av += allMADs.get(i);
            if (method == 3)
                av += allSpeech.get(i);
            if (method == 4)
                av += allMETsBNB.get(i);
            if (method == 5)
                av += allMETsF.get(i);
            System.out
                    .println("MET value " + method + " index " + i + " " + midpoint + " " + leftTime + " " + rightTime);
            ninAv++;
        }
        if (ninAv == 0)
            return null;
        return av / ninAv;
    }

    public static String getPosture(double leftTime, double rightTime) {
        rightTime = (rightTime / 1000) - CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
        leftTime = (leftTime / 1000) - CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
        String posture = "";
        Set<String> postures = new HashSet<String>();
        for (int i = 0; i < allPostureLabels.size(); i++) {
            int midpoint = i * 6000 + 3000; // 6 secondes
            if (midpoint < leftTime)
                continue;
            if (midpoint > rightTime)
                break;
            postures.add(allPostureLabels.get(i));
        }
        if (postures.size() == 0 || postures.size() > 2)
            posture = "Unknown";
        else {
            Iterator<String> namesIterator = postures.iterator();
            posture = namesIterator.next();
            while (namesIterator.hasNext())
                posture = posture + "/" + namesIterator.next();
        }
        return posture;
    }

}