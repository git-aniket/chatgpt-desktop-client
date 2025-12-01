package nl.vu.psy.ams.suite.device;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.JLabelAntialiased;
import nl.vu.psy.ams.suite.tools.StoppableRunnable;
import nl.vu.psy.ams.suite.tools.Utils;

import com.intel.bluetooth.BlueCoveImpl;

/*
 * Large dialog that shows all information about a device,
 * and provides buttons that can change device parameters (the logic of
 * these buttons should be refactored out for a nicer source code).
 * The dialog starts a thread that downloads all device information from
 * the AMS device every 2 seconds, and updates all information.
 * If connection is lost, it shows a dialog on top of the screen.
 */
public class DeviceDialog extends JFrame {

	private class RecalcThread extends Thread {

		private boolean started = true;

		public synchronized boolean isStarted() {
			return started;
		}

		public void run() {
			long oldtime = System.nanoTime();
			while (isStarted()) {
				oldtime = System.nanoTime();
				resetValues();
				setEnabledItems();
				pack();
				revalidate();
				repaint();
				for (int i = 0; i < 20; i++) {
					if (System.nanoTime() - oldtime > 2000000000.)
						break;
					if (isStarted() == false)
						return;
					if (ams.getParameterFromDevice(AmsDevice.PAR_DEVICEID) == null) {
						connectionLost(AmsDevice.PAR_DEVICEID);
						if (isGivenUp())
							return;
					}
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

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AmsDevice ams;
	private JPanel leftPanel;
	private Container middlePanel;
	DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	DecimalFormat onePlace = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	private RecalcThread recalcThread;
	private JLabelAntialiased devlab;
	private JLabelAntialiased freqlab;
	private JLabelAntialiased cflab;
	private JLabelAntialiased vallab;
	private JPanel rightPanel;
	private Font fnt;
	private boolean givenUp = false;
	private JButton startStopButton;
	private Long state;
	private JButton setParBut;
	private JButton onlinebut;
	private JButton setdevtimebut;
	private JButton setwarningsbut;
	private Long version;
	private int mot3rawAvailable;
	private JButton startOptBut;
	private JButton setChanBut;
	@SuppressWarnings("rawtypes")
	private JComboBox ecgCB, dzCB, z0CB, sclCB, motCB, pcgCB, batCB, motRawCB;
	private JButton changeStaticsBut;
	private int SCLCalcLevel;
	private GregorianCalendar devTime;

	private static final String[] highFreqOpts = { "Off", "250 Hz", "500 Hz", "1000 Hz" };

	private static final String[] medHighFreqOpts = { "Off", "100 Hz", "200 Hz", "250 Hz", "500 Hz", "1000 Hz" };

	private static final String[] lowMedFreqOpts = { "Off", "1 Hz", "10 Hz", "100 Hz", "200 Hz", "250 Hz" };

	private static final String[] lowHighFreqOpts = { "Off", "1 Hz", "10 Hz", "100 Hz", "250 Hz", "500 Hz", "1000 Hz" };

	private static final String[] SCLFreqOpts = { "Off", "10 Hz" };

	private static final String[] veryLowFreqOpts = { "Off", "1/30 Hz", "1/20 Hz", "1/15 Hz", "1/10 Hz", "1 Hz" };

	private static final String[] veryLowFreqOpts2 = { "Off", "1/30 Hz", "1/10 Hz", "1 Hz" };

	private static final double BAT_WARN_VOLTAGE = 2.4;

	public DeviceDialog(AmsDevice amsDevice) {
		// super("AMS Device " +
		// amsDevice.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SERIALNR)+"
		// (connected via " + (amsDevice.getsPort()==null ? "Bluetooth" :
		// (amsDevice.getsPort().getName().replaceAll("//./", "")))+")" );
		super("AMS Device " + amsDevice.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SERIALNR)
				+ "      (connected via " + (amsDevice.getsPort() == null ? "Bluetooth"
						: (amsDevice.getsPort().getSystemPortName().replaceAll("//./", "")))
				+ ")");
		fnt = new Font("Arial", Font.PLAIN, 20);
		this.ams = amsDevice;
		ams.getStaticsFromDevice();
		try {
			Thread.sleep(50);
		} catch (Exception e) {

		}
		ams.getSettingsFromDevice();
		leftPanel = new JPanel(new BorderLayout());
		middlePanel = new JPanel(new BorderLayout());
		rightPanel = new JPanel(new BorderLayout());
		setupDeviceLabel();
		setupFrequencyPanel();
		setupValuesPanel();
		setupCFPanel();
		setupButtons();
		this.setLayout(new BorderLayout());
		this.add(leftPanel, BorderLayout.WEST);
		this.add(middlePanel, BorderLayout.CENTER);
		this.add(rightPanel, BorderLayout.EAST);
		this.pack();
		this.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
		startThread();
		setIconImage(new ImageIcon(getClass().getResource("/img/hearmysite.png")).getImage());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				closeDialog();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void changeStatics() {
		if (Utils.askForExpertPassword(true) == false)
			return;
		stopThread();
		final JDialog diag = new JDialog(DeviceDialog.this, "Change statics", true);
		JPanel pan = new JPanel(new GridLayout(0, 2));
		pan.add(new JLabel("Hardware version:"));
		final JFormattedTextField hardwareVersionTF = new JFormattedTextField(ams.getStatics().wHardwareVersion);
		pan.add(hardwareVersionTF);
		pan.add(new JLabel("Serial number:"));
		final JFormattedTextField serialNumberTF = new JFormattedTextField(ams.getStatics().wSerialnr);
		pan.add(serialNumberTF);

		final JCheckBox motCB = new JCheckBox("Motility");
		motCB.setSelected(ams.isChannelSupported("MOT"));
		pan.add(motCB);
		pan.add(new JPanel());

		final JCheckBox pcgCB = new JCheckBox("Phono Cardiogram");
		pcgCB.setSelected(ams.isChannelSupported("PCG"));
		pan.add(pcgCB);
		pan.add(new JPanel());

		final JCheckBox icgCB = new JCheckBox("ICG (dZ and Z0)");
		icgCB.setSelected(ams.isChannelSupported("ICG"));
		pan.add(icgCB);
		pan.add(new JPanel());

		final JCheckBox sclCB = new JCheckBox("Skin Conductance");
		sclCB.setSelected(ams.isChannelSupported("SCL"));
		pan.add(sclCB);
		pan.add(new JPanel());

		SCLCalcLevel = ams.getStatics().wCalSCLdcLevel;
		final JLabel sclLab = new JLabel("SCL DC Offset: " + ams.getStatics().wCalSCLdcLevel);
		pan.add(sclLab);
		JButton calButton = new JButton("Calibrate");
		pan.add(calButton);

		calButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AmsSettings backup = ams.getSettings().deepCopy();
				AmsSettings ns = ams.getSettings().deepCopy();
				ns.dwSettingsFlags &= ~AmsDeviceConstants.AMSII_FLAG_SCLAC;
				ns.channels[AmsDeviceConstants.CH_SCL].dwDivider = 1;
				if (ns.equals(ams.getSettings()) == false) {
					tryToSendSettings(ns);
				}
				if (ams.getSettings().equals(ns) == false) {
					JOptionPane.showMessageDialog(diag, "Error sending settings...", "Error",
							JOptionPane.ERROR_MESSAGE);
					if (ams.getSettings().equals(backup) == false) {
						tryToSendSettings(backup);
					}
					return;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
				long sum = 0;
				long count = 0;
				int minval = Integer.MAX_VALUE;
				int maxval = Integer.MIN_VALUE;
				for (int i = 0; i < 100; i++) {
					Long val = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SCLRAW);
					if (val == null) {
						JOptionPane.showMessageDialog(diag, "Error in device communication...", "Error",
								JOptionPane.ERROR_MESSAGE);
						if (ams.getSettings().equals(backup) == false) {
							tryToSendSettings(backup);
						}
						return;
					} else {
						int value = (int) ((long) val);
						if (val < minval)
							minval = value;
						if (val > maxval)
							maxval = value;
						sum += value;
						count++;
						if (count > 20 && (maxval - minval) / count < 1)
							break;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
						}
					}

				}
				int newVal = 0;
				if (count > 0) {
					newVal = (int) Math.round((double) sum / count);
				}

				if (newVal < 32650) {
					JOptionPane.showMessageDialog(diag, "Value too low! (" + newVal + ")", "Error",
							JOptionPane.ERROR_MESSAGE);
					if (ams.getSettings().equals(backup) == false) {
						tryToSendSettings(backup);
					}
					return;
				} else if (newVal > 32950) {
					JOptionPane.showMessageDialog(diag,
							"Value too high! Make sure SCL plug is disconnected! (" + newVal + ")", "Error",
							JOptionPane.ERROR_MESSAGE);
					if (ams.getSettings().equals(backup) == false) {
						tryToSendSettings(backup);
					}
					return;
				} else if (maxval - minval > 5) {
					JOptionPane.showMessageDialog(diag, "Difference is very big! (" + (maxval - minval) + ")", "Error",
							JOptionPane.ERROR_MESSAGE);
					if (ams.getSettings().equals(backup) == false) {
						tryToSendSettings(backup);
					}
					return;
				} else {
					int retVal = JOptionPane.showConfirmDialog(DeviceDialog.this,
							"Calibration successful, new value: " + newVal + "\nAccept new value?",
							"Accept new value", JOptionPane.YES_NO_OPTION);
					if (retVal == JOptionPane.YES_OPTION) {
						SCLCalcLevel = newVal;
						sclLab.setText("SCL DC Offset: " + SCLCalcLevel);
					}
					if (ams.getSettings().equals(backup) == false) {
						tryToSendSettings(backup);
					}
				}

			}
		});

		pan.add(new JLabel("SCL Mode: "));

		@SuppressWarnings("rawtypes")
		final JComboBox sclModeCOB = new JComboBox(
				new String[] { "DC always", "AC (10Hz) always", "AC, except when ECG is active" });
		sclModeCOB.setSelectedIndex(ams.getStatics().cSclMode);
		pan.add(sclModeCOB);

		JButton sendButton = new JButton("Save");
		sendButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AmsStatics ns = ams.getStatics().deepCopy();
				AmsSettings set = ams.getSettings().deepCopy();
				try {
					hardwareVersionTF.commitEdit();
				} catch (ParseException e1) {
				}
				ns.wHardwareVersion = (Integer) hardwareVersionTF.getValue();

				try {
					serialNumberTF.commitEdit();
				} catch (ParseException e1) {
				}
				ns.wSerialnr = (Integer) serialNumberTF.getValue();

				long mask = 0;
				long flags = 0;

				mask |= ((1 << AmsDeviceConstants.CH_DZ) | (1 << AmsDeviceConstants.CH_Z0));
				if (icgCB.isSelected() == false)
					flags |= ((1 << AmsDeviceConstants.CH_DZ) | (1 << AmsDeviceConstants.CH_Z0));

				mask |= (1 << AmsDeviceConstants.CH_SCL);
				if (sclCB.isSelected() == false)
					flags |= (1 << AmsDeviceConstants.CH_SCL);

				mask |= (1 << AmsDeviceConstants.CH_PCG);
				if (pcgCB.isSelected() == false)
					flags |= (1 << AmsDeviceConstants.CH_PCG);

				mask |= ((1 << AmsDeviceConstants.CH_XMT) | (1 << AmsDeviceConstants.CH_YMT));
				if (motCB.isSelected() == false)
					flags |= ((1 << AmsDeviceConstants.CH_XMT) | (1 << AmsDeviceConstants.CH_YMT));

				ns.dwChannelDisableMask = (ns.dwChannelDisableMask & ~mask) | flags;

				ns.wCalSCLdcLevel = SCLCalcLevel;
				ns.cSclMode = sclModeCOB.getSelectedIndex();
				// ------------------ Change between AC and DC for SCL Channel--------------
				if ((set.dwSettingsFlags & AmsDeviceConstants.AMSII_FLAG_SCLAC) > 0) {
					// AC is on
					if (sclModeCOB.getSelectedIndex() == 0) {
						// When DC is selected
						set.dwSettingsFlags -= AmsDeviceConstants.AMSII_FLAG_SCLAC;
					}
				} else {
					set.dwSettingsFlags += AmsDeviceConstants.AMSII_FLAG_SCLAC;
				}
				tryToSendSettings(set);
				// -------------------------------------------------------------------------
				tryToSendStatics(ns);
				diag.setVisible(false);
			}
		});
		pan.add(sendButton);
		JButton cancButton = new JButton("Cancel");
		pan.add(cancButton);
		cancButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				diag.setVisible(false);
			}
		});

		diag.add(pan);
		diag.pack();
		diag.setLocationRelativeTo(DeviceDialog.this);
		diag.setVisible(true);
		startThread();
	}

	protected void closeDialog() {
		if (!isGivenUp())
			stopThread();
		ams.closeConnection();
		BlueCoveImpl.shutdown();
		DeviceDialog.this.setVisible(false);
		DeviceDialog.this.dispose();
	}

	private Long connectionLost(int type) {
		if (isGivenUp())
			return null;
		int tries = 0;
		Long val = null;
		devTime = null;

		while (val == null && devTime == null) {
			if (type == 5) {
				devTime = ams.getDeviceTime();
			} else {
				val = ams.getParameterFromDevice(type);
			}
			tries++;
			if (tries >= 3) {
				connectionLostConfirmed(type);
				break;
			}
		}
		return val;
	}

	private Long connectionLostConfirmed(int type) {
		final JDialog diag = new JDialog(this, "Connection lost"); // , Dialog.ModalityType.DOCUMENT_MODAL);
		setGivenUp(false);
		diag.setUndecorated(true);
		diag.setLayout(new BorderLayout());
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		String text = "<html><table><tr><td><h1>Connection to the AMS device has been lost!</h1></td></tr><tr><td><h1>The connection will be automatically restored if possible.</h1></td></tr>";
		if (ams.serialConn) {
			text += "<tr><td><h1>Please check the cable!</h1></td></tr></table></html>";
		} else {
			text += "<tr><td><h1>Please restart the device if possible!</h1></td></tr></table></html>";
		}
		JLabelAntialiased lab = new JLabelAntialiased(text);
		lab.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(lab);
		pan.add(new JSeparator(JSeparator.HORIZONTAL));
		JButton giveUpButton = new JButton("Give Up Reconnecting");
		pan.add(giveUpButton);
		giveUpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onlinebut.setEnabled(false);
				setGivenUp(true);
				diag.setVisible(false);
				DeviceDialog.this.closeDialog();
			}
		});
		giveUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		diag.add(pan, BorderLayout.CENTER);
		pan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		diag.pack();
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		while (true) {
			ams.tryToReconnect();
			if (ams.isConnectionOpen()) {
				Long val = null;
				devTime = null;
				if (type == 5) {
					devTime = ams.getDeviceTime();
				} else {
					val = ams.getParameterFromDevice(type);
				}
				if (val != null) {
					ams.getStaticsFromDevice();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
					}
					ams.getSettingsFromDevice();
					diag.setVisible(false);
					return val;
				}
			}
			if (givenUp)
				return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JComboBox createFreqChooser(int channel) {
		switch (channel) {
			case AmsDeviceConstants.CH_ECG:
				return new JComboBox(highFreqOpts);
			case AmsDeviceConstants.CH_DZ:
				return new JComboBox(highFreqOpts);
			case AmsDeviceConstants.CHX_Z0A:
				return new JComboBox(lowMedFreqOpts);
			case AmsDeviceConstants.CHX_SCL:
				return new JComboBox(SCLFreqOpts);
			case AmsDeviceConstants.CHX_MYA:
				return new JComboBox(veryLowFreqOpts);
			case AmsDeviceConstants.CH_PCG:
				return new JComboBox(medHighFreqOpts);
			case AmsDeviceConstants.CHX_BTA:
				return new JComboBox(veryLowFreqOpts2);
			case AmsDeviceConstants.CHX_MYR:
				return new JComboBox(lowHighFreqOpts);
			default:
				return null;
		}
	}

	public String createFreqText(String name, long divider, int bStore) {
		if (divider <= 0 || bStore == 0) {
			return "<tr><td>" + name + ":</td><td>Off</td></tr>";
		} else {
			long sr = 1000 / divider;
			if (sr != 0) {
				return "<tr><td>" + name + ":</td><td>" + sr + " Hz</td></tr>";
			} else {
				sr = divider / 1000;
				return "<tr><td>" + name + ":</td><td>1/" + sr + " Hz</td></tr>";
			}
		}
	}

	private void doStartOrStop() {
		startStopButton.setEnabled(false);
		setParBut.setEnabled(false);
		setdevtimebut.setEnabled(false);
		setwarningsbut.setEnabled(false);
		startOptBut.setEnabled(false);
		setChanBut.setEnabled(false);
		changeStaticsBut.setEnabled(false);
		if (startStopButton.getText().equals("Start")) {
			final JDialog diag = new JDialog(DeviceDialog.this, "Starting", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Starting recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialog.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					stopThread();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					ams.sendCommand(AmsDeviceConstants.AMSII_CMDINDEX_START_RECORDING);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					startThread();
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(true);
		} else if (startStopButton.getText().equals("Stop")) {
			final JDialog diag = new JDialog(DeviceDialog.this, "Stopping", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Stopping recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialog.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					stopThread();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					ams.sendCommand(AmsDeviceConstants.AMSII_CMDINDEX_STOP_RECORDING);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					startThread();
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(true);
		}
	}

	protected synchronized boolean isGivenUp() {
		return givenUp;
	}

	private void resetValues() {
		version = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_VERSION);
		if (version == null) {
			version = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_VERSION);
			if (isGivenUp())
				return;
		}
		state = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_AMSSTATE);
		if (state == null) {
			state = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_AMSSTATE);
			if (isGivenUp())
				return;
		}
		setupDeviceLabelTXT();
		setupFreqTXT();
		setupCFTXT();
		setupValuesTXT();
		// this.repaint();
	}

	private void setChannels() {
		stopThread();
		final JDialog diag = new JDialog(this, "Channel Options", true);
		JPanel pan = new JPanel(new GridLayout(0, 3, 0, 5));
		ecgCB = null;
		dzCB = null;
		z0CB = null;
		sclCB = null;
		motCB = null;
		pcgCB = null;
		batCB = null;
		motRawCB = null;
		if (ams.isChannelSupported("ECG")) {
			pan.add(new JLabel("ECG"));
			pan.add(new JLabel());
			ecgCB = createFreqChooser(AmsDeviceConstants.CH_ECG);
			ecgCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_ECG));
			ecgCB.setEnabled(false);
			pan.add(ecgCB);
		}

		if (ams.isChannelSupported("ICG")) {
			pan.add(new JLabel("ICG (dZ)"));
			pan.add(new JLabel());
			dzCB = createFreqChooser(AmsDeviceConstants.CH_DZ);
			dzCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_DZ));
			dzCB.setEnabled(false);
			pan.add(dzCB);

			pan.add(new JLabel("ICG (Z0)"));
			pan.add(new JLabel());
			z0CB = createFreqChooser(AmsDeviceConstants.CHX_Z0A);
			z0CB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_Z0A));
			z0CB.setEnabled(false);
			pan.add(z0CB);
		}

		if (ams.isChannelSupported("SCL")) {
			pan.add(new JLabel("SCL"));
			pan.add(new JLabel());
			sclCB = createFreqChooser(AmsDeviceConstants.CHX_SCL);
			sclCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_SCL));
			sclCB.setEnabled(false);
			pan.add(sclCB);
		}

		if (ams.isChannelSupported("MOT")) {
			pan.add(new JLabel("Motility"));
			pan.add(new JLabel());
			motCB = createFreqChooser(AmsDeviceConstants.CHX_MYA);
			motCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_MYA));
			motCB.setEnabled(false);
			pan.add(motCB);
		}

		if (ams.isChannelSupported("PCG")) {
			pan.add(new JLabel("PCG"));
			pan.add(new JLabel());
			pcgCB = createFreqChooser(AmsDeviceConstants.CH_PCG);
			pcgCB.setEnabled(false);
			pcgCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_PCG));
			pan.add(pcgCB);
		}

		pan.add(new JLabel("Battery Voltage"));
		pan.add(new JLabel());
		batCB = createFreqChooser(AmsDeviceConstants.CHX_BTA);
		batCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_BTA));
		batCB.setEnabled(false);
		pan.add(batCB);

		if (mot3rawAvailable == 1) {
			pan.add(new JLabel("Motility XY Raw"));
			pan.add(new JLabel());
			motRawCB = createFreqChooser(AmsDeviceConstants.CHX_MYR);
			motRawCB.setEnabled(false);
			motRawCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_MYR));
			pan.add(motRawCB);
		} else if (mot3rawAvailable == 2) {
			pan.add(new JLabel("Motility XYZ Raw"));
			pan.add(new JLabel());
			motRawCB = createFreqChooser(AmsDeviceConstants.CHX_MYR);
			motRawCB.setEnabled(false);
			motRawCB.setSelectedItem(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_MYR));
			pan.add(motRawCB);
		}

		JPanel pan2 = new JPanel();
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isChanged = false;
				AmsSettings set = ams.getSettings().deepCopy();
				if (ecgCB != null) {
					if (ecgCB.getSelectedItem().equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_ECG)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CH_ECG,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(ecgCB.getSelectedItem()));
					}
				}
				if (dzCB != null) {
					if (dzCB.getSelectedItem().equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_DZ)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CH_DZ,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(dzCB.getSelectedItem()));
					}
				}
				if (z0CB != null) {
					if (z0CB.getSelectedItem().equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_Z0A)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CHX_Z0A,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(z0CB.getSelectedItem()));
					}
				}
				if (sclCB != null) {
					if (sclCB.getSelectedItem()
							.equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_SCL)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CHX_SCL,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(sclCB.getSelectedItem()));
					}
				}
				if (motCB != null) {
					if (motCB.getSelectedItem()
							.equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_MYA)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CHX_MYA,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(motCB.getSelectedItem()));
					}
				}
				if (pcgCB != null) {
					if (pcgCB.getSelectedItem().equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CH_PCG)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CH_PCG,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(pcgCB.getSelectedItem()));
					}
				}
				if (batCB != null) {
					if (batCB.getSelectedItem()
							.equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_BTA)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CHX_BTA,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(batCB.getSelectedItem()));
					}
				}
				if (motRawCB != null) {
					if (motRawCB.getSelectedItem()
							.equals(ams.getFrequencyOfChannel(AmsDeviceConstants.CHX_MYR)) == false) {
						isChanged = true;
						set.setDivider(AmsDeviceConstants.CHX_MYR,
								AmsDeviceConstants.REVERSEDIVIDERMAP.get(motRawCB.getSelectedItem()));
					}
				}
				if (mot3rawAvailable == 1) {
					// JdH 20150119 force MZR off for old devices
					isChanged = true;
					set.setDivider(AmsDeviceConstants.CH_Xt1, 0);
				}
				diag.setVisible(false);
				if (isChanged) {
					tryToSendSettings(set);
				}
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

		JPanel pan3 = new JPanel();

		File curFile = new File(System.getProperty("user.dir"), "DoNotWarnMeAboutChannels.txt");
		if (curFile.exists()) {
			ecgCB.setEnabled(true);
			dzCB.setEnabled(true);
			z0CB.setEnabled(true);
			sclCB.setEnabled(true);
			motCB.setEnabled(true);
			if (ams.isChannelSupported("PCG")) {
				pcgCB.setEnabled(true);
			}
			batCB.setEnabled(true);
			motRawCB.setEnabled(true);
		} else {

			final JCheckBox ad = new JCheckBox("Advanced Settings");
			ad.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (ad.isSelected()) {
						JOptionPane.showMessageDialog(DeviceDialog.this,
								"It is not advised to change the Sampling Frequency for ECG and ICG Signals");
						ecgCB.setEnabled(true);
						dzCB.setEnabled(true);
						z0CB.setEnabled(true);
						sclCB.setEnabled(true);
						motCB.setEnabled(true);
						if (ams.isChannelSupported("PCG")) {
							pcgCB.setEnabled(true);
						}
						batCB.setEnabled(true);
						motRawCB.setEnabled(true);
					}
				}

			});
			pan3.add(ad);
		}
		diag.setLayout(new BorderLayout());
		diag.add(pan, BorderLayout.NORTH);
		diag.add(pan2, BorderLayout.CENTER);
		diag.add(pan3, BorderLayout.SOUTH);
		diag.pack();
		diag.getRootPane().setDefaultButton(saveButton);// ENTER will hit button Save
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		startThread();
	}

	private void setEnabledItems() {
		boolean unlocked = ams.isUnlocked();
		setParBut.setEnabled(unlocked);
		setdevtimebut.setEnabled(unlocked);
		setwarningsbut.setEnabled(unlocked);
		startOptBut.setEnabled(unlocked);
		setChanBut.setEnabled(unlocked);
		changeStaticsBut.setEnabled(unlocked);

		mot3rawAvailable = 0;
		if (version != null) {
			if (version > 67333) {
				mot3rawAvailable = 2;
				if (ams.getStatics().wHardwareVersion % 10000 >= 40
						&& ams.getStatics().wHardwareVersion % 10000 <= 41) {
					mot3rawAvailable = 1;
				} else if (ams.getStatics().wHardwareVersion % 10000 >= 510
						&& ams.getStatics().wHardwareVersion % 10000 < 550) {
					mot3rawAvailable = 1;
				}
			}
			if (version < 7) {
				startOptBut.setVisible(false);
			} else {
				startOptBut.setVisible(true);
			}
		} else {
			startOptBut.setVisible(true);
		}
		setupFreqTXT();

		startStopButton.setEnabled(false);
		startStopButton.setText("N/A");
		if (state != null) {
			if (state == AmsDeviceConstants.VUAMSII_DEVSTATE_WAITFORSTART) {
				startStopButton.setText("Start");
				startStopButton.setEnabled(true);
			} else if (state == AmsDeviceConstants.VUAMSII_DEVSTATE_RECORDING) {
				startStopButton.setText("Stop");
				startStopButton.setEnabled(true);
			}
		}
		if (version == null || version < 3) {
			startStopButton.setEnabled(false);
		}
	}

	protected synchronized void setGivenUp(boolean b) {
		givenUp = b;
	}

	private void setParameters() {
		stopThread();
		final JDialog diag = new JDialog(this, "Set Parameters", true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Recording ID:"), BorderLayout.WEST);
		String sId = "";
		for (int i = 0; i < 12; i++) {
			if (ams.getSettings().szSubjectID[i] == 0)
				break;
			sId += (char) ams.getSettings().szSubjectID[i];
		}
		final JTextField tf = new JTextField(12);
		tf.setColumns(12);
		tf.setText(sId);
		pan.add(tf, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("ICG-V Distance (mm):"), BorderLayout.WEST);
		final JFormattedTextField ftf = new JFormattedTextField(Integer.valueOf(ams.getSettings().wElectrodeDistance));
		ftf.setColumns(12);
		pan.add(ftf, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();

		JButton but = new JButton("Save");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String idText = tf.getText();
				if (idText.length() > 12) {
					JOptionPane.showMessageDialog(diag, "The subject ID can not contain more than 12 characters!",
							"Subject ID too long",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					ftf.commitEdit();
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
				int icgVmm = (Integer) ftf.getValue();
				Object[] options = { "Yes", "No" };
				if (icgVmm < 0) {
					JOptionPane.showMessageDialog(diag, "The ICG-V distance can not be negative!",
							"Negative ICG-V distance", JOptionPane.ERROR_MESSAGE);
					return;
				} else if (icgVmm < 50 || icgVmm > 600) {
					int r = JOptionPane.showOptionDialog(diag, "You have entered " + icgVmm
							+ " millimeters as the distance between the two front electrodes. This is an implausible value, are you sure you want to proceed?",
							"Warning", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (r != 0)
						return;
				}
				AmsSettings newSet = ams.getSettings().deepCopy();
				for (int i = 0; i < 12; i++) {
					if (i < idText.length()) {
						newSet.szSubjectID[i] = idText.charAt(i);
					} else {
						newSet.szSubjectID[i] = 0;
					}
				}
				newSet.wElectrodeDistance = icgVmm;
				diag.setVisible(false);
				if (ams.getSettings().equals(newSet) == false) {
					tryToSendSettings(newSet);
				}

			}
		});
		pan.add(but);

		JButton butCancel = new JButton("Cancel");
		butCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		pan.add(butCancel);

		diag.add(pan);

		diag.pack();
		diag.setLocationRelativeTo(DeviceDialog.this);
		diag.getRootPane().setDefaultButton(but);// ENTER will hit button Save

		diag.setVisible(true);
		startThread();
	}

	private void setStartOptions() {
		stopThread();
		final JDialog diag = new JDialog(this, "Start Options", true);
		JPanel pan = new JPanel(new GridLayout(0, 1));
		final JCheckBox resetCB = new JCheckBox(
				"<html>Automatically start new file after file is stopped<br>(or after accidental reset while device is recording)</html>");
		pan.add(resetCB);
		final boolean oldReset = (ams.getSettings().dwSettingsFlags
				& AmsDeviceConstants.AMSII_FLAG_AUTOSTARTSTOPRESET) != 0;
		resetCB.setSelected(oldReset);
		final JCheckBox startButCB = new JCheckBox("Enable Start recording by Button press");
		pan.add(startButCB);
		final boolean oldStartBut = (ams.getSettings().dwSettingsFlags
				& AmsDeviceConstants.AMSII_FLAG_STARTBUTTON) == 0;
		startButCB.setSelected(oldStartBut);
		final JCheckBox stopButCB = new JCheckBox("Enable Stop recording by Button press");
		final boolean oldStopBut = (ams.getSettings().dwSettingsFlags & AmsDeviceConstants.AMSII_FLAG_STOPBUTTON) == 0;
		stopButCB.setSelected(oldStopBut);
		pan.add(stopButCB);
		JPanel pan2 = new JPanel();
		JButton saveButton = new JButton("Save");
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AmsSettings set = ams.getSettings().deepCopy();
				boolean isChanged = false;
				if (oldReset != resetCB.isSelected()) {
					isChanged = true;
					if (resetCB.isSelected()) {
						set.dwSettingsFlags += AmsDeviceConstants.AMSII_FLAG_AUTOSTARTSTOPRESET;
					} else {
						set.dwSettingsFlags -= AmsDeviceConstants.AMSII_FLAG_AUTOSTARTSTOPRESET;
					}
				}
				if (oldStartBut != startButCB.isSelected()) {
					isChanged = true;
					if (startButCB.isSelected()) {
						set.dwSettingsFlags -= AmsDeviceConstants.AMSII_FLAG_STARTBUTTON;
					} else {
						set.dwSettingsFlags += AmsDeviceConstants.AMSII_FLAG_STARTBUTTON;
					}
				}
				if (oldStopBut != stopButCB.isSelected()) {
					isChanged = true;
					if (stopButCB.isSelected()) {
						set.dwSettingsFlags -= AmsDeviceConstants.AMSII_FLAG_STOPBUTTON;
					} else {
						set.dwSettingsFlags += AmsDeviceConstants.AMSII_FLAG_STOPBUTTON;
					}
				}
				if (isChanged) {
					tryToSendSettings(set);
				}
				diag.setVisible(false);
			}
		});
		pan2.add(saveButton);
		pan2.add(cancelButton);
		pan.add(pan2);
		diag.add(pan);
		diag.pack();
		diag.getRootPane().setDefaultButton(saveButton);// ENTER will hit button Save
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		startThread();
	}

	private void setupButtons() {
		JPanel newPanel = new JPanel();
		newPanel.setLayout(new GridLayout(0, 1, 0, 5));
		rightPanel.setLayout(new BorderLayout());

		startStopButton = new JButton("N/A");
		startStopButton.setEnabled(false);
		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doStartOrStop();
			}
		});
		newPanel.add(startStopButton);

		newPanel.add(new JPanel());

		setParBut = new JButton("Set Parameters");
		setParBut.setEnabled(false);
		setParBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setParameters();
			}
		});
		newPanel.add(setParBut);

		setChanBut = new JButton("Set Channels");
		setChanBut.setEnabled(false);
		setChanBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChannels();
			}
		});
		newPanel.add(setChanBut);
		setdevtimebut = new JButton("Set Device Time To Computer Time");
		setdevtimebut.setEnabled(false);
		setdevtimebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopThread();
				ams.setDeviceTimeToNow();
				startThread();
			}
		});
		newPanel.add(setdevtimebut);

		onlinebut = new JButton("Online Graph");
		onlinebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopThread();
				OnlineDialog diag = new OnlineDialog(ams, DeviceDialog.this);
				diag.setVisible(true);
			}
		});
		newPanel.add(new JPanel());
		newPanel.add(onlinebut);
		newPanel.add(new JPanel());

		setwarningsbut = new JButton("Set Warnings");
		setwarningsbut.setEnabled(false);
		setwarningsbut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setWarnings();
			}
		});
		newPanel.add(setwarningsbut);

		startOptBut = new JButton("Set Start Options");
		startOptBut.setEnabled(false);
		startOptBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setStartOptions();
			}
		});
		newPanel.add(startOptBut);

		newPanel.add(new JPanel());
		changeStaticsBut = new JButton("Edit Config");
		changeStaticsBut.setEnabled(false);
		changeStaticsBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				changeStatics();
			}
		});
		newPanel.add(changeStaticsBut);

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

	private void setupCFPanel() {
		/*
		 * String text = "<html><body><table>";
		 * double nSectorsPerSecond = 0;
		 * for (int i = 0; i < AmsDeviceConstants.NCHANNELS; i++) {
		 * if (ams.getSettings().channels[i].bStore != 0 &&
		 * ams.getSettings().channels[i].dwDivider != 0 &&
		 * ams.getSettings().channels[i].wBufSize > 0)
		 * nSectorsPerSecond += 2 * 1000.0 / ams.getSettings().channels[i].dwDivider;
		 * }
		 * nSectorsPerSecond *= 3600; // bytes per hour
		 * nSectorsPerSecond /= 512; // sectors per hour //1024*1024; // MB per
		 * // hour
		 * 
		 * String bps = "";
		 * 
		 * if (nSectorsPerSecond > 2.0 * 1024 * 1024)
		 * bps = twoPlaces.format(nSectorsPerSecond / (2.0 * 1024 * 1024)) + " GB";
		 * else
		 * bps = twoPlaces.format(nSectorsPerSecond / (2.0 * 1024)) + " MB";
		 * 
		 * text += "<tr><td>Estimated memory usage per hour:</td><td>" + bps +
		 * "</td></tr>";
		 * 
		 * Long nFreeSectors =
		 * ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_NRFREESECTORS);
		 * 
		 * String freebytes = "";
		 * 
		 * if (nFreeSectors > 2.0 * 1024 * 1024)
		 * freebytes = twoPlaces.format(nFreeSectors / (2.0 * 1024 * 1024)) + " GB";
		 * else
		 * freebytes = twoPlaces.format(nFreeSectors / (2.0 * 1024)) + " MB";
		 * 
		 * text += "<tr><td>Memory available:</td><td>" + freebytes + "</td></tr>";
		 * 
		 * text += "<tr><td>Estimated recording time left:</td><td>" +
		 * twoPlaces.format((double) nFreeSectors / nSectorsPerSecond) + " h</td></tr>";
		 * text += "</table></body></html>";
		 */
		cflab = new JLabelAntialiased("");
		cflab.setFont(fnt);
		setupCFTXT();
		;
		cflab.setBorder(BorderFactory.createTitledBorder("CompactFlash Card"));
		middlePanel.add(cflab, BorderLayout.SOUTH);
	}

	private void setupCFTXT() {
		String text = "<html><body><table>";
		double nSectorsPerSecond = 0;
		for (int i = 0; i < 21; i++) {
			if (ams.getSettings().channels[i].bStore != 0 && ams.getSettings().channels[i].dwDivider != 0
					&& ams.getSettings().channels[i].wBufSize > 0)
				nSectorsPerSecond += 2 * 1000.0 / ams.getSettings().channels[i].dwDivider;
		}
		nSectorsPerSecond *= 3600; // bytes per hour
		nSectorsPerSecond /= 512; // sectors per hour //1024*1024; // MB per
									// hour

		String bps = "";

		if (nSectorsPerSecond > 2.0 * 1024 * 1024)
			bps = twoPlaces.format(nSectorsPerSecond / (2.0 * 1024 * 1024)) + " GB";
		else
			bps = twoPlaces.format(nSectorsPerSecond / (2.0 * 1024)) + " MB";

		text += "<tr><td>Estimated memory usage per hour:</td><td>" + bps + "</td></tr>";

		Long nFreeSectors = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_NRFREESECTORS);
		if (nFreeSectors == null) {
			nFreeSectors = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_NRFREESECTORS);
			if (isGivenUp())
				return;
		}

		String freebytes = "";

		if (nFreeSectors > 2.0 * 1024 * 1024)
			freebytes = twoPlaces.format(nFreeSectors / (2.0 * 1024 * 1024)) + " GB";
		else
			freebytes = twoPlaces.format(nFreeSectors / (2.0 * 1024)) + " MB";

		text += "<tr><td>Memory available:</td><td>" + freebytes + "</td></tr>";

		double timeLeft = (double) nFreeSectors / nSectorsPerSecond;
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.CFWARNENABLED) == 0) {
			text += "<tr><td>Estimated recording time left:</td><td>" + twoPlaces.format(timeLeft) + " h</td></tr>";
		} else {
			if (timeLeft < AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.CFWARNHOURS)) {
				text += "<tr><td>Memory space left for:</td><td><font color=red>" + twoPlaces.format(timeLeft)
						+ " h</font></td></tr>";
			} else {
				text += "<tr><td>Memory space left for:</td><td>" + twoPlaces.format(timeLeft) + " h</td></tr>";
			}
		}
		text += "</table></body></html>";
		cflab.setText(text);

	}

	private void setupDeviceLabel() {
		devlab = new JLabelAntialiased("");
		devlab.setFont(fnt);
		setupDeviceLabelTXT();
		devlab.setBorder(BorderFactory.createTitledBorder("VU-AMS Device"));
		leftPanel.add(devlab, BorderLayout.NORTH);
	}

	private void setupDeviceLabelTXT() {
		String text = "<html><body><table>";
		Long par = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SERIALNR);
		if (par == null) {
			par = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_SERIALNR);
			if (isGivenUp())
				return;
		}
		text += "<tr><td>Serial Number:</td><td>" + par + "</td></tr>";
		Long version = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_VERSION);
		if (version == null) {
			version = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_VERSION);
			if (isGivenUp())
				return;
		}
		text += "<tr><td>Firmware Version:</td><td>" + +((version & 0xFF0000) >> 16) + "." + ((version & 0xFF00) >> 8)
				+ "." + (version & 0xFF) + "</td></tr>";

		int hwversion = ams.getStatics().wHardwareVersion;
		text += "<tr><td>Hardware Version:</td><td>" + hwversion + "</td></tr>";

		Long amsState = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_AMSSTATE);
		if (amsState == null) {
			amsState = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_AMSSTATE);
			if (isGivenUp())
				return;
		}

		text += "<tr><td>Device State:</td><td>";
		if (amsState == 3) {
			text += "Idle";
		} else if (amsState == 2) {
			text += "Close Cover";
		} else if (amsState == 1) {
			text += "No Memory";
		} else if (amsState == 5) {
			text += "Memory Full";
		} else if (amsState == 4) {
			text += "Recording";
		} else if (amsState == 6) {
			text += "Battery Low";
		}
		text += "</td></tr>";
		devTime = ams.getDeviceTime();
		if (devTime == null) {
			connectionLost(5);
			// devTime = ams.getDeviceTime();
			if (isGivenUp())
				return;
		}
		text += "<tr><td>Device Time:</td><td>";
		if (devTime != null) {
			long diffTime = Math.abs(devTime.getTimeInMillis() - GregorianCalendar.getInstance().getTimeInMillis());
			boolean isWarning = false;
			if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNINGENABLED) != 0) {
				if (diffTime > 1000. * AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNING)) {
					isWarning = true;
				}
			}
			if (isWarning)
				text += "<font color=red>";
			text += Utils.getDateAndTimeFromCal(devTime);
			if (isWarning)
				text += "</font>";
		}

		text += "</td></tr></table></body></html>";
		devlab.setText(text);
	}

	private void setupFreqTXT() {
		String text = "<html><body><table>";
		AmsChannelInfo chan;
		if (ams.isChannelSupported("ECG")) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CH_ECG);
			text += createFreqText("ECG", chan.getDivider(), chan.bStore);
		}
		if (ams.isChannelSupported("ICG")) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CH_DZ);
			text += createFreqText("ICG (dZ)", chan.getDivider(), chan.bStore);
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_Z0A);
			text += createFreqText("ICG (Z0)", chan.getDivider(), chan.bStore);
		}
		if (ams.isChannelSupported("SCL")) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_SCL);
			if (ams.getStatics().wCalSCLdcLevel == 0) {
				text += "<font color=red>";
				text += createFreqText("SCL", chan.getDivider(), chan.bStore);
				text += "</font>";
			} else
				text += createFreqText("SCL", chan.getDivider(), chan.bStore);
		}
		if (ams.isChannelSupported("MOT")) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_MYA);
			text += createFreqText("Motility", chan.getDivider(), chan.bStore);
		}
		if (ams.isChannelSupported("PCG")) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CH_PCG);
			text += createFreqText("PCG", chan.getDivider(), chan.bStore);
		}
		chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_BTA);
		text += createFreqText("Battery Voltage", chan.getDivider(), chan.bStore);
		if (mot3rawAvailable == 2) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_MYR);
			text += createFreqText("Motility XYZ Raw", chan.getDivider(), chan.bStore);
		} else if (mot3rawAvailable == 1) {
			chan = ams.getSettings().GetChannel(AmsDeviceConstants.CHX_MYR);
			text += createFreqText("Motility XY Raw", chan.getDivider(), chan.bStore);
		} else // JdH to prevent ugly jumping and overlapping of panels when just connected
			text += "<tr><td>Motility XYZ Raw:</td><td> reading...</td></tr>";

		text += "</table></body></html>";
		freqlab.setText(text);
	}

	private void setupFrequencyPanel() {
		freqlab = new JLabelAntialiased("");
		setupFreqTXT();
		freqlab.setFont(fnt);
		freqlab.setBorder(BorderFactory.createTitledBorder("Sampling Frequencies"));
		leftPanel.add(freqlab, BorderLayout.SOUTH);
	}

	private void setupValuesPanel() {

		/*
		 * String text = "<html><body><table>";
		 * text += "<tr><td>Recording Identification:</td><td>";
		 * for (int i = 0; i < 12; i++) {
		 * if (ams.getSettings().szSubjectID[i] == 0)
		 * break;
		 * text += (char) ams.getSettings().szSubjectID[i];
		 * }
		 * text += "</td></tr>";
		 * AmsChannelInfo batChan =
		 * ams.getSettings().channels[AmsDeviceConstants.CHX_BTA];
		 * long batval =
		 * ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_BATRAW);
		 * double batrel = (double) batval / (1 << batChan.nBits);
		 * // double highrel = (double) 3 / (batChan.lMaxValue -
		 * // batChan.lMinValue);
		 * // double lowrel = (double) 2 / (batChan.lMaxValue - batChan.lMinValue);
		 * double batVolt = batChan.lMinValue + batrel * (batChan.lMaxValue -
		 * batChan.lMinValue);
		 * text += "<tr><td>Battery Voltage:</td><td>" + twoPlaces.format(batVolt) +
		 * " V</td></tr>";
		 * //text += "<tr><td>Battery Type: <td>Alkaline";
		 * text += "<tr><td>ICG-V Distance (mm):</td><td>" +
		 * ams.getSettings().wElectrodeDistance + "</td></tr>";
		 * 
		 * long z0avg_value =
		 * ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_Z0_AVG);
		 * AmsChannelInfo z0Chan =
		 * ams.getSettings().channels[AmsDeviceConstants.CHX_Z0A];
		 * double z0rel = (double) z0avg_value / (1 << z0Chan.nBits);
		 * double z0Ohm = z0Chan.lMinValue + z0rel * (z0Chan.lMaxValue -
		 * z0Chan.lMinValue);
		 * text += "<tr><td>Thorax Impedance (Ohm):</td><td>" + twoPlaces.format(z0Ohm)
		 * + "</td></tr>";
		 * 
		 * long sclavg_value =
		 * ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SCL_FILT);
		 * AmsChannelInfo sclChan =
		 * ams.getSettings().channels[AmsDeviceConstants.CHX_SCL];
		 * double sclrel = (double) sclavg_value / (1 << sclChan.nBits);
		 * double scluS = sclChan.lMinValue + sclrel * (sclChan.lMaxValue -
		 * sclChan.lMinValue);
		 * text += "<tr><td>Skin Conductance (uS):</td><td>" + onePlace.format(scluS) +
		 * "</td></tr>";
		 * 
		 * long nSamplesInSum =
		 * ams.getSettings().channels[AmsDeviceConstants.CHX_MYA].dwDivider;
		 * long t =
		 * ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_YMT_AC);
		 * long shift = t & 0x0F;
		 * t = (t >> 4);
		 * t = (t << shift);
		 * 
		 * long motavg_value = t;
		 * if (nSamplesInSum == 0) {
		 * motavg_value = 0;
		 * } else {
		 * motavg_value /= nSamplesInSum;
		 * }
		 * AmsChannelInfo motChan =
		 * ams.getSettings().channels[AmsDeviceConstants.CHX_MYA];
		 * double motrel = (double) motavg_value / (1 << motChan.nBits);
		 * double motg = motChan.lMinValue + motrel * (motChan.lMaxValue -
		 * motChan.lMinValue);
		 * text += "<tr><td>Motility (g):</td><td>" + twoPlaces.format(motg) +
		 * "</td></tr>";
		 * 
		 * text += "</table></body></html>";
		 */
		vallab = new JLabelAntialiased("");
		vallab.setFont(fnt);
		setupValuesTXT();
		vallab.setBorder(BorderFactory.createTitledBorder("Values"));
		middlePanel.add(vallab, BorderLayout.NORTH);
	}

	private void setupValuesTXT() {
		String text = "<html><body><table>";
		text += "<tr><td>Recording Identification:</td><td>";
		for (int i = 0; i < 12; i++) {
			if (ams.getSettings().szSubjectID[i] == 0)
				break;
			text += (char) ams.getSettings().szSubjectID[i];
		}
		text += "</td></tr>";
		AmsChannelInfo batChan = ams.getSettings().channels[AmsDeviceConstants.CHX_BTA];
		Long batval = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_BATRAW);
		if (batval == null) {
			batval = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_BATRAW);
			if (isGivenUp())
				return;
		}
		double batrel = (double) batval / (1 << batChan.nBits);
		// double highrel = (double) 3 / (batChan.lMaxValue -
		// batChan.lMinValue);
		// double lowrel = (double) 2 / (batChan.lMaxValue - batChan.lMinValue);
		double batVolt = batChan.lMinValue + batrel * (batChan.lMaxValue - batChan.lMinValue);
		if (batVolt < BAT_WARN_VOLTAGE) {
			text += "<tr><td><font color=red>Battery Voltage:</font></td><td>";
		} else
			text += "<tr><td>Battery Voltage:</td><td>";
		if (batVolt < BAT_WARN_VOLTAGE) {
			text += "<font color=red>";
		}
		text += twoPlaces.format(batVolt) + " V";
		if (batVolt < BAT_WARN_VOLTAGE) {
			text += "</font>";
		}
		text += "</td></tr>";
		text += "<tr><td>ICG-V Distance (mm):</td><td>" + ams.getSettings().wElectrodeDistance + "</td></tr>";

		AmsChannelInfo z0Chan = ams.getSettings().channels[AmsDeviceConstants.CHX_Z0A];
		if (z0Chan.getDivider() == 0 || z0Chan.bStore == 0)// See if z0 is Off
			text += "<tr><td>Thorax Impedance (\u2126):</td><td>Off</td></tr>";
		else {
			Long z0avg_value = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_Z0_AVG);
			if (z0avg_value == null) {
				z0avg_value = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_Z0_AVG);
				if (isGivenUp())
					return;
			}
			double z0rel = (double) z0avg_value / (1 << z0Chan.nBits);
			double z0Ohm = z0Chan.lMinValue + z0rel * (z0Chan.lMaxValue - z0Chan.lMinValue);

			Long Ires_value = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_IRES);
			if ((Ires_value < ams.getSettings().wIresThresholdMax & Ires_value > ams.getSettings().wIresThresholdMin)
					|| Ires_value == -1)
				if (z0avg_value < ams.getSettings().wZ0ThresholdMax & z0avg_value > ams.getSettings().wZ0ThresholdMin)
					text += "<tr><td>Thorax Impedance (\u2126):</td><td>" + twoPlaces.format(z0Ohm) + "</td></tr>";
				else
					text += "<tr><td><font color=red>ICG yellow front electrode(s)</font></td><td><font color=red> problem</font></td></tr>";
			else
				text += "<tr><td><font color=red>ICG blue back electrode(s)</font></td><td><font color=red> problem</font></td></tr>";
		}

		AmsChannelInfo sclChan = ams.getSettings().channels[AmsDeviceConstants.CHX_SCL];
		if (sclChan.getDivider() == 0 || sclChan.bStore == 0)// See if SCL is Off
			text += "<tr><td>Skin Conductance (\u00B5S):</td><td>Off</td></tr>";
		else {
			Long sclavg_value = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SCL_FILT);
			if (sclavg_value == null) {
				sclavg_value = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_SCL_FILT);
				if (isGivenUp())
					return;
			}
			double sclrel = (double) sclavg_value / (1 << sclChan.nBits);
			double scluS = sclChan.lMinValue + sclrel * (sclChan.lMaxValue - sclChan.lMinValue);
			// Check if SCL is within range
			if (sclavg_value < ams.getSettings().wSCLThresholdMax & sclavg_value > ams.getSettings().wSCLThresholdMin)
				text += "<tr><td>Skin Conductance (\u00B5S):</td><td>" + onePlace.format(scluS) + "</td></tr>";
			else
				text += "<tr><td>Skin Conductance (\u00B5S):</td><td><font color=red>" + onePlace.format(scluS)
						+ "</font></td></tr>";
		}

		long nSamplesInSum = ams.getSettings().channels[AmsDeviceConstants.CHX_MYA].dwDivider;
		Long t = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_YMT_AC);
		if (t == null) {
			t = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_YMT_AC);
			if (isGivenUp())
				return;
		}
		long shift = t & 0x0F;
		t = (t >> 4);
		t = (t << shift);
		long motavg_value = t;
		if (nSamplesInSum == 0) {
			motavg_value = 0;
		} else {
			motavg_value /= nSamplesInSum;
		}
		AmsChannelInfo motChan = ams.getSettings().channels[AmsDeviceConstants.CHX_MYA];
		double motrel = (double) motavg_value / (1 << motChan.nBits);
		double motg = motChan.lMinValue + motrel * (motChan.lMaxValue - motChan.lMinValue);
		text += "<tr><td>Motility (g):</td><td>" + twoPlaces.format(motg) + "</td></tr>";

		AmsChannelInfo ecgChan = ams.getSettings().channels[AmsDeviceConstants.CH_ECG];
		if (ecgChan.getDivider() == 0 || ecgChan.bStore == 0)// See if ECG is Off
			;// do nothing
		else {
			Long ecg_value = ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_ECG);
			if (ecg_value == null) {
				ecg_value = connectionLost(AmsDeviceConstants.AMSII_PARMINDEX_YMT_AC);
				if (isGivenUp())
					return;
			}
			// Check if ECG is within range
			if (ecg_value < ams.getSettings().wECGThresholdMax & ecg_value > ams.getSettings().wECGThresholdMin)
				;// do nothing
			else
				text += "<tr><td><font color=red>ECG out of range >> ECG</font></td><td><font color=red>electrode(s) problem</font></td></tr>";
		}

		text += "</table></body></html>";
		vallab.setText(text);
	}

	private void setWarnings() {
		stopThread();
		final AmsSettings set = ams.getSettings().deepCopy();
		final boolean ecgRangeEnabled;
		if ((set.dwSettingsFlags & 0x00000400) > 0) {
			ecgRangeEnabled = false;
		} else {
			ecgRangeEnabled = true;
		}
		final boolean icgRangeEnabled;
		if ((set.dwSettingsFlags & 0x00000800) > 0) {
			icgRangeEnabled = false;
		} else {
			icgRangeEnabled = true;
		}
		final boolean sclRangeEnabled;
		if ((set.dwSettingsFlags & 0x00001000) > 0) {
			sclRangeEnabled = false;
		} else {
			sclRangeEnabled = true;
		}
		final boolean ecgBeepEnabled;
		if ((set.dwSettingsFlags & 0x00000080) > 0) {
			ecgBeepEnabled = false;
		} else {
			ecgBeepEnabled = true;
		}
		final boolean icgBeepEnabled;
		if ((set.dwSettingsFlags & 0x00000100) > 0) {
			icgBeepEnabled = false;
		} else {
			icgBeepEnabled = true;
		}
		final boolean sclBeepEnabled;
		if ((set.dwSettingsFlags & 0x00000200) > 0) {
			sclBeepEnabled = false;
		} else {
			sclBeepEnabled = true;
		}
		final int ecgL = set.wECGThresholdMin;
		final int ecgH = set.wECGThresholdMax;
		AmsChannelInfo ecgChan = set.channels[AmsDeviceConstants.CH_ECG];
		int lB = 0;
		int uB = (1 << ecgChan.nBits) - 1;
		double lV = (double) ecgChan.lMinValue / ecgChan.lMinMaxDivider;
		double uV = (double) ecgChan.lMaxValue / ecgChan.lMinMaxDivider;
		final double ecgrealSlope = (uV - lV) / (uB - lB);
		final double ecgrealConstant = lV - ecgrealSlope * lB;

		final int icgL = set.wZ0ThresholdMin;
		final int icgH = set.wZ0ThresholdMax;
		AmsChannelInfo z0Chan = set.channels[AmsDeviceConstants.CHX_Z0A];
		lB = 0;
		uB = (1 << z0Chan.nBits) - 1;
		lV = (double) z0Chan.lMinValue / z0Chan.lMinMaxDivider;
		uV = (double) z0Chan.lMaxValue / z0Chan.lMinMaxDivider;
		final double icgrealSlope = (uV - lV) / (uB - lB);
		final double icgrealConstant = lV - icgrealSlope * lB;

		final int sclL = set.wSCLThresholdMin;
		final int sclH = set.wSCLThresholdMax;
		AmsChannelInfo sclChan = set.channels[AmsDeviceConstants.CHX_SCL];
		lB = 0;
		uB = (1 << sclChan.nBits) - 1;
		lV = (double) sclChan.lMinValue / sclChan.lMinMaxDivider;
		uV = (double) sclChan.lMaxValue / sclChan.lMinMaxDivider;
		final double sclrealSlope = (uV - lV) / (uB - lB);
		final double sclrealConstant = lV - sclrealSlope * lB;

		final JDialog diag = new JDialog(this, "Set Warnings", true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		JPanel ecgPanel = new JPanel();
		JLabelAntialiased ecgOff = new JLabelAntialiased("     ");
		if (ecgChan.getDivider() == 0 || ecgChan.bStore == 0) {// See if ECG is Off
			ecgOff = new JLabelAntialiased("Off");
		}
		ecgOff.setFont(fnt);
		ecgPanel.add(ecgOff, BorderLayout.WEST);

		final JCheckBox ecgEn = new JCheckBox("ECG range check enabled");
		ecgEn.setSelected(ecgRangeEnabled);
		ecgPanel.add(ecgEn);
		final JFormattedTextField ecgLField = new JFormattedTextField(ecgL * ecgrealSlope + ecgrealConstant);
		ecgPanel.add(ecgLField);
		ecgLField.setColumns(5);
		final JFormattedTextField ecgHField = new JFormattedTextField(ecgH * ecgrealSlope + ecgrealConstant);
		ecgPanel.add(ecgHField);
		ecgHField.setColumns(5);
		final JCheckBox ecgBeep = new JCheckBox("beep                              ");
		ecgBeep.setSelected(ecgBeepEnabled);
		ecgPanel.add(ecgBeep);
		diag.add(ecgPanel);

		ActionListener ecgPanelActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ecgLField.setEnabled(ecgEn.isSelected());
				ecgHField.setEnabled(ecgEn.isSelected());
				ecgBeep.setEnabled(ecgEn.isSelected());
			}
		};

		ecgEn.addActionListener(ecgPanelActionListener);
		ecgPanelActionListener.actionPerformed(null);

		JPanel icgPanel = new JPanel();
		JLabelAntialiased icgOff = new JLabelAntialiased("     ");
		if (z0Chan.getDivider() == 0 || z0Chan.bStore == 0) { // See if z0 is Off
			icgOff = new JLabelAntialiased("Off");
		}
		icgOff.setFont(fnt);
		icgPanel.add(icgOff, BorderLayout.WEST);

		final JCheckBox icgEn = new JCheckBox("ICG range check enabled");
		icgEn.setSelected(icgRangeEnabled);
		icgPanel.add(icgEn);
		final JFormattedTextField icgLField = new JFormattedTextField(icgL * icgrealSlope + icgrealConstant);
		icgPanel.add(icgLField);
		icgLField.setColumns(5);
		final JFormattedTextField icgHField = new JFormattedTextField(icgH * icgrealSlope + icgrealConstant);
		icgPanel.add(icgHField);
		icgHField.setColumns(5);
		final JCheckBox icgBeep = new JCheckBox("beep                              ");
		icgBeep.setSelected(icgBeepEnabled);
		icgPanel.add(icgBeep);
		diag.add(icgPanel);

		ActionListener icgPanelActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				icgLField.setEnabled(icgEn.isSelected());
				icgHField.setEnabled(icgEn.isSelected());
				icgBeep.setEnabled(icgEn.isSelected());
			}
		};

		icgEn.addActionListener(icgPanelActionListener);
		icgPanelActionListener.actionPerformed(null);

		JPanel sclPanel = new JPanel();
		JLabelAntialiased sclOff = new JLabelAntialiased("     ");
		if (sclChan.getDivider() == 0 || sclChan.bStore == 0) {// See if SCL is Off
			sclOff = new JLabelAntialiased("Off");
		}
		sclOff.setFont(fnt);
		sclPanel.add(sclOff, BorderLayout.WEST);

		final JCheckBox sclEn = new JCheckBox("SCL range check enabled");
		sclEn.setSelected(sclRangeEnabled);
		sclPanel.add(sclEn);
		final JFormattedTextField sclLField = new JFormattedTextField(sclL * sclrealSlope + sclrealConstant);
		sclPanel.add(sclLField);
		sclLField.setColumns(5);
		final JFormattedTextField sclHField = new JFormattedTextField(sclH * sclrealSlope + sclrealConstant);
		sclPanel.add(sclHField);
		sclHField.setColumns(5);
		final JCheckBox sclBeep = new JCheckBox("beep                              ");
		sclBeep.setSelected(sclBeepEnabled);
		sclPanel.add(sclBeep);
		diag.add(sclPanel);

		ActionListener sclPanelActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sclLField.setEnabled(sclEn.isSelected());
				sclHField.setEnabled(sclEn.isSelected());
				sclBeep.setEnabled(sclEn.isSelected());
			}
		};

		sclEn.addActionListener(sclPanelActionListener);
		sclPanelActionListener.actionPerformed(null);

		JPanel cfWarnPanel = new JPanel();
		JLabelAntialiased space = new JLabelAntialiased("     ");
		space.setFont(fnt);
		cfWarnPanel.add(space, BorderLayout.WEST);

		final JCheckBox cfWarnCheck = new JCheckBox("Warn if recording space is less than");
		cfWarnPanel.add(cfWarnCheck);
		final JFormattedTextField cfWarnField = new JFormattedTextField(Integer.valueOf(24));
		cfWarnField.setValue(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.CFWARNHOURS));
		cfWarnPanel.add(cfWarnField);
		cfWarnField.setColumns(3);
		JLabel cfWarnLab2 = new JLabel("hour(s)                                         ");
		cfWarnPanel.add(cfWarnLab2);
		diag.add(cfWarnPanel);

		cfWarnCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cfWarnField.setEnabled(cfWarnCheck.isSelected());
			}
		});

		AppSettings aset = AppSettings.getInstance();

		int val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.CFWARNENABLED);
		if (val == 0) {
			cfWarnCheck.setSelected(false);
			cfWarnField.setEnabled(false);
		} else {
			cfWarnCheck.setSelected(true);

		}

		JPanel timeWarnPanel = new JPanel();
		JLabelAntialiased space2 = new JLabelAntialiased("     ");
		space2.setFont(fnt);
		timeWarnPanel.add(space2, BorderLayout.WEST);

		final JCheckBox timeWarnCheck = new JCheckBox("Warn if computer time differs from device time more than");
		timeWarnPanel.add(timeWarnCheck);

		final JFormattedTextField timeWarnField = new JFormattedTextField(Integer.valueOf(1000));
		timeWarnPanel.add(timeWarnField);
		timeWarnField.setColumns(4);
		timeWarnField.setValue((Integer) aset.getIntProperty(Settings.TIMEDIFFWARNING));
		JLabel timeWarnLab2 = new JLabel("second(s)");
		timeWarnPanel.add(timeWarnLab2);
		diag.add(timeWarnPanel);

		timeWarnCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				timeWarnField.setEnabled(timeWarnCheck.isSelected());
			}
		});
		if (aset.getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNINGENABLED) != 0) {
			timeWarnCheck.setSelected(true);
		} else {
			timeWarnCheck.setSelected(false);
			timeWarnField.setEnabled(false);
		}

		/*
		 * JPanel lightPanel = new JPanel(); JLabel lightLabel = new
		 * JLabel("Light sensor threshold:"); lightPanel.add(lightLabel);
		 * String[] lightSets = {"0 (max sensitivity)", "500 (default)",
		 * "4095 (off)"}; JComboBox lightcb = new JComboBox(lightSets);
		 * lightPanel.add(lightcb); diag.add(lightPanel);
		 */

		JPanel butPanel = new JPanel();
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AppSettings aset = AppSettings.getInstance();
				if (cfWarnCheck.isSelected()) {
					aset.setIntProperty(Settings.CFWARNENABLED, 1);
				} else {
					aset.setIntProperty(Settings.CFWARNENABLED, 0);
				}
				try {
					cfWarnField.commitEdit();
				} catch (ParseException e) {
				}
				aset.setIntProperty(Settings.CFWARNHOURS, (Integer) cfWarnField.getValue());

				if (timeWarnCheck.isSelected()) {
					aset.setIntProperty(Settings.TIMEDIFFWARNINGENABLED, 1);
				} else {
					aset.setIntProperty(Settings.TIMEDIFFWARNINGENABLED, 0);
				}
				try {
					timeWarnField.commitEdit();
				} catch (ParseException e) {
				}
				aset.setIntProperty(Settings.TIMEDIFFWARNING, (Integer) timeWarnField.getValue());
				diag.setVisible(false);

				boolean isChanged = false;
				if (ecgEn.isSelected() != ecgRangeEnabled) {
					isChanged = true;
					if (ecgEn.isSelected()) {
						set.dwSettingsFlags -= 0x00000400;
					} else {
						set.dwSettingsFlags += 0x00000400;
					}
				}
				if (icgEn.isSelected() != icgRangeEnabled) {
					isChanged = true;
					if (icgEn.isSelected()) {
						set.dwSettingsFlags -= 0x00000800;
					} else {
						set.dwSettingsFlags += 0x00000800;
					}
				}
				if (sclEn.isSelected() != sclRangeEnabled) {
					isChanged = true;
					if (sclEn.isSelected()) {
						set.dwSettingsFlags -= 0x00001000;
					} else {
						set.dwSettingsFlags += 0x00001000;
					}
				}
				if (ecgBeep.isSelected() != ecgBeepEnabled) {
					isChanged = true;
					if (ecgBeep.isSelected()) {
						set.dwSettingsFlags -= 0x00000080;
					} else {
						set.dwSettingsFlags += 0x00000080;
					}
				}
				if (icgBeep.isSelected() != icgBeepEnabled) {
					isChanged = true;
					if (icgBeep.isSelected()) {
						set.dwSettingsFlags -= 0x00000100;
					} else {
						set.dwSettingsFlags += 0x00000100;
					}
				}
				if (sclBeep.isSelected() != sclBeepEnabled) {
					isChanged = true;
					if (sclBeep.isSelected()) {
						set.dwSettingsFlags -= 0x00000200;
					} else {
						set.dwSettingsFlags += 0x00000200;
					}
				}

				int newVal;
				double vl;
				// ecgL * ecgrealSlope + ecgrealConstant
				try {
					ecgLField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) ecgLField.getValue();
				newVal = (int) Math.round((vl - ecgrealConstant) / ecgrealSlope);
				if (newVal != ecgL) {
					isChanged = true;
					set.wECGThresholdMin = newVal;
				}
				try {
					ecgHField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) ecgHField.getValue();
				newVal = (int) Math.round((vl - ecgrealConstant) / ecgrealSlope);
				if (newVal != ecgH) {
					isChanged = true;
					set.wECGThresholdMax = newVal;
				}

				try {
					icgLField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) icgLField.getValue();
				newVal = (int) Math.round((vl - icgrealConstant) / icgrealSlope);
				if (newVal != icgL) {
					isChanged = true;
					set.wZ0ThresholdMin = newVal;
				}
				try {
					icgHField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) icgHField.getValue();
				newVal = (int) Math.round((vl - icgrealConstant) / icgrealSlope);
				if (newVal != icgH) {
					isChanged = true;
					set.wZ0ThresholdMax = newVal;
				}

				try {
					sclLField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) sclLField.getValue();
				newVal = (int) Math.round((vl - sclrealConstant) / sclrealSlope);
				if (newVal != sclL) {
					isChanged = true;
					set.wSCLThresholdMin = newVal;
				}
				try {
					sclHField.commitEdit();
				} catch (ParseException e) {
				}
				vl = (Double) sclHField.getValue();
				newVal = (int) Math.round((vl - sclrealConstant) / sclrealSlope);
				if (newVal != sclH) {
					isChanged = true;
					set.wSCLThresholdMax = newVal;
				}

				if (isChanged) {
					tryToSendSettings(set);
				}
			}
		});
		butPanel.add(saveButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				diag.setVisible(false);
			}
		});
		butPanel.add(cancelButton);
		diag.add(butPanel);

		diag.pack();
		diag.getRootPane().setDefaultButton(saveButton);// ENTER will hit button Save
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		startThread();
	}

	void startThread() {
		recalcThread = new RecalcThread();
		recalcThread.start();
	}

	private void stopThread() {
		if (recalcThread.isStarted()) {
			recalcThread.setStarted(false);
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private void tryToSendSettings(final AmsSettings newSet) {
		final JDialog diag = new JDialog(this, "Saving settings to device", true);
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		final JLabel lab = new JLabel("Saving settings failed -- please check cable and try again!");

		// JdH: Always set the following variables because they should have been these
		// values and can't be
		// updated through firmware as settings are stored in flash during a firmware
		// upgrade
		newSet.wCFTimeout = 300;
		newSet.channels[AmsDeviceConstants.CH_Ire].dwDivider = 1;

		final JProgressBar pBar = new JProgressBar(0, 10);
		pBar.setValue(1);
		lab.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(lab);
		pBar.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(pBar);
		JPanel pan2 = new JPanel();
		JButton giveUpButton = new JButton("Give up");
		final JButton tryAgainButton = new JButton("Try again");
		tryAgainButton.setEnabled(false);
		pan2.add(giveUpButton);
		pan2.add(tryAgainButton);
		pan2.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(pan2);
		diag.add(pan);
		diag.pack();
		diag.setLocationRelativeTo(this);

		final StoppableRunnable sendThread = new StoppableRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					pBar.setValue(i + 1);
					lab.setText("Saving settings to device (attempt " + (i + 1) + "/10)");
					boolean result = ams.sendSettingsToDevice(newSet);
					try {
						Thread.sleep(100);
					} catch (Exception e) {

					}
					if (result == true) {
						setupValuesTXT();
						diag.setVisible(false);
						return;
					}
					if (isStopped()) {
						diag.setVisible(false);
						JOptionPane.showMessageDialog(DeviceDialog.this,
								"There was an error while saving the new device settings.\n\nThe new settings were NOT saved to the device!",
								"Settings not saved", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				lab.setText("Saving settings failed -- please check cable and try again!");
				tryAgainButton.setEnabled(true);
				stopThread();
			}
		};

		new Thread(sendThread).start();

		tryAgainButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendThread.startThread();
				new Thread(sendThread).start();
			}
		});

		giveUpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (sendThread.isStopped()) {
					diag.setVisible(false);
					JOptionPane.showMessageDialog(DeviceDialog.this,
							"There was an error while saving the new device settings.\n\nThe new settings were NOT saved to the device!",
							"Settings not saved",
							JOptionPane.ERROR_MESSAGE);
				} else {
					sendThread.stopThread();
				}
			}
		});

		diag.setVisible(true);

	}

	private void tryToSendStatics(final AmsStatics newSet) {
		final JDialog diag = new JDialog(this, "Saving statics to device", true);
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		final JLabel lab = new JLabel("Saving statics failed -- please check cable and try again!");

		final JProgressBar pBar = new JProgressBar(0, 10);
		pBar.setValue(1);
		lab.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(lab);
		pBar.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(pBar);
		JPanel pan2 = new JPanel();
		JButton giveUpButton = new JButton("Give up");
		final JButton tryAgainButton = new JButton("Try again");
		tryAgainButton.setEnabled(false);
		pan2.add(giveUpButton);
		pan2.add(tryAgainButton);
		pan2.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(pan2);
		diag.add(pan);
		diag.pack();
		diag.setLocationRelativeTo(this);

		final StoppableRunnable sendThread = new StoppableRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					pBar.setValue(i + 1);
					lab.setText("Saving statics to device (attempt " + (i + 1) + "/10)");
					boolean result = ams.sendStaticsToDevice(newSet);
					if (result == true) {
						setupValuesTXT();
						diag.setVisible(false);
						return;
					}
					if (isStopped()) {
						diag.setVisible(false);
						JOptionPane.showMessageDialog(DeviceDialog.this,
								"There was an error while saving the new device statics.\n\nThe new statics were NOT saved to the device!",
								"statics not saved", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				lab.setText("Saving statics failed -- please check cable and try again!");
				tryAgainButton.setEnabled(true);
				stopThread();
			}
		};

		new Thread(sendThread).start();

		tryAgainButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendThread.startThread();
				new Thread(sendThread).start();
			}
		});

		giveUpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (sendThread.isStopped()) {
					diag.setVisible(false);
					JOptionPane.showMessageDialog(DeviceDialog.this,
							"There was an error while saving the new device statics.\n\nThe new statics were NOT saved to the device!",
							"statics not saved",
							JOptionPane.ERROR_MESSAGE);
				} else {
					sendThread.stopThread();
				}
			}
		});

		diag.setVisible(true);

	}

}
