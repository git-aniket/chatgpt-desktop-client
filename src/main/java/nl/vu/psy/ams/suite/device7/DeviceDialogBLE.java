package nl.vu.psy.ams.suite.device7;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
// import java.nio.charset.StandardCharsets;
// import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
// import java.text.SimpleDateFormat;
import java.util.Arrays;
// import java.util.Calendar;
import java.util.Date;
// import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
// import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
// import nl.vu.psy.ams.suite.main.AppSettings;
// import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.JLabelAntialiased;
import nl.vu.psy.ams.suite.tools.Utils;

// import com.mathworks.toolbox.javabuilder.*;

public class DeviceDialogBLE extends JFrame {

	class RecalcThread extends Thread {

		private boolean started = true, streamStarted = false;

		public synchronized boolean isStarted() {
			return started;
		}

		public synchronized boolean isDataStream() {
			return streamStarted;
		}

		public void run() {
			long oldtime = System.nanoTime();
			byte[] buffer = new byte[516];
			while (isStarted()) {
				oldtime = System.nanoTime();
				if (isDataStream()) {
                    // remove matlab dependency
//					Object[] c;
//					try {
//						// c = ams.obj.multipleRead(2, ams.a);
//						// MWNumericArray streamData = (MWNumericArray)c[1];
//						c = ams.obj.getData(1, ams.a);
//						MWNumericArray streamData = (MWNumericArray) c[0];
//						buffer = streamData.getByteData();
//						streamData.dispose();
//					} catch (MWException e) {
//					}
					ams.handlePacket(buffer);
				}
				setupValuesTXT();
				setupSDTXT(false);
				setupFreqTXT();
				pack();
				revalidate();
				repaint();
				for (int i = 0; i < 20; i++) {
					if (System.nanoTime() - oldtime > 2000000000.)
						break;
					if (isStarted() == false)
						return;
					try {
						sleep(100);
					} catch (InterruptedException e) {
						return;
					}
				}

			}
		}

		public synchronized void setStarted(boolean started) {
			this.started = started;
		}

		public synchronized void setDataStream(boolean ds) {
			this.streamStarted = ds;
		}
	}

	private static final long serialVersionUID = 1L;
	private AmsDevice7 ams;
	private JPanel leftPanel;
	private Container middlePanel;
	DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	DecimalFormat onePlace = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	DecimalFormat zeroPlace = new DecimalFormat("0", new DecimalFormatSymbols(Locale.ENGLISH));
	RecalcThread recalcThread;
	private JPanel rightPanel, vallab;
	private JLabel batLab, phaseLab, timeLab, firmwareLab, hardwareLab, hrLab, electrodeLab;
	private Font fnt;
	private boolean givenUp = false, errorShown = false, state; // , connectionLost = false;
	private JButton onlinebut, startStopButton, changeStaticsBut;
	private JButton setdevtimebut, setPhaseBut, bleShutdownBut;
	private JLabelAntialiased freqlab;
	private JLabelAntialiased sdlab;
	int portNumber;
	double nPhase, z0_value;
	// private int packet;
	String deviceIP, subjectId = "N/A", eDistance = "N/A", formatPhase = "N/A", deviceName;

	public DeviceDialogBLE(AmsDevice7 amsDevice, String deviceName, int portNumber, int packet) {
		super();
		fnt = new Font("Arial", Font.PLAIN, 20);
		this.ams = amsDevice;
		// this.deviceIP = deviceIP;
		this.deviceName = deviceName;
		this.portNumber = portNumber;
		// this.packet = packet;
		this.setTitle("AMS7 Device " + deviceName + " connected via Bluetooth");
		leftPanel = new JPanel(new BorderLayout());
		middlePanel = new JPanel(new BorderLayout());
		rightPanel = new JPanel(new BorderLayout());
		setupButtons();
		setupFrequencyPanel();
		setupValuesPanel();
		setupSDPanel();
		this.setLayout(new BorderLayout());
		this.add(leftPanel, BorderLayout.WEST);
		this.add(middlePanel, BorderLayout.CENTER);
		this.add(rightPanel, BorderLayout.EAST);
		this.pack();
		this.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
		state = getStatus();
		startThread();
		setEnabledItems(state, true);
		setIconImage(new ImageIcon("src/main/resources/img/hearmysite.png").getImage());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				closeDialog();
			}
		});
	}

	private void changeStatics() {
		if (Utils.askForExpertPassword(true) == false)
			return;
		final JDialog diag = new JDialog(DeviceDialogBLE.this, "Change statics", true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Send custom command:"), BorderLayout.WEST);
		final JTextField tf4 = new JTextField(12);
		tf4.setColumns(30);
		pan.add(tf4, BorderLayout.EAST);
		diag.add(pan);

		// String sId = "", name = "";
		// int wait2 = 200;

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Create new data file after (minutes; 0 is
		// never):"), BorderLayout.WEST);
		// sId = "";
		// ams.webSocketClient.send("cmd !RESTART_ACQ;");
		// try {
		// int i = 0;
		// while (!name.equals("RESTART_ACQ") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JTextField tf4 = new JTextField(12);
		// tf4.setColumns(12);
		// if (name.equals("RESTART_ACQ"))
		// tf4.setText(sId);
		// else
		// System.out.println("name " + name);
		// pan.add(tf4, BorderLayout.EAST);
		// diag.add(pan);

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Turn off LEDs after (seconds; 0 is never):"),
		// BorderLayout.WEST);
		// sId = "";
		// ams.webSocketClient.send("cmd !LED_DIM;");
		// try {
		// int i = 0;
		// while (!name.equals("LED_DIM") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JTextField tf7 = new JTextField(12);
		// tf7.setColumns(12);
		// if (name.equals("LED_DIM"))
		// tf7.setText(sId);
		// else
		// System.out.println("name " + name);
		// pan.add(tf7, BorderLayout.EAST);
		// diag.add(pan);

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Device name"), BorderLayout.WEST);
		// sId = "";
		// if (!ams.webSocketClient.isOpen()) {
		// connectionLost();
		// if (isGivenUp())
		// return;
		// }
		// ams.webSocketClient.send("cmd !SENSORNAME;");
		// try {
		// int i = 0;
		// while (!name.equals("SENSORNAME") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JTextField tf1 = new JTextField(12);
		// tf1.setColumns(12);
		// if (name.equals("SENSORNAME"))
		// tf1.setText(sId);
		// else
		// System.out.println("name " + name);
		// pan.add(tf1, BorderLayout.EAST);
		// diag.add(pan);

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Serial number"), BorderLayout.WEST);
		// sId = "";
		// if (!ams.webSocketClient.isOpen()) {
		// connectionLost();
		// if (isGivenUp())
		// return;
		// }
		// ams.webSocketClient.send("cmd !SERIAL;");
		// try {
		// int i = 0;
		// while (!name.equals("SERIAL") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JTextField tf2 = new JTextField(12);
		// tf2.setColumns(12);
		// if (name.equals("SERIAL"))
		// tf2.setText(sId);
		// else
		// System.out.println("name " + name);
		// pan.add(tf2, BorderLayout.EAST);
		// diag.add(pan);

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Auto power off (seconds)"),
		// BorderLayout.WEST);
		// sId = "";
		// if (!ams.webSocketClient.isOpen()) {
		// connectionLost();
		// if (isGivenUp())
		// return;
		// }
		// ams.webSocketClient.send("cmd !AUTO_POWER;");
		// try {
		// int i = 0;
		// while (!name.equals("AUTO_POWER") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JTextField tf3 = new JTextField(12);
		// tf3.setColumns(12);
		// if (name.equals("AUTO_POWER"))
		// tf3.setText(sId);
		// else
		// System.out.println("name " + name);
		// pan.add(tf3, BorderLayout.EAST);
		// diag.add(pan);

		// pan = new JPanel();
		// pan.setLayout(new BorderLayout());
		// pan.add(new JLabelAntialiased("Send device log to file"), BorderLayout.WEST);
		// sId = "";
		// if (!ams.webSocketClient.isOpen()) {
		// connectionLost();
		// if (isGivenUp())
		// return;
		// }
		// ams.webSocketClient.send("cmd !LOG_TO_FILE;");
		// try {
		// int i = 0;
		// while (!name.equals("LOG_TO_FILE") && i < 25) {
		// Thread.sleep(wait2);
		// sId = ams.webSocketClient.getSetting();
		// if (sId != "" && sId != null) {
		// name = sId.split("=", 3)[0];
		// sId = sId.split("=", 3)[1];
		// }
		// i++;
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// final JComboBox<String> logCOB = new JComboBox<>(new String[]{"True",
		// "False"});
		// if (name.equals("LOG_TO_FILE")) {
		// if (sId.equals("true"))
		// logCOB.setSelectedIndex(0);
		// if (sId.equals("false"))
		// logCOB.setSelectedIndex(1);
		// }
		// else
		// System.out.println("name " + name);
		// pan.add(logCOB, BorderLayout.EAST);
		// diag.add(pan);

		pan = new JPanel();

		JButton but = new JButton("Send");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String idText = tf4.getText();
				if (idText != "")
					;
				writeBleQuietly(idText);
			}
		});
		pan.add(but);

		JButton butCancel = new JButton("Close");
		butCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		pan.add(butCancel);

		diag.add(pan);

		diag.pack();
		diag.setLocationRelativeTo(DeviceDialogBLE.this);
		diag.getRootPane().setDefaultButton(but);// ENTER will hit button Save

		diag.setVisible(true);
	}

	void startThread() {
		if (recalcThread == null || !recalcThread.isStarted()) {
			recalcThread = new RecalcThread();
			recalcThread.start();
		}
	}

	private void stopThread() {
		if (recalcThread != null && recalcThread.isStarted()) {
			recalcThread.setStarted(false);
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
			recalcThread = null;
		}
	}

	protected void closeDialog() {
		ams.conn = false;
		stopThread();
		DeviceDialogBLE.this.setVisible(false);
		DeviceDialogBLE.this.dispose();
        // remove matlab dependency
//		try {
//			ams.obj.bleDisconnect(ams.a);
//			ams.a.dispose();
//			ams.obj.dispose();
//		} catch (MWException e) {
//			e.printStackTrace();
//		}
	}

	void connectionLost() {
	}

	private void startReceiveStream() {
        // remove matlab dependency
//		try {
//			ams.obj.subscribeData(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//					"5F3A659E-897E-45E1-B016-007107C96DF7");
//			ams.obj.subscribeElectrode(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//					"6A3A659E-897E-45E1-B016-007107C96DF7");
//		} catch (MWException e) {
//		}
		int count = 0;
		byte[] buffer = new byte[516], buffer_old = new byte[516];
		while (count < 20) { // try to get headers
            // remove matlab dependency
//			Object[] c;
//			try {
//				// c = ams.obj.multipleRead(2, ams.a);
//				// MWNumericArray streamData = (MWNumericArray)c[1];
//				c = ams.obj.getData(1, ams.a);
//				MWNumericArray streamData = (MWNumericArray) c[0];
//				buffer = streamData.getByteData();
//				streamData.dispose();
//			} catch (MWException e) {
//			}
			if (!Arrays.equals(buffer, buffer_old)) {
				ams.handlePacket(buffer);
				buffer_old = buffer;
				count++;
			}
		}
	}

	private void doStartOrStop(String[] textData, Integer icgVmm) {
		if (startStopButton.getText().equals("Start")) {
			if (textData[0].length() > 25) {
				JOptionPane.showMessageDialog(this, "The subject ID can not contain more than 25 characters!",
						"Subject ID too long",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (textData[1].length() > 25) {
				JOptionPane.showMessageDialog(this, "The study ID can not contain more than 25 characters!",
						"Study ID too long",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (textData[2].length() > 25) {
				JOptionPane.showMessageDialog(this, "The session ID can not contain more than 25 characters!",
						"Session ID too long",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			Object[] options = { "Yes", "No" };
			if (icgVmm < 0) {
				JOptionPane.showMessageDialog(this, "The ICG-V distance can not be negative!",
						"Negative ICG-V distance", JOptionPane.ERROR_MESSAGE);
				return;
			} else if (icgVmm < 50 || icgVmm > 600) {
				int r = JOptionPane.showOptionDialog(this, "You have entered " + icgVmm
						+ " millimeters as the distance between the two front electrodes. This is an implausible value, are you sure you want to proceed?",
						"Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (r != 0) {
					return;
				}
			}
			if (textData[4].length() > 230) {
				JOptionPane.showMessageDialog(this, "The comment can not contain more than 230 characters!",
						"Comment too long",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			startStopButton.setEnabled(false);
			final JDialog diag = new JDialog(DeviceDialogBLE.this, "Starting", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Starting recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialogBLE.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					writeBleQuietly("!SUBJECT_ID=" + textData[0] + ";");
					writeBleQuietly("!STUDY_ID=" + textData[1] + ";");
					writeBleQuietly("!SESSION_ID=" + textData[2] + ";");
					writeBleQuietly("!E_DISTANCE=" + textData[3] + ";");
					writeBleQuietly("!COMMENT=" + textData[4] + ";");
					writeBleQuietly("r");
					startReceiveStream();
					getStatus(1);
					state = true;
					if (recalcThread != null)
						recalcThread.setDataStream(true);
					stopThread();
					setEnabledItems(true);
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(false);
			try {
				thrd.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			OnlineDialog7 oDiag = new OnlineDialog7(ams, null, this);
			oDiag.setVisible(true);
		} else if (startStopButton.getText().equals("Stop")) {
			final JDialog diag = new JDialog(DeviceDialogBLE.this, "Stopping", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Stopping recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialogBLE.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					writeBleQuietly("s");
					getStatus(0);
					state = false;
					setEnabledItems(false);
					startThread();
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(true);
		}
	}

	private void doShutdown() {
		bleShutdownBut.setEnabled(false);
		if (bleShutdownBut.getText().equals("Shut down Device")) {
			stopThread();
			writeBleQuietly("Q");
			closeDialog();
			// } else if (bleShutdownBut.getText().equals("Turn off Device Wifi")) {
			// ams.webSocketClient.send("cmd w");
			// closeDialog();
		}
	}

	protected synchronized boolean isGivenUp() {
		return givenUp;
	}

    // remove matlab dependency
//	MWNumericArray readBleQuietly(String service, String characteristic) {
//		Object[] c;
//		try {
//			c = ams.obj.readData(2, ams.a, deviceName, service, characteristic);
//			return (MWNumericArray) c[1];
//		} catch (MWException e) {
//			return null;
//		}
//	}

	boolean writeBleQuietly(String input) {
        // remove matlab dependency
//        try {
//			ams.obj.writeData(1, ams.a, deviceName, "60462F12-9543-9999-12C8-58B459A2712D",
//					"5C3A659E-897E-45E1-B016-007107C96DF7", input);
//			return true;
//		} catch (MWException e) {
//			return false;
//		}
        return true;
	}

	private Boolean getStatus() {
		int b = 2;
        // remove matlab dependency
//        try {
//			MWNumericArray acqData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//					"5D3A659E-897E-45E1-B016-007107C96DF7");
//			b = acqData.getByteData()[0];
//			ams.obj.subscribeAcq(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//					"5D3A659E-897E-45E1-B016-007107C96DF7");
//			while (b == 2) {
//				acqData = (MWNumericArray) ams.obj.getAcq(1, ams.a)[0];
//				b = acqData.getByteData()[0];
//				acqData.dispose();
//			}
//			ams.obj.unsubscribeAcq(ams.a);
//		} catch (MWException e) {
//			e.printStackTrace();
//		}
		return (b != 0);
	}

	private Boolean getStatus(int state) {
		int b = 2;
        // remove matlab dependency
//        try {
//			MWNumericArray acqData = null;
//			while (acqData == null)
//				acqData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//						"5D3A659E-897E-45E1-B016-007107C96DF7");
//			b = acqData.getByteData()[0];
//			ams.obj.subscribeAcq(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//					"5D3A659E-897E-45E1-B016-007107C96DF7");
//			while (b != state) {
//				acqData = (MWNumericArray) ams.obj.getAcq(1, ams.a)[0];
//				b = acqData.getByteData()[0];
//				acqData.dispose();
//			}
//			ams.obj.unsubscribeAcq(ams.a);
//		} catch (MWException e) {
//			e.printStackTrace();
//		}
		return (b != 0);
	}

	private void setEnabledItems(boolean state) {
		setEnabledItems(state, false);
	}

	private void setEnabledItems(boolean state, boolean init) {
		if (!state) {
			startStopButton.setText("Start");
			startStopButton.setEnabled(true);
			onlinebut.setEnabled(false);
			onlinebut.setVisible(false);
			setdevtimebut.setEnabled(true);
			setPhaseBut.setEnabled(true);
			changeStaticsBut.setEnabled(true);
			bleShutdownBut.setText("Shut down Device");
			bleShutdownBut.setEnabled(true);
			if (recalcThread != null)
				recalcThread.setDataStream(false);
            // remove matlab dependency
//            if (!init) {
//				try {
//					ams.obj.unsubscribeData(ams.a);
//					ams.obj.unsubscribeElectrode(ams.a);
//				} catch (MWException e) {
//					e.printStackTrace();
//				}
//			}
		} else {
            // remove matlab dependency
//			if (init) {
//				try {
//					ams.obj.subscribeData(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//							"5F3A659E-897E-45E1-B016-007107C96DF7");
//					ams.obj.subscribeElectrode(ams.a, deviceName, "59462F12-9543-9999-12C8-58B459A2712D",
//							"6A3A659E-897E-45E1-B016-007107C96DF7");
//				} catch (MWException e) {
//				}
//				if (recalcThread != null)
//					recalcThread.setDataStream(true);
//			}
			startStopButton.setText("Stop");
			startStopButton.setEnabled(true);
			onlinebut.setEnabled(true);
			onlinebut.setVisible(true);
			setdevtimebut.setEnabled(false);
			setPhaseBut.setEnabled(false);
			changeStaticsBut.setEnabled(false);
			// bleShutdownBut.setText("Turn off Device Wifi");
			bleShutdownBut.setEnabled(true);
		}
	}

	protected synchronized void setGivenUp(boolean b) {
		givenUp = b;
	}

	public String createFreqText(Ams7fsChannelInfo chan, int value, int valueT) {
		String name = chan.getSzID();
		if (chan.getDwDivider() <= 0) {
			return "<tr><td>" + name + ": </td><td>Off</td></tr>";
		} else {
			double real;
			if (chan.getFormula().length() < 1)
				real = value * chan.getRealSlope() + chan.getRealConstant();
			else {
				Set<String> variables = new HashSet<String>(chan.getConstants().keySet());
				variables.add(chan.getSzID());
				if (chan.getSzID().equals("P_sc"))
					variables.add("T_sc");
				Expression e = new ExpressionBuilder(chan.getFormula()).variables(variables).build()
						.setVariables(chan.getConstants());
				if (chan.getSzID().equals("P_sc")) {
					Ams7fsChannelInfo chanT = null;
					for (Ams7fsChannelInfo s : ams.channelInfo)
						if (s.getSzID().equals("T_sc"))
							chanT = s;
					double val = value * chan.getRealSlope();
					double valT = valueT * chanT.getRealSlope();
					e.setVariable("P_sc", val);
					e.setVariable("T_sc", valT);
					real = e.evaluate();
				} else {
					double val = value * chan.getRealSlope();
					e.setVariable(chan.getSzID(), val);
					real = e.evaluate();
				}
			}
			if (name.equals("Z0"))
				z0_value = real;
			long sr = 1000 / chan.getDwDivider();
			return "<tr><td>" + name + ": </td><td>" + twoPlaces.format(real) + " " + chan.getSzUnit() + "</td><td>"
					+ sr + " Hz</td></tr>";
		}
	}

	private void setupFreqTXT() {
		String text = "<html><body><table><font size='5'>";
		int[] values = ams.getRawValues();
		int i = 0;
		for (Ams7fsChannelInfo chan : ams.channelInfo) {
			int value = values[i];
			int valueT = values[i + 1];
			i++;
			text += createFreqText(chan, value, valueT);
		}
		text += "</font></table></body></html>";
		freqlab.setText(text);
	}

	private void setupFrequencyPanel() {
		freqlab = new JLabelAntialiased("");
		setupFreqTXT();
		freqlab.setBorder(BorderFactory.createTitledBorder("Sampling Frequencies"));
		leftPanel.add(freqlab, BorderLayout.CENTER);
	}

	private void setupValuesTXT() {
		this.setTitle("AMS7 Device " + deviceName + " connected via Bluetooth");
		String text = "";
        // remove matlab dependency
//		String batStatus = "";
//		byte[] bData = {};
//		MWNumericArray battData = readBleQuietly("180F", "2A19");
//		if (battData != null) {
//			batStatus = battData.getByteData().toString();
//			bData = battData.getByteData();
//			battData.dispose();
//			text += "Battery: ";
//			boolean isBatWarning = false, isBatFull = false;
//			String voltage = "N/A";
//			Integer nVoltage = 0;
//			if (batStatus != "") {
//				nVoltage = Integer.valueOf(bData[0]);
//				if (nVoltage < 25)
//					isBatWarning = true;
//				voltage = nVoltage.toString();
//				if (nVoltage > 80)
//					isBatFull = true;
//				voltage = nVoltage.toString();
//			}
//			if (isBatWarning)
//				batLab.setForeground(Color.red);
//			else if (isBatFull)
//				batLab.setForeground(Color.green);
//			else
//				batLab.setForeground(Color.BLACK);
//			text += voltage;
//			text += "%";
//			batLab.setText(text);
//		}
		text = "";
		String hr = "-";
//		byte[] data;
		if (state) {
//			MWNumericArray hrData = readBleQuietly("180D", "2A37");
//			if (hrData != null) {
//				data = hrData.getByteData();
//				hrData.dispose();
//				if (data.length >= 2) {
//					int flag = data[0] & 1;
//					int hri;
//					if (flag == 0)
//						hri = data[1];
//					else
//						hri = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
//					hr = Integer.toString(hri);
//					if (hri < 0 || hri > 180)
//						System.out.println("Improbable hr value" + hr);
//				}
//			}
		}
		hrLab.setText("Heart rate: " + hr + " bpm");
		// String respPhase = ams.webSocketClient.getPhase();
		formatPhase = "N/A";
//		byte[] b;
//		MWNumericArray phaseData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//				"6C3A659E-897E-45E1-B016-007107C96DF7");
//		if (phaseData != null) {
//			b = phaseData.getByteData();
//			int i = b[0] & 0xff;
//			formatPhase = String.valueOf(i);
//			nPhase = i;
//		}
		phaseLab.setText("Resp. phase: " + formatPhase);

		String devTime = "N/A";
		text += "Device Time: ";
		boolean isWarning = false;
//		MWNumericArray timeData = readBleQuietly("1805", "2A2B");
//		if (timeData != null) {
//			data = timeData.getByteData();
//			timeData.dispose();
//			if (data.length >= 10) {
//				int year = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
//				int month = data[2];
//				int day = data[3];
//				int hour = data[4];
//				int minute = data[5];
//				int second = data[6];
//				int adjust = data[9];
//				int wday = data[7];
//				Calendar cal = new Ams5fsTime(
//						new int[] { year - 1900, month - 1, day, hour, minute, second, adjust, wday })
//						.toGregorianCalendar();
//				long diffTime = Math.abs(cal.getTimeInMillis() - GregorianCalendar.getInstance().getTimeInMillis());
//				if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNINGENABLED) != 0) {
//					if (diffTime > 1000.
//							* AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNING)) {
//						isWarning = true;
//					}
//				}
//				DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
//				devTime = df.format(cal.getTime());
//			}
//		}
		if (isWarning)
			timeLab.setForeground(Color.red);
		else
			timeLab.setForeground(Color.BLACK);
		text += devTime;
		timeLab.setText(text);

		String electrodeStatus = "";
		if (!state) {
			electrodeLab.setForeground(Color.BLACK);
			electrodeStatus = "Thorax Impedance (\u2126): N/A (acq not running)";
		} else {
//			MWNumericArray elecData;
//			try {
//				elecData = (MWNumericArray) ams.obj.getElectrode(1, ams.a)[0];
//				int e = elecData.getByteData()[0];
//				elecData.dispose();
//				if (e == 0) {
//					electrodeLab.setForeground(Color.BLACK);
//					electrodeStatus = "Thorax Impedance (\u2126): " + twoPlaces.format(z0_value);
//				} else {
//					electrodeLab.setForeground(Color.red);
//					electrodeStatus = "ICG blue/green back electrode(s) problem";
//				}
//			} catch (MWException e) {
//				e.printStackTrace();
//			}
		}
		electrodeLab.setText(electrodeStatus);
	}

	private void setupValuesPanel() {
		vallab = new JPanel();
		vallab.setLayout(new BoxLayout(vallab, BoxLayout.Y_AXIS));
		batLab = new JLabel("Battery voltage: N/A");
		batLab.setAlignmentX(CENTER_ALIGNMENT);
		vallab.add(batLab);
		hrLab = new JLabel("Heart rate: N/A");
		hrLab.setAlignmentX(CENTER_ALIGNMENT);
		vallab.add(hrLab);
		JPanel pan2 = new JPanel(new FlowLayout());
		phaseLab = new JLabel("Resp. phase: N/A");
		pan2.add(phaseLab);
		pan2.add(setPhaseBut);
		vallab.add(pan2);
		JPanel pan3 = new JPanel(new FlowLayout());
		timeLab = new JLabel("Device Time: N/A");
		pan3.add(timeLab);
		pan3.add(setdevtimebut);
		vallab.add(pan3);
		JPanel pan4 = new JPanel(new BorderLayout());
		firmwareLab = new JLabel("Firmware version: N/A");
		pan4.add(firmwareLab, BorderLayout.LINE_START);
		hardwareLab = new JLabel("Hardware version: N/A");
		pan4.add(hardwareLab, BorderLayout.LINE_END);
		vallab.add(pan4);
		JPanel pan5 = new JPanel(new FlowLayout());
		electrodeLab = new JLabel("Thorax Impedance (\u2126): N/A");
		pan5.add(electrodeLab);
		vallab.add(pan5);
		setupValuesTXT();
		vallab.setFont(fnt);
		vallab.setBorder(BorderFactory.createTitledBorder("Values"));
		middlePanel.add(vallab, BorderLayout.NORTH);

		String version = "";
        // remove matlab dependency
//		MWNumericArray versData = readBleQuietly("180A", "2A26");
//		byte[] b;
//		if (versData != null) {
//			b = versData.getByteData();
//			version = new String(b, StandardCharsets.UTF_8);
//		}
		firmwareLab.setText("Firmware Version: " + version);

		version = "";
        // remove matlab dependency
//		versData = readBleQuietly("180A", "2A27");
//		if (versData != null) {
//			b = versData.getByteData();
//			version = new String(b, StandardCharsets.UTF_8);
//			versData.dispose();
//		}
		hardwareLab.setText("Hardware Version: " + version);
	}

	private void setupSDPanel() {

		sdlab = new JLabelAntialiased("");
		setupSDTXT(true);
		sdlab.setFont(fnt);
		sdlab.setBorder(BorderFactory.createTitledBorder("SD Card"));
		middlePanel.add(sdlab, BorderLayout.SOUTH);
	}

	private void setupSDTXT(boolean init) {
		if (init || state) {
			String text = "<html><body><table>";
			// bytes per second: A 1000*24, M 1000*28, C 0.1*12, D 5*16, B 1*12 -> 52093.2
			double nBytesPerSecond = 52093.2;
			nBytesPerSecond *= 3600; // bytes per hour

			String bps = "";

			if (nBytesPerSecond > 1024 * 1024 * 1024)
				bps = twoPlaces.format(nBytesPerSecond / (1024 * 1024 * 1024)) + " GB";
			else
				bps = twoPlaces.format(nBytesPerSecond / (1024 * 1024)) + " MB";

			text += "<tr><td>Estimated memory usage per hour:</td><td>" + bps + "</td></tr>";

			String mem = "";
            // remove matlab dependency
//			MWNumericArray memData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//					"6B3A659E-897E-45E1-B016-007107C96DF7");
//			if (memData != null) {
//				byte[] b = memData.getByteData();
//				mem = new String(b, StandardCharsets.UTF_8);
//				memData.dispose();
//			}
			String freebytes = "N/A";
			Long nFreeBytes = 0L;
			if (mem != "") {
				try {
					nFreeBytes = Long.valueOf(mem.split(" ", 2)[0]);
				} catch (NumberFormatException e) {
					text += "<tr><td>Memory space left for:</td><td>N/A h</td></tr>";
					text += "</table></body></html>";
					sdlab.setText(text);
				}
				if (nFreeBytes > 1024 * 1024 * 4) // some boards give very high number when no card is inserted
					nFreeBytes = 0L;
				if (nFreeBytes <= 50 && !errorShown) {
					JOptionPane.showMessageDialog(this,
							"SD card (almost) full or not inserted (" + twoPlaces.format(nFreeBytes) + " MB left)",
							"SD card error", JOptionPane.ERROR_MESSAGE);
					errorShown = true;
				} else if (nFreeBytes > 50)
					errorShown = false;
				if (nFreeBytes > 1024)
					freebytes = twoPlaces.format(nFreeBytes / (1024.0)) + " GB";
				else
					freebytes = twoPlaces.format(nFreeBytes) + " MB";
			}
			text += "<tr><td>Memory available:</td><td>" + freebytes + "</td></tr>";

			double timeLeft = (double) nFreeBytes * 1024 * 1024 / nBytesPerSecond;
			if (mem == "") {
				text += "<tr><td>Memory space left for:</td><td>N/A h</td></tr>";
			} else if (timeLeft < 24.0) {
				text += "<tr><td>Memory space left for:</td><td><font color=red>" + twoPlaces.format(timeLeft)
						+ " h</font></td></tr>";
			} else {
				text += "<tr><td>Memory space left for:</td><td>" + twoPlaces.format(timeLeft) + " h</td></tr>";
			}
			text += "</table></body></html>";
			sdlab.setText(text);
		}
	}

	private void setupButtons() {
		JPanel newPanel = new JPanel();
		newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
		JPanel pan = new JPanel();
		pan.setLayout(new FlowLayout(FlowLayout.LEFT));
		pan.add(new JLabelAntialiased("Subject ID:"));
        // remove matlab dependency
//		MWNumericArray subjectData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//				"6D3A659E-897E-45E1-B016-007107C96DF7");
//		if (subjectData != null) {
//			byte[] b = subjectData.getByteData();
//			subjectId = new String(b, StandardCharsets.UTF_8);
//			subjectData.dispose();
//		}
		final JTextField tf = new JTextField(12);
		tf.setColumns(12);
		tf.setText(subjectId);
		pan.add(tf);
		pan.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan);

		JPanel pan2 = new JPanel();
		pan2.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabelAntialiased study = new JLabelAntialiased("Study ID:");
		pan2.add(study);
//		subjectData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D", "6F3A659E-897E-45E1-B016-007107C96DF7");
//		if (subjectData != null) {
//			byte[] b = subjectData.getByteData();
//			subjectId = new String(b, StandardCharsets.UTF_8);
//			subjectData.dispose();
//		}
		final JTextField tf2 = new JTextField(12);
		tf2.setText(subjectId);
		pan2.add(tf2);
		JLabelAntialiased session = new JLabelAntialiased("Session:");
		pan2.add(session);
//		subjectData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D", "703A659E-897E-45E1-B016-007107C96DF7");
//		if (subjectData != null) {
//			byte[] b = subjectData.getByteData();
//			subjectId = new String(b, StandardCharsets.UTF_8);
//			subjectData.dispose();
//		}
		final JTextField tf3 = new JTextField(3);
		tf3.setText(subjectId);
		pan2.add(tf3);
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan2);

		JPanel pan3 = new JPanel();
		pan3.setLayout(new FlowLayout(FlowLayout.LEFT));
		pan3.add(new JLabelAntialiased("ICG-V Distance (mm):"));
		final JTextField ftf = new JTextField();
//		MWNumericArray distData = readBleQuietly("59462F12-9543-9999-12C8-58B459A2712D",
//				"6E3A659E-897E-45E1-B016-007107C96DF7");
//		if (distData != null) {
//			byte[] b = distData.getByteData();
//			eDistance = new String(b, StandardCharsets.UTF_8);
//			distData.dispose();
//		}
		ftf.setColumns(3);
		ftf.setText(eDistance);
		pan3.add(ftf);
		pan3.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan3);
		JLabel comment = new JLabel("Comment");
		newPanel.add(comment);
		JTextArea textArea = new JTextArea();
		textArea.setColumns(20);
		textArea.setLineWrap(true);
		textArea.setRows(5);
		textArea.setWrapStyleWord(true);
		// textArea.setEditable(false);
		// textArea.setEnabled(false);
		textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(textArea);
		rightPanel.setLayout(new BorderLayout());

		onlinebut = new JButton("Online Graph");
		onlinebut.setEnabled(false);
		onlinebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopThread();
				OnlineDialog7 diag = new OnlineDialog7(ams, null, DeviceDialogBLE.this);
				diag.setVisible(true);
			}
		});

		// start / stop acq button
		startStopButton = new JButton("N/A");
		startStopButton.setEnabled(false);
		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String[] textData = new String[5];
				textData[0] = tf.getText();
				textData[1] = tf2.getText();
				textData[2] = tf3.getText();
				textData[3] = ftf.getText();
				textData[4] = textArea.getText();
				int icgVmm = 0;
				try {
					icgVmm = Integer.valueOf(ftf.getText());
					doStartOrStop(textData, icgVmm);
				} catch (NumberFormatException e) {
					doStartOrStop(textData, icgVmm);
					// e.printStackTrace();
				}
			}
		});
		startStopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(startStopButton);
		onlinebut.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(onlinebut);
		// setdevtimebut = new JButton("Set Device Time To Computer Time");
		setdevtimebut = new JButton("Sync");
		setdevtimebut.setEnabled(false);
		setdevtimebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Long now = new Date().getTime() / 1000;
				writeBleQuietly(now.toString() + "T");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
				}
			}
		});
		// newPanel.add(setdevtimebut);
		// setPhaseBut = new JButton("Set respiration phase");
		setPhaseBut = new JButton("Set");
		setPhaseBut.setEnabled(true);
		setPhaseBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setPhase();
			}
		});
		// newPanel.add(setPhaseBut);

		changeStaticsBut = new JButton("Edit Config");
		changeStaticsBut.setEnabled(false);
		changeStaticsBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				changeStatics();
			}
		});
		changeStaticsBut.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(changeStaticsBut);

		bleShutdownBut = new JButton("N/A");
		bleShutdownBut.setEnabled(false);
		bleShutdownBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doShutdown();
			}
		});
		bleShutdownBut.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(bleShutdownBut);

		rightPanel.add(newPanel, BorderLayout.NORTH);

		JPanel southPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				closeDialog();
			}
		});

		southPanel.add(closeButton);
		rightPanel.add(southPanel, BorderLayout.SOUTH);
	}

	private void setPhase() {
		final JDialog diag = new JDialog(this, "Channel Options", true);
		JPanel pan = new JPanel(new GridLayout(0, 3, 0, 5));
		pan.add(new JLabel("resp. phase"));
		pan.add(new JLabel());
		String[] options = { "22", "45", "67", "90", "112", "135", "157" };
		@SuppressWarnings("rawtypes")
		JComboBox phaseCB = new JComboBox<String>(options);
		phaseCB.setSelectedItem(zeroPlace.format(nPhase));
		phaseCB.setEnabled(true);
		pan.add(phaseCB);

		JPanel pan2 = new JPanel();
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				writeBleQuietly("!PHASE=" + phaseCB.getSelectedItem() + ";");
				diag.setVisible(false);
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		pan2.add(saveButton);
		pan2.add(cancelButton);

		diag.setLayout(new BorderLayout());
		diag.add(pan, BorderLayout.NORTH);
		diag.add(pan2, BorderLayout.CENTER);
		diag.pack();
		diag.getRootPane().setDefaultButton(saveButton);// ENTER will hit button Save
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
	}
}
