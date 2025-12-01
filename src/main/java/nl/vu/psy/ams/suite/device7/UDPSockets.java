package nl.vu.psy.ams.suite.device7;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UDPSockets {

    private class UDPReceiver extends Thread {
        private HashMap<InetAddress, BlockingQueue<byte[]>> addresses;
        DatagramSocket socket;

        public UDPReceiver(HashMap<InetAddress, BlockingQueue<byte[]>> addresses, DatagramSocket socket) {
            this.addresses = addresses;
            this.socket = socket;
        }

        public void run() {
            try {
                while (addresses.size() > 0) {
                    byte[] buffer = new byte[1500];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    if (socket == null)
                        return;
                    socket.receive(response);
	    			InetSocketAddress UDPsocketAddress = (InetSocketAddress)response.getSocketAddress();
		    		InetAddress UDPaddress = UDPsocketAddress.getAddress();
                    BlockingQueue<byte[]> queue = addresses.get(UDPaddress);
                    if (queue != null)
                        queue.put(buffer);
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void addAddress(InetAddress newAddress) {
            addresses.put(newAddress, new LinkedBlockingQueue<>());
        }
    
        public void removeAddress(InetAddress newAddress) {
            addresses.remove(newAddress);
        }

        public HashMap<InetAddress, BlockingQueue<byte[]>> getAddresses() {
            return addresses;
        }
    }
    
	private static UDPSockets	instance;

	public static UDPSockets getInstance() {
		if (instance == null) {
			instance = new UDPSockets();
		}
		return instance;
	}

    private HashMap <Integer, DatagramSocket> udpSockets;
    private HashMap <Integer, UDPReceiver> receivers;

    private UDPSockets() {
        udpSockets = new HashMap<Integer,DatagramSocket>();
        receivers = new HashMap<Integer, UDPReceiver>();
    }

    public void put(Integer port, DatagramSocket socket, InetAddress address) {
        DatagramSocket existing = udpSockets.put(port, socket);
        UDPReceiver receiver = null;
        if (existing == null) {
            try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(30000);
            socket.setReuseAddress(true);
            } catch (SocketException e) {
			    e.printStackTrace();
		    }
            udpSockets.put(port, socket);       
            HashMap<InetAddress, BlockingQueue<byte[]>> addresses = new HashMap<InetAddress, BlockingQueue<byte[]>>();
            addresses.put(address, new LinkedBlockingQueue<>());
            receiver = new UDPReceiver(addresses, socket);
            new Thread(receiver).start();
            receivers.put(port, receiver);
        } else {
            receiver = receivers.get(port);
            receiver.addAddress(address);
        }
    }

    public byte[] getPacket(Integer port, InetAddress address) {
        try {
            if (this.receivers == null || this.receivers.get(port) == null || this.receivers.get(port).getAddresses().get(address) == null)
                return null;
            return receivers.get(port).getAddresses().get(address).poll(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void remove(Integer port, InetAddress address) {
        if (receivers.get(port) == null)
            return;
        receivers.get(port).removeAddress(address);
        if (!receivers.get(port).isAlive()) {
            DatagramSocket sock = udpSockets.get(port);
            sock.close();
            udpSockets.remove(port);
            receivers.remove(port);
        }
    }
}
