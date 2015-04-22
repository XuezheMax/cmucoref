package cmucoref.model;

import cmucoref.util.Pair;
import gnu.trove.list.linked.TLinkedList;

@SuppressWarnings("rawtypes")
public class FeatureVector extends TLinkedList {
	
	public FeatureVector(){}
	
	@SuppressWarnings("unchecked")
	public FeatureVector(int[] keys, int[] gids){
		for(int i = 0; i < keys.length; ++i){
			this.add(new Feature(keys[i], gids[i]));		
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addFeature(int index, int gid){
		this.add(new Feature(index, gid));
	}

	public Pair<int[], int[]> keys(){
		int size = this.size();
		int[] keys = new int[size];
		int[] gids = new int[size];
		int i = 0;
		for(Object b : this){
			Feature f = (Feature)(b);
			keys[i] = f.index;
			gids[i++] = f.gid;
		}
		return new Pair<int[], int[]>(keys, gids);
	}
	
	public final double getScore(double[] params){
		if(this.size() == 0){
			return 0.0;
		}
		
		double score = 1.0;
		
		for(Object b : this){
			Feature f = (Feature)(b);
			score *= params[f.index];
		}
		return score;
	}
	
	public void update(double[] parameters, double val){
		for(Object b : this){
			Feature f = (Feature)(b);
			parameters[f.index] += val;
		}
	}
}
