package cmucoref.mention;

public class Event {
	public int sentID;
	public int predPosition;
	public String predicate = null;
	public String grammaticRole = null;
	
	public Event() {
		sentID = -1;
		predPosition = 0;
	}
	
	public Event(int sentID, int predPosition, String predicate, String role) {
		this.sentID = sentID;
		this.predPosition = predPosition;
		this.predicate = predicate;
		this.grammaticRole = role;
	}
	
	public boolean equals(Event event) {
		return this.sentID == event.sentID && this.predPosition == event.predPosition 
				&& this.grammaticRole.equals(event.grammaticRole);
	}
	
	public boolean hasSamePredicate(Event event) {
		return this.sentID == event.sentID && this.predPosition == event.predPosition;
	}
}
