package cmucoref.model.params;

import cmucoref.model.CorefModel;

public abstract class ParameterInitializer {
	protected CorefModel model = null;
	
	public ParameterInitializer(){}
	
	public void setModel(CorefModel model){
		this.model = model;
	}
	
	public abstract void initializeParams(double[] parameters);
}
