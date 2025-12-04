package nl.vu.psy.ams.suite.gui.tabs.inspect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.LayoutManager2;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
// import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDoubleDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.FormulaDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.StepOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The inspect data tab
 */

public class InspectTab extends AmsTab implements ItemListener, AutoCloseable {

	private static final long serialVersionUID = 1L;
	private static InspectTab instance;

	public static InspectTab getInstance() {
		if (instance == null) {
			instance = new InspectTab();
		}
		return instance;
	}

	public static InspectTab getInstanceOrNull() {
		return instance;
	}

	public static InspectTab getNewInstance() {
		instance = null;
		instance = new InspectTab();
		return instance;
	}

	private boolean firstItem;
	private ArrayList<Ams7fsChannelInfo> addedChannels;
	public ArrayList<Graph> graphSet;
	private ArrayList<JCheckBox> toggleBoxes;
	private ArrayList<ExpandingPanel> expPanels;
	private JPanel togglePanel, ecgPanel, icgPanel, accPanel, rotPanel, magPanel, qcPanel, otherPanel;
	private Vector<TimeShowOverlay> tsOverlays;
	private String[] defaultChans;
	private File file;
	private Vector<BinaryDataDrawer> bds;
	private Vector<BinaryDoubleDataDrawer> bdds;
	private Vector<FormulaDrawer> fds;
	private BinaryDoubleDataDrawer y, p, r;
	private YawPitchRollVisualizer vis;

	private InspectTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new InspectToolBar(this));
	}

	private void addToggle(SignalNames chan) {
		JCheckBox show = new JCheckBox(chan.getName());
		show.addItemListener(this);
		toggleBoxes.add(show);
		switch (chan.getCategory()) {
			case "ECG":
				ecgPanel.add(show);
				break;
			case "ICG":
				icgPanel.add(show);
				break;
			case "Acceleration":
				accPanel.add(show);
				break;
			case "Rotation":
				rotPanel.add(show);
				break;
			case "Magnetic field":
				magPanel.add(show);
				break;
			case "QC":
				qcPanel.add(show);
				break;
			case "Ohter":
				otherPanel.add(show);
				break;
			default:
				otherPanel.add(show);
		}
	}

	private void addChannel(Ams7fsChannelInfo s, SignalNames name) {

		if (s.getSzID().equals("BAT") || s.getSzID().equals("Tickdiff_ADC") || s.getSzID().equals("Tickdrift_ADC")) {
			if (AppSettings.getInstance().getIntProperty(Settings.SHOWBATSIGNAL) == 0) {
				return;
			}
		}

		Graph graph = new Graph(mainXAxis);
		YAxis yAxis;
		graph.setName(s.getSzID());
		graphSet.add(graph);
		String axisLabel = name.getName();
		if (s.getSzID().equals("DZDT"))
			axisLabel = "-" + axisLabel;
		if (s.getSzID() == "Yaw" || s.getSzID() == "Pitch" || s.getSzID() == "Roll") {
			yAxis = new YAxis(0, 1, 0, 1, s.getSzID());
		} else if (s.getRealSlope() == 0) {
			yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
					(double) s.getlMinValue()
							/ s.getlMinMaxDivider(),
					(double) s.getlMaxValue() / s.getlMinMaxDivider(), axisLabel);
		} else if (s.getFormula().length() < 1) {
			yAxis = new YAxis(s.getRealSlope(), s.getRealConstant(), axisLabel);
		} else {
			yAxis = new YAxis(1, 0, axisLabel);
		}
		yAxis.setBottomValue(Integer.MIN_VALUE);
		yAxis.setTopValue(Integer.MAX_VALUE);
		graph.setActiveYAxis(yAxis);
		graph.addUnderlay(new GridOverlay(graph));
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (s.getSzID().equals("ECG")) {
			ArtefactSet arts = cod.getECGArtefacts();
			LabelOverlay lov = new LabelOverlay(graph);
			lov.setLabelConfig(AmsLabelConfiguration.getArtefactConfiguration());
			lov.setEditable(false);
			lov.setLabels(arts);
			lov.setFillFactor(0.25);
			graph.addOverlay(lov);
		}
		// NOTE: Stairs labels overlay removed - stairs detection is now handled by
		// StairsClassifier
		// and will be integrated with posture labels in future work
		if (s.getSzID().equals("MXR")) {
			LabelSet arts = CurrentOpenData.getInstance().getPostureLabels();
			LabelOverlay lov = new LabelOverlay(graph);
			lov.setLabelConfig(AmsLabelConfiguration.getPostureLabelConfiguration());
			lov.setEditable(false);
			lov.setLabels(arts);
			lov.setFillFactor(0.25);
			graph.addOverlay(lov);
		}
		// if (s.getSzID().equals("MZR")) {
		// LabelSet arts = CurrentOpenData.getInstance().getSpeechLabels();
		// LabelOverlay lov = new LabelOverlay(graph);
		// lov.setLabelConfig(AmsLabelConfiguration.getSpeechLabelConfiguration());
		// lov.setEditable(false);
		// lov.setLabels(arts);
		// lov.setFillFactor(0.25);
		// graph.addOverlay(lov);
		// }
		// if (s.getSzID().equals("MotilityIntensity")) {
		// LabelSet arts = cod.getActivityLabels();
		// LabelOverlay lov = new LabelOverlay(graph);
		// lov.setLabelConfig(AmsLabelConfiguration.getActivityLabelConfiguration());
		// lov.setEditable(false);
		// lov.setLabels(arts);
		// lov.setFillFactor(0.25);
		// graph.addOverlay(lov);
		// }

		if (firstItem) {
			EventOverlay evo = new EventOverlay(graph);
			evo.setShowCode(true);
			graph.addOverlay(evo);
			firstItem = false;
		} else {
			graph.addUnderlay(new EventOverlay(graph));
		}

		if (s.getSzID().equals("Yaw") || s.getSzID().equals("Pitch") || s.getSzID().equals("Roll")
				|| s.getSzID().equals("Altitude") || s.getSzID().equals("MotilityIntensity")) {
			int samplerate;
			if (s.getSzID().equals("Altitude"))
				samplerate = 200000;
			else
				samplerate = 50000;
			File yFile = new File(CurrentOpenData.getInstance().getFilePath(), s.getSzID() + ".dbin");
			if (s.getSzID().equals("Yaw")) {
				y = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, samplerate,
						s.getTickFile());
				y.connectToGraph(graph);
			} else if (s.getSzID().equals("Pitch")) {
				p = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, samplerate,
						s.getTickFile());
				p.connectToGraph(graph);
			} else if (s.getSzID().equals("Roll")) {
				r = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, samplerate,
						s.getTickFile());
				r.connectToGraph(graph);
			} else if (s.getSzID().equals("MotilityIntensity")) {
				BinaryDoubleDataDrawer bddd = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, 60000000,
						s.getTickFile());
				bddd.connectToGraph(graph);
				bdds.add(bddd);
			} else {
				BinaryDoubleDataDrawer bddd = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, samplerate,
						s.getTickFile());
				bdds.add(bddd);
				bddd.connectToGraph(graph);
			}
		} else if (s.getFormula().length() < 1) {
			BinaryDataDrawer drawTest = new BinaryDataDrawer(s.getSzID(), yAxis);
			if (s.getSzID().equals("DZDT"))
				drawTest.setInvert(true);
			bds.add(drawTest);
			drawTest.connectToGraph(graph);
		} else {
			FormulaDrawer drawTest = new FormulaDrawer(s.getSzID(), yAxis);
			fds.add(drawTest);
			drawTest.connectToGraph(graph);
		}
		String[] filteredChans = { "magX", "magY", "magZ", "MXR", "MYR", "MZR", "GyroX", "GyroY", "GyroZ", "ECG",
				"DZDT", "V2ecg", "V3ecg", "AccelVectorMag", "P_sc", "T_sc", "SCL" };
		if (Arrays.stream(filteredChans).anyMatch(s.getSzID()::equals)) {
			if (s.getSzID().equals("P_sc") || s.getSzID().equals("T_sc")) {
				File yFile = new File(cod.getFilePath(), "FILT" + s.getSzID() + ".dbin");
				BinaryDoubleDataDrawer bddd = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, 200000.,
						s.getTickFile(),
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				bdds.add(bddd);
				bddd.connectToGraph(graph);
			} else if ((Utils.getExtension(cod.getDataFile()).equals("7fs")
					|| cod.getFileHeader().getDwHardwareVersion() == 7) && s.getSzID().equals("SCL")) {
				File yFile = new File(cod.getFilePath(), "FILT" + s.getSzID() + ".dbin");
				BinaryDoubleDataDrawer bddd = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, 1000.,
						s.getTickFile(),
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				bdds.add(bddd);
				bddd.connectToGraph(graph);
			} else {
				BinaryDataDrawer drawTest1 = new BinaryDataDrawer(s.getSzID(), "FILT" + s.getSzID(), yAxis,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				if (s.getSzID().equals("DZDT"))
					drawTest1.setInvert(true);
				drawTest1.connectToGraph(graph);
				bds.add(drawTest1);
				if (s.getSzID().equals("AccelVectorMag"))
					graph.addOverlay(new StepOverlay(graph));
			}
		} else if (s.getSzID().equals("Z0")
				&& (Utils.getExtension(cod.getDataFile()).equals("7fs")
						|| cod.getFileHeader().getDwHardwareVersion() == 7)) {
			BinaryDataDrawer drawTest1 = new BinaryDataDrawer(s.getSzID(), "DZ", yAxis,
					new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			bds.add(drawTest1);
			drawTest1.connectToGraph(graph);
			// BinaryDataDrawer drawTest2 = new BinaryDataDrawer(s.getSzID(), "FILTDZ",
			// yAxis,
			// Color.RED);
			// drawTest2.connectToGraph(graph);
		}
		yAxis.autoScale();
		TimeShowOverlay tso;
		if (s.getSzID().equals("StepInstances"))
			tso = new TimeShowOverlay(graph);
		else
			tso = new TimeShowOverlay(graph);
		graph.addOverlay(tso);
		tsOverlays.add(tso);
		ExpandingPanel exp = new ExpandingPanel(graph.getPanel(), false, s.getSzID());
		expPanels.add(exp);
		addToggle(name);
		panel.add(exp, 1F);
	}

	private void addVisualization() {
		vis = new YawPitchRollVisualizer(y, p, r);
		ExpandingPanel exp = new ExpandingPanel(vis, false, "Visualization");
		expPanels.add(exp);
		addToggle(SignalNames.VIS);
		panel.add(exp, 1F);
	}

	private void addChannelIfExists(SignalNames chanID) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (cod.channelExists(chanID.getChan())) {
			try {
				Ams7fsChannelInfo chan = cod.getChannelInfoFromID(chanID.getChan());
				addChannel(chan, chanID);
				addedChannels.add(chan);
			} catch (Exception e) {
			}
		}

	}

	public YawPitchRollVisualizer getVisualizer() {
		return vis;
	}

	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	public enum SignalNames {
		ECG("ECG", "ECG lead II (mV)", "ECG"),
		V2ecg("V2ecg", "ECG lead avF (mV)", "ECG"),
		V3ecg("V3ecg", "ECG lead I (mV)", "ECG"),
		DZDT("DZDT", "Impedance Cardiogram - dZ/dT (\u2126)", "ICG"),
		Z0("Z0", "Thorax impedance - Z0 (\u2126)", "ICG"),
		DZ("DZ", "Thorax impedance - DZ (\u2126)", "ICG"),
		MXR("MXR", "Acceleration forward (g)", "Acceleration"),
		XMT("XMT", "Acceleration forward (g)", "Acceleration"),
		MYR("MYR", "Acceleration right (g)", "Acceleration"),
		YMT("YMT", "Acceleration right (g)", "Acceleration"),
		MZR("MZR", "Acceleration up (g)", "Acceleration"),
		ZMT("ZMT", "Acceleration up (g)", "Acceleration"),
		GyroX("GyroX", "Rotation rate roll (\u00B0/s)", "Rotation"),
		GyroY("GyroY", "Rotation rate pitch (\u00B0/s)", "Rotation"),
		GyroZ("GyroZ", "Rotation rate yaw (\u00B0/s)", "Rotation"),
		magX("magX", "Magnetic field forward (Gauss)", "Magnetic field"),
		magY("magY", "Magnetic field right (Gauss)", "Magnetic field"),
		magZ("magZ", "Magnetic field up (Gauss)", "Magnetic field"),
		BAT("BAT", "Battery voltage (mV)", "QC"),
		Visrc("Visrc", "Source resistance (k\u2126)", "QC"),
		Tickdiff("Tickdiff_ADC", "ADC Tick difference (ms)", "QC"),
		Tickdrift("Tickdrift_ADC", "ADC Aggregated tick diff. (ms)", "QC"),
		T("T", "ADC Temperature (\u00B0C)", "QC"),
		Temp("Temp", "IMU Temperature (\u00B0C)", "QC"),
		T_sc("T_sc", "Barometer Temperature (\u00B0C)", "QC"),
		P_sc("P_sc", "Barometric pressure (hPa)", "QC"),
		SCL("SCL", "Skin conductance (\u00B5S)", "Other"),
		MYA("MYA", "Average motility (g)", "Other"),
		AccelVectorMag("AccelVectorMag", "Steps", "Other"),
		MotilityIntensity("MotilityIntensity", "Motility (MADxyz (millig))", "Other"),
		Altitude("Altitude", "Relative altitude (m)", "Other"),
		VIS("Pos Visualization", "Pos Visualization", "Other");

		private String chanId, prettyName, cat;

		SignalNames(String chanId, String prettyName, String cat) {
			this.chanId = chanId;
			this.prettyName = prettyName;
			this.cat = cat;
		}

		public String getChan() {
			return chanId;
		}

		public String getName() {
			return prettyName;
		}

		public String getCategory() {
			return cat;
		}

		// ****** Reverse Lookup ************//

		public static Optional<SignalNames> get(String chan) {
			return Arrays.stream(SignalNames.values())
					.filter(env -> env.chanId.equals(chan))
					.findFirst();
		}

		public static Optional<SignalNames> getByName(String name) {
			return Arrays.stream(SignalNames.values())
					.filter(env -> env.prettyName.equals(name))
					.findFirst();
		}
	}

	public String getSignalName(String chan) {
		SignalNames sig = SignalNames.get(chan).get();
		return sig.getName();
	}

	@Override
	public void setupItems() {
		JPanel ScrollPanel = new JPanel();
		ScrollPanel.setLayout(new BoxLayout(ScrollPanel, BoxLayout.Y_AXIS));
		tsOverlays = new Vector<TimeShowOverlay>();
		bds = new Vector<BinaryDataDrawer>();
		bdds = new Vector<BinaryDoubleDataDrawer>();
		fds = new Vector<FormulaDrawer>();
		firstItem = true;
		file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, "VU-DAMS/defaultChans.txt");
		defaultChans = new String[] { "ECG", "DZ", "DZDT", "Z0", "MXR", "SCL" };
		if (file.exists()) {
			defaultChans = Utils.readLinesFromFile(file);
		} else {
			saveDefaultsToDisk(defaultChans);
		}
		CurrentOpenData cod = CurrentOpenData.getInstance();
		ArrayList<Ams7fsChannelInfo> channelInfo = cod.getChannelInfo();
		addedChannels = new ArrayList<Ams7fsChannelInfo>();
		graphSet = new ArrayList<Graph>();
		expPanels = new ArrayList<ExpandingPanel>();
		toggleBoxes = new ArrayList<JCheckBox>();
		togglePanel = new JPanel(new BorderLayout());
		togglePanel.setBorder(BorderFactory.createTitledBorder("All signals"));
		togglePanel.setLayout(new BoxLayout(togglePanel, BoxLayout.Y_AXIS));

		ecgPanel = new JPanel(new BorderLayout());
		ecgPanel.setBorder(BorderFactory.createTitledBorder("Electrocardiogram (ECG)"));
		ecgPanel.setLayout(new BoxLayout(ecgPanel, BoxLayout.Y_AXIS));
		icgPanel = new JPanel(new BorderLayout());
		icgPanel.setBorder(BorderFactory.createTitledBorder("Impedance (ICG)"));
		icgPanel.setLayout(new BoxLayout(icgPanel, BoxLayout.Y_AXIS));
		accPanel = new JPanel(new BorderLayout());
		accPanel.setBorder(BorderFactory.createTitledBorder("Acceleration (IMU)"));
		accPanel.setLayout(new BoxLayout(accPanel, BoxLayout.Y_AXIS));
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7) {
			rotPanel = new JPanel(new BorderLayout());
			rotPanel.setBorder(BorderFactory.createTitledBorder("Rotation (IMU)"));
			rotPanel.setLayout(new BoxLayout(rotPanel, BoxLayout.Y_AXIS));
			magPanel = new JPanel(new BorderLayout());
			magPanel.setBorder(BorderFactory.createTitledBorder("Magnetic field (GMU)"));
			magPanel.setLayout(new BoxLayout(magPanel, BoxLayout.Y_AXIS));
		}
		qcPanel = new JPanel(new BorderLayout());
		qcPanel.setBorder(BorderFactory.createTitledBorder("Quality Control"));
		qcPanel.setLayout(new BoxLayout(qcPanel, BoxLayout.Y_AXIS));
		otherPanel = new JPanel(new BorderLayout());
		otherPanel.setBorder(BorderFactory.createTitledBorder("Misc."));
		otherPanel.setLayout(new BoxLayout(otherPanel, BoxLayout.Y_AXIS));
		addChannelIfExists(SignalNames.ECG);
		addChannelIfExists(SignalNames.V2ecg);
		addChannelIfExists(SignalNames.V3ecg);
		addChannelIfExists(SignalNames.DZDT);
		if (!Utils.getExtension(cod.getDataFile()).equals("7fs")
				&& cod.getFileHeader().getDwHardwareVersion() != 7)
			addChannelIfExists(SignalNames.DZ);
		addChannelIfExists(SignalNames.Z0);
		addChannelIfExists(SignalNames.MXR);
		addChannelIfExists(SignalNames.XMT);
		addChannelIfExists(SignalNames.MYR);
		addChannelIfExists(SignalNames.YMT);
		addChannelIfExists(SignalNames.MZR);
		addChannelIfExists(SignalNames.ZMT);
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7) {
			addChannelIfExists(SignalNames.GyroX);
			addChannelIfExists(SignalNames.GyroY);
			addChannelIfExists(SignalNames.GyroZ);
			addChannelIfExists(SignalNames.magX);
			addChannelIfExists(SignalNames.magY);
			addChannelIfExists(SignalNames.magZ);
		}
		addChannelIfExists(SignalNames.BAT);
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7) {
			addChannelIfExists(SignalNames.Visrc);
			addChannelIfExists(SignalNames.Tickdiff);
			addChannelIfExists(SignalNames.Tickdrift);
			addChannelIfExists(SignalNames.T);
			addChannelIfExists(SignalNames.Temp);
			addChannelIfExists(SignalNames.T_sc);
			addChannelIfExists(SignalNames.P_sc);
		}
		for (Ams7fsChannelInfo s : channelInfo) {
			if (addedChannels.contains(s) == false && !s.getSzID().equals("DZRESP")) {
				if ((Utils.getExtension(cod.getDataFile()).equals("7fs")
						|| cod.getFileHeader().getDwHardwareVersion() == 7)
						&& s.getSzID().equals("DZ"))
					continue;
				try {
					addChannel(s, SignalNames.get(s.getSzID()).get());
				} catch (NoSuchElementException e) {
					System.out.println("Signal name not found: " + s.getSzID());
				}
			}
		}
		if (cod.channelExists("Yaw") && cod.channelExists("Roll") && cod.channelExists("Pitch")) {
			addVisualization();
		}
		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);

		for (int i = 0; i < toggleBoxes.size(); i++) {
			ExpandingPanel exp = expPanels.get(i);
			Container parent = exp.getParent();
			LayoutManager2 layout = (LayoutManager2) parent.getLayout();
			SignalNames sig = SignalNames.getByName(toggleBoxes.get(i).getText()).get();
			if (Arrays.stream(defaultChans).anyMatch(sig.getChan()::equals)) {
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
		ScrollPanel.add(new JLabel("Show/hide:"));
		ScrollPanel.add(ecgPanel);
		ScrollPanel.add(icgPanel);
		ScrollPanel.add(accPanel);
		if (Utils.getExtension(cod.getDataFile()).equals("7fs")
				|| cod.getFileHeader().getDwHardwareVersion() == 7) {
			ScrollPanel.add(rotPanel);
			ScrollPanel.add(magPanel);
		}
		ScrollPanel.add(qcPanel);
		ScrollPanel.add(otherPanel);

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
			}
		});
		ScrollPanel.add(but);
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
			}
		});
		ScrollPanel.add(but);
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
			}
		});
		ScrollPanel.add(but);
		but = new JButton("Show defaults");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				defaultChans = Utils.readLinesFromFile(file);
				for (int i = 0; i < toggleBoxes.size(); i++) {
					ExpandingPanel exp = expPanels.get(i);
					Container parent = exp.getParent();
					LayoutManager2 layout = (LayoutManager2) parent.getLayout();
					SignalNames sig = SignalNames.getByName(toggleBoxes.get(i).getText()).get();
					if (Arrays.stream(defaultChans).anyMatch(sig.getChan()::equals)) {
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
			}
		});
		ScrollPanel.add(but);
		but = new JButton("Set defaults");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<String> newDefaults = new ArrayList<String>();
				for (int i = 0; i < toggleBoxes.size(); i++) {
					SignalNames sig = SignalNames.getByName(toggleBoxes.get(i).getText()).get();
					if (toggleBoxes.get(i).isSelected())
						newDefaults.add(sig.getChan());
				}
				String[] defaultArray = new String[newDefaults.size()];
				newDefaults.toArray(defaultArray);
				saveDefaultsToDisk(defaultArray);
			}
		});
		ScrollPanel.add(but);
		panel.add(mainXAxis.getPanel());
		this.add(new JScrollPane(ScrollPanel), BorderLayout.EAST);
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
	}

	private void saveDefaultsToDisk(String[] defaults) {
		File file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, "VU-DAMS/defaultChans.txt");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for (String d : defaults)
				pw.println(d);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	public void autoscaleSelected(int d) {
		for (int i = 0; i < graphSet.size(); i++) {
			if (toggleBoxes.get(i).isSelected())
				graphSet.get(i).autoScale(d);
		}
	}

	@Override
	public void close() throws Exception {
		for (FormulaDrawer fd : fds)
			fd.close();
		for (BinaryDataDrawer bd : bds)
			bd.close();
		for (BinaryDoubleDataDrawer bdd : bdds)
			bdd.close();
	}
}
