package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
/*
 * Dialog that shows some quick information about 
 * an amsdata file, without opening it.
 */
public class QuickOpenDialog extends JDialog {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public QuickOpenDialog(File file) {
		super(MainFrame.getInstance().getMainFrame(), "Quick AMSDATA Info", true);
		if (!file.exists())
			return;
		int size = 1048576;
		File tempDir = Utils.getUniqueTemporaryDirectory();
		DataInputStream dis = null;
		BufferedInputStream is = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			//String header = new String();
			for (int i = 0; i < 7; i++)
				dis.readChar();
			dis.readShort();
			long staticFileLength = dis.readLong();
			dis.close();

			byte[] buffer = new byte[size];
			int nBytesRead;
			is = new BufferedInputStream(new FileInputStream(file));
			is.skip(staticFileLength);
			ZipInputStream zin = new ZipInputStream(is);
			ZipEntry entry;
			do {
				entry = zin.getNextEntry();
				if (entry != null) {
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, entry.getName())), 10 * size);
					while ((nBytesRead = zin.read(buffer, 0, size)) > -1) {
						out.write(buffer, 0, nBytesRead);
					}
					out.close();
					zin.closeEntry();
				}
			} while (entry != null);
			zin.close();

			JTabbedPane tabs = new JTabbedPane();

			String inString;
			File curFile;
			java.lang.reflect.Type collectionType;
			Gson gson = new Gson();
			LinkedList<Object> tempList;
			Ams7fsHeader fileHeader = null;

			curFile = new File(tempDir, "header.json");
			inString = Utils.readStringFromFile(curFile);
			if (inString != null) {
				fileHeader = gson.fromJson(inString, Ams7fsHeader.class);
			}

			ArrayList<Ams5fsChannelInfo> chans = new ArrayList<Ams5fsChannelInfo>();
			curFile = new File(tempDir, "channels.json");
			inString = Utils.readStringFromFile(curFile);
			if (inString != null) {
				collectionType = new TypeToken<LinkedList<Ams5fsChannelInfo>>() {
				}.getType();
				tempList = gson.fromJson(inString, collectionType);
				for (Object o : tempList) {
					chans.add((Ams5fsChannelInfo) o);
				}
			}

			collectionType = new TypeToken<LinkedList<Ams5fsPacket>>() {
			}.getType();

			ArrayList<Ams5fsPacket> events = new ArrayList<Ams5fsPacket>();
			curFile = new File(tempDir, "events.json");
			inString = Utils.readStringFromFile(curFile);
			if (inString != null) {
				tempList = gson.fromJson(inString, collectionType);
				for (Object o : tempList)
					events.add((Ams5fsPacket) o);
			}

			ArrayList<Ams5fsPacket> starts = new ArrayList<Ams5fsPacket>();
			curFile = new File(tempDir, "starts.json");
			inString = Utils.readStringFromFile(curFile);
			if (inString != null) {
				tempList = gson.fromJson(inString, collectionType);
				for (Object o : tempList)
					starts.add((Ams5fsPacket) o);
			}
			
			ArrayList<Ams5fsPacket> summaries = new ArrayList<Ams5fsPacket> ();
			curFile = new File(tempDir, "summaries.json");
			inString = Utils.readStringFromFile(curFile);
			if (inString != null) {
				tempList = gson.fromJson(inString, collectionType);
				for (Object o : tempList)
					summaries.add((Ams5fsPacket) o);
			}
			
			
			String text;
			if (fileHeader != null) {
				text = "File path\t\t\t: " + file.getParent() + "\n";
				text += "File name\t\t\t: " + file.getName() + "\n";
				if (starts.isEmpty() == false) {
					GregorianCalendar sd = starts.get(0).gettStamp().toGregorianCalendar();
					long startTime = 1000 * starts.get(0).getDwClockTick_ms();
					text += "Start time of recording\t\t: " + Utils.getDateAndTimeFromUS(startTime, sd, startTime) + "\n";
				}
				if (summaries.isEmpty() == false) {
					GregorianCalendar sd1 = summaries.get(0).gettStamp().toGregorianCalendar();
					long endtime = 1000 * summaries.get(0).getDwClockTick_ms();
					text += "End time of Recording\t\t: "+ Utils.getDateAndTimeFromUS(endtime, sd1, endtime)+ "\n";
				}
				text += "Number of recorded channels\t\t: " + chans.size() + "\n";
				text += "Subject ID\t\t\t: " + fileHeader.getSzSubjectID() + "\n";
				text += "Study ID\t\t\t: " + fileHeader.getStudyId() + "\n";
				text += "Session ID\t\t\t: " + fileHeader.getDwSession() + "\n";
				text += "Comment\t\t\t: " + fileHeader.getComment() + "\n";
				text += "Producer\t\t\t: " + fileHeader.getSzProducer() + "\n";
				text += "Device serial number\t\t: " + fileHeader.getDwSerialNumber() + "\n";
				text += "Device firmware version\t\t: " + fileHeader.getFirmwareVersion() + "\n";
				text += "Device hardware version\t\t: " + fileHeader.getDwHardwareVersion() + "\n";
				text += "Number of events\t\t: " + events.size() + "\n";
				JPanel pan = new JPanel(new BorderLayout());
				JTextArea ta = new JTextArea(text);
				ta.setEditable(false);
				JScrollPane sp = new JScrollPane(ta);
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				pan.add(sp, BorderLayout.CENTER);
				tabs.add(pan, "Recording Info");
			} else {
				tabs.add(new JPanel(), "Recording Info");
				tabs.setEnabledAt(tabs.getTabCount() - 1, false);
			}

			if (chans.isEmpty() == false && fileHeader != null) {
				text = "";
				long sr = fileHeader.getDwSampleTime_us();
				for (Ams5fsChannelInfo ci : chans) {
					String name = ci.getSzID();
					text += "Channel\t\t\t\t: " + name + "\n";
					long srC = ci.getDwDivider() * sr;
					text += "Sampling interval\t\t: " + (srC / 1000) + " [msec] (" + 1000000 / srC + " [Hz])\n";
					double minVal = (double) ci.getlMinValue() / ci.getlMinMaxDivider();
					double maxVal = (double) ci.getlMaxValue() / ci.getlMinMaxDivider();
					if (ci.getRealSlope() != 0) {
						long lowerBound = (long) -Math.pow(2, ci.getnBits() - 1);
						long upperBound = (long) Math.pow(2, ci.getnBits() - 1) - 1;
						minVal = lowerBound * ci.getRealSlope() + ci.getRealConstant();
						maxVal = upperBound * ci.getRealSlope() + ci.getRealConstant();
					}
					text += "Minimum physical value\t\t: " + minVal + " [" + ci.getSzUnit() + "]\n";
					text += "Maximum physical value\t\t: " + maxVal + " [" + ci.getSzUnit() + "]\n";
					text += "\n";
				}
				JPanel pan = new JPanel(new BorderLayout());
				JTextArea ta = new JTextArea(text);
				ta.setEditable(false);
				JScrollPane sp = new JScrollPane(ta);
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				pan.add(sp, BorderLayout.CENTER);
				tabs.add(pan, "Recorded Channels");
			} else {
				tabs.add(new JPanel(), "Recorded Channels");
				tabs.setEnabledAt(tabs.getTabCount() - 1, false);
			}

			if (events.isEmpty() == false) {
				text = "";
				for (Ams5fsPacket ev : events) {
					text += "Time\t: " + ev.getDwClockTick_ms() + " [msec]\n";
					if (starts.isEmpty() == false) {
						GregorianCalendar sd = starts.get(0).gettStamp().toGregorianCalendar();
						long startTime = 1000 * starts.get(0).getDwClockTick_ms();
						text += "Time\t: " + Utils.getDateAndTimeFromUS(ev.getDwClockTick_ms() * 1000., sd, startTime) + "\n";
					}
					text += "Type\t: " + ev.getlType() + "\n";
					text += "Code\t: " + ev.getlCode() + "\n";
					text += "Message\t: " + ev.getSzMessage() + "\n";
					text += "\n";
				}
				JPanel pan = new JPanel(new BorderLayout());
				JTextArea ta = new JTextArea(text);
				ta.setEditable(false);
				JScrollPane sp = new JScrollPane(ta);
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				pan.add(sp, BorderLayout.CENTER);
				tabs.add(pan, "Recorded Events");
			} else {
				tabs.add(new JPanel(), "Recorded Events");
				tabs.setEnabledAt(tabs.getTabCount() - 1, false);
			}

			String outputDataString = Utils.readStringFromFile(new File(tempDir, "outputdata.txt"));
			if (outputDataString != null) {
				JPanel pan = new JPanel(new BorderLayout());
				JTextArea ta = new JTextArea(outputDataString);
				ta.setEditable(false);
				JScrollPane sp = new JScrollPane(ta);
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				pan.add(sp, BorderLayout.CENTER);
				tabs.add(pan, "Output Data");
			} else {
				tabs.add(new JPanel(), "Output Data");
				tabs.setEnabledAt(tabs.getTabCount() - 1, false);
			}

			add(tabs);
			Toolkit toolkit = Toolkit.getDefaultToolkit();

			Dimension scrnsize = toolkit.getScreenSize();
			Dimension frameSize = new Dimension(1280, 720);
			frameSize.width = (int) (2 * scrnsize.getWidth() / 3);
			frameSize.height = (int) (2 * scrnsize.getHeight() / 3);
			setBounds(0, 0, frameSize.width, frameSize.height);
			setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

			ExitApp.deleteDirectory(tempDir);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
