package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;

import org.apache.commons.lang3.time.DateUtils;
import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

public class Merge7fsFilesDialog {

	private JCheckBox openCheck;
	private JTextField tf;
	private JDialog diag;
	private JCheckBox overwriteCheck;
	private static final int BUFFER_SIZE = 4096; // 4KB

	public Merge7fsFilesDialog() {

		String titleString = "Merge 7fs files";

		diag = new JDialog(MainFrame.getInstance().getMainFrame(), titleString, true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		tf = new JTextField(64);
		tf.setEnabled(false);

		JPanel browsePanel = new JPanel(new BorderLayout());
		JButton browseButton = new JButton("Select Directory");
		browseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					tf.setText(fc.getSelectedFile().getAbsolutePath());
			}
		});

		browsePanel.add(tf, BorderLayout.CENTER);
		browsePanel.add(browseButton, BorderLayout.EAST);

		JPanel optionsPanel = new JPanel();
		openCheck = new JCheckBox("Open the merged file directly");
		openCheck.setSelected(true);
		optionsPanel.add(openCheck);
		overwriteCheck = new JCheckBox("Overwrite existing file");
		overwriteCheck.setSelected(false);
		optionsPanel.add(overwriteCheck);

		diag.add(browsePanel);
		diag.add(optionsPanel);

		JPanel butPanel = new JPanel();
		final JButton convButton = new JButton("Merge");
		convButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<File> toMergeFiles = new ArrayList<File>();
				String outFile[] = new String[1];
				toMergeFiles = listMergeFiles(new File(tf.getText()), outFile);
				if (toMergeFiles.size() < 2) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"No files to merge, no merge necessary, or filename(s) in wrong format!!\n");
					return;
				}
				File output = new File(toMergeFiles.get(0).getParentFile().getAbsolutePath(), outFile[0]);
				if (output.exists() && overwriteCheck.isSelected() == false) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							output.getName() + " exists (and 'Overwrite existing file' not checked)\n");
					return;
				}
				mergeFiles(output, toMergeFiles);
				if (openCheck.isSelected()) {
					Ams7fsFile file7fs = new Ams7fsFile(output.getAbsolutePath());
					file7fs.start();
				}
				diag.setVisible(false);
			}
		});
		JButton cancButton = new JButton("Cancel");
		cancButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});

		butPanel.add(convButton);
		butPanel.add(cancButton);
		diag.add(butPanel);

		diag.pack();
		diag.setResizable(true);// JdH So it fits on MacOS
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	public static String removeExtension(String fname) {
		int pos = fname.lastIndexOf('.');
		if (pos > -1)
			return fname.substring(0, pos);
		else
			return fname;
	}

	static protected void mergeFiles(File output, ArrayList<File> toMergeFiles) {
		if (toMergeFiles.isEmpty()) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No files to merge!");
			return;
		}
		if (toMergeFiles.size() == 1) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No merge necessary!");
			return;
		}
		JFrame frame = MainFrame.getInstance().getMainFrame();
		ProgressMonitor progress = new ProgressMonitor(frame, "Merging Data Files", null, 0, toMergeFiles.size());
		int prog = 0;
		progress.setProgress(prog);
		for (File file : toMergeFiles) {
			prog++;
			try (
					InputStream inputStream = new FileInputStream(file);
					OutputStream outputStream = new FileOutputStream(output, true);) {
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = -1;

				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}
			progress.setProgress(prog);
		}
		if (progress != null)
			progress.close();
	}

	static protected ArrayList<File> listMergeFiles(File path, String outFile[]) {
		// use single element array outFile to simulate pass-by-reference
		ArrayList<File> retList = new ArrayList<File>();
		ArrayList<File> ptrList = new ArrayList<File>();
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				String ext = Utils.getExtension(files[i]);
				if (ext != null) {
					String compExt = "7fs";
					if (ext.equals(compExt)) {
						retList.add(files[i]);
					}
					if (ext.equals("ptr"))
						ptrList.add(files[i]);
				}
			}
		}
		retList.sort(Comparator.comparing(o -> o.getName().toUpperCase()));
		String ptrFile = "";
		for (File f : ptrList) {
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if (i >= 0 && i < s.length() - 1) {
				s = s.substring(0, i - 1).toLowerCase();
			}
			if (retList.get(0).getName().startsWith(s))
				ptrFile = f.getPath();
		}
		String fileName;
		String[] parts;
		int datepos = -1, firstFile = -1, lastFile = -1;
		Integer prevFile = 0, curFile = 0;
		// LocalDate prevDate = LocalDate.of(2023, 1, 1);
		// LocalTime prevTime = LocalTime.of(12, 0, 0);
		Date prevDate = null, prevTime = null;
		boolean newFormat = false;
		String outputFile = "merged.7fs";
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat timedf = new SimpleDateFormat("HHmmss");
		for (File file : retList) {
			fileName = removeExtension(file.getName());
			parts = fileName.split("_");
			if (datepos >= 0 && datepos + 1 < parts.length) {
				Date curDate = Utils.parseDate(df, parts[datepos]);
				Date curTime = Utils.parseDate(timedf, parts[datepos + 1]);
				if (newFormat && datepos + 2 < parts.length)
					curFile = Utils.parseInt(parts[datepos + 2]);
				if (prevDate == null || prevTime == null)
					lastFile++;
				else if (curDate == null || curTime == null || curFile == null || prevFile == null)
					break;
				// System.out.println((curDate.equals(prevDate)) + " " +
				// (curTime.compareTo(prevTime) > 0));
				else if (((curDate.equals(prevDate) && curTime.compareTo(prevTime) > 0)
						|| curDate.compareTo(DateUtils.addDays(prevDate, 1)) == 0)
						&& (!newFormat || curFile.equals(prevFile + 1)))
					lastFile++;
				else
					break;
			} else {
				firstFile++;
				lastFile++;
			}
			if (datepos < 0) {
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].length() == 8 && parts[i].matches("[0-9]+")) {
						datepos = i;
						if (parts[i + 2].length() == 2 && parts[i + 2].matches("[0-9]+"))
							newFormat = true;
						if (i > 0) {
							outputFile = parts[0];
							for (int j = 1; j < i; j++)
								outputFile = outputFile + '_' + parts[j];
							outputFile = outputFile + ".7fs";
						} else if (newFormat && i < parts.length - 3) {
							outputFile = parts[i + 3];
							for (int j = i + 4; j < parts.length; j++)
								outputFile = outputFile + '_' + parts[j];
							outputFile = outputFile + ".7fs";
						} else if (!newFormat && i < parts.length - 2) {
							outputFile = parts[i + 2];
							for (int j = i + 3; j < parts.length; j++)
								outputFile = outputFile + '_' + parts[j];
							outputFile = outputFile + ".7fs";
						}
						break;
					}
				}
				outFile[0] = outputFile;
			}
			if (datepos >= 0 && datepos + 1 < parts.length) {
				prevDate = Utils.parseDate(df, parts[datepos]);
				prevTime = Utils.parseDate(timedf, parts[datepos + 1]);
				if (newFormat && datepos + 2 < parts.length)
					prevFile = Utils.parseInt(parts[datepos + 2]);
			}
		}
		if (ptrFile != "") {
			BufferedReader read = null;
			try {
				read = new BufferedReader(new FileReader(ptrFile));
				String line1;
				line1 = read.readLine();
				while (line1 != null && (line1.startsWith("#")))
					line1 = read.readLine();
				while (line1 != null) {
					if (line1.length() > 1) {
						String[] values = line1.split("\\s+");
						if (values.length > 1) {
							long filepointer = Long.valueOf(values[1]);
							String[] v = values[0].split("/");
							String filename = ptrList.get(0).getParent() + File.separator + v[v.length - 1];
							FileOutputStream fout = new FileOutputStream(filename, true);
							FileChannel channel = fout.getChannel();
							channel = channel.truncate(filepointer);
							channel.close();
							fout.close();
						}
					}
					line1 = read.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} finally {
				if (read != null) {
					try {
						read.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return new ArrayList<File>(retList.subList(firstFile, lastFile + 1));
	}

	public void setVisible(boolean b) {
		diag.setVisible(b);
	}

}
