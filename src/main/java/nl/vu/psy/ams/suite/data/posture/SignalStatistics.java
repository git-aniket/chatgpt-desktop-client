package nl.vu.psy.ams.suite.data.posture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignalStatistics {

    enum Axis {X, Y, Z}

    private static final Logger logger = LogManager.getLogger(SignalStatistics.class);

    private final double mean;
    private final double variance;
    private final double stddev;
    private final double min;
    private final double max;
    private final double skewness;
    private final double kurtosis;
    private final double zeroCrossingRate;

    public SignalStatistics(double[] signal, int startIndex, int length) {
        if (signal == null || signal.length < startIndex + length || length < 2 ) {
            String message = "Illegal constructor parameters";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        double sum = 0.0;
        double sumSq = 0.0;
        double minVal = Double.POSITIVE_INFINITY;
        double maxVal = Double.NEGATIVE_INFINITY;

        // --- First pass: mean, min, max ---
        for (int i = startIndex; i < startIndex + length; i++) {
            double x = signal[i];
            sum += x;
            sumSq += x * x;
            if (x < minVal) minVal = x;
            if (x > maxVal) maxVal = x;
        }
        mean = sum / length;
        variance = ((sumSq / length) - (mean * mean)) * length / (length - 1);
        stddev = Math.sqrt(variance);
        min = minVal;
        max = maxVal;

        // --- Second pass: skewness, kurtosis ---
        double m2 = 0.0, m3 = 0.0, m4 = 0.0;
        for (int i = startIndex; i < startIndex + length; i++) {
            double x = signal[i];
            double d = x - mean;
            double d2 = d * d;
            m2 += d2;
            m3 += d2 * d;
            m4 += d2 * d2;
        }
        m2 /= length;
        m3 /= length;
        m4 /= length;

        skewness = (m3 / Math.pow(m2, 1.5));
        kurtosis = (m4 / (m2 * m2)) - 3.0; // population excess kurtosis

        // --- Zero Crossing Rate ---
        int zeroCrossings = 0;
        for (int i = startIndex + 1; i < startIndex + length; i++) {
            if ((signal[i - 1] >= mean && signal[i] < mean) || (signal[i - 1] < mean && signal[i] >= mean)) {
                zeroCrossings++;
            }
        }

        zeroCrossingRate = (double) zeroCrossings / length;
    }

    // --- Getters ---
    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return variance;
    }

    public double getStandardDeviation() {
        return stddev;
    }

    public double getMinimum() {
        return min;
    }

    public double getMaximum() {
        return max;
    }

    public double getSkewness() {
        return skewness;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public double getZeroCrossingRate() {
        return zeroCrossingRate;
    }

    public double getNormalizedMaximum() {
        return max - mean;
    }

    public double getNormalizedMinimum() {
        return min - mean;
    }

    public double getRange() {
        return max - min;
    }

    static double correlation(double[] a, double[] b, double meanA, double meanB, int startIndex, int length) {
        if (a == null || b == null || a.length != b.length || a.length < startIndex + length) {
            String message = "In correlation: Illegal parameters";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        double num = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;
        for (int i = startIndex; i < startIndex + length; i++) {
            double dx = a[i] - meanA;
            double dy = b[i] - meanB;
            num += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }

        double denominator = Math.sqrt(denomX * denomY);
        if (denominator == 0.0) {
            logger.warn("In correlation: Zero denominator -- returning 0");
            return 0.0;
        }

        return num / denominator;
    }

    static double angleWithAxis(Axis a, double x, double y, double z) {
        double norm = Math.sqrt(x * x + y * y + z * z);

        if (norm == 0.0) {
            logger.warn("In angleWithAxis: Undefined direction -- returning 0");
            return 0.0;
        }

        double ratio = switch (a) {
            case X -> x / norm;
            case Y -> y / norm;
            case Z -> z / norm;
        };

        // Clamp ratio to [-1, 1] to avoid NaN from floating-point rounding errors
        ratio = Math.max(-1.0, Math.min(1.0, ratio));

        return Math.toDegrees(Math.acos(ratio));
    }
}