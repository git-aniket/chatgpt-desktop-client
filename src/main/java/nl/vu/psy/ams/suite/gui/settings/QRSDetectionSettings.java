package nl.vu.psy.ams.suite.gui.settings;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class QRSDetectionSettings extends SettingsPane {

	/**
	 * The settings for QRS detection
	 */
	private static final long	serialVersionUID	= 1L;
	private JSlider				highSlider;
	private JSlider				lowSlider;
	private JSlider				ptpSlider;
	private JSlider				usSlider;
	private JSlider				dsSlider;
	private JCheckBox			ShowMarkerInECGArtefactsBar;
	private JCheckBox			CreateMotArtefacts;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();

		int val = set.getIntPropertyOrToBeSaved(Settings.HIGHTHRESHOLD);
		highSlider.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.LOWTHRESHOLD);
		lowSlider.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.PTPW);
		ptpSlider.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.UPW);
		usSlider.setValue(val);

		val = set.getIntPropertyOrToBeSaved(Settings.DOWNW);
		dsSlider.setValue(val);
		
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWMARKERINECGARTEFACTSBAR);
		if (val == 0) {
			ShowMarkerInECGArtefactsBar.setSelected(false);
		} else {
			ShowMarkerInECGArtefactsBar.setSelected(true);
		}
		
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.MOTARTEFACTS);
		if (val == 0) {
			CreateMotArtefacts.setSelected(false);
		} else {
			CreateMotArtefacts.setSelected(true);
		}
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();

		set.setIntProperty(Settings.HIGHTHRESHOLD, highSlider.getValue());
		set.setIntProperty(Settings.LOWTHRESHOLD, lowSlider.getValue());
		set.setIntProperty(Settings.PTPW, ptpSlider.getValue());
		set.setIntProperty(Settings.UPW, usSlider.getValue());
		set.setIntProperty(Settings.DOWNW, dsSlider.getValue());
		
		if (ShowMarkerInECGArtefactsBar.isSelected()) {
			AppSettings.getInstance().setIntProperty(Settings.SHOWMARKERINECGARTEFACTSBAR, 1);
		} else {
			AppSettings.getInstance().setIntProperty(Settings.SHOWMARKERINECGARTEFACTSBAR, 0);
		}
		
		if (CreateMotArtefacts.isSelected()) {
			AppSettings.getInstance().setIntProperty(Settings.MOTARTEFACTS, 1);
		} else {
			AppSettings.getInstance().setIntProperty(Settings.MOTARTEFACTS, 0);
		}
	}

	@Override
	public void setupPanel() {
		pan.add(new JLabel("<html><h1>QRS Detection</h1></html>"));
		pan.add(new JSeparator());

		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Default algorithm settings"));
		JLabel lab;
		lab = new JLabel("High threshold:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		highSlider = new JSlider(0, 100);
		highSlider.setMajorTickSpacing(25);
		highSlider.setMinorTickSpacing(5);
		highSlider.setPaintTicks(true);
		highSlider.setPaintLabels(true);
		highSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(lab);
		tmp.add(highSlider);
		lab = new JLabel("Low threshold:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		lowSlider = new JSlider(0, 100);
		lowSlider.setMajorTickSpacing(25);
		lowSlider.setMinorTickSpacing(5);
		lowSlider.setPaintTicks(true);
		lowSlider.setPaintLabels(true);
		lowSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(lowSlider);
		lab = new JLabel("Peak-To-Peak weight:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		ptpSlider = new JSlider(0, 100);
		ptpSlider.setMajorTickSpacing(25);
		ptpSlider.setMinorTickSpacing(5);
		ptpSlider.setPaintTicks(true);
		ptpSlider.setPaintLabels(true);
		ptpSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(ptpSlider);
		lab = new JLabel("Upward slope weight:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		usSlider = new JSlider(0, 100);
		usSlider.setMajorTickSpacing(25);
		usSlider.setMinorTickSpacing(5);
		usSlider.setPaintTicks(true);
		usSlider.setPaintLabels(true);
		usSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(usSlider);
		lab = new JLabel("Downward slope weight:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		dsSlider = new JSlider(0, 100);
		dsSlider.setMajorTickSpacing(25);
		dsSlider.setMinorTickSpacing(5);
		dsSlider.setPaintTicks(true);
		dsSlider.setPaintLabels(true);
		dsSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(dsSlider);

		pan.add(tmp);
	
		JPanel tmp2 = new JPanel();
		tmp2.setLayout(new BoxLayout(tmp2, BoxLayout.Y_AXIS));
		tmp2.add(new JSeparator(SwingConstants.HORIZONTAL));
		ShowMarkerInECGArtefactsBar = new JCheckBox("Show markers in ECG Artefact bar on Detect R-Peaks tab");
		ShowMarkerInECGArtefactsBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp2.add(ShowMarkerInECGArtefactsBar);
		CreateMotArtefacts = new JCheckBox("Create ECG artefacts when too mucn activity is detected");
		CreateMotArtefacts.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp2.add(CreateMotArtefacts);
				
		pan.add(tmp2);
	}
}
