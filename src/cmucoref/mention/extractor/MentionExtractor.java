package cmucoref.mention.extractor;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.relationextractor.*;
import cmucoref.model.Options;
import cmucoref.util.Pair;
import cmucoref.mention.SpeakerInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import edu.stanford.nlp.dcoref.Dictionaries;

public abstract class MentionExtractor {
	
	protected Dictionaries dict;
	
	public MentionExtractor(){}
	
	public void createDict(String propfile) throws FileNotFoundException, IOException{
		Properties props = new Properties();
		props.load(new FileInputStream(propfile));
		this.dict = new Dictionaries(props);
	}
	
	public Dictionaries getDict(){
		return dict;
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
					}
				}
			}
		}
		mentions.removeAll(remove);
		
		//remove single number named entity mentions
		remove.clear();
		String[] NUMBERS = {"NUMBER", "ORDINAL", "CARDINAL", "MONEY", "QUANTITY"};
		HashSet<String> numberNER = new HashSet<String>(Arrays.asList(NUMBERS));
		for(Mention mention : mentions){
			if(mention.endIndex - mention.startIndex == 1){
				if(numberNER.contains(mention.headword.ner)){
					remove.add(mention);
				}
			}
		}
		mentions.removeAll(remove);
	}
	
	protected void deleteSpuriousPronominalMentions(List<Mention> mentions, Sentence sent) {
		//remove you know mentions
		Set<Mention> remove = new HashSet<Mention>();
		for(Mention mention : mentions) {
			if(mention.isPronominal() 
				&& (mention.endIndex - mention.startIndex == 1) 
				&& mention.headString.equals("you")) {
				if(mention.headIndex + 1 < sent.length()) {
					Lexicon lex = sent.getLexicon(mention.headIndex + 1);
					if(lex.form.equals("know")) {
						remove.add(mention);
					}
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
		
		//re-assign mention ID;
		for(int i = 0; i < allMentions.size(); ++i){
			Mention mention = allMentions.get(i);
			mention.mentionID = i;
		}
		
		//find speaker for each mention
		findSpeakers(doc, allMentions, mentionList);
		
		if(options.usePreciseMatch()){
			findPreciseMatchRelation(doc, allMentions);
		}
		
		return allMentions;
	}
	
	/**
	 * 
	 * @param doc
	 * @param allMentions
	 */
	protected void findSpeakers(Document doc, List<Mention> allMentions, List<List<Mention>> mentionList) {
		SpeakerInfo.reset();
		Map<String, SpeakerInfo> speakersMap = new HashMap<String, SpeakerInfo>();
		speakersMap.put("<DEFAULT_SPEAKER>", new SpeakerInfo("<DEFAULT_SPEAKER>"));
		// find default speakers from the speaker tags of document doc
		findDefaultSpeakers(doc, allMentions, speakersMap);
		
	}
	
	protected void findQuotationSpeakers(Document doc, List<Mention> allMentions, List<List<Mention>> mentionList, 
			Dictionaries dict, Map<String, SpeakerInfo> speakersMap, 
			Pair<Integer, Integer> beginQuotation, Pair<Integer, Integer> endQuotation) {
		Sentence sent = doc.getSentence(beginQuotation.first);
		List<Mention> mentions = mentionList.get(beginQuotation.first);
		SpeakerInfo speakerInfo = findQuotationSpeaker(sent, mentions, 1, beginQuotation.second, dict, speakersMap);
		if(speakerInfo != null) {
			
			return;
		}
		
		sent = doc.getSentence(endQuotation.first);
		mentions = mentionList.get(endQuotation.first);
		speakerInfo = findQuotationSpeaker(sent, mentions, endQuotation.second + 1, sent.length(), dict, speakersMap);
		if(speakerInfo != null) {
			
			return;
		}
		
		
	}
	
	protected SpeakerInfo findQuotationSpeaker(Sentence sent, List<Mention> mentions, 
			int startIndex, int endIndex, Dictionaries dict, Map<String, SpeakerInfo> speakersMap) {
		for(int i = endIndex - 1; i >= startIndex; --i) {
			String lemma = sent.getLexicon(i).lemma;
			if(dict.reportVerb.contains(lemma)) {
				int reportVerbPos = i;
				for(int j = startIndex; j < endIndex; ++j) {
					Lexicon lex = sent.getLexicon(j);
					if(lex.collapsed_head == reportVerbPos && lex.collapsed_deprel.equals("nsubj")) {
						int speakerHeadIndex = j;
						for(Mention mention : mentions) {
							if(mention.headIndex == speakerHeadIndex 
								&& mention.startIndex >= startIndex && mention.endIndex < endIndex) {
								if(mention.utteranceInfo == null) {
									String speakerKey = mention.getSpan(sent);
									SpeakerInfo speakerInfo = new SpeakerInfo(speakerKey);
									speakersMap.put(speakerKey, speakerInfo);
									speakerInfo.setSpeaker(mention);
									mention.utteranceInfo = speakerInfo;
								}
								return mention.utteranceInfo;
							}
						}
						return new SpeakerInfo(sent.getLexicon(speakerHeadIndex).form);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * find default speakers from the speaker tags of document
	 * @param doc
	 * @param allMentions
	 * @param speakersMap
	 */
	protected void findDefaultSpeakers(Document doc, List<Mention> allMentions, Map<String, SpeakerInfo> speakersMap) {
		for(Mention mention : allMentions) {
			Sentence sent = doc.getSentence(mention.sentID);
			String speaker = sent.getSpeaker().equals("-") ? "<DEFAULT_SPEAKER>" : sent.getSpeaker();
			SpeakerInfo speakerInfo = speakersMap.get(speaker);
			if(speakerInfo == null) {
				speakerInfo = new SpeakerInfo(speaker);
				speakersMap.put(speaker, speakerInfo);
			}
			mention.speakerInfo = speakerInfo;
			speakerInfo.addMention(mention);
		}
	}
	
	protected void findPreciseMatchRelation(Document doc, List<Mention> allMentions) {
		for(int i = 1; i < allMentions.size(); ++i){
			Mention anaph = allMentions.get(i);
			//find local attribute match
			for(int j = i - 1; j >=0; --j){
				Mention antec = allMentions.get(j);
				if(anaph.ruleout(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), dict)){
					continue;
				}
				
				int distOfSent = anaph.getDistOfSent(antec);
				if(antec.isPronominal()) {
					if(distOfSent > 5) {
						continue;
					}
				}
				else {
					if(distOfSent > 1) {
						continue;
					}
				}
				
				anaph.localAttrMatch = antec;
				break;
			}
			
			//find precise match
			for(int j = 0; j < i; ++j){
				Mention antec = allMentions.get(j);
				
				if(anaph.ruleout(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), dict)){
					continue;
				}
				
				if(anaph.preciseMatch(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), dict)){
					anaph.addPreciseMatch(antec, doc);
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
	
	public void displayMention(Sentence sent, Mention mention, PrintStream printer){
		mention.display(sent, printer);
	}
}
