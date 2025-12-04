package nl.vu.psy.ams.suite.device7;

import java.io.IOException;
//import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
// import com.mathworks.toolbox.javabuilder.MWException;
// import com.mathworks.toolbox.javabuilder.MWMatrixRef;
// import com.mathworks.toolbox.javabuilder.MWNumericArray;

import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.data.files.Ams7fsFile.ChannelSet;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsPacket;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.tools.UnsignedByteBufferOnline;

public class AmsDevice7 {
	private final UnsignedByteBufferOnline ubb = new UnsignedByteBufferOnline();
	private final int[] bufferData = new int[64];
	private final int[] Abuffer = new int[10];// ADC
	private final int[] Bbuffer = new int[2];// Battery
	private final int[] Dbuffer = new int[3];// Druk(Pressure+temp)
	private final int[] Mbuffer = new int[9];// Motility
	private final int[] Gbuffer = new int[5];// Magnetometer
	private byte[] buffer = new byte[1500], /* buffer_old = new byte[1500], */ bigBlock;
	// private DatagramPacket response = new DatagramPacket(buffer, buffer.length);

	private int prevTimeA = 0, prevTimeM = 0, bytesLeft = 0;
	private long prevTstamp = 0;
	public List<List<String>> recordedChannels;
	ArrayList<Ams7fsChannelInfo> channelInfo;
	ArrayList<Ams5fsPacket> events;
	boolean conn = true, Bfound = false, Dfound = false, isBLE;
	DatagramSocket socket = null;
	MyWebSocketClient webSocketClient = null;
	DeviceDialog7 devDialog = null;
	DeviceDialogBLE bleDialog = null;
	public List<List<Integer>> listOfListsA = new ArrayList<>(),
			listOfListsB = new ArrayList<>(),
			listOfListsD = new ArrayList<>(),
			listOfListsM = new ArrayList<>(),
			listOfListsG = new ArrayList<>();
	// remove matlab dependency
	// bleMATLABClass.Class1 obj;
	// MWMatrixRef a;
	// MWNumericArray streamData;

	public AmsDevice7(boolean isBLE) {
		setDefaultChannels(true);
		events = new ArrayList<Ams5fsPacket>();
		this.isBLE = isBLE;
	}

	public void closeConnection() {
		conn = false;
	}

	public void setDevDialog(DeviceDialog7 diag) {
		this.devDialog = diag;
	}

	public void setBLEDialog(DeviceDialogBLE diag) {
		this.bleDialog = diag;
	}

	public void setDefaultChannels(boolean addChan) {
		recordedChannels = new ArrayList<List<String>>();
		List<String> subList = new ArrayList<String>();
		channelInfo = new ArrayList<Ams7fsChannelInfo>();
		Ams7fsChannelInfo chan;
		if (addChan) {
			chan = new Ams7fsChannelInfo();
			chan.setSzID("ECG");
			chan.setSzUnit("V");
			chan.setnBits(32);
			chan.setRealConstant(0);
			chan.setRealSlope(4.7683723096270114e-05);
			subList.add(chan.getSzID());
			channelInfo.add(chan);
		}
		chan = new Ams7fsChannelInfo();
		if (addChan)
			chan.setSzID("V2ecg"); // Vecg
		else
			chan.setSzID("ECG"); // Vecg
		chan.setSzUnit("V");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(4.7683723096270114e-05);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		if (addChan) {
			chan = new Ams7fsChannelInfo();
			chan.setSzID("V3ecg");
			chan.setSzUnit("V");
			chan.setnBits(32);
			chan.setRealConstant(0);
			chan.setRealSlope(4.7683723096270114e-05);
			subList.add(chan.getSzID());
			channelInfo.add(chan);
		}
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Z0"); // Vicg
		chan.setSzUnit("V");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(4.76837194582913e-05);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("DZDT"); // Vicg
		chan.setSzUnit("\u2126/s");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(1);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Visrc");
		chan.setSzUnit("V");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(4.7683723096270114e-05);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("SCL");
		chan.setSzUnit("\u00B5S");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(1); // (45.4805564880371);
		chan.setFormula("(a0+SCL*a1)/a2");
		Map<String, Double> c = new HashMap<String, Double>(3);
		c.put("a0", 0.5);
		c.put("a1", 0.0002861);
		c.put("a2", 1000.0 / (0.5 * 22));
		chan.setConstants(c);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("T");
		chan.setSzUnit("\u00B0C");
		chan.setnBits(32);
		chan.setRealConstant(-271.53060913085938);
		chan.setRealSlope(0.00058388232719153166);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		recordedChannels.add(subList);
		subList = new ArrayList<String>();

		chan = new Ams7fsChannelInfo();
		chan.setSzID("BAT"); // Vbat
		chan.setSzUnit("mV");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(1.98);
		chan.setDwDivider(1000);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		recordedChannels.add(subList);
		subList = new ArrayList<String>();

		chan = new Ams7fsChannelInfo();
		chan.setSzID("P_sc"); //
		chan.setSzUnit("Pa");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(1d / 253952);
		chan.setDwDivider(200);
		chan.setFormula("c00+P_sc*(c10+P_sc*(c20+P_sc*c30))+T_sc*c01+T_sc*P_sc*(c11+P_sc*c21)");
		c = new HashMap<String, Double>(7);
		c.put("c00", (double) 80715);
		c.put("c10", (double) -51786);
		c.put("c20", (double) -9062);
		c.put("c30", (double) -1195);
		c.put("c01", (double) -3956);
		c.put("c11", (double) 1287);
		c.put("c21", (double) 15);
		chan.setConstants(c);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("T_sc"); //
		chan.setSzUnit("\u00B0C");
		chan.setnBits(32);
		chan.setRealConstant(0);
		chan.setRealSlope(1d / 524288);
		chan.setDwDivider(200);
		chan.setFormula("c0*0.5+c1*T_sc");
		Map<String, Double> ct = new HashMap<String, Double>(2);
		ct.put("c0", (double) 221);
		ct.put("c1", (double) -296);
		chan.setConstants(ct);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		recordedChannels.add(subList);
		subList = new ArrayList<String>();

		chan = new Ams7fsChannelInfo();
		chan.setSzID("magZ");
		chan.setSzUnit("gauss");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00014615608961321414);
		chan.setDwDivider(20);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("magY");
		chan.setSzUnit("gauss");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00014615608961321414);
		chan.setDwDivider(20);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("magX");
		chan.setSzUnit("gauss");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00014615608961321414);
		chan.setDwDivider(20);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		recordedChannels.add(subList);
		subList = new ArrayList<String>();

		chan = new Ams7fsChannelInfo();
		chan.setSzID("Temp"); //
		chan.setSzUnit("\u00B0C");
		chan.setnBits(16);
		chan.setRealConstant(25);
		chan.setRealSlope(0.0075483091787439619);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("MZR"); // AccelX
		chan.setSzUnit("g");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00048828125);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("MYR"); // AccelY
		chan.setSzUnit("g");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00048828125);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("MXR"); // AccelZ
		chan.setSzUnit("g");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00048828125);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("GyroZ"); //
		chan.setSzUnit("dps");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.06103515625);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("GyroY"); //
		chan.setSzUnit("dps");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.06103515625);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("GyroX"); //
		chan.setSzUnit("dps");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.06103515625);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Tickdiff ADC"); //
		chan.setSzUnit("s");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(1);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		recordedChannels.add(subList);
		subList = new ArrayList<String>();

		chan = new Ams7fsChannelInfo();

		/*
		 * recordedChannels = new ArrayList<String>();
		 * for (Ams7fsChannelInfo ch : channelInfo)
		 * recordedChannels.add(ch.getSzID());
		 */
		// add roll, pitch, yaw
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Roll"); //
		chan.setSzUnit("\u00B0");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(1);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Pitch"); //
		chan.setSzUnit("\u00B0");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(1);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("Yaw"); //
		chan.setSzUnit("\u00B0");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(1);
		subList.add(chan.getSzID());
		channelInfo.add(chan);
		chan = new Ams7fsChannelInfo();
		chan.setSzID("AccelVectorMag"); //
		chan.setSzUnit("g");
		chan.setnBits(16);
		chan.setRealConstant(0);
		chan.setRealSlope(0.00048828125);
		channelInfo.add(chan);
		chan.setDwDivider(1);
		subList.add(chan.getSzID());
		recordedChannels.add(subList);
	}

	public void connectToUDP() {
		UDPConnectionDialog diag = new UDPConnectionDialog(this);
		diag.setVisible(true);
	}

	public void connectToBLE() {
		BLEConnectionDialog diag = new BLEConnectionDialog(this);
		diag.setVisible(true);
	}

	public void ReadFileheader(byte[] b, char tag) {
		Gson gson = Ams7fsFile.getHeaderGson();
		ubb.setBytes(b);
		ubb.getByte();
		int bytes2 = ubb.getShort();
		int wSize = bytes2 & 0x0FFF;
		ubb.getByte();
		String jsonString = ubb.getString(wSize - 4);
		System.out.println("Received header " + tag + " of size " + wSize);
		ChannelSet map = null;
		try {
			map = gson.fromJson(jsonString, ChannelSet.class);
		} catch (com.google.gson.JsonSyntaxException e) {
			System.out.println(e);
			return;
		}
		if (map == null)
			return;
		Ams7fsChannelInfo chan = null;
		switch ((char) tag) {
			case 'A':
				chan = null;
				List<Map<String, String>> ADC = map.getCalc();
				for (Map<String, String> entry : ADC) {
					String name = entry.get("name");
					if (!name.equals("tv")) {
						// System.out.println(name);
						if (name.equals("Vicg"))
							name = "Z0";
						if (name.equals("Vecg"))
							name = "ECG";
						for (Ams7fsChannelInfo s : channelInfo)
							if (s.getSzID().equals(name))
								chan = s;
						if (chan != null) {
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(32);
							chan.setRealConstant(Double.valueOf(entry.get("a0")));
							chan.setRealSlope(Double.valueOf(entry.get("a1")));
						}
					}
				}
				break;
			case 'B':
				chan = null;
				List<Map<String, String>> BAT = map.getCalc();
				for (Map<String, String> entry : BAT) {
					String name = entry.get("name");
					if (!name.equals("tv")) {
						// System.out.println(name);
						if (name.equals("Vbat"))
							name = "BAT";
						for (Ams7fsChannelInfo s : channelInfo)
							if (s.getSzID().equals(name))
								chan = s;
						if (chan != null) {
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(32);
							chan.setRealConstant(Double.valueOf(entry.get("a0")));
							chan.setRealSlope(Double.valueOf(entry.get("a1")));
							chan.setDwDivider(1000);
						} else {
							chan = new Ams7fsChannelInfo();
							chan.setSzID(name);
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(32);
							chan.setRealConstant(Double.valueOf(entry.get("a0")));
							chan.setRealSlope(Double.valueOf(entry.get("a1")));
							chan.setTickFile("TicksA");
							channelInfo.add(chan);
							recordedChannels.get(0).add(name);
						}
					}
				}
				break;
			case 'D':
				chan = null;
				List<Map<String, String>> pressure = map.getCalc();
				int kP = 1, kT = 1;
				for (Map<String, String> entry : pressure) {
					String name = entry.get("name");
					if (name.equals("P_sc"))
						kP = Integer.valueOf(entry.get("kP"));
					else if (name.equals("T_sc"))
						kT = Integer.valueOf(entry.get("kT"));
					else if (!name.equals("tv")) {
						// System.out.println(name);
						if (name.equals("P"))
							name = "P_sc";
						if (name.equals("T"))
							name = "T_sc";
						for (Ams7fsChannelInfo s : channelInfo)
							if (s.getSzID().equals(name))
								chan = s;
						if (chan != null) {
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(32);
							if (name.equals("P_sc"))
								chan.setRealSlope(1.0 / kP);
							if (name.equals("T_sc"))
								chan.setRealSlope(1.0 / kT);
							chan.setDwDivider(200);
							chan.setFormula(entry.get("formula"));
							Map<String, Double> c = new HashMap<String, Double>();
							for (String key : entry.keySet()) {
								if (key.startsWith("c")) {
									Double value = Double.valueOf(entry.get(key));
									c.put(key, value);
								}
							}
							chan.setConstants(c);
						}
					}
				}
				break;
			case 'G':
				chan = null;
				List<Map<String, String>> mot = map.getCalc();
				for (Map<String, String> entry : mot) {
					String name = entry.get("name");
					if (!name.equals("tv")) {
						// System.out.println(name);
						for (Ams7fsChannelInfo s : channelInfo)
							if (s.getSzID().equals(name))
								chan = s;
						if (chan != null) {
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(16);
							if (entry.containsKey("a0"))
								chan.setRealConstant(Double.valueOf(entry.get("a0")));
							chan.setRealSlope(Double.valueOf(entry.get("a1")));
						}
					}
				}
				break;
			case 'M':
				chan = null;
				List<Map<String, String>> magneto = map.getCalc();
				for (Map<String, String> entry : magneto) {
					String name = entry.get("name");
					if (!name.equals("tv")) {
						// System.out.println(name);
						if (name.equals("AccelX"))
							name = "MXR";
						if (name.equals("AccelY"))
							name = "MYR";
						if (name.equals("AccelZ"))
							name = "MZR";
						for (Ams7fsChannelInfo s : channelInfo)
							if (s.getSzID().equals(name))
								chan = s;
						if (chan != null) {
							chan.setSzUnit(entry.get("unit"));
							chan.setnBits(16);
							if (entry.containsKey("a0"))
								chan.setRealConstant(Double.valueOf(entry.get("a0")));
							chan.setRealSlope(Double.valueOf(entry.get("a1")));
						}
					}
				}
				break;
			default:
				if ((char) tag != 'C')
					System.out.println((char) tag);
		}
		/*
		 * recordedChannels = new ArrayList<String>();
		 * for (Ams7fsChannelInfo c : channelInfo)
		 * recordedChannels.add(c.getSzID());
		 */
	}

	public boolean handlePacket(byte[] packet) {
		if (bytesLeft == 0) {
			List<Character> tags = Arrays.asList('H', 'C', 'I', 'B', 'A', 'D', 'M', 'G');
			for (int j = 0; j < packet.length; j++) {
				char s = (char) packet[j];
				if (tags.contains(s)) {
					byte b[] = new byte[4];
					for (int i = 0; i < 4; i++)
						b[i] = packet[i + j];
					ubb.setBytes(b);
					byte tag = ubb.getByte();
					int bytes2 = ubb.getShort();
					int wSize = bytes2 & 0x0FFF;
					int type = bytes2 & 0xF000; // 0x8000 is json; 0 is data
					byte block[] = new byte[wSize];
					if (wSize == 0) {
						continue;
					}
					if (wSize + j > packet.length) {
						if (j > 0) // big packets always start at 0
							continue;
						bigBlock = new byte[wSize];
						for (int i = 0; i < packet.length; i++)
							bigBlock[i] = packet[i + j];
						bytesLeft = wSize - packet.length;
						continue;
					}
					for (int i = 0; i < wSize; i++)
						block[i] = packet[i + j];
					if (Ams7fsFile.crc8Check(block) != 0) {
						System.out.println("crc failed header " + wSize + " " + s);
					} else {
						j = j + wSize - 1;
						if (s == 'B')
							Bfound = true;
						if (s == 'D')
							Dfound = true;
						if (type == 0)
							ReadDataBlock(block);
						else
							ReadFileheader(block, (char) tag);
					}
				}
			}
		} else {
			int start = bigBlock.length - bytesLeft;
			for (int i = 0; i < packet.length; i++) {
				if (i + start == bigBlock.length)
					break;
				bigBlock[i + start] = packet[i];
			}
			bytesLeft = bytesLeft - packet.length;
			if (bytesLeft <= 0) {
				byte b[] = new byte[4];
				for (int i = 0; i < 4; i++)
					b[i] = bigBlock[i];
				ubb.setBytes(b);
				byte tag = ubb.getByte();
				int bytes2 = ubb.getShort();
				int wSize = bytes2 & 0x0FFF;
				int type = bytes2 & 0xF000; // 0x8000 is json; 0 is data
				if (Ams7fsFile.crc8Check(bigBlock) != 0) {
					System.out.println("crc failed header " + wSize + " " + (char) tag);
				} else {
					if (type == 0)
						ReadDataBlock(bigBlock);
					else
						ReadFileheader(bigBlock, (char) tag);
				}
				bytesLeft = 0;
			}
		}
		return Bfound && Dfound;
	}

	public void ReadDataBlock(byte[] b) {
		Ams7fsPacket packet;
		int wSize;
		ubb.setBytes(b);
		int wTag = ubb.getByte();
		int bytes2 = ubb.getShort();
		wSize = bytes2 & 0x0FFF;
		// int type = bytes2 & 0xF000; //0x8000 is json; 0 is data
		if (wSize / 4 <= 0)
			return;
		ubb.getByte();
		try {
			packet = new Ams7fsPacket(ubb, wTag, wSize);
		} catch (BufferUnderflowException e) {
			System.out.println("Packet of type " + (char) wTag + " of too short length " + wSize);
			return;
		}
		char packetType = (char) packet.getwTag();
		int[] dat = packet.getData();
		int l;
		if (dat == null)
			l = 0;
		else
			l = dat.length;
		int[] datA = new int[l + 3];
		int skip = 1, addedChans = 0, dummies = 0;
		int chanIndex = 0;
		List<String> packetChannels;
		switch (packetType) {
			case 'A':
				skip = 2;
				addedChans = 3;
				packetChannels = recordedChannels.get(0);
				if (dat.length != 6) { // bad data
					if (dat.length == 7 && packetChannels.size() == 5) {
						setDefaultChannels(true);
						packetChannels = recordedChannels.get(0);
					} else if (dat.length != 7) {
						System.out.println("Packet of type " + packetType + " of incorrect length");
						return;
					}
				}
				datA[0] = dat[0]; // tick
				datA[1] = dat[1]; // status
				datA[2] = dat[5]; // ECG
				datA[3] = dat[3]; // V2ecg
				datA[4] = dat[5] - dat[3]; // V3ecg
				datA[5] = dat[2]; // Z0
				datA[6] = dat[2]; // DZDT
				datA[7] = dat[4]; // Visrc
				datA[8] = dat[4]; // SCL
				datA[9] = dat[6]; // T
				for (int i = 0; i < datA.length - skip; i++)
					Abuffer[i] = datA[i + skip];
				break;
			case 'B':
				packetChannels = recordedChannels.get(1);
				if (dat.length != 2) { // bad data
					System.out.println("Packet of type " + packetType + " of incorrect length");
					return;
				}
				chanIndex = recordedChannels.get(0).size();
				for (int i = 0; i < dat.length - skip; i++)
					Bbuffer[i] = dat[i + skip];
				break;
			case 'C':
				packetChannels = new ArrayList<String>();
				// CreateStart(dat[0], dat[1] & 0xFFFFFFFF);
				break;
			case 'D':
				packetChannels = recordedChannels.get(2);
				if (dat.length != 3) { // bad data
					System.out.println("Packet of type " + packetType + " of incorrect length");
					return;
				}
				chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size();
				for (int i = 0; i < dat.length - skip; i++)
					Dbuffer[i] = dat[i + skip];
				break;
			case 'I':
				packetChannels = new ArrayList<String>();
				// packet.setDwClockTick_ms((long) dat[0]);
				events.add(packet);
				break;
			case 'G':
				packetChannels = recordedChannels.get(3);
				if (dat.length != 5) { // bad data
					System.out.println("Packet of type " + packetType + " of incorrect length");
					return;
				}
				chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size()
						+ recordedChannels.get(2).size();
				for (int i = 0; i < dat.length - skip - 1; i++)
					Gbuffer[i] = dat[i + skip];
				dummies = 1;
				break;
			case 'M':
				packetChannels = recordedChannels.get(4);
				if (dat.length != 9) { // bad data
					System.out.println("Packet of type " + packetType + " of incorrect length");
					return;
				}
				chanIndex = recordedChannels.get(0).size() + recordedChannels.get(1).size()
						+ recordedChannels.get(2).size() + recordedChannels.get(3).size();
				for (int i = 0; i < dat.length - skip - 1; i++)
					Mbuffer[i] = dat[i + skip];
				dummies = 1;
				addedChans = 1;
				break;
			default:
				packetChannels = new ArrayList<String>();
				if (packetType != 'C')
					System.out.println(packetType);
		}

		int packetIndex = 0;
		if (dat != null && packetType != 'C' && packetType != 'H') { // for markers
			if (dat.length != packetChannels.size() + skip - addedChans + dummies) { // bad data
				System.out.println("Packet of type " + packetType + " of incorrect length " + dat.length);
				return;
			}
		} else if (packetType == 'C') {
			if (dat.length != 3 && dat.length != 5) { // for int32 and int64 timestamps
				System.out.println("Packet of type " + packetType + " of incorrect length " + dat.length);
				return;
			}
		}
		if (packetType == 'A')
			for (int i = 0; i < datA.length - skip; i++) {
				bufferData[chanIndex] = datA[packetIndex + skip]; // - minVal;
				chanIndex++;
				packetIndex++;
			}
		else if (dat != null && packetType != 'H')
			for (int i = 0; i < dat.length - skip; i++) {
				bufferData[chanIndex] = dat[packetIndex + skip]; // - minVal;
				chanIndex++;
				packetIndex++;
			}
		int expectedDiff, foundDiff;
		if (isBLE)
			expectedDiff = 10;
		else
			expectedDiff = 1;
		if (packetType == 'A') {
			// if (dat[0] - prevTime != 1)
			// System.out.println("Tick " + dat[0] + " " + (dat[0] - prevTime));
			foundDiff = dat[0] - prevTimeA;
			if (Math.abs(foundDiff) > expectedDiff * 10 + 1)
				System.out.println("TickA " + prevTimeA + " " + dat[0]);
			bufferData[20] = foundDiff;
			Abuffer[packetIndex] = foundDiff;
			Abuffer[packetIndex + 1] = dat[0];
			prevTimeA = dat[0];
			// packetChannels.add(recordedChannels.get(3).get(recordedChannels.get(3).size()-1));
			// //tickcount
		}
		if (packetType == 'M') {
			// if (dat[0] - prevTime != 1)
			// System.out.println("Tick " + dat[0] + " " + (dat[0] - prevTime));
			foundDiff = dat[0] - prevTimeM;
			if (Math.abs(foundDiff) > expectedDiff * 10 + 1)
				System.out.println("TickM " + prevTimeM + " " + dat[0]);
			bufferData[21] = foundDiff;
			Mbuffer[packetIndex - 1] = foundDiff;
			Mbuffer[packetIndex] = dat[0];
			prevTimeM = dat[0];
		}
		if (packetType == 'B') {
			// if (dat[0] - prevTimeB != 1000)
			// System.out.println("TickB " + prevTimeB + " " + dat[0]);
			Bbuffer[packetIndex] = dat[0];
			// prevTimeB = dat[0];
		}
		if (packetType == 'D') {
			// if (dat[0] - prevTimeD != 200)
			// System.out.println("TickD " + prevTimeD + " " + dat[0]);
			Dbuffer[packetIndex] = dat[0];
			// prevTimeD = dat[0];
		}
		if (packetType == 'G') {
			// if (dat[0] - prevTimeG != 1000)
			// System.out.println("TickG " + prevTimeG + " " + dat[0]);
			Gbuffer[packetIndex] = dat[0];
			// prevTimeG = dat[0];
		}
		if (packetType == 'C') {
			if (dat[1] - prevTstamp != 10 && prevTstamp > 0)
				System.out.println("rtc timestamp: " + dat[1] + " " + (dat[1] - prevTstamp));
			prevTstamp = dat[1];
		}
	}

	public int[] getRawValues() {
		return bufferData;
	}

	public void getOnlineData() {
		listOfListsA.clear();
		listOfListsB.clear();
		listOfListsG.clear();
		listOfListsD.clear();
		listOfListsM.clear();
		if (!isBLE) {
			try {
				// socket.receive(response);
				int portNumber = devDialog.portNumber;
				String deviceIP = devDialog.deviceIP;
				buffer = UDPSockets.getInstance().getPacket(portNumber, InetAddress.getByName(deviceIP));
			} catch (IOException e) {
				if (!webSocketClient.isOpen()) {
					devDialog.connectionLost();
					if (devDialog.isGivenUp())
						return;
				}
				webSocketClient.send("cmd 3a");
			}
			if (buffer == null) {
				if (!webSocketClient.isOpen()) {
					devDialog.connectionLost();
					if (devDialog.isGivenUp())
						return;
				}
			}
			// remove matlab dependency
			// } else {
			// Object[] c;
			// try {
			// // c = obj.multipleRead(2, a);
			// // MWNumericArray streamData = (MWNumericArray)c[1];
			// c = obj.getData(1, a);
			// streamData = (MWNumericArray) c[0];
			// buffer = streamData.getByteData();
			// if (!Arrays.equals(buffer, buffer_old)) {
			// buffer_old = buffer;
			// } else {
			// return; // don't handle same packet twice
			// }
			// streamData.dispose();
			// } catch (MWException e) {
			// }
		}
		if (buffer != null) {
			List<Character> tags = Arrays.asList('B', 'A', 'D', 'M', 'I', 'G');
			for (int j = 0; j < buffer.length; j++) {
				char s = (char) buffer[j];
				if (tags.contains(s)) {
					byte b[] = new byte[4];
					if (4 + j > buffer.length) {
						// System.out.println("buffer end " + s);
						continue;
					}
					for (int i = 0; i < 4; i++) {
						b[i] = buffer[i + j];
					}
					ubb.setBytes(b);
					ubb.getByte();
					int bytes2 = ubb.getShort();
					int wSize = bytes2 & 0x0FFF;
					int type = bytes2 & 0xF000; // 0x8000 is json; 0 is data
					byte block[] = new byte[wSize];
					if (wSize + j > buffer.length || wSize == 0) {
						// System.out.println("buffer end " + s);
						continue;
					}
					for (int i = 0; i < wSize; i++)
						block[i] = buffer[i + j];
					if (Ams7fsFile.crc8Check(block) != 0) {
						System.out.println("crc failed online " + wSize + " " + s);
					} else {
						j = j + wSize - 1;
						if (type == 0) {
							ReadDataBlock(block);
							switch (s) {
								case 'A':
									List<Integer> intList = new ArrayList<Integer>(Abuffer.length);
									for (int i : Abuffer)
										intList.add(i);
									listOfListsA.add(intList);
									break;
								case 'B':
									intList = new ArrayList<Integer>(Bbuffer.length);
									for (int i : Bbuffer)
										intList.add(i);
									listOfListsB.add(intList);
									break;
								case 'D':
									intList = new ArrayList<Integer>(Dbuffer.length);
									for (int i : Dbuffer)
										intList.add(i);
									listOfListsD.add(intList);
									break;
								case 'M':
									intList = new ArrayList<Integer>(Mbuffer.length);
									for (int i : Mbuffer)
										intList.add(i);
									listOfListsM.add(intList);
									break;
								case 'G':
									intList = new ArrayList<Integer>(Gbuffer.length);
									for (int i : Gbuffer)
										intList.add(i);
									listOfListsG.add(intList);
									break;
								case 'I':
									System.out.println("Marker recieved");
									break;
								default:
									System.out.println("Should not happen!");
							}
						} else
							ReadFileheader(block, (char) s);
					}
				}
			}
		}
	}
}
