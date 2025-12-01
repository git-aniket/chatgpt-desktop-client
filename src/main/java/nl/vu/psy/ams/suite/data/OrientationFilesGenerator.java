package nl.vu.psy.ams.suite.data;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

// import javax.swing.ProgressMonitor;
import java.io.*;
import com.google.gson.Gson;

import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;

public class OrientationFilesGenerator extends Thread {

	private static OrientationFilesGenerator instance;

	public static OrientationFilesGenerator getInstance() {
		if (instance == null) {
			instance = new OrientationFilesGenerator();
		}
		return instance;
	}

	public static OrientationFilesGenerator getNewInstance() {
		if (instance != null) {
			// instance.interrupt();
			try {
				instance.join(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			instance = null;
		}
		instance = new OrientationFilesGenerator();
		return instance;
	}

	private OrientationFilesGenerator() {
		this.setPriority(MIN_PRIORITY);
	}

	public static boolean hasBeenGenerated = false, hasMagData = true;

	public static void clear() {
		hasBeenGenerated = false;
	}

	@Override
	public void run() {
		try {
			generateOrientationFiles();
		} catch (Exception e) {
			e.printStackTrace();
		}
		MainFrame.getInstance().getMainFrame().repaint();
	}

	public static void generateOrientationFiles() throws Exception {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		File yawFile = new File(cod.getFilePath(), "Yaw.dbin");
		File pitchFile = new File(cod.getFilePath(), "Pitch.dbin");
		File rollFile = new File(cod.getFilePath(), "Roll.dbin");

		if (hasBeenGenerated) {
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(yawFile);
			ssf1.start();
			SubsetFilesSingle ssf2 = new SubsetFilesSingle(pitchFile);
			ssf2.start();
			SubsetFilesSingle ssf3 = new SubsetFilesSingle(rollFile);
			ssf3.start();
			return;
		}
		File tempDir = cod.getFilePath();
		if (!cod.channelExists("MXR") || !cod.channelExists("MYR") || !cod.channelExists("MZR")
				|| !cod.channelExists("GyroX") || !cod.channelExists("GyroY") || !cod.channelExists("GyroZ"))
			return;
		if (!cod.channelExists("magX") || !cod.channelExists("magY") || !cod.channelExists("magZ"))
			hasMagData = false;
		hasBeenGenerated = true;

		// get channel info
		Ams7fsChannelInfo chanAccel = cod.getChannelInfoFromID("MXR");
		Ams7fsChannelInfo chanGyro = cod.getChannelInfoFromID("GyroX");
		Ams7fsChannelInfo chanMag = cod.getChannelInfoFromID("magX");
		// MadgwickFilter DeviceOrientation = new MadgwickFilter(1000);
		ComplementaryFilter DeviceOrientation = new ComplementaryFilter();

		Timer tim = new Timer();
		tim.start();
		// if (yawFile.exists() && pitchFile.exists() && rollFile.exists())
		// return;

		cod.dirtyFiles.add(yawFile);
		cod.dirtyFiles.add(pitchFile);
		cod.dirtyFiles.add(rollFile);
		int size = 1048576;
		ByteBuffer bbMX = ByteBuffer.allocateDirect(size);
		IntBuffer sbMX = bbMX.asIntBuffer();

		int[] sampMX = new int[size / 4];
		int[] sampMY = new int[size / 4];
		int[] sampMZ = new int[size / 4];
		int[] ticksG = new int[size / 4];
		int[] meanMotility = new int[size / 4]; // mean of motility
		double[] outBufY = new double[size / 4];
		double[] outBufP = new double[size / 4];
		double[] outBufR = new double[size / 4];
		// double[] vertHoriClassification = new double[size / 4];
		byte[] outbuf = new byte[2 * size];
		ByteBuffer outBB = ByteBuffer.wrap(outbuf);
		DoubleBuffer outDB = outBB.asDoubleBuffer();
		File xFile = new File(tempDir, "FILTmagX.bin");
		long fL = xFile.length();
		// ProgressMonitor progress = new
		// ProgressMonitor(MainFrame.getInstance().getMainFrame(), "Generating
		// orientation",
		// null, 0, (int) fL);
		try {
			RandomAccessFile isAX = new RandomAccessFile(new File(tempDir, "FILTMXR.bin"), "r");
			RandomAccessFile isAY = new RandomAccessFile(new File(tempDir, "FILTMYR.bin"), "r");
			RandomAccessFile isAZ = new RandomAccessFile(new File(tempDir, "FILTMZR.bin"), "r");
			RandomAccessFile isGX = new RandomAccessFile(new File(tempDir, "FILTGyroX.bin"), "r");
			RandomAccessFile isGY = new RandomAccessFile(new File(tempDir, "FILTGyroY.bin"), "r");
			RandomAccessFile isGZ = new RandomAccessFile(new File(tempDir, "FILTGyroZ.bin"), "r");
			RandomAccessFile iTicks = new RandomAccessFile(new File(tempDir, "TicksM.bin"), "r");
			FileInputStream fisMX = new FileInputStream(xFile);
			FileChannel ifMX = fisMX.getChannel();
			FileInputStream fisMY = new FileInputStream(new File(tempDir, "FILTmagY.bin"));
			FileChannel ifMY = fisMY.getChannel();
			FileInputStream fisMZ = new FileInputStream(new File(tempDir, "FILTmagZ.bin"));
			FileChannel ifMZ = fisMZ.getChannel();
			FileInputStream fisT = new FileInputStream(new File(tempDir, "TicksG.bin"));
			FileChannel ifT = fisT.getChannel();

			FileOutputStream fosY = new FileOutputStream(yawFile);
			FileChannel ofY = fosY.getChannel();
			FileOutputStream fosP = new FileOutputStream(pitchFile);
			FileChannel ofP = fosP.getChannel();
			FileOutputStream fosR = new FileOutputStream(rollFile);
			FileChannel ofR = fosR.getChannel();

			int nRead = 0;
			int timeDiff = 20; // 50 Hz
			int AX, AY, AZ, GX, GY, GZ;
			long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
			for (long i = 0; i < fL; i += nRead) {
				// progress.setProgress((int) i);
				bbMX.position(0);
				sbMX.position(0);
				nRead = ifMX.read(bbMX);
				int nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sbMX.get(sampMX, 0, nSRead);
				bbMX.position(0);
				sbMX.position(0);
				nRead = ifMY.read(bbMX);
				nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sbMX.get(sampMY, 0, nSRead);
				bbMX.position(0);
				sbMX.position(0);
				nRead = ifMZ.read(bbMX);
				nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sbMX.get(sampMZ, 0, nSRead);
				bbMX.position(0);
				sbMX.position(0);
				nRead = ifT.read(bbMX);
				nSRead = nRead / 4;
				if (nRead < 1)
					break;
				sbMX.get(ticksG, 0, nSRead);

				for (int s = 0; s < nSRead; s++) {
					long offset = correctForTicks(ticksG[s] - startTime, iTicks);
					if (4 * offset >= isAX.length() - 4)
						break;
					isAX.seek(4 * offset);
					AX = isAX.readInt();
					isAY.seek(4 * offset);
					AY = isAZ.readInt();
					isAZ.seek(4 * offset);
					AZ = isAZ.readInt();
					isGX.seek(4 * offset);
					GX = isGX.readInt();
					isGY.seek(4 * offset);
					GY = isGY.readInt();
					isGZ.seek(4 * offset);
					GZ = isGZ.readInt();
					if (s > 0)
						timeDiff = ticksG[s] - ticksG[s - 1];

					// Get device Orientation
					// after doing the getRealSlop, the accelerations are in g's, gyros are in deg/s
					// and magnetometer are in gauss
					// 1 gauss = 1e2 microtesla

					DeviceOrientation.update(
							chanGyro.getRealSlope() * GX * (Math.PI / 180.0),
							chanGyro.getRealSlope() * GY * (Math.PI / 180.0),
							chanGyro.getRealSlope() * -GZ * (Math.PI / 180.0),
							9.8 * chanAccel.getRealSlope() * AX,
							9.8 * chanAccel.getRealSlope() * AY,
							9.8 * chanAccel.getRealSlope() * AZ,
							chanMag.getRealSlope() * sampMY[s],
							chanMag.getRealSlope() * -sampMX[s],
							chanMag.getRealSlope() * sampMZ[s],
							timeDiff / 1000.0);

					outBufY[s] = DeviceOrientation.getYawDegrees();
					outBufP[s] = DeviceOrientation.getPitchDegrees();
					outBufR[s] = DeviceOrientation.getRollDegrees();
					meanMotility[s] = (int) (9.8 * chanAccel.getRealSlope()
							* (int) Math.sqrt(AX ^ 2 + AY ^ 2 + AZ ^ 2));

				}

				// isHorizontal(outBufP, ticksG, nSRead);
				// isStationary(meanMotility, ticksG, nSRead);
				// getGaitData(sampMX, ticksG, nSRead);

				int[] arr = new int[5];
				double[] arrd = new double[5];
				// fill the array with 1 to 5
				for (int m = 0; m < arr.length; ++m) {
					arr[m] = m + 1;
					arrd[m] = m + 1.1;
				}

				// String receivedData=pythonCaller(sampMX, sampMY, sampMZ);
				// //print the recievedData
				// System.out.println("Received data from Python script: "+receivedData);
				// sendDataToPythonServer(new Object[] {sampMX,sampMY,sampMZ});

				outBB.clear();
				outDB.clear();
				outDB.put(outBufY, 0, nSRead);
				outBB.limit(8 * nSRead);
				ofY.write(outBB);
				outBB.clear();
				outDB.clear();
				outDB.put(outBufP, 0, nSRead);
				outBB.limit(8 * nSRead);
				ofP.write(outBB);
				outBB.clear();
				outDB.clear();
				outDB.put(outBufR, 0, nSRead);
				outBB.limit(8 * nSRead);
				ofR.write(outBB);
			}
			isAX.close();
			isAY.close();
			isAZ.close();
			isGX.close();
			isGY.close();
			isGZ.close();
			iTicks.close();
			ofY.close();
			ifMX.close();
			fosY.close();
			fisMX.close();
			ofP.close();
			ifMY.close();
			fosP.close();
			fisMY.close();
			ofR.close();
			ifMZ.close();
			fosR.close();
			fisMZ.close();
			ifT.close();
			fisT.close();
			SubsetFilesSingle ssf1 = new SubsetFilesSingle(yawFile);
			ssf1.start();
			SubsetFilesSingle ssf2 = new SubsetFilesSingle(pitchFile);
			ssf2.start();
			SubsetFilesSingle ssf3 = new SubsetFilesSingle(rollFile);
			ssf3.start();
			// progress.close();
		} catch (IOException e) {
			e.printStackTrace();
			// progress.close();
		}

		tim.stop();
		if (AppSettings.getInstance().getIntProperty(Settings.DEBUG) == 1)
			System.out.println("generateOrientationtFiles took (" + tim.getTime() / 1000. + " sec)");
	}

	/**
	 * A function to analyze the orientation for horizontal/vertical by taking
	 * chunks of 5 sec data and shifting by 50 ms.
	 * It uses a sliding window approach to analyze chunks of data,
	 * and checks for specific conditions to identify the start and end of a
	 * horizontal orientation.
	 * 
	 * @param bufPitch array of pitch values
	 * @param nSRead   number of readings
	 * @return void
	 */
	@SuppressWarnings("unused")
	private static void isHorizontal(double bufPitch[], int ticksG[], int nSRead) {
		// Analyse Orientation for horizontal/vertical by taking chunks of 5 sec data
		final int analysisWindow = 5000; // 5 sec
		final int analysisWindowShift = 50; // 50 ms
		int indexHorizontalStart, indexHorizontalEnd;
		boolean isHorizontal;

		// check initial condition
		if (bufPitch[0] < 0 && bufPitch[(int) (analysisWindow / 2)] < 0 && bufPitch[analysisWindow] < 0)
			isHorizontal = true;
		else
			isHorizontal = false;

		// check throughout the Orientation buffer by moving across window
		for (int index = analysisWindow + 1; index < nSRead; index += analysisWindowShift) {
			if (isHorizontal) {
				// Find end of Horizontal
				if (bufPitch[index] > 0
						&& bufPitch[index - (int) (analysisWindow / 2)] < 0
						&& bufPitch[index - analysisWindow + 1] < 0) {
					indexHorizontalEnd = index - (int) (analysisWindow / 2);
					// System.out.printf("End of horizontal at %d\n", ticksG[indexHorizontalEnd]);
					isHorizontal = false;
				}
			} else if (!isHorizontal) {
				// Find for beginning of Horizontal
				if (bufPitch[index] < 0
						&& bufPitch[index - (int) (analysisWindow / 2)] > 0
						&& bufPitch[index - analysisWindow + 1] > 0) {
					indexHorizontalStart = index - (int) (analysisWindow / 2);
					// System.out.printf("Begin of horizontal at %d\n",
					// ticksG[indexHorizontalStart]);
					isHorizontal = true;
				}
			}
		}

	}

	/**
	 * A function to analyze the mean motility for stationary/moving by taking
	 * chunks of 2 sec data and shifting by 50 ms.
	 * It uses a sliding window approach to analyze chunks of data,
	 * and checks for specific conditions to identify the start and end of a
	 * horizontal orientation.
	 * 
	 * @param bufPitch array of pitch values
	 * @param nSRead   number of readings
	 * @return void
	 */
	@SuppressWarnings("unused")
	private static void isStationary(int meanMotility[], int ticksG[], int nSRead) {
		final int analysisWindowMotion = 2000; // 2 sec
		final int analysisWindowShift = 50; // 50 ms
		int indexMotionEnd, indexMotionStart;
		boolean isStationary;
		// Check if in motion
		// Check initial motion condition
		if (meanMotility[0] > 0 && meanMotility[(int) (analysisWindowMotion / 2)] > 0
				&& meanMotility[analysisWindowMotion] > 0)
			isStationary = false;
		else
			isStationary = true;

		// check throughout the Orientation buffer by moving across
		for (int index = analysisWindowMotion + 1; index < nSRead; index += analysisWindowShift) {
			// if beginning from stationary position
			if (isStationary) {
				// Find end of stationary by comparing begin middle and end of analysis window
				if (meanMotility[index] < 0
						&& meanMotility[index - (int) (analysisWindowMotion / 2)] > 0
						&& meanMotility[index - analysisWindowMotion + 1] > 0) {
					indexMotionEnd = index - (int) (analysisWindowMotion / 2);
					// System.out.printf("End of stationary at %d\n", indexMotionEnd);
					isStationary = false;
				}
			}

			else if (!isStationary) {
				// Find for beginning of stationary
				if (meanMotility[index] > 0
						&& meanMotility[index - (int) (analysisWindowMotion / 2)] < 0
						&& meanMotility[index - analysisWindowMotion + 1] < 0) {
					indexMotionStart = index - (int) (analysisWindowMotion / 2);
					// System.out.printf("Begin of stationary at %d\n", indexMotionStart);
					isStationary = true;
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static void getGaitData(int bufMX[], int ticksG[], int nSRead) {
		int peakCount = 0;

		// check for peaks in the filtered signal
		for (int i = 2; i < nSRead; i++) {
			if (bufMX[i] < bufMX[i - 1] && bufMX[i - 1] > bufMX[i - 2]) {
				// System.out.printf("Peak at %d\n", ticksG[i]);
				++peakCount;
			}
		}
		// System.out.printf("Found %d peaks\n", peakCount);

	}

	private static long correctForTicks(long offset, RandomAccessFile is) {
		long tick, diff, oldOffset = offset;
		long startTime = CurrentOpenData.getInstance().getStarts().get(0).getDwClockTick_ms();
		try {
			long len = is.length() / 4;
			if (offset > len - 1)
				offset = len - 1;
			if (offset < 0)
				offset = 0;
			is.seek(4 * offset);
			tick = is.readInt() - startTime;
			diff = tick - offset;
			offset -= diff;
			long diff2 = tick - oldOffset;
			// if (Math.abs(diff) < 10000) {
			int loopCount = 0;
			while (Math.abs(diff2) > 1 && loopCount < 1000) {
				if (offset < 0 || 4 * offset >= len * 4) {
					break;
				}
				is.seek(4 * offset);
				tick = is.readInt() - startTime;
				diff2 = tick - oldOffset;
				offset -= diff2;
				loopCount++;
			}
			if (loopCount > 2)
				System.out.println("correct ticks orientation: " + loopCount + " " + diff);
			// } else {
			// // binary search
			// long low = 0, high = len - 1, mid = 0;
			// while (low <= high) {
			// mid = low + (high - low) / 2;
			// is.seek(4 * mid);
			// tick = (is.readInt() - startTime);

			// if (tick == oldOffset) {
			// offset = mid;
			// break;
			// } else if (tick < oldOffset)
			// low = mid + 1;

			// else
			// high = mid - 1;
			// }
			// if (tick != oldOffset) { // not found
			// if (low < 0)
			// low = 0;
			// if (low > len - 1)
			// low = len - 1;
			// is.seek(4 * low);
			// long tick1 = (is.readInt() - startTime);
			// if (high < 0)
			// high = 0;
			// if (high > len - 1)
			// high = len - 1;
			// is.seek(4 * high);
			// long tick2 = (is.readInt() - startTime);
			// if (oldOffset - tick1 < tick2 - oldOffset)
			// offset = low;
			// if (offset >= len * 4)
			// offset = high;
			// }
			// System.out.println("binary search orientation: " + diff);
			// }
			if (offset < 0)
				offset = 0;
			if (offset > len * 4 - 1)
				offset = len * 4 - 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return offset;
	}

	public static void sendDataToPythonServer(Object[] arrays) {
		String hostName = "127.0.0.1"; // IP address of the Python server
		int portNumber = 1234; // Port number that the Python server is listening on

		try (
				Socket socket = new Socket(hostName, portNumber);
				OutputStream outputStream = socket.getOutputStream();
				PrintWriter out = new PrintWriter(outputStream, true);) {
			// Serialize arrays to JSON and send as byte stream
			Gson gson = new Gson();
			String json = gson.toJson(arrays);
			byte[] bytes = json.getBytes("UTF-8");
			outputStream.write(bytes);
			outputStream.flush(); // Ensure all data is sent
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + hostName);
			e.printStackTrace();
		}
	}

	public static String pythonCaller(int[] sampMX, int[] sampMY, int[] sampMZ) {
		try {
			// Command to run the Python script
			String[] command = { "python3", "/tools/pythonServer.py" };

			// Create ProcessBuilder object with the command
			ProcessBuilder pb = new ProcessBuilder(command);

			// Start the process
			Process process = pb.start();
			// Wait for the Python server to start (optional)
			Thread.sleep(4000); // Adjust the sleep time as needed

			// Create an array to hold the input arrays
			Object[] arrays = new Object[] { sampMX, sampMY, sampMZ };

			// Pass the arrays as an object to the sendDataToPythonServer function
			sendDataToPythonServer(arrays);

			// Read output from the Python script
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder receivedData = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				receivedData.append(line);
			}

			// Wait for the process to finish
			process.waitFor();

			// Print exit value
			System.out.println("Python script exited with code " + process.exitValue());

			// Return received predictions
			return receivedData.toString();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.err.println("Couldn't get I/O for the connection to host");
			return null;
		}
	}

}
