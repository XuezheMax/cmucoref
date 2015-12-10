package cmucoref.model.params;

import cmucoref.model.Parameters;

public class UniformInitializer extends ParameterInitializer{

	public UniformInitializer(){}
	
	@Override
	public void initializeMentionParams(Parameters parameters) {
	    int newClusGid = model.getMentionGid("mode=ATTR_MAT, NEWCLUSMENTION");
	    int d = model.getSizeOfMentionFeatFromGid(newClusGid) * 30;
        double logD = Math.log(d);
	    
        // initialize mention nils
		for(int j = 0; j < parameters.sizeOfNil(); ++j) {
		    if(j == newClusGid) {
		        parameters.updateNil(j, Double.NEGATIVE_INFINITY);
		    }
		    else {
		        parameters.updateNil(j, -logD);
		    }
		}
		
		// initialize mention parameters
		for(int j = 0; j < parameters.sizeOfFeat(); ++j) {
		    int gid = model.getMentionGidFromIndex(j);
		    double val = (gid == newClusGid ? -Math.log(model.getSizeOfMentionFeatFromGid(gid)) : -logD);
		    parameters.updateParam(j, val);
		}
		
		for(int j = 0; j < parameters.sizeOfUni(); ++j) {
			parameters.updateUniParam(j, Double.NEGATIVE_INFINITY);
		}
		
//		for(int j = 0; j < parameters.sizeOfFeat(); ++j) {
//			int gid = model.getMentionGidFromIndex(j);
//			parameters.updateParam(j, -Math.log(model.getSizeOfMentionFeatFromGid(gid)));
//		}
		
//		for(int j = 0; j < parameters.length; ++j) {
//			parameters[j] = Math.log(1.0 / model.givenSizeofMention());
//		}
	}

	@Override
	public void initializeEventParams(Parameters parameters) {
		double val = -Math.log(model.eventV());
		for(int j = 0; j < parameters.sizeOfNil(); ++j) {
			parameters.updateNil(j, val);
		}
		
		for(int j = 0; j < parameters.sizeOfUni(); ++j) {
			parameters.updateUniParam(j, val);
		}
		
		for(int j = 0; j < parameters.sizeOfFeat(); ++j) {
			parameters.updateParam(j, val);
		}
		
		parameters.updateUni_Nil(Double.NEGATIVE_INFINITY);
		parameters.updateNil(val);
	}
}
