package nl.vu.psy.ams.suite.gui.tabs.scl;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.data.structures.sets.SCLArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.SkinConductanceSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDoubleDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.FormulaDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRDrawer;
import nl.vu.psy.ams.suite.gui.drawing.TotalMotilityDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.EventOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SCLOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SCLSelectedOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SCLabelSelectedOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
//import nl.vu.psy.ams.suite.tools.Utils;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The Skin Conductance tab
 */
public class SCLTab extends AmsTab implements AutoCloseable {

	private static final long serialVersionUID = 1L;
	private static SCLTab instance;
	protected TreeSet<AmsLabel> labels = new TreeSet<AmsLabel>();

	public static SCLTab getInstance() {
		if (instance == null) {
			instance = new SCLTab();
		}
		return instance;
	}

	public static SCLTab getNewInstance() {
		instance = null;
		instance = new SCLTab();
		return instance;
	}

	private HRDrawer hrDrawer;
	private DataDrawer sclDrawer;
	private BinaryDataDrawer motDrawer;
	private Graph motGraph;
	private Graph EE1g;
	private int selPanel = 0;
	private TotalMotilityDrawer EE1d;
	private ArrayList<ExpandingPanel> panels;
	private LabelOverlay lov, lov1;
	private SCLInfoPanel iip;
	private EventSCLInfoPanel eventiip;
	private JPanel pan;
	private SCLOverlay sclmarkOverlay;
	private int eventcount = 0;
	private BinaryDataDrawer drawTest;
	private BinaryDoubleDataDrawer ddrawTest;

	private SCLTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new SCLToolBar(this));
	}

	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
	}

	public HRDrawer getHRDrawer() {
		return hrDrawer;
	}

	public DataDrawer getSCLDrawer() {
		return sclDrawer;
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	public SCLInfoPanel getInfoPanel() {
		return iip;
	}

	public EventSCLInfoPanel getEventInfoPanel() {
		return eventiip;
	}

	public SCLOverlay getSelOverlay() {
		return sclmarkOverlay;
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
		CurrentOpenData cod = CurrentOpenData.getInstance();
		// -------------- Type of Design---------------------------------------------
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.EVENTBASED) == 1) { // Event based design - No
																								// need for labels,
																								// Stimulus must be
																								// present
			for (Ams5fsPacket ev : cod.getEvents()) {
				if ((ev.getlType() == 0) || (ev.getlType() == 1) || (ev.getlType() == 2) || ev.getlType() == 100) {
					eventcount++;
				}
			}
			if (eventcount == 0) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No Events/Stimulus present");
				return false;
			} else {
				/*
				 * if(Utils.compareVersions(cod.getFileHeader().
				 * getAmsDataFileVersion(), "3.5") > 0){ // To Recalculate SCL Cycles
				 * cod.getEventSCLSet().recalculate();
				 * }
				 */
				if (cod.getEventSCLSet().getSCLCycles().isEmpty()) { // Check before
																		// super.setActive();
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"No SCR Cycles Detected (yet)");
					return false;
				}
				super.setActive();
				// If ECG channel exists and beats detected, draw HR graph
				if (cod.channelExists("ECG")
						&& cod.getBeatSet().getBeats().isEmpty() == false) {
					hrDrawer.recalcAverage();
				}
				redraw();
				return true;
			}
		} else { // Label based design
			/*
			 * if(cod.getLabels().getLabels().isEmpty()){
			 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
			 * "No labels present");
			 * return false;
			 * }else{
			 * if(Utils.compareVersions(cod.getFileHeader().
			 * getAmsDataFileVersion(), "3.5") > 0){
			 * cod.getSCLSet().reCalculate();
			 * }
			 */
			if (cod.getSCLSet().getSCLCycles().isEmpty()) { // Check before super.setActive();
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No SCL Peaks Detected (yet)");
				// return false;
			}
			super.setActive();
			// If ECG channel exists and beats detected, draw HR graph
			if (cod.channelExists("ECG")
					&& cod.getBeatSet().getBeats().isEmpty() == false) {
				hrDrawer.recalcAverage();
			}
			redraw();
			return true;
			// }
		}
	}

	public boolean setUnactive() {
		super.setUnactive();
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (cod.getSCLArtefacts().areCyclesUnderArtefacts() == true) {
			int selection = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"There are scl cycles under artefacts. Would you like to delete all cycles under the artefacts?",
					"SCL Cycles under artefacts",
					JOptionPane.OK_CANCEL_OPTION);
			if (selection == JOptionPane.OK_OPTION) {
				cod.getSCLArtefacts().deleteSCLCyclesUnderArtefacts();
				if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1)
					getEventInfoPanel().updateLabelTexts(
							cod.getEventSCLSet().getSCLCycles().first(), true);
				else
					getInfoPanel().updateLabelTexts(cod.getLabels().getLabels().first());
				return false;
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setupItems() {
		Vector<TimeShowOverlay> tsOverlays = new Vector<TimeShowOverlay>();
		CurrentOpenData cod = CurrentOpenData.getInstance();

		panels = new ArrayList<ExpandingPanel>();
		pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		panel.add(pan);

		Graph tGraph = new Graph(mainXAxis);
		lov = new LabelOverlay(tGraph);
		lov.setEditable(false);
		lov.setLabelConfig(cod.getLabelConfig());
		lov.setLabels(cod.getLabels());
		lov.setTitle("Labels");
		tGraph.addOverlay(lov);
		tGraph.setEmptyYAxis();
		ArrayList<String> cats = cod.getLabelConfig().getCategories();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		panel.add(tGraph.getPanel());

		Ams7fsChannelInfo s;
		try {
			s = cod.getChannelInfoFromID("SCL");

			Graph sclGraph = new Graph(mainXAxis);
			YAxis yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
					(double) s.getlMinValue()
							/ s.getlMinMaxDivider(),
					(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
			yAxis.setBottomValue(Integer.MIN_VALUE);
			yAxis.setTopValue(Integer.MAX_VALUE);
			sclGraph.setActiveYAxis(yAxis);
			if (Utils.getExtension(cod.getDataFile()).equals("7fs")
					|| cod.getFileHeader().getDwHardwareVersion() == 7) {
				sclDrawer = new FormulaDrawer("SCL", yAxis);
				sclDrawer.connectToGraph(sclGraph);

				File yFile = new File(cod.getFilePath(), "FILT" + s.getSzID() + ".dbin");
				ddrawTest = new BinaryDoubleDataDrawer(s.getSzID(), yAxis, yFile, 1000.,
						s.getTickFile(),
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				ddrawTest.connectToGraph(sclGraph);
			} else {
				sclDrawer = new BinaryDataDrawer(s.getSzID(), "SCL", yAxis, Color.BLACK);
				sclDrawer.connectToGraph(sclGraph);

				drawTest = new BinaryDataDrawer(s.getSzID(), "FILTSCL", yAxis,
						new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
				drawTest.getYAxis().setAxisTitle("FILT-SCL [\u00B5S] ");
				drawTest.connectToGraph(sclGraph);
			}
			// drawTest.setSCL(true);

			if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1) {
				eventiip = new EventSCLInfoPanel();
				pan.add(eventiip);
				// drawTest.setSCRLabel(false);
				if (!cod.getEvents().isEmpty()) {
					sclGraph.addOverlay(new SCLSelectedOverlay(sclGraph));
					sclmarkOverlay = new SCLOverlay(sclGraph, this);
					sclGraph.addUnderlay(sclmarkOverlay); // Selected Cycle Overlay
				}
			} else {
				iip = new SCLInfoPanel();
				pan.add(iip);
				// drawTest.setSCRLabel(true);
				sclGraph.addOverlay(new SCLabelSelectedOverlay(sclGraph));
			}
			sclGraph.addUnderlay(new GridOverlay(sclGraph));

			EventOverlay evol = new EventOverlay(sclGraph);
			evol.setShowCode(true);
			sclGraph.addUnderlay(evol);
			TimeShowOverlay tso = new TimeShowOverlay(sclGraph);
			sclGraph.addOverlay(tso);
			tsOverlays.add(tso);
			panel.add(new ExpandingPanel(sclGraph.getPanel(), false, "SCL"), 1F);
			// ------------------- SCL Artifacts-----------------------------------
			SCLArtefactSet arts = cod.getSCLArtefacts();
			Graph aGraph = new Graph(mainXAxis);
			lov1 = new LabelOverlay(aGraph);
			lov1.setLabelConfig(AmsLabelConfiguration.getSCLArtefactConfiguration());
			lov1.setLabels(arts);
			lov1.setTitle("SCL Artefacts");
			lov1.setEditable(true);
			// if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1){
			// lov1.setEditable(true);
			// }else{
			// lov1.setEditable(false);
			// }

			aGraph.addOverlay(lov1);
			aGraph.setEmptyYAxis();
			aGraph.getPanel().setPreferredSize(new Dimension(1, 20));
			aGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			panel.add(aGraph.getPanel());
			// //Only show SCL Artefacts bar in Skin Conductance tab in Event based Design
			// if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1)
			// panel.add(aGraph.getPanel());

			// ------------------- HR Graph-----------------------------------
			if (cod.channelExists("ECG")
					&& cod.getBeatSet().getBeats().isEmpty() == false) {
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
				hrGraph.addUnderlay(new EventOverlay(hrGraph));
				tso = new TimeShowOverlay(hrGraph);
				hrGraph.addOverlay(tso);
				tsOverlays.add(tso);
				panel.add(new ExpandingPanel(hrGraph.getPanel(), false, "ECG"), 1F);
			}

			if (cod.channelExists("MYA")) {
				ExpandingPanel exp;
				try {
					s = cod.getChannelInfoFromID("MYA");
					motGraph = new Graph(mainXAxis);
					yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
							(double) s.getlMinValue()
									/ s.getlMinMaxDivider(),
							(double) s.getlMaxValue() / s.getlMinMaxDivider(),
							s.getSzID() + " [" + s.getSzUnit() + "]");
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
					motGraph.addUnderlay(new EventOverlay(motGraph));
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
			BinaryDoubleDataDrawer bddd;

			File xFile = new File(cod.getFilePath(), "AVX.dbin");
			File yFile = new File(cod.getFilePath(), "AVY.dbin");
			File zFile = new File(cod.getFilePath(), "AVZ.dbin");
			Graph gr;
			YAxis ya;

			if (xFile.exists()) {
				gr = new Graph(mainXAxis);
				ya = new YAxis(0, 1, 0, 1, "XMOT [g]");
				ya.setBottomValue(0);
				ya.setTopValue(1);
				gr.setActiveYAxis(ya);
				gr.addUnderlay(new GridOverlay(gr));
				gr.addUnderlay(new EventOverlay(gr));
				bddd = new BinaryDoubleDataDrawer("XMOT", ya, xFile, 1000000.);
				bddd.connectToGraph(gr);
				tso = new TimeShowOverlay(gr);
				gr.addOverlay(tso);
				tsOverlays.add(tso);
				if (!cod.channelExists("MYA")) {
					panel.add(new ExpandingPanel(gr.getPanel(), false, "MYA"), 1F);
				}
				panels.add(new ExpandingPanel(gr.getPanel(), false, "XMOT"));
			}

			if (yFile.exists()) {
				gr = new Graph(mainXAxis);
				ya = new YAxis(0, 1, 0, 1, "YMOT [g]");
				ya.setBottomValue(0);
				ya.setTopValue(1);
				gr.setActiveYAxis(ya);
				gr.addUnderlay(new GridOverlay(gr));
				gr.addUnderlay(new EventOverlay(gr));
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
				gr.addUnderlay(new EventOverlay(gr));
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
				EE1g.addUnderlay(new EventOverlay(EE1g));
				EE1d = new TotalMotilityDrawer("EE1", yAx, true);
				EE1d.connectToGraph(EE1g);
				tso = new TimeShowOverlay(EE1g);
				EE1g.addOverlay(tso);
				tsOverlays.add(tso);
				panels.add(new ExpandingPanel(EE1g.getPanel(), false, "Total Motility"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);
		panel.add(mainXAxis.getPanel());

	}

	public void redraw() {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1) {
			if (cod.getEventSCLSet().getSCLCycles().isEmpty()) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No SCR Cycles Detected");
			} else {
				EventRelatedSCLSet sset = cod.getEventSCLSet();
				getEventInfoPanel().setSelEvent(0);
				eventiip.updateLabelTexts(sset.getSCLCycles().first(), true);
				getSelOverlay().setSelTime(sset.getSCLCycles().first().getTime());
				panel.repaint();
			}
		} else {
			if (cod.getSCLSet().getSCLCycles().isEmpty()) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No SCL Peaks Detected");
			} else if (!cod.getLabels().getLabels().isEmpty()) {
				AmsLabel l = cod.getLabels().getLabelAfterTime(mainXAxis.getLeftTime());
				if (l == null)
					l = cod.getLabels().getLabels().last();
				iip.updateLabelTexts(l);
			}
		}
	}

	public void erase() {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1) {
			if (!cod.getEventSCLSet().getSCLCycles().isEmpty()) {
				eventiip = new EventSCLInfoPanel();
				panel.removeAll();
				setupItems();
				EventRelatedSCLSet ev = cod.getEventSCLSet();
				ev.recalculate();
				getEventInfoPanel().updateLabelTexts(ev.getSCLCycles().first(), true);
				panel.repaint();
			}
		} else {
			if (!cod.getSCLSet().getSCLCycles().isEmpty()) {
				iip = new SCLInfoPanel();
				panel.removeAll();
				setupItems();
				SkinConductanceSet sSet = cod.getSCLSet();
				sSet.reCalculate();
				getInfoPanel().updateLabelTexts(cod.getLabels().getLabels().first());
				panel.repaint();
			}
		}
		super.repaint();
	}

	public void recalculateSCL() {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1)
			cod.getEventSCLSet().recalculate();
		else
			cod.getSCLSet().reCalculate();
		repaint();
	}

	public void updateLabelConfig() {
		lov.updateLabelConfig();
	}

	@Override
	public void close() throws Exception {
		if (drawTest != null)
			drawTest.close();
		if (ddrawTest != null)
			ddrawTest.close();
	}
}
