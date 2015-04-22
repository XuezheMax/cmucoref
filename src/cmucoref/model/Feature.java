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

	public Feature(int i, int gid) {
		index = i;
		this.gid = gid;
	}

	@Override
	public final Feature clone() {
		return new Feature(index, gid);
	}

	@Override
	public final String toString() {
		return index + "|" + gid;
	}
}

