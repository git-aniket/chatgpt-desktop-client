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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.device.AmsDeviceConstants;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.SCLValueConvertor;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
import nl.vu.psy.ams.suite.tools.Utils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 * Class that opens a 5fs file, converts the contents
 * to the VU-DAMS format and copies the result to a
 * unique temporary directory. 
 */

public class Ams5fsFile extends Thread {

	private static Logger logger = LogManager.getLogger(Ams5fsFile.class.getName());

	private Ams5fsHeader fileHeader;
	private ArrayList<Ams5fsChannelInfo> channelInfo;
	private DataInputStream in;
	private ByteBuffer inBuffer;
	private Map<Integer, Integer> tagToIndex;
	private Map<Integer, String> tagToName;
	private ArrayList<Integer> recordedChannelTags;
	private ArrayList<Ams5fsPacket> events, statics, starts, summaries, settings, others;
	// private ArrayList<ArrayList<Ams5fsPacket>> offsets;
	private ArrayList<FileChannel> fcout = new ArrayList<FileChannel>();
	private ArrayList<FileOutputStream> foss = new ArrayList<FileOutputStream>();
	private String filePath;
	private int nStarts = 0;
	private File tempDir;
	private final UnsignedByteBuffer ubb = UnsignedByteBuffer.getInstance();
	private final int[] buffer = new int[256];
	private ArrayList<ByteBuffer> bbs = new ArrayList<ByteBuffer>();
	private ArrayList<IntBuffer> sbs = new ArrayList<IntBuffer>();
	private ArrayList<File> outFiles = new ArrayList<File>();

	private int[] nWritten;
	private int nZeros = 0;

	private Integer t;

	private int shift;

	private long[] nUSTime;

	private boolean quick = false;

	public Ams5fsFile() {

	}

	public Ams5fsFile(String fileName) {
		this.filePath = fileName;
	}

	private void addDZDTToChannelInfo(File tempDir) throws IOException {
		Ams5fsChannelInfo dzdtChannel = new Ams5fsChannelInfo();
		Ams5fsChannelInfo dzChannel = null;
		for (Ams5fsChannelInfo s : channelInfo)
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
		long factor = 1000000 / (2 * dzChannel.getDwDivider() * 1000);
		// ----------------------------------------------------------------------
		dzdtChannel.setlMinValue((int) (factor * dzChannel.getlMinValue()));
		dzdtChannel.setlMaxValue((int) (factor * dzChannel.getlMaxValue()));
		dzdtChannel.setlMinMaxDivider((dzChannel.getlMinMaxDivider()));
		dzdtChannel.setnBits(dzChannel.getnBits());
		dzdtChannel.setDwDivider(dzChannel.getDwDivider());
		channelInfo.add(dzdtChannel);
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
	}

	public ArrayList<Ams5fsChannelInfo> getChannelInfo() {
		return channelInfo;
	}

	public String getChannelNameFromTag(Integer i) {
		return tagToName.get(i);
	}

	public Ams5fsHeader getFileHeader() {
		return fileHeader;
	}

	public int getIndexFromTag(int tag) {
		return tagToIndex.get(tag);
	}

	public ArrayList<Integer> getRecordedChannelTags() {
		return recordedChannelTags;
	}

	private void HandlePacket(Ams5fsPacket packet) throws IOException {
		if (packet.getwTag() >= Ams5fsPacket.PACKET_TYPE_DATABLOCK) {
			int chanIndex = getRecordedChannelTags().indexOf(packet.getwTag());
			if (chanIndex == -1) {
				System.out.println(packet.getwTag());
				return;
				// prevent immediate crash on corrupted channel tags
			}
			long nBits = channelInfo.get(chanIndex).getnBits();
			int minVal = (int) Math.pow(2, nBits - 1);
			String chanName = getChannelNameFromTag(packet.getwTag());
			if (chanName.equals("MYA") || chanName.equals("MYD") || chanName.equals("MXA") || chanName.equals("MXD")) {
				// Unpack the motility channel
				long divider = channelInfo.get(chanIndex).getDwDivider();
				int[] dat = packet.getData();
				int n = dat.length;
				double value;
				for (int i = 0; i < n; i++) {
					t = dat[i];
					shift = t & 0x0F;
					t = t >> 4;
					t = t << shift;
					value = t / divider;
					buffer[i] = (int) (value - minVal);
				}
				IntBuffer test = sbs.get(chanIndex);
				test.put(buffer, 0, n);
				nWritten[chanIndex] += 4 * n;
				if (nWritten[chanIndex] > 10485760 - 513) {
					bbs.get(chanIndex).clear();
					bbs.get(chanIndex).limit(nWritten[chanIndex]);
					fcout.get(chanIndex).write(bbs.get(chanIndex));
					sbs.get(chanIndex).clear();
					// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
					nWritten[chanIndex] = 0;

				}
				nUSTime[chanIndex] += n * fileHeader.getDwSampleTime_us() * channelInfo.get(chanIndex).getDwDivider();
			} else {
				int[] dat = packet.getData();
				int n = dat.length;
				if (chanName.equals("SCL") == false) {
					for (int i = 0; i < n; i++)
						buffer[i] = (int) (dat[i] - minVal);
				} else {
					for (int i = 0; i < n; i++)
						buffer[i] = SCLValueConvertor.convertValue((int) (dat[i] - minVal));
				}
				IntBuffer test = sbs.get(chanIndex);
				test.put(buffer, 0, n);
				nWritten[chanIndex] += 4 * n;
				if (nWritten[chanIndex] > 10485760 - 513) {
					bbs.get(chanIndex).clear();
					bbs.get(chanIndex).limit(nWritten[chanIndex]);
					fcout.get(chanIndex).write(bbs.get(chanIndex));
					sbs.get(chanIndex).clear();
					// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
					nWritten[chanIndex] = 0;

				}
				nUSTime[chanIndex] += n * fileHeader.getDwSampleTime_us() * channelInfo.get(chanIndex).getDwDivider();
				/*
				 * for (Integer s : packet.getData()) { dataOut.writeInt(s -
				 * minVal); }
				 */
			}
		} else if (packet.getwTag() == Ams5fsPacket.PACKET_TYPE_EVENT) {
			events.add(packet);
		} else if (packet.getwTag() == Ams5fsPacket.PACKET_TYPE_STATICS) {
			statics.add(packet);
		} else if (packet.getwTag() == Ams5fsPacket.PACKET_TYPE_START) {
			starts.add(packet);
			// for (Integer i : getRecordedChannelTags()) {
			// int index = getRecordedChannelTags().indexOf(i);
			// int curPos = (int) fcout.get(index).size();
			// Ams5fsPacket tempPacket = new Ams5fsPacket();
			// tempPacket.setOffset(curPos);
			// offsets.get(index).add(tempPacket);
			// }
			nStarts++;
			if (nStarts > 1) {
				long startTime = starts.get(0).getDwClockTick_ms();
				long curTime = packet.getDwClockTick_ms();
				long diffTime = (curTime - startTime) * 1000;
				for (int i = 0; i < nUSTime.length; i++) {
					// Clear buffers
					if (nWritten[i] > 0) {
						bbs.get(i).clear();
						bbs.get(i).limit(nWritten[i]);
						fcout.get(i).write(bbs.get(i));
						sbs.get(i).clear();
						// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
						nWritten[i] = 0;
					}
					long timeToBeWritten = diffTime - nUSTime[i];
					if (timeToBeWritten > 0) {
						int nShortsToBeWritten = (int) (timeToBeWritten
								/ (fileHeader.getDwSampleTime_us() * channelInfo.get(i).getDwDivider()));
						ByteBuffer bb = ByteBuffer.allocate(2 * nShortsToBeWritten);
						IntBuffer sb = bb.asIntBuffer();
						for (int j = 0; j < nShortsToBeWritten; j++)
							sb.put(Integer.MIN_VALUE);
						fcout.get(i).write(bb);
					}
				}
				// throw new
				// IOException("Merged 5fs files are not supported yet.");
			}
		} else if (packet.getwTag() == Ams5fsPacket.PACKET_TYPE_SUMMARY) {
			summaries.add(packet);
		} else if (packet.getwTag() == Ams5fsPacket.PACKET_TYPE_SETTINGS) {
			settings.add(packet);
			for (Ams5fsChannelInfo chan : channelInfo) {
				if (chan.getSzID().equals("SCL"))
					SCLValueConvertor.initialize(chan,
							(packet.getDwSettingsFlags() & AmsDeviceConstants.AMSII_FLAG_SCLAC) != 0);
			}
		} else if (packet.getwTag() == 0 && packet.getwSize() == 4) {
			nZeros++;
		} else {
			others.add(packet);
		}
	}

	public boolean isAtEndOfFile() throws IOException {
		return (in.available() == 0);
	}

	public void open(File fl) throws IOException {
		filePath = fl.toString();
		in = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
		ReadFileHeader();
		ReadChannelInfo();
	}

	public void OpenFile() throws IOException {
		Timer timerB = new Timer();
		timerB.start();
		int len = (int) (new File(filePath).length());
		if (len < 4) {
			throw new IOException("File is not a valid 5fs file");
		}
		ProgressMonitorInputStream fis = new ProgressMonitorInputStream(MainFrame.getInstance().getMainFrame(),
				"Opening 5fs file", new BufferedInputStream(
						new FileInputStream(filePath), 10485760));
		byte[] buf = new byte[len];
		fis.read(buf);
		inBuffer = ByteBuffer.wrap(buf);
		fis.close();

		tempDir = Utils.getUniqueTemporaryDirectory();
		ReadFileHeaderBuffered();
		if (fileHeader.getsFileID().equals("AMS2") == false)
			throw new IOException("File is not a valid 5fs file");
		fcout = new ArrayList<FileChannel>();
		foss = new ArrayList<FileOutputStream>();
		bbs = new ArrayList<ByteBuffer>();
		sbs = new ArrayList<IntBuffer>();
		outFiles = new ArrayList<File>();
		ReadChannelInfoBuffered();
		ReadAllPacketsBuffered(tempDir);
		timerB.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Opening 5fsFile buffered took (" + timerB.getTime() / 1000. + " sec)");

		addDZDTToChannelInfo(tempDir);
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

		// for (Integer i : getRecordedChannelTags()) {
		// int index = getRecordedChannelTags().indexOf(i);
		// writer = new PrintWriter(new BufferedWriter(
		// new FileWriter(tempDir + File.separator + getChannelNameFromTag(i) +
		// "offsets.json")));
		// writer.print(gson.toJson(offsets.get(index)));
		// writer.close();
		// }

	}

	@SuppressWarnings({ "unused" })
	private void ReadAllPackets(File tempDir) throws IOException {

		events = new ArrayList<Ams5fsPacket>();
		statics = new ArrayList<Ams5fsPacket>();
		starts = new ArrayList<Ams5fsPacket>();
		summaries = new ArrayList<Ams5fsPacket>();
		settings = new ArrayList<Ams5fsPacket>();
		others = new ArrayList<Ams5fsPacket>();

		// offsets = new ArrayList<ArrayList<Ams5fsPacket>>();

		nWritten = new int[getRecordedChannelTags().size()];
		for (int i = 0; i < nWritten.length; i++)
			nWritten[i] = 0;

		nUSTime = new long[getRecordedChannelTags().size()];
		for (int i = 0; i < nUSTime.length; i++)
			nUSTime[i] = 0;

		int j = 0;
		for (Integer i : getRecordedChannelTags()) {
			foss.add(new FileOutputStream(new File(tempDir, getChannelNameFromTag(i) + ".bin")));
			fcout.add(foss.get(j).getChannel());
			// offsets.add(new ArrayList<Ams5fsPacket>());
			ByteBuffer b = ByteBuffer.allocateDirect(10485760);
			bbs.add(b);
			sbs.add(b.asIntBuffer());
			j++;
		}

		Ams5fsPacket tempPacket;
		try {
			while (isAtEndOfFile() == false) {
				tempPacket = ReadNextPacket();
				HandlePacket(tempPacket);
			}
		} catch (IOException e) {
			// Catch exception to allow 'repairing' broken 5FS files
		}
		for (int i = 0; i < nWritten.length; i++) {
			if (nWritten[i] > 0) {
				bbs.get(i).clear();
				bbs.get(i).limit(nWritten[i]);
				fcout.get(i).write(bbs.get(i));
				// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
				nWritten[i] = 0;
			}
		}
		for (FileChannel f : fcout)
			f.close();
		for (FileOutputStream f : foss)
			f.close();
	}

	private void ReadChannelInfo() throws IOException {
		byte b[] = new byte[Ams5fsChannelInfo.SIZE];
		channelInfo = new ArrayList<Ams5fsChannelInfo>();
		String channels = null;
		boolean isECGlowsampled = false;
		boolean isDZlowsampled = false;
		for (int i = 0; i < fileHeader.getnChannels(); i++) {
			in.readFully(b);
			ubb.setBytes(b);
			Ams5fsChannelInfo chan = new Ams5fsChannelInfo(ubb);
			channelInfo.add(chan);
			if (chan.getSzID().equals("ECG") && chan.getDwDivider() != 1) {
				isECGlowsampled = true;
			}
			if (chan.getSzID().equals("DZ") && chan.getDwDivider() != 1) {
				isDZlowsampled = true;
			}
		}
		if (isECGlowsampled == true && isDZlowsampled == true) {
			channels = "ECG and DZ";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channels are not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (isECGlowsampled == true) {
			channels = "ECG";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channel is not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (isDZlowsampled == true) {
			channels = "DZ";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channel is not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		}

		tagToIndex = new HashMap<Integer, Integer>();
		tagToName = new HashMap<Integer, String>();
		recordedChannelTags = new ArrayList<Integer>();
		int i = 0;
		for (Ams5fsChannelInfo c : channelInfo) {
			recordedChannelTags.add(c.getDwChannelTag());
			tagToIndex.put(c.getDwChannelTag(), i);
			tagToName.put(c.getDwChannelTag(), c.getSzID());
			i++;
		}
	}

	private void ReadFileHeader() throws IOException {
		byte b[] = new byte[Ams5fsHeader.SIZE];
		in.readFully(b);
		ubb.setBytes(b);
		fileHeader = new Ams5fsHeader(ubb);
	}

	public Ams5fsPacket ReadNextPacket() throws IOException {
		byte b[] = new byte[4];
		in.readFully(b);
		ubb.setBytes(b);
		int wTag = ubb.getUShort();
		int wSize = ubb.getUShort();
		// ---- To avoid negative size array exception - Mainly while opening zero kb
		// files----
		if ((wSize - 4) < 0) {
			wSize = 4;
		}
		// ----------------------------------------------
		byte b2[] = new byte[wSize - 4];
		in.readFully(b2);
		ubb.setBytes(b2);
		return new Ams5fsPacket(ubb, wTag, wSize);
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

				GregorianCalendar startDate = starts.get(0).gettStamp().toGregorianCalendar();
				long startTimeInUS = 1000 * starts.get(0).getDwClockTick_ms();

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
					CurrentOpenData.getInstance().Open(tempDir, quick, false, 0, 16);
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
							"Error reading 5fs file...\n(" + e.getMessage()
									+ ")\n'0KB file' > Please see:\n www.vu-ams.nl/support/tutorials/troubleshooting",
							"File Error", JOptionPane.ERROR_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"Error reading 5fs file...\n(" + e.getMessage() + ")", "File Error",
							JOptionPane.ERROR_MESSAGE);
				}
				if (tempDir != null) {
					ExitApp.deleteDirectory(tempDir);
				}
			} catch (java.nio.BufferUnderflowException e) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
						"Error reading 5fs file...\n(" + e.getMessage() + ")", "File Size Error",
						JOptionPane.ERROR_MESSAGE);
			} finally {
				closeAllOpenFiles();
			}
			MainFrame.getInstance().getMainFrame().setEnabled(true);
		}
		logger.info("Stop open file");
	}

	public void setProgressDialog(boolean visible) {
	}

	public void setQuick(boolean quick) {
		this.quick = quick;
	}

	/// Direct buffer methods for performance comparison
	private void ReadFileHeaderBuffered() {
		byte b[] = new byte[Ams5fsHeader.SIZE];
		inBuffer.get(b);
		ubb.setBytes(b);
		fileHeader = new Ams5fsHeader(ubb);
	}

	private void ReadAllPacketsBuffered(File tempDir) throws IOException {

		events = new ArrayList<Ams5fsPacket>();
		statics = new ArrayList<Ams5fsPacket>();
		starts = new ArrayList<Ams5fsPacket>();
		summaries = new ArrayList<Ams5fsPacket>();
		settings = new ArrayList<Ams5fsPacket>();
		others = new ArrayList<Ams5fsPacket>();

		// offsets = new ArrayList<ArrayList<Ams5fsPacket>>();

		nWritten = new int[getRecordedChannelTags().size()];
		for (int i = 0; i < nWritten.length; i++)
			nWritten[i] = 0;

		nUSTime = new long[getRecordedChannelTags().size()];
		for (int i = 0; i < nUSTime.length; i++)
			nUSTime[i] = 0;

		int j = 0;
		for (Integer i : getRecordedChannelTags()) {
			File f = new File(tempDir, getChannelNameFromTag(i) + ".bin");
			outFiles.add(f);
			foss.add(new FileOutputStream(f));
			fcout.add(foss.get(j).getChannel());
			// offsets.add(new ArrayList<Ams5fsPacket>());
			ByteBuffer b = ByteBuffer.allocateDirect(10485760);
			bbs.add(b);
			sbs.add(b.asIntBuffer());
			j++;
		}

		Ams5fsPacket tempPacket;
		try {
			while (inBuffer.position() < inBuffer.capacity()) {
				tempPacket = ReadNextPacketBuffered();
				HandlePacket(tempPacket);
			}
		} catch (java.nio.BufferUnderflowException e) {
			System.out.println(e.getMessage());
			// Catch exception to allow 'repairing' broken 5FS files
		}
		if (nZeros > 0)
			System.out.println("Zero size packets found: " + nZeros);
		for (int i = 0; i < nWritten.length; i++) {
			if (nWritten[i] > 0) {
				bbs.get(i).clear();
				bbs.get(i).limit(nWritten[i]);
				fcout.get(i).write(bbs.get(i));
				// dataOut.write(bbs.get(chanIndex).array(),0,nWritten[chanIndex]);
				nWritten[i] = 0;
			}
		}
		for (FileChannel f : fcout)
			f.close();
		for (FileOutputStream f : foss)
			f.close();
		File tempDir2 = new File(tempDir, "tmp");
		tempDir2.mkdirs();
		for (File f : outFiles) {
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(f, tempDir2);
			ssf1.start();
		}
	}

	private void ReadChannelInfoBuffered() throws IOException {
		byte b[] = new byte[Ams5fsChannelInfo.SIZE];
		channelInfo = new ArrayList<Ams5fsChannelInfo>();
		String channels = null;
		boolean isECGlowsampled = false;
		boolean isDZlowsampled = false;
		for (int i = 0; i < fileHeader.getnChannels(); i++) {
			inBuffer.get(b);
			ubb.setBytes(b);
			Ams5fsChannelInfo chan = new Ams5fsChannelInfo(ubb);
			channelInfo.add(chan);
			if (chan.getSzID().equals("ECG") && chan.getDwDivider() != 1) {
				isECGlowsampled = true;
			}
			if (chan.getSzID().equals("DZ") && chan.getDwDivider() != 1) {
				isDZlowsampled = true;
			}
		}
		if (isECGlowsampled == true && isDZlowsampled == true) {
			channels = "ECG and DZ";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channels are not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (isECGlowsampled == true) {
			channels = "ECG";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channel is not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (isDZlowsampled == true) {
			channels = "DZ";
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					channels + " " + "channel is not recorded at 1000Hz.", "Channel Warning",
					JOptionPane.ERROR_MESSAGE);
		}

		tagToIndex = new HashMap<Integer, Integer>();
		tagToName = new HashMap<Integer, String>();
		recordedChannelTags = new ArrayList<Integer>();
		int i = 0;
		for (Ams5fsChannelInfo c : channelInfo) {
			recordedChannelTags.add(c.getDwChannelTag());
			tagToIndex.put(c.getDwChannelTag(), i);
			tagToName.put(c.getDwChannelTag(), c.getSzID());
			i++;
		}
	}

	public Ams5fsPacket ReadNextPacketBuffered() throws IOException {
		byte b[] = new byte[4];
		inBuffer.get(b);
		ubb.setBytes(b);
		int wTag = ubb.getUShort();
		int wSize = ubb.getUShort();
		// ---- To avoid negative size array exception - Mainly while opening zero kb
		// files----
		if ((wSize - 4) < 0) {
			wSize = 4;
		}
		// ----------------------------------------------
		byte b2[] = new byte[wSize - 4];
		inBuffer.get(b2);
		ubb.setBytes(b2);
		return new Ams5fsPacket(ubb, wTag, wSize);
	}

}
