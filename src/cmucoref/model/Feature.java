package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(index);
		out.writeInt(gid);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		index = in.readInt();
		gid = in.readInt();
		val = 1.0;
	}
}

