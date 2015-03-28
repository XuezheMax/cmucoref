package cmucoref.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
		HashMap<String, String> docMap = new HashMap<String, String>();
		
		int total = 0, tooShort = 0, missNer = 0, duplication = 0;
		for(File file : files){
			docReader.startReading(file.getAbsolutePath());
			docWriter.startWriting(destPath + file.getName().substring(0, file.getName().lastIndexOf('.')) + ".anno");
			Document doc = docReader.getNextDocument();
			while(doc != null){
				total++;
				if(doc.size() < 3){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": too few sentences");
					doc = docReader.getNextDocument();
					tooShort++;
					continue;
				}
				
				ArrayList<Sentence> sents = doc.getSentences();
				String key = sents.get(0).getRawText() + sents.get(1).getRawText() + sents.get(2).getRawText();
				
				//check duplication
				String val = docMap.get(key);
				if(val != null){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": duplicated with " + val);
					doc = docReader.getNextDocument();
					duplication++;
					continue;
				}
				
				//check ner missing
				if(!checkNer(doc)){
					System.err.println(doc.getFileName() + " docId " + doc.getDocId() + ": missing ner");
					doc = docReader.getNextDocument();
					missNer++;
					continue;
				}
				
				docMap.put(key, doc.getFileName() + " docId " + doc.getDocId());
				System.out.println(doc.getFileName() + " docId " + doc.getDocId() + ": accepted");
				docWriter.writeDocument(doc, null);
				doc = docReader.getNextDocument();
			}
			docReader.close();
			docWriter.close();
		}
		System.out.println("total documents: " + total);
		System.out.println("accepted documents: " + (total - tooShort - missNer - duplication));
		System.out.println("too short documents: " + tooShort);
		System.out.println("miss ner documents: " + missNer);
		System.out.println("duplicated documents: " + duplication);
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
