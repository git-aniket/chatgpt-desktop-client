package nl.vu.psy.ams.test;

import ml.dmlc.xgboost4j.java.XGBoostError;
import nl.vu.psy.ams.suite.data.posture.PostureClassifier;
import nl.vu.psy.ams.suite.data.posture.PosturePredictor;
import nl.vu.psy.ams.suite.data.posture.PosturePreprocessor;
import nl.vu.psy.ams.suite.data.structures.Sample;
import nl.vu.psy.ams.suite.tools.DataSetGenerator;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RawToPosturePeriodTest {
    final static String rawFile01 = "/datasets/motion.423020.raw.csv";
    final static String postureFile01 = "/datasets/motion.423020.postures.csv";

    @Test
    public void testClassification() throws IOException, XGBoostError {
        // Load data set
        var rawMotionGenerator = new DataSetGenerator(rawFile01);
        List<float[]> motion = rawMotionGenerator.generateCsvData();

        final int numRows = motion.size();
        final int numCols = motion.getFirst().length;
        System.out.print("Sample: " + rawFile01);
        System.out.println(" [" + numCols + "x" + numRows + "]");

        // Validate
        assert numCols == 6;

        // Reshape data
        double[] mxr = new double[numRows];
        double[] myr = new double[numRows];
        double[] mzr = new double[numRows];
        double[] gyroX = new double[numRows];
        double[] gyroY = new double[numRows];
        double[] gyroZ = new double[numRows];
        for (int i = 0; i < numRows; i++) {
            mxr[i] = motion.get(i)[0];
            myr[i] = motion.get(i)[1];
            mzr[i] = motion.get(i)[2];
            gyroX[i] = motion.get(i)[3];
            gyroY[i] = motion.get(i)[4];
            gyroZ[i] = motion.get(i)[5];
        }

        // Do preprocessing
        var preprocessor = PosturePreprocessor.getInstance();
        preprocessor.setAccelSamples(mxr, myr, mzr);
        preprocessor.setGyroSamples(gyroX, gyroY, gyroZ);

        long startTime = System.currentTimeMillis();
        var preprocessingResult = preprocessor.preprocess(0, 1_000);
        List<Sample> features = preprocessingResult.samples();
        System.out.printf("Preprocess time: %d ms%n", System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        List<PostureClassifier.PosturePeriod> postureTimeline = PosturePredictor.predict(features,
                preprocessingResult.epochDurationMicros());
        System.out.printf("Prediction time: %d ms%n", System.currentTimeMillis() - startTime);

        System.out.printf("Checking data set: %s%n", postureFile01);

        // Check accuracy
        boolean success = true;
        try (InputStream referenceStream = PosturePredictorTest.class.getResourceAsStream(postureFile01)) {
            assert referenceStream != null;
            try (BufferedReader references = new BufferedReader(new InputStreamReader(referenceStream))) {
                int referenceIndex = 1;
                for (PostureClassifier.PosturePeriod period : postureTimeline) {
                    long periodLength = Math.round((period.endTime() - period.startTime()) / 6_000_000);
                    for (int subPeriodIndex = 0; subPeriodIndex < periodLength; subPeriodIndex++) {
                        String reference = references.readLine();
                        if (!period.posture().name().equalsIgnoreCase(reference)) {
                            success = false;
                            System.out.printf("Prediction FAILED at sample %d. ", referenceIndex);
                            System.out.printf("Predicted %s != %s%n", period.posture().name(), reference);
                        }
                        referenceIndex++;
                    }
                }
                assertTrue(success);
                System.out.println("Prediction test SUCCEEDED.");
            }
        }
    }

}
