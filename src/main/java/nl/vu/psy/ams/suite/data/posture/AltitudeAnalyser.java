package nl.vu.psy.ams.suite.data.posture;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Analyzes altitude changes from barometric pressure data to detect stair
 * climbing.
 * Uses barometer samples and step detection data to classify movement as stairs
 * up,
 * stairs down, or level ground.
 * 
 * Singleton class for analyzing altitude changes.
 */
public class AltitudeAnalyser {

    private static AltitudeAnalyser instance;

    /**
     * Get the singleton instance of AltitudeAnalyser.
     * 
     * @return the singleton instance
     */
    public static AltitudeAnalyser getInstance() {
        if (instance == null) {
            instance = new AltitudeAnalyser();
        }
        return instance;
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private AltitudeAnalyser() {
        // Private constructor
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
    public AltitudeAnalysisResult analyseAltitudeChange(double[] pressure, int[] stepLocations, int windowSec) {
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
}
