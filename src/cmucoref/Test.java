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
		String inputfile = "tmp.conllx";
		//String outputfile = "mention-conllx.txt";
		
		DocumentReader docReader = DocumentReader.createDocumentReader(CoNLLXDocumentReader.class.getName());
		
		docReader.startReading(inputfile);
		//PrintStream printer = new PrintStream(new File(outputfile));
		
		StanfordMentionExtractor mentionExtractor = new StanfordMentionExtractor();
		Options options = new Options();
		mentionExtractor.createDict(options.getPropFile());
		
		Document doc = docReader.getNextDocument(false);
		while(doc != null){
			List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, options);
			List<Mention> allMentions = mentionExtractor.getSingleMentionList(doc, mentionList, options);
			mentionExtractor.displayMentions(doc, mentionList, System.out);
			System.out.println("--------------------------");
			for(int i = 0; i < allMentions.size(); ++i) {
				Mention anaph = allMentions.get(i);
				System.out.println("anaph mention:");
				anaph.display(doc.getSentence(anaph.sentID), System.out);
				for(int j = 0; j < i; ++j) {
					System.out.println("antec mention:");
					Mention antec = allMentions.get(j);
					antec.display(doc.getSentence(antec.sentID), System.out);
					System.out.println(anaph.ruleout(doc.getSentence(anaph.sentID), antec, 
							doc.getSentence(antec.sentID), mentionExtractor.getDict()));
					
					System.out.println(anaph.preciseMatch(doc.getSentence(anaph.sentID), antec, 
							doc.getSentence(antec.sentID), mentionExtractor.getDict()));
				}
				System.out.println("--------------------------");
			}
			//printer.flush();
			doc = docReader.getNextDocument(false);
		}
		
		//printer.close();
		docReader.close();
	}

}
