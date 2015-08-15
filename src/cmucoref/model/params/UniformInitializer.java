package cmucoref.model.params;

import java.util.Arrays;

public class UniformInitializer extends ParameterInitializer{

	public UniformInitializer(){}
	
	@Override
	public void initializeMentionParams(double[] parameters, double[] nils, double[] uni_parameters) {
		Arrays.fill(nils, Double.NEGATIVE_INFINITY);
		Arrays.fill(uni_parameters, Double.NEGATIVE_INFINITY);
		for(int j = 0; j < parameters.length; ++j) {
			int gid = model.getMentionGidFromIndex(j);
			parameters[j] = -Math.log(model.getSizeOfMentionFeatFromGid(gid));
		}
		
//		for(int j = 0; j < parameters.length; ++j) {
//			parameters[j] = Math.log(1.0 / model.givenSizeofMention());
//		}
	}

	@Override
	public void initializeEventParams(double[] parameters, double[] nils, double[] uni_parameters) {
		double val = -Math.log(model.sizeOfEvent() + 2);
		Arrays.fill(parameters, val);
		Arrays.fill(nils, val);
		Arrays.fill(uni_parameters, Double.NEGATIVE_INFINITY);
	}
}
