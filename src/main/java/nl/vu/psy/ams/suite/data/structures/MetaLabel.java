package nl.vu.psy.ams.suite.data.structures;

import java.util.LinkedList;
/*
 * A 'combined' label, that can included multiple
 * normal label codes. Used for generating bar graphs
 * of combined labels.
 */
public class MetaLabel {
	private LinkedList<Integer>	connectedCodes	= new LinkedList<Integer>();
	private String				name;

	public MetaLabel() {

	}

	public MetaLabel(String name, int... codes) {
		this.name = name;
		for (int i : codes) {
			connectedCodes.add(i);
		}
	}

	public void addConnectedCode(int code) {
		connectedCodes.add(code);
	}

	public LinkedList<Integer> getConnectedCodes() {
		return connectedCodes;
	}
	public String getName() {
		return name;
	}

	public void removeConnectedCode(int code) {
		connectedCodes.remove(code);
	}

	public void setName(String name) {
		this.name = name;
	}

}
