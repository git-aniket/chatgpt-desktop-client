package nl.vu.psy.ams.suite.data;

/**
 * Implementation of Madgwick's IMU and AHRS algorithms.
// See: http://www.x-io.co.uk/open-source-imu-and-ahrs-algorithms/
//
// From the x-io website "Open-source resources available on this website are
// provided under the GNU General Public Licence unless an alternative licence
// is provided in source."
//
// Date			Author          Notes
// 29/09/2011	SOH Madgwick    Initial release
// 02/10/2011	SOH Madgwick	Optimised for reduced CPU load
// 19/02/2012	SOH Madgwick	Magnetometer measurement is normalised
// 06/04/2023   Aniket Mazumder Adapted for Java; changed floats to doubles
 */

public class MadgwickFilter {
    private double beta = 0.1; // algorithm gain
    private double q0 = 1.0;
    private double q1;
    private double q2;
    private double q3; // quaternion of sensor frame relative to auxiliary frame
    private double invSampleFreq;
    private double sampleFreq;
    private double roll;// theta
    private double pitch;// phi
    private double yaw;
    private boolean anglesComputed;
    // private long timeLastCall = 0;
    // private long timeNow = System.currentTimeMillis();
    private boolean algoInit = false;
    public double thetaInit = 0; // Initial Roll
    public double phiInit = 0; // Inital Pitch
    public double shiInit = 0; // Inital Yaw

    // private SensorBiases sensorBias;
    // private long timeDifference;

    // private class SensorBiases {
    // double bias_ax;
    // double bias_ay;
    // double bias_az;
    // double bias_gx;
    // double bias_gy;
    // double bias_gz;
    // double bias_mx;
    // double bias_my;
    // double bias_mz;
    // }

    public MadgwickFilter(double sampling_frequency) {
        this.sampleFreq = sampling_frequency;
        this.invSampleFreq = 1.0 / this.sampleFreq;

        // change this with actual values
        // sensorBias.bias_ax = 0.0;
        // sensorBias.bias_ay = 0.0;
        // sensorBias.bias_az = 0.0;
        // sensorBias.bias_gx = 0.0;
        // sensorBias.bias_gy = 0.0;
        // sensorBias.bias_gz = 0.0;
        // sensorBias.bias_mx = 0.0;
        // sensorBias.bias_my = 0.0;
        // sensorBias.bias_mz = 0.0;

    }

    // fast inverse square root from quakeIII modified for java
    // public static double invSqrt(double x) {
    // double xhalf = 0.5d * x;
    // long i = Double.doubleToLongBits(x);
    // i = 0x5fe6ec85e7de30daL - (i >> 1);
    // x = Double.longBitsToDouble(i);
    // x *= (1.5d - xhalf * x * x);
    // return x;
    // }

    // Inverse square root from Java
    public static double invSqrt(double x) {
        return 1.0 / Math.sqrt(x);
    }

    // private void removeSensorBiases() {

    // }

    /**
     * Algorithm to initialize the sensor tilt on any orientation
     * 
     * @param ax
     * @param ay
     * @param az
     * @param mx
     * @param my
     * @param mz
     * @return true if successfully initialized
     * @return false if error
     */
    @SuppressWarnings("unused")
    public boolean InitializeOrientationAlgo(double ax, double ay, double az, double mx, double my, double mz) {

        double bx, by, bz; // Magnetometer readings after rotating to the plane where theta=phi=0
        double recipNormInitAcc, recipNormInitMag;

        if (!((ax == 0.0) && (ay == 0.0) && (az == 0.0))) {
            // Normalise accelerometer measurement
            recipNormInitAcc = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNormInitAcc;
            ay *= recipNormInitAcc;
            az *= recipNormInitAcc;

            // get angles to the main axes using atan2
            this.thetaInit = Math.atan2(ay, ax);
            this.phiInit = Math.atan2(-ax, Math.sqrt(ay * ay + az * az));

            // Normalize magnetometer measurement
            recipNormInitMag = invSqrt(mx * mx + my * my + mz * mz);
            mx *= recipNormInitMag;
            my *= recipNormInitMag;
            mz *= recipNormInitMag;

            // estimate magnetometer reading after rotating them to the plane where
            // theta=phi=0
            bx = mx * Math.cos(this.thetaInit) + my * Math.sin(this.thetaInit) * Math.sin(this.phiInit)
                    + mz * Math.sin(this.thetaInit) * Math.cos(this.phiInit);
            by = my * Math.cos(this.phiInit) - mz * Math.sin(this.phiInit);
            bz = -mx * Math.sin(this.thetaInit) + my * Math.cos(this.thetaInit) * Math.sin(this.phiInit)
                    + mz * Math.cos(this.thetaInit) * Math.cos(this.phiInit);

            // get the yaw
            this.shiInit = Math.atan2(-by, bx);

            // convert initial angles to quaternions
            this.q0 = Math.cos(this.phiInit / 2) * Math.cos(this.thetaInit / 2) * Math.cos(this.shiInit / 2)
                    + Math.sin(this.phiInit / 2) * Math.sin(this.thetaInit / 2) * Math.sin(this.shiInit / 2);
            this.q1 = Math.sin(this.phiInit / 2) * Math.cos(this.thetaInit / 2) * Math.cos(this.shiInit / 2)
                    - Math.cos(this.phiInit / 2) * Math.sin(this.thetaInit / 2) * Math.sin(this.shiInit / 2);
            this.q2 = Math.cos(this.phiInit / 2) * Math.sin(this.thetaInit / 2) * Math.cos(this.shiInit / 2)
                    + Math.sin(this.phiInit / 2) * Math.cos(this.thetaInit / 2) * Math.sin(this.shiInit / 2);
            this.q3 = Math.cos(this.phiInit / 2) * Math.cos(this.thetaInit / 2) * Math.sin(this.shiInit / 2)
                    - Math.sin(this.phiInit / 2) * Math.sin(this.thetaInit / 2) * Math.cos(this.shiInit / 2);

            System.out.println("Orientation algo initialized");
            // System.out.println(this.q0 + " " + this.q1 + "," + this.q2 + "," + this.q3);
            System.out.println("The initial orientation of the device is:");
            System.out.println("Roll:" + thetaInit + " ,Pitch:" + phiInit + " ,Yaw:" + shiInit);
            return true;
        }

        return false;
    }

    /*
     * Use this method when all the three sensor data is available for use.
     * The output is a quaternion that can be used to obtain roll, pitch and yaw.
     */
    public void update(double gx, double gy, double gz, double ax, double ay, double az, double mx, double my,
            double mz, int timeDifference) {
        if (this.algoInit == false) {
            if (InitializeOrientationAlgo(ax, ay, az, mx, my, mz) == false)
                System.out.println("Orientation algo init failure");
            else
                this.algoInit = true;
        }

        double recipNorm;
        double s0, s1, s2, s3;
        double qDot1, qDot2, qDot3, qDot4;
        double hx, hy;
        double _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0,
                q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

        // Use IMU algorithm if magnetometer measurement invalid (avoids NaN in
        // magnetometer normalisation)
        if ((mx == 0.0) && (my == 0.0) && (mz == 0.0)) {
            updateIMU(gx, gy, gz, ax, ay, az);
            return;
        }

        // Convert gyroscope degrees/sec to radians/sec
        gx *= 0.0174533;
        gy *= 0.0174533;
        gz *= 0.0174533;

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5 * (-q1 * gx - q2 * gy - q3 * gz);
        qDot2 = 0.5 * (q0 * gx + q2 * gz - q3 * gy);
        qDot3 = 0.5 * (q0 * gy - q1 * gz + q3 * gx);
        qDot4 = 0.5 * (q0 * gz + q1 * gy - q2 * gx);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in
        // accelerometer normalisation)
        if (!((ax == 0.0) && (ay == 0.0) && (az == 0.0))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Normalise magnetometer measurement
            recipNorm = invSqrt(mx * mx + my * my + mz * mz);
            mx *= recipNorm;
            my *= recipNorm;
            mz *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0mx = 2.0 * q0 * mx;
            _2q0my = 2.0 * q0 * my;
            _2q0mz = 2.0 * q0 * mz;
            _2q1mx = 2.0 * q1 * mx;
            _2q0 = 2.0 * q0;
            _2q1 = 2.0 * q1;
            _2q2 = 2.0 * q2;
            _2q3 = 2.0 * q3;
            _2q0q2 = 2.0 * q0 * q2;
            _2q2q3 = 2.0 * q2 * q3;
            q0q0 = q0 * q0;
            q0q1 = q0 * q1;
            q0q2 = q0 * q2;
            q0q3 = q0 * q3;
            q1q1 = q1 * q1;
            q1q2 = q1 * q2;
            q1q3 = q1 * q3;
            q2q2 = q2 * q2;
            q2q3 = q2 * q3;
            q3q3 = q3 * q3;

            // Reference direction of Earth's magnetic field
            hx = mx * q0q0 - _2q0my * q3 + _2q0mz * q2 + mx * q1q1 + _2q1 * my * q2 + _2q1 * mz * q3 - mx * q2q2
                    - mx * q3q3;
            hy = _2q0mx * q3 + my * q0q0 - _2q0mz * q1 + _2q1mx * q2 - my * q1q1 + my * q2q2 + _2q2 * mz * q3
                    - my * q3q3;
            _2bx = Math.sqrt(hx * hx + hy * hy);
            _2bz = -_2q0mx * q2 + _2q0my * q1 + mz * q0q0 + _2q1mx * q3 - mz * q1q1 + _2q2 * my * q3 - mz * q2q2
                    + mz * q3q3;
            _4bx = 2.0 * _2bx;
            _4bz = 2.0 * _2bz;

            // Gradient decent algorithm corrective step
            s0 = -_2q2 * (2.0 * q1q3 - _2q0q2 - ax) + _2q1 * (2.0 * q0q1 + _2q2q3 - ay)
                    - _2bz * q2 * (_2bx * (0.5 - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
                    + (-_2bx * q3 + _2bz * q1) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
                    + _2bx * q2 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5 - q1q1 - q2q2) - mz);
            s1 = _2q3 * (2.0 * q1q3 - _2q0q2 - ax) + _2q0 * (2.0 * q0q1 + _2q2q3 - ay)
                    - 4.0 * q1 * (1 - 2.0 * q1q1 - 2.0 * q2q2 - az)
                    + _2bz * q3 * (_2bx * (0.5 - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
                    + (_2bx * q2 + _2bz * q0) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
                    + (_2bx * q3 - _4bz * q1) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5 - q1q1 - q2q2) - mz);
            s2 = -_2q0 * (2.0 * q1q3 - _2q0q2 - ax) + _2q3 * (2.0 * q0q1 + _2q2q3 - ay)
                    - 4.0 * q2 * (1 - 2.0 * q1q1 - 2.0 * q2q2 - az)
                    + (-_4bx * q2 - _2bz * q0) * (_2bx * (0.5 - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
                    + (_2bx * q1 + _2bz * q3) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
                    + (_2bx * q0 - _4bz * q2) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5 - q1q1 - q2q2) - mz);
            s3 = _2q1 * (2.0 * q1q3 - _2q0q2 - ax) + _2q2 * (2.0 * q0q1 + _2q2q3 - ay)
                    + (-_4bx * q3 + _2bz * q1) * (_2bx * (0.5 - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
                    + (-_2bx * q0 + _2bz * q2) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
                    + _2bx * q1 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5 - q1q1 - q2q2) - mz);
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // timeNow = System.currentTimeMillis();
        // timeDifference = timeNow - timeLastCall;
        if (timeDifference == 0)
            timeDifference = 1;
        invSampleFreq = 1.0 / timeDifference;
        // timeLastCall = timeNow;

        // Integrate rate of change of quaternion to yield quaternion
        q0 += qDot1 * invSampleFreq;
        // if (Double.isNaN(q0))
        // System.out.println(q0);
        q1 += qDot2 * invSampleFreq;
        q2 += qDot3 * invSampleFreq;
        q3 += qDot4 * invSampleFreq;

        // Normalise quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        // if (Double.isNaN(q0))
        // System.out.println(q0);
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
        anglesComputed = false;
    }

    /*
     * Use this method when magnetometer data is unavaiable.
     * Only pitch and roll can be calculated using this method.
     */
    public void updateIMU(double gx, double gy, double gz, double ax, double ay, double az) {
        double recipNorm;
        double s0, s1, s2, s3;
        double qDot1, qDot2, qDot3, qDot4;
        double _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2, _8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

        // Convert gyroscope degrees/sec to radians/sec
        gx *= 0.0174533;
        gy *= 0.0174533;
        gz *= 0.0174533;

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5 * (-q1 * gx - q2 * gy - q3 * gz);
        qDot2 = 0.5 * (q0 * gx + q2 * gz - q3 * gy);
        qDot3 = 0.5 * (q0 * gy - q1 * gz + q3 * gx);
        qDot4 = 0.5 * (q0 * gz + q1 * gy - q2 * gx);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in
        // accelerometer normalisation)
        if (!((ax == 0.0) && (ay == 0.0) && (az == 0.0))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0 = 2.0 * q0;
            _2q1 = 2.0 * q1;
            _2q2 = 2.0 * q2;
            _2q3 = 2.0 * q3;
            _4q0 = 4.0 * q0;
            _4q1 = 4.0 * q1;
            _4q2 = 4.0 * q2;
            _8q1 = 8.0 * q1;
            _8q2 = 8.0 * q2;
            q0q0 = q0 * q0;
            q1q1 = q1 * q1;
            q2q2 = q2 * q2;
            q3q3 = q3 * q3;

            // Gradient decent algorithm corrective step
            s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
            s1 = _4q1 * q3q3 - _2q3 * ax + 4.0 * q0q0 * q1 - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
            s2 = 4.0 * q0q0 * q2 + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
            s3 = 4.0 * q1q1 * q3 - _2q1 * ax + 4.0 * q2q2 * q3 - _2q2 * ay;
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // Integrate rate of change of quaternion to yield quaternion
        q0 += qDot1 * invSampleFreq;
        q1 += qDot2 * invSampleFreq;
        q2 += qDot3 * invSampleFreq;
        q3 += qDot4 * invSampleFreq;

        // Normalise quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
        anglesComputed = false;
    }

    private void computeAngles() {
        roll = (Math.atan2(q0 * q1 + q2 * q3, 0.5f - q1 * q1 - q2 * q2) + 2 * Math.PI) % 2 * Math.PI;
        pitch = Math.asin(-2.0f * (q1 * q3 - q0 * q2));
        yaw = (Math.atan2(q1 * q2 + q0 * q3, 0.5f - q2 * q2 - q3 * q3) + 2 * Math.PI) % 2 * Math.PI;
        // if (Double.isNaN(roll) || Double.isNaN(pitch) || Double.isNaN(yaw))
        // System.out.print("angle problem");
        anglesComputed = true;
    }

    public double getRollDegrees() {
        if (!anglesComputed)
            computeAngles();
        return roll * 57.29578;
    }

    public double getPitchDegrees() {
        if (!anglesComputed)
            computeAngles();
        return pitch * 57.29578;
    }

    public double getYawDegrees() {
        if (!anglesComputed)
            computeAngles();
        return yaw * 57.29578;
    }

    public double getRollRadians() {
        if (!anglesComputed)
            computeAngles();
        return roll;
    }

    public double getPitchRadians() {
        if (!anglesComputed)
            computeAngles();
        return pitch;
    }

    public double getYawRadians() {
        if (!anglesComputed)
            computeAngles();
        return yaw;
    }
}