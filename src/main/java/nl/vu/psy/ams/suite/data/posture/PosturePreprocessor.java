package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.structures.Sample;
import nl.vu.psy.ams.suite.tools.VirtualThreadTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.LongStream;

public class PosturePreprocessor {

    public record PreprocessingResult(List<Sample> samples, long epochDurationMicros) {
    }

    private static final int featureCount = 98;
    private static final int downSamplingFactor = 10;
    static final int epochSize = 600;

    private static final Logger logger = LogManager.getLogger(PosturePreprocessor.class);
    private static PosturePreprocessor instance;

    double[] mxr, myr, mzr, gyroX, gyroY, gyroZ;
    private double[] mxrGravCorr, myrGravCorr, mzrGravCorr;
    double[] vmGravCorr, vmGyro;
    private double samplingFrequency;
    private long samplingTimeMicros;
    private int sampleSize;
    Sample[] features;

    public static synchronized PosturePreprocessor getInstance() {
        if (instance == null)
            instance = new PosturePreprocessor();
        return instance;
    }

    private PosturePreprocessor() {
    }

    public void setAccelSamples(double[] mxr, double[] myr, double[] mzr) {
        this.mxr = mxr;
        this.myr = myr;
        this.mzr = mzr;
    }

    public void setGyroSamples(double[] gyroX, double[] gyroY, double[] gyroZ) {
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
    }

    public PreprocessingResult preprocess(long startTimeMicros, long sampleTimePeriodMicros) {
        // Check null reference
        if (mxr == null || myr == null || mzr == null || gyroX == null || gyroY == null || gyroZ == null) {
            logger.error("Motion data is invalid. Stop preprocessing.");
            return new PreprocessingResult(List.of(), 0);
        }

        // Number of raw motion samples and fs
        sampleSize = mxr.length;
        samplingFrequency = 1_000_000d / (double) sampleTimePeriodMicros;
        this.samplingTimeMicros = sampleTimePeriodMicros;

        // Check sample sizes
        if (myr.length != sampleSize || mzr.length != sampleSize ||
                gyroX.length != sampleSize || gyroY.length != sampleSize || gyroZ.length != sampleSize) {
            logger.error("Motion data is invalid. Stop preprocessing.");
            return new PreprocessingResult(List.of(), 0);
        }

        // Preprocess epochs
        logger.info("Start preprocessing. Sample size: {}", sampleSize);

        // Do filter, resample and calculate vector magnitude
        lowPassFilterAndResample();
        highPassFilter();
        calculateVectorMagnitude();

        // Init result array
        int epochCount = sampleSize / epochSize;
        features = LongStream.range(0, epochCount)
                .mapToObj(i -> new Sample(startTimeMicros + samplingTimeMicros * epochSize * i,
                        new float[featureCount]))
                .toArray(Sample[]::new);

        // Process epochs
        for (int i = 0; i < epochCount; i++) {
            processEpoch(i);
        }

        logger.info("Preprocessing has finished.");
        long epochDurationMicros = samplingTimeMicros * epochSize;
        return new PreprocessingResult(List.of(features), epochDurationMicros);
    }

    private void lowPassFilterAndResample() {
        try {
            VirtualThreadTask.runInParallel(
                    // Low pass filter: order = 2, cutoff = 5 Hz
                    // Decimate using zerophase filter
                    () -> {
                        ZeroPhaseFilter.lowPass(mxr, samplingFrequency, 2, 5);
                        mxr = ZeroPhaseFilter.decimate(mxr, samplingFrequency, downSamplingFactor);
                    },
                    () -> {
                        ZeroPhaseFilter.lowPass(myr, samplingFrequency, 2, 5);
                        myr = ZeroPhaseFilter.decimate(myr, samplingFrequency, downSamplingFactor);
                    },
                    () -> {
                        ZeroPhaseFilter.lowPass(mzr, samplingFrequency, 2, 5);
                        mzr = ZeroPhaseFilter.decimate(mzr, samplingFrequency, downSamplingFactor);
                    },
                    () -> {
                        ZeroPhaseFilter.lowPass(gyroX, samplingFrequency, 2, 5);
                        gyroX = ZeroPhaseFilter.decimate(gyroX, samplingFrequency, downSamplingFactor);
                    },
                    () -> {
                        ZeroPhaseFilter.lowPass(gyroY, samplingFrequency, 2, 5);
                        gyroY = ZeroPhaseFilter.decimate(gyroY, samplingFrequency, downSamplingFactor);
                    },
                    () -> {
                        ZeroPhaseFilter.lowPass(gyroZ, samplingFrequency, 2, 5);
                        gyroZ = ZeroPhaseFilter.decimate(gyroZ, samplingFrequency, downSamplingFactor);
                    });

            // New sample size and frequency
            sampleSize = mxr.length;
            samplingFrequency /= downSamplingFactor;
            samplingTimeMicros *= downSamplingFactor;

            // Check sample sizes
            if (myr.length != sampleSize || mzr.length != sampleSize ||
                    gyroX.length != sampleSize || gyroY.length != sampleSize || gyroZ.length != sampleSize) {
                String message = "Motion data is invalid after resampling.";
                logger.error(message);
                throw new RuntimeException(message);
            }

        } catch (InterruptedException | ExecutionException e) {
            String message = "An error occurred while low-pass filtering and resampling motion data.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void highPassFilter() {
        try {
            VirtualThreadTask.runInParallel(
                    // Copy arrays
                    // High pass filter: order = 1, cutoff = 0.1 Hz
                    () -> {
                        mxrGravCorr = Arrays.copyOf(mxr, mxr.length);
                        ZeroPhaseFilter.highPass(mxrGravCorr, samplingFrequency, 1, 0.1);
                    },
                    () -> {
                        myrGravCorr = Arrays.copyOf(myr, myr.length);
                        ZeroPhaseFilter.highPass(myrGravCorr, samplingFrequency, 1, 0.1);
                    },
                    () -> {
                        mzrGravCorr = Arrays.copyOf(mzr, mzr.length);
                        ZeroPhaseFilter.highPass(mzrGravCorr, samplingFrequency, 1, 0.1);
                    });
        } catch (InterruptedException | ExecutionException e) {
            logger.error("An error occurred while high-pass filtering motion data.", e);
            throw new RuntimeException("An error occurred while high-pass filtering motion data.", e);
        }
    }

    private void calculateVectorMagnitude() {
        // Create magnitude arrays
        vmGyro = new double[sampleSize];
        vmGravCorr = new double[sampleSize];

        for (int i = 0; i < sampleSize; i++) {
            vmGyro[i] = Math.sqrt(gyroX[i] * gyroX[i] + gyroY[i] * gyroY[i] + gyroZ[i] * gyroZ[i]);
            vmGravCorr[i] = Math.sqrt(mxrGravCorr[i] * mxrGravCorr[i] + myrGravCorr[i] * myrGravCorr[i]
                    + mzrGravCorr[i] * mzrGravCorr[i]);
        }
    }

    void processEpoch(int epochIndex) {
        FeatureCalculator calculator = new FeatureCalculator(this);
        calculator.calculateForEpoch(epochIndex);
    }
}
