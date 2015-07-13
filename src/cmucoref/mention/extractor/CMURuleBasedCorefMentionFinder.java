package cmucoref.mention.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.IntPair;

public class CMURuleBasedCorefMentionFinder extends RuleBasedCorefMentionFinder {
	
	public CMURuleBasedCorefMentionFinder() {
		super(false);
	}
	
	@Override
	public List<List<Mention>> extractPredictedMentions(Annotation doc, int maxID, Dictionaries dict) {
		
		List<List<Mention>> predictedMentions = new ArrayList<List<Mention>>();
		for (CoreMap s : doc.get(CoreAnnotations.SentencesAnnotation.class)) {

			List<Mention> mentions = new ArrayList<Mention>();
			predictedMentions.add(mentions);
			Set<IntPair> mentionSpanSet = Generics.newHashSet();
			Set<IntPair> namedEntitySpanSet = Generics.newHashSet();

			extractPremarkedEntityMentions(s, mentions, mentionSpanSet, namedEntitySpanSet);
			extractNamedEntityMentions(s, mentions, mentionSpanSet, namedEntitySpanSet);
			extractNPorPRP(s, mentions, mentionSpanSet, namedEntitySpanSet);
			extractEnumerations(s, mentions, mentionSpanSet, namedEntitySpanSet);
			findHead(s, mentions);
			setBarePlural(mentions);
			removeSpuriousMentions(s, mentions, dict);
		}

		// assign mention IDs
		if(assignIds) {
			assignMentionIDs(predictedMentions, maxID);
		}
		return predictedMentions;
	}
	
	protected static void extractNamedEntityMentions(CoreMap s, List<Mention> mentions, Set<IntPair> mentionSpanSet, Set<IntPair> namedEntitySpanSet) {
		List<CoreLabel> sent = s.get(CoreAnnotations.TokensAnnotation.class);
		SemanticGraph dependency = s.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
		String preNE = "O";
		int beginIndex = -1;
		for(CoreLabel w : sent) {
			String nerString = w.get(CoreAnnotations.NamedEntityTagAnnotation.class);
			if(!nerString.equals(preNE)) {
				int endIndex = w.get(CoreAnnotations.IndexAnnotation.class) - 1;
				if(!preNE.matches("O|QUANTITY|CARDINAL|PERCENT|DURATION|TIME|SET")) {
					if(w.get(CoreAnnotations.TextAnnotation.class).equals("'s")) endIndex++;
					IntPair mSpan = new IntPair(beginIndex, endIndex);
					// Need to check if beginIndex < endIndex because, for
					// example, there could be a 's mislabeled by the NER and
					// attached to the previous NER by the earlier heuristic
					if(beginIndex < endIndex && !mentionSpanSet.contains(mSpan)) {
						int dummyMentionId = -1;
						Mention m = new Mention(dummyMentionId, beginIndex, endIndex, dependency, new ArrayList<CoreLabel>(sent.subList(beginIndex, endIndex)));
						mentions.add(m);
						mentionSpanSet.add(mSpan);
						namedEntitySpanSet.add(mSpan);
					}
				}
				beginIndex = endIndex;
				preNE = nerString;
			}
		}
		// NE at the end of sentence
		if(!preNE.matches("O|QUANTITY|CARDINAL|PERCENT|DURATION|TIME|SET")) {
			IntPair mSpan = new IntPair(beginIndex, sent.size());
			if(!mentionSpanSet.contains(mSpan)) {
				int dummyMentionId = -1;
				Mention m = new Mention(dummyMentionId, beginIndex, sent.size(), dependency, new ArrayList<CoreLabel>(sent.subList(beginIndex, sent.size())));
				mentions.add(m);
				mentionSpanSet.add(mSpan);
				namedEntitySpanSet.add(mSpan);
			}
		}
		
		removeSpuriousNamedEntityMentions(s, mentions, mentionSpanSet, namedEntitySpanSet);
	}
	
	private static final Set<String> dates = new HashSet<String>(Arrays.asList(
			"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "yesterday", "tomorrow", "today", 
			"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"));
	private static void removeSpuriousNamedEntityMentions(CoreMap s, List<Mention> mentions, Set<IntPair> mentionSpanSet, Set<IntPair> namedEntitySpanSet) {
		Set<Mention> remove = Generics.newHashSet();
		Set<IntPair> removeSpan = Generics.newHashSet();
		for(Mention m : mentions) {
			String nerTag = m.originalSpan.get(0).get(CoreAnnotations.NamedEntityTagAnnotation.class);
			String posTag = m.originalSpan.get(0).get(CoreAnnotations.PartOfSpeechAnnotation.class);
			String word = m.originalSpan.get(0).get(CoreAnnotations.TextAnnotation.class).toLowerCase();
			IntPair mSpan = new IntPair(m.startIndex, m.endIndex);
			if(nerTag.equals("DATE")) {
				if(m.originalSpan.size() > 1) {
					remove.add(m);
					removeSpan.add(mSpan);
				}
				else if(!dates.contains(word) && !posTag.equals("CD")) {
					remove.add(m);
					removeSpan.add(mSpan);
				}
			}
		}
		
		mentions.removeAll(remove);
		mentionSpanSet.removeAll(removeSpan);
		namedEntitySpanSet.removeAll(removeSpan);
	}
	
	private static final TregexPattern npOrPrpMentionPattern = TregexPattern.compile("/^(?:NP|PRP)/");
	protected static void extractNPorPRP(CoreMap s, List<Mention> mentions, Set<IntPair> mentionSpanSet, Set<IntPair> namedEntitySpanSet) {
		List<CoreLabel> sent = s.get(CoreAnnotations.TokensAnnotation.class);
		Tree tree = s.get(TreeCoreAnnotations.TreeAnnotation.class);
		tree.indexLeaves();
		SemanticGraph dependency = s.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);

		TregexPattern tgrepPattern = npOrPrpMentionPattern;
		TregexMatcher matcher = tgrepPattern.matcher(tree);
		while (matcher.find()) {
			Tree t = matcher.getMatch();
			List<Tree> mLeaves = t.getLeaves();
			int beginIdx = ((CoreLabel)mLeaves.get(0).label()).get(CoreAnnotations.IndexAnnotation.class)-1;
			int endIdx = ((CoreLabel)mLeaves.get(mLeaves.size()-1).label()).get(CoreAnnotations.IndexAnnotation.class);
			IntPair mSpan = new IntPair(beginIdx, endIdx);
			if(!mentionSpanSet.contains(mSpan) && !insideNE(mSpan, namedEntitySpanSet)) {
				int dummyMentionId = -1;
				Mention m = new Mention(dummyMentionId, beginIdx, endIdx, dependency, new ArrayList<CoreLabel>(sent.subList(beginIdx, endIdx)), t);
				mentions.add(m);
				mentionSpanSet.add(mSpan);
			}
		}
	}
	
	private static boolean insideNE(IntPair mSpan, Set<IntPair> namedEntitySpanSet) {
		for (IntPair span : namedEntitySpanSet) {
			if(span.get(0) <= mSpan.get(0) && mSpan.get(1) <= span.get(1)) {
				return true;
			}
		}
		return false;
	}
	
//	private static final Set<String> determiners = new HashSet<String>(Arrays.asList("these", "those", "this", "that"));
	private static final Set<String> nonWords = new HashSet<String>(Arrays.asList("mm", "hmm", "ahem", "um", "uh", "%mm", "%hmm", "%ahem", "%um", "%uh"));
	private static final Set<String> negWords = new HashSet<String>(Arrays.asList("nobody", "none", "nothing", "no", "not"));
	protected static void removeSpuriousMentions(CoreMap s, List<Mention> mentions, Dictionaries dict) {
		Tree tree = s.get(TreeCoreAnnotations.TreeAnnotation.class);
//		List<CoreLabel> sent = s.get(CoreAnnotations.TokensAnnotation.class);
		Set<Mention> remove = Generics.newHashSet();
		
		for(Mention m : mentions) {
			String headNE = m.headWord.get(CoreAnnotations.NamedEntityTagAnnotation.class);
			// pleonastic it
			if(isPleonastic(m, tree)) {
				remove.add(m);
			}

			// non word such as 'hmm'
			if(nonWords.contains(m.headString)) {
				remove.add(m);
			}
			
			//negative mentions
			if(negWords.contains(m.headString) 
				|| (m.originalSpan.size() > 0 
				&& negWords.contains(m.originalSpan.get(0).get(CoreAnnotations.TextAnnotation.class).toLowerCase(Locale.ENGLISH)))) {
				remove.add(m);
			}

			/*
			// quantRule : not starts with 'any', 'all' etc
			if (m.originalSpan.size() > 0 
				&& dict.quantifiers.contains(m.originalSpan.get(0).get(CoreAnnotations.TextAnnotation.class).toLowerCase(Locale.ENGLISH))) {
				remove.add(m);
			}
			*/

			/*
			// partitiveRule
			if (partitiveRule(m, sent, dict)) {
				remove.add(m);
			}
			*/
			
			/*
			String headPOS = m.headWord.get(CoreAnnotations.PartOfSpeechAnnotation.class);
			// bareNPRule
			if(headPOS.equals("NN") && !dict.temporals.contains(m.headString) && !m.headString.equals("today")
					&& !bareNNs.contains(m.headString) && m.originalSpan.size() == 1) {
				remove.add(m);
			}
			*/
			
			/*
			//remove this, that, these and those
			if(m.originalSpan.size() == 1 && determiners.contains(m.headString)) {
				remove.add(m);
			}
			*/

			if (m.headString.equals("%")) {
				remove.add(m);
			}
			
			if (headNE.equals("PERCENT") || headNE.equals("MONEY")) {
				remove.add(m);
			}

			// adjective form of nations
			if (dict.isAdjectivalDemonym(m.spanToString())) {
				remove.add(m);
			}

			// stop list (e.g., U.S., there)
			if (inStopList(m)) {
				remove.add(m);
			}
		}
		
		mentions.removeAll(remove);
	}
	
	private static boolean inStopList(Mention m) {
		String mentionSpan = m.spanToString().toLowerCase(Locale.ENGLISH);
		if (mentionSpan.equals("u.s.") || mentionSpan.equals("u.k.")
				|| mentionSpan.equals("u.s.s.r")) {
			return true;
		}
		if (mentionSpan.equals("there") || mentionSpan.startsWith("etc.")
				|| mentionSpan.equals("ltd.")) {
			return true;
		}
		if (mentionSpan.startsWith("'s ")) {
			return true;
		}
		if (mentionSpan.endsWith("etc.")) {
			return true;
		}

		return false;
	}
	
//	private static final Set<String> parts = new HashSet<String>(Arrays.asList("hundreds", "thousands", "millions", "billions", "tens", "dozens", "group", "groups", "bunch", "a number", "numbers", "a pinch", "a total"));

//	private static boolean partitiveRule(Mention m, List<CoreLabel> sent, Dictionaries dict) {
//		return m.startIndex >= 2
//				&& sent.get(m.startIndex - 1).get(CoreAnnotations.TextAnnotation.class).equalsIgnoreCase("of")
//				&& sent.get(m.startIndex - 2).get(CoreAnnotations.TextAnnotation.class).equalsIgnoreCase("all");
//	}
	
	/** Check whether pleonastic 'it'. E.g., It is possible that ... */
	private static final TregexPattern[] pleonasticPatterns = getPleonasticPatterns();
	private static boolean isPleonastic(Mention m, Tree tree) {
		if (!m.spanToString().equalsIgnoreCase("it")) {
			return false;
		}
		for (TregexPattern p : pleonasticPatterns) {
			if (checkPleonastic(m, tree, p)) {
				// SieveCoreferenceSystem.logger.fine("RuleBasedCorefMentionFinder: matched pleonastic pattern '" + p + "' for " + tree);
				return true;
			}
		}
		return false;
	}

	private static TregexPattern[] getPleonasticPatterns() {
		final String[] patterns = {
				// cdm 2013: I spent a while on these patterns. I fixed a syntax error in five patterns ($.. split with space), so it now shouldn't exception in checkPleonastic. This gave 0.02% on CoNLL11 dev
				// I tried some more precise patterns but they didn't help. Indeed, they tended to hurt vs. the higher recall patterns.

//				"@NP <: (DT=m1) $. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@VP < (VBN $.. @S|SBAR))))",// in practice, go with this one (best results)
//				"NP <: (DT=m1) $. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP $.. (/S|SBAR/))))",
//				"NP <: (DT=m1) $. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP < (/S|SBAR/))))",
				
				"@NP < (PRP=m1) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@VP < (VBN $.. @S|SBAR))))",// in practice, go with this one (best results)
				"@NP < (NP < (PRP=m1)) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@VP < (VBN $.. @S|SBAR))))", // by Max

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (ADJP $.. (/S|SBAR/))))",
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (ADJP $.. (/S|SBAR/))))", //by Max
				
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (ADJP < (/S|SBAR/))))",
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (ADJP < (/S|SBAR/))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (NP < (NP $.. /S|SBAR/))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (NP < (NP < (NP $.. /S|SBAR/)))))", // by Max
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (NP < (NP $.. /S|SBAR/))))", //by Max
				
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became|takes|took)/) $.. (NP $.. (/S|SBAR/))))", // by Max
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became|takes|took)/) $.. (NP $.. (/S|SBAR/))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (NP $.. ADVP $.. /S|SBAR/)))",
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:is|was|becomes|become|became)/) $.. (NP $.. ADVP $.. /S|SBAR/)))", // by Max
				
				// with MD
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (VP < (VBN $.. /S|SBAR/))))))",
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (VP < (VBN $.. /S|SBAR/))))))", // by Max

				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP $.. (/S|SBAR/))))))", // extraposed. OK 1/2 correct; need non-adverbial case
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP $.. (/S|SBAR/))))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP < (/S|SBAR/))))))", // OK: 3/3 good matches on dev; but 3/4 wrong on WSJ
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP < (/S|SBAR/))))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP < (NP $.. /S|SBAR/))))))",
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP < (NP $.. /S|SBAR/))))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become|take)/) $.. (NP $.. /S|SBAR/)))))", // by Max
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become|take)/) $.. (NP $.. /S|SBAR/)))))", // by Max
				
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP $.. ADVP $.. /S|SBAR/)))))",
				"NP < (NP < (PRP=m1)) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP $.. ADVP $.. /S|SBAR/)))))", // by Max

				//some other VBs
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:seems|seemed|appeared|appears|means|follows)/) $.. /S|SBAR/))",
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:seems|seemed|appeared|appears|means|follows)/) $.. /S|SBAR/))", // by Max

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:happens|happened)/) $.. SBAR))",
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:happens|happened)/) $.. SBAR))", // by Max
				
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:turns|turned)/) $.. PRT $.. /S|SBAR/))", 
				"NP < (NP < (PRP=m1)) $.. (VP < ((/^V.*/ < /^(?:turns|turned)/) $.. PRT $.. /S|SBAR/))", // by Max
				
				//eg. make it safer to do sth
				"VP $.. (S < ((NP < (PRP=m1)) $.. ADJP $.. /S|SBAR/))", // by Max
				"VP $.. (S < ((NP < (NP < (PRP=m1))) $.. ADJP $.. /S|SBAR/))" // by Max
		};

		TregexPattern[] tgrepPatterns = new TregexPattern[patterns.length];
		for (int i = 0; i < tgrepPatterns.length; i++) {
			tgrepPatterns[i] = TregexPattern.compile(patterns[i]);
		}
		return tgrepPatterns;
	}

	private static boolean checkPleonastic(Mention m, Tree tree, TregexPattern tgrepPattern) {
		try {
			TregexMatcher matcher = tgrepPattern.matcher(tree);
			while (matcher.find()) {
				Tree np1 = matcher.getNode("m1");
				if (((CoreLabel)np1.label()).get(CoreAnnotations.BeginIndexAnnotation.class)+1 == m.headWord.get(CoreAnnotations.IndexAnnotation.class)) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
