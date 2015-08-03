package cmucoref.model.params;

public class UniformInitializer extends ParameterInitializer{

	public UniformInitializer(){}
	
	@Override
	public void initializeMentionParams(double[] parameters) {
		double[] c = new double[model.givenSizeofMention()];
		for(int j = 0; j < parameters.length; ++j) {
			int gid = model.getMentionGidFromIndex(j);
			c[gid] += 1.0;
		}
		
		for(int j = 0; j < parameters.length; ++j) {
			int gid = model.getMentionGidFromIndex(j);
			parameters[j] = Math.log(1.0 / c[gid]);
		}
		
//		for(int j = 0; j < parameters.length; ++j) {
//			parameters[j] = Math.log(1.0 / model.givenSizeofMention());
//		}
	}

	@Override
	public void initializeEventParams(double[] parameters) {
		for(int j = 0; j < parameters.length; ++j) {
			parameters[j] = Math.log(1.0 / model.givenSizeofEvent());
		}
	}
}
