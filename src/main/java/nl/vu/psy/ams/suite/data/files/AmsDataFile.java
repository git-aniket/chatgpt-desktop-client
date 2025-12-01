package nl.vu.psy.ams.suite.data.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
// import nl.vu.psy.ams.suite.data.SubsetFilesSingle;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.ExternalFilePanel;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.DataCompressor;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/*
 * Class that opens a amsdata, converts the contents
 * to the VU-DAMS format and copies the result to a
 * unique temporary directory.
 * This class can also save a temporary directory
 * to a new .amsdata(i) file, or quickly save changable
 * data (beat files etc) to an existing .amsdata(i) file. 
 * 
 * AmsData files are structured:
 * 
 * "AMSDATA" (String)
 * File version (long)
 * Length of first zipped block (long) 
 * Zipped Block of large files that will not change (= signal data)
 * Zipped Block of changeable files (= beat files, labels, etc)
 * 
 * The 'Length of first zipped block' value can be used to quickly save only the 
 * changeable files, keeping the first zipped block untouched.
 */
public class AmsDataFile extends Thread {

	private static Logger logger = LogManager.getLogger(AmsDataFile.class.getName());

	private boolean saveFile;
	private boolean isCompressed;
	private String filePath;

	private boolean showProgress = true;

	private boolean forceResave = false;

	private boolean quick = false;

	private static final short FILEVERSION = 4;
	private boolean batchexport = false;
	private int typeofanalysis = 0;

	public AmsDataFile(boolean saveFile, String filePath) {
		this.saveFile = saveFile;
		this.filePath = filePath;
	}

	public AmsDataFile(boolean saveFile, String filePath, boolean batch, int typeofanalysis) {
		this.saveFile = saveFile;
		this.filePath = filePath;
		this.batchexport = batch;
		setTypeofAnalysis(typeofanalysis);
	}

	private ArrayList<File> getStaticOutFiles(File path) {
		ArrayList<File> retArray = new ArrayList<File>();
		String ext;
		if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
			ext = ".comp";
		else
			ext = ".icomp";
		FilenameFilter binFilefilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".bin")) {
					return true;
				} else {
					return false;
				}
			}
		};
		String binFilesList[] = path.list(binFilefilter);
		for (String fileName : binFilesList) {
			final int index = fileName.lastIndexOf('.'); // used the String.lastIndexOf() method
			String outName;
			if (index == -1) {
				outName = fileName + ext;
			} else {
				outName = fileName.substring(0, index) + ext;
			}
			retArray.add(new File(path, outName));
		}

		FilenameFilter dbinFilefilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".dbin")) {
					return true;
				} else {
					return false;
				}
			}
		};
		String dbinFilesList[] = path.list(dbinFilefilter);
		for (String fileName : dbinFilesList)
			retArray.add(new File(path, fileName));

		return retArray;
	}

	private ArrayList<File> getTempOutFiles(File path) {
		ArrayList<File> retArray = new ArrayList<File>();

		retArray.add(new File(path, "outputdata.txt"));

		FilenameFilter jsonFilefilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".json")) {
					return true;
				} else {
					return false;
				}
			}
		};
		String jsonFilesList[] = path.list(jsonFilefilter);
		for (String fileName : jsonFilesList)
			retArray.add(new File(path, fileName));

		if (ExternalFilePanel.getInstance().isFile1Loaded()) {
			retArray.add(new File(path, "extfile.dat"));
		}
		if (ExternalFilePanel.getInstance().isFile2Loaded()) {
			retArray.add(new File(path, "extfile2.dat"));
		}
		if (ExternalFilePanel.getInstance().isFile3Loaded()) {
			retArray.add(new File(path, "extfile3.dat"));
		}
		if (new File(path, "Actigraph_Motility.dat").exists()) {
			retArray.add(new File(path, "Actigraph_Motility.dat"));
		}

		return retArray;
	}

	public boolean isCompressed() {
		return isCompressed;
	}

	private void OpenFile(File tempDir) throws IOException {
		int size = 1048576;
		InputStream is = new BufferedInputStream(new FileInputStream(filePath));
		DataInputStream dis = new DataInputStream(is);
		/* String header = new String(); */
		for (int i = 0; i < 7; i++)
			dis.readChar();
		short fileVersion = dis.readShort();
		long staticFileLength = dis.readLong();
		dis.close();

		if (12 * staticFileLength > Utils.getFreeBytesInTemporaryDirectory()) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Not enough free space in temporary directory!\nCurrent space available: "
							+ Utils.getFreeBytesInTemporaryDirectory() / 1000000 + " MB\nSpace needed: "
							+ 12 * staticFileLength / 1000000 + " MB",
					"Not enough free space!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JFrame frame = MainFrame.getInstance().getMainFrame();
		is = new ProgressMonitorInputStream(frame, "Opening AMS Data file", new BufferedInputStream(new FileInputStream(
				filePath)));
		is.skip(24);
		ZipInputStream zin = new ZipInputStream(is);
		BufferedOutputStream out;
		File tempDir2 = new File(tempDir, "tmp");
		tempDir2.mkdirs();

		byte[] buffer = new byte[size];
		int nBytesRead;
		ZipEntry entry;
		try {
			do {
				entry = zin.getNextEntry();
				if (entry != null) {
					String note = "Extracting " + entry.getName();
					logger.info(note);
					((ProgressMonitorInputStream) is).getProgressMonitor().setNote(note);
					out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, entry.getName())), 10 * size);
					while ((nBytesRead = zin.read(buffer)) > -1) {
						out.write(buffer, 0, nBytesRead);
					}
					out.close();
					zin.closeEntry();
					if (Utils.getExtension(new File(tempDir, entry.getName())).equals("comp")) {
						DataCompressor comp = new DataCompressor();
						File inFile = new File(tempDir, entry.getName());
						File outFile = new File(tempDir, Utils.removeExtension(entry.getName()) + ".bin");
						comp.decompress(inFile, outFile, false);
						inFile.delete();
						// SubsetFilesSingle ssf1 = new SubsetFilesSingle(outFile, tempDir2);
						// ssf1.start();
					}
					if (Utils.getExtension(new File(tempDir, entry.getName())).equals("icomp")) {
						DataCompressor comp = new DataCompressor();
						File inFile = new File(tempDir, entry.getName());
						File outFile = new File(tempDir, Utils.removeExtension(entry.getName()) + ".bin");
						comp.decompress(inFile, outFile, true);
						inFile.delete();
						// SubsetFilesSingle ssf1 = new SubsetFilesSingle(outFile, tempDir2);
						// ssf1.start();
					}
				}
			} while (entry != null);
			zin.close();

			is = new ProgressMonitorInputStream(frame, "Opening AMS Data file",
					new BufferedInputStream(new FileInputStream(
							filePath)));
			is.skip(staticFileLength);
			zin = new ZipInputStream(is);
			do {
				entry = zin.getNextEntry();
				if (entry != null) {
					String note = "Extracting " + entry.getName();
					logger.info(note);
					((ProgressMonitorInputStream) is).getProgressMonitor().setNote(note);
					out = new BufferedOutputStream(new FileOutputStream(new File(tempDir, entry.getName())), 10 * size);
					try {
						while ((nBytesRead = zin.read(buffer, 0, size)) > -1) {
							out.write(buffer, 0, nBytesRead);
						}
					} catch (Exception e) {
						logger.error("Exception in opening " + entry.getName());
					} finally {
						out.close();
					}
					zin.closeEntry();
					if (Utils.getExtension(new File(tempDir, entry.getName())).equals("comp")) {
						DataCompressor comp = new DataCompressor();
						File inFile = new File(tempDir, entry.getName());
						File outFile = new File(tempDir, Utils.removeExtension(entry.getName()) + ".bin");
						comp.decompress(inFile, outFile, false);
						inFile.delete();
					}
					if (Utils.getExtension(new File(tempDir, entry.getName())).equals("icomp")) {
						DataCompressor comp = new DataCompressor();
						File inFile = new File(tempDir, entry.getName());
						File outFile = new File(tempDir, Utils.removeExtension(entry.getName()) + ".bin");
						comp.decompress(inFile, outFile, true);
						inFile.delete();
					}
				}
			} while (entry != null);
		} catch (

		Exception e) {
			logger.error("Exception in opening zip");
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Cannot unzip amsdata(i) file. If this is a downloaded file try downloading it again; it is incomplete",
					"Error opening amsdata(i) file!", JOptionPane.ERROR_MESSAGE);
		} finally {
			zin.close();
			is.close();
			CurrentOpenData.getInstance().setDataFile(new File(filePath), fileVersion);
		}
		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */

		logger.info("Extracting finished.");
	}

	@Override
	public void run() {
		MainFrame.getInstance().getMainFrame().setEnabled(false);
		if (saveFile == true) {
			logger.info("Save Ams Data start");
			try {
				SaveFile();
			} catch (IOException e) {
				e.printStackTrace();
				(new File(filePath)).delete();
				logger.error("Save AMS data file error", e);
			}
			logger.info("Save Ams Data stop");
		} else {
			try {
				logger.info("Load Ams Data start");
				Timer timer = new Timer();
				timer.start();
				File filePath = Utils.getUniqueTemporaryDirectory();
				OpenFile(filePath);
				timer.stop();
				if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
					System.out.println("Load AMSdata File took (" + timer.getTime() / 1000. + " sec)");
				if (filePath != null)
					CurrentOpenData.getInstance().Open(filePath, quick, batchexport, typeofanalysis);
				logger.info("Load Ams Data stop");
			} catch (InterruptedIOException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		MainFrame.getInstance().getMainFrame().setEnabled(true);
	}

	private void SaveChangeableData(File filePath) throws IOException {
		SaveChangeableData(filePath, null);
	}

	private void SaveChangeableData(File filePath, ArrayList<File> filtFiles) throws IOException {

		File dataPath = CurrentOpenData.getInstance().getFilePath();
		ArrayList<File> tempOutFiles = getTempOutFiles(dataPath);
		if (filtFiles != null) {
			tempOutFiles.addAll(filtFiles);
		}

		int size = 1048576;

		RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
		for (int i = 0; i < 7; i++)
			raf.readChar();
		raf.readShort();
		long staticFileLength = raf.readLong();
		raf.setLength(staticFileLength);
		raf.close();
		BufferedInputStream bis;
		ZipEntry entry;
		byte[] buffer = new byte[1048576];
		int bytesRead;

		CRC32 crc = new CRC32();
		ZipOutputStream zos = new ZipOutputStream(
				new BufferedOutputStream(new FileOutputStream(filePath, true), 10 * size));
		for (File f : tempOutFiles) {
			try {
				bis = new BufferedInputStream(new FileInputStream(f));
				entry = new ZipEntry(f.getName());
				if (isCompressed == false) {
					crc.reset();
					while ((bytesRead = bis.read(buffer)) != -1) {
						crc.update(buffer, 0, bytesRead);
					}
					entry.setMethod(ZipEntry.STORED);
					entry.setCompressedSize(f.length());
					entry.setCrc(crc.getValue());
					bis.close();
					bis = new BufferedInputStream(new FileInputStream(f));
				}
				zos.putNextEntry(entry);
				while ((bytesRead = bis.read(buffer)) != -1) {
					zos.write(buffer, 0, bytesRead);
				}
				bis.close();
			} catch (Exception e) {
				logger.error("File " + f.getName() + " not saved because of the following error: " + e.getMessage());
				// keep going to prevent corrupted files
				continue;
			}
		}

		zos.close();
	}

	public void SaveFile() throws IOException {
		CurrentOpenData.getInstance().getFileHeader().setAmsDataFileVersion(Utils.getVersionString());
		CurrentOpenData.getInstance().saveChangeablesToDisk();
		CurrentOpenData.getInstance().setDirty(false);

		Timer timer = new Timer();
		timer.start();

		if (filePath == null) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Invalid file - Please give a valid file to save work to!", "Invalid file",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		File baseDir = new File(filePath).getParentFile();
		if (baseDir.exists() == false) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Invalid file - Please give a valid file to save work to!", "Invalid file",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		int size = 1048576;

		File dataPath = CurrentOpenData.getInstance().getFilePath();
		ArrayList<File> outFiles, retArray;
		if (forceResave || Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("5fs")
				|| Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")) {
			outFiles = getStaticOutFiles(dataPath);

			FilenameFilter binFilefilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.endsWith(".bin")) {
						return true;
					} else {
						return false;
					}
				}
			};
			retArray = new ArrayList<File>();
			String binFilesList[] = dataPath.list(binFilefilter);
			for (String fileName : binFilesList)
				retArray.add(new File(dataPath, fileName));
		} else {
			outFiles = CurrentOpenData.getInstance().dirtyFiles;
			ArrayList<File> toRemove = new ArrayList<File>();
			ArrayList<File> toAdd = new ArrayList<File>();
			String ext;
			if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
					&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
				ext = ".comp";
			else
				ext = ".icomp";
			retArray = new ArrayList<File>();
			for (File f : outFiles) {
				final int index = f.getName().lastIndexOf('.'); // used the String.lastIndexOf() method
				String oldExt = f.getName().substring(index);
				if (oldExt.equals(".bin")) {
					String fileName = f.getName().substring(0, index);
					toRemove.add(f);
					toAdd.add(new File(dataPath, fileName + ext));
					retArray.add(f);
				}
			}
			outFiles.removeAll(toRemove);
			outFiles.addAll(toAdd);
		}
		ProgressMonitor progress = null;
		JFrame frame = MainFrame.getInstance().getMainFrame();
		if (showProgress)
			progress = new ProgressMonitor(frame, "Precompressing Data Files", null, 0, retArray.size());
		if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7) {
			DataCompressor comp = new DataCompressor();
			int prog = 0;
			for (File inFile : retArray) {
				final int index = inFile.getName().lastIndexOf('.'); // used the String.lastIndexOf() method
				String outName;
				outName = inFile.getName().substring(0, index);
				File outFile = new File(dataPath, outName + ".comp");
				comp.compress(inFile, outFile, false);
				prog++;
				if (progress != null)
					progress.setProgress(prog);
			}
		} else {
			DataCompressor comp = new DataCompressor();
			int prog = 0;
			for (File inFile : retArray) {
				final int index = inFile.getName().lastIndexOf('.'); // used the String.lastIndexOf() method
				String outName;
				outName = inFile.getName().substring(0, index);
				File outFile = new File(dataPath, outName + ".icomp");
				comp.compress(inFile, outFile, true);
				prog++;
				if (progress != null)
					progress.setProgress(prog);
			}
		}

		if (progress != null) {
			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
		}
		int totalFileLength = Integer.MIN_VALUE;
		for (File f : outFiles)
			totalFileLength += f.length();

		if (showProgress)
			progress = new ProgressMonitor(frame, "Saving AMS Data file", null, Integer.MIN_VALUE, totalFileLength);
		int fileLengthDone = Integer.MIN_VALUE;
		if (forceResave || Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("5fs")
				|| Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")) {
			long totalFileSize = Utils.getTotalFileSize(outFiles);
			if (totalFileSize > (new File(filePath)).getParentFile().getFreeSpace()) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
						"Not enough free space in output directory!\nCurrent space available: "
								+ (new File(filePath)).getParentFile().getFreeSpace() / 1000000 + " MB\nSpace needed: "
								+ totalFileSize / 1000000 + " MB",
						"Not enough free space!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(filePath), 10 * size));
			dos.writeChars("AMSDATA");
			dos.writeShort(FILEVERSION);
			dos.writeLong(0); // Temporary placeholder
			dos.close();

			ZipOutputStream zos = new ZipOutputStream(
					new BufferedOutputStream(new FileOutputStream(filePath, true), 10 * size));
			BufferedInputStream bis;
			ZipEntry entry;
			byte[] buffer = new byte[size];
			int bytesRead;

			CRC32 crc = new CRC32();

			for (File f : outFiles) {
				if (progress != null) {
					progress.setProgress(fileLengthDone);
				}
				try {
					bis = new BufferedInputStream(new FileInputStream(f));
					entry = new ZipEntry(f.getName());
					if (isCompressed == false) {
						crc.reset();
						while ((bytesRead = bis.read(buffer)) != -1) {
							crc.update(buffer, 0, bytesRead);
						}
						entry.setMethod(ZipEntry.STORED);
						entry.setCompressedSize(f.length());
						entry.setCrc(crc.getValue());
						bis.close();
						bis = new BufferedInputStream(new FileInputStream(f));
					}
					zos.putNextEntry(entry);
					while ((bytesRead = bis.read(buffer)) != -1) {
						zos.write(buffer, 0, bytesRead);
						fileLengthDone += bytesRead;
						if (progress != null)
							progress.setProgress(fileLengthDone);
					}
					bis.close();
				} catch (Exception e) {
					logger.error(
							"File " + f.getName() + " not saved because of the following error: " + e.getMessage());
					// keep going to prevent corrupted files
					continue;
				}
			}
			zos.close();
			long staticFileLength = (new File(filePath)).length();
			RandomAccessFile raf = new RandomAccessFile(new File(filePath), "rw");
			raf.writeChars("AMSDATA");
			raf.writeShort(FILEVERSION);
			raf.writeLong(staticFileLength);
			raf.close();
			if (progress != null) {
				progress.close();
				/*
				 * frame.toFront();
				 * frame.requestFocus();
				 */
			}

			SaveChangeableData(new File(filePath));
		} else {
			long staticFileLength = (new File(filePath)).length();
			RandomAccessFile raf = new RandomAccessFile(new File(filePath), "rw");
			raf.writeChars("AMSDATA");
			raf.writeShort(FILEVERSION);
			raf.writeLong(staticFileLength);
			raf.close();
			if (progress != null) {
				progress.close();
				/*
				 * frame.toFront();
				 * frame.requestFocus();
				 */
			}

			SaveChangeableData(new File(filePath), outFiles);
		}

		CurrentOpenData.getInstance().setDataFile(new File(filePath), FILEVERSION);
		CurrentOpenData.getInstance().dirtyFiles.clear();
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("Save AMSdata file took (" + timer.getTime() / 1000. + " sec)");

		if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7) {
			for (Ams7fsChannelInfo chan : CurrentOpenData.getInstance().getChannelInfo()) {
				if (chan.getSzID().equals("DZDT") == false) {
					File outFile = new File(dataPath, chan.getSzID() + ".comp");
					outFile.delete();
				}
			}
		} else {
			for (Ams7fsChannelInfo chan : CurrentOpenData.getInstance().getChannelInfo()) {
				if (chan.getSzID().equals("DZDT") == false) {
					File outFile = new File(dataPath, chan.getSzID() + ".icomp");
					outFile.delete();
				}
			}

		}
	}

	public void setCompressed(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}

	public void setForceReSave(boolean b) {
		this.forceResave = b;
	}

	public void setQuick(boolean quick) {
		this.quick = quick;
	}

	public void setShowProgress(boolean showProgress) {
		this.showProgress = showProgress;
	}

	public void setTypeofAnalysis(int type) {
		this.typeofanalysis = type;
	}

}
