package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
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
	
	//thread number
	public int threadNum(){
		return options.getThreadNum();
	}

	//get feature size
	public int featureSize(){
		return featAlphabet.size();
	}
	
	public void displayAlphabet(PrintWriter printer){
		featAlphabet.display(printer);
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject(options);
		out.writeObject(featAlphabet);
		out.writeObject(params);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		options = (Options) in.readObject();
		featAlphabet = (Alphabet) in.readObject();
		params = (Parameters) in.readObject();
	}
}
