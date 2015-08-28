package cmucoref.mention;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;

public class Dictionaries extends edu.stanford.nlp.dcoref.Dictionaries {
    
    public enum Definiteness {DEFINITE, GENERIC};
    
    public enum Gender { MALE, FEMALE, FeORM, NEUTRAL, UNKNOWN }
    
    //re-define pronouns
    public final Set<String> animatePronouns = new HashSet<String>(Arrays.asList(new String[]{ "i", "me", "myself", "mine", "my", "we", "us", "ourself", "ourselves", "ours", "our", "you", "yourself", "yours", "your", "yourselves", "he", "him", "himself", "his", "she", "her", "herself", "hers", "her", "who", "whom"}));
    public final Set<String> neutralPronouns = new HashSet<String>(Arrays.asList(new String[]{"it", "itself", "its", "where", "when", "the", "that", "this", "those", "these"}));
    public final Set<String> locationPronouns = new HashSet<String>(Arrays.asList("it", "its", "itself"));
    public final Set<String> norpPronouns = new HashSet<String>(Arrays.asList("it", "its", "itself", "they", "them", "their", "theirs", "themself", "themselves"));
    
    public final String [] commonNESuffixes = {
            "Corp", "Co", "Inc", "Ltd", "Province", "State"
    };
    
    public final Set<String> locationModifiers = new HashSet<String>(Arrays.asList("east", "west", "north", "south",
            "eastern", "western", "northern", "southern", "northwestern", "southwestern", "northeastern",
            "southeastern", "upper", "lower"));
    
    public final List<String> personalTitles = Arrays.asList("atty.", "attorney", "coach", "president",
            "doctor", "dir.", "director", "fr.", "father", "gov.", "governor", "prof.", "professor");
    
    public final Set<String> excludePossessivePronouns = new HashSet<String>(Arrays.asList("our", "my", "your"));
    public final Set<String> quantDeterminers = new HashSet<String>(Arrays.asList("both", "each"));
        
    public final Set<String> pluralDeterminers = new HashSet<String>(Arrays.asList("these", "those"));
        
    public final Set<String> singularDeterminers = new HashSet<String>(Arrays.asList("this", "that", "the"));
    
    public final Set<String> numbers = new HashSet<String>(Arrays.asList(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fiveteen", "sixteen", "seventeen", "eighteen", "nineteen", 
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
            "hundred", "thousand", "million", "billion"));
    
    public final Set<String> quantifiers = new HashSet<String>(Arrays.asList("both", "all", "some", "most", "any", "each"));
    
    public final Set<String> dates = new HashSet<String>(Arrays.asList(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "yesterday", 
            "tomorrow", "today", "tonight", "january", "february", "march", "april", "may", "june", "july", 
            "august", "september", "october", "november", "december"));
    
    public final Set<String> temporals = new HashSet<String>(Arrays.asList(
            "second", "seconds", "minute", "minutes", "hour", "hours", "day", "days", "week", "weeks", 
            "month", "months", "year", "years", "decade", "decades", "century", "centuries", "millennium", "millenniums",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", 
            "now", "yesterday", "tomorrow", "today", "tonight",
            "age", "time", "era", "epoch", "morning", "evening", "night", "noon", "afternoon",
            "semester", "semesters", "trimester", "trimesters", "quarter", "quarters", "term", "terms", 
            "winter", "spring", "summer", "fall", "autumn", "season",
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"));
    
    public final List<String> entityWordsToExclude = Arrays.asList("the", "this", "that", "those", 
            "these", "mr.", "miss", "mrs.", "dr.", "ms.", "inc.", "ltd.", "corp.", "'s");
    
    public final List<String> entityPOSToExclude = 
            Arrays.asList(".", ",", "``", "''", ":", "-LRB-", "-RRB-", "UH");
    
    public final Set<String> parts = new HashSet<String>(Arrays.asList("hundreds", "thousands", 
            "millions", "billions", "tens", "dozens", "group", "groups", "bunch", "a number", "numbers", 
            "a pinch", "a total", "all", "both"));
    
    public final Set<String> rolesofNoun = new HashSet<String>(Arrays.asList("nsubj", "nsubjpass", "iobj", 
            "dobj", "agent", "xsubj", "pobj", "root", "conj", "appos", "xcomp", "poss"));
    
    public final Set<String> animals = new HashSet<String>();
    
    private static void getWordsFromFile(String filename, Set<String> resultSet, boolean lowercase) throws IOException {
        if(filename==null) {
            return ;
        }
        BufferedReader reader = IOUtils.readerFromString(filename);
        while(reader.ready()) {
            if(lowercase) resultSet.add(reader.readLine().toLowerCase());
            else resultSet.add(reader.readLine());
        }
        IOUtils.closeIgnoringExceptions(reader);
    }
    
    private void loadAnimalList(String animalNameFile) {
        try {
            getWordsFromFile(animalNameFile, animals, true);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
    
    public Dictionaries(Properties props) {
        super(props);
        
        loadAnimalList(props.getProperty("cmucoref.animal", "cmucoref_models/models/animals.txt"));
    }
}
