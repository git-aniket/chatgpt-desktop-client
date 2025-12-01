package nl.vu.psy.ams.suite.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.gui.ExternalFilePanel;
import nl.vu.psy.ams.suite.gui.MainMenuBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SelectBarOverlay;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

@SuppressWarnings("unused")
public class AmsTab extends JPanel {
	/**
	 * Base class that implements a tab. If you want to add a new tab to the
	 * suite, extend this class. If you need to do special work when activating
	 * / deactivating the tab, override setActive and/or setUnactive.
	 */
	private static final long serialVersionUID = 1L;
	protected JPanel panel = new JPanel();
	private AmsToolBar toolBar;
	private boolean isActive = false;
	protected XAxis mainXAxis = new XAxis();
	protected Graph topgraph = new Graph(mainXAxis);
	protected SelectBarOverlay topOverlay = new SelectBarOverlay(topgraph, Color.ORANGE);

	public AmsTab() {
		super(new BorderLayout());
		RelativeLayout rl = new RelativeLayout(RelativeLayout.Y_AXIS);
		rl.setFill(true);
		// panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setLayout(rl);
		this.setLayout(new BorderLayout());
		setupItems();
		this.add(panel, BorderLayout.CENTER);
	}

	public void autoscale() {

	}

	public AmsToolBar getToolBar() {
		return toolBar;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean setActive() {
		if (toolBar != null) { // StartTab
			repaint();
			toolBar.addButtons();
			MainMenuBar.getInstance().setActionMenu(toolBar.getActionMenu());
			if (AppSettings.getInstance().getIntProperty(Settings.AUTOSCALEUPONTABCHANGE) != 0)
				autoscale();
			ExternalFilePanel.getInstance().setXAxis(mainXAxis);
		}
		isActive = true;
		return true;
	}

	public void setToolBar(AmsToolBar toolBar) {
		this.toolBar = toolBar;
		this.add(toolBar, BorderLayout.PAGE_START);
	}

	public boolean setUnactive() {
		toolBar.clearAllButtons();
		isActive = false;
		return true;
	}

	public void setupItems() {

	}

	public void selectLabel(double time) {

	}
}
