package cmucoref.mention.extractor.relationextractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class AppositionRelationDepExtractor extends RelationExtractor{

	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		// TODO
		return relationSet;
	}

}
