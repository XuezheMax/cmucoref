package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import cmucoref.util.Pair;

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genCoreferentFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		
		// anaph is coreferent with previous antec 
		//attribute match features
		genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, model, fv);
	}
	
	public void genSpecialCasesFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		// anaph is appositive to antec 
		//attribute match features
		String given = null;
		if(antec.isPronominal()){
			given = "ANTECTYPE=NOMINAL, COREF";
		}
		else{			
			given= "ANTECTYPE=" + antec.mentionType + ", COREF";
		}
		
		String feat = null;
		if(anaph.isPronominal()){
			feat = "ANAPHTYPE=NOMINAL";
		}
		else{
			feat = "ANAPHTYPE=" + anaph.mentionType;
		}
		
		feat = feat + ", " + "GENMAT=true" + ", " + "NUMMAT=true";
		feat = feat + ", " + "ANIMAT=true";
		
		if(antec.isProper() && anaph.isProper()){
			feat = feat + ", " + "NERMAT=true";
			
		}
		feat = feat + ", " + "SPMAT=true";
		addFeature(feat, given, model, fv);
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		//mention type features
		String feat = "ANAPHTYPE=" + anaph.mentionType;
		String given = "ANTECTYPE=" + antec.mentionType + ", COREF";
		
		//distance of sentences
		if(anaph.isPronominal() || antec.isPronominal() || !anaph.spanMatch){
			int distOfSent = anaph.getDistOfSent(antec);
			feat = feat + ", " + "DISTOFSENT=" + distOfSent;
		}
		
//		//distance of mentions (only apply for pronominals)
//		if(anaph.isPronominal() || antec.isPronominal()){
//			int distOfMention = anaph.getDistOfMention(antec);
//			feat = feat + ", " + "DISTOFMENTION=" + distOfMention;
//		}
		
		//number match
		boolean numMat = anaph.spanMatch ? true : anaph.numberAgree(antec);
				
		//gender match
		boolean genMat = anaph.spanMatch ? true : anaph.genderAgree(antec);
		
		feat = feat + ", " + "GENMAT=" + genMat + ", " + "NUMMAT=" + numMat;
		
		//animacy match (does not apply for pronominal mentions)
		if(!anaph.isPronominal() && !antec.isPronominal()){
			boolean aniMat = anaph.spanMatch ? true : anaph.animateAgree(antec);
			feat = feat + ", " + "ANIMAT=" + aniMat;
		}
		
		//person match (only apply for pronominal mentions)
		if(anaph.isPronominal() && antec.isPronominal()){
			boolean perMat = anaph.personAgree(antec);
			feat = feat + ", " + "PERMAT=" + perMat;
		}
		
		//ner match (only apply for proper mentions)
		if(anaph.isProper() && antec.isProper()){
			boolean nerMat = anaph.spanMatch ? true : anaph.NERAgree(antec);
			feat = feat + ", " + "NERMAT=" + nerMat;
		}
		
		//span match features (only apply for nominative and proper mentions)
		if((anaph.isNominative() || anaph.isProper()) && (antec.isNominative() || antec.isProper())){
			
			boolean spanMatch = anaph.spanMatch;
			boolean headMatch = anaph.spanMatch ? true : antec.headMatch(antecSent, anaph, anaphSent);
			
			if(spanMatch){
				feat = feat + ", " + "SPMAT=true";
			}
			else{
				feat = feat + ", " + "SPMAT=false" + ", " + "HDMAT=" + headMatch;
			}
		}
		addFeature(feat, given, model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		//type features
		String feat = "TYPE=" + anaph.mentionType;
		String given = "NEWCLUSTER";
		
		//attrMat within 5 sentences (only apply for pronominals)
		if(anaph.isPronominal()){
			feat = feat + ", " 
					+ "LOCATTRMATOFSENT=" + anaph.localAttrMatchOfSent + ", " 
//					+ "LOCATTRMATOFMENTION=" + anaph.localAttrMatchOfMention + ", " 
					+ "LOCATTRMATTYPE=" + anaph.localAttrMatchType;
		}
		else if(anaph.isNominative() || anaph.isProper()){
			if(anaph.spanMatch){
				feat = feat  + ", " + "SPMAT=true" + ", " 
						+ "SPMATPOS=" + anaph.closestSpanMatchPos + ", " 
						+ "SPMATTYPE=" + anaph.closestSpanMatchType;
			}
			else{
				feat = feat + ", " + "SPMAT=false";
			}
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
