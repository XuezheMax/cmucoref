package cmucoref.mention;

import java.util.Comparator;

public class MentionComparatorHeadIndexOrder implements Comparator<Mention> {

	@Override
	public int compare(Mention m1, Mention m2) {
		if(m1.sentID < m2.sentID) {
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
		else if(m1.headIndex < m2.headIndex) {
			return -1;
		}
		else if(m1.headIndex == m2.headIndex) {
			int span1 = m1.endIndex - m1.startIndex;
			int span2 = m2.endIndex - m2.startIndex;
			return span1 < span2 ? 1 : (span1 == span2 ? 0 : -1);
		}
		else {
			return 1;
		}
	}
	
	/**
	 * 
	 * @param m1
	 * @param m2
	 * @return if m1 is inside a list member of m2
	 */
	protected boolean insideListMember(Mention m1, Mention m2) {
		if(m2.getListMembers() == null) {
			return false;
		}
		
		if(m1.isListMemberOf(m2)) {
			return true;
		}
		
		for(Mention member : m2.getListMembers()) {
			if(member.cover(m1)) {
				return true;
			}
		}
		return false;
	}
}
