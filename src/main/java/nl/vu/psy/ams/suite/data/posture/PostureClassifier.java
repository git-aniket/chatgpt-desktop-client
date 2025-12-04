package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PostureClassifier {

    public record PosturePeriod(double startTime, double endTime, Posture posture) {
    }

    private static final Logger logger = LogManager.getLogger(PostureClassifier.class);
    private static PostureClassifier instance;

    private List<PosturePeriod> postureTimeline;

    public static synchronized PostureClassifier getInstance() {
        if (instance == null)
            instance = new PostureClassifier();
        return instance;
    }

    private PostureClassifier() {
    }

    public int classify(File dataDir) {
        CurrentOpenData cod = CurrentOpenData.getInstance();

        // Check accel data exists
        if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")) {
            logger.warn("Acceleration data NOT found, skipping posture classification.");
            return 0;
        }

        // Check gyro data exists
        if (!cod.channelExists("GyroX") || !cod.channelExists("GyroY") || !cod.channelExists("GyroZ")) {
            logger.warn("Gyroscope data NOT found, skipping posture classification.");
            return 0;
        }

        // Load motion data using MotionDataLoader
        var motionDataLoader = new MotionDataLoader();
        var motionData = motionDataLoader.loadData();

        if (motionData == null) {
            logger.error("Failed to load motion data, skipping posture classification.");
            return 0;
        }

        try {
            // Do posture classification
            logger.info("Starting posture classification.");
            logger.info("Data folder: {}", dataDir.getAbsolutePath());
            var preprocessor = PosturePreprocessor.getInstance();
            preprocessor.setAccelSamples(motionData.mxr(), motionData.myr(), motionData.mzr());
            preprocessor.setGyroSamples(motionData.gyroX(), motionData.gyroY(), motionData.gyroZ());

            long startTimeMicros = cod.getStartTimeInUS();
            int sampleTimeMicros = motionData.sampleTimeMicros();
            var preprocessingResult = preprocessor.preprocess(startTimeMicros, sampleTimeMicros);

            // To predict based on a feature dataset (not on preprocessor), use this:
            // var featuresFile = "/datasets/posture.98x1000.features.csv";
            // var dataSetGenerator = new DataSetGenerator(featuresFile, 0, 6);
            // features = dataSetGenerator.generate();

            // Make prediction
            postureTimeline = PosturePredictor.predict(preprocessingResult.samples(),
                    preprocessingResult.epochDurationMicros());
        } catch (Exception e) {
            logger.error("An error occurred during posture classification: {}", e.getMessage());
            return 0;
        }
        logger.info("Posture classification has finished.");
        return postureTimeline == null ? 0 : postureTimeline.size();
    }

    public void addPostureLabelsToLabelSet(LabelSet labelSet) {
        if (postureTimeline != null && labelSet != null) {
            for (PosturePeriod period : postureTimeline) {
                var label = AmsLabel.generatePostureLabel(period.startTime(), period.endTime(), period.posture());
                labelSet.add(label);
            }
        }
    }

    /**
     * Get posture labels as a timeline for use by ActivityClassification.
     * Creates a complete timeline from recording start with one label per 6-second
     * interval.
     * Each index i in the returned list represents the posture at time interval i *
     * 6 seconds.
     * 
     * @return List of posture label strings at 6-second intervals from recording
     *         start
     */
    public List<String> getPostureLabelsForTimeline() {
        List<String> labels = new ArrayList<>();
        if (postureTimeline == null || postureTimeline.isEmpty()) {
            return labels;
        }

        // Get recording duration from the last posture period
        PosturePeriod lastPeriod = postureTimeline.get(postureTimeline.size() - 1);
        long recordingEndMicros = (long) lastPeriod.endTime();

        // Calculate number of 6-second intervals needed
        final long INTERVAL_MICROS = 6000000; // 6 seconds in microseconds
        int numIntervals = (int) ((recordingEndMicros + INTERVAL_MICROS - 1) / INTERVAL_MICROS);

        // Initialize all intervals as "Unknown"
        for (int i = 0; i < numIntervals; i++) {
            labels.add("Unknown");
        }

        // Fill in the actual postures
        for (PosturePeriod period : postureTimeline) {
            long startMicros = (long) period.startTime();
            long endMicros = (long) period.endTime();
            String postureName = period.posture().toString();

            // Calculate which intervals this period covers
            int startInterval = (int) (startMicros / INTERVAL_MICROS);
            int endInterval = (int) (endMicros / INTERVAL_MICROS);

            // Set the posture for all intervals in this period
            for (int i = startInterval; i < endInterval && i < numIntervals; i++) {
                labels.set(i, postureName);
            }
        }

        return labels;
    }
}
