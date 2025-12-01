package nl.vu.psy.ams.suite.data.structures.sets;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.sclcycle.SCLDetector;
import nl.vu.psy.ams.suite.data.sclcycle.SCLShapeDetector;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.SCLCycle;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ProgressInterface;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SkinConductanceSet implements PropertyChangeListener {

	private TreeSet<SCLCycle> sclcycle = new TreeSet<SCLCycle>();
	private int changeCount = 0;
	// private LabelSet lSet;
	private AmsLabelConfiguration lconf;
	public int lcode;
	public String lvname;
	ProgressMonitor mon;
	// protected SortedSet<SCLCycle> scycle = new TreeSet<SCLCycle>();

	public void addCycle(double time) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		SCLArtefactSet arts = cod.getSCLArtefacts();
		if (arts.isTimeUnderLabel(time))
			return;
		SCLCycle newCycle = new SCLCycle(time);
		if (sclcycle.contains(newCycle)) {
			newCycle = new SCLCycle(time - 1);
		}
		SCLCycle nextCycle = getSCLCycleAfterTime(newCycle.getTime());
		if (nextCycle != null) {
			AmsLabel art = arts.getLabelAfterTime(nextCycle.getTime());
			if (art != null) {
				if (art.getLeftTime() > nextCycle.getTime()) {

				}
			} else {

			}
		}
		sclcycle.add(newCycle);
		updateSCLFeatures(newCycle.getTime());
		cod.setDirty(true);
		changeCount++;
	}

	public void addCycle(SCLCycle o) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		sclcycle.add(o);
		cod.setDirty(true);
		changeCount++;
	}

	public SCLCycle getSCLCycleAfterTime(double time) {
		return getNextCycle(new SCLCycle(time));
	}

	public SCLCycle getSCLCycleBeforeTime(double time) {
		return getPrevCycle(new SCLCycle(time));
	}

	public SortedSet<SCLCycle> getCycleBetweenTimes(double ltime, double rtime) {
		return sclcycle.subSet(new SCLCycle(ltime), new SCLCycle(rtime));
	}

	public SCLCycle getCycleClosestToTime(double time) {
		SCLCycle myCycle = new SCLCycle(time);
		SCLCycle lPoint = sclcycle.floor(myCycle);
		SCLCycle rPoint = sclcycle.ceiling(myCycle);
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

	public SortedSet<SCLCycle> getSCLCycles() {
		return sclcycle;
	}

	public int getChangeCount() {
		return changeCount;
	}

	public double[] getDataBetweenTimes(double lTime, double rTime) {
		SCLCycle rCycle = getSCLCycleBeforeTime(rTime);
		SCLCycle lCycle = getSCLCycleAfterTime(lTime);
		if (rCycle == null || lCycle == null || lCycle.getTime() > rCycle.getTime()) // no peaks between times
			return null;
		ArrayList<SCLCycle> tCycle = new ArrayList<SCLCycle>(sclcycle.subSet(lCycle, true, rCycle, true));
		ArrayList<Double> times = new ArrayList<Double>();
		int n = tCycle.size();
		for (int i = 0; i < n; i++) {
			times.add(tCycle.get(i).getTime());
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

	public SCLCycle getNextCycle(SCLCycle scl) {
		if (scl == null)
			return null;
		NavigableSet<SCLCycle> tailSet = sclcycle.tailSet(scl, false);
		if (tailSet.isEmpty())
			return null;
		return tailSet.first();
	}

	public SCLCycle getPrevCycle(SCLCycle Cycle) {
		if (sclcycle == null)
			return null;
		NavigableSet<SCLCycle> headSet = sclcycle.headSet(Cycle, false);
		if (headSet.isEmpty())
			return null;
		return headSet.last();
	}

	public double[] getSmoothedDataBetweenTimes(double lTime, double rTime) {
		SCLCycle rCycle = getSCLCycleBeforeTime(rTime);
		SCLCycle lCycle = getSCLCycleAfterTime(lTime);
		if (rCycle == null || lCycle == null || lCycle.getTime() > rCycle.getTime()) // no peaks between times
			return null;
		ArrayList<SCLCycle> tCycle = new ArrayList<SCLCycle>(sclcycle.subSet(lCycle, true, rCycle, true));
		ArrayList<Double> times = new ArrayList<Double>();
		int n = tCycle.size();
		for (int i = 0; i < n; i++) {
			times.add(tCycle.get(i).getTime());
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

	public void reCalculate() {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		Timer timer = new Timer();
		timer.start();
		TreeSet<SCLCycle> sclcycleTemp = new TreeSet<SCLCycle>();

		long startTime = cod.getStartTimeInUS();
		long endTimeInUS = cod.getEndTimeInUS();
		String SCLFile;
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7)
			SCLFile = "FILTSCL.dbin";
		else
			SCLFile = "FILTSCL.bin";

		SCLArtefactSet sclArtefacts = cod.getSCLArtefacts();
		int lTimeInS = (int) (startTime / 1000000); // time in seconds
		int rTimeInS = (int) (endTimeInUS / 1000000);
		JFrame frame = MainFrame.getInstance().getMainFrame();
		mon = new ProgressMonitor(frame, "Detecting SCL Peaks...", null, 0, 100); // lTimeInS, rTimeInS);
		class Task extends SwingWorker<Void, Void> {
			@Override
			public Void doInBackground() {
				setProgress(0);
				double curTime = startTime;
				SCLDetector scl;
				SortedSet<SCLCycle> tempCycles;
				int sampleTimeInUS = (int) cod.getFileHeader().getDwSampleTime_us();
				try {
					Ams7fsChannelInfo s = cod.getChannelInfoFromID("SCL");
					sampleTimeInUS *= s.getDwDivider();
				} catch (Exception e) {
					e.printStackTrace();
				}

				for (AmsLabel art : sclArtefacts.getLabels()) {

					scl = new SCLShapeDetector(cod.getStartTimeInUS(), sampleTimeInUS,
							new File(cod.getFilePath(), SCLFile));
					// scl.setMonitor(mon);
					tempCycles = scl.findCycle(curTime, art.getLeftTime(), new ProgressInterface() {
						public void progressUpdated(double curTime) {
							double progress = (curTime - lTimeInS) / (rTimeInS - lTimeInS);
							if (progress > 1)
								progress = 1;
							// System.out.println(progress);
							setProgress((int) (progress * 100));
						}
					});
					sclcycleTemp.addAll(tempCycles);
					curTime = art.getRightTime();
				}
				scl = new SCLShapeDetector(cod.getStartTimeInUS(), sampleTimeInUS,
						new File(cod.getFilePath(), SCLFile));
				// scl.setMonitor(mon);
				tempCycles = scl.findCycle(curTime, endTimeInUS, new ProgressInterface() {
					public void progressUpdated(double curTime) {
						double progress = (curTime - lTimeInS) / (rTimeInS - lTimeInS);
						if (progress > 1)
							progress = 1;
						// System.out.println(progress);
						setProgress((int) (progress * 100));
					}
				});
				sclcycleTemp.addAll(tempCycles);
				sclcycle.clear();
				sclcycle.addAll(sclcycleTemp);
				return null;
			}

			@Override
			public void done() {
				mon.close();
			}
		}
		Task task = new Task();
		task.addPropertyChangeListener(this);
		task.execute();

		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("SCLset recalculate took (" + timer.getTime() / 1000. + " sec)");
		cod.setDirty(true);
		changeCount++;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			mon.setProgress(progress);
			String message = String.format("Completed %d%%.\n", progress);
			mon.setNote(message);
		}

	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(getSCLCycles()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setCycles(SortedSet<SCLCycle> newCycles, double leftTime, double rightTime) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		sclcycle.removeAll(sclcycle.subSet(new SCLCycle(leftTime), new SCLCycle(rightTime)));
		sclcycle.addAll(newCycles);
		cod.setDirty(true);
		changeCount++;
	}

	public SortedSet<SCLCycle> subSet(double lTime, double rTime) {
		return sclcycle.subSet(new SCLCycle(lTime), new SCLCycle(rTime));
	}

	public boolean contains(double fromTime, double toTime) {
		return (sclcycle.subSet(new SCLCycle(fromTime), new SCLCycle(toTime)).isEmpty() == false);
	}

	public void changePeakorOnset(double oldTime, double newTime) {
		removeCycle(oldTime);
		addCycle(newTime);
	}

	public void removeCycle(double time) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (sclcycle.contains(new SCLCycle(time))) {
			SCLCycle myCycle = getCycleClosestToTime(time);
			sclcycle.remove(myCycle);
		}
		cod.setDirty(true);
		changeCount++;
	}

	public void updateSCLFeatures(double time) {
		SCLCycle cy = getCycleClosestToTime(time);
		try (BinaryFile bf = new BinaryFile("FILTSCL")) {
			if (cy.isPeak() == true) {
				cy.setPeakTime(time);
				cy.setPeakValue(bf.getDataAtTime(time));
				cy.setOnsetTime(-9999);
				cy.setOnsetValue(-9999);
				cy.setSCLValue(bf.getDataAtTime(time));
			} else {
				cy.setOnsetTime(time);
				cy.setOnsetValue(bf.getDataAtTime(time));
				cy.setPeakTime(-9999);
				cy.setPeakValue(-9999);
				cy.setSCLValue(bf.getDataAtTime(time));

			}
			// lSet = cod.getLabels();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void exportToASCII(File fl, boolean keepHeaders) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		PrintWriter pw = null;
		try {
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(2);
			nf.setMinimumFractionDigits(2);
			nf.setGroupingUsed(false);
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy/HH:mm:ss");
			lconf = cod.getLabelConfig();

			GregorianCalendar cal = Utils.getCalendarFromUS(cod.getStartTimeInUS());

			pw = new PrintWriter(fl);

			if (keepHeaders) {
				pw.println("Skin Conductance Response Per Label");

				pw.print(Utils.guaranteeExtensionAndAddition(cod.getDataFile().getName(), "scl", "_SCR"));
				pw.print(" ");

				pw.print(df.format(cal.getTime()));
				pw.print(" ");

				cal = Utils.getCalendarFromUS(cod.getEndTimeInUS());

				pw.print(df.format(cal.getTime()));
				pw.println();
				pw.println(
						"Subject_Id 		Begin date/time					End date/time				Labelcode			Label_Duration			Average_HR			SCL_LabelStart			Average_SCL			MinumumSCL			MaximumSCL");
				pw.println();
			}
			for (AmsLabel l : cod.getLabels().getLabels()) {
				pw.print(" ");
				pw.print(cod.getFileHeader().getSzSubjectID());
				pw.print("			");
				cal = Utils.getCalendarFromUS(l.getLeftTime());
				pw.print(df.format(cal.getTime()) + "			");
				cal = Utils.getCalendarFromUS(l.getRightTime());
				pw.print(df.format(cal.getTime()) + "			");
				for (Entry<String, String> entry : l.getAttributes().entrySet()) {
					if (!(entry.getKey().equalsIgnoreCase("Label No"))) {
						for (LabelValue lv : lconf.getConfig().get(entry.getKey())) {
							if (lv.getName().equals(entry.getValue())) {
								// pw.print(" " + lv.getCode());
							}
						}
					}
				}
				for (Entry<String, String> entry : l.getAttributes().entrySet()) {
					if (!(entry.getKey().equalsIgnoreCase("Label No"))) {
						for (LabelValue lv : lconf.getConfig().get(entry.getKey())) {
							if (lv.getName().equals(entry.getValue())) {
								pw.print(lv.getCode() + "=" + lv.getName() + "			");
								lcode = lv.getCode();
								lvname = lv.getName();
							}
						}
					}
				}

				double duration = Math.round(((l.getRightTime() - l.getLeftTime()) / 1000000.));
				pw.print(duration + "			");

				double averageHR = l.getAverage(true);
				pw.print(averageHR + "			");

				double sclvalueatstimulustime = l.getSCLvalueatTime();
				pw.print(sclvalueatstimulustime + "			");

				double averagescl = l.getAvSCL();
				pw.print(averagescl + "   ");

				pw.print(nf.format(l.getMinMaxSCL()[0]) + "			");
				pw.print(nf.format(l.getMinMaxSCL()[1]) + "			");
				pw.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}

	}

	public void removeSubSet(double lTime, double rTime) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		ArrayList<SCLCycle> sset = new ArrayList<SCLCycle>(sclcycle.subSet(new SCLCycle(lTime), new SCLCycle(rTime)));
		sclcycle.removeAll(sset);
		cod.setDirty(true);
		changeCount++;
	}

	public void rescan(double lTime, double rTime) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		removeSubSet(lTime, rTime);
		SCLDetector scl;
		SortedSet<SCLCycle> tempCycles;
		SCLArtefactSet sclArtefacts = cod.getSCLArtefacts();
		String SCLFile;
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7)
			SCLFile = "FILTSCL.dbin";
		else
			SCLFile = "FILTSCL.bin";

		double curTime = lTime;
		boolean stopped = false;
		int sampleTimeInUS = (int) cod.getFileHeader().getDwSampleTime_us();
		try {
			Ams7fsChannelInfo s = cod.getChannelInfoFromID("SCL");
			sampleTimeInUS *= s.getDwDivider();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (AmsLabel art : sclArtefacts.getLabels()) {
			if (art.getLeftTime() > curTime) {
				scl = new SCLShapeDetector(cod.getStartTimeInUS(), sampleTimeInUS,
						new File(cod.getFilePath(), SCLFile));
				double finalTime = rTime;
				stopped = true;
				if (art.getLeftTime() < finalTime) {
					finalTime = art.getLeftTime();
					stopped = false;
				}
				tempCycles = scl.findCycle(curTime, finalTime);
				sclcycle.addAll(tempCycles);
				curTime = art.getRightTime();
				if (stopped == true)
					break;
			} else {
				if (art.getRightTime() > curTime) {
					curTime = art.getRightTime();
					if (curTime > rTime)
						break;
				}
			}
		}
		if (stopped == false) {
			scl = new SCLShapeDetector(cod.getStartTimeInUS(), sampleTimeInUS,
					new File(cod.getFilePath(), SCLFile));
			tempCycles = scl.findCycle(curTime, rTime);
			sclcycle.addAll(tempCycles);
		}
		cod.setDirty(true);
		changeCount++;
	}

}
