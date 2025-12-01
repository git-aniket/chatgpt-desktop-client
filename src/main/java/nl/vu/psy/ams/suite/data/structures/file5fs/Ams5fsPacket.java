package nl.vu.psy.ams.suite.data.structures.file5fs;

import java.util.ArrayList;
import java.util.LinkedList;

import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;

/*
 * Packet info from 5fs file (copied from 5fs code)
 * Can represent:
 * - event
 * - start
 * - summary
 * - statics
 * - settings
 * - data
 * - labelex
 */
public class Ams5fsPacket {

	public static final int PACKET_TYPE_EVENT = 3;
	public static final int PACKET_TYPE_START = 4;
	public static final int PACKET_TYPE_SUMMARY = 5;
	public static final int PACKET_TYPE_STATICS = 6;
	public static final int PACKET_TYPE_SETTINGS = 7;
	public static final int PACKET_TYPE_DATABLOCK = 0x8000;
	public static final Integer PACKET_TYPE_LABELEX = 12;

	// public static void setData(int[] data) {
	// Ams5fsPacket.data = data;
	// }
	protected Integer wTag; // 00 -

	// type
	// tag
	// to
	// identify
	// Event
	// structure
	protected Integer wSize; // 02 -
	// total
	// nr
	// of
	// Bytes
	// of
	// this
	// structure
	// Event:
	protected Long dwClockTick_ms; // 04 -
	// clock
	// tick
	protected Integer lType; // 08 -

	// event
	// type
	// (see
	// define
	// under
	// AMSII_USER_SERIAL_EVENT)
	protected Integer lCode; // 0C -
	// event
	// code

	protected String szMessage; // 10 -

	protected Integer wStructVersion; // 04

	// Statics:

	protected Integer reservedOldSize; // 06 -
	// was a
	// redundant
	// wSize in
	// files made
	// with
	// software
	// version 1
	protected Long dwDeviceID; // 08
	protected Long dwBaudrate; // 0C
	protected Integer wSerialnr; // 10
	protected Integer wHardwareVersion; // 12
	// see
	// AMS_DEVICE_SOFTWAREVERSION
	protected Integer wCalSCLdcLevel; // 14
	protected Integer wCalibratieB; // 16
	protected Integer wCalibratieC; // 18
	protected Integer wCalibratieD; // 1A
	protected String reservedA; // 1C
	protected Long dwChannelDisableMask; // 28
	// Channel
	// disable mask
	// (0 = enable
	// all
	// channels)
	protected Byte cSclMode; // 2C
	// one
	// of
	// AMSII_SCL_MODE
	protected String reservedB; // 2D
	protected String szSubjectID; // 08 -

	// subject/recording
	// ID
	protected Long dwSampleTime_us; // 14 -
	// master
	// sample
	// Integererval
	// time in
	// microseconds
	// (reserved??)
	protected Long dwSession; // 18 -
	// recording
	// session
	// sequence
	// number
	// (to
	// RTC/EEPROM)
	protected Long dwSettingsFlags; // 1C -

	// See
	// AMSII
	// dwSettingsFlags
	// definitions
	protected Integer wMinButDownTime; // 20

	protected Integer wLongButDownTime; // 22
	protected Integer wProtocolTimeout; // 24 -
	// 700ms
	// in
	// EMDII
	protected Integer wProtocolRetries; // 26
	protected Integer wCFTimeout; // 28
	protected Integer wMonBATDivider; // 2A

	protected Integer wBATEmptyThreshold; // 2C
	protected Integer wBATLowThreshold; // 2E
	protected Integer wLDRThreshold; // 30
	protected Integer wElectrodeDistance; // 32 -
	// ICG-V
	// distance in
	// mm (VERSION
	// 3+)
	protected Integer wReserved2; // 34
	protected Integer wReserved3; // 36
	// daylight
	// saving
	// time
	// parameters
	protected Ams5fsTimeZoneDate dstBegin; // 38 -

	// start
	// of
	// daylight
	// saving time
	// (zomertijd)
	protected Ams5fsTimeZoneDate dstEnd; // 3C -
	// end
	// of
	// daylight
	// saving time
	// (zomertijd)
	protected Integer lTimeZoneJump; // 40 -
	// time
	// shift
	// (normally
	// 60
	// minutes)
	protected Short cAlertOn_hour; // 44

	protected Short cAlertOn_minute; // 45
	protected Short cAlertOff_hour; // 46
	protected Short cAlertOff_minute; // 47
	protected Integer wAlertInterval_minutes; // 48 -
	// 0 to
	// disable
	protected Integer wAlertRanomization_minutes; // 4A
	// channel info
	protected Long nChannels; // 4C -

	// #
	// available
	// channels
	LinkedList<Ams5fsChannelInfo> channels; // 50 -
											// 21*64
											// =
											// 1344 Bytes
	// fill structure with extra variables from bottom so there can be channels
	// added later!
	protected String szReserved; // 590
	// <<
	// insert
	// here
	// (don't
	// forget
	// to
	// init
	// new
	// variable
	// in
	// amsIIdefs.cpp
	// AND
	// Main.cpp!)
	protected Long dwFlushInterval; // 5E4 -
	// FlushIntegererval
	// in ms
	// to
	// update
	// FAT
	// table
	protected Integer wZ0inRangeIgnore; // 5E8

	protected Integer wSCLinRangeIgnore; // 5EA
	protected Integer wECGinRangeIgnore; // 5EC
	protected Integer wIresInRangeIgnore; // 5EE
	protected Integer wZ0ThresholdMin; // 5F0

	protected Integer wZ0ThresholdMax; // 5F2
	protected Integer wSCLThresholdMin; // 5F4
	protected Integer wSCLThresholdMax; // 5F6
	protected Integer wECGThresholdMin; // 5F8
	protected Integer wECGThresholdMax; // 5FA
	protected Integer wIresThresholdMin; // 5FC -
	// range
	// check ICG
	// current
	// resistance?
	protected Integer wIresThresholdMax; // 5FE -
	// range
	// check
	// ICG
	// current
	// resistance?
	protected Long dwPrevSummaryPos; // 08 -

	// file
	// position
	// of
	// previous
	// summary
	protected Long dwReserved; // 0C -
	protected Ams5fsTime tStamp; // 10 -

	// RTC
	// time
	// and
	// date
	protected Byte cFileStartReason; // 18 -
	// Reason
	// file is
	// started. See
	// AMSII_START_.......
	// (AMS_DEVICE_SOFTWAREVERSION
	// =>7)
	protected String cReserved; // 19 -
	protected int[] data = null;

	protected Long dwTotalNumberOfBlocks; // 10 -
	// total

	// #continues
	protected Long dwTotalNumberOfEvents; // 14 -
	// total
	// #events in
	// file
	protected Long dwTotalNumberOfScans; // 18 -
	// total
	// #scans
	// (one
	// AD-scan
	// per
	// clock
	// tick)
	protected Long dwNumberOfEventsInBlock; // 1C -
	// total
	// #events
	// in
	// file
	protected Long dwNumberOfScansInBlock; // 20 -

	// total
	// #scans (one
	// AD-scan per
	// clock tick)
	protected Long dwStartPos; // 24 -
	// file
	// position of
	// block start
	protected Long dwReserved1; // 2C -
	protected Long dwReserved2; // 30 -

	protected Long dwReserved3; // 34 -
	protected Integer wFileStopReason; // 38 -
	// Reason
	// file is
	// stopped. See
	// AMSII_STOP_.......
	// (AMS_DEVICE_SOFTWAREVERSION
	// =>7)
	protected Integer wSizeR; // 3A -
	// Size
	// of
	// this
	// structure
	// (for Reversed
	// parsing)
	protected Long dwSignature; // 3C -
	// 0x594D5553=='SUMY',
	// signature
	// to
	// identify
	// this
	// structure
	// (for
	// reversed
	// scanning)
	// protected Integer offset;

	protected Long dwBegin_ms;

	protected Long dwEnd_ms;
	protected ArrayList<Integer> lCodes;
	protected Byte[] otherData;

	public Ams5fsPacket() {

	}

	public Ams5fsPacket(UnsignedByteBuffer buff, int wTag, int wSize) {
		this.wTag = wTag;
		this.wSize = wSize;

		if (wTag == PACKET_TYPE_EVENT) {
			data = null;
			ReadEventPacket(buff);
		} else if (wTag == PACKET_TYPE_STATICS) {
			data = null;
			ReadStaticsPacket(buff);
		} else if (wTag == PACKET_TYPE_SETTINGS) {
			data = null;
			ReadSettingsPacket(buff);
		} else if (wTag == PACKET_TYPE_START) {
			data = null;
			ReadStartPacket(buff);
		} else if (wTag == PACKET_TYPE_SUMMARY) {
			data = null;
			ReadSummaryPacket(buff);
		} else if (wTag == PACKET_TYPE_LABELEX) {
			data = null;
			ReadLabelExPacket(buff);
		} else if (wTag >= PACKET_TYPE_DATABLOCK) {
			ReadDataPacket(buff);
		} else {
			data = null;
			ReadGeneralPacket(buff);
		}
	}

	public Short getcAlertOff_hour() {
		return cAlertOff_hour;
	}

	public Short getcAlertOff_minute() {
		return cAlertOff_minute;
	}

	public Short getcAlertOn_hour() {
		return cAlertOn_hour;
	}

	public Short getcAlertOn_minute() {
		return cAlertOn_minute;
	}

	public Byte getcFileStartReason() {
		return cFileStartReason;
	}

	public String getcFileStartReasonString() {
		// from amsIIdefs.h
		if (cFileStartReason == null)
			return "";
		switch (cFileStartReason) {
			case 0:
				return "AMSII_START_UNKNOWN";
			case 1:
				return "AMSII_START_BUTTON";
			case 2:
				return "AMSII_START_SERIAL";
			case 3:
				return "AMSII_START_COVER";
			case 4:
				return "AMSII_START_STOP";
			case 5:
				return "AMSII_START_RESET";
			case 6:
				return "AMSII_START_WATCHDOG";
			case 7:
				return "AMSII_START_STOP_BUT";
			case 8:
				return "AMSII_START_STOP_COV";
			case 9:
				return "AMSII_START_STOP_BAT";
			case 10:
				return "AMSII_START_RESET_STOP_BUT";
			case 11:
				return "AMSII_START_RESET_STOP_COV";
			case 12:
				return "AMSII_START_RESET_STOP_BAT";
			case 13:
				return "AMSII_START_WATCHDOG_STOP_BUT";
			case 14:
				return "AMSII_START_WATCHDOG_STOP_COV";
			case 15:
				return "AMSII_START_WATCHDOG_STOP_BAT";
			default:
				return "ERROR this should nog happen";
		}
	}

	public LinkedList<Ams5fsChannelInfo> getChannels() {
		return channels;
	}

	public String getcReserved() {
		return cReserved;
	}

	public Byte getcSclMode() {
		return cSclMode;
	}

	public int[] getData() {
		return data;
	}

	public Ams5fsTimeZoneDate getDstBegin() {
		return dstBegin;
	}

	public Ams5fsTimeZoneDate getDstEnd() {
		return dstEnd;
	}

	public Long getDwBaudrate() {
		return dwBaudrate;
	}

	public Long getDwBegin_ms() {
		return dwBegin_ms;
	}

	public Long getDwChannelDisableMask() {
		return dwChannelDisableMask;
	}

	public Long getDwClockTick_ms() {
		return dwClockTick_ms;
	}

	public Long getDwDeviceID() {
		return dwDeviceID;
	}

	public Long getDwEnd_ms() {
		return dwEnd_ms;
	}

	public Long getDwFlushInterval() {
		return dwFlushInterval;
	}

	public Long getDwNumberOfEventsInBlock() {
		return dwNumberOfEventsInBlock;
	}

	public Long getDwNumberOfScansInBlock() {
		return dwNumberOfScansInBlock;
	}

	public Long getDwPrevSummaryPos() {
		return dwPrevSummaryPos;
	}

	public Long getDwReserved() {
		return dwReserved;
	}

	public Long getDwReserved1() {
		return dwReserved1;
	}

	public Long getDwReserved2() {
		return dwReserved2;
	}

	public Long getDwReserved3() {
		return dwReserved3;
	}

	public Long getDwSampleTime_us() {
		return dwSampleTime_us;
	}

	public Long getDwSession() {
		return dwSession;
	}

	public Long getDwSettingsFlags() {
		return dwSettingsFlags;
	}

	public String getDwSettingsFlagsStringUnpacked() {
		String t = "";

		if ((dwSettingsFlags & 0x00000001) == 0x00000001)
			t += "-AMSII_FLAG_BUZDISABLE\t\t: >buzzer disabled" + "\n";
		else
			t += "-AMSII_FLAG_BUZDISABLE\t\t: buzzer enabled" + "\n";

		if ((dwSettingsFlags & 0x00000002) == 0x00000002)
			t += "-AMSII_FLAG_LEDDISABLE\t\t: >LED disabled" + "\n";
		else
			t += "-AMSII_FLAG_LEDDISABLE\t\t: LED enabled" + "\n";

		if ((dwSettingsFlags & 0x00000004) == 0x00000004)
			t += "-AMSII_FLAG_UNMOUNTMODE\t\t: >manual" + "\n";
		else
			t += "-AMSII_FLAG_UNMOUNTMODE\t\t: LDR" + "\n";

		if ((dwSettingsFlags & 0x00000008) == 0x00000008)
			t += "-AMSII_FLAG_SCLAC\t\t: >AC" + "\n";
		else
			t += "-AMSII_FLAG_SCLAC\t\t: DC" + "\n";

		if ((dwSettingsFlags & 0x00000010) == 0x00000010)
			t += "-AMSII_FLAG_BATEMPTY_BEEP\t: >battery empty beep disabled" + "\n";
		else
			t += "-AMSII_FLAG_BATEMPTY_BEEP\t: battery empty beep enabled" + "\n";

		if ((dwSettingsFlags & 0x00000020) == 0x00000020)
			t += "-AMSII_FLAG_BATLOW_BEEP\t\t: >battery low beep disabled" + "\n";
		else
			t += "-AMSII_FLAG_BATLOW_BEEP\t\t: battery low beep enabled" + "\n";

		if ((dwSettingsFlags & 0x00000040) == 0x00000040)
			t += "-AMSII_FLAG_BATLOW\t\t: >battery low check disabled" + "\n";
		else
			t += "-AMSII_FLAG_BATLOW\t\t: battery low check enabled" + "\n";

		if ((dwSettingsFlags & 0x00000080) == 0x00000080)
			t += "-AMSII_FLAG_ECGRANGE_BEEP\t: >ECG out of range beep disabled" + "\n";
		else
			t += "-AMSII_FLAG_ECGRANGE_BEEP\t: ECG out of range beep enabled" + "\n";

		if ((dwSettingsFlags & 0x00000100) == 0x00000100)
			t += "-AMSII_FLAG_ICGRANGE_BEEP\t: >ICG out of range beep disabled" + "\n";
		else
			t += "-AMSII_FLAG_ICGRANGE_BEEP\t: ICG out of range beep enabled" + "\n";

		if ((dwSettingsFlags & 0x00000200) == 0x00000200)
			t += "-AMSII_FLAG_SCLRANGE_BEEP\t: >SCL out of range beep disabled" + "\n";
		else
			t += "-AMSII_FLAG_SCLRANGE_BEEP\t: SCL out of range beep enabled" + "\n";

		if ((dwSettingsFlags & 0x00000400) == 0x00000400)
			t += "-AMSII_FLAG_ECGRANGE\t\t: >ECG range check disabled" + "\n";
		else
			t += "-AMSII_FLAG_ECGRANGE\t\t: ECG range check enabled" + "\n";

		if ((dwSettingsFlags & 0x00000800) == 0x00000800)
			t += "-AMSII_FLAG_ICGRANGE\t\t: >ICG range check disabled" + "\n";
		else
			t += "-AMSII_FLAG_ICGRANGE\t\t: ICG range check enabled" + "\n";

		if ((dwSettingsFlags & 0x00001000) == 0x00001000)
			t += "-AMSII_FLAG_SCLRANGE\t\t: >SCL range check disabled" + "\n";
		else
			t += "-AMSII_FLAG_SCLRANGE\t\t: SCL range check enabled" + "\n";

		if ((dwSettingsFlags & 0x00002000) == 0x00002000)
			t += "-AMSII_FLAG_AUTOSTARTSTOPRESET\t: >auto start enabled" + "\n";
		else
			t += "-AMSII_FLAG_AUTOSTARTSTOPRESET\t: auto start disabled" + "\n";

		if ((dwSettingsFlags & 0x00004000) == 0x00004000)
			t += "-AMSII_FLAG_AUTOSTARTCOVER\t: >auto start after battery cover is closed enabled" + "\n";
		else
			t += "-AMSII_FLAG_AUTOSTARTCOVER\t: auto start after battery cover is closed disabled" + "\n";

		if ((dwSettingsFlags & 0x00008000) == 0x00008000)
			t += "-AMSII_FLAG_STARTBUTTON\t\t: >start button disabled" + "\n";
		else
			t += "-AMSII_FLAG_STARTBUTTON\t\t: start button enabled" + "\n";

		if ((dwSettingsFlags & 0x00010000) == 0x00010000)
			t += "-AMSII_FLAG_STOPBUTTON\t\t: >stop button disabled" + "\n";
		else
			t += "-AMSII_FLAG_STOPBUTTON\t\t: stop button enabled" + "\n";

		return t;
	}

	public Long getDwSignature() {
		return dwSignature;
	}

	public Long getDwStartPos() {
		return dwStartPos;
	}

	public Long getDwTotalNumberOfBlocks() {
		return dwTotalNumberOfBlocks;
	}

	public Long getDwTotalNumberOfEvents() {
		return dwTotalNumberOfEvents;
	}

	public Long getDwTotalNumberOfScans() {
		return dwTotalNumberOfScans;
	}

	public Integer getlCode() {
		return lCode;
	}

	public ArrayList<Integer> getlCodes() {
		return lCodes;
	}

	public Integer getlTimeZoneJump() {
		return lTimeZoneJump;
	}

	public Integer getlType() {
		return lType;
	}

	public Long getnChannels() {
		return nChannels;
	}

	// public Integer getOffset() {
	// return offset;
	// }

	public Byte[] getOtherData() {
		return otherData;
	}

	public String getReservedA() {
		return reservedA;
	}

	public String getReservedB() {
		return reservedB;
	}

	public Integer getReservedOldSize() {
		return reservedOldSize;
	}

	public String getSzMessage() {
		return szMessage;
	}

	public String getSzReserved() {
		return szReserved;
	}

	public String getSzSubjectID() {
		return szSubjectID;
	}

	public Ams5fsTime gettStamp() {
		return tStamp;
	}

	public Integer getwAlertInterval_minutes() {
		return wAlertInterval_minutes;
	}

	public Integer getwAlertRanomization_minutes() {
		return wAlertRanomization_minutes;
	}

	public Integer getwBATEmptyThreshold() {
		return wBATEmptyThreshold;
	}

	public Integer getwBATLowThreshold() {
		return wBATLowThreshold;
	}

	public Integer getwCalibratieB() {
		return wCalibratieB;
	}

	public Integer getwCalibratieC() {
		return wCalibratieC;
	}

	public Integer getwCalibratieD() {
		return wCalibratieD;
	}

	public Integer getwCalSCLdcLevel() {
		return wCalSCLdcLevel;
	}

	public Integer getwCFTimeout() {
		return wCFTimeout;
	}

	public Integer getwECGinRangeIgnore() {
		return wECGinRangeIgnore;
	}

	public Integer getwECGThresholdMax() {
		return wECGThresholdMax;
	}

	public Integer getwECGThresholdMin() {
		return wECGThresholdMin;
	}

	public Integer getwElectrodeDistance() {
		return wElectrodeDistance;
	}

	public Integer getwFileStopReason() {
		return wFileStopReason;
	}

	public String getwFileStopReasonString() {
		// from amsIIdefs.h
		switch (wFileStopReason) {
			case 0:
				return "AMSII_STOP_UNKNOWN";
			case 1:
				return "AMSII_STOP_BUTTON";
			case 2:
				return "AMSII_STOP_SERIAL";
			case 3:
				return "AMSII_STOP_BATTERY";
			case 4:
				return "AMSII_STOP_CF";
			case 5:
				return "AMSII_STOP_COVER";
			default:
				return "ERROR this should nog happen";
		}
	}

	public Integer getwHardwareVersion() {
		return wHardwareVersion;
	}

	public Integer getwIresInRangeIgnore() {
		return wIresInRangeIgnore;
	}

	public Integer getwIresThresholdMax() {
		return wIresThresholdMax;
	}

	public Integer getwIresThresholdMin() {
		return wIresThresholdMin;
	}

	public Integer getwLDRThreshold() {
		return wLDRThreshold;
	}

	public Integer getwLongButDownTime() {
		return wLongButDownTime;
	}

	public Integer getwMinButDownTime() {
		return wMinButDownTime;
	}

	public Integer getwMonBATDivider() {
		return wMonBATDivider;
	}

	public Integer getwProtocolRetries() {
		return wProtocolRetries;
	}

	public Integer getwProtocolTimeout() {
		return wProtocolTimeout;
	}

	public Integer getwReserved2() {
		return wReserved2;
	}

	public Integer getwReserved3() {
		return wReserved3;
	}

	public Integer getwSCLinRangeIgnore() {
		return wSCLinRangeIgnore;
	}

	public Integer getwSCLThresholdMax() {
		return wSCLThresholdMax;
	}

	public Integer getwSCLThresholdMin() {
		return wSCLThresholdMin;
	}

	public Integer getwSerialnr() {
		return wSerialnr;
	}

	public Integer getwSize() {
		return wSize;
	}

	public Integer getwSizeR() {
		return wSizeR;
	}

	public Integer getwStructVersion() {
		return wStructVersion;
	}

	public int getwTag() {
		return wTag;
	}

	public Integer getwZ0inRangeIgnore() {
		return wZ0inRangeIgnore;
	}

	public Integer getwZ0ThresholdMax() {
		return wZ0ThresholdMax;
	}

	public Integer getwZ0ThresholdMin() {
		return wZ0ThresholdMin;
	}

	private void ReadDataPacket(UnsignedByteBuffer buff) {
		int n = (wSize - 4) / 2;
		if (data == null || n != data.length)
			data = new int[n];
		for (int i = 0; i < n; i++)
			data[i] = buff.getUShort();
	}

	private void ReadEventPacket(UnsignedByteBuffer buff) {
		dwClockTick_ms = buff.getUInt();
		lType = buff.getInt();
		lCode = buff.getInt();
		szMessage = buff.getString(32);
	}

	private void ReadGeneralPacket(UnsignedByteBuffer buff) {
		otherData = new Byte[this.wSize - 4];
		for (int i = 0; i < this.wSize - 4; i++)
			otherData[i] = buff.getByte();
	}

	private void ReadLabelExPacket(UnsignedByteBuffer buff) {
		dwBegin_ms = buff.getUInt();
		dwEnd_ms = buff.getUInt();
		lType = buff.getInt();
		// lCodes = new ArrayList<Integer>();
		// for(int i=0;i<8;i++) lCodes.add(buff.getInt());
		// szMessage = buff.getString(128);
	}

	private void ReadSettingsPacket(UnsignedByteBuffer buff) {
		wStructVersion = buff.getUShort();
		reservedOldSize = buff.getUShort();
		szSubjectID = buff.getString(12);
		dwSampleTime_us = buff.getUInt();
		dwSession = buff.getUInt();
		dwSettingsFlags = buff.getUInt();
		wMinButDownTime = buff.getUShort();
		wLongButDownTime = buff.getUShort();
		wProtocolTimeout = buff.getUShort();
		wProtocolRetries = buff.getUShort();
		wCFTimeout = buff.getUShort();
		wMonBATDivider = buff.getUShort();
		wBATEmptyThreshold = buff.getUShort();
		wBATLowThreshold = buff.getUShort();
		wLDRThreshold = buff.getUShort();
		wElectrodeDistance = buff.getUShort();
		wReserved2 = buff.getUShort();
		wReserved3 = buff.getUShort();
		dstBegin = new Ams5fsTimeZoneDate(buff);
		dstEnd = new Ams5fsTimeZoneDate(buff);
		lTimeZoneJump = buff.getInt();
		cAlertOn_hour = (short) buff.getUByte();
		cAlertOn_minute = (short) buff.getUByte();
		cAlertOff_hour = (short) buff.getUByte();
		cAlertOff_minute = (short) buff.getUByte();
		wAlertInterval_minutes = buff.getUShort();
		wAlertRanomization_minutes = buff.getUShort();
		nChannels = buff.getUInt();
		channels = new LinkedList<Ams5fsChannelInfo>();
		for (int i = 0; i < 21; i++)
			channels.add(new Ams5fsChannelInfo(buff));
		szReserved = buff.getString(84);
		dwFlushInterval = buff.getUInt();
		wZ0inRangeIgnore = buff.getUShort();
		wSCLinRangeIgnore = buff.getUShort();
		wECGinRangeIgnore = buff.getUShort();
		wIresInRangeIgnore = buff.getUShort();
		wZ0ThresholdMin = buff.getUShort();
		wZ0ThresholdMax = buff.getUShort();
		wSCLThresholdMin = buff.getUShort();
		wSCLThresholdMax = buff.getUShort();
		wECGThresholdMin = buff.getUShort();
		wECGThresholdMax = buff.getUShort();
		wIresThresholdMin = buff.getUShort();
		wIresThresholdMax = buff.getUShort();
	}

	private void ReadStartPacket(UnsignedByteBuffer buff) {
		dwClockTick_ms = buff.getUInt();
		dwPrevSummaryPos = buff.getUInt();
		dwReserved = buff.getUInt();
		tStamp = new Ams5fsTime(buff);
		cFileStartReason = buff.getByte();
		cReserved = buff.getString(7);
	}

	private void ReadStaticsPacket(UnsignedByteBuffer buff) {
		wStructVersion = buff.getUShort();
		reservedOldSize = buff.getUShort();
		dwDeviceID = buff.getUInt();
		dwBaudrate = buff.getUInt();
		wSerialnr = buff.getUShort();
		wHardwareVersion = buff.getUShort();
		wCalSCLdcLevel = buff.getUShort();
		wCalibratieB = buff.getUShort();
		wCalibratieC = buff.getUShort();
		wCalibratieD = buff.getUShort();
		reservedA = buff.getString(12);
		dwChannelDisableMask = buff.getUInt();
		cSclMode = buff.getByte();
		reservedB = buff.getString(3);
	}

	private void ReadSummaryPacket(UnsignedByteBuffer buff) {
		dwClockTick_ms = buff.getUInt();
		tStamp = new Ams5fsTime(buff);
		dwTotalNumberOfBlocks = buff.getUInt();
		dwTotalNumberOfEvents = buff.getUInt();
		dwTotalNumberOfScans = buff.getUInt();
		dwNumberOfEventsInBlock = buff.getUInt();
		dwNumberOfScansInBlock = buff.getUInt();
		dwStartPos = buff.getUInt();
		dwPrevSummaryPos = buff.getUInt();
		dwReserved1 = buff.getUInt();
		dwReserved2 = buff.getUInt();
		dwReserved3 = buff.getUInt();
		wFileStopReason = buff.getUShort();
		wSizeR = buff.getUShort();
		dwSignature = buff.getUInt();
	}

	public void setcAlertOff_hour(Short cAlertOff_hour) {
		this.cAlertOff_hour = cAlertOff_hour;
	}

	public void setcAlertOff_minute(Short cAlertOff_minute) {
		this.cAlertOff_minute = cAlertOff_minute;
	}

	public void setcAlertOn_hour(Short cAlertOn_hour) {
		this.cAlertOn_hour = cAlertOn_hour;
	}

	public void setcAlertOn_minute(Short cAlertOn_minute) {
		this.cAlertOn_minute = cAlertOn_minute;
	}

	public void setcFileStartReason(Byte cFileStartReason) {
		this.cFileStartReason = cFileStartReason;
	}

	public void setChannels(LinkedList<Ams5fsChannelInfo> channels) {
		this.channels = channels;
	}

	public void setcReserved(String cReserved) {
		this.cReserved = cReserved;
	}

	public void setcSclMode(Byte cSclMode) {
		this.cSclMode = cSclMode;
	}

	public void setDstBegin(Ams5fsTimeZoneDate dstBegin) {
		this.dstBegin = dstBegin;
	}

	public void setDstEnd(Ams5fsTimeZoneDate dstEnd) {
		this.dstEnd = dstEnd;
	}

	public void setDwBaudrate(Long dwBaudrate) {
		this.dwBaudrate = dwBaudrate;
	}

	public void setDwBegin_ms(Long dwBegin_ms) {
		this.dwBegin_ms = dwBegin_ms;
	}

	public void setDwChannelDisableMask(Long dwChannelDisableMask) {
		this.dwChannelDisableMask = dwChannelDisableMask;
	}

	public void setDwClockTick_ms(Long dwClockTick_ms) {
		this.dwClockTick_ms = dwClockTick_ms;
	}

	public void setDwDeviceID(Long dwDeviceID) {
		this.dwDeviceID = dwDeviceID;
	}

	public void setDwEnd_ms(Long dwEnd_ms) {
		this.dwEnd_ms = dwEnd_ms;
	}

	public void setDwFlushInterval(Long dwFlushInterval) {
		this.dwFlushInterval = dwFlushInterval;
	}

	public void setDwNumberOfEventsInBlock(Long dwNumberOfEventsInBlock) {
		this.dwNumberOfEventsInBlock = dwNumberOfEventsInBlock;
	}

	public void setDwNumberOfScansInBlock(Long dwNumberOfScansInBlock) {
		this.dwNumberOfScansInBlock = dwNumberOfScansInBlock;
	}

	public void setDwPrevSummaryPos(Long dwPrevSummaryPos) {
		this.dwPrevSummaryPos = dwPrevSummaryPos;
	}

	public void setDwReserved(Long dwReserved) {
		this.dwReserved = dwReserved;
	}

	public void setDwReserved1(Long dwReserved1) {
		this.dwReserved1 = dwReserved1;
	}

	public void setDwReserved2(Long dwReserved2) {
		this.dwReserved2 = dwReserved2;
	}

	public void setDwReserved3(Long dwReserved3) {
		this.dwReserved3 = dwReserved3;
	}

	public void setDwSampleTime_us(Long dwSampleTime_us) {
		this.dwSampleTime_us = dwSampleTime_us;
	}

	public void setDwSession(Long dwSession) {
		this.dwSession = dwSession;
	}

	public void setDwSettingsFlags(Long dwSettingsFlags) {
		this.dwSettingsFlags = dwSettingsFlags;
	}

	public void setDwSignature(Long dwSignature) {
		this.dwSignature = dwSignature;
	}

	public void setDwStartPos(Long dwStartPos) {
		this.dwStartPos = dwStartPos;
	}

	public void setDwTotalNumberOfBlocks(Long dwTotalNumberOfBlocks) {
		this.dwTotalNumberOfBlocks = dwTotalNumberOfBlocks;
	}

	public void setDwTotalNumberOfEvents(Long dwTotalNumberOfEvents) {
		this.dwTotalNumberOfEvents = dwTotalNumberOfEvents;
	}

	public void setDwTotalNumberOfScans(Long dwTotalNumberOfScans) {
		this.dwTotalNumberOfScans = dwTotalNumberOfScans;
	}

	public void setlCode(Integer lCode) {
		this.lCode = lCode;
	}

	public void setlCodes(ArrayList<Integer> lCodes) {
		this.lCodes = lCodes;
	}

	public void setlTimeZoneJump(Integer lTimeZoneJump) {
		this.lTimeZoneJump = lTimeZoneJump;
	}

	public void setlType(Integer lType) {
		this.lType = lType;
	}

	public void setnChannels(Long nChannels) {
		this.nChannels = nChannels;
	}

	// public void setOffset(int offset) {
	// this.offset = offset;
	// }

	// public void setOffset(Integer offset) {
	// this.offset = offset;
	// }

	public void setOtherData(Byte[] otherData) {
		this.otherData = otherData;
	}

	public void setReservedA(String reservedA) {
		this.reservedA = reservedA;
	}

	public void setReservedB(String reservedB) {
		this.reservedB = reservedB;
	}

	public void setReservedOldSize(Integer reservedOldSize) {
		this.reservedOldSize = reservedOldSize;
	}

	public void setSzMessage(String szMessage) {
		this.szMessage = szMessage;
	}

	public void setSzReserved(String szReserved) {
		this.szReserved = szReserved;
	}

	public void setSzSubjectID(String szSubjectID) {
		this.szSubjectID = szSubjectID;
	}

	public void settStamp(Ams5fsTime tStamp) {
		this.tStamp = tStamp;
	}

	public void setwAlertInterval_minutes(Integer wAlertInterval_minutes) {
		this.wAlertInterval_minutes = wAlertInterval_minutes;
	}

	public void setwAlertRanomization_minutes(Integer wAlertRanomization_minutes) {
		this.wAlertRanomization_minutes = wAlertRanomization_minutes;
	}

	public void setwBATEmptyThreshold(Integer wBATEmptyThreshold) {
		this.wBATEmptyThreshold = wBATEmptyThreshold;
	}

	public void setwBATLowThreshold(Integer wBATLowThreshold) {
		this.wBATLowThreshold = wBATLowThreshold;
	}

	public void setwCalibratieB(Integer wCalibratieB) {
		this.wCalibratieB = wCalibratieB;
	}

	public void setwCalibratieC(Integer wCalibratieC) {
		this.wCalibratieC = wCalibratieC;
	}

	public void setwCalibratieD(Integer wCalibratieD) {
		this.wCalibratieD = wCalibratieD;
	}

	public void setwCalSCLdcLevel(Integer wCalSCLdcLevel) {
		this.wCalSCLdcLevel = wCalSCLdcLevel;
	}

	public void setwCFTimeout(Integer wCFTimeout) {
		this.wCFTimeout = wCFTimeout;
	}

	public void setwECGinRangeIgnore(Integer wECGinRangeIgnore) {
		this.wECGinRangeIgnore = wECGinRangeIgnore;
	}

	public void setwECGThresholdMax(Integer wECGThresholdMax) {
		this.wECGThresholdMax = wECGThresholdMax;
	}

	public void setwECGThresholdMin(Integer wECGThresholdMin) {
		this.wECGThresholdMin = wECGThresholdMin;
	}

	public void setwElectrodeDistance(Integer wElectrodeDistance) {
		this.wElectrodeDistance = wElectrodeDistance;
	}

	public void setwFileStopReason(Integer wFileStopReason) {
		this.wFileStopReason = wFileStopReason;
	}

	public void setwHardwareVersion(Integer wHardwareVersion) {
		this.wHardwareVersion = wHardwareVersion;
	}

	public void setwIresInRangeIgnore(Integer wIresInRangeIgnore) {
		this.wIresInRangeIgnore = wIresInRangeIgnore;
	}

	public void setwIresThresholdMax(Integer wIresThresholdMax) {
		this.wIresThresholdMax = wIresThresholdMax;
	}

	public void setwIresThresholdMin(Integer wIresThresholdMin) {
		this.wIresThresholdMin = wIresThresholdMin;
	}

	public void setwLDRThreshold(Integer wLDRThreshold) {
		this.wLDRThreshold = wLDRThreshold;
	}

	public void setwLongButDownTime(Integer wLongButDownTime) {
		this.wLongButDownTime = wLongButDownTime;
	}

	public void setwMinButDownTime(Integer wMinButDownTime) {
		this.wMinButDownTime = wMinButDownTime;
	}

	public void setwMonBATDivider(Integer wMonBATDivider) {
		this.wMonBATDivider = wMonBATDivider;
	}

	public void setwProtocolRetries(Integer wProtocolRetries) {
		this.wProtocolRetries = wProtocolRetries;
	}

	public void setwProtocolTimeout(Integer wProtocolTimeout) {
		this.wProtocolTimeout = wProtocolTimeout;
	}

	public void setwReserved2(Integer wReserved2) {
		this.wReserved2 = wReserved2;
	}

	public void setwReserved3(Integer wReserved3) {
		this.wReserved3 = wReserved3;
	}

	public void setwSCLinRangeIgnore(Integer wSCLinRangeIgnore) {
		this.wSCLinRangeIgnore = wSCLinRangeIgnore;
	}

	public void setwSCLThresholdMax(Integer wSCLThresholdMax) {
		this.wSCLThresholdMax = wSCLThresholdMax;
	}

	public void setwSCLThresholdMin(Integer wSCLThresholdMin) {
		this.wSCLThresholdMin = wSCLThresholdMin;
	}

	public void setwSerialnr(Integer wSerialnr) {
		this.wSerialnr = wSerialnr;
	}

	public void setwSize(Integer wSize) {
		this.wSize = wSize;
	}

	public void setwSizeR(Integer wSizeR) {
		this.wSizeR = wSizeR;
	}

	public void setwStructVersion(Integer wStructVersion) {
		this.wStructVersion = wStructVersion;
	}

	public void setwTag(Integer wTag) {
		this.wTag = wTag;
	}

	public void setwZ0inRangeIgnore(Integer wZ0inRangeIgnore) {
		this.wZ0inRangeIgnore = wZ0inRangeIgnore;
	}

	public void setwZ0ThresholdMax(Integer wZ0ThresholdMax) {
		this.wZ0ThresholdMax = wZ0ThresholdMax;
	}

	public void setwZ0ThresholdMin(Integer wZ0ThresholdMin) {
		this.wZ0ThresholdMin = wZ0ThresholdMin;
	}

}
