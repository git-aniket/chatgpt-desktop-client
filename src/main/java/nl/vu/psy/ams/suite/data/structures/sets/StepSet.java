package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.motility.StepDetector;
import nl.vu.psy.ams.suite.data.motility.StepShapeDetector;
import nl.vu.psy.ams.suite.data.structures.Step;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

public class StepSet {

	public TreeSet<Step> steps = new TreeSet<Step>();

	public void addStep(double time) {
		Step newStep = new Step(time);
		if (steps.contains(newStep)) {
			newStep = new Step(time - 1);
		}
		steps.add(newStep);
		updateStepFeatures(newStep.getTime());
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void addStep(Step o) {
		addStep(o, true);
	}

	public void addStep(Step o, boolean setDirty) {
		steps.add(o);
		if (setDirty)
			CurrentOpenData.getInstance().setDirty(true);
	}

	public void changeStep(double oldTime, double newTime) {
		removeStep(oldTime);
		addStep(newTime);
	}

	public Step getStepAfterTime(double time) {
		return getNextStep(new Step(time));
	}

	public Step getStepBeforeTime(double time) {
		return getPrevStep(new Step(time));
	}

	public SortedSet<Step> getStepBetweenTimes(double ltime, double rtime) {
		return steps.subSet(new Step(ltime), new Step(rtime));
	}

	public Step getStepClosestToTime(double time) {
		Step myStep = new Step(time);
		Step lPoint = steps.floor(myStep);
		Step rPoint = steps.ceiling(myStep);
		double ldiff = Double.MAX_VALUE;
		if (lPoint != null)
			ldiff = time - lPoint.getTime();
		double rdiff = Double.MAX_VALUE;
		if (rPoint != null)
			rdiff = rPoint.getTime() - time;
		if (ldiff < rdiff) {
			return lPoint;
		} else {
			return rPoint;
		}
	}

	public SortedSet<Step> getSteps() {
		return steps;
	}

	public double[] getDataBetweenTimes(double lTime, double rTime) {
		Step rStep = getStepBeforeTime(rTime);
		Step lStep = getStepAfterTime(lTime);
		if (rStep == null || lStep == null || lStep.getTime() > rStep.getTime()) // no peaks between times
			return null;
		ArrayList<Step> tStep = new ArrayList<Step>(steps.subSet(lStep, true, rStep, true));
		ArrayList<Double> times = new ArrayList<Double>();
		int n = tStep.size();
		for (int i = 0; i < n; i++) {
			times.add(tStep.get(i).getTime());
		}
		int nSamples = times.size();
		if (nSamples == 0)
			return null;
		double[] ret = new double[nSamples];
		for (int i = 0; i < nSamples; i++) {
			ret[i] = times.get(i) / 1000000;
		}
		return ret;
	}

	public Step getNextStep(Step step) {
		if (step == null)
			return null;
		NavigableSet<Step> tailSet = steps.tailSet(step, false);
		if (tailSet.isEmpty())
			return null;
		return tailSet.first();
	}

	public Step getPrevStep(Step step) {
		if (step == null)
			return null;
		NavigableSet<Step> headSet = steps.headSet(step, false);
		if (headSet.isEmpty())
			return null;
		return headSet.last();
	}

	public void updateStepFeatures(double time) {
		Step s = getStepClosestToTime(time);
		BinaryFile bf = new BinaryFile("FILTAccelVectorMag");
		s.setImpactValue(bf.getMaximumAfterTime(time));
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(getSteps()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSteps(SortedSet<Step> newSteps, double leftTime, double rightTime) {
		steps.removeAll(steps.subSet(new Step(leftTime), new Step(rightTime)));
		steps.addAll(newSteps);
		CurrentOpenData.getInstance().setDirty(true);
	}

	public SortedSet<Step> subSet(double lTime, double rTime) {
		return steps.subSet(new Step(lTime), new Step(rTime));
	}

	public boolean contains(double fromTime, double toTime) {
		return (steps.subSet(new Step(fromTime), new Step(toTime)).isEmpty() == false);
	}

	public void removeStep(double time) {
		if (steps.contains(new Step(time))) {
			Step myStep = getStepClosestToTime(time);
			steps.remove(myStep);
		}
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void reCalculate(String ecgFile) {
		Timer timer = new Timer();
		timer.start();
		steps.clear();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		long endTimeInUS = CurrentOpenData.getInstance().getEndTimeInUS();

		StepDetector st;
		SortedSet<Step> tempSteps;

		st = new StepShapeDetector();
		// qrs.setMonitor(mon);
		tempSteps = st.findSteps(startTime, endTimeInUS);
		steps.addAll(tempSteps);
		// mon.close();
		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("StepSet recalculate took (" + timer.getTime() / 1000. + " sec)");
	}
}
