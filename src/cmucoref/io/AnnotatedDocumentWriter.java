package cmucoref.io;

import java.io.IOException;
import java.util.List;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;

public class AnnotatedDocumentWriter extends DocumentWriter {

	@Override
	public void writeDocument(Document doc, List<List<Mention>> mentionList)
			throws IOException {
		writer.write("#begin document (" + doc.getFileName() + ") " + "docId " + doc.getDocId());
		writer.newLine();
		int i = 0;
		for(Sentence sent : doc.getSentences()){
			List<Mention> mentions = mentionList == null ? null : mentionList.get(i++);
			writeSentence(sent, mentions);
		}
		writer.write("#end document");
		writer.newLine();
		writer.flush();
	}
	
	protected void writeSentence(Sentence sent, List<Mention> mentions) throws IOException{
		writer.write(sent.getPennTree());
		writer.newLine();
		
		Lexicon[] lexicons = sent.getLexicons();
		for(int i = 1; i < sent.length(); ++i){
			writer.write(i + "\t" + lexicons[i].form + "\t" + lexicons[i].lemma + "\t");
			writer.write(lexicons[i].cpostag + "\t" + lexicons[i].postag + "\t");
			writer.write(lexicons[i].basic_head + "\t" + lexicons[i].basic_deprel + "\t");
			writer.write(lexicons[i].collapsed_head + "\t" + lexicons[i].collapsed_deprel + "\t");
			writer.write(lexicons[i].ner);
			if(mentions != null){
				// TODO
			}
			writer.newLine();
		}
		writer.newLine();
	}
}
