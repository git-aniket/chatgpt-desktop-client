package nl.vu.psy.ams.suite.gui.tabs.actigraph;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;
/*
 * The frequency analasys toolbar
 */
public class ActigraphToolbar extends AmsToolBar {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private ActigraphTab		tab;

	public ActigraphToolbar(ActigraphTab tab) {
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
			tab.getXAxis().autoscaleConnectedGraphs();
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
		setupButton("right", "Move Right \u00BC Screen", "moveRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true));
		setupButton("zoomin", "Zoom In 2X", "zoomIn", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true));
		setupButton("zoomout", "Zoom Out 2X", "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true));
		addNewSeparator();
		setupButton("left_left", "Move Left 1 Screen", "moveLeftOneScreen", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true));
		setupButton("right_right", "Move Right 1 Screen", "moveRightOneScreen", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true));
		setupButton("zoominin", "Zoom In 4X", "zoomIn4X", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, true));
		setupButton("zoomoutout", "Zoom Out 4X", "zoomOut4X", KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, true));
		addNewSeparator();
		setupButton("autoscale", "Autoscale All", "autoscaleAll", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
	}

}