package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class GenGraphSettings extends SettingsPane {

	/**
	 * The settings for generated bar and HR+MOT graphs
	 */
	private static final long	serialVersionUID	= 1L;
	private JTextField			graphTitle;
	private JTextField			xTitle;
	private JTextField			yTitle;
	private JTextField			motgraphTitle;
	private JTextField			motxTitle;
	private JTextField			motyTitle;
	private JTextField			hrayTitle;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();
		String val;
		val = set.getPropertyOrToBeSaved(Settings.BARGRAPHTITLE);
		graphTitle.setText(val);
		val = set.getPropertyOrToBeSaved(Settings.BARGRAPHXTITLE);
		xTitle.setText(val);
		val = set.getPropertyOrToBeSaved(Settings.BARGRAPHYTITLE);
		yTitle.setText(val);

		val = set.getPropertyOrToBeSaved(Settings.GENGRAPHTITLE);
		motgraphTitle.setText(val);
		val = set.getPropertyOrToBeSaved(Settings.GENGRAPHXAXIS);
		motxTitle.setText(val);
		val = set.getPropertyOrToBeSaved(Settings.GENGRAPHMOTYAXIS);
		motyTitle.setText(val);
		val = set.getPropertyOrToBeSaved(Settings.GENGRAPHHRYAXIS);
		hrayTitle.setText(val);
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();
		set.setProperty(Settings.BARGRAPHTITLE, graphTitle.getText());
		set.setProperty(Settings.BARGRAPHXTITLE, xTitle.getText());
		set.setProperty(Settings.BARGRAPHYTITLE, yTitle.getText());

		set.setProperty(Settings.GENGRAPHTITLE, motgraphTitle.getText());
		set.setProperty(Settings.GENGRAPHXAXIS, motxTitle.getText());
		set.setProperty(Settings.GENGRAPHMOTYAXIS, motyTitle.getText());
		set.setProperty(Settings.GENGRAPHHRYAXIS, hrayTitle.getText());
	}

	@Override
	public void setupPanel() {
		pan.add(new JLabel("<html><h1>Graph Generation</h1></html>"));
		pan.add(new JSeparator());
		JPanel tmp;
		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Bar Graph titles"));
		JLabel lab;
		lab = new JLabel("Default graph title:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		graphTitle = new JTextField();
		JPanel tmp2 = new JPanel(new BorderLayout());
		tmp2.add(graphTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);
		lab = new JLabel("Default X Axis title:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		xTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(xTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);
		lab = new JLabel("Default Y Axis title:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		yTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(yTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);

		pan.add(tmp);

		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("HRA+MOT Graph titles"));
		lab = new JLabel("Default graph title (leave empty to use subject ID):");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		motgraphTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(motgraphTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);
		lab = new JLabel("Default X Axis title (leave empty to use default title):");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		motxTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(motxTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);
		lab = new JLabel("Default Motiliy Y Axis title:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		motyTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(motyTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);
		lab = new JLabel("Default HRA Y Axis title:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		hrayTitle = new JTextField();
		tmp2 = new JPanel(new BorderLayout());
		tmp2.add(hrayTitle, BorderLayout.CENTER);
		tmp2.add(lab, BorderLayout.WEST);
		tmp.add(tmp2);

		pan.add(tmp);
	}

}
