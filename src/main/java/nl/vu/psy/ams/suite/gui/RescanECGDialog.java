package nl.vu.psy.ams.suite.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Small dialog that lets you set the algorithm
 * parameters for rescanning a small ECG part.
 */
public class RescanECGDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSlider highThresholdSlider;
	private JSlider lowThresholdSlider;
	private JSlider ptpSlider;
	private JSlider upSlider;
	private JSlider downSlider;
	private BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
	private double lTime;
	private double rTime;

	public RescanECGDialog(double lTime, double rTime) {
		super(MainFrame.getInstance().getMainFrame(), true);
		this.setTitle("Rescan R-Peaks between " + Utils.getTimeFromUS(lTime) + " to " + Utils.getTimeFromUS(rTime));
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

		this.lTime = lTime;
		this.rTime = rTime;
		bSet = CurrentOpenData.getInstance().getBeatSet();
		JPanel thresholdSettings = new JPanel();
		thresholdSettings.setLayout(new BoxLayout(thresholdSettings, BoxLayout.Y_AXIS));
		thresholdSettings.setBorder(BorderFactory.createTitledBorder("Threshold Settings"));
		int defVal = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.HIGHTHRESHOLD), 0, 100);
		highThresholdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, defVal);
		highThresholdSlider.setMajorTickSpacing(25);
		highThresholdSlider.setMinorTickSpacing(5);
		highThresholdSlider.setPaintTicks(true);
		highThresholdSlider.setPaintLabels(true);
		defVal = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.LOWTHRESHOLD), 0, 100);
		lowThresholdSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, defVal);
		lowThresholdSlider.setMajorTickSpacing(25);
		lowThresholdSlider.setMinorTickSpacing(5);
		lowThresholdSlider.setPaintTicks(true);
		lowThresholdSlider.setPaintLabels(true);
		JLabel highThresholdLabel = new JLabel("High Threshold:", SwingConstants.CENTER);
		highThresholdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel lowThresholdLabel = new JLabel("Low Threshold:", SwingConstants.CENTER);
		lowThresholdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		thresholdSettings.add(highThresholdLabel);
		thresholdSettings.add(highThresholdSlider);
		thresholdSettings.add(lowThresholdLabel);
		thresholdSettings.add(lowThresholdSlider);

		JPanel weightSettings = new JPanel();
		weightSettings.setLayout(new BoxLayout(weightSettings, BoxLayout.Y_AXIS));
		weightSettings.setBorder(BorderFactory.createTitledBorder("Relative Weight Settings"));
		defVal = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.PTPW), 0, 100);
		ptpSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, defVal);
		ptpSlider.setMajorTickSpacing(25);
		ptpSlider.setMinorTickSpacing(5);
		ptpSlider.setPaintTicks(true);
		ptpSlider.setPaintLabels(true);
		defVal = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.UPW), 0, 100);
		upSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, defVal);
		upSlider.setMajorTickSpacing(25);
		upSlider.setMinorTickSpacing(5);
		upSlider.setPaintTicks(true);
		upSlider.setPaintLabels(true);
		defVal = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.DOWNW), 0, 100);
		downSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, defVal);
		downSlider.setMajorTickSpacing(25);
		downSlider.setMinorTickSpacing(5);
		downSlider.setPaintTicks(true);
		downSlider.setPaintLabels(true);
		JLabel ptpLabel = new JLabel("Peak-To-Peak Weight:", SwingConstants.CENTER);
		ptpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel upLabel = new JLabel("Upward Slope Weight:", SwingConstants.CENTER);
		upLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel downLabel = new JLabel("Downward Slope Weight:", SwingConstants.CENTER);
		downLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		weightSettings.add(ptpLabel);
		weightSettings.add(ptpSlider);
		weightSettings.add(upLabel);
		weightSettings.add(upSlider);
		weightSettings.add(downLabel);
		weightSettings.add(downSlider);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		JButton okButton = new JButton("Rescan");
		okButton.addActionListener(this);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());

		FontUIResource font = (FontUIResource) UIManager.get("InternalFrame.titleFont");
		FontMetrics metrics = this.getFontMetrics(font);
		Rectangle2D bounds = metrics.getStringBounds(getTitle(), getGraphics());
		int neededWidth = (int) (bounds.getWidth() + 50);
		this.setMinimumSize(new Dimension(neededWidth, 1));
		this.add(thresholdSettings);
		this.add(weightSettings);
		this.add(buttonPanel);
		this.pack();
		this.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("Rescan")) {
			CurrentOpenData cod = CurrentOpenData.getInstance();
			bSet.rescan(lTime, rTime, highThresholdSlider.getValue() / 100., lowThresholdSlider.getValue() / 100.,
					ptpSlider.getValue() / 100.,
					upSlider.getValue() / 100., downSlider.getValue() / 100., cod.getECGFile());
			CurrentOpenData.getInstance().getECGArtefacts().reCalculateFromPeaks();
			CurrentOpenData.getInstance().setDirty(true);
			this.setVisible(false);
		} else if (arg0.getActionCommand().equals("Cancel")) {
			this.setVisible(false);
		}
	}

}
