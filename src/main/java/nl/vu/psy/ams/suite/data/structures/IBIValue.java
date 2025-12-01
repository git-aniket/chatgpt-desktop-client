package nl.vu.psy.ams.suite.data.structures;
/*
 * Small helper class used for quickly drawing ibis
 */
public class IBIValue implements Comparable<IBIValue> {
	private double	time, value;
	public IBIValue(double time, double value) {
		this.time = time;
		this.value = value;
	}
	@Override
	public int compareTo(IBIValue o) {
		return Double.compare(time, o.time);
	}
	public double getTime() {
		return time;
	}
	public double getValue() {
		return value;
	}

}