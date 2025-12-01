package nl.vu.psy.ams.suite.gui.tabs;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class StartTab extends AmsTab {
	private static final long serialVersionUID = 1L;
	private static StartTab instance;

	public static StartTab getInstance() {
		if (instance == null) {
			instance = new StartTab();
		}
		return instance;
	}

	public static StartTab getNewInstance() {
		instance = null;
		instance = new StartTab();
		return instance;
	}

	public void setupItems() {
		Image image = null;
		URL url = null;
		ImageIcon icon = null;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int frameheight = (int) screenSize.getHeight() - 256;
		try {
			url = new URI("https://vu-ams.nl/wp-content/uploads/2025/07/VU-AMS-Core-Graphic-V3-Landscape-scaled.png")
					.toURL();
			image = ImageIO.read(url);

			icon = new ImageIcon(
					image.getScaledInstance(-1, frameheight, Image.SCALE_REPLICATE));
		} catch (MalformedURLException ex) {
			System.out.println("Malformed URL");
		} catch (IOException iox) {
			System.out.println("Can not load file");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		JLabel label = new JLabel(icon);
		panel.add(label, 1F);

	}
}
