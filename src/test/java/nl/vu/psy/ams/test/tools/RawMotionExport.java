package nl.vu.psy.ams.test.tools;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RawMotionExport {
    public static void export(String fileName) throws IOException {
        CurrentOpenData cod = CurrentOpenData.getInstance();

        // Check accel data exists
        if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")) {
            System.out.println("Acceleration data NOT found, skipping export.");
            return;
        }

        // Check gyro data exists
        if (!cod.channelExists("GyroX") || !cod.channelExists("GyroY") || !cod.channelExists("GyroZ")) {
            System.out.println("Gyroscope data NOT found, skipping export.");
            return;
        }

        // Read motion data
        BinaryFile mxrBinaryFile = new BinaryFile("MXR");
        double[] mxr = mxrBinaryFile.getDataRun();
        BinaryFile myrBinaryFile = new BinaryFile("MYR");
        double[] myr = myrBinaryFile.getDataRun();
        BinaryFile mzrBinaryFile = new BinaryFile("MZR");
        double[] mzr = mzrBinaryFile.getDataRun();
        try {
            mxrBinaryFile.close();
            myrBinaryFile.close();
            mzrBinaryFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BinaryFile gxrBinaryFile = new BinaryFile("GyroX");
        double[] gyroX = gxrBinaryFile.getDataRun();
        BinaryFile gyrBinaryFile = new BinaryFile("GyroY");
        double[] gyroY = gyrBinaryFile.getDataRun();
        BinaryFile gzrBinaryFile = new BinaryFile("GyroZ");
        double[] gyroZ = gzrBinaryFile.getDataRun();
        try {
            gxrBinaryFile.close();
            gyrBinaryFile.close();
            gzrBinaryFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check sample size
        if (mxr.length != myr.length || mxr.length != mzr.length ||
                mxr.length != gyroX.length || mxr.length != gyroY.length || mxr.length != gyroZ.length) {
            System.out.println("Motion data is corrupted. Stopping export.");
            return;
        }

        System.out.printf("Raw motion data loaded. Sample size: %d%n", mxr.length);
        System.out.printf("Exporting to CSV file: %s%n", fileName);

        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            for (int i = 0; i < mxr.length; i++) {
                pw.println(String.format("%e,%e,%e,%e,%e,%e", mxr[i], myr[i], mzr[i], gyroX[i], gyroY[i], gyroZ[i]));
            }
        }
    }
}
