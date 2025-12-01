package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Generates DZ/DT signal from DZ file by differentiation
 */
public class DZDTFileGenerator {
	public static void GenerateDZDT(String infile) {
		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File dzFile = new File(tempDir, infile + ".bin");
			File dzdtFile = new File(tempDir, "DZDT.bin");
			if (dzdtFile.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(dzdtFile);
				ssf1.start();
				return;
			}

			cod.dirtyFiles.add(dzdtFile);
			int size = 1048576;
			int[] samp = new int[size / 4];
			int[] outBuf = new int[size / 4];
			int prevval = 0, prevprevval = 0;
			boolean firstrun = true;
			long fL = dzFile.length();
			int nOut;
			FileInputStream fis = new FileInputStream(dzFile);
			byte[] buf = new byte[(int) fL];
			fis.read(buf);
			ByteBuffer inBuffer = ByteBuffer.wrap(buf);
			fis.close();
			IntBuffer sb = inBuffer.asIntBuffer();
			// ofC = (new FileOutputStream(tempDZDTFile)).getChannel();
			FileOutputStream fos = new FileOutputStream(dzdtFile);
			byte[] outbuf = new byte[(int) fL];
			ByteBuffer outBuffer = ByteBuffer.wrap(outbuf);
			IntBuffer osb = outBuffer.asIntBuffer();
			int nRead = size;
			JFrame frame = MainFrame.getInstance().getMainFrame();

			ProgressMonitor progress = new ProgressMonitor(frame, "Generating DZ/DT", null, 0, (int) fL);
			for (long i = 0; i < fL; i += nRead) {
				progress.setProgress((int) i);
				// bb.position(0);
				// sb.position(0);
				// nRead = ifC.read(bb);
				int nSRead;
				if (sb.capacity() - sb.position() < nRead / 4)
					nSRead = sb.capacity() - sb.position();
				else
					nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				if (firstrun == true) {
					outBuf[0] = (samp[1] - samp[0]);
					for (int q = 1; q < nSRead - 1; q++) {
						outBuf[q] = (samp[q + 1] - samp[q - 1]);
					}
					firstrun = false;
					prevprevval = samp[nSRead - 2];
					prevval = samp[nSRead - 1];
					nOut = nSRead - 1;
				} else {
					outBuf[0] = (samp[0] - prevprevval);
					outBuf[1] = (samp[1] - prevval);
					for (int q = 1; q < nSRead - 1; q++) {
						outBuf[q + 1] = (samp[q + 1] - samp[q - 1]);
					}
					prevprevval = samp[nSRead - 2];
					prevval = samp[nSRead - 1];
					nOut = nSRead;
				}
				// obb.clear();
				// osb.clear();
				osb.put(outBuf, 0, nOut);
				// obb.limit(4 * nOut);
				// ofC.write(obb);
			}
			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
			// obb.clear();
			// osb.clear();
			osb.put(prevval - prevprevval);
			// obb.limit(4);
			// ofC.write(obb);
			// ofC.close();
			// ifC.close();
			fos.write(outbuf);
			fos.close();
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(dzdtFile);
			ssf1.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("generateDZDT took (" + timer.getTime() / 1000. + " sec)");
	}
}
