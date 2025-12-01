package nl.vu.psy.ams.suite.main;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

/*
 * Class that is used to get and save user settings.
 * The enum defines all available settings, and its default
 * values. The class is a singleton, so settings can be loaded
 * from all points in the DAMS code. To add a new setting, simply add
 * a new value to the Settings enum. Add 'true' (such as in tempdir) to indicate
 * that a setting can not be hotswapped (it will only change after a restart of the DAMS software).
 * Use getIntProperty() to load an integer property from the settings (booleans are coded as 0=false,1=true).
 * Use getProperty() to load a string property from the settings.
 */
//JdH: in Windows XP the settings below are saved in registry in HKEY_CURRENT_USER\Software\JavaSoft\Prefs\/V/U-/D/A/M/S
public class AppSettings {
	public enum Settings {
		TEMPDIR("tempdir", "-1", true), BINARYBUFFERSIZE("binarybuffersize", "20000", true),
		MAXDISPLAYPOINTS("maximumdisplayedpoints", "10000", true), GRIDSIZE("gridsize", "25"),
		SNAPSIZE("snapsize", "5"), HIGHTHRESHOLD("highthreshold", "80"), LOWTHRESHOLD("lowthreshold", "60"),
		PTPW("peaktopeakweight", "100"), UPW("upwardslopeweight", "33"), DOWNW("downwardslopeweight", "33"),
		AUTOSCALEUPONTABCHANGE("AUTOSCALEUPONTABCHANGE", "1"),
		BARGRAPHTITLE("BARGRAPHTITLE", "Average heartrate during different activities"),
		BARGRAPHXTITLE("BARGRAPHXTITLE", "Heartrate (beats per minute)"), BARGRAPHYTITLE("BARGRAPHYTITLE", "Activity"),
		DRAWRAWHRLABELTAB("DRAWRAWHRLABELTAB", "0"), DRAWAVHRLABELTAB("DRAWAVHRLABELTAB", "1"),
		DRAWRAWRRLABELTAB("DRAWRAWRRLABELTAB", "0"), DRAWAVRRLABELTAB("DRAWAVRRLABELTAB", "1"),
		DRAWRAWMOTLABELTAB("DRAWRAWMOTLABELTAB", "1"), DRAWAVMOTLABELTAB("DRAWAVMOTLABELTAB", "1"),
		FREQLAMBDA("FREQLAMBDA", "50000"), FREQSIGMA("FREQSIGMA", "3500"), SHOWENTIREDATA("SHOWENTIREDATA", "1"),
		UPDATEREVNUMBER("UPDATEREVNUMBER", "-1"), UPDATEREVDATE("UPDATEREVDATE", "-1"),
		MISSINGVALUE("MISSINGVALUE", "-9999"), DRAWLFSIGNAL("DRAWLFSIGNAL", "0"), DRAWHFSIGNAL("DRAWHFSIGNAL", "1"),
		GENGRAPHHRYAXIS("GENGRAPHHRYAXIS", "Heart Rate (beats per minute)"),
		GENGRAPHMOTYAXIS("GENGRAPHMOTYAXIS", "Motility (g)"), GENGRAPHXAXIS("GENGRAPHXAXIS", ""),
		GENGRAPHTITLE("GENGRAPHTITLE", ""), LFLB("LFLB", "40"), LFUB("LFUB", "150"), HFLB("HFLB", "150"),
		HFUB("HFUB", "400"), DRAWRSAHF("DRAWRSAHF", "0"), GRIDENABLED("GRIDENABLED", "1"),
		LABELAVHRACOLOR("LABELAVHRACOLOR", "-16744448"), LABELAVMOTCOLOR("LABELAVMOTCOLOR", "-16744448"),
		LABELAVRRACOLOR("LABELAVRRACOLOR", "-16744448"), SHOWBATSIGNAL("SHOWBATSIGNAL", "0"),
		RSARELTHRESH("RSARELTHRESH", "33"), RSAAFTERSHORTEST("RSAAFTERSHORTEST", "1000"),
		RSAAFTERLONGEST("RSAAFTERLONGEST", "1000"), RSADZRANGECHECK("RSADZRANGECHECK", "1"),
		RSAMINDZ("RSAMINDZ", "-950"), RSAMAXDZ("RSAMAXDZ", "950"), RSARRATECHECK("RSARRATECHECK", "1"),
		RSARRATEMAX("RSARRATEMAX", "65"), RSAIBICHECK("RSAIBICHECK", "1"), RSAIBIMAX("RSAIBIMAX", "50"),
		SHOWCLINICALWARNING("SHOWCLINICALWARNING", "1"), CFWARNHOURS("CFWARNHOURS", "24"),
		CFWARNENABLED("CFWARNENABLED", "1"), TIMEDIFFWARNINGENABLED("TIMEDIFFWARNINGENABLED", "1"),
		TIMEDIFFWARNING("TIMEDIFFWARNING", "10"), BTDBREVNUMBER("BTDBREVNUMBER", "10"),
		SAVESIDINFILENAME("SAVESIDINFILENAME", "1"), EXPERTMODE("EXPERTMODE", "0"),
		SHOWUPDATEAVAILABLE("SHOWUPDATEAVAILABLE", "1"), SHOWRSATOOLTIP("SHOWRSATOOLTIP", "1"),
		SHOWMARKERINECGARTEFACTSBAR("SHOWMARKERINECGARTEFACTSBAR", "1"),
		LABELBETWEENMARKERS("LABELBETWEENMARKERS", "30"), EVENTBASED("EVENTBASED", "0"), LABELBASED("LABELBASED", "1"),
		UPDATEVERSTRING("UPDATEVERSTRING", "1.0"), FILTERDZDTSignal("FILTERDZDTSignal", "1"),
		IMPORTACTIGRAPHDATA("IMPORTACTIGRAPHDATA", "0"), ANALYZEACTIVITY("ANALYZEACTIVITY", "0"),
		TRAINNNMODEL("TRAINNNMODEL", "0"), APPENDSUBIDTOTITLEBAR("APPENDSUBIDTOTITLEBAR", "1"), DEBUG("DEBUG", "0"),
		REMOVEICGBEATS("REMOVEICGBEATS", "1"), FILTERECGSignal("FILTERECGSignal", "1"),
		OUTLIERREMOVAL("OUTLIERREMOVAL", "2"), LYINGTHRESHOLD("LYINGTHRESHOLD", "200"),
		MOTTHRESHOLD("MOTTHRESHOLD", "20"), MINHR("MINHR", "30"), MAXHR("MAXHR", "240"),
		MOTARTEFACTS("MOTARTEFACTS", "0"), FILTERDZDTNew("FILTERDZDTSignal", "1"), FILTERECGNew("FILTERECGSignal", "1"),
		SHOWEOFEVENTS("SHOWEOFEVENTS", "0");

		private final String key;
		private final String defaultValue;
		private final boolean toBeSavedLater;

		private Settings(String key, String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
			toBeSavedLater = false;
		}

		private Settings(String key, String defaultValue, boolean toBeSavedLater) {
			this.key = key;
			this.defaultValue = defaultValue;
			this.toBeSavedLater = toBeSavedLater;
		}

		public String getDefault() {
			return defaultValue;
		}

		public String getKey() {
			return key;
		}

		public boolean toBeSavedLater() {
			return toBeSavedLater;
		}
	};

	private static AppSettings instance;

	public static AppSettings getInstance() {
		if (instance == null) {
			instance = new AppSettings();
		}
		return instance;
	}

	private final HashMap<Settings, String> toBeSaved = new HashMap<Settings, String>();

	private Preferences appProperties;

	private AppSettings() {
		appProperties = Preferences.userRoot().node("VU-DAMS");
	}

	private void forceSetProperty(Settings set, String val) {
		appProperties.put(set.getKey(), val);
	}

	public Integer getIntProperty(Settings set) {
		return Integer.parseInt(getProperty(set));
	}

	public Integer getIntPropertyOrToBeSaved(Settings set) {
		return Integer.parseInt(getPropertyOrToBeSaved(set));
	}

	public String getProperty(Settings set) {
		String value = appProperties.get(set.getKey(), set.getDefault());
		if (set.equals(Settings.TEMPDIR)) {
			if (value.equals("-1")) {
				return System.getProperty("java.io.tmpdir");
			}
		}
		return value;
	}

	public String getPropertyOrToBeSaved(Settings set) {
		String value = toBeSaved.get(set);
		if (value == null)
			value = appProperties.get(set.getKey(), set.getDefault());
		if (set.equals(Settings.TEMPDIR)) {
			if (value.equals("-1")) {
				return System.getProperty("java.io.tmpdir");
			}
		}
		return value;
	}

	public void restoreDefaults() {
		for (Settings s : Settings.values()) {
			setProperty(s, s.getDefault());
		}
	}

	public void saveSettingsToBeSaved() {
		for (Entry<Settings, String> entry : toBeSaved.entrySet()) {
			forceSetProperty(entry.getKey(), entry.getValue());
		}
	}

	public void setIntProperty(Settings set, int val) {
		setProperty(set, Integer.toString(val));
	}

	public void setProperty(Settings set, String val) {
		if (set.toBeSavedLater() == true) {
			toBeSaved.put(set, val);
		} else {
			appProperties.put(set.getKey(), val);
		}
	}

}
