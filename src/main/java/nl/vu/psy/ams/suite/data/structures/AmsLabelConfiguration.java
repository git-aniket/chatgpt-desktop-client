package nl.vu.psy.ams.suite.data.structures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Configuration used when labeling. The configuration is
 * implemented as a Map from String -> list of labelvalues.
 * The String keys are the label catagories, the labelvalues are
 * pairs of codes (integers) and descriptions (strings).
 */

public class AmsLabelConfiguration {

	public static AmsLabelConfiguration getArtefactConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();
		// ----------------------- Include type of artefact while creating ECG/SCL
		// Artefact-----------------------------
		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(-2, "Clipping ECG values"));
		artefactValues.add(new LabelValue(-3, "Flat ECG signal"));
		artefactValues.add(new LabelValue(-4, "Clipping ECG values and Flat ECG signal"));
		artefactValues.add(new LabelValue(-5, "Premature Ventricular Contraction"));
		artefactValues.add(new LabelValue(-6, "Premature Atrial Contraction"));
		artefactValues.add(new LabelValue(-7, "Other"));
		artefactValues.add(new LabelValue(-8, "Manually added"));

		cfg.config.put("ECG Artefact", artefactValues);
		// ---------------------------------------------------------------------------------------------------------
		return cfg;
	}

	// ----------------------- Include type of artefact while creating ECG/SCL
	// Artefact-----------------------------
	public static AmsLabelConfiguration getSCLArtefactConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();

		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(-6, "SCLArtefact"));
		cfg.config.put("SCL Artefact", artefactValues);

		return cfg;
	}
	// ---------------------------------------------------------------------------------------------------------

	// ----------------------- Include type while creating Posture
	// labels-----------------------------
	public static AmsLabelConfiguration getPostureLabelConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();

		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(-6, "Moving"));
		artefactValues.add(new LabelValue(-5, "LYING"));
		artefactValues.add(new LabelValue(-4, "SITTING"));
		artefactValues.add(new LabelValue(-3, "STANDING"));
		artefactValues.add(new LabelValue(-2, "UNKNOWN"));
		artefactValues.add(new LabelValue(-8, "DYNAMIC"));
		artefactValues.add(new LabelValue(-7, "Stationary"));
		artefactValues.add(new LabelValue(-10, "Dynamic LPA"));
		artefactValues.add(new LabelValue(-11, "Dynamic MPA"));
		artefactValues.add(new LabelValue(-12, "Dynamic VPA"));
		artefactValues.add(new LabelValue(-13, "Static Upright"));
		artefactValues.add(new LabelValue(-14, "Static Lying"));
		artefactValues.add(new LabelValue(-15, "Dynamic MPA StairsUp"));
		artefactValues.add(new LabelValue(-16, "Dynamic LPA StairsDown"));
		// artefactValues.add(new LabelValue(-6, "Sitting"));
		// artefactValues.add(new LabelValue(-5, "Standing"));
		// artefactValues.add(new LabelValue(-4, "Stairs up"));
		// artefactValues.add(new LabelValue(-3, "Stairs down"));
		// artefactValues.add(new LabelValue(-2, "Vertical stationary"));
		// artefactValues.add(new LabelValue(-7, "Lying"));
		// artefactValues.add(new LabelValue(-8, "Walking"));
		// artefactValues.add(new LabelValue(-9, "Running"));
		// artefactValues.add(new LabelValue(-10, "Transition sit to stand"));
		// artefactValues.add(new LabelValue(-11, "Transition stand to sit"));
		// artefactValues.add(new LabelValue(-12, "Stairs"));
		cfg.config.put("Posture", artefactValues);

		return cfg;
	}

	// ---------------------------------------------------------------------------------------------------------
	public static AmsLabelConfiguration getSpeechLabelConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();

		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(-6, "Speaking"));
		artefactValues.add(new LabelValue(-5, "Quiet"));
		artefactValues.add(new LabelValue(-2, "Unknown"));
		cfg.config.put("Speech", artefactValues);

		return cfg;
	}

	public static AmsLabelConfiguration getStairsLabelConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();

		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(3, "Stairs up"));
		artefactValues.add(new LabelValue(-3, "Stairs down"));
		// artefactValues.add(new LabelValue(2, "Incline up"));
		// artefactValues.add(new LabelValue(-2, "Incline down"));
		artefactValues.add(new LabelValue(1, "Level ground"));

		cfg.config.put("Stairs", artefactValues);

		return cfg;
	}

	// ----------------------- Include type while creating Activity
	// labels-----------------------------
	public static AmsLabelConfiguration getActivityLabelConfiguration() {
		AmsLabelConfiguration cfg = new AmsLabelConfiguration();

		ArrayList<LabelValue> artefactValues = new ArrayList<LabelValue>();
		artefactValues.add(new LabelValue(-6, "Medium"));
		artefactValues.add(new LabelValue(-5, "High"));
		cfg.config.put("ActivityIntensity", artefactValues);

		return cfg;
	}
	// ---------------------------------------------------------------------------------------------------------

	private LinkedHashMap<String, ArrayList<LabelValue>> config = new LinkedHashMap<String, ArrayList<LabelValue>>();

	public ArrayList<String> getCategories() {
		ArrayList<String> cats = new ArrayList<String>(config.keySet());
		return cats;
	}

	public LinkedHashMap<String, ArrayList<LabelValue>> getConfig() {
		return config;
	}

	public void getConfigFromFile(File fl) {
		try {
			BufferedReader read = new BufferedReader(new FileReader(fl));
			String line = read.readLine();
			ArrayList<LabelValue> toBeAdded = new ArrayList<LabelValue>();
			Set<Integer> foundNumbers = new HashSet<Integer>();
			String curCat = null;
			config.clear();
			while (line != null) {
				if (line.startsWith("#")) {
					line = line.substring(1);
					while (line.startsWith(" "))
						line = line.substring(1);

					if (curCat != null) {
						config.put(curCat, toBeAdded);
					}
					toBeAdded = new ArrayList<LabelValue>();
					curCat = line;

				} else {
					if (line.length() > 3) {
						String[] parts = line.split("\\s+");
						Integer code = Utils.parseInt(parts[0]);
						if (code == null) {
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"Label name configuration aborted: non-integer code " + parts[0]
											+ ". Please fix and reload input file.");
							read.close();
							return;
						}
						if (foundNumbers.contains(code)) {
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"Label name configuration aborted: duplicate category code " + code
											+ ". Please fix and reload input file.");
							read.close();
							return;
						}
						line = line.substring(parts[0].length() + 1);
						toBeAdded.add(new LabelValue(code, line));
						foundNumbers.add(code);
					}
				}
				line = read.readLine();
			}
			read.close();
			if (curCat != null && toBeAdded.isEmpty() == false) {
				config.put(curCat, toBeAdded);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public TreeMap<String, ArrayList<String>> getNumberMapFromConfigFile(File fl) {
		TreeMap<String, ArrayList<String>> retMap = new TreeMap<String, ArrayList<String>>();
		try {
			BufferedReader read = new BufferedReader(new FileReader(fl));
			String line = read.readLine();
			String curCat = null;
			while (line != null) {
				if (line.startsWith("#")) {
					line = line.substring(1);
					while (line.startsWith(" "))
						line = line.substring(1);

					curCat = line;
				} else {
					if (line.length() > 3) {
						String vals[] = line.split(" ");
						String num = vals[0];
						String val = line.substring(vals[0].length() + 1);
						ArrayList<String> addList = new ArrayList<String>();
						addList.add(curCat);
						addList.add(val);
						retMap.put(num, addList);
					}
				}
				line = read.readLine();
			}
			read.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retMap;
	}

	public ArrayList<LabelValue> getValuesForCategory(String cat) {
		return config.get(cat);
	}

	public void saveConfigToFile(File outFile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outFile);
			for (String cat : getCategories()) {
				pw.println("#" + cat);
				ArrayList<LabelValue> vals = config.get(cat);
				for (LabelValue lv : vals) {
					pw.println(lv.getCode() + " " + lv.getName());
				}
				pw.println();
			}
		} catch (IOException e) {

		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	public void saveToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(getConfig()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
