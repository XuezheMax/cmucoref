package cmucoref.io;

import java.io.IOException;
import java.util.ArrayList;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

public class AnnotatedDocumentReader extends DocumentReader{

	@Override
	public Document getNextDocument() throws IOException {
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
		Sentence sent = getNextSentence(id++);
		while(sent != null){
			sentences.add(sent);
			sent = getNextSentence(id++);
		}
		doc.setSentences(sentences);
		return doc;
	}
	
	protected Sentence getNextSentence(int id) throws IOException {
		String parseTree = null;
		
		String line = inputReader.readLine();
		if(line == null){
			throw new IOException("reading sentence error: no document end");
		}
		else if(line.equals("#end document")){
			return null;
		}
		else if(!line.startsWith("(ROOT")){
			throw new IOException("reading sentence error: penn parse tree error:\n" + line);
		}
		else{
			parseTree = line;
		}
		
		ArrayList<String[]> lineList = new ArrayList<String[]>();
		line = inputReader.readLine();
		while (line != null && line.length() != 0) {
			lineList.add(line.split("\t"));
			line = inputReader.readLine();
		}

		int length = lineList.size();

		if (length == 0) {
			throw new IOException("reading sentence error: zero length.");
		}
		
		Lexicon[] lexicons = new Lexicon[length + 1];
		lexicons[0] = new Lexicon(0, "<ROOT-FORM>", "<ROOT-LEMMA>", "<ROOT-CPOS>", "<ROOT-POS>", "<ROOT-NER>",
				-1, "<no-type>", -1, "<no-type>");
		
		int i = 1;
		for(String[] info : lineList){
			lexicons[i] = new Lexicon(i++, info[1], info[2], info[3], info[4], info[9], 
					Integer.parseInt(info[5]), info[6], Integer.parseInt(info[7]), info[8]);
		}
		
		return new Sentence(lexicons, parseTree, id);
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

}
