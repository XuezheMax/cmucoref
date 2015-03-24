package cmucoref.mention.extractor.relationextractor;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class ListMemberRelationExtractor extends RelationExtractor {
	
	public ListMemberRelationExtractor(){}

	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();	
		for(Mention mention1 : mentions){
			Lexicon headword1 = mention1.headword;
			if(headword1.collapsed_deprel.startsWith("conj_")){
				int parIndex = headword1.collapsed_head;
				for(Mention mention2 : mentions){
					if(mention2.headIndex == parIndex){
						if(mention2.cover(mention1) && mention2.belongTo == null){
							relationSet.add(new Pair<Integer, Integer>(mention1.headIndex, mention2.headIndex));
						}
					}
				}
			}
		}
		return relationSet;
	}
	
	
}
