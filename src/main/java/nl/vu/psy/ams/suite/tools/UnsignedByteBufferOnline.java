package nl.vu.psy.ams.suite.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/*
 * Used to help decoding online data form ams7. Changes byteorder and converts
 * unsigned -> signed values.
 */
public class UnsignedByteBufferOnline {
	private ByteBuffer		byteBuffer;

	public UnsignedByteBufferOnline() {
		byteBuffer = ByteBuffer.allocateDirect(1000000);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	public int remaining() {
		return byteBuffer.remaining();
	}

	public byte getByte() {
		return byteBuffer.get();
	}

	public int getInt() {
		return byteBuffer.getInt();
	}

	public int getPosition() {
		return byteBuffer.position();
	}

	public int getShort() {
		return byteBuffer.getShort();
	}

	public String getString(int length) {
		String returnString = new String();
		char curChar;
		boolean done = false;
		for (int i = 0; i < length; i++) {
			curChar = (char) getByte();
			if (curChar != 0 && done == false) {
				returnString += curChar;
			} else {
				done = true;
			}
		}
		return returnString;
	}

	public int getUByte() {
		return (int) (byteBuffer.get() & 0xFF);
	}

	public long getUInt() {
		return ((long) byteBuffer.getInt() & 0xFFFFFFFF);
	}

	public int getUShort() {
		return (byteBuffer.getShort() & 0xFFFF);
	}

	public void setBytes(byte b[]) {
		byteBuffer.clear();
		byteBuffer.put(b);
		byteBuffer.position(0);
		byteBuffer.limit(b.length);
	}

	public void skip(int nBytes) {
		int curPosition = getPosition();
		byteBuffer.position(curPosition + nBytes);
	}

}
