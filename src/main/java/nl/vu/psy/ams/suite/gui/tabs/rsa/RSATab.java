package nl.vu.psy.ams.suite.gui.tabs.rsa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BeatValueDrawer;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRTachogramDrawer;
import nl.vu.psy.ams.suite.gui.drawing.RSADrawer;
import nl.vu.psy.ams.suite.gui.drawing.RespTachogramDrawer;
//import nl.vu.psy.ams.suite.gui.drawing.RespirationRateDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.DrawerOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.RSAArtefactOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.RSASelectedCycleOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.RSATachoScoreOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.RespCycleOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The RSA scoring tab
 */
public class RSATab extends AmsTab {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static RSATab instance;

	public static RSATab getInstance() {
		if (instance == null) {
			instance = new RSATab();
		}
		return instance;
	}

	public static RSATab getNewInstance() {
		instance = null;
		instance = new RSATab();
		return instance;
	}

	private BinaryDataDrawer dzDrawer;
	private JPanel graphPan;
	private Graph graph;
	Vector<RSASelectedCycleOverlay> selOverlays;
	private RSASelectedCycleOverlay selCycleOv1;
	private RSASelectedCycleOverlay selCycleOv2;
	private RSASelectedCycleOverlay selCycleOv3;
	private RSASelectedCycleOverlay selCycleOv4;
	private RSASelectedCycleOverlay selCycleOv5;
	private RSASelectedCycleOverlay selCycleOv6;
	private boolean drawArtefacts;
	private RSADrawer rsaDraw;
	// private RespirationRateDrawer rrDraw;
	private RespTachogramDrawer rrDraw;
	private LabelOverlay lov;

	private RSATab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new RSAToolbar(this));
	}

	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
	}

	public void flipShowHideArtefacts() {
		if (drawArtefacts == true) {
			drawArtefacts = false;
		} else {
			drawArtefacts = true;
		}
		rsaDraw.setDrawArtefacts(drawArtefacts);
		// rrDraw.setDrawArtefacts(drawArtefacts);
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	public void moveSelectedCycleToCenterScreen() {
		double midTime = mainXAxis.getMidPoint();
		for (RSASelectedCycleOverlay ts : selOverlays)
			ts.setSelectedToTime(midTime, true);
	}

	public void nextCycle() {
		for (RSASelectedCycleOverlay ts : selOverlays)
			ts.stepNext();
	}

	public void prevCycle() {
		for (RSASelectedCycleOverlay ts : selOverlays)
			ts.stepPrev();
	}

	public void recalculateRSA() {
		CurrentOpenData.getInstance().getRespSet().recalculate();
		repaint();
	}

	@Override
	public boolean setActive() {
		super.setActive();
		TimeBar.getInstance().setResp();
		return true;
	}

	@Override
	public void setupItems() {
		graphPan = new JPanel();
		graphPan.setLayout(new BoxLayout(graphPan, BoxLayout.Y_AXIS));
		Vector<TimeShowOverlay> tsOverlays = new Vector<TimeShowOverlay>();
		selOverlays = new Vector<RSASelectedCycleOverlay>();

		Graph tGraph1 = new Graph(mainXAxis);
		lov = new LabelOverlay(tGraph1);
		lov.setEditable(false);
		lov.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
		lov.setLabels(CurrentOpenData.getInstance().getLabels());
		lov.setTitle("Labels");
		tGraph1.addOverlay(lov);
		tGraph1.setEmptyYAxis();
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph1.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph1.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		graphPan.add(tGraph1.getPanel());

		Ams7fsChannelInfo s;
		try {
			CurrentOpenData cod = CurrentOpenData.getInstance();
			s = cod.getChannelInfoFromID("DZ");
			graph = new Graph(mainXAxis);
			YAxis yAxis;
			if (s.getRealSlope() == 0) {
				yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
			} else {
				yAxis = new YAxis(s.getRealSlope(), s.getRealConstant(), s.getSzID() + " [" + s.getSzUnit() + "]");
			}
			yAxis.setBottomValue(Short.MIN_VALUE);
			yAxis.setTopValue(Short.MAX_VALUE);
			graph.setActiveYAxis(yAxis);
			BinaryDataDrawer drawTest;
			if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7) {
				dzDrawer = new BinaryDataDrawer("Z0", yAxis, Color.BLACK);
				drawTest = new BinaryDataDrawer("Z0", "FILTDZ", yAxis,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			} else {
				dzDrawer = new BinaryDataDrawer("DZ", yAxis, Color.BLACK);
				drawTest = new BinaryDataDrawer(s.getSzID(), "FILTDZ", yAxis,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			}
			dzDrawer.connectToGraph(graph);
			graph.addUnderlay(new GridOverlay(graph));
			selCycleOv1 = new RSASelectedCycleOverlay(graph);
			graph.addOverlay(selCycleOv1);
			selOverlays.add(selCycleOv1);
			DrawerOverlay drawOv = new DrawerOverlay(graph, drawTest, yAxis);
			drawTest.connectToGraph(graph);
			graph.addOverlay(drawOv);
			dzDrawer.connectOverlay(drawOv);
			RespCycleOverlay rso = new RespCycleOverlay(graph);
			graph.addOverlay(rso);
			drawTest.connectOverlay(rso);
			TimeShowOverlay tso = new TimeShowOverlay(graph);
			graph.addOverlay(tso);
			tsOverlays.add(tso);
			yAxis.autoScale();
			graphPan.add(graph.getPanel());

			Graph tGraph;
			RSAArtefactOverlay lov;
			if (!Utils.getExtension(cod.getDataFile()).equals("7fs") && cod.fileHeader.getDwHardwareVersion() != 7) {
				tGraph = new Graph(mainXAxis);
				lov = new RSAArtefactOverlay(tGraph, RSAArtefactOverlay.TYPE_CLIPPING_DZ);
				tGraph.addOverlay(lov);
				tGraph.setEmptyYAxis();
				tGraph.getPanel().setPreferredSize(new Dimension(1, 20));
				tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
				graphPan.add(tGraph.getPanel());
			}
			tGraph = new Graph(mainXAxis);
			lov = new RSAArtefactOverlay(tGraph, RSAArtefactOverlay.TYPE_IRREGULAR_RR);
			tGraph.addOverlay(lov);
			tGraph.setEmptyYAxis();
			tGraph.getPanel().setPreferredSize(new Dimension(1, 20));
			tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			graphPan.add(tGraph.getPanel());

			Graph amplitudeGraph = new Graph(mainXAxis);
			BeatValueDrawer ampDrawer = new BeatValueDrawer(false, false, Color.BLACK);
			amplitudeGraph.setActiveYAxis(ampDrawer.getYAxis());
			ampDrawer.connectToGraph(amplitudeGraph);
			amplitudeGraph.addUnderlay(new GridOverlay(amplitudeGraph));
			selCycleOv5 = new RSASelectedCycleOverlay(amplitudeGraph);
			amplitudeGraph.addOverlay(selCycleOv5);
			selOverlays.add(selCycleOv5);
			if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7) {
				BeatValueDrawer filteredDraw = new BeatValueDrawer(false, true,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				DrawerOverlay drawOvA = new DrawerOverlay(amplitudeGraph, filteredDraw, ampDrawer.getYAxis());
				filteredDraw.connectToGraph(amplitudeGraph);
				amplitudeGraph.addOverlay(drawOvA);
			}
			tso = new TimeShowOverlay(amplitudeGraph);
			amplitudeGraph.addOverlay(tso);
			tsOverlays.add(tso);
			graphPan.add(amplitudeGraph.getPanel());

			Graph baseGraph = new Graph(mainXAxis);
			BeatValueDrawer baseDrawer = new BeatValueDrawer(true, false, Color.BLACK);
			baseGraph.setActiveYAxis(baseDrawer.getYAxis());
			baseDrawer.connectToGraph(baseGraph);
			baseGraph.addUnderlay(new GridOverlay(baseGraph));
			selCycleOv6 = new RSASelectedCycleOverlay(baseGraph);
			baseGraph.addOverlay(selCycleOv6);
			selOverlays.add(selCycleOv6);
			if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7) {
				BeatValueDrawer filteredDraw = new BeatValueDrawer(true, true,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				DrawerOverlay drawOvA = new DrawerOverlay(baseGraph, filteredDraw, baseDrawer.getYAxis());
				filteredDraw.connectToGraph(baseGraph);
				baseGraph.addOverlay(drawOvA);
			}
			tso = new TimeShowOverlay(baseGraph);
			baseGraph.addOverlay(tso);
			tsOverlays.add(tso);
			graphPan.add(baseGraph.getPanel());

			Graph ibiGraph = new Graph(mainXAxis);
			YAxis hrYAxis = new YAxis(0, 1000, 0, 1000, "IBI (msec)");
			hrYAxis.setBottomValue(0);
			hrYAxis.setTopValue(1);
			HRTachogramDrawer tachDraw = new HRTachogramDrawer("IBI", hrYAxis);
			ibiGraph.setActiveYAxis(hrYAxis);
			tachDraw.connectToGraph(ibiGraph);
			ibiGraph.addUnderlay(new GridOverlay(ibiGraph));
			ibiGraph.addOverlay(new RSATachoScoreOverlay(ibiGraph));
			selCycleOv2 = new RSASelectedCycleOverlay(ibiGraph);
			ibiGraph.addOverlay(selCycleOv2);
			selOverlays.add(selCycleOv2);
			tso = new TimeShowOverlay(ibiGraph);
			ibiGraph.addOverlay(tso);
			tsOverlays.add(tso);
			graphPan.add(ibiGraph.getPanel());

			// tGraph = new Graph(mainXAxis);
			// lov = new RSAArtefactOverlay(tGraph, RSAArtefactOverlay.TYPE_IRREGULAR_IBI);
			// tGraph.addOverlay(lov);
			// tGraph.setEmptyYAxis();
			// tGraph.getPanel().setPreferredSize(new Dimension(1, 20));
			// tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			// graphPan.add(tGraph.getPanel());

			ArtefactSet arts = CurrentOpenData.getInstance().getECGArtefacts();
			tGraph = new Graph(mainXAxis);
			LabelOverlay lova = new LabelOverlay(tGraph);
			lova.setLabelConfig(AmsLabelConfiguration.getArtefactConfiguration());
			lova.setLabels(arts);
			lova.setTitle("ECG Artefacts");
			lova.setEditable(false);
			tGraph.addOverlay(lova);
			if (AppSettings.getInstance().getIntProperty(Settings.SHOWMARKERINECGARTEFACTSBAR) == 1) {
				EventOverlay evo = new EventOverlay(tGraph);
				evo.setShowCode(true);
				tGraph.addOverlay(evo);
			}
			tGraph.setEmptyYAxis();
			tGraph.getPanel().setPreferredSize(new Dimension(1, 20));
			tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			graphPan.add(tGraph.getPanel());

			Graph rsaGraph = new Graph(mainXAxis);
			YAxis rsaYAxis = new YAxis(0, 500, 0, 500, "RSA (msec)");
			rsaYAxis.setBottomValue(0);
			rsaYAxis.setTopValue(1);
			rsaDraw = new RSADrawer("RSA", rsaYAxis);
			rsaGraph.setActiveYAxis(rsaYAxis);
			rsaDraw.connectToGraph(rsaGraph);
			rsaGraph.addUnderlay(new GridOverlay(rsaGraph));
			selCycleOv3 = new RSASelectedCycleOverlay(rsaGraph);
			rsaGraph.addOverlay(selCycleOv3);
			selOverlays.add(selCycleOv3);
			tso = new TimeShowOverlay(rsaGraph);
			rsaGraph.addOverlay(tso);
			tsOverlays.add(tso);
			graphPan.add(rsaGraph.getPanel());

			Graph rrGraph = new Graph(mainXAxis);
			YAxis rrYAxis = new YAxis(0, 20, 0, 20, "Respiration (sec)");
			rrYAxis.setBottomValue(0);
			rrYAxis.setTopValue(1);
			// rrDraw = new RespirationRateDrawer("RR", rrYAxis);
			rrDraw = new RespTachogramDrawer("RR", rrYAxis);
			rrGraph.setActiveYAxis(rrYAxis);
			rrDraw.connectToGraph(rrGraph);
			rrGraph.addUnderlay(new GridOverlay(rrGraph));
			selCycleOv4 = new RSASelectedCycleOverlay(rrGraph);
			rrGraph.addOverlay(selCycleOv4);
			selOverlays.add(selCycleOv4);
			tso = new TimeShowOverlay(rrGraph);
			rrGraph.addOverlay(tso);
			tsOverlays.add(tso);
			graphPan.add(rrGraph.getPanel());

		} catch (Exception e) {
			e.printStackTrace();
		}
		graphPan.add(mainXAxis.getPanel());

		for (RSASelectedCycleOverlay ts : selOverlays)
			for (RSASelectedCycleOverlay ts2 : selOverlays)
				ts.connectToEachOther(ts2);
		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);

		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.add(new RespirationCycleInfoPanel(selCycleOv1), BorderLayout.NORTH);
		outerPanel.add(graphPan, BorderLayout.CENTER);

		panel.add(outerPanel, 1F);
	}

	public void updateLabelConfig() {
		lov.updateLabelConfig();
	}
}
