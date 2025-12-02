package nl.vu.psy.ams.suite.data.posture;

import org.apache.commons.math3.util.FastMath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Helper class for reading 3-axis accelerometer data from binary files.
 * Supports both direct reading (entire file) and chunk-based reading.
 */
public class AccelerometerDataReader {

    /**
     * Result containing 3-axis accelerometer data as integer arrays.
     */
    public static class AccelData {
        public final int[] x;
        public final int[] y;
        public final int[] z;
        public final int numSamples;

        public AccelData(int[] x, int[] y, int[] z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.numSamples = x.length;
        }
    }

    /**
     * Read entire accelerometer files as raw integer arrays.
     * This is used for direct reading (non-chunked) approach.
     * 
     * @param fileX X-axis accelerometer file (e.g., FILTMXR.bin)
     * @param fileY Y-axis accelerometer file (e.g., FILTMYR.bin)
     * @param fileZ Z-axis accelerometer file (e.g., FILTMZR.bin)
     * @return AccelData containing all samples as integer arrays
     * @throws IOException if file reading fails
     */
    public static AccelData readAllAsIntegers(File fileX, File fileY, File fileZ) throws IOException {
        int numSamples = (int) (fileX.length() / 4); // 4 bytes per int

        int[] sampX, sampY, sampZ;

        try (FileInputStream fisX = new FileInputStream(fileX);
                FileChannel chX = fisX.getChannel();
                FileInputStream fisY = new FileInputStream(fileY);
                FileChannel chY = fisY.getChannel();
                FileInputStream fisZ = new FileInputStream(fileZ);
                FileChannel chZ = fisZ.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(numSamples * 4);

            // Read X-axis
            chX.read(buffer);
            buffer.flip();
            sampX = new int[numSamples];
            buffer.asIntBuffer().get(sampX);

            // Read Y-axis
            buffer.clear();
            chY.read(buffer);
            buffer.flip();
            sampY = new int[numSamples];
            buffer.asIntBuffer().get(sampY);

            // Read Z-axis
            buffer.clear();
            chZ.read(buffer);
            buffer.flip();
            sampZ = new int[numSamples];
            buffer.asIntBuffer().get(sampZ);
        }

        return new AccelData(sampX, sampY, sampZ);
    }

    /**
     * Read one chunk of accelerometer data from open file channels.
     * This is used for chunk-based reading approach.
     * 
     * @param channelX   Open FileChannel for X-axis data
     * @param channelY   Open FileChannel for Y-axis data
     * @param channelZ   Open FileChannel for Z-axis data
     * @param buffer     ByteBuffer to use for reading (will be reused)
     * @param maxSamples Maximum number of samples to read
     * @return AccelData containing chunk samples, or null if end of file
     * @throws IOException if file reading fails
     */
    public static AccelData readChunkAsIntegers(FileChannel channelX, FileChannel channelY, FileChannel channelZ,
            ByteBuffer buffer, int maxSamples) throws IOException {
        buffer.clear();
        buffer.limit(maxSamples * 4);

        // Read X-axis chunk
        int nRead = channelX.read(buffer);
        if (nRead < 1) {
            return null; // End of file
        }
        int nSamples = nRead / 4;
        buffer.flip();
        int[] sampX = new int[nSamples];
        buffer.asIntBuffer().get(sampX);

        // Read Y-axis chunk
        buffer.clear();
        buffer.limit(maxSamples * 4);
        channelY.read(buffer);
        buffer.flip();
        int[] sampY = new int[nSamples];
        buffer.asIntBuffer().get(sampY);

        // Read Z-axis chunk
        buffer.clear();
        buffer.limit(maxSamples * 4);
        channelZ.read(buffer);
        buffer.flip();
        int[] sampZ = new int[nSamples];
        buffer.asIntBuffer().get(sampZ);

        return new AccelData(sampX, sampY, sampZ);
    }

    /**
     * Calculate magnitude (Euclidean norm) of 3D acceleration vector.
     * magnitude = sqrt(x² + y² + z²)
     * 
     * @param accelData 3-axis accelerometer data
     * @return Array of magnitude values
     */
    public static int[] calculateMagnitude(AccelData accelData) {
        int[] magnitude = new int[accelData.numSamples];
        for (int i = 0; i < accelData.numSamples; i++) {
            int x = accelData.x[i];
            int y = accelData.y[i];
            int z = accelData.z[i];
            magnitude[i] = (int) FastMath.sqrt(x * x + y * y + z * z);
        }
        return magnitude;
    }
}
