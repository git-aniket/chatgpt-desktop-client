package nl.vu.psy.ams.suite.gui.tabs.scl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.EventRelatedSCLCycle;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

public class EventSCLInfoPanel extends JPanel {

	/**
	 * Panel showing SCL information about the selected event.
	 */
	private static final long serialVersionUID = 1L;
	private JPanel entirePanel;
	private JPanel labelPanel;
	private JPanel calculationPanel;

	private JLabel labelInfoL, labelInfoR;
	private JLabel calculationLabelL, calculationLabelR;
	private NumberFormat nf;
	final int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
	private EventRelatedSCLCycle checkevent;
	private int selEvent = 0;

	public EventSCLInfoPanel() {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);

		entirePanel = new JPanel();
		entirePanel.setLayout(new GridLayout(1, 3));

		// ---------------------Event Panel---------------------------------

		labelPanel = new JPanel(new GridLayout(1, 2));
		labelPanel.setBackground(Color.LIGHT_GRAY);
		labelPanel.setBorder(BorderFactory.createTitledBorder("Event Information"));

		labelInfoL = new JLabel();
		String lblText = "";
		lblText = "<html><body>No:<br>Event Time:<br>Event Code:<br>Heart Rate:<br>SCL_EventTime [\u00B5S]:<br>Baseline_SCL [\u00B5S]:</body></html>";
		labelInfoL.setText(lblText);

		labelInfoR = new JLabel();
		labelPanel.add(labelInfoL, BorderLayout.WEST);
		labelPanel.add(labelInfoR, BorderLayout.EAST);

		entirePanel.add(labelPanel);
		// -------------------------------------------------------------------

		// ---------------------Calculation Panel---------------------------------

		calculationPanel = new JPanel(new GridLayout(1, 2));
		calculationPanel.setBackground(Color.LIGHT_GRAY);
		calculationPanel.setBorder(BorderFactory.createTitledBorder("Event based Skin Conductance Response"));

		calculationLabelL = new JLabel();
		lblText = "<html><body>SCR:<br>Onset_Time:<br>SCL_Onset [\u00B5S]:<br>Latency [sec]:<br>Peak_Time:<br>SCL_Peak [\u00B5S]:<br>Rise_Time [sec]:<br>Rise_SCL [\u00B5S]:</body></html>";
		calculationLabelL.setText(lblText);

		calculationLabelR = new JLabel();
		calculationPanel.add(calculationLabelL, BorderLayout.WEST);
		calculationPanel.add(calculationLabelR, BorderLayout.EAST);

		entirePanel.add(calculationPanel);
		// -------------------------------------------------------------------
		add(entirePanel);

	}

	public void updateLabelTexts(EventRelatedSCLCycle event, boolean scale) {
		setCurrentEvent(event);
		if (scale) {
			SCLTab.getInstance().getXAxis().setTimes((event.getTime()) - 10000000, (event.getTime()) + 10000000);
			SCLTab.getInstance().autoscale();
		}
		EventRelatedSCLSet sset = CurrentOpenData.getInstance().getEventSCLSet();

		String lblText;

		lblText = "<html><body>";
		lblText += ((getSelEvent() + 1) + " of " + sset.getSCLCycles().size()) + "<br>";
		lblText += Utils.getDateAndTimeFromUSWithMS(event.getTime()) + "<br>";
		lblText += event.getEventCode() + "<br>";
		lblText += sset.getCycleClosestToTime(event.getTime()).getHeartRate() + "<br>";
		lblText += nf.format(sset.getCycleClosestToTime(event.getTime()).getSCL_Stimulus()) + "<br>";
		lblText += nf.format(sset.getCycleClosestToTime(event.getTime()).getBaselineSCL()) + "<br>";
		lblText += "</body></html>";

		labelInfoR.setText(lblText);

		lblText = "<html><body>";
		lblText += (sset.getCycleClosestToTime(event.getTime()).isSCRPresent()) + "<br>";
		if (sset.getCycleClosestToTime(event.getTime()).isSCRPresent()) {
			lblText += Utils.getDateAndTimeFromUSWithMS((sset.getCycleClosestToTime(event.getTime()).getOnset_Time()))
					+ "<br>";
			lblText += nf.format(sset.getCycleClosestToTime(event.getTime()).getOnset_SCL()) + "<br>";
			lblText += nf.format(((sset.getCycleClosestToTime(event.getTime()).getOnset_Time())
					- (sset.getCycleClosestToTime(event.getTime()).getTime())) / 1000000) + "<br>";
			lblText += Utils.getDateAndTimeFromUSWithMS((sset.getCycleClosestToTime(event.getTime()).getPeakTime()))
					+ "<br>";
			lblText += nf.format(sset.getCycleClosestToTime(event.getTime()).getPeakSCL()) + "<br>";
			lblText += nf.format(((sset.getCycleClosestToTime(event.getTime()).getPeakTime())
					- (sset.getCycleClosestToTime(event.getTime()).getOnset_Time())) / 1000000) + "<br>";
			lblText += (nf.format(sset.getCycleClosestToTime(event.getTime()).getPeakSCL()
					- (sset.getCycleClosestToTime(event.getTime()).getBaselineSCL()))) + "<br>";
		} else {
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
			lblText += -9999 + "<br>";
		}

		lblText += "</body></html>";
		calculationLabelR.setText(lblText);

	}

	public void setCurrentEvent(EventRelatedSCLCycle ev) {
		checkevent = ev;
	}

	public EventRelatedSCLCycle getCurrentEvent() {
		return checkevent;
	}

	public void setSelEvent(int ev) {
		selEvent = ev;
	}

	public int getSelEvent() {
		return selEvent;
	}
}
