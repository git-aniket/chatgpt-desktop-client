package nl.vu.psy.ams.suite.device;

/*import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;*/
import com.fazecast.jSerialComm.SerialPort;
//import com.fazecast.jSerialComm.SerialPortDataListener;
//import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
//import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.JLabelAntialiased;
import nl.vu.psy.ams.suite.tools.StoppableRunnable;

import com.intel.bluetooth.BlueCoveImpl;

/*
 * Class that provides communication with an AMS device.
 * Can use both serial cable and bluetooth, since both
 * result in an inputstream and outputstream for communication.
 * sendPacket and recievePacket are the basic methods for communicating,
 * but helper functions exist, such as getParameterFromDevice of sendSettings.
 * Packets are send and recieved using a thread, in order to detect timeouts
 * (bluetooth can block for up to a minute, so we should put it in a thread to
 * remain responsive). 
 */
public class AmsDevice {

	private class RecieveRunnable implements Runnable {

		private int[] recvPacket = null;
		private StoppableRunnable timerRun;
		private String OSname = System.getProperty("os.name");

		public RecieveRunnable(StoppableRunnable run) {
			this.timerRun = run;
		}

		public synchronized int[] getRecvPacket() {
			if (recvPacket == null)
				return null;
			int[] copy = new int[recvPacket.length];
			System.arraycopy(recvPacket, 0, copy, 0, recvPacket.length);
			return copy;
		}

		@Override
		public void run() {
			setBusyCommunicating(true);
			try {
				int size1, size2;
				size1 = in.read();
				if (size1 == -1) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				size2 = in.read();
				int pktsize = (size2 << 8) + size1;
				if (pktsize < 7) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				int tries = 0;
				int max_size = 0;
				if (OSname.contains("Mac")) {
					max_size = 0;
				} else {
					max_size = pktsize - 2;
				}
				// while (in.available() < pktsize - 2) { // For Windows
				while (in.available() < max_size) { // For Mac
					tries++;
					if (tries >= 100) {
						setBusyCommunicating(false);
						timerRun.stopThread();
						return;
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						setBusyCommunicating(false);
						timerRun.stopThread();
						return;
					}
				}
				int[] pkt = new int[pktsize - 6];
				for (int i = 0; i < pkt.length; i++) {
					pkt[i] = in.read();
				}
				int crcdat[] = new int[4];
				for (int i = 0; i < 4; i++)
					crcdat[i] = in.read();
				CRC32 crc = new CRC32();
				crc.update(size1);
				crc.update(size2);
				for (int i = 0; i < pkt.length; i++)
					crc.update(pkt[i]);
				long crcval = crc.getValue();
				if (crcdat[0] != (int) (crcval & 0x00000000000000FF)) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				if (crcdat[1] != (int) ((crcval & 0x000000000000FF00) >> 8)) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				if (crcdat[2] != (int) ((crcval & 0x0000000000FF0000) >> 16)) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				if (crcdat[3] != (int) ((crcval & 0x00000000FF000000) >> 24)) {
					setBusyCommunicating(false);
					timerRun.stopThread();
					return;
				}
				setRecvPacket(pkt);
				setBusyCommunicating(false);
				timerRun.stopThread();
				return;
			} catch (com.fazecast.jSerialComm.SerialPortIOException e1) {
				// e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				setBusyCommunicating(false);
				timerRun.stopThread();
			}
			return;
		}

		public synchronized void setRecvPacket(int[] recvPacket) {
			this.recvPacket = recvPacket;
		}
	}

	private class SendPacketRunnable implements Runnable {

		private int[] pkt;
		private StoppableRunnable timerRun;

		public SendPacketRunnable(int[] pkt, StoppableRunnable timerRun) {
			this.pkt = pkt;
			this.timerRun = timerRun;
		}

		@Override
		public void run() {
			setBusyCommunicating(true);
			int crcdat[] = new int[4];
			CRC32 crc = new CRC32();
			for (int i = 0; i < pkt.length; i++)
				crc.update(pkt[i]);
			long crcval = crc.getValue();
			crcdat[0] = (int) (crcval & 0x00000000000000FF);
			crcdat[1] = (int) ((crcval & 0x000000000000FF00) >> 8);
			crcdat[2] = (int) ((crcval & 0x0000000000FF0000) >> 16);
			crcdat[3] = (int) ((crcval & 0x00000000FF000000) >> 24);
			byte data[] = new byte[pkt.length + 4];
			for (int i = 0; i < pkt.length; i++) {
				if (pkt[i] >= 128) {
					data[i] = (byte) (pkt[i] - 256);
				} else {
					data[i] = (byte) pkt[i];
				}
			}
			for (int i = 0; i < 4; i++) {
				if (crcdat[i] >= 128) {
					data[data.length - 4 + i] = (byte) (crcdat[i] - 256);
				} else {
					data[data.length - 4 + i] = (byte) crcdat[i];
				}
			}
			try {
				out.write(data);
			} catch (com.fazecast.jSerialComm.SerialPortIOException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				setBusyCommunicating(false);
				timerRun.stopThread();
			}
		}

	}

	SerialPort[] commPorts = SerialPort.getCommPorts();
	ArrayList<Integer> commPortsStates = new ArrayList<Integer>();
	public InputStream in;
	public OutputStream out;

	private SerialPort sPort;
	boolean isConnected = false;
	private AmsSettings settings;

	private AmsStatics statics = new AmsStatics();

	StreamConnection conn;
	public static final int PAR_DEVICEID = 200;

	int curID;
	boolean serialConn = false;
	public String blueToothAddress = "";

	private boolean isBusyCommunicating = false;

	private boolean isConnectionOpen = false;

	public AmsDevice() {
		settings = new AmsSettings();
	}

	public void closeConnection() {
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (getsPort() != null) {
			getsPort().closePort();
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void connectToBlueTooth() {
		BlueToothConnectionDialog diag = new BlueToothConnectionDialog(this);
		diag.setVisible(true);

	}

	public void connectToSerialPort() {
		setAvailableSerialPorts();
		if (commPorts.length == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No COM port found. Is the AMSi USB cable connected to this computer? \nIs the AMSi driver installed? (www.vu-ams.nl/support/downloads/software)",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String text = "<html><body>Available Serial Ports:<br><table>";
		for (int i = 0; i < commPorts.length; i++) {
			SerialPort p = commPorts[i];
			text += "<tr><td>" + p.getSystemPortName() + ":</td>";
			int state = commPortsStates.get(i);
			if (state == 0) {
				text += "<td><font color=green>Available</font></td></tr>";
			} else if (state == 1) {
				text += "<td><font color=yellow>In Use</font></td></tr>";
			} else {
				text += "<td><font color=red>Connection Error</font></td></tr>";
			}
		}
		text += "</table></body></html>";
		final JLabelAntialiased label = new JLabelAntialiased(text);
		final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Ams Device Connection", true);
		diag.setLayout(new BorderLayout());
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		diag.add(label, BorderLayout.NORTH);

		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		// closeButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

		JPanel pan2 = new JPanel(new BorderLayout());
		pan2.add(new JSeparator(), BorderLayout.CENTER);

		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
		pan.add(Box.createHorizontalGlue());
		pan.add(closeButton);
		pan.add(Box.createHorizontalGlue());
		pan2.add(pan, BorderLayout.SOUTH);
		diag.add(pan2, BorderLayout.SOUTH);
		diag.pack();
		diag.setBounds(0, 0, 500, 300);
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
		final Thread connThread = new Thread() {

			@Override
			public void run() {
				boolean stop = false;
				while (stop == false) {
					for (int i = 0; i < commPorts.length; i++) {
						String text = "<html><body>Available Serial Ports:<br><table>";
						for (int j = 0; j < commPorts.length; j++) {
							SerialPort p = commPorts[j];
							text += "<tr><td>" + p.getSystemPortName() + ":</td>";
							int state = commPortsStates.get(j);
							if (i == j) {
								text += "<td>Connecting...</td></tr>";
							} else if (state == 0) {
								text += "<td><font color=green>Available</font></td></tr>";
							} else if (state == 1) {
								text += "<td><font color=yellow>In Use</font></td></tr>";
							} else {
								text += "<td><font color=red>Connection Error</font></td></tr>";
							}
						}
						text += "</table><br><br><br>";// end table and add 3 empty lines
						text += "If this window doesn't disappear; please download and install <b>AMSi driver</b> from www.vu-ams.nl";
						text += "</body></html>";
						label.setText(text);
						label.repaint();
						if (commPortsStates.get(i) == 0) {
							try {
								SerialPort port = commPorts[i];
								port.openPort();
								port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
								setsPort(port);
								getsPort().setComPortParameters(AmsDeviceConstants.BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
										SerialPort.NO_PARITY);
								in = getsPort().getInputStream();
								out = getsPort().getOutputStream();
								Long ret = getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_DEVICEID);
								if (ret == null) {
									closeConnection();
								} else {
									if (ret != 0x32534d41) {
										closeConnection();
									} else {
										isConnected = true;
										setConnectionOpen(true);
										curID = i;
										serialConn = true;
										diag.setVisible(false);
										return;
									}
								}
								try {
									sleep(500);
								} catch (InterruptedException e) {
									return;
								}
								text = "<html><body>Available Serial Ports:<br><table>";
								for (int j = 0; j < commPorts.length; j++) {
									SerialPort p = commPorts[j];
									text += "<tr><td>" + p.getSystemPortName() + ":</td>";
									int state = commPortsStates.get(j);
									if (i == j) {
										if (isConnected) {
											text += "<td>Connected!</td></tr>";
										} else {
											text += "<td>No Ams Device Found</td></tr>";
										}
									} else if (state == 0) {
										text += "<td><font color=green>Available</font></td></tr>";
									} else if (state == 1) {
										text += "<td><font color=yellow>In Use</font></td></tr>";
									} else {
										text += "<td><font color=red>Connection Error</font></td></tr>";
									}
								}
								text += "</table><br><br><br>";// end table and add 3 empty lines
								text += "If this window doesn't disappear; please download and install <b>AMSi driver</b> from www.vu-ams.nl";
								text += "</body></html>";
								label.setText(text);
								label.repaint();
								try {
									sleep(500);
								} catch (InterruptedException e) {
									return;
								}

							} catch (SerialPortInvalidPortException e) {
								commPortsStates.remove(i);
								commPortsStates.add(i, 1);
							} catch (Exception e) {
								commPortsStates.remove(i);
								commPortsStates.add(i, 2);
							}
						}
						if (isInterrupted()) {
							try {
								diag.setVisible(false);
							} catch (Exception e1) {
								e1.printStackTrace();
							}

							return;
						}
					}
					setAvailableSerialPorts();
				}
			}
		};
		connThread.start();
		diag.setVisible(true);
		// connThread.interrupt();
		try {
			connThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		if (isConnected) {
			DeviceDialog devDiag = new DeviceDialog(this);
			devDiag.setVisible(true);
		}
		diag.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		diag.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {

				diag.setVisible(false);
			}
		});
	}

	GregorianCalendar getDeviceTime() {
		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_GETTIME; // Tell device to do an
														// action
		b[3] = 0;
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null)
			return null;
		if (r[0] != 0x85)
			return null;
		int[] time = new int[r.length - 2];
		for (int i = 0; i < time.length; i++) {
			time[i] = r[i + 2];
		}
		return new Ams5fsTime(time).toGregorianCalendar();
	}

	public String getFrequencyOfChannel(int ch) {
		AmsChannelInfo chan = settings.channels[ch];
		if (chan.bStore != 0) {
			String val = AmsDeviceConstants.DIVIDERMAP.get(chan.dwDivider);
			if (val != null) {
				return val;
			} else {
				return "Off";
			}
		}
		return "Off";
	}

	int[] getOnlineDataAndContinue() {
		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_COMMAND; // Tell device to do an
														// action
		b[3] = AmsDeviceConstants.AMSII_CMDINDEX_CONTINUE_ONLINE; // Send
																	// continue
																	// online
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null)
			return null;
		if (r[0] != 13)
			return null;
		return r;

	}

	Long getParameterFromDevice(int par) {
		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_GETPARAMETER; // Tell device to get
															// a parameter
		b[3] = par;
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null)
			return null;
		if (r[0] != 0x81)
			return null;
		return Long.valueOf((r[5] << 24) + (r[4] << 16) + (r[3] << 8) + r[2]);
	}

	AmsSettings getSettings() {
		return settings;
	}

	void getSettingsFromDevice() {
		if (isConnected() == false)
			return;

		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_GETSETTINGS; // Tell device to get
															// the settings
		b[3] = 0x00;
		sendPacket(b);
		try {
			Thread.sleep(200);
		} catch (Exception e) {

		}
		int r[] = recievePacket();
		if (r == null)
			return;
		if (r[0] != 0x87)
			return;
		AmsHelper h = new AmsHelper();
		settings.wTag = h.GetUnsignedShortFromArrayOffset(r, 2);
		settings.wSize = h.GetUnsignedShortFromArrayOffset(r, 4);
		settings.wStructVersion = h.GetUnsignedShortFromArrayOffset(r, 6);
		settings.reservedOldSize = h.GetUnsignedShortFromArrayOffset(r, 8);
		for (int i = 0; i < AmsSettings.SUBJECT_ID_SIZE; i++)
			settings.szSubjectID[i] = h.GetUnsignedCharFromByte(r[10 + i]);
		settings.dwSampleTime_us = h.GetUnsignedLongFromArrayOffset(r, 22);
		settings.dwSession = h.GetUnsignedLongFromArrayOffset(r, 26);
		settings.dwSettingsFlags = h.GetUnsignedLongFromArrayOffset(r, 30);
		settings.wMinButDownTime = h.GetUnsignedShortFromArrayOffset(r, 34);
		settings.wLongButDownTime = h.GetUnsignedShortFromArrayOffset(r, 36);
		settings.wProtocolTimeout = h.GetUnsignedShortFromArrayOffset(r, 38);
		settings.wProtocolRetries = h.GetUnsignedShortFromArrayOffset(r, 40);
		settings.wCFTimeout = h.GetUnsignedShortFromArrayOffset(r, 42);
		settings.wMonBATDivider = h.GetUnsignedShortFromArrayOffset(r, 44);
		settings.wBATEmptyThreshold = h.GetUnsignedShortFromArrayOffset(r, 46);
		settings.wBATLowThreshold = h.GetUnsignedShortFromArrayOffset(r, 48);
		settings.wLDRThreshold = h.GetUnsignedShortFromArrayOffset(r, 50);
		settings.wElectrodeDistance = h.GetUnsignedShortFromArrayOffset(r, 52);
		settings.wReserved2 = h.GetUnsignedShortFromArrayOffset(r, 54);
		settings.wReserved3 = h.GetUnsignedShortFromArrayOffset(r, 56);
		settings.dst_beg_month = h.GetUnsignedCharFromByte(r[58]);
		settings.dst_beg_week = h.GetUnsignedCharFromByte(r[59]);
		settings.dst_beg_dayOfWeek = h.GetUnsignedCharFromByte(r[60]);
		settings.dst_beg_hour = h.GetUnsignedCharFromByte(r[61]);
		settings.dst_end_month = h.GetUnsignedCharFromByte(r[62]);
		settings.dst_end_week = h.GetUnsignedCharFromByte(r[63]);
		settings.dst_end_dayOfWeek = h.GetUnsignedCharFromByte(r[64]);
		settings.dst_end_hour = h.GetUnsignedCharFromByte(r[65]);
		settings.lTimeZoneJump = h.GetSignedLongFromArrayOffset(r, 66);
		settings.cAlertOn_hour = h.GetUnsignedCharFromByte(r[70]);
		settings.cAlertOn_minute = h.GetUnsignedCharFromByte(r[71]);
		settings.cAlertOff_hour = h.GetUnsignedCharFromByte(r[72]);
		settings.cAlertOff_minute = h.GetUnsignedCharFromByte(r[73]);
		settings.wAlertInterval_minutes = h.GetUnsignedShortFromArrayOffset(r, 74);
		settings.wAlertRanomization_minutes = h.GetUnsignedShortFromArrayOffset(r, 76);
		settings.nChannels = h.GetUnsignedLongFromArrayOffset(r, 78);
		for (int i = 0; i < AmsDeviceConstants.NCHANNELS; i++)
			settings.channels[i].ReadFromArray(r, 82 + i * 64);
		for (int i = 0; i < 84; i++)
			settings.szReserved[i] = h.GetUnsignedCharFromByte(r[1426 + i]);
		settings.dwFlushInterval = h.GetUnsignedLongFromArrayOffset(r, 1510);
		settings.wZ0inRangeIgnore = h.GetUnsignedShortFromArrayOffset(r, 1514);
		settings.wSCLinRangeIgnore = h.GetUnsignedShortFromArrayOffset(r, 1516);
		settings.wECGinRangeIgnore = h.GetUnsignedShortFromArrayOffset(r, 1518);
		settings.wIresInRangeIgnore = h.GetUnsignedShortFromArrayOffset(r, 1520);
		settings.wZ0ThresholdMin = h.GetUnsignedShortFromArrayOffset(r, 1522);
		settings.wZ0ThresholdMax = h.GetUnsignedShortFromArrayOffset(r, 1524);
		settings.wSCLThresholdMin = h.GetUnsignedShortFromArrayOffset(r, 1526);
		settings.wSCLThresholdMax = h.GetUnsignedShortFromArrayOffset(r, 1528);
		settings.wECGThresholdMin = h.GetUnsignedShortFromArrayOffset(r, 1530);
		settings.wECGThresholdMax = h.GetUnsignedShortFromArrayOffset(r, 1532);
		settings.wIresThresholdMin = h.GetUnsignedShortFromArrayOffset(r, 1534);
		settings.wIresThresholdMax = h.GetUnsignedShortFromArrayOffset(r, 1536);

	}

	public SerialPort getsPort() {
		return sPort;
	}

	public AmsStatics getStatics() {
		return statics;
	}

	void getStaticsFromDevice() {
		if (isConnected() == false)
			return;
		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_GETSTATICS; // Tell device to get
														// the settings
		b[3] = 0x00;
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null)
			return;
		if (r[0] != 0x89)
			return;
		AmsHelper h = new AmsHelper();
		getStatics().wTag = h.GetUnsignedShortFromArrayOffset(r, 2);
		getStatics().wSize = h.GetUnsignedShortFromArrayOffset(r, 4);
		getStatics().wStructVersion = h.GetUnsignedShortFromArrayOffset(r, 6);
		getStatics().reservedOldSize = h.GetUnsignedShortFromArrayOffset(r, 8);
		getStatics().dwDeviceID = h.GetUnsignedLongFromArrayOffset(r, 10);
		getStatics().dwBaudrate = h.GetUnsignedLongFromArrayOffset(r, 14);
		getStatics().wSerialnr = h.GetUnsignedShortFromArrayOffset(r, 18);
		getStatics().wHardwareVersion = h.GetUnsignedShortFromArrayOffset(r, 20);
		getStatics().wCalSCLdcLevel = h.GetUnsignedShortFromArrayOffset(r, 22);
		getStatics().wCalibratieB = h.GetUnsignedShortFromArrayOffset(r, 24);
		getStatics().wCalibratieC = h.GetUnsignedShortFromArrayOffset(r, 26);
		getStatics().wCalibratieD = h.GetUnsignedShortFromArrayOffset(r, 28);
		for (int i = 0; i < 12; i++)
			getStatics().reservedA[i] = h.GetUnsignedCharFromByte(r[30 + i]);
		getStatics().dwChannelDisableMask = h.GetUnsignedLongFromArrayOffset(r, 42);
		getStatics().cSclMode = h.GetUnsignedCharFromByte(r[46]);
		for (int i = 0; i < 3; i++)
			getStatics().reservedB[i] = h.GetUnsignedCharFromByte(r[47 + i]);
	}

	public synchronized boolean isBusyCommunicating() {
		return isBusyCommunicating;
	}

	boolean isChannelSupported(String chan) {
		if (chan.equals("SCL")) {
			return (getStatics().dwChannelDisableMask & (1 << AmsDeviceConstants.CH_SCL)) == 0;
		} else if (chan.equals("ICG")) {
			return (getStatics().dwChannelDisableMask
					& ((1 << AmsDeviceConstants.CH_DZ) | (1 << AmsDeviceConstants.CH_Z0))) == 0;
		} else if (chan.equals("PCG")) {
			return (getStatics().dwChannelDisableMask & (1 << AmsDeviceConstants.CH_PCG)) == 0;
		} else if (chan.equals("MOT")) {
			return (getStatics().dwChannelDisableMask & (1 << AmsDeviceConstants.CH_YMT)) == 0;
		} else if (chan.equals("ECG")) {
			return (getStatics().dwChannelDisableMask & (1 << AmsDeviceConstants.CH_ECG)) == 0;
		}
		return true;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public synchronized boolean isConnectionOpen() {
		return isConnectionOpen;
	}

	boolean isUnlocked() {
		Long amsState = getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_AMSSTATE);
		if (amsState == null) {
			return false;
		}
		if (amsState == AmsDeviceConstants.VUAMSII_DEVSTATE_WAITFORCF
				|| amsState == AmsDeviceConstants.VUAMSII_DEVSTATE_COVEROPEN
				|| amsState == AmsDeviceConstants.VUAMSII_DEVSTATE_WAITFORSTART)
			return true;
		return false;
	}

	/*
	 * public void ConnectToBluetooth(){ StreamConnection conn = null; try {
	 * conn = (StreamConnection) Connector.open(
	 * "btspp://008098E6881E:1;authenticate=false;encrypt=false;master=false");
	 * } catch (IOException e) {
	 * e.printStackTrace(); } try { in = conn.openInputStream(); out =
	 * conn.openOutputStream(); isConnected=true; } catch (IOException e) { //
	 * e.printStackTrace(); } }
	 */

	int[] recievePacket() {
		if (isBusyCommunicating()) {
			return null;
		}

		StoppableRunnable run = new StoppableRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 200; i++) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						stopThread();
					}
					if (isStopped()) {
						return;
					}
				}
			}
		};

		RecieveRunnable recvRun = new RecieveRunnable(run);

		CachedThreadPool.execute(recvRun);
		Future<?> fut = CachedThreadPool.submit(run);
		try {
			if (!run.isStopped()) {
				fut.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return recvRun.getRecvPacket();

	}

	boolean sendCommand(int com) {
		int b[] = new int[4];
		b[0] = 0x08;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_COMMAND; // Tell device to do an
														// action
		b[3] = com;
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null) {
			return false;
		}
		if (r[0] != 0x8B) {
			return false;
		}
		return true;
	}

	void sendPacket(int pkt[]) {
		if (isBusyCommunicating()) {
			return;
		}
		StoppableRunnable run = new StoppableRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 200; i++) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						stopThread();
					}
					if (isStopped()) {
						return;
					}
				}
			}
		};

		SendPacketRunnable sendRun = new SendPacketRunnable(pkt, run);

		CachedThreadPool.execute(sendRun);
		Future<?> fut = CachedThreadPool.submit(run);
		try {
			if (!run.isStopped()) {
				fut.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	boolean sendSettingsToDevice(AmsSettings set) {
		int[] setArrat = set.toDeviceArray();
		int[] b = new int[4 + setArrat.length];
		AmsHelper h = new AmsHelper();
		h.putUnsignedShortInArray(b, 0, 8 + setArrat.length);
		b[2] = AmsDeviceConstants.AMSII_TAG_SETSETTINGS;
		b[3] = 0;
		for (int i = 4; i < b.length; i++) {
			b[i] = setArrat[i - 4];
		}
		sendPacket(b);
		for (int i = 0; i < 20; i++) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		int r[] = recievePacket();
		if (r == null) {
			return false;
		}
		if (r[0] != 0x88) {
			return false;
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
		}
		getSettingsFromDevice();
		return settings.equals(set);
	}

	void sendMarkersToDevice(String s) {
		int[] b = new int[52];

		for (int i = 0; i < 52; i++) {
			b[i] = 0;
		}

		b[0] = 0x38; // Size of the packet
		b[2] = AmsDeviceConstants.AMSII_TAG_SERIAL_EVENT; // tag

		b[4] = 0x03; // wTag

		b[6] = 0x30; // wSize = 48

		b[8] = 0x11; // clock tick
		b[9] = 0x11;
		b[10] = 0x11;
		b[11] = 0x11;

		b[12] = 0x01; // lType
		b[16] = 0x04; // lCode

		for (int j = 20; j < (20 + s.length()); j++) {
			b[j] = s.charAt(j - 20);
		}
		sendPacket(b);
		return;
	}

	boolean sendStaticsToDevice(AmsStatics stat) {
		int[] statArray = stat.toDeviceArray();
		int[] b = new int[4 + statArray.length];
		AmsHelper h = new AmsHelper();
		h.putUnsignedShortInArray(b, 0, 8 + statArray.length);
		b[2] = AmsDeviceConstants.AMSII_TAG_SETSTATICS;
		b[3] = 0;
		for (int i = 4; i < b.length; i++) {
			b[i] = statArray[i - 4];
		}
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null) {
			return false;
		}
		if (r[0] != 0x8a) {
			return false;
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
		}
		getStaticsFromDevice();
		return statics.equals(stat);
	}

	// @SuppressWarnings("rawtypes")
	public void setAvailableSerialPorts() {
		// commPorts.clear();
		commPortsStates.clear();
		String name = "";
		name = System.getProperty("os.name");
		/*
		 * Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		 * while (thePorts.hasMoreElements()) {
		 */
		for (int i = 0; i < commPorts.length; i++) {
			System.out.println("open port " + commPorts[i].getSystemPortName());
			// CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			if (name.contains("Mac")) {
				if (commPorts[i].getSystemPortName().startsWith("tty.usbserial-")) {
					/*
					 * switch (com.getPortType()) {
					 * case CommPortIdentifier.PORT_SERIAL :
					 */
					try {
						SerialPort thePort = commPorts[i];
						thePort.openPort();
						thePort.closePort();
						// commPorts.add(com);
						commPortsStates.add(0);
					} catch (SerialPortInvalidPortException e) {
						// commPorts.add(com);
						commPortsStates.add(1);
					} catch (Exception e) {
						System.err.println("Failed to open port " + commPorts[i].getSystemPortName());
						e.printStackTrace();
						// commPorts.add(com);
						commPortsStates.add(2);
					}
					// }
				} else
					commPortsStates.add(1);
			} else {
				/*
				 * switch (com.getPortType()) {
				 * case CommPortIdentifier.PORT_SERIAL :
				 */
				try {
					SerialPort thePort = commPorts[i];
					thePort.openPort();
					thePort.closePort();
					// commPorts.add(com);
					commPortsStates.add(0);
				} catch (SerialPortInvalidPortException e) {
					// commPorts.add(com);
					commPortsStates.add(1);
				} catch (Exception e) {
					System.err.println("Failed to open port " + commPorts[i].getSystemPortName());
					e.printStackTrace();
					// commPorts.add(com);
					commPortsStates.add(2);
				}

				// }
			}
		}
	}

	public synchronized void setBusyCommunicating(boolean isBusyCommunicating) {
		this.isBusyCommunicating = isBusyCommunicating;
	}

	synchronized void setConnectionOpen(boolean b) {
		isConnectionOpen = b;
	}

	void setDeviceTimeToNow() {
		int[] timeArray = new Ams5fsTime(Calendar.getInstance()).toDeviceArray();
		int[] b = new int[4 + timeArray.length];
		b[0] = 8 + timeArray.length;
		b[1] = 0x00;
		b[2] = AmsDeviceConstants.AMSII_TAG_SETTIME; // Tell device to do an
														// action
		b[3] = 0;
		for (int i = 4; i < b.length; i++) {
			b[i] = timeArray[i - 4];
		}
		sendPacket(b);
		int r[] = recievePacket();
		if (r == null) {
			return;
		}
		if (r[0] != 0x86) {
			return;
		}

	}

	public void setsPort(SerialPort sPort) {
		this.sPort = sPort;
	}

	public void setStatics(AmsStatics statics) {
		this.statics = statics;
	}

	void startOnline(int chan, int div) {
		for (int i = 0; i < 5; i++) {
			int b[] = new int[132];
			b[0] = 136;
			b[1] = 0x00;
			b[2] = AmsDeviceConstants.AMSII_TAG_ONLINE_START; // Tell device to
																// start online
			b[3] = 0x00;

			b[4] = 232; // JdH b[4] and b[5] are 1000ms (3*256 = 768 768+232=1000)
			b[5] = 3; // Set timeout to 1000 ms
			b[6] = 0; // Disable events
			b[7] = 21; // Set to 21 channels
			for (int j = 8; j < 132; j++) {
				b[j] = 0;
			}
			b[8 + 4 * chan] = div;
			sendPacket(b);
			int r[] = recievePacket();
			if (r == null) {
				continue;
			}
			if (r[0] != 0x8c) {
				continue;
			}
			return;
		}

	}

	void stopOnline() {
		for (int i = 0; i < 10; i++) {
			if (sendCommand(AmsDeviceConstants.AMSII_CMDINDEX_ABORT_ONLINE) == true) {
				return;
			}
		}
		return;
	}

	public void tryToReconnect() {

		// Version 2.3 - To establish connection immediately after loosing, these lines
		// should be commented
		/*
		 * if (isBusyCommunicating())
		 * return;
		 */
		if (serialConn) {
			SerialPort port;
			try {
				closeConnection();
				port = commPorts[curID];
				port.openPort();
				port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
				setsPort((SerialPort) port);
				getsPort().setComPortParameters(AmsDeviceConstants.BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
						SerialPort.NO_PARITY);
				in = getsPort().getInputStream();
				out = getsPort().getOutputStream();
				setConnectionOpen(true);
			} catch (SerialPortInvalidPortException e) {
				setConnectionOpen(false);
				e.printStackTrace();
			}
		} else {
			closeConnection();
			try {
				System.out.println("Trying to Reconnect");
				conn = (StreamConnection) Connector.open(
						"btspp://" + blueToothAddress + ":1;authenticate=false;encrypt=false;master=false",
						Connector.READ_WRITE, true);
				in = conn.openDataInputStream();
				out = conn.openDataOutputStream();
				setConnectionOpen(true);
				isConnected = true;
			} catch (IOException e2) {
				BlueCoveImpl.shutdown();
			}
		}

	}
}
