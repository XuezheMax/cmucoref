package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;

import cmucoref.io.ObjectReader;
import cmucoref.io.ObjectWriter;
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
	
	private int V = 0;
	
	public CorefModel(Options options){
		this.options = options;
		mentionAlphabet = new Alphabet();
		if(options.useEventFeature()) {
			eventAlphabet = new AdAlphabet(10000000);
		}
	}
	
	public void createParameters(int sizeOfEvent) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		this.V = sizeOfEvent * 2 + 2;
		ParameterInitializer initializer = (ParameterInitializer) Class.forName(options.getParamInitializer()).newInstance();
		initializer.setModel(this);
		mentionParams = new Parameters(mentionFeatureSize(), givenSizeofMention(), initializer, true);
		if(options.useEventFeature()) {
			eventParams = new Parameters(eventFeatureSize(), givenSizeofEvent(), initializer, false);
		}
	}
	
	public void closeAlphabets() {
		mentionAlphabet.stopGrowth();
		if(options.useEventFeature()) {
			eventAlphabet.stopGrowth();
		}
	}
	
	public double getScore(FeatureVector mfv, FeatureVector efv){
		return getMentionScore(mfv) + getEventScore(efv);
	}
	
	private double getMentionScore(FeatureVector mfv) {
		return mentionParams.getScoreWithNil(mfv);
	}
	
	private double getEventScore(FeatureVector efv) {
		return (efv == null || efv.size() == 0) ? 0.0 : eventParams.getScoreWithNilAndUniGram(efv);
	}
	
	public void updateMentionParams(int index, double val) {
		mentionParams.updateParam(index, val);
	}
	
	public void updateMentionNils(int gid, double val) {
		mentionParams.updateNil(gid, val);
	}
	
	public void updateMentionNil(double nil) {
		mentionParams.updateNil(nil);
	}
	
	public void updateEventParams(int index, double val) {
		eventParams.updateParam(index, val);
	}
	
	public void updateEventNils(int gid, double val) {
		eventParams.updateNil(gid, val);
	}
	
	public void updateEventUniParams(int index, double val) {
		eventParams.updateUniParam(index, val);
	}
	
	public void updateEventNil(double nil) {
		eventParams.updateNil(nil);
	}
	
	public void updateEventUni_nil(double uni_nil) {
		eventParams.updateUni_Nil(uni_nil);
	}
	
	public double mentionParamAt(int index) {
		return mentionParams.paramAt(index);
	}
	
	public double mentionNilAt(int gid) {
		return mentionParams.nilAt(gid);
	}
	
	public double eventParamAt(int index) {
		return eventParams.paramAt(index);
	}
	
	public double eventNilAt(int gid) {
		return eventParams.nilAt(gid);
	}
	
	public double eventNil() {
		return eventParams.nil();
	}
	
	public double eventUniParamAt(int eid) {
		return eventParams.uniParamAt(eid);
	}
	
	//get mention gid
	public int getMentionGid(String given) {
	    return mentionAlphabet.lookupGivenIndex(given);
	}
	//get mention feature index
	public Pair<Integer, Integer> getMentionFeatureIndex(String feat, String given) {
		return mentionAlphabet.lookupIndex(feat, given);
	}
	
	//get event feature index
	public Pair<Integer, Integer> getEventFeatureIndex(String feat, String given) {
		return eventAlphabet.lookupIndex(feat, given);
	}
	
	//get mention gid from index
	public int getMentionGidFromIndex(int index) {
		return mentionAlphabet.getGidFromIndex(index);
	}
	
	//get event gid from index
	public int getEventGidFromIndex(int index) {
		return eventAlphabet.getGidFromIndex(index);
	}
	
	//get event id from index
	public int getEventIdFromIndex(int index) {
		if(index == -1) {
			throw new RuntimeException("index cannot be -1");
		}
		
		if(index < 0) {
			return -index - 2;
		}
		else {
			return ((AdAlphabet) eventAlphabet).getEventIdFromIndex(index);
		}
	}
	
	//get mention feature size from gid
	public int getSizeOfMentionFeatFromGid(int gid) {
		return mentionAlphabet.getSizeOfFeatFromGid(gid);
	}
	
	//get event feature size from gid
	public int getSizeOfEventFeatFromGid(int gid) {
		return eventAlphabet.getSizeOfFeatFromGid(gid);
	}
	
	//thread number
	public int threadNum() {
		return options.getThreadNum();
	}

	//get event size
	public int eventV() {
		return V;
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
	
	//save current parameter to file
	public void saveCurrentParams(String file) throws IOException {
		ObjectWriter out = new ObjectWriter(file);
		out.writeObject(mentionParams);
		out.writeObject(eventParams);
		out.close();
	}
	
	//load parameter form file
	public void loadParams(String file) throws ClassNotFoundException, IOException {
		ObjectReader in = new ObjectReader(file);
		mentionParams = (Parameters) in.readObject();
		eventParams = (Parameters) in.readObject();
		in.close();
	}
	
	public void displayMentionAlphabet(PrintStream printer) {
		mentionAlphabet.display(printer, this.mentionParams);
	}
	
	public void displayEventAlphabet(PrintStream printer) {
		eventAlphabet.display(printer, this.eventParams);		
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(V);
		out.writeObject(options);
		out.writeObject(mentionAlphabet);
		out.writeObject(mentionParams);
		out.writeObject(eventAlphabet);
		out.writeObject(eventParams);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		V = in.readInt();
		options = (Options) in.readObject();
		mentionAlphabet = (Alphabet) in.readObject();
		mentionParams = (Parameters) in.readObject();
		eventAlphabet = (Alphabet) in.readObject();
		eventParams = (Parameters) in.readObject();
	}
}
