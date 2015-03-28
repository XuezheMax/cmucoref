package cmucoref.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashSet;

import cmucoref.exception.OptionException;

public class Options implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final static String DOC_READER = "reader",
								DEFAULT_DOC_READER_CLASS = cmucoref.io.AnnotatedDocumentReader.class.getName(),
								DOC_WRITER = "writer",
								DEFAULT_DOC_WRITER_CLASS = cmucoref.io.AnnotatedDocumentWriter.class.getName(),
								MODE = "mode",
								DEFAULT_MODE = "test",
								THREAD_NUM = "thread-num",
								DEFAULT_THREAD_NUM = "1",
								EXTRACT_MENTION_ATTRIBUTE = "extract-mention-attribute",
								DEFAULT_EXTRACT_MENTION_ATTRIBUTE = Boolean.TRUE.toString(),
								EXTRACT_MENTION_RELATION = "extract-mention-relation",
								DEFAULT_EXTRACT_MENTION_RELATION = Boolean.TRUE.toString(),
								APPOSITION_EXTRACTOR = "apposition-extractor",
								DEFAULT_APPOSITION_EXTRACTOR = cmucoref.mention.extractor.relationextractor.AppositionRelationPennExtractor.class.getName(),
								LISTMEMBER_EXTRACTOR = "list-member-extractor",
								DEFAULT_LISTMEMBER_EXTRACTOR = cmucoref.mention.extractor.relationextractor.ListMemberRelationExtractor.class.getName(),
								PREDICATENOMINATIVE_EXTRACTOR = "predicate-nominative-extractor",
								DEFAULT_PREDICATENOMINATIVE_EXTRACTOR = cmucoref.mention.extractor.relationextractor.PredicateNominativePennExtractor.class.getName(),
								RELATIVEPRONOUN_EXTRACTOR = "relative-pronoun-extractor",
								DEFAULT_RELATIVEPRONOUN_EXTRACTOR = cmucoref.mention.extractor.relationextractor.RelativePronounRelationExtractor.class.getName(),
								CONFIGURATION = "config",
								TRAININGFILE = "train-file",
								TESTFILE = "test-file",
								DEVFILE = "dev-file",
								OUTFILE = "output-file",
								GOLDFILE = "gold-file",
								LOGFILE = "log-file",
								MODELFILE = "model-file";
	
	private gnu.trove.map.hash.THashMap<String, String> argToValueMap = null;
	private HashSet<String> valid_opt_set = null;
	private final int maxiter = 5000;
	private final double stop_eta = 0.000001;
	
	private void init(){
		argToValueMap = new gnu.trove.map.hash.THashMap<String, String>();
		valid_opt_set = new HashSet<String>();
		//reader
		valid_opt_set.add(DOC_READER);
		argToValueMap.put(DOC_READER, DEFAULT_DOC_READER_CLASS);
		//writer
		valid_opt_set.add(DOC_WRITER);
		argToValueMap.put(DOC_WRITER, DEFAULT_DOC_WRITER_CLASS);
		//mode
		valid_opt_set.add(MODE);
		argToValueMap.put(MODE, DEFAULT_MODE);
		//thread num
		valid_opt_set.add(THREAD_NUM);
		argToValueMap.put(THREAD_NUM, DEFAULT_THREAD_NUM);
		//extract mention attribute
		valid_opt_set.add(EXTRACT_MENTION_ATTRIBUTE);
		argToValueMap.put(EXTRACT_MENTION_ATTRIBUTE, DEFAULT_EXTRACT_MENTION_ATTRIBUTE);
		//relation extractor
		valid_opt_set.add(EXTRACT_MENTION_RELATION);
		argToValueMap.put(EXTRACT_MENTION_RELATION, DEFAULT_EXTRACT_MENTION_RELATION);
		valid_opt_set.add(APPOSITION_EXTRACTOR);
		argToValueMap.put(APPOSITION_EXTRACTOR, DEFAULT_APPOSITION_EXTRACTOR);
		valid_opt_set.add(LISTMEMBER_EXTRACTOR);
		argToValueMap.put(LISTMEMBER_EXTRACTOR, DEFAULT_LISTMEMBER_EXTRACTOR);
		valid_opt_set.add(PREDICATENOMINATIVE_EXTRACTOR);
		argToValueMap.put(PREDICATENOMINATIVE_EXTRACTOR, DEFAULT_PREDICATENOMINATIVE_EXTRACTOR);
		valid_opt_set.add(RELATIVEPRONOUN_EXTRACTOR);
		argToValueMap.put(RELATIVEPRONOUN_EXTRACTOR, DEFAULT_RELATIVEPRONOUN_EXTRACTOR);
		//file name
		valid_opt_set.add(TRAININGFILE);
		valid_opt_set.add(TESTFILE);
		valid_opt_set.add(DEVFILE);
		valid_opt_set.add(OUTFILE);
		valid_opt_set.add(GOLDFILE);
		valid_opt_set.add(MODELFILE);
		valid_opt_set.add(LOGFILE);
		valid_opt_set.add(CONFIGURATION);
	}
	
	private String helpInfo(){
		// TODO
		return "Usage:\n";
	}
	
	private void checkError() throws OptionException{
		// TODO
	}
	
	private void parseOptions(String[] args) throws OptionException{
		for(int i = 0; i < args.length; i+=2) {
			String argumentIdentifier = args[i];
			if(!argumentIdentifier.startsWith("-") || argumentIdentifier.length() <= 1) {
				throw new OptionException("unexpected argument name: " + argumentIdentifier);
			}
			else{
				String argIdName = argumentIdentifier.substring(1);
				if(!valid_opt_set.contains(argIdName)){
					throw new OptionException("unexpected argument name: " + argumentIdentifier + "\n" + helpInfo());
				}
				else{
					String argValue = args[i + 1];
					argToValueMap.put(argIdName, argValue);
				}
			}
		}
		
		//parse config file
		String configfile = getArgValue(CONFIGURATION);
		if(configfile != null){
			parseOptions(configfile);
		}
		
		//check error
		checkError();
	}
	
	private void parseOptions(String configfile) throws OptionException{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configfile)));
			String line = reader.readLine();
			while(line != null){
				line = line.trim();
				if(line.length() == 0){
					line = reader.readLine();
					continue;
				}
				String[] tokens = line.split("=");
				if(!valid_opt_set.contains(tokens[0])){
					reader.close();
					throw new OptionException("unexpected argument name: " + tokens[0] + "\n" + helpInfo());
				}
				else{
					argToValueMap.put(tokens[0], tokens[1]);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			throw new OptionException(e.getMessage());
		}
	}
	
	public Options(){
		init();
	}
	
	public Options(String[] args){
		init();
		try {
			parseOptions(args);
		} catch (OptionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private String getArgValue(String argName){
		return argToValueMap.get(argName);
	}
	
	private void putArgValue(String argName, String value){
		argToValueMap.put(argName, value);
	}
	
	public String getReader(){
		return getArgValue(DOC_READER);
	}
	
	public String getWriter(){
		return getArgValue(DOC_WRITER);
	}
	
	public String getMode(){
		return getArgValue(MODE);
	}
	
	public int getThreadNum(){
		return Integer.parseInt(getArgValue(THREAD_NUM));
	}
	
	public boolean extractMentionAttribute(){
		return Boolean.parseBoolean(getArgValue(EXTRACT_MENTION_ATTRIBUTE));
	}
	
	public boolean extractMentionRelation(){
		return Boolean.parseBoolean(getArgValue(EXTRACT_MENTION_RELATION));
	}
	
	public String getListMemberRelationExtractor(){
		return getArgValue(LISTMEMBER_EXTRACTOR);
	}
	
	public String getAppositionRelationExtractor(){
		return getArgValue(APPOSITION_EXTRACTOR);
	}
	
	public String getPredicateNominativeRelationExtractor(){
		return getArgValue(PREDICATENOMINATIVE_EXTRACTOR);
	}
	
	public String getRelativePronounRelationExtractor(){
		return getArgValue(RELATIVEPRONOUN_EXTRACTOR);
	}
	
	public int maxIter(){
		return maxiter;
	}
	
	public double stopEta(){
		return stop_eta;
	}
	
	public String getTrainingFile(){
		return getArgValue(TRAININGFILE);
	}
	
	public String getTestFile(){
		return getArgValue(TESTFILE);
	}
	
	public String getDevFile(){
		return getArgValue(DEVFILE);
	}
	
	public String getOutFile(){
		return getArgValue(OUTFILE);
	}
	
	public String getGoldFile(){
		return getArgValue(GOLDFILE);
	}
	
	public String getLogFile(){
		return getArgValue(LOGFILE);
	}
	
	public String getModelFile(){
		return getArgValue(MODELFILE);
	}
	
	public void putReader(String reader){
		putArgValue(DOC_READER, reader);
	}
	
	public void putWriter(String writer){
		putArgValue(DOC_WRITER, writer);
	}
}
