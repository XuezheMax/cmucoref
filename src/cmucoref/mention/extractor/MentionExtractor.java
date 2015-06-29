package cmucoref.mention.extractor;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.mention.eventextractor.EventExtractor;
import cmucoref.mention.extractor.relationextractor.*;
import cmucoref.model.Options;
import cmucoref.util.Pair;
import cmucoref.mention.SpeakerInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
		InputStream in = MentionExtractor.class.getClassLoader().getResourceAsStream(propfile);
		props.load(new InputStreamReader(in));
		this.dict = new Dictionaries(props);
	}
	
	public Dictionaries getDict(){
		return dict;
	}
	
	public abstract List<List<Mention>> extractPredictedMentions(Document doc, Options options) throws IOException;
	
	protected void deleteDuplicatedMentions(List<Mention> mentions, Sentence sent) {
		//remove duplicated mentions
		Set<Mention> remove = new HashSet<Mention>();
		for(int i = 0; i < mentions.size(); ++i) {
			Mention mention1 = mentions.get(i);
			for(int j = i + 1; j < mentions.size(); ++j) {
				Mention mention2 = mentions.get(j);
				if(mention1.equals(mention2)) {
					remove.add(mention2);
				}
			}
		}
		mentions.removeAll(remove);
	}
	
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
		for(Mention mention : mentions) {
			if(mention.endIndex - mention.startIndex == 1) {
				if(numberNER.contains(mention.headword.ner)) {
					remove.add(mention);
				}
			}
		}
		mentions.removeAll(remove);
		
		//remove NORP mentions as modifiers
		remove.clear();
		for(Mention mention : mentions) {
			if((dict.isAdjectivalDemonym(mention.getSpan(sent)) || mention.headword.ner.equals("NORP")) 
					&& (mention.headword.postag.equals("JJ") || mention.headword.basic_deprel.equals("nn"))) {
				remove.add(mention);
			}
		}
		mentions.removeAll(remove);
	}
	
	protected void deleteSpuriousPronominalMentions(List<Mention> mentions, Sentence sent) {
		//remove "you know" mentions
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
		
		//remove "you know" part in a mention
		remove.clear();
		for(Mention mention : mentions) {
			if(mention.endIndex - mention.startIndex > 2) {
				if(sent.getLexicon(mention.endIndex - 2).form.toLowerCase().equals("you")
					&& sent.getLexicon(mention.endIndex - 1).form.toLowerCase().equals("know")) {
					mention.endIndex = mention.endIndex - 2;
					boolean duplicated = false;
					for(Mention m2 : mentions) {
						if(mention == m2) {
							continue;
						}
						if(mention.equals(m2)) {
							duplicated = true;
							break;
						}
					}
					if(duplicated) {
						remove.add(mention);
					}
					else {
						mention.process(sent, mentions, dict, remove);
					}
				}
			}
		}
		mentions.removeAll(remove);
	}
	
	public List<Mention> getSingleMentionList(Document doc, List<List<Mention>> mentionList, Options options) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		List<Mention> allMentions = new ArrayList<Mention>();
		for(List<Mention> mentions : mentionList) {
			allMentions.addAll(mentions);
		}
		
		//re-assign mention ID;
		for(int i = 0; i < allMentions.size(); ++i) {
			Mention mention = allMentions.get(i);
			mention.mentionID = i;
		}
		
		//extract events for mentions
		if(options.useEventFeature()) {
			extractEvents(mentionList, doc, options);
		}
		
		//find speaker for each mention
		findSpeakers(doc, allMentions, mentionList);
		
		if(options.usePreciseMatch()) {
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
		speakersMap.put("<DEFAULT_SPEAKER>", new SpeakerInfo("<DEFAULT_SPEAKER>", false));
		// find default speakers from the speaker tags of document doc
		findDefaultSpeakers(doc, allMentions, speakersMap);
		
		//makr quotations
		markQuotaions(doc, false);
		findQuotationSpeakers(doc, allMentions, mentionList, dict, speakersMap);
		Collections.sort(allMentions, Mention.headIndexWithSpeakerOrderComparator);
		
		//find previous speakerinfo
		SpeakerInfo defaultSpeakerInfo = speakersMap.get("<DEFAULT_SPEAKER>");
		SpeakerInfo preSpeakerInfo = defaultSpeakerInfo;
		Mention preMention = null;
		for(Mention mention : allMentions) {
			if(mention.speakerInfo.isQuotationSpeaker()) {
				continue;
			}
			
			if(preMention != null && !preMention.speakerInfo.equals(mention.speakerInfo)) {
				preSpeakerInfo = preMention.speakerInfo;
			}
			
			if(preSpeakerInfo.equals(defaultSpeakerInfo)) {
				mention.preSpeakerInfo = null;
			}
			else {
				mention.preSpeakerInfo = preSpeakerInfo;
			}
			preMention = mention;
		}
	}
	
	protected void findQuotationSpeakers(Document doc, List<Mention> allMentions, 
			List<List<Mention>> mentionList, Dictionaries dict, Map<String, SpeakerInfo> speakersMap) {
		Pair<Integer, Integer> beginQuotation = new Pair<Integer, Integer>();
		Pair<Integer, Integer> endQuotation = new Pair<Integer, Integer>();
		boolean insideQuotation = false;
		
		int sizeOfDoc = doc.size();
		for(int i = 0; i < sizeOfDoc; ++i) {
			Sentence sent = doc.getSentence(i);
			for(int j = 1; j < sent.length(); ++j) {
				int utterIndex = sent.getLexicon(j).utterance;
				if(utterIndex != 0 && !insideQuotation) {
					insideQuotation = true;
					beginQuotation.first = i;
					beginQuotation.second = j;
				}
				else if(utterIndex == 0 && insideQuotation) {
					insideQuotation = false;
					endQuotation.first = i;
					endQuotation.second = j;
					findQuotationSpeakers(doc, allMentions, mentionList, dict, speakersMap, beginQuotation, endQuotation);
				}
			}
		}
		
		if(insideQuotation) {
			endQuotation.first = sizeOfDoc - 1;
			endQuotation.second = doc.getSentence(endQuotation.first).length();
			findQuotationSpeakers(doc, allMentions, mentionList, dict, speakersMap, beginQuotation, endQuotation);
		}
	}
	
	protected void findQuotationSpeakers(Document doc, List<Mention> allMentions, List<List<Mention>> mentionList, 
			Dictionaries dict, Map<String, SpeakerInfo> speakersMap, 
			Pair<Integer, Integer> beginQuotation, Pair<Integer, Integer> endQuotation) {
		Sentence sent = doc.getSentence(beginQuotation.first);
		List<Mention> mentions = mentionList.get(beginQuotation.first);
		SpeakerInfo speakerInfo = findQuotationSpeaker(sent, mentions, 1, beginQuotation.second, dict, speakersMap);
		if(speakerInfo != null) {
			assignUtterancetoSpeaker(doc, mentionList, dict, beginQuotation, endQuotation, speakerInfo);
			return;
		}
		
		sent = doc.getSentence(endQuotation.first);
		mentions = mentionList.get(endQuotation.first);
		speakerInfo = findQuotationSpeaker(sent, mentions, endQuotation.second, sent.length(), dict, speakersMap);
		if(speakerInfo != null) {
			assignUtterancetoSpeaker(doc, mentionList, dict, beginQuotation, endQuotation, speakerInfo);
			return;
		}
		
		if(beginQuotation.second <= 2 && beginQuotation.first > 0) {
			sent = doc.getSentence(beginQuotation.first - 1);
			mentions = mentionList.get(beginQuotation.first - 1);
			speakerInfo = findQuotationSpeaker(sent, mentions, 1, sent.length(), dict, speakersMap);
			if(speakerInfo != null) {
				assignUtterancetoSpeaker(doc, mentionList, dict, beginQuotation, endQuotation, speakerInfo);
				return;
			}
		}
		
		if(endQuotation.second == doc.getSentence(endQuotation.first).length() - 1 
				&& doc.size() > endQuotation.first + 1) {
			sent = doc.getSentence(endQuotation.first + 1);
			mentions = mentionList.get(endQuotation.first + 1);
			speakerInfo = findQuotationSpeaker(sent, mentions, 1, sent.length(), dict, speakersMap);
			if(speakerInfo != null) {
				assignUtterancetoSpeaker(doc, mentionList, dict, beginQuotation, endQuotation, speakerInfo);
				return;
			}
		}
	}
	
	private void assignUtterancetoSpeaker(Document doc, List<List<Mention>> mentionList, Dictionaries dict,
			Pair<Integer, Integer> beginQuotation, Pair<Integer, Integer> endQuotation, SpeakerInfo speakerInfo) {
		for(int i = beginQuotation.first; i <= endQuotation.first; ++i) {
			Sentence sent = doc.getSentence(i);
			int start = i == beginQuotation.first ? beginQuotation.second : 1;
			int end = i == endQuotation.first ? endQuotation.second : sent.length() - 1;
			List<Mention> mentions = mentionList.get(i);
			for(Mention mention : mentions) {
				if(mention.startIndex >= start && mention.endIndex <= end) {
					mention.setSpeakerInfo(speakerInfo);
				}
			}
		}
	}
	
	protected SpeakerInfo findQuotationSpeaker(Sentence sent, List<Mention> mentions, 
			int startIndex, int endIndex, Dictionaries dict, Map<String, SpeakerInfo> speakersMap) {
		
		for(int i = endIndex - 1; i >= startIndex; --i) {
			if(sent.getLexicon(i).utterance != 0) {
				continue;
			}
			String lemma = sent.getLexicon(i).lemma;
			if(dict.reportVerb.contains(lemma)) {
				int reportVerbPos = i;
				Lexicon reportVerb = sent.getLexicon(reportVerbPos);
				for(int j = startIndex; j < endIndex; ++j) {
					Lexicon lex = sent.getLexicon(j);
					if(lex.collapsed_head == reportVerbPos && (lex.collapsed_deprel.equals("nsubj") || lex.collapsed_deprel.equals("xsubj"))
						|| reportVerb.collapsed_deprel.startsWith("conj_") && lex.collapsed_head == reportVerb.collapsed_head && (lex.collapsed_deprel.equals("nsubj") || lex.collapsed_deprel.equals("xsubj"))) {
						int speakerHeadIndex = j;
						for(Mention mention : mentions) {
							if(mention.getBelognTo() == null 
								&& mention.headIndex == speakerHeadIndex 
								&& mention.startIndex >= startIndex && mention.endIndex < endIndex) {
								if(mention.utteranceInfo == null) {
									String speakerKey = mention.getSpan(sent);
									SpeakerInfo speakerInfo = new SpeakerInfo(speakerKey, true);
									speakersMap.put(speakerKey, speakerInfo);
									speakerInfo.setSpeaker(mention);
									mention.utteranceInfo = speakerInfo;
								}
								return mention.utteranceInfo;
							}
						}
						return new SpeakerInfo(sent.getLexicon(speakerHeadIndex).form, true);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * mark quotations for a document
	 * @param doc
	 * @param normalQuotationType
	 */
	private void markQuotaions(Document doc, boolean normalQuotationType) {
		int utteranceIndex = 0;
		boolean insideQuotation = false;
		boolean hasQuotation = false;
		
		for(Sentence sent : doc.getSentences()) {
			for(Lexicon lex : sent.getLexicons()) {
				lex.utterance = utteranceIndex;
				if(lex.form.equals("``") || (!insideQuotation && normalQuotationType && lex.form.equals("\""))) {
					utteranceIndex++;
					lex.utterance = utteranceIndex;
					insideQuotation = true;
					hasQuotation = true;
				}
				else if((utteranceIndex > 0 && lex.form.equals("''")) || (insideQuotation && normalQuotationType && lex.form.equals("\""))) {
					insideQuotation = false;
					utteranceIndex--;
				}
			}
		}
		
		if(!hasQuotation && !normalQuotationType) {
			markQuotaions(doc, true);
		}
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
				speakerInfo = new SpeakerInfo(speaker, false);
				speakersMap.put(speaker, speakerInfo);
			}
			mention.setSpeakerInfo(speakerInfo);
		}
	}
	
	protected void findPreciseMatchRelation(Document doc, List<Mention> allMentions) {
		for(int i = 1; i < allMentions.size(); ++i){
			Mention anaph = allMentions.get(i);
			
			//find precise match
			for(int j = 0; j < i; ++j){
				Mention antec = allMentions.get(j);
				
				if(anaph.preciseMatch(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), dict)){
					anaph.addPreciseMatch(antec, doc);
				}
				
				if(anaph.stringMatch(doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), dict)) {
					anaph.addStringMatch(antec, doc);
				}
			}
			
			//find local attribute match (only for pronominal)
			if(!anaph.isPronominal()) {
				continue;
			}
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
		}
	}
	
	protected void extractEvents(List<List<Mention>> mentionList, Document doc, Options options) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		EventExtractor eventExtractor = EventExtractor.createExtractor(options.getEventExtractor());
		eventExtractor.extractEvents(doc, mentionList, options);
	}
	
	protected void findSyntacticRelation(List<Mention> mentions, Sentence sent, Options options) throws InstantiationException, IllegalAccessException, ClassNotFoundException, MentionException{
		markListMemberRelation(mentions, sent, RelationExtractor.createExtractor(options.getListMemberRelationExtractor()));
		deleteSpuriousListMentions(mentions, sent);
		markAppositionRelation(mentions, sent, RelationExtractor.createExtractor(options.getAppositionRelationExtractor()));
		markRoleAppositionRelation(mentions, sent, RelationExtractor.createExtractor(options.getRoleAppositionRelationExtractor()));
		markPredicateNominativeRelation(mentions, sent, RelationExtractor.createExtractor(options.getPredicateNominativeRelationExtractor()));
		//markRelativePronounRelation(mentions, sent, RelationExtractor.createExtractor(options.getRelativePronounRelationExtractor()));
	}
	
	/**
	 * remove nested mention with shared headword (except enumeration/list): pick larger one
	 * @param mentions
	 * @param sent
	 */
	protected void deleteSpuriousListMentions(List<Mention> mentions, Sentence sent){
		Set<Mention> remove = new HashSet<Mention>();
		for(Mention mention1 : mentions){
			for(Mention mention2 : mentions){
				if(mention1.headIndex == mention2.headIndex && mention2.cover(mention1) && mention1.getBelognTo() == null){
					remove.add(mention1);
				}
			}
		}
		mentions.removeAll(remove);
	}
	
	protected void markListMemberRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException {
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, sent, foundPairs, "LISTMEMBER");
	}
	
	protected void markAppositionRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException {
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, sent, foundPairs, "APPOSITION");
	}
	
	protected void markRoleAppositionRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException {
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, sent, foundPairs, "ROLE_APPOSITION");
	}
	
	protected void markPredicateNominativeRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException {
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, sent, foundPairs, "PREDICATE_NOMINATIVE");
	}
	
	protected void markRelativePronounRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor) throws MentionException {
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		markMentionRelation(mentions, sent, foundPairs, "RELATIVE_PRONOUN");
	}
	
	protected void markMentionRelation(List<Mention> mentions, Sentence sent, Set<Pair<Integer, Integer>> foundPairs, String relation) throws MentionException {
		for(Mention mention1 : mentions) {
			for(Mention mention2 : mentions) {
				if(mention1.equals(mention2)) {
					continue;
				}
				
				if(relation.equals("LISTMEMBER")) {
					for(Pair<Integer, Integer> pair : foundPairs) {
						if(pair.first == mention1.mentionID && pair.second == mention2.mentionID) {
							mention2.addListMember(mention1, sent);
						}
					}
				}
				else if(relation.equals("PREDICATE_NOMINATIVE")) {
					for(Pair<Integer, Integer> pair : foundPairs) {
						if(pair.first == mention1.mentionID && pair.second == mention2.mentionID) {
							mention2.addPredicativeNominative(mention1, dict);
						}
					}
				}
				else if(relation.equals("ROLE_APPOSITION")) {
					for(Pair<Integer, Integer> pair : foundPairs) {
						if(pair.first == mention1.mentionID && pair.second == mention2.mentionID) {
							mention2.addRoleApposition(mention1, sent, dict);
						}
					}
				}
				else{
					for(Pair<Integer, Integer> pair : foundPairs) {
						if(pair.first == mention1.headIndex && pair.second == mention2.headIndex) {
							if(relation.equals("APPOSITION")) {
								mention2.addApposition(mention1, dict);
							}
							else if(relation.equals("RELATIVE_PRONOUN")) {
								mention2.addRelativePronoun(mention1);
							}
							else {
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
