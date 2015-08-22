package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class InterpolationSmootherLinearD extends Smoother {

	public InterpolationSmootherLinearD() {
		super();
	}
	
	@Override
	protected void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double[] eUnigramC, double eUnigramN, double alpha_e, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.eventV();
		double logA = Math.log(alpha_e);
		double logOneminusA = Math.log(1- alpha_e);
		
		//calculate total unigram count without nil
		double N = Double.NEGATIVE_INFINITY;
		int x = 0;
		for(int j = 0; j < gsizeOfE; ++j) {
			if(eUnigramC[j] == Double.NEGATIVE_INFINITY) {
				x++;
			}
			N = Util.logsumexp(N, eUnigramC[j]);
		}
		
		//update uni_nil
		double uni_nil = logOneminusA + Util.logsubsexp(eUnigramN, N) - eUnigramN - Math.log(d - gsizeOfE + x);
		model.updateEventUni_nil(uni_nil);
		
		//update uni_parameters
		for(int j = 0; j < gsizeOfE; ++j) {
			double val = eUnigramC[j] == Double.NEGATIVE_INFINITY ? uni_nil : logOneminusA + eUnigramC[j] - eUnigramN;
			model.updateEventUniParams(j, val);
		}
		
		//update event parameters
		for(int j = 0; j < nsizeOfE; ++j) {
			int gid = model.getEventGidFromIndex(j);
			int eid = model.getEventIdFromIndex(j);
			double val_bigram = logA + eFeatC[j] - eGivenC[gid];
			double val_unigram = model.eventUniParamAt(eid);
			double val = Util.logsumexp(val_bigram, val_unigram);
			model.updateEventParams(j, val);
		}
		
		double logD = Math.log(d);
		double nil = logA - logD;
		model.updateEventNil(nil);
		
		//update event nils
		for(int j = 0; j < gsizeOfE; ++j) {
			if(eGivenC[j] == Double.NEGATIVE_INFINITY) {
				if(model.getSizeOfEventFeatFromGid(j) > 0) {
					throw new RuntimeException("size should be zero: " + model.getSizeOfEventFeatFromGid(j));
				}
				model.updateEventNils(j, nil);
			}
			else {
				double val = logA + Util.logsubsexp(eGivenC[j], eGivenCNoNil[j]) - eGivenC[j]
						- Math.log(d - model.getSizeOfEventFeatFromGid(j));
				model.updateEventNils(j, val);
			}
		}
	}
}
