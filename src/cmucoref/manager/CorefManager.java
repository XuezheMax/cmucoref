package cmucoref.manager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cmucoref.document.Document;
import cmucoref.document.Sentence;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.DocumentReader;
import cmucoref.io.ObjectWriter;
import cmucoref.mention.Dictionaries;
import cmucoref.mention.Mention;
import cmucoref.mention.WordNet;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.featgen.MentionFeatureGenerator;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

public class CorefManager {
	
	public MentionExtractor mentionExtractor = null;
	public MentionFeatureGenerator mentionFeatGen = null;
	
	public CorefManager(MentionExtractor mentionExtractor){
		this.mentionExtractor = mentionExtractor;
		this.mentionFeatGen = new MentionFeatureGenerator();
	}
	
	public Dictionaries getDict(){
		return mentionExtractor.getDict();
	}
	
	public WordNet getWordNet() {
	    return mentionExtractor.getWordNet();
	}
	
	public int[] createDocInstance(String trainfile, String traintmp, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException {
		long clock = System.currentTimeMillis();
		System.out.println("Creating Alphabet ... ");
		File trainFile = new File(trainfile);
		int numInst = createAlphabet(trainFile, model);
		model.closeAlphabets();
		System.out.println("Done.");
		System.out.println("Num Mention Features: " + model.mentionFeatureSize());
		if(model.options.useEventFeature()) {
			System.out.println("Num of Events: " + mentionExtractor.sizeOfEvent());
			System.out.println("Num Event Features: " + model.eventFeatureSize());
		}
		System.out.println("Num Documents: " + numInst);
		System.out.print("Creating Training Instances: ");
		int threadNum = model.threadNum();
		CreateTrainingTmpThread[] threads = new CreateTrainingTmpThread[threadNum];
		String[] tokens = traintmp.split("\\.");
		for(int i = 0; i < threadNum; i++){
			String tmpfile = tokens[0] + i + "." + tokens[1];
			threads[i] = new CreateTrainingTmpThread(i, this, trainfile, tmpfile, model);
		}
			
		for(CreateTrainingTmpThread thread : threads) {
			thread.start();
		}
		for(CreateTrainingTmpThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new CreatingInstanceException(e.getMessage());
			}
		}
		System.out.print("Done.");
		
		System.out.println("Took " + (System.currentTimeMillis() - clock) / 1000 + "s.");
		System.out.println();
		
		int[] res = new int[threadNum + 1];
		for(int i = 0; i < threadNum; i++) {
			res[i] = threads[i].numInst;
		}
		res[threadNum] = numInst;
		return res;
	}
	
	private int createAlphabet(File file, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		int numInst = 0;
		if(file.isDirectory()) {
			File[] files = file.listFiles();
			Arrays.sort(files);
			for(File subFile : files) {
				numInst += createAlphabet(subFile, model);
			}
		}
		else if(file.isFile()) {
			System.out.print(file.getName() + " ");
			System.out.flush();
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getTrainReader());
			docReader.startReading(file.getAbsolutePath());
			Document doc = docReader.getNextDocument(model.options, false);
			
			boolean useEvent = model.options.useEventFeature();
			
			while(doc != null) {
				numInst++;
				List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
				List<Mention> allMentions = mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
				
				for(int i = 0; i < allMentions.size(); ++i) {
					Mention anaph = allMentions.get(i);
					Sentence anaphSent = doc.getSentence(anaph.sentID);
					
					//mode=3
					if(!anaph.isDefinite()) {
						continue;
					}
					
					//mode=1;
					if(anaph.stringMatchs != null) {
						for(Mention antec : anaph.stringMatchs) {
							Sentence antecSent = doc.getSentence(antec.sentID);
							mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 1, 
									getDict(), model, useEvent, null, null);
						}
						continue;
					}
					
					//mode=2;
					if(anaph.preciseMatchs != null) {
						for(Mention antec : anaph.preciseMatchs) {
							Sentence antecSent = doc.getSentence(antec.sentID);
							mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 2, 
									getDict(), model, useEvent, null, null);
						}
						continue;
					}
					
					//mode=0;
					for(int j = 0; j <= i; ++j) {
						Mention antec = (j == i ? null : allMentions.get(j));
						Sentence antecSent = (antec == null ? null : doc.getSentence(antec.sentID));
						if(anaph.ruleout(anaphSent, antec, antecSent, getDict(), getWordNet(), false)) {
							continue;
						}
							
						mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 0, 
								getDict(), model, useEvent, null, null);
					}
				}
				doc = docReader.getNextDocument(model.options, false);
			}
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
		return numInst;
	}
	
	protected void writeDocInstance(Document doc, ObjectWriter out, CorefModel model) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
		
		out.writeInt(allMentions.size());
		out.writeInt(-4);
		out.reset();
		
		boolean useEvent = model.options.useEventFeature();
		
		for(int i = 0; i < allMentions.size(); ++i) {
			Mention anaph = allMentions.get(i);
			Sentence anaphSent = doc.getSentence(anaph.sentID);
			
			FeatureVector[] mfvs = null;
			FeatureVector[] efvs = null;
			
			//mode=3
			if(!anaph.isDefinite()) {
				mfvs = new FeatureVector[0];
				efvs = useEvent ? new FeatureVector[0] : null;
				out.writeObject(mfvs);
				out.writeObject(efvs);
				continue;
			}
			
			//mode=1
			if(anaph.stringMatchs != null) {
				mfvs = new FeatureVector[anaph.stringMatchs.size()];
				efvs = useEvent ? new FeatureVector[anaph.stringMatchs.size()] : null;
				int j = 0;
				for(Mention antec : anaph.stringMatchs) {
					Sentence antecSent = doc.getSentence(antec.sentID);
					mfvs[j] = new FeatureVector();
					if(useEvent) {
						efvs[j] = new FeatureVector();
					}
					mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 1, 
							getDict(), model, useEvent, mfvs[j], useEvent ? efvs[j] : null);
					j++;
				}
				out.writeObject(mfvs);
				out.writeObject(efvs);
				continue;
			}
			
			//mode=2
			if(anaph.preciseMatchs != null) {
				mfvs = new FeatureVector[anaph.preciseMatchs.size()];
				efvs = useEvent ? new FeatureVector[anaph.preciseMatchs.size()] : null;
				int j = 0;
				for(Mention antec : anaph.preciseMatchs) {
					Sentence antecSent = doc.getSentence(antec.sentID);
					mfvs[j] = new FeatureVector();
					if(useEvent) {
						efvs[j] = new FeatureVector();
					}
					mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 2, 
							getDict(), model, useEvent, mfvs[j], useEvent ? efvs[j] : null);
					j++;
				}
				out.writeObject(mfvs);
				out.writeObject(efvs);
				continue;
			}
			
			//mode=0;
			// 10 new-cluster per sent
			mfvs = new FeatureVector[i + (anaphSent.getId() + 1) * 10];
			efvs = useEvent ? new FeatureVector[i + (anaphSent.getId() + 1) * 10] : null;
			// 1 new-cluster per mention
//			mfvs = new FeatureVector[i + i + 1];
//          efvs = useEvent ? new FeatureVector[i + i + 1] : null;
			for(int j = 0; j < mfvs.length; ++j) {
				Mention antec = (j < i ? allMentions.get(j) : null);
				Sentence antecSent = (antec == null ? null : doc.getSentence(antec.sentID));
				if(anaph.ruleout(anaphSent, antec, antecSent, getDict(), getWordNet(), false)) {
					continue;
				}
					
				mfvs[j] = new FeatureVector();
				if(useEvent) {
					efvs[j] = new FeatureVector();
				}
				mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 0, 
						getDict(), model, useEvent, mfvs[j], useEvent ? efvs[j] : null);
			}
			out.writeObject(mfvs);
			out.writeObject(efvs);
		}
		out.writeInt(-3);
	}
}

class CreateTrainingTmpThread extends Thread {
	private static int currentNum = 0;
	private int threadId;
	private CorefManager manager = null;
	private String tmpfile = null;
	private CorefModel model = null;
	private String trainingfile = null;
	
	public int numInst = 0;
	
	public CreateTrainingTmpThread(int threadId, CorefManager manager, String trainingfile, String tmpfile, CorefModel model){
		this.threadId = threadId;
		this.manager = manager;
		this.trainingfile = trainingfile;
		this.tmpfile = tmpfile;
		this.model = model;
	}
	
	private int writeTmpFile(File file, ObjectWriter out, CorefModel model) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(file.isDirectory()){
			int count = 0;
			File[] files = file.listFiles();
			Arrays.sort(files);
			for(File subFile : files){
				count += writeTmpFile(subFile, out, model);
			}
			return count;
		}
		else if(file.isFile()){
			boolean createTmpFile = model.options.createTrainingTmp();
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getTrainReader());
			docReader.startReading(file.getAbsolutePath());
			Document doc = docReader.getNextDocument(model.options, false);
			int num = 0;
			int threadNum = model.threadNum();
			int count = 0;
			while(doc != null){
				if((num % threadNum) == threadId){
					if(createTmpFile){
						manager.writeDocInstance(doc, out, model);
					}
					count++;
				}
				++num;
				doc = docReader.getNextDocument(model.options, false);
			}
			currentNum += count;
			System.out.print(currentNum + " ");
			return count;
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
	}
	
	public void run(){
		try {
			ObjectWriter out = model.options.createTrainingTmp() ? new ObjectWriter(tmpfile) : null;
			this.numInst = writeTmpFile(new File(trainingfile), out, model);
			if(out != null){
				out.close();
			}
		} catch(IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e){
			e.printStackTrace();
		}
	}
}
