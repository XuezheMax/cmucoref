package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;

import edu.stanford.nlp.dcoref.Dictionaries.MentionType;

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		
		// anaph is coreferent with previous antec 
		//mention type features
		addFeature("ANAPHTYPE=" + anaph.mentionType, "ANTECTYPE=" + antec.mentionType, model, fv);
		
		String given = "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
		
		//distance of sentences features (only apply for prononimals)
		if(anaph.mentionType == MentionType.PRONOMINAL || antec.mentionType == MentionType.PRONOMINAL){
			int distOfSent = anaph.getDistOfSent(antec);
			addFeature("DISTOFSENT=" + distOfSent, given, model, fv);
			given = "DISTOFSENT=" + distOfSent + ", " + given;
		}
		
		//attribute match features
		boolean attrMat = genAttrMatchFeatures(anaph, antec, given, model, fv);
		
		//head features
		if((anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER)
				&& (antec.mentionType == MentionType.NOMINAL || antec.mentionType == MentionType.PROPER)){
			genStringMatchFeatures(anaph, anaphSent, antec, antecSent, attrMat, model, fv);
		}
	}
	
	public void genAppositiveFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		// anaph is appositive to antec 
		//mention type features
		addFeature("ANAPHTYPE=" + anaph.mentionType, "ANTECTYPE=" + antec.mentionType, model, fv);
				
		String given = "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
		//distance of sentences features (only apply for prononimals)
		if(anaph.mentionType == MentionType.PRONOMINAL || antec.mentionType == MentionType.PRONOMINAL){
			int distOfSent = anaph.getDistOfSent(antec);
			addFeature("DISTOFSENT=" + distOfSent, given, model, fv);
			given = "DISTOFSENT=" + distOfSent + ", " + given;
		}
		
		//attribute match features
		boolean attrMat = genAttrMatchFeatures(anaph, antec, given, model, fv);
		
		if((anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER)
				&& (antec.mentionType == MentionType.NOMINAL || antec.mentionType == MentionType.PROPER)){
			//given = "ATTRMAT=" + attrMat;// + ", " + "DISTOFSENT=" + distOfSent;
			given = "ATTRMAT=" + attrMat + ", " + "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
			addFeature("SPMAT=true, HDMAT=true", given, model, fv);
		}
	}
	
	public void genPredicativeNominativeFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		// anaph is predicative nominative to antec 
		//mention type features
		addFeature("ANAPHTYPE=" + anaph.mentionType, "ANTECTYPE=" + antec.mentionType, model, fv);
						
		String given = "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
		//distance of sentences features (only apply for prononimals)
		if(anaph.mentionType == MentionType.PRONOMINAL || antec.mentionType == MentionType.PRONOMINAL){
			int distOfSent = anaph.getDistOfSent(antec);
			addFeature("DISTOFSENT=" + distOfSent, given, model, fv);
			given = "DISTOFSENT=" + distOfSent + ", " + given;
		}
		
		//attribute match features
		boolean attrMat = genAttrMatchFeatures(anaph, antec, given, model, fv);
		
		if((anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER)
				&& (antec.mentionType == MentionType.NOMINAL || antec.mentionType == MentionType.PROPER)){
			//given = "ATTRMAT=" + attrMat;// + ", " + "DISTOFSENT=" + distOfSent;
			given = "ATTRMAT=" + attrMat + ", " + "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
			addFeature("SPMAT=true, HDMAT=true", given, model, fv);
		}
	}
	
	protected boolean genAttrMatchFeatures(Mention anaph, Mention antec, String given, CorefModel model, FeatureVector fv){
		boolean allMat = true;
		String feat;
		//number match
		boolean numMat = anaph.numberAgree(antec);
		if(!numMat){
			allMat = false;
		}
		
		//gender match
		boolean genMat = anaph.genderAgree(antec);
		if(!genMat){
			allMat = false;
		}
		
		feat = "GENMAT=" + genMat + ", " + "NUMMAT=" + numMat;
		
		//animacy match (does not apply for pronominal mentions)
		if(anaph.mentionType != MentionType.PRONOMINAL && antec.mentionType != MentionType.PRONOMINAL){
			boolean aniMat = anaph.animateAgree(antec);
			if(!aniMat){
				allMat = false;
			}
			feat = feat + ", " + "ANIMAT=" + aniMat;
		}
		
		//person match (only apply for pronominal mentions)
		if(anaph.mentionType == MentionType.PRONOMINAL && antec.mentionType == MentionType.PRONOMINAL){
			boolean perMat = anaph.personAgree(antec);
			if(!perMat){
				allMat = false;
			}
			feat = feat + ", " + "PERMAT=" + perMat;
		}
		
		//ner match given gender and animacy match (only apply for proper mentions)
		if(anaph.mentionType == MentionType.PROPER && antec.mentionType == MentionType.PROPER){
			boolean nerMat = anaph.NERAgree(antec);
			if(!nerMat){
				allMat = false;
			}
			feat = feat + ", " + "NERMAT=" + nerMat;
		}
		addFeature(feat, given, model, fv);
		
		return allMat;
	}
	
	protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, boolean attrMat, CorefModel model, FeatureVector fv){
		boolean wordIncluded = antec.wordsInclude(antecSent, anaph, anaphSent);
		boolean headMatch = antec.headMatch(antecSent, anaph, anaphSent);
		boolean relaxMatch = anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
		
		String hdMat = Boolean.toString(headMatch);
		String spMat = Boolean.toString(wordIncluded || relaxMatch);
		
		//add head match and span match feature
		//String given = "ATTRMAT=" + Boolean.toString(attrMat);
		String given = "ATTRMAT=" + attrMat + ", " + "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
		addFeature("SPMAT=" + spMat + ", " + "HDMAT=" + hdMat, given, model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		String feat = "TYPE=" + anaph.mentionType;
		//attrMat within 5 sentences (only apply for pronominals)
		if(anaph.mentionType == MentionType.PRONOMINAL){
			feat = feat + ", " + "LOCATTRMAT=" + anaph.localAttrMatch;
		}
		else{
			feat = feat + ", " + "HDMATPOS=" + anaph.closestSpanMatchPos + ", " + "SPMAT=" + anaph.closestSpanMatch;
		}
		addFeature(feat, "NEWCLUSTER", model, fv);
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		Pair<Integer, Integer> ids = model.getFeatureIndex(feat, given);
		if(fv != null && ids != null){
			fv.addFeature(ids.first, ids.second);
		}
	}
}
