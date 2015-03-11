package cmucoref.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import cmucoref.document.Document;

public abstract class DocumentReader {
	protected BufferedReader inputReader;
	
	public static DocumentReader createDocumentReader(String readerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		return (DocumentReader) Class.forName(readerClassName).newInstance();
	}
	
	public void startReading(String file) throws IOException{
		inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
	}
	
	public void close() throws IOException{
		inputReader.close();
	}
	
	public abstract Document getNextDocument() throws IOException;
	
	protected String normalize(String s) {
		if (s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+")){
			return "<num>";
		}
		return s;
	}
}
