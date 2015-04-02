package cmucoref.mention;

import java.util.Comparator;

public class MentionComparatorPostTreeOrder implements Comparator<Mention>{

	@Override
	public int compare(Mention m1, Mention m2) {
		if(m1.endIndex < m2.endIndex){
			return -1;
		}
		else if(m1.endIndex == m2.endIndex){
			return m1.startIndex < m2.startIndex ? 1 : (m1.startIndex == m2.startIndex ? 0 : -1);
		}
		else{
			return 1;
		}
	}

}
