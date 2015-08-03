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
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(index);
		out.writeInt(gid);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		index = in.readInt();
		gid = in.readInt();
	}
}

