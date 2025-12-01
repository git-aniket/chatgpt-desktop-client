package nl.vu.psy.ams.suite.data.posture;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.FastMath;

import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.signal.Resample;

import nl.vu.psy.ams.suite.data.structures.AmsLabel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;

/**
 * PhysicalActivityCalculator
 * Description: This class provides methods to estimate METs (Metabolic
 * Equivalent of Task), and MAD's.
 * It uses two methods: Freedson and Brage for MET estimation.
 * 
 * References:
 * 1)https://actigraphcorp.my.site.com/support/s/article/What-is-the-difference-among-the-MET-Algorithms
 * 2)https://journals.physiology.org/doi/epdf/10.1152/japplphysiol.00703.2003
 * 3)https://www.camntech.com/Products/Actiheart/The%20Actiheart%20User%20Manual.pdf#page=75.06
 * 
 * @author
 */

public class PhysicalActivityCalculator {

    public enum Method {
        FREEDSON,
        BRAGE_NONBRANCHED,
        BRAGE_BRANCHED
    }

    private static PhysicalActivityCalculator instance;

    private final int SAMPLING_FREQUENCY = 1000; // Hz
    private final int samplesPerMinute = SAMPLING_FREQUENCY * 60; // 60*1000 seconds
    private int fullMinutes = 0;
    private int remainingSamples = 0;

    private double[] filteredX;
    private double[] filteredY;
    private double[] filteredZ;

    public static synchronized PhysicalActivityCalculator getInstance() {
        if (instance == null)
            instance = new PhysicalActivityCalculator();
        return instance;
    }

    private PhysicalActivityCalculator() {
    }

    public void setAccelerometerData(double[] xData, double[] yData, double[] zData) {
        // Apply filter to the raw data
        this.filteredX = zeroPhaseFilter(xData, 2, 0.25, 2.5, SAMPLING_FREQUENCY);
        this.filteredY = zeroPhaseFilter(yData, 2, 0.25, 2.5, SAMPLING_FREQUENCY);
        this.filteredZ = zeroPhaseFilter(zData, 2, 0.25, 2.5, SAMPLING_FREQUENCY);

        this.fullMinutes = filteredX.length / samplesPerMinute;
        this.remainingSamples = filteredX.length % samplesPerMinute;
    }

    // Apply zero-phase Butterworth filter
    private double[] zeroPhaseFilter(double[] signal, int order, double lowCut, double highCut, int Fs) {
        // Determine padding length. A common heuristic is 3 times the filter order.
        // Ensure padding length does not exceed the signal length to avoid issues with
        // reflection.
        int padlen = Math.min(signal.length - 1, 3 * order);
        if (padlen <= 0) { // Handle very short signals or order 0
            padlen = 1; // Minimum padding
        }

        // 1. Create a padded signal using reflection
        double[] paddedSignal = new double[signal.length + 2 * padlen];

        // Pad the beginning by reflecting the first 'padlen' samples
        for (int i = 0; i < padlen; i++) {
            paddedSignal[i] = signal[padlen - 1 - i];
        }

        // Copy the original signal to the middle
        System.arraycopy(signal, 0, paddedSignal, padlen, signal.length);

        // Pad the end by reflecting the last 'padlen' samples
        for (int i = 0; i < padlen; i++) {
            paddedSignal[padlen + signal.length + i] = signal[signal.length - 1 - i];
        }

        // 2. Apply forward filter
        Butterworth filterForward = new Butterworth(Fs);
        double[] forwardFilteredPadded = filterForward.bandPassFilter(paddedSignal, order, lowCut, highCut);

        // 3. Reverse the signal
        double[] reversedPadded = reverseArray(forwardFilteredPadded);

        // 4. Apply reverse filter
        Butterworth filterReverse = new Butterworth(Fs); // Fresh instance
        double[] backwardFilteredPadded = filterReverse.bandPassFilter(reversedPadded, order, lowCut, highCut);

        // 5. Final reversal
        double[] zeroPhaseFilteredPadded = reverseArray(backwardFilteredPadded);

        // 6. Trim the signal to remove padding
        double[] result = new double[signal.length];
        System.arraycopy(zeroPhaseFilteredPadded, padlen, result, 0, signal.length);

        return result;
    }

    private double[] reverseArray(double[] array) {
        double[] reversed = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            reversed[i] = array[array.length - 1 - i];
        }
        return reversed;
    }

    /**
     * Compute CPM from raw data using ActiGraph algorithm.
     * https://www.researchsquare.com/article/rs-1370418/v1
     * 
     * @param filteredX
     * @param filteredY
     * @param filteredZ
     * @param startIndex
     * @param endIndex
     * @return
     */
    private double computeCPMFromRawDataActiGraph(double[] filteredX, double[] filteredY, double[] filteredZ,
            int startIndex, int endIndex) {

        double countsPerMinute = 0.0;

        // Select the data based on the start and end index
        if (startIndex < 0 || endIndex > filteredX.length || startIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid start or end index for the data.");
        }
        // Extract the relevant portion of the data
        filteredX = java.util.Arrays.copyOfRange(filteredX, startIndex, endIndex);
        filteredY = java.util.Arrays.copyOfRange(filteredY, startIndex, endIndex);
        filteredZ = java.util.Arrays.copyOfRange(filteredZ, startIndex, endIndex);

        // Assuming sampling frequency is 1000 Hz
        // Resample to 3000Hz frequency
        int UP = 3;
        int DOWN = 1;
        Resample resampler = new Resample(UP, DOWN, "median");

        double[] resampledX3000 = resampler.resampleSignal(filteredX);
        double[] resampledY3000 = resampler.resampleSignal(filteredY);
        double[] resampledZ3000 = resampler.resampleSignal(filteredZ);

        // Resample to 30Hz frequency
        int M = 100; // 3000/30;

        // save every 100th sample
        double[] resampledX30 = new double[resampledX3000.length / M];
        double[] resampledY30 = new double[resampledY3000.length / M];
        double[] resampledZ30 = new double[resampledZ3000.length / M];
        for (int i = 0; i < resampledX3000.length; i += M) {
            int index = i / M;
            if (index < resampledX30.length) {
                resampledX30[index] = resampledX3000[i];
                resampledY30[index] = resampledY3000[i];
                resampledZ30[index] = resampledZ3000[i];
            }
        }

        // Bandpass filter the data with IIR filter 7th order
        double[] filteredX30 = zeroPhaseFilter(resampledX30, 7, 0.25, 3.1, 30);
        double[] filteredY30 = zeroPhaseFilter(resampledY30, 7, 0.25, 3.1, 30);
        double[] filteredZ30 = zeroPhaseFilter(resampledZ30, 7, 0.25, 3.1, 30);

        // Rescale the filtered data by factor 'a'
        double a = 17.127; // Rescaling factor
        for (int i = 0; i < filteredX30.length; i++) {
            filteredX30[i] *= a;
            filteredY30[i] *= a;
            filteredZ30[i] *= a;
        }

        // Rectify the rescaled data by taking the absolute value
        for (int i = 0; i < filteredX30.length; i++) {
            filteredX30[i] = Math.abs(filteredX30[i]);
            filteredY30[i] = Math.abs(filteredY30[i]);
            filteredZ30[i] = Math.abs(filteredZ30[i]);
        }

        // Threshold the rectified signal so that all entries greater than 128 is set to
        // 128 and all entries smaller than 4 are set to 0
        for (int i = 0; i < filteredX30.length; i++) {
            filteredX30[i] = Math.min(128,
                    Math.max(0, filteredX30[i] > 4 ? (int) Math.floor(filteredX30[i]) : filteredX30[i]));
            filteredY30[i] = Math.min(128,
                    Math.max(0, filteredY30[i] > 4 ? (int) Math.floor(filteredY30[i]) : filteredY30[i]));
            filteredZ30[i] = Math.min(128,
                    Math.max(0, filteredZ30[i] > 4 ? (int) Math.floor(filteredZ30[i]) : filteredZ30[i]));
        }

        // Further downsample the data and low-pass filter to 10Hz by a non-overlapping
        // moving average
        int downsampleFactor = 3; // Downsample to 10Hz
        double[] downsampledX = new double[(filteredX30.length + downsampleFactor - 1) / downsampleFactor];
        double[] downsampledY = new double[(filteredY30.length + downsampleFactor - 1) / downsampleFactor];
        double[] downsampledZ = new double[(filteredZ30.length + downsampleFactor - 1) / downsampleFactor];

        for (int i = 0; i < filteredX30.length; i += downsampleFactor) {
            int index = i / downsampleFactor;
            if (index < downsampledX.length) {
                double sumX = 0, sumY = 0, sumZ = 0;
                int count = 0;
                for (int j = i; j < filteredX30.length && j < i + downsampleFactor; j++) {
                    sumX += filteredX30[j];
                    sumY += filteredY30[j];
                    sumZ += filteredZ30[j];
                    count++;
                }
                downsampledX[index] = sumX / count;
                downsampledY[index] = sumY / count;
                downsampledZ[index] = sumZ / count;
            }
        }

        // Get the count by summing the data within the predefined epoch
        // given the epoch duration of 60 seconds, and the sampling frequency of 10Hz,
        // get the counts per minute
        for (int i = 0; i < downsampledX.length; i++) {
            double count = downsampledX[i] + downsampledY[i] + downsampledZ[i];
            if (count > 0) {
                countsPerMinute += count;
            }
        }

        return countsPerMinute;
    }

    /**
     * Estimate METs (Metabolic Equivalent of Task) from filtered accelerometer data
     * using one of several validated estimation models.
     *
     * @param startTime timestamp to align heart rate data
     * @param method    estimation method (FREEDSON, BRAGE_NONBRANCHED,
     *                  BRAGE_BRANCHED)
     * @return list of MET values per minute
     */
    public List<Double> estimateMETs(long startTime, Method method) {
        List<Double> metList = new ArrayList<>();

        if (filteredX.length < samplesPerMinute) {
            throw new IllegalArgumentException(
                    "Data length is less than one minute. At least 60000 samples are required.");
        }

        if (method == null) {
            throw new IllegalArgumentException("Unknown estimation method.");
        }

        int fullMinutes = filteredX.length / samplesPerMinute;
        int remainingSamples = filteredX.length % samplesPerMinute;

        switch (method) {
            case FREEDSON -> {
                for (int m = 0; m < fullMinutes; m++) {
                    int startIdx = m * samplesPerMinute;
                    int endIdx = startIdx + samplesPerMinute;
                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);
                    double met = 1.439008 + (0.000795 * cpm);
                    metList.add(met);
                }
                if (remainingSamples > 0) {
                    int startIdx = filteredX.length - remainingSamples;
                    int endIdx = filteredX.length;
                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);
                    double met = 1.439008 + (0.000795 * cpm);
                    metList.add(met);
                }
            }

            case BRAGE_NONBRANCHED -> {
                for (int m = 0; m < fullMinutes; m++) {
                    int startIdx = m * samplesPerMinute;
                    int endIdx = startIdx + samplesPerMinute;

                    double hr = getHRperMinute(startTime, m) - 44.0;
                    if (hr < 0)
                        hr = 44;

                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);
                    double PAEE = 0.028 * cpm + 4.04 * hr - 38.3;
                    PAEE = Math.max(0, PAEE);

                    // Convert PAEE (kJ/min) to METs:
                    // 1 MET ≈ 3.5 mL O₂/kg/min, and 1 L O₂ ≈ 20.92 kJ
                    // (PAEE / 3.5) gives L O₂/min; multiplied by (200 / 4184) to convert to METs
                    double met = (PAEE / 3.5) * (200.0 / 4184.0);
                    metList.add(met);
                }

                if (remainingSamples > 0) {
                    int startIdx = filteredX.length - remainingSamples;
                    int endIdx = filteredX.length;
                    double hr = getHRperMinute(startTime, fullMinutes) - 44.0;
                    if (hr < 0)
                        hr = 44;
                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);
                    double PAEE = 0.028 * cpm + 4.04 * hr - 38.3;
                    PAEE = Math.max(0, PAEE);

                    // Same conversion as above
                    double met = (PAEE / 3.5) * (200.0 / 4184.0);
                    metList.add(met);
                }
            }

            case BRAGE_BRANCHED -> {
                for (int m = 0; m < fullMinutes; m++) {
                    int startIdx = m * samplesPerMinute;
                    int endIdx = startIdx + samplesPerMinute;

                    double hr = getHRperMinute(startTime, m);
                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);

                    double adjustedHR = Math.max(0, hr - 44.0);
                    if (adjustedHR == 0)
                        adjustedHR = 44;

                    double HR_PAEE = 0.011 * adjustedHR * adjustedHR + 5.82 * adjustedHR;
                    double Acc_PAEE = (cpm > 497.0) ? (0.053 * cpm + 47.88) : (0.15 * cpm);

                    double PAEE;
                    if (cpm > 5.0) {
                        PAEE = (hr > 100.0) ? HR_PAEE : 0.5 * HR_PAEE + 0.5 * Acc_PAEE;
                    } else {
                        PAEE = (hr > 70.0) ? 0.5 * HR_PAEE + 0.5 * Acc_PAEE : Acc_PAEE;
                    }

                    // Convert PAEE (kJ/min) to METs using same formula
                    double met = (PAEE / 3.5) * (200.0 / 4184.0);
                    metList.add(met);
                }

                if (remainingSamples > 0) {
                    int startIdx = filteredX.length - remainingSamples;
                    int endIdx = filteredX.length;

                    double hr = getHRperMinute(startTime, fullMinutes);
                    double cpm = computeCPMFromRawDataActiGraph(filteredX, filteredY, filteredZ, startIdx, endIdx);

                    double adjustedHR = Math.max(0, hr - 44.0);
                    if (adjustedHR == 0)
                        adjustedHR = 44;

                    double HR_PAEE = 0.011 * adjustedHR * adjustedHR + 5.82 * adjustedHR;
                    double Acc_PAEE = (cpm > 497.0) ? (0.053 * cpm + 47.88) : (0.15 * cpm);

                    double PAEE;
                    if (cpm > 5.0) {
                        PAEE = (hr > 100.0) ? HR_PAEE : 0.5 * HR_PAEE + 0.5 * Acc_PAEE;
                    } else {
                        PAEE = (hr > 70.0) ? 0.5 * HR_PAEE + 0.5 * Acc_PAEE : Acc_PAEE;
                    }

                    // Final PAEE to METs conversion for remainder
                    double met = (PAEE / 3.5) * (200.0 / 4184.0);
                    metList.add(met);
                }
            }

            default -> throw new IllegalArgumentException("Unknown estimation method.");
        }

        return metList;
    }

    private double getHRperMinute(long startTime, int minute) {
        AmsLabel lbl = new AmsLabel(startTime + 600000000L * minute, startTime + 600000000L * (minute + 1), false, 0.0,
                "");
        double HR = lbl.getAverage(true);
        return HR;
    }

    /**
     * Calculate the mean absolute deviation (MAD) for three axes (x, y, z)
     * 
     * @param N
     * @return mean absolute deviation (MAD) for the three axes
     *         See: Performance of Different Accelerometry-Based Metrics to Estimate
     *         Oxygen Consumption during Track and Treadmill Locomotion over a Wide
     *         Intensity Range
     */
    public List<Double> calculateMADxyz() {
        if (filteredX.length < samplesPerMinute) {
            throw new IllegalArgumentException(
                    "Data length is less than one minute. At least 60000 samples are required.");
        }
        List<Double> MADxyzList = new ArrayList<>();

        for (int m = 0; m < fullMinutes; m++) {
            int N = samplesPerMinute;

            int startIdx = m * samplesPerMinute;
            int endIdx = startIdx + samplesPerMinute;

            // Extract the relevant portion of the data
            double[] filteredXpart = java.util.Arrays.copyOfRange(filteredX, startIdx, endIdx);
            double[] filteredYpart = java.util.Arrays.copyOfRange(filteredY, startIdx, endIdx);
            double[] filteredZpart = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            // Calculate the mean of each axis over N terms
            double Xave = calculateMean(filteredXpart, N);
            double Yave = calculateMean(filteredYpart, N);
            double Zave = calculateMean(filteredZpart, N);

            // Calculate the absolute deviations from the mean for each axis
            double[] deviationsX = calculateDeviations(filteredXpart, Xave, N);
            double[] deviationsY = calculateDeviations(filteredYpart, Yave, N);
            double[] deviationsZ = calculateDeviations(filteredZpart, Zave, N);

            // Calculate the MAD for each axis
            double madX = calculateMAD(deviationsX);
            double madY = calculateMAD(deviationsY);
            double madZ = calculateMAD(deviationsZ);

            // Return the sqrt of MAD values for each axis
            MADxyzList.add(Math.sqrt(madX * madX + madY * madY + madZ * madZ));

        }

        if (remainingSamples > 0) {
            int startIdx = filteredX.length - remainingSamples;
            int endIdx = filteredX.length;
            int N = endIdx - startIdx;

            // Extract the relevant portion of the data
            double[] filteredXpart = java.util.Arrays.copyOfRange(filteredX, startIdx, endIdx);
            double[] filteredYpart = java.util.Arrays.copyOfRange(filteredY, startIdx, endIdx);
            double[] filteredZpart = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            // Calculate the mean of each axis over N terms
            double Xave = calculateMean(filteredXpart, N);
            double Yave = calculateMean(filteredYpart, N);
            double Zave = calculateMean(filteredZpart, N);

            // Calculate the absolute deviations from the mean for each axis
            double[] deviationsX = calculateDeviations(filteredXpart, Xave, N);
            double[] deviationsY = calculateDeviations(filteredYpart, Yave, N);
            double[] deviationsZ = calculateDeviations(filteredZpart, Zave, N);

            // Calculate the MAD for each axis
            double madX = calculateMAD(deviationsX);
            double madY = calculateMAD(deviationsY);
            double madZ = calculateMAD(deviationsZ);

            // Return the sqrt of MAD values for each axis
            MADxyzList.add(Math.sqrt(madX * madX + madY * madY + madZ * madZ));
        }

        // Multiply the MAD values by 1000 to convert to milli g's
        for (int i = 0; i < MADxyzList.size(); i++) {
            MADxyzList.set(i, 1000 * MADxyzList.get(i));
        }
        return MADxyzList;
    }

    /**
     * Calculate the mean absolute deviation (MAD) for the resultant vector
     * derived from three axes (x, y, z).
     * 
     * @param N
     * @return mean absolute deviation (MAD) for the resultant vector
     *         See: Performance of Different Accelerometry-Based Metrics to Estimate
     *         Oxygen Consumption during Track and Treadmill Locomotion over a Wide
     *         Intensity Range
     */
    public List<Double> calculateMAD() {
        if (filteredX.length < samplesPerMinute) {
            throw new IllegalArgumentException(
                    "Data length is less than one minute. At least 60000 samples are required.");
        }
        List<Double> MADList = new ArrayList<>();

        for (int m = 0; m < fullMinutes; m++) {
            int N = samplesPerMinute;
            int startIdx = m * samplesPerMinute;
            int endIdx = startIdx + samplesPerMinute;

            // Extract the relevant portion of the data
            double[] filteredXpart = java.util.Arrays.copyOfRange(filteredX, startIdx, endIdx);
            double[] filteredYpart = java.util.Arrays.copyOfRange(filteredY, startIdx, endIdx);
            double[] filteredZpart = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            // get the resultant mean of the combined axes
            double res[] = new double[N];
            for (int i = 0; i < N; i++) {
                res[i] = Math
                        .sqrt(filteredXpart[i] * filteredXpart[i] + filteredYpart[i] * filteredYpart[i]
                                + filteredZpart[i] * filteredZpart[i]);
            }

            // Calculate the mean of the resultant vector
            double Rave = calculateMean(res, N);

            // Calculate the absolute deviations from the mean for each axis
            double[] deviations = calculateDeviations(res, Rave, N);

            // Calculate the MAD overall
            MADList.add(calculateMAD(deviations));
        }

        if (remainingSamples > 0) {
            int startIdx = filteredX.length - remainingSamples;
            int endIdx = filteredX.length;
            int N = endIdx - startIdx;

            // Extract the relevant portion of the data
            double[] filteredXpart = java.util.Arrays.copyOfRange(filteredX, startIdx, endIdx);
            double[] filteredYpart = java.util.Arrays.copyOfRange(filteredY, startIdx, endIdx);
            double[] filteredZpart = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            // get the resultant mean of the combined axes
            double res[] = new double[N];
            for (int i = 0; i < N; i++) {
                res[i] = Math
                        .sqrt(filteredXpart[i] * filteredXpart[i] + filteredYpart[i] * filteredYpart[i]
                                + filteredZpart[i] * filteredZpart[i]);
            }

            // Calculate the mean of the resultant vector
            double Rave = calculateMean(res, N);

            // Calculate the absolute deviations from the mean for each axis
            double[] deviations = calculateDeviations(res, Rave, N);

            // Calculate the MAD overall
            MADList.add(calculateMAD(deviations));
        }

        // Multiply the MAD values by 1000 to convert to milli g's
        for (int i = 0; i < MADList.size(); i++) {
            MADList.set(i, 1000 * MADList.get(i));
        }

        // Return the MAD
        return MADList;
    }

    private static double calculateMean(double[] values, int N) {
        double sum = 0;
        for (int i = 0; i < N; i++) {
            sum += values[i];
        }
        return sum / N;
    }

    private static double[] calculateDeviations(double[] values, double mean, int N) {
        double[] deviations = new double[N];
        for (int i = 0; i < N; i++) {
            deviations[i] = Math.abs(values[i] - mean);
        }
        return deviations;
    }

    private static double calculateMAD(double[] deviations) {
        double sum = 0;
        for (double deviation : deviations) {
            sum += deviation;
        }
        return sum / deviations.length;
    }

    /**
     * Detects speech activity in the input z-axis accelerometer signal.
     *
     * This method processes the raw z-axis data to identify minutes containing
     * speech,
     * based on energy computed from short-time windows. It uses adaptive
     * thresholding
     * and robust statistics to distinguish speech from background activity.
     *
     * @param zData Raw z-axis accelerometer data (double[]), sampled at 1000 Hz.
     * @return List<Double> where each entry is 1.0 if speech is detected in that
     *         minute, 0.0 otherwise.
     */
    public List<Double> calculateSpeech(double[] zData) {
        // Output: speechList contains 1.0 (speech detected) or 0.0 (no speech) per
        // minute (plus final partial minute if any)
        List<Double> speechList = new ArrayList<>();

        // Ensure there is at least one minute of data
        if (zData.length < samplesPerMinute) {
            throw new IllegalArgumentException(
                    "Data length is less than one minute. At least 60000 samples are required.");
        }

        // --- Speech detection constants ---
        // Butterworth filter order for bandpass (chosen for steep rolloff, but low
        // enough to avoid ringing)
        final int FILTER_ORDER = 2;
        // Frequency range for human speech (10-300 Hz): removes DC/movement and
        // high-frequency noise
        final int LOWER_CUTOFF_FREQUENCY = 10; // Hz
        final int UPPER_CUTOFF_FREQUENCY = 300; // Hz

        // Short-time window size for energy calculation (250 ms at 1000 Hz = 250
        // samples)
        final int WINDOWSIZE_SPEECH_ANALYSIS = 250;
        // Step size for sliding window (125 ms, 50% overlap for sensitivity)
        final int STEP_SIZE = 125;
        // Offset added to adaptive threshold to provide a buffer above noise floor
        final double OFFSET = 0.0003;
        // Percentage of windows required to exceed threshold for "speech" detection
        final int PERCENTAGE_THRESHOLD = 15;

        // This list accumulates all window energies across all minutes for robust
        // global statistics
        List<Double> globalEnergyList = new ArrayList<>();

        // --- Filtering step ---
        // Apply zero-phase Butterworth bandpass filter to z-axis data to isolate
        // speech-relevant frequencies.
        // Filtering is performed once for efficiency.
        double[] filteredZ = zeroPhaseFilter(zData, FILTER_ORDER, LOWER_CUTOFF_FREQUENCY,
                UPPER_CUTOFF_FREQUENCY, SAMPLING_FREQUENCY);

        // --- Per-minute analysis loop ---
        for (int m = 0; m < fullMinutes; m++) {
            int startIdx = m * samplesPerMinute;
            int endIdx = startIdx + samplesPerMinute;

            // Extract filtered samples for this minute
            double[] ZminuteFiltered = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            // Precompute squared samples for efficient RMS (energy) computation in windows
            List<Double> energyList = new ArrayList<>();
            double[] z2 = new double[ZminuteFiltered.length];
            for (int j = 0; j < ZminuteFiltered.length; j++) {
                z2[j] = ZminuteFiltered[j] * ZminuteFiltered[j];
            }

            // --- Sliding window energy calculation ---
            // For each window (with 50% overlap), compute RMS as a proxy for speech energy.
            for (int i = 0; i + WINDOWSIZE_SPEECH_ANALYSIS <= ZminuteFiltered.length; i += STEP_SIZE) {
                // Apache Commons Math StatUtils.mean is used for robust, efficient mean
                // computation.
                double meanSq = StatUtils.mean(z2, i, WINDOWSIZE_SPEECH_ANALYSIS);
                // FastMath.sqrt for numerical stability and speed.
                double energy = FastMath.sqrt(meanSq);
                energyList.add(energy);
                globalEnergyList.add(energy); // For global thresholding
            }

            // If no windows were processed (shouldn't occur), mark as no speech.
            if (energyList.isEmpty()) {
                speechList.add(0.0);
                continue;
            }

            // --- Adaptive thresholding ---
            // Use the median (50th percentile) of local and global energy as a robust
            // baseline.
            // Apache Commons Math Percentile is used for accurate, outlier-resistant
            // statistics.
            double localMedian = new Percentile(50.0).evaluate(toPrimitiveArray(energyList));
            double globalMedian = new Percentile(50.0).evaluate(toPrimitiveArray(globalEnergyList));
            // The adaptive threshold is the mean of local/global medians plus a small
            // offset.
            // This helps handle both local minute-specific and overall session noise/energy
            // levels.
            double adaptiveThreshold = 0.5 * localMedian + 0.5 * globalMedian + OFFSET;

            // --- Speech detection logic ---
            // Count windows with energy above the adaptive threshold (i.e., likely speech
            // activity)
            int count = 0;
            for (double energy : energyList) {
                if (energy > adaptiveThreshold)
                    count++;
            }

            // Compute the percentage of windows exceeding threshold in this minute.
            double percentage = 100.0 * count / energyList.size();
            // Optionally print debug info:
            System.out.println("Speech percentage for minute " + m + ": " + percentage);
            // If percentage exceeds the minimum, mark this minute as containing speech.
            speechList.add(percentage > PERCENTAGE_THRESHOLD ? 1.0 : 0.0);
        }

        // --- Handle remaining samples at the end ---
        // If leftover samples are enough for at least one window, process them as a
        // final partial minute.
        if (remainingSamples >= WINDOWSIZE_SPEECH_ANALYSIS) {
            int startIdx = filteredZ.length - remainingSamples;
            int endIdx = Math.min(filteredZ.length, startIdx + remainingSamples);
            double[] Zpart = java.util.Arrays.copyOfRange(filteredZ, startIdx, endIdx);

            List<Double> energyList = new ArrayList<>();
            double[] z2part = new double[Zpart.length];
            for (int j = 0; j < Zpart.length; j++) {
                z2part[j] = Zpart[j] * Zpart[j];
            }
            for (int i = 0; i + WINDOWSIZE_SPEECH_ANALYSIS <= Zpart.length; i += STEP_SIZE) {
                double meanSq = StatUtils.mean(z2part, i, WINDOWSIZE_SPEECH_ANALYSIS);
                double energy = FastMath.sqrt(meanSq);
                energyList.add(energy);
                globalEnergyList.add(energy);
            }

            if (energyList.isEmpty()) {
                // Not enough windows: mark as no speech
                speechList.add(0.0);
            } else {
                // Same adaptive thresholding as above
                double localMedian = new Percentile(50.0).evaluate(toPrimitiveArray(energyList));
                double globalMedian = new Percentile(50.0).evaluate(toPrimitiveArray(globalEnergyList));
                double adaptiveThreshold = 0.5 * localMedian + 0.5 * globalMedian + OFFSET;

                int count = 0;
                for (double energy : energyList) {
                    if (energy > adaptiveThreshold)
                        count++;
                }

                double percentage = 100.0 * count / energyList.size();
                speechList.add(percentage > PERCENTAGE_THRESHOLD ? 1.0 : 0.0);
            }

        } else if (remainingSamples > 0 && remainingSamples < WINDOWSIZE_SPEECH_ANALYSIS) {
            // If there are leftover samples but not enough for one window, mark as no
            // speech.
            speechList.add(0.0); // Not enough samples to process
        }

        // Return list of speech activity flags (1.0 = speech, 0.0 = no speech) per
        // minute
        return speechList;
    }

    /**
     * Helper method to save multiple arrays to a file.
     * 
     * @param data      variable number of arrays to save
     * @param filename
     * @param overwrite whether to overwrite or append to file
     */
    @SuppressWarnings("unused")
    private void saveDataToFile(Object[][] data, String filename, boolean overwrite) {
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
     * Helper to convert List<Double> to primitive double[] for Apache Commons Math.
     */
    private static double[] toPrimitiveArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
