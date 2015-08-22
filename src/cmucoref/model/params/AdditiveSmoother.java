package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class AdditiveSmoother extends Smoother {
	public AdditiveSmoother() {
		super();
	}
	
	@Override
	protected void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double[] eUnigramC, double eUnigramN, double alpha_e, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.eventV();
		double logD = Math.log(d);
		double logAlpha = Math.log(alpha_e);
		
		//update uni_val
		model.updateEventNil(-logD);
		
		//update event parameters
		for(int j = 0; j < nsizeOfE; ++j) {
			int gid = model.getEventGidFromIndex(j);
			double val = Util.logsumexp(eFeatC[j], logAlpha) - Util.logsumexp(eGivenC[gid], logAlpha + logD);
			model.updateEventParams(j, val);
		}
		
		//update event nils
		for(int j = 0; j < gsizeOfE; ++j) {
			double val = Util.logsubsexp(eGivenC[j], eGivenCNoNil[j]) - eGivenC[j]
						- Math.log(d - model.getSizeOfEventFeatFromGid(j));
			val = Util.logsumexp(eGivenC[j] + val, logAlpha) - Util.logsumexp(eGivenC[j], logAlpha + logD);
			model.updateEventNils(j, val);
		}
		
	}
}
