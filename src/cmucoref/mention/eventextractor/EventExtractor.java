package cmucoref.mention.eventextractor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import cmucoref.mention.*;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.model.Options;
import cmucoref.document.*;

public abstract class EventExtractor {
	protected Set<String> eventSet = new HashSet<String>();
	
	protected EventDictionaries edict;
	
	protected void addEvent2Set(Event event) {
		if(event != null) {
			eventSet.add(event.toFeature());
		}
	}
	
	public void createDict(String propfile) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		InputStream in = MentionExtractor.class.getClassLoader().getResourceAsStream(propfile);
		props.load(new InputStreamReader(in));
		this.edict = new EventDictionaries(props);
	}
	
	public static EventExtractor createExtractor(String extractorClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (EventExtractor) Class.forName(EventExtractor.class.getPackage().getName() + "." + extractorClassName).newInstance();
	}
	
	public EventExtractor() {}
	
	public abstract void extractEvents(Document doc, List<List<Mention>> mentionList, Options options);
	
	public int sizeOfEvent() {
		return eventSet.size();
	}
	
	protected boolean indexOutsideMention(int index, Mention mention) {
		return index < mention.startIndex || mention.endIndex <= index;
	}
	
	public boolean acceptableGrammRole(String grammRole, boolean includePrep) {
		return edict.roles.contains(grammRole) || includePrep && grammRole.startsWith("prep_");
	}
	
	protected String normalize(String str) {
		int pos_of_dash = str.indexOf('-');
		if(pos_of_dash < 0) {
			return str;
		}
		else {
			return str.substring(0, pos_of_dash) + str.substring(pos_of_dash + 1, str.length());
		}
	}
	
	protected String addParticle(String predicate, int predPos, Sentence sent) {
		for(int i = predPos + 1; i < sent.length(); ++i) {
			Lexicon lex = sent.getLexicon(i);
			if(lex.basic_head == predPos && lex.basic_deprel.equals("prt")) {
				return predicate + "_" + lex.lemma;
			}
		}
		return predicate;
	}
	
	protected boolean isNumber(String str) {
		if(edict.numbers.contains(str)) {
			return true;
		}
		
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
}
