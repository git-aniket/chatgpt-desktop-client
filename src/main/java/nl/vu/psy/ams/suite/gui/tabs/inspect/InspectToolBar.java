package nl.vu.psy.ams.suite.gui.tabs.inspect;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.KeyStroke;

import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;

/*
 * The inspect data toolbar
 */
public class InspectToolBar extends AmsToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private InspectTab tab;

	public InspectToolBar(InspectTab tab) {
		super();

		this.tab = tab;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("moveLeft")) {
			tab.getXAxis().move(-0.25);
		} else if (actionCommand.equals("moveRight")) {
			tab.getXAxis().move(0.25);
		} else if (actionCommand.equals("zoomIn")) {
			tab.getXAxis().zoom(2);
		} else if (actionCommand.equals("zoomOut")) {
			tab.getXAxis().zoom(0.5);
		} else if (actionCommand.equals("autoscaleAll")) {
			// tab.getXAxis().autoscaleConnectedGraphs();
			tab.autoscaleSelected(1);
		} else if (actionCommand.equals("autoscaleAllDebug")) {
			tab.getXAxis().autoscaleConnectedGraphs(true);
		} else if (actionCommand.equals("autoscaleAllRaw")) {
			// tab.getXAxis().autoscaleConnectedGraphs(0);
			tab.autoscaleSelected(0);
		} else if (actionCommand.equals("moveLeftOneScreen")) {
			tab.getXAxis().move(-1);
		} else if (actionCommand.equals("moveRightOneScreen")) {
			tab.getXAxis().move(1);
		} else if (actionCommand.equals("zoomIn4X")) {
			tab.getXAxis().zoom(4);
		} else if (actionCommand.equals("zoomOut4X")) {
			tab.getXAxis().zoom(0.25);
		}
	}

	@Override
	public void addButtons() {
		setupButton("left", "Move Left \u00BC Screen", "moveLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true));
		setupButton("right", "Move Right \u00BC Screen", "moveRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true));
		setupButton("zoomin", "Zoom In 2X", "zoomIn", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true));
		setupButton("zoomout", "Zoom Out 2X", "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true));
		addNewSeparator();
		setupButton("left_left", "Move Left 1 Screen", "moveLeftOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true));
		setupButton("right_right", "Move Right 1 Screen", "moveRightOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true));
		setupButton("zoominin", "Zoom In 4X", "zoomIn4X", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, true));
		setupButton("zoomoutout", "Zoom Out 4X", "zoomOut4X", KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, true));
		addNewSeparator();
		setupButton("autoscale_green", "Autoscale All", "autoscaleAll",
				KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		setupButton("autoscale", "Autoscale All on Raw Data", "autoscaleAllRaw",
				KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
		File curFile = new File(System.getProperty("user.dir"), "VUDAMSDebug.json");
		if (curFile.exists()) {
			setupButton("autoscale", "Autoscale All Debug", "autoscaleAllDebug",
					KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, true));
		}
	}

}
