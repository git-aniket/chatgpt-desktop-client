package nl.vu.psy.ams.suite.data.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 * Class that opens a .ams file, converts the contents
 * to the VU-DAMS format and copies the result to a
 * unique temporary directory. 
 */
public class AmsAMSFile extends Thread {

	public final static int TIME_LBL_START = 0;
	public final static int TIME_LBL_HOLD = 1;
	public final static int TIME_LBL_CONTINUE = 2;
	public final static int TIME_LBL_BATT_LOW = 3;
	public final static int TIME_LBL_EVENT = 4;
	public final static int TIME_LBL_MEM_FULL = 5;
	public final static int TIME_LBL_ERROR = 6; // only
												// used
												// within
												// amd?
	public final static int TIME_LBL_BB_BEGIN = 7;
	public final static int TIME_LBL_BB_END = 8;
	public final static int TIME_LBL_HRA_TIME = 9;
	public final static int TIME_LBL_USER_BB_BEGIN = 10;
	public final static int TIME_LBL_BEEP = 11;
	public final static int TIME_LBL_EVENT_EX = 12; // Extended
													// Event
													// marker
													// (version
													// 115+)

	private File filePath;
	private DataInputStream dis = null;
	private int m_iFileVersion;

	private LinkedList<Ams5fsPacket> starts = new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket> events = new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket> summaries = new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket> statics = new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket> settings = new LinkedList<Ams5fsPacket>();
	private LinkedList<Ams5fsPacket> others = new LinkedList<Ams5fsPacket>();
	private ArtefactSet artefacts = new ArtefactSet();
	private BeatSet bSet = new BeatSet();

	private LinkedList<Ams5fsChannelInfo> channels = new LinkedList<Ams5fsChannelInfo>();

	private GregorianCalendar startCal = null;
	// private int m_iIbiToBtbCorrection_ms;
	private int m_lTimeShift_secs;
	private long holdTime = -1;
	private boolean continued = true;
	private int majorSerialNumber;
	private int minorSerialNumber;
	private boolean contReq = false;
	private double curTime = 0;
	private DataOutputStream dosDZ;
	private double m_srDZ;
	private double curDZTime;
	private int oldDZVal = 0;
	private DataOutputStream dosMOT;
	private int m_srMOT;
	private double curMOTTime;
	private int oldMOTVal;
	private Ams5fsHeader header;
	private DataOutputStream dosECG;
	// private long continueTime;
	private boolean quick = false;
	private BeatSet icgBeatSet = new BeatSet();
	private double m_srDZDT;
	private int[] icgdata = new int[1];
	private DataOutputStream dosICG;
	private DataOutputStream dosZ0;
	private int m_srENS;
	private double curZ0Time;
	private int oldZ0Val;

	ArrayList<Integer> Z0Values = new ArrayList<Integer>();

	public AmsAMSFile(File filePath) {
		this.filePath = filePath;
	}

	private int getSign(byte val) {
		if (val < 0)
			return -1;
		return 1;
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

	private void handleDZ0(int wData) throws IOException {
		int dzVal = wData;
		if ((wData & 0x1000) != 0) {
			dzVal = wData | 0xffffe000;
		}
		dosDZ.writeInt(dzVal);
		oldDZVal = dzVal;
		curDZTime += m_srDZ * 1000000.;
	}

	private void handleICG(int wData) throws IOException {
		byte[] tmp = new byte[128];
		dis.readFully(tmp);

		int[] vals = rawICGtoShorts(tmp);

		for (ECGBeat b : icgBeatSet.getBeats()) {
			writeICGtoStream(b.getRPeakTime(), vals);
		}
		icgBeatSet.getBeats().clear();
	}

	private void handleMOT(int wData) throws IOException {
		oldMOTVal = wData + Short.MIN_VALUE;
		dosMOT.writeInt(oldMOTVal);
		dosECG.writeInt(Short.MAX_VALUE);
		curMOTTime += m_srMOT * 1000000.;
	}

	private void handleZ0(int wData) throws IOException {
		oldZ0Val = wData + 0; // Minimum value is zero
		dosZ0.writeInt(oldZ0Val);
		curZ0Time += m_srENS * 1000000.;
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

		int m_msec = ((m_msecH << 8) + m_msecL);

		if (m_year >= 70 && m_year <= 99) {
			cal = new GregorianCalendar(1900 + m_year, m_month - 1, m_day, m_hour, m_min, m_sec);
		} else {
			cal = new GregorianCalendar(2000 + m_year, m_month - 1, m_day, m_hour, m_min, m_sec);
		}

		cal.add(Calendar.MILLISECOND, m_msec);

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
				artefacts.add(
						AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime, false, 0.0, "Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
			continued = true;
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
				artefacts.add(
						AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime, false, 0.0, "Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
			continued = true;
		} else if (tag == TIME_LBL_BB_END) {
			if (holdTime == -1) {
				holdTime = difTime;
			}
		} else if (tag == TIME_LBL_HRA_TIME) {

		} else if (tag == TIME_LBL_USER_BB_BEGIN) {
			if (holdTime != -1) {
				artefacts.add(
						AmsLabel.generateECGArtefact(1000. * holdTime, 1000. * difTime, false, 0.0, "Hold/Continue"));
				// continueTime = difTime;
				holdTime = -1;
			}
			continued = true;
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
				dosMOT.writeInt(Integer.MIN_VALUE);
				dosECG.writeInt(Integer.MAX_VALUE);
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

	private void handleSND(int wData) throws IOException {
		byte[] tmp = new byte[128];
		dis.readFully(tmp);
	}

	private boolean handleTag(int wData) throws IOException {
		int tag = wData & 0xe000;
		wData &= 0x1fff;
		if (tag == 0x0000) { // TIME
			return handleRawTime(wData);
		} else if (tag == 0x2000) { // ICG
			if (majorSerialNumber == 3) {
			} else {
				handleZ0(wData); // created new method to handle Z0 values
				handleICG(wData);
			}
		} else if (tag == 0x4000) { // DZ0
			handleDZ0(wData);
		} else if (tag == 0x6000) { // HRV
		} else if (tag == 0x8000) { // IBI
			int ibiTime = wData;
			ECGBeat b = new ECGBeat(curTime + (ibiTime * 1000. / 2));
			curTime += ibiTime * 1000.;
			b.setFirstInSeries(continued);
			continued = false;
			bSet.addBeat(b);
			icgBeatSet.addBeat(b);
		} else if (tag == 0xa000) { // HRA
		} else if (tag == 0xc000) { // MOT
			handleMOT(wData);
		} else if (tag == 0xe000) { // SND
			handleSND(wData);
		} else { // UNK
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

	private int[] rawICGtoShorts(byte[] tmp) {
		int[] ret = new int[tmp.length];
		int se_mask = 0;
		if (tmp[0] < 0 && tmp[0] >= -64)
			se_mask = -256; // 0xff00
		ret[0] = (int) (se_mask + (tmp[0] & 0xff));
		for (int i = 1; i < tmp.length; i++) {
			byte val = tmp[i];
			if (getSign(val) != getSign(tmp[i - 1])) {
				if (Math.abs(val) < 64) {
					if (getSign(val) == 1) {
						se_mask += 256;
					} else {
						se_mask -= 256;
					}
				}
			}
			ret[i] = (int) (se_mask + (tmp[i] & 0xff));
		}
		return ret;
	}

	@SuppressWarnings({ "unused", "resource" })
	@Override
	public void run() {
		if (filePath != null) {
			File tempDir = Utils.getUniqueTemporaryDirectory();
			try {
				JFrame frame = MainFrame.getInstance().getMainFrame();
				dis = new DataInputStream(
						new ProgressMonitorInputStream(frame, "Opening .ams file", new BufferedInputStream(
								new FileInputStream(filePath))));
				dosDZ = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(tempDir, "DZ.bin"))));
				dosMOT = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(tempDir, "MYA.bin"))));
				dosECG = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(tempDir, "ECG.bin"))));
				dosZ0 = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(tempDir, "Z0.bin"))));

				m_iFileVersion = 0;
				m_lTimeShift_secs = 0;
				int wData = getUShort();
				if (wData == 0x1a00) {
					UnsignedByteBuffer ubb = UnsignedByteBuffer.getInstance();
					byte[] buf = new byte[1022];
					dis.readFully(buf);
					ubb.setBytes(buf);
					int cTag = ubb.getUByte();
					int cSize = ubb.getUByte();
					int iSoftwareVersion = ubb.getUShort();
					m_iFileVersion = iSoftwareVersion;
					long lCompileDate = ubb.getUInt();
					int iCpuSpeed_kHz = ubb.getUShort();
					int iRamConfig = ubb.getUShort();
					int nPages = ubb.getUByte();
					int nSampleRates = ubb.getUByte();
					int iSerialFallbackTimeout = ubb.getUShort();
					int iSampleMode = ubb.getUShort();
					int bLedEnabled = ubb.getUByte();
					int iRXHoldOff = ubb.getUByte();
					int iSamplerateHRA = ubb.getUShort();
					int iSampleRateMOT = ubb.getUShort();
					int iSampleRate0 = ubb.getUShort();
					int iSampleRate1 = ubb.getUShort();
					int iSampleRate2 = ubb.getUShort();
					int bSupportsSound = ubb.getUByte();
					int reserved0 = ubb.getUByte();
					int reserved1 = ubb.getUByte();
					int reserved2 = ubb.getUByte();

					m_lTimeShift_secs = ubb.getInt();

					wData = getUShort();
				}

				majorSerialNumber = wData / 1000;
				minorSerialNumber = wData % 1000;

				int stringLength = getUByte();

				String m_strIdentification = getString(stringLength);

				if (stringLength % 2 == 0) {
					int tmp = dis.readByte();
				}

				int m_srHRA, m_srHRV = 0;
				if (majorSerialNumber == 3) {
					double m_srGSR = (double) getUShort() / 1000;
					getUShort();
					m_srHRA = getUShort();
					m_srHRV = m_srHRA;
					m_srMOT = getUShort();
				} else {
					m_srHRA = getUShort();
					m_srHRV = m_srHRA;
					m_srMOT = getUShort();
					m_srENS = getUShort();
					m_srDZ = (double) getUShort() / 1000;
					m_srDZDT = (double) getUShort() / 1000;
				}

				Ams5fsChannelInfo dzChan = new Ams5fsChannelInfo();
				dzChan.setSzID("DZ");
				dzChan.setDwDivider(Math.round(m_srDZ * 1000));
				dzChan.setlMinValue((int) (0.003 * Integer.MIN_VALUE));
				dzChan.setlMaxValue((int) (0.003 * Integer.MAX_VALUE));
				dzChan.setlMinMaxDivider(1);
				dzChan.setnBits(16);
				dzChan.setSzUnit("\u2126");
				channels.add(dzChan);

				Ams5fsChannelInfo motChan = new Ams5fsChannelInfo();
				motChan.setSzID("MYA");
				motChan.setDwDivider(Math.round(m_srMOT * 1000));
				motChan.setlMinValue(0);
				motChan.setlMaxValue((int) ((Integer.MAX_VALUE - Integer.MIN_VALUE) * 4. / 512.));
				motChan.setnBits(16);
				motChan.setlMinMaxDivider(1);
				motChan.setSzUnit("g");
				channels.add(motChan);

				Ams5fsChannelInfo ecgChan = new Ams5fsChannelInfo();
				ecgChan.setSzID("ECG");
				ecgChan.setDwDivider(Math.round(m_srMOT * 1000));
				ecgChan.setlMinValue(0);
				ecgChan.setlMaxValue((int) ((Integer.MAX_VALUE - Integer.MIN_VALUE) * 4. / 512.));
				ecgChan.setnBits(16);
				ecgChan.setlMinMaxDivider(1);
				ecgChan.setSzUnit("V");
				channels.add(ecgChan);

				Ams5fsChannelInfo icgChan = new Ams5fsChannelInfo();
				icgChan.setSzID("DZDT");
				icgChan.setDwDivider(Math.round(m_srDZDT * 1000));
				icgChan.setlMinValue((int) (Integer.MIN_VALUE / 100));
				icgChan.setlMaxValue((int) (Integer.MAX_VALUE / 100));
				icgChan.setnBits(16);
				icgChan.setlMinMaxDivider(1);
				icgChan.setSzUnit("\u2126/s");
				channels.add(icgChan);

				// --- Adding Z0 Channel to channel information ------
				Ams5fsChannelInfo Z0Chan = new Ams5fsChannelInfo();
				Z0Chan.setSzID("Z0");
				Z0Chan.setDwDivider(Math.round(m_srENS * 1000)); // m_srENS - ensembled average time
				Z0Chan.setlMinValue((int) (((0))));
				Z0Chan.setlMaxValue((int) (((24))));
				Z0Chan.setlMinMaxDivider(1);
				Z0Chan.setnBits(10);
				Z0Chan.setSzUnit("\u2126");
				channels.add(Z0Chan);
				// ----------------------------------------------------

				wData = getUShort();
				int tag = wData & 0xe000;
				// wData &= 0x1fff;
				if (tag != 0x0000 || (wData & 0x00ff) != 0) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "AMS File open error",
							"AMS File error", JOptionPane.ERROR_MESSAGE);
					dis.close();
					dosDZ.close();
					dosMOT.close();
					dosZ0.close();
					dosECG.close();
					dosICG.close();
					return;
				}

				startCal = null;

				while (handleTag(wData)) {
					wData = getUShort();
				}
				/*
				 * frame.toFront();
				 * frame.requestFocus();
				 */

				ProgressMonitor pm = new ProgressMonitor(frame, "Opening .ams file", null, 0, icgdata.length);
				dosICG = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(tempDir, "DZDT.bin"))));
				System.out.println(icgdata.length);
				for (int i = 0; i < icgdata.length; i++) {
					pm.setProgress(i);
					dosICG.writeInt(icgdata[i]);
				}

				/*
				 * for (int i = 0; i < Z0Values.size(); i++) {
				 * dosZ0.writeInt(Z0Values.get(i));
				 * }
				 */
				pm.close();
				/*
				 * frame.toFront();
				 * frame.requestFocus();
				 */
				dosICG.close();
				dosZ0.close();
				dosDZ.close();
				dosMOT.close();
				dosECG.close();

				Ams5fsPacket summary = new Ams5fsPacket();
				long msTime = Math.round(curTime / 1000);
				summary.setDwClockTick_ms(msTime);
				GregorianCalendar cal = (GregorianCalendar) startCal.clone();
				cal.add(Calendar.MILLISECOND, (int) msTime);
				Ams5fsTime tStamp = new Ams5fsTime(cal);
				summary.settStamp(tStamp);
				summaries.add(summary);

				header = new Ams5fsHeader();
				header.setsFileID("AMS2");
				header.setDwSoftwareVersion(m_iFileVersion);
				header.setDwHardwareVersion(majorSerialNumber);
				header.setDwSerialNumber(Integer.toString(minorSerialNumber));
				header.setSzCompileDate("Unknown");
				header.setSzProducer("AMS - Vrije Universiteit van Amsterdam - FPP - ITM");
				header.settStamp(new Ams5fsTime(startCal));
				header.setSzSubjectID(m_strIdentification);
				header.setDwSampleTime_us(1000);
				header.setnChannels(3);

				// addDZDTToChannelInfo();

				OutputInfoToJSON(tempDir);

				int dotIndex = filePath.getAbsolutePath().lastIndexOf(".");
				String baseName = filePath.getAbsolutePath().substring(0, dotIndex);
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
				CurrentOpenData.getInstance().Open(tempDir, quick, false, 0);

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (dis != null)
					try {
						dis.close();
						/*
						 * frame.toFront();
						 * frame.requestFocus();
						 */
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

		}
	}

	public void setQuick(boolean quick) {
		this.quick = quick;
	}

	private void writeICGtoStream(double rPeakTime, int[] tmp) {
		double loffset = (rPeakTime - 12000) / (m_srDZDT * 1000000);
		int lOff = (int) Math.round(loffset);
		int rOff = lOff + tmp.length;
		while (rOff > icgdata.length) {
			int[] tmpList = new int[icgdata.length * 2];
			for (int i = 0; i < tmpList.length; i++)
				tmpList[i] = 0;
			for (int i = 0; i < icgdata.length; i++)
				tmpList[i] = icgdata[i];
			icgdata = tmpList;
		}
		for (int i = 0; i < tmp.length; i++) {
			int off = lOff + i;
			if (off >= 0) {
				icgdata[off] = tmp[i];
			}
		}
	}
}
