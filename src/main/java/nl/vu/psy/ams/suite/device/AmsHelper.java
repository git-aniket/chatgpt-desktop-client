package nl.vu.psy.ams.suite.device;
/*
 * Helper function to translate java primitives to / from
 * byte arrays that the AMS device understands. Does byte order
 * conversion and signed <> unsigned conversion.
 */
public class AmsHelper {
	int GetSignedCharFromByte(int b1) {
		return b1 + Byte.MIN_VALUE;
	}

	int GetSignedLongFromArrayOffset(int r[], int off) {
		return GetSignedLongFromBytes(r[off], r[off + 1], r[off + 2], r[off + 3]);
	}

	int GetSignedLongFromBytes(int b1, int b2, int b3, int b4) {
		return ((b4 << 24) + (b3 << 16) + (b2 << 8) + b1);
	}

	int GetUnsignedCharFromByte(int b1) {
		return b1;
	}

	long GetUnsignedLongFromArrayOffset(int r[], int off) {
		return GetUnsignedLongFromBytes(r[off], r[off + 1], r[off + 2], r[off + 3]);
	}

	long GetUnsignedLongFromBytes(int b1, int b2, int b3, int b4) {
		return (((long) b4 << 24) + ((long) b3 << 16) + ((long) b2 << 8) + b1);
	}

	int GetUnsignedShortFromArrayOffset(int r[], int off) {
		return GetUnsignedShortFromBytes(r[off], r[off + 1]);
	}

	int GetUnsignedShortFromBytes(int b1, int b2) {
		return (b2 << 8) + b1;
	}

	public void putSignedLongInArray(int r[], int off, int val) {
		r[off] = (val & 0x000000FF);
		r[off + 1] = ((val & 0x0000FF00) >> 8);
		r[off + 2] = ((val & 0x00FF0000) >> 16);
		r[off + 3] = ((val & 0xFF000000) >> 24);
	}

	public void putUnsignedCharInArray(int[] r, int off, int val) {
		r[off] = val;
	}

	public void putUnsignedLongInArray(int r[], int off, long val) {
		r[off] = (int) (val & 0x00000000000000FF);
		r[off + 1] = (int) ((val & 0x000000000000FF00) >> 8);
		r[off + 2] = (int) ((val & 0x0000000000FF0000) >> 16);
		r[off + 3] = (int) ((val & 0x00000000FF000000) >> 24);
	}

	public void putUnsignedShortInArray(int r[], int off, int val) {
		r[off] = val & 0x000000FF;
		r[off + 1] = (val & 0x0000FF00) >> 8;
	}
}
