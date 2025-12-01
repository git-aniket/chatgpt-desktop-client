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

import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

public class SCLInfoPanel extends JPanel {

	/**
	 * Panel showing SCL information about the selected label.
	 */
	private static final long serialVersionUID = 1L;
	private JPanel entirePanel;
	private JPanel labelPanel;
	private JPanel calculationPanel;
	private JLabel labelInfoL, labelInfoR;
	private JLabel calculationLabelL, calculationLabelR;
	private AmsLabel lab;
	private NumberFormat nf;
	final int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);

	public SCLInfoPanel() {

		super();

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);

		entirePanel = new JPanel();
		entirePanel.setLayout(new GridLayout(1, 2));

		// ---------------------Label Panel---------------------------------

		labelPanel = new JPanel(new GridLayout(1, 2));
		labelPanel.setBackground(Color.LIGHT_GRAY);
		labelPanel.setBorder(BorderFactory.createTitledBorder("Label Information"));

		labelInfoL = new JLabel();
		String lblText = "";
		lblText = "<html><body>Label No:<br>Start Time:<br>End Time:<br>Duration of the label [sec]:<br>Average Heart Rate:</body></html>";
		labelInfoL.setText(lblText);

		labelInfoR = new JLabel();
		labelPanel.add(labelInfoL, BorderLayout.WEST);
		labelPanel.add(labelInfoR, BorderLayout.EAST);

		entirePanel.add(labelPanel);
		// -------------------------------------------------------------------

		// ---------------------SCL Values Panel---------------------------------

		calculationPanel = new JPanel(new GridLayout(1, 2));
		calculationPanel.setBackground(Color.LIGHT_GRAY);
		calculationPanel.setBorder(BorderFactory.createTitledBorder("Skin Conductance Levels"));

		calculationLabelL = new JLabel();
		lblText = "<html><body>SCL_Stimulus [\u00B5S]:<br>Average_SCL [\u00B5S]:<br>Minimum_SCL [\u00B5S]:<br>Maximum_SCL [\u00B5S]:<br>nsSCR [count]:<br>nsSCRs per minute [ppm]</body></html>";
		calculationLabelL.setText(lblText);

		calculationLabelR = new JLabel();
		calculationPanel.add(calculationLabelL, BorderLayout.WEST);
		calculationPanel.add(calculationLabelR, BorderLayout.EAST);

		entirePanel.add(calculationPanel);
		// -------------------------------------------------------------------

		add(entirePanel);

	}

	public synchronized void updateLabelTexts(AmsLabel l) {

		SCLTab.getInstance().getXAxis().setTimes(l.getLeftTime() - 10000000, l.getRightTime() + 10000000);
		SCLTab.getInstance().getXAxis().updateAll();
		SCLTab.getInstance().autoscale();
		setCurrentLabel(l);
		String lblText;

		AmsLabel lSet = l;

		double durationoflabel = (lSet.getRightTime() - lSet.getLeftTime()) / 1000000.;
		double sclvalueatstimulustime = lSet.getSCLvalueatTime();

		lblText = "<html><body>";
		lblText += lSet.getLabelNo() + "<br>";
		lblText += Utils.getDateAndTimeFromUS(lSet.getLeftTime()) + "<br>";
		lblText += Utils.getDateAndTimeFromUS(lSet.getRightTime()) + "<br>";
		lblText += durationoflabel + "<br>";
		lblText += lSet.getAverage(true) + "<br>";
		lblText += "</body></html>";

		labelInfoR.setText(lblText);

		lblText = "<html><body>";
		lblText += nf.format(sclvalueatstimulustime) + "<br>";
		lblText += nf.format(lSet.getAvSCL()) + "<br>";
		lblText += nf.format(l.getMinMaxSCL()[0]) + "<br>";
		lblText += nf.format(l.getMinMaxSCL()[1]) + "<br>";
		lblText += nf.format(l.getSCLCycleCount()) + "<br>";
		lblText += nf.format(l.getSCLCycleCount() * 60. / durationoflabel) + "<br>";
		lblText += "</body></html>";

		calculationLabelR.setText(lblText);

	}

	public void setCurrentLabel(AmsLabel l) {
		this.lab = l;
	}

	public AmsLabel getCurrentLabel() {
		return lab;
	}

}
