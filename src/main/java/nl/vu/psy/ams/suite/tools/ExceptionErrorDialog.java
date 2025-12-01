package nl.vu.psy.ams.suite.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
/*
 * Dialog that shows the error message of a java exception,
 * with the possibility to 'show more details' about the exception.
 * Used so the developer can get more useful information from the user
 * if an error occurs.
 */
public class ExceptionErrorDialog {

	private static void showDiag(final JDialog diag, Component parent, final String title, String message, final Exception e) {
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		JPanel pan = new JPanel(new BorderLayout());
		JLabel lab = new JLabel(message);
		pan.add(lab, BorderLayout.CENTER);
		pan.add(new JLabel(icon), BorderLayout.WEST);
		diag.add(pan);
		pan = new JPanel();
		JButton but = new JButton("OK");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				diag.setVisible(false);
			}
		});
		pan.add(but);
		but = new JButton("Details");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				JOptionPane.showMessageDialog(diag, e.getMessage(), title + " - Details", JOptionPane.ERROR_MESSAGE);
			}
		});
		pan.add(but);
		diag.add(pan);
		diag.pack();
		diag.setLocationRelativeTo(parent);
		diag.setVisible(true);
	}

	public static void showExceptionErrorDialog(JDialog parent, String message, String title, Exception e) {
		JDialog diag = new JDialog(parent, title, true);
		showDiag(diag, parent, title, message, e);

	}

	public static void showExceptionErrorDialog(JFrame parent, String message, String title, Exception e) {
		JDialog diag = new JDialog(parent, title, true);
		showDiag(diag, parent, title, message, e);
	}
}
