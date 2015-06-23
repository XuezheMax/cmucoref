package cmucoref.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

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
	
	private void postProcessing(List<List<Mention>> mentionList) {
		for(List<Mention> mentions : mentionList) {
			for(Mention mention : mentions) {
				boolean removed = false;
				//remove appositives
				if(mention.corefTo(mention.getApposTo())) {
					Mention antec = mention.getApposTo();
					antec.corefCluster.remove(mention);
					removed = true;
				}
				
				//remove predicate nominatives
				if(mention.corefTo(mention.getPredNomiTo())) {
					Mention antec = mention.getPredNomiTo();
					antec.corefCluster.remove(mention);
					removed = true;
				}
				
				//remove role appositives
				if(mention.corefTo(mention.getRoleApposTo())) {
					Mention antec = mention.getRoleApposTo();
					antec.corefCluster.remove(mention);
					removed = true;
				}
				
				if(removed) {
					mention.setSingleton();
				}
			}
		}
		
		for(List<Mention> mentions : mentionList) {
			for(Mention mention : mentions) {
				if(mention.isSingleton()) {
					mention.corefCluster = null;
				}
			}
		}
	}
	
	public void assignCorefClustersToDocument(List<List<Mention>> mentionList, boolean postProcessing) {
		if(postProcessing) {
			postProcessing(mentionList);
		}
		
		int i = 0;
		for(List<Mention> mentions : mentionList){
			Sentence sent = sentences.get(i);
			for(Lexicon lex : sent.getLexicons()){
				lex.corefLabel = "-";
			}
			
			Collections.sort(mentions, Mention.postTreeOrderComparator);
			
			for(Mention mention : mentions) {
				if(mention.corefCluster == null) {
					continue;
				}
				
				int clusterID = mention.corefCluster.clusterID;
				int startIndex = mention.startIndex;
				int endIndex = mention.endIndex - 1;
				if(startIndex == endIndex){
					Lexicon lexicon = sent.getLexicon(startIndex);
					if(lexicon.corefLabel.equals("-")){
						lexicon.corefLabel = "(" + clusterID + ")";
					}
					else{
						System.err.println("order error");
						mention.display(sent, System.err);
						System.exit(0);
					}
				}
				else{
					Lexicon startLex = sent.getLexicon(startIndex);
					Lexicon endLex = sent.getLexicon(endIndex);
					if(startLex.corefLabel.equals("-")){
						startLex.corefLabel = "(" + clusterID;
					}
					else{
						startLex.corefLabel = "(" + clusterID + "|" + startLex.corefLabel;
					}
					
					if(endLex.corefLabel.equals("-")){
						endLex.corefLabel = clusterID + ")";
					}
					else{
						endLex.corefLabel = endLex.corefLabel + "|" + clusterID + ")";
					}
				}
			}
			i++;
		}
	}
	
	public void getCorefClustersFromDocument(List<List<Mention>> mentionList) throws MentionException {
		HashMap<Integer, Mention> clusterIDMap = new HashMap<Integer, Mention>();
		int i = 0;
		for(List<Mention> mentions : mentionList){
			HashMap<String, Mention> indexMap = new HashMap<String, Mention>();
			for(Mention mention : mentions){
				indexMap.put(mention.startIndex + " " + mention.endIndex, mention);
			}
			
			Stack<Pair<Integer, Integer>> clusterStack = new Stack<Pair<Integer, Integer>>();
			Sentence sent = sentences.get(i);
			for(Lexicon lex : sent.getLexicons()){
				if(lex.corefLabel.equals("-")){
					continue;
				}
				
				String[] tokens = lex.corefLabel.split("\\|");
				ArrayList<Pair<Integer, Integer>> waitList = new ArrayList<Pair<Integer, Integer>>();
				for(String token : tokens){
					if(token.startsWith("(")){
						if(token.endsWith(")")){
							int clusterId = Integer.parseInt(token.substring(1, token.length() - 1));
							Mention current = indexMap.get(lex.id + " " + (lex.id + 1));
							if(current == null){
								continue;
							}
							Mention antec = clusterIDMap.get(clusterId);
							if(antec == null){
								current.setRepres();
							}
							else{
								current.setAntec(antec);
							}
							clusterIDMap.put(clusterId, current);
						}
						else{
							int clusterId = Integer.parseInt(token.substring(1));
							clusterStack.push(new Pair<Integer, Integer>(lex.id, clusterId));
						}
					}
					else if(token.endsWith(")")){
						int clusterId = Integer.parseInt(token.substring(0, token.length() - 1));
						Pair<Integer, Integer> pair = null;
						for(Pair<Integer, Integer> pp : waitList){
							if(pp.second == clusterId){
								pair = pp;
								break;
							}
						}
						if(pair != null){
							waitList.remove(pair);
						}
						else{
							pair = clusterStack.pop();
							while(pair.second != clusterId){
								waitList.add(pair);
								pair = clusterStack.pop();
							}
						}
						
						pair.second = lex.id + 1;
						Mention current = indexMap.get(pair.first + " " + pair.second);
						if(current == null){
							continue;
						}
						Mention antec = clusterIDMap.get(clusterId);
						if(antec == null){
							current.setRepres();
						}
						else{
							current.setAntec(antec);
						}
						clusterIDMap.put(clusterId, current);
					}
					else{
						throw new MentionException("coreference label error: " + token+ "\n"
								+ this.getFileName() + " " + "part: " + this.docId + " sentId: " + i);
					}
				}
				for(int j = waitList.size() - 1; j >= 0; --j){
					clusterStack.push(waitList.get(j));
				}
			}
			
			if(!clusterStack.isEmpty()){
				throw new MentionException("stack is not empty");
			}
			
			i++;
		}
	}
}
