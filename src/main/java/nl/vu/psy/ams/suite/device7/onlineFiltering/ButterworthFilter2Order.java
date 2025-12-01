
/**
 * Author: Aniket Mazumder
 * Organization: Vrije Universiteit Amsterdam, The Netherlands
 * A java class that is designed to take in noisy sample data at
 * 1000Hz as input and return a low pass filtered signal with a pre-selected
 * cutoff frequency(10Hz) as output 
 */
package nl.vu.psy.ams.suite.device7.onlineFiltering;

public class ButterworthFilter2Order extends ButterworthFilter {
    private int samplingRate, cutoffFrequency;
    private double[] m_a = new double[3];
    private double[] m_b = new double[3];

    private double[] m_input_history = new double[2]; // input history buffer
    private double[] m_output_history = new double[2]; // output history buffer

    public ButterworthFilter2Order(int sampling_rate, int cutoff_frequency) {
        if (sampling_rate == 50 || sampling_rate == 1000 || sampling_rate == 100)
            if (cutoff_frequency == 2 || cutoff_frequency == 5 || cutoff_frequency == 10) {
                samplingRate = sampling_rate;
                cutoffFrequency = cutoff_frequency;
                setFilterCoefficients();
            } else
                throw new IllegalArgumentException("Incorrect butterworth filter arguments");
        else
            throw new IllegalArgumentException("Incorrect butterworth filter arguments");

    };

    // Note: The 10Hz filter doesn't really filter the data much
    // Choosing a 5Hz filter results in a smoother data.
    private void setFilterCoefficients() {
        // for 50Hz signal with cutoff 5Hz
        if (samplingRate == 50 && cutoffFrequency == 5) {
            m_b[0] = 0.0675; // numerator coefficient for input
            m_b[1] = 0.1349; // numerator coefficient for delayed input
            m_b[2] = 0.0675; // numerator coefficient for doubly delayed input
            m_a[1] = -1.1430; // denominator coefficient for delayed output
            m_a[2] = 0.4128; // denominator coefficient for doubly delayed output
        }
        // for 50Hz signal with cutoff 10Hz
        else if (samplingRate == 50 && cutoffFrequency == 10) {
            m_b[0] = 0.2066; // numerator coefficient for input
            m_b[1] = 0.4131; // numerator coefficient for delayed input
            m_b[2] = 0.2066; // numerator coefficient for doubly delayed input
            m_a[1] = -0.3695; // denominator coefficient for delayed output
            m_a[2] = 0.1958; // denominator coefficient for doubly delayed output
        }
        // for 1000Hz signal with cutoff 2Hz
        else if (samplingRate == 1000 && cutoffFrequency == 2) {
            m_b[0] = 3.9130e-5; // numerator coefficient for input
            m_b[1] = 7.8260e-5; // numerator coefficient for delayed input
            m_b[2] = 3.9130e-5; // numerator coefficient for doubly delayed input
            m_a[1] = -1.9822; // denominator coefficient for delayed output
            m_a[2] = 0.9824; // denominator coefficient for doubly delayed output

        }
        // for 1000Hz signal with cutoff 5Hz
        else if (samplingRate == 1000 && cutoffFrequency == 5) {
            m_b[0] = 0.00024135899874854145; // numerator coefficient for input
            m_b[1] = 0.0004827179974970829; // numerator coefficient for delayed input
            m_b[2] = 0.00024135899874854145; // numerator coefficient for doubly delayed input
            m_a[1] = -1.9555778328194147; // denominator coefficient for delayed output
            m_a[2] = 0.9565432688144089; // denominator coefficient for doubly delayed output
        }
        // for 100Hz signal with cutoff 5Hz
        else if (samplingRate == 100 && cutoffFrequency == 5) {
            m_b[0] = 0.020083365564211; // numerator coefficient for input
            m_b[1] = 0.040166731128422; // numerator coefficient for delayed input
            m_b[2] = 0.020083365564211; // numerator coefficient for doubly delayed input
            m_a[1] = -1.561018075800718; // denominator coefficient for delayed output
            m_a[2] = 0.641351538057563; // denominator coefficient for doubly delayed output
        }

        // for 1000Hz signal with cutoff 10Hz
        else if (samplingRate == 1000 && cutoffFrequency == 10) {
            m_b[0] = 0.0009; // numerator coefficient for input
            m_b[1] = 0.0019; // numerator coefficient for delayed input
            m_b[2] = 0.0009; // numerator coefficient for doubly delayed input
            m_a[1] = -1.9112; // denominator coefficient for delayed output
            m_a[2] = 0.9150; // denominator coefficient for doubly delayed output
        }
    }

    // Input:Noisy signal
    // Output:Filtered signal
    public double process(double input) {
        double output = 0.0;
        // compute output using filter coefficients and history buffers
        output = m_b[0] * input +
                m_b[1] * m_input_history[0] +
                m_b[2] * m_input_history[1] -
                m_a[1] * m_output_history[0] -
                m_a[2] * m_output_history[1];

        // update input and output history buffers
        m_input_history[1] = m_input_history[0]; // update delayed input
        m_input_history[0] = input; // update most recent input
        m_output_history[1] = m_output_history[0]; // update delayed output
        m_output_history[0] = output; // update most recent output

        // System.out.print(input);
        // System.out.print(" ");
        // System.out.println(output);

        return output;
    }
}
