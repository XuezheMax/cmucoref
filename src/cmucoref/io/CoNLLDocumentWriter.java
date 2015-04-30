package cmucoref.io;

import java.io.IOException;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

public class CoNLLDocumentWriter extends DocumentWriter {

	@Override
	public void writeDocument(Document doc, boolean writeCorefLabel) throws IOException {
		int docId = doc.getDocId();
		String tmp = docId < 10 ? "00" : (docId < 100 ? "0" : "");
		writer.write("#begin document (" + doc.getFileName() + "); " + "part " + tmp + doc.getDocId());
		writer.newLine();
		for(Sentence sent : doc.getSentences()){
			writeSentence(sent, doc.getFileName(), docId, writeCorefLabel);
		}
		writer.write("#end document");
		writer.newLine();
		writer.flush();
	}

	protected void writeSentence(Sentence sent, String filename, int docId, boolean writeCorefLabel) throws IOException{
		Lexicon[] lexicons = sent.getLexicons();
		int[] maxLength = new int[5];
		maxLength[0] = (sent.length() - 2) > 100 ? 3 : ((sent.length() - 2) > 10 ? 2 : 1);
		
		for(int i = 1; i < sent.length(); ++i){
			Lexicon lex = lexicons[i];
			maxLength[1] = Math.max(maxLength[1], lex.form.length());
			maxLength[2] = Math.max(maxLength[2], lex.postag.length());
			maxLength[3] = Math.max(maxLength[3], lex.lemma.length());
			maxLength[4] = Math.max(maxLength[4], lex.ner.length());
		}
		
		for(int i = 1; i < sent.length(); ++i){
			Lexicon lex = lexicons[i];
			writer.write(filename + "   " + docId + "   ");
			String id = String.valueOf(i - 1);
			writer.write(getContinunousWhiteSpace(maxLength[0] - id.length()) + id + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[1] - lex.form.length()) + lex.form + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[2] - lex.postag.length()) + lex.postag + "   ");
			writer.write("*   ");
			writer.write(getContinunousWhiteSpace(maxLength[3] - lex.lemma.length()) + lex.lemma + "   ");
			writer.write("-   -   -   ");
			writer.write(getContinunousWhiteSpace(maxLength[4] - lex.ner.length()) + lex.ner + "   ");
			
			if(writeCorefLabel){
				writer.write(lexicons[i].corefLabel);
			}
			writer.newLine();
		}
		writer.newLine();
	}
	
	private String getContinunousWhiteSpace(int n){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < n; ++i){
			sb.append(" ");
		}
		return sb.toString();
	}
}
