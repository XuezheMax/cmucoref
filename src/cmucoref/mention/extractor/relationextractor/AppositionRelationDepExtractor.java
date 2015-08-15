package cmucoref.mention.extractor.relationextractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class AppositionRelationDepExtractor extends RelationExtractor {

//	private static final TregexPattern appositionPattern = TregexPattern.compile("NP=m1 < (NP $. NP=m2)");
	
	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		for(Mention mention : mentions){
//			Lexicon headword = mention.headword;
			Lexicon headword = sent.getLexicon(mention.originalHeadIndex);
			if(headword.basic_deprel.equals("appos")){
//				if(mention.headIndex > headword.basic_head){
				if(mention.originalHeadIndex > headword.basic_head) {
					relationSet.add(new Pair<Integer, Integer>(mention.headIndex, headword.basic_head));
				}
			}
		}
		return relationSet;
	}

}
