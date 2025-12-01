package nl.vu.psy.ams.suite.gui.settings;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.tabs.rsa.RSATab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class RSASettings extends SettingsPane {
	/**
	 * The settings for RSA calculation
	 */
	private static final long serialVersionUID = 1L;
	private JFormattedTextField relThresholdTF;
	private JFormattedTextField afterShortestTF;
	private JFormattedTextField afterLongestTF;
	private JCheckBox dzRangeCB;
	private JFormattedTextField minDZTF;
	private JFormattedTextField maxDZTF;
	private JCheckBox rrRateCB;
	private JFormattedTextField rrAllowedTF;
	// private JCheckBox ibiRateCB;
	// private JFormattedTextField ibiAllowedTF;
	private JCheckBox showToolTip;

	@Override
	public void initializeSettings() {
		int val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARELTHRESH);
		relThresholdTF.setValue(val);
		relThresholdTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERSHORTEST);
		afterShortestTF.setValue(val);
		afterShortestTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERLONGEST);
		afterLongestTF.setValue(val);
		afterLongestTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSADZRANGECHECK);
		if (val == 0) {
			dzRangeCB.setSelected(false);
		} else {
			dzRangeCB.setSelected(true);
		}

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMINDZ);
		minDZTF.setValue(val / 1000.);
		minDZTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMAXDZ);
		maxDZTF.setValue(val / 1000.);
		maxDZTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATECHECK);
		if (val == 0) {
			rrRateCB.setSelected(false);
		} else {
			rrRateCB.setSelected(true);
		}
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATEMAX);
		rrAllowedTF.setValue(val);
		rrAllowedTF.setColumns(4);

		// val =
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAIBICHECK);
		// if (val == 0) {
		// ibiRateCB.setSelected(false);
		// } else {
		// ibiRateCB.setSelected(true);
		// }
		// val =
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAIBIMAX);
		// ibiAllowedTF.setValue(val);
		// ibiAllowedTF.setColumns(4);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWRSATOOLTIP);
		if (val == 0) {
			showToolTip.setSelected(false);
		} else {
			showToolTip.setSelected(true);
		}
	}

	@Override
	public void save() {
		boolean isChanged = false;

		int oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARELTHRESH);
		try {
			relThresholdTF.commitEdit();
		} catch (ParseException e) {
		}
		int valRelTresh = (Integer) relThresholdTF.getValue();

		if (oldVal != valRelTresh)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERSHORTEST);
		try {
			afterShortestTF.commitEdit();
		} catch (ParseException e) {
		}
		int valRelAfterShotest = (int) (long) ((Long) afterShortestTF.getValue());

		if (oldVal != valRelAfterShotest)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERLONGEST);
		try {
			afterLongestTF.commitEdit();
		} catch (ParseException e) {
		}
		int valRelAfterLongest = (int) (long) ((Long) afterLongestTF.getValue());

		if (oldVal != valRelAfterLongest)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATECHECK);
		int valRRCheck = 0;
		if (rrRateCB.isSelected())
			valRRCheck = 1;
		if (oldVal != valRRCheck)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATEMAX);
		try {
			rrAllowedTF.commitEdit();
		} catch (ParseException e) {
		}
		int valRRMax = (int) (long) ((Long) rrAllowedTF.getValue());
		if (oldVal != valRRMax)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSADZRANGECHECK);
		int valRangeCheck = 0;
		if (dzRangeCB.isSelected())
			valRangeCheck = 1;
		if (oldVal != valRangeCheck)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMINDZ);
		try {
			minDZTF.commitEdit();
		} catch (ParseException e) {
		}
		int valDZMin = (int) Math.round(1000. * (Double) minDZTF.getValue());
		if (oldVal != valDZMin)
			isChanged = true;

		oldVal = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAMAXDZ);
		try {
			maxDZTF.commitEdit();
		} catch (ParseException e) {
		}
		int valDZMax = (int) Math.round(1000. * (Double) maxDZTF.getValue());
		if (oldVal != valDZMax)
			isChanged = true;

		// oldVal =
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAIBICHECK);
		// int valIBICheck = 0;
		// if (ibiRateCB.isSelected())
		// valIBICheck = 1;
		// if (oldVal != valIBICheck)
		// isChanged = true;

		// oldVal =
		// AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAIBIMAX);
		// try {
		// ibiAllowedTF.commitEdit();
		// } catch (ParseException e) {
		// }
		// int valIBIMax = (int) (long) ((Long) ibiAllowedTF.getValue());
		// if (oldVal != valIBIMax)
		// isChanged = true;

		if (showToolTip.isSelected()) {
			AppSettings.getInstance().setIntProperty(Settings.SHOWRSATOOLTIP, 1);
		} else {
			AppSettings.getInstance().setIntProperty(Settings.SHOWRSATOOLTIP, 0);
		}

		if (CurrentOpenData.getInstance().isOpen()) {
			if (isChanged) {
				int retVal = JOptionPane
						.showConfirmDialog(
								MainFrame.getInstance().getMainFrame(),
								"Because the settings are changed, all respiration scoring will be recalculated. Any manual changes will be lost.\nAre you sure you want to change the settings?",
								"Confirm recalculation", JOptionPane.YES_NO_OPTION);
				if (retVal == JOptionPane.YES_OPTION) {
					AppSettings.getInstance().setIntProperty(Settings.RSARELTHRESH, valRelTresh);
					AppSettings.getInstance().setIntProperty(Settings.RSAAFTERSHORTEST, valRelAfterShotest);
					AppSettings.getInstance().setIntProperty(Settings.RSAAFTERLONGEST, valRelAfterLongest);
					AppSettings.getInstance().setIntProperty(Settings.RSARRATECHECK, valRRCheck);
					AppSettings.getInstance().setIntProperty(Settings.RSARRATEMAX, valRRMax);
					AppSettings.getInstance().setIntProperty(Settings.RSADZRANGECHECK, valRangeCheck);
					AppSettings.getInstance().setIntProperty(Settings.RSAMINDZ, valDZMin);
					AppSettings.getInstance().setIntProperty(Settings.RSAMAXDZ, valDZMax);
					// AppSettings.getInstance().setIntProperty(Settings.RSAIBICHECK, valIBICheck);
					// AppSettings.getInstance().setIntProperty(Settings.RSAIBIMAX, valIBIMax);
					if (CurrentOpenData.getInstance().channelExists("DZ"))
						CurrentOpenData.getInstance().getRespSet().recalculate();
				}
			}
		} else {
			AppSettings.getInstance().setIntProperty(Settings.RSARELTHRESH, valRelTresh);
			AppSettings.getInstance().setIntProperty(Settings.RSAAFTERSHORTEST, valRelAfterShotest);
			AppSettings.getInstance().setIntProperty(Settings.RSAAFTERLONGEST, valRelAfterLongest);
			AppSettings.getInstance().setIntProperty(Settings.RSARRATECHECK, valRRCheck);
			AppSettings.getInstance().setIntProperty(Settings.RSARRATEMAX, valRRMax);
			AppSettings.getInstance().setIntProperty(Settings.RSADZRANGECHECK, valRangeCheck);
			AppSettings.getInstance().setIntProperty(Settings.RSAMINDZ, valDZMin);
			AppSettings.getInstance().setIntProperty(Settings.RSAMAXDZ, valDZMax);
			// AppSettings.getInstance().setIntProperty(Settings.RSAIBICHECK, valIBICheck);
			// AppSettings.getInstance().setIntProperty(Settings.RSAIBIMAX, valIBIMax);
		}

		if (CurrentOpenData.getInstance().isOpen() && CurrentOpenData.getInstance().channelExists("ECG")
				&& !CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()) {
			RSATab tab = RSATab.getInstance();
			if (tab.isActive()) {
				tab.setUnactive();
				tab.setActive();
			}
		}
	}

	@Override
	public void setupPanel() {
		pan.add(new JLabel("<html><h1>Respiration Scoring</h1></html>"));
		pan.add(new JSeparator());

		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));

		relThresholdTF = new JFormattedTextField(100);
		JPanel pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("Relative Threshold:"));
		pan2.add(relThresholdTF);
		pan2.add(new JLabel("%"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);

		tmp.add(new JSeparator(SwingConstants.HORIZONTAL));

		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("dZ-HR Phase shift for shortest IBI:"));
		NumberFormat nff = NumberFormat.getInstance(Locale.US);
		nff.setMaximumFractionDigits(0);
		nff.setGroupingUsed(false);
		NumberFormatter nf = new NumberFormatter(nff);
		DefaultFormatterFactory dff = new DefaultFormatterFactory(nf);
		afterShortestTF = new JFormattedTextField(dff);
		pan2.add(afterShortestTF);
		pan2.add(new JLabel("msec"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);

		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("dZ-HR Phase shift for longest IBI:"));
		afterLongestTF = new JFormattedTextField(dff);
		pan2.add(afterLongestTF);
		pan2.add(new JLabel("msec"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);

		tmp.add(new JSeparator(SwingConstants.HORIZONTAL));
		rrRateCB = new JCheckBox("Automatic respiration rate artefact detection");
		rrRateCB.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(rrRateCB);
		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("Maximum allowed deviation:"));
		rrAllowedTF = new JFormattedTextField(dff);
		pan2.add(rrAllowedTF);
		pan2.add(new JLabel("%"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);
		rrRateCB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rrAllowedTF.setEnabled(rrRateCB.isSelected());
			}
		});

		tmp.add(new JSeparator(SwingConstants.HORIZONTAL));
		dzRangeCB = new JCheckBox("Automatic impedance range check");
		dzRangeCB.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(dzRangeCB);

		minDZTF = new JFormattedTextField(
				new DefaultFormatterFactory(new NumberFormatter(NumberFormat.getInstance(Locale.US))));
		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("Minimum dZ:"));
		pan2.add(minDZTF);
		pan2.add(new JLabel("\u2126"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);

		maxDZTF = new JFormattedTextField(
				new DefaultFormatterFactory(new NumberFormatter(NumberFormat.getInstance(Locale.US))));
		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		pan2.add(new JLabel("Maximum dZ:"));
		pan2.add(maxDZTF);
		pan2.add(new JLabel("\u2126"));
		pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(pan2);

		dzRangeCB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				minDZTF.setEnabled(dzRangeCB.isSelected());
				maxDZTF.setEnabled(dzRangeCB.isSelected());
			}
		});

		// tmp.add(new JSeparator(SwingConstants.HORIZONTAL));
		// ibiRateCB = new JCheckBox("Automatic IBI artefact detection");
		// ibiRateCB.setAlignmentX(Component.LEFT_ALIGNMENT);
		// tmp.add(ibiRateCB);
		// pan2 = new JPanel();
		// pan2.setLayout(new BoxLayout(pan2, BoxLayout.X_AXIS));
		// pan2.add(new JLabel("Maximum allowed deviation:"));
		// ibiAllowedTF = new JFormattedTextField(dff);
		// pan2.add(ibiAllowedTF);
		// pan2.add(new JLabel("%"));
		// pan2.setAlignmentX(Component.LEFT_ALIGNMENT);
		// tmp.add(pan2);
		// ibiRateCB.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// ibiAllowedTF.setEnabled(ibiRateCB.isSelected());
		// }
		// });

		pan.add(tmp);

		tmp.add(new JSeparator(SwingConstants.HORIZONTAL));
		showToolTip = new JCheckBox("Show tooltip when hovering over: Irregular Respiration and Clipping DZ bars");
		showToolTip.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(showToolTip);

	}

}
