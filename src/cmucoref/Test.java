package cmucoref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.io.*;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.extractor.StanfordMentionExtractor;
import cmucoref.model.Options;
import cmucoref.util.Pair;



public class Test {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws MentionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MentionException {
		System.getProperties().list(System.out);
		System.exit(0);
		
		String inputfile = "conll2012.eng.dev.auto.anno";
		String outputfile = "conll2012.eng.dev.auto-true.conll";
		PrintStream printer = new PrintStream("mention-conll.txt");
		
		
		Options options = new Options(args);
		
		System.out.println("post-processing: " + options.postProcessing());
		
		DocumentReader docReader = DocumentReader.createDocumentReader(AnnotatedDocumentReader.class.getName());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(CoNLLDocumentWriter.class.getName());
		
		docReader.startReading(inputfile);
		docWriter.startWriting(outputfile);
		
		StanfordMentionExtractor mentionExtractor = new StanfordMentionExtractor();
		mentionExtractor.createDict(options.getPropFile());
		
		Document doc = docReader.getNextDocument(true);
		while(doc != null){
			List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, options);
			mentionExtractor.getSingleMentionList(doc, mentionList, options);
			doc.getCorefClustersFromDocument(mentionList);
			doc.assignCorefClustersToDocument(mentionList, options.postProcessing());
			docWriter.writeDocument(doc, true);
			mentionExtractor.displayMentions(doc, mentionList, printer);
			printer.flush();
			doc = docReader.getNextDocument(true);
		}
		
		printer.close();
		docReader.close();
		docWriter.close();
	}

}
