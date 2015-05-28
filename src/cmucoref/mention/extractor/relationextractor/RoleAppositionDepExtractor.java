package cmucoref.mention.extractor.relationextractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class RoleAppositionDepExtractor extends RelationExtractor {

	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		for(Mention mention1 : mentions) {
			Lexicon hw1 = mention1.headword;
			if(hw1.basic_deprel.equals("nn")) {
				for(Mention mention2 : mentions) {
					if(mention2.headIndex == hw1.basic_head 
						&& mention2.startIndex == mention1.startIndex
						&& mention2.cover(mention1)) {
						relationSet.add(new Pair<Integer, Integer>(mention1.mentionID, mention2.mentionID));
					}
				}
			}
		}
		return relationSet;
	}

}
