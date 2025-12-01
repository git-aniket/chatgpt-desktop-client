package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Class that can provide a dialog for saving a file with
 * a given extension, possibly with a string appended or prepended.
 * Used a lot for saving .beat files, excel files, etc, since it makes 
 * a lot of things easier (it automatically suggests a filename for instance) 
 */
public class SaveDialog {
	private File outFile;
	public static boolean showEvents = false;

	public static File saveFileBrowser(final String extension, final String title) {
		String fn = "";
		File datFile = CurrentOpenData.getInstance().getDataFile();
		if (datFile != null)
			fn = Utils.guaranteeExtension(Utils.removeExtension(datFile.getPath()), extension);
		return saveFileBrowser(extension, title, fn, false, "");
	}

	public static File saveFileBrowser(final String extension, final String title, final String fn, boolean Checkbox,
			String checkboxdesc) {
		final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Save " + title, true);
		JPanel filePanel = new JPanel(new BorderLayout());
		final JTextField tf = new JTextField(64);
		final SaveDialog sd = new SaveDialog();
		tf.setEnabled(false);

		tf.setText(fn);

		filePanel.add(tf, BorderLayout.CENTER);
		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				File fl = null;
				final FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals(extension);
					}

					@Override
					public String getDescription() {
						return title;
					}
				});
				if (tf.getText().length() > 2)
					fc.setSelectedFile(new File(tf.getText()));
				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				if (fl != null) {
					// File fl = new File(fileDialog.getDirectory(), fileName);
					tf.setText(Utils.guaranteeExtension(fl.toString(), extension));
				}
			}
		});

		filePanel.add(browseButton, BorderLayout.EAST);

		final JCheckBox raw = new JCheckBox(checkboxdesc);

		raw.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setEvents(raw.isSelected());
			}

		});
		JPanel butPanel = new JPanel();
		// butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
		JButton okButton = new JButton("Save");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setEvents(raw.isSelected());
				sd.setOutFile(new File(tf.getText()));
				diag.setVisible(false);
			}
		});
		JButton canButton = new JButton("Cancel");
		canButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				sd.setOutFile(null);
				diag.setVisible(false);
			}
		});
		if (Checkbox) {
			butPanel.add(raw, BorderLayout.WEST);
		}
		butPanel.add(canButton);
		butPanel.add(okButton);

		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
		diag.add(filePanel);
		diag.add(butPanel);
		diag.pack();
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
		diag.setVisible(true);

		if (sd.outFile != null) {
			File fcfile = new File(tf.getText());
			File fd = new File(fcfile.getParent());
			File datFile = CurrentOpenData.getInstance().getDataFile();

			if (sd.outFile.exists()) {
				int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						"There is an existing file. Would you like to overwrite it?",
						"Existing file", JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.NO_OPTION || res == JOptionPane.CLOSED_OPTION)
					return saveFileBrowser(extension, title, sd.outFile.getAbsolutePath(), false, "");
			}

			else if (datFile != null) {
				if (Utils.getExtension(sd.outFile).equals("5fs")) {
					if (fd.getFreeSpace() < (datFile.length())) {
						System.out.println("fclenght:_ " + datFile.length() + " " + fd.getFreeSpace());
						JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
								"There is no enough space in the drive. Please choose a different location. If you are trying to save an .amsdata file to a CF card we always advise moving the .5FS file to another location.");
						return saveFileBrowser(extension, title, fcfile.getAbsolutePath(), false, "");
					}
				} else {
					if (fd.getFreeSpace() < (1.5 * datFile.length())) {
						System.out.println("fclenght:_ " + datFile.length() + " " + fd.getFreeSpace());
						JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
								"There is no enough space in the drive. Please choose a different location");
						return saveFileBrowser(extension, title, fcfile.getAbsolutePath(), false, "");
					}
				}
			}
		}
		return sd.outFile;
	}

	public static File saveFileBrowserWithAddition(final String extension, final String addition, final String title) {
		// For Save as purpose. Includes "copy" in the file name
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String fn;
		if (datFile != null)
			fn = Utils.guaranteeExtensionAndAddition(datFile.getPath(), extension, addition);
		else
			fn = addition;
		return saveFileBrowser(extension, title, fn, false, "");
	}

	public static File saveFileBrowserWithAdditionforQRS(final String extension, final String addition,
			final String title) { // For Save as purpose. Includes "copy" in the file name
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String fn = Utils.guaranteeExtensionAndAddition(datFile.getPath(), extension, addition);
		return saveFileBrowser(extension, title, fn, true, "Include Event Information");
	}

	public static File saveFileBrowserWithAdditionforSCL(final String extension, final String addition,
			final String title) { // For Save as purpose. Includes "copy" in the file name
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String fn = Utils.guaranteeExtensionAndAddition(datFile.getPath(), extension, addition);
		return saveFileBrowser(extension, title, fn, true, "Remove headers");
	}

	public static File saveFileBrowserWithPrepend(final String extension, final String addition, final String title) {
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String fn = Utils.guaranteeExtensionAndPrepend(datFile.getPath(), extension, addition);
		return saveFileBrowser(extension, title, fn, false, "");
	}

	public static File saveFileBrowserWithChanges(final String extension, final String addition, final String title) {
		// For Save as purpose. Includes "copy" in the file name
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String currentfilename = Utils.removeExtension(datFile.getName());
		String filenamewithsubid = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
		// File newnamefor5fs = new
		// File(datFile.getParent(),CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()
		// + "_" + datFile.getName());
		String fn;
		GregorianCalendar sd = CurrentOpenData.getInstance().getStarts().get(0).gettStamp().toGregorianCalendar();
		long startTime = 1000 * CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		String findtime1 = Utils.getDateAndTimeFromUS(startTime, sd, startTime);

		String originaltimestampin5fsfile = findtime1.substring(3, 5) + findtime1.substring(0, 2)
				+ findtime1.substring(11, 13) + findtime1.substring(14, 16);

		if (currentfilename.contains(originaltimestampin5fsfile) && currentfilename.contains(filenamewithsubid)) {
			fn = datFile.getParent() + File.separator + CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()
					+ "_" + originaltimestampin5fsfile + "." + extension;

		} else if (currentfilename.contains(originaltimestampin5fsfile)) {
			fn = datFile.getParent() + File.separator + CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()
					+ "_" + currentfilename + "." + extension;
		} else if (currentfilename.contains(filenamewithsubid)) {
			fn = datFile.getParent() + File.separator + currentfilename + "_" + originaltimestampin5fsfile + "."
					+ extension;
		} else {
			fn = datFile.getParent() + File.separator + CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()
					+ "_" + originaltimestampin5fsfile + "." + extension;
		}

		return saveFileBrowser(extension, title, fn, false, "");
	}

	private SaveDialog() {

	}

	private void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public static void setEvents(boolean show) {
		showEvents = show;
	}

}
