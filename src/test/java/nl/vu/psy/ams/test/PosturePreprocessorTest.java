package nl.vu.psy.ams.test;

import nl.vu.psy.ams.suite.data.posture.PostureFeature;
import nl.vu.psy.ams.suite.data.posture.PosturePreprocessor;
import nl.vu.psy.ams.suite.data.structures.Sample;
import nl.vu.psy.ams.suite.tools.DataSetGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PosturePreprocessorTest {
    final static String rawMotionFile = "/datasets/motion.423020.raw.csv";
    final static String featuresFile = "/datasets/motion.423020.features.csv";

    @Test
    void testAccuracy() throws IOException {
        test(false);
    }

    @Test
    void testSpeed() throws IOException {
        test(true);
    }

    public static void test(boolean oneDay) throws IOException {
        // Load data set
        var rawMotionGenerator = new DataSetGenerator(rawMotionFile);
        var featuresGenerator = new DataSetGenerator(featuresFile, 0, 6_000_000);
        List<float[]> motion;
        List<Sample> references;

        if (oneDay) {
            motion = rawMotionGenerator.generateCsvData(86_400_000);
            references = featuresGenerator.generate(14_400);
        } else {
            motion = rawMotionGenerator.generateCsvData();
            references = featuresGenerator.generate();
        }

        final int numRows = motion.size();
        final int numCols = motion.getFirst().length;
        final int referenceNumRows = references.size();
        final int referenceNumCols = references.getFirst().data().length;
        if (!oneDay) {
            System.out.print("Sample: " + rawMotionFile);
            System.out.println(" [" + numCols + "x" + numRows + "]");
            System.out.print("Reference: " + featuresFile);
            System.out.println(" [" + referenceNumCols + "x" + referenceNumRows + "]");
        }

        // Validate
        assert numCols == 6;
        assert referenceNumCols == 98;
        assert numRows >= referenceNumRows * 6000;

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
        var result = preprocessor.preprocess(0, 1_000);
        List<Sample> features = result.samples();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\nPreprocessor sample size: " + numRows);
        System.out.println("Preprocess time millisec: " + duration);

        if (!oneDay) {
            int errors = 0;
            double percentErrorLimit = 0.02;
            double avgPercentError = 0;
            for (int epoch = 0; epoch < references.size(); epoch++) {
                float[] reference = references.get(epoch).data();
                float[] feature = features.get(epoch).data();
                for (int i = 0; i < reference.length; i++) {
                    String featName = PostureFeature.values()[i].name();
                    double percentDiff = 100 * feature[i] / reference[i] - 100;
                    avgPercentError += Math.abs(percentDiff);
                    if (Math.abs(percentDiff) >= percentErrorLimit) {
                        errors++;
                        System.out.printf("%nEpoch %d%n", epoch);
                        System.out.printf("%s: \t%f \t[ %f ] \tDiff: %f %% %n",
                                featName, feature[i], reference[i], percentDiff);
                    }
                }
            }
            avgPercentError /= (references.size() * references.getFirst().data().length);
            System.out.printf("%nErrors (diff >= %.2f%%): %d", percentErrorLimit, errors);
            System.out.printf("%nAverage abs difference: %.6f %%%n", avgPercentError);

            assertThat(errors).isZero();
            assertThat(avgPercentError).isLessThan(0.00001);
        }
    }
}
