package cmucoref.io;

import java.io.IOException;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

public class CoNLLXDocumentWriter extends DocumentWriter {

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
		int[] maxLength = new int[12];
		maxLength[0] = (sent.length() - 2) > 100 ? 3 : ((sent.length() - 2) > 10 ? 2 : 1);
		
		String[] pennBit = new String[sent.length()];
		String pennTree = sent.getPennTree();
		int startPos = 0;
		for(int i = 1; i < sent.length(); ++i){
			Lexicon lex = lexicons[i];
			maxLength[1] = Math.max(maxLength[1], lex.form.length());
			maxLength[2] = Math.max(maxLength[2], lex.lemma.length());
			maxLength[3] = Math.max(maxLength[3], lex.cpostag.length());
			maxLength[4] = Math.max(maxLength[4], lex.postag.length());
			//get PennTree bit for each lexicon
			String pair = "(" + lex.postag + " " + lex.form + ")";
			int endPos1 = pennTree.indexOf(pair, startPos);
			pennBit[i] = pennTree.substring(startPos, endPos1) + "*";
			endPos1 = endPos1 + pair.length();
			int endPos2 = pennTree.indexOf('(', endPos1);
			if(endPos2 == -1){
				endPos2 = pennTree.length();
			}
			pennBit[i] = pennBit[i] + pennTree.substring(endPos1, endPos2);
			pennBit[i] = pennBit[i].replaceAll(" ", "");
			startPos = endPos2;
			
			maxLength[5] = Math.max(maxLength[5], pennBit[i].length());
			
			maxLength[6] = Math.max(maxLength[6], String.valueOf(lex.basic_head).length());
			maxLength[7] = Math.max(maxLength[7], lex.basic_deprel.length());
			maxLength[8] = Math.max(maxLength[8], String.valueOf(lex.collapsed_head).length());
			maxLength[9] = Math.max(maxLength[9], lex.collapsed_deprel.length());
			maxLength[10] = Math.max(maxLength[10], lex.ner.length());
			if(writeCorefLabel){
				maxLength[11] = Math.max(maxLength[11], lex.corefLabel.length());
			}
		}
		
		for(int i = 1; i < sent.length(); ++i){
			Lexicon lex = lexicons[i];
			writer.write(filename + "   " + docId + "   ");
			String id = String.valueOf(i);
			writer.write(getContinunousWhiteSpace(maxLength[0] - id.length()) + id + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[1] - lex.form.length()) + lex.form + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[2] - lex.lemma.length()) + lex.lemma + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[3] - lex.cpostag.length()) + lex.cpostag + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[4] - lex.postag.length()) + lex.postag + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[5] - pennBit[i].length()) + pennBit[i] + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[6] - String.valueOf(lex.basic_head).length()) + lex.basic_head + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[7] - lex.basic_deprel.length()) + lex.basic_deprel + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[8] - String.valueOf(lex.collapsed_head).length()) + lex.collapsed_head + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[9] - lex.collapsed_deprel.length()) + lex.collapsed_deprel + "   ");
			writer.write(sent.getSpeaker() + "   ");
			writer.write(getContinunousWhiteSpace(maxLength[10] - lex.ner.length()) + lex.ner + "   ");
			
			if(writeCorefLabel){
				writer.write(getContinunousWhiteSpace(maxLength[11] - lex.corefLabel.length()) + lexicons[i].corefLabel);
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
