package nl.vu.psy.ams.suite.device;

import java.util.Arrays;
/*
 * Statics structure from the AMS device.
 * (Can probably be merged with the one from 5fs file,
 * with a little bit of work)
 */
public class AmsStatics {
	int		wTag;						// 00 - type tag to identify Statics
										// structure
	int		wSize;						// 02 - total nr of bytes of this
										// structure

	int		wStructVersion;			// 04
	int		reservedOldSize;			// 06 - was a redundant wSize in files
										// made with software version 1
	long	dwDeviceID;				// 08
	long	dwBaudrate;				// 0C
	int		wSerialnr;					// 10
										// unsigned long wSoftwareVersion; //Is
										// also a static parameter but may not
										// be changed by PC software (only by
										// JTAG)
	int		wHardwareVersion;			// 12 see AMS_DEVICE_SOFTWAREVERSION
	int		wCalSCLdcLevel;			// 14
	int		wCalibratieB;				// 16
	int		wCalibratieC;				// 18
	int		wCalibratieD;				// 1A

	int[]	reservedA	= new int[12];	// 1C
	long	dwChannelDisableMask;		// 28 Channel disable mask (0 = enable
										// all channels)
	int		cSclMode;					// 2C one of AMSII_SCL_MODE
	int[]	reservedB	= new int[3];	// 2D
	// end // 30 - == sizeof(AMSII_DeviceStatics)

	public AmsStatics deepCopy() {
		AmsStatics ns = new AmsStatics();
		ns.wTag = wTag;
		ns.wSize = wSize;
		ns.wStructVersion = wStructVersion;
		ns.reservedOldSize = reservedOldSize;
		ns.dwDeviceID = dwDeviceID;
		ns.dwBaudrate = dwBaudrate;
		ns.wSerialnr = wSerialnr;
		ns.wHardwareVersion = wHardwareVersion;
		ns.wCalSCLdcLevel = wCalSCLdcLevel;
		ns.wCalibratieB = wCalibratieB;
		ns.wCalibratieC = wCalibratieC;
		ns.wCalibratieD = wCalibratieD;
		ns.reservedA = new int[reservedA.length];
		for (int i = 0; i < reservedA.length; i++)
			ns.reservedA[i] = reservedA[i];
		ns.dwChannelDisableMask = dwChannelDisableMask;
		ns.cSclMode = cSclMode;
		ns.reservedB = new int[reservedB.length];
		for (int i = 0; i < reservedB.length; i++)
			ns.reservedB[i] = reservedB[i];
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
		AmsStatics other = (AmsStatics) obj;
		if (cSclMode != other.cSclMode)
			return false;
		if (dwBaudrate != other.dwBaudrate)
			return false;
		if (dwChannelDisableMask != other.dwChannelDisableMask)
			return false;
		if (dwDeviceID != other.dwDeviceID)
			return false;
		if (!Arrays.equals(reservedA, other.reservedA))
			return false;
		if (!Arrays.equals(reservedB, other.reservedB))
			return false;
		if (reservedOldSize != other.reservedOldSize)
			return false;
		if (wCalSCLdcLevel != other.wCalSCLdcLevel)
			return false;
		if (wCalibratieB != other.wCalibratieB)
			return false;
		if (wCalibratieC != other.wCalibratieC)
			return false;
		if (wCalibratieD != other.wCalibratieD)
			return false;
		if (wHardwareVersion != other.wHardwareVersion)
			return false;
		if (wSerialnr != other.wSerialnr)
			return false;
		if (wSize != other.wSize)
			return false;
		if (wStructVersion != other.wStructVersion)
			return false;
		if (wTag != other.wTag)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cSclMode;
		result = prime * result + (int) (dwBaudrate ^ (dwBaudrate >>> 32));
		result = prime * result + (int) (dwChannelDisableMask ^ (dwChannelDisableMask >>> 32));
		result = prime * result + (int) (dwDeviceID ^ (dwDeviceID >>> 32));
		result = prime * result + Arrays.hashCode(reservedA);
		result = prime * result + Arrays.hashCode(reservedB);
		result = prime * result + reservedOldSize;
		result = prime * result + wCalSCLdcLevel;
		result = prime * result + wCalibratieB;
		result = prime * result + wCalibratieC;
		result = prime * result + wCalibratieD;
		result = prime * result + wHardwareVersion;
		result = prime * result + wSerialnr;
		result = prime * result + wSize;
		result = prime * result + wStructVersion;
		result = prime * result + wTag;
		return result;
	}

	public int[] toDeviceArray() {
		int[] ret = new int[48];
		AmsHelper h = new AmsHelper();
		h.putUnsignedShortInArray(ret, 0, wTag);
		h.putUnsignedShortInArray(ret, 2, wSize);
		h.putUnsignedShortInArray(ret, 4, wStructVersion);
		h.putUnsignedShortInArray(ret, 6, reservedOldSize);
		h.putUnsignedLongInArray(ret, 8, dwDeviceID);
		h.putUnsignedLongInArray(ret, 12, dwBaudrate);
		h.putUnsignedShortInArray(ret, 16, wSerialnr);
		h.putUnsignedShortInArray(ret, 18, wHardwareVersion);
		h.putUnsignedShortInArray(ret, 20, wCalSCLdcLevel);
		h.putUnsignedShortInArray(ret, 22, wCalibratieB);
		h.putUnsignedShortInArray(ret, 24, wCalibratieC);
		h.putUnsignedShortInArray(ret, 26, wCalibratieD);
		for (int i = 0; i < reservedA.length; i++)
			h.putUnsignedCharInArray(ret, 28 + i, reservedA[i]);
		h.putUnsignedLongInArray(ret, 40, dwChannelDisableMask);
		h.putUnsignedCharInArray(ret, 44, cSclMode);
		for (int i = 0; i < reservedB.length; i++)
			h.putUnsignedCharInArray(ret, 45 + i, reservedB[i]);
		return ret;
	}

}
