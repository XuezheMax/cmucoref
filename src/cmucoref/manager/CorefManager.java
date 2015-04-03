package cmucoref.manager;

import java.io.File;
import java.io.IOException;

import cmucoref.io.DocumentReader;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.featgen.MentionFeatureGenerator;
import cmucoref.model.CorefModel;

public class CorefManager {
	
	MentionExtractor mentionExtractur = null;
	MentionFeatureGenerator mentionFeatGen = null;
	
	public CorefManager(MentionExtractor mentionExtractor){
		this.mentionExtractur = mentionExtractor;
		this.mentionFeatGen = new MentionFeatureGenerator();
	}
	
	public void createDocInstance(String trainfile, String traintmp, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		File trainFile = new File(trainfile);
		createAlphabet(trainFile, model);
		// TODO
	}
	
	private void createAlphabet(File file, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		if(file.isDirectory()){
			File[] files = file.listFiles();
			for(File subFile : files){
				createAlphabet(subFile, model);
			}
		}
		else if(file.isFile()){
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
			docReader.startReading(file.getAbsolutePath());
			// TODO
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
	}
	
	
}
