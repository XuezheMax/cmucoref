package cmucoref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.io.AnnotatedDocumentReader;
import cmucoref.io.DocumentReader;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.extractor.StanfordMentionExtractor;
import cmucoref.model.Options;

import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.pipeline.WhitespaceTokenizerAnnotator;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.NERCombinerAnnotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class Test {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		WhitespaceTokenizerAnnotator tokenAnnotator = null;
		POSTaggerAnnotator posAnnotator = null;
		MorphaAnnotator lemmaAnnotator = null;
		NERCombinerAnnotator nerAnnotator = null;
		ParserAnnotator parserAnnotator = null;
		
		Properties properties = new Properties();
		properties.setProperty("annotators", "parse");
		properties.setProperty("parse.buildgraphs", Boolean.FALSE.toString());
		
		tokenAnnotator = new WhitespaceTokenizerAnnotator(properties);
		posAnnotator = new POSTaggerAnnotator(false);
		lemmaAnnotator = new MorphaAnnotator(false);
		String[] models = new String[] {DefaultPaths.DEFAULT_NER_THREECLASS_MODEL, 
				DefaultPaths.DEFAULT_NER_MUC_MODEL, 
				DefaultPaths.DEFAULT_NER_CONLL_MODEL};
		nerAnnotator = new NERCombinerAnnotator(new NERClassifierCombiner(models), false);
		parserAnnotator = new ParserAnnotator("parse", properties);
		
//		int size = 1;
		List<CoreMap> corefDoc = new ArrayList<CoreMap>();
//		
//		String[] strs = {"John", "is", "a", "boy", "."};
//		
//		for(int i = 0; i < size; ++i){
//			int length = 5;
//			//stanford sentence annotation
//			CoreMap sent = new ArrayCoreMap();
//			//stanford tokens annotation
//			List<CoreLabel> tokens = new ArrayList<CoreLabel>();
//			for(int j = 0; j < length; ++j){
//				CoreLabel token = new CoreLabel();
//				//token.set(IndexAnnotation.class, j);
//				token.set(TextAnnotation.class, strs[j]);
//				tokens.add(token);
//			}
//			sent.set(TokensAnnotation.class, tokens);
//			
//			//add sent to doc
//			corefDoc.add(sent);
//		}
		Annotation aa = new Annotation("I am a boy .");
		//tokenizer 
		tokenAnnotator.annotate(aa);
		List<CoreLabel> tokens = aa.get(TokensAnnotation.class);
		CoreMap s = new ArrayCoreMap();
		s.set(TokensAnnotation.class, tokens);
		corefDoc.add(s);
		
		// annotate document
		Annotation anno = new Annotation(corefDoc);
		//annotate POS
		posAnnotator.annotate(anno);
		//annotate lemma
		lemmaAnnotator.annotate(anno);
		//annotate NER
		nerAnnotator.annotate(anno);
		//annotate penn tree
		parserAnnotator.annotate(anno);
		
//		Properties props = new Properties();
//	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
//	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//	    
//	    // read some text in the text variable
//	    String text = "John is a boy ."; // Add your text here!
//	    
//	    // create an empty Annotation just with the given text
//	    Annotation anno = new Annotation(text);
//	    
//	    // run all Annotators on this text
//	    pipeline.annotate(anno);
		
		List<CoreMap> sents = anno.get(SentencesAnnotation.class);
		
		for(CoreMap sent: sents) {
			// this is the parse tree of the current sentence
			for (CoreLabel token: sent.get(TokensAnnotation.class)) {
				//int index = token.get(IndexAnnotation.class);
				// this is the text of the token
				String form = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String postag = token.get(PartOfSpeechAnnotation.class);
				// this is the lemma of the token
				String lemma = token.getString(LemmaAnnotation.class);
				// this is the NER label of the token
				String ner = token.get(NamedEntityTagAnnotation.class);
				
				System.out.println(form + " " + lemma + " " + postag + " " + ner);
			}
			// this is the parse tree of the current sentence
			Tree tree = sent.get(TreeAnnotation.class);
			System.out.println(tree.pennString());
			System.out.println(tree.toString());
			//English grammatical structure (make Copula head)
			EnglishGrammaticalStructure egs = new EnglishGrammaticalStructure(tree, 
					new PennTreebankLanguagePack().punctuationWordRejectFilter(), 
					new SemanticHeadFinder(false), true);
			
			List<TypedDependency> basicTypedDependencies = egs.typedDependencies(false);
			
			List<TypedDependency> collapsedTypedDependencies = egs.typedDependenciesCollapsed(true);
			System.out.println("------------------");
		}
	}

}
