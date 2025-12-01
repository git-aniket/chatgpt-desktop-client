package nl.vu.psy.ams.suite.device7;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
// import java.io.OutputStream;
// import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.StoppableRunnable;

// import com.mathworks.toolbox.javabuilder.*;
/*
 * Dialog that lets you choose which device ID you
 * want to connect with. 
 */
public class BLEConnectionDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JList<String> cb;
	private DefaultListModel<String> foundBLENames = new DefaultListModel<>();
	private AmsDevice7 ams;
	private JDialog connDiag;
	private boolean isConnected = false;

	public BLEConnectionDialog(final AmsDevice7 ams) {
		super(MainFrame.getInstance().getMainFrame(), "Connect to BLE device", true);
		this.ams = ams;
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		JPanel pan2 = new JPanel(new BorderLayout());
		pan2.add(new JLabel("Device name:"), BorderLayout.WEST);
		// remove matlab dependency
		// PrintStream dummy = new PrintStream(new OutputStream() {
		// public void close() {
		// }

		// public void flush() {
		// }

		// public void write(byte[] b) {
		// }

		// public void write(byte[] b, int off, int len) {
		// }

		// public void write(int b) {
		// }
		// });

		// try {
		// MWComponentOptions options = new MWComponentOptions();
		// options.setPrintStream(dummy); // send all standard dips() output to a log
		// file
		// // the following ignores all error output (this will be caught by Java
		// exception handling anyway)
		// options.setErrorStream(dummy);
		// ams.obj = new bleMATLABClass.Class1(options);
		// Object[] alist = ams.obj.CreateBleClass(1);
		// ams.a = (MWMatrixRef) alist[0];
		// Object[] a1 = ams.obj.getDeviceList(1);
		// MWStringArray aa = (MWStringArray) a1[0];
		// Object[] deviceList = aa.toArray();
		// aa.dispose();
		// for (Object dev : deviceList)
		// if (!((String)dev).equals(""))
		// foundBLENames.addElement((String)dev);
		// } catch (MWException e) {
		// e.printStackTrace();
		// }

		cb = new JList<>(foundBLENames);

		pan2.add(cb, BorderLayout.CENTER);

		pan.add(pan2);

		pan2 = new JPanel();
		final JButton rescanBut = new JButton("Scan again for devices");
		pan2.add(rescanBut, BorderLayout.CENTER);
		pan.add(pan2);

		rescanBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// remove matlab dependency
				// try {
				// foundBLENames.removeAllElements();
				// Object[] a1 = ams.obj.getDeviceList(1);
				// MWStringArray aa = (MWStringArray) a1[0];
				// Object[] deviceList = aa.toArray();
				// aa.dispose();
				// for (Object dev : deviceList)
				// if (!((String)dev).equals(""))
				// foundBLENames.addElement((String)dev);
				// cb.ensureIndexIsVisible(foundBLENames.getSize());
				// cb.setVisibleRowCount(foundBLENames.getSize());
				// pack();
				// } catch (MWException e2) {
				// e2.printStackTrace();
				// }
			}
		});

		pan2 = new JPanel();
		final JButton connectBut = new JButton("Connect");
		connectBut.setEnabled(true);
		pan2.add(connectBut);
		JButton closeBut = new JButton("Cancel");
		pan2.add(closeBut);
		pan.add(pan2);
		add(pan);
		pack();
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

		connectBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (cb.getSelectedIndex() != -1)
					connect();
			}
		});

		closeBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
	}

	private void connect() {
		String val = cb.getSelectedValue();

		final JDialog connDiag = new JDialog(BLEConnectionDialog.this, "Connecting to AMS Device " + val, true);
		JPanel pan = new JPanel(new BorderLayout());
		final JProgressBar prog = new JProgressBar(0, 4);
		pan.add(prog, BorderLayout.CENTER);
		JPanel pan2 = new JPanel();
		JButton giveUpB = new JButton("Give Up");
		pan2.add(giveUpB);
		pan.add(pan2, BorderLayout.SOUTH);
		connDiag.add(pan);
		connDiag.pack();
		connDiag.setMinimumSize(
				new Dimension(connDiag.getPreferredSize().width * 2, connDiag.getPreferredSize().height));
		connDiag.setLocationRelativeTo(BLEConnectionDialog.this);

		final StoppableRunnable run = new StoppableRunnable() {
			@Override
			public void run() {
				// remove matlab dependency
				// try {
				// ams.obj.bleConnect(ams.a,val);
				// // MWLogicalArray ba = (MWLogicalArray) b[0];
				// isConnected = true; // ba.getBoolean(1);
				// connDiag.setVisible(false);
				// } catch (MWException e) {
				// e.printStackTrace();
				// }
				connDiag.setVisible(false);
			}
		};
		CachedThreadPool.submit(run);
		giveUpB.addActionListener(l);
		connDiag.setVisible(true);

		setVisible(false);
		if (isConnected) {
			DeviceDialogBLE devDiag = new DeviceDialogBLE(ams, val, 0, 1500);
			devDiag.setVisible(true);
			ams.setBLEDialog(devDiag);
		} else {
			run.stopThread();
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"Device not found! Check if it is still on.");
		}
	}

	ActionListener l = (new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			connDiag.setVisible(false);
		}
	});

}
