package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.TreeSet;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Generates 'subset' files for each recorded signal.
 * A subset file stores the largest and smallest value of
 * the original file for a given window size. In this case,
 * the windows sizes are powers of two, until the entire subset file
 * can fit in a BinaryFileDrawer buffer.
 * The resulting subset files can be used to draw large datasets quickly,
 * since we only need to draw a line between the maximum and the minimum value for each pixel
 * of the screen.
 */

public class SubSetFileGenerator extends Thread {

	private class ChannelSortedByFileSize implements Comparable<ChannelSortedByFileSize> {
		private String chan;
		private long fileSize;

		private ChannelSortedByFileSize(String chan, long fileSize) {
			this.chan = chan;
			this.fileSize = fileSize;
		}

		@Override
		public int compareTo(ChannelSortedByFileSize o) {
			if (fileSize > o.fileSize) {
				return 1;
			}
			if (fileSize < o.fileSize) {
				return -1;
			}
			return chan.compareTo(o.chan);
		}
	}

	final int SIZE = 1048576;

	final int SIZEDIV4 = SIZE / 4;
	final int SIZEDIV8 = SIZE / 8;
	private final ByteBuffer bb = ByteBuffer.allocateDirect(SIZE);
	private IntBuffer sb = bb.asIntBuffer();
	private final int[] sBuff = new int[SIZE / 4];
	private DoubleBuffer db = bb.asDoubleBuffer();
	private final double[] dBuff = new double[SIZE / 8];
	private final ByteBuffer bbout = ByteBuffer.allocateDirect(SIZE);
	private final IntBuffer sbout = bbout.asIntBuffer();
	private final DoubleBuffer dbout = bbout.asDoubleBuffer();

	private static SubSetFileGenerator instance;

	public static SubSetFileGenerator getInstance() {
		if (instance == null) {
			instance = new SubSetFileGenerator();
		}
		return instance;
	}

	public static SubSetFileGenerator getNewInstance() {
		if (instance != null) {
			// instance.interrupt();
			try {
				instance.join(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			instance = null;
		}
		instance = new SubSetFileGenerator();
		return instance;
	}

	private SubSetFileGenerator() {
		this.setPriority(MIN_PRIORITY);
	}

	public void generateSubSetFile(String s, File tempDir) {
		generateSubSetFile(s, tempDir, s);
	}

	public void generateSubSetFile(String s, File tempDir, String name) {
		CurrentOpenData cur = CurrentOpenData.getInstance();
		int maxPointsInView = AppSettings.getInstance().getIntProperty(Settings.MAXDISPLAYPOINTS);
		Timer chanTimer = new Timer();
		ArrayList<FileChannel> outs = new ArrayList<FileChannel>();
		ArrayList<FileOutputStream> foss = new ArrayList<FileOutputStream>();
		try {
			bbout.rewind();
			bbout.limit(bbout.capacity());
			chanTimer.start();
			boolean isDouble = false;
			File inFile = new File(cur.getFilePath(), name + ".bin");
			if (!inFile.exists()) {
				isDouble = true;
				inFile = new File(cur.getFilePath(), name + ".dbin");
			}
			long nShortsInFile = inFile.length() / 4;
			int minSkip = (int) (1 + nShortsInFile / maxPointsInView);
			int maxSkip = Utils.GetNextPowerOfTwo(minSkip);
			int nSkips = (int) (Math.log(maxSkip) / Math.log(2));
			int curVal;
			double curVald;
			long nSamples;
			long counter = 0;
			if (nSkips > 0 && !isDouble) {
				int[] skips = new int[nSkips];
				int[] skips2 = new int[nSkips];
				int[] mins = new int[nSkips];
				int[] maxs = new int[nSkips];
				int[][] vals = new int[nSkips][SIZE / 4];
				int[] nvals = new int[nSkips];
				outs.clear();
				for (int i = 0; i < nSkips; i++) {
					skips[i] = (1 << (i + 1));
					skips2[i] = 2 * skips[i];
					File skipFile = new File(tempDir, inFile.getName() + "-" + skips[i] + ".tmp");
					foss.add(new FileOutputStream(skipFile));
					outs.add((foss.get(i)).getChannel());

					mins[i] = Integer.MAX_VALUE;
					maxs[i] = Integer.MIN_VALUE;
					nvals[i] = 0;
				}
				FileInputStream fis = new FileInputStream(inFile);
				byte[] buf = new byte[(int) inFile.length()];
				fis.read(buf);
				ByteBuffer inBuffer = ByteBuffer.wrap(buf);
				fis.close();
				sb = inBuffer.asIntBuffer();

				int nRead = sBuff.length;
				int i, q;
				long j;
				nSamples = inFile.length() / 4;
				int nS = 0;
				for (j = 0; j < nSamples; j += nS) {

					if (isInterrupted()) {
						for (FileChannel o : outs)
							o.close();
						for (FileOutputStream f : foss)
							f.close();
						return;
					}
					if (sb.capacity() - sb.position() < nRead / 4)
						nS = sb.capacity() - sb.position();
					else
						nS = nRead / 4;
					if (nRead < 1)
						break;
					sb.get(sBuff, 0, nS);
					for (q = 0; q < nS; q++) {
						curVal = sBuff[q];

						if (curVal > maxs[0])
							maxs[0] = curVal;
						if (curVal < mins[0])
							mins[0] = curVal;

						for (i = 0; i < nSkips; i++) {
							if (counter % (skips2[i]) == skips2[i] - 1) {
								vals[i][nvals[i]] = mins[i];
								nvals[i]++;
								vals[i][nvals[i]] = maxs[i];
								nvals[i]++;
								if (nvals[i] == SIZEDIV4) {
									bbout.position(0);
									sbout.position(0);
									sbout.put(vals[i]);
									outs.get(i).write(bbout);
									nvals[i] = 0;

								}
								if (i < nSkips - 1) {
									if (maxs[i] > maxs[i + 1])
										maxs[i + 1] = maxs[i];
									if (mins[i] < mins[i + 1])
										mins[i + 1] = mins[i];
								}
								mins[i] = Integer.MAX_VALUE;
								maxs[i] = Integer.MIN_VALUE;
							} else {
								break;
							}
						}
						counter++;
					}
				}

				for (i = 0; i < nSkips; i++) {
					if (nvals[i] > 0) {
						bbout.rewind();
						sbout.position(0);
						sbout.put(vals[i], 0, nvals[i]);
						bbout.limit(4 * nvals[i]);
						outs.get(i).write(bbout);
						nvals[i] = 2;
						vals[i][0] = mins[i];
						vals[i][1] = maxs[i];
					}
					if (nSamples / skips[i] != outs.get(i).size() / 4) {
						bbout.rewind();
						sbout.position(0);
						sbout.put(vals[i], 0, nvals[i]);
						bbout.limit(4 * nvals[i]);
						outs.get(i).write(bbout);
						nvals[i] = 0;
					}
				}
				for (FileChannel o : outs)
					o.close();
				for (i = 0; i < nSkips; i++) {
					File skipFile = new File(tempDir, inFile.getName() + "-" + skips[i] + ".tmp");
					File destFile = new File(tempDir, inFile.getName() + "-" + skips[i]);
					skipFile.renameTo(destFile);
				}
			}
			nShortsInFile = inFile.length() / 8;
			minSkip = (int) (1 + nShortsInFile / maxPointsInView);
			maxSkip = Utils.GetNextPowerOfTwo(minSkip);
			nSkips = (int) (Math.log(maxSkip) / Math.log(2));
			if (nSkips > 0 && isDouble && inFile.exists()) {
				int[] skips = new int[nSkips];
				int[] skips2 = new int[nSkips];
				double[] mins = new double[nSkips];
				double[] maxs = new double[nSkips];
				double[][] vals = new double[nSkips][SIZE / 8];
				int[] nvals = new int[nSkips];
				outs.clear();
				for (int i = 0; i < nSkips; i++) {
					skips[i] = (1 << (i + 1));
					skips2[i] = 2 * skips[i];
					File skipFile = new File(tempDir, inFile.getName() + "-" + skips[i] + ".tmp");
					foss.add(new FileOutputStream(skipFile));
					outs.add((foss.get(i)).getChannel());

					mins[i] = Integer.MAX_VALUE;
					maxs[i] = Integer.MIN_VALUE;
					nvals[i] = 0;
				}
				FileInputStream fis = new FileInputStream(inFile);
				byte[] buf = new byte[(int) inFile.length()];
				fis.read(buf);
				ByteBuffer inBuffer = ByteBuffer.wrap(buf);
				fis.close();
				db = inBuffer.asDoubleBuffer();

				int nRead = dBuff.length;
				int i, q;
				long j;
				nSamples = inFile.length() / 8;
				int nS = 0;
				for (j = 0; j < nSamples; j += nS) {

					if (isInterrupted()) {
						for (FileChannel o : outs)
							o.close();
						for (FileOutputStream f : foss)
							f.close();
						return;
					}
					if (db.capacity() - db.position() < nRead / 8)
						nS = db.capacity() - db.position();
					else
						nS = nRead / 8;
					if (nRead < 1)
						break;
					db.get(dBuff, 0, nS);
					for (q = 0; q < nS; q++) {
						curVald = dBuff[q];

						if (curVald > maxs[0])
							maxs[0] = curVald;
						if (curVald < mins[0])
							mins[0] = curVald;

						for (i = 0; i < nSkips; i++) {
							if (counter % (skips2[i]) == skips2[i] - 1) {
								vals[i][nvals[i]] = mins[i];
								nvals[i]++;
								vals[i][nvals[i]] = maxs[i];
								nvals[i]++;
								if (nvals[i] == SIZEDIV8) {
									bbout.position(0);
									dbout.position(0);
									dbout.put(vals[i]);
									outs.get(i).write(bbout);
									nvals[i] = 0;

								}
								if (i < nSkips - 1) {
									if (maxs[i] > maxs[i + 1])
										maxs[i + 1] = maxs[i];
									if (mins[i] < mins[i + 1])
										mins[i + 1] = mins[i];
								}
								mins[i] = Integer.MAX_VALUE;
								maxs[i] = Integer.MIN_VALUE;
							} else {
								break;
							}
						}
						counter++;
					}
				}

				for (i = 0; i < nSkips; i++) {
					if (nvals[i] > 0) {
						bbout.rewind();
						dbout.position(0);
						dbout.put(vals[i], 0, nvals[i]);
						bbout.limit(8 * nvals[i]);
						outs.get(i).write(bbout);
						nvals[i] = 2;
						vals[i][0] = mins[i];
						vals[i][1] = maxs[i];
					}
					if (nSamples / skips[i] != outs.get(i).size() / 8) {
						bbout.rewind();
						dbout.position(0);
						dbout.put(vals[i], 0, nvals[i]);
						bbout.limit(8 * nvals[i]);
						outs.get(i).write(bbout);
						nvals[i] = 0;
					}
				}
				for (FileChannel o : outs)
					o.close();
				for (i = 0; i < nSkips; i++) {
					File skipFile = new File(tempDir, inFile.getName() + "-" + skips[i] + ".tmp");
					File destFile = new File(tempDir, inFile.getName() + "-" + skips[i]);
					skipFile.renameTo(destFile);
				}
			}
			if (!inFile.exists())
				System.out.println("SubsetFileGenerator: infile for channel " + name + " not found.");
			// MainFrame.getInstance().getMainFrame().repaint();
			// InspectTab iTab = InspectTab.getInstanceOrNull();
			// if (iTab != null)
			// iTab.autoscale();
			// InspectTab.getInstance().getXAxis().autoscaleConnectedGraphs();
			// QRSTab.getInstance().getXAxisECG().autoscaleConnectedGraphs();
			// LabelTab.getInstance().getXAxis().autoscaleConnectedGraphs();

			if (isInterrupted())
				return;

			chanTimer.stop();
		} catch (IOException e) {
			for (FileChannel o : outs) {
				try {
					o.close();
				} catch (IOException e1) {
				}
				for (FileOutputStream f : foss)
					try {
						f.close();
					} catch (IOException e1) {
					}
			}
		}
	}

	@Override
	public void run() {
		CurrentOpenData cur = CurrentOpenData.getInstance();
		File tempDir = new File(cur.getFilePath(), "tmp");
		tempDir.mkdirs();

		Timer totalTimer = new Timer();
		totalTimer.start();

		if (CurrentOpenData.getInstance().channelExists("DZ")) {
			try {
				generateSubSetFile("DZ", tempDir, "FILTDZ");
				generateSubSetFile("DZ", tempDir, "DZRESP");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (CurrentOpenData.getInstance().channelExists("DZDT")) {
			try {
				generateSubSetFile("DZDT", tempDir, "FILTDZDT");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String[] filteredChans = { "magX", "magY", "magZ", "MXR", "MYR", "MZR", "GyroX", "GyroY", "GyroZ", "ECG", "SCL",
				"V2ecg", "V3ecg", "StepInstances", "P_sc", "T_sc" };
		for (String chan : filteredChans)
			if (CurrentOpenData.getInstance().channelExists(chan)) {
				try {
					generateSubSetFile(chan, tempDir, "FILT" + chan);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		TreeSet<ChannelSortedByFileSize> srtChannels = new TreeSet<ChannelSortedByFileSize>();
		for (Ams7fsChannelInfo s : CurrentOpenData.getInstance().getChannelInfo()) {
			File inFile = new File(cur.getFilePath(), s.getSzID() + ".bin");
			srtChannels.add(new ChannelSortedByFileSize(s.getSzID(), inFile.length()));
			if (s.getTickFile() != null) {
				inFile = new File(cur.getFilePath(), s.getTickFile() + ".bin");
				srtChannels.add(new ChannelSortedByFileSize(s.getTickFile(), inFile.length()));
			}
		}
		for (ChannelSortedByFileSize chan : srtChannels) {
			String s = chan.chan;
			generateSubSetFile(s, tempDir);
		}

		generateSubSetFile("AVX", tempDir);
		generateSubSetFile("AVY", tempDir);
		generateSubSetFile("AVZ", tempDir);

		totalTimer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("SubsetFileGenerator took (" + totalTimer.getTime() / 1000. + " sec)");

		InspectTab.getInstance().autoscale();
	}
}
