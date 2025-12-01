package nl.vu.psy.ams.suite.data.sclcycle;

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
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.RingBuffer;
import nl.vu.psy.ams.suite.tools.Timer;

/*
 * Uses the binary SCL file to find artefacts:
 * - If the signal clips
 * - If the signal flatlines
 */
public class SCLArtefactDetector {

	private long sampleTimeInUS;
	private File SCLFile;
	private long startTime;
	private double realSlope;
	private double realConstant;

	public SCLArtefactDetector() {
		this(CurrentOpenData.getInstance().getStartTimeInUS(), 100000, new File(CurrentOpenData
				.getInstance().getFilePath(), "SCL.bin"));
	}

	public SCLArtefactDetector(long startTime, long sampleTimeInUS, File SCLFile) {
		this.sampleTimeInUS = sampleTimeInUS;
		this.SCLFile = SCLFile;
		this.startTime = startTime;
	}

	public Collection<AmsLabel> getArtefacts() {

		Timer timer = new Timer();
		timer.start();
		ArrayList<AmsLabel> retArray1 = new ArrayList<AmsLabel>();
		long nShorts = SCLFile.length() / 4;
		FileInputStream is = null;
		FileChannel fc = null;
		boolean artefactStarted = false;
		double lTime = 0, rTime = 0;
		double curTime = startTime;
		int artType = 0;
		int skip = (int) Math.round(1000000. / sampleTimeInUS);
		int nSamplesIn2S = (int) (2000000 / (sampleTimeInUS));

		RingBuffer dataBuffer = new RingBuffer(nSamplesIn2S);
		for (int i = 0; i < nSamplesIn2S - 2; i++) {
			dataBuffer.add(0.);
		}
		dataBuffer.add(Double.NEGATIVE_INFINITY);
		dataBuffer.add(Double.POSITIVE_INFINITY);

		int size = 1048576;
		int nShortsRead = size / 4;
		int nBytesRead = 4 * nShortsRead;
		int nRead = 0;
		ByteBuffer bb = ByteBuffer.allocate(nBytesRead);
		IntBuffer sb = bb.asIntBuffer();
		int curVal;
		int[] buffer = new int[nShortsRead];
		double[] minmax = new double[2];
		dataBuffer.getMinMax(minmax);
		double oldVal;
		long pos = 0;

		try {
			is = new FileInputStream(SCLFile);
			fc = is.getChannel();

			Ams7fsChannelInfo s = null;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("SCL");
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
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (long i = 0; i < nShorts; i += nRead) {
				bb.clear();
				sb.clear();
				long newPos = pos;
				pos += size;
				if (pos > nShorts * 4)
					pos = nShorts * 4;
				fc.read(bb, newPos);
				nRead /= 4;
				if (nRead < 1)
					break;
				sb.get(buffer, 0, nRead);

				for (int j = 0; j < nRead; j += skip, curTime += skip * sampleTimeInUS) {

					curVal = buffer[j];
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

					if (curVal >= 0.8 * Integer.MAX_VALUE || curVal <= Integer.MIN_VALUE) {
						artType = (artType | 1);
						if (artefactStarted == true) {
							rTime = curTime;
						} else {
							artefactStarted = true;
							lTime = curTime;// - 500000;
							rTime = lTime;
						}
					} else {

						if ((realConstant + realSlope * curVal) < 0.3) {
							artType = (artType | 2);
							if (artefactStarted == true) {
								rTime = curTime;
							} else {
								artefactStarted = true;
								lTime = curTime;
								rTime = lTime;
							}

						} else {
							if (artefactStarted == true) {

								int nTypes = 0;
								String reason = "";
								if ((artType & 1) == 1) {
									reason += "Clipping SCL values";
									nTypes++;
								}
								if ((artType & 2) == 2) {
									if (nTypes > 0)
										reason += " and ";
									reason += "Flat SCL signal";
									nTypes++;
								}
								artType = 0;
								retArray1.add(AmsLabel.generateSCLArtefact(lTime, rTime, false, 0.0, reason));

								artefactStarted = false;

							}
						}
					}
				}
			}
			if (artefactStarted == true) {
				int nTypes = 0;
				String reason = "";
				if ((artType & 1) == 1) {
					reason += "Clipping SCL values";
					nTypes++;
				}
				if ((artType & 2) == 2) {
					if (nTypes > 0)
						reason += " and ";
					reason += "Flat SCL signal";
					nTypes++;
				}
				artType = 0;
				retArray1.add(AmsLabel.generateSCLArtefact(lTime, rTime, false, 0.0, reason));
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
		}
		timer.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("SCLArtefactDetector took (" + timer.getTime() / 1000. + " sec)");
		return retArray1;
	}
}
