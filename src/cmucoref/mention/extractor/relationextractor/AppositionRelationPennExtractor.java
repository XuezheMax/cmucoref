package cmucoref.mention.extractor.relationextractor;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class AppositionRelationPennExtractor extends RelationExtractor{
	
	private static final TregexPattern appositionPattern = TregexPattern.compile("NP=m1 < (NP=m2 $.. (/,/ $.. NP=m3))");
	private static final TregexPattern appositionPattern2 = TregexPattern.compile("NP=m1 < (NP=m2 $.. (/,/ $.. (SBAR < (WHNP < WP|WDT=m3))))");
	private static final TregexPattern appositionPattern3 = TregexPattern.compile("/^NP(?:-TMP|-ADV)?$/=m1 < (NP=m2 $- /^,$/ $-- NP=m3 !$ CC|CONJP)");
	private static final TregexPattern appositionPattern4 = TregexPattern.compile("/^NP(?:-TMP|-ADV)?$/=m1 < (PRN=m2 < (NP < /^NNS?|CD$/ $-- /^-LRB-$/ $+ /^-RRB-$/))");

	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		PennTreeReader pennReader = new PennTreeReader(new StringReader(sent.getPennTree()));
		try {
			Tree tree = pennReader.readTree();
			pennReader.close();
			findTreePattern(tree, appositionPattern, relationSet);
			findTreePattern(tree, appositionPattern2, relationSet);
			findTreePattern(tree, appositionPattern3, relationSet);
			findTreePattern(tree, appositionPattern4, relationSet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return relationSet;
	}

}
