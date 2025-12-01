package nl.vu.psy.ams.suite.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
// import java.io.BufferedReader;
import java.io.File;
// import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.main.BuildInfo;

// import org.apache.logging.log4j.Logger;
// import org.apache.logging.log4j.LogManager;
/*
 * Pack of utility methods, most explain themselves
 */
public class Utils {
	public enum RoundType {
		MILLISECOND,
		CENTISECOND,
		DECISECOND,
		SECOND,
		TWOSECOND,
		FIVESECOND,
		TENSECOND,
		THIRTYSECOND,
		MINUTE,
		TWOMINUTE,
		FIVEMINUTE,
		TENMINUTE,
		THIRTYMINUTE,
		HOUR,
		TWOHOUR,
		THREEHOUR,
		SIXHOUR,
		TWELVEHOUR,
		TWENTYFOURHOUR,
		THIRTYSIXHOUR,
		FOURTYEIGHTHOUR,
		NINETYSIXHOUR
	}

	public static List<String> USERFILES = Collections
			.unmodifiableList(Arrays.asList("labelconfig.json", "lastdir.txt", "outputconfig.json",
					"savedBTNumbers.json", "combinedlabels.json"));

	// private static Logger logger = LogManager.getLogger(Utils.class.getName());

	private static final long baseInt = Calendar.getInstance().getTimeInMillis();

	public static final String VERSIONSTRING = "6.0.5core";
	public static final String APPNAME = "VU-DAMS " + getAppVersion();

	private static final boolean isPrerelease = false;

	private static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	private static Cursor westCursor = new Cursor(Cursor.W_RESIZE_CURSOR);
	private static Cursor eastCursor = new Cursor(Cursor.E_RESIZE_CURSOR);
	private static Cursor northCursor = new Cursor(Cursor.N_RESIZE_CURSOR);
	private static Cursor southCursor = new Cursor(Cursor.S_RESIZE_CURSOR);

	private static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	private static final byte[] passwordHash = new byte[] { -23, 86, -28, -39, -85, -78, 2, 120, -126, -89, 80, -120,
			115, -75, -11, -6, -4, 18, 83, 104,
			64, 14, 9, 72, 28, 67, 89, 110, -44, -94, 73, 98 };

	public static String arrayToString(int[] arr) {
		String str = "";
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == 0)
				break;
			str += (char) arr[i];
		}
		return str;
	}

	public static boolean askForExpertPassword(boolean force) {
		if (force == false & AppSettings.getInstance().getIntProperty(Settings.EXPERTMODE) == 1)
			return true;
		JPasswordField pwd = new JPasswordField(10);
		Object[] options = { "OK", "Cancel" };
		int action = JOptionPane.showOptionDialog(null, pwd, "Enter Expert Password", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]); // default button title
		if (action == JOptionPane.OK_OPTION) {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				digest.reset();
				byte[] input = digest.digest(new String(pwd.getPassword()).getBytes("UTF-8"));
				if (Arrays.equals(input, passwordHash)) {
					AppSettings.getInstance().setIntProperty(Settings.EXPERTMODE, 1);
					return true;
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static double[] getAnimationTable() {
		double[] animationTable = new double[25];
		for (int i = 1; i <= 25; i++) {
			animationTable[i - 1] = (1 - Math.cos(Math.PI * i / 25)) / 2;
		}
		return animationTable;
	}

	public static String getAppVersion() {
		String retString = VERSIONSTRING + " (r" + getRevisionNumber();
		// File usDir = new File(System.getProperty("user.dir"));
		// File dateFile = new File(usDir, "date.txt");
		// if (dateFile.exists()) {
		// BufferedReader read = null;
		// try {
		// read = new BufferedReader(new FileReader(dateFile));
		// retString += " " + read.readLine();
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// if (read != null) {
		// try {
		// read.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }

		// }
		retString += " " + BuildInfo.timeStamp + ")";
		return retString;
	}

	public static String getVersionString() {
		String version = VERSIONSTRING;
		return version;
	}

	public static File getBaseTemporaryDirectory() {
		String tempDir = AppSettings.getInstance().getProperty(Settings.TEMPDIR);
		File retDir = new File(tempDir, "VU-DAMS");
		retDir = new File(retDir, Long.toString(baseInt));
		retDir.mkdirs();
		return retDir;
	}

	public static GregorianCalendar getCalendarFromUS(double time) {
		GregorianCalendar date = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		return date;
	}

	public static Color getColorFromInteger(float rel) {
		return Color.getHSBColor(rel, 0.85f, 1f);
	}

	public static String getCurrentlyExecutingMethodName() {
		Throwable t = new Throwable();
		StackTraceElement[] elements = t.getStackTrace();
		if (elements.length <= 0)
			return "[No Stack Information Available]";
		// elements[0] is this method
		if (elements.length < 2)
			return null;
		return elements[1].getMethodName();
	}

	public static Cursor getCursor(int cursorType) {
		switch (cursorType) {
			case Cursor.HAND_CURSOR:
				return handCursor;
			case Cursor.W_RESIZE_CURSOR:
				return westCursor;
			case Cursor.E_RESIZE_CURSOR:
				return eastCursor;
			case Cursor.N_RESIZE_CURSOR:
				return northCursor;
			case Cursor.S_RESIZE_CURSOR:
				return southCursor;
			default:
				return defaultCursor;
		}
	}

	public static Color getDarkColorFromInteger(float rel) {
		return Color.getHSBColor(rel, 0.85f, 0.75f);
	}

	public static String getDateAndTimeFromCal(GregorianCalendar date) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		String retString;
		retString = nf.format(date.get(Calendar.DAY_OF_MONTH));
		retString += "-" + nf.format(date.get(Calendar.MONTH) + 1);
		retString += "-" + date.get(Calendar.YEAR);
		retString += "/";
		retString += nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE)) + ":"
				+ nf.format(date.get(Calendar.SECOND));
		return retString;
	}

	public static String getDateAndTimeFromUS(double time) {
		return getDateAndTimeFromUS(time, CurrentOpenData.getInstance().getStartDate(),
				CurrentOpenData.getInstance().getStartTimeInUS());
	}

	public static String getDateAndTimeFromUSForActivity(double time) {
		return getDateAndTimeFromUSforLabels(time, CurrentOpenData.getInstance().getStartDate(),
				CurrentOpenData.getInstance().getStartTimeInUS());
	}

	public static String getDateAndTimeFromUS(double time, GregorianCalendar gc, long startTime) {
		String retString;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) gc.clone();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		retString = nf.format(date.get(Calendar.DAY_OF_MONTH));
		retString += "-" + nf.format(date.get(Calendar.MONTH) + 1);
		retString += "-" + date.get(Calendar.YEAR);
		retString += "/";
		retString += nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE)) + ":"
				+ nf.format(date.get(Calendar.SECOND));
		return retString;
	}

	public static String getDateAndTimeFromUSforLabels(double time, GregorianCalendar gc, long startTime) {
		String retString;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) gc.clone();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		retString = nf.format(date.get(Calendar.DAY_OF_MONTH));
		retString += "-" + nf.format(date.get(Calendar.MONTH) + 1);
		retString += "-" + date.get(Calendar.YEAR);
		retString += "/";
		retString += nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE)) + ":"
				+ nf.format(date.get(Calendar.SECOND)) + "." + nf.format(date.get(Calendar.MILLISECOND));
		return retString;
	}

	public static String getDateAndTimeFromUSWithMS(double time) {
		GregorianCalendar gc = CurrentOpenData.getInstance().getStartDate();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		String retString;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) gc.clone();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		retString = nf.format(date.get(Calendar.DAY_OF_MONTH));
		retString += "-" + nf.format(date.get(Calendar.MONTH) + 1);
		retString += "-" + date.get(Calendar.YEAR);
		retString += "/";
		retString += nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE)) + ":"
				+ nf.format(date.get(Calendar.SECOND));
		nf.setMinimumIntegerDigits(3);
		retString += "." + nf.format(date.get(Calendar.MILLISECOND));
		return retString;
	}

	public static int[] getDateAndTimeInt(double time) {
		int[] ret = new int[7];
		GregorianCalendar gc = CurrentOpenData.getInstance().getStartDate();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		GregorianCalendar date = (GregorianCalendar) gc.clone();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		// int year, int month, int day, int hour, int minute, int second, int subsecond
		ret[0] = date.get(Calendar.YEAR);
		ret[1] = date.get(Calendar.MONTH) + 1;
		ret[2] = date.get(Calendar.DAY_OF_MONTH);
		ret[3] = date.get(Calendar.HOUR_OF_DAY);
		ret[4] = date.get(Calendar.MINUTE);
		ret[5] = date.get(Calendar.SECOND);
		ret[6] = date.get(Calendar.MILLISECOND);
		return ret;
	}

	public static String getDateFromUS(double time) {
		String retString;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		retString = nf.format(date.get(Calendar.DAY_OF_MONTH));
		retString += "-" + nf.format(date.get(Calendar.MONTH) + 1);
		retString += "-" + date.get(Calendar.YEAR);
		return retString;
	}

	public static GregorianCalendar getDateObjectFromUS(double time) {
		GregorianCalendar date = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		return date;
	}

	public static double getDrawCoordinate(double value, double lowerBound, double upperBound) {
		return 2 * ((value - lowerBound) / (upperBound - lowerBound)) - 1;
	}

	public static String getExtension(File f) {
		String ext = "";
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i >= 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

	public static double getFloorRoundTimeFromUS(double time, RoundType roundType) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		int nMillis = date.get(Calendar.MILLISECOND);
		date.add(Calendar.MILLISECOND, -nMillis);
		final int nSec = date.get(Calendar.SECOND);
		final int nMit = date.get(Calendar.MINUTE);
		final int nHour = date.get(Calendar.HOUR_OF_DAY);
		if (roundType == RoundType.MILLISECOND) {
			int nSecOver1000 = nMillis;
			date.add(Calendar.MILLISECOND, nSecOver1000 - nMillis);
		} else if (roundType == RoundType.CENTISECOND) {
			int nSecOver100 = nMillis / 10;
			date.add(Calendar.MILLISECOND, nSecOver100 - nMillis);
		} else if (roundType == RoundType.DECISECOND) {
			int nSecOver10 = nMillis / 100;
			date.add(Calendar.MILLISECOND, nSecOver10 - nMillis);
		} else if (roundType == RoundType.SECOND) {
			int nSecOver1 = nSec / 1;
			date.add(Calendar.SECOND, nSecOver1 - nSec);
		} else if (roundType == RoundType.TWOSECOND) {
			int nSec2 = nSec / 2;
			nSec2 *= 2;
			date.add(Calendar.SECOND, nSec2 - nSec);
		} else if (roundType == RoundType.FIVESECOND) {
			int nSec5 = nSec / 5;
			nSec5 *= 5;
			date.add(Calendar.SECOND, nSec5 - nSec);
		} else if (roundType == RoundType.TENSECOND) {
			int nSec10 = nSec / 10;
			nSec10 *= 10;
			date.add(Calendar.SECOND, nSec10 - nSec);
		} else if (roundType == RoundType.THIRTYSECOND) {
			int nSec30 = nSec / 30;
			nSec30 *= 30;
			date.add(Calendar.SECOND, nSec30 - nSec);
		} else if (roundType == RoundType.MINUTE) {
			date.add(Calendar.SECOND, -nSec);
		} else if (roundType == RoundType.TWOMINUTE) {
			date.add(Calendar.SECOND, -nSec);
			int nMit2 = nMit / 2;
			nMit2 *= 2;
			date.add(Calendar.MINUTE, nMit2 - nMit);
		} else if (roundType == RoundType.FIVEMINUTE) {
			date.add(Calendar.SECOND, -nSec);
			int nMit5 = nMit / 5;
			nMit5 *= 5;
			date.add(Calendar.MINUTE, nMit5 - nMit);
		} else if (roundType == RoundType.TENMINUTE) {
			date.add(Calendar.SECOND, -nSec);
			int nMit10 = nMit / 10;
			nMit10 *= 10;
			date.add(Calendar.MINUTE, nMit10 - nMit);
		} else if (roundType == RoundType.THIRTYMINUTE) {
			date.add(Calendar.SECOND, -nSec);
			int nMit30 = nMit / 30;
			nMit30 *= 30;
			date.add(Calendar.MINUTE, nMit30 - nMit);
		} else if (roundType == RoundType.HOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
		} else if (roundType == RoundType.HOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
		} else if (roundType == RoundType.TWOHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour2 = nHour / 2;
			nHour2 *= 2;
			date.add(Calendar.HOUR_OF_DAY, nHour2 - nHour);
		} else if (roundType == RoundType.THREEHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour3 = nHour / 3;
			nHour3 *= 3;
			date.add(Calendar.HOUR_OF_DAY, nHour3 - nHour);
		} else if (roundType == RoundType.SIXHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour6 = nHour / 6;
			nHour6 *= 6;
			date.add(Calendar.HOUR_OF_DAY, nHour6 - nHour);
		} else if (roundType == RoundType.TWELVEHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour12 = nHour / 12;
			nHour12 *= 12;
			date.add(Calendar.HOUR_OF_DAY, nHour12 - nHour);
		} else if (roundType == RoundType.TWENTYFOURHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour24 = nHour / 24;
			nHour24 *= 24;
			date.add(Calendar.HOUR_OF_DAY, nHour24 - nHour);
		} else if (roundType == RoundType.THIRTYSIXHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour36 = nHour / 36;
			nHour36 *= 36;
			date.add(Calendar.HOUR_OF_DAY, nHour36 - nHour);
		} else if (roundType == RoundType.FOURTYEIGHTHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour48 = nHour / 48;
			nHour48 *= 48;
			date.add(Calendar.HOUR_OF_DAY, nHour48 - nHour);
		} else if (roundType == RoundType.NINETYSIXHOUR) {
			date.add(Calendar.SECOND, -nSec);
			date.add(Calendar.MINUTE, -nMit);
			int nHour96 = nHour / 96;
			nHour96 *= 96;
			date.add(Calendar.HOUR_OF_DAY, nHour96 - nHour);
		}
		return getTimeFromDate(date.getTime());
	}

	public static long getFreeBytesInTemporaryDirectory() {
		return getBaseTemporaryDirectory().getFreeSpace();
	}

	public static int GetNextPowerOfTwo(int number) {
		number--;
		number = number | (number >> 1);
		number = number | (number >> 2);
		number = number | (number >> 4);
		number = number | (number >> 8);
		number = number | (number >> 16);
		number++;
		return number;
	}

	public static int getPixelCoordinate(double value, double lowerBound, double upperBound, int width) {
		return (int) Math.round(width * (value - lowerBound) / (upperBound - lowerBound));
	}

	public static double getRelDrawCoordinate(double value, double lowerBound, double upperBound) {
		return ((value - lowerBound) / (upperBound - lowerBound));
	}

	public static int getRevisionNumber() {
		return Integer.valueOf(BuildInfo.revisionNumber);
	}

	public static double getTimeFromDate(Date dt) {
		GregorianCalendar startDate = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		GregorianCalendar curDate = (GregorianCalendar) Calendar.getInstance();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		curDate.setTime(dt);
		long startMS = startDate.getTimeInMillis();
		long curMS = curDate.getTimeInMillis();
		long diffMS = curMS - startMS;
		return startTime + diffMS * 1000;
	}

	public static double getTimeFromDate(Date dt, GregorianCalendar startDate, long startTimeInUS) {
		GregorianCalendar curDate = (GregorianCalendar) Calendar.getInstance();
		curDate.setTime(dt);
		long startMS = startDate.getTimeInMillis();
		long curMS = curDate.getTimeInMillis();
		long diffMS = curMS - startMS;
		return startTimeInUS + diffMS * 1000;
	}

	public static String getTimeFromUS(double time) {
		String retString;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		GregorianCalendar date = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		long startTime = CurrentOpenData.getInstance().getStartTimeInUS();
		date.add(Calendar.MILLISECOND, (int) ((time - startTime) / 1000));
		retString = nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE)) + ":"
				+ nf.format(date.get(Calendar.SECOND)) + "." + nf.format(date.get(Calendar.MILLISECOND));

		return retString;
	}

	public static long getTotalFileSize(Collection<File> outFiles) {
		long totalFileSize = 0;
		for (File f : outFiles) {
			totalFileSize += f.length();
		}
		return totalFileSize;
	}

	public static File getUniqueTemporaryDirectory() {
		String tempDir = AppSettings.getInstance().getProperty(Settings.TEMPDIR);
		File retDir = new File(tempDir, "VU-DAMS");
		retDir = new File(retDir, Long.toString(baseInt));
		retDir = new File(retDir, Long.toString(Calendar.getInstance().getTimeInMillis()));
		retDir.mkdirs();
		System.out.println("Temporary directory created: " + retDir);
		return retDir;
	}

	public static String guaranteeExtension(String fileName, String extension) {
		String retString = fileName;
		String ext = getExtension(new File(retString));
		if (extension.equals(ext) == false) {
			retString += "." + extension;
		}
		return retString;
	}

	public static String guaranteeExtensionAndAddition(String fileName, String extension, String addition) {
		String retString = removeExtension(fileName);
		retString += addition;
		return guaranteeExtension(retString, extension);
	}

	public static String guaranteeExtensionAndPrepend(String fileName, String extension, String addition) {
		String retString = removeExtension(fileName);
		String base = new File(retString).getParent();
		String name = new File(retString).getName();
		retString = new File(base, addition + name).getAbsolutePath();
		return guaranteeExtension(retString, extension);
	}

	public static boolean isCorrectJavaVersion() {
		String version = System.getProperty("java.version");
		Integer major = -1;
		Integer minor = -1;
		Integer point = -1;
		Integer vers = -1;
		if (version.startsWith("1.")) {
			if (version.length() >= 1)
				major = parseInt(version.substring(0, 1));
			if (version.length() >= 3)
				minor = parseInt(version.substring(2, 3));
			if (version.length() >= 5)
				point = parseInt(version.substring(4, 5));
			if (version.length() >= 7)
				vers = parseInt(version.substring(6, 7));
			if (version.length() >= 8)
				vers = parseInt(version.substring(6, 8));
			if (major == null || minor == null || point == null | vers == null)
				return false;
			if (major < 1)
				return false;
			if (minor < 6 & major <= 1)
				return false;
			if (point < 0)
				return false;
			if (vers < 22 & minor <= 6 & major <= 1)
				return false;
		}
		return true;
	}

	public static boolean isIsprerelease() {
		return isPrerelease;
	}

	public static String readStringFromFile(File inFile) {
		String ret = null;
		try {
			ret = new String(Files.readAllBytes(Paths.get(inFile.getAbsolutePath())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String[] readLinesFromFile(File inFile) {
		List<String> ret = null;
		try {
			ret = Files.readAllLines(Paths.get(inFile.getAbsolutePath()), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] retStrings = new String[ret.size()];
		for (int i = 0; i < ret.size(); i++)
			retStrings[i] = ret.get(i);
		return retStrings;
	}

	public static String removeExtension(String fileName) {
		String retString = fileName;
		int i = retString.lastIndexOf('.');
		if (i == -1)
			return retString;
		return retString.substring(0, i);
	}

	public static int setInsideBounds(int value, int lBound, int uBound) {
		int val = value;
		if (value < lBound)
			val = lBound;
		if (value > uBound)
			val = uBound;
		return val;
	}

	public static void submitBug() {
		String version = getVersionString();
		// int rev = getRevisionNumber();
		try {
			Desktop.getDesktop()
					.browse(new URI("mailto:vuams.fgb@vu.nl?subject=bug%2Ffeature%20report%20version%20" + version));
			// Desktop.getDesktop().browse(new
			// URI("http://vu-amsproblemlog.psy.vu.nl/suite/log.php?rev=" + rev));
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// 0
			e1.printStackTrace();
		}
	}

	public static void OpenWebsite(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// 0
			e1.printStackTrace();
		}
	}

	public static void submitTechnicalBug(int type) throws MalformedURLException {

		// Type = 1 - Watch dog reset
		// Type = 2 - Buffer Overflow
		final CurrentOpenData cod = CurrentOpenData.getInstance();
		int[] tt = cod.getFileHeader().gettStamp().toDeviceArray();

		String errorURL = String.format(
				"http://vu-amsproblemlog.psy.vu.nl/log.php?a=%04d%02d%02d%02d%02d&b=%03d&c=%s&d=%02d%n", (tt[0] + 1900),
				(tt[1] + 1), tt[2], tt[3], tt[4], cod.getFileHeader().getDwSerialNumber(),
				cod.getFileHeader().getFirmwareVersion(), type);
		try {
			URI uri = new URI(errorURL);
			Desktop.getDesktop().browse(uri);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {

		}
	}

	public static int compareVersions(String current, String newVersion) {
		// taken from best answer of
		// https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
		String[] thisParts = newVersion.split("\\.");
		String[] thatParts = current.split("\\.");
		int length = Math.max(thisParts.length, thatParts.length);
		for (int i = 0; i < length; i++) {
			try {
				int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
				int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
				if (thisPart < thatPart)
					return -1;
				if (thisPart > thatPart)
					return 1;
			} catch (java.lang.NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	public static double convertDouble(Object longValue) {

		if (longValue instanceof Long)
			return ((Long) longValue).doubleValue();
		else
			return (Double) longValue;
	}

	public static Date parseDate(DateFormat df, String input) {
		Date ret;
		try {
			ret = df.parse(input);
		} catch (ParseException e) {
			return null;
		}
		return ret;
	}

	public static Integer parseInt(String input) {
		Integer ret;
		try {
			ret = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			return null;
		}
		return ret;
	}
}
