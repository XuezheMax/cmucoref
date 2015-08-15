package cmucoref.mention;

import java.util.HashSet;
import java.util.Set;

import cmucoref.document.Sentence;

public class Event {
	public int sentID;
	public int predPosition;
	public String predicate = null;
	public String grammaticRole = null;
	private Sentence sent = null;
	private Set<Integer> parentSet = new HashSet<Integer>();
	
//	public static Set<String> roleSet = new HashSet<String>();
	
	public Event() {
		sentID = -1;
		predPosition = 0;
	}
	
	private void getParents(Sentence sent, int predPosition) {
		int parent = sent.getLexicon(predPosition).basic_head;
		while(parent > 0) {
			parentSet.add(parent);
			parent = sent.getLexicon(parent).basic_head;
		}
	}
	
	private static String normalizeGrammaticRole(String role, EventDictionaries edict) {
		if(role.equals("agent")) {
			return "prep_by";
		}
		else if(role.equals("xsubj")) {
			return "nsubj";
		}
		else if(role.equals("pobj")) {
			return "dobj";
		}
		else if(role.endsWith("mod")) {
			return "xmod";
		}
		else if(role.startsWith("prep_")) {
			String prep = role.substring(5);
			if(edict.englishPreps.contains(prep)) {
				return role;
			}
			else {
				return "prep_<other>";
			}
		}
		else {
			return role;
		}
	}
	
	public boolean isChildOf(Event e) {
		return this.parentSet.contains(e.predPosition);
	}
	
	public Event(int sentID, Sentence sent, int predPosition, String predicate, String role, boolean isPart, EventDictionaries edict) {
		this.sentID = sentID;
		this.sent = sent;
		this.predPosition = predPosition;
		getParents(this.sent, this.predPosition);
		
		this.predicate = predicate;
		this.grammaticRole = normalizeGrammaticRole(role, edict);
		
		if(isPart) {
			this.grammaticRole = "part_of[" + this.grammaticRole + "]";
		}
		
//		roleSet.add(this.grammaticRole);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Event)) {
			return false;
		}
		Event event = (Event) obj;
		
		return this.sentID == event.sentID && this.predPosition == event.predPosition 
				&& this.predicate.equals(event.predicate)
				&& this.grammaticRole.equals(event.grammaticRole);
	}
	
	@Override
	public int hashCode() {
		return sentID ^ predPosition ^ predicate.hashCode() ^ grammaticRole.hashCode();
	}
	
	public boolean hasSamePredicate(Event event) {
		return this.sentID == event.sentID && this.predPosition == event.predPosition 
				&& this.predicate.equals(event.predicate);
	}
	
	public boolean samePredicateWithSubjObj(Event event) {
		return this.hasSamePredicate(event) 
				&& this.grammaticRole.startsWith("nsubj") 
						&& (event.grammaticRole.startsWith("prep") || event.grammaticRole.endsWith("obj"));
	}
	
	public String toString() {
		return "[" + predPosition + ":" + predicate + ", " + grammaticRole + "]";
	}
	
	public String toFeature() {
		return "[" + predicate + ", " + grammaticRole + "]";
	}
}
