package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;

import edu.stanford.nlp.dcoref.Dictionaries.MentionType;

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public boolean genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		
		// anaph is coreferent with previous antec 
		//basic type and distance features
		addFeature("ANAPHTYPE=" + anaph.mentionType, "ANTECTYPE=" + antec.mentionType, model, fv);
		int distOfSent = anaph.getDistOfSent(antec);
		addFeature("DISTOFSENT=" + distOfSent, "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType, model, fv);
		
		//attribute match features
		String given = "DISTOFSENT=" + distOfSent + ", " + "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
		boolean attrMat = genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, given, model, fv);
		
		//head features
		if((anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER)
				&& (antec.mentionType == MentionType.NOMINAL || antec.mentionType == MentionType.PROPER)){
			
			//given = "ATTRMAT=" + Boolean.toString(attrMat) + ", " + "ANAPHTYPE=" + anaph.mentionType + ", " + "ANTECTYPE=" + antec.mentionType;
			given = "ATTRMAT=" + Boolean.toString(attrMat);// + ", " + "DISTOFSENT=" + distOfSent;
			//given = "ATTRMAT=" + Boolean.toString(attrMat) + ", " + "ANAPHTYPE=" + anaph.mentionType;
			genStringMatchFeatures(anaph, anaphSent, antec, antecSent, given, model, fv);
		}
		
		return attrMat;
	}
	
	protected boolean genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, String given, CorefModel model, FeatureVector fv){
		boolean allMat = true;
		//number match
		boolean numMat = anaph.numberAgree(antec);
		addFeature("NUMMAT=" + numMat, "NUMMAT(" + given + ")", model, fv);
		if(!numMat){
			allMat = false;
		}
		
		//gender match
		boolean genMat = anaph.genderAgree(antec);
		addFeature("GENMAT=" + genMat, "GENMAT(" + given + ")", model, fv);
		if(!genMat){
			allMat = false;
		}
		
		//animacy match given gender match (does not apply for pronominal mentions)
		if(anaph.mentionType != MentionType.PRONOMINAL && antec.mentionType != MentionType.PRONOMINAL){
			boolean aniMat = anaph.animateAgree(antec);
			addFeature("ANIMAT=" + aniMat, "GENMAT1=" + genMat + ", " + given, model, fv);
			if(!aniMat){
				allMat = false;
			}
		}
		
		//person match given number and gender match (only apply for pronominal mentions)
		if(anaph.mentionType == MentionType.PRONOMINAL && antec.mentionType == MentionType.PRONOMINAL){
			boolean perMat = anaph.personAgree(antec);
			addFeature("PERMAT=" + perMat, "GENMAT=" + genMat + ", " + "NUMMAT=" + numMat + ", "+ given, model, fv);
			if(!perMat){
				allMat = false;
			}
		}
		
		//ner match given gender and animacy match (only apply for proper mentions)
		if(anaph.mentionType == MentionType.PROPER && antec.mentionType == MentionType.PROPER){
			boolean nerMat = anaph.NERAgree(antec);
			addFeature("NERMAT=" + nerMat, "GENMAT2=" + genMat + ", " + given, model, fv);
			if(!nerMat){
				allMat = false;
			}
		}
		
		return allMat;
	}
	
	protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, String given, CorefModel model, FeatureVector fv){
		
		//exact string match
		boolean exactSpanMatch = anaph.exactSpanMatch(anaphSent, antec, antecSent);
		
		//Acronym
		if(anaph.mentionType == MentionType.PROPER && antec.mentionType == MentionType.PROPER){
			boolean isAcronym = anaph.isAcronymTo(antec, antecSent) || antec.isAcronymTo(anaph, anaphSent);
			exactSpanMatch = exactSpanMatch || isAcronym;
		}
		
		boolean relaxedSpanMatch = true;
		boolean wordIncluded = true;
		boolean headMatch = true;
		if(!exactSpanMatch){
			//relaxed string match
			relaxedSpanMatch = anaph.relaxedSpanMatch(anaphSent, antec, antecSent);
			
			//antec includes all non-stop words of anaph
			wordIncluded = antec.wordsInclude(antecSent, anaph, anaphSent);
//						| anaph.wordsInclude(anaphSent, antec, antecSent);
			
			//head match
			headMatch = anaph.headString.equals(antec.headString);
		}
		
//		if(!relaxedSpanMatch && wordIncluded && headMatch && anaph.mentionType == MentionType.PROPER && antec.mentionType == MentionType.NOMINAL){
//			antec.display(antecSent, System.err);
//			anaph.display(anaphSent, System.err);
//		}
		
		String hdMat = Boolean.toString(headMatch);
		String wdInd = Boolean.toString(wordIncluded);
		String rlMat = Boolean.toString(relaxedSpanMatch);
		String exMat = Boolean.toString(exactSpanMatch);
		
		//add head match feature
		addFeature("WRDIN=" + wdInd + ", " + "HDMAT=" + hdMat, given, model, fv);
		
//		//add higher level match features
//		addFeature("RLMAT=" + rlMat + ", " + "WRDIN=" + wdInd, 
//				"HDMAT=" + hdMat + ", " + given, model, fv);
		
//		//add word included feature
//		addFeature("WRDIN=" + wdInd, "HDMAT=" + hdMat + ", " + given, model, fv);
//		
//		//add relaxed match feature
//		if(headMatch){
//			addFeature("RLMAT=" + rlMat, "WRDIN=" + wdInd + ", " + "HDMAT=" + hdMat + ", " + given, model, fv);
//		}
//		
//		//add exact match feature
//		if(relaxedSpanMatch && wordIncluded){
//			addFeature("EXMAT=" + exMat, "RLMAT=" + rlMat + ", " + "WRDIN=" + wdInd + ", " + given, model, fv);
//		}
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, boolean wordIncluded, boolean headMatch, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		addFeature("TYPE=" + anaph.mentionType, "NEWCLUSTER", model, fv);
		String given  = "TYPE=" + anaph.mentionType + ", " + "NEWCLUSTER";
		
		String hdMat = Boolean.toString(headMatch);
		String wdInd = Boolean.toString(wordIncluded);
		
		if(anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER){
			addFeature("WRDIN=" + wdInd + ", " + "HDMAT=" + hdMat, given, model, fv);
		}
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		Pair<Integer, Integer> ids = model.getFeatureIndex(feat, given);
		if(fv != null && ids != null){
			fv.addFeature(ids.first, ids.second);
		}
	}
}
