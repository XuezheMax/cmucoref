package cmucoref.trainer;

import java.io.IOException;

import cmucoref.io.ObjectWriter;
import cmucoref.manager.CorefManager;
import cmucoref.model.CorefModel;

public abstract class Trainer {
	public Trainer(){}
	
	public abstract void train(CorefManager manager, CorefModel model, String trainfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException;
	
	protected void saveModel(CorefModel model, String file) throws IOException{
		ObjectWriter out = new ObjectWriter(file);
		out.writeObject(model);
		out.close();
	}
}
