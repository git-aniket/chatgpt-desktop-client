package nl.vu.psy.ams.suite.data.posture;

import com.github.psambit9791.jdsp.signal.Decimate;
import com.github.psambit9791.jdsp.filter.Butterworth;

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

    /**
     * Apply zero-phase Butterworth bandpass filter using JDSP library.
     * This method pads the signal using reflection, applies forward and backward
     * filtering
     * to achieve zero-phase distortion, and then removes the padding.
     *
     * @param signal  Input signal to filter
     * @param order   Filter order
     * @param lowCut  Lower cutoff frequency (Hz)
     * @param highCut Upper cutoff frequency (Hz)
     * @param Fs      Sampling frequency (Hz)
     * @return Filtered signal with zero phase distortion
     */
    public static double[] zeroPhaseBandPassFilterJDSP(double[] signal, int order, double lowCut, double highCut,
            int Fs) {
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

    /**
     * Reverse the elements of an array.
     *
     * @param array Input array to reverse
     * @return New array with elements in reverse order
     */
    private static double[] reverseArray(double[] array) {
        double[] reversed = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            reversed[i] = array[array.length - 1 - i];
        }
        return reversed;
    }

    /**
     * Apply zero-phase Butterworth low-pass filter using JDSP library.
     * This method pads the signal using reflection, applies forward and backward
     * filtering to achieve zero-phase distortion, and then removes the padding.
     * 
     * Uses frequency-dependent padding to avoid edge effects and settling
     * transients.
     *
     * @param signal Input signal to filter
     * @param order  Filter order
     * @param cutOff Cutoff frequency (Hz)
     * @param Fs     Sampling frequency (Hz)
     * @return Filtered signal with zero phase distortion
     */
    public static double[] zeroPhaseLowPassFilterJDSP(double[] signal, int order, double cutOff, int Fs) {
        // Calculate padding based on filter settling time for proper edge handling
        // Settling time ≈ 3/(2π × cutoff) seconds
        // Padding should be ~3× settling time to avoid transients
        double settlingTimeSec = 3.0 / (2.0 * Math.PI * cutOff);
        int padlen = (int) Math.ceil(3.0 * settlingTimeSec * Fs);
        // Cap at signal length - 1 to avoid issues
        padlen = Math.min(padlen, signal.length - 1);
        if (padlen <= 0) { // Handle very short signals
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
        double[] forwardFilteredPadded = filterForward.lowPassFilter(paddedSignal, order, cutOff);

        // 3. Reverse the signal
        double[] reversedPadded = reverseArray(forwardFilteredPadded);

        // 4. Apply reverse filter
        Butterworth filterReverse = new Butterworth(Fs); // Fresh instance
        double[] backwardFilteredPadded = filterReverse.lowPassFilter(reversedPadded, order, cutOff);

        // 5. Final reversal
        double[] zeroPhaseFilteredPadded = reverseArray(backwardFilteredPadded);

        // 6. Trim the signal to remove padding
        double[] result = new double[signal.length];
        System.arraycopy(zeroPhaseFilteredPadded, padlen, result, 0, signal.length);

        return result;
    }

    /**
     * Apply zero-phase Butterworth high-pass filter using JDSP library.
     * This method pads the signal using reflection, applies forward and backward
     * filtering to achieve zero-phase distortion, and then removes the padding.
     * 
     * Uses frequency-dependent padding to avoid edge effects and settling
     * transients.
     *
     * @param signal Input signal to filter
     * @param order  Filter order
     * @param cutOff Cutoff frequency (Hz)
     * @param Fs     Sampling frequency (Hz)
     * @return Filtered signal with zero phase distortion
     */
    public static double[] zeroPhaseHighPassFilterJDSP(double[] signal, int order, double cutOff, int Fs) {
        // Calculate padding based on filter settling time for proper edge handling
        // Settling time ≈ 3/(2π × cutoff) seconds
        // Padding should be ~3× settling time to avoid transients
        double settlingTimeSec = 3.0 / (2.0 * Math.PI * cutOff);
        int padlen = (int) Math.ceil(3.0 * settlingTimeSec * Fs);
        // Cap at signal length - 1 to avoid issues
        padlen = Math.min(padlen, signal.length - 1);
        if (padlen <= 0) { // Handle very short signals
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
        double[] forwardFilteredPadded = filterForward.highPassFilter(paddedSignal, order, cutOff);

        // 3. Reverse the signal
        double[] reversedPadded = reverseArray(forwardFilteredPadded);

        // 4. Apply reverse filter
        Butterworth filterReverse = new Butterworth(Fs); // Fresh instance
        double[] backwardFilteredPadded = filterReverse.highPassFilter(reversedPadded, order, cutOff);

        // 5. Final reversal
        double[] zeroPhaseFilteredPadded = reverseArray(backwardFilteredPadded);

        // 6. Trim the signal to remove padding
        double[] result = new double[signal.length];
        System.arraycopy(zeroPhaseFilteredPadded, padlen, result, 0, signal.length);

        return result;
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
            for (double v : b)
                sumB += v;
            for (double v : a)
                sumA += v; // a[0] usually 1

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
