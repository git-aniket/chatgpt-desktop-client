package nl.vu.psy.ams.suite.gui.tabs.rsa;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;

/*
 * The RSA scoring toolbar
 */
public class RSAToolbar extends AmsToolBar {

	private RSATab tab;
	private RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RSAToolbar(RSATab tab) {
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
		} else if (actionCommand.equals("moveLeftOneScreen")) {
			tab.getXAxis().move(-1);
		} else if (actionCommand.equals("moveRightOneScreen")) {
			tab.getXAxis().move(1);
		} else if (actionCommand.equals("zoomIn4X")) {
			tab.getXAxis().zoom(4);
		} else if (actionCommand.equals("zoomOut4X")) {
			tab.getXAxis().zoom(0.25);
		} else if (actionCommand.equals("autoscaleAll")) {
			tab.getXAxis().autoscaleConnectedGraphs();
		} else if (actionCommand.equals("autoscaleAllRaw")) {
			tab.getXAxis().autoscaleConnectedGraphs(0);
		} else if (actionCommand.equals("moveHighSusp")) {
			double time = rSet.getBeatSortedByIBISuspicion(0).getInspStart();
			rSet.setSelectedSuspiciousRR(0);
			tab.getXAxis().moveToTimeFast(time);
			tab.getXAxis().autoscaleConnectedGraphsFast();
			tab.moveSelectedCycleToCenterScreen();
		} else if (actionCommand.equals("movePrevSusp")) {
			int curSel = rSet.getSelectedSuspiciousRR();
			if (curSel > 0)
				curSel--;
			double time = rSet.getBeatSortedByIBISuspicion(curSel).getInspStart();
			rSet.setSelectedSuspiciousRR(curSel);
			tab.getXAxis().moveToTimeFast(time);
			tab.getXAxis().autoscaleConnectedGraphsFast();
			tab.moveSelectedCycleToCenterScreen();
		} else if (actionCommand.equals("moveNextSusp")) {
			int curSel = rSet.getSelectedSuspiciousRR();
			if (curSel < rSet.numberOfSuspiciousCycles() - 1)
				curSel++;
			double time = rSet.getBeatSortedByIBISuspicion(curSel).getInspStart();
			rSet.setSelectedSuspiciousRR(curSel);
			tab.getXAxis().moveToTimeFast(time);
			tab.getXAxis().autoscaleConnectedGraphsFast();
			tab.moveSelectedCycleToCenterScreen();
		} else if (actionCommand.equals("recheckIBIs")) {
			rSet.checkForIrregularRR();
			rSet.recheckSuspiciousRRS();
			CurrentOpenData.getInstance().setDirty(true);
			RespirationCycle cycle = rSet.getBeatSortedByIBISuspicion(0);
			if (cycle != null) {
				double time = cycle.getInspStart();
				rSet.setSelectedSuspiciousRR(0);
				tab.getXAxis().moveToTimeFast(time);
				tab.getXAxis().autoscaleConnectedGraphsFast();
			}
		} else if (actionCommand.equals("prevCycle")) {
			tab.prevCycle();
		} else if (actionCommand.equals("nextCycle")) {
			tab.nextCycle();
		} else if (actionCommand.equals("midCycle")) {
			tab.moveSelectedCycleToCenterScreen();
		} else if (actionCommand.equals("recalc")) {
			int retVal = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"Are you sure you want recalculate RSA for the entire registration? This will remove all manual changes!",
					"Confirm RSA recalculation",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (retVal == JOptionPane.YES_OPTION)
				tab.recalculateRSA();
		} else if (actionCommand.equals("exportRSR")) {
			File outFile = SaveDialog.saveFileBrowser("rsr", "RSR File");
			if (outFile != null) {
				// For Batch export of RSR files, a boolean variable called isAppend is added.
				// Here isAppend == false.
				rSet.toRSRFile(outFile, false);
			}
		} else if (actionCommand.equals("showHideArt")) {
			tab.flipShowHideArtefacts();
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
		setupButton("redball", "Show/Hide Artefact RSA/RR", "showHideArt",
				KeyStroke.getKeyStroke(KeyEvent.VK_H, 0, true));
		setupButton("autoscale_green", "Autoscale All", "autoscaleAll",
				KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		setupButton("autoscale", "Autoscale All on Raw Data", "autoscaleAllRaw",
				KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
		addNewSeparator();
		setupButton("firstsusp", "Move to most diverging cycle", "moveHighSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
		setupButton("prevsusp", "Move to previous most diverging cycle", "movePrevSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("nextsusp", "Move to next most diverging cycle", "moveNextSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
		setupButton("rechecksusp", "Recheck for diverging cycle", "recheckIBIs",
				KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("left", "Previous Respiration Cycle", "prevCycle",
				KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("right", "Next Respiration Cycle", "nextCycle",
				KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
		setupButton("settocenter", "Set Selected Respiration Cycle To Center Of Screen", "midCycle",
				KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
		addNewSeparator();
		setupButton("redo", "Recalculate RSA", "recalc", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, true));
		addNewSeparator();
		setupButton("export", "Export To RSR File", "exportRSR", KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, true));
	}

}
