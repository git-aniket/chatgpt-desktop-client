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

import nl.vu.psy.ams.suite.data.structures.AmsLabel;
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
public class FilteredDZGenerator {
	@SuppressWarnings("resource")
	public static void GenerateFilteredDZ(String inFile, boolean respiration) {
		Timer timer = new Timer();
		timer.start();
		try {
			ArtefactSet aSet = CurrentOpenData.getInstance().getECGArtefacts();
			CurrentOpenData cod = CurrentOpenData.getInstance();
			File tempDir = cod.getFilePath();
			File dzFile = new File(tempDir, inFile + ".bin");
			File tempFile = new File(tempDir, "temp1.bin");
			File filteredFile, fdzFile;
			if (inFile != "ECG") {
				if (respiration)
					filteredFile = new File(tempDir, "DZRESP.bin");
				else
					filteredFile = new File(tempDir, "DZ.bin");
				fdzFile = new File(tempDir, "FILTDZ.bin");
			} else {
				filteredFile = new File(tempDir, "FILTECG.bin");
				fdzFile = new File(tempDir, "temp2.bin");
			}
			if (fdzFile.exists()) {
				return;
			}

			FileChannel ifC = (new FileInputStream(dzFile)).getChannel();
			FileChannel ofC = (new FileOutputStream(tempFile)).getChannel();

			int size = 1048576;
			ByteBuffer bb = ByteBuffer.allocateDirect(size);
			IntBuffer sb = bb.asIntBuffer();
			ByteBuffer obb = ByteBuffer.allocateDirect(size);
			IntBuffer osb = obb.asIntBuffer();

			double[] backBuffer = new double[4];
			double[] forBuffer = new double[4];

			int[] samp = new int[size / 4];
			int[] outBuf = new int[size / 4];

			boolean firstrun = true;
			long fL = dzFile.length();
			int nRead = 0;
			int nOut;

			double realSlope = 1;
			double realConstant = 0;

			Ams7fsChannelInfo s = null;
			int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID(inFile);
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
					b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
							8.8712025778419194e-007 };
					a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000, 5.9920000556328326e+000,
							-3.9973311156433828e+000 };
				} else {
					// [a,b] =butter(2,[.00004 ,.0020], 'bandpass')
					b = new double[] { 0.00355258014728165, 0, -0.00710516029456329, 0, 0.00355258014728165 };
					a = new double[] { 0.838670728317484, -3.50152501965464, 5.48701236858124, -3.82415805434902 };
				}
			} else if (sampleTimeInUS == 2000) {
				b = new double[] { 3.543613214051916000e-006, 0.000000000000000000e+000, -7.087226428103832100e-006,
						0.000000000000000000e+000,
						3.543613214051916000e-006 };
				a = new double[] { 9.946827274808930400e-001, -3.984021425337335400e+000, 5.983994634684961500e+000,
						-3.994655936788726400e+000 };
			} else if (sampleTimeInUS == 4000) {
				b = new double[] { 1.413678967222108300e-005, 0.000000000000000000e+000, -2.827357934444216600e-005,
						0.000000000000000000e+000,
						1.413678967222108300e-005 };
				a = new double[] { 9.893937283496260200e-001, -3.968074508050931200e+000, 5.967967563707222900e+000,
						-3.989286783370928000e+000 };
			} else if (sampleTimeInUS == 10000) {
				b = new double[] { 8.765554878241275500e-005, 0.000000000000000000e+000, -1.753110975648255100e-004,
						0.000000000000000000e+000,
						8.765554878241275500e-005 };
				a = new double[] { 9.736948719763162100e-001, -3.920424419893688400e+000, 5.919760094458148100e+000,
						-3.973030521933406300e+000 };
			} else if (sampleTimeInUS == 100000) {
				b = new double[] { 7.820208033498339900e-003, 0.000000000000000000e+000, -1.564041606699668000e-002,
						0.000000000000000000e+000,
						7.820208033498339900e-003 };
				a = new double[] { 7.660066009432644500e-001, -3.240903236405166400e+000, 5.180304455399886300e+000,
						-3.705188856470422700e+000 };
			} else if (sampleTimeInUS == 200000) {
				b = new double[] { 2.785976611713791500e-002, 0.000000000000000000e+000, -5.571953223427583000e-002,
						0.000000000000000000e+000,
						2.785976611713791500e-002 };
				a = new double[] { 5.869195080611890900e-001, -2.565087283826984300e+000, 4.346244802380466500e+000,
						-3.364981394885388200e+000 };
			} else if (sampleTimeInUS == 1000000) {
				b = new double[] { 3.913357725017684300e-001, 0.000000000000000000e+000, -7.826715450035368600e-001,
						0.000000000000000000e+000,
						3.913357725017684300e-001 };
				a = new double[] { 1.958157126558330000e-001, -1.387778780781445700e-016, -3.695273773512407500e-001,
						0.000000000000000000e+000 };
			}

			if (b == null || a == null)
				return;

			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			double endTime = CurrentOpenData.getInstance().getEndTimeInUS();

			double curTime = startTime;
			boolean is5fsOrAms = Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("5fs")
					|| Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")
					|| Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("bin");

			JFrame frame = MainFrame.getInstance().getMainFrame();
			ProgressMonitor progress = new ProgressMonitor(frame, "Generating Filtered DZ", null, 0, (int) fL);
			for (long i = 0; i < fL; i += nRead) {

				progress.setProgress((int) i / 3);
				bb.position(0);
				sb.position(0);
				nRead = ifC.read(bb);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				if (firstrun == true) {
					double vl = realConstant + realSlope * samp[0];
					for (int q = 0; q < backBuffer.length; q++)
						backBuffer[q] = vl;
					for (int q = 0; q < forBuffer.length; q++)
						forBuffer[q] = 0;
					firstrun = false;
				}
				for (int q = 0; q < nSRead; q++) {

					if (!is5fsOrAms) {
						if ((curTime > (startTime + 10000000)) && (curTime < (endTime - 10000000))) { // 20 seconds
																										// window

							AmsLabel lbl = new AmsLabel(curTime - 10000000, curTime + 10000000, false, 0.0, "");
							double HR = lbl.getAverage(true);
							boolean isArtefact = aSet.isArtefactBetweenTimes(curTime - 10000000, curTime + 10000000); // If
																														// the
																														// Label
																														// has
																														// artefatcs

							if (isArtefact) {
								boolean isArefact_previousLabel = aSet.isArtefactBetweenTimes(curTime - 80000000,
										curTime - 60000000); // Go to the previous label to detect HR
								if (!isArefact_previousLabel) {
									AmsLabel lbl1 = new AmsLabel(curTime - 80000000, curTime - 60000000, false, 0.0,
											"");
									HR = lbl1.getAverage(true);
								} else {
									HR = 50;
								}
							}

							if (HR < 100) {

								b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
										8.8712025778419194e-007 };
								a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000,
										5.9920000556328326e+000, -3.9973311156433828e+000 };

							} else if (HR > 100 && HR < 130) { // HR > 100 RR <= 45 // 0.1 to 0.75 Hz => [b,a] =
																// butter(2,[0.0002,0.0015], 'bandpass')

								b = new double[] { 4.15782671645446e-006, 0, -8.31565343290892e-006, 0,
										4.15782671645446e-006 };
								a = new double[] { 0.994240899745738, -3.982700171491988, 5.982677626703472,
										-3.994218354948480 };

							} else if (HR > 130 && HR < 160) { // RR <= 50 => 0.1 to 0.83(50/60) => [b,a] =
																// butter(2,[0.0002,0.00166], 'bandpass')

								b = new double[] { 0.052424469906870e-004, 0, -0.104848939813740e-004, 0,
										0.052424469906870e-004 };
								a = new double[] { 0.993534383544242, -3.980575659019019, 5.980548146230042,
										-3.993506870744562 };

							} else if (HR > 160) { // RR <= 55 => 0.1 to 0.91 (55/60) => [b,a] =
													// butter(2,[0.0002,0.00182], 'bandpass')

								b = new double[] { 0.064522486837612e-004, 0, -0.129044973675225e-004, 0,
										0.064522486837612e-004 };
								a = new double[] { 0.992828369399297, -3.978452152950424, 5.978419171951428,
										-3.992795388387441 };
							}

						}
					}

					curTime += sampleTimeInUS;
					double vl = realConstant + realSlope * samp[q];
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

					double newVal = ((val - realConstant) / realSlope);
					int shortVal = (int) newVal;

					if (newVal < Integer.MIN_VALUE) {
						shortVal = Integer.MIN_VALUE;
					} else if (newVal > Integer.MAX_VALUE) {
						shortVal = Integer.MAX_VALUE;
					}

					outBuf[q] = shortVal;
				}
				nOut = nSRead;

				obb.clear();
				osb.clear();
				osb.put(outBuf, 0, nOut);
				obb.limit(4 * nOut);
				ofC.write(obb);
			}
			// progress.close();
			ofC.close();
			ifC.close();

			long pos = fL;
			ifC = (new FileInputStream(tempFile)).getChannel();
			ofC = (new FileOutputStream(filteredFile)).getChannel();

			curTime = startTime;
			firstrun = true;

			for (long i = 0; i < fL; i += nRead) {

				progress.setProgress((int) (fL / 3 + i / 3));
				bb.position(0);
				sb.position(0);
				long newPos = pos - size;
				if (newPos < 0)
					newPos = 0;
				nRead = (int) (pos - newPos);
				pos -= size;
				ifC.read(bb, newPos);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sb.get(samp, 0, nSRead);
				if (firstrun == true) {
					double vl = realConstant + realSlope * samp[nSRead - 1];
					for (int q = 0; q < backBuffer.length; q++)
						backBuffer[q] = vl;
					for (int q = 0; q < forBuffer.length; q++)
						forBuffer[q] = 0;
					firstrun = false;
				}
				for (int q = 0; q < nSRead; q++) {
					if (!is5fsOrAms) {
						if ((curTime > (startTime + 10000000)) && (curTime < (endTime - 10000000))) { // 6 seconds
																										// window
							AmsLabel lbl = new AmsLabel(curTime - 10000000, curTime + 10000000, false, 0.0, "");
							double HR = lbl.getAverage(true);
							boolean isArtefact = aSet.isArtefactBetweenTimes(curTime - 10000000, curTime + 10000000); // Label
																														// has
																														// artefatcs

							if (isArtefact) {
								boolean isArefact_previousLabel = aSet.isArtefactBetweenTimes(curTime - 80000000,
										curTime - 60000000); // Go to the previous label to detect HR
								if (!isArefact_previousLabel) {
									AmsLabel lbl1 = new AmsLabel(curTime - 80000000, curTime - 60000000, false, 0.0,
											"");
									HR = lbl1.getAverage(true);
								} else {
									HR = 50;
								}
							}

							if (HR <= 100) {

								b = new double[] { 8.8712025778419194e-007, 0, -1.7742405155683839e-006, 0,
										8.8712025778419194e-007 };
								a = new double[] { 9.9733782013963035e-001, -3.9920067601265901e+000,
										5.9920000556328326e+000, -3.9973311156433828e+000 };

							} else if (HR > 100 && HR <= 130) { // HR > 100 RR <= 45 // 0.1 to 0.75 Hz => [b,a] =
																// butter(2,[0.0002,0.0015], 'bandpass')

								b = new double[] { 4.15782671645446e-006, 0, -8.31565343290892e-006, 0,
										4.15782671645446e-006 };
								a = new double[] { 0.994240899745738, -3.982700171491988, 5.982677626703472,
										-3.994218354948480 };

							} else if (HR > 130 && HR <= 160) { // RR <= 50 => 0.1 to 0.83(50/60) => [b,a] =
																// butter(2,[0.0002,0.00166], 'bandpass')

								b = new double[] { 0.052424469906870e-004, 0, -0.104848939813740e-004, 0,
										0.052424469906870e-004 };
								a = new double[] { 0.993534383544242, -3.980575659019019, 5.980548146230042,
										-3.993506870744562 };

							} else if (HR > 160) { // RR <= 55 => 0.1 to 0.91 (55/60) => [b,a] =
													// butter(2,[0.0002,0.00182], 'bandpass')

								b = new double[] { 0.064522486837612e-004, 0, -0.129044973675225e-004, 0,
										0.064522486837612e-004 };
								a = new double[] { 0.992828369399297, -3.978452152950424, 5.978419171951428,
										-3.992795388387441 };
							}
						}
					}
					curTime += sampleTimeInUS;
					double vl = realConstant + realSlope * samp[nSRead - q - 1];
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
				ofC.write(obb, newPos);
			}

			ofC.close();
			ifC.close();

			if (respiration) {
				int nToSkip = 100000 / sampleTimeInUS;
				if (nToSkip < 1)
					nToSkip = 1;
				int skipCounter = 0;

				ifC = (new FileInputStream(filteredFile)).getChannel();
				ofC = (new FileOutputStream(fdzFile)).getChannel();

				for (long i = 0; i < fL; i += nRead) {
					progress.setProgress((int) (2. * fL / 3. + i / 3.));
					bb.position(0);
					sb.position(0);
					nRead = ifC.read(bb);
					int nSRead = nRead / 4;
					if (nRead < 1)
						break;
					sb.get(samp, 0, nSRead);
					nOut = 0;
					for (int q = 0; q < nSRead; q++) {
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
				}
				ifC.close();
				ofC.close();
			}
			progress.close();
			/*
			 * frame.toFront();
			 * frame.requestFocus();
			 */
			tempFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("filter " + inFile + " took (" + timer.getTime() / 1000. + " sec)");
	}
}
