package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import nl.vu.psy.ams.suite.tools.JLabelAntialiased;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Shows information about the app.
 */
public class AboutDialog extends JDialog implements ActionListener {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;

	public String ClipboardText;

	public AboutDialog() {
		super(MainFrame.getInstance().getMainFrame(), "VU-DAMS " + Utils.VERSIONSTRING + " - About", true);
		this.setLayout(new BorderLayout());
		this.setResizable(false);

		JLabel logoLabel = new JLabel(new ImageIcon(getClass().getResource("/img/vuamslogo.png")));
		logoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.add(logoLabel, BorderLayout.CENTER);

		String arch 		= System.getProperty("os.arch");
		String realArch 	= "null";
	    realArch 			= arch.endsWith("64")
	                          ? "64" : "32";

		JPanel southPanel = new JPanel(new BorderLayout());
		JLabel txtLabel = new JLabelAntialiased(
				"<html><center><br><b>"
						+ Utils.APPNAME
						+ " </b><br>"
						//+ Utils.getAppVersion()+" <br>"
						+"Java Version: " + System.getProperty("java.version")+" <br>"
						+"Java Virtual Machine: " + System.getProperty("sun.arch.data.model")+"-bit <br>"
						+"Operating System: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ") " + realArch + "-bit"+" <br>"
						+"<table><tr><td align=right>Website:</td><td>www.vu-ams.nl</td></tr><tr><td align=right>Email:</td><td>VUams.fgb@vu.nl</td></tr></table><br>Created by:<br>D.M. Pelt (2010-2011), M.Viswanathan (2012-2015), J. den Hartog (2012-2019), <br>R. Pool (2015-) Aniket Mazumder (2023-), and Q. Helmer (2019-) <br><br></center></html>");
		ClipboardText = txtLabel.getText();
		txtLabel.setHorizontalAlignment(SwingConstants.CENTER);
		txtLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		southPanel.add(txtLabel, BorderLayout.CENTER);
		JPanel butPanel = new JPanel();
		butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		JButton copyButton = new JButton("Copy to Clipboard");
		copyButton.addActionListener(this);
		butPanel.add(Box.createHorizontalGlue());
		butPanel.add(copyButton);
		butPanel.add(closeButton);
		butPanel.add(Box.createHorizontalGlue());
		southPanel.add(butPanel, BorderLayout.SOUTH);
		this.add(southPanel, BorderLayout.SOUTH);
		this.pack();
		this.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Close")) {
			this.setVisible(false);
		}
		if (e.getActionCommand().equals("Copy to Clipboard")) {
			ClipboardText=ClipboardText.replaceAll("<br>", "\n");
			ClipboardText=ClipboardText.replaceAll("\\<.*?\\>", "");
			StringSelection stringSelection = new StringSelection (ClipboardText);
			Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
			clpbrd.setContents (stringSelection, null);
			JOptionPane.showMessageDialog(null, "You can now paste the version info in an email", "Info copied to clipboard", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
