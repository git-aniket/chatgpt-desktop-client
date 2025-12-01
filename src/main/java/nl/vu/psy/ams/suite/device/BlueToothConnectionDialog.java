package nl.vu.psy.ams.suite.device;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
//import java.util.concurrent.Future;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.BluetoothStateException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import nl.vu.psy.ams.suite.gui.MainFrame;
//import nl.vu.psy.ams.suite.main.AppSettings;
//import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.ExceptionErrorDialog;
import nl.vu.psy.ams.suite.tools.StoppableRunnable;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intel.bluetooth.BlueCoveImpl;

/*
 * Dialog that lets you choose which device ID you
 * want to connect with. Looks up this ID in the BT
 * database, and lets you connect if it is found.
 */
public class BlueToothConnectionDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JComboBox<?> cb;
	private LinkedList<String> savedBTNumbers;
	private AmsDevice ams;
	private JDialog connDiag;
	private StoppableRunnable run;

	public BlueToothConnectionDialog(final AmsDevice ams) {
		super(MainFrame.getInstance().getMainFrame(), "Connect to BlueTooth device", true);
		this.ams = ams;
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		JPanel pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel("Device serial number:"), BorderLayout.WEST);

		savedBTNumbers = new LinkedList<String>();
		String OSname = System.getProperty("os.name");
		String path = "";
		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application " + "Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		File curFile;
		curFile = new File(path, "VU-DAMS");
		if (curFile.exists() == false) {
			curFile.mkdir();
		}
		curFile = new File(curFile, "savedBTNumbers.json");

		if (curFile.exists()) {
			String inString;
			java.lang.reflect.Type collectionType = new TypeToken<LinkedList<String>>() {
			}.getType();
			Gson gson = new Gson();
			LinkedList<String> tempList;
			inString = Utils.readStringFromFile(curFile);
			tempList = gson.fromJson(inString, collectionType);
			savedBTNumbers.addAll(tempList);
		}

		cb = new JComboBox<Object>(savedBTNumbers.toArray());
		cb.setEditable(true);

		pan2.add(cb, BorderLayout.CENTER);

		pan.add(pan2);

		pan2 = new JPanel();
		final JLabel correctLabel = new JLabel("Device not found in database");
		correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
		pan2.add(correctLabel, BorderLayout.CENTER);
		pan.add(pan2);

		pan2 = new JPanel();
		final JButton connectBut = new JButton("Connect");
		connectBut.setEnabled(false);
		pan2.add(connectBut);
		JButton closeBut = new JButton("Cancel");
		pan2.add(closeBut);
		pan.add(pan2);
		add(pan);
		pack();
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

		((JTextComponent) cb.getEditor().getEditorComponent()).getDocument()
				.addDocumentListener(new DocumentListener() {

					@Override
					public void changedUpdate(DocumentEvent e) {
						check();
					}

					private void check() {
						try {
							int val = Integer
									.parseInt(((JTextComponent) cb.getEditor().getEditorComponent()).getText());
							if (BluetoothDatabase.getDatabase().get(val) != null) {
								correctLabel.setText("Device found in database!");
								correctLabel
										.setIcon(new ImageIcon(getClass().getResource("/img/Clear Green Button.png")));
								connectBut.setEnabled(true);
							} else {
								correctLabel.setText("Device not found in database");
								correctLabel
										.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
								connectBut.setEnabled(false);
							}
						} catch (NumberFormatException e) {
							correctLabel.setText("Device not found in database");
							correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
							connectBut.setEnabled(false);
						}
					}

					@Override
					public void insertUpdate(DocumentEvent e) {
						check();
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						check();
					}
				});

		cb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg) {
				try {
					int val = Integer.parseInt(((JTextComponent) cb.getEditor().getEditorComponent()).getText());
					if (BluetoothDatabase.getDatabase().get(val) != null) {
						correctLabel.setText("Device found in database!");
						correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Clear Green Button.png")));
						connectBut.setEnabled(true);
					} else {
						correctLabel.setText("Device not found in database");
						correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
						connectBut.setEnabled(false);
					}
				} catch (NumberFormatException e) {
					correctLabel.setText("Device not found in database");
					correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
					connectBut.setEnabled(false);
				}
			}
		});

		if (cb.getSelectedItem() != null) {
			try {
				int val = Integer.parseInt(((JTextComponent) cb.getEditor().getEditorComponent()).getText());
				if (BluetoothDatabase.getDatabase().get(val) != null) {
					correctLabel.setText("Device found in database!");
					correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Clear Green Button.png")));
					connectBut.setEnabled(true);
				} else {
					correctLabel.setText("Device not found in database");
					correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
					connectBut.setEnabled(false);
				}
			} catch (NumberFormatException e) {
				correctLabel.setText("Device not found in database");
				correctLabel.setIcon(new ImageIcon(getClass().getResource("/img/Cancel Red Button.png")));
				connectBut.setEnabled(false);
			}
		}

		closeBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		cb.getEditor().getEditorComponent().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					connect();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});

		connectBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
	}

	private void connect() {
		int val = 0;
		try {
			val = Integer.parseInt(((JTextComponent) cb.getEditor().getEditorComponent()).getText());
		} catch (NumberFormatException nfe) {
			return;
		}

		savedBTNumbers.remove(Integer.toString(val));
		savedBTNumbers.addFirst(Integer.toString(val));
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			File curFile;
			/*
			 * //----------------- Previous code to save the savedBTNumbers.json to Temp
			 * Directory-----------------
			 * curFile = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
			 * curFile = new File(curFile, "VU-DAMS/savedBTNumbers.json");
			 */

			// ----------------Code to save savedBTNumbers.json to App Data
			// directory----------------------------
			String OSname = System.getProperty("os.name");
			String path = "";
			if (OSname.contains("Mac")) {
				path = System.getProperty("user.home") + "/Library/Application " + "Support";
			} else if (OSname.contains("Linux")) {
				path = System.getProperty("user.home") + "/.local/share/applications";
			} else {
				path = System.getenv("APPDATA");
			}
			curFile = new File(path, "VU-DAMS");
			curFile.mkdir();
			curFile = new File(curFile, "savedBTNumbers.json");
			// --------------------------------------------------------------------------------------------------

			writer = new PrintWriter(new BufferedWriter(new FileWriter(curFile)));
			writer.print(gson.toJson(savedBTNumbers));
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		ams.blueToothAddress = BluetoothDatabase.getDatabase().get(val);
		if (ams.blueToothAddress == null) {
			JOptionPane.showMessageDialog(BlueToothConnectionDialog.this, "AMS Device not found in database",
					"Device not found", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final JDialog connDiag = new JDialog(BlueToothConnectionDialog.this, "Connecting to AMS Device " + val, true);
		JPanel pan = new JPanel(new BorderLayout());
		final JProgressBar prog = new JProgressBar(0, 4);
		pan.add(prog, BorderLayout.CENTER);
		JPanel pan2 = new JPanel();
		JButton giveUpB = new JButton("Give Up");
		pan2.add(giveUpB);
		pan.add(pan2, BorderLayout.SOUTH);
		connDiag.add(pan);
		connDiag.pack();
		connDiag.setMinimumSize(
				new Dimension(connDiag.getPreferredSize().width * 2, connDiag.getPreferredSize().height));
		connDiag.setLocationRelativeTo(BlueToothConnectionDialog.this);

		final StoppableRunnable run = new StoppableRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 2; i++) {
					prog.setValue(i + 1);
					if (isStopped() == true) {
						ams.closeConnection();
						connDiag.setVisible(false);
						return;
					}
					ams.conn = null;
					try {

						ams.conn = (StreamConnection) Connector.open(
								"btspp://" + ams.blueToothAddress + ":1;authenticate=false;encrypt=false;master=false",
								Connector.READ_WRITE, true);
						ams.in = ams.conn.openInputStream();
						ams.out = ams.conn.openOutputStream();
						if (ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_DEVICEID) != 0x32534d41) {
							ams.in.close();
							ams.out.close();
							BlueCoveImpl.shutdown();
						} else {
							ams.setConnectionOpen(true);
							ams.isConnected = true;
							connDiag.setVisible(false);
							return;
						}
					} catch (BluetoothStateException bse) {
						BlueCoveImpl.shutdown();
					} catch (BluetoothConnectionException bce) {
						BlueCoveImpl.shutdown();
					} catch (IOException e2) {
						BlueCoveImpl.shutdown();
					}
				}
				if (isStopped()) {
					ams.closeConnection();
					connDiag.setVisible(false);
					return;
				}
				prog.setValue(3);
				ams.conn = null;
				try {

					ams.conn = (StreamConnection) Connector.open(
							"btspp://" + ams.blueToothAddress + ":1;authenticate=false;encrypt=false;master=false",
							Connector.READ_WRITE, true);

					ams.in = ams.conn.openInputStream();
					ams.out = ams.conn.openOutputStream();
					if (ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_DEVICEID) != 0x32534d41) {
						ams.in.close();
						ams.out.close();
						BlueCoveImpl.shutdown();
					} else {
						ams.setConnectionOpen(true);
						ams.isConnected = true;
						connDiag.setVisible(false);
						return;
					}
				} catch (BluetoothStateException bse) {
					if (bse.getMessage().equals("BluetoothStack not detected")) {
						ExceptionErrorDialog
								.showExceptionErrorDialog(
										BlueToothConnectionDialog.this,
										"<html>Could not detect a bluetooth device on this computer.<br>Make sure that the bluetooth device is inserted, and that the correct drivers are installed.</html>",
										"Bluetooth device not found", bse);
						BlueCoveImpl.shutdown();
					} else {
						ExceptionErrorDialog.showExceptionErrorDialog(BlueToothConnectionDialog.this,
								"A bluetooth state error occured",
								"Bluetooth state error", bse);
						BlueCoveImpl.shutdown();
					}
				} catch (BluetoothConnectionException bce) {
					ExceptionErrorDialog
							.showExceptionErrorDialog(
									BlueToothConnectionDialog.this,
									"<html>Could not connect to AMS Device. Please make sure that the device is turned on.<br>If this message returns, please restart the AMS device and try again.</html>",
									"Could not connect to AMS device", bce);
					BlueCoveImpl.shutdown();
				} catch (IOException e2) {
					ExceptionErrorDialog
							.showExceptionErrorDialog(
									BlueToothConnectionDialog.this,
									"<html>Could not connect to AMS Device. Please make sure that the device is turned on.<br>If this message returns, please restart the AMS device and try again.</html>",
									"Could not connect to AMS device", e2);
					BlueCoveImpl.shutdown();
				}
				connDiag.setVisible(false);
			}
		};

		/* final Future<?> fut = */ CachedThreadPool.submit(run);
		giveUpB.addActionListener(l);
		connDiag.setVisible(true);

		setVisible(false);
		if (ams.isConnected) {
			DeviceDialog devDiag = new DeviceDialog(ams);
			devDiag.setVisible(true);
		} else {
			ams.closeConnection();
			run.stopThread();
			BlueCoveImpl.shutdown();
		}
	}

	ActionListener l = (new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			try {
				ams.closeConnection();
				run.stopThread();
				connDiag.setVisible(false);
			} catch (NullPointerException e2) {

			}

		}
	});

}
