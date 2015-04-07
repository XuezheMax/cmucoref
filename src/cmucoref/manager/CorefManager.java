package cmucoref.manager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cmucoref.io.ObjectWriter;
import cmucoref.document.Document;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.DocumentReader;
import cmucoref.mention.Mention;
import cmucoref.mention.extractor.MentionExtractor;
import cmucoref.mention.featgen.MentionFeatureGenerator;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

public class CorefManager {
	
	MentionExtractor mentionExtractor = null;
	MentionFeatureGenerator mentionFeatGen = null;
	
	public CorefManager(MentionExtractor mentionExtractor){
		this.mentionExtractor = mentionExtractor;
		this.mentionFeatGen = new MentionFeatureGenerator();
	}
	
	public int createDocInstance(String trainfile, String traintmp, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException{
		long clock = System.currentTimeMillis();
		System.out.print("Creating Alphabet ... ");
		File trainFile = new File(trainfile);
		int numInst = createAlphabet(trainFile, model);
		model.closeAlphabets();
		System.out.println("Done.");
		System.out.println("Num Features: " + model.featureSize());
		System.out.println("Num Sentences: " + numInst);
		
		boolean createTmpFile = model.options.createTrainingTmp();
		if(createTmpFile){
			System.out.print("Creating Training Instances: ");
			int threadNum = model.threadNum();
			CreateTrainingTmpThread[] threads = new CreateTrainingTmpThread[threadNum];
			String[] tokens = traintmp.split("\\.");
			for(int i = 0; i < threadNum; i++){
				String tmpfile = tokens[0] + i + "." + tokens[1];
				threads[i] = new CreateTrainingTmpThread(i, this, trainfile, tmpfile, model);
			}
			
			for(CreateTrainingTmpThread thread : threads){
				thread.start();
			}
			for(CreateTrainingTmpThread thread : threads){
				try {
					thread.join();
				} catch (InterruptedException e) {
					throw new CreatingInstanceException(e.getMessage());
				}
			}
			System.out.println("Done.");
		}
		
		System.out.println("Took " + (System.currentTimeMillis() - clock) / 1000 + "s.");
		
		//model.displayAlphabet(new PrintWriter(System.out));
		return numInst;
	}
	
	private int createAlphabet(File file, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		int numInst = 0;
		if(file.isDirectory()){
			File[] files = file.listFiles();
			Arrays.sort(files);
			for(File subFile : files){
				numInst += createAlphabet(subFile, model);
			}
		}
		else if(file.isFile()){
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
			docReader.startReading(file.getAbsolutePath());
			Document doc = docReader.getNextDocument();
			while(doc != null){
				numInst++;
				List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
				List<Mention> allMentions = new ArrayList<Mention>();
				for(List<Mention> mentions : mentionList){
					allMentions.addAll(mentions);
				}
				
				for(int i = 1; i < allMentions.size(); ++i){
					Mention anaph = allMentions.get(i);
					if(anaph.apposTo != null){
						Mention antec = anaph.apposTo;
						mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						continue;
					}
					
					if(anaph.predNomiTo != null){
						Mention antec = anaph.predNomiTo;
						mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						continue;
					}
					
					for(int j = 0; j <= i; ++j){
						if(j < i){
							Mention antec = allMentions.get(j);
							if(antec.cover(anaph) || anaph.cover(antec)){
								continue;
							}
							mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						}
						else{
							mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), null, null, model, null);
						}
					}
				}
				doc = docReader.getNextDocument();
			}
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
		return numInst;
	}
	
	protected void writeDocInstance(Document doc, ObjectWriter out, CorefModel model) throws IOException{
		List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = new ArrayList<Mention>();
		for(List<Mention> mentions : mentionList){
			allMentions.addAll(mentions);
		}
		
		out.writeInt(allMentions.size());
		out.writeInt(-4);
		out.reset();
		
		for(int i = 1; i < allMentions.size(); ++i){
			Mention anaph = allMentions.get(i);
			if(anaph.apposTo != null){
				out.writeInt(1);
				Mention antec = anaph.apposTo;
				FeatureVector fv = new FeatureVector();
				mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
				out.writeObject(fv.keys());
				continue;
			}
			
			if(anaph.predNomiTo != null){
				out.writeInt(2);
				Mention antec = anaph.predNomiTo;
				FeatureVector fv = new FeatureVector();
				mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
				out.writeObject(fv.keys());
				continue;
			}
			
			out.writeInt(3);
			out.writeInt(i);
			for(int j = 0; j <= i; ++j){
				if(j < i){
					Mention antec = allMentions.get(j);
					if(antec.cover(anaph) || anaph.cover(antec)){
						out.writeInt(-1);
						continue;
					}
					out.writeInt(1);
					FeatureVector fv = new FeatureVector();
					mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
					out.writeObject(fv.keys());
				}
				else{
					out.writeInt(0);
					FeatureVector fv = new FeatureVector();
					mentionFeatGen.genMentionFeatures(anaph, doc.getSentence(anaph.sentID), null, null, model, fv);
					out.writeObject(fv.keys());
				}
			}
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
	public CreateTrainingTmpThread(int threadId, CorefManager manager, String trainingfile, String tmpfile, CorefModel model){
		this.threadId = threadId;
		this.manager = manager;
		this.trainingfile = trainingfile;
		this.tmpfile = tmpfile;
		this.model = model;
	}
	
	private void writeTmpFile(File file, ObjectWriter out, CorefModel model) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(file.isDirectory()){
			File[] files = file.listFiles();
			Arrays.sort(files);
			for(File subFile : files){
				writeTmpFile(subFile, out, model);
			}
		}
		else if(file.isFile()){
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
			docReader.startReading(file.getAbsolutePath());
			Document doc = docReader.getNextDocument();
			int num = 0;
			int threadNum = model.threadNum();
			int count = 0;
			while(doc != null){
				if((num % threadNum) == threadId){
					manager.writeDocInstance(doc, out, model);
					count++;
				}
				++num;
				doc = docReader.getNextDocument();
			}
			currentNum += count;
			System.out.println(currentNum);
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
	}
	
	public void run(){
		try {
			ObjectWriter out = new ObjectWriter(tmpfile);
			writeTmpFile(new File(trainingfile), out, model);
			out.close();
		} catch(IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e){
			e.printStackTrace();
		}
	}
}
