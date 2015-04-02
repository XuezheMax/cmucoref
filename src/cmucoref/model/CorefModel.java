package cmucoref.model;

import java.io.Serializable;

public class CorefModel implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Parameters params = null;
	public Options options = null;
	private Alphabet featAlphabet = null;
	
	public CorefModel(Options options){
		this.options = options;
		featAlphabet = new Alphabet();
	}
	
	public void createParameters(){
		params = new Parameters(featAlphabet.size());
	}
	
	public void closeAlphabets(){
		featAlphabet.stopGrowth();
	}
	
	public double getScore(FeatureVector fv){
		return params.getScore(fv);
	}
	
	public void update(FeatureVector fv, double val){
		params.update(fv, val);
	}
	
	public double paramAt(int index){
		return params.paramAt(index);
	}
	
	//get feature index
	public int getFeatureIndex(String feat, String given){
		return featAlphabet.lookupIndex(feat, given);
	}

	//get feature size
	public int featureSize(){
		return featAlphabet.size();
	}
}
