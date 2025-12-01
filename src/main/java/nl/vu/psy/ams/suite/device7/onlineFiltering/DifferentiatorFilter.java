package nl.vu.psy.ams.suite.device7.onlineFiltering;

/**
 * Author: Aniket Mazumder
 * Organization: Vrije Universiteit Amsterdam, The Netherlands
 * A java class that is designed to take in a signal at
 * 1000Hz as input and return a derivative of the signal.
 */

public class DifferentiatorFilter extends ButterworthFilter {
    private int samplingRate, cutoffFrequency;
    private double[] m_a = new double[4];
    private double[] m_b = new double[4];

    private double[] m_input_history = new double[3]; // input history buffer
    private double[] m_output_history = new double[3]; // output history buffer

    public DifferentiatorFilter(int sampling_rate, int cutoff_frequency) {
        if (sampling_rate == 1000)
            if (cutoff_frequency == 2) {
                samplingRate = sampling_rate;
                cutoffFrequency = cutoff_frequency;
                setFilterCoefficients();
            } else
                throw new IllegalArgumentException("Incorrect butterworth filter arguments");
        else
            throw new IllegalArgumentException("Incorrect butterworth filter arguments");

    }

    private void setFilterCoefficients() {
        if (samplingRate == 1000 && cutoffFrequency == 2) {
            m_b[0] = 0.2317; // numerator coefficient for input
            m_b[1] = 2.7756e-17; // numerator coefficient for delayed input
            m_b[2] = 5.5511e-17; // numerator coefficient for doubly delayed input
            m_b[3] = 0;// numerator coefficient for triple delayed input
            m_a[1] = -5.5511e-17; // denominator coefficient for delayed output
            m_a[2] = -2.7756e-17; // denominator coefficient for double delayed output
            m_a[3] = -0.2317; // denominator coefficient for triple delayed output
        }

    }

    public double process(double input) {
        double output = 0.0;
        // compute output using filter coefficients and history buffers
        output = m_b[0] * input +
                m_b[1] * m_input_history[0] +
                m_b[2] * m_input_history[1] +
                m_b[3] * m_input_history[2] -
                m_a[1] * m_output_history[0] -
                m_a[2] * m_output_history[1] -
                m_a[3] * m_output_history[2];

        // update input and output history buffers
        m_input_history[2] = m_input_history[1]; // update delayed input
        m_input_history[1] = m_input_history[0]; // update delayed input
        m_input_history[0] = input; // update most recent input

        m_output_history[2] = m_output_history[1]; // update delayed output
        m_output_history[1] = m_output_history[0]; // update delayed output
        m_output_history[0] = output; // update most recent output

        return output;
    }
}
