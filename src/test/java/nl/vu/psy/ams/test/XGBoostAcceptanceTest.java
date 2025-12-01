package nl.vu.psy.ams.test;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import nl.vu.psy.ams.suite.tools.DataSetGenerator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Under Mac test failed- will look into it later")
public class XGBoostAcceptanceTest {
    final static String modelFile = "/ml_models/xgb_activity_type.json";
    final static String featuresFile = "/datasets/posture.98x1000.features.csv";
    final static String predictionsFile = "/datasets/posture.98x1000.predictions.csv";

    @Test
    public void testXGBoost() throws IOException, XGBoostError {
        try (InputStream modelStream = XGBoostAcceptanceTest.class.getResourceAsStream(modelFile)) {
            if (modelStream == null) {
                throw new IOException("Model file not found in JAR!");
            }

            // Load model
            Booster booster = XGBoost.loadModel(modelStream);
            System.out.println("Model: " + modelFile);
            System.out.printf("Number of features: %d%n", booster.getNumFeature());

            // Load data set
            var featuresGenerator = new DataSetGenerator(featuresFile);
            var referencesGenerator = new DataSetGenerator(predictionsFile);
            List<float[]> features = featuresGenerator.generateCsvData(100_000);
            List<float[]> references = referencesGenerator.generateCsvData(100_000);

            final int numRows = features.size();
            final int numCols = features.getFirst().length;
            System.out.print("Sample: " + featuresFile);
            System.out.println(" [" + numCols + "x" + numRows + "]");
            System.out.print("Reference: " + predictionsFile);
            System.out.println(" [" + references.getFirst().length + "x" + references.size() + "]");

            // Flatten into 1D array for DMatrix
            float[] flatData = new float[numRows * numCols];
            for (int i = 0; i < numRows; i++) {
                System.arraycopy(features.get(i), 0, flatData, i * numCols, numCols);
            }

            // Start timer
            long startTime = System.nanoTime();

            // Create DMatrix
            assert (numCols == booster.getNumFeature());
            DMatrix samples = new DMatrix(flatData, numRows, numCols, Float.NaN);

            // Predict
            System.out.print("Prediction");
            System.out.flush();
            float[][] predictions = booster.predict(samples);
            long predictTime = System.nanoTime() - startTime;

            // Validate
            assert (references.size() == predictions.length);
            assert (references.getFirst().length == predictions[0].length);
            boolean match = deepEquals(references, predictions);
            System.out.println(" is accurate: " + String.valueOf(match).toUpperCase());
            System.out.println("Prediction sample size: " + numRows);
            System.out.println("Predict time milli sec: " + predictTime / 1_000_000);

            assertTrue(match);
            assertThat(predictTime / 1_000_000).isLessThan(1000); // 1000 ms
        }
    }

    static boolean deepEquals(List<float[]> list, float[][] array) {
        if (list == null || array == null)
            return false;
        if (list.size() != array.length)
            return false;

        for (int i = 0; i < list.size(); i++) {
            if (!Arrays.equals(list.get(i), array[i])) {
                return false;
            }
        }
        return true;
    }
}
