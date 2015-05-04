package cmucoref.io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import cmucoref.document.Document;

public abstract class DocumentWriter {
	protected BufferedWriter writer;
	
	public static DocumentWriter createDocumentWriter(String readerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		return (DocumentWriter) Class.forName(readerClassName).newInstance();
	}
	public void startWriting(String file) throws IOException {
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
	}
	
	public void close() throws IOException {
		writer.flush();
		writer.close();
	}
	
	public abstract void writeDocument(Document doc, boolean writeCorefLabel) throws IOException;
}
