package cmucoref.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

import cmucoref.exception.OptionException;

public class Options implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final static String DOC_READER = "reader",
								DEFAULT_DOC_READER_CLASS = cmucoref.io.CoNLLXDocumentReader.class.getName(),
								TRAIN_READER = "train-reader",
								DEFAULT_TRAIN_READER_CLASS = cmucoref.io.AnnotatedDocumentReader.class.getName(),
								DOC_WRITER = "writer",
								DEFAULT_DOC_WRITER_CLASS = cmucoref.io.CoNLLXDocumentWriter.class.getName(),
								TRAINER = "trainer",
								DEFAULT_TRAINER_CLASS = cmucoref.trainer.EMTrainer.class.getName(),
								DECODER = "decoder",
								DEFAULT_DECODER = cmucoref.decoder.Decoder.class.getName(),
								MODE = "mode",
								DEFAULT_MODE = "test",
								THREAD_NUM = "thread-num",
								DEFAULT_THREAD_NUM = "1",
								PARAMETER_INITIALIZER = "parameter-initializer",
								DEFAULT_PARAMETER_INITIALIZER = cmucoref.model.params.UniformInitializer.class.getName(),
								MENTION_PARAMETER_SMOOTHER = "mention-parameter-smoother",
								DEFAULT_MENTION_PARAMETER_SMOOTHER = cmucoref.model.params.Smoother.class.getSimpleName(),
								EVENT_PARAMETER_SMOOTHER = "event-parameter-smoother",
								DEFAULT_EVENT_PARAMETER_SMOOTHER = cmucoref.model.params.Smoother.class.getSimpleName(),
								SMOOTHER_ALPHA = "smoothing-alpha",
								ALPHA_UPPER = "alpha-upper-limit",
								ALPHA_LOWER = "alpha-lower-limit",
								TUNE_ALPHA = "tune-alpha",
								SMOOTHER_BETA = "smoothing-beta",
								DEFAULT_TUNE_ALPHA = Boolean.FALSE.toString(),
								MENTION_EXTRACTOR = "mention-extractor",
								DEFAULT_MENTION_EXTRACTOR_CLASS = cmucoref.mention.extractor.CMUMentionExtractor.class.getName(),
								USE_PRECISE_MATCH = "use-preicse-match",
								DEFAULT_USE_PRECISE_MATCH = Boolean.TRUE.toString(),
								USE_DEMONYM = "use-demonym",
								DEFAULT_USE_DEMONYM = Boolean.FALSE.toString(),
								EXTRACT_MENTION_ATTRIBUTE = "extract-mention-attribute",
								DEFAULT_EXTRACT_MENTION_ATTRIBUTE = Boolean.TRUE.toString(),
								EXTRACT_MENTION_RELATION = "extract-mention-relation",
								DEFAULT_EXTRACT_MENTION_RELATION = Boolean.TRUE.toString(),
								USE_EVENT_FEATURE = "use-event-feature",
								DEFAULT_USE_EVENT = Boolean.TRUE.toString(),
								EVENT_EXTRACTOR = "event-extractor",
								DEFAULT_EVENT_EXTRACTOR = cmucoref.mention.eventextractor.BasicEventExtractor.class.getSimpleName(),
								APPOSITION_EXTRACTOR = "apposition-extractor",
								DEFAULT_APPOSITION_EXTRACTOR = cmucoref.mention.extractor.relationextractor.AppositionRelationDepExtractor.class.getName(),
								ROLEAPPOSITION_EXTRACTOR = "role-apposition-extractor",
								DEFAULT_ROLEAPPOSITION_EXTRACTOR = cmucoref.mention.extractor.relationextractor.RoleAppositionDepExtractor.class.getName(),
								LISTMEMBER_EXTRACTOR = "list-member-extractor",
								DEFAULT_LISTMEMBER_EXTRACTOR = cmucoref.mention.extractor.relationextractor.ListMemberRelationExtractor.class.getName(),
								PREDICATENOMINATIVE_EXTRACTOR = "predicate-nominative-extractor",
								DEFAULT_PREDICATENOMINATIVE_EXTRACTOR = cmucoref.mention.extractor.relationextractor.PredicateNominativeDepExtractor.class.getName(),
								RELATIVEPRONOUN_EXTRACTOR = "relative-pronoun-extractor",
								DEFAULT_RELATIVEPRONOUN_EXTRACTOR = cmucoref.mention.extractor.relationextractor.RelativePronounRelationExtractor.class.getName(),
								CREATE_TRAININGTMP = "create-tmp",
								DEFAULT_CREATE_TRAININGTMP = Boolean.FALSE.toString(),
								POST_PROCESSING = "post-processing",
								DEFAULT_POST_PROCESSING = Boolean.FALSE.toString(),
								ONTONOTES = "OntoNotes",
								DEFAULT_ONTONOTES = Boolean.FALSE.toString(),
								CLEANDOC = "clean-doc",
								DEFAULT_CLEANDOC = Boolean.FALSE.toString(),
								CONFIGURATION = "config",
								TRAININGFILE = "train-file",
								TESTFILE = "test-file",
								DEVFILE = "dev-file",
								OUTFILE = "output-file",
								GOLDFILE = "gold-file",
								LOGFILE = "log-file",
								MODELFILE = "model-file",
								PROPERTYFILE = "property-file",
								DEFAULT_PROPERTY_FILE = "cmucoref_models/properties/default.properties",
								WORDNETDIR = "wordnet",
								DEFAULT_WORDNETDIR = "lib/WordNet-3.0",
								CONLL_SCORER = "conll-scorer",
								MAX_ITER = "maxIter",
								DEFAULT_MAX_ITER = "1000",
								INIT_ITER = "initIter", 
								DEFAULT_ITIN_ITER = "5";
	
	private gnu.trove.map.hash.THashMap<String, String> argToValueMap = null;
	private HashSet<String> valid_opt_set = null;
	private int maxIter = 1000;
	private int initIter = 5;
	private final double stop_eta = 0.000001;
	private final String train_tmp = "tmp/train.tmp";
	
	private void init(){
		argToValueMap = new gnu.trove.map.hash.THashMap<String, String>();
		valid_opt_set = new HashSet<String>();
		//reader
		valid_opt_set.add(DOC_READER);
		argToValueMap.put(DOC_READER, DEFAULT_DOC_READER_CLASS);
		//training reader
		valid_opt_set.add(TRAIN_READER);
		argToValueMap.put(TRAIN_READER, DEFAULT_TRAIN_READER_CLASS);
		//writer
		valid_opt_set.add(DOC_WRITER);
		argToValueMap.put(DOC_WRITER, DEFAULT_DOC_WRITER_CLASS);
		//mode
		valid_opt_set.add(MODE);
		argToValueMap.put(MODE, DEFAULT_MODE);
		//thread num
		valid_opt_set.add(THREAD_NUM);
		argToValueMap.put(THREAD_NUM, DEFAULT_THREAD_NUM);
		//trainer
		valid_opt_set.add(TRAINER);
		argToValueMap.put(TRAINER, DEFAULT_TRAINER_CLASS);
		//decoder
		valid_opt_set.add(DECODER);
		argToValueMap.put(DECODER, DEFAULT_DECODER);
		//parameter initializer
		valid_opt_set.add(PARAMETER_INITIALIZER);
		argToValueMap.put(PARAMETER_INITIALIZER, DEFAULT_PARAMETER_INITIALIZER);
		//parameter smoother
		valid_opt_set.add(MENTION_PARAMETER_SMOOTHER);
		argToValueMap.put(MENTION_PARAMETER_SMOOTHER, DEFAULT_MENTION_PARAMETER_SMOOTHER);
		valid_opt_set.add(EVENT_PARAMETER_SMOOTHER);
        argToValueMap.put(EVENT_PARAMETER_SMOOTHER, DEFAULT_EVENT_PARAMETER_SMOOTHER);
		//smoothing alpha
		valid_opt_set.add(SMOOTHER_ALPHA);
		valid_opt_set.add(ALPHA_LOWER);
		valid_opt_set.add(ALPHA_UPPER);
		//tune alpha
		valid_opt_set.add(TUNE_ALPHA);
		argToValueMap.put(TUNE_ALPHA, DEFAULT_TUNE_ALPHA);
		//smoothing beta
		valid_opt_set.add(SMOOTHER_BETA);
		//mention extractor
		valid_opt_set.add(MENTION_EXTRACTOR);
		argToValueMap.put(MENTION_EXTRACTOR, DEFAULT_MENTION_EXTRACTOR_CLASS);
		//use event feature
		valid_opt_set.add(USE_EVENT_FEATURE);
		argToValueMap.put(USE_EVENT_FEATURE, DEFAULT_USE_EVENT);
		//event extractor
		valid_opt_set.add(EVENT_EXTRACTOR);
		argToValueMap.put(EVENT_EXTRACTOR, DEFAULT_EVENT_EXTRACTOR);
		//use precise match
		valid_opt_set.add(USE_PRECISE_MATCH);
		argToValueMap.put(USE_PRECISE_MATCH, DEFAULT_USE_PRECISE_MATCH);
		//use demonym
		valid_opt_set.add(USE_DEMONYM);
		argToValueMap.put(USE_DEMONYM, DEFAULT_USE_DEMONYM);
		//extract mention attribute
		valid_opt_set.add(EXTRACT_MENTION_ATTRIBUTE);
		argToValueMap.put(EXTRACT_MENTION_ATTRIBUTE, DEFAULT_EXTRACT_MENTION_ATTRIBUTE);
		//relation extractor
		valid_opt_set.add(EXTRACT_MENTION_RELATION);
		argToValueMap.put(EXTRACT_MENTION_RELATION, DEFAULT_EXTRACT_MENTION_RELATION);
		valid_opt_set.add(APPOSITION_EXTRACTOR);
		argToValueMap.put(APPOSITION_EXTRACTOR, DEFAULT_APPOSITION_EXTRACTOR);
		valid_opt_set.add(ROLEAPPOSITION_EXTRACTOR);
		argToValueMap.put(ROLEAPPOSITION_EXTRACTOR, DEFAULT_ROLEAPPOSITION_EXTRACTOR);
		valid_opt_set.add(LISTMEMBER_EXTRACTOR);
		argToValueMap.put(LISTMEMBER_EXTRACTOR, DEFAULT_LISTMEMBER_EXTRACTOR);
		valid_opt_set.add(PREDICATENOMINATIVE_EXTRACTOR);
		argToValueMap.put(PREDICATENOMINATIVE_EXTRACTOR, DEFAULT_PREDICATENOMINATIVE_EXTRACTOR);
		valid_opt_set.add(RELATIVEPRONOUN_EXTRACTOR);
		argToValueMap.put(RELATIVEPRONOUN_EXTRACTOR, DEFAULT_RELATIVEPRONOUN_EXTRACTOR);
		//training tmp file
		valid_opt_set.add(CREATE_TRAININGTMP);
		argToValueMap.put(CREATE_TRAININGTMP, DEFAULT_CREATE_TRAININGTMP);
		//post-processiong
		valid_opt_set.add(POST_PROCESSING);
		argToValueMap.put(POST_PROCESSING, DEFAULT_POST_PROCESSING);
		//ontonotes
		valid_opt_set.add(ONTONOTES);
		argToValueMap.put(ONTONOTES, DEFAULT_ONTONOTES);
		//clean doc
		valid_opt_set.add(CLEANDOC);
		argToValueMap.put(CLEANDOC, DEFAULT_CLEANDOC);
		//conll scorer
		valid_opt_set.add(CONLL_SCORER);
		//maxIter
		valid_opt_set.add(MAX_ITER);
		argToValueMap.put(MAX_ITER, DEFAULT_MAX_ITER);
		//initIter
		valid_opt_set.add(INIT_ITER);
		argToValueMap.put(INIT_ITER, DEFAULT_ITIN_ITER);
		//file name
		valid_opt_set.add(TRAININGFILE);
		valid_opt_set.add(TESTFILE);
		valid_opt_set.add(DEVFILE);
		valid_opt_set.add(OUTFILE);
		valid_opt_set.add(GOLDFILE);
		valid_opt_set.add(MODELFILE);
		valid_opt_set.add(LOGFILE);
		valid_opt_set.add(CONFIGURATION);
		valid_opt_set.add(PROPERTYFILE);
		argToValueMap.put(PROPERTYFILE, DEFAULT_PROPERTY_FILE);
		valid_opt_set.add(WORDNETDIR);
		argToValueMap.put(WORDNETDIR, DEFAULT_WORDNETDIR);
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
		maxIter = Integer.parseInt(argToValueMap.get(MAX_ITER));
		initIter = Integer.parseInt(argToValueMap.get(INIT_ITER));
	}
	
	private void parseOptions(String configfile) throws OptionException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configfile)));
			String line = reader.readLine();
			while(line != null) {
				line = line.trim();
				if(line.length() == 0) {
					line = reader.readLine();
					continue;
				}
				String[] tokens = line.split("=");
				if(!valid_opt_set.contains(tokens[0])) {
					reader.close();
					throw new OptionException("unexpected argument name: " + tokens[0] + "\n" + helpInfo());
				}
				else {
					argToValueMap.put(tokens[0], tokens[1]);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			throw new OptionException(e.getMessage());
		}
	}
	
	public Options() {
		init();
	}
	
	public Options(String[] args) {
		init();
		try {
			parseOptions(args);
		} catch (OptionException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private String getArgValue(String argName) {
		String value = argToValueMap.get(argName);
		if(value == null) {
			throw new RuntimeException("the value of argument " + argName + " is not available");
		}
		return value;
	}
	
	private void putArgValue(String argName, String value) {
		argToValueMap.put(argName, value);
	}
	
	public String getDocReader() {
		return getArgValue(DOC_READER);
	}
	
	public String getTrainReader() {
		return getArgValue(TRAIN_READER);
	}
	
	public String getDocWriter() {
		return getArgValue(DOC_WRITER);
	}
	
	public String getMode() {
		return getArgValue(MODE);
	}
	
	public int getThreadNum() {
		return Integer.parseInt(getArgValue(THREAD_NUM));
	}
	
	public String getTrainer() {
		return getArgValue(TRAINER);
	}
	
	public String getDecoder() {
		return getArgValue(DECODER);
	}
	
	public String getParamInitializer() {
		return getArgValue(PARAMETER_INITIALIZER);
	}
	
	public String getMentionParamSmoother() {
		return getArgValue(MENTION_PARAMETER_SMOOTHER);
	}
	
	public String getEventParamSmoother() {
	    return getArgValue(EVENT_PARAMETER_SMOOTHER);
	}
	
	public double getSmoothingAlpha() {
		return Double.valueOf(argToValueMap.get(SMOOTHER_ALPHA));
	}
	
	public double getUpperAlpha() {
		return Double.valueOf(argToValueMap.get(ALPHA_UPPER));
	}
	
	public double getLowerAlpha() {
		return Double.valueOf(argToValueMap.get(ALPHA_LOWER));
	}
	
	public boolean tuneAlpha() {
		return Boolean.parseBoolean(getArgValue(TUNE_ALPHA));
	}
	
	public double getSmoothingBeta() {
	    return Double.valueOf(argToValueMap.get(SMOOTHER_BETA));
	}
	
	public String getMentionExtractor() {
		return getArgValue(MENTION_EXTRACTOR);
	}
	
	public boolean usePreciseMatch() {
		return Boolean.parseBoolean(getArgValue(USE_PRECISE_MATCH));
	}
	
	public boolean useEventFeature() {
		return Boolean.parseBoolean(getArgValue(USE_EVENT_FEATURE));
	}
	
	public boolean useDemonym() {
		return Boolean.parseBoolean(getArgValue(USE_DEMONYM));
	}
	
	public boolean extractMentionAttribute() {
		return Boolean.parseBoolean(getArgValue(EXTRACT_MENTION_ATTRIBUTE));
	}
	
	public boolean extractMentionRelation() {
		return Boolean.parseBoolean(getArgValue(EXTRACT_MENTION_RELATION));
	}
	
	public boolean postProcessing() {
		return Boolean.parseBoolean(getArgValue(POST_PROCESSING));
	}
	
	public boolean OntoNotes() {
		return Boolean.parseBoolean(getArgValue(ONTONOTES));
	}
	
	public boolean cleanDoc() {
		return Boolean.parseBoolean(getArgValue(CLEANDOC));
	}
	
	public String getEventExtractor() {
		return getArgValue(EVENT_EXTRACTOR);
	}
	
	public String getListMemberRelationExtractor() {
		return getArgValue(LISTMEMBER_EXTRACTOR);
	}
	
	public String getAppositionRelationExtractor() {
		return getArgValue(APPOSITION_EXTRACTOR);
	}
	
	public String getRoleAppositionRelationExtractor() {
		return getArgValue(ROLEAPPOSITION_EXTRACTOR);
	}
	
	public String getPredicateNominativeRelationExtractor() {
		return getArgValue(PREDICATENOMINATIVE_EXTRACTOR);
	}
	
	public String getRelativePronounRelationExtractor() {
		return getArgValue(RELATIVEPRONOUN_EXTRACTOR);
	}
	
	public int maxIter() {
		return maxIter;
	}
	
	public int initIter() {
		return initIter;
	}
	
	public double stopEta() {
		return stop_eta;
	}
	
	public boolean createTrainingTmp() {
		return Boolean.parseBoolean(getArgValue(CREATE_TRAININGTMP));
	}
	
	public String getTrainTmp() {
		return train_tmp;
	}
	
	public String getCoNLLScorer() {
		return getArgValue(CONLL_SCORER);
	}
	
	public String getTrainingFile() {
		return getArgValue(TRAININGFILE);
	}
	
	public String getTestFile() {
		return getArgValue(TESTFILE);
	}
	
	public String getDevFile() {
		return getArgValue(DEVFILE);
	}
	
	public String getOutFile() {
		return getArgValue(OUTFILE);
	}
	
	public String getGoldFile() {
		return getArgValue(GOLDFILE);
	}
	
	public String getLogFile() {
		return getArgValue(LOGFILE);
	}
	
	public String getModelFile() {
		return getArgValue(MODELFILE);
	}
	
	public String getPropFile() {
		return getArgValue(PROPERTYFILE);
	}
	
	public String getWordNet() {
	    return getArgValue(WORDNETDIR);
	}
	
	public void putDocReader(String reader) {
		putArgValue(DOC_READER, reader);
	}
	
	public void putDocWriter(String writer) {
		putArgValue(DOC_WRITER, writer);
	}
	
	public void setPostProcessing(boolean postProcessing) {
		putArgValue(POST_PROCESSING, Boolean.toString(postProcessing));
	}
	
	public void setOntoNotes(boolean OntoNotes) {
		putArgValue(ONTONOTES, Boolean.toString(OntoNotes));
	}
	
	public void setCleanDoc(boolean cleanDoc) {
		putArgValue(CLEANDOC, Boolean.toString(cleanDoc));
	}
	
	public void setUseDemonym(boolean useDemonym) {
		putArgValue(USE_DEMONYM, Boolean.toString(useDemonym));
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		//write extract mention attribute
		out.writeBoolean(extractMentionAttribute());
		//write mention extractor
		out.writeObject(getMentionExtractor());
		//write extract mention relation
		out.writeBoolean(extractMentionRelation());
		//write apposition relation extractor
		out.writeObject(getAppositionRelationExtractor());
		//write role apposition relation extractor
		out.writeObject(getRoleAppositionRelationExtractor());
		//write list member relation extractor
		out.writeObject(getListMemberRelationExtractor());
		//write predicate nominative relation extractor
		out.writeObject(getPredicateNominativeRelationExtractor());
		//write relative pronoun relation extractor
		out.writeObject(getRelativePronounRelationExtractor());
		//write use precise match
		out.writeBoolean(usePreciseMatch());
		//write use event feature
		out.writeBoolean(useEventFeature());
		//write event extractor
		out.writeObject(getEventExtractor());
		//write propertity file
		out.writeObject(getPropFile());
		//write wordnet dir
		out.writeObject(getWordNet());
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		argToValueMap = new gnu.trove.map.hash.THashMap<String, String>();
		//read extract mention attribute
		boolean flag = in.readBoolean();
		argToValueMap.put(EXTRACT_MENTION_ATTRIBUTE, Boolean.toString(flag));
		//read mention extractor
		String mentionExtractor = (String) in.readObject();
		argToValueMap.put(MENTION_EXTRACTOR, mentionExtractor);
		//read extract mention relation
		flag = in.readBoolean();
		argToValueMap.put(EXTRACT_MENTION_RELATION, Boolean.toString(flag));
		//read apposition relation extractor
		String apposExtractor = (String) in.readObject();
		argToValueMap.put(APPOSITION_EXTRACTOR, apposExtractor);
		//read role apposition relation extractor
		String roleApposExtractor = (String) in.readObject();
		argToValueMap.put(ROLEAPPOSITION_EXTRACTOR, roleApposExtractor);
		//read list member relation extractor
		String listExtractor = (String) in.readObject();
		argToValueMap.put(LISTMEMBER_EXTRACTOR, listExtractor);
		//read predicate nominative relation extractor
		String predNomiExtractor = (String) in.readObject();
		argToValueMap.put(PREDICATENOMINATIVE_EXTRACTOR, predNomiExtractor);
		//read relative pronoun relation extractor
		String relPronExtractor = (String) in.readObject();
		argToValueMap.put(RELATIVEPRONOUN_EXTRACTOR, relPronExtractor);
		//read use precise match
		flag = in.readBoolean();
		argToValueMap.put(USE_PRECISE_MATCH, Boolean.toString(flag));
		//read use event feature
		flag = in.readBoolean();
		argToValueMap.put(USE_EVENT_FEATURE, Boolean.toString(flag));
		//read event extractor
		String eventExtractor = (String) in.readObject();
		argToValueMap.put(EVENT_EXTRACTOR, eventExtractor);
		//read propertity file
		String propfile = (String) in.readObject();
		argToValueMap.put(PROPERTYFILE, propfile);
		//read wornet dir
		String wordnetDir = (String) in.readObject();
		argToValueMap.put(WORDNETDIR, wordnetDir);
	}
}
