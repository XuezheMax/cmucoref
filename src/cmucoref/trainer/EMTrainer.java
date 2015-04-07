package cmucoref.trainer;

import java.io.IOException;

import cmucoref.exception.CreatingInstanceException;
import cmucoref.manager.CorefManager;
import cmucoref.model.CorefModel;

public class EMTrainer extends Trainer{

	@Override
	public void train(CorefManager manager, CorefModel model, String trainfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException {
		int numTrainInst = manager.createDocInstance(trainfile, model.options.getTrainTmp(), model);
		model.createParameters();
		System.out.println("Num Features: " + model.featureSize());
		System.out.println("Num Sentences: " + numTrainInst);
		System.out.println("Num Threads:   " + model.threadNum());
		// TODO
	}

}
