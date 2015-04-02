package cmucoref.model;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cmucoref.util.trove.EqualsHashingStrategy;

public class Alphabet implements Serializable{
	/**
	 * 
	 */

	TCustomHashMap<String, TObjectIntHashMap<String>> map;
	
	int numEntries;

	boolean growthStopped;

	public Alphabet(int capacity) {
		this.map = new TCustomHashMap<String, TObjectIntHashMap<String>>(new EqualsHashingStrategy<String>(), capacity);
		numEntries = 0;
		growthStopped = false;
	}

	public Alphabet() {
		this(100000);
	}
	
	public int lookupIndex(String entry, String given) {
		if (entry == null || given == null) {
			throw new IllegalArgumentException("Can't lookup \"null\" in an Alphabet.");
		}
		
		TObjectIntHashMap<String> subMap = map.containsKey(given) ? map.get(given) : null;
		int ret = subMap == null ? -1 : subMap.get(entry);
		
		if(subMap == null && !growthStopped){
			subMap = new TObjectIntHashMap<String>();
			subMap.put(entry, numEntries);
			ret = numEntries;
			numEntries++;
			map.put(given, subMap);
		}
		
		return ret;
	}
	
	public int size() {
		return numEntries;
	}

	public void stopGrowth() {
		growthStopped = true;
		map.compact();
	}
	
	// Serialization

	private static final long serialVersionUID = 1;

	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeInt(numEntries);
		out.writeObject(map);
		out.writeBoolean(growthStopped);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		if(version != CURRENT_SERIAL_VERSION){
			System.err.println("version error:");
			System.err.println("current version: " + CURRENT_SERIAL_VERSION);
			System.err.println("model version: " + version);
			System.exit(1);
		}
		numEntries = in.readInt();
		map = (TCustomHashMap<String, TObjectIntHashMap<String>>) in.readObject();
		growthStopped = in.readBoolean();
	}

}
