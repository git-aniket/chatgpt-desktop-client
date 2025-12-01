package nl.vu.psy.ams.suite.device;

import java.util.Arrays;
/*
 * Channel info class from the AMS device.
 * (Can probably be merged with the Ams5fsChannelInfo class,
 * with a little bit of work)
 */
public class AmsChannelInfo {
	public static final int	CHANNEL_ID_SIZE	= 8;
	public static final int	UNIT_ID_SIZE	= 8;
	int						wTag;
	int						wSize;
	int						szID[];
	long					dwDivider;
	long					nBits;
	int						lMinValue;
	int						lMaxValue;
	int						lMinMaxDivider;
	int						szUnit[];
	int						dwChannelTag;
	int						wBufSize;
	int						bStore;
	int						szReserved[];

	AmsChannelInfo() {
		szID = new int[AmsChannelInfo.CHANNEL_ID_SIZE];
		szUnit = new int[AmsChannelInfo.UNIT_ID_SIZE];
		szReserved = new int[16];
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AmsChannelInfo other = (AmsChannelInfo) obj;
		if (bStore != other.bStore)
			return false;
		if (dwChannelTag != other.dwChannelTag)
			return false;
		if (dwDivider != other.dwDivider)
			return false;
		if (lMaxValue != other.lMaxValue)
			return false;
		if (lMinMaxDivider != other.lMinMaxDivider)
			return false;
		if (lMinValue != other.lMinValue)
			return false;
		if (nBits != other.nBits)
			return false;
		if (!Arrays.equals(szID, other.szID))
			return false;
		if (!Arrays.equals(szReserved, other.szReserved))
			return false;
		if (!Arrays.equals(szUnit, other.szUnit))
			return false;
		if (wBufSize != other.wBufSize)
			return false;
		if (wSize != other.wSize)
			return false;
		if (wTag != other.wTag)
			return false;
		return true;
	}

	public long getDivider() {
		return dwDivider;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bStore;
		result = prime * result + dwChannelTag;
		result = prime * result + (int) (dwDivider ^ (dwDivider >>> 32));
		result = prime * result + lMaxValue;
		result = prime * result + lMinMaxDivider;
		result = prime * result + lMinValue;
		result = prime * result + (int) (nBits ^ (nBits >>> 32));
		result = prime * result + Arrays.hashCode(szID);
		result = prime * result + Arrays.hashCode(szReserved);
		result = prime * result + Arrays.hashCode(szUnit);
		result = prime * result + wBufSize;
		result = prime * result + wSize;
		result = prime * result + wTag;
		return result;
	}

	void putInArray(int r[], int off) {
		AmsHelper h = new AmsHelper();
		h.putUnsignedShortInArray(r, off, wTag);
		h.putUnsignedShortInArray(r, off + 2, wSize);
		for (int i = 0; i < AmsChannelInfo.CHANNEL_ID_SIZE; i++)
			h.putUnsignedCharInArray(r, off + 4 + i, szID[i]);
		h.putUnsignedLongInArray(r, off + 12, dwDivider);
		h.putUnsignedLongInArray(r, off + 16, nBits);
		h.putSignedLongInArray(r, off + 20, lMinValue);
		h.putSignedLongInArray(r, off + 24, lMaxValue);
		h.putSignedLongInArray(r, off + 28, lMinMaxDivider);
		for (int i = 0; i < AmsChannelInfo.UNIT_ID_SIZE; i++)
			h.putUnsignedCharInArray(r, off + 32 + i, szUnit[i]);
		h.putUnsignedShortInArray(r, off + 40, dwChannelTag);
		h.putUnsignedShortInArray(r, off + 42, wBufSize);
		h.putUnsignedShortInArray(r, off + 44, bStore);
		for (int i = 0; i < 16; i++)
			h.putUnsignedCharInArray(r, off + 48 + i, szReserved[i]);
	}

	void ReadFromArray(int r[], int off) {
		AmsHelper h = new AmsHelper();
		wTag = h.GetUnsignedShortFromArrayOffset(r, off);
		wSize = h.GetUnsignedShortFromArrayOffset(r, off + 2);
		for (int i = 0; i < AmsChannelInfo.CHANNEL_ID_SIZE; i++)
			szID[i] = h.GetUnsignedCharFromByte(r[off + 4 + i]);
		dwDivider = h.GetUnsignedLongFromArrayOffset(r, off + 12);
		nBits = h.GetUnsignedLongFromArrayOffset(r, off + 16);
		lMinValue = h.GetSignedLongFromArrayOffset(r, off + 20);
		lMaxValue = h.GetSignedLongFromArrayOffset(r, off + 24);
		lMinMaxDivider = h.GetSignedLongFromArrayOffset(r, off + 28);
		for (int i = 0; i < AmsChannelInfo.UNIT_ID_SIZE; i++)
			szUnit[i] = h.GetUnsignedCharFromByte(r[off + 32 + i]);
		dwChannelTag = h.GetUnsignedShortFromArrayOffset(r, off + 40);
		wBufSize = h.GetUnsignedShortFromArrayOffset(r, off + 42);
		bStore = h.GetUnsignedShortFromArrayOffset(r, off + 44);
		for (int i = 0; i < 16; i++)
			szReserved[i] = h.GetUnsignedCharFromByte(r[off + 48 + i]);
	}
}
