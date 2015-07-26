package cmucoref.document.convert;

import java.io.IOException;

import cmucoref.document.Document;
import cmucoref.io.CoNLLRawTextDocumentReader;
import cmucoref.io.CoNLLXDocumentWriter;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;

public class CoNLLRawText2CoNLLXDocumentConverter {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		String conllfile = args[0];
		String conllxfile = args[1];
		
		DocumentReader docReader = DocumentReader.createDocumentReader(CoNLLRawTextDocumentReader.class.getName());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(CoNLLXDocumentWriter.class.getName());
		
		docReader.startReading(conllfile);		
		docWriter.startWriting(conllxfile);
		
		Document doc = docReader.getNextDocument(null, true);
		while(doc != null){
			docWriter.writeDocument(doc, true);
			doc = docReader.getNextDocument(null, true);
		}
		
		docReader.close();
		docWriter.close();
	}
}
