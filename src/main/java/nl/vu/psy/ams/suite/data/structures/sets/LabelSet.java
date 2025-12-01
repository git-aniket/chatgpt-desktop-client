package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 * Class that handles all labels of a single dataset.
 * Includes methods to export / import them from / to files.
 */
public class LabelSet {
	protected TreeSet<AmsLabel> labels = new TreeSet<AmsLabel>();
	protected TreeSet<AmsLabel> lList = new TreeSet<AmsLabel>();
	public int lcode;
	public String lvname;
	public double timewidth = 0.0;
	protected int index = 0;

	public void clear() {
		labels.clear();
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void add(AmsLabel lab) {
		add(lab, true);
	}

	public void add(AmsLabel lab, boolean setDirty) {
		labels.add(lab);
		if (setDirty)
			CurrentOpenData.getInstance().setDirty(true);
	}

	public AmsLabel getLabelAfterTime(double time) {
		ArrayList<Double> timeDiffs = new ArrayList<Double>();
		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<AmsLabel> lbls = new ArrayList<AmsLabel>(getLabels());
		for (AmsLabel l : getLabels()) {
			if (l.getLeftTime() - time >= 0) {
				timeDiffs.add(l.getLeftTime() - time);
				indices.add(lbls.indexOf(l));
			}
		}
		if (timeDiffs.isEmpty())
			return null;
		double minDiff = Double.MAX_VALUE;
		int maxIndex = -1;
		for (Double d : timeDiffs) {
			if (d < minDiff) {
				minDiff = d;
				maxIndex = indices.get(timeDiffs.indexOf(d));
			}
		}
		return lbls.get(maxIndex);
	}

	public AmsLabel getLabelBeforeTime(double time) {
		ArrayList<Double> timeDiffs = new ArrayList<Double>();
		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<AmsLabel> lbls = new ArrayList<AmsLabel>(getLabels());
		for (AmsLabel l : getLabels()) {
			if (time - l.getRightTime() >= 0) {
				timeDiffs.add(time - l.getRightTime());
				indices.add(lbls.indexOf(l));
			}
		}
		if (timeDiffs.isEmpty())
			return null;
		double minDiff = Double.MAX_VALUE;
		int maxIndex = -1;
		for (Double d : timeDiffs) {
			if (d < minDiff) {
				minDiff = d;
				maxIndex = indices.get(timeDiffs.indexOf(d));
			}
		}
		return lbls.get(maxIndex);
	}

	public AmsLabel getLabelUnderTime(double time) {
		for (AmsLabel l : getLabels()) {
			if (time > l.getLeftTime() && time < l.getRightTime())
				return l;
		}
		return null;
	}

	public boolean isTimeUnderLabel(double time) {
		for (AmsLabel l : getLabels()) {
			if (time > l.getLeftTime()) {
				if (time < l.getRightTime())
					return true;
			} else {
				return false;
			}
		}
		return false;
	}

	public void removeLabel(AmsLabel lab) {
		labels.remove(lab);
		ImpTab.getInstance().getImpDrawer().getSignalPartSet().removePart(lab);
		ImpTab.getInstance().getECGDrawer().getSignalPartSet().removePart(lab);
		if (ImpTab.getInstance().getDrawers() != null)
			for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
				d.getSignalPartSet().removePart(lab);
		ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().removePart(lab);
		ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().removePart(lab);
		if (ImpTab.getInstance().getFiltDrawers() != null)
			for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
				prt.getSignalPartSet().removePart(lab);
		CurrentOpenData.getInstance().setDirty(true);
	}

	public TreeSet<AmsLabel> getLabels() {
		return labels;
	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(getLabels()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setlabelindex(int labelindex) {
		index = labelindex;
	}

	public int getlabelindex() {
		return index;
	}
}
