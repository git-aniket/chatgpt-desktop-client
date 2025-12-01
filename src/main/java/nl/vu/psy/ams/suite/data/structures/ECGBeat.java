package nl.vu.psy.ams.suite.data.structures;

/*
 * Structure that holds information (time, suspicion, etc) for
 * a single QRS-complex
 */
public class ECGBeat implements Comparable<ECGBeat> {
	private double RPeakTime = 0, RPeakHeight = 0, baseHeight = 0, RPeakHeightF = 0, baseHeightF = 0;
	private Boolean isFirstInSeries;
	private Double IBISuspicion;

	public ECGBeat() {

	}

	public ECGBeat(double RPeakTime) {
		this.RPeakTime = RPeakTime;
	}

	@Override
	public int compareTo(ECGBeat o) {
		return Double.compare(RPeakTime, o.RPeakTime);
	}

	public Double getIBISuspicion() {
		if (IBISuspicion == null)
			return Double.valueOf(0);
		return IBISuspicion;
	}

	public double getRPeakTime() {

		return RPeakTime;
	}

	public double getRPeakHeight() {

		return RPeakHeight;
	}

	public double getBaseHeightF() {

		return baseHeightF;
	}

	public double getRPeakHeightF() {

		return RPeakHeightF;
	}

	public double getBaseHeight() {

		return baseHeight;
	}

	public boolean isFirstInSeries() {
		if (isFirstInSeries == null)
			return false;
		return isFirstInSeries;
	}

	public void setFirstInSeries(Boolean isFirst) {
		if (isFirst == false) {
			isFirstInSeries = null;
		} else {
			isFirstInSeries = isFirst;
		}
	}

	public void setIBISuspicion(Double iBISuspicion) {
		IBISuspicion = iBISuspicion;
	}

	public void setRPeakTime(double d) {
		RPeakTime = d;
	}

	public void setRPeakHeight(double d) {
		RPeakHeight = d;
	}

	public void setBaseHeight(double d) {
		baseHeight = d;
	}

	public void setRPeakHeightF(double d) {
		RPeakHeightF = d;
	}

	public void setBaseHeightF(double d) {
		baseHeightF = d;
	}
}
