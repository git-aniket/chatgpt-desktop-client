package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.File;
import java.util.TreeSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.sclcycle.SCLArtefactDetector;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class SCLArtefactSet extends LabelSet {

	public void deleteArtefact(AmsLabel art) {
		labels.remove(art);
		CurrentOpenData.getInstance().setDirty(true);
	}

	public boolean areCyclesUnderArtefacts() {
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.EVENTBASED) == 1) {
			EventRelatedSCLSet sSet = CurrentOpenData.getInstance().getEventSCLSet();
			for (AmsLabel l : labels) {
				if (sSet.subSet(l.getLeftTime(), l.getRightTime()).isEmpty() == false)
					return true;
			}
			return false;
		} else {
			SkinConductanceSet lSet = CurrentOpenData.getInstance().getSCLSet();
			for (AmsLabel l : labels) {
				if (lSet.subSet(l.getLeftTime(), l.getRightTime()).isEmpty() == false)
					return true;
			}
			return false;
		}
	}

	public boolean isTimeUnderArtefact(double time) {
		for (AmsLabel l : labels) {
			if (time > l.getLeftTime()) {
				if (time < l.getRightTime())
					return true;
			} else {
				return false;
			}
		}
		return false;
	}

	public void deleteSCLCyclesUnderArtefacts() {
		if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.EVENTBASED) == 1) {
			EventRelatedSCLSet sSet = CurrentOpenData.getInstance().getEventSCLSet();

			for (AmsLabel l : labels) {
				sSet.removeSubSet(l.getLeftTime(), l.getRightTime());
			}
		} else {
			SkinConductanceSet lSet = CurrentOpenData.getInstance().getSCLSet();

			for (AmsLabel l : labels) {
				lSet.removeSubSet(l.getLeftTime(), l.getRightTime());
			}
		}
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void importFromFile(File fl) {

	}

	public void reCalculate() {
		SCLArtefactDetector det = new SCLArtefactDetector();
		labels = new TreeSet<AmsLabel>(det.getArtefacts());
		CurrentOpenData.getInstance().setDirty(true);
	}

	@Override
	public void removeLabel(AmsLabel lab) {
		labels.remove(lab);
		CurrentOpenData.getInstance().setDirty(true);
	}

}
