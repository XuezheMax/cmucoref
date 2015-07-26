package cmucoref.document.clean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.io.AnnotatedDocumentReader;
import cmucoref.io.AnnotatedDocumentWriter;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;

public class AnnotatedDocumentCleaner {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		String dirPath = args[0];
		String destPath = args[1];
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		
		DocumentReader docReader = DocumentReader.createDocumentReader(AnnotatedDocumentReader.class.getName());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(AnnotatedDocumentWriter.class.getName());
		HashMap<String, String> docMap1 = new HashMap<String, String>();
		HashMap<String, String> docMap2 = new HashMap<String, String>();
		
		int total = 0, tooShort = 0, missNer = 0, duplication1 = 0, shortSents = 0, duplication2 = 0;;
		for(File file : files){
			docReader.startReading(file.getAbsolutePath());
			docWriter.startWriting(destPath + file.getName().substring(0, file.getName().lastIndexOf('.')) + ".anno");
			Document doc = docReader.getNextDocument(null, false);
			while(doc != null){
				total++;
				if(doc.size() < 3){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": too few sentences");
					doc = docReader.getNextDocument(null, false);
					tooShort++;
					continue;
				}
				
				//check short sentences
				if(numOfShortSent(doc) > 2){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": too many short sentences");
					doc = docReader.getNextDocument(null, false);
					shortSents++;
					continue;
				}
				
				//check ner missing
				if(!checkNer(doc)){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": missing ner");
					doc = docReader.getNextDocument(null, false);
					missNer++;
					continue;
				}
				
				ArrayList<Sentence> sents = doc.getSentences();
				String key = sents.get(0).getRawText() + "|" + sents.get(1).getRawText() + "|" + sents.get(2).getRawText();
				
				//check duplication1
				String val = docMap1.get(key);
				if(val != null){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": duplicated 1 with " + val);
					doc = docReader.getNextDocument(null, false);
					duplication1++;
					continue;
				}
				docMap1.put(key, doc.getFileName() + " docId " + doc.getDocId());
				
				//check duplication2
				if(doc.size() > 3){
					key = sents.get(1).getRawText() + "|" + sents.get(2).getRawText() + "|" + sents.get(3).getRawText();
					val = docMap2.get(key);
					if(val != null){
						System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": duplicated 2 with " + val);
						doc = docReader.getNextDocument(null, false);
						duplication2++;
						continue;
					}
					docMap2.put(key, doc.getFileName() + " docId " + doc.getDocId());
				}
				
				System.out.println(doc.getFileName() + " docId " + doc.getDocId() + ": accepted");
				docWriter.writeDocument(doc, false);
				doc = docReader.getNextDocument(null, false);
			}
			docReader.close();
			docWriter.close();
		}
		System.out.println("total documents: " + total);
		System.out.println("accepted documents: " + (total - tooShort - missNer - duplication1 - duplication2 - shortSents));
		System.out.println("too short documents: " + tooShort);
		System.out.println("too many short sents documents: " + shortSents);
		System.out.println("miss ner documents: " + missNer);
		System.out.println("duplicated 1 documents: " + duplication1);
		System.out.println("duplicated 2 documents: " + duplication2);
	}
	
	public static int numOfShortSent(Document doc){
		int num = 0;
		for(Sentence sent : doc.getSentences()){
			if(sent.length() < 5){
				num++;
			}
		}
		return num;
	}

	public static boolean checkNer(Document doc){
		for(Sentence sent : doc.getSentences()){
			for(Lexicon lex : sent.getLexicons()){
				if(lex.ner.equals("__MISSING_NER_ANNOTATION__")){
					return false;
				}
			}
		}
		return true;
	}
}
