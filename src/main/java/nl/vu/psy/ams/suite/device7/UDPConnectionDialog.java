package nl.vu.psy.ams.suite.device7;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.gui.MainFrame;
/*
 * Dialog that lets you choose which IP address you
 * want to connect with. 
 */
public class UDPConnectionDialog extends JDialog {

	private static final long	serialVersionUID	= 1L;
	private JFormattedTextField deviceIP;
	private JFormattedTextField portNumber;
	private JFormattedTextField packet;
	private AmsDevice7			ams;
	
	public UDPConnectionDialog(final AmsDevice7 ams) {
		super(MainFrame.getInstance().getMainFrame(), "Connect to UDP device", true);
		this.ams = ams;
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		JPanel pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel("Device IP:"), BorderLayout.WEST);
		deviceIP = new JFormattedTextField("192.168.4.1");
		deviceIP.setColumns(16);
		pan2.add(deviceIP, BorderLayout.CENTER);
		pan.add(pan2);
		pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel("Packet size:"), BorderLayout.WEST);
		packet = new JFormattedTextField(1500);
		packet.setColumns(8);
		pan2.add(packet, BorderLayout.CENTER);
		pan.add(pan2);
		pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel("Port:"), BorderLayout.WEST);
		portNumber = new JFormattedTextField(1234);
		portNumber.setColumns(5);
		pan2.add(portNumber, BorderLayout.CENTER);
		pan.add(pan2);

		pan2 = new JPanel();
		final JButton connectBut = new JButton("Connect");
		//connectBut.setEnabled(false);
		pan2.add(connectBut);
		JButton closeBut = new JButton("Cancel");
		pan2.add(closeBut);
		pan.add(pan2);
		add(pan);
		pack();
		getRootPane().setDefaultButton(connectBut);//ENTER will hit button Connect
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());



		closeBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		connectBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
	}

	private void connect() {
		try {
			ams.webSocketClient = new MyWebSocketClient(new URI("ws://" + deviceIP.getValue().toString() + ":80/ws"));
			if (!ams.webSocketClient.connectBlocking(10, TimeUnit.SECONDS)) {
				JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "Device not found! Check wifi connection and device IP");
				setVisible(false);
			}else{
				setVisible(false);
				DeviceDialog7 devDiag = new DeviceDialog7(ams, deviceIP.getValue().toString(), (int) portNumber.getValue(), (int) packet.getValue());
				devDiag.setVisible(true);
				ams.setDevDialog(devDiag);
			}
		} catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
}
