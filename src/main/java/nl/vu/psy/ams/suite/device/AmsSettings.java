package nl.vu.psy.ams.suite.device;

import java.util.Arrays;

import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Settings structure from the AMS device.
 * (Can probably be merged with the one from 5fs file,
 * with a little bit of work)
 */
public class AmsSettings {
	public static final int	SUBJECT_ID_SIZE	= 12;

	int						wTag;
	int						wSize;
	int						wStructVersion;
	int						reservedOldSize;
	int						szSubjectID[];
	long					dwSampleTime_us;
	long					dwSession;
	long					dwSettingsFlags;
	int						wMinButDownTime;
	int						wLongButDownTime;
	int						wProtocolTimeout;
	int						wProtocolRetries;
	int						wCFTimeout;
	int						wMonBATDivider;
	int						wBATEmptyThreshold;
	int						wBATLowThreshold;
	int						wLDRThreshold;
	int						wElectrodeDistance;
	int						wReserved2;
	int						wReserved3;
	int						dst_beg_month;
	int						dst_beg_week;
	int						dst_beg_dayOfWeek;
	int						dst_beg_hour;
	int						dst_end_month;
	int						dst_end_week;
	int						dst_end_dayOfWeek;
	int						dst_end_hour;
	int						lTimeZoneJump;
	int						cAlertOn_hour;
	int						cAlertOn_minute;
	int						cAlertOff_hour;
	int						cAlertOff_minute;
	int						wAlertInterval_minutes;
	int						wAlertRanomization_minutes;
	long					nChannels;
	AmsChannelInfo			channels[];
	int						szReserved[];
	long					dwFlushInterval;
	int						wZ0inRangeIgnore;
	int						wSCLinRangeIgnore;
	int						wECGinRangeIgnore;
	int						wIresInRangeIgnore;
	int						wZ0ThresholdMin;
	int						wZ0ThresholdMax;
	int						wSCLThresholdMin;
	int						wSCLThresholdMax;
	int						wECGThresholdMin;
	int						wECGThresholdMax;
	int						wIresThresholdMin;
	int						wIresThresholdMax;

	AmsSettings() {
		szSubjectID = new int[AmsSettings.SUBJECT_ID_SIZE];
		channels = new AmsChannelInfo[AmsDeviceConstants.NCHANNELS];
		for (int i = 0; i < AmsDeviceConstants.NCHANNELS; i++)
			channels[i] = new AmsChannelInfo();
		szReserved = new int[84];
	}

	public AmsSettings deepCopy() {
		AmsSettings ns = new AmsSettings();
		ns.wTag = wTag;
		ns.wSize = wSize;
		ns.wStructVersion = wStructVersion;
		ns.reservedOldSize = reservedOldSize;
		for (int i = 0; i < AmsSettings.SUBJECT_ID_SIZE; i++)
			ns.szSubjectID[i] = szSubjectID[i];
		ns.dwSampleTime_us = dwSampleTime_us;
		ns.dwSession = dwSession;
		ns.dwSettingsFlags = dwSettingsFlags;
		ns.wMinButDownTime = wMinButDownTime;
		ns.wLongButDownTime = wLongButDownTime;
		ns.wProtocolTimeout = wProtocolTimeout;
		ns.wProtocolRetries = wProtocolRetries;
		ns.wCFTimeout = wCFTimeout;
		ns.wMonBATDivider = wMonBATDivider;
		ns.wBATEmptyThreshold = wBATEmptyThreshold;
		ns.wBATLowThreshold = wBATLowThreshold;
		ns.wLDRThreshold = wLDRThreshold;
		ns.wElectrodeDistance = wElectrodeDistance;
		ns.wReserved2 = wReserved2;
		ns.wReserved3 = wReserved3;
		ns.dst_beg_month = dst_beg_month;
		ns.dst_beg_week = dst_beg_week;
		ns.dst_beg_dayOfWeek = dst_beg_dayOfWeek;
		ns.dst_beg_hour = dst_beg_hour;
		ns.dst_end_month = dst_end_month;
		ns.dst_end_week = dst_end_week;
		ns.dst_end_dayOfWeek = dst_end_dayOfWeek;
		ns.dst_end_hour = dst_end_hour;
		ns.lTimeZoneJump = lTimeZoneJump;
		ns.cAlertOn_hour = cAlertOn_hour;
		ns.cAlertOn_minute = cAlertOn_minute;
		ns.cAlertOff_hour = cAlertOff_hour;
		ns.cAlertOff_minute = cAlertOff_minute;
		ns.wAlertInterval_minutes = wAlertInterval_minutes;
		ns.wAlertRanomization_minutes = wAlertRanomization_minutes;
		ns.nChannels = nChannels;
		for (int i = 0; i < AmsDeviceConstants.NCHANNELS; i++) {
			ns.channels[i].wTag = channels[i].wTag;
			ns.channels[i].wSize = channels[i].wSize;
			for (int j = 0; j < AmsChannelInfo.CHANNEL_ID_SIZE; j++)
				ns.channels[i].szID[j] = channels[i].szID[j];
			ns.channels[i].dwDivider = channels[i].dwDivider;
			ns.channels[i].nBits = channels[i].nBits;
			ns.channels[i].lMinValue = channels[i].lMinValue;
			ns.channels[i].lMaxValue = channels[i].lMaxValue;
			ns.channels[i].lMinMaxDivider = channels[i].lMinMaxDivider;
			for (int j = 0; j < AmsChannelInfo.UNIT_ID_SIZE; j++)
				ns.channels[i].szUnit[j] = channels[i].szUnit[j];
			ns.channels[i].dwChannelTag = channels[i].dwChannelTag;
			ns.channels[i].wBufSize = channels[i].wBufSize;
			ns.channels[i].bStore = channels[i].bStore;
			for (int j = 0; j < 16; j++)
				ns.channels[i].szReserved[j] = channels[i].szReserved[j];

		}
		for (int i = 0; i < 84; i++)
			ns.szReserved[i] = szReserved[i];
		ns.dwFlushInterval = dwFlushInterval;
		ns.wZ0inRangeIgnore = wZ0inRangeIgnore;
		ns.wSCLinRangeIgnore = wSCLinRangeIgnore;
		ns.wECGinRangeIgnore = wECGinRangeIgnore;
		ns.wIresInRangeIgnore = wIresInRangeIgnore;
		ns.wZ0ThresholdMin = wZ0ThresholdMin;
		ns.wZ0ThresholdMax = wZ0ThresholdMax;
		ns.wSCLThresholdMin = wSCLThresholdMin;
		ns.wSCLThresholdMax = wSCLThresholdMax;
		ns.wECGThresholdMin = wECGThresholdMin;
		ns.wECGThresholdMax = wECGThresholdMax;
		ns.wIresThresholdMin = wIresThresholdMin;
		ns.wIresThresholdMax = wIresThresholdMax;

		return ns;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AmsSettings other = (AmsSettings) obj;
		if (cAlertOff_hour != other.cAlertOff_hour)
			return false;
		if (cAlertOff_minute != other.cAlertOff_minute)
			return false;
		if (cAlertOn_hour != other.cAlertOn_hour)
			return false;
		if (cAlertOn_minute != other.cAlertOn_minute)
			return false;
		if (!Arrays.equals(channels, other.channels))
			return false;
		if (dst_beg_dayOfWeek != other.dst_beg_dayOfWeek)
			return false;
		if (dst_beg_hour != other.dst_beg_hour)
			return false;
		if (dst_beg_month != other.dst_beg_month)
			return false;
		if (dst_beg_week != other.dst_beg_week)
			return false;
		if (dst_end_dayOfWeek != other.dst_end_dayOfWeek)
			return false;
		if (dst_end_hour != other.dst_end_hour)
			return false;
		if (dst_end_month != other.dst_end_month)
			return false;
		if (dst_end_week != other.dst_end_week)
			return false;
		if (dwFlushInterval != other.dwFlushInterval)
			return false;
		if (dwSampleTime_us != other.dwSampleTime_us)
			return false;
		if (dwSession != other.dwSession)
			return false;
		if (dwSettingsFlags != other.dwSettingsFlags)
			return false;
		if (lTimeZoneJump != other.lTimeZoneJump)
			return false;
		if (nChannels != other.nChannels)
			return false;
		if (reservedOldSize != other.reservedOldSize)
			return false;
		if (!Arrays.equals(szReserved, other.szReserved))
			return false;
		if (!Arrays.equals(szSubjectID, other.szSubjectID))
			return false;
		if (wAlertInterval_minutes != other.wAlertInterval_minutes)
			return false;
		if (wAlertRanomization_minutes != other.wAlertRanomization_minutes)
			return false;
		if (wBATEmptyThreshold != other.wBATEmptyThreshold)
			return false;
		if (wBATLowThreshold != other.wBATLowThreshold)
			return false;
		if (wCFTimeout != other.wCFTimeout)
			return false;
		if (wECGThresholdMax != other.wECGThresholdMax)
			return false;
		if (wECGThresholdMin != other.wECGThresholdMin)
			return false;
		if (wECGinRangeIgnore != other.wECGinRangeIgnore)
			return false;
		if (wElectrodeDistance != other.wElectrodeDistance)
			return false;
		if (wIresInRangeIgnore != other.wIresInRangeIgnore)
			return false;
		if (wIresThresholdMax != other.wIresThresholdMax)
			return false;
		if (wIresThresholdMin != other.wIresThresholdMin)
			return false;
		if (wLDRThreshold != other.wLDRThreshold)
			return false;
		if (wLongButDownTime != other.wLongButDownTime)
			return false;
		if (wMinButDownTime != other.wMinButDownTime)
			return false;
		if (wMonBATDivider != other.wMonBATDivider)
			return false;
		if (wProtocolRetries != other.wProtocolRetries)
			return false;
		if (wProtocolTimeout != other.wProtocolTimeout)
			return false;
		if (wReserved2 != other.wReserved2)
			return false;
		if (wReserved3 != other.wReserved3)
			return false;
		if (wSCLThresholdMax != other.wSCLThresholdMax)
			return false;
		if (wSCLThresholdMin != other.wSCLThresholdMin)
			return false;
		if (wSCLinRangeIgnore != other.wSCLinRangeIgnore)
			return false;
		if (wSize != other.wSize)
			return false;
		if (wStructVersion != other.wStructVersion)
			return false;
		if (wTag != other.wTag)
			return false;
		if (wZ0ThresholdMax != other.wZ0ThresholdMax)
			return false;
		if (wZ0ThresholdMin != other.wZ0ThresholdMin)
			return false;
		if (wZ0inRangeIgnore != other.wZ0inRangeIgnore)
			return false;
		return true;
	}

	public AmsChannelInfo GetChannel(int i) {
		return channels[i];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cAlertOff_hour;
		result = prime * result + cAlertOff_minute;
		result = prime * result + cAlertOn_hour;
		result = prime * result + cAlertOn_minute;
		result = prime * result + Arrays.hashCode(channels);
		result = prime * result + dst_beg_dayOfWeek;
		result = prime * result + dst_beg_hour;
		result = prime * result + dst_beg_month;
		result = prime * result + dst_beg_week;
		result = prime * result + dst_end_dayOfWeek;
		result = prime * result + dst_end_hour;
		result = prime * result + dst_end_month;
		result = prime * result + dst_end_week;
		result = prime * result + (int) (dwFlushInterval ^ (dwFlushInterval >>> 32));
		result = prime * result + (int) (dwSampleTime_us ^ (dwSampleTime_us >>> 32));
		result = prime * result + (int) (dwSession ^ (dwSession >>> 32));
		result = prime * result + (int) (dwSettingsFlags ^ (dwSettingsFlags >>> 32));
		result = prime * result + lTimeZoneJump;
		result = prime * result + (int) (nChannels ^ (nChannels >>> 32));
		result = prime * result + reservedOldSize;
		result = prime * result + Arrays.hashCode(szReserved);
		result = prime * result + Arrays.hashCode(szSubjectID);
		result = prime * result + wAlertInterval_minutes;
		result = prime * result + wAlertRanomization_minutes;
		result = prime * result + wBATEmptyThreshold;
		result = prime * result + wBATLowThreshold;
		result = prime * result + wCFTimeout;
		result = prime * result + wECGThresholdMax;
		result = prime * result + wECGThresholdMin;
		result = prime * result + wECGinRangeIgnore;
		result = prime * result + wElectrodeDistance;
		result = prime * result + wIresInRangeIgnore;
		result = prime * result + wIresThresholdMax;
		result = prime * result + wIresThresholdMin;
		result = prime * result + wLDRThreshold;
		result = prime * result + wLongButDownTime;
		result = prime * result + wMinButDownTime;
		result = prime * result + wMonBATDivider;
		result = prime * result + wProtocolRetries;
		result = prime * result + wProtocolTimeout;
		result = prime * result + wReserved2;
		result = prime * result + wReserved3;
		result = prime * result + wSCLThresholdMax;
		result = prime * result + wSCLThresholdMin;
		result = prime * result + wSCLinRangeIgnore;
		result = prime * result + wSize;
		result = prime * result + wStructVersion;
		result = prime * result + wTag;
		result = prime * result + wZ0ThresholdMax;
		result = prime * result + wZ0ThresholdMin;
		result = prime * result + wZ0inRangeIgnore;
		return result;
	}

	public void setDivider(int channel, long divider) {
		AmsChannelInfo chan = channels[channel];
		if (divider == 0) {
			chan.bStore = 0;
			chan.dwDivider = 0;
		} else {
			chan.bStore = 1;
			chan.dwDivider = divider;
		}
		if (channel == AmsDeviceConstants.CHX_MYR) {
			setDivider(AmsDeviceConstants.CH_XMT, divider);
			if (Utils.arrayToString(channels[AmsDeviceConstants.CH_Xt1].szID).equals("MZR")) {
				setDivider(AmsDeviceConstants.CH_Xt1, divider);
			}
			if (chan.bStore != 0) {
				channels[AmsDeviceConstants.CH_YMT].dwDivider = 1L;
			}
		}
		if (channel == AmsDeviceConstants.CHX_MYA) {
			if (chan.bStore != 0) {
				channels[AmsDeviceConstants.CH_YMT].dwDivider = 1L;
			}
		}
		if (channel == AmsDeviceConstants.CHX_SCL) {
			if (chan.bStore != 0) {
				channels[AmsDeviceConstants.CH_SCL].dwDivider = 1L;
			}
		}
		if (channel == AmsDeviceConstants.CHX_Z0A) {
			if (chan.bStore != 0) {
				channels[AmsDeviceConstants.CH_Z0].dwDivider = 4L;
			}
		}
	}

	public int[] toDeviceArray() {
		int[] ret = new int[1536];
		AmsHelper h = new AmsHelper();
		h.putUnsignedShortInArray(ret, 0, wTag);
		h.putUnsignedShortInArray(ret, 2, wSize);
		h.putUnsignedShortInArray(ret, 4, wStructVersion);
		h.putUnsignedShortInArray(ret, 6, reservedOldSize);
		for (int i = 0; i < AmsSettings.SUBJECT_ID_SIZE; i++)
			h.putUnsignedCharInArray(ret, 8 + i, szSubjectID[i]);
		h.putUnsignedLongInArray(ret, 20, dwSampleTime_us);
		h.putUnsignedLongInArray(ret, 24, dwSession);
		h.putUnsignedLongInArray(ret, 28, dwSettingsFlags);
		h.putUnsignedShortInArray(ret, 32, wMinButDownTime);
		h.putUnsignedShortInArray(ret, 34, wLongButDownTime);
		h.putUnsignedShortInArray(ret, 36, wProtocolTimeout);
		h.putUnsignedShortInArray(ret, 38, wProtocolRetries);
		h.putUnsignedShortInArray(ret, 40, wCFTimeout);
		h.putUnsignedShortInArray(ret, 42, wMonBATDivider);
		h.putUnsignedShortInArray(ret, 44, wBATEmptyThreshold);
		h.putUnsignedShortInArray(ret, 46, wBATLowThreshold);
		h.putUnsignedShortInArray(ret, 48, wLDRThreshold);
		h.putUnsignedShortInArray(ret, 50, wElectrodeDistance);
		h.putUnsignedShortInArray(ret, 52, wReserved2);
		h.putUnsignedShortInArray(ret, 54, wReserved3);
		h.putUnsignedCharInArray(ret, 56, dst_beg_month);
		h.putUnsignedCharInArray(ret, 57, dst_beg_week);
		h.putUnsignedCharInArray(ret, 58, dst_beg_dayOfWeek);
		h.putUnsignedCharInArray(ret, 59, dst_beg_hour);
		h.putUnsignedCharInArray(ret, 60, dst_end_month);
		h.putUnsignedCharInArray(ret, 61, dst_end_week);
		h.putUnsignedCharInArray(ret, 62, dst_end_dayOfWeek);
		h.putUnsignedCharInArray(ret, 63, dst_end_hour);
		h.putSignedLongInArray(ret, 64, lTimeZoneJump);
		h.putUnsignedCharInArray(ret, 68, cAlertOn_hour);
		h.putUnsignedCharInArray(ret, 69, cAlertOn_minute);
		h.putUnsignedCharInArray(ret, 70, cAlertOff_hour);
		h.putUnsignedCharInArray(ret, 71, cAlertOff_minute);
		h.putUnsignedShortInArray(ret, 72, wAlertInterval_minutes);
		h.putUnsignedShortInArray(ret, 74, wAlertRanomization_minutes);
		h.putUnsignedLongInArray(ret, 76, nChannels);
		for (int i = 0; i < AmsDeviceConstants.NCHANNELS; i++)
			channels[i].putInArray(ret, 80 + i * 64);
		for (int i = 0; i < 84; i++)
			h.putUnsignedCharInArray(ret, 1426 + i, szReserved[i]);
		h.putUnsignedLongInArray(ret, 1508, dwFlushInterval);
		h.putUnsignedShortInArray(ret, 1512, wZ0inRangeIgnore);
		h.putUnsignedShortInArray(ret, 1514, wSCLinRangeIgnore);
		h.putUnsignedShortInArray(ret, 1516, wECGinRangeIgnore);
		h.putUnsignedShortInArray(ret, 1518, wIresInRangeIgnore);
		h.putUnsignedShortInArray(ret, 1520, wZ0ThresholdMin);
		h.putUnsignedShortInArray(ret, 1522, wZ0ThresholdMax);
		h.putUnsignedShortInArray(ret, 1524, wSCLThresholdMin);
		h.putUnsignedShortInArray(ret, 1526, wSCLThresholdMax);
		h.putUnsignedShortInArray(ret, 1528, wECGThresholdMin);
		h.putUnsignedShortInArray(ret, 1530, wECGThresholdMax);
		h.putUnsignedShortInArray(ret, 1532, wIresThresholdMin);
		h.putUnsignedShortInArray(ret, 1534, wIresThresholdMax);

		return ret;
	}
}
