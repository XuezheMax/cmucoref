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
	
	private Parameters mentionParams = null;
	private Parameters eventParams = null;
	public Options options = null;
	private Alphabet mentionAlphabet = null;
	private Alphabet eventAlphabet = null;
	
	private int sizeOfEvent = 0;
	
	public CorefModel(Options options){
		this.options = options;
		mentionAlphabet = new Alphabet();
		if(options.useEventFeature()) {
			eventAlphabet = new Alphabet(100000000);
		}
	}
	
	public void createParameters(int sizeOfEvent) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		this.sizeOfEvent = sizeOfEvent;
		ParameterInitializer initializer = (ParameterInitializer) Class.forName(options.getParamInitializer()).newInstance();
		initializer.setModel(this);
		mentionParams = new Parameters(mentionAlphabet.size(), initializer, true);
		if(options.useEventFeature()) {
			eventParams = new Parameters(eventAlphabet.size(), initializer, false);
		}
	}
	
	public void closeAlphabets(){
		mentionAlphabet.stopGrowth();
		eventAlphabet.stopGrowth();
	}
	
	public double getScore(FeatureVector mfv, FeatureVector efv){
		return getMentionScore(mfv) + getEventScore(efv);
	}
	
	private double getMentionScore(FeatureVector mfv) {
		return mentionParams.getScore(mfv);
	}
	
	private double getEventScore(FeatureVector efv) {
		return efv == null ? 0.0 : eventParams.getScore(efv);
	}
	
	public void updateMentionParams(int index, double val) {
		mentionParams.update(index, val);
	}
	
	public void updateEventParams(int index, double val) {
		eventParams.update(index, val);
	}
	
	public double mentionParamAt(int index) {
		return mentionParams.paramAt(index);
	}
	
	public double eventParamAt(int index) {
		return eventParams.paramAt(index);
	}
	
	//get mention feature index
	public Pair<Integer, Integer> getMentionFeatureIndex(String feat, String given) {
		return mentionAlphabet.lookupIndex(feat, given);
	}
	
	//get event feature index
	public Pair<Integer, Integer> getEventFeatureIndex(String feat, String given) {
		return eventAlphabet.lookupIndex(feat, given);
	}
	
	public int getMentionGidFromIndex(int index) {
		return mentionAlphabet.getGidFromIndex(index);
	}
	
	public int getEventGidFromIndex(int index) {
		return eventAlphabet.getGidFromIndex(index);
	}
	
	//thread number
	public int threadNum() {
		return options.getThreadNum();
	}

	//get event size
	public int sizeOfEvent() {
		return sizeOfEvent;
	}
	
	//get mention given size
	public int givenSizeofMention() {
		return mentionAlphabet.sizeOfGiven();
	}
	
	//get event given size
	public int givenSizeofEvent() {
		return eventAlphabet.sizeOfGiven();
	}
	
	//get mention feature size
	public int mentionFeatureSize() {
		return mentionAlphabet.size();
	}
	
	//get event feature size
	public int eventFeatureSize() {
		return eventAlphabet.size();
	}
	
	public void displayMentionAlphabet(PrintStream printer) {
		mentionAlphabet.display(printer, this.mentionParams);
	}
	
	public void displayEventAlphabet(PrintStream printer) {
		eventAlphabet.display(printer, this.eventParams);		
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(sizeOfEvent);
		out.writeObject(options);
		out.writeObject(mentionAlphabet);
		out.writeObject(mentionParams);
		out.writeObject(eventAlphabet);
		out.writeObject(eventParams);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		sizeOfEvent = in.readInt();
		options = (Options) in.readObject();
		mentionAlphabet = (Alphabet) in.readObject();
		mentionParams = (Parameters) in.readObject();
		eventAlphabet = (Alphabet) in.readObject();
		eventParams = (Parameters) in.readObject();
	}
}
