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
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			int mode, Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
		if(mode == 0) {
			//attribute match features
			genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);			
		}
		else if(mode == 1) {
			//string match features
			genStringMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);
		}
		else if(mode == 2) {
			//precise match features
			genPreciseMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, useEvent, mfv, efv);
		}
	}
	
	protected void genPreciseMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.getMentionType());
//		given.append(", DEFINITE=" + antec.definite);
		given.append(", PREC_COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.getMentionType());
		
		//distance of sentences
		feat.append(", " + "DISTOFSENT=-1");
		
		//definiteness features
//		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//precise match features
		feat.append(", " + "PRECMAT=true");
		
		addMentionFeature(feat.toString(), given.toString(), model, mfv);
		
		if(useEvent) {
			genPreciseMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
		}
	}
	
	protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
		if(anaph.isPronominal() || antec.isPronominal()) {
			throw new RuntimeException("string match feature does not apply to pronominals");
		}
		
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.getMentionType());
//		given.append(", DEFINITE=" + antec.definite);
		given.append(", STR_COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.getMentionType());
		
		//distance of sentences
//		int distOfStrMat = anaph.getDistOfStringMatch(antec);
		feat.append(", " + "DISTOFSENT=-1");
		
		//definiteness features
//		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//string match features
		if(anaph.isList() || antec.isList()) {
			feat.append(", " + "HDMAT=true");
			feat.append(", " + "CMPMOD=true");
			feat.append(", " + "WDIND=true");
			feat.append(", " + "RLXMAT=true");
			feat.append(", " + "EXTMAT=true");
		}
		else {
			boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
			feat.append(", " + "HDMAT=" + headMatch);
			
			//Acronym or Demonym
			boolean ADNYM = (Mention.options.useDemonym() && anaph.isDemonym(anaphSent, antec, antecSent, dict))
					|| anaph.acronymMatch(anaphSent, antec, antecSent, dict);
			
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
			
//			if(!compatibleModifier && exactMatch) {
//				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
//				antec.display(antecSent, System.out);
//				anaph.display(anaphSent, System.out);
//			}
		}
		addMentionFeature(feat.toString(), given.toString(), model, mfv);
		
		if(useEvent) {
			genStringMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
		}
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv){
		
		//mention type features
		StringBuilder given = new StringBuilder();
		given.append("ANTECTYPE=" +  antec.getMentionType());
		//definiteness features
//		given.append(", DEFINITE=" + antec.definite);
		given.append(", COREF");
		
		StringBuilder feat = new StringBuilder();
		feat.append("ANAPHTYPE=" + anaph.getMentionType());
		
		//distance features
		int distOfSent = anaph.getDistOfSent(antec);
		feat.append(", " + "DISTOFSENT=" + distOfSent);
		
		//definiteness features
//		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		//string match features (does not apply for pronominals)
		if(!anaph.isPronominal() && !antec.isPronominal()) {
			if(anaph.isList() || antec.isList()) {
				boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
				feat.append(", " + "HDMAT=" + headMatch);
				feat.append(", " + "CMPMOD=false");
				feat.append(", " + "WDIND=false");
			}
			else {
				boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent, dict);
				feat.append(", " + "HDMAT=" + headMatch);
				
				//compatible modifier
				boolean compatibleModifier = antec.compatibleModifier(antecSent, anaph, anaphSent, dict);
				feat.append(", " + "CMPMOD=" + compatibleModifier);
				//wordInclude
				boolean wordIncluded = antec.wordsInclude(antecSent, anaph, anaphSent, dict);
				feat.append(", " + "WDIND=" + wordIncluded);
			}
			
			//number features;
			//feat.append(", " + "NUM=" + anaph.number);
			
			//animacy features
			//feat.append(", " + "ANI=" + anaph.animacy);
			
			//gender features
			//feat.append(", " + "GENDER=" + anaph.gender);
		}
		
		addMentionFeature(feat.toString(), given.toString(), model, mfv);
		
		if(useEvent) {
			genAttrMatchEventFeatures(anaph, anaphSent, antec, antecSent, dict, model, efv);
		}
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, 
			CorefModel model, boolean useEvent, FeatureVector mfv, FeatureVector efv) {
		// anaph starts a new cluster
		//type features
		String given = "NEWCLUSTER";
		StringBuilder feat = new StringBuilder();
		feat.append("TYPE=" + anaph.getMentionType());
		
		//definiteness features
//		feat.append(", " + "DEFINITE=" + anaph.definite);
		
		if(anaph.isPronominal()) {
			int localAttrMatchOfSent = anaph.localAttrMatch == null ? -1 : anaph.getDistOfSent(anaph.localAttrMatch);
			String localAttrMatchType = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.getMentionType().toString();
//			String localAttrMatchDefinite = anaph.localAttrMatch == null ? null : anaph.localAttrMatch.definite.toString();
			
			feat.append(", " + "LOCATTRMATOFSENT=" + localAttrMatchOfSent);
			feat.append(", " + "LOCATTRMATTYPE=" + localAttrMatchType); 
//			feat.append(", " + "LOCATTRMATDEFINITE=" + localAttrMatchDefinite);
		}
		
		addMentionFeature(feat.toString(), given, model, mfv);
		
		if(useEvent) {
			genEventNewClusterFeatures(anaph, anaphSent, model, efv);
		}
	}
	
	protected void genPreciseMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, FeatureVector efv) {
		// get event feature of antec
		addEventFeature(null, null, model, efv);
	}
	
	protected void genStringMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, FeatureVector efv) {
		// get event feature of antec
		String antecFeat = null;
		if(antec.getMainEvent() != null) {
			antecFeat = antec.getMainEvent().toFeature();
		}
		// get event feature of anaph
		String anaphFeat = null;
		if(anaph.getMainEvent() != null) {
			anaphFeat = anaph.getMainEvent().toFeature();
		}
		addEventFeature(anaphFeat, antecFeat, model, efv);
	}
	
	protected void genAttrMatchEventFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, 
			Dictionaries dict, CorefModel model, FeatureVector efv) {
		// get event feature of antec
		String antecFeat = null;
		if(antec.getMainEvent() != null) {			
			antecFeat = antec.getMainEvent().toFeature();
		}
		// get event feature of anaph
		String anaphFeat = null;
		if(anaph.getMainEvent() != null) {
			anaphFeat = anaph.getMainEvent().toFeature();
		}
		addEventFeature(anaphFeat, antecFeat, model, efv);
	}
	
	public void genEventNewClusterFeatures(Mention anaph, Sentence anaphSent, 
			CorefModel model, FeatureVector efv) {
		String given = "NEWCLUSEVENT";
		String anaphFeat = null;
		if(anaph.getMainEvent() != null) {
			anaphFeat = anaph.getMainEvent().toFeature();
		}
		addEventFeature(anaphFeat, given, model, efv);
	}
	
	protected final void addMentionFeature(String feat, String given, CorefModel model, FeatureVector mfv){
		Pair<Integer, Integer> ids = model.getMentionFeatureIndex(feat, given);
		if(mfv != null){
			mfv.addFeature(ids.first, ids.second);
		}
	}
	
	protected final void addEventFeature(String feat, String given, CorefModel model, FeatureVector efv){
		Pair<Integer, Integer> ids = model.getEventFeatureIndex(feat, given);
		if(efv != null){
			efv.addFeature(ids.first, ids.second);
		}
	}
}
