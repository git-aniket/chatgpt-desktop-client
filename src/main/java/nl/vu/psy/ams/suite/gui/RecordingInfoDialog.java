package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.tools.Utils;

/**
 * Shows information about an already opened .amsdata file, like recorded
 * channels and events.
 */
public class RecordingInfoDialog extends JDialog implements ClipboardOwner {

	private static final long serialVersionUID = 1L;
	private String text;

	public RecordingInfoDialog() {
		super(MainFrame.getInstance().getMainFrame(), "Recording Info", true);
		setLayout(new BorderLayout());
		final CurrentOpenData cod = CurrentOpenData.getInstance();
		Ams7fsHeader header = cod.getFileHeader();
		ArrayList<Ams7fsChannelInfo> chans = cod.getChannelInfo();
		File df = cod.getDataFile();
		text = "";

		text += "File path\t\t\t: " + df.getParent() + "\n";
		text += "File name\t\t\t: " + df.getName() + "\n";
		text += "Start time of Recording\t\t: " + CurrentOpenData.getInstance().getStartDate().getTime() + "\n";
		text += "End time of Recording\t\t: " + CurrentOpenData.getInstance().getEndDate().getTime() + "\n";
		text += "Number of recorded channels\t\t: " + chans.size() + "\n";
		text += "Subject ID\t\t\t: " + header.getSzSubjectID() + "\n";
		text += "Study ID\t\t\t: " + header.getStudyId() + "\n";
		text += "Session ID\t\t\t: " + header.getDwSession() + "\n";
		text += "Comment\t\t\t: " + header.getComment() + "\n";
		text += "Device serial number\t\t: " + header.getDwSerialNumber() + "\n";
		text += "Device firmware version\t\t: " + header.getFirmwareVersion() + "\n";
		text += "Device hardware version\t\t: " + header.getDwHardwareVersion() + "\n";
		text += "Number of events\t\t: " + cod.getEvents().size() + "\n";

		if (new File(System.getProperty("user.dir"), "DeveloperMode.txt").exists()) {
			text += "\n### Developer Mode: ###\n";
			text += "Producer\t\t\t: " + header.getSzProducer() + "\n";
			long ms = 0;
			if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
					|| CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() == 7)
				ms = new File(cod.getFilePath(), "ECG.bin").length() / 4;// Ugly but just assume ECG.bin exists
			else
				ms = new File(cod.getFilePath(), "ECG.bin").length() / 2;// Ugly but just assume ECG.bin exists
			// Do not use NumberOfScansInBlock from summary for calculating this difference
			// because scan count is stored by the firmware and is unaffected by 5fs file
			// editing.
			text += "RTC(ms) - ECGsampleCount\t\t: "
					+ (CurrentOpenData.getInstance().getEndDate().getTime().getTime()
							- CurrentOpenData.getInstance().getStartDate().getTime().getTime() - ms)
					+ " milliseconds\n";

			for (Ams5fsPacket st : cod.getStarts()) {
				text += "StartReason\t\t\t: " + st.getcFileStartReasonString() + " " + st.gettStamp() + "\n";
			}
			for (Ams5fsPacket su : cod.getSummaries()) {
				text += "StopReason\t\t\t: " + su.getwFileStopReasonString() + "\n";
			}

			if (CurrentOpenData.getInstance().getSettings().size() > 0) {
				text += "SettingsFlags:\t\t\t: 0x"
						+ Long.toHexString(CurrentOpenData.getInstance().getSettings().get(0).getDwSettingsFlags())
						+ "\n";
				text += CurrentOpenData.getInstance().getSettings().get(0).getDwSettingsFlagsStringUnpacked();
			}
			text += "### Developer Mode ###";
		}

		text += "\n\nChannels:\n";
		long sr = header.getDwSampleTime_us();
		for (Ams7fsChannelInfo ci : chans) {
			String name = ci.getSzID();
			long fs = new File(cod.getFilePath(), name + ".bin").length();
			text += "Channel\t\t\t\t: " + name + "\n";
			text += "Number of samples\t\t: " + fs / 2 + "\n";
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
			text += "File size\t\t\t: " + fs + " [bytes]\n";
			text += "\n";
		}
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		add(new JScrollPane(area), BorderLayout.CENTER);
		// pack();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension scrnsize = toolkit.getScreenSize();
		setPreferredSize(new Dimension(2 * scrnsize.width / 3, 2 * scrnsize.height / 3));
		setMinimumSize(new Dimension(2 * scrnsize.width / 3, 2 * scrnsize.height / 3));

		JPanel butPan = new JPanel();

		JButton copyButton = new JButton("Copy to clipboard");
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection ss = new StringSelection(text);
				cb.setContents(ss, RecordingInfoDialog.this);
			}
		});
		butPan.add(copyButton);

		JButton eventsButton = new JButton("Show events");
		eventsButton.addActionListener(new ActionListener() {
			private String eT;

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(RecordingInfoDialog.this, "Recorded Events", true);
				diag.setLayout(new BorderLayout());
				eT = "";
				for (Ams5fsPacket ev : cod.getEvents()) {
					// -------------- To display only software events/hide hardware events such as
					// buffer overflow--------------
					if ((ev.getlType() == 0) || (ev.getlType() == 1) || (ev.getlType() == 2)
							|| (ev.getlType() == 100)) {
						// -------------- Event time is now synced with export signal to ascii time.
						// However, if the signal is not measured at
						// 1000 Hz, there are small differences of milli seconds between event time and
						// sampling time-----
						eT += "Time\t: "
								+ (ev.getDwClockTick_ms() - (CurrentOpenData.getInstance().getStartTimeInUS() / 1000))
								+ " [msec]\n";
						// ----------------------------------------------------------------------------------------------------
						eT += "Time\t: " + Utils.getDateAndTimeFromUS(ev.getDwClockTick_ms() * 1000.) + "\n";
						eT += "Type\t: " + ev.getlType() + "\n";
						eT += "Code\t: " + ev.getlCode() + "\n";
						eT += "Message\t: " + ev.getSzMessage() + "\n";
						eT += "\n";
					}
					// -----------------------------------------------------------------------------------------------------------
				}
				JTextArea eVarea = new JTextArea(eT);
				eVarea.setEditable(false);
				diag.add(new JScrollPane(eVarea), BorderLayout.CENTER);
				JPanel butPan = new JPanel();

				JButton copyButton = new JButton("Copy to clipboard");
				copyButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection ss = new StringSelection(eT);
						cb.setContents(ss, RecordingInfoDialog.this);
					}
				});

				JButton closeButton = new JButton("Close");
				closeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});

				JButton technicaleventsButton = new JButton("Show Technical Events");
				technicaleventsButton.addActionListener(new ActionListener() {
					private String eT;

					@Override
					public void actionPerformed(ActionEvent e) {
						final JDialog diag = new JDialog(RecordingInfoDialog.this, "Recorded Technical Events", true);
						diag.setLayout(new BorderLayout());
						eT = "";
						for (Ams5fsPacket ev : cod.getEvents()) {

							if (!(ev.getlType() == 0) || (ev.getlType() == 1) || (ev.getlType() == 2)
									|| (ev.getlType() == 100)) {
								eT += "Time\t: "
										+ (ev.getDwClockTick_ms()
												- (CurrentOpenData.getInstance().getStartTimeInUS() / 1000))
										+ " [msec]\n";
								// ----------------------------------------------------------------------------------------------------
								eT += "Time\t: " + Utils.getDateAndTimeFromUS(ev.getDwClockTick_ms() * 1000.) + "\n";
								eT += "Type\t: " + ev.getlType() + "\n";
								eT += "Code\t: " + ev.getlCode() + "\n";
								eT += "Message\t: " + ev.getSzMessage() + "\n";
								eT += "\n";
							}
							// -----------------------------------------------------------------------------------------------------------
						}
						JTextArea eVarea = new JTextArea(eT);
						eVarea.setEditable(false);
						diag.add(new JScrollPane(eVarea), BorderLayout.CENTER);

						JPanel butPan = new JPanel();

						JButton copyButton = new JButton("Copy to clipboard");
						copyButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
								StringSelection ss = new StringSelection(eT);
								cb.setContents(ss, RecordingInfoDialog.this);
							}
						});

						JButton closeButton = new JButton("Close");
						closeButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								diag.setVisible(false);
							}
						});
						butPan.add(copyButton);
						butPan.add(closeButton);
						diag.add(butPan, BorderLayout.SOUTH);

						Toolkit toolkit = Toolkit.getDefaultToolkit();
						Dimension scrnsize = toolkit.getScreenSize();
						diag.setPreferredSize(new Dimension(1 * scrnsize.width / 5, 2 * scrnsize.height / 5));
						diag.setMinimumSize(new Dimension(1 * scrnsize.width / 5, 2 * scrnsize.height / 5));

						diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

						diag.setVisible(true);
					}
				});
				butPan.add(copyButton);
				butPan.add(technicaleventsButton);
				butPan.add(closeButton);
				diag.add(butPan, BorderLayout.SOUTH);

				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Dimension scrnsize = toolkit.getScreenSize();
				diag.setPreferredSize(new Dimension(1 * scrnsize.width / 3, 2 * scrnsize.height / 3));
				diag.setMinimumSize(new Dimension(1 * scrnsize.width / 3, 2 * scrnsize.height / 3));

				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

				diag.setVisible(true);

			}
		});
		butPan.add(eventsButton);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		butPan.add(closeButton);

		add(butPan, BorderLayout.SOUTH);

		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {

	}
}
