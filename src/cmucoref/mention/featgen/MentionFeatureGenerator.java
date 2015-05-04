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
		
		// anaph is coreferent with previous antec 
		//attribute match features
		genAttrMatchFeatures(anaph, anaphSent, antec, antecSent, dict, model, fv);
	}
	
	protected void genAttrMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, Dictionaries dict, CorefModel model, FeatureVector fv){
		//mention type features
		String feat = "ANAPHTYPE=" + anaph.mentionType;
		String given = "ANTECTYPE=" + antec.mentionType + ", COREF";
		
		//distance of sentences
		if(anaph.isPronominal() || antec.isPronominal() || !anaph.preciseMatch){
			int distOfSent = anaph.getDistOfSent(antec);
			feat = feat + ", " + "DISTOFSENT=" + distOfSent;
		}
		
		//number match
		boolean numMat = anaph.preciseMatch ? true : anaph.numberAgree(antec);
				
		//gender match
		boolean genMat = anaph.preciseMatch ? true : anaph.genderAgree(antec);
		
		feat = feat + ", " + "GENMAT=" + genMat + ", " + "NUMMAT=" + numMat;
		
		//animacy match
		boolean aniMat = anaph.preciseMatch ? true : anaph.animateAgree(antec);
		feat = feat + ", " + "ANIMAT=" + aniMat;
		
		//person match (only apply for pronominal mentions)
		if(anaph.isPronominal() && antec.isPronominal()){
			boolean perMat = anaph.personAgree(antec);
			feat = feat + ", " + "PERMAT=" + perMat;
		}
		
		//ner match
		boolean nerMat = anaph.preciseMatch ? true : anaph.NERAgree(antec, dict);
		feat = feat + ", " + "NERMAT=" + nerMat;
		
		//precise match features (only apply for nominative and proper mentions)
		if((anaph.isNominative() || anaph.isProper()) && (antec.isNominative() || antec.isProper())){
			
			boolean preciseMatch = anaph.preciseMatch;
			boolean headMatch = anaph.preciseMatch ? true : antec.headMatch(antecSent, anaph, anaphSent);
			
			if(preciseMatch){
				feat = feat + ", " + "PRECMAT=true";
			}
			else{
				feat = feat + ", " + "PRECMAT=false" + ", " + "HDMAT=" + headMatch;
			}
		}
		addFeature(feat, given, model, fv);
	}
	
	public void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		//type features
		String feat = "TYPE=" + anaph.mentionType;
		String given = "NEWCLUSTER";
		
		if(anaph.isPronominal() || anaph.preciseMatch){
			feat = feat + ", " 
					+ "LOCATTRMATOFSENT=" + anaph.localAttrMatchOfSent + ", " 
					+ "LOCATTRMATTYPE=" + anaph.localAttrMatchType;
		}
		
		if(anaph.preciseMatch){
			feat = feat  + ", " + "SPMAT=true" + ", " 
					+ "PRECMATPOS=" + anaph.closestPreciseMatchPos + ", " 
					+ "PRECMATTYPE=" + anaph.closestPreciseMatchType;
		}
		else{
			feat = feat + ", " + "PRECMAT=false";
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
