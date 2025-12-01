package nl.vu.psy.ams.suite.gui.tabs.imp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.ECGScoreOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.ImpScoreOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.ImpScoreOverlayMot;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.tools.ThreadServer;

public class ImpTab extends AmsTab {
	/*
	 * The impedance tab
	 */
	private static final long serialVersionUID = 1L;
	protected XAxis mainXAxis = new XAxis();
	private static ImpTab instance;
	public Thread recalcThread;

	public static ImpTab getInstance() {
		if (instance == null) {
			instance = new ImpTab();
		}
		return instance;
	}

	public static ImpTab getNewInstance() {
		instance = null;
		instance = new ImpTab();
		return instance;
	}

	private SignalPartDrawer iD, iDraw;

	private ImpInfoPanel iip;

	private ImpScoreOverlay iso;

	private SignalPartDrawer ecgD, ecgDraw;

	private ECGScoreOverlay eso;

	private SignalPartDrawer prt, prtFilt;
	private ArrayList<SignalPartDrawer> prts, prtsFilt;

	private XAxis xAxis;
	private int selPanel = 0;
	private ExpandingPanel exp;
	private Graph g3;
	private ArrayList<Ams7fsChannelInfo> chans;
	YAxis prtYAxis;

	private ImpTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		TimeBar.getInstance().connectToTab(this);
		setToolBar(new ImpToolbar(this));
	}

	@Override
	public void autoscale() {
		// xAxis.autoscaleConnectedGraphs();
	}

	public void tabautoscale() {
		xAxis.autoscaleConnectedGraphs(1);
	}

	public void tabautoscaleRaw() {
		xAxis.autoscaleConnectedGraphs(0);
	}

	public void defaultscale() {
		xAxis.defaultscaleConnectedGraphs();
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	public XAxis getAEXAxis() {
		return xAxis;
	}

	public SignalPartDrawer getECGDrawer() {
		return ecgD;
	}

	public SignalPartDrawer getECGRawDrawer() {
		return ecgDraw;
	}

	public ECGScoreOverlay getEso() {
		return eso;
	}

	public ImpInfoPanel getIip() {
		return iip;
	}

	public SignalPartDrawer getImpDrawer() {
		return iD;
	}

	public SignalPartDrawer getImpRawDrawer() {
		return iDraw;
	}

	public ImpScoreOverlay getIso() {
		return iso;
	}

	public SignalPartDrawer getTMPDrawer() {
		if (prts == null)
			return null;
		return prts.get(selPanel);
	}

	public SignalPartDrawer getTMPFiltDrawer() {
		if (prtsFilt == null)
			return null;
		return prtsFilt.get(selPanel);
	}

	public ArrayList<SignalPartDrawer> getDrawers() {
		return prts;
	}

	public ArrayList<SignalPartDrawer> getFiltDrawers() {
		return prtsFilt;
	}

	public ExpandingPanel getExp() {
		return exp;
	}

	public void nextPanel() {
		prts.get(selPanel).disconnectFromGraph(g3);
		prtsFilt.get(selPanel).disconnectFromGraph(g3);
		selPanel++;
		if (selPanel >= prts.size())
			selPanel = 0;
		Ams7fsChannelInfo chan = chans.get(selPanel);
		prtYAxis.setAxisTitle(chan.getSzID() + " [" + chan.getSzUnit() + "]");
		prts.get(selPanel).connectToGraph(g3);
		prtsFilt.get(selPanel).connectToGraph(g3);
		if (getTMPDrawer() != null)
			getTMPDrawer().getYAxis().autoScale(selPanel * 2 + 1);
		MainFrame.getInstance().getMainFrame().repaint();
	}

	public void prevPanel() {
		prts.get(selPanel).disconnectFromGraph(g3);
		prtsFilt.get(selPanel).disconnectFromGraph(g3);
		selPanel--;
		if (selPanel < 0)
			selPanel = prts.size() - 1;
		Ams7fsChannelInfo chan = chans.get(selPanel);
		prtYAxis.setAxisTitle(chan.getSzID() + " [" + chan.getSzUnit() + "]");
		prts.get(selPanel).connectToGraph(g3);
		prtsFilt.get(selPanel).connectToGraph(g3);
		if (getTMPDrawer() != null)
			getTMPDrawer().getYAxis().autoScale(selPanel * 2 + 1);
		MainFrame.getInstance().getMainFrame().repaint();
	}

	@Override
	public boolean setActive() {
		// super.setActive(); -- Made the tab active only when labels are present - This
		// avoid multiple toolbars in ImpTab _ needs to be checked
		if (CurrentOpenData.getInstance().getLabels().getLabels().isEmpty()
				&& (CurrentOpenData.getInstance().isBatchAnalysis() == false)) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No labels present");
			iD.setSelPart(0);
			ecgD.setSelPart(0);
			if (prt != null)
				prt.setSelPart(0);
			iDraw.setSelPart(0);
			ecgDraw.setSelPart(0);
			if (prt != null)
				prtFilt.setSelPart(0);
			return false;
		} else {
			super.setActive(); // Comment this and uncomment the first line
			redraw(iD.getSelIndex());
			iD.getYAxis().autoScale();
			ecgD.getYAxis().setBottomValue(-1);
			ecgD.getYAxis().setTopValue(1);
			if (prts != null)
				prts.get(0).getYAxis().autoScale();
			TimeBar.getInstance().setICG();
			return true;
		}
	}

	public boolean setUnactive() {

		super.setUnactive();
		if (CurrentOpenData.getInstance().getLabels().getLabels().isEmpty() == false) {
			LabelTab.getInstance().getXAxis().setTimes(iD.getSelPart().getLabel().getLeftTime() - 18000000,
					iD.getSelPart().getLabel().getRightTime() + 18000000);
			LabelTab.getInstance().getXAxis().updateAll();
		}
		return true;

	}

	@Override
	public void setupItems() {
		prts = new ArrayList<SignalPartDrawer>();
		prtsFilt = new ArrayList<SignalPartDrawer>();
		JPanel pan2 = new JPanel();
		JPanel pan3 = new JPanel();
		xAxis = new XAxis(false, true, 0);
		xAxis.setPopupEnabled(false);
		xAxis.getTimeFormatSeconds().setSelected(true);
		xAxis.setTrueSeconds(false);
		xAxis.setIsLive(true);
		xAxis.setLeftTimeF(-256000);
		xAxis.setRightTimeF(750000);

		Vector<TimeShowOverlay> tsOverlays = new Vector<TimeShowOverlay>();
		Graph g = new Graph(xAxis);
		YAxis iDYAxis = new YAxis(-1, 1, -1, 1, "-" + InspectTab.getInstance().getSignalName("DZDT"));
		iDraw = new SignalPartDrawer("IMP", iDYAxis, "DZDT", false);
		iD = new SignalPartDrawer("IMP", iDYAxis, "DZDT");

		iDraw.connectToGraph(g);
		iD.connectToGraph(g);
		iD.setInvert(true);
		iDraw.setInvert(true);
		g.setActiveYAxis(iD.getYAxis());

		g.addUnderlay(new GridOverlay(g));
		iso = new ImpScoreOverlay(g);
		getIso().setImpDrawer(iD);
		g.addUnderlay(getIso());
		TimeShowOverlay tso = new TimeShowOverlay(g);
		g.addOverlay(tso);
		tsOverlays.add(tso);

		Graph g2 = new Graph(xAxis);
		YAxis ecgDYAxis = new YAxis(-1, 1, -1, 1, InspectTab.getInstance().getSignalName("ECG"));
		ecgDraw = new SignalPartDrawer("ECG", ecgDYAxis, "ECG", false);
		ecgD = new SignalPartDrawer("ECG", ecgDYAxis, "ECG");

		ecgDraw.connectToGraph(g2);
		ecgD.connectToGraph(g2);
		// ecgD.getYAxis().setPopupEnabled(true);
		g2.setActiveYAxis(ecgD.getYAxis());

		g2.addUnderlay(new GridOverlay(g2));
		eso = new ECGScoreOverlay(g2);
		getEso().setECGDrawer(ecgD);
		g2.addOverlay(getEso());
		tso = new TimeShowOverlay(g2);
		g2.addOverlay(tso);
		tsOverlays.add(tso);
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.Y_AXIS));
		pan3.setLayout(new BoxLayout(pan3, BoxLayout.Y_AXIS));
		Ams7fsChannelInfo chan = null;
		g3 = new Graph(xAxis);
		exp = new ExpandingPanel(g3.getPanel(), false, "mot");
		pan3.add(exp);
		g3.addUnderlay(new GridOverlay(g3));
		ImpScoreOverlayMot ism = new ImpScoreOverlayMot(g3);
		ism.setImpDrawer(iD);
		g3.addUnderlay(ism);
		tso = new TimeShowOverlay(g3);
		g3.addOverlay(tso);
		tsOverlays.add(tso);
		String[] chanNames = { "MXR", "MYR", "MZR", "GyroX", "GyroY", "GyroZ" };
		chans = new ArrayList<Ams7fsChannelInfo>();
		boolean isFirst = true;
		try {
			for (String chanName : chanNames) {
				if (CurrentOpenData.getInstance().channelExists(chanName)) {
					chan = CurrentOpenData.getInstance().getChannelInfoFromID(chanName);
					chans.add(chan);
					if (isFirst)
						prtYAxis = new YAxis(-1, 1, -1, 1, chan.getSzID() + " [" + chan.getSzUnit() + "]");
					prt = new SignalPartDrawer(chan.getSzID(), prtYAxis, chan.getSzID(), false);
					prts.add(prt);
					prtFilt = new SignalPartDrawer(chan.getSzID(), prtYAxis, chan.getSzID());
					prtsFilt.add(prtFilt);
					if (isFirst) {
						prt.connectToGraph(g3);
						prtFilt.connectToGraph(g3);
						g3.setActiveYAxis(prt.getYAxis());
						// motPan.add(exp);
						exp.setVisible(false);
						LayoutManager2 layout = (LayoutManager2) pan3.getLayout();
						layout.addLayoutComponent(exp, null);
						isFirst = false;
					}
				}
			}
			if (g3.getActiveYAxis() == null) // prevent null pointer in getActiveYAxis when no movement data in
												// 5fs/amsdata file
				g3.setActiveYAxis(new YAxis(-1, 1, -1, 1, "dummy"));
			if (prts.size() == 0) {
				prts = null;
			}
			if (prtsFilt.size() == 0) {
				prtsFilt = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);

		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
		iip = new ImpInfoPanel();
		iip.setImpDrawer(iD);
		iip.setEcgDrawer(ecgD);

		getIso().setImpInfoPanel(iip);
		getEso().setImpInfoPanel(iip);
		getEso().setImpOverlay(iso);

		final JPanel gPanel = g.getPanel();
		pan3.add(gPanel);
		final JPanel g2Panel = g2.getPanel();
		pan3.add(g2Panel);
		pan2.add(pan3);
		pan2.add(xAxis.getPanel());
		pan.add(iip);
		pan.add(pan2);

		pan3.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentResized(ComponentEvent e) {
				Component source = e.getComponent();
				int height = source.getHeight();
				int topHeight = 2 * height / 3;
				int botHeight = height - topHeight;
				if (prts == null) {
					gPanel.setPreferredSize(new Dimension(gPanel.getPreferredSize().width, topHeight));
					g2Panel.setPreferredSize(new Dimension(g2Panel.getPreferredSize().width, botHeight));
				} else {
					topHeight = height / 3;
					botHeight = topHeight / 2;
					gPanel.setPreferredSize(new Dimension(gPanel.getPreferredSize().width, topHeight));
					g2Panel.setPreferredSize(new Dimension(g2Panel.getPreferredSize().width, botHeight));
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}
		});

		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, iip, pan2);

		panel.add(jsp, 1F);
	}

	public void redraw(int part) {
		int i = 0;
		try {
			if (CurrentOpenData.getInstance().getLoadLabelThread() != null)
				CurrentOpenData.getInstance().getLoadLabelThread().join();
			if (CurrentOpenData.getInstance().getTimeLabelThread() != null)
				CurrentOpenData.getInstance().getTimeLabelThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		AmsLabel currentLabel = CurrentOpenData.getInstance().getLabels()
				.getLabelAfterTime(mainXAxis.getLeftTime());
		for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
			if (l == currentLabel)
				part = i;
			i++;
		}
		iD.setSelPart(part);
		ecgD.setSelPart(part);
		if (prts != null) {
			for (SignalPartDrawer pr : prts) {
				pr.setSelPart(part);
			}
		}
		iDraw.setSelPart(part);
		ecgDraw.setSelPart(part);
		if (prtsFilt != null) {
			for (SignalPartDrawer pr : prtsFilt) {
				pr.setSelPart(part);
			}
		}
		iip.updateLabelTexts();
		repaint();
		if (CurrentOpenData.getInstance().isBatchAnalysis() == true) {

			if (CurrentOpenData.getInstance().getAnalysisType() == 3) {
				for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
					l.resettoAlgo();
				}
			}
			// getImpDrawer().batchLooping();
			// getECGDrawer().batchLooping();
		}
	}

	public void recalculate() {
		try {
			if (CurrentOpenData.getInstance().getLoadLabelThread() != null)
				CurrentOpenData.getInstance().getLoadLabelThread().join();
			if (CurrentOpenData.getInstance().getTimeLabelThread() != null)
				CurrentOpenData.getInstance().getTimeLabelThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (recalcThread != null) {
			// recalcThread.interrupt();
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
			ThreadServer.removeThread(recalcThread);
		}
		recalcThread = new Thread() {
			@Override
			public void run() {
				@SuppressWarnings("unchecked")
				ArrayList<Integer>[] outer = new ArrayList[CurrentOpenData.getInstance().getLabels()
						.getLabels().size()];
				for (int i = 0; i < CurrentOpenData.getInstance().getLabels().getLabels().size(); i++) {
					outer[i] = new ArrayList<>();
				}
				if (ecgD.parts.isEmpty()) {
					ecgD.getSignalPartSet().recalculate(outer);
					ecgD.setSelPart(0);
				}
				if (iD.parts.isEmpty()) {
					iD.getSignalPartSet().recalculate(outer);
					iD.setSelPart(0);
				}
				if (prts != null) {
					for (SignalPartDrawer pr : prts) {
						if (pr.parts.isEmpty()) {
							pr.getSignalPartSet().recalculate(outer);
							pr.setSelPart(0);
						}
					}
				}
				if (ecgDraw.parts.isEmpty()) {
					ecgDraw.getSignalPartSet().recalculate(outer);
					ecgDraw.setSelPart(0);
				}
				if (iDraw.parts.isEmpty()) {
					iDraw.getSignalPartSet().recalculate(outer);
					iDraw.setSelPart(0);
				}
				if (prtsFilt != null) {
					for (SignalPartDrawer pr : prtsFilt) {
						if (pr.parts.isEmpty()) {
							pr.getSignalPartSet().recalculate(outer);
							pr.setSelPart(0);
						}
					}
				}
				repaint();
			}

		};
		ThreadServer.addNewThread(recalcThread);
		recalcThread.start();

		if (CurrentOpenData.getInstance().isBatchAnalysis() == true) {

			if (CurrentOpenData.getInstance().getAnalysisType() == 3) {
				for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
					l.resettoAlgo();
				}
			}
			// getImpDrawer().batchLooping();
			// getECGDrawer().batchLooping();
		}
	}

	public void selectLabel(double time) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		AmsLabelSet ls = cod.getLabels();
		AmsLabel lbl = ls.getLabelUnderTime(time);
		if (lbl != null) {
			int labelNo = lbl.getLabelNo();
			iD.setSelPart(labelNo - 1);
			ecgD.setSelPart(labelNo - 1);
			if (prts != null)
				for (SignalPartDrawer pr : prts)
					pr.setSelPart(labelNo - 1);
			iDraw.setSelPart(labelNo - 1);
			ecgDraw.setSelPart(labelNo - 1);
			if (prtsFilt != null)
				for (SignalPartDrawer pr : prtsFilt)
					pr.setSelPart(labelNo - 1);
			if (getTMPDrawer() != null)
				getTMPDrawer().getYAxis().autoScale();
			iip.updateLabelTexts();
		}
	}
}
