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
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, int mode, Dictionaries dict, CorefModel model, FeatureVector fv){
		if(mode == 0) {
			//attribute match features
			genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);			
		}
		else if(mode == 1) {
			//string match features
			genStringMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);
		}
		else if(mode == 2) {
			//precise match features
			genPreciseMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);
		}
	}
	
	protected void genPreciseMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv) {
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.mentionType);
		given.append(", DEFINITE=" + antec.definite + ", COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.mentionType);
		
		//distance of sentences
		feat.append(", " + "DISTOFSENT=-1");
		
		//definiteness features
		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//precise match features
		feat.append(", " + "PRECMAT=true");
		
		addFeature(feat.toString(), given.toString(), model, fv);
	}
	
	protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv) {
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.mentionType);
		given.append(", DEFINITE=" + antec.definite);
		given.append(", COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.mentionType);
		
		//distance of sentences
		feat.append(", " + "DISTOFSENT=-1");
		
		//definiteness features
		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//string match features (does not apply for pronominals)
		if(!anaph.isPronominal() && !antec.isPronominal()) {
			boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "HDMAT=" + headMatch);
			
			//Acronym or Demonym
			boolean ADNYM = (Mention.options.useDemonym() && anaph.isDemonym(anaphSent, antec, antecSent, dict))
							|| anaph.acronymMatch(anaphSent, antec, antecSent);
			
			//compatible modifier
			boolean compatibleModifier = ADNYM || antec.compatibleModifier(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "CMPMOD=" + compatibleModifier);
			//wordInclude
			boolean wordIncluded = ADNYM || antec.wordsInclude(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "WDIND=" + wordIncluded);
			//exact match
			boolean exactMatch = ADNYM || anaph.exactSpanMatch(anaphSent, antec, antecSent);
			//relaxed match
			boolean relaxedMatch = exactMatch || anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
			
			feat.append(", " + "RLXMAT=" + relaxedMatch);
			feat.append(", " + "EXTMAT=" + exactMatch);
			
//			if(anaph.isDemonym(anaphSent, antec, antecSent, dict) && !anaph.exactSpanMatch(anaphSent, antec, antecSent)) {
//				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
//				antec.display(antecSent, System.out);
//				anaph.display(anaphSent, System.out);
//			}
		}
		addFeature(feat.toString(), given.toString(), model, fv);
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.mentionType);
		given.append(", DEFINITE=" + antec.definite);
		given.append(", COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.mentionType);
		
		//definiteness features
		int distOfSent = anaph.getDistOfSent(antec);
		feat.append(", " + "DISTOFSENT=" + distOfSent);
		
		//definiteness features
		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//string match features (does not apply for pronominals)
		if(!anaph.isPronominal() && !antec.isPronominal()) {
			boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "HDMAT=" + headMatch);
			
			//Acronym or Demonym
			boolean ADNYM = (Mention.options.useDemonym() && anaph.isDemonym(anaphSent, antec, antecSent, dict))
					|| anaph.acronymMatch(anaphSent, antec, antecSent);
			
			//compatible modifier
			boolean compatibleModifier = ADNYM || antec.compatibleModifier(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "CMPMOD=" + compatibleModifier);
			//wordInclude
			boolean wordIncluded = ADNYM || antec.wordsInclude(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "WDIND=" + wordIncluded);
			//exact match
			boolean exactMatch = ADNYM || anaph.exactSpanMatch(anaphSent, antec, antecSent);
			//relaxed match
			boolean relaxedMatch = exactMatch || anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
			
			feat.append(", " + "RLXMAT=" + relaxedMatch);
			feat.append(", " + "EXTMAT=" + exactMatch);
		}
		
		addFeature(feat.toString(), given.toString(), model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		//type features
		String given = "NEWCLUSTER";
		StringBuilder feat = new StringBuilder();
		feat.append("TYPE=" + anaph.mentionType);
		
		
//		feat = feat  + ", " + "PRECMATPOS=" + anaph.closestPreciseMatchPos + ", " 
//				+ "PRECMATTYPE=" + anaph.closestPreciseMatchType;
		
		//definiteness features
		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		if(anaph.isPronominal()) {
			int localAttrMatchOfSent = anaph.localAttrMatch == null ? -1 : anaph.getDistOfSent(anaph.localAttrMatch);
			String localAttrMatchType = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.mentionType.toString();
			String localAttrMatchDefinite = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.definite.toString();
			
			feat.append(", " + "LOCATTRMATOFSENT=" + localAttrMatchOfSent);
			feat.append(", " + "LOCATTRMATTYPE=" + localAttrMatchType); 
			feat.append(", " + "LOCATTRMATDEFINITE=" + localAttrMatchDefinite);
		}
		
		addFeature(feat.toString(), given, model, fv);
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		Pair<Integer, Integer> ids = model.getFeatureIndex(feat, given);
		if(fv != null && ids != null){
			fv.addFeature(ids.first, ids.second, 1.0);
		}
	}
}
