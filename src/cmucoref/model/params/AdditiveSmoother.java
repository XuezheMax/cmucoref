package cmucoref.model.params;

import cmucoref.model.CorefModel;
import cmucoref.util.Util;

public class AdditiveSmoother extends Smoother {
	public AdditiveSmoother() {
		super();
	}
	
	@Override
	public void smoothMentionParams(double[] mFeatC, double[] mGivenC, double beta, CorefModel model) {
	    int nsizeOfM = model.mentionFeatureSize();
	    int gsizeOfM = model.givenSizeofMention();
	    int newClusGid = model.getMentionGid("mode=ATTR_MAT, NEWCLUSMENTION");
	    int d = model.getSizeOfMentionFeatFromGid(newClusGid) * 20 * 5;
	    double logD = Math.log(d);
        double logBeta = Math.log(beta);
        
        // update mention parameters
        for(int j = 0; j < nsizeOfM; ++j) {
            int gid = model.getMentionGidFromIndex(j);
            double val = (gid == newClusGid ? mFeatC[j] - mGivenC[gid] : 
                Util.logsumexp(mFeatC[j], logBeta) - Util.logsumexp(mGivenC[gid], logBeta + logD));
            model.updateMentionParams(j, val);
        }
        
        // update nil
        model.updateMentionNil(-logD);
        
        // update mention nils
        for(int j = 0; j < gsizeOfM; ++j) {
            if(j != newClusGid) {
                double val = beta - Util.logsumexp(mGivenC[j], logBeta + logD);
                model.updateMentionNils(j, val);
            }
        }
	}
	
	@Override
	public void smoothEventParams(double[] eFeatC, double[] eGivenC, double[] eGivenCNoNil, double[] eUnigramC, double eUnigramN, double alpha, CorefModel model) {
		int nsizeOfE = model.eventFeatureSize();
		int gsizeOfE = model.givenSizeofEvent();
		
		int d = model.eventV();
		double logD = Math.log(d);
		double logAlpha = Math.log(alpha);
		
		//update nil
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
