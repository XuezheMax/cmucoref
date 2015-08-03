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
		if(collapsed_headword.postag.startsWith("VB") && this.acceptableGrammRole(collapsed_deprel, true)) {
			if(mention.getBelognTo() != null && indexOutsideMention(collapsed_headword.id, mention.getBelognTo())) {
				isPart = true;
			}
			
			if(edict.copulas.contains(collapsed_headword.lemma)) {
				Event copulaEvent = findCopulaEventWithBasicDep(sent, collapsed_headword.id, collapsed_deprel, mention, isPart, options);
				mention.addEvent(copulaEvent);
				mention.setMainEvent(copulaEvent, true);
				this.addEvent2Set(copulaEvent);
			}
			else {		
				String predicate = collapsed_headword.lemma;
				Event collapsedEvent = null;
				if(edict.englishVerbs.contains(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							predicate, collapsed_deprel, isPart);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							normalize(predicate), collapsed_deprel, isPart);
				}
				else if(predicate.equals("$")) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_$", collapsed_deprel, isPart);
				}
				else if(this.isNumber(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_<num>", collapsed_deprel, isPart);
				}
//				else {
//					unknownVerbs.add(predicate);
//				}
				mention.addEvent(collapsedEvent);
				mention.setMainEvent(collapsedEvent, true);
				this.addEvent2Set(collapsedEvent);
			}
		}
		else {
			String predicate  = collapsed_headword.lemma;
			Event collapsedEvent = null;
			if(this.acceptableGrammRole(collapsed_deprel, true)) {
				if(edict.englishVerbs.contains(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							predicate, collapsed_deprel, isPart);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							normalize(predicate), collapsed_deprel, isPart);
				}
				else if(predicate.equals("$")) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_$", collapsed_deprel, isPart);
				}
				else if(this.isNumber(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_<num>", collapsed_deprel, isPart);
				}
			}
			else {
				boolean[] used = new boolean[sent.length()];
				used[headword.id] = true;
				while(!used[collapsed_headword.id] && !collapsed_headword.postag.startsWith("VB") 
						&& collapsed_headword.collapsed_head > 0) {
					used[collapsed_headword.id] = true;
					// is part if not conj
					if(!collapsed_deprel.startsWith("conj")) {
						isPart = true;
					}
					// update deprel when new acceptable rel detected
					if(this.acceptableGrammRole(collapsed_headword.collapsed_deprel, true)) {
						collapsed_deprel = collapsed_headword.collapsed_deprel;
					}
					collapsed_headword = sent.getLexicon(collapsed_headword.collapsed_head);
				}
			
				if(collapsed_headword.postag.startsWith("VB") && this.acceptableGrammRole(collapsed_deprel, true)) {
					predicate = collapsed_headword.lemma;
					if(edict.englishVerbs.contains(predicate)) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								predicate, collapsed_deprel, isPart);
					}
					else if(edict.englishVerbs.contains(normalize(predicate))) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								normalize(predicate), collapsed_deprel, isPart);
					}
					else if(predicate.equals("$")) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								"be_$", collapsed_deprel, isPart);
					}
					else if(this.isNumber(predicate)) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								"be_<num>", collapsed_deprel, isPart);
					}
//					else {
//						unknownVerbs.add(predicate);
//					}
				}
			}
			mention.addEvent(collapsedEvent);
			mention.setMainEvent(collapsedEvent, false);
			this.addEvent2Set(collapsedEvent);
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
			
			if(basic_headword.postag.startsWith("VB") && this.acceptableGrammRole(basic_deprel, false)) {
				if(edict.copulas.contains(basic_headword.lemma)) {
					Event copulaEvent = findCopulaEventWithBasicDep(sent, headword.basic_head, basic_deprel, mention, isPart, options);
					mention.addEvent(copulaEvent);
					mention.setMainEvent(copulaEvent, true);
					this.addEvent2Set(copulaEvent);
					
					//find conjunction verbs/events
					if(copulaEvent.grammaticRole.startsWith("nsubj")) {
						findConjEventsWithBasicDep(sent, headword.basic_head, mention, isPart, options);
					}
				}
				else {
					//add basic event
					String predicate = basic_headword.lemma;
					Event basicEvent = null;
					if(edict.englishVerbs.contains(predicate)) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								predicate, basic_deprel, isPart);
					}
					else if(edict.englishVerbs.contains(normalize(predicate))) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								normalize(predicate), basic_deprel, isPart);
					}
					else if(predicate.equals("$")) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								"be_$", basic_deprel, isPart);
					}
					else if(this.isNumber(predicate)) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								"be_<num>", basic_deprel, isPart);
					}
//					else {
//						unknownVerbs.add(predicate);
//					}
					mention.addEvent(basicEvent);
					mention.setMainEvent(basicEvent, true);
					this.addEvent2Set(basicEvent);
					
					//find conjunction verbs/events
					if(basicEvent != null && basicEvent.grammaticRole.startsWith("nsubj")) {
						findConjEventsWithBasicDep(sent, headword.basic_head, mention, isPart, options);
					}
				}
			}
			else if(acceptableGrammRole(basic_deprel, false)) {
				Event basicEvent = null;
				String predicate = basic_headword.lemma;
				if(edict.englishVerbs.contains(predicate)) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							predicate, basic_deprel, isPart);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							normalize(predicate), basic_deprel, isPart);
				}
				else if(predicate.equals("$")) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							"be_$", basic_deprel, isPart);
				}
				else if(this.isNumber(predicate)) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							"be_<num>", basic_deprel, isPart);
				}
				else {
					boolean[] used = new boolean[sent.length()];
					used[headword.id] = true;
					while(!used[basic_headword.id] && !basic_headword.postag.startsWith("VB") 
							&& basic_headword.collapsed_head > 0) {
						used[basic_headword.id] = true;
						basic_headword = sent.getLexicon(basic_headword.basic_head);
					}
					
					if(basic_headword.postag.startsWith("VB")) {
						predicate = basic_headword.lemma;
						if(edict.englishVerbs.contains(predicate)) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									predicate, basic_deprel, isPart);
						}
						else if(edict.englishVerbs.contains(normalize(predicate))) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									normalize(predicate), basic_deprel, isPart);
						}
						else if(predicate.equals("$")) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									"be_$", basic_deprel, isPart);
						}
						else if(this.isNumber(predicate)) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									"be_<num>", basic_deprel, isPart);
						}
//						else {
//							unknownVerbs.add(predicate);
//						}
					}
				}
				mention.addEvent(basicEvent);
				mention.setMainEvent(basicEvent, true);
				this.addEvent2Set(basicEvent);
			}
		}
	}
	
	protected Event findCopulaEventWithBasicDep(Sentence sent, int copulaPos, String gramRole, Mention mention, boolean isPart, Options options) {
		Lexicon copula = sent.getLexicon(copulaPos);
		if(!gramRole.startsWith("nsubj") && !gramRole.startsWith("xsubj")) {
			return new Event(mention.sentID, sent, copulaPos, copula.lemma, gramRole, isPart);
		}
		
		for(int i = copulaPos + 1; i < sent.length(); ++i) {
			Lexicon lexi = sent.getLexicon(i);
			if(lexi.basic_head == copulaPos && lexi.basic_deprel.equals("acomp")) {
				String acomp = this.isNumber(lexi.lemma) ? "<num>" : lexi.lemma;
				return new Event(mention.sentID, sent, copulaPos, copula.lemma + "_" + acomp, gramRole, isPart);
			}
		}
		return new Event(mention.sentID, sent, copulaPos, copula.lemma, gramRole, isPart);
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
					Event conjEvent = null;
					if(edict.englishVerbs.contains(lexi.lemma)) {
						conjEvent = new Event(mention.sentID, sent, i, lexi.lemma, "nsubj", isPart);
					}
					else if(edict.englishVerbs.contains(normalize(lexi.lemma))) {
						conjEvent = new Event(mention.sentID, sent, i, normalize(lexi.lemma), "nsubj", isPart);
					}
					else if(lexi.lemma.equals("$")) {
						conjEvent = new Event(mention.sentID, sent, i, "be_$", "nsubj", isPart);
					}
					else if(this.isNumber(lexi.lemma)) {
						conjEvent = new Event(mention.sentID, sent, i, "be_<num>", "nsubj", isPart);
					}
//					else {
//						unknownVerbs.add(lexi.lemma);
//					}
					mention.addEvent(conjEvent);
					mention.setMainEvent(conjEvent, false);
					this.addEvent2Set(conjEvent);
					prev = i;
				}
				else {
					break;
				}
			}
		}
	}
}
