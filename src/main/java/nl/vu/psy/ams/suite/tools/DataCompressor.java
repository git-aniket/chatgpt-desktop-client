package nl.vu.psy.ams.suite.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/*
 * Compresses a data signal by storing only the difference of the current value
 * with the previous value, and use a form of run length encoding afterwards.
 * The difference between successive values is usually very small, so they can be
 * put in less bits than the original signal.
 * The resulting stream works like this: first, a 5-bit number is given that indicates how many 
 * bits the following values will be encoded in (N_b). Then, a sequence (of any possible length) of signal values is given,
 * using N_b bits per value. These values are simply encoded with 1 sign bit, and an unsigned value afterwards,
 * with one exception: if all N_b bits are 1, it signals that the value for N_b will be changed.
 * Therefore, if a value with all N_b bits 1 is found, that value should not be used as part of the signal, but
 * should be discarded. Then the next 5 bits should be used as the new value for N_b, and the algorithm can continue.
 * 
 * The decoder is fairly easy to implement, and the runlength encoder is very simple as well: it tries to fit the input
 * signal in as little bits as possible. The encoder can probably be improved a bit by choosing when to switch N_b in
 * a smarter way. 
 */
public class DataCompressor {

	private ArrayList<Integer> diffs = new ArrayList<Integer>();
	private int curNBits = -1;
	int maxNBits = -1;
	private int p;
	private int bitsInInt;

	private FileChannel fcOut;
	private FileOutputStream fos;
	private long temp;
	final int SIZE = 10485760;
	private int[] intBuf = new int[SIZE / 4];
	private int[] buffer = new int[SIZE / 4];
	private ByteBuffer bbIn = ByteBuffer.allocateDirect(SIZE);
	private ShortBuffer sbIn = bbIn.asShortBuffer();
	private IntBuffer ibIn = bbIn.asIntBuffer();
	private ByteBuffer bbOut = ByteBuffer.allocateDirect(SIZE);
	private IntBuffer ibOut = bbOut.asIntBuffer();
	private FileChannel fcIn;
	private FileInputStream fis;
	private long totalRead;
	private int nRead;
	private long fs;
	private int nIntsRead;

	private void addBitsFromIntegerToBuffer(int val, int nBits) throws IOException {
		int absVal = Math.abs(val);
		for (int i = 1; i <= nBits; i++) {
			if ((absVal & (1 << (nBits - i))) > 0) {
				addBitToBuffer();
			} else {
			}
			incrementBit();
		}
	}

	private void addBitToBuffer() {
		intBuf[p] |= 1 << (31 - bitsInInt);
	}

	private void clearIntBuffer() {
		for (int i = 0; i < SIZE / 4; i++)
			intBuf[i] = 0;
		p = 0;
	}

	private int getFirstValue(boolean is7fs) throws IOException {
		int prevVal;
		if (is7fs) {
			bbIn.limit(4);
			fcIn.read(bbIn);
			prevVal = ibIn.get();
			bbIn.rewind();
		} else {
			sbIn.clear();
			bbIn.limit(4);
			fcIn.read(bbIn);
			prevVal = ibIn.get();
			bbIn.rewind();
			sbIn.put((short) prevVal);
			bbIn.rewind();
			bbIn.limit(2);
		}
		return prevVal;
	}

	public void compress(File inFile, File outFile, boolean is7fs) { // to convert integer bin files from old amsdata
																		// files back to .comp files
		fcIn = null;
		fcOut = null;
		fis = null;
		fos = null;

		try {
			fis = new FileInputStream(inFile);
			fos = new FileOutputStream(outFile);
			fcIn = fis.getChannel();
			fcOut = fos.getChannel();
			long fs = inFile.length();
			bbIn.clear();
			ibIn.clear();
			int prevVal = getFirstValue(is7fs);
			fcOut.write(bbIn);
			int curVal;
			curNBits = -1;
			int diffVal;
			maxNBits = -1;
			diffs.clear();
			int nRead = 0;

			bitsInInt = 0;
			clearIntBuffer();
			for (long i = 4; i < fs; i += 4 * nRead) {
				bbIn.clear();
				ibIn.clear();
				nRead = fcIn.read(bbIn);
				nRead /= 4;
				if (nRead < 1)
					break;
				ibIn.get(buffer, 0, nRead);
				for (int q = 0; q < nRead; q++) {
					curVal = buffer[q];
					diffVal = curVal - prevVal;
					int nBits = getNBits(diffVal);
					if (nBits > curNBits) {
						flushDiffs();
						for (int j = 0; j <= curNBits; j++) {
							addBitToBuffer();
							incrementBit();
						}
						addBitsFromIntegerToBuffer(nBits, 5);
						curNBits = nBits;
					}
					diffs.add(diffVal);
					if (nBits > maxNBits)
						maxNBits = nBits;
					if (nBits == curNBits) {
						flushDiffs();
					} else {
						int nBitsThatCanBeSaved = (diffs.size()) * (curNBits - maxNBits);
						if (nBitsThatCanBeSaved > (curNBits + maxNBits + 12)) {
							for (int j = 0; j <= curNBits; j++) {
								addBitToBuffer();
								incrementBit();
							}
							addBitsFromIntegerToBuffer(maxNBits, 5);
							curNBits = maxNBits;
							flushDiffs();
						}
					}

					prevVal = curVal;
					flushIntBufferIfNeeded();
				}
			}

			flushDiffs();

			while (bitsInInt > 0) {
				addBitToBuffer();
				incrementBit();
			}
			if (p > 0) {
				ibOut.clear();
				bbOut.clear();
				ibOut.put(intBuf, 0, p);
				bbOut.limit(4 * p);
				fcOut.write(bbOut);
				p = 0;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fcIn != null) {
				try {
					fcIn.close();
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fcOut != null) {
				try {
					fcOut.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int readFirstValue(boolean is7fs) throws IOException {
		int curVal;
		if (is7fs) {
			ibIn.clear();
			bbIn.limit(4);
			fcIn.read(bbIn);
			curVal = ibIn.get();
			totalRead = 4;
		} else {
			sbIn.clear();
			bbIn.limit(2);
			fcIn.read(bbIn);
			curVal = sbIn.get();
			totalRead = 2;
		}
		return curVal;
	}

	public void decompress(File inFile, File outFile, boolean is7fs) { // to convert .comp files from old amsdata files
																		// to integer bin files
		int curVal;
		int nBits = -1;
		fcIn = null;
		fcOut = null;
		fis = null;
		fos = null;
		try {
			fis = new FileInputStream(inFile);
			fos = new FileOutputStream(outFile);
			fcIn = fis.getChannel();
			fcOut = fos.getChannel();
			bbIn.clear();
			curVal = readFirstValue(is7fs);
			fs = inFile.length();
			int nOut = 0;
			buffer[0] = curVal;
			nOut++;
			bbIn.clear();
			ibIn.clear();
			nRead = fcIn.read(bbIn);
			totalRead += nRead;
			nIntsRead = nRead / 4;
			ibIn.get(intBuf, 0, nIntsRead);
			p = 0;
			bitsInInt = 0;

			while (totalRead - nRead < fs) {
				if (nBits == -1) {
					if (totalRead == fs && p == nIntsRead - 1 && bitsInInt > 27)
						break;
					nBits = getIntFromIntBuffer(5);
				} else {
					if (totalRead == fs && p == nIntsRead - 1 && bitsInInt > (32 - nBits - 1))
						break;
					int sign = getBitFromBuffer();
					int diffVal = getIntFromIntBuffer(nBits);
					if (sign == 0) {
						curVal += diffVal;
						buffer[nOut] = curVal;
						nOut++;
					} else {
						if (diffVal == ((1 << nBits) - 1)) {
							nBits = -1;
						} else {
							curVal -= diffVal;
							buffer[nOut] = curVal;
							nOut++;
						}
					}
				}
				if (nOut == SIZE / 4) {
					bbOut.clear();
					ibOut.clear();
					ibOut.put(buffer);
					fcOut.write(bbOut);
					nOut = 0;
				}
			}
			if (nOut > 0) {
				bbOut.clear();
				ibOut.clear();
				ibOut.put(buffer, 0, nOut);
				bbOut.limit(4 * nOut);
				fcOut.write(bbOut);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fcIn != null) {
				try {
					fcIn.close();
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fcOut != null) {
				try {
					fcOut.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void flushDiffs() throws IOException {
		for (int j = 0; j < diffs.size(); j++) {
			if (diffs.get(j) < 0) {
				addBitToBuffer();
			}
			incrementBit();
			flushIntBufferIfNeeded();
			addBitsFromIntegerToBuffer(diffs.get(j), curNBits);
		}
		diffs.clear();
		maxNBits = -1;
	}

	private void flushIntBufferIfNeeded() throws IOException {
		if (p == SIZE / 4 - 1) {
			ibOut.clear();
			bbOut.clear();
			ibOut.put(intBuf, 0, p);
			bbOut.limit(4 * p);
			fcOut.write(bbOut);
			temp = intBuf[p];
			clearIntBuffer();
			intBuf[0] = (int) temp;
		}
	}

	private int getBitFromBuffer() throws IOException {
		if (((intBuf[p] & (1 << (31 - bitsInInt))) >> (31 - bitsInInt)) != 0) {
			bitsInInt++;
			if (bitsInInt == 32) {
				bitsInInt = 0;
				p++;

				if (p == nIntsRead) {
					if (totalRead == fs) {
						nRead = -1;
					} else {
						bbIn.clear();
						ibIn.clear();
						nRead = fcIn.read(bbIn);
						totalRead += nRead;
						nIntsRead = nRead / 4;
						ibIn.get(intBuf, 0, nIntsRead);
						p = 0;
					}
				}

			}
			return 1;
		} else {
			bitsInInt++;
			if (bitsInInt == 32) {
				bitsInInt = 0;
				p++;
				if (p == nIntsRead) {
					if (totalRead == fs) {
						nRead = -1;
					} else {
						bbIn.clear();
						ibIn.clear();
						nRead = fcIn.read(bbIn);
						totalRead += nRead;
						nIntsRead = nRead / 4;
						ibIn.get(intBuf, 0, nIntsRead);
						p = 0;
					}
				}
			}
			return 0;
		}
	}

	private int getIntFromIntBuffer(int nBits) throws IOException {
		int ret = 0;
		for (int i = 1; i <= nBits; i++) {
			ret += (getBitFromBuffer() << (nBits - i));
		}
		return ret;
	}

	private int getNBits(int diffVal) {
		if (diffVal == 0)
			return 0;
		int absVal = Math.abs(diffVal);
		int n = 1;
		int rng = (1 << (n)) - 1;
		while (rng < absVal) {
			n++;
			rng = (1 << (n)) - 1;
		}
		if (diffVal < 0 && rng == absVal)
			n++;
		return n;
	}

	private void incrementBit() throws IOException {
		bitsInInt++;
		if (bitsInInt == 32) {
			bitsInInt = 0;
			p++;
			flushIntBufferIfNeeded();
		}
	}

}
