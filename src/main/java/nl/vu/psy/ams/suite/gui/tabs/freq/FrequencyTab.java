package nl.vu.psy.ams.suite.gui.tabs.freq;

import java.awt.Dimension;
import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRDrawer;
import nl.vu.psy.ams.suite.gui.drawing.LFHFDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;

/*
 * The frequency analasys tab
 */
public class FrequencyTab extends AmsTab {

	private static final long serialVersionUID = 1L;
	private static FrequencyTab instance;

	public static FrequencyTab getInstance() {
		if (instance == null) {
			instance = new FrequencyTab();
		}
		return instance;
	}

	public static FrequencyTab getNewInstance() {
		instance = new FrequencyTab();
		return instance;
	}

	private Graph g;

	private HRDrawer hrDrawer;
	private LFHFDrawer lfhfDrawer;
	private LabelOverlay lov;

	private FrequencyTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new FrequencyToolbar(this));
	}

	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
	}

	public HRDrawer getHRDrawer() {
		return hrDrawer;
	}

	public LFHFDrawer getLfhfDrawer() {
		return lfhfDrawer;
	}

	public Graph getWaveletGraph() {
		return g;
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}

	@Override
	public boolean setActive() {
		super.setActive();
		hrDrawer.recalcAverage();
		lfhfDrawer.recalculate();
		// lfhfratDrawer.recalculate();
		return true;
	}

	@Override
	public void setupItems() {

		Graph tGraph = new Graph(mainXAxis);
		lov = new LabelOverlay(tGraph);
		lov.setEditable(false);
		lov.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
		lov.setLabels(CurrentOpenData.getInstance().getLabels());
		lov.setTitle("Labels");
		tGraph.addOverlay(lov);
		tGraph.setEmptyYAxis();
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		panel.add(tGraph.getPanel());

		Graph hrGraph = new Graph(mainXAxis);
		hrGraph.addUnderlay(new GridOverlay(hrGraph));
		hrDrawer = new HRDrawer();
		hrDrawer.connectToGraph(hrGraph);
		hrDrawer.drawAverageHR(false);
		hrGraph.setActiveYAxis(hrDrawer.getYAxis());
		panel.add(hrGraph.getPanel());

		Graph lfhfGraph = new Graph(mainXAxis);
		lfhfGraph.addUnderlay(new GridOverlay(lfhfGraph));
		lfhfDrawer = new LFHFDrawer("LF+HF", new YAxis(0, 1000, 0, 1000, "Power [ms\u00B2]"));
		lfhfDrawer.connectToGraph(lfhfGraph);
		lfhfGraph.setActiveYAxis(lfhfDrawer.getYAxis());
		lfhfDrawer.getYAxis().setBottomValue(0);
		lfhfDrawer.getYAxis().setTopValue(1000);
		panel.add(lfhfGraph.getPanel());

		if (CurrentOpenData.getInstance().channelExists("MYA")) {// JdH only try when MYA exists
			Ams7fsChannelInfo s;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("MYA");
				Graph motGraph = new Graph(mainXAxis);
				YAxis yAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
				yAxis.setBottomValue(Integer.MIN_VALUE);
				yAxis.setTopValue(Integer.MAX_VALUE);
				motGraph.setActiveYAxis(yAxis);
				@SuppressWarnings("resource")
				BinaryDataDrawer motDrawer = new BinaryDataDrawer(s.getSzID(), yAxis);
				motDrawer.connectToGraph(motGraph);
				motGraph.addUnderlay(new GridOverlay(motGraph));
				yAxis.autoScale();
				panel.add(motGraph.getPanel());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * Graph lfhfratGraph = new Graph(xAxis); lfhfratGraph.addUnderlay(new
		 * GridOverlay(lfhfratGraph)); lfhfratDrawer = new LFHFDrawer("LF/HF",
		 * new YAxis(0, 1000, 0, 1000, "LF/HF"));
		 * lfhfratDrawer.setDrawRatio(true);
		 * lfhfratDrawer.connectToGraph(lfhfratGraph);
		 * lfhfratGraph.setActiveYAxis(lfhfratDrawer.getYAxis());
		 * lfhfratDrawer.getYAxis().setBottomValue(0);
		 * lfhfratDrawer.getYAxis().setTopValue(10);
		 * panel.add(lfhfratGraph.getPanel());
		 */

		/*
		 * g = new Graph(xAxis); wdraw = new LombDrawer("Lomb Periodogram", new
		 * YAxis(0, 1, 0, 1, "Frequency (Hz)")); wdraw.connectToGraph(g);
		 * g.setActiveYAxis(wdraw.getYAxis()); panel.add(g.getPanel());
		 */
		panel.add(mainXAxis.getPanel());

	}

	public void updateLabelConfig() {
		lov.updateLabelConfig();
	}
}
