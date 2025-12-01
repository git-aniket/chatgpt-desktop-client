package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gitlab.teuniz.EDFwriter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.tools.Utils;

public class BinaryFileSet {

	ArrayList<String> names = new ArrayList<String>();
	ArrayList<Integer> sampleTimesInUS = new ArrayList<Integer>();
	ArrayList<File> baseFiles = new ArrayList<File>();
	ArrayList<Long> totalNumbersOfShorts = new ArrayList<Long>();
	ArrayList<Double> realSlopes = new ArrayList<Double>();
	ArrayList<Double> realConstants = new ArrayList<Double>();
	ArrayList<Integer> upperBounds = new ArrayList<Integer>();
	ArrayList<Integer> lowerBounds = new ArrayList<Integer>();
	ArrayList<Double> upperValues = new ArrayList<Double>();
	ArrayList<Double> lowerValues = new ArrayList<Double>();
	ArrayList<String> szUnits = new ArrayList<String>();
	private static Logger logger = LogManager.getLogger(BinaryFile.class.getName());

	public BinaryFileSet(String[] channelNames) {
		for (String channelName : channelNames) {
			this.names.add(channelName);
			String chanName = channelName;
			if (channelName.equals("FILTDZ")) {
				chanName = "DZ";
			}
			if (channelName.equals("FILTSCL")) {
				chanName = "SCL";
			}
			if (channelName.equals("FILTECG")) {
				chanName = "ECG";
			}
			// ---------------- To add FilteredDZDT Signal---------------------------
			if (channelName.equals("FILTDZDT")) {
				chanName = "DZDT";
			}
			// ---------------------------------------------------------------------

			int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
			sampleTimesInUS.add(sampleTimeInUS);
			try {
				sampleTimesInUS.set(sampleTimesInUS.size() - 1, (int) (sampleTimeInUS
						* CurrentOpenData.getInstance().getChannelInfoFromID(chanName).getDwDivider()));
			} catch (Exception e1) {
				// e1.printStackTrace();
			}
			baseFiles.add(new File(CurrentOpenData.getInstance().getFilePath(), channelName + ".bin"));
			totalNumbersOfShorts.add(baseFiles.get(baseFiles.size() - 1).length() / 4);

			Ams7fsChannelInfo chan;
			try {
				chan = CurrentOpenData.getInstance().getChannelInfoFromID(chanName);
				int lowerBound, upperBound;
				double lowerValue, upperValue, realSlope;
				// ----- For AMS Files - Files recorded using old VU-AMS Device---------
				if (chanName.equals("Z0")
						&& (Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")
								|| (CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS1")))) {
					lowerBound = 0;
					upperBound = 1024;
					lowerValue = 0.39;
					upperValue = 24.25;
					// ---------------------------------------------------------------------
				} else {
					lowerBound = (int) -Math.pow(2, chan.getnBits() - 1);
					upperBound = (int) Math.pow(2, chan.getnBits() - 1) - 1;
					lowerValue = (double) chan.getlMinValue() / chan.getlMinMaxDivider();
					upperValue = (double) chan.getlMaxValue() / chan.getlMinMaxDivider();
				}
				upperBounds.add(upperBound);
				lowerBounds.add(lowerBound);
				szUnits.add(chan.getSzUnit());

				if (chan.getRealSlope() == 0) {
					realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
					realConstants.add(lowerValue - realSlope * lowerBound);
				} else {
					realSlope = chan.getRealSlope();
					realConstants.add(chan.getRealConstant());
					lowerValue = lowerBound * realSlope + chan.getRealConstant();
					upperValue = upperBound * realSlope + chan.getRealConstant();
				}
				realSlopes.add(realSlope);
				upperValues.add(upperValue);
				lowerValues.add(lowerValue);

			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}

	public double getRealValueFromSampleValue(double sampleValue, int ci) {
		return realSlopes.get(ci) * sampleValue + realConstants.get(ci);
	}

	public void outputsToEDF(File file, ArrayList<Double> sRatesInUS, List<String> channelnames, boolean ecgonly) {
		for (int index = 0; index < channelnames.size(); index++) {
			if (CurrentOpenData.getInstance().channelExists(channelnames.get(index)) == true) {
			} else {
				channelnames.remove(index);
				sRatesInUS.remove(index);
				logger.info("The selected channel " + channelnames.get(index)
						+ " does not exists or has not been recorded. " + ", "
						+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
			}
		}
		ArrayList<Integer> sRatesInHz = new ArrayList<Integer>();
		ArrayList<Integer> skips = new ArrayList<Integer>();
		ArrayList<DataInputStream> dis = new ArrayList<DataInputStream>();
		EDFwriter ew;
		try {
			ew = new EDFwriter(file.getPath(), EDFwriter.EDFLIB_FILETYPE_EDFPLUS, channelnames.size());
			double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
			String subjectname = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
			int[] startTimeI = Utils.getDateAndTimeInt(startTime);
			ew.setStartDateTime(startTimeI[0], startTimeI[1], startTimeI[2], startTimeI[3], startTimeI[4],
					startTimeI[5], startTimeI[6]);
			ew.setPatientName(subjectname);
			for (int i = 0; i < channelnames.size(); i++) {
				if (sRatesInUS.get(i) < sampleTimesInUS.get(i)) {
					sRatesInUS.set(i, (double) sampleTimesInUS.get(i));
					logger.info("The output sample time of channel " + channelnames.get(i)
							+ " is lesser than the input sample rate. " + " , " + "File name:_ "
							+ CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
				}
				skips.add((int) Math.round(sRatesInUS.get(i) / sampleTimesInUS.get(i)));
				sRatesInHz.add((int) Math.round(1000000 / sRatesInUS.get(i)));
				dis.add(new DataInputStream(new BufferedInputStream(new FileInputStream(baseFiles.get(i)))));
				ew.setSignalLabel(i, names.get(i));
				ew.setPhysicalMaximum(i, upperValues.get(i));
				ew.setPhysicalMinimum(i, lowerValues.get(i));
				ew.setDigitalMaximum(i, upperBounds.get(i));
				ew.setDigitalMinimum(i, lowerBounds.get(i));
				ew.setPhysicalDimension(i, szUnits.get(i));
				ew.setSampleFrequency(i, sRatesInHz.get(i));
			}
			// long fL = baseFile.length() / 2;
			double[] buf1 = new double[sRatesInHz.get(0)];
			double av = 0;
			int nInAv = 0;
			int count = 0, err, ci = 0;
			boolean done = false;
			// for (long j = 0; j < fL; j++) {
			while (done == false) {
				if (count == sRatesInHz.get(ci)) {
					err = ew.writePhysicalSamples(buf1);
					if (err != 0) {
						System.out.printf("writePhysicalSamples() returned error: %d\n", err);
					}
					ci++;
					if (ci >= channelnames.size())
						ci = 0;
					buf1 = new double[sRatesInHz.get(ci)];
					count = 0;
				}
				try {
					av += getRealValueFromSampleValue(dis.get(ci).readInt(), ci);
					nInAv++;
					if (nInAv == skips.get(ci)) {
						if (channelnames.get(ci).equals("ECG")
								&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))// JdH Eco
																												// said
																												// warn
																												// and
																												// convert
																												// /3.6
							buf1[count] = (av / 3.6) / nInAv;
						else
							buf1[count] = av / nInAv;
						count++;
						nInAv = 0;
						av = 0;
					}
				} catch (EOFException e) {
					done = true;
				}
			}
			/*
			 * err = ew.writePhysicalSamples(buf1);
			 * if(err != 0)
			 * {
			 * System.out.printf("writePhysicalSamples() returned error: %d\n", err);
			 * }
			 */
			AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
			AmsLabelConfiguration config = CurrentOpenData.getInstance().getLabelConfig();
			ArrayList<String> cats = config.getCategories();
			ArrayList<Ams5fsPacket> events = new ArrayList<Ams5fsPacket>();
			for (Ams5fsPacket p : CurrentOpenData.getInstance().getEvents()) {
				if (p.getlType() <= 2) {
					events.add(p);
				}
			}
			if (ecgonly == false) {
				if (events.isEmpty() == false) {
					for (Ams5fsPacket nextEvent : events)
						ew.writeAnnotation((nextEvent.getDwClockTick_ms() - (long) startTime / 1000) * 10, -1,
								Integer.toString(nextEvent.getlCode()));
				}
				for (AmsLabel nextLabel : lSet.getLabels()) {
					String labelString = "";
					Map<String, String> att = nextLabel.getAttributes();
					for (int q = 0; q < cats.size(); q++) {
						String val = att.get(cats.get(q));
						for (LabelValue lv : config.getConfig().get(cats.get(q))) {
							if (lv.getName().equals(val)) {
								labelString += Integer.toString(lv.getCode()) + "_";
							}
						}
					}
					Long start, end;
					start = (long) ((nextLabel.getLeftTime() - (long) startTime) / 100);
					end = (long) ((nextLabel.getRightTime() - nextLabel.getLeftTime()) / 100);
					ew.writeAnnotation(start, end, labelString);
				}
			}
			err = ew.close();
			if (err != 0) {
				System.out.printf("Closing edf file returned error: %d\n", err);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (com.gitlab.teuniz.EDFException e) {
			e.printStackTrace();
		} finally {
			for (DataInputStream d : dis)
				if (d != null) {
					try {
						d.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}
	}
}
