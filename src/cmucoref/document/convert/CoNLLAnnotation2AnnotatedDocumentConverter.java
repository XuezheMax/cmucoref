package cmucoref.document.convert;

import java.io.IOException;

import cmucoref.document.Document;
import cmucoref.io.AnnotatedDocumentWriter;
import cmucoref.io.CoNLLAnnotationDocumentReader;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;

public class CoNLLAnnotation2AnnotatedDocumentConverter {

	/**
	 * 
	 * @param args
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		String conllfile = args[0];
		String conllxfile = args[1];
		
		DocumentReader docReader = DocumentReader.createDocumentReader(CoNLLAnnotationDocumentReader.class.getName());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(AnnotatedDocumentWriter.class.getName());
		
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
