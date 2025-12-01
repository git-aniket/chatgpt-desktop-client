package nl.vu.psy.ams.suite.gui.tabs.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.ParseException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import nl.vu.psy.ams.suite.gui.CombinedLabelEditor;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.graphgen.GeneratedHRMOTDialog;
import nl.vu.psy.ams.suite.gui.graphgen.GraphGenerationDialog;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;

public class LabelInformationToolbar extends AmsToolBar {

	/*
	 * The label information toolbar
	 */
	private static final long serialVersionUID = 1L;

	private LabelInformationTab tab;

	public LabelInformationToolbar(LabelInformationTab tab) {
		super();

		this.tab = tab;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("saveToASCII")) {
			File retFile = SaveDialog.saveFileBrowser("lbldat", "ASCII Data File");
			if (retFile != null) {
				tab.getData().outputDataToASCII(retFile);
				LabelInformationTab.getInstance().setUpTable();
				MainFrame.getInstance().getMainFrame().repaint();
			}
		} else if (actionCommand.equals("saveToXLS")) {
			File retFile = SaveDialog.saveFileBrowser("xls", "Excel Spreadsheet");
			if (retFile != null) {
				tab.getData().outputDataToXLS(retFile);
				LabelInformationTab.getInstance().setUpTable();
				MainFrame.getInstance().getMainFrame().repaint();
			}
		} else if (actionCommand.equals("gengraph")) {
			GraphGenerationDialog diag = new GraphGenerationDialog();
			diag.setVisible(true);
		} else if (actionCommand.equals("editLabels")) {
			CombinedLabelEditor diag = new CombinedLabelEditor();
			diag.setVisible(true);
		} else if (actionCommand.equals("editConfig")) {
			tab.getData().editConfig();
		} else if (actionCommand.equals("hramotGraph")) {
			new GeneratedHRMOTDialog().setVisible(true);
		} else if (actionCommand.equals("saveToASCIITime")) {
			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Select time width", true);
			final JFormattedTextField timeWidth = new JFormattedTextField(60);
			timeWidth.setColumns(10);
			diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
			diag.add(new JLabel("Set time width (seconds): "));
			diag.add(timeWidth);
			JButton okButton = new JButton("OK");
			diag.getRootPane().setDefaultButton(okButton);
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
					try {
						timeWidth.commitEdit();
					} catch (ParseException e1) {
					}
					double tW = ((Number) timeWidth.getValue()).doubleValue() * 1000000.;
					File retFile = SaveDialog.saveFileBrowser("lbldat", "ASCII Data File");
					if (retFile != null) {
						tab.getData().outputDataToASCII(retFile, tW, true);
						LabelInformationTab.getInstance().setUpTable();
						MainFrame.getInstance().getMainFrame().repaint();
					}
				}
			});
			diag.add(okButton);
			diag.getRootPane().setDefaultButton(okButton);// ENTER will hit button OK

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
				}
			});
			diag.add(cButton);
			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);
		} else if (actionCommand.equals("saveToXLSTime")) {
			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Select time width", true);
			final JFormattedTextField timeWidth = new JFormattedTextField(60);
			timeWidth.setColumns(10);
			diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
			diag.add(new JLabel("Set time width (seconds): "));
			diag.add(timeWidth);
			JButton okButton = new JButton("OK");
			diag.getRootPane().setDefaultButton(okButton);
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
					try {
						timeWidth.commitEdit();
					} catch (ParseException e1) {
					}
					double tW = ((Number) timeWidth.getValue()).doubleValue() * 1000000.;
					File retFile = SaveDialog.saveFileBrowser("xls", "Excel Spreadsheet");
					if (retFile != null) {
						tab.getData().outputDataToXLS(retFile, tW, true);
						LabelInformationTab.getInstance().setUpTable();
						MainFrame.getInstance().getMainFrame().repaint();
					}
				}
			});
			diag.add(okButton);
			diag.getRootPane().setDefaultButton(okButton);// ENTER will hit button OK

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
				}
			});
			diag.add(cButton);
			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);
		}
	}

	@Override
	public void addButtons() {
		setupButton("config", "Edit Output Config", "editConfig", KeyStroke.getKeyStroke(KeyEvent.VK_G, 0, true));
		addNewSeparator();
		setupButton("export", "Export 'Per Label' Information To ASCII File", "saveToASCII",
				KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("export_XLS", "Export 'Per Label' Information To Excel Spreadsheet", "saveToXLS",
				KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("export", "Export 'Per Minute' Information To ASCII File", "saveToASCIITime",
				KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("export_XLS", "Export 'Per Minute' Information To Excel Spreadsheet", "saveToXLSTime",
				KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("export_bar", "Generate Bar Graph Of Data", "gengraph",
				KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK, true));
		// setupButton("config_bar", "Edit Combined Labels", "editLabels",
		// KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, true));
		addNewSeparator();
		setupButton("export_hramot", "Generate Heart Rate graph", "hramotGraph",
				KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK, true));
	}

}
