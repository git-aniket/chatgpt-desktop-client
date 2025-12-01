package nl.vu.psy.ams.suite.data.qrs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.RingBuffer;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Uses the binary ECG file to find artefacts:
 * - If the signal clips
 * - If the signal flatlines
 */
public class ECGArtefactDetector {

	private long sampleTimeInUS;
	private File ECGFile, tickFile;
	private long startTime;
	private boolean useTicks = false;

	public ECGArtefactDetector() {
		this(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us(), new File(CurrentOpenData
						.getInstance().getFilePath(), "ECG.bin"),
				new File(CurrentOpenData.getInstance().getFilePath(), "TicksA.bin"));
	}

	public ECGArtefactDetector(long startTime, long sampleTimeInUS, File ECGFile) {
		this.sampleTimeInUS = sampleTimeInUS;
		this.ECGFile = ECGFile;
		this.startTime = startTime;
	}

	public ECGArtefactDetector(long startTime, long sampleTimeInUS, File ECGFile, File tickFile) {
		this.sampleTimeInUS = sampleTimeInUS;
		this.ECGFile = ECGFile;
		this.tickFile = tickFile;
		this.startTime = startTime;
	}

	public Collection<AmsLabel> getArtefacts(int nBitsECG) {
		Timer timer = new Timer();
		timer.start();
		ArrayList<AmsLabel> retArray = new ArrayList<AmsLabel>();
		long nShorts = ECGFile.length() / 4;
		FileInputStream is = null, isT = null;
		FileChannel fc = null, fcT = null;
		boolean artefactStarted = false;
		double lTime = 0, rTime = 0;
		long curTime = startTime, prevTime = startTime;
		int artType = 0;
		int skip = (int) Math.round(10000. / sampleTimeInUS);
		int nSamplesIn2S = (int) (2000000 / (skip * sampleTimeInUS));
		RingBuffer dataBuffer = new RingBuffer(nSamplesIn2S);
		for (int i = 0; i < nSamplesIn2S - 2; i++) {
			dataBuffer.add(0.);
		}
		dataBuffer.add(Double.NEGATIVE_INFINITY);
		dataBuffer.add(Double.POSITIVE_INFINITY);
		int size = 1048576;
		int nShortsRead = size / 4; // (int) (Math.floor(size / (4 * skip)) * skip);
		int nBytesRead = 4 * nShortsRead;
		int nRead = 0;
		ByteBuffer bb = ByteBuffer.allocate(nBytesRead);
		IntBuffer sb = bb.asIntBuffer();
		ByteBuffer bbT = ByteBuffer.allocate(nBytesRead);
		IntBuffer sbT = bbT.asIntBuffer();
		int curVal;
		int[] buffer = new int[nShortsRead];
		int[] bufferT = new int[nShortsRead];
		double[] minmax = new double[2];
		dataBuffer.getMinMax(minmax);
		double oldVal;
		int clipVal = (int) Math.pow(2, nBitsECG - 1);
		long pos = 0;
		if (tickFile.exists())
			useTicks = true;
		try {
			is = new FileInputStream(ECGFile);
			fc = is.getChannel();
			if (useTicks) {
				isT = new FileInputStream(tickFile);
				fcT = isT.getChannel();
			}
			for (long i = 0; i < nShorts; i += nRead) {
				long newPos = pos;
				pos += size;
				if (pos > nShorts)
					pos = nShorts;
				nRead = (int) (pos - newPos);
				if (nRead < 1)
					break;
				nRead /= 4;
				if (useTicks) {
					bbT.clear();
					sbT.clear();
					fcT.read(bbT, newPos);
					if (nRead < 1)
						break;
					sbT.get(bufferT, 0, nRead);
					for (int k = 0; k < nRead; k++) {
						long timeDiff = bufferT[k] * 1000L - prevTime;
						if (timeDiff > 100000L)
							retArray.add(AmsLabel.generateECGArtefact(prevTime, bufferT[k] * 1000L, false, 0.0,
									"Missing ticks"));
						if (timeDiff < 0)
							retArray.add(AmsLabel.generateECGArtefact(bufferT[k] * 1000L, prevTime, false, 0.0,
									"Reversed ticks"));
						prevTime = bufferT[k] * 1000L;
					}
				}
				bb.clear();
				sb.clear();
				fc.read(bb, newPos);
				sb.get(buffer, 0, nRead); // Relative bulk get method
											// This method transfers the long from this buffer to the given array
											// LongBuffer.get(long[] array, int offset, int length)
											// buffer size = 524288, nRead = 524280
				for (int j = 0; j < nRead; j += skip) {
					if (useTicks)
						curTime = bufferT[j] * 1000L; // ms to us
					else
						curTime += skip * sampleTimeInUS;
					curVal = buffer[j]; // buffer = long array of size 524288
					oldVal = dataBuffer.getValue();
					dataBuffer.add(curVal);
					if (oldVal == minmax[0] || oldVal == minmax[1]) {
						dataBuffer.getMinMax(minmax);
					} else {
						if (curVal < minmax[0])
							minmax[0] = curVal;
						if (curVal > minmax[1])
							minmax[1] = curVal;
					}

					if (curVal >= 0.98 * (clipVal - 1) || curVal <= 0.98 * -clipVal) {
						artType = (artType | 1);
						if (artefactStarted == true) {
							rTime = curTime;// - 1000000;
						} else {
							artefactStarted = true;
							lTime = curTime;// - 2000000;
							rTime = lTime;
						}
					} else {
						if (minmax[1] - minmax[0] < 1024) {
							artType = (artType | 2);
							if (artefactStarted == true) {
								rTime = curTime;// - 1000000;
							} else {
								artefactStarted = true;
								lTime = curTime - 2000000;
								rTime = lTime;
							}
						} else {
							if (artefactStarted == true) {
								if (curTime - rTime > 3000000) { // 3 seconds // curtime = curtime + 10 seconds
									int nTypes = 0;
									String reason = "";
									if ((artType & 1) == 1) {
										reason += "Clipping ECG values";
										nTypes++;
									}
									if ((artType & 2) == 2) {
										if (nTypes > 0)
											reason += " and ";
										reason += "Flat ECG signal";
										nTypes++;
									}
									artType = 0;
									if (lTime < rTime)
										retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
									artefactStarted = false;
								}
							}
						}
					}
				}
			}
			if (artefactStarted == true) {
				int nTypes = 0;
				String reason = "";
				if ((artType & 1) == 1) {
					reason += "Clipping ECG values";
					nTypes++;
				}
				if ((artType & 2) == 2) {
					if (nTypes > 0)
						reason += " and ";
					reason += "Flat ECG signal";
					nTypes++;
				}
				artType = 0;
				if (lTime < rTime)
					retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
				artefactStarted = false;
			}
		} catch (IOException e) {

		} finally {
			if (is != null)
				try {
					is.close();
					/*
					 * frame.toFront();
					 * frame.requestFocus();
					 */
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (isT != null)
				try {
					isT.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		// find motility artefacts
		pos = 0;
		if (AppSettings.getInstance().getIntProperty(Settings.MOTARTEFACTS) == 1) {
			File motFile = new File(CurrentOpenData.getInstance().getFilePath(), "MotilityIntensity.dbin");
			tickFile = new File(CurrentOpenData.getInstance().getFilePath(), "TicksM.bin");
			long nDoubles = ECGFile.length() / 4;
			is = null;
			isT = null;
			artefactStarted = false;
			lTime = 0;
			rTime = 0;
			curTime = startTime;
			prevTime = startTime;
			int nDoublesRead = size / 4;
			nBytesRead = 8 * nDoublesRead;
			nRead = 0;
			bb = ByteBuffer.allocate(nBytesRead);
			IntBuffer db = bb.asIntBuffer();
			int[] bufferM = new int[nDoublesRead];
			bufferT = new int[nDoublesRead];
			double motMaxVal = 1.3;
			double curValD;
			// long lindex=0, rindex=0;
			useTicks = false;
			if (tickFile.exists())
				useTicks = true;
			try {
				is = new FileInputStream(motFile);
				fc = is.getChannel();
				if (useTicks) {
					isT = new FileInputStream(tickFile);
					fcT = isT.getChannel();
				}
				for (long i = 0; i < nDoubles; i += nRead) {
					long newPos = pos;
					pos += size;
					if (pos > nDoubles)
						pos = nDoubles;
					if (useTicks) {
						bbT.clear();
						sbT.clear();
						fcT.read(bbT, newPos);
						nRead /= 4;
						if (nRead < 1)
							break;
						sbT.get(bufferT, 0, nRead);
					}
					bb.clear();
					db.clear();
					fc.read(bb, newPos);
					nRead /= 2;
					if (nRead == 0)
						break;
					db.get(bufferM, 0, nRead); // Relative bulk get method
												// This method transfers the long from this buffer to the given array
												// LongBuffer.get(long[] array, int offset, int length)
												// buffer size = 524288, nRead = 524280
					for (int j = 0; j < nRead; j += skip) {
						if (useTicks)
							curTime = bufferT[j] * 1000L; // ms to us
						else
							curTime += skip * sampleTimeInUS;
						curValD = bufferM[j] / 1000.0; // buffer = long array of size 524288
						if (curValD >= motMaxVal || curValD <= -motMaxVal) {
							if (artefactStarted == true) {
								rTime = curTime;// - 1000000;
								// rindex = i;
							} else {
								artefactStarted = true;
								lTime = curTime;// - 2000000;
								rTime = lTime;
								// lindex = i;
							}
						} else {
							if (artefactStarted == true) {
								if (curTime - rTime > 10000000) { // 10 seconds
									String reason = "Too much movement";
									if (rTime > lTime + 3000000)
										retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
									// else if (rTime <= lTime)
									// System.out.println(lindex + " " + rindex);
									artefactStarted = false;
								}
							}
						}
					}
				}
				if (artefactStarted == true) {
					String reason = "Too much movement";
					retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
					artefactStarted = false;
				}
			} catch (IOException e) {

			} finally {
				if (is != null)
					try {
						is.close();
						/*
						 * frame.toFront();
						 * frame.requestFocus();
						 */
					} catch (IOException e) {
						e.printStackTrace();
					}
				if (isT != null)
					try {
						isT.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("ECGArtefactDetector took (" + timer.getTime() / 1000. + " sec)");
		return retArray;
	}

	public Collection<AmsLabel> getArtefactsFromBeats() {
		Timer timer = new Timer();
		timer.start();
		ArrayList<AmsLabel> retArray = new ArrayList<AmsLabel>();
		// if window size / 2 + 1 surrounding beats are suspicious/deviant make artefact
		// around beats
		int windowsize = 11;
		boolean artefactStarted = false, cfound = false;
		double lTime = 0, rTime = 0;
		int susp = 0, consecutive = 0, firstIndex = 0, lastIndex = windowsize - 1;
		ArrayList<ECGBeat> beats = new ArrayList<ECGBeat>(CurrentOpenData.getInstance().getBeatSet(0).getBeats());
		if (beats.size() < windowsize)
			return retArray;
		ECGBeat[] beatBuffer = new ECGBeat[windowsize];
		for (int q = 0; q < beatBuffer.length; q++)
			beatBuffer[q] = beats.get(q);
		for (int i = windowsize / 2 + 1; i < beats.size() - (windowsize / 2 + 1); i++) {
			for (int q = 0; q < beatBuffer.length; q++) {
				if (beatBuffer[q].getIBISuspicion() > 500) {
					susp++;
					consecutive++;
				} else {
					consecutive = 0;
				}
				if (consecutive > 4)
					cfound = true;
			}
			if (susp > windowsize / 2 || cfound) {
				for (int q = beatBuffer.length - 1; q >= 0; q--) {
					if (beatBuffer[q].getIBISuspicion() > 500) {
						lastIndex = q;
						break;
					}
				}
				if (artefactStarted == true) {
					rTime = beatBuffer[lastIndex].getRPeakTime() + 200000;
				} else {
					for (int q = 0; q < beatBuffer.length; q++) {
						if (beatBuffer[q].getIBISuspicion() > 500) {
							firstIndex = q;
							break;
						}
					}
					artefactStarted = true;
					lTime = beatBuffer[firstIndex].getRPeakTime() - 200000;
					rTime = beatBuffer[lastIndex].getRPeakTime() + 200000;
				}
			} else {
				if (artefactStarted == true) {
					if (beatBuffer[lastIndex].getRPeakTime() - rTime > 10000000) { // 10 seconds // curtime = curtime +
																					// 10 seconds
						String reason = "Suspicious beats";
						if (lTime < rTime)
							retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
						artefactStarted = false;
					}
				}
			}
			for (int z = 0; z < beatBuffer.length - 1; z++)
				beatBuffer[z] = beatBuffer[z + 1];
			beatBuffer[beatBuffer.length - 1] = beats.get(i + windowsize / 2 + 1);
			susp = 0;
			consecutive = 0;
			cfound = false;
		}
		if (artefactStarted == true) {
			String reason = "Suspicious beats";
			if (lTime < rTime)
				retArray.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
			artefactStarted = false;
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("ECGArtefact detecting from beats took (" + timer.getTime() / 1000. + " sec)");
		return retArray;
	}
}
