package nl.vu.psy.ams.suite.device;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.OnlineDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.tools.SCLValueConvertor;
import nl.vu.psy.ams.suite.tools.Utils;
import nl.vu.psy.ams.suite.tools.VUDAMSDebugSettings;

import com.google.gson.Gson;

/*
 * Dialog that shows a OnlineDrawer, a reconnect button,
 * and a list of available online signals.
 */
public class OnlineDialog extends JFrame {

	private class RedrawThread implements Runnable {

		private boolean drawing = true;

		public synchronized boolean isDrawing() {
			return drawing;
		}

		@Override
		public void run() {
			long oldTime, newTime;
			oldTime = Calendar.getInstance().getTimeInMillis();
			while (isDrawing()) {
				newTime = Calendar.getInstance().getTimeInMillis();
				boolean redraw = od.updateIfNeeded(newTime - oldTime);
				oldTime = newTime;
				if (redraw)
					g.repaint();
				xAxis.repaint();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		public synchronized void setDrawing(boolean draw) {
			this.drawing = draw;
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AmsDevice ams;
	public YAxis yAxis;
	private OnlineDrawer od;
	private OnlineDataGetter odg;
	private Graph g;
	private List<String> hiddenChannels = Arrays.asList("A25", "SCLraw", "buffer", "YMT", "XMT", "MXD", "MXA", "MYA",
			"MZA", "Ires",
			"BATraw", "Z0raw", "LDRraw", "LDR");
	private RedrawThread rdt;
	private XAxis xAxis;
	private JButton sendMarker;
	private JButton displayHR;
	private JButton pauseGraph;

	private HashMap<String, Integer> chanMap;
	private DeviceDialog parentFrame;
	private JList<?> list;
	private JList<?> xAxisScale;
	private boolean marker = false;
	private JTextField text1 = new JTextField(32);
	private double defaultBottom = -100.0;
	private double defaultTop = 100.0;

	public OnlineDialog(final AmsDevice ams, DeviceDialog parent) {
		super("Online Graph");
		this.parentFrame = parent;
		parent.setEnabled(false);
		this.ams = ams;
		xAxis = new XAxis(false, true, 2);
		xAxis.setPopupEnabled(false);
		xAxis.getTimeFormatSeconds().setSelected(true);
		xAxis.setTrueSeconds(true);
		xAxis.setIsLive(true);
		g = new Graph(xAxis);
		yAxis = new YAxis(defaultBottom, defaultTop, defaultBottom, defaultTop, "Online");
		g.setActiveYAxis(yAxis);
		xAxis.setLeftTimeF(-10000000);
		xAxis.setRightTimeF(0);
		yAxis.setBottomValue(defaultBottom);
		yAxis.setTopValue(defaultTop);
		g.addOverlay(new TimeShowOverlay(g));

		boolean showVariance = false;
		String inString;
		File curFile;
		Gson gson = new Gson();
		VUDAMSDebugSettings debugSettings = null;

		curFile = new File(System.getProperty("user.dir"), "VUDAMSDebug.json");
		if (curFile.exists()) {
			inString = Utils.readStringFromFile(curFile);
			debugSettings = gson.fromJson(inString, VUDAMSDebugSettings.class);

			if (debugSettings.getOnlinehiddenchannels() == 1) { // Show Hidden Channels
				hiddenChannels = Arrays.asList();
			}
			if (debugSettings.getShowVariance() == 1) { // Show Variance
				showVariance = true;
			}

			System.out.println("Debug File exists_ " + debugSettings.getOnlinehiddenchannels());
		}
		od = new OnlineDrawer("Online", yAxis, showVariance, false, false);

		sendMarker = new JButton("Send Marker");

		displayHR = new JButton("Show Heart Rate");

		pauseGraph = new JButton("Pause Graph");

		odg = new OnlineDataGetter(ams, od, sendMarker, this); //

		setLayout(new BorderLayout());

		JPanel outerPanel = new JPanel(new BorderLayout());

		od.connectToGraph(g);

		JPanel pan = new JPanel(new BorderLayout());

		pan.add(g.getPanel(), BorderLayout.CENTER);
		pan.add(xAxis.getPanel(), BorderLayout.SOUTH);

		outerPanel.add(pan, BorderLayout.CENTER);

		ArrayList<String> onlineChans = new ArrayList<String>();
		chanMap = new HashMap<String, Integer>();
		for (int i = 0; i < ams.getSettings().channels.length; i++) {
			AmsChannelInfo chan = ams.getSettings().channels[i];
			if (chan.dwDivider != 0 && chan.wBufSize > 0) {
				String chanName = "";
				char curChar;
				boolean done = false;
				for (int j = 0; j < chan.szID.length; j++) {
					curChar = (char) chan.szID[j];
					if (curChar != 0 && done == false) {
						chanName += curChar;
					} else {
						done = true;
					}
				}
				// System.out.println(chanName);
				if (hiddenChannels.contains(chanName) == false) {
					onlineChans.add(chanName);
					chanMap.put(chanName, i);
					if (chanName.equals("DZ")) {
						onlineChans.add("DZ/DT");
						chanMap.put("DZ/DT", i);
					}
					if (chanName.equals("SCL")) {
						SCLValueConvertor.initialize(chan.nBits, chan.lMinValue, chan.lMaxValue, chan.lMinMaxDivider,
								(ams.getSettings().dwSettingsFlags & AmsDeviceConstants.AMSII_FLAG_SCLAC) != 0, 0, 0);
					}
				}
			}
		}
		list = new JList<Object>(onlineChans.toArray()); //
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		if (onlineChans.contains("ECG"))
			list.setSelectedValue("ECG", true);
		else if (onlineChans.contains("SCL"))
			list.setSelectedValue("SCL", true);
		else
			list.setSelectedValue(onlineChans.toArray()[0], true);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					int sel = chanMap.get(list.getSelectedValue());
					setChannel(sel, list.getSelectedValue().equals("DZ/DT"), list.getSelectedValue().equals("SCL"));
				}
			}
		});

		Integer[] scales = { 10, 30, 60, 120, 300 };
		xAxisScale = new JList<Integer>(scales);
		xAxisScale.setSelectedIndex(0);
		xAxisScale.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					xAxis.setLeftTimeF((Integer) xAxisScale.getSelectedValue() * -1000000.0);
				}
			}
		});

		// ----------------- Send Markers to the Device------------------
		sendMarker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				String s = text1.getText();
				ams.sendMarkersToDevice(s);
			}
		});
		// --------------------------------------------------------------

		// ----------------- Display Heart Rate------------------

		displayHR.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				displayHR.setText(od.getText());
				if (od.showHeartRate() == false) {
					od.showHeartRate(true);
					od.setECG(true);
				} else {
					od.showHeartRate(false);
				}
			}
		});
		// --------------------------------------------------------------
		// ---------------- Pause/Resume Graph Scrolling-----------------

		pauseGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				int newLeftTime = (Integer) xAxisScale.getSelectedValue() * -1000000;
				if (xAxis.getRightTime() == 0) {
					pauseGraph.setText("Resume Graph");
					xAxis.setRightTimeF(-100000);
					xAxis.setLeftTimeF(newLeftTime - 100000);

				} else {
					pauseGraph.setText("Pause Graph");
					xAxis.setRightTimeF(0);
					xAxis.setLeftTimeF(newLeftTime);
				}
			}
		});
		// --------------------------------------------------------------

		pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		pan.add(Box.createVerticalGlue());
		pan.add(list);
		pan.add(Box.createVerticalGlue());
		JLabel scaleLabel = new JLabel("<html>Set time on<br>x-axis (sec)</html>");
		scaleLabel.setAlignmentX(CENTER_ALIGNMENT);
		pan.add(scaleLabel);
		pan.add(xAxisScale);
		pan.add(Box.createVerticalGlue());

		outerPanel.add(pan, BorderLayout.EAST);

		pan = new JPanel();

		// ------------To send Markers during Online monitoring (works good with Serial
		// connection, not recommended for bluetooth mode)
		JTextPane t = new JTextPane();
		t.setText("Enter Marker Message");
		text1.setColumns(32);

		pan.add(t);
		pan.add(text1);
		pan.add(sendMarker);

		pan.add(displayHR);
		pan.add(pauseGraph);
		outerPanel.add(pan, BorderLayout.SOUTH);
		// -------------------------------------------------------------------------------

		odg.setStarted(true);
		setChannel(chanMap.get(list.getSelectedValue()), false, false);

		odg.start();

		rdt = new RedrawThread();

		Thread thrd = new Thread(rdt);

		thrd.start();

		add(outerPanel, BorderLayout.CENTER);

		// --------- Application to fit the screen------------------------------
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension scrnsize = toolkit.getScreenSize();
		Dimension frameSize = new Dimension(1280, 720);
		frameSize.width = (int) scrnsize.getWidth();
		frameSize.height = (int) (4 * scrnsize.getHeight() / 5);

		setBounds(0, 0, frameSize.width, frameSize.height);

		setLocationRelativeTo(parent);

		// Set up keybinding to show variance in screen
		outerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("V"),
				"toggleVarianceShow");
		outerPanel.getActionMap().put("toggleVarianceShow", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// if (Utils.askForExpertPassword(false))
				od.toggleVarianceInScreen();

			}
		});
		// Set up keybinding to autoscale Y axis
		outerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true),
				"autoscaleAll");
		outerPanel.getActionMap().put("autoscaleAll", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				yAxis.autoScale();
			}
		});
	}

	public void reconnect() {

		int sel = chanMap.get(list.getSelectedValue());
		ams.tryToReconnect();
		if (ams.isConnectionOpen()) {
			System.out.println("Connection Open:_ " + " " + "Is Serial Connection ? :_ " + ams.serialConn + "---"
					+ ams.getParameterFromDevice(AmsDeviceConstants.AMSII_PARMINDEX_SERIALNR));
			odg = new OnlineDataGetter(ams, od, sendMarker, this);
			setChannel(sel, list.getSelectedValue().equals("DZ/DT"), list.getSelectedValue().equals("SCL"));
			list.getSelectedIndex();
			odg.setStarted(true);
			odg.start();
		}
	}

	public void setChannel(int chan, boolean dt, boolean scl) {

		AmsChannelInfo ci = ams.getSettings().channels[chan];

		int low = 0;
		int high = (int) (Math.pow(2, ci.nBits) - 1);
		double lv = (double) ci.lMinValue / ci.lMinMaxDivider;
		double hv = (double) ci.lMaxValue / ci.lMinMaxDivider;
		odg.setNewSlopeAndConstant(low, high, lv, hv);
		String axisTitle = "";
		for (int i = 0; i < 12; i++) {
			if (ci.szID[i] == 0)
				break;
			axisTitle += (char) ci.szID[i];
		}

		axisTitle += " [";

		for (int i = 0; i < 12; i++) {
			if (ci.szUnit[i] == 0)
				break;
			axisTitle += (char) ci.szUnit[i];
		}
		axisTitle += "]";
		odg.setNewAxisTitle(axisTitle);
		yAxis.setAxisTitle(axisTitle);

		odg.setChannel(chan);
		odg.setDT(dt);

		od.setECG(list.getSelectedValue().equals("ECG"));
		displayHR.setEnabled(list.getSelectedValue().equals("ECG"));
		odg.setDZ(list.getSelectedValue().equals("DZ"));
		odg.setMYA(list.getSelectedValue().equals("MYA"));
		odg.setSCL(list.getSelectedValue().equals("SCL"));
		od.setSCL(list.getSelectedValue().equals("SCL"));

		if (dt) {
			yAxis.setAxisTitle("-DZ/DT [\u2126/s]");
			odg.setNewAxisTitle("-DZ/DT [\u2126/s]");
		}
		// if (scl) {
		// xAxis.setLeftTimeF(-60000000);
		// xAxis.setRightTimeF(0);
		// } else {
		// xAxis.setLeftTimeF(-10000000);
		// xAxis.setRightTimeF(0);
		// }

		int newDiv = (int) ci.dwDivider;
		if (newDiv < 10)
			newDiv = 10;

		odg.setCurDiv(newDiv);
		od.setDivider(newDiv);

		g.addUnderlay(new GridOverlay(g));

		yAxis.repaint();

		g.repaint();

	}

	@Override
	public void setVisible(boolean vis) {
		if (vis == false) {
			odg.setStarted(false);
			try {
				odg.join();
			} catch (InterruptedException e) {
			}
			rdt.setDrawing(false);
			parentFrame.setEnabled(true);
			if (ams.getParameterFromDevice(AmsDevice.PAR_DEVICEID) != null) {
				parentFrame.startThread();
			} else {
				ams.tryToReconnect();
				parentFrame.startThread();
			}

		}
		super.setVisible(vis);
	}

	protected void closeDialog() {
		setVisible(false);
	}

	public void setMarker(boolean state) {
		this.marker = state;
	}

	public boolean drawMarker() {
		return marker;
	}

}
