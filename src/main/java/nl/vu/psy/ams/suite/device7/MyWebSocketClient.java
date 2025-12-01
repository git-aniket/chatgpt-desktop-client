package nl.vu.psy.ams.suite.device7;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

public class MyWebSocketClient extends WebSocketClient {

    private String acq = "", date = "", mem = "", name = "", version = "", hw_version = "", phase = "", bat = "", command = "";

    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        System.out.println("------ MyWebSocket onOpen ------");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The close codes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println(
        "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                + reason);
    }

    @Override
    public void onError(Exception arg0) {
        System.out.println("------ MyWebSocket onError ------" + arg0.getMessage());
    }

    @Override
    public void onMessage(String arg0) {
        parseMessage(arg0);
        System.out.println("-------- receive data from websocket server： " + arg0 + "--------");
    }

    @Override
    public void send(String text) {
        try {
            super.send(text);
        } catch (WebsocketNotConnectedException e) {
            // try {
            //     if(!isOpen())
            //         this.reconnectBlocking();
            // } catch (InterruptedException e1) {
            //     e1.printStackTrace();
            // }
            e.printStackTrace();
        }
    }

    void parseMessage(String message) {
        String[] lines = message.split("\n", 10);
        //String[] fields = ["acq", "date", "mem", "name", "version", "phase"]
        for (int l = 0; l < lines.length; l++) {
            if (lines[l].startsWith("acq"))
                acq = lines[l].replace("acq", "");
            else if (lines[l].startsWith("date"))
                date = lines[l].replace("date ", "");
            else if (lines[l].startsWith("mem"))
                mem = lines[l].replace("mem", "");
            else if (lines[l].startsWith("name"))
                name = lines[l].replace("name", "");
            else if (lines[l].startsWith("version"))
                version = lines[l].replace("version", "");
            else if (lines[l].startsWith("hw_version"))
                hw_version = lines[l].replace("hw_version", "");
            else if (lines[l].startsWith("phase"))
                phase = lines[l].replace("phase", "");
            else if (lines[l].startsWith("bat"))
                bat = lines[l].replace("bat", "");
            else if (!lines[l].startsWith("wifi") && !lines[l].startsWith("status")) {//assume value of a setting
                command = lines[l];
                System.out.println(command);
            }
            else 
                System.out.println(lines[l]);
            if (l > 8) {
                command = lines[l];
                System.out.println(command);
            }
        }
    }

    public String getAcq() {
        return acq;
    }
    public String getDate() {
        String temp = date;
        date = "";
        return temp;
    }
    public String getMem() {
        String temp = mem;
        mem = "";
        return temp;
    }
    public String getName() {
        return name;
    }
    public String getVersion() {
        return version;
    }
    public String getHWVersion() {
        return hw_version;
    }
    public String getPhase() {
        return phase;
    }
    public String getBat() {
        String temp = bat;
        bat = "";
        return temp;
    }
    public String getSetting() {
        System.out.println("get setting " + command);
        return command;
    }
    public void setAcq (String acq) {
        this.acq = acq;
    }
}