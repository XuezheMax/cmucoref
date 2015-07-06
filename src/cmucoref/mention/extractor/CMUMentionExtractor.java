package cmucoref.mention.extractor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.mention.Mention;
import cmucoref.model.Options;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class CMUMentionExtractor extends MentionExtractor {

	private CMURuleBasedCorefMentionFinder mentionFinder = null;
	
	public CMUMentionExtractor() {
		super();
		mentionFinder = new CMURuleBasedCorefMentionFinder();
	}
	
	private static final Set<String> nonWords = new HashSet<String>(Arrays.asList("mm", "hmm", "ahem", "um", "uh", "%mm", "%hmm", "%ahem", "%um", "%uh"));
	
	protected void validateMentionBoundary(Document doc, Mention mention, Sentence sent, Options options) {
		// try not to have span starts or ends non words.
		int start = mention.startIndex;
		while(start < mention.headIndex && nonWords.contains(sent.getLexicon(start).form.toLowerCase())) {
			start++;
		}
		mention.startIndex = start;
		int end = mention.endIndex;
		while(end - 1 > mention.headIndex && nonWords.contains(sent.getLexicon(end - 1).form.toLowerCase())) {
			end--;
		}
		mention.endIndex = end;
		
		// try not to have span that ends with ,
		// for ontonotes nw/ docs, skip this step (annotation issues)
		if(options.OntoNotes() && doc.getFileName().startsWith("nw/")) {
			return;
		}
		
		Lexicon lastWord = sent.getLexicon(mention.endIndex - 1);
		if(lastWord.form.equals(",")) {
			mention.endIndex--;
		}
	}
	
	@Override
	public List<List<Mention>> extractPredictedMentions(Document doc, Options options) throws IOException {
		//assign annotations
		int size = doc.size();
		List<CoreMap> corefDoc = new ArrayList<CoreMap>(size);
		ArrayList<Sentence> sentences = doc.getSentences();
		for(Sentence sentence : sentences) {
			int length = sentence.length();
			//stanford sentence annotation
			CoreMap sent = new ArrayCoreMap();
			//stanford tokens annotation
			List<CoreLabel> tokens = new ArrayList<CoreLabel>();
			for(int j = 1; j < length; ++j) {
				Lexicon lexicon = sentence.getLexicon(j);
				CoreLabel token = new CoreLabel();
				token.set(IndexAnnotation.class, j);
				token.set(TextAnnotation.class, lexicon.form);
				token.set(LemmaAnnotation.class, lexicon.lemma);
				token.set(PartOfSpeechAnnotation.class, lexicon.postag);
				token.set(NamedEntityTagAnnotation.class, lexicon.ner);
				tokens.add(token);
			}
			sent.set(TokensAnnotation.class, tokens);
					
			//stanford tree annotation
			PennTreeReader pennReader = new PennTreeReader(new StringReader(sentence.getPennTree()));
			sent.set(TreeAnnotation.class, pennReader.readTree());
			pennReader.close();
			//add sent to doc
			corefDoc.add(sent);
		}
		//extract mentions
		Annotation anno = new Annotation(corefDoc);
		List<List<edu.stanford.nlp.dcoref.Mention>> stanfordMentionList = mentionFinder.extractPredictedMentions(anno, -1, dict);
				
		List<List<Mention>> mentionList = new ArrayList<List<Mention>>(stanfordMentionList.size());
		for(int i = 0; i < stanfordMentionList.size(); ++i) {
			List<edu.stanford.nlp.dcoref.Mention> stanfordMentions = stanfordMentionList.get(i);
			ArrayList<Mention> mentions = new ArrayList<Mention>(stanfordMentions.size());
			Sentence sent = doc.getSentence(i);
			for(int j = 0; j < stanfordMentions.size(); ++j) {
				Mention mention = new Mention(stanfordMentions.get(j), i);
				validateMentionBoundary(doc, mention, sent, options);
				mentions.add(mention);
			}
			
			//remove duplicated mentions
			deleteDuplicatedMentions(mentions, sent);
			
			//process mentions
			Set<Mention> remove = new HashSet<Mention>();
			if(options.extractMentionAttribute()) {
				for(Mention mention : mentions) {
					mention.process(sent, mentions, dict, remove);
				}
			}
			mentions.removeAll(remove);
			
			//remove spurious mentions
			deleteSpuriousNamedEntityMentions(mentions, sent);
			deleteSpuriousPronominalMentions(mentions, sent);
			
			Collections.sort(mentions, Mention.postTreeOrderComparator);
			
			// find syntactic relations
			if(options.extractMentionRelation()) {
				try {
					findSyntacticRelation(mentions, sent, options);
				} catch (InstantiationException | IllegalAccessException
						| ClassNotFoundException | MentionException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			Collections.sort(mentions, Mention.headIndexOrderComparator);
			
			//add mentions to mentionList
			mentionList.add(mentions);
		}
		return mentionList;
	}

}
