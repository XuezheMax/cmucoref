package cmucoref.mention.featgen;

import cmucoref.mention.Mention;

public abstract class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public abstract void genMentionFeatures(Mention anaph, Mention antec);
}
