package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.tools.Utils;

public class AmsLabelSet extends LabelSet {
	private AmsLabelConfiguration lconf;
	private boolean displayexperimentdetails = false;

	@SuppressWarnings("unchecked")
	protected AmsLabelSet(AmsLabelSet that) {
		this.displayexperimentdetails = that.displayexperimentdetails;
		this.index = that.getlabelindex();
		this.labels = (TreeSet<AmsLabel>) that.labels.clone();
		this.lList = (TreeSet<AmsLabel>) that.lList.clone();
		this.timewidth = that.timewidth;
	}

	public AmsLabelSet() {
		index = 2;
	}

	public AmsLabelSet clone() {
		return new AmsLabelSet(this);
	}

	@Override
	public void add(AmsLabel lab) {
		add(lab, false, true);
	}

	public void add(AmsLabel lab, boolean signalparts) {
		add(lab, signalparts, true);
	}

	public void add(AmsLabel lab, boolean signalparts, boolean setDirty) {
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();
		labels.add(lab);
		if (setDirty)
			CurrentOpenData.getInstance().setDirty(true);
		if (signalparts) {
			ImpTab.getInstance().getECGDrawer().getSignalPartSet().addPart(lab, removeIndices);
			ImpTab.getInstance().getImpDrawer().getSignalPartSet().addPart(lab, removeIndices);
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
					d.getSignalPartSet().addPart(lab, removeIndices);
			ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().addPart(lab, removeIndices);
			ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().addPart(lab, removeIndices);
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.getSignalPartSet().addPart(lab, removeIndices);
		}
	}

	public void exportToLBLFile(File fl) {
		PrintWriter pw = null;
		try {
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			nf.setMaximumFractionDigits(2);
			nf.setMinimumFractionDigits(2);
			nf.setGroupingUsed(false);
			DateFormat df = new SimpleDateFormat("dd-MM-yy/HH:mm:ss");
			CurrentOpenData cod = CurrentOpenData.getInstance();
			lconf = cod.getLabelConfig();
			pw = new PrintWriter(fl);
			pw.println("# Labelfile    Starttime   Endtime");
			pw.print(Utils.guaranteeExtensionAndAddition(cod.getDataFile().getName(), "lbl", "_icg"));
			pw.print(" ");
			GregorianCalendar cal = Utils.getCalendarFromUS(cod.getStartTimeInUS());
			pw.print(df.format(cal.getTime()));
			pw.print(" ");
			cal = Utils.getCalendarFromUS(cod.getEndTimeInUS());
			pw.print(df.format(cal.getTime()));
			pw.println();
			pw.println("# Begin date/time    End date/time   Labelcode");

			for (AmsLabel l : labels) {
				cal = Utils.getCalendarFromUS(l.getLeftTime());
				pw.print(df.format(cal.getTime()) + "  ");
				cal = Utils.getCalendarFromUS(l.getRightTime());
				pw.print(df.format(cal.getTime()) + " ");
				for (Entry<String, String> entry : l.getAttributes().entrySet()) {
					if (!(entry.getKey().equalsIgnoreCase("Label No"))) {
						for (LabelValue lv : lconf.getConfig().get(entry.getKey())) {
							if (lv.getName().equals(entry.getValue())) {
								pw.print("  " + lv.getCode());
							}
						}
					}
				}
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

	public TreeSet<AmsLabel> getTimeLabels() {
		return lList;
	}

	public void importFromFileusingMarkers(File fl, int type) {
		importFromFileusingMarkerinfo(fl, type);
	}

	public void importFromFile(File fl) {
		importFromFile(fl, (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone(),
				CurrentOpenData.getInstance().getStartTimeInUS());
	}

	public void importFromFileusingMarkerinfo(File fl, int type) {
		// -------------- Instead of label.cfg file existing in the same
		// directory---------------
		TreeMap<String, ArrayList<String>> retMap = new TreeMap<String, ArrayList<String>>();
		for (String cat : CurrentOpenData.getInstance().getLabelConfig().getCategories()) {
			ArrayList<LabelValue> vals = CurrentOpenData.getInstance().getLabelConfig().getConfig().get(cat);

			for (LabelValue lv : vals) {
				String lin = String.valueOf(lv);
				String[] n = lin.split(" ");
				ArrayList<String> addList = new ArrayList<String>();
				addList.add(cat);
				String fS = "";
				for (int i = 1; i < n.length; i++) {
					fS += n[i];
				}
				for (int i = 0; i < vals.size(); i++) {
					String mS = vals.get(i).getName().replaceAll("\\s", ""); // Remove spaces
					if (mS.equalsIgnoreCase(fS)) {
						addList.add(vals.get(i).getName());
					}
				}
				retMap.put(n[0], addList);
			}
		}
		// -------------------------------------------------------------------------------------

		BufferedReader read = null;
		double marker1 = 0;
		double marker2 = 0;
		double duration1_type2 = 0;
		double duration2_type2 = 0;
		try {
			read = new BufferedReader(new FileReader(fl));
			String line1, line2;
			line1 = read.readLine(); // Read the Header
			if (line1 != null) {
				line2 = read.readLine(); // Read the second line
				if (line2 != null) {
					line1 = line2;
					line2 = read.readLine();
					labels.clear();
					ImpTab.getInstance().getImpDrawer().clear();
					ImpTab.getInstance().getECGDrawer().clear();
					if (ImpTab.getInstance().getDrawers() != null)
						for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
							d.clear();
					ImpTab.getInstance().getImpRawDrawer().clear();
					ImpTab.getInstance().getECGRawDrawer().clear();
					if (ImpTab.getInstance().getFiltDrawers() != null)
						for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
							prt.clear();
					while (line1 != null) {
						if (line1.length() > 1) {
							if (Character.isDigit(line1.charAt(0))) {
								line1 = line1.trim();
								String[] values = line1.split(",");
								// ---------- To remove spaces---------------------
								for (int i = 0; i < values.length; i++) {
									values[i] = (values[i].replaceAll("\\s+", ""));
								}
								// -----------------------------------------------
								if (values.length >= 2) {
									try {
										if (type == 0) { // Combined template (before & after)

											/*
											 * values[0] = Start Marker/Reference marker (M1)
											 * values[1] = End Marker (M2)
											 * values[2] = Duration before Reference marker (D1)
											 * values[3] = Duration after Reference marker (D2)
											 * values[4] = Label code
											 * 
											 * If M1 and M2 are not equal to -9999, then create a label between M1 and
											 * M2
											 * 
											 * M1 should never be equal to -9999 since this is the reference marker
											 * 
											 * If(D1 and D2) != -9999, then create a label from (M1-D1)
											 * D1 should always be negative
											 * D2 should always be positive
											 */

											double M1 = 0;
											double M2 = 0;

											double D1 = 0;
											double D2 = 0;

											ArrayList<String> attr = new ArrayList<String>();
											for (int i = 4; i < values.length; i++) {
												ArrayList<String> attrs = retMap.get(values[i]);
												if (attrs != null)
													attr.addAll(attrs);
											}
											Integer code1 = Utils.parseInt(values[0]);
											Integer code2 = Utils.parseInt(values[1]);

											for (Ams5fsPacket ev : CurrentOpenData.getInstance().getEvents()) {

												if ((ev.getlType() == 0) || (ev.getlType() == 1)
														|| (ev.getlType() == 2) || ev.getlType() == 100) {
													Integer eventCode = ev.getlCode();
													if (!values[0].equals("-9999") && code1 != null) { // SM is present
														if (M1 == 0 && eventCode != null) {
															if (code1.equals(eventCode)) {
																M1 = ev.getDwClockTick_ms() * 1000.;
															}
														}

													}
													if (!values[1].equals("-9999") && code1 != null) { // EM is present
														if (code2.equals(eventCode)) {
															if (eventCode.equals(code2)) {
																M2 = ev.getDwClockTick_ms() * 1000.;
															}
														}
													}

													if (M2 == M1) {
														M2 = 0;
													}

													if (!values[2].equals("-9999")) { // D1
														D1 = Double.valueOf(values[2]) * 1000000;
													}

													if (!values[3].equals("-9999")) { // D2
														D2 = Double.valueOf(values[3]) * 1000000;
													}

													if ((M1 != 0) && (M2 != 0)) { // Both markers are present. Create a
																					// label between M1 and M2
														double mT = (M1 + M2) / 2;
														if ((isTimeUnderLabel(mT) == true)) { // Marker 1 & 2 are
																								// non-zero and there is
																								// no label - So create
																								// one
															M1 = 0;
															M2 = 0;
														} else {
															if (M2 > M1) {
																break;
															} else {
																M2 = 0;
															}
														}

													} else if ((M1 != 0 && (!values[2].equals("-9999")
															|| !values[3].equals("-9999")))) {
														// See if there's a label in the Middle of the to-be-placed
														// label:
														Double Mid = M1 + 1000000 * (-Double.valueOf(values[2])
																+ Double.valueOf(values[3])) / 2;

														if (!isTimeUnderLabel(Mid))
															break;
														else
															M1 = 0;// Keep looping through markers
													}
												}
											}

											if (M1 != 0 && M2 != 0) {
												this.add(new AmsLabel(M1, M2, false, 0.0,
														attr.toArray(new String[attr.size()])), true);
												M1 = 0;
												M2 = 0;
												D1 = 0;
												D2 = 0;
											} else if (M1 != 0 && D1 != 0 && D2 != 0) {
												this.add(new AmsLabel(M1 - D1, M1 + D2, false, 0.0,
														attr.toArray(new String[attr.size()])), true);
												M1 = 0;
												M2 = 0;
												D1 = 0;
												D2 = 0;
											} else if (M1 != 0 && D1 != 0) {
												this.add(new AmsLabel(M1 - D1, M1, false, 0.0,
														attr.toArray(new String[attr.size()])), true);
												M1 = 0;
												M2 = 0;
												D1 = 0;
												D2 = 0;
											} else if (M1 != 0 && D2 != 0) {
												this.add(new AmsLabel(M1, M1 + D2, false, 0.0,
														attr.toArray(new String[attr.size()])), true);
												M1 = 0;
												M2 = 0;
												D1 = 0;
												D2 = 0;
											}
										} else if (type == 1) {
											Integer code1 = Utils.parseInt(values[0]);
											Integer code2 = Utils.parseInt(values[1]);
											// Read the marker code
											for (Ams5fsPacket ev : CurrentOpenData.getInstance().getEvents()) {
												if ((ev.getlType() == 0) || (ev.getlType() == 1)
														|| (ev.getlType() == 2 || ev.getlType() == 100)) {

													Integer eventCode = ev.getlCode();
													if (marker1 == 0 && code1 != null && eventCode != null) {
														if (eventCode.equals(code1)) { // Start
																						// marker
															marker1 = ev.getDwClockTick_ms() * 1000.;
														}
													}

													if (marker2 == 0 && code2 != null && eventCode != null) {
														if (eventCode.equals(code2)) { // End marker
															marker2 = ev.getDwClockTick_ms() * 1000.;
														}
													}

													if (marker2 == marker1) {
														marker2 = 0;
													}
													if ((marker1 != 0) && (marker2 != 0)) {
														double mT = (marker1 + marker2) / 2;
														if ((isTimeUnderLabel(mT) == true)) { // Marker 1 & 2 are
																								// non-zero and there is
																								// no label - So create
																								// one
															marker1 = 0;
															marker2 = 0;
														} else {
															if (marker2 > marker1) {
																// break;
															} else {
																marker2 = 0;
															}
														}
													}
												}

												if (marker1 != 0 && marker2 != 0) {
													duration1_type2 = Double.valueOf(values[2]) * 1000000;
													duration2_type2 = Double.valueOf(values[3]) * 1000000;

													ArrayList<String> attr = new ArrayList<String>();
													for (int i = 4; i < values.length; i++) {
														ArrayList<String> attrs = retMap.get(values[i]);
														if (attrs != null)
															attr.addAll(attrs);
													}
													// System.out.println("Adding Labels");
													this.add(new AmsLabel(marker1 + duration1_type2,
															marker2 + duration2_type2, false, 0.0,
															attr.toArray(new String[attr.size()])), true);
													marker1 = 0;
													marker2 = 0;
												}
											}
										} else if (type == 2) {
											Integer code1 = Utils.parseInt(values[0]);
											// Read the marker code
											for (Ams5fsPacket ev : CurrentOpenData.getInstance().getEvents()) {
												if ((ev.getlType() == 0) || (ev.getlType() == 1)
														|| (ev.getlType() == 2) || ev.getlType() == 100) {
													Integer eventCode = ev.getlCode();

													if (code1 != null && eventCode != null && eventCode.equals(code1)) { // Start
																															// marker
														marker1 = ev.getDwClockTick_ms() * 1000.;
													}
													duration1_type2 = Double.valueOf(values[1]) * 1000000;
													duration2_type2 = Double.valueOf(values[2]) * 1000000;

													if (marker1 != 0 && duration1_type2 != 0) {
														double mT = (marker1 + (duration1_type2 / 2)
																+ (duration2_type2 / 2));
														if ((isTimeUnderLabel(mT) == true)) {
															marker1 = 0;
															// }else{
															// break;
														}
													}
												}
												if (marker1 != 0 && duration1_type2 < duration2_type2) {
													ArrayList<String> attr = new ArrayList<String>();
													for (int i = 2; i < values.length; i++) {
														ArrayList<String> attrs = retMap.get(values[i]);
														if (attrs != null)
															attr.addAll(attrs);
													}
													this.add(new AmsLabel(marker1 + duration1_type2,
															marker1 + duration2_type2, false, 0.0,
															attr.toArray(new String[attr.size()])), true);
													marker1 = 0;
													duration1_type2 = 0;
												}
											}
										}

									} catch (Exception e) {
										e.printStackTrace();
									}

								} else {
									JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
											"Errors detected in few lines of the created text file",
											"Label Import Error",
											JOptionPane.ERROR_MESSAGE);
									break;
								}
							} else {
								JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
										"Provided format of text file is not valid. Please use comma separated text file",
										"Label Import Error",
										JOptionPane.ERROR_MESSAGE);
								break;
							}
						}
						line1 = line2;
						line2 = read.readLine();
					}
				}
			}
			CurrentOpenData.getInstance().setDirty(true);
			// ----------------------------------------------------------------------------------------
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (read != null) {
				try {
					read.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void importFromFile(File fl, GregorianCalendar startDate, long startTimeInUS) {

		// Labels when imported from SubjectName_icg.lbl file will be fitted between the
		// start and end time of recording of the currently opened VU-DAMS File
		// Works for both .5fs and .amsdata formats

		// There are two scenarios
		// 1. Normal .lbl files. These files are exported using VU-DAMS
		// 2. Created .lbl files which suits particular experiment. This file does not
		// hold the start and end time of the recording.

		// For 5fs files - Automatically imports labels when the .lbl file in placed in
		// the same folder as the 5fs file and has similar file name
		// 1. When the recording date of the 5fs file and the date in the label file are
		// the same, then there is no need for corrections.
		// 2. When the recording date of the 5fs file and the date in the label file are
		// not the same, then there corrections are done.

		// For amsdata files - Label are drawn only when the .lbl file is imported from
		// VU-DAMS software
		// 1. When the recording date of the amsdata file and the date in the label file
		// are the same, then there is no need for corrections.
		// 2. When the recording date of the amsdata file and the date in the label file
		// are not the same, then there corrections are done.
		TreeMap<String, ArrayList<String>> map = new TreeMap<String, ArrayList<String>>();
		File lblcfgFile = new File(fl.getParent(), "label.cfg");
		if (lblcfgFile.exists() == false) {
			// JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No
			// label.cfg in label file directory", "Label Import Error",
			// JOptionPane.ERROR_MESSAGE);
			// return;
			// -------------- Instead of label.cfg file existing in the same
			// directory---------------
			for (String cat : CurrentOpenData.getInstance().getLabelConfig().getCategories()) {
				ArrayList<LabelValue> vals = CurrentOpenData.getInstance().getLabelConfig().getConfig().get(cat);

				for (LabelValue lv : vals) {
					String lin = String.valueOf(lv);
					String[] n = lin.split(" ");
					ArrayList<String> addList = new ArrayList<String>();
					addList.add(cat);
					String fS = "";
					for (int i = 1; i < n.length; i++) {
						fS += n[i];
					}
					for (int i = 0; i < vals.size(); i++) {
						String mS = vals.get(i).getName().replaceAll("\\s", ""); // Remove spaces
						if (mS.equalsIgnoreCase(fS)) {
							addList.add(vals.get(i).getName());
						}
					}
					map.put(n[0], addList);
				}
			}
			// -------------------------------------------------------------------------------------
		} else {
			CurrentOpenData.getInstance().getLabelConfig().getConfigFromFile(lblcfgFile);
			map = CurrentOpenData.getInstance().getLabelConfig().getNumberMapFromConfigFile(lblcfgFile);
		}
		ArrayList<Ams5fsPacket> events = CurrentOpenData.getInstance().getEvents();

		BufferedReader read = null;

		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy/HH:mm:ss");
		SimpleDateFormat finalnewdf = new SimpleDateFormat("dd-MM-yyyy/HH:mm:ss");

		double caldistance = 0;
		double differencebetweenlabelandstart = 0;
		double lefttimedistance = 0;
		double righttimedistance = 0;
		double lengthofthelabelinus = 0;
		int count = 0;

		Date VUDAMSFileBegDate = null;
		Date VUDAMSFileEndDate = null;
		Date begDate = null;
		Date endDate = null;
		Date prevLabelDate = null;
		Date firstlabeldate = null;
		Date startdateandtimeinlabelfile = null;

		String[] values = null;
		String[] line1values = null;
		String[] recordinginfoiinlabelfile = null;

		ArrayList<String> attrs = new ArrayList<String>();
		ArrayList<String> attr = new ArrayList<String>();

		AmsLabelConfiguration lblcfg = CurrentOpenData.getInstance().getLabelConfig();
		ArrayList<String> categories = lblcfg.getCategories();
		TreeSet<AmsLabel> templabels = new TreeSet<AmsLabel>();

		boolean isFirstLabel = false;
		long julianDayNumber1 = 0;
		long julianDayNumber2 = 0;
		try {
			read = new BufferedReader(new FileReader(fl));
			String line1;
			line1 = read.readLine();
			while (line1 != null && (line1.startsWith("#") || line1.startsWith("Label") || line1.equals("")))
				line1 = read.readLine();

			VUDAMSFileBegDate = df.parse(Utils.getDateAndTimeFromUS(startTimeInUS, startDate, startTimeInUS));
			VUDAMSFileEndDate = df.parse(Utils.getDateAndTimeFromUS(startTimeInUS,
					CurrentOpenData.getInstance().getEndDate(), startTimeInUS));

			if (line1 != null) {

				line1values = line1.split("\\s+");
				recordinginfoiinlabelfile = line1.split("\\s+");
				int lineLength = recordinginfoiinlabelfile.length;
				if (lineLength > 1)
					startdateandtimeinlabelfile = Utils.parseDate(df, recordinginfoiinlabelfile[lineLength - 2]);
				final long MILLIS_PER_DAY = 86400000L;
				// Strip out the time part of each date.
				julianDayNumber1 = VUDAMSFileBegDate.getTime() / MILLIS_PER_DAY;
				if (startdateandtimeinlabelfile != null)
					julianDayNumber2 = startdateandtimeinlabelfile.getTime() / MILLIS_PER_DAY;
				if (julianDayNumber1 != julianDayNumber2) {
					int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
							"The begin date in the label file does not exist or is not the same as in the data. If you use this file relative start and end times will be calculated for the labels. Continue with this label file?",
							"Wrong label date", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.NO_OPTION) {
						return;
					}
				}
				labels.clear();
				ImpTab.getInstance().getImpDrawer().clear();
				ImpTab.getInstance().getECGDrawer().clear();
				if (ImpTab.getInstance().getDrawers() != null)
					for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
						d.clear();
				ImpTab.getInstance().getImpRawDrawer().clear();
				ImpTab.getInstance().getECGRawDrawer().clear();
				if (ImpTab.getInstance().getFiltDrawers() != null)
					for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
						prt.clear();
			}

			line1 = read.readLine();
			while (line1 != null) {
				if (line1.length() > 1) {
					if (!line1.startsWith("#") && Character.isDigit(line1.charAt(1))) {
						line1 = line1.trim();
						values = line1.split("\\s+");
						if (count == 0) {
							firstlabeldate = Utils.parseDate(df, values[0].toString());
							if (firstlabeldate != null && startdateandtimeinlabelfile != null) {
								if (line1values[0].equals("Label")) {
									differencebetweenlabelandstart = ((firstlabeldate.getTime()
											- startdateandtimeinlabelfile.getTime()) * 1000); // us
								} else {
									differencebetweenlabelandstart = ((firstlabeldate.getTime()
											- startdateandtimeinlabelfile.getTime()) * 1000); // us
								}
							}
						}
						begDate = Utils.parseDate(df, values[0].toString());
						endDate = Utils.parseDate(df, values[1].toString());
						count += 1;
						if ((begDate != null) && (endDate != null)) {
							if (endDate.before(begDate)) {
								int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
										"The end time of label " + count + " (start date " + begDate
												+ ") lies before its begin time. If you continue using this label file this label will not be imported. Continue with this label file?",
										"Wrong label dates", JOptionPane.YES_NO_OPTION);
								if (res == JOptionPane.NO_OPTION) {
									return;
								}
							}

							// If they now are equal then it is the same day.
							if (julianDayNumber1 == julianDayNumber2) {
								if (begDate.before(VUDAMSFileBegDate) && endDate.before(VUDAMSFileBegDate)) {
									int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
											"The begin and end times of label " + count + " (start date " + begDate
													+ ") lie before the begin time of the data. If you continue using this label file this label will not be imported. Continue with this label file?",
											"Possible non synchronous labels", JOptionPane.YES_NO_OPTION);
									if (res == JOptionPane.NO_OPTION) {
										return;
									}
								} else if (begDate.before(VUDAMSFileBegDate)) {
									int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
											"The begin time of label " + count + " (start date " + begDate
													+ ") lies before the begin time of the data. If you continue using this label file this label will be truncated. Continue with this label file?",
											"Possible non synchronous labels", JOptionPane.YES_NO_OPTION);
									if (res == JOptionPane.NO_OPTION) {
										return;
									}
								}
								if (endDate.after(VUDAMSFileEndDate) && begDate.after(VUDAMSFileEndDate)) {
									int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
											"The begin and end times of label " + count + " (start date " + begDate
													+ ") lie after the end time of the data. If you continue using this label file this label will not be imported. Continue with this label file?",
											"Possible non synchronous labels", JOptionPane.YES_NO_OPTION);
									if (res == JOptionPane.NO_OPTION) {
										return;
									}
								} else if (endDate.after(VUDAMSFileEndDate)) {
									int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
											"The begin time of label " + count + " (start date " + begDate
													+ ") lies after the end time of the data. If you continue using this label file this label will be truncated. Continue with this label file?",
											"Possible non synchronous labels", JOptionPane.YES_NO_OPTION);
									if (res == JOptionPane.NO_OPTION) {
										return;
									}
								}

								String[] att = new String[categories.size() * 2];
								for (int i = 2; i < values.length; i++) {
									attrs = map.get(values[i]);
									if (attrs != null)
										attr.addAll(attrs);
								}
								for (int j = 0; j < att.length; j += 2) {
									att[j] = categories.get(j / 2);
									if (j + 1 < attr.size())
										att[j + 1] = attr.get(j + 1);
								}
								attr = new ArrayList<String>();
								if (begDate.before(VUDAMSFileBegDate))
									begDate = VUDAMSFileBegDate;
								if (endDate.after(VUDAMSFileEndDate))
									endDate = VUDAMSFileEndDate;
								if (endDate.after(begDate))
									templabels
											.add(new AmsLabel(Utils.getTimeFromDate(begDate, startDate, startTimeInUS),
													Utils.getTimeFromDate(endDate,
															startDate, startTimeInUS),
													false, 0.0, att));
								MainFrame.getInstance().getMainFrame().repaint();
								begDate = null;
								endDate = null;
							} else {
								// System.out.println("Not Equal date");
								if (caldistance == 0) {
									isFirstLabel = true;
									prevLabelDate = begDate;
									caldistance = Math
											.abs((startdateandtimeinlabelfile.getTime() - VUDAMSFileBegDate.getTime())
													* 1000)
											+ differencebetweenlabelandstart;
								} else {
									isFirstLabel = false;
									caldistance = ((begDate.getTime() - prevLabelDate.getTime()) * 1000)
											+ differencebetweenlabelandstart;
								}
								lengthofthelabelinus = (endDate.getTime() - begDate.getTime()) * 1000;
								if (isFirstLabel == true) {
									lefttimedistance = differencebetweenlabelandstart;
									righttimedistance = (differencebetweenlabelandstart + lengthofthelabelinus);
								} else {
									lefttimedistance = (((VUDAMSFileBegDate.getTime() * 1000) + (caldistance))
											- (VUDAMSFileBegDate.getTime() * 1000))
											+ (CurrentOpenData.getInstance().getStartTimeInUS()); /// ((CurrentOpenData.getInstance().getStartTimeInUS())))+CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms())*1000;
									righttimedistance = (((VUDAMSFileBegDate.getTime() * 1000) + (caldistance)
											+ lengthofthelabelinus) - (VUDAMSFileBegDate.getTime() * 1000))
											+ (CurrentOpenData.getInstance().getStartTimeInUS()); // CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms())*1000;
								}
								GregorianCalendar date1 = startDate;

								String newlefttime = (Utils.getDateAndTimeFromUS((lefttimedistance), date1,
										(CurrentOpenData.getInstance().getStartTimeInUS())));
								String newrighttime = (Utils.getDateAndTimeFromUS((righttimedistance), date1,
										(CurrentOpenData.getInstance().getStartTimeInUS())));

								Date newleftdate = finalnewdf.parse(newlefttime);
								Date newrightdate = finalnewdf.parse(newrighttime);

								String[] att = new String[categories.size() * 2];
								for (int i = 2; i < values.length; i++) {
									attrs = map.get(values[i]);
									if (attrs != null)
										attr.addAll(attrs);
								}
								for (int j = 0; j < att.length; j++) {
									if (j == attr.size())
										break;
									att[j] = attr.get(j);
								}

								templabels.add(new AmsLabel(
										Utils.getTimeFromDate(newleftdate, startDate,
												CurrentOpenData.getInstance().getStartTimeInUS()),
										Utils.getTimeFromDate(newrightdate,
												startDate, CurrentOpenData.getInstance().getStartTimeInUS()),
										false, 0.0, att));

								MainFrame.getInstance().getMainFrame().repaint();
								begDate = null;
								endDate = null;
							}
						} else if (begDate != null) {
							Integer code = Utils.parseInt(values[1]);
							String message = "";
							if (code != null) {
								for (int i = 2; i < values.length; i++) {
									if (message.length() > 45)
										break;
									message = message + values[i] + " ";
								}
							} else {
								code = -1;
								for (int i = 1; i < values.length; i++) {
									if (message.length() > 45)
										break;
									message = message + values[i] + " ";
								}
							}
							Ams5fsPacket event = new Ams5fsPacket();
							Double clockTick = Utils.getTimeFromDate(begDate, startDate, startTimeInUS) / 1000.0;
							event.setDwClockTick_ms(clockTick.longValue());
							event.setlType(100);
							event.setlCode(code);
							event.setSzMessage(message);
							events.add(event);
						}
					}
				}
				line1 = read.readLine();
			}
			for (AmsLabel l : templabels)
				this.add(l, true);
			CurrentOpenData.getInstance().setDirty(true);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (read != null) {
				try {
					read.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean isOverlapping() {
		for (AmsLabel l : getLabels()) {
			for (AmsLabel l2 : getLabels()) {
				if (l.equals(l2) == false) {
					if (l.getLeftTime() > l2.getLeftTime() && l.getLeftTime() < l2.getRightTime()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void saveonlyExplabelsToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

		TreeSet<AmsLabel> outputLabels = new TreeSet<AmsLabel>();
		for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
			if (l.isTimeLabel() == false) {
				outputLabels.add(l);
			}
		}
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(outputLabels));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void savetimewidthtoJSON(File file) {

		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		if (getTimewidth() > 0) {
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
				writer.print(gson.toJson(getTimewidth()));
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
				writer.print("0.0");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void savetimelabelstoJSON(File file) {

		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		TreeSet<AmsLabel> outputLabels = new TreeSet<AmsLabel>();
		for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
			if (l.isTimeLabel() == true) {
				outputLabels.add(l);
			}
		}

		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(outputLabels));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void setToTimeLabels(double tW) {
		String[] attr = { "Time label", "Time" };
		int count = 1;

		if (getStateoflabels() == true) {
			// getStateofLabels() - Information got from label tool bar.
			// Display experiment labels == true when the researcher chooses to have
			// experiment details in his time labels

			AmsLabelConfiguration lblcfg = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> categories = lblcfg.getCategories();
			double ltime = CurrentOpenData.getInstance().getStartTimeInUS() / (1000 * 60);
			double rtime = CurrentOpenData.getInstance().getEndTimeInUS() / (1000 * 60);
			double labelspossible = ((rtime - ltime) / 1000);

			if (labels.size() < (labelspossible - 10)) {
				lList = (TreeSet<AmsLabel>) labels.clone();
			}

			setTimewidth(tW / 1000000);

			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			double endTime = CurrentOpenData.getInstance().getEndTimeInUS();
			double curTime = startTime;
			Iterator<AmsLabel> iter = labels.iterator();

			while (iter.hasNext()) {
				AmsLabel l = iter.next();

				if (l.isTimeLabel())
					iter.remove();
			}
			// labels.clear();

			while ((curTime + tW) < endTime) {
				boolean condition = true;

				int newType = 0;
				for (AmsLabel l : lList) {

					String[] values = new String[categories.size()];
					String[] attributes = new String[(2 * categories.size()) + 2];

					if (curTime < l.getLeftTime()) {
						newType = 1;
						double diff1 = (Math.abs(curTime - l.getLeftTime())) / 1000000;
						if (diff1 < 18) {
							newType = 2;
						}
					} else if (curTime >= l.getLeftTime()) {

						if ((curTime + tW) <= l.getRightTime()) {
							newType = 3;
							// System.out.println("Within the label:_ " + curTime);
						} else {
							double diff4 = Math.ceil((Math.abs((curTime + tW) - l.getRightTime())) / 1000000);
							// System.out.println("Diff4:_ " + diff4);
							if (diff4 <= 1) {
								newType = 3;
								// System.out.println("Exactly equal to 18:_ " + curTime);
							} else if (diff4 < 18) { // 70%
								newType = 2;
								// System.out.println("Curtime+tw is outside l.getrighttime:_ " + curTime);
							} else {
								newType = 1;
								// System.out.println("Diff4 is greater than 18:_ " + curTime);
							}
						}

					} else if ((curTime == l.getLeftTime()) || (curTime == l.getRightTime())) {
						newType = 3;

					} else {

					}

					if (newType == 2 || newType == 3) {

						if (newType == 2) {
							for (int i = 0; i < (categories.size() + 1); i++) {
								if (i < categories.size()) {
									values[i] = l.getAttributes().get(categories.get(i));
									attributes[2 * i] = categories.get(i);
									attributes[(2 * i) + 1] = values[i];
								} else {
									attributes[2 * i] = "Interpolated";
									attributes[(2 * i) + 1] = "True";
								}
							}
							this.add(new AmsLabel(curTime, (curTime + tW), true, tW / 1000000, attributes), true);
						} else if (newType == 3) {
							for (int i = 0; i < (categories.size() + 1); i++) {
								if (i < categories.size()) {
									values[i] = l.getAttributes().get(categories.get(i));
									attributes[2 * i] = categories.get(i);
									attributes[(2 * i) + 1] = values[i];
								} else {
									attributes[2 * i] = "Interpolated";
									attributes[(2 * i) + 1] = "False";
								}

							}
							this.add(new AmsLabel(curTime, (curTime + tW), true, tW / 1000000, attributes), true);
						} else {
							attr[1] = "Time label no " + count;
							this.add(new AmsLabel(curTime, (curTime + tW), true, tW / 1000000), true);
							count++;
						}
						condition = false;
					}
				}

				if (condition) {
					attr[1] = "Time label no " + count;
					this.add(new AmsLabel(curTime, (curTime + tW), true, tW / 1000000), true);
					count++;
				}

				try {
				} catch (Exception e) {
					System.out.println("exception" + e);
				}
				curTime += tW;
			}
		} else {
			Iterator<AmsLabel> iter = labels.iterator();

			while (iter.hasNext()) {
				AmsLabel l = iter.next();

				if (l.isTimeLabel())
					iter.remove();
			}
			// labels.clear();
			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			double endTime = CurrentOpenData.getInstance().getEndTimeInUS();
			double curTime = startTime;
			setTimewidth(tW / 1000000);
			while (curTime + tW < endTime) {
				attr[1] = "Time label no " + count;
				this.add(new AmsLabel(curTime, curTime + tW, true, tW / 1000000, attr), true);
				curTime += tW;
				count++;
			}
		}
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void setStateoflabels(boolean state) {
		displayexperimentdetails = state;
	}

	public boolean getStateoflabels() {
		return displayexperimentdetails;
	}

	public double getTimewidth() {
		return timewidth;
	}

	public void setTimewidth(double time) {
		timewidth = time;
	}

	public void settoExpLabels() {

		String inString;
		File curFile;
		Type collectionType;
		Gson gson = new Gson();
		LinkedList<Object> tempList;

		curFile = new File(CurrentOpenData.getInstance().getFilePath(), "timelabels.json");
		if (curFile.exists()) {
			labels.clear();
			ImpTab.getInstance().getImpDrawer().clear();
			ImpTab.getInstance().getECGDrawer().clear();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
					d.clear();
			ImpTab.getInstance().getImpRawDrawer().clear();
			ImpTab.getInstance().getECGRawDrawer().clear();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.clear();
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				add((AmsLabel) o, true);
			}
			CurrentOpenData.getInstance().setDirty(true);
		}
	}

	@Override
	public TreeSet<AmsLabel> getLabels() {
		try {
			if (CurrentOpenData.getInstance().getLoadLabelThread() != null)
				CurrentOpenData.getInstance().getLoadLabelThread().join();
			if (CurrentOpenData.getInstance().getTimeLabelThread() != null)
				CurrentOpenData.getInstance().getTimeLabelThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (index == 0)
			return labels;
		TreeSet<AmsLabel> output = new TreeSet<AmsLabel>();
		for (AmsLabel l : labels) {
			if ((l.isTimeLabel() && index == 2) || (!l.isTimeLabel() && index == 1))
				output.add(l);
		}
		return output;
	}

	@Override
	public void clear() {
		labels.clear();
		ImpTab.getInstance().getImpDrawer().clear();
		ImpTab.getInstance().getECGDrawer().clear();
		if (ImpTab.getInstance().getDrawers() != null)
			for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
				prt.clear();
		ImpTab.getInstance().getImpRawDrawer().clear();
		ImpTab.getInstance().getECGRawDrawer().clear();
		if (ImpTab.getInstance().getFiltDrawers() != null)
			for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
				prt.clear();
		CurrentOpenData.getInstance().setDirty(true);
	}

	public TreeSet<AmsLabel> getAllLabels() {
		return labels;
	}
}
