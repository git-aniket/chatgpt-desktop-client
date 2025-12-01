package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

public class FilteredDZDTSignalFast {
	private FileChannel ifC;
	private FileChannel ofC;
	ByteBuffer bb;
	IntBuffer sb;
	ByteBuffer obb;
	IntBuffer osb;
	int[] samp;
	int[] outBuf;
	double[] tempBuf;
	double[] backBuffer = new double[4];
	double[] forBuffer = new double[4];
	double[] bBackBuffer = new double[4];
	double[] bForBuffer = new double[4];
	double realSlope = 1;
	double realConstant = 0;

	public void GenerateFilteredDZDT() {

		Timer timer = new Timer();
		timer.start();
		try {

			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();

			File DZDTFile = new File(tempDir, "DZDT.bin");
			File fDZDTFile = new File(tempDir, "FILTDZDT.bin");

			if (fDZDTFile.exists()) {
				SubsetFilesSingle ssf1 = new SubsetFilesSingle(fDZDTFile);
				ssf1.start();
				return;
			}

			cod.dirtyFiles.add(fDZDTFile);

			Ams7fsChannelInfo s = null;
			int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("DZDT");
				if (s.getRealSlope() == 0) {
					double lowerBound = -Math.pow(2, s.getnBits() - 1);
					double upperBound = Math.pow(2, s.getnBits() - 1) - 1;
					double lowerValue = (double) s.getlMinValue() / s.getlMinMaxDivider();
					double upperValue = (double) s.getlMaxValue() / s.getlMinMaxDivider();
					realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
					realConstant = lowerValue - realSlope * lowerBound;
				} else {
					realSlope = s.getRealSlope();
					realConstant = s.getRealConstant();
				}
				sampleTimeInUS *= s.getDwDivider();

			} catch (Exception e) {
				e.printStackTrace();
			}

			// Use Matlab to create filter coefficients used below
			//
			// format long;
			// [b,a] = butter(4,0.12,'low')

			double[] b = null;
			double[] a = null;

			if (sampleTimeInUS == 1000) { // Sampling time is 100 ms = 100000 us

				// Low pass filter - 0.04 - 20 Hz
				// b = new double[]{0.132937288988208e-004, 0.531749155952832e-004,
				// 0.797623733929248e-004, 0.531749155952832e-004, 0.132937288988208e-004};
				// a = new double[]{1.000000000000000, -3.671729089161935, 5.067998386734189,
				// -3.115966925201744, 0.719910327291871};

				// Low pass filter - 0.06 - 30 Hz
				// b = new double[]{0.062386983548435e-003, 0.249547934193739e-003,
				// 0.374321901290608e-003, 0.249547934193739e-003, 0.062386983548435e-003};
				// a = new double[]{1.000000000000000, -3.507786207390784, 4.640902412686708,
				// -2.742652821120374, 0.610534807561224};

				// Low pass filter - 0.06 - 35 Hz
				// b = new double[]{ 0.111381075595079e-003, 0.445524302380318e-003,
				// 0.668286453570477e-003, 0.445524302380318e-003, 0.111381075595079e-003};
				// a = new double[]{ 1.000000000000000, -3.425894756190268, 4.436793654298330,
				// -2.571248261036764, 0.562131460138223};
				// Low pass filter - 0.04 - 20 Hz
				// b = new double[]{0.132937288987445e-004, 0.531749155949779e-004,
				// 0.797623733924668e-004, 0.531749155949779e-004, 0.132937288987445e-004};
				// a = new double[]{1.000000000000000, -3.671729089161937, 5.067998386734193,
				// -3.115966925201749, 0.719910327291873};

				// Low pass filter - 0.09 - 45 Hz
				// b = new double[]{0.000283144330561, 0.001132577322246, 0.001698865983369,
				// 0.001132577322246, 0.000283144330561};
				// a = new double[]{1.000000000000000, -3.262313310157079, 4.047026636364971,
				// -2.256462718074147, 0.476279701155238};

				// Low pass filter - 0.12 - 60 Hz
				b = new double[] { 0.000806359865037, 0.003225439460149, 0.004838159190223, 0.003225439460149,
						0.000806359865037 };
				a = new double[] { 1.000000000000000, -3.017555238686490, 3.507193724716208, -1.847550944118580,
						0.370814215929455 };

			} else if (sampleTimeInUS == 2000) {
				// Low pass filter - 0.12 - 60 Hz
				b = new double[] { 0.008914457239463, 0.035657828957852, 0.053486743436778, 0.035657828957852,
						0.008914457239463 };
				a = new double[] { 1.000000000000000, -2.048395137764509, 1.841785841678161, -0.782440103120936,
						0.131680715038692 };
			} else if (sampleTimeInUS == 4000) {
				// Low pass filter - 0.12 - 60 Hz
				b = new double[] { 0.082672620462993, 0.330690481851974, 0.496035722777961, 0.330690481851974,
						0.082672620462993 };
				a = new double[] { 1.000000000000000, -0.156210584138285, 0.493742652574357, -0.032907218058628,
						0.018137077030451 };
			}

			if (b == null || a == null)
				return;

			long fL = DZDTFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered DZDT", null, 0, (int) fL);

			filterForAndBackward(a, b, DZDTFile, fDZDTFile, progress);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(fDZDTFile);
			ssf1.start();

			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
		} catch (IOException e) {
			e.printStackTrace();
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filterDZDT took (" + timer.getTime() / 1000. + " sec)");
	}

	void filterForAndBackward(double[] a, double[] b, File DZDTFile, File tempDZDTFile, ProgressMonitor progress)
			throws IOException {
		int size = (int) DZDTFile.length();
		bb = ByteBuffer.allocateDirect(size);
		sb = bb.asIntBuffer();
		obb = ByteBuffer.allocateDirect(size);
		osb = obb.asIntBuffer();

		samp = new int[size / 4];
		outBuf = new int[size / 4];
		tempBuf = new double[size / 4];
		FileInputStream fis = new FileInputStream(DZDTFile);
		FileOutputStream fos = new FileOutputStream(tempDZDTFile);
		ifC = fis.getChannel();
		ofC = fos.getChannel();

		int nRead = size;
		int nOut;

		bb.position(0);
		sb.position(0);
		nRead = ifC.read(bb);
		int nSRead = nRead / 4;
		if (nRead < 1) {
			ofC.close();
			ifC.close();
			fis.close();
			fos.close();
			return;
		}
		sb.get(samp, 0, nSRead);
		double vl = realConstant + realSlope * samp[0];
		for (int q = 0; q < backBuffer.length; q++)
			backBuffer[q] = vl;
		for (int q = 0; q < forBuffer.length; q++)
			forBuffer[q] = vl;
		int progressIncrement = nSRead / 10;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				progress.setProgress((int) q / 2);
			}
			vl = realConstant + realSlope * samp[q];
			double val = vl * b[0];
			for (int z = 0; z < b.length - 1; z++)
				val += backBuffer[z] * b[z + 1];
			for (int z = 0; z < a.length - 1; z++)
				val -= forBuffer[z] * a[z + 1];
			for (int z = 0; z < backBuffer.length - 1; z++)
				backBuffer[3 - z] = backBuffer[2 - z];
			for (int z = 0; z < forBuffer.length - 1; z++)
				forBuffer[3 - z] = forBuffer[2 - z];
			backBuffer[0] = vl;
			forBuffer[0] = val;

			tempBuf[q] = val;
		}

		vl = tempBuf[nSRead - 1];
		for (int q = 0; q < bBackBuffer.length; q++)
			bBackBuffer[q] = vl;
		for (int q = 0; q < bForBuffer.length; q++)
			bForBuffer[q] = vl;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				progress.setProgress(size / 2 + (int) q / 2);
			}
			vl = tempBuf[nSRead - q - 1];
			double val = vl * b[0];
			for (int z = 0; z < b.length - 1; z++)
				val += bBackBuffer[z] * b[z + 1];
			for (int z = 0; z < a.length - 1; z++)
				val -= bForBuffer[z] * a[z + 1];
			for (int z = 0; z < bBackBuffer.length - 1; z++)
				bBackBuffer[3 - z] = bBackBuffer[2 - z];
			for (int z = 0; z < bForBuffer.length - 1; z++)
				bForBuffer[3 - z] = bForBuffer[2 - z];
			bBackBuffer[0] = vl;
			bForBuffer[0] = val;
			double newVal = ((val - realConstant) / realSlope);
			int shortVal = (int) newVal;

			if (newVal < Integer.MIN_VALUE) {
				shortVal = Integer.MIN_VALUE;
			} else if (newVal > Integer.MAX_VALUE) {
				shortVal = Integer.MAX_VALUE;
			}

			outBuf[nSRead - q - 1] = shortVal;
		}
		nOut = nSRead;

		obb.clear();
		osb.clear();
		osb.put(outBuf, 0, nOut);
		obb.limit(4 * nOut);
		ofC.write(obb);

		ofC.close();
		fos.close();
		ifC.close();
		fis.close();
	}
}
