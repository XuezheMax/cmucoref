package cmucoref.mention.eventextractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Event;
import cmucoref.mention.Mention;
import cmucoref.model.Options;

public class BasicEventExtractor extends EventExtractor{

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
		
		//find rcmod event
		findRCMODEvents(sent, mention, options);
		
		//delete useless event
		deleteUselessEvent(sent, mention, options);
	}
	
	/**
	 * delete useless event like [want, nsubj] in "I want to eat food" 
	 * @param sent
	 * @param mention
	 * @param options
	 */
	protected void deleteUselessEvent(Sentence sent, Mention mention, Options options) {
		Set<Event> removed = new HashSet<Event>();
		for(Event event : mention.getEventSet()) {
			for(Event xcomp : mention.getEventSet()) {
				if(event.predPosition + 2 < sent.length()
					&& event.predPosition + 2 == xcomp.predPosition 
					&& sent.getLexicon(event.predPosition + 1).form.equals("to")
					&& sent.getLexicon(xcomp.predPosition).basic_deprel.equals("xcomp")) {
					removed.add(event);
					if(mention.getMainEvent().equals(event)) {
						mention.setMainEvent(xcomp, true);
					}
				}
				
				if(event.predPosition + 3 < sent.length()
					&& event.predPosition + 3 == xcomp.predPosition 
					&& (sent.getLexicon(event.predPosition + 1).form.equals("able") || sent.getLexicon(event.predPosition + 1).form.equals("unable"))
					&& sent.getLexicon(event.predPosition + 2).form.equals("to")
					&& sent.getLexicon(xcomp.predPosition).basic_deprel.equals("xcomp")) {
					removed.add(event);
					if(mention.getMainEvent().equals(event)) {
						mention.setMainEvent(xcomp, true);
					}
				}
			}
		}
		mention.removeEvents(removed);
	}
	
	protected void findRCMODEvents(Sentence sent, Mention mention, Options options) {
		for(int i = mention.originalHeadIndex + 1; i < mention.endIndex; ++i) {
			Lexicon rcmodVerb = sent.getLexicon(i);
			if(rcmodVerb.basic_head == mention.originalHeadIndex && rcmodVerb.basic_deprel.equals("rcmod")) {
				String predicate = rcmodVerb.lemma;
				if(!edict.englishVerbs.contains(predicate)) {
					if(edict.englishVerbs.contains(normalize(predicate))) {
						predicate = normalize(predicate);
					}
					else if(predicate.equals("$")) {
						predicate = "be_$";
					}
					else if(this.isNumber(predicate)) {
						predicate = "be_<num>";						
					}
					else {
						continue;
					}
				}
				Event rcmodEvent = null;
				boolean hasSubj = false;
				for(int j = mention.originalHeadIndex + 1; j < i; ++j) {
					Lexicon lex = sent.getLexicon(j);
					if(lex.postag.startsWith("W") && this.acceptableGrammRole(lex.basic_deprel, false)) {
						String role = lex.basic_deprel;
						if(role.equals("pobj")) {
							role = "prep_" + sent.getLexicon(lex.basic_head).lemma.toLowerCase();
						}
						rcmodEvent = new Event(mention.sentID, sent, rcmodVerb.id, predicate, role, edict);
						break;
					}
					if(lex.basic_head == i && lex.basic_deprel.startsWith("nsubj")) {
						hasSubj = true;
					}
				}
				if(rcmodEvent == null) {
					String role = hasSubj ? "dobj" : (rcmodVerb.postag.equals("VBN") ? "nsubjpass" : "nsubj");
					rcmodEvent = new Event(mention.sentID, sent, rcmodVerb.id, predicate, role, edict);
				}
				mention.addEvent(rcmodEvent);
				mention.setMainEvent(rcmodEvent, false);
				this.addEvent2Set(rcmodEvent);
			}
		}
	}
	
	protected void findEventsWithCollapsedDep(Sentence sent, Mention mention, Options options) {
		Lexicon headword = sent.getLexicon(mention.originalHeadIndex);
		Lexicon collapsed_headword = sent.getLexicon(headword.collapsed_head);
		String collapsed_deprel = headword.collapsed_deprel;
		
		if(collapsed_headword.postag.startsWith("VB") && this.acceptableGrammRole(collapsed_deprel, true)) {
			if(mention.getBelognTo() != null && indexOutsideMention(collapsed_headword.id, mention.getBelognTo())) {
				return;
			}
			
			if(edict.copulas.contains(collapsed_headword.lemma)) {
				findCopulaEventWithBasicDep(sent, collapsed_headword.id, collapsed_deprel, mention, options);
			}
			else {		
				String predicate = collapsed_headword.lemma;
				Event collapsedEvent = null;
				if(edict.englishVerbs.contains(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							addParticle(predicate, collapsed_headword.id, sent), 
							collapsed_deprel, edict);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							addParticle(normalize(predicate), collapsed_headword.id, sent), 
							collapsed_deprel, edict);
				}
				else if(predicate.equals("$")) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_$", collapsed_deprel, edict);
				}
				else if(this.isNumber(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_<num>", collapsed_deprel, edict);
				}
//				else {
//					unknownVerbs.add(predicate);
//				}
				mention.addEvent(collapsedEvent);
				mention.setMainEvent(collapsedEvent, true);
				this.addEvent2Set(collapsedEvent);
				if(collapsedEvent != null && collapsedEvent.grammaticRole.endsWith("subj")) {
					findXCOMPEventWithBasicDep(sent, headword.collapsed_head, mention, options);
					findConjEventsWithBasicDep(sent, headword.collapsed_head, mention, options);
				}
			}
		}
		else {
			String predicate  = collapsed_headword.lemma;
			Event collapsedEvent = null;
			if(this.acceptableGrammRole(collapsed_deprel, true)) {
				if(edict.englishVerbs.contains(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							addParticle(predicate, collapsed_headword.id, sent), 
							collapsed_deprel, edict);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							addParticle(normalize(predicate), collapsed_headword.id, sent), 
							collapsed_deprel, edict);
				}
				else if(predicate.equals("$")) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_$", collapsed_deprel, edict);
				}
				else if(this.isNumber(predicate)) {
					collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
							"be_<num>", collapsed_deprel, edict);
				}
			}
			
			if(collapsedEvent == null) {
				boolean[] used = new boolean[sent.length()];
				used[headword.id] = true;
				while(!used[collapsed_headword.id] && !collapsed_headword.postag.startsWith("VB") 
						&& collapsed_headword.collapsed_head > 0) {
					used[collapsed_headword.id] = true;
					// is part if not conj
					if(!collapsed_deprel.startsWith("conj")) {
						break;
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
								addParticle(predicate, collapsed_headword.id, sent), 
								collapsed_deprel, edict);
					}
					else if(edict.englishVerbs.contains(normalize(predicate))) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								addParticle(normalize(predicate), collapsed_headword.id, sent), 
								collapsed_deprel, edict);
					}
					else if(predicate.equals("$")) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								"be_$", collapsed_deprel, edict);
					}
					else if(this.isNumber(predicate)) {
						collapsedEvent = new Event(mention.sentID, sent, collapsed_headword.id, 
								"be_<num>", collapsed_deprel, edict);
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
			
			if(mention.getBelognTo() != null && indexOutsideMention(headword.basic_head, mention.getBelognTo())) {
				return;
			}
			
			if(basic_headword.postag.startsWith("VB") && this.acceptableGrammRole(basic_deprel, false)) {
				if(edict.copulas.contains(basic_headword.lemma)) {
					findCopulaEventWithBasicDep(sent, headword.basic_head, basic_deprel, mention, options);
				}
				else {
					//add basic event
					String predicate = basic_headword.lemma;
					Event basicEvent = null;
					if(edict.englishVerbs.contains(predicate)) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								addParticle(predicate, headword.basic_head, sent), 
								basic_deprel, edict);
					}
					else if(edict.englishVerbs.contains(normalize(predicate))) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								addParticle(normalize(predicate), headword.basic_head, sent), 
								basic_deprel, edict);
					}
					else if(predicate.equals("$")) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								"be_$", basic_deprel, edict);
					}
					else if(this.isNumber(predicate)) {
						basicEvent = new Event(mention.sentID, sent, headword.basic_head, 
								"be_<num>", basic_deprel, edict);
					}
//					else {
//						unknownVerbs.add(predicate);
//					}
					mention.addEvent(basicEvent);
					mention.setMainEvent(basicEvent, true);
					this.addEvent2Set(basicEvent);
					
					//find conjunction verbs/events
					if(basicEvent != null && basicEvent.grammaticRole.startsWith("nsubj")) {
						findXCOMPEventWithBasicDep(sent, headword.basic_head, mention, options);
						findConjEventsWithBasicDep(sent, headword.basic_head, mention, options);
					}
				}
			}
			else if(acceptableGrammRole(basic_deprel, false)) {
				Event basicEvent = null;
				String predicate = basic_headword.lemma;
				if(edict.englishVerbs.contains(predicate)) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							addParticle(predicate, basic_headword.id, sent), 
							basic_deprel, edict);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							addParticle(normalize(predicate), basic_headword.id, sent), 
							basic_deprel, edict);
				}
				else if(predicate.equals("$")) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							"be_$", basic_deprel, edict);
				}
				else if(this.isNumber(predicate)) {
					basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
							"be_<num>", basic_deprel, edict);
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
									addParticle(predicate, basic_headword.id, sent), 
									basic_deprel, edict);
						}
						else if(edict.englishVerbs.contains(normalize(predicate))) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									addParticle(normalize(predicate), basic_headword.id, sent), 
									basic_deprel, edict);
						}
						else if(predicate.equals("$")) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									"be_$", basic_deprel, edict);
						}
						else if(this.isNumber(predicate)) {
							basicEvent = new Event(mention.sentID, sent, basic_headword.id, 
									"be_<num>", basic_deprel, edict);
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
	
	protected void findCopulaEventWithBasicDep(Sentence sent, int copulaPos, String gramRole, Mention mention, Options options) {
//		Lexicon copula = sent.getLexicon(copulaPos);
		if(!gramRole.startsWith("nsubj") && !gramRole.startsWith("xsubj")) {
//			return new Event(mention.sentID, sent, copulaPos, copula.lemma, gramRole, edict);
			return;
		}
		
		for(int i = copulaPos + 1; i < sent.length(); ++i) {
			Lexicon lexi = sent.getLexicon(i);
			if(lexi.basic_head == copulaPos && lexi.basic_deprel.equals("acomp")) {
				String acomp = this.isNumber(lexi.form) ? "<num>" : lexi.form;
				Event copulaEvent = new Event(mention.sentID, sent, copulaPos, "be" + "_" + acomp, gramRole, edict);
				mention.addEvent(copulaEvent);
				mention.setMainEvent(copulaEvent, true);
				this.addEvent2Set(copulaEvent);
				
				findXCOMPEventForCopulaEvent(sent, copulaPos, i, mention, options);
				findConjEventsWithBasicDep(sent, copulaPos, mention, options);
				return;
			}
		}
//		return new Event(mention.sentID, sent, copulaPos, copula.lemma, gramRole, edict);
	}
	
	protected void findXCOMPEventWithBasicDep(Sentence sent, int verbPos, Mention mention, Options options) {
		if(verbPos + 2 < sent.length() && sent.getLexicon(verbPos + 1).form.equals("to")) {
			Lexicon xverb = sent.getLexicon(verbPos + 2);
			if(xverb.basic_head == verbPos && xverb.basic_deprel.equals("xcomp")) {
				Event xcompEvent = null;
				String predicate = xverb.lemma;
				if(edict.englishVerbs.contains(predicate)) {
					xcompEvent = new Event(mention.sentID, sent, xverb.id, 
							addParticle(predicate, xverb.id, sent), "nsubj", edict);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					xcompEvent = new Event(mention.sentID, sent, xverb.id, 
							addParticle(normalize(predicate), xverb.id, sent), "nsubj", edict);
				}
				mention.addEvent(xcompEvent);
				mention.setMainEvent(xcompEvent, true);
				this.addEvent2Set(xcompEvent);
				
				if(xcompEvent != null) {
					findConjEventsWithBasicDep(sent, xverb.id, mention, options);
				}
			}
		}
	}
	
	protected void findXCOMPEventForCopulaEvent(Sentence sent, int copulaPos, int acompPos, Mention mention, Options options) {
		//be acomp to do ...
		if(acompPos + 2 < sent.length() && sent.getLexicon(acompPos + 1).form.equals("to")) {
			Lexicon xverb = sent.getLexicon(acompPos + 2);
			if(xverb.basic_head == acompPos && xverb.basic_deprel.equals("xcomp")) {
				Event xcompEvent = null;
				String predicate = xverb.lemma;
				if(edict.englishVerbs.contains(predicate)) {
					xcompEvent = new Event(mention.sentID, sent, xverb.id, 
							addParticle(predicate, xverb.id, sent), "nsubj", edict);
				}
				else if(edict.englishVerbs.contains(normalize(predicate))) {
					xcompEvent = new Event(mention.sentID, sent, xverb.id, 
							addParticle(normalize(predicate), xverb.id, sent), "nsubj", edict);
				}
				mention.addEvent(xcompEvent);
				mention.setMainEvent(xcompEvent, true);
				this.addEvent2Set(xcompEvent);
				
				if(xcompEvent != null) {
					findConjEventsWithBasicDep(sent, xverb.id, mention, options);
				}
			}
		}
	}
	
	protected void findConjEventsWithBasicDep(Sentence sent, int verbPos, Mention mention, Options options) {
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
						conjEvent = new Event(mention.sentID, sent, i, 
								addParticle(lexi.lemma, i, sent), "nsubj", edict);
					}
					else if(edict.englishVerbs.contains(normalize(lexi.lemma))) {
						conjEvent = new Event(mention.sentID, sent, i, 
								addParticle(normalize(lexi.lemma), i, sent), "nsubj", edict);
					}
					else if(lexi.lemma.equals("$")) {
						conjEvent = new Event(mention.sentID, sent, i, "be_$", "nsubj", edict);
					}
					else if(this.isNumber(lexi.lemma)) {
						conjEvent = new Event(mention.sentID, sent, i, "be_<num>", "nsubj", edict);
					}
//					else {
//						unknownVerbs.add(lexi.lemma);
//					}
					mention.addEvent(conjEvent);
					mention.setMainEvent(conjEvent, false);
					this.addEvent2Set(conjEvent);
					findXCOMPEventWithBasicDep(sent, i, mention, options);
					findConjEventsWithBasicDep(sent, i, mention, options);
					prev = i;
				}
				else {
					break;
				}
			}
		}
	}
}
