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
 * Generates DZ signal from Z0 file for now by copying
 */
public class DZFileGenerator {
	public static void GenerateDZ() {
		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File zFile = new File(tempDir, "Z0.bin");
			File dzFile = new File(tempDir, "DZ.bin");
			if (dzFile.exists())
				return;
			FileInputStream fis = new FileInputStream(zFile);
			FileOutputStream fos = new FileOutputStream(dzFile);
			FileChannel ifC = fis.getChannel();
			FileChannel ofC = fos.getChannel();

			int size = 1048576;
			ByteBuffer bb = ByteBuffer.allocateDirect(size);
			IntBuffer sb = bb.asIntBuffer();
			ByteBuffer obb = ByteBuffer.allocateDirect(size);
			IntBuffer osb = obb.asIntBuffer();
			int[] samp = new int[size / 4];
			int[] outBuf = new int[size / 4];
			int prevval = 0, prevprevval = 0;
			long fL = zFile.length();
			int nRead = 0;
			int nOut;
			ProgressMonitor progress = new ProgressMonitor(MainFrame.getInstance().getMainFrame(), "Generating DZ/DT",
					null, 0, (int) fL);
			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) i);
				bb.position(0);
				sb.position(0);
				nRead = ifC.read(bb);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				outBuf[0] = samp[0];
				for (int q = 1; q < nSRead - 1; q++) {
					outBuf[q] = (samp[q]);
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
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("generateDZ took (" + timer.getTime() / 1000. + " sec)");
	}
}
