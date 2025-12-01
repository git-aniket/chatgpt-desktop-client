package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

// import javax.swing.JFrame;
import javax.swing.JOptionPane;
// import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.qrs.IBISuspicionFinder;
import nl.vu.psy.ams.suite.data.qrs.QRSDetector;
import nl.vu.psy.ams.suite.data.qrs.shape.QRSShapeDetector;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 * Class that holds all ecg beat information and
 * provides methods to get useful information from
 * these beats.
 */
/**
 * @cd from Menaka's email to Ineke:
 *     The <b>suspicion of beats</b> is calculated using second order derivative
 *     of the inter beat intervals for four beats (including the one you are
 *     deleting/adding).
 *     The output is a number (something like variance). If the variance is
 *     more, then the beat gets assigned as high suspicious one.
 * 
 *     4 beats results in 3 IBI's and then the second derivative is calculated.
 *     If the output is greater than 1000, then it is assigned as highly
 *     suspicious beat
 *     and if the output is greater than 500 and less than 1000, then it is
 *     medium suspicious, else it is normal beat.
 */
public class BeatSet {
	private class IBISuspicionComparator implements Comparator<ECGBeat> {

		@Override
		public int compare(ECGBeat o1, ECGBeat o2) {
			return Double.compare(o2.getIBISuspicion(), o1.getIBISuspicion());
		}

	}

	private TreeSet<ECGBeat> beats = new TreeSet<ECGBeat>();
	private ArrayList<ECGBeat> beatsSortedByIBISuspicion = new ArrayList<ECGBeat>();
	private int nHigh;
	private int nMed;

	private int curSelectedSuspiciousIBI = 0;
	private int changeCount = 0;
	private DateFormat df = new SimpleDateFormat("dd-MM-yy/HH:mm:ss.SSS");
	private int minDistanceMs = 10;

	public void addBeat(double time) {
		ArtefactSet arts = CurrentOpenData.getInstance().getECGArtefacts();
		if (arts.isTimeUnderLabel(time))
			return;
		if (contains(time - minDistanceMs * 1000.0, time + minDistanceMs * 1000.0))
			return;
		ECGBeat newBeat = new ECGBeat(time);
		if (beats.contains(newBeat)) {
			newBeat = new ECGBeat(time - 1);
		}
		ECGBeat nextBeat = getBeatAfterTime(newBeat.getRPeakTime());
		if (nextBeat != null) {
			if (nextBeat.isFirstInSeries()) {
				AmsLabel art = arts.getLabelAfterTime(newBeat.getRPeakTime());
				if (art != null) {
					if (art.getLeftTime() > nextBeat.getRPeakTime()) {
						nextBeat.setFirstInSeries(false);
						newBeat.setFirstInSeries(true);
					}
				} else {
					nextBeat.setFirstInSeries(false);
					newBeat.setFirstInSeries(true);
				}
			}
		}
		beats.add(newBeat);
		setIBISuspicion(newBeat);
		updateIBISuspicionAroundTime(newBeat.getRPeakTime());
		CurrentOpenData.getInstance().setDirty(true);
		String chanName = CurrentOpenData.getInstance().getECGFile();
		if (chanName.startsWith("FILT")) {
			chanName = chanName.substring(4);
		}
		chanName = chanName.substring(0, chanName.length() - 4);
		recalculateHeight(chanName, newBeat);
		changeCount++;
	}

	public void addBeat(ECGBeat o) {
		addBeat(o, true);
	}

	public void addBeat(ECGBeat o, boolean setDirty) {
		beats.add(o);
		if (setDirty)
			CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void changeBeat(double oldTime, double newTime) {
		removeBeat(oldTime);
		addBeat(newTime);
	}

	public boolean contains(double fromTime, double toTime) {
		return (beats.subSet(new ECGBeat(fromTime), new ECGBeat(toTime)).isEmpty() == false);
	}

	public void exportToASCII(File fl, boolean append, boolean showEvents) {

		/*
		 * // For Mandy Xian Hu, GGZ inGeest i.s.m. Eco de Geus
		 * // Can be used with: Batch export data > Export Beats to ASCII File
		 * try {
		 * // Manually insert 'header': File Name,No of beats,High,Medium
		 * PrintWriter out = new PrintWriter(new BufferedWriter(new
		 * FileWriter("SuspiciousBeats.csv", true)));
		 * out.println(fl.getName()+","+beats.size()+","+getnHigh()+","+getnMed());
		 * out.close();
		 * } catch (IOException e) {
		 * e.printStackTrace();
		 * }
		 */

		ArrayList<Ams5fsPacket> events = new ArrayList<Ams5fsPacket>();
		for (Ams5fsPacket p : CurrentOpenData.getInstance().getEvents()) {
			if (p.getlType() <= 2 || p.getlType() == 100) {
				events.add(p);
			}
		}
		Ams5fsPacket nextEvent = null;
		if (events.isEmpty() == false)
			nextEvent = events.get(0);
		int curEvent = 0;

		int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);

		if (beats.isEmpty())
			return;

		double firstBeatTime = beats.first().getRPeakTime();
		PrintWriter pw = null;
		ArrayList<ECGBeat> bts = new ArrayList<ECGBeat>(beats);
		int lng = bts.size();

		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(fl, append)));

			ECGBeat b0 = bts.get(0);
			double bTime0 = b0.getRPeakTime();
			double time0 = bTime0 - firstBeatTime;
			long msecTime = Math.round(time0 / 1000);
			String subjectname = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
			pw.println("Subject Id: " + subjectname);
			AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> cats = config.getCategories();
			String labelString = "";
			for (int j = 1; j <= cats.size(); j++) {
				labelString += "LabelCode" + j + "\t";
			}
			if (showEvents) {
				pw.println("R-peak time\tibi_cumulative\tibi\tinterpolated\t" + labelString
						+ "eventType\teventCode\teventMessage");
				pw.println(outputASCIIStringWithEvents(bTime0, 0L, msecTime, false, false, misVal, misVal,
						String.valueOf(misVal)));
			} else {
				pw.println("R-peak time\tibi_cumulative\tibi\tinterpolated\t" + labelString);
				pw.println(outputASCIIString(bTime0, 0L, msecTime, false));
			}
			for (int i = 1; i < lng; i++) {
				ECGBeat b = bts.get(i);
				ECGBeat bl = bts.get(i - 1);
				double bTime = b.getRPeakTime();
				double time = bTime - firstBeatTime;
				msecTime = Math.round(time / 1000);
				Long ibi = null;

				if (b.isFirstInSeries() == false) {

					ibi = Long.valueOf(Math.round((bTime - bl.getRPeakTime()) / 1000));
					int ltype = misVal;
					int lcode = misVal;
					String evMessage = String.valueOf(misVal);
					long eventtimeinMS = 0;
					boolean isEvent = false;

					// --------------------- Include Event Information in the ASCII
					// export----------------------------
					if (nextEvent != null) {
						double evtime = (nextEvent.getDwClockTick_ms() * 1000);
						eventtimeinMS = (nextEvent.getDwClockTick_ms()
								- (CurrentOpenData.getInstance().getStartTimeInUS() / 1000));
						double diff = bTime - evtime;
						if ((diff > 0) && (diff < 1000000)) {
							ltype = nextEvent.getlType();
							lcode = nextEvent.getlCode();
							evMessage = nextEvent.getSzMessage();
							if (evMessage.equals("")) {
								evMessage = String.valueOf(misVal);
							}
							isEvent = true;
							if (showEvents) {
								pw.println(outputASCIIStringWithEvents(evtime, ibi, eventtimeinMS, false, isEvent,
										ltype, lcode, evMessage));
							} else {
								pw.println(outputASCIIString(bTime, ibi, msecTime, false));
							}

							curEvent++;
							if (curEvent < events.size()) {
								nextEvent = events.get(curEvent);
							} else {
								nextEvent = null;
							}
						}
						eventtimeinMS = 0;
						isEvent = false;
						ltype = misVal;
						lcode = misVal;
						evMessage = String.valueOf(misVal);
					}
					if (showEvents) {
						pw.println(outputASCIIStringWithEvents(bTime, ibi, msecTime, false, isEvent, ltype, lcode,
								evMessage)); // Prints Beats
					} else {
						pw.println(outputASCIIString(bTime, ibi, msecTime, false));
					}
					// -----------------------------------------------------------------------------------------------
				} else {
					boolean outputDone = false;
					if (i > 1 && i < lng - 1) {
						ECGBeat bll = bts.get(i - 2);
						ECGBeat br = bts.get(i + 1);
						if (bl.isFirstInSeries() == false && br.isFirstInSeries() == false) {
							if (b.getRPeakTime() - bl.getRPeakTime() < 5000000) {
								double lval = bl.getRPeakTime() - bll.getRPeakTime();
								double rval = br.getRPeakTime() - b.getRPeakTime();
								double ltime = bl.getRPeakTime();
								double rtime = b.getRPeakTime();
								double slope = (rval - lval) / (rtime - ltime);
								ArrayList<ECGBeat> beatlist = new ArrayList<ECGBeat>();
								double curTime = b.getRPeakTime();
								double curslopetime = b.getRPeakTime() - bl.getRPeakTime();
								while (true) {
									double curval = lval + slope * curslopetime;
									if (curslopetime - curval > 0) {
										curTime -= curval;
										curslopetime -= curval;
										beatlist.add(new ECGBeat(curTime));
									} else {
										double timeLeft = curslopetime;
										if (beatlist.isEmpty() == false) {
											if (timeLeft < curval / 2) {
												double timePerBeat = timeLeft / beatlist.size();
												for (ECGBeat bUp : beatlist) {
													bUp.setRPeakTime(bUp.getRPeakTime() + timePerBeat);
												}
												beatlist.remove(beatlist.size() - 1);
											} else {
												double timePerBeat = (curval - timeLeft) / beatlist.size();
												for (ECGBeat bUp : beatlist) {
													bUp.setRPeakTime(bUp.getRPeakTime() - timePerBeat);
												}
											}
										}
										break;
									}
								}
								if (beatlist.isEmpty() == false) {
									outputDone = true;
									double timeTMP = beatlist.get(beatlist.size() - 1).getRPeakTime();
									long msecTimeTMP = Math.round((timeTMP - firstBeatTime) / 1000);
									long ibiTMP = Math.round((timeTMP - bl.getRPeakTime()) / 1000);
									if (showEvents) {
										pw.println(outputASCIIStringWithEvents(timeTMP, ibiTMP, msecTimeTMP, true,
												false, misVal, misVal, String.valueOf(misVal)));
									} else {
										pw.println(outputASCIIString(timeTMP, ibiTMP, msecTimeTMP, true));
									}

									for (int j = beatlist.size() - 2; j >= 0; j--) {
										timeTMP = beatlist.get(j).getRPeakTime();
										msecTimeTMP = Math.round((timeTMP - firstBeatTime) / 1000);
										ibiTMP = Math.round((timeTMP - beatlist.get(j + 1).getRPeakTime()) / 1000);
										if (showEvents) {
											pw.println(outputASCIIStringWithEvents(timeTMP, ibiTMP, msecTimeTMP, true,
													false, misVal, misVal, String.valueOf(misVal)));
										} else {
											pw.println(outputASCIIString(timeTMP, ibiTMP, msecTimeTMP, true));
										}

									}
									timeTMP = b.getRPeakTime();
									msecTimeTMP = Math.round((timeTMP - firstBeatTime) / 1000);
									ibiTMP = Math.round((timeTMP - beatlist.get(0).getRPeakTime()) / 1000);
									if (showEvents) {
										pw.println(outputASCIIStringWithEvents(timeTMP, ibiTMP, msecTimeTMP, true,
												false, misVal, misVal, String.valueOf(misVal)));
									} else {
										pw.println(outputASCIIString(timeTMP, ibiTMP, msecTimeTMP, true));
									}

								}
							}
						}
					}
					if (outputDone == false) {
						if (showEvents) {
							pw.println(outputASCIIStringWithEvents(bTime, ibi, msecTime, false, false, misVal, misVal,
									String.valueOf(misVal)));
						} else {
							pw.println(outputASCIIString(bTime, ibi, msecTime, false));
						}
					}
				}

			}
		} catch (IOException e) {
		} finally {
			if (pw != null) {
				pw.close();
			}
		}

	}

	public ArtefactSet getArtefactsFromBeats(File fl) {
		ArtefactSet afs = new ArtefactSet();
		double prevTime = Double.NEGATIVE_INFINITY;
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(fl)));
			byte b[] = new byte[4];
			in.readFully(b);
			if (b[0] != 66 || b[1] != 69 || b[2] != 65 || b[3] != 84) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "Not a valid .beat file",
						"Import beat error", JOptionPane.ERROR_MESSAGE);
				return null;
			}
			in.skip(28);
			b = new byte[16];
			while (in.available() > 0) {
				in.readFully(b);
				UnsignedByteBuffer buff = UnsignedByteBuffer.getInstance();
				buff.setBytes(b);
				long pos = buff.getUInt();
				buff.getUInt();
				buff.getUInt();
				int sos = buff.getUByte();
				if (sos == 1) {
					if (prevTime != Double.NEGATIVE_INFINITY) {
						afs.add(AmsLabel.generateECGArtefact(prevTime + 1, 1000. * pos - 1, false, 0.0,
								"From .beat file"));
					}
				} else {
					prevTime = pos * 1000.;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return afs;
	}

	public double getAverageBetweenTimes(double lTime, double rTime, boolean inHR) {
		double av = 0.;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		int k = 0;
		double val;
		for (int i = 0; i < n; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false) {
				val = beatsBetween.get(i + 1).getRPeakTime() - beatsBetween.get(i).getRPeakTime();
				if (inHR == true) {
					val = 60000000 / val;
				} else {
					val /= 1000;
				}
				av += val;
				k++;
			}
		}
		if (k == 0)
			return -1;
		return av / k;
	}

	public double getAverageHeightBetweenTimes(double lTime, double rTime) {
		double av = 0.;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size();
		int k = 0;
		double val;
		for (int i = 0; i < n; i++) {
			val = beatsBetween.get(i).getRPeakHeight();
			av += val;
			k++;
		}
		if (k == 0)
			return -1;
		return av / k;
	}

	public SortedSet<ECGBeat> getBeatBetweenTimes(double ltime, double rtime) {
		return beats.subSet(new ECGBeat(ltime), new ECGBeat(rtime));
	}

	public ECGBeat getBeatAfterTime(double time) {
		return getNextBeat(new ECGBeat(time));
	}

	public ECGBeat getBeatAtSelectedSuspiciousIBI() {
		if (beatsSortedByIBISuspicion.isEmpty())
			return null;
		return beatsSortedByIBISuspicion.get(curSelectedSuspiciousIBI);
	}

	public ECGBeat getBeatBeforeTime(double time) {
		return getPrevBeat(new ECGBeat(time));
	}

	public ECGBeat getBeatClosestToTime(double time) {
		ECGBeat myBeat = new ECGBeat(time);
		ECGBeat lBeat = beats.floor(myBeat);
		ECGBeat rBeat = beats.ceiling(myBeat);
		double ldiff = Double.MAX_VALUE;
		if (lBeat != null)
			ldiff = time - lBeat.getRPeakTime();
		double rdiff = Double.MAX_VALUE;
		if (rBeat != null)
			rdiff = rBeat.getRPeakTime() - time;
		if (ldiff < rdiff) {
			return lBeat;
		} else {
			return rBeat;
		}
	}

	public SortedSet<ECGBeat> getBeats() {
		return beats;
	}

	public ECGBeat getBeatSortedByIBISuspicion(int index) {
		if (index < 0 || index >= beatsSortedByIBISuspicion.size())
			return null;
		return beatsSortedByIBISuspicion.get(index);
	}

	public int getChangeCount() {
		return changeCount;
	}

	public double[] getDataBetweenTimes(double lTime, double rTime) {
		ECGBeat rBeat = getBeatBeforeTime(rTime);
		ECGBeat lBeat = getBeatAfterTime(lTime);
		if (rBeat == null || lBeat == null || lBeat.getRPeakTime() > rBeat.getRPeakTime()) // no beats between times
			return null;
		ArrayList<ECGBeat> tBeats = new ArrayList<ECGBeat>(beats.subSet(lBeat, true, rBeat, true));
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Double> times = new ArrayList<Double>();
		int n = tBeats.size();
		for (int i = 0; i < n - 1; i++) {
			if (tBeats.get(i + 1).isFirstInSeries() == false) {
				times.add(tBeats.get(i).getRPeakTime());
				values.add(tBeats.get(i + 1).getRPeakTime() - tBeats.get(i).getRPeakTime());
			}
		}
		int nSamples = times.size();
		if (nSamples == 0)
			return null;
		double[] ret = new double[2 * nSamples];
		for (int i = 0; i < nSamples; i++) {
			ret[2 * i] = times.get(i) / 1000000;
			ret[2 * i + 1] = values.get(i) / 1000000;
		}
		return ret;
	}

	public double getMaxBetweenTimes(double lTime, double rTime, boolean inHR) {
		double max = Double.NEGATIVE_INFINITY;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		if (n < 1)
			return -1;
		double val;
		for (int i = 0; i < n; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false) {
				val = beatsBetween.get(i + 1).getRPeakTime() - beatsBetween.get(i).getRPeakTime();
				if (inHR == true) {
					val = 60000000 / val;
				} else {
					val /= 1000;
				}
				if (val > max)
					max = val;
			}
		}
		return max;
	}

	public double getMinBetweenTimes(double lTime, double rTime, boolean inHR) {
		double min = Double.MAX_VALUE;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		if (n < 1)
			return -1;
		double val;
		for (int i = 0; i < n; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false) {
				val = beatsBetween.get(i + 1).getRPeakTime() - beatsBetween.get(i).getRPeakTime();
				if (inHR == true) {
					val = 60000000 / val;
				} else {
					val /= 1000;
				}
				if (val < min)
					min = val;
			}
		}
		return min;
	}

	public ECGBeat getNextBeat(ECGBeat beat) {
		if (beat == null)
			return null;
		NavigableSet<ECGBeat> tailSet = beats.tailSet(beat, false);
		if (tailSet.isEmpty())
			return null;
		return tailSet.first();
	}

	public int getnHigh() {
		return nHigh;
	}

	public int getnMed() {
		return nMed;
	}

	public int getNumberOfIBIs(double lTime, double rTime) {
		int k = 0;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		if (n < 1)
			return -1;
		for (int i = 0; i < n; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false) {
				k++;
			}
		}
		return k;
	}

	public ECGBeat getPrevBeat(ECGBeat beat) {
		if (beat == null)
			return null;
		NavigableSet<ECGBeat> headSet = beats.headSet(beat, false);
		if (headSet.isEmpty())
			return null;
		return headSet.last();
	}

	public double getRMSSDBetweenTimes(double lTime, double rTime) {
		double av = 0.0;
		int k = 0;
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		if (n < 1)
			return -1;
		double val, rval;
		for (int i = 0; i < n - 1; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false
					&& beatsBetween.get(i + 2).isFirstInSeries() == false) {
				val = beatsBetween.get(i + 1).getRPeakTime() - beatsBetween.get(i).getRPeakTime();
				val /= 1000;
				rval = beatsBetween.get(i + 2).getRPeakTime() - beatsBetween.get(i + 1).getRPeakTime();
				rval /= 1000;
				av += (rval - val) * (rval - val);
				k++;
			}
		}
		return Math.sqrt(av / k);
	}

	public int getSelectedSuspiciousIBI() {
		return curSelectedSuspiciousIBI;
	}

	public double[] getSmoothedDataBetweenTimes(double lTime, double rTime) {
		ECGBeat rBeat = getBeatBeforeTime(rTime);
		ECGBeat lBeat = getBeatAfterTime(lTime);
		if (rBeat == null || lBeat == null || lBeat.getRPeakTime() > rBeat.getRPeakTime()) // no beats between times
			return null;
		ArrayList<ECGBeat> tBeats = new ArrayList<ECGBeat>(beats.subSet(lBeat, true, rBeat, true));
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Double> times = new ArrayList<Double>();
		int n = tBeats.size();
		for (int i = 0; i < n - 1; i++) {
			if (tBeats.get(i + 1).isFirstInSeries() == false) {
				boolean isValid = true;
				for (int j = i - 2; j <= i + 2; j++) {
					if (j >= 0 && j < n) {
						if (tBeats.get(j).getIBISuspicion() > 500) {
							isValid = false;
						}
					}
				}
				if (isValid) {
					times.add(tBeats.get(i).getRPeakTime());
					values.add(tBeats.get(i + 1).getRPeakTime() - tBeats.get(i).getRPeakTime());
				}
			}
		}

		int nSamples = times.size();
		if (nSamples == 0)
			return null;
		double[] ret = new double[2 * nSamples];
		for (int i = 0; i < nSamples; i++) {
			ret[2 * i] = times.get(i) / 1000000;
			ret[2 * i + 1] = values.get(i) / 1000000;
		}
		return ret;
	}

	public double getStddevBetweenTimes(double lTime, double rTime, boolean inHR) {
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size() - 1;
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double val;
		for (int i = 0; i < n; i++) {
			if (beatsBetween.get(i + 1).isFirstInSeries() == false) {
				val = beatsBetween.get(i + 1).getRPeakTime() - beatsBetween.get(i).getRPeakTime();
				if (inHR == true) {
					val = 60000000 / val;
				} else {
					val /= 1000;
				}
				tmpM = m;
				m += (val - tmpM) / k;
				s += (val - tmpM) * (val - m);
				k++;
			}
		}

		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}

	public double getStddevHeightBetweenTimes(double lTime, double rTime) {
		ArrayList<ECGBeat> beatsBetween = new ArrayList<ECGBeat>(subSet(lTime, rTime));
		int n = beatsBetween.size();
		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double val;
		for (int i = 0; i < n; i++) {
			val = beatsBetween.get(i).getRPeakHeight();
			tmpM = m;
			m += (val - tmpM) / k;
			s += (val - tmpM) * (val - m);
			k++;
		}

		if (k < 2)
			return -1;
		return Math.sqrt(s / (k - 2));
	}

	public void importFromFile(File fl) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(fl)));
			byte b[] = new byte[4];
			in.readFully(b);
			if (b[0] != 66 || b[1] != 69 || b[2] != 65 || b[3] != 84) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "Not a valid .beat file",
						"Import beat error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			in.skip(28);
			TreeSet<ECGBeat> toBeAdded = new TreeSet<ECGBeat>();
			b = new byte[16];
			while (in.available() > 0) {
				in.readFully(b);
				UnsignedByteBuffer buff = UnsignedByteBuffer.getInstance();
				buff.setBytes(b);
				long pos = buff.getUInt();
				buff.getUInt();
				buff.getUInt();
				int sos = buff.getUByte();
				if (sos != 1) {
					toBeAdded.add(new ECGBeat(pos * 1000.));
				}
			}
			if (toBeAdded.size() > 0) {
				beats.clear();
				beats.addAll(toBeAdded);
				CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
				resetFirstInSeries();
				recalculateAllSuspiciousLevels();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	private boolean isNullOrFirstInSeries(ECGBeat beat) {
		return (beat == null || beat.isFirstInSeries());
	}

	public int numberOfSuspiciousBeats() {
		return beatsSortedByIBISuspicion.size();
	}

	private String outputASCIIString(double time, Long ibi, long msecTime, boolean isInterpolated) {
		String dateString = df.format(Utils.getCalendarFromUS(time).getTime());
		String ibiString = "";
		if (ibi != null) {
			ibiString = ibi.toString();
		}
		AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
		ArrayList<String> cats = config.getCategories();
		AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
		AmsLabel lab = lSet.getLabelUnderTime(time);
		String labelString = "";
		if (lab != null) {
			Map<String, String> att = lab.getAttributes();
			for (int j = 0; j < cats.size(); j++) {
				String val = att.get(cats.get(j));
				for (LabelValue lv : config.getConfig().get(cats.get(j))) {
					if (lv.getName().equals(val)) {
						labelString += Integer.toString(lv.getCode()) + "\t";
					}
				}
			}
		} else {
			int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
			for (int j = 0; j < cats.size(); j++)
				labelString += Integer.toString(misVal) + "\t";
		}
		String intPolString = "O";
		if (isInterpolated)
			intPolString = "I";
		return dateString + "\t" + msecTime + "\t" + ibiString + "\t" + intPolString + "\t" + labelString;
	}

	private String outputASCIIStringWithEvents(double time, Long ibi, long msecTime, boolean isInterpolated,
			boolean isEvent, int ltype, int lcode, String lmessage) {
		String output = "";
		String dateString = df.format(Utils.getCalendarFromUS(time).getTime());
		String ibiString = "";
		if (ibi != null) {
			ibiString = ibi.toString();
		}
		int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
		AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
		ArrayList<String> cats = config.getCategories();
		AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
		AmsLabel lab = lSet.getLabelUnderTime(time);
		String labelString = "";
		if (lab != null) {
			Map<String, String> att = lab.getAttributes();
			for (int j = 0; j < cats.size(); j++) {
				String val = att.get(cats.get(j));
				for (LabelValue lv : config.getConfig().get(cats.get(j))) {
					if (lv.getName().equals(val)) {
						labelString += Integer.toString(lv.getCode()) + "\t";
					}
				}
			}
		} else {
			for (int j = 0; j < cats.size(); j++)
				labelString += Integer.toString(misVal) + "\t";
		}
		String intPolString = "O";
		if (isInterpolated)
			intPolString = "I";
		if (isEvent == true) {
			output = dateString + "\t" + msecTime + "\t" + String.valueOf(misVal) + "\t" + String.valueOf(misVal) + "\t"
					+ labelString + ltype + "\t" + lcode + "\t" + lmessage;
		} else {
			output = dateString + "\t" + msecTime + "\t" + ibiString + "\t" + intPolString + "\t" + labelString
					+ String.valueOf(misVal) + "\t" + String.valueOf(misVal) + "\t" + String.valueOf(misVal);
		}
		return output;
	}

	public void reCalculate(String ecgFile) {
		Timer timer = new Timer();
		timer.start();
		beats.clear();
		double ptpw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.PTPW), 0, 100) / 100.;
		double upw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.UPW), 0, 100) / 100.;
		double downw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.DOWNW), 0, 100) / 100.;
		double hT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.HIGHTHRESHOLD), 0, 100)
				/ 100.;
		double lT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.LOWTHRESHOLD), 0, 100)
				/ 100.;
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		long endTimeInUS = CurrentOpenData.getInstance().getEndTimeInUS();

		QRSDetector qrs;
		SortedSet<ECGBeat> tempBeats;
		ArtefactSet ecgArtefacts = CurrentOpenData.getInstance().getECGArtefacts();
		// int lTimeInS = (int) (startTime / 1000000); // time in seconds
		// int rTimeInS = (int) (endTimeInUS / 1000000);
		// JFrame frame = MainFrame.getInstance().getMainFrame();
		// ProgressMonitor mon = new ProgressMonitor(frame, "Detecting ECG R-Peaks...",
		// null, lTimeInS, rTimeInS);
		double curTime = startTime;

		for (AmsLabel art : ecgArtefacts.getLabels()) {

			// System.out.println("Under artefacts"); // When there are artefacts, for
			// instance when the electrodes are not attached properly, artefacts will be
			// generated.
			// This will be called when a 5fs file is opened. The data is raw. There is no
			// possibility to add artefacts to the 5fs file.
			// These artefacts are generated during recording.
			qrs = new QRSShapeDetector(ptpw, upw, downw, hT, lT, ecgFile);
			// qrs.setMonitor(mon);
			if (art.getLeftTime() > curTime) { // sometimes first artefact starts before startTime
				tempBeats = qrs.findBeats(curTime, art.getLeftTime());
				IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
				beats.addAll(tempBeats);
			}
			curTime = art.getRightTime();
		}
		qrs = new QRSShapeDetector(ptpw, upw, downw, hT, lT, ecgFile);
		// qrs.setMonitor(mon);
		tempBeats = qrs.findBeats(curTime, endTimeInUS);
		IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
		beats.addAll(tempBeats);
		// mon.close();
		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Beatset recalculate took (" + timer.getTime() / 1000. + " sec)");
		changeCount++;
	}

	public void recalculateHeights(String ecgChan) {
		recalculateHeights(ecgChan, beats);
	}

	public void recalculateHeights(String ecgChan, Set<ECGBeat> recalcBeats) {
		Timer timer = new Timer();
		timer.start();
		BinaryFile bf = new BinaryFile(ecgChan);
		double RVal;
		double[] datarun;
		for (ECGBeat beat : recalcBeats) {
			// RVal = bf.getDataAtTime(beat.getRPeakTime());
			datarun = bf.getDataRun(beat.getRPeakTime() - 100000, 200000);
			if (datarun == null)
				continue;
			RVal = datarun[101];
			double minmax[] = new double[2];
			minmax[0] = Double.POSITIVE_INFINITY;
			minmax[1] = Double.NEGATIVE_INFINITY;
			for (double d : datarun) {
				if (d < minmax[0])
					minmax[0] = d;
				if (d > minmax[1])
					minmax[1] = d;
			}
			beat.setRPeakHeight(RVal - minmax[0]);
			// if (RVal - minmax[0] > 10)
			// System.out.println("Improbable R height " + (RVal - minmax[0]));
			beat.setBaseHeight((RVal + minmax[0]) / 2);
		}
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				|| CurrentOpenData.getInstance().fileHeader.getDwHardwareVersion() == 7) {
			BinaryFile bff = new BinaryFile("FILT" + ecgChan);
			for (ECGBeat beat : recalcBeats) {
				// RVal = bff.getDataAtTime(beat.getRPeakTime());
				datarun = bff.getDataRun(beat.getRPeakTime() - 100000, 200000);
				if (datarun == null)
					continue;
				RVal = datarun[101];
				double minmax[] = new double[2];
				minmax[0] = Double.POSITIVE_INFINITY;
				minmax[1] = Double.NEGATIVE_INFINITY;
				for (double d : datarun) {
					if (d < minmax[0])
						minmax[0] = d;
					if (d > minmax[1])
						minmax[1] = d;
				}
				beat.setRPeakHeightF(RVal - minmax[0]);
				// if (RVal - minmax[0] > 10)
				// System.out.println("Improbable R height " + (RVal - minmax[0]));
				beat.setBaseHeightF((RVal + minmax[0]) / 2);
			}
		try {
			bff.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Peak heights recalculate took (" + timer.getTime() / 1000. + " sec)");
	}

	public void recalculateHeight(String ecgChan, ECGBeat beat) {
		BinaryFile bf = new BinaryFile(ecgChan);
		double RVal;
		double[] datarun;
		// RVal = bf.getDataAtTime(beat.getRPeakTime());
		datarun = bf.getDataRun(beat.getRPeakTime() - 100000, 200000);
		try {
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (datarun == null)
			return;
		RVal = datarun[101];
		double minmax[] = new double[2];
		minmax[0] = Double.POSITIVE_INFINITY;
		minmax[1] = Double.NEGATIVE_INFINITY;
		for (double d : datarun) {
			if (d < minmax[0])
				minmax[0] = d;
			if (d > minmax[1])
				minmax[1] = d;
		}
		beat.setRPeakHeight(RVal - minmax[0]);
		// if (RVal - minmax[0] > 10)
		// System.out.println("Improbable R height " + (RVal - minmax[0]));
		beat.setBaseHeight((RVal + minmax[0]) / 2);
		if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				|| CurrentOpenData.getInstance().fileHeader.getDwHardwareVersion() == 7) {
			BinaryFile bff = new BinaryFile("FILT" + ecgChan);
			// RVal = bff.getDataAtTime(beat.getRPeakTime());
			datarun = bff.getDataRun(beat.getRPeakTime() - 100000, 200000);
		try {
			bff.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
			if (datarun == null)
				return;
			RVal = datarun[101];
			minmax = new double[2];
			minmax[0] = Double.POSITIVE_INFINITY;
			minmax[1] = Double.NEGATIVE_INFINITY;
			for (double d : datarun) {
				if (d < minmax[0])
					minmax[0] = d;
				if (d > minmax[1])
					minmax[1] = d;
			}
			beat.setRPeakHeightF(RVal - minmax[0]);
			// if (RVal - minmax[0] > 10)
			// System.out.println("Improbable R height " + (RVal - minmax[0]));
			beat.setBaseHeightF((RVal + minmax[0]) / 2);
		}
	}

	public void recalculateAllSuspiciousLevels() {
		for (ECGBeat b : beats)
			b.setIBISuspicion(0.);
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		long endTimeInUS = CurrentOpenData.getInstance().getEndTimeInUS();
		SortedSet<ECGBeat> tempBeats;
		ArtefactSet ecgArtefacts = CurrentOpenData.getInstance().getECGArtefacts();
		double curTime = startTime;
		for (AmsLabel art : ecgArtefacts.getLabels()) {
			if (curTime < art.getLeftTime()) {
				tempBeats = subSet(curTime, art.getLeftTime());
				IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
				curTime = art.getRightTime();
			}
		}
		if (curTime < endTimeInUS) {
			tempBeats = subSet(curTime, endTimeInUS);
			IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
		}
		recheckSuspiciousIBIS();
	}

	public void recheckSuspiciousIBIS() {
		beatsSortedByIBISuspicion.clear();
		ArrayList<ECGBeat> tempBeatList = new ArrayList<ECGBeat>(beats);
		Collections.sort(tempBeatList, new IBISuspicionComparator());
		TreeSet<Double> alreadyIncludedTimes = new TreeSet<Double>();
		nHigh = 0;
		nMed = 0;
		for (ECGBeat b : tempBeatList) {
			if (alreadyIncludedTimes.subSet(b.getRPeakTime() - 1000000, b.getRPeakTime() + 1000000).isEmpty()) {
				beatsSortedByIBISuspicion.add(b);
				alreadyIncludedTimes.add(b.getRPeakTime());
				if (b.getIBISuspicion() > 1000) {
					nHigh++;
				} else if (b.getIBISuspicion() > 500) {
					nMed++;
				}
			}
		}
	}

	public void removeBeat(double time) {
		if (beats.contains(new ECGBeat(time))) {
			ECGBeat myBeat = getBeatClosestToTime(time);
			beats.remove(myBeat);
			if (myBeat.isFirstInSeries()) {
				ECGBeat nextBeat = getBeatAfterTime(time);
				if (nextBeat != null) {
					ArtefactSet arts = CurrentOpenData.getInstance().getECGArtefacts();
					AmsLabel art = arts.getLabelAfterTime(time);
					if (art == null || art.getLeftTime() > nextBeat.getRPeakTime()) {
						nextBeat.setFirstInSeries(true);
					}
				}
			}
			updateIBISuspicionAroundTime(myBeat.getRPeakTime());
		}
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void removeSubSet(double lTime, double rTime) {
		ArrayList<ECGBeat> sset = new ArrayList<ECGBeat>(beats.subSet(new ECGBeat(lTime), new ECGBeat(rTime)));
		beats.removeAll(sset);
		changeCount++;
	}

	public void rescan(double lTime, double rTime, double hT, double lT, double ptpW, double upW, double downW,
			String ecgFile) {
		removeSubSet(lTime, rTime);
		QRSDetector qrs;
		SortedSet<ECGBeat> tempBeats;
		ArtefactSet ecgArtefacts = CurrentOpenData.getInstance().getECGArtefacts();
		double curTime = lTime;
		boolean stopped = false;
		String chanName = ecgFile;
		if (chanName.startsWith("FILT")) {
			chanName = chanName.substring(4);
		}
		chanName = chanName.substring(0, chanName.length() - 4);
		double timeBuf = 100000; // find (removed) peaks on edge of selected region

		for (AmsLabel art : ecgArtefacts.getLabels()) {
			if (art.getLeftTime() > curTime) {
				qrs = new QRSShapeDetector(ptpW, upW, downW, hT, lT, ecgFile);
				double finalTime = rTime + timeBuf;
				stopped = true;
				if (art.getLeftTime() < finalTime) {
					finalTime = art.getLeftTime();
					stopped = false;
				}
				tempBeats = qrs.findBeats(curTime - timeBuf, finalTime);
				IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
				beats.addAll(tempBeats);
				recalculateHeights(chanName, tempBeats);
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
			qrs = new QRSShapeDetector(1, 0.33, 0.33, 0.8, 0.6, ecgFile);
			tempBeats = qrs.findBeats(curTime - timeBuf, rTime + timeBuf);
			IBISuspicionFinder.setIBISuspicionLevel(tempBeats);
			beats.addAll(tempBeats);
			recalculateHeights(chanName, tempBeats);
		}
		resetFirstInSeries();
		recheckSuspiciousIBIS();
		changeCount++;
	}

	public void resetFirstInSeries() {
		if (beats.isEmpty())
			return;
		for (ECGBeat b : beats)
			b.setFirstInSeries(false);
		beats.first().setFirstInSeries(true);
		ArtefactSet aSet = CurrentOpenData.getInstance().getECGArtefacts();
		for (AmsLabel l : aSet.getLabels()) {
			ECGBeat beatAfterArtefact = getBeatAfterTime(l.getRightTime());
			if (beatAfterArtefact != null)
				beatAfterArtefact.setFirstInSeries(true);
		}
	}

	public void saveToBeatFile(File file) {
		int nBytes = 32; // Header
		int nBeats = 0;
		for (ECGBeat b : beats) {
			if (b.isFirstInSeries()) {
				nBytes += 32; // Beat + first_in_series beat
				nBeats += 2;
			} else {
				nBytes += 16; // Beat only
				nBeats++;
			}
		}
		ByteBuffer bb = ByteBuffer.allocate(nBytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		FileChannel out = null;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			out = fos.getChannel();
			bb.put((byte) (66));
			bb.put((byte) (69));
			bb.put((byte) (65));
			bb.put((byte) (84));
			bb.put((byte) (1));
			bb.put((byte) (2));
			bb.put((byte) (3));
			bb.put((byte) (4));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (1));
			bb.put((byte) (64));
			bb.put((byte) (32));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.putInt(nBeats);
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			for (ECGBeat b : beats) {
				int bTime = (int) Math.round(b.getRPeakTime() / 1000);
				bb.putInt(bTime);
				bb.putInt(0);
				bb.putInt(0);
				if (b.isFirstInSeries()) {
					bb.putInt(1);
					bb.putInt(bTime);
					bb.putInt(0);
					bb.putInt(0);
				}
				bb.putInt(0);
			}
			bb.clear();
			out.write(bb);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "Error saving beat file",
					"Save error", JOptionPane.ERROR_MESSAGE);
		} finally {
			if (out != null) {
				try {
					out.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(getBeats()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setBeats(SortedSet<ECGBeat> newBeats, double leftTime, double rightTime) {
		beats.removeAll(beats.subSet(new ECGBeat(leftTime), new ECGBeat(rightTime)));
		beats.addAll(newBeats);
		CurrentOpenData.getInstance().setDirty(true);
		changeCount++;
	}

	public void setIBISuspicion(ECGBeat beat) {
		if (isNullOrFirstInSeries(beat))
			return;
		ECGBeat prevBeat = getPrevBeat(beat);
		if (isNullOrFirstInSeries(prevBeat))
			return;
		ECGBeat nextBeat = getNextBeat(beat);
		if (isNullOrFirstInSeries(nextBeat))
			return;
		ECGBeat next2Beat = getNextBeat(nextBeat);
		if (isNullOrFirstInSeries(next2Beat))
			return;
		double a, b, ymin, y, yplus;
		double ibimin = 60000000.0 / AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.MAXHR);
		double ibimax = 60000000.0 / AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.MINHR);
		a = nextBeat.getRPeakTime() - beat.getRPeakTime();
		b = beat.getRPeakTime() - prevBeat.getRPeakTime();
		if (a < ibimin || a > ibimax || b < ibimin || b > ibimax) {
			beat.setIBISuspicion(1001.0);
		} else {
			a /= 1000000;
			b /= 1000000;
			ymin = beat.getRPeakTime() - prevBeat.getRPeakTime();
			y = nextBeat.getRPeakTime() - beat.getRPeakTime();
			yplus = next2Beat.getRPeakTime() - nextBeat.getRPeakTime();
			ymin /= 1000;
			y /= 1000;
			yplus /= 1000;
			double d2y = 2 * (a * ymin - (a + b) * y + b * yplus) / (b * a * a + a * b * b);
			beat.setIBISuspicion(Math.abs(d2y));
		}
	}

	public void setSelectedSuspiciousIBI(int sel) {
		curSelectedSuspiciousIBI = sel;
	}

	public SortedSet<ECGBeat> subSet(double lTime, double rTime) {
		return beats.subSet(new ECGBeat(lTime), new ECGBeat(rTime));
	}

	private void updateIBISuspicionAroundTime(double time) {
		ECGBeat prevBeat = getBeatBeforeTime(time);
		ECGBeat prev2Beat = getPrevBeat(prevBeat);
		ECGBeat nextBeat = getBeatAfterTime(time);
		ECGBeat next2Beat = getNextBeat(nextBeat);
		setIBISuspicion(prevBeat);
		setIBISuspicion(prev2Beat);
		setIBISuspicion(nextBeat);
		setIBISuspicion(next2Beat);
		CurrentOpenData.getInstance().setDirty(true);
	}

}
