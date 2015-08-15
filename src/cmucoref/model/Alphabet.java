package cmucoref.model;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cmucoref.util.Pair;
import cmucoref.util.trove.EqualsHashingStrategy;

public class Alphabet implements Serializable{
	/**
	 * 
	 */

	protected TCustomHashMap<String, TObjectIntHashMap<String>> map;
	protected TObjectIntHashMap<String> givenMap;
	protected TIntArrayList idToGid;
	protected TIntArrayList sizes;
	
	protected int numEntries;
	protected int numGivens;

	protected boolean growthStopped;
	
	private void calcSizeOfGiven() {
		sizes = new TIntArrayList(numGivens);
		sizes.add(new int[numGivens]);
		TObjectIntIterator<String> iter = givenMap.iterator();
		while(iter.hasNext()) {
			iter.advance();
			int gid = iter.value();
			if(map.contains(iter.key())) {
				sizes.set(gid, map.get(iter.key()).size());
			}
			else {
				sizes.set(gid, 0);
			}
		}
	}

	public Alphabet(int capacity) {
		this.map = new TCustomHashMap<String, TObjectIntHashMap<String>>(new EqualsHashingStrategy<String>(), capacity);
		this.givenMap = new TObjectIntHashMap<String>(capacity / 100);
		idToGid = new TIntArrayList(capacity);
		numEntries = 0;
		numGivens = 0;
		growthStopped = false;
	}

	public Alphabet() {
		this(100000);
	}
	
	public int lookupGivenIndex(String given) {
		if (given == null) {
			return -1;
		}
		
		int ret = givenMap.containsKey(given) ? givenMap.get(given) : -1;
		
		if(ret == -1 && !growthStopped) {
			ret = numGivens;
			givenMap.put(given, ret);
			numGivens++;
		}
		return ret;
	}
	
	public Pair<Integer, Integer> lookupIndex(String entry, String given) {
		int gid = lookupGivenIndex(given);
		if(gid == -1) {
			return new Pair<Integer, Integer>(-1, -1);
		}
		
		TObjectIntHashMap<String> subMap = map.containsKey(given) ? map.get(given) : null;
		if(subMap == null) {
			subMap = new TObjectIntHashMap<String>();
			map.put(given, subMap);
		}
		
		if(entry == null) {
			return new Pair<Integer, Integer>(-1, gid);
		}
		
		int ret = subMap.containsKey(entry) ? subMap.get(entry) : -1;
		
		if(ret == -1 && !growthStopped) {
			subMap.put(entry, numEntries);
			ret = numEntries;
			idToGid.add(gid);
			numEntries++;
		}
		
		return new Pair<Integer, Integer>(ret, gid);
	}
	
	public int size() {
		return numEntries;
	}
	
	public int sizeOfGiven() {
		return numGivens;
	}

	public void stopGrowth() {
		growthStopped = true;
		map.compact();
		calcSizeOfGiven();
	}
	
	public Set<Entry<String, TObjectIntHashMap<String>>> entrySet() {
		return map.entrySet();
	}
	
	public int getGidFromIndex(int index) {
		return idToGid.get(index);
	}
	
	public int getSizeOfFeatFromGid(int gid) {
		return sizes.get(gid);
	}
	
	public void display(PrintStream printer, Parameters params) {
		List<String> givens = new ArrayList<String>(map.keySet());
		Collections.sort(givens);
		for(String given : givens) {
			TObjectIntHashMap<String> subMap = map.get(given);
			int gid = givenMap.get(given);
			printer.println("num of feat: " + sizes.get(gid));
			printer.println("nil: " + params.nilAt(gid));
			List<String> feats = new ArrayList<String>(subMap.keySet());
			Collections.sort(feats);
			for(Object feat : feats) {
				int index = subMap.get(feat);
				printer.println(feat + "|" + given + ": " + params.paramAt(index));
			}
			printer.println("------------------");
		}
		printer.flush();
		printer.close();
	}
	
	// Serialization

	private static final long serialVersionUID = 1;

	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		//write version
		out.writeInt(CURRENT_SERIAL_VERSION);
		//write feature map
		out.writeInt(numEntries);
		out.writeObject(map);
		//write given map
		out.writeInt(numGivens);
		out.writeObject(givenMap);
		//write id to gid list
		out.writeObject(idToGid);
		//write sizes for each gid
		out.writeObject(sizes);
		//write growthStopped
		out.writeBoolean(growthStopped);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		if(version != CURRENT_SERIAL_VERSION) {
			System.err.println("version error:");
			System.err.println("current version: " + CURRENT_SERIAL_VERSION);
			System.err.println("model version: " + version);
			System.exit(1);
		}
		//read feature map
		numEntries = in.readInt();
		map = (TCustomHashMap<String, TObjectIntHashMap<String>>) in.readObject();
		//read given map
		numGivens = in.readInt();
		givenMap = (TObjectIntHashMap<String>) in.readObject();
		//read id to gid list
		idToGid = (TIntArrayList) in.readObject();
		//read sizes for each gid
		sizes = (TIntArrayList) in.readObject();
		//read growthStopped
		growthStopped = in.readBoolean();
	}

}
