package cmucoref.document;

import java.util.ArrayList;

public class Document {
	private ArrayList<Sentence> sentences = null;
	private String filename = null;
	private int docId = 0;
	
	public Document(){
		
	}
	
	public Document(ArrayList<Sentence> sentences, String filename, int docId){
		this.sentences = sentences;
		this.filename = filename;
		this.docId = docId;
	}
	
	public int size(){
		return sentences.size();
	}
	
	public ArrayList<Sentence> getSentences(){
		return sentences;
	}
	
	public Sentence getSentence(int index){
		return sentences.get(index);
	}
	
	public String getFileName(){
		return filename;
	}
	
	public int getDocId(){
		return docId;
	}
	
	public void setSentences(ArrayList<Sentence> sentences){
		this.sentences = sentences;
	}
	
	public void setSentence(Sentence sentence, int index){
		sentences.set(index, sentence);
	}
	
	public void setFileName(String filename){
		this.filename = filename;
	}
	
	public void setDocId(int docId){
		this.docId = docId;
	}
}
