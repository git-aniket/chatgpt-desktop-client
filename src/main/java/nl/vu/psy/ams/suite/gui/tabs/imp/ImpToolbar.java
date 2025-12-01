package nl.vu.psy.ams.suite.gui.tabs.imp;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;

public class ImpToolbar extends AmsToolBar {

	/*
	 * The impedance toolbar
	 */
	private static final long serialVersionUID = 1L;

	protected TreeSet<AmsLabel> labels = new TreeSet<AmsLabel>();
	private ImpTab tab;

	public ImpToolbar(ImpTab tab) {
		super();
		this.tab = tab;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String actionCommand = arg0.getActionCommand();
		if (actionCommand.equals("moveAvLeft")) {
			tab.getImpDrawer().prevPart();
			tab.getECGDrawer().prevPart();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.prevPart();
			tab.getImpRawDrawer().prevPart();
			tab.getECGRawDrawer().prevPart();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.prevPart();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveAvRight")) {
			tab.getImpDrawer().nextPart();
			tab.getECGDrawer().nextPart();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.nextPart();
			tab.getImpRawDrawer().nextPart();
			tab.getECGRawDrawer().nextPart();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.nextPart();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveAvFirst")) {
			tab.getImpDrawer().firstPart();
			tab.getECGDrawer().firstPart();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.firstPart();
			tab.getImpRawDrawer().firstPart();
			tab.getECGRawDrawer().firstPart();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.firstPart();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveAvLast")) {
			tab.getImpDrawer().lastPart();
			tab.getECGDrawer().lastPart();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.lastPart();
			tab.getImpRawDrawer().lastPart();
			tab.getECGRawDrawer().lastPart();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.lastPart();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveLeft")) {
			tab.getImpDrawer().prevBeat();
			tab.getECGDrawer().prevBeat();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.prevBeat();
			tab.getImpRawDrawer().prevBeat();
			tab.getECGRawDrawer().prevBeat();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.prevBeat();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveRight")) {
			tab.getImpDrawer().nextBeat();
			tab.getECGDrawer().nextBeat();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.nextBeat();
			tab.getImpRawDrawer().nextBeat();
			tab.getECGRawDrawer().nextBeat();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.nextBeat();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveFirst")) {
			tab.getImpDrawer().firstBeat();
			tab.getECGDrawer().firstBeat();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.firstBeat();
			tab.getImpRawDrawer().firstBeat();
			tab.getECGRawDrawer().firstBeat();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.firstBeat();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("moveLast")) {
			tab.getImpDrawer().lastBeat();
			tab.getECGDrawer().lastBeat();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.lastBeat();
			tab.getImpRawDrawer().lastBeat();
			tab.getECGRawDrawer().lastBeat();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.lastBeat();
			if (tab.getTMPDrawer() != null) {
				tab.getTMPDrawer().getYAxis().autoScale();
			}
		} else if (actionCommand.equals("showHideIndiv")) {
			tab.getImpDrawer().flipShowHideIndiv();
			tab.getECGDrawer().flipShowHideIndiv();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.flipShowHideIndiv();
			tab.getImpRawDrawer().flipShowHideIndiv();
			tab.getECGRawDrawer().flipShowHideIndiv();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.flipShowHideIndiv();
		} else if (actionCommand.equals("showHideRaw")) {
			tab.getImpDrawer().flipShowHideRaw();
			tab.getECGDrawer().flipShowHideRaw();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.flipShowHideRaw();
			tab.getImpRawDrawer().flipShowHideRaw();
			tab.getECGRawDrawer().flipShowHideRaw();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.flipShowHideRaw();
		} else if (actionCommand.equals("saveScore")) {
			String name = JOptionPane.showInputDialog("Enter saved name");
			if (name != null) {
				tab.getImpDrawer().saveICGScoring(name);
			}
		} else if (actionCommand.equals("showSaved")) {
			tab.getIso().flipShowSavedScoring();
			tab.getEso().flipShowSavedScoring();
		} else if (actionCommand.equals("restoreSaved")) {
			AmsLabel l = tab.getImpDrawer().getSelPart().getLabel();
			if (l != null) {
				int n = l.getNumberOfSavedICGPoints();
				if (n > 0) {
					String[] test = new String[n];
					for (int i = 0; i < n; i++) {
						test[i] = l.getSavedIGGPoint(i).getName();
					}
					String ret = (String) JOptionPane.showInputDialog(MainFrame.getInstance().getMainFrame(),
							"Restore markers to saved positions from:",
							"Resore saved scoring", JOptionPane.PLAIN_MESSAGE, null, test, test[0]);
					if (ret != null) {
						tab.getImpDrawer().restoreMarkers(ret);
					}
				}
			}
		} else if (actionCommand.equals("loop")) {
			tab.getImpDrawer().flipLooping();
			tab.getECGDrawer().flipLooping();
			if (ImpTab.getInstance().getDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getDrawers())
					prt.flipLooping();
			tab.getImpRawDrawer().flipLooping();
			tab.getECGRawDrawer().flipLooping();
			if (ImpTab.getInstance().getFiltDrawers() != null)
				for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
					prt.flipLooping();
		} else if (actionCommand.equals("namepoints")) {
			tab.getEso().showorhidenames();
			tab.getIso().showorhidenames();
		} else if (actionCommand.equals("defaultscoring")) {
			int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"Do you want to apply Algorithm Scoring for all the ECG & ICG Complexes?",
					"Apply Algorithm Scoring", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				labels = CurrentOpenData.getInstance().getLabels().getLabels();
				for (AmsLabel l : labels) {
					l.resettoAlgo();
				}
				tab.getECGDrawer().getSignalPartSet().recalculatePoints();
				tab.getImpDrawer().getSignalPartSet().recalculatePoints();
			} else {
				return;
			}
		} else if (actionCommand.equals("autoscaleAll")) {
			tab.tabautoscale();
		} else if (actionCommand.equals("defaultscaleAll")) {
			tab.defaultscale();
		} else if (actionCommand.equals("autoscaleAllRaw")) {
			tab.tabautoscaleRaw();
		} else if (actionCommand.equals("nextPan")) {
			tab.nextPanel();
		} else if (actionCommand.equals("prevPan")) {
			tab.prevPanel();
		}

		tab.getIip().updateLabelTexts();
		tab.repaint();
	}

	@Override
	public void addButtons() {
		setupButton("first", "First Average Complex", "moveAvFirst", KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
		setupButton("left", "Previous Average Complex", "moveAvLeft",
				KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("right", "Next Average Complex", "moveAvRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
		setupButton("last", "Last Average Complex", "moveAvLast", KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, true));
		setupButton("redball", "Show/Hide Raw Average Complex", "showHideRaw",
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0, true));
		addNewSeparator();
		setupButton("first", "First Complex", "moveFirst", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true));
		setupButton("left", "Previous Complex", "moveLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true));
		setupButton("right", "Next Complex", "moveRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true));
		setupButton("last", "Last Complex", "moveLast", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true));
		setupButton("greyball", "Show/Hide Individual Complexes", "showHideIndiv",
				KeyStroke.getKeyStroke(KeyEvent.VK_X, 0, true));
		setupButton("greyball_play", "Start/stop looping individual complexes", "loop",
				KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true));
		// setupButton("greyball_stop", "Show/Hide Individual Complexes",
		// "showHideIndiv", KeyStroke.getKeyStroke(KeyEvent.VK_X, 0, true));
		addNewSeparator();
		setupButton("autoscale", "Autoscale All on Raw Data", "autoscaleAllRaw",
				KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		setupButton("yin", "Default Scale", "defaultscaleAll", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, true));
		setupButton("autoscale_green", "Autoscale All", "autoscaleAll",
				KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		addNewSeparator();
		setupButton("import", "Save current scoring", "saveScore",
				KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("smallstar", "Show/Hide saved scorings", "showSaved",
				KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true));
		setupButton("restore", "Restore saved scoring", "restoreSaved",
				KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("info", "Name Points", "namepoints",
				KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("rechecksusp", "Apply Algorithm Scoring for all the complexes", "defaultscoring",
				KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("first", "Previous Panel", "prevPan", KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("last", "Next Panel", "nextPan", KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));

	}

}
