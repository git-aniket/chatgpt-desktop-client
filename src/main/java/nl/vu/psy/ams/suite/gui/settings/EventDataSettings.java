package nl.vu.psy.ams.suite.gui.settings;


import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.tabs.scl.SCLTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class EventDataSettings extends SettingsPane {

	/**
	 * The settings for labeling data
	 */
	private static final long	serialVersionUID	= 1L;
	private JComboBox<?>			eventbased;
	private int					currentSelection;
	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();		
		//------------------------ For Event Based & Label Based Selection-----------------------------
		if (set.getIntPropertyOrToBeSaved(Settings.EVENTBASED) == 1) {
			eventbased.setSelectedIndex(0);
		} else {
			eventbased.setSelectedIndex(1);
		}
		currentSelection = eventbased.getSelectedIndex();
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();
		boolean isOpen = CurrentOpenData.getInstance().isOpen();		
		boolean sclAbsent = CurrentOpenData.getInstance().channelExists("SCL") == false;
		//------------------------ For Event Based & Label Based Selection-----------------------------
		
		if(eventbased.getSelectedIndex() != currentSelection){
			if (eventbased.getSelectedItem().equals("Event based Design (SCR) ")) {		
				set.setIntProperty(Settings.EVENTBASED, 1);
				set.setIntProperty(Settings.LABELBASED, 0);			
				if (isOpen && sclAbsent == false) {
					SCLTab.getInstance().erase();
				}
			
			} else {
				set.setIntProperty(Settings.EVENTBASED, 0);
				set.setIntProperty(Settings.LABELBASED, 1);
				if (isOpen && sclAbsent == false) {
					SCLTab.getInstance().erase();	
				}
			}
		}
	}

	@Override
	public void setupPanel() {
		
		pan.add(new JLabel("<html><h1> SCL Analyze Options</h1></html>"));
		pan.add(new JSeparator());
		//------------------------- When both label based & event based SCR are used-----------------------------
		String[] analyzeOptions = {"Event based Design (SCR) ", "Label based Design (SCL) "} ;
		//-------------------------------------------------------------------------------------------------------
		//String[] analyzeOptions = {"Label based Skin Conductance Response"} ;
		eventbased = new JComboBox<Object>(analyzeOptions);
		eventbased.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Analyze Options"));
		tmp.add(eventbased);
		pan.add(tmp);
	}

}
