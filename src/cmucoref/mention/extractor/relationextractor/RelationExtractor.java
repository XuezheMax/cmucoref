package cmucoref.mention.extractor.relationextractor;

import cmucoref.util.*;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;

import java.util.Set;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;

public abstract class RelationExtractor {
	public static RelationExtractor createExtractor(String readerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		return (RelationExtractor) Class.forName(readerClassName).newInstance();
	}
	
	public abstract Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions);
	
	protected void findTreePattern(Tree tree, TregexPattern tgrepPattern, Set<Pair<Integer, Integer>> foundPairs){
		try {
			TregexMatcher m = tgrepPattern.matcher(tree);
			HeadFinder headFinder = new SemanticHeadFinder();
			while (m.find()) {
				Tree t = m.getMatch();
				Tree np1 = m.getNode("m1");
				Tree np2 = m.getNode("m2");
				Tree np3 = null;
				if(tgrepPattern.pattern().contains("m3")) np3 = m.getNode("m3");
				addFoundPair(np1, np2, t, headFinder, foundPairs);
				if(np3!=null) addFoundPair(np2, np3, t, headFinder, foundPairs);
			}
		} catch (Exception e) {
			// shouldn't happen....
			throw new RuntimeException(e);
		}
	}
	
	private void addFoundPair(Tree np1, Tree np2, Tree t, HeadFinder headFinder, Set<Pair<Integer, Integer>> foundPairs) {
		Tree head1 = np1.headTerminal(headFinder);
		Tree head2 = np2.headTerminal(headFinder);
		int h1 = ((CoreMap) head1.label()).get(CoreAnnotations.IndexAnnotation.class) - 1;
		int h2 = ((CoreMap) head2.label()).get(CoreAnnotations.IndexAnnotation.class) - 1;
		Pair<Integer, Integer> p = new Pair<Integer, Integer>(h1, h2);
		foundPairs.add(p);
	}
}
