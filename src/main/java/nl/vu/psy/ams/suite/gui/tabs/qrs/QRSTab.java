package nl.vu.psy.ams.suite.gui.tabs.qrs;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.IbiDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.ECGOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.ECGRescanOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SelectBarOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
// import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * The detect r-peaks tab
 */
public class QRSTab extends AmsTab {
	private static final long serialVersionUID = 1L;
	private static QRSTab instance;

	public static QRSTab getInstance() {
		if (instance == null) {
			instance = new QRSTab();
		}
		return instance;
	}

	public static QRSTab getNewInstance() {
		instance = null;
		instance = new QRSTab();
		return instance;
	}

	private XAxis xAxisECG;

	private XAxis xAxisMidIBI;
	private LabelOverlay lov1;
	private QRSToolBar toolbar = new QRSToolBar(this);
	private BinaryDataDrawer drawTest, drawTest1;
	private IbiDrawer middrawer, topdrawer;
	private Graph graph, midgraph;
	private YAxis yAxis, yAxisMid, yAxisTop;
	private ECGOverlay eo;

	private QRSTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis, topOverlay);
		setToolBar(toolbar);
	}

	@Override
	public boolean setActive() {
		super.setActive();
		toolbar.cb.setSelectedIndex(CurrentOpenData.getInstance().getECGChannel());
		TimeBar.getInstance().setECG();
		return true;
	}

	@Override
	public void autoscale() {
		xAxisECG.autoscaleConnectedGraphs();
		xAxisMidIBI.autoscaleConnectedGraphs();
		mainXAxis.autoscaleConnectedGraphs();
	}

	public void autoscale(int d) {
		xAxisECG.autoscaleConnectedGraphs(d);
		xAxisMidIBI.autoscaleConnectedGraphs(d);
		mainXAxis.autoscaleConnectedGraphs(d);
	}

	public void autoscaleFast() {
		xAxisECG.autoscaleConnectedGraphsFast();
		xAxisMidIBI.autoscaleConnectedGraphsFast();
		mainXAxis.autoscaleConnectedGraphsFast();
	}

	public XAxis getXAxisECG() {
		return xAxisECG;
	}

	public XAxis getxAxisMidIBI() {
		return xAxisMidIBI;
	}

	public XAxis getxAxisTopIBI() {
		return mainXAxis;
	}

	@Override
	public boolean setUnactive() {
		super.setUnactive();
		if (CurrentOpenData.getInstance().getECGArtefacts().areBeatsUnderArtefacts() == true) {
			int selection = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"There are beats under artefacts. Would you like to delete all beats under the artefacts?",
					"Beats under artefacts",
					JOptionPane.OK_CANCEL_OPTION);
			if (selection == JOptionPane.OK_OPTION) {
				CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
				repaint();
				return false;
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setupItems() {
		xAxisECG = new XAxis(true, true, 1);

		Graph tGraph1 = new Graph(xAxisECG);
		lov1 = new LabelOverlay(tGraph1);
		lov1.setEditable(false);
		lov1.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
		lov1.setLabels(CurrentOpenData.getInstance().getLabels());
		lov1.setTitle("Labels");
		tGraph1.addOverlay(lov1);
		tGraph1.setEmptyYAxis();
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph1.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph1.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		panel.add(tGraph1.getPanel());

		// ECG graph
		Ams7fsChannelInfo s;
		try {
			s = CurrentOpenData.getInstance().getChannelInfoFromID("ECG");
			graph = new Graph(xAxisECG, Color.BLUE);
			if (s.getRealSlope() == 0) {
				yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(),
						InspectTab.getInstance().getSignalName("ECG"));
			} else {
				yAxis = new YAxis(s.getRealSlope(), s.getRealConstant(), s.getSzID() + " [" + s.getSzUnit() + "]");
			}
			yAxis.setBottomValue(Short.MIN_VALUE);
			yAxis.setTopValue(Short.MAX_VALUE);
			graph.setActiveYAxis(yAxis);
			drawTest = new BinaryDataDrawer(s.getSzID(), yAxis);
			drawTest.connectToGraph(graph);
			graph.getPanel().setMinimumSize(new Dimension(600, 150));
			// graph.addUnderlay(new GridOverlay(graph));
			yAxis.autoScale();

			// ------------------------------ To draw Filtered ECG Signal - Under
			// development ---------------------
			drawTest1 = new BinaryDataDrawer(s.getSzID(), "FILTECG", yAxis,
					new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			drawTest1.connectToGraph(graph);
			// ----------------------------------------------------------------------------------------------------
			eo = new ECGOverlay(graph);
			graph.addOverlay(eo);

			Graph rescanGraph = new Graph(xAxisECG);
			ECGRescanOverlay resOv = new ECGRescanOverlay(rescanGraph);
			rescanGraph.addOverlay(resOv);
			rescanGraph.setEmptyYAxis();
			rescanGraph.getPanel().setPreferredSize(new Dimension(1, 20));
			rescanGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

			panel.add(rescanGraph.getPanel());

			panel.add(graph.getPanel(), 1F);

		} catch (Exception e) {
			e.printStackTrace();
		}

		xAxisECG.setTimes(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getStartTimeInUS() + 10000000);

		panel.add(xAxisECG.getPanel());
		// artefact labels
		ArtefactSet arts = CurrentOpenData.getInstance().getECGArtefacts();
		Graph tGraph = new Graph(xAxisECG);
		LabelOverlay lov = new LabelOverlay(tGraph);
		lov.setLabelConfig(AmsLabelConfiguration.getArtefactConfiguration());
		lov.setLabels(arts);
		lov.setTitle("ECG Artefacts");
		tGraph.addOverlay(lov);
		if (AppSettings.getInstance().getIntProperty(Settings.SHOWMARKERINECGARTEFACTSBAR) == 1) {
			EventOverlay evo = new EventOverlay(tGraph);
			evo.setShowCode(true);
			tGraph.addOverlay(evo);
		}
		tGraph.setEmptyYAxis();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20));
		tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		panel.add(tGraph.getPanel());

		// first zoom level
		xAxisMidIBI = new XAxis(true, true, 1);
		xAxisMidIBI.setTimes(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getStartTimeInUS() + 20000000);
		midgraph = new Graph(xAxisMidIBI, Color.ORANGE);
		yAxisMid = new YAxis(0, 1000.0, 0, 1000.0, "IBI (msec)");
		middrawer = new IbiDrawer(yAxisMid);
		middrawer.connectToGraph(midgraph);
		midgraph.setActiveYAxis(middrawer.getYAxis());
		JPanel tmpPanel = midgraph.getPanel();
		tmpPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
		tmpPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 75));
		panel.add(tmpPanel);
		panel.add(xAxisMidIBI.getPanel());

		SelectBarOverlay midOverlay = new SelectBarOverlay(midgraph, Color.BLUE);
		midOverlay.connectToXAxis(xAxisECG);
		midgraph.addOverlay(midOverlay);
		midgraph.addUnderlay(new GridOverlay(midgraph));

		// second zoom level
		mainXAxis.setTimes(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getStartTimeInUS() + 200000000);
		topgraph = new Graph(mainXAxis);
		yAxisTop = new YAxis(0, 1000.0, 0, 1000.0, "IBI (msec)");
		topdrawer = new IbiDrawer(yAxisTop);
		topdrawer.connectToGraph(topgraph);
		topgraph.setActiveYAxis(topdrawer.getYAxis());
		tmpPanel = topgraph.getPanel();
		tmpPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
		tmpPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 75));
		panel.add(tmpPanel);
		panel.add(mainXAxis.getPanel());

		topOverlay = new SelectBarOverlay(topgraph, Color.ORANGE);
		topOverlay.connectToXAxis(xAxisMidIBI, midOverlay);
		topgraph.addOverlay(topOverlay);
		topgraph.addUnderlay(new GridOverlay(topgraph));

		// start positions
		topOverlay.setPosition(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getStartTimeInUS() + 100000000);
		midOverlay.setPosition(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getStartTimeInUS() + 10000000);

	}

	public void updateLabelConfig() {
		lov1.updateLabelConfig();
	}

	protected void setChannel(int chanNo) {
		String[] chanNames = { "ECG", "V2ecg", "V3ecg" };
		String prettyName = InspectTab.getInstance().getSignalName(chanNames[chanNo]);
		drawTest.disconnectFromGraph(graph);
		drawTest1.disconnectFromGraph(graph);
		drawTest = new BinaryDataDrawer(chanNames[chanNo], yAxis);
		drawTest.connectToGraph(graph);
		drawTest1 = new BinaryDataDrawer(chanNames[chanNo], "FILT" + chanNames[chanNo], yAxis,
				new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
		drawTest1.connectToGraph(graph);
		graph.removeOverlay(eo);
		graph.removeMouseListener(eo);
		graph.removeMouseMotionListener(eo);
		eo = new ECGOverlay(graph);
		graph.addOverlay(eo);
		yAxis.setAxisTitle(prettyName);

		middrawer.disconnectFromGraph(midgraph);
		yAxisMid.removeDrawer(middrawer);
		middrawer = new IbiDrawer(chanNo, yAxisMid);
		middrawer.connectToGraph(midgraph);

		topdrawer.disconnectFromGraph(topgraph);
		yAxisTop.removeDrawer(topdrawer);
		topdrawer = new IbiDrawer(chanNo, yAxisTop);
		topdrawer.connectToGraph(topgraph);
		MainFrame.getInstance().getMainFrame().repaint();
		toolbar.bSet = CurrentOpenData.getInstance().getBeatSet(chanNo);

		// ImpTab.getInstance().getECGDrawer().setChannel(chanNames[chanNo]);
		// ImpTab.getInstance().getECGRawDrawer().setChannel(chanNames[chanNo]);
		// ImpTab.getInstance().getECGDrawer().getYAxis().setAxisTitle(prettyName);
		// ImpTab.getInstance().getEso().setDirty(true);
	}
}
