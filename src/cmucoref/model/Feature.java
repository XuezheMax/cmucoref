package cmucoref.model;

import gnu.trove.list.TLinkableAdapter;

@SuppressWarnings("rawtypes")
public class Feature extends TLinkableAdapter{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int index;

	public double value;

	public Feature(int i, double v) {
		index = i;
		value = v;
	}

	@Override
	public final Feature clone() {
		return new Feature(index, value);
	}

	public final Feature negation() {
		return new Feature(index, -value);
	}

	@Override
	public final String toString() {
		return index + "=" + value;
	}
}

