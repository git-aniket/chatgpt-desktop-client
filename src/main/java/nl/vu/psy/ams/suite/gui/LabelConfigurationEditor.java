package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.gui.tabs.info.LabelInformationTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.gui.tabs.qrs.QRSTab;
import nl.vu.psy.ams.suite.gui.tabs.rsa.RSATab;
import nl.vu.psy.ams.suite.gui.tabs.scl.SCLTab;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.FileDialogWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The label configuration editor dialog. Was used to edit
 * the label config, but now only provides buttons to import / export
 * label.cfg files and to set a given config as default.
 */
public class LabelConfigurationEditor extends JDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTree tree;
	private DefaultMutableTreeNode root;
	private JTextField catField = new JTextField(16);
	private JTextField valField = new JTextField(16);
	private DefaultTreeModel model;

	public LabelConfigurationEditor() {
		super(MainFrame.getInstance().getMainFrame(), true);
		this.setTitle("Edit label configuration");
		this.setLayout(new BorderLayout());

		root = new DefaultMutableTreeNode("Categories");
		AmsLabelConfiguration lblcfg = CurrentOpenData.getInstance().getLabelConfig();
		for (String s : lblcfg.getCategories()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(s);
			root.add(node);
			for (LabelValue v : lblcfg.getValuesForCategory(s)) {
				node.add(new DefaultMutableTreeNode(v));
			}
		}
		model = new DefaultTreeModel(root);
		tree = new JTree(model);
		tree.setEditable(true);
		tree.setExpandsSelectedPaths(true);
		// tree.setRootVisible(false);
		tree.setShowsRootHandles(true);

		for (int i = 0; i < tree.getRowCount(); i++)
			tree.expandRow(i);

		JPanel botPanel = new JPanel();
		botPanel.setLayout(new BoxLayout(botPanel, BoxLayout.Y_AXIS));

		JPanel addCatPanel = new JPanel();
		addCatPanel.setLayout(new BorderLayout());
		addCatPanel.add(catField, BorderLayout.CENTER);
		catField.setActionCommand("Add Category");
		JButton but = new JButton("Add Category");
		but.addActionListener(this);
		catField.addActionListener(this);
		addCatPanel.add(but, BorderLayout.EAST);

		JPanel addValPanel = new JPanel();
		addValPanel.setLayout(new BorderLayout());
		addValPanel.add(valField, BorderLayout.CENTER);
		valField.setActionCommand("Add Value");
		JButton but2 = new JButton("Add Value");
		but2.addActionListener(this);
		valField.addActionListener(this);
		addValPanel.add(but2, BorderLayout.EAST);

		JPanel addPanel = new JPanel();
		addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
		addPanel.add(addCatPanel);
		addPanel.add(addValPanel);

		JPanel removePanel = new JPanel(new BorderLayout());
		JButton removeButton = new JButton("Remove Selected");
		removeButton.addActionListener(this);
		removePanel.add(removeButton, BorderLayout.CENTER);

		JPanel importPanel = new JPanel(new BorderLayout());
		JButton importButton = new JButton("Import Label Config From File");
		importButton.addActionListener(this);
		importPanel.add(importButton, BorderLayout.CENTER);

		JPanel saveCFGPanel = new JPanel(new BorderLayout());
		JButton saveCFGButton = new JButton("Save Label Config To File");
		saveCFGButton.addActionListener(this);
		saveCFGPanel.add(saveCFGButton, BorderLayout.CENTER);

		JPanel setDefPanel = new JPanel(new BorderLayout());
		JButton setDefButton = new JButton("Set Label Config As Default");
		setDefButton.addActionListener(this);
		setDefPanel.add(setDefButton, BorderLayout.CENTER);

		botPanel.add(addPanel);
		botPanel.add(removePanel);
		botPanel.add(importPanel);
		botPanel.add(saveCFGPanel);
		botPanel.add(setDefPanel);

		botPanel.setBorder(BorderFactory.createTitledBorder("Edit categories"));

		JPanel saveCancelPanel = new JPanel();
		saveCancelPanel.setLayout(new BoxLayout(saveCancelPanel, BoxLayout.X_AXIS));
		saveCancelPanel.add(Box.createHorizontalGlue());
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		saveCancelPanel.add(saveButton);
		saveCancelPanel.add(Box.createHorizontalGlue());
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		saveCancelPanel.add(cancelButton);
		saveCancelPanel.add(Box.createHorizontalGlue());

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

		southPanel.add(botPanel);

		southPanel.add(saveCancelPanel);

		JPanel midPanel = new JPanel(new BorderLayout());
		midPanel.setBorder(BorderFactory.createTitledBorder("Current categories"));
		midPanel.add(new JScrollPane(tree), BorderLayout.CENTER);

		this.add(midPanel, BorderLayout.CENTER);
		this.add(southPanel, BorderLayout.SOUTH);
		this.pack();
		this.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("Add Category")) {
			if (catField.getText() != null && catField.getText().equals("") == false) {
				String txt = catField.getText();
				boolean alreadyInList = false;
				for (int i = 0; i < root.getChildCount(); i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
					if (txt.equals(node.toString())) {
						alreadyInList = true;
						break;
					}
				}
				if (alreadyInList == false) {
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(txt);
					model.insertNodeInto(newNode, root, root.getChildCount());
					catField.setText("");
					TreePath tp = new TreePath(newNode.getPath());
					tree.scrollPathToVisible(tp);
					tree.setSelectionPath(tp);
				}
			}
		} else if (arg0.getActionCommand().equals("Add Value")) {
			if (valField.getText() != null && valField.getText().equals("") == false) {
				TreePath path = tree.getSelectionPath();
				if (path != null) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
					boolean isCategory = false;
					for (int i = 0; i < root.getChildCount(); i++) {
						DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) root.getChildAt(i);
						if (node.equals(node2)) {
							isCategory = true;
							break;
						}
					}
					if (isCategory) {
						String[] parts = valField.getText().split(" ");
						String txt = valField.getText().split(" ")[0]; // check code only
						boolean isInt = true;
						if (Utils.parseInt(txt) == null) {
							isInt = false;
						}
						if (!isInt || parts.length > 2) {
							Integer newCode = 10;
							boolean found = false;
							while (!found) {
								boolean exists = false;
								for (int i = 0; i < root.getChildCount(); i++) {
									DefaultMutableTreeNode node3 = (DefaultMutableTreeNode) root.getChildAt(i);
									for (int j = 0; j < node3.getChildCount(); j++) {
										DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) node3.getChildAt(j);
										if ((newCode).toString().equals(node2.toString().split(" ")[0])) {
											newCode++;
											exists = true;
											break;
										}
									}
								}
								if (!exists) {
									found = true;
									valField.setText(newCode.toString() + " " + valField.getText());
								}
							}
						}
						boolean alreadyInList = false;
						for (int i = 0; i < node.getChildCount(); i++) {
							DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) node.getChildAt(i);
							if (txt.equals(node2.toString().split(" ")[0])) {
								alreadyInList = true;
								break;
							}
						}
						if (alreadyInList == false) {
							DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(valField.getText());
							model.insertNodeInto(newNode, node, node.getChildCount());
							tree.scrollPathToVisible(new TreePath(newNode.getPath()));
							valField.setText("");
						}
					}
				}
			}
		} else if (arg0.getActionCommand().equals("Remove Selected")) {
			TreePath path = tree.getSelectionPath();
			if (path != null) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (node.equals(root) == false)
					model.removeNodeFromParent(node);
			}
		} else if (arg0.getActionCommand().equals("Cancel")) {
			this.setVisible(false);
		} else if (arg0.getActionCommand().equals("Save")) {
			AmsLabelConfiguration newLblCfg = new AmsLabelConfiguration();
			for (int i = 0; i < root.getChildCount(); i++) {
				ArrayList<String> values = new ArrayList<String>();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
				for (int j = 0; j < node.getChildCount(); j++) {
					DefaultMutableTreeNode value = (DefaultMutableTreeNode) node.getChildAt(j);
					values.add(value.toString());
				}
				ArrayList<LabelValue> lValues = new ArrayList<LabelValue>();
				for (String s : values) {
					String[] parts = s.split(" ");
					if (parts.length > 1) {
						int val = Integer.parseInt(parts[0]);
						lValues.add(new LabelValue(val, s.substring(parts[0].length() + 1)));
					}
				}
				newLblCfg.getConfig().put(node.toString(), lValues);
			}
			CurrentOpenData.getInstance().setLabelConfig(newLblCfg);
			CurrentOpenData.getInstance().setDirty(true);
			LabelInformationTab.getInstance().getData().updateLabelConfig();
			LabelTab.getInstance().updateLabelConfig();
			QRSTab.getInstance().updateLabelConfig();
			RSATab.getInstance().updateLabelConfig();
			if (CurrentOpenData.getInstance().channelExists("SCL") == true)
				SCLTab.getInstance().updateLabelConfig();
			TimeBar.getInstance().updateLabelConfig();
			this.setVisible(false);
		} else if (arg0.getActionCommand().equals("Import Label Config From File")) {
			String OSname = System.getProperty("os.name");
			File fl = null;
			if (!OSname.contains("Mac")) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return (f.getName().endsWith(".cfg") | f.getName().endsWith(".cfg.txt"));
					}

					@Override
					public String getDescription() {
						return "Label Config files";
					}
				});

				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
			} else {
				FileDialogWithSavedDir fd = new FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
				fd.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File f, String s) {
						s = s.toLowerCase();
						if (s.endsWith(".cfg") | s.endsWith(".cfg.txt"))
							return true;
						return false;
					}
				});
				fd.setVisible(true);
				if (fd.getFile() != null) {
					String filename = fd.getDirectory() + File.separator + fd.getFile();
					fl = new File(filename);
				}
			}
			if (fl != null) {
				getConfigFromFile(fl);
			}
		} else if (arg0.getActionCommand().equals("Set Label Config As Default")) {
			String OSname = System.getProperty("os.name");
			String path = "";
			if (OSname.contains("Mac")) {
				path = System.getProperty("user.home") + "/Library/Application " + "Support";
			} else if (OSname.contains("Linux")) {
				path = System.getProperty("user.home") + "/.local/share/applications";
			} else {
				path = System.getenv("APPDATA");
			}
			// CurrentOpenData.getInstance().saveChangeablesToDisk();
			CurrentOpenData.getInstance().setDirty(true);
			File lbFile = new File(CurrentOpenData.getInstance().getFilePath(), "labelconfig.json");
			/*
			 * //----------------- Previous code to save the labelconfig.json to Temp
			 * Directory-----------------
			 * File outFile = new
			 * File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
			 * //outFile.mkdirs();
			 * outFile = new File(outFile, "VU-DAMS/labelconfig.json");
			 * outFile.delete();
			 * lbFile.renameTo(outFile);
			 * //---------------------------------------------------------------------------
			 * --------------------
			 */

			// ----------------Code to save labelconfig.json to App Data
			// directory----------------------------
			File outFile = new File(path, "VU-DAMS");
			if (outFile.exists() == false) {
				outFile.mkdir();
			}
			outFile = new File(outFile, "labelconfig.json");
			outFile.delete();
			lbFile.renameTo(outFile);
			// ----------------------------------------------------------------------------------------------

		} else if (arg0.getActionCommand().equals("Save Label Config To File")) {
			File outFile = SaveDialog.saveFileBrowser("cfg", "Label config file");
			if (outFile != null) {
				CurrentOpenData.getInstance().getLabelConfig().saveConfigToFile(outFile);
			}
		}
	}

	private void getConfigFromFile(File fl) {
		AmsLabelConfiguration lblcfg = CurrentOpenData.getInstance().getLabelConfig();
		lblcfg.getConfigFromFile(fl);
		LabelInformationTab.getInstance().getData().updateLabelConfig();
		LabelTab.getInstance().updateLabelConfig();
		QRSTab.getInstance().updateLabelConfig();
		RSATab.getInstance().updateLabelConfig();
		if (CurrentOpenData.getInstance().channelExists("SCL") == true)
			SCLTab.getInstance().updateLabelConfig();
		TimeBar.getInstance().updateLabelConfig();
		this.setVisible(false);
	}

}
