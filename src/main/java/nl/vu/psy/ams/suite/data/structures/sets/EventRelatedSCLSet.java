package nl.vu.psy.ams.suite.data.structures.sets;

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
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.EventRelatedSCLCycle;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EventRelatedSCLSet {

	private double lfwindow = 1000000; // To calculate baseline SCL
	private double searchtimeforpeakfromStimuli = 7000000; // To find SCR peak
	private TreeSet<EventRelatedSCLCycle> sclcycle = new TreeSet<EventRelatedSCLCycle>();
	protected SortedSet<EventRelatedSCLCycle> scycle = new TreeSet<EventRelatedSCLCycle>();
	@SuppressWarnings("unused")
	private int changeCount = 0;
	final int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);

	public void recalculate() {

		// ------------------------ New Method--------------------------------
		/*
		 * The New Method should be able to handle both label based and event based
		 * designs.
		 * For label based design, the following outputs are important : Average SCL,
		 * Minimum SCL, Maximum SCL, isSCRPresent
		 * 
		 * For event based design, the following outputs are important : Stimulus Time,
		 * Event code, SCL_StimulusTime,
		 * Baseline_SCL, isSCRPresent, Onset_Time, Onset_SCL, Latency, Peak_Time,
		 * Peak_SCL, Rise_Time, Rise_SCL
		 * 
		 */
		Timer timer = new Timer();
		timer.start();
		BinaryFile bf = new BinaryFile("FILTSCL");
		sclcycle.clear();

		int fL = CurrentOpenData.getInstance().getEvents().size();
		int j = 0;
		JFrame frame = MainFrame.getInstance().getMainFrame();
		ProgressMonitor progress = new ProgressMonitor(frame, "Detecting Event related SCL-peaks", null, 0, (int) fL);
		for (Ams5fsPacket Events : CurrentOpenData.getInstance().getEvents()) {
			progress.setProgress(j);
			j++;

			if ((Events.getlType() == 0) || (Events.getlType() == 1) || (Events.getlType() == 2)
					|| (Events.getlType() == 100)) { // If event is type 0 or 1 or 2

				double eventTime = Events.getDwClockTick_ms() * 1000; // us
				double SCL_StimulusTime = bf.getDataAtTime(eventTime);

				double baseline_SCL = bf.getAverageBetweenTimes(eventTime - getleftWindow(), eventTime); // average of
																											// SCL
																											// values in
																											// 1 sec
																											// window to
																											// the left
																											// to event
				boolean isSCRPresent = false; // If there is peak detected within 7 sec from the stimulus time, then SCR
												// is present

				// In 7 seconds, there are 70 values
				if (CurrentOpenData.getInstance().getSCLArtefacts().isTimeUnderArtefact(eventTime) == false) {

					// ----------------- To detect First SCR Peak-----------------------------------
					double maxVal = Double.NEGATIVE_INFINITY;
					double maxPeakTime = 0;

					for (int i = 0; i <= searchtimeforpeakfromStimuli; i = i + 100000) {

						double currentTime = (eventTime + i); // us
						double current_SCLValue = bf.getDataAtTime(currentTime);

						double[] last10Values = bf.getDataRun(currentTime - 1000000, 1000000); // 1 sec
						double[] next10Values = bf.getDataRun(currentTime, 1000000); // 1 sec

						if (last10Values != null && next10Values != null) { // Prevents null pointer exception
							if ((current_SCLValue > maxVal)) {
								if (getSlope(last10Values) > 0) {
									if (getSlope(next10Values) < 0) {
										if (current_SCLValue > SCL_StimulusTime) { // Mark as a peak only if the
																					// current_SCLValue is greater than
																					// the SCL_Stimulustime
											maxVal = current_SCLValue; // Find the highest point
											maxPeakTime = currentTime;
											isSCRPresent = true;
										}
									}
								}
							}
						}
					}
					// -----------------------------------------------------------------------------

					// ----------------- To detect Onset time for the First SCR
					// Peak-----------------------------------
					double minPeakTime = 0;
					double minVal = Double.POSITIVE_INFINITY;

					if (isSCRPresent == true) {

						for (int i = 0; i <= (maxPeakTime - eventTime); i = i + 100000) {
							double currentTime = (eventTime + i); // us
							double current_SCLValue = bf.getDataAtTime(currentTime);

							if ((current_SCLValue < minVal)) {
								minVal = current_SCLValue; // Find the lowest point
								minPeakTime = currentTime;
							}
						}
					}
					// -----------------------------------------------------------------------------

					EventRelatedSCLCycle cycle = new EventRelatedSCLCycle(eventTime);
					cycle.setEventCode(Events.getlCode());
					cycle.setSCL_Stimulus(SCL_StimulusTime);
					cycle.setBaselineSCL(baseline_SCL);
					cycle.setSCRPresent(isSCRPresent);

					if (isSCRPresent == true) {
						cycle.setOnset_Time(minPeakTime);
						cycle.setOnset_SCL(minVal);
						cycle.setPeakTime(maxPeakTime);
						cycle.setPeakValue(maxVal);
					} else {
						cycle.setOnset_Time(misVal);
						cycle.setOnset_SCL(misVal);
						cycle.setPeakTime(misVal);
						cycle.setPeakValue(misVal);
					}
					// ---------------- Find average HR around the event time - Now it is by default
					// 5 seconds--------------
					BeatSet beats = CurrentOpenData.getInstance().getBeatSet();
					SortedSet<ECGBeat> subsetbeats = beats.getBeatBetweenTimes((eventTime - 5000000),
							(eventTime + 5000000));
					int heartrate = (60 * subsetbeats.size()) / 10;
					cycle.setHeartRate(heartrate);

					// ----------------------------------------------------------------------------------------------------

					sclcycle.add(cycle);

					maxVal = Double.NEGATIVE_INFINITY;
					minVal = Double.POSITIVE_INFINITY;
					maxPeakTime = 0;
					minPeakTime = 0;
				}
			}
			CurrentOpenData.getInstance().setDirty(true);
			changeCount++;
		}
		progress.close();
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("EventRelatedSCLset recalculate took (" + timer.getTime() / 1000. + " sec)");

		// ---------------------------------------------------------------------
	}

	public void addCycle(EventRelatedSCLCycle o) {
		sclcycle.add(o);
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void setLeftWindow(double lf) {
		this.lfwindow = lf;
	}

	public void setSearchWindow(double rf) {
		this.searchtimeforpeakfromStimuli = rf;
	}

	public double getleftWindow() {
		return lfwindow;
	}

	public double getSearchWindow() {
		return searchtimeforpeakfromStimuli;
	}

	public SortedSet<EventRelatedSCLCycle> getSCLCycles() {
		return sclcycle;
	}

	public SortedSet<EventRelatedSCLCycle> subSet(double lTime, double rTime) {
		return sclcycle.subSet(new EventRelatedSCLCycle(lTime), new EventRelatedSCLCycle(rTime));
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

	public EventRelatedSCLCycle getSCLCycleAfterTime(double time) {
		return getNextCycle(new EventRelatedSCLCycle(time));

	}

	public EventRelatedSCLCycle getSCLCycleBeforeTime(double time) {
		return getPrevCycle(new EventRelatedSCLCycle(time));

	}

	public EventRelatedSCLCycle getNextCycle(EventRelatedSCLCycle scl) {
		if (sclcycle == null)
			return null;
		NavigableSet<EventRelatedSCLCycle> tailSet = sclcycle.tailSet(scl, false);
		if (tailSet.isEmpty())
			return null;
		return tailSet.first();
	}

	public EventRelatedSCLCycle getPrevCycle(EventRelatedSCLCycle Cycle) {
		if (sclcycle == null)
			return null;
		NavigableSet<EventRelatedSCLCycle> headSet = sclcycle.headSet(Cycle, false);
		if (headSet.isEmpty())
			return null;
		return headSet.last();
	}

	public EventRelatedSCLCycle getCycleClosestToTime(double time) {
		EventRelatedSCLCycle myCycle = new EventRelatedSCLCycle(time);
		EventRelatedSCLCycle lPoint = sclcycle.floor(myCycle);
		EventRelatedSCLCycle rPoint = sclcycle.ceiling(myCycle);
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

	public int getCycleIndexClosestToTime(double time) {
		// I think headSet.size() basically returns the Index; it seems to work
		return sclcycle.headSet(getCycleClosestToTime(time)).size();
	}

	public void exportToASCII(File fl, boolean keepHeaders) {
		EventRelatedSCLSet sSet = CurrentOpenData.getInstance().getEventSCLSet();
		PrintWriter pw = null;
		int count = 1;

		try {
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(3);
			nf.setMinimumFractionDigits(3);
			nf.setGroupingUsed(false);
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy/HH:mm:ss");
			CurrentOpenData cod = CurrentOpenData.getInstance();
			pw = new PrintWriter(fl);
			GregorianCalendar cal = Utils.getCalendarFromUS(cod.getStartTimeInUS());

			if (keepHeaders) {
				pw.println("Skin Conductance Response Per Event");
				pw.print(Utils.guaranteeExtensionAndAddition(cod.getDataFile().getName(), "scl", "_SCR"));
				pw.print(" ");

				pw.print(df.format(cal.getTime()));
				pw.print(" ");

				cal = Utils.getCalendarFromUS(cod.getEndTimeInUS());
				pw.print(df.format(cal.getTime()));
				pw.println();

				pw.println(
						"Subject_Id			No				Event date/time				EventCode			HeartRate			SCL_EventTime[\u00B5S]		Baseline_SCL[\\u00B5S]		SCR_Present				Onset_Time						SCL_Onset[\u00B5S]		Latency[sec]		Peak_Time						SCL_Peak[\u00B5S]		Rise_Time[sec]		Rise_SCL[\u00B5S]");
				pw.println();
			}
			for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
				if ((event.getlType() == 0) || (event.getlType() == 1) || (event.getlType() == 2)
						|| (event.getlType() == 100)) {
					pw.print(" ");
					pw.print(CurrentOpenData.getInstance().getFileHeader().getSzSubjectID());
					pw.print("			");
					pw.print(count + "				");
					cal = Utils.getCalendarFromUS(event.getDwClockTick_ms() * 1000);
					pw.print(df.format(cal.getTime()) + "				");
					pw.print(event.getlCode() + "				");
					pw.print(sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getHeartRate()
							+ "   				");
					pw.print(nf.format(sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getSCL_Stimulus())
							+ "					");
					pw.print(nf.format(sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getBaselineSCL())
							+ "					");

					pw.print(sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).isSCRPresent()
							+ "					");

					double val1 = sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getOnset_Time();
					if (val1 != misVal) {
						pw.print(Utils.getDateAndTimeFromUS(val1) + "				");
					} else {
						pw.print(misVal + "				");
					}

					double val2 = sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getOnset_SCL();
					if (val2 != misVal) {
						pw.print(nf.format(val2) + "				");
					} else {
						pw.print(misVal + "				");
					}

					if (val1 != misVal) {
						pw.print(nf
								.format(((sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getOnset_Time())
										- (sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getTime()))
										/ 1000000)
								+ "				");
					} else {
						pw.print(misVal + "				");
					}

					double val3 = sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getPeakTime();
					if (val3 != misVal) {
						pw.print(Utils.getDateAndTimeFromUS(val3) + "				");
					} else {
						pw.print(misVal + "				");
					}

					double val4 = sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getPeakSCL();
					if (val4 != misVal) {
						pw.print(nf.format(val4) + "				");
					} else {
						pw.print(misVal + "				");
					}

					if (val3 != misVal && val1 != misVal) {
						pw.print(nf.format(((sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getPeakTime())
								- (sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getOnset_Time()))
								/ 1000000) + "				");
					} else {
						pw.print(misVal + "				");
					}

					if (val2 != misVal && val4 != misVal) {
						pw.print((nf.format(sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getPeakSCL()
								- (sSet.getCycleClosestToTime(event.getDwClockTick_ms() * 1000).getOnset_SCL()))));
					} else {
						pw.print(misVal + "				");
					}

					pw.println();
					count++;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}

	}

	public double getSlope(double[] values) {
		double sx = 0, sxx = 0, sy = 0, sxy = 0;
		int pos = 0;
		for (int i = 0; i < values.length; i++) {
			sx += i;
			sxx += i * i;
			sxy += i * values[pos];
			sy += values[pos];
			pos++;
			if (pos == values.length)
				pos = 0;
		}
		return (values.length * sxy - sx * sy) / (values.length * sxx - sx * sx);
	}

	public boolean contains(double fromTime, double toTime) {
		return (sclcycle.subSet(new EventRelatedSCLCycle(fromTime), new EventRelatedSCLCycle(toTime))
				.isEmpty() == false);
	}

	public void removeCycle(double time) {
		if (sclcycle.contains(new EventRelatedSCLCycle(time))) {
			EventRelatedSCLCycle myCycle = getCycleClosestToTime(time);
			sclcycle.remove(myCycle);
		}
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void addCycle(double time) {
		SCLArtefactSet arts = CurrentOpenData.getInstance().getSCLArtefacts();
		if (arts.isTimeUnderLabel(time))
			return;
		EventRelatedSCLCycle newCycle = new EventRelatedSCLCycle(time);
		if (sclcycle.contains(newCycle)) {
			newCycle = new EventRelatedSCLCycle(time - 1);
		}
		EventRelatedSCLCycle nextCycle = getSCLCycleAfterTime(newCycle.getTime());
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
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void updateSCLFeatures(double time) {
		EventRelatedSCLCycle cy = getCycleClosestToTime(time);
		if (cy.isSCRPresent() == true) {
			cy.setPeakTime(time);
			cy.setOnset_Time(-9999);
			cy.setOnset_SCL(-9999);
		} else {
			try (BinaryFile bf = new BinaryFile("FILTSCL")) {
				cy.setOnset_Time(time);
				cy.setPeakTime(-9999);
				cy.setOnset_SCL(bf.getDataAtTime(time));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void removeSubSet(double lTime, double rTime) {
		ArrayList<EventRelatedSCLCycle> sset = new ArrayList<EventRelatedSCLCycle>(
				sclcycle.subSet(new EventRelatedSCLCycle(lTime), new EventRelatedSCLCycle(rTime)));
		sclcycle.removeAll(sset);
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}
}
