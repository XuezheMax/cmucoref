package cmucoref.mention;

import java.util.HashSet;
import java.util.Set;

import cmucoref.mention.eventextractor.EventExtractor;

public class Event {
	public static final String unknownRole = "<DEP>";
	public static final String unknownPredicate = "<UNKNOWN>";
	public int sentID;
	public int predPosition;
	public String predicate = null;
	public String grammaticRole = null;
	
	public Event() {
		sentID = -1;
		predPosition = 0;
	}
	
	//public static Set<String> roleSet = new HashSet<String>();
	
	public Event(int sentID, int predPosition, String predicate, String role, boolean isPart) {
		this.sentID = sentID;
		this.predPosition = predPosition;
		this.predicate = predicate;
		this.grammaticRole = role;
		if(this.grammaticRole.equals("agent")) {
			this.grammaticRole = "prep_by";
		}
		else if(this.grammaticRole.equals("xsubj")) {
			this.grammaticRole = "nsubj";
		}
		
		// normalize prep roles
		if(this.grammaticRole.startsWith("prep_")) {
			this.grammaticRole = "prep_all";
		}
		
		if(EventExtractor.acceptableGrammRole(this.grammaticRole, true)) {
			if(isPart) {
				this.grammaticRole = "part_of<" + this.grammaticRole + ">";
			}
		}
		else {
			this.grammaticRole = unknownRole;
		}
		
		//roleSet.add(this.grammaticRole);
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
}
