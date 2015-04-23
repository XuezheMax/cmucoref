package cmucoref.manager;

import java.io.File;
import java.io.IOException;
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
import cmucoref.util.Pair;

public class CorefManager {
	
	MentionExtractor mentionExtractor = null;
	MentionFeatureGenerator mentionFeatGen = null;
	
	public CorefManager(MentionExtractor mentionExtractor){
		this.mentionExtractor = mentionExtractor;
		this.mentionFeatGen = new MentionFeatureGenerator();
	}
	
	public int[] createDocInstance(String trainfile, String traintmp, CorefModel model) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException{
		long clock = System.currentTimeMillis();
		System.out.print("Creating Alphabet ... ");
		File trainFile = new File(trainfile);
		int numInst = createAlphabet(trainFile, model);
		model.closeAlphabets();
		System.out.println("Done.");
		System.out.println("Num Features: " + model.featureSize());
		System.out.println("Num Documents: " + numInst);
		
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
		System.out.print("Done.");
		
		System.out.println("Took " + (System.currentTimeMillis() - clock) / 1000 + "s.");
		System.out.println();
		
		int[] res = new int[threadNum + 1];
		for(int i = 0; i < threadNum; i++){
			res[i] = threads[i].numInst;
		}
		res[threadNum] = numInst;
		return res;
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
			Document doc = docReader.getNextDocument(false);
			while(doc != null){
				numInst++;
				List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
				List<Mention> allMentions = mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
				
				for(int i = 0; i < allMentions.size(); ++i){
					Mention anaph = allMentions.get(i);
					if(anaph.spanMatchs != null){
						boolean spMat = false;
						boolean hdMat = false;
						for(Mention antec : anaph.spanMatchs){
							boolean attrMat = mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
							if(attrMat){
								hdMat = true;
								spMat = spMat 
										|| antec.wordsInclude(doc.getSentence(antec.sentID), anaph, doc.getSentence(anaph.sentID))
										|| antec.relaxedSpanMatch(doc.getSentence(antec.sentID), anaph, doc.getSentence(anaph.sentID));
							}
						}
						//mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), spMat, hdMat, model, null);
						continue;
					}
					
					if(anaph.apposTo != null){
						Mention antec = anaph.apposTo;
						mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						continue;
					}
					
					if(anaph.predNomiTo != null){
						Mention antec = anaph.predNomiTo;
						mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						continue;
					}
					
					for(int j = 0; j <= i; ++j){
						if(j < i){
							Mention antec = allMentions.get(j);
							if(antec.cover(anaph) || anaph.cover(antec)){
								continue;
							}
							mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, null);
						}
						else{
							//mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), false, false, model, null);
						}
					}
				}
				doc = docReader.getNextDocument(false);
			}
		}
		else{
			throw new IOException(file.toString() + " is not a normal file or a directory");
		}
		return numInst;
	}
	
	protected void writeDocInstance(Document doc, ObjectWriter out, CorefModel model) throws IOException{
		List<List<Mention>> mentionList = mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
		
		out.writeInt(allMentions.size());
		out.writeInt(-4);
		out.reset();
		
		for(int i = 0; i < allMentions.size(); ++i){
			Mention anaph = allMentions.get(i);
			int[][] keys = null;
			int[][] gids = null;
			
			if(anaph.spanMatchs != null) {
				boolean spMat = false;
				boolean hdMat = false;
				keys = new int[anaph.spanMatchs.size() + 1][];
				gids = new int[anaph.spanMatchs.size() + 1][];
				for(int j = 0; j <= anaph.spanMatchs.size(); ++j){
					if(j < anaph.spanMatchs.size()){
						Mention antec = anaph.spanMatchs.get(j);
						FeatureVector fv = new FeatureVector();
						boolean attrMat = mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
						if(attrMat){
							hdMat = true;
							spMat = spMat 
									|| antec.wordsInclude(doc.getSentence(antec.sentID), anaph, doc.getSentence(anaph.sentID))
									|| antec.relaxedSpanMatch(doc.getSentence(antec.sentID), anaph, doc.getSentence(anaph.sentID));
						}
						Pair<int[], int[]> res = fv.keys();
						keys[j] = res.first;
						gids[j] = res.second;
					}
					else{
//						FeatureVector fv = new FeatureVector();
//						mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), spMat, hdMat, model, fv);
//						Pair<int[], int[]> res = fv.keys();
//						keys[j] = res.first;
//						gids[j] = res.second;
						keys[j] = null;
						gids[j] = null;
					}
				}
				out.writeObject(keys);
				out.writeObject(gids);
				continue;
			}
			
			if(anaph.apposTo != null){
				keys = new int[1][];
				gids = new int[1][];
				Mention antec = anaph.apposTo;
				FeatureVector fv = new FeatureVector();
				mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
				Pair<int[], int[]> res = fv.keys();
				keys[0] = res.first;
				gids[0] = res.second;
				out.writeObject(keys);
				out.writeObject(gids);
				continue;
			}
			
			if(anaph.predNomiTo != null){
				keys = new int[1][];
				gids = new int[1][];
				Mention antec = anaph.predNomiTo;
				FeatureVector fv = new FeatureVector();
				mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
				Pair<int[], int[]> res = fv.keys();
				keys[0] = res.first;
				gids[0] = res.second;
				out.writeObject(keys);
				out.writeObject(gids);
				continue;
			}
			
			keys = new int[i + 1][];
			gids = new int[i + 1][];
			for(int j = 0; j <= i; ++j){
				if(j < i){
					Mention antec = allMentions.get(j);
					if(antec.cover(anaph) || anaph.cover(antec)){
						keys[j] = null;
						gids[j] = null;
						continue;
					}
					FeatureVector fv = new FeatureVector();
					mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), antec, doc.getSentence(antec.sentID), model, fv);
					Pair<int[], int[]> res = fv.keys();
					keys[j] = res.first;
					gids[j] = res.second;
				}
				else{
//					FeatureVector fv = new FeatureVector();
//					mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), false, false, model, fv);
//					Pair<int[], int[]> res = fv.keys();
//					keys[j] = res.first;
//					gids[j] = res.second;
					keys[j] = null;
					gids[j] = null;
				}
			}
			out.writeObject(keys);
			out.writeObject(gids);
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
			DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
			docReader.startReading(file.getAbsolutePath());
			Document doc = docReader.getNextDocument(false);
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
				doc = docReader.getNextDocument(false);
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