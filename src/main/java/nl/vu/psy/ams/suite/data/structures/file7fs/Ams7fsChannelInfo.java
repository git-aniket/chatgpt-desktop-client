package nl.vu.psy.ams.suite.data.structures.file7fs;

import java.util.Map;

import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;

/*
 * Channel info from 5fs file (copied from 5fs code)
 */
public class Ams7fsChannelInfo {

	public static final int SIZE = 32;

	int wTag; // 00 - type tag to identify
				// ChannelInfo structure
	int wSize; // 02 - total nr of bytes of this
				// structure
	String szID;
	// 04 - unique channel ID (i.e. 'ECG')
	long dwDivider = 1; // 0C - sampling rate divider
	// (master clock decimation)

	long nBits; // 10 - number of ADC bits (i.e. 12
				// or 16)

	int lMinValue; // 14 - minimum physical value *
					// lDivider
	int lMaxValue; // 18 - maximum physical value *
					// lDivider

	int lMinMaxDivider = 1; // 1C - divider value, which is used
	// to scale min

	// and
	// max
	String szUnit; // 24 - 'mV', 'uS', 'ohm/sec'
	int dwChannelTag; // 26 - channel tag ==
						// AMSII_TAG_CHANNEL_BIT|CH_xxx
	int wBufSize; // 28 - size of internal signal
					// buffer for
	// collecting (not the
	// ringbuffer!)
	int bStore; // 2C - true if channel must be
				// stored in AMS file

	// (but only if
	// dwDivider is valid)
	String szReserved; // 30 - fill up to 64 bytes
	double realSlope; // added for 7fs
	double realConstant = 0; // added for 7fs
	String formula = "";
	Map<String, Double> constants = null;
	String tickFile;

	public Ams7fsChannelInfo() {

	}

	public Ams7fsChannelInfo(UnsignedByteBuffer buff) {
		szID = buff.getString(8).trim();
		szUnit = buff.getString(8).trim();
		szUnit = szUnit.replace("Ohm", "\u2126");
		szUnit = szUnit.replace("ohm", "\u2126");
		szUnit = szUnit.replace("deg", "\u00B0");
		szUnit = szUnit.replace("uS", "\u00B5S");
		wSize = (int) buff.getUInt();
		buff.getUInt(); // type of data
		nBits = buff.getUInt();
		buff.getUInt(); // calibration function
		// wTag = buff.getUShort();
		dwDivider = 1;
		lMinValue = -Integer.MIN_VALUE / 131072;
		lMaxValue = Integer.MAX_VALUE / 131072;
		lMinMaxDivider = 1;
		// dwChannelTag = buff.getUShort();
		// wBufSize = buff.getUShort();
		// bStore = buff.getInt();
		// szReserved = buff.getString(16);

	}

	public int getbStore() {
		return bStore;
	}

	public int getDwChannelTag() {
		return dwChannelTag;
	}

	public long getDwDivider() {
		return dwDivider;
	}

	public int getlMaxValue() {
		return lMaxValue;
	}

	public int getlMinMaxDivider() {
		return lMinMaxDivider;
	}

	public int getlMinValue() {
		return lMinValue;
	}

	public long getnBits() {
		return nBits;
	}

	public String getSzID() {
		return szID;
	}

	public String getSzReserved() {
		return szReserved;
	}

	public String getSzUnit() {
		return szUnit;
	}

	public String getTickFile() {
		return tickFile;
	}

	public int getwBufSize() {
		return wBufSize;
	}

	public int getwSize() {
		return wSize;
	}

	public int getwTag() {
		return wTag;
	}

	public double getRealSlope() {
		return realSlope;
	}

	public double getRealConstant() {
		return realConstant;
	}

	public String getFormula() {
		return formula;
	}

	public Map<String, Double> getConstants() {
		return constants;
	}

	public void setDwDivider(long dwDivider) {
		this.dwDivider = dwDivider;
	}

	public void setlMaxValue(int lMaxValue) {
		this.lMaxValue = lMaxValue;
	}

	public void setlMinMaxDivider(int lMinMaxDivider) {
		this.lMinMaxDivider = lMinMaxDivider;
	}

	public void setlMinValue(int lMinValue) {
		this.lMinValue = lMinValue;
	}

	public void setnBits(long nBits) {
		this.nBits = nBits;
	}

	public void setSzID(String szID) {
		this.szID = szID;
	}

	public void setSzUnit(String szUnit) {
		szUnit = szUnit.replace("Ohm", "\u2126");
		szUnit = szUnit.replace("ohm", "\u2126");
		szUnit = szUnit.replace("deg", "\u00B0");
		szUnit = szUnit.replace("uS", "\u00B5S");
		this.szUnit = szUnit;
	}

	public void setTickFile(String tickFile) {
		this.tickFile = tickFile;
	}

	public void setRealSlope(double realSlope) {
		this.realSlope = realSlope;
	}

	public void setRealConstant(double realConstant) {
		this.realConstant = realConstant;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public void setConstants(Map<String, Double> constants) {
		this.constants = constants;
	}
}
