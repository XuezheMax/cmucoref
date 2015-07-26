package cmucoref.io;

import java.io.IOException;
import java.util.ArrayList;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;

public class CoNLLXDocumentReader extends DocumentReader {

	public CoNLLXDocumentReader(){
		super();
	}
	
	@Override
	protected Document getNextDocument(boolean readCorefLabel) throws IOException, ClassNotFoundException {
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
		
		for(String[] info : lineList){
			//speaker
			if(speaker == null){
				speaker = info[12];
			}
			else {
				if(!speaker.equals(info[12])){
					throw new IOException("speaker info error: " + speaker + " " + info[12]);
				}
			}
			
			//penn tree
			String replacement = java.util.regex.Matcher.quoteReplacement("(" + info[6] + " " + info[3] + ")");
			pennTree.append(info[7].replaceFirst("\\*", replacement));
			
			//ner
			String ner = info[13];
			
			if(readCorefLabel){
				lexicons[i] = new Lexicon(i++, info[3], info[4], info[5], info[6], ner, 
						Integer.parseInt(info[8]), info[9], Integer.parseInt(info[10]), info[11], info[14]);
			}
			else{
				lexicons[i] = new Lexicon(i++, info[3], info[4], info[5], info[6], ner, 
						Integer.parseInt(info[8]), info[9], Integer.parseInt(info[10]), info[11], "-");
			}
		}
		
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
}
