package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.files.BinaryFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MotionDataLoader {
    private static final Logger logger = LogManager.getLogger(MotionDataLoader.class);

    public record MotionData(
            double[] mxr, double[] myr, double[] mzr,
            double[] gyroX, double[] gyroY, double[] gyroZ,
            int sampleTimeMicros) {
    }

    public MotionData loadData() {
        logger.info("Loading raw motion data.");
        try (BinaryFile mxrBinaryFile = new BinaryFile("MXR");
                BinaryFile myrBinaryFile = new BinaryFile("MYR");
                BinaryFile mzrBinaryFile = new BinaryFile("MZR");
                BinaryFile gyroXBinaryFile = new BinaryFile("GyroX");
                BinaryFile gyroYBinaryFile = new BinaryFile("GyroY");
                BinaryFile gyroZBinaryFile = new BinaryFile("GyroZ")) {

            double[] mxr = mxrBinaryFile.getDataRun();
            double[] myr = myrBinaryFile.getDataRun();
            double[] mzr = mzrBinaryFile.getDataRun();
            double[] gyroX = gyroXBinaryFile.getDataRun();
            double[] gyroY = gyroYBinaryFile.getDataRun();
            double[] gyroZ = gyroZBinaryFile.getDataRun();
            int sampleTimeMicros = mxrBinaryFile.getSampleTimeInUS();

            return new MotionData(mxr, myr, mzr, gyroX, gyroY, gyroZ, sampleTimeMicros);

        } catch (Exception e) {
            logger.error("An error occurred during motion data loading: {}", e.getMessage());
            return null;
        }
    }
}
