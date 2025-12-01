package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.MetaLabel;
import nl.vu.psy.ams.suite.data.structures.sets.MetaLabelSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Dialog that lets you edit combined labels (or MetaLabels),
 * for use in generating bar graphs.
 */
public class CombinedLabelEditor extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	MetaLabelSet mSet = CurrentOpenData.getInstance().getMetaLabels();

	final Map<String, String> codeMap = new HashMap<String, String>();

	final JList<String> list;
	final DefaultListModel<String> lModel = new DefaultListModel<String>();

	public CombinedLabelEditor() {
		super(MainFrame.getInstance().getMainFrame(), true);
		setTitle("Edit combined labels");
		ArrayList<MetaLabel> mList = mSet.getMetaLabels();
		for (int i = 0; i < mList.size(); i++) {
			String lName = mList.get(i).getName();
			lModel.addElement(lName);
			String codeVals = "";
			for (int code : mList.get(i).getConnectedCodes()) {
				codeVals += code;
				if (code != mList.get(i).getConnectedCodes().getLast())
					codeVals += " ";
			}
			codeMap.put(lName, codeVals);
		}
		list = new JList<String>(lModel);

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);

		JButton addButton = new JButton("Add New Label");
		addButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String newLabel = JOptionPane.showInputDialog("Set name of the new label:");
				if (newLabel != null) {
					int foundIndex = lModel.indexOf(newLabel);
					if (foundIndex == -1) {
						String labelCodes = JOptionPane.showInputDialog(
								"Enter space-separated list of label codes for label \"" + newLabel + "\":");
						if (labelCodes == null)
							labelCodes = "";
						codeMap.put(newLabel, labelCodes);
						lModel.addElement(newLabel);
						repaint();
					}
				}
			}
		});

		JButton editButton = new JButton("Edit Selected Label");
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (list.isSelectionEmpty() == false) {
					String selVal = (String) list.getSelectedValue();
					String curVal = codeMap.get(selVal);
					String newVal = JOptionPane.showInputDialog(
							"Enter space-separated list of label codes for label \"" + selVal + "\":", curVal);
					if (newVal != null) {
						codeMap.put(selVal, newVal);
					}
				}
			}
		});

		JButton removeButton = new JButton("Remove Selected Label");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (list.isSelectionEmpty() == false) {
					String selVal = (String) list.getSelectedValue();
					int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
							"Are you sure you want to remove the selected label \""
									+ selVal + "\"?",
							"Confirm remove", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION) {
						lModel.removeElement(selVal);
						codeMap.remove(selVal);
					}
				}
			}
		});

		JPanel addEditPanel = new JPanel();
		addEditPanel.add(addButton);
		addEditPanel.add(editButton);
		addEditPanel.add(removeButton);

		JButton defButton = new JButton("Set as default");
		defButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				save();
				CurrentOpenData.getInstance().setDirty(true);
				// CurrentOpenData.getInstance().saveChangeablesToDisk();
				File comFile = new File(CurrentOpenData.getInstance().getFilePath(), "combinedlabels.json");
				File outFile = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
				// outFile.mkdirs();
				outFile = new File(outFile, "VU-DAMS/combinedlabels.json");
				if (outFile.exists())
					outFile.delete();
				comFile.renameTo(outFile);
			}
		});
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				save();
				setVisible(false);
			}
		});
		JButton closeButton = new JButton("Cancel");
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		JPanel saveClosePanel = new JPanel();
		saveClosePanel.add(defButton);
		saveClosePanel.add(saveButton);
		saveClosePanel.add(closeButton);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.add(addEditPanel);
		buttonPanel.add(new JSeparator());
		buttonPanel.add(saveClosePanel);

		setLayout(new BorderLayout());
		add(new JScrollPane(list), BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	protected void save() {
		mSet.clear();
		for (int i = 0; i < lModel.getSize(); i++) {
			MetaLabel mLabel = new MetaLabel();
			String curVal = (String) lModel.getElementAt(i);
			String codeVal = codeMap.get(curVal);
			String[] codes = codeVal.split(" ");
			mLabel.setName(curVal);
			for (String code : codes) {
				Integer codeInt = Utils.parseInt(code);
				if (code != null)
					mLabel.addConnectedCode(codeInt);
			}
			mSet.addMetaLabel(mLabel);
		}

	}
}
