package cmucoref.mention.extractor;

import cmucoref.document.Document;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.relationextractor.*;
import cmucoref.model.Options;
import cmucoref.util.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Properties;

import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;

public abstract class MentionExtractor {
	
	protected Dictionaries dict;
	
	public MentionExtractor(){}
	
	public void createDict(String propfile) throws FileNotFoundException, IOException{
		Properties props = new Properties();
		props.load(new FileInputStream(propfile));
		this.dict = new Dictionaries(props);
	}
	
	public abstract List<List<Mention>> extractPredictedMentions(Document doc, Options options) throws IOException;
	
	protected void deleteSpuriousNamedEntityMentions(List<Mention> mentions, Sentence sent){
		//remove overlap mentions
		Set<Mention> remove = new HashSet<Mention>();
		for(Mention mention1 : mentions){
			if(mention1.isPureNerMention(sent)){
				for(Mention mention2 : mentions){
					if(mention1.overlap(mention2)){
						remove.add(mention1);
						
//						if(mention1.startIndex > mention2.startIndex){
//							mention2.endIndex = mention1.endIndex;
//						}
//						else{
//							mention2.startIndex = mention1.startIndex;
//							mention2.headIndex = mention1.headIndex;
//							mention2.process(sent, dict);
//						}
					}
				}
			}
		}
		mentions.removeAll(remove);
		
		//remove single number named entity mentions
		remove.clear();
		for(Mention mention : mentions){
			if(mention.endIndex - mention.startIndex == 1){
				if(mention.headword.ner.equals("NUMBER") || mention.headword.ner.equals("ORDINAL") || mention.headword.ner.equals("CARDINAL")){
					remove.add(mention);
				}
			}
		}
		mentions.removeAll(remove);
	}
	
	public List<Mention> getSingleMentionList(Document doc, List<List<Mention>> mentionList, Options options){
		List<Mention> allMentions = new ArrayList<Mention>();
		for(List<Mention> mentions : mentionList){
			allMentions.addAll(mentions);
		}
		
		if(options.useSpanMatch()){
			findSpanMatchRelation(doc, allMentions);
		}
		
		return allMentions;
	}
	
	protected void findSpanMatchRelation(Document doc, List<Mention> allMentions){
		for(int i = 1; i < allMentions.size(); ++i){
			Mention anaph = allMentions.get(i);
			if(anaph.mentionType != MentionType.NOMINAL && anaph.mentionType != MentionType.PROPER){
				continue;
			}
			for(int j = i - 1; j >=0; --j){
				Mention antec = allMentions.get(j);
				if(antec.mentionType != MentionType.NOMINAL && antec.mentionType != MentionType.PROPER){
					continue;
				}
				if(anaph.cover(antec) || antec.cover(anaph)){
					continue;
				}
				
				boolean headMatch = anaph.headMatch(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID));
				if(headMatch){
					anaph.addSpanMatch(antec);
					break;
				}
			}
		}
	}
	
	protected void findSyntacticRelation(List<Mention> mentions, Sentence sent, Options options) throws InstantiationException, IllegalAccessException, ClassNotFoundException, MentionException{
		markListMemberRelation(mentions, sent, RelationExtractor.createExtractor(options.getListMemberRelationExtractor()));
		deleteSpuriousListMentions(mentions, sent);
		markAppositionRelation(mentions, sent, RelationExtractor.createExtractor(options.getAppositionRelationExtractor()));
		markPredicateNominativeRelation(mentions, sent, RelationExtractor.createExtractor(options.getPredicateNominativeRelationExtractor()));
		//markRelativePronounRelation(mentions, sent, RelationExtractor.createExtractor(options.getRelativePronounRelationExtractor()));
	}
	
	protected void deleteSpuriousListMentions(List<Mention> mentions, Sentence sent){
		Set<Mention> remove = new HashSet<Mention>();
		for(Mention mention1 : mentions){
			for(Mention mention2 : mentions){
				if(mention1.headIndex == mention2.headIndex && mention2.cover(mention1) && !mention1.isListMemberOf(mention2)){
					if(mention2.startIndex == mention1.startIndex 
							&& mention2.endIndex == mention1.endIndex + 1
							&& sent.getLexicon(mention1.endIndex).form.equals(",")){
						remove.add(mention2);
					}
					else {
						remove.add(mention1);
					}
				}
			}
		}
		mentions.removeAll(remove);
	}
	
	protected void markListMemberRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException{
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, foundPairs, "LISTMEMBER");
	}
	
	protected void markAppositionRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException{
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, foundPairs, "APPOSITION");
	}
	
	protected void markPredicateNominativeRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException{
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, foundPairs, "PREDICATE_NOMINATIVE");
	}
	
	protected void markRelativePronounRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException{
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, foundPairs, "RELATIVE_PRONOUN");
	}
	
	protected void markMentionRelation(List<Mention> mentions, Set<Pair<Integer, Integer>> foundPairs, String relation) throws MentionException{
		for(Mention mention1 : mentions){
			for(Mention mention2 : mentions){
				if(mention1.equals(mention2)){
					continue;
				}
				
				if(relation.equals("LISTMEMBER")){
					for(Pair<Integer, Integer> pair : foundPairs){
						if(pair.first == mention1.mentionID && pair.second == mention2.mentionID){
							mention2.addListMember(mention1);
						}
					}
				}
				else if(relation.equals("PREDICATE_NOMINATIVE")){
					for(Pair<Integer, Integer> pair : foundPairs){
						if(pair.first == mention1.mentionID && pair.second == mention2.mentionID){
							mention2.addPredicativeNominative(mention1);
						}
					}
				}
				else{
					for(Pair<Integer, Integer> pair : foundPairs){
						if(pair.first == mention1.headIndex && pair.second == mention2.headIndex){
							if(relation.equals("APPOSITION")){
								mention2.addApposition(mention1);
							}
							else if(relation.equals("RELATIVE_PRONOUN")){
								mention2.addRelativePronoun(mention1);
							}
							else{
								throw new MentionException("Unknown mention relation: " + relation);
							}
						}
					}
				}
			}
		}
	}
	
	public void displayMentions(Document doc, List<List<Mention>> mentionList, PrintStream printer){
		printer.println("#begin document " + doc.getFileName() + " docId " + doc.getDocId());
		int sentId = 0;
		for(List<Mention> mentions : mentionList){
			printer.println("sent Id: " + sentId);
			for(Mention mention : mentions){
				displayMention(doc.getSentence(sentId), mention, printer);
			}
			sentId++;
			printer.println("----------------------------------------");
		}
		printer.println("end document");
		printer.flush();
	}
	
	public void displayMention(Mention mention, PrintStream printer) {
		printer.println("#Begin Mention " + mention.mentionID);
		printer.println("sent ID: " + mention.sentID);
		printer.println("mention ID: " + mention.mentionID);
		printer.println(mention.startIndex + " " + mention.endIndex);
		printer.println("headIndex: " + mention.headIndex);
		printer.println("headString: " + mention.headString);
		printer.println("mention type: " + mention.mentionType);
		printer.println("mention gender: " + mention.gender);
		printer.println("mention number: " + mention.number);
		printer.println("mention animacy: " + mention.animacy);
		printer.println("mention person: " + mention.person);
		printer.println("#end Mention " + mention.mentionID);
		printer.println("===================================");
		printer.flush();
	}
	
	public void displayMention(Sentence sent, Mention mention, PrintStream printer){
		mention.display(sent, printer);
	}
}
