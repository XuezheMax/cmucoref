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

public class RelativePronounRelationExtractor extends RelationExtractor{

	private static final TregexPattern relativePronounPattern = TregexPattern.compile("NP < (NP=m1 $.. (SBAR < (WHNP < WP|WDT=m2)))");
	
	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		PennTreeReader pennReader = new PennTreeReader(new StringReader(sent.getPennTree()));
		try {
			Tree tree = pennReader.readTree();
			pennReader.close();
			findTreePattern(tree, relativePronounPattern, relationSet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return relationSet;
	}

}
