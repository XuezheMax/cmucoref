package cmucoref.model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cmucoref.util.Pair;
import cmucoref.util.Util;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

public class AdAlphabet extends Alphabet {
	
	protected TIntArrayList eFeatId2EventId = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AdAlphabet(int capacity) {
		super(capacity);
		eFeatId2EventId = new TIntArrayList(capacity);
	}
	
	public AdAlphabet() {
		super();
	}

	public Pair<Integer, Integer> lookupIndex(String entry, String given) {
		int gid = lookupGivenIndex(given);
		int eid = lookupGivenIndex(entry);
		if(gid == -1) {
			return new Pair<Integer, Integer>(-(eid + 2), -1);
		}
		
		if(entry == null) {
			return new Pair<Integer, Integer>(-1, gid);
		}
		
		TObjectIntHashMap<String> subMap = map.containsKey(given) ? map.get(given) : null;
		if(subMap == null) {
			subMap = new TObjectIntHashMap<String>();
			map.put(given, subMap);
		}
		
		
		int ret = subMap.containsKey(entry) ? subMap.get(entry) : -1;
		
		if(ret == -1 && !growthStopped) {
			subMap.put(entry, numEntries);
			ret = numEntries;
			idToGid.add(gid);
			eFeatId2EventId.add(eid);
			numEntries++;
		}
		
		if(ret == -1) {
			ret = -(eid + 2);
		}
		
		return new Pair<Integer, Integer>(ret, gid);
	}
	
	public int getEventIdFromIndex(int index) {
		return eFeatId2EventId.get(index);
	}
	
	@Override
	public void display(PrintStream printer, Parameters params) {
		List<String> givens = new ArrayList<String>(givenMap.keySet());
		Collections.sort(givens);
		printer.println("unigram probability: " + givens.size());
		printer.println("uni_nil: " + params.uni_nil() + ", nil|nil: " + Util.logsumexp(params.nil(), params.uni_nil()));
		for(String given : givens) {
			int gid = givenMap.get(given);
			printer.println(given + ": " + params.uniParamAt(gid) + ", " + given + "|nil: " + Util.logsumexp(params.nil(), params.uniParamAt(gid)));
		}
		printer.println("------------------");
		printer.println("bigram probability:");
		printer.println("nil: " + params.nil() + ", nil|nil: " + Util.logsumexp(params.nil(), params.uni_nil()));
		super.display(printer, params);
	}
}
