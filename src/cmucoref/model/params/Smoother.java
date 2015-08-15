package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class Smoother {

	public static Smoother createSmoother(String smootherClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (Smoother) Class.forName(Smoother.class.getPackage().getName() + "." + smootherClassName).newInstance();
	}
	
	public Smoother() {
		
	}
	
	public final void smooth(double[] mFeatC, double[] mGivenC, double[] mGivenCNoNil, double alpha_m, 
			double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double alpha_e, 
			CorefModel model) {
		smoothMentionParams(mFeatC, mGivenC, mGivenCNoNil, alpha_m, model);
		if(eFeatC != null) {
			smoothEventParams(eFeatC, eGivenC, eGivenCNoNil, alpha_e, model);
		}
	}
	
	protected void smoothMentionParams(double[] mFeatC, double[] mGivenC, double[] mGivenCNoNil, double alpha_m, CorefModel model) {
		int nsizeOfM = model.mentionFeatureSize();
		int gsizeOfM = model.givenSizeofMention();
		// update mention parameters
		for(int j = 0; j < nsizeOfM; ++j) {
			int gid = model.getMentionGidFromIndex(j);
			double val = mFeatC[j] - mGivenC[gid];
			model.updateMentionParams(j, val);
		}
		// update mention nils
		for(int j = 0; j < gsizeOfM; ++j) {
			double val = Util.logsubsexp(mGivenC[j], mGivenCNoNil[j]) - mGivenC[j]
						- Math.log(model.mentionFeatureSize() + 1 - model.getSizeOfMentionFeatFromGid(j));
			model.updateMentionNils(j, val);
		}
		
		model.updateMentionUni_Val(Double.NEGATIVE_INFINITY);
	}
	
	protected void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double alpha_e, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.sizeOfEvent() + 2;
		double logD = Math.log(d);
		
		//update uni_val
		model.updateEventUni_Val(-logD);
		
		//update event parameters
		for(int j = 0; j < nsizeOfE; ++j) {
			int gid = model.getEventGidFromIndex(j);
			double val = eFeatC[j] - eGivenC[gid];
			model.updateEventParams(j, val);
		}
		
		//update event nils
		for(int j = 0; j < gsizeOfE; ++j) {
			double val = Util.logsubsexp(eGivenC[j], eGivenCNoNil[j]) - eGivenC[j]
						- Math.log(d - model.getSizeOfEventFeatFromGid(j));
			model.updateEventNils(j, val);
		}
		
	}
}
