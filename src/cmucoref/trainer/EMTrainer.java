package cmucoref.trainer;

import java.io.IOException;

import cmucoref.manager.CorefManager;
import cmucoref.model.CorefModel;

public class EMTrainer extends Trainer{

	@Override
	public void train(CorefManager manager, CorefModel model, String trainfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		manager.createDocInstance(trainfile, model.options.getTrainTmp(), model);
		model.createParameters();
		// TODO
	}

}
