package nl.vu.psy.ams.suite.data.structures;
/*
 * Definition of a value for the label configuration, for
 * a single catagory. Each value has a code (int) and a description (String).
 */
public class LabelValue {
	private int		code;
	private String	name;
	public LabelValue() {
		code = -1;
		name = "";
	}
	public LabelValue(int code, String name) {
		this.setCode(code);
		this.setName(name);
	}
	public int getCode() {
		return code;
	}
	public String getName() {
		return name;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return code + " " + name;
	}

}
