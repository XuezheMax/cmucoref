package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;

/**
 * 
 * @author Max
 *
 */

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		
		// anaph is coreferent with previous antec 
		//attribute match features
		genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		
		//if anaph is pronominal and precise, change anaph to proper.
		
		//mention type features
		String feat = "ANAPHTYPE=" + ((anaph.isPronominal() && anaph.preciseMatch) ? MentionType.PROPER : anaph.mentionType);
		String given = "ANTECTYPE=" +  antec.mentionType + ", COREF";
		
		//distance of sentences
		if(!anaph.preciseMatch){
			int distOfSent = anaph.getDistOfSent(antec);
			feat = feat + ", " + "DISTOFSENT=" + distOfSent;
		}
		else{
			feat = feat + ", " + "DISTOFSENT=-1";
		}
		
//		//number match
//		boolean numMat = anaph.preciseMatch ? true : anaph.numberAgree(antec);
//				
//		//gender match
//		boolean genMat = anaph.preciseMatch ? true : anaph.genderAgree(antec);
//		
//		feat = feat + ", " + "GENMAT=" + genMat + ", " + "NUMMAT=" + numMat;
//		
//		//animacy match
//		boolean aniMat = anaph.preciseMatch ? true : anaph.animateAgree(antec);
//		feat = feat + ", " + "ANIMAT=" + aniMat;
//		
//		//person match (only apply for pronominal mentions)
//		if((anaph.isPronominal() && !anaph.preciseMatch) && antec.isPronominal()){
//			boolean perMat = anaph.preciseMatch ? true : anaph.personAgree(antec);
//			feat = feat + ", " + "PERMAT=" + perMat;
//		}
//		
//		//ner match
//		boolean nerMat = anaph.preciseMatch ? true : anaph.NERAgree(antec, dict);
//		feat = feat + ", " + "NERMAT=" + nerMat;
		
		boolean preciseMatch = anaph.preciseMatch;
		
		//string match features (only apply for nominative or proper mentions)
		if((anaph.isNominative() || anaph.isProper() || (anaph.isPronominal() && anaph.preciseMatch)) 
				&& (antec.isNominative() || antec.isProper())) {
			boolean headMatch = preciseMatch ? true : antec.headMatch(antecSent, anaph, anaphSent);
			feat = feat + ", " + "HDMAT=" + headMatch;
		}
		
		//precise match features
		feat = feat + ", " + "PRECMAT=" + preciseMatch;
		
		addFeature(feat, given, model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		//type features
		MentionType anaphType = (anaph.isPronominal() && anaph.preciseMatch) ? MentionType.PROPER : anaph.mentionType;
		String feat = "TYPE=" + anaphType;
		String given = "NEWCLUSTER";
		
		if(anaphType == MentionType.PRONOMINAL) {
			feat = feat + ", " + "LOCATTRMATOFSENT=" + anaph.localAttrMatchOfSent; 
//			+ ", " + "LOCATTRMATTYPE=" + anaph.localAttrMatchType;
		}
		else {
			feat = feat  + ", " + "PRECMATPOS=" + anaph.closestPreciseMatchPos + ", " 
						+ "PRECMATTYPE=" + anaph.closestPreciseMatchType;
		}
		
		addFeature(feat, given, model, fv);
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		Pair<Integer, Integer> ids = model.getFeatureIndex(feat, given);
		if(fv != null && ids != null){
			fv.addFeature(ids.first, ids.second);
		}
	}
}
