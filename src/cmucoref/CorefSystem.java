package cmucoref;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import cmucoref.decoder.Decoder;
import cmucoref.document.Document;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;
import cmucoref.io.ObjectReader;
import cmucoref.manager.CorefManager;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.model.CorefModel;
import cmucoref.model.Options;
import cmucoref.trainer.Trainer;
import edu.stanford.nlp.io.StringOutputStream;
import edu.stanford.nlp.util.SystemUtils;

public class CorefSystem {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws CreatingInstanceException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException {
		Options options = new Options(args);
		if(options.getMode().equals("train")){
			CorefModel model = new CorefModel(options);
			Mention.options = options;
			Trainer trainer = (Trainer) Class.forName(options.getTrainer()).newInstance();
			MentionExtractor mentionExtractor = (MentionExtractor) Class.forName(options.getMentionExtractor()).newInstance();
			mentionExtractor.createDict(options.getPropFile());
			CorefManager manager = new CorefManager(mentionExtractor);
			Decoder decoder = (Decoder) Class.forName(options.getDecoder()).newInstance();
			trainer.train(manager, decoder, model, options.getTrainingFile(), options.getDevFile(),
					options.getLogFile(), options.getModelFile());
		}
		else if(options.getMode().equals("test")){
			System.out.print("Loading Model...");
			long clock = System.currentTimeMillis() / 1000;
			CorefModel model = loadModel(options.getModelFile());
			System.out.println("Done. Took: " + (System.currentTimeMillis() / 1000 - clock) + "s.");
			
			clock = System.currentTimeMillis() / 1000;
			Mention.options = model.options;
			Decoder decoder = (Decoder) Class.forName(options.getDecoder()).newInstance();
			model.options.putDocReader(options.getDocReader());
			model.options.putDocWriter(options.getDocWriter());
			model.options.setPostProcessing(options.postProcessing());
			model.options.setOntoNotes(options.OntoNotes());
			
			MentionExtractor mentionExtractor = (MentionExtractor) Class.forName(model.options.getMentionExtractor()).newInstance();
			mentionExtractor.createDict(model.options.getPropFile());
			CorefManager manager = new CorefManager(mentionExtractor);
			
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
			DocumentWriter docWriter = DocumentWriter.createDocumentWriter(model.options.getDocWriter());
			
			docReader.startReading(options.getTestFile());
			docWriter.startWriting(options.getOutFile());
			
			Document doc = docReader.getNextDocument(false);
			while(doc != null){
				System.out.println("Processing Doc: " + doc.getFileName() + " part: " + doc.getDocId());
				List<List<Mention>> mentionList = decoder.decode(doc, manager, model);
				doc.assignCorefClustersToDocument(mentionList, model.options.postProcessing());
				docWriter.writeDocument(doc, true);
				
				doc = docReader.getNextDocument(false);
			}
			docReader.close();
			docWriter.close();
			System.out.println("Done. Took: " + (System.currentTimeMillis() / 1000 - clock) + "s.");
			
			String scorer = options.getCoNLLScorer();
			String goldfile = options.getGoldFile();
			if(scorer != null && goldfile != null){
				System.out.println(getConllEvalSummary(scorer, goldfile, options.getOutFile()));
			}
		}
		else{
			System.err.println("unexpected mode: " + options.getMode());
		}
	}

	private static CorefModel loadModel(String file) throws ClassNotFoundException, IOException {
		ObjectReader in = new ObjectReader(file);
		CorefModel model = (CorefModel) in.readObject();
		in.close();
		return model;
	}
	
	protected static String getConllEvalSummary(String conllMentionEvalScript,
		      String goldFile, String predictFile) throws IOException {
		ProcessBuilder process = new ProcessBuilder(conllMentionEvalScript, "all", goldFile, predictFile, "none");
	    StringOutputStream errSos = new StringOutputStream();
	    StringOutputStream outSos = new StringOutputStream();
	    PrintWriter out = new PrintWriter(outSos);
	    PrintWriter err = new PrintWriter(errSos);
	    SystemUtils.run(process, out, err);
	    out.close();
	    err.close();
	    String summary = outSos.toString();
	    String errStr = errSos.toString();
	    if ( ! errStr.isEmpty()) {
	      summary += "\nERROR: " + errStr;
	    }
	    
	    return summary;
	}
}
