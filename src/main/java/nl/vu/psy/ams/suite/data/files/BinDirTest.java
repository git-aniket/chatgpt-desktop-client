package nl.vu.psy.ams.suite.data.files;

//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

//import javax.swing.JOptionPane;
//import javax.swing.ProgressMonitor;
//import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
//import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
//import nl.vu.psy.ams.suite.gui.MainFrame;
//import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
/*
 * Class that opens a directory containing binary files, converts the contents
 * to the VU-DAMS format and copies the result to a
 * unique temporary directory. For testing ams version 7
 */
public class BinDirTest extends Thread {

	public final static int					TIME_LBL_START			= 0;
	public final static int					TIME_LBL_HOLD			= 1;
	public final static int					TIME_LBL_CONTINUE		= 2;
	public final static int					TIME_LBL_BATT_LOW		= 3;
	public final static int					TIME_LBL_EVENT			= 4;
	public final static int					TIME_LBL_MEM_FULL		= 5;
	public final static int					TIME_LBL_ERROR			= 6;									// only
																											// used
																											// within
																											// amd?
	public final static int					TIME_LBL_BB_BEGIN		= 7;
	public final static int					TIME_LBL_BB_END			= 8;
	public final static int					TIME_LBL_HRA_TIME		= 9;
	public final static int					TIME_LBL_USER_BB_BEGIN	= 10;
	public final static int					TIME_LBL_BEEP			= 11;
	public final static int					TIME_LBL_EVENT_EX		= 12;									// Extended
																											// Event
																											// marker
																											// (version
																											// 115+)

	private File							filePath;
	private DataInputStream					dis						= null;
	private int								m_iFileVersion;

	private LinkedList<Ams5fsPacket>		starts					= new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket>		events					= new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket>		summaries				= new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket>		statics					= new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket>		settings				= new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket>		others					= new LinkedList<Ams5fsPacket>();
	private ArtefactSet						artefacts				= new ArtefactSet();
	private BeatSet							bSet					= new BeatSet();

	private LinkedList<Ams5fsChannelInfo>	channels				= new LinkedList<Ams5fsChannelInfo>();

	private GregorianCalendar				startCal				= null;
	// private int m_iIbiToBtbCorrection_ms;
	private int								m_lTimeShift_secs;
	private long							holdTime				= -1;
	private boolean							contReq					= false;
	private double							curTime					= 0;
	private DataOutputStream				dosDZ;
	private double							m_srDZ;
	private double							curDZTime;
	private int								oldDZVal				= 0;
	private DataOutputStream				dosMOT;
	private int								m_srMOT;
	private double							curMOTTime;
	private Ams5fsHeader					header;
	private DataOutputStream				dosECG;
	// private long continueTime;
	private boolean							quick					= false;
	private DataOutputStream				dosZ0;
	private int								m_srENS ;
	private double							curZ0Time;
	ArrayList<Integer> 						Z0Values				= new ArrayList<Integer>();

	public BinDirTest(File filePath) {
		this.filePath = filePath;
	}

	public String getString(int length) throws IOException {
		String returnString = new String();
		char curChar;
		boolean done = false;
		for (int i = 0; i < length; i++) {
			curChar = (char) dis.readByte();
			if (curChar != 0 && done == false) {
				returnString += curChar;
			} else {
				done = true;
			}
		}
		return returnString;
	}

	public int getUByte() throws IOException {
		return (int) (dis.readByte() & 0xFF);
	}

	public long getUInt() throws IOException {
		return ((long) dis.readInt() & 0xFFFFFFFF);
	}

	public int getUShort() throws IOException {
		return (dis.readShort() & 0xFFFF);
	}

	public boolean handleRawTime(int wData) throws IOException {

		if (contReq && ((wData & 0x00ff) != TIME_LBL_CONTINUE)) {
			return false;
		} else {
			contReq = false;
		}

		int m_year = getUByte();
		int m_month = getUByte();
		int m_day = getUByte();
		int m_hour = getUByte();
		int m_min = getUByte();
		int m_sec = getUByte();
		int m_msecH = getUByte();
		int m_msecL = getUByte();

		GregorianCalendar cal = null;

		long m_msec = ((m_msecH << 8) + m_msecL);

		if (m_year >= 70 && m_year <= 99) {
			cal = new GregorianCalendar(1900 + m_year, m_month - 1, m_day, m_hour, m_min, m_sec);
		} else {
			cal = new GregorianCalendar(2000 + m_year, m_month - 1, m_day, m_hour, m_min, m_sec);
		}

		cal.add(Calendar.MILLISECOND, (int)m_msec);

		cal.add(Calendar.SECOND, m_lTimeShift_secs);

		if (startCal == null)
			startCal = (GregorianCalendar) cal.clone();

		long difTime = cal.getTimeInMillis() - startCal.getTimeInMillis();

		int tag = (wData & 0x00ff);

		if (tag == TIME_LBL_START) {
			Ams5fsPacket start = new Ams5fsPacket();
			start.setDwClockTick_ms(difTime);
			start.setDwPrevSummaryPos(0L);
			start.setDwReserved(0L);
			Ams5fsTime time = new Ams5fsTime(cal);
			start.settStamp(time);
			start.setcFileStartReason((byte) 0);
			starts.add(start);
			curDZTime = difTime * 1000.;
			curMOTTime = difTime * 1000;
			curZ0Time = difTime * 1000;
		} else if (tag == TIME_LBL_HOLD) {
			if (holdTime == -1) {
				holdTime = difTime;
			}
			contReq = true;
		} else if (tag == TIME_LBL_CONTINUE) {
			if (holdTime != -1) {
				artefacts.add(AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime,false,0.0, "Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
		} else if (tag == TIME_LBL_BATT_LOW) {

		} else if (tag == TIME_LBL_EVENT) {
			Ams5fsPacket event = new Ams5fsPacket();
			event.setDwClockTick_ms(difTime);
			event.setlType(2);
			event.setlCode(0);
			event.setSzMessage("From AMS File");
			events.add(event);
		} else if (tag == TIME_LBL_MEM_FULL) {
			return false;
		} else if (tag == TIME_LBL_ERROR) {
			return false;
		} else if (tag == TIME_LBL_BB_BEGIN) {
			if (holdTime != -1) {
				artefacts.add(AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime, false,0.0,"Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
		} else if (tag == TIME_LBL_BB_END) {
			if (holdTime == -1) {
				holdTime = difTime;
			}
		} else if (tag == TIME_LBL_HRA_TIME) {

		} else if (tag == TIME_LBL_USER_BB_BEGIN) {
			if (holdTime != -1) {
				artefacts.add(AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime,false,0.0, "Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
		} else if (tag == TIME_LBL_BEEP) {

		} else if (tag == TIME_LBL_EVENT_EX) {
			int code = getUShort();
			Ams5fsPacket event = new Ams5fsPacket();
			event.setDwClockTick_ms(difTime);
			event.setlType(2);
			event.setlCode(code);
			event.setSzMessage("From AMS File");
			events.add(event);
		}

		if (tag == TIME_LBL_BB_BEGIN) {
			if (m_iFileVersion >= 108) {
				getUShort();
			}
		}

		if (difTime < 0 || difTime > 1000. * 60 * 60 * 24)
			return true;

		curTime = difTime * 1000.;

		if (curTime - curDZTime > 3 * m_srDZ * 1000000.) {
			int nPlus = (int) ((curTime - curDZTime) / (m_srDZ * 1000000.));
			for (int i = 0; i < nPlus; i++) {
				dosDZ.writeInt(oldDZVal);
				curDZTime += m_srDZ * 1000000.;
			}
		}

		if (curTime - curMOTTime > 3 * m_srMOT * 1000000.) {
			int nPlus = (int) ((curTime - curMOTTime) / (m_srMOT * 1000000.));
			for (int i = 0; i < nPlus; i++) {
				dosMOT.writeInt((int) Integer.MIN_VALUE);
				dosECG.writeInt((int) Integer.MAX_VALUE);
				curMOTTime += m_srMOT * 1000000.;
			}
		}
		if (curTime - curZ0Time > 3 * m_srENS * 1000000.) {
			int nPlus = (int) ((curTime - curZ0Time) / (m_srENS * 1000000.));
			for (int i = 0; i < nPlus; i++) {
				dosZ0.writeInt(0);			
				curZ0Time += m_srENS * 1000000.;
			}
		}

		return true;

	}

	private void OutputInfoToJSON(File tempDir) throws IOException {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "header.json"))));
		writer.print(gson.toJson(header));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "channels.json"))));
		writer.print(gson.toJson(channels));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "events.json"))));
		writer.print(gson.toJson(events));
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "statics.json"))));
		writer.print(gson.toJson(statics));
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "starts.json"))));
		writer.print(gson.toJson(starts));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "summaries.json"))));
		writer.print(gson.toJson(summaries));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "settings.json"))));
		writer.print(gson.toJson(settings));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "others.json"))));
		writer.print(gson.toJson(others));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "offsets.json"))));
		writer.print(gson.toJson(new ArrayList<Ams5fsPacket>()));
		writer.close();

		/*
		 * for (Integer i : getRecordedChannelTags()) { int index =
		 * getRecordedChannelTags().indexOf(i); writer = new PrintWriter(new
		 * BufferedWriter(new FileWriter(tempDir + File.separator +
		 * getChannelNameFromTag(i) + "offsets.json")));
		 * writer.print(gson.toJson(offsets.get(index))); writer.close(); }
		 */

		artefacts.saveToJSON(new File(tempDir, "artefacts.json"));
		bSet.saveToJSON(new File(tempDir, "beats.json"));

	}

	@Override
	public void run() {
		if (filePath != null) {
			File tempDir = Utils.getUniqueTemporaryDirectory();
			try {
				Ams5fsChannelInfo dzChan = new Ams5fsChannelInfo();
				dzChan.setSzID("DZ");
				dzChan.setDwDivider(1);
				dzChan.setlMinValue((int) (0.003 * Integer.MIN_VALUE));
				dzChan.setlMaxValue((int) (0.003 * Integer.MAX_VALUE));
				dzChan.setlMinMaxDivider(1);
				dzChan.setnBits(16);
				dzChan.setSzUnit("Ohm");
				channels.add(dzChan);

				Ams5fsChannelInfo motChan = new Ams5fsChannelInfo();
				motChan.setSzID("MYA");
				motChan.setDwDivider(1000);
				motChan.setlMinValue(0);
				motChan.setlMaxValue((int) ((Integer.MAX_VALUE - Integer.MIN_VALUE) * 4. / 512.));
				motChan.setnBits(16);
				motChan.setlMinMaxDivider(1);
				motChan.setSzUnit("g");
				channels.add(motChan);

				Ams5fsChannelInfo ecgChan = new Ams5fsChannelInfo();
				ecgChan.setSzID("ECG");
				ecgChan.setDwDivider(1);
				ecgChan.setlMinValue(0);
				ecgChan.setlMaxValue((int) ((Integer.MAX_VALUE - Integer.MIN_VALUE) * 4. / 512.));
				ecgChan.setnBits(16);
				ecgChan.setlMinMaxDivider(1);
				ecgChan.setSzUnit("V");
				channels.add(ecgChan);

				Ams5fsChannelInfo icgChan = new Ams5fsChannelInfo();
				icgChan.setSzID("DZDT");
				icgChan.setDwDivider(1);
				icgChan.setlMinValue(Integer.MIN_VALUE / 100);
				icgChan.setlMaxValue(Integer.MAX_VALUE / 100);
				icgChan.setnBits(16);
				icgChan.setlMinMaxDivider(1);
				icgChan.setSzUnit("Ohm/s");
				channels.add(icgChan);

				//--- Adding Z0 Channel to channel information ------
				Ams5fsChannelInfo Z0Chan = new Ams5fsChannelInfo();
				Z0Chan.setSzID("Z0");
				Z0Chan.setDwDivider(4); // m_srENS - ensembled average time	
				Z0Chan.setlMinValue((int)(((0))));
				Z0Chan.setlMaxValue((int)(((24))));
				Z0Chan.setlMinMaxDivider(1);
				Z0Chan.setnBits(10);
				Z0Chan.setSzUnit("Ohm");
				channels.add(Z0Chan);
				//----------------------------------------------------
				File[] infiles = filePath.listFiles(new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				        return name.toLowerCase().endsWith(".bin");
				    }
				});
				for (File infile : infiles) {
					Files.copy(infile.toPath(), (new File(tempDir + File.separator + infile.getName())).toPath());
				}

				/*Ams5fsPacket summary = new Ams5fsPacket();
				long msTime = Math.round(curTime / 1000);
				summary.setDwClockTick_ms(msTime);*/
				GregorianCalendar cal = new GregorianCalendar();
				/*cal.add(Calendar.MILLISECOND, (int) msTime);
				Ams5fsTime tStamp = new Ams5fsTime(cal);
				summary.settStamp(tStamp);
				summaries.add(summary);*/

				header = new Ams5fsHeader();
				header.setsFileID("AMS2");
				header.setDwSoftwareVersion(0);
				header.setDwHardwareVersion(7);
				header.setDwSerialNumber("0");
				header.setSzCompileDate("Unknown");
				header.setSzProducer("AMS - Vrije Universiteit van Amsterdam - FPP - ITM");
				header.settStamp(new Ams5fsTime(cal));
				header.setSzSubjectID("Dummy");
				header.setDwSampleTime_us(1000);
				header.setnChannels(3);

				long difTime = cal.getTimeInMillis();
				Ams5fsPacket start = new Ams5fsPacket();
				start.setDwClockTick_ms(difTime);
				start.setDwPrevSummaryPos(0L);
				start.setDwReserved(0L);
				Ams5fsTime time = new Ams5fsTime(cal);
				start.settStamp(time);
				start.setcFileStartReason((byte) 0);
				starts.add(start);
				// addDZDTToChannelInfo();

				OutputInfoToJSON(tempDir);

				String baseName = filePath.getAbsolutePath();
				File cfgFile = new File(filePath.getParent(), "label.cfg");
				if (cfgFile.exists()) {
					AmsLabelConfiguration lblcfg = new AmsLabelConfiguration();
					lblcfg.getConfigFromFile(cfgFile);
					lblcfg.saveToJSON(new File(tempDir, "labelconfig.json"));
					File labFile = new File(baseName + ".lbl");
					if (labFile.exists() == false) {
						labFile = new File(baseName + "_icg.lbl");
					}
					if (labFile.exists()) {
						AmsLabelSet lSet = new AmsLabelSet();
						lSet.importFromFile(labFile, startCal, 0);
						lSet.saveToJSON(new File(tempDir, "labels.json"));
					}
				}

				File beatFile = new File(baseName + ".beat");

				if (beatFile.exists()) {
					BeatSet bSet = new BeatSet();
					bSet.importFromFile(beatFile);
					bSet.recalculateAllSuspiciousLevels();
					bSet.saveToJSON(new File(tempDir, "beats.json"));
					ArtefactSet aSet = bSet.getArtefactsFromBeats(beatFile);
					if (aSet != null) {
						artefacts.getLabels().addAll(aSet.getLabels());
						artefacts.saveToJSON(new File(tempDir, "artefacts.json"));
					}
				}

				CurrentOpenData.getInstance().setDataFile(filePath, (short) -1);
				CurrentOpenData.getInstance().Open(tempDir, quick,false,0);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void setQuick(boolean quick) {
		this.quick = quick;
	}
}
