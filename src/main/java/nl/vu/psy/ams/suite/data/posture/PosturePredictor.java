package nl.vu.psy.ams.suite.data.posture;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import nl.vu.psy.ams.suite.data.posture.PostureClassifier.PosturePeriod;
import nl.vu.psy.ams.suite.data.structures.Sample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PosturePredictor {

    final static String modelFile = "/ml_models/xgb_activity_type.json";

    private static final Logger logger = LogManager.getLogger(PosturePredictor.class);

    public static List<PosturePeriod> predict(List<Sample> samples) throws IOException, XGBoostError {
        try (InputStream modelStream = PosturePredictor.class.getResourceAsStream(modelFile)) {
            if (modelStream == null) {
                String message = String.format("Model file [%s] not found in JAR!", modelFile);
                logger.error(message);
                throw new IOException(message);
            }

            // Load model
            Booster booster = XGBoost.loadModel(modelStream);
            logger.info("Open XGB Model: " + modelFile);
            logger.info("Number of features: {}", booster.getNumFeature());

            // Flatten into 1D array for DMatrix
            final int numRows = samples.size();
            final int numCols = samples.getFirst().data().length;
            float[] flatData = new float[numRows * numCols];
            for (int i = 0; i < numRows; i++) {
                System.arraycopy(samples.get(i).data(), 0, flatData, i * numCols, numCols);
            }

            // Check dimensions
            if (numCols != booster.getNumFeature()) {
                String message = String.format("Number of features [%d] does not match model features [%d].",
                        numCols, booster.getNumFeature());
                logger.error(message);
                throw new XGBoostError(message);
            }

            // Create DMatrix
            DMatrix features = new DMatrix(flatData, numRows, numCols, Float.NaN);

            // Predict
            float[][] predictions = booster.predict(features);

            // Get posture sequence
            List<Posture> postureSequence = new ArrayList<>();
            for (float[] prediction : predictions) {
                postureSequence.add(calulatePosture(prediction));
            }

            // Calculate timeline
            return calculatePostureTimeLine(postureSequence, samples);
        }
    }

    private record Prediction(Posture posture, float probability) {
    }

    static Posture calulatePosture(float[] predictions) throws XGBoostError {
        // Check dimensions
        if (predictions.length != Posture.values().length) {
            String message = String.format("Number of prediction classes [%d] is wrong. Should be [%d]",
                    predictions.length, Posture.values().length);
            logger.error(message);
            throw new XGBoostError(message);
        }

        List<Prediction> predictionList = new ArrayList<>();
        for (int i = 0; i < predictions.length; i++) {
            predictionList.add(new Prediction(Posture.values()[i], predictions[i]));
        }

        // Sort predictions based on probability
        predictionList.sort((a, b) -> Float.compare(b.probability, a.probability));
        Prediction winner = predictionList.getFirst();

        // If winner probability is too low, return the next-highest probability posture
        if (winner.probability < 0.3) {
            // predictionList is already sorted in descending probability
            if (predictionList.size() > 1) {
                Prediction secondBest = predictionList.get(1);
                return secondBest.posture();
            } else {
                // fallback if only one class exists (should not happen)
                return winner.posture();
            }
        } else {
            return winner.posture();
        }
    }

    static List<PosturePeriod> calculatePostureTimeLine(List<Posture> postureSequence, List<Sample> samples)
            throws XGBoostError {
        // Check dimensions
        if (postureSequence.size() != samples.size()) {
            String message = "Number of predictions and number of samples are different.";
            logger.error(message);
            throw new XGBoostError(message);
        }

        List<PosturePeriod> result = new ArrayList<>();
        if (samples.isEmpty()) {
            return result;
        }

        Posture currentPosture = postureSequence.getFirst();
        double startTime = transformTime(samples.getFirst().time());
        for (int i = 0; i < postureSequence.size(); i++) {
            var posture = postureSequence.get(i);
            if (posture != currentPosture || i == postureSequence.size() - 1) {
                double endTime = transformTime(samples.get(i).time());
                result.add(new PosturePeriod(startTime, endTime, currentPosture));

                currentPosture = posture;
                startTime = endTime;
            }
        }

        return result;
    }

    private static double transformTime(long time) {
        // TODO
        return (double) time;
    }
}
