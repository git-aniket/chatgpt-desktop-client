package nl.vu.psy.ams.suite.gui.tabs.label;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.TotalMotilityGenerator;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDoubleDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRDrawer;
import nl.vu.psy.ams.suite.gui.drawing.LFHFDrawer;
import nl.vu.psy.ams.suite.gui.drawing.RespirationRateDrawer;
import nl.vu.psy.ams.suite.gui.drawing.TotalMotilityDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelLinesOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * The label tab
 */
public class LabelTab extends AmsTab implements AutoCloseable {

	private static final long serialVersionUID = 1L;
	private static LabelTab instance;

	public static LabelTab getInstance() {
		if (instance == null) {
			instance = new LabelTab();
		}
		return instance;
	}

	public static LabelTab getNewInstance() {
		instance = null;
		instance = new LabelTab();
		return instance;
	}

	private HRDrawer hrDrawer;
	private BinaryDataDrawer motDrawer;
	private LFHFDrawer lfhfDrawer;
	private Graph motGraph, EE1g, tGraph;
	private int selPanel = 0;
	private TotalMotilityDrawer EE1d;
	private ArrayList<ExpandingPanel> panels;
	private LabelOverlay lov, lovPosture;
	private RespirationRateDrawer rrDraw;
	private AmsLabelSet labelsShow;
	private LabelSet labelsPosture;
	private BinaryDataDrawer dzDrawer;
	// private LabelSet labelsPosture;

	private LabelTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new LabelToolBar(this));
	}

	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
	}

	public HRDrawer getHRDrawer() {
		return hrDrawer;
	}

	public BinaryDataDrawer getMotDrawer() {
		return motDrawer;
	}

	public LFHFDrawer getLfhfDrawer() {
		return lfhfDrawer;
	}

	public RespirationRateDrawer getRRDrawer() {
		return rrDraw;
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	public void nextPanel() {
		panel.remove(mainXAxis.getPanel());
		panel.remove(panels.get(selPanel));
		selPanel++;
		if (selPanel >= panels.size())
			selPanel = 0;
		panel.add(panels.get(selPanel), 1F);
		panel.add(mainXAxis.getPanel());
		MainFrame.getInstance().getMainFrame().repaint();
	}

	public void prevPanel() {
		panel.remove(mainXAxis.getPanel());
		panel.remove(panels.get(selPanel));
		selPanel--;
		if (selPanel < 0)
			selPanel = panels.size() - 1;
		panel.add(panels.get(selPanel), 1F);
		panel.add(mainXAxis.getPanel());
		MainFrame.getInstance().getMainFrame().repaint();
	}

	@Override
	public boolean setActive() {
		super.setActive();
		if (CurrentOpenData.getInstance().channelExists("ECG")) {
			if (CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No Beats Detected");
			} else {
				hrDrawer.recalcAverage();
				lfhfDrawer.recalculate();
				rrDraw.recalcAverage();
			}
		}
		TotalMotilityGenerator.clear();
		if (EE1d != null)
			EE1d.resetValues();
		return true;
	}

	@Override
	public boolean setUnactive() {
		labelsShow = (AmsLabelSet) CurrentOpenData.getInstance().getLabels();
		if (labelsShow.isOverlapping() == true) {
			int selection = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"There overlapping labels, are you sure you want to leave the Label Data tab?",
					"Overlapping labels", JOptionPane.YES_NO_OPTION);
			if (selection == JOptionPane.NO_OPTION || selection == JOptionPane.CLOSED_OPTION) {
				return false;
			}
		}
		super.setUnactive();
		return true;
	}

	@Override
	public void setupItems() {
		Vector<TimeShowOverlay> tsOverlays = new Vector<TimeShowOverlay>();
		labelsShow = (AmsLabelSet) CurrentOpenData.getInstance().getLabels();
		panels = new ArrayList<ExpandingPanel>();
		tGraph = new Graph(mainXAxis);
		lov = new LabelOverlay(tGraph);
		lov.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
		lov.setLabels(labelsShow);
		lov.setTitle("label / click to add marker");
		tGraph.addOverlay(lov);
		tGraph.setEmptyYAxis();
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		panel.add(tGraph.getPanel());

		labelsPosture = CurrentOpenData.getInstance().getPostureLabels();
		Graph pGraph = new Graph(mainXAxis);
		lovPosture = new LabelOverlay(pGraph);
		lovPosture.setLabelConfig(AmsLabelConfiguration.getPostureLabelConfiguration());
		lovPosture.setEditable(false);
		lovPosture.setLabels(labelsPosture);
		pGraph.addOverlay(lovPosture);
		pGraph.setEmptyYAxis();
		pGraph.getPanel().setPreferredSize(new Dimension(1, 20));
		pGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		panel.add(pGraph.getPanel());

		boolean drawECG = false;
		TimeShowOverlay tso;
		if (CurrentOpenData.getInstance().channelExists("ECG")) {

			if (CurrentOpenData.getInstance().isOpen()
					&& CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()) {
				if (CurrentOpenData.getInstance().isBatchAnalysis() == false) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No Beats Detected");
				}
			} // else{
			drawECG = true;
			Graph hrGraph = new Graph(mainXAxis);
			hrGraph.addUnderlay(new GridOverlay(hrGraph));
			hrDrawer = new HRDrawer();
			hrDrawer.connectToGraph(hrGraph);
			if (AppSettings.getInstance().getIntProperty(Settings.DRAWRAWHRLABELTAB) == 0)
				hrDrawer.drawRawHR(false);
			if (AppSettings.getInstance().getIntProperty(Settings.DRAWAVHRLABELTAB) == 0)
				hrDrawer.drawAverageHR(false);
			hrDrawer.setAvColor(
					new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
			hrGraph.setActiveYAxis(hrDrawer.getYAxis());
			LabelLinesOverlay llov = new LabelLinesOverlay(hrGraph, labelsShow, lov);
			EventOverlay evo = new EventOverlay(hrGraph);
			evo.setEditable(true);
			evo.setShowCode(true);
			hrGraph.addOverlay(llov);
			hrGraph.addOverlay(evo);
			tso = new TimeShowOverlay(hrGraph);
			hrGraph.addOverlay(tso);
			tsOverlays.add(tso);
			panel.add(new ExpandingPanel(hrGraph.getPanel(), false, "HR"), 1F);

			Graph lfhfGraph = new Graph(mainXAxis);
			lfhfGraph.addUnderlay(new GridOverlay(lfhfGraph));
			lfhfDrawer = new LFHFDrawer("LF+HF", new YAxis(0, 1000, 0, 1000, "Power [ms\u00B2]"));
			lfhfDrawer.connectToGraph(lfhfGraph);
			lfhfGraph.setActiveYAxis(lfhfDrawer.getYAxis());
			llov = new LabelLinesOverlay(lfhfGraph, labelsShow, lov);
			llov.enableSmallLines(false);
			EventOverlay evoLH = new EventOverlay(lfhfGraph);
			evoLH.setEditable(true);
			lfhfGraph.addOverlay(evoLH);
			lfhfGraph.addOverlay(llov);
			tso = new TimeShowOverlay(lfhfGraph);
			lfhfGraph.addOverlay(tso);
			tsOverlays.add(tso);
			lfhfDrawer.getYAxis().setBottomValue(0);
			lfhfDrawer.getYAxis().setTopValue(1000);
			panel.add(new ExpandingPanel(lfhfGraph.getPanel(), false, "Power"), 1F);

			Graph rrGraph = new Graph(mainXAxis);
			YAxis rrYAxis = new YAxis(0, 50, 0, 50, "Respiration (bpm)");
			rrYAxis.setBottomValue(0);
			rrYAxis.setTopValue(1);
			rrDraw = new RespirationRateDrawer("RR", rrYAxis);
			rrGraph.setActiveYAxis(rrYAxis);
			rrDraw.connectToGraph(rrGraph);
			if (AppSettings.getInstance().getIntProperty(Settings.DRAWRAWRRLABELTAB) == 0)
				rrDraw.drawRaw(false);
			if (AppSettings.getInstance().getIntProperty(Settings.DRAWAVRRLABELTAB) == 0)
				rrDraw.drawAv(false);
			rrGraph.addUnderlay(new GridOverlay(rrGraph));
			llov = new LabelLinesOverlay(rrGraph, labelsShow, lov);
			llov.enableSmallLines(false);
			EventOverlay evorr = new EventOverlay(rrGraph);
			evorr.setEditable(true);
			rrGraph.addOverlay(evorr);
			rrGraph.addOverlay(llov);
			tso = new TimeShowOverlay(rrGraph);
			rrGraph.addOverlay(tso);
			tsOverlays.add(tso);
			panel.add(new ExpandingPanel(rrGraph.getPanel(), false, "Respiration"), 1F);

			Ams7fsChannelInfo s;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("DZ");
				Graph graph = new Graph(mainXAxis);
				YAxis yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
				yAxis.setBottomValue(Short.MIN_VALUE);
				yAxis.setTopValue(Short.MAX_VALUE);
				graph.setActiveYAxis(yAxis);
				dzDrawer = new BinaryDataDrawer(s.getSzID(), "FILTDZ", yAxis,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				dzDrawer.connectToGraph(graph);
				graph.addUnderlay(new GridOverlay(graph));
				llov = new LabelLinesOverlay(graph, labelsShow, lov);
				llov.enableSmallLines(false);
				EventOverlay evoDZ = new EventOverlay(graph);
				evoDZ.setEditable(true);
				graph.addOverlay(evoDZ);
				graph.addOverlay(llov);
				tso = new TimeShowOverlay(graph);
				graph.addOverlay(tso);
				tsOverlays.add(tso);
				yAxis.autoScale();
				panel.add(new ExpandingPanel(graph.getPanel(), false, s.getSzID()), 1F);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// }
		}
		if (CurrentOpenData.getInstance().channelExists("SCL") && drawECG == false) {
			Ams7fsChannelInfo s;// JdH assume SCL only file and draw SCL in Label tab
			ExpandingPanel exp;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("SCL");
				motGraph = new Graph(mainXAxis);
				YAxis yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
				yAxis.setBottomValue(Short.MIN_VALUE);
				yAxis.setTopValue(Short.MAX_VALUE);
				motGraph.setActiveYAxis(yAxis);
				motDrawer = new BinaryDataDrawer(s.getSzID(), yAxis);
				motDrawer.setDrawAv(true);
				motDrawer.connectToGraph(motGraph);
				motGraph.addUnderlay(new GridOverlay(motGraph));
				LabelLinesOverlay llov2 = new LabelLinesOverlay(motGraph, labelsShow, lov);
				llov2.enableSmallLines(false);
				motGraph.addOverlay(llov2);
				EventOverlay evo = new EventOverlay(motGraph);
				evo.setEditable(true);
				evo.setShowCode(true);
				motGraph.addOverlay(evo);
				tso = new TimeShowOverlay(motGraph);
				motGraph.addOverlay(tso);
				tsOverlays.add(tso);
				yAxis.autoScale();
				exp = new ExpandingPanel(motGraph.getPanel(), false, s.getSzID());
				panel.add(exp, 1F);
				panels.add(exp);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (CurrentOpenData.getInstance().channelExists("MYA")) {
			Ams7fsChannelInfo s;
			ExpandingPanel exp;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("MYA");
				motGraph = new Graph(mainXAxis);
				YAxis yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
				yAxis.setBottomValue(Short.MIN_VALUE);
				yAxis.setTopValue(Short.MAX_VALUE);
				motGraph.setActiveYAxis(yAxis);
				motDrawer = new BinaryDataDrawer(s.getSzID(), yAxis);
				if (AppSettings.getInstance().getIntProperty(Settings.DRAWAVMOTLABELTAB) != 0) {
					motDrawer.setDrawAv(true);
					motDrawer.setClipAtZero(true);
				}
				if (AppSettings.getInstance().getIntProperty(Settings.DRAWRAWMOTLABELTAB) == 0)
					motDrawer.setDrawRaw(false);
				motDrawer.connectToGraph(motGraph);
				motDrawer.setAvColor(
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVMOTCOLOR)));
				motGraph.addUnderlay(new GridOverlay(motGraph));
				LabelLinesOverlay llov2 = new LabelLinesOverlay(motGraph, labelsShow, lov);
				llov2.enableSmallLines(false);
				EventOverlay evoM = new EventOverlay(motGraph);
				evoM.setEditable(true);
				motGraph.addUnderlay(evoM);
				motGraph.addOverlay(llov2);
				tso = new TimeShowOverlay(motGraph);
				motGraph.addOverlay(tso);
				tsOverlays.add(tso);
				yAxis.autoScale();
				exp = new ExpandingPanel(motGraph.getPanel(), false, s.getSzID());
				panel.add(exp, 1F);
				panels.add(exp);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		LabelLinesOverlay llov2;
		BinaryDoubleDataDrawer bddd;

		File xFile = new File(CurrentOpenData.getInstance().getFilePath(), "AVX.dbin");
		File yFile = new File(CurrentOpenData.getInstance().getFilePath(), "AVY.dbin");
		File zFile = new File(CurrentOpenData.getInstance().getFilePath(), "AVZ.dbin");
		Graph gr;
		YAxis ya;

		if (xFile.exists()) {
			gr = new Graph(mainXAxis);
			ya = new YAxis(0, 1, 0, 1, "XMOT [g]");
			ya.setBottomValue(0);
			ya.setTopValue(1);
			gr.setActiveYAxis(ya);
			gr.addUnderlay(new GridOverlay(gr));
			EventOverlay evox = new EventOverlay(gr);
			evox.setEditable(true);
			gr.addUnderlay(evox);
			llov2 = new LabelLinesOverlay(gr, labelsShow, lov);
			llov2.enableSmallLines(false);
			gr.addOverlay(llov2);
			bddd = new BinaryDoubleDataDrawer("XMOT", ya, xFile, 1000000.);
			bddd.connectToGraph(gr);
			tso = new TimeShowOverlay(gr);
			gr.addOverlay(tso);
			tsOverlays.add(tso);
			panels.add(new ExpandingPanel(gr.getPanel(), false, "XMOT"));
		}

		if (yFile.exists()) {
			gr = new Graph(mainXAxis);
			ya = new YAxis(0, 1, 0, 1, "YMOT [g]");
			ya.setBottomValue(0);
			ya.setTopValue(1);
			gr.setActiveYAxis(ya);
			gr.addUnderlay(new GridOverlay(gr));
			EventOverlay evoy = new EventOverlay(gr);
			evoy.setEditable(true);
			gr.addUnderlay(evoy);
			llov2 = new LabelLinesOverlay(gr, labelsShow, lov);
			llov2.enableSmallLines(false);
			gr.addOverlay(llov2);
			bddd = new BinaryDoubleDataDrawer("YMOT", ya, yFile, 1000000.);
			bddd.connectToGraph(gr);
			tso = new TimeShowOverlay(gr);
			gr.addOverlay(tso);
			tsOverlays.add(tso);
			panels.add(new ExpandingPanel(gr.getPanel(), false, "YMOT"));
		}
		if (zFile.exists()) {
			gr = new Graph(mainXAxis);
			ya = new YAxis(0, 1, 0, 1, "ZMOT [g]");
			ya.setBottomValue(0);
			ya.setTopValue(1);
			gr.setActiveYAxis(ya);
			gr.addUnderlay(new GridOverlay(gr));
			EventOverlay evoz = new EventOverlay(gr);
			evoz.setEditable(true);
			gr.addUnderlay(evoz);
			llov2 = new LabelLinesOverlay(gr, labelsShow, lov);
			llov2.enableSmallLines(false);
			gr.addOverlay(llov2);
			bddd = new BinaryDoubleDataDrawer("ZMOT", ya, zFile, 1000000.);
			bddd.connectToGraph(gr);
			tso = new TimeShowOverlay(gr);
			gr.addOverlay(tso);
			tsOverlays.add(tso);
			panels.add(new ExpandingPanel(gr.getPanel(), false, "ZMOT"));
		}

		if (xFile.exists() && yFile.exists() && zFile.exists()) {
			EE1g = new Graph(mainXAxis);
			YAxis yAx = new YAxis(0, 1, 0, 1, "Total Motility [g]");
			yAx.setBottomValue(0);
			yAx.setTopValue(1);
			EE1g.setActiveYAxis(yAx);
			EE1g.addUnderlay(new GridOverlay(EE1g));
			EventOverlay evoTM = new EventOverlay(EE1g);
			evoTM.setEditable(true);
			EE1g.addUnderlay(evoTM);
			llov2 = new LabelLinesOverlay(EE1g, labelsShow, lov);
			llov2.enableSmallLines(false);
			EE1g.addOverlay(llov2);
			EE1d = new TotalMotilityDrawer("EE1", yAx, true);
			EE1d.connectToGraph(EE1g);
			tso = new TimeShowOverlay(EE1g);
			EE1g.addOverlay(tso);
			tsOverlays.add(tso);
			ExpandingPanel exp = new ExpandingPanel(EE1g.getPanel(), false, "Total Motility");
			if (!CurrentOpenData.getInstance().channelExists("MYA")) {
				panel.add(exp, 1F);
			}
			panels.add(exp);
		}

		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);

		panel.add(mainXAxis.getPanel());
	}

	public void updateLabelConfig() {
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph.getPanel().setSize(new Dimension(1, 20 * cats.size()));
		lov.updateLabelConfig();
	}

	public AmsLabelSet getLabelsShow() {
		return labelsShow;
	}

	@Override
	public void close() throws Exception {
		dzDrawer.close();
	}

}
