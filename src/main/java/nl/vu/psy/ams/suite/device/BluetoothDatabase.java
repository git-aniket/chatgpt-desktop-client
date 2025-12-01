package nl.vu.psy.ams.suite.device;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

//import nl.vu.psy.ams.suite.main.AppSettings;
//import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/*
 * Database of AMS device IDs (integer) and Bluetooth MAC addresses (String).
 * Used to quickly connect to known devices.
 */
public class BluetoothDatabase {

	private static Map<Integer, String> dataBase = null;

	public static Map<Integer, String> getDatabase() {
		if (dataBase == null) {
			loadFromDisk();
		}
		return dataBase;
	}

	private static void loadFromDisk() {
		/*
		 * //----------------- Previous code to save the BTDB.json to Temp
		 * Directory-----------------
		 * //File file = new
		 * File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		 * //file = new File(file, "VU-DAMS/BTDB.json");
		 */
		String OSname = System.getProperty("os.name");
		String path = "";
		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application " + "Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		// ----------------Code to save savedBTNumbers.json to App Data
		// directory----------------------------
		File file;
		file = new File(path, "VU-DAMS");

		if (file.exists() == false) {
			file.mkdir();
		}
		file = new File(file, "BTDB.json");
		// -------------------------------------------------------------------------------------------------

		if (file.exists()) {
			Type collectionType = new TypeToken<Map<Integer, String>>() {
			}.getType();
			Gson gson = new Gson();
			String inString;
			inString = Utils.readStringFromFile(file);
			dataBase = gson.fromJson(inString, collectionType);
		} else {
			file = new File(System.getProperty("user.dir"), "BTDB.json");
			if (file.exists()) {
				Type collectionType = new TypeToken<Map<Integer, String>>() {
				}.getType();
				Gson gson = new Gson();
				String inString;
				inString = Utils.readStringFromFile(file);
				dataBase = gson.fromJson(inString, collectionType);
			} else {
				dataBase = new HashMap<Integer, String>();
			}
		}
	}

	/*
	 * public static void saveToDisk() {
	 * 
	 * dataBase = new HashMap<Integer, String>(); dataBase.put(27,
	 * "008098E6881E"); dataBase.put(87, "008098E8D622");
	 * 
	 * File file = new File(System.getProperty("java.io.tmpdir"), "VU-DAMS");
	 * file = new File(file, "BTDB.json"); Gson gson = new
	 * GsonBuilder().setPrettyPrinting
	 * ().serializeSpecialFloatingPointValues().create(); PrintWriter writer =
	 * null; try { writer = new PrintWriter(new BufferedWriter(new
	 * FileWriter(file))); writer.print(gson.toJson(dataBase)); writer.close();
	 * } catch (IOException e) {
	 * e.printStackTrace(); } }
	 */

}
