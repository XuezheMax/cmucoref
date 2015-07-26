package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;

import cmucoref.model.params.ParameterInitializer;
import cmucoref.util.Pair;

public class CorefModel implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Parameters params = null;
	private Parameters eventParams = null;
	public Options options = null;
	private Alphabet featAlphabet = null;
	private Alphabet eventAlphabet = null;
	
	public CorefModel(Options options){
		this.options = options;
		featAlphabet = new Alphabet();
		if(options.useEventFeature()) {
			eventAlphabet = new Alphabet(100000000);
		}
	}
	
	public void createParameters() throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		ParameterInitializer initializer = (ParameterInitializer) Class.forName(options.getParamInitializer()).newInstance();
		initializer.setModel(this);
		params = new Parameters(featAlphabet.size(), initializer);
	}
	
	public void closeAlphabets(){
		featAlphabet.stopGrowth();
	}
	
	public double getScore(FeatureVector fv){
		return params.getScore(fv);
	}
	
	public void update(int index, double val){
		params.update(index, val);
	}
	
	public double paramAt(int index){
		return params.paramAt(index);
	}
	
	//get feature index
	public Pair<Integer, Integer> getFeatureIndex(String feat, String given){
		return featAlphabet.lookupIndex(feat, given);
	}
	
	public int getGidFromIndex(int index){
		return featAlphabet.getGidFromIndex(index);
	}
	
	//thread number
	public int threadNum(){
		return options.getThreadNum();
	}

	//get given size
	public int givenSize(){
		return featAlphabet.sizeOfGiven();
	}
	
	//get feature size
	public int featureSize(){
		return featAlphabet.size();
	}
	
	public void displayAlphabet(PrintStream printer){
		featAlphabet.display(printer, this.params);
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
