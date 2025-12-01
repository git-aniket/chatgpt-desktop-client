package nl.vu.psy.ams.suite.data.structures;

public class Step implements Comparable<Step> {
    
    private double	impactValue;
	private double	Time;
	
	public Step() {

	}

	public Step(double Time) {
		this.Time = Time;
	}

	public double getImpactValue(){
		return impactValue;
	}
	
	public double getTime(){
		return Time;
	}

	public void setImpactValue(double value){
		impactValue = value;
	}

	public void setTime(double time){
		Time = time;
	}

	@Override
	public int compareTo(Step o) {
		return Double.compare(Time, o.Time);
	}
}
