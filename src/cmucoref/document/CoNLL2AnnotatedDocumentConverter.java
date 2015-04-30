package cmucoref.document;

import java.io.IOException;

import cmucoref.io.AnnotatedDocumentWriter;
import cmucoref.io.CoNLLDocumentReader;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;

public class CoNLL2AnnotatedDocumentConverter {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		String conllfile = args[0];
		String annotatedfile = args[1];
		
		DocumentReader docReader = DocumentReader.createDocumentReader(CoNLLDocumentReader.class.getName());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(AnnotatedDocumentWriter.class.getName());
		
		docReader.startReading(conllfile);		
		docWriter.startWriting(annotatedfile);
		
		Document doc = docReader.getNextDocument(true);
		while(doc != null){
			docWriter.writeDocument(doc, true);
			doc = docReader.getNextDocument(true);
		}
		
		docReader.close();
		docWriter.close();
	}
}
