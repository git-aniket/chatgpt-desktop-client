package nl.vu.psy.ams.suite.data;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
//import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.posture.ActivityClassification;
import nl.vu.psy.ams.suite.data.posture.PostureClassifier;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.EventRelatedSCLCycle;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.SCLCycle;
import nl.vu.psy.ams.suite.data.structures.Step;
// import nl.vu.psy.ams.suite.data.structures.SignalPart;
// import nl.vu.psy.ams.suite.data.structures.Step;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
// import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.MetaLabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.data.structures.sets.SCLArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.SignalPartSet;
import nl.vu.psy.ams.suite.data.structures.sets.SkinConductanceSet;
import nl.vu.psy.ams.suite.data.structures.sets.StepSet;
import nl.vu.psy.ams.suite.data.structures.sets.SubSetIbiSet;
import nl.vu.psy.ams.suite.gui.ExternalFilePanel;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.MainMenuBar;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.StartTab;
import nl.vu.psy.ams.suite.gui.tabs.actigraph.ActigraphTab;
//import nl.vu.psy.ams.suite.gui.tabs.freq.FrequencyTab;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.info.LabelInformationTab;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.gui.tabs.qrs.QRSTab;
import nl.vu.psy.ams.suite.gui.tabs.rsa.RSATab;
import nl.vu.psy.ams.suite.gui.tabs.scl.SCLTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.AppPinger;
import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.SCLValueConvertor;
import nl.vu.psy.ams.suite.tools.ThreadServer;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;
import nl.vu.psy.ams.suite.tools.VUDAMSDebugSettings;

import org.apache.logging.log4j.Logger;
// import org.apache.poi.ss.usermodel.Color;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/*
 * Large singleton class that contains all information about the
 * current open data set, including the header file and all other information
 * from the original .5fs file and everything the user has added
 * afterwards (like detected beats etc). Use this class to obtain this information from
 * all other code.
 */

public class CurrentOpenData {

	private static Logger logger = LogManager.getLogger(CurrentOpenData.class.getName());

	public static Logger getLogger() {
		return logger;
	}

	private File filePath;

	private boolean isOpen;
	private GregorianCalendar startDate;
	private GregorianCalendar endDate;

	private long startTimeInUS;
	private ArrayList<Ams5fsPacket> events, statics, starts, summaries, settings, others;
	// private ArrayList<ArrayList<Ams5fsPacket>> offsets;
	private ArrayList<Ams7fsChannelInfo> channelInfo;

	public Ams7fsHeader fileHeader;
	private VUDAMSDebugSettings debugSettings;
	private long endTimeInUS;
	private BeatSet beatSet, bsTemp2, bsTemp3;
	private List<BeatSet> beatSets = new ArrayList<BeatSet>();
	private SubSetIbiSet ibiSubSet;
	private List<SubSetIbiSet> ibiSubSets = new ArrayList<SubSetIbiSet>();
	private int activeBeatset = 0;
	private String ecgFile = "FILTECG.bin";
	private ArtefactSet ecgArtefacts = new ArtefactSet();
	private SCLArtefactSet sclArtefacts = new SCLArtefactSet();
	private SkinConductanceSet sclset = new SkinConductanceSet();
	private EventRelatedSCLSet eventsclset = new EventRelatedSCLSet();
	private AmsLabelSet labels = new AmsLabelSet();
	private LabelSet postureLabels = new LabelSet();
	private LabelSet speechLabels = new LabelSet();
	private LabelSet stairsLabels = new LabelSet();
	private LabelSet activityLabels = new LabelSet();
	private MetaLabelSet metaLabels = new MetaLabelSet();
	private StepSet stepSet = new StepSet();

	private File dataFile = null;

	private AmsLabelConfiguration labelConfig = new AmsLabelConfiguration();
	private static CurrentOpenData instance;
	public int labelsize = 0;
	private boolean batchexport = false;
	private int analysistype = 0;
	private boolean addFileVersiontoHeader = false;
	private boolean addbloodresistivity = false;
	private double bloodresistivity = 135.00;
	public static final int AMSII_ERROR_WATCHDOG = 1;
	public static final int AMSII_ERROR_RINGBUFFER_OVERFLOW = 2;
	public boolean debugFileexists = false;
	public boolean isFileInfoMissing = false;
	public ArrayList<File> dirtyFiles = new ArrayList<File>();
	private boolean isDirty = false;
	private Thread loadLabels, createTimeLabels;
	private SignalPartSet tempSignalPartSet;

	public static CurrentOpenData getInstance() {
		if (instance == null)
			instance = new CurrentOpenData();
		return instance;
	}

	private RespirationSet respSet = new RespirationSet();

	private short fileVersion;

	private CurrentOpenData() {
		setOpen(false);
		events = new ArrayList<Ams5fsPacket>();
		statics = new ArrayList<Ams5fsPacket>();
		starts = new ArrayList<Ams5fsPacket>();
		summaries = new ArrayList<Ams5fsPacket>();
		settings = new ArrayList<Ams5fsPacket>();
		others = new ArrayList<Ams5fsPacket>();
		// offsets = new ArrayList<ArrayList<Ams5fsPacket>>();
		channelInfo = new ArrayList<Ams7fsChannelInfo>();
	}

	/*
	 * public void changeStartingDate(Date newDate) {
	 * long newDateInMS = newDate.getTime();
	 * long oldDateInMS = getStartDate().getTimeInMillis();
	 * long diffTimeInMS = newDateInMS - oldDateInMS;
	 * for (Ams5fsPacket start : starts) {
	 * start.gettStamp().AddMilliseconds(diffTimeInMS);
	 * }
	 * PrintWriter writer = null;
	 * Gson gson = new GsonBuilder().setPrettyPrinting().create();
	 * try {
	 * writer = new PrintWriter(new BufferedWriter(new FileWriter(new
	 * File(getFilePath(), "starts.json"))));
	 * writer.print(gson.toJson(starts));
	 * } catch (IOException e) {
	 * e.printStackTrace();
	 * } finally {
	 * writer.close();
	 * }
	 * 
	 * try {
	 * ReOpen();
	 * } catch (IOException e) {
	 * e.printStackTrace();
	 * }
	 * }
	 */

	public boolean channelExists(String channelID) {
		for (Ams7fsChannelInfo info : channelInfo) {
			if (info.getSzID().equals(channelID))
				return true;
		}
		return false;
	}

	public void ClearAllData() {
		MainMenuBar.getInstance().setFileOpen(false);
		events.clear();
		statics.clear();
		starts.clear();
		summaries.clear();
		settings.clear();
		others.clear();
		// offsets.clear();
		channelInfo.clear();
		beatSets = new ArrayList<BeatSet>();
		ecgArtefacts.clear();
		sclArtefacts.clear();
		sclset = new SkinConductanceSet();
		eventsclset = new EventRelatedSCLSet();
		stepSet = new StepSet();
		labels.clear();
		postureLabels.clear();
		speechLabels.clear();
		stairsLabels.clear();
		activityLabels.clear();
		labelConfig = new AmsLabelConfiguration();
		metaLabels.clear();
		AverageMotilityFilesGenerator.clear();
		TotalMotilityGenerator.clear();
		// OrientationFilesGenerator.clear();
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
	}

	public void clearSouthPanel() {
		MainFrame.getInstance().getSouthPanel().removeAll();
	}

	private boolean FillInfo() throws IOException {

		String inString;

		File channelsFile;
		File headerFile;
		File eventsFile;
		File staticsFile;
		File startsFile;
		File summariesFile;
		File settingsFile;
		File othersFile;
		// File offsetsFile;

		File curFile;

		channelsFile = new File(filePath, "channels.json");
		headerFile = new File(filePath, "header.json");
		eventsFile = new File(filePath, "events.json");
		staticsFile = new File(filePath, "statics.json");
		startsFile = new File(filePath, "starts.json");
		summariesFile = new File(filePath, "summaries.json");
		settingsFile = new File(filePath, "settings.json");
		othersFile = new File(filePath, "others.json");
		// offsetsFile = new File(filePath, "offsets.json");

		if (channelsFile.exists() && (headerFile.exists()) && (eventsFile.exists()) && (staticsFile.exists()) &&
				(startsFile.exists()) && (summariesFile.exists()) && (settingsFile.exists()) &&
				(othersFile.exists())) { // } && (offsetsFile.exists())) {
			isFileInfoMissing = false;
		} else {
			isFileInfoMissing = true;
			int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"AmsData File Corrupted!" + "\n"
							+ "The amsdata file is corrupted. The information contained within this file is lost."
							+ "\n" + "Do you want to send this error report to VU University Amsterdam?",
					"AmsData File Corrupted!", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				Utils.submitBug();
			}
			System.exit(0);
		}

		Type collectionType;
		Gson gson = new Gson();
		LinkedList<Object> tempList;

		curFile = new File(System.getProperty("user.dir"), "VUDAMSDebug.json");
		if (curFile.exists()) {
			setDebugFileexists(true);
			inString = Utils.readStringFromFile(curFile);
			debugSettings = gson.fromJson(inString, VUDAMSDebugSettings.class);
		}

		inString = Utils.readStringFromFile(channelsFile);

		curFile = new File(filePath, "channels.json");
		inString = Utils.readStringFromFile(curFile);
		collectionType = new TypeToken<LinkedList<Ams7fsChannelInfo>>() {
		}.getType();
		tempList = gson.fromJson(inString, collectionType);

		inString = Utils.readStringFromFile(headerFile);
		if (inString.contains("AMSDataFileVersion")) {
			addFileVersiontoHeader(false);
			System.out.println("Contains file version");
		} else {
			System.out.println("No file version");
			addFileVersiontoHeader(true);
		}

		if (inString.contains("SpecificBloodResistivity")) {
			addBloodResistivity(false);
		} else {
			addBloodResistivity(true);// for old .amsdata files
		}
		fileHeader = gson.fromJson(inString, Ams7fsHeader.class);

		// ----------------------------- To Solve the problem of DT recorded below 1000
		// Hz-----------------------------
		/*
		 * Adding DZDT Channel Info to channel.json file - This code will be executed
		 * only for previously recorded amsdata files
		 * with DZ sampled below 1000 Hz. It will modify channel.json file
		 */

		for (Object o : tempList) {
			channelInfo.add((Ams7fsChannelInfo) o);

			if (channelInfo.get(channelInfo.size() - 1).getSzID().equals("DZDT")) {

				// --- To check for the type of file - .ams or .amsdata
				if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")) {
					// -- .amsdata file
					// ------------ To check whether an ams file is converted to .amsdata format
					long number = fileHeader.getDwSoftwareVersion7();
					if ((number > 7) && (number < 67327)) {
						// ---- Ams Files (Old Device - AMS1)
						fileHeader.setsFileID("AMS1");
					} // ------------------------------------------------------------------------
					else {
						if (channelInfo.get(channelInfo.size() - 1).getDwDivider() != 1) {
							channelInfo.remove(channelInfo.size() - 1);

							Ams7fsChannelInfo dzdtChannel = new Ams7fsChannelInfo();
							Ams7fsChannelInfo dzChannel = null;
							for (Ams7fsChannelInfo s : channelInfo)
								if (s.getSzID().equals("DZ"))
									dzChannel = s;
							if (dzChannel == null) {

							} else {

								dzdtChannel.setSzID("DZDT");
								dzdtChannel.setSzUnit("\u2126/s");
								// System.out.println("Dw divider:_ " + dzChannel.getDwDivider());
								// long dzSampleTimeInUS = fileHeader.getDwSampleTime_us() /
								// dzChannel.getDwDivider();
								long factor = 1000000 / (2 * dzChannel.getDwDivider() * 1000);
								dzdtChannel.setlMinValue((int) (factor * dzChannel.getlMinValue()));
								dzdtChannel.setlMaxValue((int) (factor * dzChannel.getlMaxValue()));
								dzdtChannel.setlMinMaxDivider((dzChannel.getlMinMaxDivider()));
								dzdtChannel.setnBits(dzChannel.getnBits());
								dzdtChannel.setDwDivider(dzChannel.getDwDivider());
								channelInfo.add(dzdtChannel);
								headerFile.delete();

								PrintWriter writer;
								writer = new PrintWriter(
										new BufferedWriter(new FileWriter(new File(filePath, "channels.json"))));
								writer.print(gson.toJson(channelInfo));
								writer.close();
							}
						}
					}
				} else {
					// ---- .ams file
					long number = fileHeader.getDwSoftwareVersion7();
					if ((number > 7) && (number < 67327)) {
						// ---- Ams Files (Old Device - AMS1)
						fileHeader.setsFileID("AMS1");
					}
				}
			}
			String szUnit = channelInfo.get(channelInfo.size() - 1).getSzUnit();
			szUnit = szUnit.replace("Ohm", "\u2126");
			szUnit = szUnit.replace("ohm", "\u2126");
			szUnit = szUnit.replace("deg", "\u00B0");
			szUnit = szUnit.replace("uS", "\u00B5S");
			channelInfo.get(channelInfo.size() - 1).setSzUnit(szUnit);
			if (channelInfo.get(channelInfo.size() - 1).getSzID().equals("Z0")) {
				if (channelInfo.get(channelInfo.size() - 1).getSzUnit().equals("mV")) {
					channelInfo.get(channelInfo.size() - 1).setSzUnit("\u2126");
					channelInfo.get(channelInfo.size() - 1).setRealSlope(1 / 219780.0);
					channelInfo.get(channelInfo.size() - 1).setRealConstant(25000 / 219780.0);
				}
			}
			if (channelInfo.get(channelInfo.size() - 1).getSzID().equals("Visrc")) {
				if (channelInfo.get(channelInfo.size() - 1).getSzUnit().equals("mV")) {
					channelInfo.get(channelInfo.size() - 1).setSzUnit("k\u2126");
					double slope = channelInfo.get(channelInfo.size() - 1).getRealSlope();
					channelInfo.get(channelInfo.size() - 1).setRealSlope(slope / 200.0);
				}
			}
		}
		for (Ams7fsChannelInfo chan : channelInfo) {
			File tempDir2 = new File(filePath, "tmp");
			tempDir2.mkdirs();
			File outFile = new File(filePath, chan.getSzID() + ".bin");
			if (!outFile.exists())
				outFile = new File(filePath, chan.getSzID() + ".dbin");
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(outFile, tempDir2);
			ssf1.start();
		}
		if (!channelExists("AccelVectorMag")) {
			Ams7fsChannelInfo chan = new Ams7fsChannelInfo();
			Ams7fsChannelInfo chanM;
			try {
				chanM = getChannelInfoFromID("MXR");
				chan.setSzID("AccelVectorMag"); //
				chan.setSzUnit("g");
				chan.setnBits(chanM.getnBits());
				chan.setRealConstant(chanM.getRealConstant());
				chan.setRealSlope(chanM.getRealSlope());
				chan.setlMinMaxDivider(chanM.getlMinMaxDivider());
				chan.setlMinValue(chanM.getlMinValue());
				chan.setlMaxValue(chanM.getlMaxValue());
				chan.setDwDivider(1);
				chan.setTickFile("TicksM");
				getChannelInfo().add(chan);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (channelExists("P_sc")) {
			Ams7fsChannelInfo chan = new Ams7fsChannelInfo();
			// if (!channelExists("Activity")) {
			// chan.setSzID("Activity"); //
			// chan.setSzUnit("#");
			// chan.setnBits(16);
			// chan.setRealConstant(0);
			// chan.setRealSlope(1);
			// chan.setDwDivider(1);
			// chan.setTickFile("TicksM");
			// getChannelInfo().add(chan);
			// }
			chan = new Ams7fsChannelInfo();
			if (!channelExists("MotilityIntensity")) {
				chan.setSzID("MotilityIntensity"); //
				chan.setSzUnit("milliG");
				chan.setnBits(16);
				chan.setRealConstant(0);
				chan.setRealSlope(1);
				chan.setDwDivider(60000);
				// chan.setTickFile("TicksM");
				getChannelInfo().add(chan);
			}
			chan = new Ams7fsChannelInfo();
			if (!channelExists("Altitude")) {
				chan.setSzID("Altitude"); //
				chan.setSzUnit("m");
				chan.setnBits(16);
				chan.setRealConstant(0);
				chan.setRealSlope(1);
				chan.setDwDivider(200);
				chan.setTickFile("TicksD");
				getChannelInfo().add(chan);
			}
		}
		// ------------------------------------------------------------------------------------------------------------------------

		collectionType = new TypeToken<LinkedList<Ams5fsPacket>>() {
		}.getType();

		inString = Utils.readStringFromFile(eventsFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			events.add((Ams5fsPacket) o);

		inString = Utils.readStringFromFile(staticsFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			statics.add((Ams5fsPacket) o);

		inString = Utils.readStringFromFile(startsFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			starts.add((Ams5fsPacket) o);

		inString = Utils.readStringFromFile(summariesFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			summaries.add((Ams5fsPacket) o);

		inString = Utils.readStringFromFile(settingsFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			settings.add((Ams5fsPacket) o);

		inString = Utils.readStringFromFile(othersFile);
		tempList = gson.fromJson(inString, collectionType);
		for (Object o : tempList)
			others.add((Ams5fsPacket) o);

		// LinkedList<LinkedList<Object>> tempList2;
		collectionType = new TypeToken<LinkedList<LinkedList<Ams5fsPacket>>>() {
		}.getType();

		// if (offsetsFile.exists()) {
		// inString = Utils.readStringFromFile(offsetsFile);
		// tempList2 = gson.fromJson(inString, collectionType);
		// for (LinkedList<Object> o : tempList2) {
		// offsets.add(new ArrayList<Ams5fsPacket>());
		// for (Object o2 : o) {
		// offsets.get(offsets.size() - 1).add((Ams5fsPacket) o2);
		// }
		// }
		// }

		startDate = starts.get(0).gettStamp().toGregorianCalendar();
		startTimeInUS = 1000 * starts.get(0).getDwClockTick_ms();

		if (summaries.isEmpty()) {
			Ams7fsChannelInfo cInfo = getChannelInfo().get(0);
			String chan = getChannelInfo().get(0).getSzID();
			if (channelExists("ECG"))
				chan = "ECG";
			long st = getFileHeader().getDwSampleTime_us();
			File tickFile = new File(CurrentOpenData.getInstance().getFilePath(), cInfo.getTickFile() + ".bin");
			if (cInfo.getTickFile() != null && tickFile.exists()) {
				long startTick, endTick;
				try (RandomAccessFile is = new RandomAccessFile(tickFile, "r")) {
					startTick = is.readInt();
					long offset = is.length() / 4 - 1;
					is.seek(4 * offset);
					// System.out.println(offset + " " + is.length());
					endTick = is.readInt();
				}
				endTimeInUS = startTimeInUS + (endTick - startTick) * st;
			} else {
				curFile = new File(filePath, chan + ".bin");
				long nSamples;
				if (Utils.getExtension(getDataFile()).equals("7fs") || fileHeader.getDwHardwareVersion() == 7)
					nSamples = curFile.length() / 2;
				else
					nSamples = curFile.length() / 4;
				endTimeInUS = startTimeInUS + nSamples * st;
			}

			// -------------- Include end Date--------------------
			endDate = Utils.getDateObjectFromUS(endTimeInUS);
			/*
			 * if(dataFile.getName().endsWith(".5FS")||dataFile.getName().endsWith(".5fs")){
			 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
			 * ".5FS file has no summary. Please take care to stop recordings before you remove batteries."
			 * , "Warning", JOptionPane.OK_OPTION);
			 * logger.error("file has no summary!!!");
			 * }
			 */
			// ---------------------------------------------------
		} else {
			// -------------- Include end Date--------------------
			endDate = summaries.get(summaries.size() - 1).gettStamp().toGregorianCalendar();
			// ---------------------------------------------------
			endTimeInUS = 1000 * summaries.get(summaries.size() - 1).getDwClockTick_ms();
		}

		// -------------------- Watch Dog Reset Check----------------------------------

		/*
		 * #define AMSII_START_UNKNOWN 0 Unknown default for AMS_DEVICE_SOFTWAREVERSION
		 * <7
		 * #define AMSII_START_BUTTON 1 Button pushed
		 * #define AMSII_START_SERIAL 2 Started with Windows Software
		 * #define AMSII_START_COVER 3 Auto start after cover close
		 * (AMSII_FLAG_AUTOSTARTCOVER must be set)
		 * #define AMSII_START_STOP 4 Auto start after stop REPLACED BY 7,8 AND 9
		 * !!!!!(AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_RESET 5 Auto start after reset when AMS was recording
		 * (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_WATCHDOG 6 Auto start after WDreset when AMS was
		 * recording (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_STOP_BUT 7 Auto start after stop by button (instead of 4)
		 * (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_STOP_COV 8 Auto start after stop by cover (instead of 4)
		 * (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_STOP_BAT 9 Auto start after stop by battery (instead of
		 * 4) (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)
		 * #define AMSII_START_RESET_STOP_BUT 10 Auto start after reset and stop by
		 * button (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)#Can happen after
		 * (watchdog) reset when AMS measurement is started then stopped by (for
		 * example) cover open then
		 * #define AMSII_START_RESET_STOP_COV 11 Auto start after reset and stop by
		 * cover (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)# restarted again because of
		 * autostart in the same minute. The file with Startreason (watchdog) reset is
		 * overwritten, but
		 * #define AMSII_START_RESET_STOP_BAT 12 Auto start after reset and stop by
		 * battery (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)# the (watchdog) reset
		 * info is transferred in MainState.cpp to the second file witch was started
		 * because of autostart.
		 * #define AMSII_START_WATCHDOG_STOP_BUT 13 Auto start after watchdog and stop
		 * by button (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)# So if in one minute,
		 * meaning one .5fs filename first AMSII_START_RESET happens (by taking out
		 * batteries in dark
		 * #define AMSII_START_WATCHDOG_STOP_COV 14 Auto start after watchdog and stop
		 * by cover (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)# place) and then
		 * AMSII_START_STOP(_COV) happens (by putting back batteries in light place)
		 * this will result in one
		 * #define AMSII_START_WATCHDOG_STOP_BAT 15 Auto start after watchdog and stop
		 * by battery (AMSII_FLAG_AUTOSTARTSTOPRESET must be set)# 5fs file with
		 * Startreason: AMSII_START_RESET_STOP_COV
		 * 
		 */
		if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("5fs")) {
			if (starts.get(starts.size() - 1).getcFileStartReason() == 6) {
				int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						"WatchDog reset occurred!" + "\n"
								+ "A technical report was generated containing no subject data" + "\n"
								+ "Do you want to send this error report to VU University Amsterdam?",
						"WatchDog reset occurred!", JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION) {
					Utils.submitTechnicalBug(AMSII_ERROR_WATCHDOG);
				}
			}
		}

		// ---------------------------------------------------------------------------

		// -------------------- Buffer Overflow Check----------------------------------
		for (Ams5fsPacket ev : CurrentOpenData.getInstance().getEvents()) {
			if (ev.getlType() == 5) {
				if (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("5fs")) {
					int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
							"A ringbuffer overflow occurred!" + "\n"
									+ "A technical report was generated containing no subject data" + "\n"
									+ "Do you want to send this error report to VU University Amsterdam?",
							"Ring Buffer Overflow Warning", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION) {
						Utils.submitTechnicalBug(AMSII_ERROR_RINGBUFFER_OVERFLOW);
					}
				}
			}
		}
		// ---------------------------------------------------------------------------
		if (fileVersion >= 0 && fileVersion < 3) { // SCL value fix needed
			SCLValueConvertor.convertFile(new File(filePath, "SCL.bin"));
		}

		if (new File(System.getProperty("user.dir"), "DeveloperMode.txt").exists()) {
			// 'Blacklist' message for device 164 from 2016-1-15
			Calendar cal164 = Calendar.getInstance();
			cal164.set(Calendar.YEAR, 2016);
			cal164.set(Calendar.MONTH, Calendar.JANUARY);
			cal164.set(Calendar.DAY_OF_MONTH, 15);
			if (startDate.after(cal164) && fileHeader.getDwSerialNumber() == "164")
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
						"### Blacklist ###\nDevice 164 lost since 2016-1-15", "DeveloperMode.txt only Warning",
						JOptionPane.OK_OPTION);

			// 'Blacklist' message for device 167 from 2016-3-17
			Calendar cal167 = Calendar.getInstance();
			cal167.set(Calendar.YEAR, 2016);
			cal167.set(Calendar.MONTH, Calendar.MARCH);
			cal167.set(Calendar.DAY_OF_MONTH, 17);
			if (startDate.after(cal167) && fileHeader.getDwSerialNumber() == "167")
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
						"### Blacklist ###\nDevice 167 lost since 2016-3-17", "DeveloperMode.txt only Warning",
						JOptionPane.OK_OPTION);
		}

		return true;
	}

	public GregorianCalendar getEndDate() {
		return endDate;
	}

	public void setEndDate(GregorianCalendar endDate) {
		this.endDate = endDate;
	}

	private void generateDZDTIfNeeded() {
		if (channelExists("DZDT")) {
			DZDTFileGenerator.GenerateDZDT("DZ");
		}
	}

	private void generateDiffECGIfNeeded() {
		if (channelExists("ECG") && channelExists("V2ecg")) {
			DiffECGGenerator.GenerateECGDiff();
		}
	}

	/*
	 * private void generateDZIfNeeded() {
	 * if (channelExists("DZ")) {
	 * DZFileGenerator.GenerateDZ();
	 * }
	 * }
	 */

	private void generateFilteredDZIfNeeded() {
		String inFile;
		FilteredDZGeneratorFast fDZ = new FilteredDZGeneratorFast();
		if (channelExists("DZ")) {
			if (Utils.getExtension(getDataFile()).equals("7fs") || fileHeader.getDwHardwareVersion() == 7) {
				inFile = "Z0";
				fDZ.GenerateFilteredDZ(inFile, false);
			} else
				inFile = "DZ";
			fDZ.GenerateFilteredDZ(inFile, true);
			// FilteredMotilitySignal.GenerateFilteredMotilitySignal();
			// FilteredECGSignal.GenerateFilteredECG();
		}
	}

	private void generateFilteredECGIfNeeded() {
		FilteredDZGeneratorFast fDZ = new FilteredDZGeneratorFast();
		if (channelExists("ECG")) {
			fDZ.GenerateFilteredDZ("ECG", false);
		}
	}

	private void generateFilteredSCL() {
		if (channelExists("SCL")) {
			FilteredSCLGeneratorFast fSCL = new FilteredSCLGeneratorFast();
			fSCL.GenerateFilteredSCL();
		}
	}

	private void generateFilteredPressure() {
		if (channelExists("P_sc")) {
			FilteredPressureGeneratorFast fP = new FilteredPressureGeneratorFast();
			fP.GenerateFilteredPressure();
		}
	}

	private void generateFilteredDZDT() {
		if (channelExists("DZDT")) {
			FilteredDZDTSignalFast fDZDT = new FilteredDZDTSignalFast();
			fDZDT.GenerateFilteredDZDT();
		}
	}

	private void generateFilteredMot() {
		String[] filteredChans = { "magX", "magY", "magZ", "MXR", "MYR", "MZR", "GyroX", "GyroY", "GyroZ" };
		FilteredMotGeneratorFast fMot = new FilteredMotGeneratorFast();
		for (String chan : filteredChans)
			if (channelExists(chan)) {
				fMot.GenerateFilteredMot(chan);
			}
	}

	public void GenerateSubSampleFiles() {
		SubSetFileGenerator generator = SubSetFileGenerator.getNewInstance();
		// try {
		// OrientationFilesGenerator.getInstance().join();
		// } catch (InterruptedException e1) {
		// e1.printStackTrace();
		// }
		try {
			ActivityClassification.getInstance().join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		generator.start();
	}

	public BeatSet getBeatSet() {
		return beatSet;
	}

	public BeatSet getBeatSet(int i) {
		return beatSets.get(i);
	}

	public int ecgChannels() {
		if (Utils.getExtension(getDataFile()).equals("7fs") || fileHeader.getDwHardwareVersion() == 7)
			return 3;
		else
			return 1;
	}

	public SubSetIbiSet getIbiSubSet(int i) {
		return ibiSubSets.get(i);
	}

	public SubSetIbiSet getIbiSubSet() {
		return ibiSubSet;
	}

	public String getECGFile() {
		return ecgFile;
	}

	public int getECGChannel() {
		return activeBeatset;
	}

	public void setECGChannel(int i) {
		activeBeatset = i;
		beatSet = beatSets.get(i);
		ibiSubSet = ibiSubSets.get(i);
		switch (i) {
			case 0:
				ecgFile = "FILTECG.bin";
				break;
			case 1:
				ecgFile = "FILTV2ecg.bin";
				break;
			case 2:
				ecgFile = "FILTV3ecg.bin";
				break;
		}
	}

	public SkinConductanceSet getSCLSet() {
		return sclset;
	}

	public StepSet getStepSet() {
		return stepSet;
	}

	public ArrayList<Ams7fsChannelInfo> getChannelInfo() {
		return channelInfo;
	}

	public Ams7fsChannelInfo getChannelInfoFromID(String channelID) throws Exception {
		// Could be optimized by using a map (profile to check if its necessary)
		for (Ams7fsChannelInfo info : channelInfo) {
			if (info.getSzID().equals(channelID))
				return info;
		}
		throw new Exception("ChannelID not found: " + channelID);
	}

	public File getDataFile() {
		return dataFile;
	}

	public ArtefactSet getECGArtefacts() {
		return ecgArtefacts;
	}

	public SCLArtefactSet getSCLArtefacts() {
		return sclArtefacts;
	}

	public long getEndTimeInUS() {
		return endTimeInUS;
	}

	public ArrayList<Ams5fsPacket> getEvents() {
		return events;
	}

	public EventRelatedSCLSet getEventSCLSet() {
		return eventsclset;
	}

	public Ams7fsHeader getFileHeader() {
		return fileHeader;
	}

	public VUDAMSDebugSettings getDebugSettings() {
		return debugSettings;
	}

	public File getFilePath() {
		return filePath;
	}

	public AmsLabelConfiguration getLabelConfig() {
		return labelConfig;
	}

	public AmsLabelSet getLabels() {
		return labels;
	}

	public LabelSet getPostureLabels() {
		return postureLabels;
	}

	public LabelSet getSpeechLabels() {
		return speechLabels;
	}

	public LabelSet getStairsLabels() {
		return stairsLabels;
	}

	// public LabelSet getActivityLabels() {
	// return activityLabels;
	// }

	public MetaLabelSet getMetaLabels() {
		return metaLabels;
	}

	// public ArrayList<ArrayList<Ams5fsPacket>> getOffsets() {
	// return offsets;
	// }

	public ArrayList<Ams5fsPacket> getOthers() {
		return others;
	}

	public RespirationSet getRespSet() {
		return respSet;
	}

	public ArrayList<Ams5fsPacket> getSettings() {
		return settings;
	}

	public GregorianCalendar getStartDate() {
		return startDate;
	}

	public ArrayList<Ams5fsPacket> getStarts() {
		return starts;
	}

	public long getStartTimeInUS() {
		return startTimeInUS;
	}

	public ArrayList<Ams5fsPacket> getStatics() {
		return statics;
	}

	public ArrayList<Ams5fsPacket> getSummaries() {
		return summaries;
	}

	public Thread getLoadLabelThread() {
		return loadLabels;
	}

	public Thread getTimeLabelThread() {
		return createTimeLabels;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void addFileVersiontoHeader(boolean state) {
		this.addFileVersiontoHeader = state;
	}

	public boolean isFileVersiontobeAddded() {
		return addFileVersiontoHeader;
	}

	public void addBloodResistivity(boolean state) {
		this.addbloodresistivity = state;
	}

	public boolean isBloodResistivitytobeAdded() {
		return addbloodresistivity;
	}

	private void loadChangeablesFromDisk(int nBitsECG) throws IOException {
		String OSname = System.getProperty("os.name");
		String path = "";
		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application " + "Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		String inString;
		File curFile;
		Type collectionType;
		Gson gson = new Gson();
		LinkedList<Object> tempList;
		JFrame frame = MainFrame.getInstance().getMainFrame();
		ProgressMonitor mon = new ProgressMonitor(frame, "Loading data...", null, 0, 9);

		curFile = new File(filePath, "artefacts.json");
		if (curFile.exists()) {
			mon.setNote("Loading ECG artefacts");
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				ecgArtefacts.add((AmsLabel) o, false);
			}
		} else {
			mon.setNote("Calculating ECG artefacts");
			if (channelExists("ECG") && nBitsECG > 0)
				ecgArtefacts.reCalculate(nBitsECG);
		}
		mon.setProgress(1);

		curFile = new File(filePath, "sclartefacts.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				sclArtefacts.add((AmsLabel) o);
			}
		} else

		{
			if (channelExists("SCL"))
				sclArtefacts.reCalculate();
		}
		curFile = new File(filePath, "SCLSet.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<SCLCycle>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				sclset.addCycle((SCLCycle) o);
			}
		} else {
			if (channelExists("SCL"))
				sclset.reCalculate();
		}

		curFile = new File(filePath, "EventSCLSet.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<EventRelatedSCLCycle>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				eventsclset.addCycle((EventRelatedSCLCycle) o);
			}
		} else {
			if (channelExists("SCL"))
				eventsclset.recalculate();
		}
		mon.setProgress(2);

		mon.setNote("Load posture labels");
		curFile = new File(filePath, "postureLabels.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			if (tempList != null) {
				for (Object o : tempList) {
					postureLabels.add((AmsLabel) o, false);
				}
			}
		}
		mon.setProgress(3);

		// mon.setNote("Load posture labels");
		// curFile = new File(filePath, "speechlabels.json");
		// if (curFile.exists()) {
		// collectionType = new TypeToken<LinkedList<AmsLabel>>() {
		// }.getType();
		// inString = Utils.readStringFromFile(curFile);
		// tempList = gson.fromJson(inString, collectionType);
		// if (tempList != null) {
		// for (Object o : tempList) {
		// speechlabels.add((AmsLabel) o, false);
		// }
		// }
		// }
		// mon.setProgress(3);

		mon.setNote("Load acitivy labels");
		curFile = new File(filePath, "activitylabels.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			if (tempList != null) {
				for (Object o : tempList) {
					activityLabels.add((AmsLabel) o, false);
				}
			}
		}
		mon.setProgress(3);

		BeatSet bsTemp = new BeatSet();
		mon.setNote("Loading beats ECG1");
		curFile = new File(filePath, "beats.json");
		boolean heightsExist = false;
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<ECGBeat>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				bsTemp.addBeat((ECGBeat) o, false);
				if (((ECGBeat) o).getBaseHeight() != 0 && ((ECGBeat) o).getBaseHeightF() != 0)
					heightsExist = true;
			}
			if (tempList.isEmpty())
				bsTemp.reCalculate("FILTECG.bin");
		} else {
			if (channelExists("ECG")) {
				bsTemp.reCalculate("FILTECG.bin");
			}
		}
		bsTemp.resetFirstInSeries();
		beatSet = bsTemp;
		beatSets.add(bsTemp);
		ibiSubSets.add(new SubSetIbiSet(0));
		if (!heightsExist) {
			final Thread heightThread = new Thread() {
				@Override
				public void run() {
					bsTemp.recalculateHeights("ECG");
					bsTemp.recalculateAllSuspiciousLevels();
					ecgArtefacts.reCalculateFromPeaks();
				}
			};
			heightThread.start();
			setDirty(true);
		} else {
			bsTemp.recheckSuspiciousIBIS(); // fill beats sorted by suspicion array
		}
		// mon.setNote("Loading beats ECG2");
		if (channelExists("V2ecg")) {
			bsTemp2 = new BeatSet();
			bsTemp3 = new BeatSet();
			beatSets.add(bsTemp2);
			beatSets.add(bsTemp3);
			ibiSubSets.add(new SubSetIbiSet(1));
			ibiSubSets.add(new SubSetIbiSet(2));

			Thread extraECGChans = new Thread() {
				private File curFile;
				private Type collectionType = new TypeToken<LinkedList<ECGBeat>>() {
				}.getType();
				private String inString;
				private LinkedList<Object> tempList;

				@Override
				public void run() {
					FilteredDZGeneratorFast fDZ = new FilteredDZGeneratorFast();
					if (channelExists("V2ecg")) {
						fDZ.GenerateFilteredDZ("V2ecg", false);
					}
					if (channelExists("V3ecg")) {
						fDZ.GenerateFilteredDZ("V3ecg", false);
					}
					boolean heightsExist = false;
					curFile = new File(filePath, "beats2.json");
					if (curFile.exists()) {
						inString = Utils.readStringFromFile(curFile);
						tempList = gson.fromJson(inString, collectionType);
						for (Object o : tempList) {
							bsTemp2.addBeat((ECGBeat) o, false);
							if (((ECGBeat) o).getBaseHeight() != 0 && ((ECGBeat) o).getBaseHeightF() != 0)
								heightsExist = true;
						}
						if (tempList.isEmpty())
							bsTemp2.reCalculate("FILTV2ecg.bin");
					} else {
						bsTemp2.reCalculate("FILTV2ecg.bin");
					}
					bsTemp2.resetFirstInSeries();
					if (!heightsExist) {
						bsTemp2.recalculateHeights("V2ecg");
						setDirty(true);
					}
					bsTemp2.recalculateAllSuspiciousLevels();
					mon.setNote("Loading beats ECG3");
					if (channelExists("V3ecg")) {
						// bsTemp3 = new BeatSet();
						heightsExist = false;
						curFile = new File(filePath, "beats3.json");
						if (curFile.exists()) {
							inString = Utils.readStringFromFile(curFile);
							tempList = gson.fromJson(inString, collectionType);
							for (Object o : tempList) {
								bsTemp3.addBeat((ECGBeat) o, false);
								if (((ECGBeat) o).getBaseHeight() != 0 && ((ECGBeat) o).getBaseHeightF() != 0)
									heightsExist = true;
							}
							if (tempList.isEmpty())
								bsTemp3.reCalculate("FILTV3ecg.bin");
						} else {
							bsTemp3.reCalculate("FILTV3ecg.bin");
						}
						bsTemp3.resetFirstInSeries();
						if (!heightsExist) {
							bsTemp3.recalculateHeights("V3ecg");
							setDirty(true);
						}
						bsTemp3.recalculateAllSuspiciousLevels();
					}
				}
			};
			extraECGChans.start();
		}
		mon.setProgress(4);

		mon.setNote("Loading respiration cycles");
		generateFilteredDZIfNeeded();
		generateDZDTIfNeeded();
		generateFilteredDZDT();
		curFile = new File(filePath, "respcycles.json");
		if (curFile.exists()) {
			respSet.loadFromJSON(curFile);
			// fix reuse high/lowvalue variables gives wrong tidal volume raw with existing
			// files
			final Thread tidalFixThread = new Thread() {

				@Override
				public void run() {
					BinaryFile bf;
					if (Utils.getExtension(getDataFile()).equals("7fs") || fileHeader.getDwHardwareVersion() == 7)
						bf = new BinaryFile("Z0");
					else
						bf = new BinaryFile("DZ");

					BinaryFile bff = new BinaryFile("FILTDZ");
					for (RespirationCycle rc : respSet.getCycles()) {
						rc.setLowValue(bf.getDataAtTime(rc.getInspStart()));
						rc.setHighValue(bf.getDataAtTime(rc.getExpStart()));
						rc.setLowRealValue(bff.getDataAtTime(rc.getInspStart()));
						rc.setHighRealValue(bff.getDataAtTime(rc.getExpStart()));
					}
					try {
						bf.close();
						bff.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			tidalFixThread.start();
		} else {
			if (channelExists("DZ"))
				respSet.recalculate();
		}
		mon.setProgress(5);

		mon.setNote("Loading labels");
		ImpTab.getNewInstance(); // labels.add uses ImpTab through creating Signalparts
		curFile = new File(filePath, "labels.json");
		if (curFile.exists())

		{
			collectionType = new TypeToken<LinkedList<AmsLabel>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			final LinkedList<Object> tempListL = gson.fromJson(inString, collectionType);
			if (tempListL != null) {
				labelsize = tempListL.size();
				// load labels in separate thread, because creating signalparts is slow
				if (loadLabels != null) {
					// loadLabels.interrupt();
					try {
						loadLabels.join();
					} catch (InterruptedException e) {
					}
					ThreadServer.removeThread(loadLabels);
				}
				loadLabels = new Thread() {
					@Override
					public void run() {
						for (Object o : tempListL) {
							labels.add((AmsLabel) o, false, false);
							((AmsLabel) o).renameICGpoints();
						}
						MainFrame.getInstance().getMainFrame().repaint();
					}
				};
				ThreadServer.addNewThread(loadLabels);
				loadLabels.start();
			}
			labels.setlabelindex(1);
			if (labels.getLabels().size() == 0 && labels.getAllLabels().size() > 0)
				labels.setlabelindex(2); // time labels
		}
		mon.setProgress(6);

		mon.setNote("Calculate results");
		curFile = new File(filePath, "labelconfig.json");
		if (curFile.exists() == false)

		{
			curFile = new File(path, "VU-DAMS");
			if (curFile.exists() == false) {
				curFile.mkdir();
			}
			curFile = new File(curFile, "labelconfig.json");
			if (curFile.exists() == false) {
				curFile = new File(System.getProperty("user.dir"), "labelconfig.json");
			}
		}
		// --------------------------------------------------------------------------------------------
		if (curFile.exists()) {
			collectionType = new TypeToken<Map<String, LinkedList<LabelValue>>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			Map<String, LinkedList<LabelValue>> map = gson.fromJson(inString, collectionType);
			for (String s : map.keySet()) {
				ArrayList<LabelValue> lst = new ArrayList<LabelValue>(map.get(s));
				labelConfig.getConfig().put(s, lst);
			}
		}
		LabelInformationTab.getNewInstance().getData().updateLabelConfig();
		mon.setProgress(7);

		mon.setNote("Loading combined labels");
		curFile = new File(filePath, "combinedlabels.json");
		String Contents = null;
		Boolean bEmpty = true;
		if (curFile.exists() == true) {// does "combinedlabels.json" from .amsdata(i) file exist?
			Contents = Utils.readStringFromFile(curFile);
			bEmpty = (Contents.compareTo("[]") == 0);
		}

		if (curFile.exists() == false | bEmpty == true) {// If it doesn't exist and/or if it is empty:
			curFile = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
			curFile = new File(curFile, "VU-DAMS/combinedlabels.json");
			if (curFile.exists() == false) {
				curFile = new File(System.getProperty("user.dir"), "combinedlabels.json");
			}
		}

		if (curFile.exists()) {
			metaLabels.readFromJSON(curFile);
		}

		// ----------------- To correct for respiration rates---------------------------
		if (isDebugFileexists()) {
			if (getDebugSettings().getRecalculateFiltDZ() == 1
					&& Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("amsdata")) {
				if (Utils.compareVersions(fileHeader.getAmsDataFileVersion(), "3.9") > 0) {
					File fdzFile = new File(CurrentOpenData.getInstance().getFilePath(), "FILTDZ.bin");
					if (fdzFile.exists()) {
						fdzFile.delete();
					}
					generateFilteredDZIfNeeded();
					getRespSet().recalculate();
					// RSATab.getInstance().repaint();
					// saveChangeablesToDisk();
				}
			}
		}
		// ----------------------------------------------------------------------------
		mon.setProgress(8);

		curFile = new File(filePath, "StepSet.json");
		if (curFile.exists()) {
			collectionType = new TypeToken<LinkedList<Step>>() {
			}.getType();
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				stepSet.addStep((Step) o, false);
			}
			if (tempList.isEmpty())
				stepSet.reCalculate("FILTAccelVectorMag.bin");
		} else {
			if (channelExists("AccelVectorMag")) {
				stepSet.reCalculate("FILTAccelVectorMag.bin");
			}
		}
		mon.setProgress(9);

		curFile = new File(filePath, "OnlyExp.json");
		if (curFile.exists()) {
		}
		curFile = new File(filePath, "OnlyTime.json");
		if (curFile.exists()) {
		}
		curFile = new File(filePath, "Mergedlabels.json");
		if (curFile.exists()) {
		}

		curFile = new File(filePath, "extfile.dat");
		if (curFile.exists()) {
			ExternalFilePanel.getInstance().loadFileFromTempDir();
		}

		curFile = new File(filePath, "extfile2.dat");
		if (curFile.exists()) {
			ExternalFilePanel.getInstance().loadFile2FromTempDir();
		}

		curFile = new File(filePath, "extfile3.dat");
		if (curFile.exists()) {
			ExternalFilePanel.getInstance().loadFile3FromTempDir();
		}

		curFile = new File(filePath, "Actigraph_Motility.dat");
		if (curFile.exists()) {
			ActigraphTab.getInstance().loadFileFromTempDir();
		}

		if (mon != null)
			mon.close();
	}

	public int getLabelsize() {
		return labelsize;
	}

	public void setLabelsize(int labelsize) {
		this.labelsize = labelsize;
	}

	public void setBatchAnalysis(boolean type) {
		this.batchexport = type;
	}

	public void setTypeofAnalysis(int type) {
		this.analysistype = type;
	}

	public boolean isBatchAnalysis() {
		return batchexport;
	}

	public int getAnalysisType() {
		return analysistype;
	}

	public double getBloodresistivity() {
		return bloodresistivity;
	}

	public void setBloodresistivity(double bloodresistivity) {
		this.bloodresistivity = bloodresistivity;
	}

	public boolean isDebugFileexists() {
		return debugFileexists;
	}

	public void setDebugFileexists(boolean debugFileexists) {
		this.debugFileexists = debugFileexists;
	}

	public void Open(File filePath, boolean quick, boolean isBatch, int typeofanalysis) throws IOException {
		Open(filePath, quick, isBatch, typeofanalysis, -1);
	}

	public void Open(File filePath, boolean quick, boolean isBatch, int typeofanalysis, int nBitsECG)
			throws IOException {
		MainFrame.getInstance().getTabs().clear();
		Timer timerB = new Timer();
		timerB.start();
		setFilePath(filePath);
		setOpen(true);
		FillInfo();

		updateMainFrameTitle();// To update Subject ID asap in title bar (if enabled)
		beatSet = new BeatSet();

		// if (quick == false) {
		generateDiffECGIfNeeded();
		generateFilteredECGIfNeeded();
		generateFilteredSCL();
		generateFilteredPressure();
		generateFilteredMot();
		// }
		setBatchAnalysis(isBatch);
		setTypeofAnalysis(typeofanalysis);
		AverageMotilityFilesGenerator.generateAvMotFiles(); // Generate Average motility files (.dbin files)
		// OrientationFilesGenerator generator =
		// OrientationFilesGenerator.getNewInstance();
		// generator.start();
		ActivityClassification classification = ActivityClassification.getNewInstance();
		classification.start();
		OrientationFilesGenerator generator = OrientationFilesGenerator.getNewInstance();
		generator.start();
		// ActivityClassification classification =
		// ActivityClassification.getNewInstance();
		// classification.start();
		SetUpExternalFilePanel();
		loadChangeablesFromDisk(nBitsECG);
		logger.debug(starts.get(0).gettStamp());

		// Detect postures if NOT loaded from disk
		if (postureLabels.getLabels().isEmpty()) {
			PostureClassifier postureClassifier = PostureClassifier.getInstance();
			if (postureClassifier.classify(getFilePath()) > 0)
				postureClassifier.addPostureLabelsToLabelSet(postureLabels);
		}

		SetUpTimeBar();
		SetUpInspectPanel();
		// GenerateSubSampleFiles(); // after generating overview tab (for autoscale)
		SetUpLabelPanel();
		SetUpQRSPanel();
		// SetUpFrequencyPanel();
		SetUpRSAPanel();

		// Init BloodResistivity before SetUpImpPanel!
		if (isBloodResistivitytobeAdded() == true) {
			fileHeader.setSpecificBloodResistivity(getBloodresistivity());
			// saveChangeablesToDisk();
		} else {
			if (fileHeader.getSpecificBloodResistivity() == 0.0) {// for old .amsdata files and .5FS files
				fileHeader.setSpecificBloodResistivity(getBloodresistivity());// Add default BloodResistivity
			} else {
				setBloodresistivity(fileHeader.getSpecificBloodResistivity());// else use Bloodresistivity from .amsdata
																				// file
			}
			// saveChangeablesToDisk();
		}

		SetUpImpPanel();
		SetUpSCLPanel();

		File curFile = new File(filePath, "Actigraph_Motility.dat");
		if (curFile.exists()) {
			SetUpActigraphPanel();
		}
		SetUpInformationPanel();
		// SubSetIbiSet.getInstance().reCalculateSubSet(true);
		MainMenuBar.getInstance().setFileOpen(true);

		updateMainFrameTitle();
		// MainFrame.getInstance().getMainFrame().revalidate();
		// MainFrame.getInstance().getMainFrame().repaint();
		timerB.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Loading UI took (" + timerB.getTime() / 1000. + " sec)");

		if (isFileVersiontobeAddded() == true) {
			// If the AmsData Header File does not contain variable called
			// AMSDataFileVersion
			// NO warning message is displayed
			// Variable is created when the file is saved

			// Versions given to Rene
			if (fileHeader.getDwFileVersion() == 3) {
				fileHeader.setAmsDataFileVersion(Utils.getVersionString()); // Set Current Software Version as File
																			// Version
				// saveChangeablesToDisk();
			} else {
				fileHeader.setAmsDataFileVersion(Utils.getVersionString()); // Set Current Software Version as File
																			// Version
				// saveChangeablesToDisk();
			}
		} else {
			// AmsData Header File contains variable called AMSDataFileVersion
			// Check whether the AMSDataFileVersion == VU-DAMS Version (displayed at the top
			// corner of the software)

			if (fileHeader.getVUDAMSSoftwareVersion() == "0") { // Add VU-DAMS Software version
				fileHeader.setVUDAMSSoftwareVersion(Utils.getVersionString());
			}
			if (fileHeader.getAmsDataFileVersion() == Utils.getVersionString()) {
				// Both are equal
				// Everything is good

			} else if (Utils.compareVersions(fileHeader.getAmsDataFileVersion(), Utils.getVersionString()) < 0) {
				// Not equal
				// Chances of losing data when the file is saved
				// JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
				// "Warning!!! - You might lose data! Upgrade the software to newer Version to
				// prevent any changes to file format", "Mismatch between software version and
				// file version", JOptionPane.OK_OPTION);
				int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						"You are now using VU-DAMS Version" + " " + Utils.getVersionString() + " "
								+ "and might lose analyzing done with higher VU-DAMS vesions if you continue. " + "\n"
								+ " Do you want to close VU-DAMS now to upgrade?",
						"This File has been saved with a newer VU-DAMS version" + " " + "("
								+ fileHeader.getAmsDataFileVersion() + ")",
						JOptionPane.YES_NO_OPTION);

				if (res == JOptionPane.YES_OPTION) {
					ThreadServer.stopAllThreads();
					// SubSetFileGenerator.getInstance().interrupt();
					try {
						SubSetFileGenerator.getInstance().join(1);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					// OrientationFilesGenerator.getInstance().interrupt();
					try {
						OrientationFilesGenerator.getInstance().join(1);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					// ActivityClassification.getInstance().interrupt();
					try {
						ActivityClassification.getInstance().join(1);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

					// AppPinger.getInstance().interrupt();
					try {
						AppPinger.getInstance().join(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					CurrentOpenData.getInstance().ClearAllData();
					AppSettings.getInstance().saveSettingsToBeSaved();

					System.exit(0);
				} else {

				}
				// JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
				// "Mismatch between software version and file version. Necessary Conversions
				// are performed");

			} // else if (fileHeader.getAmsDataFileVersion() <
				// Double.valueOf(Utils.getVersionString())){
				// File Version is less than software version; moved to save
				// fileHeader.setAmsDataFileVersion(Double.valueOf(Utils.getVersionString()));
				// // Set Current Software Version as File Version
				// saveChangeablesToDisk();
				// }
		}
	}

	public void Close() {
		if (isOpen == true) {
			if (CurrentOpenData.getInstance().isDirty()
					|| CurrentOpenData.getInstance().dirtyFiles.size() > 0) {
				int result;
				if (new File(System.getProperty("user.dir"), "DoNotAskMeToSaveData.txt").exists() || batchexport)
					result = JOptionPane.NO_OPTION;
				else
					result = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
							"Would you like to save the current open data? It will be compressed so it opens faster next time.",
							"Save data",
							JOptionPane.YES_NO_CANCEL_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					MainMenuBar.getInstance().saveData();
				} else if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
					return;
				}
			}
			try {
				if (ImpTab.getInstance().recalcThread != null)
					ImpTab.getInstance().recalcThread.join(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			TimeBar.getInstance().close();
			// SubSetFileGenerator.getInstance().interrupt();
			try {
				SubSetFileGenerator.getInstance().join(1);
			} catch (InterruptedException e) {
			}
			// OrientationFilesGenerator.getInstance().interrupt();
			try {
				OrientationFilesGenerator.getInstance().join(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			// ActivityClassification.getInstance().interrupt();
			try {
				ActivityClassification.getInstance().join(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				if (LabelInformationTab.getInstanceIfExist() != null
						&& LabelInformationTab.getInstance().getData().getRecalcThread() != null) {
					try {
						LabelInformationTab.getInstance().getData().getRecalcThread().join(1);
					} catch (InterruptedException e) {
					}
				}
				ThreadServer.stopAllThreads();
				ExitApp.deleteDirectory(this.filePath);
				ClearAllData();
			}
			setOpen(false);
			clearSouthPanel();
			MainFrame.getInstance().getTabs().clear();
			MainFrame.getInstance().getTabs().addAmsTab("", StartTab.getNewInstance());
			String newtitle = "";
			if (Utils.isIsprerelease()) {
				MainMenuBar.getInstance().getMenuBar().setBackground(Color.RED);
				newtitle = MainFrame.getInstance().getMainFrame().getTitle() + "_" + "beta_version";
				MainFrame.getInstance().getMainFrame().setTitle(newtitle);
			} else {
				newtitle = MainFrame.getInstance().getMainFrame().getTitle();
				MainFrame.getInstance().getMainFrame().setTitle(newtitle);
			}
			dataFile = null;
			dirtyFiles.clear();
			isDirty = false;
		}
	}

	public void saveChangeablesToDisk() {
		PrintWriter writer = null;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		fileHeader.setSpecificBloodResistivity(getBloodresistivity());
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "header.json"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.print(gson.toJson(fileHeader));
		writer.close();
		ecgArtefacts.saveToJSON(new File(filePath, "artefacts.json"));
		sclArtefacts.saveToJSON(new File(filePath, "sclartefacts.json"));
		sclset.saveToJSON(new File(filePath, "SCLSet.json"));
		eventsclset.saveToJSON(new File(filePath, "EventSCLSet.json"));
		stepSet.saveToJSON(new File(filePath, "StepSet.json"));
		int index = labels.getlabelindex();
		labels.setlabelindex(0); // save all
		labels.saveToJSON(new File(filePath, "labels.json"));
		postureLabels.saveToJSON(new File(filePath, "postureLabels.json"));
		speechLabels.saveToJSON(new File(filePath, "speechlabels.json"));
		stairsLabels.saveToJSON(new File(filePath, "stairslabels.json"));
		activityLabels.saveToJSON(new File(filePath, "activitylabels.json"));
		beatSets.get(0).saveToJSON(new File(filePath, "beats.json"));
		if (beatSets.size() > 1) {
			beatSets.get(1).saveToJSON(new File(filePath, "beats2.json"));
			beatSets.get(2).saveToJSON(new File(filePath, "beats3.json"));
		}
		labelConfig.saveToJSON(new File(filePath, "labelconfig.json"));
		metaLabels.writeToJSON(new File(filePath, "combinedlabels.json"));
		labels.saveonlyExplabelsToJSON(new File(filePath, "OnlyExp.json"));
		// labels.savecustomizedlabelstoJSON(new File(filePath, "Mergedlabels.json"));
		labels.savetimelabelstoJSON(new File(filePath, "OnlyTime.json"));
		labels.setlabelindex(index);
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "events.json"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.print(gson.toJson(events));
		writer.close();
		LabelInformationTab.getInstance().getData().outputDataToASCII(new File(filePath, "outputdata.txt"), -1, false);
		respSet.saveToJSON(new File(filePath, "respcycles.json"));
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "channels.json"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.print(gson.toJson(channelInfo));
		writer.close();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "icgParts.json"))));
			writer.print(gson.toJson(ImpTab.getInstance().getImpDrawer().getSignalPartSet()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "ecgParts.json"))));
			writer.print(gson.toJson(ImpTab.getInstance().getECGDrawer().getSignalPartSet()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (ImpTab.getInstance().getDrawers() != null)
			for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers()) {
				try {
					writer = new PrintWriter(
							new BufferedWriter(new FileWriter(new File(filePath, prt.getName() + "Parts.json"))));
					writer.print(gson.toJson(prt.getSignalPartSet()));
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "icgRawParts.json"))));
			writer.print(gson.toJson(ImpTab.getInstance().getImpRawDrawer().getSignalPartSet()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath, "ecgRawParts.json"))));
			writer.print(gson.toJson(ImpTab.getInstance().getECGRawDrawer().getSignalPartSet()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (ImpTab.getInstance().getFiltDrawers() != null)
			for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers()) {
				try {
					writer = new PrintWriter(
							new BufferedWriter(new FileWriter(new File(filePath, prt.getName() + "FiltParts.json"))));
					writer.print(gson.toJson(prt.getSignalPartSet()));
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}

	public void setDataFile(File dataFile, short fileVersion) {
		this.dataFile = dataFile;
		this.fileVersion = fileVersion;
		updateMainFrameTitle();
	}

	public void setFilePath(File filePath) {
		this.filePath = filePath;
	}

	public void setLabelConfig(AmsLabelConfiguration newLblCfg) {
		labelConfig = newLblCfg;
		labelConfig.saveToJSON(new File(filePath, "labelconfig.json"));
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public void SetUpExternalFilePanel() {
		MainFrame.getInstance().getSouthPanel().add(ExternalFilePanel.getNewInstance(), BorderLayout.SOUTH);
	}

	/*
	 * private void SetUpFrequencyPanel() {
	 * if (channelExists("ECG")) {
	 * MainFrame.getInstance().getTabs().addAmsTab("Frequency",
	 * FrequencyTab.getNewInstance());
	 * } else {
	 * MainFrame.getInstance().getTabs().
	 * addAmsTab("<html><font color=gray>Frequency</font></html>", new AmsTab());
	 * int newTabIndex = MainFrame.getInstance().getTabs().getTabCount() - 1;
	 * MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
	 * }
	 * 
	 * }
	 */

	public void SetUpImpPanel() {
		if (channelExists("DZDT") == true && channelExists("ECG") == true) {
			MainFrame.getInstance().getTabs().addAmsTab("ICG", ImpTab.getInstance());
			if (labels != null && labels.getAllLabels().size() > 0) {
				String inString;
				File curFile;
				Type collectionType;
				Gson gson = new Gson();
				// boolean allFilesFound = true;
				// boolean recalc = false;

				curFile = new File(filePath, "icgParts.json");
				if (curFile.exists()) {
					collectionType = new TypeToken<SignalPartSet>() {
					}.getType();
					inString = Utils.readStringFromFile(curFile);
					tempSignalPartSet = gson.fromJson(inString, collectionType);
					// if ((AppSettings.getInstance()
					// .getIntProperty(Settings.FILTERDZDTNew) == 1) != tempSignalPartSet.filtered)
					// {
					// Object[] options = { "Adjust setting", "Recalculate" };
					// int res =
					// JOptionPane.showOptionDialog(MainFrame.getInstance().getMainFrame(),
					// "The setting for filtering DZDT in the ICG tab is not equal to the one this
					// file was saved with; if you choose Recalculate the scoring for all the ECG &
					// ICG Complexes will be lost.",
					// "Existing labels", JOptionPane.YES_NO_OPTION,
					// JOptionPane.QUESTION_MESSAGE,
					// null, // do not use a custom Icon
					// options, // the titles of buttons
					// options[0]); // default button title
					// if (res == 0) {
					// AppSettings.getInstance().setIntProperty(Settings.FILTERDZDTNew,
					// tempSignalPartSet.filtered ? 1 : 0);
					// ImpTab.getInstance().getImpDrawer().setSignalPartSet(tempSignalPartSet);
					// } else {
					// recalc = true;
					// }
					// } else {
					ImpTab.getInstance().getImpDrawer().setSignalPartSet(tempSignalPartSet);
					// }
					// } else {
					// allFilesFound = false;
				}
				curFile = new File(filePath, "ecgParts.json");
				if (curFile.exists()) {
					collectionType = new TypeToken<SignalPartSet>() {
					}.getType();
					inString = Utils.readStringFromFile(curFile);
					tempSignalPartSet = gson.fromJson(inString, collectionType);
					// if ((AppSettings.getInstance()
					// .getIntProperty(Settings.FILTERECGNew) == 1) != tempSignalPartSet.filtered) {
					// Object[] options = { "Adjust setting", "Recalculate" };
					// int res =
					// JOptionPane.showOptionDialog(MainFrame.getInstance().getMainFrame(),
					// "The setting for filtering ECG in the ICG tab is not equal to the one this
					// file was saved with; if you choose Recalculate the scoring for all the ECG &
					// ICG Complexes will be lost.",
					// "Existing labels", JOptionPane.YES_NO_OPTION,
					// JOptionPane.QUESTION_MESSAGE,
					// null, // do not use a custom Icon
					// options, // the titles of buttons
					// options[0]); // default button title
					// if (res == 0) {
					// AppSettings.getInstance().setIntProperty(Settings.FILTERECGNew,
					// tempSignalPartSet.filtered ? 1 : 0);
					// ImpTab.getInstance().getECGDrawer().setSignalPartSet(tempSignalPartSet);
					// } else {
					// recalc = true;
					// }
					// } else {
					ImpTab.getInstance().getECGDrawer().setSignalPartSet(tempSignalPartSet);
					// }
					// } else {
					// allFilesFound = false;
				}
				if (ImpTab.getInstance().getDrawers() != null)
					for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers()) {
						curFile = new File(filePath, prt.getName() + "Parts.json");
						if (curFile.exists()) {
							collectionType = new TypeToken<SignalPartSet>() {
							}.getType();
							inString = Utils.readStringFromFile(curFile);
							tempSignalPartSet = gson.fromJson(inString, collectionType);
							prt.setSignalPartSet(tempSignalPartSet);
							// } else {
							// allFilesFound = false;
						}
					}
				curFile = new File(filePath, "icgRawParts.json");
				if (curFile.exists()) {
					collectionType = new TypeToken<SignalPartSet>() {
					}.getType();
					inString = Utils.readStringFromFile(curFile);
					tempSignalPartSet = gson.fromJson(inString, collectionType);
					ImpTab.getInstance().getImpRawDrawer().setSignalPartSet(tempSignalPartSet);
					// }
					// } else {
					// allFilesFound = false;
				}
				curFile = new File(filePath, "ecgRawParts.json");
				if (curFile.exists()) {
					collectionType = new TypeToken<SignalPartSet>() {
					}.getType();
					inString = Utils.readStringFromFile(curFile);
					tempSignalPartSet = gson.fromJson(inString, collectionType);
					ImpTab.getInstance().getECGRawDrawer().setSignalPartSet(tempSignalPartSet);
					// } else {
					// allFilesFound = false;
				}
				if (ImpTab.getInstance().getFiltDrawers() != null)
					for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers()) {
						curFile = new File(filePath, prt.getName() + "FiltParts.json");
						if (curFile.exists()) {
							collectionType = new TypeToken<SignalPartSet>() {
							}.getType();
							inString = Utils.readStringFromFile(curFile);
							tempSignalPartSet = gson.fromJson(inString, collectionType);
							prt.setSignalPartSet(tempSignalPartSet);
							// } else {
							// allFilesFound = false;
						}
					}
				// if (!allFilesFound || recalc)
				ImpTab.getInstance().recalculate();
			}
		} else {
			MainFrame.getInstance().getTabs().addAmsTab("<html><font color=gray>ICG</font></html>", new AmsTab());
			int newTabIndex = MainFrame.getInstance().getTabs().getTabCount()
					- 1;
			MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
		}
	}

	public void SetUpInformationPanel() {
		MainFrame.getInstance().getTabs().addAmsTab("Results", LabelInformationTab.getInstance());
	}

	public void SetUpInspectPanel() {
		MainFrame.getInstance().getTabs().addAmsTab("Overview", InspectTab.getNewInstance());
	}

	private void SetUpLabelPanel() {
		// boolean hasTimeLabels = false;
		// for (AmsLabel l : labels.getAllLabels()) {
		// if (l.isTimeLabel())
		// hasTimeLabels = true;
		// }
		// if (!hasTimeLabels) {
		// if (labels.getLabels().isEmpty()) {
		// labels.setStateoflabels(false);
		// } else {
		// labels.setStateoflabels(true);
		// }
		// if (createTimeLabels != null) {
		// // createTimeLabels.interrupt();
		// try {
		// createTimeLabels.join();
		// } catch (InterruptedException e) {
		// }
		// ThreadServer.removeThread(createTimeLabels);
		// }
		// createTimeLabels = new Thread() {
		// @Override
		// public void run() {
		// AmsLabel lbl = new AmsLabel(getStartTimeInUS(), getEndTimeInUS(), false, 0.0,
		// "");
		// lbl.setisTimeLabel(true);
		// labels.add(lbl, true);
		// labels.setToTimeLabels(60 * 1000000); // create time labels when no labels
		// present
		// MainFrame.getInstance().getMainFrame().repaint();
		// }
		// };
		// ThreadServer.addNewThread(createTimeLabels);
		// createTimeLabels.start();
		// }
		MainFrame.getInstance().getTabs().addAmsTab("Label", LabelTab.getNewInstance());
	}

	public void SetUpQRSPanel() {
		if (channelExists("ECG") == true) {
			MainFrame.getInstance().getTabs().addAmsTab("ECG", QRSTab.getNewInstance());
		} else {
			MainFrame.getInstance().getTabs().addAmsTab("<html><font color=gray>ECG</font></html>", new AmsTab());
			int newTabIndex = MainFrame.getInstance().getTabs().getTabCount() - 1;
			MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
		}
	}

	public void SetUpRSAPanel() {
		if (channelExists("DZ") == true) {
			MainFrame.getInstance().getTabs().addAmsTab("Respiration", RSATab.getNewInstance());
		} else {
			MainFrame.getInstance().getTabs().addAmsTab("<html><font color=gray>Respiration</font></html>",
					new AmsTab());
			int newTabIndex = MainFrame.getInstance().getTabs().getTabCount() - 1;
			MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
		}
	}

	public void SetUpSCLPanel() {
		if (channelExists("SCL") == true) {
			MainFrame.getInstance().getTabs().addAmsTab("EDA", SCLTab.getNewInstance());
		} else {
			MainFrame.getInstance().getTabs().addAmsTab("<html><font color=gray>EDA</font></html>", new AmsTab());
			int newTabIndex = MainFrame.getInstance().getTabs().getTabCount() - 1;
			MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
		}
	}

	public void SetUpActigraphPanel() {
		File curFile = new File(filePath, "Actigraph_Motility.dat");
		if (curFile.exists()) {
			MainFrame.getInstance().getTabs().addAmsTab("Actigraph Data", ActigraphTab.getNewInstance());
		} else {
			MainFrame.getInstance().getTabs().addAmsTab("<html><font color=gray>Actigraph Data</font></html>",
					new AmsTab());
			int newTabIndex = MainFrame.getInstance().getTabs().getTabCount() - 1;
			MainFrame.getInstance().getTabs().setEnabledAt(newTabIndex, false);
		}
	}

	public void SetUpTimeBar() {
		MainFrame.getInstance().getSouthPanel().add(TimeBar.getInstance().getPanel(), BorderLayout.SOUTH);
	}

	private void updateMainFrameTitle() {
		String title = Utils.APPNAME;
		if (Utils.isIsprerelease()) {
			title += "_" + "beta_version";
		}
		if (dataFile != null) {
			title += " - " + dataFile.getName();
		}
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.APPENDSUBIDTOTITLEBAR) != 0)
			if (fileHeader != null) {
				title += " [subject " + fileHeader.getSzSubjectID() + "; study " + fileHeader.getStudyId()
						+ "; session " + fileHeader.getDwSession() + "; " + fileHeader.getDeviceId() + "]";
			}
		MainFrame.getInstance().getMainFrame().setTitle(title);
		MainFrame.getInstance().getMainFrame().repaint();
	}

	public boolean isFileInfoMissing() {
		return isFileInfoMissing;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}
}
