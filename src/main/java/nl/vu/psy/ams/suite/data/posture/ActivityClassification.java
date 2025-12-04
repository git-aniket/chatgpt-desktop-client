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
import nl.vu.psy.ams.suite.data.FilteredMotGeneratorFast;
import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
import nl.vu.psy.ams.suite.data.files.BinaryFile;

import nl.vu.psy.ams.suite.gui.MainFrame;

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

    /**
     * Generate accelerometer magnitude files for step detection and activity
     * analysis.
     * 
     * Pipeline:
     * 1. Ensure filtered accelerometer files exist (FILTMXR, FILTMYR, FILTMZR)
     * - If missing, generate them from raw channels (MXR, MYR, MZR)
     * - Filtered files have 20 Hz low-pass filter applied to remove high-frequency
     * noise
     * 2. Calculate 3D magnitude: sqrt(x² + y² + z²) → AccelVectorMag.bin
     * 3. Apply 0.005 Hz high-pass filter to remove gravity/drift →
     * FILTAccelVectorMag.bin
     * 4. Calculate average motility for the recording period
     * 
     * @throws Exception if filtered files cannot be generated (e.g., raw channels
     *                   missing)
     */
    public static void generateMeanMotilityFile() throws Exception {
        CurrentOpenData cod = CurrentOpenData.getInstance();

        File magnitudeFile = new File(cod.getFilePath(), "AccelVectorMag.bin");
        File filteredMagnitudeFile = new File(cod.getFilePath(), "FILTAccelVectorMag.bin");

        // Early return if files already exist
        if (magnitudeFile.exists() && filteredMagnitudeFile.exists() && cod.channelExists("AccelVectorMag")) {
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(filteredMagnitudeFile);
            ssf2.start();
            return;
        }

        cod.dirtyFiles.add(magnitudeFile);
        cod.dirtyFiles.add(filteredMagnitudeFile);
        tickFile = new File(cod.getFilePath(), "TicksM.bin");

        // Ensure filtered accelerometer files exist - generate if needed
        // These files are created by FilteredMotGeneratorFast with 20 Hz low-pass
        // filter
        File filteredAccelX = new File(cod.getFilePath(), "FILTMXR.bin");
        File filteredAccelY = new File(cod.getFilePath(), "FILTMYR.bin");
        File filteredAccelZ = new File(cod.getFilePath(), "FILTMZR.bin");

        if (!filteredAccelX.exists() || !filteredAccelY.exists() || !filteredAccelZ.exists()) {
            // Generate filtered files first
            FilteredMotGeneratorFast fMot = new FilteredMotGeneratorFast();
            fMot.GenerateFilteredMot("MXR");
            fMot.GenerateFilteredMot("MYR");
            fMot.GenerateFilteredMot("MZR");
        }

        // Read the filtered accelerometer data
        AccelerometerDataReader.AccelData accelData = AccelerometerDataReader.readAllAsIntegers(
                filteredAccelX, filteredAccelY, filteredAccelZ);

        // Calculate 3D magnitude: sqrt(x² + y² + z²)
        int[] magnitude = AccelerometerDataReader.calculateMagnitude(accelData);

        // Write raw magnitude to binary file
        try (FileOutputStream fos = new FileOutputStream(magnitudeFile);
                FileChannel channel = fos.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(accelData.numSamples * 4);
            IntBuffer intBuffer = buffer.asIntBuffer();
            intBuffer.put(magnitude);
            buffer.position(0);
            buffer.limit(accelData.numSamples * 4);
            channel.write(buffer);
        }

        // Register the raw magnitude file
        SubsetFilesSingle ssf1 = new SubsetFilesSingle(magnitudeFile);
        ssf1.start();

        // Apply 0.005 Hz high-pass filter to remove gravity and slow drift components
        // This isolates dynamic motion for step detection
        removeGravity(magnitudeFile, filteredMagnitudeFile);
        SubsetFilesSingle ssf2 = new SubsetFilesSingle(filteredMagnitudeFile);
        ssf2.start();

        // Calculate and store average motility for the entire recording
        try (BinaryFile bf = new BinaryFile("FILTAccelVectorMag")) {
            avMeanMotility = bf.getAverageBetweenTimes(cod.getStartTimeInUS(), cod.getEndTimeInUS());
        }
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
        File motilityIntensityFile = new File(cod.getFilePath(), "MotilityIntensity.dbin");
        File AltitudeFile = new File(cod.getFilePath(), "Altitude.dbin");

        if (motilityIntensityFile.exists() &&
                AltitudeFile.exists() &&
                cod.channelExists("MotilityIntensity") &&
                cod.channelExists("Altitude"))
            return;

        if (pressureAvailable) {
            cod.dirtyFiles.add(catFile);
            cod.dirtyFiles.add(motilityIntensityFile);
            cod.dirtyFiles.add(AltitudeFile);
        }
        File fAccelX = new File(tempDir, "FILTMXR.bin");
        File fAccelY = new File(tempDir, "FILTMYR.bin");
        File fAccelZ = new File(tempDir, "FILTMZR.bin");
        File pressureFile = new File(tempDir, "FILTP_sc.dbin");
        if (tickFile.exists())
            is = new RandomAccessFile(tickFile, "r");

        // === DIRECT READING: Load all data at once ===
        AccelerometerDataReader.AccelData allAccelData = AccelerometerDataReader.readAllAsIntegers(
                fAccelX, fAccelY, fAccelZ);

        int[] sampMX = allAccelData.x;
        int[] sampMY = allAccelData.y;
        int[] sampMZ = allAccelData.z;
        int totalSamples = allAccelData.numSamples;

        // Read all pressure/temperature data if available
        double[] allSampPres = null;
        if (pressureAvailable) {
            // Pressure/temperature are sampled at 200x slower rate
            int pressureSamples = totalSamples / 200;
            allSampPres = new double[pressureSamples];

            try (FileInputStream fisPres = new FileInputStream(pressureFile);
                    FileChannel ifPres = fisPres.getChannel()) {

                ByteBuffer bbP = ByteBuffer.allocateDirect(pressureSamples * 8);
                DoubleBuffer sbP = bbP.asDoubleBuffer();

                // Read pressure
                bbP.clear();
                sbP.clear();
                ifPres.read(bbP);
                bbP.flip();
                sbP.get(allSampPres);
            }
        }

        // Setup output file channels
        FileOutputStream fosMotility = null, fosAlt = null;
        FileChannel fcMotility = null, fcAlt = null;
        if (pressureAvailable) {
            fosMotility = new FileOutputStream(motilityIntensityFile);
            fcMotility = fosMotility.getChannel();
            fosAlt = new FileOutputStream(AltitudeFile);
            fcAlt = fosAlt.getChannel();
        }

        CurrentOpenData.getInstance().getPostureLabels().clear();

        // === PROCESS ALL DATA AT ONCE ===
        if (pressureAvailable) {
            analyseMotility(sampMX, sampMY, sampMZ, allSampPres, fcMotility, fcAlt, 0L);
        }

        // Close output channels
        if (fcMotility != null)
            fcMotility.close();
        if (fosMotility != null)
            fosMotility.close();
        if (fcAlt != null)
            fcAlt.close();
        if (fosAlt != null)
            fosAlt.close();

        if (is != null)
            is.close();

        if (pressureAvailable) {
            SubsetFilesSingle ssf1 = new SubsetFilesSingle(catFile);
            ssf1.start();
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(motilityIntensityFile);
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
     * @param inFile  Input file containing integer samples
     * @param outFile Output file for filtered samples
     */
    public static void removeGravity(File inFile, File outFile) {
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

    /**
     * Analyze motion data to calculate physical activity metrics and write results
     * to binary files.
     * 
     * @param sampMX              Raw X-axis accelerometer samples
     * @param sampMY              Raw Y-axis accelerometer samples
     * @param sampMZ              Raw Z-axis accelerometer samples
     * @param sampPres            Pressure sensor samples
     * @param fcMotility          FileChannel for writing motility intensity data
     * @param fcAlt               FileChannel for writing altitude data
     * @param startSampleAbsolute Absolute sample index for timestamp calculation
     */
    public static void analyseMotility(int[] sampMX, int[] sampMY, int[] sampMZ,
            double[] sampPres, FileChannel fcMotility, FileChannel fcAlt, long startSampleAbsolute) {

        // Convert accelerometer counts to m/s^2
        double[] accelX = MotionDataUtils.toMs2(sampMX, "MXR");
        double[] accelY = MotionDataUtils.toMs2(sampMY, "MYR");
        double[] accelZ = MotionDataUtils.toMs2(sampMZ, "MZR");

        if (accelX == null || accelY == null || accelZ == null) {
            return; // Channel calibration failed
        }

        // Calculate physical activity metrics
        long startTime = (tickFile != null && tickFile.exists())
                ? MotionDataUtils.correctForTicks(startSampleAbsolute, tickFile)
                : startSampleAbsolute;

        PhysicalActivityCalculator calc = PhysicalActivityCalculator.getInstance();
        calc.setAccelerometerData(accelX, accelY, accelZ);

        // Estimate METs using different methods and accumulate results
        allMETsBB.addAll(calc.estimateMETs(startTime * 1000, PhysicalActivityCalculator.Method.BRAGE_NONBRANCHED));
        allMETsBNB.addAll(calc.estimateMETs(startTime * 1000, PhysicalActivityCalculator.Method.BRAGE_BRANCHED));
        allMETsF.addAll(calc.estimateMETs(startTime * 1000, PhysicalActivityCalculator.Method.FREEDSON));

        List<Double> MADxyz = calc.calculateMADxyz();
        allMADxyz.addAll(MADxyz);
        allMADs.addAll(calc.calculateMAD());
        allSpeech.addAll(calc.calculateSpeech(accelZ));

        // Calculate altitude from pressure and write results to binary files
        double[] altitude = MotionDataUtils.getAltitudeFromPressure(sampPres);
        try {
            MotionDataUtils.writeToBinaryChannel(MADxyz, fcMotility);
            MotionDataUtils.writeToBinaryChannel(altitude, fcAlt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // get the average MET value for the given time range
    // leftTime and rightTime are in ms
    // ! There are multiple instances of MET values out of bounds
    // ! Needs to be fixed
    // Time constants for different data granularities
    private static final int MS_PER_MINUTE = 60000;
    private static final int MS_PER_POSTURE_INTERVAL = 6000; // 6 seconds

    // Activity metric method indices
    public static final int METRIC_MET_BRAGE_NONBRANCHED = 0;
    public static final int METRIC_MAD_XYZ = 1;
    public static final int METRIC_MAD = 2;
    public static final int METRIC_SPEECH = 3;
    public static final int METRIC_MET_BRAGE_BRANCHED = 4;
    public static final int METRIC_MET_FREEDSON = 5;

    /**
     * Calculate average activity metric for a time range using default method (MET
     * Brage Non-Branched).
     * 
     * @param leftTime  Start time in microseconds
     * @param rightTime End time in microseconds
     * @return Average metric value, or null if no data available
     */
    public static Double getAverageActivityMetric(double leftTime, double rightTime) {
        return getAverageActivityMetric(leftTime, rightTime, METRIC_MET_BRAGE_NONBRANCHED);
    }

    /**
     * Calculate average activity metric for a time range.
     * Uses minute-based averaging where only minutes with midpoint within the time
     * range are included.
     * 
     * @param leftTime  Start time in microseconds
     * @param rightTime End time in microseconds
     * @param method    Metric type (use METRIC_* constants)
     * @return Average metric value, or null if no data available
     */
    public static Double getAverageActivityMetric(double leftTime, double rightTime, int method) {
        // Get the appropriate data list based on method
        List<Double> sourceData = switch (method) {
            case METRIC_MET_BRAGE_NONBRANCHED -> allMETsBB;
            case METRIC_MAD_XYZ -> allMADxyz;
            case METRIC_MAD -> allMADs;
            case METRIC_SPEECH -> allSpeech;
            case METRIC_MET_BRAGE_BRANCHED -> allMETsBNB;
            case METRIC_MET_FREEDSON -> allMETsF;
            default -> throw new IllegalArgumentException("Unknown activity metric method: " + method);
        };

        // Filter data to time range (minute-based intervals)
        List<Double> filteredData = MotionDataUtils.getDataInTimeRange(sourceData, leftTime, rightTime, MS_PER_MINUTE);

        if (filteredData.isEmpty())
            return null;

        // Calculate average
        double sum = 0;
        for (Double value : filteredData) {
            sum += value;
        }

        return sum / filteredData.size();
    }

    /**
     * Populate the posture labels list from PostureClassifier.
     * This should be called after posture classification is complete.
     */
    public static void populatePostureLabels() {
        PostureClassifier classifier = PostureClassifier.getInstance();
        allPostureLabels.clear();
        allPostureLabels.addAll(classifier.getPostureLabelsForTimeline());
    }

    /**
     * Get the predominant posture(s) for a time range.
     * Uses 6-second interval data with midpoint-based filtering.
     * 
     * @param leftTime  Start time in microseconds
     * @param rightTime End time in microseconds
     * @return Posture string, or "Unknown" if no clear posture (0 or >2 unique
     *         postures)
     */
    public static String getPosture(double leftTime, double rightTime) {
        // Filter posture data to time range (6-second intervals)
        List<String> filteredPostures = MotionDataUtils.getDataInTimeRange(allPostureLabels, leftTime, rightTime,
                MS_PER_POSTURE_INTERVAL);

        // Collect unique postures
        Set<String> uniquePostures = new HashSet<>(filteredPostures);

        // Return based on number of unique postures found
        if (uniquePostures.isEmpty() || uniquePostures.size() > 2) {
            return "Unknown";
        } else if (uniquePostures.size() == 1) {
            return uniquePostures.iterator().next();
        } else {
            // Two postures: combine with "/"
            Iterator<String> iterator = uniquePostures.iterator();
            return iterator.next() + "/" + iterator.next();
        }
    }

}