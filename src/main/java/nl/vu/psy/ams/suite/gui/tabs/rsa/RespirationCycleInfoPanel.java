package nl.vu.psy.ams.suite.gui.tabs.rsa;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.gui.graphs.overlays.RSASelectedCycleOverlay;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The small panel at the top of the RSA tab that shows
 * information about the currently selected Respiration cycle.
 */
public class RespirationCycleInfoPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel lab;
	private RSASelectedCycleOverlay overlay;
	private NumberFormat nf = NumberFormat.getInstance(Locale.US);

	public RespirationCycleInfoPanel(RSASelectedCycleOverlay selCycleOv1) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		lab = new JLabel(
				"Inspiration Start: N/A, Expiration Start: N/A, Expiration End: N/A, RSA: N/A, Respiration Rate: N/A, Longest: N/A, Shortest: N/A, Accepted");
		add(lab);
		nf.setMaximumFractionDigits(2);
		setCycleOverlay(selCycleOv1);
		updateText();
		setBackground(Color.WHITE);
	}

	public void setCycleOverlay(RSASelectedCycleOverlay ov) {
		overlay = ov;
		ov.setInfoPanel(this);
	}

	public void updateText() {
		RespirationCycle rc = overlay.getSelectedCycle();
		if (rc == null) {
			lab.setText(
					"Inspiration Start: N/A, Expiration Start: N/A, Expiration End: N/A, RSA: N/A, Respiration Rate: N/A, Longest: N/A, Shortest: N/A, Accepted");
		} else {
			String labelText = "<html><body>";
			labelText += "Inspiration Start: " + Utils.getDateAndTimeFromUS(rc.getInspStart());
			labelText += ", Expiration Start: " + Utils.getDateAndTimeFromUS(rc.getExpStart());
			if (rc.isExpEndSet()) {
				labelText += ", Expiration End: " + Utils.getDateAndTimeFromUS(rc.getExpEnd());
			} else {
				labelText += ", Expiration End: N/A";
			}
			Double rsa = rc.getRSA();
			if (rsa != null) {
				if (rsa > 0) {
					labelText += ", RSA: " + nf.format(rsa);
				} else {
					labelText += ", RSA: N/A";
				}
			} else {
				labelText += ", RSA: N/A";
			}
			Double rr = rc.getRespirationRate();
			if (rr != null) {
				labelText += ", Respiration Rate: " + nf.format(rr);
			} else {
				labelText += ", Respiration Rate: N/A";
			}
			double[] shortest = rc.getShortestIBI();
			if (shortest != null) {
				labelText += ", Shortest: " + nf.format(shortest[1] / 1000.);
			} else {
				labelText += ", Shortest: N/A";
			}
			double[] longest = rc.getLongestIBI(shortest);
			if (longest != null) {
				labelText += ", Longest: " + nf.format(longest[1] / 1000.);
			} else {
				labelText += ", Longest: N/A";
			}
			if (rsa != null) {
				if (rsa > 0) {
					labelText += ", <font color='green'>Accepted</font>";
				} else {
					labelText += ", <font color='red'>Rejected - Longest IBI shorter than Shortest IBI</font>";
				}
			} else {
				if (rc.isExpEndSet() == false) {
					labelText += ", <font color='red'>Rejected - Last Respiration Cycle</font>";
				} else if (rc.isClippingDZ()) {
					labelText += ", <font color='red'>Rejected - Clipping DZ</font>";
				} else if (rc.isIrregularRR()) {
					labelText += ", <font color='red'>Rejected - Irregular Respiration Rate</font>";
					// } else if (rc.isIrregularIbi()) {
					// labelText += ", <font color='red'>Rejected - Irregular IBI</font>";
				} else {
					if (shortest == null && longest == null) {
						labelText += ", <font color=#FF8000>Rejected - Shortest and Longest IBI not found</font>";
					} else if (shortest == null) {
						labelText += ", <font color=#FF8000>Rejected - Shortest IBI not found</font>";
					} else if (longest == null) {
						labelText += ", <font color=#FF8000>Rejected - Longest IBI not found</font>";
					} else {
						labelText += ", <font color=#FF8000>Rejected</font>";
					}
				}
			}
			labelText += "</body></html>";
			lab.setText(labelText);
		}
	}

}
