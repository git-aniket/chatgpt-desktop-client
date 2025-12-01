package nl.vu.psy.ams.suite.gui.settings;

import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.tabs.info.LabelInformationTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class LabelInfoSettings extends SettingsPane {

	/**
	 * The settings for label information tab
	 */
	private static final long	serialVersionUID	= 1L;
	private JCheckBox			cb;
	private JFormattedTextField	ftf;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();
		int val;
		val = set.getIntPropertyOrToBeSaved(Settings.SHOWENTIREDATA);
		if (val == 1) {
			cb.setSelected(true);
		} else {
			cb.setSelected(false);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.MISSINGVALUE);
		ftf.setValue(val);
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();
		int val = set.getIntPropertyOrToBeSaved(Settings.SHOWENTIREDATA);
		if (cb.isSelected()) {
			if (val != 1) {
				set.setIntProperty(Settings.SHOWENTIREDATA, 1);
				if (CurrentOpenData.getInstance().isOpen()) {
					LabelInformationTab tab = LabelInformationTab.getInstance();
					if (tab.isActive()) {
						tab.setUnactive();
						tab.setActive();
					}
				}
			}
		} else {
			if (val == 1) {
				set.setIntProperty(Settings.SHOWENTIREDATA, 0);
				if (CurrentOpenData.getInstance().isOpen()) {
					LabelInformationTab tab = LabelInformationTab.getInstance();
					if (tab.isActive()) {
						tab.setUnactive();
						tab.setActive();
					}
				}
			}
		}

		val = set.getIntPropertyOrToBeSaved(Settings.MISSINGVALUE);
		try {
			ftf.commitEdit();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Long newLong = (Long) ftf.getValue();
		long newlong = newLong;
		int newVal = (int) newlong;
		if (newVal != val) {
			set.setIntProperty(Settings.MISSINGVALUE, newVal);
			if (CurrentOpenData.getInstance().isOpen()) {
				LabelInformationTab tab = LabelInformationTab.getInstance();
				if (tab.isActive()) {
					tab.setUnactive();
					tab.setActive();
				}
			}
		}
	}

	@Override
	public void setupPanel() {
		pan.add(new JLabel("<html><h1>Label Information Settings</h1></html>"));
		pan.add(new JSeparator());

		JPanel tmp;
		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));

		cb = new JCheckBox("Include label 0 for entire data recording");
		cb.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(cb);
		NumberFormat nff = NumberFormat.getInstance(Locale.US);
		nff.setMaximumFractionDigits(0);
		nff.setGroupingUsed(false);
		NumberFormatter nf = new NumberFormatter(nff);
		DefaultFormatterFactory dff = new DefaultFormatterFactory(nf);
		ftf = new JFormattedTextField(dff);
		ftf.setColumns(20);
		JPanel tmp2 = new JPanel();
		tmp2.setLayout(new BoxLayout(tmp2, BoxLayout.X_AXIS));
		tmp2.add(new JLabel("Missing value:"));
		tmp2.add(ftf);
		tmp2.add(Box.createHorizontalGlue());
		tmp2.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(tmp2);

		pan.add(tmp);
	}

}
