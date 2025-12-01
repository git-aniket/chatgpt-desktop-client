package nl.vu.psy.ams.suite.data.motility;

import java.util.List;
import java.util.SortedSet;

import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.structures.Step;

public interface StepDetector {
    public SortedSet<Step> findSteps(double leftTime, double rightTime);

    public List<Double> findSteps(double[] data, double[] ticks);

    public void setMonitor(ProgressMonitor mon);

}
