package nl.vu.psy.ams.suite.data.posture;

import nl.vu.psy.ams.suite.data.posture.SignalStatistics.Axis;

import static nl.vu.psy.ams.suite.data.posture.SignalStatistics.angleWithAxis;
import static nl.vu.psy.ams.suite.data.posture.SignalStatistics.correlation;

public class FeatureCalculator {

    private static final int epochSize = PosturePreprocessor.epochSize;

    private final PosturePreprocessor preprocessor;

    public FeatureCalculator(PosturePreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    void calculateForEpoch(int epochIndex) {
        int firstSampleIndex = epochSize * epochIndex;
        float[] features = preprocessor.features[epochIndex].data();

        calculateFeaturesBasedOnAcceleration(firstSampleIndex, features);
        calculateFeaturesBasedOnGyroscope(firstSampleIndex, features);
    }

    private void calculateFeaturesBasedOnAcceleration(int firstSampleIndex, float[] features) {
        // Features based on MXR
        {
            SignalStatistics mxrStats = new SignalStatistics(preprocessor.mxr, firstSampleIndex, epochSize);

            features[PostureFeature.MeanX.ordinal()] = (float) mxrStats.getMean();
            features[PostureFeature.StdX.ordinal()] = (float) mxrStats.getStandardDeviation();
            features[PostureFeature.VarianceX.ordinal()] = (float) mxrStats.getVariance();
            features[PostureFeature.MaximumX.ordinal()] = (float) mxrStats.getMaximum();
            features[PostureFeature.NormalizedMaximumX.ordinal()] = (float) mxrStats.getNormalizedMaximum();
            features[PostureFeature.MinimumX.ordinal()] = (float) mxrStats.getMinimum();
            features[PostureFeature.NormalizedMinimumX.ordinal()] = (float) mxrStats.getNormalizedMinimum();
            features[PostureFeature.RangeX.ordinal()] = (float) mxrStats.getRange();
            features[PostureFeature.KurtosisX.ordinal()] = (float) mxrStats.getKurtosis();
            features[PostureFeature.SkewnessX.ordinal()] = (float) mxrStats.getSkewness();
            features[PostureFeature.zcrX.ordinal()] = (float) mxrStats.getZeroCrossingRate();
        }

        // Features based on MYR
        {
            SignalStatistics myrStats = new SignalStatistics(preprocessor.myr, firstSampleIndex, epochSize);

            features[PostureFeature.MeanY.ordinal()] = (float) myrStats.getMean();
            features[PostureFeature.StdY.ordinal()] = (float) myrStats.getStandardDeviation();
            features[PostureFeature.VarianceY.ordinal()] = (float) myrStats.getVariance();
            features[PostureFeature.MaximumY.ordinal()] = (float) myrStats.getMaximum();
            features[PostureFeature.NormalizedMaximumY.ordinal()] = (float) myrStats.getNormalizedMaximum();
            features[PostureFeature.MinimumY.ordinal()] = (float) myrStats.getMinimum();
            features[PostureFeature.NormalizedMinimumY.ordinal()] = (float) myrStats.getNormalizedMinimum();
            features[PostureFeature.RangeY.ordinal()] = (float) myrStats.getRange();
            features[PostureFeature.KurtosisY.ordinal()] = (float) myrStats.getKurtosis();
            features[PostureFeature.SkewnessY.ordinal()] = (float) myrStats.getSkewness();
            features[PostureFeature.zcrY.ordinal()] = (float) myrStats.getZeroCrossingRate();
        }

        // Features based on MZR
        {
            SignalStatistics mzrStats = new SignalStatistics(preprocessor.mzr, firstSampleIndex, epochSize);

            features[PostureFeature.MeanZ.ordinal()] = (float) mzrStats.getMean();
            features[PostureFeature.StdZ.ordinal()] = (float) mzrStats.getStandardDeviation();
            features[PostureFeature.VarianceZ.ordinal()] = (float) mzrStats.getVariance();
            features[PostureFeature.MaximumZ.ordinal()] = (float) mzrStats.getMaximum();
            features[PostureFeature.NormalizedMaximumZ.ordinal()] = (float) mzrStats.getNormalizedMaximum();
            features[PostureFeature.MinimumZ.ordinal()] = (float) mzrStats.getMinimum();
            features[PostureFeature.NormalizedMinimumZ.ordinal()] = (float) mzrStats.getNormalizedMinimum();
            features[PostureFeature.RangeZ.ordinal()] = (float) mzrStats.getRange();
            features[PostureFeature.KurtosisZ.ordinal()] = (float) mzrStats.getKurtosis();
            features[PostureFeature.SkewnessZ.ordinal()] = (float) mzrStats.getSkewness();
            features[PostureFeature.zcrZ.ordinal()] = (float) mzrStats.getZeroCrossingRate();
        }

        // Features based on acceleration vector magnitude, VM
        {
            SignalStatistics vmStats = new SignalStatistics(preprocessor.vmGravCorr, firstSampleIndex, epochSize);

            features[PostureFeature.MeanVM.ordinal()] = (float) vmStats.getMean();
            features[PostureFeature.StdVM.ordinal()] = (float) vmStats.getStandardDeviation();
            features[PostureFeature.VarianceVM.ordinal()] = (float) vmStats.getVariance();
            features[PostureFeature.MaximumVM.ordinal()] = (float) vmStats.getMaximum();
            features[PostureFeature.NormalizedMaximumVM.ordinal()] = (float) vmStats.getNormalizedMaximum();
            features[PostureFeature.MinimumVM.ordinal()] = (float) vmStats.getMinimum();
            features[PostureFeature.NormalizedMinimumVM.ordinal()] = (float) vmStats.getNormalizedMinimum();
            features[PostureFeature.RangeVM.ordinal()] = (float) vmStats.getRange();
            features[PostureFeature.KurtosisVM.ordinal()] = (float) vmStats.getKurtosis();
            features[PostureFeature.SkewnessVM.ordinal()] = (float) vmStats.getSkewness();
        }

        // Mean values
        double meanX = features[PostureFeature.MeanX.ordinal()];
        double meanY = features[PostureFeature.MeanY.ordinal()];
        double meanZ = features[PostureFeature.MeanZ.ordinal()];

        // Features based on correlation of M_R
        {
            features[PostureFeature.CorrXY.ordinal()] =
                    (float) correlation(preprocessor.mxr, preprocessor.myr, meanX, meanY, firstSampleIndex, epochSize);
            features[PostureFeature.CorrXZ.ordinal()] =
                    (float) correlation(preprocessor.mxr, preprocessor.mzr, meanX, meanZ, firstSampleIndex, epochSize);
            features[PostureFeature.CorrYZ.ordinal()] =
                    (float) correlation(preprocessor.myr, preprocessor.mzr, meanY, meanZ, firstSampleIndex, epochSize);
        }

        // Features based on inclination of M_R
        {
            features[PostureFeature.InclinationX.ordinal()] = (float) angleWithAxis(Axis.X, meanX, meanY, meanZ);
            features[PostureFeature.InclinationY.ordinal()] = (float) angleWithAxis(Axis.Y, meanX, meanY, meanZ);
            features[PostureFeature.InclinationZ.ordinal()] = (float) angleWithAxis(Axis.Z, meanX, meanY, meanZ);
        }
    }

    private void calculateFeaturesBasedOnGyroscope(int firstSampleIndex, float[] features) {
        // Features based on gyroX
        {
            SignalStatistics gyroXStats = new SignalStatistics(preprocessor.gyroX, firstSampleIndex, epochSize);

            features[PostureFeature.MeanX_Gyro.ordinal()] = (float) gyroXStats.getMean();
            features[PostureFeature.StdX_Gyro.ordinal()] = (float) gyroXStats.getStandardDeviation();
            features[PostureFeature.VarianceX_Gyro.ordinal()] = (float) gyroXStats.getVariance();
            features[PostureFeature.MaximumX_Gyro.ordinal()] = (float) gyroXStats.getMaximum();
            features[PostureFeature.NormalizedMaximumX_Gyro.ordinal()] = (float) gyroXStats.getNormalizedMaximum();
            features[PostureFeature.MinimumX_Gyro.ordinal()] = (float) gyroXStats.getMinimum();
            features[PostureFeature.NormalizedMinimumX_Gyro.ordinal()] = (float) gyroXStats.getNormalizedMinimum();
            features[PostureFeature.RangeX_Gyro.ordinal()] = (float) gyroXStats.getRange();
            features[PostureFeature.KurtosisX_Gyro.ordinal()] = (float) gyroXStats.getKurtosis();
            features[PostureFeature.SkewnessX_Gyro.ordinal()] = (float) gyroXStats.getSkewness();
            features[PostureFeature.zcrX_Gyro.ordinal()] = (float) gyroXStats.getZeroCrossingRate();
        }

        // Features based on gyroY
        {
            SignalStatistics gyroYStats = new SignalStatistics(preprocessor.gyroY, firstSampleIndex, epochSize);

            features[PostureFeature.MeanY_Gyro.ordinal()] = (float) gyroYStats.getMean();
            features[PostureFeature.StdY_Gyro.ordinal()] = (float) gyroYStats.getStandardDeviation();
            features[PostureFeature.VarianceY_Gyro.ordinal()] = (float) gyroYStats.getVariance();
            features[PostureFeature.MaximumY_Gyro.ordinal()] = (float) gyroYStats.getMaximum();
            features[PostureFeature.NormalizedMaximumY_Gyro.ordinal()] = (float) gyroYStats.getNormalizedMaximum();
            features[PostureFeature.MinimumY_Gyro.ordinal()] = (float) gyroYStats.getMinimum();
            features[PostureFeature.NormalizedMinimumY_Gyro.ordinal()] = (float) gyroYStats.getNormalizedMinimum();
            features[PostureFeature.RangeY_Gyro.ordinal()] = (float) gyroYStats.getRange();
            features[PostureFeature.KurtosisY_Gyro.ordinal()] = (float) gyroYStats.getKurtosis();
            features[PostureFeature.SkewnessY_Gyro.ordinal()] = (float) gyroYStats.getSkewness();
            features[PostureFeature.zcrY_Gyro.ordinal()] = (float) gyroYStats.getZeroCrossingRate();
        }

        // Features based on gyroZ
        {
            SignalStatistics gyroZStats = new SignalStatistics(preprocessor.gyroZ, firstSampleIndex, epochSize);

            features[PostureFeature.MeanZ_Gyro.ordinal()] = (float) gyroZStats.getMean();
            features[PostureFeature.StdZ_Gyro.ordinal()] = (float) gyroZStats.getStandardDeviation();
            features[PostureFeature.VarianceZ_Gyro.ordinal()] = (float) gyroZStats.getVariance();
            features[PostureFeature.MaximumZ_Gyro.ordinal()] = (float) gyroZStats.getMaximum();
            features[PostureFeature.NormalizedMaximumZ_Gyro.ordinal()] = (float) gyroZStats.getNormalizedMaximum();
            features[PostureFeature.MinimumZ_Gyro.ordinal()] = (float) gyroZStats.getMinimum();
            features[PostureFeature.NormalizedMinimumZ_Gyro.ordinal()] = (float) gyroZStats.getNormalizedMinimum();
            features[PostureFeature.RangeZ_Gyro.ordinal()] = (float) gyroZStats.getRange();
            features[PostureFeature.KurtosisZ_Gyro.ordinal()] = (float) gyroZStats.getKurtosis();
            features[PostureFeature.SkewnessZ_Gyro.ordinal()] = (float) gyroZStats.getSkewness();
            features[PostureFeature.zcrZ_Gyro.ordinal()] = (float) gyroZStats.getZeroCrossingRate();
        }

        // Features based on gyroscope vector magnitude, vmGyro
        {
            SignalStatistics vmGyroStats = new SignalStatistics(preprocessor.vmGyro, firstSampleIndex, epochSize);

            features[PostureFeature.MeanVM_Gyro.ordinal()] = (float) vmGyroStats.getMean();
            features[PostureFeature.StdVM_Gyro.ordinal()] = (float) vmGyroStats.getStandardDeviation();
            features[PostureFeature.VarianceVM_Gyro.ordinal()] = (float) vmGyroStats.getVariance();
            features[PostureFeature.MaximumVM_Gyro.ordinal()] = (float) vmGyroStats.getMaximum();
            features[PostureFeature.NormalizedMaximumVM_Gyro.ordinal()] = (float) vmGyroStats.getNormalizedMaximum();
            features[PostureFeature.MinimumVM_Gyro.ordinal()] = (float) vmGyroStats.getMinimum();
            features[PostureFeature.NormalizedMinimumVM_Gyro.ordinal()] = (float) vmGyroStats.getNormalizedMinimum();
            features[PostureFeature.RangeVM_Gyro.ordinal()] = (float) vmGyroStats.getRange();
            features[PostureFeature.KurtosisVM_Gyro.ordinal()] = (float) vmGyroStats.getKurtosis();
            features[PostureFeature.SkewnessVM_Gyro.ordinal()] = (float) vmGyroStats.getSkewness();
        }

        // Mean gyro values
        double meanXGyro = features[PostureFeature.MeanX_Gyro.ordinal()];
        double meanYGyro = features[PostureFeature.MeanY_Gyro.ordinal()];
        double meanZGyro = features[PostureFeature.MeanZ_Gyro.ordinal()];

        // Features based on correlation of gyro_
        {
            features[PostureFeature.CorrXY_Gyro.ordinal()] =
                    (float) correlation(preprocessor.gyroX, preprocessor.gyroY, meanXGyro, meanYGyro,
                            firstSampleIndex, epochSize);
            features[PostureFeature.CorrXZ_Gyro.ordinal()] =
                    (float) correlation(preprocessor.gyroX, preprocessor.gyroZ, meanXGyro, meanZGyro,
                            firstSampleIndex, epochSize);
            features[PostureFeature.CorrYZ_Gyro.ordinal()] =
                    (float) correlation(preprocessor.gyroY, preprocessor.gyroZ, meanYGyro, meanZGyro,
                            firstSampleIndex, epochSize);
        }

        // Features based on inclination of gyro_
        {
            features[PostureFeature.InclinationX_Gyro.ordinal()] =
                    (float) angleWithAxis(Axis.X, meanXGyro, meanYGyro, meanZGyro);
            features[PostureFeature.InclinationY_Gyro.ordinal()] =
                    (float) angleWithAxis(Axis.Y, meanXGyro, meanYGyro, meanZGyro);
            features[PostureFeature.InclinationZ_Gyro.ordinal()] =
                    (float) angleWithAxis(Axis.Z, meanXGyro, meanYGyro, meanZGyro);
        }
    }
}
