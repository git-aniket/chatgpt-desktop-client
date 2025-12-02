package nl.vu.psy.ams.suite.data.posture;

import org.apache.commons.math3.util.FastMath;
import com.github.psambit9791.jdsp.signal.Decimate;
import nl.vu.psy.ams.suite.data.CurrentOpenData;

/**
 * StepDetector - Singleton class for detecting steps from accelerometer data.
 * 
 * This class uses phase-based step detection with anti-aliasing filtering,
 * decimation, and band-pass filtering to accurately identify steps from
 * raw acceleration data.
 * 
 * @author
 */
public class StepDetector {

    private static StepDetector instance;
    private static final int SAMPLING_FREQUENCY = 1000; // Hz

    /**
     * Get the singleton instance of StepDetector.
     * 
     * @return StepDetector instance
     */
    public static synchronized StepDetector getInstance() {
        if (instance == null) {
            instance = new StepDetector();
        }
        return instance;
    }

    private StepDetector() {
        // Private constructor for singleton pattern
    }

    /**
     * Detect steps in the given accelerometer data.
     * 
     * @param accelX              X-axis acceleration in m/s²
     * @param accelY              Y-axis acceleration in m/s²
     * @param accelZ              Z-axis acceleration in m/s²
     * @param startSampleAbsolute Absolute sample index where this data starts
     * @param tickCorrector       Function to correct for ticks (can be null)
     * @return Array of step locations (1 = step, 0 = no step)
     */
    public int[] detectSteps(double[] accelX, double[] accelY, double[] accelZ, long startSampleAbsolute,
            TickCorrector tickCorrector) {
        if (accelX == null || accelY == null || accelZ == null) {
            throw new IllegalArgumentException("Input arrays cannot be null");
        }

        // --- Constants & Parameters ---
        final int FS = SAMPLING_FREQUENCY; // 1000 Hz
        final int DECIMATE_FACTOR = 100; // 1000 -> 10 Hz
        final int FS_DEC_INT = FS / DECIMATE_FACTOR; // 10 Hz
        final double FS_DEC = (double) FS_DEC_INT;

        // Step detection tuning
        final double VECTOR_MAGNITUDE_THRESHOLD = 0.06; // m/s^2
        final double PHASE_RATE_THRESHOLD = 90.0; // deg/s
        final long MIN_STEP_INTERVAL_US = 400_000; // μs (hard minimum time gap between steps)

        // 1) PRE-DECIMATION ANTI-ALIAS LPF @ 1000 Hz (zero-phase)
        final int AA_ORDER = 5;
        final double AA_CUTOFF = 4.5; // < 5 Hz (new Nyquist after /100)

        // Use the unified zero-phase low-pass helper instead of manual fwd/bwd passes
        double[] aaX = ZeroPhaseFilter.zeroPhaseFilterLowPassFilterJDSP(accelX, AA_ORDER, AA_CUTOFF, FS);
        double[] aaY = ZeroPhaseFilter.zeroPhaseFilterLowPassFilterJDSP(accelY, AA_ORDER, AA_CUTOFF, FS);
        double[] aaZ = ZeroPhaseFilter.zeroPhaseFilterLowPassFilterJDSP(accelZ, AA_ORDER, AA_CUTOFF, FS);

        // 2) DECIMATE anti-aliased signals
        Decimate decX = new Decimate(aaX, FS, true);
        double[] dx = decX.decimate(DECIMATE_FACTOR);
        Decimate decY = new Decimate(aaY, FS, true);
        double[] dy = decY.decimate(DECIMATE_FACTOR);
        Decimate decZ = new Decimate(aaZ, FS, true);
        double[] dz = decZ.decimate(DECIMATE_FACTOR);

        // 3) BAND-PASS @ 10 Hz (analysis filter moved post-decimation)
        final int BP_ORDER = 10;
        double[] fdx = ZeroPhaseFilter.zeroPhaseBandPassFilterJDSP(dx, BP_ORDER, 0.4, 2.8, FS_DEC_INT);
        double[] fdy = ZeroPhaseFilter.zeroPhaseBandPassFilterJDSP(dy, BP_ORDER, 0.4, 2.8, FS_DEC_INT);
        double[] fdz = ZeroPhaseFilter.zeroPhaseBandPassFilterJDSP(dz, BP_ORDER, 0.4, 2.8, FS_DEC_INT);

        // 4) STEP DETECTION on the decimated signals (robust peak-based with
        // phase-based thresholding)
        // Compute resultant magnitude at decimated rate
        double[] r = new double[fdx.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = FastMath.sqrt(fdx[i] * fdx[i] + fdy[i] * fdy[i] + fdz[i] * fdz[i]);
        }

        // --- Phase-based features (computed once at the decimated rate) ---
        // Use the band-passed horizontal plane (X,Y) to compute instantaneous phase
        // Phase here is atan2(Y, X) on the band-passed signals, unwrapped for
        // continuity.
        double[] phase = new double[fdx.length];
        for (int i = 0; i < phase.length; i++) {
            phase[i] = FastMath.atan2(fdy[i], fdx[i]); // radians
        }
        // Unwrap phase to avoid 2π discontinuities
        for (int i = 1; i < phase.length; i++) {
            double dp = phase[i] - phase[i - 1];
            if (dp > FastMath.PI) {
                phase[i] -= 2 * FastMath.PI;
            } else if (dp < -FastMath.PI) {
                phase[i] += 2 * FastMath.PI;
            }
        }
        // Phase rate (angular velocity) in deg/s at the decimated rate
        double[] phaseRateDeg = new double[Math.max(0, phase.length - 1)];
        for (int i = 1; i < phase.length; i++) {
            double dphi = phase[i] - phase[i - 1]; // radians/sample
            double omega = dphi * FS_DEC * 180.0 / Math.PI; // deg/s
            phaseRateDeg[i - 1] = omega;
        }

        // Cadence guard at decimated rate (~10 Hz) to reduce double-counting; final 250
        // ms check uses timestamps
        int minSamplesBetween = (int) Math.round(0.25 * FS_DEC); // ~2–3 samples (≈250 ms)

        // --- Chunking & gating policy ---
        // We analyze decimated data in 5-second chunks (10 Hz × 5 s = 50 samples).
        // If within a chunk the band-passed X acceleration crosses ±0.5 m/s² even once,
        // we run phase-based step detection on the entire chunk; otherwise the chunk is
        // skipped.
        final int CHUNK_SAMPLES = 5 * FS_DEC_INT; // 5 s at 10 Hz => 50 samples
        final double X_GATE_ABS_THRESHOLD = 0.6; // m/s^2 on decimated X (band-passed)

        int[] step_locations = new int[accelX.length];
        java.util.Arrays.fill(step_locations, 0);

        long startUS = CurrentOpenData.getInstance().getStartTimeInUS();

        long lastAcceptedStepUS = -1L; // enforce 250 ms across chunk boundaries

        for (int chunkStart = 0; chunkStart < r.length; chunkStart += CHUNK_SAMPLES) {
            int chunkEnd = Math.min(r.length, chunkStart + CHUNK_SAMPLES);

            // Gating: check whether |fdx| (band-passed X) exceeds threshold anywhere in
            // this 5s chunk
            boolean analyzeThisChunk = false;
            for (int j = chunkStart; j < chunkEnd; j++) {
                if (Math.abs(fdx[j]) >= X_GATE_ABS_THRESHOLD) { // use band-passed decimated X
                    analyzeThisChunk = true;
                    break;
                }
            }
            if (!analyzeThisChunk) {
                continue; // skip quiet chunk entirely
            }

            // Reset cadence guard per chunk
            int lastStepIdxDec = -1000000;

            for (int i = Math.max(chunkStart + 1, 1); i < Math.min(chunkEnd - 1, r.length - 1); i++) {
                // --- Phase-based step candidate ---
                // 1) Use a simple local-peak on resultant r to identify candidate timing
                boolean isLocalPeak = r[i] > r[i - 1] && r[i] >= r[i + 1];
                if (!isLocalPeak)
                    continue;

                // 2) Phase-rate gate: require sufficiently fast rotation in the horizontal
                // plane
                // phaseRateDeg is indexed at i-1 (derived from phase differences)
                double pr = (i - 1 >= 0 && i - 1 < phaseRateDeg.length) ? Math.abs(phaseRateDeg[i - 1]) : 0.0;
                if (pr < PHASE_RATE_THRESHOLD)
                    continue;

                // 3) Cadence guard within this chunk
                if (i - lastStepIdxDec < minSamplesBetween)
                    continue;

                // Map decimated index -> original sample index
                int origIdx = i * DECIMATE_FACTOR;
                // Convert decimated index back to original 1000 Hz index, then add absolute
                // offset
                long offsetSamples = origIdx + startSampleAbsolute;
                if (tickCorrector != null) {
                    offsetSamples = tickCorrector.correctForTicks(offsetSamples);
                }
                long curTimeUS = startUS
                        + (offsetSamples - CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms())
                                * 1000L;

                // Final magnitude sanity check at full rate using a tiny window around the
                // mapped index
                int winFull = Math.max(1, (int) Math.round(0.05 * FS)); // ~50 ms at 1000 Hz
                int s0 = Math.max(0, origIdx - winFull);
                int s1 = Math.min(accelX.length - 1, origIdx + winFull);
                double maxResFull = 0.0;
                for (int j = s0; j <= s1; j++) {
                    double res = Math.sqrt(accelX[j] * accelX[j] + accelY[j] * accelY[j] + accelZ[j] * accelZ[j]);
                    if (res > maxResFull)
                        maxResFull = res;
                }
                if (maxResFull < VECTOR_MAGNITUDE_THRESHOLD)
                    continue;

                // Enforce hard minimum time gap of 250 ms between steps (global)
                if (lastAcceptedStepUS > 0 && (curTimeUS - lastAcceptedStepUS) < MIN_STEP_INTERVAL_US) {
                    continue; // too close to previous accepted step
                }

                // Accept step
                step_locations[origIdx] = 1;
                CurrentOpenData.getInstance().getStepSet().addStep(curTimeUS);
                lastStepIdxDec = i;
                lastAcceptedStepUS = curTimeUS;
            }
        }

        return step_locations;
    }

    /**
     * Functional interface for tick correction.
     */
    @FunctionalInterface
    public interface TickCorrector {
        long correctForTicks(long offset);
    }
}
