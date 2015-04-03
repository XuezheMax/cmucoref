package cmucoref;

import java.io.IOException;

import cmucoref.manager.CorefManager;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.model.CorefModel;
import cmucoref.model.Options;
import cmucoref.trainer.Trainer;

public class CorefSystem {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		Options options = new Options(args);
		if(options.getMode().equals("train")){
			CorefModel model = new CorefModel(options);
			Trainer trainer = (Trainer) Class.forName(options.getTrainer()).newInstance();
			MentionExtractor mentionExtractor = (MentionExtractor) Class.forName(options.getMentionExtractor()).newInstance();
			CorefManager manager = new CorefManager(mentionExtractor);
			trainer.train(manager, model, options.getTrainingFile(), options.getLogFile(), options.getModelFile());
		}
		else if(options.getMode().equals("test")){
			// TODO
		}
		else{
			System.err.println("unexpected mode: " + options.getMode());
		}
	}

}
