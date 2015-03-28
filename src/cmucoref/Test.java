package cmucoref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cmucoref.document.Document;
import cmucoref.io.AnnotatedDocumentReader;
import cmucoref.io.DocumentReader;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.extractor.StanfordMentionExtractor;
import cmucoref.model.Options;

import edu.stanford.nlp.dcoref.Dictionaries;

public class Test {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		DocumentReader docReader = DocumentReader.createDocumentReader(AnnotatedDocumentReader.class.getName());
		docReader.startReading(args[0]);
		Options options = new Options();
		PrintWriter printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("mention.txt")), "UTF-8"));
		//PrintWriter printer2 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("new.txt")), "UTF-8"));
		Dictionaries dict = new Dictionaries();
		MentionExtractor mentionExtractor = new StanfordMentionExtractor(dict);
		Document doc = docReader.getNextDocument();
		long start = System.currentTimeMillis();
		while(doc != null){
			List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, options);
			mentionExtractor.displayMentions(doc, mentionList, printer);
			//System.exit(0);
			doc = docReader.getNextDocument();
		}
		System.out.println("time: " + (System.currentTimeMillis() - start ) / 1000);
		docReader.close();
		printer.close();
	}

}
