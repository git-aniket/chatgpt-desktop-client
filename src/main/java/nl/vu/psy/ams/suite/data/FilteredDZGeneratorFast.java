package nl.vu.psy.ams.suite.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

// import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Generates a filtered DZ signal from original DZ signal.
 * The filtered DZ signal is used to calculate respiration
 * in the RSA tab.
 */
public class FilteredDZGeneratorFast {
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
	int HAMPELWINDOW = 5;
	double[] hampelBuffer = new double[HAMPELWINDOW]; // outlier detection
	double realSlope = 1;
	double realConstant = 0;
	int outliers = 0; // , m_outliers = 0, fp_outliers;

	public void GenerateFilteredDZ(String inFile, boolean respiration) {
		Timer timer = new Timer();
		timer.start();
		try {
			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();
			File dzFile = new File(tempDir, inFile + ".bin");
			File filteredFile, fdzFile;
			if (inFile != "ECG" && inFile != "V2ecg" && inFile != "V3ecg") {
				if (respiration) {
					filteredFile = new File(tempDir, "DZRESP.bin");
					Ams7fsChannelInfo respChannel = null;
					for (Ams7fsChannelInfo s : cod.getChannelInfo())
						if (s.getSzID().equals("DZRESP"))
							respChannel = s;
					if (respChannel == null) {
						Ams7fsChannelInfo dzChannel = new Ams7fsChannelInfo();
						Ams7fsChannelInfo zChannel = cod.getChannelInfoFromID("Z0");
						dzChannel.setSzID("DZRESP");
						dzChannel.setSzUnit("\u2126");
						dzChannel.setlMinValue(-8198);
						dzChannel.setlMaxValue(8186);
						dzChannel.setlMinMaxDivider((zChannel.getlMinMaxDivider()));
						dzChannel.setnBits(zChannel.getnBits());
						dzChannel.setDwDivider(zChannel.getDwDivider());
						cod.getChannelInfo().add(dzChannel);
					}
					fdzFile = new File(tempDir, "FILTDZ.bin");
					if (filteredFile.exists() && fdzFile.exists()
							&& (Utils.getExtension(cod.getDataFile()).equals("7fs")
									|| cod.getFileHeader().getDwHardwareVersion() == 7)) {
						SubsetFilesSingle ssf1 = new SubsetFilesSingle(filteredFile);
						ssf1.start();
						SubsetFilesSingle ssf2 = new SubsetFilesSingle(fdzFile);
						ssf2.start();
						return;
					}
					cod.dirtyFiles.add(fdzFile);
					cod.dirtyFiles.add(filteredFile);
				} else {
					filteredFile = new File(tempDir, "DZ.bin");
					fdzFile = new File(tempDir, "temp2.bin");
					if (filteredFile.exists()
							&& (Utils.getExtension(cod.getDataFile()).equals("7fs")
									|| cod.getFileHeader().getDwHardwareVersion() == 7)) {
						SubsetFilesSingle ssf1 = new SubsetFilesSingle(filteredFile);
						ssf1.start();
						return;
					}
					cod.dirtyFiles.add(filteredFile);
				}
			} else {
				filteredFile = new File(tempDir, "FILT" + inFile + ".bin");
				fdzFile = new File(tempDir, "temp2.bin");
				if (filteredFile.exists()
						&& (Utils.getExtension(cod.getDataFile()).equals("7fs")
								|| cod.getFileHeader().getDwHardwareVersion() == 7)) {
					SubsetFilesSingle ssf1 = new SubsetFilesSingle(filteredFile);
					ssf1.start();
					return;
				}
				cod.dirtyFiles.add(filteredFile);
			} // always recalculate for old files because these contain Shorts

			Ams7fsChannelInfo s = null;
			int sampleTimeInUS = (int) cod.getFileHeader().getDwSampleTime_us();
			try {
				s = cod.getChannelInfoFromID(inFile);
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

			double[] b = null;
			double[] a = null;

			/*
			 * From Matlab Function [a,b]=butter(2 , [0.0002, 0.0008] ,'bandpass') - Applies
			 * a second order filter that passes all frequencies
			 * between 0.1 and 0.4 Hz
			 * 
			 * voor bandpass 0.1 tot 0.4 Hz van een maal in de 10 sec tot 1 maal 2.5 sec
			 * ademhaling
			 * for bandpass 0.1 to 0.4 Hz of once every 10 seconds to 1 times 2.5 sec
			 * breathing
			 * 
			 */
			if (sampleTimeInUS == 1000) {
				if (respiration) {
					// b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
					// 8.8712025778419194e-007 };
					// a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000,
					// 5.9920000556328326e+000, -3.9973311156433828e+000 };
					// [a,b]=butter(2 , [0.00002, 0.0008] ,'bandpass') (0.01 - 0.4 Hz)
					// b = new double[] { 0.149856947302690e-005, 0, -0.299713894605380e-005,
					// 0, 0.149856947302690e-005 };
					// a = new double[] { 0.996540549045275, -3.989615337850353, 5.989609028018611,
					// -3.996534239213509 };
					// [a,b]=butter(2 , [0.0001, 0.0008] ,'bandpass') (0.05 - 0.4 Hz)
					b = new double[] { 0.120714892591883e-005, 0, -0.241429785183766e-005,
							0, 0.120714892591883e-005 };
					a = new double[] { 0.996894813039886, -3.990678035066261, 5.990671628561728,
							-3.996888406534731 };
				} else {
					// [a,b] =butter(2,[.00004 ,.0020], 'bandpass') (0.02 - 1 Hz)
					b = new double[] { 0.00355258014728165, 0, -0.00710516029456329, 0, 0.00355258014728165 };
					a = new double[] { 0.838670728317484, -3.50152501965464, 5.48701236858124, -3.82415805434902 };
					// [a,b]=butter(2 , [0.0002] ,.1], 'bandpass')
					// b = new double[] {0.020076095356816, 0, -0.040152190713632, 0,
					// 0.020076095356816};
					// a = new double[] {0.641408457928494, -2.843892690969342, 4.763552871395376,
					// -3.561068638037374};
				}
			}

			if (b == null || a == null)
				return;

			long fL = dzFile.length();
			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered " + inFile, null, 0, (int) fL);

			outliers = 0; // m_outliers = 0; fp_outliers = 0;
			filterForAndBackward(a, b, dzFile, filteredFile, progress, respiration);
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(filteredFile);
			ssf1.start();
			if (respiration) {
				reduceSamplesize(filteredFile, fdzFile, progress);
				SubsetFilesSingle ssf2 = new SubsetFilesSingle(fdzFile);
				ssf2.start();
				File fileTicks = new File(cod.getFilePath(), "TicksA.bin");
				File fileTicksRed = new File(cod.getFilePath(), "TicksARed.bin");
				if (fileTicks.exists()) {
					reduceSamplesize(fileTicks, fileTicksRed, progress);
					cod.dirtyFiles.add(fileTicksRed);
				}
			}
			System.out.println(inFile + ": " + outliers + " outliers found"); // ; " + m_outliers + " potential outliers
																				// missed; " + fp_outliers + " false
																				// positives.");
			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
		} catch (

		IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filter " + inFile + " took (" + timer.getTime() / 1000. + " sec)");
	}

	// replaced FileInputStream and FileOutputStream with filechannels (as in
	// filterBackward); reduces execution time by one-third
	// search for 'new byte[(int)'
	void filterForAndBackward(double[] a, double[] b, File dzFile, File tempFile, ProgressMonitor progress,
			boolean respiration) throws IOException {
		int size = (int) dzFile.length();
		bb = ByteBuffer.allocateDirect(size);
		sb = bb.asIntBuffer();
		obb = ByteBuffer.allocateDirect(size);
		osb = obb.asIntBuffer();

		samp = new int[size / 4];
		outBuf = new int[size / 4];
		tempBuf = new double[size / 4];
		FileInputStream fis = new FileInputStream(dzFile);
		FileOutputStream fos = new FileOutputStream(tempFile);
		ifC = fis.getChannel();
		ofC = fos.getChannel();

		CurrentOpenData cod = CurrentOpenData.getInstance();
		int nRead = size;
		int nOut;
		double startTime = cod.getStartTimeInUS();
		double endTime = cod.getEndTimeInUS();

		double curTime = startTime;
		boolean is5fsOrAms = Utils.getExtension(cod.getDataFile()).equals("5fs")
				|| Utils.getExtension(cod.getDataFile()).equals("ams");
		ArtefactSet aSet = cod.getECGArtefacts();
		int sampleTimeInUS = (int) cod.getFileHeader().getDwSampleTime_us();
		Comparator<Integer> comparator = (a1, b1) -> samp[a1] != samp[b1] ? Integer.compare(samp[a1], samp[b1])
				: a1 - b1;
		TreeSet<Integer> left = new TreeSet<>(comparator.reversed());
		TreeSet<Integer> right = new TreeSet<>(comparator);

		Supplier<Double> medians = (HAMPELWINDOW % 2 == 0)
				? () -> ((double) samp[left.first()] + samp[right.first()]) / 2
				: () -> (double) samp[right.first()];

		// balance lefts size and rights size (if not equal then right will be larger by
		// one)
		Runnable balance = () -> {
			while (left.size() > right.size())
				right.add(left.pollFirst());
		};

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
			forBuffer[q] = 0;
		for (int q = 0; q < hampelBuffer.length; q++) {
			hampelBuffer[q] = realConstant + realSlope * samp[q];
			left.add(q);
		}
		int progressIncrement = nSRead / 10;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				if (respiration)
					progress.setProgress((int) q / 3);
				else
					progress.setProgress((int) q / 2);
			}
			if (!is5fsOrAms && respiration) {
				if ((curTime > (startTime + 10000000)) && (curTime < (endTime - 10000000))
						&& curTime % 1000000 == startTime) {
					// 20 seconds window, once per second
					double HR = cod.getBeatSet()
							.getAverageBetweenTimes(curTime - 10000000, curTime + 10000000, true);
					boolean isArtefact = aSet.isArtefactBetweenTimes(curTime - 10000000,
							curTime + 10000000); // If the Label has artefatcs
					boolean isArefact_previousLabel = aSet.isArtefactBetweenTimes(curTime -
							80000000, curTime - 60000000); // Go to the previous label to detect HR

					if (isArtefact) {
						if (!isArefact_previousLabel) {
							HR = cod.getBeatSet()
									.getAverageBetweenTimes(curTime - 80000000, curTime - 60000000, true);
						} else {
							HR = 50;
						}
					}

					if (HR < 100) { // RR <= 25 // 0,1 to 0,4 Hz => [a,b]=butter(2 , [0.0002, 0.0008] ,'bandpass')
						// b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
						// 8.8712025778419194e-007 };
						// a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000,
						// 5.9920000556328326e+000, -3.9973311156433828e+000 };
						// [a,b]=butter(2 , [0.0001, 0.0008] ,'bandpass') (0.05 - 0.4 Hz)
						b = new double[] { 0.120714892591883e-005, 0, -0.241429785183766e-005,
								0, 0.120714892591883e-005 };
						a = new double[] { 0.996894813039886, -3.990678035066261, 5.990671628561728,
								-3.996888406534731 };
					} else if (HR > 100 && HR < 130) { // HR > 100 RR <= 45 // 0.1 to 0.75 Hz => [b,a] =
														// butter(2,[0.0002,0.0015], 'bandpass')
						// b = new double[] { 4.15782671645446e-006, 0, -8.31565343290892e-006, 0,
						// 4.15782671645446e-006 };
						// a = new double[] { 0.994240899745738, -3.982700171491988, 5.982677626703472,
						// -3.994218354948480 };
						// [b,a] = butter(2,[0.0001,0.0015], 'bandpass') (0.05 - 0.75 Hz)
						b = new double[] { 0.482110468627903e-005, 0, -0.964220937255805e-005, 0,
								0.482110468627903e-005 };
						a = new double[] { 0.993799268265878, -3.981375573277178, 5.981353332579079,
								-3.993777027565594 };
					} else if (HR > 130 && HR < 160) { // RR <= 50 => 0.1 to 0.83(50/60) => [b,a] =
														// butter(2,[0.0002,0.00166], 'bandpass')
						// b = new double[] { 0.052424469906870e-004, 0, -0.104848939813740e-004, 0,
						// 0.052424469906870e-004 };
						// a = new double[] { 0.993534383544242, -3.980575659019019, 5.980548146230042,
						// -3.993506870744562 };
						// [b,a] = butter(2,[0.0001,0.00166], 'bandpass') (0.05 - 0.83 Hz)
						b = new double[] { 0.059839184197083e-004, 0, -0.119678368394166e-004, 0,
								0.059839184197083e-004 };
						a = new double[] { 0.993093065891550, -3.979252002279845, 5.979224795571689,
								-3.993065859180719 };
					} else if (HR > 160) { // RR <= 55 => 0.1 to 0.91 (55/60) => [b,a] = butter(2,[0.0002,0.00182],
											// 'bandpass')
						// b = new double[] { 0.064522486837612e-004, 0, -0.129044973675225e-004, 0,
						// 0.064522486837612e-004 };
						// a = new double[] { 0.992828369399297, -3.978452152950424, 5.978419171951428,
						// -3.992795388387441 };
						// [b,a] = butter(2,[0.0001,0.00182], 'bandpass') (0.02 - 0.91 Hz)
						b = new double[] { 0.072717572820136e-004, 0, -0.145435145640272e-004, 0,
								0.064522486837612e-004 };
						a = new double[] { 0.992387365350768, -3.977129437016641, 5.977096764309827,
								-3.992354692640740 };
					}
				}
			}

			curTime += sampleTimeInUS;
			vl = realConstant + realSlope * samp[q];
			// remove outliers: Hampel method with threshold 10 sd
			// double[] ary = Arrays.copyOf(hampelBuffer, 5);
			// Arrays.sort(ary);
			// slow 'normal' median
			// double median0 = ary[2];

			// sliding window median from @KidOptimo on
			// https://leetcode.com/problems/sliding-window-median/solutions/96346/java-using-two-tree-sets-o-n-logk/
			balance.run();
			double median = realConstant + realSlope * medians.get();
			double[] window = new double[HAMPELWINDOW];
			for (int z = 0; z < window.length; z++)
				window[z] = Math.abs(hampelBuffer[z] - median);
			Arrays.sort(window);
			double threshold = 10 * 1.4826 * window[2];
			if (Math.abs(vl - median) > threshold) {
				// if (vl != 0.0)
				// fp_outliers++;
				vl = median;
				outliers++;
				// } else if (vl == 0.0) {
				// m_outliers++;
			}
			if (q > HAMPELWINDOW / 2 + 1 && q < samp.length - (HAMPELWINDOW / 2 + 1)) {
				for (int z = 0; z < hampelBuffer.length - 1; z++)
					hampelBuffer[z] = hampelBuffer[z + 1];
				hampelBuffer[hampelBuffer.length - 1] = realConstant + realSlope * samp[q + 1];
				// remove tail of window from either left or right
				if (!left.remove(q - HAMPELWINDOW + 1))
					right.remove(q - HAMPELWINDOW + 1);

				// add next num, this will always increase left size
				right.add(q + 1);
				left.add(right.pollFirst());
			}
			// end remove outliers
			double val = vl * b[b.length - 1];
			for (int z = 0; z < b.length - 1; z++)
				val += backBuffer[z] * b[z];
			for (int z = 0; z < a.length; z++)
				val -= forBuffer[z] * a[z];
			for (int z = 0; z < backBuffer.length - 1; z++)
				backBuffer[z] = backBuffer[z + 1];
			for (int z = 0; z < forBuffer.length - 1; z++)
				forBuffer[z] = forBuffer[z + 1];
			backBuffer[backBuffer.length - 1] = vl;
			forBuffer[forBuffer.length - 1] = val;

			tempBuf[q] = val;
		}

		vl = tempBuf[nSRead - 1];
		for (int q = 0; q < bBackBuffer.length; q++)
			bBackBuffer[q] = vl;
		for (int q = 0; q < bForBuffer.length; q++)
			bForBuffer[q] = 0;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				if (respiration)
					progress.setProgress(size / 3 + (int) q / 3);
				else
					progress.setProgress(size / 2 + (int) q / 2);
			}
			if (!is5fsOrAms && respiration) {
				if ((curTime > (startTime + 10000000)) && (curTime < (endTime - 10000000))
						&& curTime % 1000000 == startTime) {
					// 6 seconds window, once per second
					double HR = cod.getBeatSet()
							.getAverageBetweenTimes(curTime - 10000000, curTime + 10000000, true);
					boolean isArtefact = aSet.isArtefactBetweenTimes(curTime - 10000000,
							curTime + 10000000); // Label has artefatcs
					boolean isArefact_previousLabel = aSet.isArtefactBetweenTimes(curTime -
							80000000, curTime - 60000000); // Go to the previous label to detect HR

					if (isArtefact) {
						if (!isArefact_previousLabel) {
							HR = cod.getBeatSet()
									.getAverageBetweenTimes(curTime - 80000000, curTime - 60000000, true);
						} else {
							HR = 50;
						}
					}

					if (HR < 100) { // RR <= 25 // 0,1 to 0,4 Hz => [a,b]=butter(2 , [0.0002, 0.0008] ,'bandpass')
						// b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
						// 8.8712025778419194e-007 };
						// a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000,
						// 5.9920000556328326e+000, -3.9973311156433828e+000 };
						// [a,b]=butter(2 , [0.0001, 0.0008] ,'bandpass') (0.05 - 0.4 Hz)
						b = new double[] { 0.120714892591883e-005, 0, -0.241429785183766e-005,
								0, 0.120714892591883e-005 };
						a = new double[] { 0.996894813039886, -3.990678035066261, 5.990671628561728,
								-3.996888406534731 };
					} else if (HR > 100 && HR < 130) { // HR > 100 RR <= 45 // 0.1 to 0.75 Hz => [b,a] =
														// butter(2,[0.0002,0.0015], 'bandpass')
						// b = new double[] { 4.15782671645446e-006, 0, -8.31565343290892e-006, 0,
						// 4.15782671645446e-006 };
						// a = new double[] { 0.994240899745738, -3.982700171491988, 5.982677626703472,
						// -3.994218354948480 };
						// [b,a] = butter(2,[0.0001,0.0015], 'bandpass') (0.05 - 0.75 Hz)
						b = new double[] { 0.482110468627903e-005, 0, -0.964220937255805e-005, 0,
								0.482110468627903e-005 };
						a = new double[] { 0.993799268265878, -3.981375573277178, 5.981353332579079,
								-3.993777027565594 };
					} else if (HR > 130 && HR < 160) { // RR <= 50 => 0.1 to 0.83(50/60) => [b,a] =
														// butter(2,[0.0002,0.00166], 'bandpass')
						// b = new double[] { 0.052424469906870e-004, 0, -0.104848939813740e-004, 0,
						// 0.052424469906870e-004 };
						// a = new double[] { 0.993534383544242, -3.980575659019019, 5.980548146230042,
						// -3.993506870744562 };
						// [b,a] = butter(2,[0.0001,0.00166], 'bandpass') (0.05 - 0.83 Hz)
						b = new double[] { 0.059839184197083e-004, 0, -0.119678368394166e-004, 0,
								0.059839184197083e-004 };
						a = new double[] { 0.993093065891550, -3.979252002279845, 5.979224795571689,
								-3.993065859180719 };
					} else if (HR > 160) { // RR <= 55 => 0.1 to 0.91 (55/60) => [b,a] = butter(2,[0.0002,0.00182],
											// 'bandpass')
						// b = new double[] { 0.064522486837612e-004, 0, -0.129044973675225e-004, 0,
						// 0.064522486837612e-004 };
						// a = new double[] { 0.992828369399297, -3.978452152950424, 5.978419171951428,
						// -3.992795388387441 };
						// [b,a] = butter(2,[0.0001,0.00182], 'bandpass') (0.02 - 0.91 Hz)
						b = new double[] { 0.072717572820136e-004, 0, -0.145435145640272e-004, 0,
								0.064522486837612e-004 };
						a = new double[] { 0.992387365350768, -3.977129437016641, 5.977096764309827,
								-3.992354692640740 };
					}
				}
			}
			curTime += sampleTimeInUS;
			vl = tempBuf[nSRead - q - 1];
			double val = vl * b[b.length - 1];
			for (int z = 0; z < b.length - 1; z++)
				val += bBackBuffer[z] * b[z];
			for (int z = 0; z < a.length; z++)
				val -= bForBuffer[z] * a[z];
			for (int z = 0; z < bBackBuffer.length - 1; z++)
				bBackBuffer[z] = bBackBuffer[z + 1];
			for (int z = 0; z < bForBuffer.length - 1; z++)
				bForBuffer[z] = bForBuffer[z + 1];
			bBackBuffer[bBackBuffer.length - 1] = vl;
			bForBuffer[bForBuffer.length - 1] = val;
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
		ifC.close();
		fis.close();
		fos.close();
	}

	void reduceSamplesize(File filteredFile, File fdzFile, ProgressMonitor progress) throws IOException {

		CurrentOpenData cod = CurrentOpenData.getInstance();
		int nToSkip = (int) (100000 / cod.getFileHeader().getDwSampleTime_us());
		if (nToSkip < 1)
			nToSkip = 1;
		int skipCounter = 0;
		int size = (int) filteredFile.length();
		int nRead = size;

		FileInputStream fis = new FileInputStream(filteredFile);
		FileOutputStream fos = new FileOutputStream(fdzFile);
		int buflength = (int) size / nToSkip;
		if (buflength % 4 == 1)
			buflength += 3;
		if (buflength % 4 == 2)
			buflength += 2;
		if (buflength % 4 == 3)
			buflength += 1;
		ifC = fis.getChannel();
		ofC = fos.getChannel();

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
		int nOut = 0;
		int progressIncrement = nSRead / 10;
		for (int q = 0; q < nSRead; q++) {
			if (q % progressIncrement == 0) {
				progress.setProgress((int) (2 * size / 3. + q / 3.));
			}
			skipCounter++;
			if (skipCounter >= nToSkip) {
				outBuf[nOut] = samp[q];
				nOut++;
				skipCounter = 0;
			}
		}
		obb.clear();
		osb.clear();
		osb.put(outBuf, 0, nOut);
		obb.limit(4 * nOut);
		ofC.write(obb);
		ofC.close();
		ifC.close();
		fis.close();
		fos.close();

	}
}
