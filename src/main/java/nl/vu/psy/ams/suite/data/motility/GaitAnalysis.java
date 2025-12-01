// /**
// * Author: Aniket Mazumder
// * Organization: Vrije Universiteit Amsterdam, The Netherlands
// * A java class that is designed to take in a few seconds of noisy motility
// data at 1000 Hz
// * as input and return the calculated gait parameters.
// */
// package nl.vu.psy.ams.suite.data.motility;

// import nl.vu.psy.ams.suite.tools.RingBuffer;
// //import org.netlib.util.intW;

// public class GaitAnalysis {

// public class GaitParameterList {
// public class PedometerConstants {
// // Pedometer constants
// final int CONST_PEDO_MIN_STEP_COUNT_INC_THRESHOLD = 1; // min steps to
// increment step counter
// final int CONST_PEDO_MIN_STEP_TIME = 500; // min time(ms) to consider HS
// final int CONST_PEDO_MAX_STEP_TIME = 4000; // max time(ms) to consider HS
// final int CONST_DEFAULT_STEP_TIME = -1;// step time when no steps have been
// taken
// final int CONST_ANALYSIS_TIME_SECONDS = 5; // number of seconds of data to
// analyse at once
// final int CONST_BUFFER_SIZE = CONST_ANALYSIS_TIME_SECONDS * 1000; // about 5
// seconds of data
// }

// PedometerConstants pedometerConstants = new PedometerConstants();

// public class SensorData {
// RingBuffer buf_ax = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_ay = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_az = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_gx = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_gy = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_gz = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_mx = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_my = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// RingBuffer buf_mz = new RingBuffer(pedometerConstants.CONST_BUFFER_SIZE);
// int data_timestamp;
// }

// SensorData sensorData = new SensorData();

// public class PedometeVariable {
// // Pedometer variables
// int step_count;
// int step_time;
// int cadance;
// }

// PedometeVariable pedoVariables = new PedometeVariable();

// }

// GaitParameterList gp = new GaitParameterList();

// public boolean updateDataBufferAG(double ax, double ay, double az, double gx,
// double gy, double gz) {
// if (ax == 0 && ay == 0 && az == 0 && gx == 0 && gy == 0 && gz == 0) {
// System.out.println("Sensor data error");
// return false;
// }

// gp.sensorData.buf_ax.add(ax);
// gp.sensorData.buf_ay.add(ay);
// gp.sensorData.buf_az.add(az);
// gp.sensorData.buf_gx.add(gx);
// gp.sensorData.buf_gy.add(gy);
// gp.sensorData.buf_gz.add(gz);

// return true;
// }

// /**
// * Method does the following:-
// * 1. takes N(CONST_ANALYSIS_TIME_SECONDS) seconds of data
// * 2. filters the data in the buffer.
// * 3. finds peaks in the filtered buffer
// * 4. checks if two peaks may be considered a gait event
// * 5. checks if gait event is a step
// * 6. updates gait parameters if step detected
// *
// * This method should only be called once every N seconds
// */
// public boolean analyseGait() {

// // filter the data buffer with a low pass filter

// // find peaks in filtered data buffer

// // check if two peak may be considered a gait event

// // check if gait event is a step

// // update gait parameters if step detected

// return true;
// }
// }