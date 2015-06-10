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
	
	private static final Set<String> nonWords = new HashSet<String>(Arrays.asList("mm", "hmm", "ahem", "um", "uh", "%mm", "%hmm", "%ahem", "%um", "%uh"));
	private static final Set<String> bareNNs = new HashSet<String>(Arrays.asList("something", "nothing", "everything", "anything", 
			"somebody", "everybody", "nobody", "anybody", "everyone", "someone", "anyone"));
	
	protected static void removeSpuriousMentions(CoreMap s, List<Mention> mentions, Dictionaries dict) {
		Tree tree = s.get(TreeCoreAnnotations.TreeAnnotation.class);
		List<CoreLabel> sent = s.get(CoreAnnotations.TokensAnnotation.class);
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

			// quantRule : not starts with 'any', 'all' etc
			if (m.originalSpan.size() > 0 
				&& dict.quantifiers.contains(m.originalSpan.get(0).get(CoreAnnotations.TextAnnotation.class).toLowerCase(Locale.ENGLISH))) {
				//remove.add(m);
			}

			/*
			// partitiveRule
			if (partitiveRule(m, sent, dict)) {
				remove.add(m);
			}
			
			String headPOS = m.headWord.get(CoreAnnotations.PartOfSpeechAnnotation.class);
			// bareNPRule
			if(headPOS.equals("NN") && !dict.temporals.contains(m.headString) && !m.headString.equals("today")
					&& !bareNNs.contains(m.headString) && m.originalSpan.size() == 1) {
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

		// nested mention with shared headword (except apposition, enumeration): pick larger one
		for (Mention m1 : mentions){
			for (Mention m2 : mentions){
				if (m1==m2 || remove.contains(m1) || remove.contains(m2)) continue;
				if (m1.sentNum==m2.sentNum && m1.headWord==m2.headWord && m2.insideIn(m1)) {
					if (m2.endIndex < sent.size() && (sent.get(m2.endIndex).get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(",")
							|| sent.get(m2.endIndex).get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("CC"))) {
						continue;
					}
					remove.add(m2);
				}
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
	
	private static final Set<String> parts = new HashSet<String>(Arrays.asList("hundreds", "thousands", "millions", "billions", "tens", "dozens", "group", "groups", "bunch", "a number", "numbers", "a pinch", "a total"));

	private static boolean partitiveRule(Mention m, List<CoreLabel> sent, Dictionaries dict) {
		return (m.startIndex >= 2
				&& sent.get(m.startIndex - 1).get(CoreAnnotations.TextAnnotation.class).equalsIgnoreCase("of")
				&& parts.contains(sent.get(m.startIndex - 2).get(CoreAnnotations.TextAnnotation.class).toLowerCase(Locale.ENGLISH)))
				|| (m.startIndex >= 3
				&& sent.get(m.startIndex - 1).get(CoreAnnotations.TextAnnotation.class).equalsIgnoreCase("of")
				&& sent.get(m.startIndex - 3).get(CoreAnnotations.TextAnnotation.class).equalsIgnoreCase("a")
				&& parts.contains("a " + sent.get(m.startIndex - 2).get(CoreAnnotations.TextAnnotation.class).toLowerCase(Locale.ENGLISH)));
	}
	
	/** Check whether pleonastic 'it'. E.g., It is possible that ... */
	private static final TregexPattern[] pleonasticPatterns = getPleonasticPatterns();
	private static boolean isPleonastic(Mention m, Tree tree) {
		if ( ! m.spanToString().equalsIgnoreCase("it")) {
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

				//"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (VP < (VBN $.. /S|SBAR/))))", // overmatches
				// "@NP < (PRP=m1 < it|IT|It) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@VP < (VBN < expected|hoped $.. @SBAR))))",// this one seems more accurate, but ...
				"@NP < (PRP=m1 < it|IT|It) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@VP < (VBN $.. @S|SBAR))))",// in practice, go with this one (best results)

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP $.. (/S|SBAR/))))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP < (/S|SBAR/))))",
				// "@NP < (PRP=m1 < it|IT|It) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@ADJP < (/^(?:JJ|VB)/ < /^(?i:(?:hard|tough|easi)(?:er|est)?|(?:im|un)?(?:possible|interesting|worthwhile|likely|surprising|certain)|disappointing|pointless|easy|fine|okay)$/) [ < @S|SBAR | $.. (@S|SBAR !< (IN !< for|For|FOR|that|That|THAT)) ] )))", // does worse than above 2 on CoNLL11 dev

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP < /S|SBAR/)))",
				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP $.. ADVP $.. /S|SBAR/)))",
				// "@NP < (PRP=m1 < it|IT|It) $.. (@VP < (/^V.*/ < /^(?i:is|was|be|becomes|become|became)$/ $.. (@NP $.. @ADVP $.. @SBAR)))", // cleft examples, generalized to not need ADVP; but gave worse CoNLL12 dev numbers....

				// these next 5 had buggy space in "$ ..", which I fixed
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (VP < (VBN $.. /S|SBAR/))))))",

				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP $.. (/S|SBAR/))))))", // extraposed. OK 1/2 correct; need non-adverbial case
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP < (/S|SBAR/))))))", // OK: 3/3 good matches on dev; but 3/4 wrong on WSJ
				// certain can be either but relatively likely pleonastic with it ... be
				// "@NP < (PRP=m1 < it|IT|It) $.. (@VP < (MD $.. (@VP < ((/^V.*/ < /^(?:be|become)/) $.. (@ADJP < (/^JJ/ < /^(?i:(?:hard|tough|easi)(?:er|est)?|(?:im|un)?(?:possible|interesting|worthwhile|likely|surprising|certain)|disappointing|pointless|easy|fine|okay))$/) [ < @S|SBAR | $.. (@S|SBAR !< (IN !< for|For|FOR|that|That|THAT)) ] )))))", // GOOD REPLACEMENT ; 2nd clause is for extraposed ones

				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP < /S|SBAR/)))))",
				"NP < (PRP=m1) $.. (VP < (MD $.. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP $.. ADVP $.. /S|SBAR/)))))",

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:seems|appears|means|follows)/) $.. /S|SBAR/))",

				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:turns|turned)/) $.. PRT $.. /S|SBAR/))"
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
