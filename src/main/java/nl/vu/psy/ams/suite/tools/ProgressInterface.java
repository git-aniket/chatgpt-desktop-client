package nl.vu.psy.ams.suite.tools;

public interface ProgressInterface {
    /**
     * Passes the progress of between 0-1
     */
    public void progressUpdated(double progress);
}