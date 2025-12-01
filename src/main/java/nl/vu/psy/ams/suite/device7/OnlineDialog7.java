package nl.vu.psy.ams.suite.device7;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// import com.mathworks.toolbox.javabuilder.MWException;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.OnlineDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.gui.tabs.RelativeLayout;
import nl.vu.psy.ams.suite.gui.tabs.inspect.YawPitchRollVisualizer;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * Dialog that shows an OnlineDrawer for each available online signal.
 */
public class OnlineDialog7 extends JFrame implements ItemListener {

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
				boolean redraw = odECG.updateIfNeeded(newTime - oldTime);
				oldTime = newTime;
				if (redraw)
					outerPanel.repaint();
				xAxis.repaint();
				try {
					Thread.sleep(100);
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
	public YAxis yAxis;
	private OnlineDrawer odECG;
	private OnlineDataGetter7 odg;
	private RedrawThread rdt;
	private XAxis xAxis;
	private JButton sendMarker;
	private JButton displayHR;
	private JButton pauseGraph;

	private DeviceDialog7 parentFrame;
	private DeviceDialogBLE parentFrame2;
	private AmsDevice7 ams;
	private JList<?> xAxisScale;
	private ArrayList<JCheckBox> toggleBoxes;
	private ArrayList<ExpandingPanel> expPanels;
	private JPanel togglePanel, panel = new JPanel();
	private JPanel outerPanel = new JPanel(new BorderLayout());
	ArrayList<OnlineDrawer> odList = new ArrayList<OnlineDrawer>();
	ArrayList<OnlineDrawer> odListF = new ArrayList<OnlineDrawer>();
	private boolean firstItem = true;
	private final String[] filteredChans = { "ECG", "V2ecg", "V3ecg", "magX", "magY", "magZ", "MXR", "MYR", "MZR",
			"GyroX", "GyroY", "GyroZ", "StepInstances", "SCL" };
	final String[] defaultChans = { "ECG", "DZDT", "Z0", "MXR" };
	final String[] AChans = { "ECG", "V2ecg", "V3ecg", "DZDT", "Z0", "Visrc", "T" };
	final String[] MChans = { "MXR", "MYR", "MZR", "GyroX", "GyroY", "GyroZ", "Temp", "Pos Visualization", "Yaw",
			"Roll", "Pitch", "StepInstances" };
	final String[] DChans = { "P_sc", "T_sc" };
	final String[] GChans = { "magX", "magY", "magZ", "Pos Visualization", "Yaw", "Roll", "Pitch", "StepInstances" };
	private JTextField text1 = new JTextField(32);
	private Vector<TimeShowOverlay> tsOverlays;
	private OnlineDrawer r = null, p = null, ya = null;
	private YawPitchRollVisualizer vis = null;

	public OnlineDialog7(final AmsDevice7 ams, DeviceDialog7 parent, DeviceDialogBLE parentb) {
		super("Online Graph");
		this.parentFrame = parent;
		this.parentFrame2 = parentb;
		this.ams = ams;
		if (parent != null)
			parent.setEnabled(false);
		if (parentb != null)
			parentb.setEnabled(false);
		setLayout(new BorderLayout());
		expPanels = new ArrayList<ExpandingPanel>();
		toggleBoxes = new ArrayList<JCheckBox>();
		togglePanel = new JPanel();
		togglePanel.setLayout(new BoxLayout(togglePanel, BoxLayout.Y_AXIS));
		togglePanel.add(new JLabel("Show/hide:"));

		xAxis = new XAxis(false, true, 2);
		xAxis.setPopupEnabled(false);
		xAxis.getTimeFormatSeconds().setSelected(true);
		xAxis.setTrueSeconds(true);
		xAxis.setIsLive(true);
		xAxis.setLeftTimeF(-10000000);
		xAxis.setRightTimeF(0);
		RelativeLayout rl = new RelativeLayout(RelativeLayout.Y_AXIS);
		rl.setFill(true);
		panel.setLayout(rl);

		vis = new YawPitchRollVisualizer(ya, p, r);
		tsOverlays = new Vector<TimeShowOverlay>();
		for (Ams7fsChannelInfo ch : ams.channelInfo) {
			if (ch.getSzID().equals("DZDT"))
				addChannel(ch, true);
			else
				addChannel(ch, false);
		}
		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);
		for (OnlineDrawer o : odListF)
			if (o.getName() == "ECG")
				odECG = o;
		vis.setYawDrawer(ya);
		vis.setPitchDrawer(p);
		vis.setRollDrawer(r);
		ExpandingPanel exp = new ExpandingPanel(vis, false, "Pos Visualization");
		expPanels.add(exp);
		addToggle("Pos Visualization");
		panel.add(exp, 1F);

		for (int i = 0; i < toggleBoxes.size(); i++) {
			exp = expPanels.get(i);
			Container expParent = exp.getParent();
			LayoutManager2 layout = (LayoutManager2) expParent.getLayout();
			if (Arrays.stream(defaultChans).anyMatch(toggleBoxes.get(i).getText()::equals)) {
				exp.setVisible(true);
				toggleBoxes.get(i).setSelected(true);
				layout.addLayoutComponent(exp, 1F);
			} else {
				exp.setVisible(false);
				toggleBoxes.get(i).setSelected(false);
				layout.addLayoutComponent(exp, null);
			}
			exp.revalidate();
			exp.repaint();
		}
		selectChannels();

		sendMarker = new JButton("Send Marker");
		displayHR = new JButton("Show Heart Rate");
		pauseGraph = new JButton("Pause Graph");
		odg = new OnlineDataGetter7(ams, odList, odListF, this, vis); //
		panel.add(xAxis.getPanel());
		outerPanel.add(panel, BorderLayout.CENTER);

		Integer[] scales = { 10, 30, 60, 120, 300 };
		xAxisScale = new JList<Integer>(scales);
		xAxisScale.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					xAxis.setLeftTimeF((Integer) xAxisScale.getSelectedValue() * -1000000.0);
				}
			}
		});
		xAxisScale.setSelectedIndex(0);

		// ----------------- Send Markers to the Device------------------
		sendMarker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				if (parentFrame != null) {
					if (!ams.webSocketClient.isOpen()) {
						parentFrame.connectionLost();
						if (parentFrame.isGivenUp()) {
							closeDialog();
							return;
						}
					}
					String s = text1.getText();
					if (s.equals(""))
						ams.webSocketClient.send("cmd m");
					else
						ams.webSocketClient.send("cmd !MARKER=" + s + ";");
					// remove matlab dependency
					// } else {
					// String s = text1.getText();
					// if (s.equals(""))
					// writeBleQuietly("m");
					// else
					// writeBleQuietly("!MARKER=" + s + ";");

				}
			}
		});
		// --------------------------------------------------------------

		// ----------------- Display Heart Rate------------------
		displayHR.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				displayHR.setText(odECG.getText());
				if (odECG.showHeartRate() == false) {
					odECG.showHeartRate(true);
				} else {
					odECG.showHeartRate(false);
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

		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		pan.add(Box.createVerticalGlue());
		JButton but;

		but = new JButton("Show all");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < toggleBoxes.size(); i++) {
					ExpandingPanel exp = expPanels.get(i);
					Container parent = exp.getParent();
					LayoutManager2 layout = (LayoutManager2) parent.getLayout();
					exp.setVisible(true);
					toggleBoxes.get(i).setSelected(true);
					layout.addLayoutComponent(exp, 1F);
					exp.revalidate();
					exp.repaint();
				}
				selectChannels();
			}
		});
		togglePanel.add(but);
		but = new JButton("Hide all");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < toggleBoxes.size(); i++) {
					ExpandingPanel exp = expPanels.get(i);
					Container parent = exp.getParent();
					LayoutManager2 layout = (LayoutManager2) parent.getLayout();
					exp.setVisible(false);
					toggleBoxes.get(i).setSelected(false);
					layout.addLayoutComponent(exp, null);
					exp.revalidate();
					exp.repaint();
				}
				selectChannels();
			}
		});
		togglePanel.add(but);
		but = new JButton("Invert selection");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < toggleBoxes.size(); i++) {
					ExpandingPanel exp = expPanels.get(i);
					Container parent = exp.getParent();
					LayoutManager2 layout = (LayoutManager2) parent.getLayout();
					if (!toggleBoxes.get(i).isSelected()) {
						exp.setVisible(true);
						toggleBoxes.get(i).setSelected(true);
						layout.addLayoutComponent(exp, 1F);
					} else {
						exp.setVisible(false);
						toggleBoxes.get(i).setSelected(false);
						layout.addLayoutComponent(exp, null);
					}
					exp.revalidate();
					exp.repaint();
				}
				selectChannels();
			}
		});
		togglePanel.add(but);
		pan.add(togglePanel);
		pan.add(Box.createVerticalGlue());
		JLabel scaleLabel = new JLabel("<html>Set time on<br>x-axis (sec)</html>");
		scaleLabel.setAlignmentX(CENTER_ALIGNMENT);
		pan.add(scaleLabel);
		pan.add(xAxisScale);
		pan.add(Box.createVerticalGlue());

		outerPanel.add(new JScrollPane(pan), BorderLayout.EAST);
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

		odg.setStarted(true);
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

		if (parent != null)
			setLocationRelativeTo(parent);
		if (parentb != null)
			setLocationRelativeTo(parentb);

		// Set up keybinding to show variance in screen
		outerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("V"),
				"toggleVarianceShow");
		outerPanel.getActionMap().put("toggleVarianceShow", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// if (Utils.askForExpertPassword(false))
				for (OnlineDrawer od : odList)
					od.toggleVarianceInScreen();
				for (OnlineDrawer od : odListF)
					od.toggleVarianceInScreen();

			}
		});
		// Set up keybindings to autoscale Y axis
		outerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true),
				"autoscaleAll");
		outerPanel.getActionMap().put("autoscaleAll", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (OnlineDrawer od : odList)
					od.getYAxis().autoScale();
			}
		});
		outerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true),
				"autoscaleAllRaw");
		outerPanel.getActionMap().put("autoscaleAllRaw", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (OnlineDrawer od : odList)
					od.getYAxis().autoScale(0);
			}
		});
	}

	private void addToggle(String chan) {
		JCheckBox show = new JCheckBox(chan);
		show.setSelected(true);
		show.addItemListener(this);
		toggleBoxes.add(show);
		togglePanel.add(show);

	}

	private void addChannel(Ams7fsChannelInfo s, boolean dt) {
		Graph graph = new Graph(xAxis);
		YAxis yAxis;
		if (dt)
			graph.setName("DZ/DT");
		else
			graph.setName(s.getSzID());
		yAxis = new YAxis(1, 0, s.getSzID() + " [" + s.getSzUnit() + "]");
		yAxis.setBottomValue(Integer.MIN_VALUE);
		yAxis.setTopValue(Integer.MAX_VALUE);
		graph.setActiveYAxis(yAxis);

		EventOverlay evo = new EventOverlay(graph);
		evo.setEvents(ams.events);
		if (firstItem) {
			evo.setShowCode(true);
			graph.addOverlay(evo);
			firstItem = false;
		} else {
			graph.addUnderlay(evo);
		}
		boolean useTicks = true, isReal = false; // !(s.getSzID().equals("Tickdiff ADC"));
		if (s.getFormula().length() >= 1 || s.getSzID().equals("StepInstances"))
			isReal = true;
		OnlineDrawer od = new OnlineDrawer(s.getSzID(), yAxis, false, isReal, useTicks);
		if (s.getSzID().equals("Roll"))
			r = od;
		if (s.getSzID().equals("Pitch"))
			p = od;
		if (s.getSzID().equals("Yaw"))
			ya = od;
		od.setDT(dt);
		// od.setECG(s.getSzID().equals("ECG"));
		od.setDivider((int) s.getDwDivider());
		od.setSlopeAndConstant(s.getRealSlope(), s.getRealConstant());
		odList.add(od);
		od.connectToGraph(graph);
		if (Arrays.stream(filteredChans).anyMatch(s.getSzID()::equals)) {
			OnlineDrawer odF = new OnlineDrawer(s.getSzID(), yAxis, false, (s.getFormula().length() >= 1), useTicks,
					new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			odF.setECG(s.getSzID().equals("ECG"));
			odF.setMeanMot(s.getSzID().equals("StepInstances"));
			odF.setFiltered(true);
			odF.setDivider((int) s.getDwDivider());
			odF.setSlopeAndConstant(s.getRealSlope(), s.getRealConstant());
			odListF.add(odF);
			odF.connectToGraph(graph);
		}
		TimeShowOverlay tso = new TimeShowOverlay(graph, vis);
		graph.addOverlay(tso);
		tsOverlays.add(tso);
		yAxis.autoScale();
		ExpandingPanel exp = new ExpandingPanel(graph.getPanel(), false, s.getSzID());
		expPanels.add(exp);
		if (dt)
			addToggle("DZDT");
		else
			addToggle(s.getSzID());
		panel.add(exp, 1F);
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
			if (parentFrame != null) {
				parentFrame.setEnabled(true);
				parentFrame.startThread();
				parentFrame.recalcThread.setUDP(true);
			}
			if (parentFrame2 != null) {
				parentFrame2.setEnabled(true);
				parentFrame2.startThread();
				parentFrame2.recalcThread.setDataStream(true);
			}
		}
		super.setVisible(vis);
	}

	protected void closeDialog() {
		setVisible(false);
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		Object source = arg0.getItemSelectable();

		for (int i = 0; i < toggleBoxes.size(); i++) {
			if (source == toggleBoxes.get(i)) {
				ExpandingPanel exp = expPanels.get(i);
				Container parent = exp.getParent();
				LayoutManager2 layout = (LayoutManager2) parent.getLayout();
				if (arg0.getStateChange() == ItemEvent.SELECTED) {
					exp.setVisible(true);
					layout.addLayoutComponent(exp, 1F);
				} else if (arg0.getStateChange() == ItemEvent.DESELECTED) {
					exp.setVisible(false);
					layout.addLayoutComponent(exp, null);
				}
				exp.revalidate();
				exp.repaint();
				break;
			}
		}
		selectChannels();
	}

	// boolean writeBleQuietly(String input) {
	// try {
	// ams.obj.writeData(1, ams.a, ams.bleDialog.deviceName,
	// "60462F12-9543-9999-12C8-58B459A2712D",
	// "5C3A659E-897E-45E1-B016-007107C96DF7", input);
	// return true;
	// } catch (MWException e) {
	// return false;
	// }
	// }

	// boolean writeBleChanSelect(String input) {
	// try {
	// ams.obj.writeData(1, ams.a, ams.bleDialog.deviceName,
	// "60462F12-9543-9999-12C8-58B459A2712D",
	// "763A659E-897E-45E1-B016-007107C96DF7", input);
	// return true;
	// } catch (MWException e) {
	// return false;
	// }
	// }

	void selectChannels() {
		// remove matlab dependency
		// boolean A_on = false, M_on = false, G_on = false, D_on = false;
		// for (int i = 0; i < toggleBoxes.size(); i++) {
		// if (toggleBoxes.get(i).isSelected()) {
		// if (Arrays.stream(AChans).anyMatch(toggleBoxes.get(i).getText()::equals))
		// A_on = true;
		// if (Arrays.stream(MChans).anyMatch(toggleBoxes.get(i).getText()::equals))
		// M_on = true;
		// if (Arrays.stream(DChans).anyMatch(toggleBoxes.get(i).getText()::equals))
		// D_on = true;
		// if (Arrays.stream(GChans).anyMatch(toggleBoxes.get(i).getText()::equals))
		// G_on = true;
		// }
		// }
		// if (A_on)
		// writeBleChanSelect("At");
		// else
		// writeBleChanSelect("Af");
		// if (M_on)
		// writeBleChanSelect("Mt");
		// else
		// writeBleChanSelect("Mf");
		// if (D_on)
		// writeBleChanSelect("Dt");
		// else
		// writeBleChanSelect("Df");
		// if (G_on)
		// writeBleChanSelect("Gt");
		// else
		// writeBleChanSelect("Gf");
	}
}
