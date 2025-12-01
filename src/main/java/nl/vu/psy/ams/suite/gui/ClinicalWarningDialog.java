package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ExitApp;
/*
 * Shows the clinical warning dialog
 */
public class ClinicalWarningDialog extends JDialog {

	private static final long	serialVersionUID	= 1L;

	public ClinicalWarningDialog(JFrame frame) {
		super(frame, "AMS intended use", true);
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		String message = "<html><h3>This product is designed for Scientific Research applications only.<br>It is NOT intended as a Medical Device for diagnosis of disease.</h3></html>";
		JLabel lab = new JLabel(message);
		Icon icon = UIManager.getIcon("OptionPane.warningIcon");
		JPanel pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel(icon), BorderLayout.WEST);
		pan2.add(lab, BorderLayout.CENTER);
		pan2.add(new JLabel(icon), BorderLayout.EAST);
		pan2.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan.add(pan2);

		final JCheckBox doNotShowAgain = new JCheckBox("Do not show this dialog again");

		final JButton continueBut = new JButton("Continue");
		continueBut.setEnabled(false);
		continueBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (doNotShowAgain.isSelected()) {
					AppSettings.getInstance().setIntProperty(Settings.SHOWCLINICALWARNING, 0);
				}
				ClinicalWarningDialog.this.setVisible(false);
			}
		});
		final JButton exitBut = new JButton("Exit");
		exitBut.setEnabled(false);
		exitBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExitApp.NormalExit();
			}
		});

		JRadioButton agree = new JRadioButton("I Agree");
		agree.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				continueBut.setEnabled(true);
				exitBut.setEnabled(false);
			}
		});
		JRadioButton notagree = new JRadioButton("I Do Not Agree");
		notagree.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				continueBut.setEnabled(false);
				exitBut.setEnabled(true);
			}
		});

		ButtonGroup group = new ButtonGroup();
		group.add(agree);
		group.add(notagree);
		pan2 = new JPanel();
		pan2.setLayout(new BoxLayout(pan2, BoxLayout.Y_AXIS));
		pan2.add(agree);
		pan2.add(notagree);
		pan2.setAlignmentX(Component.CENTER_ALIGNMENT);

		pan.add(pan2);

		pan2 = new JPanel();
		pan2.add(doNotShowAgain);
		pan2.add(continueBut);
		pan2.add(exitBut);
		pan2.setAlignmentX(Component.CENTER_ALIGNMENT);

		pan.add(pan2);

		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		add(pan);
		pack();
		setLocationRelativeTo(frame);
		setResizable(false);
		setVisible(true);
	}

}
