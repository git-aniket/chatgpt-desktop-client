package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;

public class Import7fsFilesDialog {

	private JTextField tf, tfa, tfs;
	private JDialog diag;
	File amsFile;

	public Import7fsFilesDialog() {

		String titleString = "Import 7fs files from sd card";
		String outFile[] = new String[1];
		final String sourceDir[] = new String[2];

		diag = new JDialog(MainFrame.getInstance().getMainFrame(), titleString, true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		tf = new JTextField(64);
		tf.setEnabled(false);

		JPanel browsePanel = new JPanel(new BorderLayout());
		JButton browseButton = new JButton("Select Directory On Card");
		browseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir("VU-DAMS/lastimportdir.txt");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					tf.setText(fc.getSelectedFile().getAbsolutePath());
				String sourceName = tf.getText();
				sourceDir[0] = sourceName.substring(sourceName.lastIndexOf(File.separator) + 1,
						sourceName.length());
				sourceDir[1] = sourceName;
			}
		});
		browsePanel.add(tf, BorderLayout.CENTER);
		browsePanel.add(browseButton, BorderLayout.EAST);
		diag.add(browsePanel);

		tfa = new JTextField(64);
		tfa.setEnabled(false);

		JPanel browsePanel2 = new JPanel(new BorderLayout());
		JButton browseButton2 = new JButton("Archive/backup directory for raw data (optional)");
		browseButton2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir("VU-DAMS/lastarchivedir.txt");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					tfa.setText(fc.getSelectedFile().getAbsolutePath());
			}
		});
		browsePanel2.add(tfa, BorderLayout.CENTER);
		browsePanel2.add(browseButton2, BorderLayout.EAST);
		diag.add(browsePanel2);

		tfs = new JTextField(64);
		tfs.setEnabled(false);

		JPanel browsePanel3 = new JPanel(new BorderLayout());
		JButton browseButton3 = new JButton("Output File");
		browseButton3.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				amsFile = SaveDialog.saveFileBrowserWithAddition("amsdatai",
						System.getProperty("user.home") + File.separator + sourceDir[0] + ".amsdatai",
						"Ams Data File");
				tfs.setText(amsFile.getAbsolutePath());
			}
		});
		browsePanel3.add(tfs, BorderLayout.CENTER);
		browsePanel3.add(browseButton3, BorderLayout.EAST);
		diag.add(browsePanel3);

		JPanel butPanel = new JPanel();
		final JButton convButton = new JButton("Import");
		convButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<File> toMergeFiles = new ArrayList<File>();
				toMergeFiles = Merge7fsFilesDialog.listMergeFiles(new File(tf.getText()), outFile);
				if (toMergeFiles.size() < 1) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"No 7fs files found or filename(s) in wrong format!!\n");
					return;
				}
				// File output = new File(amsFile.getParentFile(), sourceDir[0] + ".7fs");
				// if (output.exists()) {
				// JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
				// output.getName() + " already exists\n");
				// return;
				// }
				diag.setVisible(false);
				final ArrayList<File> toMergeFilesf = toMergeFiles;
				final Thread mergeThread = new Thread() {
					@Override
					public void run() {
						Ams7fsFile file7fs;
						if (toMergeFilesf.size() > 1) {
							// Merge7fsFilesDialog.mergeFiles(output, toMergeFilesf);
							file7fs = new Ams7fsFile(toMergeFilesf, amsFile);
							// file7fs.setDelete7fs(true);
						} else
							file7fs = new Ams7fsFile(toMergeFilesf.get(0).getAbsolutePath(), amsFile);
						file7fs.start();
					}
				};
				mergeThread.start();
				if (!tfa.getText().equals("")) {
					final Thread archiveThread = new Thread() {
						@Override
						public void run() {
							try {
								String pathName = tfa.getText();
								String sourceName = tf.getText();
								Path logDir = Paths.get(sourceName).getParent();
								Path sourcePath = Paths.get(sourceName);
								Files.walk(sourcePath)
										.forEach(source -> {
											Path destination = Paths.get(pathName, sourceDir[0], source.toString()
													.substring(sourceName.length()));
											try {
												String[] names = source.toString().split("_");
												if (logDir.endsWith("DATA")) {
													Files.walk(logDir, 1)
															.forEach(logFile -> {
																String logFilename = logFile.getFileName().toString();
																if (logFilename.startsWith("log")
																		&& logFilename.endsWith(".txt"))
																	for (String name : names) {
																		if (logFilename
																				.substring(logFilename.indexOf("_") + 1,
																						logFilename.lastIndexOf("."))
																				.equals(name)) {
																			Path logdestination = Paths.get(pathName,
																					sourceDir[0], logFilename);
																			try {
																				if (Files.notExists(logdestination))
																					Files.copy(logFile, logdestination);
																			} catch (IOException e) {
																				e.printStackTrace();
																			}
																			break;
																		}
																	}
															});
												}
												Files.copy(source, destination);
												File file = new File(source.toString());
												if (file.isFile()
														&& doChecksum32(source.toString(), destination.toString()))
													Files.delete(source);
											} catch (IOException e) {
												e.printStackTrace();
											}
										});
								if (isEmpty(sourcePath))
									Files.delete(sourcePath);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					archiveThread.start();
				}
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

	public void setVisible(boolean b) {
		diag.setVisible(b);
	}

	private static boolean doChecksum32(String fileName1, String fileName2) {
		try {
			CheckedInputStream cis = null;
			long fileSize1 = new File(fileName1).length();
			long fileSize2 = new File(fileName2).length();
			if (fileSize1 != fileSize2)
				return false;
			try {
				// Computer CRC32 checksum
				cis = new CheckedInputStream(
						new FileInputStream(fileName1), new CRC32());
			} catch (FileNotFoundException e) {
				System.err.println("File not found.");
				System.exit(1);
			}
			byte[] buf = new byte[128];
			while (cis.read(buf) >= 0) {
			}
			long checksum1 = cis.getChecksum().getValue();
			System.out.println(checksum1 + " " + fileSize1 + " " + fileName1);
			try {
				// Computer CRC32 checksum
				cis = new CheckedInputStream(
						new FileInputStream(fileName2), new CRC32());
			} catch (FileNotFoundException e) {
				System.err.println("File not found.");
				return false;
			}
			buf = new byte[128];
			while (cis.read(buf) >= 0) {
			}
			long checksum2 = cis.getChecksum().getValue();
			System.out.println(checksum2 + " " + fileSize2 + " " + fileName2);
			cis.close();
			return (checksum1 == checksum2);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean isEmpty(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (Stream<Path> entries = Files.list(path)) {
				return !entries.findFirst().isPresent();
			}
		}
		return false;
	}
}
