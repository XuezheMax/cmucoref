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
import cmucoref.mention.extractor.CMUMentionExtractor;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.extractor.StanfordMentionExtractor;
import cmucoref.model.Feature;
import cmucoref.model.FeatureVector;
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
		Options options = new Options(args);
		Mention.options = options;
		
		DocumentReader reader = new CoNLLXDocumentReader();
		DocumentWriter writer = new CoNLLXDocumentWriter();
		
		reader.startReading("data/dev/original/conllx/gold/conll2012.eng.dev.gold.bn.mnb.conllx");
		writer.startWriting("outfile/oracle.dev.gold.bn.mnb.conllx");
		PrintStream printer = new PrintStream(new File("mention.conllx.dev.gold.bn.mnb.txt"));
		
//		reader.startReading("tmp.conllx");
//		writer.startWriting("outfile/tmp.conllx");
//		PrintStream printer = new PrintStream(new File("tmp.txt"));
		
		
		Document doc = reader.getNextDocument(true);
		
		MentionExtractor mentionExtractor = new CMUMentionExtractor();
		
		mentionExtractor.createDict(options.getPropFile());
		
		while(doc != null) {
			List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, options);
			mentionExtractor.getSingleMentionList(doc, mentionList, options);
			mentionExtractor.displayMentions(doc, mentionList, printer);
			doc.getCorefClustersFromDocument(mentionList);
			doc.assignCorefClustersToDocument(mentionList, false);
			writer.writeDocument(doc, true);
			doc = reader.getNextDocument(true);
		}
		
		printer.close();
		reader.close();
		writer.close();
	}

}
