package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class Smoother {

	public static Smoother createSmoother(String smootherClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (Smoother) Class.forName(Smoother.class.getPackage().getName() + "." + smootherClassName).newInstance();
	}
	
	public Smoother() {
		
	}
	
	public void smoothMentionParams(double[] mFeatC, double[] mGivenC, double beta, CorefModel model) {
		int nsizeOfM = model.mentionFeatureSize();
		// update mention parameters
		for(int j = 0; j < nsizeOfM; ++j) {
			int gid = model.getMentionGidFromIndex(j);
			double val = mFeatC[j] - mGivenC[gid];
			model.updateMentionParams(j, val);
		}
	}
	
	public void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double[] eUnigramC, double eUnigramN, double alpha, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.eventV();
		double logD = Math.log(d);
		
		//update uni_val
		model.updateEventNil(-logD);
		
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
