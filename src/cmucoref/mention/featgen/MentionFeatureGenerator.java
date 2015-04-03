package cmucoref.mention.featgen;

import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genMentionFeatures(Mention anaph, Mention antec, CorefModel model, FeatureVector fv){
		// TODO
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		int num = model.getFeatureIndex(feat, given);
		if(num >= 0){
			fv.add(num, 1.0);
		}
	}
}
