package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.model.Parameters;

public abstract class ParameterInitializer {
	protected CorefModel model = null;
	
	public ParameterInitializer(){}
	
	public void setModel(CorefModel model){
		this.model = model;
	}
	
	public abstract void initializeMentionParams(Parameters parameters);
	
	public abstract void initializeEventParams(Parameters parameters);
}
