package cmucoref.mention;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import cmucoref.mention.extractor.MentionExtractor;

public class EventDictionaries {

	public final Set<String> englishWords = new HashSet<String>();
	public final Set<String> englishVerbs = new HashSet<String>();
	public final Set<String> copulas = new HashSet<String>(Arrays.asList("appear", "be", "become", "seem", "remain"));
	public final Set<String> roles = new HashSet<String>(Arrays.asList("nsubj", "nsubjpass", "iobj", 
			"npadvmod", "tmod", "amod", "advmod", "quantmod", "vmod", 
			"pobj", "dobj", "agent", "xsubj", "xcomp"));
	public final Set<String> numbers = new HashSet<String>(Arrays.asList(
			"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
			"eleven", "twelve", "thirteen", "fourteen", "fiveteen", "sixteen", "seventeen", "eighteen", "nineteen", 
			"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
			"hundred", "thousand", "million", "billion", 
			"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", 
			"eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth",
			"twentieth", "thirtieth", "fortieth", "fiftieth", "sixtieth", "seventieth", "eightieth", "ninetieth",
			"hundredth", "thousandth", "millionth", "billionth"));
	
	public EventDictionaries(Properties props) throws IOException {
		loadEnglishWords(props.getProperty("cmucoref.englishwords", "cmucoref_models/models/wordsEn.txt"));
		loadEnglishVerbs(props.getProperty("cmucoref.englishverbs", "cmucoref_models/models/verbsEn.txt"));
	}
	
	private void loadEnglishWords(String wordfile) throws IOException {
		InputStream in = MentionExtractor.class.getClassLoader().getResourceAsStream(wordfile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String line = reader.readLine();
		while(line != null) {
			line = line.trim();
			if(line.length() > 0) {
				englishWords.add(line);
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadEnglishVerbs(String verbfile) throws IOException {
		InputStream in = MentionExtractor.class.getClassLoader().getResourceAsStream(verbfile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String line = reader.readLine();
		while(line != null) {
			line = line.trim();
			if(line.length() > 0) {
				englishVerbs.add(line);
			}
			line = reader.readLine();
		}
		reader.close();
	}
}
