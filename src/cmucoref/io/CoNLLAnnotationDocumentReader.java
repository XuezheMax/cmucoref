package cmucoref.io;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.WhitespaceTokenizerAnnotator;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.Filters;
import edu.stanford.nlp.util.Generics;

public class CoNLLAnnotationDocumentReader extends DocumentReader {

	private WhitespaceTokenizerAnnotator tokenAnnotator = null;
	private MorphaAnnotator lemmaAnnotator = null;
	
	public CoNLLAnnotationDocumentReader() {
		super();
		tokenAnnotator = new WhitespaceTokenizerAnnotator(new Properties());
		lemmaAnnotator = new MorphaAnnotator(false);
	}
	
	@Override
	public Document getNextDocument(boolean readCorefLabel) throws IOException, ClassNotFoundException {
		String line = inputReader.readLine();
		Document doc = new Document();
		
		if(line == null || line.trim().length() == 0){
			return null;
		}
		else if(!getTitle(line, doc)){
			throw new IOException("title line format error: " + line);
		}
		
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		int id = 0;
		Sentence sent = getNextSentence(id++, readCorefLabel);
		while(sent != null){
			sentences.add(sent);
			sent = getNextSentence(id++, readCorefLabel);
		}
		doc.setSentences(sentences);
		processDoc(doc);
		return doc;
	}

	protected Sentence getNextSentence(int id, boolean readCorefLabel) throws IOException {
		String line = inputReader.readLine();
		if(line == null){
			throw new IOException("reading sentence error: no document end");
		}
		else if(line.equals("#end document")){
			return null;
		}
		
		ArrayList<String[]> lineList = new ArrayList<String[]>();
		while (line != null && line.length() != 0) {
			lineList.add(line.split(" +"));
			line = inputReader.readLine();
		}

		int length = lineList.size();

		if (length == 0) {
			throw new IOException("reading sentence error: zero length.");
		}
		
		Lexicon[] lexicons = new Lexicon[length + 1];
		lexicons[0] = new Lexicon(0, "<ROOT-FORM>", "<ROOT-LEMMA>", "<ROOT-CPOS>", "<ROOT-POS>", "<ROOT-NER>",
				-1, "<no-type>", -1, "<no-type>", "-");
		StringBuilder pennTree = new StringBuilder();
		int i = 1;
		String speaker = null;
		Stack<String> nerStack = new Stack<String>();
		for(String[] info : lineList){
			//speaker
			if(speaker == null){
				speaker = info[9];
			}
			else {
				if(!speaker.equals(info[9])){
					throw new IOException("speaker info error: " + speaker + " " + info[9]);
				}
			}
			
			//penn tree
			String replacement = java.util.regex.Matcher.quoteReplacement("(" + info[4] + " " + info[3] + ")");
			pennTree.append(info[5].replaceFirst("\\*", replacement));
			
			//ner
			String ner = info[10];
			if(ner.startsWith("(")) {
				if(ner.endsWith(")")) {
					ner = ner.substring(1, ner.length() - 1);
				}
				else {
					if(ner.endsWith("*")) {
						ner = ner.substring(1, ner.length() - 1);
						nerStack.push(ner);
					}
					else {
						throw new IOException("ner error: " + ner);
					}
				}
			}
			else{
				if(ner.endsWith(")")) {
					if(!ner.equals("*)")) {
						throw new IOException("ner error: " + ner);
					}
					ner = nerStack.pop();
				}
				else {
					if(!ner.equals("*")) {
						throw new IOException("ner error: " + ner);
					}
					if(nerStack.isEmpty()) {
						ner = "O";
					}
					else {
						ner = nerStack.peek();
					}
				}
			}
			
			if(readCorefLabel){
				lexicons[i] = new Lexicon(i++, info[3], null, "_", info[4], ner, 0, null, 0, "erased", info[info.length - 1]);
			}
			else{
				lexicons[i] = new Lexicon(i++, info[3], null, "_", info[4], ner, 0, null, 0, "erased", "-");
			}
		}
		//change (TOP to (ROOT
		pennTree.replace(0, 4, "(ROOT");
		
		return new Sentence(lexicons, formatPennTreeString(pennTree.toString()), speaker, id);
	}
	
	protected boolean getTitle(String titleLine, Document doc){
		if(!titleLine.startsWith("#begin document")){
			return false;
		}
		
		int pos1 = titleLine.indexOf('(');
		int pos2 = titleLine.lastIndexOf(')');
		doc.setFileName(titleLine.substring(pos1 + 1, pos2));
		
		int pos3 = titleLine.lastIndexOf(' ');
		doc.setDocId(Integer.parseInt(titleLine.substring(pos3 + 1)));
		return true;
	}
	
	protected String formatPennTreeString(String pennTree){
		pennTree = pennTree.replaceAll("NML", "NP");
		StringBuilder res = new StringBuilder();
		
		for(int i = 0; i < pennTree.length(); ++i){
			if(pennTree.charAt(i) == '('){
				res.append(" ");
			}
			res.append(pennTree.charAt(i));
		}
		
		return res.toString().trim();
	}
	
	private void processDoc(Document doc) throws ClassNotFoundException, IOException{
		//assign annotations
		List<CoreMap> corefDoc = new ArrayList<CoreMap>();
		ArrayList<Sentence> sentences = doc.getSentences();
		for(Sentence sentence : sentences){
			Annotation textAnno = new Annotation(sentence.getRawText());
			//tokenize
			tokenAnnotator.annotate(textAnno);
			List<CoreLabel> tokens = textAnno.get(TokensAnnotation.class);
			for(int j = 0; j < tokens.size(); ++j) {
				CoreLabel token = tokens.get(j);
				Lexicon lex = sentence.getLexicon(j + 1);
				token.set(PartOfSpeechAnnotation.class, lex.postag);
			}
			
			//stanford sentence annotation
			CoreMap sent = new ArrayCoreMap();
			sent.set(TokensAnnotation.class, tokens);
			
			//add sent to doc
			corefDoc.add(sent);
		}
		// annotate document
		Annotation anno = new Annotation(corefDoc);
		
		//annotate lemma
		lemmaAnnotator.annotate(anno);
		
		List<CoreMap> sents = anno.get(SentencesAnnotation.class);
		
		int i = 0;
		for(CoreMap sent: sents) {
			Sentence sentence = doc.getSentence(i++);
			// this is the parse tree of the current sentence
			int j = 1;
			for (CoreLabel token: sent.get(TokensAnnotation.class)) {
				Lexicon lexicon = sentence.getLexicon(j++);
				// this is the lemma of the token
				lexicon.lemma = token.getString(LemmaAnnotation.class);
			}
			
			PennTreeReader pennReader = new PennTreeReader(new StringReader(sentence.getPennTree()));
			Tree tree = pennReader.readTree();
			pennReader.close();
			
			//English grammatical structure (make Copula head)
			Filter<String> puncFilter = Filters.acceptFilter();
			EnglishGrammaticalStructure egs = new EnglishGrammaticalStructure(tree, puncFilter, 
					new SemanticHeadFinder(false), true);
			
			List<TypedDependency> basicTypedDependencies = egs.typedDependencies(false);
			assignTypedDependencies(sentence, egs, basicTypedDependencies, true);
			
			List<TypedDependency> collapsedTypedDependencies = egs.typedDependenciesCollapsed(true);
			assignTypedDependencies(sentence, egs, collapsedTypedDependencies, false);
						
		}
	}
	
	private void assignTypedDependencies(Sentence sentence, EnglishGrammaticalStructure egs, List<TypedDependency> typedDependencies, boolean basic){
		Map<Integer, Integer> indexToPos = Generics.newHashMap();
	    indexToPos.put(0,0);
	    
	    List<Tree> gsLeaves = egs.root().getLeaves();
	    for (int i = 0; i < gsLeaves.size(); i++) {
	      TreeGraphNode leaf = (TreeGraphNode) gsLeaves.get(i);
	      indexToPos.put(leaf.label().index(), i + 1);
	    }
	    
	    for(TypedDependency dep : typedDependencies){
	    	int depPos = indexToPos.get(dep.dep().index());
	    	Lexicon lexicon = sentence.getLexicon(depPos);
	    	if(basic){
	    		lexicon.basic_head = indexToPos.get(dep.gov().index());
	    		lexicon.basic_deprel = dep.reln().toString();
	    		if(lexicon.basic_deprel == null){
	    			System.err.println("null basic dep");
	    			System.exit(0);
	    		}
	    	}
	    	else{
	    		lexicon.collapsed_head = indexToPos.get(dep.gov().index());
	    		lexicon.collapsed_deprel = dep.reln().toString();
	    	}
	    }
	}
}
