package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;
import edu.stanford.nlp.dcoref.Dictionaries;

/**
 * 
 * @author Max
 *
 */

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		
		//attribute match features
		genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		
		//mention type features
		String given = "ANTECTYPE=" +  antec.mentionType;
		given = given + ", DEFINITE=" + antec.definite;
		given = given + ", COREF";
		
		String feat = "ANAPHTYPE=" + anaph.mentionType;
		boolean preciseMatch = anaph.preciseMatch(anaphSent, antec, antecSent, dict);
		
		//distance of sentences
		if(!preciseMatch){
			int distOfSent = anaph.getDistOfSent(antec);
			feat = feat + ", " + "DISTOFSENT=" + distOfSent;
		}
		else{
			feat = feat + ", " + "DISTOFSENT=-1";
		}
		
		//definiteness features
		feat = feat + ", " + "DEFINITE=" + anaph.definite;
		
		//precise match features
		feat = feat + ", " + "PRECMAT=" + preciseMatch;
		
		//string match features (only apply for nominative or proper mentions)
		if((anaph.isNominative() || anaph.isProper()) && (antec.isNominative() || antec.isProper())) {
			boolean headMatch = preciseMatch ? true : antec.headMatch(antecSent, anaph, anaphSent);
			feat = feat + ", " + "HDMAT=" + headMatch;
			boolean specialRelation = preciseMatch && !anaph.spanMatch(anaphSent, antec, antecSent, dict);
			
			//Acronym or Demonym
			boolean ADNYM = specialRelation || anaph.isDemonym(anaphSent, antec, antecSent, dict);
			if(!ADNYM && anaph.isProper() && antec.isProper()) {
				ADNYM = anaph.acronymMatch(anaphSent, antec, antecSent);
			}
			
			//wordInclude
			boolean wordIncluded = ADNYM || antec.wordsInclude(antecSent, anaph, anaphSent, dict);
			feat = feat + ", " + "WDIND=" + wordIncluded;
			//exact match
			boolean exactMatch = ADNYM || anaph.exactSpanMatch(anaphSent, antec, antecSent);
			//relaxed match
			boolean relaxedMatch = exactMatch || anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
			
			feat = feat + ", " + "RLXMAT=" + relaxedMatch;
			feat = feat + ", " + "EXTMAT=" + exactMatch;
		}
		
		
		addFeature(feat, given, model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		//type features
		String feat = "TYPE=" + anaph.mentionType;
		String given = "NEWCLUSTER";
		
		
//		feat = feat  + ", " + "PRECMATPOS=" + anaph.closestPreciseMatchPos + ", " 
//				+ "PRECMATTYPE=" + anaph.closestPreciseMatchType;
		
		//definiteness features
		feat = feat + ", " + "DEFINITE=" + anaph.definite;
		
		if(anaph.isPronominal()) {
			int localAttrMatchOfSent = anaph.localAttrMatch == null ? -1 : anaph.getDistOfSent(anaph.localAttrMatch);
			String localAttrMatchType = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.mentionType.toString();
			String localAttrMatchDefinite = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.definite.toString();
			
			feat = feat + ", " + "LOCATTRMATOFSENT=" + localAttrMatchOfSent;
			feat = feat + ", " + "LOCATTRMATTYPE=" + localAttrMatchType; 
			feat = feat + ", " + "LOCATTRMATDEFINITE=" + localAttrMatchDefinite;
		}
		
		addFeature(feat, given, model, fv);
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		Pair<Integer, Integer> ids = model.getFeatureIndex(feat, given);
		if(fv != null && ids != null){
			fv.addFeature(ids.first, ids.second, 1.0);
		}
	}
}
