package nl.vu.psy.ams.suite.gui.graphgen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.EctopicBeatsDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;

public class GenerateEctopicBeatsGraph extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double[] ectopicbeatsValues;
	private XAxis xAxis;
	private EctopicBeatsDrawer ectopicbeatsdrawer;
	@SuppressWarnings("unused")
	private String title;

	public GenerateEctopicBeatsGraph() {

		super();
		setPreferredSize(new Dimension(800, 400));
		setMinimumSize(new Dimension(800, 400));
		setMaximumSize(new Dimension(800, 400));
		setBackground(Color.WHITE);

		xAxis = new XAxis(false, true, 0);
		xAxis.setPopupEnabled(false);
		xAxis.getTimeFormatSeconds().setSelected(true);
		xAxis.setTrueSeconds(false);
		xAxis.setTimes(-700000, 3400000);

		Graph graph = new Graph(xAxis);
		ectopicbeatsdrawer = new EctopicBeatsDrawer("ECG", new YAxis(-1, 1, -1, 1, "ECG [mV]"), "ECG");
		ectopicbeatsdrawer.setretArray();
		ectopicbeatsdrawer.getYAxis().setBottomValue(-6);
		ectopicbeatsdrawer.getYAxis().setTopValue(10);
		ectopicbeatsdrawer.setSelPart(0);
		ectopicbeatsdrawer.connectToGraph(graph);
		graph.setActiveYAxis(ectopicbeatsdrawer.getYAxis());
		graph.addOverlay(new TimeShowOverlay(graph));

		setLayout(new BorderLayout());
		add(graph.getPanel(), BorderLayout.CENTER);
		add(xAxis.getDoubleFillerPanel(), BorderLayout.SOUTH);

		setTitle("Ectopic beats");
		setXAxisTitle("Time [ms]");
	}

	public EctopicBeatsDrawer getDrawer() {
		return ectopicbeatsdrawer;
	}

	public void setectopicBeatValues(double[] vals) {
		ectopicbeatsValues = vals;
	}

	public double[] getectopicBeatValues() {
		return ectopicbeatsValues;
	}

	public String getXAxisTitle() {
		return xAxis.getAxisTitle();
	}

	public void setTitle(String title) {
		if (title.length() == 0) {
			this.title = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
		} else {
			this.title = title;
		}
		repaint();
	}

	public void setXAxisTitle(String title) {
		if (title.length() == 0) {
			xAxis.setAxisTitle(null);
		} else {
			xAxis.setAxisTitle(title);
		}
		repaint();
	}
}
