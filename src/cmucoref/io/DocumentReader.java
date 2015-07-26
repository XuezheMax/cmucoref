package cmucoref.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.model.Options;

public abstract class DocumentReader {
	protected BufferedReader inputReader;
	
	public static DocumentReader createDocumentReader(String readerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		return (DocumentReader) Class.forName(readerClassName).newInstance();
	}
	
	public void startReading(String file) throws IOException{
		inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
	}
	
	public void close() throws IOException{
		inputReader.close();
	}
	
	protected abstract Document getNextDocument(boolean readCorefLabel) throws IOException, ClassNotFoundException;
	
	public final Document getNextDocument(Options options, boolean readCorefLabel) throws ClassNotFoundException, IOException {
		Document doc = getNextDocument(readCorefLabel);
		if(options != null && options.cleanDoc() && doc != null) {
			clean(doc);
		}
		return doc;
	}
	
	private static Set<String> ignoredPOS = new HashSet<String>(Arrays.asList(".", ",", "``", "''", ":", "-LRB-", "-RRB-", "UH", "CC", "<ROOT-POS>"));
	protected void clean(Document doc) {
		List<Sentence> sents = doc.getSentences();
		for(Sentence sent : sents) {
			boolean removed = true;
			for(Lexicon lex : sent.getLexicons()) {
				if(!ignoredPOS.contains(lex.postag)) {
					removed = false;
					break;
				}
			}
			if(removed) {
				sent.setPresentToFalse();
			}
		}
	}
	
	protected String normalize(String s) {
		if (s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+")){
			return "<num>";
		}
		return s;
	}
}
