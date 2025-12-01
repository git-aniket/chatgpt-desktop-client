
/**
 * Author: Aniket Mazumder
 * Organization: Vrije Universiteit Amsterdam, The Netherlands
 * A java class that is designed to take in noisy sample data at
 * 1000Hz as input and return a low pass filtered signal with a pre-selected
 * cutoff frequency(10Hz) as output 
 */
package nl.vu.psy.ams.suite.device7.onlineFiltering;

public class ButterworthFilter3Order extends ButterworthFilter {
    private int samplingRate, cutoffFrequency;
    private double[] m_a = new double[5];
    private double[] m_b = new double[5];

    private double[] m_input_history = new double[4]; // input history buffer
    private double[] m_output_history = new double[4]; // output history buffer

    public ButterworthFilter3Order(int sampling_rate, int cutoff_frequency) {
        if (sampling_rate == 1000) {
            if (cutoff_frequency == 2 || cutoff_frequency == 60 || cutoff_frequency == 22) {

                samplingRate = sampling_rate;
                cutoffFrequency = cutoff_frequency;
                setFilterCoefficients();
            } else
                throw new IllegalArgumentException("Incorrect butterworth filter arguments");
        } else if (sampling_rate == 100) {
            if (cutoff_frequency == 2) {

                samplingRate = sampling_rate;
                cutoffFrequency = cutoff_frequency;
                setFilterCoefficients();
            } else
                throw new IllegalArgumentException("Incorrect butterworth filter arguments");
        } else
            throw new IllegalArgumentException("Incorrect butterworth filter arguments");

    };

    // Note: The 10Hz filter doesn't really filter the data much
    // Choosing a 5Hz filter results in a smoother data.
    private void setFilterCoefficients() {
        // for 1000Hz signal ; for ECG
        if (samplingRate == 1000 && cutoffFrequency == 2) {
            m_b[0] = 0.00355258014728165; // numerator coefficient for input
            m_b[1] = 0; // numerator coefficient for delayed input
            m_b[2] = -0.00710516029456329; // numerator coefficient for doubly delayed input
            m_b[3] = 0; // numerator coefficient for third delayed input
            m_b[4] = 0.00355258014728165; // numerator coefficient for fourth delayed input
            m_a[4] = 0.838670728317484; // denominator coefficient for delayed output
            m_a[3] = -3.50152501965464; // denominator coefficient for doubly delayed output
            m_a[2] = 5.48701236858124; // denominator coefficient for third delayed output
            m_a[1] = -3.82415805434902; // denominator coefficient for fourth delayed output
        }
        // for 100Hz (downsampled) signal with cutoff 0.4 till 35 HZ, bandpass: [a,b]
        // =butter(2,[.008 ,.7], 'bandpass'); for ECG
        if (samplingRate == 100 && cutoffFrequency == 2) {
            m_b[0] = 0.495242276215051; // numerator coefficient for input
            m_b[1] = 0; // numerator coefficient for delayed input
            m_b[2] = -0.990484552430102; // numerator coefficient for doubly delayed input
            m_b[3] = 0; // numerator coefficient for third delayed input
            m_b[4] = 0.495242276215051; // numerator coefficient for fourth delayed input
            m_a[4] = 0.263962452624325; // denominator coefficient for delayed output
            m_a[3] = 0.179979158951598; // denominator coefficient for doubly delayed output
            m_a[2] = -0.221445269710263; // denominator coefficient for third delayed output
            m_a[1] = -1.221228740216154; // denominator coefficient for fourth delayed output
        }
        // for 1000Hz signal with cutoff 60Hz, low pass; for DZDT
        else if (samplingRate == 1000 && cutoffFrequency == 60) {
            m_b[0] = 0.000806359865037; // numerator coefficient for input
            m_b[1] = 0.003225439460149; // numerator coefficient for delayed input
            m_b[2] = 0.004838159190223; // numerator coefficient for doubly delayed input
            m_b[3] = 0.003225439460149; // numerator coefficient for third delayed input
            m_b[4] = 0.000806359865037; // numerator coefficient for fourth delayed input
            m_a[1] = 0.370814215929455; // denominator coefficient for delayed output
            m_a[2] = -1.847550944118580; // denominator coefficient for doubly delayed output
            m_a[3] = 3.507193724716208; // denominator coefficient for third delayed output
            m_a[4] = -3.017555238686490; // denominator coefficient for fourth delayed output
        }
        // for 1000Hz signal ; for SCL 2Hz low pass
        if (samplingRate == 1000 && cutoffFrequency == 22) {
            m_b[0] = 1.0e-08 * 0.153324552060193; // numerator coefficient for input
            m_b[1] = 1.0e-08 * 0.613298208240774; // numerator coefficient for delayed input
            m_b[2] = 1.0e-08 * 0.919947312361161; // numerator coefficient for doubly delayed input
            m_b[3] = 1.0e-08 * 0.613298208240774; // numerator coefficient for third delayed input
            m_b[4] = 1.0e-08 * 0.153324552060193; // numerator coefficient for fourth delayed input
            m_a[4] = 0.967695543813137; // denominator coefficient for delayed output
            m_a[3] = -3.902558784823240; // denominator coefficient for doubly delayed output
            m_a[2] = 5.902025861490880; // denominator coefficient for third delayed output
            m_a[1] = -3.967162595948849; // denominator coefficient for fourth delayed output
        }
    }

    // Input:Noisy signal
    // Output:Filtered signal
    public double process(double input) {
        double output = 0.0;
        // compute output using filter coefficients and history buffers
        output = m_b[0] * input +
                m_b[1] * m_input_history[0] +
                m_b[2] * m_input_history[1] +
                m_b[3] * m_input_history[2] +
                m_b[4] * m_input_history[3] -
                m_a[1] * m_output_history[0] -
                m_a[2] * m_output_history[1] -
                m_a[3] * m_output_history[2] -
                m_a[4] * m_output_history[3];

        // update input and output history buffers
        m_input_history[3] = m_input_history[2]; // update delayed input
        m_input_history[2] = m_input_history[1]; // update delayed input
        m_input_history[1] = m_input_history[0]; // update delayed input
        m_input_history[0] = input; // update most recent input
        m_output_history[3] = m_output_history[2]; // update delayed output
        m_output_history[2] = m_output_history[1]; // update delayed output
        m_output_history[1] = m_output_history[0]; // update delayed output
        m_output_history[0] = output; // update most recent output

        // System.out.print(input);
        // System.out.print(" ");
        // System.out.println(output);

        return output;
    }
}
