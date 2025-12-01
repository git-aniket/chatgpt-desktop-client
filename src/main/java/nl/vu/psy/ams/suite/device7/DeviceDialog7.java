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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.JLabelAntialiased;
import nl.vu.psy.ams.suite.tools.Utils;

public class DeviceDialog7 extends JFrame {

	class RecalcThread extends Thread {

		private boolean	started	= true, useUDP = false;

		public synchronized boolean isStarted() {
			return started;
		}

		public synchronized boolean isUDP() {
			return useUDP;
		}

		public void run() {
			long oldtime = System.nanoTime();
			byte[] buffer = new byte[1500];
			// DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			while (isStarted()) {
				oldtime = System.nanoTime();
				ams.Bfound = false;
				ams.Dfound = false;
				boolean allChannelsFound = false;
				while (isUDP() && allChannelsFound == false && buffer != null) { // && ams.socket != null && !ams.socket.isClosed()) {
					try {
						// ams.socket.receive(response);
						buffer = UDPSockets.getInstance().getPacket(portNumber, InetAddress.getByName(deviceIP));
					} catch (UnknownHostException e) {
						if (!ams.webSocketClient.isOpen()) {
							connectionLost();
							if (isGivenUp())
								return;
						}		
						e.printStackTrace();
					}
					if (buffer == null)  {
						if (!ams.webSocketClient.isOpen()) {
							connectionLost();
							if (isGivenUp())
								return;
						}		
					}
					else
						allChannelsFound = ams.handlePacket(buffer);
				}
				setupValuesTXT();
				setupSDTXT();
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

		public synchronized void setUDP(boolean udp) {
			this.useUDP = udp;
		}
	}

	private static final long serialVersionUID = 1L;
	private AmsDevice7				ams;
	private JPanel					leftPanel;
	private Container				middlePanel;
	DecimalFormat					twoPlaces			= new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	DecimalFormat					onePlace			= new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
	DecimalFormat					zeroPlace			= new DecimalFormat("0", new DecimalFormatSymbols(Locale.ENGLISH));
	RecalcThread					recalcThread;
	private JPanel					rightPanel, vallab;
	private JLabel batLab, phaseLab, timeLab, firmwareLab, hardwareLab;
	private Font					fnt;
	private boolean					givenUp	= false, connectionLost = false, errorShown = false;
	private JButton					onlinebut, startStopButton, changeStaticsBut;
	private JButton					setdevtimebut, setPhaseBut, wifiShutdownBut;
	private JLabelAntialiased		freqlab;
	private JLabelAntialiased		sdlab;
	int portNumber;
	double nPhase;
	private int packet;
	String deviceIP, subjectId = "N/A", eDistance = "N/A", formatPhase = "N/A";
	private InetSocketAddress socketAddress;

	public DeviceDialog7(AmsDevice7 amsDevice, String deviceIP, int portNumber, int packet) {
		super();
		fnt = new Font("Arial", Font.PLAIN, 20);
		this.ams = amsDevice;
		this.deviceIP = deviceIP;
		this.portNumber = portNumber;
		this.packet = packet;
		this.setTitle("AMS7 Device " + ams.webSocketClient.getName() + " connected via UDP/Wifi");
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
		startThread();
		setEnabledItems();
		setIconImage(new ImageIcon("src/main/resources/img/hearmysite.png").getImage());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				// if (ams.socket != null)
				// 	ams.socket.close();
				try {
					UDPSockets.getInstance().remove(portNumber, InetAddress.getByName(deviceIP));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				ams.webSocketClient.close();
				closeDialog();
			}
		});
	}
	
	private void changeStatics() {
		if (Utils.askForExpertPassword(true) == false)
			return;
		final JDialog diag = new JDialog(DeviceDialog7.this, "Change statics", true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());
		String sId = "", name = "";
		int wait2 = 200;

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Create new data file after (minutes; 0 is never):"), BorderLayout.WEST);
		sId = "";
		ams.webSocketClient.send("cmd !RESTART_ACQ;");
		try {
			int i = 0;
			while (!name.equals("RESTART_ACQ") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf4 = new JTextField(12);
		tf4.setColumns(12);
		if (name.equals("RESTART_ACQ"))
			tf4.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf4, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Turn off LEDs after (seconds; 0 is never):"), BorderLayout.WEST);
		sId = "";
		ams.webSocketClient.send("cmd !LED_DIM;");
		try {
			int i = 0;
			while (!name.equals("LED_DIM") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf7 = new JTextField(12);
		tf7.setColumns(12);
		if (name.equals("LED_DIM"))
			tf7.setText(sId);
		else
			System.out.println("name " + name);
        pan.add(tf7, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Device name"), BorderLayout.WEST);
		sId = "";
		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}		
		ams.webSocketClient.send("cmd !SENSORNAME;");
		try {
			int i = 0;
			while (!name.equals("SENSORNAME") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf1 = new JTextField(12);
		tf1.setColumns(12);
		if (name.equals("SENSORNAME"))
			tf1.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf1, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Serial number"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !SERIAL;");
		try {
			int i = 0;
			while (!name.equals("SERIAL") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf2 = new JTextField(12);
		tf2.setColumns(12);
		if (name.equals("SERIAL"))
			tf2.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf2, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Auto power off (seconds)"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !AUTO_POWER;");
		try {
			int i = 0;
			while (!name.equals("AUTO_POWER") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf3 = new JTextField(12);
		tf3.setColumns(12);
		if (name.equals("AUTO_POWER"))
			tf3.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf3, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Station mode ssid"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !SSID;");
		try {
			int i = 0;
			while (!name.equals("SSID") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf5 = new JTextField(12);
		tf5.setColumns(12);
		if (name.equals("SSID"))
			tf5.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf5, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Station mode password"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !PASSWORD;");
		try {
			int i = 0;
			while (!name.equals("PASSWORD") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf6 = new JTextField(12);
		tf6.setColumns(12);
		if (name.equals("PASSWORD"))
			tf6.setText(sId);
		else
			System.out.println("name " + name);
		pan.add(tf6, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Wifi on at startup device"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !STARTUP_WIFI;");
		try {
			int i = 0;
			while (!name.equals("STARTUP_WIFI") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JComboBox<String> wifiCOB = new JComboBox<>(new String[]{"True", "False"});
		if (name.equals("STARTUP_WIFI")) {
			if (sId.equals("true"))
				wifiCOB.setSelectedIndex(0);
			if (sId.equals("false"))
				wifiCOB.setSelectedIndex(1);
		}
		else
			System.out.println("name " + name);
		pan.add(wifiCOB, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JLabelAntialiased("Send device log to file"), BorderLayout.WEST);
		sId = "";
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
		ams.webSocketClient.send("cmd !LOG_TO_FILE;");
		try {
			int i = 0;
			while (!name.equals("LOG_TO_FILE") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JComboBox<String> logCOB = new JComboBox<>(new String[]{"True", "False"});
		if (name.equals("LOG_TO_FILE")) {
			if (sId.equals("true"))
				logCOB.setSelectedIndex(0);
			if (sId.equals("false"))
				logCOB.setSelectedIndex(1);
		}
		else
			System.out.println("name " + name);
		pan.add(logCOB, BorderLayout.EAST);
		diag.add(pan);

		pan = new JPanel();

		JButton but = new JButton("Save");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				String idText = tf1.getText();
				ams.webSocketClient.send("cmd !SENSORNAME=" + idText + ";");
				diag.setVisible(false);

				idText = tf2.getText();
				// try {
				// 	Integer.parseInt(idText);
				// } catch (NumberFormatException nfe) {
				// 	JOptionPane.showMessageDialog(diag, "The serial number should be numbers only!", "Numbers only",
				// 			JOptionPane.ERROR_MESSAGE);
				// 	return;
				// }
				ams.webSocketClient.send("cmd !SERIAL=" + idText + ";");
				idText = tf4.getText();
				if (Utils.parseInt(idText) == null) {
					JOptionPane.showMessageDialog(diag, "The create new data file after setting should be numbers only!", "Numbers only",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				ams.webSocketClient.send("cmd !RESTART_ACQ=" + idText + ";");

				idText = tf7.getText();
				if (Utils.parseInt(idText) == null) {
					JOptionPane.showMessageDialog(diag, "The turn off LEDs after setting should be numbers only!", "Numbers only",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				ams.webSocketClient.send("cmd !LED_DIM=" + idText + ";");

				idText = tf3.getText();
				if (Utils.parseInt(idText) == null) {
					JOptionPane.showMessageDialog(diag, "The auto power off setting should be numbers only!", "Numbers only",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				ams.webSocketClient.send("cmd !AUTO_POWER=" + idText + ";");

				idText = tf5.getText();
				if (idText.length() < 6) {
					JOptionPane.showMessageDialog(diag, "The SSID can not contain less than 6 characters!", "SSID too short",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				ams.webSocketClient.send("cmd !SSID=" + idText + ";");

				idText = tf6.getText();
				if (idText.length() < 8) {
					JOptionPane.showMessageDialog(diag, "The wifi password can not contain less than 8 characters!", "Password too short",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				ams.webSocketClient.send("cmd !PASSWORD=" + idText + ";");

				int selIndex = wifiCOB.getSelectedIndex();
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				if (selIndex == 0)
					ams.webSocketClient.send("cmd !STARTUP_WIFI=true;");
				if (selIndex == 1)
					ams.webSocketClient.send("cmd !STARTUP_WIFI=false;");

				selIndex = logCOB.getSelectedIndex();
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				if (selIndex == 0)
					ams.webSocketClient.send("cmd !LOG_TO_FILE=true;");
				if (selIndex == 1)
					ams.webSocketClient.send("cmd !LOG_TO_FILE=false;");

				diag.setVisible(false);
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
		diag.setLocationRelativeTo(DeviceDialog7.this);
		diag.getRootPane().setDefaultButton(but);//ENTER will hit button Save

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
		DeviceDialog7.this.setVisible(false);
		DeviceDialog7.this.dispose();
			// if (ams.socket != null)
			// 	ams.socket.close();
			try {
				UDPSockets.getInstance().remove(portNumber, InetAddress.getByName(deviceIP));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		ams.webSocketClient.close();
	}

	void connectionLost() {
		if (!connectionLost) {
			connectionLost = true;
			Thread reconnect = new Thread() {
				@Override
				public void run() {
					if (isGivenUp())
						return;	
					int tries 	= 0;		

					while(!ams.webSocketClient.isOpen()) {
						try {
							ams.webSocketClient.reconnectBlocking();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						tries++;			
						if(tries >= 2) {						
							connectionLostConfirmed();
							break;
						}					
					}
					connectionLost = false;
				}
			};
			reconnect.start();
		}
	}

	private void connectionLostConfirmed() {
		final JDialog diag = new JDialog(this, "Connection lost"); //, Dialog.ModalityType.DOCUMENT_MODAL);
		setGivenUp(false);
		diag.setUndecorated(true);
		diag.setLayout(new BorderLayout());
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		String text = "<html><table><tr><td><h1>Connection to the AMS device has been lost!</h1></td></tr><tr><td><h1>The connection will be automatically restored if possible.</h1></td></tr>";
			text += "<tr><td><h1>Please check the wifi connection!</h1></td></tr></table></html>";
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
				DeviceDialog7.this.closeDialog();
			}
		});
		giveUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		diag.add(pan, BorderLayout.CENTER);
		pan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		diag.pack();
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		while (true) {
			// try {
				ams.webSocketClient.reconnect();
			// } catch (InterruptedException e) {
			// 	e.printStackTrace();
			// }	
			if (ams.webSocketClient.isOpen()) {
				diag.setVisible(false);
				break;
			}
			if (givenUp)
				return;
		}
	}

	private void startUDP() {
		try {
			InetAddress address = InetAddress.getByName(deviceIP);
			int port = portNumber;
			int packetSize =  packet;
			UDPSockets sockets = UDPSockets.getInstance();
			sockets.put(port, ams.socket, address);
			System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), port); 
			byte[] buffer = new byte[packetSize];
			 
			// DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);
			// ams.socket.send(request);
			//DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			int count = 0;
			//boolean connected = false;
			while (count < 2) { //try to get headers
				try {
					// ams.socket.receive(response);
					buffer = UDPSockets.getInstance().getPacket(portNumber, InetAddress.getByName(deviceIP));
				} catch (UnknownHostException e) {
					if (!ams.webSocketClient.isOpen()) {
						connectionLost();
						if (isGivenUp())
							return;
					}		
					e.printStackTrace();
				}
				if (buffer == null)  {
					if (!ams.webSocketClient.isOpen()) {
						connectionLost();
						if (isGivenUp())
							return;
					}		
				}
				else {
				// InetSocketAddress UDPsocketAddress = (InetSocketAddress)response.getSocketAddress();
				// InetAddress UDPaddress = UDPsocketAddress.getAddress();
				// if (UDPaddress.equals(address)) {
				// 	if (!connected) {
				// 		ams.socket.connect(socketAddress.getAddress(), UDPsocketAddress.getPort());
				// 		System.out.println("UDP connected on port " + UDPsocketAddress.getPort());
				// 		connected = true;
				// 	}
					ams.handlePacket(buffer); //first 3 packets may contain header info
					count++;
				// } else {
				// 	System.out.println("UDP address " + UDPsocketAddress.toString());
				// }
				}
			}
			//System.out.println("UDP address: " + address.toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		}
	}

	private void doStartOrStop(String idText, String idText2, Integer icgVmm) {
		startStopButton.setEnabled(false);
		if (startStopButton.getText().equals("Start")) {
			final JDialog diag = new JDialog(DeviceDialog7.this, "Starting", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Starting recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialog7.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					// stopThread();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					if (!ams.webSocketClient.isOpen()) {
						connectionLost();
						if (isGivenUp())
							return;
					}		
					if (idText.length() > 25) {
					JOptionPane.showMessageDialog(diag, "The subject ID can not contain more than 25 characters!", "Subject ID too long",
							JOptionPane.ERROR_MESSAGE);
							diag.setVisible(false);
							return;
				}
					ams.webSocketClient.send("cmd !SUBJECT_ID=" + idText + ";");
					Object[] options = {"Yes", "No"};
					if (icgVmm < 0) {
						JOptionPane.showMessageDialog(diag, "The ICG-V distance can not be negative!", "Negative ICG-V distance", JOptionPane.ERROR_MESSAGE);
						diag.setVisible(false);
						return;
					}
					else if (icgVmm < 50 || icgVmm > 600) {
						int r = JOptionPane.showOptionDialog(diag, "You have entered " + icgVmm + " millimeters as the distance between the two front electrodes. This is an implausible value, are you sure you want to proceed?", "Warning", JOptionPane.YES_NO_OPTION,
						    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
						if (r != 0) {
							diag.setVisible(false);
							return;
						}
					}
					ams.webSocketClient.send("cmd !E_DISTANCE=" + idText2 + ";");
					ams.webSocketClient.send("cmd r");
					ams.webSocketClient.send("cmd 3a");
					socketAddress = ams.webSocketClient.getRemoteSocketAddress();
					System.out.println("Websocket address: " + socketAddress.toString());
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
					ams.webSocketClient.setAcq(" 1"); //set non 0 value to go to running state
					setEnabledItems();
					// startUDP();
					// startThread();
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(true);
			stopThread();
			OnlineDialog7 oDiag = new OnlineDialog7(ams, DeviceDialog7.this, null);
			oDiag.setVisible(true);
	} else if (startStopButton.getText().equals("Stop")) {
			final JDialog diag = new JDialog(DeviceDialog7.this, "Stopping", true);
			diag.setUndecorated(true);
			diag.add(new JLabelAntialiased("<html><body><h1>Stopping recording...</h1></body></html>"));
			diag.pack();
			diag.setLocationRelativeTo(DeviceDialog7.this);
			Thread thrd = new Thread() {
				@Override
				public void run() {
					// stopThread();
					// if (ams.socket != null)
					// 	ams.socket.close();
					try {
						UDPSockets.getInstance().remove(portNumber, InetAddress.getByName(deviceIP));
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					if (!ams.webSocketClient.isOpen()) {
						connectionLost();
						if (isGivenUp()) {
							diag.setVisible(false);
							return;
						}
					}		
					ams.webSocketClient.send("cmd s");
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
					}
					ams.webSocketClient.setAcq(" 0"); //set 0 value to go to stopped state
					setEnabledItems();
					// startThread();
					diag.setVisible(false);
				}
			};
			thrd.start();
			diag.setVisible(true);
		}
	}

	private void doShutdown() {
		wifiShutdownBut.setEnabled(false);
		if (wifiShutdownBut.getText().equals("Shut down Device")) {
			if (!ams.webSocketClient.isOpen()) {
				connectionLost();
				if (isGivenUp())
					return;
			}		
			ams.webSocketClient.send("cmd Q");
			closeDialog();
		} else if (wifiShutdownBut.getText().equals("Turn off Device Wifi")) {
			if (!ams.webSocketClient.isOpen()) {
				connectionLost();
				if (isGivenUp())
					return;
			}		
			stopThread();
			ams.webSocketClient.send("cmd w");
			closeDialog();
		}
	}

	protected synchronized boolean isGivenUp() {
		return givenUp;
	}

	private void setEnabledItems() {
		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}		
		//ams.webSocketClient.send("ws_update");

		//startStopButton.setEnabled(false);
		//startStopButton.setText("N/A");
		String state = ams.webSocketClient.getAcq();
		int i = 0;
			while (state == "" && i < 25) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				state = ams.webSocketClient.getAcq();
				i++;
			}
		//if (state != "") {
			if (" 0".equals(state)) {
				startStopButton.setText("Start");
				startStopButton.setEnabled(true);
				onlinebut.setEnabled(false);
				onlinebut.setVisible(false);
				setdevtimebut.setEnabled(true);
				setPhaseBut.setEnabled(true);
				changeStaticsBut.setEnabled(true);
				wifiShutdownBut.setText("Shut down Device");
				wifiShutdownBut.setEnabled(true);
				recalcThread.setUDP(false);
			} else if (" 1".equals(state)) {
				// try {
					//if (UDPSockets.getInstance().getPacket(portNumber, InetAddress.getByName(deviceIP)) == null) {
						if (!ams.webSocketClient.isOpen()) {
							connectionLost();
							if (isGivenUp())
								return;
						}		
						ams.webSocketClient.send("cmd 3a");
						// try {
						// 	Thread.sleep(500);
						// } catch (InterruptedException e) {
						// }
						socketAddress = ams.webSocketClient.getRemoteSocketAddress();
						System.out.println("Websocket address: " + socketAddress.toString());
						startUDP();
					//}
				// } catch (UnknownHostException e) {
				// 	e.printStackTrace();
				// }
				recalcThread.setUDP(true);
				startStopButton.setText("Stop");
				startStopButton.setEnabled(true);
				onlinebut.setEnabled(true);
				onlinebut.setVisible(true);
				setdevtimebut.setEnabled(false);
				setPhaseBut.setEnabled(false);
				changeStaticsBut.setEnabled(false);
				wifiShutdownBut.setText("Turn off Device Wifi");
				wifiShutdownBut.setEnabled(true);
			}
		//}
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
				Expression e = new ExpressionBuilder(chan.getFormula()).variables(variables).build().setVariables(chan.getConstants());
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
			long sr = 1000 / chan.getDwDivider();
			return "<tr><td>" + name + ": </td><td>" + twoPlaces.format(real) + " " + chan.getSzUnit() + "</td><td>" + sr + " Hz</td></tr>";
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
		this.setTitle("AMS7 Device " + ams.webSocketClient.getName() + " connected via UDP/Wifi");
		String sId = "", name = "";
		if (subjectId.equals("N/A")) {
		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}		
		ams.webSocketClient.send("cmd !SUBJECT_ID;");
		try {
			int i = 0;
			while (!name.equals("SUBJECT_ID") && i < 25) {
				Thread.sleep(200);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (name.equals("SUBJECT_ID"))
			subjectId = sId;
		}
		if (eDistance.equals("N/A")) {
		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}		
		ams.webSocketClient.send("cmd !E_DISTANCE;");
		try {
			int i = 0;
			while (!name.equals("E_DISTANCE") && i < 25) {
				Thread.sleep(200);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					sId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (name.equals("E_DISTANCE"))
			eDistance = sId;

		}

		String text = "";
		String batStatus = ams.webSocketClient.getBat();
		text += "Battery voltage: ";
		boolean isBatWarning = false, isBatFull = false;
		String voltage = "N/A";
		Integer nVoltage = 0;
		if (batStatus != "") {
			nVoltage = Integer.valueOf(batStatus.split(" ", 3)[1]);
			if (nVoltage < 3800)
				isBatWarning = true;
			voltage = nVoltage.toString();
			if (nVoltage > 4100)
				isBatFull = true;
			voltage = nVoltage.toString();
		}
		if (isBatWarning)
			batLab.setForeground(Color.red);
		else if (isBatFull)
			batLab.setForeground(Color.green);
		else
			batLab.setForeground(Color.BLACK);
		text += voltage;
		text += " mV";
		batLab.setText(text);
		text = "";
		String respPhase = ams.webSocketClient.getPhase();
		formatPhase = "N/A";
		if (respPhase != "") {
			try {
			nPhase = Double.valueOf(respPhase);
			formatPhase = onePlace.format(nPhase);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		phaseLab.setText("Resp. phase: " + formatPhase);

		String devTime = ams.webSocketClient.getDate();
		text += "Device Time: ";
		boolean isWarning = false;
		if (devTime != "") {
			DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
			Date date = new Date();
			try {
				date = df.parse(devTime);
			} catch (ParseException e) {
				df = new SimpleDateFormat("EEE MMM  d HH:mm:ss yyyy", Locale.ENGLISH);
				try {
					date = df.parse(devTime);
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			long diffTime = Math.abs(cal.getTimeInMillis() - GregorianCalendar.getInstance().getTimeInMillis());
			if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNINGENABLED) != 0) {
				if (diffTime > 1000. * AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.TIMEDIFFWARNING)) {
					isWarning = true;
				}
			}
		} else 
			devTime = "N/A";
		if (isWarning)
			timeLab.setForeground(Color.red);
		else
			timeLab.setForeground(Color.BLACK);
		text += devTime;
		timeLab.setText(text);;

		String version = ams.webSocketClient.getVersion();
		firmwareLab.setText("Firmware Version: " + version);

		version = ams.webSocketClient.getHWVersion();
		hardwareLab.setText("Hardware Version: " + version);
	}
	
	private void setupValuesPanel() {
		vallab = new JPanel();
		vallab.setLayout(new BoxLayout(vallab, BoxLayout.Y_AXIS));
		batLab = new JLabel("Battery voltage: N/A");
		batLab.setAlignmentX(CENTER_ALIGNMENT);
		vallab.add(batLab);
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
		setupValuesTXT();
		vallab.setFont(fnt);
		vallab.setBorder(BorderFactory.createTitledBorder("Values"));
		middlePanel.add(vallab, BorderLayout.NORTH);
	}

	private void setupSDPanel() {

		sdlab = new JLabelAntialiased("");
		setupSDTXT();
		sdlab.setFont(fnt);
		sdlab.setBorder(BorderFactory.createTitledBorder("SD Card"));
		middlePanel.add(sdlab, BorderLayout.SOUTH);
	}
	private void setupSDTXT() {
		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}		
		String text = "<html><body><table>";
		//bytes per second: A 1000*24, M 1000*28, C 0.1*12, D 5*16, B 1*12 -> 52093.2
		double nBytesPerSecond = 52093.2;
		nBytesPerSecond *= 3600; // bytes per hour
	
		String bps = "";

		if (nBytesPerSecond > 1024 * 1024 * 1024)
			bps = twoPlaces.format(nBytesPerSecond / (1024 * 1024 * 1024)) + " GB";
		else
			bps = twoPlaces.format(nBytesPerSecond / (1024 * 1024)) + " MB";

		text += "<tr><td>Estimated memory usage per hour:</td><td>" + bps + "</td></tr>";

		String mem = ams.webSocketClient.getMem();
		String freebytes = "N/A";
		Long nFreeBytes = 0L;
		if (mem != "") {
			nFreeBytes = Long.valueOf(mem.split(" ", 3)[1]);
			if (nFreeBytes > 1024 * 1024 * 4) // some boards give very high number when no card is inserted
				nFreeBytes = 0L;
			if (nFreeBytes <= 50 && !errorShown) {
				JOptionPane.showMessageDialog(this, "SD card (almost) full or not inserted (" + twoPlaces.format(nFreeBytes) + " MB left)",
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
			text += "<tr><td>Memory space left for:</td><td><font color=red>" + twoPlaces.format(timeLeft) + " h</font></td></tr>";
		} else {
			text += "<tr><td>Memory space left for:</td><td>" + twoPlaces.format(timeLeft) + " h</td></tr>";
		}
		text += "</table></body></html>";
		sdlab.setText(text);

	}
	private void setupButtons() {
		JPanel newPanel = new JPanel();
		newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
		JPanel pan = new JPanel();
		pan.setLayout(new FlowLayout(FlowLayout.LEFT));
		String sId = "", name = "";
		int wait2 = 50;

		if (!ams.webSocketClient.isOpen()) {
			connectionLost();
			if (isGivenUp())
				return;
		}
		pan.add(new JLabelAntialiased("Recording ID:"));
		ams.webSocketClient.send("cmd !SUBJECT_ID;");
		try {
			int i = 0;
			while (!name.equals("SUBJECT_ID") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					subjectId = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField tf = new JTextField(12);
		tf.setColumns(12);
		if (name.equals("SUBJECT_ID"))
			tf.setText(subjectId);
		else
			System.out.println("name " + name);
		pan.add(tf);
		pan.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan);
		JPanel pan2 = new JPanel();
		pan2.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabelAntialiased study = new JLabelAntialiased("Study ID:");
		study.setForeground(Color.LIGHT_GRAY);
		pan2.add(study);
		final JTextField tf2 = new JTextField(12);
		tf2.setEnabled(false);
		pan2.add(tf2);
		JLabelAntialiased session = new JLabelAntialiased("Session:");
		session.setForeground(Color.LIGHT_GRAY);
		pan2.add(session);
		final JTextField tf3 = new JTextField(3);
		tf3.setEnabled(false);
		pan2.add(tf3);
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan2);
		JPanel pan3 = new JPanel();
		pan3.setLayout(new FlowLayout(FlowLayout.LEFT));
		pan3.add(new JLabelAntialiased("ICG-V Distance (mm):"));
		ams.webSocketClient.send("cmd !E_DISTANCE;");
		try {
			int i = 0;
			while (!name.equals("E_DISTANCE") && i < 25) {
				Thread.sleep(wait2);
				sId = ams.webSocketClient.getSetting();
				if (sId != "" && sId != null) {
					name = sId.split("=", 3)[0]; 
					eDistance = sId.split("=", 3)[1];
				}
				i++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final JTextField ftf = new JTextField();
		ftf.setColumns(3);
		if (name.equals("E_DISTANCE"))
			ftf.setText(eDistance);
		else
			System.out.println("name " + name);
		pan3.add(ftf);
		pan3.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(pan3);
		JLabel comment = new JLabel("Comment");
		comment.setForeground(Color.LIGHT_GRAY);
		newPanel.add(comment);
		JTextArea textArea = new JTextArea();
		textArea.setColumns(20);
        textArea.setLineWrap(true);
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(textArea);
		rightPanel.setLayout(new BorderLayout());

		onlinebut = new JButton("Online Graph");
		onlinebut.setEnabled(false);
		onlinebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopThread();
				OnlineDialog7 diag = new OnlineDialog7(ams, DeviceDialog7.this, null);
				diag.setVisible(true);
			}
		});


		//start / stop acq button
		startStopButton = new JButton("N/A");
		startStopButton.setEnabled(false);
		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String idText = tf.getText();
				String idText2 = ftf.getText();
				int icgVmm = 0;
				try {
					icgVmm = Integer.valueOf(ftf.getText());
					doStartOrStop(idText, idText2, icgVmm);
				} catch (NumberFormatException e) {
					doStartOrStop(idText, idText2, icgVmm);
					e.printStackTrace();
				}
			}
		});
		startStopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(startStopButton);
		onlinebut.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(onlinebut);
		//setdevtimebut = new JButton("Set Device Time To Computer Time");
		setdevtimebut = new JButton("Sync");
		setdevtimebut.setEnabled(false);
		setdevtimebut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Long now = new Date().getTime() / 1000;
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				ams.webSocketClient.send("cmd " + now.toString() + "T");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
				}
			}
		});
		//newPanel.add(setdevtimebut);
		//setPhaseBut = new JButton("Set respiration phase");
		setPhaseBut = new JButton("Set");
		setPhaseBut.setEnabled(true);
		setPhaseBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setPhase();
			}
		});
		//newPanel.add(setPhaseBut);

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

		wifiShutdownBut = new JButton("N/A");
		wifiShutdownBut.setEnabled(false);
		wifiShutdownBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doShutdown();
			}
		});
		wifiShutdownBut.setAlignmentX(Component.LEFT_ALIGNMENT);
		newPanel.add(wifiShutdownBut);

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
		String[] options = {"22", "45", "67", "90", "112", "135", "157"};
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
				if (!ams.webSocketClient.isOpen()) {
					connectionLost();
					if (isGivenUp())
						return;
				}		
				ams.webSocketClient.send("cmd " + phaseCB.getSelectedItem() + "P");
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
		diag.getRootPane().setDefaultButton(saveButton);//ENTER will hit button Save
		diag.setLocationRelativeTo(this);
		diag.setVisible(true);
		// startThread();
	}
}
