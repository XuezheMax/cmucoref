package cmucoref.trainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import cmucoref.decoder.Decoder;
import cmucoref.document.Document;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.CoNLLDocumentWriter;
import cmucoref.io.DocumentReader;
import cmucoref.io.DocumentWriter;
import cmucoref.io.ObjectReader;
import cmucoref.manager.CorefManager;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

import edu.stanford.nlp.io.StringOutputStream;
import edu.stanford.nlp.util.SystemUtils;

public class EMTrainer extends Trainer{

	@Override
	public void train(CorefManager manager, Decoder decoder, CorefModel model, String trainfile, String devfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException {
		int[] nums = manager.createDocInstance(trainfile, model.options.getTrainTmp(), model);
		model.createParameters();
		int threadNum = model.threadNum();
		System.out.println("Num Features: " + model.featureSize());
		System.out.println("Num Documents: " + nums[threadNum]);
		System.out.println("Num Threads:   " + threadNum);
		
		final int maxIter = model.options.maxIter();
		EMThread.totalTrainInst = nums[threadNum];
		
		String traintmp = model.options.getTrainTmp();
		int nsize = model.featureSize();
		int gsize = model.givenSize();
		
		PrintWriter logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logfile)));
		
		for(int itr = 0; itr < maxIter; ++itr){
			System.out.print("iter=" + itr + "[   ");
			System.out.flush();
			long clock = System.currentTimeMillis() / 1000;
			EMThread.currentNum = 0;
			EMThread[] threads = new EMThread[threadNum];
			String[] tokens = traintmp.split("\\.");
			for(int i = 0; i < threadNum; i++){
				String tmpfile = tokens[0] + i + "." + tokens[1];
				threads[i] = new EMThread(nums[i], manager, tmpfile, model);
			}
			
			for(int i = 0; i < threadNum; ++i){
				threads[i].start();
			}
			
			for(int i = 0; i < threadNum; ++i){
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.print("\b\b\b");
			
			for(int i = 1; i < threadNum; ++i){
				for(int j = 0; j < nsize; ++j){
					threads[0].featC[j] += threads[i].featC[j];
				}
				
				for(int j = 0; j < gsize; ++j){
					threads[0].givenC[j] += threads[i].givenC[j];
				}
			}
			
			for(int j = 0; j < nsize; ++j){
				int gid = model.getGidFromIndex(j);
				double val = threads[0].featC[j] / threads[0].givenC[gid];
				model.update(j, val);
			}
			System.out.println(EMThread.totalTrainInst + "|time=" + (System.currentTimeMillis() /1000 - clock) + "s." + "]");
			model.displayAlphabet(new PrintStream(new File("feat" + (itr + 1) + ".txt")));
			
			logWriter.println("Iter: " + (itr + 1));
			evaluateCurrentAcc(manager, decoder, model, devfile, logWriter);
			logWriter.println("--------------------------------------------------------------------------");
			logWriter.flush();
		}
		
		logWriter.close();
	}
	
	protected void evaluateCurrentAcc(CorefManager manager, Decoder decoder, CorefModel model, 
			String devfile, PrintWriter logWriter) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		DocumentReader docReader = DocumentReader.createDocumentReader(model.options.getDocReader());
		DocumentWriter docWriter = DocumentWriter.createDocumentWriter(CoNLLDocumentWriter.class.getName());
		
		docReader.startReading(devfile);
		String tempfile = "tmp/result.tmp";
		docWriter.startWriting(tempfile);
		
		Document doc = docReader.getNextDocument(false);
		while(doc != null){
			List<List<Mention>> mentionList = decoder.decode(doc, manager, model);
			doc.assignCorefClustersToDocument(mentionList, model.options.postProcessing());
			docWriter.writeDocument(doc, true);
			
			doc = docReader.getNextDocument(false);
		}
		docReader.close();
		docWriter.close();
		
		String scorer = model.options.getCoNLLScorer();
		String summary = getConllEvalSummary(scorer, "tmp/conll2012.eng.dev.auto.conll", "tmp/result.tmp");
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
	
	public double[] featC = null;
	public double[] givenC = null;
	
	public EMThread(int numTrainInst, CorefManager manager, String tmpfile, CorefModel model){
		this.numTrainInst = numTrainInst;
		this.manager = manager;
		this.tmpfile = tmpfile;
		this.model = model;
		featC = new double[model.featureSize()];
		givenC = new double[model.givenSize()];
	}
	
	private void EMupdate(ObjectReader in, CorefModel model, CorefManager manager) throws IOException, ClassNotFoundException{
		for(int i = 0; i < numTrainInst; ++i){
			if(i > 0 && i % 500 == 0){
				currentNum += 500;
				int percent = currentNum * 100 / totalTrainInst;
				System.out.print("\b\b\b" + (percent < 10 ? " " : "") + percent + "%");
				System.out.flush();
			}
			int sizeOfMention = in.readInt();
			int last = in.readInt();
			if(last != -4){
				throw new IOException("last number is not equal to -4");
			}
			
			for(int j = 0; j < sizeOfMention; ++j){
				int[][] keys = (int[][]) in.readObject();
				int[][] gids = (int[][]) in.readObject();
				double[] probs = new double[keys.length];
				double sumProb = 0.0;
				for(int k = 0; k < keys.length; ++k){
					if(keys[k] == null){
						probs[k] = 0.0;
						continue;
					}
					probs[k] = model.getScore(new FeatureVector(keys[k], gids[k]));
					sumProb += probs[k];
				}
				for(int k = 0; k < keys.length; ++k){
					if(keys[k] == null){
						continue;
					}
					double val = probs[k] / sumProb;
					for(int l = 0; l < keys[k].length; ++l){
						featC[keys[k][l]] += val;
						givenC[gids[k][l]] += val;
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
