package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

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
     * Convert raw accelerometer counts to m/s^2 using channel calibration lookup.
     * Convenience overload that looks up channel info by ID.
     * 
     * @param raw       Raw accelerometer counts
     * @param channelId Channel ID for calibration lookup (e.g., "MXR", "MYR",
     *                  "MZR")
     * @return Acceleration in m/s^2, or null if channel lookup fails
     */
    public static double[] toMs2(int[] raw, String channelId) {
        try {
            Ams7fsChannelInfo channelInfo = nl.vu.psy.ams.suite.data.CurrentOpenData.getInstance()
                    .getChannelInfoFromID(channelId);
            return toMs2(raw, channelInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double[] getAltitudeFromPressure(double[] pressure) {
        double[] altitude = new double[pressure.length];
        for (int j = 0; j < pressure.length; j++) {
            altitude[j] = (pressure[j] - 102000.0) / -12.2;
        }
        return altitude;
    }

    /**
     * Generic helper to filter time-based data within a specified time range.
     * Uses midpoint-based filtering to include only intervals where at least half
     * falls within the range.
     * 
     * @param <T>        Type of data in the list
     * @param data       List of time-based data (one element per interval)
     * @param leftTime   Start time in microseconds
     * @param rightTime  End time in microseconds
     * @param intervalMs Interval duration in milliseconds (e.g., 60000 for 1
     *                   minute)
     * @return List of data elements whose midpoints fall within the time range
     */
    public static <T> List<T> getDataInTimeRange(List<T> data, double leftTime, double rightTime, int intervalMs) {
        // Convert to milliseconds and adjust for recording start time
        double rightTimeMs = (rightTime / 1000) - nl.vu.psy.ams.suite.data.CurrentOpenData.getInstance()
                .getStarts().get(0).getDwClockTick_ms();
        double leftTimeMs = (leftTime / 1000) - nl.vu.psy.ams.suite.data.CurrentOpenData.getInstance()
                .getStarts().get(0).getDwClockTick_ms();

        List<T> result = new ArrayList<>();
        int halfInterval = intervalMs / 2;

        for (int i = 0; i < data.size(); i++) {
            // Calculate midpoint of this interval
            int midpoint = i * intervalMs + halfInterval;

            // Skip intervals before the time range
            if (midpoint < leftTimeMs)
                continue;

            // Stop when past the time range
            if (midpoint > rightTimeMs)
                break;

            result.add(data.get(i));
        }

        return result;
    }

    /**
     * Write a List of numeric values to a FileChannel as binary data.
     * Supports Double, Integer, Float, and Long.
     * 
     * @param data    List of numeric values to write
     * @param channel FileChannel to write to
     * @throws IOException if write operation fails
     */
    public static void writeToBinaryChannel(List<? extends Number> data, FileChannel channel) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }

        // Determine type from first element and write accordingly
        Number first = data.get(0);
        switch (first) {
            case Double d -> {
                ByteBuffer buffer = ByteBuffer.allocate(data.size() * 8);
                DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
                for (Number value : data) {
                    doubleBuffer.put(value.doubleValue());
                }
                buffer.limit(data.size() * 8);
                channel.write(buffer);
            }
            case Integer i -> {
                ByteBuffer buffer = ByteBuffer.allocate(data.size() * 4);
                IntBuffer intBuffer = buffer.asIntBuffer();
                for (Number value : data) {
                    intBuffer.put(value.intValue());
                }
                buffer.limit(data.size() * 4);
                channel.write(buffer);
            }
            case Float f -> {
                ByteBuffer buffer = ByteBuffer.allocate(data.size() * 4);
                FloatBuffer floatBuffer = buffer.asFloatBuffer();
                for (Number value : data) {
                    floatBuffer.put(value.floatValue());
                }
                buffer.limit(data.size() * 4);
                channel.write(buffer);
            }
            case Long l -> {
                ByteBuffer buffer = ByteBuffer.allocate(data.size() * 8);
                LongBuffer longBuffer = buffer.asLongBuffer();
                for (Number value : data) {
                    longBuffer.put(value.longValue());
                }
                buffer.limit(data.size() * 8);
                channel.write(buffer);
            }
            default -> throw new IllegalArgumentException("Unsupported data type: " + first.getClass().getName());
        }
    }

    /**
     * Write an array of doubles to a FileChannel as binary data.
     * 
     * @param data    Array of double values to write
     * @param channel FileChannel to write to
     * @throws IOException if write operation fails
     */
    public static void writeToBinaryChannel(double[] data, FileChannel channel) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 8);
        DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
        doubleBuffer.put(data);
        buffer.limit(data.length * 8);
        channel.write(buffer);
    }

    /**
     * Write an array of integers to a FileChannel as binary data.
     * 
     * @param data    Array of int values to write
     * @param channel FileChannel to write to
     * @throws IOException if write operation fails
     */
    public static void writeToBinaryChannel(int[] data, FileChannel channel) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(data);
        buffer.limit(data.length * 4);
        channel.write(buffer);
    }

    /**
     * Write an array of floats to a FileChannel as binary data.
     * 
     * @param data    Array of float values to write
     * @param channel FileChannel to write to
     * @throws IOException if write operation fails
     */
    public static void writeToBinaryChannel(float[] data, FileChannel channel) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(data);
        buffer.limit(data.length * 4);
        channel.write(buffer);
    }

    /**
     * Write an array of longs to a FileChannel as binary data.
     * 
     * @param data    Array of long values to write
     * @param channel FileChannel to write to
     * @throws IOException if write operation fails
     */
    public static void writeToBinaryChannel(long[] data, FileChannel channel) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 8);
        LongBuffer longBuffer = buffer.asLongBuffer();
        longBuffer.put(data);
        buffer.limit(data.length * 8);
        channel.write(buffer);
    }

    /**
     * Save multiple columns of data to a tab-separated text file.
     * Supports double[], int[], String[], and Object[] arrays.
     * Optionally adds a time column as the first column.
     *
     * @param filePath  Absolute path of the output text file
     * @param overwrite If true, overwrites existing file; otherwise appends
     * @param addTime   If true, adds a time column as the first column
     * @param fs        Sampling frequency in Hz (used for time column if addTime is
     *                  true)
     * @param columns   Variable number of column arrays (can be double[], int[],
     *                  String[], or Object[])
     * @throws IOException if file cannot be written
     */
    public static void saveColumnsToFile(String filePath, boolean overwrite, boolean addTime, double fs,
            Object... columns) throws IOException {

        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("At least one column must be provided");
        }

        File file = new File(filePath);

        // Create parent directories if they don't exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Determine the maximum number of rows
        int numRows = 0;
        for (Object col : columns) {
            if (col != null) {
                int length = getArrayLength(col);
                if (length > numRows) {
                    numRows = length;
                }
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, !overwrite))) {
            // Write header comment if not appending
            if (!overwrite) {
                StringBuilder header = new StringBuilder("# ");
                if (addTime) {
                    header.append("time_s\t");
                }
                for (int i = 0; i < columns.length; i++) {
                    header.append("col").append(i + 1);
                    if (i < columns.length - 1) {
                        header.append("\t");
                    }
                }
                writer.println(header);
                if (addTime) {
                    writer.println("# Fs = " + fs + " Hz");
                }
            }

            // Write data rows
            for (int r = 0; r < numRows; r++) {
                StringBuilder line = new StringBuilder();

                // Add time column if requested
                if (addTime) {
                    double t = r / fs;
                    line.append(t).append("\t");
                }

                // Add data columns
                for (int c = 0; c < columns.length; c++) {
                    if (columns[c] != null && r < getArrayLength(columns[c])) {
                        Object value = getArrayValue(columns[c], r);
                        line.append(value != null ? value.toString() : "");
                    }
                    if (c < columns.length - 1) {
                        line.append("\t");
                    }
                }
                writer.println(line);
            }
        }

        System.out.println("Saved data to: " + file.getAbsolutePath());
    }

    /**
     * Get the length of an array (supports double[], int[], String[], Object[]).
     */
    private static int getArrayLength(Object array) {
        if (array instanceof double[]) {
            return ((double[]) array).length;
        } else if (array instanceof int[]) {
            return ((int[]) array).length;
        } else if (array instanceof String[]) {
            return ((String[]) array).length;
        } else if (array instanceof Object[]) {
            return ((Object[]) array).length;
        }
        return 0;
    }

    /**
     * Get a value from an array at the specified index (supports double[], int[],
     * String[], Object[]).
     */
    private static Object getArrayValue(Object array, int index) {
        if (array instanceof double[]) {
            return ((double[]) array)[index];
        } else if (array instanceof int[]) {
            return ((int[]) array)[index];
        } else if (array instanceof String[]) {
            return ((String[]) array)[index];
        } else if (array instanceof Object[]) {
            return ((Object[]) array)[index];
        }
        return null;
    }

    /**
     * Convenience method: Save a single column with timestamps.
     * This is a wrapper for backward compatibility with saveToTextFileWithTime.
     *
     * @param data     Array of double values to save
     * @param fs       Sampling frequency in Hz (used to compute time)
     * @param filePath Absolute path of the output text file
     * @param append   If true, appends to existing file; otherwise overwrites
     * @throws IOException if file cannot be written
     */
    public static void saveToTextFileWithTime(double[] data, double fs, String filePath, boolean append)
            throws IOException {
        saveColumnsToFile(filePath, !append, true, fs, data);
    }

    /**
     * Convenience method: Save multiple columns without timestamps.
     *
     * @param filePath  Absolute path of the output text file
     * @param overwrite If true, overwrites existing file; otherwise appends
     * @param columns   Variable number of column arrays
     * @throws IOException if file cannot be written
     */
    public static void saveColumns(String filePath, boolean overwrite, Object... columns) throws IOException {
        saveColumnsToFile(filePath, overwrite, false, 1.0, columns);
    }
}
