package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for motion data processing.
 */
public class MotionDataUtils {

    /**
     * Convert raw accelerometer counts to physical acceleration in m/s^2 using the
     * per-channel calibration (real slope/constant). The device units are first
     * converted to g, then multiplied by standard gravity.
     * This mirrors the earlier working path in analyseMotility where values were
     * computed as raw * realSlope * 9.8.
     *
     * NOTE: Call this once per chunk and reuse the resulting arrays so that
     * multiple algorithms (steps, posture, etc.) operate on identical inputs.
     * 
     * @param raw  Raw accelerometer counts
     * @param chan Channel info containing calibration data
     * @return Acceleration in m/s^2
     */
    public static double[] toMs2(int[] raw, Ams7fsChannelInfo chan) {
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

    /**
     * Save a numeric array (e.g., altitude, velocity, etc.) to a text file along
     * with a time column. The time starts from 0 and increments by 1/Fs seconds
     * for each sample.
     *
     * @param data     Array of double values to save
     * @param Fs       Sampling frequency in Hz (used to compute time)
     * @param filePath Absolute path of the output text file
     * @param append   If true, appends to existing file; otherwise overwrites
     * @throws IOException if file cannot be written
     */
    public static void saveToTextFileWithTime(double[] data, double Fs, String filePath, boolean append)
            throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Input array is empty or null");
        }

        File file = new File(filePath);

        // Create parent directories if they don't exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
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

        System.out.println("Saved data to: " + file.getAbsolutePath());
    }
}
