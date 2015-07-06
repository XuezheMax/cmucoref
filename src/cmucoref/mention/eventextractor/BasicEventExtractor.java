package cmucoref.mention.eventextractor;

import java.util.List;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Event;
import cmucoref.mention.Mention;
import cmucoref.model.Options;

public class BasicEventExtractor extends EventExtractor {
	
	public BasicEventExtractor() {
		super();
	}

	@Override
	public void extractEvents(Document doc, List<List<Mention>> mentionList, Options options) {
		for(List<Mention> mentions : mentionList) {
			for(Mention mention : mentions) {
				findEvents(doc.getSentence(mention.sentID), mention, options);
			}
		}
	}

	protected void findEvents(Sentence sent, Mention mention, Options options) {
		// find event with basic dependencies
		findEventsWithBasicDep(sent, mention, options);
		
		//find event with collapsed dependencies
		findEventsWithCollapsedDep(sent, mention, options);
	}
	
	protected void findEventsWithCollapsedDep(Sentence sent, Mention mention, Options options) {
		Lexicon headword = sent.getLexicon(mention.originalHeadIndex);
		Lexicon collapsed_headword = sent.getLexicon(headword.collapsed_head);
		String collapsed_deprel = headword.collapsed_deprel;
		
		boolean isPart = false;
		if(collapsed_headword.postag.startsWith("VB") || acceptableGrammRole(collapsed_deprel, false)) {
			if(mention.getBelognTo() != null && indexOutsideMention(collapsed_headword.id, mention.getBelognTo())) {
				isPart = true;
			}
			
			if(copulas.contains(collapsed_headword.lemma)) {
				Event copulaEvent = findCopulaEventWithBasicDep(sent, headword.basic_head, collapsed_deprel, mention, isPart, options);
				mention.addEvent(copulaEvent);
				mention.setMainEvent(copulaEvent, false);
			}
			else {				
				Event collapsedEvent = new Event(mention.sentID, headword.collapsed_head, 
						collapsed_headword.lemma, collapsed_deprel, isPart);
				mention.addEvent(collapsedEvent);
				mention.setMainEvent(collapsedEvent, false);
			}
		}
		else {
			//mention.display(sent, System.err);
			boolean[] used = new boolean[sent.length()];
			used[headword.id] = true;
			while(!used[collapsed_headword.id] && !collapsed_headword.postag.startsWith("VB") 
					&& !acceptableGrammRole(collapsed_deprel, false)
					&& collapsed_headword.collapsed_head > 0) {
				used[collapsed_headword.id] = true;
				collapsed_deprel = collapsed_headword.collapsed_deprel;
				collapsed_headword = sent.getLexicon(collapsed_headword.collapsed_head);
			}
			
			if(collapsed_headword.postag.startsWith("VB") || acceptableGrammRole(collapsed_deprel, false)) {
				Event collapsedEvent = new Event(mention.sentID, collapsed_headword.id, 
						collapsed_headword.lemma, collapsed_deprel, true);
				mention.addEvent(collapsedEvent);
				mention.setMainEvent(collapsedEvent, false);
			}
			else {
				Event collapsedEvent = new Event(mention.sentID, 0, Event.unknownPredicate, Event.unknownRole, true);
				mention.addEvent(collapsedEvent);
				mention.setMainEvent(collapsedEvent, false);
			}
		}
	}
	
	protected void findEventsWithBasicDep(Sentence sent, Mention mention, Options options) {
		Lexicon headword = sent.getLexicon(mention.originalHeadIndex);
		Lexicon basic_headword = sent.getLexicon(headword.basic_head);
		String basic_deprel = headword.basic_deprel;
		if(indexOutsideMention(headword.basic_head, mention)) {
			boolean isPart = false;
			
			if(mention.getBelognTo() != null && indexOutsideMention(headword.basic_head, mention.getBelognTo())) {
				isPart = true;
			}
			
			if(basic_headword.postag.startsWith("VB") || acceptableGrammRole(basic_deprel, false)) {
				if(copulas.contains(basic_headword.lemma)) {
					Event copulaEvent = findCopulaEventWithBasicDep(sent, headword.basic_head, basic_deprel, mention, isPart, options);
					mention.addEvent(copulaEvent);
					mention.setMainEvent(copulaEvent, true);
					if(copulaEvent.grammaticRole.startsWith("nsubj")) {
						//find conjunction verbs/events
						findConjEventsWithBasicDep(sent, headword.basic_head, mention, isPart, options);
					}
				}
				else {
					//add basic event
					Event basicEvent = new Event(mention.sentID, headword.basic_head, basic_headword.lemma, basic_deprel, isPart);
					mention.addEvent(basicEvent);
					mention.setMainEvent(basicEvent, true);
					if(basicEvent.grammaticRole.startsWith("nsubj")) {
						//find conjunction verbs/events
						findConjEventsWithBasicDep(sent, headword.basic_head, mention, isPart, options);
					}
				}
			}
//			else if(basic_deprel.equals("pobj") && basic_headword.postag.equals("IN")) {
//				Lexicon head_of_basic_headword = sent.getLexicon(basic_headword.basic_head);
//				if(head_of_basic_headword.postag.startsWith("VB")) {
//					//add basic prep event
//					Event prepEvent = new Event(mention.sentID, basic_headword.basic_head, 
//							head_of_basic_headword.lemma, "prep_" + basic_headword.lemma);
//					mention.addEvent(prepEvent);
//					mention.setMainEvent(prepEvent, true);
//				}
//			}
		}
	}
	
	protected Event findCopulaEventWithBasicDep(Sentence sent, int copulaPos, String gramRole, Mention mention, boolean isPart, Options options) {
		Lexicon copula = sent.getLexicon(copulaPos);
		if(!gramRole.startsWith("nsubj")) {
			return new Event(mention.sentID, copulaPos, copula.lemma, gramRole, isPart);
		}
		
		for(int i = copulaPos + 1; i < sent.length(); ++i) {
			Lexicon lexi = sent.getLexicon(i);
			if(lexi.basic_head == copulaPos && lexi.basic_deprel.equals("acomp")) {
				return new Event(mention.sentID, copulaPos, copula.lemma + "_" + lexi.lemma, gramRole, isPart);
			}
		}
		return new Event(mention.sentID, copulaPos, copula.lemma, gramRole, isPart);
	}
	
	protected void findConjEventsWithBasicDep(Sentence sent, int verbPos, Mention mention, boolean isPart, Options options) {
		int prev = verbPos;
		for(int i = verbPos + 1; i < sent.length(); ++i) {
			Lexicon lexi = sent.getLexicon(i);
			if(lexi.basic_head == verbPos && lexi.basic_deprel.equals("conj") && lexi.postag.startsWith("VB")) {
				boolean hasSubj = false;
				for(int j = prev + 1; j < i; ++j) {
					Lexicon lexj = sent.getLexicon(j);
					if(lexj.basic_head == i && lexj.basic_deprel.startsWith("nsubj")) {
						hasSubj = true;
						break;
					}
				}
				if(!hasSubj) {
					Event conjEvent = new Event(mention.sentID, i, lexi.lemma, "nsubj", isPart);
					mention.addEvent(conjEvent);
					prev = i;
				}
				else {
					break;
				}
			}
		}
	}
}
