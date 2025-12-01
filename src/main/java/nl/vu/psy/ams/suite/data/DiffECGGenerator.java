package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Generates difference between two (ECG) signals
 */
public class DiffECGGenerator {
	public static void GenerateECGDiff() {
		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File ecg1File = new File(tempDir, "ECG.bin");
			File ecg2File = new File(tempDir, "V2ecg.bin");
			File ecg3File = new File(tempDir, "V3ecg.bin");
			if (ecg3File.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(ecg3File);
				ssf1.start();
				return;
			}

			cod.dirtyFiles.add(ecg3File);
			FileInputStream fis = new FileInputStream(ecg1File);
			FileInputStream fis2 = new FileInputStream(ecg2File);
			FileOutputStream fos = new FileOutputStream(ecg3File);
			FileChannel ifC = fis.getChannel();
			FileChannel ifC2 = fis2.getChannel();
			FileChannel ofC = fos.getChannel();

			int size = 1048576;
			ByteBuffer bb = ByteBuffer.allocateDirect(size);
			IntBuffer sb = bb.asIntBuffer();
			ByteBuffer bb2 = ByteBuffer.allocateDirect(size);
			IntBuffer sb2 = bb2.asIntBuffer();
			ByteBuffer obb = ByteBuffer.allocateDirect(size);
			IntBuffer osb = obb.asIntBuffer();
			int[] samp = new int[size / 4];
			int[] samp2 = new int[size / 4];
			int[] outBuf = new int[size / 4];
			int prevval = 0, prevprevval = 0;
			long fL = ecg1File.length();
			int nRead = 0;
			int nOut;
			ProgressMonitor progress = new ProgressMonitor(MainFrame.getInstance().getMainFrame(),
					"Generating diff ECG", null, 0, (int) fL);
			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) i);
				bb.position(0);
				sb.position(0);
				bb2.position(0);
				sb2.position(0);
				nRead = ifC.read(bb);
				ifC2.read(bb2);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				sb2.get(samp2, 0, nSRead);
				outBuf[0] = samp[0] - samp2[0];
				for (int q = 1; q < nSRead - 1; q++) {
					outBuf[q] = (samp[q] - samp2[q]);
				}
				nOut = nSRead - 1;
				obb.clear();
				osb.clear();
				osb.put(outBuf, 0, nOut);
				obb.limit(4 * nOut);
				ofC.write(obb);
			}
			progress.close();
			obb.clear();
			osb.clear();
			osb.put(prevval - prevprevval);
			obb.limit(4);
			ofC.write(obb);
			ofC.close();
			ifC.close();
			ifC2.close();
			fos.close();
			fis.close();
			fis2.close();
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(ecg3File);
			ssf1.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("generateDiffECG took (" + timer.getTime() / 1000. + " sec)");
	}
}
