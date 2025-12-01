package nl.vu.psy.ams.suite.data.posture;

import com.github.psambit9791.jdsp.signal.Decimate;

import java.util.Arrays;


@SuppressWarnings("unused")
public class ZeroPhaseFilter {

    public static void lowPass(double[] signal, double fs, int order, double cutoff) {
        ButterworthLowPass filter = new ButterworthLowPass(order, cutoff, fs);
        filter(signal, filter);
    }

    public static void highPass(double[] signal, double fs, int order, double cutoff) {
        ButterworthHighPass filter = new ButterworthHighPass(order, cutoff, fs);
        filter(signal, filter);
    }

    private static void filter(double[] signal, ButterworthFilter filter) {
        // Check params
        if (signal == null || signal.length < 2) {
            return;
        }

        // Store right pad for reverse filtering
        filter.storeRightPad(signal);

        // Filter
        filter.warmUpWithPaddingLeft(signal);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = filter.filter(signal[i]);
        }

        // Reverse filter
        filter.warmUpWithPaddingRight();
        for (int i = signal.length; i > 0; i--) {
            signal[i - 1] = filter.filter(signal[i - 1]);
        }
    }

    public static double[] decimateJDSP(double[] signal, double fs, int downSamplingFactor) {
        var decimate = new Decimate(signal, (int) Math.round(fs), true);
        return decimate.decimate(downSamplingFactor);
    }

    public static double[] decimate(double[] signal, double fs, int downSamplingFactor) {
        var output = new double[signal.length / downSamplingFactor];
        int index = 0;
        for (int i = 0; i < output.length; i++) {
            output[i] = signal[index];
            index += downSamplingFactor;
        }
        return output;
    }

    private static abstract class ButterworthFilter {
        protected final int order;
        protected final double cutoffFreq;
        protected final double samplingRate;
        protected final double[] a; // denominator coefficients
        protected final double[] b; // numerator coefficients
        protected final double[] z; // filter state
        protected final double[] rightPad;
        protected int padLength;

        abstract void calculateCoefficients();

        ButterworthFilter(int order, double cutoffFreq, double samplingRate) {
            this.order = order;
            this.cutoffFreq = cutoffFreq;
            this.samplingRate = samplingRate;

            this.a = new double[order + 1];
            this.b = new double[order + 1];
            this.z = new double[order]; // state array (for Direct Form II)

            this.padLength = 3 * Math.max(a.length, b.length);
            this.rightPad = new double[this.padLength];

            calculateCoefficients();
        }

        // Optional: Set initial bias/state
        void setBias(double bias) {
            Arrays.fill(z, bias);
        }

        void warmUp(double x) {
            for (int i = 0; i < order * 100; i++) {
                filter(x);
            }
        }

        void warmUpWithPaddingLeft(double[] x) {
            reducePadLengthIfSignalIsTooShort(x);

            initForConstantInput(2.0 * x[0] - x[padLength]);
            for (int i = padLength; i > 0; i--) {
                double v = 2.0 * x[0] - x[i];
                filter(v);
            }
        }

        void storeRightPad(double[] x) {
            reducePadLengthIfSignalIsTooShort(x);

            for (int i = 0; i < padLength; i++) {
                rightPad[padLength - 1 - i] = 2.0 * x[x.length - 1] - x[x.length - padLength - 1 + i];
            }
        }

        void warmUpWithPaddingRight() {
            for (int i = 0; i < padLength; i++) {
                rightPad[i] = filter(rightPad[i]);
            }

            initForConstantInput(rightPad[padLength - 1]);
            for (int i = padLength - 1; i >= 0; i--) {
                filter(rightPad[i]);
            }
        }

        void reducePadLengthIfSignalIsTooShort(double[] x) {
            if (x.length < padLength + 1) {
                padLength = x.length - 1;
            }
        }

        void initForConstantInput(double x0) {
            // compute y_ss
            double sumB = 0.0;
            double sumA = 0.0;
            for (double v : b) sumB += v;
            for (double v : a) sumA += v; // a[0] usually 1

            double ySs = (sumA == 0.0) ? 0.0 : (sumB / sumA) * x0;

            // set z[k] = sum_{i=k+1..N} b[i]*x0 - sum_{i=k+1..N} a[i]*ySs
            int N = b.length - 1;
            for (int k = 0; k < N; k++) {
                double sb = 0.0;
                double sa = 0.0;
                for (int i = k + 1; i <= N; i++) {
                    sb += b[i];
                    sa += a[i];
                }
                z[k] = sb * x0 - sa * ySs;
            }
        }

        // Filter a single sample
        double filter(double x) {
            double y = b[0] * x + z[0];
            for (int i = 1; i < order; i++) {
                z[i - 1] = b[i] * x + z[i] - a[i] * y;
            }
            z[order - 1] = b[order] * x - a[order] * y;
            return y;
        }
    }

    private static class ButterworthLowPass extends ButterworthFilter {

        ButterworthLowPass(int order, double cutoffFreq, double samplingRate) {
            super(order, cutoffFreq, samplingRate);
        }

        // Compute filter coefficients (bilinear transform)
        protected void calculateCoefficients() {
            // Normalize cutoff frequency
            double wc = Math.tan(Math.PI * cutoffFreq / samplingRate);

            if (order == 1) {
                b[0] = wc / (1 + wc);
                b[1] = b[0];
                a[0] = 1;
                a[1] = (1 - wc) / (1 + wc);
            } else if (order == 2) {
                double sqrt2 = Math.sqrt(2);
                double wc2 = wc * wc;
                double denom = 1 + sqrt2 * wc + wc2;
                b[0] = wc2 / denom;
                b[1] = 2 * b[0];
                b[2] = b[0];
                a[0] = 1;
                a[1] = 2 * (wc2 - 1) / denom;
                a[2] = (1 - sqrt2 * wc + wc2) / denom;
            } else {
                throw new IllegalArgumentException("Only order 1 or 2 supported");
            }
        }
    }

    private static class ButterworthHighPass extends ButterworthFilter {

        ButterworthHighPass(int order, double cutoffFreq, double samplingRate) {
            super(order, cutoffFreq, samplingRate);
        }

        protected void calculateCoefficients() {
            double omega = Math.tan(Math.PI * cutoffFreq / samplingRate);

            if (order == 1) {
                // 1st-order Butterworth high-pass
                double denom = omega + 1;
                b[0] = 1 / denom;
                b[1] = -b[0];
                a[0] = 1;
                a[1] = (omega - 1) / denom;
            } else if (order == 2) {
                // 2nd-order Butterworth high-pass
                double sqrt2 = Math.sqrt(2);
                double omega2 = omega * omega;
                double denom = 1 + sqrt2 * omega + omega2;

                b[0] = 1 / denom;
                b[1] = -2 / denom;
                b[2] = 1 / denom;
                a[0] = 1;
                a[1] = 2 * (omega2 - 1) / denom;
                a[2] = (1 - sqrt2 * omega + omega2) / denom;
            } else {
                throw new IllegalArgumentException("Only order 1 or 2 supported");
            }
        }
    }
}
