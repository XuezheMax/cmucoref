package cmucoref.model;

import gnu.trove.list.linked.TLinkedList;

@SuppressWarnings("rawtypes")
public class FeatureVector extends TLinkedList {
	
	public FeatureVector(){}
	
	@SuppressWarnings("unchecked")
	public FeatureVector(int[] keys){
		for(int i = 0; i < keys.length; ++i){
			this.add(new Feature(keys[i], 1.0));		
		}
	}
	
	@SuppressWarnings("unchecked")
	public void add(int index, double value){
		this.add(new Feature(index, value));
	}

	public int[] keys(){
		int size = this.size();
		int[] keys = new int[size];
		int i = 0;
		for(Object b : this){
			Feature f = (Feature)(b);
			keys[i++] = f.index;
		}
		return keys;
	}
	
	public final double getScore(double[] params){
		double score = 0.0;
		
		for(Object b : this){
			Feature f = (Feature)(b);
			score += params[f.index] * f.value;
		}
		return score;
	}
	
	public void update(double[] parameters, double val){
		for(Object b : this){
			Feature f = (Feature)(b);
			parameters[f.index] += val * f.value;
		}
	}
}
