package nl.vu.psy.ams.suite.data;

/**
 * Inputs;
 * Accelerometer (ax, ay, az): m/s²
 * Gyroscope (gx, gy, gz): rad/s
 * Magnetometer (mx, my, mz): uT or any other consistent unit
 */
public class ComplementaryFilter {

    private double q0 = 1.0; // Scalar component
    private double q1 = 0.0; // X-component
    private double q2 = 0.0; // Y-component
    private double q3 = 0.0; // Z-component

    private final double alpha = 0.98; // Tuning parameter for complementary filter
    private double previousPitch = 0.0; // For smoothing
    private final double smoothingFactor = 0.9; // Tuning parameter for smoothing

    public void update(double gyroX, double gyroY, double gyroZ,
            double accelX, double accelY, double accelZ,
            double magX, double magY, double magZ,
            double deltaTime) {
        // Normalize accelerometer data
        double accelNorm = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
        if (accelNorm > 0.0) {
            accelX /= accelNorm;
            accelY /= accelNorm;
            accelZ /= accelNorm;
        }

        // Normalize magnetometer data
        double magNorm = Math.sqrt(magX * magX + magY * magY + magZ * magZ);
        if (magNorm > 0.0) {
            magX /= magNorm;
            magY /= magNorm;
            magZ /= magNorm;
        }

        // Estimate gravity direction using current quaternion
        double gravityX = 2 * (q1 * q3 - q0 * q2);
        double gravityY = 2 * (q0 * q1 + q2 * q3);
        double gravityZ = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3;

        // Compute error between measured and estimated gravity
        double errorX = accelY * gravityZ - accelZ * gravityY;
        double errorY = accelZ * gravityX - accelX * gravityZ;
        double errorZ = accelX * gravityY - accelY * gravityX;

        // Apply complementary filter: Blend gyroscope and accelerometer corrections
        double correctedGyroX = gyroX + alpha * errorX;
        double correctedGyroY = gyroY + alpha * errorY;
        double correctedGyroZ = gyroZ + alpha * errorZ;

        // Compute reference direction of Earth's magnetic field
        double magNorthX = magX * gravityZ - magZ * gravityX;
        double magNorthY = magY * gravityZ - magZ * gravityY;

        // Normalize the horizontal component of the magnetic field
        double magNorthNorm = Math.sqrt(magNorthX * magNorthX + magNorthY * magNorthY);
        if (magNorthNorm > 0.0) {
            magNorthX /= magNorthNorm;
            magNorthY /= magNorthNorm;
        }

        // Calculate yaw correction from magnetometer
        double magYaw = Math.atan2(magNorthY, magNorthX);
        double estimatedYaw = getYawDegrees();
        double yawError = magYaw - Math.toRadians(estimatedYaw);

        // Adjust yaw correction to reduce drift
        correctedGyroZ += alpha * yawError;

        // Update quaternion with corrected gyroscope data
        double dq0 = 0.5 * (-q1 * correctedGyroX - q2 * correctedGyroY - q3 * correctedGyroZ) * deltaTime;
        double dq1 = 0.5 * (q0 * correctedGyroX + q2 * correctedGyroZ - q3 * correctedGyroY) * deltaTime;
        double dq2 = 0.5 * (q0 * correctedGyroY - q1 * correctedGyroZ + q3 * correctedGyroX) * deltaTime;
        double dq3 = 0.5 * (q0 * correctedGyroZ + q1 * correctedGyroY - q2 * correctedGyroX) * deltaTime;

        q0 += dq0;
        q1 += dq1;
        q2 += dq2;
        q3 += dq3;

        // Normalize quaternion to maintain unit length
        double quaternionNorm = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        if (quaternionNorm > 0.0) {
            q0 /= quaternionNorm;
            q1 /= quaternionNorm;
            q2 /= quaternionNorm;
            q3 /= quaternionNorm;
        }
    }

    public double getRollDegrees() {
        return Math.toDegrees(Math.atan2(2 * (q0 * q1 + q2 * q3), 1 - 2 * (q1 * q1 + q2 * q2)));
    }

    public double getPitchDegrees() {
        double sinPitch = 2 * (q0 * q2 - q3 * q1);
        sinPitch = Math.max(-1.0, Math.min(1.0, sinPitch)); // Constrain to [-1, 1]
        return Math.toDegrees(Math.asin(sinPitch)); // Direct pitch in degrees
    }

    public double getSmoothedPitchDegrees() {
        double currentPitch = getPitchDegrees();
        previousPitch = smoothingFactor * previousPitch + (1 - smoothingFactor) * currentPitch;
        return previousPitch;
    }

    public double getYawDegrees() {
        return Math.toDegrees(Math.atan2(2 * (q0 * q3 + q1 * q2), 1 - 2 * (q2 * q2 + q3 * q3)));
    }

    public double[] getEulerAngles() {
        double roll = getRollDegrees();
        double pitch = getPitchDegrees();
        double yaw = getYawDegrees();
        return new double[] { roll, pitch, yaw };
    }
}
