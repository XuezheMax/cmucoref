package cmucoref.model;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cmucoref.util.Pair;
import cmucoref.util.trove.EqualsHashingStrategy;

public class Alphabet implements Serializable{
	/**
	 * 
	 */

	private TCustomHashMap<String, TObjectIntHashMap<String>> map;
	private TObjectIntHashMap<String> givenMap;
	private TIntArrayList idToGid;
	
	private int numEntries;
	private int numGivens;

	private boolean growthStopped;

	public Alphabet(int capacity) {
		this.map = new TCustomHashMap<String, TObjectIntHashMap<String>>(new EqualsHashingStrategy<String>(), capacity);
		this.givenMap = new TObjectIntHashMap<String>(10000);
		idToGid = new TIntArrayList(100000);
		numEntries = 0;
		numGivens = 0;
		growthStopped = false;
	}

	public Alphabet() {
		this(100000);
	}
	
	public int lookupGivenIndex(String given){
		if (given == null) {
			throw new IllegalArgumentException("Can't lookup \"null\" in an Alphabet.");
		}
		
		int ret = givenMap.containsKey(given) ? givenMap.get(given) : -1;
		
		if(ret == -1 && !growthStopped){
			ret = numGivens;
			givenMap.put(given, ret);
			numGivens++;
		}
		return ret;
	}
	
	public Pair<Integer, Integer> lookupIndex(String entry, String given) {
		if (entry == null || given == null) {
			throw new IllegalArgumentException("Can't lookup \"null\" in an Alphabet.");
		}
		
		int gid = lookupGivenIndex(given);
		if(gid == -1){
			return null;
		}
		
		TObjectIntHashMap<String> subMap = map.containsKey(given) ? map.get(given) : null;
		if(subMap == null){
			subMap = new TObjectIntHashMap<String>();
			map.put(given, subMap);
		}
		
		int ret = subMap.containsKey(entry) ? subMap.get(entry) : -1;
		
		if(ret == -1 && !growthStopped){
			subMap.put(entry, numEntries);
			ret = numEntries;
			idToGid.add(gid);
			numEntries++;
		}
		
		if(ret != -1){
			return new Pair<Integer, Integer>(ret, gid);
		}
		else{
			return null;
		}
	}
	
	public int size() {
		return numEntries;
	}
	
	public int sizeOfGiven(){
		return numGivens;
	}

	public void stopGrowth() {
		growthStopped = true;
		map.compact();
	}
	
	public int getGidFromIndex(int index){
		return idToGid.get(index);
	}
	
	public void display(PrintStream printer, Parameters params){
		List<String> givens = new ArrayList<String>(map.keySet());
		Collections.sort(givens);
		for(String given : givens){
			TObjectIntHashMap<String> subMap = map.get(given);
			List<String> feats = new ArrayList<String>(subMap.keySet());
			Collections.sort(feats);
			for(Object feat : feats){
				int index = subMap.get(feat);
				printer.println(feat + "|" + given + ": " + params.paramAt(index));
			}
			printer.println("------------------");
		}
		printer.flush();
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
