package cmucoref.mention;

public class MentionComparatorSyntacticOrder extends MentionComparatorHeadIndexWithSpeakerOrder {

	@Override
	public int compare(Mention m1, Mention m2) {
		if(m1.speakerTo(m2)) {
			return -1;
		}
		else if(m2.speakerTo(m1)) {
			return 1;
		}
		else if(afterUtterance(m1, m2)) {
			return -1;
		}
		else if(afterUtterance(m2, m1)) {
			return 1;
		}
		else if(m1.sentID < m2.sentID) {
			return -1;
		}
		else if(m1.sentID > m2.sentID) {
			return 1;
		}
		else if(m2.apposTo(m1) || m2.predNomiTo(m1) || m2.roleApposTo(m1)) {
			return -1;
		}
		else if(m1.apposTo(m2) || m1.predNomiTo(m2) || m1.roleApposTo(m2)) {
			return 1;
		}
		else if(insideListMember(m1, m2)) {
			return -1;
		}
		else if(insideListMember(m2, m1)) {
			return 1;
		}
		else if(syntacticParent(m1, m2)) {
			return -1;
		}
		else if(syntacticParent(m2, m1)) {
			return 1;
		}
		else if(m1.headIndex < m2.headIndex){
			return -1;
		}
		else if(m1.headIndex == m2.headIndex){
			int span1 = m1.endIndex - m1.startIndex;
			int span2 = m2.endIndex - m2.startIndex;
			return span1 < span2 ? 1 : (span1 == span2 ? 0 : -1);
		}
		else{
			return 1;
		}
	}
	
	// event of m1 is parent of event of m2 
	protected boolean syntacticParent(Mention m1, Mention m2) {
		for(Event e1 : m1.getEventSet()) {
			for(Event e2 : m2.getEventSet()) {
				if(e2.isChildOf(e1)) {
					return true;
				}
			}
		}
		return false;
	}
}
