// package nl.vu.psy.ams.suite.device7;

// /**
// * A java class that takes in the filterd motility data and detects
// * gait-parameters by utlizing the signal in realtime.
// * @Author: Aniket Mazumder; a.mazumder@vu.nl
// * @Date: April, 2023
// * @Organisation: Vrije Univesiteit Amsterdam, The Netherlands
// * @Copyright: Vrije Universiteit Amsterdam, The Netherlands
// *
// */

// import nl.vu.psy.ams.suite.device7.GaitParameterList.UserState;
// import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter2Order;
// import nl.vu.psy.ams.suite.device7.onlineFiltering.DifferentiatorFilter;
// import nl.vu.psy.ams.suite.tools.Timer;
// import java.lang.Math;

// // Class for all gait parameters that is to be calculated
// class GaitParameterList {
// enum UserState {
// STATIONARY,
// LEVEL_GROUND_WALKING,
// CLIMBING,
// }

// // Pedometer variables
// final int PEDO_MIN_STEP_COUNT_INC_THRESHOLD = 1; // min steps to increment
// step counter
// final double PEDO_MIN_STEP_TIME = 500.0; // min time(ms) to consider HS
// final double PEDO_MAX_STEP_TIME = 4000.0; // max time(ms) to consider HS
// final double DEFAULT_STEP_TIME = -1;// step time when no steps have been
// taken

// // thresholds
// final double ACCEL_X_THRESHOLD = -1.25;
// final double ACCEL_Y_THRESHOLD = 0.35;
// final double ACCEL_Z_THRESHOLD = 1.0;
// final double PHASE_THRESHOLD = 40.0;

// // structure for parameters of gait
// double pedomterClock;

// double timeLastGE; // gait event time
// double timeCurrentGE;

// // double timeLastHS;
// // double timeCurrentHS;

// // double timeDiffHS;
// // double timeDiffGE; // time difference between two gait events

// double stepTime;
// int noOfSteps;
// UserState userState;
// Timer gaitTimer = new Timer();
// double gaitPhase;
// double gaitPhaseOld = 0.0;
// ButterworthFilter2Order normFilter = new ButterworthFilter2Order(1000, 2);
// DifferentiatorFilter normDifferentiator = new DifferentiatorFilter(1000, 2);

// // flags for gait characterization algorithm
// boolean flagSearchStep = true;
// boolean flagStepHappened = false;

// boolean flagSearchResetStep = false;
// boolean flagResetStepHappened = false;

// // boolean flagSearchStanceRollover = false;
// // boolean flagStanceRolloverHappened = false;

// // boolean flagSearchPowerPoint = false;
// // boolean flagPowerHappened = false;
// }

// // Class for the parameter calculation algorithms
// public class GaitParametersGenerator {
// GaitParameterList gp = new GaitParameterList();

// GaitParametersGenerator() {
// gp.userState = UserState.STATIONARY;
// gp.noOfSteps = 0;
// gp.stepTime = gp.DEFAULT_STEP_TIME;
// gp.gaitTimer.start();
// gp.timeLastGE = System.currentTimeMillis();
// gp.timeCurrentGE = System.currentTimeMillis();
// }

// /**
// * Function calculates and returns the step time between last two steps.
// * Atleast 2 valid gait steps must happen for calculation of step time.
// * If no steps have been taken, step time is returned as -1 as default.
// *
// * This function uses only one accel axis along with a threshold to
// * detect steps.
// *
// * @param accelx
// * @return
// */
// public double processIMUDataSingleAxis(double accelx) {

// // when a stationary user first starts to walk
// if (gp.userState == UserState.STATIONARY) {
// // check if person crossed step threshold
// if (gp.flagSearchStep == true && accelx <= gp.ACCEL_X_THRESHOLD) {
// gp.flagSearchStep = false;
// gp.flagSearchResetStep = true;
// gp.stepTime = gp.DEFAULT_STEP_TIME;
// }

// // check in person crossed re-set step threshold
// if (gp.flagSearchResetStep == true && accelx > gp.ACCEL_X_THRESHOLD) {
// gp.flagSearchResetStep = false;
// gp.flagSearchStep = true;
// gp.timeLastGE = gp.timeCurrentGE;
// gp.timeCurrentGE = System.currentTimeMillis();
// ++gp.noOfSteps;
// if (gp.noOfSteps >= gp.PEDO_MIN_STEP_COUNT_INC_THRESHOLD)
// gp.userState = UserState.LEVEL_GROUND_WALKING;
// }
// }

// // when a user has already been walking
// if (gp.userState == UserState.LEVEL_GROUND_WALKING) {
// // check if person crossed step threshold
// if (gp.flagSearchStep == true && accelx <= gp.ACCEL_X_THRESHOLD) {
// gp.flagSearchStep = false;
// gp.flagSearchResetStep = true;
// }

// // check in person crossed re-set step threshold
// if (gp.flagSearchResetStep == true && accelx > gp.ACCEL_X_THRESHOLD) {
// gp.flagSearchResetStep = false;
// gp.flagSearchStep = true;
// gp.timeLastGE = gp.timeCurrentGE;
// gp.timeCurrentGE = System.currentTimeMillis();
// gp.stepTime = gp.timeCurrentGE - gp.timeLastGE;

// if (gp.stepTime > gp.PEDO_MIN_STEP_TIME && gp.stepTime <
// gp.PEDO_MAX_STEP_TIME)
// ++gp.noOfSteps;
// else if (gp.stepTime > gp.PEDO_MAX_STEP_TIME) {
// gp.noOfSteps = 0;
// gp.userState = UserState.STATIONARY;
// gp.stepTime = gp.DEFAULT_STEP_TIME;
// }
// }

// }

// // Whether walking/stationary, if time of last GE>limit, step count=0
// if (System.currentTimeMillis() - gp.timeLastGE >= gp.PEDO_MAX_STEP_TIME) {
// gp.noOfSteps = 0;
// gp.userState = UserState.STATIONARY;
// }

// return gp.stepTime;
// }

// /**
// * Function calculates number of steps by detection of GAIT-cycle PHASE
// * based on input of all three acceleration axes from chest IMU.
// *
// * @param accelx
// * @param accely
// * @param accelz
// * @return
// */
// public double processIMUDataMultiAxis(double accelx, double accely, double
// accelz) {
// double maxNorm = 0.2367;
// double maxNormDerivative = 8.0961e-6;
// final double normOffset = 1.0;

// // get norm of the 3D acceleration
// double norm = Math.sqrt(accelx * accelx + accely * accely + accelz * accelz);
// // System.out.println(norm);

// // filter the norm using a low pass filter of order 2 and cutoff frequency 2
// double filteredNorm = gp.normFilter.process(norm);

// double filteredNormOffsetted = filteredNorm - normOffset;
// // System.out.println(norm + " " + filteredNormOffsetted);

// if (filteredNormOffsetted > maxNorm)
// maxNorm = filteredNormOffsetted;

// // get the slope of the norm
// double diffFilteredNormOffsetted =
// gp.normDifferentiator.process(filteredNormOffsetted);
// // System.out.println(diffFilteredNormOffsetted);

// if (diffFilteredNormOffsetted > maxNormDerivative)
// maxNormDerivative = diffFilteredNormOffsetted;

// // get the phase angle in percentage 0-100% between norm and its derivative
// gp.gaitPhase = (Math.PI
// + Math.atan2(filteredNormOffsetted / maxNorm, diffFilteredNormOffsetted /
// maxNormDerivative))
// * (10 / 2 * Math.PI);

// // System.out.println(maxNormDerivative);

// // detect a step
// if (gp.gaitPhaseOld < gp.PHASE_THRESHOLD && gp.gaitPhase >
// gp.PHASE_THRESHOLD)
// ++gp.noOfSteps;

// gp.gaitPhaseOld = gp.gaitPhase;

// return filteredNorm;
// }
// }
