package nl.vu.psy.ams.suite.data.posture;

// import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
// import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.DoubleBuffer;
// import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
// import java.util.HashMap;
import java.util.List;
// import java.util.Map;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.Set;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.stat.regression.SimpleRegression;
// import javax.swing.JFrame;
// import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
// import net.objecthunter.exp4j.Expression;
// import net.objecthunter.exp4j.ExpressionBuilder;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
// import javax.swing.ProgressMonitor;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

// import ml.dmlc.xgboost4j.java.*;

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
    static ByteBuffer bb;
    static IntBuffer sb;
    static ByteBuffer obb;
    static IntBuffer osb;
    static int[] samp;
    static int[] outBuf;
    static int[] tempBuf;
    static double[] backBuffer = new double[4];
    static double[] forBuffer = new double[4];
    static boolean isFirstChunk = true;
    // Tracks how many 5 Hz samples we've already written to altSm_debug.txt across
    // chunks
    static long altSmDebugSamplesWritten = 0L;
    static double firstVal;
    static int chunkno = 0;
    static int altitudeCount = 0;
    static double altitudeSum = 0;
    static double avMeanMotility = 0;
    static int lastState = 20;
    static final int windowSizeMotilityEntropy = 4096;
    static int freqAcceleromters = 1000;
    static int freqPressure = 5;

    static ArrayList<Double> altitudeCarryover = new ArrayList<Double>();

    private static final int PATCH_WIDTH = 1000; // 0.5 second transition patch width in seconds
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

    // static final int CLIMBING_UPSTAIRS = 10;
    // static final int CLIMBING_DOWNSTAIRS = 20;
    // static final int STAIRS = 30;
    // static final int STANDING = 40;
    // static final int SITTING = 50;
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
    // Cache for the most recent fused 10s posture+stairs labels
    private static List<String> lastFusedPosture10s = new ArrayList<>();

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

    public static void generateMeanMotilityFile() throws Exception {
        CurrentOpenData cod = CurrentOpenData.getInstance();
        File tempDir = cod.getFilePath();
        if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")) {
            return;
        }
        File meanMotilityFile = new File(cod.getFilePath(), "StepInstances.bin");
        File meanMotilityFileF = new File(cod.getFilePath(), "FILTStepInstances.bin");

        if (meanMotilityFile.exists() && meanMotilityFileF.exists() && cod.channelExists("StepInstances")) {
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(meanMotilityFileF);
            ssf2.start();
            return;
        }

        cod.dirtyFiles.add(meanMotilityFile);
        cod.dirtyFiles.add(meanMotilityFileF);

        File fAccelX = new File(tempDir, "FILTMXR.bin");
        File fAccelY = new File(tempDir, "FILTMYR.bin");
        File fAccelZ = new File(tempDir, "FILTMZR.bin");
        tickFile = new File(tempDir, "TicksM.bin");
        ByteBuffer bbMX = ByteBuffer.allocateDirect(size);
        IntBuffer sbMX = bbMX.asIntBuffer();

        int[] sampMX = new int[size / 4];
        int[] sampMY = new int[size / 4];
        int[] sampMZ = new int[size / 4];

        // read binary files
        long fL = fAccelX.length();
        int nRead = 0;

        FileInputStream fisMX = new FileInputStream(fAccelX);
        FileChannel ifMX = fisMX.getChannel();
        FileInputStream fisMY = new FileInputStream(fAccelY);
        FileChannel ifMY = fisMY.getChannel();
        FileInputStream fisMZ = new FileInputStream(fAccelZ);
        FileChannel ifMZ = fisMZ.getChannel();
        FileOutputStream fos = new FileOutputStream(meanMotilityFile);
        FileChannel ofC = fos.getChannel();
        for (long i = 0; i < fL; i += nRead) {
            // progress.setProgress((int) i);
            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMX.read(bbMX);
            int nSRead = nRead / 4;
            if (nRead < 1)
                break;
            sbMX.get(sampMX, 0, nSRead);
            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMY.read(bbMX);
            nSRead = nRead / 4;
            if (nRead < 1)
                break;
            sbMX.get(sampMY, 0, nSRead);
            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMZ.read(bbMX);
            nSRead = nRead / 4;
            if (nRead < 1)
                break;
            sbMX.get(sampMZ, 0, nSRead);
            double[] meanAccel = new double[sampMX.length];
            for (int j = 0; j < nSRead; j++) {
                meanAccel[j] = Math.sqrt(Math.pow(sampMX[j], 2) + Math.pow(sampMY[j], 2) + Math.pow(sampMZ[j], 2)); // -
                if (meanAccel[j] == 0)
                    System.out.println("0 found");
            }
            try {
                byte[] outbuf = new byte[size];
                ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
                IntBuffer osb = outBuffer.asIntBuffer();
                int[] outBuf = new int[size];

                osb.clear();

                for (int j = 0; j < nSRead; j++) {
                    outBuf[j] = (int) meanAccel[j];
                }
                if (nSRead * 4 != size)
                    System.out.println("incomplete block");
                osb.put(outBuf, 0, nSRead);
                ofC.write(outBuffer);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        ifMX.close();
        fisMX.close();
        ifMY.close();
        fisMY.close();
        ifMZ.close();
        fisMZ.close();
        ofC.close();
        fos.close();
        SubsetFilesSingle ssf1 = new SubsetFilesSingle(meanMotilityFile);
        ssf1.start();

        // high pass filter
        highPassFilterButt(meanMotilityFile, meanMotilityFileF);
        SubsetFilesSingle ssf2 = new SubsetFilesSingle(meanMotilityFileF);
        ssf2.start();
        try (// average
                BinaryFile bf = new BinaryFile("FILTStepInstances")) {
            avMeanMotility = bf.getAverageBetweenTimes(cod.getStartTimeInUS(), cod.getEndTimeInUS());
        }
    }

    // ! Activity intensity can be removed since this is to be replaced by the MET
    // calculations
    private static double calculateWindowSpectralEntropy(double[] window) {
        // int windowLength = window.length;
        // int paddedLength = nextPowerOfTwo(windowLength);
        // double[] paddedWindow = Arrays.copyOf(window, paddedLength);

        // // Perform FFT
        // FastFourierTransformer fft = new
        // FastFourierTransformer(DftNormalization.STANDARD);
        // Complex[] fftResult = fft.transform(paddedWindow, TransformType.FORWARD);

        // // Compute power spectrum
        // double[] powerSpectrum = new double[paddedLength / 2];
        // double sumPower = 0.0;
        // for (int i = 0; i < paddedLength / 2; i++) {
        // powerSpectrum[i] = Math.pow(fftResult[i].getReal(), 2) +
        // Math.pow(fftResult[i].getImaginary(), 2);
        // sumPower += powerSpectrum[i];
        // }

        // // Normalize power spectrum to get probability distribution
        // double[] probabilityDistribution = new double[paddedLength / 2];
        // for (int i = 0; i < paddedLength / 2; i++) {
        // probabilityDistribution[i] = powerSpectrum[i] / sumPower;
        // }

        // // Calculate entropy
        // double entropy = 0.0;
        // for (double p : probabilityDistribution) {
        // if (p > 0) {
        // entropy -= p * Math.log(p) / Math.log(2);
        // }
        // }

        // return entropy;
        return 0;
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
        if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")) {
            return;
        }
        if (!cod.channelExists("P_sc") || !cod.channelExists("T_sc")) {
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
            // File fileTicks = new File(CurrentOpenData.getInstance().getFilePath(),
            // "TicksM.bin");
            // File fileTicksRed = new File(CurrentOpenData.getInstance().getFilePath(),
            // "TicksMRed.bin");
            // if (fileTicks.exists()) {
            // try {
            // reduceSamplesize(fileTicks, fileTicksRed, 1400);
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
            // CurrentOpenData.getInstance().dirtyFiles.add(fileTicksRed);
            // }
        }
        // reduce sample size for accel x,y,z, and gyro x,y,z
        File fAccelX = new File(tempDir, "FILTMXR.bin");
        File fAccelY = new File(tempDir, "FILTMYR.bin");
        File fAccelZ = new File(tempDir, "FILTMZR.bin");
        File temperatureFile = new File(tempDir, "FILTT_sc.dbin");
        File pressureFile = new File(tempDir, "FILTP_sc.dbin");
        if (tickFile.exists())
            is = new RandomAccessFile(tickFile, "r");

        // call python script in for loop to generate activity classification
        ByteBuffer bbMX = ByteBuffer.allocateDirect(size);
        IntBuffer sbMX = bbMX.asIntBuffer();
        ByteBuffer bbP = ByteBuffer.allocateDirect(size * 2 / 200); // dw divider pressure sensor
        DoubleBuffer sbP = bbP.asDoubleBuffer();

        int[] sampMX = new int[size / 4];
        int[] sampMY = new int[size / 4];
        int[] sampMZ = new int[size / 4];
        double[] sampTemp = new double[size / 4 / 200];
        double[] sampPres = new double[size / 4 / 200];

        // read binary files
        long fL = fAccelX.length();
        int nRead = 0;

        FileInputStream fisMX = new FileInputStream(fAccelX);
        FileChannel ifMX = fisMX.getChannel();
        FileInputStream fisMY = new FileInputStream(fAccelY);
        FileChannel ifMY = fisMY.getChannel();
        FileInputStream fisMZ = new FileInputStream(fAccelZ);
        FileChannel ifMZ = fisMZ.getChannel();

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
        FileInputStream fisTemp = null;
        FileChannel ifTemp = null;
        FileInputStream fisPres = null;
        FileChannel ifPres = null;
        if (pressureAvailable) {
            fisTemp = new FileInputStream(temperatureFile);
            ifTemp = fisTemp.getChannel();
            fisPres = new FileInputStream(pressureFile);
            ifPres = fisPres.getChannel();
        }
        int rawThreshold = AppSettings.getInstance().getIntProperty(Settings.LYINGTHRESHOLD);
        double threshold = (rawThreshold - chanM.getRealConstant()) / chanM.getRealSlope() / 1000.0;
        double YPosThreshold = (400 - chanM.getRealConstant()) / chanM.getRealSlope() / 1000.0;
        isFirstChunk = true;
        // Channel calibration for counts -> m/s^2 conversion (reuse per chunk)
        Ams7fsChannelInfo chanX = cod.getChannelInfoFromID("MXR");
        Ams7fsChannelInfo chanY = cod.getChannelInfoFromID("MYR");
        Ams7fsChannelInfo chanZ = cod.getChannelInfoFromID("MZR");
        CurrentOpenData.getInstance().getPostureLabels().clear();

        // analyse data in a loop
        for (long i = 0; i < fL; i += nRead) {
            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMX.read(bbMX);
            int nSRead = nRead / 4;
            if (nRead < 1) {
                break;
            }
            sbMX.get(sampMX, 0, nSRead);

            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMY.read(bbMX);
            nSRead = nRead / 4;
            if (nRead < 1) {
                break;
            }
            sbMX.get(sampMY, 0, nSRead);

            bbMX.position(0);
            sbMX.position(0);
            nRead = ifMZ.read(bbMX);
            nSRead = nRead / 4;
            if (nRead < 1) {
                break;
            }
            sbMX.get(sampMZ, 0, nSRead);

            int nSReadP = 0;
            if (pressureAvailable) {
                bbP.position(0);
                sbP.position(0);
                int nReadP = ifTemp.read(bbP);
                nSReadP = nReadP / 8;
                if (nReadP < 1) {
                    break;
                }
                sbP.get(sampTemp, 0, nSReadP);
                bbP.position(0);
                sbP.position(0);
                nReadP = ifPres.read(bbP);
                nSReadP = nReadP / 8;
                if (nReadP < 1) {
                    break;
                }
                sbP.get(sampPres, 0, nSReadP);
            }
            // check size of sampMX to be equal to size of nSRead
            if (sampMX.length != nSRead) {
                sampMX = Arrays.copyOf(sampMX, nSRead);
                sampMY = Arrays.copyOf(sampMY, nSRead);
                sampMZ = Arrays.copyOf(sampMZ, nSRead);
                if (pressureAvailable) {
                    sampPres = Arrays.copyOf(sampPres, nSReadP);
                    sampTemp = Arrays.copyOf(sampTemp, nSReadP);
                }
            }

            if (pressureAvailable) {
                // Convert raw counts to physical units (m/s^2) once per chunk
                double[] dx = toMs2(sampMX, chanX);
                double[] dy = toMs2(sampMY, chanY);
                double[] dz = toMs2(sampMZ, chanZ);

                // Use the same calibrated arrays for both steps and posture
                long globalStartUS = CurrentOpenData.getInstance().getStartTimeInUS();
                long firstTickMs = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();

                final int FsLocal = SAMPLING_FREQUENCY; // 1000 Hz
                int[] stepLocations = StepDetector.getInstance().detectSteps(dx, dy, dz, chunkno,
                        ActivityClassification::correctForTicks);

                // 1) Run analyseMotility FIRST (computes MET/MAD/Speech and prepares fused
                // posture)

                analyseMotility(sampMX, sampMY, sampMZ, stepLocations, sampTemp, sampPres, 1, threshold, YPosThreshold,
                        fcLying, fc2, fcAlt, chunkno);

                // 2) Stairs labels (5 s epochs) — keep as a separate label stream
                int EPOCH_SAMPLES = 5 * FsLocal; // 5-second epochs for stairs
                ActivityClassification.AltitudeAnalysisResult result = ActivityClassification
                        .analyseAltitudeChange(sampPres, stepLocations, 5);
                String[] sChunk = result.labels;
                java.util.List<AmsLabel> stairsLabelsToAdd = new java.util.ArrayList<>();
                for (int e = 0; e < sChunk.length; e++) {
                    int startSample = e * EPOCH_SAMPLES;
                    int endSample = Math.min(nSRead, startSample + EPOCH_SAMPLES) - 1;
                    if (endSample < startSample)
                        continue;

                    long correctedStart = correctForTicks(startSample + (long) (chunkno * size / 4));
                    long correctedEnd = correctForTicks(endSample + (long) (chunkno * size / 4));

                    long startUS = globalStartUS + (correctedStart - firstTickMs) * 1000L;
                    long endUS = globalStartUS + (correctedEnd - firstTickMs) * 1000L;

                    stairsLabelsToAdd.add(AmsLabel.generateStairsLabel(startUS, endUS, sChunk[e]));
                    allPostureLabels.add(sChunk[e]);
                }
                CurrentOpenData.getInstance().getStairsLabels().getLabels().addAll(stairsLabelsToAdd);

                // 3) Posture labels (10 s epochs) — use the fused results prepared in
                // analyseMotility
                EPOCH_SAMPLES = 10 * FsLocal; // 10-second epochs
                java.util.List<String> fusedList = getLastFusedPosture10s();
                String[] postureChunk = fusedList.toArray(new String[0]);

                java.util.List<AmsLabel> postureLabelsToAdd = new java.util.ArrayList<>();
                for (int e = 0; e < postureChunk.length; e++) {
                    int startSample = e * EPOCH_SAMPLES;
                    int endSample = Math.min(nSRead, startSample + EPOCH_SAMPLES) - 1;
                    if (endSample < startSample)
                        continue;

                    long correctedStart = correctForTicks(startSample + (long) (chunkno * size / 4));
                    long correctedEnd = correctForTicks(endSample + (long) (chunkno * size / 4));

                    long startUS = globalStartUS + (correctedStart - firstTickMs) * 1000L;
                    long endUS = globalStartUS + (correctedEnd - firstTickMs) * 1000L;

                    postureLabelsToAdd.add(AmsLabel.generatePostureLabel(startUS, endUS, postureChunk[e]));
                    allPostureLabels.add(postureChunk[e]);
                }
                // CurrentOpenData.getInstance().getPostureLabels().getLabels().addAll(postureLabelsToAdd);

                // analyseMotility(sampMX, sampMY, sampMZ, stepLocations, sampTemp, sampPres, 1,
                // threshold, YPosThreshold,
                // fcLying, fc2, fcAlt, chunkno);
            }
            isFirstChunk = false;
            chunkno++;
        }

        if (pressureAvailable) {
            cleanupLabels("Activity");
            cleanupLabels("Stairs");
        }
        ifMX.close();
        fisMX.close();
        ifMY.close();
        fisMY.close();
        ifMZ.close();
        fisMZ.close();
        if (is != null)
            is.close();

        if (pressureAvailable) {
            fc2.close();
            fos2.close();
            fcLying.close();
            fosLying.close();
            fcAlt.close();
            fosAlt.close();
            SubsetFilesSingle ssf1 = new SubsetFilesSingle(catFile);
            ssf1.start();
            SubsetFilesSingle ssf2 = new SubsetFilesSingle(lyingFile);
            ssf2.start();
            SubsetFilesSingle ssf3 = new SubsetFilesSingle(AltitudeFile);
            ssf3.start();
        }
        altitudeSum = 0;
        if (pressureAvailable) {
            ifTemp.close();
            ifPres.close();
            fisTemp.close();
            fisPres.close();
        }
    }

    /**
     * Result type for altitude analysis: fused labels and cumulative altitude
     * change.
     */
    public static class AltitudeAnalysisResult {
        public final String[] labels;
        public final double cumulativeDeltaAlt;

        public AltitudeAnalysisResult(String[] labels, double cumulativeDeltaAlt) {
            this.labels = labels;
            this.cumulativeDeltaAlt = cumulativeDeltaAlt;
        }
    }

    /**
     * Fuse altitude-based stair detection with step locations from accelerometer.
     * Uses SimpleRegression to estimate climb rate over 2-second windows.
     * 
     * @param pressure      barometer samples (Pa), sampled at 5 Hz
     * @param stepLocations indices of steps detected in accelerometer data (at 1000
     *                      Hz)
     * @param windowSec     window length in seconds for stair analysis (not used,
     *                      always 2s here)
     * @return AltitudeAnalysisResult: fused label array and cumulative altitude
     *         change
     */
    public static AltitudeAnalysisResult analyseAltitudeChange(double[] pressure, int[] stepLocations, int windowSec) {
        // Validate
        if (pressure == null || stepLocations == null) {
            throw new IllegalArgumentException("pressure and stepLocations cannot be null");
        }
        final int Np = pressure.length;
        final int Na = stepLocations.length;
        if (Np == 0 || Na == 0) {
            return new AltitudeAnalysisResult(new String[Math.max(Np, Na)], 0.0);
        }

        // 1) Pressure → altitude (relative to first sample), 5 Hz domain
        // Use FastMath for numerical robustness
        final double P0 = pressure[0];
        double[] alt5 = new double[Np];
        for (int i = 0; i < Np; i++) {
            double ratio = pressure[i] / P0;
            alt5[i] = 44330.0 * (1.0 - FastMath.pow(ratio, 1.0 / 5.255));
        }

        // 2) Downsample to ~1 Hz by taking every 5th sample (keeps alignment with older
        // logic)
        final int factor = 5; // 0.2 s * 5 = 1 s
        final int N1 = (Np + factor - 1) / factor; // include last partial block
        double[] alt1 = new double[N1];
        for (int j = 0, i = 0; j < N1; j++, i += factor) {
            int idx = FastMath.min(i, Np - 1);
            alt1[j] = alt5[idx];
        }

        // 3) Estimate climb rate using SimpleRegression over rolling 2-second windows
        // Slope is in m/s (y in meters, x in seconds).
        final int slopeWindowSec = 2; // robust 2 s window at 1 Hz => 3 samples if you prefer; here we fit on {0,1,2}
        final int regPoints = slopeWindowSec + 1; // 0..2 seconds -> 3 points
        final int nRoc = FastMath.max(0, alt1.length - slopeWindowSec);
        double[] roc2s = new double[nRoc]; // slope m/s, aligned to the window end (like previous logic)
        double[] dAlt2s = new double[nRoc]; // net change over the same 2 s window

        for (int end = slopeWindowSec; end < alt1.length; end++) {
            int start = end - slopeWindowSec;
            // Build a small regression with x = 0..slopeWindowSec (seconds)
            SimpleRegression reg = new SimpleRegression(true); // include intercept
            for (int t = 0; t < regPoints; t++) {
                reg.addData(t, alt1[start + t]);
            }
            roc2s[end - slopeWindowSec] = reg.getSlope(); // m/s
            dAlt2s[end - slopeWindowSec] = alt1[end] - alt1[start]; // meters over 2 s
        }

        // Save debug (same filenames as before)
        // saveToTextFileWithTime(roc2s, 1.0, "roc2s.txt", true);

        // 4) Classify each 2 s block by slope sign/magnitude
        final double CLIMB_RATE_THRESHOLD = 0.15; // m/s
        String[] labels2s = new String[nRoc];
        double cumulativeAscent = 0.0;

        for (int r = 0; r < nRoc; r++) {
            double rate = roc2s[r];
            String label;
            if (rate > CLIMB_RATE_THRESHOLD) {
                label = "Stairs up";
                cumulativeAscent += FastMath.max(0.0, dAlt2s[r]);
            } else if (rate < -CLIMB_RATE_THRESHOLD) {
                label = "Stairs down";
            } else {
                label = "Level ground";
            }
            labels2s[r] = label;
        }

        // Persist label stream for inspection (numeric form)
        double[] labelsNumeric = new double[labels2s.length];
        for (int i = 0; i < labels2s.length; i++) {
            switch (labels2s[i]) {
                case "Stairs up":
                    labelsNumeric[i] = 1;
                    break;
                case "Stairs down":
                    labelsNumeric[i] = -1;
                    break;
                default:
                    labelsNumeric[i] = 0;
            }
        }
        // saveToTextFileWithTime(labelsNumeric, 2.0, "labels2s.txt", true);

        // 5) Expand 2 s labels to the 1000 Hz timeline for fusion with steps
        String[] fused = new String[Na];
        java.util.Arrays.fill(fused, "Level ground");
        final int SAMPLES_PER_LABEL = 2000; // 2 s @ 1000 Hz
        for (int k = 0; k < labels2s.length; k++) {
            int aStart = k * SAMPLES_PER_LABEL;
            if (aStart >= Na)
                break;
            int aEnd = FastMath.min(Na, aStart + SAMPLES_PER_LABEL);
            java.util.Arrays.fill(fused, aStart, aEnd, labels2s[k]);
        }

        double[] fusedNumeric = new double[Na];
        for (int i = 0; i < Na; i++) {
            switch (fused[i]) {
                case "Stairs up":
                    fusedNumeric[i] = 1;
                    break;
                case "Stairs down":
                    fusedNumeric[i] = -1;
                    break;
                default:
                    fusedNumeric[i] = 0;
            }
        }
        // dt for fused timeline is 1/1000 s
        // saveToTextFileWithTime(fusedNumeric, 0.001, "fused.txt", true);

        System.out.println("nRoc=" + nRoc + " labels2s.len=" + labels2s.length +
                " Na=" + Na + " fused.len=" + fused.length);
        return new AltitudeAnalysisResult(fused, cumulativeAscent);
    }

    static void reduceSamplesize(File filteredFile, File fdzFile, int nToSkip) throws IOException {

        if (nToSkip < 1) {
            nToSkip = 1;
        }
        int skipCounter = 0;
        int[] samp = new int[size / 4];
        int[] outBuf = new int[size / 4];
        int nRead = size;
        long fL = filteredFile.length();

        FileInputStream fis = new FileInputStream(filteredFile);
        // byte[] buf = new byte[(int) fL];
        // fis.read(buf);
        // ByteBuffer inBuffer = ByteBuffer.wrap(buf);
        // fis.close();
        // IntBuffer sb = inBuffer.asIntBuffer();
        FileOutputStream fos = new FileOutputStream(fdzFile);
        int buflength = (int) fL / nToSkip;
        if (buflength % 4 == 1) {
            buflength += 3;
        }
        if (buflength % 4 == 2) {
            buflength += 2;
        }
        if (buflength % 4 == 3) {
            buflength += 1;
        }
        // byte[] outbuf = new byte[(int) buflength];
        // ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
        // IntBuffer osb = outBuffer.asIntBuffer();
        FileChannel ifC = fis.getChannel();
        FileChannel ofC = fos.getChannel();
        long pos = 0;
        int cycle = 0, nPerCycle = 0;

        for (long i = 0; i < fL; i += nRead) {
            // int nSRead;
            // if (sb.capacity() - sb.position() < nRead / 4) {
            // nSRead = sb.capacity() - sb.position();
            // } else {
            // nSRead = nRead / 4;
            // }
            bb.position(0);
            sb.position(0);
            long newPos = pos;
            pos += size;
            if (pos > fL)
                pos = fL;
            nRead = (int) (pos - newPos);
            ifC.read(bb, newPos);
            int nSRead = nRead / 4;
            if (nRead < 1) {
                break;
            }
            sb.get(samp, 0, nSRead);
            int nOut = 0;
            for (int q = 0; q < nSRead; q++) {
                skipCounter++;
                if (skipCounter >= nToSkip) {
                    outBuf[nOut] = samp[q];
                    nOut++;
                    skipCounter = 0;
                }
            }
            if (nPerCycle == 0)
                nPerCycle = nOut;
            obb.clear();
            osb.clear();
            osb.put(outBuf, 0, nOut);
            obb.limit(4 * nOut);
            ofC.write(obb, nPerCycle * cycle * 4);
            cycle++;
        }
        // fos.write(outbuf);
        ifC.close();
        fis.close();
        ofC.close();
        fos.close();

    }

    // High pass filter for gravity removal
    public static void highPassFilterButt(File inFile, File outFile) {
        int size = (int) inFile.length();
        bb = ByteBuffer.allocateDirect(size);
        obb = ByteBuffer.allocateDirect(size);
        sb = bb.asIntBuffer();
        osb = obb.asIntBuffer();

        samp = new int[size / 4];
        outBuf = new int[size / 4];
        tempBuf = new int[size / 4];
        // [b,a] = butter(4, 0.004, 'high'); cut-off 0.005 Hz
        double[] b = new double[] { 0.983715174129757, -3.934860696519027, 5.902291044778541, -3.934860696519027,
                0.983715174129757 };
        double[] a = new double[] { 1.000000000000000, -3.967162595948849, 5.902025861490880, -3.902558784823241,
                0.967695543813138 };

        // long fL = inFile.length();
        // JFrame frame = MainFrame.getInstance().getMainFrame();
        // ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered
        // Mean Motility", null, 0, (int) fL);

        try {
            filterForAndBackward(a, b, inFile, outFile); // , progress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void filterForAndBackward(double[] a, double[] b, File sclFile, File tempFile) // , ProgressMonitor progress)
            throws IOException {
        int size = (int) sclFile.length();
        FileInputStream fis = new FileInputStream(sclFile);
        FileOutputStream fos = new FileOutputStream(tempFile);
        FileChannel ifC = fis.getChannel();
        FileChannel ofC = fos.getChannel();

        int nRead = size;
        int nOut;
        double[] state = new double[4];
        bb.position(0);
        sb.position(0);
        nRead = ifC.read(bb);
        int nSRead = nRead / 4;
        if (nRead < 1) {
            ofC.close();
            ifC.close();
            fis.close();
            fos.close();
            return;
        }
        sb.get(samp, 0, nSRead);
        for (int q = 0; q < nSRead; q++) {

            double vl = samp[q];
            double val = b[0] * vl + state[0];
            state[0] = b[1] * vl + state[1] - a[1] * val;
            state[1] = b[2] * vl + state[2] - a[2] * val;
            state[2] = b[3] * vl + state[3] - a[3] * val;
            state[3] = b[4] * vl - a[4] * val;

            double newVal = val;
            tempBuf[q] = (int) newVal;
        }
        nOut = nSRead;
        double vl = tempBuf[nSRead - 1];
        for (int q = 0; q < backBuffer.length; q++) {
            backBuffer[q] = vl;
        }
        for (int q = 0; q < forBuffer.length; q++) {
            forBuffer[q] = vl;
        }
        for (int q = 0; q < nSRead; q++) {

            vl = tempBuf[nSRead - q - 1];

            double val = vl * b[0];

            for (int z = 0; z < b.length - 1; z++) {
                val += backBuffer[z] * b[z + 1];
            }

            for (int z = 0; z < a.length - 1; z++) {
                val -= forBuffer[z] * a[z + 1];
            }

            for (int z = 0; z < backBuffer.length - 1; z++) {
                backBuffer[3 - z] = backBuffer[2 - z];
            }

            for (int z = 0; z < forBuffer.length - 1; z++) {
                forBuffer[3 - z] = forBuffer[2 - z];
            }

            backBuffer[0] = vl;
            forBuffer[0] = val;

            double newVal = val;
            outBuf[nSRead - q - 1] = (int) newVal;
        }
        nOut = nSRead;

        obb.clear();
        osb.clear();
        osb.put(outBuf, 0, nOut);
        obb.limit(4 * nOut);
        ofC.write(obb);

        ofC.close();
        ifC.close();
        fis.close();
        fos.close();
    }

    // A function to get mean motility from acceleration data
    @SuppressWarnings("unused")
    private static double[] getSignalMagnitudeVector(int[] accX, int[] accY, int[] accZ) {
        double rmsAcc[] = new double[accX.length];
        for (int i = 0; i < accX.length; i++) {
            rmsAcc[i] = Math.sqrt(accX[i] * accX[i] + accY[i] * accY[i] + accZ[i] * accZ[i]);
        }
        return rmsAcc;
    }

    private static double[] getAltitudeFromPressure(double[] pressure) {
        double[] altitude = new double[pressure.length];
        for (int j = 0; j < pressure.length; j++) {
            altitude[j] = (pressure[j] - 102000.0) / -12.2;
        }
        return altitude;
    }

    @SuppressWarnings("unused")
    private static int[] analyseHorizontalVertical(int[] sampMX, int[] sampMY, int[] sampMZ, double threshold,
            double thresholdLow) {
        int[] horizontalVerticalStates = new int[sampMX.length];
        for (int j = 0; j < sampMX.length; j++) {

            final double hvHigherThreshold = threshold; // 0.8 g's
            final double hvLowerThreshold = thresholdLow; // 0.5 * 2000;

            // HORIZONTAL and its subtypes
            if (sampMX[j] >= hvLowerThreshold) {
                // double y = sampMY[j];
                // double x = sampMX[j];
                // lyingAngleStates[j] = Math.atan2(y, x) * 180 / Math.PI;

                if (sampMZ[j] >= hvHigherThreshold) {
                    horizontalVerticalStates[j] = HORIZONTAL_LYING_ON_BACK;

                } else if (sampMZ[j] <= -hvHigherThreshold) {
                    horizontalVerticalStates[j] = HORIZONTAL_LYING_ON_BELLY;
                } else {
                    if (sampMY[j] >= hvHigherThreshold) {
                        horizontalVerticalStates[j] = HORIZONTAL_LYING_ON_RIGHT_SIDE;
                    } else if (sampMY[j] <= -hvHigherThreshold) {
                        horizontalVerticalStates[j] = HORIZONTAL_LYING_ON_LEFT_SIDE;
                    } else {
                        horizontalVerticalStates[j] = HORIZONTAL_LYING_UNKNOWN;
                    }
                }

            } // VERTICAL and its subtypes
            else {
                horizontalVerticalStates[j] = VERTICAL;
            }
        }

        return horizontalVerticalStates;
    }

    @SuppressWarnings("unused")
    private static double[] getActivityIntensity(double[] motionSignal, int windowSize) {
        double[] motilityIntensity = new double[motionSignal.length];
        for (int i = 0; i < motionSignal.length; i += windowSize) {
            if (i + windowSize > motionSignal.length)
                windowSize = motionSignal.length - i;
            double subArrayMeanAccel[] = Arrays.copyOfRange(motionSignal, i, i + windowSize);
            double temp = calculateWindowSpectralEntropy(subArrayMeanAccel);
            // if (temp > 0.19 && windowSize < 4096)
            // System.out.println("ohoh");

            // fill values of from i to i+windowSizeMotilitySignal and the activity label
            for (int j = i; j < i + windowSize && j < motionSignal.length; j++) {
                motilityIntensity[j] = temp;
            }
        }
        return motilityIntensity;
    }

    @SuppressWarnings("unused")
    private static double[] getActivityIntensityLabel(double[] motilityIntensity) {
        double[] motilityIntensityLabel = new double[motilityIntensity.length];
        for (int i = 0; i < motilityIntensity.length; i++) {
            if (motilityIntensity[i] < ACTIVITY_LOW_INTENSITY_THRESHOLD) {
                motilityIntensityLabel[i] = ACTIVITY_LOW_INTENSITY;
            } else if (motilityIntensity[i] >= ACTIVITY_LOW_INTENSITY_THRESHOLD
                    && motilityIntensity[i] < ACTIVITY_MEDIUM_INTENSITY_THRESHOLD) {
                motilityIntensityLabel[i] = ACTIVITY_MEDIUM_INTENSITY;
            } else if (motilityIntensity[i] >= ACTIVITY_MEDIUM_INTENSITY_THRESHOLD) {
                motilityIntensityLabel[i] = ACTIVITY_HIGH_INTENSITY;
            }
        }
        return motilityIntensityLabel;
    }

    @SuppressWarnings("unused")
    private static double[] fuseMotilityIntensityLableWithHVStates(double[] motilityIntensityLabel,
            int[] horizontalVerticalStates) {
        double[] activity = new double[motilityIntensityLabel.length];
        for (int i = 0; i < horizontalVerticalStates.length; i++) {
            // if subject is vertical check the intensity of motility
            if (horizontalVerticalStates[i] == VERTICAL) {
                if (motilityIntensityLabel[i] == ACTIVITY_HIGH_INTENSITY) {
                    activity[i] = RUNNING;
                } else if (motilityIntensityLabel[i] == ACTIVITY_MEDIUM_INTENSITY) {
                    activity[i] = WALKING;
                } else if (motilityIntensityLabel[i] == ACTIVITY_LOW_INTENSITY) {
                    activity[i] = STATIONARY;
                }
            } else {// else if subject is horizontal, then consider lying down
                activity[i] = LYING_DOWN;
            }
        }
        return activity;
    }

    @SuppressWarnings("unused")
    private static int[] getActivityTransitionFromAltitude(double[] altitude) {
        double[] d_altitude = centralDifference(altitude);
        double[] dd_altitude = centralDifference(d_altitude);

        int[] altitudeChangeIdx = getSignChanges(dd_altitude);
        int[] activityTransitions = new int[altitudeChangeIdx.length];

        for (int i = deltaIdx; i < altitudeChangeIdx.length - deltaIdx; i++) {
            if (altitudeChangeIdx[i] == 0) // if slope is 0
            {
                continue;
            }

            // Get the difference between higher and lower index
            double forwardDiff = altitude[i + deltaIdx] - altitude[i];
            double backwardDiff = altitude[i] - altitude[i - deltaIdx];

            if (altitudeChangeIdx[i] == 1) { // if sloping positive
                if (forwardDiff >= thresholdSitStand && forwardDiff <= thresholdStairs
                        && backwardDiff >= thresholdSitStand
                        && backwardDiff <= thresholdStairs) {
                    // this is a sit-to-stand
                    activityTransitions[i] = SIT_TO_STAND;
                } else if (forwardDiff > thresholdStairs && backwardDiff > thresholdStairs) {
                    // this is a stairs up
                    activityTransitions[i] = LEVEL_GROUND_TO_STAIRS_UP;
                }

            } else if (altitudeChangeIdx[i] == -1) {// if sloping negative
                if (forwardDiff <= -thresholdSitStand && forwardDiff >= -thresholdStairs
                        && backwardDiff <= -thresholdSitStand
                        && backwardDiff >= -thresholdStairs) {
                    // this is a stand-to-sit
                    activityTransitions[i] = STAND_TO_SIT;
                } else if (forwardDiff < -thresholdStairs && backwardDiff < -thresholdStairs) {
                    // this is a stairs down
                    activityTransitions[i] = LEVEL_GROUND_TO_STAIRS_DOWN;
                }
            }
        }

        return activityTransitions;
    }

    @SuppressWarnings("unused")
    private static double[] fuseMotilityHVWithAltitudeTransitions(double[] activityMotilityLabelHV,
            int[] activityTransitionAltitude) {
        if (activityMotilityLabelHV.length != activityTransitionAltitude.length) {
            throw new IllegalArgumentException("fusedActivity and activityTransitionAltitude must be of same length");
        }

        double[] overallActivity = new double[activityMotilityLabelHV.length];

        // ! Critical part,, check for correct implementation
        for (int i = 0; i < overallActivity.length - PATCH_WIDTH; i++) {
            overallActivity[i] = activityMotilityLabelHV[i];
            if (isWalkingOrRunning(activityMotilityLabelHV[i])) {
                if (i >= PATCH_WIDTH) {
                    switch (activityTransitionAltitude[i]) {
                        case LEVEL_GROUND_TO_STAIRS_UP -> {
                            modifyOverallActivityCreatePatch(overallActivity, i, LEVEL_GROUND_TO_STAIRS_UP);
                            i = i + PATCH_WIDTH;
                            break;
                        }
                        case LEVEL_GROUND_TO_STAIRS_DOWN -> {
                            modifyOverallActivityCreatePatch(overallActivity, i, LEVEL_GROUND_TO_STAIRS_DOWN);
                            i = i + PATCH_WIDTH;
                            break;
                        }
                        default -> {
                        }
                    }

                }
            } else if (isStationaryOrLyingDown(activityMotilityLabelHV[i])) {
                if (i >= PATCH_WIDTH) {
                    switch (activityTransitionAltitude[i]) {
                        case SIT_TO_STAND -> {
                            modifyOverallActivityCreatePatch(overallActivity, i, SIT_TO_STAND);
                            break;
                        }
                        case STAND_TO_SIT -> {
                            modifyOverallActivityCreatePatch(overallActivity, i, STAND_TO_SIT);
                            break;
                        }
                        default -> {
                        }
                    }

                }
            }
        }
        return overallActivity;
    }

    private static boolean isWalkingOrRunning(double activity) {
        return activity == WALKING || activity == RUNNING;
    }

    private static boolean isStationaryOrLyingDown(double activity) {
        return activity == STATIONARY || activity == LYING_DOWN;
    }

    private static void modifyOverallActivityCreatePatch(double[] activityWithAltitude, int i, int patchValue) {
        for (int j = i; j > i - PATCH_WIDTH; j--) {
            activityWithAltitude[j] = patchValue;
        }
    }

    /**
     * Interpolates the given input array to a desired length. This method uses
     * linear interpolation to fill in the gaps.
     * 
     * @param inputArray    The original data array to be interpolated.
     * @param desiredLength The length of the desired output array.
     * @return The interpolated data array of the desired length.
     */
    public static double[] interpolateArray(double[] inputArray, int desiredLength) {
        if (inputArray == null) {
            throw new NullPointerException("Input array cannot be null");
        }

        int originalLength = inputArray.length;
        if (originalLength == 0) {
            throw new IllegalArgumentException("Input array cannot be empty");
        }

        if (desiredLength <= 0) {
            throw new IllegalArgumentException("Desired length must be a positive integer");
        }

        double[] interpolatedArray = new double[desiredLength];

        // Create x values array
        double[] xValues = new double[originalLength];
        for (int i = 0; i < originalLength; i++) {
            xValues[i] = (double) i / (originalLength - 1);
        }

        // Interpolate for each point in the desired array
        for (int i = 0; i < desiredLength; i++) {
            double x = (double) i / (desiredLength - 1);

            // Find two surrounding points in the input array
            int index0 = Arrays.binarySearch(xValues, x);
            int index1;
            if (index0 < 0) {
                // if the exact match is not found, take the closest one
                index0 = -index0 - 2;
                index1 = Math.min(index0 + 1, originalLength - 1);
            } else {
                // otherwise take the next one
                index1 = Math.min(index0 + 1, originalLength - 1);
            }

            double x0 = xValues[index0];
            double x1 = xValues[index1];
            double y0 = inputArray[index0];
            double y1 = inputArray[index1];

            // Calculate the slope and apply it to the interpolated value
            double dx = (y1 - y0) / (x1 - x0);
            interpolatedArray[i] = y0 + (x - x0) * dx;
        }

        return interpolatedArray;
    }

    /**
     * Convert raw accelerometer counts to physical acceleration in m/s^2 using the
     * per-channel calibration (real slope/constant). The device units are first
     * converted to g, then multiplied by standard gravity.
     * This mirrors the earlier working path in analyseMotility where values were
     * computed as raw * realSlope * 9.8.
     *
     * NOTE: Call this once per chunk and reuse the resulting arrays so that
     * multiple algorithms (steps, posture, etc.) operate on identical inputs.
     */
    private static double[] toMs2(int[] raw, Ams7fsChannelInfo chan) {
        // Match the conversion used in analyseMotility: counts -> g via realSlope,
        // then g -> m/s^2 by multiplying with standard gravity. Do NOT divide by slope.
        // We intentionally do not use realConstant here because analyseMotility
        // did not apply it either; using the same convention keeps values consistent
        // across steps, posture, and motility (~9.8 m/s^2 at rest).
        final double g = 9.80665; // m/s^2
        final double slope = chan.getRealSlope();
        double[] out = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            double inG = raw[i] * slope; // counts -> g
            out[i] = inG * g; // g -> m/s^2
        }
        return out;
    }

    public static double[] diff(double[] array) {
        double[] result = new double[array.length - 1];
        for (int i = 0; i < array.length - 1; i++) {
            result[i] = array[i + 1] - array[i];
        }
        return result;
    }

    /**
     * Save multiple numeric arrays as tab-separated columns.
     * Overloads are provided for double[], int[], and boxed Object[] arrays.
     */
    public static void saveColumns(String filename, boolean overwrite, double[]... columns) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, !overwrite))) {
            int numRows = 0;
            for (double[] col : columns) {
                if (col != null && col.length > numRows)
                    numRows = col.length;
            }
            for (int r = 0; r < numRows; r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < columns.length; c++) {
                    if (columns[c] != null && r < columns[c].length) {
                        line.append(columns[c][r]);
                    }
                    if (c < columns.length - 1)
                        line.append('\t');
                }
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
        }
    }

    public static void saveColumns(String filename, boolean overwrite, int[]... columns) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, !overwrite))) {
            int numRows = 0;
            for (int[] col : columns) {
                if (col != null && col.length > numRows)
                    numRows = col.length;
            }
            for (int r = 0; r < numRows; r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < columns.length; c++) {
                    if (columns[c] != null && r < columns[c].length) {
                        line.append(columns[c][r]);
                    }
                    if (c < columns.length - 1)
                        line.append('\t');
                }
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
        }
    }

    public static void saveColumns(String filename, boolean overwrite, Object[]... columns) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, !overwrite))) {
            int numRows = 0;
            for (Object[] col : columns) {
                if (col != null && col.length > numRows)
                    numRows = col.length;
            }
            for (int r = 0; r < numRows; r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < columns.length; c++) {
                    if (columns[c] != null && r < columns[c].length) {
                        line.append(String.valueOf(columns[c][r]));
                    }
                    if (c < columns.length - 1)
                        line.append('\t');
                }
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
        }
    }

    /**
     * method is used to detect steps in the given array using acceleration
     * 
     * @param accelX in m/s^2
     * @param accelY in m/s^2
     * @param accelZ in m/s^2
     * @return locations of steps
     * @deprecated Use StepDetector.getInstance().detectSteps() instead
     */
    @Deprecated
    public static int[] getStepsNuovo(double[] accelX, double[] accelY, double[] accelZ) {
        return StepDetector.getInstance().detectSteps(accelX, accelY, accelZ, chunkno,
                ActivityClassification::correctForTicks);
    }

    /**
     * Convenience overload: posture without providing steps.
     * Delegates to the 4-arg version with stepsOpt = null.
     */
    public static String[] getPostureNuovo(double[] accelX, double[] accelY, double[] accelZ) {
        return getPostureNuovo(accelX, accelY, accelZ, null);
    }

    /**
     * Core posture classifier (10 s windows @ 1000 Hz).
     * If stepsOpt is null, steps are detected internally.
     */
    public static String[] getPostureNuovo(double[] accelX, double[] accelY, double[] accelZ, int[] stepsOpt) {
        // --- Input validation ---
        if (accelX == null || accelY == null || accelZ == null) {
            throw new IllegalArgumentException("Input arrays cannot be null");
        }
        int n = Math.min(accelX.length, Math.min(accelY.length, accelZ.length));
        if (n <= 0)
            return new String[0];

        // --- Trim arrays to same length ---
        double[] x = (n == accelX.length) ? accelX : java.util.Arrays.copyOf(accelX, n);
        double[] y = (n == accelY.length) ? accelY : java.util.Arrays.copyOf(accelY, n);
        double[] z = (n == accelZ.length) ? accelZ : java.util.Arrays.copyOf(accelZ, n);

        // --- Step data handling ---
        int[] steps;
        if (stepsOpt != null) {
            if (stepsOpt.length < n) {
                steps = java.util.Arrays.copyOf(stepsOpt, n);
            } else if (stepsOpt.length > n) {
                steps = java.util.Arrays.copyOfRange(stepsOpt, 0, n);
            } else {
                steps = stepsOpt;
            }
        } else {
            steps = getStepsNuovo(x, y, z);
        }

        // --- Parameter definitions ---
        final int Fs = SAMPLING_FREQUENCY; // 1000 Hz
        final int WINDOW_SEC = 10;
        final int MIN_STEPS_IN_10_SECONDS = 2;
        final int NUM_SAMPLES_PER_10S = WINDOW_SEC * Fs;

        // --- Intensity thresholds (tunable) ---
        final double G_CONST = 9.80665; // m/s^2
        final double LPA_THRESHOLD = 1.0;
        final double MPA_THRESHOLD = 3.0;
        final double STATIC_THRESHOLD = -2; // m/s^2

        // --- Label each 10-second window ---
        int nWindows = (int) Math.ceil(n / (double) NUM_SAMPLES_PER_10S);
        String[] windowLabels = new String[nWindows];

        for (int w = 0; w < nWindows; w++) {
            int wStart = w * NUM_SAMPLES_PER_10S;
            int wEnd = Math.min(n, wStart + NUM_SAMPLES_PER_10S);

            int stepCount = 0;
            boolean isDynamic = false;

            for (int i = wStart; i < wEnd; i++) {
                if (steps[i] == 1) {
                    stepCount++;
                    if (stepCount > MIN_STEPS_IN_10_SECONDS) {
                        isDynamic = true;
                        break;
                    }
                }
            }

            windowLabels[w] = "Unknown";

            if (isDynamic) {
                double sumDyn = 0.0;
                int count = Math.max(1, (wEnd - wStart));
                for (int i = wStart; i < wEnd; i++) {
                    double totalAcceleration = FastMath.hypot(FastMath.hypot(x[i], y[i]), z[i]);
                    double dynamicAcceleration = FastMath.max(totalAcceleration - G_CONST, 0.0);
                    if (dynamicAcceleration < 0)
                        dynamicAcceleration = 0;
                    sumDyn += dynamicAcceleration;
                }
                double meanDyn = sumDyn / count;

                if (meanDyn < LPA_THRESHOLD) {
                    windowLabels[w] = "Dynamic LPA";
                } else if (meanDyn < MPA_THRESHOLD) {
                    windowLabels[w] = "Dynamic MPA";
                } else {
                    windowLabels[w] = "Dynamic VPA";
                }
            } else {
                double avgX = 0.0;
                for (int i = wStart; i < wEnd; i++) {
                    avgX += x[i];
                }
                avgX /= Math.max(1, (wEnd - wStart));

                if (avgX >= STATIC_THRESHOLD)
                    windowLabels[w] = "Static Lying";
                else
                    windowLabels[w] = "Static Upright";
            }
        }

        // Debug
        System.out.println("10s-window posture classification completed: " + nWindows + " window(s).");
        for (int w = 0; w < windowLabels.length; w++) {
            System.out.println("Window " + (w + 1) + ": " + windowLabels[w]);
        }

        return windowLabels;
    }

    public static double analyseMotility(int[] sampMX, int[] sampMY, int[] sampMZ, int[] stepLocations,
            double sampTemp[], double sampPres[],
            int analysisWindowSize, double threshold, double thresholdLow, FileChannel fosLying,
            FileChannel fosCat, FileChannel fosAlt, int chunkno) {

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
        double[] sampleMXDouble = toMs2(java.util.Arrays.copyOf(sampMX, minLength), chanXLoc);
        double[] sampleMYDouble = toMs2(java.util.Arrays.copyOf(sampMY, minLength), chanYLoc);
        double[] sampleMZDouble = toMs2(java.util.Arrays.copyOf(sampMZ, minLength), chanZLoc);

        // **********************************************************************/

        long startTime = (chunkno * size / 4);
        startTime = correctForTicks(startTime);

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

        // --- Posture + Stairs fusion from within analyseMotility ---
        // Note: getPostureNuovo runs on 10 s windows at 1000 Hz (X/Y/Z in m/s^2).
        // analyseAltitudeChange returns a per-sample (1000 Hz) label stream derived
        // from 5 Hz pressure.
        // We fuse by scanning the corresponding 10 s span in the per-sample stairs
        // labels.
        AltitudeAnalysisResult altRes1s = analyseAltitudeChange(sampPres, stepLocations, 5);
        String[] posture10s = getPostureNuovo(sampleMXDouble, sampleMYDouble, sampleMZDouble, stepLocations);
        String[] fused10s = fusePostureWithStairs10s(posture10s, altRes1s.labels);

        // Cache fused results for this chunk (do not write epoch labels here to avoid
        // duplication).
        lastFusedPosture10s.clear();
        java.util.Collections.addAll(lastFusedPosture10s, fused10s);
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

    @SuppressWarnings("null")
    public static void cleanupLabels(String labelset) {
        TreeSet<AmsLabel> toRemove = new TreeSet<AmsLabel>();
        TreeSet<AmsLabel> toAdd = new TreeSet<AmsLabel>();
        TreeSet<AmsLabel> labels = null;
        if (labelset.equals("Activity")) {
            labels = CurrentOpenData.getInstance().getPostureLabels().getLabels();
        } else {
            labels = CurrentOpenData.getInstance().getStairsLabels().getLabels();
        }
        boolean done = false;

        while (!done) {
            for (AmsLabel l : labels) {
                NavigableSet<AmsLabel> tailSet = labels.tailSet(l, false);
                if (tailSet.isEmpty()) {
                    break;
                }
                AmsLabel l2 = tailSet.getFirst();
                if (l.getRightTime() - l.getLeftTime() < 1500000) {
                    toRemove.add(l);
                } else if (l2.getLeftTime() < l.getRightTime() + 1500000
                        && l.getAttributes().get(labelset).equals(l2.getAttributes().get(labelset))) { // partial
                    // overlap
                    // or less the
                    // 3s
                    // apart
                    double lTime = l.getLeftTime();
                    double rTime = l2.getRightTime();
                    String reason = l.getAttributes().get(labelset);
                    toRemove.add(l);
                    toRemove.add(l2);
                    if (labelset.equals("Activity")) {
                        toAdd.add(AmsLabel.generatePostureLabel(lTime, rTime, reason));
                    } else {
                        toAdd.add(AmsLabel.generateStairsLabel(lTime, rTime, reason));
                    }
                }
            }
            if (!labels.removeAll(toRemove) && toRemove.size() > 0)
                done = true;
            if (!labels.addAll(toAdd) && toAdd.size() > 0)
                done = true;
            if (toRemove.size() == 0 && toAdd.size() == 0)
                done = true;
            toRemove.clear();
            toAdd.clear();
        }
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
            // TODO:Check why the values are so large
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

    /**
     * Helper method to save data to a file.
     * 
     * @param data
     * @param filename
     * @param overwrite whether to overwrite or append to file
     */
    @SuppressWarnings("unused")
    private static void saveDataToFile(Object[][] data, String filename, boolean overwrite) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, !overwrite))) {
            int numRows = 0;
            for (Object[] col : data) {
                if (col != null && col.length > numRows) {
                    numRows = col.length;
                }
            }
            for (int row = 0; row < numRows; row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < data.length; col++) {
                    if (data[col] != null && row < data[col].length) {
                        line.append(String.valueOf(data[col][row]));
                    }
                    line.append("\t");
                }
                writer.println(line.toString().trim());
            }
            System.out.println((overwrite ? "Data overwritten in: " : "Data appended to: ") + filename);
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
        }
    }

    /**
     * Save a numeric array (e.g., altitude, velocity, etc.) to a text file along
     * with a time column.
     * The time starts from 0 and increments by 1/Fs seconds for each sample.
     *
     * @param data   Array of double values to save.
     * @param Fs     Sampling frequency in Hz (used to compute time).
     * @param path   Path of the output text file.
     * @param append If true, appends to existing file; otherwise overwrites.
     */
    public static void saveToTextFileWithTime(double[] data, double Fs, String filename, boolean append) {
        if (data == null || data.length == 0) {
            System.err.println("saveToTextFileWithTime: input array is empty or null");
            return;
        }

        try {
            // Save alongside the compiled ActivityClassification class (i.e., this module’s
            // dir)
            String classDir = new File(ActivityClassification.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getPath();
            File parentDir = new File(classDir).getParentFile();
            File file = new File(parentDir, filename);

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (PrintWriter out = new PrintWriter(new FileWriter(file, append))) {
                if (!append) {
                    out.println("# time_s\tvalue");
                    out.println("# Fs = " + Fs + " Hz");
                }
                for (int i = 0; i < data.length; i++) {
                    double t = i / Fs;
                    out.println(t + "\t" + data[i]);
                }
            }
            System.out.println("Saved variable to: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public static List<String> getLastFusedPosture10s() {
        return new ArrayList<>(lastFusedPosture10s);
    }

    /**
     * Fuse 10-second posture labels with per-sample (1000 Hz) stairs labels.
     * Rules:
     * - Only override windows that are already Dynamic (LPA/MPA/VPA).
     * - If any sample within the 10 s window is "Stairs up" -> "Dynamic MPA
     * Stairs".
     * - Else if any sample within the 10 s window is "Stairs down" -> "Dynamic LPA
     * Stairs".
     * - Static windows remain unchanged.
     */
    private static String[] fusePostureWithStairs10s(String[] posture10s, String[] stairs1000Hz) {
        if (posture10s == null || stairs1000Hz == null)
            return posture10s;
        final int Fs = SAMPLING_FREQUENCY; // 1000 Hz
        final int WINDOW_S = 10; // 10-second posture windows
        final int SAMPLES_PER_WINDOW = Fs * WINDOW_S; // 10,000 samples
        String[] out = java.util.Arrays.copyOf(posture10s, posture10s.length);
        for (int w = 0; w < posture10s.length; w++) {
            String base = posture10s[w];
            if (base == null || !base.startsWith("Dynamic "))
                continue; // only override dynamic
            int start = w * SAMPLES_PER_WINDOW;
            if (start >= stairs1000Hz.length)
                break;
            int end = Math.min(stairs1000Hz.length, start + SAMPLES_PER_WINDOW);
            boolean hasUp = false, hasDown = false;
            for (int i = start; i < end; i++) {
                String s = stairs1000Hz[i];
                if (s == null)
                    continue;
                if ("Stairs up".equals(s)) {
                    hasUp = true;
                    break;
                }
                if ("Stairs down".equals(s)) {
                    hasDown = true;
                }
            }
            if (hasUp)
                out[w] = "Dynamic MPA StairsUp";
            else if (hasDown)
                out[w] = "Dynamic LPA StairsDown";
        }
        return out;
    }
}
