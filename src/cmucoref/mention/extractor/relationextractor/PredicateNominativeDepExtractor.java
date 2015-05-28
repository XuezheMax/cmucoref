package cmucoref.mention.extractor.relationextractor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class PredicateNominativeDepExtractor extends RelationExtractor{

	private static final Set<String> copulas = new HashSet<String>(Arrays.asList("appear", "be", "become", "seem", "remain"));
	
	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		for(Mention mention1 : mentions){
			//ignore list member mentions
			if(mention1.getBelognTo() != null){
				continue;
			}
			
			for(Mention mention2 : mentions){
				//ignore list member mentions
				if(mention2.getBelognTo() != null){
					continue;
				}
				
				if(mention1 == mention2 || mention1.headIndex == mention2.headIndex){
					continue;
				}
				
				if(mention1.headIndex < mention2.headIndex 
						&& mention1.headword.basic_head == mention2.headword.basic_head){
					int head = mention1.headword.basic_head;
					if(copulas.contains(sent.getLexicon(head).lemma)
						&& mention1.headword.collapsed_deprel.equals("nsubj")
						&& mention2.headword.collapsed_deprel.equals("xcomp")){
						relationSet.add(new Pair<Integer, Integer>(mention2.mentionID, mention1.mentionID));
					}
				}
			}
		}
		return relationSet;
	}

}
