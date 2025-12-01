package nl.vu.psy.ams.suite.data.structures.sets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.MetaLabel;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/*
 * Set of combined labels. Used for generating bar graphs.
 */
public class MetaLabelSet {
	private ArrayList<MetaLabel> metaLabels = new ArrayList<MetaLabel>();

	public void addMetaLabel(MetaLabel label) {
		metaLabels.add(label);
		CurrentOpenData.getInstance().setDirty(true);
	}

	public void clear() {
		metaLabels.clear();
		CurrentOpenData.getInstance().setDirty(true);
	}

	public ArrayList<MetaLabel> getMetaLabels() {
		return metaLabels;
	}

	public void readFromJSON(File file) {
		Gson gson = new Gson();
		LinkedList<Object> tempList;
		metaLabels.clear();
		String inString;
		inString = Utils.readStringFromFile(file);
		if (inString != null) {
			Type collectionType = new TypeToken<LinkedList<MetaLabel>>() {
			}.getType();
			tempList = gson.fromJson(inString, collectionType);
			for (Object o : tempList) {
				metaLabels.add((MetaLabel) o);
			}
		}
	}

	public void writeToJSON(File file) {
		PrintWriter writer;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(metaLabels));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
