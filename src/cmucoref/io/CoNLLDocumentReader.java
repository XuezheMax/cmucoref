package cmucoref.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.NERCombinerAnnotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;
import edu.stanford.nlp.pipeline.WhitespaceTokenizerAnnotator;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.Filters;
import edu.stanford.nlp.util.Generics;

public class CoNLLDocumentReader extends DocumentReader{

	private WhitespaceTokenizerAnnotator tokenAnnotator = null;
	private POSTaggerAnnotator posAnnotator = null;
	private MorphaAnnotator lemmaAnnotator = null;
	NERCombinerAnnotator nerAnnotator = null;
	ParserAnnotator parserAnnotator = null;
	
	public CoNLLDocumentReader() {
		super();
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
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
		
		int i = 1;
		for(String[] info : lineList){
			if(readCorefLabel){
				lexicons[i] = new Lexicon(i++, info[3], null, "_", null, null, 0, null, 0, "erased", info[info.length - 1]);
			}
			else{
				lexicons[i] = new Lexicon(i++, info[3], null, "_", null, null, 0, null, 0, "erased", "-");
			}
		}
		return new Sentence(lexicons, null, id);
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
	
	private void processDoc(Document doc) throws ClassNotFoundException, IOException{
		//assign annotations
		List<CoreMap> corefDoc = new ArrayList<CoreMap>();
		ArrayList<Sentence> sentences = doc.getSentences();
		for(Sentence sentence : sentences){
			Annotation textAnno = new Annotation(sentence.getRawText());
			//tokenize
			tokenAnnotator.annotate(textAnno);
			List<CoreLabel> tokens = textAnno.get(TokensAnnotation.class);
			
			//stanford sentence annotation
			CoreMap sent = new ArrayCoreMap();
			sent.set(TokensAnnotation.class, tokens);
			
			//add sent to doc
			corefDoc.add(sent);
		}
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
		
		List<CoreMap> sents = anno.get(SentencesAnnotation.class);
		
		int i = 0;
		for(CoreMap sent: sents) {
			Sentence sentence = doc.getSentence(i++);
			// this is the parse tree of the current sentence
			int j = 1;
			for (CoreLabel token: sent.get(TokensAnnotation.class)) {
				Lexicon lexicon = sentence.getLexicon(j++);
				// this is the text of the token
				lexicon.form = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				lexicon.postag = token.get(PartOfSpeechAnnotation.class);
				// this is the lemma of the token
				lexicon.lemma = token.getString(LemmaAnnotation.class);
				// this is the NER label of the token
				lexicon.ner = token.get(NamedEntityTagAnnotation.class);
			}
			
			Tree tree = sent.get(TreeAnnotation.class);
			sentence.setPennTree(tree.toString());
			
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
