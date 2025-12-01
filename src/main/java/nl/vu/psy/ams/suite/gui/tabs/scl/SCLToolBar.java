package nl.vu.psy.ams.suite.gui.tabs.scl;


//import java.awt.GridLayout;
import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

//import javax.swing.BoxLayout;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JDialog;
//import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JTextField;
import javax.swing.KeyStroke;
//import javax.swing.filechooser.FileFilter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.SCLArtefactSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
//import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
//import nl.vu.psy.ams.suite.tools.Utils;

public class SCLToolBar extends AmsToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4775816283055757380L;
	private AmsLabelSet 					AmsLabelSet    = CurrentOpenData.getInstance().getLabels();
	private EventRelatedSCLSet			eventSCLSet = CurrentOpenData.getInstance().getEventSCLSet();
	private SCLArtefactSet				aSet		= CurrentOpenData.getInstance().getSCLArtefacts();
	/**
	 * 
	 */
	
	private SCLTab			tab;
	
	public SCLToolBar(SCLTab tab) {
		super();
		this.tab = tab;		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		
		if (actionCommand.equals("moveAvLeft")) {	
			if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED)==1){
				if(tab.getEventInfoPanel().getCurrentEvent().getTime() != eventSCLSet.getSCLCycles().first().getTime()){
					tab.getSelOverlay().setSelTime(eventSCLSet.getSCLCycleBeforeTime(tab.getEventInfoPanel().getCurrentEvent().getTime()).getTime());
					if(tab.getEventInfoPanel().getSelEvent() > 0){
						tab.getEventInfoPanel().setSelEvent(tab.getEventInfoPanel().getSelEvent()-1);
					}					
					tab.getEventInfoPanel().updateLabelTexts(eventSCLSet.getSCLCycleBeforeTime((tab.getEventInfoPanel().getCurrentEvent().getTime())),true);				
				}
			}else{
				if(tab.getInfoPanel().getCurrentLabel().getLeftTime()!= CurrentOpenData.getInstance().getLabels().getLabels().first().getLeftTime()){
					tab.getInfoPanel().updateLabelTexts(AmsLabelSet.getLabelBeforeTime(tab.getInfoPanel().getCurrentLabel().getLeftTime()));
				}
			
			}
		} else if (actionCommand.equals("moveAvRight")) {		
			if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1){
				if(tab.getEventInfoPanel().getCurrentEvent().getTime() != eventSCLSet.getSCLCycles().last().getTime()){
					tab.getSelOverlay().setSelTime(eventSCLSet.getSCLCycleAfterTime(tab.getEventInfoPanel().getCurrentEvent().getTime()).getTime());
					if(tab.getEventInfoPanel().getSelEvent() < eventSCLSet.getSCLCycles().size()){
						tab.getEventInfoPanel().setSelEvent(tab.getEventInfoPanel().getSelEvent()+1);
					}
					tab.getEventInfoPanel().updateLabelTexts(eventSCLSet.getSCLCycleAfterTime((tab.getEventInfoPanel().getCurrentEvent().getTime())),true);					
				}
			}else{		
				if(tab.getInfoPanel().getCurrentLabel().getRightTime()!= CurrentOpenData.getInstance().getLabels().getLabels().last().getRightTime()){
						tab.getInfoPanel().updateLabelTexts(AmsLabelSet.getLabelAfterTime(tab.getInfoPanel().getCurrentLabel().getRightTime()));
				}
			}			
		} else if (actionCommand.equals("moveAvFirst")) {
			if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1){				
				tab.getEventInfoPanel().setSelEvent(0);				
				tab.getEventInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getEventSCLSet().getSCLCycles().first(),true);
				tab.getSelOverlay().setSelTime(eventSCLSet.getSCLCycles().first().getTime());
			}else{
				tab.getInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getLabels().getLabels().first());
			}
		} else if (actionCommand.equals("moveAvLast")) {			
			if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1){
				tab.getEventInfoPanel().setSelEvent(eventSCLSet.getSCLCycles().size()-1);
				tab.getEventInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getEventSCLSet().getSCLCycles().last(),true);
				tab.getSelOverlay().setSelTime(eventSCLSet.getSCLCycles().last().getTime());
			}else{
				tab.getInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getLabels().getLabels().last());
			}
		} else if (actionCommand.equals("moveLeft")) {
			tab.getXAxis().move(-0.25);
		} else if (actionCommand.equals("moveRight")) {
			tab.getXAxis().move(0.25);
		} else if (actionCommand.equals("zoomIn")) {
			tab.getXAxis().zoom(2);
		} else if (actionCommand.equals("zoomOut")) {
			tab.getXAxis().zoom(0.5);
		} else if (actionCommand.equals("autoscaleAll")) {
			tab.getXAxis().autoscaleConnectedGraphs();
		} else if (actionCommand.equals("autoscaleAllRaw")) {
			tab.getXAxis().autoscaleConnectedGraphs(0);
		} else if (actionCommand.equals("moveLeftOneScreen")) {
			tab.getXAxis().move(-1);
		} else if (actionCommand.equals("moveRightOneScreen")) {
			tab.getXAxis().move(1);
		} else if (actionCommand.equals("zoomIn4X")) {
			tab.getXAxis().zoom(4);
		} else if (actionCommand.equals("zoomOut4X")) {
			tab.getXAxis().zoom(0.25);
		} else if (actionCommand.equals("delCyclesUnderArt")){
			aSet.deleteSCLCyclesUnderArtefacts();
			tab.getXAxis().updateAll();
			if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED)==1)
				tab.getEventInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getEventSCLSet().getSCLCycles().first(),true);	
			else
				tab.getInfoPanel().updateLabelTexts(CurrentOpenData.getInstance().getLabels().getLabels().first());
		} else if (actionCommand.equals("export")) {
			File fl 		= new File("");
			fl 				= SaveDialog.saveFileBrowserWithAdditionforSCL("scl", "_SCR", "SCL File");
			boolean state 	= SaveDialog.showEvents;
			if(state == true){ // Selected
				state = false;
			}else{
				state = true;
			}
			if (fl != null) {
				if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED)==1){
					CurrentOpenData.getInstance().getEventSCLSet().exportToASCII(fl,state);
				}else{
					CurrentOpenData.getInstance().getSCLSet().exportToASCII(fl,state);
				}
			}
		}else if(actionCommand.equals("analyze")){			
			int retVal = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"Are you sure you want recalculate SCL Cycles for the entire registration? This will remove all manual marked artefacts and changes!", "Confirm SCLCycle recalculation",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (retVal == JOptionPane.YES_OPTION) {
				aSet.clear();
				aSet.reCalculate();
				tab.recalculateSCL();		
			}
		} else if (actionCommand.equals("nextPan")) {
			tab.nextPanel();
		} else if (actionCommand.equals("prevPan")) {
			tab.prevPanel();
		}
		
		tab.repaint();
	}
	@Override
	public void addButtons() {
		if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 0){
			setupButton("first", "First Label", "moveAvFirst", KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
			setupButton("left", "Previous Label", "moveAvLeft", KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
			setupButton("right", "Next Label", "moveAvRight", KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
			setupButton("last", "Last Label", "moveAvLast", KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, true));
		}else{
			setupButton("first", "First Event", "moveAvFirst", KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
			setupButton("left", "Previous Event", "moveAvLeft", KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
			setupButton("right", "Next Event", "moveAvRight", KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
			setupButton("last", "Last Event", "moveAvLast", KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, true));
		}
		
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
		setupButton("autoscale_green", "Autoscale All", "autoscaleAll", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		setupButton("autoscale", "Autoscale All on Raw Data", "autoscaleAllRaw", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
		
		
		//if(AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1){						
			setupButton("delete", "Delete SCL Cycles Under Artefacts", "delCyclesUnderArt", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, true));
			setupButton("mrbomb", "ReCalculate SCL Cycles", "analyze",KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, true) );
		//}
		addNewSeparator();
		setupButton("export", "Export SCLData To Text File", "export", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("first", "Previous Panel", "prevPan", KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("last", "Next Panel", "nextPan", KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
	}
}
