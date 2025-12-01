package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.File;
import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.Ams5fsFile;
import nl.vu.psy.ams.suite.data.qrs.ECGArtefactDetector;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;

/*
 * Set that provides extra utitlities for handling artefacts.
 * Artefacts are labels in principle, but need a bit of extra
 * work.
 */
public class ArtefactSet extends LabelSet {

	public boolean areBeatsUnderArtefacts() {
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		for (AmsLabel l : labels) {
			if (bSet.subSet(l.getLeftTime(), l.getRightTime()).isEmpty() == false)
				return true;
		}
		return false;
	}

	public void deleteArtefact(AmsLabel art) {
		CurrentOpenData.getInstance().setDirty(true);
		labels.remove(art);
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		bSet.resetFirstInSeries();
		bSet.recheckSuspiciousIBIS();
	}

	public void deleteBeatsUnderArtefacts() {
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
		for (AmsLabel l : labels) {
			bSet.removeSubSet(l.getLeftTime(), l.getRightTime());
			// --- While creating artefact, include this information for respiration cycle
			// calculations
			rSet.rescan(l.getLeftTime() - 5000000, l.getRightTime() + 5000000);
		}
		bSet.resetFirstInSeries();
		bSet.recalculateAllSuspiciousLevels();
	}

	public void importFromFile(File fl) {
		try {
			Ams5fsFile amsFile = new Ams5fsFile();
			amsFile.open(fl);
			labels.clear();
			while (amsFile.isAtEndOfFile() == false) {
				Ams5fsPacket tempPacket = amsFile.ReadNextPacket();
				if (tempPacket.getwTag() == Ams5fsPacket.PACKET_TYPE_LABELEX) {
					if (tempPacket.getlType() == 1) {
						labels.add(AmsLabel.generateECGArtefact(tempPacket.getDwBegin_ms() * 1000,
								tempPacket.getDwEnd_ms() * 1000, false, 0.0,
								"Imported from Ams Inventory File"));
					}
				}
			}
			amsFile.close();
			BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
			bSet.resetFirstInSeries();
			bSet.recheckSuspiciousIBIS();
			CurrentOpenData.getInstance().setDirty(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isArtefactBetweenTimes(double lT, double rT) {
		for (AmsLabel l : labels) {
			if (l.getLeftTime() > lT && l.getLeftTime() < rT)
				return true;
			if (l.getRightTime() > lT && l.getRightTime() < rT)
				return true;
		}
		return false;
	}

	public boolean isLongArtefactBetweenTimes(double lT, double rT) {
		double longestAllowed = 5000000;
		double artL;
		for (AmsLabel l : labels) {
			if (l.getLeftTime() > lT && l.getLeftTime() < rT) {
				if (l.getRightTime() < rT) {
					artL = l.getRightTime() - l.getLeftTime();
				} else {
					artL = rT - l.getLeftTime();
				}
				if (artL > longestAllowed)
					return true;
			}
			if (l.getRightTime() > lT && l.getRightTime() < rT) {
				if (l.getLeftTime() > lT) {
					artL = l.getRightTime() - l.getLeftTime();
				} else {
					artL = l.getRightTime() - lT;
				}
				if (artL > longestAllowed)
					return true;
			}
		}
		return false;
	}

	public void reCalculate(int nBitsECG) {
		CurrentOpenData.getInstance().setDirty(true);
		ECGArtefactDetector det = new ECGArtefactDetector();
		labels = new TreeSet<AmsLabel>(det.getArtefacts(nBitsECG));
	}

	public void reCalculateFromPeaks() {
		ECGArtefactDetector det = new ECGArtefactDetector();
		TreeSet<AmsLabel> templabels = new TreeSet<AmsLabel>(det.getArtefactsFromBeats());
		labels.addAll(templabels);
		cleanup();
		CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
	}

	public void cleanup() {
		TreeSet<AmsLabel> toRemove = new TreeSet<AmsLabel>();
		TreeSet<AmsLabel> toAdd = new TreeSet<AmsLabel>();
		boolean done = false;
		while (!done) {
			for (AmsLabel l : labels) {
				NavigableSet<AmsLabel> tailSet = labels.tailSet(l, false);
				if (tailSet.isEmpty())
					break;
				AmsLabel l2 = tailSet.getFirst();
				if (l2.getLeftTime() < l.getRightTime() && l2.getRightTime() < l.getRightTime()) // full overlap
					toRemove.add(l2);
				else if (l2.getLeftTime() < l.getRightTime() + 3000000) { // partial overlap or less the 3s apart
					double lTime = l.getLeftTime();
					double rTime = l2.getRightTime();
					String key = "ECGArtefact";
					String reason = l.getAttributes().get(key);
					if (reason == null) {
						key = "ECG Artefact";
						reason = l.getAttributes().get(key);
					}
					key = "ECGArtefact";
					String reason2 = l.getAttributes().get(key);
					if (reason2 == null) {
						key = "ECG Artefact";
						reason2 = l.getAttributes().get(key);
					}
					if (!reason.equals(reason2))
						reason = reason + " / " + reason2;
					toRemove.add(l);
					toRemove.add(l2);
					toAdd.add(AmsLabel.generateECGArtefact(lTime, rTime, false, 0.0, reason));
				}
			}
			labels.removeAll(toRemove);
			labels.addAll(toAdd);
			if (toRemove.size() == 0 && toAdd.size() == 0)
				done = true;
			toRemove.clear();
			toAdd.clear();
		}
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		bSet.resetFirstInSeries();
		bSet.recalculateAllSuspiciousLevels();
	}

	@Override
	public void removeLabel(AmsLabel lab) {
		labels.remove(lab);
		BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
		bSet.resetFirstInSeries();
		bSet.recalculateAllSuspiciousLevels();
		CurrentOpenData.getInstance().setDirty(true);
	}

}
