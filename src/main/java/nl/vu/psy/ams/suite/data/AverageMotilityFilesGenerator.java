package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Generates average motility signals from raw motility singals,
 * similar to what the AMS Device does with the MYA signal:
 * We first put the raw signal through a highpass filter, to
 * remove de DC component of the motility signal (which is the
 * orientation of the device). Then we average the absolute value
 * of the resulting signal to create the average motility signal.
 */
public class AverageMotilityFilesGenerator {

	public static final int X_CHAN = 0;
	public static final int Y_CHAN = 1;
	public static final int Z_CHAN = 2;

	public static boolean hasBeenGenerated = false;

	public static void clear() {
		hasBeenGenerated = false;
	}

	public static void generateAverageMotFile(int chan) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		String chanString = null;
		File outFile = null;
		switch (chan) {
			case X_CHAN:
				if (cod.channelExists("XMT")) {
					chanString = "XMT";
				} else if (cod.channelExists("MXR")) {
					chanString = "MXR";
				}
				outFile = new File(cod.getFilePath(), "AVX.dbin");
				break;
			case Y_CHAN:
				if (cod.channelExists("YMT")) {
					chanString = "YMT";
				} else if (cod.channelExists("MYR")) {
					chanString = "MYR";
				}
				outFile = new File(cod.getFilePath(), "AVY.dbin");
				break;
			case Z_CHAN:
				if (cod.channelExists("ZMT")) {
					chanString = "ZMT";
				} else if (cod.channelExists("MZR")) {
					chanString = "MZR";
				}
				outFile = new File(cod.getFilePath(), "AVZ.dbin");
				break;
			default:
				return;
		}
		if (chanString == null)
			return;
		if (outFile.exists())
			return;

		cod.dirtyFiles.add(outFile);
		BinaryFile bf = new BinaryFile(chanString);
		double[] b = null;
		double[] a = null;

		long sampleTimeInUS = bf.getSampleTimeInUS();

		/*----- Written by Menaka---------------------
		 Remove all the frequencies below 1 Hz (This will remove all the static components in the signal reuslting only in movement data)
		[b a ] = butter(4, 0.002, 'high'); - High Pass Filter
		//----------------------------------------------*/

		if (sampleTimeInUS == 1000) {
			b = new double[] { 0.99182421200053306, -3.9672968480021322, 5.9509452720031986, -3.9672968480021322,
					0.99182421200053306 };
			a = new double[] { 1, -3.9835812586585209, 5.9508784292666981, -3.9510124365728321, 0.98371526751047855 };
		} else if (sampleTimeInUS == 2000) {
			b = new double[] { 9.837151741297564000e-001, -3.934860696519025600e+000, 5.902291044778538200e+000,
					-3.934860696519025600e+000,
					9.837151741297564000e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.967162595948848100e+000, 5.902025861490878700e+000,
					-3.902558784823238400e+000,
					9.676955438131369400e-001 };
		} else if (sampleTimeInUS == 4000) {
			b = new double[] { 9.676948088896716300e-001, -3.870779235558686500e+000, 5.806168853338030000e+000,
					-3.870779235558686500e+000,
					9.676948088896716300e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.934325820798736800e+000, 5.805125421055139500e+000,
					-3.807232457228851200e+000,
					9.364332431520188100e-001 };
		} else if (sampleTimeInUS == 10000) {
			b = new double[] { 9.211709934999421400e-001, -3.684683973999768500e+000, 5.527025960999653300e+000,
					-3.684683973999768500e+000,
					9.211709934999421400e-001 };
			a = new double[] { 1.000000000000000000e+000, -3.835825540647348900e+000, 5.520819136622231200e+000,
					-3.533535219463018100e+000,
					8.485559992664779600e-001 };
		} else if (sampleTimeInUS == 100000) {
			b = new double[] { 4.328466449902916800e-001, -1.731386579961166700e+000, 2.597079869941750100e+000,
					-1.731386579961166700e+000,
					4.328466449902916800e-001 };
			a = new double[] { 1.000000000000000000e+000, -2.369513007182036700e+000, 2.313988414415878200e+000,
					-1.054665405878566500e+000,
					1.873794923681847400e-001 };
		}

		if (b == null || a == null) {
			try {
				bf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		File file = new File(cod.getFilePath(), chanString + ".bin");

		double av = 0;

		int size = 1048576;
		ByteBuffer bb = ByteBuffer.allocateDirect(size);
		IntBuffer sb = bb.asIntBuffer();

		double[] backBuf = new double[4];
		double[] forBuf = new double[4];

		int[] samp = new int[size / 4];
		boolean firstrun = true;
		int nRead = size;

		long nvals = 0;

		long fileSize = file.length();

		int nValsIn1Sec = (int) Math.round(1000000. / sampleTimeInUS);
		int nAvs = (int) (fileSize / (4 * nValsIn1Sec));
		double[] outVals = new double[nAvs];

		int iOut = 0;

		try {
			FileInputStream fis = new FileInputStream(file);
			// byte[] buf = new byte[(int) fileSize];
			// fis.read(buf);
			// ByteBuffer inBuffer = ByteBuffer.wrap(buf);
			// fis.close();
			// sb = inBuffer.asIntBuffer();
			FileOutputStream fos = new FileOutputStream(outFile);
			FileChannel ifC = fis.getChannel();
			long pos = 0;

			for (long q = 0; q < fileSize; q += nRead) {
				bb.position(0);
				sb.position(0);
				long newPos = pos;
				pos += size;
				if (pos > fileSize)
					pos = fileSize;
				nRead = (int) (pos - newPos);
				ifC.read(bb, newPos);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				for (int j = 0; j < nSRead; j++) {
					double val = bf.getRealValueFromSampleValue(samp[j]);
					if (firstrun) {
						for (int i = 0; i < 4; i++) {
							backBuf[i] = val;
						}
						for (int i = 0; i < 4; i++) {
							forBuf[i] = val;
						}
						firstrun = false;
					}
					double newVal = b[0] * val;
					for (int i = 0; i < 4; i++) {
						newVal += b[i + 1] * backBuf[i];
					}
					for (int i = 0; i < 4; i++) {
						newVal -= a[i + 1] * forBuf[i];
					}
					for (int i = 0; i < 3; i++) {
						backBuf[3 - i] = backBuf[2 - i];
					}
					backBuf[0] = val;
					for (int i = 0; i < 3; i++) {
						forBuf[3 - i] = forBuf[2 - i];
					}
					forBuf[0] = newVal;

					av += Math.abs(newVal);
					nvals++;

					if (nvals == nValsIn1Sec) {
						av /= nvals;
						outVals[iOut] = av;
						iOut++;
						nvals = 0;
						av = 0;
					}
				}
			}
			byte[] outbuf = new byte[8 * nAvs];
			ByteBuffer outBB = ByteBuffer.wrap(outbuf);
			DoubleBuffer outDB = outBB.asDoubleBuffer();
			outDB.put(outVals);
			fos.write(outbuf);
			fos.close();
			ifC.close();
			fis.close();
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(outFile);
			ssf1.start();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void generateAvMotFiles() {
		if (hasBeenGenerated)
			return;
		hasBeenGenerated = true;
		Timer tim = new Timer();
		tim.start();
		JFrame frame = MainFrame.getInstance().getMainFrame();
		ProgressMonitor prog = new ProgressMonitor(frame, "Generating average motility files", null, 0, 4);
		prog.setProgress(1);
		generateAverageMotFile(X_CHAN);
		prog.setProgress(2);
		generateAverageMotFile(Y_CHAN);
		prog.setProgress(3);
		generateAverageMotFile(Z_CHAN);
		prog.setProgress(4);
		prog.close();
		/*
		 * frame.toFront();
		 * frame.requestFocus();
		 */
		tim.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("generateAvmotFiles took (" + tim.getTime() / 1000. + " sec)");
	}

}
