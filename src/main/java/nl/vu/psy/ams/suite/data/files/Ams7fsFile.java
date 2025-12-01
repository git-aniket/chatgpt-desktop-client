package nl.vu.psy.ams.suite.data.files;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

//import javax.annotation.processing.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsPacket;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
/*
 * Class that opens a 7fs file, converts the contents
 * to the VU-DAMS format and copies the result to a
 * unique temporary directory. 
 * Is still a stub, will be extended as more info on the file format becomes availabe.
 */

public class Ams7fsFile extends Thread {

	// helper classes to deserialize json from 7fs file
	// @Generated("jsonschema2pojo")
	private static class Calc {

		private Calc() {
		}

		@SerializedName("name")
		@Expose
		private String name;
		@SerializedName("formula")
		@Expose
		private String formula;
		@SerializedName("unit")
		@Expose
		private String unit;

		/*
		 * public Double getA0() {
		 * return a0;
		 * }
		 * 
		 * public void setA0(Double a0) {
		 * this.a0 = a0;
		 * }
		 * 
		 * public Double getA1() {
		 * return a1;
		 * }
		 * 
		 * public void setA1(Double a1) {
		 * this.a1 = a1;
		 * }
		 */
	}

	private static class Datum {

		private Datum() {
		}

		@SerializedName("name")
		@Expose
		private String name;
		@SerializedName("format")
		@Expose
		private String format;
	}

	public static class ChannelSet {

		private ChannelSet() {
		}

		@SerializedName("data")
		@Expose
		private List<List<Datum>> data = null;
		@SerializedName("calc")
		@Expose
		private List<Map<String, String>> calc = null;

		public List<Map<String, String>> getCalc() {
			return calc;
		}
	}

	// InstanceCreator classes
	static class CourseCreator implements InstanceCreator<ChannelSet> {
		@Override
		public ChannelSet createInstance(Type type) {
			ChannelSet set = new ChannelSet();
			return set;
		}
	}

	static class DatumCreator implements InstanceCreator<Datum> {
		@Override
		public Datum createInstance(Type type) {
			Datum set = new Datum();
			return set;
		}
	}

	static class CalcCreator implements InstanceCreator<Calc> {
		@Override
		public Calc createInstance(Type type) {
			Calc set = new Calc();
			return set;
		}
	}

	// end helper classes

	private static Logger logger = LogManager.getLogger(Ams7fsFile.class.getName());

	private Ams7fsHeader fileHeader;
	private ArrayList<Ams7fsChannelInfo> channelInfo;
	private DataInputStream in;
	// private ByteBuffer inBuffer;
	private Map<Integer, Integer> tagToIndex;
	private Map<Integer, String> tagToName;
	// private ArrayList<Integer> recordedChannelTags;
	private List<List<String>> recordedChannels;
	private List<String> recordedTicks;
	private ArrayList<Ams7fsPacket> events, statics, starts, summaries, settings, others;
	// private ArrayList<ArrayList<Ams7fsPacket>> offsets;
	private ArrayList<FileChannel> fcout = new ArrayList<FileChannel>();
	private ArrayList<FileOutputStream> foss = new ArrayList<FileOutputStream>();
	private ArrayList<FileChannel> fcoutT = new ArrayList<FileChannel>();
	private ArrayList<FileOutputStream> fossT = new ArrayList<FileOutputStream>();
	private String filePath;
	private int nStarts = 0;
	private File tempDir;
	private final UnsignedByteBuffer ubb = UnsignedByteBuffer.getInstance();
	private final int[] buffer = new int[256];
	private final int[] tickBuffer = new int[8];
	private ArrayList<ByteBuffer> bbs = new ArrayList<ByteBuffer>();
	private ArrayList<IntBuffer> lbs = new ArrayList<IntBuffer>();
	private ArrayList<ByteBuffer> bbt = new ArrayList<ByteBuffer>();
	private ArrayList<IntBuffer> lbt = new ArrayList<IntBuffer>();
	private ArrayList<File> outFiles = new ArrayList<File>();

	private int[] nWritten, nWrittenTicks;

	private long[] nUSTime;

	private boolean quick = false;
	private long startTick = -1, len, prevTstamp = 0;
	private int prevTime = 0, prevTimeA = 0, /* chunk = 0, chunks, */ totalDiffA = 0, totalDiff = 0, diffCountA = 0,
			diffCount = 0, skippedBytes;
	// private Map<String, Long> sampleCounts = new HashMap<>();
	private File amsFile;
	private boolean delete7fs = false, endOfFile = false, startOfFile = true;
	private ArrayList<File> mergeFiles;
	private byte b4[] = new byte[4]; // new byte[Ams7fsHeader.SIZE];
	// private ByteBuffer bb4 = ByteBuffer.wrap(b4);

	public Ams7fsFile() {

	}

	public Ams7fsFile(String fileName) {
		this.filePath = fileName;
		mergeFiles = new ArrayList<File>();
		mergeFiles.add(new File(fileName));
	}

	public Ams7fsFile(String fileName, File saveFile) {
		this.filePath = fileName;
		this.amsFile = saveFile;
		mergeFiles = new ArrayList<File>();
		mergeFiles.add(new File(fileName));
	}

	public Ams7fsFile(ArrayList<File> mergeFiles, File saveFile) {
		this.mergeFiles = mergeFiles;
		this.amsFile = saveFile;
		this.filePath = mergeFiles.get(0).getPath();
	}

	private void addDZDTToChannelInfo(File tempDir) throws IOException {
		Ams7fsChannelInfo dzdtChannel = new Ams7fsChannelInfo();
		Ams7fsChannelInfo dzChannel = null;
		for (Ams7fsChannelInfo s : channelInfo)
			if (s.getSzID().equals("DZ"))
				dzChannel = s;
		if (dzChannel == null)
			return;
		dzdtChannel.setSzID("DZDT");
		dzdtChannel.setSzUnit("\u2126/s");
		// long dzSampleTimeInUS = fileHeader.getDwSampleTime_us() /
		// dzChannel.getDwDivider();
		// long factor = 1000000 / (2 * dzSampleTimeInUS);
		// ------------When DZ is recorded below 1000 Hz-------------------------
		// long factor = 1000000 / (2 * dzChannel.getDwDivider() * 1000);
		// ----------------------------------------------------------------------
		dzdtChannel.setlMinValue(-8192); // (int) (factor * dzChannel.getlMinValue()));
		dzdtChannel.setlMaxValue(8192); // (int) (factor * dzChannel.getlMaxValue()));
		dzdtChannel.setlMinMaxDivider((dzChannel.getlMinMaxDivider()));
		dzdtChannel.setnBits(dzChannel.getnBits());
		dzdtChannel.setDwDivider(dzChannel.getDwDivider());
		dzdtChannel.setTickFile("TicksA");
		channelInfo.add(dzdtChannel);
	}

	private void addDZToChannelInfo(File tempDir) throws IOException {
		Ams7fsChannelInfo dzChannel = new Ams7fsChannelInfo();
		Ams7fsChannelInfo zChannel = null;
		for (Ams7fsChannelInfo s : channelInfo)
			if (s.getSzID().equals("Z0"))
				zChannel = s;
		if (zChannel == null)
			return;
		dzChannel.setSzID("DZ");
		dzChannel.setSzUnit("\u2126");
		// dzChannel.setlMinValue(zChannel.getlMinValue()); // -8198
		// dzChannel.setlMaxValue(zChannel.getlMaxValue()); // 8186
		dzChannel.setRealConstant(zChannel.getRealConstant());
		dzChannel.setRealSlope(zChannel.getRealSlope());
		dzChannel.setlMinMaxDivider((zChannel.getlMinMaxDivider()));
		dzChannel.setnBits(zChannel.getnBits());
		dzChannel.setDwDivider(zChannel.getDwDivider());
		dzChannel.setTickFile("TicksA");
		channelInfo.add(dzChannel);
		// 21-08-24: change these for DZ, DZRESP and FILTDZ to the calibration of Z0?
		dzChannel = new Ams7fsChannelInfo();
		dzChannel.setSzID("DZRESP");
		dzChannel.setSzUnit("\u2126");
		// dzChannel.setlMinValue(zChannel.getlMinValue()); // -8198
		// dzChannel.setlMaxValue(zChannel.getlMaxValue()); // 8186
		dzChannel.setRealConstant(zChannel.getRealConstant());
		dzChannel.setRealSlope(zChannel.getRealSlope());
		dzChannel.setlMinMaxDivider((zChannel.getlMinMaxDivider()));
		dzChannel.setnBits(zChannel.getnBits());
		dzChannel.setDwDivider(zChannel.getDwDivider());
		dzChannel.setTickFile("TicksA");
		channelInfo.add(dzChannel);
	}

	private void addECGDiffToChannelInfo() {
		Ams7fsChannelInfo ecgChannel = null, ecg2Channel = null;
		for (Ams7fsChannelInfo s : channelInfo) {
			if (s.getSzID().equals("ECG"))
				ecgChannel = s;
			if (s.getSzID().equals("V2ecg"))
				ecg2Channel = s;
		}
		if (ecg2Channel == null || ecgChannel == null)
			return;
		Ams7fsChannelInfo chan = new Ams7fsChannelInfo();
		chan.setSzID("V3ecg");
		chan.setSzUnit("V");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(4.7683723096270114e-05);
		chan.setTickFile("TicksA");
		channelInfo.add(chan);

	}

	public void close() {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void closeAllOpenFiles() {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (fcout != null) {
			for (FileChannel o : fcout) {
				try {
					o.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (fcoutT != null) {
			for (FileChannel o : fcoutT) {
				try {
					o.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ArrayList<Ams7fsChannelInfo> getChannelInfo() {
		return channelInfo;
	}

	public String getChannelNameFromTag(Integer i) {
		return tagToName.get(i);
	}

	public Ams7fsHeader getFileHeader() {
		return fileHeader;
	}

	public int getIndexFromTag(int tag) {
		return tagToIndex.get(tag);
	}

	/*
	 * public ArrayList<Integer> getRecordedChannelTags() {
	 * return recordedChannelTags;
	 * }
	 */

	private void HandlePacket(Ams7fsPacket packet) throws IOException {
		if (packet.getwTag() >= Ams7fsPacket.PACKET_TYPE_DATABLOCK) {
			char packetType = (char) packet.getwTag();
			int[] dat = packet.getData();
			// skip all zero packets when file is not closed correctly
			if (dat != null) {
				boolean toSkip = true;
				for (int i : dat)
					if (i != 0)
						toSkip = false;
				if (toSkip)
					return;
			}
			int skip = 1, diffA = 0, diff = 0, addedChans = 0, dummies = 0;
			int chanIndex = -1, tickIndex = -1;
			List<String> packetChannels;
			switch (packetType) {
				case 'A':
					skip = 2;
					packetChannels = recordedChannels.get(0);
					addedChans = 2;
					chanIndex = 0;
					tickIndex = 0;
					break;
				case 'B':
					packetChannels = recordedChannels.get(1);
					chanIndex = recordedChannels.get(0).size();
					tickIndex = 1;
					break;
				case 'C':
					packetChannels = new ArrayList<String>();
					long tstamp = dat[1] & 0xFFFFFFFF; // why? for int of 32 bits this does nothing
					CreateStart(dat[0], tstamp);
					if (tstamp - prevTstamp != 10 && prevTstamp > 0)
						logger.error("timestamp: " + tstamp + " " + (tstamp - prevTstamp));
					prevTstamp = tstamp;
					dummies = 4;
					break;
				case 'D':
					packetChannels = recordedChannels.get(2);
					chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size();
					tickIndex = 2;
					break;
				case 'I':
					packetChannels = new ArrayList<String>();
					events.add(packet);
					break;
				case 'G':
					packetChannels = recordedChannels.get(3);
					chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size()
							+ recordedChannels.get(2).size();
					tickIndex = 3;
					dummies = 1;
					break;
				case 'M':
					packetChannels = recordedChannels.get(4);
					chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size()
							+ recordedChannels.get(2).size() + recordedChannels.get(3).size();
					tickIndex = 4;
					dummies = 1;
					break;
				default:
					packetChannels = new ArrayList<String>();
					tickIndex = -1;
					// if (packetType != 'C')
					System.out.println(packetType);
					return;
			}

			int packetIndex = 0;
			if (dat != null && packetType != 'C') { // for markers
				if (dat.length != packetChannels.size() + skip - addedChans + dummies) { // bad data
					logger.error("Packet of type " + packetType + " of incorrect length " + dat.length);
					return;
				}
			} else if (packetType == 'C') {
				if (dat.length != 3 && dat.length != 5) { // for int32 and int64 timestamps
					logger.error("Packet of type " + packetType + " of incorrect length " + dat.length);
					return;
				}
			}

			if (packetType == 'A') {
				if (prevTimeA == 0)
					diffA = 0;
				else
					diffA = dat[0] - prevTimeA;
				if (diffA != 1 && prevTimeA > 0) {/// (Math.abs (dat[0] - prevTimeA) > 2 && prevTimeA > 0)
					diffCountA++;
				}
				if (diffA < 0 || diffA > 3)
					logger.error("TickA> " + prevTimeA + " " + dat[0] + " " + diffA + " " + totalDiffA
							+ " " + diffCountA);
				// if (diffA > 100) {// assume incorrectly closed file with corrupt data
				// return;
				// }
				if (!startOfFile)
					if (diffA > 100000 || diffA < -2) {// assume incorrectly closed file with corrupt data
						endOfFile = true;
						return;
					}
				totalDiffA += diffA - 1;
				prevTimeA = dat[0];
				startOfFile = false;
			}
			if (prevTime == 0 || dat == null)
				diff = 0;
			else
				diff = dat[0] - prevTime;
			if ((diff != 1 && diff != 0) && prevTime > 0) {/// (Math.abs (dat[0] - prevTimeA) > 2 && prevTimeA > 0)
				diffCount++;
			}
			if (Math.abs(diff) > 100000) // assume incorrectly closed file with corrupt data
				return;
			if (diff < -2 || diff > 3)
				logger.error("Tick " + packetType + " " + prevTime + " " + dat[0] + " " + diff + " "
						+ totalDiff + " " + diffCount);
			if (!startOfFile)
				if ((diff > 100000 || diff < -10) && (packetType == 'A' || packetType == 'M')) {// assume incorrectly
																								// closed file with
																								// corrupt data
					endOfFile = true;
					logger.error("Reading 7fs file stopped due to too large tick gap!");
					return;
				}
			totalDiff += diff - 1;
			if (dat != null)
				prevTime = dat[0];

			for (@SuppressWarnings("unused")
			String chanName : packetChannels) {
				if (startTick == -1)
					startTick = dat[0];
				if (packetIndex + skip < dat.length)
					buffer[chanIndex] = dat[packetIndex + skip]; // - minVal;
				else if (packetIndex + skip == dat.length)
					buffer[chanIndex] = diffA;
				else
					buffer[chanIndex] = totalDiffA;
				IntBuffer test = lbs.get(chanIndex);
				test.put(buffer, chanIndex, 1);
				nWritten[chanIndex] += 1;
				if (nWritten[chanIndex] >= 10485760 / 4) {
					bbs.get(chanIndex).clear();
					bbs.get(chanIndex).limit(nWritten[chanIndex]);
					byte[] check = new byte[16];
					bbs.get(chanIndex).get(check);
					bbs.get(chanIndex).clear();
					bbs.get(chanIndex).limit(nWritten[chanIndex] * 4);
					fcout.get(chanIndex).write(bbs.get(chanIndex));
					lbs.get(chanIndex).clear();
					// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
					nWritten[chanIndex] = 0;

				}
				nUSTime[chanIndex] += fileHeader.getDwSampleTime_us() * channelInfo.get(chanIndex).getDwDivider();
				/*
				 * for (Integer s : packet.getData()) { dataOut.writeShort(s -
				 * minVal); }
				 */
				// }
				chanIndex++;
				packetIndex++;
			}
			if (dat != null && dat.length > 0 && tickIndex > -1) {
				tickBuffer[tickIndex] = dat[0]; // - minVal;
				IntBuffer testT = lbt.get(tickIndex);
				testT.put(tickBuffer, tickIndex, 1);
				nWrittenTicks[tickIndex] += 1;
				if (nWrittenTicks[tickIndex] >= 10485760 / 4) {
					bbt.get(tickIndex).clear();
					bbt.get(tickIndex).limit(nWrittenTicks[tickIndex]);
					byte[] check = new byte[16];
					bbt.get(tickIndex).get(check);
					bbt.get(tickIndex).clear();
					bbt.get(tickIndex).limit(nWrittenTicks[tickIndex] * 4);
					fcoutT.get(tickIndex).write(bbt.get(tickIndex));
					lbt.get(tickIndex).clear();
					// dataOut.write(bbt.get(tickIndex).array(),0,nWrittenTicks[tickIndex]);
					nWrittenTicks[tickIndex] = 0;
				}
			}
		} else if (packet.getwTag() == Ams7fsPacket.PACKET_TYPE_EVENT) {
			events.add(packet);
		} /*
			 * else if (packet.getwTag() == Ams7fsPacket.PACKET_TYPE_STATICS) {
			 * statics.add(packet);
			 * } else if (packet.getwTag() == Ams7fsPacket.PACKET_TYPE_START) {
			 * starts.add(packet);
			 * for (Integer index = 0; index < recordedChannels.size(); index++) {
			 * int curPos = (int) fcout.get(index).size();
			 * Ams7fsPacket tempPacket = new Ams7fsPacket();
			 * tempPacket.setOffset(curPos);
			 * offsets.get(index).add(tempPacket);
			 * }
			 * nStarts++;
			 * //if (nStarts > 1) {
			 * long startTime = starts.get(0).getDwClockTick_ms();
			 * long curTime = packet.getDwClockTick_ms();
			 * long diffTime = (curTime - startTime) * 1000;
			 * for (int i = 0; i < nUSTime.length; i++) {
			 * // Clear buffers
			 * if (nWritten[i] > 0) {
			 * bbs.get(i).clear();
			 * bbs.get(i).limit(nWritten[i]);
			 * fcout.get(i).write(bbs.get(i));
			 * lbs.get(i).clear();
			 * // dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
			 * nWritten[i] = 0;
			 * }
			 * long timeToBeWritten = diffTime - nUSTime[i];
			 * if (timeToBeWritten > 0) {
			 * int nShortsToBeWritten = (int) (timeToBeWritten /
			 * (fileHeader.getDwSampleTime_us() * channelInfo.get(i).getDwDivider()));
			 * ByteBuffer bb = ByteBuffer.allocate(2 * nShortsToBeWritten);
			 * IntBuffer sb = bb.asIntBuffer();
			 * for (int j = 0; j < nShortsToBeWritten; j++)
			 * sb.put(Integer.MIN_VALUE);
			 * fcout.get(i).write(bb);
			 * }
			 * }
			 * // throw new
			 * // IOException("Merged 5fs files are not supported yet.");
			 * }//
			 * } else if (packet.getwTag() == Ams7fsPacket.PACKET_TYPE_SUMMARY) {
			 * summaries.add(packet);
			 * } else if (packet.getwTag() == Ams7fsPacket.PACKET_TYPE_SETTINGS) {
			 * settings.add(packet);
			 * } else {
			 * others.add(packet);
			 * }
			 */
	}

	public boolean isAtEndOfFile() throws IOException {
		return (in.available() == 0);
	}

	public void OpenFile() throws IOException {
		/*
		 * Timer timer = new Timer();
		 * timer.start();
		 * if (progressDialog == true) {
		 * in = new DataInputStream(new
		 * ProgressMonitorInputStream(MainFrame.getInstance().getMainFrame(),
		 * "Opening 7fs file", new BufferedInputStream(
		 * new FileInputStream(filePath), 10485760)));
		 * } else {
		 * in = new DataInputStream(new BufferedInputStream(new
		 * FileInputStream(filePath), 10485760));
		 * }
		 * tempDir = Utils.getUniqueTemporaryDirectory();
		 * ReadFileHeader();
		 * if (fileHeader.getsFileID().equals("AMS7") == false)
		 * throw new IOException("File is not a valid 7fs file");
		 * ReadAllPackets(tempDir);
		 * in.close();
		 * timer.stop();
		 * if(AppSettings.getInstance().getIntProperty(Settings.DEBUG)==1)
		 * System.out.println("Opening 7fsFile took (" + timer.getTime() / 1000. +
		 * " sec)");
		 */
		// ExitApp.NormalExit();
		Timer timerB = new Timer();
		timerB.start();
		len = (new File(filePath).length());
		// chunks = (int) (len / 1073741824L) + 1;
		if (len < 4) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Could not read 7fs file. File is too short.");
			throw new IOException("File is not a valid 7fs file");
		}
		// ProgressMonitorInputStream fis = new
		// ProgressMonitorInputStream(MainFrame.getInstance().getMainFrame(),
		// "Opening 7fs file",
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(filePath), 10485760);
		// long heapSize = Runtime.getRuntime().totalMemory();

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.//
		// Any attempt will result in an OutOfMemoryException.
		// long heapMaxSize = Runtime.getRuntime().maxMemory();
		// byte[][] buf = new byte[chunks][1073741824];
		// for (int i = 0; i < chunks; i++)
		// fis.read(buf[i]);
		// inBuffer = ByteBuffer.wrap(buf[0]);
		// fis.close();
		// FileChannel fc = fis.getChannel();

		tempDir = Utils.getUniqueTemporaryDirectory();
		long bytesRead = ReadFileHeaderBuffered(fis);
		if (fileHeader == null || fileHeader.getsFileID().equals("AMS7") == false) {
			// fc.close();
			fis.close();
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Could not read 7fs file header. File is not a valid 7fs file.");
			throw new IOException("File is not a valid 7fs file");
		}
		fcout = new ArrayList<FileChannel>();
		foss = new ArrayList<FileOutputStream>();
		bbs = new ArrayList<ByteBuffer>();
		lbs = new ArrayList<IntBuffer>();
		fcoutT = new ArrayList<FileChannel>();
		bbt = new ArrayList<ByteBuffer>();
		lbt = new ArrayList<IntBuffer>();
		outFiles = new ArrayList<File>();
		ReadAllPacketsBuffered(tempDir, fis, len, bytesRead);
		// fc.close();
		fis.close();
		timerB.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Opening 7fsFile buffered took (" + timerB.getTime() / 1000. + " sec)");

		addDZToChannelInfo(tempDir);
		addDZDTToChannelInfo(tempDir);
		addECGDiffToChannelInfo();
		ArrayList<Ams7fsChannelInfo> channelInfo2 = new ArrayList<Ams7fsChannelInfo>();
		for (Ams7fsChannelInfo s : channelInfo)
			if (s.getSzID().equals("-"))
				channelInfo2.add(s);
		for (Ams7fsChannelInfo s : channelInfo2)
			channelInfo.remove(s);
		if (nStarts == 0) { // create start time
			GregorianCalendar cal = new GregorianCalendar();
			Date date = new Date();
			cal.setTime(date);
			Ams7fsPacket startPacket = new Ams7fsPacket();
			startPacket.setDwClockTick_ms((long) 0);
			startPacket.settStamp(new Ams5fsTime(cal));
			starts.add(startPacket);
			nStarts++;
		}
		OutputInfoToJSON(tempDir);
		CurrentOpenData.getInstance().setDataFile(new File(filePath), (short) -1);
	}

	private void OutputInfoToJSON(File tempDir) throws IOException {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "header.json"))));
		writer.print(gson.toJson(fileHeader));
		writer.close();

		writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir, "channels.json"))));
		writer.print(gson.toJson(channelInfo));
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

		// writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(tempDir,
		// "offsets.json"))));
		// writer.print(gson.toJson(offsets));
		// writer.close();

		// int i = 0;
		// for (List<String> list : recordedChannels) {
		// for (String s : list) {
		// int index = i;
		// writer = new PrintWriter(
		// new BufferedWriter(new FileWriter(tempDir + File.separator + s +
		// "offsets.json")));
		// writer.print(gson.toJson(offsets.get(index)));
		// writer.close();
		// i++;
		// }
		// }
	}

	private void CreateStart(long tick, long tStamp) {
		if (nStarts == 0) { // create start time
			long diff = tick - startTick;
			GregorianCalendar cal = new GregorianCalendar();
			Date date = new Date((tStamp - diff / 1000) * 1000L);
			cal.setTime(date);
			Ams7fsPacket startPacket = new Ams7fsPacket();
			startPacket.setDwClockTick_ms(startTick);
			startPacket.settStamp(new Ams5fsTime(cal));
			starts.add(startPacket);
			nStarts++;
		}
		GregorianCalendar cal = new GregorianCalendar();
		Date date = new Date(tStamp * 1000L);
		cal.setTime(date);
		Ams7fsPacket startPacket = new Ams7fsPacket();
		startPacket.setDwClockTick_ms(tick);
		startPacket.settStamp(new Ams5fsTime(cal));
		starts.add(startPacket);
		nStarts++;
	}

	@Override
	public void run() {
		logger.info("Start open file");
		long fs = new File(filePath).length();
		if (filePath != null) {
			if (3 * (new File(filePath)).length() > Utils.getFreeBytesInTemporaryDirectory()) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
						"Not enough free space in temporary directory!\nCurrent space available: "
								+ Utils.getFreeBytesInTemporaryDirectory() / 1000000
								+ " MB\nSpace needed: " + 3 * (new File(filePath)).length() / 1000000 + " MB",
						"Not enough free space!",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			MainFrame.getInstance().getMainFrame().setEnabled(false);
			tempDir = null;
			try {
				OpenFile();

				long startTimeInUS;
				GregorianCalendar startDate;
				if (starts.size() > 0) {
					startDate = starts.get(0).gettStamp().toGregorianCalendar();
					startTimeInUS = 1000 * starts.get(0).getDwClockTick_ms();
				} else {
					startDate = new GregorianCalendar();
					startTimeInUS = 0;
				}

				int dotIndex = filePath.lastIndexOf(".");
				String baseName = filePath.substring(0, dotIndex);

				File beatFile = new File(baseName + ".beat");
				if (beatFile.exists()) {
					BeatSet bSet = new BeatSet();
					bSet.importFromFile(beatFile);
					bSet.recalculateAllSuspiciousLevels();
					bSet.saveToJSON(new File(tempDir, "beats.json"));
					File invFile = new File(baseName + ".amsinv");
					if (invFile.exists()) {
						ArtefactSet aSet = new ArtefactSet();
						aSet.importFromFile(invFile);
						aSet.saveToJSON(new File(tempDir, "artefacts.json"));
					}
				}
				File pathOf5fs = new File(filePath);
				File cfgFile = new File(pathOf5fs.getParent(), "label.cfg");
				if (cfgFile.exists()) {
					AmsLabelConfiguration lblcfg = new AmsLabelConfiguration();
					lblcfg.getConfigFromFile(cfgFile);
					lblcfg.saveToJSON(new File(tempDir, "labelconfig.json"));
					File labFile = new File(baseName + "_icg.lbl");
					if (labFile.exists()) {
						AmsLabelSet lSet = new AmsLabelSet();
						lSet.importFromFile(labFile, startDate, startTimeInUS);
						lSet.saveToJSON(new File(tempDir, "labels.json"));
					}
				}
				if (isInterrupted()) {
					closeAllOpenFiles();
					if (tempDir != null) {
						ExitApp.deleteDirectory(tempDir);
					}
				} else {
					CurrentOpenData.getInstance().Open(tempDir, quick, false, 0, 24);
				}
			} catch (InterruptedIOException e) {
				closeAllOpenFiles();
				if (tempDir != null) {
					ExitApp.deleteDirectory(tempDir);
				}
			} catch (IOException e) {
				closeAllOpenFiles();
				if (fs == 0) {// Give specific error message for 0KB files
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"Error reading 7fs file...\n(" + e.getMessage()
									+ ")\n'0KB file' > Please see:\n www.vu-ams.nl/support/tutorials/troubleshooting",
							"File Error", JOptionPane.ERROR_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"Error reading 7fs file...\n(" + e.getMessage() + ")", "File Error",
							JOptionPane.ERROR_MESSAGE);
				}
				if (tempDir != null) {
					ExitApp.deleteDirectory(tempDir);
				}
			} // finally {
			closeAllOpenFiles();
			// }
			MainFrame.getInstance().getMainFrame().setEnabled(true);
		}
		// inBuffer = null;
		logger.info("Stop open file");
		if (amsFile != null) {
			AmsDataFile fl = new AmsDataFile(true, amsFile.getAbsolutePath());
			File file7fs = CurrentOpenData.getInstance().getDataFile();
			fl.setCompressed(true);
			fl.start();
			if (delete7fs) {
				try {
					fl.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				file7fs.delete();
			}
		}
	}

	public void setProgressDialog(boolean visible) {
	}

	public void setQuick(boolean quick) {
		this.quick = quick;
	}

	public void setDelete7fs(boolean delete) {
		this.delete7fs = delete;
	}

	public static byte crc8Check(byte[] dat) {
		if (dat.length == 0)
			return -1;
		byte result = dat[0];
		for (int i = 1; i < dat.length; i++)
			result = (byte) (dat[i] ^ result);
		return result;
	}

	public static Gson getHeaderGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(ChannelSet.class, new CourseCreator());
		gsonBuilder.registerTypeAdapter(Datum.class, new DatumCreator());
		gsonBuilder.registerTypeAdapter(Calc.class, new CalcCreator());
		return gsonBuilder.serializeSpecialFloatingPointValues().create();

	}

	/// Direct buffer methods for performance comparison
	private long ReadFileHeaderBuffered(BufferedInputStream fis) throws IOException {
		Gson gson = getHeaderGson();
		ChannelSet[] chanSets = new ChannelSet[5];
		// inBuffer.get(b);
		// long read = fc.read(bb4);
		long read = fis.read(b4);
		ubb.setBytes(b4);
		byte tag = ubb.getByte();
		if (tag != 72)
			logger.debug("Header tag not found");
		int bytes2 = ubb.getShort();
		int wSize = bytes2 & 0x0FFF;
		int type = bytes2 & 0xF000; // 0x8000 is json; 0 is data
		int nChannels = 0;
		if ((wSize - 4) <= 0) {
			return read;
		}
		byte block[] = new byte[wSize];
		for (int i = 0; i < 4; i++)
			block[i] = b4[i];
		while (type != 0) {
			byte[] b = new byte[wSize - 4];
			// inBuffer.get(b);
			// ByteBuffer bb = ByteBuffer.wrap(b);
			// read += fc.read(bb);
			fis.read(b);
			ubb.setBytes(b);
			for (int i = 4; i < wSize; i++)
				block[i] = b[i - 4];
			if (crc8Check(block) != 0) {
				System.out.println("crc failed header");
				return read;
			} else {
				String jsonString = ubb.getString(wSize - 4);
				ChannelSet map = gson.fromJson(jsonString, ChannelSet.class);
				// System.out.println(map.getCalc().toString());
				switch ((char) tag) {
					case 'A':
						chanSets[0] = map;
						break;
					case 'B':
						chanSets[1] = map;
						break;
					case 'D':
						chanSets[2] = map;
						break;
					case 'G':
						chanSets[3] = map;
						break;
					case 'M':
						chanSets[4] = map;
						break;
					default:
						if ((char) tag != 'C')
							System.out.println((char) tag);
				}
			}
			// bb4.position(0);
			// read += fc.read(bb4);
			read += fis.read(b4);
			ubb.setBytes(b4);
			tag = ubb.getByte();
			switch ((char) tag) {
				case 'A':
					nChannels = nChannels + 4;
					break;
				case 'B':
					nChannels++;
					break;
				case 'D':
					nChannels = nChannels + 2;
					break;
				case 'G':
					nChannels = nChannels + 3;
					break;
				case 'M':
					nChannels = nChannels + 7;
					break;
				default:
					if ((char) tag != 'C')
						System.out.println((char) tag);
			}
			bytes2 = ubb.getShort();
			wSize = bytes2 & 0x0FFF;
			type = bytes2 & 0xF000;
			if ((wSize - 4) <= 0) {
				return read;
			}
			block = new byte[wSize];
			if (block.length >= 4) {
				for (int i = 0; i < 4; i++)
					block[i] = b4[i];
			}
		}
		byte[] b = new byte[wSize - 4];
		// inBuffer.get(b);
		// ByteBuffer bb = ByteBuffer.wrap(b);
		// read += fc.read(bb);
		read += fis.read(b);
		ubb.setBytes(b);
		for (int i = 4; i < wSize; i++)
			block[i] = b[i - 4];
		if (crc8Check(block) != 0)
			System.out.println("crc failed header");

		// read H blocks
		gson = new Gson();
		Type typet = new TypeToken<Map<String, String>>() {
		}.getType();
		HashMap<String, String> map = new HashMap<String, String>();
		while (tag == 72) {
			String jsonString = ubb.getString(wSize - 4);
			// bb4.position(0);
			// read += fc.read(bb4);
			read += fis.read(b4);
			ubb.setBytes(b4);
			tag = ubb.getByte();
			bytes2 = ubb.getShort();
			wSize = bytes2 & 0x0FFF;
			type = bytes2 & 0xF000;
			if ((wSize - 4) <= 0) {
				return read;
			}
			block = new byte[wSize];
			for (int i = 0; i < 4; i++)
				block[i] = b4[i];
			b = new byte[wSize - 4];
			// bb = ByteBuffer.wrap(b);
			// read += fc.read(bb);
			read += fis.read(b);
			ubb.setBytes(b);
			for (int i = 4; i < wSize; i++)
				block[i] = b[i - 4];
			if (crc8Check(block) != 0)
				System.out.println("crc failed block H");
			else {
				Map<String, String> tempMap;
				try {
					tempMap = gson.fromJson(jsonString, typet);
				} catch (Exception e) { // dirty hack for bad header string of firmware saa-2023-39-g46108fa
					try {
						jsonString = jsonString + "\"xxx yy 2023\"}";
					} catch (Exception e2) {
					}
					tempMap = gson.fromJson(jsonString, typet);
				}
				map.putAll(tempMap);
			}
		}
		// fileHeader = new Ams7fsHeader(ubb);
		fileHeader = new Ams7fsHeader(map, nChannels);
		// HandlePacket(new Ams7fsPacket(ubb, tag, wSize));

		channelInfo = new ArrayList<Ams7fsChannelInfo>();
		recordedTicks = new ArrayList<String>();
		Ams7fsChannelInfo chan;
		recordedChannels = new ArrayList<List<String>>();
		List<String> subList = new ArrayList<String>();
		if (chanSets[0] != null) {
			List<Map<String, String>> ADC = chanSets[0].getCalc();
			for (Map<String, String> entry : ADC) {
				String name = entry.get("name");
				if (!name.equals("tv")) {
					chan = new Ams7fsChannelInfo();
					if (name.equals("Vicg"))
						name = "Z0";
					if (name.equals("Vecg"))
						name = "ECG";
					if (name.equals("SCL")) {
						chan.setFormula(entry.get("formula"));
						Map<String, Double> c = new HashMap<String, Double>();
						for (String key : entry.keySet()) {
							if (key.startsWith("a")) {
								Double value = Double.valueOf(entry.get(key));
								c.put(key, value);
							}
						}
						chan.setConstants(c);
					}
					chan.setSzID(name);
					chan.setSzUnit(entry.get("unit"));
					chan.setnBits(32);
					chan.setRealConstant(Double.valueOf(entry.get("a0")));
					chan.setRealSlope(Double.valueOf(entry.get("a1")));
					chan.setTickFile("TicksA");
					channelInfo.add(chan);
					subList.add(name);
				}
			}
			chan = new Ams7fsChannelInfo();
			chan.setSzID("Tickdiff_ADC"); //
			chan.setSzUnit("ms");
			chan.setnBits(16);
			chan.setRealConstant(0);
			chan.setRealSlope(1);
			chan.setTickFile("TicksA");
			subList.add(chan.getSzID());
			channelInfo.add(chan);
			chan = new Ams7fsChannelInfo();
			chan.setSzID("Tickdrift_ADC"); //
			chan.setSzUnit("ms");
			chan.setnBits(16);
			chan.setRealConstant(0);
			chan.setRealSlope(1);
			chan.setTickFile("TicksA");
			subList.add(chan.getSzID());
			channelInfo.add(chan);
		}
		recordedTicks.add("TicksA");
		recordedChannels.add(subList);
		subList = new ArrayList<String>();
		if (chanSets[1] != null) {
			List<Map<String, String>> BAT = chanSets[1].getCalc();
			for (Map<String, String> entry : BAT) {
				String name = entry.get("name");
				if (!name.equals("tv")) {
					chan = new Ams7fsChannelInfo();
					if (name.equals("Vbat"))
						name = "BAT";
					chan.setSzID(name);
					chan.setSzUnit(entry.get("unit"));
					chan.setnBits(32);
					chan.setRealConstant(Double.valueOf(entry.get("a0")));
					chan.setRealSlope(Double.valueOf(entry.get("a1")));
					chan.setDwDivider(1000);
					chan.setTickFile("TicksB");
					channelInfo.add(chan);
					subList.add(name);
				}
			}
		}
		recordedTicks.add("TicksB");
		recordedChannels.add(subList);
		subList = new ArrayList<String>();
		if (chanSets[2] != null) {
			List<Map<String, String>> pressure = chanSets[2].getCalc();
			int kP = 1, kT = 1;
			for (Map<String, String> entry : pressure) {
				String name = entry.get("name");
				if (name.equals("P_sc"))
					kP = Integer.valueOf(entry.get("kP"));
				else if (name.equals("T_sc"))
					kT = Integer.valueOf(entry.get("kT"));
				else if (!name.equals("tv")) {
					chan = new Ams7fsChannelInfo();
					chan.setSzUnit(entry.get("unit"));
					chan.setnBits(32);
					if (name.equals("P")) {
						name = "P_sc";
						chan.setRealSlope(1.0 / kP);
					}
					if (name.equals("T")) {
						name = "T_sc";
						chan.setRealSlope(1.0 / kT);
					}
					chan.setSzID(name);
					chan.setDwDivider(200);
					chan.setFormula(entry.get("formula"));
					Map<String, Double> c = new HashMap<String, Double>();
					for (String key : entry.keySet()) {
						if (key.startsWith("c")) {
							Double value = Double.valueOf(entry.get(key));
							c.put(key, value);
						}
					}
					chan.setConstants(c);
					chan.setTickFile("TicksD");
					channelInfo.add(chan);
					subList.add(name);
				}
			}
		}
		recordedTicks.add("TicksD");
		recordedChannels.add(subList);
		subList = new ArrayList<String>();
		if (chanSets[3] != null) {
			List<Map<String, String>> magneto = chanSets[3].getCalc();
			for (Map<String, String> entry : magneto) {
				String name = entry.get("name");
				if (!name.equals("tv")) {
					chan = new Ams7fsChannelInfo();
					chan.setSzID(name);
					chan.setDwDivider(20);
					chan.setSzUnit(entry.get("unit"));
					chan.setnBits(16);
					if (entry.containsKey("a0"))
						chan.setRealConstant(Double.valueOf(entry.get("a0")));
					chan.setRealSlope(Double.valueOf(entry.get("a1")));
					chan.setTickFile("TicksG");
					channelInfo.add(chan);
					subList.add(name);
				}
			}
		}
		recordedTicks.add("TicksG");
		recordedChannels.add(subList);
		subList = new ArrayList<String>();
		if (chanSets[4] != null) {
			List<Map<String, String>> mot = chanSets[4].getCalc();
			for (Map<String, String> entry : mot) {
				String name = entry.get("name");
				if (!name.equals("tv")) {
					chan = new Ams7fsChannelInfo();
					if (name.equals("AccelX"))
						name = "MXR";
					if (name.equals("AccelY"))
						name = "MYR";
					if (name.equals("AccelZ"))
						name = "MZR";
					chan.setSzID(name);
					chan.setSzUnit(entry.get("unit"));
					chan.setnBits(16);
					if (entry.containsKey("a0"))
						chan.setRealConstant(Double.valueOf(entry.get("a0")));
					chan.setRealSlope(Double.valueOf(entry.get("a1")));
					chan.setTickFile("TicksM");
					channelInfo.add(chan);
					subList.add(name);
				}
			}
		}
		recordedTicks.add("TicksM");
		recordedChannels.add(subList);
		/*
		 * recordedChannels = new ArrayList<String>();
		 * for (Ams7fsChannelInfo c : channelInfo)
		 * recordedChannels.add(c.getSzID());
		 */
		return read;
	}

	private void ReadAllPacketsBuffered(File tempDir, BufferedInputStream fis, long fileLength,
			long bytesRead)
			throws IOException {

		events = new ArrayList<Ams7fsPacket>();
		statics = new ArrayList<Ams7fsPacket>();
		starts = new ArrayList<Ams7fsPacket>();
		summaries = new ArrayList<Ams7fsPacket>();
		settings = new ArrayList<Ams7fsPacket>();
		others = new ArrayList<Ams7fsPacket>();

		// offsets = new ArrayList<ArrayList<Ams7fsPacket>>();
		int noOfChannels = 0;
		for (int i = 0; i < recordedChannels.size(); i++)
			noOfChannels += recordedChannels.get(i).size();
		skippedBytes = 0;
		nWritten = new int[noOfChannels];
		for (int i = 0; i < nWritten.length; i++)
			nWritten[i] = 0;
		nWrittenTicks = new int[noOfChannels];
		for (int i = 0; i < nWrittenTicks.length; i++)
			nWrittenTicks[i] = 0;

		nUSTime = new long[noOfChannels];
		for (int i = 0; i < nUSTime.length; i++)
			nUSTime[i] = 0;

		int i3 = 0;
		for (List<String> list : recordedChannels) {
			for (String s : list) {
				File f = new File(tempDir, s + ".bin");
				outFiles.add(f);
				foss.add(new FileOutputStream(f));
				fcout.add(foss.get(i3).getChannel());
				// offsets.add(new ArrayList<Ams7fsPacket>());
				ByteBuffer b = ByteBuffer.allocateDirect(10485760);
				bbs.add(b);
				lbs.add(b.asIntBuffer());
				i3++;
			}
		}
		int i2 = 0;
		for (String s : recordedTicks) {
			fossT.add(new FileOutputStream(new File(tempDir, s + ".bin")));
			fcoutT.add(fossT.get(i2).getChannel());
			ByteBuffer b = ByteBuffer.allocateDirect(10485760);
			bbt.add(b);
			lbt.add(b.asIntBuffer());
			i2++;
		}

		Ams7fsPacket tempPacket;
		JFrame frame = MainFrame.getInstance().getMainFrame();
		int fileNo = 0;
		try {
			while (fileNo < mergeFiles.size()) {
				fileNo++;
				ProgressMonitor progress = new ProgressMonitor(frame,
						"Reading 7fs file " + fileNo + " / " + mergeFiles.size(), null, 0, (int) len);
				while (bytesRead < len) { // (inBuffer.position() < inBuffer.capacity() && chunk < chunks) {
					long[] readList = new long[] { bytesRead };
					tempPacket = ReadNextPacketBuffered(fis, readList);
					if (bytesRead == readList[0]) // read error
						break;
					bytesRead = readList[0];
					progress.setProgress((int) bytesRead);
					if (tempPacket != null)
						HandlePacket(tempPacket);
					// if (skippedBytes > 10000) // assume incorrectly closed file
					// break;
					// if (Math.abs(totalDiffA) > 1000000) // assume incorrectly closed file with
					// old data
					// break
					if (endOfFile)
						break;
				}
				progress.close();
				if (fileNo >= mergeFiles.size())
					break;
				// fc.close();
				fis.close();
				filePath = mergeFiles.get(fileNo).getPath();
				fis = new BufferedInputStream(new FileInputStream(filePath), 10485760);
				// fc = fis.getChannel();
				len += (new File(filePath).length());
				endOfFile = false;
				startOfFile = true;
				Ams7fsPacket event = new Ams7fsPacket();
				event.setDwClockTick_ms((long) prevTimeA);
				event.setlType(200);
				event.setlCode(9999);
				event.setSzMessage("Real EOF");
				events.add(event);
			}
		} catch (java.nio.BufferUnderflowException e) {
			System.out.println(e.getMessage());
			// Catch exception to allow 'repairing' broken 5FS files
		}
		for (int i = 0; i < nWritten.length; i++) {
			if (nWritten[i] > 0) {
				bbs.get(i).clear();
				bbs.get(i).limit(nWritten[i] * 4);
				fcout.get(i).write(bbs.get(i));
				// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
				nWritten[i] = 0;
			}
		}
		for (int i = 0; i < nWrittenTicks.length; i++) {
			if (nWrittenTicks[i] > 0) {
				bbt.get(i).clear();
				bbt.get(i).limit(nWrittenTicks[i] * 4);
				fcoutT.get(i).write(bbt.get(i));
				// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
				nWrittenTicks[i] = 0;
			}
		}
		if (skippedBytes > 0)
			System.out.println("Skipped " + skippedBytes + " bytes due to crc errors!");
		for (FileChannel f : fcout)
			f.close();
		for (FileOutputStream f : foss)
			f.close();
		for (FileChannel f : fcoutT)
			f.close();
		for (FileOutputStream f : fossT)
			f.close();
		File tempDir2 = new File(tempDir, "tmp");
		tempDir2.mkdirs();
		for (File f : outFiles) {
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(f, tempDir2);
			ssf1.start();
		}
		// for(Map.Entry<String, Long> entry : sampleCounts.entrySet())
		// System.out.print(entry.getKey() + " " + entry.getValue() + " ");
		// System.out.println();
	}

	public Ams7fsPacket ReadNextPacketBuffered(BufferedInputStream fis, long[] readList) throws IOException {
		List<Character> tags = Arrays.asList('B', 'A', 'D', 'M', 'I', 'G', 'C', 'H');
		// byte b[] = getBytes(buf, 4); /*new byte[4];
		// inBuffer.get(b);*/
		// bb4.position(0);
		int bytesRead = fis.read(b4); // fc.read(bb4);
		if (bytesRead <= 0)
			return null;
		readList[0] += bytesRead;
		ubb.setBytes(b4);
		int wTag = ubb.getByte();
		int bytes2 = ubb.getShort();
		int wSize = bytes2 & 0x0FFF;
		// int type = bytes2 & 0xF000; //0x8000 is json; 0 is data
		while ((!tags.contains((char) wTag) || wSize == 0 || wSize % 4 != 0) && readList[0] < len) {
			// b = getBytes(buf, 4);
			// bb4.position(0);
			bytesRead = fis.read(b4); // fc.read(bb4);
			if (bytesRead <= 0)
				break;
			readList[0] += bytesRead;
			if (!(wTag == 0 && wSize == 0)) // don't count zero bytes behind header
				skippedBytes += 4;
			// inBuffer.get(b);
			ubb.setBytes(b4);
			wTag = ubb.getByte();
			wSize = ubb.getByte();
		}
		// ---- To avoid negative size array exception - Mainly while opening zero kb
		// files----
		if ((wSize - 4) <= 0) {
			return null;
		}
		// ----------------------------------------------
		byte block[] = new byte[wSize];
		for (int i = 0; i < 4; i++)
			block[i] = b4[i];
		// byte b2[] = getBytes(buf, wSize - 4); /*new byte[wSize - 4];
		// inBuffer.get(b2);*/
		byte b2[] = new byte[wSize - 4];
		// ByteBuffer bb2 = ByteBuffer.wrap(b2);
		bytesRead = fis.read(b2); // fc.read(bb2);
		if (bytesRead <= 0)
			return null;
		readList[0] += bytesRead;
		fis.mark(0);
		ubb.setBytes(b2);
		for (int i = 4; i < wSize; i++)
			block[i] = b2[i - 4];
		// if ((char) wTag != 'A' && (char) wTag != 'M')
		// System.out.println(wSize);
		// if (prevTime > 1155540)
		// System.out.println(wSize);
		if (crc8Check(block) != 0) {
			// long pos = fc.position();
			logger.error("crc failed " + (char) wTag + " " + wSize + " " + readList[0]); // + " " + chunk);
			// inBuffer.position(inBuffer.position() - wSize + 4);
			fis.reset();
			fis.read(b4);
			// fc.position(pos + 4);
			skippedBytes += 4;
			return null;
		}
		// String wTagS = String.valueOf((char) wTag);
		// if (sampleCounts.containsKey(wTagS)) {
		// Long count = sampleCounts.get(wTagS);
		// sampleCounts.put(wTagS, count + 1L);
		// } else {
		// sampleCounts.put(wTagS, 0L);
		// }
		return new Ams7fsPacket(ubb, wTag, wSize);
	}

	// byte[] getBytes(byte[][] buf, int size) {
	// byte ret[] = new byte[size];
	// int r = inBuffer.remaining();
	// if (r > size)
	// inBuffer.get(ret);
	// else {
	// for (int i = 0; i < r; i++)
	// ret[i] = inBuffer.get();
	// chunk++;
	// if (chunk >= chunks) //prevent crash when filesize roughly equals a multiple
	// of chunksize
	// return ret;
	// inBuffer = ByteBuffer.wrap(buf[chunk]);
	// for (int i = r; i < ret.length; i++)
	// ret[i] = inBuffer.get();
	// }
	// return ret;
	// }
}
