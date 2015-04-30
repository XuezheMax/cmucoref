package cmucoref.trainer;

import java.io.IOException;

import cmucoref.decoder.Decoder;
import cmucoref.exception.CreatingInstanceException;
import cmucoref.io.ObjectWriter;
import cmucoref.manager.CorefManager;
import cmucoref.model.CorefModel;

public abstract class Trainer {
	public Trainer(){}
	
	public abstract void train(CorefManager manager, Decoder decoder, CorefModel model, String trainfile, String devfile, String logfile, String modelfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, CreatingInstanceException;
	
	protected void saveModel(CorefModel model, String file) throws IOException{
		ObjectWriter out = new ObjectWriter(file);
		out.writeObject(model);
		out.close();
	}
}
