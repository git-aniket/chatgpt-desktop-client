package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.IbiDrawer;
import nl.vu.psy.ams.suite.gui.drawing.RSADrawer;
import nl.vu.psy.ams.suite.gui.drawing.RespirationRateDrawer;
import nl.vu.psy.ams.suite.gui.drawing.ResultDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SelectBarOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TextOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.RelativeLayout;

/*
 * The bar on the bottom of the screen that shows the entire
 * recording IBI signal, and allows you to move quickly
 * through the data. Implemented as a generic graph, with
 * an ibidrawer in it and a selectbaroverlay over it.
 */
public class TimeBar {

	private static TimeBar instance;

	public static TimeBar getInstance() {
		if (instance == null) {
			instance = new TimeBar();
		}
		return instance;
	}

	public static TimeBar getNewInstance() {
		if (instance != null)
			instance.close();
		instance = new TimeBar();
		return instance;
	}

	XAxis xAxis = new XAxis(true, false, 1);

	JPanel panel = new JPanel(new BorderLayout());

	private SelectBarOverlay sBarOverlay;
	private List<SelectBarOverlay> sBarOverlays = new ArrayList<SelectBarOverlay>();
	private LabelOverlay lOverlay;
	private List<DataDrawer> drawerList = new ArrayList<DataDrawer>();
	private List<Graph> graphList = new ArrayList<Graph>();
	private List<TextOverlay> overlays = new ArrayList<TextOverlay>();
	private IbiDrawer[] IbiDrawers = new IbiDrawer[3];
	private ResultDrawer PEPDrawer, LVETDrawer;
	private RSADrawer rsaDrawer;
	private RespirationRateDrawer respDrawer;
	private TextOverlay[] tOverlays = new TextOverlay[7];

	private TimeBar() {
		RelativeLayout rl = new RelativeLayout(RelativeLayout.Y_AXIS);
		rl.setFill(true);
		panel.setLayout(rl);
		panel.setMinimumSize(new Dimension(1, 25 + 40 * 3)); // CurrentOpenData.getInstance().ecgChannels()));
		panel.setMaximumSize(new Dimension(100000, 25 + 40 * 3)); // CurrentOpenData.getInstance().ecgChannels()));
		panel.setPreferredSize(new Dimension(1, 25 + 40 * 3)); // CurrentOpenData.getInstance().ecgChannels()));
		xAxis.setTimes(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getEndTimeInUS());
		int ECGchans = CurrentOpenData.getInstance().ecgChannels();
		if (ECGchans == 2)
			ECGchans = 3;
		for (int i = 0; i < ECGchans; i++) {
			IbiDrawers[i] = new IbiDrawer(i);
		}
		PEPDrawer = new ResultDrawer("PEP [msec]");
		LVETDrawer = new ResultDrawer("LVET [msec]");
		rsaDrawer = new RSADrawer("RSA");
		rsaDrawer.setDrawDots(false);
		respDrawer = new RespirationRateDrawer("RR");
		respDrawer.drawAv(false);

		for (int i = 0; i < 3; i++) {
			Graph graph = new Graph(xAxis);

			// UnComment to enable updating TimeBar during qrs detection
			// xAxis.connectToGraph(graph);
			sBarOverlay = new SelectBarOverlay(graph);
			sBarOverlays.add(sBarOverlay);
			lOverlay = new LabelOverlay(graph);
			lOverlay.setEditable(false);
			lOverlay.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
			lOverlay.setLabels(CurrentOpenData.getInstance().getLabels());
			// sBarOverlay.setLeftTime(startTime);
			// sBarOverlay.setRightTime((startTime+endTime)/2.);
			if (i > 0)
				lOverlay.setShowTooltip(false);
			graph.addUnderlay(lOverlay);
			if (i < CurrentOpenData.getInstance().ecgChannels()) {
				tOverlays[i] = new TextOverlay(graph, "IBI " + (i + 1));
				graph.addOverlay(tOverlays[i]);
				graph.addOverlay(sBarOverlay);
				overlays.add(tOverlays[i]);
				DataDrawer backGround = IbiDrawers[i];
				backGround.connectToGraph(graph);
				backGround.getYAxis().setBottomValue(0);
				backGround.getYAxis().setTopValue(1500);
				drawerList.add(backGround);
			} else if (i == 1) {
				tOverlays[3] = new TextOverlay(graph, "PEP");
				graph.addOverlay(tOverlays[3]);
				graph.addOverlay(sBarOverlay);
				overlays.add(tOverlays[3]);
				DataDrawer backGround = PEPDrawer;
				backGround.connectToGraph(graph);
				backGround.getYAxis().setBottomValue(0);
				backGround.getYAxis().setTopValue(1500);
				drawerList.add(backGround);
			} else if (i == 2) {
				tOverlays[4] = new TextOverlay(graph, "LVET");
				graph.addOverlay(tOverlays[4]);
				graph.addOverlay(sBarOverlay);
				overlays.add(tOverlays[4]);
				DataDrawer backGround = LVETDrawer;
				backGround.connectToGraph(graph);
				backGround.getYAxis().setBottomValue(0);
				backGround.getYAxis().setTopValue(1500);
				drawerList.add(backGround);
			}
			graph.setEmptyYAxis();
			graph.getPanel().setPreferredSize(new Dimension(1, 40));
			graph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
			graphList.add(graph);
			panel.add(graph.getPanel());
		}
		if (tOverlays[3] == null) {
			tOverlays[3] = new TextOverlay(graphList.get(1), "PEP");
			tOverlays[4] = new TextOverlay(graphList.get(2), "LVET");
		}
		tOverlays[5] = new TextOverlay(graphList.get(1), "RSA");
		tOverlays[6] = new TextOverlay(graphList.get(2), "RR");
		panel.add(xAxis.getPanel());

	}

	public void setICG() {
		if (!(drawerList.get(1) instanceof ResultDrawer)) {
			drawerList.get(1).disconnectFromGraph(graphList.get(1));
			drawerList.set(1, PEPDrawer);
			drawerList.get(1).connectToGraph(graphList.get(1));
			TextOverlay tOverlay = tOverlays[3];
			graphList.get(1).removeOverlay(overlays.get(1));
			graphList.get(1).addOverlay(tOverlay);

			drawerList.get(2).disconnectFromGraph(graphList.get(2));
			drawerList.set(2, LVETDrawer);
			drawerList.get(2).connectToGraph(graphList.get(2));
			TextOverlay tOverlay2 = tOverlays[4];
			graphList.get(2).removeOverlay(overlays.get(2));
			graphList.get(2).addOverlay(tOverlay2);

			overlays.remove(2);
			overlays.remove(1);
			overlays.add(tOverlay);
			overlays.add(tOverlay2);
			panel.repaint();
		}
	}

	public void setECG() {
		if (!(drawerList.get(1) instanceof IbiDrawer) && IbiDrawers[1] != null) {
			drawerList.get(1).disconnectFromGraph(graphList.get(1));
			drawerList.set(1, IbiDrawers[1]);
			drawerList.get(1).connectToGraph(graphList.get(1));
			TextOverlay tOverlay = tOverlays[1];
			graphList.get(1).removeOverlay(overlays.get(1));
			graphList.get(1).addOverlay(tOverlay);

			drawerList.get(2).disconnectFromGraph(graphList.get(2));
			drawerList.set(2, IbiDrawers[2]);
			drawerList.get(2).connectToGraph(graphList.get(2));
			TextOverlay tOverlay2 = tOverlays[2];
			graphList.get(2).removeOverlay(overlays.get(2));
			graphList.get(2).addOverlay(tOverlay2);

			overlays.remove(2);
			overlays.remove(1);
			overlays.add(tOverlay);
			overlays.add(tOverlay2);
			panel.repaint();
		}
	}

	public void setResp() {
		if (!(drawerList.get(1) instanceof RSADrawer)) {
			drawerList.get(1).disconnectFromGraph(graphList.get(1));
			drawerList.set(1, rsaDrawer);
			drawerList.get(1).connectToGraph(graphList.get(1));
			TextOverlay tOverlay = tOverlays[5];
			graphList.get(1).removeOverlay(overlays.get(1));
			graphList.get(1).addOverlay(tOverlay);

			drawerList.get(2).disconnectFromGraph(graphList.get(2));
			drawerList.set(2, respDrawer);
			drawerList.get(2).connectToGraph(graphList.get(2));
			TextOverlay tOverlay2 = tOverlays[6];
			graphList.get(2).removeOverlay(overlays.get(2));
			graphList.get(2).addOverlay(tOverlay2);

			overlays.remove(2);
			overlays.remove(1);
			overlays.add(tOverlay);
			overlays.add(tOverlay2);
			panel.repaint();
		}
	}

	public void close() {
		if (panel != null)
			MainFrame.getInstance().getSouthPanel().remove(panel);
		instance = null;
	}

	public void connectToXAxis(XAxis xAxis) {
		for (SelectBarOverlay sb : sBarOverlays)
			sb.connectToXAxis(xAxis);
	}

	public void connectToXAxis(XAxis xAxis, SelectBarOverlay overlay) {
		for (SelectBarOverlay sb : sBarOverlays)
			sb.connectToXAxis(xAxis, overlay);
	}

	public void connectToTab(AmsTab tab) {
		for (SelectBarOverlay sb : sBarOverlays)
			sb.connectToTab(tab);
	}

	public JPanel getPanel() {
		return panel;
	}

	public SelectBarOverlay getsBarOverlay() {
		return sBarOverlay;
	}

	public XAxis getXaxis() {
		return xAxis;
	}

	public void updateLabelConfig() {
		lOverlay.updateLabelConfig();
	}
}
