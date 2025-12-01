package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
// import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
// import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class ExpertModeSettings extends SettingsPane {

	/**
	 * The settings for Expert Mode
	 */
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("rawtypes")
	private JComboBox filteringoptionD, outlierBox; // filteringoption, filteringoptionECG,
	private int currentSelectionD, currentSelectionOutlier; // currentSelection, currentSelectionECG,
	private JCheckBox removeICGbeats, showEOFEvents;

	private JFormattedTextField hrlb;

	private JFormattedTextField hrub;
	private JSlider lyingSlider, motSlider;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();

		// if (set.getIntPropertyOrToBeSaved(Settings.FILTERDZDTNew) == 1) {
		// filteringoption.setSelectedIndex(0);
		// } else {
		// filteringoption.setSelectedIndex(1);
		// }
		// currentSelection = filteringoption.getSelectedIndex();

		if (set.getIntPropertyOrToBeSaved(Settings.DEBUG) == 1) {
			filteringoptionD.setSelectedIndex(0);
		} else {
			filteringoptionD.setSelectedIndex(1);
		}
		currentSelectionD = filteringoptionD.getSelectedIndex();

		// if (set.getIntPropertyOrToBeSaved(Settings.FILTERECGNew) == 1) {
		// filteringoptionECG.setSelectedIndex(0);
		// } else {
		// filteringoptionECG.setSelectedIndex(1);
		// }
		// currentSelectionECG = filteringoptionECG.getSelectedIndex();

		int val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.REMOVEICGBEATS);
		if (val == 0) {
			removeICGbeats.setSelected(false);
		} else {
			removeICGbeats.setSelected(true);
		}

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.OUTLIERREMOVAL);
		outlierBox.setSelectedIndex(val);
		currentSelectionOutlier = outlierBox.getSelectedIndex();

		val = set.getIntPropertyOrToBeSaved(Settings.MINHR);
		hrlb.setValue(val);
		val = set.getIntPropertyOrToBeSaved(Settings.MAXHR);
		hrub.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.LYINGTHRESHOLD);
		lyingSlider.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.MOTTHRESHOLD);
		motSlider.setValue(val);

		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWEOFEVENTS);
		if (val == 0) {
			showEOFEvents.setSelected(false);
		} else {
			showEOFEvents.setSelected(true);
		}
	}

	@Override
	public void save() {

		AppSettings set = AppSettings.getInstance();
		boolean isOpen = CurrentOpenData.getInstance().isOpen();

		// if (filteringoption.getSelectedIndex() != currentSelection) {
		// int res = JOptionPane.YES_OPTION;
		// if (isOpen) {
		// res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
		// "Do you want to change filter setting for DZDT? If you continue the scoring
		// for all the ECG Complexes will be lost",
		// "Change filter setting ICG tab", JOptionPane.YES_NO_OPTION);
		// }
		// if (res == JOptionPane.YES_OPTION) {
		// if (filteringoptionECG.getSelectedItem().equals("Off")) {
		// set.setIntProperty(Settings.FILTERDZDTNew, 0);
		// } else {
		// set.setIntProperty(Settings.FILTERDZDTNew, 1);
		// }
		// if (isOpen) {
		// ImpTab.getInstance().getIso().setDirty(true);
		// ImpTab.getInstance().getImpDrawer().getSignalPartSet().recalculate();
		// ImpTab.getInstance().repaint(); // Only by switching tabs, new changes can be
		// seen
		// }
		// }
		// }
		if (filteringoptionD.getSelectedIndex() != currentSelectionD) {

			if (filteringoptionD.getSelectedItem().equals("Off")) {
				set.setIntProperty(Settings.DEBUG, 0);
				if (isOpen) {
					ImpTab.getInstance().repaint(); // Only by switching tabs, new changes can be seen
				}
			} else {
				set.setIntProperty(Settings.DEBUG, 1);
				if (isOpen) {
					ImpTab.getInstance().repaint(); // Only by switching tabs, new changes can be seen
				}
			}
		}
		// if (filteringoptionECG.getSelectedIndex() != currentSelectionECG) {
		// int res = JOptionPane.YES_OPTION;
		// if (isOpen) {
		// res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
		// "Do you want to change filter setting for ECG? If you continue the scoring
		// for all the ECG Complexes will be lost",
		// "Change filter setting ICG tab", JOptionPane.YES_NO_OPTION);
		// }
		// if (res == JOptionPane.YES_OPTION) {
		// if (filteringoptionECG.getSelectedItem().equals("Off")) {
		// set.setIntProperty(Settings.FILTERECGNew, 0);
		// } else {
		// set.setIntProperty(Settings.FILTERECGNew, 1);
		// }
		// if (isOpen) {
		// ImpTab.getInstance().getEso().setDirty(true);
		// ImpTab.getInstance().getECGDrawer().getSignalPartSet().recalculate();
		// ImpTab.getInstance().repaint(); // Only by switching tabs, new changes can be
		// seen
		// }
		// }
		// }

		if (removeICGbeats.isSelected()) {
			AppSettings.getInstance().setIntProperty(Settings.REMOVEICGBEATS, 1);
		} else {
			AppSettings.getInstance().setIntProperty(Settings.REMOVEICGBEATS, 0);
		}
		if (isOpen)
			ImpTab.getInstance().repaint(); // Only by switching tabs, new changes can be seen

		if (outlierBox.getSelectedIndex() != currentSelectionOutlier)
			set.setIntProperty(Settings.OUTLIERREMOVAL, outlierBox.getSelectedIndex());

		int val = (int) Math.round(((Number) hrlb.getValue()).doubleValue());
		if (val != set.getIntPropertyOrToBeSaved(Settings.MINHR)) {
			set.setIntProperty(Settings.MINHR, val);
		}

		val = (int) Math.round(((Number) hrub.getValue()).doubleValue());
		if (val != set.getIntPropertyOrToBeSaved(Settings.MAXHR)) {
			set.setIntProperty(Settings.MAXHR, val);
		}

		set.setIntProperty(Settings.LYINGTHRESHOLD, lyingSlider.getValue());
		set.setIntProperty(Settings.MOTTHRESHOLD, motSlider.getValue());

		if (showEOFEvents.isSelected()) {
			AppSettings.getInstance().setIntProperty(Settings.SHOWEOFEVENTS, 1);
		} else {
			AppSettings.getInstance().setIntProperty(Settings.SHOWEOFEVENTS, 0);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setupPanel() {

		pan.add(new JLabel("<html><h1> Expert Mode Options</h1></html>"));
		pan.add(new JSeparator());

		String[] analyzeOptions = { "On", "Off" };
		String[] outlierOptions = { "None", "3SD", "1 percent" };

		// filteringoption = new JComboBox(analyzeOptions);
		// filteringoption.setAlignmentX(Component.LEFT_ALIGNMENT);

		// JPanel tmp = new JPanel();
		// tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		// tmp.setBorder(BorderFactory.createTitledBorder("Filtering for DZDT Signal"));
		// tmp.add(filteringoption);

		// pan.add(tmp);

		// filteringoptionECG = new JComboBox(analyzeOptions);
		// filteringoptionECG.setAlignmentX(Component.LEFT_ALIGNMENT);

		// JPanel tmp3 = new JPanel();
		// tmp3.setLayout(new BoxLayout(tmp3, BoxLayout.Y_AXIS));
		// tmp3.setBorder(BorderFactory.createTitledBorder("Filtering for ECG Signal in
		// Imp Tab"));
		// tmp3.add(filteringoptionECG);

		// pan.add(tmp3);

		filteringoptionD = new JComboBox(analyzeOptions);
		filteringoptionD.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel tmp2 = new JPanel();
		tmp2.setLayout(new BoxLayout(tmp2, BoxLayout.Y_AXIS));
		tmp2.setBorder(BorderFactory.createTitledBorder("Debug and raw ECG/ICG per label output"));
		tmp2.add(filteringoptionD);

		removeICGbeats = new JCheckBox("In ICG tab filter out deviating beats");
		removeICGbeats.setAlignmentX(Component.LEFT_ALIGNMENT);
		pan.add(removeICGbeats);
		pan.add(tmp2);

		outlierBox = new JComboBox(outlierOptions);
		outlierBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel tmp4 = new JPanel();
		tmp4.setLayout(new BoxLayout(tmp4, BoxLayout.Y_AXIS));
		tmp4.setBorder(BorderFactory.createTitledBorder("Outlier removal method for autoscale graphs"));
		tmp4.add(outlierBox);
		pan.add(tmp4);

		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Heart rate band to not mark beats suspicious"));

		hrlb = new JFormattedTextField(NumberFormat.getInstance(Locale.US));
		hrub = new JFormattedTextField(NumberFormat.getInstance(Locale.US));

		JPanel tfPan;

		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("HR lower bound (bpm): "), BorderLayout.WEST);
		tfPan.add(hrlb, BorderLayout.CENTER);
		tmp.add(tfPan);
		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("HR upper bound (bpm): "), BorderLayout.WEST);
		tfPan.add(hrub, BorderLayout.CENTER);
		tmp.add(tfPan);

		pan.add(tmp);

		JPanel tmp5 = new JPanel();
		tmp5.setLayout(new BoxLayout(tmp5, BoxLayout.Y_AXIS));
		tmp5.setBorder(BorderFactory.createTitledBorder("Default motility settings"));
		JLabel lab, lab2;
		lab = new JLabel("Lying position threshold (mg):");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		lyingSlider = new JSlider(0, 1000);
		lyingSlider.setMajorTickSpacing(250);
		lyingSlider.setMinorTickSpacing(50);
		lyingSlider.setPaintTicks(true);
		lyingSlider.setPaintLabels(true);
		lyingSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp5.add(lab);
		tmp5.add(lyingSlider);
		lab2 = new JLabel("Motility artefact threshold (mg):");
		lab2.setAlignmentX(Component.LEFT_ALIGNMENT);
		motSlider = new JSlider(0, 100);
		motSlider.setMajorTickSpacing(25);
		motSlider.setMinorTickSpacing(5);
		motSlider.setPaintTicks(true);
		motSlider.setPaintLabels(true);
		motSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp5.add(lab2);
		tmp5.add(motSlider);
		pan.add(tmp5);

		JPanel tmp6 = new JPanel();
		tmp6.setLayout(new BoxLayout(tmp6, BoxLayout.Y_AXIS));
		tmp6.setBorder(BorderFactory.createTitledBorder("Misc. settings"));
		showEOFEvents = new JCheckBox("Show markers for file ends/starts");
		showEOFEvents.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp6.add(showEOFEvents);
		pan.add(tmp6);
	}
}
