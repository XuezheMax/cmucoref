package cmucoref;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.io.*;
import cmucoref.mention.Event;
import cmucoref.mention.Mention;
import cmucoref.mention.eventextractor.EventExtractor;
import cmucoref.mention.extractor.CMUMentionExtractor;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.extractor.StanfordMentionExtractor;
import cmucoref.model.Feature;
import cmucoref.model.FeatureVector;
import cmucoref.model.Options;
import cmucoref.util.Pair;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;


public class Test {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws MentionException 
	 */
	
	static String normalize(String str) {
		int pos_of_dash = str.indexOf('-');
		if(pos_of_dash < 0) {
			return str;
		}
		else {
			return str.substring(0, pos_of_dash) + str.substring(pos_of_dash + 1, str.length());
		}
	}
	
	static boolean isNumber(String str) {
		String[] tokens = str.split("-|:");
		if(tokens.length == 0) {
			return false;
		}
		
		int i = 0;
		while(i < tokens.length && (tokens[i].length() == 0 || tokens[i].equals("lrb"))) {
			i++;
		}
		
		if(i == tokens.length) {
			return false;
		}
		
		return tokens[i].matches("(-)?+[0-9]+(th|st|nd|rd)?|(-)?+[0-9]*\\.[0-9]+|(-)?+[0-9]+[0-9,]+(th|st|nd|rd)?");
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MentionException {
//		Set<String> verbs = new HashSet<String>();
//		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("verbsEn-old.txt"), "UTF8"));
//		String line = in.readLine();
//		while(line != null) {
//			line = line.toLowerCase().trim();
//			if(line.length() > 0) {
//				verbs.add(line);
//			}
//			line = in.readLine();
//		}
//		in.close();
//		
//		in = new BufferedReader(new InputStreamReader(new FileInputStream("verbsEn-new.txt"), "UTF8"));
//		line = in.readLine();
//		while(line != null) {
//			line = line.toLowerCase().trim();
//			if(line.length() > 0) {
//				verbs.add(line);
//			}
//			line = in.readLine();
//		}
//		in.close();
//		
//		List<String> verbList = new ArrayList<String>();
//		for(String verb : verbs) {
//			verbList.add(verb);
//		}
//		Collections.sort(verbList);
//		
//		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("verbsEn.txt"), "UTF8"));
//		for(String verb : verbList) {
//			out.write(verb + "\n");
//		}
//		out.flush();
//		out.close();
//		System.exit(0);
		
		Options options = new Options(args);
		Mention.options = options;
		
//		DocumentReader reader = new AnnotatedDocumentReader();
		DocumentReader reader = new CoNLLXDocumentReader();
		DocumentWriter writer = new CoNLLXDocumentWriter();
		
		reader.startReading("data/test/original/conllx/gold/conll2012.eng.test.gold.nw.wsj.conllx");
		writer.startWriting("outfile/oracle.test.gold.nw.wsj.conllx");
		PrintStream printer = new PrintStream(new File("mention.conllx.test.gold.nw.wsj.txt"));
		
//		reader.startReading("data/train/apw_eng_200406.anno");
//		writer.startWriting("outfile/200406.conllx");
//		PrintStream printer = new PrintStream(new File("mention-200406.txt"));
		
		Document doc = reader.getNextDocument(options, true);
		
		MentionExtractor mentionExtractor = new CMUMentionExtractor();
		mentionExtractor.createDict(options.getPropFile());
		
		EventExtractor eventExtractor = EventExtractor.createExtractor(options.getEventExtractor());
		eventExtractor.createDict(options.getPropFile());
		mentionExtractor.setEventExtractor(eventExtractor);
		
		while(doc != null) {
			System.out.println("Processing Doc: " + doc.getFileName() + " part: " + doc.getDocId());
			List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, options);
			mentionExtractor.getSingleMentionList(doc, mentionList, options);
			mentionExtractor.displayMentions(doc, mentionList, printer);
			doc.getCorefClustersFromDocument(mentionList);
			doc.assignCorefClustersToDocument(mentionList, false);
			writer.writeDocument(doc, true);
			doc = reader.getNextDocument(options, true);
		}
		
		printer.close();
		reader.close();
		writer.close();
		
//		for(String verb : eventExtractor.unknownVerbs) {
//			System.err.println(verb);
//		}
		
//		System.out.println(Event.roleSet.size());
//		for(String role : Event.roleSet) {
//			System.out.println(role);
//		}
	}

}
