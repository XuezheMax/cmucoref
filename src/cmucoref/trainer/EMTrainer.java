package cmucoref.trainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import cmucoref.decoder.Decoder;
import cmucoref.document.Document;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.CoNLLXDocumentWriter;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;
import cmucoref.io.ObjectReader;
import cmucoref.manager.CorefManager;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.Feature;
import cmucoref.model.FeatureVector;
import cmucoref.util.Util;

import edu.stanford.nlp.io.StringOutputStream;
import edu.stanford.nlp.util.SystemUtils;

public class EMTrainer extends Trainer{

	@Override
	public void train(CorefManager manager, Decoder decoder, CorefModel model, String trainfile, String devfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException {
		int[] nums = manager.createDocInstance(trainfile, model.options.getTrainTmp(), model);
		model.createParameters(manager.mentionExtractor.getSizeOfEvent());
		int threadNum = model.threadNum();
		System.out.println("Num Features: " + model.mentionFeatureSize());
		if(model.options.useEventFeature()) {
			System.out.println("Num of Events: " + model.givenSizeofEvent() + " " + model.sizeOfEvent());
			System.out.println("Num Event Features: " + model.eventFeatureSize());
		}
		System.out.println("Num Documents: " + nums[threadNum]);
		System.out.println("Num Threads:   " + threadNum);
		
		final int maxIter = model.options.maxIter();
		final int initIter = model.options.initIter();
		EMThread.totalTrainInst = nums[threadNum];
		
		boolean useEvent = model.options.useEventFeature();
		String traintmp = model.options.getTrainTmp();
		int nsizeOfM = model.mentionFeatureSize();
		int gsizeOfM = model.givenSizeofMention();
		
		int nsizeOfE = useEvent ? model.eventFeatureSize() : 0;
		int gsizeOfE = useEvent ? model.givenSizeofEvent() : 0;
		
		
		PrintWriter logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logfile)));
		
		for(int itr = 1; itr <= maxIter; ++itr){
			System.out.print("iter=" + itr + "[   ");
			System.out.flush();
			boolean updateEvent = useEvent && itr > initIter;
			long clock = System.currentTimeMillis() / 1000;
			EMThread.currentNum = 0;
			EMThread[] threads = new EMThread[threadNum];
			String[] tokens = traintmp.split("\\.");
			for(int i = 0; i < threadNum; i++){
				String tmpfile = tokens[0] + i + "." + tokens[1];
				threads[i] = new EMThread(nums[i], manager, tmpfile, model, updateEvent);
			}
			
			for(int i = 0; i < threadNum; ++i){
				threads[i].start();
			}
			
			for(int i = 0; i < threadNum; ++i) {
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.print("\b\b\b");
			
			for(int i = 1; i < threadNum; ++i) {
				for(int j = 0; j < nsizeOfM; ++j) {
					threads[0].mFeatC[j] = Util.logsumexp(threads[0].mFeatC[j], threads[i].mFeatC[j]);
				}
				
				for(int j = 0; j < gsizeOfM; ++j) {
					threads[0].mGivenC[j] = Util.logsumexp(threads[0].mGivenC[j], threads[i].mGivenC[j]);
				}
				
				if(updateEvent) {
					for(int j = 0; j < nsizeOfE; ++j) {
						threads[0].eFeatC[j] = Util.logsumexp(threads[0].eFeatC[j], threads[i].eFeatC[j]);
					}
					
					for(int j = 0; j < gsizeOfE; ++j) {
						threads[0].eGivenC[j] = Util.logsumexp(threads[0].eGivenC[j], threads[i].eGivenC[j]);
					}
				}
			}
			
			for(int j = 0; j < nsizeOfM; ++j) {
				int gid = model.getMentionGidFromIndex(j);
				double val = threads[0].mFeatC[j] - threads[0].mGivenC[gid];
				model.updateMentionParams(j, val);
			}
			
			if(updateEvent) {
				for(int j = 0; j < nsizeOfE; ++j) {
					int gid = model.getEventGidFromIndex(j);
					double val = threads[0].eFeatC[j] - threads[0].eGivenC[gid];
					model.updateEventParams(j, val);
				}
			}
			
			System.out.println(EMThread.totalTrainInst + "|time=" + (System.currentTimeMillis() /1000 - clock) + "s." + "]");
			model.displayMentionAlphabet(new PrintStream(new File("mFeat" + itr + ".txt")));
			if(itr == 1 || updateEvent) {
				model.displayEventAlphabet(new PrintStream(new File("eFeat" + itr + ".txt")));
			}
			
			logWriter.println("Iter: " + itr);
			evaluateCurrentAcc(manager, decoder, model, updateEvent, devfile, logWriter);
			logWriter.println("--------------------------------------------------------------------------");
			logWriter.flush();
		}
		
		logWriter.close();
		System.out.print("Saving Model...");
		long clock = System.currentTimeMillis() / 1000;
		saveModel(model, modelfile);
		System.out.println("Done. Took: " + (System.currentTimeMillis() / 1000 - clock) + "s.");
	}
	
	protected void evaluateCurrentAcc(CorefManager manager, Decoder decoder, CorefModel model, boolean useEvent, 
			String devfile, PrintWriter logWriter) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(CoNLLXDocumentWriter.class.getName());
		
		docReader.startReading(devfile);
		String tempfile = "tmp/result.tmp";
		docWriter.startWriting(tempfile);
		
		Document doc = docReader.getNextDocument(model.options, false);
		while(doc != null){
			List<List<Mention>> mentionList = decoder.decode(doc, manager, model, useEvent);
			doc.assignCorefClustersToDocument(mentionList, model.options.postProcessing());
			docWriter.writeDocument(doc, true);
			
			doc = docReader.getNextDocument(model.options, false);
		}
		docReader.close();
		docWriter.close();
		
		String scorer = model.options.getCoNLLScorer();
		String summary = getConllEvalSummary(scorer, devfile, "tmp/result.tmp");
		logWriter.println(summary);
	}
	
	protected String getConllEvalSummary(String conllMentionEvalScript,
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

class EMThread extends Thread {
	public static int totalTrainInst = 0;
	public static int currentNum = 0;
	private int numTrainInst = 0;
	private CorefManager manager = null;
	private String tmpfile = null;
	private CorefModel model = null;
	
	public double[] mFeatC = null;
	public double[] mGivenC = null;
	
	public double[] eFeatC = null;
	public double[] eGivenC = null;
	
	boolean updateEvent = false;
	
	public EMThread(int numTrainInst, CorefManager manager, String tmpfile, CorefModel model, boolean updateEvent){
		this.numTrainInst = numTrainInst;
		this.manager = manager;
		this.tmpfile = tmpfile;
		this.model = model;
		mFeatC = new double[model.mentionFeatureSize()];
		mGivenC = new double[model.givenSizeofMention()];
		Arrays.fill(mFeatC, Double.NEGATIVE_INFINITY);
		Arrays.fill(mGivenC, Double.NEGATIVE_INFINITY);
		
		this.updateEvent = updateEvent;
		if(this.updateEvent) {
			eFeatC = new double[model.eventFeatureSize()];
			eGivenC = new double[model.givenSizeofEvent()];
			Arrays.fill(eFeatC, Double.NEGATIVE_INFINITY);
			Arrays.fill(eGivenC, Double.NEGATIVE_INFINITY);
		}
	}
	
	private void EMupdate(ObjectReader in, CorefModel model, CorefManager manager) throws IOException, ClassNotFoundException {
		for(int i = 0; i < numTrainInst; ++i) {
			if(i > 0 && i % 1000 == 0){
				currentNum += 1000;
				int percent = currentNum * 100 / totalTrainInst;
				System.out.print("\b\b\b" + (percent < 10 ? " " : "") + percent + "%");
				System.out.flush();
			}
			int sizeOfMention = in.readInt();
			int last = in.readInt();
			if(last != -4){
				throw new IOException("last number is not equal to -4");
			}
			
			for(int j = 0; j < sizeOfMention; ++j) {
				FeatureVector[] mfvs = (FeatureVector[]) in.readObject();
				FeatureVector[] efvs = (FeatureVector[]) in.readObject();
				double[] probs = new double[mfvs.length];
				double sumProb = Double.NEGATIVE_INFINITY;
				
				for(int k = 0; k < mfvs.length; ++k) {
					if(mfvs[k] == null) {
						continue;
					}
					probs[k] = model.getScore(mfvs[k], updateEvent ? efvs[k] : null);
					sumProb = Util.logsumexp(sumProb, probs[k]);
				}
				
				for(int k = 0; k < mfvs.length; ++k) {
					if(mfvs[k] == null) {
						continue;
					}
					double val = probs[k] - sumProb;
					for(Object obj : mfvs[k]) {
						Feature f = (Feature) obj;
						mFeatC[f.index] = Util.logsumexp(mFeatC[f.index], val);
						mGivenC[f.gid] = Util.logsumexp(mGivenC[f.gid], val);
					}
					if(updateEvent) {
						for(Object obj : efvs[k]) {
							Feature f = (Feature) obj;
							eFeatC[f.index] = Util.logsumexp(eFeatC[f.index], val);
							eGivenC[f.gid] = Util.logsumexp(eGivenC[f.gid], val);
						}
					}
				}
			}
			last = in.readInt();
			if(last != -3){
				throw new IOException("last number is not equal to -3");
			}
		}
	}
	
	public void run() {
		try {
			ObjectReader in = new ObjectReader(tmpfile);
			EMupdate(in, model, manager);
			in.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
}
