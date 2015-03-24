package cmucoref.mention.extractor;

import cmucoref.document.Document;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.relationextractor.*;
import cmucoref.model.Options;
import cmucoref.util.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.dcoref.Dictionaries;

public abstract class MentionExtractor {
	
	protected Dictionaries dict;
	
	public MentionExtractor(Dictionaries dict){
		this.dict = dict;
	}
	
	public abstract List<List<Mention>> extractPredictedMentions(Document doc, Options options) throws IOException;
	
	protected List<List<Mention>> constructMentionTreeList(List<List<Mention>> originalList){
		int numSent = originalList.size();
		List<List<Mention>> mentionList = new ArrayList<List<Mention>>(numSent);
		for(int i = 0; i < numSent; ++i){
			List<Mention> orgMentions = originalList.get(i);
			mentionList.add(constructMentionTree(orgMentions));
		}
		return mentionList;
	}
	
	protected List<Mention> constructMentionTree(List<Mention> orgMentions){
		List<Mention> mentions = new ArrayList<Mention>();
		for(Mention orgMention : orgMentions){
			setParent(mentions, orgMention);
		}
		return mentions;
	}
	
	protected void postTreeOrderMentions(List<Mention> mentions, List<Mention> postOrderMentions){
		for(Mention mention : mentions){
			if(mention.children != null){
				postTreeOrderMentions(mention.children, postOrderMentions);
			}
			postOrderMentions.add(mention);
		}
	}
	
	protected void setParent(List<Mention> mentions, Mention addMention){
		int index = -1;
		int size = mentions.size();
		for(int i = 0; i < size; ++i){
			Mention mention = mentions.get(i);
			if(addMention.cover(mention)){
				if(index == -1){
					index = i;
				}
				addMention.addChild(mention);
			}
		}
		if(index == -1){
			mentions.add(addMention);			
		}
		else{
			for(int i = size - 1; i >= index; --i){
				mentions.remove(i);
			}
			mentions.add(addMention);
		}
	}
	
	protected void findSyntacticRelation(List<Mention> mentions, Sentence sent, Options options) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		markListMemberRelation(mentions, sent, RelationExtractor.createExtractor(options.getListMemberRelationExtractor()));
		markAppositionRelation(mentions, sent, RelationExtractor.createExtractor(options.getAppositionRelationExtractor()));
		markPredicateNominativeRelation(mentions, sent, RelationExtractor.createExtractor(options.getPredicateNominativeRelationExtractor()));
		markRelativePronounRelation(mentions, sent, RelationExtractor.createExtractor(options.getRelativePronounRelationExtractor()));
	}
	
	protected void markListMemberRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor){
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		try {
			markMentionRelation(mentions, foundPairs, "LISTMEMBER");
		} catch (MentionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	protected void markAppositionRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor){
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		try {
			markMentionRelation(mentions, foundPairs, "APPOSITION");
		} catch (MentionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	protected void markPredicateNominativeRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor){
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		try {
			markMentionRelation(mentions, foundPairs, "PREDICATE_NOMINATIVE");
		} catch (MentionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	protected void markRelativePronounRelation(List<Mention> mentions, Sentence sent, RelationExtractor extractor){
		Set<Pair<Integer, Integer>> foundPairs = extractor.extractRelation(sent, mentions);
		try {
			markMentionRelation(mentions, foundPairs, "RELATIVE_PRONOUN");
		} catch (MentionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	protected void markMentionRelation(List<Mention> mentions, Set<Pair<Integer, Integer>> foundPairs, String relation) throws MentionException{
		for(Mention mention1 : mentions){
			for(Mention mention2 : mentions){
				if(!relation.equals("LISTMEMBER")){
					// Ignore if m2 and m1 are in list relationship
					if(mention1.isListMemberOf(mention2) || mention2.isListMemberOf(mention1)){
						System.err.println("Relation error: " + relation);
						displayMention(mention1, new PrintWriter(System.err));
						displayMention(mention2, new PrintWriter(System.err));
						System.exit(0);
					}
				}
				
				for(Pair<Integer, Integer> pair : foundPairs){
					if(pair.first == mention1.headIndex && pair.second == mention2.headIndex){
						if(relation.equals("LISTMEMBER")){
							mention2.addListMember(mention1);
						}
						else if(relation.equals("APPOSITION")){
							mention2.addApposition(mention1);
						}
						else if(relation.equals("PREDICATE_NOMINATIVE")){
							mention2.addPredicativeNominative(mention1);
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
	
	public void displayMentions(Document doc, List<List<Mention>> mentionList, PrintWriter printer){
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
	}
	
	public void displayMention(Mention mention, PrintWriter printer) {
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
	}
	
	public void displayMention(Sentence sent, Mention mention, PrintWriter printer){
		printer.println("#Begin Mention " + mention.mentionID);
		
		printer.println("Children Mentions: ");
		if(mention.children != null){
			for(Mention child : mention.children){
				displayMention(sent, child, printer);
			}
		}
		printer.println("end");
		
		printer.println("sent ID: " + mention.sentID);
		printer.println("mention ID: " + mention.mentionID);
		printer.println(mention.startIndex + " " + mention.endIndex + " " + mention.getSpan(sent));
		printer.println("headIndex: " + mention.headIndex);
		printer.println("headString: " + mention.headString);
		printer.println("mention type: " + mention.mentionType);
		printer.println("mention gender: " + mention.gender);
		printer.println("mention number: " + mention.number);
		printer.println("mention animacy: " + mention.animacy);
		printer.println("mention person: " + mention.person);
		printer.println("#end Mention " + mention.mentionID);
		printer.println("===================================");
	}
}
