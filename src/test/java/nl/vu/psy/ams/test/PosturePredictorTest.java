package nl.vu.psy.ams.test;

import ml.dmlc.xgboost4j.java.XGBoostError;
import nl.vu.psy.ams.suite.data.posture.PostureClassifier;
import nl.vu.psy.ams.suite.data.posture.PosturePredictor;
import nl.vu.psy.ams.suite.tools.DataSetGenerator;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PosturePredictorTest {

    final static String featuresFile01 = "/datasets/posture.98x1000.features.csv";
    final static String posturesFile01 = "/datasets/posture.98x1000.postures.csv";
    final static String featuresFile02 = "/datasets/motion.423020.features.csv";
    final static String posturesFile02 = "/datasets/motion.423020.postures.csv";

    @Test
    void test98x1000() throws XGBoostError, IOException {
        test(featuresFile01, posturesFile01);
    }

    @Test
    void test423020() throws XGBoostError, IOException {
        test(featuresFile02, posturesFile02);
    }

    public static void test(String featuresFile, String posturesFile) throws IOException, XGBoostError {
        System.out.printf("Checking data set: %s%n", posturesFile);
        var features = new DataSetGenerator(featuresFile, 0, 6).generate();

        // Do prediction (epoch duration is 6 seconds = 6,000,000 microseconds)
        List<PostureClassifier.PosturePeriod> postureTimeline = PosturePredictor.predict(features, 6_000_000L);

        // Check accuracy
        boolean success = true;
        try (InputStream referenceStream = PosturePredictorTest.class.getResourceAsStream(posturesFile)) {
            assert referenceStream != null;
            try (BufferedReader references = new BufferedReader(new InputStreamReader(referenceStream))) {
                int referenceIndex = 1;
                for (PostureClassifier.PosturePeriod period : postureTimeline) {
                    long periodLength = Math.round((period.endTime() - period.startTime()) / 6.0);
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
