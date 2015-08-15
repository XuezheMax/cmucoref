package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class InterpolationSmoother extends Smoother {
	public InterpolationSmoother() {
		super();
	}
	
	@Override
	protected void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double alpha_e, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.sizeOfEvent() + 2;
		double logD = Math.log(d);
		double logA = Math.log(alpha_e);
		double logOneminusA = Math.log(1- alpha_e);
		
		double beta = 0.99;
		double logB = Math.log(beta);
		double logOneminusB = Math.log(1 - beta);
		
		//update uni_val
		double uni_val = logA - logD;
		model.updateEventUni_Val(uni_val);
		
		//calc total count
		double N = Double.NEGATIVE_INFINITY;
		for(int j = 0; j < gsizeOfE; ++j) {
			N = Util.logsumexp(N, eGivenC[j]);
		}
		
		//update uni_nil
		double uni_nil = logOneminusA + logOneminusB - logD;
		model.updateEventUni_nil(uni_nil);
		
		//update uni_parameters
		for(int j = 0; j < gsizeOfE; ++j) {
			double val = Util.logsumexp(logOneminusA + logB + eGivenC[j] - N, uni_nil);
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
		
		//update event nils
		for(int j = 0; j < gsizeOfE; ++j) {
			double val;
			if(eGivenC[j] == Double.NEGATIVE_INFINITY) {
				if(model.getSizeOfEventFeatFromGid(j) > 0) {
					throw new RuntimeException("size should be zero: " + model.getSizeOfEventFeatFromGid(j));
				}
				val = uni_val;
			}
			else {
				val = logA + Util.logsubsexp(eGivenC[j], eGivenCNoNil[j]) - eGivenC[j]
						- Math.log(d - model.getSizeOfEventFeatFromGid(j));
			}
			model.updateEventNils(j, val);
		}
	}
}
