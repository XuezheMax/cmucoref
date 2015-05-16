package cmucoref.model;

import gnu.trove.list.TLinkableAdapter;

@SuppressWarnings("rawtypes")
public class Feature extends TLinkableAdapter{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int index;

	public int gid;
	
	public double val;

	
	public Feature(int i, int gid, double val) {
		index = i;
		this.gid = gid;
		this.val = val;
	}

	public Feature(int i, int gid) {
		this(i, gid, 1.0);
	}
	
	@Override
	public final Feature clone() {
		return new Feature(index, gid, val);
	}

	@Override
	public final String toString() {
		return index + "|" + gid + ":" + val;
	}
}

