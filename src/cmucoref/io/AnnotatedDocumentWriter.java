package cmucoref.io;

import java.io.IOException;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

public class AnnotatedDocumentWriter extends DocumentWriter {

	@Override
	public void writeDocument(Document doc, boolean writeCorefLabel)
			throws IOException {
		
		writer.write("#begin document (" + doc.getFileName() + ") " + "docId " + doc.getDocId());
		writer.newLine();
		for(Sentence sent : doc.getSentences()){
			writeSentence(sent, writeCorefLabel);
		}
		writer.write("#end document");
		writer.newLine();
		writer.flush();
	}
	
	protected void writeSentence(Sentence sent, boolean writeCorefLabel) throws IOException{
		writer.write(sent.getPennTree());
		writer.newLine();
		
		Lexicon[] lexicons = sent.getLexicons();
		for(int i = 1; i < sent.length(); ++i){
			writer.write(i + "\t" + lexicons[i].form + "\t" + lexicons[i].lemma + "\t");
			writer.write(lexicons[i].cpostag + "\t" + lexicons[i].postag + "\t");
			writer.write(lexicons[i].basic_head + "\t" + lexicons[i].basic_deprel + "\t");
			writer.write(lexicons[i].collapsed_head + "\t" + lexicons[i].collapsed_deprel + "\t");
			writer.write(lexicons[i].ner);
			if(writeCorefLabel){
				writer.write("\t" + lexicons[i].corefLabel);
			}
			writer.newLine();
		}
		writer.newLine();
	}
}
