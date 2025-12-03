package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Classifies stair climbing activity using barometric pressure data and step
 * detection.
 * Uses AltitudeAnalyser to detect altitude changes and generates stairs labels.
 * 
 * Singleton class for stairs classification.
 */
public class StairsClassifier {

    public record StairsPeriod(double startTime, double endTime, String stairsType) {
    }

    private static final Logger logger = LogManager.getLogger(StairsClassifier.class);
    private static StairsClassifier instance;

    private List<StairsPeriod> stairsTimeline;

    public static synchronized StairsClassifier getInstance() {
        if (instance == null)
            instance = new StairsClassifier();
        return instance;
    }

    private StairsClassifier() {
    }

    /**
     * Classify stairs activity from pressure data and step locations.
     * 
     * @param dataDir The data directory (not currently used, kept for API
     *                consistency)
     * @return Number of stairs periods detected, or 0 if classification failed
     */
    public int classify(File dataDir) {
        CurrentOpenData cod = CurrentOpenData.getInstance();

        // Check if required data exists
        if (!cod.channelExists("P_sc") || !cod.channelExists("T_sc")) {
            logger.warn("Pressure/temperature data NOT found, skipping stairs classification.");
            return 0;
        }

        if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")) {
            logger.warn("Acceleration data NOT found, skipping stairs classification.");
            return 0;
        }

        try {
            logger.info("Starting stairs classification.");

            // Load pressure and temperature data
            double[] pressure;
            double[] temperature;
            try (BinaryFile pressureFile = new BinaryFile("FILTP_sc");
                    BinaryFile temperatureFile = new BinaryFile("FILTT_sc")) {
                pressure = pressureFile.getDataRun();
                temperature = temperatureFile.getDataRun();
            }

            if (pressure == null || temperature == null) {
                logger.error("Failed to load pressure/temperature data.");
                return 0;
            }

            // Load accelerometer data for step detection
            double[] accelX;
            double[] accelY;
            double[] accelZ;
            try (BinaryFile mxrFile = new BinaryFile("MXR");
                    BinaryFile myrFile = new BinaryFile("MYR");
                    BinaryFile mzrFile = new BinaryFile("MZR")) {
                accelX = mxrFile.getDataRun();
                accelY = myrFile.getDataRun();
                accelZ = mzrFile.getDataRun();
            }

            if (accelX == null || accelY == null || accelZ == null) {
                logger.error("Failed to load accelerometer data.");
                return 0;
            }

            // Detect steps from accelerometer data
            int[] stepLocations = StepDetector.getInstance().detectSteps(
                    accelX, accelY, accelZ, 0L, null);

            if (stepLocations == null || stepLocations.length == 0) {
                logger.warn("No steps detected, skipping stairs classification.");
                return 0;
            }

            // Analyze altitude changes using AltitudeAnalyser
            AltitudeAnalyser.AltitudeAnalysisResult result = AltitudeAnalyser.getInstance()
                    .analyseAltitudeChange(pressure, stepLocations, 5);

            // Convert per-sample labels to 5-second epoch labels
            stairsTimeline = convertToStairsPeriods(result.labels, cod.getStartTimeInUS());

            // Cleanup: merge adjacent labels and remove short periods
            cleanupStairsLabels();

            logger.info("Stairs classification finished. Detected {} stairs periods.",
                    stairsTimeline == null ? 0 : stairsTimeline.size());

        } catch (Exception e) {
            logger.error("An error occurred during stairs classification: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }

        return stairsTimeline == null ? 0 : stairsTimeline.size();
    }

    /**
     * Convert per-sample (1000 Hz) stairs labels to 5-second epoch StairsPeriod
     * objects.
     * 
     * @param labels        Per-sample labels from AltitudeAnalyser
     * @param globalStartUS Global start time in microseconds
     * @return List of StairsPeriod objects
     */
    private List<StairsPeriod> convertToStairsPeriods(String[] labels, long globalStartUS) {
        List<StairsPeriod> periods = new ArrayList<>();

        if (labels == null || labels.length == 0) {
            return periods;
        }

        final int SAMPLES_PER_EPOCH = 5000; // 5 seconds at 1000 Hz
        final int sampleTimeMicros = 1000; // 1000 Hz = 1000 microseconds per sample

        // Group into 5-second epochs
        int numEpochs = (labels.length + SAMPLES_PER_EPOCH - 1) / SAMPLES_PER_EPOCH;

        for (int e = 0; e < numEpochs; e++) {
            long startSample = (long) e * SAMPLES_PER_EPOCH;
            long endSample = Math.min(labels.length, startSample + SAMPLES_PER_EPOCH) - 1;

            if (endSample < startSample) {
                continue;
            }

            // Get the dominant label in this epoch (simple: use first sample's label)
            String epochLabel = labels[(int) startSample];

            // Calculate timestamps
            long startUS = globalStartUS + (startSample * sampleTimeMicros / 1000);
            long endUS = globalStartUS + (endSample * sampleTimeMicros / 1000);

            periods.add(new StairsPeriod(startUS, endUS, epochLabel));
        }

        return periods;
    }

    /**
     * Cleanup stairs labels by:
     * 1. Removing periods shorter than 1.5 seconds
     * 2. Merging adjacent periods with the same label that are less than 1.5
     * seconds apart
     */
    private void cleanupStairsLabels() {
        if (stairsTimeline == null || stairsTimeline.isEmpty()) {
            return;
        }

        List<StairsPeriod> cleaned = new ArrayList<>();
        StairsPeriod current = null;

        for (StairsPeriod period : stairsTimeline) {
            // Skip periods shorter than 1.5 seconds
            if (period.endTime - period.startTime < 1500000) {
                continue;
            }

            if (current == null) {
                current = period;
            } else {
                // Check if we should merge with current
                boolean sameType = current.stairsType.equals(period.stairsType);
                boolean closeEnough = period.startTime < current.endTime + 1500000; // within 1.5s

                if (sameType && closeEnough) {
                    // Merge: extend current period to include this one
                    current = new StairsPeriod(current.startTime, period.endTime, current.stairsType);
                } else {
                    // Save current and start new one
                    cleaned.add(current);
                    current = period;
                }
            }
        }

        // Don't forget the last period
        if (current != null) {
            cleaned.add(current);
        }

        stairsTimeline = cleaned;
    }

    /**
     * Add stairs labels to the provided LabelSet.
     * 
     * @param labelSet The LabelSet to add stairs labels to
     */
    public void addStairsLabelsToLabelSet(LabelSet labelSet) {
        if (stairsTimeline != null && labelSet != null) {
            for (StairsPeriod period : stairsTimeline) {
                var label = AmsLabel.generateStairsLabel(period.startTime(), period.endTime(),
                        period.stairsType());
                labelSet.add(label);
            }
        }
    }

    /**
     * Get the stairs timeline (for debugging or further processing).
     * 
     * @return List of StairsPeriod objects, or null if classification hasn't run
     */
    public List<StairsPeriod> getStairsTimeline() {
        return stairsTimeline;
    }
}
